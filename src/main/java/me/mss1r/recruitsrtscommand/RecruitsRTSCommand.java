package me.mss1r.recruitsrtscommand;

import com.mojang.logging.LogUtils;
import me.mss1r.recruitsrtscommand.client.RecruitsRTSCommandClientEvents;
import me.mss1r.recruitsrtscommand.common.event.MapCommandServerEvents;
import me.mss1r.recruitsrtscommand.config.RecruitsRTSCommandServerConfig;
import me.mss1r.recruitsrtscommand.network.RecruitsRTSCommandNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RecruitsRTSCommand.MOD_ID)
public final class RecruitsRTSCommand {
    public static final String MOD_ID = "recruitsrtscommand";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RecruitsRTSCommand(FMLJavaModLoadingContext context) {
        RecruitsRTSCommandNetwork.init();
        context.registerConfig(ModConfig.Type.SERVER, RecruitsRTSCommandServerConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new MapCommandServerEvents());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                MinecraftForge.EVENT_BUS.register(new RecruitsRTSCommandClientEvents()));
    }
}
