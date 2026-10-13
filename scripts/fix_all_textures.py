import os, re, glob, shutil

BASE = 'C:/Users/SystemX/Desktop/mods_for_1.21.1/Thaumcraft-6-Source-Code-1.21.1'
TEX_DIR = os.path.join(BASE, 'src/main/resources/assets/thaumcraft/textures/item')
BLOCK_TEX_DIR = os.path.join(BASE, 'src/main/resources/assets/thaumcraft/textures/block')
MODITEMS = os.path.join(BASE, 'src/main/java/thaumcraft/init/ModItems.java')
MODBLOCKS = os.path.join(BASE, 'src/main/java/thaumcraft/init/ModBlocks.java')

# ============ Step 1: Get all registered items ============
with open(MODITEMS, 'r', encoding='utf-8', errors='ignore') as f:
    moditems_content = f.read()

registered_items = re.findall(r'ITEMS\.register\("([^"]+)"', moditems_content)
print(f"=== Registered items: {len(registered_items)} ===")

# ============ Step 2: Check which have textures ============
all_item_textures = set()
for root, dirs, files in os.walk(TEX_DIR):
    for f in files:
        if f.endswith('.png'):
            rel = os.path.relpath(os.path.join(root, f), TEX_DIR).replace(os.sep, '/').replace('.png', '')
            all_item_textures.add(rel)

missing = []
for item in sorted(set(registered_items)):
    if item not in all_item_textures:
        missing.append(item)

print(f"\n=== Items WITHOUT textures: {len(missing)} ===")
for m in missing:
    print(f"  {m}")

# ============ Step 3: Find potential name matches ============
print(f"\n=== Checking name matches ===")
mapping = {}
for item in missing:
    # Direct match
    if item in all_item_textures:
        mapping[item] = item
        continue
    
    # Try common renames
    candidates = [
        item,
        item.replace('_', ''),
        item.replace('thaumium_', 'ingot_') if 'thaumium_' in item else None,
        item.replace('void_', 'ingot_void') if item == 'void_metal_ingot' else None,
        item.replace('void_metal_', '') if item.startswith('void_metal_') else None,
    ]
    
    # Try matching against existing textures with different naming
    for tex in all_item_textures:
        tex_norm = tex.replace('_', '').lower()
        item_norm = item.replace('_', '').lower()
        if tex_norm == item_norm:
            mapping[item] = tex
            break
        
        # Try partial matches
        parts = item.split('_')
        if len(parts) >= 2:
            for i in range(len(parts)):
                test = '_'.join(parts[:i] + parts[i+1:])
                if test.replace('_','') == tex.replace('_',''):
                    mapping[item] = tex
                    break
            if item in mapping:
                break

    if item not in mapping:
        # Check if it's a block that has a block texture
        block_tex_dir = os.path.join(BASE, 'src/main/resources/assets/thaumcraft/textures/block')
        if os.path.exists(os.path.join(block_tex_dir, item + '.png')):
            mapping[item] = f"block:{item}"
            continue

print(f"Found matches for {len(mapping)} items:")
for item, tex in sorted(mapping.items()):
    print(f"  {item} -> {tex}")

still_missing = [m for m in missing if m not in mapping]
print(f"\nStill need textures: {len(still_missing)}")
for m in still_missing:
    print(f"  MISSING: {m}")
