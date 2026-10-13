package thaumcraft.common.items.consumables;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Bottle of Taint - Throwable item that spreads taint where it lands.
 * Crafted from tainted materials.
 */
public class ItemBottleTaint extends Item {

    public ItemBottleTaint() {
        super(new Item.Properties()
                .stacksTo(8));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // Play throw sound
        player.playSound(SoundEvents.EGG_THROW, 0.5f,
                0.4f / (level.random.nextFloat() * 0.4f + 0.8f));

        if (!level.isClientSide()) {
            // TODO: Spawn EntityBottleTaint projectile
            // EntityBottleTaint bottle = new EntityBottleTaint(level, player);
            // bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0f, 0.66f, 1.0f);
            // level.addFreshEntity(bottle);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}




