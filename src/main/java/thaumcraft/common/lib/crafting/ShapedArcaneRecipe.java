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
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IArcaneWorkbench;
import thaumcraft.init.ModRecipeTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * ShapedArcaneRecipe - A shaped crafting recipe for the Arcane Workbench.
 *
 * Requires:
 * - Items arranged in a specific pattern in the 3x3 grid
 * - Vis drawn from the aura
 * - Primal crystals in the 6 crystal slots
 * - Research unlocked by the player
 */
public class ShapedArcaneRecipe implements IArcaneRecipe {

    private final ResourceLocation id;
    private final String group;
    private final ShapedRecipePattern pattern;
    private final ItemStack result;
    private final int visCost;
    private final AspectList crystals;
    private final String research;

    public ShapedArcaneRecipe(ResourceLocation id, String group, ShapedRecipePattern pattern,
                              ItemStack result, int visCost, AspectList crystals, String research) {
        this.id = id;
        this.group = group;
        this.pattern = pattern;
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

    /**
     * Check if the recipe matches the given arcane workbench inventory.
     */
    public boolean matches(IArcaneWorkbench container, Level level) {
        for (int x = 0; x <= 3 - pattern.width(); x++) {
            for (int y = 0; y <= 3 - pattern.height(); y++) {
                if (matches(container, x, y, true) || matches(container, x, y, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the recipe matches at a specific position with optional mirroring.
     */
    private boolean matches(IArcaneWorkbench container, int offsetX, int offsetY, boolean mirrored) {
        int width = pattern.width();
        int height = pattern.height();
        NonNullList<Ingredient> ingredients = pattern.ingredients();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                int checkX = x - offsetX;
                int checkY = y - offsetY;

                Ingredient ingredient = Ingredient.EMPTY;
                if (checkX >= 0 && checkY >= 0 && checkX < width && checkY < height) {
                    if (mirrored) {
                        ingredient = ingredients.get(width - checkX - 1 + checkY * width);
                    } else {
                        ingredient = ingredients.get(checkX + checkY * width);
                    }
                }

                // Slots 0-8 are the crafting grid
                if (!ingredient.test(container.getItem(x + y * 3))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) {
        return result.copy();
    }

    @Override
    public ItemStack assemble(IArcaneWorkbench inv, HolderLookup.Provider provider) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= pattern.width() && height >= pattern.height();
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
        return pattern.ingredients();
    }

    @Override
    public String getGroup() {
        return group;
    }

    public int getWidth() {
        return pattern.width();
    }

    public int getHeight() {
        return pattern.height();
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

    /**
     * Serializer for ShapedArcaneRecipe.
     * Handles JSON parsing and network serialization.
     */
    public static class Serializer implements RecipeSerializer<ShapedArcaneRecipe> {

        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "arcane_workbench_shaped");

        public static final MapCodec<ShapedArcaneRecipe> CODEC = RecordCodecBuilder.mapCodec(inst ->
                inst.group(
                                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                                Codec.INT.optionalFieldOf("vis", 0).forGetter(r -> r.visCost),
                                ASPECT_LIST_CODEC.optionalFieldOf("crystals", new AspectList()).forGetter(r -> r.crystals),
                                Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research)
                        )
                        .apply(inst, (group, pattern, result, visCost, crystals, research) ->
                                new ShapedArcaneRecipe(null, group, pattern, result, visCost, crystals, research))
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ShapedArcaneRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<ShapedArcaneRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShapedArcaneRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static void toNetwork(RegistryFriendlyByteBuf buffer, ShapedArcaneRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeUtf(recipe.research);
            buffer.writeVarInt(recipe.visCost);
            ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            Aspect[] aspects = recipe.crystals.getAspects();
            buffer.writeVarInt(aspects.length);
            for (Aspect aspect : aspects) {
                buffer.writeUtf(aspect.getTag());
                buffer.writeVarInt(recipe.crystals.getAmount(aspect));
            }
        }

        public static ShapedArcaneRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            String research = buffer.readUtf();
            int visCost = buffer.readVarInt();
            ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
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
            return new ShapedArcaneRecipe(null, group, pattern, result, visCost, crystals, research);
        }
    }
}
