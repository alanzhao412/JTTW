"""Flower-Fruit Mountain's creatures (花果山). As wild_paint.py says: each one's model is written into
FlowerFruitRenderers between its GENERATED markers (``// GENERATED <kind>: begin`` and ``end``), its
skin (textures/entity/<name>.png) painted to the same box-UV layout at twice the model's resolution,
each face where the game looks for it (king_yama.paint), its eyes glowing (<name>_glow.png); the three
kinds of the monkey army have a captain's skin too, laced and banded in violet (<name>_captain.png).

The monkey army, the Six-Eared Macaque's gathering in the Monkey King's old kingdom:
  the Stone-Throwing Gibbon (投石长臂猿): lean and long-armed, its knuckles near the ground, black-brown
    fur, a pale ring of fur round its dark face, a blue loincloth, a sling-bag of stones across it, a
    stone in its fist; no tail, as a gibbon has none;
  the Spear Ape (持枪猿兵): a macaque soldier, golden-brown fur, a red face and red rump, a red headband
    with its tails flying, a cuirass of leather lamellae laced in red, wrapped shins, and a long spear,
    steel-headed, a red tassel under the head;
  the Great Baboon (巨狒): a hulk of a baboon, a cape of silver-grey mane over its shoulders and back,
    a long dog's muzzle, red-nosed and full of fangs, a heavy scowling brow, thick arms banded in iron,
    a short arched tail.
The fake pilgrims, his monkeys in the pilgrims' likenesses, each with a monkey's tail showing under
his robe and ears a little too monkeyish:
  the false Tang Sanzang (假唐僧): a monk's grey robe under a red kasaya worked in gold, the five-leaved
    Vairocana crown (毗卢帽), a string of prayer beads, and a ringed monk's staff (锡杖);
  the false Zhu Bajie (假八戒): pot-bellied and pig-headed, a long snout, great flapping ears, tusks, a
    black monk's robe open over his belly, and the nine-tooth rake (九齿钉耙);
  the false Sha Wujing (假沙僧): tall and gaunt, blue-grey, wild red hair and beard, the necklace of
    nine skulls, an ochre robe, and the crescent-moon spade (月牙铲).
And the Six-Eared Macaque (六耳猕猴), the Monkey King's mirror to the last hair: the phoenix-winged
purple-gold cap (凤翅紫金冠) with its two long pheasant plumes, the gold fillet, golden chain mail
(锁子黄金甲) and shoulder plates, a jade belt, a tiger-skin kilt over red trousers, cloud-walking
boots, a red cloak streaming behind; dark umber fur, a heart-shaped face of tawny red, eyes burning
red-gold; three ears a side; his iron staff banded in gold at both ends.

Run from the repository root: python assets_source/flower_fruit.py"""
import math

import beastgen as bg
import wild_paint as w
from wild_paint import part, mirrored, aim, paint, Painter, k, mix
from beastgen import emit

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/FlowerFruitRenderers.java'
OUT = 'src/main/resources/assets/jttw_engine/textures/entity/'
S = w.SIDES

# Drawn sizes: FlowerFruitRenderers.SIZE_*
SIZES = {'gibbon': 0.92, 'ape': 0.95, 'baboon': 1.1, 'tang': 0.95, 'bajie': 0.95, 'sha': 0.95, 'macaque': 1.0}

GOLD = (232, 184, 62)
GOLD_DEEP = (150, 100, 30)
GOLD_LIGHT = (255, 232, 140)
IRON = (64, 62, 70)
IRON_DARK = (32, 30, 36)
STEEL = (196, 202, 212)
RED = (176, 28, 26)
RED_DEEP = (96, 12, 14)
JADE = (96, 178, 136)
IVORY = (236, 226, 200)
LEATHER = (110, 68, 40)
LEATHER_DEEP = (60, 34, 20)
CLAW = (34, 28, 26)
WOOD = (120, 78, 42)
VIOLET = (150, 72, 206)
VIOLET_DEEP = (78, 30, 116)


# ── The frame ────────────────────────────────────────────────────────────────

def biped(P):
    """A standing frame from its proportions: legs (thigh, shin, foot), hips (`body`), `chest`, arms
    (upper, fore, hand); the head and all the rest the creature adds. Its pivots as MonkeyModel looks
    for them."""
    H = 0
    t, L1, s, L2 = P['thigh'], P['thigh_len'], P['shin'], P['shin_len']
    fw, fl, fh = P['foot']
    pw, ph, pd = P['pelvis']
    cw, ch, cd = P['chest']
    aw, A1, fa, A2, hw, hl = P['arm']
    lean = P.get('lean', 0.0)
    out = [
        part('right_leg', 'root', (-P['hipw'], H, 0.2), P.get('legr', (-0.25, 0, 0.04)), [(-t / 2, -0.5, -t / 2, t, L1 + 0.5, t, P.get('thigh_style', 'thigh'))]),
        part('right_shin', 'right_leg', (0, L1, 0), P.get('shinr', (0.42, 0, 0)), [(-s / 2, 0, -s / 2, s, L2, s, P.get('shin_style', 'shin'))]),
        part('right_foot', 'right_shin', (0, L2, 0), P.get('footr', (-0.17, 0, 0)), [(-fw / 2, 0, -fl + s / 2 + 0.3, fw, fh, fl, P.get('foot_style', 'foot'))]),
        part('body', 'root', (0, H, 0.3), (lean, 0, 0), [(-pw / 2, -ph, -pd / 2, pw, ph + 0.6, pd, P.get('pelvis_style', 'pelvis'))]),
        part('chest', 'body', (0, -ph + 0.4, 0), (P.get('chest_lean', 0.0), 0, 0), [(-cw / 2, -ch, -cd / 2, cw, ch, cd, P.get('chest_style', 'chest'))]),
        part('right_arm', 'chest', (-cw / 2 - aw * 0.35, -ch + aw * 0.55, 0), P.get('armr', (-0.12, 0, 0.12)),
             [(-aw / 2, -aw * 0.45, -aw / 2, aw, A1 + aw * 0.45, aw, P.get('upperarm_style', 'upperarm'))]),
        part('right_forearm', 'right_arm', (0, A1, 0), P.get('forer', (-0.35, 0, 0)), [(-fa / 2, 0, -fa / 2, fa, A2, fa, P.get('forearm_style', 'forearm'))]),
        part('right_hand', 'right_forearm', (0, A2, 0), (0, 0, 0), [(-hw / 2, 0, -hw / 2, hw, hl, hw, P.get('hand_style', 'hand'))]),
    ]
    # fingers curled round a grip, a thumb; long toes and a thumb-like great toe on the foot
    fs = P.get('finger_style', 'finger')
    fwid = hw / 4.2
    out.append(part('right_fingers', 'right_hand', (0, hl - 0.2, 0), (0, 0, 0),
                    [(-hw / 2 + 0.05 + i * (hw - 0.1) / 4, 0, -hw / 2 - 0.15, fwid, hl * 0.62 - (0.25 if i in (0, 3) else 0), hw * 0.55, fs) for i in range(4)]))
    out.append(part('right_thumb', 'right_hand', (hw / 2 - 0.2, hl * 0.35, -hw / 2 + 0.2), (0.25, 0, -0.35), [(-0.2, 0, -0.5, fwid * 1.1, hl * 0.55, fwid * 1.1, fs)]))
    tw = fw / 4.6
    out.append(part('right_toes', 'right_foot', (0, 0, -fl + s / 2 + 0.3), (0, 0, 0),
                    [(-fw / 2 + 0.1 + i * (fw - 0.2) / 4, fh - fh * 0.75, -1.3 - (0.3 if i in (1, 2) else 0), tw, fh * 0.75, 1.5 + (0.3 if i in (1, 2) else 0), P.get('toe_style', 'toe'))
                     for i in range(4)]))
    if P.get('tufts', True):
        ts = P.get('tuft_style', 'tuft')
        out.append(part('right_elbowtuft', 'right_arm', (0, A1 - 0.6, aw * 0.45), (0.7, 0, 0), [(-aw * 0.4, -0.4, -0.3, aw * 0.8, aw * 0.9, 1.6, ts)]))
        out.append(part('right_forearmtuft', 'right_forearm', (-fa / 2, A2 * 0.35, 0), (0, 0, 0.55), [(-1.2, -0.4, -fa * 0.4, 1.4, A2 * 0.45, fa * 0.8, ts)]))
        out.append(part('right_calftuft', 'right_shin', (0, s * 0.4, s / 2), (0.45, 0, 0), [(-s * 0.4, -0.2, -0.4, s * 0.8, L2 * 0.45, 1.4, ts)]))
    return out


def tail(parent, at, segs, r0, r1, length, curl, style='tail', start=(0.9, 0, 0)):
    """A tail of `segs` segments from `at` on `parent`, tapering r0 -> r1, curling `curl` a segment."""
    out = []
    prev = parent
    for i in range(segs):
        r = r0 + (r1 - r0) * i / max(1, segs - 1)
        out.append(part('tail_%d' % i, prev, at if i == 0 else (0, 0, length), start if i == 0 else (curl, 0, 0),
                        [(-r / 2, -r / 2, 0, r, r, length + 0.3, style)]))
        prev = 'tail_%d' % i
    return out


