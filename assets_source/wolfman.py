"""The wolfman (狼妖), a wolf yaoguai soldier of the open world. As wild_paint.py says: its model is
written into WildRenderers' wolfmanLayer() between its GENERATED markers, its skin
(textures/entity/wolfman.png) painted to the same box-UV layout, its eyes glowing (wolfman_glow.png).

A tall, lean wolf demon of the kind that serves the demon kings, standing on a wolf's digitigrade legs:
a grey pelt, darker down its back and lighter on its chest, cheeks and belly; a full wolf's head, the
muzzle long, a black nose, fangs over its lip, eyes burning pale gold under heavy brows, tall pointed
ears (a bronze ring in the left), a thick ruff about its neck and a mane down its nape. A cuirass of
black lamellae laced in red, layered pauldrons each bitten by a bronze beast's head, a red sash
knotted at its hip with a bronze beast-face buckle and its tails hanging, an armoured skirt of
lamellae over leather; studded leather bracers, greaves laced over its shins; a tattered dark-red war
cloak from its shoulders, and a small black banner (背旗) on a pole at its back, red-bordered, a
wolf's claw on it. A bushy tail. In its right hand, grounded beside it, a glaive (偃月刀) taller than
it is: a crescent blade, a bronze dragon's-head socket, a red tassel, a red-lacquered shaft banded in
bronze, an iron butt-spike.

Run from the repository root: python assets_source/wolfman.py"""
import math

import beastgen as bg
import wild_paint as w
from wild_paint import part, mirrored, aim, paint, Painter, k, mix

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

SIZE = 0.9           # WolfmanRenderer.SIZE

FUR = (132, 130, 134)
FUR_DARK = (62, 60, 66)
FUR_LIGHT = (200, 196, 190)
FUR_TIP = (226, 222, 214)
LAMELLA = (46, 44, 52)
LACE = (176, 30, 28)
BRASS = (196, 146, 62)
BRASS_DEEP = (110, 72, 30)
LEATHER = (96, 60, 38)
SASH = (156, 26, 24)
CLOAK = (82, 22, 26)
CLOAK_DEEP = (36, 10, 14)
EYE = (255, 212, 84)
NOSE = (24, 20, 24)
TOOTH = (236, 228, 204)
GUM = (96, 30, 34)
STEEL = (196, 204, 214)
LACQUER = (118, 24, 22)
CLAW = (30, 28, 30)

BODY = (0.1, 0, 0)
HEAD = (-0.1, 0, 0)
ARM, FOREARM, HAND = (-0.18, 0, 0.14), (-0.62, 0, 0), (0, 0, 0)
LARM, LFOREARM, LHAND = (0.12, 0, -0.16), (-0.35, 0, 0), (0, 0, 0)


