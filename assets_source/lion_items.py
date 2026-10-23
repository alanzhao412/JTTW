"""Lion-Camel Ridge's items, 16x16: the Mane Tuft, the Ivory Shard, the Storm Feather, the Patrol
Token (令, the Little Wind-Driller's), the Azure Lion's Mane, the White Tusk, the Azure Lion's
Broadsword and the White Elephant's Lance.

Run from the repository root: python assets_source/lion_items.py"""
import math
import random
from PIL import Image

T = 'src/main/resources/assets/jttw_engine/textures/item/'
OUT = (30, 22, 20, 255)


def rgba(c, k=1.0, a=255):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (a,)


def new():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def outline(img):
    """A dark rim round whatever is drawn."""
    src = img.copy()
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < 16 and 0 <= y + dy < 16 and src.getpixel((x + dx, y + dy))[3]:
                    img.putpixel((x, y), OUT)
                    break
    return img


def line(img, pts, colour_at):
    """A stroke through points, coloured by how far along it is (0..1)."""
    n = len(pts)
    for i, (x, y) in enumerate(pts):
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), rgba(colour_at(i / max(1, n - 1))))


def diagonal(x0, y0, x1, y1):
    n = max(abs(x1 - x0), abs(y1 - y0))
    return [(round(x0 + (x1 - x0) * t / n), round(y0 + (y1 - y0) * t / n)) for t in range(n + 1)]


def tuft(base, tip, seed):
    """A hank of mane: strands fanning up from a bound root."""
    rnd = random.Random(seed)
    img = new()
    for s in range(7):
        x = 5 + s
        top = 2 + rnd.randint(0, 3)
        sway = rnd.uniform(-0.25, 0.25)
        for y in range(top, 12):
            xx = round(x + (12 - y) * sway)
            t = (y - top) / max(1, 12 - top)
            c = [a + (b - a) * (1 - t) for a, b in zip(base, tip)]
            img.putpixel((max(0, min(15, xx)), y), rgba(c, 0.85 + 0.15 * rnd.random()))
    for x in range(6, 11):                       # the cord binding it
        img.putpixel((x, 12), rgba((170, 40, 36)))
        img.putpixel((x, 13), rgba((120, 28, 26)))
    for x in range(7, 10):
        img.putpixel((x, 14), rgba(base, 0.7))
    return outline(img)


def tusk(base, cap, length=11, curve=0.35):
    img = new()
    for i in range(length):
        t = i / (length - 1)
        x = round(3 + i)
        y = round(13 - i + curve * (i - length / 2) ** 2 / 3)
        w = 2 if t < 0.6 else 1
        for d in range(w):
            c = cap if t > 0.85 and cap else base
            img.putpixel((min(15, x), max(0, min(15, y - d))), rgba(c, 1.05 - 0.2 * t if d else 0.9))
    return outline(img)


def feather():
    img = new()
    shaft = diagonal(3, 13, 12, 2)
    for i, (x, y) in enumerate(shaft):
        t = i / (len(shaft) - 1)
        w = round(3 * math.sin(math.pi * min(1, t * 1.1)))
        for d in range(1, w + 1):
            for sx, sy in ((x - d, y - d + 1), (x + d - 1, y + d)):
                if 0 <= sx < 16 and 0 <= sy < 16:
                    edge = d == w
                    img.putpixel((sx, sy), rgba((214, 170, 70) if edge else (52, 48, 66), 1 - 0.2 * t))
        img.putpixel((x, y), rgba((210, 190, 140)))
    return outline(img)


def token():
    img = new()
    for y in range(2, 14):
        for x in range(4, 12):
            r = (x in (4, 11)) or y in (2, 13)
            img.putpixel((x, y), rgba((176, 124, 40) if r else (226, 178, 66)))
    for x, y in [(7, 0), (8, 0), (7, 1), (8, 1)]:     # its cord loop
        img.putpixel((x, y), rgba((170, 40, 36)))
    # 令, roughly: a roof, a bar, a hook
    for x, y in [(8, 4), (7, 5), (9, 5), (6, 6), (10, 6), (7, 8), (8, 8), (9, 8), (8, 9), (8, 10), (9, 11)]:
        img.putpixel((x, y), rgba((110, 50, 20)))
    return outline(img)


def weapon(blade_from, blade_to, blade_c, edge_c, hilt_c, guard_c, wide=2, pommel=None):
    img = new()
    # the hilt, lower left
    for x, y in diagonal(1, 14, blade_from[0] - 1, blade_from[1] + 1):
        img.putpixel((x, y), rgba(hilt_c))
    gx, gy = blade_from
    for d in (-2, -1, 0, 1, 2):
        x, y = gx - 1 + d, gy + 1 + d
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), rgba(guard_c))
    pts = diagonal(*blade_from, *blade_to)
    for i, (x, y) in enumerate(pts):
        t = i / max(1, len(pts) - 1)
        for d in range(wide if t < 0.85 else 1):
            if 0 <= x + d < 16 and 0 <= y + d < 16:
                img.putpixel((x + d, y + d), rgba(edge_c if d == wide - 1 else blade_c, 1.05 - 0.15 * t))
        img.putpixel((x, y), rgba(blade_c, 1.1 - 0.15 * t))
    if pommel:
        img.putpixel((1, 14), rgba(pommel))
        img.putpixel((0, 15), rgba(pommel))
    return outline(img)


def lance():
    img = new()
    for x, y in diagonal(0, 15, 10, 5):
        img.putpixel((x, y), rgba((130, 34, 32)))
        if (x + y) % 4 == 0:
            img.putpixel((x, y), rgba((226, 178, 66)))
    for x, y in [(9, 5), (10, 6), (9, 7), (8, 6)]:     # a red tassel at the head
        img.putpixel((x, y), rgba((210, 40, 40)))
    for x, y in diagonal(11, 4, 15, 0):
        img.putpixel((x, y), rgba((236, 240, 246)))
        if x - 1 >= 0:
            img.putpixel((x - 1, y), rgba((176, 182, 196)))
        if y + 1 < 16:
            img.putpixel((x, y + 1), rgba((150, 156, 170)))
    return outline(img)


if __name__ == '__main__':
    tuft((120, 66, 26), (210, 160, 80), 1).save(T + 'mane_tuft.png')
    tuft((26, 58, 110), (120, 176, 230), 2).save(T + 'azure_lion_mane.png')
    img = tusk((236, 228, 204), None, length=8, curve=0.2)
    img.save(T + 'ivory_shard.png')
    tusk((244, 238, 222), (226, 178, 66), length=12, curve=0.35).save(T + 'white_tusk.png')
    feather().save(T + 'storm_feather.png')
    token().save(T + 'patrol_token.png')
    weapon((5, 10), (14, 1), (214, 222, 232), (110, 150, 210), (60, 30, 30), (226, 178, 66), wide=3, pommel=(226, 178, 66)).save(T + 'azure_lion_sabre.png')
    lance().save(T + 'white_elephant_lance.png')
    print('items done')
