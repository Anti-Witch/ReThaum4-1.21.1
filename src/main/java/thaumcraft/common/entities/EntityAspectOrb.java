package thaumcraft.common.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.init.ModEntities;

/**
 * EntityAspectOrb - TC4's coloured aspect orb (dropped by slain mobs and
 * released by unstable aura nodes).
 * <p>
 * Ported from OpenTC4 {@code thaumcraft.common.entities.EntityAspectOrb}:
 * <ul>
 *   <li>0.125 block bob with TC4 gravity (-0.03/tick), lava fizz and ground
 *       friction/slipperiness handling.</li>
 *   <li>Orbs live {@value #ORB_MAX_AGE} ticks and slowly home in on the nearest
 *       player that carries a <b>hotbar wand with room</b> for the orb's primal
 *       aspect (8 block range, re-checked every 5 ticks).</li>
 *   <li>On contact the orb charges that wand with its aspect value
 *       ({@link WandItem#addVis}) and plays the "random.orb" pickup sound, then
 *       dies - this is TC4's only use for orbs.</li>
 *   <li>Port extension (kept from the previous port revision): when the player
 *       has no suitable wand the aspect is still granted to the player's
 *       research pool, so node orbs stay useful without a wand.</li>
 * </ul>
 * The orb's aspect is synced by tag so the client renderer
 * ({@code AspectOrbRenderer}) can tint the sprite with the aspect colour.
 */
public class EntityAspectOrb extends Entity {

    /** TC4 EntityAspectOrb.orbMaxAge. */
    public static final int ORB_MAX_AGE = 150;

