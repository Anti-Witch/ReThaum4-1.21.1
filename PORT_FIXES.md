# Fixes applied to the port (baseline / environment)

Reports in chat are Ukrainian; this file is the running log of concrete code/build
changes so they survive context loss.

## 2026-10-13 — wand item: assembled 3D model (rod + caps) in GUI/hand

The wands still looked like flat/unwrapped sprites in the inventory and in hand
even though the 3D combo models existed. Two root causes:

1. **Override predicates never matched.** `models/item/wand.json` used the bare
   predicate names `tc_rod` / `tc_cap`. `ItemOverride.Deserializer` parses them
   with `ResourceLocation.parse`, so a bare name becomes `minecraft:tc_rod`,
   while `WandModelPredicates` registers `thaumcraft:tc_rod`. Every wand fell
   back to the base model, which was `item/handheld` + a flat `layer0`
   (`textures/models/wand_rod_greatwood`, i.e. the 8x20px rod strip) - the
   "unwrapped texture" look. Other items in the port (thaumonomicon, golem
   placer/core) already used the namespaced form.
2. **The 3D model itself was wrong.** `wand_base/wand_staff_base` had a 16px
   `wand_shaft` textured with `thaumcraft:models/wand`, which is TC4's
   *focus-ball* sprite (the 6x6x6 cube unwrap in `ModelWand`), not a rod
   sprite, so the middle of the wand rendered as a plain white bar; the UV
   windows also mixed the rod's end patches with its side strips.

Fix (`scripts/gen_wand_models.py`, re-runnable generator):
- `wand_base.json` / `wand_staff_base.json` rebuilt from TC4 `ModelWand`:
  handle cap + rod + tip cap, TC4's own box UV windows (32x32 layout, 2x18px
  strips on the long faces, 2x2px patches on the ends), wand rod 18px /
  staff rod 32px, everything centred on the model box (z=8) so the GUI icon
  and the hand pivot stay sane. The staff has its own (smaller) display
  scales because its rod is 32px instead of 18px.
- `wand.json` is now `parent: thaumcraft:item/wand_base` (no flat `layer0`:
  TC4's `ItemWandRenderer` draws the assembled wand for *every* render type,
  INVENTORY included) with 162 `thaumcraft:tc_rod`/`thaumcraft:tc_cap`
  overrides; the generic base doubles as the fallback when a wand carries no
  rod/cap components.
- `models/item/wand_c/*`: 162 combo models regenerated (was 153). The **wood**
  rod (TC4 stick rod, craftable with any cap except iron) was missing, hence
  9 new models and `WandModelPredicates.RODS` now starts with `"wood"`.
- Hand/first-person display values now follow vanilla `item/handheld`
  (`[0,-90,25]`/`[0,-90,55]` + the vanilla translations), the GUI keeps the
  isometric `[30,225,0]` rotation the Z-aligned model needs.

Known gaps (unchanged): no focus ball / sceptre rune overlay (TC4 drew those at
runtime), and `WandRod`/`WandCap.getTexture()` still build `wand_rod_<tag>.png`
paths for staff tags whose sprites do not exist (nothing calls them today; the
models map e.g. `blaze_staff` to `wand_rod_blaze`).

## Session 2026-10-12 (user's big multi-group request)

Coordinator fixes (files changed):

- **D2 research points not reaching the table**: `PlayerEvents.playerJoin` moved
  from `EntityJoinLevelEvent` (payload often reached the client before the local
  player existed and was dropped) to `PlayerEvent.PlayerLoggedInEvent`;
  `BlockResearchTable.useWithoutItem` now syncs knowledge right before
  `openMenu`.
- **D3 research table multiblock broken**: `ItemInkwell` converted a wooden
  table only (deleted the neighbour), leaving a single research table that
  refused to open (the GUI requires an adjacent partner). Both tables now
  become research tables.
- **D7 sanity checker chat spam removed**; added the TC4 warp meter HUD
  (`HudHandler.renderSanityHud`, hud.png 152/176/216 tiles, pink/normal/perm
  bars, cracked brain at 100).
- **C duplicate Thaumonomicon**: removed the cheater variant from the creative
  tab.
- **B jars**: `models/block/jar_*.json` got `"render_type": "minecraft:translucent"`
  (jar textures have alpha; they rendered as opaque blue), `JAR_NORMAL` added to
  the creative tab.
- **D4 Thaumonomicon frame**: `ResearchBrowserScreen.renderFrame` reimplemented
  as TC4 `drawResearchBrowserBorders` (3x3 `gui_research.png`, 16x17 border,
  256x230); the old code blitted the fully transparent
  `gui_research_browser.png` (invisible frame).
