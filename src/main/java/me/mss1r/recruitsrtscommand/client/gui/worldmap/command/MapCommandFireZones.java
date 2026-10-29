package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import me.mss1r.recruitsrtscommand.api.WorldMapView;
import me.mss1r.recruitsrtscommand.client.command.MapCommandClientState;
import me.mss1r.recruitsrtscommand.client.command.MapCommandNetworkClient;
import me.mss1r.recruitsrtscommand.client.command.MapCommandTool;
import me.mss1r.recruitsrtscommand.client.render.MapRenderUtil;
import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitType;
import me.mss1r.recruitsrtscommand.api.FireZoneShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandStyle.*;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandActions.*;

final class MapCommandFireZones {
    private WorldMapView view;
    private final MapCommandGeometry geometry;
    private boolean drawingZone;
    private MapCommandClientState.FireZone movingZone;
    private double movingGrabX;
    private double movingGrabY;
    private boolean movingZoneDragged;
    private double zoneCenterX;
    private double zoneCenterY;
    private double zoneEdgeX;
    private double zoneEdgeY;
    private static final int MIN_ZONE_RADIUS = 2;

    MapCommandFireZones(MapCommandGeometry geometry) {
        this.geometry = geometry;
    }

    void setView(WorldMapView view) {
        this.view = view;
    }

    void reset() {
        view = null;
        cancelGesture();
        movingGrabX = movingGrabY = 0;
        zoneCenterX = zoneCenterY = zoneEdgeX = zoneEdgeY = 0;
    }

    boolean cancelGesture() {
        boolean active = drawingZone || movingZone != null;
        drawingZone = false;
        movingZone = null;
        movingZoneDragged = false;
        return active;
    }

    boolean begin(double mouseX, double mouseY, List<CommandGroupSnapshot> selected) {
        MapCommandClientState.FireZone existing = zoneAt(mouseX, mouseY);
        if (existing != null) {
            movingZone = existing;
            movingZoneDragged = false;
            zoneCenterX = view.worldToScreenX(existing.target().getX());
            zoneCenterY = view.worldToScreenY(existing.target().getZ());
            movingGrabX = mouseX - zoneCenterX;
            movingGrabY = mouseY - zoneCenterY;
            return true;
        }

        // A fire zone goes to the selected engines' crews too, so it must not turn into a drive order.
        if (MapCommandClientState.activeTool() == MapCommandTool.FIRE
                && (!selected.isEmpty() || MapCommandClientState.hasObjectSelection())) {
            drawingZone = true;
            zoneCenterX = mouseX;
            zoneCenterY = mouseY;
            zoneEdgeX = mouseX;
            zoneEdgeY = mouseY;
            return true;
        }

        return false;
    }

    boolean drag(double mouseX, double mouseY, int button) {
        if (button == 1 && movingZone != null) {
            double wantX = mouseX - movingGrabX;
            double wantY = mouseY - movingGrabY;
            if (Math.hypot(wantX - view.worldToScreenX(movingZone.target().getX()),
                    wantY - view.worldToScreenY(movingZone.target().getZ())) > ZONE_DRAG_SLOP) {
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
        return false;
    }

    boolean release(double mouseX, double mouseY, int button) {
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
                    new ArrayList<>(zone.members()), view.screenToWorld(zoneCenterX, zoneCenterY),
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
        return false;
    }

    MapCommandClientState.FireZone zoneAt(double mouseX, double mouseY) {
        List<MapCommandClientState.FireZone> zones = MapCommandClientState.fireZones();
        for (int i = zones.size() - 1; i >= 0; i--) {
            MapCommandClientState.FireZone zone = zones.get(i);
            double centerX = view.worldToScreenX(zone.target().getX());
            double centerY = view.worldToScreenY(zone.target().getZ());
            double radiusX = Math.max(4.0D, zone.radiusX() * view.scale());
            double radiusZ = Math.max(4.0D, zone.radiusZ() * view.scale());
            if (zone.shape().contains(mouseX - centerX, mouseY - centerY, radiusX, radiusZ)) {
                return zone;
            }
        }
        return null;
    }

    void issueZoneOrder() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        List<UUID> gunners = MapCommandClientState.selectedGunners();
        if (gunners.isEmpty()) return;

        int[] reach = zoneReachBlocks();
        if (reach[0] < MIN_ZONE_RADIUS || reach[1] < MIN_ZONE_RADIUS) {
            return;
        }
        MapCommandNetworkClient.strategicFire(MapCommandClientState.selectionId(),
                gunners, view.screenToWorld(zoneCenterX, zoneCenterY),
                reach[0], reach[1], MapCommandClientState.fireZoneShape());
    }

    int[] zoneReachBlocks() {
        BlockPos center = view.screenToWorld(zoneCenterX, zoneCenterY);
        BlockPos edge = view.screenToWorld(zoneEdgeX, zoneEdgeY);
        return radii(center, edge, MapCommandClientState.fireZoneShape());
    }

    static int[] radii(BlockPos center, BlockPos edge, FireZoneShape shape) {
        double dx = Math.abs((double) edge.getX() - center.getX());
        double dz = Math.abs((double) edge.getZ() - center.getZ());

        if (shape == FireZoneShape.CIRCLE) {
            int radius = (int) Math.round(Math.min(Math.hypot(dx, dz), MAX_ZONE_RADIUS));
            return new int[]{radius, radius};
        }
        return new int[]{
                (int) Math.round(Math.min(dx, MAX_ZONE_RADIUS)),
                (int) Math.round(Math.min(dz, MAX_ZONE_RADIUS))
        };
    }

    void renderFireZones(GuiGraphics graphics) {
        Set<UUID> selection = new HashSet<>(MapCommandClientState.selectedGunners());
        for (MapCommandClientState.FireZone zone : MapCommandClientState.fireZones()) {
            if (movingZone == zone) continue;
            geometry.drawFireZoneRing(graphics,
                    view.worldToScreenX(zone.target().getX()), view.worldToScreenY(zone.target().getZ()),
                    Math.max(4.0D, zone.radiusX() * view.scale()),
                    Math.max(4.0D, zone.radiusZ() * view.scale()),
                    zone.shape(), zone.commanded(selection), zone.siege());
        }
    }

    void renderFireZoneHint(GuiGraphics graphics, Font font, int barTop) {
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
        graphics.drawString(font, hint, x, barTop - 14, 0xFFFFC9C0);
    }

    void renderZonePreview(GuiGraphics graphics) {
        if (movingZone != null) {
            geometry.drawFireZoneRing(graphics, zoneCenterX, zoneCenterY,
                    Math.max(3.0D, movingZone.radiusX() * view.scale()),
                    Math.max(3.0D, movingZone.radiusZ() * view.scale()),
                    movingZone.shape(), true);
            return;
        }
        if (!drawingZone) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        List<CommandGroupSnapshot> selected = MapCommandClientState.selectedForDimensionSnapshots(minecraft.level);
        if (MapCommandClientState.selectedGunners().isEmpty()) return;

        int[] reach = zoneReachBlocks();
        double radiusX = Math.max(3.0D, reach[0] * view.scale());
        double radiusZ = Math.max(3.0D, reach[1] * view.scale());
        FireZoneShape shape = MapCommandClientState.fireZoneShape();

        geometry.drawFireZoneRing(graphics, zoneCenterX, zoneCenterY, radiusX, radiusZ, shape, true);

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
}
