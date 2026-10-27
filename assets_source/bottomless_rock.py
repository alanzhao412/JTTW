"""The Bottomless Cave's own blocks, which no one digs, blasts or phases through: its rock at three
depths (the burrows' packed earth, threaded with roots; the middle floors' banded stone, a faint
rose-gold vein in it; the deep floors' dark slate, veined pink and gold, as if the mouse spirit's
wealth ran through the mountain's roots), and its great door: leaves of dark red lacquer, banded in
black iron and studded in gold, in a frame of black stone edged with gold.

Each 16x16, tiling without a seam. Run from the repository root: python assets_source/bottomless_rock.py"""
import math
import random
from PIL import Image

T = 'src/main/resources/assets/jttw_engine/textures/block/'
SIZE = 16


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def wrap_noise(rng, cells=4):
    """A smooth value noise that wraps every 16 pixels: a grid of cells x cells random values, eased between."""
    g = [[rng.random() for _ in range(cells)] for _ in range(cells)]

    def at(x, y):
        fx, fy = x * cells / SIZE, y * cells / SIZE
        x0, y0 = int(fx) % cells, int(fy) % cells
        x1, y1 = (x0 + 1) % cells, (y0 + 1) % cells
        tx, ty = fx - int(fx), fy - int(fy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = g[x0][y0] + (g[x1][y0] - g[x0][y0]) * tx
        b = g[x0][y1] + (g[x1][y1] - g[x0][y1]) * tx
        return a + (b - a) * ty
    return at


def vein(points, width):
    """Distance to a wrapped polyline of points, as a function of (x, y)."""
    def d(x, y):
        best = 99.0
        for (ax, ay), (bx, by) in zip(points, points[1:]):
            for ox in (-SIZE, 0, SIZE):
                for oy in (-SIZE, 0, SIZE):
                    px, py = x - ox, y - oy
                    vx, vy = bx - ax, by - ay
                    t = max(0.0, min(1.0, ((px - ax) * vx + (py - ay) * vy) / (vx * vx + vy * vy + 1e-9)))
                    best = min(best, math.hypot(px - (ax + vx * t), py - (ay + vy * t)))
        return best / width
    return d


def rock(name, dark, light, seed, vein_colour=None, veins=(), roots=(), pebbles=0, bands=0.0):
    rng = random.Random(seed)
    n1, n2 = wrap_noise(rng, 4), wrap_noise(rng, 8)
    img = Image.new('RGB', (SIZE, SIZE))
    vs = [vein(v, w) for v, w in veins]
    rs = [vein(v, w) for v, w in roots]
    for y in range(SIZE):
        for x in range(SIZE):
            t = 0.6 * n1(x, y) + 0.4 * n2(x, y)
            if bands:
                t = 0.7 * t + 0.3 * (0.5 + 0.5 * math.sin(2 * math.pi * (y + 2 * n1(x, y)) / SIZE * 2))
            c = lerp(dark, light, t)
            for r in rs:
                if r(x, y) < 1:
                    c = lerp(c, (52, 34, 22), 0.75)
            for v in vs:
                d = v(x, y)
                if d < 1:
                    c = lerp(c, vein_colour, 0.85 * (1 - d) + 0.15)
            if pebbles and rng.random() < pebbles:
                c = lerp(c, (196, 182, 160), 0.6)
            img.putpixel((x, y), c)
    img.save(T + name + '.png')


def gate():
    """Lacquered planks, banded and studded: the leaves of the great door."""
    rng = random.Random(7)
    n = wrap_noise(rng, 8)
    img = Image.new('RGB', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            plank = x // 4
            grain = 0.5 + 0.5 * math.sin((y + plank * 5) * 0.9 + 2 * n(x, y))
            c = lerp((74, 14, 14), (128, 28, 22), 0.35 + 0.4 * grain)
            if x % 4 == 0:
                c = lerp(c, (30, 6, 6), 0.7)  # the seam between planks
            if y in (3, 12):
                c = (28, 26, 30)  # an iron band
                if x % 4 == 2:
                    c = (226, 178, 58)  # a gold stud on it
            elif y in (2, 4, 11, 13) and x % 4 == 2:
                c = lerp(c, (40, 34, 30), 0.5)
            img.putpixel((x, y), c)
    img.save(T + 'bottomless_gate.png')


def frame():
    """Black stone, carved: a band of cloud-scroll in dull gold running across it, so a wall of it reads as one carving."""
    rng = random.Random(11)
    n = wrap_noise(rng, 4)
    n2 = wrap_noise(rng, 8)
    img = Image.new('RGB', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            c = lerp((20, 18, 22), (50, 44, 52), 0.25 + 0.45 * n(x, y) + 0.2 * n2(x, y))
            # the scroll: a wave across the block, wrapping, and its curl
            wave = 7.5 + 2.6 * math.sin(2 * math.pi * x / SIZE)
            d = abs(y - wave)
            if d < 0.7:
                c = lerp((150, 108, 34), (226, 180, 70), n2(x, y))
            elif d < 1.4:
                c = lerp(c, (12, 10, 12), 0.6)
            if y in (0, 15):
                c = lerp(c, (14, 12, 14), 0.5)
            img.putpixel((x, y), c)
    img.save(T + 'bottomless_gate_frame.png')


if __name__ == '__main__':
    rock('bottomless_earth', (58, 40, 28), (104, 76, 52), 1,
         roots=[([(0, 3), (5, 6), (9, 5), (16, 3)], 0.6), ([(3, 16), (4, 11), (7, 9)], 0.5)], pebbles=0.03)
    rock('bottomless_stone', (70, 64, 62), (124, 114, 106), 2, vein_colour=(196, 128, 116),
         veins=[([(0, 10), (6, 8), (11, 12), (16, 10)], 0.55)], bands=1.0)
    rock('bottomless_deep', (26, 24, 30), (62, 56, 66), 3, vein_colour=(214, 150, 120),
         veins=[([(0, 5), (4, 7), (8, 4), (13, 9), (16, 5)], 0.5), ([(9, 16), (10, 12), (14, 11)], 0.45)])
    gate()
    frame()
