package me.mss1r.recruitsrtscommand.client.command;

import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandOrderRoute;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.UUID;

public final class ClientCommandPacketHandler {
    private ClientCommandPacketHandler() {
    }

    public static void handleSnapshots(List<CommandGroupSnapshot> snapshots,
                                       List<CommandOrderRoute> routes,
                                       List<me.mss1r.recruitsrtscommand.api.MapObjectSnapshot> objects) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID localPlayerId = minecraft.player == null ? null : minecraft.player.getUUID();
        MapCommandClientState.updateObjects(objects, localPlayerId);
        MapCommandClientState.updateSnapshots(snapshots, localPlayerId);
        MapCommandClientState.updateRoutes(routes);
    }
}
