package thaumcraft.common.items.armor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.init.ModItems;

import java.util.List;

/**
 * Void Robe Armor - The ultimate mage armor combining void metal protection
 * with robe aesthetics. Self-repairs, provides vis discount, and goggles functionality
 * on the helmet. Causes warp when worn.
 */
public class ItemVoidRobeArmor extends ArmorItem 
        implements IVisDiscountGear, IGoggles, IRevealer, IWarpingGear {
    
    // Default color (0x6A3880, same value as TC4 ItemVoidRobeArmor.getColor)
    private static final int DEFAULT_COLOR = 0x6A3880;
    
    public ItemVoidRobeArmor(Type type) {
        super(ThaumcraftMaterials.ARMORMAT_VOIDROBE, type, 
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.EPIC));
    }
    
    // Factory methods
    public static ItemVoidRobeArmor createHelmet() {
        return new ItemVoidRobeArmor(Type.HELMET);
    }
    
    public static ItemVoidRobeArmor createChestplate() {
        return new ItemVoidRobeArmor(Type.CHESTPLATE);
    }
    
    public static ItemVoidRobeArmor createLeggings() {
        return new ItemVoidRobeArmor(Type.LEGGINGS);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int discount = getVisDiscount(stack, null);
        tooltip.add(Component.translatable("item.thaumcraft.vis_discount", discount)
                .withStyle(ChatFormatting.DARK_PURPLE));
        if (getType() == Type.HELMET) {
            tooltip.add(Component.translatable("item.thaumcraft.goggles.desc")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.VOID_METAL_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        // Self-repair while worn
        if (!level.isClientSide() && stack.isDamaged() && entity.tickCount % 20 == 0 
                && entity instanceof LivingEntity living) {
            // Check if actually worn
            for (ItemStack armorPiece : living.getArmorSlots()) {
                if (armorPiece == stack) {
                    stack.setDamageValue(stack.getDamageValue() - 1);
                    break;
                }
            }
        }
    }
    
    // ==================== IVisDiscountGear ====================
    
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return 5; // 5% discount per piece
    }
    
    // ==================== IGoggles/IRevealer ====================
    
    @Override
    public boolean showNodes(ItemStack itemstack, LivingEntity player) {
        return this.getType() == Type.HELMET;
    }
    
    @Override
    public boolean showIngamePopups(ItemStack itemstack, LivingEntity player) {
        return this.getType() == Type.HELMET;
    }
    
    // ==================== IWarpingGear ====================
    
    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 2; // TC4 ItemVoidRobeArmor.getWarp
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
     * Allow using cauldron to wash dye off robes.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        
        if (state.is(Blocks.WATER_CAULDRON)) {
            ItemStack stack = context.getItemInHand();
            if (hasCustomColor(stack)) {
                if (!level.isClientSide()) {
                    clearColor(stack);
                    LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        
        return super.useOn(context);
    }
}




