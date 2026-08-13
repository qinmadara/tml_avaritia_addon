package com.qinmadara.tml_avaritia_addon.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskTridentAttack;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.qinmadara.tml_avaritia_addon.util.AvaritiaWeaponUtil;
import com.qinmadara.tml_avaritia_addon.util.MaidTaskConfigUtil;
import committee.nova.mods.avaritia.common.entity.InfinityThrownTrident;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinityTridentItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;

import java.util.List;
import java.util.function.Predicate;

/**
 * 无尽三叉戟专属远程任务。
 * 直接继承 TLM 自带的 TaskTridentAttack（远程索敌/走位/蓄力/骑乘行为全部复用），
 * 仅覆盖 UID/图标/武器判定/条件描述/射击实现。
 *
 * 关键设计：
 * - 无尽三叉戟玩家版特效入口是 InfinityTridentItem#releaseUsing（内部 instanceof Player 门控
 *   + private shootTrident）；女仆 AI（MaidTridentTargetTask）走 startUsingItem →
 *   stopUsingItem + performRangedAttack，根本不经过 item.releaseUsing → 无需 Mixin，
 *   任务层覆写 performRangedAttack 直接复刻特效即可。
 * - 投掷实体用 InfinityThrownTrident（命中 Float.MAX_VALUE 伤害 + 落雷）；
 *   setLoyaltyLevel(0)：canCompleteReturn 要求 owner instanceof Player，女仆投出的无尽三叉戟
 *   永不回归；loyalty>0 会无物理无限环绕女仆（实体堆积），loyalty=0 则命中后插地不消失
 * - 女仆主手三叉戟保留（不似玩家从背包移除），可重复投掷；不调用 hurtAndBreak——
 *   无尽三叉戟为 IUndamageable 无限耐久武器，无需消耗耐久（且避免任何耐久损耗风险）。
 * - 瞄准逻辑对齐 TLM 原版 TaskTridentAttack：按距离调速度（1.6~3.2）/不准确度 + 无重力，
 *   保证女仆百发百中。
 */
public class TaskAvaritiaTrident extends TaskTridentAttack {

    public static final ResourceLocation UID = new ResourceLocation("tml_avaritia_addon", "avaritia_trident_attack");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return AvaritiaWeaponUtil.getInfinityTridentStack();
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return stack.getItem() instanceof InfinityTridentItem;
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Lists.newArrayList(Pair.of(
                "task.tml_avaritia_addon.avaritia_trident_attack.condition",
                maidEntity -> maidEntity.getMainHandItem().getItem() instanceof InfinityTridentItem));
    }

    @Override
    public MenuProvider getTaskConfigGuiProvider(EntityMaid maid) {
        return MaidTaskConfigUtil.emptyTaskConfig(maid);
    }

    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float distanceFactor) {
        ItemStack tridentStack = maid.getMainHandItem();
        if (!(tridentStack.getItem() instanceof InfinityTridentItem)) {
            super.performRangedAttack(maid, target, distanceFactor);
            return;
        }
        if (maid.level().isClientSide) {
            return;
        }

        // 无尽三叉戟：命中 Float.MAX_VALUE 伤害 + 落雷；loyalty=0（女仆无法回归，避免无物理环绕堆积）
        InfinityThrownTrident thrownTrident = new InfinityThrownTrident(maid.level(), maid, tridentStack);
        thrownTrident.setLoyaltyLevel(0);
        thrownTrident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;

        // 瞄准目标（对齐 TLM 原版 TaskTridentAttack：按距离调速度/不准确度 + 无重力保证命中）
        double x = target.getX() - maid.getX();
        double y = target.getEyeY() - maid.getEyeY();
        double z = target.getZ() - maid.getZ();
        float distance = maid.distanceTo(target);
        float velocity = Mth.clamp(distance / 10f, 1.6f, 3.2f);
        float inaccuracy = 1 - Mth.clamp(distance / 100f, 0, 0.9f);
        thrownTrident.setNoGravity(true);
        thrownTrident.shoot(x, y, z, velocity, inaccuracy);

        maid.level().addFreshEntity(thrownTrident);
        maid.level().playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
