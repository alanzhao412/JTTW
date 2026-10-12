"""The White Tiger, Byakko's true form: its model and its skin from one list of parts.

Writes the body of WhiteTigerRenderer.layer() (between its GENERATED markers) and paints
textures/entity/white_tiger.png to the same box-UV layout, so the two never drift apart.

The shape is a predator's: massive raised shoulders and forequarters, the back falling away to
lean haunches; the head carried low and thrust forward on a thick neck, under a heavy brow, cheek
ruffs flaring; the jaw a little open on its fangs; ears laid back; forelegs thicker than the hind,
on broad clawed paws; a long, low tail. Every box is shaded for its form: lit above, darker down
its sides, darkest beneath.
"""
import random
import re
from PIL import Image

rnd = random.Random(19)
TEX = 256

# ── the parts ────────────────────────────────────────────────────────────────
# (name, parent, pivot (x, y, z), rotation (x, y, z) radians, boxes, mirror)
# each box: (x, y, z, w, h, d, style); pixels, ground at y 24, the head toward -z
FORE = [(-3, 0, -3, 6, 7, 6, 'fur'), (-2.5, 7, -2.5, 5, 5, 5, 'fur'), (-3.5, 11, -4.5, 7, 2, 7, 'paw'),
        (-3, 12, -5.5, 1, 1, 1, 'claw'), (-1, 12, -5.5, 1, 1, 1, 'claw'), (1, 12, -5.5, 1, 1, 1, 'claw'), (2.5, 12, -5.5, 1, 1, 1, 'claw')]
HIND = [(-3, -2, -3.5, 6, 8, 7, 'fur'), (-2, 6, -1, 4, 6, 4, 'fur'), (-3, 11, -3.5, 6, 2, 6, 'paw')]
PARTS = [
    ('body', 'root', (0, 0, 0), (0, 0, 0), [
        (-7.5, -2, -19, 15, 13, 13, 'fur'),       # the forequarters: deep chest, broad shoulders
        (-7, -5, -17, 5, 3, 8, 'fur'),            # the shoulder blades, standing up as it prowls
        (2, -5, -17, 5, 3, 8, 'fur'),
        (-5.5, 0, -6, 11, 10, 11, 'fur'),         # the waist, drawn in
        (-6.5, -1, 5, 13, 11, 11, 'fur'),         # the haunches
    ], False),
    ('neck', 'root', (0, 2, -18), (0.45, 0, 0), [(-5, -1, -7, 10, 10, 8, 'fur')], False),
    ('head', 'root', (0, 7, -24), (0.08, 0, 0), [
        (-5.5, -3, -8, 11, 8, 8, 'skull'),        # the skull, broad
        (-5, -3.5, -8.8, 10, 1.5, 1.5, 'brow'),   # the brow ridge, a lip of bone over the eyes
        (-8, -1, -6, 2.5, 7, 6, 'ruff'),          # the cheek ruffs, flaring
        (5.5, -1, -6, 2.5, 7, 6, 'ruff'),
        (-2.5, 1.5, -11.5, 5, 3.5, 3.5, 'muzzle'),  # the muzzle, below the eyes
        (-2.2, 5, -11.2, 1, 2.5, 1, 'fang'),      # the upper fangs, bared
        (1.2, 5, -11.2, 1, 2.5, 1, 'fang'),
        (-5, -5, -3, 2.5, 2.5, 1, 'ear'),         # the ears, laid back
        (2.5, -5, -3, 2.5, 2.5, 1, 'ear'),
    ], False),
    ('jaw', 'head', (0, 5, -8), (0.32, 0, 0), [
        (-2.5, 0, -3.5, 5, 1.5, 4, 'jaw'),
        (-2, -1.4, -3.3, 1, 1.4, 1, 'fang'),      # the lower fangs
        (1, -1.4, -3.3, 1, 1.4, 1, 'fang'),
    ], False),
    ('right_front_leg', 'root', (-4.5, 11, -13), (0, 0, 0), FORE, False),
    ('left_front_leg', 'root', (4.5, 11, -13), (0, 0, 0), FORE, True),
    ('right_hind_leg', 'root', (-4, 11, 11), (0, 0, 0), HIND, False),
    ('left_hind_leg', 'root', (4, 11, 11), (0, 0, 0), HIND, True),
    ('tail', 'root', (0, 2, 16), (0.55, 0, 0), [(-1.25, -1.25, 0, 2.5, 2.5, 11, 'tail')], False),
    ('tail_end', 'tail', (0, 0, 11), (-0.5, 0, 0), [(-1, -1, 0, 2, 2, 10, 'tailtip')], False),
]

# ── pack each distinct box's six faces into the texture ─────────────────────
def size(b):
    import math
    w, h, d = (max(1, math.ceil(v)) for v in b[3:6])
    return w, h, d


