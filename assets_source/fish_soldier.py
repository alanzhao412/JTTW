"""The Fish Soldier (鱼兵), a carp of the Dragon King's host. As shrimp_soldier.py: its model and skin
come from one list of parts (beastgen): the model is written into FishSoldierRenderer's layer()
between its GENERATED markers, the skin painted to the same box-UV layout at twice the model's
resolution (its fins half clear; the renderer draws it translucent), with a second skin for the
glint of its eyes drawn full-bright (fish_soldier_glow.png).

A carp stood up as a man: a real fish's head, blunt and deep, its mouth a thick-lipped gaping ring
with two pairs of long barbels (a carp's, the dragon's whiskers it will grow) hanging from its
corners; round glassy eyes, gold-ringed, standing out from the sides of its head; the gill plates
behind them, each ringed with growth lines and flaring open over the red gills. Its body is clad
in individual red-gold scales, darker down its back and pale gold at the belly, the lateral line
dotted down each side; a dorsal fin rises over its back like a crest, spined and half clear; a
deep forked tail fin swings behind it, its pectoral fins fan back from its elbows, small fins at
its hips and calves; webbed hands and feet. The army's kit, the shrimp's: a teal-lacquered bowl
helmet rimmed in gold with a red plume and little gold fin-wings, teal spaulders three plates
deep, a cuirass of teal lacquered scales edged in gold with a round gold breast mirror, a sea-blue
sash across it, a red belt with a gold buckle, lamellar tassets over its thighs and a red apron
between them, teal greaves and bracers; and a sky-piercer halberd (方天画戟): a steel spear point
between two crescent blades, a red horsehair tassel and the blue wave pennant.

Run from the repository root: python assets_source/fish_soldier.py"""
import math

import beastgen as bg
from beastgen import emit
from fire_court import part, mirrored
from sea_court import aim
from king_yama import paint, point, ragged, circle, mixc, k, noisy
import shrimp_soldier as shrimp
from shrimp_soldier import ShellPainter, LIGHTS, SIDES, lacquer, gold_box, rim, TEAL, TEAL_DEEP, TEAL_LIGHT, GOLD, GOLD_DEEP, RED, RED_DEEP, SEA

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/FishSoldierRenderer.java'
OUT = 'src/main/resources/assets/jttw_engine/textures/entity/'

# How much larger than its pixels it is drawn: FishSoldierRenderer.SIZE
SIZE = 0.9

DORSAL = (178, 44, 20)          # its back: deep red
FLANK = (238, 120, 34)          # its sides: red-gold
GILT = (252, 186, 64)           # the gold of its scales
BELLY = (252, 228, 168)         # its belly: pale gold
SHEEN = (255, 244, 200)
OUTLINE = (104, 26, 12)
FIN = (246, 112, 46)
FIN_EDGE = (206, 40, 26)
FIN_RAY = (170, 46, 20)
GILL = (214, 34, 46)
MAW = (60, 10, 14)
LIP = (242, 140, 90)
LIP_DEEP = (184, 68, 42)
WEB = (250, 170, 110)
CLAW = (60, 40, 36)

BODY_LEAN = 0.08
ARM, FOREARM, HAND = (-0.5, 0, 0.16), (-0.85, 0.25, 0), (0, 0, 0)
LARM, LFOREARM = (-0.2, 0, -0.22), (-0.55, -0.15, 0)


