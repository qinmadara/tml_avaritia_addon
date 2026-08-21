package com.qinmadara.tml_avaritia_addon.event;

import com.google.common.collect.Lists;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.qinmadara.tml_avaritia_addon.Config;
import com.qinmadara.tml_avaritia_addon.tml_avaritia_addon;
import committee.nova.mods.avaritia.common.item.tools.InfinityArmorItem;
import committee.nova.mods.avaritia.util.ToolUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

/**
 * 女仆身穿无尽护甲的被动效果适配。
 *
 * 背景：Re-Avaritia 的 AbilityHandler 中所有无尽护甲特效都被 instanceof Player 门控，
 * 女仆（EntityMaid，LivingEntity）不是 Player，故特效全部不生效；只有基础护甲值/韧性/击退抗性
 * （标准装备属性）和猪灵中立（物品方法，LivingEntity 参数）对女仆天然生效。
 *
 * 本处理器镜像 AbilityHandler，但只对 EntityMaid 生效（服务端权威修改，自动同步客户端）：
 *   头盔：水下呼吸（空气槽 300）+ 夜视（Config.infinityArmorNightVision 开关）
 *   胸甲：清除负面药水效果
 *   护腿：火焰免疫（每 tick clearFire）
 *   靴子：台阶高度 1.0625（脱下恢复 0.6）+ 移动速度（MOVEMENT_SPEED 属性修饰符，bootSpeedBase 等 Config 对标 Re-Avaritia）
 * 不含（设计时跳过）：胸甲飞行 / 末影人无视 / 跳跃增强 / 饱食度（女仆无此概念或会破坏 AI）。
 */
@EventBusSubscriber(modid = tml_avaritia_addon.MODID)
public class MaidInfinityArmorHandler {

    // 无尽靴子移速修饰符的唯一 ResourceLocation（1.21.1 起属性修饰符以 ResourceLocation 标识；按同一 ID 增删，保证只存在一份，绝不叠加）
    private static final ResourceLocation BOOTS_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(tml_avaritia_addon.MODID, "infinity_boots_speed");
    // 玩家基础行走速度：把 bootSpeedBase（绝对值）换算成 MOVEMENT_SPEED 的百分比加成，与玩家穿靴 2× 对齐
    private static final double REFERENCE_WALK_SPEED = 0.1D;