slots = {}  # (w, h, d, style) -> (u, v)
shapes = []
for _, _, _, _, boxes, _ in PARTS:
    for b in boxes:
        w, h, d = size(b)
        key = (w, h, d, b[6])
        if key not in slots:
            slots[key] = None
            shapes.append(key)
shapes.sort(key=lambda k: -(k[2] + k[1]))
x = y = row = 0
for key in shapes:
    w, h, d, _ = key
    rw, rh = 2 * d + 2 * w, d + h
    if x + rw > TEX:
        x, y, row = 0, y + row + 1, 0
    slots[key] = (x, y)
    x += rw + 1
    row = max(row, rh)
assert y + row <= TEX, 'texture too small'

# ── paint ───────────────────────────────────────────────────────────────────
img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
WHITE, BELLY, STRIPE = (238, 236, 230), (214, 208, 196), (30, 28, 34)
EYE, NOSE, MOUTH = (132, 214, 250), (176, 112, 116), (110, 34, 40)
IVORY, CLAW, PAD = (236, 228, 204), (52, 50, 56), (64, 56, 60)


def px(p, c):
    if 0 <= p[0] < TEX and 0 <= p[1] < TEX:
        img.putpixel(p, tuple(max(0, min(255, int(v))) for v in c[:3]) + (255,))


