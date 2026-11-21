package hollowrealm.performance.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = PoseStack.class, priority = 1500, remap = false)
public abstract class FancyMenuPoseStackMixin {
}
