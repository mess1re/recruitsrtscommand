package me.mss1r.recruitsrtscommand.common.command;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.IStrategicFire;
import com.talhanation.recruits.entities.ScoutEntity;
import com.talhanation.recruits.entities.ai.RecruitRangedBowAttackGoal;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsGroupsSaveData;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import me.mss1r.recruitsrtscommand.api.RecruitsRTSCommandApi;
import me.mss1r.recruitsrtscommand.config.RecruitsRTSCommandServerConfig;
import me.mss1r.recruitsrtscommand.network.RecruitsRTSCommandNetwork;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static me.mss1r.recruitsrtscommand.common.command.CommandUnits.*;
import me.mss1r.recruitsrtscommand.common.command.CommandVisibility.Watcher;

final class CommandMapSnapshots {
    private static final double SCOUT_SIGHT = 2.5D;
    private static final double MOUNTED_SIGHT = 1.5D;
    private final Map<UUID, Map<UUID, CommandSelection>> playerSelections = new HashMap<>();
    private final Map<UUID, Map<UUID, MapObjectSnapshot>> visibleObjects = new HashMap<>();
    private final SnapshotRequestLimiter snapshotRequests = new SnapshotRequestLimiter();

    void clearPlayer(UUID player) {
        playerSelections.remove(player);
        visibleObjects.remove(player);
        snapshotRequests.remove(player);
    }

    void clear() {
        playerSelections.clear();
        visibleObjects.clear();
        snapshotRequests.clear();
    }

    private CommandVisibility visibility(ServerPlayer player) {
        return new CommandVisibility(player.getUUID(), watchPosts(player),
                RecruitsRTSCommandServerConfig.SCOUTING_ENABLED.get(),
                RecruitsRTSCommandServerConfig.STRANGER_HEALTH_VISIBLE.get());
    }

    void send(ServerPlayer player, List<UUID> selected,
              java.util.function.Supplier<List<CommandOrderRoute>> routes) {
        if (player == null || !snapshotRequests.acquire(player.getUUID(),
                player.serverLevel().getServer().overworld().getGameTime())) return;
        List<MapObjectSnapshot> objects = buildObjects(player);
        rememberVisibleObjects(player, objects);
        Map<UUID, List<UUID>> crews = new HashMap<>();
        List<CommandGroupSnapshot> snapshots = buildSnapshots(player, crews);
        RecruitsRTSCommandNetwork.sendSnapshots(player, snapshots, routes.get(),
                withActions(player, withCrews(player, objects, crews), selected));
    }

    private List<MapObjectSnapshot> withCrews(ServerPlayer player, List<MapObjectSnapshot> objects,
                                              Map<UUID, List<UUID>> crews) {
        if (objects.isEmpty() || crews.isEmpty()) return objects;

        List<MapObjectSnapshot> manned = new ArrayList<>(objects.size());
        for (MapObjectSnapshot object : objects) {
            List<UUID> crew = player.getUUID().equals(object.ownerId())
                    ? crews.get(object.id())
                    : null;
            manned.add(crew == null || crew.isEmpty() ? object : object.withCrew(crew));
        }
        return manned;
    }

    private List<MapObjectSnapshot> withActions(ServerPlayer player, List<MapObjectSnapshot> objects,
                                                List<UUID> selected) {
        if (objects.isEmpty()) return objects;

        List<UUID> commandable = commandableMembers(player, selected);
        List<MapObjectSnapshot> described = new ArrayList<>(objects.size());
        for (MapObjectSnapshot object : objects) {
            described.add(RecruitsRTSCommandApi.withActions(player, object, commandable));
        }
        return described;
    }

    List<UUID> commandableMembers(ServerPlayer player, List<UUID> members) {
        if (members == null || members.isEmpty()) return List.of();

        ServerLevel level = player.serverLevel();
        Set<UUID> allowed = new LinkedHashSet<>(members.size());
        for (UUID member : members) {
            if (level.getEntity(member) instanceof AbstractRecruitEntity recruit
                    && recruit.isEffectedByCommand(player.getUUID())) {
                allowed.add(member);
            }
        }
        return List.copyOf(allowed);
    }

