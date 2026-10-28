"""The nine Kings of Hell but King Yama, as Hell's temples sculpt them beside him: lesser judge-kings
in his likeness. As king_yama.py, their model and skins come from one list of parts (beastgen): the
model is written into HellRenderers' kingLayer() between its GENERATED kings markers, and each king's
skin painted to the same box-UV layout at twice the model's resolution, with a glowing skin drawn
full-bright over it (what of his emblem shines).

One frame for the nine, a head shorter and narrower than King Yama's and plainer: the same tiered
judge's robe with its cloud collar, jade belt, front apron and side panels, great hanging sleeves and
court boots, the same mian crown (a tall cap banded in gold, a plaque, the flat board hung with
strings of beads); but a man's stern face and a long beard in three locks where Yama has a demon's,
a plain cloud-collar plate on each shoulder where he has armour and gold beasts, no skulls or
flames, no chain, no ring of fire; the robe in plain silk, gold only at its edges. Each king in his
own colour, an ivory court tablet (笏) in his right hand and his court's emblem in his left (the
renderer shows only his), the same emblem worked in gold on the rank badge (补子) on his breast and
back:

   1 Qinguang   deep blue      the Karma Mirror (业镜), bronze, its glass shining
   2 Chujiang   ice blue       a sceptre of ice crystals, faintly shining; frost along his hem
   3 Songdi     slate teal     an iron hook, a chain under it
   4 Wuguan     jade green     the scales, a gold balance on a post
   6 Biancheng  purple         the bronze bell of the Wailing City
   7 Taishan    vermilion      a bronze cauldron of boiling oil, fire licking from it; flames along his hem
   8 Dushi      ochre-brown    an iron-shod cart wheel
   9 Pingdeng   white          平, "Equal", in gold on a vermilion plaque
  10 Zhuanlun   black and gold the golden Wheel of Reincarnation, its six realms, its hub shining

Run from the repository root: python assets_source/hell_kings.py"""
import math

import beastgen as bg
from beastgen import Painter, emit
from fire_court import part, mirrored
from sea_court import aim, fabric, LIGHT
from king_yama import paint, edges, vedges, strands, point, lock, circle, flames_band, mixc, k, noisy, \
    BLUE_BLACK, BROWN_BLACK, ASH

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/HellRenderers.java'

GOLD = (226, 180, 66)
GOLD_DEEP = (150, 104, 36)
JADE = (96, 178, 136)
JADE_DEEP = (52, 116, 86)
IVORY = (234, 224, 198)
IVORY_SHADE = (190, 178, 150)
BLACK = (24, 20, 24)
BRONZE = (168, 112, 52)
BRONZE_DEEP = (96, 60, 30)
BRONZE_GREEN = (82, 128, 106)       # verdigris
IRON = (78, 76, 84)
IRON_DARK = (36, 34, 40)
STEEL = (170, 174, 186)
WOOD = (120, 80, 44)
WOOD_DEEP = (74, 46, 24)
HAIR = (20, 16, 18)
LIPS = (120, 52, 48)
FIRE_DEEP = (150, 20, 8)
FIRE = (232, 76, 20)
FIRE_HOT = (255, 186, 70)

# How much larger than his pixels each is drawn: KingRenderer.SIZE (some 2.7 blocks to his crown; King Yama 3.4)
SIZE = 0.82

WHITE_HAIR = ((120, 120, 128), (196, 196, 204), (228, 228, 234), (252, 252, 255))
RED_BROWN = ((30, 10, 6), (96, 36, 18), (150, 66, 34), (196, 110, 70))

# court: name, robe, its deeper shade, the under-robe and linings, face, beard (a hair tone), beads, the plaque's jewel
KINGS = {
    1: ('qinguang', (34, 58, 128), (18, 30, 72), (196, 52, 40), (226, 188, 156), BLUE_BLACK, (200, 40, 40), (90, 200, 230)),
    2: ('chujiang', (132, 184, 220), (70, 112, 156), (238, 246, 252), (214, 196, 196), ASH, (226, 244, 252), (200, 240, 255)),
    3: ('songdi', (34, 96, 104), (14, 48, 54), (150, 34, 30), (212, 180, 150), ASH, (200, 40, 40), (210, 50, 40)),
    4: ('wuguan', (36, 112, 66), (16, 58, 34), (226, 196, 120), (222, 186, 150), BROWN_BLACK, GOLD, (96, 210, 150)),
    6: ('biancheng', (98, 42, 128), (50, 18, 70), (230, 196, 90), (216, 184, 158), BLUE_BLACK, (200, 40, 40), (190, 90, 230)),
    7: ('taishan', (204, 84, 24), (120, 36, 10), (50, 22, 18), (174, 96, 66), RED_BROWN, GOLD, (255, 140, 40)),
    8: ('dushi', (140, 98, 48), (78, 50, 22), (60, 76, 70), (208, 170, 136), BROWN_BLACK, (200, 40, 40), (120, 200, 120)),
    9: ('pingdeng', (226, 220, 202), (160, 150, 128), (60, 108, 92), (230, 200, 176), WHITE_HAIR, JADE, (110, 210, 170)),
    10: ('zhuanlun', (30, 28, 34), (12, 10, 14), (204, 164, 52), (220, 188, 160), BLUE_BLACK, GOLD, (255, 210, 90)),
}

ARM, FOREARM = (-0.08, 0, 0.16), (-1.05, -0.5, 0)
LARM = (-0.1, 0, -0.4)          # his left arm out from his side
# his left forearm raised, his emblem held up beside his head for all to see
LFOREARM = (-2.2, -0.35, 0)
# how each king's emblem is held: upright, facing ahead, turned a little toward the middle of him
HELD = aim([LARM, LFOREARM], (0, -0.2, 0))


# the emblems drawn this much larger than they are laid out below, to be seen across a hall
GRAND = 1.3


def held(name, boxes, pivot=(0.4, 1.6, -1.2), rot=None, children=()):
    """An emblem in his left fist (its parts laid out about his grip, upright, its face ahead), drawn GRAND."""
    g = lambda b: tuple(v * GRAND for v in b[:6]) + (b[6],)
    out = [part('emblem_' + name, 'left_hand', pivot, rot or HELD, [g(b) for b in boxes])]
    for n, parent, pv, r, bx, m in children:
        out.append((n, parent, tuple(v * GRAND for v in pv), r, [g(b) for b in bx], m))
    return out


