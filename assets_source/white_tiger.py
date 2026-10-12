"""The White Tiger, Byakko's true form: its model and its skin from one list of parts.

Writes the body of WhiteTigerRenderer.layer() (between its GENERATED markers) and paints
textures/entity/white_tiger.png to the same box-UV layout, so the two never drift apart. The skin
is painted at twice the model's resolution (each model pixel is 2x2 texture pixels), so the face
can carry real eyes.

A tiger's build: long and low, the back near level from shoulder to hip, the chest deep; the head
broad and round at shoulder height on a short neck, its face flat with a short muzzle and cheek
ruffs; legs strong and not long, the forelegs heavier; curved claws out over the forepaws; a long
tail carried low. Every box is shaded for its form: lit above, darker down its sides, darkest beneath.
"""
import math
import random
import re
from PIL import Image

rnd = random.Random(23)
UNITS = 256          # the model's texture size, in model pixels
S = 2                # texture pixels per model pixel
TEX = UNITS * S

# ── the parts ────────────────────────────────────────────────────────────────
# (name, parent, pivot (x, y, z), rotation (x, y, z) radians, boxes, mirror)
# each box: (x, y, z, w, h, d, style); model pixels, ground at y 24, the head toward -z
FORE = [(-2.5, 0, -2.5, 5, 7, 5, 'fur'), (-2, 7, -2, 4, 4, 4, 'fur'), (-3, 10.5, -4, 6, 1.5, 6, 'paw')]
HIND = [(-3, -3, -3, 6, 9, 6, 'fur'), (-2, 6, -1.5, 4, 5, 4, 'fur'), (-2.75, 10.5, -3.5, 5.5, 1.5, 5.5, 'paw')]
PARTS = [
    ('body', 'root', (0, 0, 0), (0, 0, 0), [
        (-6.5, 2, -18, 13, 11, 12, 'fur'),        # the chest and shoulders, deep
        (-5.5, 1.2, -16, 11, 1, 8, 'fur'),        # the shoulders' rise, slight
        (-5.5, 3, -6, 11, 9, 12, 'fur'),          # the waist, drawn in
        (-6, 2.5, 6, 12, 9.5, 12, 'fur'),         # the haunches
    ], False),
    ('neck', 'root', (0, 5, -17), (0.15, 0, 0), [(-4.5, -3.5, -5, 9, 8, 6, 'fur')], False),
    ('head', 'root', (0, 4.5, -21), (0, 0, 0), [
        (-5.5, -4.5, -8, 11, 9, 8, 'skull'),      # the head: broad, its face flat
        (-7.2, -1.5, -6.5, 2, 6, 5, 'ruff'),      # the cheek ruffs, flaring
        (5.2, -1.5, -6.5, 2, 6, 5, 'ruff'),
        (-3.5, 1.5, -10.5, 7, 3, 2.5, 'muzzle'),  # the muzzle, short and broad, below the eyes
        (-2.7, 4.5, -10.3, 0.8, 2.2, 0.8, 'fang'),  # the upper fangs
        (1.9, 4.5, -10.3, 0.8, 2.2, 0.8, 'fang'),
        (-5, -6.5, -3, 3, 2.5, 1.5, 'ear'),       # the ears, round, a little back
        (2, -6.5, -3, 3, 2.5, 1.5, 'ear'),
    ], False),
    ('jaw', 'head', (0, 4, -8), (0.3, 0, 0), [
        (-2.5, 0, -2.5, 5, 1.5, 3, 'jaw'),
        (-2, -1.2, -2.3, 0.7, 1.2, 0.7, 'fang'),  # the lower fangs
        (1.3, -1.2, -2.3, 0.7, 1.2, 0.7, 'fang'),
    ], False),
    ('right_front_leg', 'root', (-4.2, 12, -12), (0, 0, 0), FORE, False),
    ('left_front_leg', 'root', (4.2, 12, -12), (0, 0, 0), FORE, True),
    ('right_hind_leg', 'root', (-4, 12, 12), (0, 0, 0), HIND, False),
    ('left_hind_leg', 'root', (4, 12, 12), (0, 0, 0), HIND, True),
    ('tail', 'root', (0, 3.5, 17.5), (-0.55, 0, 0), [(-1.25, -1.25, 0, 2.5, 2.5, 12, 'tail')], False),
    ('tail_end', 'tail', (0, 0, 12), (0.5, 0, 0), [(-1, -1, 0, 2, 2, 11, 'tailtip')], False),
]
# curved claws over each forepaw: a base hooked down from the paw's front, its point curling under
for leg in ('right_front_leg', 'left_front_leg'):
    for k, cx in enumerate((-2.1, -0.7, 0.7, 2.1)):
        base = '%s_claw%d' % (leg.split('_')[0], k)
        PARTS.append((base, leg, (cx, 11.0, -3.6), (0.55, 0, 0), [(-0.3, -0.3, -1.6, 0.6, 0.6, 1.6, 'claw')], False))
        PARTS.append((base + '_tip', base, (0, 0, -1.6), (0.9, 0, 0), [(-0.22, -0.22, -1.1, 0.44, 0.44, 1.1, 'clawtip')], False))

