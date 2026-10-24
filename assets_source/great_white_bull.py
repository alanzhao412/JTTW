"""The Great White Bull, the Bull Demon King's true form. As lion_camel.py: its model and skin come
from one list of parts (beastgen): the model is written into GreatWhiteBullRenderer.layer() between
its GENERATED markers, the skin (textures/entity/great_white_bull.png) painted to the same box-UV
layout at twice the model's resolution, and its eyes alone (great_white_bull_eyes.png), drawn
full-bright over the skin so they burn.

A vast wedge of a bull, all its weight in a towering chest and humped shoulders, tapering along a
long ribbed barrel to lean, hard haunches: scarred white hide shaded grey-blue in its hollows, every
muscle cut (the hump, the rolls of its neck, the plates of its chest, the shoulders, the thighs), a
shaggy mane along its neck and under its dewlap, tufts at its fetlocks and hocks. A demon king's
harness: an iron collar and a bronze breast-band studded in gold, a gold boss on its breast. Its
head is carried low, under a heavy brow with eyes that burn red; a long, dark, squared snout with
flared nostrils and a gold ring; horns ridged and tapering in five joints, black at the skull, bone
along, dark at the points, sweeping out, then forward and in; split hooves of dark horn; a long tail
ending in a heavy tassel.

The eyes are as they were drawn at one texture pixel to the model pixel: on the skull's front (a box
12 x 10 x 10), each eye two model pixels along its bottom, two tall at its outer side, one at its
inner, the brow's shadow cutting its inner corner, in a dark mask; a glint of red at the skull's sides.

Run from the repository root: python assets_source/great_white_bull.py"""
import math

import beastgen as bg
from beastgen import Painter, paint, emit

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S
S = bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/GreatWhiteBullRenderer.java'

WHITE = (226, 224, 218)      # steel-white hide
COOL = (142, 152, 174)       # grey-blue, in its hollows and toward its belly
DEEP = (92, 100, 122)
SHADOW = (150, 148, 150)
MASK = (118, 114, 116)       # the face's dark mask
CHARCOAL = (52, 48, 50)      # snout, stockings
KERATIN = (34, 30, 32)       # hooves
KERATIN_HI = (88, 80, 76)
HORN_ROOT = (34, 30, 32)
BONE = (196, 186, 160)
GOLD = (214, 168, 62)
GOLD_HI = (250, 214, 110)
GOLD_DEEP = (150, 104, 36)
BRONZE = (124, 82, 42)
BRONZE_DARK = (70, 44, 24)
IRON = (66, 68, 76)
SCAR = (236, 222, 222)
SCAR_DARK = (112, 104, 120)
VEIN = (150, 160, 186)
EMBER = (104, 14, 8)         # the eyes smoulder: a deep red, not a blaze
EMBER_CORE = (140, 34, 14)
SOCKET = (16, 12, 14)        # the socket's shadow under the brow
UNDER = (24, 20, 22)


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


def tuft(name, parent, at, rot, k=1.0, style='shag'):
    """A tuft of shaggy hair hanging from its pivot (+y), tapering in three steps."""
    w, d = 4.5 * k, 3 * k
    return part(name, parent, at, rot, [
        (-w / 2, 0, -d / 2, w, 3 * k, d, style),
        (-w * 0.35, 3 * k - 0.2, -d * 0.35, w * 0.7, 2.6 * k, d * 0.7, style),
        (-w * 0.18, 5.4 * k - 0.2, -d * 0.18, w * 0.36, 2 * k, d * 0.36, style),
    ])


def lock(name, parent, at, rot, k=1.0):
    """A lock of the mane standing up (-y) from its pivot, broad across x, tapering to a ragged point."""
    w, d = 5 * k, 2.4 * k
    return part(name, parent, at, rot, [
        (-w / 2, -3.5 * k, -d / 2, w, 3.5 * k, d, 'mane'),
        (-w * 0.32, -6.3 * k, -d * 0.4, w * 0.64, 3 * k, d * 0.8, 'mane'),
        (-w * 0.15, -8.5 * k, -d * 0.3, w * 0.3, 2.4 * k, d * 0.6, 'mane'),
    ])


def spike(name, parent, at, rot, k=1.0):
    """A bone spike of the hump's crest, leaning back."""
    return part(name, parent, at, rot, [
        (-1.25 * k, -3 * k, -1.25 * k, 2.5 * k, 3 * k, 2.5 * k, 'spike'),
        (-0.75 * k, -5.6 * k, -0.75 * k, 1.5 * k, 2.8 * k, 1.5 * k, 'spiketip'),
    ])


def horn():
    """The right horn: out from the skull, thick and black, then up and forward in four ridged,
    tapering joints, in toward the point (the left is mirrored)."""
    return [
        part('right_horn', 'head', (-6, -5, -6), (0, -0.2, 0.15), [
            (-10, -2.3, -2.3, 10, 4.6, 4.6, 'hornroot'),
            (-3.5, -2.7, -2.7, 2, 5.4, 5.4, 'gold'),          # a gold ring about it
        ]),
        part('right_horn_up', 'right_horn', (-9.2, 0, 0), (0.6, 0, -0.45), [(-1.9, -5, -1.9, 3.8, 5.4, 3.8, 'horn1')]),
        part('right_horn_mid', 'right_horn_up', (0, -4.8, 0), (0.3, 0, 0.25), [(-1.5, -4, -1.5, 3, 4.4, 3, 'horn2')]),
        part('right_horn_end', 'right_horn_mid', (0, -3.8, 0), (0.3, 0, 0.35), [(-1.1, -3.4, -1.1, 2.2, 3.8, 2.2, 'horn3')]),
        part('right_horn_tip', 'right_horn_end', (0, -3.2, 0), (0.25, 0, 0.35), [(-0.7, -3, -0.7, 1.4, 3.2, 1.4, 'horn4')]),
    ]


