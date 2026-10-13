package thaumcraft.common.items.wands.foci;

/**
 * treasure focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts PROJECTILE + BREAK (FocusEngine nodes).
 */
public class ItemFocusTreasure extends ItemFocusPreset {

    public ItemFocusTreasure() {
        super(25, "thaumcraft.PROJECTILE", "thaumcraft.BREAK");
    }
}

