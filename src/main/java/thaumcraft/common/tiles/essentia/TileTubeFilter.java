package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.blocks.ILabelable;
import thaumcraft.common.items.consumables.ItemLabel;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModSounds;

/**
 * Filtered tube - only allows one aspect type to pass through.
 * The filter is set by applying a label with an aspect.
 */
public class TileTubeFilter extends TileTube implements IAspectContainer, ILabelable {

    public Aspect aspectFilter = null;

    public TileTubeFilter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileTubeFilter(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TUBE_FILTER.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        if (aspectFilter != null) {
            tag.putString("AspectFilter", aspectFilter.getTag());
        }
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        aspectFilter = Aspect.getAspect(tag.getString("AspectFilter"));
    }

    // ==================== Suction Override ====================

    @Override
    protected void calculateSuction(Aspect filter, boolean restrict, boolean directional) {
        // Always use our filter when calculating suction
        super.calculateSuction(aspectFilter, restrict, directional);
    }

    // ==================== IAspectContainer ====================
    // The container interface is used for setting the filter via phials

    @Override
    public AspectList getAspects() {
        if (aspectFilter != null) {
            return new AspectList().add(aspectFilter, -1);
        }
        return null;
    }

    @Override
    public void setAspects(AspectList aspects) {
        // Not used
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return false;
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

    // ==================== Filter Management ====================

    /**
     * Apply a label to the tube (TC4 BlockTube.onBlockActivated). Only labels that
     * carry an aspect can mark a filter.
     */
    @Override
    public boolean applyLabel(Player player, BlockPos pos, Direction side, ItemStack labelStack) {
        if (player.isShiftKeyDown() || aspectFilter != null) {
            return false;
        }
        Aspect labelAspect = (labelStack.getItem() instanceof ItemLabel label)
                ? label.getAspect(labelStack) : null;
        if (labelAspect == null) {
            return false;
        }
        setFilter(labelAspect);
        if (level != null && !level.isClientSide) {
            level.playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }

    public void setFilter(Aspect aspect) {
        this.aspectFilter = aspect;
        markDirtyAndSync();
    }

    public Aspect getFilter() {
        return aspectFilter;
    }

    public void clearFilter() {
        this.aspectFilter = null;
        markDirtyAndSync();
    }
}




