package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Infested Champion Modifier - Champion mobs that spawn silverfish on death.
 * 
 * Effects:
 * - Spawns 2-4 silverfish when killed
 * - Shows crawling/insect particles
 * 
 * Type: 2 (Physical)
 */
public class ChampionModInfested implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Check if this damage will kill the champion
        if (!champion.level().isClientSide && champion.getHealth() - amount <= 0) {
            // Spawn silverfish on death
            int count = 2 + champion.level().random.nextInt(3);
            for (int i = 0; i < count; i++) {
                Silverfish silverfish = EntityType.SILVERFISH.create(champion.level());
                if (silverfish != null) {
                    double offsetX = (champion.level().random.nextDouble() - 0.5) * champion.getBbWidth();
                    double offsetZ = (champion.level().random.nextDouble() - 0.5) * champion.getBbWidth();
                    silverfish.setPos(champion.getX() + offsetX, champion.getY(), champion.getZ() + offsetZ);
                    champion.level().addFreshEntity(silverfish);
                }
            }
        }
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().random.nextInt(5) != 0) {
            return;
        }
        
        // Crawling/insect particles
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight() * 0.3;
        
        champion.level().addParticle(ParticleTypes.MYCELIUM,
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, 0, 0);
    }
}




