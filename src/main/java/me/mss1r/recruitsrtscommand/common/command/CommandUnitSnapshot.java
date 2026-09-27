package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public record CommandUnitSnapshot(UUID unitId, BlockPos position, CommandUnitType type, int health,
                                  int aggro, boolean shields, boolean canVolley, boolean firing) {
    public static final int AGGRO_UNKNOWN = 15;
    public static final int AGGRO_NEUTRAL = 0;
}
