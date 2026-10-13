package thaumcraft.api.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.world.item.ItemStack;

/**
 * Weighted random loot entry for Thaumcraft loot bags.
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public class WeightedRandomLoot {
    
    /** The item to generate in the bag. */
    public ItemStack item;
    
    /** The weight of this loot entry. */
    public final int itemWeight;

    public WeightedRandomLoot(ItemStack stack, int weight) {
        this.item = stack;
        this.itemWeight = weight;
    }
    
    /**
     * Get the weight value.
     */
    public int getWeight() {
        return itemWeight;
    }
    
    /** Common loot bag items (most frequently dropped) */
    public static List<WeightedRandomLoot> lootBagCommon = new ArrayList<>();
    
    /** Uncommon loot bag items */
    public static List<WeightedRandomLoot> lootBagUncommon = new ArrayList<>();
    
    /** Rare loot bag items (least frequently dropped) */
    public static List<WeightedRandomLoot> lootBagRare = new ArrayList<>();
    
    /**
     * Select a random item from a list of weighted loot entries.
     * @param list the list of weighted loot entries
     * @param random the random instance
     * @return the selected loot entry, or null if list is empty
     */
    public static WeightedRandomLoot getRandomItem(List<WeightedRandomLoot> list, Random random) {
        if (list.isEmpty()) {
            return null;
        }
        
        int totalWeight = 0;
        for (WeightedRandomLoot entry : list) {
            totalWeight += entry.itemWeight;
        }
        
        if (totalWeight <= 0) {
            return null;
        }
        
        int selected = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (WeightedRandomLoot entry : list) {
            currentWeight += entry.itemWeight;
            if (selected < currentWeight) {
                return entry;
            }
        }
        
        // Fallback (should not reach here)
        return list.get(list.size() - 1);
    }
}




