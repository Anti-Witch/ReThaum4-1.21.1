package thaumcraft.common.entities.monster;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;

/**
 * EntityThaumicSlime - A tainted slime that can spit smaller copies at players.
 * Implements ITaintedMob interface (when available).
 * Larger slimes split into smaller ones on death.
 */
public class EntityThaumicSlime extends Slime {
    
    private int launched = 10;
    private int spitCounter = 100;
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }
    
    /**
     * Static spawn rule check for use with SpawnPlacementRegisterEvent.
     * Thaumic slimes don't spawn naturally - they are created by flux effects.
     */
    public static boolean checkThaumicSlimeSpawnRules(EntityType<? extends EntityThaumicSlime> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Thaumic slimes don't spawn naturally - only from flux effects
        return false;
    }
    
    public EntityThaumicSlime(EntityType<? extends EntityThaumicSlime> type, Level level) {
        super(type, level);
        int size = 1 << (1 + random.nextInt(3)); // 2, 4, or 8
        setSize(size, true);
    }
    
    public EntityThaumicSlime(Level level) {
        super(ModEntities.THAUMIC_SLIME.get(), level);
        int size = 1 << (1 + random.nextInt(3));
        setSize(size, true);
    }
    
    /**
     * Constructor for creating a launched slime projectile.
     */
    public EntityThaumicSlime(Level level, LivingEntity shooter, LivingEntity target) {
        super(ModEntities.THAUMIC_SLIME.get(), level);
        setSize(1, true);
        launched = 10;
        
        // Position at shooter's center
        double startY = (shooter.getBoundingBox().minY + shooter.getBoundingBox().maxY) / 2.0;
        
        // Calculate trajectory
        double dx = target.getX() - shooter.getX();
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 3.0f - startY;
        double dz = target.getZ() - shooter.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        if (dist >= 1.0E-7) {
            float yaw = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
            float pitch = (float)(-Mth.atan2(dy, dist) * Mth.RAD_TO_DEG);
            
            double normalizedX = dx / dist;
            double normalizedZ = dz / dist;
            
            setPos(shooter.getX() + normalizedX, startY, shooter.getZ() + normalizedZ);
            setYRot(yaw);
            setXRot(pitch);
            yRotO = yaw;
            xRotO = pitch;
            
            // Launch with velocity
            float arc = (float)dist * 0.2f;
            shoot(dx, dy + arc, dz, 1.5f, 1.0f);
        }
    }
    
    /**
     * Shoots this slime like a projectile.
     */
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        double dist = Math.sqrt(x * x + y * y + z * z);
        x /= dist;
        y /= dist;
        z /= dist;
        
        x += random.nextGaussian() * 0.0075 * inaccuracy;
        y += random.nextGaussian() * 0.0075 * inaccuracy;
        z += random.nextGaussian() * 0.0075 * inaccuracy;
        
        x *= velocity;
        y *= velocity;
        z *= velocity;
        
        setDeltaMovement(x, y, z);
        
        double horizDist = Math.sqrt(x * x + z * z);
        setYRot((float)(Mth.atan2(x, z) * Mth.RAD_TO_DEG));
        setXRot((float)(Mth.atan2(y, horizDist) * Mth.RAD_TO_DEG));
        yRotO = getYRot();
        xRotO = getXRot();
    }
    
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, 
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        // Random size based on difficulty
        int sizeIndex = random.nextInt(3);
        if (sizeIndex < 2 && random.nextFloat() < 0.5f * difficulty.getSpecialMultiplier()) {
            sizeIndex++;
        }
        int size = 1 << sizeIndex;
        setSize(size, true);
        
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }
    
    @Override
    public void setSize(int size, boolean resetHealth) {
        super.setSize(size, resetHealth);
        this.xpReward = size + 2;
    }
    
    private boolean wasOnGroundLastTick = false;
    
    @Override
    public void tick() {
        int size = getSize();
        
        // Custom landing behavior with particles
        if (onGround() && !wasOnGroundLastTick) {
            wasOnGroundLastTick = true;
            
            if (level().isClientSide) {
                for (int j = 0; j < size * 2; j++) {
                    spawnSlimeParticles();
                }
            }
            
            playSound(getJumpSound(), getSoundVolume(), 
                    (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f * 0.8f);
        } else if (!onGround()) {
            wasOnGroundLastTick = false;
        }
        
        super.tick();
        
        // Client-side particles for launched slimes
        if (level().isClientSide) {
            if (launched > 0) {
                launched--;
                for (int k = 0; k < size * (launched + 1); k++) {
                    spawnSlimeParticles();
                }
            }
        }
        
        // Server-side spit attack logic
        if (!level().isClientSide && isAlive()) {
            Player target = level().getNearestPlayer(this, 16.0);
            if (target != null) {
                if (spitCounter > 0) {
                    spitCounter--;
                }
                
                lookAt(target, 10.0f, 20.0f);
                
                // Spit attack if large enough and target is at range
                if (distanceTo(target) > 4.0f && spitCounter <= 0 && getSize() > 2) {
                    spitCounter = 101;
                    
                    EntityThaumicSlime spitSlime = new EntityThaumicSlime(level(), this, target);
                    level().addFreshEntity(spitSlime);
                    
                    // TODO: Play SoundsTC.gore when implemented
                    playSound(SoundEvents.SLIME_SQUISH, 1.0f, 
                            (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f * 0.8f);
                    
                    // Shrink after spitting
                    setSize(getSize() - 1, true);
                }
            }
        }
    }
    
    private void spawnSlimeParticles() {
        // Purple/taint colored particles
        level().addParticle(ParticleTypes.ITEM_SLIME,
                getX() + (random.nextDouble() - 0.5) * getBbWidth(),
                getY() + random.nextDouble() * getBbHeight(),
                getZ() + (random.nextDouble() - 0.5) * getBbWidth(),
                0, 0, 0);
    }
    
    @Override
    public void remove(RemovalReason reason) {
        int size = getSize();
        
        // Split into smaller slimes on death
        if (!level().isClientSide && size > 1 && isDeadOrDying()) {
            for (int k = 0; k < size; k++) {
                float offsetX = (k % 2 - 0.5f) * size / 4.0f;
                float offsetZ = (k / 2 - 0.5f) * size / 4.0f;
                
                EntityThaumicSlime smallSlime = new EntityThaumicSlime(ModEntities.THAUMIC_SLIME.get(), level());
                smallSlime.setSize(1, true);
                smallSlime.moveTo(getX() + offsetX, getY() + 0.5, getZ() + offsetZ, 
                        random.nextFloat() * 360.0f, 0.0f);
                level().addFreshEntity(smallSlime);
            }
        }
        
        super.remove(reason);
    }
    
    @Override
    protected float getAttackDamage() {
        return getSize() + 1;
    }
    
    @Override
    protected boolean isDealsDamage() {
        return true;
    }
    
    @Override
    protected void dealDamage(LivingEntity target) {
        int size = getSize();
        int effectiveSize = launched > 0 ? size + 2 : size;
        
        if (isAlive() && hasLineOfSight(target) && 
                distanceToSqr(target) < 0.6 * effectiveSize * 0.6 * effectiveSize) {
            
            if (target.hurt(damageSources().mobAttack(this), getAttackDamage())) {
                playSound(SoundEvents.SLIME_ATTACK, 1.0f, 
                        (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f);
                EnchantmentHelper.doPostAttackEffects((ServerLevel) this.level(), this, damageSources().mobAttack(this));
            }
        }
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        super.dropCustomDeathLoot(level, source, wasRecentlyHit);
        
        // TC4 EntityThaumicSlime: small slimes sometimes drop tainted goo
        if (getSize() < 3 && random.nextInt(3) == 0) {
            spawnAtLocation(new ItemStack(ModItems.TAINT_SLIME.get()), getBbHeight() / 2.0f);
        }
    }
    
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        // Thaumic slimes don't spawn naturally - only from flux effects
        return false;
    }
}




