package com.qinmadara.tml_avaritia_addon;

import com.mojang.logging.LogUtils;
import com.qinmadara.tml_avaritia_addon.init.ModContainers;
import com.qinmadara.tml_avaritia_addon.network.NetworkHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(tml_avaritia_addon.MODID)
public class tml_avaritia_addon
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "tml_avaritia_addon";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public tml_avaritia_addon()
    {
        // 注册通用配置（Forge 会在 config 目录生成 tml_avaritia_addon-common.toml）
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 注册网络通道（女仆任务设置界面模式开关 C2S 消息）
        NetworkHandler.register();

        // 注册容器 MenuType（无尽弓/弩"武器攻击模式切换"任务设置界面）
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModContainers.CONTAINER_TYPES.register(modEventBus);

        // 其余模块均通过注解自动注册：
        // - TLM 扩展入口 AvaritiaMaidExtension（@LittleMaidExtension 由 FML 自动扫描实例化）
        // - AvaritiaMaidAttackHandler 等事件处理器（@Mod.EventBusSubscriber(Bus.FORGE)）

        LOGGER.info("{} initialized.", MODID);
    }
}
