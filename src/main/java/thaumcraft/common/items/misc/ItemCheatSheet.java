package thaumcraft.common.items.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.common.lib.CommandThaumcraft;

import java.util.List;

/**
 * Cheat Sheet ("Фоліант Всезнання") - a debug/cheat item that instantly:
 *  1) Unlocks ALL Thaumcraft research for the player (including addon research).
 *  2) Fills the player's Research Table aspect pool with 9999 of every aspect.
 *     This is the AspectList bound to the player (IPlayerKnowledge), NOT essentia
 *     phials and NOT wand vis.
 *
 * Activated by right-click (PКМ). Obtainable via /give @p thaumcraft:cheat_sheet
 */
public class ItemCheatSheet extends Item {

    public ItemCheatSheet() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Client side: nothing to compute, just acknowledge the use.
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        ServerPlayer sp = (ServerPlayer) player;

        // 1) Reveal every research entry (and its prerequisites), including addons.
        for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry ri : cat.research.values()) {
                CommandThaumcraft.giveRecursiveResearch(sp, ri.getKey());
            }
        }

        // 2) Fill the player's Research Table aspect pool with 9999 of every aspect.
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(sp);
        if (knowledge != null) {
            for (Aspect aspect : Aspect.aspects.values()) {
                knowledge.addAspectsToResearchPool(aspect, 9999);
            }
            knowledge.sync(sp);
        }

        player.sendSystemMessage(Component.translatable("item.thaumcraft.cheat_sheet.unlock"));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumcraft.cheat_sheet.desc"));
    }
}
