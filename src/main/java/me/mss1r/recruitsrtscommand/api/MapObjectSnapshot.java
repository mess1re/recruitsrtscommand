package me.mss1r.recruitsrtscommand.api;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Immutable map-facing state of an object supplied by another mod. */
public record MapObjectSnapshot(
        UUID id,
        MapIcon icon,
        @Nullable UUID ownerId,
        BlockPos position,
        ResourceLocation dimension,
        int condition,
        int pipColor,
        List<Component> lines,
        List<MapObjectAction> actions,
        List<BlockPos> route,
        List<UUID> crew,
        Map<MapOrder, String> orders
) {
    public MapObjectSnapshot(UUID id, MapIcon icon, @Nullable UUID ownerId, BlockPos position,
                             ResourceLocation dimension, int condition, int pipColor,
                             List<Component> lines, List<MapObjectAction> actions) {
        this(id, icon, ownerId, position, dimension, condition, pipColor, lines, actions, List.of(),
                List.of(), Map.of());
    }

    public boolean takesMoveOrders() {
        return orders.containsKey(MapOrder.MOVE);
    }

    @Nullable
    public String orderAction(MapOrder order) {
        return orders.get(order);
    }

    /** Condition value used when an object does not expose health or durability. */
    public static final int CONDITION_UNKNOWN = 255;
    /** Pip color value used to hide the owner marker. */
    public static final int NO_PIP = 0;

    public MapObjectSnapshot {
        route = List.copyOf(route == null ? List.of() : route);
        lines = List.copyOf(lines == null ? List.of() : lines);
        actions = List.copyOf(actions == null ? List.of() : actions);
        crew = List.copyOf(crew == null ? List.of() : crew);
        orders = Map.copyOf(orders == null ? Map.of() : orders);
    }

    public MapObjectSnapshot withActions(List<MapObjectAction> replacement) {
        return new MapObjectSnapshot(id, icon, ownerId, position, dimension, condition, pipColor,
                lines, replacement, route, crew, orders);
    }

    public MapObjectSnapshot withCrew(List<UUID> aboard) {
        return new MapObjectSnapshot(id, icon, ownerId, position, dimension, condition, pipColor,
                lines, actions, route, aboard, orders);
    }

    public MapObjectSnapshot withoutActions() {
        return withActions(List.of());
    }
}
