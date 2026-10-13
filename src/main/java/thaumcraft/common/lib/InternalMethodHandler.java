package thaumcraft.common.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge.EnumKnowledgeType;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.IPlayerWarp.EnumWarpType;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.api.internal.IInternalMethodHandler;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * InternalMethodHandler - The real implementation of IInternalMethodHandler.
 * 
 * This is set as ThaumcraftApi.internalMethods during mod initialization,
 * replacing the DummyInternalMethodHandler.
 * 
 * Provides access to all internal Thaumcraft systems:
 * - Research/Knowledge management
 * - Warp system
 * - Aura (vis/flux) manipulation
 * - Golem seals and tasks
 * - Aspect tag generation
 * 
 * Ported from 1.12.2 with 1.20.1 API changes.
 */
public class InternalMethodHandler implements IInternalMethodHandler {
    
    // ==================== Knowledge & Research ====================
    
    @Override
    public boolean addKnowledge(Player player, EnumKnowledgeType type, ResearchCategory category, int amount) {
        if (amount == 0 || player.level().isClientSide()) {
            return false;
        }
        return ResearchManager.addKnowledge(player, type, category, amount);
    }
    
    @Override
    public boolean progressResearch(Player player, String researchKey) {
        if (researchKey == null || player.level().isClientSide()) {
            return false;
        }
        return ResearchManager.progressResearch(player, researchKey);
    }
    
    @Override
    public boolean completeResearch(Player player, String researchKey) {
        if (researchKey == null || player.level().isClientSide()) {
            return false;
        }
        return ResearchManager.completeResearch(player, researchKey);
    }
    
    @Override
    public boolean doesPlayerHaveRequisites(Player player, String researchKey) {
        return ResearchManager.doesPlayerHaveRequisites(player, researchKey);
    }
    
    // ==================== Warp System ====================
    
    @Override
    public void addWarpToPlayer(Player player, int amount, EnumWarpType type) {
        if (amount == 0 || player.level().isClientSide()) {
            return;
        }
        
        IPlayerWarp pw = ThaumcraftCapabilities.getWarp(player);
        if (pw == null) {
            return;
        }
        
        int cur = pw.get(type);
        // Don't let warp go negative
        if (amount < 0 && cur + amount < 0) {
            amount = -cur;
        }
        
        pw.add(type, amount);
        
        // Send packet to client for visual feedback
        if (player instanceof ServerPlayer serverPlayer) {
            byte typeId = switch (type) {
                case PERMANENT -> (byte) 0;
                case NORMAL -> (byte) 1;
                case TEMPORARY -> (byte) 2;
            };
            PacketHandler.sendToPlayer(new PacketWarpMessage(player, typeId, amount), serverPlayer);
        }
        
        // Update warp counter (for warp effects)
        if (amount > 0) {
            int totalWarp = pw.get(EnumWarpType.TEMPORARY) + pw.get(EnumWarpType.PERMANENT) + pw.get(EnumWarpType.NORMAL);
            pw.setCounter(totalWarp);
        }
        
        // Unlock WARP research when player gets non-temp warp for the first time
        if (type != EnumWarpType.TEMPORARY 
                && ThaumcraftCapabilities.knowsResearchStrict(player, "BASICTHAUMATURGY") 
                && !ThaumcraftCapabilities.knowsResearchStrict(player, "WARP")) {
            completeResearch(player, "WARP");
            player.displayClientMessage(Component.translatable("research.WARP.warn"), true);
        }
        
        // Sync warp data to client
        if (player instanceof ServerPlayer serverPlayer) {
            pw.sync(serverPlayer);
        }
    }
    
    @Override
    public int getActualWarp(Player player) {
        IPlayerWarp wc = ThaumcraftCapabilities.getWarp(player);
        if (wc == null) {
            return 0;
        }
        return wc.get(EnumWarpType.NORMAL) + wc.get(EnumWarpType.PERMANENT);
    }
    
