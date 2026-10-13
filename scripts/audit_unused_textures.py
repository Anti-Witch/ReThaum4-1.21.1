import json, glob, os, re

BASE = os.path.dirname(os.path.abspath(__file__))  # scripts/ -> project root is one up
BASE = os.path.dirname(BASE)
RES = os.path.join(BASE, "src/main/resources/assets/thaumcraft")
JAVA = os.path.join(BASE, "src/main/java/thaumcraft/init/ModItems.java")

# 1. Get all registered item names from ModItems.java (skip commented lines)
registered = set()
with open(JAVA) as f:
    for line in f:
        if line.strip().startswith('//'):
            continue
        m = re.search(r'register\("([^"]+)"', line)
        if m:
            registered.add(m.group(1))

# 2. Get all JSON model textures (direct + via overrides -> sub-models)
all_json_refs = set()
model_files = set()
for jf in glob.glob(os.path.join(RES, "models/item/*.json")):
    model_files.add(os.path.basename(jf).replace('.json', ''))
    with open(jf) as f:
        try:
            d = json.load(f)
            for v in d.get('textures', {}).values():
                if isinstance(v, str) and 'thaumcraft:item/' in v:
                    all_json_refs.add(v.replace('thaumcraft:item/', ''))
            for ov in d.get('overrides', []):
                m = ov.get('model', '')
                if m.startswith('thaumcraft:item/'):
                    mf = os.path.join(RES, m.replace('thaumcraft:', '') + '.json')
                    if os.path.exists(mf):
                        with open(mf) as mf2:
                            md = json.load(mf2)
                            for v in md.get('textures', {}).values():
                                if isinstance(v, str) and 'thaumcraft:item/' in v:
                                    all_json_refs.add(v.replace('thaumcraft:item/', ''))
        except:
            pass

# 3. Also resolve parent chains for full texture resolution
def resolve_parent(model_path, visited=None):
    if visited is None:
        visited = set()
    if model_path in visited:
        return set()
    visited.add(model_path)
    refs = set()
    if not os.path.exists(model_path):
        return refs
    with open(model_path) as f:
        try:
            d = json.load(f)
            for v in d.get('textures', {}).values():
                if isinstance(v, str) and 'thaumcraft:item/' in v:
                    refs.add(v.replace('thaumcraft:item/', ''))
            parent = d.get('parent', '')
            if parent and parent.startswith('thaumcraft:'):
                parent_path = os.path.join(RES, parent.replace('thaumcraft:', '') + '.json')
                refs |= resolve_parent(parent_path, visited)
        except:
            pass
    return refs

for jf in glob.glob(os.path.join(RES, "models/item/*.json")):
    all_json_refs |= resolve_parent(jf)
    with open(jf) as f:
        try:
            d = json.load(f)
            for ov in d.get('overrides', []):
                m = ov.get('model', '')
                if m.startswith('thaumcraft:item/'):
                    mf = os.path.join(RES, m.replace('thaumcraft:', '') + '.json')
                    all_json_refs |= resolve_parent(mf)
        except:
            pass

# 4. Actual textures
actual = {os.path.basename(f).replace('.png', '') for f in glob.glob(os.path.join(RES, "textures/item/*.png"))}

# 5. Items that NEED auto-texture (registered but no JSON model file)
needs_texture = registered - model_files

# 6. Truly unused
truly_unused = set()
for tex in actual:
    if tex in all_json_refs:
        continue
    if tex in needs_texture:
        continue
    if tex in registered:
        continue
    truly_unused.add(tex)

# 7. Items registered but MISSING texture
missing_texture = set()
for item in registered:
    if item not in actual and item not in model_files:
        missing_texture.add(item)

print(f"Registered items: {len(registered)}")
print(f"JSON model files: {len(model_files)}")
print(f"JSON texture refs (resolved): {len(all_json_refs)}")
print(f"Items needing auto-texture (no model): {len(needs_texture)}")
print(f"Actual textures: {len(actual)}")
print(f"")
print(f"=== TRULY UNUSED TEXTURES ({len(truly_unused)}) ===")
for t in sorted(truly_unused):
    print(f"  DELETE: {t}")
print(f"")
print(f"=== REGISTERED ITEMS WITHOUT TEXTURE ({len(missing_texture)}) ===")
for t in sorted(missing_texture):
    print(f"  NEED: {t}")
