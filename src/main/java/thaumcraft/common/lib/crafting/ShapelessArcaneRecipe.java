package thaumcraft.common.lib.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IArcaneWorkbench;
import thaumcraft.init.ModRecipeTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ShapelessArcaneRecipe - A shapeless crafting recipe for the Arcane Workbench.
 *
 * Requires:
 * - Items placed anywhere in the 3x3 grid (order doesn't matter)
 * - Vis drawn from the aura
 * - Primal crystals in the 6 crystal slots
 * - Research unlocked by the player
 */
public class ShapelessArcaneRecipe implements IArcaneRecipe {

    private final ResourceLocation id;
    private final String group;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int visCost;
    private final AspectList crystals;
    private final String research;

    public ShapelessArcaneRecipe(ResourceLocation id, String group,
                                 NonNullList<Ingredient> ingredients, ItemStack result,
                                 int visCost, AspectList crystals, String research) {
        this.id = id;
        this.group = group;
        this.ingredients = ingredients;
        this.result = result;
        this.visCost = visCost;
        this.crystals = crystals;
        this.research = research;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        if (input instanceof IArcaneWorkbench wb) return matches(wb, level);
        return false;
    }

    @Override
    public boolean matches(IArcaneWorkbench container, Level level) {
        List<ItemStack> inputs = new ArrayList<>();

        // Gather all non-empty items from the 3x3 grid (slots 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack item = container.getItem(i);
            if (!item.isEmpty()) {
                inputs.add(item);
            }
        }

        // Check if input count matches and all ingredients are satisfied
        return inputs.size() == ingredients.size() && RecipeMatcher.findMatches(inputs, ingredients) != null;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) {
        return result.copy();
    }

    @Override
    public ItemStack assemble(IArcaneWorkbench container, HolderLookup.Provider provider) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
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
        return ModRecipeTypes.ARCANE_WORKBENCH.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public int getVis() {
        return visCost;
    }

    @Override
    public String getResearch() {
        return research;
    }

    @Override
    public AspectList getCrystals() {
        return crystals;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(IArcaneWorkbench inv) {
        return NonNullList.withSize(9, ItemStack.EMPTY);
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

    private static final Codec<NonNullList<Ingredient>> INGREDIENTS_CODEC = Ingredient.LIST_CODEC
            .xmap(list -> {
                NonNullList<Ingredient> nl = NonNullList.create();
                nl.addAll(list);
                return nl;
            }, list -> new ArrayList<>(list));

    /**
     * Serializer for ShapelessArcaneRecipe.
     * Handles JSON parsing and network serialization.
     */
    public static class Serializer implements RecipeSerializer<ShapelessArcaneRecipe> {

        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "arcane_workbench_shapeless");

        public static final MapCodec<ShapelessArcaneRecipe> CODEC = RecordCodecBuilder.mapCodec(inst ->
                inst.group(
                                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                                Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research),
                                Codec.INT.optionalFieldOf("vis", 0).forGetter(r -> r.visCost),
                                ASPECT_LIST_CODEC.optionalFieldOf("crystals", new AspectList()).forGetter(r -> r.crystals),
                                INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(r -> r.ingredients),
                                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
                        )
                        .apply(inst, (group, research, visCost, crystals, ingredients, result) ->
                                new ShapelessArcaneRecipe(null, group, ingredients, result, visCost, crystals, research))
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessArcaneRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<ShapelessArcaneRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShapelessArcaneRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static void toNetwork(RegistryFriendlyByteBuf buffer, ShapelessArcaneRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeUtf(recipe.research);
            buffer.writeVarInt(recipe.visCost);

            Aspect[] aspects = recipe.crystals.getAspects();
            buffer.writeVarInt(aspects.length);
            for (Aspect aspect : aspects) {
                buffer.writeUtf(aspect.getTag());
                buffer.writeVarInt(recipe.crystals.getAmount(aspect));
            }

            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }

            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }

        public static ShapelessArcaneRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            String research = buffer.readUtf();
            int visCost = buffer.readVarInt();

            AspectList crystals = new AspectList();
            int crystalCount = buffer.readVarInt();
            for (int i = 0; i < crystalCount; i++) {
                String aspectName = buffer.readUtf();
                int amount = buffer.readVarInt();
                Aspect aspect = Aspect.getAspect(aspectName);
                if (aspect != null) {
                    crystals.add(aspect, amount);
                }
            }

            int ingredientCount = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientCount, Ingredient.EMPTY);
            for (int i = 0; i < ingredientCount; i++) {
                ingredients.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }

            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);

            return new ShapelessArcaneRecipe(null, group, ingredients, result, visCost, crystals, research);
        }
    }
}
