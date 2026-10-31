"""The Victorious Fighting Buddha's head: the Monkey King's own, sculpted, at four texture pixels to the
model pixel (the body, fighting_buddha.py, is drawn at two). Its own model and skins, so the face has the
detail it needs: FightingBuddhaLayer.headLayer() between its GENERATED head markers, and
fighting_buddha_head.png with its glowing fighting_buddha_head_glow.png. The crown and its ribbons stay
with the body's model, riding the same head.

One sculpted head, built up in steps so that nothing reads as a box:

- the skull rounded: a core, wider tiers at the temples and cheeks, a dome over the crown in two steps,
  a bulge at the back, and locks of fur lying over the crown and falling from the nape, overlapping;
- the bare face recessed in its frame of fur: heart-shaped, warm tan reddening toward the middle,
  shadowed round the eyes, hollow under the cheekbones; sideburns and cheek tufts standing proud of it,
  tapering, overlapping;
- the brow: a sculpted ridge in two halves, drawn down toward the nose (fierce), stepped back into the
  forehead's fur, its underside in shadow;
- the Fiery Golden Eyes (火眼金睛) set in the shadow of their sockets: small almonds, the outer corner
  raised, tapering at both ends; golden irises shading to amber, dark pupils, a faint fiery rim;
  glowing;
- the muzzle stepped out from the face in three tiers, each narrower than the last, so its sides
  recede and its corners round off, all in the face's own skin, darker down its sides and beneath; the
  nostrils at the top of its front, the upper lip below them, the mouth running round it with a smirk
  at one corner, the chin beneath;
- the ears: cups with thickness, a curled rim round an inner bowl in shadow, turned a little forward,
  a lock of fur over each.

Run from the repository root: python assets_source/fighting_buddha_head.py (fighting_buddha.py runs it too)."""
import math

import beastgen as bg
from beastgen import emit
from fire_court import part, mirrored
from king_yama import paint as exact_paint

JAVA = 'src/main/java/com/jttw/engine/client/renderer/FightingBuddhaLayer.java'

FUR = (200, 142, 60)
FUR_LIGHT = (244, 198, 112)
FUR_DARK = (138, 90, 34)
FUR_DEEP = (82, 50, 20)
SKIN = (200, 124, 86)
SKIN_LIGHT = (222, 152, 108)
SKIN_HIGH = (236, 176, 128)
SKIN_DARK = (150, 82, 56)
SKIN_DEEP = (96, 46, 32)
LIP = (140, 64, 50)
MOUTH = (60, 22, 16)
NOSTRIL = (58, 26, 18)
IRIS = (255, 190, 50)
IRIS_DEEP = (230, 128, 24)
IRIS_HOT = (255, 230, 140)
RIM = (214, 76, 18)
PUPIL = (22, 10, 6)
EAR_IN = (156, 86, 64)

SIDES = (('front', 1.0), ('back', 0.84), ('right', 0.92), ('left', 0.92), ('top', 1.08), ('bottom', 0.62))


def locks_over_crown():
    """Locks of fur lying back over the crown and falling from the nape, each its own angle, overlapping."""
    out = []
    for n, (x, y, z, rx, rz, w, h) in enumerate((
            (-2.2, -9.2, 1.0, 1.25, 0.25, 2.2, 3.4), (0.0, -9.4, 1.2, 1.3, 0.0, 2.4, 3.8),
            (2.2, -9.2, 1.0, 1.25, -0.25, 2.2, 3.4), (-3.4, -8.4, 2.6, 0.9, 0.45, 2.0, 3.6),
            (3.4, -8.4, 2.6, 0.9, -0.45, 2.0, 3.6), (-1.6, -7.4, 4.4, 0.35, 0.18, 2.4, 4.6),
            (1.6, -7.4, 4.4, 0.35, -0.18, 2.4, 4.6), (0.0, -6.2, 4.7, 0.25, 0.0, 2.6, 5.0),
            (-3.2, -6.0, 4.0, 0.3, 0.4, 2.0, 4.2), (3.2, -6.0, 4.0, 0.3, -0.4, 2.0, 4.2))):
        out.append(part('crown_lock_%d' % n, 'head', (x, y, z), (rx, 0, rz),
                        [(-w / 2, 0, -0.6, w, h, 1.2, 'lock')]))
    return out


