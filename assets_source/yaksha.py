"""The Yaksha (夜叉) of the sea, the Dragon King's patrol demon (巡海夜叉). As shrimp_soldier.py: its
model and skin come from one list of parts (beastgen): the model is written into YakshaRenderer's
layer() between its GENERATED markers, the skin painted to the same box-UV layout at twice the model's
resolution (its fins and the webs of its hands half clear; the renderer draws it translucent), with a
second skin drawn full-bright: its bulging eyes, the tips of its flaming hair, its pearls.

A fierce, muscular sea demon, taller than a man, crouched to spring: indigo-blue skin, darker in its
hollows and lit cyan over its muscles, the slabs of its chest, its ridged belly, its great shoulders
and arms; patches of blue-green scales over its shoulders and down its forearms; a spined fin down its
back, fins on its forearms and calves, fin-ears, webbed and clawed hands and feet. Its head thrust
forward: wild hair standing up off it like flames, red at the root to yellow at the tips, a flaming
beard under its jaw; a sloping, furrowed forehead to a crag of a brow jutting out over deep sockets,
knotted between the eyes and raised to a scowl at its ends, small slanted eyes burning gold sunk in its
shadow; jutting cheekbones; a heavy muzzle thrust out before the face, a broad flat nose on it, its
nostrils flared; a snarl of dark lips drawn back off dark red gums, its mouth open dark into its throat,
two great fangs hanging from its upper jaw, and two tusks rising from the corners of its great jaw past
the muzzle, yellowed and shaded; two ridged horns curving up and back from its temples.

Its ornaments: gold armlets and anklets, gold hoops in its ears, a necklace of pearls and little
skulls; a long red silk scarf (披帛) over its shoulders, its ends flowing behind; a wrap of sea-green
fish-scale hide about its hips over a red apron, held by a gold belt with a demon-faced buckle. The
army's kit: teal-and-gold bracers, a teal spaulder on its left shoulder, and its great trident (三股叉):
a steel centre prong with barbs, two side prongs on a crossbar, a gold demon-mouth socket, a red
tassel and the blue wave pennant, on a red shaft banded in gold.

Run from the repository root: python assets_source/yaksha.py"""
import math

import beastgen as bg
import wild_paint as w
from wild_paint import part, mirrored, aim, paint, emit, vnoise, clamp
from king_yama import ragged, circle, mixc, k, noisy
import shrimp_soldier as shrimp
from shrimp_soldier import LIGHTS, SIDES, lacquer, gold_box, rim, TEAL, TEAL_DEEP, TEAL_LIGHT, GOLD, GOLD_DEEP, RED, RED_DEEP, SEA
import fish_soldier as fish
from fish_soldier import armour_scales, clear_face, fin
from ogre import muscle

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/YakshaRenderer.java'

# How much larger than its pixels it is drawn: YakshaRenderer.SIZE
SIZE = 0.88

SKIN = (62, 118, 210)           # indigo-blue,
SKIN_DEEP = (26, 42, 112)       # darker in its hollows,
SKIN_HI = (136, 212, 252)       # lit cyan over its muscles
BELLY = (96, 156, 196)
SCALE = (40, 130, 150)          # its patches of blue-green scales
SCALE_EDGE = (16, 50, 74)
FLAME_ROOT = (150, 18, 24)      # its hair: red at the root,
FLAME = (240, 70, 24)           # orange,
FLAME_TIP = (255, 206, 80)      # yellow at the tips
EYE = (255, 214, 60)
TOOTH = (232, 222, 194)         # its fangs and tusks: an off-white, yellowed,
TOOTH_SHADE = (150, 126, 96)    # shaded down their sides,
TOOTH_ROOT = (104, 70, 58)      # stained where they leave the gum
GUM = (96, 18, 36)              # its gums: dark red,
LIP = (36, 20, 66)              # its lips: a dark indigo-violet,
THROAT = (22, 4, 12)            # its mouth: dark into its throat,
TONGUE = (140, 34, 58)
HORN = (226, 210, 168)
HORN_DEEP = (96, 76, 56)
NAIL = (24, 22, 30)
PEARL = (236, 240, 250)
BONE = (230, 218, 186)
SILK = (214, 40, 46)            # its scarf: red silk
SILK_DEEP = (120, 14, 30)
WRAP = (52, 140, 112)           # its fish-scale wrap: sea-green
WRAP_EDGE = (14, 54, 46)
STEEL = (206, 214, 226)
STEEL_DARK = (110, 118, 134)

BODY = (0.24, 0, 0)
HEAD = (-0.22, 0, 0)
ARM, FOREARM, HAND = (-0.62, 0.1, 0.32), (-0.95, 0.3, 0.05), (0, 0, 0)
LARM, LFOREARM, LHAND = (-0.55, -0.3, -1.0), (-1.35, 0, 0.25), (-0.35, 0, 0)


class Painter(w.Painter):
    def pxa(self, xy, c, a):
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            self.img.putpixel((x, y), tuple(max(0, min(255, int(v))) for v in c[:3]) + (int(a),))


def necklace():
    """Pearls and little skulls on a cord about its neck, hanging over its chest."""
    out = []
    n = 13
    for i in range(n):
        a = -1.25 + 2.5 * i / (n - 1)
        x, y = 4.6 * math.sin(a), -15.6 + 4.6 * math.cos(a) * (1 - abs(a) / 2.4)
        z = -3.6 - 0.5 * math.cos(a)
        if i % 4 == 2:
            out.append((x - 0.85, y - 0.8, z - 0.5, 1.7, 1.9, 1.3, 'nskull'))
        else:
            out.append((x - 0.6, y - 0.6, z - 0.3, 1.2, 1.2, 1.2, 'pearl'))
    return out


def legs():
    return [
        part('right_leg', 'root', (-3.4, 12.6, 0.8), (-0.42, 0, 0.14), [
            (-2.5, -1.2, -2.5, 5.0, 8.4, 5.0, 'thigh'),
            (-1.9, 0.6, -3.2, 3.8, 5.6, 0.9, 'quad')]),
        part('right_shin', 'right_leg', (0, 7.6, 0), (0.78, 0, -0.1), [
            (-1.9, 0, -1.9, 3.8, 7.4, 3.8, 'shin'),
            (-1.6, 0.8, 1.4, 3.2, 4.4, 1.0, 'calf'),
            (-2.25, 5.4, -2.25, 4.5, 1.3, 4.5, 'anklet')]),
        part('right_calffin', 'right_shin', (0, 0.6, 2.2), (-0.35, 0, 0), [(-0.25, 0, 0, 0.5, 5.2, 3.0, 'smallfin')]),
        part('right_foot', 'right_shin', (0, 7.2, 0), (-0.36, 0, 0), [
            (-2.1, 0, -3.6, 4.2, 1.9, 5.4, 'foot'),
            (-2.2, 0.5, -5.4, 1.3, 1.4, 1.9, 'toe'),
            (-0.65, 0.5, -5.8, 1.3, 1.4, 2.3, 'toe'),
            (0.9, 0.5, -5.4, 1.3, 1.4, 1.9, 'toe'),
            (-1.9, 1.3, -5.2, 3.8, 0.4, 1.6, 'web')]),
    ]


