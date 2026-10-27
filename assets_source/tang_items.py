"""The Tang villages' item icons (textures/item/*.png, 16x16) and the talismans' effect icons
(textures/mob_effect/*.png, 18x18):
- the money: copper cash, a round coin with a square hole (铜钱); a string of cash on a red cord; a
  tael of silver, the boat-shaped ingot (元宝);
- the food and drink: a sack of rice, a blue-and-white bowl heaped with rice, a pleated steamed bun
  (包子), a plain mantou, a sealed jug of rice wine, a celadon bowl of tea;
- the medicine: a clay bowl of dark herbal decoction, three Qi-gathering pills, a jade vial of dew;
- a rumour scroll, rolled on its rods;
- the supernatural world's goods: the demon-quelling talisman (yellow paper, red script, a seal),
  the ghost-warding talisman (yellow, black script, an eye of the eight trigrams), the house-warding
  talisman (red paper, gold script), a bundle of sandalwood incense, smouldering, and a peace charm
  in a red pouch with a gold cord.

Run from the repository root: python assets_source/tang_items.py"""
import math

from cave_skin import Skin, shade

OUT = (34, 28, 22)


def disc(s, cx, cy, rx, ry, c, k=0.25, light=(-0.5, -0.6)):
    for y in range(s.img.height):
        for x in range(s.img.width):
            dx, dy = (x - cx) / rx, (y - cy) / ry
            d = dx * dx + dy * dy
            if d <= 1:
                lit = 1.1 - k * d - 0.12 * (dx * light[0] + dy * light[1])
                s.put(x, y, shade(c, lit))


def outline(s, colour=OUT):
    """A dark rim round every filled pixel that borders an empty one."""
    img = s.img
    w, h = img.size
    filled = [[img.getpixel((x, y))[3] > 0 for x in range(w)] for y in range(h)]
    for y in range(h):
        for x in range(w):
            if filled[y][x]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and filled[ny][nx]:
                    s.put(x, y, colour + (255,))
                    break


def coin(s, cx, cy, r, base=(176, 120, 64), patina=(96, 140, 112)):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= r:
                lit = 1.12 - 0.3 * (x - cx + y - cy) / (2 * r)
                c = base if s.rnd.random() > 0.18 else patina
                if d > r - 1:
                    c = shade(base, 0.75)
                s.put(x, y, shade(c, lit))
    h = max(1, int(r * 0.35))
    for y in range(int(cy) - h, int(cy) + h + 1):
        for x in range(int(cx) - h, int(cx) + h + 1):
            s.img.putpixel((x, y), (0, 0, 0, 0))
    for x in range(int(cx) - h - 1, int(cx) + h + 2):
        s.put(x, int(cy) - h - 1, shade(base, 0.7))


# ── Money ────────────────────────────────────────────────────────────────────
s = Skin(16, 16, 1)
coin(s, 7.5, 7.5, 6.5)
for (x, y) in ((7, 3), (11, 7), (7, 11), (3, 7)):  # the four characters, worn
    s.put(x, y, (120, 74, 40))
    s.put(x + 1, y, (120, 74, 40))
outline(s)
s.save('item', 'copper_cash.png')

s = Skin(16, 16, 2)
# the cord, hanging in a curve, a tassel at its end; coins threaded on it, seen edge-on and tilted
for x in range(1, 15):
    y = int(round(3 + 0.06 * (x - 8) ** 2 * -1 + 6))
    s.put(x, y, (200, 40, 40))
for k, cx in enumerate((3, 6, 9, 12)):
    cy = 3 + 0.06 * -(cx - 8) ** 2 + 6
    for y in range(16):
        for x in range(16):
            d = ((x - cx) / 1.7) ** 2 + ((y - cy) / 3.2) ** 2
            if d <= 1:
                s.put(x, y, shade((182, 126, 66), 1.12 - 0.35 * d - 0.15 * (x - cx)))
    s.put(cx, int(round(cy)), (200, 40, 40))
for y in range(9, 15):
    s.put(14, y, (200, 40, 40))
s.put(13, 14, (220, 60, 50)); s.put(15, 14, (220, 60, 50))
outline(s)
s.save('item', 'string_of_cash.png')

s = Skin(16, 16, 3)
SILVER = (214, 220, 228)
# the boat: a low hull curving up at both ends, a dome rising from its middle
for x in range(1, 15):
    t = abs(x - 7.5) / 7
    top = int(10 - 4 * t * t)
    for y in range(top, 13):
        s.put(x, y, shade(SILVER, 1.1 - 0.35 * (y - top) / 4 - 0.1 * (x / 15)))
