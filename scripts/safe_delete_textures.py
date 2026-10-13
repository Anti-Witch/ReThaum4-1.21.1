import json, glob, os, re

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(BASE, "src/main/resources/assets/thaumcraft")
ITEM_TEX = os.path.join(RES, "textures/item")
JAVA_SRC = os.path.join(BASE, "src/main/java/thaumcraft")

# 1. Get registered item names
registered = set()
with open(os.path.join(BASE, "src/main/java/thaumcraft/init/ModItems.java")) as f:
    for line in f:
        if line.strip().startswith('//'):
            continue
        m = re.search(r'register\("([^"]+)"', line)
        if m:
            registered.add(m.group(1))

# 2. Get ALL JSON model texture references (with parent chain resolution)
def resolve_model_refs(model_path, visited=None):
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
                refs |= resolve_model_refs(parent_path, visited)
        except:
            pass
    return refs

all_json_refs = set()
for jf in glob.glob(os.path.join(RES, "models/item/*.json")):
    refs = resolve_model_refs(jf)
    all_json_refs |= refs
    # Also resolve overrides
    with open(jf) as f:
        try:
            d = json.load(f)
            for ov in d.get('overrides', []):
                m = ov.get('model', '')
                if m.startswith('thaumcraft:item/'):
                    mf = os.path.join(RES, m.replace('thaumcraft:', '') + '.json')
                    all_json_refs |= resolve_model_refs(mf)
        except:
            pass

# 3. Also check models/item/ subdirectories
for subdir in glob.glob(os.path.join(RES, "models/item/*/")):
    for jf in glob.glob(os.path.join(subdir, "*.json")):
        all_json_refs |= resolve_model_refs(jf)

# 4. Check all model file names (items with models don't need explicit texture if model is named same)
model_files = set()
for jf in glob.glob(os.path.join(RES, "models/item/*.json")):
    model_files.add(os.path.basename(jf).replace('.json', ''))

# 5. Check Java code for texture references in textures/item/ (not textures/models/)
java_item_refs = set()
for root, dirs, files in os.walk(JAVA_SRC):
    for fname in files:
        if not fname.endswith('.java'):
            continue
        fpath = os.path.join(root, fname)
        if 'java_old' in fpath:
            continue
        try:
            with open(fpath, encoding='utf-8') as f:
                content = f.read()
            # Look for textures/item/ references
            for m in re.finditer(r'textures/item[s]?/([a-zA-Z0-9_/]+)\.png', content):
                java_item_refs.add(m.group(1))
        except:
            pass

# 6. Actual texture files
actual = {}
for f in glob.glob(os.path.join(ITEM_TEX, "*.png")):
    name = os.path.basename(f).replace('.png', '')
    actual[name] = f

# Also check subdirs
for subdir in glob.glob(os.path.join(ITEM_TEX, "*/")):
    for f in glob.glob(os.path.join(subdir, "*.png")):
        name = os.path.relpath(f, ITEM_TEX).replace('.png', '').replace('\\', '/')
        actual[name] = f

# 7. Find truly unused
safe_delete = []
need_keep = []

for tex_name, tex_path in sorted(actual.items()):
    reasons = []
    
    # Check if registered item name matches
    if tex_name in registered:
        reasons.append(f"registered item '{tex_name}'")
    
    # Check if referenced by JSON models
    if tex_name in all_json_refs:
        reasons.append("JSON model reference")
    
    # Check if it's a model file name (auto-texture fallback)
    if tex_name in model_files:
        reasons.append(f"matches model file '{tex_name}.json'")
    
    # Check Java code references (textures/item/ path)
    for jref in java_item_refs:
        if tex_name in jref or jref in tex_name:
            reasons.append(f"Java code ref: {jref}")
            break
    
    if reasons:
        need_keep.append((tex_name, reasons))
    else:
        safe_delete.append(tex_name)

print(f"Total textures: {len(actual)}")
print(f"Registered items: {len(registered)}")
print(f"JSON refs: {len(all_json_refs)}")
print(f"Model files: {len(model_files)}")
print(f"Java item refs: {len(java_item_refs)}")
print(f"")
print(f"=== SAFE TO DELETE ({len(safe_delete)}) ===")
for t in sorted(safe_delete):
    print(f"  {t}")

print(f"\n=== KEEP ({len(need_keep)}) ===")
for t, reasons in sorted(need_keep):
    print(f"  {t} <- {'; '.join(reasons)}")
