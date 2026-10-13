package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

import java.util.List;

/**
 * Shovel of the Earthmover - Earth elemental shovel.
 * Places blocks in a 3x3 pattern from player's inventory.
 * Has built-in Destructive infusion enchantment.
 */
public class ItemElementalShovel extends ShovelItem {

    public ItemElementalShovel() {
        super(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, new Item.Properties()
                .attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, 1.5f, -3.0f))
                .rarity(Rarity.RARE));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        // Only works when sneaking
        if (!player.isShiftKeyDown()) {
            return super.useOn(context);
        }

        BlockState clickedState = level.getBlockState(pos);
        Block clickedBlock = clickedState.getBlock();

        // Don't work on tile entities
        if (level.getBlockEntity(pos) != null) {
            return InteractionResult.FAIL;
        }

        byte orientation = getOrientation(stack);
        int placedCount = 0;

        // Place blocks in a 3x3 grid
        for (int aa = -1; aa <= 1; aa++) {
            for (int bb = -1; bb <= 1; bb++) {
                int xx = 0, yy = 0, zz = 0;

                if (orientation == 1) {
                    // Vertical orientation along Y
                    yy = bb;
                    if (side.ordinal() <= 1) {
                        int facing = Mth.floor(player.getYRot() * 4.0f / 360.0f + 0.5f) & 3;
                        if (facing == 0 || facing == 2) {
                            xx = aa;
                        } else {
                            zz = aa;
                        }
                    } else if (side.ordinal() <= 3) {
                        zz = aa;
                    } else {
                        xx = aa;
                    }
                } else if (orientation == 2) {
                    // Mixed orientation
                    if (side.ordinal() <= 1) {
                        int facing = Mth.floor(player.getYRot() * 4.0f / 360.0f + 0.5f) & 3;
                        yy = bb;
                        if (facing == 0 || facing == 2) {
                            xx = aa;
                        } else {
                            zz = aa;
                        }
                    } else {
                        zz = bb;
                        xx = aa;
                    }
                } else {
                    // Default horizontal orientation
                    if (side.ordinal() <= 1) {
                        xx = aa;
                        zz = bb;
                    } else if (side.ordinal() <= 3) {
                        xx = aa;
                        yy = bb;
                    } else {
                        zz = aa;
                        yy = bb;
                    }
                }

                BlockPos targetPos = pos.relative(side).offset(xx, yy, zz);

                // Check if we can place here
                if (level.getBlockState(targetPos).canBeReplaced()) {
                    // Try to consume the block from player inventory
                    if (player.isCreative() || consumeBlock(player, clickedBlock, clickedState)) {
                        // Play sound
                        SoundType soundType = clickedBlock.getSoundType(clickedState, level, targetPos, player);
                        level.playSound(player, targetPos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                                0.6f, 0.9f + level.random.nextFloat() * 0.2f);

                        // Place the block
                        level.setBlock(targetPos, clickedState, Block.UPDATE_ALL);

                        // Damage the tool
                        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                        placedCount++;

                        // TODO: Add visual effect (bamf)

                        if (stack.isEmpty()) {
                            return InteractionResult.SUCCESS;
                        }
                    } else if (clickedBlock == Blocks.GRASS_BLOCK) {
                        // Special case: grass can be placed as dirt
                        if (player.isCreative() || consumeBlock(player, Blocks.DIRT, Blocks.DIRT.defaultBlockState())) {
                            SoundType soundType = Blocks.DIRT.getSoundType(Blocks.DIRT.defaultBlockState(), level, targetPos, player);
                            level.playSound(player, targetPos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                                    0.6f, 0.9f + level.random.nextFloat() * 0.2f);
                            level.setBlock(targetPos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                            placedCount++;
                        }
                    }
                }
            }
        }

        if (placedCount > 0) {
            player.swing(context.getHand());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    /**
     * Tries to consume a block from the player's inventory.
     */
    private boolean consumeBlock(Player player, Block block, BlockState state) {
        ItemStack blockStack = new ItemStack(block);

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (ItemStack.isSameItem(invStack, blockStack)) {
                invStack.shrink(1);
                if (invStack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the current placement orientation from the item's NBT.
     */
    public static byte getOrientation(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            if (tag.contains("or")) {
                return tag.getByte("or");
            }
        }
        return 0;
    }

    /**
     * Sets the placement orientation in the item's NBT.
     * Called via keybind packet.
     */
    public static void setOrientation(ItemStack stack, byte orientation) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag tag) -> {
            tag.putByte("or", (byte) (orientation % 3));
        });
    }

    /**
     * Cycles to the next orientation.
     */
    public static void cycleOrientation(ItemStack stack) {
        byte current = getOrientation(stack);
        setOrientation(stack, (byte) ((current + 1) % 3));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enchantment.thaumcraft.destructive").withStyle(style -> style.withColor(0x8B4513)));
        tooltip.add(Component.translatable("item.thaumcraft.elemental_shovel.desc").withStyle(style -> style.withColor(0x808080)));

        byte orientation = getOrientation(stack);
        String orientationKey = switch (orientation) {
            case 1 -> "item.thaumcraft.elemental_shovel.orientation.vertical";
            case 2 -> "item.thaumcraft.elemental_shovel.orientation.mixed";
            default -> "item.thaumcraft.elemental_shovel.orientation.horizontal";
        };
        tooltip.add(Component.translatable(orientationKey).withStyle(style -> style.withColor(0x808080)));

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