def face(rect, base, light, noise=5, fall=0.0):
    """A face in one tone, lit `light`, falling off `fall` toward its bottom (its form's shadow)."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            k = light * (1 - fall * (j / max(1, fh - 1)))
            n = rnd.randint(-noise, noise)
            px((fx + i, fy + j), [c * k + n for c in base])


def stripes(rect, spacing, vertical=True, reach=0.7):
    """Dark stripes: tapering, a little wavy, spaced along the face."""
    fx, fy, fw, fh = rect
    length, across = (fw, fh) if vertical else (fh, fw)
    pos = rnd.randint(1, max(1, spacing))
    while pos < length - 1:
        r = max(1, int(across * (reach + rnd.uniform(-0.15, 0.15))))
        from_top = rnd.random() < 0.7
        for t in range(r):
            thick = 2 if t < r * 0.45 else 1
            wob = round(0.8 * __import__('math').sin(t * 0.9 + pos))
            for k in range(thick):
                a, b2 = pos + k + wob, (t if from_top else across - 1 - t)
                p = (fx + a, fy + b2) if vertical else (fx + b2, fy + a)
                if fx <= p[0] < fx + fw and fy <= p[1] < fy + fh:
                    px(p, [c * rnd.uniform(0.9, 1.1) for c in STRIPE])
        pos += spacing + rnd.randint(-1, 1)


def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


for (w, h, d, style), (u, v) in slots.items():
    f = faces(u, v, w, h, d)
    if style in ('fur', 'skull', 'brow', 'ruff', 'tail', 'tailtip', 'muzzle', 'jaw'):
        face(f['top'], WHITE, 1.06)
        face(f['bottom'], BELLY, 0.72)
        for s in ('right', 'left'):
            face(f[s], WHITE, 0.94, fall=0.32)
        face(f['front'], WHITE, 1.0, fall=0.22)
        face(f['back'], WHITE, 0.9, fall=0.3)
        if style in ('fur', 'ruff', 'tail'):
            for s in ('right', 'left'):
                stripes(f[s], 3 if style != 'ruff' else 2, vertical=True, reach=0.75)
            stripes(f['top'], 3, vertical=False, reach=0.85)
            if style == 'fur':
                stripes(f['front'], 4, vertical=True, reach=0.5)
                stripes(f['back'], 4, vertical=True, reach=0.5)
        if style == 'tailtip':
            for s in ('right', 'left', 'top', 'bottom'):
                stripes(f[s], 3, vertical=s in ('top', 'bottom'), reach=1.0)
            face(f['back'], STRIPE, 1.0, noise=3)
        if style == 'skull':
            tx, ty, tw, th = f['top']
            for i in range(tw // 2 - 3, tw // 2 + 3):         # the mark of a king (王) on its forehead
                for j in (th - 1, th - 3, th - 5):
                    px((tx + i, ty + j), STRIPE)
            for j in range(th - 5, th):
                px((tx + tw // 2, ty + j), STRIPE)
            for j in range(0, th - 6, 2):
                px((tx + 1, ty + j), STRIPE)
                px((tx + tw - 2, ty + j), STRIPE)
            for s in ('right', 'left'):
                stripes(f[s], 3, vertical=True, reach=0.5)
            fx, fy, fw, fh = f['front']

            # deep-set ice-blue eyes, dark-rimmed, slanted
            for ex, slant in ((fx + 1, 1), (fx + fw - 4, -1)):
                # slanted, dark-rimmed, ice-blue, under the ridge: a hard stare
                for i in range(3):
                    px((ex + i, fy + 1 + (1 if (i * slant > 0 or (slant < 0 and i == 0)) else 0)), STRIPE)
                    px((ex + i, fy + 3), STRIPE)
                px((ex + 1, fy + 2), EYE)
                px((ex + (2 if slant > 0 else 0), fy + 2), [c * 0.7 for c in EYE])
            for j in range(4, fh):            # tear lines down past the muzzle
                if j % 2 == 0:
                    px((fx + 1, fy + j), STRIPE)
                    px((fx + fw - 2, fy + j), STRIPE)
        if style == 'brow':
            face(f['bottom'], STRIPE, 1.3, noise=4)   # the shadow it casts on the eyes
        if style == 'muzzle':
            fx, fy, fw, fh = f['front']
            for i in range(fw // 2 - 1, fw // 2 + 2):         # a dark, broad nose
                px((fx + i, fy), [c * 0.6 for c in NOSE])
            px((fx + fw // 2, fy + 1), [c * 0.5 for c in NOSE])
            face(f['top'], WHITE, 1.0, noise=4)
            for i in range(fw):
                px((fx + i, fy + fh - 1), MOUTH)
            face(f['bottom'], MOUTH, 0.8, noise=4)      # the roof of the open mouth
            for s in ('right', 'left'):                  # whisker spots
                rx, ry, rw, rh = f[s]
                for k in range(3):
                    px((rx + 1 + k, ry + 1 + (k % 2)), STRIPE)
        if style == 'jaw':
            face(f['top'], MOUTH, 0.9, noise=4)         # its tongue and the floor of the mouth
        if style == 'ruff':
            face(f['front'], WHITE, 1.05, fall=0.1)
    elif style == 'fang':
        for name, r in f.items():
            face(r, IVORY, 1.0 if name != 'bottom' else 0.9, noise=3)
    elif style == 'claw':
        for name, r in f.items():
            face(r, CLAW, 1.0, noise=3)
    elif style == 'paw':
        face(f['top'], WHITE, 1.0)
        for s in ('right', 'left', 'front', 'back'):
            face(f[s], WHITE, 0.88, fall=0.35)
        face(f['bottom'], PAD, 1.0, noise=4)
        stripes(f['top'], 3, vertical=False, reach=0.6)
    elif style == 'ear':
        face(f['front'], WHITE, 0.95)
        for s in ('back', 'top', 'right', 'left', 'bottom'):
            face(f[s], STRIPE, 1.2, noise=4)
        fx, fy, fw, fh = f['back']
        px((fx + fw // 2, fy + fh // 2), WHITE)     # the white spot on a tiger's ear

img.save('src/main/resources/assets/jttw_engine/textures/entity/white_tiger.png')

# ── the model's code ────────────────────────────────────────────────────────
def num(v):
    s = ('%g' % v)
    return s + ('f' if ('.' in s or 'e' in s) else '')


lines = ['        MeshDefinition mesh = new MeshDefinition();', '        PartDefinition root = mesh.getRoot();']
for name, parent, (px_, py_, pz_), (rx, ry, rz), boxes, mirror in PARTS:
    var = re.sub(r'_(\w)', lambda m: m.group(1).upper(), name)
    cubes = 'CubeListBuilder.create()'
    for b in boxes:
        w, h, d = size(b)
        u, v = slots[(w, h, d, b[6])]
        cubes += '\n                .texOffs(%d, %d).mirror(%s).addBox(%s, %s, %s, %s, %s, %s)' % (
            u, v, 'true' if mirror else 'false', num(b[0]), num(b[1]), num(b[2]), num(b[3]), num(b[4]), num(b[5]))
    pose = 'PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)' % tuple(num(c) for c in (px_, py_, pz_, rx, ry, rz))
    lines.append('        PartDefinition %s = %s.addOrReplaceChild("%s", %s, %s);' % (var, parent, name, cubes, pose))
lines.append('        return LayerDefinition.create(mesh, %d, %d);' % (TEX, TEX))
code = '\n'.join(lines)

java = 'src/main/java/com/jttw/engine/client/renderer/WhiteTigerRenderer.java'
s = open(java, encoding='utf-8').read()
start, end = '        // GENERATED by assets_source/white_tiger.py: begin', '        // GENERATED: end'
if start not in s:
    a = s.index('    public static LayerDefinition layer() {')
    b = s.index('    private static String legName(int i) {')
    s = s[:a] + '    public static LayerDefinition layer() {\n' + start + '\n' + end + '\n    }\n\n' + s[b:]
a = s.index(start) + len(start)
b = s.index(end)
s = s[:a] + '\n' + code + '\n' + s[b:]
open(java, 'w', encoding='utf-8').write(s)
print('model and skin written;', len(slots), 'distinct boxes')
