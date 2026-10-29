package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandRouteQueueTest {
    @Test
    void limitsLengthWithoutDiscardingExistingWaypoints() {
        CommandRouteQueue queue = new CommandRouteQueue();
        for (int i = 0; i < CommandLimits.MAX_WAYPOINTS; i++) {
            assertTrue(queue.append(new BlockPos(i, 64, 0)));
        }
        assertFalse(queue.append(new BlockPos(999, 64, 0)));
        assertEquals(CommandLimits.MAX_WAYPOINTS, queue.snapshot().size());
        assertEquals(new BlockPos(0, 64, 0), queue.peekFirst());
    }

    @Test
    void rejectsDuplicatesAndCopiesSplitMarchIndependently() {
        CommandRouteQueue original = new CommandRouteQueue();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(1, 64, 2);
        assertTrue(original.append(mutable));
        assertFalse(original.append(mutable));
        mutable.set(20, 30, 40);
        assertEquals(new BlockPos(1, 64, 2), original.peekFirst());

        CommandRouteQueue split = new CommandRouteQueue();
        split.copyFrom(original);
        split.pollFirst();
        assertTrue(split.isEmpty());
        assertFalse(original.isEmpty());
    }
}
