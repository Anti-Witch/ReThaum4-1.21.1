package thaumcraft.api.golems.parts;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.EnumGolemTrait;

/**
 * Defines a golem addon type (e.g., None, Armored, Fighter, Hauler).
 * Addons provide additional capabilities and traits.
 */
public class GolemAddon {

    protected static GolemAddon[] addons = new GolemAddon[1];
    private static byte lastID = 0;

    public byte id;
    public String key;
    public String[] research;
    public ResourceLocation icon;
    public Object[] components;
    public EnumGolemTrait[] traits;
    public IAddonFunction function;
    public PartModel model;

    public GolemAddon(String key, String[] research, ResourceLocation icon, PartModel model,
                      Object[] components, EnumGolemTrait[] traits) {
        this.key = key;
        this.research = research;
        this.icon = icon;
        this.components = components;
        this.traits = traits;
        this.model = model;
        this.function = null;
    }

    public GolemAddon(String key, String[] research, ResourceLocation icon, PartModel model,
                      Object[] components, IAddonFunction function, EnumGolemTrait[] traits) {
        this(key, research, icon, model, components, traits);
        this.function = function;
    }

    public static void register(GolemAddon addon) {
        addon.id = lastID;
        lastID++;
        if (addon.id >= addons.length) {
            GolemAddon[] temp = new GolemAddon[addon.id + 1];
            System.arraycopy(addons, 0, temp, 0, addons.length);
            addons = temp;
        }
        addons[addon.id] = addon;
    }

    public Component getLocalizedName() {
        return Component.translatable("golem.addon." + key.toLowerCase());
    }

    public Component getLocalizedDescription() {
        return Component.translatable("golem.addon.text." + key.toLowerCase());
    }

    public static GolemAddon[] getAddons() {
        return addons;
    }

    public static GolemAddon getById(int id) {
        if (id >= 0 && id < addons.length) {
            return addons[id];
        }
        return addons[0];
    }

    /**
     * Interface for addon-specific functions
     */
    public interface IAddonFunction extends IGenericFunction {
    }
}




