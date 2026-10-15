"""Paints the Great White Bull's skin (textures/entity/great_white_bull.png, 128x128) to the box
layout of GreatWhiteBullRenderer.layer(): each box's six faces in the standard box-UV places. Also
paints its eyes alone (great_white_bull_eyes.png), drawn full-bright over the skin so they burn.

Run from the repository root: python assets_source/great_white_bull.py"""
import random
from PIL import Image

W = H = 128
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
rnd = random.Random(7)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def put(x, y, c):
    img.putpixel((x, y), c if len(c) == 4 else tuple(c) + (255,))


def fill(x, y, w, h, base, k=1.0, noise=6, grad=0.0, to=None):
    """A face: the base colour at brightness k, a little mottled, darker toward its bottom by grad,
    or blending toward the colour `to` down its height."""
    for j in range(h):
        t = j / max(1, h - 1)
        c = mix(base, to, t) if to else base
        g = 1 - grad * t
        n = rnd.randint(-noise, noise)
        for i in range(w):
            n = max(-noise, min(noise, n + rnd.randint(-2, 2)))
            s = shade(c, k * g)
            put(x + i, y + j, tuple(max(0, min(255, v + n)) for v in s[:3]) + (255,))


def box(u, v, w, h, d, base, grad=0.18, noise=6, top=1.06, bottom=0.6, side=0.86, front=1.0, back=0.9, to=None):
    fill(u + d, v, w, d, base, top, noise)                         # top
    fill(u + d + w, v, w, d, base, bottom, noise)                  # bottom
    fill(u, v + d, d, h, base, side, noise, grad, to)              # right
    fill(u + d, v + d, w, h, base, front, noise, grad, to)         # front
    fill(u + d + w, v + d, d, h, base, side, noise, grad, to)      # left
    fill(u + 2 * d + w, v + d, w, h, base, back, noise, grad, to)  # back


def front_face(u, v, w, h, d):
    """Where a box's front (its -z face) lies: x, y, width, height."""
    return u + d, v + d, w, h


WHITE = (226, 224, 218)      # steel-white hide
SHADOW = (150, 148, 150)     # its shadowed undersides
MASK = (118, 114, 116)       # the face's dark mask
CHARCOAL = (52, 48, 50)      # snout, hooves, tuft
HORN_ROOT = (34, 30, 32)
BONE = (196, 186, 160)
GOLD = (214, 168, 62)
EMBER = (255, 54, 24)
EMBER_CORE = (255, 196, 96)

# ── the body ──────────────────────────────────────────────────────────
box(0, 0, 20, 19, 14, WHITE, grad=0.3)                 # chest and shoulders: shadowed toward the belly
box(0, 33, 16, 15, 17, WHITE, grad=0.32)               # hindquarters
box(28, 104, 12, 5, 9, WHITE, grad=0.1)                # the back, stepping down from the hump
box(0, 104, 8, 6, 6, WHITE, grad=0.4, to=SHADOW)       # the dewlap
box(68, 0, 14, 8, 11, WHITE, grad=0.05, top=1.0)       # the hump
box(68, 19, 14, 13, 8, WHITE, grad=0.35, to=SHADOW)    # the neck, darkening to the dewlap
box(84, 80, 2, 4, 2, BONE, grad=-0.3, noise=4, to=HORN_ROOT)  # the hump's spikes: bone, dark at the root

# a dark ridge down the hump's top, where the spikes stand
for j in range(11):
    for i in range(5, 9):
        put(68 + 11 + i, j, shade(SHADOW, 0.9 + 0.02 * rnd.randint(-2, 2)))

