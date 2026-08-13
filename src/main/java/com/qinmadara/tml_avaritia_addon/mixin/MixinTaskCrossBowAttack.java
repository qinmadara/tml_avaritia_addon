package com.qinmadara.tml_avaritia_addon.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskCrossBowAttack;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinityCrossBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 绕过 TLM 弩任务的弹药门。
 * TaskCrossBowAttack 的弹药判定在任务层
 * StartAttacking 谓词 hasCrossBow(e) && hasAmmunition(e)，
 * hasAmmunition(maid) = 副手烟花 || hasArrow(maid)，hasArrow(maid) = findArrow(maid) >= 0，
 * findArrow 通过合并背包（maid.getAvailableInv(true)）搜索受支持弹射物 → 背包无箭时女仆无法锁定目标。
 * 女仆持无尽弩时无需背包箭矢即可锁定并射击；箭矢由 TaskAvaritiaCrossBow#performRangedAttack
 * 直接生成天界箭（脱离背包，不消耗），特效为无尽弩单发/多段天界箭。本 Mixin 只负责放行弹药门。
 * TaskCrossBowAttack 为 TLM mod 类，方法名不参与 SRG 重映射，remap = false。
 */
@Mixin(TaskCrossBowAttack.class)
public abstract class MixinTaskCrossBowAttack {

    @Inject(method = "hasArrow(Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void maidAvaritia$bypassAmmoCheck(EntityMaid maid, CallbackInfoReturnable<Boolean> cir) {
        if (maid.getMainHandItem().getItem() instanceof InfinityCrossBowItem) {
            cir.setReturnValue(true);
        }
    }
}
