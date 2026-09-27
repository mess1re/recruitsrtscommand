package me.mss1r.recruitsrtscommand.network.packet;

import io.netty.handler.codec.DecoderException;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitType;
import net.minecraft.network.FriendlyByteBuf;

final class CommandPacketCodecs {
    private CommandPacketCodecs() {
    }

    private static final int MAX_MEMBERS = 4096;

    static java.util.List<java.util.UUID> readMembers(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_MEMBERS) {
            throw new DecoderException("Invalid command member count: " + count);
        }
        java.util.List<java.util.UUID> members = new java.util.ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            members.add(buf.readUUID());
        }
        return members;
    }

    static void writeMembers(FriendlyByteBuf buf, java.util.List<java.util.UUID> members) {
        java.util.List<java.util.UUID> safe = members == null ? java.util.List.of() : members;
        int count = Math.min(safe.size(), MAX_MEMBERS);
        buf.writeVarInt(count);
        for (int index = 0; index < count; index++) {
            buf.writeUUID(safe.get(index));
        }
    }

    static CommandUnitType readUnitType(FriendlyByteBuf buf) {
        return CommandUnitType.fromOrdinal(buf.readVarInt());
    }

    static void writeUnitType(FriendlyByteBuf buf, CommandUnitType type) {
        buf.writeVarInt((type == null ? CommandUnitType.ALL : type).ordinal());
    }
}
