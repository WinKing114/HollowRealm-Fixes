package hollowrealm.performance.mixin;

import com.Polarice3.Goety.common.capabilities.misc.IMisc;
import com.Polarice3.Goety.common.capabilities.misc.MiscImp;
import com.Polarice3.Goety.common.capabilities.misc.MiscProvider;
import com.Polarice3.Goety.utils.MiscCapHelper;
import hollowrealm.performance.GoetyFocusCooldownPatch;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = MiscCapHelper.class, remap = false)
public abstract class GoetyMiscCapHelperMixin {
    @Inject(
            method = "getCapability(Lnet/minecraft/world/entity/LivingEntity;)Lcom/Polarice3/Goety/common/capabilities/misc/IMisc;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void hrFixes$lazyGetCapability(LivingEntity livingEntity, CallbackInfoReturnable<IMisc> cir) {
        cir.setReturnValue(GoetyFocusCooldownPatch.resolveCapability(livingEntity, MiscProvider.CAPABILITY, MiscImp::new));
    }
}
