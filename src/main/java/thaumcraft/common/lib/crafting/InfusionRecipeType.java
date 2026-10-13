package thaumcraft.common.lib.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.init.ModRecipeTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InfusionRecipeType - A Recipe implementation for infusion altar crafting.
 *
 * Infusion recipes require:
 * - A central item on the runic matrix
 * - Component items placed on pedestals around the altar
 * - Essentia from nearby jars
 * - Research unlocked by the player
 *
 * The infusion process consumes the central item and components,
 * drains essentia, and produces the output item.
 */
public class InfusionRecipeType implements Recipe<RecipeInput>, IThaumcraftRecipe {

    private final ResourceLocation id;
    private final String group;
    private final Ingredient centralItem;
    private final NonNullList<Ingredient> components;
    private final AspectList aspects;
    private final ItemStack result;
    private final String research;
    private final int instability;
    /**
     * TC4 infusion recipes could return an Object[] {"tag", value} which applied
     * the NBT to the central item instead of producing a brand new stack (used
     * by the fortress helm upgrades: goggles and the three faceplates).
     * When true, the output is the input stack with the result's custom data
     * merged into it (so damage and other upgrades such as goggles/masks are
     * preserved).
     */
    private final boolean applyToInput;

    public InfusionRecipeType(ResourceLocation id, String group, Ingredient centralItem,
                              NonNullList<Ingredient> components, AspectList aspects,
                              ItemStack result, String research, int instability) {
        this(id, group, centralItem, components, aspects, result, research, instability, false);
    }

    public InfusionRecipeType(ResourceLocation id, String group, Ingredient centralItem,
                              NonNullList<Ingredient> components, AspectList aspects,
                              ItemStack result, String research, int instability, boolean applyToInput) {
        this.id = id;
        this.group = group;
        this.centralItem = centralItem;
        this.components = components;
        this.aspects = aspects;
        this.result = result;
        this.research = research;
        this.instability = instability;
        this.applyToInput = applyToInput;
    }

    /**
     * Standard Recipe.matches() - not typically used for infusion recipes
     * since they don't use a standard crafting grid.
     */
    @Override
    public boolean matches(RecipeInput container, Level level) {
        // Infusion matching is handled differently - via matchesInfusion()
        return false;
    }

    /**
     * Check if this recipe matches the current infusion altar state.
     *
     * @param pedestalItems Items on the pedestals around the altar
     * @param centralItemStack The item on the runic matrix
     * @param level The world
     * @param player The player crafting
     * @return true if the recipe can be crafted
     */
    public boolean matchesInfusion(List<ItemStack> pedestalItems, ItemStack centralItemStack,
                                    Level level, Player player) {
        // Check research requirement
        if (research != null && !research.isEmpty()) {
            if (!ThaumcraftCapabilities.isResearchKnown(player, research)) {
                return false;
            }
        }

        // Check central item (empty ingredient means any item is valid)
        if (centralItem != Ingredient.EMPTY && !centralItem.test(centralItemStack)) {
            return false;
        }

        // Check pedestal components using RecipeMatcher for shapeless matching
        return RecipeMatcher.findMatches(pedestalItems, components) != null;
    }