disc(s, 7.5, 7.5, 3.4, 3.2, SILVER, k=0.4)
for x in range(3, 13):
    s.put(x, 12, shade(SILVER, 0.6))
s.put(6, 6, (255, 255, 255))
s.put(7, 5, (255, 255, 255))
s.put(2, 9, (250, 250, 255))
outline(s, (60, 64, 76))
s.save('item', 'silver_tael.png')

# ── Food and drink ───────────────────────────────────────────────────────────
s = Skin(16, 16, 4)
SACK = (196, 170, 126)
for y in range(4, 15):
    w = 5 + (y - 4) * 0.35 if y < 12 else 8
    for x in range(int(8 - w), int(8 + w)):
        s.put(x, y, shade(SACK, 1.05 - 0.2 * (x - 3) / 10 + (0.06 if (x + y) % 3 == 0 else 0)))
for x in range(4, 12):
    s.put(x, 4, shade(SACK, 0.7))
for (x, y) in ((6, 3), (7, 2), (8, 3), (9, 2), (7, 3), (8, 2), (10, 3), (5, 3)):
    s.put(x, y, (246, 244, 236))
s.put(8, 1, (250, 250, 246))
for (x, y) in ((8, 6), (8, 7), (8, 8), (8, 9), (6, 7), (10, 7), (7, 6), (9, 8), (7, 8), (9, 6)):
    s.put(x, y, (70, 50, 34))   # 米, stamped on it
outline(s)
s.save('item', 'rice.png')

s = Skin(16, 16, 5)
BLUEWHITE = (236, 240, 246)
for x in range(2, 14):
    for y in range(8, 14):
        dx = abs(x - 7.5) / 6
        if y - 8 > 5 * (1 - (1 - dx * dx) ** 0.5) * 0 and dx <= 1 - (y - 8) * 0.07:
            s.put(x, y, shade(BLUEWHITE, 1.05 - 0.2 * (y - 8) / 5))
for x in range(3, 13):
    if x % 3 == 0:
        s.put(x, 10, (60, 90, 170))
        s.put(x, 11, (60, 90, 170))
for x in range(4, 12):
    s.put(x, 9, (70, 100, 180))
s.put(6, 14, shade(BLUEWHITE, 0.8)); s.put(7, 14, shade(BLUEWHITE, 0.8)); s.put(8, 14, shade(BLUEWHITE, 0.8)); s.put(9, 14, shade(BLUEWHITE, 0.8))
disc(s, 7.5, 7.6, 5.5, 3.2, (248, 246, 238), k=0.3)
for _ in range(10):
    x, y = s.rnd.randint(4, 11), s.rnd.randint(5, 8)
    s.put(x, y, (255, 255, 252))
outline(s)
s.save('item', 'rice_bowl.png')

s = Skin(16, 16, 6)
disc(s, 7.5, 9, 6.2, 4.8, (244, 238, 222), k=0.35)
# the pleats twisted to a knot at its crown
for k in range(8):
    a = k * math.pi / 4
    for r in range(1, 4):
        x, y = 7.5 + math.cos(a) * r * 1.3, 6.2 + math.sin(a) * r * 0.9
        s.put(int(round(x)), int(round(y)), (214, 200, 176))
s.put(7, 5, (226, 210, 186)); s.put(8, 5, (226, 210, 186))
for x in range(3, 13):
    s.put(x, 13, (206, 190, 166))
outline(s, (90, 76, 60))
s.save('item', 'steamed_bun.png')

s = Skin(16, 16, 7)
disc(s, 7.5, 9.5, 6, 4.5, (250, 248, 240), k=0.3)
disc(s, 7.5, 8.5, 4.5, 3, (252, 250, 246), k=0.2)
for x in range(3, 13):
    s.put(x, 13, (220, 214, 200))
outline(s, (110, 100, 86))
s.save('item', 'mantou.png')

s = Skin(16, 16, 8)
CLAY = (150, 88, 52)
for y in range(4, 15):
    t = (y - 9) / 6
    w = 5.4 * (1 - t * t * 0.6) if y > 5 else 2.2
    for x in range(int(8 - w), int(8 + w) + 1):
        s.put(x, y, shade(CLAY, 1.1 - 0.35 * (x - 8 + w) / (2 * w + 1)))
