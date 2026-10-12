"""The Black Tortoise (Xuanwu), the World Turtle's true form: a vast dark tortoise under a high,
ridged shell plated in bronze-edged scutes, on columns of legs; and coiled about its shell a great
serpent, its neck rising over the tortoise's head, hooded, jaws open, eyes burning red.
"""
import math
from beastgen import Painter, paint, emit, segment

PARTS = [
    ('shell', 'root', (0, 5, 0), (0, 0, 0), [
        (-13, 5, -15, 26, 4, 30, 'rim'),            # the shell's rim, flaring
        (-12, 1, -14, 24, 4, 28, 'shell'),
        (-10, -2, -11.5, 20, 3, 23, 'shell'),
        (-7, -4.5, -8, 14, 2.5, 16, 'shellcap'),    # its crown
        (-12, 9, -14, 24, 1.5, 28, 'plastron'),     # its belly plate
        (-1, -6.5, -6.5, 2, 2, 3, 'knob'),          # the knobs along its keel
        (-1, -6.5, -0.5, 2, 2, 3, 'knob'),
        (-1, -6.5, 5.5, 2, 2, 3, 'knob'),
        (-8.5, -3.5, -2, 2, 2, 3, 'knob'), (6.5, -3.5, -2, 2, 2, 3, 'knob'),
    ], False),
    ('tail', 'root', (0, 13, 14.5), (-0.4, 0, 0), [(-1.6, -1.6, 0, 3.2, 3.2, 6, 'skin')], False),
    ('neck', 'root', (0, 11.5, -13.5), (0.15, 0, 0), [(-3.2, -3.2, -8, 6.4, 6.4, 8.5, 'skin')], False),
    ('head', 'neck', (0, -0.5, -7.5), (-0.1, 0, 0), [
        (-3.6, -3.6, -6.5, 7.2, 6.2, 7, 'thead'),
        (-2.6, -0.5, -9, 5.2, 3.6, 3, 'beak'),       # the beak, hooked
        (-1.6, 2.5, -9.6, 3.2, 1.6, 1.2, 'beak'),
        (-4.1, -4.2, -6.8, 2.6, 1.6, 4, 'skin'),     # heavy brows
        (1.5, -4.2, -6.8, 2.6, 1.6, 4, 'skin'),
    ], False),
]
for name, x, z in (('right_front_leg', -11, -9.5), ('left_front_leg', 11, -9.5),
                   ('right_hind_leg', -11, 9.5), ('left_hind_leg', 11, 9.5)):
    PARTS.append((name, 'root', (x, 15.5, z), (0, 0, x / abs(x) * -0.25), [
        (-4.4, 0, -4.4, 8.8, 7, 8.8, 'leg'),
        (-5, 6.3, -5.6, 10, 2.2, 10.2, 'foot')], x > 0))
    for k, cx in enumerate((-3, 0, 3)):
        PARTS.append(('%s_claw%d' % (name, k), name, (cx, 7.6, -5.4), (0.5, 0, 0),
                      [(-0.45, -0.45, -1.8, 0.9, 0.9, 1.8, 'claw')], x > 0))

# the serpent: coiled once and more about the shell's rim, rising, then up over the tortoise's head
DROP = 5
coil = []
TURNS, N = 1.15, 22
for k in range(N + 1):
    th = math.pi * 0.85 + 2 * math.pi * TURNS * k / N          # starting behind, on the right
    y = 6.5 - 5.5 * k / N                                      # climbing as it goes round
    coil.append((15.5 * math.sin(th), y + DROP, 17.5 * math.cos(th)))
rise = [(9, -2 + DROP, -16), (7.5, -9 + DROP, -18), (4.5, -15 + DROP, -17.5)]
pts = coil + rise
for k in range(len(pts) - 1):
    t = 1.6 + 2.6 * min(1, k / 6)                              # from the tail's tip, thickening
    xr, yr, length = segment(pts[k], pts[k + 1])
    PARTS.append(('snake%d' % k, 'root', pts[k], (xr, yr, 0),
                  [(-t / 2, -t / 2, -0.4, t, t, length + 0.8, 'serpent')], False))
hx, hy, _ = segment(rise[-2], rise[-1])
PARTS += [
    ('snake_head', 'root', rise[-1], (hx - 0.9, hy, 0), [
        (-2.4, -2, -0.5, 4.8, 3.6, 5.5, 'shead'),
        (-1.8, -1.2, 5, 3.6, 2.2, 2.6, 'shead'),
        (-4.6, -2.2, -1.5, 9.2, 4.6, 1, 'hood'),    # its hood, spread
        (-1.6, 1.2, 6.2, 0.6, 1.6, 0.6, 'fang'),
        (1, 1.2, 6.2, 0.6, 1.6, 0.6, 'fang'),
    ], False),
    ('snake_jaw', 'snake_head', (0, 1.6, 0.5), (0.55, 0, 0), [
        (-1.8, 0, 0, 3.6, 1, 7, 'sjaw'),
        (-1.4, -1.2, 6, 0.5, 1.2, 0.5, 'fang'), (0.9, -1.2, 6, 0.5, 1.2, 0.5, 'fang')], False),
]

