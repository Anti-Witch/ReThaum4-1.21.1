package thaumcraft.common.items.wands.foci;

/**
 * entropy focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts PROJECTILE + CURSE (FocusEngine nodes).
 */
public class ItemFocusEntropy extends ItemFocusPreset {

    public ItemFocusEntropy() {
        super(25, "thaumcraft.PROJECTILE", "thaumcraft.CURSE");
    }
}

