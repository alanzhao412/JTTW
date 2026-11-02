"""Heaven's great names (its bosses): the Four Heavenly Kings here, Li Jing (heaven_great_li.py), Nezha
(heaven_great_nezha.py) and Erlang Shen (heaven_great_erlang.py). As hell_kings.py and king_yama.py:
each one's model and skins come from one list of parts (beastgen): the model is written into
HeavenGreatRenderers between its GENERATED markers, the skin painted to the same box-UV layout at twice
the model's resolution, with a glowing skin drawn full-bright over it and a third, the blaze, drawn in
its place while he winds up his great blow (heaven_great_kit.GreatPainter).

The Four Heavenly Kings (四大天王), as the gate halls of Chinese temples sculpt them: towering armoured
guardians, one frame for the four. A tall gold crown of five flame-arched plaques, a flaming jewel on
its knot, phoenix wings flaring up at its sides and ties hanging behind; a fierce face, eyes bulging
under brows knit in a V, a moustache swept out and up (and a beard in three locks, but for the East);
long-lobed ears. A breastplate moulded to the chest with two round mirrors (护心镜) and a gold collar,
the cloud collar over the shoulders; fish-scale gold over the belly and a lion's mask (吞口) biting the
belt; tiered pauldrons, a gold lion's head on each biting the arm; flaring silk at the elbows, gold
bracers; lamellar tassets over a long robe, an apron to the shins, armoured boots with upturned toes.
A short cape behind, and the heavenly ribbon (天衣) arcing behind his head in a great loop and streaming
down past his arms. Each king in his quarter's colours (the five phases) and with his attribute:

   0 East   Chiguo (持国)    white face  teal-green armour, ivory silk, orange ribbon   the pipa
   1 South  Zengzhang (增长) blue face   vermilion armour, crimson silk, teal ribbon   the great sword
   2 West   Guangmu (广目)   red face    silver armour, violet silk, red ribbon        the serpent and the pearl
   3 North  Duowen (多闻)    green face  black armour, navy silk, gold ribbon          the umbrella

Run from the repository root: python assets_source/heaven_great.py (all the great names), or
python assets_source/heaven_great.py kings (the Four Kings alone)."""
import math
import sys

import beastgen as bg
import heaven_great_kit as kit
from heaven_great_kit import (part, mirrored, aim, paint, emit, report, k, mix, clamp, each, noisy, lock, point,
                              GOLD, GOLD_DEEP, GOLD_LIGHT, GOLD_SHADOW, SILVER, IVORY, BLACK, HAIR, RED, RED_DEEP,
                              JADE, FIRE, FIRE_HOT, FIRE_DEEP, FIRE_WHITE, SIDES, LIGHT, BLUE_BLACK, BROWN_BLACK)
from sea_court import hang as _hang


def hang(name, parent, pivot, rot, segs, style):
    """sea_court.hang, each segment's turn from the last a number (about x) or a turn (x, y, z)."""
    return _hang(name, parent, pivot, rot, [(w, l, d, t if isinstance(t, tuple) else (t, 0, 0)) for w, l, d, t in segs], style)

SIZE = 0.88       # HeavenGreatRenderers.KING_SIZE

ARM, FOREARM = (-0.1, 0, 0.14), (-0.95, -0.35, 0)
LARM, LFOREARM = ((x, -y, -z) for x, y, z in (ARM, FOREARM))

# name, face, plate (armour enamel), silk (robe), ribbon, iris
KINGS = [
    ('chiguo', (238, 230, 218), (36, 128, 112), (230, 224, 204), (236, 132, 52), (232, 176, 60)),
    ('zengzhang', (66, 110, 196), (200, 52, 30), (146, 28, 30), (52, 150, 138), (255, 214, 80)),
    ('guangmu', (198, 58, 42), (214, 220, 232), (84, 62, 150), (204, 36, 34), (255, 200, 70)),
    ('duowen', (66, 150, 84), (40, 38, 54), (30, 40, 86), (242, 190, 56), (255, 130, 40)),
]
ATTRS = {'pipa': 0, 'sword': 1, 'serpent': 2, 'pearl': 2, 'umb': 3}


# ── The model ────────────────────────────────────────────────────────────────

def ribbon_arc(parent, cy, cz, R, a0, a1, n, w, style):
    """The heavenly ribbon arcing behind his head: flat segments round a circle about (0, cy, cz)."""
    out = []
    for i in range(n):
        a = math.radians(a0 + (a1 - a0) * (i + 0.5) / n)
        chord = 2 * R * math.sin(math.radians(abs(a1 - a0) / n) / 2) + 0.6
        out.append(part('%s_%d' % (style, i), parent, (R * math.sin(a), cy - R * math.cos(a), cz), (0, 0, a),
                        [(-chord / 2, -w / 2, -0.3, chord, w, 0.6, style)]))
    return out


def coil(parent, r, y0, y1, turns, n, thick0, thick1, style, start=0.0):
    """A serpent coiled about a forearm (its axis +y): segments along a helix, tail to head."""
    out = []
    pts = []
    for i in range(n + 1):
        t = i / n
        th = start + t * turns * 2 * math.pi
        pts.append((r * math.cos(th), y0 + (y1 - y0) * t, r * math.sin(th)))
    for i in range(n):
        xr, yr, length = bg.segment(pts[i], pts[i + 1])
        th = thick0 + (thick1 - thick0) * i / max(1, n - 1)
        st = style if i >= 3 else style + '_tail'
        out.append(part('%s_%d' % (style, i), parent, pts[i], (xr, yr, 0),
                        [(-th / 2, -th / 2, -0.2, th, th, length + 0.5, st)]))
    return out, pts[-1]


def path(parent, name, pts, thick, style, rot=(0, 0, 0)):
    """A body along points (a serpent's neck): each segment a child of `parent`, laid from one to the next."""
    out = []
    for i in range(len(pts) - 1):
        xr, yr, length = bg.segment(pts[i], pts[i + 1])
        t = thick[i]
        out.append(part('%s_%d' % (name, i), parent, pts[i], (xr, yr, 0), [(-t / 2, -t / 2, -0.3, t, t, length + 0.6, style)]))
    return out


def umbrella():
    out = [part('umb', 'right_hand', (0, 2, 0), aim([ARM, FOREARM], (0, 0, 0)), [
        (-0.6, -40, -0.6, 1.2, 47, 1.2, 'umb_shaft'),
        (-1.0, -41, -1.0, 2.0, 2.2, 2.0, 'umb_hub'),
        (-1.6, -44.6, -1.6, 3.2, 3.6, 3.2, 'umb_top'),
        (-0.9, -47.2, -0.9, 1.8, 2.6, 1.8, 'umb_jewel'),
        (-0.9, 6.2, -0.9, 1.8, 1.4, 1.8, 'umb_butt'),
    ])]
    n = 12
    for i in range(n):
        th = 2 * math.pi * i / n
        ox, oz = -0.9 * math.sin(th), -0.9 * math.cos(th)
        out.append(part('umb_panel_%d' % i, 'umb', (ox, -40, oz), (-0.12, th, 0), [(-3.3, 0, -0.3, 6.6, 13, 0.6, 'umb_panel')]))
        out.append(part('umb_tassel_%d' % i, 'umb_panel_%d' % i, (0, 12.6, -0.4), (0.12, 0, 0), [
            (-0.3, 0, -0.3, 0.6, 3.4, 0.6, 'umb_cord'), (-0.8, 3.2, -0.8, 1.6, 1.6, 1.6, 'umb_bead'),
            (-0.6, 4.6, -0.6, 1.2, 2.6, 1.2, 'umb_tuft')]))
    for i in range(6):                                  # the canopy's crown: a little fixed tier over it
        th = 2 * math.pi * (i + 0.5) / 6
        out.append(part('umb_cap_%d' % i, 'umb', (-0.6 * math.sin(th), -42.6, -0.6 * math.cos(th)), (-1.0, th, 0),
                        [(-1.6, 0, -0.3, 3.2, 4.2, 0.6, 'umb_cap')]))
    return out


