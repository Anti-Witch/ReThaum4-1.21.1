package thaumcraft.common.lib.events;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.EntityFluxRift;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXBlockBamf;
import thaumcraft.common.world.aura.AuraHandler;
import thaumcraft.common.world.aura.AuraThreadManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

/**
 * ServerEvents - Handles server-side tick events and scheduled tasks.
 * 
 * Features:
 * - Tick seal entities and golem tasks
 * - Delayed runnable execution (server and client)
 * - Block swap queue (for equal trade focus, etc.)
 * - Block break queue (for bore wand, etc.)
 * 
 * Ported from 1.12.2 with 1.20.1 compatibility updates.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ServerEvents {
    
    // Track tick counts per dimension for periodic tasks
    private static final Map<String, Integer> serverTicks = new HashMap<>();
    
    // Delayed runnable queues
    private static final Map<String, LinkedBlockingQueue<RunnableEntry>> serverRunList = new ConcurrentHashMap<>();
    private static final LinkedBlockingQueue<RunnableEntry> clientRunList = new LinkedBlockingQueue<>();
    
    // Block swap and break queues
    private static final Map<String, LinkedBlockingQueue<VirtualSwapper>> swapList = new ConcurrentHashMap<>();
    private static final Map<String, LinkedBlockingQueue<BreakData>> breakList = new ConcurrentHashMap<>();
    
    // Default predicate that always allows swapping
    public static final Predicate<SwapperPredicate> DEFAULT_PREDICATE = pred -> true;
    
    /**
     * World tick event - called every tick for each loaded dimension
     */
    @SubscribeEvent
    public static void onLevelTickPre(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level) {
            if (!AuraThreadManager.hasThread(level.dimension()) && AuraHandler.getAuraWorld(level.dimension()) != null) {
                AuraThreadManager.startThread(level.dimension());
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        
        String dimKey = level.dimension().location().toString();
        
        if (!serverTicks.containsKey(dimKey)) {
            serverTicks.put(dimKey, 0);
        }
        
        processRunnables(dimKey);
        tickBlockSwap(level);
        tickBlockBreak(level);
        
        int ticks = serverTicks.get(dimKey);
        
        if (ticks % 20 == 0) {
            TaskHandler.clearSuspendedOrExpiredTasks(level);
            
            ResourceKey<Level> dimension = level.dimension();
            CopyOnWriteArrayList<ChunkPos> dirtyChunks = AuraHandler.dirtyChunks.get(dimension);
            if (dirtyChunks != null && !dirtyChunks.isEmpty()) {
                for (ChunkPos pos : dirtyChunks) {
                    level.getChunkSource().getChunk(pos.x, pos.z, false);
                }
                dirtyChunks.clear();
            }
            
            if (AuraHandler.riftTrigger.containsKey(dimension)) {
                if (!ModConfig.wussMode) {
                    BlockPos riftPos = AuraHandler.riftTrigger.get(dimension);
                    EntityFluxRift.createRift(level, riftPos);
                }
                AuraHandler.riftTrigger.remove(dimension);
            }
        }
        
        SealHandler.tickSealEntities(level);
        serverTicks.put(dimKey, ticks + 1);
    }
    
    /**
     * Client tick event - process delayed client runnables
     */
    @SubscribeEvent
    public static void onClientTick(ServerTickEvent.Post event) {
        
        if (!clientRunList.isEmpty()) {
            LinkedBlockingQueue<RunnableEntry> temp = new LinkedBlockingQueue<>();
            while (!clientRunList.isEmpty()) {
                RunnableEntry entry = clientRunList.poll();
                if (entry != null) {
                    if (entry.delay > 0) {
                        entry.delay--;
                        temp.offer(entry);
                    } else {
                        try {
                            entry.runnable.run();
                        } catch (Exception e) {
                            Thaumcraft.LOGGER.warn("Error executing delayed client runnable", e);
                        }
                    }
                }
            }
            clientRunList.addAll(temp);
        }
    }
    
    /**
     * Process delayed server runnables for a dimension
     */
    private static void processRunnables(String dimKey) {
        LinkedBlockingQueue<RunnableEntry> rlist = serverRunList.get(dimKey);
        if (rlist == null) {
            serverRunList.put(dimKey, new LinkedBlockingQueue<>());
            return;
        }
        
        if (!rlist.isEmpty()) {
            LinkedBlockingQueue<RunnableEntry> temp = new LinkedBlockingQueue<>();
            while (!rlist.isEmpty()) {
                RunnableEntry entry = rlist.poll();
                if (entry != null) {
                    if (entry.delay > 0) {
                        entry.delay--;
                        temp.offer(entry);
                    } else {
                        try {
                            entry.runnable.run();
                        } catch (Exception e) {
                            Thaumcraft.LOGGER.warn("Error executing delayed server runnable", e);
                        }
                    }
                }
            }
            rlist.addAll(temp);
        }
    }
    
    /**
     * Process block swap queue
     */
    private static void tickBlockSwap(ServerLevel level) {
        String dimKey = level.dimension().location().toString();
        LinkedBlockingQueue<VirtualSwapper> queue = swapList.get(dimKey);
        if (queue == null || queue.isEmpty()) return;
        
        LinkedBlockingQueue<VirtualSwapper> nextQueue = new LinkedBlockingQueue<>();
        
        while (!queue.isEmpty()) {
            VirtualSwapper vs = queue.poll();
            if (vs == null) continue;
            
            BlockState currentState = level.getBlockState(vs.pos);
            
            // Check if swap is allowed
            boolean allow = currentState.getDestroySpeed(level, vs.pos) >= 0.0f;
            
            // Verify source matches (if specified)
            if (vs.source != null && vs.source != currentState) {
                allow = false;
            }
            
            // Check vis cost
            // TODO: Check aura vis when aura system is implemented
            // if (vs.visCost > 0.0f && AuraHelper.getVis(level, vs.pos) < vs.visCost) {
            //     allow = false;
            // }
            
            // Check if player can mine here
            if (vs.player != null && !level.mayInteract(vs.player, vs.pos)) {
                continue;
            }
            
            // Check predicate
            if (!allow || !vs.allowSwap.test(new SwapperPredicate(level, vs.player, vs.pos))) {
                continue;
            }
            
            // Check if target is same as current (skip if so)
            if (vs.target != null && !vs.target.isEmpty()) {
                Block targetBlock = Block.byItem(vs.target.getItem());
                if (targetBlock != Blocks.AIR && currentState.is(targetBlock)) {
                    continue;
                }
            }
            
            // Find target item in player inventory
            int slot = -1;
            if (!vs.consumeTarget || vs.target == null || vs.target.isEmpty()) {
                slot = 1; // Doesn't need to consume
            } else if (vs.player != null) {
                slot = findItemSlot(vs.player, vs.target);
                if (vs.player.isCreative()) {
                    slot = 1;
                }
            }
            
            if (slot < 0) continue;
            
            // Perform the swap
            if (vs.player != null && !vs.player.isCreative()) {
                // Consume target item
                if (vs.consumeTarget && slot >= 0) {
                    vs.player.getInventory().removeItem(slot, 1);
                }
                
                // Pick up replaced block
                if (vs.pickup) {
                    // TODO: Implement silk touch and fortune drops
                    ItemStack drop = new ItemStack(currentState.getBlock());
                    if (!drop.isEmpty()) {
                        if (!vs.player.getInventory().add(drop)) {
                            level.addFreshEntity(new ItemEntity(level, 
                                vs.pos.getX() + 0.5, vs.pos.getY() + 0.5, vs.pos.getZ() + 0.5, drop));
                        }
                    }
                }
                
                // Drain vis
                // TODO: Drain aura vis when aura system is implemented
            }
            
            // Place the new block
            if (vs.target == null || vs.target.isEmpty()) {
                level.removeBlock(vs.pos, false);
            } else {
                Block targetBlock = Block.byItem(vs.target.getItem());
                if (targetBlock != null && targetBlock != Blocks.AIR) {
                    level.setBlock(vs.pos, targetBlock.defaultBlockState(), 3);
                } else {
                    level.removeBlock(vs.pos, false);
                }
            }
            
            // Spawn FX
            if (vs.fx) {
                PacketHandler.sendToAllTrackingChunk(
                    new PacketFXBlockBamf(vs.pos, vs.color, true, vs.fancy, null),
                    level, vs.pos
                );
            }
            
            // Spread to adjacent blocks if lifespan > 0
            if (vs.lifespan > 0 && vs.source != null) {
                for (int xx = -1; xx <= 1; xx++) {
                    for (int yy = -1; yy <= 1; yy++) {
                        for (int zz = -1; zz <= 1; zz++) {
                            if (xx == 0 && yy == 0 && zz == 0) continue;
                            
                            BlockPos adjacent = vs.pos.offset(xx, yy, zz);
                            BlockState adjacentState = level.getBlockState(adjacent);
                            
                            if (adjacentState == vs.source && isBlockExposed(level, adjacent)) {
                                nextQueue.offer(new VirtualSwapper(
                                    adjacent, vs.source, vs.target, vs.consumeTarget,
                                    vs.lifespan - 1, vs.player, vs.fx, vs.fancy,
                                    vs.color, vs.pickup, vs.silk, vs.fortune,
                                    vs.allowSwap, vs.visCost
                                ));
                            }
                        }
                    }
                }
            }
        }
        
        if (!nextQueue.isEmpty()) {
            swapList.put(dimKey, nextQueue);
        }
    }
    
    /**
     * Process block break queue
     */
    private static void tickBlockBreak(ServerLevel level) {
        String dimKey = level.dimension().location().toString();
        LinkedBlockingQueue<BreakData> queue = breakList.get(dimKey);
        if (queue == null || queue.isEmpty()) return;
        
        LinkedBlockingQueue<BreakData> nextQueue = new LinkedBlockingQueue<>();
        
        while (!queue.isEmpty()) {
            BreakData bd = queue.poll();
            if (bd == null) continue;
            
            BlockState currentState = level.getBlockState(bd.pos);
            
            // Check if block still matches
            if (currentState != bd.source) {
                if (bd.fx) {
                    level.destroyBlockProgress(bd.pos.hashCode(), bd.pos, -1);
                }
                continue;
            }
            
            // Check if player can mine
            if (bd.player != null && !level.mayInteract(bd.player, bd.pos)) {
                continue;
            }
            
            // Check hardness
            if (currentState.getDestroySpeed(level, bd.pos) < 0.0f) {
                continue;
            }
            
            // Show break progress
            if (bd.fx) {
                int progress = (int)((1.0f - bd.durabilityCurrent / bd.durabilityMax) * 10.0f);
                level.destroyBlockProgress(bd.pos.hashCode(), bd.pos, progress);
            }
            
            // Apply damage
            bd.durabilityCurrent -= bd.strength;
            
            if (bd.durabilityCurrent <= 0.0f) {
                // Break the block
                level.destroyBlock(bd.pos, true, bd.player);
                
                if (bd.fx) {
                    level.destroyBlockProgress(bd.pos.hashCode(), bd.pos, -1);
                }
                
                // TODO: Drain vis when aura system is implemented
            } else {
                // Continue breaking next tick
                nextQueue.offer(bd);
            }
        }
        
        if (!nextQueue.isEmpty()) {
            breakList.put(dimKey, nextQueue);
        }
    }
    
    /**
     * Check if a block has any exposed face (air adjacent)
     */
    private static boolean isBlockExposed(Level level, BlockPos pos) {
        return level.getBlockState(pos.above()).isAir() ||
               level.getBlockState(pos.below()).isAir() ||
               level.getBlockState(pos.north()).isAir() ||
               level.getBlockState(pos.south()).isAir() ||
               level.getBlockState(pos.east()).isAir() ||
               level.getBlockState(pos.west()).isAir();
    }
    
    /**
     * Find the inventory slot containing an item
     */
    private static int findItemSlot(Player player, ItemStack target) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, target)) {
                return i;
            }
        }
        return -1;
    }
    
    // === Public API methods ===
    
    /**
     * Add a block swap to the queue.
     */
    public static void addSwapper(Level level, BlockPos pos, BlockState source, ItemStack target,
            boolean consumeTarget, int life, Player player, boolean fx, boolean fancy,
            int color, boolean pickup, boolean silk, int fortune,
            Predicate<SwapperPredicate> allowSwap, float visCost) {
        
        String dimKey = level.dimension().location().toString();
        LinkedBlockingQueue<VirtualSwapper> queue = swapList.computeIfAbsent(dimKey, k -> new LinkedBlockingQueue<>());
        queue.offer(new VirtualSwapper(pos, source, target, consumeTarget, life, player,
            fx, fancy, color, pickup, silk, fortune, allowSwap, visCost));
    }
    
    /**
     * Add a block break to the queue.
     */
    public static void addBreaker(Level level, BlockPos pos, BlockState source, Player player,
            boolean fx, boolean silk, int fortune, float strength,
            float durabilityCurrent, float durabilityMax, int delay, float visCost, Runnable onComplete) {
        
        String dimKey = level.dimension().location().toString();
        
        if (delay > 0) {
            addRunnableServer(level, () -> addBreaker(level, pos, source, player, fx, silk, fortune,
                strength, durabilityCurrent, durabilityMax, 0, visCost, onComplete), delay);
        } else {
            LinkedBlockingQueue<BreakData> queue = breakList.computeIfAbsent(dimKey, k -> new LinkedBlockingQueue<>());
            queue.offer(new BreakData(strength, durabilityCurrent, durabilityMax, pos, source,
                player, fx, silk, fortune, visCost));
            
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }
    
    /**
     * Add a delayed runnable to execute on the server.
     */
    public static void addRunnableServer(Level level, Runnable runnable, int delay) {
        if (level.isClientSide()) return;
        
        String dimKey = level.dimension().location().toString();
        LinkedBlockingQueue<RunnableEntry> rlist = serverRunList.computeIfAbsent(dimKey, k -> new LinkedBlockingQueue<>());
        rlist.add(new RunnableEntry(runnable, delay));
    }
    
    /**
     * Add a delayed runnable to execute on the client.
     */
    public static void addRunnableClient(Level level, Runnable runnable, int delay) {
        if (!level.isClientSide()) return;
        clientRunList.add(new RunnableEntry(runnable, delay));
    }
    
    /**
     * Level load event - initialize aura world for the dimension
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AuraThreadManager.onLevelLoad(level);
        }
    }
    
    /**
     * Level unload event - clean up aura world for the dimension
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AuraThreadManager.onLevelUnload(level);
        }
    }
    
    /**
     * Server stopping event - clean up resources
     */
    @SubscribeEvent
    public static void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        // Stop all aura threads
        AuraThreadManager.stopAllThreads();
        
        // Clear dirty chunk tracking
        AuraHandler.dirtyChunks.clear();
        AuraHandler.riftTrigger.clear();
        
        serverTicks.clear();
        serverRunList.clear();
        swapList.clear();
        breakList.clear();
        
        Thaumcraft.LOGGER.info("Thaumcraft server events cleaned up");
    }
    
    // === Inner Classes ===
    
    /**
     * Entry for delayed runnable execution
     */
    public static class RunnableEntry {
        Runnable runnable;
        int delay;
        
        public RunnableEntry(Runnable runnable, int delay) {
            this.runnable = runnable;
            this.delay = delay;
        }
    }
    
    /**
     * Predicate context for block swapping
     */
    public static class SwapperPredicate {
        public Level level;
        public Player player;
        public BlockPos pos;
        
        public SwapperPredicate(Level level, Player player, BlockPos pos) {
            this.level = level;
            this.player = player;
            this.pos = pos;
        }
    }
    
    /**
     * Data for virtual block swapping
     */
    public static class VirtualSwapper {
        BlockPos pos;
        BlockState source;
        ItemStack target;
        boolean consumeTarget;
        int lifespan;
        Player player;
        boolean fx;
        boolean fancy;
        int color;
        boolean pickup;
        boolean silk;
        int fortune;
        Predicate<SwapperPredicate> allowSwap;
        float visCost;
        
        VirtualSwapper(BlockPos pos, BlockState source, ItemStack target, boolean consumeTarget,
                int lifespan, Player player, boolean fx, boolean fancy, int color,
                boolean pickup, boolean silk, int fortune,
                Predicate<SwapperPredicate> allowSwap, float visCost) {
            this.pos = pos;
            this.source = source;
            this.target = target;
            this.consumeTarget = consumeTarget;
            this.lifespan = lifespan;
            this.player = player;
            this.fx = fx;
            this.fancy = fancy;
            this.color = color;
            this.pickup = pickup;
            this.silk = silk;
            this.fortune = fortune;
            this.allowSwap = allowSwap;
            this.visCost = visCost;
        }
    }
    
    /**
     * Data for gradual block breaking
     */
    public static class BreakData {
        float strength;
        float durabilityCurrent;
        float durabilityMax;
        BlockPos pos;
        BlockState source;
        Player player;
        boolean fx;
        boolean silk;
        int fortune;
        float visCost;
        
        public BreakData(float strength, float durabilityCurrent, float durabilityMax,
                BlockPos pos, BlockState source, Player player,
                boolean fx, boolean silk, int fortune, float visCost) {
            this.strength = strength;
            this.durabilityCurrent = durabilityCurrent;
            this.durabilityMax = durabilityMax;
            this.pos = pos;
            this.source = source;
            this.player = player;
            this.fx = fx;
            this.silk = silk;
            this.fortune = fortune;
            this.visCost = visCost;
        }
    }
}




