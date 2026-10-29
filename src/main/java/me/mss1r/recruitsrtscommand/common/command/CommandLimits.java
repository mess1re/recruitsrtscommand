package me.mss1r.recruitsrtscommand.common.command;

public final class CommandLimits {
    public static final int MAX_MEMBERS = 4096;
    public static final int MAX_WAYPOINTS = 128;
    public static final int MAX_FIRE_RADIUS = 48;
    public static final int SNAPSHOT_INTERVAL_TICKS = 5;

    private CommandLimits() {
    }

    public static boolean validFireZone(int radiusX, int radiusZ, int shape, int shapeCount) {
        return radiusX >= 0 && radiusX <= MAX_FIRE_RADIUS
                && radiusZ >= 0 && radiusZ <= MAX_FIRE_RADIUS
                && shape >= 0 && shape < shapeCount;
    }
}
