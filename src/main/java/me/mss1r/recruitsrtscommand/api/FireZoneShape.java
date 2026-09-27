package me.mss1r.recruitsrtscommand.api;

/** Shapes supported by an area-fire order. */
public enum FireZoneShape {
    CIRCLE,
    OVAL,
    RECTANGLE;

    public FireZoneShape next() {
        FireZoneShape[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static FireZoneShape fromOrdinal(int ordinal) {
        FireZoneShape[] values = values();
        if (ordinal < 0 || ordinal >= values.length) return CIRCLE;
        return values[ordinal];
    }

    public boolean contains(double dx, double dz, double radiusX, double radiusZ) {
        if (radiusX <= 0.0D || radiusZ <= 0.0D) return false;
        return switch (this) {
            case RECTANGLE -> Math.abs(dx) <= radiusX && Math.abs(dz) <= radiusZ;
            default -> {
                double nx = dx / radiusX;
                double nz = dz / radiusZ;
                yield nx * nx + nz * nz <= 1.0D;
            }
        };
    }
}
