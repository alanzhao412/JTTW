"""The Shrimp Soldier (虾兵), the Dragon King's foot soldier. As sea_court.py: its model and skin come
from one list of parts (beastgen): the model is written into ShrimpSoldierRenderer's layer() between
its GENERATED markers, the skin painted to the same box-UV layout at twice the model's resolution
(its fan, swimmerets and antennal scales half clear; the renderer draws it translucent), with a
second skin for the glint of its eyes drawn full-bright (shrimp_soldier_glow.png).

A shrimp at a glance, hunched forward on its jointed legs: a glossy, translucent orange shell
flecked with red; the carapace in front, a long serrated rostrum jutting from it, black eyes on
stalks, a pair of long whip antennae arching up and back over it, short antennules and the flat
antennal scales beside them, mouthparts working under its face; behind, the abdomen in six
segments curling down to a spread fan tail (the telson and its four uropods), swimmerets fringing
its underside; three pairs of small walking legs folded under the thorax. A soldier's kit: a
teal-lacquered bowl helmet rimmed in gold with a red plume, teal shoulder plates, a sea-blue sash
across the breast and a belt with a gold buckle, its ends trailing; two jointed arms ending in
little pincers, holding a fine spear: a steel leaf blade, a gold collar, a red horsehair tassel and
a small blue pennant marked with the sea's wave.

Run from the repository root: python assets_source/shrimp_soldier.py"""
import math

import beastgen as bg
from beastgen import Painter, emit
from fire_court import part, mirrored
from sea_court import aim
from king_yama import paint, point, ragged, circle, mixc, k, noisy

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/ShrimpSoldierRenderer.java'
OUT = 'src/main/resources/assets/jttw_engine/textures/entity/'

# How much larger than its pixels it is drawn: ShrimpSoldierRenderer.SIZE
SIZE = 0.88

SHELL = (255, 92, 30)
SHELL_PALE = (255, 184, 120)
SHELL_DEEP = (204, 32, 14)
SHELL_GLOSS = (255, 238, 214)
FLECK = (196, 38, 30)
BELLY = (250, 214, 176)
TEAL = (34, 112, 118)
TEAL_DEEP = (14, 56, 64)
TEAL_LIGHT = (90, 176, 170)
SEA = (44, 104, 170)
SEA_DEEP = (22, 54, 104)
GOLD = (232, 186, 70)
GOLD_DEEP = (156, 108, 36)
RED = (200, 36, 34)
RED_DEEP = (120, 16, 20)
STEEL = (196, 204, 214)
STEEL_DARK = (110, 118, 132)
SHAFT = (96, 30, 26)

ARM, FOREARM = (-0.55, 0, 0.2), (-0.9, 0.3, 0)
BODY_LEAN = 0.5

# the abdomen's six segments: width, height, length, the turn from the one before
SEGS = ((7.6, 6.4, 4.4, 0.35), (7.0, 6.0, 4.2, -0.4), (6.4, 5.4, 4.0, -0.4), (5.6, 4.8, 3.6, -0.4), (4.8, 4.0, 3.2, -0.4), (4.0, 3.4, 3.0, -0.4))


def abdomen():
    out = []
    parent = 'root'
    pivot = (0, 8.0, 2.6)
    for kk, (w, h, L, turn) in enumerate(SEGS):
        name = 'tail_%d' % (kk + 1)
        out.append(part(name, parent, pivot, (turn, 0, 0), [
            (-w / 2, -h / 2, 0, w, h, L, 'seg'),
            (-w / 2 - 0.3, -h / 2 - 0.35, -0.3, w + 0.6, h * 0.62, 1.3, 'tergite'),
        ]))
        if kk < 5:                                    # a pair of swimmerets under each of the first five
            out.append(part('right_swimmeret_%d' % (kk + 1), name, (-w / 2 + 1.1, h / 2 - 0.2, L * 0.45), (0.35, 0, 0.3),
                            [(-0.5, 0, -0.9, 1, 3.2 - kk * 0.2, 1.8, 'swimmeret')]))
        parent, pivot = name, (0, 0, L - 0.2)
    # the fan: the telson down the middle, two pairs of uropods spread either side of it
    out += [
        part('telson', 'tail_6', (0, 0, 2.8), (-0.15, 0, 0), [(-1.1, -0.5, 0, 2.2, 1, 6.4, 'telson')]),
        part('right_uropod', 'tail_6', (-0.9, 0, 2.6), (-0.15, -0.5, 0), [(-1.7, -0.3, 0, 3.4, 0.6, 6.4, 'uropod')]),
        part('right_uropod_in', 'tail_6', (-0.5, -0.1, 2.7), (-0.15, -0.22, 0), [(-1.4, -0.3, 0, 2.8, 0.6, 6.0, 'uropod_in')]),
    ]
    return out


