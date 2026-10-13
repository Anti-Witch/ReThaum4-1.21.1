package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.INode;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.api.wands.IWandable;
import thaumcraft.common.items.essentia.ItemJarNode;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;

/**
 * TileJarNode - "Node in a Jar" (TC4 TileJarNode).
 *
 * A full aura node that has been trapped inside a warded jar. It implements
 * {@link INode} exactly like {@link TileNode}, but the node is frozen: it never
 * ticks, never regenerates and never interacts with neighbouring nodes (TC4
 * TileJarNode has no updateEntity). The trapped node keeps its aspects, node
 * type, modifier and id so that it can be released again:
 *
 * - broken jar  -> drops the {@code jar_node} item carrying the node NBT
 *                  (TC4 BlockJar.getDrops, metadata 2)
 * - wand click  -> the node is released back into the world as a node block
 *                  (TC4 TileJarNode.onWandRightClick)
 */
public class TileJarNode extends TileThaumcraft implements INode, IWandable {

    private AspectList aspects = new AspectList();
    private AspectList aspectsBase = new AspectList();
    private NodeType nodeType = NodeType.NORMAL;
    private NodeModifier nodeModifier = null;
    private String id = "";

    /** TC4 jarring animation timer (client only). */
    public long animate = 0L;
    /** When false the jar will not drop a filled item (TC4 release mechanic). */
    public boolean drop = true;

    public TileJarNode(BlockPos pos, BlockState state) {
        this(ModBlockEntities.JAR_NODE.get(), pos, state);
    }

    public TileJarNode(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================== NBT (TC4 readCustomNBT/writeCustomNBT) ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        this.aspects.writeToNBT(tag);
        tag.putString("nodeId", this.id);

        ListTag tlist = new ListTag();
        tag.put("AspectsBase", tlist);
        for (Aspect aspect : this.aspectsBase.getAspects()) {
            if (aspect != null) {
                CompoundTag f = new CompoundTag();
                f.putString("key", aspect.getTag());
                f.putInt("amount", this.aspectsBase.getAmount(aspect));
                tlist.add(f);
            }
        }

        tag.putByte("type", (byte) this.getNodeType().ordinal());
        tag.putByte("modifier", this.getNodeModifier() == null ? -1 : (byte) this.getNodeModifier().ordinal());
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        this.aspects.readFromNBT(tag);
        this.id = tag.getString("nodeId");

        AspectList al = new AspectList();
        ListTag tlist = tag.getList("AspectsBase", Tag.TAG_COMPOUND);
        for (int j = 0; j < tlist.size(); j++) {
            CompoundTag rs = tlist.getCompound(j);
            if (rs.contains("key")) {
                al.add(Aspect.getAspect(rs.getString("key")), rs.getInt("amount"));
            }
        }

        // TC4: legacy saves only stored a single "nodeVisBase" value.
        short oldBase = tag.getShort("nodeVisBase");
        this.aspectsBase = new AspectList();
        if (oldBase > 0 && al.size() == 0) {
            for (Aspect a : this.aspects.getAspects()) {
                this.aspectsBase.merge(a, oldBase);
            }
        } else {
            this.aspectsBase = al.copy();
        }

        NodeType[] types = NodeType.values();
        int t = tag.getByte("type");
        this.setNodeType(t >= 0 && t < types.length ? types[t] : NodeType.NORMAL);

        NodeModifier[] mods = NodeModifier.values();
        int mod = tag.getByte("modifier");
        this.setNodeModifier(mod >= 0 && mod < mods.length ? mods[mod] : null);
    }

    // ==================== INode ====================

    @Override
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    @Override
    public AspectList getAspects() {
        return this.aspects;
    }

    @Override
    public AspectList getAspectsBase() {
        return this.aspectsBase;
    }

    @Override
    public void setAspects(AspectList aspects) {
        if (aspects == null) {
            aspects = new AspectList();
        }
        this.aspects = aspects.copy();
        this.aspectsBase = aspects.copy();
    }

    @Override
    public NodeType getNodeType() {
        return this.nodeType;
    }

