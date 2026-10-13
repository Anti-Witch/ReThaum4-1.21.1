package thaumcraft.common.entities.construct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXBoreDig;
import thaumcraft.common.world.aura.AuraHandler;
import thaumcraft.init.ModSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Arcane Bore - Automated mining construct.
 * Mines blocks in a spiral pattern, deposits items into adjacent inventories.
 * Requires vis from the aura to operate.
 * 
 * Ported to 1.20.1
 */
@SuppressWarnings("deprecation")
public class EntityArcaneBore extends EntityOwnedConstruct {
    
    // Synced data
    private static final EntityDataAccessor<Direction> FACING = 
            SynchedEntityData.defineId(EntityArcaneBore.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> ACTIVE = 
            SynchedEntityData.defineId(EntityArcaneBore.class, EntityDataSerializers.BOOLEAN);
    
    // Mining state
    private BlockPos digTarget = null;
    private BlockPos digTargetPrev = null;
    private float digCost = 0.25f;
    private int digDelay = 0;
    private int digDelayMax = 0;
    private int breakCounter = 0;
    
    // Spiral pattern state
    public int spiral = 0;
    public float currentRadius = 0.0f;
    private float radInc = 1.0f;
    
    // Charge (vis storage)
    private float charge = 0.0f;
    
    // Sound delay
    private long soundDelay = 0L;
    
    // Client-side digging state
    public boolean clientDigging = false;
    
    // Drop collection for harvest events
    private static final HashMap<Integer, ArrayList<ItemStack>> drops = new HashMap<>();
    
    public EntityArcaneBore(EntityType<? extends EntityArcaneBore> type, Level level) {
        super(type, level);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return EntityOwnedConstruct.createAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FACING, Direction.DOWN);
        builder.define(ACTIVE, false);
    }
    
