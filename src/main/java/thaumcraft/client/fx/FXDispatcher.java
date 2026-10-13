package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.fx.beams.FXArc;
import thaumcraft.client.fx.beams.FXBeamBore;
import thaumcraft.client.fx.beams.FXBeamWand;
import thaumcraft.client.fx.beams.FXBolt;
import thaumcraft.client.fx.particles.FXBlockRunes;
import thaumcraft.client.fx.particles.FXBlockWard;
import thaumcraft.client.fx.particles.FXBoreParticles;
import thaumcraft.client.fx.particles.FXBoreSparkle;
import thaumcraft.client.fx.particles.FXCrucibleBubble;
import thaumcraft.client.fx.particles.FXEssentiaTrail;
import thaumcraft.client.fx.particles.FXFireMote;
import thaumcraft.client.fx.particles.FXTaintParticle;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.client.fx.particles.FXSlimyBubble;
import thaumcraft.client.fx.particles.FXSmokeSpiral;
import thaumcraft.client.fx.particles.FXSwarm;
import thaumcraft.client.fx.particles.FXVent;
import thaumcraft.client.fx.particles.FXVisSparkle;
import thaumcraft.client.fx.particles.FXWisp;
import thaumcraft.init.ModSounds;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * FXDispatcher - Central particle effect dispatcher for Thaumcraft.
 * 
 * This is a stub implementation that provides the interface used by
 * the rest of the mod without crashing. Real particle effects will
 * be incrementally added.
 * 
 * For now, most methods use vanilla particles as placeholders.
 */
@OnlyIn(Dist.CLIENT)
public class FXDispatcher {
    
    public static FXDispatcher INSTANCE = new FXDispatcher();
    
    private final Random rand = new Random();
    
    public Level getWorld() {
        return Minecraft.getInstance().level;
    }

    /**
     * Get the client level, required for spawning custom particles
     */
    private ClientLevel getClientLevel() {
        return Minecraft.getInstance().level;
    }

    /**
     * Add a custom particle to the particle engine
     */
    private void addParticle(net.minecraft.client.particle.Particle particle) {
        if (particle != null) {
            Minecraft.getInstance().particleEngine.add(particle);
        }
    }

    // ==================== ParticleEngine parity (TC4) ====================

    /**
     * TC4 {@code ParticleEngine.particlesDelayed}: particles that are spawned a
     * few ticks after they were requested. Several focus effects (flux, heal,
     * curse, ...) stagger their particles this way so a single impact does not
     * spawn all sprites on the same tick.
     */
    private static final List<DelayedParticle> DELAYED_PARTICLES = new ArrayList<>();

    private static final class DelayedParticle {
        final net.minecraft.client.particle.Particle particle;
        int delay;

        DelayedParticle(net.minecraft.client.particle.Particle particle, int delay) {
            this.particle = particle;
            this.delay = delay;
        }
    }

    /**
     * TC4 {@code ParticleEngine.addEffect} - immediately spawn a custom particle.
     */
    public void addEffect(net.minecraft.client.particle.Particle particle) {
        addParticle(particle);
    }

    /**
     * TC4 {@code ParticleEngine.addEffectWithDelay} - spawn a custom particle
     * after the given number of client ticks.
     */
    public void addEffectWithDelay(net.minecraft.client.particle.Particle particle, int delay) {
        if (particle == null) return;
        if (delay <= 0) {
            addParticle(particle);
            return;
        }
        synchronized (DELAYED_PARTICLES) {
            DELAYED_PARTICLES.add(new DelayedParticle(particle, delay));
        }
    }

    /**
     * TC4 {@code ParticleEngine.updateParticles}: advance the delayed particle
     * queue. Called once per client tick from {@code ClientEvents}.
     */
    public static void tickDelayedParticles() {
        if (DELAYED_PARTICLES.isEmpty()) return;
        synchronized (DELAYED_PARTICLES) {
            Iterator<DelayedParticle> it = DELAYED_PARTICLES.iterator();
            while (it.hasNext()) {
                DelayedParticle dp = it.next();
                if (--dp.delay <= 0) {
                    Minecraft.getInstance().particleEngine.add(dp.particle);
                    it.remove();
                }
            }
        }
    }
    
    // ==================== Fire/Alumentum Effects ====================
    
