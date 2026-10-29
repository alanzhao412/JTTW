"""The Crab General (蟹将), the officer over the Dragon King's host. As shrimp_soldier.py: its model and
skin come from one list of parts (beastgen): the model is written into CrabGeneralRenderer's layer()
between its GENERATED markers, the skin painted to the same box-UV layout at twice the model's
resolution, with a second skin for the glint of its eyes and the jewel of its helmet drawn full-bright
(crab_general_glow.png).

A massive crab carrying a general's regalia. Its carapace is a broad dome built up in tiers, its
outline a crab's: a straight brow between the eyes, the front-side margins toothed with five spines
a side (the last the longest), then curving in to a narrow rear. Its shell is sculpted: the gastric
and cardiac swellings ringed by their grooves, tubercles and granules over it, a rich red-orange at
its crown deepening to crimson at its rim, flecked with cream spots and dark freckles. Its eyes stand
on stalks from their orbits at the brow's corners; under the brow, its antennae and the plates of its
mouthparts. Four pairs of jointed walking legs, each segment spined, ending in dark spiky tips; two
huge claws, the right a crusher larger than the left, their palms knobbed, their fingers serrated
with blunt teeth and tipped black.

Its regalia, the army's teal and gold made a general's: a gilded general's helmet on its crown,
embossed with waves and clouds, a flame-shaped frontlet with a red jewel, gold fin-wings and red
pompoms, a tall red plume, and two long pheasant-tail plumes (翎子) arching back over it; a saddle of
teal lamellar plates over the rear of its shell, edged in gold, a gold mirror at its middle; teal and
gold plates over its claws' arms and a gold ring at each wrist; a red cape, gold-bordered with a wave
at its hem, falling from the saddle behind; and four back flags (靠旗) rising from a socket behind the
saddle, red fields in toothed teal-and-gold borders, a gold wave across each, on red staffs with gold
spear-tips and red tassels.

Run from the repository root: python assets_source/crab_general.py"""
import math

import beastgen as bg
import wild_paint as w
from wild_paint import part, mirrored, aim, paint, emit, vnoise, clamp
from king_yama import ragged, circle, mixc, k, noisy
import shrimp_soldier as shrimp
from shrimp_soldier import LIGHTS, SIDES, lacquer, gold_box, rim, TEAL, TEAL_DEEP, TEAL_LIGHT, GOLD, GOLD_DEEP, RED, RED_DEEP, SEA
import fish_soldier as fish
from fish_soldier import armour_scales, clear_face

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/CrabGeneralRenderer.java'

# How much larger than its pixels it is drawn: CrabGeneralRenderer.SIZE
SIZE = 1.0

SHELL = (236, 80, 38)           # its shell: red-orange,
SHELL_HI = (255, 150, 72)       # lit to orange at its swellings,
CRIMSON = (166, 26, 32)         # deepening to crimson
CRIMSON_DEEP = (78, 10, 18)
CREAM = (252, 222, 168)         # its spots
UNDER = (240, 210, 170)         # its underside
UNDER_DEEP = (196, 150, 112)
TIP = (30, 18, 20)              # the black tips of its claws
TOOTH = (250, 236, 206)
GILT_HI = (255, 230, 112)       # the helmet's gilding: bright,
GILT = (250, 176, 30)
GILT_DEEP = (168, 92, 8)
PHEASANT = (226, 150, 66)       # the pheasant plumes: barred tawny,
PHEASANT_DARK = (74, 40, 22)    # dark bars,
PHEASANT_PALE = (246, 228, 190)  # pale between


class Painter(w.Painter):
    def pxa(self, xy, c, a):
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            self.img.putpixel((x, y), tuple(max(0, min(255, int(v))) for v in c[:3]) + (int(a),))


# ── The carapace ─────────────────────────────────────────────────────────────
# Its outline, seen from above: half its breadth at each z (-11 its brow .. 11 its rear)
OUTLINE = ((-11, 8.6), (-10, 9.6), (-8, 12.2), (-6, 14.0), (-4, 15.2), (-2, 15.6), (0, 15.1), (2, 13.9), (4, 12.3),
           (6, 10.5), (8, 8.7), (10, 7.1), (11, 6.4))
ZC = -1.0                       # the dome's crown: a little forward of its middle
# its tiers, from the rim up: (top y, thickness, scale)
TIERS = ((14.8, 2.2, 1.0), (13.2, 1.6, 0.93), (11.8, 1.4, 0.8), (10.7, 1.1, 0.62), (9.9, 0.8, 0.4))
CARA = {}                       # each tier box's style: (x, y, z, w, h, d, tier)


def half(z):
    """Half the carapace's breadth at z."""
    z = max(-11, min(11, z))
    for (z0, h0), (z1, h1) in zip(OUTLINE, OUTLINE[1:]):
        if z0 <= z <= z1:
            t = (z - z0) / (z1 - z0)
            return h0 + (h1 - h0) * t
    return OUTLINE[-1][1]


def carapace():
    boxes = []
    for n, (y, h, s) in enumerate(TIERS):
        z0, z1 = ZC + (-11 - ZC) * s, ZC + (11 - ZC) * s
        bands = max(3, round(11 * s))
        d = (z1 - z0) / bands
        for b in range(bands):
            zm = z0 + d * (b + 0.5)
            hw = s * half(ZC + (zm - ZC) / s) if s < 1 else half(zm)
            if n == 0:                          # the rim: as broad as the band's broadest
                hw = max(half(z0 + d * b), half(z0 + d * (b + 1)), hw)
            style = 'cara_%d_%d' % (n, b)
            box = (round(-hw, 2), y, round(z0 + d * b, 2), round(2 * hw, 2), h, round(d, 2), style)
            CARA[style] = box[:6] + (n,)
            boxes.append(box)
    return boxes


def top_at(x, z):
    """The carapace's top y over (x, z)."""
    for (bx, by, bz, bw, bh, bd, n) in sorted(CARA.values(), key=lambda b: b[1]):
        if bx <= x <= bx + bw and bz <= z <= bz + bd:
            return by
    return TIERS[0][0]


def tubercles():
    """Tubercles over the shell: on the gastric and cardiac swellings, a row over each branchial region."""
    spots = [(0, -5.5, 1.4), (0, -2.8, 1.1), (0, 4.2, 1.3), (0, 6.6, 1.0)]
    for x, z, s in ((2.4, -6.4, 1.2), (3.6, -3.6, 1.0), (1.8, 1.0, 0.9), (2.2, 5.4, 1.0),
                    (6.8, -5.0, 1.2), (9.0, -3.0, 1.1), (11.2, -1.0, 1.0), (7.8, -1.2, 0.9), (10.4, 2.2, 1.0),
                    (7.6, 3.0, 1.1), (5.6, 6.2, 0.9), (12.6, -3.4, 0.9), (5.2, -8.4, 1.0), (8.6, 5.8, 0.9)):
        spots += [(x, z, s), (-x, z, s)]
    out = []
    for x, z, s in spots:
        y = top_at(x, z)
        out.append((x - s / 2, y - s * 0.55, z - s / 2, s, s * 0.7, s, 'tubercle'))
    return out


