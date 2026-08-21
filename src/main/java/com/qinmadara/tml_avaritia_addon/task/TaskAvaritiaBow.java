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
import committee.nova.mods.avaritia.common.entity.arrow.HeavenArrowEntity;
import committee.nova.mods.avaritia.common.entity.arrow.TraceArrowEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Predicate;

/**
 * 无尽之弓专属远程任务。
 *
 * 索敌 / 走位 / 追击 / 射击状态机完全复用 TLM TaskBowAttack 的原始行为，
 * 只是把索敌条件收窄为“主手必须是无尽之弓”。普通弓在本任务下不会攻击。
 *
 * 1.21.1 修复：显式覆写 searchRadius/searchDimension/canSee。
 * 不再依赖父类 gated searchDimension（其回退分支仍是 8 格非家范围）
 * 与共享静态 TargetingConditions 的 canSee，改为以女仆自身为中心、
 * 48 格半径 + 视线判定的确定性实现，保证远距离敌人能被稳定索敌。
 */
public class TaskAvaritiaBow extends TaskBowAttack {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("tml_avaritia_addon", "avaritia_bow_attack");

    /** 索敌半径：对齐 TLM BOW_RANGE 默认 48 */
    private static final float SEARCH_RADIUS = 48.0F;
    /** 索敌 AABB 垂直范围：覆盖常见高低差地形 */
    private static final double SEARCH_VERTICAL = 16.0D;

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
    public float searchRadius(EntityMaid maid) {
        return SEARCH_RADIUS;
    }

    @Override
    public AABB searchDimension(EntityMaid maid) {
        // 始终以女仆自身为中心（不随 home 中心漂移），避免女仆远离家时传感器“失明”
        return maid.getBoundingBox().inflate(SEARCH_RADIUS, SEARCH_VERTICAL, SEARCH_RADIUS);
    }

    @Override
    public boolean canSee(EntityMaid maid, LivingEntity target) {
        // 确定性可见判定：距离在索敌半径内 + 视线无阻挡。
        // 不再使用 IRangedAttackTask.targetConditionsTest（共享静态 TargetingConditions，语义易被其他调用方影响）
        return target.isAlive()
                && maid.distanceToSqr(target) <= (double) SEARCH_RADIUS * SEARCH_RADIUS
                && maid.getSensing().hasLineOfSight(target);
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

        // 1.21.1 适配说明：1.20.1 中箭矢替换通过 Mixin 注入 BowItem#customArrow 实现，
        // 但 1.21.1 起 vanilla BowItem 已无 customArrow 方法（该方法现为 NeoForge
        // IItemExtension 的接口默认方法，Mixin 无法对 vanilla 类注入）。
        // 由于 Mixin 唯一生效路径就是本覆写的 performRangedAttack，这里直接把替换逻辑内联，行为完全一致：
        // 追踪模式（SwitchMode=infinity_bow_tracer）-> 追踪箭；默认 -> 天界箭（命中 500 伤害 + 50 子箭弹幕）
        AbstractArrow arrow = AvaritiaWeaponUtil.isModeActive(bowStack, "infinity_bow_tracer")
                ? new TraceArrowEntity(maid)
                : new HeavenArrowEntity(maid);
        arrow.setPos(maid.getX(), maid.getEyeY() - 0.1D, maid.getZ());
        arrow.setBaseDamage(arrow.getBaseDamage() * 5000.0D);
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;

        // 瞄准完全对齐 TLM 原版 TaskBowAttack：直接朝目标坐标射击，不使用 maid 的朝向。
        // 注意：必须用 getEyeY 计算（1.20.1 移植时误用 getY：箭矢出生点在女仆眼睛处，
        // 而方向向量按脚底高度计算，整条弹道被抬高约 1.4 格，导致只有一格高的目标
        // （蜘蛛/小僵尸）被箭矢从头顶越过）。
        double x = target.getX() - maid.getX();
        double y = target.getEyeY() - maid.getEyeY();
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