def head():
    return [
        part('head', 'body', (0, -11.6, -0.6), (-0.35, 0, 0), [
            (-3.8, -5, -6.2, 7.6, 6.2, 7.6, 'headshell'),
            (-3.0, 0.8, -5.8, 6, 1.4, 5, 'mouthplate'),
        ]),
        # the rostrum: long, serrated, jutting forward and up
        part('rostrum', 'head', (0, -3.8, -6), (-0.32, 0, 0), [(-0.5, -1.2, -13, 1, 2, 13.4, 'rostrum')]),
        part('crest', 'head', (0, -5, -5.4), (0, 0, 0), [(-0.3, -1.2, 0, 0.6, 1.4, 6, 'crestspines')]),
        # stalked eyes
        part('right_eyestalk', 'head', (-2.4, -3.4, -5.4), (0.15, -0.55, 0.1), [(-2.6, -0.55, -0.55, 2.8, 1.1, 1.1, 'stalk')]),
        part('right_eye', 'right_eyestalk', (-2.6, 0, 0), (0, 0, 0), [(-1.3, -1.3, -1.3, 2.6, 2.6, 2.6, 'eye')]),
        # the long whip antennae, rising forward and arching back over it
        part('right_antenna', 'head', (-1.5, -2.2, -6.0), (-0.6, 0.32, 0), [(-0.45, -0.45, -5, 0.9, 0.9, 5, 'antenna_base')]),
        part('right_antenna_2', 'right_antenna', (0, 0, -4.8), (-0.8, 0.08, 0), [(-0.3, -0.3, -8, 0.6, 0.6, 8, 'antenna')]),
        part('right_antenna_3', 'right_antenna_2', (0, 0, -7.8), (-1.0, 0.3, 0), [(-0.25, -0.25, -11, 0.5, 0.5, 11, 'antenna')]),
        part('right_antenna_4', 'right_antenna_3', (0, 0, -10.8), (-0.5, 0.9, 0), [(-0.2, -0.2, -12, 0.4, 0.4, 12, 'antenna_tip')]),
        # the short, forked antennules, and the flat antennal scales
        part('right_antennule', 'head', (-0.9, -2.8, -6.0), (-0.25, 0.2, 0), [(-0.3, -0.3, -4.5, 0.6, 0.6, 4.5, 'antennule')]),
        part('right_antennule_a', 'right_antennule', (0, 0, -4.4), (-0.4, 0.15, 0), [(-0.2, -0.2, -5, 0.4, 0.4, 5, 'antenna_tip')]),
        part('right_antennule_b', 'right_antennule', (0, 0, -4.4), (0.15, -0.1, 0), [(-0.2, -0.2, -4, 0.4, 0.4, 4, 'antenna_tip')]),
        part('right_scale', 'head', (-2.2, -1.4, -6.0), (-0.15, 0.22, 0), [(-1.0, -0.2, -5.6, 2, 0.4, 5.6, 'antscale')]),
        # the mouthparts (maxillipeds), working under the face
        part('right_maxilliped', 'head', (-1.3, 1.6, -5.2), (-0.5, 0, 0.12), [(-0.4, 0, -0.4, 0.8, 4.2, 0.8, 'legthin')]),
        # the soldier's helmet: a teal bowl rimmed in gold, a red plume
        part('helmet', 'head', (0, -5, -1.2), (0.3, 0, 0), [
            (-4.0, -0.7, -4.0, 8, 1.0, 8, 'helmbrim'),
            (-3.2, -3.8, -3.2, 6.4, 3.2, 6.4, 'helmbowl'),
            (-0.7, -5.0, -0.7, 1.4, 1.3, 1.4, 'helmknob'),
            (-1.7, -8.2, -1.7, 3.4, 3.4, 3.4, 'plume'),
        ]),
        part('plume_tail', 'helmet', (0, -7, 1.2), (1.0, 0, 0), [(-1.4, 0, -0.8, 2.8, 6, 1.6, 'plumetail')]),
    ]


