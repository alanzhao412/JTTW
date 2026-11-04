"""The common kit of Heaven's rank and file (heaven_host.py): the palette, the painter, the painting
helpers, and the trooper's frame they all share (a man of the host in gilt Tang armour: the
mingguang cuirass with its two round mirror plates, a beast's face on the belly, shoulder guards in
three tiers each bitten by a gold beast's head, a lamellar skirt over silk trousers, greaves and
upturned boots; a helmet with ear flaps, a neck guard, a gold finial and a red horsehair tassel),
and the painting of it, in each creature's own colours.

A part is beastgen's (name, parent, pivot, rotation, boxes, mirror); the model space has the ground at
y 24 and the face toward -z; a 'right_' part is mirrored to a 'left_' one."""
import math

import beastgen as bg
from beastgen import Painter, segment
from fire_court import part, mirrored
from sea_court import aim, fabric, cloud, clouds, plane
from king_yama import paint, point, ragged, circle, mixc, k, noisy, lock, BROWN_BLACK, BLUE_BLACK

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

OUT = 'src/main/resources/assets/jttw_engine/textures/entity/'

# ── The palette ──────────────────────────────────────────────────────────────

GOLD = (236, 188, 70)
GOLD_LIGHT = (255, 236, 156)
GOLD_DEEP = (164, 110, 34)
GOLD_DARK = (98, 60, 20)
SILVER = (214, 222, 236)
SILVER_DEEP = (118, 128, 150)
STEEL = (196, 204, 218)
STEEL_DARK = (98, 104, 120)
IVORY = (240, 232, 212)
BLACK = (26, 22, 28)
BOOT = (34, 28, 30)
WOOD = (120, 74, 40)
LACQUER_RED = (176, 30, 26)
PEARL = (236, 246, 255)
CLOUD = (246, 248, 255)
CLOUD_SHADE = (196, 206, 228)

SKIN = (240, 196, 158)
SKIN_SHADE = (204, 146, 112)
SKIN_LIGHT = (252, 222, 194)
LIPS = (172, 82, 70)
HAIR = (24, 20, 22)

LIGHTS = {'top': 1.1, 'bottom': 0.62, 'front': 1.0, 'back': 0.84, 'right': 0.92, 'left': 0.92}
SIDES = ('front', 'back', 'right', 'left')


def colours(silk, silk_deep, trim, plume, plume_deep, lace, cloak=None, cloak_deep=None, **more):
    """A creature's colours: its silk (robe, sleeves, trousers), the silk's border, the horsehair tassel
    on its helmet, the lacing of its armour, a captain's cloak; and whatever more it needs."""
    c = dict(silk=silk, silk_deep=silk_deep, trim=trim, plume=plume, plume_deep=plume_deep, lace=lace,
             cloak=cloak or silk, cloak_deep=cloak_deep or silk_deep, plate=GOLD, plate_deep=GOLD_DEEP,
             skin=SKIN, skin_shade=SKIN_SHADE, captain=False)
    c.update(more)
    return c


# ── The painter ──────────────────────────────────────────────────────────────

class HostPainter(Painter):
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
        self.glow.putpixel((x, y), tuple(max(0, min(255, int(v * glow))) for v in colour[:3]) + (255,))

    def get(self, xy):
        return self.img.getpixel((int(xy[0]), int(xy[1])))

    def save_as(self, name, glow=True):
        self.img.save(OUT + name + '.png')
        if glow:
            self.glow.save(OUT + name + '_glow.png')


# ── Painting helpers ─────────────────────────────────────────────────────────

def each(rect):
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            yield i, j, (fx + i, fy + j)


def rim(p, rect, colour, sides='tblr', width=1):
    fx, fy, fw, fh = rect
    for w in range(width):
        for i in range(fw):
            if 't' in sides:
                p.px((fx + i, fy + w), colour)
            if 'b' in sides:
                p.px((fx + i, fy + fh - 1 - w), colour)
        for j in range(fh):
            if 'l' in sides:
                p.px((fx + w, fy + j), colour)
            if 'r' in sides:
                p.px((fx + fw - 1 - w, fy + j), colour)


def gilt(p, f, light=1.0, base=GOLD, sides=None):
    """Cast gilt metal: a gleam at its top, deepening down each face, a dark foot; flecks of light."""
    for s, (fx, fy, fw, fh) in f.items():
        if sides and s not in sides:
            continue
        for j in range(fh):
            for i in range(fw):
                t = j / max(1, fh - 1)
                c = mixc(GOLD_LIGHT, base, min(1, t * 2.2)) if t < 0.45 else mixc(base, GOLD_DEEP, (t - 0.45) / 0.55)
                if s in ('top', 'bottom'):
                    c = base
                if (i * 7 + j * 5) % 13 == 0:
                    c = k(c, 1.1)
                p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s] * light), 3))


def gilt_rounded(p, f, light=1.0, base=GOLD):
    """Gilt metal rounded across each face: bright down its middle, dark to its edges."""
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                a = abs((i + 0.5) / fw - 0.42) * 2
                t = 1.25 - 0.55 * a * a - 0.25 * j / max(1, fh - 1)
                c = mixc(base, GOLD_LIGHT, max(0, t - 1) * 2.2) if t > 1 else mixc(GOLD_DEEP, base, max(0, t - 0.5) * 2)
                p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s] * light), 3))


def steel(p, f, base=STEEL, deep=STEEL_DARK):
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                a = (i + 0.5) / fw
                c = mixc((246, 250, 255), base, min(1, a * 2.4)) if a < 0.45 else mixc(base, deep, (a - 0.45) / 0.55)
                p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 2))


