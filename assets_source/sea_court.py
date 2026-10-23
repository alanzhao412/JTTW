"""The Mystical Waters' creatures: the Shrimp Soldier, the Crab General, the Yaksha and the four
Dragon Kings. Each one's model and skin come from one list of parts (beastgen): the model is written
into its renderer's layer() between its GENERATED markers, and the skin painted to the same box-UV
layout at twice the model's resolution, with a second, glowing skin for eyes and pearls.

The four Dragon Kings share one model; each has his own skin, his robe in his sea's colour (East
blue-green, South red, West white, North black) and his scales to match.

Run from the repository root: python assets_source/sea_court.py"""
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
KINGS = {
    # robe, its trim, scales, mane
    'ao_guang': ((40, 150, 136), (22, 92, 84), (62, 160, 170), (236, 236, 226)),   # East: blue-green
    'ao_qin': ((176, 34, 38), (110, 18, 22), (204, 112, 52), (250, 214, 150)),     # South: red
    'ao_run': ((236, 234, 226), (186, 184, 176), (196, 210, 222), (250, 250, 250)),  # West: white
    'ao_shun': ((30, 30, 38), (14, 14, 18), (60, 70, 104), (196, 200, 214)),      # North: black
}

KING = [
    part('body', 'root', (0, 0, 0), (0, 0, 0), [
        (-5, -4, -3, 10, 14, 6, 'robe'),
        (-6, -4.5, -3.5, 12, 2.5, 7, 'trim'),
        (-5.5, 6, -3.5, 11, 2, 7, 'belt'),
        (-1.5, 5.6, -3.9, 3, 2.6, 0.6, 'jade'),
    ]),
    part('skirt', 'body', (0, 9, 0), (0, 0, 0), [
        (-6, 0, -4, 12, 14, 8, 'robe'),
        (-6.3, 12, -4.3, 12.6, 2, 8.6, 'trim'),
    ]),
    part('right_boot', 'root', (-2.5, 22, -3.5), (0, 0, 0), [(-2, 0, -1.5, 4, 2, 3, 'boot')]),
    part('left_boot', 'root', (2.5, 22, -3.5), (0, 0, 0), [(-2, 0, -1.5, 4, 2, 3, 'boot')], True),
    part('tail', 'root', (0, 19, 3.5), (1.2, 0, 0), [(-2, -2, 0, 4, 4, 9, 'scales')]),
    part('tail_end', 'tail', (0, 0, 8.5), (-0.6, 0, 0), [(-1.5, -1.5, 0, 3, 3, 8, 'scales'), (-0.5, -3, 0, 1, 1.5, 8, 'mane')]),
    part('right_arm', 'root', (-6.5, -2.5, 0), (0, 0, 0), [
        (-3, -2, -2, 4, 11, 4, 'robe'),
        (-3.6, 7, -2.6, 5.2, 4, 5.2, 'trim'),
        (-2.5, 11, -1.5, 3, 3, 3, 'scales'),
        (-2.5, 13.5, -1.6, 0.8, 1.5, 0.8, 'claw'),
        (-0.9, 13.5, -1.6, 0.8, 1.5, 0.8, 'claw'),
    ]),
    part('left_arm', 'root', (6.5, -2.5, 0), (0, 0, 0), [
        (-1, -2, -2, 4, 11, 4, 'robe'),
        (-1.6, 7, -2.6, 5.2, 4, 5.2, 'trim'),
        (-0.5, 11, -1.5, 3, 3, 3, 'scales'),
        (-0.3, 13.5, -1.6, 0.8, 1.5, 0.8, 'claw'),
        (1.3, 13.5, -1.6, 0.8, 1.5, 0.8, 'claw'),
    ], True),
    part('head', 'root', (0, -4, 0), (0, 0, 0), [
        (-4, -9, -4, 8, 9, 8, 'skull'),
        (-3, -6, -11, 6, 4, 7, 'snout'),
        (-2.5, -7, -10, 5, 1, 6, 'scales'),
        (-4.5, -8, 2.5, 9, 10, 3, 'mane'),
        (-2, 0, -6, 4, 4, 2, 'mane'),
        (-3, -12, -3, 6, 3, 6, 'crown'),
        (-1.5, -14.5, -3.6, 3, 3, 1, 'crown'),
        (-0.6, -13.8, -4.1, 1.2, 1.2, 0.6, 'pearl'),
    ]),
    part('jaw', 'head', (0, -2, -4), (0.15, 0, 0), [
        (-2.5, 0, -7, 5, 1.6, 7, 'jaw'),
        (-2.1, -0.9, -6.6, 0.6, 1, 0.6, 'fang'),
        (1.5, -0.9, -6.6, 0.6, 1, 0.6, 'fang'),
    ]),
    part('right_horn', 'head', (-2.2, -9, -1), (0.75, -0.3, 0), [(-0.75, -0.75, 0, 1.5, 1.5, 8, 'horn')]),
    part('right_horn_branch', 'right_horn', (0, 0, 3.5), (-0.9, -0.3, 0), [(-0.5, -0.5, 0, 1, 1, 4, 'horn')]),
    part('left_horn', 'head', (2.2, -9, -1), (0.75, 0.3, 0), [(-0.75, -0.75, 0, 1.5, 1.5, 8, 'horn')], True),
    part('left_horn_branch', 'left_horn', (0, 0, 3.5), (-0.9, 0.3, 0), [(-0.5, -0.5, 0, 1, 1, 4, 'horn')], True),
    part('right_whisker', 'head', (-3, -3.5, -10), (-0.35, 0.5, 0), [(-0.3, -0.3, 0, 0.6, 0.6, 12, 'whisker')]),
    part('left_whisker', 'head', (3, -3.5, -10), (-0.35, -0.5, 0), [(-0.3, -0.3, 0, 0.6, 0.6, 12, 'whisker')], True),
]


