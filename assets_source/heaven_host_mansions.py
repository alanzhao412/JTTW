"""The Twenty-Eight Lunar Mansions (二十八宿), Heaven's star generals (heaven_host.py builds them).

One body for the twenty-eight: the trooper's frame made grander, armoured in lamellae lacquered in
his quarter's colour and edged in gold (the Azure Dragon's east in jade green, the Black Tortoise's
north in blue-black, the White Tiger's west in silver-white, the Vermilion Bird's south in
vermilion), a helmet with phoenix wings and a red tassel, a cloak of his quarter's colour worked
with gold clouds; his eyes shine with starlight. His mansion's beast (heaven_host_emblems.py) stands
in gold as the great crest before his helmet and is painted on the round shield on his left arm,
whose gold rim is set with the seven stars of his quarter; a straight sword (剑) in his right hand.
Each mansion's own skin (heaven_mansion_<id>.png, its glow _glow.png): his quarter's armour, his beast."""
import math

import beastgen as bg
from beastgen import layout, size
from king_yama import exact_faces
from heaven_host_kit import *
from heaven_host import cape, plume_big, paint_arms, TROOPER_SIZE
from heaven_host_emblems import EMBLEMS

MANSIONS = list(EMBLEMS)

# his left forearm before him, bearing the shield; his right holds the sword upright
M_LARM, M_LFOREARM = (-0.3, 0, -0.18), (-1.15, 0.45, 0)


def crest():
    """The great crest before the helmet: his beast, cut out in gold (18 x 14 texture pixels)."""
    return [part('emblem', 'helm', (0, -1.6, -3.75), (-0.08, 0, 0), [(-4.5, -7.0, -0.25, 9.0, 7.0, 0.5, 'emblem_crest')])]


def shield():
    return [part('shield', 'left_forearm', (0.2, 2.6, 0), aim([M_LARM, M_LFOREARM], (0.05, 0, 0)), [
        (-5.5, -5.5, -3.7, 11.0, 11.0, 0.8, 'shield_face'),
        (-6.0, -6.0, -3.0, 12.0, 12.0, 0.7, 'shield_rim'),
        (-1.0, -1.0, -2.4, 2.0, 2.0, 2.4, 'shield_grip'),
    ])]


def jian():
    """The straight sword (剑), upright in his fist: a steel blade, a gold guard, a cord-bound grip, a tassel."""
    return [part('jian', 'right_hand', (0, 1.2, 0), aim([ARM, FOREARM, (0, 0, 0)], (0.08, 0, 0)), [
        (-0.62, -16.4, -0.2, 1.24, 14.0, 0.4, 'jian_blade'),
        (-1.7, -2.6, -0.65, 3.4, 0.9, 1.3, 'guard'),
        (-0.42, -1.8, -0.42, 0.84, 3.4, 0.84, 'hilt'),
        (-0.65, 1.5, -0.65, 1.3, 1.0, 1.3, 'pommel'),
    ]), part('jian_tassel', 'jian', (0, 2.4, 0), (0.1, 0, 0), [(-0.7, 0, -0.7, 1.4, 3.6, 1.4, 'tassel_tip')])]


def mansion_parts():
    parts = (legs() + waist(skirt=7.6, apron=9.8) + body() + arms() + head()
             + without(helm(plume=True, wings=True), ['crest']) + crest() + jian() + shield() + cape(length=10.4, lower=10.2)
             + [part('right_moustache', 'head', (-0.3, -2.25, -3.45), (0, 0.15, -0.35), [(-2.2, -0.35, -0.3, 2.2, 0.6, 0.6, 'moustache')])])
    parts = mirrored(parts)
    return pose(parts, {'left_arm': M_LARM, 'left_forearm': M_LFOREARM})


MANSION = mansion_parts()

GLOW_EYES = (220, 236, 255)


def quarter_colours(q):
    """The quarter's armour, silk and cloak: 0 east (jade green), 1 north (blue-black), 2 west (silver-white), 3 south (vermilion)."""
    common = dict(plate_trim=GOLD, moustache=True, captain=True, glow_eyes=GLOW_EYES, beard=(40, 30, 28))
    if q == 0:
        return colours(silk=(214, 232, 214), silk_deep=(112, 150, 126), trim=(40, 128, 108), plume=(212, 38, 34), plume_deep=(110, 14, 16),
                       lace=(214, 180, 70), cloak=(30, 112, 98), cloak_deep=(10, 52, 46), plate=(58, 158, 128), plate_deep=(18, 72, 60),
                       helm=(70, 168, 138), field=(26, 96, 84), jewel=(255, 220, 120), **common)
    if q == 1:
        return colours(silk=(56, 74, 132), silk_deep=(20, 26, 58), trim=(36, 40, 70), plume=(70, 92, 186), plume_deep=(24, 30, 80),
                       lace=(200, 168, 70), cloak=(30, 32, 58), cloak_deep=(10, 10, 22), plate=(54, 60, 92), plate_deep=(16, 16, 32),
                       helm=(62, 68, 104), field=(22, 24, 46), jewel=(120, 170, 255), **common)
    if q == 2:
        return colours(silk=(236, 236, 244), silk_deep=(150, 154, 176), trim=(120, 128, 156), plume=(246, 246, 252), plume_deep=(170, 174, 196),
                       lace=(130, 140, 170), cloak=(232, 234, 242), cloak_deep=(140, 146, 170), plate=(214, 220, 234), plate_deep=(112, 120, 144),
                       helm=(226, 230, 240), field=(150, 160, 186), jewel=(200, 60, 60), **common)
    return colours(silk=(244, 196, 84), silk_deep=(160, 96, 26), trim=(150, 24, 18), plume=(250, 120, 40), plume_deep=(150, 40, 14),
                   lace=(250, 210, 90), cloak=(200, 44, 26), cloak_deep=(96, 14, 8), plate=(214, 64, 34), plate_deep=(104, 20, 12),
                   helm=(222, 74, 40), field=(150, 30, 16), jewel=(255, 230, 120), **common)


