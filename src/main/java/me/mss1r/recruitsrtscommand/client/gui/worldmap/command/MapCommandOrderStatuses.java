package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import me.mss1r.recruitsrtscommand.common.command.CommandOrderRoute;
import me.mss1r.recruitsrtscommand.common.command.CommandOrderStatus;
import net.minecraft.client.resources.language.I18n;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class MapCommandOrderStatuses {
    private MapCommandOrderStatuses() {
    }

    static Map<CommandOrderStatus, Integer> counts(List<CommandOrderRoute> routes, Predicate<UUID> selected) {
        Map<UUID, CommandOrderStatus> members = new HashMap<>();
        for (CommandOrderRoute route : routes) {
            for (UUID member : route.members()) {
                if (!selected.test(member)) continue;
                members.merge(member, route.status(), (previous, current) ->
                        previous.terminal() && !current.terminal() ? current : previous);
            }
        }
        Map<CommandOrderStatus, Integer> counts = new EnumMap<>(CommandOrderStatus.class);
        members.values().forEach(status -> counts.merge(status, 1, Integer::sum));
        return counts;
    }

    static String describe(List<CommandOrderRoute> routes, Predicate<UUID> selected) {
        return counts(routes, selected).entrySet().stream()
                .map(entry -> I18n.get(entry.getKey().translationKey()) + ": " + entry.getValue())
                .collect(Collectors.joining("   "));
    }
}
