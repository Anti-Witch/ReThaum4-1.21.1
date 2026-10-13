package thaumcraft.common.lib.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InfusionEnchantmentRecipeSerializer implements RecipeSerializer<InfusionEnchantmentRecipe> {

    public static final InfusionEnchantmentRecipeSerializer INSTANCE = new InfusionEnchantmentRecipeSerializer();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "infusion_enchantment");

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

    public static final MapCodec<InfusionEnchantmentRecipe> CODEC = RecordCodecBuilder.mapCodec(inst ->
            inst.group(
                            Codec.STRING.fieldOf("enchantment").forGetter(r -> r.enchantment.name()),
                            ASPECT_LIST_CODEC.fieldOf("aspects").forGetter(r -> r.getAspects()),
                            COMPONENTS_CODEC.fieldOf("components").forGetter(r -> r.getComponents())
                    )
                    .apply(inst, (enchantmentName, aspects, components) ->
                            new InfusionEnchantmentRecipe(null, EnumInfusionEnchantment.valueOf(enchantmentName.toUpperCase()), aspects, components))
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionEnchantmentRecipe> STREAM_CODEC =
            StreamCodec.of(InfusionEnchantmentRecipeSerializer::toNetwork, InfusionEnchantmentRecipeSerializer::fromNetwork);

    @Override
    public MapCodec<InfusionEnchantmentRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, InfusionEnchantmentRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    public static void toNetwork(RegistryFriendlyByteBuf buffer, InfusionEnchantmentRecipe recipe) {
        buffer.writeVarInt(recipe.enchantment.ordinal());

        buffer.writeVarInt(recipe.getComponents().size());
        for (Ingredient component : recipe.getComponents()) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, component);
        }

        Aspect[] aspectArray = recipe.getAspects().getAspects();
        buffer.writeVarInt(aspectArray.length);
        for (Aspect aspect : aspectArray) {
            buffer.writeUtf(aspect.getTag());
            buffer.writeVarInt(recipe.getAspects().getAmount(aspect));
        }
    }

    public static InfusionEnchantmentRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        int enchantmentOrdinal = buffer.readVarInt();
        EnumInfusionEnchantment enchantment = EnumInfusionEnchantment.values()[enchantmentOrdinal];

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

        return new InfusionEnchantmentRecipe(null, enchantment, aspects, components);
    }
}
