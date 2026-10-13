package thaumcraft.common.entities.monster.cult;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import java.util.Optional;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.init.ModEntities;

/**
 * EntityCultistKnight - A heavily armored melee fighter of the Crimson Cult.
 * Wears crimson plate armor and carries a sword.
 */
public class EntityCultistKnight extends EntityCultist {
    
    public EntityCultistKnight(EntityType<? extends EntityCultistKnight> type, Level level) {
        super(type, level);
    }
    
    public EntityCultistKnight(Level level) {
        this(ModEntities.CULTIST_KNIGHT.get(), level);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, EntityCultist.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, EntityEldritchGuardian.class, true));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ARMOR, 8.0);
    }
    
    @Override
    protected void setLoot(DifficultyInstance difficulty) {
        // TODO: Use actual crimson plate armor when implemented
        // setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CRIMSON_PLATE_HELM.get()));
        // setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CRIMSON_PLATE_CHEST.get()));
        // setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CRIMSON_PLATE_LEGS.get()));
        // setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CRIMSON_BOOTS.get()));
        
        // Use iron armor as placeholder
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        
        // Chance for special sword on hard difficulty
        float swordChance = (level().getDifficulty() == Difficulty.HARD) ? 0.05f : 0.01f;
        if (random.nextFloat() < swordChance) {
            int roll = random.nextInt(5);
            if (roll == 0) {
                // TODO: Use void sword when implemented
                // setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.VOID_SWORD.get()));
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
            } else {
                // TODO: Use thaumium sword when implemented
                // setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.THAUMIUM_SWORD.get()));
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            }
        } else {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
    }
    
    @Override
    protected void setEnchantmentBasedOnDifficulty(DifficultyInstance difficulty) {
        float f = difficulty.getSpecialMultiplier();
        ItemStack weapon = getMainHandItem();
        if (!weapon.isEmpty() && random.nextFloat() < 0.25f * f) {
            EnchantmentHelper.enchantItem(random, weapon, (int)(5.0f + f * random.nextInt(18)), level().registryAccess(), Optional.empty());
        }
    }
}




