"""The Vermilion Bird (Zhuque), the Phoenix's true form: a vast bird of flame, wings spread wide
and raised, its plumage vermilion running to gold and white-hot at the edges, a high crest of
flame-feathers, a long hooked beak, eyes burning gold, and long streaming tail plumes; its flight
feathers glow.
"""
import math
from beastgen import Painter, paint, emit

PARTS = [
    ('body', 'root', (0, -2, 0), (-0.2, 0, 0), [
        (-5, -5, -8, 10, 10, 16, 'plumage'),
        (-4, 0, -7, 8, 6, 12, 'breast'),           # the breast, puffed
    ], False),
    ('neck', 'body', (0, -3, -7), (-0.55, 0, 0), [(-3, -3, -6, 6, 6, 7, 'plumage')], False),
    ('head', 'neck', (0, -1, -5.5), (0.75, 0, 0), [
        (-3.2, -3.2, -6, 6.4, 6, 6.5, 'phead'),
        (-1.4, -1, -10.5, 2.8, 2.6, 5, 'beak'),
        (-1, 1, -11.5, 2, 1.4, 1.5, 'beak'),        # its hook
    ], False),
    ('legs', 'body', (0, 5, 3), (0.9, 0, 0), [
        (-3, 0, -1, 2, 7, 2, 'talonleg'), (1, 0, -1, 2, 7, 2, 'talonleg')], False),
]
# the crest: flame-feathers swept back and up off the head
for k, (x, rot, ln) in enumerate(((-1.5, 0.7, 6), (0, 0.5, 8), (1.5, 0.7, 6), (-0.8, 0.95, 5), (0.8, 0.95, 5))):
    PARTS.append(('crest%d' % k, 'head', (x, -3, -1.5), (rot, (x / 4), 0), [(-0.5, -0.4, 0, 1, 0.8, ln, 'flame')], False))
# the wings: each in three lengths, raised and spread, flight feathers fanned at the tips
for side, s in (('right', -1), ('left', 1)):
    m = s > 0
    PARTS += [
        (side + '_wing', 'body', (s * 4.5, -3.5, -3), (0, 0, s * -0.45), [
            (0 if m else -12, -1, -4, 12, 2.4, 9, 'covert')], m),
        (side + '_wing_mid', side + '_wing', (s * 12, 0, 0), (0, 0, s * 0.15), [
            (0 if m else -12, -0.8, -3.5, 12, 1.8, 9.5, 'covert')], m),
        (side + '_wing_tip', side + '_wing_mid', (s * 12, 0, 0), (0, 0, s * 0.3), [
            (0 if m else -9, -0.5, -2.5, 9, 1.2, 7, 'primary')], m),
    ]
    for f in range(6):                     # the flight feathers, long, fanned out behind the wing
        x = s * (1.5 + f * 4)
        parent = side + ('_wing' if f < 2 else '_wing_mid' if f < 4 else '_wing_tip')
        lx = x - s * (0 if f < 2 else 12 if f < 4 else 24)
        PARTS.append(('%s_feather%d' % (side, f), parent, (lx, 0.4, 4), (0.15, s * (0.05 * f), 0),
                      [(-1.2, -0.4, 0, 2.4, 0.8, 9 + f * 1.2, 'quill')], m))
# the tail plumes: long, streaming back and down, in two lengths each
for k, (x, yaw) in enumerate(((-3, -0.3), (-1.5, -0.15), (0, 0), (1.5, 0.15), (3, 0.3))):
    PARTS += [
        ('plume%d' % k, 'body', (x, -1, 7.5), (0.35, yaw, 0), [(-1, -0.4, 0, 2, 0.8, 12, 'plume')], False),
        ('plume%d_end' % k, 'plume%d' % k, (0, 0, 12), (0.25, yaw * 0.6, 0), [(-1.3, -0.4, 0, 2.6, 0.8, 10, 'plumetip')], False),
    ]
