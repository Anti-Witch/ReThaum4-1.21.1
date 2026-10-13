package thaumcraft.common.blocks.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.client.fx.FXDispatcher;

/**
 * Magical ambient grass block that spawns in the Magical Forest biome.
 * At night, it spawns wispy particle effects similar to enchantment particles.
 * Replaces normal grass during biome decoration.
 */
public class BlockGrassAmbient extends GrassBlock {
    
    public BlockGrassAmbient() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.GRASS)
                .strength(0.6f)
                .randomTicks()
                .sound(SoundType.GRASS));
    }
    
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        
        // Only spawn wispy particles at night
        // Get sky light level at block above
        int skyLight = level.getBrightness(LightLayer.SKY, pos.above());
        
        // Adjust based on time of day (celestial angle)
        float celestialAngle = level.getSunAngle(1.0f);
        float adjustedAngle = celestialAngle;
        float targetAngle = (celestialAngle < (float)Math.PI) ? 0.0f : (float)(Math.PI * 2);
        adjustedAngle += (targetAngle - celestialAngle) * 0.2f;
        
        int adjustedLight = Math.round(skyLight * Mth.cos(adjustedAngle));
        adjustedLight = Mth.clamp(adjustedLight, 0, 15);
        
        // Less light = more particles (night time effect)
        if (4 + adjustedLight * 2 < 1 + random.nextInt(13)) {
            // Pick a random position nearby
            int x = Mth.randomBetweenInclusive(random, -8, 8);
            int z = Mth.randomBetweenInclusive(random, -8, 8);
            BlockPos searchPos = pos.offset(x, 5, z);
            
            // Search downward for grass
            for (int q = 0; q < 10 && searchPos.getY() > 50; q++) {
                BlockState targetState = level.getBlockState(searchPos);
                if (targetState.is(Blocks.GRASS_BLOCK) || targetState.getBlock() instanceof GrassBlock) {
                    break;
                }
                searchPos = searchPos.below();
            }
            
            // Spawn wispy particles if we found grass
            BlockState foundState = level.getBlockState(searchPos);
            if (foundState.is(Blocks.GRASS_BLOCK) || foundState.getBlock() instanceof GrassBlock) {
                FXDispatcher.INSTANCE.drawWispyMotesOnBlock(searchPos.above(), 400, -0.01f);
            }
        }
    }
}




