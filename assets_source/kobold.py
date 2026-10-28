"""The kobold (穴居小妖), a cave yaoguai of the open world. As wild_paint.py says: its model is written
into WildRenderers' koboldLayer() between its GENERATED markers, its skin (textures/entity/kobold.png)
painted to the same box-UV layout, its eyes, its candle and its lantern glowing (kobold_glow.png).

A little dog-faced, lizard-scaled miner of the dark under the mountains, crouched on long-footed
digitigrade legs, a heavy tail behind it: slate-teal scales, a cream scaled belly, a long snout full
of teeth, amber slit eyes under scaly brows, orange webbed frills at its cheeks, two horns swept back
and a crest of spines down its neck and tail. On its head a battered iron pot for a helmet, worn
askew, a stub of candle stuck on it and burning; over its leather jerkin a cuirass of scrap iron
riveted together, a scrap-iron shoulder and greaves, a belt with a pouch, and strings of copper cash
about its neck. On its back a wooden pack-frame (背架) holding a wicker basket of ore (copper, iron,
a glowing jade crystal), and arching up from it over its head a pole with a red paper lantern
swinging from its end. A miner's pick in its right fist; in its left, a chunk of jade it has dug.

Run from the repository root: python assets_source/kobold.py"""
import math

import beastgen as bg
import wild_paint as w
from wild_paint import part, mirrored, aim, paint, Painter, k, mix

bg.UNITS = 128
bg.TEX = bg.UNITS * bg.S

SIZE = 0.68          # KoboldRenderer.SIZE

SCALE = (78, 126, 120)
SCALE_DARK = (44, 74, 74)
SCALE_EDGE = (24, 42, 44)
SCALE_HI = (150, 196, 170)
BELLY = (210, 192, 132)
BELLY_EDGE = (150, 128, 80)
FRILL = (220, 112, 40)
FRILL_DEEP = (150, 56, 24)
IRON = (96, 94, 98)
IRON_DARK = (54, 52, 56)
RUST = (138, 70, 32)
LEATHER = (112, 74, 44)
WOOD = (128, 90, 52)
WICKER = (176, 140, 84)
LANTERN = (206, 44, 30)
LANTERN_GLOW = (255, 132, 60)
GOLD = (220, 170, 60)
COPPER = (186, 114, 60)
EYE = (255, 168, 36)
TOOTH = (232, 222, 190)
GUM = (90, 24, 28)
WAX = (232, 224, 200)
FLAME = (255, 196, 80)
JADE = (100, 220, 150)
CLAW = (34, 30, 28)

BODY = (0.28, 0, 0)
HEAD = (-0.3, 0, 0)
ARM, FOREARM, HAND = (-0.75, 0, 0.12), (-0.95, 0, 0), (0, 0, 0)
LARM, LFOREARM, LHAND = (-0.35, 0, -0.18), (-0.9, 0, 0), (0, 0, 0)
POLE = (0.5, 0, -0.42)
HOOK = (-0.5, 0, 0.42)


def cash():
    """Strings of copper cash slung round its neck: two loops of coins over its breast."""
    out = []
    for loop, (r, y0, z) in enumerate(((2.4, -8.9, -3.05), (3.0, -9.0, -3.15))):
        n = 6 if loop == 0 else 8
        for i in range(n):
            a = -1.2 + 2.4 * i / (n - 1)
            x, y = r * math.sin(a), y0 + r * 0.9 * math.cos(a)
            out.append((x - 0.6, y - 0.6, z, 1.2, 1.2, 0.3, 'coin'))
    return out


