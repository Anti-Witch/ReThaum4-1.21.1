package thaumcraft.common.items.wands.foci;

/**
 * air focus - casts a preset spell when fitted to a wand.
 *
 * TC4-style preset focus: always casts PROJECTILE + AIR (FocusEngine nodes).
 */
public class ItemFocusAir extends ItemFocusPreset {

    public ItemFocusAir() {
        super(25, "thaumcraft.PROJECTILE", "thaumcraft.AIR");
    }
}

