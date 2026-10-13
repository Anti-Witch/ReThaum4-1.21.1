package thaumcraft.common.items.casters.foci;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;

import javax.annotation.Nullable;

/**
 * Heal Focus Effect - Heals living targets, damages undead.
 */
public class FocusEffectHeal extends FocusEffect {

    @Override
    public String getResearch() {
        return "FOCUSHEAL";
    }

    @Override
    public String getKey() {
        return "thaumcraft.HEAL";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.LIFE;
    }

    @Override
    public int getComplexity() {
        return getSettingValue("power") * 4;
    }

    @Override
    public float getDamageForDisplay(float finalPower) {
        // Negative indicates healing
        return -getSettingValue("power") * finalPower;
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
            
            if (hitEntity == null || !(hitEntity instanceof LivingEntity living)) {
                return false;
            }
            
            // Undead take damage instead of healing
            if (living.isInvertedHealAndHarm()) {
                float damage = getSettingValue("power") * finalPower * 1.5f;
                
                Entity caster = getCaster();
                DamageSource damageSource;
                if (caster != null) {
                    damageSource = world.damageSources().indirectMagic(living, caster);
                } else {
                    damageSource = world.damageSources().magic();
                }
                
                living.hurt(damageSource, damage);
            } else {
                // Heal living entities
                float healAmount = getSettingValue("power") * finalPower;
                living.heal(healAmount);
            }
            
            return true;
        }
        
        return false;
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
            new NodeSetting("power", "focus.heal.power", 
                new NodeSetting.NodeSettingIntRange(1, 5))
        };
    }

    @Override
    public void renderParticleFX(Level level, double posX, double posY, double posZ,
                                  double motionX, double motionY, double motionZ) {
        // TODO: Implement particle effects
        // Original used white/golden healing particles
        // For now, this is a placeholder - will need client-side particle system
    }

    @Override
    public void onCast(Entity caster) {
        if (caster != null && caster.level() != null) {
            caster.level().playSound(null, caster.blockPosition().above(), 
                SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 
                2.0f, 2.0f + (float)(caster.level().random.nextGaussian() * 0.1));
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




