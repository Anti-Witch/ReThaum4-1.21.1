package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Bore particle effect - block fragments that fly towards a target (like arcane bore).
 * Particles attract to target position and shrink as they get close.
 */
@OnlyIn(Dist.CLIENT)
public class FXBoreParticles extends ThaumcraftParticle {
    
    private final BlockState blockState;
    private final ItemStack itemStack;
    private Entity target;
    private double targetX;
    private double targetY;
    private double targetZ;
    
    public FXBoreParticles(ClientLevel level, double x, double y, double z, 
                           double tx, double ty, double tz, 
                           BlockState state, int side) {
        super(level, x, y, z);
        
        this.blockState = state;
        this.itemStack = null;
        
        // Set sprite from block
        try {
            this.setSprite(Minecraft.getInstance().getBlockRenderer()
                    .getBlockModelShaper().getBlockModel(state)
                    .getParticleIcon());
        } catch (Exception e) {
            // Fallback if texture fails
            this.remove();
            return;
        }
        
        this.rCol = 0.6f;
        this.gCol = 0.6f;
        this.bCol = 0.6f;
        this.quadSize = this.random.nextFloat() * 0.3f + 0.4f;
        
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        
        // Calculate lifetime based on distance
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;
        int base = (int)(Mth.sqrt((float)(dx * dx + dy * dy + dz * dz)) * 10.0f);
        if (base < 1) base = 1;
        this.lifetime = base / 2 + this.random.nextInt(base);
        
        // Small random initial motion
        float f3 = 0.01f;
        this.xd = this.random.nextGaussian() * f3;
        this.yd = this.random.nextGaussian() * f3;
        this.zd = this.random.nextGaussian() * f3;
        this.gravity = 0.01f;
    }
    
    public FXBoreParticles(ClientLevel level, double x, double y, double z,
                           double tx, double ty, double tz,
                           double sx, double sy, double sz,
                           ItemStack item) {
        super(level, x, y, z, sx, sy, sz);
        
        this.blockState = null;
        this.itemStack = item;
        
        // Set sprite from item
        this.setSprite(Minecraft.getInstance().getItemRenderer()
                .getModel(item, level, null, 0)
                .getParticleIcon());
        
        this.rCol = 0.6f;
        this.gCol = 0.6f;
        this.bCol = 0.6f;
        this.quadSize = this.random.nextFloat() * 0.3f + 0.4f;
        
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        
        // Calculate lifetime based on distance
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;
        int base = (int)(Mth.sqrt((float)(dx * dx + dy * dy + dz * dz)) * 10.0f);
        if (base < 1) base = 1;
        this.lifetime = base / 2 + this.random.nextInt(base);
        
        this.xd = sx + this.random.nextGaussian() * 0.01f;
        this.yd = sy + this.random.nextGaussian() * 0.01f;
        this.zd = sz + this.random.nextGaussian() * 0.01f;
        this.gravity = 0.01f;
        
        // Distance culling
        Entity renderEntity = Minecraft.getInstance().getCameraEntity();
        int visibleDistance = Minecraft.getInstance().options.graphicsMode().get().getId() >= 1 ? 64 : 32;
        if (renderEntity != null && renderEntity.distanceToSqr(x, y, z) > visibleDistance * visibleDistance) {
            this.lifetime = 0;
        }
    }
    
    public FXBoreParticles setTarget(Entity target) {
        this.target = target;
        return this;
    }
    
    public FXBoreParticles getObjectColor(BlockPos pos) {
        if (this.blockState != null) {
            try {
                int color = Minecraft.getInstance().getBlockColors()
                        .getColor(this.blockState, this.level, pos, 0);
                this.rCol *= (color >> 16 & 0xFF) / 255.0f;
                this.gCol *= (color >> 8 & 0xFF) / 255.0f;
                this.bCol *= (color & 0xFF) / 255.0f;
            } catch (Exception ignored) {}
        } else if (this.itemStack != null) {
            try {
                int color = Minecraft.getInstance().getItemColors()
                        .getColor(this.itemStack, 0);
                this.rCol *= (color >> 16 & 0xFF) / 255.0f;
                this.gCol *= (color >> 8 & 0xFF) / 255.0f;
                this.bCol *= (color & 0xFF) / 255.0f;
            } catch (Exception ignored) {}
        }
        return this;
    }
    
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        // Update target position if tracking entity
        if (this.target != null) {
            this.targetX = this.target.getX();
            this.targetY = this.target.getY() + this.target.getEyeHeight();
            this.targetZ = this.target.getZ();
        }
        
        // Check if reached target or expired
        if (this.age++ >= this.lifetime || 
            (Mth.floor(this.x) == Mth.floor(this.targetX) && 
             Mth.floor(this.y) == Mth.floor(this.targetY) && 
             Mth.floor(this.z) == Mth.floor(this.targetZ))) {
            this.remove();
            return;
        }
        
        // Move
        this.move(this.xd, this.yd, this.zd);
        
        // Friction
        this.xd *= 0.985;
        this.yd *= 0.95;
        this.zd *= 0.985;
        
        // Attract towards target
        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        double dist = Mth.sqrt((float)(dx * dx + dy * dy + dz * dz));
        double clamp = Math.min(0.25, dist / 15.0);
        
        // Shrink when close
        if (dist < 2.0) {
            this.quadSize *= 0.9f;
        }
        
        // Normalize and apply attraction
        dx /= dist;
        dy /= dist;
        dz /= dist;
        
        this.xd += dx * clamp;
        this.yd += dy * clamp;
        this.zd += dz * clamp;
        
        // Clamp velocity
        this.xd = Mth.clamp(this.xd, -clamp, clamp);
        this.yd = Mth.clamp(this.yd, -clamp, clamp);
        this.zd = Mth.clamp(this.zd, -clamp, clamp);
        
        // Add some randomness
        this.xd += this.random.nextGaussian() * 0.005;
        this.yd += this.random.nextGaussian() * 0.005;
        this.zd += this.random.nextGaussian() * 0.005;
    }
    
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        float x = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());
        
        Quaternionf quaternion = camera.rotation();
        float size = 0.1f * this.quadSize;
        
        Vector3f[] vertices = new Vector3f[]{
            new Vector3f(-1.0F, -1.0F, 0.0F),
            new Vector3f(-1.0F, 1.0F, 0.0F),
            new Vector3f(1.0F, 1.0F, 0.0F),
            new Vector3f(1.0F, -1.0F, 0.0F)
        };
        
        for (int i = 0; i < 4; ++i) {
            Vector3f vertex = vertices[i];
            vertex.rotate(quaternion);
            vertex.mul(size);
            vertex.add(x, y, z);
        }
        
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);
        
        buffer.addVertex(vertices[0].x(), vertices[0].y(), vertices[0].z())
              .setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
              .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
              .setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
              .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
              .setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
              .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
              .setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
              .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }
}