    private void rememberVisibleObjects(ServerPlayer player, List<MapObjectSnapshot> objects) {
        Map<UUID, MapObjectSnapshot> visible = new HashMap<>(objects.size());
        for (MapObjectSnapshot object : objects) visible.put(object.id(), object.withoutActions());
        visibleObjects.put(player.getUUID(), visible);
    }

    @Nullable
    MapObjectSnapshot currentVisibleObject(ServerPlayer player, UUID objectId) {
        Map<UUID, MapObjectSnapshot> remembered = visibleObjects.get(player.getUUID());
        if (remembered == null || !remembered.containsKey(objectId)) {
            return null;
        }
        return buildObjects(player).stream()
                .filter(candidate -> candidate.id().equals(objectId))
                .findFirst()
                .orElse(null);
    }

    List<MapObjectSnapshot> buildObjects(ServerPlayer player) {
        List<MapObjectSnapshot> objects = RecruitsRTSCommandApi.collectObjects(player);
        if (objects.isEmpty()) return objects;

        CommandVisibility visibility = visibility(player);
        ResourceLocation currentDimension = player.serverLevel().dimension().location();

        List<MapObjectSnapshot> seen = new ArrayList<>(objects.size());
        Set<UUID> objectIds = new HashSet<>();
        for (MapObjectSnapshot object : objects) {
            if (object == null || object.id() == null || object.icon() == null
                    || object.position() == null || object.dimension() == null
                    || !objectIds.add(object.id())) {
                continue;
            }
            if (!currentDimension.equals(object.dimension())) continue;
            if (visibility.sees(object.ownerId(), object.position())) {
                seen.add(object);
            }
        }
        return seen;
    }

    List<LivingEntity> visiblePursuitTargets(ServerPlayer player, UUID groupId, UUID objectId) {
        List<LivingEntity> targets = new ArrayList<>();
        ServerLevel level = player.serverLevel();
        CommandVisibility sight = visibility(player);
        if (objectId != null) {
            MapObjectSnapshot object = buildObjects(player).stream()
                    .filter(candidate -> objectId.equals(candidate.id())).findFirst().orElse(null);
            if (object != null && level.getEntity(objectId) instanceof LivingEntity target && target.isAlive()) {
                targets.addAll(sight.visibleTargets(object.ownerId(), List.of(target), Entity::blockPosition));
            }
        } else {
            for (RecruitsGroup group : getAllGroups(player)) {
                if (!Objects.equals(groupId, group.getUUID())) continue;
                targets.addAll(sight.visibleTargets(group.getPlayerUUID(),
                        resolveGroupMembers(level, group), Entity::blockPosition));
                break;
            }
        }
        return targets;
    }

    List<CommandGroupSnapshot> buildSnapshots(ServerPlayer player) {
        return buildSnapshots(player, null);
    }

    private List<CommandGroupSnapshot> buildSnapshots(ServerPlayer player,
                                                      Map<UUID, List<UUID>> crews) {
        if (player == null) return List.of();

        Map<UUID, CommandSelection> selectionIndex = new HashMap<>();
        List<CommandGroupSnapshot> snapshots = new ArrayList<>();
        CommandVisibility visibility = visibility(player);

        for (RecruitsGroup group : getAllGroups(player)) {
            for (CommandSelection selection : buildSelections(player, group, crews)) {
                List<CommandUnitSnapshot> units = visibility.units(selection.ownerId(), selection.units());
                if (units.isEmpty()) continue;

                BlockPos visibleAnchor = units.size() == selection.units().size()
                        ? selection.anchor()
                        : BlockPos.containing(centerOfSnapshots(units));
                List<UUID> visibleMembers = units.stream().map(CommandUnitSnapshot::unitId).toList();
                selectionIndex.put(selection.selectionId(), new CommandSelection(
                        selection.selectionId(), selection.rootGroupId(), selection.ownerId(),
                        selection.dimension(), visibleAnchor, visibleMembers, units));

                snapshots.add(new CommandGroupSnapshot(
                        selection.selectionId(),
                        selection.ownerId(),
                        visibleAnchor,
                        selection.dimension(),
                        units
                ));
            }
        }

        playerSelections.put(player.getUUID(), selectionIndex);
        return snapshots;
    }

