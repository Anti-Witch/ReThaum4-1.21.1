package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.init.ModSounds;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNotes;

import java.util.List;

/**
 * ItemResearchNotes - The TC4 research note item.
 * <p>
 * Damage states (TC4): 0 = written note, 64 = completed note (learn on use).
 * A completed note is used up when the player has all requisites: the research
 * (and any siblings whose requisites are met) is completed and the note is
 * consumed (TC4 ItemResearchNotes).
 *
 * Ported from OpenTC4 ItemResearchNotes (TC4) - hex-grid mechanic is TC4-only.
 *
 * @author Azanor
 */
public class ItemResearchNotes extends Item {

    /** TC4: notes at damage 64 (of 65) are "completed" (discovery icon). */
    public static final int DAMAGE_COMPLETE = 64;

    public ItemResearchNotes() {
        // durability(65): damage 64 = completed (discovery model override).
        // Mirrors TC4's damage-64-based note progression.
        super(new Item.Properties().stacksTo(1).durability(65));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ResearchNoteData rd = ResearchNotes.getData(stack);
            if (rd != null && rd.isComplete() && !isResearchComplete(player, rd.key)) {
                if (ResearchManager.doesPlayerHaveRequisites(player, rd.key)) {
                    ResearchManager.completeResearch(player, rd.key);
                    ResearchEntry re = ResearchCategories.getResearch(rd.key);
                    if (re != null && re.getSiblings() != null) {
                        for (String sibling : re.getSiblings()) {
                            if (!isResearchComplete(player, sibling) && ResearchManager.doesPlayerHaveRequisites(player, sibling)) {
                                ResearchManager.completeResearch(player, sibling);
                            }
                        }
                    }
                    stack.shrink(1);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.LEARN.get(), SoundSource.PLAYERS, 0.75F, 1.0F);
                } else {
                    player.sendSystemMessage(Component.translatable("tc.researcherror"));
                }
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    private static boolean isResearchComplete(Player player, String key) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        return knowledge != null && knowledge.isResearchComplete(key);
    }

    /**
     * True when the note carries a completed research (TC4: damage >= 64).
     */
    public static boolean isComplete(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    /**
     * Color of the aspect this note belongs to, used for the tinted overlay layer.
     */
    public int getColor(ItemStack stack) {
        int c = 0x999999;
        ResearchNoteData rd = ResearchNotes.getData(stack);
        if (rd != null) {
            c = rd.color;
        }
        return c;
    }

    @Override
    public Component getName(ItemStack stack) {
        // TC4: damage < 64 -> "Research Notes", damage >= 64 -> "Discovery"
        return Component.translatable(isComplete(stack)
                ? "item.discovery.name"
                : "item.researchnotes.name");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResearchNoteData rd = ResearchNotes.getData(stack);
        if (rd != null && rd.key != null) {
            ResearchEntry entry = ResearchCategories.getResearch(rd.key);
            if (entry != null) {
                tooltip.add(Component.literal("\u00a76").append(entry.getLocalizedName()));
                if (entry.getStages() != null) {
                    // TC4 adds the first stage text in italics, then the warp line
                    int warp = 0;
                    for (ResearchStage stage : entry.getStages()) {
                        warp += stage.getWarp();
                    }
                    if (entry.getStages().length > 0 && entry.getStages()[0].getText() != null) {
                        tooltip.add(Component.translatable(entry.getStages()[0].getText()).withStyle(ChatFormatting.ITALIC));
                    }
                    if (warp > 0) {
                        if (warp > 5) {
                            warp = 5;
                        }
                        String ws = Component.translatable("tc.forbidden").getString();
                        String wr = Component.translatable("tc.forbidden.level." + warp).getString();
                        tooltip.add(Component.literal("\u00a75" + ws.replaceAll("%n", wr)));
                    }
                }
            }
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }
}