def body():
    return [
        part('body', 'root', (0, 12.6, 0.8), BODY, [
            (-4.3, -3.4, -2.7, 8.6, 3.8, 5.4, 'loin'),
            (-3.9, -8.4, -2.5, 7.8, 5.4, 5.0, 'abs'),
            (-5.6, -15.6, -3.1, 11.2, 7.6, 6.4, 'chest'),
            (-5.2, -15.0, -3.9, 5.0, 4.2, 1.0, 'pec'),
            (0.2, -15.0, -3.9, 5.0, 4.2, 1.0, 'pec'),
            (-3.8, -17.2, -1.4, 7.6, 2.0, 4.4, 'traps'),
            (-5.2, -15.2, 2.7, 10.4, 6.8, 1.0, 'lats'),
            (-4.6, -4.2, -3.1, 9.2, 1.7, 6.2, 'gbelt'),
            (-1.7, -5.0, -3.7, 3.4, 3.2, 0.8, 'beltface'),
        ] + necklace()),
        # its wrap of fish-scale hide about the hips, a red apron before, fins of it at the sides and behind
        part('apron', 'body', (0, -2.6, -3.1), (-0.12, 0, 0), [(-1.8, 0, -0.3, 3.6, 8.6, 0.6, 'apron')]),
        part('right_wrap', 'body', (-2.6, -2.6, -3.0), (-0.16, 0, 0.12), [(-2.1, 0, -0.3, 3.6, 6.8, 0.6, 'wrapfront')]),
        part('right_wrap_side', 'body', (-4.3, -2.6, 0), (0, 0, 0.2), [(-0.3, 0, -2.6, 0.6, 6.4, 5.2, 'wrapside')]),
        part('wrap_back', 'body', (0, -2.6, 2.8), (0.2, 0, 0), [(-4.2, 0, -0.3, 8.4, 7.6, 0.6, 'wrapback')]),
        # the spined fin down its back
        part('dorsal', 'body', (0, -16.6, 3.2), (0.3, 0, 0), [(-0.25, 0, 0, 0.5, 13.4, 5.0, 'dorsal')]),
        part('spine', 'body', (0, -16.0, 3.6), (0.02, 0, 0), [(-1.3, 0, -0.4, 2.6, 13.0, 0.8, 'spine')]),
    ]


def head():
    return [
        # the head, sculpted in depth: a sloping forehead to a crag of a brow jutting over deep sockets, small
        # burning eyes sunk in their shadow; jutting cheekbones; a heavy muzzle thrust out before the face,
        # a broad flat nose on it, its lip snarling up off the fangs; a great underslung jaw below
        part('head', 'body', (0, -16.8, -1.4), HEAD, [
            (-3.6, -7.4, -3.2, 7.2, 6.6, 6.6, 'skull'),
            (-3.3, -7.2, -4.0, 6.6, 1.6, 1.0, 'forehead'),
            (-3.8, -5.7, -4.5, 7.6, 1.2, 1.4, 'browshelf'),
            (-1.2, -6.1, -5.25, 2.4, 1.9, 1.5, 'browknot'),
            (-3.5, -4.6, -4.0, 7.0, 1.2, 0.8, 'socket'),
            (-3.7, -3.4, -4.3, 7.4, 2.4, 3.8, 'cheeks'),
            (-4.1, -3.5, -4.6, 1.8, 1.1, 2.2, 'cheekbone'),
            (2.3, -3.5, -4.6, 1.8, 1.1, 2.2, 'cheekbone'),
            (-2.8, -3.6, -6.2, 5.6, 2.1, 2.6, 'muzzle'),
            (-0.8, -5.0, -5.5, 1.6, 1.4, 1.5, 'bridge'),
            (-1.6, -4.3, -6.7, 3.2, 1.9, 1.6, 'nose'),
            (-2.5, -3.1, -6.5, 1.0, 1.1, 1.2, 'nostril'),
            (1.5, -3.1, -6.5, 1.0, 1.1, 1.2, 'nostril'),
            (-3.0, -1.6, -6.3, 6.0, 1.0, 2.8, 'upperlip'),
            (-2.6, -0.8, -5.6, 5.2, 1.2, 4.0, 'maw'),
            (-2.3, -0.7, -6.0, 1.0, 1.3, 0.9, 'fang'),
            (1.3, -0.7, -6.0, 1.0, 1.3, 0.9, 'fang'),
            (-2.1, 0.5, -5.9, 0.6, 1.2, 0.6, 'fangtip'),
            (1.5, 0.5, -5.9, 0.6, 1.2, 0.6, 'fangtip'),
            (-3.0, -7.0, 2.6, 6.0, 6.6, 1.6, 'nape'),
        ]),
        # the scowl: each brow a ridge rising from the knot to its outer end, raised and swept back
        part('right_brow', 'head', (-0.9, -5.35, -4.75), (0, 0.22, 0.3), [(-3.4, -0.75, -0.55, 3.4, 1.5, 1.9, 'brow')]),
        part('jaw', 'head', (0, -0.8, -0.6), (0.24, 0, 0), [
            (-3.1, 0, -5.6, 6.2, 2.2, 5.6, 'jaw'),
            (-2.3, 1.9, -5.3, 4.6, 1.1, 3.0, 'chin')]),
        part('beard', 'jaw', (0, 2.4, -3.6), (0.2, 0, 0), [(-2.4, 0, -1.0, 4.8, 4.4, 2.2, 'flamebeard')]),
        # two tusks from the jaw's corners, rising outside the snarl past the muzzle toward the cheeks
        part('right_tusk', 'jaw', (-3.2, 0.4, -5.0), (-0.42, 0.2, -0.3), [(-0.55, -2.4, -0.55, 1.1, 2.7, 1.1, 'tusk')]),
        part('right_tusk_tip', 'right_tusk', (0, -2.3, 0), (-0.45, 0, 0.25), [(-0.4, -1.4, -0.4, 0.8, 1.6, 0.8, 'tusktip')]),
        # small burning eyes, slanted in a glare, sunk in the shadow under the brow
        part('right_eye', 'head', (-1.9, -4.05, -4.2), (0, 0.18, 0.2), [(-1.5, -0.8, -0.45, 3.0, 1.6, 0.5, 'eye')]),
        # fin-ears, gold hoops in them
        part('right_ear', 'head', (-3.6, -4.4, -0.6), (0.1, 0.6, -0.35), [(-0.25, -3.4, 0, 0.5, 4.8, 4.0, 'earfin')]),
        part('right_hoop', 'head', (-3.7, -1.6, -1.0), (0, 0, 0.1), [
            (-0.25, 0, -0.9, 0.5, 0.45, 1.8, 'hoop'),
            (-0.25, 2.0, -0.9, 0.5, 0.45, 1.8, 'hoop'),
            (-0.25, 0.3, -1.1, 0.5, 1.9, 0.45, 'hoop'),
            (-0.25, 0.3, 0.6, 0.5, 1.9, 0.45, 'hoop')]),
        # two ridged horns, up and back from its temples
        part('right_horn', 'head', (-2.4, -7.0, -2.0), (-0.2, 0, -0.42), [(-0.95, -3.2, -0.95, 1.9, 3.4, 1.9, 'horn1')]),
        part('right_horn_2', 'right_horn', (0, -3.0, 0), (-0.45, 0, 0.12), [(-0.7, -3.0, -0.7, 1.4, 3.2, 1.4, 'horn2')]),
        part('right_horn_3', 'right_horn_2', (0, -2.8, 0), (-0.55, 0, 0.1), [(-0.45, -2.6, -0.45, 0.9, 2.8, 0.9, 'horn3')]),
        # its hair, standing up off its head like flames
        part('hair_crown', 'head', (0, -7.2, -0.6), (-0.3, 0, 0), [(-2.4, -8.4, -1.5, 4.8, 8.6, 3.0, 'flame')]),
        part('right_hair_1', 'head', (-2.4, -7.0, -0.2), (-0.15, 0, -0.55), [(-1.9, -7.6, -1.4, 3.8, 7.8, 2.8, 'flame')]),
        part('right_hair_2', 'head', (-3.0, -5.6, 1.6), (0.4, 0, -0.95), [(-1.7, -6.8, -1.3, 3.4, 7.0, 2.6, 'flame')]),
        part('hair_back', 'head', (0, -6.8, 2.2), (0.75, 0, 0), [(-2.6, -8.2, -1.4, 5.2, 8.4, 2.8, 'flame')]),
        part('right_hair_back', 'head', (-2.0, -5.0, 2.8), (1.0, 0, -0.5), [(-1.8, -6.8, -1.3, 3.6, 7.0, 2.6, 'flame')]),
        part('hair_nape', 'head', (0, -2.4, 3.4), (1.45, 0, 0), [(-2.8, -6.4, -1.2, 5.6, 6.6, 2.4, 'flame')]),
    ]


