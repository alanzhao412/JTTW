"""The Seven Spider Spirits (七个蜘蛛精), as silk_court.py: one frame, seven skins.

Seven sisters who passed for beautiful women: each tall and slender in the court dress of the Tang,
in layers: a high-waisted skirt falling to the ground in three tiers and a trailing train, an open
long coat (褙子) over it to her knees, its borders worked in her own flower, a cross-collared jacket,
great trailing sleeves, a gauze over-robe with a web worked into it, a long silk stole (披帛) looped
behind her shoulders and down through her arms, a sash with long ends. Each sister in her own silks
and her own flower: the eldest crimson over gold with peonies, the second rose over silver with plum
blossom, the third peach over jade with lotus, the fourth gold over crimson with clouds, the fifth
jade over pale gold with waves, the sixth azure over white with cranes' clouds, the seventh lilac
over midnight with webs.

Her hair dressed high in a double-looped chignon: a gold phoenix crowning it, combs, flowers at its
sides, gold pins hung with strings of beads (步摇) that sway. Her face fair and lovely, and a little
wrong: eyes a shade too large and too dark, a gleam of her colour in them; a flower mark (花钿) on
her brow, a small red mouth. And what she is shows: a diadem across her hairline set with eight small
gems that glow like a spider's eyes; a jade spool on her sash, whence her silk pours; and fanned
behind her shoulders, eight long jointed spider's legs, each in three segments banded in her colour,
bristled, knuckled at the joints, ending in dark hooked claws, that spread wide as she spins.

The skin is painted at four texels to the model pixel.

Run from the repository root: python assets_source/spider_spirits.py"""
import math

import beastgen as bg
from silk_court import (part, mirrored, paint, emit, SilkPainter, SIDES, LIGHTS, k, mixc, noisy, each, cloth, sheen_silk,
                        web_motif, edges, vedges, gold, cut_point, extent, JAVA)

bg.UNITS = 256
bg.S = 4
bg.TEX = bg.UNITS * bg.S
S = bg.S

SKIN = (248, 230, 220)
SKIN_SHADE = (226, 192, 182)
SKIN_DEEP = (196, 150, 146)
HAIR = (20, 14, 22)
HAIR_SHEEN = (86, 72, 102)
GOLD = (234, 188, 76)
GOLD_DEEP = (160, 110, 36)
JADE = (180, 230, 210)
PEARL = (244, 240, 230)
LIP = (196, 32, 54)
BONE = (236, 226, 208)
CLAW = (44, 26, 34)

# (name, coat, accent, skirt, stole, eye, motif) for each sister, eldest first
SISTERS = [
    ('eldest', (150, 22, 44), (236, 188, 80), (236, 200, 124), (226, 120, 120), (220, 40, 60), 'peony'),
    ('second', (214, 104, 140), (250, 236, 240), (226, 224, 236), (240, 170, 196), (230, 80, 150), 'plum'),
    ('third', (232, 144, 92), (250, 224, 170), (116, 178, 148), (246, 196, 150), (240, 130, 60), 'lotus'),
    ('fourth', (226, 182, 66), (176, 30, 40), (168, 38, 46), (240, 214, 130), (230, 170, 40), 'cloud'),
    ('fifth', (64, 140, 106), (236, 214, 140), (232, 220, 166), (160, 214, 186), (60, 200, 140), 'wave'),
    ('sixth', (70, 118, 190), (236, 240, 250), (234, 238, 248), (160, 196, 236), (80, 160, 240), 'cloud'),
    ('seventh', (150, 108, 196), (236, 226, 250), (62, 50, 98), (206, 180, 236), (180, 110, 240), 'web'),
]

ARM = (0.12, 0, 0.14)
FOREARM = (-0.55, -0.2, 0)
BODY_Y = 8.5


