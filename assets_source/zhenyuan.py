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

bg.UNITS = 192
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
PALE_GOLD = (240, 218, 150)   # the halo: a paler, softer gold

# How much larger than his pixels he is drawn: ZhenyuanRenderer.SIZE
SIZE = 1.2

# The cloud is not boxes: boxes of cloud, however softly painted, read up close as little cubes, their
# sides shaded grey. ZhenyuanModel.renderCloud draws it instead as soft round puffs that always face the
# eye (billboards), heaped and overlapping, lit evenly (no side darker than another), drifting a little,
# with flat wisps of mist about its foot. Their sprites: cloud_sprites(), below.
SPRITE = 128                   # each sprite's size, in texture pixels; the sheet is 2x2 of them


def cloud_puff(rnd, blobs, size=SPRITE):
    """A puff of cumulus as a sprite: the round `blobs` (x, y, r in 0..1 of the sprite) melted into one
    billowing surface (each a ball, blended smoothly where they meet so no seam shows), lit from above
    and a little to the left, its underside and the hollows between the billows in soft lilac shadow;
    its edge thinning away to nothing (no square corners, no hard rim)."""
    from PIL import Image
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    height = [[0.0] * size for _ in range(size)]
    cover = [[0.0] * size for _ in range(size)]
    for j in range(size):
        for i in range(size):
            u, v = (i + 0.5) / size, (j + 0.5) / size
            acc, cov = 0.0, 0.0
            for bx, by, br in blobs:
                d2 = ((u - bx) ** 2 + (v - by) ** 2) / (br * br)
                z = math.sqrt(max(0.0, 1 - d2)) * br
                acc += math.exp(z * 40)                            # a soft maximum: the billows melt together
                cov = max(cov, 1 - math.sqrt(d2))
            height[j][i] = math.log(acc) / 40 - math.log(len(blobs)) / 40
            cover[j][i] = cov
    lx, ly, lz = -0.35, -0.7, 0.62                               # the light: above, a little left, toward us
    ln = math.sqrt(lx * lx + ly * ly + lz * lz)
    lx, ly, lz = lx / ln, ly / ln, lz / ln
    dark, mid, lit = (186, 192, 224), (228, 232, 248), (255, 255, 255)
    for j in range(size):
        for i in range(size):
            cov = cover[j][i]
            if cov <= 0:
                continue
            h = lambda x, y: height[max(0, min(size - 1, y))][max(0, min(size - 1, x))]
            gx = (h(i + 1, j) - h(i - 1, j)) * size / 2
            gy = (h(i, j + 1) - h(i, j - 1)) * size / 2
            nx, ny, nz = -gx, -gy, 1.0
            nn = math.sqrt(nx * nx + ny * ny + nz * nz)
            lam = max(0.0, (nx * lx + ny * ly + nz * lz) / nn)
            v = (j + 0.5) / size
            shade = (0.35 + 0.65 * lam) * (1.06 - 0.22 * max(0.0, v - 0.45) / 0.55)   # its belly a little shadowed
            shade = max(0.0, min(1.0, shade))
            if shade < 0.7:
                c = [x + (y - x) * (shade / 0.7) for x, y in zip(dark, mid)]
            else:
                c = [x + (y - x) * ((shade - 0.7) / 0.3) for x, y in zip(mid, lit)]
            n = rnd.randint(-2, 2)
            edge = min(1.0, cov / 0.4) ** 1.5                      # thinning to nothing over its outer part
            a = 236 * edge
            img.putpixel((i, j), tuple(max(0, min(255, int(x + n))) for x in c) + (max(0, min(255, int(a))),))
    return img


def cloud_wisp(rnd, size=SPRITE):
    """A streak of mist: long and low, thinning to nothing at its ends and edges, faintly banded."""
    from PIL import Image
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for j in range(size):
        for i in range(size):
            u, v = (i + 0.5) / size * 2 - 1, (j + 0.5) / size * 2 - 1
            d = math.hypot(u, v * 1.0)
            if d >= 1:
                continue
            band = 0.75 + 0.25 * math.sin(math.atan2(v, u) * 3 + d * 9)
            a = (1 - d) ** 1.4 * 200 * band
            c = [236 + 19 * (1 - d), 240 + 15 * (1 - d), 255]
            n = rnd.randint(-3, 3)
            img.putpixel((i, j), tuple(max(0, min(255, int(x + n))) for x in c) + (max(0, min(255, int(a))),))
    return img


