"""Xiaotian Quan (哮天犬), Erlang Shen's black hound: its model and its three skins from one list of parts
(beastgen). Writes XiaotianQuanModel.createBodyLayer() between its GENERATED markers and paints
textures/entity/xiaotian_quan.png (with a glowing skin: its eyes, faintly its gold markings) and
xiaotian_quan_berserk.png (the markings and eyes crimson, burning). The player Erlang's summoned hound and
the hound at the NPC Erlang's heel (HeavenGreatRenderers) are this one model.

A lean, noble hound, between a sighthound and a mastiff: a deep chest dropping to a keel between the
elbows, the waist tucked up hard behind the ribs, a short loin rising to a broad croup and muscled thighs;
long, clean legs, the hind ones angled at stifle and hock, close knuckled paws with pale claws. The neck
long and arched, the head long and lean, a strong muzzle and a jaw that shuts square, deep jowls; pricked
ears swept back; the tail a sickle curl, feathered beneath. A gold collar with a bell and a red tassel at
its throat. Its coat black, blue-grey where the light runs along it, brushed in strands; gold cloud-scroll
markings on shoulder and thigh, a flame on the brow; amber eyes.

The group names (head, upper_body, lower_body, front_right, front_left, rear_right, rear_left, tail) are
what XiaotianQuanAnimations' walk keys: keep them. The model is drawn SCALE of its first drawing's size,
about the ground. Run from the repository root: python assets_source/xiaotian_quan.py"""
import math

import beastgen as bg
from beastgen import emit
from heaven_host_kit import (HostPainter, paint, part, mixc, k, noisy, each, rim, gilt, gilt_rounded, horsehair, point, ragged,
                             GOLD, GOLD_LIGHT, GOLD_DEEP, LIGHTS, SIDES)

bg.UNITS = 128
bg.S = 4
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/model/XiaotianQuanModel.java'
SCALE = 0.86            # the whole hound, about the ground

# ── The parts ────────────────────────────────────────────────────────────────
# Drawn below at full size (ground y 24, the head toward -z, its right side -x); scaled after.


def side(name, x_sign, parts):
    """A leg's parts for one side: x mirrored (and the texture with it) for the left."""
    out = []
    for n, par, (px, py, pz), (rx, ry, rz), boxes, m in parts:
        if x_sign < 0:
            out.append((n, par, (px, py, pz), (rx, ry, rz), boxes, False))
        else:
            out.append((n.replace('right_', 'left_'), par.replace('right_', 'left_').replace('_right', '_left'), (-px, py, pz),
                        (rx, -ry, -rz), [(-x - w, y, z, w, h, d, s) for (x, y, z, w, h, d, s) in boxes], True))
    return out


def paw(name, parent, at, rot):
    return part(name, parent, at, rot, [
        (-1.2, 0.0, -2.0, 2.4, 1.2, 2.8, 'paw'),
        (-1.05, 0.25, -2.75, 2.1, 0.95, 0.8, 'toes'),         # the knuckled toes
        (-0.9, 0.85, -3.05, 1.8, 0.35, 0.4, 'claws'),         # pale claws over the ground
    ])


def front_leg():
    """The foreleg, hung from the shoulder: a muscled upper arm back to the elbow, the forearm straight down,
    the pastern sloping a little, the paw."""
    return [
        part('right_upperarm', 'front_right', (0, 0, 0), (0.18, 0, 0), [(-1.35, -1.4, -1.6, 2.7, 5.9, 3.2, 'upper')]),
        part('right_forearm', 'right_upperarm', (0, 4.4, 0), (-0.18, 0, 0), [(-0.9, -0.4, -0.95, 1.8, 5.0, 1.9, 'shank')]),
        part('right_pastern', 'right_forearm', (0, 4.5, 0), (-0.22, 0, 0), [(-0.8, -0.3, -0.85, 1.6, 1.6, 1.7, 'shank')]),
        paw('right_forepaw', 'right_pastern', (0, 1.25, 0), (0.22, 0, 0)),
    ]


