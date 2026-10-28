"""The kobold (穴居蜥妖), a lizard yaoguai of the caves of the open world. As wild_paint.py says: its model
is written into WildRenderers' koboldLayer() between its GENERATED markers, its skin
(textures/entity/kobold.png) painted to the same box-UV layout, its eyes and its lantern glowing
(kobold_glow.png).

A lizard warrior as tall as a man that mines the dark under the mountains, standing on heavy
digitigrade legs, a thick tail tapering behind it to the ground. A crocodile's head: a long snout,
teeth showing all along its jaw line, raised nostrils at its tip, a bony brow ridge over each
slit-pupilled amber eye, a crest of spines from its crown down its neck, its back and its tail.
Healthy, glossy scales in clean rows, each lit at its upper edge and shadowed at its lower one: deep
olive-green over its back and limbs, shading to a pale cream-yellow belly of broad scutes; rows of
bony osteoderm plates down its back and tail; finer scales on its head. Clawed hands and feet.

Its gear is light, so its body shows: a leather harness across its chest, a belt with a leather
flap before it, a scrap-iron pauldron riveted over its left shoulder and a scrap plate over its
right thigh, leather bracers; a satchel of ore at its left hip (copper and iron, a glowing jade), an
iron miner's lantern hanging at its right, burning. In its right fist a miner's pick, its iron head
lashed on with rawhide.

Run from the repository root: python assets_source/kobold.py"""
import math

import beastgen as bg
import wild_paint as w
from wild_paint import part, mirrored, aim, paint, Painter, k, mix

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

SIZE = 0.9           # KoboldRenderer.SIZE

DORSAL = (62, 88, 50)          # deep olive-green
DORSAL_EDGE = (34, 50, 28)
PLATE = (56, 78, 42)           # the osteoderms: a shade darker, raised
BELLY = (226, 208, 146)        # pale cream-yellow
BELLY_EDGE = (156, 136, 88)
GLOSS = (214, 230, 190)
EYE = (255, 176, 34)
TOOTH = (238, 230, 204)
GUM = (120, 46, 50)
MOUTH = (60, 18, 22)
CLAW = (36, 32, 28)
LEATHER = (110, 72, 42)
LEATHER_DEEP = (62, 40, 24)
IRON = (92, 90, 96)
RUST = (130, 66, 30)
WOOD = (122, 86, 50)
RAWHIDE = (196, 170, 120)
COPPER = (190, 112, 58)
JADE = (100, 226, 150)
FLAME = (255, 200, 90)

BODY = (0.3, 0, 0)
HEAD = (-0.2, 0, 0)
ARM, FOREARM, HAND = (-0.3, 0, 0.12), (-0.85, 0, 0), (0, 0, 0)
LARM, LFOREARM, LHAND = (-0.1, 0, -0.14), (-0.5, 0, 0), (0, 0, 0)


def back_spines(y0, y1, z, n, h, style='spine'):
    """A row of spines down its back."""
    out = []
    for i in range(n):
        y = y0 + (y1 - y0) * i / max(1, n - 1)
        out.append((-0.35, y, z, 0.7, h, 2.0, style))
    return out


def teeth(x, y, z0, length, up):
    """A row of teeth along a jaw's edge, as a thin strip (cut into teeth on the skin)."""
    return (x, y - (0.8 if up else 0), z0, 0.3, 0.8, length, 'teeth_up' if up else 'teeth_down')


