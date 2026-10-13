# Thaumcraft 4 Reforged

A community port of **Thaumcraft 4** (Minecraft 1.7.10) to **Minecraft 1.21.1 / NeoForge**.

Everything is recreated 1:1 from the original: mechanics, textures and models are taken from the
decompiled TC4 source (the OpenTC4 reference in the repo), not invented. When something is unclear,
we read the original code.

---

## Status: early alpha

The full loop is playable — research → wands → alchemy → infusion — but some systems are still
incomplete. Check the list below before reporting bugs. Full details in [STATUS.md](STATUS.md).

### What works

**Core**
- Aura per dimension (vis/flux), aura nodes with random types, modifiers and aspects.
- Thaumometer: 3D model in hand, item scanning, sees nodes through walls (x-ray).
- **Thaumonomicon**: the full TC4 tree — **201 entries** in 6 categories (Basics, Thaumaturgy,
  Alchemy, Artifice, Golemancy, Eldritch), 310 pages with text and recipes. Obtained by
  right-clicking a bookshelf with a wand.
- Research table, theorycrafting, warp, knowledge sync.

**Wands**
- Assembly on the arcane workbench (rod + 2 caps), sceptres (+50% capacity, −0.1 cost).
- All TC4 rods/caps/staffs. 3D wand in hand, 2D rod/cap icons.
- Vis charging from nodes (hold right-click on a node), node-tapper research gates.
- Focus casting: fire / shock / excavation — projectiles fire, vis is consumed, particles and
  cast sounds work.
- Wand HUD: golden dial with per-aspect capsules, gain/loss arrows, focus in the centre.

**Alchemy**
- Crucible (heating, dissolving, 49 recipes), alchemical furnace, alembics, tubes, warded jars,
  essentia reservoir, centrifuge.
- Flux spills: an overfilled crucible leaves flux goo/gas that spread and taint the land.

**Infusion**
- Runic matrix: wand activation, structure validation (4 pillars + pedestals), animations,
  pedestal particle streams, instability, **63 recipes**.
- **Infusion enchantments**: 22 recipes (protection…infinity) with player XP drain.
- NBT outputs: `goggles_revealing` builds real Goggles of Revealing from a fortress helm.

**Other**
- Aspect orbs: mobs killed by a player drop coloured orbs that fly into a hotbar wand and charge it.
- Labels: filters on jars, tubes and alembics (right-click to apply, shift-right-click to remove).
- Deconstruction table: breaks items down into aspect points.
- Golemancy (partial), armour (Goggles of Revealing, Traveller's Boots, Fortress with masks,
  Void Robe, Cultist), taint, arcane devices, mirrors, lamps, jars, Node in a Jar.

### What's not done

- Hover Harness GUI (jar slot) — the flight itself works, but the TC4 slot GUI isn't ported.
- ~126 research pages reference recipes that aren't in the port yet (Focal Manipulator,
  infusion enchantments pages, golem upgrades, runes, pure metals…).
- Research texts are English-only; the Ukrainian locale has no research pages yet.
- Focus WAVE/CHARGE cast animation, wand pedestal recharging, node vis client sync.
- Golem upgrades and the seal system.
- 3D armour models for fortress/cultist/praetor (placeholders).
- Some block entity renderers (e.g. vis relay), matrix speed/cost blocks.
- Thaumonomicon looks blurry on software rendering (Mesa llvmpipe, no GPU) — fine on a real GPU.

---

## Building

Requires **JDK 21**.

```bash
./gradlew build        # jar appears in build/libs/
./gradlew runClient    # dev client
```

Drop the jar into `mods/` on NeoForge 1.21.1. No extra dependencies (Curios is bundled).

## Licensing

- The original **Thaumcraft 4** belongs to **Azanor** (2013–2015) — all rights to the original
  mod, its code and assets are his.
- Everything created or modified by this port (port code, fixes, 1.21.1 adaptation) is **not**
  Azanor's and is licensed under **MIT** (see [LICENSE](LICENSE)).
- Sources and thanks: [CREDITS.md](CREDITS.md) — OpenTC4 (IgnoreLicensesCN, MIT),
  Thaumcraft 6 Source Code 1.20.1 (Neutronis228), Thaumcraft-1.20.1 (DreamYao520).

---

<details>
<summary>Українською</summary>

Порт **Thaumcraft 4** (Minecraft 1.7.10) на **Minecraft 1.21.1 / NeoForge**.

Все робимо 1:1 з оригіналом: механіки, текстури, моделі беремо з декомпільованого TC4
(еталон OpenTC4 лежить поруч), а не вигадуємо. Якщо сумнівно — ліземо в код і робимо як там.

**Стан: рання альфа.** Гру можна проходити: дослідження → жезли → алхімія → інфузія.
Але частина систем ще недороблена — повний список у [STATUS.md](STATUS.md).

**Що працює:**
- Аура vis/flux по вимірах, вузли з випадковими типами/модифікаторами/аспектами.
- Таумометр: 3D-модель у руці, сканування, бачить вузли крізь стіни.
- Таумономікон: усі 201 дослідження TC4 у 6 категоріях, 310 сторінок з текстом і рецептами.
  Дається ПКМ жезлом по книжковій полиці.
- Жезли: збірка на аркан-верстаку, скіпетри, заряджання від вузлів, фокуси з частинками,
  HUD з капсулами.
- Алхімія: тигель, печі, алембіки, труби, банки, есенція, протікання flux.
- Інфузія: рунічна матриця з анімаціями й частинками, 63 рецепти, infusion-зачарування
  (22 рецепти), NBT-виводи (окуляри одкровення).
- Аспект-орби з мобів, ярлики, стіл розщеплення, вузол у банці, броня з масками.

**Що не зроблено:**
- ГУІ Hover Harness (сам політ працює).
- ~126 сторінок досліджень посилаються на рецепти, яких ще немає.
- Тексти досліджень лише англійською.
- Анімація касту WAVE/CHARGE, заряджання жезла на п'єдесталі, синк vis вузлів.
- Голем-апгрейди й печатки.
- 3D-моделі броні fortress/cultist/praetor.
- Розмиття Таумономікона на програмному рендері (на реальній GPU чітко).

**Збірка:** JDK 21, `./gradlew build`. Jar кидаєш у `mods/` на NeoForge 1.21.1.

**Права:** оригінальний Thaumcraft 4 належить **Azanor**; усе, що зроблено/змінено цим портом —
під ліцензією **MIT** (див. `LICENSE` і `CREDITS.md`).

</details>
