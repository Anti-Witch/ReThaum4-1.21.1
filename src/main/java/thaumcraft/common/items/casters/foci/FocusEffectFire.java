package thaumcraft.common.items.casters.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;

import javax.annotation.Nullable;

/**
 * Fire Focus Effect - Deals fire damage and sets targets ablaze.
 * Can also ignite blocks.
 */
public class FocusEffectFire extends FocusEffect {

    @Override
    public String getResearch() {
        return "BASEAUROMANCY";
    }

    @Override
    public String getKey() {
        return "thaumcraft.FIRE";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.FIRE;
    }

    @Override
    public int getComplexity() {
        return getSettingValue("duration") + getSettingValue("power") * 2;
    }

    @Override
    public float getDamageForDisplay(float finalPower) {
        return (3 + getSettingValue("power")) * finalPower;
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
            
            if (hitEntity == null || hitEntity.fireImmune()) {
                return false;
            }
            
            float fireDuration = (float)(1 + getSettingValue("duration") * getSettingValue("duration"));
            float damage = getDamageForDisplay(finalPower);
            fireDuration *= finalPower;
            
            // Create fire damage source
            Entity caster = getCaster();
            DamageSource damageSource;
            if (caster != null) {
                // Use indirectMagic for spell fire damage
                damageSource = world.damageSources().indirectMagic(hitEntity, caster);
            } else {
                damageSource = world.damageSources().onFire();
            }
            
            hitEntity.hurt(damageSource, damage);
            
            if (fireDuration > 0.0f) {
                hitEntity.igniteForSeconds(fireDuration);
            }
            
            return true;
        } 
        else if (target.getType() == HitResult.Type.BLOCK && target instanceof BlockHitResult blockHit) {
            // Try to place fire on the block
            if (getSettingValue("duration") > 0) {
                BlockPos pos = blockHit.getBlockPos().relative(blockHit.getDirection());
                
                if (world.isEmptyBlock(pos) && world.random.nextFloat() < finalPower) {
                    world.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 
                            1.0f, world.random.nextFloat() * 0.4f + 0.8f);
                    world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
            new NodeSetting("power", "focus.common.power", 
                new NodeSetting.NodeSettingIntRange(1, 5)),
            new NodeSetting("duration", "focus.fire.burn", 
                new NodeSetting.NodeSettingIntRange(0, 5))
        };
    }

    @Override
    public void renderParticleFX(Level level, double posX, double posY, double posZ,
                                  double motionX, double motionY, double motionZ) {
        // TODO: Implement particle effects
        // Original used FXDispatcher.GenPart with fire particles
        // For now, this is a placeholder - will need client-side particle system
    }

    @Override
    public void onCast(Entity caster) {
        if (caster != null && caster.level() != null) {
            caster.level().playSound(null, caster.blockPosition().above(), 
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 
                1.0f, 1.0f + (float)(caster.level().random.nextGaussian() * 0.05));
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