- **D4 research icons were magenta**: research JSON stores full asset paths
  (`thaumcraft:textures/item/x.png`); `ResearchManager.normalizeIconLocation`
  now strips `textures/` and `.png`.
- **D4 background stretched 4x** ("purple fog"): scroll texScale
  `1/1024` -> `1/256` in `ResearchBrowserScreen`.
- **B magically-created bellows texture**: `textures/block/bellows.png`
  replaced with the original TC4 art (`textures/models/bellows.png`); was a
  woodplain placeholder.
- **B lamps always lit**: `lamp_growth`/`lamp_fertility` default texture is now
  the `_off` variant (they rendered animated/lit while disabled).
- **E lang**: phial names de-tautologised ("Порожня фіала"), research-table
  message typos fixed.
- **A6 BOM**: removed UTF-8 BOM from 230 JSON files under `data/`.
- **Infusion recipes ignored their centre item**: `InfusionRecipeType` codec only
  reads `"input"`, but 31 infusion recipes used `"center"` — renamed them to
  `"input"` (all rod_* recipes included). All JSON re-validated.
- **E lang**: added the six new staff rod names (`wand_rod_{blaze,ice,quartz,bone,reed,primal}_staff`).

### Subagent results folded in (2026-10-12)

- **A6 sandwich textures (mcmeta)**: 7 new `.mcmeta` files copied from the
  matching original animations (`causality_collapser`, `charm_undying`,
  `voidseer_charm`, `primordial_pearl`, `salis_mundus`,
  `primordial_pearl_mote`, `primordial_pearl_nodule`). `focus_1/2/3`,
  `verdant_charm*`, `node/creative_node`, `redstone_relay` left for the TC6
  removal pass (they are not real animations).
- **A7 armour**: 22 original layer textures added under
  `textures/models/armor/`; `ModArmorMaterials` paths fixed (they resolved to
  `textures/models/armor/armor/...`); separate `robe` and two-layer `voidrobe`
  materials; `ItemRobeArmor`/`ItemVoidRobeArmor` overlay logic fixed;
  `ItemCultistBoots` texture name fixed; robe item tint + `getDefaultDyeColor`
  registered. Known limitation: fortress/cultist plate sets need the TC4
  128x64 custom gear models (not ported yet).
- **E lang**: 49 keys fixed by the language subagent (alchemical brass ->
  alchemical construction, wand focus -> набалдашник, rod naming, goggles of
  revealing, nuggets, golem core brass).
- **A8/D6 wands**: `wand_base.json`/`wand_staff_base.json` UV windows pointed
  into the transparent half of the 32x32 rod/cap textures (that was the
  "unwrapped texture" look); 3D `wand_rod`/`wand_cap` parent models created,
  17 rod + 9 cap models reparented, `wand.json` regenerated (153 overrides +
  153 combo models); **6 staff rods added** (blaze, ice, quartz, bone, reed,
  primal) with items, recipes and research entries (171 research entries total).

### Late-session fixes (2026-10-12, after the first server/client tests)

- **Taint creatures dropped nothing** (their TC4 drops were commented out as
  "TC6-removed" and replaced by a bogus `flux_crystal`): restored
  `taint_slime` / `taint_tendril` items (TC4 ItemResource 11/12) with models,
  lang and creative tab entries; all taint mobs now drop them (TC4 chances);
  `void_seed` crucible recipe fixed (catalyst `wheat_seeds`, TC4 aspects).
- **`flux_crystal` (TC6) removed** (item, model, texture, lang, creative).
- **`creative_flux_sponge` (TC6) removed** (item, model, texture, lang,
  creative).
- **TC6 primal foci removed**: `focus_air/water/earth/order/entropy/treasure`
  (items, models, textures, lang, creative) - TC4 only has fire, frost, shock,
  excavation, trade, portable hole, warding, pech, hellbat, primal.
- **`nitor` could not be placed**: item was a plain `ItemMaterial`; now
  `ItemNitor` places the white nitor block (TC4 behaviour) and all 16 colour
  blocks are in the creative tab.
- **Taint biome blocks** (`taint_soil/rock/crust/fibre/weed/log`) now have
  BlockItems + item models and are in the creative tab (TC4 had them).
- **Golem placer** rendered one placeholder sprite for every material: added
  `tc_golem_material` item property + 4 material models (wood/iron/clay/
  thaumium) + overrides.
- **Lamps always lit**: `lamp_growth`/`lamp_fertility` default texture is the
  `_off` variant now.
