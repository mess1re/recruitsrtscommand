package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public record CommandGroupSnapshot(
        UUID selectionId,
        UUID ownerId,
        BlockPos anchor,
        ResourceLocation dimension,
        List<CommandUnitSnapshot> units
) {
    public int totalUnits() {
        return units.size();
    }

    public int countOf(CommandUnitType type) {
        int count = 0;
        for (CommandUnitSnapshot unit : units) {
            if (unit.type() == type) count++;
        }
        return count;
    }
}
