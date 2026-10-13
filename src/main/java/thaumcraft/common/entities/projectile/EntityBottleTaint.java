package thaumcraft.common.entities.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.world.taint.BlockFluxGoo;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModEffects;
import thaumcraft.init.ModEntities;

import java.util.List;

/**
 * EntityBottleTaint - Taint bottle throwable projectile.
 * When it hits, it:
 * 1. Applies flux taint potion effect to nearby living entities (except tainted/undead)
 * 2. Places flux goo blocks in the area
 */
public class EntityBottleTaint extends ThrowableProjectile {
    
    public EntityBottleTaint(EntityType<? extends EntityBottleTaint> type, Level level) {
        super(type, level);
    }
    
    public EntityBottleTaint(Level level, LivingEntity owner) {
        super(ModEntities.BOTTLE_TAINT.get(), owner, level);
    }
    
    public EntityBottleTaint(Level level, double x, double y, double z) {
        super(ModEntities.BOTTLE_TAINT.get(), x, y, z, level);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No additional synced data needed
    }
    
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            // Spawn taint explosion particles
            for (int a = 0; a < 100; a++) {
                double offsetX = (random.nextDouble() - 0.5) * 2.0;
                double offsetY = (random.nextDouble() - 0.5) * 2.0;
                double offsetZ = (random.nextDouble() - 0.5) * 2.0;
                
                // Purple/taint colored particles
                level().addParticle(
                    ParticleTypes.WITCH,
                    getX() + offsetX,
                    getY() + offsetY,
                    getZ() + offsetZ,
                    offsetX * 0.1,
                    offsetY * 0.1,
                    offsetZ * 0.1
                );
            }
            
            // Bottle break particles
            level().addParticle(
                ParticleTypes.ITEM_SLIME,
                getX(), getY(), getZ(),
                0, 0.1, 0
            );
        }
        super.handleEntityEvent(id);
    }
    
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        
        if (!level().isClientSide) {
            // Find nearby living entities and apply flux taint effect
            AABB searchBox = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(5.0, 5.0, 5.0);
            List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, searchBox);
            
            for (LivingEntity entity : entities) {
                // Skip tainted mobs and undead
                if (!(entity instanceof ITaintedMob) && !entity.isInvertedHealAndHarm()) {
                    // Apply flux taint effect
                    entity.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, 100, 0, false, true));
                }
            }
            
            // Place flux goo blocks in the area
            for (int a = 0; a < 10; a++) {
                int xx = (int)((random.nextFloat() - random.nextFloat()) * 4.0f);
                int zz = (int)((random.nextFloat() - random.nextFloat()) * 4.0f);
                BlockPos pos = blockPosition().offset(xx, 0, zz);
                
                if (random.nextBoolean()) {
                    // Try to place at current level or one below
                    if (canPlaceFluxGoo(pos)) {
                        placeFluxGoo(pos);
                    } else {
                        BlockPos belowPos = pos.below();
                        if (canPlaceFluxGoo(belowPos)) {
                            placeFluxGoo(belowPos);
                        }
                    }
                }
            }
            
            // Notify clients for particle effects
            level().broadcastEntityEvent(this, (byte)3);
            discard();
        }
    }
    
    /**
     * Checks if flux goo can be placed at the given position.
     * Requires solid ground below and replaceable block at position.
     */
    private boolean canPlaceFluxGoo(BlockPos pos) {
        BlockState stateBelow = level().getBlockState(pos.below());
        BlockState stateAt = level().getBlockState(pos);
        
        // Need solid ground below and air/replaceable at position
        return stateBelow.isSolidRender(level(), pos.below()) && 
               (stateAt.isAir() || stateAt.canBeReplaced());
    }
    
    /**
     * Places flux goo at the given position with a random level.
     */
    private void placeFluxGoo(BlockPos pos) {
        level().setBlockAndUpdate(pos, BlockFluxGoo.withLevel(ModBlocks.FLUX_GOO.get(), 4 + random.nextInt(4)));
    }
    
    @Override
    protected double getDefaultGravity() {
        return 0.05f; // Standard potion-like gravity
    }
}




