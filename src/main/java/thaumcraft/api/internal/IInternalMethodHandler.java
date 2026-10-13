package thaumcraft.api.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge.EnumKnowledgeType;
import thaumcraft.api.capabilities.IPlayerWarp.EnumWarpType;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.api.research.ResearchCategory;

/**
 * Interface for internal Thaumcraft methods exposed to the API.
 * 
 * @author Azanor
 * Ported to 1.20.1
 *
 * @see IInternalMethodHandler#addKnowledge
 * @see IInternalMethodHandler#progressResearch
 * @see IInternalMethodHandler#completeResearch
 * @see IInternalMethodHandler#doesPlayerHaveRequisites
 * @see IInternalMethodHandler#addWarpToPlayer
 * @see IInternalMethodHandler#getObjectAspects
 * @see IInternalMethodHandler#generateTags
 * @see IInternalMethodHandler#drainVis
 * @see IInternalMethodHandler#drainFlux
 * @see IInternalMethodHandler#addVis
 * @see IInternalMethodHandler#addFlux
 * @see IInternalMethodHandler#getTotalAura
 * @see IInternalMethodHandler#getVis
 * @see IInternalMethodHandler#getFlux
 * @see IInternalMethodHandler#getAuraBase
 * @see IInternalMethodHandler#shouldPreserveAura
 * @see IInternalMethodHandler#registerSeal
 * @see IInternalMethodHandler#getSeal
 * @see IInternalMethodHandler#getSealEntity
 * @see IInternalMethodHandler#addGolemTask 
 * @see IInternalMethodHandler#getSealStack
 */
public interface IInternalMethodHandler {
    
    /**
     * Add raw knowledge points (not whole knowledges) to the given player.
     * This method will trigger appropriate gui notifications, etc.
     * @param player the player
     * @param type the knowledge type
     * @param category the research category
     * @param amount the amount of knowledge points
     * @return if the knowledge was added
     */
    boolean addKnowledge(Player player, EnumKnowledgeType type, ResearchCategory category, int amount);
    
    /**
     * Progresses research with all the proper bells and whistles (popups, sounds, warp, etc)
     * If the research is linked to a research entry with stages the player's current stage will be increased 
     * by 1, or set to 1 if the research was not known before.
     * @param player the player
     * @param researchkey the research key
     * @return if operation succeeded
     */
    boolean progressResearch(Player player, String researchkey);
    
    /**
     * Completes research with all the proper bells and whistles (popups, sounds, warp, etc)
     * This automatically sets all its stages as complete. 
     * Most of the time you should probably use progressResearch instead.
     * @param player the player
     * @param researchkey the research key
     * @return if operation succeeded
     */
    boolean completeResearch(Player player, String researchkey);
    
    /**
     * @param player the player
     * @param researchkey the key of the research you want to check
     * @return does the player have all the required knowledge to complete the passed researchkey
     */
    boolean doesPlayerHaveRequisites(Player player, String researchkey);
    
    /**
     * Adds warp with all the proper bells and whistles (text, sounds, etc)
     * @param player the player
     * @param amount the amount of warp to add
     * @param type the warp type
     */
    void addWarpToPlayer(Player player, int amount, EnumWarpType type);
    
    /**
     * The total of the players normal + permanent warp. NOT temporary warp.
     * @param player the player
     * @return the actual warp amount
     */
    int getActualWarp(Player player);

    /**
     * Get the aspects associated with an item stack.
     * @param is the item stack
     * @return the aspect list
     */
    AspectList getObjectAspects(ItemStack is);
    
    /**
     * Generate aspect tags for an item stack by analyzing its recipes.
     * @param is the item stack
     * @return the generated aspect list
     */
    AspectList generateTags(ItemStack is);
    
    /**
     * Drain vis from the aura at the given position.
     * @param level the level
     * @param pos the position
     * @param amount the amount to drain
     * @param simulate if true, don't actually drain
     * @return the amount actually drained
     */
    float drainVis(Level level, BlockPos pos, float amount, boolean simulate);
    
    /**
     * Drain flux from the aura at the given position.
     * @param level the level
     * @param pos the position
     * @param amount the amount to drain
     * @param simulate if true, don't actually drain
     * @return the amount actually drained
     */
    float drainFlux(Level level, BlockPos pos, float amount, boolean simulate);
    
    /**
     * Add vis to the aura at the given position.
     * @param level the level
     * @param pos the position
     * @param amount the amount to add
     */
    void addVis(Level level, BlockPos pos, float amount);
    
    /**
     * Add flux to the aura at the given position.
     * @param level the level
     * @param pos the position
     * @param amount the amount to add
     * @param showEffect whether to show a visual effect
     */
    void addFlux(Level level, BlockPos pos, float amount, boolean showEffect);
    
    /**
     * Returns the aura and flux in a chunk added together.
     * @param level the level
     * @param pos the position
     * @return the total aura (vis + flux)
     */
    float getTotalAura(Level level, BlockPos pos);
    
    /**
     * Get the vis amount in the aura at the given position.
     * @param level the level
     * @param pos the position
     * @return the vis amount
     */
    float getVis(Level level, BlockPos pos);
    
    /**
     * Get the flux amount in the aura at the given position.
     * @param level the level
     * @param pos the position
     * @return the flux amount
     */
    float getFlux(Level level, BlockPos pos);
    
    /**
     * Get the base aura level at the given position.
     * @param level the level
     * @param pos the position
     * @return the base aura level
     */
    int getAuraBase(Level level, BlockPos pos);
    
    /**
     * Register a golem seal.
     * @param seal the seal to register
     */
    void registerSeal(ISeal seal);
    
    /**
     * Get a registered seal by key.
     * @param key the seal key
     * @return the seal, or null if not found
     */
    ISeal getSeal(String key);
    
    /**
     * Get a seal entity at the given position.
     * @param dimension the dimension resource key string
     * @param pos the seal position
     * @return the seal entity, or null if not found
     */
    ISealEntity getSealEntity(String dimension, SealPos pos);
    
    /**
     * Add a task for golems to perform.
     * @param dimension the dimension resource key string
     * @param task the task to add
     */
    void addGolemTask(String dimension, Task task);
    
    /**
     * Check if the aura at a position should be preserved (e.g., in a special area).
     * @param level the level
     * @param player the player
     * @param pos the position
     * @return true if aura should be preserved
     */
    boolean shouldPreserveAura(Level level, Player player, BlockPos pos);
    
    /**
     * Get an item stack representing the given seal.
     * @param key the seal key
     * @return an item stack for the seal
     */
    ItemStack getSealStack(String key);
}




