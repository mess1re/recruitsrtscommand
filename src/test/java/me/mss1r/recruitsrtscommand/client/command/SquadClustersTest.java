package me.mss1r.recruitsrtscommand.client.command;

import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SquadClustersTest {
    @AfterEach
    void clearTracks() {
        MapPositionSmoothing.clear();
    }

    @Test
    void cacheKeepsMembershipButRecalculatesInterpolatedCoordinates() {
        CommandUnitSnapshot unit = unit(0, CommandUnitType.INFANTRY);
        List<CommandGroupSnapshot> snapshots = List.of(group(List.of(unit)));
        SquadClusters.Cache cache = new SquadClusters.Cache();
        SquadClusters.Cluster first = cache.update(1, snapshots).get(0);
        MapPositionSmoothing.observe(unit.unitId(), 10, 20, System.currentTimeMillis());

        SquadClusters.Cluster nextFrame = cache.update(1, snapshots).get(0);
        assertSame(first.body(), nextFrame.body());
        assertEquals(first.members(), nextFrame.members());
        assertEquals(10.0D, nextFrame.centerX());
        assertEquals(20.0D, nextFrame.centerZ());
        assertTrue(cache.update(2, List.of()).isEmpty());
        cache.clear();
        assertEquals(1, cache.update(2, snapshots).size());
    }

    @Test
    void detachedBodyDoesNotInheritTheOtherBodiesTypes() {
        CommandGroupSnapshot group = group(List.of(unit(0, CommandUnitType.INFANTRY),
                unit(1, CommandUnitType.INFANTRY), unit(100, CommandUnitType.RANGED)));
        List<SquadClusters.Cluster> clusters = SquadClusters.of(group);
        assertEquals(2, clusters.size());
        assertEquals(2, clusters.get(0).strength());
        assertEquals(0, SquadClusters.countOf(clusters.get(0), CommandUnitType.RANGED));
        assertEquals(1, SquadClusters.countOf(clusters.get(1), CommandUnitType.RANGED));
    }

    private CommandUnitSnapshot unit(int x, CommandUnitType type) {
        return new CommandUnitSnapshot(UUID.randomUUID(), new BlockPos(x, 64, 0),
                type, 100, 0, false, false, false, 30);
    }

    private CommandGroupSnapshot group(List<CommandUnitSnapshot> units) {
        return new CommandGroupSnapshot(UUID.randomUUID(), UUID.randomUUID(), BlockPos.ZERO,
                ResourceLocation.withDefaultNamespace("overworld"), units);
    }
}
