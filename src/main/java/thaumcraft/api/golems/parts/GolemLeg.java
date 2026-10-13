package thaumcraft.api.golems.parts;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.EnumGolemTrait;

/**
 * Defines a golem leg type (e.g., Walker, Roller, Climber, Flyer).
 * Legs determine movement style and capabilities.
 */
public class GolemLeg {

    protected static GolemLeg[] legs = new GolemLeg[1];
    private static byte lastID = 0;

    public byte id;
    public String key;
    public String[] research;
    public ResourceLocation icon;
    public Object[] components;
    public EnumGolemTrait[] traits;
    public ILegFunction function;
    public PartModel model;

    public GolemLeg(String key, String[] research, ResourceLocation icon, PartModel model,
                    Object[] components, EnumGolemTrait[] traits) {
        this.key = key;
        this.research = research;
        this.icon = icon;
        this.components = components;
        this.traits = traits;
        this.model = model;
        this.function = null;
    }

    public GolemLeg(String key, String[] research, ResourceLocation icon, PartModel model,
                    Object[] components, ILegFunction function, EnumGolemTrait[] traits) {
        this(key, research, icon, model, components, traits);
        this.function = function;
    }

    public static void register(GolemLeg leg) {
        leg.id = lastID;
        lastID++;
        if (leg.id >= legs.length) {
            GolemLeg[] temp = new GolemLeg[leg.id + 1];
            System.arraycopy(legs, 0, temp, 0, legs.length);
            legs = temp;
        }
        legs[leg.id] = leg;
    }

    public Component getLocalizedName() {
        return Component.translatable("golem.leg." + key.toLowerCase());
    }

    public Component getLocalizedDescription() {
        return Component.translatable("golem.leg.text." + key.toLowerCase());
    }

    public static GolemLeg[] getLegs() {
        return legs;
    }

    public static GolemLeg getById(int id) {
        if (id >= 0 && id < legs.length) {
            return legs[id];
        }
        return legs[0];
    }

    /**
     * Interface for leg-specific movement functions
     */
    public interface ILegFunction extends IGenericFunction {
    }
}




