package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import me.mss1r.recruitsrtscommand.api.WorldMapView;
import me.mss1r.recruitsrtscommand.client.render.MapRenderUtil;
import me.mss1r.recruitsrtscommand.api.FireZoneShape;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandStyle.*;

final class MapCommandGeometry {
    private WorldMapView view;

    void setView(WorldMapView view) {
        this.view = view;
    }

    void drawFireZoneRing(GuiGraphics graphics, double centerX, double centerY,
                                  double radiusX, double radiusZ, FireZoneShape shape,
                                  boolean selected) {
        drawFireZoneRing(graphics, centerX, centerY, radiusX, radiusZ, shape, selected, false);
    }

    void drawFireZoneRing(GuiGraphics graphics, double centerX, double centerY,
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
            MapRenderUtil.outline(graphics, centerX - radiusX - 1.0D, centerY - radiusZ - 1.0D,
                    centerX + radiusX + 1.0D, centerY + radiusZ + 1.0D, shadow);
            MapRenderUtil.outline(graphics, centerX - radiusX, centerY - radiusZ,
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

    void drawZoneHatching(GuiGraphics graphics, double centerX, double centerY,
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

    void drawZoneCorners(GuiGraphics graphics, double centerX, double centerY,
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

    void drawEllipse(GuiGraphics graphics, double centerX, double centerY,
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

    void drawDisc(GuiGraphics graphics, double centerX, double centerY,
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

    void drawPathSegment(GuiGraphics graphics, double startX, double startY,
                                 double endX, double endY, int color, boolean selected) {
        double length = Math.hypot(endX - startX, endY - startY);
        if (length < 2.0D) return;

        drawRouteLine(graphics, startX, startY, endX, endY, color, selected);
    }

    void drawRouteLine(GuiGraphics graphics, double startX, double startY,
                               double endX, double endY, int color, boolean selected) {
        double thickness = (selected ? 1.6D : 1.2D) * routeUiScale();
        MapRenderUtil.line(graphics, startX, startY, endX, endY, thickness + 1.8D * routeUiScale(),
                0xC8101418);
        MapRenderUtil.line(graphics, startX, startY, endX, endY, thickness, routeSolidColor(color));
    }

    void drawWaypoint(GuiGraphics graphics, double x, double y, int color, boolean destination) {
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

    double routeUiScale() {
        return Mth.clamp(view.scale() * 0.85D, 0.4D, 1.0D);
    }

    void drawDiamondOutline(GuiGraphics graphics, double centerX, double centerY,
                                    double radius, double thickness, int color) {
        MapRenderUtil.line(graphics, centerX, centerY - radius, centerX + radius, centerY, thickness, color);
        MapRenderUtil.line(graphics, centerX + radius, centerY, centerX, centerY + radius, thickness, color);
        MapRenderUtil.line(graphics, centerX, centerY + radius, centerX - radius, centerY, thickness, color);
        MapRenderUtil.line(graphics, centerX - radius, centerY, centerX, centerY - radius, thickness, color);
    }

    int routeSolidColor(int color) {
        return 0xFF000000 | (color & 0x00FFFFFF);
    }
}