def hind_leg():
    """The hind leg from the hip: the broad thigh down and forward to the stifle, the second thigh back
    down to the hock, the hock near upright, the paw."""
    return [
        part('right_thigh', 'rear_right', (0, 0, 0), (-0.32, 0, 0), [(-1.55, -2.0, -2.3, 3.1, 6.4, 4.4, 'thigh')]),
        part('right_gaskin', 'right_thigh', (0, 4.0, -0.1), (0.95, 0, 0), [(-1.05, -0.5, -1.15, 2.1, 4.5, 2.4, 'gaskin')]),
        part('right_hock', 'right_gaskin', (0, 3.8, 0), (-0.7, 0, 0), [(-0.8, -0.4, -0.85, 1.6, 3.0, 1.7, 'shank')]),
        paw('right_hindpaw', 'right_hock', (0, 2.45, 0), (0.07, 0, 0)),
    ]


def tail():
    """A sickle tail: four joints curling up and over, a feathering of long hair beneath each."""
    out, par, at = [], 'tail', (0, 0, 0)
    for n, (rot, w, length) in enumerate([(0.55, 1.6, 3.6), (0.45, 1.45, 3.4), (0.55, 1.3, 3.1), (0.65, 1.05, 2.8)]):
        name = 'tail_%d' % n
        boxes = [(-w / 2, -w / 2, -0.2, w, w, length, 'tail')]
        boxes.append((-0.3, w / 2 - 0.1, 0.1, 0.6, 1.3 + 0.3 * n, length - 0.4, 'plume'))
        out.append(part(name, par, at, (rot, 0, 0), boxes))
        par, at = name, (0, 0, length - 0.35)
    return out


