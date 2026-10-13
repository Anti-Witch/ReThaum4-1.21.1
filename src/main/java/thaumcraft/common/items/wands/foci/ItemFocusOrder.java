package thaumcraft.common.items.wands.foci;

/**
 * order focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts TOUCH + HEAL (FocusEngine nodes).
 */
public class ItemFocusOrder extends ItemFocusPreset {

    public ItemFocusOrder() {
        super(25, "thaumcraft.TOUCH", "thaumcraft.HEAL");
    }
}

