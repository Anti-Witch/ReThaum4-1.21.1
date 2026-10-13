package thaumcraft.common.items.curios;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.common.items.ItemTCBase;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * Folio of Omniscience - a one-use tome that instantly completes every
 * research entry in the Thaumonomicon and floods the holder's research pool
 * with 9999 of each aspect (used by the Research Table minigame).
 * Consumed on use.
 */
public class ItemFolioOmniscience extends ItemTCBase {

    public ItemFolioOmniscience() {
        super(new Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);

            // 1. Complete all research entries (without popup spam)
            ResearchManager.noFlags = true;
            try {
                for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
                    for (ResearchEntry entry : cat.research.values()) {
                        ResearchManager.completeResearch(player, entry.getKey(), false);
                    }
                }
            } finally {
                ResearchManager.noFlags = false;
            }

            // 2. Flood the research pool with 9999 of every aspect
            for (Aspect aspect : Aspect.aspects.values()) {
                if (aspect != null) {
                    knowledge.addAspectsToResearchPool(aspect, 9999);
                }
            }

            knowledge.sync(serverPlayer);

            // 3. Consume (one-time use)
            stack.shrink(1);

            level.playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            player.displayClientMessage(
                    Component.translatable("tc.folio.used"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
