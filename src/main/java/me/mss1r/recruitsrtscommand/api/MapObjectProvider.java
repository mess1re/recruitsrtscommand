package me.mss1r.recruitsrtscommand.api;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Supplies server-owned objects to the tactical map and handles their actions.
 */
public interface MapObjectProvider {
    /** Returns the objects visible to the given player. */
    List<MapObjectSnapshot> collect(ServerPlayer player);

    /** Returns actions currently available for an object and selected recruits. */
    default List<MapObjectAction> actionsFor(ServerPlayer commander, UUID objectId, List<UUID> members) {
        return List.of();
    }

    /**
     * Handles an action, including an optional point selected on the map.
     * Implementations that do not use a point may override {@link #perform} instead.
     */
    default void performAt(ServerPlayer commander, UUID objectId, List<UUID> members, String actionId,
                           @Nullable net.minecraft.core.BlockPos target) {
        perform(commander, objectId, members, actionId);
    }

    /** Handles an action that does not require a map position. */
    default void perform(ServerPlayer commander, UUID objectId, List<UUID> members, String actionId) {
    }
}