def silk(p, f, base, deep, light=1.0, fold=6, fall=0.4):
    """Silk over a box: lit above, folds down its sides, the bottom in shade."""
    for s, r in f.items():
        if s == 'bottom':
            p.face(r, k(deep, 0.9), 1.0, noise=2)
        elif s == 'top':
            fabric(p, r, base, deep, 1.06 * light, fold, fall=0.1)
        else:
            fabric(p, r, base, deep, LIGHTS[s] * light, fold, fall)


def lamellae(p, rect, c, light=1.0, pw=3, ph=4, base=None, deep=None, lace=None, top_rim=True, bottom_rim=True):
    """Gilt lamellar armour: little plates in staggered rows, each rounded (lit down its middle, a gleam at
    its top, dark at its edges and foot), laced to the next row at its middle."""
    base = base or c['plate']
    deep = deep or c['plate_deep']
    lace = lace or c['lace']
    fx, fy, fw, fh = rect
    for j in range(fh):
        row, y = j // ph, j % ph
        off = (row % 2) * (pw // 2 + 1)
        for i in range(fw):
            x = (i + off) % pw
            across = abs(x + 0.5 - pw / 2) / (pw / 2)
            kk = (1.18 - 0.38 * across - 0.32 * y / max(1, ph - 1)) * light
            col = k(base, kk)
            if y == 0 and across < 0.7:
                col = k(mixc(base, GOLD_LIGHT, 0.6), light * 1.05)
            if x == pw - 1:
                col = k(deep, 0.65 * light)
            if y == ph - 1:
                col = k(lace, light) if x == pw // 2 and row % 2 == 0 else k(deep, 0.5 * light)
            p.px((fx + i, fy + j), noisy(p, col, 3))
    if top_rim:
        for i in range(fw):
            p.px((fx + i, fy), k(GOLD_LIGHT, light))
            p.px((fx + i, fy + 1), k(GOLD_DEEP, light))
    if bottom_rim:
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), k(GOLD_DARK, light))
            p.px((fx + i, fy + fh - 2), k(GOLD, light))


def lamellar_box(p, f, c, ends=True, **kw):
    for s, r in f.items():
        if s in ('top', 'bottom'):
            p.face(r, k(c['plate_deep'], 0.9 if s == 'top' else 0.6), 1.0, noise=3)
            if ends:
                rim(p, r, GOLD)
        else:
            lamellae(p, r, c, light=LIGHTS[s], **kw)


def scale_armour(p, rect, c, light=1.0, base=None):
    """Fish-scale armour (鱼鳞甲): rounded scales in staggered rows, each lit at its crown, its edge dark."""
    base = base or c['plate']
    fx, fy, fw, fh = rect
    for j in range(fh):
        row = j // 3
        for i in range(fw):
            x = (i + (row % 2) * 2) % 4
            y = j % 3
            d = math.hypot((x - 1.5) / 2, (y - 0.3) / 3)
            col = k(base, (1.3 - 0.65 * d) * light)
            if d > 0.85 or (y == 2 and x in (0, 3)):
                col = k(c['plate_deep'], 0.75 * light)
            p.px((fx + i, fy + j), noisy(p, col, 3))


def mirror_disc(p, rect, rimc=GOLD, deep=GOLD_DEEP, glass=SILVER, glow=0.0):
    """A round mirror plate (护心镜): polished, a light catching its upper left, a gold rim and boss; clear outside."""
    fx, fy, fw, fh = rect
    cx, cy, R = fx + fw / 2, fy + fh / 2, min(fw, fh) / 2
    for y in range(fy, fy + fh):
        for x in range(fx, fx + fw):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / R
            if d > 1.04:
                p.clear((x, y))
            elif d > 0.74:
                p.px((x, y), GOLD_LIGHT if (y - fy) < fh * 0.4 else rimc if d < 0.9 else deep)
            else:
                u, v = (x - fx) / fw, (y - fy) / fh
                hl = 1.0 - 0.9 * math.hypot(u - 0.35, v - 0.35)
                col = mixc(k(glass, 0.55), (255, 255, 255), max(0, min(1, hl)))
                if glow and hl > 0.55:
                    p.lit((x, y), col, glow=glow)
                else:
                    p.px((x, y), noisy(p, col, 3))
    circle(p, cx, cy, 0, max(1.0, R * 0.22), GOLD_LIGHT)


def beast_mask(p, rect, base=GOLD, eyes=(200, 40, 30), ring=False):
    """A gold beast's face (吞口), filling a face: a heavy brow, round glaring eyes, a broad nose, a mouth
    gaping on fangs (with `ring`, a ring hanging from its jaws)."""
    fx, fy, fw, fh = rect
    gilt(p, {'front': rect})
    D, dk = GOLD_DEEP, GOLD_DARK
    cx = fw / 2
    for i in range(fw):                                   # the mane round it
        p.px((fx + i, fy), D)
        p.px((fx + i, fy + fh - 1), D)
    for j in range(fh):
        p.px((fx, fy + j), D)
        p.px((fx + fw - 1, fy + j), D)
    ey = fy + max(1, int(fh * 0.28))
    for side in (-1, 1):                                  # brows and eyes
        ex = int(cx + side * max(1, fw * 0.22) - (1 if side < 0 else 0))
        p.px((ex, ey - 1), dk)
        p.px((ex - side, ey - 1), dk)
        p.px((ex, ey), eyes)
        p.px((ex, ey + 1), dk)
    nx = int(cx - 0.5)
    p.px((nx, ey + 1), GOLD_LIGHT)
    p.px((nx + (1 if fw % 2 == 0 else 0), ey + 1), GOLD_LIGHT)
    my = min(fy + fh - 2, ey + 3)                         # the mouth
    for i in range(1, fw - 1):
        p.px((fx + i, my), (52, 18, 10))
        if i in (1, fw - 2):
            p.px((fx + i, my), IVORY)
    if ring and fh > 6:
        circle(p, fx + cx, my + 2.2, 1.0, 2.0, GOLD_LIGHT)