WOLFMAN = mirrored([
    # a wolf's legs: a heavy thigh, a shin bound in a laced greave, a long ankle, a broad paw
    part('right_leg', 'root', (-2.6, 4.0, 0.8), (-0.42, 0, 0.05), [
        (-2.0, -1.0, -2.2, 4.0, 8.6, 4.4, 'thigh')]),
    part('right_shin', 'right_leg', (0, 7.4, 0.3), (1.05, 0, 0), [
        (-1.55, 0, -1.6, 3.1, 7.2, 3.2, 'shin'),
        (-1.85, 0.8, -2.1, 3.7, 4.6, 1.6, 'greave')]),
    part('right_ankle', 'right_shin', (0, 6.9, 0.1), (-0.95, 0, 0), [
        (-1.15, 0, -1.25, 2.3, 6.0, 2.5, 'ankle'),
        (-0.9, 0.6, 0.6, 1.8, 3.2, 1.4, 'fetlock')]),
    part('right_paw', 'right_ankle', (0, 5.7, 0), (0.32, 0, 0), [
        (-1.75, -0.5, -3.4, 3.5, 1.9, 4.4, 'paw'),
        (-1.75, 0.1, -4.4, 0.8, 1.2, 1.2, 'toe'),
        (-0.8, 0.1, -4.7, 0.8, 1.2, 1.4, 'toe'),
        (0.05, 0.1, -4.7, 0.8, 1.2, 1.4, 'toe'),
        (0.95, 0.1, -4.4, 0.8, 1.2, 1.2, 'toe')]),

    # the body from the hips up, a little forward; its pivot the hips
    part('body', 'root', (0, 4.0, 0.8), BODY, [
        (-3.7, -2.7, -2.5, 7.4, 3.8, 5.0, 'pelvis'),
        (-3.3, -7.4, -2.3, 6.6, 4.9, 4.6, 'waist'),
        (-5.0, -15.2, -3.3, 10.0, 8.4, 6.6, 'cuirass'),
        (-4.4, -8.0, -2.9, 8.8, 1.8, 5.8, 'cuirass_low'),
        (-4.1, -6.6, -3.0, 8.2, 2.1, 6.0, 'sash'),
        (-1.7, -7.1, -3.55, 3.4, 3.0, 0.8, 'buckle'),
        (-4.5, -17.6, -2.7, 9.0, 3.2, 5.6, 'ruff'),
        (-2.6, -17.0, 1.6, 5.2, 7.0, 2.0, 'mane'),
    ]),
    part('right_sash_tail', 'body', (-3.2, -5.2, -2.6), (-0.06, 0.2, 0.12), [(-0.85, 0, -0.25, 1.7, 8.0, 0.5, 'sashtail')]),
    part('sash_tail_2', 'body', (-2.2, -5.0, -2.9), (-0.1, 0.1, 0.02), [(-0.7, 0, -0.25, 1.4, 6.4, 0.5, 'sashtail_short')]),
    # the armoured skirt: lamellae over leather, before, at the sides and behind
    part('right_tasset', 'body', (-1.9, -2.4, -2.9), (-0.1, 0.06, 0.05), [(-1.9, 0, -0.45, 3.8, 7.6, 0.9, 'tasset')]),
    part('right_tasset_side', 'body', (-3.9, -2.4, 0.0), (0, 0, 0.12), [(-0.45, 0, -2.4, 0.9, 7.0, 4.8, 'tasset_side')]),
    part('tasset_back', 'body', (0, -2.4, 2.6), (0.12, 0, 0), [(-3.6, 0, -0.45, 7.2, 7.4, 0.9, 'tasset_back')]),
    # the war cloak, tattered, hanging from its shoulders behind it
    part('cloak', 'body', (0, -15.6, 3.4), (0.14, 0, 0), [(-5.2, 0, -0.3, 10.4, 15.5, 0.6, 'cloak')]),
    # the banner at its back, on its pole
    part('banner_pole', 'body', (2.6, -9.0, 3.9), (-0.12, 0, 0.1), [
        (-0.35, -17.0, -0.35, 0.7, 18.0, 0.7, 'bannerpole'),
        (-0.6, -18.0, -0.6, 1.2, 1.2, 1.2, 'bannerfinial')]),
    part('banner', 'banner_pole', (0, -16.6, 0), (0, 0, 0), [(0.3, 0, -0.1, 5.2, 9.0, 0.2, 'banner')]),
    # the tail, bushy, hanging and curling out behind
    part('tail', 'body', (0, -2.0, 2.7), (-0.95, 0, 0), [(-1.4, -1.4, 0, 2.8, 2.8, 4.8, 'tail1')]),
    part('tail_mid', 'tail', (0, 0, 4.4), (-0.3, 0, 0), [(-1.8, -1.8, 0, 3.6, 3.6, 5.0, 'tail2')]),
    part('tail_tip', 'tail_mid', (0, 0, 4.6), (0.35, 0, 0), [(-1.4, -1.4, 0, 2.8, 2.8, 3.6, 'tail3')]),

    # the wolf's head
    part('head', 'body', (0, -17.6, -1.0), HEAD, [
        (-3.2, -6.2, -3.4, 6.4, 5.8, 6.2, 'skull'),
        (-1.75, -3.9, -9.9, 3.5, 2.7, 6.8, 'muzzle'),
        (-1.35, -4.6, -9.2, 2.7, 0.9, 6.0, 'bridge'),
        (-0.95, -4.5, -10.4, 1.9, 1.4, 0.8, 'nose'),
        (-3.4, -6.3, 1.0, 6.8, 6.2, 2.0, 'nape'),
    ]),
    part('jaw', 'head', (0, -1.15, -3.1), (0.03, 0, 0), [
        (-1.6, 0, -6.6, 3.2, 1.4, 6.6, 'jaw'),
        (-1.3, 1.2, -4.0, 2.6, 1.6, 4.4, 'throat')]),
    part('right_fang', 'head', (-1.15, -1.2, -8.6), (0, 0, 0), [(-0.3, 0, -0.3, 0.6, 1.5, 0.6, 'fang')]),
    part('right_cheek', 'head', (-2.9, -2.6, -1.4), (0, 0.55, 0.3), [(-1.8, -1.6, -1.0, 2.0, 3.2, 3.4, 'cheekfur')]),
    part('right_eye', 'head', (-1.9, -4.55, -3.5), (0, 0.32, -0.12), [(-1.05, -0.5, -0.3, 2.1, 1.05, 0.45, 'eye')]),
    part('right_brow', 'head', (-1.75, -5.15, -3.7), (0.1, 0.25, 0.22), [(-1.5, -0.55, -0.8, 3.0, 1.1, 1.6, 'brow')]),
    part('right_ear', 'head', (-2.1, -5.9, -0.4), (-0.2, 0.15, -0.22), [(-1.4, -5.0, -0.65, 2.8, 5.2, 1.3, 'ear')]),

    # the arms: fur under layered pauldrons, studded bracers, clawed hands
    part('right_arm', 'body', (-5.7, -13.8, 0), ARM, [
        (-1.8, -1.0, -1.8, 3.6, 7.2, 3.6, 'upperarm'),
        (-3.1, -2.4, -2.7, 4.6, 2.4, 5.4, 'pauldron1'),
        (-3.5, -0.4, -2.9, 4.6, 2.6, 5.8, 'pauldron2'),
        (-3.9, 1.8, -3.0, 4.6, 2.4, 6.0, 'pauldron3'),
        (-4.3, -0.4, -1.3, 0.8, 2.8, 2.6, 'beasthead')]),
    part('right_forearm', 'right_arm', (-0.2, 6.6, 0), FOREARM, [
        (-1.5, 0, -1.5, 3.0, 6.6, 3.0, 'forearm'),
        (-1.85, 1.4, -1.85, 3.7, 4.4, 3.7, 'bracer')]),
    part('right_hand', 'right_forearm', (0, 6.5, 0), HAND, [
        (-1.55, 0, -1.65, 3.1, 2.4, 3.3, 'hand'),
        (-1.5, 2.1, -1.6, 0.7, 1.6, 0.7, 'claw'),
        (-0.6, 2.1, -1.6, 0.7, 1.8, 0.7, 'claw'),
        (0.3, 2.1, -1.6, 0.7, 1.7, 0.7, 'claw'),
        (-1.7, 0.6, -2.2, 0.8, 1.5, 0.8, 'claw')]),
])

