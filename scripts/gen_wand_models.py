#!/usr/bin/env python3
"""
Generate the TC4 wand item models for the 1.21.1 port.

Geometry/UV source: OpenTC4 `client/renderers/models/gear/ModelWand.java`
(TC4 1.7.10, Azanor):

    Rod        addBox(-1,-1,-1, 2,18,2) @ texture offset (0, 8)
    Cap        addBox(-1,-1,-1, 2, 2,2) @ texture offset (0, 0)   (tip)
    CapBottom  addBox(-1,-1,-1, 2, 2,2) @ texture offset (0, 0)   (handle end)
    (staff rods are rendered with a 2x vertical scale of the same 18px rod)

ModelWand builds the ModelBoxes while `ModelBase.textureWidth/Height` are
32, so all box UVs are laid out in a 32x32 texture space; the JSON uv values
are therefore `pixel / 2` (16 units == whole texture).

    up    [tx+d      , ty  , tx+d+w    , ty+d  ]
    down  [tx+d+w    , ty  , tx+d+2w   , ty+d  ]
    west  [tx        , ty+d, tx+d      , ty+d+h]
    north [tx+d      , ty+d, tx+d+w    , ty+d+h]
    east  [tx+d+w    , ty+d, tx+d+w+d  , ty+d+h]
    south [tx+d+w+d  , ty+d, tx+d+w+d+w, ty+d+h]

The port keeps the wand oriented along +Z (pointing away from the player),
so the box unwrap is mapped onto the Z axis: each long face of the rod maps
1:1 onto one of the four 2x18px rod strips, the end faces onto the 2x2px
patches (exactly the vanilla per-direction uv mapping, no rotation needed).

Rod/cap sprite mapping (tags from ConfigWands / WandModelPredicates):

  wood            -> wand_rod_wood            (TC4 stick rod, craftable)
  greatwood       -> wand_rod_greatwood
  silverwood      -> wand_rod_silverwood
  obsidian        -> wand_rod_obsidian
  ice             -> wand_rod_ice
  quartz          -> wand_rod_quartz
  reed            -> wand_rod_reed
  blaze           -> wand_rod_blaze
  bone            -> wand_rod_bone
  *_staff         -> the matching *_staff sprite when it exists
                     (TC4 renders staffs with the regular rod sprite
                     stretched 2x, so e.g. blaze_staff -> wand_rod_blaze)
  primal_staff    -> wand_rod_primal          (48x64 sprite)
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src/main/resources/assets/thaumcraft")
ITEM = os.path.join(ROOT, "models/item")
COMBO = os.path.join(ITEM, "wand_c")

# ---------------------------------------------------------------- rod/cap data
# order == WandModelPredicates.RODS order (tc_rod = index + 1)
RODS = [
    ("wood", "wand_rod_wood"),
    ("greatwood", "wand_rod_greatwood"),
    ("silverwood", "wand_rod_silverwood"),
    ("obsidian", "wand_rod_obsidian"),
    ("ice", "wand_rod_ice"),
    ("quartz", "wand_rod_quartz"),
    ("reed", "wand_rod_reed"),
    ("blaze", "wand_rod_blaze"),
    ("bone", "wand_rod_bone"),
    ("greatwood_staff", "wand_rod_greatwood_staff"),
    ("silverwood_staff", "wand_rod_silverwood_staff"),
    ("obsidian_staff", "wand_rod_obsidian_staff"),
    ("blaze_staff", "wand_rod_blaze"),
    ("ice_staff", "wand_rod_ice"),
    ("quartz_staff", "wand_rod_quartz"),
    ("reed_staff", "wand_rod_reed"),
    ("bone_staff", "wand_rod_bone"),
    ("primal_staff", "wand_rod_primal"),
]
# order == WandModelPredicates.CAPS order (tc_cap = index + 1)
CAPS = [
    ("iron", "wand_cap_iron"),
    ("gold", "wand_cap_gold"),
    ("thaumium", "wand_cap_thaumium"),
    ("copper", "wand_cap_copper"),
    ("silver", "wand_cap_silver"),
    ("silver_inert", "wand_cap_silver_inert"),
    ("thaumium_inert", "wand_cap_thaumium_inert"),
    ("void", "wand_cap_void"),
    ("void_inert", "wand_cap_void_inert"),
]
STAFF_TAGS = {t for t, _ in RODS if t.endswith("_staff")}

PARTICLE = "thaumcraft:models/"


def tex(sprite):
    return PARTICLE + sprite


CREDIT = "Thaumcraft 4 (Azanor) - ported geometry"

DISPLAY = {
    # vanilla item/handheld style for the hand contexts (the model is Z-aligned
    # like a tool), a block-style gui rotation because the wand is 3D, and
    # neutral values elsewhere.
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.75, 0.75, 0.75]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, 0.5], "scale": [0.75, 0.75, 0.75]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.62, 0.62, 0.62]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.62, 0.62, 0.62]},
    "gui": {"rotation": [30, 225, 0], "scale": [0.65, 0.65, 0.65]},
    "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}

# per-context scale factor for the staff (its rod is 32px instead of 18px, so
# the model is shrunk to keep a comparable on-screen size; the gui keeps a bit
# more of the length, TC4 drew a visibly longer staff icon).
STAFF_FACTORS = {
    "thirdperson_righthand": 0.65,
    "thirdperson_lefthand": 0.65,
    "firstperson_righthand": 0.65,
    "firstperson_lefthand": 0.65,
    "gui": 0.75,
    "ground": 0.65,
    "fixed": 0.65,
    "head": 0.65,
}

def scaled_display(factors):
    """Staff display: the staff rod is 32px instead of 18px, so the model is
    scaled per context to keep a comparable on-screen size (TC4 scaled the
    staff render down -20% in the inventory - and it still looked longer)."""
    out = {}
    for ctx, t in DISPLAY.items():
        t2 = {k: (list(v) if isinstance(v, list) else v) for k, v in t.items()}
        f = factors.get(ctx, 1.0)
        if "scale" in t2:
            t2["scale"] = [round(s * f, 4) for s in t2["scale"]]
        out[ctx] = t2
    return out


STAFF_DISPLAY = scaled_display(STAFF_FACTORS)

# rod texture windows (ModelWand: box 2x18x2 @ (0,8), 32x32 space -> /2).
# The port's rod runs along Z, so the four 2x18px strips land on the four long
# faces (west/east/up/down) and the 2x2px end patches on north/south.
ROD_UV = {
    "west": [0, 5, 1, 14],   # strip drawn for TC4 -Y side
    "east": [2, 5, 3, 14],   # strip drawn for TC4 +Y side
    "up": [1, 5, 2, 14],     # strip drawn for TC4 +Z side
    "down": [3, 5, 4, 14],   # strip drawn for TC4 -Z side
    "north": [1, 4, 2, 5],   # 2x2 end patch (TC4 rod top)
    "south": [2, 4, 3, 5],   # 2x2 end patch (TC4 rod bottom)
}
# cap texture windows (ModelWand: box 2x2x2 @ (0,0))
CAP_UV = {
    "up": [1, 0, 2, 1],
    "down": [2, 0, 3, 1],
    "west": [0, 1, 1, 2],
    "north": [1, 1, 2, 2],
    "east": [2, 1, 3, 2],
    "south": [3, 1, 4, 2],
}


def box(frm, to, uv, texture):
    el = {"from": list(frm), "to": list(to), "faces": {}}
    for face in ("down", "up", "north", "south", "west", "east"):
        el["faces"][face] = {"uv": uv[face], "texture": texture}
    return el


def base_model(staff, rod_sprite, cap_sprite):
    """The 3D wand: rod + tip cap + handle cap, centred on the model box."""
    rod_len = 32 if staff else 18
    cap = 3  # cap cross-section / length in px (TC4 cap is 2px, scaled up)
    # rod spans z in [0, rod_len]; caps sit outside, then the whole thing is
    # recentred so that (8, 8, 8) - the model box centre - is in the middle.
    total = rod_len + 2 * cap
    z0 = round(8.0 - total / 2.0, 4)
    rod_from, rod_to = z0 + cap, z0 + cap + rod_len
    half = (cap - 2) / 2.0  # caps are `cap` wide, the rod 2 wide

    elements = [
        # handle cap (TC4 ModelWand.CapBottom)
        box((7 - half, 7 - half, z0), (9 + half, 9 + half, z0 + cap), CAP_UV, "#wand_cap"),
        # rod (TC4 ModelWand.Rod)
        box((7, 7, rod_from), (9, 9, rod_to), ROD_UV, "#wand_rod"),
        # tip cap (TC4 ModelWand.Cap)
        box((7 - half, 7 - half, rod_to), (9 + half, 9 + half, rod_to + cap), CAP_UV, "#wand_cap"),
    ]
    return {
        "credit": CREDIT,
        "parent": "block/block",
        "ambientocclusion": False,
        "textures": {
            "particle": tex(rod_sprite),
            "wand_rod": tex(rod_sprite),
            "wand_cap": tex(cap_sprite),
        },
        "elements": elements,
        "display": STAFF_DISPLAY if staff else DISPLAY,
    }


def combo_model(rod_tag, rod_sprite, cap_tag, cap_sprite):
    parent = "thaumcraft:item/wand_staff_base" if rod_tag in STAFF_TAGS else "thaumcraft:item/wand_base"
    return {
        "parent": parent,
        "textures": {
            "particle": tex(rod_sprite),
            "wand_rod": tex(rod_sprite),
            "wand_cap": tex(cap_sprite),
        },
    }


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


def main():
    write_json(os.path.join(ITEM, "wand_base.json"),
               base_model(False, "wand_rod_greatwood", "wand_cap_iron"))
    write_json(os.path.join(ITEM, "wand_staff_base.json"),
               base_model(True, "wand_rod_greatwood_staff", "wand_cap_iron"))

    overrides = []
    for ri, (rod_tag, rod_sprite) in enumerate(RODS, start=1):
        for ci, (cap_tag, cap_sprite) in enumerate(CAPS, start=1):
            name = f"{rod_tag}__{cap_tag}"
            write_json(os.path.join(COMBO, name + ".json"),
                       combo_model(rod_tag, rod_sprite, cap_tag, cap_sprite))
            overrides.append({
                "predicate": {"thaumcraft:tc_rod": ri, "thaumcraft:tc_cap": ci},
                "model": f"thaumcraft:item/wand_c/{name}",
            })

    # The item itself: no flat layer0 - TC4 always draws the assembled 3D wand
    # (ItemWandRenderer handles every render type, INVENTORY included). The
    # generic base doubles as the fallback when a wand has no rod/cap data.
    write_json(os.path.join(ITEM, "wand.json"), {
        "parent": "thaumcraft:item/wand_base",
        "overrides": overrides,
    })
    print(f"wrote wand_base/wand_staff_base, {len(overrides)} overrides and combo models")


if __name__ == "__main__":
    main()
