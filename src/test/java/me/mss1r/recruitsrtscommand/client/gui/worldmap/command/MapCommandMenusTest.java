package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import me.mss1r.recruitsrtscommand.api.MapObjectAction;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapCommandMenusTest {
    @Test
    void groupsOptionsInTheOrderTheirFirstEntryAppears() {
        MapObjectAction attack = MapObjectAction.of("attack", Component.literal("Attack"), null);
        MapObjectAction standard = option("ammo", "standard", true);
        MapObjectAction stop = MapObjectAction.of("stop", Component.literal("Stop"), null);
        MapObjectAction explosive = option("ammo", "explosive", false);
        assertEquals(List.of(List.of(attack), List.of(standard, explosive), List.of(stop)),
                MapCommandMenus.menuRows(List.of(attack, standard, stop, explosive)));
    }

    @Test
    void cyclingSkipsDisabledOptionsAndWrapsAround() {
        MapObjectAction first = option("ammo", "standard", true);
        MapObjectAction blocked = MapObjectAction.blocked("blocked", Component.literal("Blocked"), null, null);
        MapObjectAction next = option("ammo", "explosive", false);
        assertSame(first, MapCommandMenus.shownOption(List.of(first, blocked, next)));
        assertSame(next, MapCommandMenus.nextOption(List.of(first, blocked, next)));
        assertNull(MapCommandMenus.nextOption(List.of(first, blocked)));
        MapObjectAction unselected = first.withSelected(false);
        assertSame(unselected, MapCommandMenus.nextOption(List.of(unselected, next.withSelected(true))));
    }

    private MapObjectAction option(String group, String id, boolean selected) {
        return MapObjectAction.option(group, id, Component.literal(id), null, selected);
    }
}
