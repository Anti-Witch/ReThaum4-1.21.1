package thaumcraft.common.items.casters;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.casters.FocusPackage;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Base class for all focus items.
 * A focus is installed into a caster gauntlet to provide spell effects.
 */
public class ItemFocus extends Item {
    
    private static final DecimalFormat VIS_FORMAT = new DecimalFormat("#######.#");
    
    /** Maximum complexity this focus can handle */
    private final int maxComplexity;
    
    public ItemFocus(int complexity) {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE));
        this.maxComplexity = complexity;
    }
    
    /**
     * Get the color of this focus based on its effects.
     */
    public int getFocusColor(ItemStack focusStack) {
        if (focusStack.isEmpty()) {
            return 0xFFFFFF; // White default
        }

        CustomData cd = focusStack.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains("color")) {
                return tag.getInt("color");
            }
        }

        // Calculate color from effects and cache it
        FocusPackage core = getPackage(focusStack);
        if (core != null) {
            // Default purple-ish color for magic
            int color = 0x9933FF;
            focusStack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, c -> {
                CompoundTag t = c.copyTag();
                t.putInt("color", color);
                return CustomData.of(t);
            });
            return color;
        }

        return 0xFFFFFF;
    }

    /**
     * Get a string for sorting/comparing focus configurations.
     */
    public String getSortingHelper(ItemStack focusStack) {
        if (focusStack.isEmpty()) {
            return null;
        }

        int sh = 0;
        CustomData cd = focusStack.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            sh = cd.copyTag().getInt("srt");
        }

        if (sh == 0) {
            FocusPackage pack = getPackage(focusStack);
            if (pack != null) {
                sh = pack.getSortingHelper();
                final int srt = sh;
                focusStack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, c -> {
                    CompoundTag t = c.copyTag();
                    t.putInt("srt", srt);
                    return CustomData.of(t);
                });
            }
        }

        return focusStack.getHoverName().getString() + sh;
    }

    /**
     * Store a FocusPackage configuration in this focus.
     */
    public static void setPackage(ItemStack focusStack, FocusPackage core) {
        CompoundTag tag = core.serialize();
        focusStack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, c -> {
            CompoundTag t = c.copyTag();
            t.put("package", tag);
            return CustomData.of(t);
        });
    }

    /**
     * Get the FocusPackage configuration from this focus.
     * Returns the edited package (Focal Manipulator) when present, otherwise
     * the fixed preset package of TC4-style preset foci.
     */
    @Nullable
    public static FocusPackage getPackage(ItemStack focusStack) {
        if (focusStack.isEmpty()) {
            return null;
        }

        CustomData cd = focusStack.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            CompoundTag root = cd.copyTag();
            if (root.contains("package")) {
                CompoundTag tag = root.getCompound("package");
                FocusPackage pack = new FocusPackage();
                pack.deserialize(tag);
                return pack;
            }
        }

        // TC4-style preset foci always cast their fixed package
        if (focusStack.getItem() instanceof thaumcraft.common.items.wands.foci.ItemFocusPreset preset) {
            return preset.createPresetPackage();
        }

        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addFocusInformation(stack, tooltip, flag);
    }

    /**
     * Add focus-specific tooltip information.
     */
    public void addFocusInformation(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        FocusPackage pack = getPackage(stack);
        if (pack != null) {
            float visCost = getVisCost(stack);
            String amount = VIS_FORMAT.format(visCost);
            tooltip.add(Component.translatable("item.thaumcraft.focus.cost", amount)
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.AQUA));
            
            // TODO: Add focus element descriptions
        }
    }
    
    /**
     * Get the vis cost for casting with this focus.
     */
    public float getVisCost(ItemStack focusStack) {
        FocusPackage pack = getPackage(focusStack);
        if (pack == null) {
            return 0.0f;
        }
        return pack.getComplexity() / 5.0f;
    }
    
    /**
     * Get the cooldown time after casting with this focus.
     */
    public int getActivationTime(ItemStack focusStack) {
        FocusPackage pack = getPackage(focusStack);
        if (pack == null) {
            return 0;
        }
        int complexity = pack.getComplexity();
        return Math.max(5, (complexity / 5) * (complexity / 4));
    }
    
    /**
     * Get the maximum complexity this focus can handle.
     */
    public int getMaxComplexity() {
        return maxComplexity;
    }
    
    /**
     * Create a focus with default blank configuration.
     */
    public static ItemFocus createBlank() {
        return new ItemFocus(25); // Standard complexity cap
    }
    
    /**
     * Create an advanced focus with higher complexity cap.
     */
    public static ItemFocus createAdvanced() {
        return new ItemFocus(50); // Advanced complexity cap
    }
}




