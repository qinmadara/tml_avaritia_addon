package com.qinmadara.tml_avaritia_addon.util;

import com.qinmadara.tml_avaritia_addon.Config;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinityBowItem;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinityCrossBowItem;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinitySwordItem;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinityTridentItem;
import committee.nova.mods.avaritia.init.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 无尽武器识别与模式处理工具类。
 * 模式读写与玩家版 ISwitchable 一致：mode 子标签布尔（{mode:{infinity_sword_kill:true/false,...}}）。
 */
public final class AvaritiaWeaponUtil {

    private AvaritiaWeaponUtil() {
    }

    /** 实体主手是否持有无尽之剑（当前近战任务只接受无尽之剑） */
    public static boolean isHoldingAvaritiaMelee(LivingEntity entity) {
        return isAvaritiaMelee(entity.getMainHandItem());
    }

    /** 是否为无尽之剑 */
    public static boolean isAvaritiaMelee(ItemStack stack) {
        return stack.getItem() instanceof InfinitySwordItem;
    }

    /** 是否为无尽武器（无尽之剑 + 远程） */
    public static boolean isAvaritiaWeapon(ItemStack stack) {
        return isAvaritiaMelee(stack)
                || stack.getItem() instanceof InfinityBowItem
                || stack.getItem() instanceof InfinityCrossBowItem
                || stack.getItem() instanceof InfinityTridentItem;
    }

    /** 读取无尽武器当前模式（与玩家版 ISwitchable 一致：mode 子标签布尔） */
    public static boolean isModeActive(ItemStack stack, String mode) {
        return stack.getOrCreateTagElement("mode").getBoolean(mode);
    }

    /**
     * 应用女仆武器模式（女仆无法潜行右键切换模式）：
     * - force_kill_mode 开启：把无尽剑 mode.infinity_sword_kill 写为 true（不分敌我，谨慎）；
     * - force_kill_mode 关闭（默认）：强制写 false，杀戮模式默认关闭，避免误伤主人与盟友。
     */
    public static void applyForcedMode(ItemStack stack) {
        if (stack.getItem() instanceof InfinitySwordItem) {
            boolean forced = Config.forceKillMode;
            CompoundTag mode = stack.getOrCreateTagElement("mode");
            if (mode.getBoolean("infinity_sword_kill") != forced) {
                mode.putBoolean("infinity_sword_kill", forced);
            }
        }
    }

    /** 该模式是否可被女仆模式开关切换（按主手武器白名单校验，防伪造包） */
    public static boolean isModeSwitchable(ItemStack stack, String mode) {
        return (stack.getItem() instanceof InfinityBowItem && "infinity_bow_tracer".equals(mode))
                || (stack.getItem() instanceof InfinityCrossBowItem && "infinity_crossbow_multi".equals(mode));
    }

    /** 无尽之剑的默认物品栈（用于任务图标显示） */
    public static ItemStack getInfinitySwordStack() {
        return ModItems.infinity_sword.get().getDefaultInstance();
    }

    /** 无尽之弓的默认物品栈（用于任务图标显示） */
    public static ItemStack getInfinityBowStack() {
        return ModItems.infinity_bow.get().getDefaultInstance();
    }

    /** 无尽之弩的默认物品栈（用于任务图标显示） */
    public static ItemStack getInfinityCrossBowStack() {
        return ModItems.infinity_crossbow.get().getDefaultInstance();
    }

    /** 无尽三叉戟的默认物品栈（用于任务图标显示） */
    public static ItemStack getInfinityTridentStack() {
        return ModItems.infinity_trident.get().getDefaultInstance();
    }
}
