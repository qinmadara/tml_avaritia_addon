package com.qinmadara.tml_avaritia_addon.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidAttackStrafingTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidRangedWalkToTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidShootTargetTask;
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
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Predicate;

/**
 * 无尽之弓专属远程任务。
 *
 * 索敌 / 走位 / 追击 / 射击状态机完全复用 TLM TaskBowAttack 的原始行为，
 * 只是把索敌条件收窄为“主手必须是无尽之弓”。普通弓在本任务下不会攻击。
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
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        BehaviorControl<? super EntityMaid> start = StartAttacking.create(
                entityMaid -> this.isWeapon(entityMaid, entityMaid.getMainHandItem()),
                IRangedAttackTask::findFirstValidAttackTarget);
        BehaviorControl<? super EntityMaid> stop = StopAttackingIfTargetInvalid.create(
                target -> !this.isWeapon(maid, maid.getMainHandItem()) || farAway(maid, target));
        BehaviorControl<? super EntityMaid> move = MaidRangedWalkToTarget.create(0.6F);
        BehaviorControl<? super EntityMaid> strafe = new MaidAttackStrafingTask();
        BehaviorControl<? super EntityMaid> shoot = new MaidShootTargetTask();
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
        BehaviorControl<? super EntityMaid> shoot = new MaidShootTargetTask();
        return Lists.newArrayList(Pair.of(5, start), Pair.of(5, stop), Pair.of(5, shoot));
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Lists.newArrayList(Pair.of(
                "task.tml_avaritia_addon.avaritia_bow_attack.condition",
                maidEntity -> maidEntity.getMainHandItem().getItem() instanceof InfinityBowItem));
    }

    @Override
    public MenuProvider getTaskConfigGuiProvider(EntityMaid maid) {
        return MaidTaskConfigUtil.weaponModeConfig(maid);
    }

    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float distanceFactor) {
        ItemStack bowStack = maid.getMainHandItem();
        if (!(bowStack.getItem() instanceof InfinityBowItem)) {
            // 普通弓不在本任务生效范围内
            return;
        }

        ItemStack arrowStack = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(maid, arrowStack, distanceFactor);
        arrow = ((BowItem) bowStack.getItem()).customArrow(arrow);

        // 瞄准完全对齐 TLM 原版 TaskBowAttack：直接朝目标坐标射击，不使用 maid 的朝向。
        // 避免长时间战斗后身体朝向与目标不一致，导致“只朝一个方向射击”。
        double x = target.getX() - maid.getX();
        double y = target.getY() - maid.getY();
        double z = target.getZ() - maid.getZ();
        float distance = maid.distanceTo(target);
        float velocity = Mth.clamp(distance / 10.0F, 1.6F, 3.2F);
        float inaccuracy = 1.0F - Mth.clamp(distance / 100.0F, 0.0F, 0.9F);

        arrow.setNoGravity(true);
        arrow.shoot(x, y, z, velocity, inaccuracy);
        if (distanceFactor >= 1.0F) {
            arrow.setCritArrow(true);
        }
        maid.level().addFreshEntity(arrow);
        maid.level().playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                1.0F / (maid.level().getRandom().nextFloat() * 0.4F + 1.2F) + distanceFactor * 0.5F);
    }

    private boolean farAway(EntityMaid maid, LivingEntity target) {
        return !target.isAlive() || maid.distanceTo(target) > this.searchRadius(maid);
    }
}
