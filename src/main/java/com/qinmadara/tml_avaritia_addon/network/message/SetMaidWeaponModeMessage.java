package com.qinmadara.tml_avaritia_addon.network.message;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.qinmadara.tml_avaritia_addon.util.AvaritiaWeaponUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S —— 切换女仆主手无尽武器模式（弓追踪 / 弩多段）。
 * 服务端按主手武器白名单校验后，复制栈并翻转 mode 子标签布尔
 * （与玩家版 ISwitchable#switchMode 语义一致：putBoolean(mode, !getBoolean(mode))），
 * 再 setItemInHand 写回（持久化 + 触发实体装备同步回客户端）。
 */
public class SetMaidWeaponModeMessage {

    private final int maidId;
    private final String mode;

    public SetMaidWeaponModeMessage(int maidId, String mode) {
        this.maidId = maidId;
        this.mode = mode;
    }

    public static void encode(SetMaidWeaponModeMessage msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.maidId);
        buf.writeUtf(msg.mode);
    }

    public static SetMaidWeaponModeMessage decode(FriendlyByteBuf buf) {
        return new SetMaidWeaponModeMessage(buf.readInt(), buf.readUtf());
    }

    public static void handle(SetMaidWeaponModeMessage msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (player.level() instanceof ServerLevel serverLevel
                    && serverLevel.getEntity(msg.maidId) instanceof EntityMaid maid) {
                ItemStack stack = maid.getMainHandItem();
                // 主手武器白名单校验（防伪造包，仅允许对应武器的合法模式键）
                if (AvaritiaWeaponUtil.isModeSwitchable(stack, msg.mode)) {
                    ItemStack modified = stack.copy();
                    CompoundTag modeTag = modified.getOrCreateTagElement("mode");
                    modeTag.putBoolean(msg.mode, !modeTag.getBoolean(msg.mode));
                    maid.setItemInHand(InteractionHand.MAIN_HAND, modified);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
