package com.qinmadara.tml_avaritia_addon.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidAttackStrafingTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCrossbowAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidRangedWalkToTarget;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * 无尽之弩专属远程任务。
 *
 * 索敌 / 走位 / 追击 / 装填状态机完全复用 TLM TaskCrossBowAttack 的原始行为，
 * 只是把索敌条件收窄为“主手必须是无尽之弩”。普通弩在本任务下不会攻击。
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
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        BehaviorControl<? super EntityMaid> start = StartAttacking.create(
                entityMaid -> this.isWeapon(entityMaid, entityMaid.getMainHandItem()),
                IRangedAttackTask::findFirstValidAttackTarget);
        BehaviorControl<? super EntityMaid> stop = StopAttackingIfTargetInvalid.create(
                target -> !this.isWeapon(maid, maid.getMainHandItem()) || farAway(maid, target));
        BehaviorControl<? super EntityMaid> move = MaidRangedWalkToTarget.create(0.6F);
        BehaviorControl<? super EntityMaid> strafe = new MaidAttackStrafingTask();
        BehaviorControl<? super EntityMaid> shoot = new MaidCrossbowAttack();
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
        BehaviorControl<? super EntityMaid> shoot = new MaidCrossbowAttack();
        return Lists.newArrayList(Pair.of(5, start), Pair.of(5, stop), Pair.of(5, shoot));
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Lists.newArrayList(Pair.of(
                "task.tml_avaritia_addon.avaritia_crossbow_attack.condition",
                maidEntity -> maidEntity.getMainHandItem().getItem() instanceof InfinityCrossBowItem));
    }

    @Override
    public MenuProvider getTaskConfigGuiProvider(EntityMaid maid) {
        return MaidTaskConfigUtil.weaponModeConfig(maid);
    }

    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float distanceFactor) {
        ItemStack crossbowStack = maid.getMainHandItem();
        if (!(crossbowStack.getItem() instanceof InfinityCrossBowItem) || maid.level().isClientSide) {
            // 普通弩不在本任务生效范围内
            return;
        }

        boolean multi = AvaritiaWeaponUtil.isModeActive(crossbowStack, "infinity_crossbow_multi");
        float[] angles = multi ? new float[]{-20.0F, -10.0F, 0.0F, 10.0F, 20.0F} : new float[]{0.0F};

        // 直接朝目标坐标射击，不依赖 maid 朝向，避免长时间战斗后射击方向漂移
        Vec3 aim = target.getEyePosition().subtract(maid.getEyePosition()).normalize();
        for (float angle : angles) {
            HeavenArrowEntity arrow = new HeavenArrowEntity(maid);
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            arrow.setNoGravity(true);

            Vec3 dir = angle == 0.0F ? aim : aim.yRot((float) Math.toRadians(angle));
            arrow.shoot(dir.x, dir.y, dir.z, 3.0F, 0.0F);
            maid.level().addFreshEntity(arrow);
        }
        maid.level().playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private boolean farAway(EntityMaid maid, LivingEntity target) {
        return !target.isAlive() || maid.distanceTo(target) > this.searchRadius(maid);
    }
}
