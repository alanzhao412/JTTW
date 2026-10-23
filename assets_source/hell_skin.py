"""Shared painting for Hell's skins (hell_guards.py, hell_officials.py, hell_kings.py): on top of
cave_skin's canvas and box faces, the parts of a robed person on the standard humanoid layout
(head 0,0; body 16,16; arm 40,16; leg 0,16; the hat layer 32,0), as HellRenderers' layers use it.

Run nothing here; the scripts that import it write the PNGs."""
from cave_skin import Skin, faces, shade, mix

HEAD, BODY, ARM, LEG = (0, 0, 8, 8, 8), (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (0, 16, 4, 12, 4)
SIDES = ('top', 'bottom', 'right', 'front', 'left', 'back')


def paint(s, box, colour, noise=5, **kw):
    """Every face of a box (u, v, w, h, d) in one colour, lit from above."""
    s.box(*box, colour, noise=noise, **kw)


def face_box(s, skin, hair, hair_front_rows=1, eyes=(30, 20, 20), eye_white=(235, 230, 220), brows=None, mouth=(150, 60, 60),
             beard=None, fierce=False, eye_row=3, closed=False):
    """A head: hair on top, at the back and sides; the face in front with eyes, brows and a mouth (and a beard, if one)."""
    f = faces(*HEAD)
    for side in ('top', 'back', 'right', 'left'):
        s.fill(*f[side], hair, noise=4)
    s.fill(*f['bottom'], shade(skin, 0.8), noise=2)
    x, y, w, h = f['front']
    s.fill(x, y, w, h, skin, noise=3)
    for i in range(w):
        for j in range(hair_front_rows):
            s.put(x + i, y + j, hair)
    s.put(x, y + hair_front_rows, hair)
    s.put(x + 7, y + hair_front_rows, hair)
    r = eye_row
    for ex, inner in ((1, 2), (6, 5)):
        if closed:
            s.put(x + ex, y + r, shade(skin, 0.6))
            s.put(x + inner, y + r, shade(skin, 0.6))
        else:
            s.put(x + ex, y + r, eye_white)
            s.put(x + inner, y + r, eyes)
        if brows:
            s.put(x + ex, y + r - 1, brows)
            s.put(x + inner, y + r - 1, brows if not fierce else shade(brows, 1.0))
            if fierce:
                s.put(x + inner + (1 if inner < 4 else -1), y + r - 1, brows)
    s.put(x + 3, y + r + 2, shade(skin, 0.85))
    s.put(x + 4, y + r + 2, shade(skin, 0.85))
    s.put(x + 3, y + r + 3, mouth)
    s.put(x + 4, y + r + 3, mouth)
    if beard:
        for j in range(r + 3, 8):
            for i in range(1, 7):
                if j > r + 3 or i in (1, 2, 5, 6):
                    s.put(x + i, y + j, beard)
        s.put(x + 2, y + r + 2, beard)
        s.put(x + 5, y + r + 2, beard)


def robe_body(s, colour, trim, collar=None, belt=None):
    """The body box as a robe crossed at the front, its collar edged with trim; a belt if given."""
    f = faces(*BODY)
    for side in SIDES:
        s.fill(*f[side], colour, noise=5, grad=0.15 if side in ('front', 'back', 'right', 'left') else 0)
    x, y, w, h = f['front']
    for j in range(7):
        s.put(x + 1 + j // 2, y + j, trim)
        s.put(x + 6 - j // 2, y + j, trim)
    if collar:
        s.fill(x + 3, y, 2, 2, collar, noise=2)
    if belt:
        for side in ('right', 'front', 'left', 'back'):
            bx, by, bw, bh = f[side]
            s.fill(bx, by + 7, bw, 2, belt, noise=3)


def sleeves(s, colour, trim, hand, cuff_rows=2, hand_rows=2):
    """The arm box as a sleeve to the wrist, a trimmed cuff, the hand below."""
    f = faces(*ARM)
    for side in SIDES:
        x, y, w, h = f[side]
        if side == 'top':
            s.fill(x, y, w, h, colour, noise=4)
            continue
        if side == 'bottom':
            s.fill(x, y, w, h, hand, noise=2)
            continue
        s.fill(x, y, w, h - hand_rows, colour, noise=5, grad=0.15)
        s.fill(x, y + h - hand_rows - cuff_rows, w, cuff_rows, trim, noise=3)
        s.fill(x, y + h - hand_rows, w, hand_rows, hand, noise=2)


def skirt(s, u, v, w, h, d, colour, trim, hem=2, panel=None):
    """A skirt box (u, v, w, h, d) falling to the ground: the robe's colour, a trimmed hem, a panel down the front."""
    f = faces(u, v, w, h, d)
    for side in SIDES:
        x, y, fw, fh = f[side]
        s.fill(x, y, fw, fh, colour, noise=5, grad=0.2 if side not in ('top', 'bottom') else 0)
        if side not in ('top', 'bottom'):
            s.fill(x, y + fh - hem, fw, hem, trim, noise=3)
    if panel:
        x, y, fw, fh = f['front']
        s.fill(x + fw // 2 - 1, y, 2, fh - hem, panel, noise=3)


def solid(s, u, v, w, h, colour, noise=4):
    """A plain swatch (for small boxes that share it)."""
    s.fill(u, v, w, h, colour, noise=noise)
