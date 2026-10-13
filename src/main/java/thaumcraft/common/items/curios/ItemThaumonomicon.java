package thaumcraft.common.items.curios;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.client.gui.screens.ResearchBrowserScreen;
import thaumcraft.common.items.ItemTC;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.init.ModSounds;

import java.util.List;

/**
 * The Thaumonomicon - the player's guide to all things Thaumcraft.
 * <p>
 * TC4 behaviour (ItemThaumonomicon):
 * - Normal (meta 0): right-click completes all siblings of already-completed
 *   research, then opens the research browser.
 * - Cheat (meta 1): right-click completes ALL research and discovers all
 *   aspects (pool 50), then opens the browser.
 * - Plays "thaumcraft:page" on the client.
 */
public class ItemThaumonomicon extends ItemTC {

    public static final String VARIANT_NORMAL = "normal";
    public static final String VARIANT_CHEAT = "cheat";

    public ItemThaumonomicon() {
        super(new Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .component(DataComponents.MAX_STACK_SIZE, 1));
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int variant = getVariant(stack);

        if (level.isClientSide()) {
            // TC4: "thaumcraft:page" on use
            level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                    ModSounds.PAGE.get(), SoundSource.PLAYERS, 1.0f, 1.0f, false);
            openThaumonomiconGui();
            return InteractionResultHolder.success(stack);
        }

        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge != null) {
            if (variant == 1) {
                // Cheat sheet: complete everything, discover every aspect
                for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
                    for (ResearchEntry entry : cat.research.values()) {
                        if (!knowledge.isResearchComplete(entry.getKey())) {
                            ResearchManager.completeResearch(player, entry.getKey());
                        }
                    }
                }
                for (Aspect aspect : Aspect.aspects.values()) {
                    if (!knowledge.hasDiscoveredAspect(aspect)) {
                        knowledge.addDiscoveredAspect(aspect);
                        knowledge.addAspectsToResearchPool(aspect, 50);
                    }
                }
            } else {
                // TC4: complete siblings of completed research
                for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
                    for (ResearchEntry entry : cat.research.values()) {
                        if (knowledge.isResearchComplete(entry.getKey()) && entry.getSiblings() != null) {
                            for (String sibling : entry.getSiblings()) {
                                if (!knowledge.isResearchComplete(sibling)) {
                                    ResearchManager.completeResearch(player, sibling);
                                }
                            }
                        }
                    }
                }
            }
            if (player instanceof ServerPlayer serverPlayer) {
                knowledge.sync(serverPlayer);
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    /**
     * Get the variant from the item stack (0 = normal, 1 = cheater)
     */
    public static int getVariant(ItemStack stack) {
        return stack.getDamageValue() >= 1 ? 1 : 0;
    }

    /**
     * Create a stack with the specified variant.
     */
    public static ItemStack createStack(int variant) {
        ItemStack stack = new ItemStack(thaumcraft.init.ModItems.THAUMONOMICON.get());
        stack.setDamageValue(variant);
        return stack;
    }

    /**
     * Open the Thaumonomicon GUI on the client.
     * Must be called only on the client side.
     */
    @OnlyIn(Dist.CLIENT)
    private void openThaumonomiconGui() {
        Minecraft.getInstance().setScreen(new ResearchBrowserScreen());
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int variant = getVariant(stack);
        if (variant == 0) {
            tooltip.add(Component.translatable("item.thaumcraft.thaumonomicon.desc"));
        } else {
            // TC4: "Cheat Sheet"
            tooltip.add(Component.literal("Cheat Sheet"));
            tooltip.add(Component.literal("Right-click to unlock all research").withStyle(ChatFormatting.GRAY));
        }
    }

    public Component getName(ItemStack stack) {
        int variant = getVariant(stack);
        if (variant == 1) {
            return Component.translatable("item.thaumcraft.thaumonomicon.cheat");
        }
        return Component.translatable("item.thaumcraft.thaumonomicon");
    }

    public Rarity getRarity(ItemStack stack) {
        // TC4: uncommon normal / epic cheat
        return getVariant(stack) == 1 ? Rarity.EPIC : Rarity.UNCOMMON;
    }

    public boolean isFoil(ItemStack stack) {
        return getVariant(stack) == 1;
    }
}
