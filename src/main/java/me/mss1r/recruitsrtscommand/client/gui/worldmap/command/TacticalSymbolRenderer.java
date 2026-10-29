package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import com.mojang.math.Axis;
import me.mss1r.recruitsrtscommand.client.render.MapRenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

final class TacticalSymbolRenderer {
    private TacticalSymbolRenderer() {}

    static void draw(GuiGraphics graphics, TacticalSymbolLayout layout, double x, double y,
                     int color, boolean selected, boolean hovered) {
        double halfWidth = layout.width() / 2;
        double halfHeight = layout.height() / 2;
        TacticalSymbolPalette palette = TacticalSymbolPalette.forRelation(color);
        double weight = Mth.clamp(layout.width() / 35, 0.45D, 0.75D);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().mulPose(Axis.ZP.rotation((float) layout.angle()));
            MapRenderUtil.fill(graphics, -halfWidth, -halfHeight, halfWidth, halfHeight, palette.face());
            int edge = selected ? 0xFFD7E1D3 : hovered ? 0xFFBEC9C9 : palette.edge();
            box(graphics, halfWidth, halfHeight, weight * 1.3D, edge);
            glyph(graphics, layout.kind(), halfWidth * 0.75D,
                    halfHeight * 0.70D, weight, palette.ink());
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void box(GuiGraphics graphics, double x, double y, double thickness, int color) {
        MapRenderUtil.line(graphics, -x, -y, x, -y, thickness, color);
        MapRenderUtil.line(graphics, x, -y, x, y, thickness, color);
        MapRenderUtil.line(graphics, x, y, -x, y, thickness, color);
        MapRenderUtil.line(graphics, -x, y, -x, -y, thickness, color);
    }

    private static void glyph(GuiGraphics graphics, TacticalSymbolLayout.Kind kind,
                              double x, double y, double weight, int ink) {
        switch (kind) {
            case INFANTRY -> {
                stroke(graphics, -x, -y, x, y, weight, ink);
                stroke(graphics, -x, y, x, -y, weight, ink);
            }
            case CAVALRY -> stroke(graphics, -x, y, x, -y, weight, ink);
            case RANGED -> {
                stroke(graphics, -x, y * 0.4D, -x * 0.6D, -y * 0.3D, weight, ink);
                stroke(graphics, -x * 0.6D, -y * 0.3D, 0, -y * 0.6D, weight, ink);
                stroke(graphics, 0, -y * 0.6D, x * 0.6D, -y * 0.3D, weight, ink);
                stroke(graphics, x * 0.6D, -y * 0.3D, x, y * 0.4D, weight, ink);
                stroke(graphics, -x, y * 0.4D, x, y * 0.4D, weight, ink);
                stroke(graphics, 0, y, 0, -y, weight, ink);
                stroke(graphics, -x * 0.2D, -y * 0.4D, 0, -y, weight, ink);
                stroke(graphics, x * 0.2D, -y * 0.4D, 0, -y, weight, ink);
            }
            case MIXED -> {
                stroke(graphics, -x, 0, x, 0, weight, ink);
                stroke(graphics, 0, -y, 0, y, weight, ink);
            }
        }
    }

    private static void stroke(GuiGraphics graphics, double x1, double y1, double x2, double y2,
                               double weight, int color) {
        MapRenderUtil.line(graphics, x1, y1, x2, y2, weight, color);
    }
}
