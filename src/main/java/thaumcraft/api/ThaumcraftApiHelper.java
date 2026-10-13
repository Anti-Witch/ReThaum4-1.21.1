package thaumcraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.aspects.IEssentiaTransport;

import java.nio.ByteBuffer;

/**
 * Helper class for various Thaumcraft API operations.
 */
public class ThaumcraftApiHelper {

    /**
     * Gets a connectable essentia transport tile adjacent to the given position.
     *
     * @param level the world
     * @param pos the position to check from
     * @param direction the direction to check
     * @return the connectable tile, or null if none found
     */
    public static BlockEntity getConnectableTile(Level level, BlockPos pos, Direction direction) {
        BlockEntity te = level.getBlockEntity(pos.relative(direction));
        if (te instanceof IEssentiaTransport transport && transport.isConnectable(direction.getOpposite())) {
            return te;
        }
        return null;
    }

    /**
     * Converts various object types into an Ingredient for recipe matching.
     * Supports: Ingredient, ItemStack (with or without NBT), and tag strings.
     *
     * @param obj the object to convert
     * @return the Ingredient, or null if conversion failed
     */
    public static Ingredient getIngredient(Object obj) {
        if (obj == null) return null;
        
        if (obj instanceof Ingredient ingredient) {
            return ingredient;
        }
        
        if (obj instanceof ItemStack stack) {
            if (!stack.getComponentsPatch().isEmpty()) {
                // Use strict component ingredient for items with components
                return DataComponentIngredient.of(true, stack);
            } else {
                return Ingredient.of(stack);
            }
        }
        
        if (obj instanceof ItemStack[] stacks) {
            return Ingredient.of(stacks);
        }
        
        // Tag-based ingredients would be handled here
        // For now, return null for unsupported types
        return null;
    }

    // ==================== Bit Manipulation Utilities ====================

    public static int setByteInInt(int data, byte b, int index) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(0, data);
        bb.put(index, b);
        return bb.getInt(0);
    }

    public static byte getByteInInt(int data, int index) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(0, data);
        return bb.get(index);
    }

    public static long setByteInLong(long data, byte b, int index) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(0, data);
        bb.put(index, b);
        return bb.getLong(0);
    }

    public static byte getByteInLong(long data, int index) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(0, data);
        return bb.get(index);
    }

    public static int setNibbleInInt(int data, int nibble, int nibbleIndex) {
        int shift = nibbleIndex * 4;
        return (data & ~(0xf << shift)) | (nibble << shift);
    }

    public static int getNibbleInInt(int data, int nibbleIndex) {
        return (data >> (nibbleIndex << 2)) & 0xF;
    }

    // ==================== Crystal Creation ====================

    /**
     * Create a crystal essence itemstack from an aspect.
     *
     * @param aspect the aspect for the crystal
     * @param stackSize the stack size
     * @return the crystal ItemStack
     */
    public static ItemStack makeCrystal(Aspect aspect, int stackSize) {
        if (aspect == null) return ItemStack.EMPTY;
        
        // Get the crystal essence item from the registry
        Item crystalItem = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "crystal_essence"));
        
        if (crystalItem == null) return ItemStack.EMPTY;
        
        ItemStack stack = new ItemStack(crystalItem, stackSize);
        if (crystalItem instanceof IEssentiaContainerItem container) {
            container.setAspects(stack, new AspectList().add(aspect, 1));
        }
        return stack;
    }

    /**
     * Create a single crystal itemstack from an aspect.
     *
     * @param aspect the aspect for the crystal
     * @return the crystal ItemStack
     */
    public static ItemStack makeCrystal(Aspect aspect) {
        return makeCrystal(aspect, 1);
    }
}





