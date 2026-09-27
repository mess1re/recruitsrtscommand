package me.mss1r.recruitsrtscommand.network.packet;

import me.mss1r.recruitsrtscommand.common.command.MapCommandOrderService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2SCommandBehaviorPacket(UUID selectionId, java.util.List<UUID> members,
                                       int aggroState, int fireAtWill, int followState, int shields) {
    public static final int NO_CHANGE = -1;

    public static C2SCommandBehaviorPacket decode(FriendlyByteBuf buf) {
        return new C2SCommandBehaviorPacket(buf.readUUID(), CommandPacketCodecs.readMembers(buf),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void encode(C2SCommandBehaviorPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.selectionId);
        CommandPacketCodecs.writeMembers(buf, packet.members);
        buf.writeVarInt(packet.aggroState);
        buf.writeVarInt(packet.fireAtWill);
        buf.writeVarInt(packet.followState);
        buf.writeVarInt(packet.shields);
    }

    public static void handle(C2SCommandBehaviorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            Integer aggro = packet.aggroState == NO_CHANGE ? null : packet.aggroState;
            Boolean fire = packet.fireAtWill == NO_CHANGE ? null : packet.fireAtWill != 0;
            Integer follow = packet.followState == NO_CHANGE ? null : packet.followState;
            Boolean shields = packet.shields == NO_CHANGE ? null : packet.shields != 0;
            MapCommandOrderService.INSTANCE.submitBehaviorOrder(player, packet.selectionId, packet.members,
                    aggro, fire, follow, shields);
        });
        context.setPacketHandled(true);
    }
}

