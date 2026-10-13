package thaumcraft.common.items.wands.foci;

/**
 * earth focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts TOUCH + EARTH (FocusEngine nodes).
 */
public class ItemFocusEarth extends ItemFocusPreset {

    public ItemFocusEarth() {
        super(25, "thaumcraft.TOUCH", "thaumcraft.EARTH");
    }
}

