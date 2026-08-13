package com.qinmadara.tml_avaritia_addon.network;

import com.qinmadara.tml_avaritia_addon.network.message.SetMaidWeaponModeMessage;
import com.qinmadara.tml_avaritia_addon.tml_avaritia_addon;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 本附属的 Forge 简单网络通道。
 * 用于女仆任务设置界面（MaidTaskConfigGui）模式开关的 C2S 消息
 * （SetMaidWeaponModeMessage：翻转女仆主手无尽武器 mode NBT）。
 */
public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(tml_avaritia_addon.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private NetworkHandler() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, SetMaidWeaponModeMessage.class,
                SetMaidWeaponModeMessage::encode,
                SetMaidWeaponModeMessage::decode,
                SetMaidWeaponModeMessage::handle);
    }
}
