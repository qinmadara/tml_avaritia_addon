package com.qinmadara.tml_avaritia_addon.client;

import com.qinmadara.tml_avaritia_addon.client.gui.entity.maid.task.AvaritiaWeaponModeGui;
import com.qinmadara.tml_avaritia_addon.inventory.container.AvaritiaWeaponModeContainer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端初始化——把 AvaritiaWeaponModeContainer 绑定到 AvaritiaWeaponModeGui。
 * <p>
 * 对齐 TLM InitContainerGui 的 @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD) 模式，
 * FMLClientSetupEvent 里用 MenuScreens.register 注册容器→界面的映射。
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModClientInit {
    private ModClientInit() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent evt) {
        evt.enqueueWork(() -> MenuScreens.register(AvaritiaWeaponModeContainer.TYPE, AvaritiaWeaponModeGui::new));
    }
}
