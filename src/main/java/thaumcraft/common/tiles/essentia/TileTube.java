package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

import java.util.Random;

/**
 * Essentia tube tile entity - transports essentia between containers.
 * Uses suction-based mechanics to pull essentia from sources.
 */
public class TileTube extends TileThaumcraft implements IEssentiaTransport {

    public static final int TICK_FREQ = 5;

    // Visual facing for rendering
    public Direction facing = Direction.NORTH;
    
    // Which sides are open for connections
    public boolean[] openSides = { true, true, true, true, true, true };

    // Current essentia in transit
    protected Aspect essentiaType = null;
    protected int essentiaAmount = 0;

    // Suction state
    protected Aspect suctionType = null;
    protected int suction = 0;

    // Venting animation
    protected int venting = 0;
    protected int ventColor = 0;
    
    private int count = 0;

    public TileTube(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileTube(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TUBE.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        if (essentiaType != null) {
            tag.putString("Type", essentiaType.getTag());
        }
        tag.putInt("Amount", essentiaAmount);
        tag.putInt("Side", facing.ordinal());
        
        byte[] sides = new byte[6];
        for (int i = 0; i < 6; i++) {
            sides[i] = (byte) (openSides[i] ? 1 : 0);
        }
        tag.putByteArray("Open", sides);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        essentiaType = Aspect.getAspect(tag.getString("Type"));
        essentiaAmount = tag.getInt("Amount");
        facing = Direction.values()[tag.getInt("Side")];
        
        byte[] sides = tag.getByteArray("Open");
        if (sides != null && sides.length == 6) {
            for (int i = 0; i < 6; i++) {
                openSides[i] = sides[i] == 1;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (suctionType != null) {
            tag.putString("SType", suctionType.getTag());
        }
        tag.putInt("SAmount", suction);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        suctionType = Aspect.getAspect(tag.getString("SType"));
        suction = tag.getInt("SAmount");
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileTube tile) {
        if (tile.venting > 0) {
            tile.venting--;
        }

        if (tile.count == 0) {
            tile.count = level.random.nextInt(10);
        }

        if (tile.venting <= 0) {
            if (++tile.count % 2 == 0) {
                tile.calculateSuction(null, false, false);
                tile.checkVenting();
                if (tile.essentiaType != null && tile.essentiaAmount == 0) {
                    tile.essentiaType = null;
                }
            }
            if (tile.count % TICK_FREQ == 0 && tile.suction > 0) {
                tile.equalizeWithNeighbours(false);
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileTube tile) {
        if (tile.venting > 0) {
            tile.venting--;
            // Spawn vent particles
            Random r = new Random(tile.hashCode() * 4);
            float rp = r.nextFloat() * 360.0f;
            float ry = r.nextFloat() * 360.0f;
            double fx = -Mth.sin(ry / 180.0f * (float) Math.PI) * Mth.cos(rp / 180.0f * (float) Math.PI);
            double fz = Mth.cos(ry / 180.0f * (float) Math.PI) * Mth.cos(rp / 180.0f * (float) Math.PI);
            double fy = -Mth.sin(rp / 180.0f * (float) Math.PI);
            // TODO: FXDispatcher.INSTANCE.drawVentParticles(...)
        }
    }

    /**
     * Calculate suction by checking neighbors.
     */
    protected void calculateSuction(Aspect filter, boolean restrict, boolean directional) {
        suction = 0;
        suctionType = null;

        for (Direction dir : Direction.values()) {
            if (directional && facing != dir.getOpposite()) continue;
            if (!isConnectable(dir)) continue;

            BlockEntity te = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, dir);
            if (te instanceof IEssentiaTransport transport) {
                Direction opposite = dir.getOpposite();
                
                // Check if we should propagate suction from this neighbor
                if (filter != null && transport.getSuctionType(opposite) != null 
                        && transport.getSuctionType(opposite) != filter) {
                    continue;
                }
                
                if (filter == null && getEssentiaAmount(dir) > 0 
                        && transport.getSuctionType(opposite) != null 
                        && getEssentiaType(dir) != transport.getSuctionType(opposite)) {
                    continue;
                }

                int neighborSuction = transport.getSuctionAmount(opposite);
                if (neighborSuction > 0 && neighborSuction > suction + 1) {
                    Aspect st = transport.getSuctionType(opposite);
                    if (st == null) st = filter;
                    setSuction(st, restrict ? neighborSuction / 2 : neighborSuction - 1);
                }
            }
        }
    }

    /**
     * Check if two tubes are fighting (same suction, different aspects).
     */
    protected void checkVenting() {
        for (Direction dir : Direction.values()) {
            if (!isConnectable(dir)) continue;

            BlockEntity te = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, dir);
            if (te instanceof IEssentiaTransport transport) {
                Direction opposite = dir.getOpposite();
                int neighborSuction = transport.getSuctionAmount(opposite);
                
                // Check for suction conflict
                if (suction > 0 && (neighborSuction == suction || neighborSuction == suction - 1) 
                        && suctionType != transport.getSuctionType(opposite)
                        && !(te instanceof TileTubeFilter)) {
                    
                    // Trigger venting effect
                    int colorIndex = -1;
                    if (suctionType != null) {
                        // TODO: Get color index from aspect order config
                        colorIndex = suctionType.getColor();
                    }
                    level.blockEvent(worldPosition, getBlockState().getBlock(), 1, colorIndex);
                    venting = 40;
                }
            }
        }
    }

    /**
     * Try to pull essentia from neighbors.
     */
    protected void equalizeWithNeighbours(boolean directional) {
        if (essentiaAmount > 0) return;

        for (Direction dir : Direction.values()) {
            if (directional && facing == dir.getOpposite()) continue;
            if (!isConnectable(dir)) continue;

            BlockEntity te = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, dir);
            if (te instanceof IEssentiaTransport transport) {
                Direction opposite = dir.getOpposite();
                
                if (!transport.canOutputTo(opposite)) continue;

                // Check suction compatibility
                Aspect ourSuction = getSuctionType(null);
                Aspect theirEssentia = transport.getEssentiaType(opposite);
                
                if (ourSuction != null && theirEssentia != null && ourSuction != theirEssentia) {
                    continue;
                }
                
                if (getSuctionAmount(null) <= transport.getSuctionAmount(opposite)) {
                    continue;
                }
                
                if (getSuctionAmount(null) < transport.getMinimumSuction()) {
                    continue;
                }

                // Determine aspect to pull
                Aspect toPull = ourSuction;
                if (toPull == null) {
                    toPull = theirEssentia;
                    if (toPull == null) {
                        toPull = transport.getEssentiaType(null);
                    }
                }

                // Try to take essentia
                int taken = transport.takeEssentia(toPull, 1, opposite);
                if (taken > 0) {
                    int added = addEssentia(toPull, taken, dir);
                    if (added > 0) {
                        // Play creak sound occasionally
                        if (level.random.nextInt(100) == 0) {
                            level.blockEvent(worldPosition, getBlockState().getBlock(), 0, 0);
                        }
                        return;
                    }
                }
            }
        }
    }

    // ==================== IEssentiaTransport ====================

    @Override
    public boolean isConnectable(Direction face) {
        return face != null && openSides[face.ordinal()];
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face != null && openSides[face.ordinal()];
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return face != null && openSides[face.ordinal()];
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        suctionType = aspect;
        suction = amount;
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return suctionType;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return suction;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return essentiaType;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return essentiaAmount;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        if (canOutputTo(face) && essentiaType == aspect && essentiaAmount > 0 && amount > 0) {
            essentiaAmount--;
            if (essentiaAmount <= 0) {
                essentiaType = null;
            }
            setChanged();
            return 1;
        }
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (canInputFrom(face) && essentiaAmount == 0 && amount > 0) {
            essentiaType = aspect;
            essentiaAmount++;
            setChanged();
            return 1;
        }
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    // Uses default isBlocked() from interface

    // ==================== Block Events ====================

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 0) {
            // Creak sound
            if (level != null && level.isClientSide) {
                // TODO: Play SoundsTC.creak
                level.playLocalSound(
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        SoundEvents.WOOD_STEP, SoundSource.BLOCKS,
                        1.0f, 1.3f + level.random.nextFloat() * 0.2f, false
                );
            }
            return true;
        }
        if (id == 1) {
            // Vent effect
            if (level != null && level.isClientSide) {
                if (venting <= 0) {
                    level.playLocalSound(
                            worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS,
                            0.1f, 1.0f + level.random.nextFloat() * 0.1f, false
                    );
                }
                venting = 50;
                ventColor = (param == -1) ? 0xAAAAAA : param;
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    // ==================== Side Management ====================

    /**
     * Toggle the open state of a side.
     */
    public void toggleSide(Direction side) {
        if (side != null) {
            openSides[side.ordinal()] = !openSides[side.ordinal()];
            markDirtyAndSync();
            
            // Update connected tube
            if (level != null) {
                BlockEntity te = level.getBlockEntity(worldPosition.relative(side));
                if (te instanceof TileTube otherTube) {
                    otherTube.openSides[side.getOpposite().ordinal()] = openSides[side.ordinal()];
                    otherTube.markDirtyAndSync();
                }
            }
        }
    }

    /**
     * Check if a side can connect to an essentia transport.
     */
    public boolean canConnectSide(Direction side) {
        if (level == null) return false;
        BlockEntity te = level.getBlockEntity(worldPosition.relative(side));
        return te instanceof IEssentiaTransport;
    }

    /**
     * Cycle the visual facing direction.
     */
    public void cycleFacing() {
        int current = facing.ordinal();
        for (int i = 1; i < 20; i++) {
            Direction newFacing = Direction.values()[(current + i) % 6];
            if (canConnectSide(newFacing.getOpposite()) && isConnectable(newFacing.getOpposite())) {
                facing = newFacing;
                markDirtyAndSync();
                return;
            }
        }
    }

    // ==================== Rendering ====================

    /**
     * Custom render bounding box for rendering.
     * Note: In 1.20.1, this is accessed via TESR if needed.
     */
    public AABB getCustomRenderBoundingBox() {
        return new AABB(
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 1, worldPosition.getZ() + 1
        );
    }
}




