package com.qinmadara.tml_avaritia_addon.inventory.container;

import com.github.tartaricacid.touhoulittlemaid.inventory.container.task.TaskConfigContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

/**
 * 无尽弓/弩"武器攻击模式切换"任务设置界面容器。
 * <p>
 * 结构与 TLM 自带的 DefaultMaidTaskConfigContainer 完全一致：
 * - 继承 TaskConfigContainer；
 * - TYPE 用 IMenuTypeExtension.create 从网络缓冲读取 entityId（TLM 齿轮按钮打开配置界面时
 *   通过 serverPlayer.openMenu(provider, buffer -> buffer.writeInt(id)) 写入 int entityId，
 *   DefaultMaidTaskConfigContainer 已验证此格式）；
 * - 客户端由 AvaritiaWeaponModeGui 渲染（标题 + 模式切换按钮）。
 */
public class AvaritiaWeaponModeContainer extends TaskConfigContainer {
    public static final MenuType<AvaritiaWeaponModeContainer> TYPE = IMenuTypeExtension.create(
            (windowId, inv, data) -> new AvaritiaWeaponModeContainer(windowId, inv, data.readInt()));

    public AvaritiaWeaponModeContainer(int id, Inventory inventory, int entityId) {
        super(TYPE, id, inventory, entityId);
    }
}
