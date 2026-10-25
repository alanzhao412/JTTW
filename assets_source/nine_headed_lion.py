"""The Nine-Headed Lion, the Grand Saint of Nine Spirits' true form (九灵元圣, the Taiyi Heavenly
Worthy's mount). As great_white_bull.py: its model and skin come from one list of parts (beastgen): the
model is written into NineHeadedLionRenderer.layer() between its GENERATED markers, the skin
(textures/entity/nine_headed_lion.png) painted to the same box-UV layout at twice the model's
resolution, and its eyes alone (nine_headed_lion_eyes.png), drawn full-bright over the skin so they burn.

A celestial lion grown into a demon, grander than his kin the Azure Lion: a vast golden-tawny body, its
weight in a deep chest and heavy shoulders buried in a great shaggy mane of burnished gold, a long
back to strong haunches, thick legs on great clawed paws, a long tail ending in a tuft of curls. Out of
the mane rise nine thick maned necks, fanned in an arc from the shoulders so every head shows from
the front and the side: the middle one largest and furthest forward, four to each side, set back and
forward in turn. Each head is a guardian lion's: a broad flat face under a heavy scowling brow, deep-set
eyes smouldering gold-ember, a short wrinkled snout, fangs, jaws open on a roar; the stone lions'
spiral curls ringing the face and bearding the jaw, locks like flames swept back off it. The Heavenly
Worthy's harness: a jewelled gold breast-collar hung with bells, a great bronze bell at the breast, a
saddle-cloth of celestial blue scrolled with gold clouds.

The eyes are the Great White Bull's glare: on each face's front, each eye two model pixels along its
bottom, two tall at its outer side, one at its inner, the brow's shadow cutting its inner corner, the
ember drawn full-bright and the skin about it shaded to 0.35.

Run from the repository root: python assets_source/nine_headed_lion.py"""
import math

import beastgen as bg
from beastgen import Painter, paint, emit
from fire_court import part, mirrored
from lion_camel import (pelt, mane, strands, rosette, flame_marks, glare, teeth, claws, blend, plain,
                        extent, THROAT, MOUTH)
from great_white_bull import bulge

bg.UNITS = 320
bg.TEX = bg.UNITS * bg.S
S = bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/NineHeadedLionRenderer.java'
# How much larger than its pixels it is drawn: the renderer's SCALE
SCALE = 1.25

HIDE = (200, 146, 58)          # golden tawny
HIDE_DARK = (80, 44, 14)
HIDE_PALE = (222, 180, 108)
MANE = (156, 84, 18)          # the mane, burnished gold, umber at its roots, bright at its ends
MANE_DEEP = (38, 14, 4)
MANE_TIP = (236, 176, 62)
GOLD = (226, 178, 62)
GOLD_HI = (252, 222, 128)
GOLD_DEEP = (150, 100, 30)
CLOTH = (40, 40, 104)          # the saddle-cloth: celestial blue
CLOTH_DEEP = (18, 16, 52)
JADE = (40, 150, 110)
RUBY = (160, 20, 34)
SAPPHIRE = (30, 80, 170)
EMBER = (124, 52, 6)           # the eyes smoulder gold-ember: dark, a banked fire
EMBER_CORE = (164, 90, 12)


# ── geometry ─────────────────────────────────────────────────────────────────

def rot(rx, ry, rz):
    """The rotation a part's pose makes: Rz * Ry * Rx."""
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    x = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    y = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    z = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return mm(mm(z, y), x)


def mm(a, b):
    return [[sum(a[i][t] * b[t][j] for t in range(3)) for j in range(3)] for i in range(3)]


def tr(a):
    return [[a[j][i] for j in range(3)] for i in range(3)]


def euler(m):
    """The (rx, ry, rz) whose Rz * Ry * Rx is m."""
    ry = math.asin(max(-1, min(1, -m[2][0])))
    return math.atan2(m[2][1], m[2][2]), ry, math.atan2(m[1][0], m[0][0])


def flipx(b):
    x, y, z, w, h, d, s = b
    return (-x - w, y, z, w, h, d, s)


def curl(x, y, z, s=4.0, d=2.5):
    """A spiral curl (a box, seen end on), centred at x, y, its face at z."""
    return (x - s / 2, y - s / 2, z, s, s, d, 'ncurl')