HEAD = mirrored([
    part('head', 'root', (0, 0, 0), (0, 0, 0), []),
    # the skull, rounded in tiers; its face recessed, the plate standing only a little before it
    part('skull', 'head', (0, 0, 0), (0, 0, 0), [
        (-4.0, -8.4, -3.0, 8.0, 8.2, 7.2, 'skull'),
        (-4.45, -7.5, -2.5, 8.9, 6.4, 6.2, 'skull_side'),
        (-3.4, -9.0, -2.5, 6.8, 0.7, 5.8, 'dome'),
        (-2.4, -9.5, -1.6, 4.8, 0.6, 4.2, 'dome'),
        (-3.3, -7.7, 4.1, 6.6, 6.2, 0.7, 'skull_back'),
        (-3.4, -6.5, -3.55, 6.8, 6.2, 0.6, 'face'),
        (-2.0, -0.6, -4.8, 4.0, 1.0, 2.0, 'jaw_fur'),
    ]),
    # the brow: a ridge in two halves drawn down to the nose, stepped back into the forehead's fur
    part('right_brow', 'head', (0, -6.45, -3.8), (0, 0, 0.15), [
        (-3.7, -0.7, -1.3, 3.7, 1.0, 1.3, 'brow'),
        (-3.5, -1.35, -0.75, 3.5, 0.75, 0.8, 'brow_back')]),
    part('glabella', 'head', (0, -6.35, -4.2), (0, 0, 0), [(-0.6, -0.5, -0.5, 1.2, 1.3, 0.8, 'skin_ridge')]),
    # the bridge of the nose, short, stepping down to the muzzle
    part('bridge', 'head', (0, -5.0, -3.75), (-0.8, 0, 0), [(-0.55, 0, -0.4, 1.1, 1.5, 0.8, 'bridge')]),
    # the muzzle in three tiers, each narrower and further out: its sides recede, its corners round off
    part('muzzle', 'head', (0, 0, 0), (0, 0, 0), [
        (-2.45, -4.0, -4.55, 4.9, 3.0, 1.0, 'muzzle_base'),
        (-2.0, -3.85, -5.4, 4.0, 2.7, 0.9, 'muzzle_mid'),
        (-1.45, -3.55, -6.1, 2.9, 2.1, 0.75, 'muzzle_front'),
        (-1.45, -1.35, -5.3, 2.9, 1.05, 1.2, 'chin'),
    ]),
    part('right_cheekbone', 'head', (-2.35, -3.6, -3.7), (0, 0.32, 0), [(-1.2, -0.5, -0.7, 2.4, 1.0, 0.9, 'cheekbone')]),
    # the frame of fur, standing proud of the face
    part('right_sideburn', 'head', (-3.75, -6.1, -3.1), (0.05, 0.2, 0.12), [(-0.9, 0, -0.95, 1.8, 5.2, 1.9, 'tuft')]),
    part('right_cheek_tuft_0', 'head', (-3.45, -3.3, -3.5), (0.15, 0.25, 0.6), [(-0.75, 0, -0.85, 1.5, 3.6, 1.7, 'tuft')]),
    part('right_cheek_tuft_1', 'head', (-3.95, -2.3, -2.5), (0.22, 0.35, 0.92), [(-0.75, 0, -0.85, 1.5, 3.4, 1.7, 'tuft')]),
    part('right_cheek_tuft_2', 'head', (-3.1, -0.9, -3.4), (0.45, 0.2, 0.5), [(-0.65, 0, -0.75, 1.3, 2.8, 1.5, 'tuft')]),
    part('right_brow_tuft', 'head', (-3.6, -7.0, -3.6), (0, 0.3, 1.15), [(-0.55, 0, -0.55, 1.1, 2.4, 1.1, 'tuft')]),
    # the ears: cups with a curled rim round an inner bowl, turned a little forward, a lock over each
    part('right_ear', 'head', (-4.85, -4.7, -0.1), (0.05, -0.6, 0.12), [(-0.4, -1.7, -1.7, 0.5, 3.4, 3.4, 'ear')]),
    part('right_ear_rim_0', 'right_ear', (-0.55, -1.550, 0.000), (0.000, 0, 0), [(-0.4, -0.3, -0.75, 0.8, 0.6, 1.5, 'ear_rim')]),
    part('right_ear_rim_1', 'right_ear', (-0.55, -1.096, 1.096), (-0.785, 0, 0), [(-0.4, -0.3, -0.75, 0.8, 0.6, 1.5, 'ear_rim')]),
    part('right_ear_rim_2', 'right_ear', (-0.55, -0.000, 1.550), (-1.571, 0, 0), [(-0.4, -0.3, -0.75, 0.8, 0.6, 1.5, 'ear_rim')]),
    part('right_ear_rim_3', 'right_ear', (-0.55, 1.096, 1.096), (-2.356, 0, 0), [(-0.4, -0.3, -0.75, 0.8, 0.6, 1.5, 'ear_rim')]),
    part('right_ear_rim_4', 'right_ear', (-0.55, 1.550, 0.000), (-3.142, 0, 0), [(-0.4, -0.3, -0.75, 0.8, 0.6, 1.5, 'ear_rim')]),
    part('right_ear_rim_5', 'right_ear', (-0.55, 1.096, -1.096), (-3.927, 0, 0), [(-0.4, -0.3, -0.75, 0.8, 0.6, 1.5, 'ear_rim')]),
    part('right_ear_rim_7', 'right_ear', (-0.55, -1.096, -1.096), (-5.498, 0, 0), [(-0.4, -0.3, -0.75, 0.8, 0.6, 1.5, 'ear_rim')]),
    # locks of fur at the temples, swept back over the skull's sides
    part('right_temple_lock', 'head', (-4.3, -7.2, -1.6), (1.2, 0.25, 0.12), [(-0.6, 0, -0.9, 1.2, 4.6, 1.8, 'lock')]),
    part('right_side_lock', 'head', (-4.4, -4.6, 1.6), (0.75, 0.15, 0.1), [(-0.6, 0, -0.9, 1.2, 4.2, 1.8, 'lock')]),
    part('right_ear_tuft', 'head', (-4.7, -6.2, 0.9), (0.5, 0.0, 0.5), [(-0.6, 0, -0.65, 1.2, 2.6, 1.3, 'tuft')]),
] + locks_over_crown())