    @SubscribeEvent
    public static void updateMaidAbilities(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof EntityMaid maid)) {
            return;
        }
        // 只服务端处理（女仆状态由服务端权威，修改后自动同步客户端）
        if (maid.level().isClientSide) {
            return;
        }
        boolean hasHelmet = ToolUtils.isPlayerWearing(maid, EquipmentSlot.HEAD, item -> item instanceof InfinityArmorItem);
        boolean hasChest = ToolUtils.isPlayerWearing(maid, EquipmentSlot.CHEST, item -> item instanceof InfinityArmorItem);
        boolean hasLeggings = ToolUtils.isPlayerWearing(maid, EquipmentSlot.LEGS, item -> item instanceof InfinityArmorItem);
        boolean hasBoots = ToolUtils.isPlayerWearing(maid, EquipmentSlot.FEET, item -> item instanceof InfinityArmorItem);

        if (hasHelmet) {
            handleHelmet(maid);
        }
        if (hasChest) {
            handleChest(maid);
        }
        if (hasLeggings) {
            handleLeggings(maid);
        }
        if (hasBoots) {
            handleBoots(maid);
        } else {
            // 脱下无尽靴子（或穿普通靴子/无靴）：仅在台阶高度被改过时恢复默认值，
            // 避免每 tick 写入 step height 干扰 TLM 自身移动/寻路逻辑；移速修饰符同样只在存在时移除。
            if (maid.maxUpStep() == 1.0625F) {
                setStepHeight(maid, 0.6F);
            }
            removeBootsSpeed(maid);
        }
    }

    /**
     * 全套无尽护甲的免伤（镜像 Re-Avaritia InfinityHandler#onInfiniteHurt 的玩家逻辑）。
     *
     * 玩家版在 LivingIncomingDamageEvent 中取消“非无尽伤害”（无尽伤害会穿透，再由高护甲值衰减）；
     * 女仆版在此基础上把无尽伤害一并取消——用户实测无尽之弓（天界箭/追踪箭均为 infinity 伤害类型）
     * 仍会对全套无尽甲女仆造成衰减后的伤害，这与“无尽甲无敌”的预期不符，因此对女仆统一取消全部伤害。
     * 与玩家一致的是：无尽剑攻击穿全套无尽甲的玩家时也只触发爆炸、不直接扣血，女仆全免伤与之语义对齐。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMaidIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid) || maid.level().isClientSide) {
            return;
        }
        if (ToolUtils.isInfinite(maid)) {
            event.setCanceled(true);
            maid.hurtTime = 0;
        }
    }

    /** 头盔：水下呼吸（空气槽 300）+ 夜视（Config 开关，镜像 Re-Avaritia handleHelmetStateChange） */
    private static void handleHelmet(EntityMaid maid) {
        maid.setAirSupply(300);
        if (Config.infinityArmorNightVision) {
            MobEffectInstance nv = maid.getEffect(MobEffects.NIGHT_VISION);
            if (nv == null) {
                // 首次添加夜视；duration 字段为 private，不能像 Re-Avaritia 那样直接写 nv.duration，
                // 因此只在剩余时长较低时补发一次，避免每 tick 分配/替换效果实例
                maid.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false));
            } else if (nv.getDuration() <= 240) {
                maid.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false));
            }
        }
    }

    /** 胸甲：清除负面药水效果（镜像 Re-Avaritia handleChestStateChange 的非增益移除） */
    private static void handleChest(EntityMaid maid) {
        List<MobEffectInstance> effects = Lists.newArrayList(maid.getActiveEffects());
        for (MobEffectInstance potion : effects) {
            if (!potion.getEffect().value().isBeneficial()) {
                maid.removeEffect(potion.getEffect());
            }
        }
    }

    /** 护腿：火焰免疫（每 tick 扑灭身上火焰） */
    private static void handleLeggings(EntityMaid maid) {
        if (maid.isOnFire()) {
            maid.clearFire();
        }
    }

    /** 靴子：台阶高度 + 移动速度（MOVEMENT_SPEED 属性修饰符，对齐玩家同装备的 bootSpeedBase 数值） */
    private static void handleBoots(EntityMaid maid) {
        // 台阶高度：可直接走上 1 格（17 像素）；仅在需要时写入，避免每 tick 干扰 TLM 移动/寻路
        if (maid.maxUpStep() < 1.0625F) {
            setStepHeight(maid, 1.0625F);
        }
        AttributeInstance speedAttr = maid.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) {
            return;
        }
        // 移动速度：继续使用 MOVEMENT_SPEED 属性修饰符（ADD_MULTIPLIED_BASE，1.21.1 起替代旧 MULTIPLY_BASE），
        // 而不是 Re-Avaritia 的 moveRelative 直接推速度。
        // 女仆陆地/游泳移动都由 MaidMoveControl 用 getAttributeValue(MOVEMENT_SPEED) 驱动（陆地 setSpeed、游泳 speedLerp×3），
        // 属性加成会自然叠加，且方向/启停完全由大脑与 MoveControl 控制——坐下、被传送清路后 brain 不设 MOVE_TO 就不会移动，
        // 彻底避免原 moveRelative 的“速度自持漂移/坐下仍移动/传送后仍朝原方向走”问题。
        // 数值尽量对齐 Re-Avaritia：陆地 = bootSpeedBase / 0.1（默认 +100%）；游泳乘 swimming 倍率；
        // 冲刺沿袭原版“额外固定速度增量”的相对语义，按 REFERENCE_WALK_SPEED 折算为属性比例。
        double baseRatio = Config.bootSpeedBase / REFERENCE_WALK_SPEED;
        double amount = baseRatio * (maid.isInWater() ? Config.bootSpeedSwimmingMultiplier : 1.0D);
        if (maid.isSprinting()) {
            amount += Config.bootSpeedSprintingMultiplier / REFERENCE_WALK_SPEED;
        }
        // 修饰符只保留一份；数值变化（水/陆/冲刺切换）时才移除重建，避免每 tick 增删
        AttributeModifier current = speedAttr.getModifier(BOOTS_SPEED_MODIFIER_ID);
        if (current == null || Math.abs(current.amount() - amount) > 1.0E-4D) {
            speedAttr.removeModifier(BOOTS_SPEED_MODIFIER_ID);
            speedAttr.addTransientModifier(new AttributeModifier(BOOTS_SPEED_MODIFIER_ID,
                    amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /**
     * 台阶高度设置：1.20.5+ 移除了 LivingEntity#setMaxUpStep，
     * 台阶高度改由 STEP_HEIGHT 属性驱动（Entity#maxUpStep 直接读取该属性值），这里按属性写入。
     */
    private static void setStepHeight(EntityMaid maid, float height) {
        AttributeInstance stepAttr = maid.getAttribute(Attributes.STEP_HEIGHT);
        if (stepAttr != null) {
            stepAttr.setBaseValue(height);
        }
    }

    /** 脱下无尽靴子：仅在修饰符仍存在时移除（普通靴子/无靴子时也兜底检查，确保绝不误触发加速） */
    private static void removeBootsSpeed(EntityMaid maid) {
        AttributeInstance speedAttr = maid.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null && speedAttr.getModifier(BOOTS_SPEED_MODIFIER_ID) != null) {
            speedAttr.removeModifier(BOOTS_SPEED_MODIFIER_ID);
        }
    }
}
