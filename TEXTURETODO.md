# Thaumcraft 6 - Remaining Texture/Model Tasks

Status of all texture/model work as of Phase 14 audit. Every item model texture reference, blockstate->model link and model parent chain has been programmatically verified: **0 missing references**.

---

## Resolved (was previously listed as missing)

| Item | Resolution |
|------|-----------|
| `thaumometer` (core item) | 5 generated part textures (body/brass/glass/handle/lens) - flat-color placeholders |
| `phial_empty` / `phial_filled` | Model + predicate rework via code (color tinting by content) |
| `research_notes` / `complete_notes` | Models fixed to existing textures |
| `label_blank` / `label_filled` | Fixed |
| `seal_provide` | Typo fixed (seal_provider) |
| `blank_seal` | Fixed |
| `primal_charm`, `bucket_pure`, `curiosity`, `brain_curious`, `caster_master` | Models fixed to existing textures |
| 12 `celestial_notes_*` variants | Models fixed (13 items in creative tab) |
| `golem_module_aggression/vision`, `grapple_gun_spool/tip` | Dedicated textures now used |
| Wand system (8 rods + 3 staff rods + 9 caps) | **99 combo textures generated** by compositing rod+cap overlays (System.Drawing script); plus rod/cap base textures |
| 8 wand/focus model JSONs with wrong texture refs | Re-pointed to correct rod/focus textures |

## Placeholder Quality (needs real art)

These exist and render, but are programmatically generated flat colors, not real art:

- `wand_<rod>_<cap>.png` - 99 combos (rod overlay + cap overlay composited)
- `thaumometer_body/brass/glass/handle/lens.png` - 5 flat-color parts

Real textures should be extracted from original Thaumcraft 6 (1.12.2) JAR: `assets/thaumcraft/textures/`.

## Substitute Block Textures (functional, not ideal)

| Block | Current Texture | Ideal |
|-------|-----------------|-------|
| `barrier` / `effect_sap` / `effect_shock` | `empty.png` | invisible effect blocks - OK as-is |
| `essentia_reservoir` | `metal_alchemical.png` | dedicated reservoir texture |
| `flux_scrubber` | `metal_alchemical_advanced.png` | dedicated scrubber texture |
| `focal_manipulator` | `arcane_workbench_*.png` | dedicated manipulator texture |
| `ancient_pedestal_doorway` | `ancient_stone_0.png` | doorway-specific texture |

## GUI Research Browser Category Icons

`textures/gui/category/*.png` (basics, alchemy, artifice, infusion, golemancy, eldritch, auromancy) - verify paths exist.

## Entity Textures (verify in game - `✉` no GPU available)

| Entity | Texture Path |
|--------|--------------|
| Golem | `textures/entity/golem/` (all material variants) |
| Wisp | `textures/entity/wisp.png` |
| Pech | `textures/entity/pech.png` |
| Cultists | `textures/entity/cultist/` (all variants) |
| Eldritch creatures | `textures/entity/eldritch/` (all variants) |

---

## Statistics (after Phase 14)

- Item models verified: **390** - 0 missing texture refs
- Blockstates verified: **281** - 0 missing model refs
- Model parent chains: 0 bad
- Wand combo textures generated: **99** + 5 thaumometer parts
- 121 `wand_*`/`thaumometer_*` files total in textures/item/

---

*Last Updated: August 11, 2026*
