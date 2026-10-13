package thaumcraft.common.items.wands.foci;

/**
 * warding focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts PROJECTILE + HEAL (FocusEngine nodes).
 */
public class ItemFocusWarding extends ItemFocusPreset {

    public ItemFocusWarding() {
        super(25, "thaumcraft.PROJECTILE", "thaumcraft.HEAL");
    }
}