def face_front(p, rect, c, beard=None, stern=True, glow_eyes=None, moustache=False):
    """A man's face on the front of a head box (some 14 x 14): the forehead under the helmet band, level
    brows (the brows are parts of their own), eyes of flat amber iris with a thin pupil under a dark lid, cheekbones in
    light, a mouth set firm, the chin, a shadow at the jaw."""
    fx, fy, fw, fh = rect
    sk, sh = c['skin'], c['skin_shade']
    for i, j, xy in each(rect):
        a = abs((i + 0.5) / fw - 0.5) * 2
        col = mixc(sk, SKIN_LIGHT, 0.3) if (j < 5 and a < 0.5) or (5 <= j <= 8 and 0.3 < a < 0.7) else sk
        col = mixc(col, sh, max(0, a - 0.72) * 1.2 + max(0, j - fh + 3) * 0.15)
        p.px(xy, noisy(p, col, 1))
    cx = fw // 2
    ey = int(fh * 0.42)
    iris = glow_eyes or c.get('iris', (156, 96, 36))
    pupil = (20, 12, 10)
    lash = (52, 30, 24)
    for side in (0, 1):
        # the eyes, as the game's own: a flat block of iris four wide and two high, a thin pupil inside it
        # (iris either side of it), set a little toward the nose: a level, hard stare. The upper lid's dark
        # line is drawn down over the inner corner, so the eye slants down to the nose; the outer end of
        # the lid flicks up. No whites.
        out_x, in_x = (fx + 1, fx + 4) if side == 0 else (fx + fw - 2, fx + fw - 5)
        step = 1 if side == 0 else -1
        for n in range(4):
            x = out_x + step * n
            for j in (ey, ey + 1):
                if j == ey and n == 3:
                    p.px((x, fy + j), lash)               # the lid down over the inner corner
                elif n == 2:
                    p.px((x, fy + j), pupil)
                elif glow_eyes:
                    p.lit((x, fy + j), iris, glow=0.9)
                else:
                    p.px((x, fy + j), iris if j == ey + 1 or n else k(iris, 0.8))
            p.px((x, fy + ey - 1), lash if n < 3 else k(sh, 0.8))    # the upper lid
            p.px((x, fy + ey + 2), mixc(sk, sh, 0.45))                  # the lower lid's shadow
        p.px((out_x - step, fy + ey - 1), k(sh, 0.7))                  # the outer corner raised: a fierce eye
        p.px((out_x - step, fy + ey - 2), lash)
        p.px((out_x - step, fy + ey), k(sh, 0.85))
    for j in range(ey - 2, ey + 1):                       # a furrow between the brows
        if stern:
            p.px((fx + cx, fy + j), k(sh, 0.92))
    my = fy + min(fh - 3, ey + 5)
    for i in range(cx - 2, cx + 3):                       # the mouth: a hard, level line, the lower lip only shaded
        p.px((fx + i, my), (100, 40, 36))
        p.px((fx + i, my + 1), mixc(LIPS, sk, 0.6))
    p.px((fx + cx - 3, my + 1), k(sh, 0.85))              # its corners pulled down: stern
    p.px((fx + cx + 3, my + 1), k(sh, 0.85))
    if moustache:
        for i in range(cx - 4, cx + 4):
            d = abs(i - cx + 0.5)
            p.px((fx + i, my - 1 + int(d > 2.5)), HAIR if d < 3.5 else (60, 46, 44))
    if beard:
        for i in range(cx - 2, cx + 2):
            for j in range(my + 2, fy + fh):
                p.px((fx + i, j), noisy(p, beard, 4))


def face_side(p, rect, c, front_at_right=True):
    """A head's side: skin before the ear, the ear, cropped dark hair behind (most of it under the helmet)."""
    fx, fy, fw, fh = rect
    sk, sh = c['skin'], c['skin_shade']
    for i, j, xy in each(rect):
        fwd = (i if front_at_right else fw - 1 - i) / max(1, fw - 1)     # 1 at the face, 0 at the back
        col = mixc(k(sk, 0.92), k(sh, 0.95), max(0, 0.55 - fwd))
        if fwd < 0.32:
            col = mixc(HAIR, (54, 44, 44), (j % 3) / 3)
        p.px(xy, noisy(p, col, 2))
    ex = fx + (int(fw * 0.42) if front_at_right else fw - 1 - int(fw * 0.42) - 2)
    for j in range(int(fh * 0.38), int(fh * 0.72)):                    # the ear
        for i in range(3):
            p.px((ex + i, fy + j), k(sk, 0.85) if i != 1 else sh)


def pointed(p, f, sides=SIDES, keep=0.5, sharp=0.6, down=False):
    for s in sides:
        point(p, f[s], keep=keep, sharp=sharp, down=down)


def bare(p, f, which):
    for s in which:
        for _, _, xy in each(f[s]):
            p.clear(xy)


