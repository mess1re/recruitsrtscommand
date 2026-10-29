package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import me.mss1r.recruitsrtscommand.api.WorldMapView;
import me.mss1r.recruitsrtscommand.client.command.MapCommandClientState;
import me.mss1r.recruitsrtscommand.client.command.MapCommandNetworkClient;
import me.mss1r.recruitsrtscommand.client.command.SquadClusters;
import me.mss1r.recruitsrtscommand.api.MapObjectAction;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.MapCommandStyle.*;

final class MapCommandMenus {
    private WorldMapView view;
    private final MapCommandUnitLayer units;
    private final MapCommandRelations relations;
    private SquadClusters.Cluster enemyMenuTarget;
    private MapObjectSnapshot objectMenuTarget;
    private PendingObjectOrder pendingObjectOrder;
    private double enemyMenuX;
    private double enemyMenuY;
    private static final String ATTACK_OBJECT = "recruitsrtscommand:attack";
    private static final String[] EMPTY_NOTE = new String[0];

    MapCommandMenus(MapCommandUnitLayer units, MapCommandRelations relations) {
        this.units = units;
        this.relations = relations;
    }

    void setView(WorldMapView view) {
        this.view = view;
    }

    void reset() {
        view = null;
        enemyMenuTarget = null;
        objectMenuTarget = null;
        pendingObjectOrder = null;
        enemyMenuX = enemyMenuY = 0;
    }

    boolean hasOpenMenu() {
        return enemyMenuTarget != null || objectMenuTarget != null;
    }

    void openEnemy(SquadClusters.Cluster target, double x, double y) {
        enemyMenuTarget = target;
        enemyMenuX = x;
        enemyMenuY = y;
    }

    void openObject(MapObjectSnapshot target, double x, double y) {
        objectMenuTarget = target;
        enemyMenuX = x;
        enemyMenuY = y;
    }

    boolean escape() {
        if (pendingObjectOrder != null) {
            pendingObjectOrder = null;
            return true;
        }
        if (!hasOpenMenu()) return false;
        enemyMenuTarget = null;
        objectMenuTarget = null;
        return true;
    }

    boolean clickOpenMenu(double mouseX, double mouseY, int button) {
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
                        view.screenToWorld(mouseX, mouseY));
            }
            return true;
        }

        if (enemyMenuTarget != null) {
            int row = enemyMenuRowAt(mouseX, mouseY);
            SquadClusters.Cluster enemy = enemyMenuTarget;
            enemyMenuTarget = null;
            if (row == 0 && relations.mayAttack(enemy.ownerId())) orderAgainstEnemy(enemy);
            return true;
        }

        return false;
    }

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

    void renderEnemyMenu(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
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

    void drawMenu(GuiGraphics graphics, Font font, EnemyMenu menu, int mouseX, int mouseY) {
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

    static List<String> wrap(Font font, String text, int room) {
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

    record PendingObjectOrder(UUID objectId, String actionId, String label) {
    }

    EnemyMenu enemyMenuLayout() {
        if (objectMenuTarget != null) return objectMenuLayout();
        if (enemyMenuTarget == null) return null;

        boolean anyMen = MapCommandClientState.hasSelection();
        boolean allowed = relations.mayAttack(enemyMenuTarget.ownerId());
        String footer = !allowed
                ? I18n.get("gui.recruitsrtscommand.enemy.friendly")
                : anyMen ? null : I18n.get("gui.recruitsrtscommand.enemy.no_men");
        return new EnemyMenu(
                Minecraft.getInstance().font,
                units.squadName(enemyMenuTarget.snapshot()) + "   " + enemyMenuTarget.strength(),
                new String[]{I18n.get("gui.recruitsrtscommand.enemy.attack")},
                new String[]{I18n.get("gui.recruitsrtscommand.enemy.attack.hint")},
                new boolean[]{anyMen && allowed},
                footer);
    }

    EnemyMenu objectMenuLayout() {
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

    List<MapObjectAction> objectMenuActions(MapObjectSnapshot object) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID localPlayerId = minecraft.player == null ? null : minecraft.player.getUUID();
        if (localPlayerId == null || localPlayerId.equals(object.ownerId())) {
            return object.actions();
        }

        Component label = Component.translatable("gui.recruitsrtscommand.enemy.attack");
        Component hint = Component.translatable("gui.recruitsrtscommand.enemy.attack.hint");
        MapObjectAction attack;
        if (!relations.mayAttack(object.ownerId())) {
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

    static List<List<MapObjectAction>> menuRows(List<MapObjectAction> actions) {
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

    static MapObjectAction shownOption(List<MapObjectAction> options) {
        for (MapObjectAction option : options) {
            if (option.selected()) return option;
        }
        return options.get(0);
    }

    static MapObjectAction nextOption(List<MapObjectAction> options) {
        int current = options.indexOf(shownOption(options));
        for (int step = 1; step <= options.size(); step++) {
            MapObjectAction candidate = options.get((current + step) % options.size());
            if (candidate.enabled() && !candidate.selected()) return candidate;
        }
        return null;
    }

    MapObjectSnapshot freshObjectMenuTarget() {
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

    int enemyMenuRowAt(double mouseX, double mouseY) {
        EnemyMenu menu = enemyMenuLayout();
        return menu == null ? -1 : menu.rowAt(mouseX, mouseY);
    }

    void orderAgainstEnemy(SquadClusters.Cluster enemy) {
        if (!MapCommandClientState.hasSelection()) return;
        MapCommandNetworkClient.attack(MapCommandClientState.selectionId(),
                MapCommandClientState.selectedUnitList(), enemy.selectionId());
    }

    void renderPendingOrderHint(GuiGraphics graphics, Font font, int barTop) {
        if (pendingObjectOrder == null) return;

        String hint = I18n.get("gui.recruitsrtscommand.object.point_at", pendingObjectOrder.label());
        int x = (view.screenWidth() - font.width(hint)) / 2;
        graphics.drawString(font, hint, x, barTop - 14, 0xFFE6C56A);
    }
}
