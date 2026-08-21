package com.qinmadara.tml_avaritia_addon.network.message;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.qinmadara.tml_avaritia_addon.tml_avaritia_addon;
import com.qinmadara.tml_avaritia_addon.util.AvaritiaWeaponUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S —— 切换女仆主手无尽武器模式（弓追踪 / 弩多段）。
 * 服务端按主手武器白名单校验后，复制栈并翻转 mode 子标签布尔
 * （与玩家版 ISwitchable#switchMode 语义一致：putBoolean(mode, !getBoolean(mode))），
 * 再 setItemInHand 写回（持久化 + 触发实体装备同步回客户端）。
 *
 * 1.21.1 修复：mode NBT 的写入必须走 AvaritiaWeaponUtil#setMode（先改后写）。
 * 此前使用的 ItemUtils#getOrCreateChildTag 在返回前把 CUSTOM_DATA 以拷贝形式写回，
 * 之后再 putBoolean 改动的是组件外副本，翻转永远不会生效（按钮失效根因）。
 */
public record SetMaidWeaponModeMessage(int maidId, String mode) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetMaidWeaponModeMessage> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(tml_avaritia_addon.MODID, "set_maid_weapon_mode"));

    public static final StreamCodec<ByteBuf, SetMaidWeaponModeMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SetMaidWeaponModeMessage::maidId,
            ByteBufCodecs.STRING_UTF8, SetMaidWeaponModeMessage::mode,
            SetMaidWeaponModeMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetMaidWeaponModeMessage msg, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                if (player.level() instanceof ServerLevel serverLevel
                        && serverLevel.getEntity(msg.maidId) instanceof EntityMaid maid) {
                    ItemStack stack = maid.getMainHandItem();
                    // 主手武器白名单校验（防伪造包，仅允许对应武器的合法模式键）
                    if (AvaritiaWeaponUtil.isModeSwitchable(stack, msg.mode)) {
                        ItemStack modified = stack.copy();
                        AvaritiaWeaponUtil.setMode(modified, msg.mode, !AvaritiaWeaponUtil.isModeActive(modified, msg.mode));
                        maid.setItemInHand(InteractionHand.MAIN_HAND, modified);
                    }
                }
            });
        }
    }
}
