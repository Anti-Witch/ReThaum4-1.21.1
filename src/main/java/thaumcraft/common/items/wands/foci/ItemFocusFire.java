package thaumcraft.common.items.wands.foci;

/**
 * fire focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts PROJECTILE + FIRE (FocusEngine nodes).
 */
public class ItemFocusFire extends ItemFocusPreset {

    public ItemFocusFire() {
        super(25, "thaumcraft.PROJECTILE", "thaumcraft.FIRE");
    }
}

