"""The Brocade Kasaya (锦襕袈裟): its item icon (textures/item/brocade_kasaya.png, a robe folded: red
patchwork framed in gold, a jade ring at its clasp) and its worn layer
(textures/models/armor/brocade_kasaya_layer_1.png, the patchwork over the body and arms).

Run from the repository root: python assets_source/kasaya_items.py"""
import random
from PIL import Image

GOLD, GOLD_DEEP, GOLD_LIGHT = (228, 178, 62), (142, 96, 30), (255, 228, 140)
SILK, SILK_DEEP = (176, 28, 30), (104, 14, 20)
JADE = (96, 182, 132)
R = random.Random(7)


def silk(x, y, cell):
    if x % cell == 0 or y % cell == 0:
        return GOLD if (x + y) % 2 else GOLD_DEEP
    t = R.random() * 0.25
    return tuple(int(a + (b - a) * t) for a, b in zip(SILK, SILK_DEEP))


icon = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
# a robe folded square, a fold across it, a collar, the ring
for y in range(4, 29):
    for x in range(3, 29):
        c = silk(x - 3, y - 4, 5)
        if x in (3, 28) or y in (4, 28):
            c = GOLD_LIGHT if (x + y) % 2 else GOLD
        if y == 15:
            c = GOLD_DEEP
        icon.putpixel((x, y), c + (255,))
for y in range(2, 8):
    for x in range(11, 21):
        if abs(x - 15.5) > (y - 1) * 0.9:
            continue
        icon.putpixel((x, y), (GOLD if y < 7 else GOLD_DEEP) + (255,))
for dx, dy in ((0, -2), (1, -2), (2, -1), (2, 0), (2, 1), (1, 2), (0, 2), (-1, 1), (-1, 0), (-1, -1)):
    icon.putpixel((22 + dx, 10 + dy), JADE + (255,))
icon.putpixel((22, 9), (210, 255, 230, 255))
icon.save('src/main/resources/assets/jttw_engine/textures/item/brocade_kasaya.png')

layer = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
for y in range(16, 32):
    for x in range(16, 56):
        c = silk(x, y, 4)
        if y in (16, 31) or x in (16, 40, 55):
            c = GOLD_LIGHT
        layer.putpixel((x, y), c + (255,))
layer.putpixel((27, 22), JADE + (255,))
layer.putpixel((26, 22), JADE + (255,))
layer.save('src/main/resources/assets/jttw_engine/textures/models/armor/brocade_kasaya_layer_1.png')
