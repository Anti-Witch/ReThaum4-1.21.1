import os, re

# These textures are referenced by Java code for entity/armor/wand rendering
# even though no JSON model references them directly
KEEP_PATTERNS = [
    # Armor entity rendering (constructs paths like "textures/entity/armor/" + name)
    'cultistrobe', 'cultistplate', 'cultistboots',
    'thaumiumaxe', 'thaumiumboots', 'thaumiumchest', 'thaumiumhelm', 
    'thaumiumhoe', 'thaumiumpick', 'thaumiumshovel', 'thaumiumsword',
    'thaumiumfortress', 'thaumiumingot',
    'voidaxe', 'voidboots', 'voidchest', 'voidhelm', 'voidhoe',
    'voidlegs', 'voidpick', 'voidshovel', 'voidsword', 'voidingot', 'voidseed',
    'voidrobechest', 'voidrobechestover', 'voidrobehelm', 'voidrobelegs', 'voidrobelegsover',
    # Golem entity rendering
    'golem_clay', 'golem_iron', 'golem_stone', 'golem_straw', 'golem_tallow',
    'golem_thaumium', 'golem_flesh', 'golem_wood', 'golem_over',
    'golem_core', 'golem_upgrade', 'golemdeco',
    # Wand/staff rendering
    'staff_rod',
    # Baubles/accessories rendering
    'runic_', 'vis_amulet', 'hoverharness', 'hovergirdle', 'amulet_mundane',
    'girdle_fancy', 'girdle_mundane', 'ring_mundane',
    # Bone bow rendering
    'bonebow',
    # Celestial notes
    'celestial/moon', 'celestial/stars', 'celestial/sheet',
    # Overlay textures
    'cloth_boots_over', 'cloth_chest_over', 'cloth_legs_over',
    'discovery_overlay', 'discoveryoverlay',
    'label_overlay', 'research_notes_overlay',
    'curiosity_band_worn',
    'curio_',
    'focus_hellbat', 'focus_pech', 'focus_primal', 'focus_reversal',
    'focuspouchbauble',
    # Eldritch objects
    'eldritch_object',
    # Elemental arrows
    'el_arrow',
    # Cluster variants
    'cluster_lead', 'cluster_quartz', 'cluster_silver', 'cluster_tin',
    # Lightning rings
    'lightningring',
    # Sinister stones
    'sinister_stone',
    # Nuggets (used by loot tables)
    'nuggetcopper', 'nuggetlead', 'nuggetquicksilver', 'nuggetsilver',
    'nuggetthaumium', 'nuggettin', 'nuggetvoid',
    # Mirror system
    'mirrorframe', 'mirrorframe2', 'mirrorglass', 'mirrorhand',
    # Chunk variants
    'chunk_mutton', 'chunk_rabbit',
    # Knowledge/shard
    'knowledgefragment',
    # Phial
    'phialoverlay',
    'researchnotes', 'researchnotesoverlay',
    # Nuggets (old naming)
    'nugget_brass', 'nugget_copper', 'nugget_iron', 'nugget_lead',
    'nugget_quartz', 'nugget_silver', 'nugget_tin',
    # Veradant charm
    'verdant_charm_2', 'verdant_charm_over',
    # Gauntlet
    'caster_gauntlet', 'caster_gauntlet_focus',
    # Arcanedoor
    'arcanedoor',
    # Keygold/keyiron
    'keygold', 'keyiron',
    'gogglesrevealing',
    'amulet_vis_stone',
    'shard_balanced',
    'banner_symbol',
    'focuspouchbauble',
    'soap', 'scanscreen',
    'ironbell',
    'vis_amulet_lesser',
    'runic_ring', 'runic_ring_charged', 'runic_ring_lesser', 'runic_ring_regen',
    'runic_amulet', 'runic_amulet_emergency',
    'runic_girdle', 'runic_girdle_kinetic',
    'ob_placer',
    'turret_',
    'creative_',
    'nugget_brass', 'ingot_brass',
    'mind_biothaumic', 'mind_clockwork',
    'module_aggression', 'module_vision',
    'goggles_bauble',
]

# Items that were truly removed from code and are not used anywhere
DELETE_THESE = [
    'mind_biothaumic',
    'mind_clockwork', 
    'module_aggression',
    'module_vision',
    'turret_bore',
    'goggles_bauble',
    'ingot_brass',
    'nugget_brass',
    'creative_caster',
    'creative_obelisk',
]

# Read all Java files
all_java = ''
for root, dirs, files in os.walk('src/main/java'):
    for f in files:
        if f.endswith('.java'):
            try:
                with open(os.path.join(root, f), 'r', encoding='utf-8', errors='ignore') as fh:
                    all_java += fh.read() + '\n'
            except: pass

# Read all lang/recipe JSONs
all_json = ''
for root, dirs, files in os.walk('src/main/resources'):
    for f in files:
        if f.endswith('.json'):
            try:
                with open(os.path.join(root, f), 'r', encoding='utf-8', errors='ignore') as fh:
                    all_json += fh.read() + '\n'
            except: pass

tex_dir = 'src/main/resources/assets/thaumcraft/textures/item'
all_tex = []
for root, dirs, files in os.walk(tex_dir):
    for f in files:
        if f.endswith('.png'):
            rel = os.path.relpath(os.path.join(root, f), tex_dir).replace(os.sep, '/').replace('.png','')
            all_tex.append(rel)

# Model refs
model_refs = set()
import glob
for f in glob.glob('src/main/resources/assets/thaumcraft/models/item/*.json'):
    try:
        with open(f, 'r', encoding='utf-8-sig') as fh:
            content = fh.read()
        for m in re.findall(r'"thaumcraft:item/([^"]+)"', content):
            model_refs.add(m)
    except: pass

# Classify each texture
to_delete = []
to_keep_model = []
to_keep_code = []
to_keep_dynamic = []

for tex in sorted(all_tex):
    if tex in model_refs:
        to_keep_model.append(tex)
        continue
    
    # Check direct Java reference
    if tex in all_java:
        to_keep_code.append(tex)
        continue
    
    # Check if any KEEP pattern matches
    should_keep = False
    for pattern in KEEP_PATTERNS:
        if pattern in tex:
            should_keep = True
            break
    
    if should_keep:
        to_keep_dynamic.append(tex)
    else:
        to_delete.append(tex)

print(f'=== FINAL CLASSIFICATION ===')
print(f'Total textures: {len(all_tex)}')
print(f'Used by JSON models: {len(to_keep_model)}')
print(f'Used by Java code (direct): {len(to_keep_code)}')
print(f'Used by Java code (dynamic/entity): {len(to_keep_dynamic)}')
print(f'TRULY UNUSED: {len(to_delete)}')
print()
print(f'=== TO DELETE ({len(to_delete)}) ===')
for t in to_delete:
    print(f'  {t}')
print()
print(f'=== KEPT (dynamic/entity rendering) ({len(to_keep_dynamic)}) ===')
for t in to_keep_dynamic:
    print(f'  {t}')