def emblems():
    out = []
    # 1 the Karma Mirror: a round bronze mirror on a handle, a cloud bracket under it
    out += held('qinguang', [
        (-0.7, -3.4, -0.7, 1.4, 4.4, 1.4, 'q_handle'),
        (-2.4, -4.6, -1, 4.8, 1.4, 2, 'q_bracket'),
        (-5.5, -15.6, -0.6, 11, 11, 1.2, 'q_mirror'),
    ])
    # 2 a sceptre of ice: a silver mount, a long crystal and two lesser ones leaning from it
    out += held('chujiang', [
        (-0.6, -2, -0.6, 1.2, 4, 1.2, 'i_grip'),
        (-1.9, -3.4, -1.9, 3.8, 1.6, 3.8, 'i_mount'),
        (-1.4, -14, -1.4, 2.8, 10.8, 2.8, 'i_crystal'),
    ], children=[
        part('emblem_chujiang_l', 'emblem_chujiang', (0.8, -3.6, 0), (0, 0, 0.5), [(-1, -7.5, -1, 2, 7.5, 2, 'i_shard')]),
        part('emblem_chujiang_r', 'emblem_chujiang', (-0.8, -3.6, 0), (0, 0, -0.5), [(-1, -7.5, -1, 2, 7.5, 2, 'i_shard')]),
        part('emblem_chujiang_f', 'emblem_chujiang', (0, -3.6, -0.8), (-0.45, 0, 0), [(-0.8, -5.5, -0.8, 1.6, 5.5, 1.6, 'i_shard_small')]),
    ])
    # 3 an iron hook (as a hook sword's head): a shaft up from his fist, its head curving over and down outward,
    # narrowing to a long honed point turned back in; a spur on its back; a chain under the fist
    R, cx, cy, ends = 4.2, 4.2, -13.0, (180, 425)
    n = 8
    arc = []
    for kk in range(n):
        t0 = math.radians(ends[0] + (ends[1] - ends[0]) * kk / n)
        t1 = math.radians(ends[0] + (ends[1] - ends[0]) * (kk + 1) / n)
        tm = (t0 + t1) / 2
        length = 2 * R * math.sin((t1 - t0) / 2) + 0.7
        w = 1.9 - 0.7 * kk / (n - 1)
        arc.append(part('emblem_songdi_arc_%d' % kk, 'emblem_songdi', (cx + R * math.cos(tm), cy + R * math.sin(tm), 0),
                        (0, 0, tm + math.pi / 2), [(-length / 2, -w / 2, -0.8, length, w, 1.6, 'h_arc')]))
    te = math.radians(ends[1])
    arc.append(part('emblem_songdi_point', 'emblem_songdi', (cx + R * math.cos(te), cy + R * math.sin(te), 0), (0, 0, te + math.pi),
                    [(-0.6, -4.2, -0.6, 1.2, 4.6, 1.2, 'h_point')]))
    ts = math.radians(300)
    arc.append(part('emblem_songdi_spur', 'emblem_songdi', (cx + (R + 0.6) * math.cos(ts), cy + (R + 0.6) * math.sin(ts), 0),
                    (0, 0, ts + math.pi / 2), [(-0.6, -3, -0.6, 1.2, 3.2, 1.2, 'h_point')]))
    out += held('songdi', [
        (-0.75, -13.4, -0.75, 1.5, 15.4, 1.5, 'h_shaft'),
        (-1.1, -1, -1.1, 2.2, 1, 2.2, 'h_ring'),
        (-0.3, 2, -0.7, 0.6, 1.8, 1.4, 'h_link_a'),
        (-0.7, 3.5, -0.3, 1.4, 1.8, 0.6, 'h_link_b'),
        (-0.3, 5, -0.7, 0.6, 1.8, 1.4, 'h_link_a'),
        (-1.1, 6.6, -1.1, 2.2, 2.2, 2.2, 'h_ring'),
    ], children=arc)
    # 4 the scales: a post up from his fist, the beam level across its top, a pan hanging at each end
    out += held('wuguan', [
        (-0.6, -12, -0.6, 1.2, 13, 1.2, 's_post'),
        (-6.2, -13.1, -0.65, 12.4, 1.3, 1.3, 's_beam'),
        (-1.1, -15.6, -0.45, 2.2, 2.6, 0.9, 's_pointer'),
        (-5.8, -11.8, -0.2, 0.4, 4.4, 0.4, 's_cord'),
        (5.4, -11.8, -0.2, 0.4, 4.4, 0.4, 's_cord'),
        (-7.8, -7.4, -2.2, 4.4, 1, 4.4, 's_pan'),
        (3.4, -7.4, -2.2, 4.4, 1, 4.4, 's_pan'),
        (-1.4, 0.8, -1.4, 2.8, 1, 2.8, 's_foot'),
    ], pivot=(2.6, 1.6, -1.2))
    # 6 the bell of the Wailing City: a great handbell, its handle in his fist, the bell under it, round-shouldered,
    # flaring a little to its lip, its clapper showing
    out += held('biancheng', [
        (-0.7, -3.4, -0.7, 1.4, 3.8, 1.4, 'b_handle'),
        (-1.2, -4.8, -1.2, 2.4, 1.6, 2.4, 'b_knob'),
        (-3.8, 0.2, -3.8, 7.6, 8.4, 7.6, 'b_bell'),
        (-4.4, 8.4, -4.4, 8.8, 1.2, 8.8, 'b_lip'),
        (-0.8, 9.4, -0.8, 1.6, 2.2, 1.6, 'b_clapper'),
    ], pivot=(0.4, 2.6, -1.2))
    # 7 a bronze cauldron (鼎) of boiling oil on his palm: four legs, two ears, fire licking up out of it
    out += held('taishan', [
        (-3.4, -5.4, -2.8, 6.8, 4.2, 5.6, 'c_body'),
        (-3.9, -6.4, -3.3, 7.8, 1, 6.6, 'c_rim'),
        (-3.2, -8.6, -0.6, 1.2, 2.2, 1.2, 'c_ear'),
        (2, -8.6, -0.6, 1.2, 2.2, 1.2, 'c_ear'),
        (-2.9, -1.2, -2.4, 1.2, 1.8, 1.2, 'c_leg'),
        (1.7, -1.2, -2.4, 1.2, 1.8, 1.2, 'c_leg'),
        (-2.9, -1.2, 1.2, 1.2, 1.8, 1.2, 'c_leg'),
        (1.7, -1.2, 1.2, 1.2, 1.8, 1.2, 'c_leg'),
    ], pivot=(0.4, -0.6, -1.2), children=[
        part('emblem_taishan_fire', 'emblem_taishan', (0, -6.4, 0), (0, 0.4, 0), [
            (-2.8, -6, -0.25, 5.6, 6, 0.5, 'c_flame'), (-0.25, -6, -2.8, 0.5, 6, 5.6, 'c_flame_x')]),
    ])
    # 8 a cart wheel, iron-shod, gripped at its rim
    out += held('dushi', [(-6, -12.6, -0.7, 12, 12, 1.4, 'w_cart'), (-1.2, -7.8, -1.6, 2.4, 2.4, 3.2, 'w_hub')], pivot=(0.4, 1.4, -1.2))
    # 9 平, "Equal": a command plaque on a short pole, vermilion in a gold frame, the character in gold
    out += held('pingdeng', [
        (-0.6, -6.5, -0.6, 1.2, 8.5, 1.2, 'p_pole'),
        (-4.8, -18, -0.6, 9.6, 11.5, 1.2, 'p_plaque'),
        (-3.6, -19.8, -0.5, 7.2, 1.8, 1, 'p_crest'),
        (-1.4, 1.2, -1.4, 2.8, 1.2, 2.8, 'p_foot'),
    ])
    # 10 the Wheel of Reincarnation: gold, its six realms between six spokes, flames round its rim, its hub shining
    out += held('zhuanlun', [(-7, -15, -0.5, 14, 14, 1, 'r_wheel'), (-1.4, -9.4, -1.5, 2.8, 2.8, 3, 'r_hub')], pivot=(0.4, 1.4, -1.2))
    return out


