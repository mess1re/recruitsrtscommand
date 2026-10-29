package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import me.mss1r.recruitsrtscommand.common.command.CommandOrderRoute;
import me.mss1r.recruitsrtscommand.common.command.CommandOrderStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapCommandOrderStatusesTest {
    @Test
    void aNewOrderReplacesTheOldFailureInTheStatusForEachSelectedMember() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        List<CommandOrderRoute> routes = List.of(route(CommandOrderStatus.BLOCKED, first, second),
                route(CommandOrderStatus.MOVING, first), route(CommandOrderStatus.PURSUING, other));
        assertEquals(Map.of(CommandOrderStatus.MOVING, 1, CommandOrderStatus.BLOCKED, 1),
                MapCommandOrderStatuses.counts(routes, member -> !member.equals(other)));
        assertEquals(Map.of(CommandOrderStatus.MOVING, 1),
                MapCommandOrderStatuses.counts(routes, first::equals));
    }

    @Test
    void anOldTerminalStatusCannotReplaceTheCurrentMarchWhenSnapshotsAreReordered() {
        UUID member = UUID.randomUUID();
        assertEquals(Map.of(CommandOrderStatus.REPATHING, 1), MapCommandOrderStatuses.counts(
                List.of(route(CommandOrderStatus.REPATHING, member), route(CommandOrderStatus.ARRIVED, member)),
                ignored -> true));
    }

    private static CommandOrderRoute route(CommandOrderStatus status, UUID... members) {
        return new CommandOrderRoute(List.of(members), ResourceLocation.withDefaultNamespace("overworld"),
                BlockPos.ZERO, List.of(new BlockPos(40, 64, 0)), status);
    }
}
