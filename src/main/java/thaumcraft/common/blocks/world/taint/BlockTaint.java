package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import thaumcraft.common.world.aura.AuraHandler;
import thaumcraft.common.entities.EntityFallingTaint;
import thaumcraft.common.entities.monster.tainted.EntityTaintSwarm;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModEffects;
import thaumcraft.init.ModItems;

import java.util.Collections;
import java.util.List;

/**
 * BlockTaint - Base class for taint blocks (soil, rock, crust, geyser variants).
 * 
 * Taint blocks:
 * - Die (convert to normal blocks) when not near a taint seed
 * - Spread taint fibres when near a taint seed
 * - Apply flux taint effect to entities walking on them
 * - Taint crust can fall like sand
 * - Taint geyser spawns taint swarms and generates flux
 */
public class BlockTaint extends Block implements ITaintBlock {
    
    public enum TaintType {
        SOIL,   // Converts to dirt when dying
        ROCK,   // Converts to porous stone when dying, drops flux crystal
        CRUST,  // Converts to flux goo when dying, can fall
        GEYSER  // Converts to flux goo when dying, spawns swarms
    }
    
    private final TaintType type;
    
    public BlockTaint(TaintType type) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(10.0f, 100.0f)
                .sound(SoundType.SLIME_BLOCK) // Gore-like sound
                .randomTicks()
                .requiresCorrectToolForDrops());
        this.type = type;
    }
    
    public TaintType getTaintType() {
        return type;
    }
    
    @Override
    public void die(Level level, BlockPos pos, BlockState state) {
        switch (type) {
            case ROCK -> {
                // Convert to porous stone (or cobblestone as fallback)
                if (ModBlocks.POROUS_STONE != null) {
                    level.setBlockAndUpdate(pos, ModBlocks.POROUS_STONE.get().defaultBlockState());
                } else {
                    level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
                }
            }
            case SOIL -> level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            case CRUST, GEYSER -> {
                if (ModBlocks.FLUX_GOO != null) {
                    level.setBlockAndUpdate(pos, ModBlocks.FLUX_GOO.get().defaultBlockState());
                } else {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
    
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Die if not near a taint seed
        if (!TaintHelper.isNearTaintSeed(level, pos) && random.nextInt(10) == 0) {
            die(level, pos, state);
            return;
        }
        
        switch (type) {
            case ROCK -> {
                // Taint rock spreads fibres
                TaintHelper.spreadFibres(level, pos);
            }
            case CRUST -> {
                // Taint crust can fall
                if (tryToFall(level, pos, pos)) {
                    return;
                }
                
                // Can also fall sideways if above air
                if (level.isEmptyBlock(pos.above())) {
                    Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                    boolean doIt = true;
                    
                    for (int a = 1; a < 4; a++) {
                        if (!level.isEmptyBlock(pos.relative(dir).below(a))) {
                            doIt = false;
                            break;
                        }
                        if (level.getBlockState(pos.below(a)).getBlock() != this) {
                            doIt = false;
                            break;
                        }
                    }
                    
                    if (doIt) {
                        tryToFall(level, pos, pos.relative(dir));
                    }
                }
            }
            case GEYSER -> {
                // Geyser spawns taint swarms when players are nearby
                if (random.nextFloat() < 0.2f && 
                        level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32.0)) {
                    
                    List<EntityTaintSwarm> nearby = EntityUtils.getEntitiesInRange(
                            level, pos, 32.0, EntityTaintSwarm.class);
                    
                    if (nearby.isEmpty()) {
                        EntityTaintSwarm swarm = new EntityTaintSwarm(level);
                        swarm.moveTo(pos.getX() + 0.5, pos.getY() + 1.25, pos.getZ() + 0.5,
                                random.nextInt(360), 0.0f);
                        level.addFreshEntity(swarm);
                    }
                } else if (AuraHandler.getFlux(level, pos) < 2.0f) {
                    // Generate flux if not enough in the area
                    AuraHandler.addFlux(level, pos, 0.25f);
                }
            }
            default -> {
                // Soil just exists
            }
        }
    }
    
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && 
                !living.isInvertedHealAndHarm() && level.random.nextInt(250) == 0) {
            // Apply flux taint effect
            if (ModEffects.FLUX_TAINT != null) {
                living.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, 200, 0, false, true));
            }
        }
        super.stepOn(level, pos, state, entity);
    }
    
    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int eventId, int eventParam) {
        if (eventId == 1) {
            if (level.isClientSide) {
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                        SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS,
                        0.1f, 0.9f + level.random.nextFloat() * 0.2f, false);
            }
            return true;
        }
        return super.triggerEvent(state, level, pos, eventId, eventParam);
    }
    
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Taint rock has a chance to drop flux crystal
        if (type == TaintType.ROCK) {
            RandomSource random = builder.getLevel().random;
            int rr = random.nextInt(15);
            // TC6-removed: if (rr > 13 && ModItems.FLUX_CRYSTAL != null) {
            // TC6-removed:     return Collections.singletonList(new ItemStack(ModItems.FLUX_CRYSTAL.get()));
            // TC6-removed: }
        }
        return Collections.emptyList();
    }
    
    /**
     * Check if a block can fall into the position below.
     */
    public static boolean canFallBelow(Level level, BlockPos pos) {
        BlockState bs = level.getBlockState(pos);
        Block block = bs.getBlock();
        
        // Don't fall near wood logs (they support the taint)
        for (int xx = -1; xx <= 1; xx++) {
            for (int zz = -1; zz <= 1; zz++) {
                for (int yy = -1; yy <= 1; yy++) {
                    BlockState checkState = level.getBlockState(pos.offset(xx, yy, zz));
                    if (checkState.is(net.minecraft.tags.BlockTags.LOGS)) {
                        return false;
                    }
                }
            }
        }
        
        // Can fall into air, replaceable blocks, water, lava, fire, flux goo, or taint fibre
        if (bs.isAir()) return true;
        if (block == Blocks.FIRE) return true;
        if (bs.canBeReplaced()) return true;
        if (bs.liquid()) return true;
        
        // Can fall into taint fibre
        if (block instanceof BlockTaintFibre) return true;
        
        // Can fall into partial flux goo
        if (ModBlocks.FLUX_GOO != null && block == ModBlocks.FLUX_GOO.get()) {
            // Check fluid level - only fall into partial
            return true; // Simplified - original checked fluid level
        }
        
        return false;
    }
    
    /**
     * Attempt to make this block fall.
     */
    private boolean tryToFall(Level level, BlockPos pos, BlockPos fallPos) {
        if (!BlockTaintFibre.isOnlyAdjacentToTaint(level, pos)) {
            return false;
        }
        
        if (canFallBelow(level, fallPos.below()) && fallPos.getY() >= level.getMinBuildHeight()) {
            int range = 32;
            
            if (level.isAreaLoaded(fallPos, range)) {
                if (!level.isClientSide) {
                    EntityFallingTaint fallingTaint = new EntityFallingTaint(
                            level, 
                            fallPos.getX() + 0.5, 
                            fallPos.getY() + 0.5, 
                            fallPos.getZ() + 0.5,
                            level.getBlockState(pos),
                            pos);
                    level.addFreshEntity(fallingTaint);
                    return true;
                }
            } else {
                // Instant fall if area isn't loaded
                level.removeBlock(pos, false);
                BlockPos landPos = fallPos;
                while (canFallBelow(level, landPos.below()) && landPos.getY() > level.getMinBuildHeight()) {
                    landPos = landPos.below();
                }
                if (landPos.getY() > level.getMinBuildHeight()) {
                    level.setBlockAndUpdate(landPos, defaultBlockState());
                }
            }
        }
        return false;
    }
}