def lock(name, parent, at, r, k=1.0, fin=False):
    """A lock of the mane like a tongue of flame, pointing up (-y) from its pivot, tapering in two steps
    to a point; flat across x, or (a `fin`) across z."""
    w, d = (2.2 * k, 5.5 * k) if fin else (5.5 * k, 2.2 * k)
    l1, l2, l3 = 6 * k, 4.5 * k, 3.5 * k
    return part(name, parent, at, r, [
        (-w / 2, -l1, -d / 2, w, l1, d, 'nlock'),
        (-w * 0.32, -l1 - l2, -d / 2 + 0.6 * k, w * 0.64, l2, d * 0.7, 'nlockmid'),
        (-w * 0.15, -l1 - l2 - l3, -d / 2 + 1.3 * k, w * 0.3, l3, d * 0.45, 'nlocktip'),
    ])


def head(p, parent, at, r, k, full):
    """A lion's head, `k` times the size of the model head, its pivot the back of the skull: a broad flat
    face under a heavy scowling brow, a short wrinkled snout, fangs, the jaws open on a dark maw; curls
    ringing the face and bearding the jaw; flame-locks swept back off it. The middle head (`full`) has
    the whole ring of curls, a deeper ruff and more locks; the others a lighter ruff, so they part."""
    def b(x, y, z, w, h, d, s, whole=False):
        # the face keeps whole model pixels, so the eyes' cells fall true
        if whole:
            return (-round(w * k) / 2, y * k, z * k, round(w * k), round(h * k), round(d * k), s)
        return (x * k, y * k, z * k, w * k, h * k, d * k, s)

    ring = [(3.0, -9.2, 4.2), (7.0, -7.0, 3.6), (8.8, -3.0, 4.2), (8.8, 1.6, 3.6), (6.6, 5.4, 4.2), (2.4, 7.4, 3.6)] if full \
        else [(7.0, -5.6, 3.4), (7.8, -0.4, 3.6), (5.8, 4.6, 3.2)]
    curls = []
    for side in (-1, 1):
        for x, y, sz in ring:
            curls.append(curl(x * side * k, y * k, -5.2 * k, sz * k, 2.5 * k))
    ruff = [b(-8.5, -8, -4, 17, 13, 5, 'nruff'), b(-6, -10.5, -4, 12, 18, 5, 'nruff')] if full \
        else [b(-5.5, -7, -3.2, 11, 11, 3, 'nruff')]
    beard = [curl(-3.6 * k, 3.6 * k, -5.8 * k, 3.4 * k, 2.2 * k), curl(0, 4 * k, -6.2 * k, 3.4 * k, 2.2 * k),
             curl(3.6 * k, 3.6 * k, -5.8 * k, 3.4 * k, 2.2 * k)] if full \
        else [curl(-2 * k, 3.6 * k, -5.8 * k, 3.2 * k, 2 * k), curl(2 * k, 3.6 * k, -5.8 * k, 3.2 * k, 2 * k)]
    parts = [
        part(p + '_head', parent, at, r, [
            b(-5.5, -5, -9, 11, 9, 9, 'nface', True),
            b(-5, -7.5, -8, 10, 3, 7, 'ncrown'),                   # the crown, flat and broad
            b(-7, -0.5, -8, 14, 5, 6, 'njowl'),                    # broadest under the eyes
            b(-2.5, -3.6, -13, 5, 3, 4.5, 'nsnout'),               # the bridge of the nose, wrinkled
            b(-4.6, -0.8, -13.6, 4.5, 4, 5, 'nmuzzle'), b(0.1, -0.8, -13.6, 4.5, 4, 5, 'nmuzzle'),   # the lip drawn back
            b(-0.8, -0.8, -13.2, 1.6, 3.6, 4.5, 'nsnout'),
            b(-2.3, -3, -14.2, 4.6, 2.3, 1.2, 'nnose'),
            b(-4.5, 2.6, -13.2, 1.4, 3.6, 1.4, 'nfang'), b(3.1, 2.6, -13.2, 1.4, 3.6, 1.4, 'nfang'),
            b(-4.1, 5.8, -13, 0.8, 2, 0.8, 'nfang'), b(3.3, 5.8, -13, 0.8, 2, 0.8, 'nfang'),
            b(-3, 2.9, -13.4, 6, 1.2, 0.8, 'nteeth'),
            b(-3.4, 0.8, -9.5, 6.8, 5.5, 4, 'nthroat'),
        ] + ruff + curls),
        part(p + '_brow_a', p + '_head', (-0.3 * k, -4.4 * k, -9.4 * k), (0, 0, 0.45), [b(-6.2, -1.2, -0.8, 6.2, 2.4, 2.2, 'nbrow')]),
        part(p + '_brow_b', p + '_head', (0.3 * k, -4.4 * k, -9.4 * k), (0, 0, -0.45), [flipx(b(-6.2, -1.2, -0.8, 6.2, 2.4, 2.2, 'nbrow'))]),
        part(p + '_jaw', p + '_head', (0, 2.8 * k, -8 * k), (0.35, 0, 0), [
            b(-3.1, 0, -5.8, 6.2, 2.2, 5.8, 'njaw'),
            b(-3, -2.6, -5.5, 1.2, 2.6, 1.2, 'nfang'), b(1.8, -2.6, -5.5, 1.2, 2.6, 1.2, 'nfang'),
            b(-1.6, -1.2, -5.6, 3.2, 1.2, 0.8, 'nteeth'),
        ] + beard),                                                 # his beard
    ]
    if full:                                                    # the middle head's crown of locks
        parts.append(lock(p + '_lock_top', p + '_head', (0, -9.5 * k, -1 * k), (-1.25, 0, 0), 0.95 * k, fin=True))
    sides = ((5.5, -9, 0.5, 0.95, True), (8.5, -3.5, 1.05, 1.0, False), (8.5, 3, 1.6, 0.95, False)) if full else ()
    for side, tag in ((-1, 'a'), (1, 'b')):
        for n, (x, y, rz, kk, fin) in enumerate(sides):
            parts.append(lock('%s_lock_%s%d' % (p, tag, n), p + '_head', (x * side * k, y * k, -0.5 * k),
                              (-1.05 + 0.08 * n, 0, rz * side), kk * k, fin))
    return parts


