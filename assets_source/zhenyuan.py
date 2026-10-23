"""The Great Immortal Zhenyuan (镇元子), Ancestor of the Earth Immortals. As fire_court.py and
lion_camel.py: his model and skin come from one list of parts (beastgen): the model is written into
WuzhuangRenderers' zhenyuanLayer() between its GENERATED markers, the skin painted to the same box-UV
layout at twice the model's resolution, with a second, glowing skin (his eyes, the halo, the stars in
his sleeve) drawn full-bright, dimly.

A tall, unbent immortal on a small cloud: layered Daoist robes of deep celestial indigo over an
earth-gold under-robe (yellow is earth's colour), a violet cloud-collar (云肩) with gold ruyi tips over
the shoulders, gold cloud scrolls and a sea-and-cliff hem, the bagua about a taiji on his back; wide
trailing sleeves, their mouths dark with stars (the Universe in his Sleeve); a jade belt and an
earth-gold apron; a gold lotus crown (莲花冠) on his white topknot, a jade pin through it; long white
hair down his back, long white brows, a long white beard and moustache; his horsetail whisk (拂尘) in
his right hand; a celestial ribbon (披帛) drifting about his shoulders; behind his head a ring of gold
with the eight trigrams.

Run from the repository root: python assets_source/zhenyuan.py"""
import math

import beastgen as bg
from beastgen import Painter, paint, emit
from fire_court import part, mirrored

bg.UNITS = 128
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/WuzhuangRenderers.java'
INDIGO = (40, 36, 92)
VIOLET = (84, 58, 128)
NIGHT = (14, 12, 34)
GOLD = (228, 184, 76)
GOLD_DEEP = (152, 110, 42)
EARTH = (190, 142, 58)
WHITE = (236, 236, 242)
HAIR_SHADE = (176, 180, 204)
SKIN = (228, 206, 184)
JADE = (110, 196, 160)
LACQUER = (120, 28, 34)
CLOUD = (238, 240, 250)
EYE = (110, 84, 170)          # a faint violet gleam: dark, not bright

# How much larger than his pixels he is drawn: ZhenyuanRenderer.SIZE
SIZE = 1.0

