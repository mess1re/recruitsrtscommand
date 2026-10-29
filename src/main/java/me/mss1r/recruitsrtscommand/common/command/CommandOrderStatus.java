package me.mss1r.recruitsrtscommand.common.command;

import java.util.Locale;

public enum CommandOrderStatus {
    MOVING(false),
    REPATHING(false),
    PURSUING(false),
    ARRIVED(true),
    BLOCKED(true),
    TARGET_LOST(true);

    private final boolean terminal;

    CommandOrderStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean terminal() {
        return terminal;
    }

    public String translationKey() {
        return "gui.recruitsrtscommand.order." + name().toLowerCase(Locale.ROOT);
    }
}