# The nine heads' pivots (the right ones; the left mirror them), fanned over the shoulders in two arcs
# about a point over his chest: the middle head and every other one on the outer arc, high and set
# back, the rest on the inner arc, lower and thrust forward, so that from the front each shows in the
# gap between its neighbours and from the side they stand in two ranks; the outermost low and wide
# beside his shoulders.
def fan(i):
    outer = i % 2 == 0
    radius, phi = (44 if i == 0 else 37 if outer else 27), math.radians(19 * i)
    z = (-38, -52, -33, -49, -31)[i]
    return (-radius * math.sin(phi), -7 - radius * math.cos(phi), z)


PLACES = [fan(i) for i in range(5)]
# The left heads stand a little off their right twins (the front rank further forward and lower, the
# back rank higher and further back), so that from the side the far heads show between the near ones.
LEFT_SHIFT = [None, (0, 3, -5), (0, -3, 4), (0, 3, -5), (0, -3, 4)]


def neck(name, i, shift=(0, 0, 0)):
    """A neck from the shoulders to its head in two arched segments, thick and maned, and the head on it."""
    # its root on the shoulders (in the body's pixels, grown), the right heads' on -x
    base = tuple(c * GROW for c in (-3.2 * i, -10 + 1.5 * i, -17 + 0.5 * i))
    end = tuple(e - o + d for e, o, d in zip(PLACES[i], BODY_AT, shift))
    mid = [base[c] + 0.5 * (end[c] - base[c]) for c in range(3)]
    mid[1] -= 5 - 0.6 * i                                       # arched up, the inner necks the more
    mid[2] += 2.5
    rx1, ry1, l1 = bg.segment(base, mid)
    rx2, ry2, l2 = bg.segment(mid, end)
    m1, m2 = rot(rx1, ry1, 0), rot(rx2, ry2, 0)
    k = 1.2 if i == 0 else 0.9
    t = 3.4 if i == 0 else 2.8
    # the head: facing ahead and out, the further out the more; the middle one glowering down, the rest
    # with their snouts lifted a touch
    hm = rot(0.12 if i == 0 else -0.08, math.radians(16 * i) * 0.9, 0)
    loc2 = euler(mm(tr(m1), m2))
    loch = euler(mm(tr(m2), hm))
    parts = [
        part(name + '_neck', 'body', base, (rx1, ry1, 0), [
            (-t, -t, -1, 2 * t, 2 * t, l1 + 2, 'nneck'),
            (-t - 0.8, -t - 2.2, -1, 2 * t + 1.6, 3, l1 + 2, 'nneckmane'),     # its mane along the crest
        ]),
        part(name + '_neck2', name + '_neck', (0, 0, l1), loc2, [
            (-t * 0.92, -t * 0.92, -1, 1.84 * t, 1.84 * t, l2 + 1, 'nneck'),
            (-t - 0.4, -t - 2, -1, 2 * t + 0.8, 3, l2 + 1, 'nneckmane'),
        ]),
    ]
    if i == 0:                                                  # a crest of mane on the middle neck
        parts.append(lock(name + '_crest_a', name + '_neck', (0, -t - 1, l1 * 0.4), (-1.15, 0, 0), 0.85, fin=True))
    return parts + head(name, name + '_neck2', (0, 0, l2), loch, k, i == 0)


