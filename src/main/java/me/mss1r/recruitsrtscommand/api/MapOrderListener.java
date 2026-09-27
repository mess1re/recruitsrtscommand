package me.mss1r.recruitsrtscommand.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/** Receives map orders that are not handled by Villager Recruits itself. */
public interface MapOrderListener {
    default void areaFireOrdered(ServerPlayer commander, List<UUID> members, MapFireZone zone) {
    }

    default void areaFireCleared(ServerPlayer commander, List<UUID> members) {
    }

    /** A fire zone in block coordinates. */
    record MapFireZone(BlockPos center, int radiusX, int radiusZ, FireZoneShape shape) {
        public MapFireZone {
            shape = shape == null ? FireZoneShape.CIRCLE : shape;
        }

        public boolean contains(double offsetX, double offsetZ) {
            return shape.contains(offsetX, offsetZ, radiusX, radiusZ);
        }
    }
}
