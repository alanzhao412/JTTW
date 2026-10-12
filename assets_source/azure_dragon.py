"""The Azure Dragon, the Dragon's true form: a Chinese dragon, long and serpentine, azure-scaled
with a golden belly; a long head with a heavy snout, flared nostrils and long barbels trailing,
branching horns swept back, a white mane down its neck, the jaw open on its fangs and its eyes
burning gold; spines along its back, four short legs with taloned feet held under it, a tufted
tail. Its body is a chain of segments, each turned a little from the last, so it undulates.
"""
import math
from beastgen import Painter, paint, emit

SEGMENTS = 11
THICK = [10, 11, 11, 10.5, 10, 9, 8, 7, 6, 4.6, 3.2]
LENGTH = 10
# its coil: each segment's pitch (up is negative) and turn, as it lies in the air; the chain's own
# turns are the differences between these
PITCH = [-0.35, -0.85, -0.45, 0.45, 1.0, 0.55, -0.45, -1.0, -0.55, 0.3, 0.7]
YAW = [0.1, 0.25, 0.22, 0.0, -0.22, -0.32, -0.2, 0.0, 0.22, 0.3, 0.2]
HOVER = 13.0   # the lowest of it, this many pixels up from the ground

PARTS = [
    ('head', 'root', (0, 0, -6), (-0.12, -0.15, 0), [
        (-4, -3.5, -6, 8, 7, 7, 'skull'),
        (-3, -1.2, -14.5, 6, 4, 8.5, 'snout'),       # the snout, long and heavy
        (-3.6, -2, -15.5, 7.2, 3, 2.2, 'nose'),      # the nose, flared
        (-4.6, -4.6, -7.2, 3.2, 2, 5, 'brow'),       # the brow ridges, over the eyes
        (1.4, -4.6, -7.2, 3.2, 2, 5, 'brow'),
        (-2.7, 2.8, -14, 0.8, 2.4, 0.8, 'fang'),     # the upper fangs
        (1.9, 2.8, -14, 0.8, 2.4, 0.8, 'fang'),
        (-3, -5.5, -3, 6, 2.5, 7, 'mane'),           # the mane, down its neck from the crown
        (-4.6, -1, -2, 1.5, 6, 6, 'mane'),           # and at its cheeks
        (3.1, -1, -2, 1.5, 6, 6, 'mane'),
    ], False),
    ('jaw', 'head', (0, 2.6, -5.5), (0.5, 0, 0), [
        (-2.7, 0, -8.5, 5.4, 1.8, 8.5, 'jaw'),
        (-2.2, -1.6, -8, 0.7, 1.6, 0.7, 'fang'),
        (1.5, -1.6, -8, 0.7, 1.6, 0.7, 'fang'),
        (-1.8, 0.3, -7, 3.6, 0.6, 6, 'tongue'),
    ], False),
]
# the horns, branching, swept back and up
for side, s in (('right', -1), ('left', 1)):
    PARTS += [
        (side + '_horn', 'head', (s * 2.3, -4.5, -2.5), (0.55, s * 0.28, 0), [(-0.8, -0.8, 0, 1.6, 1.6, 7, 'horn')], s > 0),
        (side + '_horn_tip', side + '_horn', (0, 0, 7), (-0.45, 0, 0), [(-0.6, -0.6, 0, 1.2, 1.2, 5, 'horntip')], s > 0),
        (side + '_horn_branch', side + '_horn', (0, 0, 3.5), (0.9, s * 0.4, 0), [(-0.5, -0.5, 0, 1, 1, 3.5, 'horntip')], s > 0),
    ]
# the barbels, long, from beside the nose, trailing back in two lengths
for side, s in (('right', -1), ('left', 1)):
    PARTS += [
        (side + '_barbel', 'head', (s * 3.2, 2.4, -13.5), (-0.1, s * 0.75, 0), [(-0.3, -0.3, 0, 0.6, 0.6, 9, 'barbel')], s > 0),
        (side + '_barbel_end', side + '_barbel', (0, 0, 9), (0.35, -s * 0.3, 0), [(-0.25, -0.25, 0, 0.5, 0.5, 9, 'barbel')], s > 0),
    ]
# the body: a chain of segments back from the neck, each a child of the last
# where its coil puts it: follow the chain in the coil's own angles, find its lowest point, and set
# the whole of it so that hangs HOVER above the ground
pts, at = [], [0.0, 0.0, 0.0]
for i in range(SEGMENTS):
    a, b = YAW[i], PITCH[i]
    d = (math.cos(b) * math.sin(a), -math.sin(b), math.cos(b) * math.cos(a))
    for k in range(4):
        pts.append((at[1] + d[1] * LENGTH * k / 3, THICK[i]))
    at = [at[j] + d[j] * LENGTH for j in range(3)]
