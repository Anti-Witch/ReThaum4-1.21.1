package thaumcraft.common.entities.monster.tainted;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import thaumcraft.init.ModEntities;

/**
 * EntityTaintSeedPrime - A larger, more powerful taint seed.
 * Spreads taint over a larger area and has more health.
 */
public class EntityTaintSeedPrime extends EntityTaintSeed {
    
    public EntityTaintSeedPrime(EntityType<? extends EntityTaintSeedPrime> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }
    
    public EntityTaintSeedPrime(Level level) {
        this(ModEntities.TAINT_SEED_PRIME.get(), level);
    }
    
    @Override
    protected int getArea() {
        return 2;
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        // TODO: Drop flux crystals when implemented
        spawnAtLocation(new ItemStack(Items.SLIME_BALL));
        if (random.nextBoolean()) {
            spawnAtLocation(new ItemStack(Items.SLIME_BALL));
        }
        if (random.nextBoolean()) {
            spawnAtLocation(new ItemStack(Items.SLIME_BALL));
        }
    }
}




