package thaumcraft.client.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.client.renderers.SealRenderer;

/**
 * ClientRenderEvents - Handles client-side rendering events.
 * 
 * Responsibilities:
 * - Render seals when player holds ISealDisplayer items
 * - Other visual effects (to be added)
 * 
 * Ported from 1.12.2 RenderEventHandler.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientRenderEvents {
    
    /**
     * Called during world rendering to add custom world visuals.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Only render during the AFTER_TRANSLUCENT_BLOCKS stage for proper layering
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null) return;
        
        // Check if player is holding an ISealDisplayer item
        if (isHoldingSealDisplayer(player)) {
            SealRenderer.renderSeals(
                event.getPoseStack(),
                event.getPartialTick().getGameTimeDeltaPartialTick(true),
                player
            );
        }
    }
    
    /**
     * Check if the player is holding an item that implements ISealDisplayer.
     * This includes seal items and the golem bell.
     */
    private static boolean isHoldingSealDisplayer(Player player) {
        // Check main hand
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof ISealDisplayer) {
            return true;
        }
        
        // Check off hand
        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty() && offHand.getItem() instanceof ISealDisplayer) {
            return true;
        }
        
        return false;
    }
}




