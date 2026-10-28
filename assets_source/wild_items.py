"""The open world's yaoguai's items, 16x16: the Wolf Pelt (a wolfman's grey hide, spread out, darker
down its spine, ragged at its edges) and the Ogre Tusk (a great curved tusk of yellowed ivory, dark
at its root).

Run from the repository root: python assets_source/wild_items.py"""
import math
import random
from PIL import Image

T = 'src/main/resources/assets/jttw_engine/textures/item/'
OUT = (30, 24, 22, 255)


def rgba(c, k=1.0):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def outline(img):
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


def pelt():
    rnd = random.Random(5)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(1, 15):
        # a hide spread out: broad at the shoulders and haunches, the legs' stubs at its corners
        half = 5.2 + 1.2 * math.cos((y - 7.5) / 7.5 * math.pi) * -0.5
        if y in (2, 3, 12, 13):
            half += 1.5
        for x in range(16):
            dx = x - 7.5
            if abs(dx) > half + rnd.uniform(-0.6, 0.4):
                continue
            t = abs(dx) / max(1, half)                        # 0 down its spine .. 1 at its edge
            c = [60 + 120 * t, 58 + 118 * t, 64 + 110 * t]
            if (x + y * 3) % 5 == 0:
                c = [v * 0.82 for v in c]
            img.putpixel((x, y), rgba(c, rnd.uniform(0.92, 1.06)))
    for x in (6, 7, 8, 9):                                     # its head's end, the ears
        img.putpixel((x, 0), rgba((70, 66, 70)))
    return outline(img)


def tusk():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    pts = []
    for i in range(40):                                       # a curve from its root (lower left) up and over to its point
        t = i / 39
        a = math.pi * (0.95 - 0.6 * t)
        r = 8.0
        x, y = 10.5 + r * math.cos(a) * 0.95, 14.2 - r * math.sin(a) * (0.75 + 0.5 * t)
        pts.append((x, y, t))
    for x, y, t in pts:
        width = 2.4 * (1 - t) + 0.6
        for dx in range(-3, 4):
            for dy in range(-3, 4):
                if math.hypot(dx, dy) <= width:
                    X, Y = int(round(x + dx)), int(round(y + dy))
                    if 0 <= X < 16 and 0 <= Y < 16:
                        root = max(0.0, 0.35 - t) / 0.35
                        c = [236 - 90 * root, 226 - 98 * root, 196 - 100 * root]
                        if dy < 0:
                            c = [v * 1.06 for v in c]
                        if dy > 0 and math.hypot(dx, dy) > width - 1:
                            c = [v * 0.78 for v in c]
                        img.putpixel((X, Y), rgba(c))
    for x, y, t in pts[3:30:5]:                               # growth rings across it
        X, Y = int(round(x)), int(round(y))
        if 0 <= X < 16 and 0 <= Y < 16:
            img.putpixel((X, Y), rgba((170, 154, 118)))
    return outline(img)


if __name__ == '__main__':
    pelt().save(T + 'wolf_pelt.png')
    tusk().save(T + 'ogre_tusk.png')
    print('wolf pelt, ogre tusk')
