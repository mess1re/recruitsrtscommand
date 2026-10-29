package me.mss1r.recruitsrtscommand.common.command;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.BowmanEntity;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import com.talhanation.recruits.util.FormationUtils;
import com.talhanation.recruits.util.RecruitCommanderUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.mss1r.recruitsrtscommand.common.command.CommandUnits.*;

final class CommandCombat {
    private CommandCombat() {
    }

    static void applyAttack(ServerLevel level, List<AbstractRecruitEntity> attackers,
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

    static void holdShootingPosition(AbstractRecruitEntity shooter, LivingEntity target) {
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

    static Map<UUID, LivingEntity> assignTargets(List<AbstractRecruitEntity> attackers,
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
}
