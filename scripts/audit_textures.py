#!/usr/bin/env python3
"""Аудит текстур/моделей порту TC4→1.21.1 проти оригіналу OpenTC4.

Запуск (з кореня проєкту):
    python3 scripts/audit_textures.py

Оригінал шукається за шляхом у OPENCODE_TC4_ORIG (або стандартним
/workspaces/Minecorrupted/OpenTC4-master/OpenTC4-master/src/main/resources).
Звіт пише у ТЕКСТУРИ_АУДИТ_RAW.md (можна змінити змінною TC4_AUDIT_OUT).
"""
import hashlib
import json
import os
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    Image = None

PROJECT = Path(__file__).resolve().parents[1]
PORT_RES = PROJECT / "src/main/resources"
PORT_ASSETS = PORT_RES / "assets"
ORIG_RES = Path(
    os.environ.get(
        "OPENCODE_TC4_ORIG",
        "/workspaces/Minecorrupted/OpenTC4-master/OpenTC4-master/src/main/resources",
    )
)
ORIG_ASSETS = ORIG_RES / "assets"
OUT = Path(os.environ.get("TC4_AUDIT_OUT", PROJECT / "ТЕКСТУРИ_АУДИТ_RAW.md"))

report = []
def out(line=""):
    report.append(line)

def md5(p: Path) -> str:
    return hashlib.md5(p.read_bytes()).hexdigest()

tex_root = PORT_ASSETS / "thaumcraft/textures"
port_tex = {str(p.relative_to(tex_root)).replace(os.sep, "/"): p for p in tex_root.rglob("*.png")}
orig_tex = {}
for p in (ORIG_ASSETS / "thaumcraft/textures").rglob("*.png"):
    rel = str(p.relative_to(ORIG_ASSETS / "thaumcraft/textures")).replace(os.sep, "/")
    parts = rel.split("/")
    parts[0] = {"items": "item", "blocks": "block"}.get(parts[0], parts[0])
    orig_tex["/".join(parts)] = p

# --- JSON syntax / BOM ---
bad_json = []
for p in PORT_RES.rglob("*.json"):
    raw = p.read_bytes()
    rel = str(p.relative_to(PORT_RES))
    if raw[:3] == b"\xef\xbb\xbf":
        bad_json.append(("BOM", rel))
        continue
    try:
        json.loads(raw.decode("utf-8"))
    except Exception as exc:
        bad_json.append(("SYNTAX", f"{rel} ({exc})"))

# --- model refs ---
model_dir = PORT_ASSETS / "thaumcraft/models"
models = {str(p.relative_to(model_dir))[:-5].replace(os.sep, "/"): p for p in model_dir.rglob("*.json")}
missing_tex, missing_parent, usage = [], [], {}

def resolve(ref, default_ns):
    if ref.startswith("#"):
        return None
    if ":" in ref:
        ns, _, path = ref.partition(":")
    else:
        ns, path = default_ns, ref
    return ns, path

for mid, p in sorted(models.items()):
    try:
        data = json.loads(p.read_text(encoding="utf-8-sig"))
    except Exception:
        continue
    for val in (data.get("textures") or {}).values():
        if not isinstance(val, str):
            continue
        r = resolve(val, "thaumcraft")
        if r and r[0] == "thaumcraft":
            if (r[1] + ".png") not in port_tex:
                missing_tex.append((f"models/{mid}.json", val))
            else:
                usage.setdefault(r[1], set()).add(mid)
    parent = data.get("parent")
    if isinstance(parent, str):
        r = resolve(parent, "minecraft")
        if r and r[0] == "thaumcraft" and r[1] not in models:
            missing_parent.append((f"models/{mid}.json", parent))

# --- PNG checks ---
empty, strips = [], []
for rel, p in sorted(port_tex.items()):
    data = p.read_bytes()
    if len(data) == 0 or data[:8] != b"\x89PNG\r\n\x1a\n":
        continue
    if Image is None:
        continue
    try:
        img = Image.open(p)
        img.load()
    except Exception:
        continue
    w, h = img.size
    if img.mode in ("RGBA", "LA", "PA") and img.getchannel("A").getextrema() == (0, 0):
        counterpart = orig_tex.get(rel)
        blank_orig = False
        if counterpart:
            try:
                blank_orig = Image.open(counterpart).convert("RGBA").getchannel("A").getextrema() == (0, 0)
            except Exception:
                pass
        if not blank_orig:
            empty.append(rel)
    if h > w and h % w == 0 and h // w >= 2 and not Path(str(p) + ".mcmeta").exists():
        strips.append(f"{rel} ({w}x{h}, {h // w} frames)")

# --- java refs ---
import re
java_missing = {}
java_re = re.compile(r'"((?:textures/)[A-Za-z0-9_./-]+)"')
for p in (PROJECT / "src/main/java").rglob("*.java"):
    text = p.read_text(errors="replace")
    for m in java_re.finditer(text):
        rel = m.group(1)
        line_start = text.rfind("\n", 0, m.start()) + 1
        line_end = text.find("\n", m.end())
        line = text[line_start : line_end if line_end > 0 else len(text)]
        if re.search(r'withDefaultNamespace|"jei"|"minecraft"', line):
            continue
        if rel in ("textures/items/",) or rel.endswith("ripple") or rel.endswith("hemis"):
            continue
        if not (PORT_ASSETS / "thaumcraft" / rel).exists():
            java_missing.setdefault(rel, []).append(str(p.relative_to(PROJECT)))

# --- compare with original ---
orig_hash = {}
for rel, p in orig_tex.items():
    orig_hash.setdefault(md5(p), []).append(rel)
foreign = []
for rel, p in sorted(port_tex.items()):
    h = md5(p)
    originals = orig_hash.get(h)
    if originals and rel not in originals:
        foreign.append((rel, originals))

out("# RAW аудит текстур (автогенерація)")
out()
out(f"- BOM/битих JSON: {len(bad_json)}")
out(f"- Відсутніх текстур у моделях: {len(missing_tex)}")
out(f"- Відсутніх parent-моделей: {len(missing_parent)}")
out(f"- Порожніх (прозорих) текстур: {len(empty)}")
out(f"- Смуг без mcmeta: {len(strips)}")
out(f"- Java-посилань на відсутні текстури: {len(java_missing)}")
out(f"- «Чужих» текстур: {len(foreign)}")
out()
out("## Порожні")
out("\n".join(f"- {x}" for x in empty))
out()
out("## Смуги без mcmeta")
out("\n".join(f"- {x}" for x in strips))
out()
out("## Java missing")
out("\n".join(f"- `{k}` <- {', '.join(v[:2])}" for k, v in sorted(java_missing.items())))
out()
out("## Чужі текстури")
out("\n".join(f"- `{r}` == {o}" for r, o in sorted(foreign)))
out()
out("## BOM / биті JSON")
out("\n".join(f"- [{k}] `{r}`" for k, r in bad_json))
out()
out("## Перевикористання (>=5 моделей)")
for rel, mids in sorted(usage.items(), key=lambda kv: -len(kv[1])):
    if len(mids) >= 5:
        out(f"- `{rel}` — {len(mids)}: {', '.join(sorted(mids))}")

OUT.write_text("\n".join(report) + "\n")
print(f"OK -> {OUT}")
print(f"BOM={len(bad_json)} missing_tex={len(missing_tex)} empty={len(empty)} strips={len(strips)} java_missing={len(java_missing)} foreign={len(foreign)}")