    // ==================== Aspect Tags ====================
    
    @Override
    public AspectList getObjectAspects(ItemStack is) {
        return ThaumcraftCraftingManager.getObjectTags(is);
    }
    
    @Override
    public AspectList generateTags(ItemStack is) {
        return ThaumcraftCraftingManager.generateTags(is);
    }
    
    // ==================== Aura System ====================
    
    @Override
    public float drainVis(Level level, BlockPos pos, float amount, boolean simulate) {
        return AuraHandler.drainVis(level, pos, amount, simulate);
    }
    
    @Override
    public float drainFlux(Level level, BlockPos pos, float amount, boolean simulate) {
        return AuraHandler.drainFlux(level, pos, amount, simulate);
    }
    
    @Override
    public void addVis(Level level, BlockPos pos, float amount) {
        AuraHandler.addVis(level, pos, amount);
    }
    
    @Override
    public void addFlux(Level level, BlockPos pos, float amount, boolean showEffect) {
        if (level.isClientSide()) {
            return;
        }
        AuraHandler.addFlux(level, pos, amount);
        
        if (showEffect && amount > 0.0f) {
            // TODO: Send flux visual effect packet when implemented
            // PacketHandler.sendToAllAround(new PacketFXPollute(pos, amount), level, pos, 32.0);
        }
    }
    
    @Override
    public float getTotalAura(Level level, BlockPos pos) {
        return AuraHandler.getTotalAura(level, pos);
    }
    
    @Override
    public float getVis(Level level, BlockPos pos) {
        return AuraHandler.getVis(level, pos);
    }
    
    @Override
    public float getFlux(Level level, BlockPos pos) {
        return AuraHandler.getFlux(level, pos);
    }
    
    @Override
    public int getAuraBase(Level level, BlockPos pos) {
        return AuraHandler.getAuraBase(level, pos);
    }
    
    @Override
    public boolean shouldPreserveAura(Level level, Player player, BlockPos pos) {
        return AuraHandler.shouldPreserveAura(level, player, pos);
    }
    
    // ==================== Golem Seals ====================
    
    @Override
    public void registerSeal(ISeal seal) {
        SealHandler.registerSeal(seal);
    }
    
    @Override
    public ISeal getSeal(String key) {
        return SealHandler.getSeal(key);
    }
    
    @Override
    public ISealEntity getSealEntity(String dimension, SealPos pos) {
        // Convert dimension string to ResourceKey<Level>
        ResourceKey<Level> dimKey = parseDimensionKey(dimension);
        if (dimKey == null) {
            return null;
        }
        return SealHandler.getSealEntity(dimKey, pos);
    }
    
    @Override
    public void addGolemTask(String dimension, Task task) {
        // Convert dimension string to ResourceKey<Level>
        ResourceKey<Level> dimKey = parseDimensionKey(dimension);
        if (dimKey == null) {
            return;
        }
        TaskHandler.addTask(dimKey, task);
    }
    
    @Override
    public ItemStack getSealStack(String key) {
        // TC4.2.3.5 has no control seal items (they are a TC5/TC6 mechanic)
        return ItemStack.EMPTY;
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Parse a dimension string (e.g., "minecraft:overworld") into a ResourceKey<Level>.
     */
    private ResourceKey<Level> parseDimensionKey(String dimension) {
        if (dimension == null || dimension.isEmpty()) {
            return null;
        }
        
        try {
            ResourceLocation loc = ResourceLocation.parse(dimension);
            return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, loc);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get a ServerLevel from a dimension string.
     * Returns null if not on server or dimension doesn't exist.
     */
    @SuppressWarnings("unused")
    private ServerLevel getServerLevel(String dimension) {
        ResourceKey<Level> dimKey = parseDimensionKey(dimension);
        if (dimKey == null) {
            return null;
        }
        
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        
        return server.getLevel(dimKey);
    }
}




