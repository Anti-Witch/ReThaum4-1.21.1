package thaumcraft.common.entities;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.init.ModEntities;

/**
 * EntitySpecialItem - A floating item entity that resists gravity.
 * Used for special item drops that should hover instead of fall.
 */
public class EntitySpecialItem extends ItemEntity {
    
    /** Random offset for bob animation, initialized on creation */
    public final float bobOffs;
    
    public EntitySpecialItem(EntityType<? extends EntitySpecialItem> type, Level level) {
        super(type, level);
        this.bobOffs = (float)(Math.random() * Math.PI * 2.0);
    }
    
    public EntitySpecialItem(Level level) {
        super(ModEntities.SPECIAL_ITEM.get(), level);
        this.bobOffs = (float)(Math.random() * Math.PI * 2.0);
    }
    
    public EntitySpecialItem(Level level, double x, double y, double z, ItemStack stack) {
        super(ModEntities.SPECIAL_ITEM.get(), level);
        this.bobOffs = (float)(Math.random() * Math.PI * 2.0);
        this.setPos(x, y, z);
        this.setItem(stack);
        this.setYRot((float)(Math.random() * 360.0));
        this.setDeltaMovement(
                (Math.random() * 0.2 - 0.1),
                0.2,
                (Math.random() * 0.2 - 0.1)
        );
    }
    
    @Override
    public void tick() {
        // Skip first tick, then apply anti-gravity
        if (tickCount > 1) {
            // Dampen upward motion
            if (getDeltaMovement().y > 0) {
                setDeltaMovement(getDeltaMovement().multiply(1.0, 0.9, 1.0));
            }
            // Apply upward anti-gravity force
            setDeltaMovement(getDeltaMovement().add(0, 0.04, 0));
            super.tick();
        }
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Immune to explosion damage
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        return super.hurt(source, amount);
    }
}




