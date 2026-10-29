package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.ClientManager;
import me.mss1r.recruitsrtscommand.api.WorldMapView;
import me.mss1r.recruitsrtscommand.client.command.MapCommandClientState;
import me.mss1r.recruitsrtscommand.client.command.ClientGroups;
import me.mss1r.recruitsrtscommand.client.command.SquadClusters;
import me.mss1r.recruitsrtscommand.client.command.MapPositionSmoothing;
import me.mss1r.recruitsrtscommand.client.command.MapCommandTool;
import me.mss1r.recruitsrtscommand.client.render.MapRenderUtil;
import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandStyle.*;

final class MapCommandUnitLayer {
    private WorldMapView view;
    private final ToIntFunction<UUID> relations;
    private final SquadClusters.Cache clusterCache = new SquadClusters.Cache();
    List<SquadClusters.Cluster> clusters = List.of();
    SquadClusters.Cluster hoveredCluster;

    MapCommandUnitLayer(ToIntFunction<UUID> relations) {
        this.relations = relations;
    }

    void setView(WorldMapView view) {
        this.view = view;
    }

    void reset() {
        view = null;
        hoveredCluster = null;
        clusters = List.of();
        clusterCache.clear();
    }

    void refreshClusters(List<CommandGroupSnapshot> snapshots) {
        clusters = clusterCache.update(MapCommandClientState.snapshotRevision(), snapshots);
    }

    private double worldToScreenX(double x) {
        return view.worldToScreenX(x);
    }

    private double worldToScreenY(double z) {
        return view.worldToScreenY(z);
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value == null ? "" : value;
        if (maxLength <= 3) return value.substring(0, Math.min(value.length(), maxLength));
        return value.substring(0, maxLength - 3) + "...";
    }

    void renderSquadMarker(GuiGraphics graphics, SquadClusters.Cluster cluster, boolean selected, boolean hovered) {
        if (usesTacticalSymbols()) {
            TacticalSymbolLayout layout = TacticalSymbolLayout.of(cluster.body(), view.scale());
            double x = markerCenterX(cluster);
            double y = worldToScreenY(cluster.centerZ());
            TacticalSymbolRenderer.draw(graphics, layout, x, y, relations.applyAsInt(cluster.ownerId()), selected, hovered);
            if (layout.width() >= 12 || selected || hovered) {
                double scale = Mth.clamp(layout.width() / 22, 0.65D, 1);
                double countWidth = Minecraft.getInstance().font.width(strengthLabel(cluster)) * scale;
                renderSquadStrength(graphics, cluster, x - countWidth / 2,
                        y + layout.bottomExtent() + 3, scale);
            }
            return;
        }
        double centerX = markerCenterX(cluster);
        double size = markerIconSize();
        double top = markerTop(cluster);
        int relation = relations.applyAsInt(cluster.ownerId());

        drawBanner(graphics, iconForSnapshot(cluster.snapshot()), centerX - size / 2.0D, top, size);

        if (selected || hovered) {
            MapRenderUtil.outline(graphics, centerX - size / 2.0D - 2.0D, top - 2.0D,
                    centerX + size / 2.0D + 2.0D, top + size + 2.0D,
                    selected ? MARKER_SELECTED_COLOR : 0x90FFFFFF);
        }

        renderSquadHealth(graphics, cluster, centerX, top + size + 2.0D, size);
        renderStanceMarks(graphics, cluster, centerX - size / 2.0D, top, size);
        if (size >= MARKER_ICON) {
            renderSquadStrength(graphics, cluster, centerX + size / 2.0D + 2.0D, top + size - 8.0D);
        }
    }

