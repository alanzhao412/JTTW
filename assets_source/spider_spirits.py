"""The Seven Spider Spirits (七个蜘蛛精), as silk_court.py: one frame, seven skins.

Seven sisters who passed for beautiful women: each tall and slender in the court dress of the Tang,
a high-waisted skirt falling to the ground in three tiers and a short train, a cross-collared jacket,
great hanging sleeves, a long silk stole (披帛) looped behind her shoulders and down through her arms;
her hair dressed high in a double-looped chignon with gold pins and dangling ornaments (步摇), a
flower mark (花钿) on her brow, painted brows, almond eyes, a small red mouth. And what she is shows
quietly: a diadem across her hairline set with eight small gems that glow like a spider's eyes; a
pale web worked into the gauze of her over-robe; at her waist a jade spool on her sash, whence her
silk pours; and folded behind her shoulders like a fan, eight slender, jointed spider's legs, pale as
bone, that spread wide as she spins. Each sister in her own silks: the eldest crimson and gold, the
second rose, the third peach, the fourth gold, the fifth jade, the sixth azure, the seventh lilac.

Run from the repository root: python assets_source/spider_spirits.py"""
import math

import beastgen as bg
from silk_court import (part, mirrored, paint, emit, SilkPainter, SIDES, LIGHTS, k, mixc, noisy, each, cloth, sheen_silk,
                        web_motif, edges, vedges, gold, gem_eye, cut_point, extent, JAVA)

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

SKIN = (246, 226, 214)
SKIN_SHADE = (220, 186, 176)
HAIR = (22, 16, 22)
HAIR_SHEEN = (84, 70, 96)
GOLD = (232, 186, 74)
JADE = (180, 230, 210)
LIP = (196, 40, 58)
BONE = (236, 228, 212)

# (robe, accent, under-robe, stole, eye) for each sister, eldest first
SISTERS = [
    ('eldest', (150, 22, 44), (236, 188, 80), (250, 234, 220), (226, 120, 120), (200, 40, 60)),
    ('second', (214, 104, 140), (250, 244, 240), (252, 232, 236), (240, 170, 196), (220, 80, 140)),
    ('third', (232, 144, 92), (120, 196, 160), (252, 240, 222), (246, 196, 150), (230, 120, 60)),
    ('fourth', (226, 182, 66), (176, 30, 40), (252, 246, 220), (240, 214, 130), (220, 160, 40)),
    ('fifth', (82, 158, 120), (236, 210, 140), (232, 246, 236), (160, 214, 186), (60, 180, 130)),
    ('sixth', (78, 128, 196), (230, 236, 248), (232, 240, 252), (160, 196, 236), (80, 150, 230)),
    ('seventh', (156, 116, 200), (248, 244, 252), (244, 236, 252), (206, 180, 236), (170, 110, 230)),
]

ARM = (0.12, 0, 0.14)
FOREARM = (-0.55, -0.2, 0)


