package thaumcraft.common.items.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IWarpingGear;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Crimson Praetor Armor - Elite armor worn by the Crimson Praetor (Cultist Leader).
 * Has special custom model rendering with extended shoulder pauldrons and cape details.
 * Provides excellent protection with toughness bonus.
 * Also inflicts warp on the wearer due to its eldritch nature.
 */
public class ItemCultistLeaderArmor extends ArmorItem implements IWarpingGear {
    
    public ItemCultistLeaderArmor(Type type) {
        super(ThaumcraftMaterials.ARMORMAT_CULTIST_LEADER, type, 
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.RARE)
                        .fireResistant());
    }
    
    // Factory methods for each armor piece
    public static ItemCultistLeaderArmor createHelmet() {
        return new ItemCultistLeaderArmor(Type.HELMET);
    }
    
    public static ItemCultistLeaderArmor createChestplate() {
        return new ItemCultistLeaderArmor(Type.CHESTPLATE);
    }
    
    public static ItemCultistLeaderArmor createLeggings() {
        return new ItemCultistLeaderArmor(Type.LEGGINGS);
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.IRON_INGOT) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.parse("thaumcraft:textures/models/cultist_leader_armor.png");
    }
    
    /**
     * Praetor armor inflicts warp on the wearer.
     * Full set gives 2 warp total.
     */
    @Override
    public int getWarp(ItemStack stack, Player player) {
        return switch (getType()) {
            case HELMET -> 1;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 0;
            default -> 0;
        };
    }
    
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            @Nonnull
            @OnlyIn(Dist.CLIENT)
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, 
                    EquipmentSlot slot, HumanoidModel<?> original) {
                // TODO: Return custom ModelLeaderArmor when implemented
                // For now, use default armor model
                return original;
            }
        });
    }
}
