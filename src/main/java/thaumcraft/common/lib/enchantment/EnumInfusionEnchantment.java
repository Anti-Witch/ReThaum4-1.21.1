package thaumcraft.common.lib.enchantment;
import thaumcraft.common.lib.utils.NBTHelper;

import com.google.common.collect.ImmutableSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * EnumInfusionEnchantment - Special infusion enchantments for Thaumcraft tools/armor.
 * 
 * These are separate from vanilla enchantments and stored in a custom NBT tag "infench".
 * 
 * Ported from Thaumcraft 1.12.2
 */
public enum EnumInfusionEnchantment {
    // Tool enchantments
    COLLECTOR(ImmutableSet.of("axe", "pickaxe", "shovel", "weapon"), 1, "INFUSIONENCHANTMENT"),
    DESTRUCTIVE(ImmutableSet.of("axe", "pickaxe", "shovel"), 1, "INFUSIONENCHANTMENT"),
    BURROWING(ImmutableSet.of("axe", "pickaxe"), 1, "INFUSIONENCHANTMENT"),
    SOUNDING(ImmutableSet.of("pickaxe"), 4, "INFUSIONENCHANTMENT"),
    REFINING(ImmutableSet.of("pickaxe"), 4, "INFUSIONENCHANTMENT"),
    ARCING(ImmutableSet.of("weapon"), 4, "INFUSIONENCHANTMENT"),
    ESSENCE(ImmutableSet.of("weapon"), 5, "INFUSIONENCHANTMENT"),
    
    // Caster enchantments
    VISBATTERY(ImmutableSet.of("chargable"), 3, "?"),
    VISCHARGE(ImmutableSet.of("chargable"), 1, "?"),
    
    // Armor enchantments
    SWIFT(ImmutableSet.of("boots"), 4, "IEARMOR"),
    AGILE(ImmutableSet.of("legs"), 1, "IEARMOR"),
    INFESTED(ImmutableSet.of("chest"), 1, "IETAINT"),
    
    // Mining enchantment
    LAMPLIGHT(ImmutableSet.of("axe", "pickaxe", "shovel"), 1, "INFUSIONENCHANTMENT");

    public static final String NBT_KEY = "infench";
    
    public final Set<String> toolClasses;
    public final int maxLevel;
    public final String research;

    EnumInfusionEnchantment(Set<String> toolClasses, int maxLevel, String research) {
        this.toolClasses = toolClasses;
        this.maxLevel = maxLevel;
        this.research = research;
    }

