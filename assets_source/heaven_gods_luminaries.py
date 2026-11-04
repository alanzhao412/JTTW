"""The Nine Luminaries (九曜星君), the star lords of the sun, the moon, the five planets and the two shadow
stars: one frame for the nine (as hell_kings.py's nine kings), each skin in his star's colours, his own
head-dress and his star's emblem shown and the rest hidden by the renderer (HeavenGodRenderers.StarLordModel).

The frame: a star lord's court robe in three flaring tiers over court boots, a jade belt hung with jade
pendants, an apron and side panels; a four-lobed cloud collar (云肩) on the shoulders; great hanging
sleeves; a celestial's long ribbon (披帛) looping in an arch behind the shoulders and streaming down either
side; gold stars and constellations scattered on the silk, the sea-and-cliff hem at its foot. An ivory
court tablet in the right hand (a brush for Mercury, none for the fierce three); the left hand raised,
holding up the star's emblem, which shines.

    0 Sun      red-gold     a sunburst crown; the solar disc ringed in flame, the three-legged crow in it
    1 Moon     silver-white a lady, her hair high, a crescent over it; the moon's disc, the hare pounding elixir
    2 Jupiter  green        Wood: a lotus crown of jade petals; a branching staff in leaf, a jade pearl at its top
    3 Mars     red          Fire: red-faced, three-eyed, glaring, a crown of flames; a flaming sword
    4 Saturn   ochre        Earth: an elder, his great bald brow, white beard and drooping brows; a dragon-head staff, a gourd
    5 Venus    white-gold   Metal: a lady, a phoenix-cock on her crown; a pipa
    6 Mercury  deep blue    Water: a scholar's black cap with long wings; a brush and an unrolled scroll
    7 Rahu     dark         a demon's dark face, fangs, wild flame-red hair; the eclipse, a black disc in a corona of fire
    8 Ketu     violet       a demon's violet face, fangs, hair streaming back like a comet's tail; a comet

Run from the repository root: python assets_source/heaven_gods.py"""
import math
import random

from fire_court import part, mirrored
from sea_court import aim
from king_yama import k, mixc, noisy, point, circle, lock, BLUE_BLACK, BROWN_BLACK, ASH
from heaven_gods_kit import *

# How much larger than his pixels he is drawn (HeavenGodRenderers.StarLordRenderer's size)
SIZE = 0.7

ARM, FOREARM = (-0.08, 0, 0.16), (-1.05, -0.5, 0)
LARM = (-0.1, 0, -0.4)
LFOREARM = (-2.2, -0.35, 0)
HELD = aim([LARM, LFOREARM], (0, -0.2, 0))
RHELD = aim([ARM, FOREARM], (0.15, 0, -0.1))

# name, robe, deep, under (lining, apron, ribbon), face, hair tone, kind, iris, glow (the emblem's light)
LUMINARIES = [
    ('sun', (204, 58, 30), (118, 24, 14), (246, 196, 70), (232, 178, 132), BLUE_BLACK, 'man', (184, 112, 30), (255, 190, 70)),
    ('moon', (230, 234, 244), (148, 160, 190), (128, 160, 214), (246, 228, 220), BLUE_BLACK, 'lady', (104, 126, 178), (226, 240, 255)),
    ('jupiter', (46, 140, 78), (18, 70, 40), (236, 206, 112), (226, 190, 152), BROWN_BLACK, 'man', (58, 132, 76), (130, 250, 170)),
    ('mars', (206, 34, 24), (112, 10, 8), (250, 170, 40), (200, 74, 54), RED_HAIR, 'fierce', (255, 220, 80), (255, 140, 40)),
    ('saturn', (176, 126, 50), (98, 64, 22), (110, 70, 40), (230, 196, 164), WHITE_HAIR, 'elder', (40, 26, 18), (255, 214, 90)),
    ('venus', (244, 240, 228), (190, 172, 132), (226, 186, 80), (248, 230, 218), BLUE_BLACK, 'lady', (156, 104, 54), (255, 238, 160)),
    ('mercury', (28, 36, 70), (8, 10, 26), (70, 130, 206), (222, 192, 160), BLUE_BLACK, 'man', (60, 96, 176), (120, 190, 255)),
    ('rahu', (44, 38, 54), (14, 12, 18), (160, 34, 30), (78, 84, 116), RED_HAIR, 'demon', (255, 120, 40), (255, 150, 60)),
    ('ketu', (92, 44, 132), (40, 16, 66), (206, 176, 236), (120, 92, 150), VIOLET_HAIR, 'demon', (130, 50, 230), (210, 160, 255)),
]
NAMES = [l[0] for l in LUMINARIES]


def held(name, boxes, pivot=(0.4, 1.6, -1.2), rot=None, children=(), grand=1.3):
    """An emblem in his left fist (laid out about his grip, upright, its face ahead), drawn `grand` larger."""
    g = lambda b: tuple(v * grand for v in b[:6]) + (b[6],)
    out = [part('emblem_' + name, 'left_hand', pivot, rot or HELD, [g(b) for b in boxes])]
    for n, parent, pv, r, bx, m in children:
        out.append((n, parent, tuple(v * grand for v in pv), r, [g(b) for b in bx], m))
    return out


def arc(prefix, parent, cx, cy, z, R, a0, a1, n, width, depth, style, rot_extra=0.0):
    """Segments laid round an arc (angles in degrees, y down), each a part of its own."""
    out = []
    for i in range(n):
        t0 = math.radians(a0 + (a1 - a0) * i / n)
        t1 = math.radians(a0 + (a1 - a0) * (i + 1) / n)
        tm = (t0 + t1) / 2
        length = 2 * R * math.sin(abs(t1 - t0) / 2) + 0.6
        out.append(part('%s_%d' % (prefix, i), parent, (cx + R * math.cos(tm), cy + R * math.sin(tm), z),
                        (0, 0, tm + math.pi / 2), [(-length / 2, -width / 2, -depth / 2, length, width, depth, style)]))
    return out


def emblems():
    out = []
    # 0 the sun: a round disc ringed in flame on a cloud bracket
    out += held('sun', [
        (-0.7, -3.4, -0.7, 1.4, 4.4, 1.4, 'su_handle'),
        (-2.6, -4.8, -1, 5.2, 1.6, 2, 'su_bracket'),
        (-6.5, -17.6, -0.6, 13, 13, 1.2, 'su_disc'),
    ])
    # 1 the moon: a pale disc on a cloud bracket
    out += held('moon', [
        (-0.7, -3.4, -0.7, 1.4, 4.4, 1.4, 'mo_handle'),
        (-2.6, -4.8, -1, 5.2, 1.6, 2, 'mo_bracket'),
        (-5.8, -16.4, -0.6, 11.6, 11.6, 1.2, 'mo_disc'),
    ])
    # 2 Wood: a staff in leaf, branching, a jade pearl at its top
    out += held('jupiter', [
        (-0.7, -15.5, -0.7, 1.4, 21, 1.4, 'ju_staff'),
        (-1.6, -18.2, -1.6, 3.2, 3, 3.2, 'ju_leaves'),
        (-1.2, -20.4, -1.2, 2.4, 2.4, 2.4, 'ju_pearl'),
    ], children=[
        part('emblem_jupiter_b1', 'emblem_jupiter', (0, -10, 0), (0, 0, -0.75), [
            (-0.45, -5.5, -0.45, 0.9, 5.8, 0.9, 'ju_twig'), (-1.5, -8, -1.5, 3, 2.8, 3, 'ju_leaves')]),
        part('emblem_jupiter_b2', 'emblem_jupiter', (0, -13, 0), (0, 0.5, 0.7), [
            (-0.45, -4.5, -0.45, 0.9, 4.8, 0.9, 'ju_twig'), (-1.3, -6.6, -1.3, 2.6, 2.4, 2.6, 'ju_leaves')]),
        part('emblem_jupiter_b3', 'emblem_jupiter', (0, -6, 0), (0.4, 0, 0.55), [
            (-0.4, -3.5, -0.4, 0.8, 3.8, 0.8, 'ju_twig'), (-1.1, -5.4, -1.1, 2.2, 2.2, 2.2, 'ju_leaves')]),
    ])
    # 3 Fire: a straight sword, flames running up its blade
    out += held('mars', [
        (-0.6, -1, -0.6, 1.2, 4, 1.2, 'ma_hilt'),
        (-0.9, 3, -0.9, 1.8, 1.4, 1.8, 'ma_pommel'),
        (-2.6, -2.4, -0.9, 5.2, 1.4, 1.8, 'ma_guard'),
        (-0.8, -17.6, -0.3, 1.6, 15.4, 0.6, 'ma_blade'),
        (-2.8, -19.4, -0.1, 5.6, 16, 0.2, 'ma_flame'),
        (-0.1, -19.4, -2.8, 0.2, 16, 5.6, 'ma_flame_x'),
    ])
    # 4 Earth: a long staff, a dragon's head at its top holding a pearl, a gourd hung from it
    out += held('saturn', [
        (-0.8, -24, -0.8, 1.6, 41, 1.6, 'sa_staff'),
        (-1.5, -28, -2.8, 3, 4.4, 4.6, 'sa_head'),
        (-1.1, -26.4, -4.6, 2.2, 2.2, 2.2, 'sa_pearl'),
        (-1.0, -30, 0.6, 2, 2.6, 1.4, 'sa_horn'),
    ], grand=1.0, children=[
        part('emblem_saturn_gourd', 'emblem_saturn', (1.2, -23.5, 0), (0, 0, -0.1), [
            (-0.25, 0, -0.25, 0.5, 2.2, 0.5, 'sa_cord'),
            (-1, 2, -1, 2, 2, 2, 'sa_gourd_hi'),
            (-1.6, 3.8, -1.6, 3.2, 3.4, 3.2, 'sa_gourd_lo'),
        ]),
    ])
    # 5 Metal: a pipa, held up by its neck
    out += held('venus', [
        (-0.7, -4.5, -0.6, 1.4, 6, 1.2, 'v_neck'),
        (-0.8, -8.4, -0.3, 1.6, 4.2, 1.2, 'v_head'),
        (-4.2, 1.2, -1.0, 8.4, 11, 2.0, 'v_body'),
        (-3.2, 0.2, -0.8, 6.4, 1.4, 1.6, 'v_shoulder'),
    ], grand=1.5, children=[
        part('emblem_venus_pegs', 'emblem_venus', (0, -7, 0), (0, 0, 0), [(-1.8, -0.3, 0, 3.6, 0.6, 0.6, 'v_peg'),
                                                                       (-1.8, 1.4, 0, 3.6, 0.6, 0.6, 'v_peg')]),
    ])
    # 6 Water: a hanging scroll, unrolled, its writing shining (the brush is in his right hand)
    out += held('mercury', [
        (-5.2, -0.8, -0.8, 10.4, 1.6, 1.6, 'me_roller'),
        (-4.5, 0.8, -0.15, 9, 12, 0.3, 'me_sheet'),
        (-5.2, 12.6, -0.8, 10.4, 1.6, 1.6, 'me_roller'),
    ], pivot=(0.4, 2.2, -1.2), grand=1.15)
    # 7 Rahu: the eclipse, a black disc in a corona of fire
    out += held('rahu', [
        (-0.7, -3.4, -0.7, 1.4, 4.4, 1.4, 'ra_handle'),
        (-2.6, -4.8, -1, 5.2, 1.6, 2, 'ra_bracket'),
        (-7, -18.4, -0.6, 14, 14, 1.2, 'ra_disc'),
    ])
    # 8 Ketu: a comet over his palm, a white-hot head in a great violet coma, a four-pointed flare about it;
    # its tail streaming up and back over his shoulder in a long curve, a broad violet dust tail widening
    # and fraying into sparks, and a straighter blue-white ion tail splitting from it
    out += held('ketu', [
        (-1.9, -10.4, -1.9, 3.8, 3.8, 3.8, 'ke_head'),
        (-7.5, -15.8, -0.1, 15, 15, 0.2, 'ke_star'),               # the coma and its flare, from the front
        (-0.1, -15.8, -7.5, 0.2, 15, 15, 'ke_star_x'),             # .. from the side
        (-5.5, -8.6, -5.5, 11, 0.2, 11, 'ke_star_y'),              # .. from above
    ], pivot=(0.4, 0.4, -1.2), children=comet_tail())
    return out


