import os, re, glob

all_java = ''
for root, dirs, files in os.walk('src/main/java'):
    for f in files:
        if f.endswith('.java'):
            try:
                with open(os.path.join(root, f), 'r', encoding='utf-8', errors='ignore') as fh:
                    all_java += fh.read() + '\n'
            except: pass

# All textures
tex_dir = 'src/main/resources/assets/thaumcraft/textures/item'
all_tex = []
for root, dirs, files in os.walk(tex_dir):
    for f in files:
        if f.endswith('.png'):
            rel = os.path.relpath(os.path.join(root, f), tex_dir).replace(os.sep, '/').replace('.png','')
            all_tex.append(rel)

# All model refs (item models)
model_refs = set()
for f in glob.glob('src/main/resources/assets/thaumcraft/models/item/*.json'):
    try:
        with open(f, 'r', encoding='utf-8-sig') as fh:
            content = fh.read()
        for m in re.findall(r'"thaumcraft:item/([^"]+)"', content):
            model_refs.add(m)
    except: pass

# Also check lang files for texture name references
all_lang = ''
for f in glob.glob('src/main/resources/assets/thaumcraft/lang/*.json'):
    try:
        with open(f, 'r', encoding='utf-8', errors='ignore') as fh:
            all_lang += fh.read()
    except: pass

# Find textures not in models AND not in Java code AND not in lang
unused = []
used_by_code = []
used_by_model = []

for tex in sorted(all_tex):
    if tex in model_refs:
        used_by_model.append(tex)
        continue
    # Check Java code
    if tex in all_java:
        used_by_code.append(tex)
        continue
    unused.append(tex)

print(f'Total textures: {len(all_tex)}')
print(f'Used by models: {len(used_by_model)}')
print(f'Used by Java code only: {len(used_by_code)}')
print(f'TRULY UNUSED: {len(unused)}')
print()
print('=== USED BY MODELS ===')
for t in used_by_model:
    print(f'  MODEL: {t}')
print()
print('=== USED BY JAVA CODE ===')
for t in used_by_code:
    print(f'  CODE: {t}')
print()
print('=== TRULY UNUSED (safe to delete) ===')
for t in unused:
    print(f'  DELETE: {t}')
