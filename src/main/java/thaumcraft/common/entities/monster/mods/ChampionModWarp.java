package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

/**
 * Warp Champion Modifier - Champion mobs that inflict warp on hit.
 * 
 * Effects:
 * - Inflicts temporary warp on player targets
 * - Shows purple/eldritch particles
 * 
 * Type: 1 (Offensive)
 */
public class ChampionModWarp implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Inflict warp on player targets
        if (target instanceof Player player && !player.level().isClientSide) {
            IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
            if (warp != null && player.level().random.nextInt(3) == 0) {
                warp.add(IPlayerWarp.EnumWarpType.TEMPORARY, 1);
            }
        }
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().random.nextInt(3) != 0) {
            return;
        }
        
        // Purple eldritch particles
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight();
        
        champion.level().addParticle(ParticleTypes.PORTAL,
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                (champion.level().random.nextDouble() - 0.5) * 0.5,
                (champion.level().random.nextDouble() - 0.5) * 0.5,
                (champion.level().random.nextDouble() - 0.5) * 0.5);
    }
}




