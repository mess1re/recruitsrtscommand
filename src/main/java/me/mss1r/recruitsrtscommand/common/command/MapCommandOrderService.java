package me.mss1r.recruitsrtscommand.common.command;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.IStrategicFire;
import com.talhanation.recruits.entities.NomadEntity;
import com.talhanation.recruits.util.FormationUtils;
import com.talhanation.recruits.util.RecruitCommanderUtil;
import me.mss1r.recruitsrtscommand.api.FireZoneShape;
import me.mss1r.recruitsrtscommand.api.MapObjectAction;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import me.mss1r.recruitsrtscommand.api.MapOrder;
import me.mss1r.recruitsrtscommand.api.RecruitsRTSCommandApi;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static me.mss1r.recruitsrtscommand.common.command.CommandUnits.*;
import static me.mss1r.recruitsrtscommand.common.command.CommandCombat.*;

public final class MapCommandOrderService {
    public static final MapCommandOrderService INSTANCE = new MapCommandOrderService();

    public static final int HEALTH_UNKNOWN = 255;
    private final CommandMapSnapshots snapshots = new CommandMapSnapshots();
    private final CommandOrderRuntime orders = new CommandOrderRuntime(snapshots);

    private MapCommandOrderService() {
    }

    public List<CommandOrderRoute> buildRoutes(ServerPlayer player) {
        return orders.buildRoutes(player);
    }

    public void submitMoveOrder(ServerPlayer player, UUID selectionId, BlockPos target, boolean append,
                                int formation) {
        orders.submitMoveOrder(player, selectionId, target, append, formation);
    }

    public void submitMoveOrder(ServerPlayer player, UUID selectionId, List<UUID> members, BlockPos target,
                                boolean append, int formation, boolean tight, boolean holdFormation) {
        orders.submitMoveOrder(player, selectionId, members, target, append, formation, tight, holdFormation);
    }

    public void tick(MinecraftServer server) {
        orders.tick(server);
    }

    public void clearPlayerOrders(UUID playerId) {
        if (playerId == null) return;
        orders.clearPlayerOrders(playerId);
        snapshots.clearPlayer(playerId);
    }

    public void clearAll() {
        orders.clearAll();
        snapshots.clear();
    }

    public void sendSnapshots(ServerPlayer player) {
        sendSnapshots(player, List.of());
    }

    public void sendSnapshots(ServerPlayer player, List<UUID> selected) {
        snapshots.send(player, selected, () -> buildRoutes(player));
    }

    public List<MapObjectSnapshot> buildObjects(ServerPlayer player) {
        return snapshots.buildObjects(player);
    }

    public List<CommandGroupSnapshot> buildSnapshots(ServerPlayer player) {
        return snapshots.buildSnapshots(player);
    }

