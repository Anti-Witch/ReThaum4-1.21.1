package thaumcraft.common.items.armor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftMaterials;

import javax.annotation.Nullable;

/**
 * Cultist Plate Armor - Crimson Cult heavy armor.
 */
public class ItemCultistPlateArmor extends ArmorItem {
    
    public ItemCultistPlateArmor(Type type) {
        super(ThaumcraftMaterials.ARMORMAT_CULTIST_PLATE, type,
                new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    }
    
    // Factory methods for different armor pieces
    public static ItemCultistPlateArmor createHelmet() {
        return new ItemCultistPlateArmor(Type.HELMET);
    }
    
    public static ItemCultistPlateArmor createChestplate() {
        return new ItemCultistPlateArmor(Type.CHESTPLATE);
    }
    
    public static ItemCultistPlateArmor createLeggings() {
        return new ItemCultistPlateArmor(Type.LEGGINGS);
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.IRON_INGOT) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        // TODO: Check if entity is InhabitedZombie
        return ResourceLocation.parse("thaumcraft:textures/models/cultist_plate_armor.png");
    }
}
