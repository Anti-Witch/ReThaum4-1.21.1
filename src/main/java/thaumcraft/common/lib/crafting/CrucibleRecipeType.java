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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.init.ModRecipeTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * CrucibleRecipeType - A Recipe implementation for crucible alchemy.
 *
 * Crucible recipes require:
 * - A catalyst item thrown into the crucible
 * - Required aspects (essentia) already dissolved in the crucible
 * - Research unlocked by the player
 *
 * The catalyst is consumed and the aspects are removed from the crucible
 * to produce the output item.
 */
public class CrucibleRecipeType implements Recipe<RecipeInput>, IThaumcraftRecipe {

    private final ResourceLocation id;
    private final String group;
    private final Ingredient catalyst;
    private final AspectList aspects;
    private final ItemStack result;
    private final String research;

    public CrucibleRecipeType(ResourceLocation id, String group, Ingredient catalyst,
                              AspectList aspects, ItemStack result, String research) {
        this.id = id;
        this.group = group;
        this.catalyst = catalyst;
        this.aspects = aspects;
        this.result = result;
        this.research = research;
    }

    /**
     * Standard Recipe.matches() - not typically used for crucible recipes
     * since they don't use a standard crafting grid.
     */
    @Override
    public boolean matches(RecipeInput container, Level level) {
        // Crucible matching is handled differently - via matchesCrucible()
        return false;
    }

    /**
     * Check if this recipe matches the given crucible state.
     *
     * @param crucibleAspects The aspects currently in the crucible
     * @param catalystStack The item being thrown in
     * @return true if the recipe can be crafted
     */
    public boolean matchesCrucible(AspectList crucibleAspects, ItemStack catalystStack) {
        if (!catalyst.test(catalystStack)) return false;
        if (crucibleAspects == null) return false;

        // Check all required aspects are present in sufficient amounts
        for (Aspect aspect : aspects.getAspects()) {
            if (crucibleAspects.getAmount(aspect) < aspects.getAmount(aspect)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the given item can act as a catalyst for this recipe.
     */
    public boolean catalystMatches(ItemStack catalystStack) {
        return catalyst.test(catalystStack);
    }

    /**
     * Creates a new AspectList with the required aspects removed.
     */
    public AspectList removeMatchingAspects(AspectList crucibleAspects) {
        AspectList result = crucibleAspects.copy();
        for (Aspect aspect : aspects.getAspects()) {
            result.remove(aspect, aspects.getAmount(aspect));
        }
        return result;
    }

    @Override
    public ItemStack assemble(RecipeInput container, HolderLookup.Provider provider) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // Crucible doesn't use dimensions
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
        return ModRecipeTypes.CRUCIBLE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, catalyst);
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getResearch() {
        return research;
    }

    public Ingredient getCatalyst() {
        return catalyst;
    }

    public AspectList getAspects() {
        return aspects;
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

    /**
     * Serializer for CrucibleRecipeType.
     */
    public static class Serializer implements RecipeSerializer<CrucibleRecipeType> {

        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "crucible");

        public static final MapCodec<CrucibleRecipeType> CODEC = RecordCodecBuilder.mapCodec(inst ->
                inst.group(
                                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                                Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research),
                                Ingredient.CODEC.fieldOf("catalyst").forGetter(r -> r.catalyst),
                                ASPECT_LIST_CODEC.optionalFieldOf("aspects", new AspectList()).forGetter(r -> r.aspects),
                                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
                        )
                        .apply(inst, (group, research, catalyst, aspects, result) ->
                                new CrucibleRecipeType(null, group, catalyst, aspects, result, research))
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipeType> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<CrucibleRecipeType> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipeType> streamCodec() {
            return STREAM_CODEC;
        }

        public static void toNetwork(RegistryFriendlyByteBuf buffer, CrucibleRecipeType recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeUtf(recipe.research);

            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.catalyst);

            Aspect[] aspectArray = recipe.aspects.getAspects();
            buffer.writeVarInt(aspectArray.length);
            for (Aspect aspect : aspectArray) {
                buffer.writeUtf(aspect.getTag());
                buffer.writeVarInt(recipe.aspects.getAmount(aspect));
            }

            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }

        public static CrucibleRecipeType fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            String research = buffer.readUtf();

            Ingredient catalyst = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);

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

            return new CrucibleRecipeType(null, group, catalyst, aspects, result, research);
        }
    }
}
