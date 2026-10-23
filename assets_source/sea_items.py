"""The Mystical Waters' items and blocks: the Water-Repelling Pearl, the Dragon Kings' regalia (the
cap, the mail, the boots: their icons and the armour layer they are worn from) and the pillar of the
Eastern treasury that becomes the Ruyi Jingu Bang.

Run from the repository root: python assets_source/sea_items.py"""
import math
import random
from PIL import Image

T = 'src/main/resources/assets/jttw_engine/textures/'
rnd = random.Random(47)
CLEAR = (0, 0, 0, 0)
OUT = (28, 20, 24, 255)


def rgba(c, k=1.0):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def icon():
    return Image.new('RGBA', (16, 16), CLEAR)


def outline(img):
    """A dark rim round the icon's shape, as vanilla's items have."""
    src = img.copy()
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < 16 and 0 <= y + dy < 16 and src.getpixel((x + dx, y + dy))[3] and src.getpixel((x + dx, y + dy)) != OUT:
                    img.putpixel((x, y), OUT)
                    break


# ── the pearl: a sphere of pale blue light, a bright highlight, a ring of the sea about it ──
img = icon()
for y in range(16):
    for x in range(16):
        dx, dy = x - 7.5, y - 7.5
        d = math.hypot(dx, dy)
        if d < 5.6:
            light = max(0, 1 - math.hypot(dx + 2, dy + 2) / 8)
            img.putpixel((x, y), rgba((150 + 100 * light, 200 + 55 * light, 245)))
        elif d < 6.8 and (x + y) % 2 == 0:
            img.putpixel((x, y), rgba((60, 150, 200)))
for p in ((5, 5), (6, 5), (5, 6)):
    img.putpixel(p, (255, 255, 255, 255))
outline(img)
img.save(T + 'item/water_repelling_pearl.png')

GOLD = (226, 178, 62)
GOLD_D = (160, 112, 36)
PURPLE = (120, 50, 150)

# ── the cap: purple-gold, a phoenix wing raised either side ──
img = icon()
for y in range(8, 14):
    for x in range(3, 13):
        img.putpixel((x, y), rgba(PURPLE if y < 11 else GOLD, 1.0 - 0.04 * (y - 8)))
for x in range(3, 13):
    img.putpixel((x, 13), rgba(GOLD_D))
for x in (6, 9):
    img.putpixel((x, 9), rgba((240, 230, 255)))
for side in (-1, 1):                        # the wings, feathers fanning up and out
    for i in range(6):
        x0 = 7.5 + side * (3 + i * 0.8)
        for j in range(3 + i // 2):
            x, y = int(round(x0)), 8 - j - i // 2
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), rgba(GOLD if j < 2 else (240, 120, 60)))
img.putpixel((7, 6), rgba((240, 60, 60)))
img.putpixel((8, 6), rgba((240, 60, 60)))
outline(img)
img.save(T + 'item/phoenix_plume_cap.png')

# ── the mail: a gold cuirass of fine links ──
img = icon()
shape = ["....######....",
         "..##########..",
         ".############.",
         ".############.",
         "..##########..",
         "..##########..",
         "..##########..",
         "..##########..",
         "...########...",
         "...########...",
         "....######...."]
for j, row in enumerate(shape):
    for i, ch in enumerate(row):
        if ch == '#':
            x, y = i + 1, j + 3
            link = (x + y) % 2 == 0
            img.putpixel((x, y), rgba(GOLD if link else GOLD_D, 1.08 if i < 7 else 0.95))
for x in range(5, 11):
    img.putpixel((x, 3), CLEAR)          # the neck
    img.putpixel((x, 4), rgba(GOLD_D, 0.8) if x in (5, 10) else CLEAR)
for x in range(4, 12):
    img.putpixel((x, 9), rgba((200, 60, 50)))   # a red sash
outline(img)
img.save(T + 'item/golden_chain_mail.png')

