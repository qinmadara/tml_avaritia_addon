package com.qinmadara.tml_avaritia_addon.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidMeleeAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.qinmadara.tml_avaritia_addon.util.AvaritiaWeaponUtil;
import com.qinmadara.tml_avaritia_addon.util.MaidTaskConfigUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * 女仆无尽武器战斗任务：索敌（StartAttacking）/ 停止（StopAttackingIfTargetInvalid）/ 攻击（MaidMeleeAttack）
 * 全部复用 TLM 自带实现；无尽武器特效触发由 MaidHurtTarget.Pre 事件处理器完成。
 * 仅当女仆手持无尽武器时，TLM 任务管理器会选择本任务。
 */
public class TaskAvaritia implements IAttackTask {

    public static final ResourceLocation UID = new ResourceLocation("tml_avaritia_addon", "avaritia_attack");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return AvaritiaWeaponUtil.getInfinitySwordStack();
    }

    @Override
    public @Nullable SoundEvent getAmbientSound(EntityMaid maid) {
        // 暂不提供自定义环境音效；需要时可按 TaskSlashBlade 的 SoundUtil.attackSound 模式补充
        return null;
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        // 仅手持无尽武器时才启动索敌（1.20.1 的 StartAttacking 需要 Function 形式的目标查找器）
        BehaviorControl<? super EntityMaid> start = StartAttacking.create(
                (Predicate<EntityMaid>) AvaritiaWeaponUtil::isHoldingAvaritiaMelee,
                // 1.20.1 中 NEAREST_VISIBLE_LIVING_ENTITIES 内存存储的是 NearestVisibleLivingEntities 包装类，
                // 需用 findClosest 解包出目标 LivingEntity（findClosest 返回 Optional<LivingEntity>）
                maidEntity -> maidEntity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                        .flatMap(nearby -> nearby.findClosest(living -> true)));
        // 失去武器或目标死亡时停止
        BehaviorControl<? super EntityMaid> stop = StopAttackingIfTargetInvalid.create(
                target -> !AvaritiaWeaponUtil.isHoldingAvaritiaMelee(maid) || !target.isAlive());
        // 攻击行为：直接复用 TLM 自带的 MaidMeleeAttack（内部处理靠近目标、swing + doHurtTarget + 攻击冷却）。
        // 注意：TLM 1.5.3 的 MaidMeleeAttack 只有 create(int) 重载，int 为攻击冷却基准值（tick），
        // 20 对应 1 秒，与 vanilla MeleeAttackAI 的默认冷却一致；内部还会按 ATTACK_SPEED 属性折算实际冷却。
        // doHurtTarget 会触发 MaidHurtTarget.Pre 事件 → AvaritiaMaidAttackHandler 复刻无尽剑特效。
        BehaviorControl<? super EntityMaid> attack = MaidMeleeAttack.create(20);
        return Lists.newArrayList(Pair.of(5, start), Pair.of(5, stop), Pair.of(5, attack));
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(EntityMaid maid) {
        return createBrainTasks(maid);
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        // 仅近战（剑/斧/镐/铲/锄）；远程武器（弓/弩/三叉戟）由各自的专属任务接管
        return AvaritiaWeaponUtil.isAvaritiaMelee(stack);
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        // TLM 1.5.3 的 IMaidTask#getConditionDescription 返回“描述文本键 + 生效条件”列表
        return Lists.newArrayList(Pair.of(
                "task.tml_avaritia_addon.avaritia_attack.condition",
                AvaritiaWeaponUtil::isHoldingAvaritiaMelee));
    }

    @Override
    public MenuProvider getTaskConfigGuiProvider(EntityMaid maid) {
        return MaidTaskConfigUtil.emptyTaskConfig(maid);
    }
}
