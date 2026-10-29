package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public record CommandUnitSnapshot(UUID unitId, BlockPos position, CommandUnitType type, int health,
                                  int aggro, boolean shields, boolean canVolley, boolean firing, float heading) {
    public static final int AGGRO_UNKNOWN = 15;
    public static final int AGGRO_NEUTRAL = 0;

    public CommandUnitSnapshot(UUID unitId, BlockPos position, CommandUnitType type, int health,
                               int aggro, boolean shields, boolean canVolley, boolean firing) {
        this(unitId, position, type, health, aggro, shields, canVolley, firing, Float.NaN);
    }
}
