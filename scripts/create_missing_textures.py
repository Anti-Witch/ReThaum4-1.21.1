import os, shutil

BASE = 'C:/Users/SystemX/Desktop/mods_for_1.21.1/Thaumcraft-6-Source-Code-1.21.1'
TEX = os.path.join(BASE, 'src/main/resources/assets/thaumcraft/textures/item')
BLOCK_TEX = os.path.join(BASE, 'src/main/resources/assets/thaumcraft/textures/block')
OPEN_TC4 = 'C:/Users/SystemX/Desktop/mods_for_1.21.1/OpenTC4-master/OpenTC4-master/src/main/resources/assets/thaumcraft/textures/items'

def copy_texture(src_name, dst_name, src_dir=TEX):
    src = os.path.join(src_dir, src_name + '.png')
    dst = os.path.join(TEX, dst_name + '.png')
    if os.path.exists(src) and not os.path.exists(dst):
        shutil.copy2(src, dst)
        print(f"  COPIED: {src_name}.png -> {dst_name}.png")
        return True
    return False

def copy_from_opentc4(tc4_name, current_name):
    src = os.path.join(OPEN_TC4, tc4_name + '.png')
    dst = os.path.join(TEX, current_name + '.png')
    if os.path.exists(src) and not os.path.exists(dst):
        shutil.copy2(src, dst)
        print(f"  FROM TC4: {tc4_name}.png -> {current_name}.png")
        return True
    # Also try without underscores
    tc4_noun = tc4_name.replace('_', '')
    src2 = os.path.join(OPEN_TC4, tc4_noun + '.png')
    if os.path.exists(src2) and not os.path.exists(dst):
        shutil.copy2(src2, dst)
        print(f"  FROM TC4: {tc4_noun}.png -> {current_name}.png")
        return True
    return False

def copy_from_block(block_name, item_name):
    src = os.path.join(BLOCK_TEX, block_name + '.png')
    dst = os.path.join(TEX, item_name + '.png')
    if os.path.exists(src) and not os.path.exists(dst):
        shutil.copy2(src, dst)
        print(f"  FROM BLOCK: {block_name}.png -> {item_name}.png")
        return True
    return False

print("=" * 60)
print("PHASE 1: Copy textures for items with name matches")
print("=" * 60)

name_matches = {
    'amber_bead': 'amber',
    'amulet_vis_crafted': 'amulet_vis',
    'amulet_vis_found': 'amulet_vis',
    'arcane_door_key': 'arcanedoor',
    'creative_flux_sponge': 'flux_sponge',
    'enchanted_fabric': 'fabric',
    'focus_air': 'focus',
    'focus_earth': 'focus',
    'focus_entropy': 'focus',
    'focus_equal_trade': 'focus_trade',
    'focus_order': 'focus',
    'focus_portable_hole': 'focus_portablehole',
    'focus_treasure': 'focus',
    'focus_water': 'focus',
    'golem_core_clay': 'golem_clay',
    'golem_core_flesh': 'golem_flesh',
    'golem_core_iron': 'golem_iron',
    'golem_core_stone': 'golem_stone',
    'golem_core_straw': 'golem_straw',
    'golem_core_thaumium': 'golem_thaumium',
    'golem_core_wood': 'golem_wood',
    'hover_harness': 'hoverharness',
    'jar_brain': 'brain',
    'label_blank': 'label',
    'quicksilver_nugget': 'quicksilver',
    'triple_meat_treat': 'tripletreat',
    'wand_rod_greatwood_staff': 'wand_rod_greatwood',
    'wand_rod_obsidian_staff': 'wand_rod_obsidian',
    'wand_rod_silverwood_staff': 'wand_rod_silverwood',
    'wisp_essence': 'essence',
    'zombie_brain': 'brain',
}

copied = 0
for item, src_name in name_matches.items():
    if copy_texture(src_name, item):
        copied += 1

print(f"Phase 1: Copied {copied} textures from name matches")

print("\n" + "=" * 60)
print("PHASE 2: Copy from OpenTC4 originals")
print("=" * 60)

tc4_matches = {
    'alembic': 'alembic',
    'amulet_runic': 'runic_amulet',
    'belt_fancy': 'girdle_fancy',
    'bow_bone': 'bonebow',
    'chunks_beef': 'nuggetbeef',
    'chunks_chicken': 'nuggetchicken',
    'chunks_fish': 'nuggetfish',
    'chunks_pork': 'nuggetpork',
    'compass_stone': 'compass_stone',
    'complete_notes': 'researchnotes',
    'essentia_output': 'filter',
    'essentia_reservoir': 'resonator',
    'focus_upgrade_hiding': 'focus',
    'folio_omniscience': 'knowledgefragment',
    'girdle_hover': 'hovergirdle',
    'girdle_runic': 'runic_girdle',
    'golem_core_brass': 'golem_iron',
    'golem_core_void': 'golem_iron',
    'golem_upgrade_durability': 'golem_upgrade_empty',
    'golem_upgrade_flight': 'golem_upgrade_empty',
    'golem_upgrade_speed': 'golem_upgrade_empty',
    'golem_upgrade_stealth': 'golem_upgrade_empty',
    'golem_upgrade_strength': 'golem_upgrade_empty',
    'jar_node': 'brain',
    'mana_bean': 'mana_bean',
    'node': 'nitor',
    'primal_arrow': 'el_arrow_fire',
    'ring_runic': 'runic_ring',
    'seal_breaker': 'seals/seal_breaker',
    'seal_breaker_advanced': 'seals/seal_breaker_advanced',
    'seal_butcher': 'seals/seal_butcher',
    'seal_empty': 'seals/seal_empty',
    'seal_fill': 'seals/seal_fill',
    'seal_guard': 'seals/seal_guard',
    'seal_harvest': 'seals/seal_harvest',
    'seal_lumber': 'seals/seal_lumber',
    'seal_pickup': 'seals/seal_pickup',
    'seal_pickup_advanced': 'seals/seal_pickup_advanced',
    'seal_provider': 'seals/seal_provider',
    'seal_stock': 'seals/seal_stock',
    'seal_use': 'seals/seal_use',
    'smelter_basic': 'alumentum',
    'smelter_thaumium': 'alumentum',
    'smelter_void': 'alumentum',
    'spawner_egg': 'brain',
    'thaumium_ingot': 'thaumiumingot',
    'thaumium_nugget': 'nuggetthaumium',
    'thaumometer': 'thaumometer',
    'trunk_spawner': 'golem',
    'tube_directional': 'tube_normal',
    'tube_restricted': 'tube_normal',
    'turret_placer_bore': 'turret_basic',
    'verdant_charm_life': 'verdant_charm',
    'verdant_charm_sustain': 'verdant_charm',
    'void_metal_ingot': 'voidingot',
    'void_metal_nugget': 'nuggetvoid',
    'wand': 'wand_rod_greatwood',
    'creative_placer': 'creative_node',
}

