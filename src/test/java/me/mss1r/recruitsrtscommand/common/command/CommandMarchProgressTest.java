package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandMarchProgressTest {
    @Test
    void aCenterAtTheWaypointDoesNotMakeDispersedTroopsArrive() {
        List<Vec3> positions = List.of(new Vec3(-100, 0, 0), new Vec3(100, 0, 0));
        assertFalse(CommandMarchProgress.enoughArrived(positions,
                position -> CommandUnits.horizontalDistSqr(position, Vec3.ZERO) <= 25));
        assertTrue(CommandMarchProgress.enoughArrived(List.of(new Vec3(3, 200, 4)),
                position -> CommandUnits.horizontalDistSqr(position, Vec3.ZERO) <= 25));
    }

    @Test
    void bothMovementAndFormationsRequireEightyPercentAndAtLeastOneMember() {
        assertFalse(CommandMarchProgress.enoughArrived(0, 0));
        assertFalse(CommandMarchProgress.enoughArrived(0, 1));
        assertTrue(CommandMarchProgress.enoughArrived(1, 1));
        assertFalse(CommandMarchProgress.enoughArrived(6, 8));
        assertTrue(CommandMarchProgress.enoughArrived(7, 8));
        assertTrue(CommandMarchProgress.enoughArrived(8, 10));
        assertFalse(CommandMarchProgress.enoughArrived(Collections.<Vec3>emptyList(), ignored -> true));
    }

    @Test
    void circlingOrRetreatingDoesNotResetTheTimeoutAndRetriesCannotExtendIt() {
        CommandMarchProgress progress = new CommandMarchProgress();
        progress.update(30);
        int retries = 0;
        for (int tick = 1; tick <= CommandMarchProgress.TIMEOUT; tick++) {
            progress.update(tick % 2 == 0 ? 30 : 31);
            if (progress.shouldRetry()) {
                progress.retried();
                retries++;
            }
        }
        assertEquals(3, retries);
        assertTrue(progress.timedOut());
        assertTrue(progress.stalled());
        assertFalse(progress.shouldRetry());
    }

    @Test
    void smallAccumulatedAdvancesCountAsProgressAndTheNextWaypointStartsFresh() {
        CommandMarchProgress progress = new CommandMarchProgress();
        progress.update(30);
        for (int tick = 1; tick <= 2000; tick++) progress.update(30 - tick * 0.01);
        assertFalse(progress.stalled());
        assertFalse(progress.timedOut());
        progress.reset();
        progress.update(100);
        assertFalse(progress.stalled());
        for (int tick = 0; tick < CommandMarchProgress.RETRY_INTERVAL; tick++) progress.update(100);
        assertTrue(progress.shouldRetry());
        progress.retried();
        assertFalse(progress.shouldRetry());
        progress.update(99);
        assertFalse(progress.stalled());
    }
}
