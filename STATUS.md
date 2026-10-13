# Port status — what's done, what isn't

An honest list of the current state of the Thaumcraft 4 → 1.21.1 port.
Last updated: 13 October 2026.

---

## Research & Thaumonomicon

**Works:**
- The full TC4 research tree: **201 entries** in 6 categories — Basics (19), Thaumaturgy (42),
  Alchemy (35), Artifice (50), Golemancy (39), Eldritch (16). It used to be a TC6 set of 156
  entries, of which 142 originals were missing and 97 were extra — all reworked.
- **310 pages** of text with `tc.research_page.*` keys, `<BR>/<PAGE>/<IMG>` markup like the original.
- Recipe pages resolve (158 links; previously none worked).
- Connection lines are drawn like TC4 — curved with a wiggle, not straight.
- The Thaumonomicon is obtained by **right-clicking a bookshelf with a wand** (the WandTrigger
  mechanism existed but its `init()` was never called — now it is).
- Category tabs are always open, Eldritch appears after ELDRITCHMINOR. `@N` in parents is stripped.
- 24 discovery triggers, 104 recipes remapped from deleted TC6 gates to TC4 keys.
- Research table (both tables + inkwell), theorycrafting, warp, knowledge sync with the server.

**Not done:**
- ~126 pages reference recipes that don't exist in the port yet: Focal Manipulator, infusion
  enchantment pages, golem upgrades, runes, masks, pure metals (Pure Tin/Silver/Lead), etc.
  Those pages currently show an empty recipe.
- No 3D blueprint previews (TC4 could rotate multiblocks on the page).
- No IMAGE / ASPECTS / SMELTING / INFUSION_ENCHANTMENT page types.
- Research texts are English-only; the Ukrainian locale has no research pages (309 pages untranslated).

---

## Wands & caps

**Works:**
- Wand assembly on the arcane workbench: rod + two caps (vis is drained from the wand in the slot).
- Sceptres: +50% capacity, −0.1 cost — like the original.
- All TC4 rods and caps, including staffs (blaze, ice, quartz, bone, reed, primal).
- In hand the wand is a real 3D model (orientation fixed: it used to lie horizontally because the
  geometry was along Z while the transforms were vanilla "handheld").
- In the inventory it's a compact diagonal stick, with no fake glint (the port had
  `isFoil` returning `true` always; TC4 has no glint).
- **Node charging**: hold right-click on a node and vis flows into the wand. This was completely
  dead: `TileNode.onUsingWandTick` existed but the wand never called it, and the helpers were
  stubs. Research gates (NODETAPPER1/2, NODEPRESERVE) and the "wood+iron can't drain" rule work.
- **Foci** (fire, shock, excavation) fire. They were completely broken: focus nodes never received
  a FocusPackage (no projectile spawned) and castFocusPackage only ran the first node, so TOUCH
  foci (excavation) never ran at all.
- **Focus particles**: projectiles have a trail, impacts burst with effect-specific particles
  (10 presets 1:1 from TC4), cast sounds returned (onCast was never called — it was silent).
- HUD: golden dial, **6 aspect capsules around the circle** (TC4: +90° rotation, offset 32),
  gain/loss arrows, focus in the centre. Fixed: staffs showed half capacity, Shift numbers were
  always "0".
- Focus insert/remove (F / Shift+F), focus pouch.

