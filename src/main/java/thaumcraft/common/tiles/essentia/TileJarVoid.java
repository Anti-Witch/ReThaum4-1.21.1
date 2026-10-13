package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.init.ModBlockEntities;

/**
 * TileJarVoid - A void jar that destroys excess essentia when full.
 * 
 * Features:
 * - Works like a normal jar but voids overflow essentia
 * - Has higher suction to pull essentia more aggressively
 * - Visual effect when voiding essentia
 * 
 * Ported from 1.12.2
 */
public class TileJarVoid extends TileJar {

    // Track voiding for visual effects
    public boolean voiding = false;
    private int voidingTimer = 0;

    public TileJarVoid(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileJarVoid(BlockPos pos, BlockState state) {
        this(ModBlockEntities.JAR_VOID.get(), pos, state);
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileJarVoid tile) {
        // Call parent tick for normal essentia filling
        TileJar.serverTick(level, pos, state, tile);

        // Update voiding visual state
        if (tile.voiding) {
            tile.voidingTimer++;
            if (tile.voidingTimer > 10) {
                tile.voiding = false;
                tile.voidingTimer = 0;
                tile.markDirtyAndSync();
            }
        }
    }

    // ==================== Override Essentia Handling ====================

    @Override
    public int addToContainer(Aspect tag, int amt) {
        if (amt == 0) return 0;

        // Check filter
        if (aspectFilter != null && tag != aspectFilter) {
            return amt; // Don't accept wrong aspect
        }

        // If jar is empty or has same aspect
        if (amount == 0 || tag == aspect) {
            aspect = tag;
            int canAdd = CAPACITY - amount;
            
            if (canAdd >= amt) {
                // Normal addition - fits completely
                amount += amt;
                markDirtyAndSync();
                return 0;
            } else {
                // Partial fit - add what we can, void the rest
                amount = CAPACITY;
                int voided = amt - canAdd;
                
                // Mark as voiding for visual effect
                if (voided > 0) {
                    voiding = true;
                    voidingTimer = 0;
                }
                
                markDirtyAndSync();
                return 0; // Return 0 because we "accepted" all of it (voided excess)
            }
        }

        return amt; // Wrong aspect type, don't accept
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (canInputFrom(face)) {
            // Void jar accepts all essentia (voids overflow)
            addToContainer(aspect, amount);
            return amount; // Report all as added (even voided)
        }
        return 0;
    }

    // ==================== Higher Suction ====================

    @Override
    public int getSuctionAmount(Direction face) {
        // Void jars have the same suction as a normal jar (1.12.2 behavior)
        if (aspectFilter != null && amount < CAPACITY) {
            return 48;
        }
        return 32;
    }

    @Override
    public int getMinimumSuction() {
        return (aspectFilter != null) ? 48 : 32;
    }

    // ==================== Fill from Above (Override for Voiding) ====================

    @Override
    protected void fillFromAbove() {
        if (level == null || level.isClientSide) return;

        var te = level.getBlockEntity(worldPosition.above());
        if (te instanceof IEssentiaTransport transport) {
            if (!transport.canOutputTo(Direction.DOWN)) return;

            Aspect toGet = null;
            if (aspectFilter != null) {
                toGet = aspectFilter;
            } else if (aspect != null && amount > 0) {
                toGet = aspect;
            } else if (transport.getEssentiaAmount(Direction.DOWN) > 0 &&
                       transport.getSuctionAmount(Direction.DOWN) < getSuctionAmount(Direction.UP) &&
                       getSuctionAmount(Direction.UP) >= transport.getMinimumSuction()) {
                toGet = transport.getEssentiaType(Direction.DOWN);
            }

            // Void jar always pulls if it has suction advantage
            if (toGet != null && transport.getSuctionAmount(Direction.DOWN) < getSuctionAmount(Direction.UP)) {
                int taken = transport.takeEssentia(toGet, 1, Direction.DOWN);
                if (taken > 0) {
                    addToContainer(toGet, taken);
                }
            }
        }
    }

    // ==================== Getters ====================

    public boolean isVoiding() {
        return voiding;
    }
}