# scars: a few pale-dark gashes across the chest's flanks
for (sx, sy) in ((4, 20), (40, 24), (58, 18)):
    for k in range(5):
        put(sx + k, sy + k // 2, shade(SHADOW, 0.75))

# ── the head ──────────────────────────────────────────────────────────
box(68, 40, 12, 10, 10, WHITE, grad=0.2)               # skull
box(66, 60, 14, 3, 4, MASK, grad=0.25, noise=4)        # brow: heavy and dark
box(0, 66, 9, 7, 7, CHARCOAL, grad=0.1, noise=5, top=1.25)  # snout
box(32, 66, 8, 2, 5, CHARCOAL, grad=0, noise=4)        # jaw
box(92, 80, 5, 1, 1, GOLD, grad=0, noise=3)            # nose ring
box(104, 80, 1, 3, 1, GOLD, grad=0, noise=3)
box(72, 80, 4, 2, 1, WHITE, grad=0, noise=4)           # ears

# the skull's front: a dark mask about the eyes, the eyes deep under the brow, burning
fx, fy, fw, fh = front_face(68, 40, 12, 10, 10)
for j in range(fh):
    for i in range(fw):
        edge = min(i, fw - 1 - i)
        if j <= 5 and (edge <= 3 or j <= 1):
            put(fx + i, fy + j, shade(MASK, 0.85 + 0.03 * rnd.randint(-1, 1)))
for ex in (fx + 1, fx + fw - 3):
    for i in range(2):
        put(ex + i, fy + 1, (16, 12, 14, 255))          # the socket's shadow under the brow
        for j, c in ((2, EMBER), (3, EMBER_CORE if i == (0 if ex > fx + 5 else 1) else EMBER)):
            put(ex + i, fy + j, c + (255,))
            glow.putpixel((ex + i, fy + j), c + (255,))
        put(ex + i, fy + 4, (24, 20, 22, 255))
# the eyes show at the skull's sides too, a glint of red under the brow
for (sx, sy) in ((68 + 10 - 2, 40 + 10 + 2), (68 + 10 + 12, 40 + 10 + 2)):
    for i in range(2):
        put(sx + i, sy, EMBER + (255,))
        glow.putpixel((sx + i, sy), EMBER + (255,))

# the brow's front: darkest at its underside, a V that scowls toward the nose
bx, by, bw, bh = front_face(66, 60, 14, 3, 4)
for i in range(bw):
    dip = 1 if abs(i - (bw - 1) / 2) < 3 else 0
    for j in range(bh):
        k = 0.55 if j >= bh - 1 - dip else 0.8
        put(bx + i, by + j, shade(MASK, k))

# the snout's front: flared nostrils, red-dark within
sx, sy, sw, sh = front_face(0, 66, 9, 7, 7)
for nx in (sx + 1, sx + sw - 3):
    for i in range(2):
        put(nx + i, sy + 1, (14, 10, 12, 255))
        put(nx + i, sy + 2, (64, 18, 16, 255))
# a lighter muzzle line where the snout meets the jaw
for i in range(sw):
    put(sx + i, sy + sh - 1, shade(CHARCOAL, 1.4))

# ── the horns: black at the skull, bone along, gold at the points ─────
box(58, 67, 9, 4, 4, HORN_ROOT, grad=0, noise=4)
# the base's faces run along its length (x): darkest at the skull, paling outward
for y in range(67, 67 + 8):
    for x in range(58, 58 + 26):
        c = img.getpixel((x, y))
        if c[3] == 0:
            continue
        put(x, y, mix(HORN_ROOT, mix(HORN_ROOT, BONE, 0.45), ((x - 58) % 13) / 12))
box(84, 67, 3, 8, 3, BONE, grad=-0.15, noise=4, to=mix(HORN_ROOT, BONE, 0.55)[:3])  # rising: paler toward the tip
box(96, 67, 2, 6, 2, GOLD, grad=0, noise=4, to=(250, 226, 140))                     # the points

# ── legs: white above, dark stockings, black cloven hooves ────────────
for (u, w, d) in ((0, 7, 7), (28, 6, 6)):
    h = 14
    box(u, 80, w, h, d, WHITE, grad=0.1)
    for y in range(80 + d, 80 + d + h):
        t = (y - (80 + d)) / (h - 1)
        if t < 0.4:
            continue
        k = (t - 0.4) / 0.6
        for x in range(u, u + 2 * (w + d)):
            c = img.getpixel((x, y))
            hoof = y >= 80 + d + h - 3
            put(x, y, shade(CHARCOAL, 0.8) if hoof else mix(c[:3], CHARCOAL, min(1, 0.35 + k * 1.2)))
    # the cleft down each hoof's front, and the sole
    for y in range(80 + d + h - 3, 80 + d + h):
        put(u + d + w // 2, y, (10, 8, 10, 255))
    for x in range(u + d + w, u + d + 2 * w):
        for y in range(80, 80 + d):
            put(x, y, shade(CHARCOAL, 0.7))

# ── the tail: a whip, the tuft dark ───────────────────────────────────
box(52, 80, 2, 14, 2, WHITE, grad=0.3, to=SHADOW)
box(60, 80, 3, 5, 3, CHARCOAL, noise=10)

img.save('src/main/resources/assets/jttw_engine/textures/entity/great_white_bull.png')
glow.save('src/main/resources/assets/jttw_engine/textures/entity/great_white_bull_eyes.png')
print('ok')
