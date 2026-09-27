package me.mss1r.recruitsrtscommand.network.packet;

import me.mss1r.recruitsrtscommand.common.command.MapCommandOrderService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2SCommandStrategicFireHoldPacket(UUID selectionId, java.util.List<UUID> members) {
    public static C2SCommandStrategicFireHoldPacket decode(FriendlyByteBuf buf) {
        return new C2SCommandStrategicFireHoldPacket(buf.readUUID(), CommandPacketCodecs.readMembers(buf));
    }

    public static void encode(C2SCommandStrategicFireHoldPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.selectionId);
        CommandPacketCodecs.writeMembers(buf, packet.members);
    }

    public static void handle(C2SCommandStrategicFireHoldPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                MapCommandOrderService.INSTANCE.submitStrategicFireHoldOrder(player, packet.selectionId, packet.members);
            }
        });
        context.setPacketHandled(true);
    }
}