def sister_parts():
    ps = [
        part('body', 'root', (0, BODY_Y, 0), (0, 0, 0), [
            (-3, -11, -1.9, 6, 11, 3.8, 'bodice'),
            (-2.7, -9.4, -2.3, 5.4, 2.8, 0.6, 'bodice_front'),
            (-3.6, -11.2, -2.1, 7.2, 1.8, 4.2, 'shoulders'),
            (-3.3, -11.7, -2.25, 6.6, 1.8, 4.5, 'collar'),
            (-3.4, -3.6, -2.35, 6.8, 2.2, 4.7, 'sash_band'),
            (-1.1, -3.8, -3.05, 2.2, 2.6, 0.9, 'jewel'),
        ]),
        # the skirt, to the ground in three tiers, and its train
        part('skirt', 'body', (0, 0, 0), (0, 0, 0), [
            (-3.8, -1.4, -2.6, 7.6, 8.4, 5.2, 'skirt1'),
            (-4.8, 6.6, -3.4, 9.6, 6.2, 6.8, 'skirt2'),
            (-6.2, 12.6, -4.4, 12.4, 2.9, 8.8, 'skirt3'),
        ]),
        part('apron', 'skirt', (0, -1.2, -3), (-0.1, 0, 0), [(-2.2, 0, -0.4, 4.4, 16.4, 0.4, 'apron')]),
        part('train', 'skirt', (0, 8.5, 4.2), (0.42, 0, 0), [(-5.0, 0, 0, 10, 7.6, 0.6, 'train')]),
        # the open long coat over it, to her knees
        # the coat's open front: down over her breast to her sash, then hanging open, falling away from her in a drape
        part('right_coat', 'body', (-2.6, -10.6, -2.2), (-0.04, 0.06, 0.1), [(-1.6, 0, -0.35, 2.4, 10.4, 0.5, 'coat_front')]),
        part('right_coat_drape', 'right_coat', (-0.2, 10.2, 0), (-0.14, 0.42, 0.06), [(-2.0, 0, -0.35, 3.0, 9.6, 0.5, 'coat_drape')]),
        part('right_coat_lapel', 'right_coat_drape', (0.9, 0.2, 0), (0, -0.9, 0), [(-0.1, 0, -0.3, 1.3, 9.0, 0.4, 'coat_lapel')]),
        part('right_coat_side', 'body', (-3.5, -10.4, 0.0), (0, 0, 0.12), [(-0.5, 0, -2.2, 0.5, 19.0, 4.4, 'coat_side')]),
        part('coat_back', 'body', (0, -10.8, 2.0), (0.1, 0, 0), [(-3.8, 0, 0, 7.6, 19.8, 0.5, 'coat_back')]),
        part('right_sash_end', 'body', (-1.6, -2.4, -2.9), (-0.06, 0, 0.06), [(-0.6, 0, -0.2, 1.2, 13, 0.4, 'sash_end')]),
        part('gauze', 'body', (0, -10.8, 0), (0, 0, 0), [(-3.6, 0, -2.5, 7.2, 13.5, 5.0, 'gauze')]),
        part('head', 'body', (0, -11.5, -0.1), (0, 0, 0), [
            (-0.9, -1.6, -0.9, 1.8, 1.8, 1.8, 'neck'),
            (-2.2, -6.2, -2.3, 4.4, 5, 4.4, 'face'),
            (-2.5, -6.6, -0.4, 5, 5.8, 2.9, 'hair_back'),
            (-2.5, -6.8, -2.5, 5, 1.3, 4.8, 'hair_top'),
            (-2.35, -6.25, -2.55, 4.7, 0.6, 0.4, 'diadem'),
        ]),
        part('right_lock', 'head', (-2.3, -5.8, -1.5), (0, 0, 0.06), [(-0.45, 0, -0.45, 0.8, 6.5, 1.1, 'lock')]),
        part('right_earring', 'head', (-2.3, -2.6, -0.6), (0, 0, 0.05), [
            (-0.15, 0, -0.15, 0.3, 1.2, 0.3, 'dangle_chain'), (-0.45, 1.2, -0.45, 0.9, 1.4, 0.9, 'pearl')]),
        part('bun', 'head', (0, -6.7, 0.6), (-0.15, 0, 0), [
            (-2.1, -3.6, -1.6, 4.2, 3.8, 3.4, 'bun'),
            (-1.4, -5.6, -1.1, 2.8, 2.2, 2.4, 'bun_top'),
            (-2.3, -1.4, -1.9, 4.6, 0.9, 3.9, 'bun_band'),
        ]),
        part('right_loop', 'bun', (-1.8, -3.4, 0.1), (0, 0, -0.55), [(-0.9, -4.6, -0.9, 1.8, 4.6, 1.8, 'hair_loop')]),
        part('right_loop_tie', 'right_loop', (0, -4.4, 0), (0, 0, 0), [(-1.0, -0.6, -1.0, 2.0, 0.8, 2.0, 'bun_band')]),
        # the gold phoenix crowning her chignon, its tail fanned behind, a pearl in its beak
        part('phoenix', 'bun', (0, -5.6, -0.4), (-0.1, 0, 0), [
            (-0.7, -1.6, -1.6, 1.4, 1.6, 2.6, 'phoenix_body'),
            (-0.45, -2.6, -2.2, 0.9, 1.4, 1.0, 'phoenix_head'),
            (-0.3, -2.0, -2.9, 0.6, 0.5, 0.8, 'phoenix_beak'),
        ]),
        part('phoenix_tail', 'phoenix', (0, -1.2, 0.9), (0.6, 0, 0), [(-1.8, -0.2, 0, 3.6, 0.3, 4.2, 'phoenix_tail')]),
        part('right_phoenix_wing', 'phoenix', (-0.6, -1.0, -0.4), (0, 0.3, 0.7), [(-2.6, -0.15, -1.2, 2.6, 0.3, 2.4, 'phoenix_wing')]),
        part('phoenix_dangle', 'phoenix', (0, -2.0, -2.8), (0.3, 0, 0), [
            (-0.12, 0, -0.12, 0.24, 2.6, 0.24, 'dangle_chain'), (-0.4, 2.6, -0.4, 0.8, 0.8, 0.8, 'pearl')]),
        part('right_flower', 'bun', (-2.0, -1.6, -1.0), (0.2, 0.4, 0.3), [
            (-0.9, -0.9, -0.9, 1.8, 1.8, 1.8, 'hair_flower'), (-1.3, -0.4, -0.4, 2.6, 0.8, 0.8, 'hair_flower_petals'),
            (-0.4, -1.3, -0.4, 0.8, 2.6, 0.8, 'hair_flower_petals')]),
        part('right_pin', 'bun', (-1.6, -2.8, -1.2), (0.3, 0.5, 0.4), [
            (-4.2, -0.25, -0.25, 4.2, 0.5, 0.5, 'pin_shaft'),
            (-5.2, -0.8, -0.8, 1.6, 1.6, 1.6, 'pin_flower'),
        ]),
        part('right_dangle', 'right_pin', (-4.6, 0.6, 0), (-0.3, 0, -0.4), [
            (-0.12, 0, -0.12, 0.24, 3.4, 0.24, 'dangle_chain'), (-0.45, 3.3, -0.45, 0.9, 0.9, 0.9, 'dangle_bead'),
            (-0.12, 4.1, -0.12, 0.24, 1.6, 0.24, 'dangle_chain'), (-0.35, 5.6, -0.35, 0.7, 1.0, 0.7, 'pearl')]),
        part('right_dangle_2', 'right_pin', (-3.6, 0.6, 0), (-0.3, 0, -0.4), [
            (-0.12, 0, -0.12, 0.24, 2.2, 0.24, 'dangle_chain'), (-0.4, 2.1, -0.4, 0.8, 0.8, 0.8, 'pearl')]),
        part('comb', 'bun', (0, -1.8, -1.9), (0.2, 0, 0), [(-1.8, -1.4, -0.3, 3.6, 1.6, 0.5, 'comb')]),
        part('right_arm', 'body', (-3.6, -10.2, 0), ARM, [(-1.4, -0.7, -1.5, 2.4, 6.2, 3, 'sleeve_upper')]),
        part('right_forearm', 'right_arm', (-0.3, 5.4, 0), FOREARM, [
            (-1.05, 0, -1.05, 2, 4.4, 2, 'cuff'),
            (-0.85, 4.2, -0.9, 1.7, 2.2, 1.8, 'hand'),
        ]),
        part('right_sleeve', 'right_forearm', (0, 0.2, 0.6), (0.48, 0.1, -0.02), [(-2.0, -0.6, -1.2, 4.0, 13.5, 6.2, 'sleeve_big')]),
        part('right_stole', 'right_forearm', (-0.9, 2.6, 0.8), (0.4, 0, 0.15), [(-0.2, 0, -1.1, 0.4, 12, 2.2, 'stole')]),
        part('right_stole_tail', 'right_stole', (0, 11.8, 0), (-0.45, 0, 0.1), [(-0.2, 0, -1, 0.4, 7, 2, 'stole_tail')]),
        part('stole_back', 'body', (0, -10.4, 2.6), (0.18, 0, 0), [(-6.4, -0.2, -0.2, 12.8, 3.4, 0.4, 'stole_back')]),
        part('silk_stream', 'body', (0, -2.5, -3.4), (0, 0, 0), [(-0.45, -0.45, -14, 0.9, 0.9, 14, 'silk_stream')]),
        part('legs_mount', 'body', (0, -9.2, 1.8), (0, 0, 0), []),
    ]
    # eight long legs fanned behind her shoulders: each rises and turns out, bends down at its knee, and
    # again at its last joint, to a hooked claw
    for i in range(4):
        la, lb, lc = 10.5 - i * 0.5, 10.5 - i * 0.4, 6.0 - i * 0.3
        ps += [
            part('right_sleg_%d' % i, 'legs_mount', (-0.9, -0.6 + i * 1.5, 0), (1.2 - i * 0.32, -(0.45 + i * 0.32), 0), [
                (-0.75, -0.75, 0, 1.5, 1.5, la, 'sleg_a')]),
            part('right_sleg_tip_%d' % i, 'right_sleg_%d' % i, (0, 0, la - 0.3), (-(1.45 - i * 0.12), 0, 0), [
                (-0.55, -0.55, 0, 1.1, 1.1, lb, 'sleg_b'),
                (-0.85, -0.85, -0.6, 1.7, 1.7, 1.7, 'sleg_knee')]),
            part('right_sleg_claw_%d' % i, 'right_sleg_tip_%d' % i, (0, 0, lb - 0.2), (-0.55, 0, 0), [
                (-0.4, -0.4, 0, 0.8, 0.8, lc, 'sleg_c'),
                (-0.6, -0.6, -0.4, 1.2, 1.2, 1.2, 'sleg_knee')]),
            part('right_sleg_hook_%d' % i, 'right_sleg_claw_%d' % i, (0, 0, lc - 0.2), (-0.7, 0, 0), [
                (-0.25, -0.25, 0, 0.5, 0.5, 2.0, 'sleg_hook')]),
        ]
    return mirrored(ps)


