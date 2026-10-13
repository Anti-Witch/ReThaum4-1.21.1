package thaumcraft.client.fx.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Custom digging particle - extends vanilla terrain particle for Thaumcraft blocks.
 * Used when breaking Thaumcraft blocks to show appropriate texture fragments.
 */
@OnlyIn(Dist.CLIENT)
public class FXDigging extends TerrainParticle {
    
    public FXDigging(ClientLevel level, double x, double y, double z, 
                     double vx, double vy, double vz, BlockState state) {
        super(level, x, y, z, vx, vy, vz, state);
    }
    
    public FXDigging(ClientLevel level, double x, double y, double z, 
                     double vx, double vy, double vz, BlockState state, BlockPos pos) {
        super(level, x, y, z, vx, vy, vz, state);
        // Update sprite based on position for proper tinting
        this.updateSprite(state, pos);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }
}




