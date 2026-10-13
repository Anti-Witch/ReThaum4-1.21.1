package thaumcraft.api.golems.parts;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.EnumGolemTrait;

/**
 * Defines a golem material type (e.g., Wood, Iron, Clay, Brass, Thaumium, Void).
 * Materials determine base health, armor, damage, and traits.
 */
public class GolemMaterial {

    protected static GolemMaterial[] materials = new GolemMaterial[1];
    private static byte lastID = 0;

    public byte id;
    public String key;
    public String[] research;
    public ResourceLocation texture;
    public int itemColor;
    public int healthMod;
    public int armor;
    public int damage;
    public ItemStack componentBase;
    public ItemStack componentMechanism;
    public EnumGolemTrait[] traits;

    public GolemMaterial(String key, String[] research, ResourceLocation texture, int itemColor,
                         int hp, int armor, int damage, ItemStack compBase, ItemStack compMech,
                         EnumGolemTrait[] traits) {
        this.key = key;
        this.research = research;
        this.texture = texture;
        this.itemColor = itemColor;
        this.componentBase = compBase;
        this.componentMechanism = compMech;
        this.healthMod = hp;
        this.armor = armor;
        this.traits = traits;
        this.damage = damage;
    }

    public static void register(GolemMaterial material) {
        material.id = lastID;
        lastID++;
        if (material.id >= materials.length) {
            GolemMaterial[] temp = new GolemMaterial[material.id + 1];
            System.arraycopy(materials, 0, temp, 0, materials.length);
            materials = temp;
        }
        materials[material.id] = material;
    }

    public Component getLocalizedName() {
        return Component.translatable("golem.material." + key.toLowerCase());
    }

    public Component getLocalizedDescription() {
        return Component.translatable("golem.material.text." + key.toLowerCase());
    }

    public static GolemMaterial[] getMaterials() {
        return materials;
    }

    public static GolemMaterial getById(int id) {
        if (id >= 0 && id < materials.length) {
            return materials[id];
        }
        return materials[0]; // Default to first material
    }
}




