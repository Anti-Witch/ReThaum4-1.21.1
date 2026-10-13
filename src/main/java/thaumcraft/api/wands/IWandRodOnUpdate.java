package thaumcraft.api.wands;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

/**
 * Called whenever the wand's onUpdate tick is run (server-side).
 * Allows rods to have custom behavior.
 */
public interface IWandRodOnUpdate {
    void onUpdate(ItemStack wandStack, Player player);
}
