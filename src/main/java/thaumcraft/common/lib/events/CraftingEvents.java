package thaumcraft.common.lib.events;
import thaumcraft.common.lib.utils.NBTHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.init.ModItems;

/**
 * CraftingEvents - Handles crafting-related events.
 * 
 * Features:
 * - Warp application when crafting warped items
 * - Research completion triggers when crafting specific items
 * - Special item crafting behaviors (labels from phials)
 * - Anvil restrictions (primordial pearl)
 * - Custom fuel burn times
 * 
 * Ported from Thaumcraft 1.12.2 to 1.20.1
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public class CraftingEvents {

    /**
     * Handle item crafting events.
     * - Apply warp for warped items
     * - Trigger research completion for crafted items
     * - Handle special label crafting from phials
     */
    @SubscribeEvent
    public static void onCrafting(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        
        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty()) return;
        
        // Apply warp for warped items
        // TODO: Add config option for wuss mode
        int warp = ThaumcraftApi.getWarp(crafted);
        if (warp > 0 && !player.level().isClientSide) {
            ThaumcraftApi.internalMethods.addWarpToPlayer(player, warp, IPlayerWarp.EnumWarpType.NORMAL);
        }
        
        // Special handling for labels crafted from phials
        // When crafting a label with aspect info, return the empty phial
        if (ModItems.LABEL_FILLED != null && crafted.getItem() == ModItems.LABEL_FILLED.get() && NBTHelper.hasTag(crafted)) {
            // Note: In 1.20.1, we can't easily modify craftMatrix during crafting
            // This would need a different approach (loot modifier or recipe remainder)
        }
        
        // Trigger research completion for crafted items
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            int stackHash = ResearchManager.createItemStackHash(crafted.copy());
            
            if (ResearchManager.craftingReferences.contains(stackHash)) {
                ResearchManager.completeResearch(player, "[#]" + stackHash);
            } else {
                // Try without NBT
                ItemStack simpleStack = new ItemStack(crafted.getItem(), crafted.getCount());
                int simpleHash = ResearchManager.createItemStackHash(simpleStack);
                
                if (ResearchManager.craftingReferences.contains(simpleHash)) {
                    ResearchManager.completeResearch(player, "[#]" + simpleHash);
                }
            }
            
            // Check item tags for research triggers
            // In 1.20.1, tags replace OreDictionary
            // This would need to be expanded based on how research is configured
        }
    }

    /**
     * Prevent primordial pearls from being used in anvils.
     * The pearl is too powerful/unique to be combined with other items.
     */
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (ModItems.PRIMORDIAL_PEARL == null) return;
        
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        
        if ((!left.isEmpty() && left.getItem() == ModItems.PRIMORDIAL_PEARL.get()) ||
            (!right.isEmpty() && right.getItem() == ModItems.PRIMORDIAL_PEARL.get())) {
            event.setCanceled(true);
        }
    }

    /**
     * Get custom burn time for Thaumcraft fuel items.
     * This is handled via IFuelHandler in 1.12.2, but in 1.20.1 
     * we should use a FuelHandler capability or data pack.
     * 
     * @param fuel The item being used as fuel
     * @return Burn time in ticks, or 0 for non-fuel items
     */
    public static int getBurnTime(ItemStack fuel) {
        if (fuel.isEmpty()) return 0;
        
        // Alumentum - powerful fuel
        if (ModItems.ALUMENTUM != null && fuel.getItem() == ModItems.ALUMENTUM.get()) {
            return 4800; // 4 minutes
        }
        
        // Greatwood log - slightly better than normal logs
        // if (fuel.getItem() == ModBlocks.LOG_GREATWOOD.get().asItem()) {
        //     return 500;
        // }
        
        // Silverwood log
        // if (fuel.getItem() == ModBlocks.LOG_SILVERWOOD.get().asItem()) {
        //     return 400;
        // }
        
        return 0;
    }
}




