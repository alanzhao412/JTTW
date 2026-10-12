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
def paw(front):
    """A paw: a pad hardly wider than the leg above it, its back rising to the leg, and four toes
    rounding its front (the forepaw's broader, set further forward)."""
    z0, d = (-3.0, 4.6) if front else (-2.6, 4.2)
    boxes = [(-2.2, 10.2, z0, 4.4, 1.8, d, 'paw'),
             (-1.8, 9.2, z0 + d - 2.4, 3.6, 1.2, 2.4, 'paw')]      # the heel, rising to the leg
    for x in (-2.0, -0.95, 0.05, 1.1):
        boxes.append((x, 10.6, z0 - 0.9, 0.9, 1.4, 1.2, 'toe'))
    return boxes


FORE = [(-2.5, 0, -2.5, 5, 7, 5, 'fur'), (-2, 7, -2, 4, 4, 4, 'fur')] + paw(True)
HIND = [(-3, -3, -3, 6, 9, 6, 'fur'), (-2, 6, -1.5, 4, 5, 4, 'fur')] + paw(False)
PARTS = [
    ('body', 'root', (0, 0, 0), (0, 0, 0), [
        (-6.5, 2, -18, 13, 11, 12, 'fur'),        # the chest and shoulders, deep
        (-5.5, 1.2, -16, 11, 1, 8, 'fur'),        # the shoulders' rise, slight
        (-5.5, 3, -6, 11, 9, 12, 'fur'),          # the waist, drawn in
        (-6, 2.5, 6, 12, 9.5, 12, 'fur'),         # the haunches
    ], False),
    ('neck', 'root', (0, 5, -17), (0.15, 0, 0), [(-4.5, -3.5, -5, 9, 8, 6, 'fur')], False),
    ('head', 'root', (0, 4.5, -21), (0, 0, 0), [
        (-5.5, -4.5, -8, 11, 8, 8, 'skull'),       # the head: broad, its face flat
        (-3.5, 0, -11, 7, 3.5, 3, 'muzzle'),       # the upper muzzle, its lip drawn back
        (-3.2, 2.5, -7.8, 6.4, 5.5, 0.6, 'throat'),  # the throat, dark, at the back of the mouth
        (-2.9, 3.5, -10.8, 1, 3.2, 1, 'fang'),     # the upper fangs, long
        (1.9, 3.5, -10.8, 1, 3.2, 1, 'fang'),
        (-1.6, 3.5, -10.8, 3.2, 0.8, 0.6, 'fang'), # the teeth between
        (-5, -6.5, -3, 3, 2.5, 1.5, 'ear'),        # the ears, round, laid back
        (2, -6.5, -3, 3, 2.5, 1.5, 'ear'),
    ], False),
    # the cheek fur: thin tufts low along the jaw, sweeping back and out to points
    ('right_ruff', 'head', (-5.3, 1.5, -6.5), (0, 0.45, 0.15), [
        (-1, -1.5, 0, 1, 4, 5, 'ruff'),
        (-1.6, 0.5, 4, 1, 2.5, 2.5, 'ruff')], False),
    ('left_ruff', 'head', (5.3, 1.5, -6.5), (0, -0.45, -0.15), [
        (0, -1.5, 0, 1, 4, 5, 'ruff'),
        (0.6, 0.5, 4, 1, 2.5, 2.5, 'ruff')], True),
    ('jaw', 'head', (0, 3.5, -7.5), (0.75, 0, 0), [  # the jaw, dropped wide in a roar
        (-3, 0, -3.5, 6, 1.8, 4, 'jaw'),
        (-2.2, 0.4, -3, 4.4, 0.6, 3, 'tongue'),     # the tongue
        (-2.6, -2.2, -3.3, 0.9, 2.2, 0.9, 'fang'),  # the lower fangs
        (1.7, -2.2, -3.3, 0.9, 2.2, 0.9, 'fang'),
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
    for k, cx in enumerate((-1.55, -0.5, 0.5, 1.55)):
        base = '%s_claw%d' % (leg.split('_')[0], k)
        PARTS.append((base, leg, (cx, 11.4, -3.8), (0.45, 0, 0), [(-0.22, -0.22, -1.1, 0.44, 0.44, 1.1, 'claw')], False))
        PARTS.append((base + '_tip', base, (0, 0, -1.1), (1.0, 0, 0), [(-0.16, -0.16, -0.8, 0.32, 0.32, 0.8, 'clawtip')], False))

# ── the body larger about the head: every part but the head (and its jaw) scaled by BODY,
# from the ground up, so the feet stay on it; the head keeps its size, set on the larger neck ──
BODY = 1.35
HEADS = ('head', 'jaw', 'right_ruff', 'left_ruff')


def grow(parts):
    out = []
    for name, parent, (px_, py_, pz_), rot, boxes, mirror in parts:
        if name in HEADS:
            if parent == 'root':
                px_, py_, pz_ = px_ * BODY, 24 - (24 - py_) * BODY, pz_ * BODY
            out.append((name, parent, (px_, py_, pz_), rot, boxes, mirror))
            continue
        if parent == 'root':
            pivot = (px_ * BODY, 24 - (24 - py_) * BODY, pz_ * BODY)
        else:
            pivot = (px_ * BODY, py_ * BODY, pz_ * BODY)
        boxes = [tuple(v * BODY for v in b[:6]) + (b[6],) for b in boxes]
        out.append((name, parent, pivot, rot, boxes, mirror))
    return out


PARTS = grow(PARTS)

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
glow = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))   # the eyes alone: drawn full-bright, so they glow
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


CYAN = (40, 236, 255)


def eye(fx, fy, inward):
    """A tiger's eye, glaring, from its outer corner (fx, fy), opening toward `inward`: small and
    sunk in black, the lid falling toward the inner corner and on into a tear line; the iris a
    piercing cyan about a dark slit (it glows); white fur above, a dark brow stripe slanting down."""
    X = lambda i: fx + i * inward
    top = [1, 1, 1, 2, 2]
    bottom = [2, 2, 3, 3, 3]
    for i in range(-1, 6):                   # a thin dark rim hugging the eye
        t_ = top[min(max(i, 0), 4)]
        b_ = bottom[min(max(i, 0), 4)]
        px((X(i), fy + t_ - 1), STRIPE)
        px((X(i), fy + b_ + 1), STRIPE)
    px((X(-1), fy + 1), STRIPE)
    px((X(-1), fy + 2), STRIPE)
    px((X(5), fy + 2), STRIPE)
    px((X(5), fy + 3), STRIPE)
    for i in range(6):                       # white fur above the black, then the brow stripe
        px((X(i), fy - 2), (252, 252, 250))
        px((X(i), fy - 3), (252, 252, 250))
    for i in range(7):                       # the brow stripe above, slanting down toward the nose
        px((X(i), fy - 4 + i // 3), STRIPE)
    for i in range(5):
        for j in range(top[i], bottom[i] + 1):
            c = CYAN if j > top[i] else [v * 0.7 for v in CYAN]
            px((X(i), fy + j), c)
            glow.putpixel((int(X(i)), int(fy + j)), tuple(int(v) for v in c[:3]) + (255,))
    for j in (top[2], top[2] + 1):           # the pupil, a dark slit
        px((X(2), fy + j), (8, 10, 14))
        glow.putpixel((int(X(2)), int(fy + j)), (0, 0, 0, 0))
    for j in range(4, 10):                   # the tear line, down from the inner corner
        px((X(5) + (j - 4) // 3 * inward, fy + j), STRIPE)


for (w, h, d, style), (u, v) in slots.items():
    f = faces(u, v, w, h, d)
    if style in ('fur', 'skull', 'ruff', 'tail', 'tailtip', 'muzzle', 'jaw', 'brow'):
        shaded(f)
    if style == 'brow':
        face(f['bottom'], STRIPE, 1.2, noise=3)
        bx, by, bw, bh = f['front']
        for i in range(bw):                 # its front: white, a dark line along its lower edge
            px((bx + i, by + bh - 1), STRIPE)
        stripes(f['top'], 4, vertical=False, reach=0.6)
    if style == 'throat':
        for r_ in f.values():
            face(r_, (30, 8, 12), 1.0, noise=3)
    if style == 'tongue':
        for r_ in f.values():
            face(r_, (190, 70, 84), 1.0, noise=6, fall=0.3)
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
        fx, fy, fw, fh = f['front']               # 22 x 16: the face
        eye(fx + 3, fy + 6, 1)
        eye(fx + fw - 4, fy + 6, -1)
        for i in range(fw // 2 - 3, fw // 2 + 3):  # the forehead's stripes, converging between the eyes
            for j in range(0, 6 - abs(i - fw // 2)):
                if (i - fw // 2) % 2 == 0:
                    px((fx + i, fy + j), STRIPE)
        for j in range(8, fh):                    # the cheeks' stripes under the eyes
            if j % 3:
                px((fx + 1, fy + j), STRIPE)
                px((fx + fw - 2, fy + j), STRIPE)
    if style == 'ruff':
        face(f['front'], WHITE, 1.05, fall=0.1)
    if style == 'muzzle':
        fx, fy, fw, fh = f['front']               # 14 x 7
        for j in range(3):                        # the nose: broad, pink, dark-edged
            for i in range(fw // 2 - 4 + j, fw // 2 + 4 - j):
                px((fx + i, fy + j), NOSE if j < 2 else [c * 0.6 for c in NOSE])
        for j in range(3, fh - 2):
            px((fx + fw // 2 - 1, fy + j), STRIPE)
            px((fx + fw // 2, fy + j), STRIPE)
        for i in range(fw):                       # the lip, drawn back from the teeth: dark, red beneath
            px((fx + i, fy + fh - 2), STRIPE)
            px((fx + i, fy + fh - 1), MOUTH)
        for s_ in ('right', 'left'):               # whisker rows
            rx, ry, rw, rh = f[s_]
            for k in range(0, rw, 2):
                px((rx + k, ry + 2), STRIPE)
                px((rx + k, ry + 4), STRIPE)
        face(f['bottom'], MOUTH, 0.7, noise=4)    # the roof of the open mouth
    if style == 'jaw':
        face(f['top'], MOUTH, 0.75, noise=4)
        jx, jy, jw, jh = f['front']
        for i in range(jw):
            px((jx + i, jy), STRIPE)
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
        shaded(f)
        face(f['bottom'], PAD, 1.0, noise=4)
        stripes(f['top'], 5, vertical=False, reach=0.5)
    if style == 'toe':
        shaded(f)
        face(f['bottom'], PAD, 1.0, noise=4)
        for k in ('right', 'left'):              # the parting between toes, in shadow
            face(f[k], WHITE, 0.62, fall=0.3)
    if style == 'ear':
        face(f['front'], (226, 214, 214), 0.95)
        for s in ('back', 'top', 'right', 'left', 'bottom'):
            face(f[s], STRIPE, 1.2, noise=4)
        bx, by, bw, bh = f['back']
        for i in range(bw // 2 - 1, bw // 2 + 1):  # the white spot on a tiger's ear
            for j in range(bh // 2 - 1, bh // 2 + 1):
                px((bx + i, by + j), (250, 250, 248))

img.save('src/main/resources/assets/jttw_engine/textures/entity/white_tiger.png')
glow.save('src/main/resources/assets/jttw_engine/textures/entity/white_tiger_eyes.png')

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
