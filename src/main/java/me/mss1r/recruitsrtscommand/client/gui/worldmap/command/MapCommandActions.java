package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import com.talhanation.recruits.client.ClientManager;
import me.mss1r.recruitsrtscommand.client.command.MapCommandClientState;
import me.mss1r.recruitsrtscommand.client.command.MapCommandNetworkClient;
import me.mss1r.recruitsrtscommand.client.command.SquadClusters;
import me.mss1r.recruitsrtscommand.client.command.MapCommandTool;
import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import me.mss1r.recruitsrtscommand.api.MapOrder;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.List;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandStyle.*;

final class MapCommandActions {
    private MapCommandActions() {
    }

    static void toggleTool(MapCommandTool tool) {
        MapCommandClientState.setActiveTool(
                MapCommandClientState.activeTool() == tool ? MapCommandTool.MOVE : tool);
    }

    static void haltSelection() {
        MapCommandNetworkClient.behavior(MapCommandClientState.selectionId(),
                MapCommandClientState.selectedUnitList(), null, null, 2);
        orderSelectedObjects(MapOrder.HALT);
    }

    static void orderSelectedObjects(MapOrder order) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !MapCommandClientState.hasObjectSelection()) return;

        for (MapObjectSnapshot object : MapCommandClientState.objects(minecraft.level)) {
            if (!MapCommandClientState.isObjectSelected(object.id())) continue;
            String actionId = object.orderAction(order);
            if (actionId != null) MapCommandNetworkClient.objectAction(object.id(), actionId);
        }
    }

    static void ceaseFire() {
        MapCommandNetworkClient.holdStrategicFire(MapCommandClientState.selectionId(),
                MapCommandClientState.selectedGunners());
    }

    static int selectedAggro() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return SquadClusters.MIXED;

        int common = SquadClusters.MIXED;
        boolean any = false;
        for (CommandGroupSnapshot snapshot : MapCommandClientState.snapshotsForDimension(minecraft.level)) {
            for (CommandUnitSnapshot unit : snapshot.units()) {
                if (!MapCommandClientState.isUnitSelected(unit.unitId())) continue;
                if (!any) {
                    common = unit.aggro();
                    any = true;
                } else if (unit.aggro() != common) {
                    return SquadClusters.MIXED;
                }
            }
        }
        return common;
    }

    static boolean selectedFiring() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return false;

        boolean any = false;
        for (CommandGroupSnapshot snapshot : MapCommandClientState.snapshotsForDimension(minecraft.level)) {
            for (CommandUnitSnapshot unit : snapshot.units()) {
                if (!MapCommandClientState.isUnitSelected(unit.unitId())) continue;
                if (!unit.firing()) return false;
                any = true;
            }
        }
        return any;
    }

    static boolean selectedShields() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return false;

        boolean any = false;
        for (CommandGroupSnapshot snapshot : MapCommandClientState.snapshotsForDimension(minecraft.level)) {
            for (CommandUnitSnapshot unit : snapshot.units()) {
                if (!MapCommandClientState.isUnitSelected(unit.unitId())) continue;
                if (!unit.shields()) return false;
                any = true;
            }
        }
        return any;
    }

    static void cycleFormation(int direction) {
        int current = Mth.clamp(ClientManager.formationSelection, 0, 8);
        int currentIndex = 0;
        for (int i = 0; i < FORMATION_CYCLE.length; i++) {
            if (FORMATION_CYCLE[i] == current) {
                currentIndex = i;
                break;
            }
        }
        int next = Math.floorMod(currentIndex + (direction < 0 ? -1 : 1), FORMATION_CYCLE.length);
        ClientManager.formationSelection = FORMATION_CYCLE[next];
    }

    static List<CommandGroupSnapshot> currentSelections() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return List.of();
        return MapCommandClientState.selectedForDimensionSnapshots(minecraft.level);
    }

    static boolean anyCrewInHand() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !MapCommandClientState.hasObjectSelection()) return false;

        for (MapObjectSnapshot object : MapCommandClientState.objects(minecraft.level)) {
            if (MapCommandClientState.isObjectSelected(object.id()) && !object.crew().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    static int countArchersSelected() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return 0;

        int archers = 0;
        for (CommandGroupSnapshot snapshot : MapCommandClientState.snapshotsForDimension(minecraft.level)) {
            for (CommandUnitSnapshot unit : snapshot.units()) {
                if (unit.canVolley() && MapCommandClientState.isUnitSelected(unit.unitId())) archers++;
            }
        }
        return archers;
    }
}
