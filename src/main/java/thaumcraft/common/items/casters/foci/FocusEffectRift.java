package thaumcraft.common.items.casters.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.blocks.misc.BlockHole;

import javax.annotation.Nullable;

/**
 * Rift Focus Effect - Creates a temporary void tunnel through blocks.
 * The tunnel allows passage and eventually closes back up.
 * Does not work in the Outer Lands dimension.
 */
public class FocusEffectRift extends FocusEffect {

    @Override
    public String getResearch() {
        return "FOCUSRIFT";
    }

    @Override
    public String getKey() {
        return "thaumcraft.RIFT";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.ELDRITCH;
    }

    @Override
    public int getComplexity() {
        return 3 + getSettingValue("duration") / 2 + getSettingValue("depth") / 4;
    }

    @Override
    public boolean execute(HitResult target, @Nullable Trajectory trajectory, float finalPower, int num) {
        if (getPackage() == null || getPackage().world == null) {
            return false;
        }
        
        if (target.getType() != HitResult.Type.BLOCK || !(target instanceof BlockHitResult blockHit)) {
            return false;
        }
        
        Level world = getPackage().world;
        
        // TODO: Check if in Outer Lands dimension and fail if so
        // if (world.dimension() == ModDimensions.OUTER_LANDS) {
        //     world.playSound(null, blockHit.getBlockPos(), SoundsTC.wandfail, SoundSource.PLAYERS, 1.0f, 1.0f);
        //     return false;
        // }
        
        float maxDist = getSettingValue("depth") * finalPower;
        int duration = 20 * getSettingValue("duration");
        
        Direction hitSide = blockHit.getDirection();
        Direction drillDirection = hitSide.getOpposite();
        BlockPos pos = blockHit.getBlockPos();
        
        int distance = 0;
        
        // Find how far we can drill
        for (distance = 0; distance < maxDist; distance++) {
            BlockState state = world.getBlockState(pos);
            
            // Stop at unbreakable blocks, air, or blacklisted blocks
            if (state.isAir() || 
                state.getDestroySpeed(world, pos) < 0 ||
                state.is(Blocks.BEDROCK) ||
                isPortableHoleBlacklisted(state)) {
                break;
            }
            
            pos = pos.relative(drillDirection);
        }
        
        // Create the hole effect
        if (distance > 0) {
            createHole(world, blockHit.getBlockPos(), hitSide, (byte)(distance + 1), duration);
            return true;
        }
        
        return false;
    }
    
    /**
     * Creates a temporary hole through blocks.
     * Uses BlockHole which stores original block and restores it after countdown.
     */
    private void createHole(Level world, BlockPos startPos, Direction side, byte count, int duration) {
        Direction drillDir = side.getOpposite();
        BlockPos pos = startPos;
        
        // Duration in ticks (setting is in seconds)
        short durationTicks = (short)(duration * 20);
        
        for (int i = 0; i < count; i++) {
            BlockState currentState = world.getBlockState(pos);
            
            // Skip if already air or blacklisted
            if (!currentState.isAir() && 
                currentState.getDestroySpeed(world, pos) >= 0 &&
                !currentState.is(Blocks.BEDROCK) &&
                !isPortableHoleBlacklisted(currentState)) {
                
                // Create hole that stores original block and restores after duration
                // First block gets full depth, subsequent blocks get depth 1
                byte depth = (i == 0) ? count : (byte)1;
                Direction dir = (i == 0) ? side : null;
                
                BlockHole.createHole(world, pos, currentState, durationTicks, depth, dir);
            }
            
            pos = pos.relative(drillDir);
        }
        
        // Play sound
        world.playSound(null, startPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0f, 0.5f);
    }
    
    /**
     * Check if a block is blacklisted from portable hole effects.
     */
    private boolean isPortableHoleBlacklisted(BlockState state) {
        // TODO: Implement proper blacklist check from config/tags
        // For now, just check for obsidian and end portal frame
        return state.is(Blocks.OBSIDIAN) || 
               state.is(Blocks.END_PORTAL_FRAME) ||
               state.is(Blocks.END_PORTAL) ||
               state.is(Blocks.NETHER_PORTAL);
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] depth = { 8, 16, 24, 32 };
        String[] depthDesc = { "8", "16", "24", "32" };
        
        return new NodeSetting[] {
            new NodeSetting("depth", "focus.rift.depth", 
                new NodeSetting.NodeSettingIntList(depth, depthDesc)),
            new NodeSetting("duration", "focus.common.duration", 
                new NodeSetting.NodeSettingIntRange(2, 10))
        };
    }

    @Override
    public void renderParticleFX(Level level, double posX, double posY, double posZ,
                                  double motionX, double motionY, double motionZ) {
        // TODO: Implement particle effects
        // Original used blue/purple void particles
        // For now, this is a placeholder - will need client-side particle system
    }

    @Override
    public void onCast(Entity caster) {
        if (caster != null && caster.level() != null) {
            caster.level().playSound(null, caster.blockPosition().above(), 
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 
                0.2f, 0.7f);
        }
    }
}




