package thaumcraft.common.items.wands.foci;

/**
 * frost focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts PROJECTILE + FROST (FocusEngine nodes).
 */
public class ItemFocusFrost extends ItemFocusPreset {

    public ItemFocusFrost() {
        super(25, "thaumcraft.PROJECTILE", "thaumcraft.FROST");
    }
}

