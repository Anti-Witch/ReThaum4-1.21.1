package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealEntity;

/**
 * SealFiltered - Abstract base class for seals that support item filtering.
 * 
 * Provides:
 * - Item filter slots
 * - Blacklist/whitelist mode
 * - Optional stack size limiters
 * - NBT serialization
 * 
 * Ported from 1.12.2. Key changes:
 * - NBTTagCompound -> CompoundTag
 * - ItemStackHelper -> ContainerHelper
 * - EnumFacing -> Direction
 */
public abstract class SealFiltered implements ISeal, ISealConfigFilter {
    
    protected NonNullList<ItemStack> filter;
    protected NonNullList<Integer> filterSize;
    protected boolean blacklist;
    
    public SealFiltered() {
        filter = NonNullList.withSize(getFilterSize(), ItemStack.EMPTY);
        filterSize = NonNullList.withSize(getFilterSize(), 0);
        blacklist = true;
    }
    
    @Override
    public void readCustomNBT(CompoundTag nbt) {
        // Load filter items
        filter = NonNullList.withSize(getFilterSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, filter, net.minecraft.core.RegistryAccess.EMPTY);
        
        // Ensure filter counts are 1
        for (ItemStack s : filter) {
            if (s.getCount() > 1) {
                s.setCount(1);
            }
        }
        
        blacklist = nbt.getBoolean("bl");
        
        // Load filter sizes
        filterSize = NonNullList.withSize(getFilterSize(), 0);
        ListTag sizeList = nbt.getList("Sizes", 10); // 10 = CompoundTag
        for (int i = 0; i < sizeList.size(); i++) {
            CompoundTag sizeTag = sizeList.getCompound(i);
            int slot = sizeTag.getByte("Slot") & 0xFF;
            if (slot >= 0 && slot < filterSize.size()) {
                filterSize.set(slot, sizeTag.getInt("Size"));
            }
        }
    }
    
    @Override
    public void writeCustomNBT(CompoundTag nbt) {
        ContainerHelper.saveAllItems(nbt, filter, net.minecraft.core.RegistryAccess.EMPTY);
        nbt.putBoolean("bl", blacklist);
        
        // Save filter sizes
        ListTag sizeList = new ListTag();
        for (int i = 0; i < filterSize.size(); i++) {
            int size = filterSize.get(i);
            if (size != 0) {
                CompoundTag sizeTag = new CompoundTag();
                sizeTag.putByte("Slot", (byte) i);
                sizeTag.putInt("Size", size);
                sizeList.add(sizeTag);
            }
        }
        nbt.put("Sizes", sizeList);
    }
    
    // GUI methods - stubbed for now, will be implemented with GUI system
    @Override
    public Object returnContainer(Level level, Player player, BlockPos pos, Direction side, ISealEntity seal) {
        // TODO: Return SealBaseContainer when GUI system is implemented
        return null;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public Object returnGui(Level level, Player player, BlockPos pos, Direction side, ISealEntity seal) {
        // TODO: Return SealBaseGUI when GUI system is implemented
        return null;
    }
    
    public int[] getGuiCategories() {
        return new int[] { 0 };
    }
    
    @Override
    public int getFilterSize() {
        return 1;
    }
    
    @Override
    public NonNullList<ItemStack> getInv() {
        return filter;
    }
    
    @Override
    public NonNullList<Integer> getSizes() {
        return filterSize;
    }
    
    @Override
    public ItemStack getFilterSlot(int i) {
        return filter.get(i);
    }
    
    @Override
    public int getFilterSlotSize(int i) {
        return filterSize.get(i);
    }
    
    @Override
    public void setFilterSlot(int i, ItemStack stack) {
        filter.set(i, stack.copy());
    }
    
    @Override
    public void setFilterSlotSize(int i, int size) {
        filterSize.set(i, size);
    }
    
    @Override
    public boolean isBlacklist() {
        return blacklist;
    }
    
    @Override
    public void setBlacklist(boolean black) {
        blacklist = black;
    }
    
    @Override
    public boolean hasStacksizeLimiters() {
        return false;
    }
}