KOBOLD = mirrored([
    # heavy digitigrade legs: a thick thigh, a shin, a long ankle, a broad clawed foot
    part('right_leg', 'root', (-2.6, 12.0, 1.2), (-0.55, 0, 0.06), [
        (-1.9, -1.2, -2.0, 3.8, 7.0, 4.0, 'thigh')]),
    part('right_shin', 'right_leg', (0, 6.2, 0.2), (1.15, 0, 0), [
        (-1.4, 0, -1.4, 2.8, 6.0, 2.8, 'shin')]),
    part('right_ankle', 'right_shin', (0, 5.8, 0), (-0.95, 0, 0), [
        (-1.1, 0, -1.1, 2.2, 4.8, 2.2, 'ankle')]),
    part('right_foot', 'right_ankle', (0, 4.5, 0), (0.35, 0, 0), [
        (-1.7, -0.5, -2.8, 3.4, 1.5, 3.8, 'foot'),
        (-1.75, -0.1, -4.6, 1.0, 1.1, 2.0, 'toe'),
        (-0.5, -0.1, -4.9, 1.0, 1.1, 2.3, 'toe'),
        (0.75, -0.1, -4.6, 1.0, 1.1, 2.0, 'toe'),
        (-0.4, -0.2, 0.7, 0.8, 0.8, 1.4, 'dewclaw')]),

    # the body from the hips up, leaning a little forward; its pivot the hips
    part('body', 'root', (0, 12.0, 1.2), BODY, [
        (-3.3, -2.8, -2.4, 6.6, 3.6, 5.0, 'hips'),
        (-3.1, -7.8, -2.5, 6.2, 5.2, 4.8, 'belly'),
        (-3.9, -13.8, -2.8, 7.8, 6.2, 5.6, 'chest'),
        (-3.5, -3.7, -2.7, 7.0, 1.2, 5.4, 'belt'),
        (-0.9, -4.0, -2.95, 1.8, 1.6, 0.4, 'buckle'),
    ] + back_spines(-13.0, -4.0, 2.6, 6, 2.0)),
    part('neck', 'body', (0, -13.2, -0.6), (0.05, 0, 0), [
        (-2.0, -3.6, -2.0, 4.0, 4.2, 3.8, 'neck'),
        (-0.3, -3.6, 1.6, 0.6, 1.4, 1.6, 'spine'),
        (-0.3, -1.8, 1.6, 0.6, 1.4, 1.6, 'spine')]),
    part('flap', 'body', (0, -2.6, -2.6), (-0.1, 0, 0), [(-1.8, 0, -0.25, 3.6, 4.6, 0.5, 'flap')]),
    # the harness: a strap from its right shoulder down across its chest to its left hip
    part('strap', 'body', (-3.0, -13.6, -2.95), (0, 0, -0.62), [(-0.6, 0, -0.2, 1.2, 12.4, 0.4, 'strap')]),
    part('strap_back', 'body', (-3.0, -13.6, 2.95), (0, 0, -0.62), [(-0.6, 0, -0.2, 1.2, 12.4, 0.4, 'strap')]),
    # the satchel of ore at its left hip
    part('satchel', 'body', (3.6, -4.0, 0.2), (0.05, 0, -0.08), [
        (-0.4, 0, -2.0, 2.4, 3.4, 4.0, 'satchel'),
        (-0.5, -0.3, -2.1, 2.6, 1.3, 4.2, 'satchelflap'),
        (0.0, -1.0, -1.4, 1.2, 1.0, 1.2, 'ore_copper'),
        (0.3, -1.4, 0.0, 1.0, 1.4, 1.0, 'ore_jade'),
        (0.1, -0.9, 0.9, 1.2, 0.9, 1.0, 'ore_iron')]),
    # the lantern hanging from its belt at its right
    part('lantern', 'body', (-3.7, -3.0, -1.0), (-0.3, 0, 0.05), [
        (-0.15, 0, -0.15, 0.3, 1.0, 0.3, 'lanterncord'),
        (-1.1, 0.9, -1.1, 2.2, 0.5, 2.2, 'lanterncap'),
        (-0.9, 1.4, -0.9, 1.8, 2.4, 1.8, 'lanterncage'),
        (-0.55, 1.75, -0.55, 1.1, 1.7, 1.1, 'lanternfire'),
        (-1.1, 3.8, -1.1, 2.2, 0.5, 2.2, 'lanterncap')]),

    # the tail: heavy at its root, tapering to the ground behind; spines down it
    part('tail', 'body', (0, -1.6, 2.2), (-0.85, 0, 0), [
        (-2.1, -2.0, -0.6, 4.2, 4.0, 6.6, 'tail1'),
        (-0.3, -3.4, 0.6, 0.6, 1.4, 1.6, 'spine'),
        (-0.3, -3.4, 3.4, 0.6, 1.4, 1.6, 'spine')]),
    part('tail_mid', 'tail', (0, 0.1, 5.6), (-0.3, 0, 0), [
        (-1.6, -1.6, 0, 3.2, 3.2, 6.0, 'tail2'),
        (-0.25, -2.8, 0.8, 0.5, 1.2, 1.4, 'spine_small'),
        (-0.25, -2.8, 3.6, 0.5, 1.2, 1.4, 'spine_small')]),
    part('tail_end', 'tail_mid', (0, 0.2, 5.6), (0.2, 0, 0), [
        (-1.15, -1.15, 0, 2.3, 2.3, 5.4, 'tail3'),
        (-0.2, -2.1, 1.2, 0.4, 1.0, 1.2, 'spine_small')]),
    part('tail_tip', 'tail_end', (0, 0.1, 5.0), (0.22, 0, 0), [
        (-0.7, -0.7, 0, 1.4, 1.4, 4.4, 'tail4')]),

    # the head: a crocodile's
    part('head', 'neck', (0, -3.0, -0.6), HEAD, [
        (-2.6, -4.4, -3.0, 5.2, 4.4, 5.0, 'cranium'),
        (-2.15, -3.5, -7.2, 4.3, 2.5, 4.4, 'snout'),
        (-1.45, -3.2, -10.4, 2.9, 2.1, 3.3, 'snouttip'),
        (-1.05, -3.75, -10.2, 2.1, 0.7, 1.5, 'nostrils'),
        (-0.3, -5.4, -1.6, 0.6, 1.2, 1.4, 'spine_small'),
        (-0.3, -5.2, 0.2, 0.6, 1.4, 1.6, 'spine'),
        teeth(-2.2, -1.0, -6.9, 4.0, False),
        teeth(-1.5, -1.1, -10.1, 3.0, False),
    ]),
    part('jaw', 'head', (0, -0.9, -2.4), (0.05, 0, 0), [
        (-2.05, 0, -4.8, 4.1, 1.4, 5.2, 'jaw'),
        (-1.4, 0.05, -8.0, 2.8, 1.2, 3.3, 'jawtip'),
        (-1.7, 1.2, -4.0, 3.4, 1.3, 4.2, 'throat'),
        teeth(-1.95, 0.1, -4.6, 4.0, True),
        teeth(-1.3, 0.1, -7.7, 3.0, True)]),
    part('right_jowl', 'head', (-2.35, -1.6, -1.6), (0, 0.12, 0), [(-0.5, -0.9, -1.6, 0.9, 2.0, 3.0, 'jowl')]),
    part('right_brow', 'head', (-1.75, -4.15, -2.0), (0.15, 0.1, 0.25), [(-0.85, -0.6, -1.9, 1.7, 1.2, 3.2, 'brow')]),
    part('right_eye', 'head', (-2.5, -3.3, -2.0), (0, 0.1, 0), [(-0.3, -0.55, -0.85, 0.5, 1.1, 1.7, 'eye')]),

    # the arms: a scaled upper arm, a forearm in a leather bracer, a clawed hand
    part('right_arm', 'body', (-4.5, -12.8, 0.1), ARM, [
        (-1.5, -1.1, -1.5, 3.0, 6.8, 3.0, 'upperarm')]),
    part('right_forearm', 'right_arm', (0, 6.0, 0), FOREARM, [
        (-1.3, 0, -1.3, 2.6, 6.0, 2.6, 'forearm'),
        (-1.5, 2.4, -1.5, 3.0, 3.0, 3.0, 'bracer')]),
    part('right_hand', 'right_forearm', (0, 5.9, 0), HAND, [
        (-1.45, 0, -1.55, 2.9, 2.0, 3.1, 'hand'),
        (-1.45, 1.8, -1.5, 0.8, 2.4, 0.8, 'finger'),
        (-0.4, 1.8, -1.5, 0.8, 2.6, 0.8, 'finger'),
        (0.65, 1.8, -1.5, 0.8, 2.4, 0.8, 'finger'),
        (-1.7, 0.6, -2.05, 0.8, 1.8, 0.8, 'finger')]),
])

