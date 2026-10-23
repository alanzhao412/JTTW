"""Paints the Bottomless Cave's item icons (textures/item/*.png, 16x16): Lady Earth Flow's twin
swords (straight jian, the male blade with a red tassel, the female with a pink one and a rosier
sheen), the Spirit Whisker, the Night-Blooming Petal, the Stolen Candle of Spirit Mountain and her
Embroidered Shoe.

Run from the repository root: python assets_source/cave_items.py"""
from cave_skin import Skin, shade

OUT = (78, 74, 86)


def sword(name, sheen, tassel):
    s = Skin(16, 16, seed=len(name))
    # the blade, from the top right corner down to the guard, a diagonal two pixels wide with an edge
    for t in range(2, 11):
        x, y = 15 - t, t - 1
        s.put(x + 1, y, OUT)
        s.put(x, y, shade(sheen, 1.15))
        s.put(x, y + 1, sheen)
        s.put(x - 1, y + 1, shade(sheen, 0.75))
        s.put(x - 1, y + 2, OUT)
    s.put(14, 0, OUT)
    s.put(13, 1, (250, 250, 255))
    # the guard: gold, across the blade
    for d in range(-2, 3):
        s.put(4 + d, 10 + d, (226, 178, 60) if d else (250, 214, 110))
    s.put(2, 8, OUT)
    s.put(6, 12, OUT)
    # the grip, wound dark, and a gold pommel
    for t in range(3):
        s.put(3 - t, 12 + t, (60, 30, 36) if t % 2 else (90, 44, 50))
    s.put(0, 15, (230, 182, 70))
    # the tassel hanging from the pommel
    for j in range(3):
        s.put(1, 13 + j, tassel)
        s.put(2, 14 + j, shade(tassel, 0.8))
    s.save('item', name + '.png')


sword('earth_flow_sword_male', (222, 228, 238), (200, 30, 40))
sword('earth_flow_sword_female', (240, 222, 230), (244, 140, 176))

# the whisker: a long pale curve
s = Skin(16, 16, seed=3)
pts = [(2, 13), (3, 12), (4, 11), (5, 10), (6, 9), (7, 8), (8, 7), (9, 7), (10, 6), (11, 5), (12, 5), (13, 4), (14, 4)]
for i, (x, y) in enumerate(pts):
    s.put(x, y, shade((246, 240, 228), 1 - i * 0.02))
    s.put(x, y + 1, (150, 140, 130))
s.put(1, 14, (120, 100, 90))
s.save('item', 'spirit_whisker.png')

# the petal: violet-pink, glowing at its heart, a dark vein
s = Skin(16, 16, seed=5)
for y in range(16):
    for x in range(16):
        dx, dy = (x - 7.5) / 5.5, (y - 8.5) / 7
        if dx * dx + dy * dy <= 1 and not (y < 4 and abs(x - 7.5) < 1):
            k = 1.25 - (dx * dx + dy * dy) * 0.5
            s.put(x, y, shade((176, 96, 196), k))
for y in range(5, 15):
    s.put(7, y, (110, 50, 130))
for (x, y) in ((6, 9), (8, 9), (7, 10)):
    s.put(x, y, (250, 210, 250))
s.save('item', 'night_petal.png')

# the candle: red wax, a gold holder, a pale gold flame
s = Skin(16, 16, seed=7)
s.fill(6, 5, 4, 8, (190, 30, 40), noise=6)
for y in range(5, 13):
    s.put(6, y, (230, 70, 70))
s.put(9, 6, (150, 20, 30))
s.fill(4, 13, 8, 2, (226, 178, 60), noise=6)
s.put(7, 4, (60, 40, 30))
s.put(7, 3, (255, 214, 90))
s.put(8, 2, (255, 240, 170))
s.put(7, 2, (255, 200, 80))
s.put(7, 1, (255, 250, 200))
s.save('item', 'spirit_mountain_candle.png')

# the shoe: red silk, a pointed upturned toe, flowers embroidered on it, a white sole
s = Skin(16, 16, seed=9)
for y in range(7, 13):
    for x in range(2, 15):
        top = 7 + max(0, (x - 9)) // 2 if x > 9 else 7
        if y >= top and not (x < 5 and y < 9):
            s.put(x, y, shade((184, 28, 50), 1.1 - (y - 7) * 0.04))
s.fill(2, 13, 13, 1, (244, 240, 230), noise=3)
s.put(15, 12, (184, 28, 50))
s.put(15, 11, (210, 50, 70))
s.fill(4, 7, 4, 1, (60, 20, 30), noise=2)   # the opening
for (x, y) in ((6, 10), (9, 10), (12, 11)):
    s.put(x, y, (250, 210, 90))
    s.put(x + 1, y, (240, 150, 180))
    s.put(x, y + 1, (240, 150, 180))
s.save('item', 'embroidered_shoe.png')
