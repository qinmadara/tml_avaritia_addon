package com.qinmadara.tml_avaritia_addon.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskAttack;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.qinmadara.tml_avaritia_addon.util.AvaritiaWeaponUtil;
import com.qinmadara.tml_avaritia_addon.util.MaidTaskConfigUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

/**
 * 女仆无尽之剑攻击任务。
 *
 * 直接继承 TLM 自带 TaskAttack，完整复用其索敌 / 追击 / 停止 / 近战攻击 / 盾牌逻辑，
 * 仅把“可用武器”收窄为无尽之剑。无尽之剑特效与 AOE 由 MaidHurtTarget.Pre 事件处理器触发，
 * 并且事件处理器只在本任务下生效。
 */
public class TaskAvaritia extends TaskAttack {

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
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        // 只接受无尽之剑；远程武器（弓/弩/三叉戟）由各自的专属任务接管
        return AvaritiaWeaponUtil.isAvaritiaMelee(stack);
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
}
