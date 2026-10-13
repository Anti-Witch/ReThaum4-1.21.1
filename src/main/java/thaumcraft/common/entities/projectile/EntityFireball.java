package thaumcraft.common.entities.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.init.ModEntities;

/**
 * EntityFireball - Generic fireball projectile.
 */
public class EntityFireball extends ThrowableItemProjectile {

    public EntityFireball(EntityType<? extends EntityFireball> type, Level level) {
        super(type, level);
    }

    public EntityFireball(Level level, LivingEntity shooter) {
        super(ModEntities.FIREBALL.get(), shooter, level);
    }

    public EntityFireball(Level level, double x, double y, double z) {
        super(ModEntities.FIREBALL.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (result.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) result;
                level().explode(this, getX(), getY(), getZ(), 1.0f, Level.ExplosionInteraction.TNT);
            } else if (result.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) result;
                entityHit.getEntity().hurt(damageSources().thrown(this, getOwner()), 3.0f);
            }
            discard();
        }
    }
}