package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.common.world.aura.AuraHandler;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModSounds;

import java.util.HashMap;

/**
 * TileFocalManipulator - Crafting station for creating and modifying foci.
 * 
 * Allows players to:
 * - Combine focus nodes into a spell configuration
 * - Name the focus
 * - Craft the focus using vis and crystal essences
 * 
 * Ported from 1.12.2
 */
public class TileFocalManipulator extends TileThaumcraftInventory {
    
    // Current vis being consumed during crafting
    public float vis = 0.0f;
    
    // Node data for the focus being constructed
    public HashMap<Integer, FocusElementNode> data = new HashMap<>();
    
    // Focus name
    public String focusName = "";
    
    // Internal tick counter
    private int ticks = 0;
    
    // Whether to gather aura this tick
    public boolean doGather = false;
    
    // Crafting costs
    public float visCost = 0.0f;
    public int xpCost = 0;
    
    // Crystal requirements (internal)
    private AspectList crystals = new AspectList();
    
    // Crystal requirements (synced to client for display)
    public AspectList crystalsSync = new AspectList();
    
    // Flag to reset GUI on client
    public boolean doGuiReset = false;
    
    public TileFocalManipulator(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOCAL_MANIPULATOR.get(), pos, state, 1);
        syncedSlots = new int[] { 0 };
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putFloat("vis", vis);
        tag.putString("focusName", focusName);
        crystalsSync.writeToNBT(tag, "crystals");
        