def legs():
    out = [
        # the two strong walking legs it stands on: a thigh forward, a shin back, a pointed foot
        part('right_leg', 'root', (-2.6, 8.4, 0.6), (-0.42, 0, 0.2), [
            (-1.0, 0, -1.0, 2.0, 7.6, 2.0, 'legseg'),
            (-1.25, -0.6, -1.25, 2.5, 1.6, 2.5, 'joint')]),
        part('right_shin', 'right_leg', (0, 7.4, 0), (0.78, 0, -0.12), [
            (-0.8, 0, -0.8, 1.6, 7.6, 1.6, 'legseg2'),
            (-1.05, -0.6, -1.05, 2.1, 1.4, 2.1, 'joint')]),
        part('right_foot', 'right_shin', (0, 7.4, 0), (-0.3, 0, 0), [(-0.55, 0, -0.55, 1.1, 2.2, 1.1, 'dactyl')]),
    ]
    # three pairs of small walking legs folded under the thorax
    for kk in range(3):
        y = -1.6 - 2.6 * kk
        out += [
            part('right_walker_%d' % kk, 'body', (-2.4 - 0.2 * kk, y, -3.6), (-0.2, 0.25, 0.75), [(-0.4, 0, -0.4, 0.8, 4.2, 0.8, 'legthin')]),
            part('right_walker_%d_tip' % kk, 'right_walker_%d' % kk, (0, 4.0, 0), (-1.0, 0, -0.5), [(-0.3, 0, -0.3, 0.6, 4.0, 0.6, 'legtip')]),
        ]
    return out


def arms():
    return [
        part('right_shoulder', 'body', (-4.4, -10.4, -0.6), (0, 0, 0.42), [
            (-3.4, -0.8, -2.8, 3.8, 1.4, 5.6, 'spauldron'),
            (-3.9, 0.4, -3.1, 4.2, 1.6, 6.2, 'spauldron2')]),
        part('right_arm', 'body', (-4.2, -9.2, -2.6), ARM, [
            (-0.95, 0, -0.95, 1.9, 6.4, 1.9, 'armseg'),
            (-1.2, -0.6, -1.2, 2.4, 1.4, 2.4, 'joint')]),
        part('right_forearm', 'right_arm', (0, 6.2, 0), FOREARM, [
            (-0.8, 0, -0.8, 1.6, 6, 1.6, 'armseg2'),
            (-1.05, -0.5, -1.05, 2.1, 1.2, 2.1, 'joint'),
            (-1.1, 3.0, -1.1, 2.2, 2.2, 2.2, 'bracer')]),
        part('right_hand', 'right_forearm', (0, 6, 0), (0, 0, 0), [
            (-1.1, 0, -1.1, 2.2, 2.4, 2.2, 'palm'),
            (-1.1, 2.2, -0.9, 0.9, 2.2, 1.0, 'finger'),
            (0.2, 2.2, -0.9, 0.9, 1.8, 1.0, 'finger')]),
        # the spear: a fine steel blade, a gold collar, a red tassel, a small pennant
        part('spear', 'right_hand', (0, 1.3, 0), aim([(BODY_LEAN, 0, 0), ARM, FOREARM, (0, 0, 0)], (0.12, 0, 0)), [
            (-0.45, -28, -0.45, 0.9, 36, 0.9, 'shaft'),
            (-0.8, -35.5, -0.25, 1.6, 7.6, 0.5, 'blade'),
            (-0.75, -28.8, -0.75, 1.5, 1.6, 1.5, 'collar'),
            (-1.3, -27.6, -1.3, 2.6, 4.4, 2.6, 'tassel'),
            (-0.6, 7.6, -0.6, 1.2, 1, 1.2, 'collar'),
            (-0.35, 8.4, -0.35, 0.7, 2, 0.7, 'butt'),
        ]),
        part('pennant', 'spear', (0.45, -22.6, 0), (0, 0, 0), [(0, 0, -0.1, 5, 6.4, 0.2, 'pennant')]),
    ]


