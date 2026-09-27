package me.mss1r.recruitsrtscommand.client.command;

import me.mss1r.recruitsrtscommand.api.FireZoneShape;
import me.mss1r.recruitsrtscommand.network.RecruitsRTSCommandNetwork;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandAttackGroupPacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandAttackObjectPacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandBehaviorPacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandObjectActionPacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandFacePacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandMoveOrderPacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandStrategicFireHoldPacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandStrategicFirePacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SRequestCommandMapSyncPacket;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;

public final class MapCommandNetworkClient {
    private MapCommandNetworkClient() {
    }

    public static boolean available() {
        return RecruitsRTSCommandNetwork.canSendToServer();
    }

    public static void requestSync() {
        if (!available()) return;
        RecruitsRTSCommandNetwork.CHANNEL.sendToServer(
                new C2SRequestCommandMapSyncPacket(MapCommandClientState.selectedUnitList()));
    }

    public static void objectAction(UUID objectId, String actionId) {
        objectAction(objectId, actionId, null);
    }

    public static void objectAction(UUID objectId, String actionId, BlockPos target) {
        if (!available()) return;
        RecruitsRTSCommandNetwork.CHANNEL.sendToServer(new C2SCommandObjectActionPacket(
                objectId, actionId, MapCommandClientState.selectedUnitList(), target));
    }

    public static void move(UUID selectionId, List<UUID> members, BlockPos target, boolean append,
                            int formation, boolean tight, boolean holdFormation) {
        if (!available()) return;
        MapCommandClientState.rememberMoveOrder(target, append);
        RecruitsRTSCommandNetwork.CHANNEL.sendToServer(
                new C2SCommandMoveOrderPacket(selectionId, members, target, append, formation, tight,
                        holdFormation));
    }

    public static void face(UUID selectionId, List<UUID> members, BlockPos lookAt, int formation,
                            boolean tight, boolean holdFormation) {
        if (!available()) return;
        RecruitsRTSCommandNetwork.CHANNEL.sendToServer(
                new C2SCommandFacePacket(selectionId, members, lookAt, formation, tight, holdFormation));
    }

    public static void attack(UUID attackerSelectionId, List<UUID> attackers, UUID targetSelectionId) {
        if (!available()) return;
        RecruitsRTSCommandNetwork.CHANNEL.sendToServer(
                new C2SCommandAttackGroupPacket(attackerSelectionId, attackers, targetSelectionId));
    }

    public static void attackObject(UUID attackerSelectionId, List<UUID> attackers, UUID objectId) {
        if (!available()) return;
        RecruitsRTSCommandNetwork.CHANNEL.sendToServer(
                new C2SCommandAttackObjectPacket(attackerSelectionId, attackers, objectId));
    }

    public static void strategicFire(UUID selectionId, List<UUID> members, BlockPos target,
                                     int radiusX, int radiusZ, FireZoneShape shape) {
        if (!available()) return;
        MapCommandClientState.rememberStrategicFireOrder(members, target, radiusX, radiusZ, shape);
        RecruitsRTSCommandNetwork.CHANNEL.sendToServer(
                new C2SCommandStrategicFirePacket(selectionId, members, target, radiusX, radiusZ, shape.ordinal()));
    }

    public static void holdStrategicFire(UUID selectionId, List<UUID> members) {
        if (!available()) return;
        MapCommandClientState.releaseFromZones(members);
        RecruitsRTSCommandNetwork.CHANNEL.sendToServer(
                new C2SCommandStrategicFireHoldPacket(selectionId, members));
    }

    public static void behavior(UUID selectionId, List<UUID> members, Integer aggroState, Boolean fireAtWill,
                                Integer followState) {
        behavior(selectionId, members, aggroState, fireAtWill, followState, null);
    }

    public static void behavior(UUID selectionId, List<UUID> members, Integer aggroState, Boolean fireAtWill,
                                Integer followState, Boolean shields) {
        if (!available()) return;
        RecruitsRTSCommandNetwork.CHANNEL.sendToServer(new C2SCommandBehaviorPacket(
                selectionId,
                members,
                aggroState == null ? C2SCommandBehaviorPacket.NO_CHANGE : aggroState,
                fireAtWill == null ? C2SCommandBehaviorPacket.NO_CHANGE : (fireAtWill ? 1 : 0),
                followState == null ? C2SCommandBehaviorPacket.NO_CHANGE : followState,
                shields == null ? C2SCommandBehaviorPacket.NO_CHANGE : (shields ? 1 : 0)
        ));
    }
}
