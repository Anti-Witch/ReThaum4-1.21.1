package thaumcraft.common.items.armor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;

/**
 * Thaumium Armor - Balanced magical armor, better than iron.
 */
public class ItemThaumiumArmor extends ArmorItem {
    
    public ItemThaumiumArmor(Type type) {
        super(ThaumcraftMaterials.ARMORMAT_THAUMIUM, type,
                new Item.Properties().stacksTo(1));
    }
    
    // Factory methods for different armor pieces
    public static ItemThaumiumArmor createHelmet() {
        return new ItemThaumiumArmor(Type.HELMET);
    }
    
    public static ItemThaumiumArmor createChestplate() {
        return new ItemThaumiumArmor(Type.CHESTPLATE);
    }
    
    public static ItemThaumiumArmor createLeggings() {
        return new ItemThaumiumArmor(Type.LEGGINGS);
    }
    
    public static ItemThaumiumArmor createBoots() {
        return new ItemThaumiumArmor(Type.BOOTS);
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        // Use layer 1 for helmet/chest/boots, layer 2 for legs
        if (slot == EquipmentSlot.LEGS) {
            return ResourceLocation.parse("thaumcraft:textures/models/thaumium_2.png");
        }
        return ResourceLocation.parse("thaumcraft:textures/models/thaumium_1.png");
    }
}