# ── pack each distinct box's six faces into the texture (model pixels) ──────
def size(b):
    return tuple(max(1, math.ceil(v)) for v in b[3:6])


slots = {}
for _, _, _, _, boxes, _ in PARTS:
    for b in boxes:
        slots.setdefault(size(b) + (b[6],), None)
x = y = row = 0
for key in sorted(slots, key=lambda k: -(k[2] + k[1])):
    w, h, d, _ = key
    rw, rh = 2 * d + 2 * w, d + h
    if x + rw > UNITS:
        x, y, row = 0, y + row + 1, 0
    slots[key] = (x, y)
    x += rw + 1
    row = max(row, rh)
assert y + row <= UNITS, 'texture too small'

# ── paint, at S texture pixels per model pixel ──────────────────────────────
img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
WHITE, BELLY, STRIPE = (238, 236, 230), (214, 208, 196), (28, 26, 32)
EYE, NOSE, MOUTH = (110, 196, 245), (176, 106, 112), (104, 30, 38)
IVORY, PAD = (236, 228, 204), (60, 52, 56)


def px(p, c):
    if 0 <= p[0] < TEX and 0 <= p[1] < TEX:
        img.putpixel((int(p[0]), int(p[1])), tuple(max(0, min(255, int(v))) for v in c[:3]) + (255,))


