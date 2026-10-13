package thaumcraft.common.golems.seals;
import thaumcraft.common.lib.utils.NBTHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * SealHarvest - Makes golems harvest grown crops.
 * 
 * Features:
 * - Configurable harvest area
 * - Replanting option
 * - Provision request for seeds
 * 
 * Ported from 1.12.2.
 */
public class SealHarvest implements ISeal, ISealConfigArea, ISealConfigToggles {
    
    private int delay;
    private int count = 0;
    private HashMap<Long, ReplantInfo> replantTasks = new HashMap<>();
    private ResourceLocation icon;
    protected SealToggle[] props;
    
    public SealHarvest() {
        delay = new Random(System.nanoTime()).nextInt(33);
        icon = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_harvest");
        props = new SealToggle[] {
            new SealToggle(true, "prep", "golem.prop.replant"),
            new SealToggle(false, "ppro", "golem.prop.provision")
        };
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:harvest";
    }
    
    @Override
    public void tickSeal(Level level, ISealEntity seal) {
        // Periodic cleanup of replant tasks outside area
        if (delay % 100 == 0) {
            var area = GolemHelper.getBoundsForArea(seal);
            Iterator<Long> it = replantTasks.keySet().iterator();
            while (it.hasNext()) {
                BlockPos pos = BlockPos.of(it.next());
                if (!area.contains(Vec3.atCenterOf(pos))) {
                    ReplantInfo info = replantTasks.get(pos.asLong());
                    if (info != null) {
                        Task task = TaskHandler.getTask(level.dimension(), info.taskId);
                        if (task != null) {
                            task.setSuspended(true);
                        }
                    }
                    it.remove();
                }
            }
        }
        
        if (delay++ % 5 != 0) {
            return;
        }
        
        BlockPos pos = GolemHelper.getPosInArea(seal, count++);
        
        // Check for grown crops
        if (isGrownCrop(level, pos)) {
            Task task = new Task(seal.getSealPos(), pos);
            task.setPriority(seal.getPriority());
            TaskHandler.addTask(level.dimension(), task);
        }
        // Check for replant tasks
        else if (props[0].getValue() && replantTasks.containsKey(pos.asLong()) && level.isEmptyBlock(pos)) {
            ReplantInfo info = replantTasks.get(pos.asLong());
            Task existingTask = TaskHandler.getTask(level.dimension(), info.taskId);
            if (existingTask == null) {
                Task newTask = new Task(seal.getSealPos(), info.pos);
                newTask.setPriority(seal.getPriority());
                TaskHandler.addTask(level.dimension(), newTask);
                info.taskId = newTask.getId();
            }
        }
    }
    
    /**
     * Check if a block is a fully grown crop
     */
    private boolean isGrownCrop(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        return false;
    }
    
    @Override
    public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        if (isGrownCrop(level, task.getPos())) {
            BlockState state = level.getBlockState(task.getPos());
            
            // Break the crop
            level.destroyBlock(task.getPos(), true);
            golem.addRankXp(1);
            golem.swingArm();
            
            // TODO: Handle replanting when seed system is implemented
        }
        
        task.setSuspended(true);
        return true;
    }
    
    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        // Check for replant task
        if (replantTasks.containsKey(task.getPos().asLong())) {
            ReplantInfo info = replantTasks.get(task.getPos().asLong());
            if (info.taskId == task.getId()) {
                boolean hasSeeds = golem.isCarrying(info.seed);
                if (!hasSeeds && props[1].getValue()) {
                    // Request provisioning
                    ISealEntity sealEntity = SealHandler.getSealEntity(golem.getGolemWorld().dimension(), task.getSealPos());
                    if (sealEntity != null) {
                        GolemHelper.requestProvisioning(golem.getGolemWorld(), sealEntity, info.seed);
                    }
                }
                return hasSeeds;
            }
        }
        return true;
    }
    
    @Override
    public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
    }
    
    @Override
    public void onTaskSuspension(Level level, Task task) {
    }
    
    @Override
    public boolean canPlaceAt(Level level, BlockPos pos, Direction side) {
        return !level.isEmptyBlock(pos);
    }
    
    @Override
    public ResourceLocation getSealIcon() {
        return icon;
    }
    
    @Override
    public void onRemoval(Level level, BlockPos pos, Direction side) {
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
    
    public int[] getGuiCategories() {
        return new int[] { 2, 3, 0, 4 };
    }
    
    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.DEFT, EnumGolemTrait.SMART };
    }
    
    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return null;
    }
    
    @Override
    public void readCustomNBT(CompoundTag nbt) {
        ListTag list = nbt.getList("replant", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            long loc = tag.getLong("taskloc");
            byte face = tag.getByte("taskface");
            boolean farmland = tag.getBoolean("farmland");
            ItemStack seed = NBTHelper.fromNbt(tag);
            replantTasks.put(loc, new ReplantInfo(BlockPos.of(loc), Direction.values()[face], 0, seed, farmland));
        }
    }
    
    @Override
    public void writeCustomNBT(CompoundTag nbt) {
        if (props[0].getValue()) {
            ListTag list = new ListTag();
            for (Long key : replantTasks.keySet()) {
                ReplantInfo info = replantTasks.get(key);
                CompoundTag tag = new CompoundTag();
                tag.putLong("taskloc", info.pos.asLong());
                tag.putByte("taskface", (byte) info.face.ordinal());
                tag.putBoolean("farmland", info.farmland);
                info.seed.save(net.minecraft.core.RegistryAccess.EMPTY, tag);
                list.add(tag);
            }
            nbt.put("replant", list);
        }
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
    
    /**
     * Info for replanting crops
     */
    private static class ReplantInfo {
        Direction face;
        BlockPos pos;
        int taskId;
        ItemStack seed;
        boolean farmland;
        
        public ReplantInfo(BlockPos pos, Direction face, int taskId, ItemStack seed, boolean farmland) {
            this.pos = pos;
            this.face = face;
            this.taskId = taskId;
            this.seed = seed;
            this.farmland = farmland;
        }
    }
}