# an earring in the left ear only
WOLFMAN += [part('earring', 'left_ear', (1.0, -2.0, 0), (0, 0, 0), [(-0.25, 0, -0.75, 0.5, 1.6, 1.5, 'ring')])]

# the glaive, grounded at its right side, the blade high over its head and turned forward
GLAIVE_AIM = aim([BODY, ARM, FOREARM, HAND], (0.06, 0.75, -0.06))
WOLFMAN += [
    part('glaive', 'right_hand', (0, 1.2, 0), GLAIVE_AIM, [
        (-0.5, -26.0, -0.5, 1.0, 36.0, 1.0, 'shaft'),
        (-0.65, -12.0, -0.65, 1.3, 1.0, 1.3, 'band'),
        (-0.65, -2.0, -0.65, 1.3, 1.0, 1.3, 'band'),
        (-0.65, 6.0, -0.65, 1.3, 1.0, 1.3, 'band'),
        (-0.6, 10.0, -0.6, 1.2, 2.4, 1.2, 'butt'),
        (-0.9, -28.4, -1.2, 1.8, 2.6, 2.4, 'socket'),
        (-0.3, -29.2, -0.3, 0.6, 0.9, 0.6, 'tassel_knot'),
        (-0.4, -39.0, -5.4, 0.8, 11.0, 5.4, 'blade'),
        (-0.3, -32.0, 0.9, 0.6, 2.4, 1.8, 'backspike')]),
    part('glaive_tassel', 'glaive', (0, -27.4, 1.1), (0.2, 0, 0), [(-0.45, 0, -0.45, 0.9, 4.0, 0.9, 'tassel')]),
]
WOLFMAN = w.settle(WOLFMAN, ('right_paw', 'left_paw'))