PARTS = mirrored([
    # the body from the waist up; its pivot the waist
    part('body', 'root', (0, 4, 0), (0, 0, 0), [
        (-6, -15, -5, 12, 15, 10, 'chest'),
        (-3.8, -16.2, -3.2, 7.6, 1.6, 6.4, 'collar'),
        (-2.2, -18.4, -2.2, 4.4, 3.6, 4.4, 'neck'),
        (-7.4, -15.6, -6, 14.8, 0.8, 12, 'cloudtop'),
        (-6.6, -15.2, -6.1, 13.2, 5, 0.5, 'lappet'),
        (-6.6, -15.2, 5.6, 13.2, 6, 0.5, 'lappet_back'),
        (-6.8, -4.4, -5.9, 13.6, 2.2, 11.8, 'jadebelt'),
        (-2, -5, -6.6, 4, 3, 0.8, 'beltplaque'),
    ]),
    part('skirt', 'body', (0, 0, 0), (0, 0, 0), [
        (-6.6, 0, -5.9, 13.2, 7, 11.8, 'robe1'),
        (-7.6, 6.5, -6.6, 15.2, 7, 13.2, 'robe2'),
        (-9, 13, -7.6, 18, 7, 15.2, 'hem'),
    ]),
    part('apron', 'skirt', (0, -1, -6.3), (-0.12, 0, 0), [(-3.4, 0, -1, 6.8, 21, 1, 'apron')]),
    part('right_panel', 'skirt', (-5.4, -1, -6), (-0.1, 0.35, 0.05), [(-2.2, 0, -0.5, 4.4, 20.5, 0.5, 'panel')]),
    part('back_panel', 'skirt', (0, -1.5, 6.2), (0.12, 0, 0), [(-4.5, 0, 0, 9, 21, 0.6, 'backpanel')]),
    part('right_boot', 'root', (-3.2, 22.4, -7), (0, 0.12, 0), [
        (-1.7, 0, -2.4, 3.4, 1.6, 4.2, 'boot'), (-1.2, -0.9, -3.2, 2.4, 1.4, 1, 'boottoe')]),
    # the arms: a cloud-collar plate on the shoulder, the forearms brought forward, great sleeves hanging from them
    part('right_arm', 'body', (-7.8, -13.4, 0), ARM, [(-3.4, -2, -2.8, 4.8, 9, 5.6, 'sleeve')]),
    part('right_shoulder', 'right_arm', (-1.2, -2.2, 0), (0, 0, 0.3), [(-4.4, -1.2, -3.9, 5.8, 2, 7.8, 'shoulder')]),
    part('right_forearm', 'right_arm', (-1, 6.6, 0), FOREARM, [
        (-2.3, 0, -2.3, 4.6, 7, 4.6, 'forearm'),
        (-2.7, 5, -2.7, 5.4, 2, 5.4, 'cuff'),
    ]),
    part('right_sleeve', 'right_forearm', (0, 0.4, 2.1), aim([ARM, FOREARM], (0.05, -0.3, 0.14)), [
        (-3.2, -1, -9, 6.4, 12, 9, 'bigsleeve')]),
    part('right_hand', 'right_forearm', (0, 7.2, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3, 3, 3, 'hand')]),
    # the ivory court tablet, held up in his right hand
    part('tablet', 'right_hand', (0, 1.6, -0.2), aim([ARM, FOREARM], (0.15, 0, -0.1)), [
        (-1.3, -12, -0.35, 2.6, 14, 0.7, 'tablet')]),
    # the head: a man's, stern
    part('head', 'body', (0, -18.4, -0.8), (0, 0, 0), [
        (-4.4, -9, -4, 8.8, 7.4, 8, 'face'),
        (-1, -5.8, -4.8, 2, 2.4, 0.9, 'nose'),
        (-4.6, -9.3, -0.6, 9.2, 8, 5, 'hair_back'),
    ]),
    part('right_brow', 'head', (-0.4, -6.5, -4.3), (0, 0.1, 0.24), [(-3.8, -0.9, -0.5, 3.8, 1.1, 1, 'brow')]),
    part('right_moustache', 'head', (-0.5, -3.3, -4.5), (0, 0.2, -0.55), [(-3.2, -0.5, -0.5, 3.2, 1, 1, 'moustache')]),
    part('jaw', 'head', (0, -2, -0.6), (0, 0, 0), [(-4.1, 0, -3.4, 8.2, 2.6, 6.8, 'jaw')]),
    # the beard in three locks: a long one down the middle, one either side
    part('beard', 'jaw', (0, 2, -3.4), (0.12, 0, 0), [(-1.4, 0, -0.9, 2.8, 9, 1.8, 'lock_c')]),
    part('beard_tip', 'beard', (0, 8.6, 0), (-0.1, 0, 0), [(-1, 0, -0.8, 2, 4.5, 1.6, 'lock_tip')]),
    part('right_beard', 'jaw', (-2.5, 1.6, -3.1), (0.12, 0.15, 0.22), [(-1.1, 0, -0.8, 2.2, 7.5, 1.6, 'lock_side')]),
    # the mian crown: a tall cap banded in gold, a plaque on its front, a hairpin through it; the board, its beads
    part('crown', 'head', (0, -9, 0), (0, 0, 0), [
        (-3.6, -4, -3.4, 7.2, 4.2, 6.8, 'crown_cap'),
        (-4.7, -0.9, -4.3, 9.4, 1.4, 8.6, 'crown_band'),
        (-2, -3.7, -3.9, 4, 3, 0.6, 'crown_plaque'),
        (-6, -3.3, -0.35, 12, 0.7, 0.7, 'crown_hairpin'),
    ]),
    part('board', 'crown', (0, -4, 0), (0.07, 0, 0), [(-5.8, -1.4, -7.6, 11.6, 1.4, 14, 'board'),
                                                      (-6, -0.4, -7.8, 12, 0.4, 14.4, 'board_rim')]
         + [(-4 + 2 * kk - 0.25, 0, -7.3, 0.5, 3.6, 0.5, 'beads') for kk in range(5)]
         + [(-4 + 2 * kk - 0.25, 0, 5.8, 0.5, 3.6, 0.5, 'beads') for kk in range(5)]),
] + emblems())


def raise_left(parts):
    """The left arm (mirrored from the right) out from his side and its forearm raised to hold up his emblem,
    its sleeve still hanging, its shoulder plate still lying on his shoulder."""
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


class KingPainter(Painter):
    def clear(self, xy):
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            self.img.putpixel((x, y), (0, 0, 0, 0))
            self.glow.putpixel((x, y), (0, 0, 0, 0))

    def lit(self, xy, colour, glow=1.0, skin=None, **_):
        """A shining pixel: on the skin `skin` (or the colour), on the glowing skin the colour at `glow`."""
        x, y = int(xy[0]), int(xy[1])
        if not (0 <= x < bg.TEX and 0 <= y < bg.TEX):
            return
        self.px(xy, skin if skin is not None else colour)
        if glow:
            self.glow.putpixel((x, y), tuple(max(0, min(255, int(v * glow))) for v in colour[:3]) + (255,))


# ── The emblems, as badges: 11x11 glyphs in gold ─────────────────────────────

