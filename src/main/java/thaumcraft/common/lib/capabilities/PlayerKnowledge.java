package thaumcraft.common.lib.capabilities;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;

/**
 * PlayerKnowledge - Implementation of the IPlayerKnowledge capability.
 * Tracks all research progress and knowledge points for a player.
 * 
 * @author Azanor
 * Ported to NeoForge 1.21.1
 */
public class PlayerKnowledge {
    
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "knowledge");
    
    /**
     * Default implementation of IPlayerKnowledge
     */
    public static class DefaultImpl implements IPlayerKnowledge {
        
        private final HashSet<String> research = new HashSet<>();
        private final Map<String, Integer> stages = new HashMap<>();
        private final Map<String, HashSet<EnumResearchFlag>> flags = new HashMap<>();
        private final Map<String, Integer> knowledge = new HashMap<>();
        private final AspectList aspectPool = new AspectList();
        /** TC4 objectsScanned/entitiesScanned/phenomenaScanned (prefixed keys). */
        private final HashSet<String> scanned = new HashSet<>();

        @Override
        public void clear() {
            research.clear();
            flags.clear();
            stages.clear();
            knowledge.clear();
            aspectPool.aspects.clear();
            scanned.clear();
        }
        
        @Override
        public EnumResearchStatus getResearchStatus(@Nonnull String res) {
            if (!isResearchKnown(res)) {
                return EnumResearchStatus.UNKNOWN;
            }
            // TC6 semantics: research with no stages (and pseudo-research like
            // "!gotdream") is complete as soon as it is known. Staged research
            // is complete once its stage reaches the total stage count.
            int stage = getResearchStage(res);
            thaumcraft.api.research.ResearchEntry entry = thaumcraft.api.research.ResearchCategories.getResearch(res);
            int total = (entry != null && entry.getStages() != null) ? entry.getStages().length : 0;
            return (total == 0 || stage >= total) ? EnumResearchStatus.COMPLETE : EnumResearchStatus.IN_PROGRESS;
        }
        
        @Override
        public boolean isResearchKnown(String res) {
            if (res == null) {
                return false;
            }
            if (res.isEmpty()) {
                return true;
            }
            String[] ss = res.split("@");
            return (ss.length <= 1 || getResearchStage(ss[0]) >= parseInt(ss[1], 0)) && research.contains(ss[0]);
        }
        
        private int parseInt(String s, int defaultValue) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        @Override
        public boolean isResearchComplete(String res) {
            return getResearchStatus(res) == EnumResearchStatus.COMPLETE;
        }
        
        @Override
        public int getResearchStage(String res) {
            if (res == null || !research.contains(res)) {
                return -1;
            }
            Integer stage = stages.get(res);
            return (stage == null) ? 0 : stage;
        }
        
        @Override
        public boolean setResearchStage(String res, int stage) {
            if (res == null || !research.contains(res) || stage <= 0) {
                return false;
            }
            stages.put(res, stage);
            return true;
        }
        
        @Override
        public boolean addResearch(@Nonnull String res) {
            if (!isResearchKnown(res)) {
                research.add(res);
                return true;
            }
            return false;
        }
        
        @Override
        public boolean removeResearch(@Nonnull String res) {
            if (isResearchKnown(res)) {
                research.remove(res);
                stages.remove(res);
                flags.remove(res);
                return true;
            }
            return false;
        }
        
        @Nonnull
        @Override
        public Set<String> getResearchList() {
            return Collections.unmodifiableSet(research);
        }
        
        @Override
        public boolean setResearchFlag(@Nonnull String res, @Nonnull EnumResearchFlag flag) {
            HashSet<EnumResearchFlag> list = flags.computeIfAbsent(res, k -> new HashSet<>());
            if (list.contains(flag)) {
                return false;
            }
            list.add(flag);
            return true;
        }
        
        @Override
        public boolean clearResearchFlag(@Nonnull String res, @Nonnull EnumResearchFlag flag) {
            HashSet<EnumResearchFlag> list = flags.get(res);
            if (list != null) {
                boolean b = list.remove(flag);
                if (list.isEmpty()) {
                    flags.remove(res);
                }
                return b;
            }
            return false;
        }
        
        @Override
        public boolean hasResearchFlag(@Nonnull String res, @Nonnull EnumResearchFlag flag) {
            return flags.get(res) != null && flags.get(res).contains(flag);
        }
        
        private String getKey(EnumKnowledgeType type, String category) {
            return type.getAbbreviation() + "_" + (category == null ? "" : category);
        }
        
        @Override
        public boolean addKnowledge(EnumKnowledgeType type, String category, int amount) {
            String key = getKey(type, category);
            int c = getKnowledgeRaw(type, category);
            if (c + amount < 0) {
                return false;
            }
            c += amount;
            knowledge.put(key, c);
            return true;
        }
        
        @Override
        public int getKnowledge(EnumKnowledgeType type, String category) {
            String key = getKey(type, category);
            int c = knowledge.getOrDefault(key, 0);
            return (int) Math.floor(c / (double) type.getProgression());
        }
        
        @Override
        public int getKnowledgeRaw(EnumKnowledgeType type, String category) {
            String key = getKey(type, category);
            return knowledge.getOrDefault(key, 0);
        }

        @Override
        public void addAspectsToResearchPool(Aspect aspect, int amount) {
            if (aspect == null) return;
            // TC4 semantics: the aspect entry doubles as the "discovered" marker,
            // so a spent-out aspect stays in the list with amount 0 instead of
            // being removed (see hasDiscoveredAspect).
            int newAmount = Math.max(0, aspectPool.getAmount(aspect) + amount);
            aspectPool.aspects.put(aspect, newAmount);
        }

        @Override
        public void addDiscoveredAspect(Aspect aspect) {
            if (aspect == null) return;
            if (!aspectPool.aspects.containsKey(aspect)) {
                aspectPool.add(aspect, 0);
            }
        }

        /**
         * TC4 getAspectsDiscovered: the six primal aspects are always considered
         * discovered, so make sure they are present in the pool list.
         */
        private void ensurePrimalDiscovered() {
            if (aspectPool.size() <= 6) {
                for (Aspect primal : Aspect.getPrimalAspects()) {
                    if (!aspectPool.aspects.containsKey(primal)) {
                        aspectPool.add(primal, 0);
                    }
                }
            }
        }

        @Override
        public boolean hasDiscoveredAspect(Aspect aspect) {
            if (aspect == null) return false;
            if (aspect.isPrimal()) return true;
            return aspectPool.aspects.containsKey(aspect);
        }

        @Override
        public boolean isScanned(String key) {
            return key != null && scanned.contains(key);
        }

        @Override
        public void markScanned(String key) {
            if (key != null) {
                scanned.add(key);
            }
        }

        @Override
        public AspectList getResearchPool() {
            ensurePrimalDiscovered();
            return aspectPool;
        }
        
        @Override
        public void sync(@Nonnull ServerPlayer player) {
            thaumcraft.common.lib.network.PacketHandler.sendToPlayer(
                new thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge(player), 
                player
            );
        }
        
        public CompoundTag serializeNBT() {
            CompoundTag rootTag = new CompoundTag();
            
            ListTag researchList = new ListTag();
            for (String resKey : research) {
                CompoundTag tag = new CompoundTag();
                tag.putString("key", resKey);
                if (stages.containsKey(resKey)) {
                    tag.putInt("stage", stages.get(resKey));
                }
                if (flags.containsKey(resKey)) {
                    HashSet<EnumResearchFlag> list = flags.get(resKey);
                    if (list != null && !list.isEmpty()) {
                        StringBuilder fs = new StringBuilder();
                        for (EnumResearchFlag flag : list) {
                            if (fs.length() > 0) {
                                fs.append(",");
                            }
                            fs.append(flag.name());
                        }
                        tag.putString("flags", fs.toString());
                    }
                }
                researchList.add(tag);
            }
            rootTag.put("research", researchList);
            
            ListTag knowledgeList = new ListTag();
            for (Map.Entry<String, Integer> entry : knowledge.entrySet()) {
                String key = entry.getKey();
                Integer c = entry.getValue();
                if (c != null && c > 0 && key != null && !key.isEmpty()) {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("key", key);
                    tag.putInt("amount", c);
                    knowledgeList.add(tag);
                }
            }
            rootTag.put("knowledge", knowledgeList);

            // Research aspect pool (AspectList bound to the player)
            aspectPool.writeToNBT(rootTag, "aspectPool");

            // TC4 scanned objects/entities (prefixed keys)
            ListTag scannedList = new ListTag();
            for (String key : scanned) {
                CompoundTag tag = new CompoundTag();
                tag.putString("key", key);
                scannedList.add(tag);
            }
            rootTag.put("scanned", scannedList);

            return rootTag;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            return serializeNBT();
        }
        
        public void deserializeNBT(CompoundTag rootTag) {
            if (rootTag == null) {
                return;
            }
            clear();
            
            ListTag researchList = rootTag.getList("research", Tag.TAG_COMPOUND);
            for (int i = 0; i < researchList.size(); i++) {
                CompoundTag tag = researchList.getCompound(i);
                String know = tag.getString("key");
                if (know != null && !know.isEmpty() && !isResearchKnown(know)) {
                    research.add(know);
                    int stage = tag.getInt("stage");
                    if (stage > 0) {
                        stages.put(know, stage);
                    }
                    String fs = tag.getString("flags");
                    if (!fs.isEmpty()) {
                        String[] ss = fs.split(",");
                        for (String s : ss) {
                            try {
                                EnumResearchFlag flag = EnumResearchFlag.valueOf(s);
                                setResearchFlag(know, flag);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                }
            }
            
            ListTag knowledgeList = rootTag.getList("knowledge", Tag.TAG_COMPOUND);
            for (int j = 0; j < knowledgeList.size(); j++) {
                CompoundTag tag = knowledgeList.getCompound(j);
                String key = tag.getString("key");
                int amount = tag.getInt("amount");
                if (key != null && !key.isEmpty()) {
                    knowledge.put(key, amount);
                }
            }

            // Research aspect pool (AspectList bound to the player)
            aspectPool.readFromNBT(rootTag, "aspectPool");

            // TC4 scanned objects/entities (prefixed keys)
            ListTag scannedList = rootTag.getList("scanned", Tag.TAG_COMPOUND);
            for (int k = 0; k < scannedList.size(); k++) {
                String key = scannedList.getCompound(k).getString("key");
                if (key != null && !key.isEmpty()) {
                    scanned.add(key);
                }
            }
            
            // TODO: Add auto-unlock research when ResearchCategories is implemented
            // addAutoUnlockResearch();
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag rootTag) {
            deserializeNBT(rootTag);
        }
    }
}
