package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.UUID;

record CommandSelection(
        UUID selectionId,
        UUID rootGroupId,
        UUID ownerId,
        ResourceLocation dimension,
        BlockPos anchor,
        List<UUID> memberIds,
        List<CommandUnitSnapshot> units
) {
}
