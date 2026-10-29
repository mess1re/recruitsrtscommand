package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import me.mss1r.recruitsrtscommand.api.WorldMapView;
import me.mss1r.recruitsrtscommand.client.command.MapCommandClientState;
import me.mss1r.recruitsrtscommand.client.render.MapRenderUtil;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

import java.util.List;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandStyle.*;

final class MapCommandObjects {
    private WorldMapView view;
    private final MapCommandUnitLayer units;
    private final MapCommandRelations relations;
    private final MapCommandGeometry geometry;

    MapCommandObjects(MapCommandUnitLayer units, MapCommandRelations relations, MapCommandGeometry geometry) {
        this.units = units;
        this.relations = relations;
        this.geometry = geometry;
    }

    void setView(WorldMapView view) {
        this.view = view;
    }

    void renderObjectSelection(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !MapCommandClientState.hasObjectSelection()) return;

        for (MapObjectSnapshot object : MapCommandClientState.objects(minecraft.level)) {
            if (!MapCommandClientState.isObjectSelected(object.id())) continue;
            double size = objectIconSize(object);
            double left = view.worldToScreenX(object.position().getX()) - size / 2.0D;
            double top = view.worldToScreenY(object.position().getZ()) - size / 2.0D;
            MapRenderUtil.outline(graphics, left - 2.0D, top - 2.0D, left + size + 2.0D, top + size + 2.0D,
                    MARKER_SELECTED_COLOR);
        }
    }

    void renderObjectRoutes(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        for (MapObjectSnapshot object : MapCommandClientState.objects(minecraft.level)) {
            List<BlockPos> route = object.route();
            if (route.isEmpty()) continue;

            double fromX = view.worldToScreenX(object.position().getX());
            double fromY = view.worldToScreenY(object.position().getZ());
            for (int leg = 0; leg < route.size(); leg++) {
                double toX = view.worldToScreenX(route.get(leg).getX());
                double toY = view.worldToScreenY(route.get(leg).getZ());
                geometry.drawPathSegment(graphics, fromX, fromY, toX, toY, OBJECT_ROUTE_COLOR, false);
                geometry.drawWaypoint(graphics, toX, toY, OBJECT_ROUTE_COLOR, leg == route.size() - 1);
                fromX = toX;
                fromY = toY;
            }
        }
        graphics.flush();
    }

    void renderObjects(GuiGraphics graphics, Minecraft minecraft) {
        if (minecraft.level == null) return;

        for (MapObjectSnapshot object : MapCommandClientState.objects(minecraft.level)) {
            double size = objectIconSize(object);
            double centerX = view.worldToScreenX(object.position().getX());
            double centerY = view.worldToScreenY(object.position().getZ());
            double left = centerX - size / 2.0D;
            double top = centerY - size / 2.0D;

            drawObjectIcon(graphics, object.icon(), left, top, size);

            if (size >= OBJECT_ICON_FLOOR + 2.0D) {
                double pip = Math.max(2.0D, size * 0.16D);
                MapRenderUtil.fill(graphics, left - 1.0D, top - 1.0D, left + pip + 1.0D, top + pip + 1.0D,
                        relations.relationColor(object.ownerId()));
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

    MapObjectSnapshot objectAtScreen(double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;

        List<MapObjectSnapshot> objects = MapCommandClientState.objects(minecraft.level);
        for (int index = objects.size() - 1; index >= 0; index--) {
            MapObjectSnapshot object = objects.get(index);
            double reach = Math.max(MARKER_HIT_MIN, objectIconSize(object) / 2.0D + 2.0D);
            double dx = mouseX - view.worldToScreenX(object.position().getX());
            double dy = mouseY - view.worldToScreenY(object.position().getZ());
            if (Math.abs(dx) <= reach && Math.abs(dy) <= reach) return object;
        }
        return null;
    }

    double objectIconSize(MapObjectSnapshot object) {
        double base = Math.max(OBJECT_ICON_FLOOR, units.markerIconSize() * 0.6D);
        return base * Math.max(0.25F, object.icon().scale());
    }

    void drawObjectIcon(GuiGraphics graphics, me.mss1r.recruitsrtscommand.api.MapIcon icon,
                                double left, double top, double size) {
        if (icon.isItem()) {
            drawItemIcon(graphics, icon, left, top, size);
            return;
        }
        if (icon.texture() == null) return;

        if (icon.smooth()) {
            Minecraft.getInstance().getTextureManager().getTexture(icon.texture()).setFilter(true, false);
        }
        units.drawBanner(graphics, icon.texture(), left, top, size);
    }

    void drawItemIcon(GuiGraphics graphics, me.mss1r.recruitsrtscommand.api.MapIcon icon,
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

    void renderObjectCondition(GuiGraphics graphics, MapObjectSnapshot object,
                                       double centerX, double top, double size) {
        double width = size * 0.6D;
        double left = centerX - width / 2.0D;
        double height = Math.max(1.0D, size * 0.12D);
        MapRenderUtil.fill(graphics, left - 1.0D, top - 1.0D, left + width + 1.0D, top + height + 1.0D,
                0xC0101418);
        MapRenderUtil.fill(graphics, left, top, left + width, top + height, 0xFF3A2018);
        MapRenderUtil.fill(graphics, left, top, left + width * (object.condition() / 100.0D), top + height,
                units.healthColor(object.condition()));
    }
}
