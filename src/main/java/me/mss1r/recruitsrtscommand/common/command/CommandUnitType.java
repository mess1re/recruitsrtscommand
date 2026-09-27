package me.mss1r.recruitsrtscommand.common.command;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.IRangedRecruit;
import com.talhanation.recruits.entities.NomadEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

public enum CommandUnitType {
    ALL("All"),
    INFANTRY("Inf"),
    RANGED("Rng"),
    CAVALRY("Cav");

    private final String label;

    CommandUnitType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean matches(AbstractRecruitEntity recruit) {
        return switch (this) {
            case ALL -> true;
            case INFANTRY -> !isRanged(recruit) && !isCavalry(recruit);
            case RANGED -> isRanged(recruit);
            case CAVALRY -> isCavalry(recruit);
        };
    }

    public static CommandUnitType fromOrdinal(int ordinal) {
        CommandUnitType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) return ALL;
        return values[ordinal];
    }

    private static boolean isRanged(AbstractRecruitEntity recruit) {
        return recruit instanceof IRangedRecruit || recruit instanceof NomadEntity;
    }

    private static boolean isCavalry(AbstractRecruitEntity recruit) {
        Entity vehicle = recruit.getVehicle();
        return vehicle instanceof AbstractHorse;
    }
}

