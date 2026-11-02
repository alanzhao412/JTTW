"""What Heaven's gods and star lords (heaven_gods.py) share: their painter, their palette, and the
brushes for silk robes, gold trim, armour, faces and hands, all painted at twice the model's resolution
to the box-UV layout beastgen gives (king_yama.paint: each face where the game looks for it)."""
import math

import beastgen as bg
from beastgen import Painter
from king_yama import k, mixc, noisy, point, circle, lock, BLUE_BLACK, BROWN_BLACK, ASH

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

OUT = 'src/main/resources/assets/jttw_engine/textures/entity/'
JAVA = 'src/main/java/com/jttw/engine/client/renderer/HeavenGodRenderers.java'

SIDES = ('front', 'back', 'right', 'left')
LIGHTS = {'top': 1.1, 'bottom': 0.62, 'front': 1.0, 'back': 0.84, 'right': 0.92, 'left': 0.92}

GOLD = (232, 186, 70)
GOLD_LIGHT = (255, 236, 150)
GOLD_DEEP = (150, 102, 34)
JADE = (96, 190, 140)
JADE_DEEP = (44, 118, 84)
IVORY = (236, 228, 204)
IVORY_SHADE = (190, 178, 150)
BLACK = (24, 20, 26)
BLACK_SHEEN = (62, 56, 70)
STEEL = (184, 190, 204)
STEEL_DARK = (92, 98, 114)
BRONZE = (176, 120, 58)
BRONZE_DEEP = (100, 62, 30)
BRONZE_GREEN = (82, 136, 112)
WOOD = (124, 84, 46)
WOOD_DEEP = (72, 46, 24)
LIPS = (150, 54, 52)
RED_LIPS = (196, 40, 52)
FIRE_DEEP = (170, 30, 10)
FIRE = (240, 96, 24)
FIRE_HOT = (255, 214, 110)
SILVER = (214, 222, 236)

WHITE_HAIR = ((120, 120, 130), (198, 198, 206), (230, 230, 236), (252, 252, 255))
RED_HAIR = ((40, 6, 4), (130, 26, 14), (206, 64, 26), (250, 140, 60))
VIOLET_HAIR = ((40, 24, 60), (120, 92, 160), (196, 176, 230), (244, 236, 255))


class GodPainter(Painter):
    """A Painter that can cut a pixel clear (from both skins) and paint a shining one."""

    def clear(self, xy):
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            self.img.putpixel((x, y), (0, 0, 0, 0))
            self.glow.putpixel((x, y), (0, 0, 0, 0))

    def lit(self, xy, colour, glow=1.0, skin=None):
        """A shining pixel: on the skin `skin` (or the colour), on the glowing skin the colour at `glow`."""
        x, y = int(xy[0]), int(xy[1])
        if not (0 <= x < bg.TEX and 0 <= y < bg.TEX):
            return
        self.px(xy, skin if skin is not None else colour)
        if glow:
            self.glow.putpixel((x, y), tuple(max(0, min(255, int(v * glow))) for v in colour[:3]) + (255,))

    def save(self, name):
        self.img.save(OUT + name + '.png')
        self.glow.save(OUT + name + '_glow.png')


def each(rect):
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            yield i, j, (fx + i, fy + j)


def bare(p, f, which=('right', 'left', 'top', 'bottom')):
    for s in which:
        for _, _, xy in each(f[s]):
            p.clear(xy)


# ── Cloth ────────────────────────────────────────────────────────────────────

def silk_face(p, rect, base, deep, light=1.0, fold=7, fall=0.35, sheen=0.1):
    """Silk: lit at the top, falling into shadow toward its foot, in soft vertical folds, faintly woven."""
    fx, fy, fw, fh = rect
    phase = p.rnd.random() * 6.3
    for j in range(fh):
        t = j / max(1, fh - 1)
        c0 = mixc(base, deep, t * fall)
        for i in range(fw):
            f = 1.0 + sheen * math.sin(i * 2 * math.pi / fold + phase) + 0.04 * math.sin(i * 0.9 + phase * 2)
            weave = 0.97 if (i + j) % 2 else 1.0
            p.px((fx + i, fy + j), [v * light * f * weave + p.rnd.randint(-2, 2) for v in c0])