def sister_parts():
    ps = [
        part('body', 'root', (0, 11.5, 0), (0, 0, 0), [
            (-3.4, -11, -2.1, 6.8, 11, 4.2, 'bodice'),
            (-3.1, -9.2, -2.5, 6.2, 3, 0.6, 'bodice_front'),
            (-3.7, -11.6, -2.5, 7.4, 2, 5, 'collar'),
            (-3.8, -3.6, -2.6, 7.6, 2.2, 5.2, 'sash_band'),
            (-1.1, -3.8, -3.3, 2.2, 2.6, 0.9, 'jewel'),
        ]),
        part('skirt', 'body', (0, 0, 0), (0, 0, 0), [
            (-4, -1.4, -2.8, 8, 6.4, 5.6, 'skirt1'),
            (-4.8, 4.6, -3.4, 9.6, 6, 6.8, 'skirt2'),
            (-5.7, 9.8, -4.1, 11.4, 2.8, 8.2, 'skirt3'),
        ]),
        part('apron', 'skirt', (0, -1.2, -3), (-0.08, 0, 0), [(-2.2, 0, -0.4, 4.4, 12.5, 0.4, 'apron')]),
        part('train', 'skirt', (0, 6, 3.8), (0.35, 0, 0), [(-4.6, 0, 0, 9.2, 7, 0.6, 'train')]),
        part('right_sash_end', 'body', (-1.6, -2.4, -2.9), (-0.06, 0, 0.06), [(-0.6, 0, -0.2, 1.2, 9, 0.4, 'sash_end')]),
        part('gauze', 'body', (0, -10.8, 0), (0, 0, 0), [(-3.9, 0, -2.75, 7.8, 13, 5.5, 'gauze')]),
        part('head', 'body', (0, -11.6, -0.2), (0, 0, 0), [
            (-1.1, -1.6, -1.1, 2.2, 1.8, 2.2, 'neck'),
            (-2.5, -6.8, -2.5, 5, 5.4, 4.8, 'face'),
            (-2.8, -7.4, -0.4, 5.6, 6.6, 3.1, 'hair_back'),
            (-2.8, -7.6, -2.8, 5.6, 1.5, 5.2, 'hair_top'),
            (-2.62, -7.0, -2.75, 5.24, 0.7, 0.4, 'diadem'),
        ]),
        part('right_lock', 'head', (-2.6, -6.4, -1.6), (0, 0, 0.06), [(-0.5, 0, -0.5, 0.9, 5.4, 1.2, 'lock')]),
        part('bun', 'head', (0, -7.3, 0.6), (-0.15, 0, 0), [
            (-2.1, -3.6, -1.6, 4.2, 3.8, 3.4, 'bun'),
            (-1.4, -5.6, -1.1, 2.8, 2.2, 2.4, 'bun_top'),
        ]),
        part('right_loop', 'bun', (-1.8, -3.4, 0.1), (0, 0, -0.55), [(-0.9, -4.2, -0.9, 1.8, 4.2, 1.8, 'hair_loop')]),
        part('right_pin', 'bun', (-1.6, -2.8, -1.2), (0.3, 0.5, 0.4), [
            (-3.5, -0.25, -0.25, 3.5, 0.5, 0.5, 'pin_shaft'),
            (-4.4, -0.7, -0.7, 1.4, 1.4, 1.4, 'pin_flower'),
        ]),
        part('right_dangle', 'right_pin', (-3.9, 0.4, 0), (0, 0, 0), [
            (-0.15, 0, -0.15, 0.3, 2.6, 0.3, 'dangle_chain'), (-0.5, 2.5, -0.5, 1, 1, 1, 'dangle_bead')]),
        part('comb', 'bun', (0, -1.8, -1.9), (0.2, 0, 0), [(-1.6, -1.2, -0.3, 3.2, 1.4, 0.5, 'comb')]),
        part('right_arm', 'body', (-3.9, -10, 0), ARM, [(-1.5, -0.7, -1.6, 2.6, 6.2, 3.2, 'sleeve_upper')]),
        part('right_forearm', 'right_arm', (-0.3, 5.4, 0), FOREARM, [
            (-1.05, 0, -1.05, 2, 4.4, 2, 'cuff'),
            (-0.85, 4.2, -0.9, 1.7, 2.2, 1.8, 'hand'),
        ]),
        part('right_sleeve', 'right_forearm', (0, 0.2, 0.6), (0.48, 0.1, -0.02), [(-1.9, -0.6, -1.1, 3.8, 9.8, 5.8, 'sleeve_big')]),
        part('right_stole', 'right_forearm', (-0.9, 2.6, 0.8), (0.4, 0, 0.15), [(-0.2, 0, -1.1, 0.4, 13, 2.2, 'stole')]),
        part('right_stole_tail', 'right_stole', (0, 12.6, 0), (-0.45, 0, 0.1), [(-0.2, 0, -1, 0.4, 7, 2, 'stole_tail')]),
        part('stole_back', 'body', (0, -10.4, 2.6), (0.18, 0, 0), [(-6.4, -0.2, -0.2, 12.8, 3.4, 0.4, 'stole_back')]),
        part('silk_stream', 'body', (0, -2.5, -3.4), (0, 0, 0), [(-0.45, -0.45, -14, 0.9, 0.9, 14, 'silk_stream')]),
        part('legs_mount', 'body', (0, -8.4, 2.2), (0, 0, 0), []),
    ]
    # eight slender legs folded behind her shoulders like a fan: each rises, turns out, bends down at its knee
    for i in range(4):
        ps += [
            part('right_sleg_%d' % i, 'legs_mount', (-0.8, -0.4 + i * 1.5, 0), (-(1.05 - i * 0.3), -(0.55 + i * 0.22), 0), [
                (-0.45, -0.45, 0, 0.9, 0.9, 9 - i * 0.6, 'sleg_a')]),
            part('right_sleg_tip_%d' % i, 'right_sleg_%d' % i, (0, 0, 8.8 - i * 0.6), (1.35 - i * 0.12, 0, 0), [
                (-0.35, -0.35, 0, 0.7, 0.7, 9.5 - i * 0.6, 'sleg_b'),
                (-0.5, -0.5, -0.3, 1, 1, 1, 'sleg_knee')]),
        ]
    return mirrored(ps)


