package thaumcraft.common.items.consumables;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.items.ItemEssentiaContainer;
import thaumcraft.init.ModItems;

import java.util.List;

/**
 * Phial item - holds 10 essentia of a single aspect.
 * Can be filled from and emptied into jars and alembics.
 */
public class ItemPhial extends ItemEssentiaContainer {

    public static final int PHIAL_CAPACITY = 10;

    private final boolean filled;

    public ItemPhial(boolean filled) {
        super(new Properties().stacksTo(64), PHIAL_CAPACITY);
        this.filled = filled;
    }

    /**
     * Create an empty phial.
     */
    public static ItemPhial createEmpty() {
        return new ItemPhial(false);
    }

    /**
     * Create a filled phial template (actual aspect is stored in NBT).
     */
    public static ItemPhial createFilled() {
        return new ItemPhial(true);
    }

    /**
     * Create a filled phial stack with the specified aspect.
     */
    public static ItemStack makeFilledPhial(Aspect aspect) {
        return makePhial(aspect, PHIAL_CAPACITY);
    }

    /**
     * Create a phial stack with specified aspect and amount.
     */
    public static ItemStack makePhial(Aspect aspect, int amount) {
        ItemStack stack = new ItemStack(ModItems.PHIAL_FILLED.get());
        ItemPhial phial = (ItemPhial) stack.getItem();
        phial.setAspects(stack, new AspectList().add(aspect, amount));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (filled) {
            AspectList aspects = getAspects(stack);
            if (aspects != null && aspects.size() > 0) {
                Aspect aspect = aspects.getAspects()[0];
                return Component.translatable("item.thaumcraft.phial.filled", aspect.getName());
            }
        }
        return Component.translatable("item.thaumcraft.phial.empty");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (filled) {
            AspectList aspects = getAspects(stack);
            if (aspects != null && aspects.size() > 0) {
                Aspect aspect = aspects.getAspects()[0];
                int amount = aspects.getAmount(aspect);
                tooltip.add(Component.literal(aspect.getName() + ": " + amount)
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();

        if (player == null) return InteractionResult.PASS;
        ItemStack heldStack = player.getItemInHand(hand);

        // Empty phial - try to fill from jar or alembic
        if (!filled) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof IAspectSource source) {
                AspectList available = source.getAspects();
                if (available != null && available.size() > 0) {
                    Aspect aspect = available.getAspects()[0];
                    int amount = available.getAmount(aspect);
                    
                    if (amount >= PHIAL_CAPACITY) {
                        if (level.isClientSide()) {
                            return InteractionResult.SUCCESS;
                        }
                        
                        if (source.takeFromContainer(aspect, PHIAL_CAPACITY)) {
                            heldStack.shrink(1);
                            ItemStack filledPhial = makeFilledPhial(aspect);
                            
                            if (!player.getInventory().add(filledPhial)) {
                                level.addFreshEntity(new ItemEntity(level, 
                                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, filledPhial));
                            }
                            
                            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.25f, 1.0f);
                            return InteractionResult.CONSUME;
                        }
                    }
                }
            }
        }
        // Filled phial - try to empty into jar
        else {
            AspectList myAspects = getAspects(heldStack);
            if (myAspects != null && myAspects.size() > 0) {
                Aspect aspect = myAspects.getAspects()[0];
                int amount = myAspects.getAmount(aspect);
                
                BlockEntity te = level.getBlockEntity(pos);
                if (te instanceof IAspectSource source) {
                    if (source.doesContainerAccept(aspect)) {
                        if (level.isClientSide()) {
                            return InteractionResult.SUCCESS;
                        }
                        
                        int added = source.addToContainer(aspect, amount);
                        if (added == 0) { // All added successfully
                            heldStack.shrink(1);
                            ItemStack emptyPhial = new ItemStack(ModItems.PHIAL_EMPTY.get());
                            
                            if (!player.getInventory().add(emptyPhial)) {
                                level.addFreshEntity(new ItemEntity(level,
                                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, emptyPhial));
                            }
                            
                            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.25f, 1.0f);
                            return InteractionResult.CONSUME;
                        }
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    public boolean isFilled() {
        return filled;
    }
}




