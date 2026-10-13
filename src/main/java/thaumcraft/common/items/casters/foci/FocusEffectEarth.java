package thaumcraft.common.items.casters.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;

import javax.annotation.Nullable;

/**
 * Earth Focus Effect - Deals damage to entities and can break blocks.
 */
public class FocusEffectEarth extends FocusEffect {

    @Override
    public String getResearch() {
        return "FOCUSELEMENTAL";
    }

    @Override
    public String getKey() {
        return "thaumcraft.EARTH";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.EARTH;
    }

    @Override
    public int getComplexity() {
        return getSettingValue("power") * 3;
    }

    @Override
    public float getDamageForDisplay(float finalPower) {
        return 2 * getSettingValue("power") * finalPower;
    }

    @Override
    public boolean execute(HitResult target, @Nullable Trajectory trajectory, float finalPower, int num) {
        if (getPackage() == null || getPackage().world == null) {
            return false;
        }
        
        Level world = getPackage().world;
        
        // TODO: Send particle effect packet
        // PacketHandler.sendToAllAround(new PacketFXFocusPartImpact(...))
        
        if (target.getType() == HitResult.Type.ENTITY && target instanceof EntityHitResult entityHit) {
            Entity hitEntity = entityHit.getEntity();
            
            if (hitEntity == null) {
                return false;
            }
            
            float damage = getDamageForDisplay(finalPower);
            
            // Create damage source
            Entity caster = getCaster();
            DamageSource damageSource;
            if (caster != null) {
                damageSource = world.damageSources().thrown(hitEntity, caster);
            } else {
                damageSource = world.damageSources().magic();
            }
            
            hitEntity.hurt(damageSource, damage);
            return true;
        } 
        else if (target.getType() == HitResult.Type.BLOCK && target instanceof BlockHitResult blockHit) {
            // Try to break the block if the caster is a player
            Entity caster = getCaster();
            if (caster instanceof ServerPlayer player) {
                BlockPos pos = blockHit.getBlockPos();
                BlockState state = world.getBlockState(pos);
                
                // Check if block hardness is low enough to break
                float hardness = state.getDestroySpeed(world, pos);
                float maxHardness = getDamageForDisplay(finalPower) / 25.0f;
                
                if (hardness >= 0 && hardness <= maxHardness) {
                    // Schedule block break with delay for visual effect
                    // In original, this used ServerEvents.addBreaker for animated breaking
                    // For now, we'll do immediate break with proper drops
                    if (canBreakBlock(player, world, pos, state)) {
                        breakBlock(player, (ServerLevel) world, pos, state);
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if the player can break the block at the given position.
     */
    private boolean canBreakBlock(ServerPlayer player, Level world, BlockPos pos, BlockState state) {
        // Check if block is unbreakable
        if (state.getDestroySpeed(world, pos) < 0) {
            return false;
        }
        
        // Check if the block is protected (spawn protection, etc.)
        if (!world.mayInteract(player, pos)) {
            return false;
        }
        
        // Check game rules and adventure mode
        if (!player.mayBuild()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Breaks the block and handles drops.
     */
    private void breakBlock(ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        Block block = state.getBlock();
        
        // Drop items
        block.playerDestroy(world, player, pos, state, blockEntity, ItemStack.EMPTY);
        
        // Remove the block
        world.destroyBlock(pos, false);
        
        // Play break sound
        world.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
            new NodeSetting("power", "focus.common.power", 
                new NodeSetting.NodeSettingIntRange(1, 5))
        };
    }

    @Override
    public void renderParticleFX(Level level, double posX, double posY, double posZ,
                                  double motionX, double motionY, double motionZ) {
        // TODO: Implement particle effects
        // Original used FXDispatcher.GenPart with rock/earth particles
        // For now, this is a placeholder - will need client-side particle system
    }

    @Override
    public void onCast(Entity caster) {
        if (caster != null && caster.level() != null) {
            caster.level().playSound(null, caster.blockPosition().above(), 
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 
                0.25f, 1.0f + (float)(caster.level().random.nextGaussian() * 0.05));
        }
    }
    
    /**
     * Gets the caster entity from the focus package.
     */
    private Entity getCaster() {
        if (getPackage() == null || getPackage().getCasterUUID() == null) {
            return null;
        }
        if (getPackage().world != null) {
            for (Player player : getPackage().world.players()) {
                if (player.getUUID().equals(getPackage().getCasterUUID())) {
                    return player;
                }
            }
        }
        return null;
    }
}




