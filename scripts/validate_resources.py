#!/usr/bin/env python3
"""
validate_resources.py - валідатор blockstates / моделей / текстур порту Thaumcraft 4 -> 1.21.1.

Що перевіряє:
  1. blockstates без зареєстрованого блока            (dead files -> гра їх не вантажить)
  2. "textures" всередині variant/multipart            (у 1.21.1 IGNOURED -> мертвий override)
  3. variant/multipart -> посилання на модель, якої немає
  4. будь-яка модель -> батько (parent) існує
  5. будь-яка модель -> текстура (з урахуванням #ref через ланцюг батьків) існує
  6. зареєстрований блок без blockstate                (буде missing model у грі)

Реєстр блоків читається прямо з ModBlocks.java (registerBlock / registerBlockNoItem).
Ванільні моделі/текстури беруться з client resources jar (NeoForm intermediate result).

Запуск:  python3 scripts/validate_resources.py [--verbose]
"""

import json
import os
import re
import sys
import zipfile
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets")
TC = os.path.join(ASSETS, "thaumcraft")
MODBLOCKS = os.path.join(ROOT, "src/main/java/thaumcraft/init/ModBlocks.java")

VANILLA_JAR = None
for base in (
    os.path.expanduser("~/.gradle/caches/neoformruntime/intermediate_results"),
):
    if os.path.isdir(base):
        for name in os.listdir(base):
            if name.startswith("stripClient_") and name.endswith("_resourcesOutput.jar"):
                VANILLA_JAR = os.path.join(base, name)
if VANILLA_JAR is None:
    print("!! ванільний client-jar не знайдено - ванільні посилання перевірятись не будуть")

VERBOSE = "--verbose" in sys.argv


def load_vanilla():
    models, textures = set(), set()
    if not VANILLA_JAR:
        return models, textures
    with zipfile.ZipFile(VANILLA_JAR) as z:
        for n in z.namelist():
            if n.startswith("assets/minecraft/models/") and n.endswith(".json"):
                models.add(n[len("assets/minecraft/models/"):-len(".json")])
            elif n.startswith("assets/minecraft/textures/") and n.endswith(".png"):
                textures.add(n[len("assets/minecraft/textures/"):-len(".png")])
    return models, textures


def parse_registered_blocks():
    if not os.path.exists(MODBLOCKS):
        return set()
    src = open(MODBLOCKS, encoding="utf-8").read()
    names = re.findall(r'registerBlock(?:NoItem)?\(\s*"([^"]+)"\s*,', src)
    return set(names)


def model_path(ref, default_ns="minecraft"):
    if ":" in ref:
        ns, path = ref.split(":", 1)
    else:
        ns, path = default_ns, ref
    return ns, path


class Pack:
    def __init__(self):
        self.cache = {}
        self.vanilla_models, self.vanilla_textures = load_vanilla()
        self.blockstates = {}
        self.models = {}
        d = os.path.join(TC, "blockstates")
        for f in sorted(os.listdir(d)):
            if f.endswith(".json"):
                self.blockstates[f[:-5]] = os.path.join(d, f)
        d = os.path.join(TC, "models")
        self.models["block"] = set()
        self.models["item"] = set()
        if os.path.isdir(d):
            for sub in ("block", "item"):
                sd = os.path.join(d, sub)
                if os.path.isdir(sd):
                    for f in os.listdir(sd):
                        if f.endswith(".json"):
                            self.models[sub].add(f[:-5])

    def model_exists(self, ns, path):
        if path in ("builtin/generated", "builtin/entity", "builtin/missing"):
            return True  # спец-моделі MC (item/generated, entity, missing)
        if ns == "thaumcraft":
            kind = path.split("/", 1)
            if len(kind) != 2:
                return False
            return kind[1] in self.models.get(kind[0], set())
        if ns == "minecraft":
            return path in self.vanilla_models
        return True  # інші моди не перевіряємо

    def texture_exists(self, ns, path):
        if ns == "thaumcraft":
            return os.path.exists(os.path.join(TC, "textures", path + ".png"))
        if ns == "minecraft":
            return path in self.vanilla_textures
        return True

    def load_model(self, ns, path):
        key = (ns, path)
        if key in self.cache:
            return self.cache[key]
        data = None
        if ns == "thaumcraft":
            fp = os.path.join(TC, "models", path + ".json")
            if os.path.exists(fp):
                try:
                    data = json.load(open(fp, encoding="utf-8-sig"))
                except Exception as e:
                    data = {"__json_error__": str(e)}
        elif ns == "minecraft" and VANILLA_JAR:
            with zipfile.ZipFile(VANILLA_JAR) as z:
                try:
                    data = json.loads(z.read("assets/minecraft/models/%s.json" % path))
                except Exception:
                    data = None
        self.cache[key] = data
        return data


def iter_apply_entries(node):
    """yield (apply_dict, when_or_None) для variant/multipart apply будь-якої форми."""
    if isinstance(node, list):
        for x in node:
            yield from iter_apply_entries(x)
        return
    if isinstance(node, dict):
        if "apply" in node:
            ap = node["apply"]
            if isinstance(ap, list):
                for x in ap:
                    if isinstance(x, dict):
                        yield (x, node.get("when"))
            elif isinstance(ap, dict):
                yield (ap, node.get("when"))
        else:
            yield (node, None)