def teeth():
    """The five spines of each front-side margin, the last the longest, and the little teeth of its brow."""
    out = []
    for i, z in enumerate((-9.6, -7.6, -5.6, -3.7, -1.7)):
        x = -half(z) + 0.4
        L = 2.0 if i < 4 else 4.2
        yaw = -0.55 + 0.1 * i
        out.append(part('right_tooth_%d' % i, 'body', (x, 15.6, z), (0, yaw, 0.12 if i < 4 else 0.22), [
            (-L * 0.55, -0.75, -0.8, L * 0.55, 1.5, 1.6, 'spinebase'),
            (-L, -0.45, -0.45, L * 0.5, 0.9, 0.9, 'spinetip')]))
    for i, x in enumerate((-1.2, -3.6)):
        out.append(part('right_brow_tooth_%d' % i, 'body', (x, 15.6, -11.0), (0, 0.15 * i, 0), [
            (-0.7, -0.6, -1.4, 1.4, 1.3, 1.6, 'browtooth')]))
    return out


def face():
    return [
        # under the brow: the epistome, the mouth's frame; the two plates of its mouthparts over the mouth
        part('right_eyestalk', 'body', (-6.6, 15.0, -10.4), (-0.3, 0.2, 0.3), [
            (-0.85, -6.2, -0.85, 1.7, 6.4, 1.7, 'eyestalk'),
            (-1.5, -0.6, -1.5, 3.0, 1.4, 3.0, 'orbit')]),
        part('right_eye', 'right_eyestalk', (0, -6.0, 0), (0, 0, 0), [(-1.6, -3.0, -1.6, 3.2, 3.2, 3.2, 'eye')]),
        part('right_maxilliped', 'body', (-1.5, 17.0, -10.6), (-0.15, 0, 0.06), [
            (-1.3, 0, -0.5, 2.6, 4.2, 0.9, 'maxilliped'),
            (-0.9, 3.8, -0.4, 1.8, 1.4, 0.7, 'palp')]),
        part('right_antenna', 'body', (-2.6, 15.8, -10.8), (-0.9, 0.5, 0), [(-0.25, -0.25, -4.0, 0.5, 0.5, 4.0, 'antenna')]),
        part('right_antennule', 'body', (-1.0, 15.6, -11.0), (-1.2, 0.2, 0), [(-0.35, -0.35, -1.8, 0.7, 0.7, 1.8, 'antennule')]),
    ]


# ── Legs and claws ───────────────────────────────────────────────────────────
# the walking legs: (z of its root, its spread back (+) or forward (-), its merus's length)
LEGS = ((-4.4, -0.5, 8.6), (-0.8, -0.12, 9.2), (2.8, 0.3, 8.8), (6.2, 0.72, 7.8))
LEG_REST = (0.75, -1.0, -0.95, 1.4)                        # merus up, carpus down, propodus down, dactyl to the floor


def legs():
    out = []
    for n, (z, yaw, L) in enumerate(LEGS):
        name = 'right_leg_%d' % n
        x = -half(z) + 3.2
        out += [
            part(name, 'body', (x, 16.4, z), (0, yaw, LEG_REST[0]), [
                (-L, -1.35, -1.15, L, 2.7, 2.3, 'merus'),
                (-L * 0.42, -2.2, -0.35, 0.7, 1.1, 0.7, 'legspine'),
                (-L * 0.78, -2.2, -0.35, 0.7, 1.1, 0.7, 'legspine')]),
            part(name + '_carpus', name, (-L + 0.2, 0, 0), (0, 0, LEG_REST[1]), [
                (-4.4, -1.15, -1.0, 4.4, 2.3, 2.0, 'carpus'),
                (-1.4, -1.9, -0.3, 0.6, 1.0, 0.6, 'legspine')]),
            part(name + '_propodus', name + '_carpus', (-4.2, 0, 0), (0, 0, LEG_REST[2]), [
                (-5.8, -0.95, -0.9, 5.8, 1.9, 1.8, 'propodus')]),
            part(name + '_dactyl', name + '_propodus', (-5.6, 0, 0), (0, 0, LEG_REST[3]), [
                (-0.7, -0.3, -0.7, 1.4, 4.8, 1.4, 'dactyl')]),
        ]
    return out


CLAW_REST = ((-1.1, 1.3, 0.1), (0.2, -1.3, 0), (0.2, -0.7, 0), (-0.3, 0, 0))   # arm, wrist, palm, finger


def claw(side, s, mirror):
    """A claw (cheliped), on `side` ('right' or 'left'), `s` times the left's size: its arm (merus),
    raised; its wrist (carpus) turning in; its great palm with the fixed finger below; the movable
    finger (dactyl) over it, hinged at the palm's upper front."""
    sx = -1 if side == 'right' else 1
    X = lambda bs: [b if sx < 0 else (-b[0] - b[3],) + b[1:] for b in bs]
    R = lambda r: (r[0], r[1] * -sx, r[2] * -sx)
    n = lambda t: '%s_%s' % (side, t)
    S = lambda *b: tuple(v * s for v in b)
    return [
        part(n('claw'), 'body', (sx * 7.2, 16.4, -9.2), R(CLAW_REST[0]), X([
            S(-1.8, -1.7, -6.2, 3.6, 3.4, 6.2) + ('clawarm',),
            S(-2.3, -2.5, -5.8, 4.6, 1.1, 5.0) + ('clawplate',),
            S(-0.5, -3.0, -4.8, 1.0, 1.0, 1.0) + ('legspine',),
            S(-0.5, -3.0, -2.6, 1.0, 1.0, 1.0) + ('legspine',)]), mirror),
        part(n('wrist'), n('claw'), (0, 0, -6.0 * s), R(CLAW_REST[1]), X([
            S(-2.0, -1.9, -4.8, 4.0, 3.8, 4.8) + ('carpusbig',),
            S(-2.25, -2.15, -1.4, 4.5, 4.3, 1.0) + ('clawring',),
            S(-0.55, -3.1, -4.0, 1.1, 1.4, 1.4) + ('clawspine',),
            S(1.6, -0.6, -3.6, 1.4, 1.1, 1.1) + ('clawspine',)]), mirror),
        part(n('pincer'), n('wrist'), (0, 0, -4.6 * s), R(CLAW_REST[2]), X([
            S(-2.3, -3.8, -7.8, 4.6, 6.8, 7.8) + ('palm',),
            S(-1.1, -4.5, -7.2, 2.2, 0.8, 6.6) + ('palmridge',),
            S(-1.6, 0.4, -13.2, 3.2, 2.5, 5.6) + ('pollex',),
            S(-1.05, 0.8, -15.8, 2.1, 1.7, 2.8) + ('fingertip',),
            S(-0.6, -0.3, -12.4, 1.2, 0.8, 1.1) + ('tooth',),
            S(-0.6, -0.3, -10.4, 1.2, 0.8, 1.1) + ('tooth',),
            S(-0.6, -0.3, -8.6, 1.2, 0.8, 1.1) + ('tooth',)]), mirror),
        part(n('finger'), n('pincer'), (0, -1.6 * s, -7.4 * s), CLAW_REST[3], X([
            S(-1.5, -1.2, -6.6, 3.0, 2.6, 6.6) + ('dactylclaw',),
            S(-1.0, -0.6, -9.2, 2.0, 1.8, 2.8) + ('fingertip',),
            S(-0.6, 1.2, -5.8, 1.2, 0.8, 1.1) + ('tooth',),
            S(-0.6, 1.2, -3.8, 1.2, 0.8, 1.1) + ('tooth',)]), mirror),
    ]