def serpent():
    """The serpent-dragon coiled about his left forearm, rising from his fist, its head reared over it."""
    parts = [part('coil', 'left_forearm', (0, 0, 0), (0, 0, 0), [])]
    c, end = coil('coil', 3.5, 0.2, 6.4, 1.75, 14, 1.2, 2.3, 'coil', start=0.4)
    parts += c
    # the neck rises from the coil's end at the wrist; its frame the model's own (aim), so it rears by turning
    parts.append(part('serpent', 'left_forearm', (end[0], end[1], end[2]), aim([LARM, LFOREARM], (0, 0, 0)), []))
    neck = [(0, 0, 0), (0.6, -3.2, 0.4), (1.0, -6.4, -0.6), (0.9, -9.2, -2.6), (0.6, -10.6, -5.0)]
    parts += path('serpent', 'serpent_neck', neck, [2.4, 2.3, 2.2, 2.1], 'serpent_neck')
    hx, hy, hz = neck[-1]
    parts.append(part('serpent_head', 'serpent', (hx, hy, hz), (0.15, 0, 0), [
        (-1.7, -1.4, -4.4, 3.4, 2.6, 4.6, 'serpent_skull'),
        (-1.3, -0.8, -7.2, 2.6, 1.6, 3.0, 'serpent_snout'),
        (-1.9, -2.0, -3.6, 0.8, 1.2, 1.6, 'serpent_brow'),
        (1.1, -2.0, -3.6, 0.8, 1.2, 1.6, 'serpent_brow'),
    ]))
    parts.append(part('right_serpent_horn', 'serpent_head', (-1.0, -1.6, -1.0), (-0.9, 0, -0.35), [(-0.4, -0.4, 0, 0.8, 0.8, 3.6, 'serpent_horn')]))
    parts.append(part('right_serpent_whisker', 'serpent_head', (-1.2, 0.2, -6.4), (0.2, 0.9, 0.3), [(-0.2, -0.2, 0, 0.4, 0.4, 4.0, 'serpent_whisker')]))
    parts.append(part('serpent_jaw', 'serpent_head', (0, 1.1, -0.8), (0.12, 0, 0), [
        (-1.4, 0, -6.2, 2.8, 0.9, 6.2, 'serpent_jaw')]))
    parts.append(part('serpent_tongue', 'serpent_jaw', (0, 0, -6.0), (0.25, 0, 0), [(-0.4, -0.2, -2.6, 0.8, 0.3, 2.6, 'serpent_tongue')]))
    return parts


def pipa():
    return [
        part('pipa', 'body', (-1.6, -6.6, -9.4), (-0.12, 0.05, 0.9), [
            (-4.4, 0, -1.4, 8.8, 7.5, 2.4, 'pipa_bowl'),
            (-3.6, 7.2, -1.3, 7.2, 1.2, 2.2, 'pipa_foot'),
            (-3.8, -2.6, -1.3, 7.6, 2.8, 2.2, 'pipa_belly'),
            (-2.8, -4.9, -1.2, 5.6, 2.5, 2.0, 'pipa_shoulder'),
            (-1.1, -14.6, -0.9, 2.2, 10, 1.6, 'pipa_neck'),
            (-2.4, 4.4, -1.8, 4.8, 0.7, 0.6, 'pipa_bridge'),
        ] + [(-1.4, -6.6 - 2 * i, -1.1, 2.8, 0.5, 0.4, 'pipa_fret') for i in range(4)]),
        part('pipa_head', 'pipa', (0, -14.4, 0), (0.55, 0, 0), [
            (-1.2, -4.4, -0.7, 2.4, 4.6, 1.4, 'pipa_head'),
            (-2.4, -1.6, -0.3, 4.8, 0.6, 0.6, 'pipa_peg'),
            (-2.4, -3.4, -0.3, 4.8, 0.6, 0.6, 'pipa_peg'),
        ]),
    ] + hang('pipa_tassel', 'pipa_head', (0, -4.0, 0.4), (-0.55, 0, 0), [(0.5, 3, 0.5, 0), (1.4, 3.6, 1.4, 0.05)], 'pipa_tassel')


def sword():
    return [part('sword', 'right_hand', (0, 2, 0), aim([ARM, FOREARM], (-0.12, 0, 0)), [
        (-0.7, -2.6, -0.7, 1.4, 6, 1.4, 'sword_grip'),
        (-1.2, 3.3, -1.2, 2.4, 1.6, 2.4, 'sword_pommel'),
        (-3.0, -4.0, -1.3, 6.0, 1.5, 2.6, 'sword_guard'),
        (-1.8, -5.4, -0.9, 3.6, 1.5, 1.8, 'sword_root'),
        (-1.4, -32, -0.35, 2.8, 26.8, 0.7, 'sword_blade'),
    ])] + hang('sword_tassel', 'sword', (0, 4.8, 0), (0, 0, 0), [(0.4, 2.4, 0.4, 0), (1.4, 4.2, 1.4, 0.05)], 'sword_tassel')


def pearl():
    return [part('pearl', 'right_hand', (0, -0.2, -0.2), aim([ARM, FOREARM], (0, 0, 0)), [
        (-2.4, -1.2, -2.4, 4.8, 1.4, 4.8, 'pearl_cup'),
        (-2.0, -5.2, -2.0, 4.0, 4.0, 4.0, 'pearl'),
    ]),
        part('pearl_flame', 'pearl', (0, -3.2, 0), (0, 0.6, 0), [
            (-3.6, -5.4, -0.25, 7.2, 7.2, 0.5, 'pearl_flame'), (-0.25, -5.4, -3.6, 0.5, 7.2, 7.2, 'pearl_flame_x')]),
    ]


