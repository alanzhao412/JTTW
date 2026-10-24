"""The Mystical Waters' creatures: the Shrimp Soldier, the Crab General, the Yaksha and the four
Dragon Kings. Each one's model and skin come from one list of parts (beastgen): the model is written
into its renderer's layer() between its GENERATED markers, and the skin painted to the same box-UV
layout at twice the model's resolution, with a second, glowing skin for eyes and pearls.

The four Dragon Kings share one model, drawn finer (a 256-pixel skin): each has his own skin, his
robe in his sea's colour (East blue-green, South red, West white, North black) and his scales to
match, and shows only his own crown, beard and regalia (the parts named for his sea; the renderer
hides the others).

Run from the repository root: python assets_source/sea_court.py"""
import math

import beastgen as bg
from beastgen import Painter, paint, emit

bg.UNITS = 128
bg.TEX = bg.UNITS * bg.S

R = 'src/main/java/com/jttw/engine/client/renderer/'
DARK = (18, 16, 20)
GOLD = (222, 176, 64)
GOLD_DEEP = (168, 120, 40)
IVORY = (238, 230, 206)
PEARL = (236, 240, 255)


def part(name, parent, pivot, rot, boxes, mirror=False):
    return (name, parent, pivot, rot, boxes, mirror)


def mirrored(parts):
    """Each 'right_' part again as 'left_', mirrored across x."""
    out = []
    for name, parent, (px, py, pz), (rx, ry, rz), boxes, mirror in parts:
        out.append((name, parent, (px, py, pz), (rx, ry, rz), boxes, mirror))
        if name.startswith('right_'):
            lb = [(-x - w, y, z, w, h, d, st) for (x, y, z, w, h, d, st) in boxes]
            out.append((name.replace('right_', 'left_'), parent.replace('right_', 'left_'), (-px, py, pz), (rx, -ry, -rz), lb, True))
    return out


def bands(p, rect, base, every=3, k=0.72, horizontal=True):
    """Dark seams across a face every few pixels: a shell's segments."""
    fx, fy, fw, fh = rect
    if horizontal:
        for j in range(every - 1, fh, every):
            for i in range(fw):
                p.px((fx + i, fy + j), [v * k for v in base])
    else:
        for i in range(every - 1, fw, every):
            for j in range(fh):
                p.px((fx + i, fy + j), [v * k for v in base])


def all_faces(p, f, base, noise=5):
    p.shaded(f, base)


# ── Shrimp Soldier ───────────────────────────────────────────────────────────
SHELL = (214, 92, 58)
SHELL_PALE = (240, 168, 120)
BELLY = (246, 206, 168)
SHAFT = (110, 74, 44)
STEEL = (200, 206, 214)

SHRIMP = [
    part('body', 'root', (0, 0, 0), (0, 0, 0), [
        (-4, 0, -2.5, 8, 12, 5, 'shell'),
        (-3.5, 1, -3, 7, 10, 0.6, 'belly'),
    ]),
    part('head', 'root', (0, 0, 0), (0, 0, 0), [
        (-3.5, -7, -5, 7, 7, 8, 'carapace'),
        (-0.5, -5.5, -11, 1, 1.5, 6, 'rostrum'),
        (-2, -1, -5.5, 4, 1.5, 2, 'mandible'),
    ]),
    part('right_eye', 'head', (-2.6, -6.5, -4.5), (0, 0.3, 0), [(-1.8, -0.9, -0.9, 1.8, 1.8, 1.8, 'eye')]),
    part('left_eye', 'head', (2.6, -6.5, -4.5), (0, -0.3, 0), [(0, -0.9, -0.9, 1.8, 1.8, 1.8, 'eye')], True),
    part('right_antenna', 'head', (-1.5, -6.5, -5), (0.3, 0.35, 0), [(-0.3, -0.3, 0, 0.6, 0.6, 18, 'antenna')]),
    part('left_antenna', 'head', (1.5, -6.5, -5), (0.3, -0.35, 0), [(-0.3, -0.3, 0, 0.6, 0.6, 18, 'antenna')], True),
    part('right_arm', 'root', (-5, 2, 0), (0, 0, 0), [(-2, -2, -1.5, 3, 12, 3, 'limb')]),
    part('spear', 'right_arm', (-0.5, 9, 0), (1.5708, 0, 0), [
        (-0.5, -18, -0.5, 1, 30, 1, 'shaft'),
        (-1, -23, -0.5, 2, 5, 1, 'blade'),
        (-1.5, -18.5, -1, 3, 1, 2, 'gold'),
    ]),
    part('left_arm', 'root', (5, 2, 0), (0, 0, 0), [(-1, -2, -1.5, 3, 12, 3, 'limb')], True),
    part('right_leg', 'root', (-2, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 12, 4, 'limb')]),
    part('left_leg', 'root', (2, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 12, 4, 'limb')], True),
    part('tail', 'root', (0, 9, 2.5), (0.7, 0, 0), [
        (-3, -2, 0, 6, 4, 4, 'shell'),
        (-2.5, -1.5, 3.5, 5, 3, 4, 'shell'),
        (-2, -1, 7, 4, 2, 3, 'shell'),
    ]),
    part('tail_fan', 'tail', (0, 0, 9.5), (0.4, 0, 0), [(-4, -0.5, 0, 8, 1, 4, 'fan')]),
]