KOBOLD += [
    # a scrap-iron pauldron riveted over its left shoulder, a scrap plate over its right thigh
    part('pauldron', 'left_arm', (0.3, -0.6, 0), (0, 0, -0.3), [
        (-1.4, -1.6, -2.1, 3.6, 2.4, 4.2, 'pauldron'),
        (-0.9, 0.7, -1.9, 3.3, 1.8, 3.8, 'pauldron2')]),
    part('thigh_plate', 'right_leg', (-1.95, 1.0, 0), (0, 0, 0.05), [(-0.4, 0, -2.0, 0.6, 4.2, 4.0, 'tasset')]),
    # the pick in its right fist, its head up and forward
    part('pick', 'right_hand', (0, 1.0, -0.2), aim([BODY, ARM, FOREARM, HAND], (-0.25, 0, 0)), [
        (-0.45, -11.0, -0.45, 0.9, 13.0, 0.9, 'haft'),
        (-0.6, -11.8, -0.6, 1.2, 1.8, 1.2, 'lashing'),
        (-0.5, -11.5, -5.4, 1.0, 1.2, 4.9, 'pickpoint'),
        (-0.6, -11.6, 0.5, 1.2, 1.4, 2.8, 'pickblade')]),
]


def enlarge(parts, top, f):
    """`top` and everything under it drawn `f` times larger, about `top`'s pivot."""
    under = {top}
    out = []
    for n, par, pv, r, boxes, m in parts:
        if n == top or par in under:
            under.add(n)
            if n != top:
                pv = tuple(c * f for c in pv)
            boxes = [tuple(c * f for c in b[:6]) + (b[6],) for b in boxes]
        out.append((n, par, pv, r, boxes, m))
    return out


