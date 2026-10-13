package thaumcraft.common.tiles.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.internal.WorldCoordinates;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.api.visnet.VisNetHandler;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TileVisNodeImpl - A vis network node tile entity implementation.
 * Can be an energized node, transducer, relay, or condenser.
 * Connects to nearby nodes to form a vis network.
 *
 * @author Azanor
 * Ported to 1.21.1
 */
public class TileVisNodeImpl extends BlockEntity implements TileVisNode {

    private AspectList auraBase = new AspectList();
    AspectList vis = new AspectList();
    private int range = 8;
    private int attunement = -1;
    private int maxVis = 0;
    private final CopyOnWriteArrayList<java.lang.ref.WeakReference<TileVisNode>> children = new CopyOnWriteArrayList<>();
    private java.lang.ref.WeakReference<TileVisNode> parent = new java.lang.ref.WeakReference<>(null);
    private final String id = UUID.randomUUID().toString();
    private boolean isSource = false;
    private boolean isReceiver = false;

    public TileVisNodeImpl(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VIS_NODE.get(), pos, state);
    }

    public TileVisNodeImpl(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ============================================================
    // TileVisNode Interface Implementation
    // ============================================================

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public boolean isSource() {
        return isSource;
    }

    public void setSource(boolean source) {
        isSource = source;
    }

    public boolean isReceiver() {
        return isReceiver;
    }

    public void setReceiver(boolean receiver) {
        isReceiver = receiver;
    }

    public int getAttunement() {
        return attunement;
    }

    public void setAttunement(int attunement) {
        this.attunement = attunement;
    }

    public AspectList getAspects() {
        return vis;
    }

    public AspectList getAuraBase() {
        return auraBase;
    }

    public int consumeVis(Aspect aspect, int amount) {
        int drain = Math.min(vis.getAmount(aspect), amount);
        if (drain > 0) {
            vis.reduce(aspect, drain);
            setChanged();
        }
        return drain;
    }

    public BlockPos getLocation() {
        return worldPosition;
    }

    public Level getWorldObj() {
        return level;
    }

    public TileVisNode getParent() {
        return parent.get();
    }

    public CopyOnWriteArrayList<java.lang.ref.WeakReference<TileVisNode>> getChildren() {
        return children;
    }

    public int getDimension() {
        return level != null ? level.dimension().location().hashCode() : 0;
    }

    public void setParent(TileVisNode parent) {
        this.parent = new java.lang.ref.WeakReference<>(parent);
    }

    public void addChild(TileVisNode child) {
        if (!children.contains(new java.lang.ref.WeakReference<>(child))) {
            children.add(new java.lang.ref.WeakReference<>(child));
        }
    }

    public boolean isInvalid() {
        return isRemoved();
    }

    public WorldCoordinates getLocationWC() {
        return new WorldCoordinates(worldPosition, level);
    }

    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    public int addToContainer(Aspect tag, int amount) {
        int out = 0;
        if (vis.getAmount(tag) + amount > auraBase.getAmount(tag)) {
            out = vis.getAmount(tag) + amount - auraBase.getAmount(tag);
        }
        vis.add(tag, amount - out);
        setChanged();
        return out;
    }

    public boolean takeFromContainer(Aspect tag, int amount) {
        if (vis.getAmount(tag) >= amount) {
            vis.remove(tag, amount);
            setChanged();
            return true;
        }
        return false;
    }

    public boolean takeFromContainer(AspectList ot) {
        return false; // Not implemented for nodes
    }

    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return vis.getAmount(tag) >= amount;
    }

    public boolean doesContainerContain(AspectList ot) {
        for (Aspect tag : ot.getAspects()) {
            if (vis.getAmount(tag) < ot.getAmount(tag)) {
                return false;
            }
        }
        return true;
    }

    public int containerContains(Aspect tag) {
        return vis.getAmount(tag);
    }

    public void readCustomNBT(CompoundTag tag) {
        // id is final, can't be assigned - skip
        this.setAttunement(tag.getInt("attunement"));
        this.setRange(tag.getInt("range"));
        this.isSource = tag.getBoolean("isSource");
        this.isReceiver = tag.getBoolean("isReceiver");

        this.auraBase = new AspectList();
        ListTag tlist = tag.getList("AuraBase", Tag.TAG_COMPOUND);
        for (int j = 0; j < tlist.size(); j++) {
            CompoundTag rs = tlist.getCompound(j);
            if (rs.contains("key")) {
                Aspect aspect = thaumcraft.api.aspects.Aspect.getAspect(rs.getString("key"));
                if (aspect != null) {
                    this.auraBase.add(aspect, rs.getInt("amount"));
                }
            }
        }

        this.vis.readFromNBT(tag);

        // Load parent/children references would be rebuilt on network init
    }

    public void writeCustomNBT(CompoundTag tag) {
        tag.putString("nodeId", id);
        tag.putInt("attunement", attunement);
        tag.putInt("range", range);
        tag.putBoolean("isSource", isSource);
        tag.putBoolean("isReceiver", isReceiver);

        ListTag tlist = new ListTag();
        tag.put("AuraBase", tlist);

        for (Aspect aspect : auraBase.getAspects()) {
            if (aspect != null) {
                CompoundTag f = new CompoundTag();
                f.putString("key", aspect.getTag());
                f.putInt("amount", auraBase.getAmount(aspect));
                tlist.add(f);
            }
        }

        vis.writeToNBT(tag);
    }

    // ============================================================
    // TileEntity Methods
    // ============================================================

    @Override
    public void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readCustomNBT(tag);
    }

    @Override
    public void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeCustomNBT(tag);
    }

    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            // Register with vis network
            VisNetHandler.addSource(level, this);
            VisNetHandler.addNode(level, this);
        }
    }

    public void setRemoved() {
        super.setRemoved();
        if (!level.isClientSide) {
            VisNetHandler.removeSource(level, this);
        }
    }

    public void tick() {
        if (level.isClientSide) return;

        // Regenerate vis if this is a source
        if (isSource && auraBase.size() > 0) {
            regenerateVis();
        }

        // Equalize vis with connected nodes
        equalizeVis();
    }

    /**
     * Regenerate vis up to aura base.
     */
    private void regenerateVis() {
        for (Aspect aspect : auraBase.getAspects()) {
            int current = vis.getAmount(aspect);
            int max = auraBase.getAmount(aspect);
            if (current < max) {
                // Regenerate 1 vis per tick per aspect
                vis.add(aspect, 1);
                setChanged();
            }
        }
    }

    /**
     * Equalize vis with connected nodes.
     */
    private void equalizeVis() {
        // Equalize with parent
        TileVisNode parentNode = getParent();
        if (parentNode != null && !parentNode.isInvalid()) {
            for (Aspect aspect : auraBase.getAspects()) {
                int myVis = vis.getAmount(aspect);
                int parentVis = parentNode.getAspects().getAmount(aspect);
                int parentMax = parentNode.getAuraBase().getAmount(aspect);

                if (parentVis + parentMax < parentNode.getAuraBase().getAmount(aspect) * 1.5) {
                    // Equalize if we have more and parent has room
                    float ratio = (float) myVis / (myVis + parentVis);
                    if (ratio > 0.6f && myVis > parentVis) {
                        int transfer = Math.min(myVis - parentVis, 1);
                        if (transfer > 0) {
                            vis.reduce(aspect, transfer);
                            parentNode.addToContainer(aspect, transfer);
                            setChanged();
                        }
                    }
                }
            }
        }

        // Equalize with children
        for (java.lang.ref.WeakReference<TileVisNode> childRef : children) {
            TileVisNode child = childRef.get();
            if (child == null || child.isInvalid()) continue;

            for (Aspect aspect : auraBase.getAspects()) {
                int myVis = vis.getAmount(aspect);
                int childVis = child.getAspects().getAmount(aspect);
                int childMax = child.getAuraBase().getAmount(aspect);

                if (childVis + childMax < child.getAuraBase().getAmount(aspect) * 1.5) {
                    if (childVis < myVis) {
                        int transfer = Math.min(myVis - childVis, 1);
                        if (transfer > 0) {
                            vis.reduce(aspect, transfer);
                            child.addToContainer(aspect, transfer);
                            setChanged();
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    // Client Events
    // ============================================================

    public boolean receiveClientEvent(int id, int data) {
        if (id == 1) {
            // Vis zap effect
            // TODO: Implement FX
            return true;
        }
        return false;
    }
}
