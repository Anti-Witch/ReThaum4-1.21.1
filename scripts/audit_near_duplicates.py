"""Пошук перефарбованих/сплюснутих/похідних текстур (near-duplicates).

Запуск: python3 scripts/audit_near_duplicates.py
Оригінали беруться з OPENCODE_TC4_ORIG / OPENCODE_TC6_ORIG (див. нижче).
"""
#!/usr/bin/env python3
"""Near-duplicate текстури: перефарбовані / сплюснуті / відредаговані копії."""
import colorsys
import hashlib
from pathlib import Path

import numpy as np
from PIL import Image

import os
PROJECT = Path(__file__).resolve().parents[1]
PORT = PROJECT / "src/main/resources/assets/thaumcraft/textures"
ORIG = Path(os.environ.get("OPENCODE_TC4_ORIG", "/workspaces/Minecorrupted/OpenTC4-master/OpenTC4-master/src/main/resources/assets/thaumcraft/textures"))
TC6 = Path(os.environ.get("OPENCODE_TC6_ORIG", "/workspaces/Minecorrupted/Thaumcraft-6-Source-Code/src/main/resources/assets/thaumcraft/textures"))
SIZE = 16

def collect(root):
    result = {}
    for p in root.rglob("*.png"):
        rel = str(p.relative_to(root)).replace("\\", "/")
        parts = rel.split("/")
        parts[0] = {"items": "item", "blocks": "block"}.get(parts[0], parts[0])
        result["/".join(parts)] = p
    return result

def prep(path):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    small = im.resize((SIZE, SIZE), Image.BILINEAR)
    arr = np.asarray(small, dtype=np.float32) / 255.0
    alpha = arr[..., 3]
    gray = arr[..., :3].mean(axis=2) * alpha
    vec = np.concatenate([(arr[..., :3] * alpha[..., None]).reshape(-1), alpha.reshape(-1)])
    norm = np.linalg.norm(vec)
    return {
        "arr": arr, "alpha": alpha, "gray": gray,
        "mask": alpha > 0.25,
        "vec": vec / norm if norm > 0 else vec,
        "dim": (w, h),
        "md5": hashlib.md5(path.read_bytes()).hexdigest(),
    }

def corr(a, b):
    m = a["mask"] | b["mask"]
    if m.sum() < 4:
        return 0.0
    ga, gb = a["gray"][m], b["gray"][m]
    if ga.std() < 1e-6 or gb.std() < 1e-6:
        return 0.0
    return float(np.corrcoef(ga, gb)[0, 1])

def iou(a, b):
    m1, m2 = a["mask"], b["mask"]
    u = (m1 | m2).sum()
    return float((m1 & m2).sum()) / float(u) if u else 0.0

def hue(a, b):
    m = (a["alpha"] > 0.4) & (b["alpha"] > 0.4)
    if m.sum() < 6:
        return None
    diffs = []
    for y, x in zip(*np.nonzero(m)):
        ha = colorsys.rgb_to_hsv(*a["arr"][y, x, :3])[0]
        hb = colorsys.rgb_to_hsv(*b["arr"][y, x, :3])[0]
        d = abs(ha - hb) * 360
        diffs.append(min(d, 360 - d))
    return float(np.mean(diffs)) if diffs else None

def classify(a, b):
    same_dim = a["dim"] == b["dim"]
    i = iou(a, b)
    c = corr(a, b)
    h = hue(a, b)
    flat_a = a["gray"].std() < 0.02
    flat_b = b["gray"].std() < 0.02
    if same_dim and i > 0.9 and c > 0.9:
        if h is not None and h > 10:
            return "RECOLOR", i, c, h
        if not flat_a and not flat_b and c > 0.97:
            return "EDITED", i, c, h
    if not same_dim and c > 0.9 and (i > 0.6 or (flat_a and flat_b)):
        return "RESIZE/SQUASH", i, c, h
    return None, i, c, h

def scan(names_a, data_a, names_b, data_b, label, cross):
    rows = []
    A = np.vstack([data_a[n]["vec"] for n in names_a])
    B = np.vstack([data_b[n]["vec"] for n in names_b])
    S = A @ B.T
    for i, na in enumerate(names_a):
        for j, nb in enumerate(names_b):
            if not cross and j <= i:
                continue
            if S[i, j] < 0.985:
                continue
            if data_a[na]["md5"] == data_b[nb]["md5"]:
                continue
            kind, ii, cc, hh = classify(data_a[na], data_b[nb])
            if kind:
                rows.append((kind, float(S[i, j]), na, nb, data_a[na]["dim"], data_b[nb]["dim"], ii, cc, hh))
    rows.sort(key=lambda r: (r[0], -r[1]))
    print(f"\n### {label}: {len(rows)} значущих")
    for kind, s, na, nb, da, db, ii, cc, hh in rows[:400]:
        extra = []
        if da != db:
            extra.append(f"{da}->{db}")
        if hh:
            extra.append(f"hue+{hh:.0f}°")
        print(f"- [{kind}] {s:.3f} iou={ii:.2f} corr={cc:.2f} {' '.join(extra)} `{na}` <-> `{nb}`")
    return rows

port_paths = collect(PORT)
orig_paths = collect(ORIG)
tc6_paths = collect(TC6)
port_names = sorted(port_paths)
orig_names = sorted(orig_paths)
tc6_names = sorted(tc6_paths)
orig_set = set(orig_names)
tc6_set = set(tc6_names)
port = {n: prep(port_paths[n]) for n in port_names}
orig = {n: prep(orig_paths[n]) for n in orig_names}
tc6 = {n: prep(tc6_paths[n]) for n in tc6_names}
print(f"port={len(port)} orig={len(orig)} tc6={len(tc6)}")

def tag(name):
    if name in orig_set:
        return "TC4"
    if name in tc6_set:
        return "TC6"
    return "ПОРТ"

def scan(names_a, data_a, names_b, data_b, label, cross, skip_both_tc4=False):
    rows = []
    A = np.vstack([data_a[n]["vec"] for n in names_a])
    B = np.vstack([data_b[n]["vec"] for n in names_b])
    S = A @ B.T
    for i, na in enumerate(names_a):
        for j, nb in enumerate(names_b):
            if not cross and j <= i:
                continue
            if skip_both_tc4 and na in orig_set and nb in orig_set:
                continue
            if S[i, j] < 0.985:
                continue
            if data_a[na]["md5"] == data_b[nb]["md5"]:
                continue
            kind, ii, cc, hh = classify(data_a[na], data_b[nb])
            if kind:
                rows.append((kind, float(S[i, j]), na, nb, data_a[na]["dim"], data_b[nb]["dim"], ii, cc, hh))
    rows.sort(key=lambda r: (r[0], -r[1]))
    print(f"\n### {label}: {len(rows)} значущих")
    for kind, s, na, nb, da, db, ii, cc, hh in rows[:400]:
        extra = []
        if da != db:
            extra.append(f"{da}->{db}")
        if hh:
            extra.append(f"hue+{hh:.0f}°")
        print(f"- [{kind}] {s:.3f} iou={ii:.2f} corr={cc:.2f} {' '.join(extra)} [{tag(na)}] `{na}` <-> [{tag(nb)}] `{nb}`")
    return rows

scan(port_names, port, port_names, port, "ПОРТ × ПОРТ (без пар TC4↔TC4)", cross=False, skip_both_tc4=True)
scan(port_names, port, orig_names, orig, "ПОРТ × TC4", cross=True)
scan(port_names, port, tc6_names, tc6, "ПОРТ × TC6", cross=True)
