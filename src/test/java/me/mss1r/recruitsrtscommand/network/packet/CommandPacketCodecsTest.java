package me.mss1r.recruitsrtscommand.network.packet;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandPacketCodecsTest {
    @Test
    void rejectsTruncatedMemberListBeforeAllocating() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(4096);
        assertThrows(DecoderException.class, () -> CommandPacketCodecs.readMembers(buffer));
    }

    @Test
    void boundedCountsRejectInvalidValuesAndTruncatedPayloads() {
        for (int invalid : new int[]{-1, 129, Integer.MAX_VALUE}) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeVarInt(invalid);
            buffer.writeZero(256);
            assertThrows(DecoderException.class, () -> CommandPacketCodecs.readCount(buffer, 128, "test"));
        }
        FriendlyByteBuf truncated = new FriendlyByteBuf(Unpooled.buffer());
        truncated.writeVarInt(128);
        assertThrows(DecoderException.class, () -> CommandPacketCodecs.readCount(truncated, 128, "test"));
        FriendlyByteBuf output = new FriendlyByteBuf(Unpooled.buffer());
        assertThrows(EncoderException.class, () -> CommandPacketCodecs.writeCount(output, 129, 128, "test"));
    }

    @Test
    void snapshotDecoderRejectsUnboundedGroups() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(Integer.MAX_VALUE);
        assertThrows(DecoderException.class, () -> S2CCommandMapSnapshotsPacket.decode(buffer));
    }

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