def sister_painter(name, robe, accent, under, stole, eye):
    def paint_box(p, style, f, dims):
        if style in ('bodice', 'bodice_front'):
            cloth(p, f, under)
            if style == 'bodice':                                    # an embroidered breast-band at the top of the skirt's bodice
                for s in ('front', 'back'):
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        for j in range(fh - 7, fh):
                            p.px((fx + i, fy + j), noisy(p, k(robe, 0.95), 3))
                        if i % 4 == 1:
                            p.px((fx + i, fy + fh - 6), accent)
        elif style == 'collar':
            cloth(p, f, robe)
            fx, fy, fw, fh = f['front']                              # crossed at the breast, edged in her accent
            for i, j, xy in each(f['front']):
                if abs((i - fw / 2) - (j - fh)) < 1.2 or abs((fw / 2 - i) - (j - fh)) < 1.2:
                    p.px(xy, accent)
            edges(p, f, accent, top=True, bottom=False)
        elif style == 'sash_band':
            cloth(p, f, accent)
            edges(p, f, k(accent, 0.7))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(1, fw - 1, 3):
                    p.px((fx + i, fy + fh // 2), k(robe, 0.9))
        elif style == 'jewel':                                       # the jade spool her silk comes from: pale, faintly glowing
            for s, r in f.items():
                for i, j, xy in each(r):
                    c = mixc(JADE, (255, 255, 255), 0.4 if (i + j) % 3 == 0 else 0)
                    p.px(xy, noisy(p, k(c, LIGHTS[s]), 3), glow=s == 'front')
        elif style.startswith('skirt') or style == 'train':
            for s, r in f.items():
                L = LIGHTS[s]
                sheen_silk(p, r, k(robe, L * (1.02 if style != 'skirt3' else 0.92)), k(mixc(robe, (255, 255, 255), 0.5), L))
            if style == 'skirt3' or style == 'train':                # a hem border: her accent, and a band of embroidered cloud
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 1), k(accent, 0.8))
                        p.px((fx + i, fy + fh - 2), accent)
                        if (i // 3) % 2 == 0:
                            p.px((fx + i, fy + fh - 4), mixc(accent, (255, 255, 255), 0.3))
            if style == 'skirt2':                                    # long pleats
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(0, fw, 3):
                        for j in range(fh):
                            p.px((fx + i, fy + j), k(robe, LIGHTS[s] * 0.82))
        elif style == 'apron':
            cloth(p, f, k(robe, 0.85))
            fx, fy, fw, fh = f['front']                              # a panel embroidered with a lotus
            for i, j, xy in each(f['front']):
                d = math.hypot(i - fw / 2 + 0.5, (j - fh * 0.62) * 0.8)
                if 2 < d < 3.2 and j < fh * 0.7:
                    p.px(xy, accent)
            vedges(p, f['front'], accent)
        elif style == 'gauze':                                       # the over-robe of fine gauze: pale, a web worked in it, half clear
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    if s in ('top', 'bottom'):
                        p.clear(xy)
                        continue
                    if s == 'front' and abs(i - fw / 2 + 0.5) < fw * 0.28:  # open down the front
                        p.clear(xy)
                        continue
                    p.px(xy, noisy(p, mixc(stole, (255, 255, 255), 0.55), 3), a=96)
                if s in ('back', 'right', 'left'):
                    web_motif(p, r, (255, 255, 255), cy=fh * 0.35, r=max(fw, fh) * 0.55)
                    for i, j, xy in each(r):
                        if p.img.getpixel(xy)[:3] == (255, 255, 255):
                            p.px(xy, (255, 255, 255), a=210)
        elif style in ('sleeve_upper', 'cuff'):
            cloth(p, f, robe)
            if style == 'cuff':
                edges(p, f, accent, top=False)
        elif style == 'sleeve_big':
            for s, r in f.items():
                sheen_silk(p, r, k(robe, LIGHTS[s]), k(mixc(robe, (255, 255, 255), 0.45), LIGHTS[s]), step=4)
            for s in SIDES:                                          # a broad cuff band of her accent, and the gauze web over it
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    for j in range(fh - 4, fh):
                        p.px((fx + i, fy + j), noisy(p, accent if j < fh - 1 else k(accent, 0.7), 3))
            fx, fy, fw, fh = f['bottom']
            for i, j, xy in each(f['bottom']):                        # its mouth, dark inside
                if 1 <= i < fw - 1 and 1 <= j < fh - 1:
                    p.px(xy, k(robe, 0.35))
        elif style == 'hand':
            cloth(p, f, SKIN, folds=False, fall=0.1)
            fx, fy, fw, fh = f['bottom']
            for i in range(0, fw, 2):
                p.px((fx + i, fy + fh - 1), (200, 60, 80))            # her nails, lacquered
        elif style in ('stole', 'stole_tail', 'stole_back'):
            for s, r in f.items():
                sheen_silk(p, r, k(stole, LIGHTS[s]), (255, 255, 255), step=3)
            if style == 'stole_tail':
                for s in SIDES:
                    cut_point(p, f[s], keep=0.5, sharp=0.4)
        elif style == 'sash_end':
            cloth(p, f, accent)
            cut_point(p, f['front'], keep=1, sharp=0.6)
            cut_point(p, f['back'], keep=1, sharp=0.6)
        elif style == 'neck':
            cloth(p, f, SKIN, folds=False, fall=0.15)
        elif style == 'face':
            paint_face(p, f, eye)
        elif style in ('hair_back', 'hair_top', 'lock', 'bun', 'bun_top', 'hair_loop'):
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    strand = (i + (j // 3)) % 3
                    c = HAIR_SHEEN if strand == 0 and s in ('top', 'right', 'left', 'back') else HAIR
                    if style in ('bun', 'hair_loop') and abs(math.sin((i + j) * 0.7)) > 0.85:
                        c = HAIR_SHEEN
                    p.px(xy, noisy(p, k(c, LIGHTS[s] * (1.05 - 0.2 * j / max(1, fh))), 3))
            if style == 'hair_back':
                fx, fy, fw, fh = f['front']                          # its face side, hidden behind hers
                for i, j, xy in each(f['front']):
                    p.px(xy, HAIR)
            if style == 'lock':
                for s in SIDES:
                    cut_point(p, f[s], keep=0.6, sharp=0.5)
        elif style == 'diadem':                                       # gold, with eight little gems: a spider's eyes, if one looks
            gold(p, f, GOLD)
            fx, fy, fw, fh = f['front']
            for g in range(8):
                x = fx + int(round((g + 0.5) * fw / 8))
                c = mixc(eye, (255, 255, 255), 0.25)
                p.px((x, fy), c, glow=True)
                if fh > 1:
                    p.px((x, fy + 1), k(c, 0.8), glow=True)
        elif style in ('pin_shaft', 'dangle_chain', 'comb'):
            gold(p, f, GOLD)
        elif style in ('pin_flower', 'dangle_bead'):
            for s, r in f.items():
                for i, j, xy in each(r):
                    c = accent if style == 'pin_flower' and (i + j) % 2 == 0 else (JADE if style == 'dangle_bead' else GOLD)
                    p.px(xy, noisy(p, k(c, LIGHTS[s]), 3))
        elif style == 'silk_stream':                                  # the silk pouring from her: white, glowing
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, (255, 252, 244) if (i + j) % 3 else (226, 222, 214), glow=True)
        elif style in ('sleg_a', 'sleg_b', 'sleg_knee'):
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    along = (j + 0.5) / fh if s in ('right', 'left', 'top', 'bottom') else 0.5
                    c = mixc(BONE, k(robe, 0.8), 0.25) if style == 'sleg_knee' else BONE
                    if style == 'sleg_b':
                        c = mixc(BONE, (60, 40, 50), along ** 2)   # darkening to a fine dark claw at its tip
                    if s in ('right', 'left') and (j % 6 == 0):
                        c = k(c, 0.75)                               # its joints' rings
                    p.px(xy, noisy(p, k(c, LIGHTS[s]), 3))
        else:
            cloth(p, f, robe)
    return paint_box


def paint_face(p, f, eye):
    """A gentle face: pale skin, a faint blush, painted brows, almond eyes darkly lashed, the iris her
    colour with a faint glow; a small red mouth; a flower mark between her brows."""
    for s, r in f.items():
        for i, j, xy in each(r):
            p.px(xy, noisy(p, k(SKIN, LIGHTS[s] * (1.02 - 0.12 * (j / max(1, r[3])))), 2))
    for s in ('right', 'left'):                                      # her hair over her temples
        fx, fy, fw, fh = f[s]
        for i, j, xy in each(f[s]):
            if j < 3 or (i > fw * 0.55 and j < fh * 0.8):
                p.px(xy, noisy(p, HAIR, 3))
    fx, fy, fw, fh = f['top']
    for i, j, xy in each(f['top']):
        p.px(xy, noisy(p, HAIR, 3))
    fx, fy, fw, fh = f['front']                                      # 10 x 11 texture pixels
    for i in range(fw):
        p.px((fx + i, fy), HAIR)
        p.px((fx + i, fy + 1), HAIR if i < 2 or i >= fw - 2 else noisy(p, SKIN, 2))
    cx = fw // 2
    # the flower mark between the brows
    p.px((fx + cx - 1, fy + 2), (220, 40, 60))
    p.px((fx + cx, fy + 2), (220, 40, 60))
    p.px((fx + cx - 1, fy + 3), (240, 90, 100))
    p.px((fx + cx, fy + 3), (240, 90, 100))
    # brows: fine arcs
    for i in range(3):
        p.px((fx + 1 + i, fy + 3 - (1 if i == 1 else 0) + 1), (60, 40, 50))
        p.px((fx + fw - 2 - i, fy + 3 - (1 if i == 1 else 0) + 1), (60, 40, 50))
    # eyes: almond, lashed above, the iris her colour
    for side in (0, 1):
        ex = fx + (1 if side == 0 else fw - 4)
        ey = fy + 5
        for i in range(3):
            p.px((ex + i, ey), (24, 14, 20))                        # the lash line
        p.px((ex + (0 if side == 0 else 2), ey - 1), (24, 14, 20))   # flicked out at the outer corner
        p.px((ex, ey + 1), (250, 246, 240))
        p.px((ex + 2, ey + 1), (250, 246, 240))
        p.px((ex + 1, ey + 1), eye, glow=True)
        p.px((ex + (1 if side == 0 else 1), ey + 1), mixc(eye, (255, 255, 255), 0.2), glow=True)
    # a faint blush, a small nose, the red mouth
    for side in (1, fw - 3):
        for i in range(2):
            p.px((fx + side + i, fy + 7), mixc(SKIN, (240, 150, 160), 0.45))
    p.px((fx + cx - 1, fy + 7), SKIN_SHADE)
    p.px((fx + cx - 1, fy + 9), LIP)
    p.px((fx + cx, fy + 9), LIP)
    p.px((fx + cx - 1, fy + 8), mixc(SKIN, LIP, 0.4))
    p.px((fx + cx, fy + 8), mixc(SKIN, LIP, 0.4))


if __name__ == '__main__':
    ps = sister_parts()
    for i, (name, robe, accent, under, stole, eye) in enumerate(SISTERS):
        p = SilkPainter(50 + i)
        slots = paint(ps, p, sister_painter(name, robe, accent, under, stole, eye))
        p.save('spider_spirit_' + name)
    emit(ps, slots, JAVA, 'sister')
    print('spider spirit', len(ps), 'parts', {a: round(b, 2) for a, b in extent(ps, 1.0).items()})
