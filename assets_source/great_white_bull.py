"""Paints the Great White Bull's skin (textures/entity/great_white_bull.png, 128x128) to the box
layout of GreatWhiteBullRenderer.layer(): each box's six faces in the standard box-UV places."""
import random
from PIL import Image

W = H = 128
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
rnd = random.Random(7)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def fill(x, y, w, h, base, k=1.0, noise=6, grad=0.0):
    """A face: the base colour at brightness k, a little mottled, darker toward its bottom by grad."""
    for j in range(h):
        for i in range(w):
            g = 1 - grad * (j / max(1, h - 1))
            n = rnd.randint(-noise, noise)
            c = shade(base, k * g)
            img.putpixel((x + i, y + j), tuple(max(0, min(255, v + n)) for v in c[:3]) + (255,))


def box(u, v, w, h, d, base, grad=0.12, noise=6, top=1.08, bottom=0.72, side=0.9, front=1.0):
    fill(u + d, v, w, d, base, top, noise)                    # top
    fill(u + d + w, v, w, d, base, bottom, noise)             # bottom
    fill(u, v + d, d, h, base, side, noise, grad)             # right
    fill(u + d, v + d, w, h, base, front, noise, grad)        # front
    fill(u + d + w, v + d, d, h, base, side, noise, grad)     # left
    fill(u + 2 * d + w, v + d, w, h, base, 0.95, noise, grad) # back


WHITE = (238, 235, 226)
IVORY = (236, 222, 180)
GOLD = (214, 168, 62)
HOOF = (58, 52, 48)
PINK = (222, 160, 158)

box(0, 0, 18, 17, 30, WHITE)              # body
box(0, 47, 14, 7, 13, WHITE, grad=0.05)   # hump
box(40, 93, 12, 12, 6, WHITE)             # neck
box(54, 47, 12, 12, 12, WHITE)            # head
box(102, 47, 8, 6, 5, (230, 214, 208), grad=0.05)  # snout
box(102, 58, 8, 3, 3, IVORY, grad=0, noise=4)      # horn, out from the brow
box(102, 64, 3, 7, 3, IVORY, grad=-0.12, noise=4)  # horn, rising
box(114, 64, 2, 5, 2, GOLD, grad=0, noise=4)       # horn point
box(0, 71, 6, 16, 6, WHITE)               # leg
box(24, 71, 2, 14, 2, WHITE, grad=0.1)    # tail
box(32, 71, 3, 4, 3, (205, 200, 192), noise=10)    # tuft

# hooves: the leg's bottom three rows, all round, and its sole
for x in range(0, 24):
    for y in range(71 + 6 + 13, 71 + 6 + 16):
        img.putpixel((x, y), shade(HOOF, 1 - 0.1 * ((y - 90) / 2)))
for x in range(12, 18):
    for y in range(71, 77):
        img.putpixel((x, y), shade(HOOF, 0.8))

# the face (head front: x 60..71, y 56..67): dark eyes with a red glint, a white blaze, heavy brows
hx, hy = 66, 59
for ex in (hx + 2, hx + 8):
    for i in range(2):
        img.putpixel((ex + i, hy + 5), (24, 20, 22, 255))
        img.putpixel((ex + i, hy + 4), shade(WHITE, 0.7))  # brow
    img.putpixel((ex + 1, hy + 5), (170, 30, 26, 255))
# the snout's front (x 101..108, y 49..54): pink, nostrils dark
sx, sy = 107, 52
for i in range(8):
    for j in range(6):
        img.putpixel((sx + i, sy + j), shade(PINK, 1 - 0.05 * j))
for nx in (sx + 1, sx + 5):
    for i in range(2):
        img.putpixel((nx + i, sy + 2), (90, 50, 52, 255))
        img.putpixel((nx + i, sy + 3), (110, 64, 66, 255))
# the snout's top, pink fading to white at the head
for i in range(8):
    for j in range(5):
        img.putpixel((102 + 5 + i, 47 + j), shade(PINK, 1.0 + 0.02 * j))

img.save('src/main/resources/assets/jttw_engine/textures/entity/great_white_bull.png')
print('ok')