# ── The regalia ──────────────────────────────────────────────────────────────

def helmet():
    y = top_at(0, -4.6)
    return [
        part('helmet', 'body', (0, y + 0.3, -4.6), (-0.08, 0, 0), [
            (-6.2, -1.6, -6.2, 12.4, 1.8, 12.4, 'ghelmbrim'),
            (-5.2, -7.2, -5.2, 10.4, 5.8, 10.4, 'ghelmbowl'),
            (-3.8, -8.9, -3.8, 7.6, 1.8, 7.6, 'ghelmtop'),
            (-0.8, -11.8, -0.8, 1.6, 3.0, 1.6, 'gcollar'),
            (-1.4, -13.6, -1.4, 2.8, 2.0, 2.8, 'gknob'),
            (-5.6, -1.6, 4.6, 11.2, 4.0, 1.2, 'gneckguard'),
            (-1.2, -2.9, -7.0, 2.4, 2.4, 1.2, 'pompom'),
        ]),
        part('frontlet', 'helmet', (0, -6.8, -5.5), (-0.12, 0, 0), [(-4.0, -6.6, -0.3, 8.0, 7.6, 0.6, 'frontlet')]),
        part('jewel', 'frontlet', (0, -2.8, -0.4), (0, 0, 0), [(-1.0, -1.0, -0.5, 2.0, 2.0, 0.6, 'jewel')]),
        part('plume', 'helmet', (0, -13.4, 0), (0, 0, 0), [
            (-2.3, -7.6, -2.3, 4.6, 7.8, 4.6, 'gplume'),
            (-1.6, -11.2, -1.6, 3.2, 3.8, 3.2, 'gplume_top')]),
        part('plume_tail', 'plume', (0, -6.4, 1.6), (1.05, 0, 0), [(-1.8, 0, -0.9, 3.6, 8.4, 1.8, 'plumetail')]),
        part('right_helmwing', 'helmet', (-5.0, -3.4, 0.6), (0, 0.3, 0.55), [(-0.3, -7.2, 0, 0.6, 7.2, 5.0, 'helmwing')]),
        part('right_pompom', 'helmet', (-6.0, -2.0, -3.0), (0, 0, 0), [(-1.2, -1.2, -1.2, 2.4, 2.4, 2.4, 'pompom')]),
        part('right_pompom_back', 'helmet', (-4.6, -2.0, 4.0), (0, 0, 0), [(-1.0, -1.0, -1.0, 2.0, 2.0, 2.0, 'pompom')]),
        # the pheasant plumes (翎子): two long tail feathers rising from the helmet's back, flaring apart and arching back
        part('right_feather', 'helmet', (-2.2, -8.0, 2.2), (0.1, 0, -0.3), [(-1.0, -8.0, -0.3, 2.0, 8.2, 0.6, 'feather')]),
        part('right_feather_2', 'right_feather', (0, -7.8, 0), (-0.16, 0, -0.06), [(-0.95, -8.0, -0.3, 1.9, 8.2, 0.6, 'feather')]),
        part('right_feather_3', 'right_feather_2', (0, -7.8, 0), (-0.26, 0, -0.05), [(-0.85, -8.0, -0.3, 1.7, 8.2, 0.6, 'feather')]),
        part('right_feather_4', 'right_feather_3', (0, -7.8, 0), (-0.34, 0, -0.04), [(-0.75, -7.4, -0.25, 1.5, 7.6, 0.5, 'feather')]),
        part('right_feather_5', 'right_feather_4', (0, -7.2, 0), (-0.45, 0, -0.03), [(-0.6, -6.8, -0.25, 1.2, 7.0, 0.5, 'feather_tip')]),
    ]


def saddle():
    """The saddle of lamellar plates over the shell's rear, its mirror; the socket of the flags behind it;
    the flags; the cape."""
    y = top_at(0, 3.5)
    out = [
        part('saddle', 'body', (0, y - 0.2, 3.2), (0.24, 0, 0), [
            (-5.6, -0.6, 0, 11.2, 1.2, 6.8, 'saddle'),
            (-3.8, -1.2, 0.8, 7.6, 0.8, 4.8, 'saddle2'),
            (-1.6, -1.8, 1.6, 3.2, 0.8, 3.2, 'mirror'),
            (-2.4, -4.2, 5.6, 4.8, 4.4, 2.6, 'socket')]),
        part('right_saddle_flap', 'saddle', (-5.4, 0, 3.4), (0, 0, 0.62), [(-0.5, 0, -3.2, 0.8, 4.2, 6.4, 'saddleflap')]),
        part('cape', 'saddle', (0, 0.2, 7.0), (1.1, 0, 0), [(-6.0, 0, -0.3, 12.0, 6.2, 0.6, 'cape')]),
        part('cape_2', 'cape', (0, 5.6, 0), (-0.95, 0, 0), [(-6.8, 0, -0.3, 13.6, 6.6, 0.6, 'cape_hem')]),
    ]
    # the four back flags (靠旗), fanned out behind
    for i, (x, lean, spread) in enumerate(((-1.0, -0.14, -0.24), (-1.9, -0.22, -0.66))):
        n = 'right_flag_%d' % (i + 1)
        out += [
            part(n, 'saddle', (x, -4.0, 6.8), (lean, 0, spread), [
                (-0.45, -27.0, -0.45, 0.9, 27.4, 0.9, 'flagstaff'),
                (-0.65, -30.4, -0.3, 1.3, 3.6, 0.6, 'flagtip'),
                (-0.7, -27.4, -0.7, 1.4, 1.0, 1.4, 'gcollar'),
                (-1.2, -26.6, -1.2, 2.4, 3.6, 2.4, 'tassel')]),
            part(n + '_cloth', n, (-0.4, -22.6, 0), (0, 0.1, 0), [(-11.0, 0, -0.1, 11.0, 15.0, 0.2, 'flag')]),
        ]
    return out


