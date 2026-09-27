package me.mss1r.recruitsrtscommand.client.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MapPositionSmoothing {
    private static final double SNAP_DISTANCE_SQR = 64.0D * 64.0D;
    private static final long MIN_INTERVAL_MILLIS = 50L;
    private static final long MAX_INTERVAL_MILLIS = 1500L;
    private static final long DEFAULT_INTERVAL_MILLIS = 500L;

    private static final Map<UUID, Track> TRACKS = new HashMap<>();

    private MapPositionSmoothing() {
    }

    public static void observe(UUID id, double x, double z, long now) {
        if (id == null) return;

        Track track = TRACKS.get(id);
        if (track == null) {
            TRACKS.put(id, new Track(x, z, now));
            return;
        }
        track.retarget(x, z, now);
    }

    public static double x(UUID id, double fallback) {
        Track track = TRACKS.get(id);
        return track == null ? fallback : track.x(System.currentTimeMillis());
    }

    public static double z(UUID id, double fallback) {
        Track track = TRACKS.get(id);
        return track == null ? fallback : track.z(System.currentTimeMillis());
    }

    public static void retain(Set<UUID> living) {
        TRACKS.keySet().retainAll(living);
    }

    public static void clear() {
        TRACKS.clear();
    }

    private static final class Track {
        private double fromX;
        private double fromZ;
        private double toX;
        private double toZ;
        private long startMillis;
        private long durationMillis = DEFAULT_INTERVAL_MILLIS;
        private long lastObservedMillis;

        private Track(double x, double z, long now) {
            this.fromX = x;
            this.fromZ = z;
            this.toX = x;
            this.toZ = z;
            this.startMillis = now;
            this.lastObservedMillis = now;
        }

        private void retarget(double x, double z, long now) {
            long interval = now - lastObservedMillis;
            lastObservedMillis = now;
            if (interval >= MIN_INTERVAL_MILLIS && interval <= MAX_INTERVAL_MILLIS) {
                durationMillis = interval;
            }

            double dx = x - toX;
            double dz = z - toZ;
            if (dx * dx + dz * dz > SNAP_DISTANCE_SQR) {
                fromX = x;
                fromZ = z;
            } else {
                fromX = x(now);
                fromZ = z(now);
            }
            toX = x;
            toZ = z;
            startMillis = now;
        }

        private double progress(long now) {
            if (durationMillis <= 0L) return 1.0D;
            double t = (double) (now - startMillis) / (double) durationMillis;
            return t <= 0.0D ? 0.0D : Math.min(t, 1.0D);
        }

        private double x(long now) {
            return fromX + (toX - fromX) * progress(now);
        }

        private double z(long now) {
            return fromZ + (toZ - fromZ) * progress(now);
        }
    }
}
