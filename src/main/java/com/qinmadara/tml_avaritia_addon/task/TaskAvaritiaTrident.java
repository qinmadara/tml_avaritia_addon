package com.qinmadara.tml_avaritia_addon.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidAttackTridentTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidRangedWalkToTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidTridentTargetTask;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

/**
 * 无尽三叉戟专属远程任务。
 *
 * 索敌 / 走位 / 追击 / 蓄力投掷状态机完全复用 TLM TaskTridentAttack 的原始行为，
 * 只是把索敌条件收窄为“主手必须是无尽三叉戟”。普通三叉戟在本任务下不会攻击。
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
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        BehaviorControl<? super EntityMaid> start = StartAttacking.create(
                entityMaid -> this.isWeapon(entityMaid, entityMaid.getMainHandItem()),
                IRangedAttackTask::findFirstValidAttackTarget);
        BehaviorControl<? super EntityMaid> stop = StopAttackingIfTargetInvalid.create(
                target -> !this.isWeapon(maid, maid.getMainHandItem()) || farAway(maid, target));
        BehaviorControl<? super EntityMaid> move = MaidRangedWalkToTarget.create(0.6F);
        BehaviorControl<? super EntityMaid> strafe = new MaidAttackTridentTask();
        BehaviorControl<? super EntityMaid> shoot = new MaidTridentTargetTask();
        return Lists.newArrayList(
                Pair.of(5, start), Pair.of(5, stop), Pair.of(5, move), Pair.of(5, strafe), Pair.of(5, shoot));
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(EntityMaid maid) {
        BehaviorControl<? super EntityMaid> start = StartAttacking.create(
                entityMaid -> this.isWeapon(entityMaid, entityMaid.getMainHandItem()),
                IRangedAttackTask::findFirstValidAttackTarget);
        BehaviorControl<? super EntityMaid> stop = StopAttackingIfTargetInvalid.create(
                target -> !this.isWeapon(maid, maid.getMainHandItem()) || farAway(maid, target));
        BehaviorControl<? super EntityMaid> shoot = new MaidTridentTargetTask();
        return Lists.newArrayList(Pair.of(5, start), Pair.of(5, stop), Pair.of(5, shoot));
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
        if (!(tridentStack.getItem() instanceof InfinityTridentItem) || maid.level().isClientSide) {
            // 普通三叉戟不在本任务生效范围内
            return;
        }

        InfinityThrownTrident thrownTrident = new InfinityThrownTrident(maid.level(), maid, tridentStack);
        thrownTrident.setLoyaltyLevel(0);
        thrownTrident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;

        // 直接朝目标坐标投掷，对齐 TLM 原版 TaskTridentAttack 的瞄准方式
        double x = target.getX() - maid.getX();
        double y = target.getY() - maid.getY();
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

    private boolean farAway(EntityMaid maid, LivingEntity target) {
        return !target.isAlive() || maid.distanceTo(target) > this.searchRadius(maid);
    }
}
