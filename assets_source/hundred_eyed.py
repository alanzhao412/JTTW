"""The Hundred-Eyed Demon Lord (百眼魔君), the Many-Eyed Daoist of the Yellow Flower Temple, as
silk_court.py: his model and skins come from one list of parts, written into SpiderCaveRenderers.

A centipede spirit in a Daoist's shape, towering some three and a half blocks: from the waist up a
gaunt Daoist in a robe of temple yellow, its cross collar black edged in gold, the eight trigrams
round its hem and the taiji on its back, great sleeves; a sallow, long face with high cheekbones,
narrow slanting eyes and a third eye shut on his brow, a thin moustache falling past his chin and a
long narrow beard, his black hair in a topknot under a gold lotus crown and a jade pin; two long
feelers sweeping back from his crown, and venom claws at the corners of his mouth; his Daoist sword
(剑) with its gold guard and red tassel in his right hand. From the waist down he is the centipede:
under a short apron the body runs out behind him in seven armoured segments, umber and black banded
with gold, each with a pair of jointed legs, to two long tail-spines. Thrown off, the robe shows what
it hid: his torso and arms plated like his body, set all over with golden eyes, rows down his flanks
and ribs and along his arms, that blaze when he raises them (the third skin: his eyes ablaze).

Run from the repository root: python assets_source/hundred_eyed.py"""
import math

import beastgen as bg
from silk_court import (part, mirrored, paint, emit, SilkPainter, SIDES, LIGHTS, k, mixc, noisy, each, chitin, cloth, sheen_silk,
                        edges, vedges, gold, gem_eye, cut_point, extent, JAVA)
from sea_court import aim

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

YELLOW = (214, 168, 52)
YELLOW_DEEP = (150, 106, 30)
BLACK = (26, 20, 18)
GOLD = (236, 190, 76)
SKIN = (196, 168, 110)        # sallow
SKIN_SHADE = (150, 120, 76)
HAIR = (20, 16, 18)
HAIR_SHEEN = (80, 70, 74)
UMBER = (92, 46, 22)
UMBER_DARK = (40, 20, 12)
SEG_BAND = (186, 132, 46)
EYE = (255, 206, 70)
EYE_HOT = (255, 250, 210)
JADE = (120, 200, 160)
RED = (176, 30, 30)
STEEL = (200, 206, 214)

ARM, FOREARM = (0.05, 0, 0.12), (-0.9, -0.35, 0)
LARM, LFOREARM = ((x, -y, -z) for x, y, z in (ARM, FOREARM))
SEGMENTS = 7


