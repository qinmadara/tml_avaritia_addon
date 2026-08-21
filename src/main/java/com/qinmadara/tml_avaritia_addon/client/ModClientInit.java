package com.qinmadara.tml_avaritia_addon.client;

import com.qinmadara.tml_avaritia_addon.client.gui.entity.maid.task.AvaritiaWeaponModeGui;
import com.qinmadara.tml_avaritia_addon.init.ModContainers;
import com.qinmadara.tml_avaritia_addon.tml_avaritia_addon;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 客户端初始化——把 AvaritiaWeaponModeContainer 绑定到 AvaritiaWeaponModeGui。
 * <p>
 * 对齐 TLM InitContainerGui 的 @EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD) 模式，
 * RegisterMenuScreensEvent 里用 event.register 注册容器→界面的映射（1.21.1 起替代 FMLClientSetupEvent + MenuScreens.register）。
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = tml_avaritia_addon.MODID)
public final class ModClientInit {
    private ModClientInit() {
    }

    @SubscribeEvent
    public static void clientSetup(RegisterMenuScreensEvent event) {
        event.register(ModContainers.AVARITIA_WEAPON_MODE.get(), AvaritiaWeaponModeGui::new);
    }
}
