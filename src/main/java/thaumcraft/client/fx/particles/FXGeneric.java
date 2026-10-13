package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * FXGeneric - The most commonly used particle type in Thaumcraft.
 * Supports sprite animation, scale/alpha keyframes, color interpolation,
 * rotation, wind effects, and custom physics.
 */
@OnlyIn(Dist.CLIENT)
public class FXGeneric extends TextureSheetParticle {

    // Animation state
    protected boolean doneFrames = false;
    protected boolean flipped = false;

    // Wind
    protected double windX = 0;
    protected double windZ = 0;

    // Render layer (0 = translucent, 1 = lit/additive)
    protected int layer = 0;

    // Color interpolation
    protected float startR, startG, startB;
    protected float endR, endG, endB;

    // Sprite animation
    protected boolean loop = false;
    protected int startParticle = 0;
    protected int numParticles = 1;
    protected int particleInc = 1;
    protected int[] finalFrames = null;

    // Scale keyframes
    protected float[] scaleKeys = new float[]{1.0f};
    protected float[] scaleFrames = new float[]{0.0f};

    // Alpha keyframes
    protected float[] alphaKeys = new float[]{1.0f};
    protected float[] alphaFrames = new float[]{0.0f};

    // Physics
    protected double slowDown = 0.98;
    protected float randomX = 0, randomY = 0, randomZ = 0;

    // Rotation
    protected float rotationSpeed = 0.0f;

    // Angle mode (for directional particles)
    protected boolean angled = false;
    protected float angleYaw = 0;
    protected float anglePitch = 0;

    // Grid size for sprite sheet (64x64 default)
    protected int gridSize = 64;

    // Sprite index tracking
    protected int spriteIndexX = 0;
    protected int spriteIndexY = 0;

    // SpriteSet for vanilla sprite provider integration
    protected SpriteSet sprites;

    public FXGeneric(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        init();
    }

    public FXGeneric(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        init();
    }

    private void init() {
        this.setSize(0.1f, 0.1f);
        this.startR = this.rCol;
        this.startG = this.gCol;
        this.startB = this.bCol;
        this.endR = this.rCol;
        this.endG = this.gCol;
        this.endB = this.bCol;
    }

    /**
     * Pre-calculate all frame values for smooth interpolation
     */
    protected void calculateFrames() {
        doneFrames = true;

        // Calculate alpha frames
        if (alphaKeys == null) {
            alphaKeys = new float[]{1.0f};
        }
        alphaFrames = new float[this.lifetime + 1];
        float inc = (alphaKeys.length - 1) / (float) this.lifetime;
        float is = 0.0f;
        for (int a = 0; a <= this.lifetime; ++a) {
            int isF = Mth.floor(is);
            float diff = (isF < alphaKeys.length - 1) ? (alphaKeys[isF + 1] - alphaKeys[isF]) : 0.0f;
            float pa = is - isF;
            alphaFrames[a] = alphaKeys[isF] + diff * pa;
            is += inc;
        }

        // Calculate scale frames
        if (scaleKeys == null) {
            scaleKeys = new float[]{1.0f};
        }
        scaleFrames = new float[this.lifetime + 1];
        inc = (scaleKeys.length - 1) / (float) this.lifetime;
        is = 0.0f;
        for (int a = 0; a <= this.lifetime; ++a) {
            int isF = Mth.floor(is);
            float diff = (isF < scaleKeys.length - 1) ? (scaleKeys[isF + 1] - scaleKeys[isF]) : 0.0f;
            float pa = is - isF;
            scaleFrames[a] = scaleKeys[isF] + diff * pa;
            is += inc;
        }
    }

