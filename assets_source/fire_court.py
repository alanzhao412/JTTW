"""The Flaming Mountains' creatures: the Ember Imp, the Furnace-Brick Brute, Red Boy's Six Strong
Generals (one model, six skins), Red Boy himself and his fire carts. As sea_court.py: each one's
model and skin come from one list of parts (beastgen): the model is written into its renderer's
layer() between its GENERATED markers, the skin painted to the same box-UV layout at twice the
model's resolution, with a second, glowing skin (eyes, embers, flames) drawn full-bright.

Run from the repository root: python assets_source/fire_court.py"""
import random

import beastgen as bg
from beastgen import Painter, paint, emit

bg.UNITS = 128
bg.TEX = bg.UNITS * bg.S

R = 'src/main/java/com/jttw/engine/client/renderer/'
DARK = (22, 14, 14)
GOLD = (226, 178, 62)
GOLD_DEEP = (168, 118, 40)
IVORY = (240, 232, 210)
EMBER = (255, 150, 40)
EMBER_HOT = (255, 226, 120)


def part(name, parent, pivot, rot, boxes, mirror=False):
    return (name, parent, pivot, rot, boxes, mirror)


def mirrored(parts):
    """Each 'right_' part again as 'left_', mirrored across x."""
    out = []
    for name, parent, (px, py, pz), (rx, ry, rz), boxes, mirror in parts:
        out.append((name, parent, (px, py, pz), (rx, ry, rz), boxes, mirror))
        if name.startswith('right_'):
            lb = [(-x - w, y, z, w, h, d, s) for (x, y, z, w, h, d, s) in boxes]
            out.append((name.replace('right_', 'left_'), parent.replace('right_', 'left_'), (-px, py, pz), (rx, -ry, -rz), lb, True))
    return out