    // ==================== NBT ====================
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Charge", charge);
        tag.putByte("Facing", (byte) getFacing().ordinal());
        tag.putBoolean("Active", isActive());
        tag.putInt("Spiral", spiral);
        tag.putFloat("Radius", currentRadius);
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        charge = tag.getFloat("Charge");
        setFacing(Direction.values()[tag.getByte("Facing") % Direction.values().length]);
        setActive(tag.getBoolean("Active"));
        spiral = tag.getInt("Spiral");
        currentRadius = tag.getFloat("Radius");
    }
    
    // ==================== Tick ====================
    
    @Override
    public void tick() {
        super.tick();
        
        if (!level().isClientSide) {
            // Heal slowly
            if (tickCount % 50 == 0) {
                heal(1.0f);
            }
            
            // Recharge vis
            if (tickCount % 10 == 0 && charge < 10.0f) {
                rechargeVis();
            }
            
            // Check for redstone activation
            if (!isPassenger()) {
                setActive(level().hasNeighborSignal(blockPosition().below()));
            }
            
            // Update held item
            if (validInventory()) {
                // Item animation update would go here
            }
        }
        
        if (!isActive()) {
            digTarget = null;
        }
        
        // Server-side mining logic
        if (digTarget != null && charge >= digCost && !level().isClientSide) {
            if (digDelay-- <= 0 && dig()) {
                charge -= digCost;
                
                if (soundDelay < System.currentTimeMillis()) {
                    soundDelay = System.currentTimeMillis() + 1200L + random.nextInt(100);
                    if (ModSounds.RUMBLE != null) {
                        playSound(ModSounds.RUMBLE.get(), 0.25f, 0.9f + random.nextFloat() * 0.2f);
                    }
                }
            }
        }
        
        // Find next block to mine
        if (!level().isClientSide && digTarget == null && isActive() && validInventory()) {
            findNextBlockToDig();
            
            if (digTarget != null) {
                level().broadcastEntityEvent(this, (byte) 16);
                // Send digging packet to nearby players
                PacketHandler.sendToAllTrackingChunk(
                        new PacketFXBoreDig(digTarget, this, digDelayMax),
                        (ServerLevel) level(), digTarget);
            } else {
                level().broadcastEntityEvent(this, (byte) 17);
            }
        }
    }
    
    // ==================== Mining Logic ====================
    
    public boolean validInventory() {
        ItemStack held = getMainHandItem();
        if (held.isEmpty()) return false;
        
        boolean isPickaxe = held.getItem() instanceof PickaxeItem || held.is(ItemTags.PICKAXES);
        
        if (!isPickaxe) return false;
        
        // Check if tool is about to break
        return held.getDamageValue() + 1 < held.getMaxDamage();
    }
    
    public int getDigRadius() {
        ItemStack held = getMainHandItem();
        if (held.isEmpty()) return 2;
        
        int r = held.getItem().getEnchantmentValue() / 3;
        // TODO: Add infusion enchantment bonus
        return Math.max(2, r);
    }
    
    public int getDigDepth() {
        int r = getDigRadius() * 8;
        // TODO: Add burrowing enchantment bonus
        return r;
    }
    
    public int getFortune() {
        if (!validInventory()) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(
                this.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),
                getMainHandItem()
        );
    }
    
    public int getDigSpeed(BlockState blockState) {
        if (!validInventory()) return 0;
        ItemStack held = getMainHandItem();
        
        int speed = (int) (held.getDestroySpeed(blockState) / 2.0f);
        speed += EnchantmentHelper.getItemEnchantmentLevel(
                this.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY),
                held
        );
        return speed;
    }
    
    public boolean hasSilkTouch() {
        if (!validInventory()) return false;
        return EnchantmentHelper.getItemEnchantmentLevel(
                this.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
                getMainHandItem()
        ) > 0;
    }
    
    private boolean dig() {
        if (digTarget == null || level().isEmptyBlock(digTarget)) {
            digTarget = null;
            return false;
        }
        
        BlockState state = level().getBlockState(digTarget);
        if (state.isAir()) {
            digTarget = null;
            return false;
        }
        
        // Try to break the block
        if (level() instanceof ServerLevel serverLevel) {
            // Create fake player for block breaking
            FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(serverLevel);
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, getMainHandItem());
            
            // Break the block with dropBlock = true to spawn the items using player's tools (fortune, silk touch)
            boolean broken = level().destroyBlock(digTarget, true, fakePlayer);
            
            if (broken) {
                // Collect nearby item entities
                List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class,
                        new AABB(digTarget).inflate(1.5));
                for (ItemEntity item : items) {
                    ejectItem(item.getItem().copy());
                    item.discard();
                }
                
                // Damage tool
                breakCounter++;
                if (breakCounter >= 50) {
                    breakCounter -= 50;
                    ItemStack held = getMainHandItem();
                    held.hurtAndBreak(1, this, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                }
            }
        }
        
        digTarget = null;
        return true;
    }
    
    private void findNextBlockToDig() {
        if (digTargetPrev == null || distanceToSqr(Vec3.atCenterOf(digTargetPrev)) > (getDigRadius() + 1) * (getDigRadius() + 1)) {
            digTargetPrev = blockPosition();
        }
        
        int digRadius = getDigRadius();
        int digDepth = getDigDepth();
        
        // Calculate target position along facing direction
        BlockPos end = digTargetPrev.relative(getFacing(), digDepth);
        
        // Raycast to find first solid block
        Vec3 start = Vec3.atCenterOf(digTargetPrev);
        Vec3 endVec = Vec3.atCenterOf(end);
        
        BlockHitResult hit = level().clip(new ClipContext(
                start, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = hit.getBlockPos();
            BlockState state = level().getBlockState(hitPos);
            
            if (state.getDestroySpeed(level(), hitPos) > -1.0f) {
                digDelay = Math.max(1, (int)(state.getDestroySpeed(level(), hitPos) * 2.0f) - getDigSpeed(state) * 2);
                digDelayMax = digDelay;
                
                if (!hitPos.equals(blockPosition()) && !hitPos.equals(blockPosition().below())) {
                    digTarget = hitPos;
                    return;
                }
            }
        }
        
        // Spiral pattern for next target
        updateSpiralTarget(digRadius);
    }
    
    private void updateSpiralTarget(int digRadius) {
        spiral += (int)(3.0f + Math.max(0.0f, (10.0f - Math.abs(currentRadius)) * 2.0f));
        if (spiral >= 360) {
            spiral -= 360;
            currentRadius += radInc;
            if (Math.abs(currentRadius) > digRadius) {
                currentRadius = 0.0f;
            }
        }
        
        // Calculate spiral position
        double angle = spiral * Math.PI / 180.0;
        double offsetX = Math.cos(angle) * currentRadius;
        double offsetY = Math.sin(angle) * currentRadius;
        
        // Apply offset based on facing direction
        int x = (int) (getX() + offsetX * (1 - Math.abs(getFacing().getStepX())));
        int y = (int) (getY() + offsetY * (getFacing().getAxis() != Direction.Axis.Y ? 1 : 0));
        int z = (int) (getZ() + offsetX * (1 - Math.abs(getFacing().getStepZ())));
        
        digTargetPrev = new BlockPos(x, y, z);
    }
    
    private void ejectItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        
        // Try to insert into adjacent inventory
        for (Direction dir : Direction.values()) {
            BlockPos adjPos = blockPosition().relative(dir);
            // TODO: Check for inventory and insert
        }
        
        // Otherwise drop on ground
        ItemEntity item = new ItemEntity(level(), getX(), getY(), getZ(), stack);
        item.setDeltaMovement(
                getFacing().getOpposite().getStepX() * 0.2,
                0.2,
                getFacing().getOpposite().getStepZ() * 0.2);
        level().addFreshEntity(item);
    }
    
    private void rechargeVis() {
        float drained = AuraHandler.drainVis(level(), blockPosition(), 10.0f, false);
        charge += drained;
    }
    
    // ==================== Interaction ====================
    
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && isOwner(player) && isAlive()) {
            if (player.isShiftKeyDown()) {
                // Drop items and destroy
                if (ModSounds.ZAP != null) {
                    playSound(ModSounds.ZAP.get(), 1.0f, 1.0f);
                }
                
                // Drop held pickaxe
                if (!getMainHandItem().isEmpty()) {
                    spawnAtLocation(getMainHandItem(), 0.5f);
                }
                
                // Drop bore placer
                // TODO: Drop turret placer item
                
                discard();
                return InteractionResult.SUCCESS;
            } else {
                // Open GUI
                // TODO: Open bore GUI
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Owner can change facing by hitting
        if (source.getEntity() instanceof LivingEntity living && isOwner(living)) {
            Direction dir = Direction.orderedByNearest(this)[0];
            if (dir != Direction.DOWN) {
                setFacing(dir);
            }
            return false;
        }
        return super.hurt(source, amount);
    }
    
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            if (!getMainHandItem().isEmpty()) {
                spawnAtLocation(getMainHandItem(), 0.5f);
            }
        }
    }
    
    // ==================== Movement ====================
    
    @Override
    public void move(MoverType type, Vec3 motion) {
        // Reduce horizontal movement
        super.move(type, new Vec3(motion.x / 5.0, motion.y, motion.z / 5.0));
    }
    
    @Override
    public boolean isPushable() {
        return true;
    }
    
    @Override
    public boolean isPickable() {
        return true;
    }
    
    // ==================== Getters/Setters ====================
    
    public Direction getFacing() {
        return entityData.get(FACING);
    }
    
    public void setFacing(Direction facing) {
        entityData.set(FACING, facing);
    }
    
    public boolean isActive() {
        return entityData.get(ACTIVE);
    }
    
    public void setActive(boolean active) {
        entityData.set(ACTIVE, active);
    }
    
    public float getCharge() {
        return charge;
    }
    
    public void setCharge(float charge) {
        this.charge = charge;
    }
    
    public BlockPos getDigTarget() {
        return digTarget;
    }
    
    /**
     * Check if client is showing digging animation
     */
    public boolean isClientDigging() {
        return clientDigging;
    }
    
    /**
     * Check if the bore has a valid inventory (pickaxe with durability)
     */
    public boolean hasValidInventory() {
        return validInventory();
    }
    
    // ==================== Client Events ====================
    
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            clientDigging = true;
        } else if (id == 17) {
            clientDigging = false;
        } else {
            super.handleEntityEvent(id);
        }
    }
    
    // ==================== Team ====================
    

}




