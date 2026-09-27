package me.mss1r.recruitsrtscommand.network.packet;

import me.mss1r.recruitsrtscommand.common.command.MapCommandOrderService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record C2SCommandObjectActionPacket(UUID objectId, String actionId, List<UUID> members,
                                           @org.jetbrains.annotations.Nullable net.minecraft.core.BlockPos target) {
    private static final int MAX_ACTION_LENGTH = 128;

    public static C2SCommandObjectActionPacket decode(FriendlyByteBuf buf) {
        return new C2SCommandObjectActionPacket(buf.readUUID(), buf.readUtf(MAX_ACTION_LENGTH),
                CommandPacketCodecs.readMembers(buf),
                buf.readBoolean() ? buf.readBlockPos() : null);
    }

    public static void encode(C2SCommandObjectActionPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.objectId);
        buf.writeUtf(packet.actionId, MAX_ACTION_LENGTH);
        CommandPacketCodecs.writeMembers(buf, packet.members);
        buf.writeBoolean(packet.target != null);
        if (packet.target != null) buf.writeBlockPos(packet.target);
    }

    public static void handle(C2SCommandObjectActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            MapCommandOrderService.INSTANCE.submitObjectAction(
                    player, packet.objectId, packet.members, packet.actionId, packet.target);
        });
        context.setPacketHandled(true);
    }
}
