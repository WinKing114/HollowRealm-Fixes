package hollowrealm.performance.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import hollowrealm.performance.FancyMenuPoseStackGuard;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, remap = false)
public abstract class FancyMenuGameRendererMixin {
    @Inject(
            method = "m_109089_(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("HEAD"),
            require = 1,
            remap = false)
    private void hrFixes$enterRenderLevel(float partialTicks, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        FancyMenuPoseStackGuard.enterRenderLevel();
    }

    @Inject(
            method = "m_109089_(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("RETURN"),
            require = 1,
            remap = false)
    private void hrFixes$exitRenderLevel(float partialTicks, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        FancyMenuPoseStackGuard.exitRenderLevel();
    }

    @Inject(
            method = "m_109093_(FJZ)V",
            at = @At("HEAD"),
            require = 1,
            remap = false)
    private void hrFixes$resetRenderLevelHead(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        FancyMenuPoseStackGuard.resetRenderLevel();
    }

    @Inject(
            method = "m_109093_(FJZ)V",
            at = @At("RETURN"),
            require = 1,
            remap = false)
    private void hrFixes$resetRenderLevelReturn(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        FancyMenuPoseStackGuard.resetRenderLevel();
    }
}