def comet_tail():
    """The comet's tails, rooted in its head (at the emblem's (0, -8.5, 0)): each a chain of segments laid
    along +z, crossed planes widening as they go, curving up and back."""
    out = []
    # the dust tail: broad, violet, curving, fraying into sparks at its end
    # (a turn about x of +a lifts a segment laid along +z: the first rises steeply, the rest bend back)
    segs = [(5.5, 7.0, 0.8), (7.5, 8.0, -0.22), (9.5, 8.5, -0.22), (11.0, 9.0, -0.2), (12.5, 9.0, -0.16)]
    parent, at = 'emblem_ketu', (0, -8.5, 1.0)
    for n, (w, ln, turn) in enumerate(segs):
        name = 'emblem_ketu_tail_%d' % n
        tag = 'ke_tail%d' % n
        boxes = [(-w / 2, -0.1, 0, w, 0.2, ln, tag), (-0.1, -w / 2, 0, 0.2, w, ln, tag + '_x')]
        if n < 2:
            boxes.append((-1.3 + 0.4 * n, -1.3 + 0.4 * n, 0, 2.6 - 0.8 * n, 2.6 - 0.8 * n, ln, 'ke_trail%d' % n))
        out.append(part(name, parent, at, (turn, 0.08 if n == 0 else 0.03, 0), boxes))
        parent, at = name, (0, 0, ln - 0.4)
    # the ion tail: narrow, straight, blue-white, splitting off the head steeper and outward
    parent, at = 'emblem_ketu', (0, -9.0, 1.0)
    for n, (w, ln, turn) in enumerate([(3.4, 9.0, 0.95), (5.0, 9.0, -0.12), (6.4, 8.0, -0.12)]):
        name = 'emblem_ketu_ion_%d' % n
        tag = 'ke_ion%d' % n
        out.append(part(name, parent, at, (turn, 0.5 if n == 0 else 0.06, 0), [
            (-w / 2, -0.1, 0, w, 0.2, ln, tag), (-0.1, -w / 2, 0, 0.2, w, ln, tag + '_x')]))
        parent, at = name, (0, 0, ln - 0.4)
    return out


def headdresses():
    out = []
    H = (0, -9, 0)
    # 0 the sun: a gold crown, a shining disc on its brow, rays fanned behind it
    out.append(part('hd_sun', 'head', H, (0, 0, 0), [
        (-3.4, -3.6, -3.2, 6.8, 3.8, 6.4, 'sun_cap'),
        (-4.7, -0.9, -4.3, 9.4, 1.4, 8.6, 'sun_band'),
        (-2.4, -6.8, -4.6, 4.8, 4.8, 0.8, 'sun_crest'),
        (-6, -3.2, -0.35, 12, 0.7, 0.7, 'sun_pin'),
    ]))
    for i, a in enumerate((-75, -50, -25, 0, 25, 50, 75)):
        out.append(part('hd_sun_ray_%d' % i, 'hd_sun', (0, -3.6, -3.4), (0, 0, math.radians(a)), [
            (-0.7, -9.6 + abs(a) / 40, -0.3, 1.4, 6.2 - abs(a) / 40, 0.6, 'sun_ray')]))
    # 1 the moon: her hair high in a bun, a loop either side, a silver crescent over it, pins hung with pearls
    out.append(part('hd_moon', 'head', H, (0, 0, 0), [
        (-2.4, -5.2, -1.4, 4.8, 5.6, 4.6, 'moon_bun'),
        (-4.6, -1.0, -4.2, 9.2, 1.4, 8.4, 'moon_band'),
        (-1.2, -2.6, -4.5, 2.4, 2.2, 0.6, 'moon_jewel'),
    ]))
    out.append(part('right_moon_loop', 'hd_moon', (-2.8, -2.4, 0.2), (0, 0, -0.35), [(-2.4, -3.6, -1.2, 2.4, 3.6, 2.4, 'moon_loop')]))
    out.append(part('right_moon_pin', 'hd_moon', (-1.8, -3.6, -0.6), (0, 0.2, 0.35), [
        (-4.6, -0.3, -0.3, 4.6, 0.6, 0.6, 'moon_pin'), (-4.4, 0.3, -0.3, 0.6, 2.8, 0.6, 'moon_dangle'),
        (-3.0, 0.3, -0.3, 0.6, 1.8, 0.6, 'moon_dangle')]))
    out += arc('hd_moon_crescent', 'hd_moon', 0, -8.4, -1.4, 3.4, 15, 165, 6, 1.4, 1.0, 'moon_crescent')
    # 2 Wood: his hair in a topknot, a lotus crown of jade petals about it, a jade pin through
    out.append(part('hd_jupiter', 'head', H, (0, 0, 0), [
        (-1.8, -3.6, -1.8, 3.6, 3.8, 3.6, 'ju_knot'),
        (-2.6, -1.8, -2.6, 5.2, 1.6, 5.2, 'ju_base'),
        (-4.6, -0.9, -4.3, 9.2, 1.4, 8.6, 'ju_band'),
        (-5, -2.8, -0.3, 10, 0.6, 0.6, 'ju_pin'),
    ]))
    for i in range(6):
        a = i * math.pi / 3 + math.pi / 6
        out.append(part('hd_jupiter_petal_%d' % i, 'hd_jupiter', (2.0 * math.sin(a), -1.6, -2.0 * math.cos(a)), (0.4, -a, 0), [
            (-1.2, -3.4, -0.15, 2.4, 3.4, 0.3, 'ju_petal')]))
    # 3 Fire: a gold band, a crown of flames rising from it, his red hair standing up behind
    out.append(part('hd_mars', 'head', H, (0, 0, 0), [
        (-4.7, -0.9, -4.3, 9.4, 1.4, 8.6, 'mars_band'),
        (-1.2, -1.6, -4.7, 2.4, 2.2, 0.6, 'mars_jewel'),
        (-3.6, -2.6, -2.4, 7.2, 2.4, 6.0, 'mars_hair'),
    ]))
    out.append(part('hd_mars_flame', 'hd_mars', (0, -1, -2.6), (-0.1, 0, 0), [
        (-3.4, -7.4, -0.1, 6.8, 7.4, 0.2, 'mars_flame'), (-0.1, -7.4, -3.4, 0.2, 7.4, 6.8, 'mars_flame_x')]))
    out.append(part('right_mars_flame', 'hd_mars', (-3.4, -0.8, -1.4), (0, 0.3, -0.35), [
        (-2.2, -5.2, -0.1, 4.4, 5.2, 0.2, 'mars_flame_s')]))
    # 4 Earth: the old star's great bald brow, a wisp of white hair at its back, brows drooping long
    out.append(part('hd_saturn', 'head', H, (0, 0, 0), [
        (-3.9, -5.0, -3.8, 7.8, 5.6, 7.2, 'saturn_dome'),
        (-3.0, -6.4, -3.0, 6.0, 1.6, 5.8, 'saturn_dome_top'),
        (-1.0, -6.6, 1.6, 2.0, 2.4, 2.0, 'saturn_knot'),
    ]))
    out.append(part('right_saturn_brow', 'hd_saturn', (-1.4, 2.6, -4.3), (0, 0.15, 0.95), [(-0.6, -0.4, -0.5, 1.2, 5.6, 1, 'saturn_brow')]))
    # 5 Metal: a gold crown over her bun, a phoenix-cock on its top, its tail plumes up behind; pins with dangles
    out.append(part('hd_venus', 'head', H, (0, 0, 0), [
        (-2.8, -4.4, -1.6, 5.6, 4.8, 4.6, 'venus_bun'),
        (-4.6, -1.6, -4.3, 9.2, 2.0, 8.6, 'venus_crown'),
        (-2.2, -3.4, -4.6, 4.4, 2.6, 0.6, 'venus_plaque'),
    ]))
    out.append(part('hd_venus_bird', 'hd_venus', (0, -4.4, -0.2), (0, 0, 0), [
        (-1.1, -2.6, -2.0, 2.2, 2.6, 3.6, 'venus_bird'),
        (-0.75, -4.8, -2.8, 1.5, 2.6, 1.5, 'venus_bird_head'),
        (-0.3, -5.8, -2.6, 0.6, 1.2, 1.2, 'venus_comb'),
        (-0.3, -4.2, -3.6, 0.6, 0.6, 0.9, 'venus_beak'),
    ]))
    out.append(part('hd_venus_tail', 'hd_venus_bird', (0, -1.6, 1.2), (-0.55, 0, 0), [
        (-2.2, -6.4, -0.1, 4.4, 6.4, 0.2, 'venus_tail'), (-0.1, -6.4, -1.6, 0.2, 6.4, 3.2, 'venus_tail_x')]))
    out.append(part('right_venus_pin', 'hd_venus', (-3.2, -1.2, -1.2), (0, 0.15, 0.25), [
        (-3.6, -0.3, -0.3, 3.6, 0.6, 0.6, 'venus_pin'), (-3.4, 0.3, -0.3, 0.6, 3.2, 0.6, 'venus_dangle'),
        (-2.2, 0.3, -0.3, 0.6, 2.2, 0.6, 'venus_dangle')]))
    # 6 Water: the scholar-official's black cap (幞头), its long wings out either side
    out.append(part('hd_mercury', 'head', H, (0, 0, 0), [
        (-4.0, -3.6, -3.8, 8.0, 4.4, 7.6, 'futou'),
        (-3.0, -6.2, -0.4, 6.0, 3.0, 4.2, 'futou_top'),
        (-4.2, -0.5, -4.1, 8.4, 0.9, 8.2, 'futou_band'),
    ]))
    out.append(part('right_futou_wing', 'hd_mercury', (-3.6, -2.6, 2.6), (0, 0, -0.03), [(-12, -0.75, -0.2, 12, 1.5, 0.4, 'futou_wing')]))
    # 7 Rahu: a gold band set with skulls, his red hair standing up in flames
    out.append(part('hd_rahu', 'head', H, (0, 0, 0), [
        (-4.7, -0.9, -4.3, 9.4, 1.6, 8.6, 'rahu_band'),
        (-3.8, -2.6, -3.0, 7.6, 2.4, 6.6, 'rahu_hair'),
    ]))
    for i, (x, z, rx, rz, ln) in enumerate(((-3.0, -2.2, -0.15, -0.55, 7), (-1.4, -2.6, -0.1, -0.2, 9), (0.4, -2.4, -0.12, 0.12, 9.5),
                                            (2.2, -2.0, -0.15, 0.45, 8), (-2.4, 0.6, -0.55, -0.4, 8.5), (0.0, 1.2, -0.6, 0.0, 10),
                                            (2.4, 0.6, -0.55, 0.4, 8.5), (-1.2, 2.6, -0.9, -0.2, 8), (1.4, 2.6, -0.9, 0.25, 8))):
        out.append(part('hd_rahu_lock_%d' % i, 'hd_rahu', (x, -1.6, z), (rx, 0, rz), [(-1.3, -ln, -1.1, 2.6, ln, 2.2, 'rahu_lock')]))
    # 8 Ketu: a band with a violet star-jewel, his hair swept back and streaming like a comet's tail
    out.append(part('hd_ketu', 'head', H, (0, 0, 0), [
        (-4.7, -0.9, -4.3, 9.4, 1.4, 8.6, 'ketu_band'),
        (-1.3, -2.8, -4.7, 2.6, 2.6, 0.6, 'ketu_jewel'),
        (-3.6, -2.2, -3.4, 7.2, 1.8, 7.4, 'ketu_hair'),
    ]))
    for i, (x, y, ry, rx, ln, w) in enumerate(((-2.6, -1.4, -0.25, 0.25, 12, 2.6), (0, -2.2, 0, 0.12, 15, 3.0), (2.6, -1.4, 0.25, 0.25, 12, 2.6),
                                               (-1.4, 0.8, -0.12, -0.05, 13, 2.4), (1.4, 0.8, 0.12, -0.05, 13, 2.4))):
        out.append(part('hd_ketu_lock_%d' % i, 'hd_ketu', (x, y, 2.4), (rx, ry, 0), [(-w / 2, -1.1, 0, w, 2.2, ln, 'ketu_lock')]))
    return out


