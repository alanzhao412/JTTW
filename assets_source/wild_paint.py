"""What the open world's yaoguai (goblin.py, kobold.py, wolfman.py, ogre.py) share: as king_yama.py,
each one's model and skin come from one list of parts (beastgen): the model is written into
WildRenderers between its GENERATED markers (``// GENERATED <kind>: begin`` and ``end``), the skin
painted to the same box-UV layout at twice the model's resolution (each face where the game looks for
it, by the box's true size: king_yama.paint), and a second, glowing skin (eyes, a candle, a lantern)
drawn full-bright over it.

Here: a Painter that can cut (clear) and light, and the brushes the four share: hide and skin
mottled and warty, fur in strands, scales, cloth woven and folded and patched and torn, leather,
iron dented and rusted and riveted, wood grained, rope twisted, lamellae laced, teeth, and eyes.

Faces are named as beastgen names them: 'front' looks forward (-z), 'right' out of the creature's
right side (-x), 'top' up. On a side face, texture rows run down; on the front, texture columns run
from the creature's right to its left (as a viewer before it sees it)."""
import math
import random

import beastgen as bg
from beastgen import Painter as _Painter, emit  # noqa: F401
from fire_court import part, mirrored          # noqa: F401  (re-exported for the four)
from sea_court import aim                       # noqa: F401
from king_yama import paint, exact_faces        # noqa: F401
from PIL import Image

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/WildRenderers.java'

SIDES = ('front', 'back', 'right', 'left')
LIGHT = {'top': 1.12, 'front': 1.0, 'right': 0.9, 'left': 0.9, 'back': 0.8, 'bottom': 0.62}


def k(c, f):
    return [v * f for v in c[:3]]


def mix(a, b, t):
    return [x + (y - x) * t for x, y in zip(a, b)]


def clamp(v, lo=0.0, hi=1.0):
    return max(lo, min(hi, v))


class Painter(_Painter):
    """beastgen's Painter, that can also cut a pixel away (all skins) and light one (the glow skin)."""

    def clear(self, xy):
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            self.img.putpixel((x, y), (0, 0, 0, 0))
            self.glow.putpixel((x, y), (0, 0, 0, 0))

    def lit(self, xy, colour, glow=1.0, skin=None):
        """A burning pixel: `skin` (or the colour) on the skin, the colour at `glow` on the glow skin."""
        x, y = int(xy[0]), int(xy[1])
        if not (0 <= x < bg.TEX and 0 <= y < bg.TEX):
            return
        self.px(xy, skin if skin is not None else colour)
        self.glow.putpixel((x, y), tuple(max(0, min(255, int(v * glow))) for v in colour[:3]) + (255,))

    def get(self, xy):
        return self.img.getpixel((int(xy[0]), int(xy[1])))


# ── Noise ────────────────────────────────────────────────────────────────────