def paint_shrimp(p, style, f, dims):
    if style in ('shell', 'carapace', 'limb'):
        p.shaded(f, SHELL, belly=SHELL_PALE)
        for k in ('right', 'left', 'front', 'back', 'top'):
            bands(p, f[k], SHELL, every=3 if style != 'limb' else 4, horizontal=style != 'carapace' or k in ('front', 'back'))
    if style == 'carapace':
        fx, fy, fw, fh = f['top']
        for j in range(fh):                      # a pale ridge along the top
            p.px((fx + fw // 2, fy + j), SHELL_PALE)
            p.px((fx + fw // 2 - 1, fy + j), SHELL_PALE)
    if style == 'belly':
        for r in f.values():
            p.face(r, BELLY, 1.0, noise=4)
        bands(p, f['front'], BELLY, every=3, k=0.8)
    if style == 'rostrum':
        for r in f.values():
            p.gradient(r, SHELL, SHELL_PALE, horizontal=True)
    if style == 'mandible':
        p.shaded(f, (150, 60, 40))
    if style == 'eye':
        for r in f.values():
            p.face(r, (12, 10, 14), 1.0, noise=2)
        fx, fy, fw, fh = f['front']
        p.px((fx + 1, fy + 1), (200, 200, 210))      # a glint
    if style == 'antenna':
        for r in f.values():
            p.face(r, SHELL_PALE, 1.0, noise=10)
    if style == 'fan':
        for k, r in f.items():
            p.gradient(r, SHELL, SHELL_PALE, horizontal=k not in ('top', 'bottom'))
        bands(p, f['top'], SHELL, every=4, k=0.75, horizontal=False)
    if style == 'shaft':
        p.shaded(f, SHAFT)
    if style == 'blade':
        p.shaded(f, STEEL)
    if style == 'gold':
        p.shaded(f, GOLD)


# ── Crab General ─────────────────────────────────────────────────────────────
CRAB = (178, 54, 40)
CRAB_DARK = (120, 30, 26)
CRAB_UNDER = (232, 196, 160)
PLUME = (196, 30, 36)
LEGS = []
for side, sx in ((-1, 'right'), (1, 'left')):
    for i, z in enumerate((-3.5, 1.5, 6.5)):
        n = '%s_leg%d' % (sx, i)
        if side < 0:
            LEGS.append(part(n, 'root', (-11, 15, z), (0, 0, 0.4), [(-8, -1, -1, 8, 2, 2, 'leg')]))
            LEGS.append(part(n + '_low', 'root', (-18.4, 12, z), (0, 0, 0.15), [(-1, 0, -1, 2, 12, 2, 'legtip')]))
        else:
            LEGS.append(part(n, 'root', (11, 15, z), (0, 0, -0.4), [(0, -1, -1, 8, 2, 2, 'leg')], True))
            LEGS.append(part(n + '_low', 'root', (18.4, 12, z), (0, 0, -0.15), [(-1, 0, -1, 2, 12, 2, 'legtip')], True))

CRAB_PARTS = [
    part('body', 'root', (0, 14, 0), (0, 0, 0), [
        (-12, -6, -9, 24, 7, 18, 'carapace'),
        (-9, 1, -7, 18, 3, 14, 'under'),
        (-3, -3, -9.6, 6, 3, 1, 'mouth'),
        (-4, -10, -5, 8, 4, 8, 'helm'),
        (-5, -7, -6, 10, 1, 10, 'helmgold'),
        (-0.5, -15, -2, 1, 5, 6, 'plume'),
    ]),
    part('right_eye', 'body', (-3.5, -6, -8), (0, 0, 0), [(-0.75, -4, -0.75, 1.5, 4, 1.5, 'stalk'), (-1.25, -6, -1.25, 2.5, 2, 2.5, 'eye')]),
    part('left_eye', 'body', (3.5, -6, -8), (0, 0, 0), [(-0.75, -4, -0.75, 1.5, 4, 1.5, 'stalk'), (-1.25, -6, -1.25, 2.5, 2, 2.5, 'eye')], True),
    part('right_claw', 'root', (-9, 15, -8), (0, 0.5, 0), [(-1.5, -1.5, -8, 3, 3, 8, 'arm')]),
    part('right_pincer', 'right_claw', (0, 0, -8), (0, -0.7, 0), [(-3.5, -3, -9, 7, 6, 9, 'claw')]),
    part('right_finger', 'right_pincer', (0, 3, -1), (0.3, 0, 0), [(-2.5, 0, -8, 5, 2, 8, 'clawtip')]),
    part('left_claw', 'root', (9, 15, -8), (0, -0.5, 0), [(-1.5, -1.5, -8, 3, 3, 8, 'arm')], True),
    part('left_pincer', 'left_claw', (0, 0, -8), (0, 0.7, 0), [(-3.5, -3, -9, 7, 6, 9, 'claw')], True),
    part('left_finger', 'left_pincer', (0, 3, -1), (0.3, 0, 0), [(-2.5, 0, -8, 5, 2, 8, 'clawtip')], True),
] + LEGS


def paint_crab(p, style, f, dims):
    if style == 'carapace':
        p.shaded(f, CRAB, belly=CRAB_UNDER)
        fx, fy, fw, fh = f['top']                # knobs and a darker saddle across the shell
        for _ in range(60):
            x, y = fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)
            p.px((x, y), [v * 1.25 for v in CRAB])
        for i in range(fw):
            for j in range(fh // 3, fh // 3 + 3):
                p.px((fx + i, fy + j), [v * 0.8 for v in CRAB])
        fx, fy, fw, fh = f['front']              # a toothed front rim
        for i in range(0, fw, 4):
            p.px((fx + i, fy + fh - 1), CRAB_UNDER)
    if style in ('under', 'mouth'):
        p.shaded(f, CRAB_UNDER if style == 'under' else CRAB_DARK)
    if style in ('arm', 'leg', 'legtip'):
        p.shaded(f, CRAB, belly=CRAB_UNDER)
        for k in ('top', 'right', 'left'):
            bands(p, f[k], CRAB, every=6, k=0.7, horizontal=False)
    if style == 'claw':
        p.shaded(f, CRAB, belly=CRAB_UNDER)
        for _ in range(40):
            k = p.rnd.choice(('top', 'right', 'left'))
            fx, fy, fw, fh = f[k]
            p.px((fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)), [v * 1.3 for v in CRAB])
    if style == 'clawtip':
        for k, r in f.items():
            p.gradient(r, CRAB, (40, 30, 30), horizontal=k in ('top', 'bottom', 'right', 'left'))
    if style == 'helm':
        p.shaded(f, GOLD)
        bands(p, f['front'], GOLD, every=3, k=0.75)
    if style == 'helmgold':
        p.shaded(f, GOLD_DEEP)
    if style == 'plume':
        for r in f.values():
            p.face(r, PLUME, 1.0, noise=10, fall=0.3)
    if style == 'stalk':
        p.shaded(f, CRAB)
    if style == 'eye':
        for r in f.values():
            p.face(r, (14, 12, 16), 1.0, noise=2)
        fx, fy, fw, fh = f['front']
        p.px((fx + 1, fy + 1), (220, 180, 60), glow=True)


# ── Yaksha ───────────────────────────────────────────────────────────────────
BLUE = (58, 104, 168)
BLUE_DARK = (34, 62, 110)
HAIR = (196, 40, 30)
CLOTH = (60, 120, 80)

YAKSHA = [
    part('head', 'root', (0, 0, 0), (0, 0, 0), [
        (-4, -8, -4, 8, 8, 8, 'face'),
        (-4.5, -10, -2, 9, 4, 7, 'hair'),
        (-4.5, -8, 2.5, 9, 10, 2.5, 'hair'),
        (-1.5, -1, -4.6, 1, 2.5, 1, 'tusk'),
        (0.5, -1, -4.6, 1, 2.5, 1, 'tusk'),
    ]),
    part('right_horn', 'head', (-2.5, -8, -2), (-0.4, 0, -0.35), [(-0.75, -4, -0.75, 1.5, 4, 1.5, 'horn')]),
    part('left_horn', 'head', (2.5, -8, -2), (-0.4, 0, 0.35), [(-0.75, -4, -0.75, 1.5, 4, 1.5, 'horn')], True),
    part('body', 'root', (0, 0, 0), (0, 0, 0), [
        (-4, 0, -2, 8, 12, 4, 'skin'),
        (-4.5, 9, -2.5, 9, 1.5, 5, 'gold'),
        (-4.5, 10.5, -2.5, 9, 6, 5, 'cloth'),
    ]),
    part('right_arm', 'root', (-5, 2, 0), (0, 0, 0), [(-3, -2, -2, 4, 12, 4, 'skin'), (-3.4, 6, -2.4, 4.8, 2, 4.8, 'gold')]),
    part('left_arm', 'root', (5, 2, 0), (0, 0, 0), [(-1, -2, -2, 4, 12, 4, 'skin'), (-1.4, 6, -2.4, 4.8, 2, 4.8, 'gold')], True),
    part('right_leg', 'root', (-1.9, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 12, 4, 'leg'), (-2.3, 9, -2.3, 4.6, 1.5, 4.6, 'gold')]),
    part('left_leg', 'root', (1.9, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 12, 4, 'leg'), (-2.3, 9, -2.3, 4.6, 1.5, 4.6, 'gold')], True),
]


def paint_yaksha(p, style, f, dims):
    if style in ('skin', 'leg', 'face'):
        p.shaded(f, BLUE, belly=BLUE_DARK)
    if style == 'face':
        fx, fy, fw, fh = f['front']                # 16x16: a scowl, burning eyes, a wide mouth
        for i in range(2, 14):
            p.px((fx + i, fy + 5 - (1 if i in (6, 7, 8, 9) else 0)), BLUE_DARK)
        for ex in (3, 10):
            for i in range(3):
                p.px((fx + ex + i, fy + 7), (250, 60, 30), glow=True)
                p.px((fx + ex + i, fy + 8), (200, 30, 20), glow=True)
        for i in range(4, 12):
            p.px((fx + i, fy + 12), (40, 10, 18))
            p.px((fx + i, fy + 13), (90, 20, 30))
        for i in (5, 7, 8, 10):
            p.px((fx + i, fy + 12), IVORY)
    if style == 'hair':
        for k, r in f.items():
            p.face(r, HAIR, 1.0 if k == 'top' else 0.9, noise=12, fall=0.3)
            fx, fy, fw, fh = r
            for i in range(0, fw, 2):
                for j in range(fh):
                    if p.rnd.random() < 0.3:
                        p.px((fx + i, fy + j), [v * 0.7 for v in HAIR])
    if style == 'tusk':
        for r in f.values():
            p.face(r, IVORY, 1.0, noise=3)
    if style == 'horn':
        for r in f.values():
            p.gradient(r, (60, 50, 40), IVORY)
    if style == 'gold':
        p.shaded(f, GOLD)
    if style == 'cloth':
        p.shaded(f, CLOTH)
        for k in ('front', 'back', 'right', 'left'):
            bands(p, f[k], CLOTH, every=3, k=0.75, horizontal=False)


# ── The Dragon Kings ─────────────────────────────────────────────────────────
# A Dragon King (龙王) as the temples carve him: a tall man's shape in an emperor's dragon robe, and
# a dragon's head. The head long, in stepped sections tapering to a high blunt nose with flared
# nostrils, the jaws open on fangs, long barbels trailing from the lip and long brows sweeping
# back, branching antlers, a mane and spined frills behind the cheeks and down the neck, a beard;
# eyes deep under a heavy brow. The robe to the ground, wide-sleeved, a cloud collar (云肩) over
# the shoulders, gold-edged scale pauldrons, a hooped jade belt, an apron panel down the front, a
# dragon on the breast and the sea-and-cliff hem (海水江崖); scaled, taloned hands from the sleeves,
# clawed feet and the tail from under the hem. The four share one model; each king shows only his
# own crown, beard and regalia (the parts named for his sea):
#   East, Ao Guang, the eldest: blue-green; an emperor's mian crown, its board hung with beads; a
#       long white beard to the belt; he holds the court tablet (笏). Drawn largest.
#   South, Ao Qin: red; a gold crown of flames round a ruby; a forked beard like fire; the flaming
#       pearl in his hand.
#   West, Ao Run: white; the black official's hat with its long wings; a pointed silver beard; a jade
#       ruyi sceptre.
#   North, Ao Shun: black; an iron crown of icicles; a long beard parted in two; a straight sword and
#       the frost pearl.
SEAS = ('east', 'south', 'west', 'north')


def chain(name, parent, pivot, rot, segs, style):
    """A whisker or a horn: segments along +z, each a child of the last; segs are (length, thickness,
    turn from the last)."""
    out, par, at = [], parent, pivot
    for i, (length, thick, turn) in enumerate(segs):
        n = name if i == 0 else '%s_%d' % (name, i + 1)
        out.append(part(n, par, at, rot if i == 0 else turn, [(-thick / 2, -thick / 2, 0, thick, thick, length, style)]))
        par, at = n, (0, 0, length - 0.2)
    return out


def hang(name, parent, pivot, rot, segs, style):
    """A lock hanging down in tapering segments, each a child of the last: segs are (width, length,
    depth, turn from the last)."""
    out, par, at = [], parent, pivot
    for i, (w, length, d, turn) in enumerate(segs):
        n = name if i == 0 else '%s_%d' % (name, i + 1)
        out.append(part(n, par, at, rot if i == 0 else turn, [(-w / 2, 0, -d / 2, w, length, d, style)]))
        par, at = n, (0, length - 0.4, 0)
    return out


def _rot(r):
    cx, sx, cy, sy, cz, sz = (f(a) for a in r for f in (math.cos, math.sin))
    x = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    y = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    z = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return _mul(_mul(z, y), x)


def _mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def aim(ancestors, world):
    """The turn a part needs, under parts turned `ancestors` (outermost first), to lie turned `world`
    (x, y, z radians, as a part's) in the model: so a hand's sleeve hangs straight down and what it
    holds stands up, however the arm is bent."""
    f = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    for r in ancestors:
        f = _mul(f, _rot(r))
    ft = [[f[j][i] for j in range(3)] for i in range(3)]
    m = _mul(ft, _rot(world))
    b = math.asin(max(-1, min(1, -m[2][0])))
    return (math.atan2(m[2][1], m[2][2]), b, math.atan2(m[1][0], m[0][0]))


ARM, FOREARM = (-0.12, 0, 0.1), (-1.2, -0.55, 0)
LARM, LFOREARM = ((x, -y, -z) for x, y, z in (ARM, FOREARM))   # the left, mirrored


KING = mirrored([
    # the body from the waist up, robed; its pivot the waist, so it carries the head and arms as it bobs
    part('body', 'root', (0, 2, 0), (0, 0, 0), [
        (-6.5, -18, -4.5, 13, 18, 9, 'chest'),
        (-4, -19.5, -3.5, 8, 2, 7, 'collar'),
        (-2.5, -24, -2.5, 5, 5, 5, 'neck'),
        (-8, -18.6, -5.5, 16, 0.6, 11, 'cloudtop'),          # the cloud collar, over the shoulders
        (-7, -18.4, -5.6, 14, 6, 0.2, 'lappet'),               # its points, down the breast
        (-7, -18.4, 5.4, 14, 7, 0.2, 'lappet_back'),           # and down the back
        (-7.5, -4, -5.5, 15, 2, 11, 'jadebelt'),
        (-2.5, -4.5, -6, 5, 3, 1, 'beltplaque'),
    ]),
    part('skirt', 'body', (0, 0, 0), (0, 0, 0), [
        (-7.5, 0, -5, 15, 8, 10, 'robe1'),
        (-8.5, 8, -6, 17, 7, 12, 'robe2'),
        (-9.5, 15, -7, 19, 7, 14, 'hem'),
    ]),
    part('apron', 'skirt', (0, -1.5, -5.3), (-0.17, 0, 0), [(-3.5, 0, -1, 7, 23, 1, 'apron')]),
    part('right_pendant', 'skirt', (-6, -1.5, -5.2), (-0.12, 0, 0.06), [
        (-0.5, 0, -0.5, 1, 6, 1, 'cord'), (-1.5, 6, -0.8, 3, 2.5, 1, 'jadedisc'), (-0.5, 8.5, -0.5, 1, 4, 1, 'tassel')]),
    # the clawed feet, from under the hem
    part('right_foot', 'root', (-3.4, 22, -6), (0, 0.18, 0), [(-2, 0, -3.5, 4, 2, 4, 'foot')]),
    part('right_toes', 'right_foot', (0, 1.2, -3.4), (0.35, 0, 0), [
        (-1.9, -0.6, -1.8, 0.8, 1.2, 1.8, 'talon'), (-0.4, -0.6, -2.1, 0.8, 1.2, 2.1, 'talon'), (1.1, -0.6, -1.8, 0.8, 1.2, 1.8, 'talon')]),
    # the tail, from under the hem behind, along the floor
    part('tail', 'root', (0, 20.8, 5.5), (-0.12, 0, 0), [(-2.5, -2.5, 0, 5, 5, 7, 'tail'), (-0.1, -5, 0.5, 0.2, 3, 6.5, 'dorsal')]),
    part('tail2', 'tail', (0, 0.2, 6.6), (0.12, 0.35, 0), [(-2, -2, 0, 4, 4, 6, 'tail'), (-0.1, -4.2, 0.3, 0.2, 2.5, 5.5, 'dorsal')]),
    part('tail3', 'tail2', (0, 0.3, 5.6), (0, 0.45, 0), [(-1.5, -1.5, 0, 3, 3, 5, 'tail'), (-0.1, -3.4, 0.3, 0.2, 2, 4.5, 'dorsal')]),
    part('tail_tip', 'tail3', (0, 0.2, 4.6), (-0.1, 0.4, 0), [(-1, -1, 0, 2, 2, 4, 'tail'), (-0.1, -3.5, 1, 0.2, 7, 8, 'tuft')]),
    # the arms: scaled pauldrons over the shoulders, the forearms brought in before him, the sleeves
    # hanging wide from them, the hands out of their mouths
    part('right_arm', 'body', (-7.5, -16, 0), ARM, [(-3.5, -2, -3, 5, 10, 6, 'sleeve')]),
    part('right_pauldron', 'right_arm', (-1, -2.5, 0), (0, 0, 0.28), [
        (-4, -1.5, -4, 5, 2, 8, 'pauldron'),
        (-4.5, 0.3, -4.5, 5, 2, 9, 'pauldron2'),
        (-5, 2.1, -4.5, 5, 2, 9, 'pauldron3'),
    ]),
    part('right_forearm', 'right_arm', (-1, 7.5, 0), FOREARM, [
        (-2.5, 0, -2.5, 5, 8, 5, 'forearm'),
        (-3, 6, -3, 6, 2, 6, 'cuff'),
    ]),
    part('right_sleeve', 'right_forearm', (0, 0.5, 2.2), aim([ARM, FOREARM], (0.06, -0.35, 0.05)), [(-3, -1, -9, 6, 12, 9, 'bigsleeve')]),
    part('right_hand', 'right_forearm', (0, 8.2, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3, 3, 3, 'hand')]),
    part('right_claws', 'right_hand', (0, 3, -0.6), (-0.5, 0, 0), [
        (-1.5, 0, -1.1, 0.7, 2, 0.7, 'talon'), (-0.35, 0, -1.3, 0.7, 2.2, 0.7, 'talon'), (0.8, 0, -1.1, 0.7, 2, 0.7, 'talon')]),
    part('right_thumb', 'right_hand', (-1.2, 1.5, -1.4), (-0.6, 0, 0.5), [(-0.35, 0, -0.35, 0.7, 2, 0.7, 'talon')]),
    # the head: the skull, the cheeks broad at the hinge of the jaw, the mane behind
    part('head', 'body', (0, -23.5, -1.5), (0, 0, 0), [
        (-4.5, -8, -5, 9, 8, 9, 'skull'),
        (-5.5, -4, -3, 11, 4, 6, 'cheek'),
        (-4, -8.5, 1.5, 8, 9, 4, 'mane'),
        (-1.5, -8.8, -5.2, 3, 1, 5, 'ridge'),
        (-5.9, -7.6, -5.8, 2.4, 1.8, 2.6, 'eye_r'),     # the eyes high and wide, clear of the snout
        (3.5, -7.6, -5.8, 2.4, 1.8, 2.6, 'eye_l'),
    ]),
    part('right_brow', 'head', (-0.6, -7.7, -5.6), (0, 0, 0.3), [(-4.8, -1.3, -0.5, 4.8, 1.8, 4.5, 'brow')]),
    part('right_browflame', 'right_brow', (-4.2, -0.6, 0.5), (0.45, -0.45, 0), [(-0.1, -3.5, 0, 0.2, 4.5, 7, 'browflame')]),
    part('right_cheekbone', 'head', (-4.6, -4.4, -4.2), (0, 0.35, 0.15), [(-1.4, -1, -0.5, 2, 2.2, 4, 'cheekplate')]),
    # the snout in steps, narrowing, to a high flared nose
    part('snout', 'head', (0, -3.4, -5), (0.05, 0, 0), [
        (-3.5, -2.6, -4, 7, 4, 4, 'snout'),
        (-3, -2.2, -8, 6, 4, 4, 'snout2'),
        (-3.5, -3.2, -11, 7, 5, 3, 'nose'),
        (-1, -3.2, -7.6, 2, 1, 4.2, 'ridge2'),
        (-2.9, 1.5, -10, 0.6, 1.3, 7, 'teeth'),
        (2.3, 1.5, -10, 0.6, 1.3, 7, 'teeth'),
        (-3, 1.4, -10.8, 1, 2.6, 1, 'fang'),
        (2, 1.4, -10.8, 1, 2.6, 1, 'fang'),
    ]),
    part('right_nostril', 'snout', (-2.2, -2.9, -9.9), (0.45, 0, -0.45), [(-1.1, -1.5, -1.1, 2.2, 1.5, 2.2, 'nostril')]),
    part('jaw', 'head', (0, -0.6, -3), (0.26, 0, 0), [
        (-3.5, 0, -5, 7, 2, 6, 'jaw'),
        (-2.8, 0, -10, 5.6, 1.8, 5, 'jaw2'),
        (-2.6, -1.1, -9.6, 0.6, 1.1, 6, 'teeth'),
        (2, -1.1, -9.6, 0.6, 1.1, 6, 'teeth'),
        (-2.6, -2, -9.9, 0.9, 2, 0.9, 'fang'),
        (1.7, -2, -9.9, 0.9, 2, 0.9, 'fang'),
        (-1.5, -0.4, -8.5, 3, 0.5, 7.5, 'tongue'),
    ]),
    # the hair of his cheeks swept back in flames, the crest down the back of the head, the mane hanging
    part('right_frill', 'head', (-5.2, -4.2, -1.5), (0.2, -0.55, 0.12), [(-0.1, -3.5, 0, 0.2, 8, 8, 'frill')]),
    part('right_ear', 'head', (-4.2, -7, 0), (0.5, -0.7, 0.3), [(-0.1, -2, 0, 0.2, 3.5, 5, 'earfin')]),
    part('crest', 'head', (0, -8.4, 0.5), (-0.35, 0, 0), [(-0.1, -4, 0, 0.2, 4.5, 8, 'crestfin')]),
] + hang('mane_lock', 'head', (0, -2, 5), (0.4, 0, 0), [
    (6, 6, 2.4, None), (5, 6, 2, (0.15, 0, 0)), (3.5, 4, 1.6, (0.1, 0, 0))], 'manelock') + hang(
    'right_mane', 'head', (-3.6, -4.5, 3.6), (0.3, 0.2, 0.28), [
        (3, 6, 2.4, None), (2.4, 5, 2, (0.15, 0, -0.1)), (1.6, 3.5, 1.4, (0.1, 0, -0.1))], 'manelock') + chain(
    'right_antler', 'head', (-3, -7.6, 0.2), (0.95, -0.38, 0), [
        (6, 1.8, None), (5, 1.4, (-0.45, 0.05, 0)), (4.5, 1.1, (-0.4, 0.1, 0)), (3, 0.8, (-0.3, 0, 0))], 'horn') + [
    part('right_antler_tine', 'right_antler', (0, 0, 3.2), (0.9, -0.25, 0), [(-0.55, -0.55, 0, 1.1, 1.1, 4, 'horn')]),
    part('right_antler_tine2', 'right_antler_2', (0, 0, 3.2), (1.0, 0.2, 0), [(-0.5, -0.5, 0, 1, 1, 3.5, 'horn')]),
    part('right_antler_tine3', 'right_antler_3', (0, 0, 3), (0.9, -0.3, 0), [(-0.4, -0.4, 0, 0.8, 0.8, 2.5, 'horn')]),
    part('right_antler_brow', 'right_antler', (0, 0, 1.2), (1.75, -0.35, 0), [(-0.45, -0.45, 0, 0.9, 0.9, 2.6, 'horn')]),
] + chain('right_whisker', 'snout', (-3.5, 0.2, -10), (-0.15, -1.1, 0), [
    (5, 0.9, None), (5.5, 0.8, (-0.25, 0.45, 0)), (5, 0.7, (-0.1, 0.35, 0)), (4, 0.5, (0.5, 0.2, 0))], 'whisker') + [
    # ── each king's own: crown, beard, regalia ──
    # East: the mian crown, its board hung with strings of beads front and back, a pin through it
    part('east_crown', 'head', (0, -8, -1), (0, 0, 0), [
        (-2.5, -3.5, -2.5, 5, 3.5, 5, 'miancap'),
        (-4, -4.5, -6, 8, 1, 11, 'mianboard'),
        (-6, -2.4, -0.4, 12, 0.8, 0.8, 'hairpin'),
        (-0.8, -3, -2.9, 1.6, 1.6, 0.6, 'jewel'),
        (-4.6, -3.6, -2.6, 9.2, 0.8, 5.2, 'crownrim'),
    ] + [(-3.4 + k * 1.36, -3.5, -5.6, 0.4, 3.8, 0.4, 'beads') for k in range(6)]
      + [(-3.4 + k * 1.36, -3.5, 4.4, 0.4, 3.8, 0.4, 'beads') for k in range(6)]),
    part('right_east_ribbon', 'east_crown', (-5.8, -2, 0), (0.1, 0, 0.12), [(-0.3, 0, -0.3, 0.6, 4, 0.6, 'ribbon'), (-0.7, 4, -0.7, 1.4, 1.4, 1.4, 'jewel')]),
] + hang('east_beard', 'jaw', (0, 1.5, -5.6), (-0.34, 0, 0), [
    (4, 5, 1.8, None), (3.4, 5, 1.5, (0.12, 0, 0.04)), (2.6, 5, 1.2, (0.1, 0, -0.06)), (1.4, 4, 0.8, (0.12, 0, 0.05))], 'beard') + [
    part('east_item', 'right_hand', (0, 1.8, -0.2), aim([ARM, FOREARM], (-0.3, 0.1, 0)), [
        (-1.3, -14, -0.4, 2.6, 15.5, 0.8, 'hu')]),
    # South: a gold crown of flames round a ruby
    part('south_crown', 'head', (0, -8, -3), (0, 0, 0), [
        (-4.7, -1.2, -2.4, 9.4, 1.8, 4.6, 'crownband'),
        (-0.9, -2.6, -2.9, 1.8, 1.8, 0.8, 'ruby'),
    ]),
    part('south_flame', 'south_crown', (0, -1, -2.2), (-0.15, 0, 0), [(-2, -7.5, 0, 4, 7.5, 0.3, 'flametine')]),
    part('right_south_flame', 'south_crown', (-2.6, -1, -2), (-0.1, 0.25, -0.38), [(-1.6, -5.5, 0, 3.2, 5.5, 0.3, 'flametine')]),
    part('right_south_flame2', 'south_crown', (-4.3, -0.6, -0.6), (-0.05, 0.9, -0.7), [(-1.2, -4, 0, 2.4, 4, 0.3, 'flametine')]),
    part('south_beard', 'jaw', (0, 1.5, -5.8), (-0.3, 0, 0), [(-2.2, 0, -0.9, 4.4, 3, 1.8, 'beard')]),
] + hang('right_south_fork', 'south_beard', (-1.1, 2.6, 0), (0.1, 0, 0.35), [
    (2.2, 5, 1.5, None), (1.6, 4, 1.1, (0.05, 0, -0.15)), (0.9, 3, 0.7, (0.05, 0, -0.2))], 'beard') + [
    part('south_item', 'left_hand', (0, 3, -1.8), aim([LARM, LFOREARM], (0, 0, 0)), [
        (-1.6, -3.4, -1.6, 3.2, 3.2, 3.2, 'firepearl'),
        (-0.1, -7.5, -2.5, 0.2, 7, 5, 'pearlflame'),
        (-2.5, -7.5, -0.1, 5, 7, 0.2, 'pearlflame'),
    ]),
    # West: the official's black hat (幞头), its two long wings straight out
    part('west_crown', 'head', (0, -8, -1), (0, 0, 0), [
        (-3.2, -4, -3.9, 6.4, 4.4, 6, 'futou'),
        (-2.6, -5.6, -0.6, 5.2, 1.8, 3.6, 'futou_back'),
        (-3.4, -1.4, -4.1, 6.8, 1.4, 6.4, 'hatband'),
        (-0.8, -3, -4.4, 1.6, 1.6, 0.6, 'jewel'),
    ]),
    part('right_west_wing', 'west_crown', (-2.8, -2.4, 1.2), (0, 0.05, 0.06), [(-12, -0.2, -0.6, 12, 0.4, 1.2, 'wing')]),
] + hang('west_beard', 'jaw', (0, 1.5, -6), (-0.3, 0, 0), [
    (3.4, 4, 1.6, None), (2.4, 4, 1.2, (0.1, 0, 0)), (1.2, 3, 0.8, (0.15, 0, 0))], 'beard') + hang(
    'right_west_moustache', 'snout', (-3.2, 1.6, -9.6), (0.25, -0.5, 0.35), [
        (1.2, 4, 1.2, None), (0.9, 3.5, 0.9, (0.1, 0, 0.2)), (0.6, 3, 0.6, (0.1, 0, 0.2))], 'beard') + [
    part('west_item', 'right_hand', (0, 1.8, -0.2), aim([ARM, FOREARM], (-0.3, 0.1, 0)), [
        (-0.5, -13, -0.5, 1, 15, 1, 'ruyishaft'),
        (-1.9, -15.6, -1.6, 3.8, 3, 3.2, 'ruyihead'),
        (-0.6, 1.5, -0.6, 1.2, 4, 1.2, 'tassel'),
    ]),
    # North: an iron crown of icicles round a frost gem
    part('north_crown', 'head', (0, -8, -3), (0, 0, 0), [
        (-4.7, -1.2, -2.4, 9.4, 1.8, 4.6, 'ironband'),
        (-0.9, -2.6, -2.9, 1.8, 1.8, 0.8, 'frostgem'),
    ]),
    part('north_spike', 'north_crown', (0, -1, -1.4), (-0.1, 0, 0), [(-0.7, -7.5, -0.7, 1.4, 7, 1.4, 'icicle'), (-0.35, -9.5, -0.35, 0.7, 2.5, 0.7, 'icicle')]),
    part('right_north_spike', 'north_crown', (-2.2, -1, -1.6), (-0.1, 0, -0.28), [(-0.6, -5.5, -0.6, 1.2, 5, 1.2, 'icicle'), (-0.3, -7, -0.3, 0.6, 2, 0.6, 'icicle')]),
    part('right_north_spike2', 'north_crown', (-4, -0.8, -0.6), (0, 0, -0.62), [(-0.5, -4, -0.5, 1, 3.6, 1, 'icicle'), (-0.25, -5.2, -0.25, 0.5, 1.6, 0.5, 'icicle')]),
    part('north_beard', 'jaw', (0, 1.5, -5.6), (-0.3, 0, 0), [(-2.6, 0, -0.9, 5.2, 3.5, 1.8, 'beard')]),
] + hang('right_north_strand', 'north_beard', (-1.3, 3, 0), (0.1, 0, 0.12), [
    (2.4, 6, 1.6, None), (1.9, 6, 1.3, (0.05, 0, -0.06)), (1.2, 4, 0.9, (0.05, 0, -0.08))], 'beard') + [
    part('north_item', 'right_hand', (0, 1.8, -0.2), aim([ARM, FOREARM], (-0.25, 0.1, 0.12)), [
        (-0.6, -17, -0.2, 1.2, 16, 0.4, 'swordblade'),
        (-0.3, -18.4, -0.2, 0.6, 1.6, 0.4, 'swordblade'),
        (-1.8, -1.2, -0.7, 3.6, 1, 1.4, 'guard'),
        (-0.5, -0.2, -0.5, 1, 3.8, 1, 'grip'),
        (-0.8, 3.4, -0.8, 1.6, 1, 1.6, 'guard'),
    ]),
    part('north_item2', 'left_hand', (0, 3, -1.8), aim([LARM, LFOREARM], (0, 0, 0)), [(-1.6, -3.4, -1.6, 3.2, 3.2, 3.2, 'frostpearl')]),
])

