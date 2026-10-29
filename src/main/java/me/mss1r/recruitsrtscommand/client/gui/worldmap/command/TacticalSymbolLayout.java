package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import me.mss1r.recruitsrtscommand.common.command.CommandUnitType;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitSnapshot;

import java.util.List;
import net.minecraft.util.Mth;

record TacticalSymbolLayout(double width, double height, double angle, Kind kind) {
    static final double MAX_MAP_SCALE = 1.25D;

    enum Kind { INFANTRY, RANGED, CAVALRY, MIXED }

    static TacticalSymbolLayout of(List<CommandUnitSnapshot> units, double mapScale) {
        double baseWidth = Mth.clamp(14 + Math.sqrt(units.size()) * 1.2D, 18, 30);
        double width = Math.max(8, baseWidth * Mth.clamp(mapScale / MAX_MAP_SCALE, 0, 1));
        // Local -Y is the front of the symbol; MC yaw 0 faces south (+map Y).
        return new TacticalSymbolLayout(width, width * 0.60D,
                Math.toRadians(heading(units) + 180), kind(units));
    }

    static boolean visible(double mapScale, boolean pickingIndividuals) {
        return mapScale <= MAX_MAP_SCALE && !pickingIndividuals;
    }

    boolean contains(double dx, double dy, double padding) {
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        double localX = dx * cosine + dy * sine;
        double localY = -dx * sine + dy * cosine;
        return Math.abs(localX) <= width / 2 + padding && Math.abs(localY) <= height / 2 + padding;
    }

    double bottomExtent() {
        return Math.abs(Math.sin(angle)) * width / 2 + Math.abs(Math.cos(angle)) * height / 2;
    }

    private static double heading(List<CommandUnitSnapshot> units) {
        double sine = 0;
        double cosine = 0;
        float first = 0;
        boolean known = false;
        for (var unit : units) {
            if (!Float.isFinite(unit.heading())) continue;
            if (!known) first = unit.heading();
            known = true;
            sine += Math.sin(Math.toRadians(unit.heading()));
            cosine += Math.cos(Math.toRadians(unit.heading()));
        }
        return sine * sine + cosine * cosine < 1.0E-6 ? first : Math.toDegrees(Math.atan2(sine, cosine));
    }

    private static Kind kind(List<CommandUnitSnapshot> units) {
        int infantry = 0;
        int ranged = 0;
        int cavalry = 0;
        for (var unit : units) {
            if (unit.type() == CommandUnitType.INFANTRY) infantry++;
            else if (unit.type() == CommandUnitType.RANGED) ranged++;
            else if (unit.type() == CommandUnitType.CAVALRY) cavalry++;
        }
        if (infantry > ranged && infantry > cavalry) return Kind.INFANTRY;
        if (ranged > infantry && ranged > cavalry) return Kind.RANGED;
        if (cavalry > infantry && cavalry > ranged) return Kind.CAVALRY;
        return Kind.MIXED;
    }
}
