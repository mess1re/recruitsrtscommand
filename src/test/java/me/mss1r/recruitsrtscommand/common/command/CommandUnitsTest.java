package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandUnitsTest {
    @Test
    void facingUsesMinecraftYawAndKeepsTheHeadingAtCloseRange() {
        assertEquals(0, CommandUnits.facingYaw(Vec3.ZERO, new Vec3(0, 0, 10), 25), 0.001);
        assertEquals(-90, CommandUnits.facingYaw(Vec3.ZERO, new Vec3(10, 0, 0), 25), 0.001);
        assertEquals(90, CommandUnits.facingYaw(Vec3.ZERO, new Vec3(-10, 0, 0), 25), 0.001);
        assertEquals(25, CommandUnits.facingYaw(Vec3.ZERO, new Vec3(1, 50, 0), 25), 0.001);
    }

    @Test
    void commandDistanceDoesNotOverflowOrCountHeight() {
        assertEquals(3_600_000_000_000_000.0D, CommandUnits.horizontalDistSqr(
                new BlockPos(-30_000_000, 200, 0), new BlockPos(30_000_000, -60, 0)));
        assertEquals(25, CommandUnits.horizontalDistSqr(BlockPos.ZERO, new BlockPos(3, 200, 4)));
    }
}
