package thaumcraft.api.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.tasks.Task;

/**
 * Interface for seal behaviors.
 * Seals are placed on blocks and create tasks for golems to execute.
 */
public interface ISeal {

    /**
     * @return A unique string identifier for this seal. 
     * A good idea would be to append your modid before the identifier.
     * For example: "thaumcraft:fetch"
     * This will also be used to create the item model for the seal placer.
     */
    String getKey();

    /**
     * Check if this seal can be placed at the given position and side
     */
    boolean canPlaceAt(Level level, BlockPos pos, Direction side);

    /**
     * Called each tick to update the seal and create tasks
     */
    void tickSeal(Level level, ISealEntity seal);

    /**
     * Called when a golem starts working on a task from this seal
     */
    void onTaskStarted(Level level, IGolemAPI golem, Task task);

    /**
     * Called when a golem completes a task from this seal
     * @return true if the task was successfully completed
     */
    boolean onTaskCompletion(Level level, IGolemAPI golem, Task task);

    /**
     * Called when a task is suspended (e.g., golem dies or gets too far)
     */
    void onTaskSuspension(Level level, Task task);

    /**
     * Check if a golem can perform a specific task from this seal
     */
    boolean canGolemPerformTask(IGolemAPI golem, Task task);

    /**
     * Read seal-specific data from NBT
     */
    void readCustomNBT(CompoundTag nbt);

    /**
     * Write seal-specific data to NBT
     */
    void writeCustomNBT(CompoundTag nbt);

    /**
     * @return Icon used to render the seal in world.
     * Usually the same as your seal placer item icon.
     */
    ResourceLocation getSealIcon();

    /**
     * Called when the seal is removed from the world
     */
    void onRemoval(Level level, BlockPos pos, Direction side);

    /**
     * @return Container for seal configuration GUI (server-side)
     */
    Object returnContainer(Level level, Player player, BlockPos pos, Direction side, ISealEntity seal);

    /**
     * @return GUI screen for seal configuration (client-side)
     */
    @OnlyIn(Dist.CLIENT)
    Object returnGui(Level level, Player player, BlockPos pos, Direction side, ISealEntity seal);

    /**
     * @return Traits that a golem MUST have to use this seal
     */
    EnumGolemTrait[] getRequiredTags();

    /**
     * @return Traits that prevent a golem from using this seal
     */
    EnumGolemTrait[] getForbiddenTags();
}




