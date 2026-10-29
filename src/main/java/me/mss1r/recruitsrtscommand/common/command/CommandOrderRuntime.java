package me.mss1r.recruitsrtscommand.common.command;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.util.FormationUtils;
import com.talhanation.recruits.util.RecruitCommanderUtil;
import me.mss1r.recruitsrtscommand.config.RecruitsRTSCommandServerConfig;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static me.mss1r.recruitsrtscommand.common.command.CommandUnits.*;
import static me.mss1r.recruitsrtscommand.common.command.CommandCombat.*;

final class CommandOrderRuntime {
    private static final int ORDER_STALE_TIMEOUT_TICKS = 900;
    private static final int ORDER_STALL_REISSUE_TICKS = 100;
    private static final double MOVEMENT_EPSILON_SQR = 0.02D;
    private static final double FORMATION_SLOT_TOLERANCE = 2.5D;
    private static final double FORMATION_ARRIVED_SHARE = 0.8D;
    private static final int ORDER_STALL_REISSUE_LIMIT = 3;
    private static final int FORMATION_SETTLE_TICKS = 20;
    private static final int PURSUIT_REPATH_TICKS = 20;
    private static final double PURSUIT_CONTACT = 12.0D;

    private final Map<OrderKey, ActiveOrder> orders = new HashMap<>();
    private final CommandMarchPriority marching = new CommandMarchPriority();
    private final CommandMapSnapshots snapshots;

    CommandOrderRuntime(CommandMapSnapshots snapshots) {
        this.snapshots = snapshots;
    }

    void pursue(ServerPlayer player, CommandSelection selection, UUID targetSquad, UUID targetObject) {
        ActiveOrder chase = new ActiveOrder(player.getUUID(), selection.rootGroupId(),
                selection.memberIds(), selection.dimension());
        chase.pursuing = targetSquad;
        chase.pursuingObject = targetObject;
        orders.put(unusedOrderKey(selection.selectionId(), selection.dimension()), chase);
    }

    void clearPlayerOrders(UUID playerId) {
        if (playerId == null) return;
        orders.entrySet().removeIf(entry -> playerId.equals(entry.getValue().ownerId));
        marching.clearOwner(playerId);
    }

    void clearAll() {
        orders.clear();
        marching.clear();
    }

