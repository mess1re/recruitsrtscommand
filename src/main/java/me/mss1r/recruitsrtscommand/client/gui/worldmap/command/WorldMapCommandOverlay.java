package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import com.talhanation.recruits.client.ClientManager;
import me.mss1r.recruitsrtscommand.api.WorldMapOverlay;
import me.mss1r.recruitsrtscommand.api.WorldMapView;
import me.mss1r.recruitsrtscommand.client.command.MapCommandClientState;
import me.mss1r.recruitsrtscommand.client.command.MapCameraMotion;
import me.mss1r.recruitsrtscommand.client.command.MapCommandNetworkClient;
import me.mss1r.recruitsrtscommand.client.command.SquadClusters;
import me.mss1r.recruitsrtscommand.client.command.MapCommandTool;
import me.mss1r.recruitsrtscommand.client.render.MapRenderUtil;
import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandOrderRoute;
import me.mss1r.recruitsrtscommand.common.command.CommandOrderStatus;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import me.mss1r.recruitsrtscommand.api.MapOrder;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandStyle.*;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandActions.*;
import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandWidgets.*;

public final class WorldMapCommandOverlay implements WorldMapOverlay {
    public static final WorldMapCommandOverlay INSTANCE = new WorldMapCommandOverlay();

    private WorldMapView view;
    private final MapCommandRelations relations = new MapCommandRelations();
    private final MapCommandUnitLayer units = new MapCommandUnitLayer(relations::relationColor);
    private final MapCommandMenus menus = new MapCommandMenus(units, relations);
    private final MapCommandBar bar = new MapCommandBar();
    private final MapCommandGeometry geometry = new MapCommandGeometry();
    private final MapCommandObjects objects = new MapCommandObjects(units, relations, geometry);
    private final MapCommandFireZones zones = new MapCommandFireZones(geometry);
    private final MapCameraMotion cameraMotion = new MapCameraMotion();
    private boolean selectingSquads;
    private long lastBannerClickMillis;
    private UUID lastBannerClicked;
    private boolean selectionMoved;
    private double selectionStartX;
    private double selectionStartY;
    private double selectionCurrentX;
    private double selectionCurrentY;

    private WorldMapCommandOverlay() {
    }

    private void bindView(WorldMapView view) {
        this.view = view;
        units.setView(view);
        bar.setView(view);
        menus.setView(view);
        geometry.setView(view);
        objects.setView(view);
        zones.setView(view);
    }

    public void reset() {
        bindView(null);
        units.reset();
        bar.reset();
        cameraMotion.reset();
        selectingSquads = false;
        zones.reset();
        menus.reset();
        lastBannerClickMillis = 0L;
        lastBannerClicked = null;
        selectionMoved = false;
        selectionStartX = selectionStartY = selectionCurrentX = selectionCurrentY = 0.0D;
    }

