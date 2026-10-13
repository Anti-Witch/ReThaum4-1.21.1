package thaumcraft.common.config;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.items.wands.WandRodPrimalOnUpdate;
import thaumcraft.init.ModItems;

import java.util.List;

/**
 * ConfigWands - Registers all TC4-style wand rods and caps.
 *
 * The rod/cap definitions are stored in {@link WandRod#rods} and
 * {@link WandCap#caps}, keyed by the same tags the ArcaneWandRecipe writes into
 * the {@code thaumcraft:wand_rod} / {@code thaumcraft:wand_cap} Data Components.
 * WandItem looks these up at runtime, so this init is required for the wand
 * system to function.
 *
 * Values are taken 1:1 from OpenTC4 common/config/ConfigItems:
 * <ul>
 *   <li>rods: wood 25/1, greatwood 50/3, obsidian 75/6 (earth regen),
 *       blaze 75/6 (fire regen + glow), ice 75/6 (water), quartz 75/6 (order),
 *       bone 75/6 (entropy), reed 75/6 (air), silverwood 100/9</li>
 *   <li>staff rods: greatwood 125/8, obsidian 175/14 (earth), silverwood 250/24</li>
 *   <li>caps: iron 1.1/1, gold 1.0/3, thaumium 0.9/6, void 0.8/9,
 *       copper 1.1 (order/entropy special 1.0)/2, silver 1.0 (elementals 0.95)/4</li>
 * </ul>
 * The inert caps are crafting intermediates in TC4, not functional caps.
 */
public class ConfigWands {

    public static void init() {
        // ==================== Rods ====================
        // TC4 basic rod: a plain stick
        new WandRod("wood", 25, new ItemStack(Items.STICK), 1);
        registerRod("greatwood", 50, ModItems.WAND_ROD_GREATWOOD, 3, null);
        registerRod("obsidian", 75, ModItems.WAND_ROD_OBSIDIAN, 6, new WandRodPrimalOnUpdate(Aspect.EARTH));
        registerRod("blaze", 75, ModItems.WAND_ROD_BLAZE, 6, new WandRodPrimalOnUpdate(Aspect.FIRE));
        registerRod("ice", 75, ModItems.WAND_ROD_ICE, 6, new WandRodPrimalOnUpdate(Aspect.WATER));
        registerRod("quartz", 75, ModItems.WAND_ROD_QUARTZ, 6, new WandRodPrimalOnUpdate(Aspect.ORDER));
        registerRod("bone", 75, ModItems.WAND_ROD_BONE, 6, new WandRodPrimalOnUpdate(Aspect.ENTROPY));
        registerRod("reed", 75, ModItems.WAND_ROD_REED, 6, new WandRodPrimalOnUpdate(Aspect.AIR));
        registerRod("silverwood", 100, ModItems.WAND_ROD_SILVERWOOD, 9, null);
        WandRod blaze = WandRod.rods.get("blaze");
        if (blaze != null) {
            blaze.setGlowing(true);
        }

        // Staff rods (their capacity already includes the staff bonus).
        // 1:1 OpenTC4 ConfigItems: STAFF_ROD_GREATWOOD 125/8, OBSIDIAN 175/14,
        // BLAZE 175/14 (glow), ICE 175/14, QUARTZ 175/14, BONE 175/14,
        // REED 175/14, SILVERWOOD 250/24, PRIMAL 250/32.
        registerRod("greatwood_staff", 125, ModItems.WAND_ROD_GREATWOOD_STAFF, 8, null);
        registerRod("obsidian_staff", 175, ModItems.WAND_ROD_OBSIDIAN_STAFF, 14, new WandRodPrimalOnUpdate(Aspect.EARTH));
        registerRod("blaze_staff", 175, ModItems.WAND_ROD_BLAZE_STAFF, 14, new WandRodPrimalOnUpdate(Aspect.FIRE));
        registerRod("ice_staff", 175, ModItems.WAND_ROD_ICE_STAFF, 14, new WandRodPrimalOnUpdate(Aspect.WATER));
        registerRod("quartz_staff", 175, ModItems.WAND_ROD_QUARTZ_STAFF, 14, new WandRodPrimalOnUpdate(Aspect.ORDER));
        registerRod("bone_staff", 175, ModItems.WAND_ROD_BONE_STAFF, 14, new WandRodPrimalOnUpdate(Aspect.ENTROPY));
        registerRod("reed_staff", 175, ModItems.WAND_ROD_REED_STAFF, 14, new WandRodPrimalOnUpdate(Aspect.AIR));
        registerRod("silverwood_staff", 250, ModItems.WAND_ROD_SILVERWOOD_STAFF, 24, null);
        registerRod("primal_staff", 250, ModItems.WAND_ROD_PRIMAL_STAFF, 32, new WandRodPrimalOnUpdate());
        WandRod blazeStaff = WandRod.rods.get("blaze_staff");
        if (blazeStaff != null) {
            blazeStaff.setGlowing(true);
        }

        // ==================== Caps ====================
        registerCap("iron", 1.1f, ModItems.WAND_CAP_IRON, 1);
        registerCap("gold", 1.0f, ModItems.WAND_CAP_GOLD, 3);
        registerCap("thaumium", 0.9f, ModItems.WAND_CAP_THAUMIUM, 6);
        registerCap("void", 0.8f, ModItems.WAND_CAP_VOID, 9);
        // TC4 special caps: the listed aspects use the special modifier
        registerSpecialCap("copper", 1.1f, List.of(Aspect.ORDER, Aspect.ENTROPY), 1.0f,
                ModItems.WAND_CAP_COPPER, 2);
        registerSpecialCap("silver", 1.0f,
                List.of(Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER), 0.95f,
                ModItems.WAND_CAP_SILVER, 4);
    }

    private static void registerRod(String tag, int capacity,
                                    net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, ?> item,
                                    int craftCost, thaumcraft.api.wands.IWandRodOnUpdate onUpdate) {
        if (onUpdate != null) {
            new WandRod(tag, capacity, new ItemStack(item.get()), craftCost, onUpdate);
        } else {
            new WandRod(tag, capacity, new ItemStack(item.get()), craftCost);
        }
    }

    private static void registerCap(String tag, float baseCostModifier,
                                    net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, ?> item,
                                    int craftCost) {
        new WandCap(tag, baseCostModifier, new ItemStack(item.get()), craftCost);
    }

    private static void registerSpecialCap(String tag, float baseCostModifier, List<Aspect> aspects,
                                           float specialModifier,
                                           net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, ?> item,
                                           int craftCost) {
        new WandCap(tag, baseCostModifier, aspects, specialModifier, new ItemStack(item.get()), craftCost);
    }
}
