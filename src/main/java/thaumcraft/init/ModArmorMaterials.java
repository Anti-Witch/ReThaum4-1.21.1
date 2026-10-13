package thaumcraft.init;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registers the custom Thaumcraft ArmorMaterials.
 *
 * In NeoForge 1.21.1 ArmorMaterial is a record registered in the
 * Registries.ARMOR_MATERIAL registry. The static fields in
 * ThaumcraftMaterials (ARMORMAT_*) must be non-null Holder references or any
 * ArmorItem constructed from them will crash with an NPE in
 * ArmorItem.getDefaultAttributeModifiers (Holder.value() on a null holder).
 *
 * Layer textures: an ArmorMaterial.Layer("thaumcraft:xxx") resolves to
 * assets/thaumcraft/textures/models/armor/xxx_layer_1.png (outer model) and
 * xxx_layer_2.png (inner/legs model). Those files are copies of the original
 * TC4 textures from textures/models/ (thaumium_1/2.png, robes_1/2.png, ...).
 * Layers with dyeable=true get the dye colour applied when worn; the robe
 * materials use a second, non-dyeable layer for the decorative overlay pass,
 * mirroring the two render passes of TC4 (pass 0 = dyed base, pass 1 = trim).
 */
public final class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Thaumcraft.MODID);

    private ModArmorMaterials() {
    }

    private static ArmorMaterial material(
            Holder<SoundEvent> equipSound,
            int enchantmentValue,
            Supplier<Ingredient> repair,
            int helmet, int chest, int legs, int boots,
            List<ArmorMaterial.Layer> layers,
            float toughness, float knockback) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmorItem.Type.CHESTPLATE, chest);
        defense.put(ArmorItem.Type.LEGGINGS, legs);
        defense.put(ArmorItem.Type.BOOTS, boots);

        return new ArmorMaterial(
                defense,
                enchantmentValue,
                equipSound,
                repair,
                layers,
                toughness,
                knockback);
    }

    /**
     * Builds an armor layer pointing at textures/models/armor/&lt;path&gt;_layer_1.png
     * and _layer_2.png. Dyeable layers are tinted with the item dye colour.
     */
    private static ArmorMaterial.Layer layer(String path, boolean dyeable) {
        return new ArmorMaterial.Layer(
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path), "", dyeable);
    }

    private static Holder<ArmorMaterial> reg(String name, ArmorMaterial material) {
        return ARMOR_MATERIALS.register(name, () -> material);
    }

    static {
        ThaumcraftMaterials.ARMORMAT_THAUMIUM = reg("thaumium",
                material(SoundEvents.ARMOR_EQUIP_IRON, 10,
                        () -> Ingredient.of(ModItems.THAUMIUM_INGOT.get()), 2, 6, 5, 2,
                        List.of(layer("thaumium", false)), 1.0F, 0.0F));

        ThaumcraftMaterials.ARMORMAT_VOID = reg("void",
                material(SoundEvents.ARMOR_EQUIP_NETHERITE, 15,
                        () -> Ingredient.of(ModItems.VOID_METAL_INGOT.get()), 3, 7, 6, 3,
                        List.of(layer("void", false)), 2.0F, 0.1F));

        // Void robe: TC4 renders pass 0 with void_robe_armor_overlay.png tinted by
        // the robe colour and pass 1 with void_robe_armor.png untinted.
        ThaumcraftMaterials.ARMORMAT_VOIDROBE = reg("voidrobe",
                material(SoundEvents.ARMOR_EQUIP_LEATHER, 12,
                        () -> Ingredient.of(ModItems.VOID_METAL_INGOT.get()), 2, 6, 5, 2,
                        List.of(layer("voidrobe", true), layer("voidrobe_trim", false)), 1.0F, 0.0F));

        // Cloth robe (TC4 ItemRobeArmor): dyed cloth base + decorative overlay.
        // Same stats as ARMORMAT_SPECIAL which was previously (mis)used for robes.
        ThaumcraftMaterials.ARMORMAT_ROBE = reg("robe",
                material(SoundEvents.ARMOR_EQUIP_IRON, 10,
                        () -> Ingredient.of(ModItems.THAUMIUM_INGOT.get()), 2, 6, 5, 2,
                        List.of(layer("robes", true), layer("robes_overlay", false)), 1.0F, 0.0F));

        ThaumcraftMaterials.ARMORMAT_FORTRESS = reg("fortress",
                material(SoundEvents.ARMOR_EQUIP_IRON, 15,
                        () -> Ingredient.of(ModItems.THAUMIUM_INGOT.get()), 3, 8, 6, 3,
                        List.of(layer("fortress", false)), 2.0F, 0.15F));

        // TC6-removed: ARMORMAT_CULTIST_PLATE uses BRASS_INGOT (removed)
        // Use thaumium instead as repair material
        ThaumcraftMaterials.ARMORMAT_CULTIST_PLATE = reg("cultist_plate",
                material(SoundEvents.ARMOR_EQUIP_IRON, 12,
                        () -> Ingredient.of(ModItems.THAUMIUM_INGOT.get()), 2, 6, 5, 2,
                        List.of(layer("cultist_plate", false)), 1.0F, 0.0F));

        ThaumcraftMaterials.ARMORMAT_CULTIST_ROBE = reg("cultist_robe",
                material(SoundEvents.ARMOR_EQUIP_LEATHER, 15,
                        () -> Ingredient.of(ModItems.ENCHANTED_FABRIC.get()), 1, 3, 2, 1,
                        List.of(layer("cultist_robe", false)), 0.0F, 0.0F));

        // TC6-removed: ARMORMAT_CULTIST_LEADER uses BRASS_INGOT (removed)
        // Use thaumium instead as repair material
        ThaumcraftMaterials.ARMORMAT_CULTIST_LEADER = reg("cultist_leader",
                material(SoundEvents.ARMOR_EQUIP_NETHERITE, 18,
                        () -> Ingredient.of(ModItems.THAUMIUM_INGOT.get()), 3, 8, 6, 3,
                        List.of(layer("cultist_leader", false)), 2.0F, 0.1F));

        ThaumcraftMaterials.ARMORMAT_SPECIAL = reg("special",
                material(SoundEvents.ARMOR_EQUIP_IRON, 10,
                        () -> Ingredient.of(ModItems.THAUMIUM_INGOT.get()), 2, 6, 5, 2,
                        List.of(layer("special", false)), 1.0F, 0.0F));
    }
}
