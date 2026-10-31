"""Silk-Web Ridge's blocks and items: the binding web (a thick, knotted web, heavier and whiter than a
cobweb, beads of glue on its threads), the sealing silk (a dense weave of white silk in overlapping
layers, a shimmer of pearl along its threads, see-through between them), woven silk (a bolt of it,
wound), and the items: a skein of Spider Silk, an Insect Carapace (a glossy chestnut plate with a
ridge down it), a Golden Eye (a golden iris round a slit pupil in a lidded rim of dark chitin) and the
Golden-Light Eye (the same eye set in a gilded mount, rays of light about it); their block states and
models.

Run from the repository root: python assets_source/spider_items.py"""
import json
import math
import random
from PIL import Image

T = 'src/main/resources/assets/jttw_engine/textures/'
M = 'src/main/resources/assets/jttw_engine/models/'
B = 'src/main/resources/assets/jttw_engine/blockstates/'
CLEAR = (0, 0, 0, 0)


def rgba(c, k=1.0, a=255):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (a,)


def put(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((int(x), int(y)), c)


def line(img, x0, y0, x1, y1, c, wrap=False):
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(n + 1):
        t = i / n
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        if wrap:
            put(img, int(x) % 16, int(y) % 16, c)
        else:
            put(img, round(x), round(y), c)


def binding_web():
    """A heavy web: spokes from a knot a little off the middle, rings round it, glue beads on them."""
    img = Image.new('RGBA', (16, 16), CLEAR)
    cx, cy = 7.5, 8
    silk, shade, bead = (246, 244, 236), (196, 194, 186), (255, 255, 250)
    for k in range(8):
        a = k * math.pi / 4 + 0.2
        line(img, cx, cy, cx + math.cos(a) * 9, cy + math.sin(a) * 9, rgba(silk if k % 2 == 0 else shade))
    for r in (2.5, 4.5, 6.5):
        for k in range(8):
            a0, a1 = k * math.pi / 4 + 0.2, (k + 1) * math.pi / 4 + 0.2
            sag = 0.4 * r / 6
            line(img, cx + math.cos(a0) * r, cy + math.sin(a0) * r, cx + math.cos(a1) * (r - sag), cy + math.sin(a1) * (r - sag), rgba(shade))
    for k in range(8):
        a = k * math.pi / 4 + 0.2
        for r in (2.5, 4.5, 6.5):
            put(img, round(cx + math.cos(a) * r), round(cy + math.sin(a) * r), rgba(bead))
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        put(img, int(cx) + dx, int(cy) + dy, rgba((255, 255, 255)))
    img.save(T + 'block/binding_web.png')


def silk_seal():
    """Sealing silk: a cord of white threads laid thick in every direction, pearly where the light
    takes them, shadowed in the folds; a few pinholes of nothing where the threads part."""
    rnd = random.Random(73)
    img = Image.new('RGBA', (16, 16))
    # the body: soft white, shaded in long diagonal folds so a strand of it reads as twisted cord
    for y in range(16):
        for x in range(16):
            fold = 0.5 + 0.5 * math.sin((x + y) * math.pi / 4 + 0.6)
            v = 0.92 + 0.07 * fold + rnd.uniform(-0.02, 0.02)
            img.putpixel((x, y), rgba((246, 244, 238), v))
    # threads over it, mostly along the folds, a few across
    for _ in range(30):
        x0, y0 = rnd.uniform(0, 16), rnd.uniform(0, 16)
        a = (math.pi / 4 if rnd.random() < 0.7 else -math.pi / 4) + rnd.uniform(-0.18, 0.18)
        length = rnd.uniform(4, 11)
        tone = rnd.choice(((255, 254, 250), (250, 248, 242), (222, 220, 214), (236, 232, 244)))
        line(img, x0, y0, x0 + math.cos(a) * length, y0 + math.sin(a) * length, rgba(tone), wrap=True)
    # the shimmer of pearl
    for _ in range(6):
        x, y = rnd.randrange(16), rnd.randrange(16)
        put(img, x, y, rgba((255, 250, 240)))
        put(img, (x + 1) % 16, (y + 1) % 16, rgba((246, 238, 250)))
    img.save(T + 'block/silk_seal.png')


def silk_veil():
    """The sisters' gauze: silk spun so fine it is all but clear: a milky haze, a few fine threads
    across it catching the light, a glint of dew."""
    rnd = random.Random(31)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), rgba((244, 244, 248), a=rnd.randint(58, 78)))
    for _ in range(7):
        x0, y0 = rnd.uniform(0, 16), rnd.uniform(0, 16)
        a = rnd.uniform(0, math.pi)
        length = rnd.uniform(7, 16)
        line(img, x0, y0, x0 + math.cos(a) * length, y0 + math.sin(a) * length, rgba((252, 252, 255), a=rnd.randint(110, 160)), wrap=True)
    # its edges a little thicker, as gauze gathers on the strand it hangs from
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            if rnd.random() < 0.5:
                put(img, x, y, rgba((250, 250, 252), a=60))
    for _ in range(3):
        put(img, rnd.randrange(16), rnd.randrange(16), rgba((255, 255, 255), a=200))
    img.save(T + 'block/silk_veil.png')


