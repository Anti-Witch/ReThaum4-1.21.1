package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * Magic mirror tile entity - teleports items between linked mirrors.
 * Can be linked across dimensions. Generates instability when used.
 */
public class TileMirror extends TileThaumcraft implements Container {

    // Link data
    public boolean linked = false;
    public int linkX = 0;
    public int linkY = 0;
    public int linkZ = 0;
    public ResourceKey<Level> linkDimension = Level.OVERWORLD;
    
    // Instability
    public int instability = 0;
    private static final int INSTABILITY_THRESHOLD = 128;
    
    // Tick counter
    private int count = 0;
    private int linkCheckInterval = 40;
    
    // Output buffer for items coming through the mirror
    private List<ItemStack> outputStacks = new ArrayList<>();

    public TileMirror(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileMirror(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MIRROR_ITEM.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putBoolean("Linked", linked);
        tag.putInt("LinkX", linkX);
        tag.putInt("LinkY", linkY);
        tag.putInt("LinkZ", linkZ);
        tag.putString("LinkDim", linkDimension.location().toString());
        tag.putInt("Instability", instability);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        linked = tag.getBoolean("Linked");
        linkX = tag.getInt("LinkX");
        linkY = tag.getInt("LinkY");
        linkZ = tag.getInt("LinkZ");
        if (tag.contains("LinkDim")) {
            linkDimension = ResourceKey.create(Registries.DIMENSION, 
                ResourceLocation.parse(tag.getString("LinkDim")));
        }
        instability = tag.getInt("Instability");
    }

    @Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // Save output stacks
        ListTag itemList = new ListTag();
        for (int i = 0; i < outputStacks.size(); i++) {
            ItemStack stack = outputStacks.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(registries, itemTag);
                itemList.add(itemTag);
            }
        }
        tag.put("Items", itemList);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Load output stacks
        outputStacks.clear();
        ListTag itemList = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemList.size(); i++) {
            CompoundTag itemTag = itemList.getCompound(i);
            ItemStack stack = ItemStack.parseOptional(registries, itemTag);
            if (!stack.isEmpty()) {
                outputStacks.add(stack);
            }
        }
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileMirror tile) {
        tile.count++;
        
        // Eject items from output buffer
        tile.ejectItems();
        
        // Check and handle instability
        tile.checkInstability();
        
        // Periodically verify link
        if (tile.count % tile.linkCheckInterval == 0) {
            if (!tile.isLinkValidSimple()) {
                // Slow down link checks if disconnected
                if (tile.linkCheckInterval < 600) {
                    tile.linkCheckInterval += 20;
                }
                tile.restoreLink();
            } else {
                tile.linkCheckInterval = 40;
            }
        }
    }

    // ==================== Link Management ====================

    /**
     * Try to restore a broken link.
     */
    public void restoreLink() {
        if (!isDestinationValid()) return;
        
        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return;
        
        BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (te instanceof TileMirror target) {
            // Establish bidirectional link
            target.linked = true;
            target.linkX = worldPosition.getX();
            target.linkY = worldPosition.getY();
            target.linkZ = worldPosition.getZ();
            target.linkDimension = level.dimension();
            target.syncTile(false);
            
            linked = true;
            setChanged();
            target.setChanged();
            syncTile(false);
        }
    }

    /**
     * Break the link when mirror is removed.
     */
    public void invalidateLink() {
        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return;
        
        if (!isChunkLoaded(targetWorld, linkX, linkZ)) return;
        
        BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (te instanceof TileMirror target) {
            target.linked = false;
            setChanged();
            target.setChanged();
            target.syncTile(false);
        }
    }

    /**
     * Full link validation - checks both directions.
     */
    public boolean isLinkValid() {
        if (!linked) return false;
        
        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return false;
        
        BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (!(te instanceof TileMirror target)) {
            linked = false;
            setChanged();
            syncTile(false);
            return false;
        }
        
        if (!target.linked) {
            linked = false;
            setChanged();
            syncTile(false);
            return false;
        }
        
        // Check that target points back to us
        if (target.linkX != worldPosition.getX() || 
            target.linkY != worldPosition.getY() || 
            target.linkZ != worldPosition.getZ() ||
            !target.linkDimension.equals(level.dimension())) {
            linked = false;
            setChanged();
            syncTile(false);
            return false;
        }
        
        return true;
    }

    /**
     * Simple link check without modification.
     */
    public boolean isLinkValidSimple() {
        if (!linked) return false;
        
        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return false;
        
        BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (!(te instanceof TileMirror target)) return false;
        
        return target.linked && 
               target.linkX == worldPosition.getX() && 
               target.linkY == worldPosition.getY() && 
               target.linkZ == worldPosition.getZ() &&
               target.linkDimension.equals(level.dimension());
    }

    /**
     * Check if destination mirror exists and is available for linking.
     */
    public boolean isDestinationValid() {
        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return false;
        
        BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (!(te instanceof TileMirror target)) {
            linked = false;
            setChanged();
            syncTile(false);
            return false;
        }
        
        // Destination is valid if it's not linked to something else
        return !target.isLinkValid();
    }

    // ==================== Item Transport ====================

    /**
     * Transport an item entity through the mirror.
     */
    public boolean transport(ItemEntity itemEntity) {
        ItemStack items = itemEntity.getItem();
        if (!linked || !isLinkValid()) return false;
        
        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return false;
        
        BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (te instanceof TileMirror target) {
            target.addStack(items.copy());
            addInstability(null, items.getCount());
            itemEntity.discard();
            setChanged();
            target.setChanged();
            
            // Trigger visual effect
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
            }
            return true;
        }
        return false;
    }

    /**
     * Transport an item stack directly (for hopper/pipe interaction).
     */
    public boolean transportDirect(ItemStack items) {
        if (items.isEmpty()) return false;
        addStack(items.copy());
        setChanged();
        return true;
    }

    /**
     * Add a stack to the output buffer.
     */
    public void addStack(ItemStack stack) {
        outputStacks.add(stack.copy());
        setChanged();
    }

    /**
     * Eject items from the output buffer.
     */
    private void ejectItems() {
        if (outputStacks.isEmpty() || count <= 20) return;
        
        int index = level.random.nextInt(outputStacks.size());
        ItemStack stack = outputStacks.get(index);
        
        if (stack.isEmpty()) {
            outputStacks.remove(index);
            return;
        }
        
        // Eject one item at a time
        ItemStack outItem = stack.copy();
        outItem.setCount(1);
        
        if (spawnItem(outItem)) {
            stack.shrink(1);
            addInstability(null, 1);
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
            }
            
            if (stack.isEmpty()) {
                outputStacks.remove(index);
            }
        }
        setChanged();
    }

    /**
     * Spawn an item entity in front of the mirror.
     */
    private boolean spawnItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        
        try {
            Direction facing = getFacing();
            double x = worldPosition.getX() + 0.5;
            double y = worldPosition.getY() + 0.25;
            double z = worldPosition.getZ() + 0.5;
            
            ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
            itemEntity.setDeltaMovement(
                facing.getStepX() * 0.15,
                facing.getStepY() * 0.15,
                facing.getStepZ() * 0.15
            );
            itemEntity.setPickUpDelay(20);
            
            level.addFreshEntity(itemEntity);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== Instability ====================

    /**
     * Add instability to this mirror and optionally the linked mirror.
     */
    protected void addInstability(Level targetWorld, int amount) {
        instability += amount;
        setChanged();
        
        if (targetWorld != null) {
            BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
            if (te instanceof TileMirror target) {
                target.instability += amount;
                if (target.instability < 0) target.instability = 0;
                target.setChanged();
            }
        }
    }

    /**
     * Process instability - too much causes aura pollution.
     */
    private void checkInstability() {
        if (instability > INSTABILITY_THRESHOLD) {
            AuraHelper.polluteAura(level, worldPosition, 1.0f, true);
            instability -= INSTABILITY_THRESHOLD;
            setChanged();
        }
        
        // Slowly decay instability
        if (instability > 0 && count % 100 == 0) {
            instability--;
        }
    }

    // ==================== Helpers ====================

    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    private ServerLevel getTargetWorld() {
        if (level == null || level.isClientSide()) return null;
        MinecraftServer server = level.getServer();
        if (server == null) return null;
        return server.getLevel(linkDimension);
    }

    private boolean isChunkLoaded(ServerLevel world, int x, int z) {
        return world.hasChunkAt(new BlockPos(x, 0, z));
    }

    // ==================== Block Events ====================

    @Override
    public boolean triggerEvent(int id, int data) {
        if (id == 1) {
            // Visual effect for item transport
            // Client-side particles would be spawned here
            return true;
        }
        return super.triggerEvent(id, data);
    }

    // ==================== Container Implementation ====================
    // This allows hoppers and pipes to insert items

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // When a hopper inserts an item, transport it
        if (!linked || !isLinkValid()) {
            spawnItem(stack.copy());
            return;
        }
        
        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) {
            spawnItem(stack.copy());
            return;
        }
        
        BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (te instanceof TileMirror target) {
            target.addStack(stack.copy());
            addInstability(null, stack.getCount());
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
            }
        } else {
            spawnItem(stack.copy());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        outputStacks.clear();
    }
}




