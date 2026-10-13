package thaumcraft.common.items.consumables;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.entities.projectile.EntityAlumentum;

/**
 * Alumentum - Throwable explosive item.
 * When thrown, creates an explosion on impact.
 * 
 * Ported to 1.20.1
 */
public class ItemAlumentum extends Item {
    
    public ItemAlumentum() {
        super(new Properties().stacksTo(64));
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Consume item if not creative
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        
        // Play throw sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EGG_THROW, SoundSource.PLAYERS,
                0.3f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f));
        
        // Spawn projectile on server
        if (!level.isClientSide) {
            EntityAlumentum alumentum = new EntityAlumentum(level, player);
            alumentum.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0f, 0.4f, 2.0f);
            level.addFreshEntity(alumentum);
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}