BULL = mirrored([
    # a wedge: a towering chest and shoulders, a long barrel drawn in at the belly, lean haunches;
    # the hump crested with bone spikes, the neck thick and thrust forward and down
    part('body', 'root', (0, 0, 0), (0, 0, 0), [
        (-10, -9, -18, 20, 19, 16, 'chest'),                 # chest and shoulders
        (-9, -7, -3, 18, 15, 10, 'ribs'),                    # the ribs, behind the shoulders
        (-8, -7, 7, 16, 12.5, 7, 'flank'),                   # the loins, the belly drawn up
        (-8.5, -8, 13, 17, 13, 8, 'haunch'),                 # the haunches
        (-6, -11, -6, 12, 5, 16, 'back'),                    # the back, stepping down from the hump
        (-7, -16, -17, 14, 8, 11, 'hump'),                   # the hump
        (-5, -18, -15, 10, 3, 7, 'hump'),                    # its crown
        (-7, -8, -24, 14, 13, 8, 'neck'),                    # the neck
        (-6, -12, -22, 12, 5, 7, 'neck'),                    # its crest, up to the hump
        (-3.5, 4, -25, 7, 7, 9, 'dewlap'),                   # the dewlap, heavy under the neck
        (-8.3, -9, 11.5, 3, 2, 4, 'hide'), (5.3, -9, 11.5, 3, 2, 4, 'hide'),   # the hip bones, sharp
        (-7.6, -12.6, -21.6, 15.2, 18.2, 2.4, 'collar'),     # an iron collar, studded in gold
        (-10.5, 0, -18.6, 21, 2.6, 15, 'strap'),             # a bronze breast-band
        (-9.6, -7.6, -2.2, 19.2, 16, 2.4, 'girth'),          # and its girth
        (-2.2, -1.2, -19.6, 4.4, 4.6, 1.4, 'boss'),          # a gold boss on the breast
    ]),
    # the muscles cut out of it: the plates of the chest, the shoulders, the rolls of the neck
    part('right_pec', 'body', (-0.4, -1, -18), (0.08, 0.22, 0), [(-9, -4, -1.6, 9, 13, 2, 'muscle')]),
    part('right_shoulder', 'body', (-10, -5, -11), (-0.12, 0, 0.1), [(-1.6, -7, -5.5, 2.2, 14, 10, 'muscle')]),
    part('right_shoulder_top', 'body', (-7.2, -14, -12), (0, 0, -0.55), [(-1.6, -1, -5, 3, 6, 9, 'muscle')]),
    part('right_neck_roll', 'body', (-7, -6, -20), (0.45, 0, 0.06), [(-1.4, -4, -3, 2, 10, 5, 'muscle')]),
    part('right_neck_roll2', 'body', (-6.7, -1, -23.5), (0.7, 0, 0.08), [(-1.3, -3, -2, 1.8, 8, 4, 'muscle')]),
    part('right_ribcage', 'body', (-9, -1, 2), (0, 0, 0.06), [(-0.8, -5, -4.5, 1.4, 10, 9, 'ribs')]),
    part('right_thigh_plate', 'body', (-8.5, -4, 17), (0.12, 0, 0.05), [(-1.2, -4, -4, 1.8, 11, 8, 'muscle')]),
    part('rump', 'body', (0, -8, 13), (-0.28, 0, 0), [(-7.5, 0, 0, 15, 4, 9.5, 'back')]),   # sloping down to the tail
    # bone spikes along the hump's crest, falling away
    spike('spike1', 'body', (0, -17.5, -14), (-0.45, 0, 0), 1.15),
    spike('spike2', 'body', (0, -17.5, -10.5), (-0.55, 0, 0), 1.0),
    spike('spike3', 'body', (0, -15.5, -7), (-0.65, 0, 0), 0.85),
    spike('spike4', 'body', (0, -10.5, -2), (-0.8, 0, 0), 0.7),
    spike('spike5', 'body', (0, -10.5, 3), (-0.95, 0, 0), 0.55),
    # the shaggy mane down its neck, and the hair under its dewlap
    lock('mane1', 'body', (0, -12, -21), (-0.6, 0, 0), 1.25),
    lock('right_mane1', 'body', (-4.5, -11.5, -21), (-0.75, 0, -0.55), 1.2),
    lock('right_mane2', 'body', (-6.8, -7, -22), (-1.0, 0, -1.2), 1.15),
    lock('right_mane3', 'body', (-7, -1, -23), (-1.25, 0, -1.6), 1.0),
    lock('right_mane4', 'body', (-6.6, -14, -16), (-0.9, 0, -0.75), 1.0),
    tuft('dewlap_tuft', 'body', (0, 10.5, -22), (0.25, 0, 0), 1.15),
    tuft('right_dewlap_tuft', 'body', (-2.6, 10, -19), (0.2, 0, 0.12), 0.9),
    tuft('chest_tuft', 'body', (0, 9.5, -15), (0.15, 0, 0), 0.9),
    # the head carried low, as a bull about to charge: a skull under a heavy brow, a long squared
    # snout and jaw, a ring through the nose
    part('head', 'body', (0, -3, -23), (0.25, 0, 0), [
        (-6, -6, -10, 12, 10, 10, 'skull'),
        (-7, -7, -11, 14, 3, 4, 'brow'),                     # overhanging the eyes
        (-4.5, -2, -17, 9, 7, 7, 'snout'),
        (-4, 5, -15, 8, 2, 5, 'jaw'),
        (-6.8, -1, -9, 1.6, 6, 7, 'cheek'), (5.2, -1, -9, 1.6, 6, 7, 'cheek'),   # the jaw muscles
        (-2.5, 5, -17.8, 5, 1, 1, 'ring'),                   # the nose ring
        (-2.5, 2, -17.8, 1, 3, 1, 'ring'), (1.5, 2, -17.8, 1, 3, 1, 'ring'),
    ]),
    tuft('forelock', 'head', (0, -5.5, -6), (-2.7, 0, 0), 1.1),   # the poll's shag, bristling up between the horns
    tuft('right_forelock', 'head', (-2.6, -5.5, -4), (-2.5, 0, 0.35), 0.85),
    tuft('beard', 'head', (0, 6.5, -12), (0.3, 0, 0), 0.75),
    # ears laid back under the horns
    part('right_ear', 'head', (-6, -3, -3), (0.3, 0.5, -0.4), [(-4.5, 0, 0, 4.5, 2.2, 1, 'ear')]),
] + horn() + [
    # thick forelegs under the weight, the knee and cannon below, split hooves
    part('right_front_leg', 'root', (-6, 10, -12), (0, 0, 0), [
        (-3.6, -4, -3.8, 7.2, 11, 7.6, 'foreleg'),
        (-3.3, 6.5, -3.4, 6.6, 1.5, 6.8, 'knee'),
    ]),
    part('right_front_shin', 'right_front_leg', (0, 7.5, 0), (0, 0, 0), [
        (-2.6, 0, -2.6, 5.2, 4.2, 5.2, 'cannon'),
        (-3.1, 2.4, -3.1, 6.2, 2.4, 6.4, 'fetlock'),
        (-2.9, 3.8, -3.4, 2.7, 2.7, 5.4, 'hoof'), (0.2, 3.8, -3.4, 2.7, 2.7, 5.4, 'hoof'),
        (-2, 2.8, 2.2, 1.4, 1.4, 1.2, 'hoof'), (0.6, 2.8, 2.2, 1.4, 1.4, 1.2, 'hoof'),   # dewclaws
    ]),
    # leaner hind legs: the thigh, angled down and back to the hock, the cannon, the hoof
    part('right_hind_leg', 'root', (-5, 6, 16), (0, 0, 0), [
        (-3.4, -5, -4.6, 6.8, 11, 9, 'thigh'),
    ]),
    part('right_hind_gaskin', 'right_hind_leg', (0, 5.5, 0.4), (0.4, 0, 0), [(-2.6, -0.5, -2.6, 5.2, 7.5, 5, 'foreleg')]),
    part('right_hind_shin', 'right_hind_gaskin', (0, 6.8, 0), (-0.4, 0, 0), [
        (-2.2, 0, -2.2, 4.4, 4.2, 4.4, 'cannon'),
        (-1.4, -1.2, 1.4, 2.8, 2.6, 1.6, 'hock'),           # the point of the hock
        (-2.7, 2.3, -2.8, 5.4, 2.2, 5.6, 'fetlock'),
        (-2.6, 3.64, -3.1, 2.5, 2.6, 5, 'hoof'), (0.1, 3.64, -3.1, 2.5, 2.6, 5, 'hoof'),
    ]),
    tuft('right_hock_tuft', 'right_hind_shin', (0, -0.4, 2.6), (0.5, 0, 0), 0.6),
    # a long tail, a heavy tassel at its end
    part('tail', 'body', (0, -6, 21.2), (0.35, 0, 0), [(-1.1, 0, -1.1, 2.2, 7, 2.2, 'tail')]),
    part('tail_mid', 'tail', (0, 6.6, 0), (0.08, 0, 0), [(-0.9, 0, -0.9, 1.8, 6.4, 1.8, 'tail')]),
    part('tail_tip', 'tail_mid', (0, 6, 0), (0.06, 0, 0), [
        (-0.8, 0, -0.8, 1.6, 3, 1.6, 'tail'),
        (-1.7, 2.2, -1.7, 3.4, 3, 3.4, 'tassel'),
        (-1.4, 4.8, -1.4, 2.8, 2.8, 2.8, 'tassel'),
        (-0.8, 7.2, -0.8, 1.6, 2, 1.6, 'tassel'),
    ]),
])