    private List<CommandSelection> buildSelections(ServerPlayer player, RecruitsGroup group,
                                                  Map<UUID, List<UUID>> crews) {
        if (group == null || group.members == null || group.members.isEmpty()) return List.of();

        ServerLevel level = player.serverLevel();
        ResourceLocation dimension = level.dimension().location();
        List<AbstractRecruitEntity> recruits = resolveGroupMembers(level, group);
        if (recruits.isEmpty()) return List.of();

        List<UUID> memberIds = recruits.stream()
                .map(Entity::getUUID)
                .sorted()
                .toList();
        return List.of(new CommandSelection(
                group.getUUID(),
                group.getUUID(),
                group.getPlayerUUID(),
                dimension,
                anchorOf(recruits),
                memberIds,
                unitSnapshots(player, recruits, crews)
        ));
    }

    private int healthPercent(AbstractRecruitEntity recruit) {
        float max = recruit.getMaxHealth();
        if (max <= 0.0F) return 0;
        return Mth.clamp(Math.round(recruit.getHealth() / max * 100.0F), 0, 100);
    }

    private boolean canVolley(AbstractRecruitEntity recruit) {
        return recruit instanceof IStrategicFire
                && RecruitRangedBowAttackGoal.isHoldingBow(recruit);
    }

    private CommandUnitType dominantType(AbstractRecruitEntity recruit) {
        if (CommandUnitType.CAVALRY.matches(recruit)) return CommandUnitType.CAVALRY;
        if (CommandUnitType.RANGED.matches(recruit)) return CommandUnitType.RANGED;
        return CommandUnitType.INFANTRY;
    }

    private List<CommandUnitSnapshot> unitSnapshots(ServerPlayer player,
                                                    List<AbstractRecruitEntity> recruits,
                                                    Map<UUID, List<UUID>> crews) {
        return recruits.stream()
                .filter(recruit -> !aboardAnObject(player, recruit, crews))
                .map(recruit -> new CommandUnitSnapshot(recruit.getUUID(), recruit.blockPosition(),
                        dominantType(recruit), healthPercent(recruit),
                        recruit.getState(), recruit.getShouldBlock(), canVolley(recruit),
                        recruit.getShouldRanged(), Mth.wrapDegrees(recruit.yBodyRot)))
                .toList();
    }

    private boolean aboardAnObject(ServerPlayer player, AbstractRecruitEntity recruit,
                                   Map<UUID, List<UUID>> crews) {
        Entity vehicle = recruit.getVehicle();
        if (vehicle == null) return false;

        Map<UUID, MapObjectSnapshot> visible = visibleObjects.get(player.getUUID());
        if (visible == null || visible.isEmpty()) return false;

        for (Entity ridden = vehicle; ridden != null; ridden = ridden.getVehicle()) {
            if (!visible.containsKey(ridden.getUUID())) continue;

            if (crews != null && recruit.isEffectedByCommand(player.getUUID())) {
                crews.computeIfAbsent(ridden.getUUID(), ignored -> new ArrayList<>())
                        .add(recruit.getUUID());
            }
            return true;
        }
        return false;
    }

