"""The Hundred-Eyed Demon Lord (百眼魔君), the Many-Eyed Daoist of the Yellow Flower Temple, as
silk_court.py: his model and skins come from one list of parts, written into SpiderCaveRenderers.

A centipede spirit in a Daoist master's shape, towering near four blocks. A tall, gaunt figure in a
voluminous Daoist robe of temple yellow, its skirt falling to the ground in long gores that hang in
deep folds and flare toward the hem, open up the front: damask lozenges woven through the silk,
auspicious clouds worked in ochre and the eight trigrams round the hem over a black hem piped gold,
the opening's edges bordered in black and gold (gold as trim, not banding), the taiji on his back; a black cross collar
edged in gold and a stiff high collar standing behind his head; a crimson sash knotted at his
waist, its ends and a jade pendant hanging to his knees; great sleeves lined in black. Over his
shoulders three tiers of a centipede's armoured segments, umber-black and gold-rimmed.

And the eyes: dozens of them set through the silk, on his chest, his sleeves, his skirt and his
back, each a heavy-lidded golden eye half-shut and dull; when he casts his golden light they all
blaze open. His face long and sallow, the cheeks sunken under high cheekbones, narrow slanting eyes
under a heavy brow, a third eye shut on his forehead; a long thin moustache, a beard falling to his
sash in three locks; his hair in a topknot under a tall gold guan, a jade pin through it, ribbons
behind, and two long centipede feelers sweeping back from his crown; poison claws curled at the
corners of his jaw. Small centipede legs fringe his flanks through the robe's seams, run along the
backs of his arms and scuttle round his hem; a ridge of armoured plates runs down his back through a
slit in the robe; and from under the back of his robe a long segmented centipede tail trails behind
him on many legs to two hooked tail-spines. In his right hand his Daoist sword (剑): a ridged blade
inlaid with the seven stars of the Dipper, a gold guard of cloud-wings, a bound grip, a long red
tassel. Thrown off, the robe shows what it hid: a torso of glossy black-umber segments, each ringed
with golden eyes, arms the same.

The skin is painted at four texels to the model pixel (his face and eyes need the room).

Run from the repository root: python assets_source/hundred_eyed.py"""
import math

import beastgen as bg
from silk_court import (part, mirrored, paint, emit, SilkPainter, SIDES, LIGHTS, k, mixc, noisy, each, chitin, cloth,
                        edges, vedges, gold, cut_point, extent, JAVA)
from sea_court import aim

bg.UNITS = 256
bg.S = 4
bg.TEX = bg.UNITS * bg.S
S = bg.S

YELLOW = (220, 170, 46)
YELLOW_LIGHT = (252, 216, 112)
YELLOW_DEEP = (150, 100, 22)
OCHRE = (178, 104, 22)
BLACK = (24, 18, 18)
BLACK_SHEEN = (62, 52, 50)
GOLD = (238, 194, 82)
GOLD_DEEP = (150, 100, 32)
CRIMSON = (138, 22, 30)
CRIMSON_DEEP = (82, 10, 18)
WHITE = (238, 230, 208)
JADE = (110, 196, 156)
JADE_DEEP = (44, 120, 92)
SKIN = (196, 176, 124)        # sallow
SKIN_LIGHT = (222, 204, 154)
SKIN_SHADE = (138, 116, 76)
SKIN_HOLLOW = (96, 76, 50)
HAIR = (18, 14, 18)
HAIR_SHEEN = (74, 66, 76)
HAIR_GREY = (120, 116, 118)
CHITIN = (44, 26, 18)         # his own hide: glossy black-umber
CHITIN_SHEEN = (170, 120, 70)
UMBER = (110, 62, 26)
BAND = (186, 132, 44)
EYE = (255, 200, 60)
EYE_DULL = (126, 86, 26)
EYE_HOT = (255, 248, 200)
LID = (150, 116, 70)
STEEL = (206, 212, 222)
STEEL_DEEP = (120, 128, 142)
RED = (186, 28, 30)
CELADON = (150, 196, 170)

ARM, FOREARM = (0.06, 0, 0.1), (-0.6, -0.25, 0)
LARM, LFOREARM = ((x, -y, -z) for x, y, z in (ARM, FOREARM))
TAIL = 9
RINGS = 6


def ring_dims(i):
    """The i-th of his torso's segments, from the shoulders down: (top, height, width, depth)."""
    return -14.4 + i * 2.4, 2.7, 10.4 - i * 0.55, 6.2 - i * 0.2


TIERS = 9


def tier_dims(t):
    """The t-th tier of his robe's bell, from the waist down: (top, width, depth, centre z): flaring
    faster toward the hem, and trailing back a little."""
    q = t / (TIERS - 1)
    return 0.6 + t * 2.25, 10.2 + 12.0 * q ** 1.5, 7.2 + 8.4 * q ** 1.5, 1.2 * q * q


GORE_UP, GORE_LOW = 8.2, 12.6


def gores():
    """The skirt's gores down his right side, from the front opening round to his back: for each, the angle
    round him (0 before him), its breadth at the top and at the hem, how far its upper and lower lengths lean
    out (so the bell curves out toward the hem), the lower length's style (the one beside the opening edged in
    gold), and whether an eye is set in it."""
    out = []
    n = 8
    for k_ in range(n):
        th = 0.4 + (math.pi - 0.62) * k_ / (n - 1)
        r0 = math.hypot(5.0 * math.sin(th), 3.55 * math.cos(th))
        r1 = math.hypot(11.0 * math.sin(th), 7.9 * math.cos(th))
        drop = r1 - r0                                         # how far out the hem lies beyond the waist
        x0 = math.atan2(drop * 0.32, GORE_UP)
        x1 = math.atan2(drop - GORE_UP * math.sin(x0), GORE_LOW) - x0
        w0 = 2 * math.pi * r0 / (2 * n) * 1.5 + 0.6
        w1 = 2 * math.pi * r1 / (2 * n) * 1.25 + 0.4
        low = 'gore_front' if k_ == 0 else 'gore_low' if k_ % 2 == 0 else 'gore_low_dark'
        out.append((th, w0, w1, x0, x1, low, k_ in (2, 5)))
    return out


