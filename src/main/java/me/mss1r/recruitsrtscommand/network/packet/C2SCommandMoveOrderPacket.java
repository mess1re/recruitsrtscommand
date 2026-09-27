package me.mss1r.recruitsrtscommand.network.packet;

import me.mss1r.recruitsrtscommand.common.command.MapCommandOrderService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2SCommandMoveOrderPacket(
        UUID selectionId,
        java.util.List<UUID> members,
        BlockPos target,
        boolean append,
        int formation,
        boolean tight,
        boolean holdFormation
) {
    public static C2SCommandMoveOrderPacket decode(FriendlyByteBuf buf) {
        return new C2SCommandMoveOrderPacket(
                buf.readUUID(),
                CommandPacketCodecs.readMembers(buf),
                buf.readBlockPos(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public static void encode(C2SCommandMoveOrderPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.selectionId);
        CommandPacketCodecs.writeMembers(buf, packet.members);
        buf.writeBlockPos(packet.target);
        buf.writeBoolean(packet.append);
        buf.writeVarInt(packet.formation);
        buf.writeBoolean(packet.tight);
        buf.writeBoolean(packet.holdFormation);
    }

    public static void handle(C2SCommandMoveOrderPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                MapCommandOrderService.INSTANCE.submitMoveOrder(
                        player, packet.selectionId, packet.members, packet.target, packet.append,
                        packet.formation, packet.tight, packet.holdFormation);
            }
        });
        context.setPacketHandled(true);
    }
}

