package com.qinmadara.tml_avaritia_addon.init;

import com.qinmadara.tml_avaritia_addon.inventory.container.AvaritiaWeaponModeContainer;
import com.qinmadara.tml_avaritia_addon.tml_avaritia_addon;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本附属的容器（MenuType）注册。
 * <p>
 * 对齐 TLM InitContainer 的 DeferredRegister 模式（Registries.MENU）。
 * 需在主类构造器里调用 ModContainers.CONTAINER_TYPES.register(modEventBus) 挂到 mod 事件总线，
 * 客户端再用 RegisterMenuScreensEvent 绑定 GUI（见 ModClientInit）。
 */
public final class ModContainers {
    public static final DeferredRegister<MenuType<?>> CONTAINER_TYPES = DeferredRegister.create(
            Registries.MENU, tml_avaritia_addon.MODID);

    /** 无尽弓/弩"武器攻击模式切换"任务设置界面容器 */
    public static final DeferredHolder<MenuType<?>, MenuType<AvaritiaWeaponModeContainer>> AVARITIA_WEAPON_MODE = CONTAINER_TYPES.register(
            "avaritia_weapon_mode_container", () -> AvaritiaWeaponModeContainer.TYPE);

    private ModContainers() {
    }
}