    /**
     * Get the infusion enchantment tag list from an item stack.
     * 
     * @param stack The item stack
     * @return The ListTag containing infusion enchantments, or null if none
     */
    public static ListTag getInfusionEnchantmentTagList(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !NBTHelper.hasTag(stack)) {
            return null;
        }
        CompoundTag tag = NBTHelper.getTag(stack);
        if (tag == null || !tag.contains(NBT_KEY, Tag.TAG_LIST)) {
            return null;
        }
        return tag.getList(NBT_KEY, Tag.TAG_COMPOUND);
    }

    /**
     * Get all infusion enchantments on an item stack.
     * 
     * @param stack The item stack
     * @return List of infusion enchantments (may be empty)
     */
    public static List<EnumInfusionEnchantment> getInfusionEnchantments(ItemStack stack) {
        ListTag nbttaglist = getInfusionEnchantmentTagList(stack);
        List<EnumInfusionEnchantment> list = new ArrayList<>();
        
        if (nbttaglist != null) {
            for (int j = 0; j < nbttaglist.size(); ++j) {
                CompoundTag tag = nbttaglist.getCompound(j);
                int k = tag.getShort("id");
                if (k >= 0 && k < values().length) {
                    list.add(values()[k]);
                }
            }
        }
        return list;
    }

    /**
     * Get the level of a specific infusion enchantment on an item stack.
     * 
     * @param stack The item stack
     * @param enchantment The enchantment to check
     * @return The level of the enchantment, or 0 if not present
     */
    public static int getInfusionEnchantmentLevel(ItemStack stack, EnumInfusionEnchantment enchantment) {
        ListTag nbttaglist = getInfusionEnchantmentTagList(stack);
        
        if (nbttaglist != null) {
            for (int j = 0; j < nbttaglist.size(); ++j) {
                CompoundTag tag = nbttaglist.getCompound(j);
                int k = tag.getShort("id");
                int l = tag.getShort("lvl");
                if (k >= 0 && k < values().length && values()[k] == enchantment) {
                    return l;
                }
            }
        }
        return 0;
    }

    /**
     * Check if an item stack has a specific infusion enchantment.
     * 
     * @param stack The item stack
     * @param enchantment The enchantment to check
     * @return true if the item has the enchantment
     */
    public static boolean hasInfusionEnchantment(ItemStack stack, EnumInfusionEnchantment enchantment) {
        return getInfusionEnchantmentLevel(stack, enchantment) > 0;
    }

    /**
     * Add or upgrade an infusion enchantment on an item stack.
     * 
     * @param stack The item stack
     * @param ie The enchantment to add
     * @param level The level to add
     */
    public static void addInfusionEnchantment(ItemStack stack, EnumInfusionEnchantment ie, int level) {
        if (stack == null || stack.isEmpty() || level > ie.maxLevel) {
            return;
        }

        // Get or create the tag
        CompoundTag stackTag = NBTHelper.getOrCreateTag(stack);
        ListTag nbttaglist;
        
        if (stackTag.contains(NBT_KEY, Tag.TAG_LIST)) {
            nbttaglist = stackTag.getList(NBT_KEY, Tag.TAG_COMPOUND);
            
            // Check if enchantment already exists
            for (int j = 0; j < nbttaglist.size(); ++j) {
                CompoundTag tag = nbttaglist.getCompound(j);
                int k = tag.getShort("id");
                int l = tag.getShort("lvl");
                
                if (k == ie.ordinal()) {
                    // Enchantment exists - only upgrade if new level is higher
                    if (level <= l) {
                        return;
                    }
                    tag.putShort("lvl", (short) level);
                    return;
                }
            }
        } else {
            nbttaglist = new ListTag();
        }

        // Add new enchantment
        CompoundTag nbttagcompound = new CompoundTag();
        nbttagcompound.putShort("id", (short) ie.ordinal());
        nbttagcompound.putShort("lvl", (short) level);
        nbttaglist.add(nbttagcompound);
        
        if (nbttaglist.size() > 0) {
            stackTag.put(NBT_KEY, nbttaglist);
        }
    }

    /**
     * Remove an infusion enchantment from an item stack.
     * 
     * @param stack The item stack
     * @param ie The enchantment to remove
     */
    public static void removeInfusionEnchantment(ItemStack stack, EnumInfusionEnchantment ie) {
        if (stack == null || stack.isEmpty() || !NBTHelper.hasTag(stack)) {
            return;
        }

        CompoundTag stackTag = NBTHelper.getTag(stack);
        if (stackTag == null || !stackTag.contains(NBT_KEY, Tag.TAG_LIST)) {
            return;
        }

        ListTag nbttaglist = stackTag.getList(NBT_KEY, Tag.TAG_COMPOUND);
        for (int j = nbttaglist.size() - 1; j >= 0; --j) {
            CompoundTag tag = nbttaglist.getCompound(j);
            int k = tag.getShort("id");
            if (k == ie.ordinal()) {
                nbttaglist.remove(j);
                break;
            }
        }

        // Remove the tag entirely if empty
        if (nbttaglist.isEmpty()) {
            stackTag.remove(NBT_KEY);
        }
    }

    /**
     * Get a display-friendly name for this enchantment.
     * 
     * @return The enchantment name in title case
     */
    public String getDisplayName() {
        String name = this.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}




