package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.parts.*;
import thaumcraft.common.golems.GolemProperties;
import thaumcraft.common.golems.ItemGolemPlacer;
import thaumcraft.common.menu.GolemBuilderMenu;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;

/**
 * TileGolemBuilder - Tile entity for the Golem Builder block.
 * 
 * Allows assembling golems from component parts. In the original mod,
 * this required essentia (Machina aspect) to power the crafting process.
 * 
 * This simplified version creates golems when components are provided.
 * 
 * Inventory slots:
 * - Slot 0: Output (golem placer)
 */
public class TileGolemBuilder extends TileThaumcraftInventory implements MenuProvider {
    
    // Current golem being built (as packed long)
    private long golemProps = -1L;
    
    // Crafting progress (public for menu sync)
    public int cost = 0;
    public int maxCost = 0;
    
    // Animation
    public int pressAnimation = 0;
    
    // Ticking
    private int tickCounter = 0;
    
    // Part selection state
    private int selectedMaterial = 0;
    private int selectedHead = 0;
    private int selectedArms = 0;
    private int selectedLegs = 0;
    private int selectedAddon = 0;
    
    public TileGolemBuilder(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOLEM_BUILDER.get(), pos, state, 1);
    }
    
    // ==================== MenuProvider ====================
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thaumcraft.golem_builder");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GolemBuilderMenu(containerId, playerInventory, this);
    }
    
    // ==================== Ticking ====================
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileGolemBuilder tile) {
        tile.tickCounter++;
        
        if (tile.cost > 0 && tile.golemProps >= 0) {
            // Progress crafting
            if (tile.tickCounter % 5 == 0) {
                tile.cost--;
                tile.setChanged();
                
                // Complete crafting
                if (tile.cost <= 0) {
                    tile.completeCraft();
                }
            }
        }
    }
    
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileGolemBuilder tile) {
        // Animate press
        if (tile.pressAnimation < 90 && tile.cost > 0 && tile.golemProps >= 0) {
            tile.pressAnimation += 6;
            if (tile.pressAnimation >= 60) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.66f, 
                        1.0f + level.random.nextFloat() * 0.1f, false);
            }
        }
        
        // Retract press when done
        if (tile.pressAnimation > 0 && (tile.cost <= 0 || tile.golemProps < 0)) {
            tile.pressAnimation -= 3;
        }
    }
    
    // ==================== Crafting ====================
    
    /**
     * Start crafting a golem with the given properties.
     * Returns true if crafting started successfully.
     */
    public boolean startCraft(long golemId) {
        if (level == null || level.isClientSide) return false;
        
        IGolemProperties props = GolemProperties.fromLong(golemId);
        
        // Check if output slot can accept result
        ItemStack output = getItem(0);
        ItemStack result = createGolemStack(props);
        
        if (!output.isEmpty()) {
            if (output.getCount() >= output.getMaxStackSize()) return false;
            if (!ItemStack.isSameItemSameComponents(output, result)) return false;
        }
        
        // Calculate crafting cost based on traits and components
        int craftCost = props.getTraits().size() * 2;
        for (ItemStack component : props.generateComponents()) {
            craftCost += component.getCount();
        }
        
        this.golemProps = golemId;
        this.cost = craftCost;
        this.maxCost = craftCost;
        
        setChanged();
        syncTile(false);
        
        return true;
    }
    
    /**
     * Complete the current craft and output the golem placer.
     */
    private void completeCraft() {
        if (golemProps < 0) return;
        
        IGolemProperties props = GolemProperties.fromLong(golemProps);
        ItemStack result = createGolemStack(props);
        
        ItemStack output = getItem(0);
        if (output.isEmpty()) {
            setItem(0, result);
        } else if (ItemStack.isSameItemSameComponents(output, result) && output.getCount() < output.getMaxStackSize()) {
            output.grow(1);
        }
        
        // Play completion sound
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        
        // Reset state
        golemProps = -1L;
        cost = 0;
        maxCost = 0;
        
        setChanged();
        syncTile(false);
    }
    
    private ItemStack createGolemStack(IGolemProperties props) {
        ItemStack stack = new ItemStack(ModItems.GOLEM_PLACER.get());
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putLong("props", props.toLong());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        return stack;
    }
    
    // ==================== Part Selection ====================
    
    /**
     * Cycle through available materials.
     * @param direction positive to go forward, negative to go backward
     */
    public void cycleMaterial(int direction) {
        GolemMaterial[] mats = GolemMaterial.getMaterials();
        int count = 0;
        for (GolemMaterial m : mats) if (m != null) count++;
        if (count == 0) return;
        
        selectedMaterial = (selectedMaterial + direction + count) % count;
        setChanged();
        syncTile(false);
    }
    
    /**
     * Cycle through available heads.
     */
    public void cycleHead(int direction) {
        GolemHead[] heads = GolemHead.getHeads();
        int count = 0;
        for (GolemHead h : heads) if (h != null) count++;
        if (count == 0) return;
        
        selectedHead = (selectedHead + direction + count) % count;
        setChanged();
        syncTile(false);
    }
    
    /**
     * Cycle through available arms.
     */
    public void cycleArms(int direction) {
        GolemArm[] arms = GolemArm.getArms();
        int count = 0;
        for (GolemArm a : arms) if (a != null) count++;
        if (count == 0) return;
        
        selectedArms = (selectedArms + direction + count) % count;
        setChanged();
        syncTile(false);
    }
    
    /**
     * Cycle through available legs.
     */
    public void cycleLegs(int direction) {
        GolemLeg[] legs = GolemLeg.getLegs();
        int count = 0;
        for (GolemLeg l : legs) if (l != null) count++;
        if (count == 0) return;
        
        selectedLegs = (selectedLegs + direction + count) % count;
        setChanged();
        syncTile(false);
    }
    
    /**
     * Cycle through available addons.
     */
    public void cycleAddon(int direction) {
        GolemAddon[] addons = GolemAddon.getAddons();
        int count = 0;
        for (GolemAddon a : addons) if (a != null) count++;
        if (count == 0) return;
        
        selectedAddon = (selectedAddon + direction + count) % count;
        setChanged();
        syncTile(false);
    }
    
    /**
     * Attempt to craft the currently selected golem.
     */
    public void tryCraft(Player player) {
        if (level == null || level.isClientSide) return;
        if (cost > 0) return; // Already crafting
        
        // Build the golem properties from current selections
        GolemProperties props = new GolemProperties();
        
        GolemMaterial mat = GolemMaterial.getById(selectedMaterial);
        GolemHead head = GolemHead.getById(selectedHead);
        GolemArm arms = GolemArm.getById(selectedArms);
        GolemLeg legs = GolemLeg.getById(selectedLegs);
        GolemAddon addon = GolemAddon.getById(selectedAddon);
        
        if (mat != null) props.setMaterial(mat);
        if (head != null) props.setHead(head);
        if (arms != null) props.setArms(arms);
        if (legs != null) props.setLegs(legs);
        if (addon != null) props.setAddon(addon);
        
        // Start the craft
        startCraft(props.toLong());
    }
    
    // ==================== Getters ====================
    
    public long getGolemProps() {
        return golemProps;
    }
    
    public int getSelectedMaterial() {
        return selectedMaterial;
    }
    
    public int getSelectedHead() {
        return selectedHead;
    }
    
    public int getSelectedArms() {
        return selectedArms;
    }
    
    public int getSelectedLegs() {
        return selectedLegs;
    }
    
    public int getSelectedAddon() {
        return selectedAddon;
    }
    
    /**
     * Build current golem properties from selections (for preview).
     */
    public IGolemProperties getCurrentGolemProperties() {
        GolemProperties props = new GolemProperties();
        GolemMaterial mat = GolemMaterial.getById(selectedMaterial);
        GolemHead head = GolemHead.getById(selectedHead);
        GolemArm arms = GolemArm.getById(selectedArms);
        GolemLeg legs = GolemLeg.getById(selectedLegs);
        GolemAddon addon = GolemAddon.getById(selectedAddon);
        
        if (mat != null) props.setMaterial(mat);
        if (head != null) props.setHead(head);
        if (arms != null) props.setArms(arms);
        if (legs != null) props.setLegs(legs);
        if (addon != null) props.setAddon(addon);
        
        return props;
    }
    
    public float getCraftingProgress() {
        if (maxCost <= 0) return 0;
        return 1.0f - (float) cost / maxCost;
    }
    
    // ==================== Item Validation ====================
    
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // Only allow golem placers in output slot
        return stack.getItem() instanceof ItemGolemPlacer;
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putLong("golem", golemProps);
        tag.putInt("cost", cost);
        tag.putInt("maxCost", maxCost);
        tag.putInt("selMat", selectedMaterial);
        tag.putInt("selHead", selectedHead);
        tag.putInt("selArms", selectedArms);
        tag.putInt("selLegs", selectedLegs);
        tag.putInt("selAddon", selectedAddon);
    }
    
    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        golemProps = tag.getLong("golem");
        cost = tag.getInt("cost");
        maxCost = tag.getInt("maxCost");
        selectedMaterial = tag.getInt("selMat");
        selectedHead = tag.getInt("selHead");
        selectedArms = tag.getInt("selArms");
        selectedLegs = tag.getInt("selLegs");
        selectedAddon = tag.getInt("selAddon");
    }
    
    // ==================== Rendering ====================
    
    public AABB getRenderBoundingBox() {
        return new AABB(
            worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
            worldPosition.getX() + 2, worldPosition.getY() + 2, worldPosition.getZ() + 2
        );
    }
}




