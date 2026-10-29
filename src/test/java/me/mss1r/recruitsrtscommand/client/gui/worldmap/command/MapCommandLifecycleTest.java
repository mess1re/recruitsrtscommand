package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import me.mss1r.recruitsrtscommand.api.FireZoneShape;
import me.mss1r.recruitsrtscommand.client.command.MapCommandClientState;
import me.mss1r.recruitsrtscommand.client.command.MapCommandTool;
import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MapCommandLifecycleTest {
    @AfterEach
    void clearState() {
        MapCommandClientState.clear();
    }

    @Test
    void cancelledFireGestureCannotSubmitOnMouseRelease() {
        MapCommandClientState.clear();
        MapCommandClientState.setActiveTool(MapCommandTool.FIRE);
        var zones = new MapCommandFireZones(new MapCommandGeometry());
        var group = new CommandGroupSnapshot(UUID.randomUUID(), UUID.randomUUID(), BlockPos.ZERO,
                ResourceLocation.withDefaultNamespace("overworld"), List.of());
        assertTrue(zones.begin(10, 20, List.of(group)));
        assertTrue(zones.drag(30, 40, 1));
        assertTrue(zones.cancelGesture());
        assertFalse(zones.release(30, 40, 1));
        assertFalse(zones.cancelGesture());
        assertTrue(zones.begin(10, 20, List.of(group)));
        zones.reset();
        assertFalse(zones.release(30, 40, 1));
    }

    @Test
    void resettingBarClosesPickersAndHelp() {
        MapCommandBar bar = new MapCommandBar();
        bar.formationOpen = true;
        bar.aggroOpen = true;
        bar.showControls = true;
        bar.reset();
        assertFalse(bar.formationOpen);
        assertFalse(bar.aggroOpen);
        assertFalse(bar.showControls);
    }

    @Test
    void fireZoneDimensionsUseShapeAndServerCompatibleLimits() {
        assertArrayEquals(new int[]{5, 5}, MapCommandFireZones.radii(
                BlockPos.ZERO, new BlockPos(3, 200, 4), FireZoneShape.CIRCLE));
        assertArrayEquals(new int[]{3, 4}, MapCommandFireZones.radii(
                BlockPos.ZERO, new BlockPos(-3, 200, -4), FireZoneShape.RECTANGLE));
        assertArrayEquals(new int[]{48, 48}, MapCommandFireZones.radii(
                BlockPos.ZERO, new BlockPos(1000, 0, -1000), FireZoneShape.OVAL));
    }
}