# Per king: robe, its shadow, the gold of his embroidery, the coloured thread beside it, his scales,
# his belly-plates, his mane and beard, his horns, his eyes (the iris and its hot core, a colour set
# against his skin, glowing out of a dark socket), a gem.
KINGS = {
    'ao_guang': dict(robe=(30, 116, 126), deep=(10, 46, 58), gold=(232, 190, 84), thread=(130, 214, 200),
                     scale=(44, 128, 140), belly=(216, 204, 150), hair=(240, 240, 232), horn=(230, 212, 164),
                     eye=(176, 104, 10), core=(232, 172, 34), gem=(90, 200, 180)),
    'ao_qin': dict(robe=(166, 28, 30), deep=(70, 8, 14), gold=(236, 184, 70), thread=(252, 160, 60),
                   scale=(178, 70, 34), belly=(240, 200, 136), hair=(236, 128, 44), horn=(226, 198, 150),
                   eye=(176, 38, 6), core=(236, 104, 16), gem=(236, 60, 30)),
    'ao_run': dict(robe=(226, 226, 220), deep=(126, 132, 148), gold=(210, 184, 112), thread=(140, 176, 210),
                   scale=(184, 198, 210), belly=(242, 238, 222), hair=(250, 250, 252), horn=(200, 198, 186),
                   eye=(176, 148, 50), core=(244, 226, 124), gem=(160, 214, 190)),
    'ao_shun': dict(robe=(30, 32, 44), deep=(6, 6, 12), gold=(176, 186, 206), thread=(84, 128, 184),
                    scale=(40, 54, 84), belly=(132, 144, 170), hair=(150, 158, 176), horn=(160, 160, 150),
                    eye=(28, 98, 160), core=(110, 198, 240), gem=(120, 196, 240)),
}
LIGHT = {'top': 1.08, 'bottom': 0.62, 'front': 1.0, 'back': 0.86, 'right': 0.93, 'left': 0.93}