for y in range(1, 4):
    for x in range(6, 10):
        s.put(x, y, (200, 40, 40) if y > 1 else (170, 30, 30))   # the red cloth sealing its mouth
s.put(5, 3, (200, 40, 40)); s.put(10, 3, (200, 40, 40))
for y in range(8, 12):
    for x in range(6, 10):
        s.put(x, y, (226, 60, 50))   # the paper label, 酒
s.put(7, 9, (30, 20, 20)); s.put(8, 10, (30, 20, 20)); s.put(7, 10, (30, 20, 20))
outline(s)
s.save('item', 'rice_wine.png')

s = Skin(16, 16, 9)
CELADON = (150, 196, 170)
for x in range(2, 14):
    for y in range(8, 14):
        dx = abs(x - 7.5) / 6
        if dx <= 1 - (y - 8) * 0.08:
            s.put(x, y, shade(CELADON, 1.05 - 0.25 * (y - 8) / 5))
for x in range(6, 10):
    s.put(x, 14, shade(CELADON, 0.7))
disc(s, 7.5, 8, 5.6, 1.6, (150, 120, 50), k=0.2)
for (x, y) in ((6, 5), (7, 4), (9, 3), (8, 2), (10, 5)):
    s.put(x, y, (230, 230, 230, 150))   # steam
outline(s)
s.save('item', 'tea.png')

# ── Medicine ─────────────────────────────────────────────────────────────────
s = Skin(16, 16, 10)
for x in range(2, 14):
    for y in range(8, 14):
        dx = abs(x - 7.5) / 6
        if dx <= 1 - (y - 8) * 0.08:
            s.put(x, y, shade((130, 84, 54), 1.05 - 0.25 * (y - 8) / 5))
disc(s, 7.5, 8, 5.6, 1.6, (54, 30, 18), k=0.1)
s.put(5, 8, (110, 70, 40)); s.put(9, 7, (90, 120, 50))   # a leaf afloat
for (x, y) in ((6, 5), (8, 4), (7, 2), (9, 3)):
    s.put(x, y, (200, 200, 190, 140))
outline(s)
s.save('item', 'herbal_decoction.png')

s = Skin(16, 16, 11)
for cx, cy in ((5, 10), (10.5, 9.5), (7.5, 5.5)):
    disc(s, cx, cy, 2.6, 2.6, (176, 96, 46), k=0.5)
    s.put(int(cx) - 1, int(cy) - 1, (250, 220, 150))
outline(s)
s.save('item', 'qi_gathering_pill.png')

s = Skin(16, 16, 12)
JADE = (110, 200, 150)
disc(s, 7.5, 10, 4.5, 4.5, JADE, k=0.4)
for y in range(3, 7):
    for x in range(6, 10):
        s.put(x, y, shade(JADE, 1.0 - 0.1 * (x - 6)))
for x in range(5, 11):
    s.put(x, 2, (228, 186, 80))
s.put(6, 9, (220, 255, 236)); s.put(5, 10, (220, 255, 236))
outline(s, (30, 70, 50))
s.save('item', 'jade_dew.png')

# ── A rumour scroll ──────────────────────────────────────────────────────────
s = Skin(16, 16, 13)
PAPER = (236, 224, 190)
# half unrolled: the paper with its columns of writing, a roll at either end
for y in range(3, 13):
    for x in range(3, 13):
        s.put(x, y, shade(PAPER, 1.02 - 0.06 * ((x + y) % 4 == 0)))
for x in range(4, 12, 2):
    for y in range(4, 12):
        if (y * 7 + x) % 5 != 0:
            s.put(x, y, (52, 42, 36))
for y in range(2, 14):
    for x in (1, 2, 13, 14):
        s.put(x, y, shade((120, 74, 42), 1.1 if x in (1, 13) else 0.85))
for x in (1, 2, 13, 14):
    s.put(x, 1, (70, 40, 26)); s.put(x, 14, (70, 40, 26))
s.put(11, 11, (190, 30, 30)); s.put(10, 11, (190, 30, 30))
outline(s)
s.save('item', 'rumour_scroll.png')