    void renderStanceMarks(GuiGraphics graphics, SquadClusters.Cluster cluster,
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

    int aggroPipColor(int aggro) {
        return switch (aggro) {
            case 1 -> 0xFFE05A4A;
            case 2 -> 0xFFE09A3A;
            default -> 0xFF5AA0E0;
        };
    }

    void drawBanner(GuiGraphics graphics, ResourceLocation icon, double left, double top, double size) {
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

    void renderSquadHealth(GuiGraphics graphics, SquadClusters.Cluster cluster,
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

    int healthColor(int health) {
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

    void renderSquadStrength(GuiGraphics graphics, SquadClusters.Cluster cluster, double x, double y) {
        renderSquadStrength(graphics, cluster, x, y, 1.0D);
    }

    void renderSquadStrength(GuiGraphics graphics, SquadClusters.Cluster cluster, double x, double y, double scale) {
        Font font = Minecraft.getInstance().font;
        String strength = strengthLabel(cluster);
        int width = font.width(strength);

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0D);
        graphics.pose().scale((float) scale, (float) scale, 1.0F);
        graphics.fill(-1, -1, width + 1, 9, 0xE010141B);
        graphics.drawString(font, strength, 0, 0, 0xFFF2D98C, false);
        graphics.pose().popPose();
    }

    String strengthLabel(SquadClusters.Cluster cluster) {
        return cluster.whole() ? String.valueOf(cluster.strength())
                : cluster.strength() + "/" + cluster.snapshot().totalUnits();
    }

    double markerIconSize() {
        return Mth.clamp(view.scale() * MARKER_ICON * 0.5D, MARKER_ICON_FLOOR, MARKER_ICON);
    }

    double markerCenterX(SquadClusters.Cluster cluster) {
        return worldToScreenX(cluster.centerX());
    }

    double markerTop(SquadClusters.Cluster cluster) {
        double size = markerIconSize();
        double reach = view.scale() < UNIT_DOT_MIN_ZOOM ? 0.0D : cluster.reachNorth() * view.scale();
        return worldToScreenY(cluster.centerZ()) - Math.max(MARKER_LIFT_MIN, reach) - size - 2.0D;
    }

    double smoothX(UUID id, double fallback) {
        return MapPositionSmoothing.x(id, fallback);
    }

    double smoothZ(UUID id, double fallback) {
        return MapPositionSmoothing.z(id, fallback);
    }

    String squadTooltip(SquadClusters.Cluster cluster) {
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

    String squadName(CommandGroupSnapshot snapshot) {
        String name = ClientGroups.nameOf(snapshot.selectionId());
        return abbreviate(name == null ? I18n.get("gui.recruitsrtscommand.status.unnamed") : name, 22);
    }

    void appendGroupState(StringBuilder text, Map<UUID, Component> states, UUID groupId) {
        if (states == null || groupId == null) return;
        Component state = states.get(groupId);
        if (state == null) return;
        String line = state.getString();
        if (line.isBlank()) return;
        text.append("   ").append(abbreviate(line, 22));
    }

    void renderUnitDots(GuiGraphics graphics, CommandGroupSnapshot snapshot, boolean selected) {
        if (snapshot.units() == null || snapshot.units().isEmpty()) return;
        if (usesTacticalSymbols()) return;
        if (!selected && view.scale() < UNIT_DOT_MIN_ZOOM
                && MapCommandClientState.activeTool() != MapCommandTool.PICK) return;

        int relation = relations.applyAsInt(snapshot.ownerId());
        Minecraft minecraft = Minecraft.getInstance();
        boolean ours = minecraft.player != null && minecraft.player.getUUID().equals(snapshot.ownerId());
        boolean anySelected = ours && MapCommandClientState.hasSelection();
        double radius = Mth.clamp(view.scale() * 1.0D, 0.9D, 1.7D);
        for (CommandUnitSnapshot unit : snapshot.units()) {
            MapRenderUtil.dot(graphics, worldToScreenX(smoothX(unit.unitId(), unit.position().getX())),
                    worldToScreenY(smoothZ(unit.unitId(), unit.position().getZ())),
                    radius + 0.7D, 0xC8101418);
        }

        for (CommandUnitSnapshot unit : snapshot.units()) {
            boolean commanded = !anySelected || MapCommandClientState.isUnitSelected(unit.unitId());
            int color = relation;
            MapRenderUtil.dot(graphics, worldToScreenX(smoothX(unit.unitId(), unit.position().getX())),
                    worldToScreenY(smoothZ(unit.unitId(), unit.position().getZ())),
                    radius, commanded ? color : dimmed(color));
        }
    }

    int dimmed(int color) {
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

    SquadClusters.Cluster findClusterAtScreen(double mouseX, double mouseY) {
        double size = markerIconSize();
        double reach = Math.max(MARKER_HIT_MIN, size / 2.0D + 2.0D);
        for (int i = clusters.size() - 1; i >= 0; i--) {
            SquadClusters.Cluster cluster = clusters.get(i);
            double x = markerCenterX(cluster);
            if (usesTacticalSymbols()) {
                double y = worldToScreenY(cluster.centerZ());
                if (TacticalSymbolLayout.of(cluster.body(), view.scale()).contains(mouseX - x, mouseY - y, 2)) return cluster;
                continue;
            }
            double y = markerTop(cluster) + size / 2.0D;
            if (Math.abs(mouseX - x) <= reach && Math.abs(mouseY - y) <= reach) return cluster;
        }
        return null;
    }

    ResourceLocation iconForSnapshot(CommandGroupSnapshot snapshot) {
        ResourceLocation chosen = ClientGroups.imageOf(snapshot.selectionId());
        if (chosen != null) return chosen;

        int infantry = snapshot.countOf(CommandUnitType.INFANTRY);
        int ranged = snapshot.countOf(CommandUnitType.RANGED);
        int cavalry = snapshot.countOf(CommandUnitType.CAVALRY);
        if (cavalry > 0 && cavalry >= infantry && cavalry >= ranged) return ICON_CAVALRY;
        if (ranged > 0 && ranged >= infantry) return ICON_RANGED;
        return ICON_INFANTRY;
    }

    boolean usesTacticalSymbols() {
        return TacticalSymbolLayout.visible(view.scale(), MapCommandClientState.activeTool() == MapCommandTool.PICK);
    }
}
