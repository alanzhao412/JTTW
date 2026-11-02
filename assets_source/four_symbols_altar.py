"""The Altar of the Four Symbols (四象坛): a stepped altar of dark polished stone banded with carved
clouds, a jade table inlaid with the eight trigrams round a taiji, the Four Symbols' pillars at its
corners (the Azure Dragon's east, the Vermilion Bird's south, the White Tiger's west, the Black
Tortoise's north), each crowned in its colour, and on the table a bronze ding, two-eared and
three-legged, with incense burning in it. Its model and textures.

Run from the repository root: python assets_source/four_symbols_altar.py"""
import json
import math
import random
from PIL import Image

T = 'src/main/resources/assets/jttw_engine/textures/block/'
M = 'src/main/resources/assets/jttw_engine/models/block/'
rnd = random.Random(44)


def rgba(c, k=1.0):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def noise(k=0.06):
    return 1 + rnd.uniform(-k, k)


# ── the stone: polished dark stone, a carved band of clouds along its middle ──
STONE, STONE_D, STONE_L = (62, 64, 74), (38, 38, 46), (96, 98, 110)
CLOUD = ['++++++++',
         '..##....',
         '.#..#...',
         '#.##.#..',
         '#.+#.#.#',
         '.#..#.#.',
         '..##..#.',
         '++++++++']
img = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        c = STONE
        if y in (0, 15):
            c = STONE_L if y == 0 else STONE_D                     # the bevels
        elif 4 <= y <= 11:                                         # the cloud band: ruyi curls, two to a face
            row = CLOUD[y - 4][x % 8]
            c = {'#': STONE_L, '+': STONE_D, '.': STONE}[row]
        img.putpixel((x, y), rgba(c, noise()))
img.save(T + 'altar_stone.png')

# ── the top of the steps: plain polished stone with a fine border ──
img = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        edge = x in (0, 15) or y in (0, 15)
        inner = x in (2, 13) or y in (2, 13)
        c = STONE_L if edge else (STONE_D if inner and 2 <= x <= 13 and 2 <= y <= 13 else STONE)
        img.putpixel((x, y), rgba(c, noise(0.05)))
img.save(T + 'altar_stone_top.png')

# ── the jade table: green jade, the eight trigrams round a taiji ──
JADE, JADE_D, JADE_L = (70, 160, 115), (40, 105, 75), (150, 215, 175)
img = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        dx, dy = x - 7.5, y - 7.5
        d = math.hypot(dx, dy)
        c = JADE
        if x in (0, 15) or y in (0, 15):
            c = JADE_D
        elif d < 3.1:                                              # the taiji
            white = dx > 0
            if math.hypot(dx, dy + 1.55) < 1.55:
                white = False
            if math.hypot(dx, dy - 1.55) < 1.55:
                white = True
            if math.hypot(dx, dy + 1.55) < 0.6:
                white = True
            if math.hypot(dx, dy - 1.55) < 0.6:
                white = False
            c = (235, 235, 225) if white else (30, 34, 34)
        elif 4.2 < d < 6.6:                                        # the trigrams: three bars each, broken or whole
            sector = int(((math.atan2(dy, dx) + math.pi) / (2 * math.pi)) * 8) % 8
            bar = int((d - 4.2) / 0.8)
            ang_in = ((math.atan2(dy, dx) + math.pi) / (2 * math.pi) * 8) % 1
            broken = (sector >> bar) & 1 and 0.4 < ang_in < 0.6
            if 0.15 < ang_in < 0.85 and not broken and (d - 4.2) % 0.8 < 0.55:
                c = (225, 190, 80)
        img.putpixel((x, y), rgba(c, noise(0.05)))
img.save(T + 'altar_jade.png')

# ── the bronze: the ding's body, old bronze gone green in its hollows, a taotie band ──
BRONZE, BRONZE_D, VERD = (150, 105, 50), (90, 60, 30), (80, 140, 115)
img = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        c = BRONZE
        if y < 2:
            c = (190, 145, 70)                                     # the lip, worn bright
        elif 5 <= y <= 9:                                          # the taotie band: eyes and curls
            pat = [".#..##..##..#...", "##.#..#.#..#.##.", ".#.#.##.##.#.#..", "##.#..#.#..#.##.", ".#..##..##..#..."][y - 5]
            c = BRONZE_D if pat[x] == '#' else BRONZE
            if (x, y) in ((5, 7), (10, 7)):
                c = (220, 60, 40)                                  # the eyes, inlaid
        elif y >= 14:
            c = BRONZE_D
        if c == BRONZE_D and rnd.random() < 0.35:
            c = VERD                                               # verdigris in the hollows
        img.putpixel((x, y), rgba(c, noise(0.07)))
img.save(T + 'altar_bronze.png')

# ── the symbols: four colours in quarters, for the pillars' crowns; a pillar's stone below ──
img = Image.new('RGBA', (16, 16))
quarters = {(0, 0): ((60, 120, 220), (30, 60, 140)),     # the Azure Dragon
            (1, 0): ((220, 50, 40), (130, 20, 20)),       # the Vermilion Bird
            (0, 1): ((240, 240, 235), (170, 170, 175)),   # the White Tiger
            (1, 1): ((40, 40, 50), (15, 15, 22))}         # the Black Tortoise
