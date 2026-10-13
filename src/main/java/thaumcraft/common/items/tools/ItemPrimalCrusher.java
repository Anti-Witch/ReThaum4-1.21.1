package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.init.ModItems;

import java.util.List;

/**
 * Primal Crusher - A multi-tool that works as both pickaxe and shovel.
 * Made from void metal, it self-repairs and causes warp.
 * Has built-in Destructive and Refining infusion enchantments.
 */
public class ItemPrimalCrusher extends DiggerItem implements IWarpingGear {

    /**
     * Custom tier for the Primal Crusher - based on void metal but enhanced.
     */
    public static final Tier PRIMAL_VOID_TIER = new Tier() {
        @Override
        public int getUses() {
            return 500;
        }

        @Override
        public float getSpeed() {
            return 8.0f;
        }

        @Override
        public float getAttackDamageBonus() {
            return 4.0f;
        }

        @Override
        public int getEnchantmentValue() {
            return 20;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.VOID_METAL_INGOT.get());
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        }
    };

    public ItemPrimalCrusher() {
        super(PRIMAL_VOID_TIER,
                BlockTags.MINEABLE_WITH_PICKAXE,  // Primary tag, but we override getDestroySpeed
                new Item.Properties()
                        .attributes(DiggerItem.createAttributes(PRIMAL_VOID_TIER, 3.5f, -2.8f))
                        .rarity(Rarity.EPIC));
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        // Works on both pickaxe and shovel blocks
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return 5 >= state.getBlock().defaultDestroyTime();
        }
        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // Fast on all pickaxe and shovel blocks
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return PRIMAL_VOID_TIER.getSpeed();
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.VOID_METAL_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }

    /**
     * Self-repair mechanic - repairs 1 durability every 20 ticks.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (stack.isDamaged() && entity != null && entity.tickCount % 20 == 0 && entity instanceof LivingEntity living) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 2;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enchantment.thaumcraft.destructive").withStyle(style -> style.withColor(0x8B4513)));
        tooltip.add(Component.translatable("enchantment.thaumcraft.refining").withStyle(style -> style.withColor(0xFFD700)));
        tooltip.add(Component.translatable("item.thaumcraft.primal_crusher.desc").withStyle(style -> style.withColor(0x808080)));
        tooltip.add(Component.translatable("item.thaumcraft.self_repair").withStyle(style -> style.withColor(0x9400D3)));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
