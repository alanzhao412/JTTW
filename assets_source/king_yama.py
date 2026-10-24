"""King Yama (阎罗王), Lord of the Fifth Court, judge of the dead, as Hell's temples sculpt him. As
zhenyuan.py and sea_court.py: his model and skin come from one list of parts (beastgen): the model is
written into HellRenderers' yamaLayer() between its GENERATED markers, the skin painted to the same
box-UV layout at twice the model's resolution, with two glowing skins drawn full-bright over it: his
eyes and his hellfire smouldering (king_yama_glow.png), and the same blazing in his wrath
(king_yama_wrath_glow.png).

A towering, broad judge-emperor, a head taller than the other nine kings: a face of dark oxblood,
eyes bulging and burning like coals under heavy scowling brows knit into a V, flaring black brows,
a broad nose, tusks over the lip, a wild black beard in three flaring tongues and a moustache swept
out and up, black sideburns flaring from his cheeks; the emperor's mian crown (冕旒), a tall black cap
banded in gold with a plaque bearing 王, its flat board hung before and behind with strings of red
and gold beads, red cords to jade ear-balls at his cheeks; an imperial dragon robe in layers of deep
crimson and black, gold dragons, flames and skulls worked over it, a sea-and-cliff hem of black
waves, a black cloud collar edged in gold; armoured shoulders of gold-edged black lamellae, each
bitten by a gold beast's head; a jade belt hooped round him with a skull for its buckle, jade
pendants, and the iron chain that drags souls to judgement hanging from it; great hanging sleeves
lined in black; the vermilion judge's brush (朱笔) in his right hand, the Book of Life and Death
(生死簿) in his left; and behind his head a ring of hellfire set with skulls, its flames licking
outward. In his wrath the flames flare, his eyes blaze and his jaws gape, and fire runs round his
hem.

Run from the repository root: python assets_source/king_yama.py"""
import math

import beastgen as bg
from beastgen import Painter, layout, size, emit
from fire_court import part, mirrored
from sea_court import aim
from PIL import Image

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/HellRenderers.java'

CRIMSON = (128, 16, 22)
CRIMSON_DEEP = (78, 8, 14)
BLACK = (22, 16, 20)
BLACK_SHEEN = (48, 36, 44)
GOLD = (228, 180, 64)
GOLD_DEEP = (150, 102, 34)
JADE = (96, 178, 136)
JADE_DEEP = (52, 116, 86)
IVORY = (232, 222, 196)
BONE = (214, 204, 176)
BONE_SHADE = (150, 138, 112)
FACE = (104, 32, 26)          # dark oxblood
FACE_SHADE = (62, 16, 14)
FACE_LIGHT = (140, 52, 38)
HAIR = (16, 12, 14)
HAIR_SHEEN = (70, 38, 38)
HAIR_GREY = (44, 40, 46)
MOUTH = (60, 6, 10)
IRON = (62, 60, 66)
IRON_DARK = (30, 28, 34)
VERMILION = (214, 40, 24)
# his eyes: coals, deeper than a blaze (as the Great White Bull's), hotter at the core; blazing in his wrath
EMBER = (150, 30, 10)
EMBER_CORE = (214, 84, 22)
WRATH_EMBER = (240, 92, 24)
WRATH_CORE = (255, 214, 110)
# hellfire
FIRE_DEEP = (150, 20, 8)
FIRE = (232, 76, 20)
FIRE_HOT = (255, 186, 70)

# How much larger than his pixels he is drawn: YamaRenderer.SIZE
SIZE = 0.9

ARM, FOREARM = (-0.08, 0, 0.2), (-1.05, -0.5, 0)
LARM, LFOREARM = ((x, -y, -z) for x, y, z in (ARM, FOREARM))
HALO_Y, HALO_Z = -28.5, 7.6


def halo_parts():
    """Round the ring of hellfire behind his head: skulls set over its upper part, and tongues of flame
    between them, licking outward."""
    out = []
    for k, deg in enumerate((-120, -90, -60, -30, 0, 30, 60, 90, 120)):
        a = math.radians(deg)
        x, y = 11.2 * math.sin(a), -11.2 * math.cos(a)
        out.append(part('halo_skull_%d' % k, 'halo', (x, y, -0.4), (0, 0, 0),
                        [(-1.3, -1.5, -1.8, 2.6, 3, 2, 'skull'), (-0.9, 1.3, -1.7, 1.8, 0.8, 1.4, 'skulljaw')]))
    for k, deg in enumerate(range(-135, 136, 18)):
        a = math.radians(deg)
        r = 14
        tall = 7.5 if k % 2 == 0 else 5.5
        out.append(part('halo_flame_%d' % k, 'halo', (r * math.sin(a), -r * math.cos(a), 0.3), (0, 0, a),
                        [(-2, -tall, -0.25, 4, tall, 0.5, 'flame' if k % 2 == 0 else 'flame_small')]))
    return out


def hem_flames():
    """Hellfire running round his hem in his wrath (hidden till then)."""
    out = []
    for k in range(12):
        a = k * math.pi * 2 / 12 + 0.26
        x, z = 11.8 * math.sin(a), -9.4 * math.cos(a)
        out.append(part('hem_flame_%d' % k, 'skirt', (x, 23, z), (0, -a, 0),
                        [(-3, -11 + (k % 2) * 3, -0.25, 6, 11 - (k % 2) * 3, 0.5, 'hemflame' if k % 2 == 0 else 'hemflame_small')]))
    return out


