package me.mss1r.recruitsrtscommand.client;

import com.talhanation.recruits.client.gui.worldmap.WorldMapScreen;
import me.mss1r.recruitsrtscommand.api.WorldMapOverlayRegistry;
import me.mss1r.recruitsrtscommand.client.command.MapCommandClientState;
import me.mss1r.recruitsrtscommand.client.command.MapCommandNetworkClient;
import me.mss1r.recruitsrtscommand.client.gui.worldmap.command.WorldMapCommandOverlay;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class RecruitsRTSCommandClientEvents {
    private static final int COMMAND_SYNC_INTERVAL_TICKS = 10;
    private int commandSyncTickCounter = 0;

    public RecruitsRTSCommandClientEvents() {
        WorldMapOverlayRegistry.register(WorldMapCommandOverlay.INSTANCE);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        boolean mapOpen = minecraft.screen instanceof WorldMapScreen;
        if (mapOpen && (commandSyncTickCounter++ % COMMAND_SYNC_INTERVAL_TICKS) == 0) {
            MapCommandNetworkClient.requestSync();
        } else if (!mapOpen) {
            commandSyncTickCounter = 0;
        }
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            MapCommandClientState.clear();
        }
    }
}
