"""Paints Lady Earth Flow's skin (textures/entity/lady_earth_flow.png, 128x64) to the layout of
CaveRenderers.ladyLayer(): the Golden-Nosed, White-Furred Mouse Spirit as a beautiful woman. A pale
face, dark almond eyes under pink lids, red lips; long white hair, a bun pinned with gold; round
white mouse's ears, pink within; a nose of gold. A rose-red robe embroidered with gold flowers, a
gold sash, a collar of white fur and white fur at the cuffs and the hem; a thin pink tail.

Run from the repository root: python assets_source/lady_earth_flow.py"""
from cave_skin import Skin, faces, shade, mix

SKIN = (246, 230, 218)
HAIR = (236, 236, 242)
ROBE = (176, 34, 62)
DEEP = (120, 18, 42)
GOLD = (228, 182, 64)
FUR = (250, 250, 248)
PINK = (232, 168, 172)

s = Skin(128, 64, seed=43)


def hair_fill(x, y, w, h):
    for j in range(h):
        for i in range(w):
            c = HAIR if (i * 3 + j // 3) % 7 else shade(HAIR, 0.86)   # strands
            n = s.rnd.randint(-4, 4)
            s.put(x + i, y + j, tuple(v + n for v in c))


def fur_fill(x, y, w, h):
    for j in range(h):
        for i in range(w):
            k = 1.0 if s.rnd.random() < 0.7 else 0.9
            s.put(x + i, y + j, shade(FUR, k))


def robe_fill(x, y, w, h, flowers=True):
    s.fill(x, y, w, h, ROBE, noise=5, to=DEEP)
    if not flowers:
        return
    for k in range(max(1, w * h // 30)):
        fx, fy = x + s.rnd.randrange(w), y + s.rnd.randrange(h)
        s.put(fx, fy, GOLD)
        if fx + 1 < x + w:
            s.put(fx + 1, fy, shade(GOLD, 0.85))
        if fy + 1 < y + h:
            s.put(fx, fy + 1, shade(GOLD, 0.85))


# ── head: white hair round it; her face in front ───────────────────────
s.box(0, 0, 8, 8, 8, HAIR)
for side in ('top', 'right', 'left', 'back'):
    hair_fill(*faces(0, 0, 8, 8, 8)[side])
x, y, w, h = faces(0, 0, 8, 8, 8)['front']
s.fill(x, y + 1, 8, 7, SKIN, noise=2)
for i in range(8):
    s.put(x + i, y, HAIR)
    if i in (0, 1, 6, 7):
        s.put(x + i, y + 1, HAIR)
for j in range(2, 7):
    s.put(x, y + j, HAIR)
    s.put(x + 7, y + j, HAIR)
# her eyes: dark, long, a fine brow over each; a small mouth; her cheeks barely flushed
for ex, inner in ((1, 2), (6, 5)):
    s.put(x + ex, y + 3, (150, 140, 150))          # brows, fine and grey-white like her hair
    s.put(x + inner, y + 3, (170, 160, 170))
    s.put(x + ex, y + 4, (250, 246, 244))          # the white of the eye
    s.put(x + inner, y + 4, (52, 30, 52))          # the iris, toward the nose
s.put(x + 2, y + 5, (250, 214, 212))
s.put(x + 5, y + 5, (250, 214, 212))
s.put(x + 3, y + 6, (204, 70, 84))
s.put(x + 4, y + 6, (182, 50, 66))                 # a small red mouth
# the hat layer: her hair over it, long at the back and sides, a fringe in front
for side in ('top', 'right', 'left', 'back'):
    hair_fill(*faces(32, 0, 8, 8, 8)[side])
x, y, w, h = faces(32, 0, 8, 8, 8)['front']
for i in range(8):
    s.put(x + i, y, HAIR)
    if i in (0, 1, 2, 5, 6, 7):
        s.put(x + i, y + 1, shade(HAIR, 0.95))
for j in range(2, 8):
    s.put(x, y + j, HAIR)
    s.put(x + 7, y + j, HAIR)
for side in ('right', 'left'):
    x, y, w, h = faces(32, 0, 8, 8, 8)[side]
    for j in range(h):
        for i in range(w):
            if j < 7 or i > 2:
                s.put(x + i, y + j, shade(HAIR, 0.97))

# ── body: the robe's bodice, crossed at the front over a gold sash ─────
for side in ('top', 'bottom', 'right', 'front', 'left', 'back'):
    robe_fill(*faces(16, 16, 8, 12, 4)[side], flowers=side != 'top')
for side in ('right', 'front', 'left', 'back'):
    x, y, w, h = faces(16, 16, 8, 12, 4)[side]
    s.fill(x, y + 8, w, 2, GOLD, noise=4)
    for i in range(w):
        s.put(x + i, y + 10, shade(GOLD, 0.7) if i % 2 else shade(GOLD, 0.85))
x, y, w, h = faces(16, 16, 8, 12, 4)['front']
for j in range(7):
    s.put(x + 2 + j // 2, y + j, GOLD)          # the crossed collar's gold edges
    s.put(x + 5 - j // 2, y + j, GOLD)
s.fill(x + 3, y, 2, 2, SKIN, noise=2)

# ── arms: the robe's sleeves, white fur at the cuffs, her hands ───────
for side in ('top', 'right', 'front', 'left', 'back'):
    x, y, w, h = faces(40, 16, 4, 12, 4)[side]
    if side == 'top':
        robe_fill(x, y, w, h, False)
        continue
    robe_fill(x, y, w, 8)
    fur_fill(x, y + 8, w, 2)
    s.fill(x, y + 10, w, 2, SKIN, noise=2)
s.fill(*faces(40, 16, 4, 12, 4)['bottom'], SKIN, noise=2)

# ── the skirt: the robe to the ground, a gold panel down its front, white fur at its hem ──
for side in ('top', 'bottom', 'right', 'front', 'left', 'back'):
    x, y, w, h = faces(64, 0, 10, 14, 7)[side]
    robe_fill(x, y, w, h, flowers=side not in ('top', 'bottom'))
    if side in ('right', 'front', 'left', 'back'):
        fur_fill(x, y + h - 2, w, 2)
x, y, w, h = faces(64, 0, 10, 14, 7)['front']
for j in range(h - 2):
    s.put(x + 4, y + j, GOLD)
    s.put(x + 5, y + j, shade(GOLD, 0.85))

# ── the collar of white fur ──────────────────────────────────────────
for side in ('top', 'bottom', 'right', 'front', 'left', 'back'):
    fur_fill(*faces(64, 24, 10, 2, 6)[side])

# ── her hair down her back ───────────────────────────────────────────
for side in ('top', 'bottom', 'right', 'front', 'left', 'back'):
    hair_fill(*faces(100, 0, 8, 13, 1)[side])
x, y, w, h = faces(100, 0, 8, 13, 1)['back']
for i in range(w):
    s.put(x + i, y + 3, GOLD if i % 3 else shade(GOLD, 0.8))   # a gold cord binding it

# ── ears: white fur, pink within ─────────────────────────────────────
for side in ('top', 'bottom', 'right', 'left', 'back'):
    fur_fill(*faces(100, 16, 3, 3, 1)[side])
x, y, w, h = faces(100, 16, 3, 3, 1)['front']
fur_fill(x, y, w, h)
s.put(x + 1, y + 1, PINK)
s.put(x + 1, y + 2, shade(PINK, 0.9))

# ── the golden nose ──────────────────────────────────────────────────
for side, k in (('top', 1.15), ('bottom', 0.8), ('right', 0.9), ('front', 1.05), ('left', 0.9), ('back', 0.9)):
    x, y, w, h = faces(110, 16, 1, 1, 1)[side]
    s.put(x, y, shade(GOLD, k))

# ── the tail: thin, bare, pink ───────────────────────────────────────
s.box(64, 34, 1, 1, 10, PINK, noise=5)

# ── the bun, pinned with gold ────────────────────────────────────────
for side in ('top', 'bottom', 'right', 'front', 'left', 'back'):
    hair_fill(*faces(98, 34, 4, 3, 4)[side])
for side in ('right', 'left'):
    x, y, w, h = faces(98, 34, 4, 3, 4)[side]
    for i in range(w):
        s.put(x + i, y + 1, GOLD)

s.save('entity', 'lady_earth_flow.png')
