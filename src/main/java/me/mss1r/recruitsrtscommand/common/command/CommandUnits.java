package me.mss1r.recruitsrtscommand.common.command;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.IStrategicFire;
import com.talhanation.recruits.entities.IRangedRecruit;
import com.talhanation.recruits.entities.NomadEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class CommandUnits {
    private CommandUnits() {
    }

    static List<AbstractRecruitEntity> resolveGroupMembers(ServerLevel level, RecruitsGroup group) {
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        for (UUID memberId : group.members) {
            Entity entity = level.getEntity(memberId);
            if (entity instanceof AbstractRecruitEntity recruit && recruit.isAlive()) {
                recruits.add(recruit);
            }
        }
        return recruits;
    }

    static List<AbstractRecruitEntity> resolveMembers(ServerLevel level, Iterable<UUID> memberIds, UUID ownerId) {
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        for (UUID memberId : memberIds) {
            Entity entity = level.getEntity(memberId);
            if (entity instanceof AbstractRecruitEntity recruit && recruit.isEffectedByCommand(ownerId)) {
                recruits.add(recruit);
            }
        }
        return recruits;
    }

    static List<LivingEntity> resolveLivingMembers(ServerLevel level, Iterable<UUID> memberIds) {
        List<LivingEntity> entities = new ArrayList<>();
        for (UUID memberId : memberIds) {
            Entity entity = level.getEntity(memberId);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                entities.add(living);
            }
        }
        return entities;
    }

    static BlockPos anchorOf(List<AbstractRecruitEntity> recruits) {
        Vec3 center = centerOf(recruits);
        return BlockPos.containing(center);
    }

    static Vec3 centerOf(List<AbstractRecruitEntity> recruits) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (AbstractRecruitEntity recruit : recruits) {
            Vec3 position = recruit.position();
            x += position.x;
            y += position.y;
            z += position.z;
        }
        double count = Math.max(1, recruits.size());
        return new Vec3(x / count, y / count, z / count);
    }

    static boolean isInCombat(AbstractRecruitEntity recruit) {
        return recruit.getTarget() != null || recruit.getLastHurtByMob() != null;
    }

    static boolean isRangedLike(AbstractRecruitEntity recruit) {
        return recruit instanceof IRangedRecruit || recruit instanceof NomadEntity;
    }

    static double horizontalDistSqr(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return (double) (dx * dx + dz * dz);
    }

    static void standDown(List<AbstractRecruitEntity> recruits) {
        for (AbstractRecruitEntity recruit : recruits) {
            if (recruit instanceof IStrategicFire strategicFire) {
                strategicFire.setShouldStrategicFire(false);
            }
        }
    }

    static float facingYaw(Vec3 from, Vec3 to, float fallback) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        if (dx * dx + dz * dz < 4.0D) return fallback;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    static ServerLevel serverLevel(MinecraftServer server, ResourceLocation dimension) {
        if (server == null || dimension == null) return null;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimension);
        return server.getLevel(key);
    }
}
