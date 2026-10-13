package thaumcraft.client.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.client.lib.AspectRenderer;
import thaumcraft.common.items.tools.ItemThaumometer;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketScanItemToServer;
import thaumcraft.common.lib.research.ScanGeneric;

import java.util.ArrayList;
import java.util.List;

/**
 * Thaumometer client handler (game event bus).
 * <p>
 * - Suppresses the vanilla empty offhand arm while the scanner renderer draws
 *   both supporting arms in first person (TC4 look).
 * - "Compact inventory scanning" (port feature, not present in TC4): while the
 *   player holds a thaumometer and hovers over an item in any container GUI,
 *   the item's aspects are shown next to the cursor and the item is scanned
 *   after a short hover, without throwing it on the ground.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class ThaumometerClientHandler {

    /** Hover time before the inventory scan is sent (1 second, like the world hold). */
    private static final int SCAN_HOVER_TICKS = 20;
    private static final int ASPECTS_PER_ROW = 5;

    private static ItemStack hoveredStack = ItemStack.EMPTY;
    private static int hoverTicks = 0;
    private static boolean scanSentForHovered = false;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.OFF_HAND || !event.getItemStack().isEmpty()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        // The thaumometer item renderer draws both cupped arms (TC4); without
        // this the vanilla empty offhand arm would be a third arm.
        if (player.getMainHandItem().getItem() instanceof ItemThaumometer) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !holdsThaumometer(player)) {
            reset();
            return;
        }
        Slot slot = getHoveredSlot(mc.screen);
        ItemStack stack = slot == null ? ItemStack.EMPTY : slot.getItem();
        if (stack.isEmpty()) {
            reset();
            return;
        }
        if (!ItemStack.isSameItemSameComponents(stack, hoveredStack)) {
            hoveredStack = stack.copy();
            hoverTicks = 0;
            scanSentForHovered = false;
            return;
        }
        if (scanSentForHovered) {
            return;
        }
        // Already scanned objects have nothing new to teach (TC4
        // ScanManager.isValidScanTarget) - do not spam the server.
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        String key = ScanGeneric.getScanKey(player, stack);
        if (knowledge != null && key != null && knowledge.isScanned(key)) {
            scanSentForHovered = true;
            return;
        }
        hoverTicks++;
        if (hoverTicks >= SCAN_HOVER_TICKS) {
            scanSentForHovered = true;
            PacketHandler.sendToServer(new PacketScanItemToServer(stack.copyWithCount(1)));
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !holdsThaumometer(player)) {
            return;
        }
        Slot slot = getHoveredSlot(event.getScreen());
        if (slot == null || !slot.hasItem()) {
            return;
        }
        ItemStack stack = slot.getItem();

        AspectList aspects = null;
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        String key = ScanGeneric.getScanKey(player, stack);
        if (knowledge != null && key != null && knowledge.isScanned(key)) {
            aspects = AspectHelper.getObjectAspects(stack);
        }

        renderInfoPanel(event.getGuiGraphics(), mc, stack, aspects,
                event.getMouseX(), event.getMouseY(),
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    /**
     * Compact panel next to the cursor: the item name and, once the item type
     * has been scanned, its aspect icons with amounts.
     */
    private static void renderInfoPanel(GuiGraphics graphics, Minecraft mc, ItemStack stack,
                                        AspectList aspects, int mouseX, int mouseY,
                                        int screenWidth, int screenHeight) {
        Component name = stack.getHoverName();
        List<Aspect> list = new ArrayList<>();
        if (aspects != null) {
            for (Aspect aspect : aspects.getAspectsSorted()) {
                if (aspect != null && aspects.getAmount(aspect) > 0) {
                    list.add(aspect);
                }
            }
        }

        int rows = Math.max(1, (list.size() + ASPECTS_PER_ROW - 1) / ASPECTS_PER_ROW);
        int iconsWidth = Math.min(Math.max(list.size(), 1), ASPECTS_PER_ROW) * 18 - 2;
        int panelWidth = Math.max(mc.font.width(name), iconsWidth) + 6;
        int panelHeight = 12 + rows * 18 + 2;

        int x = net.minecraft.util.Mth.clamp(mouseX + 12, 2, screenWidth - panelWidth - 2);
        int y = net.minecraft.util.Mth.clamp(mouseY - 12, 2, screenHeight - panelHeight - 2);

        // TC4-style dark purple tooltip background
        graphics.fill(x - 3, y - 3, x + panelWidth + 3, y + panelHeight + 3, 0xC0100010);
        graphics.drawString(mc.font, name, x, y, 0xFFFFFF, true);

        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                Aspect aspect = list.get(i);
                int ix = x + (i % ASPECTS_PER_ROW) * 18;
                int iy = y + 12 + (i / ASPECTS_PER_ROW) * 18;
                AspectRenderer.drawAspect(graphics, ix, iy, aspect, aspects.getAmount(aspect));
            }
        }
    }

    private static boolean holdsThaumometer(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof ItemThaumometer
                || player.getOffhandItem().getItem() instanceof ItemThaumometer;
    }

    private static Slot getHoveredSlot(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            return containerScreen.getSlotUnderMouse();
        }
        return null;
    }

    private static void reset() {
        hoveredStack = ItemStack.EMPTY;
        hoverTicks = 0;
        scanSentForHovered = false;
    }
}