- **Jar models**: `render_type: translucent` on all `jar_*.json`; `JAR_NORMAL`
  added to the creative tab.
- **Bellows texture**: `block/bellows.png` replaced with the original TC4 art.
- **Data tags fixed** (`#neoforge:needs_netherite_tool`, `nitor_group` now lists
  the real nitor blocks).
- **Spawn placement** for `thaumcraft:taintacle` registered (NeoForge warning
  gone).
- **31 infusion recipes** used `"center"` which the codec ignores - renamed to
  `"input"`.
- **D2/D3/D4/D7 fixes** as listed above.

### Post-test fixes (2026-10-12 evening, after live in-game verification)

- **Systemic `render_type` bug**: 53 block models with translucent textures had
  no `render_type`, so NeoForge rendered them in the SOLID layer and their
  transparent pixels turned black. Fixed with `cutout` / `translucent` as
  appropriate: tubes, pedestals, lamps, loot urns/crates, stabilizer, bellows,
  centrifuge, everfull urn, levitator, essentia input/output, arcane ear,
  brain box, redstone relay, taint growth/orbs, inlay, recharge pedestal.
  (TC6 leftovers dioptra/pattern_crafter/vis_generator skipped.)
- **`grass_ambient` black sides** (VERIFIED in-game before/after): it used
  vanilla `grass_block` without `render_type` -> SOLID -> the 211/256
  transparent overlay pixels rendered black. New model with
  `cutout_mipped`; snowy variant uses `grass_block_snow`.
- **Leaves**: silverwood tint fixed to the TC4 constant `0x8899AA` (was biome
  green), greatwood uses the 3x3-averaged biome foliage colour; item icons of
  leaves/grass now delegate to the block colour; sapling tint removed.
- **Cross plants verified in-game** (screenshots): saplings/cinderpearl/
  shimmerleaf/vishroom/taint_weed render correctly as cutout cross models; the
  "flattened" look in the user's screenshots is the normal top-down view of a
  cross model.
- `BlockLeavesTC` silverwood light 4 -> 7 (TC4); `item/taint_weed.json` got
  display transforms.
- **Thaumometer** (subagent): full TC4 first-person renderer (BEWLR), blue
  translucent lens quad, aspect/name overlay on the lens (TC4 had no screen
  HUD), removed the fake bottom-right HUD and stray numbers, **fixed the
  critical server-side scan bug** (`startUsingItem` was client-only), added
  inventory scanning (hover 1 s + `PacketScanItemToServer`).
- **Infusion recipes**: 31 files used `"center"` which the codec ignored -
  renamed to `"input"`.
- lang: added `tc.addaspectdiscovery` / `tc.addaspectpool`; staff rod names.

### G4 — wisp essence (2026-10-12)

- `ItemWispEssence` (new): TC4-style single-aspect essentia item with
  `IEssentiaContainerItem` (aspect in CustomData), coloured icon + tooltip.
  Replaces the plain `ItemMaterial` registration of `wisp_essence`.
- `BlockNode.playerWillDestroy`: TC4 `BlockAiry.onBlockHarvested` behaviour -
  destroying a node drops wisp essence (one per 10 vis of an aspect, min 5 vis,
  2 points of the aspect each).
- Item colour handler for wisp essence added in `Thaumcraft.java`.
- G3 note: warp on node destruction is NOT in TC4 (`WarpEvents`/`TileNode`/
  `BlockAiry` checked) - the user's "lots of warp" came from something else.

## Rebranding (2026-10-11)

- Renamed the mod from "Thaumcraft 6.2.0" to **Thaumcraft 4 Reforged
  4.2.3.5+1.21.1** (user request). `mod_id` intentionally stays `thaumcraft`
  so existing worlds/configs/datapacks keep working.
  - `gradle.properties`: `mod_name`, `mod_version`, `mod_description`, new
    `mod_archive_name=thaumcraft-4-reforged`
  - `build.gradle`: `archivesName = mod_archive_name` -> jar is now
    `thaumcraft-4-reforged-4.2.3.5+1.21.1.jar`
  - `neoforge.mods.toml`: `logoFile="TCLogo.png"` (the file was already in
    `src/main/resources`, the old name `thaumcraft_logo.png` did not exist)
  - `Thaumcraft.java`: MODNAME / VERSION constants
- VNC stand: `x11vnc` now runs with `-xkb -repeat -nolookup` (keyboard layout
  and key auto-repeat work; before, keys could stick) and
  `scripts/game-vnc.sh` prints the direct noVNC URL
  (`/vnc.html?autoconnect=1&resize=scale`) plus a reminder to click the screen
  for keyboard focus / use fullscreen.

