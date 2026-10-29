package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import com.talhanation.recruits.client.ClientManager;
import me.mss1r.recruitsrtscommand.api.WorldMapView;
import me.mss1r.recruitsrtscommand.client.command.MapCommandClientState;
import me.mss1r.recruitsrtscommand.client.command.MapCommandNetworkClient;
import me.mss1r.recruitsrtscommand.client.command.SquadClusters;
import me.mss1r.recruitsrtscommand.client.command.MapCommandTool;
import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandStyle.*;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandActions.*;
import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandWidgets.*;

final class MapCommandBar {
    private WorldMapView view;
    boolean showControls;
    boolean formationOpen;
    boolean aggroOpen;
    private final List<TypeSegment> typeSegments = new ArrayList<>();
    private static final int[] AGGRO_CYCLE = {3, 0, 1, 2};

    void setView(WorldMapView view) {
        this.view = view;
    }

    void reset() {
        view = null;
        showControls = false;
        formationOpen = false;
        aggroOpen = false;
        typeSegments.clear();
    }

    void renderSelectedStatus(GuiGraphics graphics, Font font, List<CommandGroupSnapshot> selected) {
        typeSegments.clear();
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

    int drawTypeCount(GuiGraphics graphics, Font font, int x, CommandUnitType type, int count,
                              int kinds, String key) {
        if (count <= 0) return x;

        String text = I18n.get(key, count);
        int width = font.width(text);
        boolean pressable = kinds > 1;
        if (pressable) typeSegments.add(new TypeSegment(type, x, width));

        graphics.drawString(font, text, x, STATUS_Y + 11, pressable ? 0xFFD7E3F0 : 0xFFB8C2CE);
        return x + width + 8;
    }

    record TypeSegment(CommandUnitType type, int x, int width) {
    }

    CommandUnitType typeSegmentAt(double mouseX, double mouseY) {
        for (TypeSegment segment : typeSegments) {
            if (isOver(mouseX, mouseY, segment.x(), STATUS_Y + 11, segment.width(), 9)) {
                return segment.type();
            }
        }
        return null;
    }

    void renderCommandBar(GuiGraphics graphics, Font font, int mouseX, int mouseY, String squadTooltip) {
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
        if (label == null) label = squadTooltip;
        if (label != null) {
            drawTooltip(graphics, font, mouseX, mouseY, label, view);
        }
    }

    void renderFormationPicker(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
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

    void renderAggroPicker(GuiGraphics graphics, int mouseX, int mouseY) {
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

    int aggroPickerLeft() {
        int width = AGGRO_CYCLE.length * BUTTON_SIZE + (AGGRO_CYCLE.length - 1) * BUTTON_GAP;
        int over = barLeft() + BUTTON_AGGRO * (BUTTON_SIZE + BUTTON_GAP) + (BUTTON_SIZE - width) / 2;
        return Mth.clamp(over, 5, Math.max(5, view.screenWidth() - width - 5));
    }

    int aggroIndexAt(double mouseX, double mouseY) {
        if (!aggroOpen) return -1;
        int x = aggroPickerLeft();
        int y = formationPickerTop();
        for (int index = 0; index < AGGRO_CYCLE.length; index++) {
            int buttonX = x + index * (BUTTON_SIZE + BUTTON_GAP);
            if (isOver(mouseX, mouseY, buttonX, y, BUTTON_SIZE, BUTTON_SIZE)) return index;
        }
        return -1;
    }

    int formationPickerLeft() {
        int width = FORMATION_CYCLE.length * BUTTON_SIZE + (FORMATION_CYCLE.length - 1) * BUTTON_GAP
                + BUTTON_GAP * 5 + TIGHT_WIDTH * 2;
        return Mth.clamp((view.screenWidth() - width) / 2, 5, Math.max(5, view.screenWidth() - width - 5));
    }

    int formationPickerTop() {
        return barTop() - BUTTON_SIZE - 14;
    }

    String tightLabel() {
        return MapCommandClientState.tightFormation() ? "[X]" : "[ ]";
    }

    String holdLabel() {
        return MapCommandClientState.holdFormation() ? "[X]" : "[ ]";
    }

    int holdButtonLeft() {
        return tightButtonLeft() + TIGHT_WIDTH + BUTTON_GAP;
    }

    boolean isOverHold(double mouseX, double mouseY) {
        return formationOpen
                && isOver(mouseX, mouseY, holdButtonLeft(), formationPickerTop(), TIGHT_WIDTH, BUTTON_SIZE);
    }

    int tightButtonLeft() {
        int width = FORMATION_CYCLE.length * BUTTON_SIZE + (FORMATION_CYCLE.length - 1) * BUTTON_GAP;
        return formationPickerLeft() + width + BUTTON_GAP * 4;
    }

    boolean isOverTight(double mouseX, double mouseY) {
        return formationOpen
                && isOver(mouseX, mouseY, tightButtonLeft(), formationPickerTop(), TIGHT_WIDTH, BUTTON_SIZE);
    }

    int formationIndexAt(double mouseX, double mouseY) {
        if (!formationOpen) return -1;
        int x = formationPickerLeft();
        int y = formationPickerTop();
        for (int index = 0; index < FORMATION_CYCLE.length; index++) {
            int buttonX = x + index * (BUTTON_SIZE + BUTTON_GAP);
            if (isOver(mouseX, mouseY, buttonX, y, BUTTON_SIZE, BUTTON_SIZE)) return index;
        }
        return -1;
    }

    int barLeft() {
        return (view.screenWidth() - (BAR_BUTTONS * BUTTON_SIZE + (BAR_BUTTONS - 1) * BUTTON_GAP)) / 2;
    }

    int barTop() {
        return view.screenHeight() - BUTTON_SIZE - 34;
    }

    ResourceLocation barIcon(int index) {
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

    boolean needsSelection(int index) {
        return switch (index) {
            case BUTTON_FORMATION, BUTTON_FIRE_ZONE, BUTTON_FACE, BUTTON_PICK -> false;
            default -> true;
        };
    }

    boolean takesObjects(int index) {
        return index == BUTTON_HALT || index == BUTTON_CEASE_FIRE;
    }

    ResourceLocation iconForAggro(int aggro) {
        return switch (aggro) {
            case 1 -> ICON_AGGRO_AGGRESSIVE;
            case 2 -> ICON_AGGRO_RAID;
            case 3 -> ICON_AGGRO_PASSIVE;
            default -> ICON_AGGRO_NEUTRAL;
        };
    }

    String aggroName(int aggro) {
        return I18n.get(switch (aggro) {
            case 1 -> "gui.recruitsrtscommand.aggro.aggressive";
            case 2 -> "gui.recruitsrtscommand.aggro.raid";
            case 3 -> "gui.recruitsrtscommand.aggro.passive";
            default -> "gui.recruitsrtscommand.aggro.neutral";
        });
    }

    boolean barSelected(int index) {
        MapCommandTool tool = MapCommandClientState.activeTool();
        return (index == BUTTON_FIRE_ZONE && tool == MapCommandTool.FIRE)
                || (index == BUTTON_FACE && tool == MapCommandTool.FACE)
                || (index == BUTTON_PICK && tool == MapCommandTool.PICK)
                || (index == BUTTON_AGGRO && aggroOpen)
                || (index == BUTTON_SHIELDS && selectedShields())
                || (index == BUTTON_FIRE_AT_WILL && selectedFiring());
    }

    String barLabel(double mouseX, double mouseY) {
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

    int barIndexAt(double mouseX, double mouseY) {
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

    boolean clickCommandBar(double mouseX, double mouseY) {
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

    void renderControls(GuiGraphics graphics, Font font) {
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

    ResourceLocation iconForFormation(int formation) {
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

    String formationName(int formation) {
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
}
