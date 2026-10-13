package thaumcraft.common.items.wands.foci;

/**
 * excavation focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts TOUCH + BREAK (FocusEngine nodes).
 */
public class ItemFocusExcavation extends ItemFocusPreset {

    public ItemFocusExcavation() {
        super(25, "thaumcraft.TOUCH", "thaumcraft.BREAK");
    }
}