    @Override
    public void tick() {
        if (!doneFrames) {
            calculateFrames();
        }

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Update rotation
        this.oRoll = this.roll;
        this.roll += this.rotationSpeed;

        // Apply gravity
        this.yd -= 0.04 * this.gravity;

        // Move particle
        this.move(this.xd, this.yd, this.zd);

        // Apply slowdown/friction
        this.xd *= this.slowDown;
        this.yd *= this.slowDown;
        this.zd *= this.slowDown;

        // Apply random movement
        if (randomX != 0 || randomY != 0 || randomZ != 0) {
            this.xd += this.random.nextGaussian() * randomX;
            this.yd += this.random.nextGaussian() * randomY;
            this.zd += this.random.nextGaussian() * randomZ;
        }

        // Apply wind
        this.xd += this.windX;
        this.zd += this.windZ;

        // Ground friction
        if (this.onGround && slowDown != 1.0) {
            this.xd *= 0.7;
            this.zd *= 0.7;
        }

        // Update alpha from keyframes
        if (alphaFrames != null && alphaFrames.length > 0) {
            this.alpha = alphaFrames[Math.min(this.age, alphaFrames.length - 1)];
        }

        // Update scale from keyframes
        if (scaleFrames != null && scaleFrames.length > 0) {
            this.quadSize = scaleFrames[Math.min(this.age, scaleFrames.length - 1)];
        }

        // Update color interpolation
        float progress = (float) this.age / (float) this.lifetime;
        this.rCol = Mth.lerp(progress, this.startR, this.endR);
        this.gCol = Mth.lerp(progress, this.startG, this.endB);
        this.bCol = Mth.lerp(progress, this.startB, this.endB);

        // Update sprite animation
        updateSpriteIndex();
    }

    /**
     * Update sprite index based on animation settings
     */
    protected void updateSpriteIndex() {
        int index;
        if (loop) {
            index = startParticle + (this.age / particleInc) % numParticles;
        } else {
            float fs = this.age / (float) this.lifetime;
            index = (int) (startParticle + Math.min(numParticles * fs, numParticles - 1));
        }

        // Handle final frames (death animation)
        if (finalFrames != null && finalFrames.length > 0 && this.age > this.lifetime - finalFrames.length) {
            int frame = this.lifetime - this.age;
            if (frame < 0) frame = 0;
            if (frame < finalFrames.length) {
                index = finalFrames[frame];
            }
        }

        setParticleTextureIndex(index);
    }

    /**
     * Set sprite index from a linear index in the grid
     */
    public void setParticleTextureIndex(int index) {
        if (index < 0) index = 0;
        this.spriteIndexX = index % gridSize;
        this.spriteIndexY = index / gridSize;
    }

    @Override
    protected float getU0() {
        return (float) spriteIndexX / (float) gridSize;
    }

    @Override
    protected float getU1() {
        return ((float) spriteIndexX + 1.0f) / (float) gridSize;
    }

    @Override
    protected float getV0() {
        return (float) spriteIndexY / (float) gridSize;
    }

