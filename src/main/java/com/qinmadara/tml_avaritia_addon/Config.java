package com.qinmadara.tml_avaritia_addon;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 本模组配置。
 *
 * 注意：ModConfigEvent.Loading / Reloading 由 FML 在 MOD 总线上发出，
 * 因此 @EventBusSubscriber 必须显式指定 bus = Bus.MOD（1.20.1 移植到 1.21.1 时
 * 曾丢失该参数而默认注册到 GAME 总线，导致 onLoad 永不触发、配置文件全部不生效）。
 * 另补充 Reloading 订阅，游戏内修改/重载配置后立即同步静态字段。
 */
@EventBusSubscriber(modid = tml_avaritia_addon.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // 女仆剑击是否额外附带范围伤害（原版右键 AOE 的女仆版；女仆无法右键，故并入每次攻击，默认开启）
    private static final ModConfigSpec.BooleanValue MAID_SWORD_AOE = BUILDER
            .comment("Maid's infinity sword attack also triggers the right-click AOE effect (maid cannot right-click, so merged into each attack; default on)")
            .define("maid_sword_aoe", true);

    // 强制女仆无尽剑进入 Re-Avaritia 的 AOE 杀戮模式（infinity_sword_kill）。
    // 默认关：AOE 只命中敌对生物且排除 forge:neutral_creatures，避免误伤主人/盟友/中立生物；
    // 开启后与原版 kill mode 一致，AOE 不再区分敌我（仅排除女仆自身，并沿用物品/弹射物过滤）。
    private static final ModConfigSpec.BooleanValue FORCE_KILL_MODE = BUILDER
            .comment("Force maid's infinity sword AOE into kill mode (default off: AOE hits only hostile mobs; kill mode ignores friend/foe like Re-Avaritia)")
            .define("force_kill_mode", false);

    // ===== 无尽护甲·女仆被动适配 =====
    // 头盔：夜视开关
    private static final ModConfigSpec.BooleanValue INFINITY_ARMOR_NIGHT_VISION = BUILDER
            .comment("Infinity Open or Off Night Vision (maid only)")
            .define("infinity_armor_night_vision", true);
    // 靴子：移动速度（MOVEMENT_SPEED 属性修饰符，对齐玩家同装备的 bootSpeedBase 数值）
    private static final ModConfigSpec.DoubleValue BOOT_SPEED_BASE = BUILDER
            .comment("Boot speed base (maid only, aligns with Re-Avaritia boot_speed_base; applied as a % boost to MOVEMENT_SPEED: bootSpeedBase=0.1 → +100% → 2x base speed, same ratio as the player's boots)")
            .defineInRange("boot_speed_base", 0.1D, 0.01D, 1.0D);
    private static final ModConfigSpec.DoubleValue BOOT_SPEED_SWIMMING_MULTIPLIER = BUILDER
            .comment("Boot speed swimming multiplier (maid only, applied while the maid is in water)")
            .defineInRange("boot_speed_swimming_multiplier", 1.2D, 0.1D, 5.0D);
    private static final ModConfigSpec.DoubleValue BOOT_SPEED_SPRINTING_MULTIPLIER = BUILDER
            .comment("Boot speed sprinting multiplier (maid only, applied while the maid is sprinting; note: TLM maid AI doesn't set sprinting natively, so this is dormant unless sprinting is set by another source)")
            .defineInRange("boot_speed_sprinting_multiplier", 0.2D, 0.01D, 1.0D);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean maidSwordAoe;
    public static boolean forceKillMode;
    public static boolean infinityArmorNightVision;
    public static double bootSpeedBase;
    public static double bootSpeedSwimmingMultiplier;
    public static double bootSpeedSprintingMultiplier;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event)
    {
        apply(event);
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event)
    {
        apply(event);
    }

    /** 把配置值同步到静态字段（Loading 与 Reloading 共用；只处理本模组自己的配置，避免其他配置事件误触发） */
    private static void apply(final ModConfigEvent event)
    {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        maidSwordAoe = MAID_SWORD_AOE.get();
        forceKillMode = FORCE_KILL_MODE.get();
        infinityArmorNightVision = INFINITY_ARMOR_NIGHT_VISION.get();
        bootSpeedBase = BOOT_SPEED_BASE.get();
        bootSpeedSwimmingMultiplier = BOOT_SPEED_SWIMMING_MULTIPLIER.get();
        bootSpeedSprintingMultiplier = BOOT_SPEED_SPRINTING_MULTIPLIER.get();
    }
}
