package thaumcraft.common.items.casters.foci;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import thaumcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

/**
 * Curse Focus Effect - Deals damage and applies multiple negative effects.
 * Effects include poison, slowness, weakness, mining fatigue, hunger, and bad luck.
 */
public class FocusEffectCurse extends FocusEffect {

    @Override
    public String getResearch() {
        return "FOCUSCURSE";
    }

    @Override
    public String getKey() {
        return "thaumcraft.CURSE";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.DEATH;
    }

    @Override
    public int getComplexity() {
        return getSettingValue("duration") + getSettingValue("power") * 3;
    }

    @Override
    public float getDamageForDisplay(float finalPower) {
        return (1.0f + getSettingValue("power")) * finalPower;
    }

    @Override
    public boolean execute(HitResult target, @Nullable Trajectory trajectory, float finalPower, int num) {
        if (getPackage() == null || getPackage().world == null) {
            return false;
        }
        
        Level world = getPackage().world;
        
        // TODO: Send particle effect packet
        // PacketHandler.sendToAllAround(new PacketFXBlockBamf(...))
        
        if (target.getType() == HitResult.Type.ENTITY && target instanceof EntityHitResult entityHit) {
            Entity hitEntity = entityHit.getEntity();
            
            if (hitEntity == null) {
                return false;
            }
            
            float damage = getDamageForDisplay(finalPower);
            int duration = 20 * getSettingValue("duration");
            int effectLevel = Math.max(0, (int)(getSettingValue("power") * finalPower / 2.0f));
            
            // Apply damage
            Entity caster = getCaster();
            DamageSource damageSource;
            if (caster != null) {
                damageSource = world.damageSources().indirectMagic(hitEntity, caster);
            } else {
                damageSource = world.damageSources().magic();
            }
            
            hitEntity.hurt(damageSource, damage);
            
            // Apply curse effects to living entities
            if (hitEntity instanceof LivingEntity living) {
                // Poison always applied
                living.addEffect(new MobEffectInstance(MobEffects.POISON, duration, effectLevel));
                
                // Random chance for additional effects, decreasing probability
                float chance = 0.85f;
                
                if (world.random.nextFloat() < chance) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, effectLevel));
                    chance -= 0.15f;
                }
                
                if (world.random.nextFloat() < chance) {
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, effectLevel));
                    chance -= 0.15f;
                }
                
                if (world.random.nextFloat() < chance) {
                    living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration * 2, effectLevel));
                    chance -= 0.15f;
                }
                
                if (world.random.nextFloat() < chance) {
                    living.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration * 3, effectLevel));
                    chance -= 0.15f;
                }
                
                if (world.random.nextFloat() < chance) {
                    living.addEffect(new MobEffectInstance(MobEffects.UNLUCK, duration * 3, effectLevel));
                }
            }
            
            return true;
        } else {
            // Block curse effect - spawn sap effect blocks in area
            BlockPos hitPos = BlockPos.containing(target.getLocation());
            int power = getSettingValue("power");
            int radius = 1 + power / 2;
            
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = hitPos.offset(x, 0, z);
                    // Find surface
                    for (int y = 2; y >= -2; y--) {
                        BlockPos surfacePos = checkPos.offset(0, y, 0);
                        BlockPos abovePos = surfacePos.above();
                        
                        if (!world.isEmptyBlock(surfacePos) && world.isEmptyBlock(abovePos)) {
                            if (world.random.nextFloat() < 0.7f) {
                                world.setBlock(abovePos, ModBlocks.EFFECT_SAP.get().defaultBlockState(), 3);
                                world.scheduleTick(abovePos, ModBlocks.EFFECT_SAP.get(), 40 + world.random.nextInt(60));
                            }
                            break;
                        }
                    }
                }
            }
            return true;
        }
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
            new NodeSetting("power", "focus.common.power", 
                new NodeSetting.NodeSettingIntRange(1, 5)),
            new NodeSetting("duration", "focus.common.duration", 
                new NodeSetting.NodeSettingIntRange(1, 10))
        };
    }

    @Override
    public void renderParticleFX(Level level, double posX, double posY, double posZ,
                                  double motionX, double motionY, double motionZ) {
        // TODO: Implement particle effects
        // Original used dark red/purple curse particles
        // For now, this is a placeholder - will need client-side particle system
    }

    @Override
    public void onCast(Entity caster) {
        if (caster != null && caster.level() != null) {
            caster.level().playSound(null, caster.blockPosition().above(), 
                SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 
                0.15f, 1.0f + caster.level().random.nextFloat() / 2.0f);
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