def hound():
    parts = [
        # the forequarters: the deep ribcage, the keel between the elbows, the forechest, the withers
        part('upper_body', 'root', (0, 24, 0), (0, 0, 0), [
            (-3.4, -13.5, -8.2, 6.8, 6.4, 8.6, 'chest'),
            (-2.6, -7.3, -7.4, 5.2, 1.2, 5.6, 'keel'),
            (-2.5, -12.6, -9.2, 5.0, 4.8, 1.2, 'forechest'),
            (-2.4, -14.1, -7.4, 4.8, 0.8, 5.0, 'withers'),
            (-2.9, -8.4, -0.4, 5.8, 1.2, 2.2, 'ribs_end'),          # the last ribs, stepping up to the tuck
        ]),
        part('right_shoulder', 'upper_body', (-3.3, -12.6, -6.2), (0.3, 0, 0.06), [(-0.6, -0.5, -2.2, 1.0, 5.4, 4.6, 'shoulder')]),
        # the neck, long and arched, rising forward from the withers; the collar at its foot
        part('neck', 'upper_body', (0, -11.4, -6.6), (0.62, 0, 0), [
            (-2.05, -4.6, -2.25, 4.1, 5.6, 4.5, 'neck'),
            (-1.75, -8.6, -1.95, 3.5, 4.4, 3.9, 'neck'),
            (-1.2, -8.2, 1.55, 2.4, 6.2, 0.9, 'crest'),             # the crest of the neck, its arch
            (-1.5, -5.4, -2.75, 3.0, 4.6, 0.8, 'throat'),
        ]),
        part('collar', 'neck', (0, -2.6, 0), (0, 0, 0), [(-2.4, -0.75, -2.6, 4.8, 1.5, 5.2, 'collar')]),
        part('bell', 'collar', (0, 0.55, -2.75), (-0.62, 0, 0), [
            (-0.8, 0.0, -0.8, 1.6, 1.5, 1.6, 'bell'),
            (-1.0, 1.25, -1.0, 2.0, 0.5, 2.0, 'bell_lip'),
            (-0.35, -0.5, -0.35, 0.7, 0.6, 0.7, 'bell_loop'),
        ]),
        part('tassel', 'bell', (0, 1.75, 0), (0, 0, 0), [(-0.5, 0, -0.5, 1.0, 0.6, 1.0, 'tassel_knot'),
                                                        (-0.45, 0.5, -0.45, 0.9, 2.4, 0.9, 'tassel')]),
        # the hindquarters: the loin, tucked up; the croup, broad over the thighs
        part('lower_body', 'root', (0, 13.5, 4), (0, 0, 0), [
            (-2.6, -2.8, -4.4, 5.2, 4.0, 6.4, 'loin'),
            (-3.1, -3.0, 1.0, 6.2, 5.4, 5.8, 'croup'),
            (-2.2, -3.4, -2.0, 4.4, 0.6, 7.6, 'spine'),
        ]),
        # the head, held level on the arched neck
        part('head', 'root', (0, 6.6, -10.8), (0, 0, 0), [
            (-2.3, -2.6, -3.9, 4.6, 3.8, 4.6, 'skull'),
            (-2.5, -0.6, -3.5, 5.0, 2.4, 2.8, 'jowl'),
            (-1.95, -2.95, -4.35, 3.9, 0.75, 1.2, 'brow'),
            (-1.35, -1.3, -6.4, 2.7, 1.85, 2.7, 'muzzle'),          # the muzzle, dropped below the eyes at the stop
            (-1.0, -1.15, -8.9, 2.0, 1.6, 2.6, 'snout'),
            (-0.75, -1.4, -9.35, 1.5, 1.0, 0.8, 'nose'),
            (-1.15, 0.2, -8.4, 2.3, 0.65, 4.7, 'flews'),          # the upper lip hanging over the jaw
            (-1.6, -1.3, 0.2, 3.2, 2.6, 1.0, 'nape'),
        ]),
        part('jaw', 'head', (0, 0.6, -3.5), (0.07, 0, 0), [(-1.1, 0.0, -4.5, 2.2, 1.0, 4.5, 'jaw')]),
        part('right_ear', 'head', (-1.45, -2.3, -1.1), (-0.42, 0.1, -0.2), [(-0.95, -3.3, -0.35, 1.9, 3.5, 0.7, 'ear')]),
        part('front_right', 'root', (-2.25, 13.0, -5.9), (0, 0, 0), []),
        part('rear_right', 'root', (-2.25, 13.6, 8.6), (0, 0, 0), []),
        part('tail', 'root', (0, 11.4, 10.4), (0, 0, 0), []),
    ]
    legs = front_leg() + hind_leg()
    out = []
    for p in parts:
        out += side(None, -1, [p])
        if p[0].startswith('right_') or p[0].endswith('_right'):
            out += side(None, 1, [p])
    out += side(None, -1, legs) + side(None, 1, legs) + tail()
    # each left part's name and parent: front_left's children under it
    fixed = []
    for n, par, piv, rot, boxes, m in out:
        if n.endswith('_right') and m:
            n = n[:-6] + '_left'
        fixed.append((n, par, piv, rot, boxes, m))
    return fixed


def scaled(parts, f):
    """The hound at f of its size, about the ground."""
    out = []
    for n, par, (px, py, pz), rot, boxes, m in parts:
        piv = (px * f, 24 + (py - 24) * f, pz * f) if par == 'root' else (px * f, py * f, pz * f)
        out.append((n, par, piv, rot, [tuple(v * f for v in b[:6]) + (b[6],) for b in boxes], m))
    return out


HOUND = scaled(hound(), SCALE)


# ── Its colours ──────────────────────────────────────────────────────────────

SHADOW = (6, 6, 10)
FUR = (22, 23, 30)
SHEEN = (58, 68, 88)          # blue-grey, where the light runs along the coat
SHINE = (110, 126, 150)
BRONZE = (120, 86, 48)        # a warm glint here and there
GOLD_MARK = (214, 164, 60)
CRIMSON = (232, 40, 30)
EYE_AMBER = (255, 184, 56)
EYE_RED = (255, 54, 30)
CLAW = (178, 172, 160)
NOSE = (12, 12, 14)
RED = (196, 30, 28)
RED_DEEP = (100, 10, 12)