SHRIMP = mirrored([
    # the thorax, leaning forward from the hips: the carapace over it, the breastplate of its underside,
    # a sea-blue sash across it and a belt about its foot
    part('body', 'root', (0, 8.4, 1.4), (BODY_LEAN, 0, 0), [
        (-4.4, -12, -3.4, 8.8, 12, 7.4, 'carapace'),
        (-3.5, -11.4, -3.9, 7, 10.8, 0.6, 'sternum'),
        (-0.6, -12.6, 2.6, 1.2, 1, 1.6, 'ridge'),
        (-4.7, -2.2, -4.0, 9.4, 1.8, 8.0, 'belt'),
        (-1.3, -2.6, -4.6, 2.6, 2.6, 0.8, 'buckle'),
    ]),
    part('sash', 'body', (0, -6.2, -4.1), (0, 0, 0.62), [(-1.1, -7.2, -0.3, 2.2, 14.4, 0.6, 'sash')]),
    part('right_sash_tail', 'body', (-2.4, -0.8, -4.3), (-0.1, 0, 0.12), [(-0.8, 0, -0.3, 1.6, 7.5, 0.6, 'sashtail')]),
] + head() + abdomen() + legs() + arms())


# ── Painting ─────────────────────────────────────────────────────────────────

class ShellPainter(Painter):
    def clear(self, xy):
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            self.img.putpixel((x, y), (0, 0, 0, 0))
            self.glow.putpixel((x, y), (0, 0, 0, 0))

    def pxa(self, xy, c, a):
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            self.img.putpixel((x, y), tuple(max(0, min(255, int(v))) for v in c[:3]) + (int(a),))


SIDES = ('front', 'back', 'right', 'left')
LIGHTS = {'top': 1.1, 'bottom': 0.7, 'front': 1.0, 'back': 0.86, 'right': 0.93, 'left': 0.93}


def shell(p, f, base=SHELL, segs=0, along='y', fleck=0.05, gloss=True, belly=False):
    """Glossy translucent shell: saturated and deep at the edges, paler and lit through at the middle,
    a streak of gloss along it, red pigment flecks; dark seams between `segs` segments."""
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                across = abs((i + 0.5) / fw - 0.5) * 2 if along == 'y' else abs((j + 0.5) / fh - 0.5) * 2
                t = 1 - across ** 1.6
                col = mixc(SHELL_DEEP, mixc(base, SHELL_PALE, 0.4 + 0.35 * t), t ** 0.7) if not belly else mixc(mixc(BELLY, SHELL, 0.4), BELLY, t)
                col = k(col, LIGHTS[s])
                if gloss and s in ('top', 'back', 'right', 'left', 'front') and 0.18 < (i + 0.5) / fw < 0.32 and along == 'y':
                    col = mixc(col, SHELL_GLOSS, 0.55)
                if gloss and along == 'x' and 0.18 < (j + 0.5) / fh < 0.32:
                    col = mixc(col, SHELL_GLOSS, 0.5)
                if p.rnd.random() < fleck:
                    col = mixc(col, FLECK, 0.6)
                p.px((fx + i, fy + j), noisy(p, col, 3))
        if segs:
            n = fh if along == 'y' else fw
            for kk in range(1, segs):
                c0 = int(n * kk / segs)
                for m in range(fw if along == 'y' else fh):
                    xy = (fx + m, fy + c0) if along == 'y' else (fx + c0, fy + m)
                    p.px(xy, k(SHELL_DEEP, 0.7 * LIGHTS[s]))
                    xy2 = (fx + m, fy + c0 + 1) if along == 'y' else (fx + c0 + 1, fy + m)
                    p.px(xy2, mixc(SHELL_PALE, SHELL_GLOSS, 0.4))


