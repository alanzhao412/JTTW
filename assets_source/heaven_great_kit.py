"""What Heaven's great names share (heaven_great.py and its heaven_great_*.py): a Painter with a third
skin, the blaze (drawn full-bright in place of the glow while the boss winds up his great blow), and
the brushes their armour, silk and faces are painted with: polished gold and silver, lacquered
lamellae, fish scales and the mountain-pattern (山文甲), silk falling in folds, gold borders with
key-frets and cloud scrolls, round breast mirrors, lions' masks, and a god's fierce face.

Faces are named as beastgen names them: 'front' looks forward (-z), 'right' out of the creature's right
side (-x), 'top' up. On the front, texture columns run from the creature's right to its left (as one
before him sees it); on 'right' his front is at the face's right end, on 'left' at its left end."""
import math

import beastgen as bg
import wild_paint as wp
from wild_paint import k, mix, clamp, vnoise, each, part, mirrored, aim, paint, emit, report  # noqa: F401
from sea_court import fabric, fret, cloud, clouds  # noqa: F401
from king_yama import lock, point, BLUE_BLACK, BROWN_BLACK, ASH  # noqa: F401
from PIL import Image

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/HeavenGreatRenderers.java'
OUT = 'src/main/resources/assets/jttw_engine/textures/entity/'

SIDES = ('front', 'back', 'right', 'left')
LIGHT = {'top': 1.12, 'front': 1.0, 'right': 0.9, 'left': 0.9, 'back': 0.8, 'bottom': 0.6}

GOLD = (232, 184, 66)
GOLD_DEEP = (150, 100, 32)
GOLD_LIGHT = (255, 236, 150)
GOLD_SHADOW = (96, 60, 20)
SILVER = (206, 212, 224)
SILVER_DEEP = (110, 118, 136)
SILVER_LIGHT = (250, 252, 255)
STEEL = (180, 188, 200)
IVORY = (238, 228, 204)
PEARL = (240, 244, 255)
BLACK = (22, 18, 22)
HAIR = (18, 14, 16)
RED = (186, 30, 28)
RED_DEEP = (106, 14, 16)
JADE = (96, 182, 140)
JADE_DEEP = (40, 110, 80)
FIRE_DEEP = (170, 30, 8)
FIRE = (240, 96, 24)
FIRE_HOT = (255, 206, 90)
FIRE_WHITE = (255, 246, 200)


def noisy(p, c, n=3):
    return [v + p.rnd.randint(-n, n) for v in c[:3]]


class GreatPainter(wp.Painter):
    """wild_paint's Painter with a third skin: the blaze, the glow as it burns in his wind-up (all the glow
    skin has, brighter, and what only burns then)."""

    def __init__(self, seed):
        super().__init__(seed)
        self.blaze = Image.new('RGBA', (bg.TEX, bg.TEX), (0, 0, 0, 0))

    def px(self, p, c, glow=False):
        super().px(p, c, glow)
        if glow:
            x, y = int(p[0]), int(p[1])
            if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
                self.blaze.putpixel((x, y), self.glow.getpixel((x, y)))

    def clear(self, xy):
        super().clear(xy)
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            self.blaze.putpixel((x, y), (0, 0, 0, 0))

    def lit(self, xy, colour, glow=1.0, skin=None, blaze=None, hot=None):
        """A shining pixel: `skin` (or the colour) on the skin, the colour at `glow` on the glow skin, and
        `hot` (or the colour) at `blaze` (or glow) on the blaze."""
        x, y = int(xy[0]), int(xy[1])
        if not (0 <= x < bg.TEX and 0 <= y < bg.TEX):
            return
        self.px(xy, skin if skin is not None else colour)
        q = lambda c, f: tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)
        self.glow.putpixel((x, y), q(colour, glow))
        self.blaze.putpixel((x, y), q(hot or colour, glow if blaze is None else blaze))

    def flare(self, xy, colour, f=1.0):
        """A pixel that burns only in the wind-up (the blaze alone)."""
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            self.blaze.putpixel((x, y), tuple(max(0, min(255, int(v * f))) for v in colour[:3]) + (255,))

    def save(self, name):
        self.img.save(OUT + name + '.png')
        self.glow.save(OUT + name + '_glow.png')
        self.blaze.save(OUT + name + '_blaze.png')