def fur(p, f, light=1.0, sheen=1.0, belly=False):
    """A short black coat: the light running blue-grey along its top and the upper flanks, falling to near
    black beneath, darker to each edge so the form rounds; brushed in strands, a rare bronze glint."""
    for s, (fx, fy, fw, fh) in f.items():
        for i in range(fw):
            # a strand: each column's own tone, renewed every few pixels down it
            seed = p.rnd.random()
            for j in range(fh):
                if j % 5 == 0:
                    seed = p.rnd.random()
                u, t = (i + 0.5) / fw, (j + 0.5) / fh
                round_ = 1 - 0.55 * ((u - 0.5) * 2) ** 2
                if s == 'top':
                    lift = 0.75 * round_
                elif s == 'bottom':
                    lift = 0.0
                else:
                    lift = max(0.0, (0.9 - t * 1.4)) * round_ * (0.8 if s == 'back' else 1.0)
                if belly and s in ('right', 'left'):
                    lift *= 0.8
                lift = lift * sheen * LIGHTS[s] * light
                col = mixc(FUR, SHEEN, min(1, lift)) if lift < 1 else mixc(SHEEN, SHINE, min(1, lift - 1))
                if s == 'bottom' or t > 0.75 and s != 'top':
                    col = mixc(col, SHADOW, 0.55 if s == 'bottom' else (t - 0.75) * 1.6)
                col = k(col, 0.86 + 0.28 * seed)
                if lift > 0.45 and p.rnd.random() < 0.025:
                    col = mixc(col, BRONZE if p.rnd.random() < 0.35 else SHINE, 0.6)
                p.px((fx + i, fy + j), noisy(p, col, 2))


def scroll(p, rect, colour, glow, cx=0.5, cy=0.5, scale=1.0, flip=False):
    """A gold cloud scroll (祥云) on a face: a spiral curling in, a second smaller curl and a tail sweeping
    back; `glow` how brightly it shines on the glowing skin."""
    fx, fy, fw, fh = rect
    r0 = min(fw, fh) * 0.32 * scale
    ox, oy = fx + fw * cx, fy + fh * cy
    pts = []
    for n in range(160):
        a = n / 160 * 2.6 * math.pi
        r = r0 * (1 - a / (3.1 * math.pi))
        pts.append((ox + r * math.cos(a), oy + r * math.sin(a)))
    for n in range(90):
        a = n / 90 * 1.8 * math.pi
        r = r0 * 0.55 * (1 - a / (2.4 * math.pi))
        pts.append((ox + r0 * 1.25 + r * math.cos(-a + math.pi), oy - r0 * 0.25 + r * math.sin(-a + math.pi)))
    for n in range(60):
        t = n / 60
        pts.append((ox - r0 * (1 + 1.4 * t), oy + r0 * (1 - 0.3 * t * t)))
    for x, y in pts:
        if flip:
            x = 2 * ox - x
        for dx, dy in ((0, 0),):                          # a fine line: a thread of gold, not a blot
            xy = (int(x) + dx, int(y) + dy)
            if fx <= xy[0] < fx + fw and fy <= xy[1] < fy + fh:
                p.lit(xy, colour, glow=glow, skin=noisy(p, colour, 6))


def flame(p, rect, colour, glow):
    """A small flame mark: three tongues rising from a point."""
    fx, fy, fw, fh = rect
    cx = fx + fw / 2
    for j in range(fh):
        t = j / max(1, fh - 1)                         # 0 at the tips .. 1 at the root
        half = fw * 0.36 * math.sin(min(1, t * 1.15) * math.pi * 0.85)
        for i in range(fw):
            dx = fx + i + 0.5 - cx
            if abs(dx) > half:
                continue
            tongue = abs(math.sin((dx / max(1, fw)) * math.pi * 3))
            if t < 0.45 and tongue < 0.5 - t:
                continue
            p.lit((fx + i, fy + j), colour, glow=glow, skin=noisy(p, mixc(colour, (255, 250, 220), 0.3 * (1 - t)), 5))