def faces(u, v, w, h, d):
    f = {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
         'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
    return {k: tuple(c * S for c in r) for k, r in f.items()}


def face(rect, base, light, noise=5, fall=0.0):
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            k = light * (1 - fall * (j / max(1, fh - 1)))
            n = rnd.randint(-noise, noise)
            px((fx + i, fy + j), [c * k + n for c in base])


def stripes(rect, spacing, vertical=True, reach=0.7):
    """Dark stripes, tapering to points and a little wavy, spaced along the face (texture pixels)."""
    fx, fy, fw, fh = rect
    length, across = (fw, fh) if vertical else (fh, fw)
    pos = rnd.randint(1, max(1, spacing))
    while pos < length - 2:
        r = max(2, int(across * (reach + rnd.uniform(-0.15, 0.12))))
        from_top = rnd.random() < 0.7
        for t in range(r):
            thick = 3 if t < r * 0.35 else 2 if t < r * 0.7 else 1
            wob = round(1.2 * math.sin(t * 0.45 + pos))
            for k in range(thick):
                a, b2 = pos + k + wob, (t if from_top else across - 1 - t)
                p = (fx + a, fy + b2) if vertical else (fx + b2, fy + a)
                if fx <= p[0] < fx + fw and fy <= p[1] < fy + fh:
                    px(p, [c * rnd.uniform(0.9, 1.1) for c in STRIPE])
        pos += spacing + rnd.randint(-2, 2)


def shaded(f, base=WHITE):
    face(f['top'], base, 1.06)
    face(f['bottom'], BELLY, 0.72)
    for s in ('right', 'left'):
        face(f[s], base, 0.94, fall=0.32)
    face(f['front'], base, 1.0, fall=0.22)
    face(f['back'], base, 0.9, fall=0.3)


def eye(fx, fy, inward):
    """A tiger's eye, from its outer corner (fx, fy), opening toward `inward`: deep-set and hooded,
    narrow, the heavy upper lid falling toward the inner corner and on into a tear line; a small
    iris, shadowed under the lid, about a slit pupil; no glint. Lighter fur above, under a dark brow
    stripe slanting down toward the nose."""
    X = lambda i: fx + i * inward
    top = [1, 1, 1, 1, 2, 2, 2]              # the open eye: two rows at most, narrowing inward
    bottom = [2, 2, 3, 3, 3, 3, 3]
    for i in range(7):                       # paler fur above the eye, softly
        px((X(i), fy + top[i] - 3), (246, 244, 238))
    for i in range(9):                       # the brow stripe, heavy, slanting down inward
        for j in (-6, -5):
            px((X(i), fy + j + i // 2), STRIPE)
    for i in range(7):
        for j in range(top[i], bottom[i] + 1):
            shadow = 0.55 if j == top[i] else 0.85   # the lid's shadow on the iris
            px((X(i), fy + j), [c * shadow for c in EYE])
        for j in (top[i] - 1, top[i] - 2):            # the heavy upper lid
            px((X(i), fy + j), STRIPE)
        px((X(i), fy + bottom[i] + 1), STRIPE)        # the lower lid
    for j in (top[3], top[3] + 1, top[3] + 2):        # the pupil: a narrow upright slit
        px((X(3), fy + j), (12, 12, 16))
    px((X(-1), fy + 1), STRIPE)                       # the corners, drawn out sharp
    px((X(-1), fy + 0), STRIPE)
    for i in (7, 8):
        px((X(i), fy + 3), STRIPE)
        px((X(i), fy + 4), STRIPE)
    for j in range(5, 10):                            # the tear line, down from the inner corner
        px((X(8) + (j - 5) // 3 * inward, fy + j), STRIPE)


for (w, h, d, style), (u, v) in slots.items():
    f = faces(u, v, w, h, d)
    if style in ('fur', 'skull', 'ruff', 'tail', 'tailtip', 'muzzle', 'jaw'):
        shaded(f)
    if style in ('fur', 'ruff', 'tail'):
        for s in ('right', 'left'):
            stripes(f[s], 6 if style != 'ruff' else 4, vertical=True, reach=0.75)
        stripes(f['top'], 6, vertical=False, reach=0.85)
        if style == 'fur':
            stripes(f['front'], 8, vertical=True, reach=0.45)
            stripes(f['back'], 8, vertical=True, reach=0.45)
    if style == 'tailtip':
        for s in ('right', 'left', 'top', 'bottom'):
            stripes(f[s], 6, vertical=s in ('top', 'bottom'), reach=1.0)
        face(f['back'], STRIPE, 1.0, noise=3)
    if style == 'skull':
        # the mark of a king (王) on the forehead, on top of the head toward its front
        tx, ty, tw, th = f['top']
        cx = tx + tw // 2
        for i in range(-5, 5):
            for j in (th - 2, th - 6, th - 10):
                px((cx + i, ty + j), STRIPE)
                px((cx + i, ty + j - 1), STRIPE)
        for j in range(th - 11, th - 1):
            px((cx - 1, ty + j), STRIPE)
            px((cx, ty + j), STRIPE)
        for s in ('right', 'left'):
            stripes(f[s], 5, vertical=True, reach=0.5)
        fx, fy, fw, fh = f['front']               # 22 x 18: the face
        eye(fx + 1, fy + 7, 1)
        eye(fx + fw - 2, fy + 7, -1)
        for i in range(fw // 2 - 3, fw // 2 + 3):  # the forehead's short stripes between the eyes
            if i % 2 == 0:
                for j in range(1, 5):
                    px((fx + i, fy + j), STRIPE)
    if style == 'ruff':
        face(f['front'], WHITE, 1.05, fall=0.1)
    if style == 'muzzle':
        fx, fy, fw, fh = f['front']               # 12 x 7
        for j in range(3):                        # the nose: a broad dark triangle
            for i in range(fw // 2 - 3 + j, fw // 2 + 3 - j):
                px((fx + i, fy + j), [c * 0.55 for c in NOSE])
        for j in range(3, fh - 1):                # the line from nose to lip
            px((fx + fw // 2 - 1, fy + j), STRIPE)
        for i in range(fw):                       # the lip
            px((fx + i, fy + fh - 1), MOUTH if 1 < i < fw - 2 else STRIPE)
        for s in ('right', 'left'):               # whisker spots
            rx, ry, rw, rh = f[s]
            for k in range(4):
                px((rx + 1 + k, ry + 2 + (k % 2) * 2), STRIPE)
        face(f['bottom'], MOUTH, 0.8, noise=4)    # the roof of the open mouth
    if style == 'jaw':
        face(f['top'], MOUTH, 0.9, noise=4)
    if style == 'fang':
        for r in f.values():
            face(r, IVORY, 1.0, noise=3)
    if style == 'claw':
        for r in f.values():
            face(r, IVORY, 0.95, noise=3, fall=0.2)
    if style == 'clawtip':
        for r in f.values():
            face(r, (60, 56, 52), 1.0, noise=3)
    if style == 'paw':
        face(f['top'], WHITE, 1.0)
        for s in ('right', 'left', 'front', 'back'):
            face(f[s], WHITE, 0.88, fall=0.35)
        face(f['bottom'], PAD, 1.0, noise=4)
        stripes(f['top'], 5, vertical=False, reach=0.6)
    if style == 'ear':
        face(f['front'], (226, 214, 214), 0.95)
        for s in ('back', 'top', 'right', 'left', 'bottom'):
            face(f[s], STRIPE, 1.2, noise=4)
        bx, by, bw, bh = f['back']
        for i in range(bw // 2 - 1, bw // 2 + 1):  # the white spot on a tiger's ear
            for j in range(bh // 2 - 1, bh // 2 + 1):
                px((bx + i, by + j), (250, 250, 248))

img.save('src/main/resources/assets/jttw_engine/textures/entity/white_tiger.png')

# ── the model's code ────────────────────────────────────────────────────────
def num(v):
    s = ('%g' % v)
    return s + ('f' if ('.' in s or 'e' in s) else '')


def camel(n):
    return re.sub(r'_(\w)', lambda m: m.group(1).upper(), n)


lines = ['        MeshDefinition mesh = new MeshDefinition();', '        PartDefinition root = mesh.getRoot();']
for name, parent, (px_, py_, pz_), (rx, ry, rz), boxes, mirror in PARTS:
    cubes = 'CubeListBuilder.create()'
    for b in boxes:
        u, v = slots[size(b) + (b[6],)]
        cubes += '\n                .texOffs(%d, %d).mirror(%s).addBox(%s, %s, %s, %s, %s, %s)' % (
            u, v, 'true' if mirror else 'false', num(b[0]), num(b[1]), num(b[2]), num(b[3]), num(b[4]), num(b[5]))
    pose = 'PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)' % tuple(num(c) for c in (px_, py_, pz_, rx, ry, rz))
    lines.append('        PartDefinition %s = %s.addOrReplaceChild("%s", %s, %s);' % (
        camel(name), 'root' if parent == 'root' else camel(parent), name, cubes, pose))
lines.append('        return LayerDefinition.create(mesh, %d, %d);' % (UNITS, UNITS))
code = '\n'.join(lines)

java = 'src/main/java/com/jttw/engine/client/renderer/WhiteTigerRenderer.java'
s = open(java, encoding='utf-8').read()
start, end = '        // GENERATED by assets_source/white_tiger.py: begin', '        // GENERATED: end'
a = s.index(start) + len(start)
b = s.index(end)
s = s[:a] + '\n' + code + '\n' + s[b:]
open(java, 'w', encoding='utf-8').write(s)
print('model and skin written;', len(slots), 'distinct boxes;', TEX, 'px texture')
