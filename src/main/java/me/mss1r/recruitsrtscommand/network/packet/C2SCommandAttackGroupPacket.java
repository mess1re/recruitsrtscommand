package me.mss1r.recruitsrtscommand.network.packet;

import me.mss1r.recruitsrtscommand.common.command.MapCommandOrderService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2SCommandAttackGroupPacket(UUID attackerSelectionId, java.util.List<UUID> attackers,
                                          UUID targetSelectionId) {
    public static C2SCommandAttackGroupPacket decode(FriendlyByteBuf buf) {
        return new C2SCommandAttackGroupPacket(buf.readUUID(), CommandPacketCodecs.readMembers(buf),
                buf.readUUID());
    }

    public static void encode(C2SCommandAttackGroupPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.attackerSelectionId);
        CommandPacketCodecs.writeMembers(buf, packet.attackers);
        buf.writeUUID(packet.targetSelectionId);
    }

    public static void handle(C2SCommandAttackGroupPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                MapCommandOrderService.INSTANCE.submitAttackGroupOrder(
                        player, packet.attackerSelectionId, packet.attackers, packet.targetSelectionId);
            }
        });
        context.setPacketHandled(true);
    }
}