def scarf_end(side, sx):
    """One end of the scarf: from the shoulder, in three lengths of silk flowing back."""
    n = '%s_scarf' % side
    return [
        part(n, 'body', (sx * 6.6, -15.4, 1.8), (0.3, 0, -sx * 0.45), [(-1.8, 0, -0.25, 3.6, 7.0, 0.5, 'scarf')], sx > 0),
        part(n + '_2', n, (0, 6.8, 0), (0.3, 0, sx * 0.3), [(-1.8, 0, -0.25, 3.6, 7.0, 0.5, 'scarf')], sx > 0),
        part(n + '_3', n + '_2', (0, 6.8, 0), (0.35, 0, sx * 0.15), [(-1.8, 0, -0.25, 3.6, 7.6, 0.5, 'scarftail')], sx > 0),
    ]


def arms():
    held = aim([BODY, ARM, FOREARM, HAND], (0.62, 0, 0.1))
    return [
        part('right_arm', 'body', (-6.4, -14.2, 0.2), ARM, [
            (-3.3, -2.4, -3.1, 5.8, 5.2, 6.2, 'shoulder'),
            (-2.3, 0, -2.3, 4.6, 7.8, 4.6, 'upperarm'),
            (-1.7, 1.2, -3.0, 3.4, 4.4, 0.8, 'bicep'),
            (-2.65, 4.8, -2.65, 5.3, 1.4, 5.3, 'armlet')]),
        part('right_forearm', 'right_arm', (0, 7.4, 0), FOREARM, [
            (-2.0, 0, -2.0, 4.0, 7.0, 4.0, 'forearm'),
            (-2.4, 2.2, -2.4, 4.8, 4.4, 4.8, 'bracer')]),
        part('right_armfin', 'right_forearm', (-1.2, 0.4, 2.0), (0.4, -0.25, 0), [(-0.25, 0, 0, 0.5, 6.2, 3.6, 'armfin')]),
        part('right_hand', 'right_forearm', (0, 6.8, 0), HAND, [
            (-1.9, 0, -1.7, 3.8, 3.0, 3.4, 'palm'),
            (-1.9, 2.6, -2.2, 3.8, 1.8, 1.8, 'fist'),
            (-2.4, 0.6, -1.4, 0.9, 2.2, 1.4, 'thumb')]),
        # the trident: a steel centre prong with barbs, two side prongs on a crossbar, a gold demon-mouth
        # socket, a red tassel and the blue wave pennant, on a red shaft banded in gold
        part('trident', 'right_hand', (0, 1.6, -0.4), held, [
            (-0.5, -30.0, -0.5, 1.0, 46.0, 1.0, 'shaft'),
            (-0.9, -31.2, -0.9, 1.8, 1.4, 1.8, 'collar'),
            (-1.7, -34.6, -1.7, 3.4, 3.4, 3.4, 'socket'),
            (-6.2, -36.0, -0.6, 12.4, 1.4, 1.2, 'crossbar'),
            (-0.7, -47.2, -0.7, 1.4, 11.4, 1.4, 'prong'),
            (-1.4, -51.0, -0.4, 2.8, 4.0, 0.8, 'prongtip'),
            (-6.2, -43.6, -0.55, 1.2, 7.8, 1.1, 'sideprong'),
            (5.0, -43.6, -0.55, 1.2, 7.8, 1.1, 'sideprong'),
            (-6.6, -46.6, -0.35, 2.0, 3.2, 0.7, 'sidetip'),
            (4.6, -46.6, -0.35, 2.0, 3.2, 0.7, 'sidetip'),
            (-1.6, -30.4, -1.6, 3.2, 4.6, 3.2, 'tassel'),
            (-0.7, 15.6, -0.7, 1.4, 1.2, 1.4, 'collar'),
            (-0.4, 16.6, -0.4, 0.8, 2.4, 0.8, 'butt'),
        ]),
        part('right_barb', 'trident', (-0.7, -43.0, 0), (0, 0, -0.5), [(-1.8, -0.5, -0.3, 1.8, 1.0, 0.6, 'barb')]),
        part('right_sidebarb', 'trident', (-5.0, -40.6, 0), (0, 0, 0.55), [(0, -0.5, -0.3, 1.6, 1.0, 0.6, 'barb')]),
        part('pennant', 'trident', (0.5, -25.6, 0), (0, 0, 0), [(0, 0, -0.1, 6.0, 7.6, 0.2, 'pennant')]),
    ]


def left_arm():
    """The left arm, raised, its webbed, clawed hand spread (its own: not mirrored from the right)."""
    return [
        part('left_arm', 'body', (6.4, -14.2, 0.2), LARM, [
            (-3.3, -2.4, -3.1, 5.8, 5.2, 6.2, 'shoulder'),
            (-2.3, 0, -2.3, 4.6, 7.8, 4.6, 'upperarm'),
            (-1.7, 1.2, -3.0, 3.4, 4.4, 0.8, 'bicep'),
            (-2.65, 4.8, -2.65, 5.3, 1.4, 5.3, 'armlet')], True),
        part('left_spaulder', 'left_arm', (0.4, -2.2, 0), (0, 0, -0.3), [
            (-3.6, -0.8, -3.4, 6.2, 1.4, 6.8, 'spauldron'),
            (-3.4, 0.4, -3.6, 6.0, 1.6, 7.2, 'spauldron2'),
            (-3.2, 1.8, -3.7, 5.6, 1.5, 7.4, 'spauldron')], True),
        part('left_forearm', 'left_arm', (0, 7.4, 0), LFOREARM, [
            (-2.0, 0, -2.0, 4.0, 7.0, 4.0, 'forearm'),
            (-2.4, 2.2, -2.4, 4.8, 4.4, 4.8, 'bracer')], True),
        part('left_armfin', 'left_forearm', (1.2, 0.4, 2.0), (0.4, 0.25, 0), [(-0.25, 0, 0, 0.5, 6.2, 3.6, 'armfin')], True),
        part('left_hand', 'left_forearm', (0, 6.8, 0), LHAND, [
            (-1.9, 0, -1.7, 3.8, 3.0, 3.4, 'palm'),
            (-2.4, 0.4, -1.2, 0.9, 2.4, 1.4, 'thumb')], True),
        part('left_finger_1', 'left_hand', (-1.4, 2.8, -0.2), (0, 0, 0.32), [(-0.5, 0, -0.55, 1.0, 3.8, 1.1, 'finger')], True),
        part('left_finger_2', 'left_hand', (-0.2, 2.8, -0.2), (0, 0, 0.1), [(-0.5, 0, -0.55, 1.0, 4.2, 1.1, 'finger')], True),
        part('left_finger_3', 'left_hand', (1.0, 2.8, -0.2), (0, 0, -0.12), [(-0.5, 0, -0.55, 1.0, 4.0, 1.1, 'finger')], True),
        part('left_finger_4', 'left_hand', (1.8, 2.6, -0.2), (0, 0, -0.38), [(-0.45, 0, -0.5, 0.9, 3.4, 1.0, 'finger')], True),
        part('left_web', 'left_hand', (0, 2.6, -0.1), (0, 0, 0), [(-2.3, 0, -0.2, 4.6, 3.0, 0.3, 'handweb')], True),
    ]


def mirror_box(b):
    x, y, z, wd, h, d, s = b
    return (-x - wd, y, z, wd, h, d, s)


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


