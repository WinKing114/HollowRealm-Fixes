package hollowrealm.performance.mixin;

import com.Polarice3.Goety.common.capabilities.soulenergy.FocusCooldown;
import hollowrealm.performance.GoetyFocusCooldownPatch;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = FocusCooldown.class, remap = false)
public abstract class GoetyFocusCooldownMixin {
    @Shadow(remap = false)
    public Map<Item, ?> cooldowns;

    @Shadow(remap = false)
    public Map<String, ?> cooldownsSpecific;

    @Inject(
            method = "getSpecificCooldownPercent(Lnet/minecraft/world/item/ItemStack;)F",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void hrFixes$skipEmptySpecificCooldownPercent(ItemStack stack, CallbackInfoReturnable<Float> cir) {
        if (GoetyFocusCooldownPatch.shouldShortCircuitSpecificCooldown(this.cooldownsSpecific, stack)) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(
            method = "isOnSpecificCooldown(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void hrFixes$skipEmptyIsOnSpecificCooldown(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (GoetyFocusCooldownPatch.shouldShortCircuitSpecificCooldown(this.cooldownsSpecific, stack)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "removeSpecificCooldown(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void hrFixes$skipEmptyRemoveSpecificCooldown(Player player, Level level, ItemStack stack, CallbackInfo ci) {
        if (GoetyFocusCooldownPatch.shouldShortCircuitSpecificCooldown(this.cooldownsSpecific, stack)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "getCooldownPercent(Lnet/minecraft/world/item/Item;)F",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void hrFixes$skipEmptyCooldownPercent(Item item, CallbackInfoReturnable<Float> cir) {
        if (GoetyFocusCooldownPatch.shouldShortCircuitItemCooldown(this.cooldowns, item)) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(
            method = "isOnCooldown(Lnet/minecraft/world/item/Item;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void hrFixes$skipEmptyIsOnCooldown(Item item, CallbackInfoReturnable<Boolean> cir) {
        if (GoetyFocusCooldownPatch.shouldShortCircuitItemCooldown(this.cooldowns, item)) {
            cir.setReturnValue(false);
        }
    }
}