lowest = max(y + t / 2 for y, t in pts)
ROOT_Y = 24 - HOVER - lowest
parent = 'root'
for i in range(SEGMENTS):
    t = THICK[i]
    name = 'seg%d' % i
    pivot = (0, ROOT_Y, -5) if i == 0 else (0, 0, LENGTH)
    turn = (PITCH[i] - (PITCH[i - 1] if i else 0), YAW[i] - (YAW[i - 1] if i else 0), 0)
    boxes = [(-t / 2, -t / 2, 0, t, t, LENGTH, 'body'),
             (-t / 2 + 0.6, t / 2 - 0.4, 0, t - 1.2, 0.8, LENGTH, 'belly')]
    if i < SEGMENTS - 1:          # spines along its back, two to a segment
        sp = max(1.2, t * 0.32)
        boxes += [(-0.5, -t / 2 - sp, 1.5, 1, sp, 3, 'spine'), (-0.5, -t / 2 - sp, 6.5, 1, sp, 3, 'spine')]
    if i < 3:                     # the mane runs on down the neck
        boxes += [(-t / 2 + 1, -t / 2 - 1.2, 0, t - 2, 1.5, LENGTH, 'mane')]
    PARTS.append((name, parent, pivot, turn, boxes, False))
    parent = name
PARTS[0] = (PARTS[0][0], PARTS[0][1], (0, ROOT_Y + 0.5, -6)) + PARTS[0][3:]   # the head, on the front of the neck
PARTS.append(('tail_tuft', parent, (0, 0, LENGTH), (0, 0, 0), [
    (-2.5, -3.5, 0, 5, 7, 6, 'mane'), (-1.5, -2, 5, 3, 4, 5, 'mane')], False))
# four legs, short, held under it, taloned
for seg, front in ((1, True), (6, False)):
    t = THICK[seg]
    for side, s in (('right', -1), ('left', 1)):
        leg = '%s_%s_leg' % (side, 'front' if front else 'hind')
        PARTS += [
            (leg, 'seg%d' % seg, (s * (t / 2 - 0.5), t / 2 - 1.5, 5), (0.35, 0, s * -0.45), [
                (-1.6, 0, -1.6, 3.2, 5.5, 3.2, 'body')], s > 0),
            (leg + '_lower', leg, (0, 5.5, 0), (-0.8, 0, s * 0.55), [
                (-1.3, 0, -1.3, 2.6, 4.5, 2.6, 'body'), (-1.6, 4, -2.2, 3.2, 1.2, 3.2, 'foot')], s > 0),
        ]
        for k, cx in enumerate((-1.1, 0, 1.1)):
            PARTS.append(('%s_claw%d' % (leg, k), leg + '_lower', (cx, 5, -2.2), (0.8, 0, 0),
                          [(-0.25, -0.25, -1.4, 0.5, 0.5, 1.4, 'talon')], s > 0))

AZURE = (44, 118, 156)
DEEP = (24, 70, 100)
GOLD = (226, 184, 92)
MANE = (238, 236, 226)
HORN = (232, 214, 168)
EYE = (255, 206, 60)
DARK = (16, 22, 30)
MOUTH = (110, 30, 38)
IVORY = (238, 230, 206)


