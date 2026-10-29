package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.UUID;

import static me.mss1r.recruitsrtscommand.client.gui.worldmap.command.TacticalSymbolPalette.*;

final class MapCommandRelations {
    int relationColor(UUID ownerId) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID localPlayerId = minecraft.player == null ? null : minecraft.player.getUUID();
        if (localPlayerId != null && localPlayerId.equals(ownerId)) return OWN_COLOR;

        RecruitsFaction ownFaction = ClientManager.ownFaction;
        RecruitsFaction targetFaction = findFactionForOwner(ownerId);
        if (ownFaction == null || targetFaction == null) return NEUTRAL_COLOR;
        if (ownFaction.equalsFaction(targetFaction)) return ALLY_COLOR;

        RecruitsDiplomacyManager.DiplomacyStatus relation = ClientManager.getRelation(
                ownFaction.getStringID(), targetFaction.getStringID());
        if (relation == RecruitsDiplomacyManager.DiplomacyStatus.ENEMY) return ENEMY_COLOR;
        if (relation == RecruitsDiplomacyManager.DiplomacyStatus.ALLY) return ALLY_COLOR;
        return NEUTRAL_COLOR;
    }

    boolean mayAttack(UUID ownerId) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID localPlayerId = minecraft.player == null ? null : minecraft.player.getUUID();
        if (localPlayerId != null && localPlayerId.equals(ownerId)) return false;

        RecruitsFaction ownFaction = ClientManager.ownFaction;
        RecruitsFaction targetFaction = findFactionForOwner(ownerId);
        if (ownFaction == null || targetFaction == null) return true;
        if (ownFaction.equalsFaction(targetFaction)) return false;
        if (ClientManager.getRelation(ownFaction.getStringID(), targetFaction.getStringID())
                == RecruitsDiplomacyManager.DiplomacyStatus.ALLY) {
            return false;
        }
        return !ClientManager.hasTreaty(ownFaction.getStringID(), targetFaction.getStringID());
    }

    RecruitsFaction findFactionForOwner(UUID ownerId) {
        if (ClientManager.factions == null) return null;
        for (RecruitsFaction faction : ClientManager.factions) {
            for (RecruitsPlayerInfo member : faction.getMembers()) {
                if (Objects.equals(ownerId, member.getUUID())) return faction;
            }
        }
        return null;
    }
}
