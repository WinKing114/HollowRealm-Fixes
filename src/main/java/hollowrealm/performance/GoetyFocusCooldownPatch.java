package hollowrealm.performance;

import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

public final class GoetyFocusCooldownPatch {
    private GoetyFocusCooldownPatch() {
    }

    public static boolean shouldShortCircuitSpecificCooldown(Map<?, ?> cooldownsSpecific, ItemStack stack) {
        return cooldownsSpecific == null || cooldownsSpecific.isEmpty() || stack == null || stack.m_41619_();
    }

    public static boolean shouldShortCircuitSpecificCooldown(Map<?, ?> cooldownsSpecific, boolean stackMissingOrEmpty) {
        return cooldownsSpecific == null || cooldownsSpecific.isEmpty() || stackMissingOrEmpty;
    }

    public static boolean shouldShortCircuitItemCooldown(Map<?, ?> cooldowns, Object item) {
        return cooldowns == null || cooldowns.isEmpty() || item == null;
    }

    public static boolean hasNoActiveCooldowns(Map<?, ?> cooldowns, Map<?, ?> cooldownsSpecific) {
        return (cooldowns == null || cooldowns.isEmpty())
                && (cooldownsSpecific == null || cooldownsSpecific.isEmpty());
    }

    public static <T> T resolveCapability( Object provider, Capability<T> capability, NonNullSupplier<T> fallback) {
        if (provider instanceof ICapabilityProvider capProvider && capability != null) {
            return resolveCapability(capProvider.getCapability(capability), fallback);
        }
        return fallback.get();
    }

    public static <T> T resolveCapability(LazyOptional<T> optional, NonNullSupplier<T> fallback) {
        if (optional == null) {
            return fallback.get();
        }
        return optional.orElseGet(fallback);
    }
}
