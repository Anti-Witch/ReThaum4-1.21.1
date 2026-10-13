package thaumcraft.api.aspects;

import java.io.Serializable;
import java.util.LinkedHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * AspectList - A container for multiple aspects and their amounts.
 * Used for storing aspect requirements, item aspects, etc.
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public class AspectList implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public LinkedHashMap<Aspect, Integer> aspects = new LinkedHashMap<>();

    /**
     * Creates a new aspect list with preloaded values based on the aspects of the given item.
     * @param stack the itemstack of the given item
     */
    public AspectList(ItemStack stack) {
        try {
            AspectList temp = AspectHelper.getObjectAspects(stack);
            if (temp != null) {
                for (Aspect tag : temp.getAspects()) {
                    add(tag, temp.getAmount(tag));
                }
            }
        } catch (Exception e) {
            // Ignore errors during aspect lookup
        }
    }
    
    public AspectList() {
    }
    
    public AspectList copy() {
        AspectList out = new AspectList();
        for (Aspect a : getAspects()) {
            out.add(a, getAmount(a));
        }
        return out;
    }
    
    /**
     * @return the amount of different aspects in this collection
     */
    public int size() {
        return aspects.size();
    }
    
    /**
     * @return the amount of total vis in this collection
     */
    public int visSize() {
        int q = 0;
        for (Aspect as : aspects.keySet()) {
            q += getAmount(as);
        }
        return q;
    }
    
    /**
     * @return an array of all the aspects in this collection
     */
    public Aspect[] getAspects() {
        return aspects.keySet().toArray(new Aspect[0]);
    }

    /**
     * @return an array of all the aspects in this collection sorted by name
     */
    public Aspect[] getAspectsSorted() {
        return getAspectsSortedByName();
    }

    /**
     * @return an array of all the aspects in this collection sorted by name
     */
    public Aspect[] getAspectsSortedByName() {
        try {
            Aspect[] out = aspects.keySet().toArray(new Aspect[0]);
            boolean change;
            do {
                change = false;
                for (int a = 0; a < out.length - 1; a++) {
                    Aspect e1 = out[a];
                    Aspect e2 = out[a + 1];
                    if (e1 != null && e2 != null && e1.getTag().compareTo(e2.getTag()) > 0) {
                        out[a] = e2;
                        out[a + 1] = e1;
                        change = true;
                        break;
                    }
                }
            } while (change);
            return out;
        } catch (Exception e) {
            return getAspects();
        }
    }
    
    /**
     * @return an array of all the aspects in this collection sorted by amount
     */
    public Aspect[] getAspectsSortedByAmount() {
        try {
            Aspect[] out = aspects.keySet().toArray(new Aspect[0]);
            boolean change;
            do {
                change = false;
                for (int a = 0; a < out.length - 1; a++) {
                    int e1 = getAmount(out[a]);
                    int e2 = getAmount(out[a + 1]);
                    if (e1 > 0 && e2 > 0 && e2 > e1) {
                        Aspect ea = out[a];
                        Aspect eb = out[a + 1];
                        out[a] = eb;
                        out[a + 1] = ea;
                        change = true;
                        break;
                    }
                }
            } while (change);
            return out;
        } catch (Exception e) {
            return getAspects();
        }
    }
    
    /**
     * @param key the aspect to query
     * @return the amount associated with the given aspect in this collection
     */
    public int getAmount(Aspect key) {
        return aspects.get(key) == null ? 0 : aspects.get(key);
    }
    
    /**
     * Reduces the amount of an aspect in this collection by the given amount.
     * @param key the aspect to reduce
     * @param amount the amount to reduce by
     * @return true if successful
     */
    public boolean reduce(Aspect key, int amount) {
        if (getAmount(key) >= amount) {
            int am = getAmount(key) - amount;
            aspects.put(key, am);
            return true;
        }
        return false;
    }
    
    /**
     * Reduces the amount of an aspect in this collection by the given amount.
     * If reduced to 0 or less the aspect will be removed completely.
     * @param key the aspect to reduce
     * @param amount the amount to reduce by
     * @return this AspectList for chaining
     */
    public AspectList remove(Aspect key, int amount) {
        int am = getAmount(key) - amount;
        if (am <= 0) {
            aspects.remove(key);
        } else {
            aspects.put(key, am);
        }
        return this;
    }
    
    /**
     * Simply removes the aspect from the list
     * @param key the aspect to remove
     * @return this AspectList for chaining
     */
    public AspectList remove(Aspect key) {
        aspects.remove(key);
        return this;
    }
    
    /**
     * Adds this aspect and amount to the collection.
     * If the aspect exists then its value will be increased by the given amount.
     * @param aspect the aspect to add
     * @param amount the amount to add
     * @return this AspectList for chaining
     */
    public AspectList add(Aspect aspect, int amount) {
        if (aspect == null) return this;
        if (aspects.containsKey(aspect)) {
            int oldamount = aspects.get(aspect);
            amount += oldamount;
        }
        aspects.put(aspect, amount);
        return this;
    }

    /**
     * Adds this aspect and amount to the collection.
     * If the aspect exists then only the highest of the old or new amount will be used.
     * @param aspect the aspect to merge
     * @param amount the amount to merge
     * @return this AspectList for chaining
     */
    public AspectList merge(Aspect aspect, int amount) {
        if (aspects.containsKey(aspect)) {
            int oldamount = aspects.get(aspect);
            if (amount < oldamount) {
                amount = oldamount;
            }
        }
        aspects.put(aspect, amount);
        return this;
    }
    
    public AspectList add(AspectList in) {
        for (Aspect a : in.getAspects()) {
            add(a, in.getAmount(a));
        }
        return this;
    }
    
    public AspectList remove(AspectList in) {
        for (Aspect a : in.getAspects()) {
            remove(a, in.getAmount(a));
        }
        return this;
    }
    
    public AspectList merge(AspectList in) {
        for (Aspect a : in.getAspects()) {
            merge(a, in.getAmount(a));
        }
        return this;
    }
    
    /**
     * Check if this list contains all aspects from another list with at least the required amounts
     * @param required the required aspects
     * @return true if all required aspects are present with sufficient amounts
     */
    public boolean contains(AspectList required) {
        for (Aspect a : required.getAspects()) {
            if (getAmount(a) < required.getAmount(a)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if this list contains the given aspect
     * @param aspect the aspect to check
     * @return true if the aspect is present
     */
    public boolean contains(Aspect aspect) {
        return aspects.containsKey(aspect) && aspects.get(aspect) > 0;
    }
    
    /**
     * Reads the list of aspects from NBT
     * @param nbt the compound tag to read from
     */
    public void readFromNBT(CompoundTag nbt) {
        aspects.clear();
        ListTag tlist = nbt.getList("Aspects", Tag.TAG_COMPOUND);
        for (int j = 0; j < tlist.size(); j++) {
            CompoundTag rs = tlist.getCompound(j);
            if (rs.contains("key")) {
                Aspect aspect = Aspect.getAspect(rs.getString("key"));
                if (aspect != null) {
                    add(aspect, rs.getInt("amount"));
                }
            }
        }
    }
    
    public void readFromNBT(CompoundTag nbt, String label) {
        aspects.clear();
        ListTag tlist = nbt.getList(label, Tag.TAG_COMPOUND);
        for (int j = 0; j < tlist.size(); j++) {
            CompoundTag rs = tlist.getCompound(j);
            if (rs.contains("key")) {
                Aspect aspect = Aspect.getAspect(rs.getString("key"));
                if (aspect != null) {
                    add(aspect, rs.getInt("amount"));
                }
            }
        }
    }
    
    /**
     * Writes the list of aspects to NBT
     * @param nbt the compound tag to write to
     */
    public void writeToNBT(CompoundTag nbt) {
        ListTag tlist = new ListTag();
        nbt.put("Aspects", tlist);
        for (Aspect aspect : getAspects()) {
            if (aspect != null) {
                CompoundTag f = new CompoundTag();
                f.putString("key", aspect.getTag());
                f.putInt("amount", getAmount(aspect));
                tlist.add(f);
            }
        }
    }
    
    public void writeToNBT(CompoundTag nbt, String label) {
        ListTag tlist = new ListTag();
        nbt.put(label, tlist);
        for (Aspect aspect : getAspects()) {
            if (aspect != null) {
                CompoundTag f = new CompoundTag();
                f.putString("key", aspect.getTag());
                f.putInt("amount", getAmount(aspect));
                tlist.add(f);
            }
        }
    }
    
    /**
     * Sets the amount of an aspect to a specific value.
     * @param aspect the aspect to set
     * @param amount the amount to set (if <= 0, removes the aspect)
     * @return this AspectList for chaining
     */
    public AspectList setAmount(Aspect aspect, int amount) {
        if (aspect == null) return this;
        if (amount <= 0) {
            aspects.remove(aspect);
        } else {
            aspects.put(aspect, amount);
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AspectList[");
        boolean first = true;
        for (Aspect a : getAspects()) {
            if (!first) sb.append(", ");
            sb.append(a.getTag()).append("=").append(getAmount(a));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}




