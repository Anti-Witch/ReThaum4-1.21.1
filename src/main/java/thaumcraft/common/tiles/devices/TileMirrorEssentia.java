package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

/**
 * Essentia mirror tile entity - teleports essentia between linked mirrors.
 * Acts as an IAspectSource that proxies requests to the linked mirror's neighbors.
 */
public class TileMirrorEssentia extends TileThaumcraft implements IAspectSource {

    // Link data
    public boolean linked = false;
    public int linkX = 0;
    public int linkY = 0;
    public int linkZ = 0;
    public ResourceKey<Level> linkDimension = Level.OVERWORLD;
    public Direction linkedFacing = Direction.DOWN;

    // Instability
    public int instability = 0;
    private static final int INSTABILITY_THRESHOLD = 64;

    // Tick counter
    private int count = 0;
    private int linkCheckInterval = 40;

    public TileMirrorEssentia(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileMirrorEssentia(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MIRROR_ESSENTIA.get(), pos, state);
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

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileMirrorEssentia tile) {
        tile.count++;

        // Check instability
        tile.checkInstability();

        // Periodically verify link
        if (tile.count % tile.linkCheckInterval == 0) {
            if (!tile.isLinkValidSimple()) {
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

    public void restoreLink() {
        if (!isDestinationValid()) return;

        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return;

        BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (te instanceof TileMirrorEssentia target) {
            target.linked = true;
            target.linkX = worldPosition.getX();
            target.linkY = worldPosition.getY();
            target.linkZ = worldPosition.getZ();
            target.linkDimension = level.dimension();
            target.syncTile(false);

            // Cache the target's facing for essentia routing
            linkedFacing = getFacingAt(targetWorld, new BlockPos(linkX, linkY, linkZ));
            linked = true;
            setChanged();
            target.setChanged();
            syncTile(false);
        }
    }

    public void invalidateLink() {
        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return;

        if (!isChunkLoaded(targetWorld, linkX, linkZ)) return;

        BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (te instanceof TileMirrorEssentia target) {
            target.linked = false;
            target.linkedFacing = Direction.DOWN;
            setChanged();
            target.setChanged();
            target.syncTile(false);
        }
    }

    public boolean isLinkValid() {
        if (!linked) return false;

        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return false;

        BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (!(te instanceof TileMirrorEssentia target)) {
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

    public boolean isLinkValidSimple() {
        if (!linked) return false;

        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return false;

        BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (!(te instanceof TileMirrorEssentia target)) return false;

        return target.linked &&
               target.linkX == worldPosition.getX() &&
               target.linkY == worldPosition.getY() &&
               target.linkZ == worldPosition.getZ() &&
               target.linkDimension.equals(level.dimension());
    }

    public boolean isDestinationValid() {
        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return false;

        BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (!(te instanceof TileMirrorEssentia target)) {
            linked = false;
            setChanged();
            syncTile(false);
            return false;
        }

        return !target.isLinkValid();
    }

    // ==================== Instability ====================

    protected void addInstability(Level targetWorld, int amount) {
        instability += amount;
        setChanged();

        if (targetWorld != null) {
            BlockEntity te = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
            if (te instanceof TileMirrorEssentia target) {
                target.instability += amount;
                if (target.instability < 0) target.instability = 0;
                target.setChanged();
            }
        }
    }

    private void checkInstability() {
        if (instability > INSTABILITY_THRESHOLD) {
            AuraHelper.polluteAura(level, worldPosition, 1.0f, true);
            instability -= INSTABILITY_THRESHOLD;
            setChanged();
        }

        if (instability > 0 && count % 100 == 0) {
            instability--;
        }
    }

    // ==================== IAspectSource ====================

    @Override
    public AspectList getAspects() {
        return null;
    }

    @Override
    public void setAspects(AspectList aspects) {
        // Not used
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        if (!isLinkValid()) return false;

        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return false;

        // Update linked facing if needed
        if (linkedFacing == Direction.DOWN) {
            linkedFacing = getFacingAt(targetWorld, new BlockPos(linkX, linkY, linkZ));
        }

        // Check if the linked mirror's network can accept this essentia
        BlockEntity linked = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (linked == null) return false;
        return EssentiaHandler.canAcceptEssentia(linked, aspect, linkedFacing, 8, false);
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        if (!isLinkValid() || amount > 1) return amount;

        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return amount;

        if (linkedFacing == Direction.DOWN) {
            linkedFacing = getFacingAt(targetWorld, new BlockPos(linkX, linkY, linkZ));
        }

        BlockEntity linked = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (linked == null) return amount;

        if (EssentiaHandler.addEssentia(linked, aspect, linkedFacing, 8, false, 1)) {
            addInstability(null, amount);
            return 0;
        }
        return amount;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (!isLinkValid() || amount > 1) return false;

        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return false;

        if (linkedFacing == Direction.DOWN) {
            linkedFacing = getFacingAt(targetWorld, new BlockPos(linkX, linkY, linkZ));
        }

        BlockEntity linked = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (linked == null) return false;

        if (EssentiaHandler.drainEssentia(linked, aspect, linkedFacing, 8, true, 1)) {
            addInstability(null, amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList aspects) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        if (!isLinkValid() || amount > 1) return false;

        ServerLevel targetWorld = getTargetWorld();
        if (targetWorld == null) return false;

        if (linkedFacing == Direction.DOWN) {
            linkedFacing = getFacingAt(targetWorld, new BlockPos(linkX, linkY, linkZ));
        }

        BlockEntity linked = targetWorld.getBlockEntity(new BlockPos(linkX, linkY, linkZ));
        if (linked == null) return false;
        return EssentiaHandler.findEssentia(linked, aspect, linkedFacing, 8, false);
    }

    @Override
    public boolean doesContainerContain(AspectList aspects) {
        return false;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return 0;
    }

    @Override
    public boolean isBlocked() {
        return false;
    }

    // ==================== Helpers ====================

    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    private Direction getFacingAt(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.DOWN;
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
}




