package thaumcraft.common.items.wands.foci;

/**
 * water focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts TOUCH + FROST (FocusEngine nodes).
 */
public class ItemFocusWater extends ItemFocusPreset {

    public ItemFocusWater() {
        super(25, "thaumcraft.TOUCH", "thaumcraft.FROST");
    }
}

