package com.qinmadara.tml_avaritia_addon.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.qinmadara.tml_avaritia_addon.util.AvaritiaWeaponUtil;
import committee.nova.mods.avaritia.common.entity.arrow.HeavenArrowEntity;
import committee.nova.mods.avaritia.common.entity.arrow.TraceArrowEntity;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinityBowItem;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 无尽弓远程特效。
 * ProjectileUtil.getMobArrow -> ((BowItem)item).customArrow(arrow) -> addFreshEntity。
 * @Mixin 目标必须为 BowItem（Mixin 无法对未在目标类声明的方法生成 refmap/混淆映射），
 * 方法体内用 instanceof InfinityBowItem 门控，确保只有女仆持无尽弓时才替换箭矢。
 */
@Mixin(BowItem.class)
public abstract class MixinInfinityBowItem {

    // remap=false：customArrow 在 namedToIntermediate.tsrg 中无映射条目（实测确认），
    // 说明该方法在 SRG 环境下保持原名 customArrow，无需 refmap 翻译。
    @Inject(method = "customArrow(Lnet/minecraft/world/entity/projectile/AbstractArrow;)Lnet/minecraft/world/entity/projectile/AbstractArrow;", at = @At("HEAD"), cancellable = true, remap = false)
    private void maidAvaritia$replaceArrow(AbstractArrow arrow, CallbackInfoReturnable<AbstractArrow> cir) {
        // 仅女仆射出的箭生效；其他生物（骷髅等）射弓仍走原逻辑（普通箭）
        if (!(arrow.getOwner() instanceof EntityMaid maid)) {
            return;
        }
        ItemStack stack = maid.getMainHandItem();
        // 仅无尽之弓生效：女仆手持原版弓等其他弓时，不替换箭矢
        if (!(stack.getItem() instanceof InfinityBowItem)) {
            return;
        }
        // 追踪模式（SwitchMode=infinity_bow_tracer）-> 追踪箭；默认 -> 天界箭（命中 500 伤害 + 50 子箭弹幕）
        AbstractArrow replaced = AvaritiaWeaponUtil.isModeActive(stack, "infinity_bow_tracer")
                ? new TraceArrowEntity(maid)
                : new HeavenArrowEntity(maid);
        replaced.setPos(maid.getX(), maid.getEyeY() - 0.1D, maid.getZ());
        replaced.setBaseDamage(arrow.getBaseDamage() * 5000.0D);
        replaced.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        cir.setReturnValue(replaced);
    }
}