ZHENYUAN = mirrored([
    # the small cloud he stands on, and the figure over it (it rises and falls a little)
    part('cloud', 'root', (0, 22.4, 0), (0, 0, 0), [
        (-8, -0.6, -7, 16, 2.4, 14, 'cloud'),
        (-11.5, 0, -4, 5, 1.8, 8, 'puff'),
        (7, 0.2, -4.5, 5, 1.6, 9, 'puff'),
        (-5, 0.2, -10.5, 10, 1.6, 4, 'puff'),
        (-4, 0, 6.5, 9, 1.8, 4.5, 'puff'),
        (-13.5, 0.6, 0, 3, 1, 4, 'puff_top'),
        (11.5, 0.8, -1, 3, 1, 3, 'puff_top'),
        (-7, -1.6, -5.5, 6, 1.2, 6, 'puff_top'),
        (2, -1.4, 0.5, 5, 1, 6, 'puff_top'),
    ]),
    part('figure', 'root', (0, 0, 0), (0, 0, 0), []),
    part('body', 'figure', (0, -10, 0), (0, 0, 0), [
        (-4.5, 0, -2.6, 9, 14, 5.2, 'robe'),
        (-5.6, -0.4, -3.2, 11.2, 3.8, 6.4, 'mantle'),
        (-4.9, 13, -3, 9.8, 2, 6, 'belt'),
        (-5.6, 14.6, -3.4, 11.2, 8, 6.8, 'skirt_upper'),
        (-6.8, 21, -4.4, 13.6, 10.2, 8.8, 'skirt_lower'),
        (-2.2, 21.6, -5, 4.4, 9.4, 0.5, 'apron'),
        (2.2, 15, -3.9, 1.4, 3, 0.4, 'pendant'),
    ]),
    part('train', 'body', (0, 30.6, 4), (-0.32, 0, 0), [(-6.4, -0.6, -0.4, 12.8, 0.8, 7, 'train')]),
    part('halo', 'body', (0, -7, 6.2), (0, 0, 0), [(-9, -9, 0, 18, 18, 1, 'halo')]),
    part('hair', 'body', (0, -6.5, 4.5), (0.06, 0, 0), [(-3.8, 0, -0.2, 7.6, 18, 1, 'mane')]),
    part('hair_end', 'hair', (0, 18, 0), (0.05, 0, 0), [(-3, 0, -0.2, 6, 6, 0.8, 'mane_end')]),
    part('right_ribbon', 'body', (-5.4, 1.5, 2.8), (0.55, 0, 0.45), [(-1.2, 0, -0.2, 2.4, 10, 0.4, 'ribbon')]),
    part('right_ribbon_end', 'right_ribbon', (0, 10, 0), (-0.5, 0, -0.3), [(-1.2, 0, -0.2, 2.4, 11, 0.4, 'ribbon')]),
    part('head', 'figure', (0, -10, 0), (0, 0, 0), [
        (-4, -9, -4, 8, 9, 8, 'face'),
        (-4.3, -9.3, -0.5, 8.6, 9.6, 4.8, 'hair_back'),
        (-1.6, -12, -1.6, 3.2, 3.2, 3.2, 'knot'),
        (-2.6, -11.2, -2.6, 5.2, 1.6, 5.2, 'crown_band'),
        (-5, -11.6, -0.4, 10, 0.8, 0.8, 'pin'),
        (-0.7, -13.4, -0.7, 1.4, 1.4, 1.4, 'pearl'),
    ]),
    part('crown_front', 'head', (0, -11, -2.6), (-0.3, 0, 0), [(-1.6, -4.4, -0.3, 3.2, 4.4, 0.6, 'petal')]),
    part('crown_back', 'head', (0, -11, 2.6), (0.3, 0, 0), [(-1.6, -4.4, -0.3, 3.2, 4.4, 0.6, 'petal')]),
    part('right_petal', 'head', (-2.6, -11, 0), (0, 0, -0.32), [(-0.3, -3.8, -1.4, 0.6, 3.8, 2.8, 'petal_side')]),
    part('right_brow', 'head', (-2.2, -5.6, -4.2), (0, 0, -0.55), [(-4, -0.5, -0.3, 4, 1, 0.6, 'brow')]),
    part('right_whisker', 'head', (-1.8, -2.6, -4.3), (-0.1, 0, 0.18), [(-0.5, 0, -0.3, 1, 7, 0.6, 'whisker')]),
    part('beard', 'head', (0, -1.6, -4.3), (0.1, 0, 0), [(-2.6, 0, -0.7, 5.2, 8, 1.4, 'beard')]),
    part('beard_tip', 'beard', (0, 8, 0), (-0.04, 0, 0), [(-1.8, 0, -0.6, 3.6, 8, 1.2, 'beard_tip')]),
    part('right_arm', 'figure', (-6, -8, 0), (0, 0, 0), [
        (-3.2, -1.6, -2.4, 4, 12, 4.8, 'arm'),
        (-2.6, 10.4, -1.4, 2.6, 2.4, 2.8, 'hand'),
    ]),
    # the great sleeve hangs from the forearm; a gold lip about its mouth, the mouth itself the dark
    part('right_sleeve', 'right_arm', (-1.2, 3, 1.2), (0, 0, 0), [
        (-3.5, 0, -3.5, 7, 16, 8, 'sleeve'),
        (-4, 15, -4, 8, 1.4, 0.6, 'lip'),
        (-4, 15, 4.4, 8, 1.4, 0.6, 'lip'),
        (-4, 15, -3.4, 0.6, 1.4, 7.8, 'lip_side'),
        (3.4, 15, -3.4, 0.6, 1.4, 7.8, 'lip_side'),
    ]),
    part('whisk', 'right_arm', (-1.3, 11.8, -0.6), (-0.5, 0, 0), [
        (-0.5, -7, -0.5, 1, 9, 1, 'handle'),
        (-0.8, -8, -0.8, 1.6, 1.2, 1.6, 'gold'),
        (-0.7, 2, -0.7, 1.4, 1, 1.4, 'gold'),
    ]),
    part('whisk_tail', 'whisk', (0, -7.6, 0), (0, 0, 0), [
        (-1.1, 0, -1.1, 2.2, 8, 2.2, 'horsetail'),
        (-0.7, 8, -0.7, 1.4, 5, 1.4, 'horsetail_tip'),
    ]),
])


# ── Painting helpers ─────────────────────────────────────────────────────────

SCROLL = ['.##..##.',
          '#..##..#',
          '#.#..#.#',
          '.#....#.']


