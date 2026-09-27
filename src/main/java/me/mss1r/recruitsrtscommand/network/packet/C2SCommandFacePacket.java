package me.mss1r.recruitsrtscommand.network.packet;

import me.mss1r.recruitsrtscommand.common.command.MapCommandOrderService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record C2SCommandFacePacket(UUID selectionId, List<UUID> members, BlockPos lookAt,
                                   int formation, boolean tight, boolean holdFormation) {
    public static C2SCommandFacePacket decode(FriendlyByteBuf buf) {
        return new C2SCommandFacePacket(
                buf.readUUID(),
                CommandPacketCodecs.readMembers(buf),
                buf.readBlockPos(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public static void encode(C2SCommandFacePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.selectionId);
        CommandPacketCodecs.writeMembers(buf, packet.members);
        buf.writeBlockPos(packet.lookAt);
        buf.writeVarInt(packet.formation);
        buf.writeBoolean(packet.tight);
        buf.writeBoolean(packet.holdFormation);
    }

    public static void handle(C2SCommandFacePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                MapCommandOrderService.INSTANCE.submitFaceOrder(player, packet.selectionId, packet.members,
                        packet.lookAt, packet.formation, packet.tight, packet.holdFormation);
            }
        });
        context.setPacketHandled(true);
    }
}
