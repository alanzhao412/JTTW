"""The Vermilion Bird (Zhuque), the Phoenix's true form: a vast bird of flame. Its wings are broad
and raised, each built as a real wing is: a rounded leading edge, rows of coverts stepping back
over it, the secondaries hanging in a row along its trailing edge and the long primaries fanned out
from its tip. A ruff of feathers flares about its neck; a long crest of flame-feathers sweeps back
off its head, over a hooked beak and eyes burning gold; feathered thighs over taloned feet; and a
train of long plumes, each curling to an eye, streams behind. Vermilion running to gold and white-hot
at every edge; its wings, crest and plumes glow.
"""
import math
from beastgen import Painter, paint, emit

PARTS = [
    ('body', 'root', (0, -2, 0), (-0.2, 0, 0), [
        # sculpted, front to back: a deep rounded breast, the shoulders broadest, a waist drawn in,
        # the rump tapering into the tail
        (-4, -3.5, -10, 8, 8, 2, 'breast'),
        (-5, -4.5, -8, 10, 10, 3, 'breast'),
        (-6, -5, -5, 12, 10.5, 5, 'plumage'),       # the shoulders
        (-5.5, -4.5, 0, 11, 9, 4, 'plumage'),
        (-4.5, -4, 4, 9, 8, 3, 'plumage'),          # the waist
        (-3.5, -3.5, 7, 7, 6.5, 3, 'plumage'),      # the rump
        (-2.4, -3, 10, 4.8, 4.5, 2, 'plumage'),
        (-3.5, 4.5, -7, 7, 2, 11, 'down'),          # the belly, soft beneath
        (-1.2, -6.2, -6, 2.4, 1.4, 15, 'mantle'),   # a ridge down its back
    ], False),
    ('neck', 'body', (0, -3, -7), (-0.55, 0, 0), [(-3, -3, -6, 6, 6, 7, 'plumage')], False),
    ('head', 'neck', (0, -1, -5.5), (0.75, 0, 0), [
        (-3.2, -3.2, -6, 6.4, 6, 6.5, 'phead'),
        (-3.7, 0.5, -1.5, 1, 3, 4, 'cheek'),         # cheek feathers, swept back behind the eye
        (2.7, 0.5, -1.5, 1, 3, 4, 'cheek'),
        (-1.4, -1, -10.5, 2.8, 2.6, 5, 'beak'),
        (-1, 1, -11.5, 2, 1.6, 1.6, 'beak'),        # its hook
    ], False),
    ('legs', 'body', (0, 5, 3), (0.9, 0, 0), [
        (-3.4, -1, -1.5, 2.8, 4, 3, 'down'), (0.6, -1, -1.5, 2.8, 4, 3, 'down'),   # feathered thighs
        (-2.8, 3, -0.8, 1.6, 5, 1.6, 'talonleg'), (1.2, 3, -0.8, 1.6, 5, 1.6, 'talonleg')], False),
]
# contour feathers: rows of overlapping plates over the back and down the flanks, each lifted a
# little off the body and pointing back, so the body has depth and the light catches every row
for row, (z, w) in enumerate(((-6, 10), (-3, 11), (0, 10.5), (3, 9.5), (6, 8))):
    for k in range(3):                                   # over the back
        x = (k - 1) * w / 3.2
        PARTS.append(('mantle%d_%d' % (row, k), 'body', (x, -5.2 + abs(k - 1) * 0.8, z), (-0.28, 0, (k - 1) * 0.35),
                      [(-w / 6 - 0.3, -0.35, 0, w / 3 + 0.6, 0.7, 4.2, 'contour')], False))
    for side, sgn in (('r', -1), ('l', 1)):              # down the flanks, two rows deep
        for lvl, y in enumerate((-2.2, 1.2)):
            PARTS.append(('flank%d_%s%d' % (row, side, lvl), 'body', (sgn * (w / 2 + 0.1), y, z),
                          (-0.15, sgn * 0.22, sgn * (0.25 + lvl * 0.15)),
                          [(-0.35 if sgn < 0 else -0.35, -1.6, 0, 0.7, 3.4, 4.4, 'contour')], False))
