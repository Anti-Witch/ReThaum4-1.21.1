package thaumcraft.common.items.armor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.init.ModItems;

import java.util.List;

/**
 * Thaumaturge's Robe Armor - Cloth armor that provides vis discounts.
 * Can be dyed like leather armor.
 */
public class ItemRobeArmor extends ArmorItem implements IVisDiscountGear {
    
    // Default robe color (0x6A3880, same value as TC4 ItemRobeArmor.getColor)
    private static final int DEFAULT_COLOR = 0x6A3880;
    
    public ItemRobeArmor(Type type) {
        super(ThaumcraftMaterials.ARMORMAT_ROBE, type, 
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.UNCOMMON));
    }
    
    // Factory methods for different armor pieces
    public static ItemRobeArmor createChest() {
        return new ItemRobeArmor(Type.CHESTPLATE);
    }
    
    public static ItemRobeArmor createLegs() {
        return new ItemRobeArmor(Type.LEGGINGS);
    }
    
    public static ItemRobeArmor createBoots() {
        return new ItemRobeArmor(Type.BOOTS);
    }
    
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        // TC4 ItemRobeArmor: boots give 1%, other pieces give 2%
        return this.getType() == Type.BOOTS ? 1 : 2;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int discount = getVisDiscount(stack, null);
        tooltip.add(Component.translatable("item.thaumcraft.vis_discount", discount)
                .withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.ENCHANTED_FABRIC.get()) || super.isValidRepairItem(toRepair, repair);
    }
    
    // ==================== Dyeable Implementation ====================

    public int getColor(ItemStack stack) {
        DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
        return dyedColor != null ? dyedColor.rgb() : DEFAULT_COLOR;
    }

    public boolean hasCustomColor(ItemStack stack) {
        return stack.has(DataComponents.DYED_COLOR);
    }

    public void clearColor(ItemStack stack) {
        stack.remove(DataComponents.DYED_COLOR);
    }

    public void setColor(ItemStack stack, int color) {
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color, true));
    }
    
    /**
     * Allow using cauldron to wash dye off robes (vanilla behavior for leather)
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        
        // Check if clicking on a water cauldron
        if (state.is(Blocks.WATER_CAULDRON)) {
            ItemStack stack = context.getItemInHand();
            if (hasCustomColor(stack)) {
                if (!level.isClientSide()) {
                    clearColor(stack);
                    // Decrease water level
                    LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        
        return super.useOn(context);
    }
}