def cloud_sprites():
    """The cloud's sprite sheet (256x256): a big heaped puff, a second unlike it, a small lumpy one, a
    round wisp of mist (laid flat about the cloud's foot)."""
    import random
    from PIL import Image
    rnd = random.Random(31)
    sheet = Image.new('RGBA', (SPRITE * 2, SPRITE * 2), (0, 0, 0, 0))
    sheet.paste(cloud_puff(rnd, [(0.5, 0.56, 0.3), (0.3, 0.62, 0.2), (0.72, 0.6, 0.22), (0.42, 0.38, 0.2),
                                 (0.62, 0.4, 0.17), (0.5, 0.7, 0.22)]), (0, 0))
    sheet.paste(cloud_puff(rnd, [(0.45, 0.55, 0.27), (0.68, 0.5, 0.24), (0.3, 0.66, 0.18), (0.55, 0.33, 0.19),
                                 (0.78, 0.68, 0.15), (0.5, 0.72, 0.2)]), (SPRITE, 0))
    sheet.paste(cloud_puff(rnd, [(0.5, 0.55, 0.26), (0.32, 0.6, 0.19), (0.68, 0.62, 0.19), (0.5, 0.36, 0.16)]),
                (0, SPRITE))
    sheet.paste(cloud_wisp(rnd), (SPRITE, SPRITE))
    sheet.save('src/main/resources/assets/jttw_engine/textures/entity/zhenyuan_cloud.png')


# where the figure stands over the cloud: lifted to make room for the long robe (ZhenyuanModel)
FIGURE_Y = -4

