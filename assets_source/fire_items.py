"""The Flaming Mountains' blocks and items: flame rock, cinder ash, the burning earth (a crust of dark
plates, the cracks between them glowing and pulsing; fanned out, cold), the True Samadhi Fire on the
ground (crimson flames round a white heart, unlike ordinary fire's orange, animated as vanilla's
fire is), the Ember Core and the Furnace Brick.

Run from the repository root: python assets_source/fire_items.py"""
import math
import random
from PIL import Image

T = 'src/main/resources/assets/jttw_engine/textures/'
CLEAR = (0, 0, 0, 0)
OUT = (34, 16, 12, 255)


def rgba(c, k=1.0, a=255):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (a,)


def tile(seed, base, noise=14, dark=None, spots=0, spot=None):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            n = rnd.randint(-noise, noise)
            img.putpixel((x, y), rgba([v + n for v in base]))
    if dark:
        for _ in range(3):                                  # dark cracks wandering across
            x, y = rnd.randrange(16), rnd.randrange(16)
            for _ in range(rnd.randint(4, 9)):
                img.putpixel((x % 16, y % 16), rgba(dark))
                x += rnd.choice((-1, 0, 1))
                y += rnd.choice((0, 1))
    for _ in range(spots):
        img.putpixel((rnd.randrange(16), rnd.randrange(16)), rgba(spot))
    return img


def plates(seed, n=7):
    """Cracked earth: the plates a few random points make (tiling: distances wrap), and for each pixel
    how near it is to a crack between two plates (0 on the crack)."""
    rnd = random.Random(seed)
    pts = [(rnd.uniform(0, 16), rnd.uniform(0, 16)) for _ in range(n)]
    edge = {}
    for y in range(16):
        for x in range(16):
            ds = []
            for px, py in pts:
                dx = min(abs(x + 0.5 - px), 16 - abs(x + 0.5 - px))
                dy = min(abs(y + 0.5 - py), 16 - abs(y + 0.5 - py))
                ds.append(math.hypot(dx, dy))
            ds.sort()
            edge[(x, y)] = ds[1] - ds[0]
    return edge


EDGE = plates(9, n=5)


def earth(glow_k, cold=False):
    rnd = random.Random(5)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            e = EDGE[(x, y)]
            n = rnd.randint(-8, 8)
            if e < 0.6:                      # a crack
                if cold:
                    c = (34, 30, 30)
                else:
                    c = (255, 120 + 90 * glow_k, 30 + 50 * glow_k) if e < 0.25 else (210, 64 + 40 * glow_k, 18)
            elif e < 1.3 and not cold:       # the plate's edge, scorched by it
                c = (120 + n, 36, 22)
            else:                            # the plate: dark, crusted
                k = 0.85 + 0.15 * min(1, e / 4)
                c = [v * k + n for v in ((72, 50, 46) if cold else (84, 30, 24))]
            img.putpixel((x, y), rgba(c))
    return img


# ── flame rock, cinder ash ──
tile(1, (150, 58, 40), noise=14, dark=(96, 30, 22), spots=6, spot=(186, 92, 52)).save(T + 'block/flame_rock.png')
tile(2, (74, 68, 66), noise=12, dark=(46, 42, 42), spots=5, spot=(230, 110, 40)).save(T + 'block/cinder_ash.png')

# ── burning earth: eight frames, its cracks brightening and dimming ──
frames = 8
strip = Image.new('RGBA', (16, 16 * frames))
for f in range(frames):
    k = 0.5 + 0.5 * math.sin(f * 2 * math.pi / frames)
    strip.paste(earth(k), (0, 16 * f))
strip.save(T + 'block/burning_earth.png')
open(T + 'block/burning_earth.png.mcmeta', 'w').write('{\n  "animation": {\n    "frametime": 4,\n    "interpolate": true\n  }\n}\n')
earth(0, cold=True).save(T + 'block/burning_earth_quenched.png')

# ── the True Samadhi Fire: sixteen frames of tongues of flame, crimson round a white-gold heart ──
frames = 16
strip = Image.new('RGBA', (16, 16 * frames), CLEAR)
rnd = random.Random(41)
tongues = [(1.5 + i * 2.6 + rnd.uniform(-0.6, 0.6), rnd.uniform(8, 15), rnd.uniform(0, 6.3), rnd.uniform(1.0, 1.8)) for i in range(6)]
for f in range(frames):
    t = f / frames * 2 * math.pi
    for y in range(16):
        for x in range(16):
            h = 0.0
            for cx, height, ph, w in tongues:
                top = height * (0.75 + 0.25 * math.sin(t * 2 + ph))
                sway = cx + 1.2 * math.sin(t + ph + y * 0.35)
                up = 15 - y
                if up > top:
                    continue
                width = w * 2.2 * (1 - up / max(1, top)) + 0.5
                d = abs(x - sway) / width
                if d < 1:
                    h = max(h, (1 - d) * (1 - up / max(1, top) * 0.6))
            if h > 0.1:
                if h > 0.8:
                    c = (255, 244, 224)
                elif h > 0.62:
                    c = (255, 176, 120)
                elif h > 0.4:
                    c = (236, 46, 52)
                elif h > 0.24:
                    c = (178, 16, 44)
                else:
                    c = (112, 8, 42)
                strip.putpixel((x, 16 * f + y), rgba(c))
strip.save(T + 'block/samadhi_flame.png')
open(T + 'block/samadhi_flame.png.mcmeta', 'w').write('{\n  "animation": {}\n}\n')


def icon():
    return Image.new('RGBA', (16, 16), CLEAR)


def outline(img):
    src = img.copy()
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < 16 and 0 <= y + dy < 16 and src.getpixel((x + dx, y + dy))[3] and src.getpixel((x + dx, y + dy)) != OUT:
                    img.putpixel((x, y), OUT)
                    break


# ── the Ember Core: a lump of coal, glowing from a split in it ──
img = icon()
rnd = random.Random(3)
for y in range(16):
    for x in range(16):
        dx, dy = x - 7.5, y - 8
        if (dx / 5.6) ** 2 + (dy / 4.8) ** 2 <= 1:
            light = max(0, 1 - math.hypot(dx + 2, dy + 2) / 7)
            img.putpixel((x, y), rgba((44 + 40 * light, 34 + 26 * light, 34 + 20 * light)))
for x, y in ((5, 8), (6, 8), (7, 9), (8, 9), (9, 8), (10, 7), (7, 7), (8, 10)):
    img.putpixel((x, y), rgba((255, 170 + rnd.randint(0, 60), 40)))
for x, y in ((6, 9), (9, 9), (8, 8)):
    img.putpixel((x, y), rgba((255, 240, 160)))
outline(img)
img.save(T + 'item/ember_core.png')

# ── the Furnace Brick: a fire-brick, glowing at its broken end, a trigram cut in it ──
img = icon()
for y in range(5, 12):
    for x in range(2, 14):
        k = 1.0 if y > 5 else 1.15
        c = (176, 82, 46) if x < 12 else (255, 140, 50)
        img.putpixel((x, y), rgba([v * k + rnd.randint(-8, 8) for v in c]))
for x in range(3, 7):                     # ☲ li, the trigram of fire: broken line between two whole
    img.putpixel((x, 7), rgba((90, 34, 20)))
    img.putpixel((x, 9), rgba((90, 34, 20)))
for x in (3, 6):
    img.putpixel((x, 8), rgba((90, 34, 20)))
for y in range(5, 12):
    img.putpixel((13, y), rgba((255, 220, 120)))
outline(img)
img.save(T + 'item/furnace_brick.png')
print('fire blocks and items written')
