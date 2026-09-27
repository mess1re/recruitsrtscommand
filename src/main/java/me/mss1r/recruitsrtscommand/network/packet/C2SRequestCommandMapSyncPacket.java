package me.mss1r.recruitsrtscommand.network.packet;

import me.mss1r.recruitsrtscommand.common.command.MapCommandOrderService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRequestCommandMapSyncPacket(java.util.List<java.util.UUID> selected) {
    public static C2SRequestCommandMapSyncPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestCommandMapSyncPacket(CommandPacketCodecs.readMembers(buf));
    }

    public static void encode(C2SRequestCommandMapSyncPacket packet, FriendlyByteBuf buf) {
        CommandPacketCodecs.writeMembers(buf, packet.selected);
    }

    public static void handle(C2SRequestCommandMapSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) MapCommandOrderService.INSTANCE.sendSnapshots(player, packet.selected);
        });
        context.setPacketHandled(true);
    }
}