KOBOLD = mirrored([
    # digitigrade legs: a thick thigh, a shin with a scrap greave, a long ankle, a clawed foot
    part('right_leg', 'root', (-2.2, 13.0, 1.0), (-0.8, 0, 0.08), [
        (-1.55, -0.8, -1.6, 3.1, 5.0, 3.2, 'thigh')]),
    part('right_shin', 'right_leg', (0, 3.9, 0.2), (1.5, 0, 0), [
        (-1.1, 0, -1.1, 2.2, 4.4, 2.2, 'shin'),
        (-1.4, 0.4, -1.6, 2.8, 3.0, 1.0, 'greave')]),
    part('right_ankle', 'right_shin', (0, 4.2, 0), (-1.35, 0, 0), [(-0.9, 0, -0.9, 1.8, 3.6, 1.8, 'ankle')]),
    part('right_foot', 'right_ankle', (0, 3.4, 0), (0.62, 0, 0), [
        (-1.4, -0.4, -3.0, 2.8, 1.2, 3.6, 'foot'),
        (-1.45, 0.0, -4.0, 0.7, 0.8, 1.2, 'toe'),
        (-0.35, 0.0, -4.2, 0.7, 0.8, 1.4, 'toe'),
        (0.75, 0.0, -4.0, 0.7, 0.8, 1.2, 'toe')]),

    part('body', 'root', (0, 13.0, 1.0), BODY, [
        (-3.0, -2.6, -2.5, 6.0, 3.4, 5.0, 'hips'),
        (-3.2, -9.0, -2.6, 6.4, 6.8, 5.0, 'torso'),
        (-3.6, -9.5, -2.95, 7.2, 3.3, 5.7, 'cuirass'),
        (-3.5, -3.3, -2.95, 7.0, 1.2, 5.6, 'belt'),
        (1.6, -3.0, -3.4, 1.8, 2.0, 1.2, 'pouch'),
        (-1.3, -10.6, -1.6, 2.6, 2.0, 2.8, 'neck'),
    ] + cash()),
    part('right_tasset', 'body', (-1.6, -2.3, -2.7), (-0.08, 0.1, 0.05), [(-1.4, 0, -0.3, 2.8, 4.2, 0.6, 'tasset')]),
    part('right_tasset_side', 'body', (-3.0, -2.3, 0), (0, 0, 0.12), [(-0.4, 0, -2.0, 0.6, 3.6, 4.0, 'tasset_side')]),
    # the tail, heavy, curving down to the ground behind; spines along it
    part('tail', 'body', (0, -1.0, 2.2), (-0.7, 0, 0), [
        (-1.5, -1.5, 0, 3.0, 3.0, 4.6, 'tail1'),
        (-0.3, -2.4, 0.6, 0.6, 1.0, 1.4, 'spine'),
        (-0.3, -2.3, 2.6, 0.6, 0.9, 1.2, 'spine')]),
    part('tail_mid', 'tail', (0, 0, 4.3), (-0.35, 0.1, 0), [
        (-1.1, -1.1, 0, 2.2, 2.2, 4.4, 'tail2'),
        (-0.25, -1.9, 0.8, 0.5, 0.9, 1.2, 'spine_small'),
        (-0.25, -1.8, 2.6, 0.5, 0.8, 1.0, 'spine_small')]),
    part('tail_tip', 'tail_mid', (0, 0, 4.2), (0.25, 0.15, 0), [
        (-0.7, -0.7, 0, 1.4, 1.4, 4.0, 'tail3'),
        (-0.2, -1.3, 1.0, 0.4, 0.7, 1.0, 'spine_small')]),

    # the pack-frame on its back, a wicker basket of ore in it
    part('frame', 'body', (0, -9.6, 2.6), (0.08, 0, 0), [
        (-3.0, -3.0, 0, 0.8, 12.0, 0.8, 'pole_v'),
        (2.2, -3.0, 0, 0.8, 12.0, 0.8, 'pole_v'),
        (-3.0, -2.4, 0.05, 6.0, 0.7, 0.7, 'crossbar'),
        (-3.0, 7.6, 0.05, 6.0, 0.7, 0.7, 'crossbar'),
        (-2.8, -1.4, 0.8, 5.6, 6.2, 3.6, 'basket'),
        (-2.2, -2.6, 1.2, 1.6, 1.4, 1.6, 'ore_copper'),
        (-0.5, -2.9, 1.8, 1.4, 1.6, 1.4, 'ore_jade'),
        (0.9, -2.4, 1.0, 1.6, 1.2, 1.8, 'ore_iron'),
        (-1.9, -2.0, 2.8, 1.3, 0.9, 1.2, 'ore_copper')]),
    # and from it, a pole arching up over its head, a red lantern swinging from its end
    part('pole', 'frame', (-2.6, -2.6, 0.4), POLE, [(-0.35, -11.0, -0.35, 0.7, 11.4, 0.7, 'pole')]),
    part('pole_hook', 'pole', (0, -10.8, 0), HOOK, [(-0.25, -0.2, -1.8, 0.5, 0.5, 2.0, 'hook')]),

    # the head: a long dog's snout, scaly; under a battered iron pot with a candle on it
    part('head', 'body', (0, -10.4, -1.0), HEAD, [
        (-3.0, -5.0, -3.0, 6.0, 5.0, 5.6, 'cranium'),
        (-2.0, -2.9, -8.2, 4.0, 2.5, 5.4, 'snout'),
        (-1.0, -3.5, -7.8, 2.0, 0.8, 4.6, 'snout_ridge'),
        (-0.7, -2.9, -8.6, 1.4, 1.0, 0.5, 'noseleather'),
        (-1.2, -0.6, 1.8, 2.4, 1.6, 1.4, 'nape')]),
    part('jaw', 'head', (0, -0.5, -2.6), (0.18, 0, 0), [(-1.9, 0, -5.5, 3.8, 1.3, 5.4, 'jaw')]),
    part('right_eye', 'head', (-2.3, -3.25, -2.9), (0, 0.35, 0), [(-1.0, -0.7, -0.35, 2.0, 1.4, 0.5, 'eye')]),
    part('right_brow', 'head', (-1.7, -4.25, -3.25), (0, 0.2, -0.32), [(-1.3, -0.45, -0.7, 2.6, 0.9, 1.4, 'brow')]),
    part('right_frill', 'head', (-2.6, -2.2, -0.2), (0.1, 0.7, 0.35), [(-3.8, -1.8, -0.2, 3.8, 3.6, 0.4, 'frill')]),
    part('right_horn', 'head', (-1.7, -4.3, 0.2), (-1.2, 0, -0.3), [(-0.5, -3.2, -0.5, 1.0, 3.4, 1.0, 'horn')]),
    part('crest', 'head', (0, -4.4, 1.4), (0.3, 0, 0), [
        (-0.2, -1.4, -0.6, 0.4, 1.6, 1.2, 'crest'),
        (-0.2, -1.1, 0.8, 0.4, 1.3, 1.1, 'crest'),
        (-0.2, -0.8, 2.0, 0.4, 1.0, 1.0, 'crest')]),
    part('helmet', 'head', (0, -4.7, -0.1), (0.12, 0, 0.14), [
        (-3.1, -2.6, -3.1, 6.2, 2.8, 6.2, 'pot'),
        (-3.6, -0.1, -3.6, 7.2, 0.6, 7.2, 'potrim'),
        (-0.6, -3.3, -0.6, 1.2, 0.8, 1.2, 'drip'),
        (-0.45, -4.9, -0.45, 0.9, 1.7, 0.9, 'candle')]),
    part('flame', 'helmet', (0, -4.9, 0), (0, 0, 0), [
        (-0.4, -1.3, -0.05, 0.8, 1.3, 0.1, 'flame'),
        (-0.05, -1.3, -0.4, 0.1, 1.3, 0.8, 'flame_x')]),

    # the arms: a scrap pauldron over the shoulder, a leather bracer, clawed hands
    part('right_arm', 'body', (-3.6, -8.2, -0.4), ARM, [
        (-1.2, -0.6, -1.2, 2.4, 4.2, 2.4, 'arm'),
        (-1.9, -1.5, -1.8, 2.9, 2.3, 3.6, 'pauldron')]),
    part('right_forearm', 'right_arm', (-0.1, 3.8, 0), FOREARM, [
        (-1.0, 0, -1.0, 2.0, 4.0, 2.0, 'forearm'),
        (-1.25, 1.2, -1.25, 2.5, 2.4, 2.5, 'bracer')]),
    part('right_hand', 'right_forearm', (0, 4.0, 0), HAND, [
        (-1.1, 0, -1.2, 2.2, 1.6, 2.4, 'hand'),
        (-1.05, 1.4, -1.1, 0.6, 1.2, 0.6, 'claw'),
        (-0.3, 1.4, -1.1, 0.6, 1.3, 0.6, 'claw'),
        (0.45, 1.4, -1.1, 0.6, 1.2, 0.6, 'claw')]),
])

