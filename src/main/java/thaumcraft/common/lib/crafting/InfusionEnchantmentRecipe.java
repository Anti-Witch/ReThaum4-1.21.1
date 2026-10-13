package thaumcraft.common.lib.crafting;
import thaumcraft.common.lib.utils.NBTHelper;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;
import java.util.Random;


import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.items.IRechargable;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.init.ModRecipeSerializers;
import thaumcraft.common.lib.compat.CuriosCompat;

public class InfusionEnchantmentRecipe extends InfusionRecipeType {
    
    public final EnumInfusionEnchantment enchantment;
    
    public InfusionEnchantmentRecipe(ResourceLocation id, EnumInfusionEnchantment ench, AspectList as, NonNullList<Ingredient> components) {
        super(id, "", Ingredient.EMPTY, components, as, ItemStack.EMPTY, ench.research, 4);
        this.enchantment = ench;
    }
    
    @Override
    public boolean matchesInfusion(List<ItemStack> input, ItemStack central, Level level, Player player) {
        if (central == null || central.isEmpty() || !ThaumcraftCapabilities.isResearchKnown(player, getResearch())) {
            return false;
        }
        
        if (EnumInfusionEnchantment.getInfusionEnchantmentLevel(central, enchantment) >= enchantment.maxLevel) {
            return false;
        }
        
        boolean cool = false;
        if (!enchantment.toolClasses.contains("all")) {
            ItemAttributeModifiers itemMods = central.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            boolean hasAttackDamage = false;
            for (var entry : itemMods.modifiers()) {
                if (entry.attribute() == Attributes.ATTACK_DAMAGE) { hasAttackDamage = true; break; }
            }
            if (hasAttackDamage && enchantment.toolClasses.contains("weapon")) {
                cool = true;
            }

            if (!cool && central.getItem() instanceof TieredItem) {
                // Simplified check matching the previous implementation
                if (enchantment.toolClasses.contains("axe") && central.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.AXE_DIG)) cool = true;
                if (enchantment.toolClasses.contains("pickaxe") && central.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.PICKAXE_DIG)) cool = true;
                if (enchantment.toolClasses.contains("shovel") && central.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHOVEL_DIG)) cool = true;
                if (enchantment.toolClasses.contains("sword") && central.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SWORD_DIG)) cool = true;
                if (enchantment.toolClasses.contains("hoe") && central.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.HOE_DIG)) cool = true;
            }
            
            if (!cool && central.getItem() instanceof ArmorItem armorItem) {
                String at = "none";
                switch (armorItem.getType()) {
                    case HELMET -> at = "helm";
                    case CHESTPLATE -> at = "chest";
                    case LEGGINGS -> at = "legs";
                    case BOOTS -> at = "boots";
                }
                if (enchantment.toolClasses.contains("armor") || enchantment.toolClasses.contains(at)) {
                    cool = true;
                }
            }
            
            // Check if item is a Curios-compatible bauble
            if (!cool && enchantment.toolClasses.contains("bauble")) {
                // Items with ICurioItem interface or in curios slot identifiers are baubles
                // For simplicity, we check if it can go in any curios slot
                if (CuriosCompat.isCuriosLoaded()) {
                    // Check if this is a curio-capable item
                    try {
                        Class<?> curioInterface = Class.forName("top.theillusivec4.curios.api.type.capability.ICurioItem");
                        if (curioInterface.isAssignableFrom(central.getItem().getClass())) {
                            cool = true;
                        }
                    } catch (ClassNotFoundException ignored) {
                        // Curios not available
                    }
                }
            }
            
            if (!cool && central.getItem() instanceof IRechargable && enchantment.toolClasses.contains("chargable")) {
                cool = true;
            }
            
            if (!cool) {
                return false;
            }
        }
        
        return RecipeMatcher.findMatches(input, getComponents()) != null;
    }
    
    @Override
    public ItemStack getRecipeOutput(Player player, ItemStack input, List<ItemStack> comps) {
        if (input == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = input.copy();
        int cl = EnumInfusionEnchantment.getInfusionEnchantmentLevel(out, enchantment);
        if (cl >= enchantment.maxLevel) {
            return ItemStack.EMPTY;
        }
        
        List<EnumInfusionEnchantment> el = EnumInfusionEnchantment.getInfusionEnchantments(input);
        Random rand = new Random(System.nanoTime());
        if (rand.nextInt(10) < el.size()) {
            int base = 1;
            if (NBTHelper.hasTag(input)) {
                base += NBTHelper.getTag(input).getByte("TC.WARP");
            }
            NBTHelper.getOrCreateTag(out).putByte("TC.WARP", (byte) base);
        }
        
        EnumInfusionEnchantment.addInfusionEnchantment(out, enchantment, cl + 1);
        return out;
    }
    
    @Override
    public AspectList getAspects(Player player, ItemStack input, List<ItemStack> comps) {
        AspectList out = new AspectList();
        if (input == null || input.isEmpty()) {
            return out;
        }
        
        int cl = EnumInfusionEnchantment.getInfusionEnchantmentLevel(input, enchantment) + 1;
        if (cl > enchantment.maxLevel) {
            return out;
        }
        
        List<EnumInfusionEnchantment> el = EnumInfusionEnchantment.getInfusionEnchantments(input);
        int otherEnchantments = el.size();
        if (el.contains(enchantment)) {
            --otherEnchantments;
        }
        
        float modifier = cl + otherEnchantments * 0.33f;
        for (Aspect a : super.getAspects().getAspects()) {
            out.add(a, (int)(super.getAspects().getAmount(a) * modifier));
        }
        
        return out;
    }
    
    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.INFUSION_ENCHANTMENT.get();
    }
}




