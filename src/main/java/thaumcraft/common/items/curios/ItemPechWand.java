package thaumcraft.common.items.curios;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.lib.research.ResearchManager;

import java.util.List;

/**
 * Pech Wand - A rare curio that grants research knowledge when used.
 * Can only be used if the player knows basic auromancy.
 * Also unlocks the Pech Focus research if not already known.
 */
public class ItemPechWand extends Item {

    public ItemPechWand() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);

        if (knowledge == null) {
            return InteractionResultHolder.pass(stack);
        }

        // Check if player knows the thaumaturgy basics (TC4 root research)
        if (!knowledge.isResearchKnown("BASICTHAUMATURGY")) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("not.pechwand")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResultHolder.fail(stack);
        }

        // Consume the item
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // Play sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,
                0.5f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f));

        if (!level.isClientSide()) {
            // Unlock Pech Focus research if not known
            if (!knowledge.isResearchKnown("FOCUSPECH")) {
                ResearchManager.completeResearch(player, "FOCUSPECH");
                player.sendSystemMessage(Component.translatable("got.pechwand")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }

            // Grant observation and theory knowledge along the auromancy path
            knowledge.addKnowledge(IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, "THAUMATURGY", 4);
            knowledge.addKnowledge(IPlayerKnowledge.EnumKnowledgeType.THEORY, "THAUMATURGY", 4);

            player.sendSystemMessage(Component.translatable("item.thaumcraft.pech_wand.used")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.curio.text").withStyle(style -> style.withColor(0x808080)));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
