package thaumcraft.api.capabilities;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.capabilities.PlayerKnowledge;
import thaumcraft.common.lib.capabilities.PlayerWarp;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * ThaumcraftCapabilities - Access point for Thaumcraft's player data attachments.
 * 
 * Ported to NeoForge 1.21.1
 */
public class ThaumcraftCapabilities {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Thaumcraft.MODID);

    /**
     * The attachment type for IPlayerKnowledge
     */
    public static final Supplier<AttachmentType<PlayerKnowledge.DefaultImpl>> KNOWLEDGE =
            ATTACHMENT_TYPES.register("knowledge", () ->
                    AttachmentType.serializable(PlayerKnowledge.DefaultImpl::new)
                            .copyOnDeath()
                            .build()
            );

    /**
     * The attachment type for IPlayerWarp
     */
    public static final Supplier<AttachmentType<PlayerWarp.DefaultImpl>> WARP =
            ATTACHMENT_TYPES.register("warp", () ->
                    AttachmentType.serializable(PlayerWarp.DefaultImpl::new)
                            .copyOnDeath()
                            .build()
            );

    /**
     * Retrieves the knowledge handler for the supplied player.
     * @param player The player to get knowledge for
     * @return The knowledge handler
     */
    @Nonnull
    public static IPlayerKnowledge getKnowledge(@Nonnull Player player) {
        return player.getData(KNOWLEDGE.get());
    }

    /**
     * Retrieves the warp handler for the supplied player.
     * @param player The player to get warp for
     * @return The warp handler
     */
    @Nonnull
    public static IPlayerWarp getWarp(@Nonnull Player player) {
        return player.getData(WARP.get());
    }

    /**
     * Shortcut method to check if player knows the passed research entries.
     */
    public static boolean knowsResearch(@Nonnull Player player, @Nonnull String... research) {
        IPlayerKnowledge knowledge = getKnowledge(player);
        for (String r : research) {
            if (r.contains("&&")) {
                String[] rr = r.split("&&");
                if (!knowsResearch(player, rr)) return false;
            } else if (r.contains("||")) {
                String[] rr = r.split("\\|\\|");
                boolean anyTrue = false;
                for (String str : rr) {
                    if (knowsResearch(player, str)) {
                        anyTrue = true;
                        break;
                    }
                }
                if (!anyTrue) return false;
            } else {
                if (!knowledge.isResearchKnown(r)) return false;
            }
        }
        return true;
    }

    /**
     * Shortcut method to check if player knows all the passed research entries.
     */
    public static boolean knowsResearchStrict(@Nonnull Player player, @Nonnull String... research) {
        IPlayerKnowledge knowledge = getKnowledge(player);
        for (String r : research) {
            if (r.contains("&&")) {
                String[] rr = r.split("&&");
                if (!knowsResearchStrict(player, rr)) return false;
            } else if (r.contains("||")) {
                String[] rr = r.split("\\|\\|");
                boolean anyTrue = false;
                for (String str : rr) {
                    if (knowsResearchStrict(player, str)) {
                        anyTrue = true;
                        break;
                    }
                }
                if (!anyTrue) return false;
            } else if (r.contains("@")) {
                if (!knowledge.isResearchKnown(r)) return false;
            } else {
                if (!knowledge.isResearchComplete(r)) return false;
            }
        }
        return true;
    }

    /**
     * Simple check if player knows a specific research.
     */
    public static boolean isResearchKnown(@Nonnull Player player, @Nonnull String researchKey) {
        return getKnowledge(player).isResearchKnown(researchKey);
    }

    public static boolean isResearchComplete(@Nonnull Player player, @Nonnull String researchKey) {
        return getKnowledge(player).isResearchComplete(researchKey);
    }
}