    @Override
    public void renderMap(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, WorldMapView view) {
        bindView(view);
        if (!MapCommandClientState.isCommandModeEnabled()) {
            cameraMotion.reset();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        List<CommandGroupSnapshot> snapshots = MapCommandClientState.snapshotsForDimension(minecraft.level);
        Set<UUID> selectedIds = selectedIds(minecraft.level);
        units.refreshClusters(snapshots);
        SquadClusters.Cluster hovered = units.findClusterAtScreen(mouseX, mouseY);

        renderRoutes(graphics);
        objects.renderObjectRoutes(graphics);
        zones.renderFireZones(graphics);
        objects.renderObjects(graphics, minecraft);
        objects.renderObjectSelection(graphics);
        for (CommandGroupSnapshot snapshot : snapshots) {
            units.renderUnitDots(graphics, snapshot, selectedIds.contains(snapshot.selectionId()));
        }
        units.hoveredCluster = hovered;
        for (SquadClusters.Cluster cluster : units.clusters) {
            units.renderSquadMarker(graphics, cluster, commandedShare(cluster), hovered == cluster);
        }
        renderPickTarget(graphics, mouseX, mouseY);
        renderSelectionRectangle(graphics);
        zones.renderZonePreview(graphics);

        graphics.flush();
        panMap();
    }

    @Override
    public void renderUi(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, WorldMapView view) {
        bindView(view);
        Font font = Minecraft.getInstance().font;
        boolean available = MapCommandNetworkClient.available();
        boolean enabled = MapCommandClientState.isCommandModeEnabled();

        drawTextButton(graphics, font, toggleLeft(), TOGGLE_Y, TOGGLE_W, TOGGLE_H,
                I18n.get("gui.recruitsrtscommand.toggle"),
                enabled, available, isOver(mouseX, mouseY, toggleLeft(), TOGGLE_Y, TOGGLE_W, TOGGLE_H));

        if (!enabled) {
            drawTooltip(graphics, font, mouseX, mouseY, tooltipAt(mouseX, mouseY, available), view);
            return;
        }

        bar.renderSelectedStatus(graphics, font, currentSelections());
        menus.renderPendingOrderHint(graphics, font, bar.barTop());
        zones.renderFireZoneHint(graphics, font, bar.barTop());
        bar.renderCommandBar(graphics, font, mouseX, mouseY,
                units.hoveredCluster != null && !menus.hasOpenMenu()
                        ? units.squadTooltip(units.hoveredCluster) : null);
        bar.renderControls(graphics, font);

        menus.renderEnemyMenu(graphics, font, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, WorldMapView view) {
        bindView(view);
        if (isOver(mouseX, mouseY, toggleLeft(), TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            if (MapCommandNetworkClient.available()) {
                MapCommandClientState.toggleCommandMode();
                if (MapCommandClientState.isCommandModeEnabled()) MapCommandNetworkClient.requestSync();
                else reset();
            }
            return true;
        }

        if (!MapCommandClientState.isCommandModeEnabled()) return false;

        if (menus.clickOpenMenu(mouseX, mouseY, button)) return true;

        if (button == 0 && bar.clickCommandBar(mouseX, mouseY)) return true;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return true;

        SquadClusters.Cluster hit = units.findClusterAtScreen(mouseX, mouseY);
        List<CommandGroupSnapshot> selected = MapCommandClientState.selectedForDimensionSnapshots(minecraft.level);
        UUID localPlayerId = minecraft.player.getUUID();

        if (button == 0) {
            if (Screen.hasAltDown()) {
                MapCommandClientState.FireZone zone = zones.zoneAt(mouseX, mouseY);
                if (zone != null) {
                    MapCommandNetworkClient.holdStrategicFire(MapCommandClientState.selectionId(),
                            new ArrayList<>(zone.members()));
                    return true;
                }
            }

            MapObjectSnapshot pressed = objects.objectAtScreen(mouseX, mouseY);
            if (pressed != null && localPlayerId.equals(pressed.ownerId())) {
                boolean append = Screen.hasShiftDown() || Screen.hasControlDown()
                        || MapCommandClientState.activeTool() == MapCommandTool.PICK;
                MapCommandClientState.selectObjects(List.of(pressed.id()), append);
                return true;
            }

            if (MapCommandClientState.activeTool() == MapCommandTool.PICK) {
                UUID picked = unitAtScreen(mouseX, mouseY, localPlayerId);
                if (picked != null) {
                    MapCommandClientState.selectUnits(List.of(picked), true);
                    return true;
                }
            }

            if (hit != null && localPlayerId.equals(hit.ownerId())) {
                selectFromBanner(hit);
                return true;
            }
            if (Screen.hasShiftDown()) {
                startSelection(mouseX, mouseY);
                return true;
            }
            return false;
        }

        if (button != 1) return false;

        if (MapCommandClientState.activeTool() == MapCommandTool.PICK) {
            UUID dropped = unitAtScreen(mouseX, mouseY, localPlayerId);
            if (dropped != null) {
                MapCommandClientState.deselectUnits(List.of(dropped));
                return true;
            }
            MapObjectSnapshot droppedObject = objects.objectAtScreen(mouseX, mouseY);
            if (droppedObject != null && MapCommandClientState.isObjectSelected(droppedObject.id())) {
                MapCommandClientState.deselectObjects(List.of(droppedObject.id()));
                return true;
            }
        }

        if (hit != null && !localPlayerId.equals(hit.ownerId())) {
            menus.openEnemy(hit, mouseX, mouseY);
            return true;
        }

        MapObjectSnapshot object = objects.objectAtScreen(mouseX, mouseY);
        if (object != null) {
            menus.openObject(object, mouseX, mouseY);
            return true;
        }

        if (hit != null) return true;

        if (zones.begin(mouseX, mouseY, selected)) return true;

        if (MapCommandClientState.hasObjectSelection()) {
            BlockPos target = screenToWorld(mouseX, mouseY);
            for (MapObjectSnapshot machine : MapCommandClientState.objects(minecraft.level)) {
                if (machine.takesMoveOrders() && MapCommandClientState.isObjectSelected(machine.id())) {
                    String queued = Screen.hasShiftDown()
                            ? machine.orderAction(MapOrder.MOVE_APPEND)
                            : null;
                    MapCommandNetworkClient.objectAction(machine.id(),
                            queued != null ? queued : machine.orderAction(MapOrder.MOVE), target);
                }
            }
            if (selected.isEmpty()) return true;
        }

        if (selected.isEmpty()) return true;

        if (MapCommandClientState.activeTool() == MapCommandTool.FACE) {
            MapCommandNetworkClient.face(MapCommandClientState.selectionId(),
                    MapCommandClientState.selectedUnitList(), screenToWorld(mouseX, mouseY),
                    Mth.clamp(ClientManager.formationSelection, 0, 8),
                    MapCommandClientState.tightFormation(), MapCommandClientState.holdFormation());
            return true;
        }

        issueMoveOrder(mouseX, mouseY);
        return true;
    }

    private void issueMoveOrder(double mouseX, double mouseY) {
        MapCommandNetworkClient.move(MapCommandClientState.selectionId(),
                MapCommandClientState.selectedUnitList(), screenToWorld(mouseX, mouseY),
                Screen.hasShiftDown(), Mth.clamp(ClientManager.formationSelection, 0, 8),
                MapCommandClientState.tightFormation(), MapCommandClientState.holdFormation());
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, WorldMapView view) {
        bindView(view);
        if (zones.drag(mouseX, mouseY, button)) return true;
        if (button == 2) {
            view.pan(dragX, dragY);
            return true;
        }
        if (button != 0 || !selectingSquads) return false;
        selectionCurrentX = mouseX;
        selectionCurrentY = mouseY;
        selectionMoved = selectionMoved || Math.hypot(selectionCurrentX - selectionStartX, selectionCurrentY - selectionStartY) > 4.0D;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button, WorldMapView view) {
        bindView(view);
        if (zones.release(mouseX, mouseY, button)) return true;
        if (button != 0 || !selectingSquads) return false;
        selectionCurrentX = mouseX;
        selectionCurrentY = mouseY;
        finishSelection(Screen.hasControlDown());
        return true;
    }

    @Override
    public boolean isMouseBlockingMap(double mouseX, double mouseY, WorldMapView view) {
        bindView(view);
        return selectingSquads
                || isOver(mouseX, mouseY, toggleLeft(), TOGGLE_Y, TOGGLE_W, TOGGLE_H)
                || (MapCommandClientState.isCommandModeEnabled()
                    && (bar.barIndexAt(mouseX, mouseY) >= 0 || bar.formationIndexAt(mouseX, mouseY) >= 0
                            || bar.aggroIndexAt(mouseX, mouseY) >= 0
                        || bar.isOverTight(mouseX, mouseY) || bar.isOverHold(mouseX, mouseY)));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers, WorldMapView view) {
        bindView(view);
        if (!MapCommandClientState.isCommandModeEnabled()) return false;

        Minecraft minecraft = Minecraft.getInstance();
        UUID localPlayerId = minecraft.player == null ? null : minecraft.player.getUUID();
        List<CommandGroupSnapshot> selected = currentSelections();

        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            int slot = keyCode - GLFW.GLFW_KEY_1;
            if (Screen.hasControlDown()) {
                MapCommandClientState.storeControlGroup(slot);
            } else {
                MapCommandClientState.recallControlGroup(slot, Screen.hasShiftDown());
            }
            return true;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_TAB -> {
                if (minecraft.level != null) {
                    MapCommandClientState.cycleSelection(minecraft.level, Screen.hasShiftDown() ? -1 : 1, localPlayerId);
                }
                return true;
            }
            case GLFW.GLFW_KEY_F -> {
                MapCommandClientState.setActiveTool(
                        MapCommandClientState.activeTool() == MapCommandTool.FIRE
                                ? MapCommandTool.MOVE : MapCommandTool.FIRE);
                return true;
            }
            case GLFW.GLFW_KEY_Q -> {
                cycleFormation(-1);
                return true;
            }
            case GLFW.GLFW_KEY_E -> {
                cycleFormation(1);
                return true;
            }
            case GLFW.GLFW_KEY_T -> {
                MapCommandClientState.toggleTightFormation();
                return true;
            }
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> {
                if (MapCommandClientState.activeTool() == MapCommandTool.FIRE) {
                    MapCommandClientState.cycleFireZoneShape();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_X -> {
                toggleTool(MapCommandTool.PICK);
                return true;
            }
            case GLFW.GLFW_KEY_R -> {
                toggleTool(MapCommandTool.FACE);
                return true;
            }
            case GLFW.GLFW_KEY_G -> {
                MapCommandClientState.toggleHoldFormation();
                return true;
            }
            case GLFW.GLFW_KEY_F1 -> {
                bar.showControls = !bar.showControls;
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (menus.escape() || zones.cancelGesture()) return true;
                if (bar.aggroOpen) {
                    bar.aggroOpen = false;
                    return true;
                }
                if (bar.formationOpen) {
                    bar.formationOpen = false;
                    return true;
                }
                if (MapCommandClientState.hasSelection()) {
                    MapCommandClientState.clearSelection();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_H -> {
                if (!selected.isEmpty()) {
                    haltSelection();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_K -> {
                if (!selected.isEmpty()) {
                    ceaseFire();
                    return true;
                }
            }
        }
        return false;
    }

    private void renderRoutes(GuiGraphics graphics) {
        for (CommandOrderRoute route : MapCommandClientState.routes(Minecraft.getInstance().level)) {
            List<BlockPos> waypoints = route.waypoints();
            if (waypoints.isEmpty()) continue;

            boolean selected = false;
            for (UUID member : route.members()) {
                if (MapCommandClientState.isUnitSelected(member)) {
                    selected = true;
                    break;
                }
            }

            BlockPos origin = route.origin();
            if (origin == null) continue;

            int color = route.status() == CommandOrderStatus.BLOCKED
                    ? 0xFFE46058 : selected ? PATH_COLOR : 0xA0D6CA62;
            double previousX = worldToScreenX(origin.getX());
            double previousY = worldToScreenY(origin.getZ());
            for (int index = 0; index < waypoints.size(); index++) {
                BlockPos waypoint = waypoints.get(index);
                double x = worldToScreenX(waypoint.getX());
                double y = worldToScreenY(waypoint.getZ());
                geometry.drawPathSegment(graphics, previousX, previousY, x, y, color, selected);
                geometry.drawWaypoint(graphics, x, y, color, index == waypoints.size() - 1);
                previousX = x;
                previousY = y;
            }
        }
        graphics.flush();
    }

    private int toggleLeft() {
        int width = view == null ? 480 : view.screenWidth();
        return Math.max(TOGGLE_MARGIN,
                width - TOGGLE_MARGIN - GEAR_SIZE - GEAR_GAP - TOGGLE_W);
    }

    private String tooltipAt(double mouseX, double mouseY, boolean available) {
        if (isOver(mouseX, mouseY, toggleLeft(), TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            return I18n.get(available
                    ? "gui.recruitsrtscommand.toggle.tooltip"
                    : "gui.recruitsrtscommand.toggle.unavailable");
        }
        return null;
    }

    private void selectFromBanner(SquadClusters.Cluster cluster) {
        long now = System.currentTimeMillis();
        boolean again = cluster.selectionId().equals(lastBannerClicked)
                && now - lastBannerClickMillis <= DOUBLE_CLICK_MILLIS;
        lastBannerClicked = cluster.selectionId();
        lastBannerClickMillis = now;

        List<UUID> men = new ArrayList<>();
        if (Screen.hasShiftDown() || (again && !cluster.whole())) {
            for (CommandUnitSnapshot unit : cluster.snapshot().units()) {
                men.add(unit.unitId());
            }
        } else {
            men.addAll(cluster.members());
        }

        if (Screen.hasAltDown()) {
            MapCommandClientState.deselectUnits(men);
        } else {
            MapCommandClientState.selectUnits(men, Screen.hasControlDown() || again);
        }
    }

    private void renderPickTarget(GuiGraphics graphics, int mouseX, int mouseY) {
        if (MapCommandClientState.activeTool() != MapCommandTool.PICK) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        UUID target = unitAtScreen(mouseX, mouseY, minecraft.player.getUUID());
        if (target == null) return;

        for (CommandGroupSnapshot snapshot : MapCommandClientState.snapshotsForDimension(minecraft.level)) {
            for (CommandUnitSnapshot unit : snapshot.units()) {
                if (!unit.unitId().equals(target)) continue;
                double x = worldToScreenX(units.smoothX(unit.unitId(), unit.position().getX()));
                double y = worldToScreenY(units.smoothZ(unit.unitId(), unit.position().getZ()));
                boolean held = MapCommandClientState.isUnitSelected(target);
                geometry.drawDiamondOutline(graphics, x, y, 5.0D, 1.8D, 0xC0101418);
                geometry.drawDiamondOutline(graphics, x, y, 4.2D, 1.0D, held ? 0xFFFF6E5A : SELECTED_COLOR);
                return;
            }
        }
    }

    private UUID unitAtScreen(double mouseX, double mouseY, UUID localPlayerId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || localPlayerId == null) return null;

        UUID nearest = null;
        double nearestDistanceSqr = PICK_REACH * PICK_REACH;
        for (CommandGroupSnapshot snapshot : MapCommandClientState.snapshotsForDimension(minecraft.level)) {
            if (!localPlayerId.equals(snapshot.ownerId())) continue;
            for (CommandUnitSnapshot unit : snapshot.units()) {
                double x = worldToScreenX(units.smoothX(unit.unitId(), unit.position().getX()));
                double y = worldToScreenY(units.smoothZ(unit.unitId(), unit.position().getZ()));
                double distanceSqr = (mouseX - x) * (mouseX - x) + (mouseY - y) * (mouseY - y);
                if (distanceSqr <= nearestDistanceSqr) {
                    nearestDistanceSqr = distanceSqr;
                    nearest = unit.unitId();
                }
            }
        }
        return nearest;
    }

    private boolean commandedShare(SquadClusters.Cluster cluster) {
        if (!MapCommandClientState.hasSelection()) return false;
        for (UUID member : cluster.members()) {
            if (MapCommandClientState.isUnitSelected(member)) return true;
        }
        return false;
    }

    private BlockPos screenToWorld(double mouseX, double mouseY) {
        return view.screenToWorld(mouseX, mouseY);
    }

    private double worldToScreenX(double worldX) {
        return view.offsetX() + worldX * view.scale();
    }

    private double worldToScreenY(double worldZ) {
        return view.offsetZ() + worldZ * view.scale();
    }

    private void startSelection(double mouseX, double mouseY) {
        selectingSquads = true;
        selectionMoved = false;
        selectionStartX = mouseX;
        selectionStartY = mouseY;
        selectionCurrentX = mouseX;
        selectionCurrentY = mouseY;
    }

    private void finishSelection(boolean append) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            selectingSquads = false;
            return;
        }

        if (!selectionMoved) {
            if (!append) MapCommandClientState.clearSelection();
            selectingSquads = false;
            return;
        }

        double left = Math.min(selectionStartX, selectionCurrentX);
        double right = Math.max(selectionStartX, selectionCurrentX);
        double top = Math.min(selectionStartY, selectionCurrentY);
        double bottom = Math.max(selectionStartY, selectionCurrentY);
        List<UUID> boxed = new ArrayList<>();
        UUID localPlayerId = minecraft.player.getUUID();
        if (units.usesTacticalSymbols()) {
            for (SquadClusters.Cluster cluster : units.clusters) {
                if (!localPlayerId.equals(cluster.ownerId())) continue;
                double x = worldToScreenX(cluster.centerX());
                double y = worldToScreenY(cluster.centerZ());
                if (x >= left && x <= right && y >= top && y <= bottom) boxed.addAll(cluster.members());
            }
        } else for (CommandGroupSnapshot snapshot : MapCommandClientState.snapshotsForDimension(minecraft.level)) {
            if (!localPlayerId.equals(snapshot.ownerId())) continue;
            for (CommandUnitSnapshot unit : snapshot.units()) {
                double x = worldToScreenX(units.smoothX(unit.unitId(), unit.position().getX()));
                double y = worldToScreenY(units.smoothZ(unit.unitId(), unit.position().getZ()));
                if (x >= left && x <= right && y >= top && y <= bottom) {
                    boxed.add(unit.unitId());
                }
            }
        }

        List<UUID> boxedObjects = new ArrayList<>();
        for (MapObjectSnapshot object : MapCommandClientState.objects(minecraft.level)) {
            if (!localPlayerId.equals(object.ownerId())) continue;
            double x = worldToScreenX(object.position().getX());
            double y = worldToScreenY(object.position().getZ());
            if (x >= left && x <= right && y >= top && y <= bottom) {
                boxedObjects.add(object.id());
            }
        }

        if (Screen.hasAltDown()) {
            MapCommandClientState.deselectUnits(boxed);
            MapCommandClientState.deselectObjects(boxedObjects);
        } else if (boxed.isEmpty() && boxedObjects.isEmpty() && !append) {
            MapCommandClientState.clearSelection();
        } else {
            MapCommandClientState.select(boxed, boxedObjects, append);
        }
        selectingSquads = false;
    }

    private void panMap() {
        double speed = cameraMotion.advance(System.nanoTime());
        double dx = 0.0D;
        double dy = 0.0D;
        if (isKeyDown(GLFW.GLFW_KEY_A) || isKeyDown(GLFW.GLFW_KEY_LEFT)) dx += speed;
        if (isKeyDown(GLFW.GLFW_KEY_D) || isKeyDown(GLFW.GLFW_KEY_RIGHT)) dx -= speed;
        if (isKeyDown(GLFW.GLFW_KEY_W) || isKeyDown(GLFW.GLFW_KEY_UP)) dy += speed;
        if (isKeyDown(GLFW.GLFW_KEY_S) || isKeyDown(GLFW.GLFW_KEY_DOWN)) dy -= speed;

        if (dx != 0.0D || dy != 0.0D) {
            view.pan(dx, dy);
        }
    }

    private void renderSelectionRectangle(GuiGraphics graphics) {
        if (!selectingSquads || !selectionMoved) return;
        double left = Math.min(selectionStartX, selectionCurrentX);
        double right = Math.max(selectionStartX, selectionCurrentX);
        double top = Math.min(selectionStartY, selectionCurrentY);
        double bottom = Math.max(selectionStartY, selectionCurrentY);
        MapRenderUtil.fill(graphics, left, top, right, bottom, 0x303A8BFF);
        MapRenderUtil.fill(graphics, left, top, right, top + 1.0D, 0xC08CCBFF);
        MapRenderUtil.fill(graphics, left, bottom - 1.0D, right, bottom, 0xC08CCBFF);
        MapRenderUtil.fill(graphics, left, top, left + 1.0D, bottom, 0xC08CCBFF);
        MapRenderUtil.fill(graphics, right - 1.0D, top, right, bottom, 0xC08CCBFF);
    }

    private Set<UUID> selectedIds(net.minecraft.world.level.Level level) {
        Set<UUID> result = new HashSet<>();
        for (CommandGroupSnapshot snapshot : MapCommandClientState.selectedForDimensionSnapshots(level)) {
            result.add(snapshot.selectionId());
        }
        return result;
    }

    private boolean isKeyDown(int keyCode) {
        Minecraft minecraft = Minecraft.getInstance();
        return GLFW.glfwGetKey(minecraft.getWindow().getWindow(), keyCode) == GLFW.GLFW_PRESS;
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value == null ? "" : value;
        if (maxLength <= 3) return value.substring(0, Math.min(value.length(), maxLength));
        return value.substring(0, maxLength - 3) + "...";
    }

}
