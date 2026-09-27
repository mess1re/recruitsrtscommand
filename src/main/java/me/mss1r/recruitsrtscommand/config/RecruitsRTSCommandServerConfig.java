package me.mss1r.recruitsrtscommand.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RecruitsRTSCommandServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue COMMAND_RADIUS_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue ARRIVAL_DISTANCE_BLOCKS;
    public static final ForgeConfigSpec.IntValue REPATH_INTERVAL_TICKS;
    public static final ForgeConfigSpec.BooleanValue CONTINUE_MOVE_IN_COMBAT;
    public static final ForgeConfigSpec.BooleanValue SCOUTING_ENABLED;
    public static final ForgeConfigSpec.IntValue SIGHT_RANGE_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue STRANGER_HEALTH_VISIBLE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("rts");
        COMMAND_RADIUS_BLOCKS = builder
                .comment("Maximum horizontal distance from the commander to a selected squad anchor for map-issued RTS orders.")
                .defineInRange("commandRadiusBlocks", 512, 32, 4096);
        ARRIVAL_DISTANCE_BLOCKS = builder
                .comment("How close the squad center must be to a queued waypoint before the next waypoint is issued.")
                .defineInRange("arrivalDistanceBlocks", 5.0D, 1.0D, 32.0D);
        REPATH_INTERVAL_TICKS = builder
                .comment("How often active map move orders are reissued while a squad is marching.")
                .defineInRange("repathIntervalTicks", 80, 20, 400);
        CONTINUE_MOVE_IN_COMBAT = builder
                .comment("Allow map move orders to keep marching even when recruits are in combat.")
                .define("continueMoveInCombat", true);
        SCOUTING_ENABLED = builder
                .comment("Show other players' and patrols' recruits on the map only where your own men can see them.")
                .define("scoutingEnabled", true);
        STRANGER_HEALTH_VISIBLE = builder
                .comment("Send health information for other players' and patrols' recruits to the map.")
                .define("strangerHealthVisible", false);
        SIGHT_RANGE_BLOCKS = builder
                .comment("How far one of your men, or you, can make out a stranger on the map, in blocks.")
                .defineInRange("sightRangeBlocks", 96, 8, 1024);
        builder.pop();

        SPEC = builder.build();
    }

    private RecruitsRTSCommandServerConfig() {
    }
}

