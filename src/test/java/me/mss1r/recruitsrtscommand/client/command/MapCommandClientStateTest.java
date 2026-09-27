package me.mss1r.recruitsrtscommand.client.command;

import me.mss1r.recruitsrtscommand.api.FireZoneShape;
import me.mss1r.recruitsrtscommand.api.MapIcon;
import me.mss1r.recruitsrtscommand.api.MapObjectAction;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapCommandClientStateTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID objectId = UUID.randomUUID();
    private final UUID crewId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MapCommandClientState.clear();
    }

    @AfterEach
    void tearDown() {
        MapCommandClientState.clear();
    }

    @Test
    void crewOnlyFireZoneSurvivesSnapshotCleanup() {
        MapCommandClientState.updateObjects(List.of(object(List.of(crewId), List.of())), owner);
        MapCommandClientState.rememberStrategicFireOrder(
                List.of(crewId), BlockPos.ZERO, 8, 4, FireZoneShape.OVAL);

        MapCommandClientState.updateSnapshots(List.of(), owner);

        assertEquals(1, MapCommandClientState.fireZones().size());
        assertTrue(MapCommandClientState.fireZones().get(0).siege());
    }

    @Test
    void vanishedObjectIsRemovedFromSelection() {
        MapCommandClientState.updateObjects(List.of(object(List.of(), List.of())), owner);
        MapCommandClientState.selectObjects(List.of(objectId), false);
        assertTrue(MapCommandClientState.selectedObjects().contains(objectId));

        MapCommandClientState.updateObjects(List.of(), owner);

        assertFalse(MapCommandClientState.selectedObjects().contains(objectId));
        assertFalse(MapCommandClientState.hasAnything());
    }

    @Test
    void optionSelectionAdvancesOptimistically() {
        MapObjectAction first = MapObjectAction.option("ammo", "standard",
                Component.literal("Standard"), null, true);
        MapObjectAction second = MapObjectAction.option("ammo", "explosive",
                Component.literal("Explosive"), null, false);
        MapCommandClientState.updateObjects(List.of(object(List.of(), List.of(first, second))), owner);

        MapCommandClientState.selectObjectOption(objectId, "ammo", "explosive");

        List<MapObjectAction> actions = MapCommandClientState.objectSnapshot(objectId).actions();
        assertFalse(actions.get(0).selected());
        assertTrue(actions.get(1).selected());
    }

    private MapObjectSnapshot object(List<UUID> crew, List<MapObjectAction> actions) {
        return new MapObjectSnapshot(
                objectId,
                MapIcon.item(ResourceLocation.fromNamespaceAndPath("minecraft", "stick"), 1.0F),
                owner,
                BlockPos.ZERO,
                ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
                100,
                MapObjectSnapshot.NO_PIP,
                List.of(Component.literal("Test")),
                actions,
                List.of(),
                crew,
                Map.of());
    }
}