def heads():
    out = neck('mid', 0)
    for i in range(1, 5):
        out += neck('right_h%d' % i, i)
        out += [q for q in mirrored(neck('right_h%d' % i, i, LEFT_SHIFT[i])) if q[0].startswith('left_')]
    return out


def grow(parts, k):
    """The parts `k` times larger, still standing on the ground."""
    out = []
    for name, parent, (px, py, pz), r, boxes, mirror in parts:
        at = (px * k, 24 + (py - 24) * k, pz * k) if parent == 'root' else (px * k, py * k, pz * k)
        out.append((name, parent, at, r, [tuple(v * k for v in b[:6]) + (b[6],) for b in boxes], mirror))
    return out


# The body is drawn larger than the heads' pixels, so his bulk carries the nine: GROW times, about
# the ground; BODY_AT is where its pivot stands, then.
GROW = 1.2
BODY_AT = (0, 24 - 24 * GROW, 0)

BODY = mirrored([
    # a deep chest and heavy shoulders, a long back drawn in at the waist, strong haunches
    part('body', 'root', (0, 0, 0), (0, 0, 0), [
        (-12, -9, -25, 24, 20, 20, 'nfur'),                     # chest and shoulders
        (-9, 0, -27, 18, 11, 8, 'nchest'),                      # the breast
        (-8.5, -7, -6, 17, 13, 17, 'nflank'),                 # the waist, the belly tucked up
        (-10, -8, 9, 20, 14, 13, 'nhaunch'),                    # the haunches
        (-13, -13, -26, 26, 11, 12, 'nmane'),                   # the great mane about the necks' roots
        (-12, -14, -16, 24, 10, 9, 'nmane'),                    # and over the shoulders
        (-11, -12, -9, 22, 8, 6, 'nmane'),                      # falling back along the withers
        # the Heavenly Worthy's harness: a jewelled breast-collar, bells, a saddle-cloth
        (-11.6, 4, -26.6, 23.2, 3, 12, 'ncollar'),
        (-2.6, 7, -28.2, 5.2, 6, 5.2, 'nbigbell'),              # the great bell at his breast
        (-0.8, 12.8, -26.4, 1.6, 2, 1.6, 'nclapper'),
        (-8.2, 7, -27.2, 2.6, 3, 2.6, 'nbell'), (5.6, 7, -27.2, 2.6, 3, 2.6, 'nbell'),
        (-11.4, 7, -21.8, 2.6, 3, 2.6, 'nbell'), (8.8, 7, -21.8, 2.6, 3, 2.6, 'nbell'),
        (-9.6, -9.2, -5, 19.2, 2.2, 14, 'ncloth'),              # the saddle-cloth over his back
        (-9.8, -8, -5.2, 1.4, 11, 14.4, 'nclothside'), (8.4, -8, -5.2, 1.4, 11, 14.4, 'nclothside'),
        (-3, -10.2, -2, 6, 1.2, 8, 'njewelplate'),               # a jewelled plate on it
    ]),
    part('rump', 'body', (0, -8, 10), (-0.35, 0, 0), [(-9.5, 0, 0, 19, 7, 13, 'nfur')]),   # sloping to the tail
    lock('spine_a', 'body', (0, -10, 6), (-1.4, 0, 0), 0.8, fin=True),
    lock('spine_b', 'body', (0, -9.5, 13), (-1.5, 0, 0), 0.65, fin=True),
    lock('right_mane_a', 'body', (-12, -12, -18), (-1.3, 0, -0.7), 1.25, fin=True),
    lock('right_mane_b', 'body', (-13, -4, -19), (-1.3, 0, -1.25), 1.3),
    lock('right_mane_c', 'body', (-12.5, 3, -20), (-1.2, 0, -1.9), 1.15),
    lock('right_mane_d', 'body', (-11.5, -10, -10), (-1.45, 0, -0.95), 1.05),
    lock('right_mane_e', 'body', (-11, -3, -10), (-1.5, 0, -1.4), 0.95),
    # the mane falling in locks down his breast
    lock('apron', 'body', (0, -3, -27.5), (2.95, 0, 0), 1.1),
    lock('right_apron', 'body', (-5, -3, -27.2), (2.95, 0, -0.18), 1.05),
    lock('right_apron_b', 'body', (-9.5, -3.5, -26), (2.95, 0, -0.4), 0.95),
    # thick forelegs: the shoulder's muscle, the forearm, a great clawed paw
    part('right_foreleg', 'body', (-8.5, -1, -15), (0, 0, 0), [
        (-5, -4, -6, 10, 13, 12, 'nfur'),
        (-4, 8, -4.3, 8, 13, 8.4, 'nleg'),
        curl(0, 12, 4.1, 4.8, 2.5),                             # a curl at the elbow
        (-5, 20.2, -9, 10, 4.8, 11, 'npaw'),
    ] + [(-4.6 + i * 2.55, 21.6, -11.2, 1.7, 3, 2.6, 'nclaw') for i in range(4)]),
    part('right_hindleg', 'body', (-6.5, -2, 15.5), (0, 0, 0), []),
    part('right_thigh', 'right_hindleg', (0, 0, 0), (-0.5, 0, 0), [(-5, -3, -6.5, 10, 15, 12, 'nfur')]),
    part('right_shin', 'right_thigh', (0, 11, -1), (1.4, 0, 0), [(-3.4, -1.5, -3, 6.8, 11, 6, 'nleg')]),
    part('right_hock', 'right_shin', (0, 9.5, 0), (-1.2, 0, 0), [(-3, -1, -2.7, 6, 9, 5.4, 'nleg')]),
    part('right_hindpaw', 'right_hock', (0, 7.6, 0), (0.3, 0, 0), [
        (-4.5, -0.7, -7.5, 9, 4.4, 9, 'npaw'),
    ] + [(-3.9 + i * 2.7, 1, -9.5, 1.6, 2.6, 2.4, 'nclaw') for i in range(3)]),
    # a long tail, ending in a tuft of curls and a flame of mane
    part('tail', 'body', (0, -5, 22), (-0.6, 0, 0), [(-1.4, -1.4, 0, 2.8, 2.8, 11, 'ntail')]),
    part('tail_mid', 'tail', (0, 0, 10.5), (0.9, 0, 0), [(-1.2, -1.2, 0, 2.4, 2.4, 10, 'ntail')]),
    part('tail_tip', 'tail_mid', (0, 0, 9.5), (0.7, 0, 0), [
        (-1.1, -1.1, 0, 2.2, 2.2, 5, 'ntail'),
        (-3, -3, 3.5, 6, 6, 5, 'nmane'),
        curl(0, 0, 8.4, 4.4, 1.5),
    ]),
    lock('tail_flame', 'tail_tip', (0, -1, 7), (-1.1, 0, 0), 1.0, fin=True),
])
LION = grow(BODY, GROW) + heads()


