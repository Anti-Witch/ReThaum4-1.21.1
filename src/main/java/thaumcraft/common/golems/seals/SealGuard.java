package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.tasks.TaskHandler;

import java.util.List;
import java.util.Random;

/**
 * SealGuard - Makes golems attack hostile entities in an area.
 * 
 * Features:
 * - Configurable guard area
 * - Target selection (mobs, animals, players)
 * - Requires FIGHTER trait
 * 
 * Ported from 1.12.2.
 */
public class SealGuard implements ISeal, ISealConfigArea, ISealConfigToggles {
    
    private int delay;
    protected SealToggle[] props;
    private ResourceLocation icon;
    
    public SealGuard() {
        delay = new Random(System.nanoTime()).nextInt(22);
        props = new SealToggle[] {
            new SealToggle(true, "pmob", "golem.prop.mob"),
            new SealToggle(false, "panimal", "golem.prop.animal"),
            new SealToggle(false, "pplayer", "golem.prop.player")
        };
        icon = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_guard");
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:guard";
    }
    
    @Override
    public void tickSeal(Level level, ISealEntity seal) {
        if (delay++ % 20 != 0) {
            return;
        }
        
        AABB area = GolemHelper.getBoundsForArea(seal);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);
        
        if (!targets.isEmpty()) {
            for (LivingEntity target : targets) {
                if (isValidTarget(target)) {
                    Task task = new Task(seal.getSealPos(), target);
                    task.setPriority(seal.getPriority());
                    task.setLifespan((short) 10);
                    TaskHandler.addTask(level.dimension(), task);
                }
            }
        }
    }
    
    private boolean isValidTarget(LivingEntity target) {
        boolean valid = false;
        
        // Check if targeting mobs/monsters
        if (props[0].getValue() && (target instanceof Enemy || target instanceof Monster)) {
            valid = true;
        }
        
        // Check if targeting animals
        if (props[1].getValue() && target instanceof Animal) {
            valid = true;
        }
        
        // Check if targeting players (only if PVP enabled)
        if (props[2].getValue() && target instanceof Player) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && server.isPvpAllowed()) {
                valid = true;
            }
        }
        
        return valid;
    }
    
    @Override
    public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
        if (task.getEntity() != null && 
            task.getEntity() instanceof LivingEntity target && 
            isValidTarget(target)) {
            
            if (golem instanceof Mob mob) {
                mob.setTarget(target);
            }
            golem.addRankXp(1);
        }
        task.setSuspended(true);
    }
    
    @Override
    public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        task.setSuspended(true);
        return true;
    }
    
    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        return task.getEntity() != null && !golem.getGolemEntity().isAlliedTo(task.getEntity());
    }
    
    @Override
    public boolean canPlaceAt(Level level, BlockPos pos, Direction side) {
        return !level.isEmptyBlock(pos);
    }
    
    @Override
    public ResourceLocation getSealIcon() {
        return icon;
    }
    
    public int[] getGuiCategories() {
        return new int[] { 2, 0, 4 };
    }
    
    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.FIGHTER };
    }
    
    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return null;
    }
    
    @Override
    public void onTaskSuspension(Level level, Task task) {
    }
    
    @Override
    public void readCustomNBT(CompoundTag nbt) {
    }
    
    @Override
    public void writeCustomNBT(CompoundTag nbt) {
    }
    
    @Override
    public void onRemoval(Level level, BlockPos pos, Direction side) {
    }
    
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




