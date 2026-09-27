package me.mss1r.recruitsrtscommand.api;

import com.talhanation.recruits.client.gui.worldmap.WorldMapScreen;
import me.mss1r.recruitsrtscommand.mixin.WorldMapScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Coordinate conversion and camera access for a tactical-map overlay.
 */
public final class WorldMapView {
    private static final int FALLBACK_SURFACE_Y = 64;

    private final WorldMapScreen screen;

    public WorldMapView(WorldMapScreen screen) {
        this.screen = screen;
    }

    public WorldMapScreen screen() {
        return screen;
    }

    public Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    public Player player() {
        return screen.getPlayer();
    }

    public int screenWidth() {
        return screen.width;
    }

    public int screenHeight() {
        return screen.height;
    }

    public double offsetX() {
        return ((WorldMapScreenAccessor) screen).recruitsrtscommand$offsetX();
    }

    public double offsetZ() {
        return ((WorldMapScreenAccessor) screen).recruitsrtscommand$offsetZ();
    }

    /** Moves the map camera by a screen-space delta. */
    public void pan(double deltaX, double deltaY) {
        ((WorldMapScreenAccessor) screen).recruitsrtscommand$camera().panByScreenDelta(deltaX, deltaY);
    }

    public double scale() {
        return screen.getScale();
    }

    public double worldToScreenX(double worldX) {
        return worldX * scale() + offsetX();
    }

    public double worldToScreenY(double worldZ) {
        return worldZ * scale() + offsetZ();
    }

    public double screenToWorldX(double screenX) {
        return (screenX - offsetX()) / scale();
    }

    public double screenToWorldZ(double screenY) {
        return (screenY - offsetZ()) / scale();
    }

    /** Converts a screen point to a block position at the loaded surface height. */
    public BlockPos screenToWorld(double screenX, double screenY) {
        int worldX = (int) Math.floor(screenToWorldX(screenX));
        int worldZ = (int) Math.floor(screenToWorldZ(screenY));
        return new BlockPos(worldX, resolveSurfaceY(worldX, worldZ), worldZ);
    }

    public int resolveSurfaceY(int worldX, int worldZ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return FALLBACK_SURFACE_Y;
        }

        ChunkPos chunk = new ChunkPos(worldX >> 4, worldZ >> 4);
        if (minecraft.level.getChunkSource().getChunk(chunk.x, chunk.z, false) == null) {
            return FALLBACK_SURFACE_Y;
        }
        int y = minecraft.level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
        return Math.max(y, minecraft.level.getMinBuildHeight());
    }
}