def silk(p, f, base, deep, fold=7, fall=0.35):
    for s, r in f.items():
        if s == 'bottom':
            p.face(r, k(deep, 0.9), 1.0, noise=2)
        elif s == 'top':
            silk_face(p, r, base, deep, 1.06, fold, fall=0.1)
        else:
            silk_face(p, r, base, deep, LIGHTS[s], fold, fall)


def hline(p, rect, y, colour, x0=0, x1=None):
    fx, fy, fw, fh = rect
    for i in range(x0, fw if x1 is None else x1):
        p.px((fx + i, fy + y), colour)


def vline(p, rect, x, colour):
    fx, fy, fw, fh = rect
    for j in range(fh):
        p.px((fx + x, fy + j), colour)


def border(p, rect, colour, inner=None, width=1):
    """A frame round a face, `inner` one pixel in from it."""
    fx, fy, fw, fh = rect
    for i, j, xy in each(rect):
        e = min(i, j, fw - 1 - i, fh - 1 - j)
        if e < width:
            p.px(xy, colour)
        elif inner and e == width:
            p.px(xy, inner)


def edges(p, f, colour, sides=SIDES, top=True, bottom=True, deep=None):
    for s in sides:
        fx, fy, fw, fh = f[s]
        for i in range(fw):
            if top:
                p.px((fx + i, fy), colour)
                if deep and fh > 3:
                    p.px((fx + i, fy + 1), deep)
            if bottom:
                p.px((fx + i, fy + fh - 1), colour)
                if deep and fh > 3:
                    p.px((fx + i, fy + fh - 2), deep)


def hem_band(p, rect, rows, ground, line=GOLD, deep=GOLD_DEEP):
    """A border along a face's foot: a band of `ground` between gold lines."""
    fx, fy, fw, fh = rect
    for j in range(rows):
        for i in range(fw):
            p.px((fx + i, fy + fh - 1 - j), noisy(p, ground, 3))
    hline(p, rect, fh - 1, line)
    hline(p, rect, fh - rows - 1, line)
    if fh - rows - 2 >= 0:
        hline(p, rect, fh - rows - 2, deep)