def fringe(p, rect, base, alpha=190, down=True, hairs=True):
    """A thin, half-clear paddle: tinted through, a darker vein down it, a fine fringe of hairs at its edge."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            edge = min(i, fw - 1 - i)
            t = j / max(1, fh - 1)
            col = mixc(base, SHELL_PALE, 0.3 + 0.4 * (1 - t))
            a = alpha
            if edge == 0 and hairs:
                col, a = mixc(col, (255, 230, 210), 0.5), alpha * 0.6
            if i == fw // 2:
                col = k(col, 0.75)
            p.pxa((fx + i, fy + j), noisy(p, col, 4), a)


def gold_box(p, f, light=1.0):
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                t = j / max(1, fh - 1)
                c = mixc((255, 232, 150), GOLD, min(1, t * 2)) if t < 0.5 else mixc(GOLD, GOLD_DEEP, (t - 0.5) * 2)
                p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s] * light), 4))


def steel_box(p, f, base=STEEL):
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                across = (i + 0.5) / fw
                c = mixc((240, 244, 252), base, min(1, across * 2.2)) if across < 0.5 else mixc(base, STEEL_DARK, (across - 0.5) * 2)
                p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))


def lacquer(p, f, base, deep, light_top=True):
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                t = j / max(1, fh - 1)
                col = mixc(mixc(base, (200, 240, 230), 0.25), deep, t * 0.8) if s in SIDES else base
                p.px((fx + i, fy + j), noisy(p, k(col, LIGHTS[s]), 3))


def rim(p, rect, colour, top=False, bottom=True):
    fx, fy, fw, fh = rect
    for i in range(fw):
        if top:
            p.px((fx + i, fy), colour)
        if bottom:
            p.px((fx + i, fy + fh - 1), colour)


def paint_shrimp(p, style, f, dims):
    if style == 'carapace':                                       # the carapace: glossy, its grooves, flecked
        shell(p, f, fleck=0.06)
        for s in ('right', 'left'):                               # the branchial groove down each side; the gill chamber's swell
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                x = int(fw * 0.35 + 2 * math.sin(j * 0.3))
                p.px((fx + x, fy + j), k(SHELL_DEEP, 0.8))
            for j in range(fh // 3, fh):
                for i in range(fw):
                    if (i + j) % 7 == 0:
                        p.px((fx + i, fy + j), mixc(SHELL, FLECK, 0.5))
        fx, fy, fw, fh = f['back']                                # the back: a darker line of the gut seen through it
        for j in range(fh):
            p.px((fx + fw // 2, fy + j), (150, 60, 40))
            p.px((fx + fw // 2 - 1, fy + j), (176, 76, 50))
        for j in range(0, fh, 5):                                 # faint segment lines under the shell
            for i in range(2, fw - 2):
                if i % 2:
                    p.px((fx + i, fy + j), k(SHELL_DEEP, 0.95))
    if style == 'sternum':                                        # the pale underside, plated in segments
        shell(p, f, segs=5, belly=True, gloss=False)
    if style == 'ridge':
        shell(p, f, gloss=False)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i in range(0, fw, 2):
                p.clear((fx + i, fy))
    if style == 'headshell':                                      # the head's shell: gloss, the cervical groove
        shell(p, f, fleck=0.05)
        for s in ('right', 'left', 'top'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                p.px((fx + fw // 2, fy + j), k(SHELL_DEEP, 0.85))
        fx, fy, fw, fh = f['front']                               # the face: the mouth's dark under the rostrum, spines at the eye
        for i in range(fw // 2 - 3, fw // 2 + 3):
            for j in range(fh - 3, fh):
                p.px((fx + i, fy + j), (90, 26, 22))
        for i in (2, fw - 3):
            p.px((fx + i, fy + 2), SHELL_GLOSS)
    if style == 'mouthplate':
        shell(p, f, gloss=False, belly=True)
        fx, fy, fw, fh = f['front']
        for i in range(0, fw, 2):
            p.px((fx + i, fy + fh - 1), (90, 26, 22))
    if style == 'rostrum':                                        # the rostrum: a long blade, saw-toothed above and below, to a point
        shell(p, f, along='x', fleck=0.02)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]                                 # 27x4 along its length; its tip at the front (-z)
            for i in range(fw):
                tip = (i / fw) if s == 'left' else 1 - i / fw    # 0 at the root .. 1 at the tip
                for j in range(fh):
                    top_cut = (i % 3 == 0) and j == 0 and tip < 0.85
                    bottom_cut = (i % 4 == 1) and j == fh - 1 and tip > 0.35
                    taper = j >= fh * (1 - (1 - tip) * 1.1) + 1 and tip > 0.6
                    if top_cut or bottom_cut or (tip > 0.75 and j > 1 + (1 - tip) * 6):
                        p.clear((fx + i, fy + j))
                    elif j == 0 and not top_cut:
                        p.px((fx + i, fy + j), SHELL_GLOSS)
        for s in ('top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                if j % 3 == 0:
                    for i in range(fw):
                        p.px((fx + i, fy + j), SHELL_PALE)
    if style == 'crestspines':                                    # the little spines along the carapace's crest
        shell(p, f, along='x', gloss=False)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                if i % 3:
                    p.clear((fx + i, fy))
    if style == 'stalk':
        shell(p, f, along='x', gloss=False, fleck=0)
    if style == 'eye':                                            # a glossy black compound eye, a glint
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    hexed = (i + (j // 2) % 2) % 2 == 0 and j % 2 == 0
                    col = (24, 26, 34) if not hexed else (44, 48, 60)
                    p.px((fx + i, fy + j), k(col, LIGHTS[s]))
            p.px((fx + 1, fy + 1), (230, 240, 255), glow=True)
            p.px((fx + 2, fy + 1), (150, 170, 200), glow=True)
            p.px((fx + 1, fy + 2), (120, 140, 170))
    if style in ('antenna_base', 'antenna', 'antenna_tip', 'antennule'):  # ringed whips, red and pale in turn
        for s, (fx, fy, fw, fh) in f.items():
            long_x = s in ('top', 'bottom')
            n = fh if not long_x else fw
            for j in range(fh):
                for i in range(fw):
                    a = j if not long_x else i
                    if s in ('front', 'back'):
                        a = 0
                    ring = (a // 2) % 2
                    col = mixc(SHELL, FLECK, 0.45) if ring else SHELL_PALE
                    if style == 'antenna_tip':
                        col = mixc(col, SHELL_PALE, 0.4)
                    p.px((fx + i, fy + j), noisy(p, k(col, LIGHTS[s]), 4))
    if style == 'antscale':                                       # the antennal scale: a clear blade, a spine at its edge
        for s, r in f.items():
            fringe(p, r, SHELL, alpha=200)
        fx, fy, fw, fh = f['top']
        for j in range(fh):
            p.px((fx, fy + j), SHELL_DEEP)
        point(p, f['top'], keep=1, down=False, sharp=0.6)
        point(p, f['bottom'], keep=1, down=False, sharp=0.6)
    if style in ('legthin', 'legtip', 'legseg', 'legseg2', 'armseg', 'armseg2', 'dactyl'):  # jointed limbs: shell, banded at the ends
        shell(p, f, fleck=0.03)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), k(SHELL_DEEP, 0.8))
                p.px((fx + i, fy + fh - 1), mixc(SHELL, FLECK, 0.6))
            if style in ('legseg', 'legseg2', 'armseg', 'armseg2'):   # spines down its back edge
                for j in range(2, fh - 1, 3):
                    p.px((fx, fy + j), SHELL_GLOSS)
        if style in ('dactyl', 'legtip'):
            for s in SIDES:
                point(p, f[s], keep=0.5, down=True, sharp=0.5)
    if style == 'joint':                                          # the soft joint between: pale, ringed
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), noisy(p, k(BELLY if j % 2 else SHELL_PALE, LIGHTS[s] * 0.95), 3))
    if style == 'seg':                                            # an abdominal segment: the tergite over the back, the pleuron's
        shell(p, f, along='y', fleck=0.06)                         # rounded flap down its side, the pale underside
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]                                 # d x h: along it, top to bottom
            for i in range(fw):
                for j in range(fh):
                    t = j / max(1, fh - 1)
                    flap = abs((i + 0.5) / fw - 0.5) * 2
                    if t > 0.55 and flap > 0.7 + (1 - t) * 0.5:
                        p.px((fx + i, fy + j), k(SHELL_DEEP, 0.75))
                    if t > 0.85:
                        p.px((fx + i, fy + j), mixc(BELLY, SHELL, 0.4))
        for s in ('bottom',):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), noisy(p, k(BELLY, 0.85 if j % 3 else 0.7), 3))
        fx, fy, fw, fh = f['top']                                 # the back: the gut's dark line, gloss
        for j in range(fh):
            p.px((fx + fw // 2, fy + j), (160, 64, 42))
    if style == 'tergite':                                        # the overlapping front edge of each segment's shell
        shell(p, f, along='y', fleck=0.02)
        for s in SIDES + ('top',):
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), SHELL_GLOSS if s == 'top' else mixc(SHELL_PALE, SHELL_GLOSS, 0.5))
                p.px((fx + i, fy + fh - 1), k(SHELL_DEEP, 0.7))
        fx, fy, fw, fh = f['top']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), k(SHELL_DEEP, 0.6))
    if style in ('swimmeret',):
        for s, r in f.items():
            fringe(p, r, SHELL, alpha=185)
        for s in SIDES:
            ragged(p, f[s], teeth=2, depth=2)
    if style in ('telson',):                                      # the telson: a tapering spine, its tip forked
        shell(p, f, along='y', fleck=0.03)
        for s in ('top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    t = 1 - j / max(1, fh - 1) if s == 'top' else j / max(1, fh - 1)
                    if abs(i + 0.5 - fw / 2) > (fw / 2) * (1 - 0.65 * t) + 0.3:
                        p.clear((fx + i, fy + j))
            for j in range(0, fh, 3):
                p.px((fx, fy + j), SHELL_GLOSS)
                p.px((fx + fw - 1, fy + j), SHELL_GLOSS)
    if style in ('uropod', 'uropod_in'):                          # a fan blade: clear amber, a darker vein, a red-fringed edge
        for s, r in f.items():
            fringe(p, r, SHELL, alpha=205)
        for s in ('top', 'bottom'):
            fx, fy, fw, fh = f[s]                                 # w x d: across it, along it (its end at the box's +z)
            for j in range(fh):
                t = j / max(1, fh - 1)
                if s == 'top':
                    t = 1 - t
                for i in range(fw):
                    edge = abs(i + 0.5 - fw / 2) / (fw / 2)
                    if t > 0.6 and edge > 1 - (t - 0.6) * 1.6 + 0.35:
                        p.clear((fx + i, fy + j))
                    elif t > 0.82:
                        p.pxa((fx + i, fy + j), mixc(FLECK, SHELL, 0.3), 220)
                for i in (fw // 2 - 1, fw // 2):
                    p.pxa((fx + i, fy + j), k(SHELL_DEEP, 0.9), 230)
    # ── the soldier's kit ──
    if style in ('spauldron', 'spauldron2'):                       # teal lacquered shoulder plates, gold-edged, studded
        lacquer(p, f, TEAL, TEAL_DEEP)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            rim(p, f[s], GOLD)
            for i in range(1, fw - 1, 3):
                p.px((fx + i, fy + 1), GOLD)
        fx, fy, fw, fh = f['top']
        for j in range(fh):
            for i in range(fw):
                if (i + j) % 5 == 0:
                    p.px((fx + i, fy + j), TEAL_LIGHT)
    if style == 'bracer':
        lacquer(p, f, TEAL, TEAL_DEEP)
        for s in SIDES:
            rim(p, f[s], GOLD, top=True)
    if style in ('helmbrim', 'helmbowl', 'helmknob'):              # the helmet: teal lacquer, gold rim and ribs, a gold knob
        if style == 'helmknob':
            gold_box(p, f)
        else:
            lacquer(p, f, TEAL, TEAL_DEEP)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                if style == 'helmbrim':
                    for i in range(fw):
                        for j in range(fh):
                            p.px((fx + i, fy + j), GOLD if j == 0 else GOLD_DEEP)
                else:
                    for i in range(0, fw, 3):
                        for j in range(fh):
                            p.px((fx + i, fy + j), GOLD_DEEP)
                    rim(p, f[s], GOLD)
            if style == 'helmbowl':
                fx, fy, fw, fh = f['front']                       # a gold wave on its brow
                for i in range(fw):
                    p.px((fx + i, fy + fh - 3 - int(1.2 * (1 + math.sin(i * 1.1)))), GOLD)
                fx, fy, fw, fh = f['top']
                for i in range(fw):
                    p.px((fx + i, fy + fh // 2), GOLD_DEEP)
                    p.px((fx + fw // 2, fy + i if i < fh else fy), GOLD_DEEP)
    if style in ('plume', 'plumetail', 'tassel'):                  # red horsehair, ragged
        for s, (fx, fy, fw, fh) in f.items():
            for i in range(fw):
                sh = p.rnd.uniform(0.7, 1.15)
                for j in range(fh):
                    p.px((fx + i, fy + j), noisy(p, k(RED, sh * (1.15 - 0.4 * j / max(1, fh)) * LIGHTS[s]), 5))
            if s in SIDES:
                ragged(p, (fx, fy, fw, fh), teeth=2, depth=3)
                if style == 'tassel':
                    for i in range(fw):
                        p.px((fx + i, fy), GOLD)
                if style == 'plume':
                    ragged(p, (fx, fy, fw, fh), teeth=2, depth=2, down=False)
    if style in ('sash', 'sashtail'):                             # sea-blue silk, a gold stitched edge
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    t = 1.1 + (0.1 if (i + j // 2) % 4 == 0 else 0) - 0.2 * (j / fh if style == 'sashtail' else 0)
                    p.px((fx + i, fy + j), noisy(p, k(SEA, t * LIGHTS[s]), 3))
            if s in ('front', 'back'):
                for j in range(fh):
                    p.px((fx, fy + j), GOLD_DEEP)
                    p.px((fx + fw - 1, fy + j), GOLD_DEEP)
                if style == 'sash':
                    for j in range(4, fh - 4, 8):               # little gold wave knots along it
                        for i in range(1, fw - 1):
                            p.px((fx + i, fy + j), GOLD)
                else:
                    ragged(p, (fx, fy, fw, fh), teeth=2, depth=2)
    if style == 'belt':                                           # a red leather belt, gold studs
        for s, (fx, fy, fw, fh) in f.items():
            p.face((fx, fy, fw, fh), RED_DEEP, LIGHTS[s], noise=3)
            if s in SIDES:
                for i in range(fw):
                    p.px((fx + i, fy), GOLD_DEEP)
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
                for i in range(2, fw - 1, 4):
                    p.px((fx + i, fy + fh // 2), GOLD)
    if style == 'buckle':
        gold_box(p, f)
        fx, fy, fw, fh = f['front']
        circle(p, fx + fw / 2, fy + fh / 2, 0, 1.3, (96, 178, 160))
    if style in ('palm', 'finger'):                               # little pincers
        shell(p, f, fleck=0.03)
        if style == 'finger':
            for s in SIDES:
                point(p, f[s], keep=0.5, down=True, sharp=0.5)
    if style == 'shaft':                                          # dark red lacquer, gold bands
        for s, (fx, fy, fw, fh) in f.items():
            p.face((fx, fy, fw, fh), SHAFT, LIGHTS[s], noise=3)
            if s in SIDES:
                for j in range(0, fh, 12):
                    for i in range(fw):
                        p.px((fx + i, fy + j), GOLD)
                p.px((fx, fy + 2), (150, 60, 50))
    if style == 'collar':
        gold_box(p, f)
    if style == 'butt':
        steel_box(p, f, STEEL_DARK)
        for s in SIDES:
            point(p, f[s], keep=0.5, down=True, sharp=0.5)
    if style == 'blade':                                          # a fine leaf blade, a ridge down it
        steel_box(p, f)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                p.px((fx + fw // 2, fy + j), (244, 248, 255))
                p.px((fx + fw // 2 - 1, fy + j), STEEL_DARK)
            fx, fy, fw, fh = f[s]
            for j in range(fh):                                   # leaf-shaped: widest low, to a point at the top
                t = j / max(1, fh - 1)
                half = (fw / 2) * (0.35 + 0.65 * math.sin(min(1, t * 1.15) * math.pi * 0.62))
                for i in range(fw):
                    if abs(i + 0.5 - fw / 2) > half + 0.2:
                        p.clear((fx + i, fy + j))
        for s in ('right', 'left'):
            point(p, f[s], keep=0.5, down=False, sharp=0.3)
    if style == 'pennant':                                        # a little sea-blue pennant, gold-bordered, a white wave on it
        for s, (fx, fy, fw, fh) in f.items():
            if s not in ('front', 'back'):
                p.face((fx, fy, fw, fh), SEA_DEEP, 1.0, noise=2)
                continue
            for j in range(fh):
                for i in range(fw):
                    ii = i if s == 'front' else fw - 1 - i
                    t = ii / max(1, fw - 1)                       # 0 at the staff .. 1 at the fly
                    if j > fh * (1 - 0.45 * t) or j < fh * 0.3 * t * 0:
                        p.clear((fx + i, fy + j))
                        continue
                    col = SEA
                    if j == 0 or ii == 0 or j >= fh * (1 - 0.45 * t) - 1:
                        col = GOLD
                    p.px((fx + i, fy + j), noisy(p, k(col, 1.0 - 0.15 * t), 3))
            for i in range(2, fw - 2):                            # the wave
                ii = i if s == 'front' else fw - 1 - i
                y = int(fh * 0.38 + 1.2 * math.sin(ii * 1.2))
                p.px((fx + i, fy + y), (236, 244, 255))


if __name__ == '__main__':
    p = ShellPainter(19)
    slots = paint(SHRIMP, p, paint_shrimp)
    p.img.save(OUT + 'shrimp_soldier.png')
    p.glow.save(OUT + 'shrimp_soldier_glow.png')
    emit(SHRIMP, slots, JAVA)
    print('shrimp soldier', len(SHRIMP), 'parts,', len(slots), 'boxes')
    from lion_camel import extent
    print('   drawn at', SIZE, {kk: round(v, 2) for kk, v in extent(SHRIMP, SIZE).items()})
    low = [q for q in SHRIMP if 'antenn' not in q[0] and q[0] not in ('spear', 'pennant')]
    print('   without antennae and spear', {kk: round(v, 2) for kk, v in extent(low, SIZE).items()})