**Not done:**
- No WAVE/CHARGE cast animation (a plain hand swing exists).
- No wand recharging on a pedestal (WandItem isn't IRechargable).
- No node vis sync to the client — the node renderer shows stale colours until you rejoin.
- Preset focus recipes (creative only).
- HUD cosmetics: no cost markers on capsules (hud 136,0), no +90° rotation for the bottom dial.

---

## Alchemy

**Works:**
- Crucible: heating, dissolving thrown and held items, 49 recipes, phial from jars/alembics.
  Aspects no longer burn in a cold crucible (spill only when heat > 150).
- **Spills**: an overfilled crucible places flux goo/gas around itself (3×3×3), levels 0..7, gas
  rises and dissipates, goo spreads and taints the land. Previously it only "polluted aura" with
  no blocks, and BlockFluxGas didn't exist at all.
- **Alembics**: `BlockAlembic.newBlockEntity()` returned `null` — alembics had no tile entities and
  distillation didn't work. `TileAlembic` is hooked up and is an IAspectSource now.
- **Tubes**: `BlockTube.newBlockEntity()` also returned `null` — tubes were dead. Now they have
  block entities and tickers. The chain furnace → alembic → tubes → jars works.
- Jars with filters (labels), essentia reservoir, centrifuge.
- Flux goo: fixed a crash when two level-7 blocks merged.

**Not done:**
- Furnace variants (aux/thaumium/void/vent) — no tile entities, decorative only.
- No TC4 multiblocks (BlockMetalDevice/BlockAlchemyFurnace) — the port uses separate blocks.
- Balance differences: bellows, complex-aspect decomposition into primals.
- `ALCHEMICAL_BRASS_BLOCK` is a plain decorative block.

---

## Infusion (runic matrix)

**Works:**
- **Wand activation**: `TileInfusionMatrix.onCasterRightClick()` was dead code — the class didn't
  implement IWandable, so right-clicking with a wand did nothing. Now it works.
- Structure (4 pillars + centre pedestal), craft start, cycle, essentia drain from jars,
  instability (24 events), completion, cancellation — checked against decompiled TC4 4.2.3.5.
- **63 infusion recipes**.
- **Infusion enchantments**: 22 recipes (protection…infinity) — items get enchanted through the
  matrix, with player XP drained, like TC4.
- **NBT outputs**: `goggles_revealing` is now real — fortress helm + slime ball + goggles → a helm
  with `custom_data {goggles:true}`, keeping damage and masks.
- Animations: correct textures (infuser_arcane/ancient/eldritch — it used to draw a quarter of a
  block texture), a real additive halo (triangle fan), wobble.
- Particles: item/block streams from pedestals to the matrix, rune particles, sparks at negative
  stability, particles on asymmetric stabilisers.
- Sounds: infuserstart, infuser, craftstart, craftfail, wand.
- Fixed: crash (`UUID.fromString` on a player name), component-eating timing, item protection by
  the inlay grid and stabiliser, matrix explosion when broken mid-craft.

**Not done:**
- Research pages for infusion enchantments (text/render in the Thaumonomicon) and a JEI category.
- Custom TC4 enchantments `EnchantmentRepair` / `EnchantmentHaste` (InfEnchRepair/InfEnchHaste) —
  missing, so those two recipes aren't added.
- OBJ pillar rendering (pillar.obj + TileInfusionPillar) — pillars render simplified.
- matrix_speed / matrix_cost blocks (INFUSIONBOOST).

---

## Aura & nodes

**Works:**
- Per-dimension aura (vis + flux), background threads.
- Nodes: types (normal/unstable/dark/pure/tainted/hungry), modifiers (bright/faint/pale), random
  aspects. Creative now places a **random** node (previously a fixed NORMAL with 50 of each primal).
- Thaumometer sees nodes through walls (x-ray, depth-test off) in a view cone at distance 48.
- Goggles of Revealing: reveal only from the helmet slot (previously any armour piece with
  IGoggles, even a chestplate without goggles); without goggles nodes draw a faint outline.
- **Node in a Jar**: the jar is fully functional — the node inside is alive and glowing,
  right-clicking with a wand releases it back into the world, breaking it drops a `jar_node` item
  with all node data (type, modifier, aspects); place it back and the node is there. It used to be
  a stub and the node was simply lost.

**Not done:**
- In-world aspect icons above nodes/jars with `showIngamePopups` — never called.
- Node vis sync to the client (see above).

---

## Armour

**Works:**
- Goggles of Revealing: node x-ray, 5% vis discount.
- Traveller's Boots: speed + step assist (STEP_HEIGHT +0.4) + fall reduction (−0.25).
- Void Robe: self-repair, dyeing, warp 2 (fixed from 3).
- Cultist robe/boots: discount 1, warp 1 (added to boots).
- Goggle repair with gold, goggle recipe with gold (was thaumium/brass).
- **Hover Harness**: chestplate fuelled by **Potentia** (ENERGY aspect, not Aer), creative-style
  flight, 1 Potentia per 360 ticks (288 with the girdle), toggle key **H**, sounds, speed ×0.7.
  Refuels from jars and phials. Harness and girdle recipes as in TC4.
- **Fortress**: ISpecialArmor formulas work — magic /35, fire and explosions /20, rest /25, a full
  set + mask gives effective fire immunity (~110%), durability wears by absorbed damage.
  Set multiplier `0.875 + 0.125/piece + 0.05/mask` — as TC4.
- **Masks**: Angry Ghost withers attackers, Sipping Fiend heals HP on hit, Grinning Devil softens
  warp events. Infusion recipes for the helm-goggles and all three masks added.

**Not done:**
- Hover Harness GUI with a jar slot (in the port a jar is a block, not an NBT item).
- 3D armour models (fortress/cultist/praetor) — placeholders, need custom 128×64 gear models.
- Cultist leader: the port invented warp, TC4 has none (noted, untouched).

---

## Golemancy

**Works:**
- 15 golem cores (TC4 variants), 8 materials, 13 recipes.
- Basic spawn/behaviour.

**Not done:**
- Golem upgrades (all `golem_upgrade_*` were removed as TC6 content — a TC4 version is needed).
- Seals (48 files) — assets remain, items removed, nothing wired up.
- Full AI/behaviour parity.

---

## Misc

**Works:**
- Aspect orbs: mobs killed by a player drop coloured orbs (50% per primal, 1..N vis) that fly to a
  hotbar wand and charge it. Full EntityAspectOrb rewrite for TC4.
- Labels: right-click a jar/alembic/filter tube to set an aspect filter, shift-right-click to
  remove and drop a clean label. Previously no block implemented ILabelable — the label was dead.
- Deconstruction table: breaks items into aspect points (1:1 TC4 mechanics, GUI, render, arcane
  recipe).
- Devices: arcane workbench, research table, lamps (on/off), bellows, brain box, charger (OBJ),
  ear, pedestals, mirrors (2 layers), crucible-cauldron, 3D chest.
- Taint, flux goo/gas, taintacles, wisp essence with aspects.
- Data migrated to the 1.21.1 format: `c:` tags instead of `forge:`, `loot_table/`, `structure/`,
  112 replacements in 61 files.

**Not done:**
- Brain-in-a-jar XP interaction (TODO in BlockJar).
- Arcane Bore — block missing, recipe removed, research has a missing icon.
- Flux rifts — TC6 content, not removed (dead weight).
- 40 blockstates without textures (disabled content).
- focus_blank / focus_advanced — unclear if they're TC4 at all.

---

## Rendering & models

**Works:**
- 53 models got the correct `render_type` (tubes, pedestals, lamps, urns, bellows).
- Leaves — `cutout_mipped` (transparent), grass — `cutout_mipped`, 3 items with biome tint.
- Jars translucent, crystals as JSON clusters size 0..3, candles as BER, stabiliser as OBJ BER.
- Armour: 22 layer textures, materials, tint.
- Thaumometer: TC4 model in hand, hexagon in the inventory.
- Wands: 162 combo models, rod/cap as 2D.

**Not done:**
- Thaumonomicon blur on software rendering (llvmpipe, no GPU) — left as is; it's fine on a real GPU.
- Some BERs (e.g. vis relay — cube_all instead of OBJ).
- 3D armour models (see above).

---

## Localisation

**Works:**
- `uk_ua` (primary) + `en_us`, 49+ keys from recent fixes, staff rods, phials, taint,
  `tc.addaspectdiscovery` / `tc.addaspectpool` messages, `got.thaumonomicon`.
- The interface is fully Ukrainian (menus, creative, tooltips).

**Not done:**
- Research texts are English-only (309 pages).
- Some new items/blocks may only have en_us.

---

## What's next

Everything that was in progress is finished:
1. Hover Harness + Fortress — done (flight, armour formulas, masks, recipes).
2. Node in a Jar — done.
3. Focus particles — done (trail, impact, sounds).
4. Infusion enchantments — done (22 recipes + NBT outputs).

Next candidates:
- Research pages for infusion enchantments and a JEI category.
- Custom TC4 Repair/Haste enchantments (for InfEnchRepair/InfEnchHaste).
- 3D armour models for fortress/cultist/praetor.
- Golem upgrades and seals.
- The remaining ~126 recipes for research pages.

---

<details>
<summary>Українською</summary>

# Стан порту — що зроблено, що ні

Це чесний список поточного стану порту Thaumcraft 4 на 1.21.1.
Оновлено: 13 жовтня 2026.

## Дослідження і Таумономікон

**Працює:**
- Уся гілка досліджень TC4: **201 дослідження** у 6 категоріях — Основи (19), Тауматургія (42),
  Алхімія (35), Мистецтво (50), Големантія (39), Ельдрітч (16). Раніше стояв TC6-набір зі 156
  записів, з яких 142 оригінальних не було взагалі, а 97 були зайві — усе перебрано.
- **310 сторінок** тексту, розмітка `<BR>/<PAGE>/<IMG>` як в оригіналі.
- Рецепти на сторінках відкриваються (158 посилань; раніше не працювало жодне).
- Лінії між вузлами малюються як у TC4 — з вигином і «хитанням», а не прямими.
- Таумономікон видається ПКМ жезлом по книжковій полиці.
- Вкладки категорій відкриті завжди, Ельдрітч — після ELDRITCHMINOR.
- 24 discovery-тригери, 104 рецепти перемарковані з TC6-гейтів на TC4-ключі.
- Стіл досліджень, теоркрафт, warp, синхронізація знань.

**Не зроблено:**
- ~126 сторінок посилаються на рецепти, яких ще немає (Focal Manipulator, infusion-зачарування,
  голем-апгрейди, рунічні, маски, чисті метали).
- Немає 3D-прев'ю блюпринтів і типів сторінок IMAGE/ASPECTS/SMELTING/INFUSION_ENCHANTMENT.
- Тексти досліджень лише англійською (309 сторінок).

## Жезли і набалдашники

**Працює:**
- Збірка на аркан-верстаку (стрижень + 2 наконечники), скіпетри (+50%, −0.1).
- Усі стрижні/наконечники/посохи TC4. 3D-жезл у руці (орієнтацію виправлено), 2D-іконки.
- В інвентарі — компактна діагональна паличка, без фальшивого блиску (isFoil був `true` завжди).
- Заряджання від вузлів (було повністю мертве), гейти NODETAPPER1/2, NODEPRESERVE.
- Фокуси (вогонь/блискавка/копання) стріляють, частинки й звуки касту працюють.
- HUD: коло з 6 капсулами аспектів, стрілки, фокус у центрі.
- Вставляння/знімання фокусів (F / Shift+F), focus pouch.

**Не зроблено:**
- Анімація WAVE/CHARGE, заряджання на п'єдесталі, синк vis вузлів, рецепти пресет-фокусів,
  косметика HUD (позначки витрат).

## Алхімія

**Працює:**
- Тигель (нагрів, розчинення, 49 рецептів), протікання flux goo/gas (раніше лише «аура»).
- Алембіки й труби (обидва повертали `null` BE — були мертві), ланцюг печі → алембік → труби → банки.
- Банки з фільтрами, резервуар, центрифуга. Виправлено краш flux goo при злитті рівнів 7.

**Не зроблено:**
- Варіанти печі (aux/thaumium/void/vent) без тайлів, TC4-мультиблоків немає, розбіжності балансу.

## Інфузія

**Працює:**
- Активація жезлом (була мертвим кодом), структура, цикл, нестабільність, 63 рецепти.
- Infusion-зачарування: 22 рецепти, дрейн досвіду гравця.
- NBT-виводи: goggles_revealing (шолом fortress + slime + окуляри).
- Анімації (правильні текстури infuser, halo, wobble), частинки потоків, звуки.
- Виправлено краш UUID, таймінг, захист предметів, вибух матриці.

**Не зроблено:**
- Сторінки досліджень для infusion-зачарувань і JEI-категорія.
- Кастомні зачарування Repair/Haste (InfEnchRepair/InfEnchHaste).
- OBJ-рендер стовпа, блоки matrix_speed/cost.

## Аура і вузли

**Працює:**
- Аура по вимірах, вузли з випадковими типами/модифікаторами/аспектами (креатив теж).
- Таумометр бачить вузли крізь стіни, конус зору, дистанція 48.
- Окуляри: reveal лише зі шолома; без окулярів — слабкий контур.
- Node in a Jar: банка з вузлом жива, ПКМ жезлом випускає вузол, дроп `jar_node` з даними.

**Не зроблено:**
- Світові іконки аспектів (`showIngamePopups`), синк vis вузлів на клієнт.

## Броня

**Працює:**
- Окуляри одкровення (x-ray, дисконт 5%), чоботи мандрівника (крок-асист, падіння).
- Void Robe (warp 2), Cultist (warp 1), ремонт окулярів золотом.
- Hover Harness: політ на Potentia (клавіша H), рецепти, заправка з банок/фіалів.
- Fortress: формули ISpecialArmor, сет-множник, імунітет до вогню з маскою.
- Маски: Angry Ghost (Wither), Sipping Fiend (+HP), Grinning Devil (warp). Рецепти додано.

**Не зроблено:**
- ГУІ Hover Harness зі слотом під банку, 3D-моделі броні, вигаданий warp у cultist leader.

## Големи

**Працює:** 15 ядер, 8 матеріалів, 13 рецептів, базовий спавн.

**Не зроблено:** апгрейди, печатки (seal-система), повний паритет AI.

## Різне

**Працює:** аспект-орби з мобів, ярлики (банки/труби/алембіки), стіл розщеплення, пристрої,
тейнг, flux goo/gas, міграція даних на 1.21.1 (c:-теги, loot_table, structure).

**Не зроблено:** мозок у банці (XP), аркан-бур, flux rifts (TC6), 40 blockstates без текстур.

## Рендеринг і моделі

**Працює:** 53 моделі з правильним render_type, листя/трава cutout_mipped, банки translucent,
кристали-кластери, свічки BER, стабілізатор OBJ-BER, броня 22 шари, таумометр, жезли 162 комбо.

**Не зроблено:** розмиття Таумономікона на llvmpipe, деякі BER (vis relay), 3D-моделі броні.

## Локалізація

**Працює:** uk_ua (основна) + en_us, інтерфейс повністю українською.

**Не зроблено:** тексти досліджень лише англійською (309 сторінок).

## Що далі

Усе, що було в роботі, завершено: Hover Harness + Fortress, вузол у банці, частинки фокусів,
infusion-зачарування. Наступні кандидати: сторінки досліджень для infusion-зачарувань і JEI,
кастомні зачарування Repair/Haste, 3D-моделі броні, голем-апгрейди й печатки, решта ~126 рецептів.

</details>
