package thaumcraft.api.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge.EnumKnowledgeType;
import thaumcraft.api.capabilities.IPlayerWarp.EnumWarpType;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.api.research.ResearchCategory;

/**
 * Dummy implementation of IInternalMethodHandler that does nothing.
 * Used before Thaumcraft fully initializes to prevent null pointer exceptions.
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public class DummyInternalMethodHandler implements IInternalMethodHandler {
    
    @Override
    public boolean addKnowledge(Player player, EnumKnowledgeType type, ResearchCategory category, int amount) {
        return false;
    }
    
    @Override
    public boolean progressResearch(Player player, String researchkey) {
        return false;
    }
    
    @Override
    public boolean completeResearch(Player player, String researchkey) {
        return false;
    }
    
    @Override
    public boolean doesPlayerHaveRequisites(Player player, String researchkey) {
        return false;
    }

    @Override
    public void addWarpToPlayer(Player player, int amount, EnumWarpType type) {
        // No-op
    }
    
    @Override
    public int getActualWarp(Player player) {
        return 0;
    }

    @Override
    public AspectList getObjectAspects(ItemStack is) {
        return null;
    }

    @Override
    public AspectList generateTags(ItemStack is) {
        return null;
    }

    @Override
    public float drainVis(Level level, BlockPos pos, float amount, boolean simulate) {
        return 0;
    }

    @Override
    public float drainFlux(Level level, BlockPos pos, float amount, boolean simulate) {
        return 0;
    }

    @Override
    public void addVis(Level level, BlockPos pos, float amount) {
        // No-op
    }

    @Override
    public void addFlux(Level level, BlockPos pos, float amount, boolean showEffect) {
        // No-op
    }

    @Override
    public float getTotalAura(Level level, BlockPos pos) {
        return 0;
    }

    @Override
    public float getVis(Level level, BlockPos pos) {
        return 0;
    }

    @Override
    public float getFlux(Level level, BlockPos pos) {
        return 0;
    }

    @Override
    public int getAuraBase(Level level, BlockPos pos) {
        return 0;
    }

    @Override
    public void registerSeal(ISeal seal) {
        // No-op
    }

    @Override
    public ISeal getSeal(String key) {
        return null;
    }

    @Override
    public ISealEntity getSealEntity(String dimension, SealPos pos) {
        return null;
    }

    @Override
    public void addGolemTask(String dimension, Task task) {
        // No-op
    }

    @Override
    public boolean shouldPreserveAura(Level level, Player player, BlockPos pos) {
        return false;
    }

    @Override
    public ItemStack getSealStack(String key) {
        return ItemStack.EMPTY;
    }
}