def k(c, f):
    return [v * f for v in c[:3]]


def mixc(a, b, t):
    t = max(0.0, min(1.0, t))
    return [x + (y - x) * t for x, y in zip(a, b)]


def smooth_noise(p, n, scale):
    """A smooth wandering line of values, n long: random points joined by cosine curves."""
    pts = [p.rnd.uniform(-1, 1) for _ in range(n // scale + 3)]
    out = []
    for i in range(n):
        a, t = divmod(i / scale, 1)
        a = int(a)
        t = (1 - math.cos(t * math.pi)) / 2
        out.append(pts[a] * (1 - t) + pts[a + 1] * t)
    return out


def fur(p, rect, light=1.0, base=FUR, slant=0.0, root_dark=True):
    """Fur: soft locks of differing widths, overlapping, each shading from its root to a lit tip, with
    fine strands within; partings soft, colours blending, no hard stripes."""
    fx, fy, fw, fh = rect
    if fw <= 0 or fh <= 0:
        return
    wave = smooth_noise(p, fw + fh + 4, 5)
    hue = smooth_noise(p, fw + fh + 4, 7)
    strand = [p.rnd.uniform(-0.11, 0.11) for _ in range(fw + fh + 4)]
    for j in range(fh):
        t = j / max(1, fh - 1)
        for i in range(fw):
            x = i + slant * j
            xi = int(x) % (fw + fh + 4)
            crest = 0.5 + 0.5 * wave[xi]
            c = mixc(FUR_DARK, FUR_LIGHT, 0.2 + 0.6 * crest)
            c = mixc(c, base, 0.45)
            c = mixc(c, (196, 112, 48), 0.15 * (0.5 + 0.5 * hue[xi]))
            f = light * (1 + strand[xi])
            if (i * 7 + j // 3) % 11 == 0:
                f *= 1.1
            if root_dark:
                f *= 0.86 + 0.22 * t if t < 0.6 else 1.0 - 0.12 * (t - 0.6)
            p.px((fx + i, fy + j), [v * f + p.rnd.randint(-3, 3) for v in c])


def shaded(p, f, base, top=1.08, bottom=0.6, side=0.86):
    """Skin: lit above, the sides falling into shade toward their edges, the underside dark."""
    for s, light in SIDES:
        fx, fy, fw, fh = f[s]
        for j in range(fh):
            for i in range(fw):
                t = j / max(1, fh - 1)
                if s == 'top':
                    l = top
                elif s == 'bottom':
                    l = bottom
                elif s in ('right', 'left'):
                    l = side * (1.02 - 0.18 * t)
                else:
                    edge = abs(i + 0.5 - fw / 2) / (fw / 2)
                    l = light * (1.04 - 0.14 * t) * (1 - 0.12 * edge ** 2)
                p.px((fx + i, fy + j), [v * l + p.rnd.randint(-2, 2) for v in base])


def taper(p, rect, sharp=1.3):
    """Cuts a lock's face to a point at its end, a little ragged."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        half = (fw / 2) * (1 - (j / fh) ** sharp) + 0.4
        for i in range(fw):
            if abs(i + 0.5 - fw / 2) > half + p.rnd.uniform(-0.3, 0.3):
                p.clear((fx + i, fy + j))


def face(p, rect):
    """The face's front: heart-shaped bare skin in its frame of fur, blending into it at the edge; the
    sockets in shadow, the eyes set in them, small and fierce; the cheeks hollow under the cheekbones."""
    fx, fy, fw, fh = rect
    fur(p, rect, 0.85, base=FUR_DARK)
    cx = fw / 2
    r = fw * 0.25
    eye_y = fh * 0.12
    for j in range(fh):
        for i in range(fw):
            x, y = i + 0.5, j + 0.5
            lobe = min(math.hypot(x - (cx - r * 0.95), y - fh * 0.3), math.hypot(x - (cx + r * 0.95), y - fh * 0.3)) - r * 1.12
            half = (cx - 0.3) * max(0.0, 1 - (y - fh * 0.3) / (fh * 0.78))
            body = abs(x - cx) - half
            d = min(lobe, body) if y > fh * 0.3 else lobe
            if y < fh * 0.04 or d > 1.2:
                continue
            ex = abs(x - cx)
            c = mixc(SKIN, SKIN_LIGHT, 0.55 - 0.5 * ex / cx)
            # the sockets: a soft shadow round each eye
            near_eye = math.hypot((ex - r * 0.95) / (r * 1.25), (y - eye_y - 2.2) / (fh * 0.16))
            if near_eye < 1:
                c = mixc(SKIN_DEEP, c, near_eye ** 1.2)
            if y > fh * 0.45 and ex > cx * 0.4:              # the hollow of the cheek
                c = k(c, 0.84 + 0.16 * min(1.0, abs(y - fh * 0.62) / (fh * 0.2)))
            if d > -1.5:                                     # blending into the fur at its edge
                c = mixc(c, mixc(FUR_DARK, FUR_DEEP, 0.4), (d + 1.5) / 2.7)
            p.px((fx + i, fy + j), [v + p.rnd.randint(-2, 2) for v in c])
    # the eyes: small almonds tapering at both ends, the outer corner raised, the inner drawn down
    for side in (-1, 1):
        ecx = cx + side * r * 0.98
        w = 6
        for n in range(w):
            i = int(round(ecx - w / 2 + n)) if side < 0 else int(round(ecx + w / 2 - 1 - n))
            t = n / (w - 1)                     # 0 at the outer corner .. 1 at the inner
            open_ = math.sin(math.pi * (0.12 + 0.76 * t)) * 2.6       # how open it is here
            mid = eye_y + 1.8 + 0.9 * t                               # the eye's line sloping down inward
            top, bottom = mid - open_ / 2, mid + open_ / 2
            j0, j1 = int(math.floor(top + 0.25)), int(math.ceil(bottom - 0.25))
            for j in range(j0, max(j0 + 1, j1)):
                v = (j + 0.5 - top) / max(0.5, bottom - top)            # 0 at the lid .. 1 below
                c = mixc(IRIS_DEEP, IRIS, 0.4 + 0.6 * v) if v > 0.25 else IRIS_DEEP
                if n in (0, w - 1):
                    c = RIM
                p.px((fx + i, fy + j), c, glow=True)
            # the upper lid's dark line
            p.px((fx + i, fy + j0 - 1), SKIN_DEEP)
        # the pupil, a little toward the nose: he glares ahead
        pc = int(round(ecx - side * 0.5))
        pj = int(eye_y + 2.0 + 0.5)
        for j in (pj, pj + 1):
            p.px((fx + pc, fy + j), PUPIL)
            p.glow.putpixel((fx + pc, fy + j), (0, 0, 0, 0))
        # a glint of light in the iris
        p.px((fx + pc + side, fy + pj), IRIS_HOT, glow=True)


def muzzle_front(p, f):
    """The muzzle's front tier: its face rounded by shading, the nostrils at its top, the upper lip."""
    shaded(p, f, SKIN_LIGHT, top=1.12, bottom=0.55, side=0.84)
    fx, fy, fw, fh = f['front']
    cx = fw / 2
    for j in range(fh):
        for i in range(fw):
            t = j / max(1, fh - 1)
            edge = abs(i + 0.5 - cx) / cx
            c = mixc(SKIN_HIGH, SKIN, 0.35 + 0.5 * t)
            c = k(c, 1 - 0.2 * edge ** 2)
            p.px((fx + i, fy + j), c)
    for side in (-1, 1):                        # flat nostrils, set wide, sloping outward
        nx = int(cx + side * 1.6)
        p.px((fx + nx, fy + 2), NOSTRIL)
        p.px((fx + nx + (side if side > 0 else 0) - (0 if side > 0 else 1), fy + 2), k(NOSTRIL, 1.5))
        p.px((fx + nx, fy + 3), k(NOSTRIL, 1.8))
    for j in range(4, 6):                       # the philtrum
        p.px((fx + int(cx) - 1, fy + j), mixc(SKIN_DARK, SKIN, 0.4))
        p.px((fx + int(cx), fy + j), mixc(SKIN_DARK, SKIN, 0.4))
    for i in range(1, fw - 1):                  # the upper lip's soft edge
        p.px((fx + i, fy + fh - 2), mixc(LIP, SKIN, 0.4))
    for i in range(0, fw):                      # the mouth, along the front's foot
        p.px((fx + i, fy + fh - 1), MOUTH)


def muzzle_tier(p, f, base, mouth_row=None, smirk=False):
    """A tier of the muzzle behind the front: its skin shading off down the sides; the mouth running
    round its front at the same height as the front tier's, a smirk drawn up at one corner."""
    shaded(p, f, base, top=1.06, bottom=0.55, side=0.8)
    if mouth_row is None:
        return
    fx, fy, fw, fh = f['front']
    for i in range(fw):
        j = mouth_row
        if smirk and i >= fw - 3:
            j -= 1 + (i >= fw - 2)
        if 0 <= j < fh:
            p.px((fx + i, fy + j), MOUTH)
            if j + 1 < fh:
                p.px((fx + i, fy + j + 1), mixc(LIP, SKIN, 0.3))


def paint_box(p, style, f, dims):
    if style in ('skull', 'skull_side', 'dome', 'skull_back', 'jaw_fur'):
        for s, light in SIDES:
            fur(p, f[s], light, base=FUR, slant=0.2 if s in ('right', 'left') else 0.0)
    elif style == 'face':
        for s, light in SIDES:
            if s == 'front':
                face(p, f[s])
            else:
                fur(p, f[s], light * 0.8, base=FUR_DARK)
    elif style in ('brow', 'brow_back'):
        for s, light in SIDES:
            if s == 'bottom':
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    for i in range(fw):
                        p.px((fx + i, fy + j), mixc(FUR_DEEP, SKIN_DEEP, 0.4 + 0.3 * j / max(1, fh - 1)))
            else:
                fur(p, f[s], light * (1.08 if s in ('top', 'front') else 0.9), base=FUR, slant=0.5, root_dark=False)
        if style == 'brow':                              # its foot shading into the socket
            fx, fy, fw, fh = f['front']
            for j in range(max(0, fh - 3), fh):
                for i in range(fw):
                    t = (j - (fh - 3)) / 2.0
                    p.px((fx + i, fy + j), mixc(mixc(FUR_DARK, FUR, 0.5), SKIN_DEEP, t))
    elif style == 'skin_ridge':
        shaded(p, f, SKIN, top=1.1, bottom=0.6)
    elif style == 'bridge':
        shaded(p, f, SKIN, top=1.12, bottom=0.6, side=0.8)
        fx, fy, fw, fh = f['front']
        for j in range(fh):
            for i in range(fw):
                if abs(i + 0.5 - fw / 2) < fw * 0.2:
                    p.px((fx + i, fy + j), mixc(SKIN, SKIN_LIGHT, 0.6))
    elif style == 'cheekbone':
        shaded(p, f, SKIN, top=1.15, bottom=0.6, side=0.85)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy), mixc(SKIN_LIGHT, SKIN_HIGH, 0.4))
    elif style == 'muzzle_front':
        muzzle_front(p, f)
    elif style == 'muzzle_mid':
        # its mouth at the height of the front tier's foot: the front's foot is 0.3 above the mid's
        rows = f['front'][3]
        muzzle_tier(p, f, SKIN_LIGHT, mouth_row=int(round(rows * (2.1 + 0.3) / 2.7)) - 1, smirk=True)
    elif style == 'muzzle_base':
        shaded(p, f, SKIN, top=1.04, bottom=0.55, side=0.78)
    elif style == 'chin':
        shaded(p, f, SKIN, top=0.8, bottom=0.5, side=0.78)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy), mixc(LIP, SKIN, 0.35))
    elif style == 'tuft':
        for s, light in SIDES:
            fur(p, f[s], light * 1.04, base=FUR, slant=0.3)
            if s in ('front', 'back', 'right', 'left'):
                taper(p, f[s])
    elif style == 'lock':
        for s, light in SIDES:
            fur(p, f[s], light, base=FUR, slant=0.15)
            if s in ('front', 'back', 'right', 'left'):
                taper(p, f[s], sharp=1.6)
    elif style == 'ear':
        for s, light in SIDES:
            fur(p, f[s], light * 0.9, base=FUR_DARK)
        for s in ('left', 'right'):                      # the bowl within: bare, deep in shadow at its heart
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    r = math.hypot((i + 0.5 - fw / 2) / (fw / 2), (j + 0.5 - fh / 2) / (fh / 2))
                    if r > 1:
                        p.clear((fx + i, fy + j))
                    else:
                        p.px((fx + i, fy + j), mixc(SKIN_DEEP, EAR_IN, r ** 0.8))
    elif style == 'ear_rim':
        shaded(p, f, SKIN, top=1.12, bottom=0.62, side=0.9)
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


