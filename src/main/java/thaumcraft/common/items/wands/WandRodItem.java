package thaumcraft.common.items.wands;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.WandRod;

/**
 * WandRodItem - The rod component of a TC4-style wand.
 *
 * Each registered rod item (greatwood, silverwood, obsidian, etc.) is a
 * WandRodItem. The rod definitions themselves (capacity, crafting cost,
 * texture) live in {@link WandRod#rods} and are keyed by rod tag. The wand
 * (see {@link WandItem}) stores the rod tag on its stack so this item itself
 * only needs to identify the rod type when combined at the wand workbench.
 *
 * NOTE: This class has no counterpart in the reference 1.20.1 project (which
 * uses a FocusEngine/caster system). It is part of the TC4-style wand system
 * this port implements; mechanics follow Thaumcraft 4 as expressed in the
 * existing thaumcraft.api.wands package.
 */
public class WandRodItem extends Item {

    public WandRodItem(Properties properties) {
        super(properties);
    }

}