def lord_parts():
    ps = []
    # ── his body from the waist up: lean, his own hide, glossy segments ringed with eyes; a long neck ──
    body = [(-1.5, -19.2, -1.6, 3.0, 5.0, 2.8, 'neck')]
    for i in range(RINGS):
        y, h, w, d = ring_dims(i)
        body.append((-w / 2, y, -d / 2, w, h, d, 'ring' if i % 2 == 0 else 'ring_red'))
        # eyes round the ring: across the front, down each flank, across the back
        for c in range(3):
            x = -w / 2 + w * (c + 0.5) / 3 - 1.4 + (0.5 if i % 2 else -0.4)
            if abs(x + 1.4) < 1.5 and i < 4:
                continue                                         # the sternum's plates run there
            body.append((x, y + 0.7, -d / 2 - 0.3, 2.8, 1.3, 0.35, 'hide_eye'))
        for side in (-1, 1):
            body.append((w / 2 - 0.05 if side > 0 else -w / 2 - 0.3, y + 0.7, -1.3 + (i % 2) * 0.8, 0.35, 1.3, 2.6, 'hide_eye_side'))
        for c in range(2):
            x = -w / 2 + w * (c + 0.5) / 2 - 1.3
            body.append((x, y + 0.7, d / 2 - 0.05, 2.6, 1.3, 0.35, 'hide_eye'))
    for i in range(5):                                           # the sternum: a column of gold-rimmed plates
        body.append((-1.2, -14.0 + i * 2.4, -3.75 + i * 0.22, 2.4, 2.1, 0.7, 'sternum'))
    for i in range(7):                                           # the ridge of plates down his back, through the robe's slit
        body.append((-2.0 + i * 0.12, -14.6 + i * 2.15, 3.3 - i * 0.12, 4.0 - i * 0.24, 2.3, 1.5, 'dorsal'))
    ps.append(part('body', 'root', (0, 3, 0), (0, 0, 0), body))

    # the robe over it (thrown off in his rage): narrow at the shoulders, a high collar, the cross collar, eyes through the silk
    robe = [
        (-5.6, -15.2, -3.6, 11.2, 8.8, 7.2, 'robe_chest'),
        (-4.8, -6.6, -3.4, 9.6, 7.0, 6.8, 'robe_waist'),
        (-6.0, -15.6, -3.8, 12.0, 1.4, 7.6, 'robe_yoke'),
        (-3.2, -16.4, -2.8, 6.4, 1.6, 5.6, 'inner_collar'),
    ]
    for r, xs, y in ((0, (-4.6, 1.0), -12.8), (1, (-4.9, -1.6, 1.7), -10.0), (2, (-4.0, 0.4), -7.2)):   # eyes across his breast
        for x in xs:
            robe.append((x, y, -3.85 - (0.2 if r == 2 else 0), 3.6, 1.6, 0.3, 'robe_eye'))
    for r in range(2):
        for side in (-1, 1):                                       # and down his flanks
            robe.append((5.55 if side > 0 else -5.85, -13.4 + r * 3.6, -1.9 + r * 1.2, 0.3, 1.6, 3.6, 'robe_eye_side'))
    for x in (-4.6, 1.0):                                          # and across his back, under the collar
        robe.append((x, -11.0, 3.55, 3.6, 1.6, 0.3, 'robe_eye'))
    ps.append(part('robe', 'body', (0, 0, 0), (0, 0, 0), robe))
    ps += [
        part('collar_over', 'robe', (2.8, -15.6, -3.85), (0, 0, 0.6), [(-1.1, 0, -0.4, 2.2, 12.6, 0.8, 'robe_collar')]),
        part('collar_under', 'robe', (-2.8, -15.6, -3.8), (0, 0, -0.6), [(-1.1, 0, -0.3, 2.2, 6.4, 0.6, 'robe_collar')]),
        # the stiff high collar standing behind his head, black, gold-rimmed, lined white
        part('high_collar', 'robe', (0, -15.4, 0.6), (0.3, 0, 0), [
            (-5.2, -7.4, 1.8, 10.4, 7.8, 0.9, 'high_collar'),
            (-5.2, -5.6, -2.0, 0.9, 6.0, 4.0, 'high_collar_side'),
            (4.3, -5.6, -2.0, 0.9, 6.0, 4.0, 'high_collar_side'),
        ]),
    ]
    # the sash: crimson, gold-edged, knotted before him, its ends and a jade pendant hanging (it ties the skirt: it stays)
    ps += [
        part('sash', 'body', (0, 0, 0), (0, 0, 0), [
            (-5.4, -1.6, -3.75, 10.8, 2.8, 7.5, 'sash'),
            (-1.6, -2.0, -4.4, 3.2, 3.4, 1.0, 'sash_knot'),
        ]),
        part('right_sash_end', 'sash', (-1.0, 0.6, -4.3), (-0.12, 0, 0.07), [(-0.9, 0, -0.25, 1.8, 13, 0.5, 'sash_end')]),
        part('sash_end_short', 'sash', (1.1, 0.8, -4.2), (-0.1, 0, -0.04), [(-0.8, 0, -0.25, 1.6, 10, 0.5, 'sash_end_short')]),
        part('pendant', 'sash', (-3.8, 0.6, -4.1), (-0.12, 0, 0.03), [
            (-0.25, 0, -0.25, 0.5, 4, 0.5, 'cord'), (-1.5, 4, -0.5, 3, 3, 1, 'jade_disc'), (-0.5, 7, -0.4, 1, 4.5, 0.8, 'tassel')]),
    ]
    # ── the robe's skirt: a bell of silk falling from his waist to the ground in long gores, gathered under
    # the sash and flaring as they fall, curving out toward the hem, each lying a little over the next so that
    # the cloth hangs in deep folds, light on their ridges and dark in their troughs; open up the front over
    # the centipede's belly. Gold only where silk would carry it: the hem's border, the edges of the opening,
    # and a little embroidery. Inside, a dark lining (seen between the folds) ──
    skirt = []
    for t, (y0, h, w, d) in enumerate(((0.6, 7.2, 10.0, 7.0), (7.6, 7.0, 13.6, 9.6), (14.4, 6.8, 17.2, 12.0))):
        skirt.append((-w / 2, y0, -d / 2 + 0.4 * t, w, h, d, 'lining_core'))
    ps.append(part('skirt', 'body', (0, 0, 0), (0, 0, 0), skirt))
    for k_, (th, w0, w1, x0, x1, low, eye) in enumerate(gores()):
        rx0, rz0 = 5.0, 3.55
        ps.append(part('right_fold_%d' % k_, 'skirt', (-math.sin(th) * rx0, 0.9, -math.cos(th) * rz0 + 0.3), (-x0, -th, 0),
                       [(-w0 / 2, 0, -0.5, w0, GORE_UP + 0.4, 0.5, 'gore_up_front' if k_ == 0 else 'gore_up' if k_ % 2 == 0 else 'gore_up_dark')]))
        boxes = [(-w1 / 2, 0, -0.5, w1, GORE_LOW, 0.5, low)]
        if eye:
            boxes.append((-1.8, GORE_LOW * 0.42, -0.8, 3.6, 1.6, 0.3, 'robe_eye'))
        ps.append(part('right_fold_low_%d' % k_, 'right_fold_%d' % k_, (0, GORE_UP, 0), (-x1, 0, 0), boxes))
    ps += [
        part('right_back_panel', 'skirt', (-2.1, 1.0, 4.6), (0.3, 0, 0.05), [(-3.2, 0, 0, 3.4, 20.0, 0.5, 'back_panel')]),
    ]
    # the centipede's belly in the robe's front slit, and its little legs reaching out of it
    belly = []
    for t in range(1, TIERS - 1):
        y0, w, d, zc = tier_dims(t)
        belly.append((-1.3 + 0.05 * t, y0, zc - d / 2 - 0.15, 2.6 - 0.1 * t, 2.2, 0.4, 'belly_plate' if t % 2 else 'belly_plate_red'))
    ps.append(part('belly', 'skirt', (0, 0, 0), (0, 0, 0), belly))
    for i, t in enumerate((2, 4, 6)):
        y0, w, d, zc = tier_dims(t)
        ps.append(part('right_slit_leg_%d' % i, 'skirt', (-1.2, y0 + 1.2, zc - d / 2), (0.5, 0.6, 0.5),
                       [(-2.6, -0.35, -0.35, 2.6, 0.7, 0.7, 'fleg')]))
        ps.append(part('right_slit_leg_tip_%d' % i, 'right_slit_leg_%d' % i, (-2.5, 0, 0), (0, 0, -1.0),
                       [(-2.0, -0.25, -0.25, 2.0, 0.5, 0.5, 'fleg_tip')]))
    # the little legs round his hem, scuttling (they carry him)
    y0, w, d, zc = tier_dims(TIERS - 1)
    for i in range(6):
        z = -d / 2 + 1.5 + i * (d - 3) / 5 + zc
        ps.append(part('right_hem_leg_%d' % i, 'skirt', (-w / 2 + 0.6 + abs(i - 2.5) * 0.4, 20.2, z), (0, -0.5 + i * 0.2, 0.55),
                       [(-2.6, -0.4, -0.4, 2.6, 0.8, 0.8, 'fleg')]))
        ps.append(part('right_hem_leg_tip_%d' % i, 'right_hem_leg_%d' % i, (-2.5, 0, 0), (0, 0, -1.0),
                       [(-2.4, -0.3, -0.3, 2.4, 0.6, 0.6, 'fleg_tip')]))
    # the little legs along his flanks, out through the robe's seams
    for i in range(RINGS):
        y, h, w, d = ring_dims(i)
        ps.append(part('right_flank_leg_%d' % i, 'body', (-w / 2 + 0.2, y + 1.3, 1.2), (0, -0.55 - i * 0.06, 0.25 + i * 0.04),
                       [(-4.6, -0.45, -0.45, 4.6, 0.9, 0.9, 'fleg_big')]))
        ps.append(part('right_flank_leg_tip_%d' % i, 'right_flank_leg_%d' % i, (-4.5, 0, 0), (0, 0.2, -1.1),
                       [(-4.0, -0.3, -0.3, 4.0, 0.6, 0.6, 'fleg_tip')]))
    # ── the head: long, gaunt ──
    ps += [
        part('head', 'body', (0, -18.6, -1.3), (0.14, 0, 0), [
            (-3.0, -11.4, -3.0, 6, 9.6, 6, 'face'),
            (-2.2, -2.4, -2.7, 4.4, 1.2, 4.2, 'chin'),
            (-3.35, -7.0, -2.5, 6.7, 1.3, 4.0, 'cheekbone'),
            (-3.15, -8.5, -3.45, 6.3, 0.9, 1.0, 'brow_ridge'),
            (-0.6, -7.9, -3.75, 1.2, 3.6, 0.8, 'nose'),
            (-3.3, -11.8, -0.4, 6.6, 8.4, 3.7, 'hair_back'),
            (-3.3, -12.1, -3.3, 6.6, 1.2, 6.5, 'hair_top'),
        ]),
        part('right_ear', 'head', (-3.0, -7.4, -0.4), (0, 0.2, 0), [(-0.6, -1.2, -0.5, 0.6, 3.6, 1.3, 'ear')]),
        # the beard in three long locks to his sash, a thin moustache drooping past his chin
        part('beard', 'head', (0, -1.7, -3.0), (-0.42, 0, 0), [(-1.1, 0, -0.55, 2.2, 7.5, 1.1, 'beard')]),
        part('beard_mid', 'beard', (0, 7.3, 0), (0.36, 0, 0), [(-0.9, 0, -0.5, 1.8, 6.5, 1.0, 'beard_mid')]),
        part('beard_tip', 'beard_mid', (0, 6.3, 0), (0.08, 0, 0), [(-0.55, 0, -0.4, 1.1, 5, 0.8, 'beard_tip')]),
        part('right_beard_lock', 'head', (-1.7, -2.3, -2.7), (-0.36, 0.05, 0.12), [(-0.55, 0, -0.45, 1.1, 6.5, 0.9, 'beard_lock')]),
        part('right_beard_lock_tip', 'right_beard_lock', (0, 6.3, 0), (0.3, 0, -0.08), [(-0.4, 0, -0.35, 0.8, 4, 0.7, 'beard_tip')]),
        part('right_moustache', 'head', (-0.5, -4.0, -3.35), (0.06, 0.1, 0.32), [(-0.3, 0, -0.3, 0.6, 4.5, 0.6, 'moustache')]),
        part('right_moustache_tip', 'right_moustache', (0, 4.3, 0), (0.05, 0, -0.25), [(-0.25, 0, -0.25, 0.5, 6.5, 0.5, 'moustache')]),
        part('right_fang', 'head', (-1.9, -2.6, -2.5), (0.25, 0.2, 0.3), [(-0.4, 0, -0.4, 0.8, 2.2, 0.8, 'venom_claw')]),
        part('right_fang_tip', 'right_fang', (0, 2.1, 0), (-0.3, 0, -0.7), [(-0.3, 0, -0.3, 0.6, 1.8, 0.6, 'venom_tip')]),
        # the guan: a tall gold crown over his topknot, a jade pin through it, ribbons behind
        part('crown', 'head', (0, -12.0, 0.4), (0, 0, 0), [
            (-1.4, -2.0, -1.4, 2.8, 2.2, 2.8, 'topknot'),
            (-2.3, -1.4, -2.3, 4.6, 1.3, 4.6, 'crown_band'),
            (-1.9, -4.9, -2.1, 3.8, 3.6, 4.2, 'guan'),
            (-0.7, -6.0, -2.6, 1.4, 1.3, 5.2, 'guan_ridge'),
            (-5.4, -3.6, -0.3, 10.8, 0.6, 0.6, 'hairpin'),
            (-1.0, -4.0, -2.45, 2.0, 2.0, 0.5, 'guan_gem'),
        ]),
        part('right_ribbon', 'crown', (-1.8, -1.0, 1.8), (0.32, 0, 0.12), [(-0.6, 0, -0.1, 1.2, 8, 0.2, 'ribbon')]),
        part('right_ribbon_tail', 'right_ribbon', (0, 7.9, 0), (0.25, 0, 0.05), [(-0.6, 0, -0.1, 1.2, 7, 0.2, 'ribbon_tail')]),
        part('right_feeler', 'crown', (-1.0, -4.4, -0.8), (-1.0, 0.35, -0.3), [(-0.3, -10, -0.3, 0.6, 10, 0.6, 'feeler')]),
        part('right_feeler_tip', 'right_feeler', (0, -10, 0), (-0.75, 0, -0.1), [(-0.25, -13, -0.25, 0.5, 13, 0.5, 'feeler')]),
    ]
    for i in range(5):                                             # lotus petals round the guan's foot
        a = -math.pi / 2 + (i - 2) * 0.66
        ps.append(part('crown_petal_%d' % i, 'crown', (2.0 * math.sin(a), -1.6, -2.0 * math.cos(a) * 0.95),
                       (-0.3 * math.cos(a), a, 0.3 * math.sin(a)), [(-1.0, -2.6, -0.25, 2.0, 2.6, 0.5, 'crown_petal')]))
    # ── the arms ──
    ps += [
        part('right_arm', 'body', (-5.6, -13.6, 0), ARM, [
            (-2.6, -1.2, -1.9, 3.4, 4.6, 3.8, 'arm_ring'),
            (-2.4, 3.2, -1.8, 3.0, 4.8, 3.6, 'arm_ring_red'),
            (-2.9, 0.4, -1.3, 0.35, 1.3, 2.6, 'hide_eye_side'),
            (-2.7, 5.0, -0.8, 0.35, 1.3, 2.6, 'hide_eye_side'),
        ]),
        part('right_robe_sleeve', 'right_arm', (0, 0, 0), (0, 0, 0), [
            (-3.1, -1.8, -2.4, 4.6, 10.0, 4.8, 'robe_sleeve'),
            (-3.4, 2.4, -1.8, 0.3, 1.6, 3.6, 'robe_eye_side'),
        ]),
        # three tiers of armoured segments over his shoulder, each with its eye
        part('right_pauldron', 'right_arm', (-0.4, -1.8, 0), (0, 0, 0.6), [
            (-2.6, -1.4, -2.6, 4.0, 1.5, 5.2, 'plate1'),
            (-3.2, -0.1, -2.9, 4.2, 1.6, 5.8, 'plate2'),
            (-3.8, 1.3, -3.2, 4.5, 1.7, 6.4, 'plate3'),
            (-4.1, 1.5, -1.0, 0.35, 1.3, 2.6, 'hide_eye_side'),
            (-4.0, 2.8, -3.0, 0.8, 1.3, 6.0, 'plate_lip'),
        ]),
        part('right_forearm', 'right_arm', (-0.9, 7.8, 0), FOREARM, [
            (-1.5, 0, -1.5, 3.0, 7.8, 3.0, 'forearm'),
            (-1.9, 6.6, -1.9, 3.8, 1.6, 3.8, 'cuff_plate'),
            (-1.85, 2.0, -1.3, 0.35, 1.3, 2.6, 'hide_eye_side'),
        ]),
        # the great sleeve hanging from his forearm: long thin panels of silk, flaring wider to the cuff,
        # lined black, eyes through the silk
        part('right_great_sleeve', 'right_forearm', (0, 0.4, 0.6), aim([ARM, FOREARM], (0.04, -0.28, 0.12)), []),
        part('right_sleeve_outer', 'right_great_sleeve', (-2.9, -1.0, -0.6), (0, 0, 0.12), [
            (-0.25, 0, -5.4, 0.5, 6.0, 6.6, 'sleeve_a'), (-0.25, 5.8, -7.0, 0.5, 5.6, 8.4, 'sleeve_b'),
            (-0.25, 11.2, -8.8, 0.5, 6.0, 10.4, 'sleeve_c'),
            (0.25, 0.4, -5.2, 0.3, 5.6, 6.2, 'lining'), (0.25, 5.8, -6.8, 0.3, 5.6, 8.0, 'lining'), (0.25, 11.2, -8.6, 0.3, 5.6, 10.0, 'lining'),
            (-0.55, 7.0, -4.8, 0.3, 1.6, 3.6, 'robe_eye_side'), (-0.55, 12.8, -7.4, 0.3, 1.6, 3.6, 'robe_eye_side'),
        ]),
        part('right_sleeve_inner', 'right_great_sleeve', (2.7, -1.0, -0.6), (0, 0, -0.05), [
            (-0.25, 0, -5.4, 0.5, 6.0, 6.6, 'sleeve_a'), (-0.25, 5.8, -7.0, 0.5, 5.6, 8.4, 'sleeve_b'),
            (-0.25, 11.2, -8.8, 0.5, 5.2, 10.4, 'sleeve_c_short'),
            (-0.55, 0.4, -5.2, 0.3, 5.6, 6.2, 'lining'), (-0.55, 5.8, -6.8, 0.3, 5.6, 8.0, 'lining'),
        ]),
        part('right_sleeve_front', 'right_great_sleeve', (0, -1.0, -5.9), (-0.22, 0, 0), [
            (-2.9, 0, -0.25, 5.8, 6.0, 0.5, 'sleeve_fa'), (-3.3, 5.8, -0.25, 6.6, 5.6, 0.5, 'sleeve_fb'),
            (-3.8, 11.2, -0.25, 7.6, 5.8, 0.5, 'sleeve_fc'),
            (-1.8, 6.6, -0.55, 3.6, 1.6, 0.3, 'robe_eye'),
        ]),
        part('right_sleeve_back', 'right_great_sleeve', (0, -1.0, 0.9), (0.06, 0, 0), [
            (-2.9, 0, -0.25, 5.8, 6.0, 0.5, 'sleeve_fa'), (-3.2, 5.8, -0.25, 6.4, 5.6, 0.5, 'sleeve_fb'),
            (-3.5, 11.2, -0.25, 7.0, 5.4, 0.5, 'sleeve_fc_short'),
        ]),
        part('right_hand', 'right_forearm', (0, 8.0, 0), (0, 0, 0), [
            (-1.5, 0, -1.5, 3.0, 2.4, 3.0, 'palm'),
            (-1.55, 2.2, -1.5, 0.8, 3.4, 0.8, 'finger'),
            (-0.4, 2.2, -1.6, 0.8, 3.8, 0.8, 'finger'),
            (0.75, 2.2, -1.5, 0.8, 3.2, 0.8, 'finger'),
            (-1.4, 1.0, -2.2, 0.8, 2.2, 0.8, 'finger'),
        ]),
    ]
    # little legs down the backs of his arms
    for i in range(3):
        ps.append(part('right_arm_leg_%d' % i, 'right_arm', (-2.4, 1.0 + i * 2.6, 2.2), (0.3, -0.9, 0.4),
                       [(-2.8, -0.35, -0.35, 2.8, 0.7, 0.7, 'fleg')]))
        ps.append(part('right_arm_leg_tip_%d' % i, 'right_arm_leg_%d' % i, (-2.7, 0, 0), (0, 0, -0.8),
                       [(-2.2, -0.25, -0.25, 2.2, 0.5, 0.5, 'fleg_tip')]))
    for i in range(2):
        ps.append(part('right_forearm_leg_%d' % i, 'right_forearm', (-1.4, 1.6 + i * 3.0, 1.4), (0.2, -0.8, 0.3),
                       [(-2.4, -0.3, -0.3, 2.4, 0.6, 0.6, 'fleg')]))
    # his sword in his right hand, point up and forward
    ps += [
        part('sword', 'right_hand', (0, 2.0, -0.5), aim([ARM, FOREARM], (-0.08, 1.35, -0.28)), [
            (-0.3, -27.5, -1.25, 0.6, 23.5, 2.5, 'blade'),
            (-0.45, -27.0, -0.3, 0.9, 23.0, 0.6, 'blade_ridge'),
            (-0.25, -30.0, -0.8, 0.5, 2.6, 1.6, 'blade_tip'),
            (-0.2, -31.4, -0.3, 0.4, 1.5, 0.6, 'blade_tip'),
            (-0.9, -4.6, -2.4, 1.8, 1.2, 4.8, 'guard'),
            (-0.7, -5.4, -1.2, 1.4, 0.9, 2.4, 'guard_crown'),
            (-0.6, -3.4, -0.6, 1.2, 5.2, 1.2, 'grip'),
            (-0.9, 1.8, -0.9, 1.8, 1.4, 1.8, 'pommel'),
        ]),
        part('guard_wing_front', 'sword', (0, -4.0, -2.3), (0.55, 0, 0), [(-0.45, 0, -0.4, 0.9, 1.8, 0.8, 'guard_wing')]),
        part('guard_wing_back', 'sword', (0, -4.0, 2.3), (-0.55, 0, 0), [(-0.45, 0, -0.4, 0.9, 1.8, 0.8, 'guard_wing')]),
        part('tassel', 'sword', (0, 3.2, 0), (0.1, 0, 0), [(-0.3, 0, -0.3, 0.6, 2.2, 0.6, 'cord'), (-0.6, 2.2, -0.6, 1.2, 1.2, 1.2, 'tassel_bead'),
                                                         (-0.55, 3.4, -0.55, 1.1, 5.5, 1.1, 'tassel')]),
        # the teacup in his left hand (only while he pours)
        part('teacup', 'left_hand', (0, 2.6, -1.0), aim([LARM, LFOREARM], (0, 0, 0)), [
            (-1.2, -0.2, -1.2, 2.4, 1.8, 2.4, 'cup'), (-1.5, -0.6, -1.5, 3.0, 0.5, 3.0, 'cup_rim'), (-0.8, 1.6, -0.8, 1.6, 0.4, 1.6, 'cup_foot')]),
    ]
    # ── the centipede's tail, trailing from under the back of his robe ──
    prev = 'root'
    for s in range(TAIL):
        t = s / (TAIL - 1)
        w, h, L = 7.4 - 4.6 * t, 3.8 - 1.9 * t, 4.4 - 0.8 * t
        name = 'tail_%d' % s
        pivot = (0, 21.6, 4.6) if s == 0 else (0, 0.1, 4.0 - 0.8 * (s - 1) / (TAIL - 1))
        ps.append(part(name, prev, pivot, (0.04 if s == 0 else -0.02, 0, 0), [
            (-w / 2, -h / 2, 0, w, h, L, 'tseg'),
            (-w / 2 - 0.35, -h / 2 - 0.45, 0.35, w + 0.7, 1.0, L - 0.5, 'tplate'),
            (-w / 2 - 0.4, -0.4, L / 2 - 0.8, 0.45, 1.2, 1.6, 'hide_eye_side'),
            (w / 2 - 0.05, -0.4, L / 2 - 0.8, 0.45, 1.2, 1.6, 'hide_eye_side'),
        ]))
        ps.append(part('right_tleg_%d' % s, name, (-w / 2 + 0.2, h / 2 - 0.6, L / 2), (0, -0.25 + 0.5 * t, 0.45),
                       [(-3.4 + t, -0.4, -0.4, 3.4 - t, 0.8, 0.8, 'fleg')]))
        ps.append(part('right_tleg_tip_%d' % s, 'right_tleg_%d' % s, (-3.3 + t, 0, 0), (0, 0, -1.15),
                       [(-3.0, -0.3, -0.3, 3.0, 0.6, 0.6, 'fleg_tip')]))
        prev = name
    ps.append(part('right_tail_spine', prev, (-0.8, -0.2, 3.4), (0.18, -0.35, 0), [(-0.4, -0.4, 0, 0.8, 0.8, 6.5, 'tail_spine')]))
    ps.append(part('right_tail_hook', 'right_tail_spine', (0, 0, 6.3), (0.3, 0.6, 0), [(-0.3, -0.3, 0, 0.6, 0.6, 3.2, 'tail_hook')]))
    return mirrored(ps)


