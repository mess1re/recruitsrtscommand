package me.mss1r.recruitsrtscommand.common.command;

import com.talhanation.recruits.entities.ai.HorsemanAttackAI;
import com.talhanation.recruits.entities.ai.NomadAttackAI;
import com.talhanation.recruits.entities.ai.RecruitMeleeAttackGoal;
import com.talhanation.recruits.entities.ai.RecruitMoveTowardsTargetGoal;
import com.talhanation.recruits.entities.ai.RecruitRangedBowAttackGoal;
import com.talhanation.recruits.entities.ai.RecruitRangedCrossbowAttackGoal;
import com.talhanation.recruits.entities.ai.compat.RecruitRangedMusketAttackGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

final class CommandMarchGoals {
    private CommandMarchGoals() {
    }

    static void install(GoalSelector selector, BooleanSupplier marching) {
        install(selector, marching, CommandMarchGoals::controlsCombatMovement);
    }

    static void install(GoalSelector selector, BooleanSupplier marching, Predicate<Goal> combatMovement) {
        var goals = new ArrayList<>(selector.getAvailableGoals());
        boolean changed = false;
        for (int i = 0; i < goals.size(); i++) {
            WrappedGoal goal = goals.get(i);
            if (goal instanceof MarchAwareGoal || !combatMovement.test(goal.getGoal())) continue;
            goal.stop();
            goals.set(i, new MarchAwareGoal(goal, marching));
            changed = true;
        }
        if (changed) {
            // Keep goal identities, priorities and insertion order for Recruits' own goal management.
            selector.getAvailableGoals().clear();
            selector.getAvailableGoals().addAll(goals);
        }
    }

    private static boolean controlsCombatMovement(Goal goal) {
        return goal instanceof HorsemanAttackAI || goal instanceof NomadAttackAI
                || goal instanceof RecruitMeleeAttackGoal || goal instanceof RecruitMoveTowardsTargetGoal
                || goal instanceof RecruitRangedBowAttackGoal<?> || goal instanceof RecruitRangedCrossbowAttackGoal
                || goal instanceof RecruitRangedMusketAttackGoal;
    }

    private static final class MarchAwareGoal extends WrappedGoal {
        private final BooleanSupplier marching;

        private MarchAwareGoal(WrappedGoal original, BooleanSupplier marching) {
            super(original.getPriority(), original.getGoal());
            this.marching = marching;
        }

        @Override
        public boolean canUse() {
            return !marching.getAsBoolean() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !marching.getAsBoolean() && super.canContinueToUse();
        }

        @Override
        public void tick() {
            if (!marching.getAsBoolean()) super.tick();
        }
    }
}