def mix(a, b, t):
    return [x + (y - x) * t for x, y in zip(a, b)]


def mul(c, k):
    return [v * k for v in c]


def fabric(p, rect, base, deep, light=1.0, fold=7, fall=0.35):
    """Silk: lit at the top, falling into shadow toward the hem, in soft vertical folds, faintly woven."""
    fx, fy, fw, fh = rect
    phase = p.rnd.random() * 6.3
    for j in range(fh):
        t = j / max(1, fh - 1)
        c0 = mix(base, deep, t * fall)
        for i in range(fw):
            f = 1.0 + 0.09 * math.sin(i * 2 * math.pi / fold + phase) + 0.04 * math.sin(i * 0.9 + phase * 2)
            weave = 0.97 if (i + j) % 2 else 1.0
            n = p.rnd.randint(-2, 2)
            p.px((fx + i, fy + j), [v * light * f * weave + n for v in c0])


def fabric_box(p, f, c, fall=0.35, fold=7):
    for k, r in f.items():
        if k == 'bottom':
            p.face(r, mul(c['deep'], 1.1), 1.0, noise=2)
        elif k == 'top':
            fabric(p, r, c['robe'], c['deep'], 1.04, fold, fall=0.1)
        else:
            fabric(p, r, c['robe'], c['deep'], LIGHT[k], fold, fall)