def head():
    return [
        # the head: deep and blunt, a carp's: the crown behind, the face, the snout tapering to the lips,
        # the brow sloping from the snout up to the crown; no neck between it and the shoulders
        part('head', 'body', (0, -12.2, -0.2), (0, 0, 0), [
            (-4, -8.6, -5, 8, 9, 8, 'skull'),
            (-3.6, -7.2, -8, 7.2, 7.6, 3.2, 'face'),
            (-2.9, -5.8, -10, 5.8, 5.4, 2.2, 'snout'),
            (-3.4, -0.8, -7.6, 6.8, 1.4, 8.4, 'throat'),
        ]),
        # the mouth: a thick-lipped ring pushed out from the snout, gaping, a dark maw inside it
        part('mouth', 'head', (0, -3.0, -9.8), (0.1, 0, 0), [
            (-2.4, -2.4, -2.0, 4.8, 1.2, 2.2, 'lip'),
            (-1.4, -1.3, -0.5, 2.8, 2.6, 0.6, 'maw'),
        ]),
        part('right_lip', 'mouth', (-1.4, -1.3, -0.2), (0, 0.15, 0), [(-1.0, 0, -1.8, 1.0, 2.6, 2.0, 'lipside')]),
        part('jaw', 'head', (0, -1.2, -7.8), (0.36, 0, 0), [
            (-2.6, 0, -2.4, 5.2, 1.3, 3.4, 'jaw'),
            (-2.3, -0.9, -3.6, 4.6, 1.3, 1.6, 'lip'),
        ]),
        # two pairs of barbels: long ones from the mouth's corners, short ones from the upper lip
        part('right_barbel', 'mouth', (-2.3, -0.3, -1.6), (0.3, 0.2, 0.45), [(-0.3, 0, -0.3, 0.6, 3.8, 0.6, 'barbel')]),
        part('right_barbel_2', 'right_barbel', (0, 3.7, 0), (-0.5, 0, -0.35), [(-0.25, 0, -0.25, 0.5, 3.6, 0.5, 'barbel')]),
        part('right_barbel_3', 'right_barbel_2', (0, 3.5, 0), (-0.5, 0, 0.4), [(-0.2, 0, -0.2, 0.4, 3.0, 0.4, 'barbel_tip')]),
        part('right_whisker', 'mouth', (-1.3, -2.3, -1.8), (0.9, 0.2, 0.35), [(-0.2, 0, -0.2, 0.4, 2.8, 0.4, 'barbel_tip')]),
        # round glassy eyes, standing out from the sides of the face: a gold ring, the eye, its dark dome
        part('right_eye', 'head', (-3.6, -4.6, -6.2), (0, 0.2, 0), [
            (-0.6, -2.5, -2.5, 0.8, 5, 5, 'eyering'),
            (-1.2, -2, -2, 0.7, 4, 4, 'eyeball'),
            (-1.7, -1.2, -1.2, 0.6, 2.4, 2.4, 'eyedome'),
        ]),
        # the gill plates: the preopercle, and the great opercle behind it, flaring off the red gills
        part('right_gills', 'head', (-3.8, -6.6, -3.6), (0, 0, 0), [(-0.4, 0, 0, 0.5, 7.0, 4.2, 'gillred')]),
        part('right_preop', 'head', (-4.0, -7.0, -4.8), (0, -0.08, 0), [(-0.4, 0, 0, 0.4, 7.4, 1.6, 'preop')]),
        part('right_gill', 'head', (-4.0, -7.8, -3.6), (0, -0.36, 0.03), [(-0.5, 0, 0, 0.5, 8.2, 5.6, 'operculum')]),
        # the soldier's helmet: the shrimp's teal bowl, gold-rimmed, a red plume, with little gold fin-wings
        part('helmet', 'head', (0, -8.5, -0.8), (0.12, 0, 0), [
            (-4.3, -0.8, -4.3, 8.6, 1.0, 8.6, 'helmbrim'),
            (-3.4, -3.8, -3.4, 6.8, 3.2, 6.8, 'helmbowl'),
            (-0.7, -5.0, -0.7, 1.4, 1.3, 1.4, 'helmknob'),
            (-1.7, -8.2, -1.7, 3.4, 3.4, 3.4, 'plume'),
            (-3.8, 0, 3.4, 7.6, 2.4, 1.0, 'helmguard'),
        ]),
        part('plume_tail', 'helmet', (0, -7, 1.2), (1.0, 0, 0), [(-1.4, 0, -0.8, 2.8, 6, 1.6, 'plumetail')]),
        part('right_helmwing', 'helmet', (-3.3, -2.0, 0.6), (0, 0.35, 0.55), [(-0.2, -4.6, 0, 0.4, 4.6, 3.2, 'helmwing')]),
    ]


def body():
    return [
        # the body: scaled, its cuirass over the chest, a belt, a gold mirror on its breast
        part('body', 'root', (0, 12.2, 0.4), (BODY_LEAN, 0, 0), [
            (-4.2, -12, -2.8, 8.4, 12, 6, 'torso'),
            (-4.7, -12.4, -3.4, 9.4, 7.6, 7.0, 'cuirass'),
            (-1.6, -10.8, -3.9, 3.2, 3.2, 0.5, 'mirror'),
            (-4.9, -5.2, -3.6, 9.8, 1.8, 7.4, 'belt'),
            (-1.4, -5.6, -4.2, 2.8, 2.6, 0.8, 'buckle'),
        ]),
        part('sash', 'body', (0, -5.6, -4.0), (0, 0, 0.62), [(-1.1, -6.8, -0.3, 2.2, 13.0, 0.6, 'sash')]),
        part('apron', 'body', (0, -3.4, -3.7), (-0.06, 0, 0), [(-1.5, 0, -0.2, 3, 8.4, 0.4, 'apron')]),
        part('right_tasset', 'body', (-2.6, -3.5, -3.3), (-0.16, 0, 0.12), [(-2.4, 0, -0.4, 4.6, 7.0, 0.8, 'tasset')]),
        part('right_hip_tasset', 'body', (-4.6, -3.5, 0.2), (0, 0, 0.18), [(-0.8, 0, -2.6, 0.8, 6.4, 5.2, 'sidetasset')]),
        part('rear_tasset', 'body', (0, -3.5, 3.4), (0.14, 0, 0), [(-3.6, 0, -0.4, 7.2, 6.2, 0.8, 'tasset')]),
        # the dorsal fin, rising over its back like a crest from between the shoulders down to its tail
        part('dorsal', 'body', (0, -16.2, 2.6), (0.16, 0, 0), [(-0.25, 0, 0, 0.5, 15, 5.4, 'dorsal')]),
        # small fins at its hips
        part('right_pelvic', 'body', (-4.0, -0.4, 1.6), (0.7, 0, 0.35), [(-0.2, 0, 0, 0.4, 4.2, 3.2, 'smallfin')]),
    ]


def tail():
    return [
        # the tail: two scaled segments sweeping down behind it to the deep forked caudal fin
        part('tail_1', 'body', (0, -2.4, 2.4), (-0.9, 0, 0), [(-2.5, -2.4, 0, 5, 4.8, 5, 'tailseg')]),
        part('tail_2', 'tail_1', (0, 0, 4.8), (-0.3, 0, 0), [(-1.8, -1.8, 0, 3.6, 3.6, 4.4, 'tailseg2')]),
        part('caudal', 'tail_2', (0, 0, 4.0), (0.3, 0, 0), [(-0.25, -6.4, 0, 0.5, 12.8, 8.4, 'caudal')]),
    ]


