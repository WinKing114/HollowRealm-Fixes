package hollowrealm.performance;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public final class HRFixesMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOG = LogManager.getLogger("HR Fixes");

    private static final List<String> LODESTONE = List.of("1.20.1-1.6.4.1");
    private static final List<String> ATTRIBUTESLIB = List.of("1.3.7");
    private static final List<String> APOTHEOSIS = List.of("7.4.8");
    private static final List<String> JEI = List.of("15.56.0.205");
    private static final List<String> COO_OVERLAP = List.of("4.6", "5.5", "6.5");
    private static final List<String> STRUCTURIFY = List.of("2.0.42", "2.0.43");
    private static final List<String> L2HOSTILITY = List.of("2.5.19");
    private static final List<String> L2DAMAGETRACKER = List.of("0.4.4");
    private static final List<String> GOETY = List.of("2.5.60.5");
    private static final List<String> FANCYMENU = List.of("3.9.14");

    private static final String L2_KUBEJS_PLUGIN = "dev.xkmc.l2hostility.compat.kubejs.LHKJSPlugin";
    private static final String L2_KUBEJS_PLUGIN_PATH = "dev/xkmc/l2hostility/compat/kubejs/LHKJSPlugin.class";
    private static final String L2_KUBEJS_PLUGIN_SHA256 = "b32703b98f588d6fb49adc976b7d599f32f48c00a908e30531f5e4b65547161f";
    private static final String L2_ATTACK_EVENT_HANDLER = "dev.xkmc.l2damagetracker.contents.attack.AttackEventHandler";
    private static final String L2_ATTACK_EVENT_HANDLER_PATH = "dev/xkmc/l2damagetracker/contents/attack/AttackEventHandler.class";
    private static final String L2_ATTACK_EVENT_HANDLER_SHA256 = "f662ae8e993946d354f4a709d6526818fe6e82bb7d21fe776415c61c14f6a090";
    private static final String L2_REMOVE_IF = "(Ljava/util/function/Predicate;)Z";

    private static final String CHAT_COMPONENT = "net.minecraft.client.gui.components.ChatComponent";
    private static final String CHAT_RENDER = "(Lnet/minecraft/client/gui/GuiGraphics;III)V";

    private static final String TOOLTIP_TARGET = "dev.shadowsoffire.attributeslib.client.AttributesLibClient";
    private static final String TOOLTIP_METHOD = "lambda$applyTextFor$6";
    private static final String TOOLTIP_DESCRIPTOR = "(Ljava/util/Map;Lnet/minecraft/world/entity/ai/attributes/Attribute;Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V";
    private static final String TOOLTIP_CLASS_SHA256 = "a3aebc9f96f6d549a017dd038671bd0cc6abfb93e861c16bfe4e8ed6be8765eb";
    private static final String LODESTONE_MIXIN_CLASS_SHA256 = "e6fa7b392a31ab6e772f0b2bfde34bb7ca32d95c9884f8065ccf121940e544c2";
    private static final String ATTRIBUTESLIB_CLIENT = "dev/shadowsoffire/attributeslib/client/AttributesLibClient.class";
    private static final String LODESTONE_MIXIN_CLASS = "team/lodestar/lodestone/mixin/common/attributeslib/AttributeLibClientMixin.class";

    private static final String SMITHING_CATEGORY = "mezz.jei.library.plugins.vanilla.anvil.SmithingRecipeCategory";
    private static final String SMITHING_SET_RECIPE = "(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lnet/minecraft/world/item/crafting/SmithingRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V";
    private static final String SLOT_BUILDER = "Lmezz/jei/api/gui/builder/IRecipeSlotBuilder;";
    private static final String ADVENTURE_JEI_PLUGIN = "dev.shadowsoffire.apotheosis.adventure.compat.AdventureJEIPlugin";
    private static final String REGISTER_VANILLA_EXTENSIONS = "(Lmezz/jei/api/registration/IVanillaCategoryExtensionRegistration;)V";

    private static final String GOETY_FOCUS_COOLDOWN = "com.Polarice3.Goety.common.capabilities.soulenergy.FocusCooldown";
    private static final String GOETY_FOCUS_COOLDOWN_PATH = "com/Polarice3/Goety/common/capabilities/soulenergy/FocusCooldown.class";
    private static final String GOETY_FOCUS_COOLDOWN_SHA256 = "586c48cedf265e652db8bed56cc27a2e7a7f61dcf3266985bdc76be19e107aaf";

    private static final String GOETY_SE_HELPER = "com.Polarice3.Goety.utils.SEHelper";
    private static final String GOETY_SE_HELPER_PATH = "com/Polarice3/Goety/utils/SEHelper.class";
    private static final String GOETY_SE_HELPER_SHA256 = "c575d7a0d43c97f985571b8af067108748a2f1062b040340954a5543ef1ef3b1";

    private static final String GOETY_MISC_HELPER = "com.Polarice3.Goety.utils.MiscCapHelper";
    private static final String GOETY_MISC_HELPER_PATH = "com/Polarice3/Goety/utils/MiscCapHelper.class";
    private static final String GOETY_MISC_HELPER_SHA256 = "593b43afb9d4bdd95bdeb3aed1878b6127596a1adaf7275952e872c621f5d3b0";

    private static final String GOETY_LICHDOM_HELPER = "com.Polarice3.Goety.utils.LichdomHelper";
    private static final String GOETY_LICHDOM_HELPER_PATH = "com/Polarice3/Goety/utils/LichdomHelper.class";
    private static final String GOETY_LICHDOM_HELPER_SHA256 = "9d0da14362a49b0ff270e231796442e018f82ceb21cb75a6cf6b2d25f3020d45";

    private static final String GAME_RENDERER = "net.minecraft.client.renderer.GameRenderer";
    private static final String POSE_STACK = "com.mojang.blaze3d.vertex.PoseStack";
    private static final String FANCYMENU_POSE_STACK_MIXIN_PATH = "de/keksuccino/fancymenu/mixin/mixins/common/client/MixinPoseStack.class";
    private static final String FANCYMENU_POSE_STACK_MIXIN_SHA256 = "d37c2213a9a87d48217fbb4e420659e289bc7820fe1de015b033ad6b2aa03f78";
    private static final String FANCYMENU_MODEL_VIEW_CHECK = "isRenderSystemModelViewStack_FancyMenu";
    private static final String FANCYMENU_GUARD_OWNER = "hollowrealm/performance/FancyMenuPoseStackGuard";
    private static final String FANCYMENU_GUARD_METHOD = "shouldSkipPoseStackTracking";

    private static final ResourceLookup DEFAULT_LOOKUP = path -> {
        try (InputStream stream = MixinService.getService().getResourceAsStream(path)) {
            return stream == null ? null : stream.readAllBytes();
        }
    };

    private static volatile Map<String, String> installedVersions;
    private static volatile ResourceLookup resourceLookup = DEFAULT_LOOKUP;
    private static final Set<String> LOGGED_OVERLAP_GATE = ConcurrentHashMap.newKeySet();

    private final Map<String, Decision> decisions = new ConcurrentHashMap<>();

    @FunctionalInterface
    interface ResourceLookup {
        byte[] load(String path) throws IOException;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    private static Map<String, String> versions() {
        Map<String, String> cached = installedVersions;
        if (cached != null) {
            return cached;
        }
        LoadingModList modList = LoadingModList.get();
        if (modList == null) {
            return Map.of();
        }
        Map<String, String> collected = new LinkedHashMap<>();
        for (ModInfo info : modList.getMods()) {
            collected.put(info.getModId(), info.getVersion().toString());
        }
        if (!collected.isEmpty()) {
            installedVersions = Map.copyOf(collected);
        }
        return collected;
    }

    private static Gate gate(String modId, List<String> allowedVersions) {
        String version = versions().get(modId);
        if (version == null) {
            return new Gate(modId + " is not installed", false);
        }
        if (allowedVersions.contains(version)) {
            return null;
        }
        return new Gate(modId + " " + version + " is not audited (expected " + String.join(" / ", allowedVersions) + ")", true);
    }

    private static Gate firstGate(Gate... gates) {
        for (Gate gate : gates) {
            if (gate != null) {
                return gate;
            }
        }
        return null;
    }

    private static Decision skip(Gate gate) {
        return new Decision(false, gate.problem(), gate.relevant());
    }

    private static Decision apply(String reason) {
        return new Decision(true, reason, false);
    }

    public static boolean shouldDisableStructurifyOverlap() {
        Gate cooGate = gate("collections_of_optimizations", COO_OVERLAP);
        Gate structurifyGate = gate("structurify", STRUCTURIFY);
        boolean disable = cooGate == null && structurifyGate == null;
        if (LOGGED_OVERLAP_GATE.add(String.valueOf(disable))) {
            if (disable) {
                LOG.info("CoO/Structurify overlap gate: disabling CoO's Structurify overlap mixin for {} x {}",
                        versions().get("collections_of_optimizations"), versions().get("structurify"));
            } else {
                LOG.info("CoO/Structurify overlap gate: keeping CoO mixins ({}; {})",
                        cooGate == null ? "CoO audited" : cooGate.problem(),
                        structurifyGate == null ? "Structurify audited" : structurifyGate.problem());
            }
        }
        return disable;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String key = targetClassName + "|" + mixinClassName;
        Decision decision = this.decisions.get(key);
        if (decision == null) {
            Decision computed = decide(targetClassName, mixinClassName);
            Decision existing = this.decisions.putIfAbsent(key, computed);
            if (existing == null) {
                decision = computed;
                logDecision(targetClassName, mixinClassName, decision);
            } else {
                decision = existing;
            }
        }
        return decision.enabled();
    }

    private static void logDecision(String targetClassName, String mixinClassName, Decision decision) {
        String shortMixin = simpleName(mixinClassName);
        if (decision.enabled()) {
            LOG.info("APPLY {} -> {} ({})", shortMixin, targetClassName, decision.reason());
        } else if (decision.relevant()) {
            LOG.warn("SKIP {} -> {}: {}", shortMixin, targetClassName, decision.reason());
        } else {
            LOG.info("SKIP {} -> {}: {}", shortMixin, targetClassName, decision.reason());
        }
    }

    static Decision evaluateDecision(String targetClassName, String mixinClassName) {
        return decide(targetClassName, mixinClassName);
    }

    private static Decision decide(String targetClassName, String mixinClassName) {
        try {
            return gate(targetClassName, mixinClassName);
        } catch (RuntimeException ex) {
            LOG.error("Compatibility gate failed for {} -> {}; skipping this patch", mixinClassName, targetClassName, ex);
            return new Decision(false, "gate error: " + ex, true);
        }
    }

    private static Decision gate(String targetClassName, String mixinClassName) {
        return switch (simpleName(mixinClassName)) {
            case "LodestoneTooltipMixin" -> lodestone(targetClassName);
            case "ApotheosisJeiMixin" -> apotheosisJei(targetClassName);
            case "SocketingJeiLayoutMixin" -> socketingLayout(targetClassName);
            case "KubeJsAttackCacheMixin" -> l2KubeJsCache(targetClassName);
            case "ChatLineHeightGuardMixin" -> chatLineHeight(targetClassName);
            case "GoetyFocusCooldownMixin" -> goetyFocusCooldown(targetClassName);
            case "GoetySEHelperMixin" -> goetySeHelper(targetClassName);
            case "GoetyMiscCapHelperMixin" -> goetyMiscCapHelper(targetClassName);
            case "GoetyLichdomHelperMixin" -> goetyLichdomHelper(targetClassName);
            case "FancyMenuGameRendererMixin" -> fancyMenuGameRenderer(targetClassName);
            case "FancyMenuPoseStackMixin" -> fancyMenuPoseStack(targetClassName);
            default -> new Decision(false, "no audited gate for this mixin", true);
        };
    }

    private static Decision lodestone(String targetClassName) {
        if (!TOOLTIP_TARGET.equals(targetClassName)) {
            return new Decision(false, "unexpected target class", true);
        }
        Gate modGate = firstGate(gate("lodestone", LODESTONE), gate("attributeslib", ATTRIBUTESLIB));
        if (modGate != null) {
            return skip(modGate);
        }
        if (!fingerprint(ATTRIBUTESLIB_CLIENT, TOOLTIP_CLASS_SHA256)) {
            return new Decision(false, "AttributesLibClient.class fingerprint changed; re-audit before enabling", true);
        }
        if (!fingerprint(LODESTONE_MIXIN_CLASS, LODESTONE_MIXIN_CLASS_SHA256)) {
            return new Decision(false, "Lodestone AttributeLibClientMixin.class fingerprint changed; re-audit before enabling", true);
        }
        return apply("audited Lodestone/AttributesLib pair and fingerprints");
    }

    private static Decision apotheosisJei(String targetClassName) {
        Gate modGate = firstGate(gate("apotheosis", APOTHEOSIS), gate("jei", JEI));
        if (modGate != null) {
            return skip(modGate);
        }
        if (!ADVENTURE_JEI_PLUGIN.equals(targetClassName)) {
            return new Decision(false, "unexpected target class", true);
        }
        ClassNode node = readClass(targetClassName, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
        if (node == null) {
            return new Decision(false, "AdventureJEIPlugin class bytes are missing or unreadable", true);
        }
        if (node.methods.stream().anyMatch(m -> "registerVanillaCategoryExtensions".equals(m.name) && REGISTER_VANILLA_EXTENSIONS.equals(m.desc))) {
            return new Decision(false, "AdventureJEIPlugin already registers vanilla category extensions", true);
        }
        return apply("audited Apotheosis JEI plugin without socketing extension");
    }

    private static Decision socketingLayout(String targetClassName) {
        Gate modGate = firstGate(gate("apotheosis", APOTHEOSIS), gate("jei", JEI));
        if (modGate != null) {
            return skip(modGate);
        }
        if (!SMITHING_CATEGORY.equals(targetClassName)) {
            return new Decision(false, "unexpected target class", true);
        }
        if (!hasMethod(targetClassName, "setRecipe", SMITHING_SET_RECIPE)) {
            return new Decision(false, "SmithingRecipeCategory.setRecipe signature changed", true);
        }
        if (!hasSmithingLocals(targetClassName)) {
            return new Decision(false, "SmithingRecipeCategory.setRecipe local layout changed", true);
        }
        return apply("audited JEI smithing layout and local capture");
    }

    private static Decision l2KubeJsCache(String targetClassName) {
        Gate modGate = firstGate(gate("l2hostility", L2HOSTILITY), gate("l2damagetracker", L2DAMAGETRACKER));
        if (modGate != null) {
            return skip(modGate);
        }
        if (!L2_KUBEJS_PLUGIN.equals(targetClassName)) {
            return new Decision(false, "unexpected target class", true);
        }
        if (!fingerprint(L2_KUBEJS_PLUGIN_PATH, L2_KUBEJS_PLUGIN_SHA256)) {
            return new Decision(false, "LHKJSPlugin.class fingerprint changed; re-audit before enabling", true);
        }
        if (!fingerprint(L2_ATTACK_EVENT_HANDLER_PATH, L2_ATTACK_EVENT_HANDLER_SHA256)) {
            return new Decision(false, "AttackEventHandler.class fingerprint changed; re-audit before enabling", true);
        }
        if (!l2ClearCachesShape()) {
            return new Decision(false, "LHKJSPlugin.clearCaches no longer has one Collection.removeIf site", true);
        }
        if (!l2RegistrationIsSynchronized()) {
            return new Decision(false, "AttackEventHandler.register is no longer the synchronized writer", true);
        }
        return apply("audited L2 Hostility cache clear under the AttackEventHandler registration lock");
    }

    private static Decision chatLineHeight(String targetClassName) {
        if (!CHAT_COMPONENT.equals(targetClassName)) {
            return new Decision(false, "unexpected target class", true);
        }
        ClassNode node = readClass(CHAT_COMPONENT, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        if (!evaluateChatLineHeightShape(node)) {
            return new Decision(false, "ChatComponent render or line-height computation changed", true);
        }
        return apply("audited vanilla chat line-height computation");
    }

    private static Decision goetyFocusCooldown(String targetClassName) {
        if (!GOETY_FOCUS_COOLDOWN.equals(targetClassName)) {
            return new Decision(false, "unexpected target class", true);
        }
        Gate modGate = gate("goety", GOETY);
        if (modGate != null) {
            return skip(modGate);
        }
        if (!fingerprint(GOETY_FOCUS_COOLDOWN_PATH, GOETY_FOCUS_COOLDOWN_SHA256)) {
            return new Decision(false, "FocusCooldown.class fingerprint changed; re-audit before enabling", true);
        }
        ClassNode node = readClass(GOETY_FOCUS_COOLDOWN, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
        if (!evaluateGoetyFocusCooldownShape(node)) {
            return new Decision(false, "FocusCooldown fields or cooldown query descriptors changed", true);
        }
        return apply("audited Goety FocusCooldown empty-map and empty-stack guard");
    }

    private static Decision goetySeHelper(String targetClassName) {
        if (!GOETY_SE_HELPER.equals(targetClassName)) {
            return new Decision(false, "unexpected target class", true);
        }
        Gate modGate = gate("goety", GOETY);
        if (modGate != null) {
            return skip(modGate);
        }
        if (!fingerprint(GOETY_SE_HELPER_PATH, GOETY_SE_HELPER_SHA256)) {
            return new Decision(false, "SEHelper.class fingerprint changed; re-audit before enabling", true);
        }
        ClassNode node = readClass(GOETY_SE_HELPER, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
        if (!evaluateGoetySeHelperShape(node)) {
            return new Decision(false, "SEHelper capability or cooldown descriptors changed", true);
        }
        return apply("audited Goety SEHelper lazy capability fallback and single-lookup cooldown queries");
    }

    private static Decision goetyMiscCapHelper(String targetClassName) {
        if (!GOETY_MISC_HELPER.equals(targetClassName)) {
            return new Decision(false, "unexpected target class", true);
        }
        Gate modGate = gate("goety", GOETY);
        if (modGate != null) {
            return skip(modGate);
        }
        if (!fingerprint(GOETY_MISC_HELPER_PATH, GOETY_MISC_HELPER_SHA256)) {
            return new Decision(false, "MiscCapHelper.class fingerprint changed; re-audit before enabling", true);
        }
        if (!hasStaticMethod(GOETY_MISC_HELPER, "getCapability", "(Lnet/minecraft/world/entity/LivingEntity;)Lcom/Polarice3/Goety/common/capabilities/misc/IMisc;")) {
            return new Decision(false, "MiscCapHelper.getCapability descriptor changed", true);
        }
        return apply("audited Goety MiscCapHelper lazy capability fallback");
    }

    private static Decision goetyLichdomHelper(String targetClassName) {
        if (!GOETY_LICHDOM_HELPER.equals(targetClassName)) {
            return new Decision(false, "unexpected target class", true);
        }
        Gate modGate = gate("goety", GOETY);
        if (modGate != null) {
            return skip(modGate);
        }
        if (!fingerprint(GOETY_LICHDOM_HELPER_PATH, GOETY_LICHDOM_HELPER_SHA256)) {
            return new Decision(false, "LichdomHelper.class fingerprint changed; re-audit before enabling", true);
        }
        if (!hasStaticMethod(GOETY_LICHDOM_HELPER, "getCapability", "(Lnet/minecraft/world/entity/player/Player;)Lcom/Polarice3/Goety/common/capabilities/lichdom/ILichdom;")) {
            return new Decision(false, "LichdomHelper.getCapability descriptor changed", true);
        }
        return apply("audited Goety LichdomHelper lazy capability fallback");
    }

    private static Decision fancyMenuGameRenderer(String targetClassName) {
        if (!GAME_RENDERER.equals(targetClassName)) {
            return new Decision(false, "unexpected target class", true);
        }
        Gate modGate = gate("fancymenu", FANCYMENU);
        if (modGate != null) {
            return skip(modGate);
        }
        if (!fingerprint(FANCYMENU_POSE_STACK_MIXIN_PATH, FANCYMENU_POSE_STACK_MIXIN_SHA256)) {
            return new Decision(false, "FancyMenu MixinPoseStack.class fingerprint changed; re-audit before enabling", true);
        }
        if (!hasMethod(GAME_RENDERER, "m_109089_", "(FJLcom/mojang/blaze3d/vertex/PoseStack;)V")
                || !hasMethod(GAME_RENDERER, "m_109093_", "(FJZ)V")) {
            return new Decision(false, "GameRenderer renderLevel/render SRG signatures changed", true);
        }
        return apply("audited FancyMenu 3.9.14 GameRenderer.renderLevel scope tracker");
    }

    private static Decision fancyMenuPoseStack(String targetClassName) {
        if (!POSE_STACK.equals(targetClassName)) {
            return new Decision(false, "unexpected target class", true);
        }
        Gate modGate = gate("fancymenu", FANCYMENU);
        if (modGate != null) {
            return skip(modGate);
        }
        if (!fingerprint(FANCYMENU_POSE_STACK_MIXIN_PATH, FANCYMENU_POSE_STACK_MIXIN_SHA256)) {
            return new Decision(false, "FancyMenu MixinPoseStack.class fingerprint changed; re-audit before enabling", true);
        }
        ClassNode node = readClass(POSE_STACK, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
        if (!evaluatePoseStackShape(node)) {
            return new Decision(false, "PoseStack SRG transform methods changed", true);
        }
        return apply("audited FancyMenu 3.9.14 PoseStack 3D world-rendering skip guard");
    }

    private static boolean l2ClearCachesShape() {
        ClassNode node = readClass(L2_KUBEJS_PLUGIN, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        if (node == null) {
            return false;
        }
        int methods = 0;
        int removeIfSites = 0;
        for (MethodNode method : node.methods) {
            if (!"clearCaches".equals(method.name) || !"()V".equals(method.desc)) {
                continue;
            }
            methods++;
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call
                        && "java/util/Collection".equals(call.owner)
                        && "removeIf".equals(call.name)
                        && L2_REMOVE_IF.equals(call.desc)) {
                    removeIfSites++;
                }
            }
        }
        return methods == 1 && removeIfSites == 1;
    }

    private static boolean l2RegistrationIsSynchronized() {
        ClassNode node = readClass(L2_ATTACK_EVENT_HANDLER, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
        if (node == null) {
            return false;
        }
        boolean hasSyncedRegister = node.methods.stream().anyMatch(m ->
                "register".equals(m.name)
                        && "(ILdev/xkmc/l2damagetracker/contents/attack/AttackListener;)V".equals(m.desc)
                        && (m.access & Opcodes.ACC_STATIC) != 0
                        && (m.access & Opcodes.ACC_SYNCHRONIZED) != 0);
        boolean hasGetListeners = node.methods.stream().anyMatch(m ->
                "getListeners".equals(m.name)
                        && "()Ljava/util/Collection;".equals(m.desc)
                        && (m.access & Opcodes.ACC_STATIC) != 0);
        return hasSyncedRegister && hasGetListeners;
    }

    static boolean evaluateChatLineHeightShape(ClassNode node) {
        if (node == null) {
            return false;
        }
        boolean hasRender = node.methods.stream().anyMatch(m -> "m_280165_".equals(m.name) && CHAT_RENDER.equals(m.desc));
        if (!hasRender) {
            return false;
        }
        int lineHeightMethods = 0;
        int optionGetSites = 0;
        for (MethodNode method : node.methods) {
            if (!"m_240691_".equals(method.name) || !"()I".equals(method.desc)) {
                continue;
            }
            lineHeightMethods++;
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call
                        && "net/minecraft/client/OptionInstance".equals(call.owner)
                        && "m_231551_".equals(call.name)
                        && "()Ljava/lang/Object;".equals(call.desc)) {
                    optionGetSites++;
                }
            }
        }
        return lineHeightMethods == 1 && optionGetSites == 1;
    }

    static boolean evaluateGoetyFocusCooldownShape(ClassNode node) {
        if (node == null || node.fields == null || node.methods == null) {
            return false;
        }
        boolean hasCooldownsField = node.fields.stream().anyMatch(f -> "cooldowns".equals(f.name) && "Ljava/util/Map;".equals(f.desc));
        boolean hasSpecificField = node.fields.stream().anyMatch(f -> "cooldownsSpecific".equals(f.name) && "Ljava/util/Map;".equals(f.desc));
        return hasCooldownsField
                && hasSpecificField
                && hasInstanceMethodOnNode(node, "getSpecificCooldownPercent", "(Lnet/minecraft/world/item/ItemStack;)F")
                && hasInstanceMethodOnNode(node, "isOnSpecificCooldown", "(Lnet/minecraft/world/item/ItemStack;)Z")
                && hasInstanceMethodOnNode(node, "removeSpecificCooldown", "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)V")
                && hasInstanceMethodOnNode(node, "getCooldownPercent", "(Lnet/minecraft/world/item/Item;)F")
                && hasInstanceMethodOnNode(node, "isOnCooldown", "(Lnet/minecraft/world/item/Item;)Z");
    }

    static boolean evaluateGoetySeHelperShape(ClassNode node) {
        if (node == null || node.methods == null) {
            return false;
        }
        return hasStaticMethodOnNode(node, "getCapability", "(Lnet/minecraft/world/entity/player/Player;)Lcom/Polarice3/Goety/common/capabilities/soulenergy/ISoulEnergy;")
                && hasStaticMethodOnNode(node, "getFocusCoolDown", "(Lnet/minecraft/world/entity/player/Player;)Lcom/Polarice3/Goety/common/capabilities/soulenergy/FocusCooldown;")
                && hasStaticMethodOnNode(node, "isOnCooldown", "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Z")
                && hasStaticMethodOnNode(node, "getCooldownPercent", "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)F");
    }

    static boolean evaluatePoseStackShape(ClassNode node) {
        if (node == null || node.methods == null) {
            return false;
        }
        return hasInstanceMethodOnNode(node, "m_85836_", "()V")
                && hasInstanceMethodOnNode(node, "m_85849_", "()V")
                && hasInstanceMethodOnNode(node, "m_85841_", "(FFF)V")
                && hasInstanceMethodOnNode(node, "m_252781_", "(Lorg/joml/Quaternionf;)V")
                && hasInstanceMethodOnNode(node, "m_252880_", "(FFF)V");
    }

    private static byte[] resource(String path) throws IOException {
        return resourceLookup.load(path);
    }

    private static ClassNode readClass(String className, int parsingOptions) {
        try {
            byte[] bytes = resource(className.replace('.', '/') + ".class");
            if (bytes == null) {
                return null;
            }
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, parsingOptions);
            return node;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean hasMethod(String className, String name, String desc) {
        ClassNode node = readClass(className, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
        return hasInstanceMethodOnNode(node, name, desc);
    }

    private static boolean hasStaticMethod(String className, String name, String desc) {
        ClassNode node = readClass(className, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
        return hasStaticMethodOnNode(node, name, desc);
    }

    private static boolean hasInstanceMethodOnNode(ClassNode node, String name, String desc) {
        if (node == null) {
            return false;
        }
        long count = node.methods.stream()
                .filter(m -> name.equals(m.name) && desc.equals(m.desc) && (m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC)) == 0)
                .count();
        return count == 1L;
    }

    private static boolean hasStaticMethodOnNode(ClassNode node, String name, String desc) {
        if (node == null) {
            return false;
        }
        long count = node.methods.stream()
                .filter(m -> name.equals(m.name) && desc.equals(m.desc)
                        && (m.access & Opcodes.ACC_STATIC) != 0
                        && (m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0)
                .count();
        return count == 1L;
    }

    private static boolean hasSmithingLocals(String className) {
        ClassNode node = readClass(className, ClassReader.SKIP_FRAMES);
        if (node == null) {
            return false;
        }
        for (MethodNode method : node.methods) {
            if (!"setRecipe".equals(method.name) || !SMITHING_SET_RECIPE.equals(method.desc)) {
                continue;
            }
            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC)) != 0
                    || method.localVariables == null || method.localVariables.isEmpty()) {
                return false;
            }
            int returnIndex = -1;
            for (int i = 0; i < method.instructions.size(); i++) {
                if (method.instructions.get(i).getOpcode() == Opcodes.RETURN) {
                    returnIndex = i;
                }
            }
            if (returnIndex < 0) {
                return false;
            }
            String[] expectedLocals = new String[]{
                    "Lmezz/jei/api/recipe/category/extensions/vanilla/smithing/ISmithingCategoryExtension;",
                    SLOT_BUILDER, SLOT_BUILDER, SLOT_BUILDER, SLOT_BUILDER
            };
            for (int i = 0; i < expectedLocals.length; i++) {
                int matches = 0;
                for (LocalVariableNode local : method.localVariables) {
                    int start = method.instructions.indexOf(local.start);
                    int end = method.instructions.indexOf(local.end);
                    if (local.index == 4 + i && expectedLocals[i].equals(local.desc) && start >= 0 && start <= returnIndex && returnIndex < end) {
                        matches++;
                    }
                }
                if (matches != 1) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean fingerprint(String path, String expectedSha256) {
        try {
            byte[] bytes = resource(path);
            if (bytes == null) {
                return false;
            }
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return expectedSha256.equals(HexFormat.of().formatHex(digest));
        } catch (IOException | RuntimeException | NoSuchAlgorithmException ignored) {
            return false;
        }
    }

    private static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    static void setInstalledVersionsForTests(Map<String, String> versions) {
        installedVersions = versions == null ? null : Map.copyOf(versions);
    }

    static void setResourceLookupForTests(ResourceLookup lookup) {
        resourceLookup = lookup == null ? DEFAULT_LOOKUP : lookup;
    }

    static void resetVersionCacheForTests() {
        installedVersions = null;
        resourceLookup = DEFAULT_LOOKUP;
        LOGGED_OVERLAP_GATE.clear();
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        String shortMixin = simpleName(mixinClassName);
        LOG.info("MERGED {} -> {}", shortMixin, targetClassName);
        if ("LodestoneTooltipMixin".equals(shortMixin) && TOOLTIP_TARGET.equals(targetClassName)) {
            applyLodestoneSynchronization(targetClass);
        } else if ("FancyMenuPoseStackMixin".equals(shortMixin) && POSE_STACK.equals(targetClassName)) {
            applyFancyMenuPoseStackGuard(targetClass);
        }
    }

    static boolean applyLodestoneSynchronization(ClassNode targetClass) {
        try {
            List<MethodNode> callbacks = targetClass.methods.stream()
                    .filter(m -> TOOLTIP_METHOD.equals(m.name) && TOOLTIP_DESCRIPTOR.equals(m.desc))
                    .toList();
            if (callbacks.size() != 1) {
                LOG.warn("SKIP Lodestone tooltip synchronization: expected exactly one audited callback, found {}", callbacks.size());
                return false;
            }
            MethodNode callback = callbacks.get(0);
            if ((callback.access & Opcodes.ACC_STATIC) == 0 || (callback.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                LOG.warn("SKIP Lodestone tooltip synchronization: transformed method shape changed");
                return false;
            }
            if ((callback.access & Opcodes.ACC_SYNCHRONIZED) != 0) {
                LOG.info("Lodestone tooltip callback is already synchronized; nothing to do");
                return true;
            }
            callback.access |= Opcodes.ACC_SYNCHRONIZED;
            LOG.info("Serialized audited Lodestone/AttributesLib base-modifier callback");
            return true;
        } catch (RuntimeException ex) {
            LOG.error("Lodestone tooltip synchronization failed; leaving the callback unchanged", ex);
            return false;
        }
    }

    static boolean applyFancyMenuPoseStackGuard(ClassNode targetClass) {
        try {
            List<MethodNode> candidates = targetClass.methods.stream()
                    .filter(m -> "()Z".equals(m.desc)
                            && (FANCYMENU_MODEL_VIEW_CHECK.equals(m.name) || m.name.endsWith("$" + FANCYMENU_MODEL_VIEW_CHECK)))
                    .toList();
            if (candidates.size() != 1) {
                LOG.warn("SKIP FancyMenu PoseStack guard: expected 1 {} method, found {}",
                        FANCYMENU_MODEL_VIEW_CHECK, candidates.size());
                return false;
            }
            MethodNode method = candidates.get(0);
            if ((method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0
                    || method.instructions == null
                    || method.instructions.size() == 0) {
                LOG.warn("SKIP FancyMenu PoseStack guard: unexpected method flags or empty body");
                return false;
            }
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call
                        && FANCYMENU_GUARD_OWNER.equals(call.owner)
                        && FANCYMENU_GUARD_METHOD.equals(call.name)
                        && "()Z".equals(call.desc)) {
                    LOG.info("FancyMenu PoseStack guard is already applied; nothing to do");
                    return true;
                }
            }
            LabelNode continueLabel = new LabelNode();
            InsnList prefix = new InsnList();
            prefix.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    FANCYMENU_GUARD_OWNER,
                    FANCYMENU_GUARD_METHOD,
                    "()Z",
                    false));
            prefix.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
            prefix.add(new InsnNode(Opcodes.ICONST_1));
            prefix.add(new InsnNode(Opcodes.IRETURN));
            prefix.add(continueLabel);
            prefix.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
            method.instructions.insert(prefix);
            method.maxStack = Math.max(method.maxStack, 2);
            method.maxLocals = Math.max(method.maxLocals, 1);
            LOG.info("Guarded FancyMenu PoseStack matrix tracker during GameRenderer.renderLevel");
            return true;
        } catch (RuntimeException ex) {
            LOG.error("FancyMenu PoseStack guard failed; leaving PoseStack unchanged", ex);
            return false;
        }
    }

    private record Gate(String problem, boolean relevant) {
    }

    record Decision(boolean enabled, String reason, boolean relevant) {
    }
}
