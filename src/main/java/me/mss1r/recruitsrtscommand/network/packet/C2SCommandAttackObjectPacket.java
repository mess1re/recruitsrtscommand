package me.mss1r.recruitsrtscommand.network.packet;

import me.mss1r.recruitsrtscommand.common.command.MapCommandOrderService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2SCommandAttackObjectPacket(UUID attackerSelectionId, java.util.List<UUID> attackers,
                                           UUID objectId) {
    public static C2SCommandAttackObjectPacket decode(FriendlyByteBuf buf) {
        return new C2SCommandAttackObjectPacket(buf.readUUID(), CommandPacketCodecs.readMembers(buf),
                buf.readUUID());
    }

    public static void encode(C2SCommandAttackObjectPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.attackerSelectionId);
        CommandPacketCodecs.writeMembers(buf, packet.attackers);
        buf.writeUUID(packet.objectId);
    }

    public static void handle(C2SCommandAttackObjectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                MapCommandOrderService.INSTANCE.submitAttackObjectOrder(
                        player, packet.attackerSelectionId, packet.attackers, packet.objectId);
            }
        });
        context.setPacketHandled(true);
    }
}
