package com.qinmadara.tml_avaritia_addon.network;

import com.qinmadara.tml_avaritia_addon.network.message.SetMaidWeaponModeMessage;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 本附属的 NeoForge 网络注册。
 * 用于女仆任务设置界面（MaidTaskConfigGui）模式开关的 C2S 消息
 * （SetMaidWeaponModeMessage：翻转女仆主手无尽武器 mode NBT）。
 */
public final class NetworkHandler {

    private static final String VERSION = "1";

    private NetworkHandler() {
    }

    public static void registerPacket(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(SetMaidWeaponModeMessage.TYPE, SetMaidWeaponModeMessage.STREAM_CODEC, SetMaidWeaponModeMessage::handle);
    }
}
