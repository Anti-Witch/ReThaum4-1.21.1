package thaumcraft.common.items.wands.foci;

/**
 * portable hole focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts TOUCH + BREAK (FocusEngine nodes).
 */
public class ItemFocusPortableHole extends ItemFocusPreset {

    public ItemFocusPortableHole() {
        super(25, "thaumcraft.TOUCH", "thaumcraft.BREAK");
    }
}

