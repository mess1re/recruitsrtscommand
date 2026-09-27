package me.mss1r.recruitsrtscommand.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-side integration entry point for map objects and map orders.
 */
public final class RecruitsRTSCommandApi {
    private static final List<MapObjectProvider> OBJECT_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<MapOrderListener> ORDER_LISTENERS = new CopyOnWriteArrayList<>();

    private RecruitsRTSCommandApi() {
    }

    /** Registers a provider whose objects can appear and receive actions on the map. */
    public static void registerObjectProvider(MapObjectProvider provider) {
        if (provider != null) OBJECT_PROVIDERS.add(provider);
    }

    /** Registers a listener for orders that other mods may handle. */
    public static void registerOrderListener(MapOrderListener listener) {
        if (listener != null) ORDER_LISTENERS.add(listener);
    }

    public static void areaFireOrdered(ServerPlayer commander, List<UUID> members, BlockPos center,
                                       int radiusX, int radiusZ, FireZoneShape shape) {
        MapOrderListener.MapFireZone zone =
                new MapOrderListener.MapFireZone(center, radiusX, radiusZ, shape);
        for (MapOrderListener listener : ORDER_LISTENERS) {
            try {
                listener.areaFireOrdered(commander, members, zone);
            } catch (RuntimeException failure) {
                RTSCommandApiLog.listenerFailed(listener, failure);
            }
        }
    }

    public static void areaFireCleared(ServerPlayer commander, List<UUID> members) {
        for (MapOrderListener listener : ORDER_LISTENERS) {
            try {
                listener.areaFireCleared(commander, members);
            } catch (RuntimeException failure) {
                RTSCommandApiLog.listenerFailed(listener, failure);
            }
        }
    }

    public static MapObjectSnapshot withActions(ServerPlayer commander, MapObjectSnapshot object,
                                                List<UUID> members) {
        List<MapObjectAction> actions = new ArrayList<>();
        for (MapObjectProvider provider : OBJECT_PROVIDERS) {
            try {
                List<MapObjectAction> offered = provider.actionsFor(commander, object.id(), members);
                if (offered != null) actions.addAll(offered);
            } catch (RuntimeException failure) {
                RTSCommandApiLog.providerFailed(provider, failure);
            }
        }
        return actions.isEmpty() ? object : object.withActions(actions);
    }

    public static void performObjectAction(ServerPlayer commander, UUID objectId, List<UUID> members,
                                           String actionId) {
        performObjectAction(commander, objectId, members, actionId, null);
    }

    public static void performObjectAction(ServerPlayer commander, UUID objectId, List<UUID> members,
                                           String actionId, net.minecraft.core.BlockPos target) {
        for (MapObjectProvider provider : OBJECT_PROVIDERS) {
            try {
                provider.performAt(commander, objectId, members, actionId, target);
            } catch (RuntimeException failure) {
                RTSCommandApiLog.providerFailed(provider, failure);
            }
        }
    }

    public static List<MapObjectSnapshot> collectObjects(ServerPlayer player) {
        if (player == null || OBJECT_PROVIDERS.isEmpty()) return List.of();

        List<MapObjectSnapshot> objects = new ArrayList<>();
        for (MapObjectProvider provider : OBJECT_PROVIDERS) {
            try {
                List<MapObjectSnapshot> supplied = provider.collect(player);
                if (supplied != null) objects.addAll(supplied);
            } catch (RuntimeException failure) {
                RTSCommandApiLog.providerFailed(provider, failure);
            }
        }
        return objects;
    }
}
