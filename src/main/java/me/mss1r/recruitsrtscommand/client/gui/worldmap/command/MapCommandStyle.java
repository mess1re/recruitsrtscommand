package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import net.minecraft.resources.ResourceLocation;
import me.mss1r.recruitsrtscommand.common.command.CommandLimits;

final class MapCommandStyle {
    private MapCommandStyle() {
    }

    static final int TOGGLE_Y = 10;
    static final int TOGGLE_MARGIN = 10;
    static final int GEAR_SIZE = 20;
    static final int GEAR_GAP = 4;
    static final int TOGGLE_W = 42;
    static final int TOGGLE_H = 20;
    static final int STATUS_X = 10;
    static final int STATUS_Y = 60;
    static final int BUTTON_SIZE = 22;
    static final int TIGHT_WIDTH = BUTTON_SIZE;
    static final int BUTTON_GAP = 2;
    static final int BUTTON_BG = 0x80222631;
    static final int BUTTON_HOVERED = 0xA0444B5C;
    static final int BUTTON_SELECTED = 0xB02E6042;
    static final int BUTTON_DISABLED = 0x50222631;
    static final int OUTLINE = 0x50FFFFFF;
    static final int OUTLINE_HOVERED = 0x90FFFFFF;
    static final int OUTLINE_SELECTED = 0xFF2BEA68;
    static final int PATH_COLOR = 0xE8FFE66A;
    static final int FIRE_ZONE_COLOR = 0xE8FF665A;
    static final int SIEGE_ZONE_COLOR = 0xE8FFA83A;
    static final int SELECTED_COLOR = 0xFFFFFFFF;
    static final int MARKER_SELECTED_COLOR = 0xFF4FA3FF;
    static final int HUD_ICON_SIZE = 21;
    static final int TEXTURE_ICON_SIZE = 21;
    static final int ORDER_PREVIEW_LIMIT = 60;
    static final int MARKER_ICON = 21;
    static final double OBJECT_ICON_FLOOR = 3.5D;
    static final double MARKER_ICON_FLOOR = 5.0D;
    static final double MARKER_LIFT_MIN = 6.0D;
    static final double MARKER_HIT_MIN = 7.0D;

    static final double PICK_REACH = 6.0D;
    static final long DOUBLE_CLICK_MILLIS = 350L;
    static final int[][] OUTLINE_OFFSETS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
    static final double UNIT_DOT_MIN_ZOOM = 0.60D;
    static final double MAX_ZONE_RADIUS = CommandLimits.MAX_FIRE_RADIUS;
    static final double ZONE_DRAG_SLOP = 3.0D;
    static final int BUTTON_FORMATION = 0;
    static final int BUTTON_FIRE_ZONE = 1;
    static final int BUTTON_HALT = 2;
    static final int BUTTON_CEASE_FIRE = 3;
    static final int BUTTON_FIRE_AT_WILL = 4;
    static final int BUTTON_FACE = 5;
    static final int BUTTON_PICK = 6;
    static final int BUTTON_AGGRO = 7;
    static final int BUTTON_SHIELDS = 8;
    static final int BAR_BUTTONS = 9;
    static final int[] FORMATION_CYCLE = {7, 4, 2, 5, 1, 3, 6, 8, 0};

    static final ResourceLocation ICON_FORMATION_NONE = recruitsIcon("textures/gui/image/none.png");
    static final ResourceLocation ICON_FORMATION_LINE = recruitsIcon("textures/gui/image/line.png");
    static final ResourceLocation ICON_FORMATION_SQUARE = recruitsIcon("textures/gui/image/square.png");
    static final ResourceLocation ICON_FORMATION_TRIANGLE = recruitsIcon("textures/gui/image/triangle.png");
    static final ResourceLocation ICON_FORMATION_HCIRCLE = recruitsIcon("textures/gui/image/hcircle.png");
    static final ResourceLocation ICON_FORMATION_HSQUARE = recruitsIcon("textures/gui/image/hsquare.png");
    static final ResourceLocation ICON_FORMATION_VFORM = recruitsIcon("textures/gui/image/vform.png");
    static final ResourceLocation ICON_FORMATION_CIRCLE = recruitsIcon("textures/gui/image/circle.png");
    static final ResourceLocation ICON_FORMATION_MOVEMENT = recruitsIcon("textures/gui/image/movement.png");
    static final ResourceLocation ICON_FIRE_ZONE = recruitsIcon("textures/gui/image/group/3arrow.png");
    static final ResourceLocation ICON_FIRE_AT_WILL = recruitsIcon("textures/gui/image/group/bow.png");
    static final ResourceLocation ICON_FACE = recruitsIcon("textures/gui/image/group/arrow.png");
    static final ResourceLocation ICON_PICK = recruitsIcon("textures/gui/image/group/spyglass.png");
    static final int ENEMY_MENU_LINE = 9;
    static final int ENEMY_MENU_ROW = 13;
    static final int ENEMY_MENU_HINT_LINES = 2;
    static final int ENEMY_MENU_HEAD = 26;
    static final int ENEMY_MENU_PAD = 8;
    static final int OBJECT_ROUTE_COLOR = 0xA07FB2C8;

    static final int ENEMY_MENU_MAX_WIDTH = 240;
    static final int ENEMY_MENU_MIN_WIDTH = 150;
    static final float ENEMY_MENU_Z = 450.0F;
    static final int MENU_BACK = 0xD0000000;
    static final int MENU_EDGE = 0xFFFFFFFF;
    static final int MENU_RULE = 0x66FFFFFF;
    static final int MENU_ROW_HOVERED = 0x33FFFFFF;
    static final int MENU_LABEL = 0xFFB8B8B8;
    static final int MENU_VALUE = 0xFFFFFFFF;
    static final int MENU_TEXT_OFF = 0xFF777777;
    static final ResourceLocation ICON_CEASE_FIRE = recruitsIcon("textures/gui/image/none.png");
    static final ResourceLocation ICON_AGGRO_NEUTRAL = recruitsIcon("textures/gui/image/neutral.png");
    static final ResourceLocation ICON_AGGRO_AGGRESSIVE = recruitsIcon("textures/gui/image/enemy.png");
    static final ResourceLocation ICON_AGGRO_RAID = recruitsIcon("textures/gui/image/group/axe.png");
    static final ResourceLocation ICON_AGGRO_PASSIVE = recruitsIcon("textures/gui/image/ally.png");
    static final ResourceLocation ICON_SHIELDS = recruitsIcon("textures/gui/image/group/shield.png");
    static final ResourceLocation ICON_HOLD = ownIcon("textures/gui/hold_position.png");
    static final ResourceLocation ICON_INFANTRY = recruitsIcon("textures/gui/image/group/sword.png");
    static final ResourceLocation ICON_RANGED = recruitsIcon("textures/gui/image/group/bow.png");
    static final ResourceLocation ICON_CAVALRY = recruitsIcon("textures/gui/image/group/horse.png");

    private static ResourceLocation recruitsIcon(String path) {
        return ResourceLocation.fromNamespaceAndPath("recruits", path);
    }

    private static ResourceLocation ownIcon(String path) {
        return ResourceLocation.fromNamespaceAndPath("recruitsrtscommand", path);
    }

}