for side, s in (('right', -1), ('left', 1)):
    for k, cx in enumerate((-0.6, 0.6)):
        PARTS.append(('%s_talon%d' % (side, k), 'legs', (s * 2 + cx, 7, 0), (0.6, 0, 0),
                      [(-0.25, -0.25, -1.6, 0.5, 0.5, 1.6, 'talon')], False))

RED = (214, 46, 26)
ORANGE = (248, 120, 30)
GOLD = (255, 204, 70)
HOT = (255, 244, 196)
DARK = (40, 14, 10)
EYE = (255, 226, 90)


def feathers(p, rect, base, tip, glow=False, vertical=False):
    """Feathers over a face: each a lit vane with a dark shaft down it, base colour to its tip colour."""
    fx, fy, fw, fh = rect
    width = 4
    for i in range(fw):
        for j in range(fh):
            t = (j / max(1, fh - 1)) if not vertical else (i / max(1, fw - 1))
            c = [a + (b - a) * t for a, b in zip(base, tip)]
            k = 1.0 - 0.18 * abs(((i if not vertical else j) % width) - width / 2) / (width / 2)
            if (i if not vertical else j) % width == width // 2:
                k *= 0.7                                  # the shaft
            n = p.rnd.randint(-4, 4)
            p.px((fx + i, fy + j), [v * k + n for v in c], glow)


def paint_box(p, style, f, dims):
    if style in ('plumage', 'phead'):
        for k in ('top', 'right', 'left', 'front', 'back'):
            p.scales(f[k], RED, cell=4, light=1.05 if k == 'top' else 0.9)
        p.face(f['bottom'], ORANGE, 0.85, noise=5)
    if style == 'breast':
        for k, r in f.items():
            p.scales(r, ORANGE, cell=4, light=1.0)
    if style == 'phead':
        for k, inward in (('right', -1), ('left', 1)):
            fx, fy, fw, fh = f[k]
            p.eye(fx + (fw - 3 if inward < 0 else 2), fy + 3, inward, EYE, w=5)
            for i in range(fw):                       # a dark stripe back from the eye, a raptor's mask
                p.px((fx + i, fy + 7), DARK)
    if style == 'beak':
        for k, r in f.items():
            p.gradient(r, (250, 214, 120), (150, 90, 40), horizontal=k in ('top', 'bottom'))
    if style == 'flame':
        for k, r in f.items():
            p.gradient(r, ORANGE, HOT, glow=True, horizontal=k in ('top', 'bottom'))
    if style == 'covert':
        for k in ('top', 'bottom', 'front', 'back', 'right', 'left'):
            feathers(p, f[k], RED, ORANGE, vertical=k in ('top', 'bottom'))
    if style == 'primary':
        for k, r in f.items():
            feathers(p, r, ORANGE, GOLD, glow=True, vertical=k in ('top', 'bottom'))
    if style == 'quill':
        for k, r in f.items():
            feathers(p, r, ORANGE, HOT, glow=True)
    if style == 'plume':
        for k, r in f.items():
            feathers(p, r, RED, GOLD, glow=True)
    if style == 'plumetip':
        for k, r in f.items():
            feathers(p, r, GOLD, HOT, glow=True)
            fx, fy, fw, fh = r                         # the eye at the plume's end
            if fh > 8:
                for i in range(fw // 2 - 2, fw // 2 + 2):
                    for j in range(fh - 6, fh - 2):
                        p.px((fx + i, fy + j), (30, 120, 160) if abs(i - fw // 2 + 0.5) < 1 else RED, True)
    if style == 'talonleg':
        for r in f.values():
            p.face(r, (120, 70, 40), 0.9, fall=0.3)
    if style == 'talon':
        for r in f.values():
            p.face(r, (40, 30, 26), 1.0, noise=3)


if __name__ == '__main__':
    painter = Painter(53)
    slots = paint(PARTS, painter, paint_box)
    painter.save('vermilion_bird')
    emit(PARTS, slots, 'src/main/java/com/jttw/engine/client/renderer/VermilionBirdRenderer.java')
    print('vermilion bird:', len(PARTS), 'parts,', len(slots), 'boxes')
