package thaumcraft.common.entities.construct;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import thaumcraft.init.ModSounds;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * EntityOwnedConstruct - Base class for player-owned construct entities.
 * Constructs are magical automatons that serve their owner.
 * Features:
 * - Owner tracking via UUID
 * - Team membership with owner
 * - Valid spawn check (must be spawned properly)
 * - Won't attack owner or teammates
 * - Underwater breathing
 */
public abstract class EntityOwnedConstruct extends PathfinderMob implements OwnableEntity {
    
    private static final EntityDataAccessor<Byte> DATA_FLAGS = 
            SynchedEntityData.defineId(EntityOwnedConstruct.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID = 
            SynchedEntityData.defineId(EntityOwnedConstruct.class, EntityDataSerializers.OPTIONAL_UUID);
    
    private static final byte FLAG_OWNED = 0x04;
    
    private boolean validSpawn = false;
    
    protected EntityOwnedConstruct(EntityType<? extends EntityOwnedConstruct> type, Level level) {
        super(type, level);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS, (byte) 0);
        builder.define(DATA_OWNER_UUID, Optional.empty());
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }
    
    // ==================== Owner System ====================
    
    public boolean isOwned() {
        return (this.entityData.get(DATA_FLAGS) & FLAG_OWNED) != 0;
    }
    
    public void setOwned(boolean owned) {
        byte flags = this.entityData.get(DATA_FLAGS);
        if (owned) {
            this.entityData.set(DATA_FLAGS, (byte)(flags | FLAG_OWNED));
        } else {
            this.entityData.set(DATA_FLAGS, (byte)(flags & ~FLAG_OWNED));
        }
    }
    
    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }
    
    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }
    
    @Nullable
    @Override
    public LivingEntity getOwner() {
        try {
            UUID uuid = getOwnerUUID();
            return uuid == null ? null : level().getPlayerByUUID(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public boolean isOwner(LivingEntity entity) {
        return entity == getOwner();
    }
    
    // ==================== Team System ====================
    

    
    @Override
    public boolean isAlliedTo(Entity other) {
        if (isOwned()) {
            LivingEntity owner = getOwner();
            if (other == owner) {
                return true;
            }
            if (owner != null) {
                return owner.isAlliedTo(other);
            }
        }
        return super.isAlliedTo(other);
    }
    
    // ==================== Valid Spawn ====================
    
    public void setValidSpawn() {
        this.validSpawn = true;
    }
    
    public boolean hasValidSpawn() {
        return this.validSpawn;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Clear attack target if it's a teammate
        if (getTarget() != null && isAlliedTo(getTarget())) {
            setTarget(null);
        }
        
        // Remove if not spawned properly
        if (!level().isClientSide && !validSpawn) {
            discard();
        }
    }
    
    // ==================== Immunities ====================
    
    @Override
    protected int decreaseAirSupply(int air) {
        return air; // Doesn't drown
    }
    
    
    
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false; // Never despawn
    }
    
    // ==================== Sounds ====================
    
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CLACK.get();
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CLACK.get();
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TOOL.get();
    }
    
    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }
    
    // ==================== Interaction ====================
    
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isRemoved()) {
            return InteractionResult.PASS;
        }
        
        // Don't interact if sneaking or using name tag
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        
        // Only owner can interact
        if (!level().isClientSide && !isOwner(player)) {
            player.displayClientMessage(Component.translatable("tc.notowned"), true);
            return InteractionResult.SUCCESS;
        }
        
        return super.mobInteract(player, hand);
    }
    
    // ==================== Death ====================
    
    @Override
    public void die(DamageSource source) {
        // Send death message to owner if named
        if (!level().isClientSide && level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_SHOWDEATHMESSAGES) 
                && hasCustomName() && getOwner() instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(getCombatTracker().getDeathMessage());
        }
        super.die(source);
    }
    
    // ==================== NBT ====================
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("ValidSpawn", validSpawn);
        
        UUID uuid = getOwnerUUID();
        if (uuid != null) {
            tag.putUUID("Owner", uuid);
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        validSpawn = tag.getBoolean("ValidSpawn");
        
        if (tag.hasUUID("Owner")) {
            setOwnerUUID(tag.getUUID("Owner"));
            setOwned(true);
        } else if (tag.contains("OwnerUUID", 8)) {
            // Legacy support
            String uuidStr = tag.getString("OwnerUUID");
            if (!uuidStr.isEmpty()) {
                try {
                    setOwnerUUID(UUID.fromString(uuidStr));
                    setOwned(true);
                } catch (Throwable e) {
                    setOwned(false);
                }
            }
        }
    }
}