# ── Painting ─────────────────────────────────────────────────────────────────

def lid_eye(p, rect, iris_calm=0.0, lid=LID, rim=(60, 34, 16), open_=0.45):
    """An eye set into a face: a pointed almond outlined dark, a heavy upper lid with its crease above
    it, yellowed whites, a round golden iris slit black with a point of light; ablaze, it burns open
    (the blaze skin) round its slit."""
    fx, fy, fw, fh = rect
    cx, cy = fw / 2, fh / 2
    for i, j, xy in each(rect):
        u, v = (i + 0.5 - cx) / (fw / 2), (j + 0.5 - cy) / (fh / 2)
        hgt = max(0.0, 1 - abs(u) ** 1.5) ** 1.1 * 0.82
        if abs(u) > 0.97 or abs(v) > hgt + 0.3:
            crease = -hgt - 0.55 < v < -hgt - 0.3 and abs(u) < 0.75     # the lid's crease, a shadow in the cloth above it
            p.px(xy, noisy(p, k(rim, 0.72) if crease else rim, 3))
            continue
        if abs(v) > hgt:
            p.px(xy, (34, 16, 8))                                      # the lids' rims
            continue
        lid_line = -hgt + 2 * hgt * (1 - open_)
        if v < lid_line:
            base = mixc(lid, (236, 206, 160), 0.3 * (1 + v))
            p.lit(xy, base, calm=0.0, blaze=0.9, hot=mixc(EYE, EYE_HOT, 0.3), skin=noisy(p, base, 2))
            continue
        if v < lid_line + 0.22:
            p.px(xy, (24, 10, 6))                                      # the lash line
            continue
        d = math.hypot(u / 0.42, v / 0.95)
        if d > 1:
            sc = (226, 210, 160) if abs(u) < 0.8 or p.rnd.random() < 0.6 else (196, 92, 70)
            p.lit(xy, sc, calm=0.0, blaze=0.7, hot=mixc(EYE, EYE_HOT, 0.3), skin=noisy(p, sc, 3))
            continue
        if abs(u) < 0.09:
            p.px(xy, (10, 4, 2))                                       # the slit
            continue
        iris = mixc(mixc(EYE, EYE_DULL, 0.3), (110, 60, 12), d * d)
        if d < 0.5 and u < -0.15 and v < 0:
            iris = mixc(iris, (255, 250, 220), 0.6)                    # the point of light
        hot = mixc(EYE_HOT, EYE, min(1, d * 1.2))
        p.lit(xy, iris, calm=iris_calm, blaze=1.0, hot=hot, skin=noisy(p, iris, 2))