PARTS = mirrored([
    # the body from the waist up; its pivot the waist
    part('body', 'root', (0, 4, 0), (0, 0, 0), [
        (-6, -15, -5, 12, 15, 10, 'chest'),
        (-3.8, -16.2, -3.2, 7.6, 1.6, 6.4, 'collar'),
        (-2.2, -18.4, -2.2, 4.4, 3.6, 4.4, 'neck'),
        (-7.6, -15.6, -6, 15.2, 0.8, 12, 'cloudtop'),
        (-7.2, -15.2, -6.1, 14.4, 6, 0.5, 'lobes'),
        (-7.2, -15.2, 5.6, 14.4, 6, 0.5, 'lobes_back'),
        (-6.8, -4.4, -5.9, 13.6, 2.2, 11.8, 'jadebelt'),
        (-2, -5, -6.6, 4, 3, 0.8, 'beltplaque'),
    ]),
    part('skirt', 'body', (0, 0, 0), (0, 0, 0), [
        (-6.6, 0, -5.9, 13.2, 7, 11.8, 'robe1'),
        (-7.8, 6.5, -6.6, 15.6, 7, 13.2, 'robe2'),
        (-9.4, 13, -7.8, 18.8, 7, 15.6, 'hem'),
    ]),
    part('apron', 'skirt', (0, -1, -6.3), (-0.12, 0, 0), [(-3.4, 0, -1, 6.8, 21, 1, 'apron')]),
    part('right_panel', 'skirt', (-5.4, -1, -6), (-0.1, 0.35, 0.05), [(-2.2, 0, -0.5, 4.4, 20.5, 0.5, 'panel')]),
    part('back_panel', 'skirt', (0, -1.5, 6.2), (0.12, 0, 0), [(-4.5, 0, 0, 9, 21, 0.6, 'backpanel')]),
    part('right_pendant', 'skirt', (-6.6, -2, -2.6), (0, 0, 0.12), [
        (-0.3, 0, -0.3, 0.6, 3.4, 0.6, 'pendant_cord'),
        (-1.2, 3.2, -0.5, 2.4, 2.4, 1.0, 'pendant_jade'),
        (-0.3, 5.4, -0.3, 0.6, 2.2, 0.6, 'pendant_cord'),
        (-0.9, 7.4, -0.5, 1.8, 3.6, 1.0, 'pendant_tassel'),
    ]),
    part('right_boot', 'root', (-3.2, 22.4, -7.4), (0, 0.12, 0), [
        (-1.7, 0, -2.4, 3.4, 1.6, 4.2, 'boot'), (-1.2, -1.1, -3.4, 2.4, 1.6, 1.2, 'boottoe')]),
    # the ribbon (披帛): an arch behind the shoulders, streaming down either side
    part('arch', 'body', (0, -14, 6.4), (0.12, 0, 0), []),
] + arc('arch_seg', 'arch', 0, 0, 0, 12.4, 180, 360, 10, 2.2, 0.4, 'ribbon') + [
    part('right_ribbon', 'body', (-12.2, -14.6, 6.2), (0.08, 0.35, 0.1), [(-1.2, 0, -0.2, 2.4, 13, 0.4, 'ribbon_a')]),
    part('right_ribbon_2', 'right_ribbon', (0, 12.6, 0), (0.12, 0, 0.16), [(-1.3, 0, -0.2, 2.6, 13, 0.4, 'ribbon_b')]),
    # the arms: the cloud collar's lobe on the shoulder, the forearms brought forward, great sleeves hanging
    part('right_arm', 'body', (-7.8, -13.4, 0), ARM, [(-3.4, -2, -2.8, 4.8, 9, 5.6, 'sleeve')]),
    part('right_shoulder', 'right_arm', (-1.2, -2.2, 0), (0, 0, 0.3), [(-4.4, -1.2, -3.9, 5.8, 2, 7.8, 'shoulder')]),
    part('right_forearm', 'right_arm', (-1, 6.6, 0), FOREARM, [
        (-2.3, 0, -2.3, 4.6, 7, 4.6, 'forearm'),
        (-2.7, 5, -2.7, 5.4, 2, 5.4, 'cuff'),
    ]),
    part('right_sleeve', 'right_forearm', (0, 0.4, 2.1), aim([ARM, FOREARM], (0.05, -0.3, 0.14)), [
        (-3.4, -1, -9.4, 6.8, 12.5, 9.4, 'bigsleeve')]),
    part('right_hand', 'right_forearm', (0, 7.2, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3, 3, 3, 'hand')]),
    part('tablet', 'right_hand', (0, 1.6, -0.2), RHELD, [(-1.3, -12, -0.35, 2.6, 14, 0.7, 'tablet')]),
    part('brush', 'right_hand', (0, 1.6, -0.2), RHELD, [
        (-0.45, -9, -0.45, 0.9, 12, 0.9, 'brush_shaft'),
        (-0.8, -12.4, -0.8, 1.6, 3.6, 1.6, 'brush_tip'),
        (-0.7, 2.6, -0.7, 1.4, 1, 1.4, 'brush_end')]),
    # the head
    part('head', 'body', (0, -18.4, -0.8), (0, 0, 0), [
        (-4.4, -9, -4, 8.8, 7.4, 8, 'face'),
        (-1, -5.8, -4.8, 2, 2.4, 0.9, 'nose'),
        (-4.6, -9.3, -0.6, 9.2, 8, 5, 'hair_back'),
    ]),
    part('right_ear', 'head', (-4.4, -5.6, -0.8), (0, -0.2, 0), [(-0.7, -1.4, -0.9, 0.8, 3.2, 1.8, 'ear')]),
    part('right_brow', 'head', (-0.4, -6.5, -4.3), (0, 0.1, 0.24), [(-3.8, -0.9, -0.5, 3.8, 1.1, 1, 'brow')]),
    part('right_moustache', 'head', (-0.5, -3.3, -4.5), (0, 0.2, -0.55), [(-3.2, -0.5, -0.5, 3.2, 1, 1, 'moustache')]),
    part('right_sidelock', 'head', (-4.3, -7.6, -2.4), (-0.05, 0, 0.06), [(-0.8, 0, -1, 1.6, 9.5, 2, 'sidelock')]),
    part('jaw', 'head', (0, -2, -0.6), (0, 0, 0), [(-4.1, 0, -3.4, 8.2, 2.6, 6.8, 'jaw')]),
    part('beard', 'jaw', (0, 2, -3.4), (0.12, 0, 0), [(-1.4, 0, -0.9, 2.8, 9, 1.8, 'lock_c')]),
    part('beard_tip', 'beard', (0, 8.6, 0), (-0.1, 0, 0), [(-1, 0, -0.8, 2, 4.5, 1.6, 'lock_tip')]),
    part('right_beard', 'jaw', (-2.5, 1.6, -3.1), (0.12, 0.15, 0.22), [(-1.1, 0, -0.8, 2.2, 7.5, 1.6, 'lock_side')]),
] + headdresses() + emblems())