def ordered(parts):
    out, seen, pending = [], {'root'}, list(parts)
    while pending:
        rest = [q for q in pending if q[1] not in seen]
        for q in pending:
            if q[1] in seen:
                out.append(q)
                seen.add(q[0])
        assert len(rest) < len(pending), [q[0] for q in rest]
        pending = [q for q in rest if q[0] not in seen]
    return out


BODY = part('body', 'root', (0, 0, 0), (0, 0, 0), carapace() + tubercles() + [
    (-10.6, 17.0, -8.4, 21.2, 2.4, 16.4, 'sternum'),
    (-2.6, 19.2, -3.0, 5.2, 0.8, 9.0, 'apron_shell'),
    (-6.6, 17.0, -10.6, 13.2, 2.0, 2.4, 'epistome'),
])

CRAB = mirrored([BODY] + teeth() + face() + legs() + helmet() + saddle())
CRAB += claw('right', 1.3, False) + claw('left', 1.0, True)
CRAB = ordered(CRAB)
CRAB = w.settle(CRAB, ['right_leg_%d_dactyl' % n for n in range(4)])


# ── Painting ─────────────────────────────────────────────────────────────────

def at(box, s, i, j, fw, fh):
    """Where texture pixel (i, j) of face `s` of box (x, y, z, w, h, d) lies in the model."""
    x0, y0, z0, bw, bh, bd = box[:6]
    u, v = (i + 0.5) / fw, (j + 0.5) / fh
    if s == 'top':
        return x0 + u * bw, y0, z0 + bd - v * bd
    if s == 'bottom':
        return x0 + u * bw, y0 + bh, z0 + v * bd
    if s == 'front':
        return x0 + u * bw, y0 + v * bh, z0
    if s == 'back':
        return x0 + bw - u * bw, y0 + v * bh, z0 + bd
    if s == 'right':
        return x0, y0 + v * bh, z0 + bd - u * bd
    return x0 + bw, y0 + v * bh, z0 + u * bd


def H(x, z, seed):
    return w._hash(int(math.floor(x)), int(math.floor(z)), seed)


def shell_colour(x, z, height=0.0):
    """The carapace's colour at (x, z) seen from above: orange at its swellings, crimson to its rim, its
    grooves dark, cream spots and dark freckles; `height` (0 the rim .. 1 the crown) lightens it."""
    e = clamp(abs(x) / max(1, half(z)))
    e = max(e, clamp((abs(z - ZC) - 7) / 4))
    m = vnoise(x * 2 + 40, z * 2 + 40, 3.0, 5)
    c = mixc(SHELL_HI, SHELL, clamp(0.25 + e * 0.9 - height * 0.35 + (m - 0.5) * 0.5))
    if e > 0.7:
        c = mixc(c, CRIMSON, (e - 0.7) / 0.3 * 0.85)
    # the grooves: about the gastric swelling, about the cardiac, the branchial lines out to the sides
    g = math.hypot(x / 5.4, (z + 4.4) / 4.6)
    g2 = math.hypot(x / 3.8, (z - 4.4) / 3.0)
    groove = min(abs(g - 1) * 4.6, abs(g2 - 1) * 3.6)
    for sgn in (-1, 1):                                           # the branchial lines: out and back from the gastric
        bx = sgn * x
        if 4.5 < bx < 13:
            zz = -3.2 + (bx - 4.5) * 0.55
            groove = min(groove, abs(z - zz) * 1.6 + max(0, bx - 12))
    if groove < 1:
        c = mixc(c, CRIMSON_DEEP, (1 - groove) * 0.75)
    elif groove < 1.6:
        c = mixc(c, SHELL_HI, (1.6 - groove) * 0.35)              # the swelling's lit lip beside its groove
    # cream spots, larger toward the crown; dark freckles; fine granules
    gx, gz = x * 0.85, z * 0.85
    cx, cz = math.floor(gx), math.floor(gz)
    h = H(gx, gz, 17)
    if h > 0.72 and e < 0.9:
        ox, oz = 0.25 + 0.5 * H(gx, gz, 23), 0.25 + 0.5 * H(gx, gz, 29)
        d = math.hypot(gx - cx - ox, gz - cz - oz)
        r = 0.22 + 0.16 * (h - 0.72) / 0.28
        if d < r:
            c = mixc(CREAM, SHELL_HI, d / r * 0.6)
        elif d < r + 0.12:
            c = k(c, 0.8)
    if H(x * 1.7, z * 1.7, 41) > 0.93:
        c = mixc(c, CRIMSON_DEEP, 0.65)
    if H(x * 2, z * 2, 47) > 0.8:
        c = k(c, 1.12)
    return c


def paint_carapace(p, style, f):
    x0, y0, z0, bw, bh, bd, n = CARA[style]
    height = n / (len(TIERS) - 1)
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                x, y, z = at(CARA[style], s, i, j, fw, fh)
                if s == 'top':
                    c = k(shell_colour(x, z, height), 1.06)
                elif s == 'bottom':
                    c = k(UNDER, 0.72) if n == 0 else k(CRIMSON, 0.6)
                else:
                    # a step's side: the shell turning down, crimson, lit along its lip
                    t = (j + 0.5) / fh
                    base = shell_colour(x, z, height)
                    c = mixc(base, CRIMSON, 0.35 + 0.4 * t)
                    if j == 0:
                        c = mixc(base, SHELL_HI, 0.4)
                    c = k(c, LIGHTS[s])
                    if n == 0:
                        # the rim: its granules beaded along it, its underside's lip pale
                        if (i + (j // 2)) % 3 == 0 and 0 < j < fh - 2:
                            c = mixc(c, CREAM, 0.35)
                        if j >= fh - 1:
                            c = k(UNDER_DEEP, LIGHTS[s])
                p.px((fx + i, fy + j), noisy(p, c, 3))


def shell(p, f, faces=None, base=SHELL, spots=0.06, freckles=0.04, under=False, along='x', seed=0):
    """Shell for a leg or a claw: red-orange over its top, crimson down its sides, cream beneath (if
    `under`), mottled, cream-spotted, crimson-freckled."""
    for s in faces or f:
        fx, fy, fw, fh = f[s]
        for j in range(fh):
            for i in range(fw):
                m = vnoise(fx + i, fy + j, 2.5, 9 + seed)
                if s == 'top':
                    c = mixc(SHELL_HI, base, 0.45 + 0.4 * m)
                elif s == 'bottom':
                    c = mixc(UNDER, SHELL_HI, 0.45) if under else mixc(base, CRIMSON, 0.5)
                else:
                    t = (j + 0.5) / fh
                    c = mixc(mixc(SHELL_HI, base, 0.5 + 0.5 * m), CRIMSON, clamp(t * 0.9 - 0.1))
                    if under and t > 0.72:
                        c = mixc(c, mixc(UNDER, SHELL_HI, 0.45), (t - 0.72) * 3)
                    if j == 0:
                        c = mixc(c, SHELL_HI, 0.45)
                if p.rnd.random() < spots:
                    c = mixc(c, CREAM, 0.7)
                elif p.rnd.random() < freckles:
                    c = mixc(c, CRIMSON_DEEP, 0.6)
                p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))


