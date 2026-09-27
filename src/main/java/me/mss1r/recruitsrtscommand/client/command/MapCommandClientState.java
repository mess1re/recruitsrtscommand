package me.mss1r.recruitsrtscommand.client.command;

import com.talhanation.recruits.client.gui.CommandScreen;
import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitType;
import me.mss1r.recruitsrtscommand.api.FireZoneShape;
import me.mss1r.recruitsrtscommand.api.MapObjectAction;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import me.mss1r.recruitsrtscommand.common.command.CommandOrderRoute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class MapCommandClientState {
    private static final Map<UUID, CommandGroupSnapshot> SNAPSHOTS = new LinkedHashMap<>();
    private static final List<CommandOrderRoute> ROUTES = new ArrayList<>();
    private static final List<MapObjectSnapshot> OBJECTS = new ArrayList<>();
    private static final List<PendingRoute> PENDING = new ArrayList<>();
    private static final List<FireZone> FIRE_ZONES = new ArrayList<>();
    private static final long ORDER_ECHO_GRACE_MILLIS = 2_000L;

    private static boolean commandModeEnabled = false;
    private static final Set<UUID> SELECTED_UNITS = new LinkedHashSet<>();
    private static final Set<UUID> SELECTED_OBJECTS = new LinkedHashSet<>();
    private static final int MISSING_SYNCS_BEFORE_DROPPED = 4;
    private static final Map<UUID, Integer> MISSING_SYNCS = new HashMap<>();
    private static UUID selectionId = UUID.randomUUID();
    private static boolean manualDeselect = false;
    private static MapCommandTool activeTool = MapCommandTool.MOVE;
    private static FireZoneShape fireZoneShape = FireZoneShape.CIRCLE;

    private MapCommandClientState() {
    }

    public static void updateSnapshots(List<CommandGroupSnapshot> snapshots, UUID localPlayerId) {
        SNAPSHOTS.clear();
        long now = System.currentTimeMillis();
        Set<UUID> seen = new LinkedHashSet<>();
        for (CommandGroupSnapshot snapshot : snapshots) {
            SNAPSHOTS.put(snapshot.selectionId(), snapshot);
            seen.add(snapshot.selectionId());
            MapPositionSmoothing.observe(snapshot.selectionId(),
                    snapshot.anchor().getX(), snapshot.anchor().getZ(), now);
            for (CommandUnitSnapshot unit : snapshot.units()) {
                seen.add(unit.unitId());
                MapPositionSmoothing.observe(unit.unitId(),
                        unit.position().getX(), unit.position().getZ(), now);
            }
        }
        MapPositionSmoothing.retain(seen);

        Set<UUID> living = new LinkedHashSet<>();
        if (localPlayerId != null) {
            for (CommandGroupSnapshot snapshot : snapshots) {
                if (!localPlayerId.equals(snapshot.ownerId())) continue;
                for (CommandUnitSnapshot unit : snapshot.units()) living.add(unit.unitId());
            }
            for (MapObjectSnapshot object : OBJECTS) {
                if (localPlayerId.equals(object.ownerId())) living.addAll(object.crew());
            }
        }
        MISSING_SYNCS.keySet().retainAll(SELECTED_UNITS);
        boolean anyGone = false;
        for (UUID selected : new ArrayList<>(SELECTED_UNITS)) {
            if (living.contains(selected)) {
                MISSING_SYNCS.remove(selected);
                continue;
            }
            int misses = MISSING_SYNCS.merge(selected, 1, Integer::sum);
            if (misses >= MISSING_SYNCS_BEFORE_DROPPED) {
                SELECTED_UNITS.remove(selected);
                MISSING_SYNCS.remove(selected);
                anyGone = true;
            }
        }
        if (anyGone) selectionId = UUID.randomUUID();

        FIRE_ZONES.removeIf(zone -> Collections.disjoint(zone.members(), living));

        if (hasAnything()) return;

        if (manualDeselect || localPlayerId == null) return;
        for (CommandGroupSnapshot snapshot : snapshots) {
            if (localPlayerId.equals(snapshot.ownerId())) {
                selectUnits(unitIds(snapshot), false);
                break;
            }
        }
    }

    private static List<UUID> unitIds(CommandGroupSnapshot snapshot) {
        List<UUID> ids = new ArrayList<>(snapshot.units().size());
        for (CommandUnitSnapshot unit : snapshot.units()) ids.add(unit.unitId());
        return ids;
    }

    public static List<CommandGroupSnapshot> snapshotsForDimension(Level level) {
        if (level == null) return List.of();
        return snapshotsForDimension(level.dimension().location());
    }

    public static List<CommandGroupSnapshot> snapshotsForDimension(ResourceLocation dimension) {
        List<CommandGroupSnapshot> result = new ArrayList<>();
        for (CommandGroupSnapshot snapshot : SNAPSHOTS.values()) {
            if (snapshot.dimension().equals(dimension)) result.add(snapshot);
        }
        return result;
    }

    public static List<CommandGroupSnapshot> selectedForDimensionSnapshots(Level level) {
        if (level == null) return List.of();
        List<CommandGroupSnapshot> result = new ArrayList<>();
        ResourceLocation dimension = level.dimension().location();
        for (CommandGroupSnapshot snapshot : SNAPSHOTS.values()) {
            if (!snapshot.dimension().equals(dimension)) continue;
            for (CommandUnitSnapshot unit : snapshot.units()) {
                if (SELECTED_UNITS.contains(unit.unitId())) {
                    result.add(snapshot);
                    break;
                }
            }
        }
        return result;
    }

    private static final List<Set<UUID>> CONTROL_GROUPS = new ArrayList<>(
            Collections.nCopies(9, Set.<UUID>of()));
    private static final List<Set<UUID>> CONTROL_GROUP_OBJECTS = new ArrayList<>(
            Collections.nCopies(9, Set.<UUID>of()));

    public static void storeControlGroup(int slot) {
        if (slot < 0 || slot >= CONTROL_GROUPS.size()) return;
        CONTROL_GROUPS.set(slot, Set.copyOf(SELECTED_UNITS));
        CONTROL_GROUP_OBJECTS.set(slot, Set.copyOf(SELECTED_OBJECTS));
    }

    public static void recallControlGroup(int slot, boolean append) {
        if (slot < 0 || slot >= CONTROL_GROUPS.size()) return;
        select(CONTROL_GROUPS.get(slot), CONTROL_GROUP_OBJECTS.get(slot), append);
    }

    public static Set<UUID> selectedUnits() {
        return Set.copyOf(SELECTED_UNITS);
    }

    public static List<UUID> selectedUnitList() {
        return List.copyOf(SELECTED_UNITS);
    }

    public static boolean isUnitSelected(UUID unitId) {
        return SELECTED_UNITS.contains(unitId);
    }

    public static boolean hasSelection() {
        return !SELECTED_UNITS.isEmpty();
    }

    public static boolean hasAnything() {
        return !SELECTED_UNITS.isEmpty() || !SELECTED_OBJECTS.isEmpty();
    }

    public static Set<UUID> selectedObjects() {
        return Set.copyOf(SELECTED_OBJECTS);
    }

    public static boolean isObjectSelected(UUID objectId) {
        return SELECTED_OBJECTS.contains(objectId);
    }

    public static boolean hasObjectSelection() {
        return !SELECTED_OBJECTS.isEmpty();
    }

    public static void selectObjects(Collection<UUID> objectIds, boolean append) {
        select(null, objectIds, append);
    }

    public static void select(Collection<UUID> unitIds, Collection<UUID> objectIds, boolean append) {
        if (!append) {
            SELECTED_UNITS.clear();
            SELECTED_OBJECTS.clear();
        }
        if (unitIds != null) SELECTED_UNITS.addAll(unitIds);
        if (objectIds != null) SELECTED_OBJECTS.addAll(objectIds);
        selectionId = UUID.randomUUID();
        manualDeselect = SELECTED_UNITS.isEmpty() && SELECTED_OBJECTS.isEmpty();
    }

    public static void deselectObjects(Collection<UUID> objectIds) {
        SELECTED_OBJECTS.removeAll(objectIds);
        selectionId = UUID.randomUUID();
        manualDeselect = SELECTED_UNITS.isEmpty() && SELECTED_OBJECTS.isEmpty();
    }

    public static void clearObjectSelection() {
        SELECTED_OBJECTS.clear();
    }

    public static List<UUID> selectedGunners() {
        if (SELECTED_OBJECTS.isEmpty()) return selectedUnitList();

        Set<UUID> gunners = new LinkedHashSet<>(SELECTED_UNITS);
        for (MapObjectSnapshot object : OBJECTS) {
            if (SELECTED_OBJECTS.contains(object.id())) gunners.addAll(object.crew());
        }
        return List.copyOf(gunners);
    }

    public static UUID selectionId() {
        return selectionId;
    }

    public static void selectUnits(Collection<UUID> unitIds, boolean append) {
        select(unitIds, null, append);
    }

    public static void retainType(CommandUnitType type) {
        if (type == null || type == CommandUnitType.ALL) return;

        Set<UUID> kept = new LinkedHashSet<>();
        for (CommandGroupSnapshot snapshot : SNAPSHOTS.values()) {
            for (CommandUnitSnapshot unit : snapshot.units()) {
                if (unit.type() == type && SELECTED_UNITS.contains(unit.unitId())) {
                    kept.add(unit.unitId());
                }
            }
        }
        if (kept.isEmpty() || (kept.size() == SELECTED_UNITS.size() && SELECTED_OBJECTS.isEmpty())) {
            return;
        }

        SELECTED_OBJECTS.clear();
        SELECTED_UNITS.clear();
        SELECTED_UNITS.addAll(kept);
        selectionId = UUID.randomUUID();
        manualDeselect = false;
    }

    public static void deselectUnits(Collection<UUID> unitIds) {
        SELECTED_UNITS.removeAll(unitIds);
        selectionId = UUID.randomUUID();
        manualDeselect = SELECTED_UNITS.isEmpty() && SELECTED_OBJECTS.isEmpty();
    }

    public static void clearSelection() {
        SELECTED_OBJECTS.clear();
        SELECTED_UNITS.clear();
        selectionId = UUID.randomUUID();
        manualDeselect = true;
    }

    public static void cycleSelection(Level level, int direction, UUID localPlayerId) {
        if (localPlayerId == null) {
            clearSelection();
            return;
        }

        List<CommandGroupSnapshot> own = snapshotsForDimension(level).stream()
                .filter(snapshot -> localPlayerId.equals(snapshot.ownerId()))
                .toList();
        if (own.isEmpty()) {
            clearSelection();
            return;
        }

        Set<UUID> held = selectedSquadIds();
        int current = -1;
        for (int i = 0; i < own.size(); i++) {
            if (held.contains(own.get(i).selectionId())) {
                current = i;
                break;
            }
        }

        int step = direction < 0 ? -1 : 1;
        int next = current < 0 ? 0 : Math.floorMod(current + step, own.size());
        selectUnits(unitIds(own.get(next)), false);
    }

    public static boolean isCommandModeEnabled() {
        return commandModeEnabled;
    }

    public static void setCommandModeEnabled(boolean value) {
        commandModeEnabled = value;
    }

    public static void toggleCommandMode() {
        commandModeEnabled = !commandModeEnabled;
    }

    public static boolean tightFormation() {
        return CommandScreen.tightFormation;
    }

    public static void toggleTightFormation() {
        CommandScreen.tightFormation = !CommandScreen.tightFormation;
    }

    public static boolean holdFormation() {
        return CommandScreen.holdFormation;
    }

    public static void toggleHoldFormation() {
        CommandScreen.holdFormation = !CommandScreen.holdFormation;
    }

    public static MapCommandTool activeTool() {
        return activeTool;
    }

    public static void setActiveTool(MapCommandTool tool) {
        activeTool = tool == null ? MapCommandTool.MOVE : tool;
    }

    public static void clear() {
        SNAPSHOTS.clear();
        MapPositionSmoothing.clear();
        SELECTED_UNITS.clear();
        SELECTED_OBJECTS.clear();
        ROUTES.clear();
        OBJECTS.clear();
        PENDING.clear();
        FIRE_ZONES.clear();
        manualDeselect = false;
    }

    public static void rememberMoveOrder(BlockPos target, boolean append) {
        if (target == null || SELECTED_UNITS.isEmpty()) return;

        List<UUID> men = List.copyOf(SELECTED_UNITS);
        List<BlockPos> waypoints = new ArrayList<>();
        PendingRoute existing = null;
        for (PendingRoute pending : PENDING) {
            if (pending.route.members().equals(men)) {
                existing = pending;
                break;
            }
        }
        if (append && existing != null) waypoints.addAll(existing.route.waypoints());
        waypoints.add(target.immutable());

        PENDING.remove(existing);
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation dimension = minecraft.level == null
                ? ResourceLocation.withDefaultNamespace("overworld")
                : minecraft.level.dimension().location();
        PENDING.add(new PendingRoute(
                new CommandOrderRoute(men, dimension, selectedCenter(), waypoints),
                System.currentTimeMillis()));
    }

    public static void updateRoutes(List<CommandOrderRoute> routes) {
        ROUTES.clear();
        if (routes != null) ROUTES.addAll(routes);

        long now = System.currentTimeMillis();
        PENDING.removeIf(pending -> now - pending.createdAtMillis > ORDER_ECHO_GRACE_MILLIS);
    }

    public static void updateObjects(List<MapObjectSnapshot> objects, UUID localPlayerId) {
        OBJECTS.clear();
        if (objects != null) OBJECTS.addAll(objects);

        Set<UUID> selectable = new LinkedHashSet<>();
        for (MapObjectSnapshot object : OBJECTS) {
            if (localPlayerId != null && localPlayerId.equals(object.ownerId())) {
                selectable.add(object.id());
            }
        }
        if (SELECTED_OBJECTS.retainAll(selectable)) {
            selectionId = UUID.randomUUID();
        }
    }

    public static void selectObjectOption(UUID objectId, String group, String actionId) {
        if (objectId == null || group == null || actionId == null) return;
        for (int objectIndex = 0; objectIndex < OBJECTS.size(); objectIndex++) {
            MapObjectSnapshot object = OBJECTS.get(objectIndex);
            if (!objectId.equals(object.id())) continue;

            List<MapObjectAction> actions = new ArrayList<>(object.actions().size());
            for (MapObjectAction action : object.actions()) {
                actions.add(group.equals(action.group())
                        ? action.withSelected(actionId.equals(action.id()))
                        : action);
            }
            OBJECTS.set(objectIndex, object.withActions(actions));
            return;
        }
    }

    static MapObjectSnapshot objectSnapshot(UUID objectId) {
        for (MapObjectSnapshot object : OBJECTS) {
            if (object.id().equals(objectId)) return object;
        }
        return null;
    }

    public static List<MapObjectSnapshot> objects(Level level) {
        if (level == null) return List.of();
        ResourceLocation here = level.dimension().location();

        List<MapObjectSnapshot> inWorld = new ArrayList<>();
        for (MapObjectSnapshot object : OBJECTS) {
            if (object.dimension().equals(here)) inWorld.add(object);
        }
        return inWorld;
    }

    public static List<CommandOrderRoute> routes(Level level) {
        if (level == null) return List.of();
        ResourceLocation here = level.dimension().location();

        List<CommandOrderRoute> all = new ArrayList<>();
        for (CommandOrderRoute route : ROUTES) {
            if (route.dimension().equals(here)) all.add(route);
        }
        for (PendingRoute pending : PENDING) {
            if (!pending.route.dimension().equals(here)) continue;
            boolean covered = false;
            for (CommandOrderRoute known : ROUTES) {
                if (!Collections.disjoint(known.members(), pending.route.members())) {
                    covered = true;
                    break;
                }
            }
            if (!covered) all.add(pending.route);
        }
        return all;
    }

    private record PendingRoute(CommandOrderRoute route, long createdAtMillis) {
    }

    public static Set<UUID> selectedSquadIds() {
        Set<UUID> squads = new LinkedHashSet<>();
        for (CommandGroupSnapshot snapshot : SNAPSHOTS.values()) {
            for (CommandUnitSnapshot unit : snapshot.units()) {
                if (SELECTED_UNITS.contains(unit.unitId())) {
                    squads.add(snapshot.selectionId());
                    break;
                }
            }
        }
        return squads;
    }

    public static BlockPos selectedCenter() {
        long x = 0L;
        long y = 0L;
        long z = 0L;
        int count = 0;
        for (CommandGroupSnapshot snapshot : SNAPSHOTS.values()) {
            for (CommandUnitSnapshot unit : snapshot.units()) {
                if (!SELECTED_UNITS.contains(unit.unitId())) continue;
                x += unit.position().getX();
                y += unit.position().getY();
                z += unit.position().getZ();
                count++;
            }
        }
        return count == 0 ? null : new BlockPos((int) (x / count), (int) (y / count), (int) (z / count));
    }

    public static void rememberStrategicFireOrder(Collection<UUID> members, BlockPos target,
                                                  int radiusX, int radiusZ, FireZoneShape shape) {
        if (target == null || members == null || members.isEmpty()) return;

        Set<UUID> assigned = new LinkedHashSet<>(members);
        releaseFromZones(assigned);
        FIRE_ZONES.add(new FireZone(Set.copyOf(assigned), target.immutable(), radiusX, radiusZ, shape,
                reachesCrew(assigned), System.currentTimeMillis()));
    }

    public static void releaseFromZones(Collection<UUID> members) {
        if (members == null || members.isEmpty()) return;
        List<FireZone> kept = new ArrayList<>(FIRE_ZONES.size());
        for (FireZone zone : FIRE_ZONES) {
            Set<UUID> left = new LinkedHashSet<>(zone.members());
            left.removeAll(members);
            if (!left.isEmpty()) {
                kept.add(new FireZone(Set.copyOf(left), zone.target(), zone.radiusX(), zone.radiusZ(),
                        zone.shape(), reachesCrew(left), zone.createdAtMillis()));
            }
        }
        FIRE_ZONES.clear();
        FIRE_ZONES.addAll(kept);
    }

    private static boolean reachesCrew(Set<UUID> members) {
        for (MapObjectSnapshot object : OBJECTS) {
            for (UUID member : object.crew()) {
                if (members.contains(member)) return true;
            }
        }
        return false;
    }

    public static List<FireZone> fireZones() {
        return Collections.unmodifiableList(FIRE_ZONES);
    }

    public record FireZone(Set<UUID> members, BlockPos target, int radiusX, int radiusZ,
                           FireZoneShape shape, boolean siege, long createdAtMillis) {
        public boolean commanded(Set<UUID> selection) {
            for (UUID member : members) {
                if (selection.contains(member)) return true;
            }
            return false;
        }
    }

    public static FireZoneShape fireZoneShape() {
        return fireZoneShape;
    }

    public static void cycleFireZoneShape() {
        fireZoneShape = fireZoneShape.next();
    }

}