def gleam(p, xy, colour, glow=False):
    """A pixel of the skin; with `glow` (a fraction), the same pixel dimmed by it on the glowing skin."""
    p.px(xy, colour)
    if glow:
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            p.glow.putpixel((x, y), tuple(max(0, min(255, int(v * glow))) for v in colour[:3]) + (255,))


def scroll(p, x, y, colour, glow=False):
    """A little ruyi cloud scroll, 8x4 texture pixels."""
    for j, row in enumerate(SCROLL):
        for i, c in enumerate(row):
            if c == '#':
                p.px((x + i, y + j), colour, glow)


def scrolls(p, rect, colour, every=12, rows=8, skip_bottom=0):
    fx, fy, fw, fh = rect
    for k, j in enumerate(range(2, fh - 4 - skip_bottom, rows)):
        for i in range((k * 5) % every - 2, fw - 7, every):
            if i >= 0:
                scroll(p, fx + i, fy + j, colour)


def sea_hem(p, rect, rows=8):
    """The sea-and-cliff hem (海水江崖): slanting waves of violet, indigo and gold between gold lines."""
    fx, fy, fw, fh = rect
    top = fy + fh - rows
    band = (VIOLET, INDIGO, (120, 96, 168), GOLD_DEEP)
    for j in range(rows):
        for i in range(fw):
            c = band[((i + j) // 2) % len(band)]
            p.px((fx + i, top + j), [v + p.rnd.randint(-5, 5) for v in c])
    for i in range(fw):
        p.px((fx + i, top - 1), GOLD)
        p.px((fx + i, top - 2), GOLD_DEEP)
        p.px((fx + i, fy + fh - 1), GOLD)


def robe(p, f, base=INDIGO, light=1.0):
    """Cloth: lit above, a soft fall down its sides, faint vertical folds."""
    p.shaded(f, base, light=light)
    for k in ('front', 'back', 'right', 'left'):
        fx, fy, fw, fh = f[k]
        for i in range(1, fw, 3 + (fw > 12)):
            for j in range(fh):
                if p.rnd.random() < 0.7:
                    p.px((fx + i, fy + j), [c * 0.82 * light for c in base])


def circle(p, cx, cy, r0, r1, colour, glow=False, skip=None):
    for y in range(int(cy - r1 - 1), int(cy + r1 + 2)):
        for x in range(int(cx - r1 - 1), int(cx + r1 + 2)):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r0 <= d <= r1 and not (skip and skip(x + 0.5 - cx, y + 0.5 - cy)):
                gleam(p, (x, y), colour, glow)


def taiji(p, cx, cy, r):
    """The yin-yang: dark and light halves, each with the other's eye, a gold rim."""
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d > r:
                continue
            light = dx < 0
            if math.hypot(dx, dy + r / 2) < r / 2:
                light = True
            if math.hypot(dx, dy - r / 2) < r / 2:
                light = False
            if math.hypot(dx, dy + r / 2) < r / 7 + 0.4:
                light = False
            if math.hypot(dx, dy - r / 2) < r / 7 + 0.4:
                light = True
            p.px((x, y), (236, 232, 220) if light else (18, 16, 26))
    circle(p, cx, cy, r, r + 1, GOLD)


TRIGRAMS = ('111', '011', '101', '001', '110', '010', '100', '000')


def trigram(p, cx, cy, angle, lines, colour, glow=False, length=5):
    """Three bars (whole or broken), centred at (cx, cy), lying across the radius at `angle`."""
    ux, uy = math.cos(angle), math.sin(angle)          # outward
    vx, vy = -uy, ux                                  # along the bar
    for n, whole in enumerate(lines):
        off = (n - 1) * 2
        for t in range(length):
            s = t - (length - 1) / 2
            if whole == '0' and abs(s) < 0.8:
                continue
            gleam(p, (round(cx + ux * off + vx * s), round(cy + uy * off + vy * s)), colour, glow)


def bagua(p, cx, cy, r):
    """The eight trigrams about a taiji."""
    circle(p, cx, cy, r - 0.5, r + 0.6, GOLD)
    for k, lines in enumerate(TRIGRAMS):
        a = -math.pi / 2 + k * math.pi / 4
        trigram(p, cx + math.cos(a) * (r - 3), cy + math.sin(a) * (r - 3), a, lines, GOLD, length=3)
    taiji(p, cx, cy, r - 6)


def strands(p, rect, base, shade, lean=0):
    """Hair or horsetail: long strands down the face, lighter and darker."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        k = 0.92 + 0.12 * p.rnd.random()
        for j in range(fh):
            c = base if (i + (j * lean) // 6) % 3 else shade
            p.px((fx + i, fy + j), [v * k * (1 - 0.12 * j / max(1, fh)) + p.rnd.randint(-4, 4) for v in c])


def gold_edges(p, f, sides=('front', 'back', 'right', 'left'), top=True, bottom=True, colour=GOLD):
    for k in sides:
        fx, fy, fw, fh = f[k]
        for i in range(fw):
            if top:
                p.px((fx + i, fy), colour)
            if bottom:
                p.px((fx + i, fy + fh - 1), colour)


# ── The skin ────────────────────────────────────────────────────────────────

def paint_zhenyuan(p, style, f, dims):
    if style in ('cloud', 'puff', 'puff_top'):
        for k, r in f.items():
            if k == 'bottom':
                p.face(r, (176, 178, 206), 1.0, noise=6)
            elif k == 'top':
                p.face(r, CLOUD, 1.0, noise=5)
            else:
                p.gradient(r, CLOUD, (190, 190, 222), noise=5)
        for k in ('front', 'back', 'right', 'left', 'top'):        # soft curls
            fx, fy, fw, fh = f[k]
            for _ in range(fw * fh // 14):
                x, y = fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)
                p.px((x, y), (214, 216, 238))
                p.px((x + 1, y), (214, 216, 238))

    if style == 'robe':
        robe(p, f)
        fx, fy, fw, fh = f['front']                                # 18x24: the crossed collar, left over right
        for j in range(fh - 4):
            cross = fw - 4 - j * 0.62                              # his left lapel, from his left shoulder across
            for w, c in ((0, WHITE), (1, WHITE), (2, GOLD), (3, EARTH), (4, EARTH), (5, GOLD)):
                a = int(cross) - w
                if 0 <= a < fw:
                    p.px((fx + a, fy + j), c)
                b = int(1 + j * 0.62) + w                          # the lapel under it, to where it is crossed
                if b < cross - 6:
                    p.px((fx + b, fy + j), c)
        scrolls(p, (fx, fy + 12, fw, fh - 12), GOLD_DEEP, every=10)
        fx, fy, fw, fh = f['back']                                 # the bagua on his back
        bagua(p, fx + fw / 2, fy + 11, 9)
        for k in ('right', 'left'):
            scrolls(p, f[k], GOLD_DEEP, every=9)

    if style == 'mantle':                                         # the cloud collar: violet, gold ruyi tips
        p.shaded(f, VIOLET)
        fx, fy, fw, fh = f['top']
        cx, cy = fx + fw / 2, fy + fh / 2
        for y in range(fy, fy + fh):
            for x in range(fx, fx + fw):
                a = math.atan2(y - cy, (x - cx) * fh / fw)
                if abs(math.sin(a * 4)) < 0.2:
                    p.px((x, y), GOLD_DEEP)
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                p.px((fx + i, fy), GOLD)
                # scalloped ruyi tips down the lower edge
                t = (i % 6) / 5
                tip = int(2.5 - 2.5 * abs(t - 0.5) * 2)
                for j in range(fh - 3 - tip, fh):
                    p.px((fx + i, fy + j), GOLD if j >= fh - 2 - tip else GOLD_DEEP)
            for i in range(2, fw - 3, 6):
                p.px((fx + i + 1, fy + 2), (226, 220, 250))

    if style == 'belt':                                           # jade, gold plaques
        p.shaded(f, (70, 150, 120))
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(1, fw - 1, 4):
                for j in range(1, fh - 1):
                    p.px((fx + i, fy + j), GOLD)
                    p.px((fx + i + 1, fy + j), GOLD_DEEP)
        gold_edges(p, f, colour=GOLD_DEEP)

    if style == 'skirt_upper':
        robe(p, f)
        fx, fy, fw, fh = f['front']                               # the earth-gold under-robe in the opening
        for j in range(fh):
            for i in range(fw // 2 - 4, fw // 2 + 4):
                edge = abs(i - fw // 2 + 0.5) > 3
                p.px((fx + i, fy + j), GOLD if edge else [c + p.rnd.randint(-6, 6) for c in EARTH])
        for k in ('right', 'left', 'back'):
            scrolls(p, f[k], GOLD_DEEP, every=10)

    if style == 'skirt_lower':
        robe(p, f)
        for k in ('front', 'back', 'right', 'left'):
            scrolls(p, f[k], GOLD_DEEP, every=11, skip_bottom=8)
            sea_hem(p, f[k], rows=7)
        fx, fy, fw, fh = f['front']
        for j in range(fh - 9):                                   # the opening's gold borders run on down
            for i in (fw // 2 - 5, fw // 2 + 4):
                p.px((fx + i, fy + j), GOLD)

    if style == 'apron':                                          # the earth-gold apron, a taiji at its head
        for k, r in f.items():
            p.face(r, EARTH, 1.0, noise=6, fall=0.2)
        fx, fy, fw, fh = f['front']
        for j in range(fh):
            p.px((fx, fy + j), GOLD)
            p.px((fx + fw - 1, fy + j), GOLD)
        taiji(p, fx + fw / 2, fy + 4.5, 2.6)
        for j in range(9, fh - 2, 3):
            for i in range(2, fw - 2):
                if (i + j) % 3 == 0:
                    p.px((fx + i, fy + j), GOLD_DEEP)
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), GOLD)

    if style == 'pendant':                                        # a jade pendant on a red cord
        for k, r in f.items():
            p.face(r, JADE, 1.0, noise=8)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy), LACQUER)

    if style == 'train':
        robe(p, f, light=0.95)
        fx, fy, fw, fh = f['top']
        scrolls(p, (fx, fy, fw, fh - 3), GOLD_DEEP, every=10, rows=6)
        for i in range(fw):
            for j in (fh - 1, fh - 2):
                p.px((fx + i, fy + j), GOLD if j == fh - 2 else GOLD_DEEP)

    if style == 'halo':                                           # a ring of gold, the eight trigrams on it
        for k in ('front', 'back'):
            fx, fy, fw, fh = f[k]
            cx, cy = fx + fw / 2, fy + fh / 2
            circle(p, cx, cy, fw / 2 - 2.2, fw / 2 - 0.6, GOLD, glow=0.4)
            circle(p, cx, cy, fw / 2 - 7.5, fw / 2 - 6.6, GOLD_DEEP, glow=0.4,
                   skip=lambda dx, dy: abs(math.sin(math.atan2(dy, dx) * 12)) < 0.35)
            for n, lines in enumerate(TRIGRAMS):
                a = -math.pi / 2 + n * math.pi / 4
                trigram(p, cx + math.cos(a) * (fw / 2 - 4.4), cy + math.sin(a) * (fw / 2 - 4.4), a, lines,
                        GOLD, glow=0.4, length=5)

    if style == 'mane':                                           # long white hair down his back
        for k, r in f.items():
            strands(p, r, WHITE, HAIR_SHADE)
    if style == 'mane_end':
        for k, r in f.items():
            strands(p, r, WHITE, HAIR_SHADE)
        fx, fy, fw, fh = f['back']                                # to a point
        for j in range(fh):
            for i in range(fw):
                if abs(i - fw / 2 + 0.5) > (fh - j) * 0.9 + 1:
                    p.img.putpixel((fx + i, fy + j), (0, 0, 0, 0))

    if style == 'ribbon':                                         # the celestial ribbon: pale silk, gold-edged
        for k, r in f.items():
            p.gradient(r, (214, 200, 244), (170, 150, 220), noise=4)
        for k in ('front', 'back'):
            fx, fy, fw, fh = f[k]
            for j in range(fh):
                p.px((fx, fy + j), GOLD)
                p.px((fx + fw - 1, fy + j), GOLD)

    if style == 'face':
        p.shaded(f, WHITE)                                        # white hair over the head
        for k in ('right', 'left', 'back', 'top'):
            strands(p, f[k], WHITE, HAIR_SHADE)
        for k in ('right', 'left'):                               # the face's edge: skin before the ear
            fx, fy, fw, fh = f[k]
            for j in range(5, fh):
                for i in range(3):
                    x = fx + (fw - 1 - i if k == 'right' else i)
                    p.px((x, fy + j), [c * 0.92 for c in SKIN])
        fx, fy, fw, fh = f['front']                               # 16x18
        for j in range(fh):
            for i in range(fw):
                p.px((fx + i, fy + j), [c * (1 - 0.1 * j / fh) + p.rnd.randint(-3, 3) for c in SKIN])
        for j in range(4):                                        # hair swept back from a high brow
            for i in range(fw):
                if j < 2 or i < 3 - j or i > fw - 4 + j:
                    p.px((fx + i, fy + j), WHITE if i % 3 else HAIR_SHADE)
        for i in range(fw):
            if i < 2 or i > fw - 3:
                for j in range(4, 12):
                    p.px((fx + i, fy + j), WHITE if i % 2 else (214, 216, 230))
        p.px((fx + 7, fy + 4), LACQUER)                           # the mark between his brows
        p.px((fx + 8, fy + 4), LACQUER)
        p.px((fx + 7, fy + 5), GOLD)
        p.px((fx + 8, fy + 5), GOLD)
        for i in range(6):                                        # long white brows, knit low over the nose: stern
            for s in (1, -1):
                x = fx + (7 - i if s > 0 else 8 + i)
                p.px((x, fy + 7 - i // 2), WHITE)
                p.px((x, fy + 6 - i // 2), HAIR_SHADE if i < 2 else WHITE)
        shadow = [c * 0.66 for c in SKIN]
        for ex in (3, 10):                                        # deep-set, half-closed eyes, a dim gleam
            for i in range(-1, 4):
                p.px((fx + ex + i, fy + 8), shadow)
            for i in range(3):
                p.px((fx + ex + i, fy + 9), (34, 26, 40))
            gleam(p, (fx + ex + 1, fy + 9), EYE, glow=0.8)
            gleam(p, (fx + ex + (0 if ex == 3 else 2), fy + 9), [c * 0.7 for c in EYE], glow=0.8)
            for i in range(3):
                p.px((fx + ex + i, fy + 10), [c * 0.8 for c in SKIN])
        for j in range(9, 13):                                    # a long straight nose
            p.px((fx + 7, fy + j), [c * 0.84 for c in SKIN])
        p.px((fx + 6, fy + 12), [c * 0.74 for c in SKIN])
        p.px((fx + 9, fy + 12), [c * 0.74 for c in SKIN])
        for i in range(3, 13):                                    # the moustache over his mouth
            p.px((fx + i, fy + 13), WHITE if i % 4 else HAIR_SHADE)
        for i in range(2, 14):
            if i in (2, 3, 12, 13) or 5 <= i <= 10:
                p.px((fx + i, fy + 14), WHITE)
        for i in range(fw):                                       # and the beard from the jaw
            for j in range(15, fh):
                if abs(i - fw / 2 + 0.5) < 3 + (j - 14) * 1.5:
                    p.px((fx + i, fy + j), WHITE if (i + j) % 4 else HAIR_SHADE)
        for i in range(5, 11):
            p.px((fx + i, fy + 14 if i in (5, 10) else fy + 15), (96, 60, 64))
        fx, fy, fw, fh = f['bottom']
        p.face(f['bottom'], WHITE, 0.85, noise=4)

    if style == 'hair_back':
        for k, r in f.items():
            strands(p, r, WHITE, HAIR_SHADE)

    if style == 'knot':
        for k, r in f.items():
            strands(p, r, WHITE, HAIR_SHADE)

    if style in ('lip', 'lip_side'):
        p.shaded(f, GOLD)

    if style in ('crown_band', 'gold'):
        p.shaded(f, GOLD)
        if style == 'crown_band':
            for k in ('front', 'back', 'right', 'left'):
                fx, fy, fw, fh = f[k]
                for i in range(1, fw - 1, 3):
                    p.px((fx + i, fy + 1), JADE)
                    p.px((fx + i, fy + 2), (70, 150, 120))

    if style in ('petal', 'petal_side'):                           # gold lotus petals, a jade vein
        p.shaded(f, GOLD)
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            if fw < 3:
                continue
            for j in range(fh):
                half = fw / 2
                w = half * min(1, (j + 1) / (fh * 0.5))   # to a point at the top
                for i in range(fw):
                    if abs(i - half + 0.5) > w:
                        p.img.putpixel((fx + i, fy + j), (0, 0, 0, 0))
                    elif abs(i - half + 0.5) > w - 1.2:
                        p.px((fx + i, fy + j), GOLD_DEEP)
            for j in range(fh // 3, fh - 1):
                p.px((fx + fw // 2, fy + j), JADE)
                p.px((fx + fw // 2 - 1, fy + j), (70, 150, 120))

    if style == 'pearl':
        p.shaded(f, (186, 236, 214))
        fx, fy, fw, fh = f['front']
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            gleam(p, (fx + 1, fy + 1), (186, 236, 214), glow=0.35)

    if style == 'pin':
        p.shaded(f, JADE)
        for k in ('front', 'back', 'top', 'bottom'):
            fx, fy, fw, fh = f[k]
            for i in list(range(3)) + list(range(fw - 3, fw)):
                for j in range(fh):
                    p.px((fx + i, fy + j), GOLD)

    if style == 'brow':
        for k, r in f.items():
            strands(p, r, WHITE, HAIR_SHADE)

    if style in ('whisker', 'beard', 'beard_tip'):
        for k, r in f.items():
            strands(p, r, WHITE, HAIR_SHADE)
        if style == 'beard_tip':
            for k in ('front', 'back'):
                fx, fy, fw, fh = f[k]
                for j in range(fh):
                    for i in range(fw):
                        if abs(i - fw / 2 + 0.5) > (fh - j) * 0.7 + 0.6:
                            p.img.putpixel((fx + i, fy + j), (0, 0, 0, 0))

    if style == 'arm':                                             # the sleeve over the arm, gold at the cuff
        robe(p, f)
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for j in (fh - 3, fh - 2):
                    p.px((fx + i, fy + j), GOLD)
                p.px((fx + i, fy + fh - 4), GOLD_DEEP)
            scrolls(p, (fx, fy, fw, fh - 4), GOLD_DEEP, every=8)
        p.face(f['top'], VIOLET, 1.0)

    if style == 'hand':
        p.shaded(f, SKIN)
        for k in ('front', 'back'):
            fx, fy, fw, fh = f[k]
            for i in range(1, fw, 2):
                p.px((fx + i, fy + fh - 1), [c * 0.8 for c in SKIN])

    if style == 'sleeve':                                          # the great hanging sleeve; its mouth the universe
        robe(p, f)
        for k in ('front', 'back', 'right', 'left'):
            scrolls(p, f[k], GOLD_DEEP, every=9, skip_bottom=7)
            sea_hem(p, f[k], rows=5)
        p.face(f['top'], VIOLET, 0.9)
        fx, fy, fw, fh = f['bottom']                               # 14x16: night, a swirl of stars
        cx, cy = fx + fw / 2, fy + fh / 2
        for y in range(fy, fy + fh):
            for x in range(fx, fx + fw):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                a = math.atan2(y + 0.5 - cy, x + 0.5 - cx)
                arm = abs(math.sin(a * 2 + d * 0.55))
                c = [n + (v - n) * min(1, 1.6 * max(0, 0.7 - arm)) * max(0, 1 - d / 9) for n, v in zip(NIGHT, (126, 92, 196))]
                p.px((x, y), c)
        for _ in range(16):
            x, y = fx + p.rnd.randrange(1, fw - 1), fy + p.rnd.randrange(1, fh - 1)
            gleam(p, (x, y), (200, 196, 255), glow=0.5)
        gleam(p, (cx, cy), (150, 120, 220), glow=0.6)
        for i in range(fw):
            p.px((fx + i, fy), GOLD)
            p.px((fx + i, fy + fh - 1), GOLD)
        for j in range(fh):
            p.px((fx, fy + j), GOLD)
            p.px((fx + fw - 1, fy + j), GOLD)

    if style == 'handle':                                          # red lacquer, banded gold
        p.shaded(f, LACQUER)
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(0, fh, 5):
                for i in range(fw):
                    p.px((fx + i, fy + j), GOLD)

    if style in ('horsetail', 'horsetail_tip'):
        for k, r in f.items():
            strands(p, r, WHITE, HAIR_SHADE)


if __name__ == '__main__':
    p = Painter(88)
    slots = paint(ZHENYUAN, p, paint_zhenyuan)
    p.save('zhenyuan')
    emit(ZHENYUAN, slots, JAVA, 'zhenyuan')
    print('zhenyuan', len(ZHENYUAN), 'parts,', len(slots), 'boxes')
    from lion_camel import extent
    print('   drawn at', SIZE, {k: round(v, 2) for k, v in extent(ZHENYUAN, SIZE).items()})