# ── Painting ─────────────────────────────────────────────────────────────────

def motif(p, rect, kind, colour, ground, light=1.0, density=1.0, seed=0):
    """Her flower scattered over a face of silk, staggered, small: peonies, plum blossom, lotus, clouds,
    waves or webs."""
    fx, fy, fw, fh = rect
    step = int(7 * S / density)
    for row, y0 in enumerate(range(S * 2, fh - S, step)):
        for x0 in range(S * 2 + (row % 2) * step // 2, fw - S, step):
            cx, cy = fx + x0, fy + y0
            draw_motif(p, cx, cy, kind, colour, ground, light)


def draw_motif(p, cx, cy, kind, colour, ground, light=1.0, r=None):
    r = r or int(1.6 * S)
    hi = k(mixc(colour, (255, 255, 255), 0.35), light)
    lo = k(mixc(colour, ground, 0.35), light)
    centre = k(GOLD, light)
    for dj in range(-r - 1, r + 2):
        for di in range(-r - 1, r + 2):
            d = math.hypot(di, dj)
            a = math.atan2(dj, di)
            xy = (cx + di, cy + dj)
            if kind in ('peony', 'plum', 'lotus'):
                petals = 5 if kind == 'plum' else 8 if kind == 'peony' else 6
                edge = r * (0.65 + 0.35 * abs(math.cos(a * petals / 2)))
                if kind == 'lotus':
                    edge = r * (0.5 + 0.5 * max(0, math.cos(a * 3))) if dj <= 0 else r * 0.45
                if d < r * 0.28:
                    p.px(xy, centre)
                elif d < edge:
                    p.px(xy, hi if d < edge * 0.6 else lo)
                elif d < edge + 0.8 and kind == 'peony':
                    p.px(xy, k(mixc(colour, (0, 0, 0), 0.3), light))
            elif kind == 'cloud':
                for ox, oy, rr in ((-0.45, 0.1, 0.5), (0.0, -0.2, 0.6), (0.45, 0.1, 0.5)):
                    dd = math.hypot(di - ox * r, dj - oy * r)
                    if abs(dd - rr * r) < 0.9:
                        p.px(xy, hi)
                if abs(dj - 0.55 * r) < 0.7 and abs(di) < r:
                    p.px(xy, hi)
            elif kind == 'wave':
                if abs(math.hypot(di, dj - r * 0.6) - r * 0.6) < 0.8 and dj < r * 0.6:
                    p.px(xy, hi)
                if abs(math.hypot(di, dj - r * 0.6) - r * 0.25) < 0.7 and dj < r * 0.6:
                    p.px(xy, lo)
            elif kind == 'web':
                if d <= r:
                    spoke = abs(math.remainder(a, math.pi / 4)) * d
                    ring = abs(math.remainder(d, r / 2.5))
                    if spoke < 0.5 or ring < 0.45:
                        p.px(xy, hi)


def border(p, rect, colour, kind, ground, width=None, vertical=False, light=1.0):
    """A border of her accent with her flower worked along it."""
    fx, fy, fw, fh = rect
    width = width or 3 * S
    if vertical:
        for i in range(min(fw, width)):
            for j in range(fh):
                p.px((fx + i, fy + j), noisy(p, k(colour, light * (0.85 if i in (0, width - 1) else 1)), 2))
        for y in range(width // 2 + S, fh - S, width + S):
            draw_motif(p, fx + width // 2, fy + y, kind, ground, colour, light, r=width // 2 - 1)
    else:
        for j in range(max(0, fh - width), fh):
            for i in range(fw):
                p.px((fx + i, fy + j), noisy(p, k(colour, light * (0.85 if j in (fh - width, fh - 1) else 1)), 2))
        for x in range(width // 2 + S, fw - S, width + S):
            draw_motif(p, fx + x, fy + fh - width // 2 - 1, kind, ground, colour, light, r=width // 2 - 1)


def sister_painter(name, coat, accent, skirt, stole, eye, kind):
    def silk(p, f, base, step=4 * S, pattern=True, density=1.0):
        for s, r in f.items():
            L = LIGHTS[s]
            sheen_silk(p, r, k(base, L), k(mixc(base, (255, 255, 255), 0.5), L), step=step)
            if pattern and s in SIDES:
                motif(p, r, kind, accent, base, light=L, density=density)

    def paint_box(p, style, f, dims):
        if style in ('bodice', 'bodice_front'):
            cloth(p, f, mixc(skirt, (255, 255, 255), 0.25))
            if style == 'bodice':                                    # an embroidered breast-band at the top of the skirt
                for s in ('front', 'back'):
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        for j in range(fh - 8 * S, fh):
                            p.px((fx + i, fy + j), noisy(p, k(skirt, 0.95), 3))
                    for x in range(2 * S, fw - S, 4 * S):
                        draw_motif(p, fx + x, fy + fh - 5 * S, kind, accent, skirt, r=int(1.6 * S))
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 8 * S), accent)
        elif style == 'shoulders':
            silk(p, f, coat, density=1.4)
            edges(p, f, accent, top=False, width=S)
        elif style == 'collar':
            cloth(p, f, coat)
            fx, fy, fw, fh = f['front']                              # crossed at the breast, edged in her accent
            for i, j, xy in each(f['front']):
                if abs((i - fw / 2) - (j - fh)) < S or abs((fw / 2 - i) - (j - fh)) < S:
                    p.px(xy, accent)
            edges(p, f, accent, top=True, bottom=False, width=S)
        elif style == 'sash_band':
            cloth(p, f, accent)
            edges(p, f, k(accent, 0.7), width=S // 2)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(S, fw - S, 3 * S):
                    draw_motif(p, fx + i, fy + fh // 2, kind, coat, accent, r=S)
        elif style == 'jewel':                                       # the jade spool her silk comes from: pale, faintly glowing
            for s, r in f.items():
                for i, j, xy in each(r):
                    c = mixc(JADE, (255, 255, 255), 0.4 if (i + j) % 3 == 0 else 0)
                    p.px(xy, noisy(p, k(c, LIGHTS[s]), 3), glow=s == 'front')
        elif style.startswith('skirt') or style == 'train':
            silk(p, f, skirt, density=0.9)
            if style in ('skirt3', 'train'):
                for s in SIDES:
                    border(p, f[s], coat, kind, accent, width=min(f[s][3] - S, 4 * S) if style == 'skirt3' else 4 * S)
            if style == 'skirt2':                                    # long pleats
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(0, fw, 3 * S):
                        for j in range(fh):
                            p.px((fx + i, fy + j), k(skirt, LIGHTS[s] * 0.8))
            if style == 'skirt1':
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        for j in range(S):
                            p.px((fx + i, fy + j), accent)
        elif style == 'apron':
            silk(p, f, k(skirt, 0.92), pattern=False)
            fx, fy, fw, fh = f['front']                              # a panel embroidered with her flower, large
            draw_motif(p, fx + fw // 2, fy + int(fh * 0.55), kind, accent, skirt, r=int(fw * 0.32))
            draw_motif(p, fx + fw // 2, fy + int(fh * 0.3), kind, coat, skirt, r=int(fw * 0.2))
            vedges(p, f['front'], accent, S // 2)
            border(p, f['front'], coat, kind, accent, width=3 * S)
        elif style == 'coat_lapel':                                    # its edge turned back: her accent, lined
            cloth(p, f, accent)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    p.px((fx, fy + j), k(accent, 0.7))
        elif style in ('coat_front', 'coat_drape', 'coat_side', 'coat_back'):
            silk(p, f, coat, density=1.2)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                if fw < 4 * S or fh < 4 * S:
                    continue
                border(p, f[s], accent, kind, coat, width=3 * S)
                if style in ('coat_front', 'coat_drape') and s == 'front':           # the open edge, bordered all down it
                    border(p, f[s], accent, kind, coat, width=3 * S, vertical=True)
            if style == 'coat_back':
                fx, fy, fw, fh = f['back']
                web_motif(p, (fx, fy + 4 * S, fw, fh // 2), mixc(accent, (255, 255, 255), 0.4), cy=fh * 0.2, r=fw * 0.45)
        elif style == 'gauze':                                       # the over-robe of fine gauze: pale, a web worked in it, half clear
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    if s in ('top', 'bottom'):
                        p.clear(xy)
                        continue
                    if s == 'front' and abs(i - fw / 2 + 0.5) < fw * 0.3:  # open down the front
                        p.clear(xy)
                        continue
                    p.px(xy, noisy(p, mixc(stole, (255, 255, 255), 0.55), 3), a=80)
                if s in ('back', 'right', 'left'):
                    web_motif(p, r, (255, 255, 255), cy=fh * 0.35, r=max(fw, fh) * 0.55, spokes=10)
                    for i, j, xy in each(r):
                        if p.img.getpixel(xy)[:3] == (255, 255, 255):
                            p.px(xy, (255, 255, 255), a=200)
        elif style in ('sleeve_upper', 'cuff'):
            silk(p, f, coat, density=1.4)
            if style == 'cuff':
                edges(p, f, accent, top=False, width=S)
        elif style == 'sleeve_big':
            silk(p, f, coat, density=1.1)
            for s in SIDES:                                          # a broad cuff band of her accent with her flower
                border(p, f[s], accent, kind, coat, width=4 * S)
            fx, fy, fw, fh = f['bottom']
            for i, j, xy in each(f['bottom']):                        # its mouth, lined in her skirt's colour, dark within
                inner = S <= i < fw - S and S <= j < fh - S
                p.px(xy, k(skirt, 0.35) if inner else k(skirt, 0.8))
        elif style == 'hand':
            cloth(p, f, SKIN, folds=False, fall=0.1)
            fx, fy, fw, fh = f['bottom']
            for i in range(0, fw, 2):
                p.px((fx + i, fy + fh - 1), (200, 60, 80))            # her nails, lacquered
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    if i % S == 0:
                        for j in range(fh // 2, fh):
                            p.px((fx + i, fy + j), SKIN_SHADE)
                    p.px((fx + i, fy + fh - 1), (190, 50, 76))
        elif style in ('stole', 'stole_tail', 'stole_back'):
            for s, r in f.items():
                sheen_silk(p, r, k(stole, LIGHTS[s]), (255, 255, 255), step=3 * S)
                if s in SIDES and r[2] >= 4 * S:
                    motif(p, r, kind, mixc(stole, (255, 255, 255), 0.5), stole, density=1.5)
            if style == 'stole_tail':
                for s in SIDES:
                    cut_point(p, f[s], keep=0.5, sharp=0.4)
        elif style == 'sash_end':
            cloth(p, f, accent)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for j in range(0, fh, 3 * S):
                    for i in range(fw):
                        p.px((fx + i, fy + j), k(coat, 0.9))
                cut_point(p, f[s], keep=1, sharp=0.6)
        elif style == 'neck':
            cloth(p, f, SKIN, folds=False, fall=0.15)
        elif style == 'face':
            paint_face(p, f, eye)
        elif style in ('hair_back', 'hair_top', 'lock', 'bun', 'bun_top', 'hair_loop'):
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    strand = (i + (j // (2 * S))) % (2 * S)
                    c = HAIR_SHEEN if strand < S // 2 and s in ('top', 'right', 'left', 'back') else HAIR
                    if style in ('bun', 'hair_loop') and abs(math.sin((i + j) * 0.35)) > 0.88:
                        c = HAIR_SHEEN
                    p.px(xy, noisy(p, k(c, LIGHTS[s] * (1.05 - 0.2 * j / max(1, fh))), 3))
            if style == 'hair_back':
                fx, fy, fw, fh = f['front']                          # its face side, hidden behind hers
                for i, j, xy in each(f['front']):
                    p.px(xy, HAIR)
            if style == 'lock':
                for s in SIDES:
                    cut_point(p, f[s], keep=1.0, sharp=0.35)
        elif style == 'bun_band':
            gold(p, f, GOLD)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(S, fw, 3 * S):
                    for dj in range(fh):
                        p.px((fx + i, fy + dj), mixc(eye, (255, 255, 255), 0.3), glow=False)
        elif style == 'diadem':                                       # gold, with eight little gems: a spider's eyes, if one looks
            gold(p, f, GOLD)
            fx, fy, fw, fh = f['front']
            for g in range(8):
                x = fx + int(round((g + 0.5) * fw / 8))
                c = mixc(eye, (255, 255, 255), 0.25)
                big = 2 if g in (3, 4) else 1
                for di in range(-big, big):
                    for dj in range(0, min(fh, 2 + big)):
                        p.px((x + di, fy + dj), c if dj else mixc(c, (255, 255, 255), 0.5), glow=True)
        elif style in ('pin_shaft', 'dangle_chain', 'comb', 'phoenix_body', 'phoenix_head', 'phoenix_wing', 'phoenix_tail', 'phoenix_beak'):
            gold(p, f, GOLD)
            if style == 'comb':
                fx, fy, fw, fh = f['front']
                for x in range(2 * S, fw - S, 4 * S):
                    draw_motif(p, fx + x, fy + fh // 2, kind, eye, GOLD, r=S + 1)
            if style in ('phoenix_tail', 'phoenix_wing'):              # feathers: rays of gold, eyes of her colour
                for s in ('top', 'bottom'):
                    fx, fy, fw, fh = f[s]
                    for i, j, xy in each(f[s]):
                        a = math.atan2(j + 0.5, i + 0.5 - fw / 2)
                        if abs(math.sin(a * 7)) < 0.25:
                            p.px(xy, GOLD_DEEP)
                        if style == 'phoenix_tail' and abs(j - fh * 0.8) < S * 0.6 and abs(math.sin(a * 3.5)) > 0.8:
                            p.px(xy, mixc(eye, (255, 255, 255), 0.2), glow=True)
                    cut_point(p, f[s], keep=1, down=False, sharp=0.5) if style == 'phoenix_wing' else None
            if style == 'phoenix_head':
                fx, fy, fw, fh = f['right']
                p.px((fx + 1, fy + 1), (200, 30, 40))
                fx, fy, fw, fh = f['left']
                p.px((fx + fw - 2, fy + 1), (200, 30, 40))
        elif style in ('pin_flower', 'hair_flower', 'hair_flower_petals'):
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    d = math.hypot(i + 0.5 - fw / 2, j + 0.5 - fh / 2) / (min(fw, fh) / 2)
                    c = GOLD if d < 0.35 else mixc(eye, (255, 255, 255), 0.25 + 0.3 * d) if style != 'pin_flower' else accent
                    if style == 'pin_flower' and d >= 0.35:
                        c = mixc(accent, coat, 0.3 if (i + j) % 2 else 0)
                    p.px(xy, noisy(p, k(c, LIGHTS[s]), 3))
        elif style in ('dangle_bead', 'pearl'):
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    c = JADE if style == 'dangle_bead' else PEARL
                    if i < fw / 2 and j < fh / 2:
                        c = mixc(c, (255, 255, 255), 0.5)
                    p.px(xy, noisy(p, k(c, LIGHTS[s]), 3))
        elif style == 'silk_stream':                                  # the silk pouring from her: white, glowing
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, (255, 252, 244) if (i + j) % 3 else (226, 222, 214), glow=True)
        elif style.startswith('sleg') and style != 'sleg_spine':
            band = k(coat, 0.75)
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    along = (j + 0.5) / fh if s in ('right', 'left', 'top', 'bottom') else 0.5
                    c = BONE
                    if style == 'sleg_knee':
                        c = mixc(band, (20, 10, 16), 0.35)
                    elif style == 'sleg_a':
                        c = band if (j // (2 * S)) % 3 == 0 else BONE
                    elif style == 'sleg_b':
                        c = mixc(BONE, band, 0.6) if (j // (2 * S)) % 4 == 0 else mixc(BONE, (120, 96, 100), along * 0.6)
                    elif style == 'sleg_c':
                        c = mixc((150, 120, 120), CLAW, along)
                    elif style == 'sleg_hook':
                        c = mixc(CLAW, (10, 6, 8), along)
                    edge = s in ('right', 'left') and (i == 0 or i == fw - 1)
                    if edge and style != 'sleg_knee':
                        c = k(c, 0.8)
                    if s in ('top',) and style in ('sleg_a', 'sleg_b') and (i == fw // 2):
                        c = mixc(c, (255, 255, 255), 0.4)                # a glint down its upper edge
                    if style in ('sleg_a', 'sleg_b') and p.rnd.random() < 0.04:
                        c = (40, 26, 30)                                  # bristles
                    p.px(xy, noisy(p, k(c, LIGHTS[s]), 3))
            if style == 'sleg_hook':
                for s in SIDES:
                    cut_point(p, f[s], keep=0.5, sharp=0.6)
        elif style == 'sleg_spine':
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, k(CLAW, LIGHTS[s]), 3))
        else:
            cloth(p, f, coat)
    return paint_box


def paint_face(p, f, eye):
    """A lovely face, a little wrong: pale skin, a faint blush, finely painted brows arched long, eyes
    a shade too large and too dark under heavy lashes, a gleam of her colour in them (glowing faintly),
    a small red mouth in the Tang fashion, a flower mark between her brows."""
    for s_, r in f.items():
        for i, j, xy in each(r):
            p.px(xy, noisy(p, k(SKIN, LIGHTS[s_] * (1.02 - 0.08 * (j / max(1, r[3])))), 2))
    for s_ in ('right', 'left'):                                     # her hair over her temples and ears
        fx, fy, fw, fh = f[s_]
        for i, j, xy in each(f[s_]):
            back = i > fw * 0.45 if s_ == 'right' else i < fw * 0.55
            if j < fh * 0.2 or (back and j < fh * 0.8):
                p.px(xy, noisy(p, HAIR, 3))
    for i, j, xy in each(f['top']):
        p.px(xy, noisy(p, HAIR, 3))
    fx, fy, fw, fh = f['front']                                      # 18 x 20 at four texels
    cx = fw / 2
    for i in range(fw):                                              # the hairline: a soft widow's peak, parted wings at her temples
        u = abs(i + 0.5 - cx) / cx
        line = int(fh * (0.1 + 0.08 * u * u)) if u < 0.75 else int(fh * (0.1 + 0.4 * (u - 0.75) / 0.25))
        if u < 0.12:
            line += 1
        for j in range(line):
            p.px((fx + i, fy + j), noisy(p, HAIR if (i + j) % 5 else HAIR_SHEEN, 2))
    # the flower mark: a little red three-petalled bloom
    mx, my = fx + int(cx), fy + int(fh * 0.24)
    for di, dj in ((-1, 0), (0, 0), (0, -1), (-1, -1), (-1, 1), (0, 1), (-2, 0), (1, 0)):
        p.px((mx + di, my + dj), (220, 36, 60) if abs(di + 0.5) + abs(dj) < 2 else (240, 110, 120))
    # brows: fine willow-leaf arcs, soft, high above the eyes, fading at their tails
    for side in (-1, 1):
        n = int(fw * 0.3)
        for t in range(n):
            x = int(cx) + side * (2 + t) - (1 if side < 0 else 0)
            y = fy + int(fh * 0.27) - int(round(1.8 * math.sin(math.pi * (t + 1) / (n + 1))))
            fade = 0.85 if t < n - 2 else 0.5
            p.px((x, y), mixc(SKIN, (92, 62, 80), fade))
    # the eyes: too large, too dark, black-lashed, a gleam of her colour, a white point of light
    for side in (-1, 1):
        ew, eh = int(fw * 0.3), max(3, int(fh * 0.21))
        x0 = int(cx) + side * 2 - (ew if side < 0 else 0) + (0 if side < 0 else 0)
        y0 = fy + int(fh * 0.42)
        for i in range(ew):
            u = (i + 0.5) / ew * 2 - 1
            half = (1 - u * u) ** 0.6 * eh / 2
            for j in range(-int(half) - 1, int(half) + 1):
                xy = (fx + x0 + i, y0 + eh // 2 + j)
                if j <= -int(half) - 1 + 0:
                    p.px(xy, (20, 10, 16))                          # the lash line, heavy
                    continue
                iris = abs(u + side * 0.1) < 0.62
                if iris:
                    d = math.hypot(u + side * 0.1, j / max(1, eh / 2))
                    c = mixc((14, 8, 14), eye, max(0, 0.5 - d * 0.5)) if d > 0.25 else (8, 4, 8)
                    p.px(xy, c)
                    if 0.3 < d < 0.55:
                        p.glow.putpixel((int(xy[0]), int(xy[1])), tuple(int(v * 0.6) for v in eye) + (255,))
                else:
                    p.px(xy, (240, 232, 228))
        gx = fx + x0 + ew // 2 + (-1 if side < 0 else 0)
        p.px((gx, y0 + eh // 2 - 1), (255, 255, 255))                  # the point of light
        for i in range(ew):                                            # a soft shadow under, a hint of colour over the lid
            p.px((fx + x0 + i, y0 + eh + 1), mixc(SKIN, SKIN_SHADE, 0.7))
            p.px((fx + x0 + i, y0 - 1), mixc(SKIN, eye, 0.18))
    # the nose: a faint shadow, a fine tip
    for j in range(int(fh * 0.6), int(fh * 0.72)):
        p.px((fx + int(cx) - 1, fy + j), mixc(SKIN, SKIN_SHADE, 0.5))
    p.px((fx + int(cx) - 1, fy + int(fh * 0.73)), SKIN_DEEP)
    p.px((fx + int(cx), fy + int(fh * 0.73)), SKIN_SHADE)
    # a blush high on each cheek
    for side in (-1, 1):
        for di in range(-2, 3):
            for dj in range(-1, 2):
                xy = (fx + int(cx) + side * int(fw * 0.32) + di, fy + int(fh * 0.66) + dj)
                p.px(xy, mixc(SKIN, (240, 150, 160), 0.4 - 0.08 * abs(di)))
    # a small red mouth, the lower lip full, the upper a bow
    my = fy + int(fh * 0.82)
    for i in range(-2, 2):
        p.px((fx + int(cx) + i, my), LIP if abs(i + 0.5) < 1.6 else mixc(SKIN, LIP, 0.5))
        p.px((fx + int(cx) + i, my + 1), mixc(LIP, (255, 255, 255), 0.15) if abs(i + 0.5) < 1.2 else mixc(SKIN, LIP, 0.4))
    p.px((fx + int(cx) - 1, my - 1), mixc(SKIN, LIP, 0.6))
    p.px((fx + int(cx), my - 1), mixc(SKIN, LIP, 0.6))
    p.px((fx + int(cx) - 1, my + 2), mixc(SKIN, SKIN_SHADE, 0.6))


if __name__ == '__main__':
    ps = sister_parts()
    for i, (name, coat, accent, skirt, stole, eye, kind) in enumerate(SISTERS):
        p = SilkPainter(50 + i)
        slots = paint(ps, p, sister_painter(name, coat, accent, skirt, stole, eye, kind))
        p.save('spider_spirit_' + name)
    emit(ps, slots, JAVA, 'sister')
    print('spider spirit', len(ps), 'parts', {a: round(b, 2) for a, b in extent(ps, 1.08).items()})