YAMA = mirrored([
    # the body from the waist up; its pivot the waist, so it carries the head, arms and halo as it breathes
    part('body', 'root', (0, 1, 0), (0, 0, 0), [
        (-7, -18, -5, 14, 18, 10, 'chest'),
        (-4.5, -19.4, -3.6, 9, 2, 7.2, 'collar'),
        (-2.6, -22, -2.6, 5.2, 4, 5.2, 'neck'),
        (-8.6, -18.7, -6, 17.2, 1, 12, 'cloudtop'),
        (-7.6, -18.2, -6.1, 15.2, 6, 0.5, 'lappet'),
        (-7.6, -18.2, 5.6, 15.2, 7, 0.5, 'lappet_back'),
        (-7.9, -5, -6.1, 15.8, 2.5, 12.2, 'jadebelt'),
        (-2.5, -5.8, -6.6, 5, 4, 1, 'beltskull'),
    ]),
    part('skirt', 'body', (0, 0, 0), (0, 0, 0), [
        (-7.6, 0, -5.5, 15.2, 8, 11, 'robe1'),
        (-8.8, 7.5, -6.5, 17.6, 8, 13, 'robe2'),
        (-10.5, 15, -8, 21, 8, 16, 'hem'),
    ]),
    part('apron', 'skirt', (0, -1, -5.8), (-0.13, 0, 0), [(-4, 0, -1, 8, 23, 1, 'apron')]),
    part('right_panel', 'skirt', (-6.2, -1, -5.1), (-0.1, 0.35, 0.05), [(-2.5, 0, -0.5, 5, 22, 0.5, 'panel')]),
    part('right_pendant', 'skirt', (-5.3, -1.6, -6.4), (-0.14, 0, 0.04), [
        (-0.4, 0, -0.4, 0.8, 5, 0.8, 'cord'), (-1.6, 5, -0.7, 3.2, 3.2, 1, 'jadedisc'), (-0.6, 8.2, -0.5, 1.2, 4, 1, 'tassel')]),
    # the chain that drags souls to judgement, hung at his left hip
    part('chain', 'skirt', (7.4, -2.6, -2.5), (0, 0, -0.08), [
        (-0.3, 0, -0.7, 0.6, 1.8, 1.4, 'link_a'),
        (-0.7, 1.5, -0.3, 1.4, 1.8, 0.6, 'link_b'),
        (-0.3, 3, -0.7, 0.6, 1.8, 1.4, 'link_a'),
        (-0.7, 4.5, -0.3, 1.4, 1.8, 0.6, 'link_b'),
        (-0.3, 6, -0.7, 0.6, 1.8, 1.4, 'link_a'),
        (-0.7, 7.5, -0.3, 1.4, 1.8, 0.6, 'link_b'),
        (-0.3, 9, -0.7, 0.6, 1.8, 1.4, 'link_a'),
        (-0.7, 10.5, -0.3, 1.4, 1.8, 0.6, 'link_b'),
        (-1.2, 12.1, -1.2, 2.4, 2.4, 2.4, 'shackle'),
    ]),
    # his court boots, their toes turned up, from under the hem
    part('right_boot', 'root', (-3.6, 22.4, -6.4), (0, 0.12, 0), [
        (-1.9, 0, -2.6, 3.8, 1.6, 4.6, 'boot'), (-1.3, -0.9, -3.4, 2.6, 1.4, 1, 'boottoe')]),
    # the arms: armoured shoulders, the forearms brought forward, great sleeves hanging from them
    part('right_arm', 'body', (-9, -16, 0), ARM, [(-4, -2, -3.2, 5.6, 10, 6.4, 'sleeve')]),
    part('right_pauldron', 'right_arm', (-1.4, -2.6, 0), (0, 0, 0.32), [
        (-4.6, -1.8, -4.4, 6, 2, 9, 'pauldron'),
        (-5.4, 0.2, -4.8, 6.5, 2, 10, 'pauldron2'),
        (-6.2, 2.2, -5.2, 7, 2.5, 10.5, 'pauldron3'),
        (-6.8, -0.8, -2.5, 1, 4.5, 5, 'beastface'),
        (-7.3, 0.6, -1.8, 0.5, 1.5, 3.5, 'beastjaw'),
    ]),
    part('right_forearm', 'right_arm', (-1.2, 7.4, 0), FOREARM, [
        (-2.6, 0, -2.6, 5.2, 8, 5.2, 'forearm'),
        (-3.1, 5.8, -3.1, 6.2, 2.2, 6.2, 'cuff'),
    ]),
    part('right_sleeve', 'right_forearm', (0, 0.4, 2.4), aim([ARM, FOREARM], (0.05, -0.3, 0.14)), [
        (-3.6, -1, -10.5, 7.2, 13.5, 10.5, 'bigsleeve')]),
    part('right_hand', 'right_forearm', (0, 8.2, 0), (0, 0, 0), [(-1.7, 0, -1.7, 3.4, 3.4, 3.4, 'hand')]),
    # the vermilion brush, held up in his right fist
    part('brush', 'right_hand', (0, 1.8, -0.2), aim([ARM, FOREARM], (0.2, 0, -0.15)), [
        (-0.5, -14, -0.5, 1, 18, 1, 'brushhandle'),
        (-0.8, -15, -0.8, 1.6, 1.5, 1.6, 'ferrule'),
        (-0.8, 3.5, -0.8, 1.6, 1.5, 1.6, 'ferrule'),
        (-1, -19, -1, 2, 4, 2, 'brushtip'),
    ]),
    # the Book of Life and Death, in his left
    part('book', 'left_hand', (0.4, 2.4, -1.6), aim([LARM, LFOREARM], (-0.32, 0.2, 0)), [
        (-3.2, -4.4, -1.2, 6.4, 8.8, 2.4, 'book'),
        (-3.5, -4.6, -0.3, 0.6, 9.2, 0.6, 'bookspine'),
    ]),
    # the head: broad, heavy-jowled
    part('head', 'body', (0, -21.6, -1), (0, 0, 0), [
        (-5, -10, -4.5, 10, 7, 9, 'face'),
        (-5.8, -5.6, -3.2, 11.6, 3, 6.6, 'cheek'),
        (-3.6, -3.6, -4.1, 7.2, 3, 5, 'maw'),
        (-1.4, -6.4, -5.5, 2.8, 2.8, 1.2, 'nose'),
        (-5.3, -10.3, -0.6, 10.6, 9.4, 5.6, 'hair_back'),
        (-2.9, -3.4, -4.9, 0.9, 1.7, 0.8, 'fang'),
        (2, -3.4, -4.9, 0.9, 1.7, 0.8, 'fang'),
    ]),
    part('right_eye', 'head', (-2.5, -6.7, -4.5), (0, 0.18, 0), [(-1.6, -0.8, -0.5, 3.2, 1.8, 0.6, 'eye')]),
    part('right_brow', 'head', (-0.3, -7.5, -4.8), (0, 0.12, 0.34), [(-5.4, -1.5, -0.9, 5.4, 1.9, 2.2, 'brow')]),
    part('right_browflame', 'right_brow', (-5, -1, 0.4), (0.2, -0.5, 0.5), [(-0.25, -4, 0, 0.5, 4.5, 4.5, 'browflame')]),
    part('right_sideburn', 'head', (-5.6, -6.4, -1.2), (0.05, 0.25, 0.55), [(-0.4, 0, -1.6, 0.8, 7.5, 3.2, 'sideburn')]),
    part('right_moustache', 'head', (-0.7, -4.0, -5.6), (0, 0.22, -0.12), [(-5.5, -0.75, -0.75, 5.5, 1.5, 1.5, 'moustache')]),
    part('right_moustache_tip', 'right_moustache', (-5.2, 0, 0), (0, 0.2, 0.8), [(-4, -0.5, -0.5, 4, 1, 1, 'moustache_tip')]),
    part('jaw', 'head', (0, -3, -0.8), (0, 0, 0), [
        (-4.7, 0, -3.9, 9.4, 3.2, 7.4, 'jaw'),
        (-2.6, -0.9, -3.8, 0.8, 1, 0.7, 'tusk'),
        (1.8, -0.9, -3.8, 0.8, 1, 0.7, 'tusk'),
    ]),
    part('beard', 'jaw', (0, 2, -3.9), (0.16, 0, 0), [(-3, 0, -1, 6, 8.5, 2, 'beard')]),
    part('beard_tip', 'beard', (0, 8.2, 0), (-0.08, 0, 0), [(-2.5, 0, -1, 5, 7, 2, 'beard_tip')]),
    part('right_beard', 'jaw', (-3.4, 1.4, -3.2), (0.14, 0.25, 0.62), [(-2, 0, -1, 4, 10, 2, 'sidebeard')]),
    # the mian crown: a tall cap banded in gold, its plaque bearing 王; the flat board, a little low at
    # the front, its strings of beads; red cords to jade balls at the ears
    part('crown', 'head', (0, -10, 0), (0, 0, 0), [
        (-4.2, -4.6, -3.8, 8.4, 4.8, 7.6, 'crown_cap'),
        (-5.4, -1.2, -4.9, 10.8, 1.6, 9.8, 'crown_band'),
        (-2.4, -4.2, -4.4, 4.8, 3.4, 0.6, 'crown_plaque'),
        (-0.6, -6.6, -0.6, 1.2, 2.2, 1.2, 'crown_pin'),
        (-7, -5.2, -0.4, 14, 0.8, 0.8, 'crown_hairpin'),
    ]),
    part('board', 'crown', (0, -4.6, 0), (0.07, 0, 0), [(-6.8, -0.8, -9.2, 13.6, 0.8, 16.4, 'board')]
         + [(-6 + 2 * k - 0.25, 0, -8.9, 0.5, 4.5, 0.5, 'beads') for k in range(7)]
         + [(-6 + 2 * k - 0.25, 0, 6.6, 0.5, 4.5, 0.5, 'beads') for k in range(7)]),
    part('right_eartie', 'crown', (-7, -4.8, 0), (0, 0, -0.08), [
        (-0.25, 0, -0.25, 0.5, 6, 0.5, 'cord_red'), (-0.8, 6, -0.8, 1.6, 1.6, 1.6, 'earball')]),
    # the ring of hellfire behind his head
    part('halo', 'body', (0, HALO_Y, HALO_Z), (0, 0, 0), [(-15, -15, 0, 30, 30, 1, 'halo')]),
] + halo_parts() + hem_flames())


