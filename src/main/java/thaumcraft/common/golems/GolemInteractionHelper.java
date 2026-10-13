package thaumcraft.common.golems;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.common.lib.utils.InventoryUtils;

import java.util.UUID;

/**
 * GolemInteractionHelper - Enables golems to interact with blocks.
 * 
 * Uses a FakePlayer to simulate player interactions like harvesting,
 * placing blocks, and right-clicking machines.
 */
public class GolemInteractionHelper {
    
    private static final GameProfile GOLEM_PROFILE = new GameProfile(
            UUID.fromString("41C82C87-7F55-4A23-8F37-4B2B6F2E3A5E"), 
            "FakeThaumcraftGolem");
    
    /**
     * Simulate a golem clicking on a block.
     * 
     * @param level The world
     * @param golem The golem performing the action
     * @param pos The block position to click
     * @param face The face being clicked
     * @param clickStack The item to click with
     * @param sneaking Whether the golem should be sneaking
     * @param rightClick True for right-click, false for left-click (break)
     */
    public static void golemClick(Level level, IGolemAPI golem, BlockPos pos, Direction face, 
            ItemStack clickStack, boolean sneaking, boolean rightClick) {
        
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        FakePlayer fp = FakePlayerFactory.get(serverLevel, GOLEM_PROFILE);
        
        // Position the fake player at the golem's location
        if (golem.getGolemEntity() != null) {
            fp.setPos(golem.getGolemEntity().getX(), 
                      golem.getGolemEntity().getY(), 
                      golem.getGolemEntity().getZ());
            fp.setYRot(golem.getGolemEntity().getYRot());
            fp.setXRot(golem.getGolemEntity().getXRot());
        }
        
        // Set up the fake player's held item
        fp.setItemInHand(InteractionHand.MAIN_HAND, clickStack.copy());
        fp.setShiftKeyDown(sneaking);
        
        BlockState blockState = level.getBlockState(pos);
        
        if (!rightClick) {
            // Left click - break block
            try {
                fp.gameMode.destroyBlock(pos);
            } catch (Exception ignored) {
                // Silently ignore exceptions from block breaking
            }
        } else {
            // Right click - interact or place
            
            // If holding a block item, check if we can place it
            if (clickStack.getItem() instanceof BlockItem blockItem) {
                BlockPos placePos = pos.relative(face);
                if (!mayPlace(level, blockItem, placePos, face)) {
                    // Move golem closer if can't place
                    if (golem.getGolemEntity() != null) {
                        golem.getGolemEntity().setPos(
                                golem.getGolemEntity().getX() + face.getStepX(),
                                golem.getGolemEntity().getY() + face.getStepY(),
                                golem.getGolemEntity().getZ() + face.getStepZ());
                    }
                }
            }
            
            try {
                // Create a block hit result
                Vec3 hitVec = new Vec3(
                        pos.getX() + 0.5 + face.getStepX() * 0.5,
                        pos.getY() + 0.5 + face.getStepY() * 0.5,
                        pos.getZ() + 0.5 + face.getStepZ() * 0.5);
                BlockHitResult hitResult = new BlockHitResult(hitVec, face, pos, false);
                
                fp.gameMode.useItemOn(fp, level, fp.getItemInHand(InteractionHand.MAIN_HAND), 
                        InteractionHand.MAIN_HAND, hitResult);
            } catch (Exception ignored) {
                // Silently ignore exceptions from block interaction
            }
        }
        
        // Grant XP to golem
        golem.addRankXp(1);
        
        // Handle consumed items
        if (!fp.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() && 
                fp.getItemInHand(InteractionHand.MAIN_HAND).getCount() <= 0) {
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        
        // Transfer any items the fake player received to the golem
        dropSomeItems(fp, golem);
        
        // Visual feedback
        golem.swingArm();
    }
    
    /**
     * Check if a block can be placed at a position.
     */
    private static boolean mayPlace(Level level, BlockItem blockItem, BlockPos pos, Direction face) {
        BlockState existingState = level.getBlockState(pos);
        
        // Can't place if there's already a solid block
        if (existingState.canOcclude()) {
            return false;
        }
        
        // Check for entity collision
        return level.getEntities(null, 
                blockItem.getBlock().defaultBlockState().getShape(level, pos).bounds().move(pos)).isEmpty();
    }
    
    /**
     * Transfer items from the fake player's inventory to the golem.
     */
    private static void dropSomeItems(FakePlayer fp, IGolemAPI golem) {
        // Transfer main inventory items
        for (int i = 0; i < fp.getInventory().items.size(); i++) {
            ItemStack stack = fp.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                // Try to give to golem
                if (golem.canCarry(stack, true)) {
                    ItemStack remaining = golem.holdItem(stack);
                    fp.getInventory().items.set(i, remaining);
                }
                
                // Drop anything left over
                if (!fp.getInventory().items.get(i).isEmpty()) {
                    InventoryUtils.dropItemAtEntity(golem.getGolemWorld(), 
                            fp.getInventory().items.get(i), golem.getGolemEntity());
                    fp.getInventory().items.set(i, ItemStack.EMPTY);
                }
            }
        }
        
        // Transfer armor inventory items (shouldn't normally happen but just in case)
        for (int i = 0; i < fp.getInventory().armor.size(); i++) {
            ItemStack stack = fp.getInventory().armor.get(i);
            if (!stack.isEmpty()) {
                if (golem.canCarry(stack, true)) {
                    ItemStack remaining = golem.holdItem(stack);
                    fp.getInventory().armor.set(i, remaining);
                }
                
                if (!fp.getInventory().armor.get(i).isEmpty()) {
                    InventoryUtils.dropItemAtEntity(golem.getGolemWorld(), 
                            fp.getInventory().armor.get(i), golem.getGolemEntity());
                    fp.getInventory().armor.set(i, ItemStack.EMPTY);
                }
            }
        }
    }
    
    /**
     * Make a golem harvest a crop block.
     */
    public static boolean harvestCrop(Level level, IGolemAPI golem, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        
        // Use left-click to break the crop
        golemClick(level, golem, pos, Direction.UP, ItemStack.EMPTY, false, false);
        return true;
    }
    
    /**
     * Make a golem use an item on a block (like bonemeal).
     */
    public static boolean useItemOnBlock(Level level, IGolemAPI golem, BlockPos pos, 
            ItemStack item, Direction face) {
        if (item.isEmpty()) {
            return false;
        }
        
        golemClick(level, golem, pos, face, item, false, true);
        return true;
    }
}