# ── painting ─────────────────────────────────────────────────────────────────

def gold(p, f, base=GOLD):
    plain(p, f, base)
    for k in ('front', 'back', 'right', 'left'):
        fx, fy, fw, fh = f[k]
        for i in range(fw):
            p.px((fx + i, fy), GOLD_HI)
            p.px((fx + i, fy + fh - 1), GOLD_DEEP)


def jewel(p, rect, cx, cy, c):
    """A cabochon a model pixel and a half across: lit at its crown, dark at its foot, set in dark gold."""
    for a in range(-2, 2):
        for b2 in range(-2, 2):
            x, y = cx + a, cy + b2
            if abs(a + 0.5) + abs(b2 + 0.5) <= 2.5:
                lit = 1.4 if (a, b2) == (-1, -1) else 0.65 if b2 == 1 else 1.0
                p.px((rect[0] + x, rect[1] + y), [v * lit for v in c])
    for a in (-3, 2):
        p.px((rect[0] + cx + a, rect[1] + cy), GOLD_DEEP)


def cloud_scroll(p, rect, cx, cy, r):
    rosette(p, rect, cx, cy, r, GOLD, GOLD_DEEP, GOLD_HI, turns=1.2)


def paint_lion(p, style, f, dims):
    if style == 'nhaunch':
        pelt(p, f, HIDE, HIDE_DARK, HIDE_PALE, along=True)
        bulge(p, f, (0.55, 0.45), (0.8, 0.75))                         # the round of the haunch
        for k in ('right', 'left'):
            flame_marks(p, f[k], HIDE_DARK, n=2)
        fx, fy, fw, fh = f['back']                                     # the cleft under the tail, shadowed
        for j in range(fh // 5, fh):
            for d in range(-2, 2):
                c = p.img.getpixel((fx + fw // 2 + d, fy + j))
                p.px((fx + fw // 2 + d, fy + j), [v * (0.55 if d in (-1, 0) else 0.75) for v in c[:3]])
        for j in range(fh):                                            # rounded off at its sides
            for i in list(range(3)) + list(range(fw - 3, fw)):
                c = p.img.getpixel((fx + i, fy + j))
                p.px((fx + i, fy + j), [v * 0.8 for v in c[:3]])
    elif style in ('nfur', 'nflank', 'ntail', 'nchest', 'nleg'):
        base = {'nflank': [v * 0.95 for v in HIDE], 'nchest': blend(HIDE, HIDE_PALE, 0.35)}.get(style, HIDE)
        pelt(p, f, base, HIDE_DARK, HIDE_PALE, along=style != 'nleg')
        if style in ('nfur', 'nflank', 'nleg'):
            for k in ('right', 'left'):
                flame_marks(p, f[k], HIDE_DARK, n=max(1, f[k][2] * f[k][3] // 300))
    elif style == 'nface':
        pelt(p, f, blend(HIDE, HIDE_PALE, 0.3), HIDE_DARK)
        fx, fy, fw, fh = f['front']
        # a dark mask about the eyes and down the sides of the face; the scowl's furrows between the brows
        for j in range(fh):
            for i in range(fw):
                edge = abs(i + 0.5 - fw / 2) / (fw / 2)
                if j < 6 * S and edge > 0.25 or j < 2 * S:
                    c = p.img.getpixel((fx + i, fy + j))
                    p.px((fx + i, fy + j), [v * (0.42 if j < 2 * S else 0.6) for v in c[:3]])
        for j in range(0, 4 * S):
            for d in (-S, S - 1):
                p.px((fx + fw // 2 + d, fy + j), [v * 0.4 for v in HIDE_DARK])
        w = fw // S
        glare(p, f, ((1, 2), (w - 2, w - 3)), 1, EMBER, EMBER_CORE)
    elif style == 'ncrown':
        pelt(p, f, [v * 0.88 for v in HIDE], HIDE_DARK)
        for k in ('top', 'front'):
            fx, fy, fw, fh = f[k]
            for j in range(fh):
                for d in (-S - 1, S):
                    p.px((fx + fw // 2 + d, fy + j), [v * 0.45 for v in HIDE_DARK])
    elif style == 'njowl':
        pelt(p, f, blend(HIDE, HIDE_PALE, 0.15), HIDE_DARK)
        fx, fy, fw, fh = f['front']
        for j in range(fh):
            for i in range(fw):
                if abs(i + 0.5 - fw / 2) > fw * 0.3:
                    c = p.img.getpixel((fx + i, fy + j))
                    p.px((fx + i, fy + j), [v * 0.7 for v in c[:3]])
    elif style == 'nbrow':
        for k, r in f.items():
            p.face(r, (60, 34, 12), {'top': 1.9, 'bottom': 0.4, 'front': 1.05}.get(k, 1.0), noise=3)
        fx, fy, fw, fh = f['top']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), [v * 0.8 for v in HIDE_PALE])
    elif style == 'nsnout':
        pelt(p, f, [v * 0.86 for v in HIDE], HIDE_DARK)
        for k in ('top', 'right', 'left'):                             # the snarl's wrinkles, deep
            fx, fy, fw, fh = f[k]
            if k == 'top':
                for j in range(S, fh, S * 2):
                    for i in range(fw):
                        p.px((fx + i, fy + j), [v * 0.35 for v in HIDE_DARK])
                        if j + 1 < fh:
                            p.px((fx + i, fy + j + 1), [v * 1.12 for v in HIDE])
            else:
                for i in range(S, fw, S * 2):
                    for j in range(fh):
                        p.px((fx + i, fy + j), [v * 0.35 for v in HIDE_DARK])
    elif style == 'nmuzzle':
        pelt(p, f, blend(HIDE, HIDE_PALE, 0.15), HIDE_DARK)
        fx, fy, fw, fh = f['front']
        for row in (fh // 4, fh // 2 - 1):                             # whisker spots
            for i in range(S, fw - S, S + 1):
                p.px((fx + i, fy + row), [v * 0.4 for v in HIDE_DARK])
        for k in ('front', 'right', 'left'):                           # the lip drawn back, black, over red gums
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                gum = k == 'front' and S <= i < fw - S or k != 'front' and i < fw // 2
                for j in range(fh - S, fh):
                    p.px((fx + i, fy + j), (98, 24, 30) if gum else (16, 10, 12))
                for j in range(fh - 2 * S, fh - S):
                    p.px((fx + i, fy + j), (16, 10, 12))
        fx, fy, fw, fh = f['bottom']
        for j in range(fh):
            for i in range(fw):
                c = THROAT if j % 3 == 0 else MOUTH
                p.px((fx + i, fy + j), [v + p.rnd.randint(-4, 4) for v in c])
    elif style == 'nnose':
        plain(p, f, (46, 26, 18))
        p.face(f['top'], (96, 62, 40), 1.0, noise=3)
        fx, fy, fw, fh = f['front']
        for j in range(fh // 2, fh):
            for i in range(fw):
                if abs(i + 0.5 - fw / 2) > fw / 2 - (j - fh // 2 + 1):
                    p.px((fx + i, fy + j), blend(HIDE, HIDE_PALE, 0.3))
        for side in (-1, 1):
            cx = fx + fw // 2 + side * (fw // 4)
            for j in range(fh // 2, fh):
                for i in range(-S, S):
                    p.px((cx + i, fy + j), (6, 2, 2))
    elif style == 'nfang':
        teeth(p, f)
    elif style == 'nteeth':
        teeth(p, f)
        fx, fy, fw, fh = f['front']
        for i in range(0, fw, S + 1):
            for j in range(fh):
                p.px((fx + i, fy + j), (60, 20, 24))
    elif style == 'nthroat':
        plain(p, f, THROAT)
    elif style == 'njaw':
        pelt(p, f, blend(HIDE, HIDE_PALE, 0.5), HIDE)
        fx, fy, fw, fh = f['top']
        for j in range(fh):
            for i in range(fw):
                c = THROAT if j > fh * 0.7 else MOUTH
                p.px((fx + i, fy + j), [v + p.rnd.randint(-4, 4) for v in c])
        for k in ('front', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for j in range(S):
                    p.px((fx + i, fy + j), (16, 10, 12))
    elif style in ('nmane', 'nneckmane'):
        mane(p, f, MANE, MANE_DEEP, MANE_TIP)
        for k in ('right', 'left', 'front', 'back', 'top'):            # curls among the locks
            fx, fy, fw, fh = f[k]
            rad = 2.4 * S
            if fw < rad * 2 + 2 or fh < rad * 2 + 2:
                continue
            for cy in range(int(rad), fh - int(rad) + 1, int(rad * 2.1)):
                for cx in range(int(rad) + (int(rad) if (cy // int(rad * 2.1)) % 2 else 0), fw - int(rad) + 1, int(rad * 2.3)):
                    if p.rnd.random() < 0.35:
                        rosette(p, f[k], cx, cy, rad, MANE, MANE_DEEP, MANE_TIP)
    elif style == 'nneck':
        pelt(p, f, [v * 0.92 for v in HIDE], HIDE_DARK, HIDE_PALE)
        for k in ('right', 'left', 'bottom'):                          # shaggy down its sides, in strands
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                if p.rnd.random() < 0.35:
                    for j in range(fh):
                        c = p.img.getpixel((fx + i, fy + j))
                        p.px((fx + i, fy + j), blend(c[:3], MANE_DEEP, 0.35))
            flame_marks(p, f[k], HIDE_DARK, n=2)
    elif style == 'nruff':
        mane(p, f, MANE, MANE_DEEP, MANE_TIP)
        fx, fy, fw, fh = f['front']                                    # curl on curl behind the face
        rad = 2.2 * S
        for row, cy in enumerate(range(int(rad), fh, int(rad * 1.9))):
            for cx in range(int(rad) + (int(rad) if row % 2 else 0), fw, int(rad * 2)):
                rosette(p, f['front'], cx, cy, rad, MANE, MANE_DEEP, MANE_TIP)
    elif style == 'ncurl':
        strands(p, f, MANE, MANE_DEEP, MANE_TIP, 0.35, 0.8)
        fx, fy, fw, fh = f['front']
        p.face(f['front'], blend(MANE_DEEP, MANE, 0.3), 1.0, noise=2)
        rosette(p, f['front'], fw / 2, fh / 2, fw / 2, MANE, MANE_DEEP, MANE_TIP, turns=1.5)
    elif style == 'nlock':
        strands(p, f, MANE, MANE_DEEP, MANE_TIP, 0.4, 1.0)
    elif style == 'nlockmid':
        strands(p, f, MANE, MANE_DEEP, MANE_TIP, 0.2, 0.5)
    elif style == 'nlocktip':
        strands(p, f, MANE, MANE_DEEP, MANE_TIP, 0.0, 0.25)
    elif style == 'npaw':
        pelt(p, f, [v * 0.9 for v in HIDE], HIDE_DARK)
        p.face(f['bottom'], (30, 22, 20), 1.0, noise=4)
        for k in ('front', 'top'):                                    # the toes, parted
            fx, fy, fw, fh = f[k]
            for i in range(fw // 4, fw, fw // 4):
                for j in range(fh if k == 'front' else fh // 3):
                    jj = j if k == 'front' else fh - 1 - j
                    p.px((fx + i, fy + jj), [v * 0.45 for v in HIDE_DARK])
    elif style == 'nclaw':
        claws(p, f)
    elif style == 'ncollar':
        gold(p, f)
        for k in ('front', 'right', 'left'):                          # its edges, and jewels set along it
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                p.px((fx + i, fy + 1), GOLD_DEEP)
                p.px((fx + i, fy + fh - 2), GOLD_DEEP)
            for n, i in enumerate(range(S * 3, fw - S * 2, S * 5)):
                jewel(p, f[k], i, fh // 2, (RUBY, SAPPHIRE, JADE)[n % 3])
    elif style in ('nbell', 'nbigbell'):
        for k, r in f.items():
            p.gradient(r, GOLD_HI, GOLD_DEEP if style == 'nbell' else (120, 70, 22), noise=3)
        for k in ('front', 'right', 'left', 'back'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                p.px((fx + i, fy + fh // 3), GOLD_DEEP)
                p.px((fx + i, fy + fh - 1), (40, 26, 10))
                if style == 'nbigbell':
                    p.px((fx + i, fy + fh - 3), GOLD_DEEP)
                    p.px((fx + i, fy + fh - 4), GOLD_HI)
        if style == 'nbigbell':                                       # a trigram on its face, a jewel over it
            fx, fy, fw, fh = f['front']
            for row in range(3):
                y = fy + fh // 3 + 3 + row * 3
                for i in range(2, fw - 2):
                    if row != 1 or abs(i - fw / 2) > 1.5:
                        p.px((fx + i, y), (90, 54, 16))
            jewel(p, f['front'], fw // 2, fh // 3 - 2, RUBY)
        else:
            fx, fy, fw, fh = f['front']
            for j in range(fh * 2 // 3, fh - 1):
                p.px((fx + fw // 2, fy + j), (40, 26, 10))
        p.face(f['bottom'], (30, 20, 10), 1.0, noise=2)
    elif style == 'nclapper':
        plain(p, f, (70, 44, 18))
    elif style in ('ncloth', 'nclothside'):
        for k, r in f.items():
            p.face(r, CLOTH, {'top': 1.1, 'bottom': 0.6}.get(k, 0.92), noise=4, fall=0.15)
        for k, r in f.items():                                        # gold edging, and cloud scrolls
            fx, fy, fw, fh = r
            if fw < 4 or fh < 4:
                continue
            for i in range(fw):
                for d in range(S):
                    p.px((fx + i, fy + d), GOLD)
                    p.px((fx + i, fy + fh - 1 - d), GOLD_DEEP if d == 0 else GOLD)
            for j in range(fh):
                for d in range(S):
                    p.px((fx + d, fy + j), GOLD)
                    p.px((fx + fw - 1 - d, fy + j), GOLD)
            if fw > 14 and fh > 14:
                for cx in range(10, fw - 6, 14):
                    for cy in range(10, fh - 6, 14):
                        cloud_scroll(p, r, cx, cy, 4.5)
        if style == 'nclothside':                                     # a fringe of gold tassels along its foot
            for k in ('right', 'left', 'front', 'back'):
                fx, fy, fw, fh = f[k]
                for i in range(0, fw, 3):
                    for j in range(fh - 4, fh):
                        p.px((fx + i, fy + j), RUBY if j < fh - 2 else GOLD_HI)
    elif style == 'njewelplate':
        gold(p, f)
        fx, fy, fw, fh = f['top']
        jewel(p, f['top'], fw // 2, fh // 2, SAPPHIRE)
        jewel(p, f['top'], fw // 2, fh // 5 + 1, RUBY)
        jewel(p, f['top'], fw // 2, fh - fh // 5 - 1, JADE)
    else:
        raise ValueError(style)


if __name__ == '__main__':
    p = Painter(29)
    slots = paint(LION, p, paint_lion)
    p.img.save('src/main/resources/assets/jttw_engine/textures/entity/nine_headed_lion.png')
    p.glow.save('src/main/resources/assets/jttw_engine/textures/entity/nine_headed_lion_eyes.png')
    emit(LION, slots, JAVA)
    print(len(LION), 'parts,', len(slots), 'boxes; drawn at', SCALE,
          {k: round(v, 2) for k, v in extent(LION, SCALE).items()})
