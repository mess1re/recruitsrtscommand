package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import me.mss1r.recruitsrtscommand.common.command.CommandUnitType;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitSnapshot;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TacticalSymbolLayoutTest {
    @Test
    void onlyZoomedOutViewUsesSymbolsAndPickingAlwaysShowsIndividuals() {
        assertTrue(TacticalSymbolLayout.visible(0.0625, false));
        assertTrue(TacticalSymbolLayout.visible(1.25, false));
        assertFalse(TacticalSymbolLayout.visible(1.26, false));
        assertFalse(TacticalSymbolLayout.visible(0.0625, true));
    }

    @Test
    void oppositeSidesOfYawBoundaryAverageNorthNotSouth() {
        var layout = layout(List.of(unit(CommandUnitType.INFANTRY, 179),
                unit(CommandUnitType.INFANTRY, -179)));
        assertEquals(1, Math.cos(layout.angle()), 1.0E-6);
        assertEquals(0, Math.sin(layout.angle()), 1.0E-6);
    }

    @Test
    void opposingOrUnknownHeadingsProduceAFiniteStableSymbol() {
        var opposed = layout(List.of(unit(CommandUnitType.INFANTRY, 90),
                unit(CommandUnitType.INFANTRY, -90)));
        assertEquals(Math.toRadians(270), opposed.angle(), 1.0E-6);
        var unknown = layout(List.of(unit(CommandUnitType.INFANTRY, Float.NaN)));
        assertTrue(Double.isFinite(unknown.angle()));
    }

    @Test
    void glyphUsesTheVisibleBodyRatherThanOtherDetachedUnits() {
        assertEquals(TacticalSymbolLayout.Kind.INFANTRY, layout(List.of(
                unit(CommandUnitType.INFANTRY, 0), unit(CommandUnitType.INFANTRY, 0),
                unit(CommandUnitType.RANGED, 0))).kind());
        assertEquals(TacticalSymbolLayout.Kind.CAVALRY, layout(List.of(
                unit(CommandUnitType.CAVALRY, 0))).kind());
        assertEquals(TacticalSymbolLayout.Kind.RANGED, layout(List.of(
                unit(CommandUnitType.RANGED, 0))).kind());
        assertEquals(TacticalSymbolLayout.Kind.MIXED, layout(List.of(
                unit(CommandUnitType.RANGED, 0), unit(CommandUnitType.INFANTRY, 0))).kind());
    }

    @Test
    void rotatedHitTestRejectsEmptyCornersOfBoundingBox() {
        var layout = new TacticalSymbolLayout(30, 12, Math.PI / 4, TacticalSymbolLayout.Kind.INFANTRY);
        double diagonal = 14 / Math.sqrt(2);
        assertTrue(layout.contains(diagonal, diagonal, 0));
        assertFalse(layout.contains(diagonal, -diagonal, 0));
        assertFalse(layout.contains(15, 15, 0));
        assertTrue(layout.contains(0, 0, 0));
        assertEquals(21 / Math.sqrt(2), layout.bottomExtent(), 1.0E-6);
    }

    @Test
    void symbolSizeIsReadableAndBoundedForBothOneAndThousandsOfMen() {
        var one = layout(List.of(unit(CommandUnitType.INFANTRY, 0)));
        var many = new ArrayList<CommandUnitSnapshot>();
        for (int i = 0; i < 4096; i++) many.add(unit(CommandUnitType.INFANTRY, 0));
        var large = layout(many);
        assertEquals(18, one.width());
        assertEquals(30, large.width());
        assertEquals(large.width() * 0.60D, large.height());
    }

    @Test
    void zoomingOutNeverEnlargesASymbol() {
        var units = new ArrayList<CommandUnitSnapshot>();
        for (int i = 0; i < 64; i++) units.add(unit(CommandUnitType.INFANTRY, 40));
        double previousWidth = Double.MAX_VALUE;
        for (double zoom : new double[]{1.25, 1, 0.75, 0.5, 0.25, 0.125, 0.0625}) {
            var symbol = TacticalSymbolLayout.of(units, zoom);
            assertTrue(symbol.width() <= previousWidth);
            assertTrue(symbol.width() >= 8);
            assertEquals(symbol.width() * 0.60D, symbol.height());
            previousWidth = symbol.width();
        }
        assertEquals(8, previousWidth);
    }

    @Test
    void hitTestUsesTheRenderedZoomedSize() {
        var units = List.of(unit(CommandUnitType.INFANTRY, 180));
        var near = TacticalSymbolLayout.of(units, 1.25);
        var far = TacticalSymbolLayout.of(units, 0.0625);
        assertTrue(near.contains(7, 0, 0));
        assertFalse(far.contains(7, 0, 0));
        assertTrue(far.contains(0, 0, 0));
    }

    @Test
    void ownSymbolIsClearGreenAndAlliedSymbolIsBlue() {
        int face = TacticalSymbolPalette.forRelation(TacticalSymbolPalette.OWN_COLOR).face();
        assertTrue(((face >> 8) & 255) > (face & 255));
        assertTrue(((face >> 8) & 255) > ((face >> 16) & 255));
        assertTrue(((face >> 8) & 255) < 200);
        assertTrue(((face >> 8) & 255) - ((face >> 16) & 255) >= 100);
        int ally = TacticalSymbolPalette.forRelation(TacticalSymbolPalette.ALLY_COLOR).face();
        assertTrue((ally & 255) > ((ally >> 8) & 255));
        assertNotEquals(face, TacticalSymbolPalette.forRelation(TacticalSymbolPalette.ENEMY_COLOR).face());
        assertNotEquals(face, TacticalSymbolPalette.forRelation(TacticalSymbolPalette.ALLY_COLOR).face());
    }

    @Test
    void symbolsKeepTheSameRelationColorsAsUnitDots() {
        for (int color : new int[]{TacticalSymbolPalette.OWN_COLOR, TacticalSymbolPalette.ALLY_COLOR,
                TacticalSymbolPalette.NEUTRAL_COLOR, TacticalSymbolPalette.ENEMY_COLOR}) {
            assertEquals(color, TacticalSymbolPalette.forRelation(color).face());
        }
    }

    private TacticalSymbolLayout layout(List<CommandUnitSnapshot> units) {
        return TacticalSymbolLayout.of(units, TacticalSymbolLayout.MAX_MAP_SCALE);
    }

    private CommandUnitSnapshot unit(CommandUnitType type, float heading) {
        return new CommandUnitSnapshot(UUID.randomUUID(), BlockPos.ZERO, type, 100,
                0, false, false, false, heading);
    }
}
