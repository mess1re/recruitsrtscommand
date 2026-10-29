package me.mss1r.recruitsrtscommand.common.command;

import java.util.function.Predicate;

final class CommandMarchProgress {
    static final int RETRY_INTERVAL = 100;
    static final int TIMEOUT = 900;
    private static final int MAX_RETRIES = 3;
    private static final double MIN_PROGRESS = 0.5D;
    private double bestDistance = Double.POSITIVE_INFINITY;
    private int stagnantTicks;
    private int retries;

    void update(double distance) {
        if (distance <= bestDistance - MIN_PROGRESS) {
            bestDistance = distance;
            stagnantTicks = 0;
            retries = 0;
        } else {
            stagnantTicks++;
        }
    }

    boolean stalled() {
        return stagnantTicks >= RETRY_INTERVAL;
    }

    boolean shouldRetry() {
        return retries < MAX_RETRIES && stagnantTicks >= RETRY_INTERVAL * (retries + 1);
    }

    void retried() {
        retries++;
    }

    boolean timedOut() {
        return stagnantTicks >= TIMEOUT;
    }

    static boolean enoughArrived(int arrived, int total) {
        return total > 0 && arrived >= (int) Math.ceil(total * 0.8D);
    }

    static <T> boolean enoughArrived(Iterable<T> members, Predicate<T> atDestination) {
        int total = 0;
        int arrived = 0;
        for (T member : members) {
            total++;
            if (atDestination.test(member)) arrived++;
        }
        return enoughArrived(arrived, total);
    }

    void reset() {
        bestDistance = Double.POSITIVE_INFINITY;
        stagnantTicks = 0;
        retries = 0;
    }
}