def bands(p, f, at_start=True, at_end=True):
    """The pale joint membrane at each end of a leg segment (its long axis along x: the side faces'
    columns), a crimson ring by it."""
    for s in ('top', 'bottom', 'front', 'back'):
        fx, fy, fw, fh = f[s]
        for j in range(fh):
            for i in ((0, 1) if at_start else ()) + ((fw - 1,) if at_end else ()):
                p.px((fx + i, fy + j), k(UNDER_DEEP, LIGHTS[s]))
            if at_start and fw > 3:
                p.px((fx + 2, fy + j), k(CRIMSON_DEEP, LIGHTS[s]))


def spines_along(p, f, faces=('top',), every=3, colour=None):
    """Little spines along a segment's upper edge: a pale point, its shadow."""
    colour = colour or CREAM
    for s in faces:
        fx, fy, fw, fh = f[s]
        for i in range(1, fw - 1, every):
            p.px((fx + i, fy), colour)
            if fh > 2:
                p.px((fx + i, fy + 1), k(CRIMSON, 0.9))


def taper(p, rect, toward='left', keep=0.35):
    """Cuts a face (its long axis across it) to a point toward one end."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        t = i / max(1, fw - 1) if toward == 'right' else 1 - i / max(1, fw - 1)   # 0 its root .. 1 its point
        half_ = max(keep, (1 - t) * 0.5 + 0.12) * fh
        for j in range(fh):
            if abs(j + 0.5 - fh / 2) > half_:
                p.clear((fx + i, fy + j))


def gold_relief(p, rect, light=1.0, pattern='cloud'):
    """Gilding, embossed: lit bright toward its top, ribbed (each rib lit on one edge, shadowed on the
    other), with a band of waves or of cloud scrolls raised across it, catching the light."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            t = j / max(1, fh - 1)
            c = mixc(GILT_HI, GILT, min(1, t * 1.8)) if t < 0.55 else mixc(GILT, GILT_DEEP, (t - 0.55) * 1.6)
            if pattern == 'wave':
                yy = (j + 1.6 * math.sin(i * 1.05)) % 4
                if yy < 1:
                    c = mixc(c, GILT_DEEP, 0.6)
                elif yy < 2:
                    c = mixc(c, (255, 248, 200), 0.45)
            elif pattern == 'cloud':
                if 0.3 < t < 0.75:                                # the band of scrolls
                    cx, cy = (i % 8) - 3.5, ((j - fh * 0.3) % 7) - 3
                    r = math.hypot(cx, cy)
                    if abs(r - 2.2) < 0.45 and math.atan2(cy, cx) > -2.2:
                        c = mixc(c, (255, 248, 200), 0.6)
                    elif abs(r - 2.2) < 0.9 and math.atan2(cy, cx) > -2.2 and cy > 0:
                        c = mixc(c, GILT_DEEP, 0.45)
                if i % 6 == 0:
                    c = mixc(c, (255, 248, 200), 0.4)
                elif i % 6 == 1:
                    c = mixc(c, GILT_DEEP, 0.35)
            p.px((fx + i, fy + j), noisy(p, k(c, light), 3))


