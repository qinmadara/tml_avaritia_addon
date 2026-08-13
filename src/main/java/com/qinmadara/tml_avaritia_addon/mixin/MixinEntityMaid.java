package com.qinmadara.tml_avaritia_addon.mixin;

import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.qinmadara.tml_avaritia_addon.util.AvaritiaWeaponUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 女仆基础能力 Mixin。
 * 扩展近战判定距离：手持无尽近战武器时 getMeleeAttackRangeSqr 放大到 64（8 格），
 * 与 MaidMeleeAttack#isWithinMeleeAttackRange 判定链路一致，保证无尽武器 8 格攻击范围。
 */
@Mixin(EntityMaid.class)
public abstract class MixinEntityMaid extends TamableAnimal implements CrossbowAttackMob, IMaid {

    private MixinEntityMaid(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Inject(method = "getMeleeAttackRangeSqr(Lnet/minecraft/world/entity/LivingEntity;)D",
            at = @At("HEAD"), cancellable = true)
    private void maidAvaritia$injectMeleeRange(LivingEntity target, CallbackInfoReturnable<Double> cir) {
        if (AvaritiaWeaponUtil.isHoldingAvaritiaMelee((EntityMaid) (Object) this)) {
            cir.setReturnValue(64.0D);
        }
    }
}