## Pending (requested by the user, do after in-game testing)

- **Recipe-book category warnings**: `Unknown recipe category: thaumcraft:infusion/...`
  and `...:crucible/...` spam from `ClientRecipeBook` (hundreds of lines). Harmless
  but noisy; fix by registering a `RecipeBookCategory` for our custom recipe
  types or overriding `recipeBookCategory()`. Low priority.

## Fixed from the user's in-game logs (2026-10-11 evening)

- **Data tags used the old directory names** (`data/thaumcraft/tags/blocks/`
  and `tags/items/`). In 1.21.1 the paths are singular (`tags/block/`,
  `tags/item/`); none of the port's block/item tags were loading. Renamed the
  folders. Also filled the empty `incorrect_for_*_tool` tags with TC4-like
  tiers: thaumium = diamond tier (`#minecraft:needs_netherite_tool`), void =
  everything mineable (empty), elemental = thaumium tier.
- `textures/models/Thaumaturge_eyes.png` had an uppercase letter (invalid pack
  path, "Invalid path in pack ... ignoring"): renamed to `thaumaturge_eyes.png`.
- Research JSON was parsed twice (client + integrated server share statics) and
  spammed "Research X already exists": `ResearchManager.parseAllResearch()` is
  now idempotent.
- (User-side, not our bugs) the client log also shows:
  - `LanguageReload` 1.0.2 crash on opening the creative inventory
    (`NullPointerException: this.separateTranslations is null` in
    `ClientLanguage.handler$...languagereload$onGet`) - third-party mixin bug,
    reproduce without that mod.
  - `Enhanced Block Entities` logs "Sheets loaded too early" (Connector issue).
  - The user's PC runs Minecraft on **llvmpipe** (software OpenGL) which is why
    loading/gameplay is slow - not related to this mod.

