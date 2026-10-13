package thaumcraft.common.golems;

import net.minecraft.world.item.enchantment.EnchantmentHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import thaumcraft.init.ModSounds;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;
import thaumcraft.common.golems.ai.AIGotoBlock;
import thaumcraft.common.golems.ai.AIGotoEntity;
import thaumcraft.common.golems.ai.AIGotoHome;
import thaumcraft.common.golems.ai.AIOwnerHurtByTarget;
import thaumcraft.common.golems.ai.AIOwnerHurtTarget;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.lib.utils.NBTHelper;
import thaumcraft.init.ModItems;

import java.nio.ByteBuffer;

/**
 * EntityThaumcraftGolem - The main golem entity.
 *
 * Features:
 * - Modular construction from Material, Head, Arms, Legs, and Addon
 * - 19 traits affecting behavior
 * - Task-based AI driven by seals
 * - XP/Rank system for smart golems
 * - Color matching with seals
 * - Inventory for carrying items (1-2 slots based on HAULER trait)
 */
public class EntityThaumcraftGolem extends EntityOwnedConstruct implements IGolemAPI, RangedAttackMob {

    // Synched data for properties (packed into 2 ints = long)
    private static final EntityDataAccessor<Integer> DATA_PROPS1 =
            SynchedEntityData.defineId(EntityThaumcraftGolem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PROPS2 =
            SynchedEntityData.defineId(EntityThaumcraftGolem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PROPS3 =
            SynchedEntityData.defineId(EntityThaumcraftGolem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_CLIMBING =
            SynchedEntityData.defineId(EntityThaumcraftGolem.class, EntityDataSerializers.BYTE);

    // Flags stored in PROPS3 byte 1
    private static final byte FLAG_FOLLOWING = 0x02;
    private static final byte FLAG_COMBAT = 0x08;

    // XP multiplier for ranking up
    public static final int XP_MULTIPLIER = 1000;

    int rankXp = 0;
    public boolean redrawParts = false;
    private boolean firstRun = true;
    protected Task task = null;
    protected int taskID = Integer.MAX_VALUE;

    public EntityThaumcraftGolem(EntityType<? extends EntityThaumcraftGolem> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    // Convenience constructor used by the golem placer item.
    public EntityThaumcraftGolem(Level level) {
        this(thaumcraft.init.ModEntities.THAUMCRAFT_GOLEM.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROPS1, 0);
        builder.define(DATA_PROPS2, 0);
        builder.define(DATA_PROPS3, 0);
        builder.define(DATA_CLIMBING, (byte) 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 0.6);
    }

    @Override
    protected void registerGoals() {
        rebuildAI();
    }

    // ==================== Properties ====================

    @Override
    public IGolemProperties getProperties() {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putInt(this.entityData.get(DATA_PROPS1));
        bb.putInt(this.entityData.get(DATA_PROPS2));
        return GolemProperties.fromLong(bb.getLong(0));
    }

    @Override
    public void setProperties(IGolemProperties prop) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(prop.toLong());
        bb.rewind();
        this.entityData.set(DATA_PROPS1, bb.getInt());
        this.entityData.set(DATA_PROPS2, bb.getInt());
    }

    @Override
    public byte getGolemColor() {
        int props3 = this.entityData.get(DATA_PROPS3);
        return (byte) (props3 & 0xFF);
    }

    public void setGolemColor(byte color) {
        int props3 = this.entityData.get(DATA_PROPS3);
        props3 = (props3 & 0xFFFFFF00) | (color & 0xFF);
        this.entityData.set(DATA_PROPS3, props3);
    }

    public byte getFlags() {
        int props3 = this.entityData.get(DATA_PROPS3);
        return (byte) ((props3 >> 8) & 0xFF);
    }

    public void setFlags(byte flags) {
        int props3 = this.entityData.get(DATA_PROPS3);
        props3 = (props3 & 0xFFFF00FF) | ((flags & 0xFF) << 8);
        this.entityData.set(DATA_PROPS3, props3);
    }

    // ==================== Attribute Updates ====================

    private void updateEntityAttributes() {
        IGolemProperties props = getProperties();

        // Health
        int maxHealth = 10 + props.getMaterial().healthMod;
        if (props.hasTrait(EnumGolemTrait.FRAGILE)) {
            maxHealth = (int) (maxHealth * 0.75);
        }
        maxHealth += props.getRank();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);

        // Step height (1.21.1: STEP_HEIGHT attribute, not a runtime setter)
        getAttribute(Attributes.STEP_HEIGHT).setBaseValue(props.hasTrait(EnumGolemTrait.WHEELED) ? 0.5f : 0.6f);

        // Home distance
        int homeRange = props.hasTrait(EnumGolemTrait.SCOUT) ? 48 : 32;
        restrictTo(getRestrictCenter().equals(BlockPos.ZERO) ? blockPosition() : getRestrictCenter(), homeRange);

        // Follow range
        double followRange = props.hasTrait(EnumGolemTrait.SCOUT) ? 56.0 : 40.0;
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(followRange);

        // Navigation
        this.navigation = createGolemNavigation();

        // Flying movement control
        if (props.hasTrait(EnumGolemTrait.FLYER)) {
            this.moveControl = new GolemFlyingMoveControl(this);
        }

        // Attack damage
        if (props.hasTrait(EnumGolemTrait.FIGHTER)) {
            double damage = props.getMaterial().damage;
            if (props.hasTrait(EnumGolemTrait.BRUTAL)) {
                damage = Math.max(damage * 1.5, damage + 1.0);
            }
            damage += props.getRank() * 0.25;
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        } else {
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
        }

        rebuildAI();
    }

    private void rebuildAI() {
        this.goalSelector.removeAllGoals(g -> true);
        this.targetSelector.removeAllGoals(g -> true);

        IGolemProperties props = getProperties();

        // Swimming for non-flyers
        if (!(this.navigation instanceof FlyingPathNavigation)) {
            this.goalSelector.addGoal(0, new FloatGoal(this));
        }

        if (isFollowingOwner()) {
            // Follow owner mode
            this.goalSelector.addGoal(4, new GolemFollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        } else {
            // Work mode - Task-based AI
            this.goalSelector.addGoal(3, new AIGotoBlock(this));
            this.goalSelector.addGoal(3, new AIGotoEntity(this));
            this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.5));
            this.goalSelector.addGoal(6, new AIGotoHome(this));
        }

        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        if (props.hasTrait(EnumGolemTrait.FIGHTER)) {
            if (props.hasTrait(EnumGolemTrait.RANGED) && props.getArms().function != null) {
                RangedAttackGoal rangedAI = props.getArms().function.getRangedAttackAI(this);
                if (rangedAI != null) {
                    this.goalSelector.addGoal(1, rangedAI);
                }
            }

            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));

            if (isFollowingOwner()) {
                // Attack what owner attacks and what attacks owner
                this.targetSelector.addGoal(1, new AIOwnerHurtByTarget(this));
                this.targetSelector.addGoal(2, new AIOwnerHurtTarget(this));
            }

            this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        }
    }

    private PathNavigation createGolemNavigation() {
        IGolemProperties props = getProperties();
        if (props.hasTrait(EnumGolemTrait.FLYER)) {
            return new FlyingPathNavigation(this, level());
        } else if (props.hasTrait(EnumGolemTrait.CLIMBER)) {
            return new WallClimberNavigation(this, level());
        } else {
            return new GroundPathNavigation(this, level());
        }
    }

    // ==================== Update ====================

    @Override
    public void tick() {
        super.tick();

        if (getProperties().hasTrait(EnumGolemTrait.FLYER)) {
            setNoGravity(true);
        }

        if (!level().isClientSide) {
            if (firstRun) {
                firstRun = false;
                if (hasRestriction() && !blockPosition().equals(getRestrictCenter())) {
                    teleportToHome();
                }
            }

            // Clear suspended tasks
            if (task != null && task.isSuspended()) {
                task = null;
            }

            // Clear dead targets
            if (getTarget() != null && getTarget().isDeadOrDying()) {
                setTarget(null);
            }

            // Ranged target distance check
            if (getTarget() != null && getProperties().hasTrait(EnumGolemTrait.RANGED) && distanceToSqr(getTarget()) > 1024.0) {
                setTarget(null);
            }

            // Health regeneration
            int healInterval = getProperties().hasTrait(EnumGolemTrait.REPAIR) ? 40 : 100;
            if (tickCount % healInterval == 0) {
                heal(1.0f);
            }

            // Climbing detection
            if (getProperties().hasTrait(EnumGolemTrait.CLIMBER)) {
                setBesideClimbableBlock(horizontalCollision);
            }
        } else {
            // Client-side part redraw
            if (tickCount < 20 || tickCount % 20 == 0) {
                redrawParts = true;
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
    }

    // ==================== Movement ====================

    public float getGolemMoveSpeed() {
        IGolemProperties props = getProperties();
        float speed = 1.0f + props.getRank() * 0.025f;
        if (props.hasTrait(EnumGolemTrait.LIGHT)) speed += 0.2f;
        if (props.hasTrait(EnumGolemTrait.HEAVY)) speed -= 0.175f;
        if (props.hasTrait(EnumGolemTrait.FLYER)) speed -= 0.33f;
        if (props.hasTrait(EnumGolemTrait.WHEELED)) speed += 0.25f;
        return speed;
    }

    @Override
    public boolean onClimbable() {
        return isBesideClimbableBlock();
    }

    public boolean isBesideClimbableBlock() {
        return (this.entityData.get(DATA_CLIMBING) & 0x01) != 0;
    }

    public void setBesideClimbableBlock(boolean climbing) {
        byte b = this.entityData.get(DATA_CLIMBING);
        if (climbing) {
            b |= 0x01;
        } else {
            b &= 0xFE;
        }
        this.entityData.set(DATA_CLIMBING, b);
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (getProperties().hasTrait(EnumGolemTrait.HEAVY) && !getProperties().hasTrait(EnumGolemTrait.FLYER)) {
            super.playStepSound(pos, state);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        if (getProperties().hasTrait(EnumGolemTrait.FLYER) || getProperties().hasTrait(EnumGolemTrait.CLIMBER)) {
            return false;
        }
        return super.causeFallDamage(distance, multiplier, source);
    }

    private void teleportToHome() {
        if (!hasRestriction()) return;

        BlockPos home = getRestrictCenter();
        double oldX = getX();
        double oldY = getY();
        double oldZ = getZ();

        setPos(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);

        // Find valid position above
        BlockPos checkPos = blockPosition();
        while (checkPos.getY() < level().getMaxBuildHeight()) {
            if (level().getBlockState(checkPos.above()).blocksMotion()) {
                break;
            }
            setPos(getX(), getY() + 1, getZ());
            checkPos = checkPos.above();
        }

        // Check if position is valid
        if (level().noCollision(this)) {
            getNavigation().stop();
        } else {
            setPos(oldX, oldY, oldZ);
        }
    }

    // ==================== Damage ====================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && getProperties().hasTrait(EnumGolemTrait.FIREPROOF)) {
            return false;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && getProperties().hasTrait(EnumGolemTrait.BLASTPROOF)) {
            amount = Math.min(getMaxHealth() / 2.0f, amount * 0.3f);
        }
        if (source == damageSources().cactus()) {
            return false;
        }
        if (hasRestriction() && (source == damageSources().inWall() || source == damageSources().fellOutOfWorld())) {
            teleportToHome();
        }
        return super.hurt(source, amount);
    }

    @Override
    public int getArmorValue() {
        int armor = getProperties().getMaterial().armor;
        if (getProperties().hasTrait(EnumGolemTrait.ARMORED)) {
            armor = (int) Math.max(armor * 1.5, armor + 1);
        }
        if (getProperties().hasTrait(EnumGolemTrait.FRAGILE)) {
            armor = (int) (armor * 0.75);
        }
        return armor;
    }

    // ==================== Combat ====================

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target);
        setInCombat(getTarget() != null);
    }

    @Override
    public boolean isInCombat() {
        return (getFlags() & FLAG_COMBAT) != 0;
    }

    public void setInCombat(boolean combat) {
        byte flags = getFlags();
        if (combat) {
            flags |= FLAG_COMBAT;
        } else {
            flags &= ~FLAG_COMBAT;
        }
        setFlags(flags);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float damage = (float) getAttribute(Attributes.ATTACK_DAMAGE).getValue();

        boolean hit = target.hurt(damageSources().mobAttack(this), damage);
        if (hit) {
            if (target instanceof LivingEntity living && getProperties().hasTrait(EnumGolemTrait.DEFT)) {
                living.setLastHurtByMob(this);
            }

            // Apply enchantments
            if (this.level() instanceof ServerLevel serverLevel) {
                EnchantmentHelper.doPostAttackEffects(serverLevel, this, this.damageSources().mobAttack(this));
            }

            // Call arm function
            if (getProperties().getArms().function != null) {
                getProperties().getArms().function.onMeleeAttack(this, target);
            }

            // XP for kills
            if (target instanceof Mob && !target.isAlive()) {
                addRankXp(8);
            }
        }
        return hit;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (getProperties().getArms().function != null) {
            getProperties().getArms().function.onRangedAttack(this, target, distanceFactor);
        }
    }

    // ==================== Following ====================

    public boolean isFollowingOwner() {
        return (getFlags() & FLAG_FOLLOWING) != 0;
    }

    public void setFollowingOwner(boolean following) {
        byte flags = getFlags();
        boolean wasFollowing = (flags & FLAG_FOLLOWING) != 0;
        if (following) {
            flags |= FLAG_FOLLOWING;
        } else {
            flags &= ~FLAG_FOLLOWING;
        }
        setFlags(flags);

        // Rebuild AI if follow state changed (server-side only)
        if (!level().isClientSide && wasFollowing != following) {
            // Clear current task when switching modes
            if (task != null) {
                task.setReserved(false);
                task = null;
            }
            rebuildAI();
        }
    }

    // ==================== Tasks ====================

    public Task getTask() {
        if (task == null && taskID != Integer.MAX_VALUE) {
            task = TaskHandler.getTask(level().dimension(), taskID);
            taskID = Integer.MAX_VALUE;
        }
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    // ==================== XP/Rank ====================

    public int getRankXp() {
        return rankXp;
    }

    public void setRankXp(int xp) {
        this.rankXp = xp;
    }

    @Override
    public void addRankXp(int xp) {
        if (!getProperties().hasTrait(EnumGolemTrait.SMART) || level().isClientSide) {
            return;
        }

        int rank = getProperties().getRank();
        if (rank < 10) {
            rankXp += xp;
            int xpNeeded = (rank + 1) * (rank + 1) * XP_MULTIPLIER;
            if (rankXp >= xpNeeded) {
                rankXp -= xpNeeded;
                IGolemProperties props = getProperties();
                props.setRank(rank + 1);
                setProperties(props);
                playSound(SoundEvents.PLAYER_LEVELUP, 0.25f, 1.0f);
                level().broadcastEntityEvent(this, (byte) 9);
            }
        }
    }

    // ==================== Inventory ====================

    @Override
    public ItemStack holdItem(ItemStack stack) {
        if (stack.isEmpty()) return stack;

        int slots = getProperties().hasTrait(EnumGolemTrait.HAULER) ? 2 : 1;
        for (int i = 0; i < slots; i++) {
            EquipmentSlot slot = EquipmentSlot.values()[i];
            ItemStack current = getItemBySlot(slot);

            if (current.isEmpty()) {
                setItemSlot(slot, stack.copy());
                return ItemStack.EMPTY;
            }

            if (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() < current.getMaxStackSize()) {
                int space = current.getMaxStackSize() - current.getCount();
                int toAdd = Math.min(stack.getCount(), space);
                current.grow(toAdd);
                stack.shrink(toAdd);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    @Override
    public ItemStack dropItem(ItemStack stack) {
        int slots = getProperties().hasTrait(EnumGolemTrait.HAULER) ? 2 : 1;
        ItemStack result = ItemStack.EMPTY;

        for (int i = 0; i < slots; i++) {
            EquipmentSlot slot = EquipmentSlot.values()[i];
            ItemStack current = getItemBySlot(slot);

            if (!current.isEmpty()) {
                if (stack.isEmpty()) {
                    result = current.copy();
                    setItemSlot(slot, ItemStack.EMPTY);
                    break;
                } else if (ItemStack.isSameItemSameComponents(current, stack)) {
                    int toDrop = Math.min(stack.getCount(), current.getCount());
                    result = current.copy();
                    result.setCount(toDrop);
                    current.shrink(toDrop);
                    if (current.isEmpty()) {
                        setItemSlot(slot, ItemStack.EMPTY);
                    }
                    break;
                }
            }
        }

        // Shift items if hauler
        if (getProperties().hasTrait(EnumGolemTrait.HAULER)) {
            ItemStack first = getItemBySlot(EquipmentSlot.values()[0]);
            ItemStack second = getItemBySlot(EquipmentSlot.values()[1]);
            if (first.isEmpty() && !second.isEmpty()) {
                setItemSlot(EquipmentSlot.values()[0], second.copy());
                setItemSlot(EquipmentSlot.values()[1], ItemStack.EMPTY);
            }
        }

        return result;
    }

    @Override
    public int canCarryAmount(ItemStack stack) {
        int total = 0;
        int slots = getProperties().hasTrait(EnumGolemTrait.HAULER) ? 2 : 1;

        for (int i = 0; i < slots; i++) {
            ItemStack current = getItemBySlot(EquipmentSlot.values()[i]);
            if (current.isEmpty()) {
                total += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(current, stack)) {
                total += stack.getMaxStackSize() - current.getCount();
            }
        }
        return total;
    }

    @Override
    public boolean canCarry(ItemStack stack, boolean partial) {
        int amount = canCarryAmount(stack);
        if (amount > 0) {
            return partial || amount >= stack.getCount();
        }
        return false;
    }

    @Override
    public boolean isCarrying(ItemStack stack) {
        if (stack.isEmpty()) return false;

        int slots = getProperties().hasTrait(EnumGolemTrait.HAULER) ? 2 : 1;
        for (int i = 0; i < slots; i++) {
            ItemStack current = getItemBySlot(EquipmentSlot.values()[i]);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NonNullList<ItemStack> getCarrying() {
        int slots = getProperties().hasTrait(EnumGolemTrait.HAULER) ? 2 : 1;
        NonNullList<ItemStack> list = NonNullList.withSize(slots, ItemStack.EMPTY);
        for (int i = 0; i < slots; i++) {
            list.set(i, getItemBySlot(EquipmentSlot.values()[i]));
        }
        return list;
    }

    // ==================== IGolemAPI ====================

    @Override
    public LivingEntity getGolemEntity() {
        return this;
    }

    @Override
    public Level getGolemWorld() {
        return level();
    }

    @Override
    public void swingArm() {
        if (!swinging || swingTime >= 3 || swingTime < 0) {
            swingTime = -1;
            swinging = true;
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.broadcastEntityEvent(this, (byte) 4);
            }
        }
    }

    // ==================== Interaction ====================

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isRemoved()) return InteractionResult.PASS;
        if (player.getItemInHand(hand).getItem() == Items.NAME_TAG) return InteractionResult.PASS;

        if (!level().isClientSide && isOwner(player)) {
            ItemStack heldItem = player.getItemInHand(hand);

            if (player.isShiftKeyDown()) {
                // Pick up golem
                playSound(ModSounds.ZAP.get(), 1.0f, 1.0f);
                if (task != null) {
                    task.setReserved(false);
                }
                dropCarried();

                // Create golem placer item with saved data
                ItemStack placer = new ItemStack(ModItems.GOLEM_PLACER.get());
                NBTHelper.putLong(placer, "props", getProperties().toLong());
                NBTHelper.putInt(placer, "xp", rankXp);
                spawnAtLocation(placer, 0.5f);

                discard();
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }

            // Dye coloring
            if (heldItem.getItem() instanceof DyeItem dyeItem) {
                setGolemColor((byte) (16 - dyeItem.getDyeColor().getId()));
                heldItem.shrink(1);
                playSound(ModSounds.ZAP.get(), 1.0f, 1.5f);
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }

            // Handle golem bell for follow/stay toggle
            if (heldItem.getItem() == ModItems.GOLEM_BELL.get()) {
                setFollowingOwner(!isFollowingOwner());
                playSound(ModSounds.SCAN.get(), 1.0f, 1.0f);
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void dropCarried() {
        for (ItemStack stack : getCarrying()) {
            if (!stack.isEmpty()) {
                spawnAtLocation(stack, 0.25f);
            }
        }
        // Clear inventory
        int slots = getProperties().hasTrait(EnumGolemTrait.HAULER) ? 2 : 1;
        for (int i = 0; i < slots; i++) {
            setItemSlot(EquipmentSlot.values()[i], ItemStack.EMPTY);
        }
    }

    // ==================== Death ====================

    @Override
    public void die(DamageSource source) {
        if (task != null) {
            task.setReserved(false);
        }
        super.die(source);
        if (!level().isClientSide) {
            dropCarried();
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        float bonus = 0 * 0.15f;

        for (ItemStack stack : getProperties().generateComponents()) {
            ItemStack drop = stack.copy();
            if (random.nextFloat() < 0.3f + bonus) {
                if (drop.getCount() > 1) {
                    drop.shrink(random.nextInt(drop.getCount()));
                }
                spawnAtLocation(drop, 0.25f);
            }
        }
    }

    // ==================== NBT ====================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("props", getProperties().toLong());
        tag.putLong("homepos", getRestrictCenter().asLong());
        tag.putByte("gflags", getFlags());
        tag.putInt("rankXP", rankXp);
        tag.putByte("color", getGolemColor());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setProperties(GolemProperties.fromLong(tag.getLong("props")));
        restrictTo(BlockPos.of(tag.getLong("homepos")), 32);
        setFlags(tag.getByte("gflags"));
        rankXp = tag.getInt("rankXP");
        setGolemColor(tag.getByte("color"));
        updateEntityAttributes();
    }

    // ==================== Inner Classes ====================

    /**
     * Custom flying movement control for flying golems
     */
    private class GolemFlyingMoveControl extends MoveControl {
        public GolemFlyingMoveControl(Mob mob) {
            super(mob);
        }

        @Override
        public void tick() {
            if (operation == Operation.MOVE_TO) {
                double dx = wantedX - mob.getX();
                double dy = wantedY - mob.getY();
                double dz = wantedZ - mob.getZ();
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (distance < mob.getBoundingBox().getSize()) {
                    operation = Operation.WAIT;
                    Vec3 motion = mob.getDeltaMovement();
                    mob.setDeltaMovement(motion.multiply(0.5, 0.5, 0.5));
                } else {
                    Vec3 motion = mob.getDeltaMovement();
                    mob.setDeltaMovement(
                        motion.x + dx / distance * 0.033 * speedModifier,
                        motion.y + dy / distance * 0.0125 * speedModifier,
                        motion.z + dz / distance * 0.033 * speedModifier
                    );

                    if (mob.getTarget() == null) {
                        Vec3 m = mob.getDeltaMovement();
                        mob.setYRot((float) (-Mth.atan2(m.x, m.z) * Mth.RAD_TO_DEG));
                        mob.yBodyRot = mob.getYRot();
                    } else {
                        double tx = mob.getTarget().getX() - mob.getX();
                        double tz = mob.getTarget().getZ() - mob.getZ();
                        mob.setYRot((float) (-Mth.atan2(tx, tz) * Mth.RAD_TO_DEG));
                        mob.yBodyRot = mob.getYRot();
                    }
                }
            }
        }
    }

    /**
     * Simple follow owner goal
     */
    private class GolemFollowOwnerGoal extends Goal {
        private final EntityThaumcraftGolem golem;
        private LivingEntity owner;
        private final double speedModifier;
        private final float stopDistance;
        private final float startDistance;
        private int timeToRecalcPath;

        public GolemFollowOwnerGoal(EntityThaumcraftGolem golem, double speed, float startDist, float stopDist) {
            this.golem = golem;
            this.speedModifier = speed;
            this.startDistance = startDist;
            this.stopDistance = stopDist;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = golem.getOwner();
            if (owner == null || owner.isSpectator()) return false;
            if (golem.distanceToSqr(owner) < startDistance * startDistance) return false;
            this.owner = owner;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (golem.getNavigation().isDone()) return false;
            return golem.distanceToSqr(owner) > stopDistance * stopDistance;
        }

        @Override
        public void start() {
            timeToRecalcPath = 0;
        }

        @Override
        public void stop() {
            owner = null;
            golem.getNavigation().stop();
        }

        @Override
        public void tick() {
            golem.getLookControl().setLookAt(owner, 10.0f, golem.getMaxHeadXRot());
            if (--timeToRecalcPath <= 0) {
                timeToRecalcPath = 10;
                golem.getNavigation().moveTo(owner, speedModifier);
            }
        }
    }
}