    private List<Watcher> watchPosts(ServerPlayer player) {
        double base = RecruitsRTSCommandServerConfig.SIGHT_RANGE_BLOCKS.get();
        List<Watcher> posts = new ArrayList<>();
        posts.add(new Watcher(player.position(), base));
        if (!RecruitsRTSCommandServerConfig.SCOUTING_ENABLED.get()) return posts;

        ServerLevel level = player.serverLevel();
        for (RecruitsGroup group : getAllGroups(player)) {
            if (!Objects.equals(group.getPlayerUUID(), player.getUUID())) continue;
            for (UUID memberId : group.members) {
                if (level.getEntity(memberId) instanceof AbstractRecruitEntity recruit && recruit.isAlive()) {
                    posts.add(new Watcher(recruit.position(), base * sightFactor(recruit)));
                }
            }
        }
        return posts;
    }

    private double sightFactor(AbstractRecruitEntity recruit) {
        if (recruit instanceof ScoutEntity) return SCOUT_SIGHT;
        if (CommandUnitType.CAVALRY.matches(recruit)) return MOUNTED_SIGHT;
        return 1.0D;
    }

    private Vec3 centerOfSnapshots(List<CommandUnitSnapshot> units) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (CommandUnitSnapshot unit : units) {
            x += unit.position().getX();
            y += unit.position().getY();
            z += unit.position().getZ();
        }
        return new Vec3(x / units.size(), y / units.size(), z / units.size());
    }

    CommandSelection resolveClientSelection(ServerPlayer player, UUID selectionId, List<UUID> members) {
        if (members == null || members.isEmpty()) return null;

        ServerLevel level = player.serverLevel();
        ResourceLocation dimension = level.dimension().location();
        Set<UUID> owned = new LinkedHashSet<>();
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        for (UUID memberId : members) {
            if (level.getEntity(memberId) instanceof AbstractRecruitEntity recruit
                    && recruit.isEffectedByCommand(player.getUUID())
                    && owned.add(memberId)) {
                recruits.add(recruit);
            }
        }
        if (owned.isEmpty()) return null;

        List<UUID> ownedMembers = List.copyOf(owned);
        return new CommandSelection(selectionId, groupIdOf(player, ownedMembers), player.getUUID(), dimension,
                BlockPos.containing(centerOf(recruits)), ownedMembers, unitSnapshots(player, recruits, null));
    }

    private UUID groupIdOf(ServerPlayer player, List<UUID> members) {
        for (RecruitsGroup group : getAllGroups(player)) {
            for (UUID memberId : members) {
                if (group.members.contains(memberId)) {
                    return group.getUUID();
                }
            }
        }
        return null;
    }

    CommandSelection resolveOwnedSelection(ServerPlayer player, UUID selectionId) {
        CommandSelection selection = resolveSelection(player, selectionId);
        if (selection == null || !player.getUUID().equals(selection.ownerId())) return null;
        return resolveClientSelection(player, selectionId, selection.memberIds());
    }

    CommandSelection resolveSelection(ServerPlayer player, UUID selectionId) {
        Map<UUID, CommandSelection> index = playerSelections.get(player.getUUID());
        if (index == null || !index.containsKey(selectionId)) {
            buildSnapshots(player);
            index = playerSelections.get(player.getUUID());
        }
        return index == null ? null : index.get(selectionId);
    }

    boolean canCommand(ServerPlayer player, CommandSelection selection) {
        if (!Objects.equals(player.serverLevel().dimension().location(), selection.dimension())) return false;
        double radius = RecruitsRTSCommandServerConfig.COMMAND_RADIUS_BLOCKS.get();
        return horizontalDistSqr(player.blockPosition(), selection.anchor()) <= radius * radius;
    }

    private List<RecruitsGroup> getAllGroups(ServerPlayer player) {
        MinecraftServer server = player.serverLevel().getServer();
        ServerLevel overworld = server.overworld();
        if (overworld == null) return List.of();

        List<RecruitsGroup> allGroups = RecruitsGroupsSaveData.get(overworld).getAllGroups();
        List<RecruitsGroup> validGroups = new ArrayList<>(allGroups.size());
        for (RecruitsGroup group : allGroups) {
            if (group != null && !group.removed && !group.isDisabled()) validGroups.add(group);
        }
        return validGroups;
    }
}
