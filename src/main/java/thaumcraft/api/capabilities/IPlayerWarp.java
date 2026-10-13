package thaumcraft.api.capabilities;

import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * IPlayerWarp - Capability interface for tracking player warp (corruption) levels.
 * 
 * Warp is a negative effect that builds up from using certain dark magic,
 * crafting forbidden items, or encountering eldritch horrors.
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public interface IPlayerWarp extends INBTSerializable<CompoundTag> {

    /**
     * Clears all warp.
     */
    void clear();
    
    /**
     * Get the warp amount of a specific type
     * @param type the warp type to query
     * @return the amount of warp
     */
    int get(@Nonnull EnumWarpType type);
    
    /**
     * Set the warp amount of a specific type
     * @param type the warp type to set
     * @param amount the amount to set
     */
    void set(@Nonnull EnumWarpType type, int amount);
    
    /**
     * Add warp of a specific type
     * @param type the warp type to add
     * @param amount the amount to add (can be negative to remove)
     * @return the new warp amount
     */
    int add(@Nonnull EnumWarpType type, int amount);
    
    /**
     * Get the total warp from all sources
     * @return the combined warp amount
     */
    int getTotalWarp();
    
    /**
     * Get the total permanent warp (PERMANENT + NORMAL)
     * @return the combined permanent warp
     */
    int getPermanentWarp();
    
    /**
     * Check if the counter should tick (for temporary warp decay)
     * @return true if the counter should tick
     */
    boolean shouldTick();
    
    /**
     * Get the counter value (used for temporary warp decay timing)
     * @return the counter value
     */
    int getCounter();
    
    /**
     * Set the counter value
     * @param count the new counter value
     */
    void setCounter(int count);
    
    /**
     * Sync the warp data to the client
     * @param player the player to sync
     */
    void sync(ServerPlayer player);
    
    /**
     * Types of warp
     */
    enum EnumWarpType {
        /**
         * Permanent warp - cannot be removed except by special means
         */
        PERMANENT,
        /**
         * Normal warp - can be reduced with sanity soap, bath salts, etc.
         */
        NORMAL,
        /**
         * Temporary warp - decays over time
         */
        TEMPORARY
    }
}




