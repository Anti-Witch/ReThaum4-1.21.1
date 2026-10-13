package thaumcraft.common.items.wands.foci;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.common.items.casters.ItemFocus;

/**
 * ItemFocusPreset - Base class for the TC4-style preset foci.
 *
 * Each TC4 focus item (fire, frost, shock, ...) is a fixed spell: a medium
 * node plus an effect node. Unlike the blank {@link ItemFocus} (edited in the
 * Focal Manipulator), preset foci always cast their fixed package. The
 * package is rebuilt fresh on every query so shared node instances never leak
 * state between casts.
 *
 * The casting pipeline (WandItem, ItemCaster, CasterManager) reads packages
 * via {@link ItemFocus#getPackage(ItemStack)}, which falls back to
 * {@link #createPresetPackage()} when no edited package is stored on the
 * stack.
 */
public class ItemFocusPreset extends ItemFocus {

    private final String mediumKey;
    private final String effectKey;

    /**
     * @param complexity   max complexity of this focus (e.g. 25)
     * @param mediumKey    FocusEngine node key of the medium (e.g. "thaumcraft.PROJECTILE")
     * @param effectKey    FocusEngine node key of the effect (e.g. "thaumcraft.FIRE")
     */
    public ItemFocusPreset(int complexity, String mediumKey, String effectKey) {
        super(complexity);
        this.mediumKey = mediumKey;
        this.effectKey = effectKey;
    }

    /**
     * Build a fresh preset package: medium root + effect.
     */
    public FocusPackage createPresetPackage() {
        FocusPackage pack = new FocusPackage();
        if (mediumKey != null && !mediumKey.isEmpty()) {
            pack.addNode(FocusEngine.createFocusNode(mediumKey));
        }
        if (effectKey != null && !effectKey.isEmpty()) {
            pack.addNode(FocusEngine.createFocusNode(effectKey));
        }
        pack.setComplexity(10);
        return pack;
    }
}