KOBOLD += [
    # the pick, held up in its right fist, its head forward
    part('pick', 'right_hand', (0, 0.9, -0.1), aim([BODY, ARM, FOREARM, HAND], (-0.35, 0, 0)), [
        (-0.4, -9.0, -0.4, 0.8, 10.4, 0.8, 'haft'),
        (-0.55, -9.4, -0.55, 1.1, 1.6, 1.1, 'socket'),
        (-0.5, -9.2, -4.6, 1.0, 1.1, 4.1, 'pick_point'),
        (-0.6, -9.3, 0.5, 1.2, 1.3, 2.4, 'pick_blade')]),
    # a chunk of jade in its left
    part('jade', 'left_hand', (0, 1.6, -0.3), (0.3, 0.4, 0.2), [(-0.9, -0.6, -0.9, 1.8, 2.0, 1.8, 'jade')]),
    # the lantern, hanging straight down from the hook whatever the pole does
    part('lantern', 'pole_hook', (0, 0.1, -1.6), aim([BODY, (0.08, 0, 0), POLE, HOOK], (0, 0, 0)), [
        (-0.1, 0, -0.1, 0.2, 1.4, 0.2, 'lantern_cord'),
        (-1.0, 1.3, -1.0, 2.0, 0.5, 2.0, 'lantern_cap'),
        (-1.6, 1.8, -1.6, 3.2, 3.6, 3.2, 'lantern'),
        (-1.0, 5.4, -1.0, 2.0, 0.5, 2.0, 'lantern_cap'),
        (-0.3, 5.9, -0.3, 0.6, 2.0, 0.6, 'lantern_tassel')]),
]
KOBOLD = w.settle(KOBOLD, ('right_foot', 'left_foot'))