for (qx, qy), (c, d) in quarters.items():
    for y in range(8):
        for x in range(8):
            k = 1.15 if (x < 2 or y < 2) else (0.85 if (x > 5 or y > 5) else 1.0)
            col = d if (x + y) % 5 == 0 else c
            if (x, y) in ((3, 3), (4, 4)):
                col = (235, 200, 90)                              # a gold boss
            img.putpixel((qx * 8 + x, qy * 8 + y), rgba(col, k * noise(0.04)))
img.save(T + 'altar_symbols.png')

# ── the incense: red sticks, glowing tips ──
img = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        c = (150, 40, 30) if y > 2 else ((255, 170, 60) if y == 2 else (120, 110, 100))
        img.putpixel((x, y), rgba(c, noise(0.08)))
img.save(T + 'altar_incense.png')


# ── the model ──
def face(tex, uv=None, cull=None):
    f = {'texture': '#' + tex}
    if uv:
        f['uv'] = uv
    return f


def box(frm, to, tex, top=None, bottom=None, uv=None, shade=True):
    faces = {}
    for side in ('north', 'south', 'east', 'west'):
        faces[side] = face(tex, uv)
    faces['up'] = face(top or tex, uv)
    faces['down'] = face(bottom or tex, uv)
    e = {'from': frm, 'to': to, 'faces': faces}
    if not shade:
        e['shade'] = False
    return e


elements = [
    # the steps
    box([0, 0, 0], [16, 3, 16], 'stone', top='stone_top'),
    box([1.5, 3, 1.5], [14.5, 5, 14.5], 'stone', top='stone_top'),
    # the jade table, on a waisted neck
    box([4, 5, 4], [12, 6.5, 12], 'stone', top='stone_top'),
    box([3, 6.5, 3], [13, 8, 13], 'jade', top='jade'),
    # the ding: three legs, its belly, its lip and its two upright ears
    box([5.25, 8, 5.25], [6.25, 9, 6.25], 'bronze', uv=[0, 14, 2, 16]),
    box([9.75, 8, 5.25], [10.75, 9, 6.25], 'bronze', uv=[0, 14, 2, 16]),
    box([7.5, 8, 9.75], [8.5, 9, 10.75], 'bronze', uv=[0, 14, 2, 16]),
    box([5, 9, 5], [11, 11.5, 11], 'bronze', top='bronze', uv=[0, 3, 16, 12]),
    box([4.5, 11.5, 4.5], [11.5, 12.25, 11.5], 'bronze', uv=[0, 0, 16, 2]),
    box([6.5, 12.25, 4.5], [7.25, 13.75, 5.25], 'bronze', uv=[0, 0, 2, 2]),
    box([8.75, 12.25, 4.5], [9.5, 13.75, 5.25], 'bronze', uv=[0, 0, 2, 2]),
    box([6.5, 13.25, 4.5], [9.5, 13.75, 5.25], 'bronze', uv=[0, 0, 4, 2]),
    box([6.5, 12.25, 10.75], [7.25, 13.75, 11.5], 'bronze', uv=[0, 0, 2, 2]),
    box([8.75, 12.25, 10.75], [9.5, 13.75, 11.5], 'bronze', uv=[0, 0, 2, 2]),
    box([6.5, 13.25, 10.75], [9.5, 13.75, 11.5], 'bronze', uv=[0, 0, 4, 2]),
    # the incense, three sticks
    box([7.6, 12, 7.6], [7.9, 15.5, 7.9], 'incense', uv=[0, 2, 1, 16]),
    box([6.6, 12, 8.3], [6.9, 15, 8.6], 'incense', uv=[0, 2, 1, 16]),
    box([8.9, 12, 7.0], [9.2, 14.6, 7.3], 'incense', uv=[0, 2, 1, 16]),
]
# the Four Symbols' pillars at the corners: azure east (+x, -z), vermilion south, white west, black north
corners = [((0.5, 0.5), [8, 0, 16, 8]), ((13.5, 0.5), [0, 0, 8, 8]), ((13.5, 13.5), [8, 8, 16, 16]), ((0.5, 13.5), [0, 8, 8, 16])]
for (x, z), quv in corners:
    elements.append(box([x, 3, z], [x + 2, 9, z + 2], 'stone', top='stone_top', uv=[6, 0, 8, 16]))
    elements.append(box([x - 0.25, 9, z - 0.25], [x + 2.25, 10, z + 2.25], 'symbols', uv=quv))
    elements.append(box([x + 0.5, 10, z + 0.5], [x + 1.5, 10.75, z + 1.5], 'symbols', uv=[quv[0] + 3, quv[1] + 3, quv[0] + 5, quv[1] + 5]))

model = {
    'parent': 'minecraft:block/block',
    'ambientocclusion': False,
    'textures': {'particle': 'jttw_engine:block/altar_stone', 'stone': 'jttw_engine:block/altar_stone',
                 'stone_top': 'jttw_engine:block/altar_stone_top', 'jade': 'jttw_engine:block/altar_jade',
                 'bronze': 'jttw_engine:block/altar_bronze', 'symbols': 'jttw_engine:block/altar_symbols',
                 'incense': 'jttw_engine:block/altar_incense'},
    'elements': elements,
}
with open(M + 'four_symbols_altar.json', 'w', encoding='utf-8', newline='\n') as f:
    json.dump(model, f, indent=2)
    f.write('\n')
