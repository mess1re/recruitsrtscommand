package me.mss1r.recruitsrtscommand.network;

import me.mss1r.recruitsrtscommand.RecruitsRTSCommand;
import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandAttackGroupPacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandAttackObjectPacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandBehaviorPacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandObjectActionPacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandMoveOrderPacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandFacePacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandStrategicFireHoldPacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SCommandStrategicFirePacket;
import me.mss1r.recruitsrtscommand.network.packet.C2SRequestCommandMapSyncPacket;
import me.mss1r.recruitsrtscommand.network.packet.S2CCommandMapSnapshotsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;

public final class RecruitsRTSCommandNetwork {
    private static final String PROTOCOL_VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(RecruitsRTSCommand.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    private static int packetId = 0;
    private static boolean initialized = false;

    private RecruitsRTSCommandNetwork() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        CHANNEL.registerMessage(packetId++, C2SRequestCommandMapSyncPacket.class,
                C2SRequestCommandMapSyncPacket::encode, C2SRequestCommandMapSyncPacket::decode,
                C2SRequestCommandMapSyncPacket::handle);
        CHANNEL.registerMessage(packetId++, C2SCommandMoveOrderPacket.class,
                C2SCommandMoveOrderPacket::encode, C2SCommandMoveOrderPacket::decode,
                C2SCommandMoveOrderPacket::handle);
        CHANNEL.registerMessage(packetId++, C2SCommandAttackGroupPacket.class,
                C2SCommandAttackGroupPacket::encode, C2SCommandAttackGroupPacket::decode,
                C2SCommandAttackGroupPacket::handle);
        CHANNEL.registerMessage(packetId++, C2SCommandAttackObjectPacket.class,
                C2SCommandAttackObjectPacket::encode, C2SCommandAttackObjectPacket::decode,
                C2SCommandAttackObjectPacket::handle);
        CHANNEL.registerMessage(packetId++, C2SCommandStrategicFirePacket.class,
                C2SCommandStrategicFirePacket::encode, C2SCommandStrategicFirePacket::decode,
                C2SCommandStrategicFirePacket::handle);
        CHANNEL.registerMessage(packetId++, C2SCommandFacePacket.class,
                C2SCommandFacePacket::encode, C2SCommandFacePacket::decode,
                C2SCommandFacePacket::handle);
        CHANNEL.registerMessage(packetId++, C2SCommandStrategicFireHoldPacket.class,
                C2SCommandStrategicFireHoldPacket::encode, C2SCommandStrategicFireHoldPacket::decode,
                C2SCommandStrategicFireHoldPacket::handle);
        CHANNEL.registerMessage(packetId++, C2SCommandBehaviorPacket.class,
                C2SCommandBehaviorPacket::encode, C2SCommandBehaviorPacket::decode,
                C2SCommandBehaviorPacket::handle);
        CHANNEL.registerMessage(packetId++, C2SCommandObjectActionPacket.class,
                C2SCommandObjectActionPacket::encode, C2SCommandObjectActionPacket::decode,
                C2SCommandObjectActionPacket::handle);
        CHANNEL.registerMessage(packetId++, S2CCommandMapSnapshotsPacket.class,
                S2CCommandMapSnapshotsPacket::encode, S2CCommandMapSnapshotsPacket::decode,
                S2CCommandMapSnapshotsPacket::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendSnapshots(ServerPlayer player, List<CommandGroupSnapshot> snapshots,
                                     List<me.mss1r.recruitsrtscommand.common.command.CommandOrderRoute> routes,
                                     List<me.mss1r.recruitsrtscommand.api.MapObjectSnapshot> objects) {
        if (player == null || player.connection == null || !CHANNEL.isRemotePresent(player.connection.connection)) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CCommandMapSnapshotsPacket(snapshots, routes, objects));
    }

    public static boolean canSendToServer() {
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> RecruitsRTSCommandNetwork::isClientChannelPresent);
    }

    private static boolean isClientChannelPresent() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return false;
        return CHANNEL.isRemotePresent(minecraft.getConnection().getConnection());
    }
}
