package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import com.mojang.blaze3d.systems.RenderSystem;
import me.mss1r.recruitsrtscommand.api.WorldMapView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandStyle.*;

final class MapCommandWidgets {
    private MapCommandWidgets() {
    }

    static void drawIconButton(GuiGraphics graphics, int x, int y, ResourceLocation icon, String fallback,
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

    static void drawTextButton(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                String label, boolean selected, boolean enabled, boolean hovered) {
        int bg = !enabled ? BUTTON_DISABLED : selected ? BUTTON_SELECTED : hovered ? BUTTON_HOVERED : BUTTON_BG;
        int outline = selected ? OUTLINE_SELECTED : hovered ? OUTLINE_HOVERED : OUTLINE;
        graphics.fill(x, y, x + width, y + height, bg);
        graphics.renderOutline(x, y, width, height, outline);
        graphics.drawCenteredString(font, label, x + width / 2, y + (height - 8) / 2, enabled ? 0xFFFFFF : 0xFF999999);
    }

    static void drawTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, String text, WorldMapView view) {
        if (text == null || text.isBlank()) return;
        int width = font.width(text) + 8;
        int x = Math.min(mouseX + 12, view.screenWidth() - width - 4);
        int y = Math.min(mouseY + 12, view.screenHeight() - 18);
        graphics.fill(x, y, x + width, y + 15, 0xD010141B);
        graphics.renderOutline(x, y, width, 15, OUTLINE_HOVERED);
        graphics.drawString(font, text, x + 4, y + 4, 0xFFFFFF, false);
    }

    static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