    @Override
    protected float getV1() {
        return ((float) spriteIndexY + 1.0f) / (float) gridSize;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

        float size = this.getQuadSize(partialTicks);

        // Get UV coordinates
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        // Handle flipped textures
        if (flipped) {
            float temp = u0;
            u0 = u1;
            u1 = temp;
        }

        // Calculate interpolated color
        float progress = Mth.clamp((this.age + partialTicks) / this.lifetime, 0.0f, 1.0f);
        float pr = Mth.lerp(progress, startR, endR);
        float pg = Mth.lerp(progress, startG, endG);
        float pb = Mth.lerp(progress, startB, endB);

        int light = this.getLightColor(partialTicks);

        Quaternionf quaternion;
        if (this.roll == 0.0F && !angled) {
            quaternion = camera.rotation();
        } else if (angled) {
            // Custom angle mode for directional particles
            quaternion = new Quaternionf();
            quaternion.rotateY((float) Math.toRadians(-angleYaw + 90.0f));
            quaternion.rotateX((float) Math.toRadians(anglePitch + 90.0f));
            if (this.roll != 0.0f) {
                float rollAngle = Mth.lerp(partialTicks, this.oRoll, this.roll);
                quaternion.rotateZ(rollAngle);
            }
        } else {
            quaternion = new Quaternionf(camera.rotation());
            float rollAngle = Mth.lerp(partialTicks, this.oRoll, this.roll);
            quaternion.rotateZ(rollAngle);
        }

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

        buffer.addVertex(vertices[0].x(), vertices[0].y(), vertices[0].z())
                .setUv(u1, v1).setColor(pr, pg, pb, this.alpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
                .setUv(u1, v0).setColor(pr, pg, pb, this.alpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
                .setUv(u0, v0).setColor(pr, pg, pb, this.alpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
                .setUv(u0, v1).setColor(pr, pg, pb, this.alpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return layer == 0 ? ThaumcraftParticleRenderTypes.THAUMCRAFT_PARTICLE_SHEET_TRANSLUCENT : ThaumcraftParticleRenderTypes.THAUMCRAFT_PARTICLE_SHEET_LIT;
    }

    // ==================== Configuration Methods ====================
    // Note: Using void returns to avoid conflicts with parent class methods

    @Override
    public void setColor(float r, float g, float b) {
        // Handle colors > 1 as 0-255 range
        if (r > 1.0f) r /= 255.0f;
        if (g > 1.0f) g /= 255.0f;
        if (b > 1.0f) b /= 255.0f;

        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.startR = r;
        this.startG = g;
        this.startB = b;
        this.endR = r;
        this.endG = g;
        this.endB = b;
    }

    public void setColorRange(float r1, float g1, float b1, float r2, float g2, float b2) {
        // Handle colors > 1 as 0-255 range
        if (r1 > 1.0f) r1 /= 255.0f;
        if (g1 > 1.0f) g1 /= 255.0f;
        if (b1 > 1.0f) b1 /= 255.0f;
        if (r2 > 1.0f) r2 /= 255.0f;
        if (g2 > 1.0f) g2 /= 255.0f;
        if (b2 > 1.0f) b2 /= 255.0f;

        this.rCol = r1;
        this.gCol = g1;
        this.bCol = b1;
        this.startR = r1;
        this.startG = g1;
        this.startB = b1;
        this.endR = r2;
        this.endG = g2;
        this.endB = b2;
    }

    public void setAlphaF(float alpha) {
        this.alpha = alpha;
        this.alphaKeys = new float[]{alpha};
    }

    public void setAlphaKeyframes(float... alphaKeyframes) {
        this.alpha = alphaKeyframes[0];
        this.alphaKeys = alphaKeyframes;
    }

    public void setScale(float scale) {
        this.quadSize = scale;
        this.scaleKeys = new float[]{scale};
    }

    public void setScaleKeyframes(float... scaleKeyframes) {
        this.quadSize = scaleKeyframes[0];
        this.scaleKeys = scaleKeyframes;
    }

    public void setMaxAge(int maxAge) {
        this.lifetime = maxAge;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public void setParticles(int startParticle, int numParticles, int particleInc) {
        this.startParticle = startParticle;
        this.numParticles = numParticles;
        this.particleInc = particleInc;
        setParticleTextureIndex(startParticle);
    }

    public void setParticle(int particleIndex) {
        this.startParticle = particleIndex;
        this.numParticles = 1;
        this.particleInc = 1;
        setParticleTextureIndex(particleIndex);
    }

    public void setRotationSpeed(float speed) {
        this.rotationSpeed = (float) (speed * 0.017453292519943); // Convert degrees to radians
    }

    public void setRotationSpeedWithStart(float startAngle, float speed) {
        this.roll = (float) (startAngle * Math.PI * 2.0);
        this.rotationSpeed = (float) (speed * 0.017453292519943);
    }

    public void setSlowDown(double slowDown) {
        this.slowDown = slowDown;
    }

    public void setRandomMovementScale(float x, float y, float z) {
        this.randomX = x;
        this.randomY = y;
        this.randomZ = z;
    }

    public void setWindStrength(double windStrength) {
        // Simple wind calculation based on moon phase
        int m = (int) ((this.level.getDayTime() / 24000L) % 8);
        double angle = m * (40 + this.random.nextInt(10)) / 180.0f * Math.PI;
        this.windX = Math.cos(angle) * windStrength;
        this.windZ = Math.sin(angle) * windStrength;
    }

    public void setWind(double windX, double windZ) {
        this.windX = windX;
        this.windZ = windZ;
    }

    public void setFinalFrames(int... frames) {
        this.finalFrames = frames;
    }

    public void setAngles(float yaw, float pitch) {
        this.angleYaw = yaw;
        this.anglePitch = pitch;
        this.angled = true;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
    }

    public void setGridSize(int size) {
        this.gridSize = size;
    }

    public void setNoClip(boolean noClip) {
        this.hasPhysics = !noClip;
    }

    public void setSprites(SpriteSet sprites) {
        this.sprites = sprites;
    }

    public boolean isFlipped() {
        return flipped;
    }
}




