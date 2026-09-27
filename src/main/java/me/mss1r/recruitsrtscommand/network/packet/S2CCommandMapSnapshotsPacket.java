package me.mss1r.recruitsrtscommand.network.packet;

import me.mss1r.recruitsrtscommand.client.command.ClientCommandPacketHandler;
import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandOrderRoute;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record S2CCommandMapSnapshotsPacket(List<CommandGroupSnapshot> snapshots,
                                           List<CommandOrderRoute> routes,
                                           List<MapObjectSnapshot> objects) {
    public static S2CCommandMapSnapshotsPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<CommandGroupSnapshot> snapshots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            java.util.UUID selectionId = buf.readUUID();
            java.util.UUID ownerId = buf.readUUID();
            BlockPos anchor = buf.readBlockPos();
            net.minecraft.resources.ResourceLocation dimension = buf.readResourceLocation();

            int unitCount = buf.readVarInt();
            List<CommandUnitSnapshot> units = new ArrayList<>(unitCount);
            for (int j = 0; j < unitCount; j++) {
                java.util.UUID unitId = buf.readUUID();
                BlockPos unitPos = buf.readBlockPos();
                CommandUnitType unitType = CommandPacketCodecs.readUnitType(buf);
                int health = buf.readUnsignedByte();
                int stance = buf.readUnsignedByte();
                units.add(new CommandUnitSnapshot(unitId, unitPos, unitType, health,
                        stance & 0x0F, (stance & 0x10) != 0, (stance & 0x20) != 0,
                        (stance & 0x40) != 0));
            }
            snapshots.add(new CommandGroupSnapshot(selectionId, ownerId, anchor, dimension, units));
        }

        int routeCount = buf.readVarInt();
        List<CommandOrderRoute> routes = new ArrayList<>(routeCount);
        for (int i = 0; i < routeCount; i++) {
            List<java.util.UUID> members = CommandPacketCodecs.readMembers(buf);
            net.minecraft.resources.ResourceLocation routeDimension = buf.readResourceLocation();
            BlockPos origin = buf.readBoolean() ? buf.readBlockPos() : null;
            int legs = buf.readVarInt();
            List<BlockPos> waypoints = new ArrayList<>(legs);
            for (int j = 0; j < legs; j++) waypoints.add(buf.readBlockPos());
            routes.add(new CommandOrderRoute(members, routeDimension, origin, waypoints));
        }
        int objectCount = buf.readVarInt();
        List<MapObjectSnapshot> objects = new ArrayList<>(objectCount);
        for (int i = 0; i < objectCount; i++) {
            java.util.UUID id = buf.readUUID();
            me.mss1r.recruitsrtscommand.api.MapIcon icon = new me.mss1r.recruitsrtscommand.api.MapIcon(
                    buf.readBoolean() ? buf.readResourceLocation() : null,
                    buf.readBoolean() ? buf.readResourceLocation() : null,
                    buf.readFloat(), buf.readBoolean());
            java.util.UUID owner = buf.readBoolean() ? buf.readUUID() : null;
            BlockPos at = buf.readBlockPos();
            net.minecraft.resources.ResourceLocation world = buf.readResourceLocation();
            int condition = buf.readUnsignedByte();
            int pip = buf.readInt();
            int lineCount = buf.readVarInt();
            List<net.minecraft.network.chat.Component> lines = new ArrayList<>(lineCount);
            for (int j = 0; j < lineCount; j++) lines.add(buf.readComponent());
            int actionCount = buf.readVarInt();
            List<me.mss1r.recruitsrtscommand.api.MapObjectAction> actions = new ArrayList<>(actionCount);
            for (int j = 0; j < actionCount; j++) {
                actions.add(new me.mss1r.recruitsrtscommand.api.MapObjectAction(
                        buf.readUtf(128),
                        buf.readComponent(),
                        buf.readBoolean() ? buf.readComponent() : null,
                        buf.readBoolean(),
                        buf.readBoolean() ? buf.readComponent() : null,
                        buf.readBoolean(),
                        buf.readBoolean() ? buf.readUtf(64) : null,
                        buf.readBoolean()));
            }
            int legCount = buf.readVarInt();
            List<BlockPos> route = new ArrayList<>(legCount);
            for (int j = 0; j < legCount; j++) route.add(buf.readBlockPos());
            int crewCount = buf.readVarInt();
            List<java.util.UUID> crew = new ArrayList<>(crewCount);
            for (int j = 0; j < crewCount; j++) crew.add(buf.readUUID());
            int orderCount = buf.readVarInt();
            java.util.Map<me.mss1r.recruitsrtscommand.api.MapOrder, String> orders =
                    new java.util.EnumMap<>(me.mss1r.recruitsrtscommand.api.MapOrder.class);
            for (int j = 0; j < orderCount; j++) {
                int ordinal = buf.readVarInt();
                String actionId = buf.readUtf(128);
                me.mss1r.recruitsrtscommand.api.MapOrder[] known =
                        me.mss1r.recruitsrtscommand.api.MapOrder.values();
                if (ordinal >= 0 && ordinal < known.length) orders.put(known[ordinal], actionId);
            }
            objects.add(new MapObjectSnapshot(id, icon, owner, at, world, condition, pip, lines,
                    actions, route, crew, orders));
        }
        return new S2CCommandMapSnapshotsPacket(snapshots, routes, objects);
    }

    public static void encode(S2CCommandMapSnapshotsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.snapshots.size());
        for (CommandGroupSnapshot snapshot : packet.snapshots) {
            buf.writeUUID(snapshot.selectionId());
            buf.writeUUID(snapshot.ownerId());
            buf.writeBlockPos(snapshot.anchor());
            buf.writeResourceLocation(snapshot.dimension());

            List<CommandUnitSnapshot> units = snapshot.units();
            buf.writeVarInt(units.size());
            for (CommandUnitSnapshot unit : units) {
                buf.writeUUID(unit.unitId());
                buf.writeBlockPos(unit.position());
                CommandPacketCodecs.writeUnitType(buf, unit.type());
                buf.writeByte(unit.health());
                buf.writeByte((unit.aggro() & 0x0F) | (unit.shields() ? 0x10 : 0)
                        | (unit.canVolley() ? 0x20 : 0) | (unit.firing() ? 0x40 : 0));
            }
        }

        buf.writeVarInt(packet.routes.size());
        for (CommandOrderRoute route : packet.routes) {
            CommandPacketCodecs.writeMembers(buf, route.members());
            buf.writeResourceLocation(route.dimension());
            buf.writeBoolean(route.origin() != null);
            if (route.origin() != null) buf.writeBlockPos(route.origin());
            buf.writeVarInt(route.waypoints().size());
            for (BlockPos waypoint : route.waypoints()) buf.writeBlockPos(waypoint);
        }

        buf.writeVarInt(packet.objects.size());
        for (MapObjectSnapshot object : packet.objects) {
            buf.writeUUID(object.id());
            buf.writeBoolean(object.icon().texture() != null);
            if (object.icon().texture() != null) buf.writeResourceLocation(object.icon().texture());
            buf.writeBoolean(object.icon().item() != null);
            if (object.icon().item() != null) buf.writeResourceLocation(object.icon().item());
            buf.writeFloat(object.icon().scale());
            buf.writeBoolean(object.icon().smooth());
            buf.writeBoolean(object.ownerId() != null);
            if (object.ownerId() != null) buf.writeUUID(object.ownerId());
            buf.writeBlockPos(object.position());
            buf.writeResourceLocation(object.dimension());
            buf.writeByte(object.condition());
            buf.writeInt(object.pipColor());
            buf.writeVarInt(object.lines().size());
            for (net.minecraft.network.chat.Component line : object.lines()) buf.writeComponent(line);

            buf.writeVarInt(object.actions().size());
            for (me.mss1r.recruitsrtscommand.api.MapObjectAction action : object.actions()) {
                buf.writeUtf(action.id(), 128);
                buf.writeComponent(action.label());
                buf.writeBoolean(action.hint() != null);
                if (action.hint() != null) buf.writeComponent(action.hint());
                buf.writeBoolean(action.enabled());
                buf.writeBoolean(action.reason() != null);
                if (action.reason() != null) buf.writeComponent(action.reason());
                buf.writeBoolean(action.needsPoint());
                buf.writeBoolean(action.group() != null);
                if (action.group() != null) buf.writeUtf(action.group(), 64);
                buf.writeBoolean(action.selected());
            }
            buf.writeVarInt(object.route().size());
            for (BlockPos leg : object.route()) buf.writeBlockPos(leg);
            buf.writeVarInt(object.crew().size());
            for (java.util.UUID member : object.crew()) buf.writeUUID(member);
            buf.writeVarInt(object.orders().size());
            for (java.util.Map.Entry<me.mss1r.recruitsrtscommand.api.MapOrder, String> order
                    : object.orders().entrySet()) {
                buf.writeVarInt(order.getKey().ordinal());
                buf.writeUtf(order.getValue(), 128);
            }
        }
    }

    public static void handle(S2CCommandMapSnapshotsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientCommandPacketHandler.handleSnapshots(
                        packet.snapshots, packet.routes, packet.objects)));
        context.setPacketHandled(true);
    }
}