# ── Painting helpers ─────────────────────────────────────────────────────────

def exact_faces(u, v, w, h, d):
    """A box's six faces in the texture where the game maps them: by its true (fractional) size, to the
    nearest texture pixel (beastgen.faces takes the size rounded up, which shifts a thin box's front)."""
    U = lambda a: int(round(a * bg.S))
    def rect(x0, y0, x1, y1):
        return U(x0), U(y0), max(1, U(x1) - U(x0)), max(1, U(y1) - U(y0))
    return {'top': rect(u + d, v, u + d + w, v + d), 'bottom': rect(u + d + w, v, u + d + 2 * w, v + d),
            'right': rect(u, v + d, u + d, v + d + h), 'front': rect(u + d, v + d, u + d + w, v + d + h),
            'left': rect(u + d + w, v + d, u + 2 * d + w, v + d + h), 'back': rect(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}


def paint(parts, painter, paint_box):
    """As beastgen.paint, each box's faces where the game will look for them."""
    slots = layout(parts)
    true = {}
    for _, _, _, _, boxes, _ in parts:
        for b in boxes:
            true.setdefault(size(b) + (b[6],), b[3:6])
    for key, (u, v) in slots.items():
        w, h, d = true[key]
        paint_box(painter, key[3], exact_faces(u, v, w, h, d), (w, h, d))
    return slots


class YamaPainter(Painter):
    """A Painter with a third skin: the glow in his wrath."""

    def __init__(self, seed):
        super().__init__(seed)
        self.wrath = Image.new('RGBA', (bg.TEX, bg.TEX), (0, 0, 0, 0))

    def lit(self, xy, colour, calm=1.0, wrath=1.0, skin=None, hot=None):
        """A burning pixel: on the skin `skin` (or the colour), on the glowing skin the colour at `calm`,
        on his wrath's the colour (or `hot`) at `wrath`."""
        x, y = int(xy[0]), int(xy[1])
        if not (0 <= x < bg.TEX and 0 <= y < bg.TEX):
            return
        self.px(xy, skin if skin is not None else colour)
        k = lambda c, f: tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)
        if calm:
            self.glow.putpixel((x, y), k(colour, calm))
        self.wrath.putpixel((x, y), k(hot or colour, wrath))

    def clear(self, xy):
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            for im in (self.img, self.glow, self.wrath):
                im.putpixel((x, y), (0, 0, 0, 0))

    def save(self, name):
        super().save(name)
        self.wrath.save('src/main/resources/assets/jttw_engine/textures/entity/%s_wrath_glow.png' % name)


def k(c, f):
    return [v * f for v in c[:3]]


def mixc(a, b, t):
    return [x + (y - x) * t for x, y in zip(a, b)]


def noisy(p, c, n=5):
    return [v + p.rnd.randint(-n, n) for v in c]


def edges(p, f, colour, sides=('front', 'back', 'right', 'left'), top=True, bottom=True, width=1):
    for s in sides:
        fx, fy, fw, fh = f[s]
        for i in range(fw):
            for w in range(width):
                if top:
                    p.px((fx + i, fy + w), colour)
                if bottom:
                    p.px((fx + i, fy + fh - 1 - w), colour)


def vedges(p, rect, colour, width=1):
    fx, fy, fw, fh = rect
    for j in range(fh):
        for w in range(width):
            p.px((fx + w, fy + j), colour)
            p.px((fx + fw - 1 - w, fy + j), colour)


def cloth(p, f, base, light=1.0, folds=True):
    """Heavy silk: lit above, a fall down its sides, faint folds."""
    p.shaded(f, base, light=light)
    if folds:
        for s in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[s]
            for i in range(2, fw, 4 + (fw > 16)):
                for j in range(fh):
                    if p.rnd.random() < 0.75:
                        p.px((fx + i, fy + j), k(base, 0.8 * light))


def strands(p, rect, base, sheen, lean=0):
    """Hair in locks: a lit edge, a grey-black body, a dark parting, a little ragged."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        kk = 0.9 + 0.15 * p.rnd.random()
        for j in range(fh):
            n = (i + (j * lean) // 4 + (j // 9) * 2) % 3
            c = sheen if n == 0 else HAIR_GREY if n == 1 else base
            p.px((fx + i, fy + j), [v * kk * (1 - 0.15 * j / max(1, fh)) + p.rnd.randint(-3, 3) for v in c])


def point(p, rect, keep=0.0, down=True, sharp=0.8):
    """Cuts a face to a point (transparent outside): toward its bottom (or its top)."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        t = (fh - 1 - j) if down else j
        half = max(keep, (t + 1) * sharp)
        for i in range(fw):
            if abs(i - fw / 2 + 0.5) > half:
                p.clear((fx + i, fy + j))


def ragged(p, rect, teeth=3, depth=4, down=True):
    """Cuts a face's end into ragged tufts."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        t = (i % teeth) / max(1, teeth - 1)
        cut = int(depth * abs(t - 0.5) * 2) + p.rnd.randint(0, 1)
        for j in range(cut):
            p.clear((fx + i, fy + fh - 1 - j) if down else (fx + i, fy + j))


SKULL = ['.####.',
         '######',
         '#..#..',
         '#..#..',
         '######',
         '.#.#.#']


def skull_mark(p, x, y, colour, shadow, mirror=False):
    """A little skull, 6x6 texture pixels."""
    for j, row in enumerate(SKULL):
        for i, c in enumerate(row):
            ii = 5 - i if mirror else i
            if c == '#':
                p.px((x + ii, y + j), colour)
            elif j in (2, 3) and i in (1, 2, 4):
                p.px((x + ii, y + j), shadow)


def skull_row(p, rect, y, colour, shadow, every=10, start=2):
    fx, fy, fw, fh = rect
    for i in range(start, fw - 5, every):
        skull_mark(p, fx + i, fy + y, colour, shadow)
        p.px((fx + i + 1, fy + y + 2), shadow)
        p.px((fx + i + 4, fy + y + 2), shadow)


def flames_band(p, rect, top, rows, colour, tip, every=5, glow=0.0):
    """A band of upward flame tongues painted across a face from row `top`, `rows` tall."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        ph = (i % every) / every
        reach = rows * (0.35 + 0.65 * (1 - abs(ph - 0.5) * 2)) + p.rnd.uniform(-0.8, 0.8)
        for j in range(rows):
            up = rows - 1 - j
            if up <= reach:
                t = up / max(1, reach)
                c = mixc(tip, colour, t)
                if glow:
                    p.lit((fx + i, fy + top + j), c, calm=glow * 0.6, wrath=glow, skin=c)
                else:
                    p.px((fx + i, fy + top + j), c)