    public void drawFireMote(float x, float y, float z, float vx, float vy, float vz, 
            float r, float g, float b, float alpha, float scale) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXFireMote particle = new FXFireMote(level, x, y, z, vx, vy, vz, r, g, b, scale);
            particle.setAlpha(alpha);
            addParticle(particle);
        }
    }
    
    public void drawAlumentum(float x, float y, float z, float vx, float vy, float vz, 
            float r, float g, float b, float alpha, float scale) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            // Alumentum uses fire motes but with layer 1 (additive blending)
            FXFireMote particle = new FXFireMote(level, x, y, z, vx, vy, vz, r, g, b, scale, 1);
            particle.setAlpha(alpha);
            addParticle(particle);
        }
    }
    
    // ==================== Taint Effects ====================
    
    /**
     * Draw taint corruption particles.
     */
    public void drawTaintParticles(float x, float y, float z, float vx, float vy, float vz, float scale) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXTaintParticle particle = new FXTaintParticle(level, x, y, z, vx, vy, vz, scale);
            addParticle(particle);
        }
    }
    
    // ==================== Lightning/Spark Effects ====================
    
    public void drawLightningFlash(double x, double y, double z, float r, float g, float b, float alpha, float scale) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.FLASH, x, y, z, 0, 0, 0);
        }
    }
    
    public void spark(double x, double y, double z, float size, float r, float g, float b, float a) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0);
        }
    }
    
    public void sparkle(float x, float y, float z, float r, float g, float b) {
        Level level = getWorld();
        if (level != null && rand.nextInt(6) < 4) {
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.01, 0);
        }
    }
    
    /**
     * Creates a slimy bubble particle effect (for liquid death / flux goo).
     * Animated bubble that rises, inflates, and pops.
     */
    public void slimyBubbleFX(float x, float y, float z, float scale, float r, float g, float b) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXSlimyBubble bubble = new FXSlimyBubble(level, x, y, z, scale, r, g, b);
            addParticle(bubble);
        }
    }
    
    // ==================== Generic Particle Drawing ====================
    
    public void drawGenericParticles(double x, double y, double z, double mx, double my, double mz, 
            float r, float g, float b, float alpha, boolean loop, int start, int num, int inc, 
            int age, int delay, float scale, float rot, int layer) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXGeneric particle = new FXGeneric(level, x, y, z, mx, my, mz);
            particle.setColor(r, g, b);
            particle.setAlphaF(alpha);
            particle.setLoop(loop);
            particle.setParticles(start, num, inc);
            particle.setMaxAge(age);
            particle.setScale(scale);
            particle.setRotationSpeed(rot);
            particle.setLayer(layer);
            addParticle(particle);
        }
    }
    
    public void drawGenericParticles16(double x, double y, double z, double x2, double y2, double z2, 
            float r, float g, float b, float alpha, boolean loop, int start, int num, int inc, 
            int age, int delay, float scale, float rot, int layer) {
        drawGenericParticles(x, y, z, x2, y2, z2, r, g, b, alpha, loop, start, num, inc, age, delay, scale, rot, layer);
    }
    
    /**
     * TC4 {@code FXDispatcher.drawGenericParticles(..., GenPart)}: spawn a
     * fully configured {@link FXGeneric} particle from a {@link GenPart} preset.
     */
    public void drawGenericParticles(double x, double y, double z, double mx, double my, double mz, GenPart part) {
        if (part == null) return;
        ClientLevel level = getClientLevel();
        if (level == null) return;

        FXGeneric particle = new FXGeneric(level, x, y, z, mx, my, mz);
        particle.setMaxAge(part.age);
        particle.setColorRange(part.redStart, part.greenStart, part.blueStart,
                part.redEnd, part.greenEnd, part.blueEnd);
        particle.setAlphaKeyframes(part.alpha != null && part.alpha.length > 0
                ? part.alpha : new float[] { 1.0f });
        particle.setScaleKeyframes(part.scale != null && part.scale.length > 0
                ? part.scale : new float[] { 1.0f });
        particle.setLoop(part.loop);
        particle.setParticles(part.partStart, part.partNum, part.partInc);
        particle.setLayer(part.layer);
        if (part.rotstart != 0.0f || part.rot != 0.0f) {
            particle.setRotationSpeedWithStart(part.rotstart, part.rot);
        }
        particle.setSlowDown(part.slowDown);
        particle.setGravity(part.grav);
        particle.setGridSize(part.grid);
        addEffectWithDelay(particle, part.delay);
    }
    
    // ==================== Crucible Effects ====================
    
    /**
     * Create a colored bubble in the crucible.
     */
    public void crucibleBubble(float x, float y, float z, float cr, float cg, float cb) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXCrucibleBubble bubble = new FXCrucibleBubble(level, x, y, z, cr, cg, cb);
            addParticle(bubble);
        }
    }
    
    /**
     * Create boiling bubbles in the crucible.
     */
    public void crucibleBoil(BlockPos pos, Object tile, int aspectColor) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            // Extract color from aspect
            float r = ((aspectColor >> 16) & 0xFF) / 255.0f;
            float g = ((aspectColor >> 8) & 0xFF) / 255.0f;
            float b = (aspectColor & 0xFF) / 255.0f;
            
            for (int a = 0; a < 2; a++) {
                double x = pos.getX() + 0.2f + rand.nextFloat() * 0.6f;
                double y = pos.getY() + 0.65f;
                double z = pos.getZ() + 0.2f + rand.nextFloat() * 0.6f;
                
                FXCrucibleBubble bubble = new FXCrucibleBubble(level, x, y, z, r, g, b);
                addParticle(bubble);
            }
        }
    }
    
    /**
     * Create frothy splash particles on crucible surface.
     */
    public void crucibleFroth(float x, float y, float z) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            // Small white splash particle
            FXGeneric splash = new FXGeneric(level, x, y, z, 
                    (rand.nextFloat() - 0.5f) * 0.02, 0.02, (rand.nextFloat() - 0.5f) * 0.02);
            splash.setColor(0.9f, 0.9f, 1.0f);
            splash.setAlphaF(0.6f);
            splash.setMaxAge(6 + rand.nextInt(4));
            splash.setScale(0.2f + rand.nextFloat() * 0.1f);
            splash.setParticles(160, 1, 1);
            addParticle(splash);
        }
    }
    
    /**
     * Create dripping particles from crucible overflow.
     */
    public void crucibleFrothDown(float x, float y, float z) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            // Dripping particle that falls
            FXGeneric drip = new FXGeneric(level, x, y, z, 0, -0.01, 0);
            drip.setColor(0.6f, 0.7f, 0.9f);
            drip.setAlphaF(0.5f);
            drip.setMaxAge(15 + rand.nextInt(10));
            drip.setScale(0.15f);
            drip.setParticles(160, 1, 1);
            drip.setGravity(0.05f);
            addParticle(drip);
        }
    }
    
    // ==================== Bamf/Teleport Effects ====================
    
    public void drawBamf(BlockPos p, boolean sound, boolean flair, Direction side) {
        drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, sound, flair, side);
    }
    
    public void drawBamf(BlockPos p, float r, float g, float b, boolean sound, boolean flair, Direction side) {
        drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, r, g, b, sound, flair, side);
    }
    
    public void drawBamf(BlockPos p, int color, boolean sound, boolean flair, Direction side) {
        Color c = new Color(color);
        drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 
                c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, sound, flair, side);
    }
    
    public void drawBamf(double x, double y, double z, int color, boolean sound, boolean flair, Direction side) {
        Color c = new Color(color);
        drawBamf(x, y, z, c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, sound, flair, side);
    }
    
    public void drawBamf(double x, double y, double z, boolean sound, boolean flair, Direction side) {
        drawBamf(x, y, z, 0.5f, 0.1f, 0.6f, sound, flair, side);
    }
    
    /**
     * Create a "bamf" teleportation poof effect with custom colors.
     */
    public void drawBamf(double x, double y, double z, float r, float g, float b, boolean sound, boolean flair, Direction side) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        if (sound) {
            level.playLocalSound(x, y, z, ModSounds.POOF.get(), SoundSource.BLOCKS, 
                    0.4f, 1.0f + (float) rand.nextGaussian() * 0.05f, false);
        }
        
        // Spawn colored smoke particles
        int count = 8 + rand.nextInt(4);
        for (int a = 0; a < count; a++) {
            double vx = rand.nextGaussian() * 0.1;
            double vy = rand.nextGaussian() * 0.1;
            double vz = rand.nextGaussian() * 0.1;
            if (side != null) {
                vx += side.getStepX() * 0.1;
                vy += side.getStepY() * 0.1;
                vz += side.getStepZ() * 0.1;
            }
            
            // Create colored poof particle
            FXGeneric poof = new FXGeneric(level, x + vx * 0.5, y + vy * 0.5, z + vz * 0.5, vx, vy, vz);
            poof.setColor(r, g, b);
            poof.setAlphaF(0.7f);
            poof.setMaxAge(12 + rand.nextInt(8));
            poof.setScale(0.4f + rand.nextFloat() * 0.3f);
            poof.setParticles(72, 4, 1);  // Wispy sprites
            poof.setLoop(true);
            poof.setGravity(-0.02f);  // Slight upward float
            addParticle(poof);
        }
        
        if (flair) {
            // Add sparkle flair
            for (int a = 0; a < 5; a++) {
                FXGeneric sparkle = new FXGeneric(level, x, y, z,
                        rand.nextGaussian() * 0.08, rand.nextGaussian() * 0.08, rand.nextGaussian() * 0.08);
                sparkle.setColor(1.0f, 1.0f, 1.0f);
                sparkle.setAlphaF(0.9f);
                sparkle.setMaxAge(8 + rand.nextInt(5));
                sparkle.setScale(0.15f + rand.nextFloat() * 0.1f);
                sparkle.setParticles(0, 4, 1);  // Bright sparkle
                sparkle.setLayer(1);  // Additive blending
                addParticle(sparkle);
            }
        }
    }
    
    // ==================== Wispy Motes ====================
    
    /**
     * Create wispy mote particles rising from a block.
     */
    public void drawWispyMotesOnBlock(BlockPos pp, int age, float grav) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            double x = pp.getX() + rand.nextFloat();
            double y = pp.getY();
            double z = pp.getZ() + rand.nextFloat();
            
            FXGeneric mote = new FXGeneric(level, x, y, z, 0, 0.03 + rand.nextFloat() * 0.02, 0);
            mote.setColor(0.8f, 0.6f, 1.0f);  // Light purple
            mote.setAlphaF(0.6f);
            mote.setMaxAge(age > 0 ? age : 15 + rand.nextInt(10));
            mote.setScale(0.15f + rand.nextFloat() * 0.1f);
            mote.setParticles(64, 4, 1);  // Wispy sprites
            mote.setLoop(true);
            mote.setGravity(grav);
            mote.setLayer(1);
            addParticle(mote);
        }
    }
    
    /**
     * Create a wispy mote particle with velocity.
     */
    public void drawWispyMotes(double x, double y, double z, double vx, double vy, double vz, int age, float grav) {
        drawWispyMotes(x, y, z, vx, vy, vz, age, 0.8f, 0.6f, 1.0f, grav);
    }
    
    /**
     * Create a colored wispy mote particle.
     */
    public void drawWispyMotes(double x, double y, double z, double vx, double vy, double vz, 
            int age, float r, float g, float b, float grav) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXGeneric mote = new FXGeneric(level, x, y, z, vx, vy, vz);
            mote.setColor(r, g, b);
            mote.setAlphaF(0.6f);
            mote.setMaxAge(age > 0 ? age : 15 + rand.nextInt(10));
            mote.setScale(0.15f + rand.nextFloat() * 0.1f);
            mote.setParticles(64, 4, 1);
            mote.setLoop(true);
            mote.setGravity(grav);
            mote.setLayer(1);
            addParticle(mote);
        }
    }
    
    // ==================== Scan Effects ====================
    
    /**
     * Highlight a block with scan sparkles.
     */
    public void scanHighlight(BlockPos p) {
        Level level = getWorld();
        if (level == null) return;
        
        AABB bb = level.getBlockState(p).getShape(level, p).bounds().move(p);
        scanHighlight(bb);
    }
    
    /**
     * Highlight an entity with scan sparkles.
     */
    public void scanHighlight(Entity e) {
        scanHighlight(e.getBoundingBox());
    }
    
    /**
     * Highlight a bounding box with scan sparkles.
     */
    public void scanHighlight(AABB bb) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        int num = Mth.ceil(bb.getSize() * 3);
        double cx = (bb.minX + bb.maxX) / 2;
        double cy = (bb.minY + bb.maxY) / 2;
        double cz = (bb.minZ + bb.maxZ) / 2;
        
        for (int a = 0; a < num; a++) {
            double x = cx + rand.nextGaussian() * (bb.maxX - bb.minX) * 0.35;
            double y = cy + rand.nextGaussian() * (bb.maxY - bb.minY) * 0.35;
            double z = cz + rand.nextGaussian() * (bb.maxZ - bb.minZ) * 0.35;
            
            FXGeneric sparkle = new FXGeneric(level, x, y, z, 0, 0.01, 0);
            sparkle.setColor(0.9f, 0.95f, 1.0f);  // Bright white-blue
            sparkle.setAlphaF(0.8f);
            sparkle.setMaxAge(10 + rand.nextInt(8));
            sparkle.setScale(0.1f + rand.nextFloat() * 0.08f);
            sparkle.setParticles(0, 4, 1);  // Sparkle sprite
            sparkle.setLayer(1);  // Additive
            sparkle.setGravity(-0.01f);  // Slight upward float
            addParticle(sparkle);
        }
    }
    
    /**
     * Create sparkles flowing from block toward a point.
     */
    public void drawBlockSparkles(BlockPos p, Vec3 start) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        // Create sparkles that flow toward the start position
        for (int i = 0; i < 3; i++) {
            double x = p.getX() + rand.nextFloat();
            double y = p.getY() + rand.nextFloat();
            double z = p.getZ() + rand.nextFloat();
            
            double vx = (start.x - x) * 0.05;
            double vy = (start.y - y) * 0.05;
            double vz = (start.z - z) * 0.05;
            
            FXGeneric sparkle = new FXGeneric(level, x, y, z, vx, vy, vz);
            sparkle.setColor(0.9f, 0.95f, 1.0f);
            sparkle.setAlphaF(0.7f);
            sparkle.setMaxAge(12 + rand.nextInt(6));
            sparkle.setScale(0.12f + rand.nextFloat() * 0.08f);
            sparkle.setParticles(0, 4, 1);
            sparkle.setLayer(1);
            addParticle(sparkle);
        }
    }
    
    /**
     * Create a simple sparkle particle.
     */
    public void drawSimpleSparkle(Random rand, double x, double y, double z, double x2, double y2, double z2, 
            float scale, float r, float g, float b, int delay, float decay, float grav, int baseAge) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXGeneric sparkle = new FXGeneric(level, x, y, z, x2, y2, z2);
            sparkle.setColor(r, g, b);
            sparkle.setAlphaF(0.8f);
            sparkle.setMaxAge(baseAge > 0 ? baseAge : 15);
            sparkle.setScale(scale > 0 ? scale : 0.15f);
            sparkle.setParticles(0, 4, 1);
            sparkle.setLayer(1);
            sparkle.setGravity(grav);
            addParticle(sparkle);
        }
    }
    
    /**
     * Create a line of sparkle particles.
     */
    public void drawLineSparkle(Random rand, double x, double y, double z, double x2, double y2, double z2, 
            float scale, float r, float g, float b, int delay, float decay, float grav, int baseAge) {
        drawSimpleSparkle(rand, x, y, z, x2, y2, z2, scale, r, g, b, delay, decay, grav, baseAge);
    }
    
    // ==================== Block Mist/Fog ====================
    
    public void drawBlockMistParticles(BlockPos p, int c) {
        Level level = getWorld();
        if (level == null) return;
        
        for (int a = 0; a < 4; a++) {
            double x = p.getX() + rand.nextFloat();
            double y = p.getY() + rand.nextFloat();
            double z = p.getZ() + rand.nextFloat();
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0.02, 0);
        }
    }
    
    public void drawBlockMistParticlesFlat(BlockPos p, int c) {
        Level level = getWorld();
        if (level == null) return;
        
        for (int a = 0; a < 3; a++) {
            double x = p.getX() + rand.nextFloat();
            double y = p.getY() + 0.1;
            double z = p.getZ() + rand.nextFloat();
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0.01, 0);
        }
    }
    
    public void drawFocusCloudParticle(double x, double y, double z, double mx, double my, double mz, int c) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.CLOUD, x, y, z, mx, my, mz);
        }
    }
    
    // ==================== Vis/Aura Effects ====================
    
    public void visSparkle(int x, int y, int z, int x2, int y2, int z2, int color) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            Color c = new Color(color);
            FXVisSparkle particle = new FXVisSparkle(level, 
                    x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(),
                    x2 + 0.5, y2 + 0.5, z2 + 0.5);
            particle.setColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
            addParticle(particle);
        }
    }
    
    public void drawLevitatorParticles(double x, double y, double z, double x2, double y2, double z2) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, x2, y2, z2);
        }
    }
    
    public void drawStabilizerParticles(double x, double y, double z, double x2, double y2, double z2, int life) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.PORTAL, x, y, z, x2, y2, z2);
        }
    }
    
    public void drawGolemFlyParticles(double x, double y, double z, double x2, double y2, double z2) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, x2, y2, z2);
        }
    }
    
    public void drawPollutionParticles(BlockPos p) {
        Level level = getWorld();
        if (level == null) return;
        
        double x = p.getX() + 0.2 + rand.nextFloat() * 0.6;
        double y = p.getY() + 0.2 + rand.nextFloat() * 0.6;
        double z = p.getZ() + 0.2 + rand.nextFloat() * 0.6;
        level.addParticle(ParticleTypes.WITCH, x, y, z, 0, 0.02, 0);
    }
    
    // ==================== Essentia Effects ====================
    
    /**
     * Create an essentia stream flowing from source to target.
     * Uses FXEssentiaTrail for a flowing chain of particles.
     */
    public void essentiaTrailFx(BlockPos p1, BlockPos p2, int count, int color, float scale, int ext) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        // Spawn essentia trail particles
        for (int i = 0; i < count; i++) {
            double startX = p1.getX() + 0.5 + rand.nextGaussian() * 0.05;
            double startY = p1.getY() + 0.5 + rand.nextGaussian() * 0.05;
            double startZ = p1.getZ() + 0.5 + rand.nextGaussian() * 0.05;
            
            FXEssentiaTrail trail = new FXEssentiaTrail(level, startX, startY, startZ,
                    p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5,
                    color, scale, ext);
            addParticle(trail);
        }
    }
    
    /**
     * Create a small essentia drip/drop particle.
     */
    public void essentiaDropFx(double x, double y, double z, float r, float g, float b, float alpha) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            // Create a small colored drip particle
            FXGeneric drop = new FXGeneric(level, x, y, z, 0, -0.02, 0);
            drop.setColor(r, g, b);
            drop.setAlphaF(alpha);
            drop.setMaxAge(8 + rand.nextInt(4));
            drop.setScale(0.3f + rand.nextFloat() * 0.2f);
            drop.setParticles(144, 1, 1);  // Essentia blob sprite
            drop.setGravity(0.1f);
            addParticle(drop);
        }
    }
    
    public void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXVent vent = new FXVent(level, x, y, z, x2, y2, z2, color);
            addParticle(vent);
        }
    }
    
    public void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXVent vent = new FXVent(level, x, y, z, x2, y2, z2, color);
            vent.setScale(scale);
            addParticle(vent);
        }
    }
    
    public void drawVentParticles2(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
        drawVentParticles(x, y, z, x2, y2, z2, color, scale);
    }
    
    public void jarSplashFx(double x, double y, double z) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.SPLASH, x, y, z, 0, 0.1, 0);
        }
    }
    
    public void waterTrailFx(BlockPos p1, BlockPos p2, int count, int color, float scale) {
        essentiaTrailFx(p1, p2, count, color, scale, 0);
    }
    
    // ==================== Infusion Effects ====================
    
    /**
     * TC4/TC6 drawInfusionParticles1 - a particle carrying the texture of the
     * infused item that flies from a pedestal into the matrix.
     */
    public void drawInfusionParticles1(double x, double y, double z, BlockPos pos, ItemStack stack) {
        ClientLevel level = getClientLevel();
        if (level == null || stack == null || stack.isEmpty()) return;
        try {
            FXBoreParticles fb = new FXBoreParticles(level, x, y, z,
                    pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5,
                    (float) rand.nextGaussian() * 0.03f, (float) rand.nextGaussian() * 0.03f, (float) rand.nextGaussian() * 0.03f,
                    stack).getObjectColor(pos);
            fb.setTCAlpha(0.3f);
            addParticle(fb);
        } catch (Exception ignored) {
            // Malformed item model - skip the effect instead of crashing
        }
    }
    
    /**
     * TC4/TC6 drawInfusionParticles2 - same as above but for a block placed on a
     * pedestal (uses the block's particle icon).
     */
    public void drawInfusionParticles2(double x, double y, double z, BlockPos pos, BlockState state, int md) {
        ClientLevel level = getClientLevel();
        if (level == null || state == null) return;
        try {
            FXBoreParticles fb = new FXBoreParticles(level, x, y, z,
                    pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5,
                    state, md).getObjectColor(pos);
            fb.setTCAlpha(0.3f);
            addParticle(fb);
        } catch (Exception ignored) {
            // Malformed block model - skip the effect instead of crashing
        }
    }
    
    /**
     * TC4/TC6 drawInfusionParticles3 - purple magical sparkle that flies from a
     * pedestal into the matrix.
     */
    public void drawInfusionParticles3(double x, double y, double z, int x2, int y2, int z2) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        FXBoreSparkle fb = new FXBoreSparkle(level, x, y, z, x2 + 0.5, y2 - 0.5, z2 + 0.5);
        fb.setColor(0.4f + rand.nextFloat() * 0.2f, 0.2f, 0.6f + rand.nextFloat() * 0.3f);
        addParticle(fb);
    }
    
    /**
     * TC4/TC6 drawInfusionParticles4 - green sparkle used when experience (or
     * essentia) is drained from a nearby player.
     */
    public void drawInfusionParticles4(double x, double y, double z, int x2, int y2, int z2) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        FXBoreSparkle fb = new FXBoreSparkle(level, x, y, z, x2 + 0.5, y2 - 0.5, z2 + 0.5);
        fb.setColor(0.2f, 0.6f + rand.nextFloat() * 0.3f, 0.3f);
        addParticle(fb);
    }
    
    // ==================== Arc/Lightning ====================
    
    public void arcLightning(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float h) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXArc arc = new FXArc(level, x, y, z, tx, ty, tz, r, g, b, h);
            addParticle(arc);
        }
    }
    
    public void arcBolt(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float width) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXBolt bolt = new FXBolt(level, x, y, z, tx, ty, tz, r, g, b, width);
            addParticle(bolt);
        }
    }
    
    // ==================== Beam Effects ====================
    
    /**
     * Create or update a continuous beam from a living entity to a target point.
     * Used for wand/gauntlet casting effects.
     * 
     * @param p The source entity (caster)
     * @param tx Target X coordinate
     * @param ty Target Y coordinate
     * @param tz Target Z coordinate
     * @param type Beam texture type (0-3)
     * @param color Beam color as packed RGB int
     * @param reverse Reverse UV scroll direction
     * @param endmod End width modifier
     * @param input Existing beam particle to update, or null to create new
     * @param impact Impact flash counter (triggers flash effect when > 0)
     * @return The beam particle instance for subsequent calls
     */
    public Object beamCont(LivingEntity p, double tx, double ty, double tz, int type, int color, boolean reverse, float endmod, Object input, int impact) {
        ClientLevel level = getClientLevel();
        if (level == null) return null;
        
        FXBeamWand beam = null;
        Color c = new Color(color);
        
        // Check if we have an existing beam to update
        if (input instanceof FXBeamWand existingBeam) {
            beam = existingBeam;
        }
        
        // Create new beam if needed
        if (beam == null || !beam.isAlive()) {
            beam = new FXBeamWand(level, p, tx, ty, tz, 
                    c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 8);
            beam.setType(type);
            beam.setEndMod(endmod);
            beam.setReverse(reverse);
            addParticle(beam);
        } else {
            // Update existing beam
            beam.updateBeam(tx, ty, tz);
            beam.setEndMod(endmod);
            beam.impact = impact;
        }
        
        return beam;
    }
    
    /**
     * Create or update a point-to-point beam (not attached to entity).
     * Used for arcane bore mining beams, infusion effects, etc.
     * 
     * @param px Source X coordinate
     * @param py Source Y coordinate
     * @param pz Source Z coordinate
     * @param tx Target X coordinate
     * @param ty Target Y coordinate
     * @param tz Target Z coordinate
     * @param type Beam texture type (0-3)
     * @param color Beam color as packed RGB int
     * @param reverse Reverse UV scroll direction
     * @param endmod End width modifier
     * @param input Existing beam particle to update, or null to create new
     * @param impact Impact flash counter (triggers flash effect when > 0)
     * @return The beam particle instance for subsequent calls
     */
    public Object beamBore(double px, double py, double pz, double tx, double ty, double tz, int type, int color, boolean reverse, float endmod, Object input, int impact) {
        ClientLevel level = getClientLevel();
        if (level == null) return null;
        
        FXBeamBore beam = null;
        Color c = new Color(color);
        
        // Check if we have an existing beam to update
        if (input instanceof FXBeamBore existingBeam) {
            beam = existingBeam;
        }
        
        // Create new beam if needed
        if (beam == null || !beam.isAlive()) {
            beam = new FXBeamBore(level, px, py, pz, tx, ty, tz, 
                    c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 8);
            beam.setType(type);
            beam.setEndMod(endmod);
            beam.setReverse(reverse);
            addParticle(beam);
        } else {
            // Update existing beam
            beam.updateBeam(px, py, pz, tx, ty, tz);
            beam.setEndMod(endmod);
            beam.impact = impact;
        }
        
        return beam;
    }
    
    // ==================== Misc Effects ====================
    
    public void burst(double sx, double sy, double sz, float size) {
        Level level = getWorld();
        if (level == null) return;
        
        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.POOF, sx, sy, sz, 
                    rand.nextGaussian() * 0.1, rand.nextGaussian() * 0.1, rand.nextGaussian() * 0.1);
        }
    }
    
    public void excavateFX(BlockPos pos, LivingEntity p, int progress) {
        // Block breaking animation is handled by vanilla
    }
    
    public void blockRunes(double x, double y, double z, float r, float g, float b, int dur, float grav) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXBlockRunes runes = new FXBlockRunes(level, x, y, z, r, g, b, dur);
            runes.setGravity(grav);
            addParticle(runes);
        }
    }
    
    public void blockRunes2(double x, double y, double z, float r, float g, float b, int dur, float grav) {
        blockRunes(x, y, z, r, g, b, dur, grav);
    }
    
    public void drawPedestalShield(BlockPos pos) {
        Level level = getWorld();
        if (level != null) {
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                double x = pos.getX() + 0.5 + Math.cos(angle) * 0.5;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * 0.5;
                level.addParticle(ParticleTypes.ENCHANT, x, pos.getY() + 1, z, 0, 0.1, 0);
            }
        }
    }
    
    public void blockWard(double x, double y, double z, Direction side, float r, float g, float b) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            // The r, g, b parameters here are actually hit coordinates on the block face (0-1)
            FXBlockWard ward = new FXBlockWard(level, x + 0.5, y + 0.5, z + 0.5, side, r, g, b);
            addParticle(ward);
        }
    }
    
    public void smokeSpiral(double x, double y, double z, float rad, int start, int miny, int color) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            Color c = new Color(color);
            FXSmokeSpiral spiral = new FXSmokeSpiral(level, x, y, z, rad, start, miny);
            spiral.setColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
            addParticle(spiral);
        }
    }
    
    public void drawCurlyWisp(double x, double y, double z, double vx, double vy, double vz, 
            float scale, float r, float g, float b, float a, Direction side, int seed, int layer, int delay) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.WITCH, x, y, z, vx, vy, vz);
        }
    }
    
    public void voidStreak(double x, double y, double z, double x2, double y2, double z2, int seed, float scale) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.PORTAL, x, y, z, x2 - x, y2 - y, z2 - z);
        }
    }
    
    public void furnaceLavaFx(int x, int y, int z, int facingX, int facingZ) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.LAVA, 
                    x + 0.5 + facingX * 0.6, y + 0.3, z + 0.5 + facingZ * 0.6, 0, 0, 0);
        }
    }
    
    public void bottleTaintBreak(double x, double y, double z) {
        Level level = getWorld();
        if (level == null) return;
        
        for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.WITCH, x, y, z, 
                    rand.nextGaussian() * 0.15, rand.nextDouble() * 0.2, rand.nextGaussian() * 0.15);
        }
        level.playLocalSound(x, y, z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 
                1.0f, rand.nextFloat() * 0.1f + 0.9f, false);
    }
    
    public void cultistSpawn(double x, double y, double z, double a, double b, double c) {
        Level level = getWorld();
        if (level != null) {
            for (int i = 0; i < 5; i++) {
                level.addParticle(ParticleTypes.FLAME, x, y, z, a, b, c);
            }
        }
    }
    
    public void pechsCurseTick(double posX, double posY, double posZ) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.WITCH, posX, posY, posZ, 0, 0, 0);
        }
    }
    
    public void wispFXEG(double posX, double posY, double posZ, Entity target) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXWisp particle = new FXWisp(level, posX, posY, posZ, target);
            addParticle(particle);
        }
    }
    
    public void drawSlash(double x, double y, double z, double x2, double y2, double z2, int dur) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.SWEEP_ATTACK, (x + x2) / 2, (y + y2) / 2, (z + z2) / 2, 0, 0, 0);
        }
    }
    
    /**
     * Create sparkle particles flying from a block to the bore entity.
     */
    public void boreDigFx(int x, int y, int z, Entity e, BlockState bi, int md, int delay) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        float p = 50.0f;
        for (int a = 0; a < p / delay; a++) {
            if (rand.nextInt(4) == 0) {
                // Sparkle particle that homes to the entity
                FXBoreSparkle sparkle = new FXBoreSparkle(level, 
                        x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(), e);
                addParticle(sparkle);
            } else {
                // Use vanilla enchant particle as fallback for block debris
                level.addParticle(ParticleTypes.ENCHANT, 
                        x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(),
                        (e.getX() - x) * 0.1, (e.getY() - y) * 0.1, (e.getZ() - z) * 0.1);
            }
        }
    }
    
    public void sonicBoom(double x, double y, double z, Entity source, int duration) {
        Level level = getWorld();
        if (level == null) return;
        
        // Create expanding ring of particles
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI * 2 / 16;
            double px = x + Math.cos(angle) * 0.5;
            double pz = z + Math.sin(angle) * 0.5;
            double vx = Math.cos(angle) * 0.3;
            double vz = Math.sin(angle) * 0.3;
            level.addParticle(ParticleTypes.SONIC_BOOM, px, y + 1, pz, vx, 0, vz);
        }
    }
    
    public void boreTrailFx(BlockPos p1, Entity e, int count, int color, float scale, int ext) {
        Level level = getWorld();
        if (level == null) return;
        
        level.addParticle(ParticleTypes.ENCHANT, 
                p1.getX() + 0.5, p1.getY() + 0.5, p1.getZ() + 0.5,
                (e.getX() - p1.getX()) * 0.1, (e.getY() - p1.getY()) * 0.1, (e.getZ() - p1.getZ()) * 0.1);
    }
    
    // ==================== Entity Effects ====================
    
    public void splooshFX(Entity e) {
        Level level = getWorld();
        if (level != null) {
            level.addParticle(ParticleTypes.WITCH, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 
                    rand.nextGaussian() * 0.1, rand.nextGaussian() * 0.1, rand.nextGaussian() * 0.1);
        }
    }
    
    /**
     * Create explosion of taint particles from an entity.
     */
    public void taintsplosionFX(Entity e) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        for (int i = 0; i < 8; i++) {
            double px = e.getX() + rand.nextGaussian() * 0.3;
            double py = e.getY() + rand.nextFloat() * e.getBbHeight();
            double pz = e.getZ() + rand.nextGaussian() * 0.3;
            
            FXTaintParticle particle = new FXTaintParticle(level, px, py, pz,
                    rand.nextGaussian() * 0.15, rand.nextGaussian() * 0.15, rand.nextGaussian() * 0.15,
                    0.5f + rand.nextFloat() * 0.5f);
            addParticle(particle);
        }
    }
    
    /**
     * Create taint particles rising when tentacle emerges.
     */
    public void tentacleAriseFX(Entity e) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        int count = (int)(3 * e.getBbHeight());
        for (int i = 0; i < count; i++) {
            double px = e.getX() + rand.nextGaussian() * 0.3;
            double py = e.getY() + rand.nextFloat() * 0.3;
            double pz = e.getZ() + rand.nextGaussian() * 0.3;
            
            FXTaintParticle particle = new FXTaintParticle(level, px, py, pz,
                    rand.nextGaussian() * 0.02, 0.05 + rand.nextFloat() * 0.05, rand.nextGaussian() * 0.02,
                    0.4f + rand.nextFloat() * 0.3f);
            addParticle(particle);
        }
    }
    
    /**
     * Create taint splash when slime jumps.
     */
    public void slimeJumpFX(Entity e, int size) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        int count = 2 + size;
        for (int i = 0; i < count; i++) {
            double px = e.getX() + rand.nextGaussian() * 0.2 * size;
            double py = e.getY() + e.getBbHeight() / 2;
            double pz = e.getZ() + rand.nextGaussian() * 0.2 * size;
            
            FXTaintParticle particle = new FXTaintParticle(level, px, py, pz,
                    rand.nextGaussian() * 0.05, 0.05 + rand.nextFloat() * 0.05, rand.nextGaussian() * 0.05,
                    0.3f + rand.nextFloat() * 0.2f * size);
            addParticle(particle);
        }
    }
    
    /**
     * Create taint splatter when entity lands.
     */
    public void taintLandFX(Entity e) {
        ClientLevel level = getClientLevel();
        if (level == null) return;
        
        for (int i = 0; i < 4; i++) {
            double angle = rand.nextFloat() * Math.PI * 2;
            double speed = 0.05 + rand.nextFloat() * 0.05;
            
            FXTaintParticle particle = new FXTaintParticle(level, e.getX(), e.getY() + 0.1, e.getZ(),
                    Math.cos(angle) * speed, 0.02, Math.sin(angle) * speed,
                    0.3f + rand.nextFloat() * 0.2f);
            addParticle(particle);
        }
    }
    
    /**
     * Create a swarm particle that follows the target entity.
     * @param targetedEntity The entity to swarm around
     * @param speed Movement speed
     * @param turnSpeed Turn speed
     * @param pg Gravity
     * @return The created particle
     */
    public Object swarmParticleFX(Entity targetedEntity, float speed, float turnSpeed, float pg) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            // Spawn at random offset around target
            double x = targetedEntity.getX() + (rand.nextFloat() - rand.nextFloat()) * 2.0f;
            double y = targetedEntity.getY() + (rand.nextFloat() - rand.nextFloat()) * 2.0f;
            double z = targetedEntity.getZ() + (rand.nextFloat() - rand.nextFloat()) * 2.0f;
            
            // Purple/pink taint colors
            float r = 0.8f + rand.nextFloat() * 0.2f;
            float g = rand.nextFloat() * 0.4f;
            float b = 1.0f - rand.nextFloat() * 0.2f;
            
            FXSwarm swarm = new FXSwarm(level, x, y, z, targetedEntity, r, g, b, speed, turnSpeed, pg);
            addParticle(swarm);
            return swarm;
        }
        return null;
    }
    
    // ==================== Nitor Effects ====================
    
    /**
     * Draw the white core glow of a Nitor flame.
     */
    public void drawNitorCore(double x, double y, double z, double vx, double vy, double vz) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            FXGeneric particle = new FXGeneric(level, x, y, z, vx, vy, vz);
            particle.setMaxAge(10);
            particle.setColor(1.0f, 1.0f, 1.0f);
            particle.setAlphaF(1.0f);
            particle.setParticles(457, 1, 1);  // Bright glow particle
            particle.setScale(1.0f + (float)rand.nextGaussian() * 0.1f);
            particle.setLayer(1);
            particle.setRandomMovementScale(0.0002f, 0.0002f, 0.0002f);
            addParticle(particle);
        }
    }
    
    /**
     * Draw the colored flame particles of a Nitor.
     */
    public void drawNitorFlames(double x, double y, double z, double vx, double vy, double vz, int color, int delay) {
        ClientLevel level = getClientLevel();
        if (level != null) {
            Color c = new Color(color);
            FXGeneric particle = new FXGeneric(level, x, y, z, vx, vy, vz);
            particle.setMaxAge(10 + rand.nextInt(5));
            particle.setColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
            particle.setAlphaF(0.66f);
            particle.setLoop(true);
            particle.setGridSize(64);
            particle.setParticles(264, 8, 1);  // Flame animation
            particle.setScale(3.0f + rand.nextFloat());
            particle.setRandomMovementScale(0.0025f, 0.0f, 0.0025f);
            // Delay is not currently supported by FXGeneric - particle spawns immediately
            addParticle(particle);
        }
    }
    
    // ==================== GUI Effects ====================
    
    public void drawSimpleSparkleGui(Random rand, double x, double y, double x2, double y2, 
            float scale, float r, float g, float b, int delay, float decay, float grav) {
        // GUI particles need special handling - stub for now
    }
    
    /**
     * GenPart - Generic particle configuration class.
     * Mirrors TC4 {@code FXDispatcher.GenPart}.
     */
    public static class GenPart {
        public int grid = 64;
        public int age = 0;
        public float redStart = 1f, greenStart = 1f, blueStart = 1f;
        public float redEnd = 1f, greenEnd = 1f, blueEnd = 1f;
        public float[] alpha = new float[] { 1f };
        public float[] scale = new float[] { 1f };
        public float rot = 0f;
        public float rotstart = 0f;
        public boolean loop = false;
        public int partStart = 0, partNum = 1, partInc = 1;
        public int layer = 0;
        public double slowDown = 0.98;
        public float grav = 0f;
        public int delay = 0;
    }
}