def legs():
    return [
        part('right_leg', 'root', (-2.3, 12.4, 0.6), (-0.18, 0, 0.05), [(-1.7, -0.6, -1.7, 3.4, 6.6, 3.4, 'thigh')]),
        part('right_shin', 'right_leg', (0, 5.8, 0), (0.3, 0, -0.04), [
            (-1.35, 0, -1.35, 2.7, 6.0, 2.7, 'shin'),
            (-1.6, 0.4, -1.7, 3.2, 4.4, 2.4, 'greave')]),
        part('right_calffin', 'right_shin', (0, 0.6, 1.2), (-0.25, 0, 0), [(-0.2, 0, 0, 0.4, 4.4, 2.4, 'smallfin')]),
        part('right_foot', 'right_shin', (0, 5.9, 0), (-0.12, 0, 0), [
            (-1.6, 0, -3.4, 3.2, 1.8, 4.8, 'foot'),
            (-1.7, 0.6, -4.8, 1.0, 1.2, 1.6, 'toe'),
            (-0.5, 0.6, -5.1, 1.0, 1.2, 1.8, 'toe'),
            (0.7, 0.6, -4.8, 1.0, 1.2, 1.6, 'toe')]),
    ]


def arms():
    hand_held = aim([(BODY_LEAN, 0, 0), ARM, FOREARM, HAND], (0.06, 0, 0))
    return [
        part('right_shoulder', 'body', (-4.6, -11.6, 0.2), (0, 0, 0.4), [
            (-3.6, -0.8, -2.9, 4.0, 1.4, 5.8, 'spauldron'),
            (-4.0, 0.4, -3.1, 4.2, 1.6, 6.2, 'spauldron2'),
            (-4.2, 1.8, -3.2, 3.6, 1.5, 6.4, 'spauldron3')]),
        part('right_arm', 'body', (-4.9, -10.8, 0.2), ARM, [(-1.4, -0.4, -1.4, 2.8, 6.2, 2.8, 'upperarm')]),
        part('right_forearm', 'right_arm', (0, 5.6, 0), FOREARM, [
            (-1.2, 0, -1.2, 2.4, 5.6, 2.4, 'forearm'),
            (-1.5, 1.4, -1.5, 3.0, 3.4, 3.0, 'bracer')]),
        # the pectoral fin, fanning back from the elbow
        part('right_pectoral', 'right_forearm', (-0.9, 0.2, 0.9), (0.35, 0.25, 0.2), [(-0.2, 0, 0, 0.4, 5.4, 5.4, 'pectoral')]),
        part('right_hand', 'right_forearm', (0, 5.5, 0), HAND, [
            (-1.3, 0, -1.3, 2.6, 2.2, 2.6, 'palm'),
            (-1.3, 2.0, -1.3, 0.8, 2.2, 1.0, 'finger'),
            (-0.4, 2.0, -1.3, 0.8, 2.4, 1.0, 'finger'),
            (0.5, 2.0, -1.3, 0.8, 2.2, 1.0, 'finger')]),
        # the halberd: a steel spear point between two crescent blades, a red tassel, the wave pennant
        part('halberd', 'right_hand', (0, 1.3, -0.4), hand_held, [
            (-0.45, -30, -0.45, 0.9, 40, 0.9, 'shaft'),
            (-0.8, -37.4, -0.25, 1.6, 7.6, 0.5, 'blade'),
            (-0.75, -30.8, -0.75, 1.5, 1.6, 1.5, 'collar'),
            (-0.9, -29.4, -0.9, 1.8, 1.4, 1.8, 'collar'),
            (-1.3, -28.0, -1.3, 2.6, 4.6, 2.6, 'tassel'),
            (-0.6, 9.6, -0.6, 1.2, 1, 1.2, 'collar'),
            (-0.35, 10.4, -0.35, 0.7, 2, 0.7, 'butt'),
        ]),
        part('right_crescent', 'halberd', (-0.4, -30.6, 0), (0, 0, 0), [(-4.4, -4.2, -0.2, 4.4, 8.4, 0.4, 'crescent')]),
        part('pennant', 'halberd', (0.45, -24.0, 0), (0, 0, 0), [(0, 0, -0.1, 5, 6.4, 0.2, 'pennant')]),
    ]


def left_arm():
    """The left arm, free: its open webbed hand a little forward (not mirrored: it holds nothing)."""
    return [
        part('left_arm', 'body', (4.9, -10.8, 0.2), LARM, [(-1.4, -0.4, -1.4, 2.8, 6.2, 2.8, 'upperarm')], True),
        part('left_forearm', 'left_arm', (0, 5.6, 0), LFOREARM, [
            (-1.2, 0, -1.2, 2.4, 5.6, 2.4, 'forearm'),
            (-1.5, 1.4, -1.5, 3.0, 3.4, 3.0, 'bracer')], True),
        part('left_pectoral', 'left_forearm', (0.9, 0.2, 0.9), (0.35, -0.25, -0.2), [(-0.2, 0, 0, 0.4, 5.4, 5.4, 'pectoral')], True),
        part('left_hand', 'left_forearm', (0, 5.5, 0), (0.2, 0, 0), [
            (-1.3, 0, -1.3, 2.6, 2.2, 2.6, 'palm'),
            (-1.1, 2.0, -1.3, 0.8, 2.2, 1.0, 'finger'),
            (-0.2, 2.0, -1.3, 0.8, 2.4, 1.0, 'finger'),
            (0.7, 2.0, -1.3, 0.8, 2.2, 1.0, 'finger')], True),
    ]


