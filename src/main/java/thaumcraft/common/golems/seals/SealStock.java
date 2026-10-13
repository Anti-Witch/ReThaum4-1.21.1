package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;

import java.util.Random;

/**
 * SealStock - Requests items to keep an inventory stocked.
 * 
 * This seal monitors the attached inventory and creates provision requests
 * when the filtered items drop below the configured amounts.
 * Works in conjunction with SealProvide to deliver items.
 * 
 * Features:
 * - 9-slot filter with stack size limiters
 * - Meta/NBT/Ore/Mod matching options
 * - Whitelist mode (stock these specific items)
 * 
 * Ported from 1.12.2.
 */
@SuppressWarnings("deprecation")
public class SealStock extends SealFiltered implements ISealConfigToggles {
    
    private int delay;
    private ResourceLocation icon;
    protected SealToggle[] props;
    
    public SealStock() {
        delay = new Random(System.nanoTime()).nextInt(50);
        icon = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_stock");
        props = new SealToggle[] {
            new SealToggle(true, "pmeta", "golem.prop.meta"),
            new SealToggle(true, "pnbt", "golem.prop.nbt"),
            new SealToggle(false, "pore", "golem.prop.ore"),
            new SealToggle(false, "pmod", "golem.prop.mod")
        };
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:stock";
    }
    
    @Override
    public int getFilterSize() {
        return 9;
    }
    
    @Override
    public void tickSeal(Level level, ISealEntity seal) {
        if (delay++ % 20 != 0) {
            return;
        }
        
        IItemHandler inv = getItemHandler(level, seal.getSealPos().pos, seal.getSealPos().face);
        if (inv == null) return;
        
        // Check each filter slot
        for (int a = 0; a < 9; a++) {
            ItemStack filterStack = getFilterSlot(a);
            if (filterStack.isEmpty()) continue;
            
            int targetAmount = getFilterSlotSize(a);
            if (targetAmount <= 0) continue;
            
            // Count current items in inventory matching this filter
            int currentAmount = countItemsMatching(inv, filterStack);
            
            if (currentAmount < targetAmount) {
                // Request the difference
                ItemStack requested = filterStack.copy();
                int needed = Math.min(requested.getMaxStackSize(), targetAmount - currentAmount);
                requested.setCount(needed);
                
                // Check if there's room for this item
                ItemStack testInsert = requested.copy();
                ItemStack leftover = ItemHandlerHelper.insertItem(inv, testInsert, true);
                
                if (leftover.getCount() < requested.getCount()) {
                    // There's room for at least some items
                    requested.setCount(requested.getCount() - leftover.getCount());
                    GolemHelper.requestProvisioning(level, seal.getSealPos().pos, seal.getSealPos().face, requested);
                }
            }
        }
    }
    
    private IItemHandler getItemHandler(Level level, BlockPos pos, Direction face) {
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            var cap = blockEntity.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, blockEntity.getBlockPos(), face);
            if (cap != null) {
                return cap;
            }
        }
        return null;
    }
    
    /**
     * Count items in inventory matching the filter stack.
     */
    private int countItemsMatching(IItemHandler inv, ItemStack filterStack) {
        int count = 0;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack slotStack = inv.getStackInSlot(i);
            if (matchesItem(slotStack, filterStack)) {
                count += slotStack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Check if two item stacks match based on toggle settings.
     */
    private boolean matchesItem(ItemStack stack, ItemStack filter) {
        if (stack.isEmpty() || filter.isEmpty()) return false;
        
        // Basic item match
        if (!ItemStack.isSameItem(stack, filter)) {
            // Check ore/tag matching
            if (props[2].getValue()) {
                // TODO: Implement tag-based matching
                return false;
            }
            // Check mod matching
            if (props[3].getValue()) {
                String stackMod = stack.getItem().builtInRegistryHolder().key().location().getNamespace();
                String filterMod = filter.getItem().builtInRegistryHolder().key().location().getNamespace();
                return stackMod.equals(filterMod);
            }
            return false;
        }
        
        // Meta match (damage value in modern MC)
        if (props[0].getValue()) {
            if (stack.getDamageValue() != filter.getDamageValue()) {
                return false;
            }
        }
        
        // NBT match
        if (props[1].getValue()) {
            return ItemStack.isSameItemSameComponents(stack, filter);
        }
        
        return true;
    }
    
    @Override
    public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
        // Stock seal doesn't create tasks directly
    }
    
    @Override
    public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        // Stock seal doesn't handle task completion
        return true;
    }
    
    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        // Stock seal doesn't dispatch golems directly
        return false;
    }
    
    @Override
    public boolean canPlaceAt(Level level, BlockPos pos, Direction side) {
        return getItemHandler(level, pos, side) != null;
    }
    
    @Override
    public ResourceLocation getSealIcon() {
        return icon;
    }
    
    @Override
    public int[] getGuiCategories() {
        return new int[] { 1, 3, 0, 4 };
    }
    
    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return null;
    }
    
    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.CLUMSY };
    }
    
    @Override
    public void onTaskSuspension(Level level, Task task) {
    }
    
    @Override
    public void onRemoval(Level level, BlockPos pos, Direction side) {
    }
    
    @Override
    public boolean hasStacksizeLimiters() {
        return true;
    }
    
    @Override
    public boolean isBlacklist() {
        return false; // Stock seal is always whitelist mode
    }
    
    @Override
    public Object returnContainer(Level level, Player player, BlockPos pos, Direction side, ISealEntity seal) {
        return null;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public Object returnGui(Level level, Player player, BlockPos pos, Direction side, ISealEntity seal) {
        return null;
    }
    
    @Override
    public SealToggle[] getToggles() {
        return props;
    }
    
    @Override
    public void setToggle(int index, boolean value) {
        if (index >= 0 && index < props.length) {
            props[index].setValue(value);
        }
    }
}