def king_eye(p, fx, fy, away):
    """A dragon's eye from its corner at the snout end, running `away` toward the back: gold, burning."""
    for i in range(-1, 7):
        p.px((fx + i * away, fy - 1 - (1 if i > 3 else 0)), DARK)   # the brow
    for i in range(6):
        for j in (0, 1):
            p.px((fx + i * away, fy + j - (1 if i > 3 else 0)), (255, 206, 60), glow=True)
    for j in (0, 1):
        p.px((fx + 2 * away, fy + j), (60, 30, 6), glow=True)


def king_painter(robe, trim, scale, mane):
    def paint_king(p, style, f, dims):
        if style == 'robe':
            p.shaded(f, robe)
            for k in ('front', 'back', 'right', 'left'):       # woven waves
                fx, fy, fw, fh = f[k]
                for j in range(3, fh, 6):
                    for i in range(fw):
                        if (i + j // 6 * 2) % 4 < 2:
                            p.px((fx + i, fy + j), [v * 0.82 + 20 for v in robe])
        if style == 'trim':
            p.shaded(f, GOLD)
            for k in ('front', 'back', 'right', 'left'):
                fx, fy, fw, fh = f[k]
                for i in range(fw):
                    p.px((fx + i, fy + fh // 2), trim)
        if style == 'belt':
            p.shaded(f, trim)
        if style == 'jade':
            for r in f.values():
                p.face(r, (120, 210, 150), 1.0, noise=4)
        if style == 'boot':
            p.shaded(f, (30, 26, 24))
        if style in ('scales', 'skull', 'snout'):
            for k in ('top', 'right', 'left', 'front', 'back'):
                p.scales(f[k], scale, cell=4, light=1.05 if k == 'top' else 0.92)
            p.face(f['bottom'], [v * 0.8 + 30 for v in scale], 0.9)
        if style == 'skull':
            fx, fy, fw, fh = f['right']
            king_eye(p, fx + fw - 2, fy + 7, -1)
            fx, fy, fw, fh = f['left']
            king_eye(p, fx + 1, fy + 7, 1)
        if style == 'snout':
            fx, fy, fw, fh = f['front']
            for cx in (fx + 2, fx + fw - 4):
                for i in range(2):
                    p.px((cx + i, fy + 2), DARK)
                    p.px((cx + i, fy + 3), DARK)
            for k in ('right', 'left'):
                fx, fy, fw, fh = f[k]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), DARK)
        if style == 'jaw':
            p.shaded(f, scale)
            p.face(f['top'], (120, 34, 40), 0.85, noise=4)
        if style == 'fang' or style == 'claw':
            for r in f.values():
                p.face(r, IVORY, 1.0, noise=3)
        if style == 'mane':
            for k, r in f.items():
                p.face(r, mane, 1.0 if k == 'top' else 0.9, noise=8, fall=0.25)
                fx, fy, fw, fh = r
                for i in range(0, fw, 2):
                    for j in range(fh):
                        if p.rnd.random() < 0.3:
                            p.px((fx + i, fy + j), [v * 0.8 for v in mane])
        if style == 'horn':
            for k, r in f.items():
                p.gradient(r, (210, 190, 140), (250, 238, 200), horizontal=k in ('top', 'bottom'))
        if style == 'whisker':
            for r in f.values():
                p.face(r, GOLD, 1.0, noise=6)
        if style == 'crown':
            p.shaded(f, GOLD)
            fx, fy, fw, fh = f['front']
            for i in range(0, fw, 3):
                p.px((fx + i, fy), (255, 236, 150))
        if style == 'pearl':
            for r in f.values():
                p.face(r, PEARL, 1.0, noise=3, glow=True)
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
    slots = None
    for king, colours in KINGS.items():
        p = Painter(11)
        slots = paint(KING, p, king_painter(*colours))
        p.save(king)
    emit(KING, slots, R + 'DragonKingRenderer.java')
    print('dragon kings', len(KING), 'parts,', len(slots), 'boxes')