KOBOLD = enlarge(KOBOLD, 'head', 1.15)
KOBOLD = w.settle(KOBOLD, ('right_foot', 'left_foot'))


# ── The skin: clean, glossy reptile scales ───────────────────────────────────

def tone_side(s, i, fw, belly_from=0.62):
    """How far toward the belly's colour a column of a side face is: none behind, up toward the front."""
    t = i / max(1, fw - 1)
    if s == 'left':
        t = 1 - t                                                 # 0 at the back .. 1 at the front
    return w.clamp((t - belly_from) / (1 - belly_from)) if belly_from < 1 else 0.0


def scale_rows(p, rect, colour_at, cw=4, ch=3, light=1.0, gloss=True, fall=0.12):
    """Overlapping scales in clean, regular rows, offset by half a scale each row: every scale lit
    along its upper edge, shading down to a dark rim at its lower edge (the scale below it tucked
    under), a narrow shadow at its side, and a glint of light near its upper corner. One colour for
    the whole face (`colour_at(i, j)`, which may shade smoothly across it); no noise, no blotches."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        row = j // ch
        off = (cw // 2) if row % 2 else 0
        cy = j % ch
        for i in range(fw):
            cx = (i + off) % cw
            base = colour_at(i, j)
            kk = 1.1 - 0.2 * cy / max(1, ch - 1)
            c = base
            if cy == 0:
                c = mix(base, (255, 255, 240), 0.06)
            if cy == ch - 1:
                kk = 0.76 if 0 < cx < cw - 1 else 0.68
            elif cx == cw - 1:
                kk *= 0.84
            elif cx == 0 and cy == ch - 2:
                kk *= 0.92
            if gloss and cy == 0 and cx == 1 and cw >= 3:
                c = mix(base, GLOSS, 0.3)
                kk = 1.12
            kk *= light * (1 - fall * j / max(1, fh - 1))
            p.px((fx + i, fy + j), [v * kk for v in c])


def scutes(p, rect, light=1.0, cols=2, ch=3, base=BELLY, edge=BELLY_EDGE):
    """The belly's broad scutes: wide plates in rows, `cols` across, lit above, a seam beneath each."""
    fx, fy, fw, fh = rect
    cw = max(2, fw // cols)
    for j in range(fh):
        cy = j % ch
        for i in range(fw):
            cx = i % cw
            c = base
            kk = 1.08 - 0.16 * cy / max(1, ch - 1)
            if cy == ch - 1:
                c, kk = edge, 1.0
            elif cx == cw - 1 and i != fw - 1:
                c, kk = mix(base, edge, 0.6), 1.0
            if cy == 0 and cx == 1:
                c = mix(base, (255, 252, 236), 0.4)
            p.px((fx + i, fy + j), [v * kk * light for v in c])


def osteoderms(p, rect, light=1.0, pw=4, ph=4, base=PLATE, between=None):
    """The back's bony plates in rows: raised ovals, a lit ridge down each one's middle, a dark rim."""
    fx, fy, fw, fh = rect
    between = between or k(DORSAL, 0.7)
    for j in range(fh):
        row = j // ph
        off = (pw // 2) if row % 2 else 0
        cy = j % ph
        for i in range(fw):
            cx = (i + off) % pw
            dx = (cx - (pw - 1) / 2) / (pw / 2)
            dy = (cy - (ph - 1) / 2) / (ph / 2)
            d = dx * dx + dy * dy
            if d > 1.05:
                c = between
            else:
                c = k(base, 1.2 - 0.45 * max(0, dy) - 0.15 * abs(dx))
                if abs(dx) < 0.3 and dy < 0.2:
                    c = mix(c, GLOSS, 0.25)                       # its keel, catching the light
                if dy > 0.55:
                    c = k(base, 0.6)
            p.px((fx + i, fy + j), [v * light for v in c])


def scaly(p, f, back='scales', belly_faces=('bottom',), inner_pale=0.35, cw=4, ch=3, side_belly=1.0, faces=None):
    """A box in scales: olive on its outer faces, paling on its inner side, scutes beneath."""
    for s in faces or f:
        rect = f[s]
        fx, fy, fw, fh = rect
        lv = w.LIGHT[s]
        if s in belly_faces and s == 'front' and fw >= 10:          # the scutes down its middle, olive scales at its flanks
            scale_rows(p, rect, lambda i, j: DORSAL, cw=cw, ch=ch, light=lv)
            m = max(2, fw // 5)
            scutes(p, (fx + m, fy, fw - 2 * m, fh), light=lv, cols=2)
            for j in range(fh):                                   # the scales lapping onto it at its edges
                p.px((fx + m, fy + j), k(mix(DORSAL, BELLY, 0.5), lv * 0.8))
                p.px((fx + fw - m - 1, fy + j), k(mix(DORSAL, BELLY, 0.5), lv * 0.8))
        elif s in belly_faces:
            scutes(p, rect, light=lv)
        elif s == 'back' and back == 'plates':
            osteoderms(p, rect, light=lv)
        else:
            if s == 'left' and inner_pale:
                col = lambda i, j: mix(DORSAL, BELLY, inner_pale)
            elif s in ('right', 'left') and side_belly < 1:
                col = lambda i, j, s=s, fw=fw: mix(DORSAL, BELLY, tone_side(s, i, fw, side_belly) ** 1.2)
            else:
                col = lambda i, j: DORSAL
            scale_rows(p, rect, col, cw=cw, ch=ch, light=lv)


def clawed(p, f, length=0.5):
    for s in w.SIDES + ('bottom',):
        fx, fy, fw, fh = f[s]
        for i, j, xy in w.each(p, f[s]):
            if j >= fh * (1 - length) or s == 'bottom':
                c = CLAW if j > fh * (1 - length) + 1 else (110, 104, 92)
                p.px(xy, k(c, w.LIGHT[s]))
    for s in w.SIDES:
        w.point(p, f[s], down=True, keep=0.3, sharp=0.55)


def paint_kobold(p, style, f, dims):
    S = w.SIDES

    if style in ('thigh', 'upperarm', 'shin', 'ankle', 'forearm'):
        scaly(p, f, back='plates' if style == 'thigh' else 'scales')

    if style in ('foot', 'hand'):
        scaly(p, f, cw=3, ch=2)
        w.flat(p, f, k(BELLY_EDGE, 0.8), faces=('bottom',))
    if style in ('toe', 'finger'):
        scaly(p, f, cw=3, ch=2)
        clawed(p, f, length=0.45 if style == 'toe' else 0.4)
    if style == 'dewclaw':
        scaly(p, f, cw=3, ch=2)
        clawed(p, f, length=0.7)

    if style == 'hips':
        scaly(p, f, back='plates', belly_faces=('front', 'bottom'), side_belly=0.6)
    if style == 'belly':                                          # the belly: broad scutes before, olive behind
        scaly(p, f, back='plates', belly_faces=('front', 'bottom'), side_belly=0.55)
    if style == 'chest':                                          # the chest: scutes before, rows of plates behind
        scaly(p, f, back='plates', belly_faces=('front',), side_belly=0.6)
    if style == 'neck':
        scaly(p, f, back='plates', belly_faces=('front', 'bottom'), cw=3, ch=2, side_belly=0.55)

    if style in ('tail1', 'tail2', 'tail3', 'tail4'):             # the tail: plates along its top, scutes under it
        cw = 4 if style in ('tail1', 'tail2') else 3
        for s in f:
            rect = f[s]
            lv = w.LIGHT[s]
            if s == 'top':
                osteoderms(p, rect, light=lv, pw=4, ph=4)
            elif s == 'bottom':
                scutes(p, rect, light=lv, cols=1, ch=3)
            elif s in ('right', 'left'):                          # its sides: olive above, paling to cream below
                fx, fy, fw, fh = rect
                scale_rows(p, rect, lambda i, j, fh=fh: mix(DORSAL, BELLY, w.clamp((j / max(1, fh - 1) - 0.55) / 0.45) ** 1.3),
                           cw=cw, ch=3, light=lv, fall=0)
            else:
                scale_rows(p, rect, lambda i, j: DORSAL, cw=cw, ch=3, light=lv)

    if style in ('spine', 'spine_small'):                         # the crest's spines: horn-dark, pale at their points
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                t = 1 - j / max(1, fh - 1)
                p.px(xy, k(mix(k(DORSAL, 0.55), (196, 186, 140), t ** 1.5), w.LIGHT[s]))
        for s in ('right', 'left'):                               # cut to a back-swept point
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                u = i / max(1, fw - 1)
                if s == 'left':
                    u = 1 - u                                     # 0 at its back .. 1 at its front
                if j < fh * u * 0.95:
                    p.clear(xy)
        w.cut_all(p, f, ('top', 'front', 'back'))

    if style == 'cranium':                                        # the head: fine scales; a crest of plates down the crown
        scaly(p, f, cw=3, ch=2, inner_pale=0, belly_faces=('bottom',))
        fx, fy, fw, fh = f['top']
        osteoderms(p, (fx + fw // 2 - 2, fy, 4, fh), light=w.LIGHT['top'], pw=4, ph=3)
        fx, fy, fw, fh = f['front']
        scale_rows(p, f['front'], lambda i, j: k(DORSAL, 0.85), cw=3, ch=2, light=w.LIGHT['front'])

    if style in ('snout', 'snouttip'):                            # the long snout: fine scales above, paling to its lip, its lip line dark
        for s in f:
            rect = f[s]
            fx, fy, fw, fh = rect
            lv = w.LIGHT[s]
            if s == 'bottom':                                     # its palate
                w.flat(p, {s: rect}, MOUTH, noise=2)
            elif s in ('right', 'left', 'front'):
                scale_rows(p, rect, lambda i, j, fh=fh: mix(DORSAL, BELLY, w.clamp((j / max(1, fh - 1) - 0.5) / 0.5) * 0.55),
                           cw=3, ch=2, light=lv, fall=0)
                w.hline(p, rect, fh - 1, k((30, 26, 22), lv))     # the lip line
            else:
                fx, fy, fw, fh = rect                             # its top: rows of small bony scutes across it
                for i, j, xy in w.each(p, rect):
                    cy = j % 3
                    cx = (i + (j // 3) % 2) % 3
                    c = k(DORSAL, 1.1 if cy == 0 else (0.62 if cy == 2 else 0.95))
                    if cx == 2 and cy < 2:
                        c = k(c, 0.82)
                    if cy == 0 and cx == 0:
                        c = mix(c, GLOSS, 0.35)
                    p.px(xy, [v * lv for v in c])

    if style == 'nostrils':                                       # the raised nostrils at its tip
        scaly(p, f, cw=3, ch=2, inner_pale=0)
        fx, fy, fw, fh = f['top']
        for x in (1, fw - 2):
            for j in range(1, fh - 1):
                p.px((fx + x, fy + j), (16, 14, 12))

    if style in ('teeth_down', 'teeth_up'):                       # teeth all along its jaw line, interlocking
        w.cut_all(p, f, ('top', 'bottom', 'front', 'back'))
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                u = i if s == 'left' else fw - 1 - i
                ph = (u + (1 if style == 'teeth_up' else 0)) % 3
                tip = (j >= fh - 1) if style == 'teeth_up' else (j == 0)
                if ph == 2 or (ph == 1 and tip):
                    p.clear(xy)
                else:
                    p.px(xy, k(mix(TOOTH, (200, 186, 150), 0.4 * (j / max(1, fh - 1) if style == 'teeth_down' else 1 - j / max(1, fh - 1))), w.LIGHT[s]))
            p.clear((fx, fy)) if False else None

    if style in ('jaw', 'jawtip'):                                # the lower jaw: olive on its sides, cream beneath; its mouth within
        for s in f:
            rect = f[s]
            fx, fy, fw, fh = rect
            lv = w.LIGHT[s]
            if s == 'top':                                        # within: a dark tongue, ringed with teeth
                for i, j, xy in w.each(p, rect):
                    edge = i in (0, fw - 1) or j >= fh - 1
                    p.px(xy, (TOOTH if (i + j) % 2 == 0 else (60, 30, 30)) if edge else (86, 40, 44))
            elif s == 'bottom':
                scutes(p, rect, light=lv, cols=2, ch=2)
            else:
                scale_rows(p, rect, lambda i, j, fh=fh: mix(DORSAL, BELLY, 0.25 + 0.5 * j / max(1, fh - 1)), cw=3, ch=2, light=lv, fall=0)
                w.hline(p, rect, 0, k((30, 26, 22), lv))

    if style == 'throat':
        for s in f:
            scutes(p, f[s], light=w.LIGHT[s], cols=1, ch=2)

    if style == 'jowl':                                           # the swell of muscle behind its jaw, in larger scales
        scaly(p, f, cw=4, ch=3, inner_pale=0)

    if style == 'brow':                                           # the bony brow ridge over its eye
        for s in f:
            lv = w.LIGHT[s]
            if s == 'bottom':
                w.flat(p, {s: f[s]}, k(DORSAL, 0.35), noise=1)
            else:
                osteoderms(p, f[s], light=lv, pw=3, ph=3)

    if style == 'eye':                                            # an amber eye, a black slit pupil, set in a dark lid
        w.flat(p, f, k(DORSAL, 0.4), noise=1)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]                                 # 3x2
            for i, j, xy in w.each(p, f[s]):
                p.lit(xy, mix((255, 226, 120), EYE, j / max(1, fh - 1)))
            px_ = fx + fw // 2
            for j in range(fh):
                p.px((px_, fy + j), (12, 8, 6))
                p.glow.putpixel((px_, fy + j), (0, 0, 0, 0))
            p.lit((fx + (0 if s == 'left' else fw - 1), fy), (255, 250, 230))

    if style == 'belt':
        w.leather(p, f, LEATHER, LEATHER_DEEP, stitch=(190, 150, 100))
    if style == 'buckle':
        w.iron(p, f, IRON, rivets=0, rim=False, dents=0)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            if 0 < i < fw - 1 and 0 < j < fh - 1:
                p.px(xy, LEATHER_DEEP)
    if style == 'flap':
        w.leather(p, f, LEATHER, LEATHER_DEEP, stitch=(190, 150, 100))
        for s in ('front', 'back'):
            w.ragged(p, f[s], teeth=2, depth=1)
    if style == 'strap':
        w.leather(p, f, LEATHER, LEATHER_DEEP)
        fx, fy, fw, fh = f['front']
        for j in range(3, fh, 7):                                 # brass studs along it
            p.px((fx + fw // 2, fy + j), (210, 170, 80))
    if style in ('satchel', 'satchelflap'):
        w.leather(p, f, k(LEATHER, 0.9), LEATHER_DEEP, stitch=(190, 150, 100))
    if style == 'ore_copper':
        w.hide(p, f, (96, 88, 84), (60, 56, 54), mottle=0.4)
        for s in f:
            fx, fy, fw, fh = f[s]
            p.px((fx + 1, fy + 1), COPPER)
            p.px((fx + fw - 1, fy + fh - 1), k(COPPER, 1.2))
    if style == 'ore_iron':
        w.hide(p, f, (100, 96, 92), (66, 62, 60), mottle=0.4)
        for s in f:
            fx, fy, fw, fh = f[s]
            p.px((fx + 1, fy), (216, 176, 150))
    if style == 'ore_jade':
        for s in f:
            for i, j, xy in w.each(p, f[s]):
                p.lit(xy, k(JADE, 0.8 + 0.3 * ((i + j) % 2)), glow=0.7)

    if style in ('lanterncap', 'lanterncord'):
        w.iron(p, f, (64, 62, 66), rivets=0, dents=0.1)
    if style == 'lanterncage':                                    # an iron cage, its panes of horn glowing
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if s in ('top', 'bottom') or i in (0, fw - 1) or j in (0, fh - 1) or i == fw // 2:
                    p.px(xy, k((54, 52, 56), w.LIGHT[s]))
                else:
                    p.lit(xy, mix((255, 210, 120), (255, 150, 60), j / max(1, fh - 1)), glow=0.9)
    if style == 'lanternfire':
        for s in f:
            for i, j, xy in w.each(p, f[s]):
                p.lit(xy, FLAME)

    if style == 'bracer':
        w.leather(p, f, LEATHER, LEATHER_DEEP, stitch=(190, 150, 100))
        for s in S:
            fx, fy, fw, fh = f[s]
            for j in range(1, fh - 1, 2):                         # laced
                p.px((fx + fw // 2, fy + j), RAWHIDE)

    if style in ('pauldron', 'pauldron2', 'tasset'):              # scrap iron, rust gathering at its edges, riveted along its rim
        w.iron(p, f, IRON, rust=0.35, rivets=0, dents=0.3, rustc=RUST)
        for sd in S:
            fx, fy, fw, fh = f[sd]
            for i in range(1, fw - 1, 4):
                w.rivet(p, fx + i, fy + 1, IRON)

    if style == 'haft':
        w.wood(p, f, WOOD, along='y')
        for s in S:
            fx, fy, fw, fh = f[s]
            for j in range(int(fh * 0.75), fh - 2):
                if j % 2 == 0:
                    w.hline(p, f[s], j, k(RAWHIDE, 0.7 * w.LIGHT[s]))
    if style == 'lashing':
        w.rope(p, f, RAWHIDE)
    if style in ('pickpoint', 'pickblade'):
        w.iron(p, f, IRON, rust=0.15, rivets=0, dents=0.2, rustc=RUST)
        for s in ('right', 'left', 'top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                u = i / max(1, fw - 1)
                end = (u < 0.2) if (s == 'right') == (style == 'pickpoint') else (u > 0.8)
                if end:
                    p.px(xy, k((200, 200, 206), w.LIGHT[s]))


if __name__ == '__main__':
    p = Painter(43)
    slots = paint(KOBOLD, p, paint_kobold)
    p.save('kobold')
    w.emit(KOBOLD, slots, w.JAVA, 'kobold')
    w.report('kobold', KOBOLD, SIZE, slots)
