package me.mss1r.recruitsrtscommand.client.command;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public final class ClientGroups {
    private ClientGroups() {
    }

    @Nullable
    public static RecruitsGroup byId(UUID groupId) {
        if (groupId == null || ClientManager.groups == null) return null;
        for (RecruitsGroup group : ClientManager.groups) {
            if (group != null && Objects.equals(group.getUUID(), groupId)) return group;
        }
        return null;
    }

    @Nullable
    public static String nameOf(UUID groupId) {
        RecruitsGroup group = byId(groupId);
        if (group == null) return null;
        String name = group.getName();
        return name == null || name.isBlank() ? null : name;
    }

    @Nullable
    public static ResourceLocation imageOf(UUID groupId) {
        RecruitsGroup group = byId(groupId);
        if (group == null || RecruitsGroup.IMAGES == null || RecruitsGroup.IMAGES.isEmpty()) return null;
        int index = group.getImage();
        if (index < 0 || index >= RecruitsGroup.IMAGES.size()) return null;
        return RecruitsGroup.IMAGES.get(index);
    }
}
