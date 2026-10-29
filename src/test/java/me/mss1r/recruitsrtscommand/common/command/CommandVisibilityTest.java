package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommandVisibilityTest {
    private final UUID viewer = UUID.randomUUID();
    private final UUID stranger = UUID.randomUUID();

    @Test
    void scoutingSendsOnlyUnitsWithinHorizontalSight() {
        CommandVisibility visibility = visibility(true, false);
        CommandUnitSnapshot near = unit(new BlockPos(9, 200, 0));
        CommandUnitSnapshot far = unit(new BlockPos(10, 64, 0));

        List<CommandUnitSnapshot> seen = visibility.units(stranger, List.of(near, far));

        assertEquals(List.of(near.unitId()), seen.stream().map(CommandUnitSnapshot::unitId).toList());
        assertTrue(visibility.sees(stranger, near.position()));
        assertFalse(visibility.sees(stranger, far.position()));
    }

    @Test
    void visibleStrangersDoNotExposeHealthOrCommandState() {
        CommandUnitSnapshot unit = unit(BlockPos.ZERO);
        CommandUnitSnapshot masked = visibility(true, false).units(stranger, List.of(unit)).get(0);
        assertEquals(CommandUnitSnapshot.HEALTH_UNKNOWN, masked.health());
        assertEquals(CommandUnitSnapshot.AGGRO_UNKNOWN, masked.aggro());
        assertFalse(masked.shields());
        assertFalse(masked.canVolley());
        assertFalse(masked.firing());
        assertEquals(unit.heading(), masked.heading());
        assertEquals(unit.type(), masked.type());
        assertEquals(unit.health(), visibility(true, true).units(stranger, List.of(unit)).get(0).health());
    }

    @Test
    void ownUnitsRemainVisibleAndKeepTheirState() {
        List<CommandUnitSnapshot> ours = List.of(unit(new BlockPos(1000, 64, 1000)));
        CommandVisibility visibility = new CommandVisibility(viewer, List.of(), true, false);
        assertSame(ours, visibility.units(viewer, ours));
        assertTrue(visibility.sees(viewer, ours.get(0).position()));
        assertTrue(visibility.units(stranger, ours).isEmpty());
    }

    @Test
    void disablingScoutingDoesNotDisableHealthPrivacy() {
        CommandUnitSnapshot far = unit(new BlockPos(1000, 64, 1000));
        CommandUnitSnapshot seen = visibility(false, false).units(stranger, List.of(far)).get(0);
        assertEquals(far.position(), seen.position());
        assertEquals(CommandUnitSnapshot.HEALTH_UNKNOWN, seen.health());
    }

    private CommandVisibility visibility(boolean scouting, boolean health) {
        return new CommandVisibility(viewer,
                List.of(new CommandVisibility.Watcher(new Vec3(0.5, 64, 0.5), 9)), scouting, health);
    }

    @Test
    void pursuitUsesCurrentPositionsEvenIfTheMapHasNotSentAnotherSnapshot() {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(2, 64, 0);
        List<BlockPos.MutableBlockPos> targets = List.of(position);
        CommandVisibility sight = visibility(true, false);
        assertEquals(targets, sight.visibleTargets(stranger, targets, target -> target));
        position.set(100, 64, 0);
        assertTrue(sight.visibleTargets(stranger, targets, target -> target).isEmpty());
        assertEquals(targets, visibility(false, false).visibleTargets(stranger, targets, target -> target));
    }

    @Test
    void losingOneMemberOfAVisibleEnemySquadDoesNotExposeItsHiddenMembers() {
        BlockPos near = new BlockPos(2, 64, 0);
        BlockPos far = new BlockPos(100, 64, 0);
        assertEquals(List.of(near), visibility(true, false).visibleTargets(stranger, List.of(near, far), target -> target));
        CommandVisibility noWatchers = new CommandVisibility(viewer, List.of(), true, false);
        assertTrue(noWatchers.visibleTargets(stranger, List.of(near), target -> target).isEmpty());
    }

    private CommandUnitSnapshot unit(BlockPos position) {
        return new CommandUnitSnapshot(UUID.randomUUID(), position, CommandUnitType.RANGED,
                73, 3, true, true, true, -170);
    }
}
