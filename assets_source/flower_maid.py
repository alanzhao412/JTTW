"""Paints the Flower Maid's skins (textures/entity/flower_maid.png and flower_maid_captain.png, 64x64)
to the layout of CaveRenderers.maidLayer(), a person's frame: a pale face with dark eyes and a
rosebud mouth, black hair up in a bun with a flower in it, a gown of pink petals with a green sash,
its skirt falling to the ground in scalloped layers that deepen toward the hem. The captain's gown
is violet, its flowers gold.

Run from the repository root: python assets_source/flower_maid.py"""
from cave_skin import Skin, faces, shade, mix

SKIN = (240, 218, 200)
HAIR = (28, 22, 32)


def petal_pattern(s, x, y, w, h, gown, deep, flower, seed):
    """A gown's face: layers of scalloped petals, deepening downward, a few small flowers on it."""
    for j in range(h):
        t = j / max(1, h - 1)
        for i in range(w):
            c = mix(gown, deep, t * 0.8)
            # scallops every 3 rows: the lower edge of each layer darker
            if (j + (i // 2) % 2) % 3 == 2:
                c = shade(c, 0.82)
            n = s.rnd.randint(-5, 5)
            s.put(x + i, y + j, tuple(v + n for v in c))
    for k in range(max(1, w * h // 40)):
        fx, fy = x + s.rnd.randrange(w), y + s.rnd.randrange(h)
        s.put(fx, fy, flower)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if x <= fx + dx < x + w and y <= fy + dy < y + h:
                s.put(fx + dx, fy + dy, shade(flower, 0.9))
        s.put(fx, fy, (246, 210, 90))


def paint(captain):
    gown = (168, 112, 204) if captain else (238, 156, 186)
    deep = (110, 60, 150) if captain else (196, 78, 120)
    flower = (240, 200, 80) if captain else (252, 236, 244)
    sash = (210, 170, 70) if captain else (92, 150, 82)
    s = Skin(64, 64, seed=23 if captain else 21)

    # head: hair all round and on top; the face in front
    s.box(0, 0, 8, 8, 8, HAIR, noise=5)
    x, y, w, h = faces(0, 0, 8, 8, 8)['front']
    s.fill(x, y + 2, 8, 6, SKIN, noise=3)
    for i in range(8):
        s.put(x + i, y + 2, HAIR if i in (0, 1, 6, 7) else SKIN)   # hair framing the brow
    for i in (0, 7):
        for j in range(2, 6):
            s.put(x + i, y + j, HAIR)                               # and falling by the cheeks
    for ex in (2, 5):
        s.put(x + ex, y + 4, (30, 20, 30))
        s.put(x + ex, y + 3, shade(HAIR, 1.2))                      # brows
    s.put(x + 1, y + 5, (240, 160, 170))
    s.put(x + 6, y + 5, (240, 160, 170))                            # blush
    s.put(x + 3, y + 6, (200, 60, 90))
    s.put(x + 4, y + 6, (200, 60, 90))                              # a rosebud mouth
    # hat layer: a few strands loose at the back
    hx, hy, hw, hh = faces(32, 0, 8, 8, 8)['back']
    for i in range(hw):
        for j in range(hh):
            if (i + j) % 5 == 0:
                s.put(hx + i, hy + j, shade(HAIR, 1.15))

    # body: the gown's bodice, a sash at the waist, a collar crossed at the front
    s.box(16, 16, 8, 12, 4, gown, noise=5)
    for side in ('right', 'front', 'left', 'back'):
        x, y, w, h = faces(16, 16, 8, 12, 4)[side]
        petal_pattern(s, x, y, w, h, gown, gown, flower, 1)
        s.fill(x, y + 8, w, 2, sash, noise=4)
    x, y, w, h = faces(16, 16, 8, 12, 4)['front']
    for j in range(5):
        s.put(x + 3 - j // 2, y + j, shade(gown, 0.7))
        s.put(x + 4 + j // 2, y + j, shade(gown, 0.7))
    s.fill(x + 3, y, 2, 1, SKIN, noise=2)

    # arms: wide sleeves of the gown, hands at the cuff
    s.box(40, 16, 4, 12, 4, gown, noise=5)
    for side in ('right', 'front', 'left', 'back'):
        x, y, w, h = faces(40, 16, 4, 12, 4)[side]
        petal_pattern(s, x, y, w, h - 3, gown, deep, flower, 2)
        s.fill(x, y + h - 3, w, 1, flower, noise=3)
        s.fill(x, y + h - 2, w, 2, SKIN, noise=3)
    x, y, w, h = faces(40, 16, 4, 12, 4)['bottom']
    s.fill(x, y, w, h, SKIN, noise=3)

    # the skirt: petals in layers, deepening to the hem, a band of flowers at it
    s.box(0, 32, 10, 14, 7, gown, noise=5)
    for side in ('right', 'front', 'left', 'back'):
        x, y, w, h = faces(0, 32, 10, 14, 7)[side]
        petal_pattern(s, x, y, w, h, gown, deep, flower, 3)
        for i in range(w):
            s.put(x + i, y + h - 1, shade(deep, 0.75 if i % 2 else 0.9))
    x, y, w, h = faces(0, 32, 10, 14, 7)['top']
    s.fill(x, y, w, h, sash, noise=4)

    # the bun, and the flower in it
    s.box(36, 32, 4, 3, 4, HAIR, noise=5)
    s.box(36, 40, 3, 3, 2, flower, noise=4)
    x, y, w, h = faces(36, 40, 3, 3, 2)['front']
    s.put(x + 1, y + 1, (246, 200, 70))
    x, y, w, h = faces(36, 40, 3, 3, 2)['left']
    s.put(x, y + 1, (246, 200, 70))
    s.save('entity', 'flower_maid_captain.png' if captain else 'flower_maid.png')


paint(False)
paint(True)