def raise_left(parts):
    """The left arm (mirrored from the right) out from his side, its forearm raised to hold up his emblem."""
    out = []
    for name, parent, pivot, rot, boxes, mirror in parts:
        if name == 'left_arm':
            rot = LARM
        elif name == 'left_shoulder':
            rot = (rot[0], rot[1], rot[2] - (LARM[2] + ARM[2]))
        elif name == 'left_forearm':
            rot = LFOREARM
        elif name == 'left_sleeve':
            rot = aim([LARM, LFOREARM], (0.05, 0.3, -0.14))
        out.append((name, parent, pivot, rot, boxes, mirror))
    return out


PARTS = raise_left(PARTS)

# the style prefixes of each one's own boxes: another's are left bare (the renderer shows only his)
OWN = {'sun': ('su_', 'sun_'), 'moon': ('mo_', 'moon_'), 'jupiter': ('ju_',), 'mars': ('ma_', 'mars_'),
       'saturn': ('sa_', 'saturn_'), 'venus': ('v_', 'venus_'), 'mercury': ('me_', 'futou'),
       'rahu': ('ra_', 'rahu_'), 'ketu': ('ke_', 'ketu_')}


def owner(style):
    for n, prefixes in OWN.items():
        if style.startswith(prefixes):
            return n
    return None


# ── The skins ────────────────────────────────────────────────────────────────

CROW = ['....##.....',
        '...####....',
        '..######.#.',
        '.#########.',
        '##########.',
        '.########..',
        '..######...',
        '...#..#....',
        '...#..#.#..',
        '..##.##.##.']

HARE = ['......#.#.....',
        '.....#..#.....',
        '.....#.#......',
        '....####......',
        '...#####......',
        '..######...##.',
        '.#########.##.',
        '.##########...',
        '..#########...',
        '...##...##....']


def stamp(p, glyph, x0, y0, colour, glow=None, flip=False):
    for j, row in enumerate(glyph):
        for i, c in enumerate(row):
            if c == '#':
                ii = len(row) - 1 - i if flip else i
                if glow is not None:
                    p.lit((x0 + ii, y0 + j), colour, glow=glow)
                else:
                    p.px((x0 + ii, y0 + j), colour)


def along_z(s, i, j, fw, fh):
    """How far back along a box laid along +z a pixel of its face lies: 0 at its front .. 1 at its back."""
    if s == 'top':
        return 1 - j / max(1, fh - 1)
    if s == 'bottom':
        return j / max(1, fh - 1)
    if s == 'right':
        return 1 - i / max(1, fw - 1)
    if s == 'left':
        return i / max(1, fw - 1)
    return 0.0 if s == 'front' else 1.0


def tress(p, f, tone, axis='y', light=1.0, keep=0.3, up=True):
    """A lock of hair along `axis` ('y': standing up from its root at the box's foot (`up`) or hanging from
    its top; 'z': streaming back from its root at the box's front): strands along it, lit at its crest,
    paling toward its tip, cut to a point there."""
    for s, r in f.items():
        fx, fy, fw, fh = r
        cols = [p.rnd.uniform(-0.35, 0.35) for _ in range(max(fw, fh) + 1)]
        for i, j, xy in each(r):
            if axis == 'y':
                a = j / max(1, fh - 1)
                along = 1 - a if up else a
                c_, n = i, fw
                if s in ('top', 'bottom'):
                    along = 1.0 if (s == 'top') == up else 0.0
            else:
                along = along_z(s, i, j, fw, fh)
                c_, n = (i, fw) if s in ('top', 'bottom') else (j, fh)
            across = (c_ + 0.5) / n
            t = 0.6 + 1.5 * (1 - abs(across - 0.45) * 2) + cols[c_ % len(cols)] + 0.7 * along
            t += {'top': 0.3, 'bottom': -0.8, 'back': -0.3}.get(s, 0)
            if n > 2 and min(c_, n - 1 - c_) == 0:
                t = min(t, 0.6)
            p.px(xy, noisy(p, shade_of(tone, t * light), 3))
            # its point: the lock narrowing toward its tip
            if (axis == 'y' and s in SIDES) or (axis == 'z' and s in ('top', 'bottom', 'right', 'left')):
                if along > 0.35 and abs(across - 0.5) * 2 > max(keep, (1 - along) / 0.65):
                    p.clear(xy)


