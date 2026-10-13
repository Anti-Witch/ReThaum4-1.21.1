package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.init.ModBlockEntities;

import java.util.ArrayList;

/**
 * TileRechargePedestal - Recharges vis-powered items from ambient aura.
 * 
 * Place an IRechargable item (gauntlet, caster, etc.) on the pedestal
 * and it will slowly recharge from the local aura.
 */
public class TileRechargePedestal extends TileThaumcraftInventory implements IAspectContainer, WorldlyContainer {
    
    private static final int[] SLOTS = {0};
    private int counter = 0;
    
    public TileRechargePedestal(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }
    
    public TileRechargePedestal(BlockPos pos, BlockState state) {
        this(ModBlockEntities.RECHARGE_PEDESTAL.get(), pos, state);
    }
    
    // ==================== Tick ====================
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileRechargePedestal tile) {
        tile.counter++;
        
        if (tile.counter % 10 == 0) {
            ItemStack stack = tile.getItem(0);
            if (!stack.isEmpty() && stack.getItem() instanceof IRechargable) {
                float recharged = RechargeHelper.rechargeItem(level, stack, pos, null, 5);
                if (recharged > 0) {
                    tile.syncTile(false);
                    tile.setChanged();
                    
                    // Send sparkle effect
                    ArrayList<Aspect> primals = Aspect.getPrimalAspects();
                    int color = primals.get(level.random.nextInt(primals.size())).getColor();
                    level.blockEvent(pos, state.getBlock(), 5, color);
                }
            }
        }
    }
    
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileRechargePedestal tile) {
        // Client-side animation handled by renderer
    }
    
    // ==================== Inventory ====================
    
    /**
     * Set item from infusion crafting (preserves some state).
     */
    public void setInventorySlotContentsFromInfusion(int slot, ItemStack stack) {
        setItem(slot, stack);
        setChanged();
        if (level != null && !level.isClientSide) {
            syncTile(false);
        }
    }
    
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof IRechargable;
    }
    
    // ==================== WorldlyContainer ====================
    
    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }
    
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return stack.getItem() instanceof IRechargable;
    }
    
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }
    
    // ==================== IAspectContainer ====================
    
    @Override
    public AspectList getAspects() {
        ItemStack stack = getItem(0);
        if (!stack.isEmpty() && stack.getItem() instanceof IRechargable) {
            float charge = RechargeHelper.getCharge(stack);
            return new AspectList().add(Aspect.ENERGY, Math.round(charge));
        }
        return null;
    }
    
    @Override
    public void setAspects(AspectList aspects) {
        // Not used - charge is stored in item
    }
    
    @Override
    public int addToContainer(Aspect tag, int amount) {
        return 0;
    }
    
    @Override
    public boolean takeFromContainer(Aspect tag, int amount) {
        return false;
    }
    
    @Override
    public boolean takeFromContainer(AspectList list) {
        return false;
    }
    
    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return false;
    }
    
    @Override
    public boolean doesContainerContain(AspectList list) {
        return false;
    }
    
    @Override
    public int containerContains(Aspect tag) {
        return 0;
    }
    
    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }
    
    // ==================== Block Events ====================
    
    public boolean triggerEvent(int id, int param) {
        if (id == 5) {
            if (level != null && level.isClientSide) {
                // Sparkle effect - draw vis sparkles from nearby to the pedestal
                FXDispatcher.INSTANCE.visSparkle(
                        worldPosition.getX() + level.random.nextInt(3) - level.random.nextInt(3),
                        worldPosition.above().getY() + level.random.nextInt(3),
                        worldPosition.getZ() + level.random.nextInt(3) - level.random.nextInt(3),
                        worldPosition.getX(),
                        worldPosition.above().getY(),
                        worldPosition.getZ(),
                        param);
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }
    
    // ==================== Rendering ====================
    
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.0);
    }
}




