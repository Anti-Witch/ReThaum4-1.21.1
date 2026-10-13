package thaumcraft.common.items.wands.foci;

/**
 * equal trade focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts TOUCH + EXCHANGE (FocusEngine nodes).
 */
public class ItemFocusEqualTrade extends ItemFocusPreset {

    public ItemFocusEqualTrade() {
        super(25, "thaumcraft.TOUCH", "thaumcraft.EXCHANGE");
    }
}

