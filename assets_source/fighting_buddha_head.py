"""The Victorious Fighting Buddha's head: the Monkey King's own, sculpted, at four texture pixels to the
model pixel (the body, fighting_buddha.py, is drawn at two). Its own model and skins, so the face has the
detail it needs: FightingBuddhaLayer.headLayer() between its GENERATED head markers, and
fighting_buddha_head.png with its glowing fighting_buddha_head_glow.png. The crown and its ribbons stay
with the body's model, riding the same head.

- a skull of golden-brown fur, a crest swept back over the crown and a mane at the nape;
- the bare face recessed in a frame of fur: heart-shaped, warm tan going to red, darker about the
  eyes, hollow under prominent cheekbones; sideburns and thick tufts at the cheeks standing proud of it,
  fur at the jaw;
- a heavy brow ridge in two halves, drawn down toward the nose in a frown, casting the eyes into deep
  sockets; in them the Fiery Golden Eyes (火眼金睛), angled and fierce: golden irises, dark pupils, a rim
  of fire, burning (glowing);
- a muzzle standing out from the face, a short nose bridge sloping to it, flat monkey nostrils on its
  front, a defined upper lip, a mouth line with a confident smirk, a chin beneath;
- round ears out at the sides, shaded within, half in the fur.

Run from the repository root: python assets_source/fighting_buddha_head.py (fighting_buddha.py runs it too)."""
import math

import beastgen as bg
from beastgen import emit
from fire_court import part, mirrored
from king_yama import paint as exact_paint

JAVA = 'src/main/java/com/jttw/engine/client/renderer/FightingBuddhaLayer.java'

FUR = (190, 132, 52)
FUR_LIGHT = (236, 186, 98)
FUR_DARK = (122, 78, 28)
FUR_DEEP = (74, 44, 16)
SKIN = (214, 132, 90)
SKIN_LIGHT = (238, 170, 120)
SKIN_HIGH = (250, 198, 150)
SKIN_DARK = (158, 86, 58)
SKIN_DEEP = (104, 48, 32)
LIP = (150, 66, 52)
MOUTH = (52, 18, 14)
NOSTRIL = (46, 20, 14)
IRIS = (255, 196, 46)
IRIS_HOT = (255, 236, 140)
RIM = (255, 96, 20)
PUPIL = (20, 8, 4)
EAR_IN = (170, 92, 70)

SIDES = (('front', 1.0), ('back', 0.84), ('right', 0.92), ('left', 0.92), ('top', 1.08), ('bottom', 0.62))

HEAD = mirrored([
    part('head', 'root', (0, 0, 0), (0, 0, 0), []),
    # the skull, its face recessed: the face plate stands only a little before it
    part('skull', 'head', (0, 0, 0), (0, 0, 0), [
        (-4.3, -8.6, -3.0, 8.6, 8.4, 7.4, 'skull'),
        (-3.3, -9.8, -2.2, 6.6, 1.6, 6.2, 'crest'),
        (-4.1, -8.2, 4.2, 8.2, 7.2, 1.6, 'mane'),
        (-3.4, -6.5, -3.55, 6.8, 6.2, 0.6, 'face'),
        (-2.1, -0.7, -5.2, 4.2, 1.2, 2.4, 'jaw_fur'),
    ]),
    # the brow ridge in two halves, drawn down toward the nose: a frown
    part('right_brow', 'head', (0, -6.45, -3.9), (0, 0, 0.2), [(-3.8, -0.95, -1.55, 3.8, 1.35, 1.6, 'brow')]),
    part('glabella', 'head', (0, -6.3, -4.3), (0, 0, 0), [(-0.75, -0.6, -0.6, 1.5, 1.6, 1.0, 'skin_ridge')]),
    # the nose's short bridge, stepping out to the muzzle
    part('bridge', 'head', (0, -4.75, -3.7), (-0.9, 0, 0), [(-0.6, 0, -0.5, 1.2, 1.6, 1.0, 'bridge')]),
    part('muzzle', 'head', (0, 0, 0), (0, 0, 0), [
        (-2.05, -3.75, -6.4, 4.1, 2.5, 2.9, 'muzzle'),
        (-1.5, -4.25, -5.7, 3.0, 0.55, 2.0, 'muzzle_top'),
        (-2.5, -3.3, -5.6, 0.6, 1.9, 1.9, 'lip_corner'),
        (1.9, -3.3, -5.6, 0.6, 1.9, 1.9, 'lip_corner'),
        (-1.6, -1.3, -5.7, 3.2, 1.25, 2.1, 'chin'),
    ]),
    part('right_cheekbone', 'head', (-2.35, -3.55, -3.75), (0, 0.32, 0), [(-1.25, -0.55, -0.8, 2.5, 1.15, 1.0, 'cheekbone')]),
    # the frame of fur, standing proud of the face
    part('right_sideburn', 'head', (-3.7, -6.0, -3.2), (0.05, 0.2, 0.12), [(-0.95, 0, -1.0, 1.9, 5.4, 2.0, 'tuft')]),
    part('right_cheek_tuft_0', 'head', (-3.5, -3.3, -3.6), (0.15, 0.25, 0.62), [(-0.8, 0, -0.9, 1.6, 3.8, 1.8, 'tuft')]),
    part('right_cheek_tuft_1', 'head', (-3.9, -2.2, -2.6), (0.22, 0.35, 0.95), [(-0.8, 0, -0.9, 1.6, 3.6, 1.8, 'tuft')]),
    part('right_cheek_tuft_2', 'head', (-3.2, -0.9, -3.5), (0.45, 0.2, 0.55), [(-0.7, 0, -0.8, 1.4, 3.0, 1.6, 'tuft_small')]),
    part('right_brow_tuft', 'head', (-3.6, -6.9, -3.9), (0, 0.3, 1.2), [(-0.6, 0, -0.6, 1.2, 2.6, 1.2, 'tuft_small')]),
    # the ears, round, out at the sides and half in the fur
    part('right_ear', 'head', (-5.0, -4.7, -0.2), (0.1, -0.5, 0.15), [(-0.45, -1.8, -1.8, 0.9, 3.6, 3.6, 'ear')]),
    part('right_ear_tuft', 'head', (-4.6, -6.1, 0.9), (0.5, 0.0, 0.5), [(-0.6, 0, -0.7, 1.2, 2.6, 1.4, 'tuft_small')]),
])


