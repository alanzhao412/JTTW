"""Paints Hell's item icons (textures/item/*.png, 16x16): the creatures' materials (Hell Bank Note,
Ox-Head's Horn, Horse-Face's Mane, Underworld Ink, Soul-Catching Chain), each king's relic (Shard of
the Karma Mirror, Rime Shackle, Iron Soul-Hook, Weight of the Scales, Seal of King Yama, Page of the
Book of Life and Death, Bell of the Wailing City, Ember of the Oil Cauldron, Spoke of the Crushing
Cart, Rod of Equal Measure), King Yama's Vermilion Brush and Meng Po's Soup.

Run from the repository root: python assets_source/hell_items.py"""
from cave_skin import Skin, shade

OUT = (30, 24, 28)


def save(s, name):
    s.save('item', name + '.png')


def line(s, pts, c):
    for p in pts:
        s.put(p[0], p[1], c)


def disc(s, cx, cy, r, c, k=0.3):
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r:
                s.put(x, y, shade(c, 1.15 - k * d / r))


# the Hell Bank Note: yellow paper, a red border, a portrait in a box, a stamp
s = Skin(16, 16, 1)
s.fill(1, 4, 14, 8, (232, 196, 90), noise=6)
for x in range(1, 15):
    s.put(x, 4, (170, 30, 30))
    s.put(x, 11, (170, 30, 30))
for y in range(4, 12):
    s.put(1, y, (170, 30, 30))
    s.put(14, y, (170, 30, 30))
s.fill(6, 6, 4, 4, (110, 70, 40), noise=4)
s.put(3, 6, (60, 40, 30))
s.put(3, 8, (60, 40, 30))
s.put(12, 9, (200, 40, 40))
save(s, 'hell_bank_note')

# Ox-Head's horn: a curved horn, pale, darker at the root
s = Skin(16, 16, 2)
pts = [(3, 13), (4, 12), (5, 11), (6, 10), (7, 9), (8, 8), (9, 7), (10, 6), (11, 5), (12, 4), (12, 3), (13, 2)]
for i, (x, y) in enumerate(pts):
    w = 3 if i < 4 else 2 if i < 8 else 1
    for d in range(w):
        s.put(x + d, y, shade((226, 214, 180), 0.7 + i * 0.03))
        s.put(x, y + d, shade((226, 214, 180), 0.65 + i * 0.03))
save(s, 'ox_horn')

# Horse-Face's mane: a dark tuft, bound with red
s = Skin(16, 16, 3)
for x in range(4, 12):
    for y in range(2 + abs(x - 8) // 2, 14):
        if (x + y) % 3:
            s.put(x, y, shade((48, 34, 26), 0.8 + ((x * 5 + y) % 4) * 0.1))
s.fill(5, 5, 6, 1, (180, 30, 30), noise=3)
save(s, 'horse_mane')

# Underworld ink: a black ink stick, a gold character
s = Skin(16, 16, 4)
for i in range(10):
    for w in range(3):
        s.put(3 + i + w, 12 - i, (26, 24, 30))
line(s, [(7, 9), (8, 8), (9, 7)], (220, 180, 70))
s.put(13, 2, (60, 60, 70))
save(s, 'underworld_ink')

# the Soul-Catching Chain: iron links, a diagonal
s = Skin(16, 16, 5)
for k in range(5):
    cx, cy = 2 + k * 3, 13 - k * 3
    for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1)):
        pass
    line(s, [(cx, cy), (cx + 1, cy - 1), (cx + 2, cy), (cx + 1, cy + 1)], (150, 150, 160) if k % 2 else (100, 100, 112))
save(s, 'soul_chain')

# the Shard of the Karma Mirror: a triangle of pale glass, a gold edge
s = Skin(16, 16, 6)
for y in range(3, 14):
    for x in range(3, 3 + (y - 2)):
        if x < 14:
            s.put(x, y, (190, 230, 240) if (x + y) % 5 else (250, 255, 255))
line(s, [(3, y) for y in range(3, 14)], (220, 180, 70))
line(s, [(x, 13) for x in range(3, 14)], (220, 180, 70))
save(s, 'karma_mirror_shard')

# the Rime Shackle: an iron cuff, frosted
s = Skin(16, 16, 7)
for y in range(16):
    for x in range(16):
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
        if 3.5 <= d <= 6:
            s.put(x, y, (170, 210, 236) if (x + y) % 3 else (240, 250, 255))
save(s, 'rime_shackle')

# the Iron Soul-Hook: a grey hook on a ring
s = Skin(16, 16, 8)
line(s, [(9, 1), (9, 2), (9, 3), (9, 4), (9, 5), (9, 6), (9, 7), (9, 8), (9, 9), (8, 10), (7, 11), (6, 11), (5, 10), (4, 9), (4, 8)],
     (120, 120, 132))
line(s, [(10, 2), (10, 3), (10, 4), (10, 5), (10, 6), (10, 7), (10, 8), (10, 9), (9, 10), (8, 11)], (80, 80, 92))
line(s, [(3, 7), (4, 6)], (200, 200, 210))
line(s, [(8, 0), (10, 0), (11, 1), (7, 1)], (90, 90, 100))
save(s, 'iron_soul_hook')

