package thaumcraft.api.golems.parts;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;

/**
 * Defines a golem arm type (e.g., Basic, Fine, Claws, Breakers, Darts).
 * Arms determine combat abilities and dexterity.
 */
public class GolemArm {

    protected static GolemArm[] arms = new GolemArm[1];
    private static byte lastID = 0;

    public byte id;
    public String key;
    public String[] research;
    public ResourceLocation icon;
    public Object[] components;
    public EnumGolemTrait[] traits;
    public IArmFunction function;
    public PartModel model;

    public GolemArm(String key, String[] research, ResourceLocation icon, PartModel model,
                    Object[] components, EnumGolemTrait[] traits) {
        this.key = key;
        this.research = research;
        this.icon = icon;
        this.components = components;
        this.traits = traits;
        this.model = model;
        this.function = null;
    }

    public GolemArm(String key, String[] research, ResourceLocation icon, PartModel model,
                    Object[] components, IArmFunction function, EnumGolemTrait[] traits) {
        this(key, research, icon, model, components, traits);
        this.function = function;
    }

    public static void register(GolemArm arm) {
        arm.id = lastID;
        lastID++;
        if (arm.id >= arms.length) {
            GolemArm[] temp = new GolemArm[arm.id + 1];
            System.arraycopy(arms, 0, temp, 0, arms.length);
            arms = temp;
        }
        arms[arm.id] = arm;
    }

    public Component getLocalizedName() {
        return Component.translatable("golem.arm." + key.toLowerCase());
    }

    public Component getLocalizedDescription() {
        return Component.translatable("golem.arm.text." + key.toLowerCase());
    }

    public static GolemArm[] getArms() {
        return arms;
    }

    public static GolemArm getById(int id) {
        if (id >= 0 && id < arms.length) {
            return arms[id];
        }
        return arms[0];
    }

    /**
     * Interface for arm-specific combat functions
     */
    public interface IArmFunction extends IGenericFunction {
        /**
         * Called when the golem performs a melee attack
         */
        void onMeleeAttack(IGolemAPI golem, Entity target);

        /**
         * Called when the golem performs a ranged attack
         */
        void onRangedAttack(IGolemAPI golem, LivingEntity target, float distanceFactor);

        /**
         * Get the ranged attack AI for this arm type
         */
        <T extends Mob & RangedAttackMob> RangedAttackGoal getRangedAttackAI(T mob);
    }
}




