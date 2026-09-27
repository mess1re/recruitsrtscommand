package me.mss1r.recruitsrtscommand.mixin;

import com.talhanation.recruits.client.gui.worldmap.WorldMapScreen;
import me.mss1r.recruitsrtscommand.api.WorldMapOverlay;
import me.mss1r.recruitsrtscommand.api.WorldMapOverlayRegistry;
import me.mss1r.recruitsrtscommand.api.WorldMapView;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.talhanation.recruits.client.gui.worldmap.WorldMapScreen")
public abstract class WorldMapScreenMixin {
    @Inject(method = {"render", "m_88315_"}, at = @At("TAIL"), remap = false)
    private void recruitsrtscommand$renderOverlays(GuiGraphics graphics, int mouseX, int mouseY,
                                                   float partialTicks, CallbackInfo ci) {
        WorldMapView view = recruitsrtscommand$view();
        for (WorldMapOverlay overlay : WorldMapOverlayRegistry.overlays()) {
            overlay.renderMap(graphics, mouseX, mouseY, partialTicks, view);
        }
        for (WorldMapOverlay overlay : WorldMapOverlayRegistry.overlays()) {
            overlay.renderUi(graphics, mouseX, mouseY, partialTicks, view);
        }
    }

    @Inject(method = {"mouseClicked", "m_6375_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void recruitsrtscommand$mouseClicked(double mouseX, double mouseY, int button,
                                                 CallbackInfoReturnable<Boolean> cir) {
        WorldMapView view = recruitsrtscommand$view();
        for (WorldMapOverlay overlay : WorldMapOverlayRegistry.overlays()) {
            if (overlay.mouseClicked(mouseX, mouseY, button, view)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = {"mouseReleased", "m_6348_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void recruitsrtscommand$mouseReleased(double mouseX, double mouseY, int button,
                                                  CallbackInfoReturnable<Boolean> cir) {
        WorldMapView view = recruitsrtscommand$view();
        for (WorldMapOverlay overlay : WorldMapOverlayRegistry.overlays()) {
            if (overlay.mouseReleased(mouseX, mouseY, button, view)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = {"mouseDragged", "m_7979_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void recruitsrtscommand$mouseDragged(double mouseX, double mouseY, int button,
                                                 double dragX, double dragY,
                                                 CallbackInfoReturnable<Boolean> cir) {
        WorldMapView view = recruitsrtscommand$view();
        for (WorldMapOverlay overlay : WorldMapOverlayRegistry.overlays()) {
            if (overlay.mouseDragged(mouseX, mouseY, button, dragX, dragY, view)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = {"mouseScrolled", "m_6050_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void recruitsrtscommand$mouseScrolled(double mouseX, double mouseY, double scrollY,
                                                  CallbackInfoReturnable<Boolean> cir) {
        WorldMapView view = recruitsrtscommand$view();
        for (WorldMapOverlay overlay : WorldMapOverlayRegistry.overlays()) {
            if (overlay.mouseScrolled(mouseX, mouseY, scrollY, view)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = {"mouseMoved", "m_94757_"}, at = @At("TAIL"), remap = false)
    private void recruitsrtscommand$mouseMoved(double mouseX, double mouseY, CallbackInfo ci) {
        WorldMapView view = recruitsrtscommand$view();
        for (WorldMapOverlay overlay : WorldMapOverlayRegistry.overlays()) {
            overlay.mouseMoved(mouseX, mouseY, view);
        }
    }

    @Inject(method = {"keyPressed", "m_7933_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void recruitsrtscommand$keyPressed(int keyCode, int scanCode, int modifiers,
                                               CallbackInfoReturnable<Boolean> cir) {
        WorldMapView view = recruitsrtscommand$view();
        for (WorldMapOverlay overlay : WorldMapOverlayRegistry.overlays()) {
            if (overlay.keyPressed(keyCode, scanCode, modifiers, view)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    private WorldMapView recruitsrtscommand$view() {
        return new WorldMapView((WorldMapScreen) (Object) this);
    }
}
