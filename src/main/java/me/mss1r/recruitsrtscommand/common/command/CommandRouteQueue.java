package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

final class CommandRouteQueue {
    private final Deque<BlockPos> points = new ArrayDeque<>();

    boolean append(BlockPos point) {
        if (point == null || point.equals(points.peekLast()) || points.size() >= CommandLimits.MAX_WAYPOINTS) {
            return false;
        }
        points.addLast(point.immutable());
        return true;
    }

    boolean isEmpty() {
        return points.isEmpty();
    }

    BlockPos peekFirst() {
        return points.peekFirst();
    }

    void pollFirst() {
        points.pollFirst();
    }

    void clear() {
        points.clear();
    }

    List<BlockPos> snapshot() {
        return List.copyOf(points);
    }

    void copyFrom(CommandRouteQueue other) {
        points.clear();
        points.addAll(other.points);
    }
}
