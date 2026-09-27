package me.mss1r.recruitsrtscommand.common.event;

import me.mss1r.recruitsrtscommand.common.command.MapCommandOrderService;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class MapCommandServerEvents {
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MapCommandOrderService.INSTANCE.tick(event.getServer());
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MapCommandOrderService.INSTANCE.clearPlayerOrders(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        MapCommandOrderService.INSTANCE.clearAll();
    }
}