def monkey_head(parent, at, rot, M):
    """A monkey's head: a round skull, a flat face set deep in it under a heavy ridged brow, round
    cheek pads, a muzzle (short for a macaque, a dog's long one for a baboon) with its upper lip and
    nose, a jaw that opens, ruffs of fur at the cheeks, locks of fur over the crown, round ears
    (`ears` a side, stacked); its parts as M says, each style prefixed with M['p']."""
    W, Hh, D = M['w'], M['h'], M['d']
    pfx = M['p']
    mw, mh, ml = M['muzzle']
    out = [
        part('head', parent, at, rot, [
            (-W, -Hh, -D, 2 * W, Hh, 2 * D, pfx + 'skull'),
            (-W * 0.84, -Hh * 0.86, -D - 0.55, 2 * W * 0.84, Hh * 0.74, 0.8, pfx + 'face'),
            (-mw, -Hh * 0.42, -D - 0.6 - ml, 2 * mw, mh, ml + 0.2, pfx + 'muzzle'),
            (-mw * 1.04, -Hh * 0.42 + mh - 0.75, -D - 0.75 - ml, 2 * mw * 1.04, 0.8, ml + 0.3, pfx + 'lip'),
            (-mw * 0.6, -Hh * 0.42 - 0.4, -D - 0.85 - ml, 2 * mw * 0.6, mh * 0.5, 0.55, pfx + 'nose'),
            (-W * 1.03, -Hh * 1.04, -D * 0.35, 2 * W * 1.03, Hh * 0.66, D * 1.4, pfx + 'crown'),
            (-W * 0.95, -Hh * 0.2, -D * 0.6, 2 * W * 0.95, Hh * 0.28, D * 1.5, pfx + 'nape'),
        ]),
        # the brow: two heavy ridges meeting over the nose, angled down to it in a scowl
        part('right_brow', 'head', (0, -Hh * 0.84, -D - 0.75), (0, 0.18, 0.2), [(-W * 0.95, -0.6, -0.65, W * 0.98, 1.25, 1.3, pfx + 'brow')]),
        part('right_cheekpad', 'head', (-W * 0.5, -Hh * 0.38, -D - 0.45), (0, 0.3, 0), [(-1.3, -1.2, -0.7, 2.4, 2.2, 1.2, pfx + 'cheek')]),
        part('jaw', 'head', (0, -Hh * 0.42 + mh - 0.2, -D - 0.2), (0.06, 0, 0), [
            (-mw * 0.86, 0, -ml * 0.88 - 0.4, 2 * mw * 0.86, M.get('jaw_h', 1.4), ml * 0.88 + 0.6, pfx + 'jaw'),
            (-mw * 0.8, M.get('jaw_h', 1.4) - 0.2, -ml * 0.6, 2 * mw * 0.8, 1.0, ml * 0.6 + 0.6, pfx + 'chin')]),
        part('right_cheek', 'head', (-W * 0.9, -Hh * 0.42, -D * 0.4), (0.1, 0.55, 0.3), [
            (-1.8, -1.8, -1.4, 2.2, 4.2 * M.get('ruff', 1.0), 3.6, pfx + 'cheekfur')]),
        part('right_cheek_low', 'head', (-W * 0.75, -Hh * 0.12, -D * 0.25), (0.35, 0.35, 0.55), [
            (-1.5, -0.6, -1.2, 1.9, 3.2 * M.get('ruff', 1.0), 3.0, pfx + 'cheekfur')]),
    ]
    # locks of fur over the crown, swept back
    for i, (x, z, r) in enumerate(((0, -D * 0.5, 0.0), (-W * 0.5, -D * 0.1, 0.3), (W * 0.5, -D * 0.1, -0.3), (0, D * 0.35, 0.0))):
        out.append(part('crownlock_%d' % i, 'head', (x, -Hh * 1.0, z), (-0.75, r, r * 0.6), [(-1.1, -0.5, -0.4, 2.2, 1.0, 3.2 + 0.6 * (i % 2), pfx + 'lock')]))
    n = M.get('ears', 1)
    for e in range(n):
        y = -Hh * 0.6 + (e - (n - 1) / 2) * 2.15
        z = -D * 0.15 + abs(e - (n - 1) / 2) * 0.5
        out.append(part('right_ear_%d' % e if n > 1 else 'right_ear', 'head', (-W - 0.1, y, z), (0, -0.35, 0.18 - 0.08 * e), [
            (-0.9, -1.2 * M.get('ear', 1.0), -1.0 * M.get('ear', 1.0), 1.0, 2.4 * M.get('ear', 1.0), 2.1 * M.get('ear', 1.0), pfx + 'ear')]))
    return out


# ── Brushes ──────────────────────────────────────────────────────────────────

def pelt(p, f, base, dark, tip=None, under=None, faces=None, density=0.45):
    w.fur(p, f, base, dark, tip=tip, under=under, faces=faces, density=density)


def tufted(p, f, base, dark, tip=None, depth=3, every=3, faces=('front', 'back', 'right', 'left')):
    pelt(p, f, base, dark, tip)
    for s in faces:
        w.tufts(p, f[s], depth=depth, every=every)


def skin(p, f, base, shade, faces=None, mottle=0.3):
    w.hide(p, f, base, shade, mottle=mottle, faces=faces)


def lamellae(p, f, plate, lace, rows=3, cols=2, faces=S, rim=None):
    for s in faces:
        w.lamellar(p, f[s], k(plate, w.LIGHT[s]), lace, rows=rows, cols=cols, rim=rim)