def flat_eye(p, rect, iris, glow):
    """One flat block of eye: the iris, lit (a shade darker along its bottom), or with no iris a dark pupil."""
    x, y, w, h = rect
    for j in range(h):
        for i in range(w):
            if iris is None:
                p.px((x + i, y + j), (6, 4, 4))
                p.glow.putpixel((x + i, y + j), (0, 0, 0, 0))
            else:
                p.lit((x + i, y + j), iris if j < h - 1 else k(iris, 0.8), glow=glow)


def painter(berserk):
    mark = CRIMSON if berserk else GOLD_MARK
    mark_glow = 1.0 if berserk else 0.28
    iris = EYE_RED if berserk else EYE_AMBER
    eye_glow = 1.0 if berserk else 0.55

    def paint_box(p, style, f, dims):
        if style in ('chest', 'withers', 'forechest', 'loin', 'croup', 'spine', 'neck', 'crest', 'nape', 'upper', 'thigh', 'gaskin',
                     'shank', 'tail', 'shoulder'):
            fur(p, f, belly=style in ('loin',))
            if style == 'shoulder':                       # a cloud scroll over the shoulder blade
                scroll(p, f['right'], mark, mark_glow, cx=0.5, cy=0.42, scale=1.25)
            if style == 'thigh':                          # and one over each thigh
                scroll(p, f['right'], mark, mark_glow, cx=0.55, cy=0.38, scale=1.1, flip=True)
            if style == 'shank':                          # the legs darker, the light only down their fronts
                for s in ('right', 'left', 'back'):
                    fx, fy, fw, fh = f[s]
                    for i, j, xy in each(f[s]):
                        c = p.get(xy)
                        p.px(xy, k(c, 0.82))
            return
        if style in ('keel', 'ribs_end', 'throat', 'jowl'):
            fur(p, f, light=0.85, sheen=0.7)
            return
        if style == 'skull':
            fur(p, f, sheen=1.1)
            # one eye each side, flat and blocky like vanilla's, in the band between the brow and the dropped muzzle:
            # from the front an iris block at the skull's edge with the pupil inside it, like the wolf's; the iris
            # wraps round the corner onto the side face (flush either side of the front in the UV), so from the side
            # and three-quarters it is the same eye
            # a thin pupil in each eye, seen from the front only; from the side just the iris
            h = 3
            fx, fy, fw, fh = f['front']
            y = fy + 2
            flat_eye(p, (fx, y, 5, h), iris, eye_glow)
            flat_eye(p, (fx + 2, y, 1, h), None, 0)
            flat_eye(p, (fx + fw - 5, y, 5, h), iris, eye_glow)
            flat_eye(p, (fx + fw - 3, y, 1, h), None, 0)
            fx, fy, fw, fh = f['right']
            flat_eye(p, (fx + fw - 6, y, 6, h), iris, eye_glow)
            fx, fy, fw, fh = f['left']
            flat_eye(p, (fx, y, 6, h), iris, eye_glow)
            fx, fy, fw, fh = f['top']                    # a flame on the brow
            flame(p, (fx + fw // 2 - 3, fy + fh - 9, 6, 8), mark, mark_glow)
            return
        if style == 'brow':
            fur(p, f, sheen=1.2)
            fx, fy, fw, fh = f['front']
            flame(p, (fx + fw // 2 - 3, fy, 6, fh), mark, mark_glow)
            return
        if style == 'snout':
            fur(p, f, light=0.92, sheen=0.85)
            fx, fy, fw, fh = f['front']                   # the mouth's line under the nose
            for j in range(fh // 2, fh):
                p.px((fx + fw // 2, fy + j), (2, 2, 4))
            return
        if style == 'muzzle':
            fur(p, f, light=0.95, sheen=0.9)
            for s in ('right', 'left'):                  # whisker pits along the muzzle
                fx, fy, fw, fh = f[s]
                for i in range(3, fw - 3, 3):
                    p.px((fx + i, fy + fh - 3), (4, 4, 6))
            return
        if style == 'nose':                               # a black nose, wet: a cool shine on its top
            for s, r in f.items():
                p.face(r, NOSE, LIGHTS[s], noise=2)
            fx, fy, fw, fh = f['top']
            for i in range(1, fw - 1):
                p.px((fx + i, fy + fh // 2), (92, 98, 112))
            fx, fy, fw, fh = f['front']
            for i in (1, fw - 2):
                p.px((fx + i, fy + fh - 2), (0, 0, 0))
                p.px((fx + i, fy + fh - 1), (0, 0, 0))
            return
        if style in ('flews', 'jaw'):
            fur(p, f, light=0.8, sheen=0.6)
            for s in ('right', 'left', 'front'):          # the lip line, black
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + (fh - 1 if style == 'flews' else 0)), (2, 2, 4))
            return
        if style == 'ear':                                 # pricked: pointed, the inner ear dusky
            fur(p, f, sheen=1.2)
            fx, fy, fw, fh = f['front']
            for i, j, xy in each(f['front']):
                if abs(i - fw / 2 + 0.5) < fw * 0.28 and j > 2:
                    p.px(xy, noisy(p, mixc((54, 38, 46), (26, 20, 26), j / fh), 2))
            for s in SIDES:
                point(p, f[s], keep=0.5, sharp=0.55, down=False)
            return
        if style == 'paw':
            fur(p, f, light=0.8, sheen=0.5)
            return
        if style == 'toes':                               # knuckled toes, the clefts between them dark
            fur(p, f, light=0.85, sheen=0.6)
            fx, fy, fw, fh = f['front']
            for q in (1, 2, 3):
                x = fx + round(fw * q / 4)
                for j in range(fh):
                    p.px((x, fy + j), (2, 2, 4))
            return
        if style == 'claws':
            for s, r in f.items():
                p.face(r, CLAW, LIGHTS[s], noise=6)
            fx, fy, fw, fh = f['front']
            for q in (1, 2, 3):
                x = fx + round(fw * q / 4)
                for j in range(fh):
                    p.clear((x, fy + j))
            return
        if style == 'plume':                              # long hair feathering under the tail
            for s, r in f.items():
                horsehair(p, r, (40, 46, 60), (8, 8, 12), LIGHTS[s], cut=0.5 if s in ('right', 'left') else 0)
            return
        if style == 'collar':                             # a gold collar, studded with red jewels
            gilt_rounded(p, f, 1.05)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                rim(p, f[s], GOLD_DEEP, 'tb')
                for i in range(2, fw - 1, 6):
                    p.px((fx + i, fy + fh // 2), RED)
                    p.px((fx + i + 1, fy + fh // 2), RED)
                    p.px((fx + i, fy + fh // 2 - 1), (255, 120, 110))
            return
        if style in ('bell', 'bell_lip', 'bell_loop'):
            gilt_rounded(p, f, 1.1)
            if style == 'bell':
                for s in SIDES:                           # the bell's slot
                    fx, fy, fw, fh = f[s]
                    for j in range(fh // 2, fh):
                        p.px((fx + fw // 2, fy + j), (40, 24, 8))
            return
        if style == 'tassel_knot':
            for s, r in f.items():
                p.face(r, RED, LIGHTS[s], noise=6)
            return
        if style == 'tassel':
            for s, r in f.items():
                horsehair(p, r, RED, RED_DEEP, LIGHTS[s], cut=0.3 if s in SIDES else 0)
            return
        p.shaded(f, (255, 0, 255))                       # unpainted: shows up loud
    return paint_box


def build():
    from lion_camel import extent
    slots = None
    for name, berserk in (('xiaotian_quan', False), ('xiaotian_quan_berserk', True)):
        p = HostPainter(7)
        slots = paint(HOUND, p, painter(berserk))
        p.save_as(name)
    emit(HOUND, slots, JAVA)
    print('xiaotian_quan', len(HOUND), 'parts,', len(slots), 'boxes;', {kk: round(v, 2) for kk, v in extent(HOUND, 1.0).items()})


if __name__ == '__main__':
    build()
