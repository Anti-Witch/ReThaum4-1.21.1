package thaumcraft.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.IFocusElement;
import thaumcraft.client.fx.particles.FXGeneric;

import javax.annotation.Nullable;
import java.awt.Color;

/**
 * Client-side particle presets for the focus system.
 * <p>
 * These are direct ports of the TC4/TC6 {@code FocusEffect.renderParticleFX}
 * bodies (and {@code EntityFocusProjectile.renderParticle}). They live in the
 * client package so the common focus classes never have to touch
 * {@link ClientLevel}/{@link FXGeneric} directly.
 */
@OnlyIn(Dist.CLIENT)
public final class FocusFX {

    private FocusFX() {}

    @Nullable
    private static ClientLevel asClientLevel(Level level) {
        return level instanceof ClientLevel clientLevel ? clientLevel : null;
    }

    /**
     * TC4 FocusEffectAir.renderParticleFX - a puff of wind sprites.
     */
    public static void air(Level level, double posX, double posY, double posZ,
                           double motionX, double motionY, double motionZ) {
        ClientLevel world = asClientLevel(level);
        if (world == null) return;

        RandomSource rand = world.random;
        FXDispatcher.GenPart pp = new FXDispatcher.GenPart();
        pp.grav = -0.1f;
        pp.age = 20 + rand.nextInt(10);
        pp.alpha = new float[] { 0.5f, 0.0f };
        pp.grid = 32;
        pp.partStart = 337;
        pp.partInc = 1;
        pp.partNum = 5;
        pp.slowDown = 0.75;
        pp.rot = (float) rand.nextGaussian() / 2.0f;
        float s = (float) (2.0 + rand.nextGaussian() * 0.5);
        pp.scale = new float[] { s, s * 2.0f };
        FXDispatcher.INSTANCE.drawGenericParticles(posX, posY, posZ, motionX, motionY, motionZ, pp);
    }

    /**
     * TC4 FocusEffectBreak.renderParticleFX - block debris sprites.
     */
    public static void breakBlock(Level level, double posX, double posY, double posZ,
                                  double motionX, double motionY, double motionZ) {
        ClientLevel world = asClientLevel(level);
        if (world == null) return;

        RandomSource rand = world.random;
        FXGeneric fb = new FXGeneric(world, posX, posY, posZ, motionX, motionY, motionZ);
        fb.setMaxAge(6 + rand.nextInt(6));
        int q = rand.nextInt(4);
        fb.setParticles(704 + q * 3, 3, 1);
        fb.setSlowDown(0.8);
        fb.setScale((float) (1.7 + rand.nextGaussian() * 0.3));
        FXDispatcher.INSTANCE.addEffect(fb);
    }

    /**
     * TC4 FocusEffectCurse.renderParticleFX - dark red curse motes.
     */
    public static void curse(Level level, double posX, double posY, double posZ,
                             double motionX, double motionY, double motionZ) {
        ClientLevel world = asClientLevel(level);
        if (world == null) return;

        RandomSource rand = world.random;
        FXGeneric fb = new FXGeneric(world, posX, posY, posZ, motionX, motionY, motionZ);
        fb.setMaxAge(8);
        fb.setColor(0.41f + rand.nextFloat() * 0.2f, 0.0f, 0.019f + rand.nextFloat() * 0.2f);
        fb.setAlphaKeyframes(0.0f, rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 0.0f);
        fb.setGridSize(16);
        fb.setParticles(72 + rand.nextInt(4), 1, 1);
        fb.setScale(2.0f + rand.nextFloat() * 4.0f);
        fb.setLoop(false);
        fb.setSlowDown(0.9);
        fb.setGravity(0.0f);
        fb.setRotationSpeedWithStart(rand.nextFloat(), 0.0f);
        FXDispatcher.INSTANCE.addEffectWithDelay(fb, rand.nextInt(4));
    }

    /**
     * TC4 FocusEffectEarth.renderParticleFX - rock/dirt motes that fall.
     */
    public static void earth(Level level, double posX, double posY, double posZ,
                             double motionX, double motionY, double motionZ) {
        ClientLevel world = asClientLevel(level);
        if (world == null) return;

        RandomSource rand = world.random;
        FXDispatcher.GenPart pp = new FXDispatcher.GenPart();
        pp.grav = 0.4f;
        pp.layer = 1;
        pp.age = 20 + rand.nextInt(10);
        pp.alpha = new float[] { 1.0f, 0.0f };
        pp.partStart = 75 + rand.nextInt(4);
        pp.partInc = 1;
        pp.partNum = 1;
        pp.slowDown = 0.9;
        pp.rot = (float) rand.nextGaussian();
        float s = (float) (1.0 + rand.nextGaussian() * 0.2);
        pp.scale = new float[] { s, s / 2.0f };
        FXDispatcher.INSTANCE.drawGenericParticles(posX, posY, posZ, motionX, motionY, motionZ, pp);
    }

