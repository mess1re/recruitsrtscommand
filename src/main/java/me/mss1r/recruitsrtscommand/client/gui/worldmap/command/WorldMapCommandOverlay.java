package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import me.mss1r.recruitsrtscommand.api.WorldMapOverlay;
import me.mss1r.recruitsrtscommand.api.WorldMapView;
import me.mss1r.recruitsrtscommand.client.command.MapCommandClientState;
import me.mss1r.recruitsrtscommand.client.command.ClientGroups;
import me.mss1r.recruitsrtscommand.client.command.MapCommandNetworkClient;
import me.mss1r.recruitsrtscommand.client.command.SquadClusters;
import me.mss1r.recruitsrtscommand.client.command.MapPositionSmoothing;
import me.mss1r.recruitsrtscommand.client.command.MapCommandTool;
import me.mss1r.recruitsrtscommand.client.render.MapRenderUtil;
import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandOrderRoute;
import me.mss1r.recruitsrtscommand.api.MapObjectAction;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import me.mss1r.recruitsrtscommand.api.MapOrder;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitType;
import me.mss1r.recruitsrtscommand.api.FireZoneShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class WorldMapCommandOverlay implements WorldMapOverlay {
    public static final WorldMapCommandOverlay INSTANCE = new WorldMapCommandOverlay();

    private static final int TOGGLE_Y = 10;
    private static final int TOGGLE_MARGIN = 10;
    private static final int GEAR_SIZE = 20;
    private static final int GEAR_GAP = 4;
    private static final int TOGGLE_W = 42;
    private static final int TOGGLE_H = 20;
    private static final int STATUS_X = 10;
    private static final int STATUS_Y = 60;
    private static final int BUTTON_SIZE = 22;
    private static final int TIGHT_WIDTH = BUTTON_SIZE;
    private static final int BUTTON_GAP = 2;
    private static final int BUTTON_BG = 0x80222631;
    private static final int BUTTON_HOVERED = 0xA0444B5C;
    private static final int BUTTON_SELECTED = 0xB02E6042;
    private static final int BUTTON_DISABLED = 0x50222631;
    private static final int OUTLINE = 0x50FFFFFF;
    private static final int OUTLINE_HOVERED = 0x90FFFFFF;
    private static final int OUTLINE_SELECTED = 0xFF2BEA68;
    private static final int OWN_COLOR = 0xFF2BEA68;
    private static final int ALLY_COLOR = 0xFF49D5FF;
    private static final int NEUTRAL_COLOR = 0xFFFFD45A;
    private static final int ENEMY_COLOR = 0xFFFF5555;
    private static final int PATH_COLOR = 0xE8FFE66A;
    private static final int FIRE_ZONE_COLOR = 0xE8FF665A;
    private static final int SIEGE_ZONE_COLOR = 0xE8FFA83A;
    private static final int SELECTED_COLOR = 0xFFFFFFFF;
    private static final int MARKER_SELECTED_COLOR = 0xFF4FA3FF;
    private static final int HUD_ICON_SIZE = 21;
    private static final int TEXTURE_ICON_SIZE = 21;
    private static final int ORDER_PREVIEW_LIMIT = 60;
    private static final int MARKER_ICON = 21;
    private static final double OBJECT_ICON_FLOOR = 3.5D;
    private static final double MARKER_ICON_FLOOR = 5.0D;
    private static final double MARKER_LIFT_MIN = 6.0D;
    private static final double MARKER_HIT_MIN = 7.0D;

    private static final double PICK_REACH = 6.0D;
    private static final long DOUBLE_CLICK_MILLIS = 350L;
    private static final int[][] OUTLINE_OFFSETS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
    private static final double UNIT_DOT_MIN_ZOOM = 0.60D;
    private static final double MAX_ZONE_RADIUS = 48.0D;
    private static final double ZONE_DRAG_SLOP = 3.0D;
    private static final int BUTTON_FORMATION = 0;
    private static final int BUTTON_FIRE_ZONE = 1;
    private static final int BUTTON_HALT = 2;
    private static final int BUTTON_CEASE_FIRE = 3;
    private static final int BUTTON_FIRE_AT_WILL = 4;
    private static final int BUTTON_FACE = 5;
    private static final int BUTTON_PICK = 6;
    private static final int BUTTON_AGGRO = 7;
    private static final int BUTTON_SHIELDS = 8;
    private static final int BAR_BUTTONS = 9;
    private static final double PAN_SPEED = 7.0D;
    private static final int[] FORMATION_CYCLE = {7, 4, 2, 5, 1, 3, 6, 8, 0};

    private static final ResourceLocation ICON_FORMATION_NONE = recruitsIcon("textures/gui/image/none.png");
    private static final ResourceLocation ICON_FORMATION_LINE = recruitsIcon("textures/gui/image/line.png");
    private static final ResourceLocation ICON_FORMATION_SQUARE = recruitsIcon("textures/gui/image/square.png");
    private static final ResourceLocation ICON_FORMATION_TRIANGLE = recruitsIcon("textures/gui/image/triangle.png");
    private static final ResourceLocation ICON_FORMATION_HCIRCLE = recruitsIcon("textures/gui/image/hcircle.png");
    private static final ResourceLocation ICON_FORMATION_HSQUARE = recruitsIcon("textures/gui/image/hsquare.png");
    private static final ResourceLocation ICON_FORMATION_VFORM = recruitsIcon("textures/gui/image/vform.png");
    private static final ResourceLocation ICON_FORMATION_CIRCLE = recruitsIcon("textures/gui/image/circle.png");
    private static final ResourceLocation ICON_FORMATION_MOVEMENT = recruitsIcon("textures/gui/image/movement.png");
    private static final ResourceLocation ICON_FIRE_ZONE = recruitsIcon("textures/gui/image/group/3arrow.png");
    private static final ResourceLocation ICON_FIRE_AT_WILL = recruitsIcon("textures/gui/image/group/bow.png");
    private static final ResourceLocation ICON_FACE = recruitsIcon("textures/gui/image/group/arrow.png");
    private static final ResourceLocation ICON_PICK = recruitsIcon("textures/gui/image/group/spyglass.png");
    private static final int ENEMY_MENU_LINE = 9;
    private static final int ENEMY_MENU_ROW = 13;
    private static final int ENEMY_MENU_HINT_LINES = 2;
    private static final int ENEMY_MENU_HEAD = 26;
    private static final int ENEMY_MENU_PAD = 8;
    private static final int OBJECT_ROUTE_COLOR = 0xA07FB2C8;

    private static final int ENEMY_MENU_MAX_WIDTH = 240;
    private static final int ENEMY_MENU_MIN_WIDTH = 150;
    private static final float ENEMY_MENU_Z = 450.0F;
    private static final int MENU_BACK = 0xD0000000;
    private static final int MENU_EDGE = 0xFFFFFFFF;
    private static final int MENU_RULE = 0x66FFFFFF;
    private static final int MENU_ROW_HOVERED = 0x33FFFFFF;
    private static final int MENU_LABEL = 0xFFB8B8B8;
    private static final int MENU_VALUE = 0xFFFFFFFF;
    private static final int MENU_TEXT_OFF = 0xFF777777;
    private static final ResourceLocation ICON_CEASE_FIRE = recruitsIcon("textures/gui/image/none.png");
    private static final ResourceLocation ICON_AGGRO_NEUTRAL = recruitsIcon("textures/gui/image/neutral.png");
    private static final ResourceLocation ICON_AGGRO_AGGRESSIVE = recruitsIcon("textures/gui/image/enemy.png");
    private static final ResourceLocation ICON_AGGRO_RAID = recruitsIcon("textures/gui/image/group/axe.png");
    private static final ResourceLocation ICON_AGGRO_PASSIVE = recruitsIcon("textures/gui/image/ally.png");
    private static final ResourceLocation ICON_SHIELDS = recruitsIcon("textures/gui/image/group/shield.png");
    private static final ResourceLocation ICON_HOLD = ownIcon("textures/gui/hold_position.png");
    private static final ResourceLocation ICON_INFANTRY = recruitsIcon("textures/gui/image/group/sword.png");
    private static final ResourceLocation ICON_RANGED = recruitsIcon("textures/gui/image/group/bow.png");
    private static final ResourceLocation ICON_CAVALRY = recruitsIcon("textures/gui/image/group/horse.png");

    private WorldMapView view;
    private boolean selectingSquads;
    private boolean showControls;
    private boolean drawingZone;
    private MapCommandClientState.FireZone movingZone;
    private double movingGrabX;
    private double movingGrabY;
    private boolean movingZoneDragged;
    private boolean formationOpen;
    private boolean aggroOpen;
    private SquadClusters.Cluster hoveredCluster;
    private final List<TypeSegment> typeSegments = new ArrayList<>();
    private SquadClusters.Cluster enemyMenuTarget;
    private MapObjectSnapshot objectMenuTarget;
    private static final String ATTACK_OBJECT = "recruitsrtscommand:attack";
    private PendingObjectOrder pendingObjectOrder;
    private double enemyMenuX;
    private double enemyMenuY;
    private List<SquadClusters.Cluster> clusters = List.of();
    private long lastBannerClickMillis;
    private UUID lastBannerClicked;
    private double zoneCenterX;
    private double zoneCenterY;
    private double zoneEdgeX;
    private double zoneEdgeY;
    private boolean selectionMoved;
    private double selectionStartX;
    private double selectionStartY;
    private double selectionCurrentX;
    private double selectionCurrentY;

    private WorldMapCommandOverlay() {
    }

    @Override
    public void renderMap(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, WorldMapView view) {
        this.view = view;
        if (!MapCommandClientState.isCommandModeEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        List<CommandGroupSnapshot> snapshots = MapCommandClientState.snapshotsForDimension(minecraft.level);
        Set<UUID> selectedIds = selectedIds(minecraft.level);
        clusters = SquadClusters.of(snapshots);
        SquadClusters.Cluster hovered = findClusterAtScreen(mouseX, mouseY);

        renderRoutes(graphics);
        renderObjectRoutes(graphics);
        renderFireZones(graphics);
        renderObjects(graphics, minecraft);
        renderObjectSelection(graphics);
        for (CommandGroupSnapshot snapshot : snapshots) {
            renderUnitDots(graphics, snapshot, selectedIds.contains(snapshot.selectionId()));
        }
        hoveredCluster = hovered;
        for (SquadClusters.Cluster cluster : clusters) {
            renderSquadMarker(graphics, cluster, commandedShare(cluster), hovered == cluster);
        }
        renderPickTarget(graphics, mouseX, mouseY);
        renderSelectionRectangle(graphics);
        renderZonePreview(graphics);

        graphics.flush();
        panMap();
    }

    @Override
    public void renderUi(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, WorldMapView view) {
        this.view = view;
        Font font = Minecraft.getInstance().font;
        boolean available = MapCommandNetworkClient.available();
        boolean enabled = MapCommandClientState.isCommandModeEnabled();

        drawTextButton(graphics, font, toggleLeft(), TOGGLE_Y, TOGGLE_W, TOGGLE_H,
                I18n.get("gui.recruitsrtscommand.toggle"),
                enabled, available, isOver(mouseX, mouseY, toggleLeft(), TOGGLE_Y, TOGGLE_W, TOGGLE_H));

        if (!enabled) {
            drawTooltip(graphics, font, mouseX, mouseY, tooltipAt(mouseX, mouseY, available));
            return;
        }

        renderSelectedStatus(graphics, font, currentSelections());
        renderPendingOrderHint(graphics, font);
        renderFireZoneHint(graphics, font);
        renderCommandBar(graphics, font, mouseX, mouseY);
        renderControls(graphics, font);

        renderEnemyMenu(graphics, font, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, WorldMapView view) {
        this.view = view;
        if (isOver(mouseX, mouseY, toggleLeft(), TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            if (MapCommandNetworkClient.available()) {
                MapCommandClientState.toggleCommandMode();
                if (MapCommandClientState.isCommandModeEnabled()) MapCommandNetworkClient.requestSync();
            }
            return true;
        }

        if (!MapCommandClientState.isCommandModeEnabled()) return false;

        if (objectMenuTarget != null) {
            int row = enemyMenuRowAt(mouseX, mouseY);
            MapObjectSnapshot object = freshObjectMenuTarget();
            if (object == null) return true;
            List<List<MapObjectAction>> rows = menuRows(objectMenuActions(object));
            if (row < 0 || row >= rows.size()) {
                objectMenuTarget = null;
                return true;
            }

            List<MapObjectAction> options = rows.get(row);
            if (options.size() > 1) {
                MapObjectAction next = nextOption(options);
                if (next != null) {
                    MapCommandClientState.selectObjectOption(object.id(), next.group(), next.id());
                    MapCommandNetworkClient.objectAction(object.id(), next.id());
                }
                return true;
            }

            objectMenuTarget = null;
            MapObjectAction action = options.get(0);
            if (action.enabled()) {
                if (ATTACK_OBJECT.equals(action.id())) {
                    MapCommandNetworkClient.attackObject(MapCommandClientState.selectionId(),
                            MapCommandClientState.selectedUnitList(), object.id());
                } else if (action.needsPoint()) {
                    pendingObjectOrder = new PendingObjectOrder(object.id(), action.id(),
                            action.label().getString());
                } else {
                    MapCommandNetworkClient.objectAction(object.id(), action.id());
                }
            }
            return true;
        }

        if (pendingObjectOrder != null) {
            PendingObjectOrder pending = pendingObjectOrder;
            pendingObjectOrder = null;
            if (button == 1) {
                MapCommandNetworkClient.objectAction(pending.objectId(), pending.actionId(),
                        screenToWorld(mouseX, mouseY));
            }
            return true;
        }

        if (enemyMenuTarget != null) {
            int row = enemyMenuRowAt(mouseX, mouseY);
            SquadClusters.Cluster enemy = enemyMenuTarget;
            enemyMenuTarget = null;
            if (row == 0 && mayAttack(enemy.ownerId())) orderAgainstEnemy(enemy);
            return true;
        }

        if (button == 0 && clickCommandBar(mouseX, mouseY)) return true;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return true;

        SquadClusters.Cluster hit = findClusterAtScreen(mouseX, mouseY);
        List<CommandGroupSnapshot> selected = MapCommandClientState.selectedForDimensionSnapshots(minecraft.level);
        UUID localPlayerId = minecraft.player.getUUID();

        if (button == 0) {
            if (Screen.hasAltDown()) {
                MapCommandClientState.FireZone zone = zoneAt(mouseX, mouseY);
                if (zone != null) {
                    MapCommandNetworkClient.holdStrategicFire(MapCommandClientState.selectionId(),
                            new ArrayList<>(zone.members()));
                    return true;
                }
            }

            MapObjectSnapshot pressed = objectAtScreen(mouseX, mouseY);
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
            MapObjectSnapshot droppedObject = objectAtScreen(mouseX, mouseY);
            if (droppedObject != null && MapCommandClientState.isObjectSelected(droppedObject.id())) {
                MapCommandClientState.deselectObjects(List.of(droppedObject.id()));
                return true;
            }
        }

        if (hit != null && !localPlayerId.equals(hit.ownerId())) {
            enemyMenuTarget = hit;
            enemyMenuX = mouseX;
            enemyMenuY = mouseY;
            return true;
        }

        MapObjectSnapshot object = objectAtScreen(mouseX, mouseY);
        if (object != null) {
            objectMenuTarget = object;
            enemyMenuX = mouseX;
            enemyMenuY = mouseY;
            return true;
        }

        if (hit != null) return true;

        MapCommandClientState.FireZone existing = zoneAt(mouseX, mouseY);
        if (existing != null) {
            movingZone = existing;
            movingZoneDragged = false;
            zoneCenterX = worldToScreenX(existing.target().getX());
            zoneCenterY = worldToScreenY(existing.target().getZ());
            movingGrabX = mouseX - zoneCenterX;
            movingGrabY = mouseY - zoneCenterY;
            return true;
        }

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

        if (MapCommandClientState.activeTool() == MapCommandTool.FIRE) {
            drawingZone = true;
            zoneCenterX = mouseX;
            zoneCenterY = mouseY;
            zoneEdgeX = mouseX;
            zoneEdgeY = mouseY;
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

    private MapCommandClientState.FireZone zoneAt(double mouseX, double mouseY) {
        List<MapCommandClientState.FireZone> zones = MapCommandClientState.fireZones();
        for (int i = zones.size() - 1; i >= 0; i--) {
            MapCommandClientState.FireZone zone = zones.get(i);
            double centerX = worldToScreenX(zone.target().getX());
            double centerY = worldToScreenY(zone.target().getZ());
            double radiusX = Math.max(4.0D, zone.radiusX() * view.scale());
            double radiusZ = Math.max(4.0D, zone.radiusZ() * view.scale());
            if (zone.shape().contains(mouseX - centerX, mouseY - centerY, radiusX, radiusZ)) {
                return zone;
            }
        }
        return null;
    }

    private static final int MIN_ZONE_RADIUS = 2;

    private void issueZoneOrder() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        List<CommandGroupSnapshot> selected = MapCommandClientState.selectedForDimensionSnapshots(minecraft.level);
        if (selected.isEmpty()) return;

        int[] reach = zoneReachBlocks();
        if (reach[0] < MIN_ZONE_RADIUS || reach[1] < MIN_ZONE_RADIUS) {
            return;
        }
        MapCommandNetworkClient.strategicFire(MapCommandClientState.selectionId(),
                MapCommandClientState.selectedGunners(), screenToWorld(zoneCenterX, zoneCenterY),
                reach[0], reach[1], MapCommandClientState.fireZoneShape());
    }

    private int[] zoneReachBlocks() {
        BlockPos center = screenToWorld(zoneCenterX, zoneCenterY);
        BlockPos edge = screenToWorld(zoneEdgeX, zoneEdgeY);
        double dx = Math.abs(edge.getX() - center.getX());
        double dz = Math.abs(edge.getZ() - center.getZ());

        if (MapCommandClientState.fireZoneShape() == FireZoneShape.CIRCLE) {
            int radius = (int) Math.round(Math.min(Math.hypot(dx, dz), MAX_ZONE_RADIUS));
            return new int[]{radius, radius};
        }
        return new int[]{
                (int) Math.round(Math.min(dx, MAX_ZONE_RADIUS)),
                (int) Math.round(Math.min(dz, MAX_ZONE_RADIUS))
        };
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, WorldMapView view) {
        this.view = view;
        if (button == 1 && movingZone != null) {
            double wantX = mouseX - movingGrabX;
            double wantY = mouseY - movingGrabY;
            if (Math.hypot(wantX - worldToScreenX(movingZone.target().getX()),
                    wantY - worldToScreenY(movingZone.target().getZ())) > ZONE_DRAG_SLOP) {
                movingZoneDragged = true;
            }
            zoneCenterX = wantX;
            zoneCenterY = wantY;
            return true;
        }
        if (button == 1 && drawingZone) {
            zoneEdgeX = mouseX;
            zoneEdgeY = mouseY;
            return true;
        }
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
        this.view = view;
        if (button == 1 && movingZone != null) {
            MapCommandClientState.FireZone zone = movingZone;
            boolean dragged = movingZoneDragged;
            movingZone = null;
            movingZoneDragged = false;

            if (!dragged) {
                MapCommandClientState.selectUnits(zone.members(), Screen.hasControlDown());
                return true;
            }

            zoneCenterX = mouseX - movingGrabX;
            zoneCenterY = mouseY - movingGrabY;
            MapCommandNetworkClient.strategicFire(MapCommandClientState.selectionId(),
                    new ArrayList<>(zone.members()), screenToWorld(zoneCenterX, zoneCenterY),
                    zone.radiusX(), zone.radiusZ(), zone.shape());
            return true;
        }
        if (button == 1 && drawingZone) {
            zoneEdgeX = mouseX;
            zoneEdgeY = mouseY;
            drawingZone = false;
            issueZoneOrder();
            return true;
        }
        if (button != 0 || !selectingSquads) return false;
        selectionCurrentX = mouseX;
        selectionCurrentY = mouseY;
        finishSelection(Screen.hasControlDown());
        return true;
    }

    @Override
    public boolean isMouseBlockingMap(double mouseX, double mouseY, WorldMapView view) {
        this.view = view;
        return selectingSquads
                || isOver(mouseX, mouseY, toggleLeft(), TOGGLE_Y, TOGGLE_W, TOGGLE_H)
                || (MapCommandClientState.isCommandModeEnabled()
                    && (barIndexAt(mouseX, mouseY) >= 0 || formationIndexAt(mouseX, mouseY) >= 0
                            || aggroIndexAt(mouseX, mouseY) >= 0
                        || isOverTight(mouseX, mouseY) || isOverHold(mouseX, mouseY)));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers, WorldMapView view) {
        this.view = view;
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
                showControls = !showControls;
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (pendingObjectOrder != null) {
                    pendingObjectOrder = null;
                    return true;
                }
                if (enemyMenuTarget != null || objectMenuTarget != null) {
                    enemyMenuTarget = null;
                    objectMenuTarget = null;
                    return true;
                }
                if (aggroOpen) {
                    aggroOpen = false;
                    return true;
                }
                if (formationOpen) {
                    formationOpen = false;
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

    private void toggleTool(MapCommandTool tool) {
        MapCommandClientState.setActiveTool(
                MapCommandClientState.activeTool() == tool ? MapCommandTool.MOVE : tool);
    }

    private void haltSelection() {
        MapCommandNetworkClient.behavior(MapCommandClientState.selectionId(),
                MapCommandClientState.selectedUnitList(), null, null, 2);
        orderSelectedObjects(MapOrder.HALT);
    }

    private void orderSelectedObjects(MapOrder order) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !MapCommandClientState.hasObjectSelection()) return;

        for (MapObjectSnapshot object : MapCommandClientState.objects(minecraft.level)) {
            if (!MapCommandClientState.isObjectSelected(object.id())) continue;
            String actionId = object.orderAction(order);
            if (actionId != null) MapCommandNetworkClient.objectAction(object.id(), actionId);
        }
    }

    private void ceaseFire() {
        MapCommandNetworkClient.holdStrategicFire(MapCommandClientState.selectionId(),
                MapCommandClientState.selectedGunners());
    }

    private void renderSelectedStatus(GuiGraphics graphics, Font font, List<CommandGroupSnapshot> selected) {
        if (selected.isEmpty()) {
            return;
        }

        int men = 0;
        int infantry = 0;
        int ranged = 0;
        int cavalry = 0;
        for (CommandGroupSnapshot squad : selected) {
            for (CommandUnitSnapshot unit : squad.units()) {
                if (!MapCommandClientState.isUnitSelected(unit.unitId())) continue;
                men++;
                switch (unit.type()) {
                    case RANGED -> ranged++;
                    case CAVALRY -> cavalry++;
                    default -> infantry++;
                }
            }
        }
        if (men == 0) {
            return;
        }

        typeSegments.clear();
        int kinds = (infantry > 0 ? 1 : 0) + (ranged > 0 ? 1 : 0) + (cavalry > 0 ? 1 : 0);

        String headline = I18n.get(men == 1 ? "gui.recruitsrtscommand.status.man" : "gui.recruitsrtscommand.status.men", men)
                + "   " + I18n.get(selected.size() == 1
                        ? "gui.recruitsrtscommand.status.squad" : "gui.recruitsrtscommand.status.squads", selected.size());
        graphics.drawString(font, headline, STATUS_X, STATUS_Y, 0xFFFFFF);

        int x = STATUS_X;
        x = drawTypeCount(graphics, font, x, CommandUnitType.INFANTRY, infantry, kinds,
                "gui.recruitsrtscommand.status.infantry");
        x = drawTypeCount(graphics, font, x, CommandUnitType.RANGED, ranged, kinds,
                "gui.recruitsrtscommand.status.ranged");
        drawTypeCount(graphics, font, x, CommandUnitType.CAVALRY, cavalry, kinds,
                "gui.recruitsrtscommand.status.cavalry");
    }

    private int drawTypeCount(GuiGraphics graphics, Font font, int x, CommandUnitType type, int count,
                              int kinds, String key) {
        if (count <= 0) return x;

        String text = I18n.get(key, count);
        int width = font.width(text);
        boolean pressable = kinds > 1;
        if (pressable) typeSegments.add(new TypeSegment(type, x, width));

        graphics.drawString(font, text, x, STATUS_Y + 11, pressable ? 0xFFD7E3F0 : 0xFFB8C2CE);
        return x + width + 8;
    }

    private record TypeSegment(CommandUnitType type, int x, int width) {
    }

    private CommandUnitType typeSegmentAt(double mouseX, double mouseY) {
        for (TypeSegment segment : typeSegments) {
            if (isOver(mouseX, mouseY, segment.x(), STATUS_Y + 11, segment.width(), 9)) {
                return segment.type();
            }
        }
        return null;
    }

    private void renderObjectSelection(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !MapCommandClientState.hasObjectSelection()) return;

        for (MapObjectSnapshot object : MapCommandClientState.objects(minecraft.level)) {
            if (!MapCommandClientState.isObjectSelected(object.id())) continue;
            double size = objectIconSize(object);
            double left = worldToScreenX(object.position().getX()) - size / 2.0D;
            double top = worldToScreenY(object.position().getZ()) - size / 2.0D;
            drawRect(graphics, left - 2.0D, top - 2.0D, left + size + 2.0D, top + size + 2.0D,
                    MARKER_SELECTED_COLOR);
        }
    }

    private void renderObjectRoutes(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        for (MapObjectSnapshot object : MapCommandClientState.objects(minecraft.level)) {
            List<BlockPos> route = object.route();
            if (route.isEmpty()) continue;

            double fromX = worldToScreenX(object.position().getX());
            double fromY = worldToScreenY(object.position().getZ());
            for (int leg = 0; leg < route.size(); leg++) {
                double toX = worldToScreenX(route.get(leg).getX());
                double toY = worldToScreenY(route.get(leg).getZ());
                drawPathSegment(graphics, fromX, fromY, toX, toY, OBJECT_ROUTE_COLOR, false);
                drawWaypoint(graphics, toX, toY, OBJECT_ROUTE_COLOR, leg == route.size() - 1);
                fromX = toX;
                fromY = toY;
            }
        }
        graphics.flush();
    }

    private void renderObjects(GuiGraphics graphics, Minecraft minecraft) {
        if (minecraft.level == null) return;

        for (MapObjectSnapshot object : MapCommandClientState.objects(minecraft.level)) {
            double size = objectIconSize(object);
            double centerX = worldToScreenX(object.position().getX());
            double centerY = worldToScreenY(object.position().getZ());
            double left = centerX - size / 2.0D;
            double top = centerY - size / 2.0D;

            drawObjectIcon(graphics, object.icon(), left, top, size);

            if (size >= OBJECT_ICON_FLOOR + 2.0D) {
                double pip = Math.max(2.0D, size * 0.16D);
                MapRenderUtil.fill(graphics, left - 1.0D, top - 1.0D, left + pip + 1.0D, top + pip + 1.0D,
                        relationColor(object.ownerId()));
                int stateColor = object.pipColor() == MapObjectSnapshot.NO_PIP
                        ? 0xD0101418
                        : object.pipColor();
                MapRenderUtil.fill(graphics, left, top, left + pip, top + pip, stateColor);
            }

            if (object.condition() < 100) {
                renderObjectCondition(graphics, object, centerX, top + size + 3.0D, size);
            }
        }
    }

    private MapObjectSnapshot objectAtScreen(double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;

        List<MapObjectSnapshot> objects = MapCommandClientState.objects(minecraft.level);
        for (int index = objects.size() - 1; index >= 0; index--) {
            MapObjectSnapshot object = objects.get(index);
            double reach = Math.max(MARKER_HIT_MIN, objectIconSize(object) / 2.0D + 2.0D);
            double dx = mouseX - worldToScreenX(object.position().getX());
            double dy = mouseY - worldToScreenY(object.position().getZ());
            if (Math.abs(dx) <= reach && Math.abs(dy) <= reach) return object;
        }
        return null;
    }

    private double objectIconSize(MapObjectSnapshot object) {
        double base = Math.max(OBJECT_ICON_FLOOR, markerIconSize() * 0.6D);
        return base * Math.max(0.25F, object.icon().scale());
    }

    private void drawObjectIcon(GuiGraphics graphics, me.mss1r.recruitsrtscommand.api.MapIcon icon,
                                double left, double top, double size) {
        if (icon.isItem()) {
            drawItemIcon(graphics, icon, left, top, size);
            return;
        }
        if (icon.texture() == null) return;

        if (icon.smooth()) {
            Minecraft.getInstance().getTextureManager().getTexture(icon.texture()).setFilter(true, false);
        }
        drawBanner(graphics, icon.texture(), left, top, size);
    }

    private void drawItemIcon(GuiGraphics graphics, me.mss1r.recruitsrtscommand.api.MapIcon icon,
                              double left, double top, double size) {
        net.minecraft.world.item.Item item =
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(icon.item());
        if (item == null) return;

        double factor = size / 16.0D;
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0.0D);
        graphics.pose().scale((float) factor, (float) factor, 1.0F);
        graphics.renderFakeItem(new net.minecraft.world.item.ItemStack(item), 0, 0);
        graphics.pose().popPose();
    }

    private void renderObjectCondition(GuiGraphics graphics, MapObjectSnapshot object,
                                       double centerX, double top, double size) {
        double width = size * 0.6D;
        double left = centerX - width / 2.0D;
        double height = Math.max(1.0D, size * 0.12D);
        MapRenderUtil.fill(graphics, left - 1.0D, top - 1.0D, left + width + 1.0D, top + height + 1.0D,
                0xC0101418);
        MapRenderUtil.fill(graphics, left, top, left + width, top + height, 0xFF3A2018);
        MapRenderUtil.fill(graphics, left, top, left + width * (object.condition() / 100.0D), top + height,
                healthColor(object.condition()));
    }

    private void renderSquadMarker(GuiGraphics graphics, SquadClusters.Cluster cluster, boolean selected, boolean hovered) {
        double centerX = markerCenterX(cluster);
        double size = markerIconSize();
        double top = markerTop(cluster);
        int relation = relationColor(cluster.ownerId());

        drawBanner(graphics, iconForSnapshot(cluster.snapshot()), centerX - size / 2.0D, top, size);

        if (selected || hovered) {
            drawRect(graphics, centerX - size / 2.0D - 2.0D, top - 2.0D,
                    centerX + size / 2.0D + 2.0D, top + size + 2.0D,
                    selected ? MARKER_SELECTED_COLOR : 0x90FFFFFF);
        }

        renderSquadHealth(graphics, cluster, centerX, top + size + 2.0D, size);
        renderStanceMarks(graphics, cluster, centerX - size / 2.0D, top, size);
        if (size >= MARKER_ICON) {
            renderSquadStrength(graphics, cluster, centerX + size / 2.0D + 2.0D, top + size - 8.0D);
        }
    }

    private void renderStanceMarks(GuiGraphics graphics, SquadClusters.Cluster cluster,
                                   double left, double top, double size) {
        if (size < MARKER_ICON_FLOOR + 2.0D) return;

        double pip = Math.max(2.0D, size * 0.16D);
        int aggro = cluster.aggro();
        if (aggro == 1 || aggro == 2 || aggro == 3) {
            MapRenderUtil.fill(graphics, left - 1.0D, top - 1.0D, left + pip + 1.0D, top + pip + 1.0D,
                    0xC0101418);
            MapRenderUtil.fill(graphics, left, top, left + pip, top + pip, aggroPipColor(aggro));
        }
        if (cluster.shields()) {
            double right = left + size;
            MapRenderUtil.fill(graphics, right - pip - 1.0D, top - 1.0D, right + 1.0D, top + pip + 1.0D,
                    0xC0101418);
            MapRenderUtil.fill(graphics, right - pip, top, right, top + pip, 0xFFBFC6D2);
        }
    }

    private int aggroPipColor(int aggro) {
        return switch (aggro) {
            case 1 -> 0xFFE05A4A;
            case 2 -> 0xFFE09A3A;
            default -> 0xFF5AA0E0;
        };
    }

    private void drawBanner(GuiGraphics graphics, ResourceLocation icon, double left, double top, double size) {
        float scale = (float) (size / MARKER_ICON);
        graphics.flush();
        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0.0D);
        graphics.pose().scale(scale, scale, 1.0F);

        graphics.setColor(0.05F, 0.07F, 0.09F, 1.0F);
        for (int[] offset : OUTLINE_OFFSETS) {
            graphics.blit(icon, offset[0], offset[1], 0.0F, 0.0F,
                    MARKER_ICON, MARKER_ICON, MARKER_ICON, MARKER_ICON);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(icon, 0, 0, 0.0F, 0.0F, MARKER_ICON, MARKER_ICON, MARKER_ICON, MARKER_ICON);

        graphics.pose().popPose();
        graphics.flush();
    }

    private void renderSquadHealth(GuiGraphics graphics, SquadClusters.Cluster cluster,
                                   double centerX, double top, double size) {
        int health = cluster.health();
        if (health < 0) return;

        double width = size * 0.8D;
        double left = centerX - width / 2.0D;
        double height = Math.max(2.0D, size / 10.0D);
        MapRenderUtil.fill(graphics, left - 1.0D, top + 1.0D, left + width + 1.0D, top + height + 2.0D, 0xD0101418);

        double filled = width * Mth.clamp(health, 0, 100) / 100.0D;
        if (filled >= 1.0D) {
            MapRenderUtil.fill(graphics, left, top + 1.5D, left + filled, top + height + 1.0D, healthColor(health));
        }
    }

    private int healthColor(int health) {
        double share = Mth.clamp(health, 0, 100) / 100.0D;
        int red;
        int green;
        if (share > 0.5D) {
            double t = (share - 0.5D) * 2.0D;
            red = (int) Math.round(230 - (230 - 60) * t);
            green = (int) Math.round(190 + (220 - 190) * t);
        } else {
            double t = share * 2.0D;
            red = 230;
            green = (int) Math.round(60 + (190 - 60) * t);
        }
        return 0xFF000000 | (red << 16) | (green << 8) | 70;
    }

    private void renderSquadStrength(GuiGraphics graphics, SquadClusters.Cluster cluster, double x, double y) {
        Font font = Minecraft.getInstance().font;
        String strength = cluster.whole()
                ? String.valueOf(cluster.strength())
                : cluster.strength() + "/" + cluster.snapshot().totalUnits();
        int width = font.width(strength);

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0D);
        graphics.fill(-1, -1, width + 1, 9, 0xE010141B);
        graphics.drawString(font, strength, 0, 0, 0xFFF2D98C, false);
        graphics.pose().popPose();
    }

    private double markerIconSize() {
        return Mth.clamp(view.scale() * MARKER_ICON * 0.5D, MARKER_ICON_FLOOR, MARKER_ICON);
    }

    private double markerCenterX(SquadClusters.Cluster cluster) {
        return worldToScreenX(cluster.centerX());
    }

    private double markerTop(SquadClusters.Cluster cluster) {
        double size = markerIconSize();
        double reach = view.scale() < UNIT_DOT_MIN_ZOOM ? 0.0D : cluster.reachNorth() * view.scale();
        return worldToScreenY(cluster.centerZ()) - Math.max(MARKER_LIFT_MIN, reach) - size - 2.0D;
    }

    private double smoothX(UUID id, double fallback) {
        return MapPositionSmoothing.x(id, fallback);
    }

    private double smoothZ(UUID id, double fallback) {
        return MapPositionSmoothing.z(id, fallback);
    }

    private String squadTooltip(SquadClusters.Cluster cluster) {
        CommandGroupSnapshot snapshot = cluster.snapshot();
        int men = cluster.strength();
        StringBuilder text = new StringBuilder(squadName(snapshot))
                .append("   ")
                .append(I18n.get(men == 1 ? "gui.recruitsrtscommand.status.man" : "gui.recruitsrtscommand.status.men", men));

        if (!cluster.whole()) {
            text.append("   ").append(I18n.get("gui.recruitsrtscommand.status.detached",
                    cluster.snapshot().totalUnits()));
        }

        int health = cluster.health();
        if (health >= 0 && health < 100) {
            text.append("   ").append(I18n.get("gui.recruitsrtscommand.status.health", health));
        }
        appendGroupState(text, ClientManager.groupMoveState, snapshot.selectionId());
        appendGroupState(text, ClientManager.groupAggroState, snapshot.selectionId());
        return text.toString();
    }

    private String squadName(CommandGroupSnapshot snapshot) {
        String name = ClientGroups.nameOf(snapshot.selectionId());
        return abbreviate(name == null ? I18n.get("gui.recruitsrtscommand.status.unnamed") : name, 22);
    }

    private void appendGroupState(StringBuilder text, Map<UUID, Component> states, UUID groupId) {
        if (states == null || groupId == null) return;
        Component state = states.get(groupId);
        if (state == null) return;
        String line = state.getString();
        if (line.isBlank()) return;
        text.append("   ").append(abbreviate(line, 22));
    }

    private void renderUnitDots(GuiGraphics graphics, CommandGroupSnapshot snapshot, boolean selected) {
        if (snapshot.units() == null || snapshot.units().isEmpty()) return;
        if (!selected && view.scale() < UNIT_DOT_MIN_ZOOM) return;

        int relation = relationColor(snapshot.ownerId());
        Minecraft minecraft = Minecraft.getInstance();
        boolean ours = minecraft.player != null && minecraft.player.getUUID().equals(snapshot.ownerId());
        boolean anySelected = ours && MapCommandClientState.hasSelection();
        double radius = Mth.clamp(view.scale() * 1.0D, 0.9D, 1.7D);
        for (CommandUnitSnapshot unit : snapshot.units()) {
            drawDot(graphics, worldToScreenX(smoothX(unit.unitId(), unit.position().getX())),
                    worldToScreenY(smoothZ(unit.unitId(), unit.position().getZ())),
                    radius + 0.7D, 0xC8101418);
        }

        for (CommandUnitSnapshot unit : snapshot.units()) {
            boolean commanded = !anySelected || MapCommandClientState.isUnitSelected(unit.unitId());
            int color = unitColor(unit.type(), relation);
            drawDot(graphics, worldToScreenX(smoothX(unit.unitId(), unit.position().getX())),
                    worldToScreenY(smoothZ(unit.unitId(), unit.position().getZ())),
                    radius, commanded ? color : dimmed(color));
        }
    }

    private int dimmed(int color) {
        int alpha = (color >>> 24) & 0xFF;
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        int grey = (red + green + blue) / 3;
        red = (int) (grey * 0.45D + red * 0.15D);
        green = (int) (grey * 0.45D + green * 0.15D);
        blue = (int) (grey * 0.45D + blue * 0.15D);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private void drawDot(GuiGraphics graphics, double centerX, double centerY, double radius, int color) {
        double inset = radius * 0.42D;
        MapRenderUtil.fill(graphics, centerX - radius + inset, centerY - radius,
                centerX + radius - inset, centerY + radius, color);
        MapRenderUtil.fill(graphics, centerX - radius, centerY - radius + inset,
                centerX + radius, centerY + radius - inset, color);
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

            int color = selected ? PATH_COLOR : 0xA0D6CA62;
            double previousX = worldToScreenX(origin.getX());
            double previousY = worldToScreenY(origin.getZ());
            for (int index = 0; index < waypoints.size(); index++) {
                BlockPos waypoint = waypoints.get(index);
                double x = worldToScreenX(waypoint.getX());
                double y = worldToScreenY(waypoint.getZ());
                drawPathSegment(graphics, previousX, previousY, x, y, color, selected);
                drawWaypoint(graphics, x, y, color, index == waypoints.size() - 1);
                previousX = x;
                previousY = y;
            }
        }
        graphics.flush();
    }

    private void renderFireZones(GuiGraphics graphics) {
        Set<UUID> selection = new HashSet<>(MapCommandClientState.selectedGunners());
        for (MapCommandClientState.FireZone zone : MapCommandClientState.fireZones()) {
            if (movingZone == zone) continue;
            drawFireZoneRing(graphics,
                    worldToScreenX(zone.target().getX()), worldToScreenY(zone.target().getZ()),
                    Math.max(4.0D, zone.radiusX() * view.scale()),
                    Math.max(4.0D, zone.radiusZ() * view.scale()),
                    zone.shape(), zone.commanded(selection), zone.siege());
        }
    }

    private void drawFireZoneRing(GuiGraphics graphics, double centerX, double centerY,
                                  double radiusX, double radiusZ, FireZoneShape shape,
                                  boolean selected) {
        drawFireZoneRing(graphics, centerX, centerY, radiusX, radiusZ, shape, selected, false);
    }

    private void drawFireZoneRing(GuiGraphics graphics, double centerX, double centerY,
                                  double radiusX, double radiusZ, FireZoneShape shape,
                                  boolean selected, boolean siege) {
        int rim = siege
                ? (selected ? SIEGE_ZONE_COLOR : 0x90FFA83A)
                : (selected ? FIRE_ZONE_COLOR : 0x90FF665A);
        int shadow = selected ? 0xD0101418 : 0x90101418;
        int ground = siege
                ? (selected ? 0x28FFA83A : 0x18FFA83A)
                : (selected ? 0x28FF665A : 0x18FF665A);

        if (shape == FireZoneShape.RECTANGLE) {
            MapRenderUtil.fill(graphics, centerX - radiusX, centerY - radiusZ,
                    centerX + radiusX, centerY + radiusZ, ground);
            if (selected) drawZoneHatching(graphics, centerX, centerY, radiusX, radiusZ, shape);
            drawRect(graphics, centerX - radiusX - 1.0D, centerY - radiusZ - 1.0D,
                    centerX + radiusX + 1.0D, centerY + radiusZ + 1.0D, shadow);
            drawRect(graphics, centerX - radiusX, centerY - radiusZ,
                    centerX + radiusX, centerY + radiusZ, rim);
        } else {
            drawDisc(graphics, centerX, centerY, radiusX, radiusZ, ground);
            if (selected) drawZoneHatching(graphics, centerX, centerY, radiusX, radiusZ, shape);
            drawEllipse(graphics, centerX, centerY, radiusX, radiusZ, shadow, 3.2D);
            drawEllipse(graphics, centerX, centerY, radiusX, radiusZ, rim, 1.6D);
        }

        drawZoneCorners(graphics, centerX, centerY, radiusX, radiusZ, rim);

        MapRenderUtil.line(graphics, centerX - 3.5D, centerY, centerX + 3.5D, centerY, 1.2D, rim);
        MapRenderUtil.line(graphics, centerX, centerY - 3.5D, centerX, centerY + 3.5D, 1.2D, rim);
    }

    private void drawZoneHatching(GuiGraphics graphics, double centerX, double centerY,
                                  double radiusX, double radiusZ, FireZoneShape shape) {
        double spacing = 6.0D;
        for (double offset = -radiusZ * 2.0D; offset <= radiusX * 2.0D; offset += spacing) {
            double startX = centerX - radiusX + offset;
            double startY = centerY - radiusZ;
            double runX = startX;
            double runY = startY;
            double segmentX = Double.NaN;
            double segmentY = 0.0D;

            for (double step = 0.0D; step <= radiusZ * 2.0D + 1.0D; step += 1.5D) {
                runX = startX + step;
                runY = startY + step;
                boolean inside = shape.contains(runX - centerX, runY - centerY, radiusX, radiusZ);
                if (inside && Double.isNaN(segmentX)) {
                    segmentX = runX;
                    segmentY = runY;
                } else if (!inside && !Double.isNaN(segmentX)) {
                    MapRenderUtil.line(graphics, segmentX, segmentY, runX, runY, 1.0D, 0x50FFD2CC);
                    segmentX = Double.NaN;
                }
            }
            if (!Double.isNaN(segmentX)) {
                MapRenderUtil.line(graphics, segmentX, segmentY, runX, runY, 1.0D, 0x50FFD2CC);
            }
        }
    }

    private void drawZoneCorners(GuiGraphics graphics, double centerX, double centerY,
                                 double radiusX, double radiusZ, int color) {
        double arm = Math.min(7.0D, Math.min(radiusX, radiusZ) * 0.7D);
        if (arm < 2.0D) return;

        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                double cornerX = centerX + sx * radiusX;
                double cornerY = centerY + sy * radiusZ;
                MapRenderUtil.line(graphics, cornerX, cornerY, cornerX - sx * arm, cornerY, 3.0D, 0xB0101418);
                MapRenderUtil.line(graphics, cornerX, cornerY, cornerX, cornerY - sy * arm, 3.0D, 0xB0101418);
                MapRenderUtil.line(graphics, cornerX, cornerY, cornerX - sx * arm, cornerY, 1.3D, color);
                MapRenderUtil.line(graphics, cornerX, cornerY, cornerX, cornerY - sy * arm, 1.3D, color);
            }
        }
    }

    private void drawEllipse(GuiGraphics graphics, double centerX, double centerY,
                             double radiusX, double radiusZ, int color, double thickness) {
        int segments = 40;
        double previousX = centerX + radiusX;
        double previousY = centerY;
        for (int i = 1; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            double x = centerX + Math.cos(angle) * radiusX;
            double y = centerY + Math.sin(angle) * radiusZ;
            MapRenderUtil.line(graphics, previousX, previousY, x, y, thickness, color);
            previousX = x;
            previousY = y;
        }
    }

    private void drawRect(GuiGraphics graphics, double left, double top, double right, double bottom, int color) {
        MapRenderUtil.fill(graphics, left, top, right, top + 1.0D, color);
        MapRenderUtil.fill(graphics, left, bottom - 1.0D, right, bottom, color);
        MapRenderUtil.fill(graphics, left, top, left + 1.0D, bottom, color);
        MapRenderUtil.fill(graphics, right - 1.0D, top, right, bottom, color);
    }

    private void drawDisc(GuiGraphics graphics, double centerX, double centerY,
                          double radiusX, double radiusZ, int color) {
        int rows = Math.max(4, (int) Math.ceil(radiusZ * 2.0D));
        for (int i = 0; i < rows; i++) {
            double top = centerY - radiusZ + radiusZ * 2.0D * i / rows;
            double bottom = centerY - radiusZ + radiusZ * 2.0D * (i + 1) / rows;
            double middle = (top + bottom) / 2.0D - centerY;
            double share = 1.0D - (middle * middle) / (radiusZ * radiusZ);
            if (share <= 0.0D) continue;
            double halfWidth = radiusX * Math.sqrt(share);
            MapRenderUtil.fill(graphics, centerX - halfWidth, top, centerX + halfWidth, bottom, color);
        }
    }

    private void drawPathSegment(GuiGraphics graphics, double startX, double startY,
                                 double endX, double endY, int color, boolean selected) {
        double length = Math.hypot(endX - startX, endY - startY);
        if (length < 2.0D) return;

        drawRouteLine(graphics, startX, startY, endX, endY, color, selected);
    }

    private void drawRouteLine(GuiGraphics graphics, double startX, double startY,
                               double endX, double endY, int color, boolean selected) {
        double thickness = (selected ? 1.6D : 1.2D) * routeUiScale();
        MapRenderUtil.line(graphics, startX, startY, endX, endY, thickness + 1.8D * routeUiScale(),
                0xC8101418);
        MapRenderUtil.line(graphics, startX, startY, endX, endY, thickness, routeSolidColor(color));
    }

    private void drawWaypoint(GuiGraphics graphics, double x, double y, int color, boolean destination) {
        int solid = routeSolidColor(color);
        double scale = routeUiScale();
        double radius = (destination ? 6.0D : 3.8D) * scale;
        drawDiamondOutline(graphics, x, y, radius, 1.8D * scale, 0xC0101418);
        drawDiamondOutline(graphics, x, y, radius - 0.8D * scale, 1.0D * scale, solid);
        if (destination) {
            double pip = 1.2D * scale;
            MapRenderUtil.fill(graphics, x - pip, y - pip, x + pip, y + pip, 0xC0101418);
            MapRenderUtil.fill(graphics, x - pip * 0.66D, y - pip * 0.66D,
                    x + pip * 0.66D, y + pip * 0.66D, solid);
        }
    }

    private double routeUiScale() {
        return Mth.clamp(view.scale() * 0.85D, 0.4D, 1.0D);
    }

    private void drawDiamondOutline(GuiGraphics graphics, double centerX, double centerY,
                                    double radius, double thickness, int color) {
        MapRenderUtil.line(graphics, centerX, centerY - radius, centerX + radius, centerY, thickness, color);
        MapRenderUtil.line(graphics, centerX + radius, centerY, centerX, centerY + radius, thickness, color);
        MapRenderUtil.line(graphics, centerX, centerY + radius, centerX - radius, centerY, thickness, color);
        MapRenderUtil.line(graphics, centerX - radius, centerY, centerX, centerY - radius, thickness, color);
    }

    private int routeSolidColor(int color) {
        return 0xFF000000 | (color & 0x00FFFFFF);
    }

    private void drawIconButton(GuiGraphics graphics, int x, int y, ResourceLocation icon, String fallback,
                                boolean selected, boolean enabled, boolean hovered) {
        int bg = !enabled ? BUTTON_DISABLED : selected ? BUTTON_SELECTED : hovered ? BUTTON_HOVERED : BUTTON_BG;
        int outline = selected ? OUTLINE_SELECTED : hovered ? OUTLINE_HOVERED : OUTLINE;
        graphics.fill(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, bg);
        graphics.renderOutline(x, y, BUTTON_SIZE, BUTTON_SIZE, outline);

        if (icon != null) {
            graphics.flush();
            RenderSystem.enableBlend();
            int iconOffset = (BUTTON_SIZE - HUD_ICON_SIZE) / 2;
            graphics.blit(icon, x + iconOffset, y + iconOffset, 0.0F, 0.0F,
                    HUD_ICON_SIZE, HUD_ICON_SIZE, TEXTURE_ICON_SIZE, TEXTURE_ICON_SIZE);
        } else if (fallback != null) {
            graphics.drawCenteredString(Minecraft.getInstance().font, fallback, x + BUTTON_SIZE / 2, y + 5,
                    enabled ? 0xFFFFFF : 0xFF999999);
        }

    }

    private void drawTextButton(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                String label, boolean selected, boolean enabled, boolean hovered) {
        int bg = !enabled ? BUTTON_DISABLED : selected ? BUTTON_SELECTED : hovered ? BUTTON_HOVERED : BUTTON_BG;
        int outline = selected ? OUTLINE_SELECTED : hovered ? OUTLINE_HOVERED : OUTLINE;
        graphics.fill(x, y, x + width, y + height, bg);
        graphics.renderOutline(x, y, width, height, outline);
        graphics.drawCenteredString(font, label, x + width / 2, y + (height - 8) / 2, enabled ? 0xFFFFFF : 0xFF999999);
    }

    private void renderCommandBar(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        boolean hasSelection = MapCommandClientState.hasSelection();
        int x = barLeft();
        int y = barTop();
        for (int index = 0; index < BAR_BUTTONS; index++) {
            int buttonX = x + index * (BUTTON_SIZE + BUTTON_GAP);
            boolean enabled = !needsSelection(index) || hasSelection
                    || (takesObjects(index) && MapCommandClientState.hasObjectSelection());
            drawIconButton(graphics, buttonX, y, barIcon(index), null, barSelected(index), enabled,
                    isOver(mouseX, mouseY, buttonX, y, BUTTON_SIZE, BUTTON_SIZE));
        }

        renderFormationPicker(graphics, font, mouseX, mouseY);
        renderAggroPicker(graphics, mouseX, mouseY);

        String label = isOverHold(mouseX, mouseY)
                ? I18n.get(MapCommandClientState.holdFormation()
                        ? "gui.recruitsrtscommand.bar.hold.on"
                        : "gui.recruitsrtscommand.bar.hold.off")
                : isOverTight(mouseX, mouseY)
                ? I18n.get(MapCommandClientState.tightFormation()
                        ? "gui.recruitsrtscommand.bar.tight.on"
                        : "gui.recruitsrtscommand.bar.tight.off")
                : aggroOpen && aggroIndexAt(mouseX, mouseY) >= 0
                ? aggroName(AGGRO_CYCLE[aggroIndexAt(mouseX, mouseY)])
                : formationOpen && formationIndexAt(mouseX, mouseY) >= 0
                ? formationName(FORMATION_CYCLE[formationIndexAt(mouseX, mouseY)])
                : barLabel(mouseX, mouseY);
        if (label == null && typeSegmentAt(mouseX, mouseY) != null) {
            label = I18n.get("gui.recruitsrtscommand.status.only_kind");
        }
        if (label == null && hoveredCluster != null && enemyMenuTarget == null
                && objectMenuTarget == null) {
            label = squadTooltip(hoveredCluster);
        }
        if (label != null) {
            drawTooltip(graphics, font, mouseX, mouseY, label);
        }
    }

    private void renderFormationPicker(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!formationOpen) return;

        int current = Mth.clamp(ClientManager.formationSelection, 0, 8);
        int x = formationPickerLeft();
        int y = formationPickerTop();
        for (int index = 0; index < FORMATION_CYCLE.length; index++) {
            int formation = FORMATION_CYCLE[index];
            int buttonX = x + index * (BUTTON_SIZE + BUTTON_GAP);
            drawIconButton(graphics, buttonX, y, iconForFormation(formation), null, formation == current,
                    true, isOver(mouseX, mouseY, buttonX, y, BUTTON_SIZE, BUTTON_SIZE));
        }

        int tightX = tightButtonLeft();
        drawTextButton(graphics, font, tightX, y, TIGHT_WIDTH, BUTTON_SIZE, tightLabel(),
                MapCommandClientState.tightFormation(), true,
                isOver(mouseX, mouseY, tightX, y, TIGHT_WIDTH, BUTTON_SIZE));

        int holdX = holdButtonLeft();
        drawTextButton(graphics, font, holdX, y, TIGHT_WIDTH, BUTTON_SIZE, holdLabel(),
                MapCommandClientState.holdFormation(), true,
                isOver(mouseX, mouseY, holdX, y, TIGHT_WIDTH, BUTTON_SIZE));
    }

    private void renderAggroPicker(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!aggroOpen) return;

        int current = selectedAggro();
        int x = aggroPickerLeft();
        int y = formationPickerTop();
        for (int index = 0; index < AGGRO_CYCLE.length; index++) {
            int aggro = AGGRO_CYCLE[index];
            int buttonX = x + index * (BUTTON_SIZE + BUTTON_GAP);
            drawIconButton(graphics, buttonX, y, iconForAggro(aggro), null, aggro == current,
                    true, isOver(mouseX, mouseY, buttonX, y, BUTTON_SIZE, BUTTON_SIZE));
        }
    }

    private int aggroPickerLeft() {
        int width = AGGRO_CYCLE.length * BUTTON_SIZE + (AGGRO_CYCLE.length - 1) * BUTTON_GAP;
        int over = barLeft() + BUTTON_AGGRO * (BUTTON_SIZE + BUTTON_GAP) + (BUTTON_SIZE - width) / 2;
        return Mth.clamp(over, 5, Math.max(5, view.screenWidth() - width - 5));
    }

    private int aggroIndexAt(double mouseX, double mouseY) {
        if (!aggroOpen) return -1;
        int x = aggroPickerLeft();
        int y = formationPickerTop();
        for (int index = 0; index < AGGRO_CYCLE.length; index++) {
            int buttonX = x + index * (BUTTON_SIZE + BUTTON_GAP);
            if (isOver(mouseX, mouseY, buttonX, y, BUTTON_SIZE, BUTTON_SIZE)) return index;
        }
        return -1;
    }

    private int formationPickerLeft() {
        int width = FORMATION_CYCLE.length * BUTTON_SIZE + (FORMATION_CYCLE.length - 1) * BUTTON_GAP
                + BUTTON_GAP * 5 + TIGHT_WIDTH * 2;
        return Mth.clamp((view.screenWidth() - width) / 2, 5, Math.max(5, view.screenWidth() - width - 5));
    }

    private int formationPickerTop() {
        return barTop() - BUTTON_SIZE - 14;
    }

    private String tightLabel() {
        return MapCommandClientState.tightFormation() ? "[X]" : "[ ]";
    }

    private String holdLabel() {
        return MapCommandClientState.holdFormation() ? "[X]" : "[ ]";
    }

    private int holdButtonLeft() {
        return tightButtonLeft() + TIGHT_WIDTH + BUTTON_GAP;
    }

    private boolean isOverHold(double mouseX, double mouseY) {
        return formationOpen
                && isOver(mouseX, mouseY, holdButtonLeft(), formationPickerTop(), TIGHT_WIDTH, BUTTON_SIZE);
    }

    private int tightButtonLeft() {
        int width = FORMATION_CYCLE.length * BUTTON_SIZE + (FORMATION_CYCLE.length - 1) * BUTTON_GAP;
        return formationPickerLeft() + width + BUTTON_GAP * 4;
    }

    private boolean isOverTight(double mouseX, double mouseY) {
        return formationOpen
                && isOver(mouseX, mouseY, tightButtonLeft(), formationPickerTop(), TIGHT_WIDTH, BUTTON_SIZE);
    }

    private int formationIndexAt(double mouseX, double mouseY) {
        if (!formationOpen) return -1;
        int x = formationPickerLeft();
        int y = formationPickerTop();
        for (int index = 0; index < FORMATION_CYCLE.length; index++) {
            int buttonX = x + index * (BUTTON_SIZE + BUTTON_GAP);
            if (isOver(mouseX, mouseY, buttonX, y, BUTTON_SIZE, BUTTON_SIZE)) return index;
        }
        return -1;
    }

    private boolean anyCrewInHand() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !MapCommandClientState.hasObjectSelection()) return false;

        for (MapObjectSnapshot object : MapCommandClientState.objects(minecraft.level)) {
            if (MapCommandClientState.isObjectSelected(object.id()) && !object.crew().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private int countArchersSelected() {
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

    private void renderEnemyMenu(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (enemyMenuTarget == null && objectMenuTarget == null) return;

        EnemyMenu menu = enemyMenuLayout();
        if (menu == null) return;

        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, ENEMY_MENU_Z);
        try {
            drawMenu(graphics, font, menu, mouseX, mouseY);
        } finally {
            graphics.flush();
            graphics.pose().popPose();
        }
    }

    private void drawMenu(GuiGraphics graphics, Font font, EnemyMenu menu, int mouseX, int mouseY) {
        graphics.fill(menu.x, menu.y, menu.x + menu.width, menu.y + menu.height, MENU_BACK);
        graphics.renderOutline(menu.x, menu.y, menu.width, menu.height, MENU_EDGE);

        String title = font.plainSubstrByWidth(menu.title, menu.width - ENEMY_MENU_PAD * 2);
        graphics.drawString(font, title, menu.x + ENEMY_MENU_PAD, menu.y + ENEMY_MENU_PAD, MENU_VALUE);
        graphics.fill(menu.x + ENEMY_MENU_PAD, menu.y + 21,
                menu.x + menu.width - ENEMY_MENU_PAD, menu.y + 22, MENU_RULE);

        int hoveredRow = menu.rowAt(mouseX, mouseY);
        for (int index = 0; index < menu.labels.length; index++) {
            int rowY = menu.y + menu.rowTop[index];
            if (index == hoveredRow && menu.enabled[index]) {
                graphics.fill(menu.x + 1, rowY, menu.x + menu.width - 1, rowY + ENEMY_MENU_ROW,
                        MENU_ROW_HOVERED);
            }
            graphics.drawString(font, menu.labels[index], menu.x + ENEMY_MENU_PAD, rowY + 3,
                    menu.enabled[index] ? MENU_VALUE : MENU_TEXT_OFF);
        }

        String[] note = hoveredRow >= 0 ? menu.hints[hoveredRow]
                : menu.footer == null ? EMPTY_NOTE : menu.footerLines;
        int noteColour = hoveredRow >= 0 && !menu.enabled[hoveredRow] ? MENU_TEXT_OFF : MENU_LABEL;
        for (int line = 0; line < note.length; line++) {
            graphics.drawString(font, note[line], menu.x + ENEMY_MENU_PAD,
                    menu.y + menu.noteTop + line * ENEMY_MENU_LINE, noteColour);
        }
    }

    private static final String[] EMPTY_NOTE = new String[0];

    private final class EnemyMenu {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final String title;
        private final String[] labels;
        private final String[][] hints;
        private final int[] rowTop;
        private final int noteTop;
        private final boolean[] enabled;
        private final String footer;
        private final String[] footerLines;

        private EnemyMenu(Font font, String title, String[] labels, String[] hints,
                          boolean[] enabled, String footer) {
            this.title = title;
            this.labels = labels;
            this.enabled = enabled;
            this.footer = footer;

            int widest = font.width(title);
            for (String label : labels) widest = Math.max(widest, font.width(label));
            if (footer != null) widest = Math.max(widest, font.width(footer));
            for (String hint : hints) widest = Math.max(widest, font.width(hint));
            int content = Mth.clamp(widest, ENEMY_MENU_MIN_WIDTH - ENEMY_MENU_PAD * 2,
                    ENEMY_MENU_MAX_WIDTH - ENEMY_MENU_PAD * 2);
            this.width = content + ENEMY_MENU_PAD * 2;

            this.hints = new String[hints.length][];
            this.rowTop = new int[hints.length];
            int cursor = ENEMY_MENU_HEAD;
            for (int index = 0; index < hints.length; index++) {
                this.hints[index] = wrap(font, hints[index], content).toArray(new String[0]);
                this.rowTop[index] = cursor;
                cursor += ENEMY_MENU_ROW;
            }

            this.footerLines = wrap(font, footer, content).toArray(new String[0]);
            this.noteTop = cursor + 5;
            this.height = noteTop + ENEMY_MENU_HINT_LINES * ENEMY_MENU_LINE + 3;

            this.x = enemyMenuX + width + 2 <= view.screenWidth()
                    ? (int) enemyMenuX
                    : (int) Math.max(2.0D, enemyMenuX - width);
            this.y = enemyMenuY + height + 2 <= view.screenHeight()
                    ? (int) enemyMenuY
                    : (int) Math.max(2.0D, enemyMenuY - height);
        }

        private int rowAt(double mouseX, double mouseY) {
            if (mouseX < x || mouseX >= x + width) {
                return -1;
            }
            for (int index = 0; index < labels.length; index++) {
                int top = y + rowTop[index];
                if (mouseY >= top && mouseY < top + ENEMY_MENU_ROW) {
                    return index;
                }
            }
            return -1;
        }
    }

    private static List<String> wrap(Font font, String text, int room) {
        List<String> lines = new ArrayList<>(2);
        if (text == null || text.isEmpty()) {
            return lines;
        }
        String rest = text;
        while (!rest.isEmpty() && lines.size() < 2) {
            String head = font.plainSubstrByWidth(rest, room);
            if (head.length() < rest.length()) {
                int space = head.lastIndexOf(' ');
                if (space > 0) {
                    head = head.substring(0, space);
                }
            }
            if (head.isEmpty()) {
                head = rest.substring(0, 1);
            }
            lines.add(head.trim());
            rest = rest.substring(head.length()).trim();
        }
        return lines;
    }

    private record PendingObjectOrder(UUID objectId, String actionId, String label) {
    }

    private EnemyMenu enemyMenuLayout() {
        if (objectMenuTarget != null) return objectMenuLayout();
        if (enemyMenuTarget == null) return null;

        boolean anyMen = MapCommandClientState.hasSelection();
        boolean allowed = mayAttack(enemyMenuTarget.ownerId());
        String footer = !allowed
                ? I18n.get("gui.recruitsrtscommand.enemy.friendly")
                : anyMen ? null : I18n.get("gui.recruitsrtscommand.enemy.no_men");
        return new EnemyMenu(
                Minecraft.getInstance().font,
                squadName(enemyMenuTarget.snapshot()) + "   " + enemyMenuTarget.strength(),
                new String[]{I18n.get("gui.recruitsrtscommand.enemy.attack")},
                new String[]{I18n.get("gui.recruitsrtscommand.enemy.attack.hint")},
                new boolean[]{anyMen && allowed},
                footer);
    }

    private EnemyMenu objectMenuLayout() {
        MapObjectSnapshot object = freshObjectMenuTarget();
        if (object == null) return null;
        List<MapObjectAction> actions = objectMenuActions(object);

        String title = object.lines().isEmpty()
                ? I18n.get("gui.recruitsrtscommand.object.unnamed")
                : object.lines().get(0).getString();
        if (actions.isEmpty()) {
            return new EnemyMenu(Minecraft.getInstance().font, title, new String[0], new String[0],
                    new boolean[0], I18n.get("gui.recruitsrtscommand.object.no_actions"));
        }

        List<List<MapObjectAction>> rows = menuRows(actions);
        String[] labels = new String[rows.size()];
        String[] hints = new String[rows.size()];
        boolean[] enabled = new boolean[rows.size()];
        for (int index = 0; index < rows.size(); index++) {
            MapObjectAction shown = shownOption(rows.get(index));
            labels[index] = shown.label().getString();
            enabled[index] = rows.get(index).stream().anyMatch(MapObjectAction::enabled);
            hints[index] = !enabled[index] && shown.reason() != null
                    ? shown.reason().getString()
                    : shown.hint() == null ? "" : shown.hint().getString();
        }
        return new EnemyMenu(Minecraft.getInstance().font, title, labels, hints, enabled, null);
    }

    private List<MapObjectAction> objectMenuActions(MapObjectSnapshot object) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID localPlayerId = minecraft.player == null ? null : minecraft.player.getUUID();
        if (localPlayerId == null || localPlayerId.equals(object.ownerId())) {
            return object.actions();
        }

        Component label = Component.translatable("gui.recruitsrtscommand.enemy.attack");
        Component hint = Component.translatable("gui.recruitsrtscommand.enemy.attack.hint");
        MapObjectAction attack;
        if (!mayAttack(object.ownerId())) {
            attack = MapObjectAction.blocked(ATTACK_OBJECT, label, hint,
                    Component.translatable("gui.recruitsrtscommand.enemy.friendly"));
        } else if (!MapCommandClientState.hasSelection()) {
            attack = MapObjectAction.blocked(ATTACK_OBJECT, label, hint,
                    Component.translatable("gui.recruitsrtscommand.enemy.no_men"));
        } else {
            attack = MapObjectAction.of(ATTACK_OBJECT, label, hint);
        }

        List<MapObjectAction> actions = new ArrayList<>(object.actions().size() + 1);
        actions.add(attack);
        actions.addAll(object.actions());
        return actions;
    }

    private List<List<MapObjectAction>> menuRows(List<MapObjectAction> actions) {
        List<List<MapObjectAction>> rows = new ArrayList<>(actions.size());
        Map<String, List<MapObjectAction>> groups = new HashMap<>();
        for (MapObjectAction action : actions) {
            if (!action.isOption()) {
                rows.add(List.of(action));
                continue;
            }
            List<MapObjectAction> group = groups.get(action.group());
            if (group == null) {
                group = new ArrayList<>(3);
                groups.put(action.group(), group);
                rows.add(group);
            }
            group.add(action);
        }
        return rows;
    }

    private MapObjectAction shownOption(List<MapObjectAction> options) {
        for (MapObjectAction option : options) {
            if (option.selected()) return option;
        }
        return options.get(0);
    }

    private MapObjectAction nextOption(List<MapObjectAction> options) {
        int current = options.indexOf(shownOption(options));
        for (int step = 1; step <= options.size(); step++) {
            MapObjectAction candidate = options.get((current + step) % options.size());
            if (candidate.enabled() && !candidate.selected()) return candidate;
        }
        return null;
    }

    private boolean mayAttack(UUID ownerId) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID localPlayerId = minecraft.player == null ? null : minecraft.player.getUUID();
        if (localPlayerId != null && localPlayerId.equals(ownerId)) return false;

        RecruitsFaction ownFaction = ClientManager.ownFaction;
        RecruitsFaction targetFaction = findFactionForOwner(ownerId);
        if (ownFaction == null || targetFaction == null) return true;
        if (ownFaction.equalsFaction(targetFaction)) return false;
        if (ClientManager.getRelation(ownFaction.getStringID(), targetFaction.getStringID())
                == RecruitsDiplomacyManager.DiplomacyStatus.ALLY) {
            return false;
        }
        return !ClientManager.hasTreaty(ownFaction.getStringID(), targetFaction.getStringID());
    }

    private MapObjectSnapshot freshObjectMenuTarget() {
        if (objectMenuTarget == null) return null;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            for (MapObjectSnapshot object : MapCommandClientState.objects(minecraft.level)) {
                if (object.id().equals(objectMenuTarget.id())) {
                    objectMenuTarget = object;
                    return object;
                }
            }
        }
        objectMenuTarget = null;
        return null;
    }

    private int enemyMenuRowAt(double mouseX, double mouseY) {
        EnemyMenu menu = enemyMenuLayout();
        return menu == null ? -1 : menu.rowAt(mouseX, mouseY);
    }

    private void orderAgainstEnemy(SquadClusters.Cluster enemy) {
        if (!MapCommandClientState.hasSelection()) return;
        MapCommandNetworkClient.attack(MapCommandClientState.selectionId(),
                MapCommandClientState.selectedUnitList(), enemy.selectionId());
    }

    private void renderPendingOrderHint(GuiGraphics graphics, Font font) {
        if (pendingObjectOrder == null) return;

        String hint = I18n.get("gui.recruitsrtscommand.object.point_at", pendingObjectOrder.label());
        int x = (view.screenWidth() - font.width(hint)) / 2;
        graphics.drawString(font, hint, x, barTop() - 14, 0xFFE6C56A);
    }

    private void renderFireZoneHint(GuiGraphics graphics, Font font) {
        if (drawingZone || MapCommandClientState.activeTool() != MapCommandTool.FIRE) return;

        String hint;
        if (!MapCommandClientState.hasSelection() && !MapCommandClientState.hasObjectSelection()) {
            hint = I18n.get("gui.recruitsrtscommand.zone.select_first");
        } else if (countArchersSelected() == 0 && !anyCrewInHand()) {
            hint = I18n.get("gui.recruitsrtscommand.zone.no_bows");
        } else {
            return;
        }

        int x = (view.screenWidth() - font.width(hint)) / 2;
        graphics.drawString(font, hint, x, barTop() - 14, 0xFFFFC9C0);
    }

    private int toggleLeft() {
        int width = view == null ? 480 : view.screenWidth();
        return Math.max(TOGGLE_MARGIN,
                width - TOGGLE_MARGIN - GEAR_SIZE - GEAR_GAP - TOGGLE_W);
    }

    private int barLeft() {
        return (view.screenWidth() - (BAR_BUTTONS * BUTTON_SIZE + (BAR_BUTTONS - 1) * BUTTON_GAP)) / 2;
    }

    private int barTop() {
        return view.screenHeight() - BUTTON_SIZE - 34;
    }

    private ResourceLocation barIcon(int index) {
        return switch (index) {
            case BUTTON_FORMATION -> iconForFormation(Mth.clamp(ClientManager.formationSelection, 0, 8));
            case BUTTON_FIRE_ZONE -> ICON_FIRE_ZONE;
            case BUTTON_HALT -> ICON_HOLD;
            case BUTTON_CEASE_FIRE -> ICON_CEASE_FIRE;
            case BUTTON_FACE -> ICON_FACE;
            case BUTTON_PICK -> ICON_PICK;
            case BUTTON_AGGRO -> iconForAggro(selectedAggro());
            case BUTTON_SHIELDS -> ICON_SHIELDS;
            default -> ICON_FIRE_AT_WILL;
        };
    }

    private boolean needsSelection(int index) {
        return switch (index) {
            case BUTTON_FORMATION, BUTTON_FIRE_ZONE, BUTTON_FACE, BUTTON_PICK -> false;
            default -> true;
        };
    }

    private boolean takesObjects(int index) {
        return index == BUTTON_HALT || index == BUTTON_CEASE_FIRE;
    }

    private static final int[] AGGRO_CYCLE = {3, 0, 1, 2};

    private ResourceLocation iconForAggro(int aggro) {
        return switch (aggro) {
            case 1 -> ICON_AGGRO_AGGRESSIVE;
            case 2 -> ICON_AGGRO_RAID;
            case 3 -> ICON_AGGRO_PASSIVE;
            default -> ICON_AGGRO_NEUTRAL;
        };
    }

    private String aggroName(int aggro) {
        return I18n.get(switch (aggro) {
            case 1 -> "gui.recruitsrtscommand.aggro.aggressive";
            case 2 -> "gui.recruitsrtscommand.aggro.raid";
            case 3 -> "gui.recruitsrtscommand.aggro.passive";
            default -> "gui.recruitsrtscommand.aggro.neutral";
        });
    }

    private int selectedAggro() {
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

    private boolean selectedFiring() {
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

    private boolean selectedShields() {
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

    private boolean barSelected(int index) {
        MapCommandTool tool = MapCommandClientState.activeTool();
        return (index == BUTTON_FIRE_ZONE && tool == MapCommandTool.FIRE)
                || (index == BUTTON_FACE && tool == MapCommandTool.FACE)
                || (index == BUTTON_PICK && tool == MapCommandTool.PICK)
                || (index == BUTTON_AGGRO && aggroOpen)
                || (index == BUTTON_SHIELDS && selectedShields())
                || (index == BUTTON_FIRE_AT_WILL && selectedFiring());
    }

    private String barLabel(double mouseX, double mouseY) {
        int index = barIndexAt(mouseX, mouseY);
        return switch (index) {
            case BUTTON_FORMATION -> I18n.get("gui.recruitsrtscommand.bar.formation",
                    formationName(Mth.clamp(ClientManager.formationSelection, 0, 8)));
            case BUTTON_FIRE_ZONE -> I18n.get("gui.recruitsrtscommand.bar.fire_zone",
                    I18n.get("gui.recruitsrtscommand.zone.shape."
                            + MapCommandClientState.fireZoneShape().name().toLowerCase(java.util.Locale.ROOT)));
            case BUTTON_HALT -> I18n.get("gui.recruitsrtscommand.bar.halt");
            case BUTTON_CEASE_FIRE -> I18n.get("gui.recruitsrtscommand.bar.cease_fire");
            case BUTTON_FIRE_AT_WILL -> I18n.get(selectedFiring()
                    ? "gui.recruitsrtscommand.bar.fire_at_will.on"
                    : "gui.recruitsrtscommand.bar.fire_at_will.off");
            case BUTTON_FACE -> I18n.get("gui.recruitsrtscommand.bar.face");
            case BUTTON_PICK -> I18n.get("gui.recruitsrtscommand.bar.pick");
            case BUTTON_AGGRO -> I18n.get("gui.recruitsrtscommand.bar.aggro",
                    !MapCommandClientState.hasSelection()
                            ? I18n.get("gui.recruitsrtscommand.aggro.none")
                            : selectedAggro() == SquadClusters.MIXED
                            ? I18n.get("gui.recruitsrtscommand.aggro.mixed")
                            : aggroName(selectedAggro()));
            case BUTTON_SHIELDS -> I18n.get(selectedShields()
                    ? "gui.recruitsrtscommand.bar.shields.on"
                    : "gui.recruitsrtscommand.bar.shields.off");
            default -> null;
        };
    }

    private int barIndexAt(double mouseX, double mouseY) {
        int x = barLeft();
        int y = barTop();
        for (int index = 0; index < BAR_BUTTONS; index++) {
            int buttonX = x + index * (BUTTON_SIZE + BUTTON_GAP);
            if (isOver(mouseX, mouseY, buttonX, y, BUTTON_SIZE, BUTTON_SIZE)) {
                return index;
            }
        }
        return -1;
    }

    private boolean clickCommandBar(double mouseX, double mouseY) {
        CommandUnitType kind = typeSegmentAt(mouseX, mouseY);
        if (kind != null) {
            MapCommandClientState.retainType(kind);
            return true;
        }

        if (isOverTight(mouseX, mouseY)) {
            MapCommandClientState.toggleTightFormation();
            return true;
        }
        if (isOverHold(mouseX, mouseY)) {
            MapCommandClientState.toggleHoldFormation();
            return true;
        }

        int aggroIndex = aggroIndexAt(mouseX, mouseY);
        if (aggroIndex >= 0) {
            if (MapCommandClientState.hasSelection()) {
                MapCommandNetworkClient.behavior(MapCommandClientState.selectionId(),
                        MapCommandClientState.selectedUnitList(), AGGRO_CYCLE[aggroIndex], null, null);
            }
            aggroOpen = false;
            return true;
        }

        int formationIndex = formationIndexAt(mouseX, mouseY);
        if (formationIndex >= 0) {
            ClientManager.formationSelection = FORMATION_CYCLE[formationIndex];
            formationOpen = false;
            return true;
        }

        int index = barIndexAt(mouseX, mouseY);
        if (index < 0) {
            formationOpen = false;
            aggroOpen = false;
            return false;
        }
        if (index != BUTTON_AGGRO) aggroOpen = false;
        if (needsSelection(index) && !MapCommandClientState.hasSelection()
                && !(takesObjects(index) && MapCommandClientState.hasObjectSelection())) {
            return true;
        }

        switch (index) {
            case BUTTON_FORMATION -> formationOpen = !formationOpen;
            case BUTTON_FIRE_ZONE -> toggleTool(MapCommandTool.FIRE);
            case BUTTON_FACE -> toggleTool(MapCommandTool.FACE);
            case BUTTON_PICK -> toggleTool(MapCommandTool.PICK);
            case BUTTON_HALT -> haltSelection();
            case BUTTON_CEASE_FIRE -> ceaseFire();
            case BUTTON_AGGRO -> aggroOpen = !aggroOpen;
            case BUTTON_SHIELDS -> MapCommandNetworkClient.behavior(MapCommandClientState.selectionId(),
                    MapCommandClientState.selectedUnitList(), null, null, null, !selectedShields());
            case BUTTON_FIRE_AT_WILL -> MapCommandNetworkClient.behavior(MapCommandClientState.selectionId(),
                    MapCommandClientState.selectedUnitList(), null, !selectedFiring(), null);
            default -> {
                return true;
            }
        }
        return true;
    }

    private void renderControls(GuiGraphics graphics, Font font) {
        if (!showControls) {
            String hint = I18n.get("gui.recruitsrtscommand.controls.hint");
            graphics.drawString(font, hint, view.screenWidth() - font.width(hint) - 8,
                    view.screenHeight() - 12, 0x80FFFFFF);
            return;
        }

        String[] keys = {
                "drag_left", "shift_drag", "click_banner", "modifiers", "tab_esc",
                "store_group", "recall_group", "right_click", "shift_right", "tight", "hold", "face", "shape", "zone", "pick",
        };
        String[][] entries = new String[keys.length][2];
        for (int index = 0; index < keys.length; index++) {
            entries[index][0] = I18n.get("gui.recruitsrtscommand.controls." + keys[index]);
            entries[index][1] = I18n.get("gui.recruitsrtscommand.controls." + keys[index] + ".desc");
        }

        int keyWidth = 0;
        for (String[] entry : entries) {
            keyWidth = Math.max(keyWidth, font.width(entry[0]));
        }
        int x = 10;
        int y = view.screenHeight() - entries.length * 10 - 18;
        for (int index = 0; index < entries.length; index++) {
            int lineY = y + index * 10;
            graphics.drawString(font, entries[index][0], x, lineY, 0xFFE6C56A);
            graphics.drawString(font, entries[index][1], x + keyWidth + 8, lineY, 0xFFD8DEE9);
        }
    }

    private String tooltipAt(double mouseX, double mouseY, boolean available) {
        if (isOver(mouseX, mouseY, toggleLeft(), TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            return I18n.get(available
                    ? "gui.recruitsrtscommand.toggle.tooltip"
                    : "gui.recruitsrtscommand.toggle.unavailable");
        }
        return null;
    }

    private void drawTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, String text) {
        if (text == null || text.isBlank()) return;
        int width = font.width(text) + 8;
        int x = Math.min(mouseX + 12, view.screenWidth() - width - 4);
        int y = Math.min(mouseY + 12, view.screenHeight() - 18);
        graphics.fill(x, y, x + width, y + 15, 0xD010141B);
        graphics.renderOutline(x, y, width, 15, OUTLINE_HOVERED);
        graphics.drawString(font, text, x + 4, y + 4, 0xFFFFFF, false);
    }

    private SquadClusters.Cluster findClusterAtScreen(double mouseX, double mouseY) {
        double size = markerIconSize();
        double reach = Math.max(MARKER_HIT_MIN, size / 2.0D + 2.0D);
        for (int i = clusters.size() - 1; i >= 0; i--) {
            SquadClusters.Cluster cluster = clusters.get(i);
            double x = markerCenterX(cluster);
            double y = markerTop(cluster) + size / 2.0D;
            if (Math.abs(mouseX - x) <= reach && Math.abs(mouseY - y) <= reach) return cluster;
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
                double x = worldToScreenX(smoothX(unit.unitId(), unit.position().getX()));
                double y = worldToScreenY(smoothZ(unit.unitId(), unit.position().getZ()));
                boolean held = MapCommandClientState.isUnitSelected(target);
                drawDiamondOutline(graphics, x, y, 5.0D, 1.8D, 0xC0101418);
                drawDiamondOutline(graphics, x, y, 4.2D, 1.0D, held ? 0xFFFF6E5A : SELECTED_COLOR);
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
                double x = worldToScreenX(smoothX(unit.unitId(), unit.position().getX()));
                double y = worldToScreenY(smoothZ(unit.unitId(), unit.position().getZ()));
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

    private void cycleFormation(int direction) {
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

    private List<CommandGroupSnapshot> currentSelections() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return List.of();
        return MapCommandClientState.selectedForDimensionSnapshots(minecraft.level);
    }

    private BlockPos screenToWorld(double mouseX, double mouseY) {
        int worldX = (int) Math.floor((mouseX - view.offsetX()) / view.scale());
        int worldZ = (int) Math.floor((mouseY - view.offsetZ()) / view.scale());
        return new BlockPos(worldX, resolveSurfaceY(worldX, worldZ), worldZ);
    }

    private int resolveSurfaceY(int worldX, int worldZ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return 64;
        return Math.max(minecraft.level.getMinBuildHeight(), minecraft.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, worldX, worldZ));
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
        for (CommandGroupSnapshot snapshot : MapCommandClientState.snapshotsForDimension(minecraft.level)) {
            if (!localPlayerId.equals(snapshot.ownerId())) continue;
            for (CommandUnitSnapshot unit : snapshot.units()) {
                double x = worldToScreenX(unit.position().getX());
                double y = worldToScreenY(unit.position().getZ());
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

    private void renderZonePreview(GuiGraphics graphics) {
        if (movingZone != null) {
            drawFireZoneRing(graphics, zoneCenterX, zoneCenterY,
                    Math.max(3.0D, movingZone.radiusX() * view.scale()),
                    Math.max(3.0D, movingZone.radiusZ() * view.scale()),
                    movingZone.shape(), true);
            return;
        }
        if (!drawingZone) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        List<CommandGroupSnapshot> selected = MapCommandClientState.selectedForDimensionSnapshots(minecraft.level);
        if (selected.isEmpty()) return;

        int[] reach = zoneReachBlocks();
        double radiusX = Math.max(3.0D, reach[0] * view.scale());
        double radiusZ = Math.max(3.0D, reach[1] * view.scale());
        FireZoneShape shape = MapCommandClientState.fireZoneShape();

        drawFireZoneRing(graphics, zoneCenterX, zoneCenterY, radiusX, radiusZ, shape, true);

        Font font = Minecraft.getInstance().font;
        String reachText = shape == FireZoneShape.CIRCLE
                ? I18n.get("gui.recruitsrtscommand.zone.reach", reach[0])
                : I18n.get("gui.recruitsrtscommand.zone.reach_two", reach[0] * 2, reach[1] * 2);
        graphics.pose().pushPose();
        graphics.pose().translate(zoneCenterX + 4.0D, zoneCenterY - 12.0D, 0.0D);
        graphics.fill(-2, -2, font.width(reachText) + 2, 9, 0xC010141B);
        graphics.drawString(font, reachText, 0, 0, 0xFFFFC9C0, false);
        graphics.pose().popPose();

        int archers = 0;
        for (CommandGroupSnapshot squad : selected) {
            archers += squad.countOf(CommandUnitType.RANGED);
        }
        archers = Math.min(archers, ORDER_PREVIEW_LIMIT);
        for (int index = 0; index < archers; index++) {
            double spread = Math.sqrt((index + 0.5D) / archers);
            double angle = index * 2.39996322972865332D;
            double unitX = Math.cos(angle) * spread;
            double unitZ = Math.sin(angle) * spread;
            if (shape == FireZoneShape.RECTANGLE) {
                double longest = Math.max(Math.abs(unitX), Math.abs(unitZ));
                if (longest > 0.0001D) {
                    unitX = unitX / longest * spread;
                    unitZ = unitZ / longest * spread;
                }
            }
            MapRenderUtil.fill(graphics, zoneCenterX + unitX * radiusX, zoneCenterY + unitZ * radiusZ,
                    zoneCenterX + unitX * radiusX + 1.0D, zoneCenterY + unitZ * radiusZ + 1.0D, SELECTED_COLOR);
        }
    }

    private void panMap() {
        double dx = 0.0D;
        double dy = 0.0D;
        if (isKeyDown(GLFW.GLFW_KEY_A) || isKeyDown(GLFW.GLFW_KEY_LEFT)) dx += PAN_SPEED;
        if (isKeyDown(GLFW.GLFW_KEY_D) || isKeyDown(GLFW.GLFW_KEY_RIGHT)) dx -= PAN_SPEED;
        if (isKeyDown(GLFW.GLFW_KEY_W) || isKeyDown(GLFW.GLFW_KEY_UP)) dy += PAN_SPEED;
        if (isKeyDown(GLFW.GLFW_KEY_S) || isKeyDown(GLFW.GLFW_KEY_DOWN)) dy -= PAN_SPEED;

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

    private ResourceLocation iconForFormation(int formation) {
        return switch (formation) {
            case 1 -> ICON_FORMATION_LINE;
            case 2 -> ICON_FORMATION_SQUARE;
            case 3 -> ICON_FORMATION_TRIANGLE;
            case 4 -> ICON_FORMATION_HCIRCLE;
            case 5 -> ICON_FORMATION_HSQUARE;
            case 6 -> ICON_FORMATION_VFORM;
            case 7 -> ICON_FORMATION_CIRCLE;
            case 8 -> ICON_FORMATION_MOVEMENT;
            default -> ICON_FORMATION_NONE;
        };
    }

    private String formationName(int formation) {
        String key = switch (formation) {
            case 1 -> "gui.recruits.command.text.formation_lineup";
            case 2 -> "gui.recruits.command.text.formation_square";
            case 3 -> "gui.recruits.command.text.formation_triangle";
            case 4 -> "gui.recruits.command.text.formation_hollow_circle";
            case 5 -> "gui.recruits.command.text.formation_hollow_square";
            case 6 -> "gui.recruits.command.text.formation_v";
            case 7 -> "gui.recruits.command.text.formation_circle";
            case 8 -> "gui.recruits.command.text.formation_movement";
            default -> "gui.recruits.command.text.formation_none";
        };
        return I18n.get(key);
    }

    private ResourceLocation iconForSnapshot(CommandGroupSnapshot snapshot) {
        ResourceLocation chosen = ClientGroups.imageOf(snapshot.selectionId());
        if (chosen != null) return chosen;

        int infantry = snapshot.countOf(CommandUnitType.INFANTRY);
        int ranged = snapshot.countOf(CommandUnitType.RANGED);
        int cavalry = snapshot.countOf(CommandUnitType.CAVALRY);
        if (cavalry > 0 && cavalry >= infantry && cavalry >= ranged) return ICON_CAVALRY;
        if (ranged > 0 && ranged >= infantry) return ICON_RANGED;
        return ICON_INFANTRY;
    }

    private int relationColor(UUID ownerId) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID localPlayerId = minecraft.player == null ? null : minecraft.player.getUUID();
        if (localPlayerId != null && localPlayerId.equals(ownerId)) return OWN_COLOR;

        RecruitsFaction ownFaction = ClientManager.ownFaction;
        RecruitsFaction targetFaction = findFactionForOwner(ownerId);
        if (ownFaction == null || targetFaction == null) return NEUTRAL_COLOR;
        if (ownFaction.equalsFaction(targetFaction)) return ALLY_COLOR;

        RecruitsDiplomacyManager.DiplomacyStatus relation = ClientManager.getRelation(
                ownFaction.getStringID(), targetFaction.getStringID());
        if (relation == RecruitsDiplomacyManager.DiplomacyStatus.ENEMY) return ENEMY_COLOR;
        if (relation == RecruitsDiplomacyManager.DiplomacyStatus.ALLY) return ALLY_COLOR;
        return NEUTRAL_COLOR;
    }

    private int unitColor(CommandUnitType type, int relationColor) {
        int alpha = 0xFF000000;
        if (type == CommandUnitType.RANGED) return alpha | 0xFFD85A;
        if (type == CommandUnitType.CAVALRY) return alpha | 0x61D6FF;
        return relationColor;
    }

    private RecruitsFaction findFactionForOwner(UUID ownerId) {
        if (ClientManager.factions == null) return null;
        for (RecruitsFaction faction : ClientManager.factions) {
            for (RecruitsPlayerInfo member : faction.getMembers()) {
                if (Objects.equals(ownerId, member.getUUID())) return faction;
            }
        }
        return null;
    }

    private boolean isKeyDown(int keyCode) {
        Minecraft minecraft = Minecraft.getInstance();
        return GLFW.glfwGetKey(minecraft.getWindow().getWindow(), keyCode) == GLFW.GLFW_PRESS;
    }

    private static ResourceLocation recruitsIcon(String path) {
        return ResourceLocation.fromNamespaceAndPath("recruits", path);
    }

    private static ResourceLocation ownIcon(String path) {
        return ResourceLocation.fromNamespaceAndPath("recruitsrtscommand", path);
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value == null ? "" : value;
        if (maxLength <= 3) return value.substring(0, Math.min(value.length(), maxLength));
        return value.substring(0, maxLength - 3) + "...";
    }

    private static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