- **Curios dependency** (found from the user's client log 2026-10-11):
  the mod crashed at startup without Curios
  (`NoClassDefFoundError: top/theillusivec4/curios/api/type/capability/ICurioItem`
  at `ModItems.<clinit>` line ~510). Reason: method references to Curios item
  classes (ItemAmuletVis, ItemCloudRing, ...) are resolved during `ModItems`
  static init, so the whole mod dies when Curios is absent.
  - Fixed properly: Curios is now bundled into our jar via **Jar-in-Jar**
    (`jarJar "top.theillusivec4.curios:curios-neoforge:${curios_version}"` in
    build.gradle + `required` in neoforge.mods.toml). Verified:
    `META-INF/jarjar/curios-neoforge-9.5.1+1.21.1.jar` + metadata.json are
    inside `build/libs/thaumcraft-4-reforged-4.2.3.5+1.21.1.jar`, so players do NOT need to install
    Curios separately anymore.
  - Still TODO (nice-to-have): make the dependency soft again via per-item
    factories (plain Item when Curios is missing) - but with JiJ this is no
    longer urgent.

- **Rename the mod artefact** (not the `mod_id`!):
  - `gradle.properties`: `mod_name=Thaumcraft 4 Reforged`,
    `mod_version=4.2.3.5+1.21.1` (or user's own numbering),
    update `mod_description`.
  - `build.gradle`: `archivesName = 'thaumcraft-4-reforged'` ->
    jar `thaumcraft-4-reforged-4.2.3.5+1.21.1.jar`.
  - `src/main/templates/META-INF/neoforge.mods.toml`: fix `logoFile` - it points
    at `thaumcraft_logo.png` but the file is `src/main/resources/TCLogo.png`
    (the mod logo is currently not shown at all).
  - NEVER change `mod_id=thaumcraft`: registry ids, lang keys, recipes,
    research, capabilities and existing player worlds all depend on it.



## 2026-10-11 — Phase 0: build baseline on Linux/JDK21

### Environment after upgrade (final build baseline)
- 4 vCPU, 15 GiB RAM (12 GiB available), no swap needed.
- The NeoForm decompile OOM issues below were environment-only (7.8 GB / 2 vCPU
  box). With the upgraded machine the stock pipeline (including the original
  `--include-classpath` args and `-Xmx3G`) completes; **no NeoForm config
  patching is required anymore**. `scripts/build.sh` now only verifies the
  cached config is pristine and restores it from `.bak` if a patched one is
  found.

### gradle.properties
- Replaced Windows-only `org.gradle.java.home=C:/Program Files/Java/jdk-21.0.10`
  with `/usr/local/sdkman/candidates/java/21.0.10-ms` (JDK 21 in this Codespace).
- `build.gradle` already had `-Xmaxerrs 3000` (requirement from message.txt) — no change.

### (Historical, 7.8 GB box) NeoForm decompile step — OOM
- Symptom: `createMinecraftArtifacts` failed in the `decompile` step; Vineflower
  OOM'd (`java.lang.OutOfMemoryError: Java heap space` at
  `SSAUConstructorSparseEx.updateLiveMap`) or was SIGTERM'd by environment
  memory pressure.
- Root cause: machine RAM, not the code. Decompiling 8.3k Minecraft classes with
  `--bytecode-source-mapping --dump-code-lines` plus `-Xmx3G` cannot fit in
  ~2 GB available RAM.
- Workarounds tried and their value for reference:
  - `--include-classpath <file>` -> OOM (tries to decompile libraries too).
  - `--library=<file>` -> MalformedInputException (file has `lib:` format).
  - Gradle daemon heap + tool heap overcommit was an aggravating factor; run
    heavy gradle tasks with `--no-daemon` on small machines.
- On the upgraded machine none of this is needed; keep the config pristine.
- With the `--library` fix, Vineflower needs roughly >= 3 GB heap for the
  1.21.1 jar on this box; `--include-classpath` needed way more.

## 2026-10-11 — Phase 1: remove TC6-only content (jar rebuilt OK)

Deleted (with all registrations/resources):
- **Grapple gun**: `ItemGrappleGun.java`, `EntityGrapple.java`, `GrappleRenderer.java`,
  `ModItems.GRAPPLE_GUN/_SPOOL/_TIP` (+import), `ModEntities.GRAPPLE` (+import),
  creative-tab entries, `Thaumcraft.java` renderer registrations (legacy + event),
  item models/textures, 3 recipes (2 arcane + 1 infusion), lang keys,
  research entry `GRAPPLEGUN` (artifice.json).
  - Kept `GrapplerModel` because `FocusMineRenderer` uses it (TC6 focus-mine stays
    until the Phase 4 wand-focus rework).
- **Caster gauntlet leftovers**: `ItemCaster.java`, `IInteractWithCaster.java`,
  9 caster item models, 2 caster item textures, lang keys, `ItemCreativePlacer`
  (TC6 creative debug item with a `type=caster` blockstate) + its assets/lang.
  - Kept `CasterManager`, `FocusInit`, `ItemFocus`, `ItemFocusPouch`, `api/casters/*`:
    the port's wand casting pipeline currently depends on the FocusEngine; they will
    be removed in Phase 4 when foci/wand get the TC4 `IWandFocus` pipeline.
- **Theorycraft / inspiration**: whole `api/research/theorycraft/**` and
  `common/lib/research/theorycraft/**` (52 files), `ConfigResearch` wiring,
  `THEORYRESEARCH` + `CELESTIALSCANNING` research entries (basics.json),
  `card.*`/`research.THEORYRESEARCH.*`/`research.CELESTIALSCANNING.*` lang keys.
  - `ResearchTableData` was NOT used by the hex minigame (it uses
    `common.lib.research.ResearchNoteData`) - safe removal confirmed.

Build: `./gradlew build` BUILD SUCCESSFUL (jar `build/libs/thaumcraft-4-reforged-4.2.3.5+1.21.1.jar`).

### Phase 1 smoke test - dedicated-server crash fix (dist safety)
`runGameTestServer` surfaced a real bug: the common mod class `thaumcraft.Thaumcraft`
declared `getClientWorld()` / `isShiftKeyDown()` whose bytecode verifies
`Minecraft.level : ClientLevel`, so the dedicated server died at mod construction
("Attempted to load class net/minecraft/client/multiplayer/ClientLevel for invalid
dist DEDICATED_SERVER").
- Fix: moved both helpers to client-only `thaumcraft/client/lib/ClientHelper.java`;
  `Thaumcraft` (common) no longer references client classes in its own bytecode.
- `PacketSealToClient` now calls `ClientHelper.getClientWorld()`.
- Lesson: audit all common classes for client-class bytecode references (Phase 7).

## 2026-10-11 — Phase 2: research system 1:1 with TC4

### Crash / data-integrity fixes found by the new server smoke test
- **Chunk save NPE (world corruption risk)**: `TileNode.generateId()` returned `""`
  without assigning `id` when `level == null` (chunk save on unload), so
  `saveAdditional` wrote a null `nodeId` -> `CompoundTag.putString` NPE ->
  "Failed to save chunk". Fixed in `TileNode.generateId()`.
- **@OnlyIn(CLIENT) field in a common packet**: `PacketAuraToClient.currentAura`
  was stripped on the dedicated server -> `<clinit>` NoSuchFieldError. Removed;
  aura data is stored in the client-only `HudHandler` again.
- **Recipe JSON errors** (10): 8 `smelting/*` used a string `result`
  (`"minecraft:charcoal"` instead of `{"id": ...}`); 2 `crucible/*`
  (`hedge_dye`, `tallow_candles`) used `ingredient` instead of `catalyst`.
- **AuraThreadManager** re-initialized + debug-logged every tick: `startThread`
  is now idempotent (handled-dimensions set; `hasThread` reflects it).

### Research model (TC4 semantics)
- `IPlayerKnowledge`/`PlayerKnowledge`: added `addDiscoveredAspect` /
  `hasDiscoveredAspect` (TC4: primal aspects always known; aspects stay
  discovered when the pool is spent). `addAspectsToResearchPool` now clamps at 0
  and keeps zero-amount keys; primals are auto-added to the pool list
  (TC4 `getAspectsDiscovered`).
- `ScanGeneric.onSuccess`: scanning now grants the object's aspects to the
  research pool with TC4's +2 first-discovery bonus and the 100-amount cap
  (TC4 `ScanManager.checkAndSyncAspectKnowledge`). Previously scans granted no
  aspects at all, making the hex table unplayable.
- `TileResearchTable.combineAspects`: first discovery of the combined aspect
  grants +2 bonus (TC4).
- `ResearchNotes.hasDiscoveredAspect` now uses the discovery set instead of
  "pool > 0".

### Research table GUI (1:1 TC4)
- `ResearchTableScreen` rewritten to the TC4 layout: 255x255 window, main blit
  (0,0,255,167) + strip (0,166,184,88) at +40/+167, hex canvas at +169/+83,
  single 5x5 aspect panel with paging arrows, combine button at +35/+139 with
  pressed frame, selector previews at +13/+71,+139, dup star at +37/+5, hover
  copy-cost banner at +100/+21, no-ink tooltip at +157/+84, wheel paging.
- Tooltips/dragged aspect/drag-orb moved out of `renderLabels` (they were drawn
  offset by leftPos/topPos) into `render()` after `super.render`.
- Runes rotate 90° like TC4; click sound is the TC4 `cameraclack` (3 ogg files
  copied from OpenTC4, sounds.json + ModSounds entry added).
- `ResearchTableMenu`: note slot (70,10), inventory rows 175/193/211, hotbar 233.
- Plain left click no longer quick-moves slots (TC4/vanilla behaviour).

### Research notes item
- `ItemResearchNotes`: damage-64 "complete" state exposed as `isComplete`,
  discovery name ("Discovery"), TC4 tooltip (§6 name, italic first stage,
  §5 forbidden-warp line), rarity EPIC set on completion.
- Model: `research_notes.json` override -> `discovery_notes.json`
  (discovery.png + discoveryoverlay.png, overlay tinted by `ItemColor`);
  removed the TC6-era `complete_notes` item/model/texture.

### Thaumonomicon
- TC4 behaviour: plays `thaumcraft:page`, normal variant completes siblings of
  completed research, cheat variant completes everything + discovers all aspects
  (pool 50) and syncs. Cheat model override added
  (`thaumcraft:tc_variant` property), cheat variant added to the creative tab.

### Browser
- Entry frames and category tabs now use the original TC4 `gui_research.png`
  art (26x26 frames at 0/26/54/86/110,230; 24x24 tabs at 152,232) instead of the
  port-only `gui_research_browser.png` (which was fully transparent, so all
  chrome was invisible). TC6-only RESEARCH/PAGE flag markers removed (TC4 has
  none).

### Research note creation now costs materials (TC4)
- `ResearchManager.createResearchNoteForPlayer`: reuses an existing note for the
  research, otherwise requires scribing tools with ink + paper (consumed), and
  `PacketSyncProgressToServer` uses it instead of creating notes for free.

## 2026-10-11 — Phase 3: Thaumometer 1:1 with TC4

- `ItemThaumometer` rewritten to TC4's hold-to-scan mechanic:
  - `getUseDuration` = 25 ticks, `UseAnim.NONE`; the item always enters use.
  - Client remembers the initially aimed target (`startScan`); every tick the
    target is re-checked and the scan is cancelled if it changed.
  - On completion (remaining <= 5) the HUD scan-result display is updated and
    `player.stopUsingItem()` is called (TC4 does the same).
  - `thaumcraft:cameraticks` plays every 2 ticks (volume 0.2, pitch
    0.45 + rand*0.1); the sound file was copied from OpenTC4 and registered in
    sounds.json + `ModSounds.CAMERATICKS`.
  - Range is TC4's 10 blocks (was 9); entity hits use the eye-height position
    for the runes effect like TC4.
  - Runes FX (`FXDispatcher.blockRunes`) play at the target when a scan starts.
  - The server applies the knowledge once when `remaining == 5` (the port has no
    PacketScannedToServer; equivalent behaviour without a new packet).
- Anti-farming (TC4 `isValidScanTarget` / `objectsScanned`):
  - `IPlayerKnowledge.isScanned/markScanned` (+ persistence and sync) added.
  - `ScanGeneric.getScanKey`: entities -> "E<type>", items/blocks ->
    "O<item-stack-hash>"; `ScanGeneric.onSuccess` now returns early when the
    object type was already scanned, so aspects/observation cannot be farmed.

## 2026-10-11 — testing pipeline + client-asset fixes

- `TESTING.md` documents the two working smoke tests (dedicated server + Xvfb
  llvmpipe client).
- Wand/rod/cap/thaumometer item textures live in `textures/models/`, which is
  NOT part of the vanilla `blocks` atlas; added
  `assets/minecraft/atlases/blocks.json` (NeoForge merges mod entries from the
  minecraft namespace resource stack) with a `models` directory source.
  Verified: client log went from 22 "Missing textures in model" warnings to 0.

## 2026-10-11 — Phase 3 additions (aspect chain + scanned-object registry)

- `ScanGeneric.validScan` gate (TC4): compound aspects cannot be learned before
  their component aspects; shows `tc.discoveryerror` with the missing parent's
  help text. The 48 TC4 `tc.aspect.help.*` language keys were added to en_us.
- `IPlayerKnowledge.isScanned/markScanned` + `ScanGeneric.getScanKey`: scanning
  the same object type twice no longer grants aspects (TC4 objectsScanned /
  entitiesScanned lists). Persisted in NBT and synced.

## 2026-10-11 — Phase 4a: wand assembly the TC4 way

User decision: TC4 has no "wand workbench" (wand assembly happens in the Arcane
Workbench; `gui_wandtable.png`/`wandtable.png` belong to the Focal Manipulator,
which IS the TC4 focus-upgrade table and stays).

Removed:
- `BlockWandWorkbench`, `TileWandWorkbench`, `WandWorkbenchMenu`,
  `WandWorkbenchScreen` + all registrations (`ModBlocks`, `ModBlockEntities`,
  `ModMenuTypes`, `Thaumcraft` screen, `ModCreativeTabs`), the
  `BlockArcaneWorkbenchCharger` special case, blockstate/models/lang keys,
  and the 99 `minecraft:crafting_shaped` wand recipes in `data/**/recipe/wand/`.

Added (1:1 OpenTC4):
- `ArcaneWandRecipe` (special arcane recipe): caps at (2,0)/(0,2), rod at (1,1),
  other slots empty; output = wand with rod/cap components; cost =
  capCraftCost * rodCraftCost of every primal aspect from the wand; requires
  `CAP_x` + `ROD_x`; excludes wood+iron.
- `ArcaneSceptreRecipe`: caps at (1,0)/(2,1)/(0,2), rod (1,1), primal charm
  (2,0); cost x1.5; requires the `SCEPTRE` research; sets the new
  `thaumcraft:wand_sceptre` component.
- Both are consulted first by `ThaumcraftCraftingManager.findMatchingArcaneRecipe`
  (TC4 craftingSpecial); the result slot consumes the grid-derived cost.
- `WAND_SCEPTRE` data component; `WandItem.isSceptre` (+50% capacity, -0.1 cost
  modifier).
- `WandItem.getConsumptionModifier` now uses TC4 semantics: the cap modifier
  *replaces* 1.0 (iron 1.1 = more expensive, void 0.8 = cheaper), particular
  aspects use the cap's special modifier, sceptre -0.1, clamp 0.1.
- `WandItem.getMaxVis` is now the per-aspect capacity (TC4); `getMaxVisTotal`
  for the HUD gauge; staff capacity comes from the rod definition only.
- Rod recharge is TC4's `WandRodPrimalOnUpdate` (elemental rods +1 vis every
  200 ticks up to max/10; primal staff every 50 ticks); the non-TC4 aura drain
  in `inventoryTick` was removed.
- `ConfigWands` completed: wood rod (stick, 25/1), elemental rods now carry
  their aspect onUpdate (blaze glows), staff rods obsidian (earth onUpdate),
  copper/silver caps registered with their TC4 special aspects, inert caps are
  no longer functional caps. `WandRod.getResearch` uses exact case (`ROD_tag`).
- New items `shard_balanced` (TC4 shard meta 6) and `primal_charm`
  (itemResource meta 15) with models/lang/creative-tab entries; textures were
  already present (copied from OpenTC4).
- New recipes: `crafting/wand_basic` (TC4 WandBasic), `arcane_workbench/
  rod_greatwood` + the three staff rods (TC4 patterns/costs), 6 crucible
  balanced-shard recipes, `arcane_workbench/primal_charm`, and 7 rod infusions
  (obsidian/ice/quartz/reed/blaze/bone/silverwood) with TC4 aspects,
  ingredients and instability.
- New research entries in auromancy.json (TC4 data): CAP_iron/CAP_gold/
  CAP_copper/CAP_thaumium/CAP_silver/CAP_void, ROD_wood/ROD_greatwood/ROD_reed/
  ROD_blaze/ROD_obsidian/ROD_ice/ROD_quartz/ROD_bone/ROD_silverwood, the three
  staff-rod entries, SCEPTRE and FOCALMANIPULATION. Names/page texts reuse the
  original TC4 `tc.research_name.*` / `tc.research_page.*` keys already present
  in the language files.






## Session 2026-10-13 (live in-game verification round)

Verified in-game by the assistant (Xvfb + xdotool screenshots):
- Leaves are finally TRANSPARENT in the world: vanilla `block/leaves` model has
  no `render_type` and custom LeavesBlocks are not in the vanilla render-layer
  map -> SOLID. Added `render_type: cutout_mipped` to both leaf models.
- Brain-in-a-jar: renderer used `models/brain.png` (16x16 placeholder);
  switched to the original TC4 `models/brain2.png` (256x128).
- Thaumometer HUD (stray white baseline stripe + shift numbers) removed
  entirely - TC4 has no aura gauge for the thaumometer (OpenTC4 check).
- Nitor: 16 colour blocks had identical cube icons; now 2D icons + item colour
  handler per dye.
- Creative tab: research table removed (TC4 mechanic = two tables + Inkwell),
  added wood/stone tables; the 153 pre-assembled wands replaced by a single
  greatwood+iron wand; rod/cap items back to 2D icons (25 models).
- Thaumonomicon: full TC4 rewrite of ResearchBrowserScreen (window 256x230,
  nine-slice frame, 224x196 background window, TC4 node frames/tabs). Fixed
  the two reasons nodes rendered as black squares: icon paths lost their
  `.png` (blit needs the full path) and blending was disabled between draw
  calls. gui_researchback.png is authentic TC4 (md5 match) - the "purple fog"
  IS the TC4 browser background; the page background is gui_researchbook.png.
- Block models (subagent): arcane workbench, workbench charger, bellows,
  arcane ear, pedestals, lamps were TC6 leftovers with placeholder textures;
  rebuilt from OpenTC4 renderers (original textures, vis_relay.obj for the
  charger, TC4 bellow animation).
- Wands (subagent): model overrides never applied because predicates in
  wand.json were `tc_rod`/`tc_cap` (parsed as `minecraft:tc_rod`) while the
  code registers `thaumcraft:tc_rod`. Regenerated wand_base/staff_base from
  TC4 ModelWand geometry, wand.json with 162 namespaced overrides, added the
  missing wood rod.
- TC6 removal (subagent): 194 files (vis crystals, turrets, collapser, charms,
  matrix speed/cost, dioptra, pattern crafter, void siphon, potion sprayer,
  vis generator/battery, condenser, taint crawler, golem upgrades). Kept TC4
  blocks that TC6-named versions shadowed (smelter = Alchemical Furnace,
  centrifuge, essentia reservoir, vis relay, seals system).
- Golem cores (subagent): one `golem_core` item with 15 TC4 variants
  (data component + overrides + original icons), 8 TC4 materials (straw, wood,
  tallow, clay, flesh, stone, iron, thaumium), 13 TC4 recipes.
- Crystals (subagent): JSON clusters with size 0..3 (BER would hide ~45
  already-placed blocks), candles: TC4-style BER (1-5 candles by seed).
- Stabilizer/chest/mirrors/crucible (subagent): OBJ stabilizer BER, 3D
  hungry-chest item, two-layer mirrors, crucible cauldron model.
