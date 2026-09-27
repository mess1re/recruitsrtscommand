package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record CommandOrderRoute(List<UUID> members, ResourceLocation dimension,
                                @Nullable BlockPos origin, List<BlockPos> waypoints) {
    public CommandOrderRoute {
        members = List.copyOf(members == null ? List.of() : members);
        waypoints = List.copyOf(waypoints == null ? List.of() : waypoints);
    }
}
