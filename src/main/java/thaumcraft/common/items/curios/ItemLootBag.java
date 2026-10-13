package thaumcraft.common.items.curios;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.init.ModSounds;

import java.util.List;

/**
 * Loot Bag - Contains random Thaumcraft loot when opened.
 * Comes in three tiers: Common, Uncommon, and Rare.
 */
public class ItemLootBag extends Item {

    public enum LootTier {
        COMMON(Rarity.COMMON),
        UNCOMMON(Rarity.UNCOMMON),
        RARE(Rarity.RARE);

        private final Rarity rarity;

        LootTier(Rarity rarity) {
            this.rarity = rarity;
        }

        public Rarity getRarity() {
            return rarity;
        }
    }

    private final LootTier tier;

    public ItemLootBag(LootTier tier) {
        super(new Item.Properties()
                .stacksTo(16)
                .rarity(tier.getRarity()));
        this.tier = tier;
    }

    public static ItemLootBag createCommon() {
        return new ItemLootBag(LootTier.COMMON);
    }

    public static ItemLootBag createUncommon() {
        return new ItemLootBag(LootTier.UNCOMMON);
    }

    public static ItemLootBag createRare() {
        return new ItemLootBag(LootTier.RARE);
    }

    public LootTier getTier() {
        return tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // Generate random loot based on tier
            int itemCount = 8 + level.random.nextInt(5);

            for (int i = 0; i < itemCount; i++) {
                ItemStack loot = generateLoot(level, tier);
                if (loot != null && !loot.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(level,
                            player.getX(), player.getY(), player.getZ(),
                            loot.copy());
                    level.addFreshEntity(itemEntity);
                }
            }

            player.playSound(ModSounds.COINS.get(), 0.5f, 1.0f);
        }

        stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Generate random loot based on tier.
     * Based on TC4 loot tables (Config.java:529-584) using available items.
     */
    private ItemStack generateLoot(Level level, LootTier tier) {
        var rand = level.random;
        int roll = rand.nextInt(10000); // 0-9999 for weighted rolls
        
        // Weighted loot tables matching TC4 using available items
        return switch (tier) {
            case COMMON -> {
                if (roll < 2500) yield new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET, 1 + rand.nextInt(4));
                if (roll < 2600) yield new ItemStack(net.minecraft.world.item.Items.DIAMOND, 1);
                if (roll < 2750) yield new ItemStack(net.minecraft.world.item.Items.EMERALD, 1);
                // TC6-removed: if (roll < 3050) yield new ItemStack(thaumcraft.init.ModItems.PRIMAL_CHARM.get(), 1);
                if (roll < 3300) yield new ItemStack(thaumcraft.init.ModItems.RESEARCH_NOTES.get(), 1);
                if (roll < 3400) yield new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 1 + rand.nextInt(3));
                if (roll < 3500) yield new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL, 1 + rand.nextInt(3));
                if (roll < 3550) yield new ItemStack(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, 1 + rand.nextInt(2));
                if (roll < 3580) yield new ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE, 1);
                if (roll < 3590) yield new ItemStack(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE, 1);
                yield new ItemStack(thaumcraft.init.ModItems.SALIS_MUNDUS.get(), 1 + rand.nextInt(2));
            }
            case UNCOMMON -> {
                if (roll < 2250) yield new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 2);
                if (roll < 2750) yield new ItemStack(net.minecraft.world.item.Items.DIAMOND, 1 + rand.nextInt(2));
                if (roll < 3500) yield new ItemStack(net.minecraft.world.item.Items.EMERALD, 1 + rand.nextInt(2));
                if (roll < 4500) yield new ItemStack(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, 1 + rand.nextInt(3));
                if (roll < 5100) yield new ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE, 1);
                if (roll < 5300) yield new ItemStack(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE, 1);
                if (roll < 5800) yield new ItemStack(thaumcraft.init.ModItems.AMULET_VIS_CRAFTED.get(), 1);
                if (roll < 6400) yield new ItemStack(thaumcraft.init.ModItems.TRAVELLER_BOOTS.get(), 1);
                // TC6-removed: PRIMAL_CHARM removed
                yield new ItemStack(thaumcraft.init.ModItems.SALIS_MUNDUS.get(), 1);
            }
            case RARE -> {
                if (roll < 2000) yield new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 5);
                if (roll < 2100) yield new ItemStack(net.minecraft.world.item.Items.NETHER_STAR, 1);
                if (roll < 2200) yield new ItemStack(thaumcraft.init.ModItems.VOID_SEED.get(), 1);
                if (roll < 2700) yield new ItemStack(net.minecraft.world.item.Items.DIAMOND, 1 + rand.nextInt(3));
                if (roll < 3600) yield new ItemStack(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE, 1);
                if (roll < 3900) yield new ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE, 1);
                // TC6-removed: if (roll < 6000) yield new ItemStack(thaumcraft.init.ModItems.PRIMAL_CHARM.get(), 1 + rand.nextInt(3));
                if (roll < 6250) yield generateRareGear(level);
                yield new ItemStack(thaumcraft.init.ModItems.PRIMORDIAL_PEARL.get(), 1);
            }
        };
    }
    
    /**
     * Generate rare gear (tools/armor/baubles) like TC4's generateLoot
     */
    private ItemStack generateRareGear(Level level) {
        var rand = level.random;
        int type = rand.nextInt(8);
        return switch (type) {
            case 0 -> new ItemStack(thaumcraft.init.ModItems.THAUMIUM_SWORD.get());
            case 1 -> new ItemStack(thaumcraft.init.ModItems.THAUMIUM_PICK.get());
            case 2 -> new ItemStack(thaumcraft.init.ModItems.THAUMIUM_AXE.get());
            case 3 -> new ItemStack(thaumcraft.init.ModItems.VOID_SWORD.get());
            case 4 -> new ItemStack(thaumcraft.init.ModItems.AMULET_VIS_CRAFTED.get());
            case 5 -> new ItemStack(thaumcraft.init.ModItems.GOGGLES.get());
            case 6 -> new ItemStack(thaumcraft.init.ModItems.TRAVELLER_BOOTS.get());
            default -> new ItemStack(thaumcraft.init.ModItems.CLOUD_RING.get());
        };
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tc.lootbag").withStyle(style -> style.withColor(0x808080)));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