GLYPHS = {
    'qinguang': ['...#####...',
                 '..#.....#..',
                 '.#..##...#.',
                 '#..#......#',
                 '#..#......#',
                 '#.........#',
                 '#.........#',
                 '.#.......#.',
                 '..#.....#..',
                 '...#####...',
                 '....###....'],
    'chujiang': ['.....#.....',
                 '..#..#..#..',
                 '...#.#.#...',
                 '#...###...#',
                 '.#.#####.#.',
                 '#####.#####',
                 '.#.#####.#.',
                 '#...###...#',
                 '...#.#.#...',
                 '..#..#..#..',
                 '.....#.....'],
    'songdi': ['...#####...',
               '..#.....#..',
               '..#......#.',
               '..#......#.',
               '..#.#....#.',
               '..#.##..#..',
               '..#..###...',
               '..#........',
               '..#........',
               '.###.......',
               '..#........'],
    'wuguan': ['.....#.....',
               '....###....',
               '###########',
               '#....#....#',
               '#....#....#',
               '##...#...##',
               '###..#..###',
               '.....#.....',
               '.....#.....',
               '...#####...',
               '...........'],
    'biancheng': ['....###....',
                  '....#.#....',
                  '...#####...',
                  '..#.....#..',
                  '..#.###.#..',
                  '..#.....#..',
                  '.#.......#.',
                  '.#..###..#.',
                  '###########',
                  '.....#.....',
                  '....###....'],
    'taishan': ['.#..#..#...',
                '#..#..#..#.',
                '.#..#..#...',
                '...........',
                '#.#######.#',
                '###########',
                '.#.......#.',
                '.#.#####.#.',
                '..#######..',
                '..#.....#..',
                '.##.....##.'],
    'dushi': ['...#####...',
              '.##..#..##.',
              '.#.#.#.#.#.',
              '#...###...#',
              '#..#####..#',
              '#####.#####',
              '#..#####..#',
              '#...###...#',
              '.#.#.#.#.#.',
              '.##..#..##.',
              '...#####...'],
    'pingdeng': ['###########',
                 '...........',
                 '.#...#...#.',
                 '..#..#..#..',
                 '...#.#.#...',
                 '...........',
                 '###########',
                 '.....#.....',
                 '.....#.....',
                 '.....#.....',
                 '.....#.....'],
    'zhuanlun': ['#....#....#',
                 '.#.#####.#.',
                 '..##.#.##..',
                 '.#.#.#.#.#.',
                 '.##..#..##.',
                 '#####.#####',
                 '.##..#..##.',
                 '.#.#.#.#.#.',
                 '..##.#.##..',
                 '.#.#####.#.',
                 '#....#....#'],
}


def glyph(p, name, x, y, colour, scale=1):
    for j, row in enumerate(GLYPHS[name]):
        for i, c in enumerate(row):
            if c == '#':
                for a in range(scale):
                    for b in range(scale):
                        p.px((x + i * scale + a, y + j * scale + b), colour)


def badge(p, rect, cx, top, ground, name):
    """The rank badge (补子): a square of the under-robe's colour, gold-bordered, the king's emblem in gold."""
    x0, y0 = int(cx - 8), top
    for j in range(16):
        for i in range(16):
            edge = min(i, j, 15 - i, 15 - j)
            p.px((x0 + i, y0 + j), GOLD if edge == 0 else GOLD_DEEP if edge == 1 else noisy(p, ground, 3))
    ink = GOLD if sum(ground) < 400 else GOLD_DEEP
    glyph(p, name, x0 + 3, y0 + 3, ink)


# ── The skin ─────────────────────────────────────────────────────────────────