YAKSHA = mirrored(legs() + body() + head() + arms())
# the left arm is its own (raised, its hand spread): replace the mirrored copies; the trident's barbs are its own
YAKSHA = [q for q in YAKSHA if q[0] not in ('left_arm', 'left_forearm', 'left_armfin', 'left_hand')]
YAKSHA += [(n, pa, pv, r, [mirror_box(b) for b in bx], m) for (n, pa, pv, r, bx, m) in left_arm()]
YAKSHA += scarf_end('right', -1) + scarf_end('left', 1)
YAKSHA += [part('scarf_back', 'body', (0, -16.4, 3.2), (0.3, 0, 0), [(-6.4, -0.6, -0.3, 12.8, 2.6, 0.6, 'scarfback')])]
YAKSHA = ordered(YAKSHA)
YAKSHA = w.settle(YAKSHA, ('right_foot', 'left_foot'))


# ── Painting ─────────────────────────────────────────────────────────────────

def skin(p, f, faces=None, fall=0.2, warts=0.0):
    w.hide(p, f, SKIN, SKIN_DEEP, SKIN_HI, mottle=0.45, warts=warts, faces=faces, fall=fall, seed=7)


def scaled(p, rect, light=1.0, cw=4, ch=3):
    w.scale_tiles(p, rect, k(SCALE, light), k(SCALE_EDGE, light), cw=cw, ch=ch, hi=k((120, 220, 220), light), seed=3)


def flame_face(p, rect, up=True, tongues=2, seed=0, glow=True, keep=0.3):
    """A lock of flaming hair on a face: `tongues` tongues of flame rising (or hanging), each swaying,
    tapering to its point; dark red at the root, red-orange in its body, a hot yellow core up its middle
    (its tip burning, full-bright); a darker line between the tongues; cut away round them."""
    fx, fy, fw, fh = rect
    n = max(1, tongues)
    for j in range(fh):
        t = 1 - (j + 0.5) / fh if up else (j + 0.5) / fh           # 0 at its root .. 1 at its end
        for i in range(fw):
            u = (i + 0.5) / fw
            best = None
            for q in range(n):
                c0 = (q + 0.5) / n + 0.08 * math.sin(t * 4.5 + q * 2.1 + seed)      # each tongue sways
                tall = 1.0 - 0.28 * abs((q + 0.5) / n - 0.5) * 2 - 0.12 * ((q + seed) % 2)
                width = (0.5 / n) * (1.15 - t / tall) ** 0.8 if t < tall else 0
                d = abs(u - c0)
                if d <= width:
                    a = d / max(1e-3, width)                       # 0 its middle .. 1 its edge
                    if best is None or a < best[0]:
                        best = (a, t / tall)
            if best is None or t > 1:
                if t > keep or n == 1:
                    p.clear((fx + i, fy + j))
                    continue
                best = (0.9, t)
            a, r = best
            c = mixc(FLAME_ROOT, FLAME, min(1, r * 2.2))
            core = (1 - a) * min(1, r * 1.6)
            if core > 0.45:
                c = mixc(c, FLAME_TIP, (core - 0.45) * 1.6)
            if a > 0.78:
                c = k(mixc(c, FLAME_ROOT, 0.5), 0.85)               # its edge, dark between the tongues
            if glow and r > 0.55 and a < 0.6:
                p.lit((fx + i, fy + j), c, 0.6 * (r - 0.55) / 0.45)
            else:
                p.px((fx + i, fy + j), noisy(p, c, 4))