def _hash(ix, iy, seed):
    h = (ix * 374761393 + iy * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return (h ^ (h >> 16)) / 0xFFFFFFFF


def vnoise(x, y, scale=6.0, seed=1):
    """Smooth value noise, 0..1."""
    x, y = x / scale, y / scale
    ix, iy = math.floor(x), math.floor(y)
    fx, fy = x - ix, y - iy
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = _hash(ix, iy, seed), _hash(ix + 1, iy, seed)
    c, d = _hash(ix, iy + 1, seed), _hash(ix + 1, iy + 1, seed)
    return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy


# ── Brushes ──────────────────────────────────────────────────────────────────

def each(p, rect):
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            yield i, j, (fx + i, fy + j)


def flat(p, f, c, noise=3, faces=None):
    for s in faces or f:
        for i, j, xy in each(p, f[s]):
            p.px(xy, [v * LIGHT[s] + p.rnd.randint(-noise, noise) for v in c])


def hide(p, f, base, shade, light=None, mottle=0.35, warts=0.0, fall=0.25, seed=3, belly=None, faces=None):
    """Skin or hide: lit above and toward the front, darker down each face and in shadow beneath; blotched
    with soft mottling between `base` and `shade`, a sheen of `light` on the lit faces, and warts (a lit
    bump with a shadow under it)."""
    light = light or k(base, 1.25)
    for s in faces or f:
        r = f[s]
        fx, fy, fw, fh = r
        for i, j, xy in each(p, r):
            m = vnoise(fx + i, fy + j, 5.0, seed) * 0.65 + vnoise(fx + i, fy + j, 2.0, seed + 7) * 0.35
            c = mix(base, shade, clamp((m - 0.5) * 2 * mottle + 0.5 * mottle))
            if s == 'bottom' and belly:
                c = belly
            t = j / max(1, fh - 1)
            lv = LIGHT[s] * (1 - fall * t)
            if s in ('top', 'front') and m > 0.72:
                c = mix(c, light, (m - 0.72) * 2)
            p.px(xy, [v * lv + p.rnd.randint(-3, 3) for v in c])
        if warts:
            n = int(fw * fh * warts / 12)
            for _ in range(n):
                x, y = fx + p.rnd.randrange(max(1, fw - 1)), fy + p.rnd.randrange(max(1, fh - 1))
                c = p.get((x, y))
                p.px((x, y), k(c, 1.22))
                p.px((x + 1, y), k(c, 1.1))
                p.px((x, y + 1), k(c, 0.72))
                p.px((x + 1, y + 1), k(c, 0.8))


def strands(p, rect, base, dark, tip=None, down=True, density=0.45, lean=0.0, root_dark=0.25, seed=0):
    """Fur or hair in strands running down a face (or up, `down` False): each column a strand of its own
    tone, dark at its root, lighter toward its tip, some strands deep in shadow between locks."""
    fx, fy, fw, fh = rect
    tip = tip or k(base, 1.25)
    for i in range(fw):
        tone = p.rnd.random()
        col = dark if tone < density * 0.5 else base
        for j in range(fh):
            t = j / max(1, fh - 1) if down else 1 - j / max(1, fh - 1)
            ii = int(i + lean * j) % fw
            c = mix(k(col, 1 - root_dark), col, clamp(t * 2))
            if t > 0.6:
                c = mix(c, tip, (t - 0.6) * 1.2 * (0.5 + tone * 0.5))
            if (ii + j // 3) % 5 == 0 and p.rnd.random() < 0.6:
                c = k(c, 0.78)
            p.px((fx + ii, fy + j), [v + p.rnd.randint(-4, 4) for v in c])


def fur(p, f, base, dark, tip=None, under=None, density=0.45, faces=None, lean=0.0):
    """A box in fur: strands down every side, lighter on top, darker beneath (or `under`'s tone)."""
    for s in faces or f:
        lv = LIGHT[s]
        b = under if (s == 'bottom' and under) else base
        strands(p, f[s], k(b, lv), k(dark, lv), k(tip or k(b, 1.25), lv), density=density, lean=lean)


def tufts(p, rect, depth=3, every=3, down=True):
    """Cuts a face's end into tufts of fur."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        ph = (i % every) / max(1, every - 1)
        cut = int(round(depth * abs(ph - 0.5) * 2)) + p.rnd.randint(0, 1)
        for j in range(min(cut, fh - 1)):
            p.clear((fx + i, fy + fh - 1 - j) if down else (fx + i, fy + j))


def scales(p, rect, base, edge, cell=4, light=1.0, belly=False):
    """Overlapping scales: each a rounded plate lit at its crown, a dark rim at its lower edge."""
    fx, fy, fw, fh = rect
    rh = cell // 2 + 1
    for j in range(fh):
        row = j // rh
        off = (cell // 2) if row % 2 else 0
        cy = j % rh
        for i in range(fw):
            cx = (i + off) % cell
            r = abs(cx - (cell - 1) / 2) / (cell / 2)
            kk = light * (1.15 - 0.4 * cy / rh - 0.18 * r)
            c = base
            if cy == rh - 1 or (cy == rh - 2 and r > 0.75):
                c = edge
                kk = light
            if belly:
                c = base if cy < rh - 1 else edge
            p.px((fx + i, fy + j), [v * kk + p.rnd.randint(-3, 3) for v in c])


def plates(p, rect, base, edge, rows=3, light=1.0):
    """A pale belly in bands: broad plates, lit above, a dark seam under each."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        t = (j % rows) / max(1, rows - 1)
        for i in range(fw):
            round_ = 1 - abs(i / max(1, fw - 1) - 0.5) * 0.5
            c = edge if j % rows == rows - 1 else base
            p.px((fx + i, fy + j), [v * light * (1.1 - 0.25 * t) * round_ + p.rnd.randint(-3, 3) for v in c])


def weave(p, rect, base, deep, light=1.0, fold=6, fall=0.3, coarse=True):
    """Coarse cloth: a visible weave, folds running down it, darker toward its foot."""
    fx, fy, fw, fh = rect
    for i, j, xy in each(p, rect):
        t = j / max(1, fh - 1)
        f_ = 0.5 + 0.5 * math.sin((i + 0.6 * math.sin(j * 0.25)) * 2 * math.pi / fold)
        c = mix(deep, base, 0.35 + 0.65 * f_)
        if coarse and (i + j) % 2 == 0:
            c = k(c, 0.93)
        if coarse and j % 3 == 0 and p.rnd.random() < 0.3:
            c = k(c, 1.07)
        p.px(xy, [v * light * (1 - fall * t) + p.rnd.randint(-4, 4) for v in c])


def cloth(p, f, base, deep, fold=6, faces=None, fall=0.3, coarse=True):
    for s in faces or f:
        weave(p, f[s], base, deep, LIGHT[s], fold=fold if s not in ('top', 'bottom') else 99, fall=fall if s not in ('top', 'bottom') else 0,
              coarse=coarse)


def patch(p, rect, colour, deep, stitch, x, y, w, h):
    """A patch sewn over a face: its own cloth, a row of stitches round it."""
    fx, fy, fw, fh = rect
    for j in range(h):
        for i in range(w):
            X, Y = fx + x + i, fy + y + j
            if fx <= X < fx + fw and fy <= Y < fy + fh:
                edge = i in (0, w - 1) or j in (0, h - 1)
                c = stitch if edge and (i + j) % 2 == 0 else mix(deep, colour, 0.6 + 0.4 * ((i * 3 + j) % 4 > 0))
                p.px((X, Y), [v + p.rnd.randint(-4, 4) for v in c])


def ragged(p, rect, teeth=3, depth=3, down=True, keep_glow=False):
    """Cuts a face's end into ragged tatters."""
    fx, fy, fw, fh = rect
    i = 0
    while i < fw:
        w = p.rnd.randint(1, teeth)
        cut = p.rnd.randint(0, depth)
        for ii in range(i, min(fw, i + w)):
            for j in range(min(cut, fh - 1)):
                p.clear((fx + ii, fy + fh - 1 - j) if down else (fx + ii, fy + j))
        i += w


def tear_holes(p, rect, n=2, size_=2):
    fx, fy, fw, fh = rect
    for _ in range(n):
        x, y = fx + p.rnd.randrange(1, max(2, fw - 2)), fy + p.rnd.randrange(1, max(2, fh - 3))
        for j in range(size_):
            for i in range(size_ - (j == size_ - 1)):
                p.clear((x + i, y + j))


def point(p, rect, down=True, keep=0.5, sharp=0.6):
    """Cuts a face to a point toward its end."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        t = (fh - 1 - j) if down else j
        half = max(keep, (t + 1) * sharp)
        for i in range(fw):
            if abs(i - fw / 2 + 0.5) > half:
                p.clear((fx + i, fy + j))


def leather(p, f, base, deep=None, stitch=None, faces=None, worn=0.25):
    """Leather: smooth, scuffed lighter in places, a stitched seam along its edges."""
    deep = deep or k(base, 0.7)
    for s in faces or f:
        fx, fy, fw, fh = f[s]
        for i, j, xy in each(p, f[s]):
            m = vnoise(fx + i, fy + j, 3.0, 11)
            c = mix(deep, base, 0.5 + 0.5 * m)
            if m > 1 - worn * 0.5:
                c = k(c, 1.15)
            p.px(xy, [v * LIGHT[s] + p.rnd.randint(-3, 3) for v in c])
        if stitch and s in SIDES and fh > 3:
            for i in range(1, fw - 1, 2):
                p.px((fx + i, fy + 1), stitch)
                p.px((fx + i, fy + fh - 2), stitch)


def iron(p, f, base, rust=0.0, rivets=0, dents=0.15, faces=None, rim=True, rustc=(120, 62, 30)):
    """Hammered iron: dark, lit along its top edge, scratched and dented, rust gathering low; rivets."""
    hi = k(base, 1.8)
    for s in faces or f:
        fx, fy, fw, fh = f[s]
        for i, j, xy in each(p, f[s]):
            m = vnoise(fx + i, fy + j, 2.5, 21)
            c = k(base, 0.85 + 0.3 * m)
            if rust and vnoise(fx + i, fy + j, 3.0, 33) * (0.6 + 0.6 * j / max(1, fh)) > 1 - rust:
                c = mix(c, rustc, 0.6 + 0.4 * p.rnd.random())
            if p.rnd.random() < dents * 0.1:
                c = k(c, 0.7)
            p.px(xy, [v * LIGHT[s] + p.rnd.randint(-4, 4) for v in c])
        if rim and s in SIDES:
            for i in range(fw):
                p.px((fx + i, fy), k(hi, LIGHT[s]))
                if fh > 2:
                    p.px((fx + i, fy + fh - 1), k(base, 0.55))
        for _ in range(int(fw * fh * dents / 30)):                    # scratches
            x, y = fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)
            for t in range(p.rnd.randint(2, 4)):
                p.px((x + t, y + t // 2), k(hi, 0.85 * LIGHT[s]))
        if rivets and s in SIDES and fw > 3 and fh > 3:
            for i in range(1, fw - 1, rivets):
                rivet(p, fx + i, fy + 1, base)
                if fh > 6:
                    rivet(p, fx + i, fy + fh - 3, base)


def rivet(p, x, y, base):
    p.px((x, y), k(base, 2.1))
    p.px((x + 1, y), k(base, 1.4))
    p.px((x, y + 1), k(base, 1.2))
    p.px((x + 1, y + 1), k(base, 0.5))


def wood(p, f, base, dark=None, along='y', faces=None, knots=0.02):
    """Wood: grain running along its length, knots here and there, darker at its ends."""
    dark = dark or k(base, 0.62)
    for s in faces or f:
        fx, fy, fw, fh = f[s]
        for i, j, xy in each(p, f[s]):
            a, b = (i, j) if along == 'y' else (j, i)
            g = 0.5 + 0.5 * math.sin(a * 1.7 + 1.3 * math.sin(b * 0.21 + a))
            c = mix(dark, base, 0.35 + 0.65 * g)
            p.px(xy, [v * LIGHT[s] + p.rnd.randint(-3, 3) for v in c])
        for _ in range(int(fw * fh * knots)):
            x, y = fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)
            p.px((x, y), k(dark, 0.7))
            p.px((x + 1, y), k(dark, 0.85))


def rope(p, f, base, faces=None, pitch=3):
    """Twisted rope: strands spiralling round it."""
    for s in faces or f:
        fx, fy, fw, fh = f[s]
        for i, j, xy in each(p, f[s]):
            t = ((i + j) % pitch) / pitch
            c = k(base, 0.7 + 0.5 * (1 - abs(t - 0.5) * 2))
            p.px(xy, [v * LIGHT[s] + p.rnd.randint(-3, 3) for v in c])


def lamellar(p, rect, plate, lace, rows=3, cols=2, light=1.0, rim=None):
    """Lamellae: small upright plates in rows, each lit at its top and dark at its seam, laced in `lace`
    between the rows (a cross-stitch over each seam)."""
    fx, fy, fw, fh = rect
    rim = rim or k(plate, 1.7)
    for j in range(fh):
        r = j % rows
        for i in range(fw):
            c = plate
            kk = light * (1.18 - 0.35 * r / max(1, rows - 1))
            if i % cols == cols - 1:
                kk *= 0.6
            if r == 0:
                c = rim
                kk = light * 0.9
            if r == rows - 1 and (i + j // rows) % 2 == 0:
                c, kk = lace, light
            p.px((fx + i, fy + j), [v * kk + p.rnd.randint(-3, 3) for v in c])


def hline(p, rect, y, colour, x0=0, x1=None):
    fx, fy, fw, fh = rect
    for i in range(x0, fw if x1 is None else x1):
        p.px((fx + i, fy + y), colour)


def vline(p, rect, x, colour, y0=0, y1=None):
    fx, fy, fw, fh = rect
    for j in range(y0, fh if y1 is None else y1):
        p.px((fx + x, fy + j), colour)


def border(p, f, colour, sides=SIDES, top=True, bottom=True, width=1):
    for s in sides:
        fx, fy, fw, fh = f[s]
        for i in range(fw):
            for w in range(width):
                if top:
                    p.px((fx + i, fy + w), colour)
                if bottom:
                    p.px((fx + i, fy + fh - 1 - w), colour)


def disc(p, cx, cy, r, colour, rim=None, glow=None):
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r:
                c = rim if (rim and d > r - 1) else colour
                if glow:
                    p.lit((x, y), c, glow)
                else:
                    p.px((x, y), c)


def cut_all(p, f, faces):
    for s in faces:
        for i, j, xy in each(p, f[s]):
            p.clear(xy)


def eye(p, rect, x, y, w, h, iris, pupil=(14, 10, 8), lid=None, socket=None, slit=False, glow=1.0, look=0):
    """A glaring eye on a face, at (x, y) within it, `w` x `h`: a dark socket round it, the iris
    glowing, a pupil (round, or a slit), a catch-light; a heavy lid shadowing its top row."""
    fx, fy, fw, fh = rect
    socket = socket or (24, 14, 12)
    for j in range(-1, h + 1):
        for i in range(-1, w + 1):
            X, Y = fx + x + i, fy + y + j
            if 0 <= i < w and 0 <= j < h:
                continue
            if (i in (-1, w) and j in (-1, h)):
                continue
            p.px((X, Y), socket)
    for j in range(h):
        for i in range(w):
            X, Y = fx + x + i, fy + y + j
            d = abs(i - (w - 1) / 2) / max(1, w / 2)
            c = mix(k(iris, 1.25), iris, d)
            if j == 0 and lid:
                c = lid
                p.px((X, Y), c)
                continue
            p.lit((X, Y), c, glow)
    cx = x + (w - 1) // 2 + look
    for j in range(1 if lid else 0, h):
        if slit or h <= 2:
            for i in ((cx,) if w < 4 else (cx, cx + 1)) if slit else (cx,):
                p.px((fx + i, fy + y + j), pupil)
                p.glow.putpixel((fx + i, fy + y + j), (0, 0, 0, 0))
    if not slit and h > 2:
        for j in range(1, h):
            for i in (cx, cx + 1) if w > 3 else (cx,):
                p.px((fx + i, fy + y + j), pupil)
                p.glow.putpixel((fx + i, fy + y + j), (0, 0, 0, 0))
    p.lit((fx + x + max(0, cx - 1), fy + y + (1 if lid else 0)), (255, 250, 220), 1.0)


def teeth_row(p, rect, y, colour, gum, every=2, jagged=True, down=True, x0=0, x1=None, rows=2):
    """A row of crooked teeth along a face at row y: ivory points, dark gaps between."""
    fx, fy, fw, fh = rect
    x1 = fw if x1 is None else x1
    for i in range(x0, x1):
        tall = rows if (i % every == 0) else (rows - 1 if jagged and p.rnd.random() < 0.5 else rows)
        for j in range(rows):
            yy = fy + y + (j if down else -j)
            if j < tall and (i % every != every - 1 or every == 1):
                p.px((fx + i, yy), k(colour, 1 - 0.12 * j))
            else:
                p.px((fx + i, yy), gum)


def glyph(p, x, y, rows, colour, glow=None):
    """Pixels from a little bitmap ('#' set)."""
    for j, row in enumerate(rows):
        for i, c in enumerate(row):
            if c == '#':
                if glow:
                    p.lit((x + i, y + j), colour, glow)
                else:
                    p.px((x + i, y + j), colour)


def talisman(p, rect, paper=(222, 196, 96), ink=(176, 26, 20), x=None, y=0, w=None, h=None):
    """A Daoist talisman (符): yellow paper, a red seal-script spell down it, a red frame."""
    fx, fy, fw, fh = rect
    x = 0 if x is None else x
    w = fw - x if w is None else w
    h = fh - y if h is None else h
    for j in range(h):
        for i in range(w):
            X, Y = fx + x + i, fy + y + j
            c = paper
            if i in (0, w - 1) or j in (0, h - 1):
                c = ink
            elif i == w // 2 and 1 < j < h - 2:
                c = ink
            elif j % 4 == 2 and 0 < i < w - 1 and (i + j // 4) % 2 == 0:
                c = ink
            p.px((X, Y), [v + p.rnd.randint(-5, 5) for v in c])


def save(p, name):
    p.save(name)


# ── Measuring ────────────────────────────────────────────────────────────────

def _mul(a, b):
    return [[sum(a[i][t] * b[t][j] for t in range(3)) for j in range(3)] for i in range(3)]


def _apply(m, v):
    return [sum(m[i][t] * v[t] for t in range(3)) for i in range(3)]


def _rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    x = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    y = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    z = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return _mul(_mul(z, y), x)


def corners(parts, only=None):
    """Every box corner of the model at rest, in model space: {part name: [points]}."""
    frames, out = {}, {}
    eye_ = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    for name, parent, pivot, r, boxes, _ in parts:
        pm, pt = frames.get(parent, (eye_, [0, 0, 0]))
        m = _mul(pm, _rot(*r))
        t = [a + b for a, b in zip(pt, _apply(pm, pivot))]
        frames[name] = (m, t)
        pts = []
        for b in boxes:
            for cx_ in (b[0], b[0] + b[3]):
                for cy_ in (b[1], b[1] + b[4]):
                    for cz_ in (b[2], b[2] + b[5]):
                        pts.append([a + c for a, c in zip(t, _apply(m, (cx_, cy_, cz_)))])
        out[name] = pts
    return out


def settle(parts, feet):
    """The model moved up or down so the lowest point of its `feet` parts stands on the ground (y 24)."""
    c = corners(parts)
    low = max(p[1] for n in feet for p in c[n])
    dy = 24 - low
    return [(n, par, (pv[0], pv[1] + dy, pv[2]) if par == 'root' else pv, r, b, m) for n, par, pv, r, b, m in parts]


def report(name, parts, size_, slots):
    c = corners(parts)
    pts = [p for v in c.values() for p in v]
    lo = [min(p[i] for p in pts) for i in range(3)]
    hi = [max(p[i] for p in pts) for i in range(3)]
    print('%s: %d parts, %d boxes; drawn at %s: %.2f blocks tall, %.2f wide, %.2f deep (y %.1f..%.1f)' % (
        name, len(parts), len(slots), size_, (hi[1] - lo[1]) * size_ / 16, (hi[0] - lo[0]) * size_ / 16,
        (hi[2] - lo[2]) * size_ / 16, lo[1], hi[1]))


def scale_tiles(p, rect, base, edge, light=1.0, cw=4, ch=3, vary=0.12, hi=None, seed=0):
    """Scales as tiles in offset rows, each its own shade: lit along its upper left, a dark rim along
    its rounded lower edge (its corners darker still), so each reads as a plate lapping the one below."""
    fx, fy, fw, fh = rect
    hi = hi or k(base, 1.35)
    for j in range(fh):
        row = j // ch
        off = (cw // 2) if row % 2 else 0
        cy = j % ch
        for i in range(fw):
            col = (i + off) // cw
            cx = (i + off) % cw
            tone = 1 + vary * (2 * _hash(col, row, seed + 5) - 1)
            c = k(base, tone)
            if cy == 0 and cx < cw - 1:
                c = mix(c, hi, 0.5)
            if cx == 0 and cy < ch - 1:
                c = mix(c, hi, 0.25)
            if cy == ch - 1:
                c = edge if (0 < cx < cw - 1) else k(edge, 0.7)
            elif cx == cw - 1:
                c = mix(c, edge, 0.55)
            p.px((fx + i, fy + j), [v * light + p.rnd.randint(-3, 3) for v in c])
