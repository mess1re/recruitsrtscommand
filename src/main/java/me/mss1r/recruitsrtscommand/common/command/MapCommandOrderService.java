package me.mss1r.recruitsrtscommand.common.command;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.IRangedRecruit;
import com.talhanation.recruits.entities.IStrategicFire;
import com.talhanation.recruits.entities.BowmanEntity;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import com.talhanation.recruits.entities.NomadEntity;
import com.talhanation.recruits.entities.ScoutEntity;
import com.talhanation.recruits.entities.ai.RecruitRangedBowAttackGoal;
import com.talhanation.recruits.util.FormationUtils;
import com.talhanation.recruits.util.RecruitCommanderUtil;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsGroupsSaveData;
import me.mss1r.recruitsrtscommand.api.FireZoneShape;
import me.mss1r.recruitsrtscommand.api.MapObjectAction;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import me.mss1r.recruitsrtscommand.api.MapOrder;
import me.mss1r.recruitsrtscommand.api.RecruitsRTSCommandApi;
import me.mss1r.recruitsrtscommand.config.RecruitsRTSCommandServerConfig;
import me.mss1r.recruitsrtscommand.network.RecruitsRTSCommandNetwork;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class MapCommandOrderService {
    public static final MapCommandOrderService INSTANCE = new MapCommandOrderService();

    public static final int HEALTH_UNKNOWN = 255;
    private static final int ORDER_STALE_TIMEOUT_TICKS = 900;
    private static final int ORDER_STALL_REISSUE_TICKS = 100;
    private static final double MOVEMENT_EPSILON_SQR = 0.02D;
    private static final double FORMATION_SLOT_TOLERANCE = 2.5D;
    private static final double FORMATION_ARRIVED_SHARE = 0.8D;
    private static final int ORDER_STALL_REISSUE_LIMIT = 3;
    private static final int FORMATION_SETTLE_TICKS = 20;
    private static final double SCOUT_SIGHT = 2.5D;
    private static final double MOUNTED_SIGHT = 1.5D;
    private static final int PURSUIT_REPATH_TICKS = 20;
    private static final double PURSUIT_CONTACT = 12.0D;

    private final Map<OrderKey, ActiveOrder> orders = new HashMap<>();
    private final Map<UUID, Map<UUID, SelectionTarget>> playerSelections = new HashMap<>();
    private final Map<UUID, Map<UUID, MapObjectSnapshot>> visibleObjects = new HashMap<>();

    private MapCommandOrderService() {
    }

    public void sendSnapshots(ServerPlayer player) {
        sendSnapshots(player, List.of());
    }

    public void sendSnapshots(ServerPlayer player, List<UUID> selected) {
        List<MapObjectSnapshot> objects = buildObjects(player);
        rememberVisibleObjects(player, objects);
        Map<UUID, List<UUID>> crews = new HashMap<>();
        List<CommandGroupSnapshot> snapshots = buildSnapshots(player, crews);
        RecruitsRTSCommandNetwork.sendSnapshots(player, snapshots, buildRoutes(player),
                withActions(player, withCrews(player, objects, crews), selected));
    }

    private List<MapObjectSnapshot> withCrews(ServerPlayer player, List<MapObjectSnapshot> objects,
                                              Map<UUID, List<UUID>> crews) {
        if (objects.isEmpty() || crews.isEmpty()) return objects;

        List<MapObjectSnapshot> manned = new ArrayList<>(objects.size());
        for (MapObjectSnapshot object : objects) {
            List<UUID> crew = player.getUUID().equals(object.ownerId())
                    ? crews.get(object.id())
                    : null;
            manned.add(crew == null || crew.isEmpty() ? object : object.withCrew(crew));
        }
        return manned;
    }

    private List<MapObjectSnapshot> withActions(ServerPlayer player, List<MapObjectSnapshot> objects,
                                                List<UUID> selected) {
        if (objects.isEmpty()) return objects;

        List<UUID> commandable = commandableMembers(player, selected);
        List<MapObjectSnapshot> described = new ArrayList<>(objects.size());
        for (MapObjectSnapshot object : objects) {
            described.add(RecruitsRTSCommandApi.withActions(player, object, commandable));
        }
        return described;
    }

    private List<UUID> commandableMembers(ServerPlayer player, List<UUID> members) {
        if (members == null || members.isEmpty()) return List.of();

        ServerLevel level = player.serverLevel();
        Set<UUID> allowed = new LinkedHashSet<>(members.size());
        for (UUID member : members) {
            if (level.getEntity(member) instanceof AbstractRecruitEntity recruit
                    && recruit.isEffectedByCommand(player.getUUID())) {
                allowed.add(member);
            }
        }
        return List.copyOf(allowed);
    }

    private void rememberVisibleObjects(ServerPlayer player, List<MapObjectSnapshot> objects) {
        Map<UUID, MapObjectSnapshot> visible = new HashMap<>(objects.size());
        for (MapObjectSnapshot object : objects) visible.put(object.id(), object.withoutActions());
        visibleObjects.put(player.getUUID(), visible);
    }

    @Nullable
    private MapObjectSnapshot currentVisibleObject(ServerPlayer player, UUID objectId) {
        Map<UUID, MapObjectSnapshot> remembered = visibleObjects.get(player.getUUID());
        if (remembered == null || !remembered.containsKey(objectId)) {
            return null;
        }
        return buildObjects(player).stream()
                .filter(candidate -> candidate.id().equals(objectId))
                .findFirst()
                .orElse(null);
    }

    public void submitObjectAction(ServerPlayer player, UUID objectId, List<UUID> members, String actionId,
                                   BlockPos target) {
        if (player == null || objectId == null || actionId == null || actionId.isEmpty()) return;

        MapObjectSnapshot object = currentVisibleObject(player, objectId);
        if (object == null) return;

        List<UUID> commanded = commandableMembers(player, members);
        MapOrder mapOrder = object.orders().entrySet().stream()
                .filter(entry -> entry.getValue().equals(actionId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (mapOrder != null) {
            boolean needsPoint = mapOrder == MapOrder.MOVE || mapOrder == MapOrder.MOVE_APPEND;
            if (!player.getUUID().equals(object.ownerId()) || needsPoint != (target != null)) return;
            RecruitsRTSCommandApi.performObjectAction(player, objectId, commanded, actionId, target);
            return;
        }

        MapObjectAction offered = RecruitsRTSCommandApi.withActions(player, object, commanded).actions()
                .stream()
                .filter(action -> action.id().equals(actionId))
                .filter(MapObjectAction::enabled)
                .findFirst()
                .orElse(null);
        if (offered == null || offered.needsPoint() != (target != null)) return;

        RecruitsRTSCommandApi.performObjectAction(player, objectId, commanded, actionId, target);
    }

    public List<MapObjectSnapshot> buildObjects(ServerPlayer player) {
        List<MapObjectSnapshot> objects = RecruitsRTSCommandApi.collectObjects(player);
        if (objects.isEmpty()) return objects;

        List<Watcher> watchers = watchPosts(player);
        boolean scouting = RecruitsRTSCommandServerConfig.SCOUTING_ENABLED.get();
        ResourceLocation currentDimension = player.serverLevel().dimension().location();

        List<MapObjectSnapshot> seen = new ArrayList<>(objects.size());
        Set<UUID> objectIds = new HashSet<>();
        for (MapObjectSnapshot object : objects) {
            if (object == null || object.id() == null || object.icon() == null
                    || object.position() == null || object.dimension() == null
                    || !objectIds.add(object.id())) {
                continue;
            }
            if (!currentDimension.equals(object.dimension())) continue;
            boolean ours = player.getUUID().equals(object.ownerId());
            if (ours || !scouting || withinSight(object.position(), watchers)) {
                seen.add(object);
            }
        }
        return seen;
    }

    public List<CommandOrderRoute> buildRoutes(ServerPlayer player) {
        if (player == null) return List.of();

        List<CommandOrderRoute> routes = new ArrayList<>();
        for (ActiveOrder order : orders.values()) {
            if (!player.getUUID().equals(order.ownerId) || order.waypoints.isEmpty()) continue;

            ServerLevel level = serverLevel(player.serverLevel().getServer(), order.dimension);
            List<AbstractRecruitEntity> walking = level == null
                    ? List.of()
                    : resolveMembers(level, order.memberIds, order.ownerId);
            BlockPos origin = walking.isEmpty() ? null : BlockPos.containing(centerOf(walking));
            routes.add(new CommandOrderRoute(new ArrayList<>(order.memberIds), order.dimension,
                    origin, new ArrayList<>(order.waypoints)));
        }
        return routes;
    }

    public List<CommandGroupSnapshot> buildSnapshots(ServerPlayer player) {
        return buildSnapshots(player, null);
    }

    private List<CommandGroupSnapshot> buildSnapshots(ServerPlayer player,
                                                      Map<UUID, List<UUID>> crews) {
        if (player == null) return List.of();

        Map<UUID, SelectionTarget> selectionIndex = new HashMap<>();
        List<CommandGroupSnapshot> snapshots = new ArrayList<>();
        List<Watcher> watchers = watchPosts(player);

        for (RecruitsGroup group : getAllGroups(player)) {
            for (SelectionTarget selection : buildSelections(player, group, crews)) {
                List<CommandUnitSnapshot> units = seenBy(player, watchers, selection);
                if (units.isEmpty()) continue;

                BlockPos visibleAnchor = units.size() == selection.units.size()
                        ? selection.anchor
                        : BlockPos.containing(centerOfSnapshots(units));
                List<UUID> visibleMembers = units.stream().map(CommandUnitSnapshot::unitId).toList();
                selectionIndex.put(selection.selectionId, new SelectionTarget(
                        selection.selectionId, selection.rootGroupId, selection.ownerId,
                        selection.dimension, visibleAnchor, visibleMembers, units));

                snapshots.add(new CommandGroupSnapshot(
                        selection.selectionId,
                        selection.ownerId,
                        visibleAnchor,
                        selection.dimension,
                        units
                ));
            }
        }

        playerSelections.put(player.getUUID(), selectionIndex);
        return snapshots;
    }

    public void submitMoveOrder(ServerPlayer player, UUID selectionId, BlockPos target, boolean append,
                                int formation) {
        submitMoveOrder(player, selectionId, null, target, append, formation, false, false);
    }

    public void submitMoveOrder(ServerPlayer player, UUID selectionId, List<UUID> members, BlockPos target,
                                boolean append, int formation, boolean tight, boolean holdFormation) {
        if (player == null || selectionId == null || target == null) return;

        SelectionTarget selection = members == null || members.isEmpty()
                ? resolveOwnedSelection(player, selectionId)
                : resolveClientSelection(player, selectionId, members);
        if (selection == null || !canCommand(player, selection)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), selection.dimension);
        if (level == null || !level.getWorldBorder().isWithinBounds(target)) return;

        BlockPos surface = FormationUtils.getPositionOrSurface(level, target);
        if (append) {
            appendMoveOrder(player, selection, surface, formation, tight, holdFormation);
            return;
        }

        detachMembersFromOrders(player.getUUID(), selection.dimension, selection.memberIds);
        OrderKey key = unusedOrderKey(selection.selectionId, selection.dimension);
        ActiveOrder order = new ActiveOrder(
                player.getUUID(), selection.rootGroupId, selection.memberIds, selection.dimension);
        orders.put(key, order);
        order.ownerId = player.getUUID();
        order.rootGroupId = selection.rootGroupId;
        order.dimension = selection.dimension;
        order.memberIds.clear();
        order.memberIds.addAll(selection.memberIds);
        order.formation = Mth.clamp(formation, 0, 8);
        order.tight = tight;
        order.holdFormation = holdFormation;

        order.waypoints.clear();
        order.resetIssueState();
        if (order.waypoints.isEmpty() || !Objects.equals(order.waypoints.peekLast(), surface)) {
            order.waypoints.addLast(surface.immutable());
        }
    }

    private void appendMoveOrder(ServerPlayer player, SelectionTarget selection, BlockPos surface,
                                 int formation, boolean tight, boolean holdFormation) {
        Set<UUID> remaining = new LinkedHashSet<>(selection.memberIds);
        List<Map.Entry<OrderKey, ActiveOrder>> existing = new ArrayList<>(orders.entrySet());

        for (Map.Entry<OrderKey, ActiveOrder> entry : existing) {
            ActiveOrder current = entry.getValue();
            if (!player.getUUID().equals(current.ownerId)
                    || !selection.dimension.equals(entry.getKey().dimension)
                    || current.pursuing != null || current.pursuingObject != null
                    || current.waypoints.isEmpty()) {
                continue;
            }

            Set<UUID> selectedHere = new LinkedHashSet<>(current.memberIds);
            selectedHere.retainAll(remaining);
            if (selectedHere.isEmpty()) continue;

            ActiveOrder appended;
            if (selectedHere.size() == current.memberIds.size()) {
                appended = current;
            } else {
                current.memberIds.removeAll(selectedHere);
                appended = current.copyMarchFor(selectedHere);
                orders.put(new OrderKey(UUID.randomUUID(), selection.dimension), appended);
            }
            configureMoveOrder(appended, player.getUUID(), selection.rootGroupId,
                    selection.dimension, formation, tight, holdFormation);
            appendWaypoint(appended, surface);
            remaining.removeAll(selectedHere);
        }

        for (Map.Entry<OrderKey, ActiveOrder> entry : existing) {
            ActiveOrder current = entry.getValue();
            if (!player.getUUID().equals(current.ownerId)
                    || !selection.dimension.equals(entry.getKey().dimension)
                    || current.pursuing == null && current.pursuingObject == null) {
                continue;
            }
            current.memberIds.removeAll(selection.memberIds);
            if (current.memberIds.isEmpty()) orders.remove(entry.getKey(), current);
        }

        if (!remaining.isEmpty()) {
            ActiveOrder order = new ActiveOrder(player.getUUID(), selection.rootGroupId,
                    List.copyOf(remaining), selection.dimension);
            configureMoveOrder(order, player.getUUID(), selection.rootGroupId,
                    selection.dimension, formation, tight, holdFormation);
            appendWaypoint(order, surface);
            orders.put(unusedOrderKey(selection.selectionId, selection.dimension), order);
        }
    }

    private static void configureMoveOrder(ActiveOrder order, UUID ownerId, UUID rootGroupId,
                                           ResourceLocation dimension, int formation,
                                           boolean tight, boolean holdFormation) {
        order.ownerId = ownerId;
        order.rootGroupId = rootGroupId;
        order.dimension = dimension;
        order.formation = Mth.clamp(formation, 0, 8);
        order.tight = tight;
        order.holdFormation = holdFormation;
    }

    private static void appendWaypoint(ActiveOrder order, BlockPos surface) {
        if (order.waypoints.isEmpty() || !Objects.equals(order.waypoints.peekLast(), surface)) {
            order.waypoints.addLast(surface.immutable());
        }
    }

    public void submitAttackGroupOrder(ServerPlayer player, UUID attackerSelectionId, UUID targetSelectionId) {
        submitAttackGroupOrder(player, attackerSelectionId, null, targetSelectionId);
    }

    public void submitAttackGroupOrder(ServerPlayer player, UUID attackerSelectionId, List<UUID> members,
                                       UUID targetSelectionId) {
        if (player == null || attackerSelectionId == null || targetSelectionId == null
                || attackerSelectionId.equals(targetSelectionId)) {
            return;
        }

        buildSnapshots(player);
        SelectionTarget attacker = members == null || members.isEmpty()
                ? resolveOwnedSelection(player, attackerSelectionId)
                : resolveClientSelection(player, attackerSelectionId, members);
        SelectionTarget target = resolveSelection(player, targetSelectionId);
        if (attacker == null || target == null || Objects.equals(attacker.ownerId, target.ownerId)) return;
        if (!canCommand(player, attacker) || !Objects.equals(attacker.dimension, target.dimension)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), attacker.dimension);
        if (level == null) return;

        List<AbstractRecruitEntity> attackers = resolveMembers(level, attacker.memberIds, attacker.ownerId);
        List<LivingEntity> targets = resolveLivingMembers(level, target.memberIds);
        if (attackers.isEmpty() || targets.isEmpty()) return;

        AbstractRecruitEntity judge = attackers.get(0);
        targets.removeIf(victim -> !judge.canAttack(victim));
        if (targets.isEmpty()) return;

        detachMembersFromOrders(player.getUUID(), attacker.dimension, attacker.memberIds);
        standDown(attackers);

        applyAttack(level, attackers, targets);

        OrderKey key = unusedOrderKey(attacker.selectionId, attacker.dimension);
        ActiveOrder chase = new ActiveOrder(player.getUUID(), attacker.rootGroupId,
                attacker.memberIds, attacker.dimension);
        chase.pursuing = target.selectionId;
        orders.put(key, chase);
    }

    public void submitAttackObjectOrder(ServerPlayer player, UUID attackerSelectionId,
                                        List<UUID> members, UUID objectId) {
        if (player == null || attackerSelectionId == null || objectId == null) return;

        MapObjectSnapshot object = currentVisibleObject(player, objectId);
        if (object == null) return;
        if (player.getUUID().equals(object.ownerId())) return;

        SelectionTarget attacker = members == null || members.isEmpty()
                ? resolveOwnedSelection(player, attackerSelectionId)
                : resolveClientSelection(player, attackerSelectionId, members);
        if (attacker == null || !canCommand(player, attacker)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), attacker.dimension);
        if (level == null || !(level.getEntity(objectId) instanceof LivingEntity victim)
                || !victim.isAlive()) {
            return;
        }

        List<AbstractRecruitEntity> attackers = resolveMembers(level, attacker.memberIds, attacker.ownerId);
        if (attackers.isEmpty()) return;

        if (!attackers.get(0).canAttack(victim)) return;

        detachMembersFromOrders(player.getUUID(), attacker.dimension, attacker.memberIds);
        standDown(attackers);
        applyAttack(level, attackers, List.of(victim));

        ActiveOrder chase = new ActiveOrder(player.getUUID(), attacker.rootGroupId,
                attacker.memberIds, attacker.dimension);
        chase.pursuingObject = objectId;
        orders.put(unusedOrderKey(attacker.selectionId, attacker.dimension), chase);
    }

    public void submitFaceOrder(ServerPlayer player, UUID selectionId, List<UUID> members, BlockPos lookAt,
                                int formation, boolean tight, boolean holdFormation) {
        if (player == null || selectionId == null || lookAt == null || formation <= 0) return;

        SelectionTarget selection = members == null || members.isEmpty()
                ? resolveOwnedSelection(player, selectionId)
                : resolveClientSelection(player, selectionId, members);
        if (selection == null || !canCommand(player, selection)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), selection.dimension);
        if (level == null) return;

        List<AbstractRecruitEntity> recruits = resolveMembers(level, selection.memberIds, selection.ownerId);
        if (recruits.isEmpty()) return;

        float heading = facingYaw(centerOf(recruits), Vec3.atCenterOf(lookAt), player.getYRot());
        float previousYaw = player.getYRot();
        float previousHeadYaw = player.getYHeadRot();
        player.setYRot(heading);
        player.setYHeadRot(heading);
        try {
            CommandEvents.onFaceCommand(player, recruits, formation, tight, holdFormation);
        } finally {
            player.setYRot(previousYaw);
            player.setYHeadRot(previousHeadYaw);
        }
    }

    public void submitStrategicFireOrder(ServerPlayer player, UUID selectionId, BlockPos target, int radius) {
        submitStrategicFireOrder(player, selectionId, null, target, radius, radius,
                FireZoneShape.CIRCLE.ordinal());
    }

    public void submitStrategicFireOrder(ServerPlayer player, UUID selectionId, List<UUID> members,
                                         BlockPos target, int radiusX, int radiusZ, int shapeOrdinal) {
        if (player == null || selectionId == null || target == null) return;

        SelectionTarget selection = members == null || members.isEmpty()
                ? resolveOwnedSelection(player, selectionId)
                : resolveClientSelection(player, selectionId, members);
        if (selection == null || !canCommand(player, selection)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), selection.dimension);
        if (level == null) return;

        detachMembersFromOrders(player.getUUID(), selection.dimension, selection.memberIds);
        BlockPos surfaceTarget = FormationUtils.getPositionOrSurface(level, target);
        List<AbstractRecruitEntity> recruits = resolveMembers(level, selection.memberIds, selection.ownerId);
        List<AbstractRecruitEntity> archers = new ArrayList<>(recruits);
        archers.sort(Comparator.comparing(recruit -> recruit.getUUID().toString()));
        FireZoneShape shape = FireZoneShape.fromOrdinal(shapeOrdinal);
        for (AbstractRecruitEntity recruit : recruits) {
            recruit.setTarget(null);
            recruit.setFollowState(3);
            recruit.setHoldPos(recruit.position());
            recruit.setShouldMovePos(false);
            if (recruit instanceof IStrategicFire strategicFire) {
                strategicFire.setStrategicFirePos(spreadFirePos(level, surfaceTarget, shape, radiusX, radiusZ,
                        archers.indexOf(recruit), archers.size()));
                strategicFire.setShouldStrategicFire(true);
            } else if (recruit instanceof NomadEntity) {
                recruit.setMovePos(surfaceTarget);
                recruit.setShouldMovePos(true);
                recruit.setShouldRanged(true);
            }
        }

        RecruitsRTSCommandApi.areaFireOrdered(player, List.copyOf(selection.memberIds), surfaceTarget,
                radiusX, radiusZ, shape);
    }

    private BlockPos spreadFirePos(ServerLevel level, BlockPos center, FireZoneShape shape,
                                   int radiusX, int radiusZ, int index, int total) {
        if (radiusX <= 0 || radiusZ <= 0 || total <= 1 || index < 0) {
            return center;
        }

        double spread = Math.sqrt((index + 0.5D) / total);
        double angle = index * 2.39996322972865332D;
        double unitX = Math.cos(angle) * spread;
        double unitZ = Math.sin(angle) * spread;
        if (shape == FireZoneShape.RECTANGLE) {
            double longest = Math.max(Math.abs(unitX), Math.abs(unitZ));
            if (longest > 0.0001D) {
                unitX /= longest;
                unitZ /= longest;
                unitX *= spread;
                unitZ *= spread;
            }
        }

        BlockPos spot = center.offset(
                (int) Math.round(unitX * radiusX), 0,
                (int) Math.round(unitZ * radiusZ));
        return FormationUtils.getPositionOrSurface(level, spot);
    }

    public void submitStrategicFireHoldOrder(ServerPlayer player, UUID selectionId, List<UUID> members) {
        if (player == null || selectionId == null) return;

        SelectionTarget selection = members == null || members.isEmpty()
                ? resolveOwnedSelection(player, selectionId)
                : resolveClientSelection(player, selectionId, members);
        if (selection == null || !canCommand(player, selection)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), selection.dimension);
        if (level == null) return;

        List<AbstractRecruitEntity> recruits = resolveMembers(level, selection.memberIds, selection.ownerId);
        for (AbstractRecruitEntity recruit : recruits) {
            if (recruit instanceof IStrategicFire strategicFire) {
                strategicFire.setShouldStrategicFire(false);
            }
            if (recruit instanceof NomadEntity) {
                recruit.setShouldRanged(false);
                recruit.setShouldMovePos(false);
                recruit.clearMovePos();
            }
        }

        RecruitsRTSCommandApi.areaFireCleared(player, List.copyOf(selection.memberIds));
    }

    public void submitBehaviorOrder(ServerPlayer player, UUID selectionId, List<UUID> members, Integer aggroState,
                                    Boolean fireAtWill, Integer followState) {
        submitBehaviorOrder(player, selectionId, members, aggroState, fireAtWill, followState, null);
    }

    public void submitBehaviorOrder(ServerPlayer player, UUID selectionId, List<UUID> members, Integer aggroState,
                                    Boolean fireAtWill, Integer followState, Boolean shields) {
        if (player == null || selectionId == null) return;
        if (aggroState == null && fireAtWill == null && followState == null && shields == null) return;

        SelectionTarget selection = members == null || members.isEmpty()
                ? resolveOwnedSelection(player, selectionId)
                : resolveClientSelection(player, selectionId, members);
        if (selection == null || !canCommand(player, selection)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), selection.dimension);
        if (level == null) return;

        List<AbstractRecruitEntity> recruits = resolveMembers(level, selection.memberIds, selection.ownerId);
        if (recruits.isEmpty()) return;

        if (followState != null) {
            int state = Mth.clamp(followState, 0, 5);
            detachMembersFromOrders(player.getUUID(), selection.dimension, selection.memberIds);
            standDown(recruits);
            RecruitCommanderUtil.setRecruitsClearTargets(recruits);
            for (AbstractRecruitEntity recruit : recruits) {
                recruit.setFollowState(state);
                if (state == 0) {
                    recruit.setShouldMovePos(false);
                    recruit.setShouldHoldPos(false);
                    recruit.clearMovePos();
                    recruit.clearHoldPos();
                }
            }
        }

        if (aggroState != null) {
            int state = Mth.clamp(aggroState, 0, 3);
            if (state == 2) detachMembersFromOrders(
                    player.getUUID(), selection.dimension, selection.memberIds);
            for (AbstractRecruitEntity recruit : recruits) {
                CommandEvents.onAggroCommand(player.getUUID(), recruit, state, null, false);
            }
        }

        if (fireAtWill != null) {
            for (AbstractRecruitEntity recruit : recruits) {
                CommandEvents.onRangedFireCommand(player, player.getUUID(), recruit, null, fireAtWill);
            }
        }

        if (shields != null) {
            for (AbstractRecruitEntity recruit : recruits) {
                CommandEvents.onShieldsCommand(player, player.getUUID(), recruit, null, shields);
            }
        }
    }

    public void tick(MinecraftServer server) {
        if (server == null || orders.isEmpty()) return;

        double arrivalDistance = Math.max(1.0D, RecruitsRTSCommandServerConfig.ARRIVAL_DISTANCE_BLOCKS.get());
        int repathInterval = Math.max(20, RecruitsRTSCommandServerConfig.REPATH_INTERVAL_TICKS.get());
        boolean continueInCombat = RecruitsRTSCommandServerConfig.CONTINUE_MOVE_IN_COMBAT.get();

        orders.entrySet().removeIf(entry -> tickOrder(server, entry.getKey(), entry.getValue(),
                arrivalDistance, repathInterval, continueInCombat));
    }

    public void clearPlayerOrders(UUID playerId) {
        if (playerId == null) return;
        orders.entrySet().removeIf(entry -> playerId.equals(entry.getValue().ownerId));
        playerSelections.remove(playerId);
        visibleObjects.remove(playerId);
    }

    public void clearAll() {
        orders.clear();
        playerSelections.clear();
        visibleObjects.clear();
    }

    private boolean tickOrder(MinecraftServer server, OrderKey key, ActiveOrder order, double arrivalDistance,
                              int repathInterval, boolean continueInCombat) {
        ServerPlayer owner = server.getPlayerList().getPlayer(order.ownerId);
        if (owner == null) return true;

        ServerLevel level = serverLevel(server, key.dimension);
        if (level == null) return true;

        List<AbstractRecruitEntity> recruits = resolveMembers(level, order.memberIds, order.ownerId);
        if (recruits.isEmpty()) return true;

        if (order.pursuing != null || order.pursuingObject != null) {
            return tickPursuit(server, level, order, recruits);
        }

        if (order.waypoints.isEmpty()) return true;
        if (!continueInCombat && recruits.stream().anyMatch(MapCommandOrderService::isInCombat)) return true;

        BlockPos target = order.waypoints.peekFirst();
        if (target == null) return true;

        double distanceSqr = distanceSqrToTarget(recruits, target);
        Vec3 center = centerOf(recruits);
        boolean moved = order.lastCenter == null || order.lastCenter.distanceToSqr(center) > MOVEMENT_EPSILON_SQR;
        order.lastCenter = center;
        if (moved) {
            order.stagnantTicks = 0;
            order.bestDistanceSqr = Math.min(order.bestDistanceSqr, distanceSqr);
        } else if (distanceSqr + 1.0D < order.bestDistanceSqr) {
            order.bestDistanceSqr = distanceSqr;
            order.stagnantTicks = 0;
        } else {
            order.stagnantTicks++;
        }
        if (order.stagnantTicks > ORDER_STALE_TIMEOUT_TICKS) return true;

        if (hasArrived(order, recruits, distanceSqr, arrivalDistance)) {
            order.waypoints.pollFirst();
            order.resetIssueState();
            target = order.waypoints.peekFirst();
            if (target == null) return true;
        }

        order.ticksSinceIssue++;
        BlockPos surface = FormationUtils.getPositionOrSurface(level, target);
        boolean newWaypoint = !order.commandIssued || !Objects.equals(order.issuedWaypoint, surface);
        boolean stalled = order.stagnantTicks > ORDER_STALL_REISSUE_TICKS;
        boolean dueForRepath = order.formation <= 0 && order.ticksSinceIssue >= repathInterval;

        if (stalled && ++order.stallReissues > ORDER_STALL_REISSUE_LIMIT) {
            return true;
        }
        if (newWaypoint || stalled || dueForRepath) {
            issueMove(owner, level, recruits, order, surface, newWaypoint);
            if (newWaypoint || stalled) {
                order.stagnantTicks = 0;
                order.bestDistanceSqr = distanceSqr;
            }
        }

        return false;
    }

    private boolean tickPursuit(MinecraftServer server, ServerLevel level, ActiveOrder order,
                                List<AbstractRecruitEntity> recruits) {
        List<LivingEntity> targets;
        if (order.pursuingObject != null) {
            targets = level.getEntity(order.pursuingObject) instanceof LivingEntity quarry
                    && quarry.isAlive()
                    ? List.of(quarry)
                    : List.of();
        } else {
            ServerPlayer owner = server.getPlayerList().getPlayer(order.ownerId);
            SelectionTarget squad = owner == null ? null : resolveSelection(owner, order.pursuing);
            targets = squad == null ? List.of() : resolveLivingMembers(level, squad.memberIds);
        }
        if (targets.isEmpty()) {
            for (AbstractRecruitEntity recruit : recruits) {
                recruit.setShouldMovePos(false);
            }
            return true;
        }

        order.ticksSinceIssue++;
        if (order.ticksSinceIssue < PURSUIT_REPATH_TICKS) return false;
        order.ticksSinceIssue = 0;

        Map<UUID, LivingEntity> assignment = assignTargets(recruits, targets);
        boolean anyShooters = false;
        boolean anyFar = false;

        for (AbstractRecruitEntity recruit : recruits) {
            LivingEntity victim = assignment.get(recruit.getUUID());
            if (victim == null) continue;

            recruit.setTarget(victim);
            if (isRangedLike(recruit)) {
                anyShooters = true;
                holdShootingPosition(recruit, victim);
                continue;
            }
            if (recruit.distanceToSqr(victim) <= PURSUIT_CONTACT * PURSUIT_CONTACT) {
                recruit.setShouldMovePos(false);
                continue;
            }
            anyFar = true;
            RecruitCommanderUtil.setRecruitsMove(List.of(recruit),
                    FormationUtils.getPositionOrSurface(level, victim.blockPosition()));
        }

        return !anyFar && !anyShooters;
    }

    private boolean hasArrived(ActiveOrder order, List<AbstractRecruitEntity> recruits,
                               double distanceSqr, double arrivalDistance) {
        if (order.formation <= 0) {
            return distanceSqr <= arrivalDistance * arrivalDistance;
        }
        if (!order.commandIssued || order.ticksSinceIssue < FORMATION_SETTLE_TICKS) return false;

        int inPlace = 0;
        for (AbstractRecruitEntity recruit : recruits) {
            if (isAtAssignedSpot(recruit)) inPlace++;
        }
        return inPlace >= Math.max(1, (int) Math.ceil(recruits.size() * FORMATION_ARRIVED_SHARE));
    }

    private boolean isAtAssignedSpot(AbstractRecruitEntity recruit) {
        Vec3 assigned = recruit.getHoldPos();
        if (assigned == null) return recruit.isInFormation;
        double dx = recruit.getX() - assigned.x;
        double dz = recruit.getZ() - assigned.z;
        return dx * dx + dz * dz <= FORMATION_SLOT_TOLERANCE * FORMATION_SLOT_TOLERANCE;
    }

    private void issueMove(ServerPlayer owner, ServerLevel level, List<AbstractRecruitEntity> recruits,
                           ActiveOrder order, BlockPos surface, boolean newWaypoint) {
        if (newWaypoint) {
            standDown(recruits);
            RecruitCommanderUtil.setRecruitsClearTargets(recruits);
            order.facingYaw = facingYaw(centerOf(recruits), Vec3.atCenterOf(surface), order.facingYaw);
        }

        if (order.formation > 0) {
            applyFormationFacing(owner, order, recruits, Vec3.atCenterOf(surface));
        } else {
            RecruitCommanderUtil.setRecruitsMove(recruits, surface);
        }

        order.commandIssued = true;
        order.issuedWaypoint = surface.immutable();
        order.ticksSinceIssue = 0;
    }

    private void applyFormationFacing(ServerPlayer owner, ActiveOrder order,
                                      List<AbstractRecruitEntity> recruits, Vec3 center) {
        float previousYaw = owner.getYRot();
        float previousHeadYaw = owner.getYHeadRot();
        owner.setYRot(order.facingYaw);
        owner.setYHeadRot(order.facingYaw);
        try {
            CommandEvents.applyFormation(order.formation, recruits, owner, center, order.tight,
                    order.holdFormation);
        } finally {
            owner.setYRot(previousYaw);
            owner.setYHeadRot(previousHeadYaw);
        }
    }

    private float facingYaw(Vec3 from, Vec3 to, float fallback) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        if (dx * dx + dz * dz < 4.0D) return fallback;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private List<SelectionTarget> buildSelections(ServerPlayer player, RecruitsGroup group,
                                                  Map<UUID, List<UUID>> crews) {
        if (group == null || group.members == null || group.members.isEmpty()) return List.of();

        ServerLevel level = player.serverLevel();
        ResourceLocation dimension = level.dimension().location();
        List<AbstractRecruitEntity> recruits = resolveGroupMembers(level, group);
        if (recruits.isEmpty()) return List.of();

        List<UUID> memberIds = recruits.stream()
                .map(Entity::getUUID)
                .sorted()
                .toList();
        return List.of(new SelectionTarget(
                group.getUUID(),
                group.getUUID(),
                group.getPlayerUUID(),
                dimension,
                anchorOf(recruits),
                memberIds,
                unitSnapshots(player, recruits, crews)
        ));
    }

    private int healthPercent(AbstractRecruitEntity recruit) {
        float max = recruit.getMaxHealth();
        if (max <= 0.0F) return 0;
        return Mth.clamp(Math.round(recruit.getHealth() / max * 100.0F), 0, 100);
    }

    private boolean canVolley(AbstractRecruitEntity recruit) {
        return recruit instanceof IStrategicFire
                && RecruitRangedBowAttackGoal.isHoldingBow(recruit);
    }

    private CommandUnitType dominantType(AbstractRecruitEntity recruit) {
        if (CommandUnitType.CAVALRY.matches(recruit)) return CommandUnitType.CAVALRY;
        if (CommandUnitType.RANGED.matches(recruit)) return CommandUnitType.RANGED;
        return CommandUnitType.INFANTRY;
    }

    private List<CommandUnitSnapshot> unitSnapshots(ServerPlayer player,
                                                    List<AbstractRecruitEntity> recruits,
                                                    Map<UUID, List<UUID>> crews) {
        return recruits.stream()
                .filter(recruit -> !aboardAnObject(player, recruit, crews))
                .map(recruit -> new CommandUnitSnapshot(recruit.getUUID(), recruit.blockPosition(),
                        dominantType(recruit), healthPercent(recruit),
                        recruit.getState(), recruit.getShouldBlock(), canVolley(recruit),
                        recruit.getShouldRanged()))
                .toList();
    }

    private boolean aboardAnObject(ServerPlayer player, AbstractRecruitEntity recruit,
                                   Map<UUID, List<UUID>> crews) {
        Entity vehicle = recruit.getVehicle();
        if (vehicle == null) return false;

        Map<UUID, MapObjectSnapshot> visible = visibleObjects.get(player.getUUID());
        if (visible == null || visible.isEmpty()) return false;

        for (Entity ridden = vehicle; ridden != null; ridden = ridden.getVehicle()) {
            if (!visible.containsKey(ridden.getUUID())) continue;

            if (crews != null && recruit.isEffectedByCommand(player.getUUID())) {
                crews.computeIfAbsent(ridden.getUUID(), ignored -> new ArrayList<>())
                        .add(recruit.getUUID());
            }
            return true;
        }
        return false;
    }

    private record Watcher(Vec3 position, double range) {
    }

    private List<CommandUnitSnapshot> seenBy(ServerPlayer player, List<Watcher> watchers, SelectionTarget selection) {
        if (Objects.equals(selection.ownerId, player.getUUID())) return selection.units;

        boolean scouting = RecruitsRTSCommandServerConfig.SCOUTING_ENABLED.get();
        if (scouting && watchers.isEmpty()) return List.of();

        boolean tellHealth = RecruitsRTSCommandServerConfig.STRANGER_HEALTH_VISIBLE.get();
        List<CommandUnitSnapshot> seen = new ArrayList<>();
        for (CommandUnitSnapshot unit : selection.units) {
            if (scouting && !withinSight(unit, watchers)) continue;
            seen.add(new CommandUnitSnapshot(unit.unitId(), unit.position(), unit.type(),
                    tellHealth ? unit.health() : HEALTH_UNKNOWN,
                    CommandUnitSnapshot.AGGRO_UNKNOWN, false, false, false));
        }
        return seen;
    }

    private boolean withinSight(CommandUnitSnapshot unit, List<Watcher> watchers) {
        return withinSight(unit.position(), watchers);
    }

    private boolean withinSight(BlockPos position, List<Watcher> watchers) {
        for (Watcher watcher : watchers) {
            double dx = watcher.position().x - (position.getX() + 0.5D);
            double dz = watcher.position().z - (position.getZ() + 0.5D);
            if (dx * dx + dz * dz <= watcher.range() * watcher.range()) return true;
        }
        return false;
    }

    private List<Watcher> watchPosts(ServerPlayer player) {
        double base = RecruitsRTSCommandServerConfig.SIGHT_RANGE_BLOCKS.get();
        List<Watcher> posts = new ArrayList<>();
        posts.add(new Watcher(player.position(), base));
        if (!RecruitsRTSCommandServerConfig.SCOUTING_ENABLED.get()) return posts;

        ServerLevel level = player.serverLevel();
        for (RecruitsGroup group : getAllGroups(player)) {
            if (!Objects.equals(group.getPlayerUUID(), player.getUUID())) continue;
            for (UUID memberId : group.members) {
                if (level.getEntity(memberId) instanceof AbstractRecruitEntity recruit && recruit.isAlive()) {
                    posts.add(new Watcher(recruit.position(), base * sightFactor(recruit)));
                }
            }
        }
        return posts;
    }

    private double sightFactor(AbstractRecruitEntity recruit) {
        if (recruit instanceof ScoutEntity) return SCOUT_SIGHT;
        if (CommandUnitType.CAVALRY.matches(recruit)) return MOUNTED_SIGHT;
        return 1.0D;
    }

    private Vec3 centerOfSnapshots(List<CommandUnitSnapshot> units) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (CommandUnitSnapshot unit : units) {
            x += unit.position().getX();
            y += unit.position().getY();
            z += unit.position().getZ();
        }
        return new Vec3(x / units.size(), y / units.size(), z / units.size());
    }

    private SelectionTarget resolveClientSelection(ServerPlayer player, UUID selectionId, List<UUID> members) {
        if (members == null || members.isEmpty()) return null;

        ServerLevel level = player.serverLevel();
        ResourceLocation dimension = level.dimension().location();
        Set<UUID> owned = new LinkedHashSet<>();
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        for (UUID memberId : members) {
            if (level.getEntity(memberId) instanceof AbstractRecruitEntity recruit
                    && recruit.isEffectedByCommand(player.getUUID())
                    && owned.add(memberId)) {
                recruits.add(recruit);
            }
        }
        if (owned.isEmpty()) return null;

        List<UUID> ownedMembers = List.copyOf(owned);
        return new SelectionTarget(selectionId, groupIdOf(player, ownedMembers), player.getUUID(), dimension,
                BlockPos.containing(centerOf(recruits)), ownedMembers, unitSnapshots(player, recruits, null));
    }

    private UUID groupIdOf(ServerPlayer player, List<UUID> members) {
        for (RecruitsGroup group : getAllGroups(player)) {
            for (UUID memberId : members) {
                if (group.members.contains(memberId)) {
                    return group.getUUID();
                }
            }
        }
        return null;
    }

    private SelectionTarget resolveOwnedSelection(ServerPlayer player, UUID selectionId) {
        SelectionTarget selection = resolveSelection(player, selectionId);
        if (selection == null || !player.getUUID().equals(selection.ownerId)) return null;
        return resolveClientSelection(player, selectionId, selection.memberIds);
    }

    private SelectionTarget resolveSelection(ServerPlayer player, UUID selectionId) {
        Map<UUID, SelectionTarget> index = playerSelections.get(player.getUUID());
        if (index == null || !index.containsKey(selectionId)) {
            buildSnapshots(player);
            index = playerSelections.get(player.getUUID());
        }
        return index == null ? null : index.get(selectionId);
    }

    private boolean canCommand(ServerPlayer player, SelectionTarget selection) {
        if (!Objects.equals(player.serverLevel().dimension().location(), selection.dimension)) return false;
        double radius = RecruitsRTSCommandServerConfig.COMMAND_RADIUS_BLOCKS.get();
        return horizontalDistSqr(player.blockPosition(), selection.anchor) <= radius * radius;
    }

    @Nullable
    private BlockPos centerOfNamed(List<CommandUnitSnapshot> units, Set<UUID> memberIds) {
        long x = 0L;
        long y = 0L;
        long z = 0L;
        int count = 0;
        for (CommandUnitSnapshot unit : units) {
            if (!memberIds.contains(unit.unitId())) continue;
            x += unit.position().getX();
            y += unit.position().getY();
            z += unit.position().getZ();
            count++;
        }
        if (count == 0) return null;
        return new BlockPos((int) (x / count), (int) (y / count), (int) (z / count));
    }

    private List<AbstractRecruitEntity> resolveGroupMembers(ServerLevel level, RecruitsGroup group) {
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        for (UUID memberId : group.members) {
            Entity entity = level.getEntity(memberId);
            if (entity instanceof AbstractRecruitEntity recruit && recruit.isAlive()) {
                recruits.add(recruit);
            }
        }
        return recruits;
    }

    private List<AbstractRecruitEntity> resolveMembers(ServerLevel level, Iterable<UUID> memberIds, UUID ownerId) {
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        for (UUID memberId : memberIds) {
            Entity entity = level.getEntity(memberId);
            if (entity instanceof AbstractRecruitEntity recruit && recruit.isEffectedByCommand(ownerId)) {
                recruits.add(recruit);
            }
        }
        return recruits;
    }

    private List<LivingEntity> resolveLivingMembers(ServerLevel level, Iterable<UUID> memberIds) {
        List<LivingEntity> entities = new ArrayList<>();
        for (UUID memberId : memberIds) {
            Entity entity = level.getEntity(memberId);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                entities.add(living);
            }
        }
        return entities;
    }

    private List<RecruitsGroup> getAllGroups(ServerPlayer player) {
        MinecraftServer server = player.serverLevel().getServer();
        ServerLevel overworld = server.overworld();
        if (overworld == null) return List.of();

        List<RecruitsGroup> allGroups = RecruitsGroupsSaveData.get(overworld).getAllGroups();
        List<RecruitsGroup> validGroups = new ArrayList<>(allGroups.size());
        for (RecruitsGroup group : allGroups) {
            if (group != null && !group.removed && !group.isDisabled()) validGroups.add(group);
        }
        return validGroups;
    }

    private void standDown(List<AbstractRecruitEntity> recruits) {
        for (AbstractRecruitEntity recruit : recruits) {
            if (recruit instanceof IStrategicFire strategicFire) {
                strategicFire.setShouldStrategicFire(false);
            }
        }
    }

    private void detachMembersFromOrders(UUID ownerId, ResourceLocation dimension,
                                         Collection<UUID> members) {
        Set<UUID> affected = members instanceof Set<UUID> set ? set : new HashSet<>(members);
        var iterator = orders.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<OrderKey, ActiveOrder> entry = iterator.next();
            ActiveOrder order = entry.getValue();
            if (!Objects.equals(ownerId, order.ownerId)
                    || !Objects.equals(dimension, entry.getKey().dimension)
                    || !intersects(order.memberIds, affected)) {
                continue;
            }
            order.memberIds.removeAll(affected);
            if (order.memberIds.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private OrderKey unusedOrderKey(UUID preferred, ResourceLocation dimension) {
        OrderKey key = new OrderKey(preferred, dimension);
        while (orders.containsKey(key)) {
            key = new OrderKey(UUID.randomUUID(), dimension);
        }
        return key;
    }

    private void applyAttack(ServerLevel level, List<AbstractRecruitEntity> attackers,
                             List<LivingEntity> targets) {
        Map<UUID, LivingEntity> assignment = assignTargets(attackers, targets);
        for (AbstractRecruitEntity recruit : attackers) {
            LivingEntity victim = assignment.get(recruit.getUUID());
            if (victim == null) continue;
            recruit.setTarget(victim);
            recruit.setAggroState(1);
            if (isRangedLike(recruit)) {
                recruit.setShouldRanged(true);
                holdShootingPosition(recruit, victim);
            } else {
                RecruitCommanderUtil.setRecruitsMove(List.of(recruit),
                        FormationUtils.getPositionOrSurface(level, victim.blockPosition()));
            }
        }

    }

    private void holdShootingPosition(AbstractRecruitEntity shooter, LivingEntity target) {
        double reach = shooter instanceof CrossBowmanEntity
                ? Math.sqrt(3500.0D)
                : shooter instanceof BowmanEntity ? 44.0D : 0.0D;
        if (reach <= 0.0D) return;

        double holdRange = reach * 0.75D;
        Vec3 toTarget = target.position().subtract(shooter.position());
        double distance = toTarget.horizontalDistance();

        Vec3 stand = distance <= holdRange
                ? shooter.position()
                : shooter.position().add(toTarget.multiply(1 / distance, 0, 1 / distance).scale(holdRange));

        shooter.setShouldMovePos(false);
        shooter.setHoldPos(stand);
        shooter.setFollowState(3);
    }

    private Map<UUID, LivingEntity> assignTargets(List<AbstractRecruitEntity> attackers,
                                                  List<LivingEntity> targets) {
        Map<UUID, LivingEntity> assignment = new HashMap<>();
        if (attackers.isEmpty() || targets.isEmpty()) return assignment;

        List<AbstractRecruitEntity> ordered = new ArrayList<>(attackers);
        ordered.sort(Comparator.comparing(recruit -> recruit.getUUID().toString()));
        int share = (attackers.size() + targets.size() - 1) / targets.size();
        Map<UUID, Integer> load = new HashMap<>();

        for (AbstractRecruitEntity recruit : ordered) {
            LivingEntity best = null;
            double bestSqr = Double.MAX_VALUE;
            LivingEntity closest = null;
            double closestSqr = Double.MAX_VALUE;
            for (LivingEntity target : targets) {
                double distanceSqr = recruit.distanceToSqr(target);
                if (distanceSqr < closestSqr) {
                    closestSqr = distanceSqr;
                    closest = target;
                }
                if (load.getOrDefault(target.getUUID(), 0) >= share) continue;
                if (distanceSqr < bestSqr) {
                    bestSqr = distanceSqr;
                    best = target;
                }
            }

            LivingEntity chosen = best != null ? best : closest;
            if (chosen == null) continue;
            assignment.put(recruit.getUUID(), chosen);
            load.merge(chosen.getUUID(), 1, Integer::sum);
        }
        return assignment;
    }

    private LivingEntity nearestTarget(AbstractRecruitEntity recruit, List<LivingEntity> targets) {
        LivingEntity nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity target : targets) {
            double distance = recruit.distanceToSqr(target);
            if (distance < bestDistance) {
                nearest = target;
                bestDistance = distance;
            }
        }
        return nearest;
    }

    private BlockPos anchorOf(List<AbstractRecruitEntity> recruits) {
        Vec3 center = centerOf(recruits);
        return BlockPos.containing(center);
    }

    private Vec3 centerOf(List<AbstractRecruitEntity> recruits) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (AbstractRecruitEntity recruit : recruits) {
            Vec3 position = recruit.position();
            x += position.x;
            y += position.y;
            z += position.z;
        }
        double count = Math.max(1, recruits.size());
        return new Vec3(x / count, y / count, z / count);
    }

    private double distanceSqrToTarget(List<AbstractRecruitEntity> recruits, BlockPos target) {
        Vec3 center = centerOf(recruits);
        double dx = center.x - (target.getX() + 0.5D);
        double dz = center.z - (target.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static boolean isInCombat(AbstractRecruitEntity recruit) {
        return recruit.getTarget() != null || recruit.getLastHurtByMob() != null;
    }

    private static boolean isRangedLike(AbstractRecruitEntity recruit) {
        return recruit instanceof IRangedRecruit || recruit instanceof NomadEntity;
    }

    private static double horizontalDistSqr(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return (double) (dx * dx + dz * dz);
    }

    private static boolean intersects(Iterable<UUID> left, Iterable<UUID> right) {
        Set<UUID> leftSet = new HashSet<>();
        for (UUID id : left) leftSet.add(id);
        for (UUID id : right) {
            if (leftSet.contains(id)) return true;
        }
        return false;
    }

    private static ServerLevel serverLevel(MinecraftServer server, ResourceLocation dimension) {
        if (server == null || dimension == null) return null;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimension);
        return server.getLevel(key);
    }

    private record OrderKey(UUID selectionId, ResourceLocation dimension) {
    }

    private static final class ActiveOrder {
        private UUID ownerId;
        private UUID rootGroupId;
        private ResourceLocation dimension;
        private final Set<UUID> memberIds = new LinkedHashSet<>();
        private final Deque<BlockPos> waypoints = new ArrayDeque<>();
        private boolean commandIssued;
        private BlockPos issuedWaypoint;
        private int ticksSinceIssue;
        private int formation;
        private float facingYaw;
        private UUID pursuing;
        private UUID pursuingObject;
        private boolean tight;
        private boolean holdFormation;
        private Vec3 lastCenter;
        private int stagnantTicks;
        private int stallReissues;
        private double bestDistanceSqr = Double.MAX_VALUE;

        private ActiveOrder(UUID ownerId, UUID rootGroupId, List<UUID> members, ResourceLocation dimension) {
            this.ownerId = ownerId;
            this.rootGroupId = rootGroupId;
            this.dimension = dimension;
            this.memberIds.addAll(members);
        }

        private ActiveOrder copyMarchFor(Collection<UUID> members) {
            ActiveOrder copy = new ActiveOrder(ownerId, rootGroupId, List.copyOf(members), dimension);
            copy.waypoints.addAll(waypoints);
            copy.formation = formation;
            copy.facingYaw = facingYaw;
            copy.tight = tight;
            copy.holdFormation = holdFormation;
            copy.resetIssueState();
            return copy;
        }

        private void resetIssueState() {
            commandIssued = false;
            issuedWaypoint = null;
            ticksSinceIssue = 0;
            stagnantTicks = 0;
            stallReissues = 0;
            lastCenter = null;
            bestDistanceSqr = Double.MAX_VALUE;
        }
    }

    private record SelectionTarget(
            UUID selectionId,
            UUID rootGroupId,
            UUID ownerId,
            ResourceLocation dimension,
            BlockPos anchor,
            List<UUID> memberIds,
            List<CommandUnitSnapshot> units
    ) {
    }
}