    @Override
    public ItemStack assemble(RecipeInput container, HolderLookup.Provider provider) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // Infusion doesn't use dimensions
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return result;
    }

    public ItemStack getResultItem() {
        return result.copy();
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.INFUSION.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> allIngredients = NonNullList.create();
        allIngredients.add(centralItem);
        allIngredients.addAll(components);
        return allIngredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getResearch() {
        return research;
    }

    public Ingredient getCentralItem() {
        return centralItem;
    }

    public NonNullList<Ingredient> getComponents() {
        return components;
    }

    public AspectList getAspects() {
        return aspects;
    }

    public int getInstability() {
        return instability;
    }

    /**
     * Gets the recipe output, potentially modified based on input.
     * Override this in subclasses for recipes that modify the output based on input properties.
     */
    public ItemStack getRecipeOutput(Player player, ItemStack input, List<ItemStack> pedestalItems) {
        if (!applyToInput) {
            return result.copy();
        }
        // TC4 Object[]{"tag", value} outputs: keep the central stack (damage,
        // existing goggles/mask upgrades) and merge the recipe's custom data in.
        ItemStack out = input.copy();
        CustomData resultData = result.get(DataComponents.CUSTOM_DATA);
        if (resultData != null) {
            CompoundTag merged = out.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            merged.merge(resultData.copyTag());
            out.set(DataComponents.CUSTOM_DATA, CustomData.of(merged));
        }
        return out;
    }

    /**
     * Gets the aspects required, potentially modified based on input.
     * Override this in subclasses for recipes with variable essentia costs.
     */
    public AspectList getAspects(Player player, ItemStack input, List<ItemStack> pedestalItems) {
        return aspects;
    }

    /**
     * Gets the instability level, potentially modified based on input.
     * Override this in subclasses for recipes with variable instability.
     */
    public int getInstability(Player player, ItemStack input, List<ItemStack> pedestalItems) {
        return instability;
    }

    /**
     * Codec for an AspectList (serialized as a map of aspect name -> amount).
     */
    public static final Codec<AspectList> ASPECT_LIST_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(
                    map -> {
                        AspectList list = new AspectList();
                        map.forEach((name, amt) -> {
                            Aspect a = Aspect.getAspect(name);
                            if (a != null) list.add(a, amt);
                        });
                        return list;
                    },
                    list -> {
                        Map<String, Integer> map = new HashMap<>();
                        for (Aspect a : list.getAspects()) map.put(a.getTag(), list.getAmount(a));
                        return map;
                    }
            );

    private static final Codec<NonNullList<Ingredient>> COMPONENTS_CODEC = Ingredient.LIST_CODEC
            .xmap(list -> {
                NonNullList<Ingredient> nl = NonNullList.create();
                nl.addAll(list);
                return nl;
            }, list -> new ArrayList<>(list));

    /**
     * Serializer for InfusionRecipeType.
     */
    public static class Serializer implements RecipeSerializer<InfusionRecipeType> {

        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "infusion");

        public static final MapCodec<InfusionRecipeType> CODEC = RecordCodecBuilder.mapCodec(inst ->
                inst.group(
                                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                                Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research),
                                Codec.INT.optionalFieldOf("instability", 0).forGetter(r -> r.instability),
                                Ingredient.CODEC.optionalFieldOf("input", Ingredient.EMPTY).forGetter(r -> r.centralItem),
                                COMPONENTS_CODEC.optionalFieldOf("ingredients", NonNullList.create()).forGetter(r -> r.components),
                                ASPECT_LIST_CODEC.optionalFieldOf("aspects", new AspectList()).forGetter(r -> r.aspects),
                                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                                Codec.BOOL.optionalFieldOf("apply_to_input", false).forGetter(r -> r.applyToInput)
                        )
                        .apply(inst, (group, research, instability, centralItem, components, aspects, result, applyToInput) ->
                                new InfusionRecipeType(null, group, centralItem, components, aspects, result, research, instability, applyToInput))
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, InfusionRecipeType> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<InfusionRecipeType> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, InfusionRecipeType> streamCodec() {
            return STREAM_CODEC;
        }

        public static void toNetwork(RegistryFriendlyByteBuf buffer, InfusionRecipeType recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeUtf(recipe.research);
            buffer.writeVarInt(recipe.instability);

            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.centralItem);

            buffer.writeVarInt(recipe.components.size());
            for (Ingredient component : recipe.components) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, component);
            }

            Aspect[] aspectArray = recipe.aspects.getAspects();
            buffer.writeVarInt(aspectArray.length);
            for (Aspect aspect : aspectArray) {
                buffer.writeUtf(aspect.getTag());
                buffer.writeVarInt(recipe.aspects.getAmount(aspect));
            }

            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeBoolean(recipe.applyToInput);
        }

        public static InfusionRecipeType fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            String research = buffer.readUtf();
            int instability = buffer.readVarInt();

            Ingredient centralItem = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);

            int componentCount = buffer.readVarInt();
            NonNullList<Ingredient> components = NonNullList.withSize(componentCount, Ingredient.EMPTY);
            for (int i = 0; i < componentCount; i++) {
                components.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }

            AspectList aspects = new AspectList();
            int aspectCount = buffer.readVarInt();
            for (int i = 0; i < aspectCount; i++) {
                String aspectName = buffer.readUtf();
                int amount = buffer.readVarInt();
                Aspect aspect = Aspect.getAspect(aspectName);
                if (aspect != null) {
                    aspects.add(aspect, amount);
                }
            }

            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            boolean applyToInput = buffer.readBoolean();

            return new InfusionRecipeType(null, group, centralItem, components, aspects, result, research, instability, applyToInput);
        }
    }
}
