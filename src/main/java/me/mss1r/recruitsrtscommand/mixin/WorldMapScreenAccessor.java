package me.mss1r.recruitsrtscommand.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.talhanation.recruits.client.gui.worldmap.WorldMapScreen")
public interface WorldMapScreenAccessor {
    @Accessor(value = "offsetX", remap = false)
    double recruitsrtscommand$offsetX();

    @Accessor(value = "offsetZ", remap = false)
    double recruitsrtscommand$offsetZ();

    @Accessor(value = "camera", remap = false)
    com.talhanation.recruits.client.gui.worldmap.WorldMapCamera recruitsrtscommand$camera();
}