        // Serialize node data
        ListTag nodeList = new ListTag();
        for (FocusElementNode node : data.values()) {
            nodeList.add(node.serialize());
        }
        tag.put("nodes", nodeList);
    }
    
    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        vis = tag.getFloat("vis");
        focusName = tag.getString("focusName");
        crystalsSync = new AspectList();
        crystalsSync.readFromNBT(tag, "crystals");
        
        // Deserialize node data
        ListTag nodeList = tag.getList("nodes", Tag.TAG_COMPOUND);
        data.clear();
        for (int i = 0; i < nodeList.size(); i++) {
            CompoundTag nodeTag = nodeList.getCompound(i);
            FocusElementNode node = new FocusElementNode();
            node.deserialize(nodeTag);
            data.put(node.id, node);
        }
    }
    
    // ==================== Inventory ====================
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack prev = getItem(slot);
        super.setItem(slot, stack);
        
        // Reset focus data when focus is changed
        if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, prev)) {
            if (level != null && level.isClientSide) {
                data.clear();
                doGuiReset = true;
            } else {
                vis = 0.0f;
                crystalsSync = new AspectList();
                setChanged();
                syncTile(false);
            }
        }
    }
    
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemFocus;
    }
    
    // ==================== Tick ====================
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileFocalManipulator tile) {
        tile.ticks++;
        boolean complete = false;
        
        if (tile.ticks % 20 == 0) {
            // Check if we should still be crafting
            if (tile.vis > 0.0f) {
                ItemStack focus = tile.getItem(0);
                if (focus.isEmpty() || !(focus.getItem() instanceof ItemFocus)) {
                    // Focus removed during craft - cancel
                    complete = true;
                    tile.vis = 0.0f;
                    level.playSound(null, pos, ModSounds.WAND_FAIL.get(), SoundSource.BLOCKS, 0.33f, 1.0f);
                }
            }
            
            // Consume vis during crafting
            if (!complete && tile.vis > 0.0f) {
                float amt = tile.spendAura(Math.min(20.0f, tile.vis));
                if (amt > 0.0f) {
                    level.blockEvent(pos, state.getBlock(), 5, 1);
                    tile.vis -= amt;
                    tile.syncTile(false);
                    tile.setChanged();
                }
                
                // Crafting complete
                if (tile.vis <= 0.0f) {
                    ItemStack focus = tile.getItem(0);
                    if (!focus.isEmpty() && focus.getItem() instanceof ItemFocus) {
                        complete = true;
                        tile.endCraft();
                    }
                }
            }
        }
        
        if (complete) {
            tile.vis = 0.0f;
            tile.syncTile(false);
            tile.setChanged();
        }
    }
    
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileFocalManipulator tile) {
        tile.ticks++;
        
        // Visual effects during crafting
        if (tile.vis > 0.0f && level.random.nextFloat() < 0.5f) {
            // TODO: Add particle effects when FX system is ported
            // FXDispatcher.INSTANCE.drawGenericParticles(...)
        }
    }
    
    // ==================== Aura Consumption ====================
    
    /**
     * Spend aura from surrounding chunks for crafting.
     * If arcane workbench charger is above, draws from wider area.
     */
    public float spendAura(float amount) {
        if (level == null) return 0.0f;
        
        // TODO: Check for arcane workbench charger for increased range
        // if (level.getBlockState(worldPosition.above()).getBlock() == ModBlocks.ARCANE_WORKBENCH_CHARGER) {
        //     return drainFromWideArea(amount);
        // }
        
        return AuraHandler.drainVis(level, worldPosition, amount, false);
    }
    
    // ==================== Focus Generation ====================
    
    /**
     * Generate a FocusPackage from the current node data.
     */
    private FocusPackage generateFocus() {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        FocusPackage core = new FocusPackage();
        int totalComplexity = 0;
        HashMap<String, Integer> compCount = new HashMap<>();
        
        // Calculate complexity with multipliers for duplicate nodes
        for (FocusElementNode node : data.values()) {
            if (node.node != null) {
                int count = compCount.getOrDefault(node.node.getKey(), 0) + 1;
                node.complexityMultiplier = 0.5f * (count + 1);
                compCount.put(node.node.getKey(), count);
                totalComplexity += (int)(node.node.getComplexity() * node.complexityMultiplier);
            }
        }
        
        core.setComplexity(totalComplexity);
        
        // Build the focus tree starting from root node (id 0)
        FocusElementNode root = data.get(0);
        if (root != null) {
            traverseChildren(core, root);
        }
        
        return core;
    }
    
    /**
     * Recursively traverse and add child nodes to the focus package.
     */
    private void traverseChildren(FocusPackage currentPackage, FocusElementNode currentNode) {
        if (currentPackage == null || currentNode == null) {
            return;
        }
        
        currentPackage.addNode(currentNode.node);
        
        if (currentNode.children == null || currentNode.children.length == 0) {
            return;
        }
        
        if (currentNode.children.length == 1) {
            // Single child - continue same package
            traverseChildren(currentPackage, data.get(currentNode.children[0]));
        } else {
            // Multiple children - split node
            if (currentNode.node instanceof FocusModSplit splitNode) {
                splitNode.getSplitPackages().clear();
                for (int childId : currentNode.children) {
                    FocusPackage splitPackage = new FocusPackage();
                    traverseChildren(splitPackage, data.get(childId));
                    splitNode.getSplitPackages().add(splitPackage);
                }
            }
        }
    }
    
    // ==================== Crafting ====================
    
    /**
     * Complete the focus crafting process.
     */
    public void endCraft() {
        vis = 0.0f;
        ItemStack focus = getItem(0);
        
        if (!focus.isEmpty() && focus.getItem() instanceof ItemFocus) {
            FocusPackage core = generateFocus();
            if (core != null && level != null) {
                level.playSound(null, worldPosition, ModSounds.WAND.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
                
                // Remove color override if any
                focus.remove(net.minecraft.core.component.DataComponents.DYED_COLOR);
                
                // Set name and package
                focus.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(focusName));
                ItemFocus.setPackage(focus, core);
                
                setItem(0, focus);
                crystalsSync = new AspectList();
                data.clear();
                syncTile(false);
                setChanged();
            }
        }
    }
    
    /**
     * Start the focus crafting process.
     * @param id Unused (legacy)
     * @param player The crafting player
     * @return true if crafting started successfully
     */
    public boolean startCraft(int id, Player player) {
        if (level == null || level.isClientSide) return false;
        if (data == null || data.isEmpty() || vis > 0.0f) return false;
        
        ItemStack focus = getItem(0);
        if (focus.isEmpty() || !(focus.getItem() instanceof ItemFocus focusItem)) {
            return false;
        }
        
        int maxComplexity = focusItem.getMaxComplexity();
        int totalComplexity = 0;
        crystals = new AspectList();
        HashMap<String, Integer> compCount = new HashMap<>();
        
        // Validate all nodes and calculate costs
        for (FocusElementNode node : data.values()) {
            if (node.node == null) {
                return false;
            }
            
            // Check player knows the research. TC4-faithful data no longer
            // contains the TC6 caster research keys, so a key that is not part
            // of the research registry is treated as "no requirement" instead
            // of blocking the element forever.
            String elementResearch = node.node.getResearch();
            if (elementResearch != null && !elementResearch.isEmpty()
                    && ResearchCategories.getResearch(elementResearch) != null
                    && !ThaumcraftCapabilities.knowsResearchStrict(player, elementResearch)) {
                return false;
            }
            
            // Calculate complexity with multiplier
            int count = compCount.getOrDefault(node.node.getKey(), 0) + 1;
            node.complexityMultiplier = 0.5f * (count + 1);
            compCount.put(node.node.getKey(), count);
            totalComplexity += (int)(node.node.getComplexity() * node.complexityMultiplier);
            
            // Track crystal requirements
            if (node.node.getAspect() != null) {
                crystals.add(node.node.getAspect(), 1);
            }
        }
        
        // Calculate vis and XP costs
        vis = (float)(totalComplexity * 10 + maxComplexity / 5);
        xpCost = (int)Math.max(1, Math.round(Math.sqrt(totalComplexity)));
        
        // Check XP
        if (!player.getAbilities().instabuild && player.experienceLevel < xpCost) {
            vis = 0.0f;
            return false;
        }
        
        // Consume XP
        if (!player.getAbilities().instabuild) {
            player.giveExperienceLevels(-xpCost);
        }
        
        // Check and consume crystals
        if (crystals.getAspects().length > 0) {
            ItemStack[] components = new ItemStack[crystals.getAspects().length];
            int idx = 0;
            
            for (Aspect aspect : crystals.getAspects()) {
                components[idx] = ThaumcraftApiHelper.makeCrystal(aspect, crystals.getAmount(aspect));
                idx++;
            }
            
            // Verify player has all crystals
            for (ItemStack component : components) {
                if (!playerHasItem(player, component)) {
                    vis = 0.0f;
                    return false;
                }
            }
            
            // Consume crystals
            for (ItemStack component : components) {
                consumePlayerItem(player, component);
            }
            
            crystalsSync = crystals.copy();
        }
        
        setChanged();
        syncTile(false);
        level.playSound(null, worldPosition, ModSounds.CRAFT_START.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }
    
    /**
     * Check if player has the required item.
     */
    private boolean playerHasItem(Player player, ItemStack required) {
        int needed = required.getCount();
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItem(stack, required)) {
                needed -= stack.getCount();
                if (needed <= 0) return true;
            }
        }
        return false;
    }
    
    /**
     * Consume item from player inventory.
     */
    private void consumePlayerItem(Player player, ItemStack required) {
        int needed = required.getCount();
        for (int i = 0; i < player.getInventory().items.size() && needed > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (ItemStack.isSameItem(stack, required)) {
                int take = Math.min(needed, stack.getCount());
                stack.shrink(take);
                needed -= take;
            }
        }
    }
    
    // ==================== Block Events ====================
    
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) {
            doGuiReset = true;
            return true;
        }
        if (id == 5) {
            // Vis sparkle effect
            // TODO: Add particle effect when FX system is ported
            return true;
        }
        return super.triggerEvent(id, param);
    }
    
}