# the breast: scalloped rows of feathers down its front, each overlapping the one below
for row, (y, w) in enumerate(((-2.5, 7), (0, 8), (2.5, 7.5), (5, 6))):
    for k in range(3):
        x = (k - 1) * w / 3.1
        PARTS.append(('breastrow%d_%d' % (row, k), 'body', (x, y, -10.3 + abs(row - 1.5) * 0.5),
                      (0.55, (k - 1) * 0.25, 0), [(-w / 6 - 0.2, -0.3, -3.2, w / 3 + 0.4, 0.6, 3.4, 'scallop')], False))
# at each wing's root, a tuft of feathers over the shoulder
for side, sgn in (('r', -1), ('l', 1)):
    for k in range(3):
        PARTS.append(('shoulder_%s%d' % (side, k), 'body', (sgn * (4.5 + k * 0.6), -4.8 + k * 0.6, -4 + k * 2),
                      (-0.35, sgn * 0.5, sgn * 0.7), [(-1.2, -0.3, 0, 2.4, 0.6, 5 + k, 'contour')], False))
# flame plumes rising off its back, curling back
for k, (z, h) in enumerate(((-4, 7), (0, 9), (4, 7), (7, 5))):
    PARTS += [
        ('backflame%d' % k, 'body', (0, -6, z), (-1.15, 0, 0), [(-0.7, -0.4, 0, 1.4, 0.8, h, 'flame')], False),
        ('backflame%d_end' % k, 'backflame%d' % k, (0, 0, h), (0.9, 0, 0), [(-0.5, -0.35, 0, 1, 0.7, h * 0.6, 'flametip')], False),
    ]
# long feathers hanging from its flanks and belly, drawn back
for side, sgn in (('r', -1), ('l', 1)):
    for k in range(4):
        PARTS.append(('hang_%s%d' % (side, k), 'body', (sgn * (3.2 - k * 0.3), 5, -3 + k * 3),
                      (0.55 + k * 0.05, 0, sgn * 0.12), [(-1.1, -0.3, 0, 2.2, 0.6, 6 + k, 'contour')], False))
# the ruff: feathers flaring out and back all round the base of the neck
for k in range(10):
    a = 2 * math.pi * k / 10
    x, y = 3.4 * math.sin(a), 3.4 * -math.cos(a)
    PARTS.append(('ruff%d' % k, 'neck', (x, y, -1), (0.5 * math.cos(a) + 0.35, 0.5 * math.sin(a), 0),
                  [(-1.2, -0.3, 0, 2.4, 0.6, 6, 'ruff')], False))
# the crest: long flame-feathers swept back off the crown, each curling up at its end
for k, (x, rot, ln) in enumerate(((-2, 0.75, 7), (-1, 0.55, 9), (0, 0.4, 11), (1, 0.55, 9), (2, 0.75, 7),
                                  (-0.5, 0.95, 6), (0.5, 0.95, 6))):
    PARTS += [
        ('crest%d' % k, 'head', (x, -3, -2), (rot, x / 6, 0), [(-0.5, -0.4, 0, 1, 0.8, ln, 'flame')], False),
        ('crest%d_end' % k, 'crest%d' % k, (0, 0, ln), (-0.6, 0, 0), [(-0.4, -0.35, 0, 0.8, 0.7, ln * 0.5, 'flametip')], False),
    ]
