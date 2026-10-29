package me.mss1r.recruitsrtscommand.network.packet;

import io.netty.buffer.Unpooled;
import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitType;
import me.mss1r.recruitsrtscommand.common.command.CommandOrderRoute;
import me.mss1r.recruitsrtscommand.common.command.CommandOrderStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandMapSnapshotsPacketTest {
    @Test
    void routeStatusesAndEmptyPursuitPathsSurvivePacketRoundTrip() {
        List<CommandOrderRoute> routes = java.util.Arrays.stream(CommandOrderStatus.values())
                .map(status -> new CommandOrderRoute(List.of(UUID.randomUUID()),
                        ResourceLocation.withDefaultNamespace("overworld"), BlockPos.ZERO,
                        status == CommandOrderStatus.PURSUING ? List.of() : List.of(new BlockPos(20, 64, 30)), status))
                .toList();
        var original = new S2CCommandMapSnapshotsPacket(List.of(), routes, List.of());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            S2CCommandMapSnapshotsPacket.encode(original, buffer);
            assertEquals(original, S2CCommandMapSnapshotsPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void headingsAndStancesSurvivePacketRoundTrip() {
        var unit = new CommandUnitSnapshot(UUID.randomUUID(), new BlockPos(12, 80, -24),
                CommandUnitType.CAVALRY, 65, 3, true, true, true, -179.5F);
        var group = new CommandGroupSnapshot(UUID.randomUUID(), UUID.randomUUID(), unit.position(),
                ResourceLocation.withDefaultNamespace("overworld"), List.of(unit));
        var original = new S2CCommandMapSnapshotsPacket(List.of(group), List.of(), List.of());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            S2CCommandMapSnapshotsPacket.encode(original, buffer);
            assertEquals(original, S2CCommandMapSnapshotsPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
