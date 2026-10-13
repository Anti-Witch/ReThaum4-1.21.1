package thaumcraft.api.wands;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * IWandable - Interface for blocks/tiles that can be interacted with using a wand.
 *
 * Add this to a tile entity or block that you wish wands to interact with.
 *
 * @author Azanor
 */
public interface IWandable {

    /**
     * Called when a player right-clicks this block/tile with a wand.
     *
     * @param world The world
     * @param wandStack The wand item stack
     * @param player The player
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param side The side clicked
     * @param md Block metadata/state data
     * @return >= 0 if handled, -1 if not handled (allows normal wand behavior)
     */
    int onWandRightClick(Level world, ItemStack wandStack, Player player, int x, int y, int z, int side, int md);

    /**
     * Called when a player right-clicks in the air with a wand while looking at this block/tile.
     *
     * @param world The world
     * @param wandStack The wand item stack
     * @param player The player
     * @return The modified wand stack, or null if not handled
     */
    ItemStack onWandRightClick(Level world, ItemStack wandStack, Player player);

    /**
     * Called every tick while the player is using the wand on this block/tile.
     *
     * @param wandStack The wand item stack
     * @param player The player
     * @param count The use duration count
     */
    void onUsingWandTick(ItemStack wandStack, Player player, int count);

    /**
     * Called when the player stops using the wand on this block/tile.
     *
     * @param wandStack The wand item stack
     * @param world The world
     * @param player The player
     * @param count The use duration count
     */
    void onWandStoppedUsing(ItemStack wandStack, Level world, Player player, int count);
}