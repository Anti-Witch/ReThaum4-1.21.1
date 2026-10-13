package thaumcraft.common.items.consumables;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.ILabelable;

/**
 * Label - Can be applied to essentia containers to filter them.
 * Labels can be blank or filled with an aspect.
 */
public class ItemLabel extends Item {

    private final boolean isFilled;

    public ItemLabel(boolean filled) {
        super(new Item.Properties());
        this.isFilled = filled;
    }

    public static ItemLabel createBlank() {
        return new ItemLabel(false);
    }

    public static ItemLabel createFilled() {
        return new ItemLabel(true);
    }

    /**
     * Gets the aspect stored on this label, if any.
     */
    public Aspect getAspect(ItemStack stack) {
        if (isFilled) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null) {
                CompoundTag tag = data.copyTag();
                if (tag.contains("aspect")) {
                    return Aspect.getAspect(tag.getString("aspect"));
                }
            }
        }
        return null;
    }

    /**
     * Sets the aspect on this label.
     */
    public void setAspect(ItemStack stack, Aspect aspect) {
        if (aspect != null) {
            CompoundTag tag = new CompoundTag();
            tag.putString("aspect", aspect.getTag());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        // Sneaking is reserved for removing filters; let the block handle it
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        BlockState state = level.getBlockState(pos);
        BlockEntity te = level.getBlockEntity(pos);

        // Only blocks/tiles that can actually be labelled consume the label
        boolean labelable = state.getBlock() instanceof ILabelable || te instanceof ILabelable;
        if (!labelable) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Check if the block implements ILabelable
        if (state.getBlock() instanceof ILabelable labelableBlock) {
            if (labelableBlock.applyLabel(player, pos, side, stack)) {
                stack.shrink(1);
                player.inventoryMenu.broadcastChanges();
            }
            return InteractionResult.SUCCESS;
        }

        // Check if the tile entity implements ILabelable
        if (te instanceof ILabelable labelableTile) {
            if (labelableTile.applyLabel(player, pos, side, stack)) {
                stack.shrink(1);
                player.inventoryMenu.broadcastChanges();
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}




