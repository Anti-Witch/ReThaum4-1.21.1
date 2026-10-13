package thaumcraft.api.golems.parts;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.EnumGolemTrait;

/**
 * Defines a golem head type (e.g., Basic, Smart, Scout).
 * Heads can provide intelligence traits and special behaviors.
 */
public class GolemHead {

    protected static GolemHead[] heads = new GolemHead[1];
    private static byte lastID = 0;

    public byte id;
    public String key;
    public String[] research;
    public ResourceLocation icon;
    public Object[] components;
    public EnumGolemTrait[] traits;
    public IHeadFunction function;
    public PartModel model;

    public GolemHead(String key, String[] research, ResourceLocation icon, PartModel model,
                     Object[] components, EnumGolemTrait[] traits) {
        this.key = key;
        this.research = research;
        this.icon = icon;
        this.components = components;
        this.traits = traits;
        this.model = model;
        this.function = null;
    }

    public GolemHead(String key, String[] research, ResourceLocation icon, PartModel model,
                     Object[] components, IHeadFunction function, EnumGolemTrait[] traits) {
        this(key, research, icon, model, components, traits);
        this.function = function;
    }

    public static void register(GolemHead head) {
        head.id = lastID;
        lastID++;
        if (head.id >= heads.length) {
            GolemHead[] temp = new GolemHead[head.id + 1];
            System.arraycopy(heads, 0, temp, 0, heads.length);
            heads = temp;
        }
        heads[head.id] = head;
    }

    public Component getLocalizedName() {
        return Component.translatable("golem.head." + key.toLowerCase());
    }

    public Component getLocalizedDescription() {
        return Component.translatable("golem.head.text." + key.toLowerCase());
    }

    public static GolemHead[] getHeads() {
        return heads;
    }

    public static GolemHead getById(int id) {
        if (id >= 0 && id < heads.length) {
            return heads[id];
        }
        return heads[0];
    }

    /**
     * Interface for head-specific behavior functions
     */
    public interface IHeadFunction extends IGenericFunction {
    }
}




