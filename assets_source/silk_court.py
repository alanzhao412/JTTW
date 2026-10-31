"""Shared machinery for Silk-Web Ridge's creatures (silk_kin.py: the spider demons and the insect sons;
spider_spirits.py: the Seven Spider Spirits; hundred_eyed.py: the Hundred-Eyed Demon Lord). As
king_yama.py: one list of parts gives the model (written into SpiderCaveRenderers between its
GENERATED markers) and the skin, painted to the same box-UV layout at twice the model's resolution,
each box's faces where the game maps them (by its true size), with a glowing skin (eyes, the silk's
sheen) drawn full-bright over it, and for the Demon Lord a third: his thousand eyes ablaze.

A part is (name, parent, pivot, rotation, boxes, mirror); a box (x, y, z, w, h, d, style), in model
pixels, the ground at y 24 and the head toward -z."""
import math
import random

import beastgen as bg
from beastgen import Painter, layout, size, emit
from PIL import Image

JAVA = 'src/main/java/com/jttw/engine/client/renderer/SpiderCaveRenderers.java'
OUT = 'src/main/resources/assets/jttw_engine/textures/entity/'

SIDES = ('front', 'back', 'right', 'left')
LIGHTS = {'top': 1.12, 'bottom': 0.6, 'front': 1.0, 'back': 0.82, 'right': 0.9, 'left': 0.9}


def part(name, parent, pivot, rot, boxes, mirror=False):
    return (name, parent, pivot, rot, boxes, mirror)


def mirrored(parts):
    """Each 'right_' part again as 'left_', mirrored across x."""
    out = []
    for name, parent, (px, py, pz), (rx, ry, rz), boxes, mirror in parts:
        out.append((name, parent, (px, py, pz), (rx, ry, rz), boxes, mirror))
        if name.startswith('right_'):
            lb = [(-x - w, y, z, w, h, d, s) for (x, y, z, w, h, d, s) in boxes]
            out.append((name.replace('right_', 'left_'), parent.replace('right_', 'left_'), (-px, py, pz), (rx, -ry, -rz), lb, True))
    return out