def cloud(p, cx, cy, colour, r=2, rect=None):
    """An auspicious cloud (祥云): three curls in a row over a trailing tail, drawn in thread."""
    def put(x, y):
        if rect is None or (rect[0] <= x < rect[0] + rect[2] and rect[1] <= y < rect[1] + rect[3]):
            p.px((x, y), colour)
    for ox, oy in ((-r - 1, 0), (0, -1), (r + 1, 0)):
        for a in range(0, 360, 30):
            if 200 < a < 340 and ox == 0:
                continue
            put(cx + ox + round(r * math.cos(math.radians(a))), cy + oy + round(r * math.sin(math.radians(a))))
    for i in range(r * 2 + 2):
        put(cx + r + 2 + i, cy + r - (i // 3))


def clouds(p, rect, colour, every=9, r=2, share=0.7):
    """Clouds scattered over a face in staggered rows, here and there one left out."""
    fx, fy, fw, fh = rect
    row = 0
    for y in range(fy + 3, fy + fh - 3, every):
        off = (every // 2 + 3) if row % 2 else 0
        for x in range(fx + 3 + off, fx + fw - 3, every + 6):
            if p.rnd.random() < share:
                cloud(p, x + p.rnd.randint(-2, 2), y + p.rnd.randint(-1, 1), colour, r, rect)
        row += 1


def fret(p, rect, gold, deep, rows=None):
    """A gold band with a key-fret (回纹) running along it."""
    fx, fy, fw, fh = rect
    tile = ['1111110', '1000010', '1011010', '1010010', '1010000', '1011111']
    for j in range(fh):
        for i in range(fw):
            jj = int(j * len(tile) / max(1, fh)) if fh < 6 else j % 6
            on = tile[jj][i % 7] == '1'
            edge = j == 0 or j == fh - 1
            c = mul(gold, 1.12) if edge else (gold if on else mix(gold, deep, 0.55))
            p.px((fx + i, fy + j), [v + p.rnd.randint(-3, 3) for v in c])


def gold_box(p, f, gold, deep, glint=False):
    for k, r in f.items():
        fx, fy, fw, fh = r
        for j in range(fh):
            for i in range(fw):
                t = j / max(1, fh - 1)
                c = mix(mul(gold, 1.12), mul(gold, 0.72), t) if k not in ('top', 'bottom') else mul(gold, LIGHT[k])
                if (i * 7 + j * 3) % 11 == 0:
                    c = mul(c, 1.12)
                p.px((fx + i, fy + j), [v * (1 if k in ('top', 'bottom') else LIGHT[k]) + p.rnd.randint(-4, 4) for v in c])
        if k not in ('top', 'bottom'):
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), mix(gold, deep, 0.6))


def scaly(p, f, c, cell=4, belly=True):
    """Scales over a box, lit above; the underside belly-plates."""
    for k, r in f.items():
        if k == 'bottom' and belly:
            plates(p, r, c['belly'])
            continue
        p.scales(r, c['scale'], cell=cell, light=LIGHT[k] * 1.02)
        fx, fy, fw, fh = r
        if k not in ('top', 'bottom'):                  # darker toward the underside
            for j in range(fh * 2 // 3, fh):
                for i in range(fw):
                    q = p.img.getpixel((fx + i, fy + j))
                    p.px((fx + i, fy + j), mul(q[:3], 0.86))


def plates(p, rect, base):
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            k = 0.7 if j % 4 == 3 else 1.0 - 0.05 * (j % 4)
            p.px((fx + i, fy + j), [v * k * 0.9 + p.rnd.randint(-3, 3) for v in base])


def hair(p, rect, base, light=1.0, ragged=0.0, tip=None, root_dark=0.7):
    """Hair in strands down a face: each strand its own tone, darker in its partings, paler at its end;
    with `ragged`, the strands end unevenly in the face's last part (the rest left clear)."""
    fx, fy, fw, fh = rect
    tip = tip or mix(base, (255, 255, 255), 0.35)
    i = 0
    while i < fw:
        width = p.rnd.randint(2, 3)
        tone = p.rnd.uniform(0.86, 1.1)
        end = fh - int(fh * ragged * p.rnd.random())
        for di in range(width):
            if i + di >= fw:
                break
            part_ = di == width - 1
            for j in range(end):
                s = j / max(1, fh - 1)
                cc = mix(mul(base, root_dark), base, min(1, s * 2.5))
                if s > 0.7:
                    cc = mix(cc, tip, (s - 0.7) / 0.3 * 0.6)
                k = light * tone * (0.78 if part_ else 1.0)
                p.px((fx + i + di, fy + j), [v * k + p.rnd.randint(-4, 4) for v in cc])
        i += width


def plane(p, f, thin, mask, colour, glow=False):
    """A thin plate whose shape is cut from its two broad faces (`thin` the axis it is thin along):
    mask(s, t) says whether a point is solid, s across the face (0 at the part's -z, or -x, end), t down
    it; colour(s, t) its colour. Its edges are left clear."""
    if thin == 'x':
        faces_ = (('right', True), ('left', False))
    elif thin == 'z':
        faces_ = (('front', False), ('back', True))
    else:
        faces_ = (('top', False), ('bottom', False))
    for k, flip in faces_:
        fx, fy, fw, fh = f[k]
        for j in range(fh):
            for i in range(fw):
                s = (i + 0.5) / fw
                s = 1 - s if flip else s
                t = (j + 0.5) / fh
                if mask(s, t):
                    c = colour(s, t)
                    p.px((fx + i, fy + j), [v * (0.92 if flip else 1.0) + p.rnd.randint(-3, 3) for v in c], glow)


def tri(x):
    """A triangle wave, 0..1..0 over each unit."""
    x = x % 1.0
    return 1 - abs(2 * x - 1)


def dragon_motif(p, rect, gold, dark, pearl):
    """A gold dragon coiled on a face, rising toward a flaming pearl, among the robe's clouds."""
    fx, fy, fw, fh = rect
    pts = []
    for n in range(80):
        t = n / 79
        x = fw * (0.5 + 0.3 * math.sin(t * 3.3 * math.pi + 0.4))
        y = fh * (0.2 + 0.68 * t)
        pts.append((x, y, 2.6 - 1.6 * t))
    for (x, y, th) in pts:                       # its outline, then its body, scaled
        for dx in range(-3, 4):
            for dy in range(-3, 4):
                d = math.hypot(dx, dy)
                if d <= th + 0.9:
                    p.px((fx + x + dx, fy + y + dy), dark)
    for n, (x, y, th) in enumerate(pts):
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                if math.hypot(dx, dy) <= th - 0.2:
                    c = gold if (int(x + dx) + int(y + dy)) % 3 else mul(gold, 0.78)
                    p.px((fx + x + dx, fy + y + dy), c)
    hx, hy = fx + pts[0][0], fy + pts[0][1]      # its head, horns back, a beard; its legs
    for dx in range(-3, 4):
        for dy in range(-2, 3):
            if abs(dx) + abs(dy) <= 3:
                p.px((hx + dx, hy + dy), gold)
    p.px((hx - 1, hy - 1), dark)
    for i in range(4):
        p.px((hx + 2 + i, hy - 2 - i // 2), mul(gold, 1.1))
        p.px((hx - 2 - i, hy + 2 + i // 2), mul(gold, 0.9))
    for t_ in (18, 40, 62):
        x, y, _ = pts[t_]
        for i in range(3):
            p.px((fx + x + 2 + i, fy + y + 1 + i), gold)
            p.px((fx + x - 2 - i, fy + y + 1 + i), gold)
    px_, py_ = hx + 5, hy - 5                     # the pearl it reaches for, in flames
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            if dx * dx + dy * dy <= 4:
                p.px((px_ + dx, py_ + dy), pearl if dx * dx + dy * dy < 3 else dark)
    for i in range(3):
        p.px((px_ - 1 + i, py_ - 3 - (i % 2)), (240, 120, 40))


def sea_cliff(p, rect, c, peaks=(0.5,), wave_top=0.34, stripe_top=0.64):
    """The hem's sea and cliffs (海水江崖): slanting bands of standing water at the foot, rolling waves
    above them, and gold crags rising out of the waves."""
    fx, fy, fw, fh = rect
    robe, deep, gold, thread = c['robe'], c['deep'], c['gold'], c['thread']
    foam = mix(thread, (255, 255, 255), 0.55)
    water = mix(robe, deep, 0.35)
    stripes = [thread, foam, mix(robe, deep, 0.15), gold, mix(thread, deep, 0.3)]
    for j in range(fh):
        t = j / max(1, fh - 1)
        for i in range(fw):
            s = i / max(1, fw - 1)
            if t >= stripe_top:
                band = ((i + (fh - j)) // 3) % len(stripes)
                cc = stripes[band]
                if j == int(stripe_top * (fh - 1)):
                    cc = gold
            elif t >= wave_top:
                # scale-like rows of wave crests
                h = int(wave_top * fh)
                jj = j - h
                row = jj // 4
                ii = i + (4 if row % 2 else 0)
                cx, cy = ii % 8 - 3.5, jj % 4
                d = math.hypot(cx, cy * 1.6)
                cc = foam if 3.0 < d < 4.4 else (mix(water, thread, 0.25) if d < 3.0 else water)
            else:
                cc = None
            if cc is not None:
                p.px((fx + i, fy + j), [v * 0.96 + p.rnd.randint(-3, 3) for v in cc])
    for pk in peaks:                                     # the crags
        cx = pk * fw
        top, foot = fh * 0.08, fh * stripe_top
        for j in range(int(top), int(foot)):
            half = (j - top) / (foot - top) * fw * 0.14 + 1
            for i in range(int(cx - half), int(cx + half) + 1):
                if 0 <= i < fw:
                    edge = abs(i - cx) > half - 1.2
                    ridge = (i - int(cx)) % 3 == 0
                    cc = mix(gold, deep, 0.5) if edge else (mul(gold, 0.85) if ridge else gold)
                    p.px((fx + i, fy + j), cc)


# The eye's shape, outer corner first: the rows lit in each column. The top falls toward the nose, a
# slanted glare under the scowling brow (tall at the outer corner, a sliver at the inner).
EYE_ROWS = ((0, 2), (0, 3), (0, 3), (1, 3), (1, 3), (2, 3))
EYE_SLIT = 3                                               # the pupil: a dark slit, nearer the nose


def king_eye_box(p, f, c, side):
    """The eye: a dark socket; in it the iris, burning about a slit pupil, hottest beside the slit,
    dimmer under the lid and at the corners; drawn on the skin dark and again full-bright."""
    socket = (14, 9, 8)
    for k, r in f.items():                                 # the bony rim seating it in the skull
        p.scales(r, mul(c['scale'], 0.5), cell=4, light=LIGHT[k])
    p.face(f['front'], socket, 1.0, noise=2)

    def lit(x, y, col):
        p.px((x, y), mul(col, 0.3))                        # the skin dark beneath: the glow is drawn over it
        p.glow.putpixel((x, y), tuple(int(v) for v in col) + (255,))

    fx, fy, fw, fh = f['front']
    for o, (top, bottom) in enumerate(EYE_ROWS):
        x = fx + (o if side == 'right' else fw - 1 - o)
        for j in range(top, bottom + 1):
            if o == EYE_SLIT:
                p.px((x, fy + j), (6, 4, 4))
                continue
            if abs(o - EYE_SLIT) == 1 and j > top or o == EYE_SLIT - 2 and j == top + 1:
                col = c['core']                            # hottest beside the slit
            elif j == top or o in (0, len(EYE_ROWS) - 1):
                col = mul(c['eye'], 0.8)                   # under the lid, and at the corners
            else:
                col = c['eye']
            lit(x, fy + j, col)
    fx, fy, fw, fh = f[side]                               # round the outer corner, seen from aside
    for i in range(3):
        x = fx + fw - 1 - i if side == 'right' else fx + i
        for j in range(fh):
            p.px((x, fy + j), socket)
        for j in range(i // 2, 3):
            lit(x, fy + j, mul(c['eye'], (0.85, 0.7, 0.55)[i]))


def king_painter(c):
    robe, deep, gold, thread, scale, hair_c, horn = (c[k] for k in ('robe', 'deep', 'gold', 'thread', 'scale', 'hair', 'horn'))
    hair_dark = mul(hair_c, 0.55)

    def paint_king(p, style, f, dims):
        if style == 'chest':
            fabric_box(p, f, c, fall=0.2)
            for k in ('front', 'back'):
                clouds(p, f[k], mix(robe, thread, 0.55), every=10)
            fx, fy, fw, fh = f['front']
            dragon_motif(p, (fx + 2, fy + 10, fw - 4, fh - 12), gold, mul(deep, 0.8), (250, 240, 220))
            for j in range(fh * 2 // 3):                    # the crossed collar, the left over the right
                x = fx + fw // 2 + 2 - int(j * 0.75)
                for d in range(-2, 3):
                    q = (x + d, fy + j)
                    p.px(q, gold if abs(d) == 2 else mul(deep, 1.3))
            fx, fy, fw, fh = f['back']
            dragon_motif(p, (fx + 3, fy + 12, fw - 6, fh - 14), gold, mul(deep, 0.8), (250, 240, 220))
        if style in ('robe1', 'robe2', 'sleeve', 'forearm'):
            fabric_box(p, f, c)
            for k in ('front', 'back', 'right', 'left'):
                clouds(p, f[k], mix(robe, thread, 0.5), every=9)
        if style == 'hem':
            fabric_box(p, f, c, fall=0.15)
            for k in ('front', 'back'):
                sea_cliff(p, f[k], c, peaks=(0.5, 0.18, 0.82))
            for k in ('right', 'left'):
                sea_cliff(p, f[k], c, peaks=(0.5,))
            for k in ('front', 'back', 'right', 'left'):
                fx, fy, fw, fh = f[k]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), mul(gold, 0.7))
                    p.px((fx + i, fy + fh - 2), gold)
        if style == 'apron':
            for k, r in f.items():
                fabric(p, r, mix(robe, deep, 0.25), deep, LIGHT[k], 5, 0.25)
            fx, fy, fw, fh = f['front']
            for j in range(fh):
                for i in range(fw):
                    if i < 2 or i >= fw - 2:
                        p.px((fx + i, fy + j), gold if i in (0, fw - 1) else mul(deep, 1.2))
            sea_cliff(p, (fx + 2, fy + fh * 3 // 4, fw - 4, fh // 4), c, peaks=(0.5,), wave_top=0.2, stripe_top=0.6)
            dragon_motif(p, (fx + 1, fy + 4, fw - 2, fh // 2), gold, mul(deep, 0.8), (250, 240, 220))
        if style == 'bigsleeve':
            fabric_box(p, f, c, fold=6)
            for k in ('right', 'left', 'back'):
                clouds(p, f[k], mix(robe, thread, 0.5), every=9)
                fx, fy, fw, fh = f[k]
                sea_cliff(p, (fx, fy + fh - 9, fw, 9), c, peaks=(), wave_top=0.0, stripe_top=0.45)
            lining = mix(thread, (255, 255, 255), 0.25)
            fx, fy, fw, fh = f['front']                     # its mouth: the lining, the hand's shadow in it
            for j in range(fh):
                for i in range(fw):
                    if i < 2 or i >= fw - 2 or j >= fh - 2:
                        cc = gold if (i in (0, fw - 1) or j == fh - 1) else mul(deep, 1.2)
                    else:
                        d = math.hypot((i - fw / 2) / fw, (j - fh * 0.2) / fh)
                        cc = mix(mul(deep, 0.35), lining, min(1, max(0, (d - 0.12) * 3)))
                        cc = mul(cc, 0.85 + 0.15 * math.sin(i * 1.3))
                    p.px((fx + i, fy + j), cc)
            fx, fy, fw, fh = f['bottom']
            for j in range(fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), gold if (i in (0, fw - 1) or j in (0, fh - 1)) else mul(deep, 0.5))
        if style == 'cuff':
            for k, r in f.items():
                fret(p, r, gold, deep)
        if style in ('collar',):
            for k, r in f.items():
                p.face(r, (236, 230, 214), LIGHT[k], noise=3)
                fx, fy, fw, fh = r
                for i in range(fw):
                    p.px((fx + i, fy), gold)
        if style == 'neck':
            scaly(p, f, c, cell=4)
        if style == 'cloudtop':
            def m(s, t):                                    # four ruyi lobes about the neck
                x, y = s - 0.5, t - 0.5
                a = math.atan2(y, x)
                rr = 0.42 + 0.08 * math.cos(4 * a)
                return math.hypot(x * 1.0, y * 1.0) < rr
            def col(s, t):
                x, y = s - 0.5, t - 0.5
                d = math.hypot(x, y)
                a = math.atan2(y, x)
                rr = 0.42 + 0.08 * math.cos(4 * a)
                if d > rr - 0.05:
                    return gold
                if d > rr - 0.1:
                    return mul(deep, 1.1)
                return mix(robe, thread, 0.35) if int((a + 3.2) * 6) % 2 else mix(robe, gold, 0.25)
            plane(p, f, 'y', m, col)
            for k in ('front', 'back', 'right', 'left'):
                fx, fy, fw, fh = f[k]
                for i in range(fw):
                    for j in range(fh):
                        p.px((fx + i, fy + j), gold)
        if style in ('lappet', 'lappet_back'):
            def lobe(s):                                    # ruyi points: a broad one in the middle, two beside
                return 0.45 + 0.55 * max(0, math.cos((s - 0.5) * math.pi * 3)) ** 0.7
            def m(s, t):
                return t < lobe(s) and 0.03 < s < 0.97
            def col(s, t):
                e = lobe(s) - t
                if e < 0.1:
                    return gold
                if e < 0.18:
                    return mul(deep, 1.15)
                ring = math.hypot((s - 0.5) * 2.2, (t - 0.35) * 1.4)
                if abs(ring - 0.42) < 0.05:
                    return mix(gold, robe, 0.3)
                return mix(mix(robe, thread, 0.3), deep, t * 0.3)
            plane(p, f, 'z', m, col)
        if style == 'jadebelt':
            for k, r in f.items():
                fx, fy, fw, fh = r
                p.face(r, mul(deep, 1.4), LIGHT[k], noise=3)
                if k in ('top', 'bottom'):
                    continue
                for i in range(fw):
                    p.px((fx + i, fy), gold)
                    p.px((fx + i, fy + fh - 1), gold)
                for x in range(fx + 2, fx + fw - 4, 7):        # its jade plaques
                    for i in range(4):
                        for j in range(1, fh - 1):
                            jade = (130, 214, 160) if (i + j) % 3 else (176, 236, 196)
                            p.px((x + i, fy + j), jade)
        if style == 'beltplaque':
            for k, r in f.items():
                p.face(r, (120, 206, 150), LIGHT[k], noise=4)
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                p.px((fx + i, fy), gold)
                p.px((fx + i, fy + fh - 1), gold)
            for j in range(fh):
                p.px((fx, fy + j), gold)
                p.px((fx + fw - 1, fy + j), gold)
            p.px((fx + fw // 2, fy + fh // 2), (236, 248, 236))
        if style == 'cord':
            for k, r in f.items():
                p.face(r, gold, LIGHT[k], noise=4)
        if style == 'jadedisc':
            for k, r in f.items():
                p.face(r, (120, 206, 150), LIGHT[k], noise=5)
            fx, fy, fw, fh = f['front']
            p.px((fx + fw // 2, fy + fh // 2), (40, 70, 50))
        if style == 'tassel':
            for k, r in f.items():
                hair(p, r, (190, 40, 36) if k != 'top' else (160, 30, 30), LIGHT[k], ragged=0.4)
        if style in ('pauldron', 'pauldron2', 'pauldron3'):
            for k, r in f.items():                          # lames of scale, each edged in gold
                if k == 'bottom':
                    p.face(r, mul(deep, 0.8), 1.0)
                    continue
                p.scales(r, mix(robe, scale, 0.4) if style != 'pauldron' else mix(gold, deep, 0.2), cell=4, light=LIGHT[k])
                fx, fy, fw, fh = r
                if k != 'top':
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 1), mul(gold, 0.8))
                        p.px((fx + i, fy + fh - 2), mul(gold, 1.12))
                        p.px((fx + i, fy), mul(deep, 1.1))
        if style == 'hand':
            scaly(p, f, c, cell=4)
        if style == 'talon':
            for k, r in f.items():
                p.gradient(r, (60, 50, 44), (232, 222, 196), noise=3)
        if style == 'foot':
            scaly(p, f, c, cell=4)
        if style == 'tail':
            scaly(p, f, c, cell=4)
            fx, fy, fw, fh = f['top']                        # a darker line along its spine
            for j in range(fh):
                for i in range(fw // 2 - 1, fw // 2 + 1):
                    p.px((fx + i, fy + j), mul(scale, 0.6))
        if style == 'dorsal':
            plane(p, f, 'x', lambda s, t: (1 - t) < 0.35 + 0.65 * tri(s * 3 + 0.5),
                  lambda s, t: mix(mul(hair_c, 0.9), mul(scale, 0.6), t))
        if style == 'tuft':
            def m(s, t):
                y = abs(t - 0.5) * 2
                return s < 0.25 + 0.75 * (1 - y ** 1.6) * (0.75 + 0.25 * tri(t * 5))
            plane(p, f, 'x', m, lambda s, t: mix(mul(hair_c, 0.7), hair_c, s))
        if style in ('skull', 'snout', 'snout2', 'cheek'):
            for k, r in f.items():
                if k == 'bottom':
                    if style in ('snout', 'snout2'):
                        p.face(r, (96, 26, 32), 0.8, noise=3)       # the roof of the mouth
                    else:
                        plates(p, r, c['belly'])
                    continue
                p.scales(r, scale, cell=6 if style == 'skull' and k == 'top' else 4, light=LIGHT[k] * 1.04)
            if style in ('snout', 'snout2'):
                for k in ('right', 'left'):                  # the lip, dark, along the snout's foot
                    fx, fy, fw, fh = f[k]
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 1), (40, 14, 16))
                        p.px((fx + i, fy + fh - 2), mul(c['belly'], 0.9))
            if style == 'skull':
                for k in ('right', 'left'):                  # deep sockets under the brow at the front
                    fx, fy, fw, fh = f[k]
                    x0 = fx + fw - 6 if k == 'right' else fx
                    for i in range(6):
                        for j in range(4, 7):
                            q = p.img.getpixel((x0 + i, fy + j))
                            p.px((x0 + i, fy + j), mul(q[:3], 0.55))
                fx, fy, fw, fh = f['front']                  # the brow's shadow over the face's front
                for i in range(fw):
                    for j in range(4, 8):
                        q = p.img.getpixel((fx + i, fy + j))
                        p.px((fx + i, fy + j), mul(q[:3], 0.6))
                for i in list(range(7)) + list(range(fw - 7, fw)):   # and darker still about the eyes
                    for j in range(0, 7):
                        q = p.img.getpixel((fx + i, fy + j))
                        p.px((fx + i, fy + j), mul(q[:3], 0.35 if 1 <= j <= 5 and (i < 6 or i >= fw - 6) else 0.6))
                for k in ('right', 'left'):
                    fx, fy, fw, fh = f[k]
                    x0 = fx + fw - 3 if k == 'right' else fx
                    for i in range(3):
                        for j in range(1, 6):
                            q = p.img.getpixel((x0 + i, fy + j))
                            p.px((x0 + i, fy + j), mul(q[:3], 0.4))
            if style == 'cheek':
                for k in ('front',):
                    fx, fy, fw, fh = f[k]
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 1), (40, 14, 16))
        if style == 'nose':
            for k, r in f.items():
                p.scales(r, scale, cell=4, light=LIGHT[k] * 1.06)
            fx, fy, fw, fh = f['front']                      # a broad lip, the nostrils high and dark
            for i in range(fw):
                for j in range(fh - 3, fh):
                    p.px((fx + i, fy + j), mix(c['belly'], scale, 0.3))
                p.px((fx + i, fy + fh - 1), (40, 14, 16))
            for cx in (fx + 2, fx + fw - 5):
                for i in range(3):
                    for j in range(2, 5):
                        p.px((cx + i, fy + j), (20, 10, 10) if j > 2 else mul(scale, 0.6))
            fx, fy, fw, fh = f['bottom']
            p.face((fx, fy, fw, fh), (96, 26, 32), 0.8, noise=3)
        if style == 'nostril':
            for k, r in f.items():
                p.scales(r, scale, cell=4, light=LIGHT[k] * 1.1)
            fx, fy, fw, fh = f['front']
            for i in range(1, fw - 1):
                p.px((fx + i, fy + fh - 1), (20, 10, 10))
                p.px((fx + i, fy + fh - 2), (20, 10, 10))
        if style == 'cheekplate':
            for k, r in f.items():
                p.scales(r, mix(scale, c['belly'], 0.25), cell=4, light=LIGHT[k] * 1.05)
        if style == 'earfin':
            plane(p, f, 'x', lambda s, t: (1 - t) < 0.2 + 0.8 * (1 - s) ** 0.6 * (0.7 + 0.3 * tri(s * 3)),
                  lambda s, t: mix(mul(scale, 0.7), mix(scale, c['belly'], 0.5), 1 - t))
        if style in ('ridge', 'ridge2'):
            for k, r in f.items():
                plates(p, r, mix(scale, c['belly'], 0.45))
        if style == 'brow':
            for k, r in f.items():
                if k == 'bottom':
                    p.face(r, (16, 10, 10), 1.0, noise=2)
                else:
                    p.scales(r, mul(scale, 1.1), cell=4, light=LIGHT[k])
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), (24, 14, 12))
        if style in ('eye_r', 'eye_l'):
            king_eye_box(p, f, c, 'right' if style == 'eye_r' else 'left')
        if style in ('jaw', 'jaw2'):
            for k, r in f.items():
                if k == 'top':
                    p.face(r, (110, 30, 38), 0.85, noise=4)      # inside the mouth
                elif k == 'bottom':
                    plates(p, r, c['belly'])
                else:
                    p.scales(r, scale, cell=4, light=LIGHT[k])
                    fx, fy, fw, fh = r
                    for i in range(fw):
                        p.px((fx + i, fy), (40, 14, 16))
        if style == 'teeth':
            for k, r in f.items():
                fx, fy, fw, fh = r
                if k in ('top', 'bottom'):
                    continue
                for i in range(fw):
                    reach = fh if i % 3 == 1 else max(1, fh - 2)
                    for j in range(reach):
                        p.px((fx + i, fy + j), mix((240, 234, 214), (190, 176, 140), j / max(1, fh)))
        if style == 'fang':
            for k, r in f.items():
                p.gradient(r, (236, 230, 210), (200, 186, 150), noise=3)
        if style == 'tongue':
            for k, r in f.items():
                p.face(r, (196, 66, 82), LIGHT[k], noise=6)
        if style == 'mane':
            for k, r in f.items():
                hair(p, r, hair_c, LIGHT[k])
        if style == 'manelock':
            for k, r in f.items():
                if k == 'bottom':
                    continue
                if k == 'top':
                    p.face(r, mul(hair_c, 0.8), 1.0)
                else:
                    hair(p, r, hair_c, LIGHT[k], ragged=0.35)
        if style == 'beard':
            for k, r in f.items():
                if k == 'bottom':
                    continue
                if k == 'top':
                    p.face(r, mul(hair_c, 0.8), 1.0)
                else:
                    hair(p, r, hair_c, LIGHT[k], ragged=0.3, root_dark=0.8)
        if style == 'frill':
            def reach(t):                                   # three locks, curling at their ends
                return 0.35 + 0.65 * tri(t * 3 + 0.15 * math.sin(t * 9)) ** 0.7 * (1 - 0.35 * t)
            def m(s, t):
                return s < reach(t)
            def col(s, t):
                lock = tri(t * 3 + 0.15 * math.sin(t * 9))   # each lock lit along its middle
                strand = 0.9 + 0.1 * math.sin(t * 23 + s * 11)
                cc = mix(mul(scale, 0.55), hair_c, min(1, s * 2.6))
                cc = mul(cc, (0.7 + 0.3 * lock) * strand)
                if s > reach(t) - 0.1:
                    cc = mix(cc, (255, 255, 255), 0.18)
                return cc
            plane(p, f, 'x', m, col)
        if style == 'browflame':
            def m(s, t):                                    # flames of brow, streaming back
                return (1 - t) < 0.25 + 0.75 * tri(s * 2.5 + 0.2) ** 0.8 * (1 - 0.6 * s)
            plane(p, f, 'x', m, lambda s, t: mix(hair_c, hair_dark, t * 0.8))
        if style == 'crestfin':
            def m(s, t):
                return (1 - t) < 0.3 + 0.7 * tri(s * 3.5) ** 0.9
            plane(p, f, 'x', m, lambda s, t: mix(hair_c, mul(scale, 0.7), t))
        if style == 'horn':
            for k, r in f.items():
                p.gradient(r, mul(horn, 0.72), mul(horn, 1.08), noise=3, horizontal=k in ('top', 'bottom', 'right', 'left'))
            for k in ('right', 'left', 'top', 'bottom'):        # rings round the horn
                fx, fy, fw, fh = f[k]
                for i in range(1, fw, 3):
                    for j in range(fh):
                        q = p.img.getpixel((fx + i, fy + j))
                        p.px((fx + i, fy + j), mul(q[:3], 0.8))
        if style == 'whisker':
            for k, r in f.items():
                p.gradient(r, gold, mix(gold, (255, 250, 220), 0.5), noise=4, horizontal=k in ('top', 'bottom', 'right', 'left'))
        # ── regalia ──
        if style == 'miancap':
            for k, r in f.items():
                p.face(r, (26, 22, 30), LIGHT[k], noise=3)
                fx, fy, fw, fh = r
                if k not in ('top', 'bottom'):
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 2), gold)
                    for i in range(1, fw, 4):
                        for j in range(1, fh - 3):
                            if j % 3 == 1:
                                p.px((fx + i, fy + j), mul(gold, 0.85))
        if style == 'mianboard':
            for k, r in f.items():
                fx, fy, fw, fh = r
                p.face(r, (24, 20, 28) if k == 'top' else (150, 30, 30), LIGHT[k], noise=3)
                if k == 'top':
                    for i in range(fw):
                        p.px((fx + i, fy), gold)
                        p.px((fx + i, fy + fh - 1), gold)
                    for j in range(fh):
                        p.px((fx, fy + j), gold)
                        p.px((fx + fw - 1, fy + j), gold)
                    clouds(p, (fx + 2, fy + 2, fw - 4, fh - 4), mul(gold, 0.8), every=8)
                elif k != 'bottom':
                    for i in range(fw):
                        p.px((fx + i, fy), gold)
        if style == 'beads':
            for k, r in f.items():
                fx, fy, fw, fh = r
                for j in range(fh):
                    cc = ((236, 236, 220), (120, 210, 170), (210, 50, 40), gold)[(j // 2) % 4]
                    for i in range(fw):
                        p.px((fx + i, fy + j), mul(cc, LIGHT[k]))
        if style in ('hairpin', 'crownrim', 'hatband'):
            gold_box(p, f, gold, deep)
        if style == 'ribbon':
            for k, r in f.items():
                p.face(r, (176, 34, 40), LIGHT[k], noise=4, fall=0.3)
        if style in ('jewel', 'ruby', 'frostgem'):
            gem = c['gem'] if style != 'ruby' else (230, 40, 36)
            for k, r in f.items():
                p.face(r, mul(gem, 0.6), 1.0, noise=4)
                fx, fy, fw, fh = r
                for j in range(1, fh - 1):
                    for i in range(1, fw - 1):
                        p.px((fx + i, fy + j), mul(gem, 0.5))
                        p.glow.putpixel((fx + i, fy + j), tuple(int(v * 0.75) for v in gem) + (255,))
        if style == 'hu':
            for k, r in f.items():
                p.gradient(r, (246, 240, 222), (214, 204, 176), noise=3)
            fx, fy, fw, fh = f['front']
            for j in range(fh):
                p.px((fx, fy + j), gold)
                p.px((fx + fw - 1, fy + j), gold)
            for j in range(4, fh - 6, 4):                       # writing down it
                p.px((fx + fw // 2, fy + j), (60, 50, 44))
                p.px((fx + fw // 2 - 1, fy + j + 1), (60, 50, 44))
        if style == 'crownband':
            gold_box(p, f, gold, deep)
            fx, fy, fw, fh = f['front']
            for i in range(2, fw - 2, 4):
                p.px((fx + i, fy + fh // 2), (220, 50, 40))
        if style == 'flametine':
            def m(s, t):
                w = 0.5 * (t ** 0.6) * (0.8 + 0.2 * math.sin(t * 14))
                return abs(s - 0.5 - 0.08 * math.sin(t * 6)) < w
            def col(s, t):
                core = abs(s - 0.5) < 0.18 * t
                return mix((255, 236, 150), gold, t) if not core else (230, 60, 30)
            plane(p, f, 'z', m, col)
        if style == 'firepearl':
            for k, r in f.items():
                fx, fy, fw, fh = r
                for j in range(fh):
                    for i in range(fw):
                        d = math.hypot((i + 0.5) / fw - 0.4, (j + 0.5) / fh - 0.4)
                        cc = mix((255, 236, 200), (236, 70, 30), min(1, d * 1.6))
                        p.px((fx + i, fy + j), mul(cc, 0.6))
                        p.glow.putpixel((fx + i, fy + j), tuple(int(v * 0.7) for v in cc) + (255,))
        if style == 'pearlflame':
            def m(s, t):
                return abs(s - 0.5) < 0.5 * (t ** 0.8) * (0.7 + 0.3 * tri(s * 4 + t * 2)) and t < 0.75
            for thin in ('x', 'z'):
                if (thin == 'x') == (dims[0] == 1):
                    plane(p, f, thin, m, lambda s, t: mix((255, 220, 120), (230, 70, 20), t), glow=True)
        if style == 'futou':
            for k, r in f.items():
                p.face(r, (24, 22, 28), LIGHT[k], noise=3)
                fx, fy, fw, fh = r
                for i in range(fw):                             # its lacquer, glossed
                    if (i + fy) % 7 == 0:
                        for j in range(fh):
                            p.px((fx + i, fy + j), (44, 42, 52))
        if style == 'futou_back':
            for k, r in f.items():
                p.face(r, (20, 18, 24), LIGHT[k], noise=3)
        if style == 'wing':
            for k, r in f.items():
                fx, fy, fw, fh = r
                p.face(r, (22, 20, 26), LIGHT[k], noise=3)
                if k in ('top', 'bottom'):
                    for i in range(fw):
                        p.px((fx + i, fy), gold)
                        p.px((fx + i, fy + fh - 1), gold)
                    for j in range(fh):
                        p.px((fx, fy + j), gold)
        if style == 'ruyishaft':
            for k, r in f.items():
                p.gradient(r, (196, 232, 210), (120, 190, 150), noise=4)
        if style == 'ruyihead':
            for k, r in f.items():
                p.face(r, (150, 214, 176), LIGHT[k], noise=5)
                fx, fy, fw, fh = r
                if k in ('front', 'back'):                      # its lingzhi-cloud head, carved
                    cloud(p, fx + fw // 2 - 1, fy + fh // 2, (90, 160, 120), r=1, rect=r)
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 1), gold)
        if style == 'ironband':
            for k, r in f.items():
                p.face(r, (52, 56, 66), LIGHT[k], noise=4)
                fx, fy, fw, fh = r
                if k not in ('top', 'bottom'):
                    for i in range(fw):
                        p.px((fx + i, fy), gold)
                        p.px((fx + i, fy + fh - 1), gold)
                    for i in range(2, fw - 1, 4):
                        p.px((fx + i, fy + fh // 2), mul(gold, 0.8))
        if style == 'icicle':
            for k, r in f.items():
                p.gradient(r, (190, 220, 240), (110, 150, 190), noise=4)
                fx, fy, fw, fh = r
                if k == 'front':
                    for j in range(fh):
                        p.px((fx, fy + j), (236, 248, 255))
        if style == 'frostpearl':
            for k, r in f.items():
                fx, fy, fw, fh = r
                for j in range(fh):
                    for i in range(fw):
                        d = math.hypot((i + 0.5) / fw - 0.4, (j + 0.5) / fh - 0.4)
                        cc = mix((236, 250, 255), (80, 150, 220), min(1, d * 1.6))
                        p.px((fx + i, fy + j), mul(cc, 0.6))
                        p.glow.putpixel((fx + i, fy + j), tuple(int(v * 0.6) for v in cc) + (255,))
        if style == 'swordblade':
            for k, r in f.items():
                p.gradient(r, (214, 224, 236), (150, 160, 178), noise=3, horizontal=k in ('front', 'back'))
            fx, fy, fw, fh = f['front']
            for j in range(fh):
                p.px((fx + fw // 2, fy + j), (120, 130, 150))
        if style == 'guard':
            gold_box(p, f, gold, deep)
        if style == 'grip':
            for k, r in f.items():
                fx, fy, fw, fh = r
                for j in range(fh):
                    for i in range(fw):
                        p.px((fx + i, fy + j), (40, 30, 30) if (i + j) % 3 else (90, 30, 30))
    return paint_king


if __name__ == '__main__':
    for name, parts, painter_fn, java, seed in (
            ('shrimp_soldier', SHRIMP, paint_shrimp, 'ShrimpSoldierRenderer.java', 3),
            ('crab_general', CRAB_PARTS, paint_crab, 'CrabGeneralRenderer.java', 5),
            ('yaksha', YAKSHA, paint_yaksha, 'YakshaRenderer.java', 7)):
        p = Painter(seed)
        slots = paint(parts, p, painter_fn)
        p.save(name)
        emit(parts, slots, R + java)
        print(name, len(parts), 'parts,', len(slots), 'boxes')
    bg.UNITS = 256                        # the kings drawn finer
    bg.TEX = bg.UNITS * bg.S
    slots = None
    for king, colours in KINGS.items():
        p = Painter(11)
        slots = paint(KING, p, king_painter(colours))
        p.save(king)
    emit(KING, slots, R + 'DragonKingRenderer.java')
    print('dragon kings', len(KING), 'parts,', len(slots), 'boxes')