    public void submitObjectAction(ServerPlayer player, UUID objectId, List<UUID> members, String actionId,
                                   BlockPos target) {
        if (player == null || objectId == null || actionId == null || actionId.isEmpty()) return;

        MapObjectSnapshot object = snapshots.currentVisibleObject(player, objectId);
        if (object == null) return;

        List<UUID> commanded = snapshots.commandableMembers(player, members);
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
        CommandSelection attacker = members == null || members.isEmpty()
                ? snapshots.resolveOwnedSelection(player, attackerSelectionId)
                : snapshots.resolveClientSelection(player, attackerSelectionId, members);
        CommandSelection target = snapshots.resolveSelection(player, targetSelectionId);
        if (attacker == null || target == null || Objects.equals(attacker.ownerId(), target.ownerId())) return;
        if (!snapshots.canCommand(player, attacker) || !Objects.equals(attacker.dimension(), target.dimension())) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), attacker.dimension());
        if (level == null) return;

        List<AbstractRecruitEntity> attackers = resolveMembers(level, attacker.memberIds(), attacker.ownerId());
        List<LivingEntity> targets = resolveLivingMembers(level, target.memberIds());
        if (attackers.isEmpty() || targets.isEmpty()) return;

        AbstractRecruitEntity judge = attackers.get(0);
        targets.removeIf(victim -> !judge.canAttack(victim));
        if (targets.isEmpty()) return;

        orders.detachMembersFromOrders(player.getUUID(), attacker.dimension(), attacker.memberIds());
        standDown(attackers);

        applyAttack(level, attackers, targets);

        orders.pursue(player, attacker, target.selectionId(), null);
    }

    public void submitAttackObjectOrder(ServerPlayer player, UUID attackerSelectionId,
                                        List<UUID> members, UUID objectId) {
        if (player == null || attackerSelectionId == null || objectId == null) return;

        MapObjectSnapshot object = snapshots.currentVisibleObject(player, objectId);
        if (object == null) return;
        if (player.getUUID().equals(object.ownerId())) return;

        CommandSelection attacker = members == null || members.isEmpty()
                ? snapshots.resolveOwnedSelection(player, attackerSelectionId)
                : snapshots.resolveClientSelection(player, attackerSelectionId, members);
        if (attacker == null || !snapshots.canCommand(player, attacker)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), attacker.dimension());
        if (level == null || !(level.getEntity(objectId) instanceof LivingEntity victim)
                || !victim.isAlive()) {
            return;
        }

        List<AbstractRecruitEntity> attackers = resolveMembers(level, attacker.memberIds(), attacker.ownerId());
        if (attackers.isEmpty()) return;

        if (!attackers.get(0).canAttack(victim)) return;

        orders.detachMembersFromOrders(player.getUUID(), attacker.dimension(), attacker.memberIds());
        standDown(attackers);
        applyAttack(level, attackers, List.of(victim));

        orders.pursue(player, attacker, null, objectId);
    }

    public void submitFaceOrder(ServerPlayer player, UUID selectionId, List<UUID> members, BlockPos lookAt,
                                int formation, boolean tight, boolean holdFormation) {
        if (player == null || selectionId == null || lookAt == null || formation <= 0) return;

        CommandSelection selection = members == null || members.isEmpty()
                ? snapshots.resolveOwnedSelection(player, selectionId)
                : snapshots.resolveClientSelection(player, selectionId, members);
        if (selection == null || !snapshots.canCommand(player, selection)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), selection.dimension());
        if (level == null) return;

        List<AbstractRecruitEntity> recruits = resolveMembers(level, selection.memberIds(), selection.ownerId());
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
        if (!CommandLimits.validFireZone(radiusX, radiusZ, shapeOrdinal, FireZoneShape.values().length)) return;

        CommandSelection selection = members == null || members.isEmpty()
                ? snapshots.resolveOwnedSelection(player, selectionId)
                : snapshots.resolveClientSelection(player, selectionId, members);
        if (selection == null || !snapshots.canCommand(player, selection)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), selection.dimension());
        if (level == null || !level.getWorldBorder().isWithinBounds(target)) return;

        orders.detachMembersFromOrders(player.getUUID(), selection.dimension(), selection.memberIds());
        BlockPos surfaceTarget = FormationUtils.getPositionOrSurface(level, target);
        List<AbstractRecruitEntity> recruits = resolveMembers(level, selection.memberIds(), selection.ownerId());
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

        RecruitsRTSCommandApi.areaFireOrdered(player, List.copyOf(selection.memberIds()), surfaceTarget,
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

        CommandSelection selection = members == null || members.isEmpty()
                ? snapshots.resolveOwnedSelection(player, selectionId)
                : snapshots.resolveClientSelection(player, selectionId, members);
        if (selection == null || !snapshots.canCommand(player, selection)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), selection.dimension());
        if (level == null) return;

        List<AbstractRecruitEntity> recruits = resolveMembers(level, selection.memberIds(), selection.ownerId());
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

        RecruitsRTSCommandApi.areaFireCleared(player, List.copyOf(selection.memberIds()));
    }

    public void submitBehaviorOrder(ServerPlayer player, UUID selectionId, List<UUID> members, Integer aggroState,
                                    Boolean fireAtWill, Integer followState) {
        submitBehaviorOrder(player, selectionId, members, aggroState, fireAtWill, followState, null);
    }

    public void submitBehaviorOrder(ServerPlayer player, UUID selectionId, List<UUID> members, Integer aggroState,
                                    Boolean fireAtWill, Integer followState, Boolean shields) {
        if (player == null || selectionId == null) return;
        if (aggroState == null && fireAtWill == null && followState == null && shields == null) return;

        CommandSelection selection = members == null || members.isEmpty()
                ? snapshots.resolveOwnedSelection(player, selectionId)
                : snapshots.resolveClientSelection(player, selectionId, members);
        if (selection == null || !snapshots.canCommand(player, selection)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), selection.dimension());
        if (level == null) return;

        List<AbstractRecruitEntity> recruits = resolveMembers(level, selection.memberIds(), selection.ownerId());
        if (recruits.isEmpty()) return;

        if (followState != null) {
            int state = Mth.clamp(followState, 0, 5);
            orders.detachMembersFromOrders(player.getUUID(), selection.dimension(), selection.memberIds());
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
            if (state == 2) orders.detachMembersFromOrders(
                    player.getUUID(), selection.dimension(), selection.memberIds());
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


}