def lord_parts():
    ps = [
        # the centipede's first segment carries him: its pivot under his waist
        part('seg_0', 'root', (0, 18, 0), (0, 0, 0), [(-6.5, -4.5, -4.5, 13, 9, 9, 'segment_0'),
                                                       (-5.5, -5.2, -4, 11, 1, 8, 'seg_plate')]),
        part('waist', 'seg_0', (0, -4.5, -1.5), (0, 0, 0), [(-5.2, -3, -3.2, 10.4, 3.4, 6.4, 'belly')]),
        part('torso', 'waist', (0, -2.6, 0), (0, 0, 0), [
            (-6, -15, -3.6, 12, 15, 7.2, 'torso'),
            (-3.6, -16.4, -2.4, 7.2, 1.8, 4.8, 'neck_base'),
        ] + [(-6.3, -13 + 2.4 * i, -2.2 + (i % 2) * 2.2, 0.7, 1.4, 1.6, 'flank_eye') for i in range(5)]
          + [(5.6, -13 + 2.4 * i, -2.2 + (i % 2) * 2.2, 0.7, 1.4, 1.6, 'flank_eye') for i in range(5)]
          + [(-4 + 2.6 * i, -10 + (i % 2) * 2.6, -3.95, 1.6, 1.2, 0.5, 'chest_eye') for i in range(4)]),
        part('robe', 'torso', (0, 0, 0), (0, 0, 0), [
            (-6.6, -15.5, -4.2, 13.2, 15.8, 8.4, 'robe_torso'),
            (-6.9, -16.2, -4.5, 13.8, 2.4, 9, 'robe_collar'),
            (-7.1, -3, -4.6, 14.2, 2.4, 9.2, 'robe_belt'),
        ]),
        part('skirt', 'waist', (0, 0, 0), (0, 0, 0), [
            (-7.2, -0.6, -4.8, 14.4, 6, 9.6, 'robe_skirt1'),
            (-8.2, 5, -5.6, 16.4, 4, 11.2, 'robe_skirt2'),
        ]),
        part('apron', 'skirt', (0, -0.4, -5), (-0.12, 0, 0), [(-3.2, 0, -0.5, 6.4, 10, 0.5, 'robe_apron')]),
        part('head', 'torso', (0, -16, -0.6), (0, 0, 0), [
            (-1.8, -2, -1.8, 3.6, 2.4, 3.6, 'neck'),
            (-3.4, -9.4, -3.4, 6.8, 7.8, 6.4, 'face'),
            (-3.7, -9.8, -0.4, 7.4, 6.8, 3.8, 'hair_back'),
            (-3.7, -10, -3.6, 7.4, 1.4, 7, 'hair_top'),
        ]),
        part('right_brow', 'head', (-0.4, -6.9, -3.5), (0, 0.1, 0.32), [(-3.2, -0.5, -0.4, 3.2, 1, 1, 'brow')]),
        part('right_fang', 'head', (-2, -2.2, -3.2), (0, 0, 0.25), [(-0.5, 0, -0.5, 1, 2.6, 1, 'venom_claw')]),
        part('right_moustache', 'head', (-0.6, -3.3, -3.6), (0, 0.15, 0.1), [(-0.4, 0, -0.4, 0.8, 7, 0.8, 'moustache')]),
        part('beard', 'head', (0, -1.7, -3.2), (0.12, 0, 0), [(-0.9, 0, -0.7, 1.8, 9, 1.4, 'beard')]),
        part('beard_tip', 'beard', (0, 8.8, 0), (-0.1, 0, 0), [(-0.6, 0, -0.5, 1.2, 5, 1, 'beard_tip')]),
        part('crown', 'head', (0, -10, 0), (0, 0, 0), [
            (-1.6, -2.6, -1.6, 3.2, 2.6, 3.2, 'topknot'),
            (-2.4, -3.2, -2.5, 4.8, 2.2, 5, 'crown'),
            (-1.2, -4.6, -2.7, 2.4, 1.6, 1, 'crown_petal'),
            (-4.6, -2.2, -0.3, 9.2, 0.6, 0.6, 'hairpin'),
        ]),
        part('right_feeler', 'crown', (-1.2, -3, -1.6), (-0.9, 0.35, -0.25), [(-0.3, -9, -0.3, 0.6, 9, 0.6, 'feeler')]),
        part('right_feeler_tip', 'right_feeler', (0, -9, 0), (-0.75, 0, -0.1), [(-0.25, -11, -0.25, 0.5, 11, 0.5, 'feeler')]),
        # the arms: bare, plated and eyed, under great sleeves that fall away with the robe
        part('right_arm', 'torso', (-6.6, -13.6, 0), ARM, [(-3, -1, -2, 3.8, 8.6, 4, 'arm')]
             + [(-3.4, 1 + 2.3 * i, -1.2 + (i % 2) * 1.6, 0.6, 1.3, 1.3, 'arm_eye') for i in range(3)]),
        part('right_robe_sleeve', 'right_arm', (0, 0, 0), (0, 0, 0), [(-3.5, -1.5, -2.6, 4.8, 9.4, 5.2, 'robe_sleeve')]),
        part('right_forearm', 'right_arm', (-1.1, 7.8, 0), FOREARM, [
            (-1.6, 0, -1.6, 3.2, 7.6, 3.2, 'forearm'),
            (-2.0, 6.8, -2.0, 4, 1.6, 4, 'cuff_plate'),
        ] + [(-1.9, 1.6 + 2.6 * i, -0.6, 0.4, 1.2, 1.2, 'arm_eye') for i in range(2)]),
        part('right_great_sleeve', 'right_forearm', (0, 0.4, 0.8), aim([ARM, FOREARM], (0.05, -0.3, 0.12)), [
            (-3.4, -1, -8.8, 6.8, 12.5, 9.4, 'robe_sleeve_big')]),
        part('right_hand', 'right_forearm', (0, 8.2, 0), (0, 0, 0), [(-1.7, 0, -1.7, 3.4, 3, 3.4, 'claw_hand'),
                                                                     (-1.5, 2.8, -1.9, 0.8, 1.8, 0.8, 'talon'),
                                                                     (0.6, 2.8, -1.9, 0.8, 1.8, 0.8, 'talon')]),
        # his Daoist sword in his right hand, point up
        part('sword', 'right_hand', (0, 1.6, -0.4), aim([ARM, FOREARM], (-0.15, 0, 0)), [
            (-0.3, -26, -0.9, 0.6, 22, 1.8, 'blade'),
            (-0.25, -28, -0.6, 0.5, 2, 1.2, 'blade_tip'),
            (-0.8, -4.4, -2.2, 1.6, 1, 4.4, 'guard'),
            (-0.6, -3.4, -0.6, 1.2, 5, 1.2, 'grip'),
            (-0.8, 1.6, -0.8, 1.6, 1.2, 1.6, 'pommel'),
        ]),
        part('tassel', 'sword', (0, 2.8, 0), (0.1, 0, 0), [(-0.4, 0, -0.4, 0.8, 5, 0.8, 'tassel')]),
    ]
    # the centipede's body, behind him, segment on segment, each with its pair of legs
    prev = 'seg_0'
    for s in range(SEGMENTS + 1):
        name = 'seg_0' if s == 0 else 'seg_%d' % s
        if s > 0:
            w = 12.4 - s * 0.9
            ps.append(part(name, prev, (0, 0, 4.4 if s == 1 else 6.2), (0, 0, 0), [
                (-w / 2, -4 + s * 0.25, 0, w, 8 - s * 0.5, 6.6, 'segment'),
                (-w / 2 + 0.6, -4.6 + s * 0.25, 0.6, w - 1.2, 0.8, 5.2, 'seg_plate')]))
            prev = name
        lx = (12.4 - s * 0.9) / 2 if s > 0 else 6.5
        z = 3.3 if s > 0 else 0
        ps += [
            part('right_cleg_%d' % s, name, (-lx + 0.4, 1.6, z), (0, 0.15 * (s - 3) / 3, 0.45), [
                (-5.5, -0.8, -0.8, 5.5, 1.6, 1.6, 'cleg')]),
            part('right_cleg_tip_%d' % s, 'right_cleg_%d' % s, (-5.5, 0, 0), (0, 0, -1.25), [
                (-8.2, -0.55, -0.55, 8.2, 1.1, 1.1, 'cleg_tip'),
                (-1, -1, -1, 1.4, 2, 2, 'cleg_knee')]),
        ]
    ps.append(part('right_tailspine', prev, (-1.8, -0.5, 6.4), (0.15, -0.3, 0), [(-0.5, -0.5, 0, 1, 1, 9, 'tailspine')]))
    return mirrored(ps)


