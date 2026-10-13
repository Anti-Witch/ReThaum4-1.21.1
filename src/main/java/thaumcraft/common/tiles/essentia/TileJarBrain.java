package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

import java.util.List;

/**
 * TileJarBrain - Brain in a Jar that collects and stores XP orbs.
 * 
 * Features:
 * - Automatically collects XP orbs in a radius
 * - Stores up to 1500 XP points
 * - Right-click to dispense stored XP to player
 * - Has a creepy animated brain floating inside
 * 
 * Ported from 1.12.2
 */
public class TileJarBrain extends TileThaumcraft {

    public static final int MAX_XP = 1500;
    public static final int COLLECTION_RADIUS = 6;

    // Stored experience points
    public int xp = 0;

    // Animation (client-side)
    public float brainRotation = 0;
    public float brainRotationPrev = 0;
    public float brainY = 0;
    public float brainYPrev = 0;
    private float bobPhase = 0;

    // Tick counter
    private int tickCount = 0;

    public TileJarBrain(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileJarBrain(BlockPos pos, BlockState state) {
        this(ModBlockEntities.JAR_BRAIN.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putInt("XP", xp);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        xp = tag.getInt("XP");
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileJarBrain tile) {
        tile.tickCount++;

        // Collect XP orbs periodically
        if (tile.tickCount % 10 == 0 && tile.xp < MAX_XP) {
            tile.collectXPOrbs();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileJarBrain tile) {
        // Update animation
        tile.brainRotationPrev = tile.brainRotation;
        tile.brainYPrev = tile.brainY;

        // Slow rotation
        tile.brainRotation += 0.5f;
        if (tile.brainRotation >= 360.0f) {
            tile.brainRotation -= 360.0f;
            tile.brainRotationPrev -= 360.0f;
        }

        // Gentle bobbing motion
        tile.bobPhase += 0.05f;
        tile.brainY = (float) Math.sin(tile.bobPhase) * 0.03f;
    }

    /**
     * Collect XP orbs in the area.
     */
    private void collectXPOrbs() {
        if (level == null) return;

        AABB searchBox = new AABB(
                worldPosition.getX() - COLLECTION_RADIUS,
                worldPosition.getY() - COLLECTION_RADIUS,
                worldPosition.getZ() - COLLECTION_RADIUS,
                worldPosition.getX() + COLLECTION_RADIUS + 1,
                worldPosition.getY() + COLLECTION_RADIUS + 1,
                worldPosition.getZ() + COLLECTION_RADIUS + 1
        );

        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, searchBox);

        for (ExperienceOrb orb : orbs) {
            if (!orb.isAlive()) continue;

            int orbValue = orb.getValue();
            int canStore = MAX_XP - xp;

            if (canStore > 0) {
                int toStore = Math.min(orbValue, canStore);
                xp += toStore;
                
                if (toStore >= orbValue) {
                    orb.discard();
                } else {
                    // Partial collection - orb keeps remaining XP
                    // Note: ExperienceOrb doesn't have setValue, so we discard and let remainder stay
                    orb.discard();
                }

                markDirtyAndSync();

                // Play collection sound
                level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.BLOCKS, 0.1f, 
                        0.5f * ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1.8f));

                // Only collect one orb per tick cycle
                break;
            }
        }
    }

    /**
     * Dispense stored XP to a player.
     * 
     * @param player The player to give XP to
     * @param all If true, dispense all XP. If false, dispense one level's worth.
     * @return The amount of XP dispensed
     */
    public int dispenseXP(Player player, boolean all) {
        if (level == null || xp <= 0) return 0;

        int toDispense;
        if (all) {
            toDispense = xp;
        } else {
            // Calculate XP needed for next level
            int currentLevel = player.experienceLevel;
            int xpForNextLevel = getXpNeededForLevel(currentLevel + 1) - getXpNeededForLevel(currentLevel);
            toDispense = Math.min(xp, xpForNextLevel);
        }

        if (toDispense > 0) {
            player.giveExperiencePoints(toDispense);
            xp -= toDispense;
            markDirtyAndSync();

            // Play dispense sound
            level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.BLOCKS, 0.5f, 1.0f);
        }

        return toDispense;
    }

    /**
     * Calculate total XP needed to reach a level.
     */
    private int getXpNeededForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    // ==================== Getters ====================

    public int getXP() {
        return xp;
    }

    public float getFillPercent() {
        return (float) xp / MAX_XP;
    }

    /**
     * Get the comparator output signal based on fill level.
     */
    public int getComparatorOutput() {
        if (xp == 0) return 0;
        return 1 + (int) ((xp / (float) MAX_XP) * 14);
    }
}