def sea_hem(p, rect, rows=10):
    """The sea-and-cliff hem (海水江崖), in Hell's colours: slanting waves of black and crimson between gold lines."""
    fx, fy, fw, fh = rect
    top = fy + fh - rows
    band = (BLACK, CRIMSON_DEEP, BLACK_SHEEN, GOLD_DEEP, CRIMSON_DEEP)
    for j in range(rows):
        for i in range(fw):
            c = band[((i + j) // 2) % len(band)]
            p.px((fx + i, top + j), noisy(p, c, 4))
    for i in range(fw):                                            # crests of the cliffs rising from it
        if (i // 6) % 3 == 1:
            h = 3 + (i % 6 < 3) * 2 - abs(i % 6 - 3)
            for j in range(max(0, h)):
                p.px((fx + i, top - 1 - j), GOLD_DEEP if j == h - 1 else BLACK_SHEEN)
    for i in range(fw):
        p.px((fx + i, top - 1), GOLD)
        p.px((fx + i, fy + fh - 1), GOLD)
        p.px((fx + i, fy + fh - 2), GOLD_DEEP)


SCROLL = ['.##..##.',
          '#..##..#',
          '#.#..#.#',
          '.#....#.']


def scrolls(p, rect, colour, every=12, rows=9, skip_bottom=0, skip_top=0):
    fx, fy, fw, fh = rect
    for kk, j in enumerate(range(2 + skip_top, fh - 4 - skip_bottom, rows)):
        for i in range((kk * 5) % every - 2, fw - 7, every):
            if i >= 0:
                for jj, row in enumerate(SCROLL):
                    for ii, c in enumerate(row):
                        if c == '#':
                            p.px((fx + i + ii, fy + j + jj), colour)


def circle(p, cx, cy, r0, r1, colour):
    for y in range(int(cy - r1 - 1), int(cy + r1 + 2)):
        for x in range(int(cx - r1 - 1), int(cx + r1 + 2)):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r0 <= d <= r1:
                p.px((x, y), colour)


def dragon_roundel(p, cx, cy, r):
    """A coiled gold dragon in a roundel: a ring, a body coiling inward, a head at the top with a flaming pearl."""
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d <= r:
                p.px((x, y), noisy(p, BLACK, 3))
    circle(p, cx, cy, r - 1, r + 0.2, GOLD)
    for t in range(0, 260):                                       # the body: a spiral, thick, scaled
        a = t * 0.06
        rr = (r - 2.5) * (1 - t / 330)
        x, y = cx + math.cos(a) * rr, cy + math.sin(a) * rr
        for ox, oy in ((0, 0), (1, 0), (0, 1)):
            p.px((x + ox, y + oy), GOLD if (t // 6) % 2 else GOLD_DEEP)
    hx, hy = cx + math.cos(0) * (r - 2.5), cy                   # the head, and its fiery pearl
    for i in range(-1, 3):
        for j in range(-2, 2):
            p.px((hx + i, hy + j), GOLD)
    p.px((hx + 1, hy - 1), VERMILION)
    for i in range(-1, 2):
        for j in range(-1, 2):
            p.px((cx + i, cy - r * 0.45 + j), VERMILION if (i, j) != (0, 0) else FIRE_HOT)


# ── The skin ─────────────────────────────────────────────────────────────────

def paint_yama(p, style, f, dims):
    sides = ('front', 'back', 'right', 'left')

    if style == 'chest':                                          # the dragon robe's breast
        cloth(p, f, CRIMSON)
        fx, fy, fw, fh = f['front']                               # 28x36
        dragon_roundel(p, fx + fw / 2, fy + 18, 9)
        for j in range(fh):                                       # gold-bordered side seams
            p.px((fx, fy + j), GOLD_DEEP)
            p.px((fx + fw - 1, fy + j), GOLD_DEEP)
        for s in ('right', 'left'):
            scrolls(p, f[s], GOLD_DEEP, every=9, skip_top=10)
        fx, fy, fw, fh = f['back']                                # a great skull in flames on his back
        flames_band(p, (fx, fy, fw, fh), fh - 22, 20, FIRE_DEEP, (200, 70, 24), every=6)
        skull_big(p, fx + fw / 2 - 5, fy + 12)

    if style == 'collar':                                         # the round collar: black, gold-edged
        p.shaded(f, BLACK)
        edges(p, f, GOLD)

    if style == 'neck':
        p.shaded(f, FACE_SHADE)

    if style == 'cloudtop':                                       # the cloud collar over the shoulders
        p.shaded(f, BLACK)
        fx, fy, fw, fh = f['top']
        cx, cy = fx + fw / 2, fy + fh / 2
        for y in range(fy, fy + fh):
            for x in range(fx, fx + fw):
                a = math.atan2(y - cy, (x - cx) * fh / fw)
                if abs(math.sin(a * 4)) < 0.16:
                    p.px((x, y), GOLD_DEEP)
                elif abs(math.sin(a * 4 + 0.8)) < 0.1:
                    p.px((x, y), CRIMSON)
        edges(p, f, GOLD)

    if style in ('lappet', 'lappet_back'):                        # the cloud collar's points, down breast and back
        for kk, r in f.items():
            p.face(r, BLACK, 1.0, noise=4)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                t = (i % 10) / 9
                depth = int(fh * (1 - abs(t - 0.5) * 2) * 0.85) + 2
                for j in range(fh):
                    if j >= depth:
                        p.clear((fx + i, fy + j))
                    elif j >= depth - 2:
                        p.px((fx + i, fy + j), GOLD)
                    elif j == depth - 3:
                        p.px((fx + i, fy + j), GOLD_DEEP)
                if 3 <= i % 10 <= 6 and depth > 6:
                    p.px((fx + i, fy + depth - 5), CRIMSON)

    if style == 'jadebelt':                                       # jade plaques on red leather, gold-rimmed
        p.shaded(f, CRIMSON_DEEP)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for i in range(1, fw - 2, 5):
                for j in range(1, fh - 1):
                    for w in range(3):
                        p.px((fx + i + w, fy + j), JADE if w < 2 else JADE_DEEP)
                p.px((fx + i, fy + 1), (170, 226, 196))
        edges(p, f, GOLD)

    if style == 'beltskull':                                     # the buckle: a gold skull
        p.shaded(f, GOLD_DEEP)
        fx, fy, fw, fh = f['front']                               # 10x8
        for j in range(fh):
            for i in range(fw):
                p.px((fx + i, fy + j), GOLD)
        for i, j in ((2, 2), (3, 2), (2, 3), (3, 3), (6, 2), (7, 2), (6, 3), (7, 3)):
            p.lit((fx + i, fy + j), EMBER, calm=0.5, wrath=1.0, skin=(30, 10, 8), hot=WRATH_EMBER)
        p.px((fx + 4, fy + 4), GOLD_DEEP)
        p.px((fx + 5, fy + 4), GOLD_DEEP)
        for i in range(1, fw - 1):
            p.px((fx + i, fy + 6), GOLD_DEEP if i % 2 else (40, 24, 12))
        for i in (0, fw - 1):
            p.px((fx + i, fy), GOLD_DEEP)
            p.px((fx + i, fy + fh - 1), GOLD_DEEP)

    if style in ('robe1', 'robe2', 'hem'):                       # the robe's tiers
        cloth(p, f, CRIMSON if style != 'robe2' else (112, 14, 20))
        for s in sides:
            fx, fy, fw, fh = f[s]
            if style == 'hem':
                skull_row(p, f[s], 3, BONE, BONE_SHADE, every=11, start=3 + (s == 'back') * 4)
                flames_band(p, f[s], 9, fh - 9 - 10, FIRE_DEEP, (196, 70, 24), every=5)
                sea_hem(p, f[s], rows=10)
            else:
                scrolls(p, f[s], GOLD_DEEP, every=11, skip_bottom=3)
                for i in range(fw):                               # gold at the tier's lower edge
                    p.px((fx + i, fy + fh - 1), GOLD)
                    p.px((fx + i, fy + fh - 2), GOLD_DEEP)
        if style == 'robe1':
            fx, fy, fw, fh = f['front']                           # the under-robe's black opening
            for j in range(fh):
                for i in range(fw // 2 - 5, fw // 2 + 5):
                    edge = abs(i - fw // 2 + 0.5) > 4
                    p.px((fx + i, fy + j), GOLD if edge else noisy(p, BLACK, 3))

    if style == 'apron':                                         # the front panel: black, a skull over flames, a fringe
        for kk, r in f.items():
            p.face(r, BLACK, 1.0, noise=4, fall=0.15)
        fx, fy, fw, fh = f['front']                               # 16x46
        vedges(p, (fx, fy, fw, fh), GOLD)
        for i in range(fw):
            p.px((fx + i, fy), GOLD)
            p.px((fx + i, fy + 1), GOLD_DEEP)
        dragon_band = fy + 4
        for j in range(0, 8):                                     # a gold dragon-scale band at its head
            for i in range(2, fw - 2):
                if (i + (j // 2) * 2) % 4 == 0 or j in (0, 7):
                    p.px((fx + i, dragon_band + j), GOLD_DEEP)
        flames_band(p, (fx + 2, fy, fw - 4, fh), 20, 18, FIRE_DEEP, FIRE, every=4, glow=0.35)
        skull_big(p, fx + 3, fy + 16)
        for i in range(fw):                                       # the fringe: gold and red tassels
            for j in range(fh - 6, fh):
                if i % 2 == 0 or j < fh - 3:
                    p.px((fx + i, fy + j), GOLD if i % 4 < 2 else VERMILION)
                else:
                    p.clear((fx + i, fy + j))

    if style == 'panel':                                         # side panels, crimson edged gold, flames up their feet
        for kk, r in f.items():
            p.face(r, CRIMSON_DEEP, 1.0, noise=4, fall=0.2)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            vedges(p, f[s], GOLD)
            scrolls(p, (fx + 1, fy, fw - 2, fh - 14), GOLD_DEEP, every=8, rows=10)
            flames_band(p, (fx + 1, fy, fw - 2, fh), fh - 14, 12, FIRE_DEEP, (190, 60, 20), every=4)
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), GOLD)

    if style == 'cord':
        p.shaded(f, VERMILION)
    if style == 'cord_red':
        p.shaded(f, VERMILION)
    if style == 'jadedisc':                                      # a bi disc of jade
        p.shaded(f, JADE)
        fx, fy, fw, fh = f['front']
        circle(p, fx + fw / 2, fy + fh / 2, 0, 1.2, JADE_DEEP)
        circle(p, fx + fw / 2, fy + fh / 2, fw / 2 - 0.8, fw / 2, JADE_DEEP)
    if style == 'tassel':
        for kk, r in f.items():
            strands(p, r, VERMILION, (150, 20, 16))
    if style == 'earball':
        p.shaded(f, JADE)
        for s in sides:
            fx, fy, fw, fh = f[s]
            p.px((fx + 1, fy + 1), (180, 236, 206))

    if style in ('link_a', 'link_b'):                             # iron links, dark through their middles
        p.shaded(f, IRON)
        for s in sides:
            fx, fy, fw, fh = f[s]
            if fw >= 2 and fh >= 3:
                for j in range(1, fh - 1):
                    for i in range(1, max(2, fw - 1)):
                        if fw > 2:
                            p.px((fx + i, fy + j), IRON_DARK)
    if style == 'shackle':                                       # an iron collar at the chain's end
        p.shaded(f, IRON)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh // 2), IRON_DARK)
            p.px((fx + fw // 2, fy + 1), (120, 118, 126))

    if style == 'boot':                                          # black court boots, white soles
        p.shaded(f, BLACK)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), IVORY)
                p.px((fx + i, fy + fh - 2), (200, 192, 170))
            for i in range(fw):
                p.px((fx + i, fy), GOLD_DEEP)
    if style == 'boottoe':
        p.shaded(f, BLACK)
        edges(p, f, GOLD_DEEP, bottom=False)

    if style == 'sleeve':                                        # the upper arm's sleeve
        cloth(p, f, CRIMSON)
        for s in sides:
            scrolls(p, f[s], GOLD_DEEP, every=9)

    if style in ('pauldron', 'pauldron2', 'pauldron3'):          # lamellae: black plates, gold rims, rows of studs
        p.shaded(f, BLACK_SHEEN)
        for s in sides + ('top',):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    if (i + (j // 2) * 2) % 4 == 0:
                        p.px((fx + i, fy + j), BLACK)
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), GOLD)
                if s != 'top':
                    p.px((fx + i, fy + fh - 2), GOLD_DEEP)
            for i in range(1, fw - 1, 3):
                p.px((fx + i, fy + 1), GOLD)
    if style == 'beastface':                                     # the gold beast's head biting at the shoulder (吞口)
        p.shaded(f, GOLD_DEEP)
        fx, fy, fw, fh = f['right']                               # the face that looks out from the shoulder: 9x8
        for j in range(fh):
            for i in range(fw):
                p.px((fx + i, fy + j), noisy(p, GOLD, 6))
        for i in (1, 2, fw - 3, fw - 2):                          # brows and glaring eyes
            p.px((fx + i, fy + 1), GOLD_DEEP)
            p.lit((fx + i, fy + 2), EMBER, calm=0.45, wrath=1.0, skin=(60, 14, 8), hot=WRATH_EMBER)
        for i in range(2, fw - 2):
            p.px((fx + i, fy + 4), GOLD_DEEP)
        p.px((fx + fw // 2, fy + 3), GOLD_DEEP)
        for kk in ('left',):
            p.face(f[kk], GOLD_DEEP, 0.8)
    if style == 'beastjaw':
        p.shaded(f, GOLD)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for i in range(0, fw, 2):
                p.px((fx + i, fy), IVORY)

    if style == 'forearm':
        cloth(p, f, CRIMSON)
    if style == 'cuff':                                          # a gold cuff, a black band
        p.shaded(f, GOLD)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + 1), BLACK)
                p.px((fx + i, fy + 2), BLACK)

    if style == 'bigsleeve':                                     # the great sleeve: crimson, lined black, a flame-and-skull border
        cloth(p, f, CRIMSON)
        for s in sides:
            fx, fy, fw, fh = f[s]
            scrolls(p, f[s], GOLD_DEEP, every=9, skip_bottom=12)
            for j in range(fh - 11, fh):                          # the black border at its mouth
                for i in range(fw):
                    p.px((fx + i, fy + j), noisy(p, BLACK, 3))
            flames_band(p, (fx, fy, fw, fh), fh - 11, 6, FIRE_DEEP, (180, 60, 20), every=5)
            skull_row(p, (fx, fy, fw, fh), fh - 9, BONE, BONE_SHADE, every=9, start=2)
            for i in range(fw):
                p.px((fx + i, fy + fh - 12), GOLD)
                p.px((fx + i, fy + fh - 1), GOLD)
        fx, fy, fw, fh = f['bottom']                              # the mouth: dark within, a gold rim
        for j in range(fh):
            for i in range(fw):
                edge = min(i, j, fw - 1 - i, fh - 1 - j)
                p.px((fx + i, fy + j), GOLD if edge == 0 else BLACK if edge < 3 else (10, 4, 6))

    if style == 'hand':                                          # his fists, dark as his face
        p.shaded(f, FACE)
        for s in ('front', 'right', 'left'):
            fx, fy, fw, fh = f[s]
            for i in range(0, fw, 2):
                p.px((fx + i, fy + fh - 2), FACE_SHADE)
                p.px((fx + i, fy + fh - 1), FACE_SHADE)

    if style == 'brushhandle':                                   # black lacquer, banded gold
        p.shaded(f, BLACK_SHEEN)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 7):
                for i in range(fw):
                    p.px((fx + i, fy + j), GOLD_DEEP)
    if style == 'ferrule':
        p.shaded(f, GOLD)
    if style == 'brushtip':                                      # the hair dipped in vermilion, to a point at the top
        for kk, r in f.items():
            p.gradient(r, VERMILION, (40, 30, 30), noise=6)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for j in range(fh // 2):
                for i in range(fw):
                    if abs(i - fw / 2 + 0.5) > (j + 1) * 0.6:
                        p.clear((fx + i, fy + j))

    if style == 'book':                                          # the Book of Life and Death: a dark blue cover, the title label, a seal
        for kk, r in f.items():
            p.face(r, (26, 30, 58), 1.0, noise=4)
        for s in ('right', 'left', 'top', 'bottom'):              # the leaves' edges
            fx, fy, fw, fh = f[s]
            p.face((fx, fy, fw, fh), IVORY, 0.95, noise=6)
            if s in ('right', 'left', 'top', 'bottom'):
                for i in range(fw):
                    for j in range(fh):
                        if (j if s in ('right', 'left') else i) % 3 == 0:
                            p.px((fx + i, fy + j), (196, 186, 160))
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]                                 # 13x18
            for i in range(fw):
                p.px((fx + i, fy), (16, 18, 36))
                p.px((fx + i, fy + fh - 1), (16, 18, 36))
            lx = fx + fw - 6 if s == 'front' else fx + 2          # the title label: pale, three dark marks (生死簿)
            for j in range(2, fh - 3):
                for i in range(4):
                    p.px((lx + i, fy + j), (226, 214, 176))
            for j in (4, 5, 8, 9, 12, 13):
                p.px((lx + 1, fy + j), (30, 20, 20))
                p.px((lx + 2, fy + j), (30, 20, 20))
            p.px((lx + 1, fy + 7), (30, 20, 20))
            p.px((lx + 2, fy + 11), (30, 20, 20))
            for i in range(2):                                    # the seal of Hell, vermilion
                for j in range(2):
                    p.lit((fx + 2 + i, fy + fh - 5 + j), VERMILION, calm=0.25, wrath=0.6)
            for j in range(fh):                                   # the stitching down the spine side
                if j % 3 == 1:
                    p.px((fx + (0 if s == 'front' else fw - 1), fy + j), IVORY)
    if style == 'bookspine':
        p.shaded(f, (20, 22, 44))
        for s in sides:
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 4):
                for i in range(fw):
                    p.px((fx + i, fy + j), GOLD_DEEP)

    if style == 'face':
        p.shaded(f, HAIR)
        for s in ('right', 'left', 'back', 'top'):
            strands(p, f[s], HAIR, HAIR_SHEEN)
        for s in ('right', 'left'):                               # the face's edge before the ear, the ear
            fx, fy, fw, fh = f[s]
            for j in range(5, fh):
                for i in range(5):
                    x = fx + (fw - 1 - i if s == 'right' else i)
                    p.px((x, fy + j), k(FACE, 0.85 - 0.04 * i))
            ex = fx + (fw - 9 if s == 'right' else 6)
            for j in range(6, 12):
                for i in range(3):
                    p.px((ex + i, fy + j), k(FACE, 0.7) if i != 1 else FACE_SHADE)
        fx, fy, fw, fh = f['front']                               # 20x14: brow to upper lip
        for j in range(fh):
            for i in range(fw):
                c = FACE_LIGHT if (j < 4 and 5 < i < 14) else FACE
                p.px((fx + i, fy + j), [v * (1 - 0.1 * j / fh) + p.rnd.randint(-3, 3) for v in c])
        for i in range(fw):                                       # black hair at the crown's band edge
            p.px((fx + i, fy), HAIR)
        for i in (8, 9, 10, 11):                                  # the furrows knotted between his brows
            for j in range(2, 6):
                if (i in (9, 10)) == (j % 2 == 0):
                    p.px((fx + i, fy + j), FACE_SHADE)
        for j in range(2, 6):
            p.px((fx + 9, fy + j), FACE_SHADE)
            p.px((fx + 10, fy + j), FACE_SHADE)
        for i in range(fw):                                       # deep sockets under the brow
            for j in (5, 6):
                if 1 <= i <= 8 or 11 <= i <= 18:
                    p.px((fx + i, fy + j), k(FACE_SHADE, 0.7))
        for i in range(fw):                                       # cheeks: heavy folds under the eyes
            for j in (10,):
                if 1 <= i <= 6 or 13 <= i <= 18:
                    p.px((fx + i, fy + j), FACE_SHADE)
        for j in range(11, fh):                                   # the snarl lines by the nose
            p.px((fx + 6 - (j - 11), fy + j), FACE_SHADE)
            p.px((fx + 13 + (j - 11), fy + j), FACE_SHADE)
        p.face(f['bottom'], MOUTH, 1.0, noise=3)                  # (seen when his jaws gape)
        fx, fy, fw, fh = f['bottom']
        for i in range(2, fw - 2):                                # the upper teeth along the front edge
            p.px((fx + i, fy + fh - 1), IVORY if i % 3 else BONE_SHADE)
            p.px((fx + i, fy + fh - 2), BONE if i % 3 else BONE_SHADE)

    if style == 'cheek':                                         # broad jowls; black whiskers at the side
        p.shaded(f, FACE)
        for s in ('right', 'left'):
            strands(p, f[s], HAIR, HAIR_SHEEN)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), FACE_SHADE)
        p.face(f['bottom'], FACE_SHADE, 1.0)

    if style == 'maw':                                           # inside his mouth: dark red, a tongue
        for kk, r in f.items():
            p.face(r, MOUTH, 1.0, noise=4)
        fx, fy, fw, fh = f['front']
        for i in range(3, fw - 3):
            for j in range(fh - 3, fh):
                p.px((fx + i, fy + j), (150, 40, 44))
        for i in range(fw):
            p.lit((fx + i, fy + 2), FIRE_DEEP, calm=0, wrath=0.9, skin=MOUTH)

    if style == 'nose':                                          # broad, flared, a dark nostril each side
        p.shaded(f, FACE_LIGHT)
        fx, fy, fw, fh = f['front']
        for i in (0, 1, fw - 2, fw - 1):
            p.px((fx + i, fy + fh - 1), (24, 6, 6))
        p.face(f['bottom'], FACE_SHADE, 1.0)

    if style == 'hair_back':                                     # his hair, bound up under the crown
        for kk, r in f.items():
            strands(p, r, HAIR, HAIR_SHEEN)

    if style in ('fang', 'tusk'):
        p.shaded(f, IVORY)
        for s in sides:
            fx, fy, fw, fh = f[s]
            p.px((fx, fy + fh - 1), BONE_SHADE)

    if style == 'eye':                                           # bulging, burning, glaring: the lid slants down toward the nose
        p.shaded(f, (30, 8, 6))
        fx, fy, fw, fh = f['front']                               # 7x4: the right eye (its inner corner at the right)
        for i in range(fw):
            for j in range(fh):
                inner = i / max(1, fw - 1)                        # 0 outer .. 1 inner
                lid = 0 if inner < 0.35 else 1 if inner < 0.75 else 2
                if j < lid:
                    p.px((fx + i, fy + j), FACE_SHADE)
                    continue
                core = abs(i - fw * 0.55) < 1.3 and j >= 1
                c = EMBER_CORE if core else EMBER
                p.lit((fx + i, fy + j), c, calm=1.0, wrath=1.0, skin=k(c, 0.35), hot=WRATH_CORE if core else WRATH_EMBER)
        p.px((fx + int(fw * 0.55), fy + fh - 1), (8, 2, 2))      # a pinprick of pupil
        p.glow.putpixel((fx + int(fw * 0.55), fy + fh - 1), (0, 0, 0, 0))
        for s in ('right', 'left', 'top', 'bottom'):              # the eyeball's rim shows red at its sides
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                for j in range(fh):
                    p.lit((fx + i, fy + j), EMBER, calm=0.5, wrath=0.9, skin=k(EMBER, 0.3), hot=WRATH_EMBER)

    if style == 'brow':                                          # the heavy brow ridge: dark skin under black, flaring hair
        p.shaded(f, FACE_SHADE)
        for s in ('top', 'front', 'back'):
            strands(p, f[s], HAIR, HAIR_SHEEN, lean=1)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), (20, 6, 6))
        p.face(f['bottom'], (24, 6, 6), 1.0)
    if style == 'browflame':                                     # the brows' ends sweep up in black flames
        for kk, r in f.items():
            strands(p, r, HAIR, HAIR_SHEEN)
        for s in ('front', 'back', 'top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    p.clear((fx + i, fy + j))
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                top = int(fh * (1 - i / fw) * 0.9)
                for j in range(fh):
                    if j < top - (i % 3):
                        p.clear((fx + i, fy + j))

    if style == 'sideburn':                                      # black sideburns flaring from the cheeks
        for kk, r in f.items():
            strands(p, r, HAIR, HAIR_SHEEN)
        for s in ('right', 'left', 'front', 'back'):
            ragged(p, f[s], teeth=3, depth=5)

    if style in ('moustache', 'moustache_tip'):
        for kk, r in f.items():
            strands(p, r, HAIR, HAIR_SHEEN)
        if style == 'moustache_tip':                              # curling up to a point
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    for j in range(fh):
                        if i < fw * 0.4 and abs(j - fh / 2 + 0.5) > i * 0.35 + 0.4:
                            p.clear((fx + i, fy + j))

    if style == 'jaw':                                           # the lower face: a black-bearded chin; the lower teeth on top
        p.shaded(f, FACE)
        for s in ('right', 'left', 'back', 'bottom'):
            strands(p, f[s], HAIR, HAIR_SHEEN)
        fx, fy, fw, fh = f['front']                               # 19x7
        for j in range(fh):
            for i in range(fw):
                if j >= 2 or i < 3 or i > fw - 4:
                    p.px((fx + i, fy + j), HAIR if (i + j) % 3 else HAIR_SHEEN)
        for i in range(4, fw - 4):                                # the lower lip, dark
            p.px((fx + i, fy), (70, 14, 14))
            p.px((fx + i, fy + 1), FACE_SHADE)
        fx, fy, fw, fh = f['top']                                 # inside: the lower teeth along the front, the tongue
        p.face((fx, fy, fw, fh), MOUTH, 1.0, noise=3)
        for i in range(2, fw - 2):
            p.px((fx + i, fy), IVORY if i % 3 else BONE_SHADE)
            p.px((fx + i, fy + 1), BONE)
        for i in range(5, fw - 5):
            for j in range(3, fh - 3):
                p.px((fx + i, fy + j), (150, 40, 44))

    if style in ('beard', 'beard_tip', 'sidebeard'):              # the wild black beard
        for kk, r in f.items():
            strands(p, r, HAIR, HAIR_SHEEN, lean=1)
        for s in ('front', 'back'):
            if style == 'beard_tip':
                point(p, f[s], keep=0.5, sharp=0.55)
            else:
                ragged(p, f[s], teeth=4, depth=4 if style == 'beard' else 7)
        for s in ('right', 'left'):
            ragged(p, f[s], teeth=3, depth=4)

    if style == 'crown_cap':                                     # the tall black cap, gold-ribbed
        p.shaded(f, BLACK)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for i in range(1, fw, 4):
                for j in range(fh):
                    p.px((fx + i, fy + j), GOLD_DEEP)
            for i in range(fw):
                p.px((fx + i, fy), GOLD)
    if style == 'crown_band':                                    # the gold band, jewels set in it
        p.shaded(f, GOLD)
        for s in sides:
            fx, fy, fw, fh = f[s]
            for i in range(2, fw - 2, 5):
                p.px((fx + i, fy + 1), VERMILION)
                p.px((fx + i + 1, fy + 1), VERMILION)
                p.px((fx + i, fy + 2), (140, 20, 16))
                p.px((fx + i + 1, fy + 2), (140, 20, 16))
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), GOLD_DEEP)
    if style == 'crown_plaque':                                  # the plaque: 王 in gold on crimson, a gold rim
        p.shaded(f, GOLD)
        fx, fy, fw, fh = f['front']                               # 10x7
        for j in range(1, fh - 1):
            for i in range(1, fw - 1):
                p.px((fx + i, fy + j), CRIMSON)
        for i in range(2, fw - 2):                                # three bars and the upright
            for j in (1, 3, 5):
                if j != 3 or 3 <= i <= fw - 4:
                    p.lit((fx + i, fy + j), GOLD, calm=0.3, wrath=0.6)
        for j in range(1, 6):
            p.lit((fx + fw // 2, fy + j), GOLD, calm=0.3, wrath=0.6)
            p.lit((fx + fw // 2 - 1, fy + j), GOLD, calm=0.3, wrath=0.6)
    if style == 'crown_pin':
        p.shaded(f, GOLD)
    if style == 'crown_hairpin':                                 # the jade pin through cap and topknot
        p.shaded(f, JADE)
        for s in ('front', 'back', 'top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for i in list(range(3)) + list(range(fw - 3, fw)):
                for j in range(fh):
                    p.px((fx + i, fy + j), GOLD)
    if style == 'board':                                         # the mian board: black above, crimson below, gold-edged
        p.shaded(f, BLACK)
        p.face(f['bottom'], CRIMSON, 0.9, noise=4)
        fx, fy, fw, fh = f['top']
        for i in range(fw):
            for w in (0, 1):
                p.px((fx + i, fy + w), GOLD)
                p.px((fx + i, fy + fh - 1 - w), GOLD)
        for j in range(fh):
            p.px((fx, fy + j), GOLD)
            p.px((fx + fw - 1, fy + j), GOLD)
        for j in range(6, fh - 6, 6):                              # gold cloud bands across it
            for i in range(3, fw - 3):
                if (i + j) % 3:
                    p.px((fx + i, fy + j), GOLD_DEEP)
        for s in sides:
            p.face(f[s], GOLD, 1.0, noise=4)
    if style == 'beads':                                         # strings of beads: red and gold, alternately, a gap between
        for kk, r in f.items():
            fx, fy, fw, fh = r
            for j in range(fh):
                c = (196, 30, 26) if (j // 2) % 2 == 0 else GOLD
                for i in range(fw):
                    p.px((fx + i, fy + j), k(c, 1.1 if j % 2 == 0 else 0.75))

    if style == 'halo':                                          # the ring of hellfire: a black ring rimmed with flame, open in the middle
        for kk in ('right', 'left', 'top', 'bottom'):            # (its thin edges: nothing)
            fx, fy, fw, fh = f[kk]
            for j in range(fh):
                for i in range(fw):
                    p.clear((fx + i, fy + j))
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]                                 # 60x60
            cx, cy, R = fx + fw / 2, fy + fh / 2, fw / 2
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    dx, dy = x + 0.5 - cx, y + 0.5 - cy
                    d = math.hypot(dx, dy) / R
                    a = math.atan2(dy, dx)
                    lick = 0.05 * math.sin(a * 18) + 0.03 * math.sin(a * 7 + 1)
                    outer = 0.93 + lick
                    if d < 0.6 or d > outer:
                        p.clear((x, y))
                    elif d < 0.65:
                        p.px((x, y), GOLD)
                    elif d < 0.7:
                        p.px((x, y), GOLD_DEEP)
                    elif d < 0.8:
                        c = noisy(p, (36, 8, 10), 4)
                        if int((a + math.pi) * 18 / math.pi) % 2 == 0 and 0.73 < d < 0.77:
                            c = CRIMSON_DEEP
                        p.px((x, y), c)
                    else:                                         # the fire at its edge
                        t = (d - 0.8) / max(0.02, outer - 0.8)
                        c = mixc(FIRE_HOT, FIRE, min(1, t * 1.3)) if t < 0.6 else mixc(FIRE, FIRE_DEEP, (t - 0.6) / 0.4)
                        p.lit((x, y), c, calm=0.55, wrath=1.0, skin=c)
                    if 0.6 <= d < 0.65 and int((a + math.pi) * 12 / math.pi) % 3 == 0:
                        p.lit((x, y), FIRE, calm=0.35, wrath=0.9, skin=GOLD)

    if style in ('skull', 'skulljaw'):                           # the skulls set round it: bone, dark sockets that smoulder
        p.shaded(f, BONE)
        if style == 'skull':
            fx, fy, fw, fh = f['front']                           # 6x6
            for i, j in ((1, 2), (1, 3), (4, 2), (4, 3)):
                p.lit((fx + i, fy + j), FIRE_DEEP, calm=0.7, wrath=1.0, skin=(26, 10, 8), hot=FIRE)
            for i, j in ((2, 2), (3, 2)):
                p.px((fx + i, fy + j), BONE_SHADE)
            p.px((fx + 2, fy + 4), (40, 28, 24))
            p.px((fx + 3, fy + 4), (40, 28, 24))
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), BONE_SHADE)
        else:
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                if i % 2:
                    p.px((fx + i, fy), (40, 28, 24))

    if style in ('flame', 'flame_small', 'hemflame', 'hemflame_small'):                        # a tongue of hellfire: a deep red root, a hot heart, a flickering tip
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                t = 1 - j / max(1, fh - 1)                        # 0 at the root .. 1 at the tip
                half = (fw / 2) * (1 - t ** 1.6) + 0.3
                sway = 0.9 * math.sin(t * 3.2) * t
                for i in range(fw):
                    dx = i + 0.5 - fw / 2 - sway
                    if abs(dx) > half:
                        p.clear((fx + i, fy + j))
                        continue
                    core = abs(dx) < half * 0.45 and t < 0.75
                    c = mixc(FIRE_HOT, FIRE, t) if core else mixc(FIRE, FIRE_DEEP, min(1, t * 1.4))
                    p.lit((fx + i, fy + j), c, calm=0.6, wrath=1.0, skin=c)
        for s in ('right', 'left', 'top', 'bottom'):                # its thin edges: nothing (the flat tongue alone shows)
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    p.clear((fx + i, fy + j))


SKULL_BIG = ['..######..',
             '.########.',
             '##########',
             '#...##...#',
             '#...##...#',
             '##########',
             '.###..###.',
             '..######..',
             '..#.##.#..']


def skull_big(p, x, y):
    """A larger skull, 10x9 texture pixels, its sockets smouldering."""
    for j, row in enumerate(SKULL_BIG):
        for i, c in enumerate(row):
            if c == '#':
                p.px((x + i, y + j), BONE if j < 6 else BONE_SHADE)
            elif j in (3, 4) and 0 < i < 9 and i not in (4, 5):
                p.lit((x + i, y + j), FIRE_DEEP, calm=0.6, wrath=1.0, skin=(30, 8, 6), hot=FIRE)
            elif j == 6 and i in (4, 5):
                p.px((x + i, y + j), (30, 20, 18))


if __name__ == '__main__':
    p = YamaPainter(55)
    slots = paint(YAMA, p, paint_yama)
    p.save('king_yama')
    emit(YAMA, slots, JAVA, 'yama')
    print('king yama', len(YAMA), 'parts,', len(slots), 'boxes')
    from lion_camel import extent
    print('   drawn at', SIZE, {kk: round(v, 2) for kk, v in extent(YAMA, SIZE).items()})