# ── painting ─────────────────────────────────────────────────────────────────

def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return [a[i] + (b[i] - a[i]) * t for i in range(3)]


def mul(c, k):
    return [v * k for v in c]


def get(p, x, y):
    return p.img.getpixel((int(x), int(y)))[:3]


def darken(p, x, y, k):
    p.px((x, y), mul(get(p, x, y), k))


SIDES = ('front', 'back', 'right', 'left')


def hide(p, f, base=WHITE, cool=0.0, strokes=1.0, light=1.0):
    """White hide in short hair: lit above, cooling to grey-blue down its sides and beneath; strokes
    of hair, paler and darker, running down and back."""
    for k, r in f.items():
        fx, fy, fw, fh = r
        lit = {'top': 1.05, 'bottom': 0.84, 'front': 1.0, 'back': 0.9}.get(k, 0.95) * light
        for j in range(fh):
            t = j / max(1, fh - 1)
            for i in range(fw):
                if k == 'top':
                    c = mix(base, COOL, 0.04 + cool * 0.5)
                elif k == 'bottom':
                    c = mix(base, DEEP, 0.55 + cool * 0.3)
                else:
                    c = mix(base, COOL, 0.04 + 0.85 * t ** 1.5 + cool)
                n = p.rnd.randint(-4, 4)
                p.px((fx + i, fy + j), [v * lit + n for v in c])
        for _ in range(max(1, fw * fh // 90)):                # mottled: soft shadows and lights over its form
            cx, cy, rad = fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh), p.rnd.uniform(2.5, 6)
            kk = p.rnd.choice((0.88, 0.92, 1.05))
            for j in range(int(cy - rad), int(cy + rad) + 1):
                for i in range(int(cx - rad), int(cx + rad) + 1):
                    d = math.hypot(i - cx, j - cy) / rad
                    if d < 1 and fx <= i < fx + fw and fy <= j < fy + fh:
                        darken(p, i, j, 1 + (kk - 1) * (1 - d))
        for _ in range(int(fw * fh * 0.11 * strokes)):         # the hair, in short strokes
            x, y = fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)
            light_stroke = p.rnd.random() < 0.45
            for d in range(p.rnd.randint(2, 4)):
                q = (x + (d // 2 if k in ('right', 'left') else 0), y + d)
                if fx <= q[0] < fx + fw and fy <= q[1] < fy + fh:
                    c = get(p, *q)
                    p.px(q, [min(255, v * 1.1 + 8) for v in c] if light_stroke else mix(c, DEEP, 0.3))


def contour(p, rect, top=0, bottom=0, left=0, right=0, k=0.72):
    """Darkens a face's edges by `k`, a band `n` texels deep along each: where a muscle meets the next."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            d = []
            if top and j < top:
                d.append(1 - j / top)
            if bottom and j >= fh - bottom:
                d.append((j - (fh - bottom) + 1) / bottom)
            if left and i < left:
                d.append(1 - i / left)
            if right and i >= fw - right:
                d.append((i - (fw - right) + 1) / right)
            if d:
                darken(p, fx + i, fy + j, 1 - (1 - k) * max(d))


def highlight(p, rect, rows, k=1.1):
    fx, fy, fw, fh = rect
    for j in range(min(rows, fh)):
        for i in range(fw):
            c = get(p, fx + i, fy + j)
            p.px((fx + i, fy + j), [min(255, v * (k - 0.03 * j)) for v in c])


def scars(p, rect, n):
    """Old battle scars: pale, raised slashes, a dark seam along their lower edge."""
    fx, fy, fw, fh = rect
    for _ in range(n):
        length = p.rnd.randint(min(fw, 6), max(7, min(fw, 14)))
        x, y = fx + p.rnd.randrange(max(1, fw - length // 2)), fy + p.rnd.randrange(max(1, fh - 4))
        slope = p.rnd.choice((0.35, 0.5, -0.4, 0.7))
        for t in range(length):
            qx, qy = x + t, int(y + t * slope)
            if fx <= qx < fx + fw and fy <= qy < fy + fh - 1:
                p.px((qx, qy), SCAR)
                p.px((qx, qy + 1), SCAR_DARK)
                if 1 < t < length - 2 and t % 3 == 1 and qy > fy:   # its stitch-marks
                    p.px((qx, qy - 1), SCAR_DARK)


def veins(p, rect, n):
    """Veins standing out under the hide: faint, branching, wandering down."""
    fx, fy, fw, fh = rect
    for _ in range(n):
        x, y = fx + p.rnd.randrange(fw), fy + p.rnd.randrange(max(1, fh // 3))
        for _ in range(fh):
            if fx <= x < fx + fw and fy <= y < fy + fh:
                p.px((x, y), mix(get(p, x, y), VEIN, 0.55))
            y += 1
            x += p.rnd.choice((-1, 0, 0, 1))
            if p.rnd.random() < 0.08:
                bx, by = x, y
                for _ in range(4):
                    bx += 1
                    by += 1
                    if fx <= bx < fx + fw and fy <= by < fy + fh:
                        p.px((bx, by), mix(get(p, bx, by), VEIN, 0.4))


def bulge(p, f, at, size, k=0.66, lift=1.1):
    """A muscle bulging under the hide of both flanks: an oval at `at` (its fraction back from the
    face's front edge and down it) of `size` (fractions across and down), lit along its crown and
    shadowed round its lower and rear rim."""
    for side in ('right', 'left'):
        fx, fy, fw, fh = f[side]
        back, down = at
        cx = fw * (1 - back) if side == 'right' else fw * back    # the right flank's front is at its far edge
        cy = fh * down
        rx, ry = fw * size[0] / 2, fh * size[1] / 2
        for j in range(fh):
            for i in range(fw):
                dx, dy = (i + 0.5 - cx) / rx, (j + 0.5 - cy) / ry
                d = math.hypot(dx, dy)
                if d >= 1.15:
                    continue
                if d > 0.82:                                       # its rim, darkest below and behind
                    rim = (d - 0.82) / 0.33
                    below = max(0.0, dy) * 0.8 + 0.2
                    darken(p, fx + i, fy + j, 1 - (1 - k) * (1 - abs(rim - 0.5) * 2) * below)
                elif dy < -0.25:                                   # its crown, catching the light
                    darken(p, fx + i, fy + j, 1 + (lift - 1) * (1 - d))


def ribs(p, rect, every=6):
    """The ribs showing through a flank: shadowed hollows curving down and back, lit between."""
    fx, fy, fw, fh = rect
    for x0 in range(every // 2, fw + every, every):
        for j in range(int(fh * 0.22), int(fh * 0.86)):
            t = j / fh
            x = x0 + int(3 * math.sin(t * 2.4)) - 2
            for dx, k in ((0, 0.74), (1, 0.84), (-1, 1.08)):
                if 0 <= x + dx < fw:
                    darken(p, fx + x + dx, fy + j, k + (1 - k) * (0.6 * abs(t - 0.55)))


def shag(p, f, base, root, tip, ragged=0.5):
    """Shaggy hair in strands: dark at the roots (the top of its sides), pale at their ends, which
    end raggedly (the rest left clear); its top and bottom ends a mass of hair."""
    for k, r in f.items():
        fx, fy, fw, fh = r
        if k in ('top', 'bottom'):
            p.face(r, mix(base, root, 0.4) if k == 'top' else mix(base, tip, 0.3), 1.0, noise=8)
            continue
        lit = {'front': 1.0, 'back': 0.88}.get(k, 0.94)
        i = 0
        while i < fw:
            width = p.rnd.randint(1, 3)
            tone = p.rnd.uniform(0.84, 1.12)
            end = fh - int(fh * ragged * p.rnd.random() ** 1.5)
            for di in range(width):
                if i + di >= fw:
                    break
                for j in range(end):
                    s = j / max(1, fh - 1)
                    c = mix(root, base, min(1, s * 2.2))
                    if s > 0.6:
                        c = mix(c, tip, (s - 0.6) / 0.4 * 0.7)
                    kk = lit * tone * (0.76 if di == width - 1 else 1.0)
                    p.px((fx + i + di, fy + j), [v * kk + p.rnd.randint(-5, 5) for v in c])
            i += width


def plain(p, f, c, noise=4):
    for k, r in f.items():
        p.face(r, c, {'top': 1.1, 'bottom': 0.66, 'front': 1.0, 'back': 0.86}.get(k, 0.92), noise=noise, fall=0.15)


def gold(p, f, base=GOLD):
    plain(p, f, base, 3)
    for k in SIDES:
        fx, fy, fw, fh = f[k]
        for i in range(fw):
            p.px((fx + i, fy), GOLD_HI)
            p.px((fx + i, fy + fh - 1), GOLD_DEEP)


def studs(p, rect, every, row, colour=GOLD):
    """A row of round studs across a face, `row` texels down: each lit above, shadowed below."""
    fx, fy, fw, fh = rect
    for i in range(every // 2, fw - 1, every):
        p.px((fx + i, fy + row), GOLD_HI)
        p.px((fx + i + 1, fy + row), colour)
        p.px((fx + i, fy + row + 1), colour)
        p.px((fx + i + 1, fy + row + 1), GOLD_DEEP)


def horn_face(p, f, root, tip, ridges=True, along=False):
    """A horn's joint: from `root` at its base to `tip` at its end, ringed with ridges. Along x (the
    root, `along`), or up its faces (-y: the base at the bottom)."""
    for k, r in f.items():
        fx, fy, fw, fh = r
        lit = {'top': 1.12, 'bottom': 0.7, 'front': 1.0, 'back': 0.84}.get(k, 0.94)
        for j in range(fh):
            for i in range(fw):
                if along:
                    t = 1 - i / max(1, fw - 1) if k in ('front', 'top', 'bottom') else i / max(1, fw - 1)
                    if k in ('right', 'left'):
                        t = 1.0 if k == 'right' else 0.0
                    ridge = ridges and k not in ('right', 'left') and (i % 4 == 0)
                else:
                    t = 1 - j / max(1, fh - 1)
                    if k in ('top', 'bottom'):
                        t = 1.0 if k == 'top' else 0.0
                    ridge = ridges and k not in ('top', 'bottom') and ((fh - 1 - j) % 4 == 0)
                c = mix(root, tip, t)
                sheen = 1.12 if (k in ('front', 'top') and i == fw // 3) else 1.0
                kk = lit * sheen * (0.7 if ridge else 1.0)
                p.px((fx + i, fy + j), [v * kk + p.rnd.randint(-3, 3) for v in c])


def hoof(p, f):
    """A half of a cloven hoof: dark horn, faintly ringed with growth and striped down, the coronet
    dark at its top, the sole beneath."""
    for k, r in f.items():
        fx, fy, fw, fh = r
        if k in ('top', 'bottom'):
            p.face(r, mul(KERATIN, 0.8), 1.0, noise=3)
            continue
        lit = {'front': 1.0, 'back': 0.8}.get(k, 0.9)
        for j in range(fh):
            t = j / max(1, fh - 1)
            for i in range(fw):
                c = mix(KERATIN_HI, KERATIN, t * 0.8)
                if j % 3 == 1:
                    c = mul(c, 1.18)                          # its growth rings
                if i % 3 == 0:
                    c = mul(c, 0.86)
                if j == 0:
                    c = (24, 22, 26)                          # the coronet
                p.px((fx + i, fy + j), [v * lit + p.rnd.randint(-3, 3) for v in c])


def cell(p, rect, i, j, c):
    """One model pixel (S x S texture pixels) of a face: `i` across, `j` down."""
    for a in range(S):
        for b in range(S):
            p.px((rect[0] + i * S + a, rect[1] + j * S + b), c)


def ember_cell(p, rect, i, j, c):
    """A model pixel of a burning eye: the skin dark beneath, the glow drawn over it."""
    for a in range(S):
        for b in range(S):
            x, y = rect[0] + i * S + a, rect[1] + j * S + b
            p.px((x, y), [v * 0.35 for v in c])
            p.glow.putpixel((x, y), tuple(c) + (255,))


def paint_skull(p, f):
    hide(p, f, cool=0.04)
    rect = f['front']
    fw, fh = rect[2] // S, rect[3] // S
    # a dark mask about the eyes, the eyes deep under the brow, burning
    for j in range(fh):
        for i in range(fw):
            edge = min(i, fw - 1 - i)
            if j <= 5 and (edge <= 3 or j <= 1):
                cell(p, rect, i, j, mul(MASK, 0.85 + 0.03 * p.rnd.randint(-1, 1)))
    for ex in (1, fw - 3):
        inner = 0 if ex > 5 else 1                     # the column nearer the nose
        for i in range(2):
            cell(p, rect, ex + i, 1, SOCKET)          # the socket's shadow under the brow
            # angled, glaring: two along the bottom, two tall at the outside, one at the inside, so
            # the eye's top slopes down toward the nose
            for j in ((3,) if i == inner else (2, 3)):
                ember_cell(p, rect, ex + i, j, EMBER_CORE if (i == inner and j == 3) else EMBER)
            if i == inner:
                cell(p, rect, ex + i, 2, SOCKET)      # the brow's shadow, cutting the eye's inner corner
            cell(p, rect, ex + i, 4, UNDER)
    # the eyes show at the skull's sides too, a glint of red under the brow
    for side, col in (('right', f['right'][2] // S - 2), ('left', 0)):
        for i in range(2):
            ember_cell(p, f[side], col + i, 2, EMBER)
    # the forehead between the eyes: its hair whorled, a scar across it
    fx, fy, fw_, fh_ = rect
    scars(p, (fx + 4 * S, fy + 6 * S, 4 * S, 3 * S), 1)


def paint_bull(p, style, f, dims):
    if style == 'chest':
        hide(p, f)
        bulge(p, f, (0.42, 0.42), (0.62, 0.62))                     # the shoulder
        bulge(p, f, (0.3, 0.8), (0.42, 0.34))                       # the forearm's root, the elbow
        for k in ('right', 'left'):
            scars(p, f[k], 3)
            veins(p, f[k], 2)
            contour(p, f[k], bottom=6, k=0.8)
        fx, fy, fw, fh = f['front']                           # the breastbone's furrow down its middle
        for j in range(fh):
            for d in (-1, 0):
                darken(p, fx + fw // 2 + d, fy + j, 0.84)
        scars(p, f['front'], 2)
    elif style == 'ribs':
        hide(p, f, cool=0.03)
        for k in ('right', 'left'):
            ribs(p, f[k])
            scars(p, f[k], 1)
    elif style == 'flank':
        hide(p, f, cool=0.06)
        for k in ('right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):                               # the flank's hollow before the thigh
                for j in range(fh // 3, fh):
                    darken(p, fx + i, fy + j, 0.9 - 0.06 * (j / fh))
            ribs(p, (fx, fy, fw // 2, fh), every=6)
    elif style == 'haunch':
        hide(p, f)
        bulge(p, f, (0.55, 0.45), (0.8, 0.75))                      # the round of the haunch
        for k in ('right', 'left', 'back'):
            scars(p, f[k], 1 if k == 'back' else 2)
        fx, fy, fw, fh = f['back']                            # the cleft between the buttocks, under the tail
        for j in range(fh // 4, fh):
            for d in (-1, 0):
                darken(p, fx + fw // 2 + d, fy + j, 0.7)
    elif style in ('back', 'hide'):
        hide(p, f)
        if 'top' in f:                                       # a dark ridge down the spine
            fx, fy, fw, fh = f['top']
            for j in range(fh):
                for d in (-1, 0):
                    darken(p, fx + fw // 2 + d, fy + j, 0.82)
    elif style == 'hump':
        hide(p, f)
        for k in ('right', 'left'):
            scars(p, f[k], 1)
            highlight(p, f[k], 3)
            contour(p, f[k], bottom=4, k=0.78)
        fx, fy, fw, fh = f['top']
        for j in range(fh):
            for d in range(-2, 2):
                darken(p, fx + fw // 2 + d, fy + j, 0.82)
    elif style == 'neck':
        hide(p, f, cool=0.04)
        bulge(p, f, (0.5, 0.35), (0.85, 0.5), k=0.72)
        for k in ('right', 'left'):
            fx, fy, fw, fh = f[k]
            for x0 in range(3, fw, 5):                       # the folds of the neck
                for j in range(fh):
                    x = x0 + int(1.5 * math.sin(j * 0.3))
                    if 0 <= x < fw:
                        darken(p, fx + x, fy + j, 0.8)
            veins(p, f[k], 2)
            scars(p, f[k], 1)
    elif style == 'dewlap':
        hide(p, f, cool=0.12)
        for k in SIDES:
            fx, fy, fw, fh = f[k]
            for x0 in range(2, fw, 4):                       # hanging in folds
                for j in range(fh):
                    darken(p, fx + x0, fy + j, 0.82)
    elif style == 'muscle':
        hide(p, f, light=1.02)
        for k in SIDES:
            fx, fy, fw, fh = f[k]
            highlight(p, f[k], 3, 1.1)
            contour(p, f[k], bottom=5, left=3, right=3, k=0.6)
        veins(p, f['right'], 1)
        veins(p, f['left'], 1)
    elif style == 'mane':
        shag(p, f, (214, 212, 208), (78, 84, 104), (248, 246, 240), ragged=0.0)
        for k in SIDES:                                       # the lock's tip, ragged
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for j in range(p.rnd.randint(0, 2)):
                    p.img.putpixel((fx + i, fy + j), (0, 0, 0, 0))
    elif style == 'shag':
        shag(p, f, (204, 202, 200), (84, 90, 110), (242, 240, 234), ragged=0.5)
    elif style == 'spike':
        for k, r in f.items():
            p.gradient(r, mix(HORN_ROOT, BONE, 0.5), HORN_ROOT, noise=4)
    elif style == 'spiketip':
        for k, r in f.items():
            p.gradient(r, (226, 216, 190), mix(HORN_ROOT, BONE, 0.5), noise=4)
    elif style == 'skull':
        paint_skull(p, f)
    elif style == 'brow':
        for k, r in f.items():
            p.face(r, MASK, {'top': 1.06, 'bottom': 0.6, 'back': 0.9}.get(k, 0.86), noise=4, fall=0.25)
        # its front: darkest at its underside, a V that scowls toward the nose
        rect = f['front']
        bw, bh = rect[2] // S, rect[3] // S
        for i in range(bw):
            dip = 1 if abs(i - (bw - 1) / 2) < 3 else 0
            for j in range(bh):
                cell(p, rect, i, j, mul(MASK, 0.55 if j >= bh - 1 - dip else 0.8))
        for k in ('top',):                                    # its hair bristling over it
            fx, fy, fw, fh = f[k]
            for i in range(0, fw, 2):
                p.px((fx + i, fy + fh - 1), mul(MASK, 0.7))
    elif style == 'snout':
        for k, r in f.items():
            p.face(r, CHARCOAL, {'top': 1.25, 'bottom': 0.6, 'back': 0.9}.get(k, 0.86), noise=5, fall=0.1)
        rect = f['front']
        sw, sh = rect[2] // S, rect[3] // S
        fx, fy, fw, fh = rect
        for j in range(fh):                                   # the muzzle's wet sheen, its pores
            for i in range(fw):
                if p.rnd.random() < 0.12:
                    p.px((fx + i, fy + j), mul(CHARCOAL, 1.35))
        for nx in (1, sw - 3):                                # flared nostrils, red-dark within
            for i in range(2):
                cell(p, rect, nx + i, 1, (14, 10, 12))
                cell(p, rect, nx + i, 2, (64, 18, 16))
            for i in range(-1, 3):                            # their flared rims
                p.px((fx + (nx + i) * S + 1, fy + S - 1), mul(CHARCOAL, 1.5))
        for i in range(sw):                                   # a lighter muzzle line where the snout meets the jaw
            cell(p, rect, i, sh - 1, mul(CHARCOAL, 1.4))
        fx, fy, fw, fh = f['top']                             # the bridge of the nose, wrinkled across
        for j in range(1, fh - 1, 3):
            for i in range(1, fw - 1):
                darken(p, fx + i, fy + j, 0.7)
        for k in ('right', 'left'):                           # wrinkles back along its sides
            fx, fy, fw, fh = f[k]
            for j in range(2, fh - 2, 3):
                for i in range(fw):
                    if (i + j) % 5:
                        darken(p, fx + i, fy + j, 0.78)
    elif style == 'jaw':
        for k, r in f.items():
            p.face(r, CHARCOAL, {'top': 1.1, 'bottom': 0.7}.get(k, 0.9), noise=4)
    elif style == 'cheek':
        hide(p, f, cool=0.1)
        for k in ('right', 'left'):
            contour(p, f[k], bottom=3, right=2, left=2, k=0.72)
    elif style == 'ring':
        gold(p, f)
    elif style == 'ear':
        hide(p, f, cool=0.06)
        fx, fy, fw, fh = f['front']
        for i in range(1, fw - 1):
            for j in range(1, fh - 1):
                p.px((fx + i, fy + j), (150, 126, 128))      # pinkish-grey within
    elif style == 'hornroot':
        horn_face(p, f, (20, 18, 20), mix(HORN_ROOT, BONE, 0.3), along=True)
    elif style == 'horn1':
        horn_face(p, f, mix(HORN_ROOT, BONE, 0.3), mix(HORN_ROOT, BONE, 0.75))
    elif style == 'horn2':
        horn_face(p, f, mix(HORN_ROOT, BONE, 0.75), BONE)
    elif style == 'horn3':
        horn_face(p, f, BONE, mix(BONE, HORN_ROOT, 0.55))
    elif style == 'horn4':
        horn_face(p, f, mix(BONE, HORN_ROOT, 0.55), (16, 14, 16), ridges=False)
    elif style == 'gold':
        gold(p, f)
        for k in SIDES:
            fx, fy, fw, fh = f[k]
            for j in range(2, fh - 2, 3):
                p.px((fx + fw // 2, fy + j), GOLD_DEEP)
    elif style == 'collar':
        for k, r in f.items():
            p.face(r, IRON, {'top': 1.15, 'bottom': 0.6, 'front': 1.0}.get(k, 0.9), noise=5, fall=0.2)
        for k in SIDES:
            fx, fy, fw, fh = f[k]
            if fw < fh:                                       # the sides: down them, along their length
                for j in range(0, fh, 6):
                    for i in range(fw):
                        darken(p, fx + i, fy + j, 0.7)
                for j in range(3, fh - 1, 6):
                    p.px((fx + fw // 2 - 1, fy + j), GOLD_HI)
                    p.px((fx + fw // 2, fy + j), GOLD)
                    p.px((fx + fw // 2 - 1, fy + j + 1), GOLD)
                    p.px((fx + fw // 2, fy + j + 1), GOLD_DEEP)
            else:
                studs(p, (fx, fy, fw, fh), 6, 1)
        for k in ('front', 'back'):
            fx, fy, fw, fh = f[k]
            for j in range(fh):
                p.px((fx, fy + j), mul(IRON, 1.4))
                p.px((fx + fw - 1, fy + j), mul(IRON, 0.6))
    elif style in ('strap', 'girth'):
        for k, r in f.items():
            p.face(r, BRONZE, {'top': 1.12, 'bottom': 0.6, 'front': 1.0}.get(k, 0.88), noise=5, fall=0.2)
        for k in SIDES:
            fx, fy, fw, fh = f[k]
            if fh <= 6:                                       # a band: edged, studs along it
                for i in range(fw):
                    p.px((fx + i, fy), mul(BRONZE, 1.3))
                    p.px((fx + i, fy + fh - 1), BRONZE_DARK)
                studs(p, f[k], 6, fh // 2 - 1)
            else:                                             # the girth's sides, down them
                for j in range(fh):
                    p.px((fx, fy + j), mul(BRONZE, 1.3))
                    p.px((fx + fw - 1, fy + j), BRONZE_DARK)
                for j in range(3, fh - 1, 6):
                    p.px((fx + fw // 2 - 1, fy + j), GOLD_HI)
                    p.px((fx + fw // 2, fy + j), GOLD)
                    p.px((fx + fw // 2 - 1, fy + j + 1), GOLD)
                    p.px((fx + fw // 2, fy + j + 1), GOLD_DEEP)
    elif style == 'boss':
        gold(p, f)
        fx, fy, fw, fh = f['front']                           # a demon's face in the gold: brows, eyes, fangs
        for i in range(fw):
            for j in range(fh):
                dx, dy = (i + 0.5 - fw / 2) / (fw / 2), (j + 0.5 - fh / 2) / (fh / 2)
                if dx * dx + dy * dy > 1.0:
                    p.px((fx + i, fy + j), GOLD_DEEP)
        for i in (fw // 2 - 3, fw // 2 + 2):
            p.px((fx + i, fy + 3), (110, 12, 14))
        for i in range(fw // 2 - 3, fw // 2 + 3):
            p.px((fx + i, fy + 2), GOLD_DEEP)
            p.px((fx + i, fy + fh - 3), GOLD_DEEP)
        for i in (fw // 2 - 2, fw // 2 + 1):
            p.px((fx + i, fy + fh - 2), GOLD_HI)
    elif style == 'foreleg':
        hide(p, f)
        bulge(p, f, (0.5, 0.3), (0.9, 0.5), k=0.75)
        for k in ('right', 'left', 'front', 'back'):
            veins(p, f[k], 2)
            highlight(p, f[k], 3, 1.06)
            contour(p, f[k], left=1, right=1, k=0.82)
        scars(p, f['right'], 1)
        scars(p, f['left'], 1)
    elif style == 'thigh':
        hide(p, f)
        bulge(p, f, (0.45, 0.4), (0.9, 0.7))
        for k in ('right', 'left'):
            fx, fy, fw, fh = f[k]                             # the thigh's muscles parted down it
            for j in range(fh):
                x = fw // 2 + int(2 * math.sin(j / fh * 3))
                darken(p, fx + x, fy + j, 0.74)
                darken(p, fx + x + 1, fy + j, 0.86)
            veins(p, f[k], 2)
            scars(p, f[k], 1)
            contour(p, f[k], bottom=4, k=0.8)
    elif style == 'knee':
        hide(p, f, cool=0.2)
        for k in SIDES:
            contour(p, f[k], top=1, bottom=1, k=0.7)
    elif style == 'cannon':
        for k, r in f.items():                                # white into dark stockings
            fx, fy, fw, fh = r
            lit = {'top': 1.05, 'bottom': 0.6, 'front': 1.0, 'back': 0.84}.get(k, 0.92)
            for j in range(fh):
                t = j / max(1, fh - 1)
                for i in range(fw):
                    c = mix(mix(WHITE, COOL, 0.45), CHARCOAL, 0.25 + t * 0.7)
                    if k in SIDES and i % 4 == 1 and p.rnd.random() < 0.5:
                        c = mul(c, 0.86)                     # the sinews down it
                    p.px((fx + i, fy + j), [v * lit + p.rnd.randint(-4, 4) for v in c])
    elif style == 'hock':
        for k, r in f.items():
            p.face(r, mix(COOL, CHARCOAL, 0.4), {'top': 1.1}.get(k, 0.9), noise=5)
    elif style == 'fetlock':
        shag(p, f, (88, 86, 92), (40, 38, 42), (150, 150, 156), ragged=0.35)
    elif style == 'hoof':
        hoof(p, f)
    elif style == 'tail':
        hide(p, f, cool=0.12)
        for k in SIDES:
            fx, fy, fw, fh = f[k]
            for j in range(0, fh, 4):
                for i in range(fw):
                    darken(p, fx + i, fy + j, 0.86)
    elif style == 'tassel':
        shag(p, f, (70, 66, 70), (30, 28, 32), (132, 130, 136), ragged=0.3)
    else:
        raise ValueError(style)


def lowest(parts, names):
    """How low (model y, the ground at 24) the boxes of the parts named reach, at rest."""
    def rot(rx, ry, rz):
        cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
        x = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
        y = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
        z = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
        return mul3(mul3(z, y), x)

    def mul3(a, b):
        return [[sum(a[i][t] * b[t][j] for t in range(3)) for j in range(3)] for i in range(3)]

    def apply(m, v):
        return [sum(m[i][t] * v[t] for t in range(3)) for i in range(3)]

    frames, low = {}, -1e9
    ident = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    for name, parent, pivot, r, boxes, _ in parts:
        pm, pt = frames.get(parent, (ident, [0, 0, 0]))
        m = mul3(pm, rot(*r))
        t = [a + b for a, b in zip(pt, apply(pm, pivot))]
        frames[name] = (m, t)
        if name in names:
            for b in boxes:
                for c in ((b[0], b[1] + b[4], b[2]), (b[0], b[1] + b[4], b[2] + b[5])):
                    low = max(low, t[1] + apply(m, c)[1])
    return low


if __name__ == '__main__':
    p = Painter(7)
    slots = paint(BULL, p, paint_bull)
    p.img.save('src/main/resources/assets/jttw_engine/textures/entity/great_white_bull.png')
    p.glow.save('src/main/resources/assets/jttw_engine/textures/entity/great_white_bull_eyes.png')
    emit(BULL, slots, JAVA)
    print(len(BULL), 'parts,', len(slots), 'boxes')
    print('front hooves reach', round(lowest(BULL, ('right_front_shin',)), 2),
          '; hind hooves', round(lowest(BULL, ('right_hind_shin',)), 2), '(the ground at 24)')