def cracks(p, rect, colour, n=3, hot=None, glow=True):
    """Glowing cracks wandering over a face."""
    fx, fy, fw, fh = rect
    for _ in range(n):
        x, y = fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)
        for _ in range(p.rnd.randint(fw // 3 + 2, fw + 4)):
            if fx <= x < fx + fw and fy <= y < fy + fh:
                p.px((x, y), hot if hot and p.rnd.random() < 0.3 else colour, glow=glow)
            x += p.rnd.choice((-1, 0, 1))
            y += p.rnd.choice((0, 1, 1))


def flame_face(p, rect, base=(240, 70, 30), tip=EMBER_HOT, upward=True, glow=True, density=1.0):
    """Tongues of flame up a face: hot at the root, ragged and clear at the top."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        reach = int(fh * (0.45 + 0.55 * p.rnd.random() * density))
        for j in range(fh):
            up = (fh - 1 - j) if upward else j
            if up > reach:
                continue
            t = up / max(1, reach)
            c = [a + (b - a) * (1 - t) for a, b in zip(base, tip)] if t < 0.5 else [a * (1.1 - t * 0.4) for a in base]
            p.px((fx + i, fy + j), c, glow=glow)


def eye(p, x, y, w, h, colour):
    for i in range(w):
        for j in range(h):
            p.px((x + i, y + j), colour, glow=True)


# ── Ember Imp ────────────────────────────────────────────────────────────────
HIDE = (150, 44, 26)
HIDE_DARK = (92, 24, 18)

IMP = mirrored([
    part('body', 'root', (0, 9, 0), (0, 0, 0), [(-3, 0, -2, 6, 7, 4, 'hide')]),
    part('head', 'root', (0, 9, -0.5), (0, 0, 0), [
        (-3.5, -7, -3.5, 7, 7, 7, 'face'),
        (-3.2, -8.5, -1.2, 1.2, 2, 1.2, 'horn'),
        (2, -8.5, -1.2, 1.2, 2, 1.2, 'horn'),
    ]),
    part('crest', 'head', (0, -7, 0), (0, 0, 0), [(-1.5, -4, -3, 3, 4, 7, 'flame'), (-0.75, -6.5, -1.5, 1.5, 2.5, 4, 'flame')]),
    part('right_arm', 'root', (-3.8, 10, 0), (0, 0, 0), [(-1.5, -1, -1.25, 2.5, 8, 2.5, 'limb')]),
    part('right_leg', 'root', (-1.6, 16, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3, 8, 3, 'limb')]),
    part('tail', 'root', (0, 15, 2), (0.9, 0, 0), [(-0.6, -0.6, 0, 1.2, 1.2, 7, 'limb'), (-1, -1, 6, 2, 2, 3, 'flame')]),
])


def paint_imp(p, style, f, dims):
    if style in ('hide', 'face', 'limb'):
        p.shaded(f, HIDE, belly=HIDE_DARK)
        for k in ('front', 'back', 'right', 'left', 'top'):
            cracks(p, f[k], EMBER, n=2 if style == 'limb' else 3, hot=EMBER_HOT)
    if style == 'face':
        fx, fy, fw, fh = f['front']                      # 14x14: a brow, two slanted yellow eyes, a grin of fangs
        for i in range(1, fw - 1):
            p.px((fx + i, fy + 4), HIDE_DARK)
        for ex, d in ((2, 1), (fw - 6, -1)):
            for i in range(4):
                p.px((fx + ex + i, fy + 5 + (i if d > 0 else 3 - i) // 2), (255, 230, 90), glow=True)
                p.px((fx + ex + i, fy + 6 + (i if d > 0 else 3 - i) // 2), (255, 190, 40), glow=True)
        for i in range(3, fw - 3):
            p.px((fx + i, fy + 10), DARK)
            p.px((fx + i, fy + 11), (200, 60, 20), glow=True)
        for i in (4, 6, fw - 7, fw - 5):
            p.px((fx + i, fy + 10), IVORY)
    if style == 'horn':
        for r in f.values():
            p.gradient(r, (40, 30, 28), (120, 90, 70))
    if style == 'flame':
        for k, r in f.items():
            flame_face(p, r, upward=k not in ('top', 'bottom'))


# ── Furnace-Brick Brute ──────────────────────────────────────────────────────
BRICK = (170, 84, 48)
MORTAR = (70, 40, 30)

BRUTE = mirrored([
    part('body', 'root', (0, 6, 0), (0, 0, 0), [
        (-9, -20, -5.5, 18, 20, 11, 'brick'),
        (-4.5, -13, -6.1, 9, 7, 0.6, 'grate'),
        (-2.5, -27, 1, 5, 7, 5, 'chimney'),
    ]),
    part('head', 'root', (0, -14, -3), (0, 0, 0), [(-4, -6, -4, 8, 7, 8, 'head')]),
    part('right_arm', 'root', (-11, -11, 0), (0, 0, 0), [(-3.5, -2, -3.5, 7, 22, 7, 'brick'), (-4, 18, -4, 8, 7, 8, 'fist')]),
    part('right_leg', 'root', (-4.5, 6, 0), (0, 0, 0), [(-3.5, 0, -3.5, 7, 18, 7, 'brick')]),
])


def bricks(p, rect, glow_share=0.08):
    """Courses of fire-brick: each brick its own shade, mortar between, some mortar glowing with the furnace's fire."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        row = j // 5
        off = 5 if row % 2 else 0
        for i in range(fw):
            mortar = j % 5 == 4 or (i + off) % 10 == 9
            if mortar:
                hot = p.rnd.random() < glow_share * 4 and (row * 7 + (i + off) // 10) % 3 == 0
                p.px((fx + i, fy + j), EMBER if hot else MORTAR, glow=hot)
            else:
                k = 0.85 + 0.3 * ((row * 13 + (i + off) // 10 * 7) % 5) / 5
                n = p.rnd.randint(-8, 8)
                p.px((fx + i, fy + j), [c * k + n for c in BRICK])


def paint_brute(p, style, f, dims):
    if style in ('brick', 'fist', 'head', 'chimney'):
        for k, r in f.items():
            bricks(p, r, 0.1 if style != 'head' else 0.05)
    if style == 'brick' and dims[1] == 20:
        fx, fy, fw, fh = f['front']                      # ☲ li, fire's trigram, cut glowing into the chest
        cx, cy = fx + fw // 2, fy + 6
        for row, broken in ((0, False), (3, True), (6, False)):
            for i in range(-6, 7):
                if broken and abs(i) < 2:
                    continue
                p.px((cx + i, cy + row), EMBER_HOT, glow=True)
                p.px((cx + i, cy + row + 1), EMBER, glow=True)
    if style == 'head':
        fx, fy, fw, fh = f['front']
        for ex in (3, fw - 6):
            eye(p, fx + ex, fy + 6, 3, 2, (255, 210, 70))
        for i in range(4, fw - 4):
            p.px((fx + i, fy + 10), (255, 120, 30), glow=True)
    if style == 'grate':
        for r in f.values():
            fx, fy, fw, fh = r
            for j in range(fh):
                for i in range(fw):
                    bar = i % 3 == 0
                    c = (40, 34, 34) if bar else [a * (0.7 + 0.3 * j / max(1, fh)) for a in (255, 140, 40)]
                    p.px((fx + i, fy + j), c, glow=not bar)
    if style == 'chimney':
        fx, fy, fw, fh = f['top']
        for j in range(2, fh - 2):
            for i in range(2, fw - 2):
                p.px((fx + i, fy + j), (255, 120, 30), glow=True)


# ── Red Boy's Six Strong Generals ────────────────────────────────────────────
GENERAL = mirrored([
    part('body', 'root', (0, 0, 0), (0, 0, 0), [
        (-5, -2, -3, 10, 14, 6, 'cuirass'),
        (-5.5, 8, -3.4, 11, 1.5, 6.8, 'belt'),
        (-5.5, 9.5, -3.6, 11, 6.5, 7.2, 'tasset'),
        (-2, -0.5, -3.6, 4, 4, 0.6, 'mirror'),
    ]),
    part('head', 'root', (0, -2, 0), (0, 0, 0), [
        (-4, -8, -4, 8, 8, 8, 'face'),
        (-4.6, -9.5, -4.6, 9.2, 4.5, 9.2, 'helm'),
        (-1.5, -1.4, -4.4, 1, 1.6, 0.6, 'tusk'),
        (0.5, -1.4, -4.4, 1, 1.6, 0.6, 'tusk'),
    ]),
    part('crest', 'head', (0, -9.5, 0), (0, 0, 0), [(-0.75, -4, -3, 1.5, 4, 6, 'crest')]),
    part('right_horn', 'head', (-3.5, -8.5, -1), (0, 0, -0.5), [(-0.75, -5, -0.75, 1.5, 5, 1.5, 'horn')]),
    part('right_arm', 'root', (-6.5, 0, 0), (0, 0, 0), [
        (-2.5, -2, -2, 4, 13, 4, 'arm'),
        (-3.3, -3, -2.8, 5.6, 4.5, 5.6, 'pauldron'),
        (-2.9, 8, -2.4, 4.8, 2.5, 4.8, 'bracer'),
    ]),
    part('glaive', 'right_arm', (-0.5, 10, 0), (1.5708, 0, 0), [
        (-0.5, -22, -0.5, 1, 34, 1, 'shaft'),
        (-0.6, -30, -1.8, 1.2, 8, 3.2, 'blade'),
        (-1.5, -22.5, -1.2, 3, 1, 2.4, 'guard'),
    ]),
    part('right_leg', 'root', (-2.4, 12, 0), (0, 0, 0), [(-2.2, 0, -2.2, 4.4, 12, 4.4, 'greave'), (-2.6, 9.5, -2.8, 5.2, 2.5, 5.6, 'boot')]),
])

# armour, its lacing, skin, skirt, crest, eyes
GENERALS = {
    'cloud_soaring_sprite': ((214, 220, 230), (120, 150, 200), (150, 172, 196), (190, 204, 224), (246, 246, 252), (120, 210, 255)),
    'fog_diving_monster': ((96, 106, 118), (60, 66, 76), (104, 126, 114), (74, 84, 96), (170, 176, 186), (190, 240, 210)),
    'mad_hurrying_wind': ((214, 112, 40), (120, 50, 20), (176, 72, 52), (150, 60, 30), (255, 176, 50), (255, 226, 90)),
    'swift_rushing_wind': ((62, 142, 92), (30, 80, 50), (92, 132, 156), (44, 104, 72), (150, 226, 170), (255, 255, 170)),
    'flame_head_general': ((172, 32, 30), (100, 14, 14), (126, 44, 32), (112, 20, 20), None, (255, 210, 70)),
    'eye_spark_officer': ((40, 36, 40), (20, 18, 20), (80, 66, 98), (54, 44, 44), (210, 168, 64), (255, 244, 130)),
}


def lamellar(p, rect, base, lace):
    """Rows of small lacquered plates, laced."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            edge = j % 4 == 3 or (i + (j // 4) * 2) % 4 == 3
            k = 0.78 if edge else 1.0 + 0.06 * ((j % 4) == 0)
            n = p.rnd.randint(-5, 5)
            p.px((fx + i, fy + j), [c * k + n for c in (lace if edge else base)])


def general_painter(name, colours):
    armour, lace, skin, skirt, crest, eyes = colours
    flaming = name == 'flame_head_general'
    sparks = name == 'eye_spark_officer'

    def paint_general(p, style, f, dims):
        if style in ('cuirass', 'pauldron', 'helm', 'greave'):
            for k, r in f.items():
                lamellar(p, r, armour, lace)
        if style == 'helm' and flaming:
            for k in ('top', 'front', 'back', 'right', 'left'):
                fx, fy, fw, fh = f[k]
                flame_face(p, (fx, fy, fw, fh // 2 + 1) if k != 'top' else f[k], density=1.2)
        if style == 'helm':
            fx, fy, fw, fh = f['front']                   # the brim, gilded
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), GOLD)
                p.px((fx + i, fy + fh - 2), GOLD_DEEP)
        if style in ('belt', 'bracer', 'guard'):
            p.shaded(f, GOLD)
        if style == 'mirror':                             # the breast-mirror (护心镜)
            for r in f.values():
                p.face(r, (236, 214, 140), 1.0, noise=6)
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                for j in range(fh):
                    if (i - fw / 2 + 0.5) ** 2 + (j - fh / 2 + 0.5) ** 2 > (fw / 2) ** 2:
                        p.px((fx + i, fy + j), GOLD_DEEP)
        if style == 'tasset':
            for k, r in f.items():
                lamellar(p, r, skirt, lace)
        if style == 'face':
            p.shaded(f, skin)
            fx, fy, fw, fh = f['front']                   # 16x16
            for i in range(2, 14):                        # a heavy scowl
                p.px((fx + i, fy + 5 - (1 if i in (2, 3, 12, 13) else 0)), DARK)
            if sparks:
                for ex in (2, 9):
                    eye(p, fx + ex, fy + 6, 5, 3, eyes)
                    p.px((fx + ex + 2, fy + 7), (255, 255, 255), glow=True)
            else:
                for ex in (3, 10):
                    eye(p, fx + ex, fy + 7, 3, 2, eyes)
            for i in range(4, 12):                        # a snarl
                p.px((fx + i, fy + 12), DARK)
            for i in (5, 10):
                p.px((fx + i, fy + 11), IVORY)
        if style == 'tusk':
            for r in f.values():
                p.face(r, IVORY, 1.0, noise=3)
        if style == 'horn':
            for r in f.values():
                p.gradient(r, IVORY, (70, 54, 40))
        if style == 'crest':
            if flaming or crest is None:
                for k, r in f.items():
                    flame_face(p, r, upward=k not in ('top', 'bottom'))
            else:
                for k, r in f.items():
                    p.face(r, crest, 1.0 if k == 'top' else 0.9, noise=12, fall=0.3)
        if style == 'arm':
            p.shaded(f, skin)
        if style == 'shaft':
            p.shaded(f, (92, 30, 26))
            for k in ('front', 'back', 'right', 'left'):
                fx, fy, fw, fh = f[k]
                for j in range(0, fh, 9):
                    for i in range(fw):
                        p.px((fx + i, fy + j), GOLD)
        if style == 'blade':
            for k, r in f.items():
                p.gradient(r, (236, 240, 246), (150, 156, 168), horizontal=k in ('front', 'back'))
        if style == 'boot':
            p.shaded(f, (40, 30, 26))
    return paint_general


# ── Red Boy ──────────────────────────────────────────────────────────────────
CHILD = (238, 176, 134)
STUBBLE = (120, 96, 92)
RED = (206, 30, 32)
HAIR = (28, 22, 22)

BOY = mirrored([
    part('body', 'root', (0, 9, 0), (0, 0, 0), [
        (-3, 0, -2, 6, 8, 4, 'torso'),
        (-2.8, 0.6, -2.4, 5.6, 6.4, 0.5, 'stomacher'),
        (-3.4, -0.6, -2.4, 6.8, 1.2, 4.8, 'gold'),
    ]),
    part('head', 'root', (0, 9, 0), (0, 0, 0), [
        (-4, -8, -4, 8, 8, 8, 'face'),
        (-3.3, -10.2, -1.2, 2.4, 2.4, 2.4, 'tuft'),
        (0.9, -10.2, -1.2, 2.4, 2.4, 2.4, 'tuft'),
        (-2, -8.7, -4.3, 4, 1.6, 1.2, 'fringe'),
    ]),
    part('right_arm', 'root', (-4, 10, 0), (0, 0, 0), [(-2, -1, -1.25, 2.5, 7, 2.5, 'arm'), (-2.2, 4.3, -1.45, 2.9, 0.9, 2.9, 'gold')]),
    part('right_leg', 'root', (-1.6, 17, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3, 7, 3, 'leg'), (-1.7, 5.4, -1.7, 3.4, 0.8, 3.4, 'gold')]),
])


def paint_boy(p, style, f, dims):
    if style in ('torso', 'arm', 'leg', 'face'):
        p.shaded(f, CHILD, belly=[c * 0.85 for c in CHILD])
    if style == 'torso':                                  # red breeches up to his waist, a sash
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(fh - 5, fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), [c * (0.9 if j == fh - 5 else 1) + p.rnd.randint(-6, 6) for c in RED])
            for i in range(fw):
                p.px((fx + i, fy + fh - 6), GOLD)
    if style == 'leg':                                    # breeches to the knee
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(0, 6):
                for i in range(fw):
                    p.px((fx + i, fy + j), [c + p.rnd.randint(-6, 6) for c in RED])
        fx, fy, fw, fh = f['bottom']
        p.face(f['bottom'], [c * 0.8 for c in CHILD], 1.0)
    if style == 'stomacher':                              # the red stomacher (肚兜), a gold flame on it
        for r in f.values():
            p.face(r, RED, 1.0, noise=6)
        fx, fy, fw, fh = f['front']
        for j in range(fh):
            for i in range(fw):
                if j > fh * 0.55 and abs(i - fw / 2 + 0.5) > (fh - j) * 0.9:
                    p.px((fx + i, fy + j), CHILD)          # cut to a point at the bottom
        cx = fx + fw // 2
        for j, w in ((3, 1), (4, 2), (5, 2), (6, 3), (7, 2)):
            for i in range(-w + 1, w):
                p.px((cx + i, fy + j), GOLD)
    if style == 'face':
        p.face(f['top'], STUBBLE, 1.0, noise=6)          # the shaved crown, dark with stubble
        for k in ('right', 'left', 'back'):              # stubble down to the ears, an ear either side
            fx, fy, fw, fh = f[k]
            for j in range(5):
                for i in range(fw):
                    p.px((fx + i, fy + j), [c + p.rnd.randint(-6, 6) for c in STUBBLE])
            if k != 'back':
                ex = fx + fw // 2 - 1
                for j in range(7, 11):
                    for i in range(2):
                        p.px((ex + i, fy + j), [c * 0.86 for c in CHILD])
        fx, fy, fw, fh = f['front']                       # 16x16: a round child's face, fierce brows
        for i in range(5):                                # brows sloping down to the nose: a scowl
            p.px((fx + 2 + i, fy + 5 + i // 2), HAIR)
            p.px((fx + 2 + i, fy + 6 + i // 2), HAIR)
            p.px((fx + 13 - i, fy + 5 + i // 2), HAIR)
            p.px((fx + 13 - i, fy + 6 + i // 2), HAIR)
        for ex in (3, 10):                                # wide eyes, the fire in them
            for i in range(3):
                for j in (9, 10):
                    p.px((fx + ex + i, fy + j), (250, 250, 246))
            p.px((fx + ex + (2 if ex == 3 else 0), fy + 9), (24, 14, 14))
            p.px((fx + ex + (2 if ex == 3 else 0), fy + 10), (255, 110, 30), glow=True)
            p.px((fx + ex + 1, fy + 9), (24, 14, 14))
            p.px((fx + ex + 1, fy + 10), (24, 14, 14))
        for i in range(6, 10):                            # a set mouth
            p.px((fx + i, fy + 13), (150, 36, 36))
        p.px((fx + 7, fy + 12), [c * 0.8 for c in CHILD])  # a little nose
        p.px((fx + 8, fy + 12), [c * 0.8 for c in CHILD])
        for i in (2, 3, 12, 13):                          # rosy cheeks
            p.px((fx + i, fy + 12), (236, 130, 120))
        for i in range(fw):                               # stubble over the brow, under the fringe
            for j in range(3):
                p.px((fx + i, fy + j), [c + p.rnd.randint(-6, 6) for c in STUBBLE])
    if style in ('tuft', 'fringe'):
        for k, r in f.items():
            p.face(r, HAIR, 1.0 if k == 'top' else 0.9, noise=6)
    if style == 'tuft':                                   # tied up with red cord
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), RED)
    if style == 'gold':
        p.shaded(f, GOLD)


# ── The fire cart ────────────────────────────────────────────────────────────
LACQUER = (176, 28, 30)

CART = mirrored([
    part('body', 'root', (0, 13, 0), (0, 0, 0), [
        (-7, -2, -7, 14, 4, 14, 'bed'),
        (-7, -6, -7, 14, 4, 1, 'wall'),
        (-7, -6, 6, 14, 4, 1, 'wall'),
        (-7, -6, -6, 1, 4, 12, 'wall'),
        (6, -6, -6, 1, 4, 12, 'wall'),
    ]),
    part('flames', 'body', (0, -6, 0), (0, 0, 0), [(-6, -6, -6, 12, 7, 12, 'flame')]),
    part('flames_high', 'flames', (0, -5, 0), (0, 0, 0), [(-3.5, -8, -3.5, 7, 8, 7, 'flame')]),
    part('right_wheel', 'root', (-8, 18, 0), (0, 0, 0), [(-1, -6, -6, 2, 12, 12, 'wheel')]),
    part('shafts', 'root', (0, 14, -7), (0.6, 0, 0), [(-5, 0, -12, 1, 1, 12, 'wood'), (4, 0, -12, 1, 1, 12, 'wood')]),
])


def paint_cart(p, style, f, dims):
    if style in ('bed', 'wall'):
        p.shaded(f, LACQUER)
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                p.px((fx + i, fy), GOLD)
                if fh > 4:
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
    if style == 'flame':
        for k, r in f.items():
            if k == 'bottom':
                continue
            flame_face(p, r, base=(230, 40, 30), upward=k != 'top', density=1.3)
    if style == 'wheel':
        for k, r in f.items():
            fx, fy, fw, fh = r
            if k in ('right', 'left'):                    # a spoked wheel, its rim gold
                cx, cy, rad = fx + fw / 2 - 0.5, fy + fh / 2 - 0.5, fw / 2
                for j in range(fh):
                    for i in range(fw):
                        d = ((i + fx - cx) ** 2 + (j + fy - cy) ** 2) ** 0.5
                        if d > rad:
                            p.px((fx + i, fy + j), (0, 0, 0))
                            p.img.putpixel((fx + i, fy + j), (0, 0, 0, 0))
                        elif d > rad - 2.5:
                            p.px((fx + i, fy + j), GOLD)
                        elif d < 2.5 or abs(i + fx - cx) < 1 or abs(j + fy - cy) < 1 or abs(abs(i + fx - cx) - abs(j + fy - cy)) < 1:
                            p.px((fx + i, fy + j), (120, 30, 24))
                        else:
                            p.img.putpixel((fx + i, fy + j), (0, 0, 0, 0))
            else:
                p.face(r, GOLD_DEEP, 1.0, noise=6)
    if style == 'wood':
        p.shaded(f, (110, 40, 30))


if __name__ == '__main__':
    for name, parts, painter_fn, java, seed in (
            ('ember_imp', IMP, paint_imp, 'EmberImpRenderer.java', 3),
            ('furnace_brute', BRUTE, paint_brute, 'FurnaceBruteRenderer.java', 5),
            ('red_boy', BOY, paint_boy, 'RedBoyRenderer.java', 7),
            ('fire_cart', CART, paint_cart, 'FireCartRenderer.java', 9)):
        p = Painter(seed)
        slots = paint(parts, p, painter_fn)
        p.save(name)
        emit(parts, slots, R + java)
        print(name, len(parts), 'parts,', len(slots), 'boxes')
    slots = None
    for name, colours in GENERALS.items():
        p = Painter(11)
        slots = paint(GENERAL, p, general_painter(name, colours))
        p.save('strong_general_' + name)
    emit(GENERAL, slots, R + 'StrongGeneralRenderer.java')
    print('strong generals', len(GENERAL), 'parts,', len(slots), 'boxes')
