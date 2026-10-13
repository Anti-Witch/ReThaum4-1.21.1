package thaumcraft.common.lib.events;
import thaumcraft.common.lib.utils.NBTHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;

import java.util.HashSet;
import java.util.Set;

/**
 * PlayerEvents - Handles all player-related events for Thaumcraft.
 * 
 * Ported from 1.12.2. Key functionality:
 * - Attach capabilities to players
 * - Sync knowledge/warp when player joins
 * - Clone capabilities on death/dimension change
 * - Track research progress triggers (crystal pickup, thaumonomicon, etc.)
 * 
 * Key API changes:
 * - AttachCapabilitiesEvent<Entity> same
 * - EntityJoinWorldEvent -> EntityJoinLevelEvent
 * - PlayerEvent.Clone same
 * - EntityPlayer -> Player
 * - EntityPlayerMP -> ServerPlayer
 * - world.isRemote -> level.isClientSide
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public class PlayerEvents {
    
    // Players that need their knowledge synced
    public static final Set<String> syncList = new HashSet<>();
    
    // ==================== Capability Events ====================
    
    // NOTE: Capability attachment is handled by ThaumcraftCapabilities class
    // to avoid duplicate registration errors.
    
    /**
     * Clone capabilities when player respawns or changes dimension
     */
    @SubscribeEvent
    public static void cloneCapabilitiesEvent(PlayerEvent.Clone event) {
        try {
            // Clone knowledge
            IPlayerKnowledge oldKnowledge = ThaumcraftCapabilities.getKnowledge(event.getOriginal());
            IPlayerKnowledge newKnowledge = ThaumcraftCapabilities.getKnowledge(event.getEntity());
            if (oldKnowledge != null && newKnowledge != null) {
                CompoundTag nbtKnowledge = oldKnowledge.serializeNBT(event.getOriginal().level().registryAccess());
                newKnowledge.deserializeNBT(event.getOriginal().level().registryAccess(), nbtKnowledge);
            }
            
            // Clone warp
            IPlayerWarp oldWarp = ThaumcraftCapabilities.getWarp(event.getOriginal());
            IPlayerWarp newWarp = ThaumcraftCapabilities.getWarp(event.getEntity());
            if (oldWarp != null && newWarp != null) {
                CompoundTag nbtWarp = oldWarp.serializeNBT(event.getOriginal().level().registryAccess());
                newWarp.deserializeNBT(event.getOriginal().level().registryAccess(), nbtWarp);
            }
        } catch (Exception e) {
            Thaumcraft.LOGGER.error("Could not clone player [{}] knowledge when respawning/changing dimensions", 
                event.getOriginal().getName().getString(), e);
        }
    }
    
    // ==================== Join/Login Events ====================
    
    /**
     * Sync capabilities when player joins world.
     *
     * NOTE: this used to listen to EntityJoinLevelEvent, which fires before the
     * client is ready to receive/apply the payload, so the first sync was often
     * lost (client showed an empty research pool until something else triggered
     * a sync). PlayerLoggedInEvent fires once the player is fully in the world.
     */
    @SubscribeEvent
    public static void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
            
            if (knowledge != null) {
                // TC4: auto-unlock research (BASICTHAUMATURGY, CRUCIBLE, ...)
                // as soon as the player data has been loaded.
                ResearchManager.unlockAutoResearch(player);
                knowledge.sync(player);
            }
            if (warp != null) {
                warp.sync(player);
            }
            
            Thaumcraft.LOGGER.debug("Synced Thaumcraft data for player: {}", player.getName().getString());
        }
    }
    
    // ==================== Tick Events ====================
    
    /**
     * TC4 ItemHoverHarness/Hover: if the harness is no longer worn, stop hovering
     * and hand back the flight abilities we granted. Runs on both sides so the
     * client stops flying immediately as well.
     */
    @SubscribeEvent
    public static void hoverHarnessCheck(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof thaumcraft.common.items.armor.ItemHoverHarness)) {
            thaumcraft.common.items.armor.Hover.onHarnessRemoved(player);
        }
    }
    
    /**
     * Player tick event - handles periodic sync and warp effects
     */
    @SubscribeEvent
    public static void livingTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
                // TC4 EventHandlerEntity: restore the traveller boots step height
                // when the boots are no longer worn (unequip without a tick,
                // boots broken or dropped)
                if (!(player.getItemBySlot(EquipmentSlot.FEET).getItem()
                        instanceof thaumcraft.common.items.armor.ItemBootsTraveller)) {
                    thaumcraft.common.items.armor.ItemBootsTraveller.clearStepHeight(player);
                }

                // Periodic knowledge sync (every 20 ticks)
                if (player.tickCount % 20 == 0 && player instanceof ServerPlayer serverPlayer) {
                    String playerName = player.getName().getString();
                    if (syncList.remove(playerName)) {
                        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
                        if (knowledge != null) {
                            knowledge.sync(serverPlayer);
                        }
                    }
                }
                
                // Periodic research triggers (dimension/height-based discoveries)
                if (player.tickCount % 200 == 0) {
                    thaumcraft.common.config.ConfigResearch.checkPeriodicResearch(player);
                }
                
                // Warp effects (every 2000 ticks = ~100 seconds)
                if (player.tickCount % 2000 == 0) {
                    WarpEvents.checkWarpEvent(player);
                }
                
                // Death Gaze effect check (every 20 ticks)
                if (player.tickCount % 20 == 5) {
                    WarpEvents.checkDeathGaze(player);
                }
            }
    }
    
    // ==================== Sleep Events ====================
    
    /**
     * Handle player waking up - triggers the dream that starts Thaumcraft progression.
     * When a player sleeps for the first time after picking up a crystal, they receive
     * the "gotdream" research flag which allows them to craft a Thaumonomicon.
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Check if player has picked up a crystal but hasn't had the dream yet
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge == null) return;
        
        boolean hasPickedCrystal = knowledge.isResearchKnown("!gotcrystal");
        boolean hasDream = knowledge.isResearchKnown("!gotdream");
        
        if (hasPickedCrystal && !hasDream) {
            // Grant the dream research
            ThaumcraftApi.internalMethods.completeResearch(player, "!gotdream");
            
            // Show dream message to player
            player.displayClientMessage(
                Component.translatable("tc.dream.1"), false);
            player.displayClientMessage(
                Component.translatable("tc.dream.2"), false);
            
            Thaumcraft.LOGGER.info("Player {} received the Thaumcraft dream", player.getName().getString());
        }
    }
    
    // ==================== Item Events ====================
    
    /**
     * Handle item pickup - triggers research progression when picking up crystals.
     * This is essential for starting Thaumcraft progression.
     */
    @SubscribeEvent
    public static void onItemPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide) {
            return;
        }
        
        ItemStack stack = event.getOriginalStack();
        if (stack.isEmpty()) return;
        
        Item item = stack.getItem();
        
        // Check if it's a crystal item (primal shard item or crystal block)
        boolean isCrystal = false;
        
        // Check for primal shards (TC4 crystal cluster drops)
        if (item == ModItems.SHARD_AIR.get() ||
                 item == ModItems.SHARD_FIRE.get() ||
                 item == ModItems.SHARD_WATER.get() ||
                 item == ModItems.SHARD_EARTH.get() ||
                 item == ModItems.SHARD_ORDER.get() ||
                 item == ModItems.SHARD_ENTROPY.get()) {
            isCrystal = true;
        }
        // Check for crystal block items
        else if (item == ModBlocks.CRYSTAL_AIR.get().asItem() ||
                 item == ModBlocks.CRYSTAL_FIRE.get().asItem() ||
                 item == ModBlocks.CRYSTAL_WATER.get().asItem() ||
                 item == ModBlocks.CRYSTAL_EARTH.get().asItem() ||
                 item == ModBlocks.CRYSTAL_ORDER.get().asItem() ||
                 item == ModBlocks.CRYSTAL_ENTROPY.get().asItem()) {
            isCrystal = true;
        }
        
        if (isCrystal) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge != null && !knowledge.isResearchKnown("!gotcrystal")) {
                // Grant crystal pickup research
                ThaumcraftApi.internalMethods.completeResearch(player, "!gotcrystal");
                
                // Show hint message
                player.displayClientMessage(
                    Component.translatable("tc.crystal.pickup"), true);
                
                Thaumcraft.LOGGER.info("Player {} picked up first crystal - sleep to receive dream!", 
                    player.getName().getString());
            }
        }
        
        // Check for thaumonomicon pickup
        if (item == ModItems.THAUMONOMICON.get()) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge != null && !knowledge.isResearchKnown("!gotthaumonomicon")) {
                ThaumcraftApi.internalMethods.completeResearch(player, "!gotthaumonomicon");
                Thaumcraft.LOGGER.info("Player {} picked up Thaumonomicon - research unlocked!", 
                    player.getName().getString());
            }
        }
    }
    
    /**
     * Track who threw an item (for item-specific mechanics)
     */
    @SubscribeEvent
    public static void droppedItem(ItemTossEvent event) {
        ItemEntity itemEntity = event.getEntity();
        CompoundTag itemData = itemEntity.getPersistentData();
        itemData.putString("thrower", event.getPlayer().getName().getString());
    }
    
    // ==================== Tooltips ====================

    /**
     * TC4 EventHandlerRunic.tooltipEvent: IWarpingGear items list their warp
     * value ("Warping +N") in the tooltip.
     */
    @SubscribeEvent
    public static void onItemTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        int warp = getFinalWarp(event.getItemStack(), event.getEntity());
        if (warp > 0) {
            event.getToolTip().add(Component.translatable("item.warping")
                    .append(" " + warp)
                    .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        }
    }

    // ==================== Utility Methods ====================
    
    /**
     * Mark a player for knowledge sync on next tick
     */
    public static void markForSync(Player player) {
        syncList.add(player.getName().getString());
    }
    
    /**
     * Get runic charge from an item
     */
    public static int getRunicCharge(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty() || !NBTHelper.hasTag(stack)) {
            return 0;
        }
        CompoundTag tag = NBTHelper.getTag(stack);
        if (tag != null && tag.contains("TC.RUNIC")) {
            return tag.getByte("TC.RUNIC");
        }
        return 0;
    }
    
    /**
     * Get warp value from an item
     */
    public static int getFinalWarp(net.minecraft.world.item.ItemStack stack, Player player) {
        if (stack.isEmpty()) {
            return 0;
        }
        int warp = 0;
        
        // Check for IWarpingGear interface
        if (stack.getItem() instanceof thaumcraft.api.items.IWarpingGear warpingGear) {
            warp += warpingGear.getWarp(stack, player);
        }
        
        // Check for NBT warp tag
        if (NBTHelper.hasTag(stack)) {
            CompoundTag tag = NBTHelper.getTag(stack);
            if (tag != null && tag.contains("TC.WARP")) {
                warp += tag.getByte("TC.WARP");
            }
        }
        
        return warp;
    }
    
    /**
     * Get vis discount from an item
     */
    public static int getFinalDiscount(net.minecraft.world.item.ItemStack stack, Player player) {
        if (stack.isEmpty() || !(stack.getItem() instanceof thaumcraft.api.items.IVisDiscountGear gear)) {
            return 0;
        }
        return gear.getVisDiscount(stack, player);
    }
}