def luminary_skin(index):
    name, robe, deep, under, face, hair, kind, iris, light = LUMINARIES[index]
    fierce = kind in ('fierce', 'demon')
    dark = sum(robe) < 200
    trim = GOLD if name != 'moon' else SILVER
    trim_deep = GOLD_DEEP if name != 'moon' else (130, 140, 166)
    face_shade = k(face, 0.72)
    beard = hair if name != 'mercury' else BLUE_BLACK
    wave_cols = {
        'sun': ((150, 30, 20), (246, 196, 70), (204, 58, 30), (255, 230, 150)),
        'moon': ((120, 150, 210), (236, 240, 250), (170, 190, 230), (255, 255, 255)),
        'jupiter': ((20, 90, 50), (90, 190, 120), (236, 206, 112), (190, 240, 200)),
        'mars': ((120, 10, 8), (250, 140, 30), (206, 34, 24), (255, 220, 120)),
        'saturn': ((110, 70, 30), (220, 180, 90), (176, 126, 50), (250, 226, 160)),
        'venus': ((190, 160, 90), (255, 250, 236), (226, 186, 80), (255, 255, 255)),
        'mercury': ((10, 14, 40), (70, 130, 206), (28, 36, 70), (170, 210, 255)),
        'rahu': ((14, 12, 18), (160, 34, 30), (70, 60, 80), (240, 120, 60)),
        'ketu': ((40, 16, 66), (206, 176, 236), (120, 70, 170), (250, 236, 255)),
    }[name]
    face_opts = dict(kind=kind, skin=face, hair=hair, eye=iris, third=name == 'mars',
                     paint={'rahu': (196, 34, 26), 'ketu': (84, 206, 232)}.get(name),
                     hairline=0 if name == 'saturn' else 1, mark=(220, 40, 60) if name == 'moon' else (232, 160, 40) if name == 'venus' else None,
                     lid=(150, 170, 230) if name == 'moon' else (220, 140, 120))

    def paint_lum(p, style, f, dims):
        who = owner(style)
        if who is not None and who != name:
            return

        if style == 'chest':
            silk(p, f, robe, deep)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                stars(p, (fx + 2, fy + 8, fw - 4, fh - 12), trim)
                vline(p, f[s], 0, trim_deep)
                vline(p, f[s], fw - 1, trim_deep)
            fx, fy, fw, fh = f['front']                            # the crossed collar of the inner robe
            for j in range(fh // 2):
                for d in range(3):
                    for x in (fw // 2 - j // 2 - d, fw // 2 + j // 2 + d - 1):
                        if 0 <= x < fw:
                            p.px((fx + x, fy + j), trim if d == 0 else under)

        if style == 'collar':
            p.shaded(f, under)
            edges(p, f, trim)
        if style == 'neck':
            p.shaded(f, face_shade)

        if style == 'cloudtop':                                    # the cloud collar's top: four lobes in the lining's colour, gold-edged
            p.shaded(f, deep)
            fx, fy, fw, fh = f['top']
            cx, cy = fx + fw / 2, fy + fh / 2
            for i, j, xy in each(f['top']):
                dx, dy = (i + 0.5 - fw / 2) / (fw / 2), (j + 0.5 - fh / 2) / (fh / 2)
                a = math.atan2(dy, dx)
                r = math.hypot(dx, dy)
                reach = 0.55 + 0.45 * abs(math.cos(2 * a)) ** 0.6
                if r < reach - 0.12:
                    p.px(xy, noisy(p, under, 3))
                elif r < reach:
                    p.px(xy, trim)
            edges(p, f, trim)
        if style in ('lobes', 'lobes_back'):                       # the cloud collar's lobes hanging over the chest: ruyi-shaped
            for s, r in f.items():
                p.face(r, deep, 1.0, noise=3)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    t = (i % (fw // 2)) / (fw // 2)               # two lobes either side of the middle
                    depth = int(fh * (0.35 + 0.65 * math.sin(t * math.pi) ** 0.7))
                    for j in range(fh):
                        if j >= depth:
                            p.clear((fx + i, fy + j))
                        elif j >= depth - 2:
                            p.px((fx + i, fy + j), trim)
                        elif j >= depth - 4:
                            p.px((fx + i, fy + j), under)
                    if depth > 6 and (i % (fw // 2)) == fw // 4:
                        circle(p, fx + i, fy + depth / 2 - 1, 0, 1.3, trim)

        if style == 'jadebelt':
            p.shaded(f, (52, 30, 26) if name != 'moon' else (90, 100, 130))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(1, fw - 2, 6):
                    for j in range(1, fh - 1):
                        for w in range(3):
                            p.px((fx + i + w, fy + j), JADE if w < 2 else JADE_DEEP)
                    p.px((fx + i, fy + 1), (190, 240, 210))
            edges(p, f, trim)
        if style == 'beltplaque':
            gold_box(p, f)
            fx, fy, fw, fh = f['front']
            circle(p, fx + fw / 2, fy + fh / 2, 0, 1.4, light)
            p.lit((fx + fw // 2, fy + fh // 2), light, glow=0.8)

        if style in ('robe1', 'robe2', 'hem'):                     # the tiers: silk sown with stars, gold at each tier's foot
            silk(p, f, robe if style != 'robe2' else mixc(robe, deep, 0.15), deep)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                if style == 'hem':
                    stars(p, f[s], trim, skip_bottom=8)
                    waves(p, f[s], 7, wave_cols, trim, trim_deep)
                else:
                    stars(p, f[s], trim)
                    hline(p, f[s], fh - 1, trim)
                    hline(p, f[s], fh - 2, trim_deep)
            if style == 'robe1':
                fx, fy, fw, fh = f['front']
                for j in range(fh):
                    for i in range(fw // 2 - 4, fw // 2 + 4):
                        p.px((fx + i, fy + j), trim if abs(i - fw // 2 + 0.5) > 3 else noisy(p, under, 3))

        if style == 'apron':                                       # the front panel: the lining's colour, gold-bordered, cloud-worked
            for kk, r in f.items():
                p.face(r, under, 1.0, noise=3, fall=0.15)
            fx, fy, fw, fh = f['front']
            vline(p, f['front'], 0, trim)
            vline(p, f['front'], fw - 1, trim)
            for i in range(fw):
                for j in (0, 1, 6, 7):
                    p.px((fx + i, fy + j), trim if j in (0, 7) else trim_deep)
            for j in range(12, fh - 10, 10):
                cloud_mark(p, fx + fw // 2 - 3, fy + j, trim if sum(under) < 500 else trim_deep, 1, f['front'])
            # the emblem's star at its head: a shining roundel
            circle(p, fx + fw / 2, fy + 3.5, 0, 2.6, trim)
            circle(p, fx + fw / 2, fy + 3.5, 0, 1.5, light)
            for i in range(fw):                                    # a fringe
                for j in range(fh - 5, fh):
                    if i % 2 == 0 or j < fh - 3:
                        p.px((fx + i, fy + j), trim if i % 4 < 2 else robe)
                    else:
                        p.clear((fx + i, fy + j))

        if style == 'panel':
            for kk, r in f.items():
                p.face(r, mixc(robe, deep, 0.5), 1.0, noise=3, fall=0.2)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                vline(p, f[s], 0, trim)
                vline(p, f[s], fw - 1, trim)
                hline(p, f[s], fh - 1, trim)
                for j in range(6, fh - 4, 9):
                    circle(p, fx + fw / 2, fy + j, 0, 1.2, trim_deep)
        if style == 'backpanel':
            for kk, r in f.items():
                p.face(r, mixc(robe, deep, 0.5), 1.0, noise=3, fall=0.15)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                border(p, f[s], trim)
                hem_band(p, f[s], 5, under, trim, trim_deep)
                clouds(p, (fx + 2, fy + 2, fw - 4, fh - 10), trim_deep, every=11, r=1)

        if style == 'pendant_cord':
            p.shaded(f, (180, 30, 30))
        if style == 'pendant_jade':                                # a jade disc (璧)
            p.shaded(f, JADE)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                circle(p, fx + fw / 2, fy + fh / 2, 0, 0.9, JADE_DEEP)
                p.px((fx + 1, fy + 1), (200, 250, 220))
        if style == 'pendant_tassel':
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    p.px(xy, k((200, 36, 36), 1.1 - 0.4 * (i % 2) - 0.2 * j / fh))
                hline(p, r, 0, trim)
                hline(p, r, 1, trim_deep)

        if style == 'boot':
            p.shaded(f, BLACK)
            for s in SIDES:
                hline(p, f[s], f[s][3] - 1, IVORY)
                hline(p, f[s], f[s][3] - 2, IVORY_SHADE)
        if style == 'boottoe':                                     # the upturned toe of a court boot, cloud-shaped
            p.shaded(f, BLACK)
            edges(p, f, trim, bottom=False)

        if style in ('ribbon', 'ribbon_a', 'ribbon_b'):            # the ribbon: the lining's colour, fine gold edges, a cloud here and there
            rib = under if name not in ('mercury',) else (140, 190, 240)
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    along = (i if style == 'ribbon' else j)
                    across = (j / max(1, fh - 1)) if style == 'ribbon' else (i / max(1, fw - 1))
                    wave = 1 + 0.12 * math.sin(along * 0.5)
                    c = k(rib, LIGHTS[s] * wave * (1.08 - 0.25 * abs(across - 0.4)))
                    p.px(xy, noisy(p, c, 2))
                if s in ('front', 'back'):
                    if style == 'ribbon':
                        hline(p, r, 0, trim)
                        hline(p, r, fh - 1, trim)
                    else:
                        vline(p, r, 0, trim)
                        vline(p, r, fw - 1, trim)
            if style == 'ribbon_b':                                # its end: cut in a swallowtail
                for s in ('front', 'back'):
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        cut = int(4 * (1 - abs(i + 0.5 - fw / 2) / (fw / 2)))
                        for j in range(cut):
                            p.clear((fx + i, fy + fh - 1 - j))

        if style in ('sleeve', 'forearm'):
            silk(p, f, robe, deep)
            for s in SIDES:
                stars(p, f[s], trim, n=1)
        if style == 'shoulder':                                    # the cloud collar's lobe over the shoulder
            p.shaded(f, deep)
            for s in SIDES + ('top',):
                fx, fy, fw, fh = f[s]
                hline(p, f[s], fh - 1, trim)
                if s != 'top':
                    hline(p, f[s], fh - 2, under)
            fx, fy, fw, fh = f['top']
            for i, j, xy in each(f['top']):
                dx, dy = (i + 0.5 - fw / 2) / (fw / 2), (j + 0.5 - fh / 2) / (fh / 2)
                r = math.hypot(dx * 0.9, dy)
                if 0.55 < r < 0.75:
                    p.px(xy, trim)
                elif r <= 0.55:
                    p.px(xy, noisy(p, under, 3))
        if style == 'cuff':
            p.shaded(f, under)
            edges(p, f, trim)

        if style == 'bigsleeve':                                   # the great sleeve: silk sown with stars, a broad border at its mouth
            silk(p, f, robe, deep)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                stars(p, f[s], trim, skip_bottom=8)
                hem_band(p, f[s], 7, under, trim, trim_deep)
                for i in range(2, fw - 4, 8):
                    cloud_mark(p, fx + i + 2, fy + fh - 4, trim_deep if sum(under) > 400 else trim, 1, f[s])
            fx, fy, fw, fh = f['bottom']
            for i, j, xy in each(f['bottom']):
                e = min(i, j, fw - 1 - i, fh - 1 - j)
                p.px(xy, trim if e == 0 else k(under, 0.7) if e < 3 else (14, 10, 14))

        if style == 'hand':
            hand(p, f, face, nails=(110, 40, 40) if fierce else None)

        if style == 'tablet':
            p.shaded(f, IVORY)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                vline(p, f[s], 0, IVORY_SHADE)
                vline(p, f[s], fw - 1, IVORY_SHADE)
                hline(p, f[s], 0, IVORY_SHADE)
                for j in range(fh - 7, fh):
                    for i in range(fw):
                        p.px((fx + i, fy + j), (122, 32, 28) if j % 2 else (90, 24, 20))
                for j in range(4, fh - 10, 3):                     # a line of writing
                    p.px((fx + fw // 2, fy + j), (120, 110, 90))
        if style == 'brush_shaft':                                 # a brush of mottled bamboo
            p.shaded(f, (170, 140, 80))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(0, fh, 5):
                    hline(p, f[s], j, (110, 80, 40))
                for _ in range(fh // 3):
                    p.px((fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)), (100, 60, 40))
        if style == 'brush_tip':                                   # its tip, inked, drawn to a point upward
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    t = j / max(1, fh - 1)
                    p.px(xy, mixc((20, 20, 30), (220, 210, 190), t * 0.9))
            for s in SIDES:
                point(p, f[s], keep=0.5, down=False, sharp=0.35)
        if style == 'brush_end':
            gold_box(p, f)

        # the head
        if style == 'face':
            hair_box(p, f, hair)
            for s in ('right', 'left'):                            # skin before the ear
                fx, fy, fw, fh = f[s]
                top = 0 if name == 'saturn' else 4
                for j in range(top, fh):
                    for i in range(6):
                        x = fx + (fw - 1 - i if s == 'right' else i)
                        p.px((x, fy + j), k(face, 0.92 - 0.03 * i))
            face_front(p, f['front'], face_opts)
            p.face(f['bottom'], face_shade, 1.0)
            if name == 'saturn':                                   # the top under his dome: skin
                p.face(f['top'], face, 1.0)
        if style == 'nose':
            skin_box(p, f, face)
            fx, fy, fw, fh = f['front']
            p.px((fx, fy + fh - 1), face_shade)
            p.px((fx + fw - 1, fy + fh - 1), face_shade)
            if fierce:                                             # flared nostrils
                p.px((fx, fy + fh - 1), (40, 10, 10))
                p.px((fx + fw - 1, fy + fh - 1), (40, 10, 10))
            p.face(f['bottom'], face_shade, 1.0)
        if style == 'ear':
            skin_box(p, f, face)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for j in range(1, fh - 1):
                    p.px((fx + fw // 2, fy + j), face_shade)
                if kind == 'lady':                                 # an earring, a pearl
                    p.px((fx + fw // 2, fy + fh - 1), (250, 250, 255))
        if style == 'hair_back':
            hair_box(p, f, hair)
            if name == 'saturn':                                   # his bald crown: the hair only a white fringe low at the back
                for s, r in f.items():
                    fx, fy, fw, fh = r
                    for i, j, xy in each(r):
                        if s == 'top' or j < fh * 0.45:
                            p.px(xy, noisy(p, k(face, LIGHTS[s] * (1.05 - 0.1 * j / fh)), 2))
        if style == 'brow':
            brow_box(p, f, beard)
        if style == 'moustache':
            brow_box(p, f, beard)
        if style == 'sidelock':
            tress(p, f, hair, axis='y', up=False, keep=0.6)
        if style == 'jaw':
            skin_box(p, f, face)
            if kind not in ('lady', 'demon') or name == 'rahu':    # a man's beard about the chin
                fx, fy, fw, fh = f['front']
                for i, j, xy in each(f['front']):
                    if j >= 2 or i < 3 or i > fw - 4:
                        p.px(xy, shade_of(beard, 1.0 + ((i + j) % 3) * 0.4))
                for s in ('right', 'left', 'bottom'):
                    for i, j, xy in each(f[s]):
                        p.px(xy, shade_of(beard, 0.8 + ((i + 2 * j) % 3) * 0.4))
            elif kind == 'demon':                                  # a demon's chin: tusks up over the lip
                fx, fy, fw, fh = f['front']
                for i in (3, fw - 4):
                    for j in range(0, 3):
                        p.px((fx + i, fy + j), IVORY)
                        p.px((fx + i + (1 if i < fw / 2 else -1), fy + j + 1), IVORY_SHADE)
        if style.startswith('lock_'):
            beard_lock(p, f, beard, keep=0.6 if style == 'lock_c' else 0.4, taper=0.3 if style != 'lock_tip' else 0.45)

        paint_headdress(p, style, f, name, robe, deep, under, face, hair, light, trim, trim_deep)
        paint_emblem(p, style, f, name, light)

    return paint_lum


def paint_headdress(p, style, f, name, robe, deep, under, face, hair, light, trim, trim_deep):
    # 0 the sun's crown
    if style == 'sun_cap':
        p.shaded(f, (150, 24, 20))
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(1, fw, 4):
                vline(p, (fx, fy, fw, fh), i, GOLD_DEEP)
            hline(p, f[s], 0, GOLD)
    if style in ('sun_band',):
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(2, fw, 5):
                p.lit((fx + i, fy + fh // 2), (255, 90, 40), glow=0.6)
    if style == 'sun_pin':
        gold_box(p, f)
    if style == 'sun_crest':                                       # a small sun on his brow, shining
        gold_box(p, f)
        def c(r, a, d):
            if r > 0.85:
                return GOLD_DEEP
            if r > 0.7:
                return GOLD
            return ('glow', mixc((255, 250, 210), (255, 120, 40), r / 0.7), 0.9)
        disc(p, f['front'], c)
        disc(p, f['back'], lambda r, a, d: GOLD if r < 0.9 else GOLD_DEEP)
        bare(p, f)
    if style == 'sun_ray':                                         # a gold ray, tapering to a shining point
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i, j, xy in each(f[s]):
                if j < fh * 0.35:
                    p.lit(xy, mixc((255, 240, 160), (255, 150, 40), j / (fh * 0.35)), glow=0.75)
            point(p, f[s], keep=0.5, down=False, sharp=0.35)

    # 1 the moon's head-dress
    if style in ('moon_bun', 'moon_loop', 'venus_bun'):
        hair_box(p, f, hair)
        for s in SIDES:                                            # a band of silver round its root
            if style != 'moon_loop':
                hline(p, f[s], f[s][3] - 2, trim)
                hline(p, f[s], f[s][3] - 3, trim_deep)
    if style == 'moon_band':                                       # a silver band set with pearls
        metal(p, f, SILVER, (120, 130, 160))
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(1, fw, 3):
                p.px((fx + i, fy + fh // 2), (255, 255, 255))
    if style == 'moon_jewel':
        metal(p, f, SILVER, (120, 130, 160))
        fx, fy, fw, fh = f['front']
        circle(p, fx + fw / 2, fy + fh / 2, 0, 1.4, (170, 210, 255))
        p.lit((fx + fw // 2, fy + fh // 2), (220, 240, 255), glow=0.9)
    if style == 'moon_crescent':                                   # the silver crescent, shining
        for s, r in f.items():
            for i, j, xy in each(r):
                p.lit(xy, mixc((255, 255, 255), (170, 196, 240), j / max(1, r[3] - 1)), glow=0.8)
    if style in ('moon_pin', 'venus_pin'):
        metal(p, f, SILVER if style == 'moon_pin' else GOLD, GOLD_DEEP)
    if style in ('moon_dangle', 'venus_dangle'):                   # strings of pearls (or coral beads)
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                bead = (250, 250, 255) if style == 'moon_dangle' else (210, 40, 40)
                p.px(xy, k(bead, 1.0 if j % 2 == 0 else 0.6))

    # 2 Wood's lotus crown
    if style == 'ju_knot':
        hair_box(p, f, hair)
    if style in ('ju_base', 'ju_band'):
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(1, fw - 1, 4):
                for j in range(1, fh - 1):
                    p.px((fx + i, fy + j), JADE)
    if style == 'ju_pin':
        p.shaded(f, JADE)
        edges(p, f, GOLD, top=False)
    if style == 'ju_petal':                                        # a jade petal, pointed, a gold vein down it
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                t = j / max(1, fh - 1)
                p.px(xy, noisy(p, k(mixc((150, 230, 180), JADE_DEEP, t), LIGHTS[s]), 2))
            if s in ('front', 'back'):
                vline(p, r, fw // 2, GOLD)
                point(p, r, keep=0.5, down=False, sharp=0.45)

    # 3 Fire's flame crown
    if style == 'mars_band':
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(2, fw, 5):
                p.lit((fx + i, fy + fh // 2), FIRE, glow=0.7)
    if style == 'mars_jewel':
        gold_box(p, f)
        fx, fy, fw, fh = f['front']
        for i, j, xy in each((fx + 1, fy + 1, fw - 2, fh - 2)):
            p.lit(xy, (255, 120, 40), glow=0.9)
    if style == 'mars_hair':
        hair_box(p, f, RED_HAIR, lean=1)
    if style in ('mars_flame', 'mars_flame_x', 'mars_flame_s'):
        flat = ('right', 'left') if style == 'mars_flame_x' else ('front', 'back')
        for s in flat:
            flame_face(p, f[s], tongues=2 if style != 'mars_flame_s' else 1)
        bare(p, f, [s for s in ('front', 'back', 'right', 'left', 'top', 'bottom') if s not in flat])

    # 4 Earth's bald brow
    if style in ('saturn_dome', 'saturn_dome_top'):
        skin_box(p, f, face)
        fx, fy, fw, fh = f['front']
        if style == 'saturn_dome':                                 # lines across the great brow, a gleam on it
            for jj in (fh - 5, fh - 3):
                for i in range(4, fw - 4):
                    if (i // 3) % 3 != 2:
                        p.px((fx + i, fy + jj), k(face, 0.82))
            for i in range(fw // 2 - 3, fw // 2):
                p.px((fx + i, fy + 2), k(face, 1.18))
            for s in ('back', 'right', 'left'):                    # the white fringe low behind
                rx, ry, rw, rh = f[s]
                for i, j, xy in each(f[s]):
                    if j > rh * 0.7 and (s == 'back' or (i > rw * 0.5 if s == 'right' else i < rw * 0.5)):
                        p.px(xy, shade_of(WHITE_HAIR, 1.4 + p.rnd.uniform(-0.4, 0.4)))
    if style == 'saturn_knot':
        hair_box(p, f, WHITE_HAIR)
        hline(p, f['front'], f['front'][3] - 1, (170, 40, 40))
    if style == 'saturn_brow':
        tress(p, f, WHITE_HAIR, axis='y', up=False, keep=0.4)

    # 5 Metal's phoenix crown
    if style == 'venus_crown':
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(fw):                                    # an openwork of scrolls, kingfisher-blue between
                if (i // 2) % 3 == 1:
                    for j in range(1, fh - 1):
                        p.px((fx + i, fy + j), (60, 140, 200))
            hline(p, f[s], 0, GOLD_LIGHT)
    if style == 'venus_plaque':
        gold_box(p, f)
        fx, fy, fw, fh = f['front']
        circle(p, fx + fw / 2, fy + fh / 2, 0, 1.6, (255, 250, 220))
        p.lit((fx + fw // 2, fy + fh // 2), (255, 250, 210), glow=0.9)
    if style in ('venus_bird', 'venus_bird_head'):                 # the cock, gold, its feathers scaled
        gold_box(p, f)
        for s in SIDES:
            p.scales(f[s], GOLD, cell=3, light=LIGHTS[s])
        if style == 'venus_bird_head':
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                p.px((fx + fw // 3, fy + 1), (20, 10, 10))
    if style == 'venus_comb':
        p.shaded(f, (220, 40, 40))
    if style == 'venus_beak':
        p.shaded(f, GOLD_LIGHT)
    if style in ('venus_tail', 'venus_tail_x'):                    # the tail plumes, rising and curling
        flat = ('right', 'left') if style == 'venus_tail_x' else ('front', 'back')
        for s in flat:
            cutout(p, f[s], lambda s_, t: abs(s_ - 0.5 - 0.25 * math.sin(t * 3)) < 0.18 + 0.28 * t,
                   lambda s_, t: mixc((255, 240, 170), (220, 130, 40), (1 - t) * 0.7) if int(t * 8) % 2 else mixc(GOLD, (60, 140, 200), 0.3))
        bare(p, f, [s for s in ('front', 'back', 'right', 'left', 'top', 'bottom') if s not in flat])

    # 6 Water's winged cap
    if style in ('futou', 'futou_top'):
        p.shaded(f, (26, 24, 30))
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i, j, xy in each(f[s]):
                if (i + j) % 2 == 0:
                    p.px(xy, (36, 34, 44))                         # gauze
            if style == 'futou':
                hline(p, f[s], 0, (54, 52, 66))
    if style == 'futou_band':
        p.shaded(f, (20, 18, 24))
        fx, fy, fw, fh = f['front']
        for i in range(fw // 2 - 2, fw // 2 + 2):
            p.px((fx + i, fy), GOLD)
            p.px((fx + i, fy + 1), (80, 150, 230))
    if style == 'futou_wing':                                      # a long black wing, gold-edged, stiffened
        p.shaded(f, (30, 28, 36))
        for s in ('front', 'back', 'top', 'bottom'):
            fx, fy, fw, fh = f[s]
            border(p, f[s], GOLD_DEEP)
            if s in ('front', 'back'):
                for i in range(3, fw - 2, 6):
                    p.px((fx + i, fy + fh // 2), GOLD)

    # 7 Rahu's wild hair
    if style == 'rahu_band':
        gold_box(p, f)
        for s in SIDES:                                            # set with little skulls
            fx, fy, fw, fh = f[s]
            for i in range(1, fw - 3, 5):
                for (a, b) in ((0, 0), (1, 0), (2, 0), (0, 1), (2, 1), (1, 1)):
                    if b < fh:
                        p.px((fx + i + a, fy + b), IVORY if (a, b) != (1, 1) else (40, 20, 20))
    if style == 'rahu_hair':
        hair_box(p, f, RED_HAIR, lean=1)
    if style == 'rahu_lock':                                       # a lock of hair standing up like a flame, its tip burning
        tress(p, f, RED_HAIR, axis='y', up=True, keep=0.4)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i, j, xy in each((fx, fy, fw, max(1, fh // 4))):
                if p.img.getpixel(xy)[3]:
                    p.lit(xy, mixc((255, 200, 80), (240, 90, 30), j / max(1, fh // 4)), glow=0.6)

    # 8 Ketu's streaming hair
    if style == 'ketu_band':
        p.shaded(f, (60, 40, 90))
        edges(p, f, SILVER)
    if style == 'ketu_jewel':
        metal(p, f, SILVER, (120, 120, 150))
        fx, fy, fw, fh = f['front']
        for i, j, xy in each((fx + 1, fy + 1, fw - 2, fh - 2)):
            p.lit(xy, (220, 170, 255), glow=0.9)
    if style == 'ketu_hair':
        hair_box(p, f, VIOLET_HAIR)
    if style == 'ketu_lock':
        tress(p, f, VIOLET_HAIR, axis='z', keep=0.4)
        for s in ('top', 'right', 'left'):                         # its ends fading into starlight
            fx, fy, fw, fh = f[s]
            for i, j, xy in each(f[s]):
                along = along_z(s, i, j, fw, fh)
                if along > 0.75 and p.img.getpixel(xy)[3] and p.rnd.random() < 0.4:
                    p.lit(xy, (240, 220, 255), glow=0.6)


def paint_emblem(p, style, f, name, light):
    # 0 the solar disc: a red-gold sun in a ring of flame, the three-legged crow black within it
    if style in ('su_handle', 'mo_handle', 'ra_handle'):
        metal(p, f, GOLD if style != 'mo_handle' else SILVER, GOLD_DEEP)
        for s in SIDES:
            for j in range(1, f[s][3], 3):
                hline(p, f[s], j, GOLD_DEEP)
    if style in ('su_bracket', 'mo_bracket', 'ra_bracket'):        # a cloud bracket
        base = GOLD if style == 'su_bracket' else SILVER if style == 'mo_bracket' else (70, 50, 60)
        metal(p, f, base, GOLD_DEEP)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            if i % 3 == 1:
                p.px((fx + i, fy + 1), JADE if style != 'ra_bracket' else (180, 40, 30))
    if style == 'su_disc':
        def front(r, a, d):
            lick = 0.07 * math.sin(a * 14)
            if r > 0.92 + lick:
                return None
            if r > 0.74:
                return ('glow', mixc(FIRE_HOT, FIRE, (r - 0.74) / 0.26), 0.9)
            if r > 0.68:
                return ('glow', GOLD_LIGHT, 0.8)
            return ('glow', mixc((255, 246, 200), (240, 70, 30), (r / 0.68) ** 1.5), 0.85)
        disc(p, f['front'], front)
        disc(p, f['back'], lambda r, a, d: None if r > 0.92 + 0.07 * math.sin(a * 14) else
             (('glow', FIRE, 0.6) if r > 0.74 else (220, 90, 40) if r < 0.6 else GOLD))
        fx, fy, fw, fh = f['front']                                # 26x26
        stamp(p, CROW, fx + fw // 2 - 5, fy + fh // 2 - 5, (24, 12, 10), glow=None)
        for j, row in enumerate(CROW):                            # the crow dark on the glowing skin too
            for i, c in enumerate(row):
                if c == '#':
                    p.glow.putpixel((fx + fw // 2 - 5 + i, fy + fh // 2 - 5 + j), (40, 8, 0, 255))
        p.px((fx + fw // 2 - 1, fy + fh // 2 - 4), (255, 80, 40))     # its eye
        bare(p, f)

    # 1 the moon: a pale disc, the hare pounding the elixir of life under the cassia
    if style == 'mo_disc':
        def front(r, a, d):
            if r > 0.97:
                return None
            if r > 0.88:
                return SILVER if r < 0.93 else (140, 150, 180)
            shadow = 0.12 * (d[0] + d[1])                          # a faint shading across it
            return ('glow', mixc((250, 252, 255), (176, 196, 232), max(0, min(1, r * 0.6 + shadow))), 0.75)
        disc(p, f['front'], front)
        disc(p, f['back'], lambda r, a, d: None if r > 0.97 else SILVER if r > 0.88 else (200, 210, 230))
        fx, fy, fw, fh = f['front']                                # 23x23
        for j, row in enumerate(HARE):
            for i, c in enumerate(row):
                if c == '#':
                    xy = (fx + fw // 2 - 7 + i, fy + fh // 2 - 3 + j)
                    p.lit(xy, (150, 168, 210), glow=0.5)
        for j in range(4):                                         # the mortar before it
            for i in range(3):
                p.lit((fx + fw // 2 + 4 + i, fy + fh // 2 + 3 + j), (130, 140, 186), glow=0.45)
        for i in range(5):                                         # the cassia's branches above
            p.lit((fx + fw // 2 - 2 + i, fy + 4 + (i % 2)), (170, 186, 220), glow=0.55)
            p.lit((fx + fw // 2, fy + 4 + i), (170, 186, 220), glow=0.55)
        bare(p, f)

    # 2 Wood's staff
    if style in ('ju_staff', 'ju_twig'):                           # gnarled wood, knots
        p.shaded(f, WOOD)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 6):
                p.px((fx + p.rnd.randrange(fw), fy + j), WOOD_DEEP)
                p.px((fx + p.rnd.randrange(fw), fy + j + 1), (160, 120, 70))
            vline(p, f[s], 0, WOOD_DEEP)
    if style == 'ju_leaves':                                       # a cluster of leaves, lit on top, faintly shining
        for s, r in f.items():
            for i, j, xy in each(r):
                leaf = ((i // 2) + (j // 2)) % 3
                c = [(70, 170, 90), (40, 120, 60), (120, 220, 130)][leaf]
                c = k(c, LIGHTS[s])
                if leaf == 2 and p.rnd.random() < 0.5:
                    p.lit(xy, (150, 255, 170), glow=0.45)
                else:
                    p.px(xy, noisy(p, c, 4))
    if style == 'ju_pearl':
        for s, r in f.items():
            for i, j, xy in each(r):
                d = math.hypot(i + 0.5 - r[2] / 2, j + 0.5 - r[3] / 2) / (r[2] / 2)
                p.lit(xy, mixc((230, 255, 236), (60, 200, 120), min(1, d)), glow=0.95)

    # 3 Fire's sword
    if style in ('ma_hilt',):
        p.shaded(f, (150, 30, 26))
        for s in SIDES:
            for j in range(0, f[s][3], 2):
                hline(p, f[s], j, (90, 14, 12))
    if style in ('ma_pommel', 'ma_guard'):
        gold_box(p, f)
        if style == 'ma_guard':
            fx, fy, fw, fh = f['front']
            p.lit((fx + fw // 2, fy + fh // 2), (255, 120, 40), glow=0.9)
    if style == 'ma_blade':                                        # the blade, white-hot down its ridge
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                across = abs((i + 0.5) / fw - 0.5) * 2
                c = mixc((255, 250, 220), (255, 150, 50), across)
                p.lit(xy, c, glow=0.85)
        for s in SIDES:
            point(p, f[s], keep=0.5, down=False, sharp=0.6)
    if style in ('ma_flame', 'ma_flame_x'):
        flat = ('right', 'left') if style == 'ma_flame_x' else ('front', 'back')
        for s in flat:
            flame_face(p, f[s], tongues=3)
        bare(p, f, [s for s in ('front', 'back', 'right', 'left', 'top', 'bottom') if s not in flat])

    # 4 Earth's dragon-head staff
    if style == 'sa_staff':
        p.shaded(f, (110, 74, 40))
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 7):                              # knots, a gnarled grain
                hline(p, f[s], j, (70, 44, 22))
                p.px((fx + p.rnd.randrange(fw), fy + j + 2), (150, 110, 60))
            vline(p, f[s], 0, (70, 44, 22))
    if style in ('sa_head', 'sa_horn'):                            # the dragon's head carved in the wood, gilt
        p.shaded(f, (130, 90, 46))
        for s in SIDES + ('top',):
            fx, fy, fw, fh = f[s]
            for i, j, xy in each(f[s]):
                if (i + 2 * j) % 5 == 0:
                    p.px(xy, GOLD)
        if style == 'sa_head':
            fx, fy, fw, fh = f['front']
            p.px((fx + 1, fy + 2), (255, 220, 80))
            p.px((fx + fw - 2, fy + 2), (255, 220, 80))
            for s in ('right', 'left'):
                rx, ry, rw, rh = f[s]
                p.lit((rx + rw // 3, ry + 2), (255, 220, 80), glow=0.8)
    if style == 'sa_pearl':
        for s, r in f.items():
            for i, j, xy in each(r):
                d = math.hypot(i + 0.5 - r[2] / 2, j + 0.5 - r[3] / 2) / (r[2] / 2)
                p.lit(xy, mixc((255, 250, 200), (240, 170, 40), min(1, d)), glow=0.95)
    if style == 'sa_cord':
        p.shaded(f, (190, 40, 36))
    if style in ('sa_gourd_hi', 'sa_gourd_lo'):                    # a gourd, glossy, a red cord at its waist
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                across = abs((i + 0.5) / fw - 0.4) * 2
                p.px(xy, noisy(p, k((214, 160, 60), (1.25 - 0.5 * across) * LIGHTS[s]), 3))
        if style == 'sa_gourd_hi':
            for s in SIDES:
                hline(p, f[s], f[s][3] - 1, (190, 40, 36))

    # 5 Metal's pipa
    if style in ('v_neck', 'v_head', 'v_shoulder'):
        p.shaded(f, (120, 60, 34))
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            if style == 'v_neck' and s == 'front':                 # the frets
                for j in range(1, fh, 3):
                    hline(p, f[s], j, IVORY)
            if style == 'v_head':
                hline(p, f[s], 0, GOLD)
        if style == 'v_neck':
            fx, fy, fw, fh = f['front']
            for j in range(fh):
                for x in (fw // 2 - 1, fw // 2):
                    p.lit((fx + x, fy + j), (255, 240, 180), glow=0.5)
    if style == 'v_peg':
        p.shaded(f, IVORY)
    if style == 'v_body':                                          # the pear-shaped body: rosewood, a gold phoenix on its face, strings shining
        def mask(s, t):
            half = 0.24 + 0.26 * math.sin(min(1.0, t * 1.15) * math.pi * 0.62)
            return abs(s - 0.5) < half and t < 0.98
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            cutout(p, f[s], mask, lambda s_, t: k((140, 70, 36), 1.15 - 0.4 * abs(s_ - 0.45)))
            if s == 'front':
                for i, j, xy in each(f[s]):                        # the soundboard, pale paulownia, a gold phoenix
                    s_, t = (i + 0.5) / fw, (j + 0.5) / fh
                    if mask(s_, t) and abs(s_ - 0.5) < 0.38 and 0.12 < t < 0.92 and p.img.getpixel(xy)[3]:
                        p.px(xy, noisy(p, (226, 196, 140), 3))
                cloud_mark(p, fx + fw // 2 - 3, fy + fh // 2, GOLD_DEEP, 2, f[s])
                for j in range(fh * 2 // 10, fh * 9 // 10):        # the four strings
                    for x in (fw // 2 - 2, fw // 2 - 1, fw // 2, fw // 2 + 1):
                        if p.img.getpixel((fx + x, fy + j))[3]:
                            p.lit((fx + x, fy + j), (255, 240, 190), glow=0.55)
                for i in range(fw // 2 - 3, fw // 2 + 3):          # the bridge
                    p.px((fx + i, fy + fh * 8 // 10), (90, 40, 20))
        for s in ('right', 'left', 'top', 'bottom'):
            p.face(f[s], (110, 54, 28), LIGHTS[s], noise=3)

    # 6 Water's scroll
    if style == 'me_roller':
        p.shaded(f, (40, 30, 30))
        for s in SIDES + ('top', 'bottom'):
            fx, fy, fw, fh = f[s]
            if s in SIDES:
                for i in list(range(2)) + list(range(fw - 2, fw)):
                    vline(p, f[s], i, IVORY if s != 'bottom' else IVORY_SHADE)
    if style == 'me_sheet':                                        # silk paper, blue-mounted, its writing shining
        for s, r in f.items():
            p.face(r, (232, 226, 206), LIGHTS[s], noise=3)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            border(p, f[s], (60, 100, 170), inner=(90, 140, 210), width=2)
            if s == 'front':
                for col in range(fw - 5, 3, -3):                   # columns of characters, top to bottom, right to left
                    j = 4
                    while j < fh - 5:
                        h = p.rnd.randint(2, 3)
                        for jj in range(h):
                            for ii in range(2):
                                if p.rnd.random() < 0.7:
                                    p.lit((fx + col + ii - 1, fy + j + jj), (80, 170, 255), glow=0.85, skin=(30, 40, 80))
                        j += h + 1

    # 7 the eclipse: a black disc, its rim burning white-gold, a corona of red flame about it
    if style == 'ra_disc':
        def front(r, a, d):
            lick = 0.08 * math.sin(a * 11) + 0.04 * math.sin(a * 23)
            if r > 0.95 + lick:
                return None
            if r > 0.72:
                return ('glow', mixc((255, 220, 140), (200, 40, 20), (r - 0.72) / 0.28), 0.85)
            if r > 0.64:
                return ('glow', (255, 250, 230), 1.0)
            return (10 + int(10 * r), 8, 14)
        disc(p, f['front'], front)
        disc(p, f['back'], lambda r, a, d: None if r > 0.95 + 0.08 * math.sin(a * 11) + 0.04 * math.sin(a * 23)
             else ('glow', (220, 70, 30), 0.6) if r > 0.68 else (40, 30, 40))
        bare(p, f)

    # 8 the comet: a white-hot head in a great violet coma, a four-pointed flare; its tails streaming
    if style == 'ke_head':
        for s, r in f.items():
            for i, j, xy in each(r):
                d = math.hypot(i + 0.5 - r[2] / 2, j + 0.5 - r[3] / 2) / (r[2] / 2)
                p.lit(xy, mixc((255, 255, 255), (226, 196, 255), min(1, d) ** 1.5), glow=1.0)
    if style in ('ke_star', 'ke_star_x', 'ke_star_y'):
        flat = {'ke_star': ('front', 'back'), 'ke_star_x': ('right', 'left'), 'ke_star_y': ('top', 'bottom')}[style]
        spikes = style != 'ke_star_y'
        for s in flat:
            for i, j, xy in each(f[s]):
                dx, dy = ((i + 0.5) / f[s][2] - 0.5) * 2, ((j + 0.5) / f[s][3] - 0.5) * 2
                r = math.hypot(dx, dy)
                lo, hi = min(abs(dx), abs(dy)), max(abs(dx), abs(dy))
                spike = spikes and lo < 0.075 * (1 - hi) + 0.02 and hi < 0.98
                ray = spikes and abs(abs(dx) - abs(dy)) < 0.06 * (1 - r) and r < 0.62
                edge = 0.5 + 0.06 * math.sin(math.atan2(dy, dx) * 7)
                if r < 0.2:
                    c, g = mixc((255, 255, 255), (240, 222, 255), r / 0.2), 1.0
                elif r < edge:
                    t = (r - 0.2) / (edge - 0.2)
                    c, g = mixc((226, 190, 255), (124, 56, 220), t ** 0.8), 0.95 - 0.35 * t
                    if t > 0.7 and p.rnd.random() < (t - 0.7) * 1.6:
                        c, g = None, 0
                elif spike or ray:
                    c, g = mixc((250, 240, 255), (170, 110, 255), hi), 1.0 - 0.4 * hi
                elif r < 0.9 and p.rnd.random() < 0.035:                # sparks thrown off about it
                    c, g = (246, 226, 255), 1.0
                else:
                    c, g = None, 0
                if c is None:
                    p.clear(xy)
                else:
                    p.lit(xy, c, glow=g)
        bare(p, f, [s for s in ('front', 'back', 'right', 'left', 'top', 'bottom') if s not in flat])
    if style in ('ke_trail0', 'ke_trail1'):                        # the tail's bright core, streaming out of the head
        for s, r in f.items():
            for i, j, xy in each(r):
                a = (along_z(s, i, j, r[2], r[3]) + (0 if style == 'ke_trail0' else 1)) / 2
                p.lit(xy, mixc((255, 250, 255), (176, 120, 250), a), glow=1.0 - 0.3 * a)
    if style.startswith(('ke_tail', 'ke_ion')):
        # a tail: streaks running along it, bright at its heart, widening and fading from white through
        # violet (the dust tail) or ice-blue (the ion tail) to nothing, fraying into sparks at its end
        ion = style.startswith('ke_ion')
        stage = int(style[7 if not ion else 6])
        stages = 3 if ion else 5
        flat = ('right', 'left') if style.endswith('_x') else ('top', 'bottom')
        hot, mid, deep = (((236, 248, 255), (120, 180, 255), (70, 90, 220)) if ion
                          else ((250, 236, 255), (176, 110, 250), (96, 30, 180)))
        rnd = random.Random(sum(ord(ch) * (n + 1) for n, ch in enumerate(style)))
        for s in flat:
            fx, fy, fw, fh = f[s]
            phase = [rnd.random() * 6.3 for _ in range(3)]
            for i, j, xy in each(f[s]):
                along = along_z(s, i, j, fw, fh)
                u = (i + 0.5) / fw if s in ('top', 'bottom') else (j + 0.5) / fh
                across = abs(u - 0.5) * 2
                a = (stage + along) / stages                       # 0 at the head .. 1 at the tail's end
                streak = 0.5 + 0.5 * math.sin(u * (9 if ion else 13) + phase[0]) * math.sin(u * 5 + phase[1])
                reach = (0.55 + 0.4 * a) * (0.85 + 0.15 * streak)
                if across > reach:
                    p.clear(xy)
                    continue
                start = 0.35 if ion else 0.55
                if a > start and rnd.random() < (a - start) * (1.6 if ion else 1.9) * (0.6 + 0.6 * (1 - streak)):
                    if rnd.random() < 0.06:                         # a spark left where the tail frays
                        p.lit(xy, hot, glow=0.9)
                    else:
                        p.clear(xy)
                    continue
                heart = max(0.0, 1 - across / max(0.2, reach)) ** 1.3
                c = mixc(mid, deep, min(1, a * 1.1 + across * 0.4))
                c = mixc(c, hot, heart * (1 - a) * 0.9 + 0.25 * streak * heart)
                p.lit(xy, c, glow=(0.75 if ion else 0.95) - 0.45 * a)
        bare(p, f, [s for s in ('front', 'back', 'right', 'left', 'top', 'bottom') if s not in flat])
