package com.qinmadara.tml_avaritia_addon.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidMeleeAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidUseShieldTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskAttack;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.qinmadara.tml_avaritia_addon.util.AvaritiaWeaponUtil;
import com.qinmadara.tml_avaritia_addon.util.MaidTaskConfigUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Predicate;

/**
 * 女仆无尽之剑攻击任务。
 *
 * 直接继承 TLM 自带 TaskAttack，完整复用其索敌 / 追击 / 停止 / 近战攻击 / 盾牌逻辑，
 * 仅把“可用武器”收窄为无尽之剑。无尽之剑特效与 AOE 由 MaidHurtTarget.Pre 事件处理器触发，
 * 并且事件处理器只在本任务下生效。
 *
 * 1.21.1 修复：TaskAttack 未覆写 searchRadius/searchDimension（默认 8 格非家范围），
 * 且其 farAway 在女仆有主人时按“主人与目标的距离”判断（主人站远一点目标就会被立刻丢弃）。
 * 这里显式覆写为以女仆自身为中心的 32 格索敌范围、以女仆-目标距离判断停止追击，
 * 并保持 TaskAttack 原始的战斗行为组合（StartAttacking / StopAttackingIfTargetInvalid /
 * SetWalkTargetFromAttackTargetIfTargetOutOfReach / MaidMeleeAttack / MaidUseShieldTask）。
 */
public class TaskAvaritia extends TaskAttack {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("tml_avaritia_addon", "avaritia_attack");

    /** 索敌半径：与 TLM 远程任务（BOW_RANGE/TRIDENT_RANGE 默认 48）同一量级，保证远距离敌人也能被发现 */
    private static final float SEARCH_RADIUS = 32.0F;
    /** 索敌 AABB 垂直范围：覆盖常见高低差地形 */
    private static final double SEARCH_VERTICAL = 16.0D;

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return AvaritiaWeaponUtil.getInfinitySwordStack();
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        // 只接受无尽之剑；远程武器（弓/弩/三叉戟）由各自的专属任务接管
        return AvaritiaWeaponUtil.isAvaritiaMelee(stack);
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
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        BehaviorControl<EntityMaid> start = StartAttacking.create(
                entityMaid -> this.isWeapon(entityMaid, entityMaid.getMainHandItem()),
                IAttackTask::findFirstValidAttackTarget);
        BehaviorControl<EntityMaid> stop = StopAttackingIfTargetInvalid.create(
                target -> !this.isWeapon(maid, maid.getMainHandItem()) || farAway(maid, target));
        BehaviorControl<Mob> move = SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(0.6F);
        BehaviorControl<EntityMaid> attack = MaidMeleeAttack.create(20);
        MaidUseShieldTask shield = new MaidUseShieldTask();

        return Lists.newArrayList(
                Pair.of(5, start), Pair.of(5, stop), Pair.of(5, move), Pair.of(5, attack), Pair.of(5, shield));
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(EntityMaid maid) {
        BehaviorControl<EntityMaid> start = StartAttacking.create(
                entityMaid -> this.isWeapon(entityMaid, entityMaid.getMainHandItem()),
                IAttackTask::findFirstValidAttackTarget);
        BehaviorControl<EntityMaid> stop = StopAttackingIfTargetInvalid.create(
                target -> !this.isWeapon(maid, maid.getMainHandItem()) || farAway(maid, target));
        BehaviorControl<EntityMaid> attack = MaidMeleeAttack.create(20);
        MaidUseShieldTask shield = new MaidUseShieldTask();

        return Lists.newArrayList(
                Pair.of(5, start), Pair.of(5, stop), Pair.of(5, attack), Pair.of(5, shield));
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Lists.newArrayList(Pair.of(
                "task.tml_avaritia_addon.avaritia_attack.condition",
                AvaritiaWeaponUtil::isHoldingAvaritiaMelee));
    }

    @Override
    public MenuProvider getTaskConfigGuiProvider(EntityMaid maid) {
        return MaidTaskConfigUtil.emptyTaskConfig(maid);
    }

    /** 停止追击判定：目标死亡或超出索敌半径。不使用 TaskAttack 的“主人距离”判定，避免主人站位导致目标被丢弃。 */
    private boolean farAway(EntityMaid maid, LivingEntity target) {
        return !target.isAlive() || maid.distanceTo(target) > this.searchRadius(maid);
    }
}
