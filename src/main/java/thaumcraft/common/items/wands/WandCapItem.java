package thaumcraft.common.items.wands;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.WandCap;

/**
 * WandCapItem - The cap component of a TC4-style wand.
 *
 * Each registered cap item (iron, gold, thaumium, void, etc.) is a
 * WandCapItem. The cap definitions (cost modifier discount, texture) live in
 * {@link WandCap#caps} and are keyed by cap tag. The wand (see
 * {@link WandItem}) stores the cap tag on its stack, so this item only needs
 * to identify the cap type when combined at the wand workbench.
 *
 * NOTE: This class has no counterpart in the reference 1.20.1 project (which
 * uses a FocusEngine/caster system). It is part of the TC4-style wand system
 * this port implements; mechanics follow Thaumcraft 4 as expressed in the
 * existing thaumcraft.api.wands package.
 */
public class WandCapItem extends Item {

    public WandCapItem(Properties properties) {
        super(properties);
    }

}