    /**
     * TC4 FocusEffectExchange.renderParticleFX - grey transmutation sparkles.
     */
    public static void exchange(Level level, double x, double y, double z,
                                double vx, double vy, double vz) {
        ClientLevel world = asClientLevel(level);
        if (world == null) return;

        RandomSource rand = world.random;
        FXGeneric fb = new FXGeneric(world, x, y, z,
                vx + rand.nextGaussian() * 0.01, vy + rand.nextGaussian() * 0.01, vz + rand.nextGaussian() * 0.01);
        fb.setMaxAge(9);
        fb.setColor(0.25f + rand.nextFloat() * 0.25f,
                0.25f + rand.nextFloat() * 0.25f,
                0.25f + rand.nextFloat() * 0.25f);
        fb.setAlphaKeyframes(0.0f, 0.6f, 0.6f, 0.0f);
        fb.setGridSize(64);
        fb.setParticles(448, 9, 1);
        fb.setScaleKeyframes(0.5f, 0.25f);
        fb.setGravity((float) rand.nextGaussian() * 0.01f);
        fb.setRandomMovementScale(0.0025f, 0.0025f, 0.0025f);
        FXDispatcher.INSTANCE.addEffect(fb);
    }

    /**
     * TC4 FocusEffectFire.renderParticleFX - a splash of flame sprites.
     */
    public static void fire(Level level, double posX, double posY, double posZ,
                            double motionX, double motionY, double motionZ) {
        ClientLevel world = asClientLevel(level);
        if (world == null) return;

        RandomSource rand = world.random;
        FXDispatcher.GenPart pp = new FXDispatcher.GenPart();
        pp.grav = -0.2f;
        pp.age = 10;
        pp.alpha = new float[] { 0.7f };
        pp.partStart = 640;
        pp.partInc = 1;
        pp.partNum = 10;
        pp.slowDown = 0.75;
        pp.scale = new float[] { (float) (1.5 + rand.nextGaussian() * 0.2) };
        FXDispatcher.INSTANCE.drawGenericParticles(posX, posY, posZ, motionX, motionY, motionZ, pp);
    }

    /**
     * TC4 FocusEffectFlux.renderParticleFX - purple flux wisps.
     */
    public static void flux(Level level, double x, double y, double z,
                            double vx, double vy, double vz) {
        ClientLevel world = asClientLevel(level);
        if (world == null) return;

        RandomSource rand = world.random;
        FXGeneric fb = new FXGeneric(world, x, y, z,
                vx + rand.nextGaussian() * 0.01, vy + rand.nextGaussian() * 0.01, vz + rand.nextGaussian() * 0.01);
        fb.setMaxAge((int) (15.0f + 10.0f * rand.nextFloat()));
        fb.setColor(0.25f + rand.nextFloat() * 0.25f, 0.0f, 0.25f + rand.nextFloat() * 0.25f);
        fb.setAlphaKeyframes(0.0f, 1.0f, 1.0f, 0.0f);
        fb.setGridSize(64);
        fb.setParticles(128, 14, 1);
        fb.setScaleKeyframes(2.0f + rand.nextFloat(), 0.25f + rand.nextFloat() * 0.25f);
        fb.setLoop(true);
        fb.setSlowDown(0.9);
        fb.setGravity((float) rand.nextGaussian() * 0.1f);
        fb.setRandomMovementScale(0.0125f, 0.0125f, 0.0125f);
        fb.setRotationSpeed((float) rand.nextGaussian());
        FXDispatcher.INSTANCE.addEffectWithDelay(fb, rand.nextInt(4));
    }

    /**
     * TC4 FocusEffectFrost.renderParticleFX - snowflakes that settle.
     */
    public static void frost(Level level, double posX, double posY, double posZ,
                             double motionX, double motionY, double motionZ) {
        ClientLevel world = asClientLevel(level);
        if (world == null) return;

        RandomSource rand = world.random;
        FXGeneric fb = new FXGeneric(world, posX, posY, posZ, motionX, motionY, motionZ);
        fb.setMaxAge(40 + rand.nextInt(40));
        fb.setAlphaKeyframes(1.0f, 0.0f);
        fb.setParticles(8, 1, 1);
        fb.setGravity(0.033f);
        fb.setSlowDown(0.8);
        fb.setRandomMovementScale(0.0025f, 1.0E-4f, 0.0025f);
        fb.setScale((float) (0.7 + rand.nextGaussian() * 0.3));
        fb.setRotationSpeedWithStart(rand.nextFloat() * 3.0f, (float) rand.nextGaussian() / 4.0f);
        FXDispatcher.INSTANCE.addEffectWithDelay(fb, 0);
    }

    /**
     * TC4 FocusEffectHeal.renderParticleFX - white/golden healing sparkles.
     */
    public static void heal(Level level, double x, double y, double z,
                            double vx, double vy, double vz) {
        ClientLevel world = asClientLevel(level);
        if (world == null) return;

        RandomSource rand = world.random;
        FXGeneric fb = new FXGeneric(world, x, y, z,
                vx + rand.nextGaussian() * 0.01, vy + rand.nextGaussian() * 0.01, vz + rand.nextGaussian() * 0.01);
        fb.setMaxAge((int) (10.0f + 10.0f * rand.nextFloat()));
        fb.setColor(1.0f, 1.0f, 1.0f);
        fb.setAlphaKeyframes(0.0f, 0.7f, 0.7f, 0.0f);
        fb.setGridSize(64);
        fb.setParticles(0, 1, 1);
        fb.setScaleKeyframes(rand.nextFloat() * 2.0f, rand.nextFloat());
        fb.setSlowDown(0.8);
        fb.setGravity((float) rand.nextGaussian() * 0.1f);
        fb.setRandomMovementScale(0.0125f, 0.0125f, 0.0125f);
        fb.setRotationSpeed((float) rand.nextGaussian());
        FXDispatcher.INSTANCE.addEffectWithDelay(fb, rand.nextInt(4));
    }

