package thaumcraft.common.lib.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.TagParser;
import thaumcraft.common.lib.utils.NBTHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.core.registries.BuiltInRegistries;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.api.research.*;
import thaumcraft.common.lib.events.PlayerEvents;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ResearchManager - Manages all research-related operations.
 * 
 * Responsibilities:
 * - Parse research JSON files
 * - Progress/complete research for players
 * - Award knowledge points
 * - Track crafting references for research triggers
 * 
 * Ported from 1.12.2 with modernized JSON parsing and item handling.
 */
public class ResearchManager {
    
    // Players that need their knowledge synced (thread-safe)
    public static ConcurrentHashMap<String, Boolean> syncList = new ConcurrentHashMap<>();
    
    // Flag to suppress popups during certain operations
    public static boolean noFlags = false;

    // Crafting item hashes that trigger research progress
    public static LinkedHashSet<Integer> craftingReferences = new LinkedHashSet<>();

    // Map: item hash -> set of research keys that progress when this item is crafted
    public static Map<Integer, Set<String>> craftingReferencesMap = new HashMap<>();

    // ==================== Knowledge Management ====================
    
    /**
     * Add knowledge points to a player.
     * @param player The player
     * @param type The knowledge type (THEORY, OBSERVATION)
     * @param category The research category (can be null)
     * @param amount Raw amount to add
     * @return true if successful
     */
    public static boolean addKnowledge(Player player, IPlayerKnowledge.EnumKnowledgeType type, 
                                       ResearchCategory category, int amount) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge == null) return false;
        
        String catKey = category != null ? category.key : null;
        if (!type.hasFields()) {
            catKey = null;
        }
        
        // TODO: Fire ResearchEvent.Knowledge event when event system is implemented
        // if (NeoForge.EVENT_BUS.post(new ResearchEvent.Knowledge(player, type, category, amount))) {
        //     return false;
        // }
        
        int before = knowledge.getKnowledge(type, catKey);
        knowledge.addKnowledge(type, catKey, amount);
        int gained = knowledge.getKnowledge(type, catKey) - before;

        // Send knowledge gain packet for visual/sound feedback (restored 2026-08-09)
        if (amount > 0 && gained > 0 && player instanceof ServerPlayer serverPlayer) {
            for (int a = 0; a < gained; a++) {
                thaumcraft.common.lib.network.PacketHandler.sendToPlayer(
                        new thaumcraft.common.lib.network.misc.PacketKnowledgeGain(type, catKey), serverPlayer);
            }
        }
        
        syncList.put(player.getName().getString(), true);
        return true;
    }
    
    // ==================== Research Progress ====================

    /**
     * Complete all stages of a research entry.
     */
    public static boolean completeResearch(Player player, String researchKey) {
        return completeResearch(player, researchKey, true);
    }

    /**
     * Complete all stages of a research entry.
     */
    public static boolean completeResearch(Player player, String researchKey, boolean sync) {
        boolean success = false;
        while (progressResearch(player, researchKey, sync)) {
            success = true;
        }
        if (success && !player.level().isClientSide) {
            // Mark player for knowledge sync to client
            thaumcraft.common.lib.events.PlayerEvents.markForSync(player);
        }
        return success;
    }
    
    /**
     * TC4 {@code EventHandlerEntity}: every {@code AutoUnlock} research is
     * completed as soon as the player data is loaded, so the root entries
     * (BASICTHAUMATURGY, CRUCIBLE, THAUMOMETER, ...) are known without ever
     * solving a research note. Parents are resolved over several passes so
     * auto-unlock chains (RESEARCH -> THAUMONOMICON -> ...) work regardless of
     * the iteration order of the category maps.
     */
    public static void unlockAutoResearch(Player player) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge == null) return;

        boolean progress = true;
        int guard = 0;
        while (progress && guard++ < 16) {
            progress = false;
            for (ResearchCategory category : ResearchCategories.researchCategories.values()) {
                for (ResearchEntry entry : category.research.values()) {
                    if (entry.hasMeta(ResearchEntry.EnumResearchMeta.AUTOUNLOCK)
                            && !knowledge.isResearchComplete(entry.getKey())
                            && doesPlayerHaveRequisites(player, entry.getKey())) {
                        if (completeResearch(player, entry.getKey(), false)) {
                            progress = true;
                        }
                    }
                }
            }
        }
    }

    /**
     * Start research and show popup notification.
     */
    public static boolean startResearchWithPopup(Player player, String researchKey) {
        boolean success = progressResearch(player, researchKey, true);
        if (success) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge != null) {
                knowledge.setResearchFlag(researchKey, IPlayerKnowledge.EnumResearchFlag.POPUP);
                knowledge.setResearchFlag(researchKey, IPlayerKnowledge.EnumResearchFlag.RESEARCH);
            }
        }
        return success;
    }

    public static boolean progressResearch(Player player, String researchKey) {
        return progressResearch(player, researchKey, true);
    }
    
    /**
     * Progress research by one stage.
     * @param player The player
     * @param researchKey The research key
     * @param sync Whether to sync to client
     * @return true if progress was made
     */
    public static boolean progressResearch(Player player, String researchKey, boolean sync) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge == null) return false;
        
        // Already complete or missing prerequisites
        if (knowledge.isResearchComplete(researchKey) || !doesPlayerHaveRequisites(player, researchKey)) {
            return false;
        }
        
        // TODO: Fire ResearchEvent.Research event
        // if (NeoForge.EVENT_BUS.post(new ResearchEvent.Research(player, researchKey))) {
        //     return false;
        // }
        
        // Add research if not known
        if (!knowledge.isResearchKnown(researchKey)) {
            knowledge.addResearch(researchKey);
        }
        
        ResearchEntry entry = ResearchCategories.getResearch(researchKey);
        if (entry != null) {
            boolean showPopups = true;
            
            if (entry.getStages() != null && entry.getStages().length > 0) {
                int currentStage = knowledge.getResearchStage(researchKey);
                ResearchStage stage = null;
                
                if (currentStage > 0) {
                    currentStage = Math.min(currentStage, entry.getStages().length);
                    stage = entry.getStages()[currentStage - 1];
                }
                
                // Auto-progress stages with no requirements
                if (entry.getStages().length == 1 && currentStage == 0 && isStageAutoComplete(entry.getStages()[0])) {
                    currentStage++;
                } else if (entry.getStages().length > 1 && entry.getStages().length <= currentStage + 1 
                           && currentStage < entry.getStages().length && isStageAutoComplete(entry.getStages()[currentStage])) {
                    currentStage++;
                }
                
                knowledge.setResearchStage(researchKey, Math.min(entry.getStages().length + 1, currentStage + 1));
                showPopups = (currentStage >= entry.getStages().length);
                
                // Handle warp from research
                int warp = 0;
                if (stage != null) {
                    warp = stage.getWarp();
                }
                if (showPopups && currentStage > 0) {
                    currentStage = Math.min(currentStage, entry.getStages().length);
                    stage = entry.getStages()[currentStage - 1];
                }
                if (stage != null) {
                    warp += stage.getWarp();
                    if (warp > 0 && !player.level().isClientSide) {
                        // Split warp between permanent and normal
                        if (warp > 1) {
                            int w2 = warp / 2;
                            if (warp - w2 > 0) {
                                addWarpToPlayer(player, warp - w2, IPlayerWarp.EnumWarpType.PERMANENT);
                            }
                            if (w2 > 0) {
                                addWarpToPlayer(player, w2, IPlayerWarp.EnumWarpType.NORMAL);
                            }
                        } else {
                            addWarpToPlayer(player, warp, IPlayerWarp.EnumWarpType.PERMANENT);
                        }
                    }
                }
            }
            
            // Handle completion
            if (showPopups && sync) {
                knowledge.setResearchFlag(researchKey, IPlayerKnowledge.EnumResearchFlag.POPUP);
                if (!noFlags) {
                    knowledge.setResearchFlag(researchKey, IPlayerKnowledge.EnumResearchFlag.RESEARCH);
                } else {
                    noFlags = false;
                }
                
                // Give reward items
                if (entry.getRewardItem() != null) {
                    for (ItemStack reward : entry.getRewardItem()) {
                        if (!player.getInventory().add(reward.copy())) {
                            player.spawnAtLocation(reward.copy(), 1.0f);
                        }
                    }
                }
                
                // Give reward knowledge
                if (entry.getRewardKnow() != null) {
                    for (ResearchStage.Knowledge rk : entry.getRewardKnow()) {
                        addKnowledge(player, rk.type, rk.category, rk.type.getProgression() * rk.amount);
                    }
                }
                
                // Check for addendum unlocks
                checkAddendumUnlocks(player, knowledge, researchKey);
            }
        }
        
        // Complete sibling research
        if (entry != null && entry.getSiblings() != null) {
            for (String sibling : entry.getSiblings()) {
                if (!knowledge.isResearchComplete(sibling) && doesPlayerHaveRequisites(player, sibling)) {
                    completeResearch(player, sibling, sync);
                }
            }
        }
        
        if (sync) {
            syncList.put(player.getName().getString(), true);
            if (entry != null) {
                player.giveExperiencePoints(5);
            }
        }
        
        return true;
    }
    
    private static boolean isStageAutoComplete(ResearchStage stage) {
        return stage.getCraft() == null && stage.getObtain() == null 
               && stage.getKnow() == null && stage.getResearch() == null;
    }
    
    private static void checkAddendumUnlocks(Player player, IPlayerKnowledge knowledge, String completedResearch) {
        for (String catKey : ResearchCategories.researchCategories.keySet()) {
            ResearchCategory category = ResearchCategories.getResearchCategory(catKey);
            if (category == null) continue;
            
            for (ResearchEntry entry : category.research.values()) {
                if (entry == null || entry.getAddenda() == null || !knowledge.isResearchComplete(entry.getKey())) {
                    continue;
                }
                
                for (ResearchAddendum addendum : entry.getAddenda()) {
                    if (addendum.getResearch() != null && Arrays.asList(addendum.getResearch()).contains(completedResearch)) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("tc.addaddendum", entry.getLocalizedName()));
                        knowledge.setResearchFlag(entry.getKey(), IPlayerKnowledge.EnumResearchFlag.PAGE);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * TC4 ResearchManager.createResearchNoteForPlayer: reuse an existing note
     * for this research from the inventory, otherwise require the player to
     * have paper and scribing tools with ink left; the ink and paper are
     * consumed on success.
     *
     * @return the note stack (also placed in the player inventory), or null when
     * the player cannot get a note
     */
    public static ItemStack createResearchNoteForPlayer(Player player, String key) {
        // Reuse an existing note for this research
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof thaumcraft.common.items.ItemResearchNotes) {
                thaumcraft.common.lib.research.ResearchNoteData data =
                        thaumcraft.common.lib.research.ResearchNotes.getData(stack);
                if (data != null && key.equals(data.key)) {
                    return stack;
                }
            }
        }

        // Need scribing tools with ink and one paper
        if (!thaumcraft.common.lib.research.ResearchNotes.consumeInkFromPlayer(player, false)) {
            return null;
        }
        if (!player.getInventory().contains(new ItemStack(net.minecraft.world.item.Items.PAPER))) {
            return null;
        }

        thaumcraft.common.lib.research.ResearchNotes.consumeInkFromPlayer(player, true);
        player.getInventory().clearOrCountMatchingItems(
                stack -> stack.is(net.minecraft.world.item.Items.PAPER), 1, player.inventoryMenu.getCraftSlots());

        ItemStack created = thaumcraft.common.lib.research.ResearchNotes.createNote(
                new ItemStack(thaumcraft.init.ModItems.RESEARCH_NOTES.get()), key, player.level());
        if (created != null && !created.isEmpty()) {
            if (!player.getInventory().add(created.copy())) {
                player.drop(created.copy(), false);
            }
            return created;
        }
        return null;
    }

    /**
     * Check if player has all prerequisite research.
     * <p>
     * TC4 semantics: both the visible parents and the hidden parents must be
     * complete, and the "@N" stage suffix some data sets append to a key must
     * be ignored (the port cannot track "N stages of research X").
     */
    public static boolean doesPlayerHaveRequisites(Player player, String key) {
        ResearchEntry entry = ResearchCategories.getResearch(key);
        if (entry == null) return true;

        String[] parents = entry.getParentsClean();
        if (parents != null && !ThaumcraftCapabilities.knowsResearchStrict(player, parents)) {
            return false;
        }
        String[] hidden = entry.getParentsHiddenClean();
        return hidden == null || ThaumcraftCapabilities.knowsResearchStrict(player, hidden);
    }
    
    // ==================== Warp Management ====================
    
    /**
     * Add warp to a player.
     */
    public static void addWarpToPlayer(Player player, int amount, IPlayerWarp.EnumWarpType type) {
        IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
        if (warp != null) {
            warp.add(type, amount);
            if (player instanceof ServerPlayer serverPlayer) {
                warp.sync(serverPlayer);
            }
        }
    }
    
    // ==================== Aspect Combination ====================
    
    /**
     * Get the result of combining two aspects.
     */
    public static Aspect getCombinationResult(Aspect aspect1, Aspect aspect2) {
        for (Aspect aspect : Aspect.aspects.values()) {
            if (aspect.getComponents() != null) {
                Aspect[] components = aspect.getComponents();
                if ((components[0] == aspect1 && components[1] == aspect2) ||
                    (components[0] == aspect2 && components[1] == aspect1)) {
                    return aspect;
                }
            }
        }
        return null;
    }

    /**
     * TC4 ResearchManager.reduceToPrimals: recursively split every compound
     * aspect into the primal aspects it is built from, summing the amounts of
     * primals reached through several paths.
     * <p>
     * Used by the aspect orb drop logic ({@code EntityEvents.onLivingDrops}):
     * TC4 only ever stores primal aspects in wands, so entity aspects are
     * reduced before spawning orbs.
     */
    public static AspectList reduceToPrimals(AspectList in) {
        AspectList out = new AspectList();
        if (in == null) {
            return out;
        }
        for (Aspect aspect : in.getAspects()) {
            if (aspect == null) {
                continue;
            }
            int amount = in.getAmount(aspect);
            if (amount <= 0) {
                continue;
            }
            if (aspect.isPrimal()) {
                out.add(aspect, amount);
                continue;
            }
            Aspect[] components = aspect.getComponents();
            if (components == null || components.length != 2) {
                continue;
            }
            AspectList split = new AspectList();
            split.add(components[0], amount);
            split.add(components[1], amount);
            out.add(reduceToPrimals(split));
        }
        return out;
    }

    // ==================== JSON Parsing ====================
    
    /**
     * Parse all research JSON files from registered locations.
     */
    /** Research JSON is static data; the client and an integrated server share statics. */
    private static boolean researchParsed = false;

    public static void parseAllResearch() {
        // Guard against re-parsing: ConfigResearch.init() runs on the client at
        // startup and again on the integrated/dedicated server, but
        // ResearchCategories is static, so a second parse only produces
        // "already exists" warnings.
        if (researchParsed) {
            return;
        }
        researchParsed = true;
        int totalEntries = 0;
        
        for (ResourceLocation loc : CommonInternals.jsonLocs.values()) {
            // TC4 research data lives in data/<ns>/<path>.json. Fall back to the
            // assets tree so addons that still ship research there keep working.
            String path = "/data/" + loc.getNamespace() + "/" + loc.getPath();
            if (!path.endsWith(".json")) {
                path += ".json";
            }

            try (InputStream stream = ResearchManager.class.getResourceAsStream(path)) {
                if (stream != null) {
                    InputStreamReader reader = new InputStreamReader(stream);
                    JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray entries = obj.getAsJsonArray("entries");
                    
                    int count = 0;
                    for (JsonElement element : entries) {
                        try {
                            JsonObject entryObj = element.getAsJsonObject();
                            ResearchEntry entry = parseResearchJson(entryObj);
                            addResearchToCategory(entry);
                            count++;
                        } catch (Exception e) {
                            Thaumcraft.LOGGER.warn("Invalid research entry in {}: {}", loc, e.getMessage());
                        }
                    }
                    
                    Thaumcraft.LOGGER.info("Loaded {} research entries from {}", count, path);
                    totalEntries += count;
                } else {
                    Thaumcraft.LOGGER.warn("Research file not found: {}", path);
                }
            } catch (Exception e) {
                Thaumcraft.LOGGER.warn("Failed to parse research file {}: {}", loc, e.getMessage());
            }
        }
        
        Thaumcraft.LOGGER.info("Total research entries loaded: {}", totalEntries);
    }
    
    /**
     * Parse a single research entry from JSON.
     */
    private static ResearchEntry parseResearchJson(JsonObject obj) throws Exception {
        ResearchEntry entry = new ResearchEntry();
        
        // Required fields
        entry.setKey(obj.get("key").getAsString());
        entry.setName(obj.get("name").getAsString());
        entry.setCategory(obj.get("category").getAsString());
        
        // Icons
        if (obj.has("icons")) {
            String[] iconStrings = arrayJsonToString(obj.getAsJsonArray("icons"));
            if (iconStrings != null && iconStrings.length > 0) {
                Object[] icons = new Object[iconStrings.length];
                for (int i = 0; i < iconStrings.length; i++) {
                    ItemStack stack = parseJSONtoItemStack(iconStrings[i]);
                    if (!stack.isEmpty()) {
                        icons[i] = stack;
                    } else if (iconStrings[i].startsWith("focus")) {
                        icons[i] = iconStrings[i];
                    } else {
                        icons[i] = normalizeIconLocation(iconStrings[i]);
                    }
                }
                entry.setIcons(icons);
            }
        }
        
        // Location on research tree
        if (obj.has("location")) {
            Integer[] loc = arrayJsonToInt(obj.getAsJsonArray("location"));
            if (loc != null && loc.length == 2) {
                entry.setDisplayColumn(loc[0]);
                entry.setDisplayRow(loc[1]);
            }
        }
        
        // Parents and siblings
        if (obj.has("parents")) {
            entry.setParents(arrayJsonToString(obj.getAsJsonArray("parents")));
        }
        if (obj.has("parents_hidden")) {
            entry.setParentsHidden(arrayJsonToString(obj.getAsJsonArray("parents_hidden")));
        }
        if (obj.has("siblings")) {
            entry.setSiblings(arrayJsonToString(obj.getAsJsonArray("siblings")));
        }
        
        // Metadata flags
        if (obj.has("meta")) {
            String[] metaStrings = arrayJsonToString(obj.getAsJsonArray("meta"));
            if (metaStrings != null) {
                List<ResearchEntry.EnumResearchMeta> metas = new ArrayList<>();
                for (String s : metaStrings) {
                    try {
                        metas.add(ResearchEntry.EnumResearchMeta.valueOf(s.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        Thaumcraft.LOGGER.warn("Unknown research meta: {}", s);
                    }
                }
                entry.setMeta(metas.toArray(new ResearchEntry.EnumResearchMeta[0]));
            }
        }
        
        // Rewards
        if (obj.has("reward_item")) {
            entry.setRewardItem(parseJsonItemList(arrayJsonToString(obj.getAsJsonArray("reward_item"))));
        }
        if (obj.has("reward_knowledge")) {
            String[] knowledgeStrings = arrayJsonToString(obj.getAsJsonArray("reward_knowledge"));
            if (knowledgeStrings != null) {
                List<ResearchStage.Knowledge> knowledge = new ArrayList<>();
                for (String s : knowledgeStrings) {
                    ResearchStage.Knowledge k = ResearchStage.Knowledge.parse(s);
                    if (k != null) knowledge.add(k);
                }
                if (!knowledge.isEmpty()) {
                    entry.setRewardKnow(knowledge.toArray(new ResearchStage.Knowledge[0]));
                }
            }
        }
        
        // Stages
        if (obj.has("stages")) {
            JsonArray stagesJson = obj.getAsJsonArray("stages");
            List<ResearchStage> stages = new ArrayList<>();
            for (JsonElement stageElement : stagesJson) {
                stages.add(parseStageJson(stageElement.getAsJsonObject(), entry.getKey()));
            }
            if (!stages.isEmpty()) {
                entry.setStages(stages.toArray(new ResearchStage[0]));
            }
        }
        
        // Addenda
        if (obj.has("addenda")) {
            JsonArray addendaJson = obj.getAsJsonArray("addenda");
            List<ResearchAddendum> addenda = new ArrayList<>();
            for (JsonElement addElement : addendaJson) {
                addenda.add(parseAddendumJson(addElement.getAsJsonObject()));
            }
            if (!addenda.isEmpty()) {
                entry.setAddenda(addenda.toArray(new ResearchAddendum[0]));
            }
        }

        // TC4 hex-grid puzzle data (required aspects + canvas complexity, 1-3)
        if (obj.has("aspects")) {
            entry.setRequiredAspects(parseAspectList(obj.getAsJsonArray("aspects")));
        }
        if (obj.has("complexity")) {
            entry.setComplexity(Math.min(3, Math.max(1, obj.get("complexity").getAsInt())));
        }

        // TC4 discovery triggers
        if (obj.has("triggers")) {
            JsonObject triggers = obj.getAsJsonObject("triggers");
            if (triggers.has("aspects")) {
                AspectList list = parseAspectList(triggers.getAsJsonArray("aspects"));
                if (list != null) {
                    entry.setAspectTriggers(list.getAspects());
                }
            }
            if (triggers.has("items")) {
                ItemStack[] items = parseJsonItemList(arrayJsonToString(triggers.getAsJsonArray("items")));
                if (items != null) {
                    entry.setItemTriggers(items);
                }
            }
            if (triggers.has("entities")) {
                entry.setEntityTriggers(arrayJsonToString(triggers.getAsJsonArray("entities")));
            }
        }

        return entry;
    }

    /**
     * Parse a JSON array of aspect names with optional amounts ("aer", "terra:2")
     * into an AspectList. Mirrors TC4 ResearchItem.tags.
     */
    private static AspectList parseAspectList(JsonArray array) {
        if (array == null || array.isEmpty()) return null;
        AspectList out = new AspectList();
        for (JsonElement e : array) {
            String s = e.getAsString();
            int amount = 1;
            if (s.contains(":")) {
                try {
                    amount = Integer.parseInt(s.substring(s.indexOf(":") + 1));
                    s = s.substring(0, s.indexOf(":"));
                } catch (NumberFormatException ignored) {}
            }
            Aspect aspect = Aspect.getAspect(s);
            if (aspect != null) {
                out.add(aspect, Math.max(1, amount));
            }
        }
        return out.size() > 0 ? out : null;
    }
    
    /**
     * Parse a research stage from JSON.
     */
    private static ResearchStage parseStageJson(JsonObject obj, String entryKey) {
        ResearchStage stage = new ResearchStage();
        
        if (obj.has("text")) {
            stage.setText(obj.get("text").getAsString());
        }
        
        if (obj.has("recipes")) {
            stage.setRecipes(arrayJsonToResourceLocations(obj.getAsJsonArray("recipes")));
            // Map crafted recipe results to this research entry so crafting them progresses the research
            for (JsonElement r : obj.getAsJsonArray("recipes")) {
                ResourceLocation rl = ResourceLocation.parse(r.getAsString());
                ItemStack is = new ItemStack(BuiltInRegistries.ITEM.get(rl));
                if (!is.isEmpty()) {
                    int code = createItemStackHash(is);
                    craftingReferences.add(code);
                    craftingReferencesMap.computeIfAbsent(code, k -> new HashSet<>()).add(entryKey);
                }
            }
        }

        if (obj.has("required_item")) {
            stage.setObtain(parseJsonOreList(arrayJsonToString(obj.getAsJsonArray("required_item"))));
            Object[] obtainItems = stage.getObtain();
            if (obtainItems != null) {
                for (Object o : obtainItems) {
                    if (o instanceof ItemStack stack) {
                        int code = createItemStackHash(stack);
                        craftingReferences.add(code);
                        craftingReferencesMap.computeIfAbsent(code, k -> new HashSet<>()).add(entryKey);
                    }
                }
            }
        }
        
        if (obj.has("required_craft")) {
            String[] craftStrings = arrayJsonToString(obj.getAsJsonArray("required_craft"));
            Object[] craftItems = parseJsonOreList(craftStrings);
            stage.setCraft(craftItems);

            // Create crafting references for tracking
            if (craftItems != null) {
                int[] refs = new int[craftItems.length];
                for (int i = 0; i < craftItems.length; i++) {
                    int code;
                    if (craftItems[i] instanceof ItemStack stack) {
                        code = createItemStackHash(stack);
                    } else {
                        code = ("tag:" + craftItems[i]).hashCode();
                    }
                    craftingReferences.add(code);
                    craftingReferencesMap.computeIfAbsent(code, k -> new HashSet<>()).add(entryKey);
                    refs[i] = code;
                }
                stage.setCraftReference(refs);
            }
        }
        
        if (obj.has("required_knowledge")) {
            String[] knowStrings = arrayJsonToString(obj.getAsJsonArray("required_knowledge"));
            if (knowStrings != null) {
                List<ResearchStage.Knowledge> knowledge = new ArrayList<>();
                for (String s : knowStrings) {
                    ResearchStage.Knowledge k = ResearchStage.Knowledge.parse(s);
                    if (k != null) knowledge.add(k);
                }
                if (!knowledge.isEmpty()) {
                    stage.setKnow(knowledge.toArray(new ResearchStage.Knowledge[0]));
                }
            }
        }
        
        if (obj.has("required_research")) {
            String[] researchStrings = arrayJsonToString(obj.getAsJsonArray("required_research"));
            if (researchStrings != null) {
                String[] keys = new String[researchStrings.length];
                String[] icons = new String[researchStrings.length];
                for (int i = 0; i < researchStrings.length; i++) {
                    String[] parts = researchStrings[i].split(";");
                    keys[i] = parts[0];
                    icons[i] = parts.length > 1 ? parts[1] : null;
                }
                stage.setResearch(keys);
                stage.setResearchIcon(icons);
            }
        }
        
        if (obj.has("warp")) {
            stage.setWarp(obj.get("warp").getAsInt());
        }
        
        return stage;
    }
    
    /**
     * Parse a research addendum from JSON.
     */
    private static ResearchAddendum parseAddendumJson(JsonObject obj) {
        ResearchAddendum addendum = new ResearchAddendum();
        
        addendum.setText(obj.get("text").getAsString());
        
        if (obj.has("recipes")) {
            addendum.setRecipes(arrayJsonToResourceLocations(obj.getAsJsonArray("recipes")));
        }
        
        if (obj.has("required_research")) {
            addendum.setResearch(arrayJsonToString(obj.getAsJsonArray("required_research")));
        }
        
        return addendum;
    }
    
    /**
     * Add a parsed research entry to its category.
     */
    private static void addResearchToCategory(ResearchEntry entry) {
        ResearchCategory category = ResearchCategories.getResearchCategory(entry.getCategory());
        if (category == null) {
            Thaumcraft.LOGGER.warn("Cannot add research {} - category {} not found", 
                    entry.getKey(), entry.getCategory());
            return;
        }
        
        if (category.research.containsKey(entry.getKey())) {
            Thaumcraft.LOGGER.warn("Research {} already exists in category {}", 
                    entry.getKey(), entry.getCategory());
            return;
        }
        
        // Check for position conflicts. TC4 itself has entries without a
        // location (CAP_iron / ROD_wood) that default to [0,0] and therefore
        // overlap the category root - the node is simply drawn twice. Keep the
        // entry and only warn instead of dropping data.
        for (ResearchEntry existing : category.research.values()) {
            if (existing.getDisplayColumn() == entry.getDisplayColumn() && 
                existing.getDisplayRow() == entry.getDisplayRow()) {
                Thaumcraft.LOGGER.warn("Research {} overlaps with {} at position [{},{}]",
                        entry.getKey(), existing.getKey(), entry.getDisplayColumn(), entry.getDisplayRow());
                break;
            }
        }
        
        category.research.put(entry.getKey(), entry);
        
        // Update category bounds
        if (entry.getDisplayColumn() < category.minDisplayColumn) {
            category.minDisplayColumn = entry.getDisplayColumn();
        }
        if (entry.getDisplayRow() < category.minDisplayRow) {
            category.minDisplayRow = entry.getDisplayRow();
        }
        if (entry.getDisplayColumn() > category.maxDisplayColumn) {
            category.maxDisplayColumn = entry.getDisplayColumn();
        }
        if (entry.getDisplayRow() > category.maxDisplayRow) {
            category.maxDisplayRow = entry.getDisplayRow();
        }
    }
    
    // ==================== JSON Helper Methods ====================

    /**
     * Normalise a research icon path from JSON into a texture ResourceLocation
     * that can be bound directly (TC4's {@code icon_resource}, used by
     * {@code GuiGraphics#blit}).
     *
     * The research JSON stores full asset paths such as
     * "thaumcraft:textures/item/thaumonomicon.png" - and that is exactly the
     * form a direct texture bind needs ("textures/" prefix plus ".png"). Only
     * the missing parts are added here; stripping them (the old behaviour) made
     * every node icon sample a non-existent texture, i.e. a magenta/black
     * "missing texture" square.
     */
    public static ResourceLocation normalizeIconLocation(String raw) {
        ResourceLocation loc = ResourceLocation.parse(raw);
        String path = loc.getPath();
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        return ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), path);
    }

    private static String[] arrayJsonToString(JsonArray array) {
        if (array == null || array.isEmpty()) return null;
        List<String> list = new ArrayList<>();
        for (JsonElement e : array) {
            list.add(e.getAsString());
        }
        return list.toArray(new String[0]);
    }
    
    private static Integer[] arrayJsonToInt(JsonArray array) {
        if (array == null || array.isEmpty()) return null;
        List<Integer> list = new ArrayList<>();
        for (JsonElement e : array) {
            list.add(e.getAsInt());
        }
        return list.toArray(new Integer[0]);
    }
    
    private static ResourceLocation[] arrayJsonToResourceLocations(JsonArray array) {
        if (array == null || array.isEmpty()) return null;
        List<ResourceLocation> list = new ArrayList<>();
        for (JsonElement e : array) {
            list.add(ResourceLocation.parse(e.getAsString()));
        }
        return list.toArray(new ResourceLocation[0]);
    }
    
    private static ItemStack[] parseJsonItemList(String[] strings) {
        if (strings == null || strings.length == 0) return null;
        List<ItemStack> items = new ArrayList<>();
        for (String s : strings) {
            ItemStack stack = parseJSONtoItemStack(s.replace("'", "\""));
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        return items.isEmpty() ? null : items.toArray(new ItemStack[0]);
    }
    
    private static Object[] parseJsonOreList(String[] strings) {
        if (strings == null || strings.length == 0) return null;
        List<Object> items = new ArrayList<>();
        for (String s : strings) {
            s = s.replace("'", "\"");
            if (s.startsWith("tag:") || s.startsWith("oredict:")) {
                // Tag reference - in 1.20.1 we use tags instead of ore dictionary
                String tagName = s.contains(":") ? s.substring(s.indexOf(":") + 1) : s;
                items.add(tagName);
            } else {
                ItemStack stack = parseJSONtoItemStack(s);
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
        }
        return items.isEmpty() ? null : items.toArray();
    }
    
    /**
     * Parse an item stack from a JSON string.
     * Supports multiple formats:
     * - "modid:itemname"
     * - "modid:itemname;count"
     * - "modid:itemname;count;meta" (legacy, meta is ignored in 1.20.1)
     * - "modid:itemname;count;meta;{nbt}"
     * - "modid:itemname;count;{nbt}"
     */
    public static ItemStack parseJSONtoItemStack(String entry) {
        if (entry == null || entry.isEmpty()) return ItemStack.EMPTY;
        
        // Handle single quotes in NBT
        entry = entry.replace("'", "\"");
        
        String[] split = entry.split(";");
        String name = split[0].trim();
        int count = 1;
        String nbt = null;
        
        // Parse count and NBT from remaining segments
        for (int i = 1; i < split.length; i++) {
            String segment = split[i].trim();
            if (segment.startsWith("{")) {
                // This is NBT data - collect the rest of the string
                StringBuilder nbtBuilder = new StringBuilder();
                for (int j = i; j < split.length; j++) {
                    if (j > i) nbtBuilder.append(";");
                    nbtBuilder.append(split[j]);
                }
                nbt = nbtBuilder.toString();
                break;
            }
            try {
                int value = Integer.parseInt(segment);
                // First numeric value after name is count, second would be old metadata (ignored)
                if (count == 1) {
                    count = Math.max(1, value);
                }
                // Ignore additional numeric values (old metadata system)
            } catch (NumberFormatException ignored) {}
        }
        
        // Ensure name is lowercase (1.20+ requires lowercase ResourceLocations)
        name = name.toLowerCase();
        
        try {
            ResourceLocation itemId = ResourceLocation.parse(name);
            Item item = BuiltInRegistries.ITEM.get(itemId);
            
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                // Item not found - try fallback mappings for common old names
                String mappedName = LEGACY_ITEM_MAPPINGS.get(name);
                if (mappedName != null) {
                    itemId = ResourceLocation.parse(mappedName);
                    item = BuiltInRegistries.ITEM.get(itemId);
                }
            }
            
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                ItemStack stack = new ItemStack(item, count);
                if (nbt != null && !nbt.isEmpty()) {
                    try {
                        NBTHelper.setTag(stack, TagParser.parseTag(nbt));
                    } catch (Exception nbtEx) {
                        Thaumcraft.LOGGER.debug("Failed to parse NBT for {}: {}", entry, nbtEx.getMessage());
                    }
                }
                return stack;
            } else {
                // Only warn once per unknown item to avoid log spam
                if (!warnedItems.contains(name)) {
                    warnedItems.add(name);
                    Thaumcraft.LOGGER.debug("Unknown item in research: {}", name);
                }
            }
        } catch (Exception e) {
            Thaumcraft.LOGGER.warn("Failed to parse item stack: {}", entry, e);
        }
        
        return ItemStack.EMPTY;
    }
    
    // Track items we've already warned about to avoid log spam
    private static final Set<String> warnedItems = new HashSet<>();
    
    // Fallback mappings for legacy item names that might still be in JSON files
    private static final Map<String, String> LEGACY_ITEM_MAPPINGS = new HashMap<>();
    static {
        // Old generic items mapped to specific variants
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:ingot", "thaumcraft:thaumium_ingot");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:plate", "thaumcraft:plate_brass");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:nugget", "thaumcraft:brass_nugget");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:metal", "thaumcraft:thaumium_ingot");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:mind", "thaumcraft:brain_clockwork");
        
        // Block/item name variations
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:amulet_vis", "thaumcraft:amulet_vis_crafted");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:seal", "thaumcraft:seal_blank");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:focus_basic", "thaumcraft:focus_blank");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:focus_greater", "thaumcraft:focus_advanced");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:biothaumic_mind", "thaumcraft:brain_biothaumic");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:clockwork_mind", "thaumcraft:brain_clockwork");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:crimson_rites", "thaumcraft:thaumonomicon");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:module", "thaumcraft:golem_module_vision");
        
        // Block name variations
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:sapling_greatwood", "thaumcraft:greatwood_sapling");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:sapling_silverwood", "thaumcraft:silverwood_sapling");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:log_greatwood", "thaumcraft:greatwood_log");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:log_silverwood", "thaumcraft:silverwood_log");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:tube", "thaumcraft:tube_normal");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:smelter_basic", "thaumcraft:smelter");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:arcane_lamp", "thaumcraft:lamp_arcane");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:mirror", "thaumcraft:mirror_item");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:paving_barrier", "thaumcraft:paving_stone_barrier");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:paving_travel", "thaumcraft:paving_stone_travel");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:stone_ancient", "thaumcraft:ancient_stone");
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:stone_eldritch_tile", "thaumcraft:eldritch_stone_tile");
        
        // Placeholder items for research requirements (use diamond as proxy for "any enchanted item")
        LEGACY_ITEM_MAPPINGS.put("thaumcraft:enchanted_placeholder", "minecraft:enchanted_book");
        
        // Minecraft items that changed names
        LEGACY_ITEM_MAPPINGS.put("minecraft:dye", "minecraft:white_dye");
        LEGACY_ITEM_MAPPINGS.put("minecraft:hardened_clay", "minecraft:terracotta");
    }
    
    /**
     * Create a hash code for an ItemStack (used for crafting triggers).
     */
    public static int createItemStackHash(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy.toString().hashCode();
    }
}




