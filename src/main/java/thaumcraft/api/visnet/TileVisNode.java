package thaumcraft.api.visnet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.internal.WorldCoordinates;

/**
 * TileVisNode - A node in the Vis network.
 * Can be a source (node), relay, condenser, or transducer.
 *
 * @author Azanor
 * Ported to 1.21.1
 */
public interface TileVisNode {

    /**
     * Get the range at which this node can connect to other nodes.
     * @return range in chunks
     */
    int getRange();

    /**
     * Check if this node is a vis source (produces vis).
     * @return true if source
     */
    boolean isSource();

    /**
     * Check if this node is a receiver (consumes vis).
     * @return true if receiver
     */
    boolean isReceiver();

    /**
     * Get the attunement filter for this node.
     * -1 = no filter (accepts all)
     * @return attunement aspect tag or -1
     */
    int getAttunement();

    /**
     * Get the AspectList of vis available at this node.
     * @return vis aspects
     */
    AspectList getAspects();

    /**
     * Get the base aura for this node (maximum capacity).
     * @return aura base per aspect
     */
    AspectList getAuraBase();

    /**
     * Consume vis from this node.
     * @param aspect the aspect to consume
     * @param amount the amount to consume
     * @return amount actually consumed
     */
    int consumeVis(Aspect aspect, int amount);

    /**
     * Get the world coordinates of this node.
     */
    BlockPos getLocation();

    /**
     * Get the world this node is in.
     */
    Level getWorldObj();

    /**
     * Get the node's parent in the vis network tree.
     */
    TileVisNode getParent();

    /**
     * Get the node's children in the vis network tree.
     */
    java.util.List<java.lang.ref.WeakReference<TileVisNode>> getChildren();

    /**
     * Get the dimension this node is in.
     */
    int getDimension();

    /**
     * Set the parent node.
     */
    void setParent(TileVisNode parent);

    /**
     * Add a child node.
     */
    void addChild(TileVisNode child);

    /**
     * Check if this node is invalid (removed).
     */
    boolean isInvalid();

    /**
     * Get the node's location as a WorldCoordinates object.
     */
    WorldCoordinates getLocationWC();

    /**
     * Check if a specific aspect can be accepted.
     */
    boolean doesContainerAccept(Aspect aspect);

    /**
     * Add vis to this node.
     */
    int addToContainer(Aspect aspect, int amount);

    /**
     * Take vis from this node.
     */
    boolean takeFromContainer(Aspect aspect, int amount);

    /**
     * Take multiple aspects from this node.
     */
    boolean takeFromContainer(AspectList aspects);

    /**
     * Check if this node contains enough of an aspect.
     */
    boolean doesContainerContainAmount(Aspect aspect, int amount);

    /**
     * Check if this node contains all required aspects.
     */
    boolean doesContainerContain(AspectList aspects);

    /**
     * Get amount of specific aspect in this node.
     */
    int containerContains(Aspect aspect);

    /**
     * Read custom NBT data.
     */
    void readCustomNBT(CompoundTag tag);

    /**
     * Write custom NBT data.
     */
    void writeCustomNBT(CompoundTag tag);
}