# the Weight of the Scales: a dark trapezoid weight, a ring on top
s = Skin(16, 16, 9)
for y in range(6, 14):
    half = 2 + (y - 6) // 2
    for x in range(8 - half, 8 + half):
        s.put(x, y, shade((70, 66, 74), 1.2 - (y - 6) * 0.05))
line(s, [(6, 3), (7, 2), (8, 2), (9, 3), (9, 4), (6, 4), (6, 5), (9, 5)], (200, 170, 70))
save(s, 'scales_weight')

# the Seal of King Yama: a red-gold block, a crouching beast handle
s = Skin(16, 16, 10)
s.fill(3, 8, 10, 6, (184, 34, 34), noise=6)
s.fill(3, 13, 10, 1, (230, 190, 80), noise=4)
s.fill(5, 4, 6, 4, (222, 178, 64), noise=6)
s.put(5, 3, (222, 178, 64))
s.put(10, 3, (222, 178, 64))
s.put(6, 5, (40, 30, 20))
save(s, 'yama_seal')

# a Page of the Book of Life and Death: yellowed, columns of black writing, a red stroke through a name
s = Skin(16, 16, 11)
s.fill(3, 1, 10, 14, (226, 210, 168), noise=6)
for x in (5, 7, 9, 11):
    for y in range(3, 13):
        if (x * 3 + y) % 4:
            s.put(x, y, (40, 34, 30))
line(s, [(8, 4), (8, 5), (8, 6), (8, 7), (8, 8)], (200, 30, 30))
save(s, 'book_page')

# the Bell of the Wailing City: bronze
s = Skin(16, 16, 12)
for y in range(3, 13):
    half = 2 + (y - 3) * 4 // 10
    for x in range(8 - half, 8 + half):
        s.put(x, y, shade((176, 120, 50), 1.25 - abs(x - 7.5) * 0.08))
s.fill(4, 13, 8, 1, (130, 86, 40), noise=4)
line(s, [(7, 1), (8, 1), (7, 2), (8, 2)], (110, 80, 40))
s.put(7, 14, (90, 60, 30))
save(s, 'wailing_bell')

# the Ember of the Oil Cauldron: glowing coal
s = Skin(16, 16, 13)
disc(s, 7.5, 8.5, 5, (255, 140, 40), 0.6)
for (x, y) in ((6, 7), (9, 9), (7, 10), (8, 6)):
    s.put(x, y, (255, 240, 160))
for (x, y) in ((4, 10), (11, 7), (5, 6)):
    s.put(x, y, (120, 30, 10))
save(s, 'oil_cauldron_ember')

# the Spoke of the Crushing Cart: dark wood, iron-capped
s = Skin(16, 16, 14)
for i in range(11):
    s.put(3 + i, 13 - i, (110, 72, 40))
    s.put(4 + i, 13 - i, (84, 54, 30))
line(s, [(2, 14), (3, 14), (2, 13), (13, 2), (14, 2), (14, 1)], (120, 120, 130))
save(s, 'cart_wheel_spoke')

# the Rod of Equal Measure: a gold balance-rod, a pan hung at each end
s = Skin(16, 16, 15)
line(s, [(x, 5) for x in range(1, 15)], (230, 190, 80))
line(s, [(7, 3), (8, 3), (7, 4), (8, 4)], (200, 150, 60))
for px in (2, 13):
    line(s, [(px, 6), (px, 7), (px, 8)], (150, 150, 160))
    line(s, [(px - 2, 9), (px - 1, 10), (px, 10), (px + 1, 10), (px + 2, 9)], (200, 160, 70))
save(s, 'equal_measure_rod')

# King Yama's Vermilion Brush: a red lacquered shaft, a gold ferrule, a vermilion-wet tip
s = Skin(16, 16, 16)
for i in range(10):
    s.put(5 + i, 10 - i, (170, 24, 30))
    s.put(6 + i, 10 - i, (120, 16, 20))
line(s, [(4, 11), (5, 11), (4, 10)], (226, 180, 70))
line(s, [(3, 12), (2, 13), (3, 13), (2, 12), (1, 14)], (30, 24, 24))
s.put(1, 15, (230, 40, 30))
s.put(2, 14, (230, 40, 30))
save(s, 'yama_brush')

# Meng Po's Soup: a bowl, a grey-green broth, steam
s = Skin(16, 16, 17)
for y in range(8, 14):
    half = 6 - (y - 8)
    for x in range(8 - half, 8 + half):
        s.put(x, y, shade((150, 104, 64), 1.1 - (y - 8) * 0.06))
s.fill(2, 7, 12, 1, (150, 170, 140), noise=6)
s.fill(3, 8, 10, 1, (130, 150, 120), noise=6)
for (x, y) in ((6, 4), (7, 3), (9, 5), (10, 4), (8, 2)):
    s.put(x, y, (220, 226, 230, 160))
save(s, 'meng_po_soup')
