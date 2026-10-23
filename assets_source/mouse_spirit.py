"""Paints the Mouse Spirit's skins (textures/entity/mouse_spirit.png and mouse_spirit_captain.png,
64x64) to the box layout of CaveRenderers.mouseLayer(): grey-brown fur, a paler belly, a ragged
vest, pink ears, hands, feet, nose and a long bare tail, beady black eyes with a red glint. The
captain is a black rat in a red headband, its vest dark.

Run from the repository root: python assets_source/mouse_spirit.py"""
from cave_skin import Skin, faces, shade

PINK = (214, 150, 152)
NOSE = (236, 120, 140)


def paint(captain):
    fur = (70, 64, 66) if captain else (148, 134, 118)
    belly = (110, 100, 100) if captain else (206, 194, 176)
    vest = (52, 26, 24) if captain else (104, 72, 46)
    s = Skin(64, 64, seed=11 if captain else 7)

    # head: fur all round; on its face, beady eyes high and wide
    s.box(0, 0, 6, 5, 5, fur, noise=9)
    f = faces(0, 0, 6, 5, 5)['front']
    for ex in (f[0] + 1, f[0] + 4):
        # small black beady eyes high on the face, above the snout
        s.put(ex, f[1] + 1, (12, 10, 12))
        s.put(ex + (-1 if ex == f[0] + 1 else 1), f[1] + 1, shade(fur, 0.8))
    # a pale chin under the snout
    for i in range(1, 5):
        s.put(f[0] + i, f[1] + 4, shade(belly, 0.95))
    if captain:
        # a red headband round the head, knotted at the back
        for side in ('right', 'front', 'left', 'back'):
            x, y, w, h = faces(0, 0, 6, 5, 5)[side]
            for i in range(w):
                s.put(x + i, y + 0, (176, 30, 30))
        bx, by, _, _ = faces(0, 0, 6, 5, 5)['back']
        s.put(bx + 2, by + 1, (150, 20, 20))
        s.put(bx + 3, by + 1, (150, 20, 20))

    # snout: lighter fur toward the tip, a pink nose, whiskers on its sides
    s.box(22, 0, 3, 3, 3, fur, noise=6, to=belly)
    nf = faces(22, 0, 3, 3, 3)
    x, y, _, _ = nf['front']
    s.fill(x, y, 3, 3, shade(belly, 0.95), noise=4)
    s.put(x + 1, y, NOSE)
    s.put(x, y, shade(NOSE, 0.85))
    s.put(x + 2, y, shade(NOSE, 0.85))
    for side in ('right', 'left'):
        x, y, w, h = nf[side]
        for i in range(w):
            s.put(x + i, y + 1, (236, 236, 228))
        s.put(x + 1, y + 2, (236, 236, 228))

    # ears: fur behind, pink within
    s.box(34, 0, 3, 3, 1, fur, noise=6)
    x, y, w, h = faces(34, 0, 3, 3, 1)['front']
    s.fill(x, y, w, h, PINK, noise=5)
    s.put(x + 1, y + 1, shade(PINK, 0.85))

    # body: fur, a pale belly, a ragged vest open at the front, with a frayed hem
    s.box(0, 12, 6, 8, 4, vest, noise=8)
    x, y, w, h = faces(0, 12, 6, 8, 4)['front']
    s.fill(x + 1, y, 4, h, belly, noise=6)
    for side in ('right', 'front', 'left', 'back'):
        x, y, w, h = faces(0, 12, 6, 8, 4)[side]
        for i in range(w):
            if (i * 7 + len(side)) % 3 == 0:
                s.put(x + i, y + h - 1, fur)   # frayed: fur showing at the hem
        if side != 'front':
            s.put(x + w // 2, y + 3, shade(vest, 0.7))  # a patch, a tear
    x, y, w, h = faces(0, 12, 6, 8, 4)['top']
    s.fill(x, y, w, h, fur, noise=6)

    # arms and legs: fur, pink hands and feet
    s.box(24, 12, 2, 6, 2, fur, noise=7)
    for side in ('right', 'front', 'left', 'back'):
        x, y, w, h = faces(24, 12, 2, 6, 2)[side]
        s.fill(x, y + h - 2, w, 2, PINK, noise=5)
    x, y, w, h = faces(24, 12, 2, 6, 2)['bottom']
    s.fill(x, y, w, h, PINK, noise=5)
    s.box(32, 12, 2, 6, 2, fur, noise=7, grad=0.2)
    s.box(40, 12, 2, 1, 3, PINK, noise=5)

    # the tail: bare, pink, ringed
    for (u, v, n) in ((0, 26, 8), (20, 26, 7)):
        s.box(u, v, 1, 1, n, PINK, noise=6)
        for side in ('top', 'bottom'):
            x, y, w, h = faces(u, v, 1, 1, n)[side]
            for j in range(0, h, 2):
                s.put(x, y + j, shade(PINK, 0.82))
        for side in ('right', 'left'):
            x, y, w, h = faces(u, v, 1, 1, n)[side]
            for i in range(0, w, 2):
                s.put(x + i, y, shade(PINK, 0.82))
    s.save('entity', 'mouse_spirit_captain.png' if captain else 'mouse_spirit.png')


paint(False)
paint(True)