def mirror_box(b):
    x, y, z, w, h, d, s = b
    return (-x - w, y, z, w, h, d, s)


FISH = mirrored(body() + head() + tail() + legs() + arms())
# the left arm is its own (posed apart from the right): replace the mirrored copies
FISH = [q for q in FISH if q[0] not in ('left_arm', 'left_forearm', 'left_pectoral', 'left_hand')]
FISH += [(n, pa, pv, r, [mirror_box(b) for b in bx], m) for (n, pa, pv, r, bx, m) in left_arm()]


def ordered(parts):
    out, seen = [], {'root'}
    pending = list(parts)
    while pending:
        rest = []
        for q in pending:
            if q[1] in seen:
                out.append(q)
                seen.add(q[0])
            else:
                rest.append(q)
        assert len(rest) < len(pending), [q[0] for q in rest]
        pending = rest
    return out


FISH = ordered(FISH)


# ── Painting ─────────────────────────────────────────────────────────────────

def scale_base(s, i, j, fw, fh, belly_side):
    """The scales' colour across a face: deep red down the back, red-gold flanks, pale gold belly."""
    if s in ('back', 'top'):
        t = 0.0
    elif s in ('front', 'bottom'):
        t = 1.0
    else:
        u = (i + 0.5) / fw                           # right: its back at the left; left: its front at the left
        t = u if s == 'right' else 1 - u
    t = t if belly_side else 0.15 + t * 0.45
    if t < 0.5:
        return mixc(DORSAL, FLANK, t * 2)
    return mixc(FLANK, BELLY, (t - 0.5) * 2)


def scale_at(i, j, cw, ch):
    """Where (i, j) falls on a sheet of overlapping scales, each a lobe hanging over the one below:
    (how far down its scale it is, 0..1; how far off its middle, -1..1)."""
    bulge = ch * 0.8
    r = j // ch
    for rr in (r - 1, r):
        off = (rr % 2) * cw / 2
        xc = math.floor((i + 0.5 - off) / cw) * cw + off + cw / 2
        dx = (i + 0.5 - xc) / (cw / 2)
        depth = ch + bulge * max(0, 1 - dx * dx)
        yy = j + 0.5 - rr * ch
        if 0 <= yy <= depth:
            return yy / depth, dx
    return 0.5, 0


def fish_scales(p, f, cw=6, ch=5, belly_side=True, lateral=True, sides=None, base_fn=None):
    """Individual overlapping scales: each lit gold across its crown, a glint at its middle, outlined
    dark at its rim; the lateral line a dotted row down each side."""
    for s, (fx, fy, fw, fh) in f.items():
        if sides and s not in sides:
            continue
        for j in range(fh):
            for i in range(fw):
                v, dx = scale_at(i, j, cw, ch)
                base = base_fn(s, i, j, fw, fh) if base_fn else scale_base(s, i, j, fw, fh, belly_side)
                col = mixc(mixc(base, GILT, 0.4), base, min(1, v * 1.2))       # gold across its crown
                if v < 0.14:
                    col = k(col, 0.8)                                          # in the shadow of the one above
                elif v < 0.45 and abs(dx) < 0.4:
                    col = mixc(col, SHEEN, 0.3)                                # the glint at its middle
                if v > 0.84:
                    col = mixc(col, OUTLINE, 0.7)                              # its rim
                p.px((fx + i, fy + j), noisy(p, k(col, LIGHTS[s]), 1))
        if lateral and s in ('right', 'left'):                                 # the lateral line: a dark dot on each scale in a row
            x = fx + int(fw * (0.4 if s == 'right' else 0.6))
            for j in range(2, fh - 1, ch):
                p.px((x, fy + j), (60, 16, 8))
                p.px((x, fy + j + 1), (96, 30, 14))


def line(p, a, b, colour, alpha=255, rect=None):
    (x0, y0), (x1, y1) = a, b
    n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    for t in range(n + 1):
        x, y = x0 + (x1 - x0) * t / n, y0 + (y1 - y0) * t / n
        if rect and not (rect[0] <= x < rect[0] + rect[2] and rect[1] <= y < rect[1] + rect[3]):
            continue
        p.pxa((x, y), colour, alpha)