    List<CommandOrderRoute> buildRoutes(ServerPlayer player) {
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
                    origin, order.waypoints.snapshot()));
        }
        return routes;
    }

    void submitMoveOrder(ServerPlayer player, UUID selectionId, BlockPos target, boolean append,
                                int formation) {
        submitMoveOrder(player, selectionId, null, target, append, formation, false, false);
    }

    void submitMoveOrder(ServerPlayer player, UUID selectionId, List<UUID> members, BlockPos target,
                                boolean append, int formation, boolean tight, boolean holdFormation) {
        if (player == null || selectionId == null || target == null) return;

        CommandSelection selection = members == null || members.isEmpty()
                ? snapshots.resolveOwnedSelection(player, selectionId)
                : snapshots.resolveClientSelection(player, selectionId, members);
        if (selection == null || !snapshots.canCommand(player, selection)) return;

        ServerLevel level = serverLevel(player.serverLevel().getServer(), selection.dimension());
        if (level == null || !level.getWorldBorder().isWithinBounds(target)) return;

        BlockPos surface = FormationUtils.getPositionOrSurface(level, target);
        if (append) {
            appendMoveOrder(player, selection, surface, formation, tight, holdFormation);
            return;
        }

        detachMembersFromOrders(player.getUUID(), selection.dimension(), selection.memberIds());
        OrderKey key = unusedOrderKey(selection.selectionId(), selection.dimension());
        ActiveOrder order = new ActiveOrder(
                player.getUUID(), selection.rootGroupId(), selection.memberIds(), selection.dimension());
        orders.put(key, order);
        order.ownerId = player.getUUID();
        order.rootGroupId = selection.rootGroupId();
        order.dimension = selection.dimension();
        order.memberIds.clear();
        order.memberIds.addAll(selection.memberIds());
        order.formation = Mth.clamp(formation, 0, 8);
        order.tight = tight;
        order.holdFormation = holdFormation;

        order.waypoints.clear();
        order.resetIssueState();
        order.waypoints.append(surface);
    }

    private void appendMoveOrder(ServerPlayer player, CommandSelection selection, BlockPos surface,
                                 int formation, boolean tight, boolean holdFormation) {
        Set<UUID> remaining = new LinkedHashSet<>(selection.memberIds());
        List<Map.Entry<OrderKey, ActiveOrder>> existing = new ArrayList<>(orders.entrySet());

        for (Map.Entry<OrderKey, ActiveOrder> entry : existing) {
            ActiveOrder current = entry.getValue();
            if (!player.getUUID().equals(current.ownerId)
                    || !selection.dimension().equals(entry.getKey().dimension)
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
                orders.put(new OrderKey(UUID.randomUUID(), selection.dimension()), appended);
            }
            configureMoveOrder(appended, player.getUUID(), selection.rootGroupId(),
                    selection.dimension(), formation, tight, holdFormation);
            appendWaypoint(appended, surface);
            remaining.removeAll(selectedHere);
        }

        for (Map.Entry<OrderKey, ActiveOrder> entry : existing) {
            ActiveOrder current = entry.getValue();
            if (!player.getUUID().equals(current.ownerId)
                    || !selection.dimension().equals(entry.getKey().dimension)
                    || current.pursuing == null && current.pursuingObject == null) {
                continue;
            }
            current.memberIds.removeAll(selection.memberIds());
            if (current.memberIds.isEmpty()) orders.remove(entry.getKey(), current);
        }

        if (!remaining.isEmpty()) {
            ActiveOrder order = new ActiveOrder(player.getUUID(), selection.rootGroupId(),
                    List.copyOf(remaining), selection.dimension());
            configureMoveOrder(order, player.getUUID(), selection.rootGroupId(),
                    selection.dimension(), formation, tight, holdFormation);
            appendWaypoint(order, surface);
            orders.put(unusedOrderKey(selection.selectionId(), selection.dimension()), order);
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
        order.waypoints.append(surface);
    }

    void tick(MinecraftServer server) {
        marching.clear();
        if (server == null || orders.isEmpty()) return;

        double arrivalDistance = Math.max(1.0D, RecruitsRTSCommandServerConfig.ARRIVAL_DISTANCE_BLOCKS.get());
        int repathInterval = Math.max(20, RecruitsRTSCommandServerConfig.REPATH_INTERVAL_TICKS.get());
        boolean continueInCombat = RecruitsRTSCommandServerConfig.CONTINUE_MOVE_IN_COMBAT.get();

        orders.entrySet().removeIf(entry -> tickOrder(server, entry.getKey(), entry.getValue(),
                arrivalDistance, repathInterval, continueInCombat));
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
        if (!continueInCombat && recruits.stream().anyMatch(CommandUnits::isInCombat)) return true;

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

        for (AbstractRecruitEntity recruit : recruits) {
            marching.claim(recruit.getUUID(), order.ownerId, order.dimension);
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
            CommandSelection squad = owner == null ? null : snapshots.resolveSelection(owner, order.pursuing);
            targets = squad == null ? List.of() : resolveLivingMembers(level, squad.memberIds());
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
        for (AbstractRecruitEntity recruit : recruits) {
            CommandMarchGoals.install(recruit.goalSelector, () -> recruit.isAlive()
                    && marching.contains(recruit.getUUID(), recruit.getOwnerUUID(),
                    recruit.level().dimension().location()));
        }
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

    void detachMembersFromOrders(UUID ownerId, ResourceLocation dimension,
                                         Collection<UUID> members) {
        marching.remove(ownerId, dimension, members);
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

    private double distanceSqrToTarget(List<AbstractRecruitEntity> recruits, BlockPos target) {
        Vec3 center = centerOf(recruits);
        double dx = center.x - (target.getX() + 0.5D);
        double dz = center.z - (target.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static boolean intersects(Iterable<UUID> left, Iterable<UUID> right) {
        Set<UUID> leftSet = new HashSet<>();
        for (UUID id : left) leftSet.add(id);
        for (UUID id : right) {
            if (leftSet.contains(id)) return true;
        }
        return false;
    }

    private record OrderKey(UUID selectionId, ResourceLocation dimension) {
    }

    private static final class ActiveOrder {
        private UUID ownerId;
        private UUID rootGroupId;
        private ResourceLocation dimension;
        private final Set<UUID> memberIds = new LinkedHashSet<>();
        private final CommandRouteQueue waypoints = new CommandRouteQueue();
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
            copy.waypoints.copyFrom(waypoints);
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
}
