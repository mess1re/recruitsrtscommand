package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

final class CommandVisibility {
    record Watcher(Vec3 position, double range) {
    }

    private final UUID viewer;
    private final List<Watcher> watchers;
    private final boolean scouting;
    private final boolean tellHealth;

    CommandVisibility(UUID viewer, List<Watcher> watchers, boolean scouting, boolean tellHealth) {
        this.viewer = viewer;
        this.watchers = List.copyOf(watchers);
        this.scouting = scouting;
        this.tellHealth = tellHealth;
    }

    boolean sees(UUID owner, BlockPos position) {
        if (Objects.equals(viewer, owner) || !scouting) return true;
        for (Watcher watcher : watchers) {
            double dx = watcher.position().x - (position.getX() + 0.5D);
            double dz = watcher.position().z - (position.getZ() + 0.5D);
            if (dx * dx + dz * dz <= watcher.range() * watcher.range()) return true;
        }
        return false;
    }

    List<CommandUnitSnapshot> units(UUID owner, List<CommandUnitSnapshot> units) {
        if (Objects.equals(viewer, owner)) return units;
        List<CommandUnitSnapshot> seen = new ArrayList<>();
        for (CommandUnitSnapshot unit : units) {
            if (!sees(owner, unit.position())) continue;
            seen.add(new CommandUnitSnapshot(unit.unitId(), unit.position(), unit.type(),
                    tellHealth ? unit.health() : CommandUnitSnapshot.HEALTH_UNKNOWN,
                    CommandUnitSnapshot.AGGRO_UNKNOWN, false, false, false, unit.heading()));
        }
        return seen;
    }
}
