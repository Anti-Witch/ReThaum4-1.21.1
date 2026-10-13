package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;
import java.util.List;

/**
 * EntityPech - A neutral trader mob found in magical biomes.
 * 
 * Features:
 * - Three types: regular (melee), mage (magic wand), and forager (bow)
 * - Can be "tamed" by giving them valued items
 * - Has a trade inventory for trading with players
 * - Will pick up items on the ground
 * - Becomes angry when attacked (along with nearby pech)
 * - Slow health regeneration
 */
@SuppressWarnings("deprecation")
public class EntityPech extends Monster implements RangedAttackMob {
    
    private static final EntityDataAccessor<Byte> DATA_TYPE = 
            SynchedEntityData.defineId(EntityPech.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_ANGER = 
            SynchedEntityData.defineId(EntityPech.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_TAMED = 
            SynchedEntityData.defineId(EntityPech.class, EntityDataSerializers.BOOLEAN);
    
    // Pech types
    public static final int TYPE_REGULAR = 0;
    public static final int TYPE_MAGE = 1;
    public static final int TYPE_FORAGER = 2;
    
    // Trade inventory (9 slots)
    public NonNullList<ItemStack> loot = NonNullList.withSize(9, ItemStack.EMPTY);
    public boolean trading = false;
    
    // Animation
    public float mumble = 0.0f;
    private int chargeCount = 0;
    
    /**
     * Get the mumble animation value for rendering.
     */
    public float getMumble() {
        return mumble;
    }
    
    // AI goals that need to be swapped based on equipment
    private RangedAttackGoal aiArrowAttack;
    private RangedAttackGoal aiBlastAttack;
    private MeleeAttackGoal aiMeleeAttack;
    
    public EntityPech(EntityType<? extends EntityPech> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        ((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
        
        setDropChance(EquipmentSlot.MAINHAND, 0.2f);
        setDropChance(EquipmentSlot.OFFHAND, 0.2f);
    }
    
    public EntityPech(Level level) {
        this(ModEntities.PECH.get(), level);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, (byte) 0);
        builder.define(DATA_ANGER, 0);
        builder.define(DATA_TAMED, false);
    }
    
    @Override
    protected void registerGoals() {
        aiArrowAttack = new RangedAttackGoal(this, 0.6, 20, 50, 15.0f);
        aiBlastAttack = new RangedAttackGoal(this, 0.6, 20, 50, 15.0f);
        aiMeleeAttack = new MeleeAttackGoal(this, 0.6, false);
        
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // AI goal 1 is trading (TODO: implement trading AI)
        // AI goal 2 is combat (set by setCombatTask)
        // AI goal 3 is item pickup (TODO: implement item pickup AI)
        this.goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.5));
        this.goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0f, 1.0f));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0f));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, 
                target -> getAnger() > 0));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.ARMOR, 2.0);
    }
    
    /**
     * Static spawn rule check for use with SpawnPlacementRegisterEvent.
     * Pechs spawn in magical biomes in moderately lit areas.
     */
    public static boolean checkPechSpawnRules(EntityType<? extends EntityPech> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Pechs spawn in dimmer areas but not total darkness
        // They prefer magical forest biomes (handled by biome modifiers)
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        
        // Spawn in areas with moderate light - not pitch black, not bright daylight
        // Sky light <= 12, but some block light is okay
        return skyLight <= 12 && (blockLight >= 0 && blockLight <= 10);
    }
    
    /**
     * Sets the appropriate combat AI based on held weapon.
     */
    public void setCombatTask() {
        if (level() != null && !level().isClientSide) {
            goalSelector.removeGoal(aiMeleeAttack);
            goalSelector.removeGoal(aiArrowAttack);
            goalSelector.removeGoal(aiBlastAttack);
            
            ItemStack weapon = getMainHandItem();
            if (weapon.getItem() instanceof BowItem) {
                goalSelector.addGoal(2, aiArrowAttack);
            } else if (weapon.is(Items.STICK)) { // TODO: Check for pechWand when implemented
                goalSelector.addGoal(2, aiBlastAttack);
            } else {
                goalSelector.addGoal(2, aiMeleeAttack);
            }
        }
    }
    
    // ==================== Pech Type System ====================
    
    public int getPechType() {
        return this.entityData.get(DATA_TYPE);
    }
    
    public void setPechType(int type) {
        this.entityData.set(DATA_TYPE, (byte) type);
    }
    
    public int getAnger() {
        return this.entityData.get(DATA_ANGER);
    }
    
    public void setAnger(int anger) {
        this.entityData.set(DATA_ANGER, anger);
    }
    
    public boolean isTamed() {
        return this.entityData.get(DATA_TAMED);
    }
    
    public void setTamed(boolean tamed) {
        this.entityData.set(DATA_TAMED, tamed);
    }
    
    // ==================== Spawn / Equipment ====================
    
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        
        setEquipmentBasedOnDifficulty(difficulty);
        
        ItemStack weapon = getMainHandItem();
        if (weapon.is(Items.STICK)) { // TODO: pechWand
            setPechType(TYPE_MAGE);
            setDropChance(EquipmentSlot.MAINHAND, 0.1f);
        } else if (!weapon.isEmpty()) {
            if (weapon.getItem() instanceof BowItem) {
                setPechType(TYPE_FORAGER);
            }
            populateDefaultEquipmentEnchantments(level, random, difficulty);
        }
        
        float diffFactor = difficulty.getSpecialMultiplier();
        setCanPickUpLoot(random.nextFloat() < 0.75f * diffFactor);
        setCombatTask();
        
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }
    
    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
        int roll = random.nextInt(20);
        ItemStack weapon = switch (roll) {
            case 0, 12 -> new ItemStack(Items.STICK); // TODO: pechWand
            case 1 -> new ItemStack(Items.STONE_SWORD);
            case 3 -> new ItemStack(Items.STONE_AXE);
            case 5 -> new ItemStack(Items.IRON_SWORD);
            case 6 -> new ItemStack(Items.IRON_AXE);
            case 7 -> new ItemStack(Items.FISHING_ROD);
            case 8 -> new ItemStack(Items.STONE_PICKAXE);
            case 9 -> new ItemStack(Items.IRON_PICKAXE);
            case 2, 4, 10, 11, 13 -> new ItemStack(Items.BOW);
            default -> ItemStack.EMPTY;
        };
        
        if (!weapon.isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }
    }
    
    private void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        populateDefaultEquipmentSlots(random, difficulty);
    }
    
    // ==================== Combat ====================
    
    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (getPechType() == TYPE_FORAGER) {
            // Bow attack
            Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), null);
            
            double dx = target.getX() - getX();
            double dy = target.getBoundingBox().minY + target.getBbHeight() / 3.0 - arrow.getY();
            double dz = target.getZ() - getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            
            arrow.shoot(dx, dy + dist * 0.2, dz, 1.6f, 14 - level().getDifficulty().getId() * 4);
            arrow.setBaseDamage(power * 2.0 + random.nextGaussian() * 0.25 + level().getDifficulty().getId() * 0.11);
            
            playSound(SoundEvents.ARROW_SHOOT, 1.0f, 1.0f / (getRandom().nextFloat() * 0.4f + 0.8f));
            level().addFreshEntity(arrow);
        } else if (getPechType() == TYPE_MAGE) {
            // Magic attack
            // TODO: Implement FocusEngine.castFocusPackage when focus system is complete
            // For now, fire a simple projectile
            swing(getUsedItemHand());
            // Placeholder: just do direct damage for now
            if (distanceToSqr(target) < 256) {
                target.hurt(damageSources().indirectMagic(this, this), 4.0f + random.nextFloat() * 2.0f);
            }
        }
    }
    
    private void becomeAngryAt(Entity target) {
        if (target instanceof Player player && player.isCreative()) {
            return;
        }
        
        if (getAnger() <= 0) {
            level().broadcastEntityEvent(this, (byte) 19);
            playSound(ModSounds.PECH_CHARGE.get(), getSoundVolume(), getVoicePitch());
        }
        
        if (target instanceof LivingEntity living) {
            setTarget(living);
        }
        setAnger(400 + random.nextInt(400));
        setTamed(false);
        setCombatTask();
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        
        Entity attacker = source.getEntity();
        if (attacker instanceof Player) {
            // Alert nearby pechs
            AABB searchBox = getBoundingBox().inflate(32.0, 16.0, 32.0);
            List<EntityPech> nearbyPechs = level().getEntitiesOfClass(EntityPech.class, searchBox);
            for (EntityPech pech : nearbyPechs) {
                pech.becomeAngryAt(attacker);
            }
            becomeAngryAt(attacker);
        }
        
        return super.hurt(source, amount);
    }
    
    // ==================== Update Logic ====================
    
    @Override
    public void tick() {
        // Decay mumble animation
        if (mumble > 0.0f) {
            mumble *= 0.75f;
        }
        
        // Decay anger
        if (getAnger() > 0) {
            setAnger(getAnger() - 1);
        }
        
        // Charging sound when angry
        if (getAnger() > 0 && getTarget() != null) {
            if (chargeCount > 0) {
                --chargeCount;
            }
            if (chargeCount == 0) {
                chargeCount = 100;
                playSound(ModSounds.PECH_CHARGE.get(), getSoundVolume(), getVoicePitch());
            }
            level().broadcastEntityEvent(this, (byte) 17);
        }
        
        // Particles when angry (client)
        if (level().isClientSide && random.nextInt(15) == 0 && getAnger() > 0) {
            double dx = random.nextGaussian() * 0.02;
            double dy = random.nextGaussian() * 0.02;
            double dz = random.nextGaussian() * 0.02;
            level().addParticle(ParticleTypes.ANGRY_VILLAGER,
                    getX() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(),
                    getY() + 0.5 + random.nextFloat() * getBbHeight(),
                    getZ() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(),
                    dx, dy, dz);
        }
        
        // Happy particles when tamed (client)
        if (level().isClientSide && random.nextInt(25) == 0 && isTamed()) {
            double dx = random.nextGaussian() * 0.02;
            double dy = random.nextGaussian() * 0.02;
            double dz = random.nextGaussian() * 0.02;
            level().addParticle(ParticleTypes.HAPPY_VILLAGER,
                    getX() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(),
                    getY() + 0.5 + random.nextFloat() * getBbHeight(),
                    getZ() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(),
                    dx, dy, dz);
        }
        
        super.tick();
    }
    
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        
        // Slow health regeneration
        if (tickCount % 40 == 0) {
            heal(1.0f);
        }
    }
    
    // ==================== Interaction ====================
    
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        
        if (isTamed()) {
            // TODO: Open trade GUI when implemented
            // player.openMenu(...)
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        
        return super.mobInteract(player, hand);
    }
    
    // ==================== Sounds ====================
    
    @Override
    public void playAmbientSound() {
        if (!level().isClientSide) {
            // Check for nearby pechs to "chat" with
            if (random.nextInt(3) == 0) {
                AABB searchBox = getBoundingBox().inflate(4.0, 2.0, 4.0);
                List<EntityPech> nearbyPechs = level().getEntitiesOfClass(EntityPech.class, searchBox, e -> e != this);
                if (!nearbyPechs.isEmpty()) {
                    level().broadcastEntityEvent(this, (byte) 17);
                    playSound(ModSounds.PECH_TRADE.get(), getSoundVolume(), getVoicePitch());
                    return;
                }
            }
            level().broadcastEntityEvent(this, (byte) 16);
        }
        super.playAmbientSound();
    }
    
    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }
    
    @Override
    protected float getSoundVolume() {
        return 0.4f;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.PECH_IDLE.get();
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PECH_HIT.get();
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PECH_DEATH.get();
    }
    
    // ==================== Status Events ====================
    
    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case 16 -> mumble = (float) Math.PI;
            case 17 -> mumble = (float) (Math.PI * 2);
            case 18 -> {
                // Happy particles (became tamed)
                for (int i = 0; i < 5; ++i) {
                    double dx = random.nextGaussian() * 0.02;
                    double dy = random.nextGaussian() * 0.02;
                    double dz = random.nextGaussian() * 0.02;
                    level().addParticle(ParticleTypes.HAPPY_VILLAGER,
                            getX() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(),
                            getY() + 0.5 + random.nextFloat() * getBbHeight(),
                            getZ() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(),
                            dx, dy, dz);
                }
            }
            case 19 -> {
                // Angry particles
                for (int i = 0; i < 5; ++i) {
                    double dx = random.nextGaussian() * 0.02;
                    double dy = random.nextGaussian() * 0.02;
                    double dz = random.nextGaussian() * 0.02;
                    level().addParticle(ParticleTypes.ANGRY_VILLAGER,
                            getX() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(),
                            getY() + 0.5 + random.nextFloat() * getBbHeight(),
                            getZ() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(),
                            dx, dy, dz);
                }
                mumble = (float) (Math.PI * 2);
            }
            default -> super.handleEntityEvent(id);
        }
    }
    
    // ==================== NBT ====================
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("PechType", (byte) getPechType());
        tag.putShort("Anger", (short) getAnger());
        tag.putBoolean("Tamed", isTamed());
        
        // Save loot inventory
        net.minecraft.world.ContainerHelper.saveAllItems(tag, loot, registryAccess());
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        
        if (tag.contains("PechType")) {
            setPechType(tag.getByte("PechType"));
        }
        setAnger(tag.getShort("Anger"));
        setTamed(tag.getBoolean("Tamed"));
        
        // Load loot inventory
        loot = NonNullList.withSize(9, ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, loot, registryAccess());
        
        setCombatTask();
    }
    
    // ==================== Despawn ====================
    
    @Override
    public boolean removeWhenFarAway(double distance) {
        // Don't despawn if carrying significant loot
        try {
            if (loot == null) return true;
            int count = 0;
            for (ItemStack stack : loot) {
                if (!stack.isEmpty()) {
                    ++count;
                }
            }
            return count < 5;
        } catch (Exception e) {
            return true;
        }
    }
    
    public boolean canBeLeashed(Player player) {
        return false;
    }
    
    // ==================== Trading ====================
    
    /**
     * Check if the Pech values this item for trading.
     * Items with high DESIRE aspect are valued.
     * 
     * @param item The item to check
     * @return true if the Pech will accept this for trading
     */
    public boolean isValued(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        
        // Check if it's in the valued items map
        if (valuedItems.containsKey(item.getItem())) {
            return true;
        }
        
        // Check if it has high DESIRE aspect
        // TODO: Use ThaumcraftCraftingManager.getObjectTags when aspect system is complete
        // For now, accept most non-common items
        return !item.getItem().builtInRegistryHolder().is(net.minecraft.tags.ItemTags.DIRT) &&
               item.getRarity() != net.minecraft.world.item.Rarity.COMMON;
    }
    
    /**
     * Get the trade value of an item.
     * Higher value = better trades.
     * 
     * @param item The item to evaluate
     * @return The trade value (0-32)
     */
    public int getValue(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return 0;
        }
        
        // Check valued items map first
        if (valuedItems.containsKey(item.getItem())) {
            return valuedItems.get(item.getItem());
        }
        
        // Base value on rarity
        return switch (item.getRarity()) {
            case UNCOMMON -> 4;
            case RARE -> 8;
            case EPIC -> 16;
            default -> 2;
        };
    }
    
    // Static trade data
    private static final java.util.HashMap<net.minecraft.world.item.Item, Integer> valuedItems = new java.util.HashMap<>();
    
    static {
        // Initialize valued items
        // TODO: Populate with proper valued items list
        valuedItems.put(net.minecraft.world.item.Items.GOLD_INGOT, 4);
        valuedItems.put(net.minecraft.world.item.Items.DIAMOND, 8);
        valuedItems.put(net.minecraft.world.item.Items.EMERALD, 6);
        valuedItems.put(net.minecraft.world.item.Items.LAPIS_LAZULI, 2);
        valuedItems.put(net.minecraft.world.item.Items.AMETHYST_SHARD, 3);
    }
}