ZHENYUAN = mirrored([
    # the small cloud he stands on (drawn by ZhenyuanModel.renderCloud), and the figure over it (it rises and falls a little)
    part('cloud', 'root', (0, 22.4, 0), (0, 0, 0), []),
    part('figure', 'root', (0, FIGURE_Y, 0), (0, 0, 0), []),
    # a tall, narrow-waisted figure: the breast, a waist drawn in under the jade belt, and the robe
    # belling out below it in tiers to a wide hem
    part('body', 'figure', (0, -10, 0), (0, 0, 0), [
        (-4.5, 0, -2.7, 9, 9, 5.4, 'robe'),
        (-5.6, -0.4, -3.2, 11.2, 3.8, 6.4, 'mantle'),
        (-3.8, 8.6, -2.3, 7.6, 4.4, 4.6, 'waist'),
        (-4.3, 12.4, -2.7, 8.6, 2, 5.4, 'belt'),
        (-5, 14, -3.2, 10, 7, 6.4, 'skirt_upper'),
        (-6.2, 20.5, -3.9, 12.4, 7.2, 7.8, 'skirt_mid'),
        (-7.4, 27.2, -4.6, 14.8, 6.6, 9.2, 'skirt_lower'),
        (-8.6, 33.4, -5.4, 17.2, 1.8, 10.8, 'hem'),
        (2.4, 14.6, -3.6, 1.4, 3, 0.4, 'pendant'),
    ]),
    # the earth-gold apron from the belt, and the jade belt's two sash ends beside it
    part('apron', 'body', (0, 14.2, -3.3), (-0.13, 0, 0), [(-2.1, 0, -0.5, 4.2, 19, 0.5, 'apron')]),
    part('right_sash', 'body', (-1.6, 13.6, -2.9), (-0.08, 0, 0.05), [(-0.7, 0, -0.2, 1.4, 9, 0.4, 'sash')]),
    # the outer robe's panels, hung from the waist and swinging out over the tiers: two at the front,
    # parted on the under-robe, one at each side, one down the back; each with its folds
    part('right_front_panel', 'body', (-2.4, 13, -3.4), (-0.14, 0.22, 0.12), [
        (-4.4, 0, -0.35, 4.4, 22, 0.7, 'panel'),
        (-3.4, 1, -0.75, 0.8, 20.5, 0.45, 'fold'),
        (-1.6, 2, -0.75, 0.8, 19.5, 0.45, 'fold'),
    ]),
    part('right_side_panel', 'body', (-5.2, 13, 0), (0, 0, 0.22), [
        (-0.35, 0, -3.5, 0.7, 22, 7, 'panel_side'),
        (-0.75, 1.5, -1.4, 0.45, 20, 0.8, 'fold'),
        (-0.75, 1.5, 1.4, 0.45, 20, 0.8, 'fold'),
    ]),
    part('back_panel', 'body', (0, 13, 3.2), (0.14, 0, 0), [
        (-5, 0, -0.35, 10, 22, 0.7, 'panel_back'),
        (-2.8, 1, 0.3, 0.8, 20.5, 0.45, 'fold'),
        (2, 1, 0.3, 0.8, 20.5, 0.45, 'fold'),
    ]),
    part('train', 'body', (0, 34.4, 5), (-0.32, 0, 0), [(-7.4, -0.6, -0.4, 14.8, 0.8, 8, 'train')]),
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
    # it billows: narrow at the arm, belling wide to its mouth, a loose fold swinging out from it
    part('right_sleeve', 'right_arm', (-1.2, 3, 1.2), (0, 0, 0), [
        (-3.5, 0, -3.5, 7, 7, 8, 'sleeve_top'),
        (-4.6, 6.4, -4.4, 9.2, 10.6, 9.8, 'sleeve'),
        (-5.1, 15.8, -4.9, 10.2, 1.4, 0.6, 'lip'),
        (-5.1, 15.8, 5.3, 10.2, 1.4, 0.6, 'lip'),
        (-5.1, 15.8, -4.3, 0.6, 1.4, 9.6, 'lip_side'),
        (4.5, 15.8, -4.3, 0.6, 1.4, 9.6, 'lip_side'),
    ]),
    part('right_sleeve_flap', 'right_sleeve', (-4.6, 6, 0.4), (0, 0, 0.2), [(-0.4, 0, -4.6, 0.8, 11.5, 9.4, 'sleeve_flap')]),
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

def soft(p, xy, colour, alpha):
    """A pixel of the skin, partly see-through (the cloud and the halo are drawn translucent)."""
    x, y = int(xy[0]), int(xy[1])
    if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
        a = max(0, min(255, int(alpha)))
        p.img.putpixel((x, y), tuple(max(0, min(255, int(v))) for v in colour[:3]) + (a,) if a else (0, 0, 0, 0))


def mixc(a, b, t):
    return [x + (y - x) * t for x, y in zip(a, b)]


def folds(p, rect, base, period=7, depth=0.16, phase=0.0, light=1.0):
    """Cloth falling in soft vertical folds: lit ridges, shadowed hollows, a darker crease in each."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        s = math.sin((i + phase) * 2 * math.pi / period)
        kk = light * (1 + depth * s)
        crease = s < -0.92
        for j in range(fh):
            fall = 1 - 0.18 * j / max(1, fh)
            c = [v * kk * fall * (0.8 if crease else 1) + p.rnd.randint(-3, 3) for v in base]
            p.px((fx + i, fy + j), c)


def paint_zhenyuan(p, style, f, dims):
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

    if style == 'waist':                                          # drawn in under the breast: the robe gathered
        p.shaded(f, INDIGO)
        for k in ('front', 'back', 'right', 'left'):
            folds(p, f[k], INDIGO, period=4, depth=0.2)
        fx, fy, fw, fh = f['front']                               # the crossed collar's lapels run on down
        for j in range(fh):
            for i in range(fw // 2 - 3, fw // 2 + 3):
                p.px((fx + i, fy + j), WHITE if abs(i - fw // 2 + 0.5) < 1 else GOLD if abs(i - fw // 2 + 0.5) < 2 else EARTH)

    if style in ('skirt_upper', 'skirt_mid', 'skirt_lower'):    # the under-robe's tiers, belling out
        p.shaded(f, INDIGO, light=0.92)
        for k in ('back', 'right', 'left'):
            folds(p, f[k], INDIGO, period=6, depth=0.28, light=0.92)
        fx, fy, fw, fh = f['front']                               # the earth-gold under-robe in the parting
        folds(p, f['front'], INDIGO, period=6, depth=0.28, light=0.9)
        half = 4 + (style != 'skirt_upper') + (style == 'skirt_lower')
        for j in range(fh):
            for i in range(fw // 2 - half, fw // 2 + half):
                edge = abs(i - fw // 2 + 0.5) > half - 1
                c = GOLD if edge else [c_ * (1 + 0.1 * math.sin(i * 1.4)) + p.rnd.randint(-6, 6) for c_ in EARTH]
                p.px((fx + i, fy + j), c)
        if style == 'skirt_lower':
            for k in ('front', 'back', 'right', 'left'):
                sea_hem(p, f[k], rows=5)
        else:
            for k in ('back', 'right', 'left'):
                scrolls(p, f[k], GOLD_DEEP, every=10)

    if style == 'hem':                                            # the hem sweeping out wide: gold-bordered sea and cliffs
        p.shaded(f, INDIGO)
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(fh):
                for i in range(fw):
                    c = (VIOLET, INDIGO, (120, 96, 168), GOLD_DEEP)[((i + j) // 2) % 4]
                    p.px((fx + i, fy + j), c)
            for i in range(fw):
                p.px((fx + i, fy), GOLD)
                p.px((fx + i, fy + fh - 1), GOLD_DEEP)
        p.face(f['top'], INDIGO, 1.0)
        fx, fy, fw, fh = f['top']
        for i in range(fw):
            p.px((fx + i, fy), GOLD)
            p.px((fx + i, fy + fh - 1), GOLD)

    if style in ('panel', 'panel_side', 'panel_back'):            # the outer robe's panels: folds, scrolls, a gold border, sea at the foot
        for k, r in f.items():
            p.face(r, INDIGO, 0.9 if k in ('front', 'back') else 0.8, noise=3)
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            if fw < 3:
                for j in range(fh):
                    for i in range(fw):
                        p.px((fx + i, fy + j), GOLD_DEEP)
                continue
            outer = (k == 'front') == (style != 'panel_back') if style != 'panel_side' else k in ('right', 'left')
            folds(p, f[k], INDIGO, period=5 + (fw > 14), depth=0.32, phase=1.5, light=1.0 if outer else 0.75)
            if outer:
                scrolls(p, (fx + 2, fy + 2, fw - 4, fh - 14), GOLD_DEEP, every=9)
                sea_hem(p, f[k], rows=8)
                for j in range(fh):                               # the gold border down its edges
                    p.px((fx, fy + j), GOLD)
                    p.px((fx + fw - 1, fy + j), GOLD)
                    p.px((fx + 1, fy + j), GOLD_DEEP)
                    p.px((fx + fw - 2, fy + j), GOLD_DEEP)
            else:                                                 # its lining: violet
                for j in range(fh):
                    for i in range(fw):
                        p.px((fx + i, fy + j), [c * (0.8 - 0.15 * j / fh) for c in VIOLET])
        if style == 'panel_back':                                 # the bagua over his back's skirts
            fx, fy, fw, fh = f['back']
            bagua(p, fx + fw / 2, fy + 12, 8)

    if style == 'fold':                                           # a fold standing out of the cloth: lit along its ridge
        for k, r in f.items():
            p.face(r, INDIGO, 1.12 if k in ('front', 'back', 'top') else 0.78, noise=3)
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for j in range(fh - 16, fh):
                    t = (j - (fh - 16)) / 16
                    p.px((fx + i, fy + j), [c * (1 + 0.15 * (i % 2)) for c in mixc(INDIGO, VIOLET, t)])
                p.px((fx + i, fy + fh - 1), GOLD)
                p.px((fx + i, fy + fh - 2), GOLD_DEEP)

    if style == 'sash':                                           # the belt's sash ends: jade-green silk, gold-tipped
        for k, r in f.items():
            p.gradient(r, (90, 168, 136), (60, 120, 96), noise=4)
        for k in ('front', 'back'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for j in range(fh - 3, fh):
                    p.px((fx + i, fy + j), GOLD if j < fh - 1 else GOLD_DEEP)

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

    if style == 'halo':                                           # a ring of pale gold, the eight trigrams on it: half there, drawn translucent
        for k in ('right', 'left', 'top', 'bottom'):
            fx, fy, fw, fh = f[k]
            for j in range(fh):
                for i in range(fw):
                    soft(p, (fx + i, fy + j), PALE_GOLD, 0)
        for k in ('front', 'back'):
            fx, fy, fw, fh = f[k]
            cx, cy, R = fx + fw / 2, fy + fh / 2, fw / 2
            for y in range(fy, fy + fh):                          # a faint wash of light within the ring
                for x in range(fx, fx + fw):
                    d = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / R
                    soft(p, (x, y), (250, 240, 200), 0 if d > 0.98 else 26 * (1 - d) + 10 * (d > 0.55))
            for r0, r1, a in ((R - 2.0, R - 0.9, 190), (R - 2.6, R - 2.0, 90), (R - 0.9, R - 0.4, 80)):
                for y in range(fy, fy + fh):
                    for x in range(fx, fx + fw):
                        d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                        if r0 <= d < r1:
                            soft(p, (x, y), PALE_GOLD, a)
                            p.glow.putpixel((x, y), tuple(int(v * 0.16) for v in PALE_GOLD) + (255,))
            for y in range(fy, fy + fh):                          # the dotted inner ring
                for x in range(fx, fx + fw):
                    dx, dy = x + 0.5 - cx, y + 0.5 - cy
                    if R - 7.4 <= math.hypot(dx, dy) <= R - 6.6 and abs(math.sin(math.atan2(dy, dx) * 12)) >= 0.35:
                        soft(p, (x, y), PALE_GOLD, 130)
            for n, lines in enumerate(TRIGRAMS):
                a = -math.pi / 2 + n * math.pi / 4
                ux, uy = math.cos(a), math.sin(a)
                ccx, ccy = cx + ux * (R - 4.4), cy + uy * (R - 4.4)
                for m, whole in enumerate(lines):
                    off = (m - 1) * 2
                    for t in range(5):
                        s_ = t - 2
                        if whole == '0' and abs(s_) < 0.8:
                            continue
                        xy = (round(ccx + ux * off - uy * s_), round(ccy + uy * off + ux * s_))
                        soft(p, xy, PALE_GOLD, 200)
                        if 0 <= xy[0] < bg.TEX and 0 <= xy[1] < bg.TEX:
                            p.glow.putpixel(xy, tuple(int(v * 0.2) for v in PALE_GOLD) + (255,))

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

    if style == 'sleeve_top':                                      # the sleeve where it hangs from the arm, gathered
        p.shaded(f, INDIGO)
        for k in ('front', 'back', 'right', 'left'):
            folds(p, f[k], INDIGO, period=4, depth=0.2)
        p.face(f['top'], VIOLET, 0.9)

    if style == 'sleeve_flap':                                     # a loose fold of the sleeve swinging out
        for k, r in f.items():
            p.face(r, INDIGO, 0.85, noise=3)
        for k in ('right', 'left'):
            fx, fy, fw, fh = f[k]
            folds(p, f[k], INDIGO, period=5, depth=0.22, phase=2)
            scrolls(p, (fx, fy, fw, fh - 6), GOLD_DEEP, every=9)
            sea_hem(p, f[k], rows=5)
            for j in range(fh):
                p.px((fx, fy + j), GOLD_DEEP)
                p.px((fx + fw - 1, fy + j), GOLD_DEEP)
        for k in ('front', 'back', 'bottom'):
            p.face(f[k], GOLD, 0.9, noise=3)

    if style == 'sleeve':                                          # the great hanging sleeve; its mouth the universe
        robe(p, f)
        for k in ('front', 'back', 'right', 'left'):
            folds(p, f[k], INDIGO, period=6, depth=0.18, phase=k == 'back')
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
    cloud_sprites()
    emit(ZHENYUAN, slots, JAVA, 'zhenyuan')
    print('zhenyuan', len(ZHENYUAN), 'parts,', len(slots), 'boxes')
    from lion_camel import extent
    print('   drawn at', SIZE, {k: round(v, 2) for k, v in extent(ZHENYUAN, SIZE).items()})
