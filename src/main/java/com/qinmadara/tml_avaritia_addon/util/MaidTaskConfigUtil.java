package com.qinmadara.tml_avaritia_addon.util;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.task.DefaultMaidTaskConfigContainer;
import com.qinmadara.tml_avaritia_addon.inventory.container.AvaritiaWeaponModeContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * 女仆任务配置界面工具。
 * 对齐 TLM IMaidTask#getTaskConfigGuiProvider 的默认实现（返回 DefaultMaidTaskConfigContainer），
 * 由 TLM 的 InitContainerGui 已注册的 MenuScreens 映射自动打开空配置界面（剑/三叉戟用）。
 * weaponModeConfig 返回本附属自定义容器 AvaritiaWeaponModeContainer（已由 ModContainers 注册
 * MenuType + ModClientInit 注册 MenuScreens → 客户端 AvaritiaWeaponModeGui，弓/弩用）。
 * DefaultMaidTaskConfigGui extends MaidTaskConfigGui<TaskConfigContainer>。
 */
public final class MaidTaskConfigUtil {

    private MaidTaskConfigUtil() {
    }

    /** 返回默认（空）任务配置界面 MenuProvider（对齐 IMaidTask 默认实现，避免 4 个任务重复匿名类） */
    public static MenuProvider emptyTaskConfig(EntityMaid maid) {
        final int entityId = maid.getId();
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Maid Task Config Container");
            }

            @Override
            public AbstractMaidContainer createMenu(int index, Inventory playerInventory, Player player) {
                return new DefaultMaidTaskConfigContainer(index, playerInventory, entityId);
            }
        };
    }

    /** 返回"武器攻击模式切换"任务设置界面 MenuProvider（无尽弓/弩专用：AvaritiaWeaponModeContainer → 客户端 AvaritiaWeaponModeGui） */
    public static MenuProvider weaponModeConfig(EntityMaid maid) {
        final int entityId = maid.getId();
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Avaritia Weapon Mode Config Container");
            }

            @Override
            public AbstractMaidContainer createMenu(int index, Inventory playerInventory, Player player) {
                return new AvaritiaWeaponModeContainer(index, playerInventory, entityId);
            }
        };
    }
}
