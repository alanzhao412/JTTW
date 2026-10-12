"""Shared machinery for the divine beasts' true forms (dragon.py, black_tortoise.py,
vermilion_bird.py): one list of parts gives both the model (written into the renderer's layer()
between its GENERATED markers) and the skin, painted to the same box-UV layout at twice the model's
resolution, and a second, glowing skin (drawn full-bright, as a spider's eyes are).

A part is (name, parent, pivot (x, y, z), rotation (x, y, z) radians, boxes, mirror); a box is
(x, y, z, w, h, d, style), in model pixels with the ground at y 24 and the head toward -z. A
painter is called once per distinct (w, h, d, style) with that box's faces (texture pixels).
"""
import math
import random
import re
from PIL import Image

UNITS = 256          # the model's texture size, in model pixels
S = 2                # texture pixels per model pixel
TEX = UNITS * S


def size(b):
    return tuple(max(1, math.ceil(v)) for v in b[3:6])


def layout(parts):
    """Each distinct box's six faces, packed into the texture: {(w, h, d, style): (u, v)}."""
    slots = {}
    for _, _, _, _, boxes, _ in parts:
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
    return slots


def faces(u, v, w, h, d):
    f = {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
         'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
    return {k: tuple(c * S for c in r) for k, r in f.items()}


class Painter:
    """Draws onto the skin (and, with glow=True, onto the glowing skin too)."""

    def __init__(self, seed):
        self.img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
        self.glow = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
        self.rnd = random.Random(seed)

    def px(self, p, c, glow=False):
        x, y = int(p[0]), int(p[1])
        if 0 <= x < TEX and 0 <= y < TEX:
            col = tuple(max(0, min(255, int(v))) for v in c[:3]) + (255,)
            self.img.putpixel((x, y), col)
            if glow:
                self.glow.putpixel((x, y), col)

    def face(self, rect, base, light=1.0, noise=5, fall=0.0, glow=False):
        """A face in one tone, lit `light`, darkening by `fall` toward its bottom."""
        fx, fy, fw, fh = rect
        for j in range(fh):
            for i in range(fw):
                k = light * (1 - fall * (j / max(1, fh - 1)))
                n = self.rnd.randint(-noise, noise)
                self.px((fx + i, fy + j), [c * k + n for c in base], glow)

    def gradient(self, rect, top, bottom, noise=4, glow=False, horizontal=False):
        """A face blending from one colour to another, down it (or across it)."""
        fx, fy, fw, fh = rect
        for j in range(fh):
            for i in range(fw):
                t = (i / max(1, fw - 1)) if horizontal else (j / max(1, fh - 1))
                n = self.rnd.randint(-noise, noise)
                self.px((fx + i, fy + j), [a + (b - a) * t + n for a, b in zip(top, bottom)], glow)

    def shaded(self, f, base, belly=None, light=1.0):
        """A box shaded for its form: lit above, darker down its sides, darkest beneath."""
        self.face(f['top'], base, 1.08 * light)
        self.face(f['bottom'], belly or base, 0.7 * light)
        for s in ('right', 'left'):
            self.face(f[s], base, 0.95 * light, fall=0.3)
        self.face(f['front'], base, 1.0 * light, fall=0.2)
        self.face(f['back'], base, 0.88 * light, fall=0.3)

    def scales(self, rect, base, cell=4, edge=0.62, light=1.0):
        """Overlapping scales over a face: each a rounded plate, lit at its crown, dark at its lower edge."""
        fx, fy, fw, fh = rect
        for j in range(fh):
            row = j // (cell // 2 + 1)
            off = (cell // 2) if row % 2 else 0
            for i in range(fw):
                cx = (i + off) % cell
                cy = j % (cell // 2 + 1)
                r = abs(cx - (cell - 1) / 2) / (cell / 2)
                k = light * (1.12 - 0.35 * (cy / (cell // 2 + 1)) - 0.15 * r)
                if cy == cell // 2 or (cy == cell // 2 - 1 and r > 0.7):
                    k *= edge
                n = self.rnd.randint(-3, 3)
                self.px((fx + i, fy + j), [c * k + n for c in base])

    def stripes(self, rect, colour, spacing, vertical=True, reach=0.7):
        """Tapering stripes, a little wavy, spaced along the face."""
        fx, fy, fw, fh = rect
        length, across = (fw, fh) if vertical else (fh, fw)
        pos = self.rnd.randint(1, max(1, spacing))
        while pos < length - 2:
            r = max(2, int(across * (reach + self.rnd.uniform(-0.15, 0.12))))
            from_top = self.rnd.random() < 0.7
            for t in range(r):
                thick = 3 if t < r * 0.35 else 2 if t < r * 0.7 else 1
                wob = round(1.2 * math.sin(t * 0.45 + pos))
                for k in range(thick):
                    a, b2 = pos + k + wob, (t if from_top else across - 1 - t)
                    p = (fx + a, fy + b2) if vertical else (fx + b2, fy + a)
                    if fx <= p[0] < fx + fw and fy <= p[1] < fy + fh:
                        self.px(p, colour)
            pos += spacing + self.rnd.randint(-2, 2)

    def eye(self, fx, fy, inward, iris, w=5, slit=True, rise=False):
        """A narrow, glaring eye `w` wide from its outer corner, opening toward `inward`: the lid falling
        toward the inner corner, a thin dark rim, the iris glowing about a dark slit. With `rise`, it
        slopes the other way along `inward` (for a face whose texture runs the other way about)."""
        X = lambda i: fx + i * inward
        top = [1] * (w // 2 + 1) + [2] * (w - w // 2 - 1)
        bottom = [2] * (w // 2) + [3] * (w - w // 2)
        if rise:
            top, bottom = top[::-1], bottom[::-1]
        dark = (14, 12, 16)
        for i in range(-1, w + 1):
            t_ = top[min(max(i, 0), w - 1)]
            b_ = bottom[min(max(i, 0), w - 1)]
            self.px((X(i), fy + t_ - 1), dark)
            self.px((X(i), fy + b_ + 1), dark)
        for i in range(w):
            for j in range(top[i], bottom[i] + 1):
                c = iris if j > top[i] else [v * 0.72 for v in iris]
                self.px((X(i), fy + j), c, glow=True)
        if slit:
            m = w // 2
            self.px((X(m), fy + top[m] + 1), (8, 8, 12))
            self.glow.putpixel((int(X(m)), int(fy + top[m] + 1)), (0, 0, 0, 0))

    def save(self, name):
        self.img.save('src/main/resources/assets/jttw_engine/textures/entity/%s.png' % name)
        self.glow.save('src/main/resources/assets/jttw_engine/textures/entity/%s_glow.png' % name)


def paint(parts, painter, paint_box):
    slots = layout(parts)
    for (w, h, d, style), (u, v) in slots.items():
        paint_box(painter, style, faces(u, v, w, h, d), (w, h, d))
    return slots


def num(v):
    s = '%g' % round(v, 4)
    return s + ('f' if ('.' in s or 'e' in s) else '')


def camel(n):
    return re.sub(r'_(\w)', lambda m: m.group(1).upper(), n)


def emit(parts, slots, java):
    """Writes the model's code into the renderer, between its GENERATED markers."""
    lines = ['        MeshDefinition mesh = new MeshDefinition();', '        PartDefinition root = mesh.getRoot();']
    for name, parent, (px_, py_, pz_), (rx, ry, rz), boxes, mirror in parts:
        cubes = 'CubeListBuilder.create()'
        for b in boxes:
            u, v = slots[size(b) + (b[6],)]
            cubes += '\n                .texOffs(%d, %d).mirror(%s).addBox(%s, %s, %s, %s, %s, %s)' % (
                u, v, 'true' if mirror else 'false', num(b[0]), num(b[1]), num(b[2]), num(b[3]), num(b[4]), num(b[5]))
        pose = 'PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)' % tuple(num(c) for c in (px_, py_, pz_, rx, ry, rz))
        lines.append('        PartDefinition %s = %s.addOrReplaceChild("%s", %s, %s);' % (
            camel(name), 'root' if parent == 'root' else camel(parent), name, cubes, pose))
    lines.append('        return LayerDefinition.create(mesh, %d, %d);' % (UNITS, UNITS))
    s = open(java, encoding='utf-8').read()
    start, end = '        // GENERATED: begin', '        // GENERATED: end'
    a = s.index(start) + len(start)
    b = s.index(end)
    open(java, 'w', encoding='utf-8').write(s[:a] + '\n' + '\n'.join(lines) + '\n' + s[b:])


def segment(p, q):
    """A part's rotation (x, y) laying its +z along from point p to point q, and the length."""
    dx, dy, dz = q[0] - p[0], q[1] - p[1], q[2] - p[2]
    length = math.sqrt(dx * dx + dy * dy + dz * dz)
    return math.asin(max(-1, min(1, -dy / length))), math.atan2(dx, dz), length
