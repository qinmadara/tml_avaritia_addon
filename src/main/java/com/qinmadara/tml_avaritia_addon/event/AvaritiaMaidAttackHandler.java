package com.qinmadara.tml_avaritia_addon.event;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidHurtTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.qinmadara.tml_avaritia_addon.tml_avaritia_addon;
import com.qinmadara.tml_avaritia_addon.util.AvaritiaMaidAttackHelper;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinitySwordItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 女仆近战攻击事件处理器。
 * 订阅 TLM 官方事件 MaidHurtTarget.Pre（@Cancelable，@ApiStatus.AvailableSince("1.4.0")）。
 * 该事件只由 EntityMaid#doHurtTarget 发出 → 其他非玩家生物天然无法触发无尽武器特效。
 */
@Mod.EventBusSubscriber(modid = tml_avaritia_addon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AvaritiaMaidAttackHandler {

    private AvaritiaMaidAttackHandler() {
    }

    @SubscribeEvent
    public static void onMaidHurtTarget(MaidHurtTarget.Pre event) {
        EntityMaid maid = event.getMaid();
        Entity target = event.getTarget();
        ItemStack stack = maid.getMainHandItem();

        // 无尽之剑：复刻 onLeftClickEntity 完整特效（单目标 + 横扫 + 斩杀/PVP 无尽甲爆炸）
        if (stack.getItem() instanceof InfinitySwordItem sword) {
            AvaritiaMaidAttackHelper.infinitySwordAttack(maid, target, stack, sword);
            // 取消原版属性伤害，避免双倍伤害；EntityMaid#doHurtTarget 将直接 return true，AI 冷却正常
            event.setCanceled(true);
        }
        // 无尽斧的破盾特效（InfinityAxeItem#onLeftClickEntity）后续按同一模式扩展
    }
}