KING = mirrored([
    # the waist: hips, belt, the lion's mask biting it
    part('waist', 'root', (0, 4, 0), (0, 0, 0), [
        (-7, -2, -4.6, 14, 4, 9.2, 'hips'),
        (-8, -4.6, -5.8, 16, 3.8, 11.6, 'belt'),
        (-3.6, -6.8, -7.8, 7.2, 6.2, 2, 'buckle_mask'),
        (-4.6, -7.6, -6.8, 9.2, 7.6, 1, 'buckle_mane'),
    ]),
    part('apron', 'waist', (0, 0.8, -6.0), (-0.06, 0, 0), [(-3.2, 0, -0.5, 6.4, 16.5, 0.8, 'apron')]),
    part('right_tasset', 'waist', (-4.4, 0.2, -5.1), (-0.14, 0.12, 0.12), [(-3.8, 0, -0.6, 7.6, 11.5, 1.2, 'tasset')]),
    part('right_side_tasset', 'waist', (-7.6, 0.2, 0.2), (0, 0, 0.22), [(-1.2, 0, -4.8, 1.2, 11, 9.6, 'tasset_side')]),
    part('back_tasset', 'waist', (0, 0.2, 5.4), (0.14, 0, 0), [(-7.2, 0, -0.6, 14.4, 12, 1.2, 'tasset_back')]),
] + hang('right_sash', 'waist', (-7.9, -1.6, -3.2), (0.02, 0, 0.1), [(2.0, 6, 0.6, 0), (2.2, 6, 0.6, 0.06), (2.6, 5, 0.6, 0.08)], 'sash') + [
    # the legs: billowing trousers, a robe over the thigh, greaves, armoured boots
    part('right_leg', 'root', (-3.8, 6, 0.4), (0, 0, 0), [(-3.1, 0, -3.1, 6.2, 9.4, 6.2, 'thigh')]),
    part('right_robe', 'right_leg', (0, -0.6, -3.3), (-0.05, 0, 0.04), [(-3.5, 0, -0.5, 7, 13.6, 0.6, 'robe')]),
    part('right_robe_back', 'right_leg', (0, -0.6, 3.3), (0.06, 0, 0.04), [(-3.5, 0, -0.1, 7, 13.6, 0.6, 'robe')]),
    part('right_shin', 'right_leg', (0, 9, 0), (0, 0, 0), [
        (-2.7, 0, -2.7, 5.4, 6, 5.4, 'shin'),
        (-3.1, 0.6, -3.4, 6.2, 5.2, 3.4, 'greave'),
        (-3.2, 5.6, -3.6, 6.4, 3.4, 6.8, 'boot'),
        (-2.4, 7.0, -5.6, 4.8, 2.0, 2.2, 'boot_toe'),
        (-1.4, 5.8, -6.3, 2.8, 2.0, 1.0, 'boot_tip'),
    ]),
    # the body: torso, breastplate and its mirrors, fish-scale belly, gorget, cloud collar
    part('body', 'waist', (0, -3, 0), (0, 0, 0), [
        (-7.5, -16, -5, 15, 15, 10, 'torso'),
        (-8.3, -16.4, -5.9, 16.6, 9, 11.8, 'breastplate'),
        (-7.4, -8, -5.6, 14.8, 7.6, 11.2, 'abdomen'),
        (-5.4, -7.4, -6.7, 10.8, 4.8, 1.2, 'belly'),
        (-6.4, -15.2, -6.5, 5.2, 5.2, 0.7, 'mirror'),
        (1.2, -15.2, -6.5, 5.2, 5.2, 0.7, 'mirror'),
        (-1.4, -14.4, -6.4, 2.8, 4.2, 0.6, 'chestcord'),
        (-5, -18.2, -4.2, 10, 2.6, 8.4, 'gorget'),
        (-9.2, -17.0, -6.4, 18.4, 1.4, 12.8, 'mantle'),
        (-8.8, -16.4, -6.5, 17.6, 4.6, 0.5, 'lappet'),
        (-8.8, -16.4, 6.0, 17.6, 4.6, 0.5, 'lappet'),
    ]),
    part('cape', 'body', (0, -16.2, 5.9), (0.12, 0, 0), [(-8.4, 0, -0.4, 16.8, 13, 0.8, 'cape')]),
    part('cape_lower', 'cape', (0, 12.8, 0), (0.06, 0, 0), [(-8.8, 0, -0.4, 17.6, 11, 0.8, 'cape_lower')]),
] + ribbon_arc('body', -18, 8.2, 14, -112, 112, 15, 2.6, 'ribbon_arc') + hang(
    'right_streamer', 'body', (-14 * math.sin(math.radians(112)), -18 + 14 * math.cos(math.radians(112)) + 0.4, 8.2), (0.15, 0, 0.18),
    [(2.6, 7, 0.6, 0), (2.8, 7, 0.6, 0.1), (3.0, 6, 0.6, 0.14), (3.2, 5, 0.6, 0.18)], 'streamer') + [
    # the head: a god's fierce face
    part('head', 'body', (0, -18, -0.4), (0, 0, 0), [
        (-4.6, -10.6, -4.4, 9.2, 9.2, 8.8, 'face'),
        (-4.2, -1.8, -4.1, 8.4, 2.6, 7.8, 'jaw'),
        (-5, -5.8, -3.4, 10, 3, 6.2, 'cheek'),
        (-1.3, -7.0, -5.5, 2.6, 3.4, 1.2, 'nose'),
        (-2.1, -4.4, -5.1, 4.2, 1.2, 0.8, 'nose_wing'),
        (-4.9, -10.8, -0.8, 9.8, 9.6, 5.8, 'hair_back'),
    ]),
    part('right_eye', 'head', (-2.3, -7.0, -4.45), (0, 0.12, 0), [(-1.5, -0.95, -0.6, 3, 1.9, 0.8, 'eye')]),
    part('right_brow', 'head', (-0.4, -8.3, -4.7), (0, 0.1, 0.3), [(-4.6, -1.1, -0.8, 4.6, 1.6, 1.6, 'brow')]),
    part('right_ear', 'head', (-4.7, -6.8, -0.2), (0, 0, 0.08), [(-1.0, -2, -1.2, 1.0, 5.6, 2.4, 'ear')]),
    part('right_moustache', 'head', (-0.5, -3.7, -5.3), (0, 0.25, -0.32), [(-4.2, -0.55, -0.6, 4.2, 1.1, 1.2, 'moustache')]),
    part('right_moustache_tip', 'right_moustache', (-4.0, 0, 0), (0, 0.2, 0.95), [(-2.6, -0.45, -0.5, 2.6, 0.9, 1.0, 'moustache_tip')]),
    part('beard', 'head', (0, -1.0, -4.0), (0, 0, 0), []),
    part('beard_c', 'beard', (0, 0.4, 0), (0.12, 0, 0), [(-1.6, 0, -1, 3.2, 7.5, 2, 'beard_c')]),
    part('beard_tip', 'beard_c', (0, 7.2, 0), (-0.12, 0, 0), [(-1.1, 0, -0.8, 2.2, 3.6, 1.6, 'beard_tip')]),
    part('right_beard', 'beard', (-2.7, -0.6, 0.6), (0.12, 0.2, 0.28), [(-1.2, 0, -0.9, 2.4, 6.8, 1.8, 'beard_side')]),
    # the crown: a gold band and cap, five flame-arched plaques, its knot and flaming jewel, phoenix wings, ties
    part('crown', 'head', (0, -10.4, 0), (0, 0, 0), [
        (-5.1, -1.8, -4.9, 10.2, 2.2, 9.8, 'crown_band'),
        (-4.2, -6, -4.2, 8.4, 4.4, 8.4, 'crown_cap'),
        (-3.3, -10.4, -5.6, 6.6, 9, 0.8, 'crown_front'),
        (-1.6, -9.2, -1.6, 3.2, 3.4, 3.2, 'crown_knot'),
        (-1.1, -12.0, -1.1, 2.2, 2.9, 2.2, 'crown_jewel'),
    ]),
    part('right_crown_petal', 'crown', (-3.9, -1.4, -3.4), (0, 0.72, 0), [(-2.4, -7.6, -0.4, 4.8, 7.6, 0.8, 'crown_petal')]),
    part('right_crown_side', 'crown', (-5.0, -1.4, 0.4), (0, 1.5, 0), [(-2.1, -6.2, -0.4, 4.2, 6.2, 0.8, 'crown_side')]),
    part('right_crown_wing', 'crown', (-4.8, -3.4, 2.0), (0.15, 0.2, -0.5), [(-0.4, -7.4, -0.6, 0.8, 7.4, 5.4, 'crown_wing')]),
] + hang('right_crown_tie', 'crown', (-4.6, -0.6, 3.6), (0.3, 0, 0.3), [(1.4, 5, 0.4, 0), (1.4, 5, 0.4, 0.1), (1.7, 4.4, 0.4, 0.14)], 'tie') + [
    # the arms: silk sleeves, tiered pauldrons each bitten by a lion's head, silk flaring at the elbow, bracers
    part('right_arm', 'body', (-10.2, -14.2, 0.3), ARM, [
        (-2.9, -1.2, -2.9, 5.8, 8.6, 5.8, 'upperarm'),
        (-3.3, 4.6, -3.3, 6.6, 1.4, 6.6, 'armband'),
    ]),
    part('right_pauldron', 'right_arm', (-0.4, -0.9, 0), (0, 0, 0.42), [
        (-4.8, -1.6, -4.3, 7.4, 1.8, 8.6, 'pauldron'),
        (-5.6, 0.0, -4.8, 8, 2, 9.6, 'pauldron2'),
        (-6.4, 1.8, -5.3, 8.6, 2.4, 10.6, 'pauldron3'),
        (-7.4, -2.0, -4.4, 1.0, 8.8, 8.8, 'pauldron_mane'),
        (-8.4, -0.6, -3.2, 1.8, 6.4, 6.4, 'pauldron_lion'),
        (-8.1, 5.6, -2.6, 1.6, 1.4, 5.2, 'lion_jaw'),
    ]),
    part('right_forearm', 'right_arm', (0, 7.6, 0), FOREARM, [
        (-2.5, 0, -2.5, 5, 7, 5, 'forearm'),
        (-2.95, 1.0, -2.95, 5.9, 5.2, 5.9, 'bracer'),
        (-3.9, -1.6, -3.9, 7.8, 2.6, 7.8, 'frill'),
    ]),
    part('right_hand', 'right_forearm', (0, 7, 0), (0, 0, 0), [(-2, 0, -2, 4, 4, 4, 'fist')]),
] + pipa() + sword() + pearl() + umbrella())