def paint_yaksha(p, style, f, dims):
    S = SIDES

    if style in ('collar', 'tassel', 'butt', 'pennant', 'shaft', 'spauldron', 'spauldron2'):
        shrimp.paint_shrimp(p, style, f, dims)
        return
    if style == 'bracer':                                         # the army's bracer: teal lamellae, gold-banded, a gold boss
        for s, r in f.items():
            armour_scales(p, r, LIGHTS[s], cw=5, ch=3)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                for j in (0, 1, fh - 2, fh - 1):
                    p.px((fx + i, fy + j), GOLD if j in (0, fh - 1) else GOLD_DEEP)
            circle(p, fx + fw / 2, fy + fh / 2, 0, 1.5, GOLD)
            circle(p, fx + fw / 2, fy + fh / 2, 0, 0.6, (255, 244, 200))
        return

    # ── its body ──
    if style == 'thigh':
        skin(p, f)
        for s in ('front', 'right', 'left'):
            fx, fy, fw, fh = f[s]
            muscle(p, f[s], fw * 0.5, fh * 0.42, fw * 0.45, fh * 0.42, k_=1.15)
    if style == 'quad':
        skin(p, f, fall=0.1)
        fx, fy, fw, fh = f['front']
        muscle(p, f['front'], fw * 0.5, fh * 0.45, fw * 0.55, fh * 0.55, k_=1.2, rim=0.6)
    if style in ('shin', 'calf'):
        skin(p, f)
        if style == 'shin':                                       # scales down its shin
            for s in ('front',):
                fx, fy, fw, fh = f[s]
                scaled(p, (fx + 1, fy, fw - 2, int(fh * 0.7)), LIGHTS[s], cw=3, ch=2)
        else:
            fx, fy, fw, fh = f['back']
            muscle(p, f['back'], fw * 0.5, fh * 0.35, fw * 0.5, fh * 0.45, k_=1.18)
    if style in ('foot', 'toe', 'palm', 'fist', 'thumb', 'finger'):
        skin(p, f, warts=0.1)
        if style in ('toe', 'thumb', 'finger'):                    # black claws at their ends
            for s in S + ('bottom',):
                fx, fy, fw, fh = f[s]
                if style == 'toe':
                    if s == 'front':
                        p.face((fx, fy, fw, fh), NAIL, 1.0, noise=3)
                    elif s in ('right', 'left', 'top'):
                        for j in range(fh):
                            for i in range(fw):
                                tip = (i < 2) if s == 'left' else (i >= fw - 2) if s == 'right' else (j >= fh - 2)
                                if tip:
                                    p.px((fx + i, fy + j), NAIL)
                else:
                    for i in range(fw):
                        for j in range(max(0, fh - 3), fh):
                            p.px((fx + i, fy + j), k(NAIL, 1.2 if j < fh - 1 else 1.6))
                    if style == 'finger' and s in S:
                        w.point(p, (fx, fy, fw, fh), down=True, keep=0.6, sharp=0.6)
                    for j in range(1, fh - 3, 3):
                        p.px((fx, fy + j), k(SKIN_DEEP, 1.2))
        if style == 'fist':                                       # knuckles, black claws curled round the shaft
            fx, fy, fw, fh = f['front']
            for i in range(0, fw, 2):
                p.px((fx + i, fy), k(SKIN_HI, 0.9))
                p.px((fx + i, fy + fh - 1), NAIL)
                p.px((fx + i + 1, fy + fh - 1), k(NAIL, 1.5))
        if style == 'foot':
            fx, fy, fw, fh = f['top']
            scaled(p, (fx, fy, fw, fh), 1.1, cw=3, ch=2)
    if style in ('web', 'handweb'):                               # the webs between its toes and fingers: half clear, veined
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    a = 170 if s in ('front', 'back', 'top', 'bottom') else 210
                    c = mixc(SKIN_HI, SKIN, 0.4)
                    if i % 3 == 0:
                        c = k(SKIN, 0.8)
                        a = 230
                    p.pxa((fx + i, fy + j), noisy(p, c, 4), a)
            if style == 'handweb' and s in ('front', 'back'):     # its edge scalloped between the fingers
                for i in range(fw):
                    dip = int(1.5 + 1.5 * math.cos(i / max(1, fw) * 4 * math.pi))
                    for j in range(fh - dip, fh):
                        p.clear((fx + i, fy + j))
    if style == 'anklet' or style == 'armlet':                    # gold rings, a band of red beads, gold studs
        gold_box(p, f)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), (255, 240, 170))
                p.px((fx + i, fy + fh - 1), GOLD_DEEP)
                if i % 3 == 1 and fh > 2:
                    p.px((fx + i, fy + fh // 2), RED)
    if style == 'loin':
        skin(p, f)
    if style == 'spine':                                          # the ridge of its spine: a row of blue-green scales, its fin rising from it
        for s_, r in f.items():
            scaled(p, r, LIGHTS[s_] * 1.1, cw=3, ch=2)
    if style == 'abs':                                            # its ridged belly: the six slabs of muscle, a navel
        skin(p, f, fall=0.05)
        fx, fy, fw, fh = f['front']
        for r in range(3):
            for side in (-1, 1):
                muscle(p, f['front'], fw / 2 + side * fw * 0.22, fh * (0.18 + 0.3 * r), fw * 0.2, fh * 0.14, k_=1.18, rim=0.7)
        for j in range(fh):
            p.px((fx + fw // 2, fy + j), k(SKIN_DEEP, 1.1))
        p.px((fx + fw // 2, fy + fh - 2), k(SKIN_DEEP, 0.6))
        for s in ('right', 'left'):                               # the obliques
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 3):
                for i in range(fw):
                    if (i + j) % 6 < 2:
                        p.px((fx + i, fy + j), k(SKIN_DEEP, 1.2))
    if style == 'chest':                                          # the slabs of its chest, its ribs; its back's blades
        skin(p, f)
        fx, fy, fw, fh = f['front']
        for j in range(int(fh * 0.62), fh, 2):
            for i in range(1, int(fw * 0.26)):
                p.px((fx + i, fy + j), k(SKIN_DEEP, 1.25))
                p.px((fx + fw - 1 - i, fy + j), k(SKIN_DEEP, 1.25))
        for s in ('back',):
            fx, fy, fw, fh = f[s]
            for side in (-1, 1):
                muscle(p, f[s], fw / 2 + side * fw * 0.25, fh * 0.35, fw * 0.2, fh * 0.35, k_=1.15)
            for j in range(fh):
                p.px((fx + fw // 2, fy + j), k(SKIN_DEEP, 0.9))
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            scaled(p, (fx, fy, fw, int(fh * 0.4)), LIGHTS[s], cw=4, ch=3)
            muscle(p, f[s], fw * 0.5, fh * 0.6, fw * 0.45, fh * 0.35)
    if style == 'pec':                                            # a slab of its chest, lit across its swell
        skin(p, f, fall=0.1)
        fx, fy, fw, fh = f['front']
        muscle(p, f['front'], fw * 0.5, fh * 0.4, fw * 0.6, fh * 0.6, k_=1.22, rim=0.55)
    if style == 'traps':
        skin(p, f)
        fx, fy, fw, fh = f['top']
        scaled(p, f['top'], 1.1, cw=4, ch=3)
    if style == 'lats':
        skin(p, f)
        fx, fy, fw, fh = f['back']
        for side in (-1, 1):
            muscle(p, f['back'], fw / 2 + side * fw * 0.28, fh * 0.45, fw * 0.22, fh * 0.45, k_=1.15)
        scaled(p, (fx + fw // 2 - 3, fy, 6, fh), LIGHTS['back'], cw=3, ch=2)
    if style == 'shoulder':                                       # the round of its shoulder, scaled over its top
        skin(p, f)
        for s in S:
            fx, fy, fw, fh = f[s]
            muscle(p, f[s], fw * 0.5, fh * 0.45, fw * 0.5, fh * 0.5, k_=1.16)
            scaled(p, (fx, fy, fw, int(fh * 0.35)), LIGHTS[s], cw=4, ch=3)
        scaled(p, f['top'], 1.1, cw=4, ch=3)
    if style == 'upperarm':
        skin(p, f)
        for s in ('front', 'right', 'left', 'back'):
            fx, fy, fw, fh = f[s]
            muscle(p, f[s], fw * 0.5, fh * 0.32, fw * 0.45, fh * 0.32)
    if style == 'bicep':
        skin(p, f, fall=0.1)
        fx, fy, fw, fh = f['front']
        muscle(p, f['front'], fw * 0.5, fh * 0.45, fw * 0.6, fh * 0.55, k_=1.22, rim=0.55)
    if style == 'forearm':
        skin(p, f)
        for s in S:                                               # scales down its back, veins down its front
            fx, fy, fw, fh = f[s]
            if s in ('back', 'right', 'left'):
                scaled(p, (fx, fy, fw, int(fh * 0.4)), LIGHTS[s], cw=3, ch=2)
            else:
                for i in range(1, fw, 4):
                    for j in range(fh):
                        if (j + i) % 3:
                            p.px((fx + i + (j // 4) % 2, fy + j), k(SKIN_DEEP, 1.3))

    # ── its fins ──
    if style in ('dorsal', 'armfin', 'smallfin', 'earfin'):
        for s, r in f.items():
            if s in ('right', 'left'):
                front = 'right' if s == 'right' else 'left'
                if style == 'dorsal':
                    fin(p, r, root=front, rays=11, scallop=0.14, spine=True, taper=0.55, alpha=200)
                elif style == 'earfin':
                    fin(p, r, root=front, rays=6, fan=True, round_tip=0.3, scallop=0.16, alpha=215)
                else:
                    fin(p, r, root=front, rays=6, scallop=0.14, taper=0.4, alpha=205)
            else:
                clear_face(p, r)
        # its fins are blue, not a carp's red-gold: retint what fin() drew
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                c = p.img.getpixel((int(xy[0]), int(xy[1])))
                if c[3] == 0:
                    continue
                lum = (c[0] * 0.5 + c[1] * 0.35 + c[2] * 0.15) / 255
                col = mixc((10, 34, 104), (70, 180, 236), lum)
                if c[0] > 180 and c[1] < 80:                      # its edge
                    col = (30, 170, 200)
                p.pxa(xy, col, c[3])

    # ── its head ──
    if style in ('skull', 'nape'):
        skin(p, f, warts=0.15)
        if style == 'skull':
            fx, fy, fw, fh = f['front']                           # its forehead: furrows
            for j in (2, 5):
                for i in range(3, fw - 3):
                    if (i * 3 + j) % 7 > 1:
                        p.px((fx + i, fy + j + (abs(i - fw / 2) > fw / 4)), k(SKIN_DEEP, 1.2))
            for s in ('right', 'left'):                           # its gill slits behind the jaw
                fx, fy, fw, fh = f[s]
                for g in range(3):
                    x = fx + (fw - 4 - 2 * g if s == 'left' else 3 + 2 * g)
                    for j in range(fh - 6, fh - 1):
                        p.px((x, fy + j), k(SKIN_DEEP, 0.7))
                        p.px((x + 1, fy + j), mixc((180, 40, 60), SKIN, 0.5))
    if style == 'brow':                                           # one brow: a knotted ridge, lit along its crest, shadowing down; bristling flame
        skin(p, f, warts=0.45, fall=0.1)
        for s in ('front', 'right', 'left', 'back'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                c = p.get(xy)
                if j == 0:
                    c = mixc(c, (180, 232, 255), 0.55)            # lit along its crest
                elif j == 1:
                    c = mixc(c, SKIN_HI, 0.35)
                c = k(c, 1 - 0.45 * max(0, j - 1) / max(1, fh - 2))   # shadowing down to the eye
                if j >= fh - 1:
                    c = k(SKIN_DEEP, 0.55)
                if s == 'front' and (i * 5 + j * 3) % 9 == 0 and 0 < j < fh - 1:
                    c = k(SKIN_DEEP, 0.9)                         # its knots and creases
                p.px(xy, c)
        w.flat(p, f, k(SKIN_DEEP, 0.3), faces=('bottom',))
        for i, j, xy in w.each(p, f['top']):                      # bristling brows of flame along its crest
            if p.rnd.random() < 0.6:
                p.px(xy, mixc(FLAME, FLAME_ROOT, p.rnd.random() * 0.8))
            if p.rnd.random() < 0.12:
                p.lit(xy, FLAME_TIP, 0.5)
    if style == 'browknot':                                       # the knot between the brows: the scowl's furrows plunging to the nose
        skin(p, f, warts=0.3, fall=0.1)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            c = p.get(xy)
            if i in (fw // 2 - 1, fw // 2):
                c = k(SKIN_DEEP, 0.6 + 0.1 * (j % 2))             # the deep cleft down its middle
            elif i in (1, fw - 2) and j > 0:
                c = k(SKIN_DEEP, 0.85)                            # a furrow each side
            elif j == 0 or i in (2, fw - 3):
                c = mixc(c, (180, 232, 255), 0.45)                # the bulges of flesh between, lit
            if j >= fh - 1:
                c = k(c, 0.7)
            p.px(xy, c)
        w.flat(p, f, k(SKIN_DEEP, 0.4), faces=('bottom',))
    if style == 'browshelf':                                      # the brow's shelf over the sockets: heavy, dark beneath
        skin(p, f, warts=0.3, fall=0.3)
        w.flat(p, f, k(SKIN_DEEP, 0.3), faces=('bottom',))
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            p.px(xy, k(p.get(xy), 0.8 - 0.25 * j / max(1, fh - 1)))
    if style == 'forehead':                                       # the sloping forehead: furrowed, lit at its swell
        skin(p, f, warts=0.2, fall=0.1)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            c = p.get(xy)
            d = abs(i + 0.5 - fw / 2) / (fw / 2)
            if j % 2 == 1 and 0.2 < d < 0.85 and (i + j) % 5:
                c = k(SKIN_DEEP, 1.15)                            # its furrows
            elif d < 0.5:
                c = mixc(c, SKIN_HI, 0.25 * (1 - d / 0.5))
            p.px(xy, c)
    if style == 'socket':                                         # the hollows the eyes are sunk in: deep shadow
        w.flat(p, f, k(SKIN_DEEP, 0.45), noise=2)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy), k(SKIN_DEEP, 0.3))
    if style == 'eye':                                            # a small burning eye, almond-shaped: gold, a hot core, a black slit, red-rimmed
        w.flat(p, f, k(SKIN_DEEP, 0.4), noise=1)
        fx, fy, fw, fh = f['front']                               # 6x4 painted, 5x3 seen: its outer corner at i=0, its inner at the right
        vw, vh = max(3, int(fw * 0.95)), max(2, int(fh * 0.8))
        for i in range(vw):
            for j in range(vh):
                u = (i + 0.5) / vw                                 # 0 outer .. 1 inner
                v = (j + 0.5) / vh                                 # 0 top .. 1 bottom
                lid = 0.2 + 0.4 * u                                # the upper lid bearing down toward the nose: a glare
                low = 1.0 - 0.35 * abs(u - 0.45) * 2              # the lower lid, rounding up to each corner
                if v < lid - 0.2 or v > low or (u < 0.1 and v < 0.5) or (u > 0.88 and v < 0.7):
                    p.px((fx + i, fy + j), k(SKIN_DEEP, 0.5))     # the lids, in shadow
                    continue
                d = abs(u - 0.45) * 2
                c = mixc((255, 246, 190), EYE, min(1, d * 1.4))
                if d > 0.75:
                    c = (232, 84, 30)                             # red at its corners
                p.lit((fx + i, fy + j), c, 1.0)
        pc = int(vw * 0.45)
        for j in range(vh):                                       # a black slit of a pupil
            if p.glow.getpixel((fx + pc, fy + j))[3]:
                p.px((fx + pc, fy + j), (14, 8, 6))
                p.glow.putpixel((fx + pc, fy + j), (0, 0, 0, 0))
    if style == 'muzzle':                                         # the heavy muzzle: flesh thrust forward, lit across its swell, creased by the snarl
        skin(p, f, warts=0.25, fall=0.15)
        fx, fy, fw, fh = f['top']                                 # its top, back to front: lit across its crown
        for i, j, xy in w.each(p, f['top']):
            d = abs(i + 0.5 - fw / 2) / (fw / 2)
            p.px(xy, mixc(p.get(xy), SKIN_HI, 0.5 * (1 - d)))
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            d = abs(i + 0.5 - fw / 2) / (fw / 2)
            c = mixc(p.get(xy), SKIN_HI, 0.35 * (1 - abs(d - 0.55) / 0.45)) if d > 0.1 else p.get(xy)
            if d > 0.88:
                c = k(c, 0.7)
            p.px(xy, c)
        for s in ('right', 'left'):                               # its sides: the snarl's creases fanning back from the lip
            fx, fy, fw, fh = f[s]
            for g in range(3):
                for t in range(fw):
                    x = t if s == 'right' else fw - 1 - t          # from its front corner back
                    y = fh - 2 - g * 1.4 - t * 0.35
                    if 0 <= y < fh and t < fw - 1 - g:
                        p.px((fx + x, fy + y), k(SKIN_DEEP, 0.8))
                        if y - 1 >= 0:
                            p.px((fx + x, fy + y - 1), mixc(p.get((fx + x, fy + y - 1)), SKIN_HI, 0.4))
        w.flat(p, f, k(SKIN_DEEP, 0.5), faces=('bottom',))
    if style == 'bridge':                                         # the nose's bridge between the eyes, from the knot of the brow: lit down its crest
        skin(p, f, warts=0.1, fall=0.1)
        for s in ('front', 'top'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                d = abs(i + 0.5 - fw / 2) / (fw / 2)
                p.px(xy, mixc(p.get(xy), (180, 232, 255), 0.5 * max(0, 1 - d * 1.4)) if d < 0.7 else k(p.get(xy), 0.75))
    if style == 'cheeks':                                         # the flesh of its face: shadowed under the eyes, creased by the snarl
        skin(p, f, warts=0.2)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            if j < 1:
                p.px(xy, k(p.get(xy), 0.78))                      # the shadow under the eyes
            elif j >= fh - 2:
                p.px(xy, k(p.get(xy), 0.85))
        for side in (-1, 1):                                      # the swell of each cheek, lit at its crown
            muscle(p, f['front'], fw / 2 + side * fw * 0.3, fh * 0.45, fw * 0.2, fh * 0.5, k_=1.6, rim=0.7)
        for j in range(fh):                                       # the snarl's creases from the nose
            for x in (fw // 2 - 4 - j // 2, fw // 2 + 3 + j // 2):
                p.px((fx + x, fy + j), k(SKIN_DEEP, 0.75))
                p.px((fx + x + (1 if x > fw // 2 else -1), fy + j), k(SKIN, 1.12))
        w.flat(p, f, k(SKIN_DEEP, 0.7), faces=('top',))
    if style == 'cheekbone':                                      # a jutting cheekbone: lit along its crest, shadowed under
        skin(p, f, fall=0.4)
        for s in ('front', 'top', 'right', 'left'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if s == 'top' or j == 0:
                    p.px(xy, mixc(p.get(xy), (190, 236, 255), 0.75))
                elif j < fh - 1:
                    p.px(xy, mixc(p.get(xy), SKIN_HI, 0.5))
                elif j >= fh - 1:
                    p.px(xy, k(SKIN_DEEP, 0.9))
        w.flat(p, f, k(SKIN_DEEP, 0.55), faces=('bottom',))
    if style == 'nose':                                           # broad and flat: a lit bridge, a bulbous tip, shadowed below
        skin(p, f, warts=0.15, fall=0.1)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            d = abs(i + 0.5 - fw / 2) / (fw / 2)
            c = mixc(p.get(xy), SKIN_HI, 0.4)
            if d < 0.35 and j < fh - 2:
                c = mixc(c, (190, 236, 255), 0.6 * (1 - d / 0.35))      # the bridge, lit
            if d > 0.75:
                c = k(c, 0.8)
            if j >= fh - 1:
                c = k(c, 0.75)
            p.px(xy, c)
        for i in range(fw // 2 - 1, fw // 2 + 1):                 # a lit tip
            p.px((fx + i, fy + fh - 3), mixc(SKIN_HI, (200, 236, 255), 0.3))
        fx, fy, fw, fh = f['bottom']                              # its nostrils, from beneath: two dark holes, a septum between
        for i, j, xy in w.each(p, f['bottom']):
            p.px(xy, k(SKIN_DEEP, 0.8))
        for i in list(range(0, 3)) + list(range(fw - 3, fw)):
            for j in range(fh):
                p.px((fx + i, fy + j), (8, 4, 16))
    if style == 'nostril':                                        # a flared nostril's wing: a round of flesh, its hole dark beneath and within
        skin(p, f, fall=0.2)
        for s in ('front', 'top'):
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), mixc(p.get((fx + i, fy)), SKIN_HI, 0.4))
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), k(SKIN_DEEP, 0.6))
        w.flat(p, f, (8, 4, 16), faces=('bottom',))
        for s in ('right', 'left'):                               # the inner side, toward the nose: the hole's dark rim
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if j >= fh // 2:
                    p.px(xy, (14, 6, 24))
    if style == 'upperlip':                                       # its upper lip: indigo flesh, drawn up in a snarl, dark-lipped, the gum bared
        skin(p, f, warts=0.1, fall=0.1)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            d = abs(i + 0.5 - fw / 2) / (fw / 2)
            c = p.get(xy)
            if j < fh - 2 and 0.15 < d < 0.8:
                c = mixc(c, SKIN_HI, 0.55 * (1 - abs(d - 0.45) / 0.35))  # the muzzle's swell either side
            if d < 0.1 and j < fh - 2:
                c = k(c, 0.78)                                    # the groove under the nose
            if j == fh - 2:
                c = mixc(LIP, SKIN_DEEP, 0.3) if d < 0.85 else k(SKIN_DEEP, 0.9)
            elif j == fh - 1:
                c = k(GUM, 0.9 + 0.2 * (1 - d)) if d < 0.85 else LIP   # the gum bared under the lip
            p.px(xy, noisy(p, c, 3))
        for s in ('right', 'left'):                               # the mouth's corners: the lips meeting, dark
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if j >= fh - 2:
                    p.px(xy, LIP)
        fx, fy, fw, fh = f['bottom']                              # its palate, seen as its jaws gape: gum at the front, dark behind
        for i, j, xy in w.each(p, f['bottom']):
            t = j / max(1, fh - 1)                                # 0 behind .. 1 at its lip (the bottom face runs back to front)
            p.px(xy, noisy(p, mixc(THROAT, GUM, t ** 1.5), 3))
    if style == 'maw':                                            # the inside of its mouth: deep, dark into the throat
        for s, (fx, fy, fw, fh) in f.items():
            for i, j, xy in w.each(p, (fx, fy, fw, fh)):
                c = THROAT
                if s == 'front':
                    d = abs(i + 0.5 - fw / 2) / (fw / 2)
                    c = mixc(THROAT, (70, 12, 30), 0.6 * d)       # darkest at its middle: the throat
                    if j == 0:
                        c = k(GUM, 0.7)
                p.px(xy, noisy(p, c, 2))
    if style in ('fang', 'fangtip'):                              # a great fang: off-white, shaded, stained at the gum, narrowing to a point
        for s, (fx, fy, fw, fh) in f.items():
            for i, j, xy in w.each(p, (fx, fy, fw, fh)):
                t = j / max(1, fh - 1)                            # 0 its root .. 1 its point
                c = mixc(TOOTH_ROOT, TOOTH, min(1, t * 3)) if style == 'fang' else mixc(TOOTH, (244, 238, 220), t)
                if s in ('right', 'left', 'back'):
                    c = mixc(c, TOOTH_SHADE, 0.45)
                elif s == 'front' and i == fw - 1:
                    c = mixc(c, TOOTH_SHADE, 0.5)                 # its edge, rounding away
                elif s == 'front' and i == 0 and 0.3 < t < 0.8:
                    c = mixc(c, (250, 244, 226), 0.5)             # a glint down it
                p.px(xy, noisy(p, k(c, LIGHTS[s]), 3))
            if s in S and style == 'fangtip':
                w.point(p, (fx, fy, fw, fh), down=True, keep=0.25, sharp=0.55)
    if style == 'jaw':                                            # its lower jaw: indigo flesh, a dark lower lip, the mouth dark inside
        skin(p, f, warts=0.2)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            d = abs(i + 0.5 - fw / 2) / (fw / 2)
            p.px((fx + i, fy), k(GUM, 0.85) if d < 0.75 else LIP)
            p.px((fx + i, fy + 1), LIP if d < 0.85 else k(SKIN_DEEP, 0.9))
            p.px((fx + i, fy + 2), k(p.get((fx + i, fy + 2)), 0.8))
        for s in ('right', 'left'):
            sx, sy, sw, sh = f[s]
            for i in range(sw):
                p.px((sx + i, sy), LIP)
        fx, fy, fw, fh = f['top']                                 # inside: the gum along its front and sides, the tongue, the throat behind
        for i, j, xy in w.each(p, f['top']):
            back = 1 - j / max(1, fh - 1)                         # the top face's first row is its back
            edge = min(i, fw - 1 - i, fh - 1 - j)
            c = mixc(THROAT, (60, 10, 24), 0.4 * (1 - back))
            if edge < 1:
                c = GUM
            elif 2 < i < fw - 3 and 1 < j < fh - 2:
                d = abs(i + 0.5 - fw / 2) / (fw / 2 - 2)
                c = mixc(k(TONGUE, 0.55 + 0.45 * (1 - back)), THROAT, d * 0.6)   # its tongue, lit at its tip
                if abs(i + 0.5 - fw / 2) < 0.6:
                    c = k(c, 0.7)
            p.px(xy, noisy(p, c, 3))
    if style == 'chin':
        skin(p, f, warts=0.2)
    if style == 'flamebeard':                                     # a beard of flame hanging under its jaw
        for s, r in f.items():
            if s in ('top', 'bottom'):
                clear_face(p, r)
            else:
                flame_face(p, r, up=False, tongues=3, seed=3, keep=0.25)
    if style in ('tusk', 'tusktip'):                              # tusks: yellowed off-white, stained at the root, shaded round, lit down one edge
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                t = 1 - j / max(1, fh - 1)                        # 0 its root .. 1 its point
                if style == 'tusk':
                    c = mixc(TOOTH_ROOT, TOOTH, min(1, t * 3))
                else:
                    c = mixc(TOOTH, (240, 232, 210), t)
                if s in ('back', 'left'):
                    c = mixc(c, TOOTH_SHADE, 0.45)
                elif s in ('front', 'right') and i == 0:
                    c = mixc(c, (248, 242, 224), 0.4)
                p.px(xy, noisy(p, k(c, w.LIGHT[s]), 3))
        if style == 'tusktip':
            for s in S:
                w.point(p, f[s], down=False, keep=0.25, sharp=0.5)
    if style == 'eyeball':                                        # a bulging eye: burning gold, ringed red, a black pupil, a glint
        skin(p, f)
        fx, fy, fw, fh = f['front']
        cx, cy = fx + fw / 2, fy + fh / 2
        for i, j, xy in w.each(p, f['front']):
            d = math.hypot((i + 0.5 - fw / 2) / (fw / 2), (j + 0.5 - fh / 2) / (fh / 2))
            if d > 1.05:
                continue
            c = mixc((255, 250, 200), EYE, d) if d < 0.75 else (230, 70, 30)
            p.lit(xy, c, 1.0)
        for j in range(1, fh - 1):
            for i in (fw // 2 - 1, fw // 2):
                p.px((fx + i, fy + j), (14, 8, 6))
                p.glow.putpixel((fx + i, fy + j), (0, 0, 0, 0))
        p.lit((fx + fw // 2 - 2, fy + 1), (255, 255, 255))
        for s in ('right', 'left', 'top', 'bottom'):              # the bulge round it: red-rimmed, burning
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if s == 'top':
                    p.px(xy, k(SKIN_DEEP, 0.8))
                elif (s in ('right', 'left') and ((i < 2) if s == 'right' else (i >= fw - 2))) or (s == 'bottom' and j < 2):
                    pass
                else:
                    p.lit(xy, (220, 60, 30), 0.6)
    if style == 'hoop':
        gold_box(p, f)
    if style in ('horn1', 'horn2', 'horn3'):                      # horns of dark horn, ridged, paling to their points
        base = {'horn1': HORN_DEEP, 'horn2': mixc(HORN_DEEP, HORN, 0.5), 'horn3': HORN}[style]
        tip = {'horn1': mixc(HORN_DEEP, HORN, 0.5), 'horn2': HORN, 'horn3': (250, 244, 224)}[style]
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                t = 1 - j / max(1, fh - 1)
                c = mixc(base, tip, t)
                if j % 3 == 0:
                    c = k(c, 0.75)
                p.px(xy, k(c, w.LIGHT[s]))
        if style == 'horn3':
            for s in S:
                w.point(p, f[s], down=False, keep=0.3, sharp=0.4)
    if style == 'flame':                                          # a lock of its hair, standing up off its head like flame
        for s, r in f.items():
            if s == 'top':
                clear_face(p, r)
            elif s == 'bottom':
                p.face(r, FLAME_ROOT, 0.8, noise=6)
            else:
                flame_face(p, r, up=True, tongues=2 if s in ('right', 'left') else 3, seed={'front': 1, 'back': 3, 'right': 5, 'left': 2}[s])

    # ── its ornaments ──
    if style == 'pearl':                                          # a pearl: lustrous, a pink-blue sheen, a glint
        for s, (fx, fy, fw, fh) in f.items():
            for i, j, xy in w.each(p, (fx, fy, fw, fh)):
                d = math.hypot((i + 0.5) / fw - 0.35, (j + 0.5) / fh - 0.35)
                c = mixc(PEARL, (190, 196, 230), min(1, d * 1.5))
                if d > 0.6:
                    c = mixc(c, (230, 200, 214), 0.4)
                p.lit(xy, k(c, LIGHTS[s]), 0.25)
    if style == 'nskull':                                         # a little skull: sockets dark, a grin
        for s in f:
            for i, j, xy in w.each(p, f[s]):
                p.px(xy, noisy(p, k(BONE, w.LIGHT[s]), 4))
        fx, fy, fw, fh = f['front']
        for i, j in ((0, 1), (fw - 1, 1), (1, 1), (fw - 2, 1)):
            p.px((fx + i, fy + j), (30, 20, 30))
        p.px((fx + fw // 2, fy + 2), (120, 110, 90))
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), (40, 30, 30) if i % 2 else BONE)
    if style == 'gbelt':                                          # a gold belt: plaques, red-enamelled studs between them
        gold_box(p, f)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), (255, 240, 170))
                p.px((fx + i, fy + fh - 1), GOLD_DEEP)
                if i % 5 == 0:
                    for j in range(1, fh - 1):
                        p.px((fx + i, fy + j), GOLD_DEEP)
                if i % 5 == 2 and fh > 2:
                    p.px((fx + i, fy + fh // 2), RED)
    if style == 'beltface':                                       # its buckle: a gold demon's face, a ring in its jaws
        gold_box(p, f)
        fx, fy, fw, fh = f['front']
        for i in (1, fw - 2):
            p.lit((fx + i, fy + 2), (255, 80, 40), 0.6)
        for i in range(1, fw - 1):
            p.px((fx + i, fy + 1), GOLD_DEEP)
        for i in range(fw // 2 - 1, fw // 2 + 1):
            p.px((fx + i, fy + 3), GOLD_DEEP)
        circle(p, fx + fw / 2, fy + fh - 1.8, 0.9, 1.7, (255, 236, 150))
        for i in range(2, fw - 2):
            p.px((fx + i, fy + fh - 3), (60, 20, 10) if i % 2 else (250, 240, 210))
    if style == 'apron':                                          # a red apron, a gold wave at its hem
        fish.paint_fish(p, 'apron', f, dims)
    if style in ('wrapfront', 'wrapside', 'wrapback'):            # its wrap: sea-green fish-scale hide, gold-edged, ragged
        for s, r in f.items():
            w.scale_tiles(p, r, k(WRAP, LIGHTS[s]), k(WRAP_EDGE, LIGHTS[s]), cw=4, ch=3, hi=k((150, 230, 190), LIGHTS[s]), seed=9)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), GOLD)
                p.px((fx + i, fy + 1), GOLD_DEEP)
            if fw > 3:
                for j in range(fh):
                    p.px((fx, fy + j), GOLD_DEEP)
                    p.px((fx + fw - 1, fy + j), GOLD_DEEP)
            ragged(p, f[s], teeth=3, depth=3)
    if style in ('scarf', 'scarftail', 'scarfback'):              # red silk, sheened, a gold edge and gold cloud dots
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    sheen = 0.5 + 0.5 * math.sin((i + j * 0.35) * 0.9)
                    c = mixc(SILK_DEEP, SILK, 0.4 + 0.6 * sheen)
                    if sheen > 0.92:
                        c = mixc(c, (255, 150, 140), 0.35)
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))
            if s in ('front', 'back'):
                if style == 'scarfback':
                    for i in range(fw):
                        p.px((fx + i, fy), GOLD)
                        p.px((fx + i, fy + fh - 1), GOLD)
                else:
                    for j in range(fh):
                        p.px((fx, fy + j), GOLD)
                        p.px((fx + fw - 1, fy + j), GOLD)
                    for j in range(3, fh - 2, 5):
                        p.px((fx + fw // 2, fy + j), (255, 226, 130))
                if style == 'scarftail':
                    w.point(p, (fx, fy, fw, fh), down=True, keep=0.8, sharp=0.6)
                    ragged(p, (fx, fy, fw, fh), teeth=2, depth=2)

    # ── its trident ──
    if style in ('socket',):                                      # the gold demon-mouth socket (吞口): a face, its jaws on the prongs
        gold_box(p, f)
        for s in S:
            fx, fy, fw, fh = f[s]
            p.lit((fx + 1, fy + 2), (255, 80, 40), 0.5)
            p.lit((fx + fw - 2, fy + 2), (255, 80, 40), 0.5)
            for i in range(fw):
                p.px((fx + i, fy), (60, 24, 10) if i % 2 else (255, 240, 180))
            for i in range(1, fw - 1):
                p.px((fx + i, fy + fh - 2), GOLD_DEEP)
    if style in ('crossbar', 'prong', 'sideprong', 'barb'):       # steel, bright along its edge
        shrimp.steel_box(p, f, STEEL)
        for s in S:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                p.px((fx, fy + j), (250, 252, 255))
        if style == 'barb':
            for s in ('front', 'back', 'top', 'bottom'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    for j in range(fh):
                        if (s in ('front', 'top', 'bottom') and i < fw * 0.35 and abs(j + 0.5 - fh / 2) > fh * 0.25):
                            p.clear((fx + i, fy + j))
    if style in ('prongtip', 'sidetip'):                          # a leaf point
        shrimp.steel_box(p, f, STEEL)
        for s in S:
            fx, fy, fw, fh = f[s]
            w.point(p, (fx, fy, fw, fh), down=False, keep=0.3, sharp=0.4 if style == 'prongtip' else 0.5)
            for j in range(fh):
                p.px((fx + fw // 2, fy + j), (252, 254, 255))


if __name__ == '__main__':
    p = Painter(41)
    slots = paint(YAKSHA, p, paint_yaksha)
    p.save('yaksha')
    emit(YAKSHA, slots, JAVA)
    w.report('yaksha', YAKSHA, SIZE, slots)
    low = [q for q in YAKSHA if q[0] not in ('trident', 'pennant', 'right_barb', 'left_barb') and 'hair' not in q[0] and 'horn' not in q[0]]
    w.report('   to its crown, without trident, hair and horns', low, SIZE, slots)