def dragon_eye(p, fx, fy, away):
    """A dragon's eye with a positive canthal tilt, from its inner corner at the snout end (fx, fy),
    running `away` toward the back of the head: long and narrow, low at the snout and rising to its
    outer corner, under a heavy brow brought down hard over the inner corner on the same angle; the
    iris burning gold about a dark slit (it glows)."""
    X = lambda i: fx + i * away
    L = 9
    base = fy + 5
    for i in range(-2, L + 2):                 # the brow, dark and heavy, low over the inner corner
        for k in (0, 1):
            p.px((X(i), base - 3 + k - i // 3), DARK)
    for i in range(L):
        top = base - i // 3                    # rising toward the outer corner
        h = 1 if i < 2 or i > L - 3 else 2     # narrowest at its corners
        for j in range(h + 1):
            p.px((X(i), top + j), EYE if j else [v * 0.7 for v in EYE], glow=True)
        p.px((X(i), top + h + 1), DARK)
    for j in range(3):                         # the pupil: a narrow slit
        y = base - 4 // 3 + j
        p.px((X(4), y), (8, 6, 4))
        p.glow.putpixel((int(X(4)), int(y)), (0, 0, 0, 0))
    for i in range(L, L + 3):                  # its outer corner drawn out sharp, up and back
        p.px((X(i), base - i // 3 + 1), DARK)
    for i in (-1, -2):                         # and the inner corner, a hard notch toward the snout
        p.px((X(i), base + 1), DARK)


def paint_box(p, style, f, dims):
    if style in ('body', 'skull', 'snout', 'brow'):
        for k in ('top', 'right', 'left', 'front', 'back'):
            p.scales(f[k], AZURE, cell=4,
                     light=1.05 if k == 'top' else 0.92 if k in ('right', 'left') else 0.88)
        p.face(f['bottom'], GOLD, 0.82, fall=0.1)
    if style == 'body':
        # a darker line along its spine, and its sides fading darker toward the belly
        for k in ('right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for j in range(fh * 2 // 3, fh):
                    c = p.img.getpixel((fx + i, fy + j))
                    p.px((fx + i, fy + j), [v * 0.85 for v in c[:3]])
    if style == 'belly':
        for k, r in f.items():
            p.face(r, GOLD, 1.0 if k != 'bottom' else 0.9, noise=4)
            fx, fy, fw, fh = r
            for i in range(0, fw, 6):               # the belly's plates, ridged across
                for j in range(fh):
                    p.px((fx + i, fy + j), [v * 0.7 for v in GOLD])
    if style == 'skull':
        fx, fy, fw, fh = f['right']                 # its eyes on the sides of its head, burning gold
        dragon_eye(p, fx + fw - 3, fy - 1, -1)
        fx, fy, fw, fh = f['left']
        dragon_eye(p, fx + 2, fy - 1, 1)
    if style == 'nose':
        p.shaded(f, AZURE)
        fx, fy, fw, fh = f['front']                 # flared nostrils
        for cx in (fx + 3, fx + fw - 5):
            for i in range(2):
                for j in range(2):
                    p.px((cx + i, fy + 2 + j), DARK)
    if style == 'snout':
        fx, fy, fw, fh = f['bottom']
        p.face((fx, fy, fw, fh), MOUTH, 0.7, noise=3)   # the roof of the open mouth
        for k in ('right', 'left'):                 # the lip, dark, along the snout
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), DARK)
                p.px((fx + i, fy + fh - 2), DARK)
    if style == 'brow':
        p.face(f['bottom'], DARK, 1.0, noise=3)
    if style == 'jaw':
        p.shaded(f, AZURE, belly=GOLD)
        p.face(f['top'], MOUTH, 0.8, noise=4)
    if style == 'tongue':
        for r in f.values():
            p.face(r, (196, 66, 82), 1.0, noise=6, fall=0.2)
    if style == 'fang':
        for r in f.values():
            p.face(r, IVORY, 1.0, noise=3)
    if style == 'talon':
        for k, r in f.items():
            p.face(r, (40, 36, 34) if k == 'front' else IVORY, 0.95, noise=3)
    if style == 'foot':
        for k in ('top', 'right', 'left', 'front', 'back'):
            p.scales(f[k], AZURE, cell=4, light=0.95)
        p.face(f['bottom'], DEEP, 0.8)
    if style in ('horn', 'horntip'):
        for k, r in f.items():
            p.gradient(r, HORN, (250, 236, 196) if style == 'horn' else (180, 150, 90), horizontal=k in ('top', 'bottom'))
        for k in ('right', 'left', 'top'):          # rings round the horn
            fx, fy, fw, fh = f[k]
            for i in range(0, fw, 3):
                for j in range(fh):
                    p.px((fx + i, fy + j), [v * 0.8 for v in HORN])
    if style == 'mane':
        for k, r in f.items():
            p.face(r, MANE, 1.0 if k == 'top' else 0.9, noise=8, fall=0.25)
            fx, fy, fw, fh = r                      # strands
            for i in range(0, fw, 2):
                for j in range(0, fh, 1):
                    if p.rnd.random() < 0.35:
                        p.px((fx + i, fy + j), [v * 0.78 for v in MANE])
    if style == 'spine':
        for k, r in f.items():
            p.gradient(r, (250, 214, 120), DEEP)
    if style == 'barbel':
        for r in f.values():
            p.face(r, GOLD, 1.0, noise=6)


if __name__ == '__main__':
    painter = Painter(31)
    slots = paint(PARTS, painter, paint_box)
    painter.save('azure_dragon')
    emit(PARTS, slots, 'src/main/java/com/jttw/engine/client/renderer/AzureDragonRenderer.java')
    print('azure dragon:', len(PARTS), 'parts,', len(slots), 'boxes')