def paint_crab(p, style, f, dims):
    if style.startswith('cara_'):
        paint_carapace(p, style, f)
        return
    if style in ('shaft', 'collar', 'butt', 'plume', 'plumetail', 'tassel', 'helmwing'):
        if style == 'helmwing':
            fish.paint_fish(p, 'helmwing', f, dims)
        else:
            shrimp.paint_shrimp(p, style, f, dims)
        return

    if style == 'tubercle':                                       # a knob of shell: lit orange at its crown, crimson round its foot
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    if s == 'top':
                        d = math.hypot((i + 0.5) / fw - 0.5, (j + 0.5) / fh - 0.5)
                        c = mixc(CREAM, SHELL_HI, min(1, d * 3)) if d < 0.35 else SHELL
                    else:
                        c = mixc(SHELL_HI, CRIMSON, (j + 0.5) / fh)
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))
    if style == 'sternum':                                        # its underside: pale, plated
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    c = UNDER if s == 'bottom' else mixc(UNDER, UNDER_DEEP, 0.5)
                    if s == 'bottom' and (j % 5 == 0 or abs(i - fw // 2) < 1):
                        c = UNDER_DEEP
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s] * 0.9), 3))
    if style == 'apron_shell':
        w.flat(p, f, UNDER_DEEP)
    if style == 'epistome':                                       # under the brow: the mouth's frame, dark between the plates
        shell(p, f, spots=0.02)
        fx, fy, fw, fh = f['front']
        for j in range(fh):
            for i in range(fw):
                if abs(i + 0.5 - fw / 2) < fw * 0.28:
                    p.px((fx + i, fy + j), noisy(p, (60, 14, 20) if j > 1 else (110, 28, 30), 3))
                elif j == 0:
                    p.px((fx + i, fy + j), SHELL_HI)
    if style in ('spinebase', 'spinetip', 'browtooth'):           # the margin's spines: shell, paling to a sharp cream point
        shell(p, f, spots=0.0, freckles=0.0)
        if style == 'spinetip':
            for s in ('front', 'back', 'top', 'bottom'):
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    for i in range(fw):
                        t = (i + 0.5) / fw if s == 'back' else 1 - (i + 0.5) / fw     # 0 its root .. 1 its point
                        p.px((fx + i, fy + j), noisy(p, k(mixc(SHELL, CREAM, t), LIGHTS[s]), 3))
                taper(p, f[s], toward='left' if s in ('front', 'top', 'bottom') else 'right', keep=0.2)
            w.flat(p, f, CREAM, faces=('right',))
        if style == 'browtooth':
            for s in SIDES:
                w.point(p, f[s], down=True, keep=0.3, sharp=0.5)
    if style in ('eyestalk', 'orbit'):                            # the stalk: pale shell, ringed; the orbit's crimson rim
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    c = mixc(SHELL_HI, UNDER, 0.4) if style == 'eyestalk' else CRIMSON
                    if style == 'eyestalk' and j % 4 == 3:
                        c = k(SHELL, 0.9)
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))
    if style == 'eye':                                            # a glossy black compound eye, a cornea's glint, a gold rim below
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    hexed = (i + (j // 2) % 2) % 2 == 0 and j % 2 == 0
                    c = (26, 22, 30) if not hexed else (52, 50, 64)
                    if j >= fh - 1 and s in SIDES:
                        c = GOLD_DEEP
                    p.px((fx + i, fy + j), k(c, LIGHTS[s]))
            if s in ('front', 'right', 'left', 'top'):
                p.lit((fx + 1, fy + 1), (240, 246, 255))
                p.lit((fx + 2, fy + 1), (170, 190, 220), 0.8)
                p.lit((fx + 1, fy + 2), (130, 150, 190), 0.6)
    if style in ('maxilliped', 'palp'):                           # the plates of its mouthparts: cream, edged, bristled
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    c = mixc(UNDER, SHELL_HI, 0.25)
                    if i in (0, fw - 1) or j == fh - 1:
                        c = mixc(SHELL, CRIMSON, 0.4)
                    if style == 'maxilliped' and j == fh // 2:
                        c = UNDER_DEEP
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))
            if style == 'palp' and s in SIDES:
                w.tufts(p, (fx, fy, fw, fh), depth=2, every=2)
    if style in ('antenna', 'antennule'):
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    a = j if s in ('right', 'left', 'top', 'bottom') else 0
                    c = SHELL_HI if (a // 2) % 2 else CRIMSON
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))

    # ── legs ──
    if style in ('merus', 'carpus', 'propodus'):
        shell(p, f, under=True, spots=0.05)
        bands(p, f)
        spines_along(p, f, faces=('front', 'back'), every=3)
        if style == 'propodus':                                   # the bristles along its underside
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i in range(0, fw, 2):
                    p.px((fx + i, fy + fh - 1), (70, 40, 30))
    if style == 'dactyl':                                         # the dactyl: shell darkening to a black, spiky point
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    t = (j + 0.5) / fh
                    c = mixc(mixc(SHELL, CRIMSON, 0.3), TIP, clamp((t - 0.35) * 1.8))
                    if j < 2:
                        c = UNDER_DEEP
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))
            if s in SIDES:
                w.point(p, (fx, fy, fw, fh), down=True, keep=0.3, sharp=0.32)
                for j in range(3, fh - 3, 3):                     # little spines down it
                    p.px((fx, fy + j), CREAM)
    if style in ('legspine', 'clawspine'):
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), noisy(p, k(mixc(CREAM, SHELL, j / max(1, fh)), LIGHTS[s]), 3))
            if s in SIDES:
                w.point(p, (fx, fy, fw, fh), down=False, keep=0.3, sharp=0.6)

    # ── claws ──
    if style in ('clawarm', 'carpusbig'):
        shell(p, f, under=True, spots=0.07, seed=3)
        for s in SIDES:                                           # granules
            fx, fy, fw, fh = f[s]
            for _ in range(fw * fh // 10):
                x, y = fx + p.rnd.randrange(fw), fy + p.rnd.randrange(max(1, fh // 2))
                p.px((x, y), k(SHELL_HI, 1.1))
                p.px((x, y + 1), k(CRIMSON, 0.9))
    if style == 'palm':                                           # the great palm: knobbed, crimson-shaded, cream below
        shell(p, f, under=True, spots=0.05, seed=5)
        for s in SIDES + ('top',):
            fx, fy, fw, fh = f[s]
            for _ in range(fw * fh // 9):                         # tubercles: lit knobs, a shadow under each
                x, y = fx + p.rnd.randrange(1, max(2, fw - 1)), fy + p.rnd.randrange(max(1, int(fh * 0.65)))
                p.px((x, y), mixc(CREAM, SHELL_HI, 0.5))
                p.px((x + 1, y), k(SHELL_HI, 1.05))
                p.px((x, y + 1), k(CRIMSON, 0.85))
            if s in ('right', 'left'):                            # the ridges along its outer face
                for i in range(fw):
                    for yy in (int(fh * 0.3), int(fh * 0.55)):
                        if (i // 2) % 2 == 0:
                            p.px((fx + i, fy + yy), k(SHELL_HI, 1.08))
                            p.px((fx + i, fy + yy + 1), k(CRIMSON, 0.85))
        fx, fy, fw, fh = f['front']                               # its front: where the fingers part, dark
        for j in range(int(fh * 0.38), int(fh * 0.58)):
            for i in range(1, fw - 1):
                p.px((fx + i, fy + j), (50, 12, 18))
    if style == 'palmridge':
        shell(p, f, spots=0.0)
        spines_along(p, f, faces=('right', 'left'), every=2)
    if style in ('pollex', 'dactylclaw'):                         # a finger: shell darkening toward its tip, serrated edge
        shell(p, f, under=style == 'pollex', spots=0.04, seed=7)
        for s in ('right', 'left', 'top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    # 0 its root .. 1 its tip (toward -z: the right face's last column, the left's first; the top's last row)
                    t = {'right': (i + 0.5) / fw, 'left': 1 - (i + 0.5) / fw, 'top': (j + 0.5) / fh, 'bottom': 1 - (j + 0.5) / fh}[s]
                    if t > 0.55:
                        p.px((fx + i, fy + j), noisy(p, mixc(p.get((fx + i, fy + j)), TIP, (t - 0.55) * 1.6), 3))
        for s in ('right', 'left'):                               # the serrated cutting edge
            fx, fy, fw, fh = f[s]
            yy = 0 if style == 'pollex' else fh - 1
            for i in range(fw):
                if i % 3 != 2:
                    p.px((fx + i, fy + yy), TOOTH if i % 3 == 0 else mixc(TOOTH, UNDER_DEEP, 0.5))
                else:
                    p.clear((fx + i, fy + yy))
    if style == 'fingertip':                                      # the black tip, curved to a point
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), noisy(p, k(mixc(TIP, (80, 50, 50), 0.3 if j == 0 else 0), LIGHTS[s]), 3))
        for s in ('right', 'left'):
            taper(p, f[s], toward=s, keep=0.25)
        for s in ('top', 'bottom'):
            w.point(p, f[s], down=(s == 'top'), keep=0.3, sharp=0.45)
    if style == 'tooth':                                          # the pincers' blunt teeth: ivory knobs
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), noisy(p, k(mixc(TOOTH, UNDER_DEEP, 0.3 * (j / max(1, fh))), LIGHTS[s]), 3))

    # ── regalia ──
    if style == 'clawplate':                                      # teal lamellar plates over the claw's arm, gold-edged
        for s, r in f.items():
            if s == 'bottom':
                lacquer(p, {s: r}, TEAL, TEAL_DEEP)
                continue
            armour_scales(p, r, LIGHTS[s], cw=6, ch=4)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), GOLD)
                p.px((fx + i, fy + fh - 1), GOLD_DEEP)
        fx, fy, fw, fh = f['top']
        for j in range(fh):
            p.px((fx, fy + j), GOLD)
            p.px((fx + fw - 1, fy + j), GOLD)
            p.px((fx + fw // 2, fy + j), GOLD_DEEP)
    if style == 'clawring':                                       # a gold ring at the wrist, a red band in it
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh // 2), RED)
    if style in ('ghelmbrim', 'ghelmbowl', 'ghelmtop', 'gcollar', 'gknob'):   # the gilded helmet
        for s, r in f.items():
            gold_relief(p, r, LIGHTS[s], pattern='wave' if style == 'ghelmbrim' else 'cloud')
        if style == 'ghelmbrim':                                  # an enamel band round the brim: teal, a red bead row
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    for j in range(1, fh - 1):
                        p.px((fx + i, fy + j), noisy(p, k(TEAL, LIGHTS[s]), 3))
                    if i % 3 == 1:
                        p.px((fx + i, fy + fh // 2), RED)
                    p.px((fx + i, fy), (255, 236, 160))
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
        if style == 'ghelmbowl':                                  # its ribs, down the bowl
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(0, fw, 5):
                    for j in range(fh):
                        p.px((fx + i, fy + j), (255, 240, 170) if j % 2 else GOLD)
                        p.px((fx + i + 1, fy + j), GOLD_DEEP)
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
                    p.px((fx + i, fy), (255, 240, 170))
        if style == 'gknob':
            for s in SIDES:
                circle(p, f[s][0] + f[s][2] / 2, f[s][1] + f[s][3] / 2, 0, 1.1, (255, 250, 220))
    if style == 'gneckguard':                                     # the neck guard: teal lamellae under a gold rim
        for s, r in f.items():
            armour_scales(p, r, LIGHTS[s], cw=5, ch=3)
        for s in SIDES:
            rim(p, f[s], GOLD, top=True)
            ragged(p, f[s], teeth=4, depth=1)
    if style == 'frontlet':                                       # a flame-shaped frontlet: a gold rim round teal enamel, a cloud scroll, the jewel's setting
        for s_, (fx, fy, fw, fh) in f.items():
            if s_ not in ('front', 'back'):
                clear_face(p, (fx, fy, fw, fh))
                continue
            for j in range(fh):
                for i in range(fw):
                    u = abs(i + 0.5 - fw / 2) / (fw / 2)             # 0 its middle .. 1 its edge
                    v = j / max(1, fh - 1)                            # 0 its top .. 1 its foot
                    # a flame: a point up the middle, two lesser tongues either side
                    edge = min(1.0, 0.14 + v * 1.2 + 0.3 * abs(math.sin(u * math.pi * 1.5)))
                    if u > edge:
                        p.clear((fx + i, fy + j))
                    elif u > edge - 0.22 or v > 0.88:
                        p.px((fx + i, fy + j), noisy(p, mixc(GILT_HI, GILT, v), 3))
                    elif u > edge - 0.3:
                        p.px((fx + i, fy + j), GILT_DEEP)
                    else:
                        c = mixc((40, 150, 140), TEAL_DEEP, v * 0.8)
                        if abs(math.hypot(u * 4, (v - 0.35) * 6) - 1.6) < 0.35:
                            c = GILT_HI                           # a gold scroll in the enamel
                        p.px((fx + i, fy + j), noisy(p, c, 3))
            circle(p, fx + fw / 2, fy + fh * 0.62, 1.9, 2.9, GILT_HI)
    if style == 'jewel':                                          # a red jewel, glowing
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    d = math.hypot((i + 0.5) / fw - 0.4, (j + 0.5) / fh - 0.4)
                    c = mixc((255, 150, 140), (200, 20, 34), min(1, d * 2.2))
                    p.lit((fx + i, fy + j), c, 0.75)
    if style in ('gplume', 'gplume_top'):                         # the tall red plume: horsehair, ragged, a gold cup at its foot
        shrimp.paint_shrimp(p, 'plume', f, dims)
        if style == 'gplume':
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), GOLD)
                    p.px((fx + i, fy + fh - 2), GOLD_DEEP)
    if style == 'pompom':                                         # a red silk pompom (绒球), fluffy
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    d = math.hypot((i + 0.5) / fw - 0.42, (j + 0.5) / fh - 0.4)
                    p.px((fx + i, fy + j), noisy(p, k(mixc((255, 90, 80), RED_DEEP, min(1, d * 1.6)), LIGHTS[s]), 8))
            if s in SIDES:
                for i in (0, fw - 1):
                    p.clear((fx + i, fy))
                    p.clear((fx + i, fy + fh - 1))
    if style in ('feather', 'feather_tip'):                       # a pheasant's tail feather: tawny, barred dark, each bar edged pale; a dark shaft
        for s_, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    yy = j % 7
                    c = mixc(PHEASANT, (226, 170, 90), 0.5 * (1 - abs(i + 0.5 - fw / 2) / (fw / 2)))
                    if yy in (0, 1):
                        c = PHEASANT_DARK
                    elif yy == 2:
                        c = PHEASANT_PALE
                    if s_ in ('front', 'back') and abs(i + 0.5 - fw / 2) < 0.6:
                        c = mixc(c, PHEASANT_DARK, 0.6)
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s_]), 3))
            if style == 'feather_tip' and s_ in SIDES:
                w.point(p, (fx, fy, fw, fh), down=False, keep=0.3, sharp=0.3)
    if style in ('saddle', 'saddle2', 'saddleflap'):              # the saddle: teal lamellar plates, laced, gold-edged
        for s, r in f.items():
            if s == 'bottom':
                lacquer(p, {s: r}, TEAL, TEAL_DEEP)
                continue
            armour_scales(p, r, LIGHTS[s], cw=6 if style != 'saddle2' else 5, ch=4)
        for s in ('top',):
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                for j in (0, 1, fh - 2, fh - 1):
                    p.px((fx + i, fy + j), GOLD if j in (0, fh - 1) else GOLD_DEEP)
            for j in range(fh):
                for i in (0, 1, fw - 2, fw - 1):
                    p.px((fx + i, fy + j), GOLD if i in (0, fw - 1) else GOLD_DEEP)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                for j in range(fh):
                    p.px((fx + i, fy + j), GOLD if j < fh / 2 else GOLD_DEEP)
        if style == 'saddleflap':
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    for i in range(fw):
                        if i in (0, fw - 1) or j == fh - 1:
                            p.px((fx + i, fy + j), GOLD)
                ragged(p, f[s], teeth=4, depth=1)
    if style == 'mirror':                                         # the gold mirror at the saddle's middle, a wave across it
        gold_box(p, f)
        fx, fy, fw, fh = f['top']
        cx, cy, r = fx + fw / 2, fy + fh / 2, fw / 2
        for j in range(fh):
            for i in range(fw):
                d = math.hypot(fx + i + 0.5 - cx, fy + j + 0.5 - cy)
                if d > r - 0.8:
                    p.px((fx + i, fy + j), GOLD_DEEP)
                else:
                    p.px((fx + i, fy + j), mixc((255, 250, 220), GOLD, (i + j) / (fw + fh)))
        for i in range(1, fw - 1):
            p.px((fx + i, fy + fh // 2 + int(round(0.9 * math.sin(i * 1.3)))), (40, 130, 120))
    if style == 'socket':                                         # the flags' socket (旗筒): red leather, gold bands
        w.leather(p, f, RED_DEEP, stitch=GOLD_DEEP)
        for s in SIDES + ('top',):
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), GOLD)
                p.px((fx + i, fy + fh - 1), GOLD_DEEP)
            if s == 'back':
                circle(p, fx + fw / 2, fy + fh / 2, 0, 2.0, GOLD)
                circle(p, fx + fw / 2, fy + fh / 2, 0, 0.9, RED)
    if style in ('cape', 'cape_hem'):                             # the red cape: heavy silk, folds, a gold border, a wave at its hem
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    fold = 0.5 + 0.5 * math.sin(i * 2 * math.pi / 7 + 0.3 * j / max(1, fh))
                    c = mixc(RED_DEEP, (222, 46, 40), 0.35 + 0.65 * fold)
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))
            if s in ('front', 'back'):
                for j in range(fh):
                    for i in (0, 1, fw - 2, fw - 1):
                        p.px((fx + i, fy + j), GOLD if i in (1, fw - 2) else GOLD_DEEP)
                if style == 'cape_hem':
                    for i in range(fw):                           # the sea at its hem: a gold wave over a teal band
                        y = fh - 6 - int(round(1.4 * (1 + math.sin(i * 0.8))))
                        p.px((fx + i, fy + y), GOLD)
                        for j in range(fh - 4, fh - 1):
                            p.px((fx + i, fy + j), noisy(p, TEAL if (i + j) % 4 else TEAL_LIGHT, 3))
                        p.px((fx + i, fy + fh - 1), GOLD)
                    ragged(p, (fx, fy, fw, fh), teeth=3, depth=1)
                else:
                    circle(p, fx + fw / 2, fy + fh * 0.55, 2.6, 3.6, GOLD)       # a gold roundel, a wave in it
                    circle(p, fx + fw / 2, fy + fh * 0.55, 0, 2.6, TEAL)
                    for i in range(-2, 3):
                        p.px((fx + fw / 2 + i, fy + fh * 0.55 + round(0.8 * math.sin(i * 1.4))), (230, 244, 240))
    if style == 'flagstaff':                                      # red lacquer, gold bands
        shrimp.paint_shrimp(p, 'shaft', f, dims)
    if style == 'flagtip':                                        # a gold spear tip
        gold_box(p, f)
        for s in SIDES:
            w.point(p, f[s], down=False, keep=0.3, sharp=0.35)
    if style == 'flag':                                           # a back flag (靠旗): a red field, a toothed teal-and-gold border, a gold wave
        for s, (fx, fy, fw, fh) in f.items():
            if s not in ('front', 'back'):
                clear_face(p, (fx, fy, fw, fh))                # its edges: nothing (the triangle's are cut in its faces)
                continue
            for j in range(fh):
                for i in range(fw):
                    ii = fw - 1 - i if s == 'front' else i     # 0 at the staff .. fw-1 at the fly
                    a = ii / max(1, fw - 1)
                    bnd = fh * (1 - 0.92 * a)                   # the flag's lower edge: rising from the staff's foot to its fly
                    if j > bnd:
                        p.clear((fx + i, fy + j))
                        continue
                    edge = min(j, bnd - j, ii)                  # how far in from its edge
                    c = mixc((234, 50, 40), (176, 24, 28), j / fh)
                    if edge < 1.2:
                        c = GOLD
                    elif edge < 3.2:                            # the toothed border: teal teeth into gold
                        tooth = ((ii + j) % 4) < 2
                        c = TEAL if tooth else (GOLD if edge < 2.2 else (234, 50, 40))
                    elif edge < 4.0:
                        c = GOLD_DEEP
                    p.px((fx + i, fy + j), noisy(p, c, 3))
            for ii in range(5, fw - 4):                         # a gold wave across it
                a = ii / max(1, fw - 1)
                yy = fh * 0.36 * (1 - 0.5 * a) + 1.4 * math.sin(ii * 0.9)
                i = fw - 1 - ii if s == 'front' else ii
                if yy < fh * (1 - 0.92 * a) - 4:
                    p.px((fx + i, fy + yy), (255, 226, 120))
                    p.px((fx + i, fy + yy + 1), GOLD_DEEP)
            for ii in range(5, fw // 2):                        # and a crest of foam over it
                a = ii / max(1, fw - 1)
                yy = fh * 0.62 * (1 - 0.6 * a)
                i = fw - 1 - ii if s == 'front' else ii
                if ii % 3 and yy < fh * (1 - 0.92 * a) - 4:
                    p.px((fx + i, fy + yy), (240, 240, 230))


if __name__ == '__main__':
    p = Painter(31)
    slots = paint(CRAB, p, paint_crab)
    p.save('crab_general')
    emit(CRAB, slots, JAVA)
    w.report('crab general', CRAB, SIZE, slots)
    low = [q for q in CRAB if 'flag' not in q[0] and 'feather' not in q[0] and 'plume' not in q[0]]
    w.report('   without flags and plumes', low, SIZE, slots)
