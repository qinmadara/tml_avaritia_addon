package com.qinmadara.tml_avaritia_addon;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// 配置骨架（后续需求按此模式继续扩展）
@Mod.EventBusSubscriber(modid = tml_avaritia_addon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 女仆剑击是否额外附带范围伤害（原版右键 AOE 的女仆版；女仆无法右键，故并入每次攻击，默认开启）
    private static final ForgeConfigSpec.BooleanValue MAID_SWORD_AOE = BUILDER
            .comment("Maid's infinity sword attack also triggers the right-click AOE effect (maid cannot right-click, so merged into each attack; default on)")
            .define("maid_sword_aoe", true);

    // 强制女仆无尽剑进入斩杀模式（默认关：女仆使用普通/默认模式，避免杀戮模式“不分敌我”误伤主人/盟友）
    private static final ForgeConfigSpec.BooleanValue FORCE_KILL_MODE = BUILDER
            .comment("Force maid's infinity sword into kill mode (default off: maid uses normal mode to avoid friendly fire; kill mode ignores friend/foe)")
            .define("force_kill_mode", false);

    // 女仆手持无尽武器时的攻击距离（格）
    private static final ForgeConfigSpec.DoubleValue MAID_MELEE_REACH = BUILDER
            .comment("Melee reach (blocks) for maid holding infinity weapons")
            .defineInRange("maid_melee_reach", 8.0D, 3.0D, 16.0D);

    // ===== 无尽护甲·女仆被动适配 =====
    // 对标 Re-Avaritia ModConfig 的护甲相关项（同名同值同范围）；这些项只对女仆生效，
    // 玩家端仍由 Re-Avaritia 自身的 AbilityHandler 处理。
    // 头盔：夜视开关
    private static final ForgeConfigSpec.BooleanValue INFINITY_ARMOR_NIGHT_VISION = BUILDER
            .comment("Infinity Open or Off Night Vision (maid only)")
            .define("infinity_armor_night_vision", true);
    // 靴子：移动速度（MOVEMENT_SPEED 属性修饰符，对齐玩家同装备的 bootSpeedBase 数值）
    private static final ForgeConfigSpec.DoubleValue BOOT_SPEED_BASE = BUILDER
            .comment("Boot speed base (maid only, aligns with Re-Avaritia boot_speed_base; applied as a % boost to MOVEMENT_SPEED: bootSpeedBase=0.1 → +100% → 2x base speed, same ratio as the player's boots)")
            .defineInRange("boot_speed_base", 0.1D, 0.01D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue BOOT_SPEED_FLYING_MULTIPLIER = BUILDER
            .comment("Boot speed flying multiplier (config parity with Re-Avaritia; maid AI-driven, currently unused)")
            .defineInRange("boot_speed_flying_multiplier", 1.1D, 0.1D, 5.0D);
    private static final ForgeConfigSpec.DoubleValue BOOT_SPEED_SWIMMING_MULTIPLIER = BUILDER
            .comment("Boot speed swimming multiplier (maid only, applied while the maid is in water)")
            .defineInRange("boot_speed_swimming_multiplier", 1.2D, 0.1D, 5.0D);
    private static final ForgeConfigSpec.DoubleValue BOOT_SPEED_SNEAKING_MULTIPLIER = BUILDER
            .comment("Boot speed sneaking multiplier (config parity with Re-Avaritia; maid can't sneak, currently unused)")
            .defineInRange("boot_speed_sneaking_multiplier", 0.1D, 0.01D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue BOOT_SPEED_BACKWARD_MULTIPLIER = BUILDER
            .comment("Boot speed backward multiplier (config parity with Re-Avaritia; maid AI-driven, currently unused)")
            .defineInRange("boot_speed_backward_multiplier", 0.25D, 0.01D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue BOOT_SPEED_STRAFING_MULTIPLIER = BUILDER
            .comment("Boot speed strafing multiplier (config parity with Re-Avaritia; maid AI-driven, currently unused)")
            .defineInRange("boot_speed_strafing_multiplier", 0.45D, 0.01D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue BOOT_SPEED_SPRINTING_MULTIPLIER = BUILDER
            .comment("Boot speed sprinting multiplier (maid only, applied while the maid is sprinting; note: TLM maid AI doesn't set sprinting natively, so this is dormant unless sprinting is set by another source)")
            .defineInRange("boot_speed_sprinting_multiplier", 0.2D, 0.01D, 1.0D);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean maidSwordAoe;
    public static boolean forceKillMode;
    public static double maidMeleeReach;
    public static boolean infinityArmorNightVision;
    public static double bootSpeedBase;
    public static double bootSpeedFlyingMultiplier;
    public static double bootSpeedSwimmingMultiplier;
    public static double bootSpeedSneakingMultiplier;
    public static double bootSpeedBackwardMultiplier;
    public static double bootSpeedStrafingMultiplier;
    public static double bootSpeedSprintingMultiplier;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        maidSwordAoe = MAID_SWORD_AOE.get();
        forceKillMode = FORCE_KILL_MODE.get();
        maidMeleeReach = MAID_MELEE_REACH.get();
        infinityArmorNightVision = INFINITY_ARMOR_NIGHT_VISION.get();
        bootSpeedBase = BOOT_SPEED_BASE.get();
        bootSpeedFlyingMultiplier = BOOT_SPEED_FLYING_MULTIPLIER.get();
        bootSpeedSwimmingMultiplier = BOOT_SPEED_SWIMMING_MULTIPLIER.get();
        bootSpeedSneakingMultiplier = BOOT_SPEED_SNEAKING_MULTIPLIER.get();
        bootSpeedBackwardMultiplier = BOOT_SPEED_BACKWARD_MULTIPLIER.get();
        bootSpeedStrafingMultiplier = BOOT_SPEED_STRAFING_MULTIPLIER.get();
        bootSpeedSprintingMultiplier = BOOT_SPEED_SPRINTING_MULTIPLIER.get();
    }
}