def k(c, f):
    return [v * f for v in c[:3]]


def mixc(a, b, t):
    t = max(0.0, min(1.0, t))
    return [x + (y - x) * t for x, y in zip(a, b)]


def fur(p, rect, light=1.0, base=FUR, sweep=0.0):
    """Fur in locks of differing width: each strand its own shade and length, the lock's crest lit in
    broken streaks, dark partings between the locks, all a little ragged."""
    fx, fy, fw, fh = rect
    # the locks across the face: their widths, 3 to 6 texels
    bounds, x = [], 0
    while x < fw + 8:
        w = p.rnd.randint(3, 6)
        bounds.append((x, w))
        x += w
    streak = {}
    for i in range(fw):
        tone = p.rnd.uniform(-0.12, 0.12)
        for j in range(fh):
            xx = i + sweep * j + 0.6 * math.sin(j * 0.35 + i)
            for x0, w in bounds:
                if x0 <= xx < x0 + w:
                    lock = (xx - x0) / w
                    break
            else:
                lock = 0.5
            crest = math.sin(lock * math.pi)
            c = mixc(FUR_DARK, FUR_LIGHT, crest * 0.8 + 0.1)
            c = mixc(c, base, 0.4)
            key = (i, j // 4)
            if key not in streak:
                streak[key] = p.rnd.random()
            f = light * (1.04 - 0.2 * j / max(1, fh - 1) + tone)
            if crest > 0.7 and streak[key] > 0.6:
                f *= 1.12
            if lock < 0.14 or lock > 0.92:
                f *= 0.7
            p.px((fx + i, fy + j), [v * f + p.rnd.randint(-6, 6) for v in c])


def shaded(p, f, base, top=1.1, bottom=0.6):
    for s, light in SIDES:
        fx, fy, fw, fh = f[s]
        for j in range(fh):
            for i in range(fw):
                t = j / max(1, fh - 1)
                l = light * (top if s == 'top' else bottom if s == 'bottom' else 1.06 - 0.22 * t)
                p.px((fx + i, fy + j), [v * l + p.rnd.randint(-3, 3) for v in base])


def taper(p, rect):
    """Cuts a tuft's face to a point at its end, a little ragged."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        half = (fw / 2) * (1 - (j / fh) ** 1.4) + 0.4
        for i in range(fw):
            if abs(i + 0.5 - fw / 2) > half + p.rnd.uniform(-0.3, 0.3):
                p.clear((fx + i, fy + j))


def face(p, rect):
    """The face's front: heart-shaped bare skin in its frame of fur; the sockets dark, the eyes in them
    angled, golden, burning at the rims; the cheeks hollow below the cheekbones."""
    fx, fy, fw, fh = rect
    fur(p, rect, 0.9, base=FUR_DARK)
    cx = fw / 2
    r = fw * 0.25
    eye_y = fh * 0.1
    for j in range(fh):
        for i in range(fw):
            x, y = i + 0.5, j + 0.5
            lobe = min(math.hypot(x - (cx - r * 0.95), y - fh * 0.3), math.hypot(x - (cx + r * 0.95), y - fh * 0.3)) - r * 1.12
            half = (cx - 0.3) * max(0.0, 1 - (y - fh * 0.3) / (fh * 0.78))
            body = abs(x - cx) - half
            d = min(lobe, body) if y > fh * 0.3 else lobe
            if y < fh * 0.06:
                continue
            if d > 0.4:
                continue
            # skin: warm tan, reddening toward the middle, darker about the eyes, hollow under the cheekbones
            ex = abs(x - cx)
            c = mixc(SKIN, SKIN_LIGHT, 0.5 - 0.5 * ex / cx)
            near_eye = math.hypot((ex - r * 0.95) / (r * 1.3), (y - eye_y - 1) / (fh * 0.17))
            if near_eye < 1:
                c = mixc(SKIN_DEEP, c, near_eye ** 1.5)
            if y > fh * 0.52 and ex > cx * 0.42:          # the hollow of the cheek
                c = k(c, 0.82)
            if ex < 1.6 and fh * 0.3 < y < fh * 0.6:        # the bridge of the nose catching the light
                c = mixc(c, SKIN_HIGH, 0.5)
            if d > -0.8:                                     # the fur's edge over the skin
                c = mixc(c, FUR_DEEP, 0.55)
            p.px((fx + i, fy + j), [v + p.rnd.randint(-3, 3) for v in c])
    # the eyes: almond-shaped, the outer corner raised (fierce), the inner corner drawn down under the brow
    for side in (-1, 1):
        ecx = cx + side * r * 0.98
        w = 8
        for n in range(w):
            i = int(round(ecx - w / 2 + n)) if side < 0 else int(round(ecx + w / 2 - 1 - n))
            t = n / (w - 1)                     # 0 at the outer corner .. 1 at the inner
            top = eye_y + 0.2 + 1.4 * t ** 1.2
            bottom = eye_y + 4.8 + 0.2 * t
            if bottom - top < 0.8:
                continue
            for j in range(int(math.floor(top)), int(math.ceil(bottom))):
                rim = j == int(math.floor(top)) or j == int(math.ceil(bottom)) - 1 or n in (0, w - 1)
                if rim:
                    p.px((fx + i, fy + j), RIM, glow=True)
                else:
                    iris = IRIS_HOT if j == int(math.floor(top)) + 1 and 1 < n < w - 2 else IRIS
                    p.px((fx + i, fy + j), iris, glow=True)
            # the lid's dark line above, the socket's shadow under the brow
            for jj in (int(math.floor(top)) - 1, int(math.floor(top)) - 2):
                if 0 <= jj:
                    p.px((fx + i, fy + jj), SKIN_DEEP if jj == int(math.floor(top)) - 1 else mixc(SKIN_DEEP, SKIN_DARK, 0.5))
        # the pupil: dark, a little toward the nose, so he glares ahead
        pc = int(round(ecx + side * -0.6))
        for i in (pc - 1, pc):
            for j in (int(eye_y + 2.0), int(eye_y + 3.0)):
                p.px((fx + i, fy + j), PUPIL)
                p.glow.putpixel((fx + i, fy + j), (0, 0, 0, 0))


def muzzle(p, f):
    shaded(p, f, SKIN_LIGHT)
    fx, fy, fw, fh = f['top']
    for j in range(fh):                         # the top: lit, sloping light toward the front
        for i in range(fw):
            c = mixc(SKIN_HIGH, SKIN_LIGHT, abs(i + 0.5 - fw / 2) / (fw / 2))
            p.px((fx + i, fy + j), c)
    fx, fy, fw, fh = f['front']
    cx = fw / 2
    for j in range(fh):
        for i in range(fw):
            t = j / max(1, fh - 1)
            c = mixc(SKIN_HIGH, SKIN, t * 0.9)
            c = k(c, 1 - 0.18 * (abs(i + 0.5 - cx) / cx) ** 2)
            p.px((fx + i, fy + j), c)
    # flat monkey nostrils, set wide on the front, the septum between
    for side in (-1, 1):
        nx = int(cx + side * 2.2)
        for i in (nx - 1, nx):
            p.px((fx + i, fy + 2), NOSTRIL)
            p.px((fx + i, fy + 3), k(NOSTRIL, 1.6))
        p.px((fx + nx + (1 if side < 0 else -2), fy + 2), k(NOSTRIL, 2.2))
    # the philtrum, and the upper lip's line
    for j in range(4, 6):
        p.px((fx + int(cx) - 1, fy + j), SKIN_DARK)
        p.px((fx + int(cx), fy + j), SKIN_DARK)
    for i in range(1, fw - 1):
        p.px((fx + i, fy + 6), LIP)
    # the mouth: a long line, its right corner (his left) drawn up in a smirk
    for i in range(0, fw):
        j = fh - 2
        if i >= fw - 4:
            j -= 1 + (i >= fw - 2)
        p.px((fx + i, fy + j), MOUTH)
        if i >= fw - 4:
            p.px((fx + i, fy + j + 1), LIP)
    fx, fy, fw, fh = f['bottom']                # beneath it, in shadow
    for j in range(fh):
        for i in range(fw):
            p.px((fx + i, fy + j), SKIN_DEEP)


def paint_box(p, style, f, dims):
    if style in ('skull', 'crest', 'mane', 'jaw_fur'):
        for s, light in SIDES:
            fur(p, f[s], light, base=FUR if style != 'mane' else FUR_DARK, sweep=0.15 if s in ('right', 'left') else 0.0)
    elif style == 'face':
        for s, light in SIDES:
            if s == 'front':
                face(p, f[s])
            else:
                fur(p, f[s], light * 0.8, base=FUR_DARK)
    elif style == 'brow':
        # golden fur over it and before it, its underside dark (the shadow over the eyes)
        for s, light in SIDES:
            if s == 'bottom':
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    for i in range(fw):
                        p.px((fx + i, fy + j), mixc(FUR_DEEP, SKIN_DEEP, 0.5))
            else:
                fur(p, f[s], light * (1.15 if s in ('top', 'front') else 0.9), base=FUR, sweep=0.4)
        fx, fy, fw, fh = f['front']                     # the skin of the ridge at its foot
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), SKIN_DARK)
            p.px((fx + i, fy + fh - 2), mixc(SKIN_DARK, FUR_DARK, 0.5))
    elif style in ('skin_ridge', 'bridge', 'cheekbone'):
        shaded(p, f, SKIN_LIGHT if style != 'cheekbone' else SKIN, top=1.25, bottom=0.55)
        if style == 'cheekbone':                         # its highlight along the top edge of the front
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                p.px((fx + i, fy), SKIN_HIGH)
                p.px((fx + i, fy + 1), mixc(SKIN_HIGH, SKIN, 0.5))
        if style == 'bridge':
            fx, fy, fw, fh = f['front']
            for j in range(fh):
                for i in range(fw):
                    if abs(i + 0.5 - fw / 2) < fw * 0.2:
                        p.px((fx + i, fy + j), mixc(SKIN_LIGHT, SKIN_HIGH, 0.4))
    elif style == 'muzzle':
        muzzle(p, f)
    elif style in ('muzzle_top', 'lip_corner'):
        shaded(p, f, SKIN_LIGHT if style == 'muzzle_top' else SKIN, top=1.2, bottom=0.6)
    elif style == 'chin':
        shaded(p, f, SKIN, top=0.9, bottom=0.5)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy), LIP)                      # the lower lip
            p.px((fx + i, fy + 1), mixc(LIP, SKIN_LIGHT, 0.6))
    elif style in ('tuft', 'tuft_small'):
        for s, light in SIDES:
            fur(p, f[s], light * 1.05, base=FUR, sweep=0.25)
            if s in ('front', 'back', 'right', 'left'):
                taper(p, f[s])
    elif style == 'ear':
        for s, light in SIDES:
            fur(p, f[s], light, base=FUR_DARK)
        for s in ('left', 'right'):                      # its face: round, bare, shaded within
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    r = math.hypot((i + 0.5 - fw / 2) / (fw / 2), (j + 0.5 - fh / 2) / (fh / 2))
                    if r > 1:
                        p.clear((fx + i, fy + j))
                    elif r > 0.78:
                        p.px((fx + i, fy + j), SKIN)
                    else:
                        p.px((fx + i, fy + j), mixc(SKIN_DEEP, EAR_IN, r / 0.78))
        for s in ('front', 'back', 'top', 'bottom'):     # its rim, rounded off
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    edge = (i + 0.5) / fw if s in ('front', 'back') else (j + 0.5) / fh
                    if s in ('front', 'back') and (j < fh * 0.15 or j > fh * 0.85):
                        p.clear((fx + i, fy + j))
    else:
        raise ValueError(style)


def generate(painter_class):
    bg.UNITS = 64
    bg.S = 4
    bg.TEX = bg.UNITS * bg.S
    p = painter_class(77)
    slots = exact_paint(HEAD, p, paint_box)
    p.img.save('src/main/resources/assets/jttw_engine/textures/entity/fighting_buddha_head.png')
    p.glow.save('src/main/resources/assets/jttw_engine/textures/entity/fighting_buddha_head_glow.png')
    emit(HEAD, slots, JAVA, 'head')
    print('fighting buddha head', len(HEAD), 'parts,', len(slots), 'boxes')


if __name__ == '__main__':
    from fighting_buddha import BuddhaPainter
    generate(BuddhaPainter)
