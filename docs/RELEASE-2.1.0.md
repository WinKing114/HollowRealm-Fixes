# HollowRealm Fixes 2.1.0

## What's New

- **Goety Focus Cooldown & Capability Allocation Guard (`GoetyFocusCooldownPatch`, `GoetyFocusCooldownMixin`, `GoetySEHelperMixin`, `GoetyMiscCapHelperMixin`, `GoetyLichdomHelperMixin`)**:
  - Short-circuits `FocusCooldown.getSpecificCooldownPercent(ItemStack)`, `isOnSpecificCooldown(ItemStack)`, and `removeSpecificCooldown(Player, Level, ItemStack)` at `@At("HEAD")` when `cooldownsSpecific.isEmpty()` (or `stack == null || stack.isEmpty()`), eliminating per-frame `FocusCooldown.keyOf(stack)` `CompoundTag` string serialization across hotbar and GUI item slots (~51 MB/s GC allocation + 3.6% Render Thread CPU savings).
  - Short-circuits `FocusCooldown.getCooldownPercent(Item)` and `isOnCooldown(Item)` when `cooldowns.isEmpty()`.
  - Replaces eager `.getCapability(CAP).orElse(new SEImp())` and `.orElse(new LichImp())` throwaway object allocations in `SEHelper.getCapability(Player)`, `MiscCapHelper.getCapability(LivingEntity)`, and `LichdomHelper.getCapability(Player)` with lazy `.orElseGet(...)` resolution, and collapses duplicate `getFocusCoolDown(player)` lookups in `SEHelper.isOnCooldown` / `getCooldownPercent`.
- **FancyMenu 3D World-Rendering PoseStack Guard (`FancyMenuPoseStackGuard`, `FancyMenuGameRendererMixin`, `FancyMenuPoseStackMixin`)**:
  - Tracks `GameRenderer.renderLevel` (`m_109089_`) entry/exit depth and `GameRenderer.render` (`m_109093_`) frame boundaries on the render thread and guards `MixinPoseStack.isRenderSystemModelViewStack_FancyMenu()` (`priority = 1500`) so FancyMenu's UI matrix tracking (`TranslationState`, `RotationState`, boxed `Float`, and 3 `ThreadLocal` stacks) is skipped during 3D world rendering while operating normally during all 2D Screen/GUI/HUD rendering (~20 MB/s allocation + 3.0% Render Thread CPU savings).
- **Retained 2.0.1 Fixes**:
  - `ChatLineHeightGuardMixin`, `KubeJsAttackCacheMixin`, `LodestoneTooltipMixin`, `ApotheosisJeiMixin`, `SocketingJeiLayoutMixin`, and `structurify_overlap_gate.js`.

## Verification Summary

- **14 / 14 JUnit 5 tests pass** via `./gradlew test build` and `python -X utf8 build.py` against real installed JAR bytecode from HollowRealm.
- See [VERIFICATION-2.1.0.md](VERIFICATION-2.1.0.md) for SHA-256 digests and audited bytecode details.