# the wings
SEG = [12, 12, 10]
for side, s in (('right', -1), ('left', 1)):
    m = s > 0
    names = [side + '_wing', side + '_wing_mid', side + '_wing_tip']
    parents = ['body', names[0], names[1]]
    pivots = [(s * 4.5, -3.5, -3), (s * SEG[0], 0, 0), (s * SEG[1], 0, 0)]
    rots = [(-0.2, s * 0.3, s * -0.72), (0, s * 0.12, s * 0.22), (0, s * 0.12, s * 0.3)]
    for g in range(3):
        L = SEG[g]
        x0 = 0 if m else -L
        boxes = [
            (x0, -1.6, -5, L, 2.6, 3, 'leading'),        # the leading edge, rounded and thick
            (x0, -0.9, -2.2, L, 1.8, 5, 'covert'),        # the lesser coverts
            (x0, -0.4, 2.6, L, 1.2, 4, 'covert2'),        # the greater coverts, stepping back
        ]
        PARTS.append((names[g], parents[g], pivots[g], rots[g], boxes, m))
        # the secondaries (inner, mid) in a row along the trailing edge; the primaries (tip) fanned out
        count = 5 if g < 2 else 7
        for f in range(count):
            t = (f + 0.5) / count
            x = s * L * t
            if g < 2:
                ln, rot = 11 + g * 1.5, (0.1, s * 0.04 * f, 0)
            else:
                ln, rot = 13 + f * 1.6, (0.05, s * (0.25 + 0.17 * f), 0)
            PARTS.append(('%s_q%d_%d' % (side, g, f), names[g], (x, 0, 6.2 if g < 2 else 4.5), rot,
                          [(-1.1, -0.35, -0.5, 2.2, 0.7, ln, 'quill' if g < 2 else 'primary')], m))
# the train: seven long plumes, each in three lengths curling down to an eye; tail coverts over them
for k in range(7):
    x = (k - 3) * 1.3
    yaw = (k - 3) * 0.12
    PARTS += [
        ('plume%d' % k, 'body', (x, -1, 7.5), (0.12 + abs(k - 3) * 0.03, yaw, 0), [(-1, -0.4, 0, 2, 0.8, 13, 'plume')], False),
        ('plume%d_mid' % k, 'plume%d' % k, (0, 0, 13), (0.1, yaw * 0.5, 0), [(-1.1, -0.4, 0, 2.2, 0.8, 11, 'plume')], False),
        ('plume%d_end' % k, 'plume%d_mid' % k, (0, 0, 11), (0.18, yaw * 0.4, 0), [(-1.6, -0.4, 0, 3.2, 0.8, 9, 'plumetip')], False),
    ]
for k in range(5):
    PARTS.append(('tailcovert%d' % k, 'body', ((k - 2) * 1.6, -2.5, 6.5), (0.2, (k - 2) * 0.18, 0),
                  [(-1.3, -0.4, 0, 2.6, 0.8, 7, 'covert2')], False))
for side, s in (('right', -1), ('left', 1)):
    for k, cx in enumerate((-0.6, 0, 0.6)):
        PARTS.append(('%s_talon%d' % (side, k), 'legs', (s * 2 + cx, 8, 0), (0.6, (k - 1) * 0.4, 0),
                      [(-0.25, -0.25, -1.8, 0.5, 0.5, 1.8, 'talon')], False))

RED = (255, 78, 34)
ORANGE = (255, 150, 42)
GOLD = (255, 214, 80)
HOT = (255, 250, 214)
DARK = (60, 18, 8)
EYE = (255, 236, 110)