def woven_silk():
    """A bolt of woven silk: white strands wound round and round, shadowed between the turns."""
    rnd = random.Random(5)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            turn = (y + (x // 4) % 2) % 3
            base = (238, 236, 228) if turn else (204, 200, 192)
            n = rnd.randint(-6, 6)
            img.putpixel((x, y), rgba([v + n for v in base]))
    for _ in range(6):
        x, y = rnd.randrange(16), rnd.randrange(16)
        line(img, x, y, x + rnd.randint(2, 5), y + rnd.randint(-1, 1), rgba((252, 250, 244)), wrap=True)
    img.save(T + 'block/woven_silk.png')


def spider_silk():
    """A skein of spider silk: a twisted loop of white strands, bound at its waist."""
    img = Image.new('RGBA', (16, 16), CLEAR)
    for k in range(3):
        for i in range(60):
            t = i / 60 * 2 * math.pi
            x = 8 + 5.2 * math.cos(t) + 0.6 * k - 0.6
            y = 8 + 3.4 * math.sin(2 * t) * 0.9 + (k - 1) * 0.5
            tone = (250, 248, 242) if (i + k * 7) % 9 else (190, 188, 180)
            put(img, round(x), round(y), rgba(tone))
    for y in range(5, 11):
        put(img, 8, y, rgba((176, 92, 120)))
        put(img, 7, y, rgba((140, 64, 92)))
    put(img, 2, 8, rgba((120, 118, 112)))
    put(img, 14, 8, rgba((120, 118, 112)))
    img.save(T + 'item/spider_silk.png')


def carapace():
    """A plate of an insect son's shell: glossy chestnut, a ridge down it, its edge darker."""
    img = Image.new('RGBA', (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 6.5, (y - 8) / 7
            r = dx * dx + dy * dy
            if r > 1:
                continue
            k = 1.15 - 0.45 * r - 0.25 * (y / 16)
            c = (126, 64, 30)
            if r > 0.8:
                c = (40, 22, 14)
            if abs(x - 7.5) < 0.6:
                c = (60, 32, 16)
            if 0.12 < dx * -1 + dy * -1 < 0.35 and r < 0.6:
                c, k = (240, 200, 150), 1.0          # the gloss
            put(img, x, y, rgba(c, k))
    img.save(T + 'item/insect_carapace.png')


def eye(mounted=False):
    """A golden eye: a lidded rim of dark chitin, a golden iris round a slit pupil; mounted, in a gilded setting with rays about it."""
    img = Image.new('RGBA', (16, 16), CLEAR)
    if mounted:
        for k in range(12):
            a = k * math.pi / 6
            line(img, 8 + math.cos(a) * 5.5, 8 + math.sin(a) * 5.5, 8 + math.cos(a) * 7.5, 8 + math.sin(a) * 7.5,
                 rgba((255, 226, 120) if k % 2 == 0 else (230, 170, 60)))
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                if 4.6 <= d <= 5.6:
                    put(img, x, y, rgba((200, 150, 50) if (x + y) % 3 else (250, 214, 110)))
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 4.6, (y - 7.5) / 3.0
            r = dx * dx + dy * dy
            if r > 1:
                continue
            if r > 0.72:
                put(img, x, y, rgba((44, 26, 18)))
                continue
            k = 1.15 - 0.5 * math.sqrt(r)
            c = (250, 196, 60) if r < 0.45 else (210, 132, 30)
            if abs(x - 7.5) < 0.8 and r < 0.65:
                c, k = (20, 10, 6), 1
            put(img, x, y, rgba(c, k))
    put(img, 6, 6, rgba((255, 250, 220)))
    img.save(T + 'item/%s.png' % ('golden_light_eye' if mounted else 'golden_eye'))


def models():
    def w(path, obj):
        with open(path, 'w', newline='\n') as f:
            f.write(json.dumps(obj, indent=2) + '\n')
    w(B + 'binding_web.json', {'variants': {'fresh=false': {'model': 'jttw_engine:block/binding_web'},
                                            'fresh=true': {'model': 'jttw_engine:block/binding_web'}}})
    w(M + 'block/binding_web.json', {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout',
                                     'textures': {'cross': 'jttw_engine:block/binding_web'}})
    w(M + 'item/binding_web.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'jttw_engine:block/binding_web'}})
    w(B + 'silk_seal.json', {'variants': {'': {'model': 'jttw_engine:block/silk_seal'}}})
    w(M + 'block/silk_seal.json', {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:cutout',
                                   'textures': {'all': 'jttw_engine:block/silk_seal'}})
    w(B + 'silk_veil.json', {'variants': {'': {'model': 'jttw_engine:block/silk_veil'}}})
    w(M + 'block/silk_veil.json', {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent',
                                   'textures': {'all': 'jttw_engine:block/silk_veil'}})
    w(B + 'woven_silk.json', {'variants': {'': {'model': 'jttw_engine:block/woven_silk'}}})
    w(M + 'block/woven_silk.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'jttw_engine:block/woven_silk'}})
    w(M + 'item/woven_silk.json', {'parent': 'jttw_engine:block/woven_silk'})
    for item in ('spider_silk', 'insect_carapace', 'golden_eye', 'golden_light_eye'):
        w(M + 'item/%s.json' % item, {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'jttw_engine:item/%s' % item}})
    for egg in ('spider_demon', 'stinger_son', 'mantis_son', 'cockroach_son', 'spider_spirit', 'hundred_eyed_demon'):
        w(M + 'item/%s_spawn_egg.json' % egg, {'parent': 'minecraft:item/template_spawn_egg'})


if __name__ == '__main__':
    binding_web()
    silk_seal()
    silk_veil()
    woven_silk()
    spider_silk()
    carapace()
    eye()
    eye(mounted=True)
    models()
