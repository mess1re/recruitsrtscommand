package me.mss1r.recruitsrtscommand.common.command;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommandMarchPriorityTest {
    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    private static final ResourceLocation NETHER = ResourceLocation.withDefaultNamespace("the_nether");

    @Test
    void onlyCommandedMembersWithTheSameOwnerAndDimensionYieldCombat() {
        CommandMarchPriority priority = new CommandMarchPriority();
        UUID member = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        priority.claim(member, owner, OVERWORLD);
        assertTrue(priority.contains(member, owner, OVERWORLD));
        assertFalse(priority.contains(UUID.randomUUID(), owner, OVERWORLD));
        assertFalse(priority.contains(member, UUID.randomUUID(), OVERWORLD));
        assertFalse(priority.contains(member, null, OVERWORLD));
        assertFalse(priority.contains(member, owner, NETHER));
    }

    @Test
    void replacingAnOrderOrRebuildingTheActiveMarchesReleasesOldMembers() {
        CommandMarchPriority priority = new CommandMarchPriority();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        priority.claim(first, owner, OVERWORLD);
        priority.claim(second, owner, OVERWORLD);
        priority.remove(owner, NETHER, List.of(first));
        assertTrue(priority.contains(first, owner, OVERWORLD));
        priority.remove(owner, OVERWORLD, List.of(first));
        assertFalse(priority.contains(first, owner, OVERWORLD));
        assertTrue(priority.contains(second, owner, OVERWORLD));

        priority.clear();
        priority.claim(first, owner, OVERWORLD);
        assertTrue(priority.contains(first, owner, OVERWORLD));
        assertFalse(priority.contains(second, owner, OVERWORLD));
    }

    @Test
    void logoutReleasesOnlyThatPlayersMarchesAndServerStopReleasesAll() {
        CommandMarchPriority priority = new CommandMarchPriority();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();
        priority.claim(first, firstOwner, OVERWORLD);
        priority.claim(second, secondOwner, OVERWORLD);
        priority.clearOwner(firstOwner);
        assertFalse(priority.contains(first, firstOwner, OVERWORLD));
        assertTrue(priority.contains(second, secondOwner, OVERWORLD));
        priority.clear();
        assertFalse(priority.contains(second, secondOwner, OVERWORLD));
    }
}