def eye_box(p, f, dims):
    """A domed golden eye on his plating: its iris and slit facing out, blazing in his glare."""
    for s, r in f.items():
        fx, fy, fw, fh = r
        for i, j, xy in each(r):
            if s in ('right', 'left', 'front'):
                u, v = (i + 0.5) / fw, (j + 0.5) / fh
                edge = min(u, 1 - u, v, 1 - v)
                if edge < 0.2:
                    p.px(xy, (60, 30, 14))
                elif abs(u - 0.5) < 0.14:
                    p.px(xy, (20, 8, 4))
                else:
                    p.lit(xy, k(EYE, 0.8 + 0.3 * (1 - v)), calm=0.55, blaze=1.0, hot=EYE_HOT)
            else:
                p.px(xy, noisy(p, UMBER_DARK, 3))


def painted_eyes(p, rect, n, seed=0, size=(3, 2)):
    """Rows of golden eyes over a face of plating, glowing faintly; ablaze in his glare."""
    fx, fy, fw, fh = rect
    w, h = size
    placed = 0
    for row in range(1, fh - h, h + 2):
        for col in range(1 + (row // (h + 2)) % 2 * 2, fw - w, w + 2):
            if placed >= n:
                return
            if (col * 7 + row * 3 + seed) % 5 == 0:
                continue
            for j in range(-1, h + 1):
                for i in range(-1, w + 1):
                    p.px((fx + col + i, fy + row + j), (54, 26, 12))
            gem_eye(p, fx + col, fy + row, EYE, w=w, h=h, calm=0.45)
            placed += 1


def lord_painter():
    def paint_box(p, style, f, dims):
        if style in ('torso', 'arm', 'forearm', 'belly', 'neck_base'):
            # his own hide under the robe: plated umber, banded gold at the seams, set with eyes
            chitin(p, f, UMBER, (220, 170, 110), plate=4)
            for s in ('front', 'right', 'left', 'back'):
                painted_eyes(p, f[s], 14 if style == 'torso' else 5, seed=len(s))
        elif style in ('flank_eye', 'chest_eye', 'arm_eye'):
            eye_box(p, f, dims)
        elif style in ('cuff_plate', 'claw_hand', 'talon'):
            chitin(p, f, UMBER_DARK if style != 'cuff_plate' else SEG_BAND, (230, 190, 130))
        elif style in ('robe_torso', 'robe_sleeve', 'robe_skirt1', 'robe_skirt2', 'robe_apron', 'robe_sleeve_big'):
            for s, r in f.items():
                sheen_silk(p, r, k(YELLOW, LIGHTS[s]), k((250, 220, 120), LIGHTS[s]), step=5)
            if style == 'robe_torso':
                taiji(p, f['back'])
                fx, fy, fw, fh = f['front']                           # the black cross collar running down to the belt
                for i, j, xy in each(f['front']):
                    if abs((i - fw / 2) + (j - fh * 0.15) * 0.5) < 2.2 and i < fw / 2 + 2 or abs((fw / 2 - i) + (j - fh * 0.15) * 0.5) < 2.2 and i > fw / 2 - 2:
                        if j < fh * 0.7:
                            p.px(xy, noisy(p, BLACK, 3))
            if style == 'robe_skirt2':
                for s in SIDES:
                    trigrams(p, f[s])
            if style == 'robe_apron':
                fx, fy, fw, fh = f['front']
                vedges(p, f['front'], BLACK, 2)
                taiji(p, (fx, fy + fh // 3, fw, fw))
            if style == 'robe_sleeve_big':
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        for j in range(fh - 4, fh):
                            p.px((fx + i, fy + j), noisy(p, BLACK if j < fh - 1 else GOLD, 3))
                fx, fy, fw, fh = f['bottom']
                for i, j, xy in each(f['bottom']):
                    p.px(xy, k(YELLOW_DEEP, 0.4))
        elif style in ('robe_collar', 'robe_belt'):
            cloth(p, f, BLACK)
            edges(p, f, GOLD)
            if style == 'robe_belt':
                fx, fy, fw, fh = f['front']
                for i in range(fw // 2 - 2, fw // 2 + 2):
                    for j in range(fh):
                        p.px((fx + i, fy + j), JADE)
        elif style == 'face':
            paint_lord_face(p, f)
        elif style == 'neck':
            cloth(p, f, SKIN, folds=False)
        elif style in ('hair_back', 'hair_top', 'topknot'):
            for s, r in f.items():
                for i, j, xy in each(r):
                    c = HAIR_SHEEN if (i + j // 3) % 4 == 0 else HAIR
                    p.px(xy, noisy(p, k(c, LIGHTS[s]), 3))
            if style == 'hair_back':
                for i, j, xy in each(f['front']):
                    p.px(xy, HAIR)
        elif style in ('crown', 'crown_petal'):
            gold(p, f, GOLD)
            if style == 'crown':
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(0, fw, 3):
                        p.px((fx + i, fy), (255, 240, 180))
                        p.px((fx + i + 1, fy + fh - 1), (140, 96, 30))
        elif style == 'hairpin':
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, k(JADE, LIGHTS[s]), 3))
        elif style == 'brow':
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, (30, 24, 22), 3))
        elif style in ('moustache', 'beard', 'beard_tip'):
            for s, r in f.items():
                for i, j, xy in each(r):
                    c = HAIR_SHEEN if i % 2 else (14, 12, 14)
                    p.px(xy, noisy(p, c, 3))
            for s in SIDES:
                cut_point(p, f[s], keep=0.5, sharp=0.35)
        elif style == 'venom_claw':
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, mixc((120, 30, 20), (230, 210, 170), (j + 0.5) / r[3]), 3))
        elif style == 'feeler':
            chitin(p, f, (70, 36, 18), gloss=False)
            for s in SIDES:
                for i, j, xy in each(f[s]):
                    if j % 3 == 0:
                        p.px(xy, SEG_BAND)
        elif style.startswith('segment'):
            chitin(p, f, UMBER, (230, 180, 110))
            for s in SIDES:                                          # a gold band round the segment's fore edge, eyes down its flanks
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    for j in range(2):
                        p.px((fx + i, fy + j), noisy(p, SEG_BAND, 4))
                if s in ('right', 'left'):
                    gem_eye(p, fx + fw // 2 - 1, fy + fh // 2, EYE, w=3, h=2, calm=0.4)
            fx, fy, fw, fh = f['top']
            for i, j, xy in each(f['top']):
                if abs(i - fw / 2 + 0.5) < 1:
                    p.px(xy, k(UMBER_DARK, 1))
        elif style == 'seg_plate':
            chitin(p, f, UMBER_DARK, (200, 160, 110))
            fx, fy, fw, fh = f['top']
            for i in range(1, fw - 1, 4):
                gem_eye(p, fx + i, fy + fh // 2 - 1, EYE, w=2, h=2, pupil=False, calm=0.4)
        elif style in ('cleg', 'cleg_tip', 'cleg_knee', 'tailspine'):
            chitin(p, f, (150, 96, 40) if style == 'cleg' else (110, 60, 26), (240, 200, 140))
            if style in ('cleg_tip', 'tailspine'):
                for s in ('right', 'left', 'top', 'bottom'):
                    fx, fy, fw, fh = f[s]
                    for i, j, xy in each(f[s]):
                        t = (i + 0.5) / fw if s in ('top', 'bottom') else 0
                        if s in ('top', 'bottom') and ((style == 'cleg_tip' and t < 0.3) or (style == 'tailspine' and t > 0.7)):
                            p.px(xy, (230, 190, 90))
        elif style in ('blade', 'blade_tip'):
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    c = k(STEEL, LIGHTS[s] * (1.15 if s in ('right', 'left') and abs(i - fw / 2) < 1 else 1.0))
                    p.px(xy, noisy(p, c, 3))
        elif style in ('guard', 'pommel'):
            gold(p, f, GOLD)
        elif style == 'grip':
            cloth(p, f, (60, 20, 24), folds=False)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(0, fh, 2):
                    for i in range(fw):
                        p.px((fx + i, fy + j), (30, 10, 12))
        elif style == 'tassel':
            cloth(p, f, RED)
        else:
            cloth(p, f, YELLOW)
    return paint_box


def taiji(p, rect):
    """The taiji, white and black, in a black ring, set in the middle of a face."""
    fx, fy, fw, fh = rect
    r = min(fw, fh) * 0.3
    cx, cy = fw / 2, fh * 0.42
    for i, j, xy in each(rect):
        x, y = i + 0.5 - cx, j + 0.5 - cy
        d = math.hypot(x, y)
        if d > r + 1:
            continue
        if d > r:
            p.px(xy, BLACK)
            continue
        upper = y < 0
        white = x > 0
        if math.hypot(x, y + r / 2) < r / 2:
            white = True
        if math.hypot(x, y - r / 2) < r / 2:
            white = False
        if math.hypot(x, y + r / 2) < r / 7:
            white = False
        if math.hypot(x, y - r / 2) < r / 7:
            white = True
        p.px(xy, (244, 236, 214) if white else BLACK)


TRIGRAMS = ['111', '011', '101', '001', '110', '010', '100', '000']


def trigrams(p, rect):
    """The eight trigrams in black round the hem, between gold lines."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        p.px((fx + i, fy + fh - 1), GOLD)
        p.px((fx + i, fy + 1), GOLD)
    step = max(6, fw // 4)
    for n, x0 in enumerate(range(2, fw - 4, step)):
        t = TRIGRAMS[n % 8]
        for row, bit in enumerate(t):
            y = fy + 3 + row * 2
            for i in range(4):
                if bit == '1' or i in (0, 3):
                    p.px((fx + x0 + i, y), BLACK)


def paint_lord_face(p, f):
    """A long sallow face: hollow cheeks, narrow slanting eyes glinting gold, a shut third eye on his
    brow (open and burning in his glare), a thin mouth, black hair over his temples."""
    for s, r in f.items():
        for i, j, xy in each(r):
            p.px(xy, noisy(p, k(SKIN, LIGHTS[s] * (1.05 - 0.15 * j / max(1, r[3]))), 3))
    for s in ('right', 'left'):
        fx, fy, fw, fh = f[s]
        for i, j, xy in each(f[s]):
            if j < 4 or (i > fw * 0.5 and j < fh * 0.7):
                p.px(xy, noisy(p, HAIR, 3))
    for i, j, xy in each(f['top']):
        p.px(xy, noisy(p, HAIR, 3))
    fx, fy, fw, fh = f['front']                                      # 14 x 16
    for i in range(fw):
        for j in range(3):
            if j < 2 or i < 2 or i >= fw - 2:
                p.px((fx + i, fy + j), HAIR)
    cx = fw // 2
    # the third eye, shut (a gold line), blazing open in his glare
    for i in (-1, 0):
        p.lit((fx + cx + i, fy + 4), (120, 80, 30), calm=0.0, blaze=1.0, hot=EYE_HOT)
    p.lit((fx + cx - 1, fy + 5), (90, 60, 30), calm=0.0, blaze=1.0, hot=EYE)
    p.lit((fx + cx, fy + 5), (90, 60, 30), calm=0.0, blaze=1.0, hot=EYE)
    # eyes: narrow, slanting up and out, gold irises in a dark rim
    for side in (0, 1):
        x0 = fx + (2 if side == 0 else fw - 6)
        y0 = fy + 7
        for i in range(4):
            slope = (3 - i) // 2 if side == 0 else i // 2
            p.px((x0 + i, y0 - slope), (20, 14, 10))
            if 0 < i < 3:
                p.lit((x0 + i, y0 + 1 - slope), EYE, calm=0.8, blaze=1.0, hot=EYE_HOT)
        p.px((x0 + (1 if side == 0 else 2), y0 + 1 - (1 if side == 0 else 1)), (20, 10, 6))
    # hollow cheeks, a long nose, the thin mouth
    for j in range(9, 13):
        p.px((fx + 2, fy + j), SKIN_SHADE)
        p.px((fx + fw - 3, fy + j), SKIN_SHADE)
    for j in range(7, 11):
        p.px((fx + cx, fy + j), mixc(SKIN, SKIN_SHADE, 0.5))
    p.px((fx + cx - 1, fy + 11), SKIN_SHADE)
    p.px((fx + cx + 1, fy + 11), SKIN_SHADE)
    for i in range(-2, 2):
        p.px((fx + cx + i, fy + 13), (90, 40, 34))


if __name__ == '__main__':
    ps = lord_parts()
    p = SilkPainter(88)
    slots = paint(ps, p, lord_painter())
    p.save('hundred_eyed_demon')
    emit(ps, slots, JAVA, 'lord')
    print('hundred-eyed demon', len(ps), 'parts', {a: round(b, 2) for a, b in extent(ps, 1.0).items()})