def resolve_texture(pack, model_ref, chain=None, depth=0):
    """Повертає (resolved_ref_or_None, problems[])."""
    problems = []
    chain = chain or []
    ns, path = model_path(model_ref)
    if (ns, path) in chain or depth > 12:
        return None, ["циклічний parent: %s" % model_ref]
    chain = chain + [(ns, path)]
    data = pack.load_model(ns, path)
    if data is None:
        return None, []
    if "__json_error__" in data:
        problems.append("битий JSON моделі %s: %s" % (model_ref, data["__json_error__"]))
        return None, problems
    parent = data.get("parent")
    if parent:
        pns, ppath = model_path(parent)
        if not pack.model_exists(pns, ppath):
            problems.append("немає батька %s (модель %s)" % (parent, model_ref))
        else:
            _, pproblems = resolve_texture(pack, parent, chain, depth + 1)
            problems.extend(pproblems)
    return (data, problems)


def main():
    pack = Pack()
    registered = parse_registered_blocks()
    problems = []
    dead_boostates = []
    dead_override = []
    missing_model_refs = []
    missing_parents = []
    missing_textures = []

    bs_names = set(pack.blockstates)
    for bs in sorted(bs_names - registered):
        dead_boostates.append(bs)

    for bs, fp in sorted(pack.blockstates.items()):
        try:
            data = json.load(open(fp, encoding="utf-8-sig"))
        except Exception as e:
            problems.append("blockstate %s: не валідний JSON: %s" % (bs, e))
            continue
        refs = []
        for key, var in (data.get("variants") or {}).items():
            for ent in (var if isinstance(var, list) else [var]):
                if not isinstance(ent, dict):
                    continue
                if "textures" in ent:
                    dead_override.append((bs, "variants[%s]" % key, None))
                refs.append((ent.get("model"), key))
        for ent, when in iter_apply_entries(data.get("multipart", [])):
            if "textures" in ent:
                dead_override.append((bs, "multipart", when))
            refs.append((ent.get("model"), when))
        for ref, when in refs:
            if ref is None:
                ref = "thaumcraft:block/" + bs  # implicit model for "*" key
            ns, path = model_path(ref)
            if not pack.model_exists(ns, path):
                missing_model_refs.append((bs, ref, when))
            elif ns == "thaumcraft":
                _, probs = resolve_texture(pack, ref)
                for p in probs:
                    if p.startswith("немає батька"):
                        missing_parents.append((ref, p))
                    else:
                        problems.append("%s -> %s" % (ref, p))

    # --- моделі: батьки та текстури ---
    for kind in ("block", "item"):
        for name in sorted(pack.models[kind]):
            ref = "thaumcraft:%s/%s" % (kind, name)
            data = pack.load_model("thaumcraft", "%s/%s" % (kind, name))
            if not isinstance(data, dict):
                problems.append("битий JSON моделі %s" % ref)
                continue
            if "__json_error__" in data:
                problems.append("битий JSON моделі %s: %s" % (ref, data["__json_error__"]))
                continue
            parent = data.get("parent")
            if parent:
                pns, ppath = model_path(parent)
                if not pack.model_exists(pns, ppath):
                    missing_parents.append((ref, "немає батька %s" % parent))
            for key, val in (data.get("textures") or {}).items():
                if val.startswith("#"):
                    continue
                tns, tpath = model_path(val)
                if not pack.texture_exists(tns, tpath):
                    missing_textures.append((ref, key, val))

    # --- звіт ---
    print("=" * 78)
    print("Валідація ресурсів Thaumcraft 4 -> 1.21.1")
    print("=" * 78)
    print("зареєстровано блоків : %d" % len(registered))
    print("blockstates файлів   : %d" % len(pack.blockstates))
    print("моделей block / item : %d / %d" % (len(pack.models["block"]), len(pack.models["item"])))
    print()

    def section(title, items, fmt=lambda x: "  " + str(x)):
        print("--- %s: %d" % (title, len(items)))
        for it in items:
            print(fmt(it))
        print()

    print("--- 3. посилання на неіснуючі моделі: %d" % len(missing_model_refs))
    for bs, ref, when in missing_model_refs:
        print("  %-28s -> %-45s when=%s" % (bs, ref, when))
    print()

    print("--- 4. неіснуючі parent: %d" % len(missing_parents))
    for ref, msg in missing_parents:
        print("  %-50s %s" % (ref, msg))
    print()

    print("--- 5. неіснуючі текстури в моделях: %d" % len(missing_textures))
    for ref, key, val in missing_textures:
        print("  %-50s %-10s %s" % (ref, key, val))
    print()

    print("--- битий JSON: %d" % len(problems))
    for p in problems:
        print("  " + p)
    print()

    print("--- 1. blockstates без зареєстрованого блока (гра не вантажить): %d" % len(dead_boostates))
    if VERBOSE:
        for d in dead_boostates:
            print("  " + d)
    else:
        print("  " + ", ".join(dead_boostates))
    print()

    print("--- 2. мертвий override \"textures\" у variant/multipart: %d файлів, %d місць" % (
        len(set(x[0] for x in dead_override)), len(dead_override)))
    for bs, where, when in dead_override:
        print("  %-28s %-9s when=%s" % (bs, where, when))
    print()

    print("--- 6. зареєстровані блоки без blockstate: %d" % len(registered - bs_names))
    for b in sorted(registered - bs_names):
        print("  " + b)

    bad = len(missing_model_refs) + len(missing_parents) + len(missing_textures) + len(problems)
    print()
    print("РАЗОМ критичних проблем (моделі/текстури): %d" % bad)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
