"""Mount Wuzhuang's item icons (textures/item/*.png) and the hanging ginseng fruit
(textures/block/ginseng_fruit_*.png, cross models, hanging from the top): the ginseng fruit, shaped
like a newborn child (a round head, little arms and legs, a green sprig at its crown); the green
fruit, smaller and greener; the blossom, white with a pink heart; the Golden Striker, a gold rod
with a knob; the Silk-Lined Tray, cinnabar lacquer round white silk; Zhenyuan's flywhisk, a dark
handle and a long white horsetail; a crane's plume, white with a black tip; a Daoist talisman, a
yellow paper strip written over in red.

Run from the repository root: python assets_source/wuzhuang_items.py"""
from cave_skin import Skin, shade

OUT = (40, 34, 26)


def disc(s, cx, cy, rx, ry, c, k=0.25):
    for y in range(16):
        for x in range(16):
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if d <= 1:
                s.put(x, y, shade(c, 1.12 - k * d))


def baby(s, dy, c, leaf=(70, 150, 60), size=1.0):
    """The child-shaped fruit, its head's top at row dy."""
    disc(s, 8, dy + 3, 2.6 * size, 2.6 * size, c)
    disc(s, 8, dy + 8, 2.4 * size, 3.0 * size, c)
    # arms and legs
    for p in ((5, dy + 7), (11, dy + 7), (6, dy + 11), (10, dy + 11), (6, dy + 12), (10, dy + 12)):
        s.put(p[0], p[1], shade(c, 0.95))
    # a face: two dots of eyes, closed; a mouth
    s.put(7, dy + 3, shade(c, 0.6))
    s.put(9, dy + 3, shade(c, 0.6))
    s.put(8, dy + 5, (200, 120, 110))
    # the sprig at its crown
    s.put(8, dy, leaf)
    s.put(7, dy - 1, leaf)
    s.put(9, dy - 1, shade(leaf, 0.8))


RIPE = (240, 226, 184)
GREEN = (176, 210, 140)

# the item
s = Skin(16, 16, 1)
baby(s, 2, RIPE)
s.save('item', 'ginseng_fruit.png')

# hanging on the tree: a stem from the top
for name, colour, size, dy in (('ripe', RIPE, 1.0, 4), ('green', GREEN, 0.75, 5)):
    s = Skin(16, 16, 3)
    for y in range(0, dy):
        s.put(8, y, (60, 110, 50))
    baby(s, dy, colour, size=size)
    s.save('block', 'ginseng_fruit_' + name + '.png')
s = Skin(16, 16, 4)
for y in range(0, 6):
    s.put(8, y, (60, 110, 50))
for p in ((8, 6), (7, 7), (9, 7), (8, 8), (6, 7), (10, 7), (8, 5)):
    s.put(p[0], p[1], (250, 246, 240))
s.put(8, 7, (236, 140, 170))
s.put(6, 4, (70, 140, 60))
s.put(10, 3, (70, 140, 60))
s.save('block', 'ginseng_fruit_blossom.png')

# the Golden Striker: a gold rod, corner to corner, a knob at its head
s = Skin(16, 16, 5)
for i in range(3, 14):
    s.put(i, 15 - i, shade((230, 190, 70), 0.8 + (i % 3) * 0.1))
    s.put(i + 1, 15 - i, shade((200, 160, 50), 0.9))
for p in ((13, 1), (14, 1), (13, 2), (14, 2), (12, 2), (13, 3)):
    s.put(p[0], p[1], (250, 214, 90))
s.put(2, 13, (120, 70, 40))
s.put(3, 13, (120, 70, 40))
s.save('item', 'golden_striker.png')

# the Silk-Lined Tray: a round red tray seen from above at a slant, white silk within
s = Skin(16, 16, 6)
disc(s, 8, 9, 7, 4.6, (176, 34, 30), k=0.15)
disc(s, 8, 8.6, 5.2, 3.2, (246, 244, 236), k=0.2)
for x in range(3, 13):
    s.put(x, 13, (120, 22, 20))
s.save('item', 'silk_lined_tray.png')

# Zhenyuan's flywhisk: a dark handle, a long white horsetail fanning out
s = Skin(16, 16, 7)
for i in range(0, 7):
    s.put(2 + i, 14 - i, (80, 50, 28))
s.put(8, 8, (226, 184, 70))
for j in range(9):
    for w in range(-1 - j // 3, 2 + j // 3):
        x, y = 9 + j - w // 2, 7 - j + w
        if 0 <= x < 16 and 0 <= y < 16:
            s.put(x, y, shade((244, 242, 236), 0.85 + ((x + y) % 3) * 0.07))
s.save('item', 'zhenyuan_flywhisk.png')

# a crane's plume: white, a black tip, its quill
s = Skin(16, 16, 8)
for i in range(12):
    x, y = 3 + i, 14 - i
    s.put(x, y, (210, 205, 190))
    if i > 1:
        for w in (1, 2):
            c = (28, 28, 32) if i > 9 else (246, 244, 240)
            s.put(x - w, y - w + 1, shade(c, 1 - w * 0.06))
            s.put(x + w - 1, y + w, shade(c, 0.94 - w * 0.06))
s.save('item', 'crane_plume.png')

# a Daoist talisman: a yellow paper strip, red writing down it
s = Skin(16, 16, 9)
s.fill(5, 1, 6, 14, (236, 206, 90), noise=8)
for y in range(2, 14):
    if y % 3 != 1:
        s.put(7, y, (190, 30, 30))
        s.put(8, y, (190, 30, 30) if y % 2 else (236, 206, 90))
for p in ((6, 3), (9, 3), (6, 9), (9, 10), (7, 12), (8, 12)):
    s.put(p[0], p[1], (170, 26, 26))
s.save('item', 'daoist_talisman.png')
