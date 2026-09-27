package me.mss1r.recruitsrtscommand.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Describes either a texture-backed or item-backed map icon. */
public record MapIcon(
        @Nullable ResourceLocation texture,
        @Nullable ResourceLocation item,
        float scale,
        boolean smooth
) {
    public static MapIcon sprite(ResourceLocation texture) {
        return new MapIcon(texture, null, 1.0F, false);
    }

    public static MapIcon drawing(ResourceLocation texture, float scale) {
        return new MapIcon(texture, null, scale, true);
    }

    public static MapIcon item(ResourceLocation item, float scale) {
        return new MapIcon(null, item, scale, false);
    }

    public boolean isItem() {
        return item != null;
    }
}