def fin(p, rect, root='left', alpha=190, rays=7, fork=0.0, scallop=0.12, spine=False, fan=False, round_tip=0.0, taper=0.0):
    """A fin on a face: a half-clear membrane, dark rays from its root to its edge, its edge deep red
    and scalloped between the rays. `root` names the face's side it grows from; fan rays spread from
    the root's middle, else they run side by side, raked back."""
    fx, fy, fw, fh = rect
    L = fw if root in ('left', 'right') else fh        # along the rays
    W = fh if root in ('left', 'right') else fw        # across

    def to_xy(along, across):
        if root == 'left':
            return fx + along * (fw - 1), fy + across * (fh - 1)
        if root == 'right':
            return fx + (1 - along) * (fw - 1), fy + across * (fh - 1)
        if root == 'top':
            return fx + across * (fw - 1), fy + along * (fh - 1)
        return fx + across * (fw - 1), fy + (1 - along) * (fh - 1)

    def edge_at(across):
        e = 1.0
        if fork:
            e -= fork * max(0, 1 - abs(across - 0.5) * 2.4) ** 1.2
        if round_tip:
            e -= round_tip * (abs(across - 0.5) * 2) ** 2
        if taper:                                       # deepest at its leading end (across 0), shallowing away
            e -= taper * across ** 0.8
        return e
    for j in range(fh):
        for i in range(fw):
            if root == 'left':
                along, across = i / max(1, fw - 1), j / max(1, fh - 1)
            elif root == 'right':
                along, across = 1 - i / max(1, fw - 1), j / max(1, fh - 1)
            elif root == 'top':
                along, across = j / max(1, fh - 1), i / max(1, fw - 1)
            else:
                along, across = 1 - j / max(1, fh - 1), i / max(1, fw - 1)
            phase = across * rays
            e = edge_at(across) - scallop * (0.5 - 0.5 * math.cos(phase * 2 * math.pi))
            if along > e:
                p.clear((fx + i, fy + j))
                continue
            col = mixc((255, 206, 140), FIN, min(1, 0.3 + along))
            a = alpha - 50 * along
            if along > e - 0.1:
                col, a = FIN_EDGE, 225
            p.pxa((fx + i, fy + j), noisy(p, col, 3), a)
    for r in range(rays + 1):                           # the rays
        across = (r + 0.5) / (rays + 1) if not fan else r / rays
        e = edge_at(across) - 0.04
        start = (0, 0.5 + (across - 0.5) * 0.35) if fan else (0, across)
        a = to_xy(*start)
        b = to_xy(e, across)
        line(p, a, b, FIN_RAY, 240, rect)
    if spine:                                           # the stout leading spine, serrated
        for i in range(L):
            x, y = to_xy(i / max(1, L - 1), 0)
            p.pxa((x, y), (130, 34, 16), 255)
            x, y = to_xy(i / max(1, L - 1), 1 / max(1, W - 1))
            p.pxa((x, y), (170, 50, 24) if i % 2 else (230, 140, 80), 255)


def clear_face(p, rect):
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            p.clear((fx + i, fy + j))