def king_skin(court):
    name, robe, deep, under, face, beard, beads, jewel = KINGS[court]
    face_shade = k(face, 0.72)
    sides = ('front', 'back', 'right', 'left')
    dark = sum(robe) < 160

    def silk(p, f, base=None, folds=True):
        base = base or robe
        low = mixc(base, deep, 0.6) if base is robe else k(base, 0.6)
        for s, r in f.items():
            if s == 'bottom':
                p.face(r, k(low, 0.9), 1.0, noise=2)
            elif s == 'top':
                fabric(p, r, base, low, 1.04, 7, fall=0.1)
            else:
                fabric(p, r, base, low, LIGHT[s], 7 if folds else 99, 0.35)

    def hem_trim(p, rect, rows):
        """The robe's lower border: a band of the under-robe's colour between gold lines."""
        fx, fy, fw, fh = rect
        for j in range(rows):
            for i in range(fw):
                p.px((fx + i, fy + fh - 1 - j), noisy(p, under, 3))
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), GOLD)
            p.px((fx + i, fy + fh - rows - 1), GOLD)
            p.px((fx + i, fy + fh - rows - 2), GOLD_DEEP)

    def paint_king(p, style, f, dims):
        # parts of other kings' emblems are left bare: the renderer shows only his
        if style[:2] in ('q_', 'i_', 'h_', 's_', 'b_', 'c_', 'w_', 'p_', 'r_') and EMBLEM_OF[style[0]] != name:
            return

        if style == 'chest':
            silk(p, f)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]                              # 24x30
                for j in range(fh):                                # gold-bordered side seams
                    p.px((fx, fy + j), GOLD_DEEP)
                    p.px((fx + fw - 1, fy + j), GOLD_DEEP)
                badge(p, f[s], fx + fw / 2, fy + 11, under if not dark else k(under, 0.9), name)

        if style == 'collar':
            p.shaded(f, under)
            edges(p, f, GOLD)
        if style == 'neck':
            p.shaded(f, face_shade)

        if style == 'cloudtop':                                    # the cloud collar: the robe's deep shade, gold-edged
            p.shaded(f, deep)
            fx, fy, fw, fh = f['top']
            cx, cy = fx + fw / 2, fy + fh / 2
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    a = math.atan2(y - cy, (x - cx) * fh / fw)
                    if abs(math.sin(a * 4)) < 0.14:
                        p.px((x, y), GOLD_DEEP)
            edges(p, f, GOLD)

        if style in ('lappet', 'lappet_back'):                     # the cloud collar's points: plain, gold-edged
            for kk, r in f.items():
                p.face(r, deep, 1.0, noise=4)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    t = (i % 9) / 8
                    depth = int(fh * (1 - abs(t - 0.5) * 2) * 0.85) + 2
                    for j in range(fh):
                        if j >= depth:
                            p.clear((fx + i, fy + j))
                        elif j >= depth - 2:
                            p.px((fx + i, fy + j), GOLD)

        if style == 'jadebelt':                                    # jade plaques on dark leather, gold-rimmed
            p.shaded(f, (52, 30, 26))
            for s in sides:
                fx, fy, fw, fh = f[s]
                for i in range(1, fw - 2, 6):
                    for j in range(1, fh - 1):
                        for w in range(3):
                            p.px((fx + i + w, fy + j), JADE if w < 2 else JADE_DEEP)
                    p.px((fx + i, fy + 1), (170, 226, 196))
            edges(p, f, GOLD)

        if style == 'beltplaque':                                  # a gold plaque, his jewel set in it
            p.shaded(f, GOLD_DEEP)
            fx, fy, fw, fh = f['front']                            # 8x6
            p.face((fx, fy, fw, fh), GOLD, 1.0, noise=6)
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), GOLD_DEEP)
            for i in range(3, 5):
                for j in range(2, 4):
                    p.px((fx + i, fy + j), jewel if (i, j) != (3, 2) else (255, 255, 255))

        if style in ('robe1', 'robe2', 'hem'):                     # the tiers: plain silk, gold at each tier's foot
            silk(p, f, robe if style != 'robe2' else mixc(robe, deep, 0.18))
            for s in sides:
                fx, fy, fw, fh = f[s]
                if style == 'hem':
                    hem_trim(p, f[s], 6)
                    if name == 'chujiang':                         # frost creeping up his hem
                        for i in range(fw):
                            reach = 4 + int(4 * abs(math.sin(i * 0.7))) + p.rnd.randint(0, 2)
                            for j in range(reach):
                                p.px((fx + i, fy + fh - 9 - j), mixc((250, 252, 255), robe, j / reach * 0.8))
                    if name == 'taishan':                          # flames licking up from his hem
                        flames_band(p, f[s], fh - 22, 14, FIRE_DEEP, (236, 120, 30), every=5)
                else:
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 1), GOLD)
                        p.px((fx + i, fy + fh - 2), GOLD_DEEP)
            if style == 'robe1':
                fx, fy, fw, fh = f['front']                        # the under-robe's opening
                for j in range(fh):
                    for i in range(fw // 2 - 4, fw // 2 + 4):
                        p.px((fx + i, fy + j), GOLD if abs(i - fw // 2 + 0.5) > 3 else noisy(p, under, 3))

        if style == 'apron':                                       # the front panel: the under-robe's colour, gold-bordered, a fringe
            for kk, r in f.items():
                p.face(r, under, 1.0, noise=4, fall=0.15)
            fx, fy, fw, fh = f['front']                            # 14x42
            vedges(p, (fx, fy, fw, fh), GOLD)
            for i in range(fw):
                for j in (0, 1, 6, 7):
                    p.px((fx + i, fy + j), GOLD if j in (0, 7) else GOLD_DEEP)
            for j in range(10, fh - 10, 8):                        # a gold lozenge down it, here and there
                cx, cy = fx + fw // 2, fy + j + 3
                for (a, b) in ((0, -2), (-1, -1), (1, -1), (-2, 0), (2, 0), (-1, 1), (1, 1), (0, 2)):
                    p.px((cx + a, cy + b), GOLD_DEEP if sum(under) > 400 else GOLD)
            for i in range(fw):                                    # the fringe
                for j in range(fh - 5, fh):
                    if i % 2 == 0 or j < fh - 3:
                        p.px((fx + i, fy + j), GOLD if i % 4 < 2 else beads)
                    else:
                        p.clear((fx + i, fy + j))

        if style == 'panel':                                       # side panels: the robe's deep shade, gold-edged
            for kk, r in f.items():
                p.face(r, mixc(robe, deep, 0.5), 1.0, noise=4, fall=0.2)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                vedges(p, f[s], GOLD)
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), GOLD)
                    p.px((fx + i, fy + fh - 2), GOLD_DEEP)

        if style == 'backpanel':
            for kk, r in f.items():
                p.face(r, mixc(robe, deep, 0.5), 1.0, noise=4, fall=0.15)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                vedges(p, f[s], GOLD)
                for i in range(fw):
                    p.px((fx + i, fy), GOLD)
                    p.px((fx + i, fy + fh - 1), GOLD)
                hem_trim(p, f[s], 5)

        if style == 'boot':
            p.shaded(f, BLACK)
            for s in sides:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), IVORY)
                    p.px((fx + i, fy + fh - 2), (200, 192, 170))
        if style == 'boottoe':
            p.shaded(f, BLACK)
            edges(p, f, GOLD_DEEP, bottom=False)

        if style in ('sleeve', 'forearm'):
            silk(p, f)
        if style == 'shoulder':                                    # the cloud-collar plate on the shoulder
            p.shaded(f, deep)
            for s in sides + ('top',):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), GOLD)
                for i in range(2, fw - 1, 5):
                    p.px((fx + i, fy + (1 if s != 'top' else fh // 2)), GOLD)
            fx, fy, fw, fh = f['top']
            for j in range(fh):
                p.px((fx, fy + j), GOLD)
                p.px((fx + fw - 1, fy + j), GOLD)
        if style == 'cuff':
            p.shaded(f, under)
            edges(p, f, GOLD)

        if style == 'bigsleeve':                                   # the great sleeve: plain silk, a border of the lining's colour at its mouth
            silk(p, f)
            for s in sides:
                hem_trim(p, f[s], 7)
            fx, fy, fw, fh = f['bottom']
            for j in range(fh):
                for i in range(fw):
                    edge = min(i, j, fw - 1 - i, fh - 1 - j)
                    p.px((fx + i, fy + j), GOLD if edge == 0 else k(under, 0.7) if edge < 3 else (14, 10, 12))

        if style == 'hand':
            p.shaded(f, face)
            for s in ('front', 'right', 'left'):
                fx, fy, fw, fh = f[s]
                for i in range(0, fw, 2):
                    p.px((fx + i, fy + fh - 2), face_shade)

        if style == 'tablet':                                      # the ivory court tablet (笏)
            p.shaded(f, IVORY)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                vedges(p, f[s], IVORY_SHADE)
                for i in range(fw):
                    p.px((fx + i, fy), IVORY_SHADE)
                for j in range(fh - 7, fh):                        # the grip, wrapped
                    for i in range(fw):
                        p.px((fx + i, fy + j), (122, 32, 28) if j % 2 else (90, 24, 20))

        if style == 'face':                                        # a man's face, stern; his hair dressed up under the crown
            p.shaded(f, HAIR)
            for s in ('right', 'left', 'back', 'top'):
                strands(p, f[s], HAIR, (60, 44, 44))
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]                              # 16x15: skin before the ear, the ear
                for j in range(4, fh):
                    for i in range(6):
                        x = fx + (fw - 1 - i if s == 'right' else i)
                        p.px((x, fy + j), k(face, 0.92 - 0.03 * i))
                ex = fx + (fw - 9 if s == 'right' else 6)
                for j in range(5, 10):
                    for i in range(3):
                        p.px((ex + i, fy + j), k(face, 0.8) if i != 1 else face_shade)
            fx, fy, fw, fh = f['front']                            # 18x15
            for j in range(fh):
                for i in range(fw):
                    c = mixc(face, (255, 236, 220), 0.12) if (j < 4 and 5 < i < 12) else face
                    p.px((fx + i, fy + j), [v * (1 - 0.08 * j / fh) + p.rnd.randint(-2, 2) for v in c])
            for i in range(fw):
                p.px((fx + i, fy), HAIR)
            for j in range(2, 5):                                  # a furrow between his brows
                p.px((fx + 8, fy + j), face_shade)
                p.px((fx + 9, fy + j), k(face, 0.85))
            for side in (0, 1):                                    # eyes: stern, narrowed, dark
                ex = fx + (3 if side == 0 else fw - 3 - 5)
                for i in range(5):
                    p.px((ex + i, fy + 6), k(face_shade, 0.7))
                    p.px((ex + i, fy + 7), (236, 230, 216) if i not in (2, 3) else (30, 20, 18))
                    p.px((ex + i, fy + 8), k(face, 0.86))
                p.px((ex + (2 if side == 0 else 3), fy + 7), (10, 6, 6))
            for j in range(9, 12):                                 # lines from the nose
                p.px((fx + 5 - (j - 9) // 2, fy + j), face_shade)
                p.px((fx + fw - 6 + (j - 9) // 2, fy + j), face_shade)
            for i in range(6, fw - 6):
                p.px((fx + i, fy + fh - 1), LIPS)
            p.face(f['bottom'], face_shade, 1.0)

        if style == 'nose':
            p.shaded(f, face)
            fx, fy, fw, fh = f['front']
            p.px((fx, fy + fh - 1), face_shade)
            p.px((fx + fw - 1, fy + fh - 1), face_shade)
            p.face(f['bottom'], face_shade, 1.0)

        if style == 'hair_back':
            for kk, r in f.items():
                strands(p, r, HAIR, (60, 44, 44))

        if style in ('brow', 'moustache'):
            lock(p, f, along_x=True, tone=beard, grey=0.05)

        if style == 'jaw':                                         # the chin under the beard
            p.shaded(f, face)
            fx, fy, fw, fh = f['front']
            for j in range(fh):
                for i in range(fw):
                    if j >= 2 or i < 3 or i > fw - 4:
                        p.px((fx + i, fy + j), shade_beard(beard, (i + j) % 3))
            for s in ('right', 'left', 'bottom'):
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    for i in range(fw):
                        p.px((fx + i, fy + j), shade_beard(beard, (i + 2 * j) % 3))

        if style.startswith('lock_'):
            lock(p, f, keep=0.6 if style == 'lock_c' else 0.4, taper=0.3 if style != 'lock_tip' else 0.45, tone=beard, grey=0.05)

        if style == 'crown_cap':
            p.shaded(f, BLACK)
            for s in sides:
                fx, fy, fw, fh = f[s]
                for i in range(1, fw, 5):
                    for j in range(fh):
                        p.px((fx + i, fy + j), GOLD_DEEP)
                for i in range(fw):
                    p.px((fx + i, fy), GOLD)
        if style == 'crown_band':
            p.shaded(f, GOLD)
            for s in sides:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
        if style == 'crown_plaque':                                # a gold plaque, his jewel in it
            p.shaded(f, GOLD)
            fx, fy, fw, fh = f['front']                            # 8x6
            for j in range(1, fh - 1):
                for i in range(1, fw - 1):
                    p.px((fx + i, fy + j), deep if not dark else (60, 50, 40))
            for i in range(3, 5):
                for j in range(2, 4):
                    p.px((fx + i, fy + j), jewel if (i, j) != (3, 2) else (255, 255, 255))
        if style == 'crown_hairpin':
            p.shaded(f, JADE)
            for s in ('front', 'back', 'top', 'bottom'):
                fx, fy, fw, fh = f[s]
                for i in list(range(3)) + list(range(fw - 3, fw)):
                    for j in range(fh):
                        p.px((fx + i, fy + j), GOLD)
        if style == 'board':                                       # the board: black above, his colour beneath, gold-edged
            p.shaded(f, BLACK)
            p.face(f['bottom'], robe, 0.9, noise=4)
            fx, fy, fw, fh = f['top']
            for i in range(fw):
                p.px((fx + i, fy), GOLD)
                p.px((fx + i, fy + fh - 1), GOLD)
            for j in range(fh):
                p.px((fx, fy + j), GOLD)
                p.px((fx + fw - 1, fy + j), GOLD)
            for s in sides:
                p.face(f[s], GOLD, 1.0, noise=4)
        if style == 'board_rim':
            p.shaded(f, GOLD_DEEP)
        if style == 'beads':
            for kk, r in f.items():
                fx, fy, fw, fh = r
                for j in range(fh):
                    c = beads if (j // 2) % 2 == 0 else GOLD
                    for i in range(fw):
                        p.px((fx + i, fy + j), k(c, 1.1 if j % 2 == 0 else 0.75))

        paint_emblem(p, style, f, sides)

    return paint_king


EMBLEM_OF = {'q': 'qinguang', 'i': 'chujiang', 'h': 'songdi', 's': 'wuguan', 'b': 'biancheng', 'c': 'taishan',
             'w': 'dushi', 'p': 'pingdeng', 'r': 'zhuanlun'}


def ping(p, rect, ink, shadow):
    """平, in strokes two pixels broad, to fill the rect: a short bar at the top, two dots, a long bar, the upright."""
    fx, fy, fw, fh = rect

    def bar(x0, x1, y):
        for x in range(int(fx + fw * x0), int(fx + fw * x1) + 1):
            for d in (0, 1):
                p.px((x, int(fy + fh * y) + d), ink)
            p.px((x, int(fy + fh * y) + 2), shadow)

    bar(0.12, 0.88, 0.1)
    bar(0.0, 1.0, 0.6)
    cx = fx + fw // 2 - 1
    for y in range(int(fy + fh * 0.1), fy + fh):
        p.px((cx, y), ink)
        p.px((cx + 1, y), ink)
        p.px((cx + 2, y), shadow)
    for k_ in range(4):                                            # the two dots, slanting in toward the upright
        for d in (0, 1):
            p.px((int(fx + fw * 0.2) + k_ + d, int(fy + fh * 0.28) + k_), ink)
            p.px((int(fx + fw * 0.8) - k_ - d, int(fy + fh * 0.28) + k_), ink)


def shade_beard(tone, n):
    return tone[1] if n else tone[2]


def disc(p, rect, colour_at, clear_out=True):
    """A round plate painted on a square face: colour_at(r, a) for r 0 (centre) .. 1 (rim), a its angle; clear outside."""
    fx, fy, fw, fh = rect
    cx, cy, R = fx + fw / 2, fy + fh / 2, fw / 2
    for y in range(fy, fy + fh):
        for x in range(fx, fx + fw):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy) / R
            a = math.atan2(dy, dx)
            c = colour_at(r, a) if r <= 1 else None
            if c is None:
                if clear_out:
                    p.clear((x, y))
            elif c[0] == 'glow':
                p.lit((x, y), c[1], glow=c[2])
            else:
                p.px((x, y), c)


def bare_edges(p, f, which=('right', 'left', 'top', 'bottom')):
    for s in which:
        fx, fy, fw, fh = f[s]
        for j in range(fh):
            for i in range(fw):
                p.clear((fx + i, fy + j))


def metal(p, f, base, deep):
    """Cast metal: lit above, dark beneath, a bright glint at its top edges."""
    p.shaded(f, base)
    for s in ('front', 'back', 'right', 'left'):
        fx, fy, fw, fh = f[s]
        for i in range(fw):
            p.px((fx + i, fy), mixc(base, (255, 255, 255), 0.3))
            p.px((fx + i, fy + fh - 1), deep)


def paint_emblem(p, style, f, sides):
    # 1 the Karma Mirror
    if style == 'q_handle':
        metal(p, f, BRONZE, BRONZE_DEEP)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for j in range(1, fh, 3):
                for i in range(fw):
                    p.px((fx + i, fy + j), BRONZE_DEEP)
    if style == 'q_bracket':                                       # a cloud bracket
        metal(p, f, BRONZE, BRONZE_DEEP)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            if i % 3 == 1:
                p.px((fx + i, fy + 1), BRONZE_GREEN)
    if style == 'q_mirror':                                        # its glass bright and shining, a bronze rim worked with clouds; its back cast bronze
        def front(r, a):
            if r > 0.8:
                return BRONZE_DEEP if r > 0.95 else (GOLD if int((a + math.pi) * 8 / math.pi) % 2 else BRONZE)
            if r > 0.74:
                return BRONZE_DEEP
            t = r / 0.74
            c = mixc((236, 250, 255), (120, 190, 226), t)
            if -1.2 < a < -0.6 and 0.3 < r < 0.6:                 # a glint across the glass
                c = (255, 255, 255)
            return ('glow', c, 0.8)

        def back(r, a):
            if r > 0.86:
                return BRONZE_DEEP if r > 0.95 else BRONZE
            if r < 0.16:
                return GOLD                                        # the boss
            if abs(r - 0.5) < 0.05 or (0.55 < r < 0.8 and int((a + math.pi) * 6 / math.pi) % 2 == 0 and abs(r - 0.68) < 0.07):
                return BRONZE_GREEN
            return noisy(p, BRONZE, 4)
        disc(p, f['front'], front)
        disc(p, f['back'], back)
        bare_edges(p, f)

    # 2 the sceptre of ice
    if style == 'i_grip':
        metal(p, f, STEEL, (90, 96, 110))
    if style == 'i_mount':
        metal(p, f, STEEL, (90, 96, 110))
        edges(p, f, (240, 244, 255), bottom=False)
    if style in ('i_crystal', 'i_shard', 'i_shard_small'):         # ice: pale, glassy, facets catching light, cut to a point
        for s, r in f.items():
            fx, fy, fw, fh = r
            for j in range(fh):
                for i in range(fw):
                    t = j / max(1, fh - 1)
                    facet = (i * 2 // max(1, fw)) % 2
                    c = mixc((236, 250, 255), (120, 186, 230), 0.25 + 0.5 * t + 0.15 * facet)
                    if s in ('right', 'left'):
                        c = k(c, 0.85)
                    p.lit((fx + i, fy + j), c, glow=0.45 if (i + j) % 4 else 0.7)
            if s in ('front', 'back', 'right', 'left'):            # a bright edge down one side
                for j in range(fh):
                    p.lit((fx, fy + j), (250, 254, 255), glow=0.7)
        for s in ('front', 'back', 'right', 'left'):
            point(p, f[s], keep=0.5, down=False, sharp=0.5)

    # 3 the iron hook
    if style in ('h_shaft', 'h_arc'):                              # black iron, rusted here and there, its inner edge honed
        metal(p, f, IRON, IRON_DARK)
        for s in sides + ('top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for _ in range(max(1, fw * fh // 14)):
                p.px((fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)), (110, 60, 40))     # rust
        if style == 'h_arc':
            fx, fy, fw, fh = f['bottom']
            for i in range(fw):
                for j in range(fh):
                    p.px((fx + i, fy + j), STEEL)
    if style == 'h_point':                                         # the barb, honed bright, pointing up
        p.shaded(f, STEEL)
        for s in ('front', 'back', 'right', 'left'):
            point(p, f[s], keep=0.5, down=False, sharp=0.45)
    if style in ('h_link_a', 'h_link_b'):
        p.shaded(f, IRON)
        for s in sides:
            fx, fy, fw, fh = f[s]
            if fw > 2:
                for j in range(1, fh - 1):
                    for i in range(1, fw - 1):
                        p.px((fx + i, fy + j), IRON_DARK)
    if style == 'h_ring':
        p.shaded(f, IRON)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh // 2), IRON_DARK)

    # 4 the scales
    if style in ('s_post', 's_beam', 's_foot'):
        metal(p, f, GOLD, GOLD_DEEP)
    if style == 's_pointer':
        metal(p, f, GOLD, GOLD_DEEP)
        for s in ('front', 'back'):
            point(p, f[s], keep=0.5, down=False, sharp=0.6)
    if style == 's_cord':
        p.shaded(f, (150, 30, 26))
    if style == 's_pan':
        metal(p, f, GOLD, GOLD_DEEP)
        fx, fy, fw, fh = f['top']
        circle(p, fx + fw / 2, fy + fh / 2, fw / 2 - 1.5, fw / 2 - 0.6, GOLD_DEEP)

    # 6 the bell
    if style in ('b_handle', 'b_knob'):
        metal(p, f, BRONZE, BRONZE_DEEP)
    if style == 'b_bell':                                          # the bell, cut to its shape: a round crown, its sides swelling to the lip
        def half(t):                                               # its half-breadth, a share of the box's, t 0 at the top .. 1 at the foot
            return 0.5 + 0.5 * t ** 0.8 if t > 0.12 else 0.56 * math.sqrt(t / 0.12) + 0.02
        for s in sides:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                t = (j + 0.5) / fh
                hw = half(t)
                for i in range(fw):
                    u = abs((i + 0.5) / fw - 0.5) * 2
                    if u > hw:
                        p.clear((fx + i, fy + j))
                        continue
                    shade = abs((i + 0.5) / fw - 0.42) * 2 / max(0.3, hw)
                    c = mixc(mixc(BRONZE, (236, 190, 120), 0.35), BRONZE_DEEP, min(1, shade * 0.9))
                    if abs(t - 0.36) < 0.03 or abs(t - 0.74) < 0.03:
                        c = GOLD                                   # its bands
                    elif 0.42 < t < 0.68 and (i % 4 == 1) and (j % 3 == 0) and u < hw - 0.15:
                        c = GOLD                                   # its studs (枚)
                    elif 0.8 < t < 0.95 and abs(i + 0.5 - fw / 2) < 2.5:
                        c = GOLD_DEEP                              # the striking boss
                    elif t > 0.78 and (i // 2) % 3 == 0:
                        c = BRONZE_GREEN
                    p.px((fx + i, fy + j), noisy(p, c, 3))
        fx, fy, fw, fh = f['top']                                  # its crown, round
        for j in range(fh):
            for i in range(fw):
                d = math.hypot((i + 0.5) / fw - 0.5, (j + 0.5) / fh - 0.5) * 2
                if d > 0.58:
                    p.clear((fx + i, fy + j))
                else:
                    p.px((fx + i, fy + j), GOLD if d > 0.46 else BRONZE)
        fx, fy, fw, fh = f['bottom']
        for j in range(fh):
            for i in range(fw):
                p.px((fx + i, fy + j), (26, 16, 10))
    if style in ('b_crown', 'b_shoulder', 'b_body', 'b_lip'):      # bronze, rounded, banded in gold, studded, verdigris in its hollows
        metal(p, f, BRONZE, BRONZE_DEEP)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for j in range(fh):                                    # rounded: dark toward its edges, a highlight down it
                for i in range(fw):
                    t = abs((i + 0.5) / fw - 0.42) * 2
                    p.px((fx + i, fy + j), noisy(p, mixc(mixc(BRONZE, (236, 190, 120), 0.35), BRONZE_DEEP, min(1, t * 1.1)), 3))
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), GOLD)
            if style == 'b_shoulder':
                for i in range(2, fw - 2, 4):                      # the studs (枚), in rows
                    for j in range(2, fh - 2, 3):
                        p.px((fx + i, fy + j), GOLD)
                        p.px((fx + i + 1, fy + j), GOLD_DEEP)
            if style == 'b_body':
                for i in range(fw):
                    p.px((fx + i, fy + 1), GOLD)
                    if (i // 2) % 3 == 0:
                        p.px((fx + i, fy + fh - 3), BRONZE_GREEN)
                for j in range(2, fh - 1):                         # the striking boss
                    for i in range(fw // 2 - 2, fw // 2 + 2):
                        if abs(j - fh // 2) < 2:
                            p.px((fx + i, fy + j), GOLD_DEEP)
        if style == 'b_lip':
            fx, fy, fw, fh = f['bottom']                           # the mouth: dark within
            for j in range(fh):
                for i in range(fw):
                    if min(i, j, fw - 1 - i, fh - 1 - j) > 1:
                        p.px((fx + i, fy + j), (26, 16, 10))
    if style == 'b_clapper':
        metal(p, f, IRON, IRON_DARK)

    # 7 the cauldron
    if style in ('c_body', 'c_rim', 'c_ear', 'c_leg'):
        metal(p, f, BRONZE, BRONZE_DEEP)
        if style == 'c_body':                                      # a taotie mask on its belly, verdigris in its hollows
            for s in sides:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + 1), GOLD)
                    if (i + 1) % 3 == 0:
                        p.px((fx + i, fy + fh - 2), BRONZE_GREEN)
                cx = fx + fw // 2
                for (a, b) in ((-3, 3), (-2, 3), (2, 3), (3, 3), (-1, 5), (0, 5), (1, 5), (-3, 4), (3, 4)):
                    if 0 <= a + fw // 2 < fw:
                        p.px((cx + a, fy + b), GOLD if b < 5 else BRONZE_GREEN)
        if style == 'c_rim':                                       # the oil within, seething, glowing
            fx, fy, fw, fh = f['top']
            for j in range(fh):
                for i in range(fw):
                    if min(i, j, fw - 1 - i, fh - 1 - j) > 0:
                        c = mixc(FIRE_HOT, FIRE, p.rnd.random() * 0.8)
                        p.lit((fx + i, fy + j), c, glow=0.9)
    if style in ('c_flame', 'c_flame_x'):                          # a crossed tongue of fire
        flat = ('right', 'left') if style == 'c_flame_x' else ('front', 'back')
        for s in flat:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                t = 1 - j / max(1, fh - 1)
                half = (fw / 2) * (1 - t ** 1.4) + 0.3
                sway = 1.0 * math.sin(t * 3.2) * t
                for i in range(fw):
                    dx = i + 0.5 - fw / 2 - sway
                    if abs(dx) > half:
                        p.clear((fx + i, fy + j))
                        continue
                    core = abs(dx) < half * 0.45 and t < 0.75
                    c = mixc(FIRE_HOT, FIRE, t) if core else mixc(FIRE, FIRE_DEEP, min(1, t * 1.4))
                    p.lit((fx + i, fy + j), c, glow=0.9)
        bare_edges(p, f, [s for s in ('front', 'back', 'right', 'left', 'top', 'bottom') if s not in flat])

    # 8 the cart wheel
    if style == 'w_cart':                                          # an iron tyre studded with nails, wooden felloes, twelve spokes
        def wheel(r, a):
            if r > 0.88:
                return IRON if r < 0.97 and int((a + math.pi) * 12 / math.pi) % 2 else IRON_DARK
            if r > 0.72:
                return noisy(p, WOOD, 4) if int((a + math.pi) * 6 / math.pi) % 2 else noisy(p, k(WOOD, 0.85), 4)
            if r < 0.2:
                return WOOD_DEEP
            spoke = abs(math.sin(a * 6)) < 0.16 + 0.05 / max(0.3, r)
            return noisy(p, WOOD if r > 0.4 else k(WOOD, 0.9), 3) if spoke else None
        disc(p, f['front'], wheel)
        disc(p, f['back'], wheel)
        bare_edges(p, f)
    if style == 'w_hub':
        p.shaded(f, WOOD_DEEP)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), IRON)
                p.px((fx + i, fy + fh - 1), IRON)
        fx, fy, fw, fh = f['front']
        circle(p, fx + fw / 2, fy + fh / 2, 0, 1.0, IRON_DARK)

    # 9 the plaque of 平
    if style in ('p_pole',):                                       # red lacquer, banded gold
        p.shaded(f, (150, 30, 26))
        for s in sides:
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 5):
                for i in range(fw):
                    p.px((fx + i, fy + j), GOLD)
    if style in ('p_crest', 'p_foot'):
        metal(p, f, GOLD, GOLD_DEEP)
    if style == 'p_plaque':                                        # vermilion in a gold frame, 平 in gold
        metal(p, f, GOLD, GOLD_DEEP)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]                                  # 19x23
            for j in range(fh):
                for i in range(fw):
                    edge = min(i, j, fw - 1 - i, fh - 1 - j)
                    p.px((fx + i, fy + j), GOLD if edge == 0 else GOLD_DEEP if edge == 1 else GOLD if edge == 2 else noisy(p, (176, 34, 26), 4))
            ping(p, (fx + 3, fy + 3, fw - 6, fh - 6), GOLD, GOLD_DEEP)

    # 10 the Wheel of Reincarnation
    if style == 'r_wheel':                                         # gold: flames round its rim, the six realms in their colours between six spokes
        realms = ((236, 236, 240), (90, 160, 220), (200, 60, 50), (110, 180, 90), (150, 100, 190), (60, 50, 60))

        def wheel(r, a):
            lick = 0.06 * math.sin(a * 16)
            if r > 0.9 + lick:
                return None
            if r > 0.8:
                return ('glow', mixc(FIRE_HOT, FIRE, (r - 0.8) / 0.2), 0.5)
            if r > 0.7:
                return GOLD if r > 0.74 else GOLD_DEEP
            if r < 0.2:
                return ('glow', (255, 236, 160), 0.85) if r < 0.12 else GOLD
            sector = int(((a + math.pi / 2) % (2 * math.pi)) / (math.pi / 3))
            if abs(math.sin((a + math.pi / 2) * 3)) < 0.14 + 0.05 / max(0.3, r):
                return GOLD                                        # the spokes
            if r < 0.27:
                return GOLD_DEEP
            return noisy(p, realms[sector % 6], 4)
        disc(p, f['front'], wheel)
        disc(p, f['back'], wheel)
        bare_edges(p, f)
    if style == 'r_hub':
        metal(p, f, GOLD, GOLD_DEEP)
        fx, fy, fw, fh = f['front']
        circle(p, fx + fw / 2, fy + fh / 2, 0, 1.4, (255, 240, 180))
        for x in range(fx, fx + fw):
            for y in range(fy, fy + fh):
                if math.hypot(x + 0.5 - fx - fw / 2, y + 0.5 - fy - fh / 2) < 1.4:
                    p.lit((x, y), (255, 240, 180), glow=0.9)


if __name__ == '__main__':
    slots = None
    for court in KINGS:
        p = KingPainter(court * 13)
        slots = paint(PARTS, p, king_skin(court))
        p.save('king_' + KINGS[court][0])
    emit(PARTS, slots, JAVA, 'kings')
    print('the nine kings:', len(PARTS), 'parts,', len(slots), 'boxes')
    from lion_camel import extent
    print('   drawn at', SIZE, {kk: round(v, 2) for kk, v in extent(PARTS, SIZE).items()})
