package me.mss1r.recruitsrtscommand.network.packet;

import me.mss1r.recruitsrtscommand.common.command.MapCommandOrderService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2SCommandStrategicFirePacket(UUID selectionId, java.util.List<UUID> members,
                                            BlockPos target, int radiusX, int radiusZ, int shape) {
    public static C2SCommandStrategicFirePacket decode(FriendlyByteBuf buf) {
        return new C2SCommandStrategicFirePacket(
                buf.readUUID(),
                CommandPacketCodecs.readMembers(buf),
                buf.readBlockPos(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void encode(C2SCommandStrategicFirePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.selectionId);
        CommandPacketCodecs.writeMembers(buf, packet.members);
        buf.writeBlockPos(packet.target);
        buf.writeVarInt(packet.radiusX);
        buf.writeVarInt(packet.radiusZ);
        buf.writeVarInt(packet.shape);
    }

    public static void handle(C2SCommandStrategicFirePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                MapCommandOrderService.INSTANCE.submitStrategicFireOrder(player, packet.selectionId, packet.members, packet.target,
                        packet.radiusX, packet.radiusZ, packet.shape);
            }
        });
        context.setPacketHandled(true);
    }
}