def armour_scales(p, rect, light, cw=8, ch=5):
    """Fish-scale armour (鱼鳞甲): teal lacquered scales, each gold-rimmed, lit across its crown."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            v, dx = scale_at(i, j, cw, ch)
            col = mixc(mixc(TEAL, (170, 236, 222), 0.35), TEAL_DEEP, min(1, v * 1.1))
            if v < 0.14:
                col = k(TEAL_DEEP, 0.7)
            elif v < 0.4 and abs(dx) < 0.3:
                col = mixc(col, (210, 250, 240), 0.2)
            if v > 0.78:
                col = GOLD if v < 0.9 else GOLD_DEEP
            p.px((fx + i, fy + j), noisy(p, k(col, light), 1))


KIT = ('spauldron', 'spauldron2', 'helmbrim', 'helmbowl', 'helmknob', 'plume', 'plumetail', 'tassel', 'sash', 'belt',
       'buckle', 'shaft', 'collar', 'butt', 'blade', 'pennant', 'bracer')


def head_colour(t, fwd=0.5):
    """The head's colour at height t (0 its crown .. 1 its chin): deep red over the crown, red-gold
    cheeks, pale gold under the chin."""
    c = mixc(DORSAL, FLANK, min(1, t * 2.4))
    c = mixc(c, GILT, max(0, min(1, (t - 0.35) * 2.5)) * 0.5)
    return mixc(c, BELLY, max(0, t - 0.68) * 3)


def head_skin(p, f, sheen_band=(0.22, 0.36)):
    """The smooth head: deep red over the crown, red-gold down the cheeks with a band of sheen, pale beneath."""
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                t = j / max(1, fh - 1)
                if s == 'top':
                    c = mixc(DORSAL, FLANK, abs(i + 0.5 - fw / 2) / (fw / 2) * 0.45)
                    if abs(i + 0.5 - fw / 2) < 1:
                        c = mixc(c, SHEEN, 0.2)
                elif s == 'bottom':
                    c = mixc(BELLY, LIP, 0.15)
                else:
                    c = head_colour(t)
                    if sheen_band[0] < t < sheen_band[1]:
                        c = mixc(c, SHEEN, 0.28)
                if p.rnd.random() < 0.04:
                    c = mixc(c, GILT, 0.5)
                p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 2))


def paint_fish(p, style, f, dims):
    if style in KIT:
        shrimp.paint_shrimp(p, style, f, dims)
        return
    if style == 'spauldron3':
        shrimp.paint_shrimp(p, 'spauldron', f, dims)
    if style in ('torso', 'tailseg', 'tailseg2'):
        fish_scales(p, f, cw=8, ch=5)
    if style in ('thigh', 'upperarm'):
        fish_scales(p, f, cw=6, ch=4, belly_side=False, lateral=False)
    if style in ('shin', 'forearm'):
        fish_scales(p, f, cw=6, ch=4, belly_side=False, lateral=False)
    if style == 'throat':                                         # under the chin: pale gold, the throat's folds
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    c = mixc(BELLY, LIP, 0.12 if (j // 2) % 2 else 0.3)
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 2))
    if style in ('skull', 'face'):
        head_skin(p, f)
        if style == 'skull':                                      # the scales begin at the nape
            fish_scales(p, f, cw=5, ch=4, lateral=False, sides=('back',))
    if style == 'brow':                                           # the brow, sloping up from the snout: deep red, shining
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    c = mixc(DORSAL, FLANK, abs(i + 0.5 - fw / 2) / (fw / 2) * 0.6)
                    if s == 'top' and abs(i + 0.5 - fw / 2) < 1.2:
                        c = mixc(c, SHEEN, 0.3)
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 2))
    if style == 'snout':                                          # the snout: nostrils, two pits each side
        head_skin(p, f, sheen_band=(0.1, 0.25))
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            x = fx + (fw - 3 if s == 'right' else 2)
            for y in (fy + 2, fy + 3):
                p.px((x, y), MAW)
            p.px((x + (-2 if s == 'right' else 2), fy + 3), MAW)
        fx, fy, fw, fh = f['front']
        for j in range(fh):
            for i in range(fw):
                p.px((fx + i, fy + j), noisy(p, head_colour(0.15 + 0.7 * j / fh), 2))
    if style in ('lip', 'lipside'):                               # the thick fleshy lips: coral gold, creased, the throat dark inside
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    t = j / max(1, fh - 1)
                    c = mixc(LIP, LIP_DEEP, abs(t - 0.35) * 1.3)
                    if style == 'lip' and s in ('front', 'back') and i % 3 == 0:
                        c = k(c, 0.88)
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 2))
        if style == 'lip':
            fx, fy, fw, fh = f['bottom']
            for j in range(fh):
                for i in range(2, fw - 2):
                    p.px((fx + i, fy + j), MAW)
        else:
            for s in ('left', 'right'):                           # its inner face
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    for i in range(fw):
                        if s == 'left':
                            p.px((fx + i, fy + j), MAW)
    if style == 'maw':                                            # deep in its mouth: dark red, the gill rakers pale
        for s, (fx, fy, fw, fh) in f.items():
            p.face((fx, fy, fw, fh), MAW, 1.0, noise=2)
        fx, fy, fw, fh = f['front']
        for i in range(0, fw, 2):
            p.px((fx + i, fy + fh - 1), (150, 70, 70))
            p.px((fx + i, fy), (120, 50, 50))
        for j in range(1, fh - 1):
            p.px((fx + fw // 2, fy + j), (14, 2, 4))
            p.px((fx + fw // 2 - 1, fy + j), (14, 2, 4))
    if style == 'jaw':                                            # the lower jaw: pale gold beneath, the mouth's dark above
        head_skin(p, f)
        for s in ('bottom', 'front'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), noisy(p, mixc(BELLY, LIP, 0.3), 2))
        fx, fy, fw, fh = f['top']
        for j in range(fh):
            for i in range(fw):
                p.px((fx + i, fy + j), noisy(p, (110, 26, 30) if 1 < i < fw - 2 else LIP_DEEP, 2))
    if style in ('barbel', 'barbel_tip'):                         # fleshy barbels, coral, ringed, pale at the tip
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    t = j / max(1, fh - 1)
                    c = mixc(LIP_DEEP, LIP, t if style == 'barbel' else 0.6 + 0.4 * t)
                    if j % 3 == 0:
                        c = k(c, 0.82)
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 2))
    if style == 'eyering':                                        # the socket: a gold ring of skin round the eye
        head_skin(p, f)
        for s in ('right', 'front', 'back', 'top', 'bottom'):
            fx, fy, fw, fh = f[s]
            if s == 'right':
                cx, cy, r = fx + fw / 2, fy + fh / 2, fw / 2
                for j in range(fh):
                    for i in range(fw):
                        d = math.hypot(fx + i + 0.5 - cx, fy + j + 0.5 - cy)
                        if d <= r + 0.2:
                            c = mixc((255, 222, 120), GOLD_DEEP, (d / r) ** 2)
                            p.px((fx + i, fy + j), noisy(p, c, 2))
            else:
                p.face((fx, fy, fw, fh), GILT, LIGHTS[s], noise=3)
    if style == 'eyeball':                                        # the eye: a gold iris round a great black pupil
        for s, (fx, fy, fw, fh) in f.items():
            if s not in ('right', 'front'):
                p.face((fx, fy, fw, fh), (220, 168, 60), LIGHTS[s], noise=3)
                continue
            if s == 'front':                                      # seen from before it, the eye's bulge: gold, the pupil's edge
                p.face((fx, fy, fw, fh), (232, 184, 56), 1.0, noise=3)
                for j in range(1, fh - 1):
                    p.px((fx, fy + j), (12, 10, 14))
                continue
            cx, cy, r = fx + fw / 2, fy + fh / 2, fw / 2
            for j in range(fh):
                for i in range(fw):
                    d = math.hypot(fx + i + 0.5 - cx, fy + j + 0.5 - cy)
                    c = (232, 184, 56) if d > r * 0.72 else (12, 10, 14)
                    if d > r * 0.95:
                        c = (120, 70, 20)
                    p.px((fx + i, fy + j), c)
    if style == 'eyedome':                                        # the glassy dome over the pupil: black, a bright glint
        for s, (fx, fy, fw, fh) in f.items():
            p.face((fx, fy, fw, fh), (14, 12, 18), LIGHTS[s], noise=2)
            if s == 'front':
                p.px((fx, fy), (230, 240, 255), glow=True)
            if s == 'right':
                p.px((fx + 1, fy + 1), (255, 255, 255), glow=True)
                p.px((fx + 2, fy + 1), (210, 230, 255), glow=True)
                p.px((fx + 1, fy + 2), (180, 200, 240), glow=True)
                p.px((fx + fw - 2, fy + fh - 2), (90, 110, 150))
    if style == 'gillred':                                        # the gills: red filaments, combed
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    c = GILL if (i + (j // 3)) % 2 else k(GILL, 0.62)
                    p.px((fx + i, fy + j), noisy(p, c, 6))
    if style in ('operculum', 'preop'):                           # a gill plate: gold, ringed with growth lines; the opercle's rear edge rounded, red-rimmed
        head_skin(p, f)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            hx = fx + (fw - 1 if s == 'right' else 0)             # its hinge: the front, a third of the way down
            hy = fy + fh * 0.3
            for j in range(fh):
                for i in range(fw):
                    back = (fw - 1 - i) if s == 'right' else i     # 0 at its front .. fw-1 at its rear
                    bx, by = back / max(1, fw - 1), (j + 0.5) / fh
                    # the rear edge: a curve, so the plate is a rounded lobe
                    if style == 'operculum' and (bx ** 2 / 1.0 + ((by - 0.42) / 0.62) ** 2) > 1.0:
                        p.clear((fx + i, fy + j))
                        continue
                    d = math.hypot((fx + i - hx) * 1.2, fy + j - hy)
                    t = by
                    c = mixc(head_colour(0.35 + 0.45 * t), GILT, 0.35)
                    if int(d) % 4 == 0 and d > 2:
                        c = k(c, 0.74)
                    elif int(d) % 4 == 1:
                        c = mixc(c, SHEEN, 0.2)
                    if style == 'operculum' and (bx ** 2 + ((by - 0.42) / 0.62) ** 2) > 0.8:
                        c = FIN_EDGE
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 2))
        if style == 'operculum':
            for s in ('top', 'bottom', 'back', 'front'):
                clear_face(p, f[s])
    if style in ('dorsal', 'crest', 'smallfin', 'pectoral', 'caudal', 'calffin'):
        for s, r in f.items():
            if s in ('right', 'left'):
                front = 'right' if s == 'right' else 'left'        # the side its root is on: its front, toward -z
                if style == 'dorsal':                             # rooted along the spine (the face's top .. bottom), rays raked back
                    fin(p, r, root=front, rays=13, scallop=0.12, spine=True, taper=0.7)
                elif style == 'caudal':                           # the tail fin: rooted at the peduncle, fanned and deeply forked
                    fin(p, r, root=front, rays=16, fork=0.5, fan=True, scallop=0.04)
                elif style == 'pectoral':
                    fin(p, r, root=front, rays=8, fan=True, round_tip=0.35, scallop=0.08)
                else:
                    fin(p, r, root='top', rays=5, round_tip=0.4, scallop=0.1)
            else:
                clear_face(p, r)
    if style == 'foot':                                           # a webbed foot: scaled above, pale below
        fish_scales(p, f, cw=4, ch=3, belly_side=False, lateral=False)
        p.face(f['bottom'], BELLY, 0.7, noise=3)
    if style == 'toe':                                            # a toe, a dark claw at its end
        for s, (fx, fy, fw, fh) in f.items():
            p.face((fx, fy, fw, fh), mixc(FLANK, WEB, 0.4), LIGHTS[s], noise=3)
        p.face(f['front'], CLAW, 1.0, noise=3)
        for s in ('top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), CLAW)
                p.px((fx + i, fy + fh - 2), CLAW)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in ((fw - 1, fw - 2) if s == 'right' else (0, 1)):
                    p.px((fx + i, fy + j), CLAW)
    if style in ('palm', 'finger'):                               # webbed hands: scaled backs, pale palms, dark claws
        for s, (fx, fy, fw, fh) in f.items():
            c = BELLY if s in ('back', 'bottom') else mixc(FLANK, GILT, 0.3)
            p.face((fx, fy, fw, fh), c, LIGHTS[s], noise=3)
        if style == 'palm':
            fish_scales(p, f, cw=4, ch=3, belly_side=False, lateral=False, sides=('front', 'right', 'left', 'top'))
        if style == 'finger':
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), CLAW)
                    p.px((fx + i, fy + fh - 2), CLAW)
                    if i % 2 == 0:
                        p.px((fx + i, fy + fh // 2 - 1), k(FLANK, 0.75))
    # ── the soldier's kit ──
    if style == 'cuirass':                                        # teal lacquered fish-scale armour, gold-edged
        for s, r in f.items():
            if s == 'bottom':
                lacquer(p, {s: r}, TEAL, TEAL_DEEP)
                continue
            armour_scales(p, r, LIGHTS[s])
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), GOLD)
                p.px((fx + i, fy + 1), GOLD)
                p.px((fx + i, fy + 2), GOLD_DEEP)
                p.px((fx + i, fy + fh - 1), GOLD_DEEP)
                p.px((fx + i, fy + fh - 2), GOLD)
        fx, fy, fw, fh = f['top']                                 # the collar's opening: the scales of its neck show
        for j in range(fh):
            for i in range(fw):
                if 3 <= i < fw - 3 and 3 <= j < fh - 3:
                    p.px((fx + i, fy + j), noisy(p, mixc(DORSAL, FLANK, 0.5), 3))
                elif i in (2, fw - 3) or j in (2, fh - 3):
                    p.px((fx + i, fy + j), GOLD)
    if style == 'mirror':                                         # the round breast mirror: polished gold, a wave across its middle
        gold_box(p, f)
        fx, fy, fw, fh = f['front']
        cx, cy, r = fx + fw / 2, fy + fh / 2, fw / 2
        for j in range(fh):
            for i in range(fw):
                d = math.hypot(fx + i + 0.5 - cx, fy + j + 0.5 - cy)
                if d > r + 0.2:
                    p.clear((fx + i, fy + j))
                elif d > r - 1:
                    p.px((fx + i, fy + j), GOLD_DEEP)
                else:
                    t = (j + i * 0.5) / (fh + fw * 0.5)
                    p.px((fx + i, fy + j), mixc((255, 250, 214), GOLD, t))
        for i in range(1, fw - 1):
            p.px((fx + i, fy + fh // 2 + int(round(0.9 * math.sin(i * 1.4)))), (60, 150, 140))
    if style in ('tasset', 'sidetasset'):                         # fish-scale tassets over the thighs, gold-edged
        for s, r in f.items():
            armour_scales(p, r, LIGHTS[s], cw=6, ch=4)
        for s in SIDES:
            rim(p, f[s], GOLD)
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                p.px((fx, fy + j), GOLD_DEEP)
                p.px((fx + fw - 1, fy + j), GOLD_DEEP)
            ragged(p, f[s], teeth=6, depth=2)
    if style == 'apron':                                          # a red apron between them, a gold wave at its hem
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    c = k(RED, 1.05 - 0.25 * j / fh + (0.08 if (i // 2) % 2 else 0))
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 2))
            if s in ('front', 'back'):
                for i in range(fw):
                    p.px((fx + i, fy + fh - 4 - int(round(1.0 * (1 + math.sin(i * 1.3))))), GOLD)
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
                for j in range(fh):
                    p.px((fx, fy + j), GOLD_DEEP)
                    p.px((fx + fw - 1, fy + j), GOLD_DEEP)
                circle(p, fx + fw / 2, fy + fh * 0.3, 0, 1.8, GOLD)
                circle(p, fx + fw / 2, fy + fh * 0.3, 0, 0.8, RED_DEEP)
    if style == 'greave':                                         # teal greaves, gold-banded, a gold boss
        lacquer(p, f, TEAL, TEAL_DEEP)
        for s in SIDES:
            rim(p, f[s], GOLD, top=True)
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh // 2), GOLD_DEEP)
            if s == 'front':
                circle(p, fx + fw / 2, fy + fh * 0.28, 0, 1.4, GOLD)
    if style == 'helmguard':                                      # the helmet's neck guard: fish-scale lamellae, gold-edged
        for s, r in f.items():
            armour_scales(p, r, LIGHTS[s], cw=5, ch=3)
        for s in SIDES:
            rim(p, f[s], GOLD)
    if style == 'helmwing':                                       # the helmet's little gold fin-wings, ribbed
        for s, (fx, fy, fw, fh) in f.items():
            if s not in ('right', 'left'):
                clear_face(p, (fx, fy, fw, fh))
                continue
            for j in range(fh):
                for i in range(fw):
                    t = 1 - j / max(1, fh - 1)                     # 0 at its root (the box's bottom) .. 1 at its tip
                    c = mixc(GOLD, (255, 236, 160), t * 0.6)
                    if i % 3 == 1:
                        c = GOLD_DEEP
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 2))
            point(p, (fx, fy, fw, fh), keep=0.5, down=False, sharp=0.45)
    if style == 'crescent':                                       # a crescent blade: steel, its hollow facing out, gilt where it meets the shaft
        for s, (fx, fy, fw, fh) in f.items():
            if s not in ('front', 'back'):
                clear_face(p, (fx, fy, fw, fh))
                continue
            ox = fx + (fw if s == 'front' else 0)                 # the shaft's side
            for j in range(fh):
                for i in range(fw):
                    x, y = fx + i + 0.5, fy + j + 0.5
                    far = abs(x - ox) / fw                        # 0 at the shaft .. 1 at its outer edge
                    dy = (y - (fy + fh / 2)) / (fh / 2)
                    outer = dy * dy + far * far
                    inner = ((far - 1.2) / 0.72) ** 2 + (dy / 0.62) ** 2
                    if outer > 1.0 or inner < 1.0 or (far < 0.16 and abs(dy) > 0.4):
                        p.clear((fx + i, fy + j))
                    elif far < 0.28:
                        p.px((fx + i, fy + j), GOLD)
                    elif outer > 0.78 or inner < 1.3:
                        p.px((fx + i, fy + j), (246, 250, 255))
                    else:
                        p.px((fx + i, fy + j), noisy(p, (170, 180, 196), 3))


if __name__ == '__main__':
    p = ShellPainter(23)
    slots = paint(FISH, p, paint_fish)
    p.img.save(OUT + 'fish_soldier.png')
    p.glow.save(OUT + 'fish_soldier_glow.png')
    import os
    if os.path.exists(JAVA):
        emit(FISH, slots, JAVA)
    print('fish soldier', len(FISH), 'parts,', len(slots), 'boxes')
    from lion_camel import extent
    print('   drawn at', SIZE, {kk: round(v, 2) for kk, v in extent(FISH, SIZE).items()})
    low = [q for q in FISH if q[0] not in ('halberd', 'pennant', 'right_crescent', 'left_crescent')]
    print('   without the halberd', {kk: round(v, 2) for kk, v in extent(low, SIZE).items()})
