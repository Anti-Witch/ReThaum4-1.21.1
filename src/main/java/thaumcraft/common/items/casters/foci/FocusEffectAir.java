package thaumcraft.common.items.casters.foci;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.client.fx.FocusFX;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;

/**
 * Air Focus Effect - Deals damage and applies knockback to targets.
 */
public class FocusEffectAir extends FocusEffect {

    @Override
    public String getResearch() {
        return "FOCUSELEMENTAL";
    }

    @Override
    public String getKey() {
        return "thaumcraft.AIR";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.AIR;
    }

    @Override
    public int getComplexity() {
        return getSettingValue("power") * 2;
    }

    @Override
    public float getDamageForDisplay(float finalPower) {
        return (1 + getSettingValue("power")) * finalPower;
    }

    @Override
    public boolean execute(HitResult target, @Nullable Trajectory trajectory, float finalPower, int num) {
        if (getPackage() == null || getPackage().world == null) {
            return false;
        }
        
        Level world = getPackage().world;
        Vec3 hitPos = target.getLocation();

        // TC4: spawn the impact burst for every nearby client
        PacketHandler.sendToAllAround(
                new PacketFXFocusPartImpact(hitPos.x, hitPos.y, hitPos.z, new String[] { getKey() }),
                world, hitPos.x, hitPos.y, hitPos.z, 64.0);

        // Play wind sound at impact
        world.playSound(null, hitPos.x, hitPos.y, hitPos.z, 
            SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5f, 0.66f);
        
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
            
            // Apply knockback to living entities
            if (hitEntity instanceof LivingEntity living) {
                float knockbackStrength = damage * 0.25f;
                
                if (trajectory != null) {
                    // Knockback in the direction the spell was traveling
                    living.knockback(knockbackStrength, 
                        -trajectory.direction.x, 
                        -trajectory.direction.z);
                } else {
                    // Fallback: knockback based on entity rotation
                    float yawRad = hitEntity.getYRot() * ((float) Math.PI / 180F);
                    living.knockback(knockbackStrength, 
                        -Mth.sin(yawRad), 
                        Mth.cos(yawRad));
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
                new NodeSetting.NodeSettingIntRange(1, 5))
        };
    }

    @Override
    public void renderParticleFX(Level level, double posX, double posY, double posZ,
                                  double motionX, double motionY, double motionZ) {
        FocusFX.air(level, posX, posY, posZ, motionX, motionY, motionZ);
    }

    @Override
    public void onCast(Entity caster) {
        if (caster != null && caster.level() != null) {
            // TC4 FocusEffectAir.onCast: SoundsTC.wind
            caster.level().playSound(null, caster.blockPosition().above(), 
                ModSounds.WIND.get(), SoundSource.PLAYERS, 
                0.125f, 2.0f);
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




