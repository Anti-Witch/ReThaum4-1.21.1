package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.init.ModBlockEntities;

/**
 * TileJarNode - Node in a jar (for node jarring mechanic).
 */
public class TileJarNode extends BlockEntity {

    private Aspect aspect = null;
    private int amount = 0;

    public TileJarNode(BlockPos pos, BlockState state) {
        super(ModBlockEntities.JAR_NODE.get(), pos, state);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Aspect")) {
            this.aspect = Aspect.getAspect(tag.getString("Aspect"));
        }
        this.amount = tag.getInt("Amount");
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.aspect != null) {
            tag.putString("Aspect", this.aspect.getTag());
        }
        tag.putInt("Amount", this.amount);
    }

    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1);
    }

    public boolean canInputFrom(Direction face) {
        return false;
    }

    public boolean canOutputTo(Direction face) {
        return false;
    }

    public int getSuction(Direction face) {
        return 0;
    }

    public AspectList getEssentia() {
        AspectList list = new AspectList();
        if (aspect != null && amount > 0) {
            list.add(aspect, amount);
        }
        return list;
    }

    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return 0;
    }

    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return 0;
    }

    public Aspect getSuctionType(Direction face) {
        return null;
    }

    public boolean isConnectable(Direction face) {
        return false;
    }

    public void setSuction(Aspect aspect, int amount) {
        // Stub
    }

    public Aspect getAspect() {
        return aspect;
    }

    public void setAspect(Aspect aspect) {
        this.aspect = aspect;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}