# ── Metals ───────────────────────────────────────────────────────────────────

def polished(p, f, hi, base, lo, faces=None, round_=0.5, streak=True, rim=None):
    """Polished metal: each face lit toward its top and rounded across (light down a band off its middle,
    falling to shadow at its edges), a bright glint along its top edge, its foot in shadow."""
    for s in faces or f:
        fx, fy, fw, fh = f[s]
        L = LIGHT[s]
        for i, j, xy in each(p, f[s]):
            v = j / max(1, fh - 1)
            u = (i + 0.5) / fw
            if s in ('top', 'bottom'):
                t = 0.75 + 0.25 * (1 - abs(v - 0.5) * 2)
            else:
                t = 1.05 - 0.55 * v
            if round_:
                band = 1 - min(1, abs(u - 0.38) * 2.4)
                t += round_ * (band * 0.45 - 0.2)
            t *= L
            c = mix(lo, base, clamp(t * 1.6)) if t < 0.62 else mix(base, hi, clamp((t - 0.62) * 2.2))
            p.px(xy, noisy(p, c, 3))
        if s not in ('top', 'bottom') and fh > 2:
            for i in range(fw):
                p.px((fx + i, fy), mix(hi, (255, 255, 255), 0.3))
                p.px((fx + i, fy + fh - 1), rim or lo)


def gold(p, f, faces=None, round_=0.5):
    polished(p, f, GOLD_LIGHT, GOLD, GOLD_DEEP, faces, round_, rim=GOLD_SHADOW)


def silver(p, f, faces=None, round_=0.5):
    polished(p, f, SILVER_LIGHT, SILVER, SILVER_DEEP, faces, round_, rim=(70, 76, 92))


def enamel(p, f, base, faces=None, round_=0.5):
    """A lacquered or enamelled plate in a colour: polished, its highlight paler."""
    polished(p, f, mix(k(base, 1.35), (255, 244, 220), 0.22), base, k(base, 0.45), faces, round_)


def gold_rim(p, f, sides=SIDES, top=True, bottom=True, width=1, colour=None):
    for s in sides:
        fx, fy, fw, fh = f[s]
        for i in range(fw):
            for w in range(width):
                c = colour or (GOLD_LIGHT if w == 0 else GOLD)
                if top:
                    p.px((fx + i, fy + w), c)
                if bottom:
                    p.px((fx + i, fy + fh - 1 - w), GOLD_DEEP if w == 0 and colour is None else c)


def frame(p, rect, colour=GOLD, inner=GOLD_DEEP, width=1):
    fx, fy, fw, fh = rect
    for i, j, xy in each(p, rect):
        e = min(i, j, fw - 1 - i, fh - 1 - j)
        if e < width:
            p.px(xy, colour)
        elif e == width and inner:
            p.px(xy, inner)


def studs(p, rect, y, every=4, colour=GOLD_LIGHT, shade=GOLD_DEEP, x0=2):
    fx, fy, fw, fh = rect
    for i in range(x0, fw - 1, every):
        p.px((fx + i, fy + y), colour)
        p.px((fx + i + 1, fy + y + 1), shade)


# ── Armour ───────────────────────────────────────────────────────────────────