SHELL = (34, 44, 40)
EDGE = (176, 142, 72)
SKIN = (66, 72, 68)
SERPENT = (40, 92, 62)
BELLY = (196, 186, 120)
DARK = (14, 16, 16)
MOUTH = (110, 30, 38)
IVORY = (236, 228, 204)
RED = (255, 60, 40)
AMBER = (255, 170, 40)


def scutes(p, rect, cell=12):
    """Bronze-edged plates over a face, offset row to row, each darker at its edge than its heart."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        row = j // cell
        off = cell // 2 if row % 2 else 0
        for i in range(fw):
            cx, cy = (i + off) % cell, j % cell
            edge = cx == 0 or cy == 0
            r = max(abs(cx - cell / 2), abs(cy - cell / 2)) / (cell / 2)
            if edge:
                p.px((fx + i, fy + j), [v * (0.95 + 0.1 * p.rnd.random()) for v in EDGE])
            else:
                k = 1.15 - 0.4 * r
                p.px((fx + i, fy + j), [v * k + p.rnd.randint(-3, 3) for v in SHELL])


def paint_box(p, style, f, dims):
    if style in ('shell', 'shellcap', 'rim'):
        for k in ('top', 'right', 'left', 'front', 'back'):
            scutes(p, f[k], 12 if style != 'rim' else 8)
        p.face(f['bottom'], DARK, 1.0)
    if style == 'plastron':
        for r in f.values():
            p.face(r, (120, 104, 62), 0.9, fall=0.2)
    if style == 'knob':
        for r in f.values():
            p.gradient(r, (220, 206, 170), (120, 104, 70))
    if style in ('skin', 'leg', 'thead'):
        p.shaded(f, SKIN)
        for k in ('top', 'right', 'left', 'front', 'back'):     # its hide, cracked into plates
            fx, fy, fw, fh = f[k]
            for j in range(0, fh, 4):
                for i in range(fw):
                    if (i + j // 4 * 2) % 5 == 0 or j % 4 == 0 and p.rnd.random() < 0.5:
                        p.px((fx + i, fy + j), [v * 0.7 for v in SKIN])
    if style == 'thead':
        for k, inward in (('right', -1), ('left', 1)):          # small, hard eyes, glowing amber
            fx, fy, fw, fh = f[k]
            p.eye(fx + (fw - 3 if inward < 0 else 2), fy + 3, inward, AMBER, w=5, pupil_at=1)
    if style == 'foot':
        p.shaded(f, SKIN)
        p.face(f['bottom'], DARK, 1.0)
    if style == 'claw':
        for k, r in f.items():
            p.face(r, (40, 36, 34) if k == 'front' else IVORY, 0.9, noise=3)
    if style == 'beak':
        for r in f.values():
            p.gradient(r, (70, 66, 58), (20, 20, 22))
    if style in ('serpent', 'shead', 'sjaw'):
        for k in ('top', 'right', 'left', 'front', 'back'):
            p.scales(f[k], SERPENT, cell=4, light=1.0 if k == 'top' else 0.9)
        p.face(f['bottom'], BELLY, 0.85, noise=4)
        if style == 'serpent':                                  # dark diamonds down its back
            fx, fy, fw, fh = f['top']
            for j in range(fh):
                m = abs((j % 10) - 5)
                for i in range(fw // 2 - (5 - m) // 2, fw // 2 + (5 - m) // 2 + 1):
                    p.px((fx + i, fy + j), DARK)
    if style == 'shead':
        for k, inward in (('right', -1), ('left', 1)):
            fx, fy, fw, fh = f[k]
            if fw >= 8:
                p.eye(fx + (fw - 3 if inward < 0 else 2), fy + 1, inward, RED, w=4, pupil_at=1)
    if style == 'sjaw':
        p.face(f['top'], MOUTH, 0.8, noise=4)
    if style == 'hood':
        for k, r in f.items():
            p.scales(r, SERPENT, cell=4, light=0.95)
        fx, fy, fw, fh = f['back']                              # the eye-marks on the back of its hood
        for cx in (fx + fw // 4, fx + 3 * fw // 4):
            for i in range(-2, 2):
                for j in range(-1, 2):
                    p.px((cx + i, fy + fh // 2 + j), BELLY if abs(i + 0.5) > 1 else DARK)
    if style == 'fang':
        for r in f.values():
            p.face(r, IVORY, 1.0, noise=3)


if __name__ == '__main__':
    painter = Painter(41)
    slots = paint(PARTS, painter, paint_box)
    painter.save('black_tortoise')
    emit(PARTS, slots, 'src/main/java/com/jttw/engine/client/renderer/BlackTortoiseRenderer.java')
    print('black tortoise:', len(PARTS), 'parts,', len(slots), 'boxes')
