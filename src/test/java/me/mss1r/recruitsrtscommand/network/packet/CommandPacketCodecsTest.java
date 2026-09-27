package me.mss1r.recruitsrtscommand.network.packet;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandPacketCodecsTest {
    @Test
    void rejectsMemberCountLargerThanPacketLimit() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(4097);

        assertThrows(DecoderException.class, () -> CommandPacketCodecs.readMembers(buffer));
    }

    @Test
    void rejectsNegativeMemberCount() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(-1);

        assertThrows(DecoderException.class, () -> CommandPacketCodecs.readMembers(buffer));
    }

    @Test
    void encoderCapsMembersWithoutCorruptingFollowingFields() {
        List<UUID> members = new ArrayList<>();
        for (int index = 0; index < 4100; index++) {
            members.add(new UUID(index, ~index));
        }
        UUID following = UUID.randomUUID();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        CommandPacketCodecs.writeMembers(buffer, members);
        buffer.writeUUID(following);

        assertEquals(members.subList(0, 4096), CommandPacketCodecs.readMembers(buffer));
        assertEquals(following, buffer.readUUID());
        assertEquals(0, buffer.readableBytes());
    }
}
