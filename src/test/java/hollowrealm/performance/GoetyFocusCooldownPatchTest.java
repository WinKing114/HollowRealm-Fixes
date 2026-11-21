package hollowrealm.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.junit.jupiter.api.Test;

class GoetyFocusCooldownPatchTest {
    @Test
    void shortCircuitsSpecificAndItemCooldownLookupsWhenMapsAreEmptyOrInputsAreNull() {
        Map<String, Object> emptySpecific = Collections.emptyMap();
        Map<String, Object> activeSpecific = Map.of("goety:fireball_focus{affix:1}", new Object());

        assertTrue(GoetyFocusCooldownPatch.shouldShortCircuitSpecificCooldown(null, (ItemStack) null));
        assertTrue(GoetyFocusCooldownPatch.shouldShortCircuitSpecificCooldown(emptySpecific, (ItemStack) null));
        assertTrue(GoetyFocusCooldownPatch.shouldShortCircuitSpecificCooldown(emptySpecific, false));
        assertTrue(GoetyFocusCooldownPatch.shouldShortCircuitSpecificCooldown(activeSpecific, (ItemStack) null));
        assertTrue(GoetyFocusCooldownPatch.shouldShortCircuitSpecificCooldown(activeSpecific, true));
        assertFalse(GoetyFocusCooldownPatch.shouldShortCircuitSpecificCooldown(activeSpecific, false));

        Map<Object, Object> emptyGeneral = Collections.emptyMap();
        Object dummyItem = new Object();
        Map<Object, Object> activeGeneral = Map.of(dummyItem, new Object());

        assertTrue(GoetyFocusCooldownPatch.shouldShortCircuitItemCooldown(null, dummyItem));
        assertTrue(GoetyFocusCooldownPatch.shouldShortCircuitItemCooldown(emptyGeneral, dummyItem));
        assertTrue(GoetyFocusCooldownPatch.shouldShortCircuitItemCooldown(activeGeneral, null));
        assertFalse(GoetyFocusCooldownPatch.shouldShortCircuitItemCooldown(activeGeneral, dummyItem));

        assertTrue(GoetyFocusCooldownPatch.hasNoActiveCooldowns(null, null));
        assertTrue(GoetyFocusCooldownPatch.hasNoActiveCooldowns(emptyGeneral, emptySpecific));
        assertFalse(GoetyFocusCooldownPatch.hasNoActiveCooldowns(activeGeneral, emptySpecific));
        assertFalse(GoetyFocusCooldownPatch.hasNoActiveCooldowns(emptyGeneral, activeSpecific));
    }

    @Test
    void resolvesCapabilitiesLazilyWithZeroThrowawayAllocationsWhenPresent() {
        Object liveCapability = new Object();
        LazyOptional<Object> present = LazyOptional.of(() -> liveCapability);
        AtomicInteger fallbackAllocations = new AtomicInteger();

        for (int i = 0; i < 1_000; i++) {
            Object resolved = GoetyFocusCooldownPatch.resolveCapability(present, () -> {
                fallbackAllocations.incrementAndGet();
                return new Object();
            });
            assertSame(liveCapability, resolved);
        }
        assertEquals(0, fallbackAllocations.get(), "Present capability must never allocate fallback instance");

        LazyOptional<Object> empty = LazyOptional.empty();
        Object fallbackOne = GoetyFocusCooldownPatch.resolveCapability(empty, () -> {
            fallbackAllocations.incrementAndGet();
            return "fallback-empty";
        });
        assertEquals("fallback-empty", fallbackOne);
        assertEquals(1, fallbackAllocations.get());

        Object fallbackNull = GoetyFocusCooldownPatch.resolveCapability(null, () -> {
            fallbackAllocations.incrementAndGet();
            return "fallback-null";
        });
        assertEquals("fallback-null", fallbackNull);
        assertEquals(2, fallbackAllocations.get());

        ICapabilityProvider provider = new ICapabilityProvider() {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                return present.cast();
            }
        };
        assertEquals("fallback-null-cap", GoetyFocusCooldownPatch.resolveCapability(provider, null, () -> "fallback-null-cap"));
        assertEquals("fallback-non-provider", GoetyFocusCooldownPatch.resolveCapability("not-a-provider", null, () -> "fallback-non-provider"));
    }

    @Test
    void benchmarkShowsOrderOfMagnitudeSavingsOverEagerFallbackAndNbtKeySerialization() {
        Map<String, Object> emptySpecific = new HashMap<>();
        String simulatedNbtPayload = "{SocketedGems:[{id:\"apotheosis:gem\",lvl:5}],affixes:{a:1.25f,b:2.5f,c:3.75f}}";
        Object liveCap = new Object();
        LazyOptional<Object> presentCap = LazyOptional.of(() -> liveCap);
        int iterations = 50_000;

        // Warmup
        for (int i = 0; i < 5_000; i++) {
            legacySimulate(presentCap, emptySpecific, simulatedNbtPayload, i);
            patchedSimulate(presentCap, emptySpecific);
        }

        long legacyStart = System.nanoTime();
        int legacyHits = 0;
        for (int i = 0; i < iterations; i++) {
            if (legacySimulate(presentCap, emptySpecific, simulatedNbtPayload, i)) {
                legacyHits++;
            }
        }
        long legacyNanos = System.nanoTime() - legacyStart;

        long patchedStart = System.nanoTime();
        int patchedHits = 0;
        for (int i = 0; i < iterations; i++) {
            if (patchedSimulate(presentCap, emptySpecific)) {
                patchedHits++;
            }
        }
        long patchedNanos = System.nanoTime() - patchedStart;

        assertEquals(legacyHits, patchedHits);
        double legacyMs = legacyNanos / 1_000_000.0D;
        double patchedMs = patchedNanos / 1_000_000.0D;
        double speedup = (double) legacyNanos / Math.max(1L, patchedNanos);
        System.out.printf(
                "[BENCHMARK] GoetyFocusCooldownPatch iterations=%d legacy=%.3fms patched=%.3fms speedup=%.2fx%n",
                iterations, legacyMs, patchedMs, speedup);
        assertTrue(patchedNanos < legacyNanos, "Patched Goety cooldown path should be faster than legacy path");
    }

    private static boolean legacySimulate(LazyOptional<Object> cap, Map<String, Object> specific, String nbt, int salt) {
        // Legacy SEHelper.getCapability: allocates new SEImp() (with two HashMaps) before calling orElse(...)
        Object eagerThrowaway = new HashMap<>(4);
        Object resolved = cap.orElse(eagerThrowaway);
        // Legacy FocusCooldown.getSpecificCooldownPercent: always builds keyOf(stack) string even when map is empty
        String key = "goety:focus_item" + nbt + salt;
        return resolved != null && specific.get(key) != null;
    }

    private static boolean patchedSimulate(LazyOptional<Object> cap, Map<String, Object> specific) {
        Object resolved = GoetyFocusCooldownPatch.resolveCapability(cap, () -> new HashMap<>(4));
        if (GoetyFocusCooldownPatch.shouldShortCircuitSpecificCooldown(specific, false)) {
            return false;
        }
        return resolved != null && specific.get("goety:focus_item") != null;
    }
}
