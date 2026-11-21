package hollowrealm.performance.mixin;

import com.Polarice3.Goety.common.capabilities.lichdom.ILichdom;
import com.Polarice3.Goety.common.capabilities.lichdom.LichImp;
import com.Polarice3.Goety.common.capabilities.lichdom.LichProvider;
import com.Polarice3.Goety.utils.LichdomHelper;
import hollowrealm.performance.GoetyFocusCooldownPatch;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = LichdomHelper.class, remap = false)
public abstract class GoetyLichdomHelperMixin {
    @Inject(
            method = "getCapability(Lnet/minecraft/world/entity/player/Player;)Lcom/Polarice3/Goety/common/capabilities/lichdom/ILichdom;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void hrFixes$lazyGetCapability(Player player, CallbackInfoReturnable<ILichdom> cir) {
        cir.setReturnValue(GoetyFocusCooldownPatch.resolveCapability(player, LichProvider.CAPABILITY, LichImp::new));
    }
}
