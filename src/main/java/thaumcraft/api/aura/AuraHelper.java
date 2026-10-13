package thaumcraft.api.aura;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXPollute;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Public API for interacting with the Thaumcraft aura system.
 * Use these methods to query and modify vis/flux in the aura.
 */
public class AuraHelper {

    /**
     * Consume vis from the aura at the given location.
     *
     * @param level the world
     * @param pos the position to drain from
     * @param amount the amount to drain
     * @param simulate if true, only simulates the drain without actually removing vis
     * @return how much was actually drained
     */
    public static float drainVis(Level level, BlockPos pos, float amount, boolean simulate) {
        return AuraHandler.drainVis(level, pos, amount, simulate);
    }

    /**
     * Consume flux from the aura at the given location.
     * Added for completeness, but should really not be used. Add instability instead.
     *
     * @param level the world
     * @param pos the position to drain from
     * @param amount the amount to drain
     * @param simulate if true, only simulates the drain without actually removing flux
     * @return how much was actually drained
     */
    public static float drainFlux(Level level, BlockPos pos, float amount, boolean simulate) {
        return AuraHandler.drainFlux(level, pos, amount, simulate);
    }

    /**
     * Adds vis to the aura at the given location.
     *
     * @param level the world
     * @param pos the position to add vis at
     * @param amount the amount to add
     */
    public static void addVis(Level level, BlockPos pos, float amount) {
        AuraHandler.addVis(level, pos, amount);
    }

    /**
     * Get how much vis is in the aura at the given location.
     *
     * @param level the world
     * @param pos the position to check
     * @return the current vis amount
     */
    public static float getVis(Level level, BlockPos pos) {
        return AuraHandler.getVis(level, pos);
    }

    /**
     * Adds flux to the aura at the specified block position.
     * This pollutes the aura and can cause flux rifts if too much accumulates.
     *
     * @param level the world
     * @param pos the position to pollute
     * @param amount how much flux to add
     * @param showEffect if set to true, a flux smoke effect and sound will also be displayed
     */
    public static void polluteAura(Level level, BlockPos pos, float amount, boolean showEffect) {
        AuraHandler.addFlux(level, pos, amount);
        
        if (showEffect && level instanceof ServerLevel serverLevel) {
            // Send pollution particle effect packet
            PacketHandler.sendToAllTrackingChunk(new PacketFXPollute(pos, amount), serverLevel, pos);
            
            // Play pollution sound
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 
                    0.2f + level.random.nextFloat() * 0.1f, 
                    0.9f + level.random.nextFloat() * 0.2f);
        }
    }

    /**
     * Get how much flux is in the aura at the given location.
     *
     * @param level the world
     * @param pos the position to check
     * @return the current flux amount
     */
    public static float getFlux(Level level, BlockPos pos) {
        return AuraHandler.getFlux(level, pos);
    }

    /**
     * Gets the general aura baseline at the given location.
     * This is the maximum natural vis level for the chunk.
     *
     * @param level the world
     * @param pos the position to check
     * @return the base aura level
     */
    public static int getAuraBase(Level level, BlockPos pos) {
        return AuraHandler.getAuraBase(level, pos);
    }

    /**
     * Gets if the local aura is below 10% and that the player has the node preserver research.
     * If the passed in player is null it will ignore the need for the research to be completed
     * and just assume it is.
     *
     * @param level the world
     * @param player the player to check research for (can be null)
     * @param pos the position to check
     * @return true if aura should be preserved
     */
    public static boolean shouldPreserveAura(Level level, Player player, BlockPos pos) {
        return AuraHandler.shouldPreserveAura(level, player, pos);
    }

    /**
     * Gets the total aura (vis + flux) at the given location.
     *
     * @param level the world
     * @param pos the position to check
     * @return the total aura amount
     */
    public static float getTotalAura(Level level, BlockPos pos) {
        return AuraHandler.getTotalAura(level, pos);
    }

    /**
     * Gets the flux saturation ratio at the given location.
     * This is flux divided by the base aura level.
     *
     * @param level the world
     * @param pos the position to check
     * @return the flux saturation ratio (0.0 to potentially > 1.0)
     */
    public static float getFluxSaturation(Level level, BlockPos pos) {
        return AuraHandler.getFluxSaturation(level, pos);
    }
}




