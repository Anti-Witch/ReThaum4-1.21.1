package thaumcraft.common.items.casters.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;

import javax.annotation.Nullable;

/**
 * Frost Focus Effect - Deals cold damage, applies slowness, and freezes water.
 */
public class FocusEffectFrost extends FocusEffect {

    @Override
    public String getResearch() {
        return "FOCUSELEMENTAL";
    }

    @Override
    public String getKey() {
        return "thaumcraft.FROST";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.COLD;
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
            
            if (hitEntity == null) {
                return false;
            }
            
            float damage = getDamageForDisplay(finalPower);
            int duration = 20 * getSettingValue("duration");
            int potency = (int)(1.0f + getSettingValue("power") * finalPower / 3.0f);
            
            // Create damage source
            Entity caster = getCaster();
            DamageSource damageSource;
            if (caster != null) {
                damageSource = world.damageSources().thrown(hitEntity, caster);
            } else {
                damageSource = world.damageSources().freeze();
            }
            
            hitEntity.hurt(damageSource, damage);
            
            // Apply slowness effect to living entities
            if (hitEntity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, potency));
            }
            
            return true;
        } 
        else if (target.getType() == HitResult.Type.BLOCK && target instanceof BlockHitResult blockHit) {
            // Freeze water in an area around the hit point
            Vec3 hitVec = target.getLocation();
            float radius = Math.min(16.0f, 2 * getSettingValue("power") * finalPower);
            
            BlockPos centerPos = blockHit.getBlockPos();
            int r = Mth.ceil(radius);
            
            for (BlockPos mutablePos : BlockPos.betweenClosed(
                    centerPos.offset(-r, -r, -r), centerPos.offset(r, r, r))) {
                
                double distSq = mutablePos.distToCenterSqr(hitVec.x, hitVec.y, hitVec.z);
                if (distSq <= radius * radius) {
                    BlockState state = world.getBlockState(mutablePos);
                    
                    // Check if it's a source water block
                    if (state.getBlock() instanceof LiquidBlock && 
                        state.getFluidState().is(Fluids.WATER) &&
                        state.getFluidState().isSource()) {
                        
                        // Replace with frosted ice
                        world.setBlockAndUpdate(mutablePos, Blocks.FROSTED_ICE.defaultBlockState());
                        // Schedule tick for ice to melt
                        world.scheduleTick(mutablePos, Blocks.FROSTED_ICE, 
                            Mth.nextInt(world.random, 60, 120));
                    }
                }
            }
            
            return true;
        }
        
        return false;
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
            new NodeSetting("power", "focus.common.power", 
                new NodeSetting.NodeSettingIntRange(1, 5)),
            new NodeSetting("duration", "focus.common.duration", 
                new NodeSetting.NodeSettingIntRange(2, 10))
        };
    }

    @Override
    public void renderParticleFX(Level level, double posX, double posY, double posZ,
                                  double motionX, double motionY, double motionZ) {
        // TODO: Implement particle effects
        // Original used FXGeneric with snow/ice particles
        // For now, this is a placeholder - will need client-side particle system
    }

    @Override
    public void onCast(Entity caster) {
        if (caster != null && caster.level() != null) {
            caster.level().playSound(null, caster.blockPosition().above(), 
                SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 
                0.2f, 1.0f + (float)(caster.level().random.nextGaussian() * 0.05));
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




