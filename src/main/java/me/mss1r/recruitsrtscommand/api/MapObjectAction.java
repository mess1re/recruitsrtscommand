package me.mss1r.recruitsrtscommand.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * An action displayed for a selected map object.
 * Actions may be disabled, require a target point, or belong to an option group.
 */
public record MapObjectAction(
        String id,
        Component label,
        @Nullable Component hint,
        boolean enabled,
        @Nullable Component reason,
        boolean needsPoint,
        @Nullable String group,
        boolean selected
) {
    public static MapObjectAction of(String id, Component label, @Nullable Component hint) {
        return new MapObjectAction(id, label, hint, true, null, false, null, false);
    }

    public static MapObjectAction blocked(String id, Component label, @Nullable Component hint,
                                          Component reason) {
        return new MapObjectAction(id, label, hint, false, reason, false, null, false);
    }

    public static MapObjectAction at(String id, Component label, @Nullable Component hint) {
        return new MapObjectAction(id, label, hint, true, null, true, null, false);
    }

    public static MapObjectAction blockedAt(String id, Component label, @Nullable Component hint,
                                            Component reason) {
        return new MapObjectAction(id, label, hint, false, reason, true, null, false);
    }

    public static MapObjectAction option(String group, String id, Component label,
                                         @Nullable Component hint, boolean selected) {
        return new MapObjectAction(id, label, hint, true, null, false, group, selected);
    }

    public static MapObjectAction blockedOption(String group, String id, Component label,
                                                @Nullable Component hint, boolean selected,
                                                Component reason) {
        return new MapObjectAction(id, label, hint, false, reason, false, group, selected);
    }

    public boolean isOption() {
        return group != null;
    }

    public MapObjectAction withSelected(boolean replacement) {
        return new MapObjectAction(id, label, hint, enabled, reason, needsPoint, group, replacement);
    }
}