    /**
     * TC4 FocusEffectRift.renderParticleFX - blue rift shards.
     */
    public static void rift(Level level, double posX, double posY, double posZ,
                            double motionX, double motionY, double motionZ) {
        ClientLevel world = asClientLevel(level);
        if (world == null) return;

        RandomSource rand = world.random;
        FXGeneric fb = new FXGeneric(world, posX, posY, posZ, motionX, motionY, motionZ);
        fb.setMaxAge(16 + rand.nextInt(16));
        fb.setParticles(384 + rand.nextInt(16), 1, 1);
        fb.setSlowDown(0.75);
        fb.setAlphaKeyframes(1.0f, 0.0f);
        fb.setScale((float) (0.7 + rand.nextGaussian() * 0.3));
        fb.setColor(0.25f, 0.25f, 1.0f);
        fb.setRandomMovementScale(0.01f, 0.01f, 0.01f);
        FXDispatcher.INSTANCE.addEffectWithDelay(fb, 0);
    }

    /**
     * TC4 EntityFocusCloud.onUpdate (client): the cloud is filled with the
     * focus effect's own particles plus coloured cloud puffs.
     */
    public static void cloudParticles(Level level, String[] parts, double posX, double posY, double posZ, float radius) {
        ClientLevel world = asClientLevel(level);
        if (world == null || parts == null || parts.length == 0) return;

        RandomSource rand = world.random;
        for (int a = 0; a < radius; ++a) {
            String key = parts[rand.nextInt(parts.length)];
            FXDispatcher.INSTANCE.drawFocusCloudParticle(
                    posX + rand.nextGaussian() * radius / 2.0 * 0.85,
                    posY + rand.nextGaussian() * radius / 2.0 * 0.85,
                    posZ + rand.nextGaussian() * radius / 2.0 * 0.85,
                    rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01,
                    FocusEngine.getElementColor(key));

            IFocusElement element = FocusEngine.getElement(key);
            if (element instanceof FocusEffect effect) {
                effect.renderParticleFX(world,
                        posX + rand.nextGaussian() * radius / 2.0,
                        posY + rand.nextGaussian() * radius / 2.0,
                        posZ + rand.nextGaussian() * radius / 2.0,
                        rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01);
            }
        }
    }

    /**
     * TC4 EntityFocusMine.onUpdate (client): armed mines shimmer with the focus
     * effect's particles.
     */
    public static void mineParticles(Level level, String[] parts, double posX, double posY, double posZ) {
        ClientLevel world = asClientLevel(level);
        if (world == null || parts == null || parts.length == 0) return;

        RandomSource rand = world.random;
        String key = parts[rand.nextInt(parts.length)];
        IFocusElement element = FocusEngine.getElement(key);
        if (element instanceof FocusEffect effect) {
            effect.renderParticleFX(world,
                    posX + rand.nextGaussian() * 0.1,
                    posY + rand.nextGaussian() * 0.1,
                    posZ + rand.nextGaussian() * 0.1,
                    rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01);
        }
    }

    /**
     * TC4 EntityFocusProjectile.renderParticle: the trail left behind a flying
     * focus projectile - a coloured mote plus (on the first tick) one full
     * effect burst so the projectile matches the focus that was cast.
     *
     * @param parts effect keys carried by the projectile (synced to the client)
     */
    public static void projectileTrail(Level level, String[] parts, double posX, double posY, double posZ,
                                       double motionX, double motionY, double motionZ, boolean first) {
        ClientLevel world = asClientLevel(level);
        if (world == null || parts == null || parts.length == 0) return;

        RandomSource rand = world.random;
        String key = parts[rand.nextInt(parts.length)];
        Color c1 = new Color(FocusEngine.getElementColor(key));

        float scale = 1.0f;
        FXDispatcher.INSTANCE.drawFireMote(
                (float) posX, (float) posY, (float) posZ,
                0.0125f * (rand.nextFloat() - 0.5f) * scale,
                0.0125f * (rand.nextFloat() - 0.5f) * scale,
                0.0125f * (rand.nextFloat() - 0.5f) * scale,
                c1.getRed() / 255.0f, c1.getGreen() / 255.0f, c1.getBlue() / 255.0f,
                0.5f, 7.0f * scale);

        if (first) {
            IFocusElement element = FocusEngine.getElement(key);
            if (element instanceof FocusEffect effect) {
                effect.renderParticleFX(world, posX, posY, posZ, motionX, motionY, motionZ);
            }
        }
    }
}