# ── The skin ─────────────────────────────────────────────────────────────────

def scaly(p, f, faces=None, belly_face=None, cell=4):
    """Slate-teal scales over every face, each its own shade, lit on top, darker beneath; a darker
    stripe down its back; a cream scaled belly on `belly_face`."""
    for s in faces or f:
        if s == belly_face:
            w.plates(p, f[s], k(BELLY, w.LIGHT[s]), k(BELLY_EDGE, w.LIGHT[s]), rows=3)
            continue
        base = SCALE if s != 'bottom' else mix(SCALE, BELLY, 0.5)
        fx, fy, fw, fh = f[s]
        w.scale_tiles(p, f[s], base, mix(SCALE_EDGE, base, 0.45), w.LIGHT[s], cw=max(3, cell), ch=2, hi=SCALE_HI, seed=fx * 7 + fy, vary=0.16)
        for i, j, xy in w.each(p, f[s]):                          # a darker stripe down its back
            if s in ('top', 'back') and abs(i - fw / 2 + 0.5) < fw * 0.18:
                p.px(xy, k(p.get(xy), 0.72))


def claws(p, f):
    for s in w.SIDES + ('bottom',):
        fx, fy, fw, fh = f[s]
        for i, j, xy in w.each(p, f[s]):
            if j >= fh * 0.45 or s == 'bottom':
                p.px(xy, k(mix(CLAW, (120, 110, 96), 0.3 * (j < fh * 0.6)), w.LIGHT[s]))


def scrap(p, f, faces=None, rivets=3):
    """Scrap iron: mismatched plates, some rusty, riveted at their seams."""
    w.iron(p, f, IRON, rust=0.4, rivets=rivets, dents=0.4, faces=faces, rustc=RUST)
    for s in faces or w.SIDES:
        fx, fy, fw, fh = f[s]
        if fw > 6:
            x = fw // 2 + p.rnd.randint(-2, 2)
            w.vline(p, f[s], x, k(IRON_DARK, 0.7))
            for j in range(1, fh - 1, 3):
                w.rivet(p, fx + x + 1, fy + j, IRON)


