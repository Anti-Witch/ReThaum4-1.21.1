package thaumcraft.common.entities.monster.tainted;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import thaumcraft.init.ModEntities;

/**
 * EntityTaintacleSmall - A smaller, weaker taintacle spawned by large ones.
 * Has a limited lifespan and doesn't drop loot.
 */
public class EntityTaintacleSmall extends EntityTaintacle {
    
    private int lifetime = 200; // 10 seconds
    
    public EntityTaintacleSmall(EntityType<? extends EntityTaintacleSmall> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.length = 5; // Shorter than regular taintacles
    }
    
    public EntityTaintacleSmall(Level level) {
        super(ModEntities.TAINTACLE_SMALL.get(), level);
        this.xpReward = 0;
        this.length = 5; // Shorter than regular taintacles
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return EntityTaintacle.createAttributes()
                .add(Attributes.MAX_HEALTH, 5.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Die after lifetime expires
        if (!level().isClientSide) {
            if (lifetime-- <= 0) {
                hurt(damageSources().magic(), 10.0f);
            }
        }
    }
    
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnType) {
        return false; // Only spawned by parent taintacle
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        // No drops
    }
    
    @Override
    protected void spawnSmallTentacle(net.minecraft.world.entity.Entity target) {
        // Small taintacles don't spawn more taintacles
    }
}