KING = KING + serpent()


# ── The skins ────────────────────────────────────────────────────────────────

def bare(p, f, faces=None):
    for s in faces or f:
        for i, j, xy in each(p, f[s]):
            p.clear(xy)


def cut(p, rect, mask):
    """Clears a face where mask(u, v) (0..1 across and down) is false."""
    fx, fy, fw, fh = rect
    for i, j, xy in each(p, rect):
        if not mask((i + 0.5) / fw, (j + 0.5) / fh):
            p.clear(xy)


def arch(u, v, peak=0.0, waist=0.35):
    """A flame-arched plaque's shape: straight sides below, an ogee arch rising to a point above."""
    if v > waist:
        return True
    t = min(1.0, (waist - v) / waist)       # 0 at the arch's foot .. 1 at its tip
    half = 0.5 * math.cos(t * math.pi / 2) ** 0.8
    return abs(u - 0.5) <= half + 0.02


def king_skin(q):
    name, face, plate, silk_c, rib, iris = KINGS[q]
    silver = q == 2
    deep_silk = k(silk_c, 0.55) if sum(silk_c) > 300 else mix(silk_c, (8, 6, 12), 0.5)
    trim_band = rib                                       # the borders' band: his ribbon's colour
    face_dark = kit.skin_shade(face, 0.35)

    def armour(p, f, faces=None, round_=0.5):
        if silver:
            kit.silver(p, f, faces, round_)
        else:
            kit.enamel(p, f, plate, faces, round_)

    def lam(p, rect, light=1.0, ph=5):
        base = (196, 204, 220) if silver else plate
        kit.lamellae(p, rect, base, RED if q != 0 else GOLD, pw=4, ph=ph, light=light, edge=k(base, 0.4))

    def paint_box(p, style, f, dims):
        a = style.split('_')[0]
        if a in ATTRS and ATTRS[a] != q:
            bare(p, f)
            return
        if style in ('coil', 'coil_tail') and q != 2:
            bare(p, f)
            return

        # ── the waist ──
        if style == 'hips':
            kit.silk(p, f, silk_c, deep_silk)
        if style == 'belt':                                # dark leather set with gold plaques, gold-rimmed
            p.shaded(f, (60, 34, 26))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(1, fw - 3, 6):
                    for j in range(2, fh - 2):
                        for w in range(4):
                            p.px((fx + i + w, fy + j), GOLD if w < 3 else GOLD_DEEP)
                    p.px((fx + i + 1, fy + fh // 2), JADE)
            kit.gold_rim(p, f)
        if style == 'buckle_mask':
            kit.gold(p, f)
            kit.lion(p, f['front'], open_=0.38)
        if style == 'buckle_mane':                         # the mane about the mask: gold curls, cut round
            kit.gold(p, f)
            fx, fy, fw, fh = f['front']
            for i, j, xy in each(p, f['front']):
                u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh - 0.5
                r = math.hypot(u, v) * 2
                if r > 1.0:
                    p.clear(xy)
                else:
                    curl = 0.5 + 0.5 * math.sin(math.atan2(v, u) * 9 + r * 10)
                    p.px(xy, noisy(p, mix(GOLD_DEEP, GOLD_LIGHT, curl * 0.85)))
        if style == 'apron':                               # the apron: his silk, a gold border, a cloud medallion, a fringe
            for s, r in f.items():
                p.face(r, silk_c, LIGHT[s], noise=3, fall=0.2)
            fx, fy, fw, fh = f['front']
            kit.frame(p, (fx, fy, fw, fh - 4), GOLD, GOLD_DEEP)
            for j in range(2, fh - 6):
                for i in (2, fw - 3):
                    p.px((fx + i, fy + j), trim_band)
            cx, cy = fx + fw // 2, fy + fh // 2 - 4
            kit.wp.disc(p, cx, cy, 4.2, k(deep_silk, 1.1), rim=GOLD)
            kit.cloud(p, cx - 1, cy, GOLD, r=1)
            for j in range(10, fh - 8, 6):
                kit.cloud(p, cx - 1, fy + j if abs(fy + j - cy) > 6 else fy + 3, GOLD_DEEP, r=1)
            for i in range(fw):                            # the fringe
                for j in range(fh - 4, fh):
                    if i % 2 == 0 or j < fh - 2:
                        p.px((fx + i, fy + j), GOLD if (i // 2) % 2 else trim_band)
                    else:
                        p.clear((fx + i, fy + j))
            fx, fy, fw, fh = f['back']
            kit.frame(p, (fx, fy, fw, fh), GOLD, None)

        if style in ('tasset', 'tasset_side', 'tasset_back'):     # lamellar skirt plates, gold-rimmed, a scalloped foot
            for s, r in f.items():
                if s in ('top', 'bottom'):
                    p.face(r, k(plate, 0.6) if not silver else (150, 156, 170), LIGHT[s], noise=3)
                    continue
                lam(p, r, LIGHT[s])
                fx, fy, fw, fh = r
                for i in range(fw):
                    p.px((fx + i, fy), GOLD_LIGHT)
                    p.px((fx + i, fy + 1), GOLD_DEEP)
                if s in ('front', 'back') and fw > 6:
                    for i in range(fw):                   # the scalloped foot, gold-edged
                        t = (i % 6) / 5
                        dip = int(2.5 * (1 - abs(t - 0.5) * 2))
                        for j in range(fh - 3 + dip, fh):
                            p.clear((fx + i, fy + j))
                        p.px((fx + i, fy + fh - 4 + dip), GOLD)
                    kit.studs(p, r, 3, every=6, x0=2)
                    if style == 'tasset':
                        for j in range(4, fh - 5):        # a gold-bordered band down its middle
                            p.px((fx + fw // 2 - 3, fy + j), GOLD_DEEP)
                            p.px((fx + fw // 2 + 2, fy + j), GOLD_DEEP)
                else:
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 1), GOLD)
        if style == 'sash':
            kit.ribbon(p, f, mix(rib, (255, 255, 255), 0.1), k(rib, 0.55))
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    if (i % 3) == 1:
                        p.clear((fx + i, fy + fh - 1))

        # ── the legs ──
        if style == 'thigh':
            kit.silk(p, f, mix(silk_c, (255, 255, 255), 0.15), deep_silk, fold=5)
        if style == 'robe':                                # the long robe's panel: his silk, folds, a border at its hem
            kit.silk(p, f, silk_c, deep_silk, fold=6)
            for s in ('front', 'back'):
                kit.hem(p, f[s], 5, trim_band)
                fx, fy, fw, fh = f[s]
                kit.scroll_band(p, f[s], fh - 4, GOLD, GOLD_DEEP, period=7)
        if style == 'shin':
            kit.silk(p, f, mix(silk_c, (255, 255, 255), 0.15), deep_silk, fold=5)
        if style == 'greave':                              # an enamelled greave, a gold ridge, a cloud at the knee
            armour(p, f)
            kit.gold_rim(p, f)
            fx, fy, fw, fh = f['front']
            for j in range(2, fh - 1):
                p.px((fx + fw // 2, fy + j), GOLD)
                p.px((fx + fw // 2 - 1, fy + j), GOLD_DEEP)
            kit.cloud(p, fx + fw // 2 - 2, fy + 3, GOLD_LIGHT, r=1)
        if style == 'boot':                                # black boots, gold-rimmed, a white sole
            p.shaded(f, (34, 28, 34))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), GOLD)
                    p.px((fx + i, fy + fh - 1), IVORY)
                    p.px((fx + i, fy + fh - 2), (200, 190, 168))
                kit.scroll_band(p, f[s], 2, GOLD_DEEP, (20, 16, 20), period=6)
        if style == 'boot_toe':
            p.shaded(f, (34, 28, 34))
            kit.gold_rim(p, f, bottom=False)
        if style == 'boot_tip':                            # the upturned toe, gold
            kit.gold(p, f)

        # ── the body ──
        if style == 'torso':
            armour(p, f)
            lam(p, f['back'], LIGHT['back'])
            kit.gold_rim(p, f)
        if style == 'breastplate':                         # moulded to the chest: two pectoral plates outlined in gold
            armour(p, f, round_=0.7)
            kit.gold_rim(p, f, width=2)
            fx, fy, fw, fh = f['front']
            for side in (0, 1):                            # each pectoral's curved lower edge
                x0 = fx + (2 if side == 0 else fw // 2 + 1)
                w_ = fw // 2 - 3
                for i in range(w_):
                    t = (i + 0.5) / w_
                    y = fy + fh - 4 - int(3 * math.sin(t * math.pi))
                    p.px((x0 + i, y), GOLD)
                    p.px((x0 + i, y + 1), GOLD_DEEP)
            for j in range(2, fh - 2):
                p.px((fx + fw // 2, fy + j), GOLD_DEEP)
            for s in ('right', 'left'):
                lam(p, f[s], LIGHT[s], ph=4)
                kit.gold_rim(p, f, sides=(s,), width=2)
            lam(p, f['back'], LIGHT['back'], ph=4)
            kit.gold_rim(p, f, sides=('back',), width=2)
        if style == 'abdomen':                             # gold fish-scales over the belly
            for s, r in f.items():
                if s in ('top', 'bottom'):
                    p.face(r, GOLD_DEEP, LIGHT[s], noise=3)
                    continue
                kit.fish_scales(p, r, GOLD, GOLD_SHADOW, light=LIGHT[s])
            kit.gold_rim(p, f)
        if style == 'belly':                               # the round belly plate, his enamel, gold-rimmed, a cloud
            armour(p, f, round_=0.8)
            fx, fy, fw, fh = f['front']
            kit.frame(p, f['front'], GOLD, GOLD_DEEP)
            kit.scroll_band(p, f['front'], fh // 2 - 1, GOLD, GOLD_DEEP, period=7)
        if style == 'mirror':
            kit.gold(p, f)
            kit.mirror_plate(p, f['front'], glass=(170, 196, 230) if not silver else (210, 220, 240))
            bare(p, f, ('top', 'bottom', 'right', 'left'))
        if style == 'chestcord':                           # the knot binding the mirrors' cords
            for s, r in f.items():
                p.face(r, RED, LIGHT[s], noise=4)
            fx, fy, fw, fh = f['front']
            for j in range(fh):
                p.px((fx + (j % 2) + 1, fy + j), (240, 90, 70))
            kit.wp.disc(p, fx + fw / 2, fy + 2, 1.4, GOLD)
        if style == 'gorget':                              # a gold collar, a key-fret round it
            for s, r in f.items():
                if s in SIDES:
                    kit.fret(p, r, GOLD, GOLD_DEEP)
                else:
                    p.face(r, GOLD_DEEP, LIGHT[s], noise=3)
        if style == 'mantle':                              # the cloud collar's top: his enamel, a gold cloud-scroll, gold edges
            armour(p, f)
            fx, fy, fw, fh = f['top']
            cx, cy = fx + fw / 2, fy + fh / 2
            for i, j, (x, y) in each(p, f['top']):
                a = math.atan2(y - cy, (x - cx) * fh / fw)
                if abs(math.sin(a * 4)) < 0.15:
                    p.px((x, y), GOLD_DEEP)
            kit.frame(p, f['top'], GOLD, None)
            kit.gold_rim(p, f)
        if style == 'lappet':                              # the cloud collar's points over breast and back
            for s, r in f.items():
                armour(p, f, faces=(s,))
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    t = (i % 9) / 8
                    depth = int(fh * (1 - abs(t - 0.5) * 2) * 0.85) + 2
                    for j in range(fh):
                        if j >= depth:
                            p.clear((fx + i, fy + j))
                        elif j >= depth - 2:
                            p.px((fx + i, fy + j), GOLD if j == depth - 1 else GOLD_DEEP)
                    p.px((fx + i, fy), GOLD)
        if style in ('cape', 'cape_lower'):                # the cape: his silk, a ribbon-coloured lining, clouds, a gold hem
            kit.silk(p, f, silk_c, deep_silk, fold=8)
            fx, fy, fw, fh = f['front']                    # the lining, toward his back
            for i, j, xy in each(p, f['front']):
                p.px(xy, noisy(p, k(mix(rib, deep_silk, 0.5), 0.8 - 0.2 * j / fh)))
            kit.clouds(p, f['back'], k(GOLD_DEEP, 1.1) if sum(silk_c) > 300 else GOLD_DEEP, every=10, r=2, share=0.55)
            if style == 'cape':
                kit.hem(p, f['back'], 3, trim_band, at_bottom=False)
            else:
                kit.hem(p, f['back'], 5, trim_band)
                fx, fy, fw, fh = f['back']
                kit.scroll_band(p, f['back'], fh - 4, GOLD, GOLD_DEEP)
        if style in ('ribbon_arc', 'streamer'):            # the heavenly ribbon: silk, gold threads at its edges
            kit.ribbon(p, f, rib, k(rib, 0.5))
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                horizontal = style == 'ribbon_arc'
                n = fw if horizontal else fh
                for t in range(n):
                    a, b = (t, 0) if horizontal else (0, t)
                    a2, b2 = (t, fh - 1) if horizontal else (fw - 1, t)
                    p.px((fx + a, fy + b), GOLD)
                    p.px((fx + a2, fy + b2), GOLD)
                if horizontal:
                    for i in range(1, fw, 5):
                        p.px((fx + i, fy + fh // 2), mix(rib, (255, 255, 255), 0.5))
            if style == 'streamer':
                for s in ('front', 'back'):
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 1), GOLD)

        # ── the head ──
        if style == 'face':
            kit.flesh(p, f, face)
            fx, fy, fw, fh = f['front']                    # 18x18
            for i in range(fw):                            # the hairline under the crown
                p.px((fx + i, fy), HAIR)
                p.px((fx + i, fy + 1), kit.skin_shade(face, 0.6))
            for j in range(3, 8):                          # the furrows of his frown
                p.px((fx + fw // 2 - 1, fy + j), face_dark)
                p.px((fx + fw // 2 + 1, fy + j), kit.skin_shade(face, 0.6))
            for side in (0, 1):                            # the sockets about the bulging eyes
                ex = fx + (2 if side == 0 else fw - 8)
                for i in range(6):
                    p.px((ex + i, fy + 6), face_dark)
                    p.px((ex + i, fy + 11), kit.skin_shade(face, 0.6))
                for j in range(7, 11):
                    p.px((ex - 1 + (0 if side == 0 else 7), fy + j), kit.skin_shade(face, 0.55))
            for j in range(12, fh):                        # the lines down from the nose's wings
                p.px((fx + 4 - (j - 12) // 2, fy + j), face_dark)
                p.px((fx + fw - 5 + (j - 12) // 2, fy + j), face_dark)
            if q == 2 or q == 1:                           # a mark of rank on the brow
                for j in range(2, 5):
                    p.lit((fx + fw // 2, fy + j), (255, 220, 120) if q == 1 else (255, 240, 180), 0.5)
            for s in ('right', 'left'):                    # the hair at his temples, back over the ears
                fx, fy, fw, fh = f[s]
                for i, j, (x, y) in each(p, f[s]):
                    toward_back = (fw - 1 - i) if s == 'right' else i
                    if toward_back < fw * 0.55 or j < 3:
                        p.px((x, y), noisy(p, mix(HAIR, (60, 50, 60), (i + j) % 3 * 0.3)))
            for s in ('back', 'top'):
                for i, j, xy in each(p, f[s]):
                    p.px(xy, noisy(p, mix(HAIR, (60, 50, 60), (i * 2 + j) % 3 * 0.3)))
        if style == 'cheek':                               # the cheekbones, swelling
            kit.flesh(p, f, face, round_=0.8)
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                if i < 4 or i > fw - 5:
                    p.px((fx + i, fy), kit.skin_shade(face, 1.4))
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, (x, y) in each(p, f[s]):
                    toward_back = (fw - 1 - i) if s == 'right' else i
                    if toward_back < fw * 0.4:
                        p.px((x, y), noisy(p, HAIR))
        if style == 'jaw':                                 # the jaw: the mouth set in a snarl, teeth bared
            kit.flesh(p, f, face, round_=0.7)
            fx, fy, fw, fh = f['front']                    # 17x5
            m0, m1 = 3, fw - 3
            for i in range(m0, m1):
                curve = 1 if (i - m0 < 2 or m1 - i <= 2) else 0
                p.px((fx + i, fy + curve), (90, 24, 26))
                p.px((fx + i, fy + curve + 1), IVORY if 1 < i - m0 < m1 - m0 - 2 and (i % 2) else (70, 16, 20))
                p.px((fx + i, fy + curve + 2), (70, 16, 20) if 1 < i - m0 < m1 - m0 - 2 else kit.skin_shade(face, 0.7))
            p.px((fx + m0 + 1, fy + 2), IVORY)
            p.px((fx + m1 - 2, fy + 2), IVORY)
            for i in range(m0 + 1, m1 - 1):
                p.px((fx + i, fy + 3), kit.skin_shade(face, 0.6))
            p.face(f['bottom'], kit.skin_shade(face, 0.45), 1.0, noise=2)
        if style == 'nose':
            kit.flesh(p, f, face, round_=0.4)
            fx, fy, fw, fh = f['front']
            for j in range(fh - 1):
                p.px((fx + 1, fy + j), kit.skin_shade(face, 1.5))
            p.face(f['bottom'], kit.skin_shade(face, 0.5), 1.0, noise=2)
        if style == 'nose_wing':
            kit.flesh(p, f, face, round_=0.3)
            fx, fy, fw, fh = f['front']
            for i in (1, 2, fw - 3, fw - 2):
                p.px((fx + i, fy + fh - 1), (50, 18, 20))
        if style == 'eye':
            kit.eyeball(p, f, iris, glow=0.55, blaze=0.9)
        if style == 'brow':                                # a great flaring brow
            lock(p, f, along_x=True, tone=BLUE_BLACK if q != 0 else BROWN_BLACK)
        if style == 'ear':                                 # a long-lobed ear
            kit.flesh(p, f, face, round_=0.5)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for j in range(2, fh - 4):
                    for i in range(1, fw - 1):
                        if (i + j) % 4 == 0:
                            p.px((fx + i, fy + j), kit.skin_shade(face, 0.55))
                kit.wp.disc(p, fx + fw / 2, fy + fh - 2.5, 1.2, GOLD, rim=GOLD_DEEP)       # a gold ring in the lobe
        if style in ('moustache', 'moustache_tip'):
            lock(p, f, along_x=True, tone=BLUE_BLACK if q != 0 else BROWN_BLACK)
        if style in ('beard_c', 'beard_side', 'beard_tip'):
            lock(p, f, keep=0.6 if style != 'beard_tip' else 0.4, taper=0.3, tone=BLUE_BLACK if q != 3 else BROWN_BLACK, grey=0.04)
        if style == 'hair_back':
            for s, r in f.items():
                kit.wp.strands(p, r, (40, 32, 40), HAIR) if hasattr(kit.wp, 'strands') else p.face(r, HAIR, 1.0)

        # ── the crown ──
        if style == 'crown_band':                          # gold, a row of jewels
            kit.gold(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(2, fw - 1, 5):
                    p.lit((fx + i, fy + fh // 2), (230, 50, 50) if (i // 5) % 2 else (60, 200, 160), 0.35)
                    p.px((fx + i + 1, fy + fh // 2), GOLD_DEEP)
        if style == 'crown_cap':
            armour(p, f)
            kit.gold_rim(p, f)
        if style in ('crown_front', 'crown_petal', 'crown_side'):   # a flame-arched plaque: gold, his enamel within, a jewel
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u, v = (i + 0.5) / fw, (j + 0.5) / fh
                    if not arch(u, v, waist=0.45):
                        p.clear(xy)
                        continue
                    inner = arch(0.5 + (u - 0.5) * 1.45, v - 0.1, waist=0.38) and v < 0.88 and abs(u - 0.5) < 0.36
                    if s == 'back':
                        c = k(GOLD_DEEP, 0.9)
                    elif inner:
                        c = mix(k(plate, 1.1) if not silver else (150, 170, 210), k(plate, 0.6) if not silver else (90, 110, 150), v)
                    else:
                        c = mix(GOLD_LIGHT, GOLD, v) if (i + j) % 5 else GOLD_DEEP
                    p.px(xy, noisy(p, c, 3))
                if s == 'front':                           # the jewel, and a gold flame curling up over it
                    cx, cy = fx + fw / 2, fy + fh * 0.62
                    kit.wp.disc(p, cx, cy, max(1.2, fw * 0.16), (230, 40, 40) if q != 1 else (60, 210, 170), rim=GOLD_LIGHT)
                    for j in range(int(fh * 0.25), int(fh * 0.5)):
                        p.px((int(cx + 0.8 * math.sin(j * 0.9)), fy + j), GOLD_LIGHT)
            for s in ('right', 'left', 'top', 'bottom'):
                p.face(f[s], GOLD_DEEP, 1.0, noise=3)
            fx, fy, fw, fh = f['right']
            for j in range(int(fh * 0.4)):
                for i in range(fw):
                    p.clear((fx + i, fy + j))
            fx, fy, fw, fh = f['left']
            for j in range(int(fh * 0.4)):
                for i in range(fw):
                    p.clear((fx + i, fy + j))
            fx, fy, fw, fh = f['top']
            for i, j, xy in each(p, f['top']):
                p.clear(xy)
        if style == 'crown_knot':
            kit.gold(p, f, round_=0.8)
            for s in SIDES:
                kit.scroll_band(p, f[s], 2, GOLD_LIGHT, GOLD_DEEP, period=4)
        if style == 'crown_jewel':                         # a flaming pearl, shining
            for s, r in f.items():
                for i, j, xy in each(p, r):
                    u, v = (i + 0.5) / r[2], (j + 0.5) / r[3]
                    d = math.hypot(u - 0.5, v - 0.55) * 2
                    c = mix(FIRE_WHITE, FIRE if q != 1 else (80, 200, 255), clamp(d))
                    p.lit(xy, c, 0.75, blaze=1.0)
            for s in SIDES:
                point(p, f[s], keep=0.5, down=False, sharp=0.5)
        if style == 'crown_wing':                          # a phoenix wing: gold feathers sweeping up and back
            for s, r in f.items():
                p.face(r, GOLD_DEEP, 1.0, noise=3)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u = (i + 0.5) / fw                     # 0 front .. 1 back on 'right' (reversed on 'left')
                    if s == 'right':
                        u = 1 - u
                    v = (j + 0.5) / fh
                    reach = 0.25 + 0.75 * (1 - v) ** 0.7   # the wing broadens toward its top, its back edge feathered
                    feather = 0.08 * math.sin(v * 22)
                    if u > reach + feather:
                        p.clear(xy)
                        continue
                    stripe = int(v * 7 + u * 3) % 2
                    c = mix(GOLD_LIGHT, GOLD, u) if stripe else mix(GOLD, GOLD_DEEP, u)
                    if u > reach + feather - 0.12:
                        c = k(plate, 1.1) if not silver else (170, 190, 230)
                    p.px(xy, noisy(p, c, 3))
        if style == 'tie':
            kit.ribbon(p, f, rib, k(rib, 0.5))

        # ── the arms ──
        if style == 'upperarm':
            kit.silk(p, f, silk_c, deep_silk, fold=5)
        if style == 'armband':
            kit.gold(p, f)
            for s in SIDES:
                kit.studs(p, f[s], 1, every=4)
        if style in ('pauldron', 'pauldron2', 'pauldron3'):   # the shoulder's tiers: his enamel, a gold lip, studs
            armour(p, f, round_=0.6)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), GOLD)
                    p.px((fx + i, fy + fh - 2), GOLD_DEEP)
                    if i % 4 == 1 and fh > 3:
                        p.px((fx + i, fy + 1), GOLD_LIGHT)
            kit.frame(p, f['top'], GOLD, None)
        if style == 'pauldron_mane':                       # the lion's mane: gold curls, cut round
            kit.gold(p, f)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh - 0.45
                    r = math.hypot(u, v) * 2
                    if r > 1.0:
                        p.clear(xy)
                    else:
                        curl = 0.5 + 0.5 * math.sin(math.atan2(v, u) * 10 + r * 11)
                        p.px(xy, noisy(p, mix(GOLD_SHADOW, GOLD_LIGHT, curl * 0.9)))
        if style == 'pauldron_lion':
            kit.gold(p, f)
            kit.lion(p, f['right'], open_=0.0, mane=False)
            kit.lion(p, f['left'], open_=0.0, mane=False)
        if style == 'lion_jaw':                            # the lower jaw, its fangs biting the arm
            kit.gold(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), IVORY if i % 2 == 0 else (60, 14, 12))
        if style == 'forearm':
            kit.silk(p, f, silk_c, deep_silk, fold=5)
        if style == 'bracer':                              # gold bracers, his enamel panel, a cloud
            kit.gold(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(2, fh - 2):
                    for i in range(2, fw - 2):
                        p.px((fx + i, fy + j), noisy(p, k(plate if not silver else (190, 200, 220), 1.0 - 0.3 * j / fh)))
                kit.cloud(p, fx + fw // 2 - 2, fy + fh // 2, GOLD_LIGHT, r=1)
        if style == 'frill':                               # the silk flaring at the elbow, its edge scalloped, lined
            kit.silk(p, f, silk_c, deep_silk, fold=3)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), trim_band)
                    if i % 3 == 0:
                        p.clear((fx + i, fy + fh - 1))
            p.face(f['bottom'], k(trim_band, 0.6), 1.0, noise=3)
        if style == 'fist':
            kit.flesh(p, f, face)
            for s in ('front', 'right', 'left', 'top'):
                fx, fy, fw, fh = f[s]
                for i in range(1, fw, 2):
                    p.px((fx + i, fy + fh - 2), kit.skin_shade(face, 0.6))

        # ── the pipa ──
        if style in ('pipa_bowl', 'pipa_belly', 'pipa_shoulder', 'pipa_foot'):
            WOOD, WOOD_D = (150, 64, 34), (80, 30, 18)
            for s, r in f.items():                         # the back: lacquered red-brown wood, staved
                fx, fy, fw, fh = r
                for i, j, xy in each(p, r):
                    c = mix(WOOD, WOOD_D, abs((i + 0.5) / fw - 0.4))
                    if s in ('back',) and i % 4 == 0:
                        c = k(c, 0.75)
                    p.px(xy, noisy(p, k(c, LIGHT[s])))
            fx, fy, fw, fh = f['front']                    # the soundboard: pale wutong wood, its strings
            for i, j, xy in each(p, f['front']):
                p.px(xy, noisy(p, mix((222, 188, 130), (180, 140, 90), j / fh), 4))
            for s in ('front',):
                for i in range(4):
                    x = fx + int(fw / 2 - 2 + i * 1.3)
                    for j in range(fh):
                        p.px((x, fy + j), (250, 246, 236))
            if style == 'pipa_bowl':                       # rounded below, gold rosette
                for s in ('front', 'back'):
                    fx, fy, fw, fh = f[s]
                    for i, j, xy in each(p, f[s]):
                        u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh
                        if v > 0.55 and math.hypot(u * 2, (v - 0.55) / 0.45) > 1.02:
                            p.clear(xy)
                fx, fy, fw, fh = f['front']
                for s_ in (-1, 1):
                    kit.wp.disc(p, fx + fw / 2 + s_ * fw * 0.3, fy + 3, 1.3, GOLD, rim=GOLD_DEEP)
            if style == 'pipa_foot':
                bare(p, f, ('right', 'left', 'front', 'back'))
            if style == 'pipa_shoulder':
                for s in ('front', 'back'):
                    fx, fy, fw, fh = f[s]
                    for i, j, xy in each(p, f[s]):
                        if abs((i + 0.5) / fw - 0.5) * 2 > 0.55 + 0.45 * (j + 0.5) / fh:
                            p.clear(xy)
        if style == 'pipa_neck':
            for s, r in f.items():
                p.face(r, (60, 26, 16), LIGHT[s], noise=3)
            fx, fy, fw, fh = f['front']
            for i, j, xy in each(p, f['front']):
                p.px(xy, noisy(p, (40, 20, 14)))
            for i in range(4):
                x = fx + int(i * fw / 4 + 0.5)
                for j in range(fh):
                    p.px((x, fy + j), (236, 232, 220))
        if style == 'pipa_fret':
            p.shaded(f, IVORY)
        if style == 'pipa_bridge':
            p.shaded(f, (50, 24, 14))
            kit.gold_rim(p, f, bottom=False)
        if style == 'pipa_head':                           # the bent-back pegbox, carved, a gold cap
            p.shaded(f, (70, 30, 18))
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                p.px((fx + i, fy), GOLD)
                p.px((fx + i, fy + 1), GOLD_DEEP)
            kit.cloud(p, fx + 1, fy + 3, GOLD_DEEP, r=1)
        if style == 'pipa_peg':
            p.shaded(f, IVORY)
        if style == 'pipa_tassel':
            for s, r in f.items():
                kit.wp.strands(p, r, RED, RED_DEEP) if dims[0] > 1 else p.face(r, RED, LIGHT[s])
            if dims[0] > 1:
                for s in SIDES:
                    kit.wp.tufts(p, f[s])

        # ── the sword ──
        if style == 'sword_blade':                         # steel, a ridge down it, a gold inlay at its root, its point
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(p, r):
                    u = (i + 0.5) / fw
                    c = mix((250, 252, 255), (150, 160, 178), abs(u - 0.5) * 2) if s in ('front', 'back') else (230, 236, 246)
                    if s in ('front', 'back') and abs(u - 0.5) < 0.12:
                        c = (120, 130, 150)
                    p.px(xy, noisy(p, c, 2))
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for j in range(fh - 9, fh - 2):            # seven stars inlaid in gold near its root
                    if j % 2 == 0:
                        p.lit((fx + fw // 2 - 1, fy + j), GOLD_LIGHT, 0.3, blaze=0.9)
                for j in range(fh):
                    p.flare((fx, fy + j), (200, 230, 255), 0.8)
                    p.flare((fx + fw - 1, fy + j), (200, 230, 255), 0.8)
                point(p, f[s], keep=0.0, down=False, sharp=0.35)
        if style in ('sword_guard', 'sword_root', 'sword_pommel'):
            kit.gold(p, f)
            if style == 'sword_guard':
                kit.lion(p, f['front'], open_=0.3, mane=False, glow=0.3)
                kit.lion(p, f['back'], open_=0.3, mane=False, glow=0.3)
        if style == 'sword_grip':
            p.shaded(f, RED_DEEP)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(0, fh, 2):
                    for i in range(fw):
                        p.px((fx + i, fy + j), (60, 10, 12))
        if style == 'sword_tassel':
            for s, r in f.items():
                p.face(r, RED, LIGHT[s], noise=6)
            if dims[0] > 1:
                for s in SIDES:
                    kit.wp.tufts(p, f[s])

        # ── the pearl ──
        if style == 'pearl':                               # the wish-granting pearl: shining, a swirl in it
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(p, r):
                    u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh - 0.5
                    d = math.hypot(u, v) * 2
                    sw = 0.5 + 0.5 * math.sin(math.atan2(v, u) * 2 + d * 6)
                    c = mix((255, 255, 255), (150, 210, 255), clamp(d * 0.8 + sw * 0.3))
                    p.lit(xy, c, 0.85, blaze=1.0, hot=(255, 250, 220))
        if style == 'pearl_cup':                           # a gold lotus cup
            kit.gold(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    if i % 3 == 0:
                        p.px((fx + i, fy), GOLD_LIGHT)
                        p.px((fx + i, fy + 1), GOLD_DEEP)
        if style in ('pearl_flame', 'pearl_flame_x'):     # flames licking round the pearl
            flat = ('front', 'back') if style == 'pearl_flame' else ('right', 'left')
            for s in flat:
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh
                    ring = abs(u) * 2
                    tongue = 0.62 + 0.25 * math.sin(u * 14) - (1 - v) * 0.5
                    if ring < 0.5 and 0.35 < v < 0.95:
                        p.clear(xy)
                        continue
                    if ring > tongue + 0.35 or v < 0.08:
                        p.clear(xy)
                        continue
                    c = mix(FIRE_HOT, FIRE, clamp(1 - v + ring * 0.3))
                    p.lit(xy, c, 0.8, blaze=1.0)
            bare(p, f, [s for s in f if s not in flat])

        # ── the serpent ──
        if style in ('coil', 'coil_tail', 'serpent_neck'):    # green scales, gold belly plates down one side
            SC, SC_D = (54, 150, 90), (20, 70, 44)
            for s, r in f.items():
                if s in ('top', 'bottom'):
                    p.face(r, SC_D, 1.0, noise=3)
                    continue
                if s == 'bottom' or s == 'right':
                    kit.wp.plates(p, r, (226, 196, 104), (140, 110, 50), rows=4)
                else:
                    kit.fish_scales(p, r, SC, SC_D, light=LIGHT[s], cw=4, ch=3)
            if style in ('coil', 'serpent_neck'):
                fx, fy, fw, fh = f['left']                  # a gold ridge of fins along its back
                for j in range(0, fh, 3):
                    p.px((fx + fw // 2, fy + j), GOLD)
            if style == 'coil_tail':
                for s in ('front', 'back', 'right', 'left'):
                    pass
        if style in ('serpent_skull', 'serpent_snout', 'serpent_jaw', 'serpent_brow'):
            SC, SC_D = (60, 160, 96), (22, 76, 48)
            for s, r in f.items():
                if s == 'bottom' and style != 'serpent_brow':
                    kit.wp.plates(p, r, (226, 196, 104), (140, 110, 50), rows=3)
                else:
                    kit.fish_scales(p, r, SC, SC_D, light=LIGHT[s], cw=3, ch=2)
            if style == 'serpent_skull':                    # its eyes, glowing gold
                for s in ('right', 'left'):
                    fx, fy, fw, fh = f[s]
                    ex = fx + (2 if s == 'right' else fw - 4)
                    for a in range(2):
                        for b in range(2):
                            p.lit((ex + a, fy + 1 + b), (255, 220, 60), 0.9, blaze=1.0, hot=(255, 255, 160))
                    p.px((ex + 1, fy + 1), (20, 10, 6))
            if style == 'serpent_snout':
                fx, fy, fw, fh = f['front']
                p.px((fx + 1, fy + 1), (10, 10, 10))
                p.px((fx + fw - 2, fy + 1), (10, 10, 10))
                for s in ('right', 'left', 'bottom'):       # the mouth's line, fangs
                    fx, fy, fw, fh = f[s]
                    if s != 'bottom':
                        for i in range(fw):
                            p.px((fx + i, fy + fh - 1), IVORY if i % 2 else (70, 20, 20))
            if style == 'serpent_jaw':
                fx, fy, fw, fh = f['top']                   # its mouth within, red; fangs at its edge
                for i, j, xy in each(p, f['top']):
                    p.px(xy, (150, 30, 40) if 0 < i < fw - 1 else IVORY)
            if style == 'serpent_brow':
                kit.gold(p, f)
        if style == 'serpent_horn':
            kit.gold(p, f)
            for s in SIDES:
                point(p, f[s], keep=0.2, down=True, sharp=0.5) if False else None
        if style == 'serpent_whisker':
            p.shaded(f, GOLD)
        if style == 'serpent_tongue':
            p.shaded(f, (200, 40, 60))

        # ── the umbrella ──
        if style == 'umb_shaft':
            p.shaded(f, (150, 26, 26))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(0, fh, 12):
                    for jj in range(2):
                        for i in range(fw):
                            p.px((fx + i, fy + j + jj), GOLD if jj == 0 else GOLD_DEEP)
        if style in ('umb_hub', 'umb_top', 'umb_butt'):
            kit.gold(p, f, round_=0.8)
        if style == 'umb_jewel':
            for s, r in f.items():
                for i, j, xy in each(p, r):
                    p.lit(xy, mix(FIRE_WHITE, (255, 140, 60), (j + 0.5) / r[3]), 0.7, blaze=1.0)
            for s in SIDES:
                point(p, f[s], keep=0.5, down=False, sharp=0.5)
        if style in ('umb_panel', 'umb_cap'):               # the canopy: rich silk, a jewelled gold border, a fringe at its foot
            outer = (226, 60, 50) if style == 'umb_panel' else GOLD
            for s, r in f.items():
                fx, fy, fw, fh = r
                if s in ('front',):                        # its outside
                    for i, j, xy in each(p, r):
                        v = (j + 0.5) / fh
                        stripe = int(v * 6) % 3
                        c = [outer, (60, 80, 170), (50, 140, 100)][stripe] if style == 'umb_panel' else GOLD
                        c = k(c, 1.05 - 0.25 * abs((i + 0.5) / fw - 0.5) * 2)
                        p.px(xy, noisy(p, c, 3))
                    if style == 'umb_panel':
                        for j in range(fh):
                            if int((j + 0.5) / fh * 6) != int((j + 1.5) / fh * 6):
                                for i in range(fw):
                                    p.px((fx + i, fy + j), GOLD)
                        for j in range(2, fh - 2, 5):
                            kit.wp.disc(p, fx + fw / 2, fy + j + 1, 1.0, (255, 250, 230))
                        kit.wp.vline(p, r, 0, GOLD) if hasattr(kit.wp, 'vline') else None
                        kit.wp.vline(p, r, fw - 1, GOLD) if hasattr(kit.wp, 'vline') else None
                elif s == 'back':                          # its lining
                    p.face(r, (240, 220, 160), 0.8, noise=4)
                else:
                    p.face(r, GOLD_DEEP, 1.0, noise=3)
            if style == 'umb_panel':
                for s in ('front', 'back'):                 # the fringe at its foot
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        for j in range(fh - 3, fh):
                            if i % 2:
                                p.px((fx + i, fy + j), GOLD_LIGHT if j < fh - 1 else GOLD)
                            else:
                                p.clear((fx + i, fy + j))
                    for i, j, xy in each(p, f[s]):          # it tapers to the top (each a gore of the canopy)
                        u, v = (i + 0.5) / fw, (j + 0.5) / fh
                        if abs(u - 0.5) * 2 > 0.25 + 0.75 * v ** 0.6:
                            p.clear(xy)
        if style == 'umb_cord':
            p.shaded(f, RED)
        if style == 'umb_bead':
            for s, r in f.items():
                for i, j, xy in each(p, r):
                    p.lit(xy, mix((120, 230, 200), (40, 140, 110), (j + 0.5) / r[3]), 0.35, blaze=0.8)
        if style == 'umb_tuft':
            for s, r in f.items():
                p.face(r, RED, LIGHT[s], noise=8)
            for s in SIDES:
                kit.wp.tufts(p, f[s])

    return paint_box


def build_kings():
    slots = None
    for q in range(4):
        p = kit.GreatPainter(100 + q)
        slots = paint(KING, p, king_skin(q))
        p.save('heaven_king_' + KINGS[q][0])
    emit(KING, slots, kit.JAVA, 'kings')
    report('the Four Kings', KING, SIZE, slots)


if __name__ == '__main__':
    which = sys.argv[1:] or ['kings', 'li', 'nezha', 'erlang']
    if 'kings' in which:
        build_kings()
    if 'li' in which:
        import heaven_great_li
        heaven_great_li.build()
    if 'nezha' in which:
        import heaven_great_nezha
        heaven_great_nezha.build()
    if 'erlang' in which:
        import heaven_great_erlang
        heaven_great_erlang.build()