# ── The supernatural world's goods ───────────────────────────────────────────
def talisman(name, paper, ink, seal, eye=False, seed=20):
    s = Skin(16, 16, seed)
    for y in range(1, 15):
        for x in range(4, 12):
            s.put(x, y, shade(paper, 1.04 - 0.08 * (y / 14) + (0.05 if (x + y * 3) % 7 == 0 else 0)))
    # the script: a column of strokes down the middle, a flourish at the foot
    for y in range(3, 12):
        if y % 4 != 2:
            s.put(7, y, ink)
            s.put(8, y, ink if y % 2 else paper)
    for (x, y) in ((6, 4), (9, 5), (6, 8), (9, 9), (5, 12), (6, 13), (9, 12), (10, 13), (7, 12), (8, 13)):
        s.put(x, y, ink)
    # the seal at its head
    for y in range(2, 4):
        for x in range(6, 10):
            s.put(x, y, seal)
    if eye:
        for (x, y) in ((6, 7), (7, 6), (8, 6), (9, 7), (8, 8), (7, 8)):
            s.put(x, y, (24, 24, 30))
        s.put(7, 7, seal); s.put(8, 7, seal)
    outline(s, (60, 44, 26))
    s.save('item', name + '.png')


talisman('demon_quelling_talisman', (236, 200, 80), (190, 26, 26), (200, 30, 30), seed=21)
talisman('ghost_warding_talisman', (232, 214, 120), (30, 28, 34), (190, 26, 26), eye=True, seed=22)
talisman('house_ward_talisman', (196, 40, 40), (240, 200, 90), (250, 220, 120), seed=23)

s = Skin(16, 16, 24)
for k, x0 in enumerate((5, 7, 9, 11)):
    for i in range(10):
        x, y = x0 - i // 3, 14 - i
        s.put(x, y, (150, 60, 40) if i < 7 else (120, 70, 46))
    s.put(x0 - 3, 4, (255, 170, 60))
    s.put(x0 - 3, 3, (200, 200, 200, 160))
for x in range(3, 12):
    s.put(x, 12, (200, 40, 40))   # the paper band round the bundle
for (x, y) in ((3, 2), (5, 1), (8, 2), (7, 0)):
    s.put(x, y, (210, 210, 210, 120))
outline(s)
s.save('item', 'incense.png')

s = Skin(16, 16, 25)
RED = (196, 32, 36)
for y in range(4, 15):
    w = (y - 3) * 0.6
    for x in range(int(8 - w), int(8 + w) + 1):
        if 0 <= x < 16:
            s.put(x, y, shade(RED, 1.1 - 0.3 * (y - 4) / 10))
for x in range(4, 12):
    s.put(x, 9, (228, 186, 80))
for (x, y) in ((7, 11), (8, 11), (7, 12), (8, 12)):
    s.put(x, y, (240, 200, 90))   # 平安, in gold
for y in range(1, 4):
    s.put(8, y, (228, 186, 80))
s.put(7, 1, (228, 186, 80)); s.put(9, 1, (228, 186, 80))
outline(s, (70, 20, 20))
s.save('item', 'peace_charm.png')


# ── Effect icons (18x18) ─────────────────────────────────────────────────────
def effect(name, draw, seed):
    s = Skin(18, 18, seed)
    draw(s)
    outline(s, (30, 24, 20))
    s.save('mob_effect', name + '.png')


def quelling(s):
    for y in range(2, 16):
        for x in range(6, 12):
            s.put(x, y, (236, 200, 80))
    for y in range(4, 14):
        if y % 3:
            s.put(8, y, (190, 26, 26)); s.put(9, y, (190, 26, 26))
    for i in range(4):
        s.put(3 + i, 3 + i, (255, 150, 40)); s.put(14 - i, 3 + i, (255, 150, 40))


def ward(s):
    for y in range(18):
        for x in range(18):
            d = math.hypot(x - 8.5, y - 8.5)
            if 4.5 < d <= 7.5 and int(math.degrees(math.atan2(y - 8.5, x - 8.5)) // 45) % 2 == 0:
                s.put(x, y, (200, 50, 40))
            elif d <= 3.5:
                s.put(x, y, (236, 232, 220) if x > 8.5 else (30, 30, 36))


def calm(s):
    for i in range(14):
        x = 8 + round(2.5 * math.sin(i * 0.7))
        s.put(x, 15 - i, (200, 196, 190))
        s.put(x + 1, 15 - i, (170, 166, 160))
    for y in range(13, 17):
        for x in range(6, 12):
            s.put(x, y, (150, 90, 50))


effect('demon_quelling', quelling, 31)
effect('ghost_ward', ward, 32)
effect('incense_calm', calm, 33)
