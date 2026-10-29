package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandCombatTest {
    @Test
    void anArcherStopsAtShootingRangeInsteadOfWalkingThatRangeForward() {
        Vec3 stand = CommandCombat.shootingPosition(Vec3.ZERO, new Vec3(40, 0, 0), 33);
        assertEquals(new Vec3(7, 0, 0), stand);
        assertEquals(33, stand.distanceTo(new Vec3(40, 0, 0)), 0.00001);
        assertEquals(new Vec3(67, 0, 0),
                CommandCombat.shootingPosition(Vec3.ZERO, new Vec3(100, 0, 0), 33));
    }

    @Test
    void approachingDiagonallyKeepsTheShootersHeightAndTheDesiredHorizontalDistance() {
        Vec3 start = new Vec3(10, 64, 20);
        Vec3 target = new Vec3(70, 100, 100);
        Vec3 stand = CommandCombat.shootingPosition(start, target, 33);
        assertEquals(64, stand.y);
        assertEquals(33 * 33, CommandUnits.horizontalDistSqr(stand, target), 0.00001);
    }

    @Test
    void aShooterAlreadyInRangeDoesNotAdvanceOrDivideByZero() {
        Vec3 shooter = new Vec3(3, 64, 4);
        assertSame(shooter, CommandCombat.shootingPosition(shooter, new Vec3(3, 100, 4), 33));
        assertSame(shooter, CommandCombat.shootingPosition(shooter, new Vec3(10, 64, 4), 33));
    }
}