    private static final EntityDataAccessor<String> DATA_ASPECT =
            SynchedEntityData.defineId(EntityAspectOrb.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_VALUE =
            SynchedEntityData.defineId(EntityAspectOrb.class, EntityDataSerializers.INT);

    /** TC4 EntityAspectOrb.orbAge. */
    public int orbAge = 0;
    /** TC4 EntityAspectOrb.orbMaxAge. */
    public int orbMaxAge = ORB_MAX_AGE;
    /** TC4 EntityAspectOrb.orbCooldown (kept for save compatibility). */
    public int orbCooldown;

    private int orbHealth = 5;
    private Aspect aspect;
    private int aspectValue = 1;

    /** Server-side current homing target (TC4 EntityAspectOrb.closestPlayer). */
    private Player closestPlayer;

    public EntityAspectOrb(EntityType<? extends EntityAspectOrb> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public EntityAspectOrb(Level level, double x, double y, double z, Aspect aspect, int value) {
        this(ModEntities.ASPECT_ORB.get(), level);
        this.setPos(x, y, z);
        this.setYRot((float) (Math.random() * 360.0));
        // TC4 gives the orb a small random initial velocity
        this.setDeltaMovement(
                ((float) Math.random() * 0.2f - 0.1f) * 2.0f,
                (float) Math.random() * 0.2f * 2.0f,
                ((float) Math.random() * 0.2f - 0.1f) * 2.0f);
        this.setAspect(aspect);
        this.setAspectValue(value);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ASPECT, "");
        builder.define(DATA_VALUE, 1);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_ASPECT.equals(key)) {
            this.aspect = Aspect.getAspect(this.entityData.get(DATA_ASPECT));
        } else if (DATA_VALUE.equals(key)) {
            this.aspectValue = this.entityData.get(DATA_VALUE);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.orbCooldown > 0) {
            --this.orbCooldown;
        }

        // TC4 EntityAspectOrb.onUpdate gravity
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.03, 0.0));

        // TC4: lava makes the orb pop up with a fizz
        if (this.isInLava()) {
            this.setDeltaMovement(
                    (this.random.nextFloat() - this.random.nextFloat()) * 0.2f,
                    0.2f,
                    (this.random.nextFloat() - this.random.nextFloat()) * 0.2f);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.LAVA_EXTINGUISH, SoundSource.AMBIENT,
                    0.4f, 2.0f + this.random.nextFloat() * 0.4f);
        }

        if (this.level().isClientSide) {
            this.spawnClientParticles();
        } else {
            this.serverTick();
        }

        // TC4 movement: move, then apply drag (ground friction / slipperiness)
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 motion = this.getDeltaMovement();
        float friction = 0.98f;
        if (this.onGround()) {
            friction = 0.58800006f;
            BlockPos below = BlockPos.containing(this.getX(), this.getBoundingBox().minY - 0.5, this.getZ());
            BlockState state = this.level().getBlockState(below);
            if (!state.isAir()) {
                friction = state.getBlock().getFriction() * 0.98f;
            }
        }
        this.setDeltaMovement(
                motion.x * friction,
                motion.y * 0.98,
                motion.z * friction);
        if (this.onGround()) {
            this.setDeltaMovement(
                    this.getDeltaMovement().x,
                    this.getDeltaMovement().y * -0.9,
                    this.getDeltaMovement().z);
        }

        // Age out (TC4 ages orbs on both sides so the client can shrink the
        // sprite as the orb expires)
        ++this.orbAge;
        if (this.orbAge >= this.orbMaxAge) {
            this.discard();
        }
    }

    /**
     * TC4 server logic: pick the nearest player with a hotbar wand that has room
     * for this orb's aspect and accelerate towards them.
     */
    private void serverTick() {
        double range = 8.0;

        // TC4: re-acquire a target every 5 ticks while we have none
        if (this.tickCount % 5 == 0 && this.closestPlayer == null) {
            AABB area = this.getBoundingBox().inflate(range);
            Player best = null;
            double bestDist = Double.MAX_VALUE;
            for (Player player : this.level().getEntitiesOfClass(Player.class, area)) {
                if (!player.isAlive() || player.isSpectator()) {
                    continue;
                }
                double dist = player.distanceToSqr(this);
                if (dist < bestDist
                        && WandItem.findWandInHotbarWithRoom(player, this.getAspect(), this.getAspectValue()) >= 0) {
                    bestDist = dist;
                    best = player;
                }
            }
            this.closestPlayer = best;
        }

        if (this.closestPlayer != null && (this.closestPlayer.isRemoved()
                || !this.closestPlayer.isAlive()
                || this.closestPlayer.level() != this.level())) {
            this.closestPlayer = null;
        }

        if (this.closestPlayer != null) {
            // TC4: d1 = delta / 8, d4 = |d1|, d5 = 1 - d4; motion += d1 / d4 * d5^2 * 0.1
            Vec3 delta = new Vec3(
                    this.closestPlayer.getX(),
                    this.closestPlayer.getY() + this.closestPlayer.getEyeHeight(),
                    this.closestPlayer.getZ()).subtract(this.position()).scale(1.0 / range);
            double len = delta.length();
            double pull = 1.0 - len;
            if (pull > 0.0 && len > 1.0E-4) {
                pull *= pull;
                this.setDeltaMovement(this.getDeltaMovement().add(delta.scale(pull * 0.1 / len)));
            }
        }
    }

    /**
     * TC4 EntityAspectOrb.onCollideWithPlayer: a primal orb touching a player
     * that carries a wand with room for it is absorbed and charges that wand.
     * The port additionally feeds the research pool when no wand can take the
     * aspect (see class javadoc).
     */
    @Override
    public void playerTouch(Player player) {
        if (this.level().isClientSide || this.isRemoved()) {
            return;
        }
        Aspect orbAspect = this.getAspect();
        if (orbAspect == null) {
            return;
        }

        int slot = WandItem.findWandInHotbarWithRoom(player, orbAspect, this.getAspectValue());
        if (this.orbCooldown == 0 && orbAspect.isPrimal() && slot >= 0) {
            ItemStack wandStack = player.getInventory().getItem(slot);
            if (wandStack.getItem() instanceof WandItem wand) {
                wand.addVis(wandStack, orbAspect, this.getAspectValue(), true);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                        0.1f, 0.5f * ((this.random.nextFloat() - this.random.nextFloat()) * 0.7f + 1.8f));
                this.discard();
                return;
            }
        }

        // Port fallback: no wand with room - still grant the aspect to the
        // player's research pool (previous port behaviour).
        this.grantAspectToPlayer(player);
        this.discard();
    }

    /**
     * Add the orb's aspect to the player's research aspect pool.
     */
    private void grantAspectToPlayer(Player player) {
        if (this.aspect == null) {
            return;
        }
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge != null) {
            knowledge.addAspectsToResearchPool(this.aspect, this.aspectValue);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                knowledge.sync(serverPlayer);
            }
        }
    }

    /**
     * TC4 orb render: a glowing sprite tinted with the aspect colour plus a
     * small coloured particle trail.
     */
    private void spawnClientParticles() {
        Aspect orbAspect = this.getAspect();
        if (orbAspect == null) {
            return;
        }
        int color = orbAspect.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        this.level().addParticle(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, r, g, b),
                this.getX() + (this.random.nextDouble() - 0.5) * 0.3,
                this.getY() + (this.random.nextDouble() - 0.5) * 0.3,
                this.getZ() + (this.random.nextDouble() - 0.5) * 0.3,
                0.0, 0.0, 0.0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        this.markHurt();
        this.orbHealth = (int) (this.orbHealth - amount);
        if (this.orbHealth <= 0) {
            this.discard();
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        return distSqr < 1024.0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("Health", (short) this.orbHealth);
        tag.putInt("Age", this.orbAge);
        tag.putInt("Value", this.aspectValue);
        tag.putInt("MaxAge", this.orbMaxAge);
        if (this.getAspect() != null) {
            tag.putString("Aspect", this.getAspect().getTag());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.orbHealth = tag.getShort("Health") & 255;
        this.orbAge = tag.getInt("Age");
        this.setAspectValue(tag.getInt("Value"));
        if (tag.contains("MaxAge")) {
            this.orbMaxAge = Math.max(1, tag.getInt("MaxAge"));
        }
        if (tag.contains("Aspect")) {
            this.setAspect(Aspect.getAspect(tag.getString("Aspect")));
        }
    }

    public Aspect getAspect() {
        if (this.aspect == null) {
            // Client-side: the aspect arrives through the synced data
            this.aspect = Aspect.getAspect(this.entityData.get(DATA_ASPECT));
        }
        return this.aspect;
    }

    public void setAspect(Aspect aspect) {
        this.aspect = aspect;
        this.entityData.set(DATA_ASPECT, aspect == null ? "" : aspect.getTag());
    }

    public int getAspectValue() {
        return this.aspectValue;
    }

    public void setAspectValue(int value) {
        this.aspectValue = Math.max(1, value);
        this.entityData.set(DATA_VALUE, this.aspectValue);
    }

    /** Kept for the old port API (TileNode used it). */
    public int getAmount() {
        return this.getAspectValue();
    }

    /** Kept for the old port API (TileNode used it). */
    public void setAmount(int amount) {
        this.setAspectValue(amount);
    }
}