def feathers(p, rect, base, tip, glow=False, vertical=False, width=4):
    """Feathers over a face: each a lit vane with a darker shaft, base colour to its tip colour, the
    edge of each vane lit white-hot."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        for j in range(fh):
            t = (j / max(1, fh - 1)) if not vertical else (i / max(1, fw - 1))
            c = [a + (b - a) * t for a, b in zip(base, tip)]
            pos = (i if not vertical else j) % width
            k = 1.05 - 0.15 * abs(pos - width / 2) / (width / 2)
            if pos == width // 2:
                k *= 0.78                                 # the shaft
            if pos == 0:
                c = [a + (b - a) * 0.5 for a, b in zip(c, HOT)]  # the vane's edge, hot
            n = p.rnd.randint(-4, 4)
            p.px((fx + i, fy + j), [v * k + n for v in c], glow)


def paint_box(p, style, f, dims):
    if style in ('plumage', 'phead', 'mantle'):
        for k in ('top', 'right', 'left', 'front', 'back'):
            p.scales(f[k], RED, cell=4, edge=0.8, light=1.15 if k == 'top' else 1.05)
        p.face(f['bottom'], ORANGE, 1.0, noise=5)
    if style == 'scallop':
        for k, r in f.items():
            feathers(p, r, GOLD, ORANGE, vertical=k in ('top', 'bottom'))
            fx, fy, fw, fh = r
            for i in range(fw):                       # the scallop's rim, dark
                p.px((fx + i, fy + fh - 1), DARK)
    if style == 'contour':
        for k, r in f.items():
            feathers(p, r, RED if k in ('top', 'right', 'left') else ORANGE, GOLD, vertical=k in ('top', 'bottom'))
    if style in ('breast', 'down'):
        for k, r in f.items():
            p.scales(r, ORANGE, cell=4, edge=0.85, light=1.1)
    if style == 'phead':
        for k, inward in (('right', -1), ('left', 1)):
            fx, fy, fw, fh = f[k]
            p.eye(fx + (fw - 2 if inward < 0 else 1), fy + 2, inward, EYE, w=7, rise=True, pupil_at=1)
            for i in range(fw):                       # a dark streak back from the eye, a raptor's mask
                p.px((fx + i, fy + 7 + (i % 3 == 0)), DARK)
    if style == 'cheek':
        for k, r in f.items():
            feathers(p, r, GOLD, HOT, glow=True)
    if style == 'beak':
        for k, r in f.items():
            p.gradient(r, (255, 226, 140), (170, 100, 40), horizontal=k in ('top', 'bottom'))
    if style == 'flame':
        for k, r in f.items():
            p.gradient(r, ORANGE, GOLD, glow=True, horizontal=k in ('top', 'bottom'))
    if style == 'flametip':
        for k, r in f.items():
            p.gradient(r, GOLD, HOT, glow=True, horizontal=k in ('top', 'bottom'))
    if style == 'ruff':
        for k, r in f.items():
            feathers(p, r, ORANGE, GOLD, glow=True)
    if style == 'leading':
        for k, r in f.items():
            p.scales(r, RED, cell=4, edge=0.8, light=1.15)
    if style == 'covert':
        for k, r in f.items():
            feathers(p, r, RED, ORANGE, vertical=k in ('top', 'bottom'))
    if style == 'covert2':
        for k, r in f.items():
            feathers(p, r, ORANGE, GOLD, glow=True, vertical=k in ('top', 'bottom'))
    if style == 'quill':
        for k, r in f.items():
            feathers(p, r, ORANGE, HOT, glow=True)
    if style == 'primary':
        for k, r in f.items():
            feathers(p, r, GOLD, HOT, glow=True)
    if style == 'plume':
        for k, r in f.items():
            feathers(p, r, RED, GOLD, glow=True)
    if style == 'plumetip':
        for k, r in f.items():
            feathers(p, r, GOLD, HOT, glow=True)
            fx, fy, fw, fh = r                         # the eye at the plume's end
            if fh > 8 and fw > 4:
                cx, cy = fx + fw // 2, fy + fh - 5
                for i in range(-3, 3):
                    for j in range(-3, 3):
                        d = (i + 0.5) ** 2 + (j + 0.5) ** 2
                        if d < 9:
                            p.px((cx + i, cy + j), (40, 150, 200) if d < 2.5 else (255, 60, 30) if d < 5.5 else GOLD, True)
    if style == 'talonleg':
        for r in f.values():
            p.face(r, (200, 120, 50), 0.95, fall=0.3)
    if style == 'talon':
        for r in f.values():
            p.face(r, (50, 36, 30), 1.0, noise=3)


if __name__ == '__main__':
    painter = Painter(53)
    slots = paint(PARTS, painter, paint_box)
    painter.save('vermilion_bird')
    emit(PARTS, slots, 'src/main/java/com/jttw/engine/client/renderer/VermilionBirdRenderer.java')
    print('vermilion bird:', len(PARTS), 'parts,', len(slots), 'boxes')
