package com.qinmadara.tml_avaritia_addon.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskBowAttack;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.qinmadara.tml_avaritia_addon.util.AvaritiaWeaponUtil;
import com.qinmadara.tml_avaritia_addon.util.MaidTaskConfigUtil;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinityBowItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.MenuProvider;

import java.util.List;
import java.util.function.Predicate;

/**
 * 无尽之弓专属远程任务。
 * 直接继承 TLM 自带的 TaskBowAttack（远程索敌/射击/骑乘行为全部复用），
 * 仅覆盖 UID/图标/武器判定/条件描述，使面板显示"无尽之弓攻击"。
 * 箭矢特效由 MixinInfinityBowItem#customArrow 触发。
 */
public class TaskAvaritiaBow extends TaskBowAttack {

    public static final ResourceLocation UID = new ResourceLocation("tml_avaritia_addon", "avaritia_bow_attack");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return AvaritiaWeaponUtil.getInfinityBowStack();
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return stack.getItem() instanceof InfinityBowItem;
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Lists.newArrayList(Pair.of(
                "task.tml_avaritia_addon.avaritia_bow_attack.condition",
                maidEntity -> maidEntity.getMainHandItem().getItem() instanceof InfinityBowItem));
    }

    @Override
    public MenuProvider getTaskConfigGuiProvider(EntityMaid maid) {
        // 无尽弓使用"武器攻击模式切换"任务设置界面（标题 + 追踪模式开关按钮）
        return MaidTaskConfigUtil.weaponModeConfig(maid);
    }

    /**
     * 无尽弓无限箭矢（与玩家行为对齐：无箭可射、不消耗箭）。
     * TaskBowAttack#performRangedAttack 通过私有 getArrow(maid, charge)
     * 从合并背包（maid.getAvailableInv(true)）取真实箭栈，并在"无限附魔等级 <= 0"时执行
     * arrowStack.shrink(1) 消耗并写回背包；无尽弓的"无限"是武器特效而非附魔（无自带附魔），
     * 附魔判定恒为 0 → 必然消耗。
     * 这里对无尽弓主手改用脱离背包的普通箭作为无限箭源：不 shrink、不写回背包 → 不消耗；
     * 箭矢经 ProjectileUtil.getMobArrow → InfinityBowItem#customArrow（MixinInfinityBowItem）
     * 替换为天界箭/追踪箭，特效链路不变。
     */
    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float distanceFactor) {
        ItemStack bowStack = maid.getMainHandItem();
        if (!(bowStack.getItem() instanceof InfinityBowItem)) {
            super.performRangedAttack(maid, target, distanceFactor);
            return;
        }
        // 脱离背包的普通箭（无限箭源，不消耗背包）
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(maid, arrowStack, distanceFactor);
        arrow = ((BowItem) bowStack.getItem()).customArrow(arrow);
        // 对齐 TLM 原版 TaskBowAttack 行为：无重力直线飞行，保证远距离命中精度
        arrow.setNoGravity(true);
        arrow.shootFromRotation(maid, maid.getXRot(), maid.getYRot(), 0.0F, distanceFactor * 3.0F, 1.0F);
        if (distanceFactor >= 1.0F) {
            arrow.setCritArrow(true);
        }
        maid.level().addFreshEntity(arrow);
        maid.level().playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                1.0F / (maid.level().getRandom().nextFloat() * 0.4F + 1.2F) + distanceFactor * 0.5F);
    }
}