copied2 = 0
for item, tc4_name in tc4_matches.items():
    dst = os.path.join(TEX, item + '.png')
    if os.path.exists(dst):
        continue
    if copy_from_opentc4(tc4_name, item):
        copied2 += 1
    elif copy_texture(tc4_name, item):
        copied2 += 1
    elif copy_texture(tc4_name.replace('/', '_'), item):
        copied2 += 1

print(f"Phase 2: Copied {copied2} textures from OpenTC4")

print("\n" + "=" * 60)
print("PHASE 3: Copy celestial notes from celestial/ subfolder")
print("=" * 60)

copied3 = 0
for i in range(1, 9):
    src = os.path.join(TEX, f'celestial/moon_{i}.png')
    dst = os.path.join(TEX, f'celestial_notes_moon_{i}.png')
    if os.path.exists(src) and not os.path.exists(dst):
        shutil.copy2(src, dst)
        print(f"  COPIED: celestial/moon_{i}.png -> celestial_notes_moon_{i}.png")
        copied3 += 1

for i in range(1, 5):
    src = os.path.join(TEX, f'celestial/stars_{i}.png')
    dst = os.path.join(TEX, f'celestial_notes_stars_{i}.png')
    if os.path.exists(src) and not os.path.exists(dst):
        shutil.copy2(src, dst)
        print(f"  COPIED: celestial/stars_{i}.png -> celestial_notes_stars_{i}.png")
        copied3 += 1

src = os.path.join(TEX, 'celestial/sun.png')
dst = os.path.join(TEX, 'celestial_notes_sun.png')
if os.path.exists(src) and not os.path.exists(dst):
    shutil.copy2(src, dst)
    print(f"  COPIED: celestial/sun.png -> celestial_notes_sun.png")
    copied3 += 1

print(f"Phase 3: Copied {copied3} celestial textures")

print("\n" + "=" * 60)
print("PHASE 4: Copy seal textures from seals/ subfolder")
print("=" * 60)

copied4 = 0
seal_items = [
    'seal_breaker', 'seal_breaker_advanced', 'seal_butcher', 'seal_empty',
    'seal_fill', 'seal_guard', 'seal_harvest', 'seal_lumber',
    'seal_pickup', 'seal_pickup_advanced', 'seal_provider', 'seal_stock', 'seal_use'
]
for item in seal_items:
    src = os.path.join(TEX, f'seals/{item}.png')
    dst = os.path.join(TEX, f'{item}.png')
    if os.path.exists(src) and not os.path.exists(dst):
        shutil.copy2(src, dst)
        print(f"  COPIED: seals/{item}.png -> {item}.png")
        copied4 += 1

print(f"Phase 4: Copied {copied4} seal textures")

print("\n" + "=" * 60)
print("PHASE 5: Copy vis_crystal textures from cluster/ as placeholder")
print("=" * 60)

copied5 = 0
vis_crystal_map = {
    'vis_crystal_air': 'cluster_iron',
    'vis_crystal_earth': 'cluster_iron',
    'vis_crystal_entropy': 'cluster_cinnabar',
    'vis_crystal_fire': 'cluster_gold',
    'vis_crystal_order': 'cluster_copper',
    'vis_crystal_water': 'cluster_iron',
}
for item, src_name in vis_crystal_map.items():
    if copy_texture(src_name, item):
        copied5 += 1

print(f"Phase 5: Copied {copied5} vis crystal textures")

print("\n" + "=" * 60)
print("PHASE 6: Final check - still missing")
print("=" * 60)

# Recheck
all_tex = set()
for root, dirs, files in os.walk(TEX):
    for f in files:
        if f.endswith('.png'):
            rel = os.path.relpath(os.path.join(root, f), TEX).replace(os.sep, '/').replace('.png', '')
            all_tex.add(rel)

import re
with open(os.path.join(BASE, 'src/main/java/thaumcraft/init/ModItems.java'), 'r', encoding='utf-8', errors='ignore') as f:
    moditems = f.read()
registered = set(re.findall(r'ITEMS\.register\("([^"]+)"', moditems))

still_missing = sorted(registered - all_tex)
print(f"Still missing: {len(still_missing)}")
for m in still_missing:
    print(f"  {m}")

print(f"\nTotal textures now: {len(all_tex)}")
