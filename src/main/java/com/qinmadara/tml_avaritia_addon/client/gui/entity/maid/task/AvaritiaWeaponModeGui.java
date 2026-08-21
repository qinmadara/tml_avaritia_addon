package com.qinmadara.tml_avaritia_addon.client.gui.entity.maid.task;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.task.MaidTaskConfigGui;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.qinmadara.tml_avaritia_addon.inventory.container.AvaritiaWeaponModeContainer;
import com.qinmadara.tml_avaritia_addon.network.message.SetMaidWeaponModeMessage;
import com.qinmadara.tml_avaritia_addon.util.AvaritiaWeaponUtil;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinityBowItem;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinityCrossBowItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 无尽弓/弩的"武器攻击模式切换"任务设置界面。
 * <p>
 * 只包含：一个空背景 + 一个标题 + 一个模式切换按钮（界面内顶部居中）。
 * - 按钮按女仆主手武器显示：弓→"追踪模式：开/关"；弩→"多段模式：开/关"；
 * - 点击发 C2S SetMaidWeaponModeMessage（服务端翻转主手武器 mode NBT 并同步），并乐观翻转按钮文字；
 * - 剑/三叉戟走 TLM 空配置界面（DefaultMaidTaskConfigGui），不经过本界面。
 * <p>
 * 保留女仆主界面框架（super.renderBg），仅用极淡的柔和半透明
 * 深色（0x4D1B1B23，约 30%）覆盖任务设置内容区（leftPos+81, topPos+28, 176x137），
 * 让自然背景透出、不突兀，同时不超出任务设置界面。
 * 标题用白色（0xFFFFFF），标题与按钮均以内容区水平中心（leftPos+169）居中：
 * 标题 topPos+36（内容区顶部 +8），按钮 topPos+62（宽 100 → x=leftPos+119）。
 */
public class AvaritiaWeaponModeGui extends MaidTaskConfigGui<AvaritiaWeaponModeContainer> {
    // 任务设置内容区（对齐 MaidsoulKitchen visualZone 参考：leftPos+81, topPos+28, 176x137）
    private static final int ZONE_X = 81;
    private static final int ZONE_Y = 28;
    private static final int ZONE_WIDTH = 176;
    private static final int ZONE_HEIGHT = 137;
    private static final int TITLE_START_Y = 8;   // 标题距内容区顶部

    // 内容区水平中心 = leftPos + 81 + 176/2 = leftPos + 169；按钮宽 100 → x=leftPos+119；标题下方 → y=topPos+62
    private static final int BUTTON_X = 119;
    private static final int BUTTON_Y = 62;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;

    public AvaritiaWeaponModeGui(AvaritiaWeaponModeContainer container, Inventory inv, Component titleIn) {
        super(container, inv, titleIn);
    }

    @Override
    protected void initAdditionWidgets() {
        super.initAdditionWidgets();
        EntityMaid maid = this.getMaid();
        if (maid == null) {
            return;
        }
        ItemStack mainHand = maid.getMainHandItem();
        if (mainHand.getItem() instanceof InfinityBowItem) {
            this.addRenderableWidget(makeModeButton(maid, mainHand, "infinity_bow_tracer", "gui.tml_avaritia_addon.mode.bow_tracer"));
        } else if (mainHand.getItem() instanceof InfinityCrossBowItem) {
            this.addRenderableWidget(makeModeButton(maid, mainHand, "infinity_crossbow_multi", "gui.tml_avaritia_addon.mode.crossbow_multi"));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        super.renderBg(graphics, partialTicks, x, y);
        // 柔和半透明背景：仅覆盖任务设置内容区（leftPos+81, topPos+28, 176x137），
        // 让女仆主界面背景自然透出（MaidsoulKitchen 风格），不超出任务设置界面
        graphics.fill(leftPos + ZONE_X, topPos + ZONE_Y, leftPos + ZONE_X + ZONE_WIDTH, topPos + ZONE_Y + ZONE_HEIGHT, 0x4D1B1B23);
    }

    @Override
    protected void renderAddition(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderAddition(graphics, mouseX, mouseY, partialTicks);
        // 标题：内容区顶部居中（MaidsoulKitchen 风格白色 0xFFFFFF，中心 leftPos+169，y=topPos+36）
        graphics.drawCenteredString(font, Component.translatable("gui.tml_avaritia_addon.weapon_mode.title"),
                leftPos + ZONE_X + ZONE_WIDTH / 2, topPos + ZONE_Y + TITLE_START_Y, 0xFFFFFF);
    }

    private Button makeModeButton(EntityMaid maid, ItemStack stack, String mode, String labelKey) {
        boolean active = AvaritiaWeaponUtil.isModeActive(stack, mode);
        Button button = Button.builder(
                        modeLabel(labelKey, active),
                        b -> {
                            // 发送 C2S 消息 → 服务端翻转女仆主手武器 mode NBT 并同步
                            PacketDistributor.sendToServer(new SetMaidWeaponModeMessage(maid.getId(), mode));
                            // 乐观翻转按钮文字（服务端同步后重开界面自动校正）
                            b.setMessage(modeLabel(labelKey, !AvaritiaWeaponUtil.isModeActive(maid.getMainHandItem(), mode)));
                        })
                .bounds(this.leftPos + BUTTON_X, this.topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        return button;
    }

    private static Component modeLabel(String labelKey, boolean active) {
        return Component.translatable(labelKey,
                Component.translatable(active ? "gui.tml_avaritia_addon.mode.on" : "gui.tml_avaritia_addon.mode.off"));
    }
}
