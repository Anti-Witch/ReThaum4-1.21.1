package thaumcraft.api.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * Interface for a seal entity instance in the world.
 * Represents a placed seal with its configuration.
 */
public interface ISealEntity {

    /**
     * Called each tick to update the seal entity
     */
    void tickSealEntity(Level level);

    /**
     * @return The seal type for this entity
     */
    ISeal getSeal();

    /**
     * @return The position and face of this seal
     */
    SealPos getSealPos();

    /**
     * @return Task priority (higher = more important)
     */
    byte getPriority();

    /**
     * Set task priority
     */
    void setPriority(byte priority);

    /**
     * Read seal entity data from NBT
     */
    void readNBT(CompoundTag nbt);

    /**
     * Write seal entity data to NBT
     */
    CompoundTag writeNBT();

    /**
     * Sync seal data to clients
     */
    void syncToClient(Level level);

    /**
     * @return The area of effect for this seal (as x, y, z extents)
     */
    BlockPos getArea();

    /**
     * Set the area of effect
     */
    void setArea(BlockPos area);

    /**
     * @return true if seal configuration is locked
     */
    boolean isLocked();

    /**
     * Set locked state
     */
    void setLocked(boolean locked);

    /**
     * @return true if seal responds to redstone
     */
    boolean isRedstoneSensitive();

    /**
     * Set redstone sensitivity
     */
    void setRedstoneSensitive(boolean redstone);

    /**
     * @return Owner's name/UUID
     */
    String getOwner();

    /**
     * Set owner
     */
    void setOwner(String owner);

    /**
     * @return Color for golem matching (0-16, 0 = any color)
     */
    byte getColor();

    /**
     * Set color
     */
    void setColor(byte color);

    /**
     * @return true if seal is currently stopped by redstone signal
     */
    boolean isStoppedByRedstone(Level level);
}




