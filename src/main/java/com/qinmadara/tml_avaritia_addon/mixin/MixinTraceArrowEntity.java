package com.qinmadara.tml_avaritia_addon.mixin;

import committee.nova.mods.avaritia.common.entity.arrow.TraceArrowEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修复 Re-Avaritia TraceArrowEntity#seekNextTarget 的目标选择：
 *
 * 原实现的 selector 只排除 Player，且内部 owner 字段在构造时被初始化为箭矢自身
 * （字段初始化先于 setOwner），导致“攻击者”参数恒为 null——女仆射出的追踪箭会把
 * 离箭最近的“女仆本人”选为追踪目标（女仆不是 Player，无法被 selector 排除），
 * 表现为追踪箭原地绕着女仆转、不去追敌人。
 *
 * 这里在 seekNextTarget 头部整体接管目标选择：
 * 1. 优先追踪射击者的当前攻击目标（女仆大脑 ATTACK_TARGET）；
 * 2. 兜底按“最近且可见、非射击者、非玩家”选择；
 * 3. TargetingConditions 传入真实射击者，使 isCombat 的 canAttack/isAlliedTo 判定生效，
 *    追踪箭只会追女仆可攻击的对象。
 */
@Mixin(TraceArrowEntity.class)
public abstract class MixinTraceArrowEntity {

    @Shadow
    private LivingEntity homingTarget;

    @Shadow
    private Vec3 seekOrigin;

    @Inject(method = "seekNextTarget", at = @At("HEAD"), cancellable = true)
    private void tmlAvaritia$seekNextTarget(CallbackInfo ci) {
        TraceArrowEntity arrow = (TraceArrowEntity) (Object) this;
        // 与原实现一致的前置条件：跳跃次数与暴击箭限制
        if (arrow.getJumpCount() > 16 || !arrow.isCritArrow()) {
            return;
        }
        if (arrow.level().isClientSide) {
            return;
        }
        if (this.seekOrigin == null) {
            this.seekOrigin = arrow.position();
        }

        Entity shooter = arrow.getOwner();
        LivingEntity attacker = shooter instanceof LivingEntity living ? living : null;

        // 1. 优先追踪射击者的当前攻击目标（女仆正在战斗的敌人）
        LivingEntity preferred = null;
        if (attacker instanceof Mob mob) {
            preferred = mob.getBrain()
                    .getMemory(MemoryModuleType.ATTACK_TARGET)
                    .filter(target -> target.isAlive() && target != shooter && target.hasLineOfSight(arrow))
                    .orElse(null);
        }
        if (preferred != null) {
            this.homingTarget = preferred;
            ci.cancel();
            return;
        }

        // 2. 兜底：最近的可见目标（排除射击者自身与玩家，与原实现语义对齐）
        TargetingConditions conditions = TargetingConditions.forCombat()
                .selector(living -> living != shooter
                        && !(living instanceof Player)
                        && living.hasLineOfSight(arrow));
        this.homingTarget = arrow.level().getNearestEntity(
                LivingEntity.class,
                conditions,
                attacker,
                this.seekOrigin.x,
                this.seekOrigin.y,
                this.seekOrigin.z,
                arrow.getBoundingBox().inflate(64.0D));
        ci.cancel();
    }
}
