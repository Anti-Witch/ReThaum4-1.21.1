package thaumcraft.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.init.ModItems;

import java.util.List;

/**
 * Client-side item model predicates for the wand.
 *
 * The wand is a single item whose rod/cap are stored as Data Components, so
 * its 3D model (rod sprite + cap sprite, TC4-style) is selected through
 * model overrides in {@code models/item/wand.json}. The override models live
 * under {@code models/item/wand_c/<rod>__<cap>.json} and are matched by the
 * two float predicates registered here.
 *
 * IMPORTANT: the index order of {@link #RODS} and {@link #CAPS} must match
 * the "tc_rod"/"tc_cap" values used in wand.json's overrides.
 */
public final class WandModelPredicates {

    /** Rod tag order; indexes are the tc_rod override values (1-based). */
    public static final List<String> RODS = List.of(
            "wood",
            "greatwood", "silverwood", "obsidian", "ice", "quartz", "reed",
            "blaze", "bone",
            "greatwood_staff", "silverwood_staff", "obsidian_staff",
            "blaze_staff", "ice_staff", "quartz_staff", "reed_staff",
            "bone_staff", "primal_staff");

    /** Cap tag order; indexes are the tc_cap override values (1-based). */
    public static final List<String> CAPS = List.of(
            "iron", "gold", "thaumium", "copper", "silver", "silver_inert",
            "thaumium_inert", "void", "void_inert");

    private WandModelPredicates() {
    }

    public static void register() {
        ItemProperties.register(ModItems.THAUMONOMICON.get(),
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "tc_variant"),
                (stack, level, entity, seed) -> stack.getDamageValue() >= 1 ? 1.0f : 0.0f);
        // NOTE: model override predicate names are parsed with
        // ResourceLocation.parse(), so a bare "tc_rod" would resolve to
        // minecraft:tc_rod and never match the thaumcraft:tc_rod registered
        // here. wand.json must use the namespaced form (it does).
        ItemProperties.register(ModItems.WAND.get(),
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "tc_rod"),
                (stack, level, entity, seed) -> {
                    WandRod rod = WandItem.getRod(stack);
                    String tag = rod == null ? null : rod.getTag();
                    int idx = tag == null ? -1 : RODS.indexOf(tag);
                    return idx < 0 ? 0.0f : idx + 1.0f;
                });
        ItemProperties.register(ModItems.WAND.get(),
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "tc_cap"),
                (stack, level, entity, seed) -> {
                    WandCap cap = WandItem.getCap(stack);
                    String tag = cap == null ? null : cap.getTag();
                    int idx = tag == null ? -1 : CAPS.indexOf(tag);
                    return idx < 0 ? 0.0f : idx + 1.0f;
                });

        // Golem placer: TC4 renders the placer as the golem body of its
        // material (ItemGolemPlacer iconGolem[8]). The port used a single
        // placeholder sprite for every material.
        ItemProperties.register(ModItems.GOLEM_PLACER.get(),
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "tc_golem_material"),
                (stack, level, entity, seed) -> {
                    if (!thaumcraft.common.lib.utils.NBTHelper.hasTag(stack)) return 0.0f;
                    net.minecraft.nbt.CompoundTag tag = thaumcraft.common.lib.utils.NBTHelper.getTag(stack);
                    if (tag == null || !tag.contains("props")) return 0.0f;
                    thaumcraft.api.golems.IGolemProperties props =
                            thaumcraft.common.golems.GolemProperties.fromLong(tag.getLong("props"));
                    thaumcraft.api.golems.parts.GolemMaterial mat = props.getMaterial();
                    if (mat == null || mat.key == null) return 0.0f;
                    // TC4 EnumGolemType order: STRAW, WOOD, TALLOW, CLAY, FLESH,
                    // STONE, IRON, THAUMIUM (material id + 1 in the model overrides).
                    return mat.id + 1.0f;
                });

        // Golem animation cores: TC4 kept all 12 cores in one item and picked
        // the sprite from the metadata (ItemGolemCore.getIconFromDamage).
        // Here the variant is a Data Component, so the model override uses
        // variant id + 1 (0 = the blank base sprite).
        ItemProperties.register(ModItems.GOLEM_CORE.get(),
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "tc_golem_core"),
                (stack, level, entity, seed) -> {
                    thaumcraft.common.golems.GolemCoreType type =
                            thaumcraft.common.golems.ItemGolemCore.getCoreType(stack);
                    return type.isBlank() ? 0.0f : type.getId() + 1.0f;
                });
    }
}