def paint_kobold(p, style, f, dims):
    if style in ('thigh', 'shin', 'ankle', 'arm', 'forearm', 'neck', 'nape'):
        scaly(p, f, cell=4 if style in ('thigh', 'arm') else 3)

    if style in ('tail1', 'tail2', 'tail3'):
        scaly(p, f, belly_face='bottom', cell=4 if style == 'tail1' else 3)
        if style == 'tail3':
            for s in w.SIDES:
                pass

    if style in ('foot',):
        scaly(p, f, cell=3)

    if style in ('toe', 'claw'):
        scaly(p, f, cell=2)
        claws(p, f)

    if style == 'hand':
        scaly(p, f, cell=3)

    if style == 'torso':                                          # its chest: a pale scaled belly down the front
        scaly(p, f, belly_face='front')

    if style == 'hips':
        w.leather(p, f, LEATHER, stitch=(170, 140, 96))

    if style == 'cuirass':                                        # scrap iron riveted together over the breast
        scrap(p, f)
        w.leather(p, f, LEATHER, faces=('top', 'bottom'))
        fx, fy, fw, fh = f['front']                               # a plate of copper patched onto it
        for j in range(2, 6):
            for i in range(fw - 7, fw - 3):
                p.px((fx + i, fy + j), k(COPPER, 0.95 + 0.1 * (j == 2)))
        w.rivet(p, fx + fw - 7, fy + 2, COPPER)
        w.rivet(p, fx + fw - 4, fy + 5, COPPER)

    if style == 'belt':
        w.leather(p, f, k(LEATHER, 0.85), stitch=(170, 140, 96))
        fx, fy, fw, fh = f['front']
        for j in range(fh):                                       # an iron buckle
            for i in range(fw // 2 - 2, fw // 2 + 2):
                p.px((fx + i, fy + j), IRON if j in (0, fh - 1) or i in (fw // 2 - 2, fw // 2 + 1) else k(LEATHER, 0.6))

    if style == 'pouch':
        w.leather(p, f, (130, 88, 54), stitch=(190, 160, 110))
        w.flat(p, f, k(LEATHER, 0.7), faces=('top',))

    if style == 'coin':                                           # copper cash, square-holed, green at the edges
        w.flat(p, f, COPPER, noise=6)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if i in (0, fw - 1) or j in (0, fh - 1):
                    p.px(xy, mix(COPPER, (80, 140, 110), 0.4))
            p.px((fx + fw // 2, fy + fh // 2), (24, 16, 12))
            p.px((fx + fw // 2 - 1, fy + fh // 2), (24, 16, 12))
            p.px((fx + 1, fy + 1), (240, 190, 130))

    if style in ('tasset', 'tasset_side'):                        # leather flaps, iron-plated, frayed
        w.leather(p, f, LEATHER)
        for s in w.SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(0, fh - 2, 3):
                w.hline(p, f[s], j, k(IRON, w.LIGHT[s]))
                w.hline(p, f[s], j + 1, k(IRON_DARK, w.LIGHT[s]))
            w.ragged(p, f[s], teeth=2, depth=2)

    if style in ('spine', 'spine_small', 'crest'):                # spines: orange, darker at the root, pointed
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                p.px(xy, k(mix(FRILL, FRILL_DEEP, j / max(1, fh - 1)), w.LIGHT[s]))
        for s in ('right', 'left'):
            w.point(p, f[s], down=False, keep=0.4, sharp=0.6)

    if style in ('pole_v', 'crossbar', 'pole', 'haft'):
        w.wood(p, f, WOOD if style != 'haft' else (140, 100, 60), along='y' if style != 'crossbar' else 'x')
        if style == 'crossbar':                                   # bound with cord at its ends
            for s in w.SIDES:
                w.vline(p, f[s], 0, (170, 140, 90))
                w.vline(p, f[s], f[s][2] - 1, (170, 140, 90))
        if style == 'haft':
            for s in w.SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(fh - 8, fh - 2):                   # its grip, bound in leather
                    w.hline(p, f[s], j, k(LEATHER, w.LIGHT[s] * (0.8 + 0.2 * (j % 2))))

    if style == 'hook':
        w.iron(p, f, IRON_DARK, rivets=0, rim=False)

    if style == 'basket':                                         # wicker, woven over and under
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                over = ((i // 2) + (j // 2)) % 2
                c = k(WICKER, (1.05 if over else 0.72) * w.LIGHT[s])
                if j % 2 == 1 and not over:
                    c = k(c, 0.85)
                p.px(xy, [v + p.rnd.randint(-5, 5) for v in c])
        w.flat(p, f, (50, 44, 40), faces=('top',))                 # (its mouth: dark, the ore in it)
        w.border(p, f, k(WICKER, 0.6), top=True, bottom=True)

    if style == 'ore_copper':
        w.hide(p, f, (96, 88, 80), (60, 54, 50), mottle=0.6)
        for s in f:
            fx, fy, fw, fh = f[s]
            for _ in range(fw * fh // 4):
                p.px((fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)), (214, 128, 72))
    if style == 'ore_iron':
        w.hide(p, f, (110, 104, 98), (64, 60, 58), mottle=0.6)
        for s in f:
            fx, fy, fw, fh = f[s]
            for _ in range(fw * fh // 4):
                p.px((fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)), (226, 192, 160))
    if style in ('ore_jade', 'jade'):                             # jade crystal, glowing a little from within
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                t = ((i + j) % 4) / 3
                c = mix(JADE, (40, 120, 90), t * 0.7)
                p.lit(xy, k(c, w.LIGHT[s]), 0.55)
            p.lit((fx, fy), (220, 255, 230), 0.9)

    if style == 'cranium':                                        # its skull, scaled; its face scaled finer
        scaly(p, f, cell=3)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            if j > fh - 4:
                p.px(xy, k(SCALE_DARK, 1.0))

    if style in ('snout', 'snout_ridge'):                         # the snout: scales, a black nose at its end, a lip of teeth
        scaly(p, f, cell=3)
        if style == 'snout':
            for s in ('right', 'left'):                           # its upper teeth along its lip
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), GUM)
                    if i % 2 == 0:
                        p.px((fx + i, fy + fh - 1), TOOTH)
                        if i % 4 == 0 and fh > 3:
                            p.px((fx + i, fy + fh - 2), TOOTH)
            fx, fy, fw, fh = f['bottom']
            for i, j, xy in w.each(p, f['bottom']):
                p.px(xy, GUM if 0 < i < fw - 1 else TOOTH)

    if style == 'noseleather':                                    # a black dog's nose, wet-lit, its nostrils
        w.flat(p, f, (30, 26, 30))
        fx, fy, fw, fh = f['front']
        p.px((fx, fy + fh - 1), (6, 4, 6))
        p.px((fx + fw - 1, fy + fh - 1), (6, 4, 6))
        p.px((fx + 1, fy), (120, 116, 126))

    if style == 'jaw':                                            # the lower jaw: a row of teeth along its top, a pale throat
        scaly(p, f, cell=2, belly_face='bottom')
        fx, fy, fw, fh = f['top']
        for i, j, xy in w.each(p, f['top']):
            p.px(xy, (60, 16, 20) if 0 < i < fw - 1 else (TOOTH if j % 2 == 0 else GUM))
        for s in ('right', 'left', 'front'):
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                if i % 2 == 1:
                    p.px((fx + i, fy), TOOTH)

    if style == 'eye':                                            # amber eyes, a slit pupil
        scaly(p, f, cell=2)
        w.eye(p, f['front'], 0, 0, f['front'][2], f['front'][3], EYE, slit=True, glow=1.0)

    if style == 'brow':
        scaly(p, f, cell=2)
        w.flat(p, f, SCALE_EDGE, faces=('bottom',))

    if style == 'frill':                                          # a webbed frill: orange skin between dark spines, a ragged edge
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                spine = (i % 3 == 0)
                c = FRILL_DEEP if spine else mix(FRILL, (255, 180, 90), 0.3 * (1 - j / fh))
                p.px(xy, [v + p.rnd.randint(-5, 5) for v in c])
            for i in range(fw):                                   # cut to a fan, wider at its edge
                t = 1 - i / max(1, fw - 1) if s == 'front' else i / max(1, fw - 1)  # 1 at the root .. 0 at the edge
                half = fh / 2 * (0.45 + 0.55 * (1 - t))
                for j in range(fh):
                    if abs(j - fh / 2 + 0.5) > half or (t < 0.2 and i % 3 == 1 and j in (0, fh - 1)):
                        p.clear((fx + i, fy + j))
        w.flat(p, f, FRILL_DEEP, faces=('right', 'left', 'top', 'bottom'))

    if style == 'horn':
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                t = 1 - j / max(1, fh - 1)
                p.px(xy, k(mix((60, 54, 50), (210, 196, 160), t), w.LIGHT[s] * (0.9 if j % 2 else 1)))
        for s in w.SIDES:
            w.point(p, f[s], down=False, keep=0.3, sharp=0.45)

    if style == 'pot':                                            # a battered iron pot for a helmet, dented, rusted
        w.iron(p, f, IRON, rust=0.45, rivets=0, dents=0.6, rustc=RUST)
        fx, fy, fw, fh = f['front']                               # a dent, a patch
        for i in range(3, 6):
            p.px((fx + i, fy + 2), k(IRON, 0.5))
        fx, fy, fw, fh = f['top']
        for i, j, xy in w.each(p, f['top']):                      # wax run down over its top
            if math.hypot(i - fw / 2 + 0.5, j - fh / 2 + 0.5) < 2.2:
                p.px(xy, WAX)

    if style == 'potrim':
        w.iron(p, f, IRON_DARK, rust=0.3, rivets=0, rim=False, rustc=RUST)

    if style == 'drip':
        w.flat(p, f, WAX, noise=6)

    if style == 'candle':                                         # a stub of candle, dripping, its wick black
        w.flat(p, f, WAX, noise=5)
        for s in w.SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                for j in range(p.rnd.randint(0, fh)):
                    p.px((fx + i, fy + j), k(WAX, 1.05))
        w.flat(p, f, (40, 34, 30), faces=('top',))

    if style in ('flame', 'flame_x'):                             # the candle's flame: a teardrop of fire
        flat_ = ('right', 'left') if style == 'flame_x' else ('front', 'back')
        w.cut_all(p, f, [s for s in f if s not in flat_])
        for s in flat_:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                t = j / max(1, fh - 1)
                half = (fw / 2) * (0.25 + 0.75 * t)
                if abs(i - fw / 2 + 0.5) > half:
                    p.clear(xy)
                else:
                    p.lit(xy, mix((255, 250, 210), FLAME, 1 - t) if t > 0.5 else mix((255, 120, 40), FLAME, t * 2), 1.0)

    if style == 'pauldron':
        scrap(p, f, rivets=2)

    if style == 'greave':
        scrap(p, f, rivets=2)

    if style == 'bracer':
        w.leather(p, f, LEATHER, stitch=(170, 140, 96))
        for s in w.SIDES:
            w.hline(p, f[s], 1, k(IRON, w.LIGHT[s]))
            w.hline(p, f[s], f[s][3] - 2, k(IRON, w.LIGHT[s]))

    if style == 'socket':
        w.iron(p, f, IRON_DARK, rivets=0)

    if style in ('pick_point', 'pick_blade'):                     # the pick's iron head: a long point before, a blade behind
        w.iron(p, f, IRON, rust=0.25, rivets=0, dents=0.3, rustc=RUST)
        for s in ('top',):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                p.px((fx + fw // 2, fy + j), (196, 196, 200))
        for s in ('right', 'left'):                               # tapered to its point (the box's front end)
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                t = i / max(1, fw - 1)
                tip = (1 - t) if s == 'right' else t              # 'right' face: its front end at the left of the texture
                if style == 'pick_blade':
                    tip = 1 - tip
                keep = 0.25 + 0.75 * min(1, tip * 1.6)
                for j in range(fh):
                    if abs(j - fh / 2 + 0.5) > keep * fh / 2:
                        p.clear((fx + i, fy + j))

    if style == 'lantern':                                        # a red paper lantern: ribbed, glowing, a gold 福 on it
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                rib = i % 3 == 0 if s in w.SIDES else False
                t = abs(j - fh / 2 + 0.5) / (fh / 2)
                c = mix(LANTERN_GLOW, LANTERN, 0.35 + 0.65 * t)
                if rib:
                    c = k(LANTERN, 0.6)
                p.lit(xy, c, 0.85, skin=c)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            w.glyph(p, fx + fw // 2 - 2, fy + fh // 2 - 2, ['.##.', '####', '.##.', '#..#'], GOLD, 0.9)
    if style == 'lantern_cap':
        w.wood(p, f, (60, 36, 26))
        w.border(p, f, GOLD)
    if style == 'lantern_cord':
        w.flat(p, f, (40, 30, 26))
    if style == 'lantern_tassel':                                 # a red tassel, gold-bound
        for s in f:
            w.strands(p, f[s], k(LANTERN, w.LIGHT[s]), k(LANTERN, 0.6), k((240, 90, 60), w.LIGHT[s]))
            w.hline(p, f[s], 0, GOLD) if s in w.SIDES else None
            w.hline(p, f[s], 1, GOLD) if s in w.SIDES else None


if __name__ == '__main__':
    p = Painter(47)
    slots = paint(KOBOLD, p, paint_kobold)
    p.save('kobold')
    w.emit(KOBOLD, slots, w.JAVA, 'kobold')
    w.report('kobold', KOBOLD, SIZE, slots)
