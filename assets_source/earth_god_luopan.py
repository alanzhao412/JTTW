"""The Earth God's Luopan: a geomancer's compass of wood and bronze, its rings lacquered red and gold,
its needle red-tipped. Sixteen frames, the needle turned a sixteenth of the way round each, and the item
models that pick one by the compass's "angle" (0: straight ahead; growing clockwise).

Run from the repository root: python assets_source/earth_god_luopan.py"""
import json
import math
import os
from PIL import Image

T = 'src/main/resources/assets/jttw_engine/textures/item/'
M = 'src/main/resources/assets/jttw_engine/models/item/'
NAME = 'earth_god_luopan'
FRAMES = 16
CLEAR = (0, 0, 0, 0)
OUT = (40, 24, 14, 255)
WOOD = (122, 76, 38, 255)
WOOD_D = (92, 54, 26, 255)
LACQUER = (168, 40, 32, 255)
GOLD = (228, 184, 72, 255)
FACE = (222, 196, 132, 255)
FACE_D = (186, 156, 96, 255)
INK = (60, 40, 24, 255)
TIP = (214, 36, 30, 255)
TAIL = (70, 76, 88, 255)
C = 7.5


def disc():
    img = Image.new('RGBA', (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - C, y - C)
            if d > 7.4:
                continue
            if d > 6.6:
                c = OUT
            elif d > 5.6:
                c = WOOD if (x + y) % 3 else WOOD_D
            elif d > 4.9:
                c = LACQUER
            elif d > 4.3:
                c = GOLD
            else:
                c = FACE if (x * 7 + y * 3) % 5 else FACE_D
            img.putpixel((x, y), c)
    # the eight trigrams' ticks on the gold ring
    for k in range(8):
        a = k * math.pi / 4
        img.putpixel((round(C + math.cos(a) * 5.3), round(C + math.sin(a) * 5.3)), INK)
    return img


def needle(img, k):
    """The needle, turned k sixteenths clockwise from straight up: the red tip ahead, the dark tail behind."""
    a = k * 2 * math.pi / FRAMES
    ux, uy = math.sin(a), -math.cos(a)
    for i in range(0, 17):
        t = i / 4
        px, py = C + ux * t, C + uy * t
        img.putpixel((int(round(px)), int(round(py))), TIP)
    for i in range(1, 11):
        t = i / 4
        px, py = C - ux * t, C - uy * t
        img.putpixel((int(round(px)), int(round(py))), TAIL)
    img.putpixel((int(round(C)), int(round(C))), GOLD)


os.makedirs(T, exist_ok=True)
for k in range(FRAMES):
    img = disc()
    needle(img, k)
    img.save(T + '%s_%02d.png' % (NAME, k))
    if k:
        with open(M + '%s_%02d.json' % (NAME, k), 'w', encoding='utf-8') as f:
            json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'jttw_engine:item/%s_%02d' % (NAME, k)}}, f, indent=2)
            f.write('\n')

# the base model is frame 0 (straight ahead); each other frame from half a step before it, frame 0 again past the last
overrides = [{'predicate': {'angle': (k - 0.5) / FRAMES}, 'model': 'jttw_engine:item/%s_%02d' % (NAME, k)} for k in range(1, FRAMES)]
overrides.append({'predicate': {'angle': (FRAMES - 0.5) / FRAMES}, 'model': 'jttw_engine:item/%s' % NAME})
with open(M + NAME + '.json', 'w', encoding='utf-8') as f:
    json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'jttw_engine:item/%s_00' % NAME},
               'overrides': overrides}, f, indent=2)
    f.write('\n')
print('wrote', FRAMES, 'frames')
