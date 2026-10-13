package thaumcraft.common.items.wands.foci;

/**
 * shock focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts PROJECTILE + FLUX (FocusEngine nodes).
 */
public class ItemFocusShock extends ItemFocusPreset {

    public ItemFocusShock() {
        super(25, "thaumcraft.PROJECTILE", "thaumcraft.FLUX");
    }
}

