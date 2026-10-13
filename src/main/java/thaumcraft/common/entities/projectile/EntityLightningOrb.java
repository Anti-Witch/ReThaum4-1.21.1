package thaumcraft.common.entities.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.init.ModEntities;

/**
 * EntityLightningOrb - Lightning orb projectile.
 */
public class EntityLightningOrb extends ThrowableItemProjectile {

    public EntityLightningOrb(EntityType<? extends EntityLightningOrb> type, Level level) {
        super(type, level);
    }

    public EntityLightningOrb(Level level, LivingEntity shooter) {
        super(ModEntities.LIGHTNING_ORB.get(), shooter, level);
    }

    public EntityLightningOrb(Level level, double x, double y, double z) {
        super(ModEntities.LIGHTNING_ORB.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.LIGHTNING_ROD;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (result.getType() == HitResult.Type.BLOCK) {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        getX(), getY(), getZ(), 0, 0, 0);
            } else if (result.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) result;
                entityHit.getEntity().hurt(damageSources().thrown(this, getOwner()), 8.0f);
                entityHit.getEntity().setRemainingFireTicks(60);
            }
            discard();
        }
    }
}
