package thaumcraft.common.lib.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * Generic scanner that gives observation knowledge based on an object's aspects.
 * This is the fallback scanner that handles any entity or item with aspects.
 */
public class ScanGeneric implements IScanThing {

    // Cap observation knowledge per category (5 full theories = 80 raw points)
    private static final int MAX_OBSERVATION_PER_CATEGORY = 5;

    // TC4 Config.aspectTotalCap: soft cap of the research aspect pool per aspect
    private static final int ASPECT_TOTAL_CAP = 100;

    @Override
    public boolean checkThing(Player player, Object obj) {
        if (obj == null) return false;

        AspectList aspects = null;

        if (obj instanceof Entity && !(obj instanceof ItemEntity)) {
            aspects = AspectHelper.getEntityAspects((Entity) obj);
        } else {
            ItemStack stack = ScanningManager.getItemFromParams(player, obj);
            if (stack != null && !stack.isEmpty()) {
                aspects = AspectHelper.getObjectAspects(stack);
            }
        }

        return aspects != null && aspects.size() > 0;
    }

    @Override
    public void onSuccess(Player player, Object obj) {
        if (obj == null) return;

        AspectList aspects = null;

        if (obj instanceof Entity && !(obj instanceof ItemEntity)) {
            aspects = AspectHelper.getEntityAspects((Entity) obj);
        } else {
            ItemStack stack = ScanningManager.getItemFromParams(player, obj);
            if (stack != null && !stack.isEmpty()) {
                aspects = AspectHelper.getObjectAspects(stack);
            }
        }

if (aspects != null) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge == null) return;

            // TC4 ScanManager.validScan: compound aspects can only be learned
            // once their component (parent) aspects are known. When a parent is
            // missing the scan grants nothing and the player is told what to
            // study ("To understand this you need to study <aspect>").
            for (Aspect aspect : aspects.getAspects()) {
                if (aspect == null || aspect.isPrimal()) continue;
                Aspect[] parents = aspect.getComponents();
                if (parents == null) continue;
                for (Aspect parent : parents) {
                    if (parent != null && !knowledge.hasDiscoveredAspect(parent)) {
                        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            serverPlayer.displayClientMessage(
                                    Component.translatable("tc.discoveryerror",
                                            Component.translatable("tc.aspect.help." + parent.getTag())),
                                    true);
                        }
                        return;
                    }
                }
            }

            // TC4 ScanManager.isValidScanTarget/completeScan: scanning an object
            // type that was already scanned gives nothing (and the thaumometer
            // refuses to scan it again).
            String scanKey = getScanKey(player, obj);
            if (scanKey != null && knowledge.isScanned(scanKey)) {
                return;
            }
            if (scanKey != null) {
                knowledge.markScanned(scanKey);
            }

            // Give observation knowledge based on aspects in each category
            for (ResearchCategory category : ResearchCategories.researchCategories.values()) {
                // Only give knowledge if player hasn't maxed out observation for this category
                int currentObs = knowledge.getKnowledge(IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, category.key);
                if (currentObs >= MAX_OBSERVATION_PER_CATEGORY) continue;

                int knowledgeGain = category.applyFormula(aspects);
                if (knowledgeGain > 0) {
                    ThaumcraftApi.internalMethods.addKnowledge(player, 
                            IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, 
                            category, knowledgeGain);
                }
            }

            // TC4 ScanManager.checkAndSyncAspectKnowledge: scanning teaches the
            // aspects of the scanned object. First discovery of an aspect grants
            // a +2 bonus and marks it as discovered (it stays known even when the
            // pool is spent). Pool gains shrink once the per-aspect cap is hit.
            boolean changed = false;
            for (Aspect aspect : aspects.getAspects()) {
                if (aspect == null) continue;
                int amount = aspects.getAmount(aspect);
                if (amount == 0) continue;
                if (!knowledge.hasDiscoveredAspect(aspect)) {
                    knowledge.addDiscoveredAspect(aspect);
                    amount += 2;
                    // TC4 PacketAspectDiscovery notification ("You have discovered X!").
                    // The port has no notification list; use the existing lang key.
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        serverPlayer.displayClientMessage(
                                Component.translatable("tc.addaspect", aspect.getName()), false);
                    }
                }
                int current = knowledge.getResearchPool().getAmount(aspect);
                if (current >= ASPECT_TOTAL_CAP) {
                    amount = (int) Math.sqrt(amount);
                }
                if (amount > 1 && current >= (float) ASPECT_TOTAL_CAP * 1.25F) {
                    amount = 1;
                }
                if (amount != 0) {
                    knowledge.addAspectsToResearchPool(aspect, amount);
                    changed = true;
                }
            }
            if (changed && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                knowledge.sync(serverPlayer);
            }
        }
    }

    /**
     * TC4 ScanManager.isValidScanTarget keying: entities use their type,
     * items/blocks use item registry name + damage (the port's item-stack hash).
     *
     * @return the scanned-object key, or null when the object cannot be keyed
     */
    public static String getScanKey(Player player, Object obj) {
        if (obj == null) return null;
        if (obj instanceof Entity entity && !(obj instanceof ItemEntity)) {
            return "E" + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        }
        ItemStack stack = ScanningManager.getItemFromParams(player, obj);
        if (stack != null && !stack.isEmpty()) {
            return "O" + ResearchManager.createItemStackHash(stack);
        }
        return null;
    }

    @Override
    public String getResearchKey(Player player, Object obj) {
        // Return null to suppress the default "known/unknown object" chat messages
        // ScanGeneric gives observation knowledge directly, not research progression
        return null;
    }
}