def eye_box(p, f, open_=0.45, calm=0.0, lid=LID, rim=(60, 34, 16), back=CHITIN):
    """A domed eye set in a socket: its face outward (whichever that is) an eye, its sides a rim."""
    big = max(f[s][2] * f[s][3] for s in SIDES)
    for s, r in f.items():
        if r[2] * r[3] >= big and s in SIDES:
            lid_eye(p, r, iris_calm=calm, lid=lid, rim=rim, open_=open_)
        else:
            for i, j, xy in each(r):
                p.px(xy, noisy(p, k(rim, LIGHTS[s]), 3))


def silk_robe(p, rect, s, base=YELLOW, light=1.0, damask=True, folds=6, deep=0.35):
    """Yellow silk: lit by its face, folds running down it, catching the light along their ridges and
    falling into ochre-brown in their troughs, darkening down the face; a damask lozenge woven through it."""
    fx, fy, fw, fh = rect
    L = LIGHTS[s] * light
    period = max(8, fw // folds) if s in SIDES else 9999
    for i, j, xy in each(rect):
        c = k(base, L * (1.06 - 0.18 * j / max(1, fh)))
        if s in SIDES:
            ph = ((i + 0.1 * j) % period) / period
            fold = math.cos(ph * 2 * math.pi)
            if fold > 0:
                c = mixc(c, k(YELLOW_LIGHT, L), fold ** 4 * 0.5)
            else:
                c = mixc(c, k((104, 60, 16), L), (-fold) ** 2 * deep)
        if damask:
            a, b = (i + 2 * S) % (6 * S), (j + 3 * S) % (6 * S)
            if abs(a - 3 * S) + abs(b - 3 * S) == 2 * S:
                c = mixc(c, k(OCHRE, L), 0.3)
        p.px(xy, noisy(p, c, 2))


def band(p, rect, y, h, colour=BLACK, pipe=GOLD):
    """A band of black (or another colour) across a face, piped gold at its edges."""
    fx, fy, fw, fh = rect
    for j in range(max(0, y), min(fh, y + h)):
        for i in range(fw):
            c = pipe if j in (y, y + h - 1) else colour
            p.px((fx + i, fy + j), noisy(p, c, 3))


def clouds(p, rect, y, h, colour=OCHRE, period=None, light=1.0):
    """A band of auspicious clouds: rolling three-lobed heads with a curl, outlined in thread."""
    fx, fy, fw, fh = rect
    period = period or max(12, int(h * 2.2))
    for j in range(max(0, y), min(fh, y + h)):
        for i in range(fw):
            u = (i % period) / period
            v = (j - y) / h
            top = 0.55 - 0.4 * abs(math.sin(u * math.pi * 3)) if u < 0.66 else 0.45
            r = math.hypot((u - 0.8) * period / h, v - 0.55)
            inside = (v > top and v < 0.92 and u < 0.7) or r < 0.32
            outline = (inside and (abs(v - top) < 1.5 / h or abs(v - 0.92) < 1.5 / h)) or abs(r - 0.32) < 1.2 / h \
                or (u < 0.7 and inside and abs(math.sin(u * math.pi * 3)) < 0.12)
            if outline:
                p.px((fx + i, fy + j), k(mixc(colour, BLACK, 0.3), light))
            elif inside:
                p.px((fx + i, fy + j), noisy(p, k(mixc(colour, YELLOW_LIGHT, 0.45), light), 2))


TRIGRAMS = ['111', '011', '101', '001', '110', '010', '100', '000']


def trigrams(p, rect, y, h, start=0):
    """The eight trigrams in black on yellow, between gold rules."""
    fx, fy, fw, fh = rect
    band(p, rect, y, S, GOLD, GOLD)
    band(p, rect, y + h - S, S, GOLD, GOLD)
    bar_h = max(1, h // 8)
    step = max(6 * S, fw // 4)
    w = int(step * 0.6)
    for n, x0 in enumerate(range(S, fw - w, step)):
        t = TRIGRAMS[(n + start) % 8]
        for row, bit in enumerate(t):
            yy = fy + y + 2 * S + row * bar_h * 2
            for i in range(w):
                if bit == '1' or i < w * 0.42 or i > w * 0.58:
                    for b in range(bar_h):
                        p.px((fx + x0 + i, yy + b), BLACK)


def taiji(p, rect, cx=None, cy=None, r=None):
    fx, fy, fw, fh = rect
    r = r or min(fw, fh) * 0.3
    cx = fw / 2 if cx is None else cx
    cy = fh * 0.42 if cy is None else cy
    for i, j, xy in each(rect):
        x, y = i + 0.5 - cx, j + 0.5 - cy
        d = math.hypot(x, y)
        if d > r + 1.5 * S:
            continue
        if d > r:
            p.px(xy, GOLD if d > r + S else BLACK)
            continue
        white = x > 0
        if math.hypot(x, y + r / 2) < r / 2:
            white = True
        if math.hypot(x, y - r / 2) < r / 2:
            white = False
        if math.hypot(x, y + r / 2) < r / 7:
            white = False
        if math.hypot(x, y - r / 2) < r / 7:
            white = True
        p.px(xy, WHITE if white else BLACK)


def eye_motif(p, x, y, w=None):
    """An eye embroidered in gold thread on the silk: shut, a line of lashes; ablaze, it burns open."""
    w = w or 5 * S
    h = max(3, w // 2)
    for i in range(w):
        u = (i + 0.5) / w * 2 - 1
        top = int(round((1 - u * u) * h / 2))
        for j in range(-top, top + 1):
            xy = (x + i, y + j)
            if abs(j) == top:
                p.px(xy, GOLD_DEEP)
            elif abs(u) < 0.15:
                p.px(xy, (40, 20, 8))
            else:
                p.lit(xy, YELLOW_DEEP, calm=0.0, blaze=1.0, hot=mixc(EYE_HOT, EYE, abs(u)), skin=mixc(YELLOW, GOLD_DEEP, 0.5))
        if (i % 3 == 0) and abs(u) < 0.8:
            p.px((x + i, y + h // 2 + 1), GOLD_DEEP)


def hide(p, f, base=CHITIN, ring=True):
    """His own hide: glossy black-umber segments, a gold seam at each one's lower edge."""
    chitin(p, f, base, CHITIN_SHEEN)
    if ring:
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                for j in range(fh - S, fh):
                    p.px((fx + i, fy + j), noisy(p, k(BAND, LIGHTS[s] * (0.7 if j == fh - 1 else 1)), 4))
                p.px((fx + i, fy), k(CHITIN, 0.5))


def hair(p, f, grey=0.0):
    for s, r in f.items():
        for i, j, xy in each(r):
            c = HAIR_SHEEN if (i + j // 4) % 5 == 0 else HAIR
            if grey and p.rnd.random() < grey:
                c = HAIR_GREY
            p.px(xy, noisy(p, k(c, LIGHTS[s]), 3))


def lord_painter():
    def paint_box(p, style, f, dims):
        fr = f['front']
        if style in ('ring', 'ring_red', 'arm_ring', 'arm_ring_red', 'forearm', 'neck_hide'):
            hide(p, f, base=(92, 18, 20) if style.endswith('red') else CHITIN)
            if style.startswith('ring'):                                   # painted eyes between the raised ones, a dim glow
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for x0 in range(S, fw - 3 * S, 7 * S):
                        lid_eye(p, (fx + x0 + 3 * S, fy + S, 3 * S, 2 * S), iris_calm=0.35, open_=0.6)
        elif style in ('hide_eye', 'hide_eye_side'):
            eye_box(p, f, open_=0.75, calm=0.8, lid=(90, 56, 30))
        elif style in ('robe_eye', 'robe_eye_side', 'skirt_eye'):
            eye_box(p, f, open_=0.7, calm=0.3, lid=mixc(SKIN, YELLOW_DEEP, 0.25), rim=YELLOW)
        elif style == 'sternum':
            chitin(p, f, BAND, (255, 230, 160))
            edges(p, f, k(CHITIN, 1), width=S // 2)
        elif style == 'dorsal' or style.startswith('plate'):
            chitin(p, f, CHITIN, CHITIN_SHEEN)
            for s in SIDES + ('top',):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    for j in range(S):
                        p.px((fx + i, fy + fh - 1 - j), noisy(p, k(GOLD, LIGHTS[s] * (0.85 if j else 1.0)), 4))
            if style == 'dorsal':                                  # a ridge-spine down the plate's middle
                fx, fy, fw, fh = f['back']
                for j in range(fh - S):
                    for i in range(fw // 2 - S // 2, fw // 2 + S // 2):
                        p.px((fx + i, fy + j), (200, 150, 70))
        elif style in ('robe_chest', 'robe_waist', 'robe_yoke', 'robe_sleeve', 'skirt1', 'skirt2', 'skirt3', 'great_sleeve',
                       'apron', 'back_panel', 'over_panel', 'great_sleeve_low'):
            for s, r in f.items():
                silk_robe(p, r, s, folds=3 if style in ('robe_sleeve', 'apron', 'back_panel') else 5)
            if style == 'robe_chest':
                fx, fy, fw, fh = f['back']
                taiji(p, f['back'], cy=fh * 0.55, r=fh * 0.34)
                band(p, f['back'], 0, S, GOLD_DEEP, GOLD_DEEP)
                # the slit down his back, the ridge through it: dark at its middle
                for j in range(fh):
                    for i in range(fw // 2 - S, fw // 2 + S):
                        p.px((fx + i, fy + j), k(BLACK, 1))
                for s in ('right', 'left'):
                    eye_motif(p, f[s][0] + S, f[s][1] + f[s][3] - 4 * S, w=f[s][2] - 2 * S)
                eye_motif(p, fr[0] + fr[2] // 2 - 3 * S, fr[1] + 4 * S, w=6 * S)
            elif style == 'robe_waist':
                fx, fy, fw, fh = f['back']
                for j in range(fh):
                    for i in range(fw // 2 - S, fw // 2 + S):
                        p.px((fx + i, fy + j), k(BLACK, 1))
                for s in ('front',):
                    clouds(p, f[s], f[s][3] - 6 * S, 5 * S)
            elif style == 'robe_yoke':
                for s in SIDES:
                    band(p, f[s], 0, f[s][3], BLACK, GOLD)
                fx, fy, fw, fh = f['top']
                for i, j, xy in each(f['top']):
                    if j < S or j >= fh - S or i < S or i >= fw - S:
                        p.px(xy, GOLD)
            elif style == 'robe_sleeve':
                for s in SIDES:
                    clouds(p, f[s], 3 * S, 5 * S)
            elif style == 'skirt1':
                for s in SIDES:
                    clouds(p, f[s], f[s][3] - 7 * S, 6 * S)
                fx, fy, fw, fh = f['back']
                for j in range(fh):
                    for i in range(fw // 2 - S, fw // 2 + S):
                        p.px((fx + i, fy + j), BLACK)
            elif style == 'skirt2':
                for s in SIDES:
                    band(p, f[s], 0, S, GOLD_DEEP, GOLD_DEEP)
                    clouds(p, f[s], f[s][3] - 7 * S, 6 * S)
                    for x0 in range(4 * S, f[s][2] - 6 * S, 11 * S):
                        eye_motif(p, f[s][0] + x0, f[s][1] + 3 * S, w=4 * S)
            elif style == 'skirt3':
                for n, s in enumerate(SIDES):
                    band(p, f[s], 0, S, GOLD_DEEP, GOLD_DEEP)
                    trigrams(p, f[s], f[s][3] - 13 * S, 9 * S, start=n * 2)
                    band(p, f[s], f[s][3] - 2 * S, 2 * S, BLACK, GOLD)
            elif style == 'great_sleeve':
                for s in SIDES:
                    clouds(p, f[s], f[s][3] - 8 * S, 6 * S)
                    eye_motif(p, f[s][0] + f[s][2] // 2 - 3 * S, f[s][1] + 3 * S, w=6 * S)
            elif style == 'great_sleeve_low':
                for s in SIDES:
                    band(p, f[s], 0, S, GOLD_DEEP, GOLD_DEEP)
                    trigrams(p, f[s], f[s][3] - 11 * S, 9 * S, start=SIDES.index(s) * 3)
                for i, j, xy in each(f['bottom']):                    # its mouth: the black lining, dark within
                    fx, fy, fw, fh = f['bottom']
                    inner = S < i < fw - S and S < j < fh - S
                    p.px(xy, noisy(p, (20, 14, 14) if inner else BLACK, 3))
            elif style == 'apron':
                fx, fy, fw, fh = fr
                vedges(p, fr, BLACK, S)
                vedges(p, (fx + S, fy, fw - 2 * S, fh), GOLD, S // 2)
                taiji(p, fr, cy=fh * 0.28, r=fw * 0.3)
                for n in range(3):
                    eye_motif(p, fx + fw // 2 - 3 * S, fy + int(fh * 0.48) + n * 7 * S, w=6 * S)
                band(p, fr, fh - 4 * S, 4 * S, BLACK, GOLD)
            elif style == 'back_panel':
                fx, fy, fw, fh = f['back']
                vedges(p, f['back'], BLACK, S)
                trigrams(p, f['back'], fh - 16 * S, 9 * S, start=5)
                band(p, f['back'], fh - 4 * S, 4 * S, BLACK, GOLD)
                eye_motif(p, fx + fw // 2 - 3 * S, fy + 8 * S, w=6 * S)
            elif style == 'over_panel':
                for s in ('right', 'left'):
                    fx, fy, fw, fh = f[s]
                    vedges(p, f[s], BLACK, S)
                    band(p, f[s], fh - 4 * S, 4 * S, BLACK, GOLD)
                    eye_motif(p, fx + fw // 2 - 3 * S, fy + 6 * S, w=6 * S)
                    eye_motif(p, fx + fw // 2 - 3 * S, fy + 18 * S, w=6 * S)
        elif style.startswith('gore_'):
            # one of the skirt's gores: temple-yellow silk hanging in a single deep fold, its ridge catching the
            # light and its edges falling into shadow where it passes under the next; deepening toward ochre as
            # it falls; the darker gores lying further in. The lower lengths carry the hem's embroidery: a band
            # of ochre clouds, the trigrams, and the black hem piped gold. The gores beside the opening are edged
            # in black and gold down their length.
            dark = style.endswith('_dark')
            low = style.startswith('gore_low') or style == 'gore_front'
            front = style in ('gore_front', 'gore_up_front')
            for s_, r in f.items():
                fx, fy, fw, fh = r
                L = LIGHTS[s_] * (0.86 if dark else 1.0)
                for i, j, xy in each(r):
                    v = j / max(1, fh - 1)
                    q = (0.25 + 0.45 * v) if low else 0.08 + 0.2 * v
                    c = mixc(YELLOW, OCHRE, q)
                    if s_ in ('front', 'back'):
                        u = (i + 0.5) / fw
                        ridge = math.cos((u - 0.5) * math.pi)          # 1 on the fold's crest, 0 at its edges
                        c = mixc(c, YELLOW_LIGHT, 0.4 * ridge ** 6)
                        c = mixc(c, (96, 54, 14), 0.55 * (1 - ridge) ** 1.5)
                        a_, b_ = (i + 2 * S) % (6 * S), (j + 3 * S) % (6 * S)   # the damask lozenge woven through
                        if abs(a_ - 3 * S) + abs(b_ - 3 * S) == 2 * S:
                            c = mixc(c, OCHRE, 0.25)
                    else:
                        c = mixc(c, (96, 54, 14), 0.5)
                    if s_ == 'back':
                        c = mixc((70, 40, 14), (40, 22, 10), v)          # its inside: the dark lining
                    if not low and v < 0.12:
                        c = k(c, 0.75 + 2.0 * v)                        # gathered in shadow under the sash
                    p.px(xy, noisy(p, k(c, L), 2))
            if low:
                for s_ in ('front',):
                    fx, fy, fw, fh = f[s_]
                    clouds(p, f[s_], fh - 15 * S, 5 * S, light=LIGHTS[s_] * (0.9 if dark else 1.0))
                    trigrams(p, f[s_], fh - 9 * S - S // 2, 6 * S, start=fx // 7)
                    band(p, f[s_], fh - 3 * S, 3 * S, BLACK, GOLD)
                    if not dark and s_ == 'front':
                        eye_motif(p, fx + fw // 2 - 2 * S, fy + int(fh * 0.22), w=4 * S)
                for s_ in ('right', 'left'):
                    fx, fy, fw, fh = f[s_]
                    for j in range(fh - 3 * S, fh):
                        for i in range(fw):
                            p.px((fx + i, fy + j), noisy(p, GOLD if j in (fh - 3 * S, fh - 1) else BLACK, 3))
            if front:
                for s_ in ('front',):
                    fx, fy, fw, fh = f[s_]
                    vedges(p, f[s_], BLACK, S)
                    vedges(p, (fx + S, fy, fw - 2 * S, fh), GOLD, S // 2)
        elif style == 'lining_core':
            # inside the robe, between and behind its folds: the dark lining
            for s_, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, k((44, 26, 10), LIGHTS[s_] * (0.85 - 0.25 * j / max(1, r[3]))), 3))
        elif style in ('belly_plate', 'belly_plate_red'):
            chitin(p, f, CHITIN if style == 'belly_plate' else (96, 20, 18), (230, 170, 120))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), BAND)
                    p.px((fx + i, fy), k(CHITIN, 0.5))
        elif style.startswith('sleeve_'):
            for s, r in f.items():
                silk_robe(p, r, s, base=mixc(YELLOW, OCHRE, 0.18 if style[-1] != 'c' and not style.endswith('short') else 0.3),
                          folds=3, deep=0.6)
            if style.endswith('c') or style.endswith('c_short'):   # the cuff: black, gold-piped
                for s in SIDES:
                    band(p, f[s], f[s][3] - 3 * S, 3 * S, BLACK, GOLD)
            elif style.endswith('b'):
                for s in SIDES:
                    clouds(p, f[s], S, 4 * S)
        elif style == 'lining':
            cloth(p, f, (28, 20, 20), folds=True)
        elif style == 'sleeve_cuff':
            cloth(p, f, BLACK, folds=False)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), GOLD)
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
            for i, j, xy in each(f['bottom']):
                fx, fy, fw, fh = f['bottom']
                inner = S < i < fw - S and S < j < fh - S
                p.px(xy, (14, 10, 10) if inner else BLACK)
                if inner:
                    p.clear(xy)
        elif style == 'hem':
            cloth(p, f, BLACK, folds=False)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), GOLD)
                    if (i // S) % 4 == 0:
                        p.px((fx + i, fy + fh // 2), GOLD_DEEP)
        elif style == 'inner_collar':
            cloth(p, f, WHITE, folds=False)
        elif style in ('robe_collar', 'high_collar', 'high_collar_side'):
            cloth(p, f, BLACK, folds=False)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(f[s]):
                    if i < S or i >= fw - S or (style != 'robe_collar' and (j < S)):
                        p.px(xy, GOLD)
            if style == 'robe_collar':                            # trigrams worked down it in gold
                fx, fy, fw, fh = fr
                for n in range(0, fh // (6 * S)):
                    t = TRIGRAMS[n % 8]
                    for row, bit in enumerate(t):
                        y = fy + 3 * S + n * 6 * S + row * S * 3 // 2
                        for i in range(2 * S, fw - 2 * S):
                            if bit == '1' or abs(i - fw / 2) > S * 0.6:
                                p.px((fx + i, y), GOLD_DEEP)
            if style == 'high_collar':
                fx, fy, fw, fh = f['front']                      # its inside, toward his head: white lining
                for i, j, xy in each(f['front']):
                    if S <= i < fw - S and j >= S:
                        p.px(xy, noisy(p, WHITE, 3))
                fx, fy, fw, fh = f['back']
                taiji(p, f['back'], cy=fh * 0.55, r=fh * 0.3)
        elif style in ('sash', 'sash_knot', 'sash_end', 'sash_end_short'):
            cloth(p, f, CRIMSON, folds=style != 'sash')
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                if style == 'sash':
                    for i in range(fw):
                        p.px((fx + i, fy), GOLD)
                        p.px((fx + i, fy + fh - 1), GOLD)
                        if (i // S) % 6 == 0:
                            for j in range(S, fh - S):
                                p.px((fx + i, fy + j), CRIMSON_DEEP)
                elif style.startswith('sash_end'):
                    vedges(p, f[s], GOLD_DEEP, 1)
                    for i in range(fw):
                        for j in range(fh - 2 * S, fh):
                            p.px((fx + i, fy + j), GOLD if j < fh - S else BLACK)
        elif style in ('jade_disc', 'hairpin'):
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    c = mixc(JADE, JADE_DEEP, 0.5 + 0.5 * math.sin((i + j) * 0.7))
                    if style == 'jade_disc' and s in ('front', 'back') and math.hypot(i - fw / 2 + 0.5, j - fh / 2 + 0.5) < fw * 0.18:
                        c = (20, 20, 20)
                    p.px(xy, noisy(p, k(c, LIGHTS[s]), 3))
            if style == 'hairpin':
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in list(range(2 * S)) + list(range(fw - 2 * S, fw)):
                        for j in range(fh):
                            p.px((fx + i, fy + j), GOLD)
        elif style == 'cord':
            cloth(p, f, RED, folds=False)
        elif style == 'tassel':
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, k(RED if i % 2 else (140, 18, 20), LIGHTS[s]), 4))
            for s in SIDES:
                cut_point(p, f[s], keep=1.5, sharp=1.2)
        elif style == 'tassel_bead':
            gold(p, f, GOLD)
        elif style == 'face':
            paint_lord_face(p, f)
        elif style in ('chin', 'cheekbone', 'nose', 'ear', 'palm', 'finger'):
            cloth(p, f, SKIN, folds=False, fall=0.2)
            if style == 'cheekbone':                               # lit along its top, hollow beneath
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        for j in range(fh // 2):
                            p.px((fx + i, fy + j), SKIN_LIGHT)
                        p.px((fx + i, fy + fh - 1), SKIN_HOLLOW)
                fx, fy, fw, fh = f['bottom']
                for i, j, xy in each(f['bottom']):
                    p.px(xy, SKIN_HOLLOW)
            if style == 'nose':
                fx, fy, fw, fh = fr
                for j in range(fh):
                    p.px((fx, fy + j), SKIN_SHADE)
                    p.px((fx + fw - 1, fy + j), SKIN_SHADE)
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), SKIN_HOLLOW)
            if style == 'finger':                                  # long, knuckled, a dark nail like a claw
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        for j in range(fh - 3 * S, fh):
                            p.px((fx + i, fy + j), noisy(p, (40, 26, 20), 3))
                        p.px((fx + i, fy + fh // 3), SKIN_SHADE)
            if style == 'chin':
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        p.px((fx + i, fy), SKIN_SHADE)
            if style == 'ear':
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for j in range(fh):
                        for i in range(fw):
                            if j > fh * 0.6 and (i in (0, fw - 1)):
                                p.px((fx + i, fy + j), SKIN_SHADE)
        elif style == 'brow_ridge':
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, k(SKIN_SHADE if s != 'top' else SKIN, LIGHTS[s]), 3))
            # the brows: black, heavy at the bridge, slanting up and out, cruel
            fx, fy, fw, fh = fr
            for i in range(fw):
                u = abs(i + 0.5 - fw / 2) / (fw / 2)
                if u < 0.12:
                    continue
                thick = int(fh * (1.0 - 0.55 * u)) + 1
                top = int((fh - thick) * (1 - u) * 0.4)
                for j in range(top, min(fh, top + thick)):
                    p.px((fx + i, fy + j), noisy(p, (16, 12, 14), 2))
            for s in ('bottom',):
                for i, j, xy in each(f[s]):
                    p.px(xy, SKIN_HOLLOW)
        elif style in ('hair_back', 'hair_top', 'topknot'):
            hair(p, f, grey=0.05)
            if style == 'hair_back':
                for i, j, xy in each(fr):
                    p.px(xy, HAIR)
        elif style in ('crown_band', 'guan', 'guan_ridge', 'crown_petal'):
            gold(p, f, GOLD)
            if style == 'guan':                                    # cloud scrolls chased into its sides
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i, j, xy in each(f[s]):
                        if (abs(math.sin((i + j * 0.5) / S * 0.9)) < 0.15) and S < j < fh - S:
                            p.px(xy, GOLD_DEEP)
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 1), GOLD_DEEP)
            if style == 'crown_petal':
                for s in ('front', 'back'):
                    cut_point(p, f[s], keep=0.5, down=False, sharp=0.6)
                    fx, fy, fw, fh = f[s]
                    for j in range(fh):
                        p.px((fx + fw // 2, fy + j), GOLD_DEEP)
        elif style == 'guan_gem':
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.lit(xy, (60, 200, 150), calm=0.3, blaze=0.6, skin=k(JADE, 0.8 + 0.4 * ((i + j) % 3 == 0)))
        elif style in ('ribbon', 'ribbon_tail'):
            cloth(p, f, BLACK)
            for s in ('front', 'back'):
                vedges(p, f[s], GOLD, 1)
            if style == 'ribbon_tail':
                for s in ('front', 'back'):
                    cut_point(p, f[s], keep=1, down=True, sharp=0.9)
        elif style in ('moustache', 'beard', 'beard_mid', 'beard_tip', 'beard_lock'):
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    c = HAIR_SHEEN if (i + j // 5) % 3 == 0 else (12, 10, 12)
                    if style in ('beard_tip', 'beard_mid') and p.rnd.random() < 0.18 + 0.2 * (j / fh):
                        c = HAIR_GREY
                    p.px(xy, noisy(p, k(c, LIGHTS[s]), 3))
            if style in ('beard_tip', 'moustache'):
                for s in SIDES:
                    cut_point(p, f[s], keep=0.5, sharp=0.3 if style == 'beard_tip' else 0.12)
        elif style in ('venom_claw', 'venom_tip'):
            for s, r in f.items():
                for i, j, xy in each(r):
                    t = (j + 0.5) / r[3]
                    c = mixc((60, 22, 14), (110, 20, 16), t) if style == 'venom_claw' else mixc((110, 20, 16), (236, 220, 180), t)
                    p.px(xy, noisy(p, k(c, LIGHTS[s]), 3))
            if style == 'venom_tip':
                for s in SIDES:
                    cut_point(p, f[s], keep=0.5, sharp=0.5)
        elif style == 'feeler':
            chitin(p, f, (70, 36, 18), gloss=False)
            for s in SIDES:
                for i, j, xy in each(f[s]):
                    if (j // S) % 3 == 0:
                        p.px(xy, BAND)
        elif style in ('fleg', 'fleg_big', 'fleg_tip'):
            chitin(p, f, (120, 70, 28) if style != 'fleg_tip' else (64, 34, 16), (240, 200, 140))
            for s in SIDES + ('top', 'bottom'):
                fx, fy, fw, fh = f[s]
                along = fw if s in ('front', 'back', 'top', 'bottom') else fh
                for i, j, xy in each(f[s]):
                    a = i if s in ('front', 'back', 'top', 'bottom') else j
                    if style != 'fleg_tip' and (a // S) % 4 == 3:
                        p.px(xy, BAND)
                    if style == 'fleg_tip' and a < along * 0.3:
                        p.px(xy, (230, 190, 100))
        elif style in ('tseg', 'tplate'):
            if style == 'tseg':
                chitin(p, f, CHITIN, CHITIN_SHEEN)
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        for j in range(fh):
                            if (s in ('right', 'left') and i < S) or (s in ('front', 'back') and j < S):
                                p.px((fx + i, fy + j), BAND)
            else:
                chitin(p, f, UMBER, (230, 180, 110))
                fx, fy, fw, fh = f['top']
                for i, j, xy in each(f['top']):
                    if abs(i - fw / 2 + 0.5) < S * 0.6:
                        p.px(xy, k(CHITIN, 1))
                    elif j >= fh - S:
                        p.px(xy, GOLD)
                for x0 in (S, fw - 4 * S):                         # an eye on each side of its back
                    lid_eye(p, (fx + x0, fy + fh // 2 - S, 3 * S, 2 * S), iris_calm=0.4, open_=0.6)
        elif style in ('tail_spine', 'tail_hook'):
            chitin(p, f, (90, 44, 20) if style == 'tail_spine' else (170, 120, 60), (240, 200, 140))
        elif style == 'cuff_plate':
            chitin(p, f, BAND, (255, 230, 160))
        elif style in ('blade', 'blade_tip', 'blade_ridge'):
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    t = (i + 0.5) / fw
                    c = mixc(STEEL, STEEL_DEEP, abs(t - 0.5) * 1.4) if s in ('right', 'left') else STEEL_DEEP
                    if style == 'blade_ridge':
                        c = k(STEEL, 1.1)
                    p.px(xy, noisy(p, k(c, LIGHTS[s]), 2))
            if style == 'blade':                                    # the seven stars of the Dipper inlaid in gold, glinting
                for s in ('right', 'left'):
                    fx, fy, fw, fh = f[s]
                    stars = [(0.3, 0.70), (0.7, 0.66), (0.62, 0.58), (0.35, 0.55), (0.5, 0.45), (0.38, 0.36), (0.55, 0.26)]
                    for a, b in stars:
                        for di in range(-1, 2):
                            for dj in range(-1, 2):
                                if abs(di) + abs(dj) < 2:
                                    p.lit((fx + int(a * fw) + di, fy + int(b * fh) + dj), GOLD, calm=0.6, blaze=1.0, hot=EYE_HOT)
                    for j in range(int(fh * 0.25), int(fh * 0.72)):
                        if j % (2 * S) == 0:
                            pass
            if style == 'blade_tip':
                for s in ('right', 'left'):
                    cut_point(p, f[s], keep=0.5, down=False, sharp=0.5)
        elif style in ('guard', 'guard_crown', 'pommel', 'guard_wing'):
            gold(p, f, GOLD)
            if style == 'guard':
                for s in ('right', 'left'):                         # a taotie mask in the guard
                    fx, fy, fw, fh = f[s]
                    for i, j, xy in each(f[s]):
                        u = abs(i + 0.5 - fw / 2) / (fw / 2)
                        if (0.25 < u < 0.45 and j < fh * 0.6) or (u < 0.15 and j > fh * 0.4):
                            p.px(xy, GOLD_DEEP)
            if style == 'pommel':
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    p.lit((fx + fw // 2, fy + fh // 2), RED, calm=0.3, blaze=0.5)
        elif style == 'grip':
            cloth(p, f, (60, 20, 24), folds=False)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    for i in range(fw):
                        if (i + j) % (2 * S) < S // 2:
                            p.px((fx + i, fy + j), (120, 40, 36))
        elif style in ('cup', 'cup_rim', 'cup_foot'):
            for s, r in f.items():
                for i, j, xy in each(r):
                    c = CELADON if s != 'top' or style != 'cup' else (110, 140, 60)
                    p.px(xy, noisy(p, k(c, LIGHTS[s] * (1.1 if (i // S) % 3 == 0 else 1)), 3))
        elif style == 'neck':
            hide(p, f, ring=False)
            fx, fy, fw, fh = fr
            for i, j, xy in each(fr):
                p.px(xy, noisy(p, k(SKIN_SHADE, 0.9), 3))
        else:
            cloth(p, f, YELLOW)
    return paint_box


def paint_lord_face(p, f):
    """A long sallow face: sunken cheeks under high bones, narrow slanting eyes glinting gold under a
    heavy brow, a third eye shut on his forehead (burning open in his glare), a long nose, a thin
    hard mouth turned down, black hair swept back from a peak."""
    for s, r in f.items():
        fx, fy, fw, fh = r
        for i, j, xy in each(r):
            t = j / max(1, fh)
            p.px(xy, noisy(p, k(SKIN, LIGHTS[s] * (1.06 - 0.12 * t)), 3))
    for s in ('right', 'left'):                                      # hair over the temples and back of his head
        fx, fy, fw, fh = f[s]
        for i, j, xy in each(f[s]):
            back = (i > fw * 0.45) if s == 'right' else (i < fw * 0.55)
            if j < fh * 0.22 or (back and j < fh * 0.75):
                p.px(xy, noisy(p, HAIR, 3))
            elif j > fh * 0.55 and not back:                         # the hollow under the cheekbone
                p.px(xy, noisy(p, mixc(SKIN, SKIN_HOLLOW, 0.5), 3))
    for i, j, xy in each(f['top']):
        p.px(xy, noisy(p, HAIR, 3))
    fx, fy, fw, fh = f['front']                                      # 24 x 38 at four texels
    U = lambda x: int(round(x * fw / 24))
    V = lambda y: int(round(y * fh / 38))
    P = lambda x, y, c: p.px((fx + U(x), fy + V(y)), c)
    cx = fw / 2
    # the hairline, a peak at the middle
    for i in range(fw):
        u = abs(i + 0.5 - cx) / cx
        line = V(5) + int(V(3) * (1 - u)) * (-1) + V(2)
        line = V(7) - int(V(4) * (1 - u) ** 2) if u < 0.9 else fh // 2
        for j in range(0, max(0, line)):
            p.px((fx + i, fy + j), noisy(p, HAIR if (i + j // 3) % 6 else HAIR_SHEEN, 2))
        if u > 0.82:
            for j in range(0, int(fh * 0.55)):
                p.px((fx + i, fy + j), noisy(p, HAIR, 2))
    # shading: temples, the deep hollows under the cheekbones, the long jaw
    for i, j, xy in each(f['front']):
        u = (i + 0.5 - cx) / cx
        v = (j + 0.5) / fh
        if abs(u) > 0.82 and v < 0.55:
            continue
        if 0.58 < v < 0.84 and 0.48 < abs(u) < 0.9:
            p.px(xy, noisy(p, mixc(SKIN, SKIN_HOLLOW, 0.55 + 0.3 * (abs(u) - 0.48)), 2))
        elif 0.48 < v < 0.56 and 0.4 < abs(u) < 0.95:
            p.px(xy, noisy(p, SKIN_LIGHT, 2))                          # light along the cheekbones
        elif v > 0.86 and abs(u) > 0.55:
            p.px(xy, noisy(p, SKIN_SHADE, 2))
    # the third eye: a shut vertical slit, gold-lidded; open and burning in his glare
    for j in range(V(9), V(14)):
        for i in range(-U(1), U(1)):
            t = abs((j - (V(9) + V(14)) / 2) / ((V(14) - V(9)) / 2))
            w = U(1) * (1 - t * t)
            if abs(i + 0.5) <= w + 0.5:
                xy = (fx + int(cx) + i, fy + j)
                if abs(i + 0.5) < 1:
                    p.lit(xy, (60, 30, 12), calm=0.0, blaze=1.0, hot=EYE_HOT, skin=(60, 30, 12))
                else:
                    p.lit(xy, (150, 110, 60), calm=0.0, blaze=1.0, hot=EYE, skin=(150, 110, 60))
    for j in range(V(8), V(15)):
        p.px((fx + int(cx) - U(1) - 1, fy + j), SKIN_SHADE)
        p.px((fx + int(cx) + U(1), fy + j), SKIN_SHADE)
    # the sockets: shadowed under the brow
    for side in (-1, 1):
        for i in range(U(2), U(11)):
            for j in range(V(17), V(19)):
                x = fx + (int(cx) - 1 - i if side < 0 else int(cx) + i)
                p.px((x, fy + j), noisy(p, SKIN_SHADE, 2))
    # the eyes: long, narrow, slanting up to their outer corners; a thin line of gold iris and a black slit
    for side in (-1, 1):
        n = U(8)
        for i in range(n):
            x = fx + (int(cx) - U(2) - 1 - i if side < 0 else int(cx) + U(2) + i)
            t = i / max(1, n - 1)
            y = fy + V(20) - int(t * V(3))
            lidh = max(1, int(V(2) * (1 - abs(t - 0.45) * 1.3)))
            p.px((x, y - 1), (18, 10, 8))
            for j in range(lidh):
                if 0.25 < t < 0.65:
                    if abs(t - 0.42) < 0.06:
                        p.px((x, y + j), (10, 4, 2))
                    else:
                        p.lit((x, y + j), EYE, calm=0.85, blaze=1.0, hot=EYE_HOT)
                else:
                    p.px((x, y + j), (210, 196, 150))
            p.px((x, y + lidh), (60, 40, 24))
            if t > 0.85:
                p.px((x + side, y - 2), (18, 10, 8))                    # the outer corner flicked up
    # the long nose's shadow, the nasolabial lines cut deep
    for j in range(V(19), V(28)):
        p.px((fx + int(cx) - U(1) - 1, fy + j), mixc(SKIN, SKIN_SHADE, 0.6))
    for j in range(V(27), V(33)):
        t = (j - V(27)) / max(1, V(33) - V(27))
        for side in (-1, 1):
            x = fx + int(cx) + side * (U(3) + int(t * U(1.5))) - (1 if side < 0 else 0)
            p.px((x, fy + j), SKIN_HOLLOW)
    # the mouth: thin, hard, turned down at its corners
    for i in range(-U(3), U(3)):
        t = abs(i + 0.5) / U(3)
        y = fy + V(32) + int(t * t * V(1.5))
        p.px((fx + int(cx) + i, y), (70, 30, 32))
        p.px((fx + int(cx) + i, y + 1), (130, 92, 76))


if __name__ == '__main__':
    ps = lord_parts()
    p = SilkPainter(88)
    slots = paint(ps, p, lord_painter())
    p.save('hundred_eyed_demon')
    emit(ps, slots, JAVA, 'lord')
    print('hundred-eyed demon', len(ps), 'parts', {a: round(b, 2) for a, b in extent(ps, 1.05).items()})