def horsehair(p, rect, base, deep, light=1.0, cut=0.0, down=True):
    """Dyed horsehair (a helmet's tassel): fine strands, lit and dark by turns, ragged at the end."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        tone = p.rnd.uniform(0.8, 1.15)
        for j in range(fh):
            t = j / max(1, fh - 1) if down else 1 - j / max(1, fh - 1)
            col = mixc(k(base, 1.15), deep, 0.25 + 0.6 * t) if (i % 2 == 0) else mixc(base, deep, 0.45 + 0.5 * t)
            p.px((fx + i, fy + j), noisy(p, k(col, tone * light), 4))
    if cut:
        ragged(p, rect, teeth=2, depth=max(1, int(fh * cut)), down=down)


def cloud_scrolls(p, rect, colour, every=9, r=2, share=0.75):
    clouds(p, rect, colour, every=every, r=r, share=share)


def key_fret(p, rect, gold=GOLD, deep=GOLD_DEEP):
    """A gold border with a key fret (回纹) running along it, two pixels a step."""
    fx, fy, fw, fh = rect
    tile = ['11111', '10001', '10101', '10111', '10000']
    for j in range(fh):
        for i in range(fw):
            on = tile[min(4, j * 5 // max(1, fh))][i % 5] == '1' if fh >= 4 else (i + j) % 3 != 0
            p.px((fx + i, fy + j), noisy(p, gold if on else mixc(gold, deep, 0.7), 3))


def flame_mask(s, t, tongues=3):
    """A flame-shaped plaque: tongues rising, s across 0..1, t down 0..1."""
    w = 0.5 * (1 - (1 - t) ** 1.6)
    if abs(s - 0.5) > w + 0.02:
        return False
    wave = abs(math.sin((s * tongues) * math.pi))
    return t > 0.32 * (1 - wave) * (1 - t) * 2


# ── The trooper's frame ──────────────────────────────────────────────────────

# the right arm holding a polearm upright before it; the left at rest
ARM, FOREARM = (-0.22, 0, 0.1), (-0.95, -0.3, 0)
LARM, LFOREARM = (0.06, 0, -0.1), (-0.3, 0.12, 0)
HEAD_Y = -11.3                     # the head's pivot in the body


def legs(boot=BOOT):
    return [
        part('right_leg', 'root', (-2.1, 12, 0.2), (0, 0, 0), [(-2.0, -0.5, -2.1, 4.0, 6.8, 4.2, 'thigh')]),
        part('right_shin', 'right_leg', (0, 6, 0), (0, 0, 0), [
            (-1.75, 0, -1.75, 3.5, 4.6, 3.5, 'shin'),
            (-2.05, 0.7, -2.3, 4.1, 3.7, 2.2, 'greave'),
            (-1.95, -0.8, -2.65, 3.9, 2.0, 1.0, 'kneeguard'),
            (-1.95, 4.4, -2.2, 3.9, 1.6, 4.3, 'boot'),
            (-1.45, 4.9, -3.3, 2.9, 1.1, 1.3, 'boottoe'),
            (-0.8, 4.3, -3.85, 1.6, 0.9, 0.7, 'boottip'),
        ]),
    ]


def waist(skirt=7.0, apron=9.0):
    return [
        part('waist', 'root', (0, 11.5, 0), (0, 0, 0), [
            (-4.0, -1.5, -2.5, 8.0, 3.0, 5.0, 'hips'),
            (-4.7, -2.9, -3.1, 9.4, 1.9, 6.2, 'belt'),
            (-1.4, -3.15, -3.55, 2.8, 2.3, 0.6, 'buckle'),
        ]),
        part('apron', 'waist', (0, -1.3, -3.25), (-0.05, 0, 0), [(-1.4, 0, -0.3, 2.8, apron, 0.6, 'apron')]),
        part('robe_back', 'waist', (0, -0.9, 2.4), (0.1, 0, 0), [(-4.0, 0, -0.3, 8.0, apron + 1.2, 0.6, 'robe_back')]),
        part('right_tasset', 'waist', (-2.3, -1.3, -2.85), (-0.14, 0.14, 0.13), [(-2.4, 0, -0.4, 4.8, skirt, 0.8, 'tasset')]),
        part('right_side_tasset', 'waist', (-4.25, -1.3, 0.1), (0, 0, 0.27), [(-0.8, 0, -2.6, 0.8, skirt - 0.3, 5.2, 'tasset_side')]),
        part('back_tasset', 'waist', (0, -1.3, 2.75), (0.16, 0, 0), [(-4.3, 0, -0.4, 8.6, skirt + 0.2, 0.8, 'tasset_back')]),
        part('right_sash', 'waist', (-3.5, -2.0, -2.5), (-0.04, 0.2, 0.12), [(-0.6, 0, -0.3, 1.2, apron - 0.6, 0.6, 'sashend')]),
    ]


def body(mirrors=True):
    boxes = [
        (-4.0, -9.6, -2.6, 8.0, 9.8, 5.2, 'torso'),
        (-4.6, -10.0, -3.1, 9.2, 5.8, 6.2, 'cuirass'),
        (-4.2, -4.5, -2.85, 8.4, 4.4, 5.7, 'abdomen'),
        (-4.55, -5.0, -3.25, 9.1, 0.8, 6.5, 'chestband'),
        (-4.35, -0.9, -3.0, 8.7, 1.2, 6.0, 'waistsash'),
        (-1.9, -4.1, -3.35, 3.8, 3.2, 0.7, 'bellybeast'),
        (-3.0, -11.0, -2.5, 6.0, 1.4, 5.0, 'gorget'),
        (-3.3, -10.5, -2.9, 6.6, 1.0, 5.8, 'scarf'),
        (-1.3, -11.8, -1.3, 2.6, 1.4, 2.6, 'neck'),
    ]
    if mirrors:
        boxes += [(-0.45, -9.6, -3.35, 0.9, 4.8, 0.35, 'strap'),
                  (-0.9, -10.0, -3.55, 1.8, 1.4, 0.6, 'scarfknot'),
                  (-3.95, -9.3, -3.55, 3.1, 3.1, 0.5, 'mirror'), (0.85, -9.3, -3.55, 3.1, 3.1, 0.5, 'mirror')]
    return [part('body', 'waist', (0, -2.7, 0), (0, 0, 0), boxes)]


def arms(sleeve='sleeve'):
    return [
        part('right_arm', 'body', (-5.3, -8.6, 0), ARM, [(-1.8, -1.2, -1.9, 3.6, 5.2, 3.8, sleeve)]),
        part('right_pauldron', 'right_arm', (-0.3, -1.6, 0), (0, 0, 0.34), [
            (-2.4, -1.5, -2.3, 3.8, 1.0, 4.6, 'pauldron_cap'),
            (-3.0, -0.8, -2.75, 4.6, 1.4, 5.5, 'pauldron1'),
            (-3.5, 0.4, -3.05, 5.0, 1.4, 6.1, 'pauldron2'),
            (-4.0, 1.6, -3.4, 5.4, 1.6, 6.8, 'pauldron3'),
            (-4.6, 0.7, -1.5, 0.7, 2.8, 3.0, 'beastface'),
        ]),
        part('right_forearm', 'right_arm', (0, 3.8, 0), FOREARM, [
            (-1.4, 0, -1.4, 2.8, 4.3, 2.8, 'forearm'),
            (-1.75, 0.9, -1.75, 3.5, 3.2, 3.5, 'bracer'),
            (-2.2, -0.9, -2.2, 4.4, 2.0, 4.4, 'elbowpuff'),
        ]),
        part('right_hand', 'right_forearm', (0, 4.2, 0), (0, 0, 0), [(-1.2, 0, -1.2, 2.4, 2.4, 2.4, 'fist')]),
    ]


def head():
    return [
        part('head', 'body', (0, HEAD_Y, -0.1), (0, 0, 0), [
            (-3.3, -7.0, -3.3, 6.6, 7.0, 6.6, 'face'),
            (-0.55, -4.1, -3.85, 1.1, 1.8, 0.6, 'nose'),
        ]),
        part('right_brow', 'head', (-0.35, -5.05, -3.4), (0, 0.1, 0.2), [(-2.5, -0.4, -0.35, 2.5, 0.7, 0.6, 'brow')]),
    ]


def helm(plume=True, wings=False, crest='helmcrest'):
    """The helmet: a gold-banded brow, a bowl in tiers, a finial with a pearl, a red horsehair tassel
    bursting from under it; a peak over the brow, a flame-shaped plaque before the bowl; ear flaps; a neck guard."""
    out = [
        part('helm', 'head', (0, -5.6, 0), (0, 0, 0), [
            (-3.65, -1.25, -3.65, 7.3, 1.5, 7.3, 'helmband'),
            (-3.45, -3.45, -3.45, 6.9, 2.3, 6.9, 'helmbowl'),
            (-2.55, -4.6, -2.55, 5.1, 1.25, 5.1, 'helmcrown'),
            (-1.25, -5.5, -1.25, 2.5, 1.0, 2.5, 'helmknob'),
            (-0.35, -8.6, -0.35, 0.7, 3.2, 0.7, 'finial'),
            (-0.6, -9.3, -0.6, 1.2, 1.0, 1.2, 'pearl'),
            (-3.85, -0.5, -4.35, 7.7, 0.7, 1.1, 'visor'),
        ]),
        part('crest', 'helm', (0, -1.0, -3.6), (-0.12, 0, 0), [(-1.5, -3.8, -0.3, 3.0, 3.8, 0.5, crest)]),
        part('right_flap', 'helm', (-3.55, -0.3, 0.3), (0, 0, 0.16), [(-0.6, 0, -2.9, 0.7, 4.8, 5.6, 'earflap')]),
        part('neckguard', 'helm', (0, -0.2, 3.25), (0.28, 0, 0), [(-3.55, 0, -0.4, 7.1, 4.6, 0.8, 'neckguard')]),
    ]
    if plume:
        out += [
            part('plume', 'helm', (0, -7.0, 0), (0, 0, 0), [(-1.3, 0, -1.3, 2.6, 1.0, 2.6, 'plume_top'),
                                                            (-2.5, 0.8, -2.5, 5.0, 2.5, 5.0, 'plume')]),
            part('plume_tail', 'plume', (0, 1.6, 1.8), (0.6, 0, 0), [(-2.0, 0, -1.1, 4.0, 6.0, 2.2, 'plume_long')]),
        ]
    if wings:
        # the phoenix wings (凤翅): gold blades sweeping up and back from over the ears
        out += [part('right_wing', 'helm', (-3.6, -1.6, 0.4), (0.3, 0.35, -0.6), [(-0.25, -7.6, -2.0, 0.5, 8.0, 5.0, 'wing')])]
    return out


def ribbon_arch(name, parent, centre, radius, a0, a1, n, width=1.5, style='ribbon', droop=0.0):
    """A silk ribbon (披帛) in an arch about `centre` in the x-y plane, from angle a0 to a1 (degrees, 0 to the
    model's left (+x), 90 down), in n segments; its broad face toward the back. Returns its parts."""
    out = [part(name, parent, centre, (0, 0, 0), [])]
    pts = []
    for i in range(n + 1):
        a = math.radians(a0 + (a1 - a0) * i / n)
        r = radius
        pts.append((r * math.cos(a), r * math.sin(a) + droop * math.sin(math.pi * i / n), 0.0))
    for i in range(n):
        p, q = pts[i], pts[i + 1]
        rx, ry, length = segment(p, q)
        out.append(part('%s_%d' % (name, i), name, p, (rx, ry, 0), [(-0.15, -width / 2, -0.1, 0.3, width, length + 0.2, style)]))
    return out


def trail(name, parent, pivot, rot, segs, style, width=1.4, thick=0.3):
    """A hanging ribbon end in segments, each a child of the last (segs: (length, turn from the last))."""
    out, par, at = [], parent, pivot
    for i, (length, turn) in enumerate(segs):
        n = name if i == 0 else '%s_%d' % (name, i + 1)
        out.append(part(n, par, at, rot if i == 0 else turn, [(-width / 2, 0, -thick / 2, width, length, thick, style)]))
        par, at = n, (0, length - 0.15, 0)
    return out


def pose(parts, rots):
    """The parts with these ones turned otherwise: {name: rotation}."""
    return [(n, par, piv, rots.get(n, rot), boxes, m) for n, par, piv, rot, boxes, m in parts]


def without(parts, names):
    return [p_ for p_ in parts if p_[0] not in names]


# ── Painting the trooper ─────────────────────────────────────────────────────

def paint_trooper(p, style, f, dims, c):
    """Paints one of the trooper's common boxes in colours `c`; returns whether it knew the style."""
    silkc, deep, trim = c['silk'], c['silk_deep'], c['trim']
    if style in ('thigh', 'shin', 'hips'):                 # silk trousers, gathered at the knee
        silk(p, f, silkc, deep, fold=4)
        if style == 'shin':
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), k(deep, 0.8))
        return True
    if style == 'greave':                                  # a gilt greave, a cloud scroll embossed on it
        gilt_rounded(p, f)
        for s in SIDES:
            rim(p, f[s], GOLD_DEEP, 'lrb')
        fx, fy, fw, fh = f['front']
        cloud(p, fx + fw // 2 - 3, fy + fh // 2, GOLD_DEEP, r=1, rect=f['front'])
        return True
    if style == 'kneeguard':                               # a round knee boss, a beast's face on it
        gilt_rounded(p, f, 1.05)
        beast_mask(p, f['front'])
        return True
    if style in ('boot', 'boottoe', 'boottip'):            # black cloud boots, white-soled, the toes turned up
        p.shaded(f, BOOT)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), (220, 214, 200))
                if style == 'boot':
                    p.px((fx + i, fy), k(trim, 0.9))
        if style == 'boottip':
            gilt(p, f)
        return True
    if style == 'belt':                                    # a leather belt, gold plaques along it, a jade in each
        for s, r in f.items():
            p.face(r, (70, 30, 26), LIGHTS[s], noise=3)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            rim(p, f[s], GOLD_DEEP, 'tb')
            for i in range(1, fw - 3, 6):
                for j in range(1, fh - 1):
                    for w in range(3):
                        p.px((fx + i + w, fy + j), k(GOLD if j < fh - 2 else GOLD_DEEP, LIGHTS[s]))
                p.px((fx + i + 1, fy + fh // 2), (110, 196, 150))
        return True
    if style == 'buckle':
        gilt(p, f)
        beast_mask(p, f['front'], ring=False)
        return True
    if style == 'bellybeast':                              # the beast on the belly (腹吞): a lion's face, a ring in its jaws
        gilt_rounded(p, f, 1.05)
        beast_mask(p, f['front'], ring=True)
        return True
    if style == 'apron':                                   # the apron between the tassets: silk, a gold border, a cloud
        for s, r in f.items():
            fabric(p, r, trim, k(trim, 0.6), LIGHTS[s], 5, 0.35)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            rim(p, f[s], GOLD, 'lrb')
            cloud(p, fx + fw // 2 - 3, fy + fh - 6, GOLD, r=1, rect=f[s])
            for j in range(3, fh - 9, 4):
                p.px((fx + fw // 2, fy + j), GOLD_DEEP)
        return True
    if style == 'sashend':                                 # the ends of the silk sash, hanging
        for s, r in f.items():
            fabric(p, r, c['plume'], c['plume_deep'], LIGHTS[s], 3, 0.4)
        for s in ('front', 'back'):
            ragged(p, f[s], teeth=2, depth=2)
        return True
    if style in ('tasset', 'tasset_side', 'tasset_back'):  # the lamellar skirt; gold studs, a scalloped foot
        lamellar_box(p, f, c)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            if fw > 3:
                for i in range(1, fw - 1, 3):
                    p.px((fx + i, fy + 2), GOLD_LIGHT)
            ragged(p, f[s], teeth=3, depth=1)
        return True
    if style == 'torso':                                   # under the cuirass: lamellae at the sides and back
        lamellar_box(p, f, c)
        fx, fy, fw, fh = f['back']
        for j in range(2, 10):
            for i in range(3, fw - 3):
                if j in (2, 9) or i in (3, fw - 4):
                    p.px((fx + i, fy + j), GOLD_LIGHT)
        return True
    if style == 'cuirass':                                 # the breastplate: gilt, moulded, a cloud scroll along its top
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                a = abs((i + 0.5) / fw - 0.5) * 2
                t = 1.2 - 0.35 * a ** 1.6 - 0.25 * j / max(1, fh - 1)
                col = mixc(c['plate'], GOLD_LIGHT, max(0, t - 1) * 2) if t > 1 else mixc(c['plate_deep'], c['plate'], max(0, t - 0.4) / 0.6)
                p.px(xy, noisy(p, k(col, LIGHTS[s]), 3))
            if s in SIDES:
                rim(p, r, GOLD_LIGHT, 't')
                rim(p, r, GOLD_DARK, 'b')
                for j in range(3, fh - 1, 3):
                    for i in range(0, fw, 4):
                        p.px((fx + i + (j // 3) % 2 * 2, fy + j), k(c['plate_deep'], 0.8))
            if s == 'front':
                for i in range(2, fw - 2):
                    if (i // 3) % 2 == 0:
                        p.px((fx + i, fy + 2), GOLD_DEEP)
        return True
    if style == 'abdomen':
        for s, r in f.items():
            if s in ('top', 'bottom'):
                p.face(r, c['plate_deep'], LIGHTS[s], noise=3)
            else:
                scale_armour(p, r, c, LIGHTS[s])
                rim(p, r, GOLD_DEEP, 'tb')
        return True
    if style == 'chestband':                               # the band round the chest under the mirrors: silk, knotted
        for s, r in f.items():
            p.face(r, c['plume'], LIGHTS[s], noise=3)
            rim(p, r, c['plume_deep'], 'b')
        return True
    if style == 'waistsash':
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                t = 1.15 - 0.4 * j / max(1, fh - 1) + (0.12 if (i + j) % 4 == 0 else 0)
                p.px(xy, noisy(p, k(c['plume'], t * LIGHTS[s]), 3))
        return True
    if style == 'strap':
        p.shaded(f, c['plume'])
        for s in ('front',):
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 3):
                p.px((fx, fy + j), GOLD)
        return True
    if style == 'mirror':
        p.face(f['top'], GOLD_DEEP, 1.0)
        p.face(f['bottom'], GOLD_DARK, 1.0)
        for s in ('right', 'left'):
            p.face(f[s], GOLD_DEEP, 1.0)
        mirror_disc(p, f['front'], glow=0.0)
        mirror_disc(p, f['back'])
        return True
    if style == 'gorget':                                  # the collar: gilt, a fret along it
        gilt(p, f)
        for s in SIDES:
            key_fret(p, f[s])
        return True
    if style == 'neck':
        p.shaded(f, c['skin'])
        return True
    if style == 'robe_back':                               # the robe's skirt behind, under the tassets
        for s, r in f.items():
            fabric(p, r, c['silk'], c['silk_deep'], LIGHTS[s], 4, 0.5)
        for s in ('front', 'back'):
            rim(p, f[s], c['trim'], 'b', 2)
            ragged(p, f[s], teeth=4, depth=1)
        return True
    if style in ('scarf', 'scarfknot'):                    # the silk scarf knotted at the throat
        for s, r in f.items():
            fabric(p, r, c['plume'], c['plume_deep'], LIGHTS[s], 3 if style == 'scarf' else 2, 0.35)
        return True
    if style == 'pauldron_cap':                            # the dome of the shoulder guard
        gilt_rounded(p, f, 1.08)
        return True
    if style == 'sleeve':                                  # the upper sleeve, silk, a border at its foot
        silk(p, f, silkc, deep, fold=4)
        return True
    if style == 'elbowpuff':                               # the sleeve puffing out at the elbow between the guards
        silk(p, f, silkc, deep, fold=3, fall=0.5)
        for s in SIDES:
            rim(p, f[s], trim, 'b')
        return True
    if style in ('pauldron1', 'pauldron2', 'pauldron3'):   # a tier of the shoulder guard: gilt, rounded, a dark lip
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                t = (1.25 - 0.45 * j / max(1, fh - 1)) * LIGHTS[s]
                if s in ('top', 'bottom'):
                    t = LIGHTS[s] * (1.2 - 0.4 * abs(j / max(1, fh - 1) - 0.5))
                col = mixc(c['plate'], GOLD_LIGHT, max(0, t - 1.05) * 2) if t > 1.05 else k(c['plate'], t)
                p.px(xy, noisy(p, col, 3))
            if s in SIDES:
                rim(p, r, GOLD_DARK, 'b')
                for i in range(1, fw, 3):
                    p.px((fx + i, fy + 1), GOLD_LIGHT)
            if s == 'bottom':
                rim(p, r, GOLD_DARK)
        return True
    if style == 'beastface':                               # the beast's head biting the arm (吞口)
        gilt(p, f)
        beast_mask(p, f['right'])
        beast_mask(p, f['left'])
        return True
    if style == 'forearm':
        silk(p, f, silkc, deep, fold=3)
        return True
    if style == 'bracer':                                  # a gilt bracer, banded, a cloud on it
        gilt_rounded(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            rim(p, f[s], GOLD_DARK, 'tb')
            for i in range(fw):
                p.px((fx + i, fy + fh // 2), GOLD_DEEP)
        return True
    if style == 'fist':
        p.shaded(f, c['skin'])
        for s in ('front', 'right', 'left'):
            fx, fy, fw, fh = f[s]
            for i in range(0, fw, 2):
                p.px((fx + i, fy + fh - 2), c['skin_shade'])
            for i in range(fw):
                p.px((fx + i, fy), k(c['skin_shade'], 1.05))
        return True
    if style == 'face':
        p.shaded(f, c['skin'])
        face_front(p, f['front'], c, moustache=c.get('moustache', False), glow_eyes=c.get('glow_eyes'),
                   beard=c.get('beard'))
        face_side(p, f['right'], c, True)
        face_side(p, f['left'], c, False)
        for _, j, xy in each(f['back']):
            p.px(xy, noisy(p, mixc(HAIR, (54, 44, 44), (j % 3) / 3), 2))
        p.face(f['top'], HAIR, 1.0)
        p.face(f['bottom'], c['skin_shade'], 0.9)
        return True
    if style == 'nose':
        p.shaded(f, c['skin'])
        fx, fy, fw, fh = f['front']
        p.px((fx, fy + fh - 1), c['skin_shade'])
        p.px((fx + fw - 1, fy + fh - 1), c['skin_shade'])
        p.face(f['bottom'], k(c['skin_shade'], 0.8), 1.0)
        return True
    if style == 'brow':
        lock(p, f, along_x=True, tone=BROWN_BLACK if not c.get('blue_brow') else BLUE_BLACK)
        return True
    # ── the helmet ──
    if style == 'helmband':                                # the brow band: gold, a fret, a jewel at the front
        gilt(p, f, 1.05)
        for s in SIDES:
            key_fret(p, f[s])
        fx, fy, fw, fh = f['front']
        circle(p, fx + fw / 2, fy + fh / 2, 0, 1.3, c.get('jewel', (220, 40, 40)))
        return True
    if style in ('helmbowl', 'helmcrown', 'helmknob'):     # the bowl: gilt, ribbed, rounded
        bowl = c.get('helm', c['plate'])
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                a = abs((i + 0.5) / fw - 0.45) * 2
                t = 1.25 - 0.4 * a * a - 0.3 * j / max(1, fh - 1)
                if s == 'top':
                    t = 1.2 - 0.3 * a
                col = mixc(bowl, GOLD_LIGHT, max(0, t - 1) * 2.4) if t > 1 else mixc(k(bowl, 0.6), bowl, max(0, t - 0.5) * 2)
                if s in SIDES and style == 'helmbowl' and i % 4 == 1:
                    col = k(col, 0.78)                     # the ribs between its plates
                p.px(xy, noisy(p, k(col, LIGHTS[s] if s != 'top' else 1), 3))
            if s in SIDES:
                rim(p, r, GOLD_DARK, 'b')
        return True
    if style == 'finial':
        gilt(p, f, 1.1)
        return True
    if style == 'pearl':                                   # the pearl on the finial: it shines
        for s, r in f.items():
            for i, j, xy in each(r):
                p.lit(xy, mixc(PEARL, (190, 210, 240), j / max(1, r[3])), glow=0.55)
        return True
    if style == 'visor':
        gilt(p, f, 1.05)
        for s in SIDES:
            rim(p, f[s], GOLD_DARK, 'b')
        return True
    if style == 'helmcrest':                               # a gold plaque before the bowl, cut like a flame
        def col(s, t):
            return mixc(GOLD_LIGHT, GOLD_DEEP, t)
        bare(p, f, ('top', 'bottom', 'right', 'left'))
        for s in ('front', 'back'):
            for _, _, xy in each(f[s]):
                p.clear(xy)
        plane(p, f, 'z', lambda s, t: flame_mask(s, t), col)
        fx, fy, fw, fh = f['front']
        circle(p, fx + fw / 2, fy + fh * 0.68, 0, 0.9, c.get('jewel', (220, 40, 40)))
        return True
    if style == 'earflap':                                 # the ear flaps: gilt lamellae, a silk lining, flared
        lamellar_box(p, f, c, pw=3, ph=3)
        for s in ('right', 'left'):
            rim(p, f[s], GOLD_LIGHT, 'lrb')
        return True
    if style == 'neckguard':
        lamellar_box(p, f, c, pw=3, ph=3)
        for s in ('front', 'back'):
            ragged(p, f[s], teeth=3, depth=1)
        return True
    if style in ('plume', 'plume_long', 'plume_top'):                   # the horsehair tassel (红缨)
        for s, r in f.items():
            horsehair(p, r, c['plume'], c['plume_deep'], LIGHTS[s], cut=0.3 if style == 'plume_long' and s in SIDES else 0)
        return True
    if style == 'wing':                                    # a phoenix wing: a gold blade in feathered steps
        bare(p, f, ('top', 'bottom', 'front', 'back'))
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in each(f[s]):
                s_ = (i + 0.5) / fw if s == 'right' else 1 - (i + 0.5) / fw     # 0 at the back .. 1 at the front
                t = (j + 0.5) / fh                                               # 0 at the tip .. 1 at the root
                width = 0.15 + 0.85 * t ** 0.7
                if s_ > width or (t < 0.9 and (s_ * 6 + t * 10) % 3 < 0.35 and s_ > width - 0.3):
                    p.clear(xy)
                    continue
                col = mixc(GOLD_LIGHT, GOLD_DEEP, 0.3 + 0.6 * abs(s_ - width / 2) / max(0.1, width))
                if (j + int(s_ * 8)) % 4 == 0:
                    col = k(col, 0.78)
                p.px(xy, noisy(p, col, 3))
        return True
    if style == 'ribbon':                                  # the celestial ribbon (披帛): silk, gold-edged
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                t = 0.95 + 0.12 * math.sin(i * 0.7) + 0.08 * math.sin(j * 0.5)
                p.px(xy, noisy(p, k(c['ribbon'], t * LIGHTS[s]), 3))
            if s in ('right', 'left'):
                rim(p, r, c.get('ribbon_edge', GOLD), 'tb')
        return True
    return False