def waves(p, rect, rows, colours, line=GOLD, deep=GOLD_DEEP):
    """The sea-and-cliff hem (海水江崖): slanting bands of wave in `colours` along a face's foot, the
    cliffs' peaks rising from them, gold lines over and under."""
    fx, fy, fw, fh = rect
    top = fy + fh - rows
    for j in range(rows):
        for i in range(fw):
            c = colours[((i + j) // 2) % len(colours)]
            p.px((fx + i, top + j), noisy(p, c, 3))
    for i in range(fw):                                            # curling crests over the waves
        if (i // 7) % 2 == 0:
            a = (i % 7) / 6 * math.pi
            y = top - 1 - int(2 * math.sin(a))
            p.px((fx + i, y), colours[-1])
            p.px((fx + i, y + 1), colours[0])
    for i in range(fw):                                            # the cliffs between them
        if (i // 7) % 4 == 2:
            h = 4 - abs(i % 7 - 3)
            for j in range(h):
                p.px((fx + i, top - 1 - j), line if j == h - 1 else deep)
    hline(p, rect, fh - 1, line)


def cloud_mark(p, cx, cy, colour, r=2, rect=None):
    """An auspicious cloud (祥云): three curls over a trailing tail, drawn in thread."""
    def put(x, y):
        if rect is None or (rect[0] <= x < rect[0] + rect[2] and rect[1] <= y < rect[1] + rect[3]):
            p.px((x, y), colour)
    for ox, oy in ((-r - 1, 0), (0, -1), (r + 1, 0)):
        for a in range(0, 360, 30):
            if 200 < a < 340 and ox == 0:
                continue
            put(cx + ox + round(r * math.cos(math.radians(a))), cy + oy + round(r * math.sin(math.radians(a))))
    for i in range(r * 2 + 2):
        put(cx + r + 2 + i, cy + r - (i // 3))


def clouds(p, rect, colour, every=10, r=2, share=0.7, skip_bottom=0):
    fx, fy, fw, fh = rect
    row = 0
    for y in range(fy + 3, fy + fh - 3 - skip_bottom, every):
        off = (every // 2 + 3) if row % 2 else 0
        for x in range(fx + 3 + off, fx + fw - 3, every + 6):
            if p.rnd.random() < share:
                cloud_mark(p, x + p.rnd.randint(-2, 2), y + p.rnd.randint(-1, 1), colour, r, rect)
        row += 1


def stars(p, rect, colour, n=None, glow=0.0, skip_bottom=0):
    """Gold stars scattered on silk, now and then three joined in a little constellation."""
    fx, fy, fw, fh = rect
    n = n if n is not None else max(1, fw * fh // 70)
    for _ in range(n):
        x = fx + p.rnd.randint(1, max(1, fw - 2))
        y = fy + p.rnd.randint(1, max(1, fh - 2 - skip_bottom))
        pts = [(x, y)]
        if p.rnd.random() < 0.4:
            x2, y2 = x + p.rnd.randint(3, 5), y + p.rnd.randint(-3, 3)
            x3, y3 = x2 + p.rnd.randint(3, 5), y2 + p.rnd.randint(-3, 3)
            for (a, b), (c, d) in (((x, y), (x2, y2)), ((x2, y2), (x3, y3))):
                for t in range(1, 6):
                    q = (round(a + (c - a) * t / 6), round(b + (d - b) * t / 6))
                    if fx <= q[0] < fx + fw and fy <= q[1] < fy + fh:
                        p.px(q, mixc(colour, p.img.getpixel(q)[:3], 0.55))
            pts += [(x2, y2), (x3, y3)]
        for (a, b) in pts:
            if fx <= a < fx + fw and fy <= b < fy + fh:
                if glow:
                    p.lit((a, b), GOLD_LIGHT, glow=glow)
                else:
                    p.px((a, b), GOLD_LIGHT)
                for da, db in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if fx <= a + da < fx + fw and fy <= b + db < fy + fh:
                        p.px((a + da, b + db), colour)


def fret(p, rect, gold, deep):
    """A gold band with a key-fret (回纹) running along it."""
    fx, fy, fw, fh = rect
    tile = ['1111110', '1000010', '1011010', '1010010', '1010000', '1011111']
    for j in range(fh):
        for i in range(fw):
            jj = int(j * len(tile) / max(1, fh)) if fh < 6 else j % 6
            on = tile[jj][i % 7] == '1'
            edge = j == 0 or j == fh - 1
            c = k(gold, 1.1) if edge else (gold if on else mixc(gold, deep, 0.6))
            p.px((fx + i, fy + j), noisy(p, c, 3))


# ── Metal ────────────────────────────────────────────────────────────────────

def gold_box(p, f, light=1.0, base=GOLD, hi=GOLD_LIGHT, lo=GOLD_DEEP):
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                t = j / max(1, fh - 1)
                c = mixc(hi, base, min(1, t * 2)) if t < 0.5 else mixc(base, lo, (t - 0.5) * 2)
                if (i * 7 + j * 3) % 13 == 0:
                    c = k(c, 1.1)
                p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s] * light), 4))


def steel_box(p, f, base=STEEL, dark=STEEL_DARK):
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                across = (i + 0.5) / fw
                c = mixc((240, 244, 252), base, min(1, across * 2.2)) if across < 0.5 else mixc(base, dark, (across - 0.5) * 2)
                p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))


def metal(p, f, base, deep):
    """Cast metal: lit above, dark beneath, a bright glint along its top edges."""
    p.shaded(f, base)
    for s in SIDES:
        fx, fy, fw, fh = f[s]
        for i in range(fw):
            p.px((fx + i, fy), mixc(base, (255, 255, 255), 0.3))
            p.px((fx + i, fy + fh - 1), deep)


def plate(p, f, base, trim=GOLD, deep=GOLD_DEEP, studs=True, rounded=True):
    """A lacquered armour plate: rounded across each face (lit down its middle), a gold rim, studs."""
    for s, r in f.items():
        fx, fy, fw, fh = r
        for i, j, xy in each(r):
            across = abs((i + 0.5) / fw - 0.5) * 2
            t = (1.3 - (0.5 * across ** 1.5 if rounded else 0) - 0.3 * j / max(1, fh - 1)) * LIGHTS[s]
            p.px(xy, noisy(p, k(base, t), 2))
        if s in SIDES and fh > 2:
            hline(p, r, 0, trim)
            hline(p, r, fh - 1, deep)
            if fh > 4:
                hline(p, r, fh - 2, trim)
            if studs and fw > 5 and fh > 5:
                for i in range(2, fw - 1, 4):
                    p.px((fx + i, fy + 2), GOLD_LIGHT)
        if s in ('top', 'bottom'):
            border(p, r, trim if s == 'top' else deep)


def scale_mail(p, rect, base, edge, light=1.0, cw=4, ch=3):
    """Fish-scale armour (鱼鳞甲): rounded scales in staggered rows, gold at their edges."""
    fx, fy, fw, fh = rect
    for i, j, xy in each(rect):
        row = j // ch
        x = (i + (row % 2) * (cw // 2)) % cw
        y = j % ch
        d = math.hypot((x - (cw - 1) / 2) / (cw / 2), (y - 0.2) / ch)
        t = (1.35 - 0.7 * d) * light
        c = k(base, t)
        if d > 0.9 or (y == ch - 1 and x in (0, cw - 1)):
            c = k(edge, 0.85 * light)
        p.px(xy, noisy(p, c, 3))


def lamellar(p, rect, base, lace, trim, light=1.0, pw=4, ph=5, sheen=None):
    """Lamellar: lacquered plates in staggered rows, each rounded, a gleam at its top, laced at its foot."""
    fx, fy, fw, fh = rect
    sheen = sheen or k(base, 1.6)
    for i, j, xy in each(rect):
        row, y = j // ph, j % ph
        off = (row % 2) * (pw // 2)
        x = (i + off) % pw
        across = abs(x + 0.5 - pw / 2) / (pw / 2)
        kk = (1.25 - 0.45 * across - 0.3 * y / max(1, ph - 1)) * light
        c = k(base, kk)
        if y == 0 and across < 0.6:
            c = k(sheen, light)
        if x == 0:
            c = k(base, 0.45 * light)
        if y == ph - 1:
            c = k(lace, light) if x in (pw // 2 - 1, pw // 2) and row % 2 == 0 else k(base, 0.35 * light)
        p.px(xy, noisy(p, c, 2))
    hline(p, rect, 0, k(trim, light))
    hline(p, rect, fh - 1, k(GOLD_DEEP, light))


def disc(p, rect, colour_at, clear_out=True):
    """A round plate painted on a face: colour_at(r, a) for r 0 (centre) .. 1 (rim), a its angle (None:
    clear, ('glow', c, g): shining); outside it cleared."""
    fx, fy, fw, fh = rect
    cx, cy, R = fx + fw / 2, fy + fh / 2, min(fw, fh) / 2
    for y in range(fy, fy + fh):
        for x in range(fx, fx + fw):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy) / R
            a = math.atan2(dy, dx)
            c = colour_at(r, a, (dx / R, dy / R)) if r <= 1 else None
            if c is None:
                if clear_out:
                    p.clear((x, y))
            elif c[0] == 'glow':
                p.lit((x, y), c[1], glow=c[2])
            else:
                p.px((x, y), c)


def cutout(p, rect, mask, colour, glow=0.0, flip=False):
    """A flat shape cut from a face: mask(s, t) (s across 0..1, t down 0..1) whether it is solid there,
    colour(s, t) its colour; the rest cleared."""
    fx, fy, fw, fh = rect
    for i, j, xy in each(rect):
        s = (i + 0.5) / fw
        s = 1 - s if flip else s
        t = (j + 0.5) / fh
        if mask(s, t):
            c = colour(s, t)
            if glow:
                p.lit(xy, c, glow=glow)
            else:
                p.px(xy, noisy(p, c, 2))
        else:
            p.clear(xy)


def flame_face(p, rect, hot=FIRE_HOT, mid=FIRE, deep=FIRE_DEEP, glow=0.9, tongues=1, upward=True):
    """Tongues of fire cut from a face, licking upward (or downward), hot at their roots."""
    fx, fy, fw, fh = rect
    for i, j, xy in each(rect):
        t = 1 - j / max(1, fh - 1) if upward else j / max(1, fh - 1)      # 0 at the root .. 1 at the tip
        u = ((i + 0.5) / fw * tongues) % 1.0
        half = 0.5 * (1 - t ** 1.3) + 0.04
        sway = 0.12 * math.sin(t * 4 + i * 0.3) * t
        dx = u - 0.5 - sway
        if abs(dx) > half:
            p.clear(xy)
            continue
        core = abs(dx) < half * 0.45 and t < 0.7
        c = mixc(hot, mid, t) if core else mixc(mid, deep, min(1, t * 1.3))
        p.lit(xy, c, glow=glow)


# ── Flesh ────────────────────────────────────────────────────────────────────

def skin_box(p, f, skin, light=1.0):
    """Bare skin over a box: rounded across each face, lit above."""
    for s, r in f.items():
        fx, fy, fw, fh = r
        for i, j, xy in each(r):
            across = abs((i + 0.5) / fw - 0.5) * 2
            t = (1.08 - 0.22 * across ** 2 - (0.12 * j / max(1, fh - 1) if s in SIDES else 0)) * LIGHTS[s] * light
            p.px(xy, noisy(p, k(skin, t), 2))


def hand(p, f, skin, nails=None):
    """A fist: the fingers' creases across its front, the knuckles lit."""
    skin_box(p, f, skin)
    shade = k(skin, 0.7)
    for s in ('front', 'right', 'left'):
        fx, fy, fw, fh = f[s]
        for i in range(0, fw, 2):
            p.px((fx + i, fy + fh - 2), shade)
        hline(p, f[s], 1, k(skin, 1.12))
    if nails:
        fx, fy, fw, fh = f['bottom']
        for i in range(0, fw, 2):
            p.px((fx + i, fy), nails)


def strands(p, rect, tone, lean=0, light=1.0):
    """Hair in fine locks down a face: each strand its own shade, dark in its partings."""
    fx, fy, fw, fh = rect
    cols = [p.rnd.uniform(-0.3, 0.3) for _ in range(fw + fh)]
    for i, j, xy in each(rect):
        n = (i + (j * lean) // 4) % 3
        t = (1.0 + (0.8 if n == 0 else 0.25 if n == 1 else -0.25) + cols[(i + (j * lean) // 4) % len(cols)]) * light
        t -= 0.4 * j / max(1, fh)
        p.px(xy, noisy(p, shade_of(tone, t), 3))


def shade_of(tone, t):
    t = max(0.0, min(3.0, t))
    i = min(2, int(t))
    return mixc(tone[i], tone[i + 1], t - i)


def hair_box(p, f, tone, lean=0):
    for s, r in f.items():
        strands(p, r, tone, lean, LIGHTS.get(s, 1.0))


def face_front(p, rect, o):
    """A face, painted on the front of the head's box (rect, some 18x15): o['kind'] one of 'man', 'lady',
    'elder', 'fierce' (bulging glowing eyes, scowling), 'demon' (fierce, fanged); o['skin'] its colour,
    o['hair'] the hair tone (its hairline along the top, `hairline` rows of it), o['eye'] the iris (a
    fierce one's glows), o['third'] a third eye on the brow."""
    fx, fy, fw, fh = rect
    skin = o['skin']
    shade = k(skin, 0.72)
    kind = o['kind']
    hair = o.get('hair', BLUE_BLACK)
    hairline = o.get('hairline', 1)
    cx = fw / 2
    for i, j, xy in each(rect):                                    # the skin, lit across the brow and cheeks
        across = abs((i + 0.5) / fw - 0.5) * 2
        t = 1.06 - 0.16 * across ** 2 - 0.08 * j / fh
        c = k(skin, t)
        if kind in ('lady',) and 8 <= j <= 11 and 0.4 < across < 0.85:
            c = mixc(c, (236, 120, 130), 0.28)                     # her cheeks, rouged
        if kind == 'elder' and 9 <= j <= 11 and 0.45 < across < 0.8:
            c = mixc(c, (230, 120, 110), 0.25)
        p.px(xy, noisy(p, c, 2))
    for j in range(hairline):                                      # the hairline
        for i in range(fw):
            p.px((fx + i, fy + j), shade_of(hair, 1.2 - 0.3 * j))
    if o.get('widow'):
        for i in range(fw):
            d = abs(i + 0.5 - cx)
            for j in range(hairline, hairline + max(0, 2 - int(d / 2.5))):
                p.px((fx + i, fy + j), shade_of(hair, 0.9))

    ey = o.get('eye_row', 7)                                       # the eyes' row
    if kind == 'lady':
        for side in (0, 1):
            ex = 3 if side == 0 else fw - 3 - 5
            sgn = 1 if side == 0 else -1
            for i in range(5):                                     # brows: fine, arched, long
                bx = ex + i - (1 if side == 0 else -1) * 0
                by = ey - 3 + (0 if 1 <= i <= 3 else 1)
                p.px((fx + bx, fy + by), shade_of(hair, 0.8))
            for i in range(5):                                     # the eye: almond, a dark lash line, a dark iris
                p.px((fx + ex + i, fy + ey - 1), (40, 26, 30))
                p.px((fx + ex + i, fy + ey), (244, 238, 232))
                p.px((fx + ex + i, fy + ey + 1), k(skin, 0.86))
            ix = ex + (2 if side == 0 else 2)
            p.px((fx + ix, fy + ey), o.get('eye', (40, 30, 34)))
            p.px((fx + ix + 1, fy + ey), (20, 14, 16))
            p.px((fx + ex + (4 if side == 0 else 0), fy + ey - 1), (20, 12, 16))   # a flick at the outer corner
            p.px((fx + ex + (5 if side == 0 else -1), fy + ey - 2), (20, 12, 16))
            for i in range(5):                                     # a touch of colour on the lid
                p.px((fx + ex + i, fy + ey - 2), mixc(k(skin, 0.95), o.get('lid', (200, 110, 140)), 0.35))
        for i in range(int(cx) - 2, int(cx) + 2):                  # small red lips
            p.px((fx + i, fy + fh - 1), RED_LIPS if abs(i + 0.5 - cx) < 1.6 else k(RED_LIPS, 0.75))
        p.px((fx + int(cx) - 1, fy + fh - 2), k(skin, 0.82))
        if o.get('mark'):                                          # a flower mark (花钿) on the brow
            mx = int(cx) - 1
            for (a, b) in ((0, 0), (1, 0), (0, 1), (1, 1), (0, -1), (1, -1)):
                p.px((fx + mx + a, fy + ey - 5 + b), o['mark'])
        return

    if kind == 'elder':
        for side in (0, 1):                                        # eyes narrowed in a smile, crow's feet
            ex = 3 if side == 0 else fw - 3 - 5
            for i in range(5):
                arch = 1 if 1 <= i <= 3 else 0
                p.px((fx + ex + i, fy + ey - arch), (52, 34, 30))
                p.px((fx + ex + i, fy + ey + 1 - arch), k(skin, 0.86))
            ox = ex - 1 if side == 0 else ex + 5
            for d in range(2):
                p.px((fx + ox - d * (1 if side == 0 else -1), fy + ey + d), shade)
            for i in range(4):                                     # lines across the brow
                for jj in (2, 4):
                    p.px((fx + int(cx) - 4 + i * 2, fy + jj), k(skin, 0.84))
        for j in range(9, 12):
            p.px((fx + 4 - (j - 9) // 2, fy + j), shade)
            p.px((fx + fw - 5 + (j - 9) // 2, fy + j), shade)
        for i in range(6, fw - 6):
            p.px((fx + i, fy + fh - 1), LIPS)
        return

    if kind == 'man':
        for j in range(2, 5):                                      # a furrow between the brows
            p.px((fx + int(cx) - 1, fy + j), shade)
        for side in (0, 1):                                        # eyes: calm, narrowed, dark
            ex = 3 if side == 0 else fw - 3 - 5
            for i in range(5):
                p.px((fx + ex + i, fy + ey - 1), k(shade, 0.7))
                p.px((fx + ex + i, fy + ey), (238, 232, 218) if i not in (2, 3) else o.get('eye', (34, 22, 20)))
                p.px((fx + ex + i, fy + ey + 1), k(skin, 0.86))
            p.px((fx + ex + (2 if side == 0 else 3), fy + ey), (10, 6, 6))
        for j in range(9, 12):                                     # lines from the nose
            p.px((fx + 5 - (j - 9) // 2, fy + j), shade)
            p.px((fx + fw - 6 + (j - 9) // 2, fy + j), shade)
        for i in range(6, fw - 6):
            p.px((fx + i, fy + fh - 1), LIPS)
        return

    # fierce and demon: great round eyes glaring, glowing; a deep scowl
    iris = o.get('eye', (255, 200, 60))
    for j in range(1 + hairline, ey - 2):                          # the scowl's furrows
        p.px((fx + int(cx) - 1, fy + j), shade)
        p.px((fx + int(cx), fy + j), k(skin, 0.86))
    for side in (0, 1):
        ex = 2 if side == 0 else fw - 2 - 6
        for i in range(6):
            for jj in range(-1, 3):
                edge = (i in (0, 5) and jj in (-1, 2))
                if edge:
                    continue
                c = (250, 244, 230) if jj in (-1, 2) or i in (0, 5) else iris
                if jj == -1:
                    c = (24, 12, 12)
                p.lit((fx + ex + i, fy + ey + jj), c, glow=0.0 if jj == -1 else 0.9)
        px_ = ex + (3 if side == 0 else 2)
        p.px((fx + px_, fy + ey + 1), (10, 4, 4))
        p.px((fx + px_, fy + ey), (10, 4, 4))
        p.glow.putpixel((fx + px_, fy + ey + 1), (0, 0, 0, 0))
        p.glow.putpixel((fx + px_, fy + ey), (0, 0, 0, 0))
        for i in range(6):                                         # the lower lid's shadow
            p.px((fx + ex + i, fy + ey + 3), shade)
    if o.get('third'):                                             # a third eye, upright, on the brow
        tx = int(cx) - 1
        for jj in range(4):
            for ii in range(2):
                p.lit((fx + tx + ii, fy + 1 + hairline + jj), iris if 0 < jj < 3 else (24, 12, 12), glow=0.9 if 0 < jj < 3 else 0)
    for j in range(ey + 3, fh - 1):                                # flared nostril shadow and snarl lines
        p.px((fx + 3 - (j - ey - 3) // 2, fy + j), shade)
        p.px((fx + fw - 4 + (j - ey - 3) // 2, fy + j), shade)
    for i in range(4, fw - 4):                                     # the mouth: a dark snarl
        p.px((fx + i, fy + fh - 1), (60, 16, 16))
    if kind == 'demon':
        for i in (5, fw - 6):                                      # fangs over the lip
            p.px((fx + i, fy + fh - 1), IVORY)
            p.px((fx + i, fy + fh - 2), IVORY)


def brow_box(p, f, tone):
    lock(p, f, along_x=True, tone=tone, grey=0.0)


def beard_lock(p, f, tone, keep=0.5, taper=0.35):
    lock(p, f, keep=keep, taper=taper, tone=tone, grey=0.0)