def lamellae(p, rect, plate, lace, pw=4, ph=5, light=1.0, edge=None, gleam=None):
    """Lamellar armour: plates in staggered rows, each rounded (lit down its middle, a gleam at its top,
    dark at its edges and foot), laced to the row below at its middle."""
    fx, fy, fw, fh = rect
    edge = edge or k(plate, 0.38)
    gleam = gleam or mix(plate, (255, 255, 255), 0.5)
    for j in range(fh):
        row, y = j // ph, j % ph
        off = (row % 2) * (pw // 2)
        for i in range(fw):
            x = (i + off) % pw
            across = abs(x + 0.5 - pw / 2) / (pw / 2)
            kk = (1.2 - 0.4 * across - 0.35 * y / max(1, ph - 1)) * light
            c = k(plate, kk)
            if y == 0 and across < 0.6:
                c = k(gleam, light)
            if x == 0:
                c = k(edge, light)
            if y == ph - 1:
                c = k(lace, light) if x in (pw // 2 - 1, pw // 2) else k(edge, light * 0.8)
            p.px((fx + i, fy + j), noisy(p, c, 2))


def fish_scales(p, rect, base, edge, light=1.0, cw=4, ch=3):
    wp.scale_tiles(p, rect, base, edge, light=light, cw=cw, ch=ch, vary=0.1, hi=mix(base, (255, 255, 255), 0.5))


def mountain(p, rect, base, deep, hi, light=1.0, cell=6):
    """The mountain-pattern armour (山文甲): small plates each shaped like 山 (a Y-branched star),
    interlocking in a lattice; each plate lit along its upper arms, dark in the seams between."""
    fx, fy, fw, fh = rect
    for i, j, xy in each(p, rect):
        row = j // (cell // 2 + 1)
        x = (i + (cell // 2 if row % 2 else 0)) % cell
        y = j % (cell // 2 + 1)
        cx = abs(x - (cell - 1) / 2)
        seam = (y == cell // 2) or (cx >= (cell - 1) / 2 - 0.1 and y < cell // 2) or (abs(cx - (y * 0.7)) < 0.4 and y > 0)
        if seam:
            c = k(deep, 0.75 * light)
        elif y == 0:
            c = k(hi, light)
        else:
            c = k(mix(hi, base, y / (cell // 2)), light * (1.0 - 0.06 * cx))
        p.px(xy, noisy(p, c, 2))


def mirror_plate(p, rect, rim=GOLD, glass=(150, 170, 200), deep=(54, 60, 80), glow=0.0):
    """A round breast-mirror (护心镜): a gold rim beaded and a polished disc, lit toward its upper left;
    clear outside the circle."""
    fx, fy, fw, fh = rect
    cx, cy, R = fx + fw / 2, fy + fh / 2, min(fw, fh) / 2
    for i, j, (x, y) in each(p, rect):
        d = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / R
        if d > 1.0:
            p.clear((x, y))
            continue
        if d > 0.78:
            a = math.atan2(y + 0.5 - cy, x + 0.5 - cx)
            c = GOLD_LIGHT if int((a + math.pi) * 5) % 2 == 0 and d < 0.92 else rim if d < 0.92 else GOLD_DEEP
            p.px((x, y), noisy(p, c, 3))
            continue
        hl = clamp(1 - math.hypot(x + 0.5 - (cx - R * 0.3), y + 0.5 - (cy - R * 0.3)) / (R * 0.7))
        c = mix(deep, glass, clamp(1 - d * 0.6))
        c = mix(c, (250, 252, 255), hl * 0.85)
        if glow:
            p.lit((x, y), c, glow * (0.4 + hl))
        else:
            p.px((x, y), noisy(p, c, 2))
    if R > 3:
        wp.disc(p, cx, cy, max(1.0, R * 0.18), GOLD_LIGHT)


def lion(p, rect, metal=GOLD, deep=GOLD_DEEP, hi=GOLD_LIGHT, eye=(230, 60, 30), mouth=(60, 14, 12), teeth=IVORY,
         open_=0.35, glow=0.6, mane=True):
    """A lion's mask (吞口) in gold filling a face: a ring of mane in curls, heavy brows over round glaring
    eyes, a broad flat nose, puffed cheeks, and the mouth gaping in its lower part, fangs at its
    corners (what it bites shows dark within)."""
    fx, fy, fw, fh = rect
    cx = fw / 2
    mouth_top = fh * (1 - open_)
    for i, j, xy in each(p, rect):
        u = (i + 0.5 - cx) / (fw / 2)                    # -1 .. 1 across
        v = (j + 0.5) / fh                                # 0 .. 1 down
        r = math.hypot(u, (v - 0.5) * 2)
        c = metal
        if mane and r > 0.82:                              # the mane: curls about the rim
            a = math.atan2(v - 0.5, u)
            curl = 0.5 + 0.5 * math.sin(a * 7 + r * 9)
            c = mix(deep, hi, curl * 0.8)
        else:
            t = 1.1 - 0.5 * v - 0.25 * abs(u)
            c = mix(deep, metal, clamp(t * 1.4)) if t < 0.7 else mix(metal, hi, clamp((t - 0.7) * 2.5))
        p.px(xy, noisy(p, c, 3))
    # brows: two heavy arcs knit at the middle
    for i in range(fw):
        u = (i + 0.5 - cx) / (fw / 2)
        if abs(u) < 0.8:
            y = int(fh * (0.18 + 0.1 * abs(u)))
            p.px((fx + i, fy + y), hi)
            p.px((fx + i, fy + y + 1), deep)
    # eyes: round, glaring
    ew = max(2, int(fw * 0.18))
    for side in (-1, 1):
        ex = int(cx + side * fw * 0.24 - ew / 2)
        ey = int(fh * 0.3)
        for a in range(ew):
            for b in range(max(2, ew - 1)):
                c = eye if not (a == ew // 2 and b >= 1) else (20, 8, 6)
                if glow:
                    p.lit((fx + ex + a, fy + ey + b), c, glow)
                else:
                    p.px((fx + ex + a, fy + ey + b), c)
        for a in range(-1, ew + 1):
            p.px((fx + ex + a, fy + ey - 1), GOLD_SHADOW)
    # the nose: broad, flat, nostrils dark
    nw = max(3, int(fw * 0.3))
    ny = int(fh * 0.5)
    for a in range(nw):
        p.px((fx + int(cx - nw / 2) + a, fy + ny), hi)
        p.px((fx + int(cx - nw / 2) + a, fy + ny + 1), metal if a not in (0, nw - 1) else (40, 20, 10))
    # the mouth, gaping: dark within, a row of teeth along its top and fangs at its corners
    if open_ > 0:
        mw = int(fw * 0.62)
        mx = int(cx - mw / 2)
        for a in range(mw):
            for b in range(int(mouth_top), fh - 1):
                p.px((fx + mx + a, fy + b), mouth)
            p.px((fx + mx + a, fy + int(mouth_top)), teeth if a % 2 == 0 else k(teeth, 0.7))
            p.px((fx + mx + a, fy + int(mouth_top) - 1), deep)
        for a in (0, mw - 1):
            for b in range(int(mouth_top), min(fh - 1, int(mouth_top) + 3)):
                p.px((fx + mx + a, fy + b), teeth)


# ── Cloth ────────────────────────────────────────────────────────────────────

def silk(p, f, base, deep=None, fold=7, fall=0.35, faces=None):
    deep = deep or k(base, 0.55)
    for s in faces or f:
        r = f[s]
        if s == 'bottom':
            p.face(r, k(deep, 0.9), 1.0, noise=2)
        elif s == 'top':
            fabric(p, r, base, deep, 1.04, fold, fall=0.1)
        else:
            fabric(p, r, base, deep, LIGHT[s], fold, fall)


def hem(p, rect, rows, band, line=GOLD, deep=GOLD_DEEP, at_bottom=True):
    """A border along a face's foot (or head): a band of `band` between gold lines."""
    fx, fy, fw, fh = rect
    rows = min(rows, fh - 2)
    for j in range(rows):
        y = fy + fh - 1 - j if at_bottom else fy + j
        for i in range(fw):
            p.px((fx + i, y), noisy(p, band, 3))
    for i in range(fw):
        y0 = fy + fh - 1 if at_bottom else fy
        y1 = fy + fh - rows if at_bottom else fy + rows - 1
        p.px((fx + i, y0), line)
        p.px((fx + i, y1), line)
        y2 = y1 - 1 if at_bottom else y1 + 1
        if fy <= y2 < fy + fh:
            p.px((fx + i, y2), deep)


def scroll_band(p, rect, y, colour, shade, period=8):
    """A running cloud-scroll (卷草) along a face at row y: a wave with a curl in each trough."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        t = (i % period) / period
        yy = y + round(1.4 * math.sin(t * 2 * math.pi))
        p.px((fx + i, fy + yy), colour)
        p.px((fx + i, fy + yy + 1), shade)
        if i % period == period // 2:
            for a, b in ((0, -2), (1, -2), (1, -1)):
                p.px((fx + i + a, fy + yy + b), colour)


def ribbon(p, f, base, deep, edge=GOLD, faces=None):
    """A silk streamer: lit silk, a gold thread down each long edge."""
    for s in faces or f:
        fx, fy, fw, fh = f[s]
        for i, j, xy in each(p, f[s]):
            u = (i + 0.5) / fw
            t = 1.08 - 0.3 * abs(u - 0.4) * 2
            c = mix(deep, base, clamp(t * LIGHT[s]))
            if s in ('front', 'back') and (i == 0 or i == fw - 1):
                c = edge
            p.px(xy, noisy(p, c, 3))


# ── Faces ────────────────────────────────────────────────────────────────────

def skin_shade(face, t):
    """A face's colour at t: 0 deep shadow .. 1 its colour .. 2 its highlight."""
    shadow = mix(k(face, 0.55), (60, 20, 30), 0.15)
    light = mix(face, (255, 250, 240), 0.35)
    return mix(shadow, face, clamp(t)) if t < 1 else mix(face, light, clamp(t - 1))


def flesh(p, f, face, faces=None, round_=0.6):
    """Skin over a box: lit above and across its middle, shadowed to its edges and beneath."""
    for s in faces or f:
        fx, fy, fw, fh = f[s]
        for i, j, xy in each(p, f[s]):
            u = abs((i + 0.5) / fw - 0.5) * 2
            v = j / max(1, fh - 1)
            t = 1.15 - round_ * u ** 2 - 0.25 * v
            t *= LIGHT[s]
            p.px(xy, noisy(p, skin_shade(face, t), 2))


def eyeball(p, f, iris, white=(244, 238, 226), lid=(30, 16, 18), glow=0.0, look=0, blaze=None):
    """A bulging eye's front: white, a big round iris ringed dark with its pupil, the upper lid heavy over
    it, a catch-light; its sides the lid's shade."""
    for s in f:
        if s != 'front':
            p.face(f[s], k(lid, 1.6), 1.0, noise=2)
    fx, fy, fw, fh = f['front']
    cx, cy = fx + fw / 2 + look, fy + fh / 2 + 0.3
    R = min(fw * 0.32, fh * 0.55)
    for i, j, (x, y) in each(p, f['front']):
        d = math.hypot(x + 0.5 - cx, (y + 0.5 - cy) * 1.1)
        if j == 0:
            p.px((x, y), lid)
        elif d < R * 0.45:
            p.px((x, y), (10, 6, 8))
            p.glow.putpixel((x, y), (0, 0, 0, 255)) if glow else None
        elif d < R:
            c = mix(k(iris, 1.25), k(iris, 0.75), d / R)
            if glow:
                p.lit((x, y), c, glow, blaze=blaze)
            else:
                p.px((x, y), c)
        elif d < R + 0.7:
            p.px((x, y), k(iris, 0.4))
        else:
            edge = min(i, fw - 1 - i)
            p.px((x, y), k(white, 0.82 if edge == 0 or j == fh - 1 else 1.0))
    p.px((int(cx - R * 0.4), int(cy - R * 0.4)), (255, 255, 255))