    @Override
    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType == null ? NodeType.NORMAL : nodeType;
    }

    @Override
    public void setNodeModifier(NodeModifier nodeModifier) {
        this.nodeModifier = nodeModifier;
    }

    @Override
    public NodeModifier getNodeModifier() {
        return this.nodeModifier;
    }

    @Override
    public int getNodeVisBase(Aspect aspect) {
        return this.aspectsBase.getAmount(aspect);
    }

    @Override
    public void setNodeVisBase(Aspect aspect, short nodeVisBase) {
        if (this.aspectsBase.getAmount(aspect) < nodeVisBase) {
            this.aspectsBase.merge(aspect, nodeVisBase);
        } else {
            this.aspectsBase.reduce(aspect, this.aspectsBase.getAmount(aspect) - nodeVisBase);
        }
    }

    // ==================== IAspectContainer (TC4 TileJarNode) ====================

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        int out = 0;
        if (this.aspects.getAmount(aspect) + amount > this.aspectsBase.getAmount(aspect)) {
            out = this.aspects.getAmount(aspect) + amount - this.aspectsBase.getAmount(aspect);
        }
        this.aspects.add(aspect, amount - out);
        this.markDirtyAndSync();
        return out;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (this.aspects.getAmount(aspect) >= amount) {
            this.aspects.remove(aspect, amount);
            this.markDirtyAndSync();
            return true;
        }
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList ot) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return this.aspects.getAmount(tag) >= amount;
    }

    @Override
    public boolean doesContainerContain(AspectList ot) {
        for (Aspect a : ot.getAspects()) {
            if (this.aspects.getAmount(a) < ot.getAmount(a)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int containerContains(Aspect tag) {
        return this.aspects.getAmount(tag);
    }

    // ==================== Item transfer ====================

    /**
     * Build the "node in a jar" item carrying this node's data
     * (TC4 BlockJar.getDrops for metadata 2).
     */
    public ItemStack createItemStack() {
        ItemStack stack = new ItemStack(ModItems.JAR_NODE.get());
        if (stack.getItem() instanceof ItemJarNode item) {
            item.setAspects(stack, this.getAspects().copy());
            item.setNodeAttributes(stack, this.getNodeType(), this.getNodeModifier(), this.getId());
        }
        return stack;
    }

    /**
     * Restore a captured node from an item stack (TC4 ItemJarNode.onItemUse).
     */
    public void applyFromItem(ItemJarNode item, ItemStack stack) {
        if (item == null || stack == null || stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA)) {
            return;
        }
        AspectList aspects = item.getAspects(stack);
        if (aspects != null) {
            this.setAspects(aspects);
        }
        NodeType type = item.getNodeType(stack);
        if (type != null) {
            this.setNodeType(type);
        }
        this.setNodeModifier(item.getNodeModifier(stack));
        this.setId(item.getNodeId(stack));
        this.markDirtyAndSync();
    }

    // ==================== Client event (TC4 receiveClientEvent 9) ====================

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 9) {
            if (this.level != null && this.level.isClientSide) {
                // TC4 blockSparkle around the freshly jarred node.
                for (int yy = -1; yy < 3; ++yy) {
                    for (int xx = -1; xx < 2; ++xx) {
                        for (int zz = -1; zz < 2; ++zz) {
                            double px = this.worldPosition.getX() + xx + this.level.random.nextDouble();
                            double py = this.worldPosition.getY() + yy + this.level.random.nextDouble();
                            double pz = this.worldPosition.getZ() + zz + this.level.random.nextDouble();
                            this.level.addParticle(ParticleTypes.ENCHANT, px, py, pz, 0.0D, 0.0D, 0.0D);
                        }
                    }
                }
                this.animate = System.currentTimeMillis() + 1000L;
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    // ==================== IWandable (TC4 TileJarNode.onWandRightClick) ====================

    /**
     * Right-clicking the jar with a wand releases the trapped node back into the
     * world (the jar shatters). TC4 setBlock(x, y, z, blockAiry, 0, 3).
     */
    @Override
    public int onWandRightClick(Level world, ItemStack wandStack, Player player, int x, int y, int z, int side, int md) {
        BlockPos pos = this.worldPosition;

        if (!world.isClientSide) {
            this.drop = false;

            world.setBlockAndUpdate(pos, ModBlocks.NODE.get().defaultBlockState());
            if (world.getBlockEntity(pos) instanceof TileNode node) {
                node.setAspects(this.getAspects());
                node.setNodeModifier(this.getNodeModifier());
                node.setNodeType(this.getNodeType());
                node.setId(this.getId());
                node.setChanged();
                world.sendBlockUpdated(pos, node.getBlockState(), node.getBlockState(), 3);
            }

            // TC4: block break particles + random.glass sound
            world.levelEvent(2001, pos, Block.getId(ModBlocks.JAR_NODE.get().defaultBlockState()));
            world.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F,
                    0.9F + world.random.nextFloat() * 0.2F);
        }

        player.swing(InteractionHand.MAIN_HAND);
        return 0;
    }

    @Override
    public ItemStack onWandRightClick(Level world, ItemStack wandStack, Player player) {
        return null;
    }

    @Override
    public void onUsingWandTick(ItemStack wandStack, Player player, int count) {
    }

    @Override
    public void onWandStoppedUsing(ItemStack wandStack, Level world, Player player, int count) {
    }
}
