package com.qinmadara.tml_avaritia_addon.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskCrossBowAttack;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.qinmadara.tml_avaritia_addon.util.AvaritiaWeaponUtil;
import com.qinmadara.tml_avaritia_addon.util.MaidTaskConfigUtil;
import committee.nova.mods.avaritia.common.entity.arrow.HeavenArrowEntity;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinityCrossBowItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * 无尽之弩专属远程任务。
 * 直接继承 TLM 自带的 TaskCrossBowAttack（远程索敌/走位/装填状态机/骑乘行为全部复用），
 * 仅覆盖 UID/图标/武器判定/条件描述/射击实现。
 *
 * 关键设计：
 * - 无尽弩玩家版特效入口是 InfinityCrossBowItem#use() → private performShooting(Level, Player, ...)
 *   （参数强制 Player 且 private，女仆 AI 到不了；releaseUsing 被 instanceof Player 门控，
 *   女仆装填是 no-op、不 setCharged，但 MaidCrossbowAttack 状态机 CHARGING→CHARGED→READY_TO_ATTACK
 *   纯按装填 tick 数与冷却计数推进、不依赖 isCharged → 女仆必然走到 READY_TO_ATTACK
 *   并调用 shooter.performRangedAttack(target, 1.0F) 分发到本任务）。
 * - 本方法复刻玩家版特效：天界箭（HeavenArrowEntity），多段模式（SwitchMode NBT =
 *   "infinity_crossbow_multi"）时扇形 5 发（偏航 -20/-10/0/10/20，velocity 3.0），
 *   与玩家版 shootInfnityArrow(3.0F, 1.0F, angle) 对齐。
 * - 弹药门（hasArrow/hasAmmunition）由 MixinTaskCrossBowAttack 放行；
 *   本任务直接生成天界箭（脱离背包，不消耗箭矢）。
 */
public class TaskAvaritiaCrossBow extends TaskCrossBowAttack {

    public static final ResourceLocation UID = new ResourceLocation("tml_avaritia_addon", "avaritia_crossbow_attack");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return AvaritiaWeaponUtil.getInfinityCrossBowStack();
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return stack.getItem() instanceof InfinityCrossBowItem;
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Lists.newArrayList(Pair.of(
                "task.tml_avaritia_addon.avaritia_crossbow_attack.condition",
                maidEntity -> maidEntity.getMainHandItem().getItem() instanceof InfinityCrossBowItem));
    }

    @Override
    public MenuProvider getTaskConfigGuiProvider(EntityMaid maid) {
        // 无尽弩使用"武器攻击模式切换"任务设置界面（标题 + 多段模式开关按钮）
        return MaidTaskConfigUtil.weaponModeConfig(maid);
    }

    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float distanceFactor) {
        ItemStack crossbowStack = maid.getMainHandItem();
        if (!(crossbowStack.getItem() instanceof InfinityCrossBowItem)) {
            super.performRangedAttack(maid, target, distanceFactor);
            return;
        }
        if (maid.level().isClientSide) {
            return;
        }
        // 多段模式：SwitchMode NBT 与玩家版 ISwitchable 一致（infinity_crossbow_multi）
        boolean multi = AvaritiaWeaponUtil.isModeActive(crossbowStack, "infinity_crossbow_multi");
        float[] angles = multi ? new float[]{-20.0F, -10.0F, 0.0F, 10.0F, 20.0F} : new float[]{0.0F};
        // 瞄准目标（女仆版：无重力直线飞行保证命中，对齐 TLM 原版远程任务精度处理）
        Vec3 aim = target.getEyePosition().subtract(maid.getEyePosition()).normalize();
        for (float angle : angles) {
            HeavenArrowEntity arrow = new HeavenArrowEntity(maid);
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            arrow.setNoGravity(true);
            // 与玩家版 shootFromRotation(player, xRot, yRot + angle, 0, 3.0F, 1.0F) 的扇形展开对齐：
            // 对瞄准向量绕 Y 轴旋转 angle 度
            Vec3 dir = aim;
            if (angle != 0.0F) {
                dir = aim.yRot((float) Math.toRadians(angle));
            }
            arrow.shoot(dir.x, dir.y, dir.z, 3.0F, 0.0F);
            maid.level().addFreshEntity(arrow);
        }
        maid.level().playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
