package thaumcraft.api;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.Block;
import thaumcraft.Thaumcraft;
import thaumcraft.init.ModItems;


/**
 * Custom materials for Thaumcraft armor and tools.
 * Updated for 1.21.1:
 * - Tier now requires getIncorrectBlocksForDrops() instead of getLevel()
 * - ArmorMaterial is now a record, registered via DeferredRegister
 *   For now we store Holder<ArmorMaterial> references that are populated at registration time.
 */
public class ThaumcraftMaterials {

    // ==================== Block Tags for Tool Tiers ====================
    // These tags define which blocks the tool CANNOT mine (incorrect blocks)
    public static final TagKey<Block> INCORRECT_FOR_THAUMIUM = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "incorrect_for_thaumium_tool"));
    public static final TagKey<Block> INCORRECT_FOR_VOID = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "incorrect_for_void_tool"));
    public static final TagKey<Block> INCORRECT_FOR_ELEMENTAL = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "incorrect_for_elemental_tool"));

    // ==================== Tool Tiers ====================
    
    public static final Tier TOOLMAT_THAUMIUM = new Tier() {
        @Override public int getUses() { return 500; }
        @Override public float getSpeed() { return 7.0F; }
        @Override public float getAttackDamageBonus() { return 2.5F; }
        @Override public int getEnchantmentValue() { return 22; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return INCORRECT_FOR_THAUMIUM; }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.of(ModItems.THAUMIUM_INGOT.get()); 
        }
    };
    
    public static final Tier TOOLMAT_VOID = new Tier() {
        @Override public int getUses() { return 150; }
        @Override public float getSpeed() { return 8.0F; }
        @Override public float getAttackDamageBonus() { return 3.0F; }
        @Override public int getEnchantmentValue() { return 10; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return INCORRECT_FOR_VOID; }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.of(ModItems.VOID_METAL_INGOT.get()); 
        }
    };
    
    public static final Tier TOOLMAT_ELEMENTAL = new Tier() {
        @Override public int getUses() { return 1500; }
        @Override public float getSpeed() { return 9.0F; }
        @Override public float getAttackDamageBonus() { return 3.0F; }
        @Override public int getEnchantmentValue() { return 18; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return INCORRECT_FOR_ELEMENTAL; }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.of(ModItems.THAUMIUM_INGOT.get()); 
        }
    };

    // ==================== Armor Materials ====================
    // In 1.21.1 ArmorMaterial is a record. We store Holders that are
    // populated during mod init via DeferredRegister in ModArmorMaterials.
    // These are placeholder references - the actual registration happens in init code.
    
    public static Holder<ArmorMaterial> ARMORMAT_THAUMIUM;
    public static Holder<ArmorMaterial> ARMORMAT_SPECIAL;
    public static Holder<ArmorMaterial> ARMORMAT_VOID;
    public static Holder<ArmorMaterial> ARMORMAT_VOIDROBE;
    public static Holder<ArmorMaterial> ARMORMAT_ROBE;
    public static Holder<ArmorMaterial> ARMORMAT_FORTRESS;
    public static Holder<ArmorMaterial> ARMORMAT_CULTIST_PLATE;
    public static Holder<ArmorMaterial> ARMORMAT_CULTIST_ROBE;
    public static Holder<ArmorMaterial> ARMORMAT_CULTIST_LEADER;
}