def chainmail(p, f, base, deep, faces=S):
    """Golden chain mail: rings in offset rows, each lit at its top and dark at its centre."""
    for s in faces:
        fx, fy, fw, fh = f[s]
        lv = w.LIGHT[s]
        for j in range(fh):
            for i in range(fw):
                ii = (i + (j // 2) % 2) % 2
                jj = j % 2
                c = base if (ii == 0 and jj == 0) else mix(base, deep, 0.45) if ii != jj else deep
                p.px((fx + i, fy + j), [v * lv * (1 - 0.15 * j / max(1, fh)) + p.rnd.randint(-5, 5) for v in c])


def trim(p, f, colour, faces=S, top=True, bottom=True, width=1):
    w.border(p, f, colour, sides=faces, top=top, bottom=bottom, width=width)


def tiger(p, f, faces=S):
    """Tiger skin: orange, black stripes tapering down it, white at its hem."""
    for s in faces:
        fx, fy, fw, fh = f[s]
        lv = w.LIGHT[s]
        for i, j, xy in w.each(p, f[s]):
            c = (214, 128, 34) if j < fh - 2 else (230, 222, 200)
            p.px(xy, [v * lv + p.rnd.randint(-6, 6) for v in c])
        x = p.rnd.randint(0, 3)
        while x < fw:
            length = p.rnd.randint(max(2, fh // 2), max(3, fh - 2))
            for j in range(length):
                thick = 2 if j < length * 0.6 else 1
                for t in range(thick):
                    xx = fx + x + t + int(math.sin(j * 0.6 + x) * 1.2)
                    if fx <= xx < fx + fw:
                        p.px((xx, fy + j), (26, 18, 14))
            x += p.rnd.randint(4, 6)


def monkey_face(p, rect, skin_c, shade, eye_c, ring=None, eye_w=3, eye_y=None, brow_c=None, heart=True, glow=1.0, lid=None):
    """A monkey's flat face: a heart-shaped mask of bare skin, darker round deep-set eyes that glow, the
    fur of the skull round it (`ring`, a pale ring of fur for the gibbon); the eyes high, close together."""
    fx, fy, fw, fh = rect
    cx = (fw - 1) / 2
    for i, j, xy in w.each(p, rect):
        dx = (i - cx) / (fw / 2)
        dy = j / max(1, fh - 1)
        # the heart: two lobes over the eyes, narrowing to the muzzle
        edge = 1.0 - 0.25 * dy if heart else 1.0
        lobe = abs(abs(dx) - 0.45) < 0.5 or dy > 0.25
        inside = abs(dx) < edge and lobe and not (dy < 0.12 and abs(dx) < 0.12 and heart)
        c = mix(skin_c, shade, w.clamp(abs(dx) * 0.6 + w.vnoise(fx + i, fy + j, 3, 9) * 0.3)) if inside else (ring or k(shade, 0.7))
        p.px(xy, [v + p.rnd.randint(-4, 4) for v in c])
    ey = eye_y if eye_y is not None else max(1, int(fh * 0.32))
    gap = max(1, fw // 10)
    left_x = int(cx - gap - eye_w + 1)
    right_x = int(cx + gap + 1)
    for x in (left_x, right_x):
        w.eye(p, rect, x, ey, eye_w, 2, eye_c, lid=lid or k(shade, 0.6), socket=k(shade, 0.45), glow=glow)
    # wrinkles under the eyes and at the brow
    for x in range(left_x - 1, right_x + eye_w + 1):
        if 0 <= x < fw and (x + ey) % 2 == 0:
            p.px((fx + x, fy + ey + 3), k(shade, 0.8))


def muzzle(p, f, skin_c, shade, nostril=(30, 14, 12), lips=None, teeth=None, fangs=False):
    skin(p, f, skin_c, shade)
    fx, fy, fw, fh = f['front']
    m = fw // 2
    for x in (m - 2, m + 1):
        if 0 <= x < fw:
            p.px((fx + x, fy + 1), nostril)
    for i in range(fw):                                            # the mouth line
        p.px((fx + i, fy + fh - 1), lips or k(shade, 0.5))
    if teeth:
        w.teeth_row(p, f['front'], fh - 2, teeth, k(shade, 0.4), every=2, rows=1, x0=1, x1=fw - 1)
    if fangs:
        for x in (1, fw - 2):
            p.px((fx + x, fy + fh - 1), IVORY)


def eye_glow(p, rect, xs, y, wdt, c):
    for x in xs:
        for i in range(wdt):
            p.lit((rect[0] + x + i, rect[1] + y), c, 1.0)


def staff_iron(p, f):
    w.iron(p, f, IRON_DARK, dents=0.05, rim=False)


def gold(p, f, faces=None):
    w.iron(p, f, GOLD, dents=0.08, rim=True, faces=faces)


def cloth(p, f, base, deep=None, fold=4, faces=None, fall=0.3):
    w.cloth(p, f, base, deep or k(base, 0.55), fold=fold, faces=faces, fall=fall, coarse=False)


def common(p, style, f, pfx, fur, dark, tip, face, shade, hand, nail=CLAW):
    """What every frame shares, in this creature's colours: fingers and toes, fur tufts, the lip,
    chin, cheek pads, nape and the locks over the crown."""
    if style in ('finger', 'toe'):
        skin(p, f, hand, k(hand, 0.55))
        for s_ in ('front', 'bottom'):
            fx, fy, fw, fh = f[s_]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), nail)
        for s_ in ('right', 'left'):                              # the knuckle's crease
            fx, fy, fw, fh = f[s_]
            for i in range(fw):
                p.px((fx + i, fy + fh // 2), k(hand, 0.6))
    if style == 'tuft':
        tufted(p, f, fur, dark, tip, depth=3, every=2)
    if style == pfx + 'lip':
        skin(p, f, face, shade)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), k(shade, 0.35))
        for s_ in ('right', 'left'):
            fx, fy, fw, fh = f[s_]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), k(shade, 0.45))
    if style in (pfx + 'chin', pfx + 'cheek'):
        skin(p, f, face, shade)
        if style == pfx + 'cheek':
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                if i % 2 == 0:
                    p.px((fx + i, fy + fh - 1), k(shade, 0.8))   # a crease under the cheek
    if style == pfx + 'nape':
        pelt(p, f, fur, dark, tip)
    if style == pfx + 'lock':
        w.fur(p, f, fur, dark, tip=tip, lean=0.2)
        for s_ in ('right', 'left', 'top', 'bottom'):
            w.point(p, f[s_], down=True, keep=0.4, sharp=0.5)


# ── The Stone-Throwing Gibbon ────────────────────────────────────────────────

GIB_FUR, GIB_DARK, GIB_TIP = (58, 46, 40), (28, 22, 20), (104, 88, 74)
GIB_RING = (214, 206, 186)
GIB_FACE, GIB_SHADE = (52, 40, 40), (24, 18, 18)
GIB_EYE = (238, 170, 60)
LOIN = (54, 72, 118)

GIBBON = biped(dict(hipw=1.7, thigh=2.8, thigh_len=4.8, shin=2.4, shin_len=4.6, foot=(2.6, 4.6, 1.3),
                    pelvis=(5.0, 3.0, 3.8), chest=(6.2, 7.0, 4.2), arm=(2.2, 7.8, 2.0, 7.4, 2.4, 3.2),
                    lean=0.22, chest_lean=0.1, legr=(-0.5, 0, 0.06), shinr=(0.85, 0, 0), footr=(-0.35, 0, 0),
                    armr=(-0.18, 0, 0.1), forer=(-0.25, 0, 0)))
GIBBON = mirrored(GIBBON + monkey_head('chest', (0, -7.0, -0.8), (-0.2, 0, 0),
                                       dict(w=3.3, h=5.9, d=3.1, p='g_', muzzle=(1.5, 1.7, 1.1), ears=1, ear=0.85, ruff=1.3)) + [
    part('right_shoulderfur', 'chest', (-3.0, -6.6, 0), (0, 0, 0.3), [(-1.4, -0.6, -2.2, 2.6, 2.6, 4.4, 'g_ruff')]),
    part('loincloth', 'body', (0, -0.6, -2.0), (-0.05, 0, 0), [(-2.2, 0, -0.3, 4.4, 4.6, 0.6, 'g_loin')]),
    part('sway_loin_back', 'body', (0, -0.6, 2.0), (0.1, 0, 0), [(-2.2, 0, -0.3, 4.4, 3.6, 0.6, 'g_loin')]),
    part('loinbelt', 'body', (0, -1.6, 0), (0, 0, 0), [(-2.7, -0.4, -2.1, 5.4, 1.2, 4.2, 'g_belt')]),
    # the sling-bag of stones across it, its strap over the left shoulder
    part('strap', 'chest', (0, -3.6, 0), (0, 0, 0.62), [(-0.5, -4.6, -2.3, 1.0, 9.2, 4.6, 'g_strap')]),
    part('bag', 'body', (-2.6, -1.4, 0.6), (0, 0, 0.12), [(-1.3, 0, -1.4, 2.6, 2.8, 2.8, 'g_bag'),
                                                           (-1.0, -0.8, -1.0, 0.9, 0.9, 0.9, 'g_pebble'),
                                                           (0.1, -0.7, -0.4, 0.9, 0.8, 0.9, 'g_pebble')]),
])
GIBBON += [part('stone', 'right_hand', (0, 2.6, -0.2), (0, 0, 0), [(-1.2, -0.6, -1.2, 2.4, 2.2, 2.4, 'g_stone')])]
GIBBON = w.settle(GIBBON, ('right_foot', 'left_foot'))


def paint_gibbon(p, style, f, dims, captain=False):
    common(p, style, f, 'g_', GIB_FUR, GIB_DARK, GIB_TIP, GIB_FACE, GIB_SHADE, (40, 32, 30))
    if style in ('thigh', 'shin', 'upperarm', 'forearm', 'chest', 'pelvis', 'g_skull', 'g_crown'):
        pelt(p, f, GIB_FUR, GIB_DARK, GIB_TIP)
        if style == 'chest':
            fx, fy, fw, fh = f['front']                           # a paler throat and chest
            for i, j, xy in w.each(p, (fx + 2, fy, fw - 4, fh // 2)):
                c = p.get(xy)
                p.px(xy, mix(c[:3], (120, 104, 90), 0.4))
    if style in ('foot', 'hand'):
        skin(p, f, (40, 32, 30), (20, 16, 16))
        fx, fy, fw, fh = f['front']
        for x in range(0, fw, 2):                                 # its long fingers
            for j in range(fh // 2, fh):
                p.px((fx + x, fy + j), (18, 14, 14))
    if style == 'g_ruff':
        tufted(p, f, k(GIB_FUR, 1.15), GIB_DARK, GIB_TIP, depth=3)
    if style == 'g_face':
        w.flat(p, f, GIB_RING)
        monkey_face(p, f['front'], GIB_FACE, GIB_SHADE, GIB_EYE, ring=GIB_RING, eye_w=2, heart=False)
    if style == 'g_brow':
        pelt(p, f, GIB_RING, k(GIB_RING, 0.7))
    if style == 'g_cheekfur':
        tufted(p, f, GIB_RING, k(GIB_RING, 0.72), depth=2, every=2)
    if style == 'g_muzzle':
        muzzle(p, f, GIB_FACE, GIB_SHADE, lips=(14, 10, 10))
    if style == 'g_nose':
        skin(p, f, (30, 22, 22), (14, 10, 10))
    if style == 'g_jaw':
        skin(p, f, GIB_FACE, GIB_SHADE)
        w.flat(p, f, k(GIB_RING, 0.9), faces=('bottom',))
    if style == 'g_ear':
        skin(p, f, (70, 50, 46), (34, 24, 22))
    if style == 'g_loin':
        cloth(p, f, VIOLET_DEEP if captain else LOIN)
        trim(p, f, VIOLET if captain else (190, 160, 70), top=False)
        for s in ('front', 'back'):
            w.ragged(p, f[s], teeth=2, depth=2)
    if style == 'g_belt':
        w.rope(p, f, (150, 120, 70))
    if style == 'g_strap':
        w.leather(p, f, LEATHER, LEATHER_DEEP)
    if style == 'g_bag':
        w.leather(p, f, (130, 96, 60), LEATHER_DEEP, stitch=(70, 46, 26))
    if style in ('g_pebble', 'g_stone'):
        w.hide(p, f, (128, 124, 116), (80, 78, 74), mottle=0.6)


# ── The Spear Ape ────────────────────────────────────────────────────────────

APE_FUR, APE_DARK, APE_TIP = (150, 104, 54), (82, 54, 28), (206, 160, 96)
APE_FACE, APE_SHADE = (206, 92, 78), (130, 46, 40)
APE_EYE = (250, 196, 70)
CUIRASS = (122, 76, 42)
LACE = (176, 30, 28)

APE = biped(dict(hipw=1.9, thigh=3.0, thigh_len=5.2, shin=2.6, shin_len=5.2, foot=(2.8, 4.6, 1.3),
                 pelvis=(5.6, 3.2, 4.0), chest=(7.0, 8.0, 4.8), arm=(2.5, 6.4, 2.3, 6.2, 2.5, 2.8),
                 lean=0.16, chest_lean=0.08, legr=(-0.35, 0, 0.05), shinr=(0.6, 0, 0), footr=(-0.25, 0, 0),
                 armr=(-0.2, 0, 0.3), forer=(-0.45, 0, 0)))
APE = mirrored(APE + monkey_head('chest', (0, -8.0, -1.0), (-0.12, 0, 0),
                                 dict(w=3.5, h=6.1, d=3.3, p='a_', muzzle=(1.9, 2.1, 1.7), ears=1, ear=0.95)) + [
    # the cuirass of leather lamellae, its shoulder guards, the red headband and its flying tails
    part('cuirass', 'chest', (0, 0, 0), (0, 0, 0), [(-3.9, -7.6, -2.8, 7.8, 6.4, 5.6, 'a_cuirass'),
                                                    (-3.6, -1.6, -2.7, 7.2, 1.6, 5.4, 'a_cuirass_low')]),
    part('right_pauldron', 'right_arm', (-0.6, -0.8, 0), (0, 0, 0.28), [(-1.8, -1.2, -1.8, 3.2, 2.4, 3.6, 'a_pauldron')]),
    part('headband', 'head', (0, -4.1, 0), (0, 0, 0), [(-3.35, -0.6, -3.2, 6.7, 1.3, 6.4, 'a_band')]),
    part('sway_bandtail', 'headband', (0.6, 0, 3.1), (0.5, 0.15, 0), [(-0.5, 0, -0.15, 1.0, 5.0, 0.3, 'a_bandtail')]),
    part('sway_bandtail2', 'headband', (-0.6, 0, 3.1), (0.75, -0.2, 0), [(-0.45, 0, -0.15, 0.9, 4.2, 0.3, 'a_bandtail')]),
    part('right_tasset', 'body', (-1.4, -0.4, -2.1), (-0.08, 0, 0.05), [(-1.4, 0, -0.4, 2.8, 4.6, 0.8, 'a_tasset')]),
    part('sway_back_tasset', 'body', (0, -0.4, 2.1), (0.1, 0, 0), [(-2.6, 0, -0.4, 5.2, 4.2, 0.8, 'a_tasset')]),
    part('right_wrap', 'right_shin', (0, 0.6, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3.0, 3.6, 3.0, 'a_wrap')]),
])
APE += tail('body', (0, -1.6, 1.9), 4, 1.5, 0.9, 3.0, 0.32, 'a_tail', start=(1.15, 0, 0))
SPEAR_AIM = aim([(0.16, 0, 0), (0.08, 0, 0), (-0.2, 0, 0.3), (-0.45, 0, 0)], (0.02, 0, 0))
APE += [
    part('spear', 'right_hand', (0, 1.6, 0), SPEAR_AIM, [
        (-0.45, -26.0, -0.45, 0.9, 32.0, 0.9, 'a_shaft'),
        (-0.6, -26.8, -0.6, 1.2, 1.4, 1.2, 'a_socket'),
        (-0.25, -32.6, -0.85, 0.5, 6.0, 1.7, 'a_blade'),
        (-0.55, 5.6, -0.55, 1.1, 1.4, 1.1, 'a_butt')]),
    part('sway_tassel', 'spear', (0, -25.6, 0), (0.05, 0, 0), [(-0.7, 0, -0.7, 1.4, 3.2, 1.4, 'a_tassel')]),
]
APE = w.settle(APE, ('right_foot', 'left_foot'))


def paint_ape(p, style, f, dims, captain=False):
    common(p, style, f, 'a_', APE_FUR, APE_DARK, APE_TIP, APE_FACE, APE_SHADE, (150, 90, 70))
    lace = VIOLET if captain else LACE
    if style in ('thigh', 'upperarm', 'forearm', 'chest', 'pelvis', 'a_skull', 'a_crown', 'shin'):
        pelt(p, f, APE_FUR, APE_DARK, APE_TIP)
    if style in ('foot', 'hand'):
        skin(p, f, (150, 90, 70), (90, 50, 40))
        fx, fy, fw, fh = f['front']
        for x in range(0, fw, 2):
            for j in range(fh // 2, fh):
                p.px((fx + x, fy + j), (70, 36, 28))
    if style == 'a_face':
        w.flat(p, f, APE_FUR)
        monkey_face(p, f['front'], APE_FACE, APE_SHADE, APE_EYE, ring=k(APE_FUR, 0.9), eye_w=2)
    if style == 'a_brow':
        pelt(p, f, k(APE_FUR, 0.9), APE_DARK)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), k(APE_SHADE, 0.7))
    if style == 'a_cheekfur':
        tufted(p, f, APE_TIP, APE_FUR, depth=2, every=2)
    if style == 'a_muzzle':
        muzzle(p, f, APE_FACE, APE_SHADE, teeth=IVORY)
    if style == 'a_nose':
        skin(p, f, k(APE_FACE, 0.85), APE_SHADE)
    if style == 'a_jaw':
        skin(p, f, APE_FACE, APE_SHADE)
    if style == 'a_ear':
        skin(p, f, (186, 106, 86), (110, 56, 44))
    if style in ('a_cuirass', 'a_cuirass_low'):
        lamellae(p, f, CUIRASS, lace, rows=4 if style == 'a_cuirass' else 1, cols=3, rim=GOLD_DEEP)
        w.leather(p, f, CUIRASS, LEATHER_DEEP, faces=('top', 'bottom'))
        if style == 'a_cuirass':
            fx, fy, fw, fh = f['front']
            w.disc(p, fx + fw / 2, fy + fh * 0.4, 2.8, GOLD, rim=GOLD_DEEP)          # a bronze mirror on the breast
            w.disc(p, fx + fw / 2, fy + fh * 0.4, 1.2, GOLD_LIGHT)
    if style == 'a_pauldron':
        lamellae(p, f, CUIRASS, lace, rows=2, cols=2, rim=GOLD_DEEP)
        w.leather(p, f, CUIRASS, faces=('top',))
    if style == 'a_tasset':
        lamellae(p, f, k(CUIRASS, 0.9), lace, rows=3, cols=2)
    if style in ('a_band', 'a_bandtail'):
        cloth(p, f, VIOLET if captain else RED, fold=3)
    if style == 'a_wrap':
        cloth(p, f, (196, 180, 150), fold=2)
        for s in S:
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 2):
                for i in range(fw):
                    p.px((fx + i, fy + j), (150, 134, 104))
    if style == 'a_tail':
        pelt(p, f, APE_FUR, APE_DARK, APE_TIP)
    if style == 'a_shaft':
        w.wood(p, f, (110, 30, 24), along='y')
    if style in ('a_socket', 'a_butt'):
        w.iron(p, f, IRON, dents=0.1)
    if style == 'a_blade':
        w.iron(p, f, STEEL, dents=0.02, rim=False)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            w.point(p, f[s], down=False, keep=0.4, sharp=0.6)
            for j in range(fh):
                p.px((fx + fw // 2, fy + j), k(STEEL, 0.8))
    if style == 'a_tassel':
        pelt(p, f, (196, 30, 26), (110, 12, 12), (236, 70, 50))
        for s in S:
            w.tufts(p, f[s], depth=2, every=2)


# ── The Great Baboon ─────────────────────────────────────────────────────────

BAB_FUR, BAB_DARK, BAB_TIP = (124, 116, 92), (66, 60, 46), (176, 168, 140)
BAB_MANE, BAB_MANE_DARK, BAB_MANE_TIP = (172, 170, 160), (96, 94, 88), (224, 222, 212)
BAB_FACE, BAB_SHADE = (70, 54, 52), (32, 24, 24)
BAB_NOSE = (176, 54, 46)
BAB_EYE = (240, 140, 40)

BABOON = biped(dict(hipw=2.6, thigh=4.2, thigh_len=5.6, shin=3.4, shin_len=5.2, foot=(3.8, 5.4, 1.6),
                    pelvis=(8.0, 4.0, 6.0), chest=(11.0, 10.0, 7.6), arm=(4.0, 8.4, 3.8, 8.6, 4.2, 3.2),
                    lean=0.62, chest_lean=0.28, legr=(-0.95, 0, 0.08), shinr=(1.05, 0, 0), footr=(-0.15, 0, 0),
                    armr=(-0.75, 0, 0.16), forer=(-0.2, 0, 0)))
BABOON = mirrored(BABOON + monkey_head('chest', (0, -9.0, -3.2), (-0.75, 0, 0),
                                       dict(w=4.0, h=6.0, d=3.7, p='b_', muzzle=(2.4, 3.0, 5.8), ears=1, ear=0.75, jaw_h=2.2, ruff=1.5)) + [
    # its cape of mane over the shoulders and down the back, a crest of it over the skull
    part('mane', 'chest', (0, -10.0, 0.6), (0.35, 0, 0), [(-6.4, -1.0, -3.6, 12.8, 9.0, 8.6, 'b_mane')]),
    part('sway_mane_back', 'chest', (0, -9.6, 4.4), (0.25, 0, 0), [(-5.6, 0, -0.6, 11.2, 11.0, 1.8, 'b_manecape')]),
    part('right_manefront', 'chest', (-3.2, -9.6, -3.6), (-0.2, 0.15, 0.1), [(-2.6, 0, -0.6, 5.2, 7.5, 1.4, 'b_manecape')]),
    part('crest', 'head', (0, -5.3, 0.6), (0.2, 0, 0), [(-2.2, -1.2, -2.4, 4.4, 2.0, 5.6, 'b_crestfur')]),
    part('right_band', 'right_forearm', (0, 2.6, 0), (0, 0, 0), [(-2.2, 0, -2.2, 4.4, 1.6, 4.4, 'b_ironband'),
                                                                 (-2.2, 3.0, -2.2, 4.4, 1.6, 4.4, 'b_ironband')]),
    part('right_fang', 'jaw', (-1.3, -0.4, -5.2), (0, 0, 0), [(-0.35, -1.6, -0.35, 0.7, 1.8, 0.7, 'b_fang')]),
    part('right_upfang', 'head', (-1.5, -5.4 * 0.42 + 2.6, -3.4 - 0.6 - 4.6), (0, 0, 0), [(-0.35, 0, -0.35, 0.7, 1.7, 0.7, 'b_fang')]),
    part('loin', 'body', (0, -1.0, -3.1), (-0.12, 0, 0), [(-2.8, 0, -0.4, 5.6, 4.8, 0.8, 'b_loin')]),
    part('belt', 'body', (0, -2.4, 0), (0, 0, 0), [(-4.2, -0.6, -3.2, 8.4, 1.6, 6.4, 'b_belt')]),
])
BABOON += tail('body', (0, -2.6, 2.9), 3, 2.2, 1.4, 2.6, -0.5, 'b_tail', start=(0.25, 0, 0))
BABOON = w.settle(BABOON, ('right_foot', 'left_foot', 'right_hand', 'left_hand'))


def paint_baboon(p, style, f, dims, captain=False):
    common(p, style, f, 'b_', BAB_FUR, BAB_DARK, BAB_TIP, BAB_FACE, BAB_SHADE, (58, 46, 44))
    if style in ('thigh', 'shin', 'upperarm', 'forearm', 'chest', 'pelvis', 'b_skull', 'b_crown', 'b_tail'):
        pelt(p, f, BAB_FUR, BAB_DARK, BAB_TIP)
    if style in ('foot', 'hand'):
        skin(p, f, (58, 46, 44), (28, 22, 22))
        fx, fy, fw, fh = f['front']
        for x in range(0, fw, 2):
            for j in range(fh // 3, fh):
                p.px((fx + x, fy + j), (20, 16, 16))
    if style in ('b_mane', 'b_manecape', 'b_crestfur'):
        tufted(p, f, BAB_MANE, BAB_MANE_DARK, BAB_MANE_TIP, depth=4 if style != 'b_crestfur' else 2, every=3)
    if style == 'b_face':
        w.flat(p, f, BAB_FUR)
        monkey_face(p, f['front'], BAB_FACE, BAB_SHADE, BAB_EYE, ring=k(BAB_FUR, 0.8), eye_w=2, heart=False)
    if style == 'b_brow':
        skin(p, f, k(BAB_FACE, 0.8), BAB_SHADE)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), (14, 10, 10))
    if style == 'b_cheekfur':
        tufted(p, f, BAB_MANE, BAB_MANE_DARK, depth=3, every=2)
    if style == 'b_muzzle':
        skin(p, f, BAB_FACE, BAB_SHADE)
        for s in ('right', 'left'):                               # ridges down the muzzle's sides
            fx, fy, fw, fh = f[s]
            for i in range(0, fw, 3):
                for j in range(fh - 1):
                    p.px((fx + i, fy + j), k(BAB_SHADE, 0.8))
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), (30, 10, 10))
        fx, fy, fw, fh = f['top']
        for i, j, xy in w.each(p, (fx, fy, fw, max(1, fh // 4))):
            p.px(xy, BAB_NOSE)
    if style == 'b_nose':
        skin(p, f, BAB_NOSE, k(BAB_NOSE, 0.6))
        fx, fy, fw, fh = f['front']
        for x in (1, fw - 2):
            p.px((fx + x, fy + fh // 2), (24, 8, 8))
    if style == 'b_jaw':
        skin(p, f, BAB_FACE, BAB_SHADE)
        for s in ('right', 'left', 'front'):
            w.teeth_row(p, f[s], 0, IVORY, (90, 20, 24), every=2, rows=1)
    if style == 'b_fang':
        w.flat(p, f, IVORY)
    if style == 'b_ear':
        skin(p, f, (80, 60, 56), (40, 28, 26))
    if style == 'b_ironband':
        w.iron(p, f, (VIOLET_DEEP if captain else IRON), rivets=2, dents=0.2)
    if style == 'b_loin':
        cloth(p, f, VIOLET_DEEP if captain else RED_DEEP)
        for s in ('front', 'back'):
            w.ragged(p, f[s], teeth=3, depth=3)
    if style == 'b_belt':
        w.leather(p, f, LEATHER_DEEP, stitch=(VIOLET if captain else GOLD_DEEP))


# ── The fake pilgrims ────────────────────────────────────────────────────────

ROBE_GREY = (150, 140, 120)
KASAYA = (170, 34, 28)
MONK_SKIN, MONK_SHADE = (226, 180, 140), (150, 104, 78)
FAKE_FUR, FAKE_FUR_DARK, FAKE_FUR_TIP = (148, 112, 66), (82, 58, 30), (196, 158, 104)
TANG_EYE = (236, 200, 120)


def robe_frame(P, skirt_w, skirt_d, skirt_len, style):
    """A robed frame: biped, the legs under a long robe's skirt hung from the hips in panels."""
    parts = biped(P)
    parts += [
        part('sway_skirt_front', 'body', (0, 0, -skirt_d / 2), (-0.06, 0, 0), [(-skirt_w / 2, -0.5, -0.4, skirt_w, skirt_len, 0.8, style)]),
        part('sway_skirt_back', 'body', (0, 0, skirt_d / 2), (0.08, 0, 0), [(-skirt_w / 2, -0.5, -0.4, skirt_w, skirt_len, 0.8, style)]),
        part('right_skirt_side', 'body', (-skirt_w / 2, 0, 0), (0, 0, 0.1), [(-0.4, -0.5, -skirt_d / 2, 0.8, skirt_len - 0.4, skirt_d, style)]),
    ]
    return parts


TANG = robe_frame(dict(hipw=1.8, thigh=2.9, thigh_len=5.6, shin=2.5, shin_len=5.6, foot=(2.8, 4.4, 1.4),
                       pelvis=(6.0, 3.0, 4.2), chest=(7.2, 8.4, 4.6), arm=(2.6, 6.6, 2.4, 6.4, 2.4, 2.6),
                       lean=0.04, chest_lean=0.02, armr=(-0.1, 0, 0.08), forer=(-0.4, 0, 0),
                       tufts=False, upperarm_style='t_sleeve', forearm_style='t_sleeve2', hand_style='t_hand', foot_style='t_shoe',
                       pelvis_style='t_robe', chest_style='t_robe_chest', thigh_style='t_trouser', shin_style='t_trouser'),
                  7.6, 5.4, 11.2, 't_robe')
TANG = mirrored(TANG + monkey_head('chest', (0, -8.4, -0.6), (0, 0, 0),
                                   dict(w=3.3, h=6.2, d=3.2, p='t_', muzzle=(1.5, 1.6, 0.8), ears=1, ear=1.1)) + [
    # the red kasaya across him from the left shoulder, its gold-barred patches, its gold ring at the breast
    part('kasaya', 'chest', (0, 0, 0), (0, 0, 0), [(-3.9, -8.2, -2.6, 7.8, 9.0, 5.2, 't_kasaya')]),
    part('sway_kasaya_hang', 'chest', (1.2, -0.6, -2.6), (-0.04, 0, 0.04), [(-3.0, 0, -0.4, 6.0, 8.0, 0.8, 't_kasaya')]),
    part('kasaya_ring', 'chest', (1.6, -6.0, -2.9), (0, 0, 0), [(-0.8, -0.8, -0.4, 1.6, 1.6, 0.5, 't_ring')]),
    part('right_cuff', 'right_forearm', (0, 3.4, 0), (0, 0, 0), [(-1.7, 0, -1.7, 3.4, 3.2, 3.4, 't_bigsleeve')]),
    # the Vairocana crown: a gold band, five leaves standing up round it
    part('crown_band', 'head', (0, -5.0, 0), (0, 0, 0), [(-3.25, -0.6, -3.25, 6.5, 1.2, 6.5, 't_crownband')]),
    part('beads', 'chest', (0, -7.8, -2.6), (0.05, 0, 0), [(-2.6, 0, -0.4, 5.2, 5.2, 0.6, 't_beads')]),
])
for i, a in enumerate((-1.0, -0.5, 0.0, 0.5, 1.0)):
    TANG.append(part('crown_leaf_%d' % i, 'crown_band', (3.4 * math.sin(a), -0.4, -3.4 * math.cos(a)), (0.12, -a, 0),
                     [(-1.15, -2.8, -0.2, 2.3, 2.8, 0.4, 't_leaf')]))
TANG += tail('body', (0, -0.4, 2.4), 4, 1.2, 0.8, 2.9, 0.2, 'fake_tail', start=(1.25, 0, 0))
STAFF_AIM = aim([(0.04, 0, 0), (0.02, 0, 0), (-0.1, 0, 0.08), (-0.4, 0, 0)], (0.05, 0, 0))
TANG += [part('staff', 'right_hand', (0, 1.3, 0), STAFF_AIM, [
    (-0.4, -24.0, -0.4, 0.8, 33.0, 0.8, 't_staffshaft'),
    (-0.6, -24.6, -0.6, 1.2, 1.2, 1.2, 't_gold'),
    (-0.25, -30.0, -2.0, 0.5, 5.6, 4.0, 't_staffring'),
    (-0.6, 8.6, -0.6, 1.2, 1.2, 1.2, 't_gold')])]
for i, (dz, dy) in enumerate(((-2.4, -26.2), (2.4, -26.2), (-2.3, -28.2), (2.3, -28.2), (-1.7, -30.0), (1.7, -30.0))):
    TANG.append(part('sway_staffring_%d' % i, 'staff', (0, dy, dz), (0, 0, 0), [(-0.3, 0, -0.55, 0.6, 1.4, 1.1, 't_littlering')]))
TANG = w.settle(TANG, ('right_foot', 'left_foot'))


def fake_tell(p, f):
    """A monkey's fur showing where the likeness wears thin."""
    pelt(p, f, FAKE_FUR, FAKE_FUR_DARK, FAKE_FUR_TIP)


def paint_tang(p, style, f, dims):
    common(p, style, f, 't_', FAKE_FUR, FAKE_FUR_DARK, FAKE_FUR_TIP, MONK_SKIN, MONK_SHADE, (150, 110, 74))
    if style in ('t_robe', 'pelvis', 't_robe_chest'):
        cloth(p, f, ROBE_GREY, fold=4)
        if style == 't_robe':
            trim(p, f, (90, 82, 70), top=False, width=1)
    if style == 't_trouser':
        cloth(p, f, (196, 186, 160), fold=3)
    if style == 't_shoe':
        w.leather(p, f, (60, 50, 40))
        w.flat(p, f, (220, 214, 196), faces=('bottom',))
    if style in ('t_sleeve', 't_sleeve2'):
        cloth(p, f, ROBE_GREY, fold=3)
    if style == 't_bigsleeve':
        cloth(p, f, ROBE_GREY, fold=3)
        trim(p, f, (90, 82, 70), top=False)
    if style == 't_hand':
        fake_tell(p, f)                                           # hairy hands: the likeness wears thin
        skin(p, f, (186, 140, 100), (110, 78, 54), faces=('front',))
    if style == 't_kasaya':
        cloth(p, f, KASAYA, RED_DEEP, fold=5)
        for s in S:                                               # the field of patches, barred in gold
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                for j in range(fh):
                    if i % 6 == 0 or j % 7 == 0:
                        p.px((fx + i, fy + j), k(GOLD, w.LIGHT[s] * 0.92))
        trim(p, f, GOLD_DEEP)
    if style == 't_ring':
        gold(p, f)
        fx, fy, fw, fh = f['front']
        w.disc(p, fx + fw / 2, fy + fh / 2, 1.0, (60, 40, 20))
    if style in ('t_skull', 't_crown'):
        skin(p, f, MONK_SKIN, MONK_SHADE)                          # a monk's shaven head
    if style == 't_face':
        w.flat(p, f, MONK_SKIN)
        monkey_face(p, f['front'], MONK_SKIN, MONK_SHADE, TANG_EYE, ring=MONK_SKIN, eye_w=2, heart=False, glow=0.35)
        fx, fy, fw, fh = f['front']
        for i in range(2, fw - 2):                                # a calm brow, a little too furry
            p.px((fx + i, fy + 1), FAKE_FUR_DARK if i % 3 else (40, 30, 20))
    if style == 't_brow':
        skin(p, f, MONK_SKIN, MONK_SHADE)
    if style == 't_cheekfur':
        tufted(p, f, FAKE_FUR, FAKE_FUR_DARK, depth=2, every=2)    # sideburns of monkey fur, the likeness slipping
    if style == 't_muzzle':
        muzzle(p, f, MONK_SKIN, MONK_SHADE, nostril=(110, 70, 50), lips=(150, 80, 70))
    if style == 't_nose':
        skin(p, f, MONK_SKIN, MONK_SHADE)
    if style == 't_jaw':
        skin(p, f, MONK_SKIN, MONK_SHADE)
    if style == 't_ear':
        skin(p, f, (210, 150, 120), (140, 90, 70))
        fake_tell(p, f) if False else None
    if style == 't_crownband':
        gold(p, f)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(1, fw, 3):
                p.px((fx + i, fy + fh // 2), (190, 40, 40))
    if style == 't_leaf':                                         # each leaf a gold plaque, a seated Buddha painted on it
        gold(p, f)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            w.point(p, f[s], down=False, keep=1.4, sharp=0.9)
            cx = fx + fw // 2
            for j in range(fh // 3, fh - 1):
                p.px((cx, fy + j), (176, 48, 36))
                if j > fh // 2:
                    p.px((cx - 1, fy + j), (176, 48, 36))
                    p.px((cx + 1, fy + j), (176, 48, 36))
            w.disc(p, cx + 0.5, fy + fh / 3, 1.0, (60, 110, 170))
    if style == 't_beads':
        w.flat(p, f, (0, 0, 0))
        for s in S + ('top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                p.clear(xy)
        fx, fy, fw, fh = f['front']                                # a string of dark beads in a V, a red tassel
        for t in range(0, 20):
            x = fw / 2 + (t - 10) * fw / 22
            y = abs(t - 10) * -fh / 12 + fh - 2
            p.px((fx + x, fy + y), (70, 40, 24))
            p.px((fx + x + 1, fy + y), (110, 70, 40))
        p.px((fx + fw // 2, fy + fh - 1), (190, 30, 30))
    if style == 'fake_tail':
        fake_tell(p, f)
    if style == 't_staffshaft':
        w.wood(p, f, (150, 96, 40), along='y')
    if style in ('t_gold', 't_staffring', 't_littlering'):
        gold(p, f)
        if style == 't_staffring':                                # the ring's loop, open in the middle
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    if 1 <= i < fw - 1 and 1 <= j < fh - 1:
                        p.clear(xy)


BAJIE_SKIN, BAJIE_SHADE = (92, 82, 86), (46, 40, 44)
BAJIE_SNOUT = (196, 140, 140)
BAJIE_EYE = (240, 210, 120)
BLACK_ROBE = (40, 38, 46)

BAJIE = robe_frame(dict(hipw=2.4, thigh=3.6, thigh_len=4.8, shin=3.2, shin_len=4.4, foot=(3.4, 4.8, 1.4),
                        pelvis=(8.4, 4.0, 6.4), chest=(9.6, 8.0, 7.0), arm=(3.0, 6.0, 2.8, 5.8, 2.8, 2.8),
                        lean=0.0, armr=(-0.15, 0, 0.2), forer=(-0.4, 0, 0),
                        tufts=False, upperarm_style='j_arm', forearm_style='j_arm', hand_style='j_hand', foot_style='j_shoe',
                        pelvis_style='j_robe', chest_style='j_chest', thigh_style='j_trouser', shin_style='j_trouser'),
                   9.2, 7.4, 8.6, 'j_robe')
BAJIE = mirrored(BAJIE + [
    # the great belly, bare under the open robe
    part('belly', 'chest', (0, -2.2, -3.4), (0, 0, 0), [(-4.4, -4.6, -2.4, 8.8, 7.4, 3.2, 'j_belly'), (-3.4, -3.6, -3.2, 6.8, 5.4, 1.0, 'j_belly')]),
    part('right_lapel', 'chest', (-3.6, -8.0, -3.6), (0, 0, 0.18), [(-1.2, 0, -0.5, 2.6, 9.0, 1.0, 'j_lapel')]),
    part('j_belt', 'body', (0, -3.4, 0), (0, 0, 0), [(-4.6, -0.6, -3.8, 9.2, 1.6, 7.6, 'j_belt')]),
    # the pig's head: a broad head, a long snout, flapping ears, tusks
    part('head', 'chest', (0, -8.0, -1.2), (0, 0, 0), [
        (-3.8, -6.0, -3.6, 7.6, 6.2, 7.2, 'j_head'),
        (-2.6, -3.6, -8.4, 5.2, 3.6, 5.0, 'j_snout'),
        (-2.8, -3.8, -8.8, 5.6, 3.8, 0.6, 'j_snoutend'),
        (-4.2, -3.2, -3.8, 8.4, 3.6, 4.0, 'j_jowl'),
        (-3.4, -4.9, -3.9, 6.8, 1.0, 0.6, 'j_brow'),
        (-3.6, -6.4, -1.6, 7.2, 1.2, 4.6, 'j_bristle')]),
    part('jaw', 'head', (0, -0.3, -3.2), (0.05, 0, 0), [(-2.0, 0, -4.2, 4.0, 1.4, 4.6, 'j_jaw')]),
    part('right_tusk', 'jaw', (-1.6, 0.1, -3.8), (-0.3, 0, 0.1), [(-0.35, -1.8, -0.35, 0.7, 1.9, 0.7, 'j_tusk')]),
    part('right_ear', 'head', (-3.7, -5.8, -0.8), (-0.95, 0.35, 0.55), [(-0.4, 0, -2.6, 0.8, 6.8, 5.2, 'j_ear')]),
    part('right_brow', 'head', (-1.8, -4.9, -3.75), (0, 0, 0.25), [(-1.6, -0.5, -0.4, 3.0, 0.9, 0.8, 'j_brow')]),
    part('right_eye', 'head', (-1.8, -4.3, -3.65), (0, 0, 0), [(-0.8, -0.4, -0.2, 1.6, 0.9, 0.3, 'j_eye')]),
])
BAJIE += tail('body', (0, -0.6, 3.4), 4, 1.3, 0.8, 3.0, 0.25, 'fake_tail', start=(1.2, 0, 0))
RAKE_AIM = aim([(0, 0, 0), (0, 0, 0), (-0.15, 0, 0.2), (-0.4, 0, 0)], (0.1, 0, 0))
BAJIE += [part('rake', 'right_hand', (0, 1.4, 0), RAKE_AIM, [
    (-0.45, -24.0, -0.45, 0.9, 32.0, 0.9, 'j_rakeshaft'),
    (-0.7, -24.8, -0.7, 1.4, 1.4, 1.4, 'j_rakeiron'),
    (-0.8, -26.0, -4.6, 1.6, 1.8, 9.2, 'j_rakehead')])]
for i in range(9):
    BAJIE.append(part('rake_tooth_%d' % i, 'rake', (0, -24.4, -4.2 + i * 1.05), (0, 0, 0), [(-0.25, 0, -0.25, 0.5, 2.6, 0.5, 'j_tooth')]))
BAJIE = w.settle(BAJIE, ('right_foot', 'left_foot'))


def paint_bajie(p, style, f, dims):
    common(p, style, f, 'j_', FAKE_FUR, FAKE_FUR_DARK, FAKE_FUR_TIP, BAJIE_SKIN, BAJIE_SHADE, (110, 90, 70))
    if style in ('j_robe', 'pelvis', 'j_chest', 'j_arm', 'j_lapel'):
        cloth(p, f, BLACK_ROBE, k(BLACK_ROBE, 0.5), fold=4)
        if style in ('j_robe', 'j_lapel'):
            trim(p, f, (120, 30, 30), top=False)
    if style == 'j_trouser':
        cloth(p, f, (96, 80, 60), fold=3)
    if style == 'j_shoe':
        w.leather(p, f, (36, 30, 30))
    if style == 'j_hand':
        fake_tell(p, f)
        skin(p, f, BAJIE_SKIN, BAJIE_SHADE, faces=('front', 'bottom'))
    if style in ('j_belly', 'j_head', 'j_jaw', 'j_jowl'):
        skin(p, f, BAJIE_SKIN, BAJIE_SHADE, mottle=0.4)
        if style == 'j_belly':
            fx, fy, fw, fh = f['front']
            w.disc(p, fx + fw / 2, fy + fh * 0.6, 0.8, BAJIE_SHADE)   # his navel
        if style == 'j_head':
            fx, fy, fw, fh = f['front']
            for i in range(fw):                                     # a wrinkled brow
                if i % 3 != 1:
                    p.px((fx + i, fy + fh // 3), BAJIE_SHADE)
    if style == 'j_snout':
        skin(p, f, mix(BAJIE_SKIN, BAJIE_SNOUT, 0.5), BAJIE_SHADE)
        for s in ('right', 'left', 'top'):
            fx, fy, fw, fh = f[s]
            for i in range(0, fw, 2):
                p.px((fx + i, fy + fh // 2), BAJIE_SHADE)
    if style == 'j_snoutend':
        skin(p, f, BAJIE_SNOUT, k(BAJIE_SNOUT, 0.6))
        fx, fy, fw, fh = f['front']
        for x in (fw // 2 - 2, fw // 2 + 1):
            for j in (fh // 2 - 1, fh // 2):
                for t in range(2):
                    p.px((fx + x + t, fy + j), (40, 20, 22))
    if style == 'j_brow':
        skin(p, f, k(BAJIE_SKIN, 0.8), BAJIE_SHADE)
    if style == 'j_bristle':
        tufted(p, f, (40, 34, 36), (16, 14, 14), depth=2, every=2, faces=('front', 'right', 'left'))
    if style == 'j_eye':
        w.flat(p, f, (240, 230, 210))
        fx, fy, fw, fh = f['front']
        for j in range(fh):
            p.lit((fx + fw // 2, fy + j), BAJIE_EYE, 0.5)
            p.px((fx + fw // 2 - 1, fy + j), (20, 14, 12))
    if style == 'j_tusk':
        w.flat(p, f, IVORY)
    if style == 'j_ear':
        skin(p, f, BAJIE_SKIN, BAJIE_SHADE)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if abs(i - fw / 2) < fw * 0.3 and 1 < j < fh - 2:
                    p.px(xy, mix(BAJIE_SNOUT, BAJIE_SKIN, 0.4))   # the pink inside
            w.point(p, f[s], down=True, keep=1.0, sharp=0.9)
    if style == 'j_belt':
        cloth(p, f, (176, 140, 60), fold=2)
    if style == 'fake_tail':
        fake_tell(p, f)
    if style == 'j_rakeshaft':
        w.wood(p, f, (130, 84, 40), along='y')
    if style in ('j_rakeiron', 'j_rakehead', 'j_tooth'):
        w.iron(p, f, (150, 156, 166), dents=0.1)
        if style == 'j_rakehead':
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i in range(1, fw - 1, 3):
                    p.px((fx + i, fy + fh // 2), GOLD)


SHA_SKIN, SHA_SHADE = (92, 112, 126), (44, 56, 66)
SHA_HAIR, SHA_HAIR_DARK, SHA_HAIR_TIP = (176, 50, 30), (96, 22, 14), (230, 100, 50)
SHA_EYE = (250, 220, 90)
OCHRE = (176, 120, 46)

SHA = robe_frame(dict(hipw=1.9, thigh=3.0, thigh_len=6.2, shin=2.6, shin_len=6.2, foot=(3.0, 4.8, 1.4),
                      pelvis=(6.4, 3.0, 4.4), chest=(7.8, 9.2, 4.8), arm=(2.6, 7.2, 2.4, 7.0, 2.6, 2.8),
                      lean=0.06, chest_lean=0.06, armr=(-0.1, 0, 0.1), forer=(-0.35, 0, 0),
                      tufts=False, upperarm_style='s_arm', forearm_style='s_bracer', hand_style='s_hand', foot_style='s_shoe',
                      pelvis_style='s_robe', chest_style='s_robe_chest', thigh_style='s_trouser', shin_style='s_trouser'),
                 8.2, 5.6, 10.0, 's_robe')
SHA = mirrored(SHA + monkey_head('chest', (0, -9.2, -0.8), (0, 0, 0),
                                 dict(w=3.4, h=6.4, d=3.2, p='s_', muzzle=(1.7, 1.7, 1.0), ears=1, ear=1.05)) + [
    part('beard', 'jaw', (0, 1.2, -1.2), (0.3, 0, 0), [(-2.6, 0, -1.0, 5.2, 4.6, 2.0, 's_beard')]),
    part('mane', 'head', (0, -5.6, 1.0), (0.4, 0, 0), [(-3.6, -1.6, -2.8, 7.2, 3.8, 7.0, 's_hair'),
                                                       (-3.2, 1.6, 2.4, 6.4, 5.2, 1.8, 's_hair')]),
    part('right_sideburn', 'head', (-3.1, -3.4, -1.0), (0, 0.2, 0.3), [(-1.0, -1.0, -1.4, 1.6, 4.4, 3.0, 's_hair')]),
])
for i in range(9):                                                 # the necklace of nine skulls
    a = -1.2 + i * 0.3
    SHA.append(part('skull_%d' % i, 'chest', (3.9 * math.sin(a) * 1.05, -8.0 + 2.4 * math.cos(a * 1.1), -2.6 - 1.4 * math.cos(a)),
                    (0, -a * 0.8, 0), [(-0.7, -0.7, -0.8, 1.4, 1.5, 1.4, 's_skull_bead')]))
SHA += [part('right_cuffband', 'right_forearm', (0, 5.0, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3.0, 1.2, 3.0, 's_goldband')])]
SHA += tail('body', (0, -0.4, 2.4), 4, 1.3, 0.8, 3.0, 0.22, 'fake_tail', start=(1.2, 0, 0))
SPADE_AIM = aim([(0.06, 0, 0), (0.06, 0, 0), (-0.1, 0, 0.1), (-0.35, 0, 0)], (0.08, 0, 0))
SHA += [part('spade', 'right_hand', (0, 1.4, 0), SPADE_AIM, [
    (-0.45, -30.0, -0.45, 0.9, 36.0, 0.9, 's_spadeshaft'),
    (-0.7, -30.8, -0.7, 1.4, 1.4, 1.4, 's_goldband'),
    (-0.25, -35.6, -2.2, 0.5, 5.2, 4.4, 's_spadeblade'),
    (-0.7, 5.2, -0.7, 1.4, 1.4, 1.4, 's_goldband'),
    (-0.25, 6.4, -2.6, 0.5, 2.6, 5.2, 's_crescent')])]
SHA = w.settle(SHA, ('right_foot', 'left_foot'))


def paint_sha(p, style, f, dims):
    common(p, style, f, 's_', SHA_HAIR, SHA_HAIR_DARK, SHA_HAIR_TIP, SHA_SKIN, SHA_SHADE, (96, 100, 104))
    if style in ('s_robe', 'pelvis', 's_robe_chest', 's_arm'):
        cloth(p, f, OCHRE, fold=4)
        if style == 's_robe':
            trim(p, f, (110, 66, 24), top=False)
    if style == 's_trouser':
        cloth(p, f, (70, 60, 50), fold=3)
    if style == 's_shoe':
        w.leather(p, f, (50, 40, 34))
    if style == 's_bracer':
        w.leather(p, f, (80, 52, 32), stitch=GOLD_DEEP)
    if style == 's_hand':
        fake_tell(p, f)
        skin(p, f, SHA_SKIN, SHA_SHADE, faces=('front',))
    if style in ('s_skull', 's_crown', 's_brow', 's_jaw', 's_nose'):
        skin(p, f, SHA_SKIN, SHA_SHADE)
    if style == 's_face':
        w.flat(p, f, SHA_SKIN)
        monkey_face(p, f['front'], SHA_SKIN, SHA_SHADE, SHA_EYE, ring=SHA_SKIN, eye_w=2, heart=False)
    if style == 's_muzzle':
        muzzle(p, f, SHA_SKIN, SHA_SHADE, lips=(40, 20, 20), teeth=IVORY)
    if style == 's_cheekfur':
        tufted(p, f, FAKE_FUR, FAKE_FUR_DARK, depth=2, every=2)
    if style == 's_ear':
        skin(p, f, k(SHA_SKIN, 1.1), SHA_SHADE)
    if style in ('s_beard', 's_hair'):
        tufted(p, f, SHA_HAIR, SHA_HAIR_DARK, SHA_HAIR_TIP, depth=3, every=2)
    if style == 's_skull_bead':
        w.flat(p, f, (226, 218, 196))
        fx, fy, fw, fh = f['front']
        for x in (0, fw - 1):
            p.px((fx + x, fy + 1), (30, 20, 18))
        p.px((fx + fw // 2, fy + fh - 1), (60, 40, 30))
    if style == 's_goldband':
        gold(p, f)
    if style == 'fake_tail':
        fake_tell(p, f)
    if style == 's_spadeshaft':
        w.wood(p, f, (80, 54, 40), along='y')
    if style in ('s_spadeblade', 's_crescent'):
        w.iron(p, f, STEEL, dents=0.05, rim=False)
        if style == 's_crescent':
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    if abs(i - fw / 2) < fw * 0.3 and j < fh - 2:
                        p.clear(xy)
        else:
            for s in ('right', 'left'):
                w.point(p, f[s], down=False, keep=1.4, sharp=1.2)


# ── The Six-Eared Macaque ────────────────────────────────────────────────────

MAC_FUR, MAC_DARK, MAC_TIP = (104, 72, 44), (52, 34, 22), (164, 122, 74)
MAC_FACE, MAC_SHADE = (196, 112, 70), (120, 58, 34)
MAC_EYE = (255, 96, 30)
CLOAK = (150, 20, 22)

MACAQUE = biped(dict(hipw=1.9, thigh=3.0, thigh_len=6.0, shin=2.7, shin_len=6.0, foot=(3.0, 5.0, 1.6),
                     pelvis=(6.0, 3.4, 4.4), chest=(7.6, 9.0, 5.0), arm=(2.7, 7.4, 2.5, 7.2, 2.7, 2.8),
                     lean=0.08, chest_lean=0.05, legr=(-0.2, 0, 0.06), shinr=(0.32, 0, 0), footr=(-0.12, 0, 0),
                     armr=(-0.12, 0, 0.12), forer=(-0.4, 0, 0), thigh_style='m_trouser', shin_style='m_boot',
                     foot_style='m_bootfoot', chest_style='m_mail', pelvis_style='m_mail_low'))
MACAQUE = mirrored(MACAQUE + monkey_head('chest', (0, -9.0, -0.9), (-0.05, 0, 0),
                                         dict(w=3.7, h=6.5, d=3.4, p='m_', muzzle=(2.0, 2.2, 2.0), ears=3, ear=0.9, ruff=1.35)) + [
    # the cap: a gold dome, phoenix wings rising at its sides, the two long pheasant plumes arcing back
    part('cap', 'head', (0, -5.4, 0.2), (0, 0, 0), [(-3.0, -2.4, -3.0, 6.0, 2.6, 6.0, 'm_cap'),
                                                    (-0.9, -3.4, -3.3, 1.8, 1.6, 1.2, 'm_capjewel')]),
    part('right_capwing', 'cap', (-3.1, -1.0, 0), (0.1, 0.1, -0.55), [(-0.3, -3.4, -1.6, 0.6, 3.6, 3.4, 'm_wing')]),
    part('right_plume', 'cap', (-1.0, -2.4, 1.0), (-0.95, 0.1, -0.15), [(-0.35, -14.0, -0.35, 0.7, 14.0, 0.7, 'm_plume')]),
    part('right_plume_tip', 'right_plume', (0, -13.6, 0), (-0.55, 0, -0.1), [(-0.3, -9.0, -0.3, 0.6, 9.0, 0.6, 'm_plume')]),
    part('fillet', 'head', (0, -4.2, 0), (0, 0, 0), [(-3.45, -0.5, -3.3, 6.9, 0.9, 6.6, 'm_fillet')]),
    # the shoulder plates, the jade belt, the tiger-skin kilt, the cloak
    part('right_pauldron', 'right_arm', (-0.7, -0.8, 0), (0, 0, 0.32), [(-2.0, -1.4, -2.2, 3.6, 2.6, 4.4, 'm_pauldron'),
                                                                        (-2.4, 0.9, -2.4, 3.4, 1.8, 4.8, 'm_pauldron2')]),
    part('belt', 'body', (0, -3.0, 0), (0, 0, 0), [(-3.5, -0.7, -2.6, 7.0, 1.6, 5.2, 'm_belt'),
                                                  (-1.2, -1.0, -2.9, 2.4, 2.2, 0.6, 'm_buckle')]),
    part('sway_kilt_front', 'body', (0, -1.4, -2.3), (-0.08, 0, 0), [(-3.2, 0, -0.4, 6.4, 5.6, 0.8, 'm_tiger')]),
    part('sway_kilt_back', 'body', (0, -1.4, 2.3), (0.12, 0, 0), [(-3.2, 0, -0.4, 6.4, 5.2, 0.8, 'm_tiger')]),
    part('right_kilt_side', 'body', (-3.1, -1.4, 0), (0, 0, 0.14), [(-0.4, 0, -2.2, 0.8, 4.8, 4.4, 'm_tiger')]),
    part('sway_cloak', 'chest', (0, -8.6, 2.8), (0.16, 0, 0), [(-4.6, 0, -0.3, 9.2, 15.0, 0.6, 'm_cloak')]),
    part('right_bracer', 'right_forearm', (0, 2.4, 0), (0, 0, 0), [(-1.55, 0, -1.55, 3.1, 4.2, 3.1, 'm_bracer')]),
    part('right_bootcuff', 'right_shin', (0, 0.8, 0), (0, 0, 0), [(-1.7, 0, -1.7, 3.4, 1.4, 3.4, 'm_cuff')]),
])
MACAQUE += tail('body', (0, -1.6, 2.3), 5, 1.6, 0.9, 3.0, 0.24, 'm_tail', start=(1.2, 0, 0))
# the iron staff, gold-banded at both ends, held at his right side, upright
MSTAFF_AIM = aim([(0.08, 0, 0), (0.05, 0, 0), (-0.12, 0, 0.12), (-0.4, 0, 0)], (1.2, 0, 0))
MACAQUE += [part('staff', 'right_hand', (0, 1.4, 0), MSTAFF_AIM, [
    (-0.55, -18.0, -0.55, 1.1, 36.0, 1.1, 'm_staff'),
    (-0.75, -21.0, -0.75, 1.5, 3.2, 1.5, 'm_staffgold'),
    (-0.75, 17.8, -0.75, 1.5, 3.2, 1.5, 'm_staffgold')])]
MACAQUE = w.settle(MACAQUE, ('right_foot', 'left_foot'))


def paint_macaque(p, style, f, dims):
    common(p, style, f, 'm_', MAC_FUR, MAC_DARK, MAC_TIP, MAC_FACE, MAC_SHADE, (110, 64, 44))
    if style in ('upperarm', 'forearm', 'm_skull', 'm_crown', 'm_tail'):
        pelt(p, f, MAC_FUR, MAC_DARK, MAC_TIP)
    if style == 'hand':
        skin(p, f, (110, 64, 44), (60, 32, 22))
        fx, fy, fw, fh = f['front']
        for x in range(0, fw, 2):
            for j in range(fh // 2, fh):
                p.px((fx + x, fy + j), (44, 22, 16))
    if style == 'm_face':
        w.flat(p, f, MAC_FUR)
        monkey_face(p, f['front'], MAC_FACE, MAC_SHADE, MAC_EYE, ring=k(MAC_FUR, 0.9), eye_w=2)
    if style == 'm_brow':
        pelt(p, f, MAC_TIP, MAC_FUR)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), (40, 20, 12))
    if style == 'm_cheekfur':
        tufted(p, f, MAC_TIP, MAC_FUR, (210, 170, 110), depth=3, every=2)
    if style == 'm_muzzle':
        muzzle(p, f, MAC_FACE, MAC_SHADE, teeth=IVORY, fangs=True)
    if style == 'm_nose':
        skin(p, f, k(MAC_FACE, 0.85), MAC_SHADE)
    if style == 'm_jaw':
        skin(p, f, MAC_FACE, MAC_SHADE)
    if style == 'm_ear':
        skin(p, f, (200, 120, 84), (124, 64, 40))
        for s in ('right',):
            fx, fy, fw, fh = f[s]
            w.disc(p, fx + fw / 2, fy + fh / 2, min(fw, fh) * 0.25, (110, 50, 30))
    if style in ('m_cap', 'm_wing'):
        gold(p, f)
        for s in S:                                               # worked with purple-gold scrolls
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                if (i // 2) % 3 == 0:
                    p.px((fx + i, fy + fh // 2), (120, 50, 130))
        if style == 'm_wing':
            for s in ('right', 'left'):
                w.point(p, f[s], down=False, keep=0.8, sharp=0.7)
    if style == 'm_capjewel':
        w.flat(p, f, (210, 30, 40))
        fx, fy, fw, fh = f['front']
        p.lit((fx + fw // 2, fy + fh // 2), (255, 120, 110), 0.6)
    if style == 'm_plume':                                         # a pheasant's tail feather: barred buff and brown
        for s in S + ('top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                c = (212, 170, 96) if (j // 3) % 2 == 0 else (110, 64, 30)
                p.px(xy, k(c, w.LIGHT[s]))
    if style == 'm_fillet':
        gold(p, f)
    if style in ('m_mail', 'm_mail_low'):
        chainmail(p, f, GOLD, GOLD_DEEP)
        w.flat(p, f, k(GOLD_DEEP, 0.9), faces=('top', 'bottom'))
        if style == 'm_mail':
            fx, fy, fw, fh = f['front']                           # the breast's great gold mirror, and the collar
            w.disc(p, fx + fw / 2, fy + fh * 0.36, 3.6, GOLD_LIGHT, rim=GOLD_DEEP)
            w.disc(p, fx + fw / 2, fy + fh * 0.36, 1.6, (200, 60, 40))
            for i in range(fw):
                p.px((fx + i, fy), RED)
                p.px((fx + i, fy + 1), RED_DEEP)
    if style in ('m_pauldron', 'm_pauldron2'):
        lamellae(p, f, GOLD, RED, rows=2, cols=3, rim=GOLD_DEEP)
        gold(p, f, faces=('top',))
    if style == 'm_belt':
        w.leather(p, f, RED_DEEP)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(1, fw - 1, 4):
                for t in range(2):
                    p.px((fx + i + t, fy + fh // 2), JADE)
    if style == 'm_buckle':
        gold(p, f)
        fx, fy, fw, fh = f['front']
        w.disc(p, fx + fw / 2, fy + fh / 2, 1.2, JADE)
    if style == 'm_tiger':
        tiger(p, f)
    if style == 'm_cloak':
        for s in ('front', 'back'):
            w.weave(p, f[s], CLOAK, k(CLOAK, 0.45), light=w.LIGHT[s], fold=5, fall=0.35)
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), GOLD_DEEP)
                p.px((fx + i, fy + fh - 2), GOLD)
            for j in range(fh):
                for i in (0, fw - 1):
                    p.px((fx + i, fy + j), GOLD_DEEP)
        w.flat(p, f, k(CLOAK, 0.5), faces=('right', 'left', 'top', 'bottom'))
    if style == 'm_trouser':
        cloth(p, f, RED, fold=3)
    if style in ('m_boot', 'm_bootfoot'):
        w.leather(p, f, (60, 40, 30), stitch=GOLD_DEEP)
        if style == 'm_bootfoot':
            w.flat(p, f, (220, 210, 190), faces=('bottom',))
            for s in S:                                           # cloud scrolls on the cloud-walking boots
                fx, fy, fw, fh = f[s]
                for i in range(0, fw, 3):
                    p.px((fx + i, fy + fh // 2), GOLD)
    if style == 'm_cuff':
        tufted(p, f, MAC_TIP, MAC_FUR, depth=1, every=2)
    if style == 'm_bracer':
        gold(p, f)
        for s in S:
            fx, fy, fw, fh = f[s]
            for j in range(1, fh, 3):
                for i in range(fw):
                    p.px((fx + i, fy + j), GOLD_DEEP)
    if style == 'm_staff':
        staff_iron(p, f)
        for s in S:
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 7):
                p.px((fx + fw // 2, fy + j), k(IRON, 1.4))
    if style == 'm_staffgold':
        gold(p, f)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh // 2), GOLD_DEEP)


# ── Captains' skins ──────────────────────────────────────────────────────────

def build(name, parts, painter, tag, seed, captain=None):
    p = Painter(seed)
    slots = paint(parts, p, painter)
    w.save(p, name)
    emit(parts, slots, JAVA, tag)
    w.report(name, parts, SIZES[tag], slots)
    if captain:
        c = Painter(seed)
        paint(parts, c, lambda pp, st, ff, dd: captain(pp, st, ff, dd, True))
        c.img.save(OUT + name + '_captain.png')


if __name__ == '__main__':
    build('stone_gibbon', GIBBON, paint_gibbon, 'gibbon', 61, paint_gibbon)
    build('spear_ape', APE, paint_ape, 'ape', 62, paint_ape)
    build('great_baboon', BABOON, paint_baboon, 'baboon', 63, paint_baboon)
    build('false_tang_sanzang', TANG, paint_tang, 'tang', 64)
    build('false_zhu_bajie', BAJIE, paint_bajie, 'bajie', 65)
    build('false_sha_wujing', SHA, paint_sha, 'sha', 66)
    build('six_eared_macaque', MACAQUE, paint_macaque, 'macaque', 67)


# ── Items ────────────────────────────────────────────────────────────────────

def items():
    """The Macaque's iron staff (a black iron staff on the diagonal, gold-banded at both ends) and a
    hank of monkey hair (golden-brown, curling), 32x32."""
    import random
    from PIL import Image
    rnd = random.Random(7)
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    for t in range(3, 29):
        x, y = t, 31 - t
        for d in (-1, 0, 1):
            band = t < 8 or t > 23
            c = (GOLD if d <= 0 else GOLD_DEEP) if band else ((92, 90, 100) if d < 0 else (40, 38, 46) if d == 0 else (22, 20, 26))
            if band and t in (7, 24):
                c = GOLD_DEEP
            if 0 <= x + d < 32:
                img.putpixel((x + d, y), c + (255,))
    for t in (2, 29):
        img.putpixel((t, 31 - t), GOLD_LIGHT + (255,))
    img.save('src/main/resources/assets/jttw_engine/textures/item/macaque_staff.png')
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    for strand in range(26):
        a = rnd.uniform(0, math.pi)
        r0 = rnd.uniform(2, 6)
        for t in range(40):
            ang = a + t * 0.12
            r = r0 + t * 0.22
            x, y = 16 + math.cos(ang) * r * 0.8, 17 + math.sin(ang) * r * 0.55 - t * 0.12
            if 0 <= x < 32 and 0 <= y < 32:
                tone = 0.75 + 0.5 * (t / 40)
                c = tuple(max(0, min(255, int(v * tone))) for v in (150, 104, 54))
                img.putpixel((int(x), int(y)), c + (255,))
    img.save('src/main/resources/assets/jttw_engine/textures/item/monkey_hair.png')


if __name__ == '__main__':
    items()