def paint_mansion(p, style, f, dims, c):
    if style == 'emblem_crest':                            # stamped per mansion (stamp())
        bare(p, f, ('top', 'bottom', 'right', 'left', 'front', 'back'))
        return True
    if style == 'shield_face':                             # the field in his colour, lacquered; the beast is stamped on it
        bare(p, f, ('top', 'bottom', 'right', 'left'))
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            cx, cy, R = fx + fw / 2, fy + fh / 2, fw / 2
            for i, j, xy in each(f[s]):
                d = math.hypot(i + 0.5 - fw / 2, j + 0.5 - fh / 2) / R
                if d > 1.0:
                    p.clear(xy)
                elif d > 0.9:
                    p.px(xy, GOLD_DEEP)
                else:
                    hl = 1.12 - 0.3 * math.hypot((i - fw * 0.35) / fw, (j - fh * 0.3) / fh)
                    p.px(xy, noisy(p, k(c['field'], hl), 2))
        return True
    if style == 'shield_rim':                              # the gold rim, set with the seven stars of his quarter
        bare(p, f, ('top', 'bottom', 'right', 'left'))
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            R = fw / 2
            for i, j, xy in each(f[s]):
                d = math.hypot(i + 0.5 - fw / 2, j + 0.5 - fh / 2) / R
                if d > 1.0:
                    p.clear(xy)
                else:
                    a = math.atan2(j + 0.5 - fh / 2, i + 0.5 - fw / 2)
                    col = mixc(GOLD_LIGHT, GOLD_DEEP, min(1, max(0, (j / fh) * 1.2)))
                    if (int(math.degrees(a) + 360) // 15) % 2 == 0:
                        col = k(col, 0.86)
                    p.px(xy, noisy(p, col, 3))
            if s == 'front':
                for n in range(7):                         # the seven stars, round the rim
                    a = -math.pi / 2 + n * 2 * math.pi / 7
                    x, y = fx + fw / 2 + math.cos(a) * R * 0.88, fy + fh / 2 + math.sin(a) * R * 0.88
                    p.lit((x - 0.5, y - 0.5), (255, 250, 220), glow=0.95)
                    p.lit((x - 0.5, y + 0.5), (255, 240, 180), glow=0.6)
        return True
    if style == 'shield_grip':
        gilt(p, f)
        return True
    if style == 'jian_blade':                              # a bright straight blade, a ridge down it, a point
        steel(p, f)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                p.px((fx + fw // 2, fy + j), (252, 254, 255))
            point(p, f[s], keep=1.0, down=False, sharp=0.6)
        bare(p, f, ('top',))
        return True
    return False


def mansion_painter(c):
    def paint_box(p, style, f, dims):
        if paint_mansion(p, style, f, dims, c) or paint_trooper(p, style, f, dims, c) or paint_arms(p, style, f, dims, c):
            return
        p.shaded(f, (255, 0, 255))
    return paint_box


def faces_of(parts, style):
    """The faces (texture pixels) of the box of this style."""
    slots = layout(parts)
    for key, (u, v) in slots.items():
        if key[3] == style:
            for _, _, _, _, boxes, _ in parts:
                for b in boxes:
                    if b[6] == style:
                        return exact_faces(u, v, *b[3:6])


def stamp(p, name, c):
    """His beast: cut out in gold on the crest, painted in gold on the shield, its eye shining."""
    rows = EMBLEMS[name]
    cf = faces_of(MANSION, 'emblem_crest')
    for s, flip in (('front', False), ('back', True)):
        fx, fy, fw, fh = cf[s]
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                x = fx + (17 - i if flip else i)
                if ch == '.':
                    continue
                t = j / 13
                if ch == '*':
                    p.lit((x, fy + j), GLOW_EYES, glow=0.9)
                elif ch == 'o':
                    p.px((x, fy + j), k(GOLD_DEEP, 0.75))
                else:
                    edge = any(not (0 <= j + dj < 14 and 0 <= i + di < 18) or rows[j + dj][i + di] == '.'
                               for di, dj in ((0, -1), (-1, 0)))
                    p.px((x, fy + j), noisy(p, GOLD_LIGHT if edge else mixc(GOLD, GOLD_DEEP, t * 0.8), 3))
    sf = faces_of(MANSION, 'shield_face')
    fx, fy, fw, fh = sf['front']
    ox, oy = fx + (fw - 18) // 2, fy + (fh - 14) // 2
    solid = lambda i, j: 0 <= j < 14 and 0 <= i < 18 and rows[j][i] != '.'
    for j in range(-1, 15):
        for i in range(-1, 19):
            if solid(i, j):
                ch = rows[j][i]
                if ch == '*':
                    p.lit((ox + i, oy + j), GLOW_EYES, glow=0.9)
                elif ch == 'o':
                    p.px((ox + i, oy + j), k(c['field'], 0.55))
                else:
                    lit = not solid(i, j - 1) or not solid(i - 1, j)
                    p.px((ox + i, oy + j), noisy(p, GOLD_LIGHT if lit else mixc(GOLD, GOLD_DEEP, j / 16), 3))
            elif any(solid(i + di, j + dj) for di, dj in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                p.px((ox + i, oy + j), k(c['field'], 0.5))   # a dark outline, so it stands off the field
