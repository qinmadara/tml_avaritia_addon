package com.qinmadara.tml_avaritia_addon.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskBowAttack;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinityBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 绕过 TLM 弓任务的弹药门。
 * 箭矢本体由 TaskAvaritiaBow#performRangedAttack 覆盖提供（脱离背包的无限箭源），
 * 特效由 MixinInfinityBowItem#customArrow 触发，本 Mixin 只负责放行弹药门。
 * TaskBowAttack 为 TLM mod 类，方法名不参与 SRG 重映射，remap = false。
 */
@Mixin(TaskBowAttack.class)
public abstract class MixinTaskBowAttack {

    @Inject(method = "hasArrow(Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void maidAvaritia$bypassAmmoCheck(EntityMaid maid, CallbackInfoReturnable<Boolean> cir) {
        if (maid.getMainHandItem().getItem() instanceof InfinityBowItem) {
            cir.setReturnValue(true);
        }
    }
}
