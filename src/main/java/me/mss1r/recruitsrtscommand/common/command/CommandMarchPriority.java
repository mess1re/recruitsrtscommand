package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class CommandMarchPriority {
    private final Map<UUID, Commander> marching = new HashMap<>();

    void claim(UUID memberId, UUID ownerId, ResourceLocation dimension) {
        marching.put(memberId, new Commander(ownerId, dimension));
    }

    boolean contains(UUID memberId, UUID ownerId, ResourceLocation dimension) {
        Commander commander = marching.get(memberId);
        return commander != null && commander.matches(ownerId, dimension);
    }

    void remove(UUID ownerId, ResourceLocation dimension, Iterable<UUID> members) {
        for (UUID memberId : members) {
            if (contains(memberId, ownerId, dimension)) marching.remove(memberId);
        }
    }

    void clearOwner(UUID ownerId) {
        marching.values().removeIf(commander -> commander.ownerId.equals(ownerId));
    }

    void clear() {
        marching.clear();
    }

    private record Commander(UUID ownerId, ResourceLocation dimension) {
        boolean matches(UUID owner, ResourceLocation level) {
            return ownerId.equals(owner) && dimension.equals(level);
        }
    }
}