def exact_faces(u, v, w, h, d):
    U = lambda a: int(round(a * bg.S))

    def rect(x0, y0, x1, y1):
        return U(x0), U(y0), max(1, U(x1) - U(x0)), max(1, U(y1) - U(y0))
    return {'top': rect(u + d, v, u + d + w, v + d), 'bottom': rect(u + d + w, v, u + d + 2 * w, v + d),
            'right': rect(u, v + d, u + d, v + d + h), 'front': rect(u + d, v + d, u + d + w, v + d + h),
            'left': rect(u + d + w, v + d, u + 2 * d + w, v + d + h), 'back': rect(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}


def paint(parts, painter, paint_box):
    slots = layout(parts)
    true = {}
    for _, _, _, _, boxes, _ in parts:
        for b in boxes:
            true.setdefault(size(b) + (b[6],), b[3:6])
    for key, (u, v) in slots.items():
        w, h, d = true[key]
        paint_box(painter, key[3], exact_faces(u, v, w, h, d), (w, h, d))
    return slots


class SilkPainter(Painter):
    """A Painter with a third skin: the blaze (the Demon Lord's eyes in his glare)."""

    def __init__(self, seed):
        super().__init__(seed)
        self.blaze = Image.new('RGBA', (bg.TEX, bg.TEX), (0, 0, 0, 0))
        self.used_blaze = False

    def px(self, p, c, glow=False, a=255):
        x, y = int(p[0]), int(p[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            col = tuple(max(0, min(255, int(v))) for v in c[:3]) + (a,)
            self.img.putpixel((x, y), col)
            if glow:
                self.glow.putpixel((x, y), col[:3] + (255,))

    def lit(self, xy, colour, calm=1.0, blaze=1.0, skin=None, hot=None):
        """A glowing pixel: on the skin `skin` (or the colour), on the glow the colour at `calm`, ablaze the colour (or `hot`) at `blaze`."""
        x, y = int(xy[0]), int(xy[1])
        if not (0 <= x < bg.TEX and 0 <= y < bg.TEX):
            return
        self.px(xy, skin if skin is not None else colour)
        k = lambda c, f: tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)
        if calm:
            self.glow.putpixel((x, y), k(colour, calm))
        if blaze:
            self.used_blaze = True
            self.blaze.putpixel((x, y), k(hot or colour, blaze))

    def clear(self, xy):
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            for im in (self.img, self.glow, self.blaze):
                im.putpixel((x, y), (0, 0, 0, 0))

    def save(self, name, glow=True):
        self.img.save(OUT + name + '.png')
        if glow:
            self.glow.save(OUT + name + '_glow.png')
        if self.used_blaze:
            self.blaze.save(OUT + name + '_blaze_glow.png')


# ── Colour helpers ───────────────────────────────────────────────────────────

def k(c, f):
    return [v * f for v in c[:3]]


def mixc(a, b, t):
    return [x + (y - x) * t for x, y in zip(a, b)]


def noisy(p, c, n=5):
    return [v + p.rnd.randint(-n, n) for v in c]


def each(rect):
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            yield i, j, (fx + i, fy + j)


# ── Materials ────────────────────────────────────────────────────────────────

def chitin(p, f, base, sheen=None, bands=0, band=None, light=1.0, gloss=True, plate=0):
    """Glossy chitin: a deep base lit toward its upper edge, a hard specular streak along it, darker
    at its edges; `bands` stripes of `band` across its length (a wasp's), `plate` segment seams."""
    sheen = sheen or mixc(base, (255, 250, 240), 0.55)
    for s, r in f.items():
        fx, fy, fw, fh = r
        L = LIGHTS[s] * light
        for i, j, xy in each(r):
            u, v = (i + 0.5) / fw, (j + 0.5) / fh
            edge = min(u, 1 - u, v, 1 - v)
            c = base
            if bands and band is not None:
                along = v if s in SIDES else u
                if int(along * bands * 2) % 2 == 1:
                    c = band
            t = L * (0.78 + 0.5 * min(1, edge * 4)) * (1.08 - 0.25 * v if s in SIDES else 1)
            col = k(c, t)
            if gloss and s in ('top', 'front', 'right', 'left') and abs(u - 0.32) < 0.07 + 0.03 * math.sin(v * 9):
                col = mixc(col, sheen, 0.6 if s == 'top' else 0.35)
            if plate and s in SIDES and (j % plate) == plate - 1:
                col = k(col, 0.55)
            p.px(xy, noisy(p, col, 4))


def fuzz(p, f, base, tip=None, light=1.0, density=0.5):
    """Fur or bristle: a base with light tips scattered, darker roots."""
    tip = tip or mixc(base, (255, 240, 200), 0.45)
    for s, r in f.items():
        L = LIGHTS[s] * light
        for i, j, xy in each(r):
            c = tip if p.rnd.random() < density * 0.4 else base if p.rnd.random() < 0.8 else k(base, 0.6)
            p.px(xy, noisy(p, k(c, L * (0.9 + 0.2 * p.rnd.random())), 6))


def cloth(p, f, base, light=1.0, folds=True, fall=0.25):
    """Silk or cotton: lit above, a fall down its sides, soft folds."""
    for s, r in f.items():
        fx, fy, fw, fh = r
        L = LIGHTS[s] * light
        for i, j, xy in each(r):
            t = L * (1 - fall * (j / max(1, fh - 1)) if s in SIDES else L)
            if folds and s in SIDES:
                t *= 0.9 + 0.1 * math.cos((i + 0.5) / max(2, fw) * math.pi * max(1, fw // 3))
            p.px(xy, noisy(p, k(base, t), 3))


def sheen_silk(p, rect, base, sheen, step=5):
    """Silk catching the light: a soft highlight running down every few columns."""
    fx, fy, fw, fh = rect
    for i, j, xy in each(rect):
        w = 0.5 + 0.5 * math.cos((i + 0.3 * j) / step * 2 * math.pi)
        p.px(xy, noisy(p, mixc(base, sheen, 0.35 * w ** 3), 2))


def web_motif(p, rect, colour, cx=None, cy=None, r=None, spokes=8):
    """A spider's web worked over a face: spokes from its hub, rings round it."""
    fx, fy, fw, fh = rect
    cx = fw / 2 if cx is None else cx
    cy = fh * 0.4 if cy is None else cy
    r = r or max(fw, fh) * 0.6
    for i, j, xy in each(rect):
        dx, dy = i + 0.5 - cx, j + 0.5 - cy
        d = math.hypot(dx, dy)
        if d > r or d < 0.5:
            continue
        a = math.atan2(dy, dx)
        spoke = abs(math.remainder(a, 2 * math.pi / spokes)) * d
        ring = abs(math.remainder(d, r / 4))
        if spoke < 0.55 or ring < 0.4:
            p.px(xy, colour)


def edges(p, f, colour, sides=SIDES, top=True, bottom=True, width=1):
    for s in sides:
        fx, fy, fw, fh = f[s]
        for i in range(fw):
            for w in range(width):
                if top:
                    p.px((fx + i, fy + w), colour)
                if bottom:
                    p.px((fx + i, fy + fh - 1 - w), colour)


def vedges(p, rect, colour, width=1):
    fx, fy, fw, fh = rect
    for j in range(fh):
        for w in range(width):
            p.px((fx + w, fy + j), colour)
            p.px((fx + fw - 1 - w, fy + j), colour)


def gold(p, f, base=(226, 178, 66), light=1.0, glint=True):
    for s, r in f.items():
        L = LIGHTS[s] * light
        fx, fy, fw, fh = r
        for i, j, xy in each(r):
            t = L * (1.15 - 0.4 * (j / max(1, fh - 1)))
            c = k(base, t)
            if glint and (i + j) % 7 == 0:
                c = mixc(c, (255, 245, 200), 0.6)
            p.px(xy, noisy(p, c, 3))


def compound_eye(p, f, iris, dark=(10, 8, 6), glow=0.55, facets=2):
    """A compound eye: a dome of facets, each lit at its crown, the whole glowing faintly."""
    for s, r in f.items():
        for i, j, xy in each(r):
            cell = ((i // facets) + (j // facets)) % 2
            lit = (i % facets == 0 and j % facets == 0)
            c = mixc(iris, (255, 255, 240), 0.35) if lit else k(iris, 0.85 if cell else 0.65)
            if s == 'back':
                c = dark
            p.px(xy, noisy(p, c, 4), glow=glow > 0 and s != 'back')


def gem_eye(p, x, y, iris, w=3, h=2, pupil=True, blaze=None, calm=0.9):
    """A small glowing eye painted at (x, y): w x h of iris round a dark slit; ablaze brighter (the SilkPainter's blaze)."""
    for j in range(h):
        for i in range(w):
            c = iris if (i, j) != (w // 2, h // 2) or not pupil else (12, 8, 6)
            if pupil and i == w // 2:
                p.px((x + i, y + j), (14, 8, 6))
                continue
            if hasattr(p, 'lit'):
                p.lit((x + i, y + j), c, calm=calm, blaze=1.0 if blaze is None else blaze, hot=mixc(c, (255, 250, 220), 0.5))
            else:
                p.px((x + i, y + j), c, glow=True)


def cut_point(p, rect, keep=0.0, down=True, sharp=0.8):
    """Cuts a face to a point (transparent outside): toward its bottom (or top)."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        t = (fh - 1 - j) if down else j
        half = max(keep, (t + 1) * sharp)
        for i in range(fw):
            if abs(i - fw / 2 + 0.5) > half:
                p.clear((fx + i, fy + j))


def extent(parts, k_):
    """How far the model reaches, drawn at `k_` times its pixels, in blocks (height, breadth, ahead, behind)."""
    def mul(a, b):
        return [[sum(a[i][t] * b[t][j] for t in range(3)) for j in range(3)] for i in range(3)]

    def apply(m, v):
        return [sum(m[i][t] * v[t] for t in range(3)) for i in range(3)]

    def rot(rx, ry, rz):
        cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
        x = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
        y = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
        z = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
        return mul(mul(z, y), x)

    frames = {}
    lo, hi = [1e9] * 3, [-1e9] * 3
    eye = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    for name, parent, pivot, r, boxes, _ in parts:
        pm, pt = frames.get(parent, (eye, [0, 0, 0]))
        m = mul(pm, rot(*r))
        t = [a + b for a, b in zip(pt, apply(pm, pivot))]
        frames[name] = (m, t)
        for b in boxes:
            for cx_ in (b[0], b[0] + b[3]):
                for cy_ in (b[1], b[1] + b[4]):
                    for cz_ in (b[2], b[2] + b[5]):
                        q = [a + c for a, c in zip(t, apply(m, (cx_, cy_, cz_)))]
                        lo = [min(a, c) for a, c in zip(lo, q)]
                        hi = [max(a, c) for a, c in zip(hi, q)]
    return {'height': (24 - lo[1]) * k_ / 16, 'feet_to_top': (hi[1] - lo[1]) * k_ / 16, 'breadth': (hi[0] - lo[0]) * k_ / 16,
            'ahead': -lo[2] * k_ / 16, 'behind': hi[2] * k_ / 16, 'lowest': hi[1]}