# ── The same head for another: the Six-Eared Macaque (flower_fruit.py) ──────────────────────────────

def head_with_ears(n, gap=2.9):
    """The head with its ears (each cup and its rim) `n` a side, stacked close, `gap` apart: the
    Six-Eared Macaque's three, so that at a glance he passes for the Monkey King. The rest as HEAD."""
    out = []
    for name, parent, pivot, rot, boxes, mirror in HEAD:
        if 'ear' in name and 'tuft' not in name and 'year' not in name:
            continue
        out.append((name, parent, pivot, rot, boxes, mirror))
    ears = [p_ for p_ in HEAD if ('_ear' in p_[0] and 'tuft' not in p_[0])]
    for e in range(n):
        dy = (e - (n - 1) / 2) * gap
        tilt = (e - (n - 1) / 2) * 0.12
        for name, parent, (px, py, pz), (rx, ry, rz), boxes, mirror in ears:
            new = name.replace('_ear', '_ear_%d' % e, 1)
            if parent == 'head':
                out.append((new, 'head', (px, py + dy, pz + abs(e - (n - 1) / 2) * 0.3), (rx, ry, rz + (tilt if px < 0 else -tilt)), boxes, mirror))
            else:
                out.append((new, parent.replace('_ear', '_ear_%d' % e, 1), (px, py, pz), (rx, ry, rz), boxes, mirror))
    return out


def generate_for(painter_class, name, java, tag, parts, eyes=None, seed=77):
    """Paints `parts` (HEAD, or head_with_ears) as `name`, its eyes `eyes` (iris, deep, hot, rim) if
    given, and writes its model into `java` between its `tag` markers. The Buddha's own is generate()."""
    global IRIS, IRIS_DEEP, IRIS_HOT, RIM
    keep = (IRIS, IRIS_DEEP, IRIS_HOT, RIM)
    if eyes:
        IRIS, IRIS_DEEP, IRIS_HOT, RIM = eyes
    try:
        bg.UNITS = 64
        bg.S = 4
        bg.TEX = bg.UNITS * bg.S
        p = painter_class(seed)
        slots = exact_paint(parts, p, paint_box)
        p.img.save('src/main/resources/assets/jttw_engine/textures/entity/%s.png' % name)
        p.glow.save('src/main/resources/assets/jttw_engine/textures/entity/%s_glow.png' % name)
        emit(parts, slots, java, tag)
        print(name, len(parts), 'parts,', len(slots), 'boxes')
    finally:
        IRIS, IRIS_DEEP, IRIS_HOT, RIM = keep
