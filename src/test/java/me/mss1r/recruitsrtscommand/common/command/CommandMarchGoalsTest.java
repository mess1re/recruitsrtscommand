package me.mss1r.recruitsrtscommand.common.command;

import com.talhanation.recruits.entities.ai.HorsemanAttackAI;
import com.talhanation.recruits.entities.ai.RecruitMeleeAttackGoal;
import com.talhanation.recruits.entities.ai.RecruitMoveTowardsTargetGoal;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CommandMarchGoalsTest {
    @Test
    void movementAndFormationWinOverCombatUntilTheOrderEnds() {
        for (EnumSet<Goal.Flag> flags : List.of(EnumSet.noneOf(Goal.Flag.class),
                EnumSet.of(Goal.Flag.MOVE), EnumSet.of(Goal.Flag.LOOK))) {
            AtomicBoolean marching = new AtomicBoolean(false);
            AtomicReference<String> navigation = new AtomicReference<>();
            ProbeGoal combat = new ProbeGoal(navigation, "enemy", flags);
            ProbeGoal move = new ProbeGoal(navigation, "waypoint", EnumSet.of(Goal.Flag.MOVE));
            move.enabled = false;
            GoalSelector selector = selector();
            selector.addGoal(1, combat);
            selector.addGoal(3, move);
            selector.tick();
            assertEquals("enemy", navigation.get());

            marching.set(true);
            move.enabled = true;
            CommandMarchGoals.install(selector, marching::get, goal -> goal == combat);
            selector.tick();
            assertEquals("waypoint", navigation.get());
            assertEquals(1, combat.stops);
            assertFalse(selector.getRunningGoals().anyMatch(goal -> goal.getGoal() == combat));

            marching.set(false);
            move.enabled = false;
            selector.tick();
            assertEquals("enemy", navigation.get());
            assertEquals(2, combat.starts);
        }
    }

    @Test
    void aRunningCombatGoalCannotWriteNavigationBeforeTheNextSelectorCleanup() {
        AtomicBoolean marching = new AtomicBoolean(false);
        AtomicReference<String> navigation = new AtomicReference<>();
        ProbeGoal combat = new ProbeGoal(navigation, "enemy", EnumSet.noneOf(Goal.Flag.class));
        GoalSelector selector = selector();
        selector.addGoal(1, combat);
        CommandMarchGoals.install(selector, marching::get, goal -> goal == combat);
        selector.tick();
        assertEquals("enemy", navigation.get());

        navigation.set("waypoint");
        marching.set(true);
        selector.tickRunningGoals(false);
        assertEquals("waypoint", navigation.get());
        selector.tick();
        assertEquals(1, combat.stops);
    }

    @Test
    void repeatedWaypointsKeepGoalIdentityPriorityFlagsAndOrder() {
        AtomicReference<String> navigation = new AtomicReference<>();
        ProbeGoal combat = new ProbeGoal(navigation, "enemy", EnumSet.of(Goal.Flag.LOOK));
        ProbeGoal other = new ProbeGoal(navigation, "other", EnumSet.of(Goal.Flag.MOVE));
        GoalSelector selector = selector();
        selector.addGoal(2, combat);
        selector.addGoal(2, other);
        CommandMarchGoals.install(selector, () -> true, goal -> goal == combat);
        WrappedGoal wrapped = selector.getAvailableGoals().iterator().next();
        CommandMarchGoals.install(selector, () -> true, goal -> goal == combat);

        assertSame(wrapped, selector.getAvailableGoals().iterator().next());
        assertEquals(List.of(combat, other), selector.getAvailableGoals().stream().map(WrappedGoal::getGoal).toList());
        assertEquals(2, wrapped.getPriority());
        assertSame(combat.getFlags(), wrapped.getFlags());
        assertTrue(wrapped.requiresUpdateEveryTick());
        assertEquals(combat.isInterruptable(), wrapped.isInterruptable());
        selector.removeGoal(combat);
        assertEquals(1, selector.getAvailableGoals().size());
    }

    @Test
    void emergencyMovementRemainsAvailableDuringAMarch() {
        AtomicReference<String> navigation = new AtomicReference<>();
        ProbeGoal emergency = new ProbeGoal(navigation, "danger", EnumSet.of(Goal.Flag.MOVE));
        ProbeGoal combat = new ProbeGoal(navigation, "enemy", EnumSet.of(Goal.Flag.MOVE));
        GoalSelector selector = selector();
        selector.addGoal(0, emergency);
        selector.addGoal(1, combat);
        CommandMarchGoals.install(selector, () -> true, goal -> goal == combat);
        selector.tick();
        assertEquals("danger", navigation.get());
        assertEquals(0, combat.starts);
    }

    @Test
    void actualRecruitsCombatGoalsAreGuardedWithoutCallingTheirEntityCode() {
        GoalSelector selector = selector();
        List<Goal> combat = List.of(new HorsemanAttackAI(null),
                new RecruitMeleeAttackGoal(null, 1, 10), new RecruitMoveTowardsTargetGoal(null, 1, 10));
        combat.forEach(goal -> selector.addGoal(1, goal));
        CommandMarchGoals.install(selector, () -> true);
        for (WrappedGoal goal : selector.getAvailableGoals()) {
            assertFalse(goal.canUse());
            assertFalse(goal.canContinueToUse());
            assertDoesNotThrow(goal::tick);
        }
    }

    private static GoalSelector selector() {
        return new GoalSelector(() -> InactiveProfiler.INSTANCE);
    }

    private static final class ProbeGoal extends Goal {
        private final AtomicReference<String> navigation;
        private final String destination;
        private boolean enabled = true;
        private int starts;
        private int stops;

        private ProbeGoal(AtomicReference<String> navigation, String destination, EnumSet<Flag> flags) {
            this.navigation = navigation;
            this.destination = destination;
            setFlags(flags);
        }

        @Override
        public boolean canUse() {
            return enabled;
        }

        @Override
        public void start() {
            starts++;
        }

        @Override
        public void stop() {
            stops++;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            navigation.set(destination);
        }
    }
}
