package thaumcraft.common.lib.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.entities.EntityFollowingItem;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * ToolEvents - Handles special effects from infusion enchantments on tools.
 * 
 * Enchantment effects:
 * - ARCING: Chain lightning attack to nearby enemies
 * - BURROWING: Mines the furthest block in a vein first (trees/ores)
 * - DESTRUCTIVE: 3x3 area mining
 * - REFINING: Extra drops from ores (bonus smelting results)
 * - COLLECTOR: Drops fly to the player
 * - LAMPLIGHT: Places light source when mining in dark areas
 * - SOUNDING: Right-click reveals nearby ores
 * - ESSENCE: Mobs drop aspect crystals
 * 
 * Ported from Thaumcraft 1.12.2 to 1.20.1
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ToolEvents {
    
    // Track last clicked face per player for DESTRUCTIVE enchantment
    private static final HashMap<Integer, Direction> lastFaceClicked = new HashMap<>();
    
    // Track blocked positions to prevent recursive mining
    public static final HashMap<String, ArrayList<BlockPos>> blockedBlocks = new HashMap<>();
    
    // Prevent recursive calls during DESTRUCTIVE mining
    private static boolean blockDestructiveRecursion = false;

    /**
     * Handle ARCING enchantment - chain lightning attack to nearby enemies.
     */
    @SubscribeEvent
    public static void playerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        
        InteractionHand hand = player.getUsedItemHand();
        if (hand == null) hand = InteractionHand.MAIN_HAND;
        
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty()) return;
        
        List<EnumInfusionEnchantment> enchantments = EnumInfusionEnchantment.getInfusionEnchantments(heldItem);
        
        // ARCING - Chain lightning to nearby enemies
        if (enchantments.contains(EnumInfusionEnchantment.ARCING) && event.getTarget().isAlive()) {
            int rank = EnumInfusionEnchantment.getInfusionEnchantmentLevel(heldItem, EnumInfusionEnchantment.ARCING);
            
            // Find nearby entities around the target
            List<Entity> targets = player.level().getEntitiesOfClass(Entity.class,
                    event.getTarget().getBoundingBox().inflate(1.5 + rank, 1.0 + rank / 2.0, 1.5 + rank),
                    e -> e != player && e != event.getTarget());
            
            int count = 0;
            if (targets.size() > 0) {
                for (Entity target : targets) {
                    if (target.isRemoved() || !target.isAlive()) continue;
                    
                    // Skip friendly entities
                    if (EntityUtils.isFriendly(player, target)) continue;
                    
                    // Only attack mobs, not other players (unless PvP)
                    if (!(target instanceof Mob)) continue;
                    if (target instanceof Player otherPlayer && otherPlayer.getName().equals(player.getName())) continue;
                    
                    if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                        // Calculate damage
                        double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                        float damage = (float) (attackDamage * 0.5f);
                        
                        // Apply damage
                        DamageSource damageSource = player.damageSources().playerAttack(player);
                        if (livingTarget.hurt(damageSource, damage)) {
                            // Knockback
                            float yawRad = player.getYRot() * ((float) Math.PI / 180f);
                            livingTarget.push(
                                    -Mth.sin(yawRad) * 0.5f,
                                    0.1,
                                    Mth.cos(yawRad) * 0.5f
                            );
                            
                            ++count;
                            
                            // Send slash effect packet (TODO: implement PacketFXSlash)
                            // if (!player.level().isClientSide) {
                            //     PacketHandler.sendToAllAround(new PacketFXSlash(...), ...);
                            // }
                        }
                    }
                    
                    if (count >= rank) break;
                }
                
                // Play wind sound if we hit anything
                if (count > 0 && !player.level().isClientSide) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.WIND.get(), SoundSource.PLAYERS,
                            1.0f, 0.9f + player.level().random.nextFloat() * 0.2f);
                }
            }
        }
    }

    /**
     * Handle SOUNDING enchantment - reveal ores when right-clicking while sneaking.
     */
    @SubscribeEvent
    public static void playerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getEntity() == null) return;
        
        Player player = event.getEntity();
        InteractionHand usedHand = player.getUsedItemHand();
        final InteractionHand hand = (usedHand != null) ? usedHand : InteractionHand.MAIN_HAND;
        
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty()) return;
        
        List<EnumInfusionEnchantment> enchantments = EnumInfusionEnchantment.getInfusionEnchantments(heldItem);
        
        // SOUNDING - Reveal ores when sneaking and right-clicking
        if (enchantments.contains(EnumInfusionEnchantment.SOUNDING) && player.isShiftKeyDown()) {
            heldItem.hurtAndBreak(5, player, player.getEquipmentSlotForItem(heldItem));
            
            BlockPos pos = event.getPos();
            event.getLevel().playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.WAND_FAIL.get(), SoundSource.BLOCKS,
                    0.2f, 0.2f + event.getLevel().random.nextFloat() * 0.2f);
            
            // Send scan source packet to reveal ores (TODO: implement PacketFXScanSource)
            int level = EnumInfusionEnchantment.getInfusionEnchantmentLevel(heldItem, EnumInfusionEnchantment.SOUNDING);
            if (player instanceof ServerPlayer serverPlayer) {
                // PacketHandler.sendTo(new PacketFXScanSource(pos, level), serverPlayer);
            }
        }
    }

    /**
     * Track left-clicked face for DESTRUCTIVE enchantment.
     */
    @SubscribeEvent
    public static void playerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() != null && event.getFace() != null) {
            lastFaceClicked.put(event.getEntity().getId(), event.getFace());
        }
    }

    /**
     * Add a blocked position to prevent mining during recursive operations.
     */
    public static void addBlockedBlock(Level level, BlockPos pos) {
        String dimKey = level.dimension().location().toString();
        blockedBlocks.computeIfAbsent(dimKey, k -> new ArrayList<>());
        ArrayList<BlockPos> list = blockedBlocks.get(dimKey);
        if (!list.contains(pos)) {
            list.add(pos);
        }
    }

    /**
     * Remove a blocked position.
     */
    public static void clearBlockedBlock(Level level, BlockPos pos) {
        String dimKey = level.dimension().location().toString();
        blockedBlocks.computeIfAbsent(dimKey, k -> new ArrayList<>());
        blockedBlocks.get(dimKey).remove(pos);
    }

    /**
     * Handle BURROWING enchantment during block break.
     */
    @SubscribeEvent
    public static void breakBlockEvent(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;
        
        String dimKey = level.dimension().location().toString();
        
        // Check if this block is being blocked from breaking
        if (blockedBlocks.containsKey(dimKey)) {
            ArrayList<BlockPos> list = blockedBlocks.get(dimKey);
            if (list != null && list.contains(event.getPos())) {
                event.setCanceled(true);
                return;
            }
        }
        
        if (level.isClientSide || event.getPlayer() == null) return;
        
        Player player = event.getPlayer();
        InteractionHand usedHand = player.getUsedItemHand();
        final InteractionHand hand = (usedHand != null) ? usedHand : InteractionHand.MAIN_HAND;
        
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty()) return;
        
        List<EnumInfusionEnchantment> enchantments = EnumInfusionEnchantment.getInfusionEnchantments(heldItem);
        
        // BURROWING - Break furthest block in vein first
        if (enchantments.contains(EnumInfusionEnchantment.BURROWING) && !player.isShiftKeyDown()) {
            if (isToolEffective(level, event.getPos(), heldItem) && isValidBurrowBlock(level, event.getPos())) {
                event.setCanceled(true);
                
                // Don't damage tool if this is the fake bore
                if (!player.getName().getString().equals("FakeThaumcraftBore")) {
                    heldItem.hurtAndBreak(1, player, player.getEquipmentSlotForItem(heldItem));
                }
                
                // Break the furthest connected block of the same type
                breakFurthestBlock(level, event.getPos(), event.getState(), player);
            }
        }
    }

    /**
     * Check if a block is valid for burrowing (logs or ores).
     */
    private static boolean isValidBurrowBlock(Level level, BlockPos pos) {
        return BlockUtils.isLog(level, pos) || BlockUtils.isOre(level, pos);
    }

    /**
     * Find and break the furthest connected block of the same type.
     * Used by BURROWING enchantment.
     */
    private static void breakFurthestBlock(Level level, BlockPos pos, BlockState state, Player player) {
        // Simple implementation - find furthest connected block and break it
        // A full implementation would do BFS/DFS to find the furthest block
        
        BlockPos furthest = findFurthestConnectedBlock(level, pos, state, 32);
        if (furthest != null && !furthest.equals(pos)) {
            BlockState furthestState = level.getBlockState(furthest);
            
            // Drop items
            if (level instanceof ServerLevel serverLevel) {
                Block.dropResources(furthestState, level, furthest, level.getBlockEntity(furthest), player, player.getMainHandItem());
            }
            
            // Remove block
            level.destroyBlock(furthest, false, player);
        } else {
            // No further block found, break the original
            if (level instanceof ServerLevel serverLevel) {
                Block.dropResources(state, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem());
            }
            level.destroyBlock(pos, false, player);
        }
    }

    /**
     * Find the furthest connected block of the same type.
     */
    private static BlockPos findFurthestConnectedBlock(Level level, BlockPos start, BlockState targetState, int maxDistance) {
        Block targetBlock = targetState.getBlock();
        BlockPos furthest = start;
        double maxDistSq = 0;
        
        ArrayList<BlockPos> visited = new ArrayList<>();
        ArrayList<BlockPos> toVisit = new ArrayList<>();
        toVisit.add(start);
        
        while (!toVisit.isEmpty() && visited.size() < 512) {
            BlockPos current = toVisit.remove(0);
            if (visited.contains(current)) continue;
            visited.add(current);
            
            // Check distance from start
            double distSq = current.distSqr(start);
            if (distSq > maxDistance * maxDistance) continue;
            
            // Check if same block type
            BlockState currentState = level.getBlockState(current);
            if (currentState.getBlock() != targetBlock) continue;
            
            // Track furthest
            if (distSq > maxDistSq) {
                maxDistSq = distSq;
                furthest = current;
            }
            
            // Add neighbors
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!visited.contains(neighbor) && !toVisit.contains(neighbor)) {
                    toVisit.add(neighbor);
                }
            }
        }
        
        return furthest;
    }

    /**
     * Handle harvest drops for REFINING, DESTRUCTIVE, COLLECTOR, and LAMPLIGHT enchantments.
     * 
     * Note: In 1.20.1, BlockEvent.BreakEvent replaces HarvestDropsEvent for most purposes.
     * Drop modification should be done via loot modifiers or in break event.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getPlayer() == null) return;
        
        Player player = event.getPlayer();
        InteractionHand usedHand2 = player.getUsedItemHand();
        final InteractionHand hand = (usedHand2 != null) ? usedHand2 : InteractionHand.MAIN_HAND;
        
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty()) return;
        
        List<EnumInfusionEnchantment> enchantments = EnumInfusionEnchantment.getInfusionEnchantments(heldItem);
        if (enchantments.isEmpty()) return;
        
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        
        // Skip if tool isn't effective
        if (!isToolEffective(level, pos, heldItem)) return;
        
        // DESTRUCTIVE - Break 3x3 area
        if (!blockDestructiveRecursion && enchantments.contains(EnumInfusionEnchantment.DESTRUCTIVE) && !player.isShiftKeyDown()) {
            blockDestructiveRecursion = true;
            
            Direction face = lastFaceClicked.get(player.getId());
            if (face == null) {
                face = Direction.orderedByNearest(player)[0];
            }
            
            // Break 3x3 area on the clicked face
            for (int aa = -1; aa <= 1; ++aa) {
                for (int bb = -1; bb <= 1; ++bb) {
                    if (aa == 0 && bb == 0) continue;
                    
                    int xx = 0, yy = 0, zz = 0;
                    if (face.getAxis() == Direction.Axis.Y) {
                        xx = aa;
                        zz = bb;
                    } else if (face.getAxis() == Direction.Axis.Z) {
                        xx = aa;
                        yy = bb;
                    } else {
                        zz = aa;
                        yy = bb;
                    }
                    
                    BlockPos targetPos = pos.offset(xx, yy, zz);
                    BlockState targetState = level.getBlockState(targetPos);
                    
                    if (targetState.getDestroySpeed(level, targetPos) >= 0.0f && 
                            isToolEffective(level, targetPos, heldItem)) {
                        
                        // Damage tool
                        if (!player.getName().getString().equals("FakeThaumcraftBore")) {
                            heldItem.hurtAndBreak(1, player, player.getEquipmentSlotForItem(heldItem));
                        }
                        
                        // Break block with drops
                        level.destroyBlock(targetPos, true, player);
                    }
                }
            }
            
            blockDestructiveRecursion = false;
        }
        
        // LAMPLIGHT - Place light in dark areas
        if (enchantments.contains(EnumInfusionEnchantment.LAMPLIGHT) && !player.isShiftKeyDown()) {
            // Schedule for next tick to run after block is broken
            level.getServer().execute(() -> {
                if (level.isEmptyBlock(pos) && level.getRawBrightness(pos, 0) < 10) {
                    // Place glimmer effect block
                    if (ModBlocks.EFFECT_GLIMMER != null && ModBlocks.EFFECT_GLIMMER.get() != null) {
                        level.setBlock(pos, ModBlocks.EFFECT_GLIMMER.get().defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            });
        }
    }

    /**
     * Handle COLLECTOR and ESSENCE enchantments for mob drops.
     */
    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event) {
        if (event.getSource() == null || event.getSource().getEntity() == null) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        
        InteractionHand usedHand = player.getUsedItemHand();
        final InteractionHand hand = (usedHand != null) ? usedHand : InteractionHand.MAIN_HAND;
        
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty()) return;
        
        List<EnumInfusionEnchantment> enchantments = EnumInfusionEnchantment.getInfusionEnchantments(heldItem);
        if (enchantments.isEmpty()) return;
        
        Level level = event.getEntity().level();
        
        // COLLECTOR - Make drops fly to player
        if (enchantments.contains(EnumInfusionEnchantment.COLLECTOR)) {
            List<ItemEntity> newDrops = new ArrayList<>();
            
            for (ItemEntity ei : event.getDrops()) {
                ItemStack stack = ei.getItem().copy();
                EntityFollowingItem followingItem = new EntityFollowingItem(
                        level, ei.getX(), ei.getY(), ei.getZ(), stack, player, 10);
                followingItem.setDeltaMovement(ei.getDeltaMovement());
                followingItem.setDefaultPickUpDelay();
                
                // Remove old item, add following item
                ei.discard();
                newDrops.add(followingItem);
            }
            
            event.getDrops().clear();
            event.getDrops().addAll(newDrops);
        }
        
        // ESSENCE - Drop aspect crystals
        if (enchantments.contains(EnumInfusionEnchantment.ESSENCE)) {
            AspectList aspects = AspectHelper.getEntityAspects(event.getEntity());
            
            if (aspects != null && aspects.size() > 0) {
                AspectList workingAspects = aspects.copy();
                int enchantLevel = EnumInfusionEnchantment.getInfusionEnchantmentLevel(heldItem, EnumInfusionEnchantment.ESSENCE);
                
                // Chance to start dropping crystals
                int b = (level.random.nextInt(5) < enchantLevel) ? 0 : 99;
                
                Aspect[] aspectArray = workingAspects.getAspects();
                while (b < enchantLevel && aspectArray != null && aspectArray.length > 0) {
                    Aspect aspect = aspectArray[level.random.nextInt(aspectArray.length)];
                    
                    if (workingAspects.getAmount(aspect) > 0) {
                        workingAspects.remove(aspect, 1);
                        
                        ItemStack crystal = ThaumcraftApiHelper.makeCrystal(aspect);
                        if (!crystal.isEmpty()) {
                            ItemEntity crystalEntity;
                            
                            if (enchantments.contains(EnumInfusionEnchantment.COLLECTOR)) {
                                crystalEntity = new EntityFollowingItem(
                                        level,
                                        event.getEntity().getX(),
                                        event.getEntity().getY() + event.getEntity().getEyeHeight(),
                                        event.getEntity().getZ(),
                                        crystal, player, 10);
                            } else {
                                crystalEntity = new ItemEntity(
                                        level,
                                        event.getEntity().getX(),
                                        event.getEntity().getY() + event.getEntity().getEyeHeight(),
                                        event.getEntity().getZ(),
                                        crystal);
                            }
                            
                            event.getDrops().add(crystalEntity);
                        }
                        ++b;
                    }
                    
                    aspectArray = workingAspects.getAspects();
                    
                    // Random chance to stop early
                    if (level.random.nextInt(enchantLevel) == 0) {
                        break;
                    }
                    
                    b += 1 + level.random.nextInt(2);
                }
            }
        }
    }

    /**
     * Check if a tool is effective against a block.
     * In 1.20.1, we check if the tool's tier is appropriate and if it can harvest.
     */
    private static boolean isToolEffective(Level level, BlockPos pos, ItemStack tool) {
        if (tool.isEmpty()) return false;
        
        BlockState state = level.getBlockState(pos);
        
        // Check if tool is appropriate for this block
        if (tool.getItem() instanceof DiggerItem digger) {
            return digger.isCorrectToolForDrops(tool, state) || 
                   tool.getDestroySpeed(state) > 1.0f;
        }
        
        return tool.getDestroySpeed(state) > 1.0f;
    }
}




