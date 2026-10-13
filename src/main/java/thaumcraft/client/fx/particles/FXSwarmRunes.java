package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Swarm runes particle - magical rune particles that swarm around a target entity.
 * Used for magical effects that track and orbit around entities.
 */
@OnlyIn(Dist.CLIENT)
public class FXSwarmRunes extends ThaumcraftParticle {
    
    private Entity target;
    private float turnSpeed;
    private float speed;
    private int deathTimer;
    private float rotationPitch;
    private float rotationYaw;
    public int particle;
    
    public FXSwarmRunes(ClientLevel level, double x, double y, double z, 
                        Entity target, float r, float g, float b) {
        super(level, x, y, z);
        
        this.turnSpeed = 10.0f;
        this.speed = 0.2f;
        this.deathTimer = 0;
        this.particle = 0;
        
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.startR = r;
        this.startG = g;
        this.startB = b;
        this.endR = r;
        this.endG = g;
        this.endB = b;
        
        this.quadSize = this.random.nextFloat() * 0.5f + 1.0f;
        this.target = target;
        
        float f3 = 0.2f;
        this.xd = (this.random.nextFloat() - this.random.nextFloat()) * f3;
        this.yd = (this.random.nextFloat() - this.random.nextFloat()) * f3;
        this.zd = (this.random.nextFloat() - this.random.nextFloat()) * f3;
        
        this.gravity = 0.1f;
        this.lifetime = 250;
        this.noClip = true;
    }
    
    public FXSwarmRunes(ClientLevel level, double x, double y, double z, 
                        Entity target, float r, float g, float b, 
                        float sp, float ts, float pg) {
        this(level, x, y, z, target, r, g, b);
        this.speed = sp;
        this.turnSpeed = ts;
        this.gravity = pg;
        this.particle = this.random.nextInt(16);
    }
    
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        this.age++;
        
        boolean targetDead = target == null || !target.isAlive() || 
                             (target instanceof LivingEntity living && living.deathTime > 0);
        
        if (this.age > 200 || targetDead) {
            this.deathTimer++;
            this.xd *= 0.9;
            this.zd *= 0.9;
            this.yd -= this.gravity / 2.0f;
            
            if (this.deathTimer > 50) {
                this.remove();
                return;
            }
        } else {
            this.yd += this.gravity;
        }
        
        // Move
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        
        // Friction
        this.xd *= 0.985;
        this.yd *= 0.985;
        this.zd *= 0.985;
        
        // Track target
        if (this.age < 200 && target != null && target.isAlive() && 
            !(target instanceof LivingEntity living && living.deathTime > 0)) {
            
            boolean hurt = target instanceof LivingEntity living && living.hurtTime > 0;
            
            Vec3 v1 = new Vec3(this.x, this.y, this.z);
            double distSq = v1.distanceToSqr(target.getX(), target.getY(), target.getZ());
            double targetWidth = target.getBbWidth();
            
            if (distSq > targetWidth * targetWidth && !hurt) {
                faceEntity(target, turnSpeed / 2.0f + this.random.nextInt((int)(turnSpeed / 2.0f)), 
                          turnSpeed / 2.0f + this.random.nextInt((int)(turnSpeed / 2.0f)));
            } else {
                if (hurt && distSq < targetWidth * targetWidth) {
                    this.age += 100;
                }
                faceEntity(target, -(turnSpeed / 2.0f + this.random.nextInt((int)(turnSpeed / 2.0f))), 
                          -(turnSpeed / 2.0f + this.random.nextInt((int)(turnSpeed / 2.0f))));
            }
            
            this.xd = -Mth.sin(rotationYaw / 180.0f * (float)Math.PI) * Mth.cos(rotationPitch / 180.0f * (float)Math.PI);
            this.zd = Mth.cos(rotationYaw / 180.0f * (float)Math.PI) * Mth.cos(rotationPitch / 180.0f * (float)Math.PI);
            this.yd = -Mth.sin(rotationPitch / 180.0f * (float)Math.PI);
            setHeading(this.xd, this.yd, this.zd, speed, 15.0f);
        }
    }
    
    private void faceEntity(Entity target, float yawDelta, float pitchDelta) {
        double dx = target.getX() - this.x;
        double dz = target.getZ() - this.z;
        double dy = (target.getBoundingBox().minY + target.getBoundingBox().maxY) / 2.0 - this.y;
        double dist = Mth.sqrt((float)(dx * dx + dz * dz));
        float targetYaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float)(-(Math.atan2(dy, dist) * 180.0 / Math.PI));
        this.rotationPitch = updateRotation(this.rotationPitch, targetPitch, pitchDelta);
        this.rotationYaw = updateRotation(this.rotationYaw, targetYaw, yawDelta);
    }
    
    private float updateRotation(float current, float target, float maxDelta) {
        float delta = Mth.wrapDegrees(target - current);
        if (delta > maxDelta) delta = maxDelta;
        if (delta < -maxDelta) delta = -maxDelta;
        return current + delta;
    }
    
    private void setHeading(double vx, double vy, double vz, float speed, float spread) {
        float len = Mth.sqrt((float)(vx * vx + vy * vy + vz * vz));
        vx /= len;
        vy /= len;
        vz /= len;
        vx += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        vy += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        vz += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        this.xd = vx * speed;
        this.yd = vy * speed;
        this.zd = vz * speed;
    }
    
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float bob = Mth.sin(this.age / 3.0f) * 0.25f + 1.0f;
        float trans = (50.0f - this.deathTimer) / 50.0f * 0.66f;
        
        Vec3 cameraPos = camera.getPosition();
        float x = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());
        
        Quaternionf quaternion = camera.rotation();
        float size = 0.07f * this.quadSize * bob;
        
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
        
        // Texture coords for rune sprite
        float u0 = this.particle / 64.0f;
        float u1 = u0 + 0.015625f;
        float v0 = 0.09375f;
        float v1 = v0 + 0.015625f;
        int light = 240; // Full bright for magic particles
        
        buffer.addVertex(vertices[0].x(), vertices[0].y(), vertices[0].z())
              .setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, trans)
              .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
              .setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, trans)
              .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
              .setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, trans)
              .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
              .setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, trans)
              .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}