# ── The skin ─────────────────────────────────────────────────────────────────

def pelt(p, f, base=FUR, dark=FUR_DARK, under=FUR_LIGHT, faces=None, density=0.45, back_dark=True):
    """Grey fur: strands down every side, darker down its back, lighter beneath."""
    for s in faces or f:
        b = base
        if s == 'back' and back_dark:
            b = mix(base, dark, 0.45)
        if s == 'bottom':
            b = under
        w.strands(p, f[s], k(b, w.LIGHT[s]), k(dark, w.LIGHT[s]), k(FUR_TIP if s != 'back' else base, w.LIGHT[s]), density=density, lean=0.2)


def lamellae(p, f, plate=LAMELLA, lace=LACE, faces=None, rows=3, cols=2, rim_c=None):
    for s in faces or w.SIDES:
        w.lamellar(p, f[s], k(plate, 1.0), lace, rows=rows, cols=cols, light=w.LIGHT[s], rim=rim_c or k(plate, 2.2))


def claws(p, f):
    for s in w.SIDES + ('bottom',):
        fx, fy, fw, fh = f[s]
        for i, j, xy in w.each(p, f[s]):
            if j >= fh * 0.4 or s == 'bottom':
                p.px(xy, k(mix(CLAW, (150, 146, 140), 0.35 * (j < fh * 0.55)), w.LIGHT[s]))
    for s in w.SIDES:
        w.point(p, f[s], down=True, keep=0.35, sharp=0.6)


