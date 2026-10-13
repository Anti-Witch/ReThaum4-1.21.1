package thaumcraft.common.items.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.common.items.consumables.ItemPhial;
import thaumcraft.common.tiles.essentia.TileJar;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Thaumostatic Harness (TC4 ItemHoverHarness).
 *
 * Chestplate that opens the door to flight. TC4 stored a filled warded jar
 * inside the harness (via a GUI) and consumed its Potentia (Aspect.ENERGY)
 * while hovering. The port has no filled-jar item yet, so the harness keeps
 * its own Potentia reserve which is loaded from warded jar blocks or filled
 * Potentia phials.
 *
 * TC4 details:
 * - armorMatSpecial, max damage 400, repaired with gold ingots
 * - vis discount: 2% general, 5% for Aer
 * - onArmorTick -> Hover.handleHoverArmor (only outside creative)
 * - right click opened container id 17 (jar slot)
 */
public class ItemHoverHarness extends ArmorItem implements IVisDiscountGear {

    public ItemHoverHarness() {
        super(ThaumcraftMaterials.ARMORMAT_SPECIAL, Type.CHESTPLATE,
                new Item.Properties()
                        .stacksTo(1)
                        .durability(400)
                        .rarity(Rarity.EPIC));
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                            ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.parse("thaumcraft:textures/models/hoverharness.png");
    }

    /** TC4 getIsRepairable: gold ingots. */
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.GOLD_INGOT) || super.isValidRepairItem(toRepair, repair);
    }

    // ==================== Vis discount ====================

    /**
     * TC4 ItemHoverHarness.getVisDiscount: 5% for Aer, 2% for everything else.
     * The port's IVisDiscountGear has no aspect parameter, so the general value
     * is returned here; the tooltip shows both TC4 values.
     */
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return 2;
    }

    // ==================== Tick ====================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) {
            return;
        }
        // TC4 onArmorTick only ran for the worn chestplate
        if (player.getItemBySlot(EquipmentSlot.CHEST) != stack) {
            return;
        }
        Hover.handleHoverArmor(player, stack);
    }

    // ==================== Filling / emptying (TC4 jar GUI replacement) ====================

    /**
     * TC4 opened a GUI to insert a filled jar. With no filled-jar item in the
     * port, the harness is filled by clicking a warded jar holding Potentia
     * (shift-click pours the fuel back out).
     *
     * Implemented in onItemUseFirst because warded jars consume the default
     * block interaction before Item#useOn would ever be reached.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack held, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(pos) instanceof TileJar jar)) {
            return InteractionResult.PASS;
        }
        // Server-authoritative: the server applies the transfer and syncs the
        // jar and the harness back to the client.
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }
        // Only jars holding (or accepting) Potentia are of interest
        if (jar.getAspect() != null && jar.getAspect() != Aspect.ENERGY) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            // Pour the harness' fuel back into the jar
            int fuel = Hover.getFuel(held);
            if (fuel <= 0 || !jar.doesContainerAccept(Aspect.ENERGY)) {
                return InteractionResult.PASS;
            }
            int remainder = jar.addToContainer(Aspect.ENERGY, fuel);
            int moved = fuel - remainder;
            if (moved <= 0) {
                return InteractionResult.PASS;
            }
            Hover.setFuel(held, fuel - moved);
            playJarSound(level, pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Fill the harness from the jar
        int available = jar.getAspect() == Aspect.ENERGY ? jar.getAmount() : 0;
        int room = Hover.JAR_CAPACITY - Hover.getFuel(held);
        int moved = Math.min(room, available);
        if (moved <= 0) {
            return InteractionResult.PASS;
        }
        if (jar.takeFromContainer(Aspect.ENERGY, moved)) {
            Hover.addFuel(held, moved);
            playJarSound(level, pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * Filling from a Potentia phial held in the other hand (a portable stand-in
     * for the TC4 jar GUI).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);

        if (other.getItem() instanceof ItemPhial phial && phial.getAspect(other) == Aspect.ENERGY) {
            int amount = phial.getAmount(other);
            if (amount > 0 && Hover.getFuel(held) < Hover.JAR_CAPACITY) {
                // Server-authoritative: the server moves the Potentia and syncs the stacks
                if (!level.isClientSide) {
                    int added = Hover.addFuel(held, amount);
                    if (added > 0) {
                        other.shrink(1);
                        ItemStack empty = new ItemStack(ModItems.PHIAL_EMPTY.get());
                        if (other.isEmpty()) {
                            player.setItemInHand(otherHand, empty);
                        } else if (!player.getInventory().add(empty)) {
                            player.drop(empty, false);
                        }
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                ModSounds.JAR.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
                    }
                }
                return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
            }
        }
        return InteractionResultHolder.pass(held);
    }

    private static void playJarSound(Level level, BlockPos pos) {
        TileJar.playJarSound(level, pos, 0.4F);
    }

    // ==================== Tooltip ====================

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int fuel = Hover.getFuel(stack);
        tooltip.add(Component.translatable("item.thaumcraft.hover_harness.fuel", fuel, Hover.JAR_CAPACITY)
                .withStyle(fuel > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.thaumcraft.hover_harness.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.thaumcraft.vis_discount", 2)
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.thaumcraft.hover_harness.vis_discount_aer", 5)
                .withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
