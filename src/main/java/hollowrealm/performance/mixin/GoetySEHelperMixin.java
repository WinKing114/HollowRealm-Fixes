package hollowrealm.performance.mixin;

import com.Polarice3.Goety.common.capabilities.soulenergy.FocusCooldown;
import com.Polarice3.Goety.common.capabilities.soulenergy.ISoulEnergy;
import com.Polarice3.Goety.common.capabilities.soulenergy.SEImp;
import com.Polarice3.Goety.common.capabilities.soulenergy.SEProvider;
import com.Polarice3.Goety.utils.SEHelper;
import hollowrealm.performance.GoetyFocusCooldownPatch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = SEHelper.class, remap = false)
public abstract class GoetySEHelperMixin {
    @Inject(
            method = "getCapability(Lnet/minecraft/world/entity/player/Player;)Lcom/Polarice3/Goety/common/capabilities/soulenergy/ISoulEnergy;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void hrFixes$lazyGetCapability(Player player, CallbackInfoReturnable<ISoulEnergy> cir) {
        cir.setReturnValue(GoetyFocusCooldownPatch.resolveCapability(player, SEProvider.CAPABILITY, SEImp::new));
    }

    @Inject(
            method = "isOnCooldown(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void hrFixes$fastIsOnCooldown(Player player, ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || itemStack == null) {
            cir.setReturnValue(false);
            return;
        }
        FocusCooldown cooldown = SEHelper.getFocusCoolDown(player);
        if (cooldown == null
                || GoetyFocusCooldownPatch.hasNoActiveCooldowns(cooldown.cooldowns, cooldown.cooldownsSpecific)
                || itemStack.m_41619_()) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(cooldown.isOnCooldown(itemStack.m_41720_()) || cooldown.isOnSpecificCooldown(itemStack));
    }

    @Inject(
            method = "getCooldownPercent(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)F",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void hrFixes$fastGetCooldownPercent(Player player, ItemStack itemStack, CallbackInfoReturnable<Float> cir) {
        if (player == null || itemStack == null) {
            cir.setReturnValue(0.0F);
            return;
        }
        FocusCooldown cooldown = SEHelper.getFocusCoolDown(player);
        if (cooldown == null
                || GoetyFocusCooldownPatch.hasNoActiveCooldowns(cooldown.cooldowns, cooldown.cooldownsSpecific)
                || itemStack.m_41619_()) {
            cir.setReturnValue(0.0F);
            return;
        }
        float percent = cooldown.getCooldownPercent(itemStack.m_41720_());
        cir.setReturnValue(percent > 0.0F ? percent : cooldown.getSpecificCooldownPercent(itemStack));
    }
}