# ── the boots: pale lotus fibre, a cloud's curl at the toe ──
img = icon()
LOTUS = (238, 222, 226)
for side, x0 in ((0, 2), (1, 9)):
    for y in range(4, 13):
        for x in range(x0, x0 + 3):
            img.putpixel((x, y), rgba(LOTUS, 0.95 if x == x0 else 1.05))
    for x in range(x0 - 1 + side, x0 + 5 - (1 - side)):
        img.putpixel((x, 12), rgba(LOTUS, 0.85))
        img.putpixel((x, 13), rgba((120, 160, 220)))       # a cloud-blue sole
    img.putpixel((x0 + 1, 6), rgba((236, 150, 180)))       # a lotus petal on each
    img.putpixel((x0 + 1, 7), rgba((236, 150, 180)))
outline(img)
img.save(T + 'item/cloud_stepping_boots.png')

# ── the armour layer: cap on the head, mail on the body and arms, boots on the legs ──
img = Image.new('RGBA', (64, 32), CLEAR)


def fill(x, y, w, h, colour, chain=False):
    for j in range(h):
        for i in range(w):
            c = colour
            if chain:
                c = GOLD if (i + j) % 2 == 0 else GOLD_D
            img.putpixel((x + i, y + j), rgba(c, 1.0 + rnd.uniform(-0.05, 0.05)))


def box(u, v, w, h, d, colour, chain=False):
    fill(u + d, v, w, d, colour, chain)
    fill(u + d + w, v, w, d, colour, chain)
    fill(u, v + d, d, h, colour, chain)
    fill(u + d, v + d, w, h, colour, chain)
    fill(u + d + w, v + d, d, h, colour, chain)
    fill(u + 2 * d + w, v + d, w, h, colour, chain)


box(0, 0, 8, 8, 8, PURPLE)
for x in range(0, 32):                      # a gold band round the cap's rim, and gold wings
    for y in (13, 14, 15):
        if img.getpixel((x, y))[3]:
            img.putpixel((x, y), rgba(GOLD))
for y in range(8, 13):                      # the wings, on its sides
    for x in (2, 3, 4, 5):
        if (x + y) % 2 == 0:
            img.putpixel((x, y), rgba(GOLD))
            img.putpixel((x + 16, y), rgba(GOLD))
for x in (11, 12):                          # a pearl on its brow
    img.putpixel((x, 12), rgba((240, 236, 255)))
box(16, 16, 8, 12, 4, GOLD, chain=True)
for x in range(16, 40):                     # a red sash about the waist
    if img.getpixel((x, 28))[3]:
        img.putpixel((x, 28), rgba((200, 60, 50)))
box(40, 16, 4, 12, 4, GOLD, chain=True)
box(0, 16, 4, 12, 4, LOTUS)
for x in range(0, 16):                      # cloud-blue soles and cuffs
    for y in (19, 30, 31):
        if img.getpixel((x, y))[3]:
            img.putpixel((x, y), rgba((120, 160, 220)))
import os
os.makedirs(T + 'models/armor', exist_ok=True)
img.save(T + 'models/armor/dragon_regalia_layer_1.png')

# ── the pillar: dark iron, banded with gold, a glowing inscription down it ──
IRON = (52, 50, 58)
side = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        c = rgba(IRON, 0.9 + 0.2 * math.sin(x * 0.7) * 0.5 + rnd.uniform(-0.04, 0.04))
        if y in (0, 1, 14, 15):
            c = rgba(GOLD, 1.05 if y in (0, 15) else 0.85)
        side.putpixel((x, y), c)
for y in range(3, 13):                      # the inscription, a column of glowing marks
    if y % 3 != 2:
        for x in (7, 8):
            side.putpixel((x, y), rgba((255, 214, 110)))
side.save(T + 'block/sea_calming_pillar.png')
top = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        d = math.hypot(x - 7.5, y - 7.5)
        top.putpixel((x, y), rgba(GOLD) if 5.5 < d < 7.2 else rgba(IRON, 1.1 - d / 20))
top.save(T + 'block/sea_calming_pillar_top.png')
print('sea items written')