def beast_face(p, rect, base=BRASS, deep=BRASS_DEEP, glow_eyes=False):
    """A bronze beast's face (吞口): brows, round eyes, a snarling mouth, studs about it."""
    fx, fy, fw, fh = rect
    for i, j, xy in w.each(p, rect):
        p.px(xy, [v + p.rnd.randint(-6, 6) for v in base])
    for i in range(fw):
        p.px((fx + i, fy), deep)
        p.px((fx + i, fy + fh - 1), deep)
    ey = max(1, fh // 3)
    for ex in (max(0, fw // 4 - 1), min(fw - 2, fw - fw // 4 - 1)):
        p.px((fx + ex, fy + ey - 1), deep)
        p.px((fx + ex + 1, fy + ey - 1), deep)
        p.px((fx + ex, fy + ey), (40, 16, 10))
        p.px((fx + ex + 1, fy + ey), (220, 60, 30))
    for i in range(max(1, fw // 4), fw - max(1, fw // 4)):
        p.px((fx + i, fy + fh - 3), (40, 16, 10))
        if i % 2 == 0:
            p.px((fx + i, fy + fh - 2), (230, 220, 190))
    p.px((fx + fw // 2, fy + ey + 1), deep)


def paint_wolfman(p, style, f, dims):
    S = w.SIDES

    if style in ('thigh', 'ankle', 'upperarm', 'forearm', 'waist'):
        pelt(p, f)

    if style == 'shin':
        pelt(p, f)

    if style == 'fetlock':                                        # a tuft at the back of its ankle
        pelt(p, f, base=FUR_LIGHT)
        for s in S:
            w.tufts(p, f[s], depth=3, every=2)

    if style in ('paw', 'hand'):
        pelt(p, f, base=mix(FUR, FUR_DARK, 0.3))
        if style == 'paw':
            w.flat(p, f, (40, 34, 36), faces=('bottom',))

    if style in ('toe', 'claw'):
        pelt(p, f, base=mix(FUR, FUR_DARK, 0.3))
        claws(p, f)

    if style == 'greave':                                         # an iron greave laced over the shin
        w.iron(p, f, (70, 68, 76), rivets=3, dents=0.2)
        for s in ('front',):
            fx, fy, fw, fh = f[s]
            for j in range(1, fh - 1, 2):
                p.px((fx + 1, fy + j), LACE)
                p.px((fx + fw - 2, fy + j), LACE)
            w.vline(p, f[s], fw // 2, k((70, 68, 76), 1.7), 1, fh - 1)

    if style == 'pelvis':
        w.leather(p, f, LEATHER, stitch=(160, 120, 80))

    if style == 'cuirass':                                        # black lamellae laced in red; a bronze mirror (护心镜) on its breast
        lamellae(p, f)
        w.leather(p, f, LEATHER, faces=('top',))
        fx, fy, fw, fh = f['front']                               # 20x17
        cx, cy = fx + fw / 2, fy + fh * 0.42
        w.disc(p, cx, cy, 4.6, BRASS, rim=BRASS_DEEP)
        w.disc(p, cx, cy, 2.6, k(BRASS, 1.15), rim=k(BRASS, 0.8))
        w.disc(p, cx, cy, 1.0, (240, 220, 160))
        for a in range(8):                                        # its rim of studs
            x, y = cx + 3.6 * math.cos(a * math.pi / 4), cy + 3.6 * math.sin(a * math.pi / 4)
            p.px((x, y), BRASS_DEEP)
        for s in ('front', 'back'):                               # straps over its shoulders
            fx, fy, fw, fh = f[s]
            for x in (3, fw - 5):
                for j in range(0, 4):
                    for t in range(2):
                        p.px((fx + x + t, fy + j), k(LEATHER, 1.1))
                w.rivet(p, fx + x, fy + 3, BRASS)

    if style == 'cuirass_low':
        lamellae(p, f, rows=2)

    if style == 'sash':                                           # the red sash, wound twice
        w.cloth(p, f, SASH, k(SASH, 0.55), fold=3, coarse=False)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh // 2), k(SASH, 0.6 * w.LIGHT[s]))

    if style == 'buckle':
        w.flat(p, f, BRASS_DEEP)
        beast_face(p, f['front'])

    if style in ('sashtail', 'sashtail_short'):
        w.cloth(p, f, SASH, k(SASH, 0.55), fold=3, coarse=False)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for i in range(fw):                                   # a gold-fringed end
                p.px((fx + i, fy + fh - 1), BRASS if i % 2 == 0 else k(SASH, 0.5))
                p.px((fx + i, fy + fh - 2), BRASS)

    if style in ('tasset', 'tasset_side', 'tasset_back'):         # lamellae over leather, a leather hem
        lamellae(p, f, rows=3)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), k(LEATHER, w.LIGHT[s]))
                p.px((fx + i, fy + fh - 2), k(LEATHER, 0.8 * w.LIGHT[s]))
            for i in range(fw):
                p.px((fx + i, fy), k(BRASS_DEEP, w.LIGHT[s]))

    if style == 'ruff':                                           # a thick ruff of pale fur about its neck
        pelt(p, f, base=mix(FUR, FUR_LIGHT, 0.5), density=0.35, back_dark=False)
        for s in S:
            w.tufts(p, f[s], depth=4, every=3)

    if style == 'mane':
        pelt(p, f, base=mix(FUR, FUR_DARK, 0.4), density=0.5)
        for s in S:
            w.tufts(p, f[s], depth=4, every=3)

    if style == 'cloak':                                          # the war cloak: dark red, a black border, tattered
        for s in ('front', 'back'):
            w.weave(p, f[s], CLOAK, CLOAK_DEEP, light=w.LIGHT[s], fold=5, fall=0.35)
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in (0, 1, fw - 2, fw - 1):
                    p.px((fx + i, fy + j), k((22, 18, 22), w.LIGHT[s]))
            for i in range(fw):
                p.px((fx + i, fy), (22, 18, 22))
                p.px((fx + i, fy + 1), BRASS_DEEP)
            w.ragged(p, f[s], teeth=3, depth=7)
            w.tear_holes(p, f[s], n=3, size_=2)
        w.flat(p, f, CLOAK_DEEP, faces=('right', 'left', 'top', 'bottom'))
        fx, fy, fw, fh = f['back']                                # a wolf's head in black on its back
        w.glyph(p, fx + fw // 2 - 5, fy + 8, [
            '#........#',
            '##......##',
            '###....###',
            '##########',
            '#.##..##.#',
            '##########',
            '.########.',
            '..######..',
            '...####...',
            '....##....'], (24, 20, 24))

    if style == 'bannerpole':
        w.wood(p, f, LACQUER, along='y')
    if style == 'bannerfinial':
        w.iron(p, f, BRASS, rivets=0)
    if style == 'banner':                                         # a black pennant, red-bordered, a white wolf's claw on it
        w.cut_all(p, f, ('right', 'left', 'top', 'bottom'))
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                t = i / max(1, fw - 1) if s == 'front' else 1 - i / max(1, fw - 1)  # 0 at the pole .. 1 at its tail
                if j > fh * (1 - 0.55 * t) and t > 0.15:          # a swallow-tailed foot, cut on the slant
                    p.clear(xy)
                    continue
                edge = i in (0, fw - 1) or j in (0, fh - 1) or j > fh * (1 - 0.55 * t) - 1.5
                p.px(xy, [v + p.rnd.randint(-4, 4) for v in ((170, 26, 24) if edge else (26, 22, 28))])
            for n in range(3):                                    # three claw marks
                for j in range(fh // 4, fh // 4 + 4):
                    x = fx + 3 + n * 2 + (j - fh // 4) // 2
                    p.px((x if s == 'front' else fx + fw - 1 - (x - fx), fy + j), (226, 222, 214))

    if style in ('tail1', 'tail2', 'tail3'):
        pelt(p, f, base=mix(FUR, FUR_DARK, 0.25) if style != 'tail3' else FUR_LIGHT, density=0.5)
        if style == 'tail3':
            for s in S:
                w.tufts(p, f[s], depth=2, every=2)

    if style == 'skull':                                          # its head: fur, darker on the crown, pale toward the cheeks
        pelt(p, f, density=0.35)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):                    # the dark mask between its eyes, pale below them
            if abs(i - fw / 2 + 0.5) < 2 and j < fh * 0.6:
                p.px(xy, k(p.get(xy), 0.7))
            elif j > fh * 0.65:
                p.px(xy, mix(p.get(xy), FUR_LIGHT, 0.5))

    if style == 'nape':
        pelt(p, f, base=mix(FUR, FUR_DARK, 0.3))
        for s in ('back', 'right', 'left'):
            w.tufts(p, f[s], depth=3, every=3)

    if style in ('muzzle', 'bridge'):                             # the long muzzle: grey above, pale along its lips, teeth under its lip
        pelt(p, f, density=0.3)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if j >= fh - 3:
                    p.px(xy, mix(p.get(xy), FUR_LIGHT, 0.6))
            if style == 'muzzle':
                for i in range(fw):                               # its lip: a black line, a tooth showing here and there
                    p.px((fx + i, fy + fh - 1), (30, 24, 28))
                    if i in (fw // 3, fw // 3 + 1):
                        p.px((fx + i, fy + fh - 1), TOOTH)
        if style == 'muzzle':                                     # under it: its palate's dark, rimmed with teeth (seen when it howls)
            w.flat(p, f, (70, 26, 32), faces=('bottom',))
            fx, fy, fw, fh = f['bottom']
            for i, j, xy in w.each(p, f['bottom']):
                if i in (0, fw - 1):
                    p.px(xy, TOOTH if j % 2 == 0 else (40, 20, 24))
                elif j % 3 == 1:
                    p.px(xy, (110, 46, 52))
            fx, fy, fw, fh = f['front']                           # its end: pale fur round the nose, the lip's black line
            for i, j, xy in w.each(p, f['front']):
                p.px(xy, mix(p.get(xy), FUR_LIGHT, 0.45))
                if j == fh - 1:
                    p.px(xy, (30, 24, 28))

    if style == 'nose':                                           # a black nose, wet, its nostrils
        w.flat(p, f, NOSE)
        for s in ('front', 'top'):
            fx, fy, fw, fh = f[s]
            p.clear((fx, fy))
            p.clear((fx + fw - 1, fy))
        fx, fy, fw, fh = f['front']
        p.px((fx, fy + fh - 1), (4, 2, 4))
        p.px((fx + fw - 1, fy + fh - 1), (4, 2, 4))
        p.px((fx + 1, fy), (130, 126, 136))

    if style in ('jaw', 'throat'):
        pelt(p, f, base=FUR_LIGHT if style == 'throat' else FUR, density=0.3)
        if style == 'jaw':
            fx, fy, fw, fh = f['top']
            for i, j, xy in w.each(p, f['top']):
                p.px(xy, GUM if 0 < i < fw - 1 else (TOOTH if j % 2 == 0 else (40, 20, 22)))
            for s in ('right', 'left', 'front'):                  # its lower lip: black
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), (30, 24, 28))

    if style == 'fang':
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                p.px(xy, k(mix(TOOTH, (190, 176, 140), j / max(1, fh)), w.LIGHT[s]))
        for s in S:
            w.point(p, f[s], down=True, keep=0.3, sharp=0.6)

    if style == 'cheekfur':                                       # pale fur sweeping back off its cheeks
        pelt(p, f, base=FUR_LIGHT, density=0.3, back_dark=False)
        for s in S:
            w.tufts(p, f[s], depth=3, every=2)

    if style == 'eye':                                            # pale-gold eyes burning under its brows
        pelt(p, f)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            p.lit(xy, mix(EYE, (255, 250, 210), 0.4 if j == 0 else 0))
        p.px((fx + fw // 2, fy + fh - 1), (20, 12, 8))
        p.px((fx + fw // 2, fy), (20, 12, 8))
        p.glow.putpixel((fx + fw // 2, fy + fh - 1), (0, 0, 0, 0))
        p.glow.putpixel((fx + fw // 2, fy), (0, 0, 0, 0))
        for i in range(fw):                                       # a black rim about it
            p.px((fx + i, fy - 1), (20, 16, 18)) if False else None

    if style == 'brow':
        pelt(p, f, base=mix(FUR, FUR_DARK, 0.5), density=0.6)
        w.flat(p, f, (30, 26, 30), faces=('bottom',))

    if style == 'ear':                                            # tall pointed ears, dark-rimmed, pale and pink within
        pelt(p, f, base=mix(FUR, FUR_DARK, 0.3))
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            if 0 < i < fw - 1 and j > fh * 0.25:
                p.px(xy, mix(FUR_LIGHT, (170, 120, 120), 0.4 * (j / fh)))
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), k(FUR_DARK, 0.6))
            w.point(p, f[s], down=False, keep=0.5, sharp=0.45)
        w.cut_all(p, f, ('top',))
        for s in ('right', 'left'):                               # its thin edges, cut clean toward the tip
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if j < fh * 0.45:
                    p.clear(xy)

    if style == 'ring':
        w.iron(p, f, BRASS, rivets=0, rim=False, dents=0)

    if style in ('pauldron1', 'pauldron2', 'pauldron3'):         # layered lamellar pauldrons, bronze-rimmed
        lamellae(p, f, faces=S + ('top',), rows=3)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), BRASS)
                p.px((fx + i, fy + fh - 2), BRASS_DEEP)

    if style == 'beasthead':                                      # the bronze beast's head biting at its shoulder
        w.iron(p, f, BRASS, rivets=0, rim=False, dents=0.1)
        beast_face(p, f['right'])

    if style == 'bracer':                                         # leather bracers, bronze-studded
        w.leather(p, f, LEATHER, stitch=(170, 130, 90))
        for s in S:
            fx, fy, fw, fh = f[s]
            for j in range(2, fh - 1, 3):
                for i in range(1, fw - 1, 3):
                    p.px((fx + i, fy + j), BRASS)
                    p.px((fx + i, fy + j + 1), BRASS_DEEP)
            w.hline(p, f[s], 0, BRASS_DEEP)
            w.hline(p, f[s], fh - 1, BRASS_DEEP)

    if style == 'shaft':                                          # the glaive's shaft, red lacquer, wound for the grip
        w.wood(p, f, LACQUER, along='y')
        for s in S:
            fx, fy, fw, fh = f[s]
            for j in range(int(fh * 0.62), int(fh * 0.78)):
                if j % 2 == 0:
                    w.hline(p, f[s], j, k((30, 22, 20), w.LIGHT[s]))

    if style in ('band', 'socket', 'tassel_knot'):
        w.iron(p, f, BRASS, rivets=0, dents=0.1)
        if style == 'socket':                                     # a dragon's head biting the blade's root
            fx, fy, fw, fh = f['right']
            beast_face(p, f['right'])
            beast_face(p, f['left'])

    if style == 'butt':
        w.iron(p, f, (70, 68, 76), rivets=0)
        for s in S:
            w.point(p, f[s], down=True, keep=0.3, sharp=0.4)

    if style == 'blade':                                          # the crescent blade: a long curved edge before, a notched back
        w.iron(p, f, STEEL, rivets=0, rim=False, dents=0.1)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]                                 # its side: (depth 5.4 wide x 11 tall) the crescent
            for i, j, xy in w.each(p, f[s]):
                u = i / max(1, fw - 1)                            # across: 0 .. 1
                if s == 'right':
                    u = 1 - u                                     # 0 at the shaft .. 1 at the edge (the box's front)
                v = j / max(1, fh - 1)                            # 0 at the tip .. 1 at the root
                edge = 0.35 + 0.65 * math.sin(math.pi * min(1, v * 1.05)) ** 0.7   # the curve of its edge
                back = 0.22 * (1 - v) ** 2
                if u > edge or u < back or (v < 0.08 and u < 0.6) or (v > 0.93 and u > 0.45):
                    p.clear(xy)
                    continue
                c = k(STEEL, 0.85 + 0.25 * (1 - v))
                if u > edge - 0.12:
                    c = (236, 240, 248)                           # the bright edge
                elif u < back + 0.12 or (0.4 < u < 0.48 and 0.2 < v < 0.85):
                    c = k(STEEL, 0.6)                             # the back's ridge, and a blood-groove
                p.px(xy, [x * w.LIGHT[s] + p.rnd.randint(-3, 3) for x in c])
            for j in range(int(fh * 0.55), int(fh * 0.8)):        # a dragon traced on it in gold
                x = int(fw * (0.25 + 0.12 * math.sin(j * 0.9)))
                p.px((fx + (x if s == 'left' else fw - 1 - x), fy + j), BRASS)

    if style == 'backspike':
        w.iron(p, f, STEEL, rivets=0, rim=False)
        for s in ('right', 'left'):
            w.point(p, f[s], down=False, keep=0.3, sharp=0.5)

    if style == 'tassel':
        for s in f:
            w.strands(p, f[s], k(LACE, w.LIGHT[s] * 1.1), k(LACE, 0.6), k((240, 90, 70), w.LIGHT[s]))
        for s in S:
            w.hline(p, f[s], 0, BRASS)


if __name__ == '__main__':
    p = Painter(61)
    slots = paint(WOLFMAN, p, paint_wolfman)
    p.save('wolfman')
    w.emit(WOLFMAN, slots, w.JAVA, 'wolfman')
    w.report('wolfman', WOLFMAN, SIZE, slots)
