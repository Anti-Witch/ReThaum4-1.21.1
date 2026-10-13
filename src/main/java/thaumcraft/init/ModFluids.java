package thaumcraft.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.misc.BlockFluidDeath;
import thaumcraft.common.blocks.misc.BlockFluidPure;
import thaumcraft.common.blocks.world.taint.BlockFluxGoo;

import java.util.function.Consumer;

/**
 * Registry for Thaumcraft fluids: Flux Goo, Liquid Death and Purifying Fluid.
 *
 * Uses the NeoForge 1.21.1 fluid pattern (note: {@code ForgeFlowingFluid} was
 * renamed to {@code BaseFlowingFluid} in this NeoForge version):
 *  - a {@link FluidType} (FluidType registry) describing the fluid's physical properties,
 *  - a source {@link Fluid} and a flowing {@link Fluid} (Fluid registry),
 *  - a {@link LiquidBlock} (registered in {@link ModBlocks}) that renders/holds the fluid,
 *  - a {@link BucketItem} (Item registry) so the fluid can be picked up and placed.
 *
 * The block classes (BlockFluxGoo / BlockFluidDeath / BlockFluidPure) extend
 * {@link LiquidBlock} and reference these fluids via getFluidState, so they render
 * through the fluid renderer and flow using the vanilla fluid spreading logic.
 */
public class ModFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Thaumcraft.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Thaumcraft.MODID);
    public static final DeferredRegister<Item> BUCKETS =
            DeferredRegister.create(Registries.ITEM, Thaumcraft.MODID);

    // ===================================================================
    // Flux Goo
    // ===================================================================
    public static final DeferredHolder<FluidType, FluidType> FLUX_GOO_TYPE = FLUID_TYPES.register("flux_goo",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.thaumcraft.flux_goo")
                    .density(1200)
                    .viscosity(1500)
                    .temperature(300)
                    .lightLevel(0)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
                @Override
                @SuppressWarnings("removal")
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new ThaumFluidExtensions("block/fluxgoo", 0xFFFFFFFF));
                }
            });

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FLUX_GOO =
            FLUIDS.register("flux_goo", () -> new BaseFlowingFluid.Source(fluxGooProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_FLUX_GOO =
            FLUIDS.register("flowing_flux_goo", () -> new BaseFlowingFluid.Flowing(fluxGooProperties()));
    public static final DeferredHolder<Item, BucketItem> FLUX_GOO_BUCKET = BUCKETS.register("flux_goo_bucket",
            () -> new BucketItem(FLUX_GOO.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private static BaseFlowingFluid.Properties fluxGooProperties() {
        return new BaseFlowingFluid.Properties(() -> FLUX_GOO_TYPE.get(), () -> FLUX_GOO.get(), () -> FLOWING_FLUX_GOO.get())
                .block(() -> (LiquidBlock) ModBlocks.FLUX_GOO.get())
                .bucket(() -> FLUX_GOO_BUCKET.get());
    }

    // ===================================================================
    // Liquid Death
    // ===================================================================
    public static final DeferredHolder<FluidType, FluidType> LIQUID_DEATH_TYPE = FLUID_TYPES.register("liquid_death",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.thaumcraft.liquid_death")
                    .density(1200)
                    .viscosity(1200)
                    .temperature(300)
                    .lightLevel(3)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
                @Override
                @SuppressWarnings("removal")
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new ThaumFluidExtensions("block/fluiddeath", 0xFFFFFFFF));
                }
            });

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_DEATH =
            FLUIDS.register("liquid_death", () -> new BaseFlowingFluid.Source(liquidDeathProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_LIQUID_DEATH =
            FLUIDS.register("flowing_liquid_death", () -> new BaseFlowingFluid.Flowing(liquidDeathProperties()));
    public static final DeferredHolder<Item, BucketItem> LIQUID_DEATH_BUCKET = BUCKETS.register("liquid_death_bucket",
            () -> new BucketItem(LIQUID_DEATH.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private static BaseFlowingFluid.Properties liquidDeathProperties() {
        return new BaseFlowingFluid.Properties(() -> LIQUID_DEATH_TYPE.get(), () -> LIQUID_DEATH.get(), () -> FLOWING_LIQUID_DEATH.get())
                .block(() -> (LiquidBlock) ModBlocks.LIQUID_DEATH.get())
                .bucket(() -> LIQUID_DEATH_BUCKET.get());
    }

    // ===================================================================
    // Purifying Fluid
    // ===================================================================
    public static final DeferredHolder<FluidType, FluidType> PURIFYING_FLUID_TYPE = FLUID_TYPES.register("purifying_fluid",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.thaumcraft.purifying_fluid")
                    .density(1000)
                    .viscosity(1000)
                    .temperature(300)
                    .lightLevel(10)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
                @Override
                @SuppressWarnings("removal")
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new ThaumFluidExtensions("block/fluidpure", 0xFFFFFFFF));
                }
            });

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> PURIFYING_FLUID =
            FLUIDS.register("purifying_fluid", () -> new BaseFlowingFluid.Source(purifyingFluidProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_PURIFYING_FLUID =
            FLUIDS.register("flowing_purifying_fluid", () -> new BaseFlowingFluid.Flowing(purifyingFluidProperties()));
    public static final DeferredHolder<Item, BucketItem> PURIFYING_FLUID_BUCKET = BUCKETS.register("purifying_fluid_bucket",
            () -> new BucketItem(PURIFYING_FLUID.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private static BaseFlowingFluid.Properties purifyingFluidProperties() {
        return new BaseFlowingFluid.Properties(() -> PURIFYING_FLUID_TYPE.get(), () -> PURIFYING_FLUID.get(), () -> FLOWING_PURIFYING_FLUID.get())
                .block(() -> (LiquidBlock) ModBlocks.PURIFYING_FLUID.get())
                .bucket(() -> PURIFYING_FLUID_BUCKET.get());
    }

    // ===================================================================
    // Helpers
    // ===================================================================

    /**
     * Minimal client-side fluid extension: provides the still/flowing texture and a tint colour.
     * The tint is white (0xFFFFFFFF) because the textures themselves carry the desired colour.
     */
    private static class ThaumFluidExtensions implements IClientFluidTypeExtensions {
        private final ResourceLocation stillTexture;
        private final int tintColor;

        ThaumFluidExtensions(String texturePath, int tintColor) {
            this.stillTexture = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, texturePath);
            this.tintColor = tintColor;
        }

        @Override
        public ResourceLocation getStillTexture() {
            return stillTexture;
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return stillTexture;
        }

        @Override
        public int getTintColor() {
            return tintColor;
        }
    }
}
