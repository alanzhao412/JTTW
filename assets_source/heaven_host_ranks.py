"""The heavenly archer and the thunder drummer (heaven_host.py builds them).

The heavenly archer: lighter armour (a gilt lamellar vest over a quilted coat of sky-blue silk, one
tier on each shoulder), a round gilt cap with two long pheasant feathers (翎子) sweeping up and back
from it, a quiver of red-fletched arrows over his right shoulder on a baldric, and a long gilt
recurve bow with dark horn tips. He holds the bow at his side; fighting, he turns side-on, raises
it, draws the string to his cheek with an arrow nocked, and looses. His captain in crimson and gold
with a crimson cloak.

The thunder drummer: a stocky, bearded man of the host, his arms bare (gilt bracers), a padded
indigo coat studded in gold under a broad sash, a thunder medallion on his breast; no helmet, his
hair in a topknot with a gold pin, a red headband whose tails stream behind; on his back a frame of
red-lacquered posts carrying three war drums (one great drum over his head, one at each shoulder),
red-lacquered and gold-studded, their heads painted with the thunder spiral (雷纹); a drumstick in
each fist, red-knobbed. Winding up his roll, he raises both sticks high over his head. His captain
in crimson, gold-trimmed, a crimson cloak."""
import math

from beastgen import segment
from heaven_host_kit import *
from heaven_host import halberd, cape, ribbon, plume_big, paint_arms, TROOPER_SIZE

# ── The archer ───────────────────────────────────────────────────────────────

# his left arm holds the bow at his side; his right hangs
A_LARM, A_LFOREARM = (-0.12, 0, -0.12), (-0.55, 0.15, 0)
A_RARM, A_RFOREARM = (0.08, 0, 0.12), (-0.35, -0.1, 0)
BOW_REST = (0.12, -math.pi / 2, 0)
DRAW = 8.5                       # how far back from the bow the string is drawn


def strip(parts, styles):
    """The parts without these boxes."""
    return [(n, par, piv, rot, [b for b in boxes if b[6] not in styles], m) for n, par, piv, rot, boxes, m in parts]


def restyle(parts, names):
    """The parts with these boxes' styles renamed: {old: new}."""
    return [(n, par, piv, rot, [b[:6] + (names.get(b[6], b[6]),) for b in boxes], m) for n, par, piv, rot, boxes, m in parts]


def curve(name, parent, pivot, rot, pts, w, t, style):
    """A thin strip along the points (in the parent's frame, under `rot`), a part per stretch, each a child
    of `name`: broad `w` across the strip's path in the y-z plane, `t` thick in x."""
    out = [part(name, parent, pivot, rot, [])]
    for i in range(len(pts) - 1):
        rx, ry, length = segment(pts[i], pts[i + 1])
        out.append(part('%s_%d' % (name, i), name, pts[i], (rx, ry, 0), [(-t / 2, -w / 2, -0.1, t, w, length + 0.2, style)]))
    return out


def bow():
    """The long recurve bow in his left fist: the grip, the limbs bending away from the string, the horn tips
    curling back toward it; the string; and, shown as he draws, the string drawn back to a nock with an
    arrow on it. Its frame: the limbs along y, the string toward +z."""
    up = [(0, -1.4, -0.2), (0, -6.2, -1.3), (0, -10.6, -1.1), (0, -13.2, 0.1), (0, -14.4, 1.0)]
    down = [(x, -y, z) for x, y, z in up]
    out = [part('bow', 'left_hand', (0, 1.2, 0), aim([A_LARM, A_LFOREARM, (0, 0, 0)], BOW_REST), [
        (-0.55, -1.7, -0.75, 1.1, 3.4, 1.5, 'bowgrip')])]
    out += curve('bow_upper', 'bow', (0, 0, 0), (0, 0, 0), up, 1.0, 0.8, 'bowlimb')
    out += curve('bow_lower', 'bow', (0, 0, 0), (0, 0, 0), down, 1.0, 0.8, 'bowlimb')
    out += [part('bowstring', 'bow', (0, -14.3, 0.95), (0, 0, 0), [(-0.12, 0, -0.12, 0.24, 28.6, 0.24, 'string')])]
    # drawn: the string from each tip back to the nock, and the arrow
    for side, tip in (('drawn_upper', up[-1]), ('drawn_lower', down[-1])):
        rx, ry, length = segment(tip, (0, 0, DRAW))
        out.append(part(side, 'bow', tip, (rx, ry, 0), [(-0.12, -0.12, 0, 0.24, 0.24, length, 'string')]))
    out.append(part('arrow', 'bow', (0.55, 0, DRAW), (0, 0, 0), [
        (-0.18, -0.18, -DRAW - 3.2, 0.36, 0.36, DRAW + 3.4, 'arrowshaft'),
        (-0.4, -0.4, -DRAW - 4.6, 0.8, 0.8, 1.5, 'arrowhead'),
        (-0.06, -0.7, -0.4, 0.12, 1.4, 2.6, 'fletch'),
    ]))
    return out


def quiver():
    """A quiver slung at his back, its mouth over his right shoulder, red-fletched arrows in it; the baldric
    across his breast."""
    arrows = [(-0.9 + 0.75 * i, -11.0 - 0.6 * (i % 2), -0.3 + 0.35 * (i % 3) - 0.3) for i in range(4)]
    return [
        part('quiver', 'body', (1.6, -2.6, 3.6), (0.1, 0, -0.5), [
            (-1.5, -8.6, -1.25, 3.0, 9.0, 2.5, 'quiver'),
            (-1.75, -9.4, -1.5, 3.5, 1.2, 3.0, 'quiver_mouth'),
            (-1.7, -1.0, -1.45, 3.4, 1.0, 2.9, 'quiver_mouth'),
        ] + [(x - 0.17, y, z - 0.17, 0.34, 3.0, 0.34, 'arrowshaft') for x, y, z in arrows]
          + [(x - 0.06, y - 0.2, z - 0.55, 0.12, 2.2, 1.1, 'fletch') for x, y, z in arrows]),
        part('baldric', 'body', (0, -5.4, 0), (0, 0, -0.62), [(-0.55, -6.2, -3.3, 1.1, 12.4, 6.6, 'baldric')]),
    ]


def archer_helm():
    """A round gilt cap: a brow band, a low bowl, a finial with its pearl; and from its back two long pheasant
    feathers (翎子) rising and sweeping back."""
    out = without(helm(plume=False, crest='helmcrest'), ['right_flap', 'neckguard'])
    out += [part('right_flap', 'helm', (-3.55, -0.3, 0.3), (0, 0, 0.1), [(-0.6, 0, -2.6, 0.7, 3.2, 5.0, 'earflap')]),
            part('neckguard', 'helm', (0, -0.2, 3.25), (0.22, 0, 0), [(-3.55, 0, -0.4, 7.1, 3.2, 0.8, 'neckguard')])]
    pts = [(0, 0, 0), (-0.5, -4.5, 1.6), (-1.3, -8.6, 4.2), (-2.0, -11.6, 7.8), (-2.4, -12.8, 11.8), (-2.4, -12.4, 15.4)]
    out += curve('right_feather', 'helm', (-1.5, -3.4, 2.2), (0, 0, 0), pts, 2.0, 0.3, 'feather')
    return out


def archer_parts():
    arms_ = strip(arms(), ('pauldron2', 'pauldron3', 'beastface'))
    parts = (legs() + waist(skirt=5.6, apron=8.6) + body(mirrors=False) + arms_ + head() + archer_helm() + quiver()
             + cape() + [part('right_moustache', 'head', (-0.3, -2.25, -3.45), (0, 0.15, -0.35), [(-2.2, -0.35, -0.3, 2.2, 0.6, 0.6, 'moustache')])])
    parts = restyle(parts, {'cuirass': 'vest', 'abdomen': 'quilt', 'torso': 'coat', 'tasset_side': 'tasset_side', 'bellybeast': 'bellyknot'})
    parts = mirrored(parts) + bow()
    return pose(parts, {'left_arm': A_LARM, 'left_forearm': A_LFOREARM, 'right_arm': A_RARM, 'right_forearm': A_RFOREARM})


ARCHER = archer_parts()

ARCHER_COLOURS = {
    False: colours(silk=(132, 186, 222), silk_deep=(56, 98, 150), trim=(236, 242, 248), plume=(196, 40, 36), plume_deep=(104, 16, 18),
                   lace=(56, 98, 150)),
    True: colours(silk=(168, 28, 32), silk_deep=(84, 10, 16), trim=(236, 190, 76), plume=(226, 36, 30), plume_deep=(118, 12, 14),
                  lace=(120, 14, 18), cloak=(156, 22, 28), cloak_deep=(72, 8, 14), moustache=True, captain=True, jewel=(80, 200, 220)),
}


def paint_archer(p, style, f, dims, c):
    if style == 'vest':                                    # a gilt lamellar vest, open at the throat, over the coat
        lamellar_box(p, f, c, pw=3, ph=3)
        fx, fy, fw, fh = f['front']
        for j in range(fh):                                # the coat showing at the opening
            w = max(0, 3 - j // 2)
            for i in range(fw // 2 - w, fw // 2 + w):
                p.px((fx + i, fy + j), noisy(p, c['trim'], 3))
            p.px((fx + fw // 2, fy + j), GOLD_DEEP)
        return True
    if style in ('quilt', 'coat'):                         # the quilted coat: silk in diamonds, studded
        for s, r in f.items():
            fabric(p, r, c['silk'], c['silk_deep'], LIGHTS[s], 4, 0.25)
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                if (i + j) % 6 == 0 or (i - j) % 6 == 0:
                    p.px(xy, k(c['silk_deep'], 1.1 * LIGHTS[s]))
                if (i + j) % 6 == 3 and (i - j) % 6 == 3:
                    p.px(xy, GOLD)
        return True
    if style == 'bellyknot':                               # a jade disc on the sash's knot
        gilt(p, f)
        fx, fy, fw, fh = f['front']
        circle(p, fx + fw / 2, fy + fh / 2, 0, fw / 2, (96, 178, 136))
        circle(p, fx + fw / 2, fy + fh / 2, 0, 1.0, (40, 90, 66))
        return True
    if style == 'feather':                                 # a pheasant's tail feather: barred brown and cream, a dark shaft
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                along = i if s in ('right', 'left') else j
                bar = (along // 3) % 2
                col = (246, 236, 210) if bar else (176, 112, 58)
                if s in ('right', 'left') and j in (fh // 2,):
                    col = (90, 60, 40)
                if s in ('right', 'left') and (j == 0 or j == fh - 1):
                    col = k(col, 0.8)
                p.px(xy, noisy(p, k(col, LIGHTS[s]), 4))
        return True
    if style == 'quiver':                                  # red leather, gold-mounted, a tiger's stripes on it
        p.shaded(f, (150, 34, 28))
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(4, fh - 2, 5):
                for i in range(fw):
                    if (i + j) % 4 != 0:
                        p.px((fx + i, fy + j), (60, 20, 16))
            rim(p, f[s], GOLD, 'lr')
        return True
    if style == 'quiver_mouth':
        gilt(p, f)
        fx, fy, fw, fh = f['top']
        for i, j, xy in each(f['top']):
            if 1 <= i < fw - 1 and 1 <= j < fh - 1:
                p.px(xy, (30, 14, 12))
        return True
    if style == 'arrowshaft':
        p.shaded(f, (196, 160, 108))
        return True
    if style == 'arrowhead':
        steel(p, f)
        return True
    if style == 'fletch':                                  # red and white fletching
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                p.px(xy, noisy(p, (210, 40, 34) if (i // 2) % 2 == 0 else (240, 236, 228), 4))
        return True
    if style == 'baldric':                                 # the baldric: red leather, gold studs
        for s, r in f.items():
            p.face(r, (126, 36, 28), LIGHTS[s], noise=3)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for j in range(1, fh, 4):
                p.px((fx + fw // 2, fy + j), GOLD_LIGHT)
        return True
    if style == 'bowgrip':                                 # the grip, wrapped in red silk cord
        p.shaded(f, (150, 30, 26))
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 2):
                for i in range(fw):
                    p.px((fx + i, fy + j), (96, 18, 16))
        return True
    if style == 'bowlimb':                                 # gilt lacquered horn, its back painted with a cloud scroll
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                along = i if s in ('right', 'left') else j
                col = mixc(GOLD_LIGHT, GOLD, 0.5) if (along // 4) % 3 else GOLD_DEEP
                p.px(xy, noisy(p, k(col, LIGHTS[s]), 3))
        return True
    if style == 'string':
        p.shaded(f, (236, 230, 214))
        return True
    return False


def archer_painter(c):
    def paint_box(p, style, f, dims):
        if paint_archer(p, style, f, dims, c) or paint_trooper(p, style, f, dims, c) or paint_arms(p, style, f, dims, c):
            return
        p.shaded(f, (255, 0, 255))
    return paint_box


# ── The thunder drummer ──────────────────────────────────────────────────────

# both fists before him, the sticks raised
D_ARM, D_FOREARM = (-0.3, 0, 0.16), (-1.0, -0.35, 0)


def drum(name, parent, pivot, rot, r, depth, tag):
    """A war drum, its barrel near round (three boxes crossed), a round head proud of it at either end."""
    a = r * 0.42
    return [part(name, parent, pivot, rot, [
        (-r, -a, -depth / 2, 2 * r, 2 * a, depth, 'barrel_w' + tag),
        (-a, -r, -depth / 2, 2 * a, 2 * r, depth, 'barrel_h' + tag),
        (-r * 0.84, -r * 0.84, -depth / 2 + 0.05, 2 * r * 0.84, 2 * r * 0.84, depth - 0.1, 'barrel_c' + tag),
        (-r + 0.2, -r + 0.2, -depth / 2 - 0.15, 2 * r - 0.4, 2 * r - 0.4, depth + 0.3, 'drumhead' + tag),
    ])]


def drum_frame():
    return ([part('drum_frame', 'body', (0, 0, 3.4), (0, 0, 0), [
        (-5.9, -25.5, 0, 1.0, 25.5, 1.0, 'post'), (4.9, -25.5, 0, 1.0, 25.5, 1.0, 'post'),
        (-6.6, -26.3, -0.1, 13.2, 1.2, 1.2, 'crossbar'),
        (-11.0, -13.6, 0.0, 22.0, 1.0, 1.0, 'crossbar'),
        (-6.3, -28.2, -0.2, 1.8, 2.0, 1.4, 'postcap'), (4.5, -28.2, -0.2, 1.8, 2.0, 1.4, 'postcap'),
    ])]
        + drum('drum_top', 'drum_frame', (0, -20.4, 2.0), (0, 0, 0), 5.4, 4.4, '')
        + drum('right_drum', 'drum_frame', (-10.4, -12.6, 0.6), (0, math.pi / 2, 0.08), 4.0, 3.2, '_small')
        + [part('right_pennant', 'drum_frame', (-5.4, -28.2, 0.5), (0, 0, 0), [(-0.1, -0.2, 0, 0.2, 4.4, 6.6, 'pennant')])])


def drummer_head():
    """No helmet: hair in a topknot with a gold pin, a red headband knotted behind, its tails streaming."""
    return [
        part('hair', 'head', (0, 0, 0), (0, 0, 0), [
            (-3.5, -7.4, -3.4, 7.0, 2.0, 7.0, 'haircap'),
            (-1.3, -9.6, -0.6, 2.6, 2.4, 2.6, 'topknot'),
            (-2.4, -8.8, 0.4, 4.8, 0.5, 0.5, 'hairpin'),
            (-3.6, -6.3, -3.6, 7.2, 1.3, 7.2, 'headband'),
            (-1.1, -6.6, -3.85, 2.2, 1.7, 0.4, 'bandplaque'),
        ]),
        part('band_tail', 'hair', (0, -5.8, 3.6), (0.5, 0, 0), [(-1.6, 0, -0.2, 1.4, 5.5, 0.4, 'bandtail'),
                                                               (0.2, 0, -0.2, 1.4, 4.6, 0.4, 'bandtail')]),
        part('right_brow', 'head', (-0.35, -5.05, -3.4), (0, 0.1, 0.32), [(-2.6, -0.5, -0.4, 2.6, 0.9, 0.7, 'brow')]),
        part('right_moustache', 'head', (-0.3, -2.25, -3.45), (0, 0.15, -0.45), [(-2.4, -0.35, -0.3, 2.4, 0.7, 0.6, 'moustache')]),
        part('beard', 'head', (0, -1.0, -3.3), (0.2, 0, 0), [(-2.3, 0, -0.6, 4.6, 4.8, 1.2, 'beard')]),
    ]


def sticks():
    return [part('right_stick', 'right_hand', (0, 1.2, 0), aim([D_ARM, D_FOREARM, (0, 0, 0)], (0.5, 0, 0)), [
        (-0.32, -7.6, -0.32, 0.64, 9.0, 0.64, 'stick'),
        (-0.85, -9.1, -0.85, 1.7, 1.7, 1.7, 'stickknob'),
        (-0.45, 1.2, -0.45, 0.9, 0.6, 0.9, 'stickend'),
    ])]


def drummer_parts():
    arms_ = strip(arms(sleeve='bare_upper'), ('pauldron2', 'pauldron3', 'elbowpuff'))
    arms_ = restyle(arms_, {'forearm': 'bare_forearm'})
    parts = (legs() + waist(skirt=5.0, apron=8.0) + body(mirrors=False) + arms_ + without(head(), ['right_brow']) + drummer_head()
             + drum_frame() + sticks())
    parts = restyle(parts, {'cuirass': 'padded', 'abdomen': 'padded_low', 'torso': 'padded_low', 'bellybeast': 'medallion'})
    parts = mirrored(parts)
    return pose(parts, {'right_arm': D_ARM, 'right_forearm': D_FOREARM, 'left_arm': tuple(-v if i else v for i, v in enumerate(D_ARM)),
                        'left_forearm': tuple(-v if i else v for i, v in enumerate(D_FOREARM))})


DRUMMER = drummer_parts()

DRUMMER_COLOURS = {
    False: colours(silk=(70, 58, 132), silk_deep=(30, 22, 66), trim=(196, 44, 36), plume=(204, 36, 32), plume_deep=(108, 14, 16),
                   lace=(196, 44, 36), beard=(34, 26, 24), skin=(222, 168, 124), skin_shade=(170, 112, 80)),
    True: colours(silk=(150, 24, 30), silk_deep=(70, 8, 14), trim=(236, 190, 76), plume=(232, 190, 76), plume_deep=(150, 102, 34),
                  lace=(120, 14, 18), cloak=(156, 22, 28), cloak_deep=(72, 8, 14), beard=(34, 26, 24), skin=(222, 168, 124),
                  skin_shade=(170, 112, 80), captain=True),
}


def thunder_head(p, rect, c, glow=True):
    """A drumhead: pale hide, a red rim of gold studs, the thunder spiral (雷纹) and a red-and-gold taiji at its heart."""
    fx, fy, fw, fh = rect
    cx, cy, R = fx + fw / 2, fy + fh / 2, min(fw, fh) / 2
    for y in range(fy, fy + fh):
        for x in range(fx, fx + fw):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy) / R
            if d > 1.0:
                p.clear((x, y))
                continue
            if d > 0.86:
                col = LACQUER_RED if (int(math.degrees(math.atan2(dy, dx))) // 24) % 2 else GOLD_LIGHT
                p.px((x, y), col)
                continue
            a = math.atan2(dy, dx)
            spiral = (d * 3.2 - a / (2 * math.pi)) % 1.0
            col = (236, 220, 186)
            if spiral < 0.22 and d > 0.3:
                col = (186, 40, 30)
            if d < 0.3:
                col = GOLD if dx > 0 else (186, 40, 30)
            p.px((x, y), noisy(p, mixc(col, (200, 180, 150), d * 0.3), 3))
            if glow and spiral < 0.22 and d > 0.3:
                p.lit((x, y), (255, 210, 120), glow=0.0)


def paint_drummer(p, style, f, dims, c):
    if style in ('bare_upper', 'bare_forearm'):            # bare arms, the muscle rounded, lit
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                a = abs((i + 0.5) / fw - 0.45) * 2
                t = 1.12 - 0.35 * a * a - (0.12 if (style == 'bare_upper' and 0.4 < j / fh < 0.55) else 0)
                p.px(xy, noisy(p, k(c['skin'], t * LIGHTS[s]), 2))
        return True
    if style in ('padded', 'padded_low'):                  # a padded coat, gold-studded in rows
        for s, r in f.items():
            fabric(p, r, c['silk'], c['silk_deep'], LIGHTS[s], 5, 0.3)
            if s in SIDES:
                fx, fy, fw, fh = r
                for j in range(2, fh, 4):
                    for i in range(1 + (j // 4) % 2 * 2, fw, 4):
                        p.px((fx + i, fy + j), GOLD_LIGHT)
                        p.px((fx + i, fy + j + 1), GOLD_DEEP)
        if style == 'padded':
            for s in SIDES:
                rim(p, f[s], GOLD, 't', 2)
        return True
    if style == 'medallion':                               # a thunder medallion: gold, the spiral
        gilt(p, f)
        thunder_head(p, f['front'], c, glow=False)
        return True
    if style in ('haircap', 'topknot'):
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                p.px(xy, noisy(p, mixc(HAIR, (70, 56, 56), ((i + j // 2) % 3) / 3), 2))
        if style == 'haircap':
            for s in ('front',):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    for j in range(fh - 1, fh):
                        p.px((fx + i, fy + j), c['skin'])
        return True
    if style == 'hairpin':
        gilt(p, f, 1.1)
        return True
    if style in ('headband', 'bandtail'):
        for s, r in f.items():
            fabric(p, r, c['plume'], c['plume_deep'], LIGHTS[s], 3, 0.3)
        if style == 'bandtail':
            for s in ('front', 'back'):
                ragged(p, f[s], teeth=2, depth=2)
        return True
    if style == 'bandplaque':
        gilt(p, f)
        fx, fy, fw, fh = f['front']
        p.px((fx + fw // 2, fy + fh // 2), (80, 200, 220))
        return True
    if style == 'beard':                                   # a short, full black beard
        lock(p, f, tone=BLUE_BLACK, keep=0.8, taper=0.45)
        return True
    if style in ('post', 'crossbar'):                      # red-lacquered wood, gold-banded
        p.shaded(f, LACQUER_RED)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            if style == 'post':
                for j in range(0, fh, 12):
                    for i in range(fw):
                        p.px((fx + i, fy + j), GOLD)
            else:
                for i in range(0, fw, 10):
                    for j in range(fh):
                        p.px((fx + i, fy + j), GOLD)
        return True
    if style == 'postcap':                                 # a gold flame finial
        gilt(p, f, 1.1)
        pointed(p, f, keep=0.4, sharp=0.9)
        return True
    if style == 'pennant':                                 # a little swallow-tailed pennant, the thunder sign on it
        bare(p, f, ('top', 'bottom', 'front', 'back'))
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in each(f[s]):
                s_ = (i + 0.5) / fw if s == 'left' else 1 - (i + 0.5) / fw      # 0 at the pole .. 1 at the tail
                t = (j + 0.5) / fh
                notch = s_ > 0.7 and abs(t - 0.5) < (s_ - 0.7) * 1.2
                if t > 1 - 0.55 * s_ or notch:
                    p.clear(xy)
                    continue
                p.px(xy, noisy(p, c['plume'] if (j > 1) else GOLD, 3))
        return True
    if style.startswith('barrel'):                         # the drum's barrel: red lacquer, gold studs in two rows
        for s, r in f.items():
            fx, fy, fw, fh = r
            p.face(r, LACQUER_RED, LIGHTS[s], noise=3, fall=0.2)
            if s in ('front', 'back'):
                continue
            n = fh if s in ('right', 'left') else fw
            for a in range(0, max(fw, fh), 3):
                for row in (1, (fh if s in ('top', 'bottom') else fw) - 2):
                    pass
            for i, j, xy in each(r):
                along = j if s in ('right', 'left') else i
                across = i if s in ('right', 'left') else j
                span = fw if s in ('right', 'left') else fh
                if across in (1, span - 2) and along % 3 == 1:
                    p.px(xy, GOLD_LIGHT)
                if across in (0, span - 1):
                    p.px(xy, GOLD_DEEP)
        return True
    if style.startswith('drumhead'):
        bare(p, f, ('top', 'bottom', 'right', 'left'))
        thunder_head(p, f['front'], c)
        thunder_head(p, f['back'], c)
        return True
    if style == 'stick':
        p.shaded(f, (150, 104, 60))
        return True
    if style in ('stickknob', 'stickend'):
        p.shaded(f, LACQUER_RED)
        for s in SIDES:
            rim(p, f[s], GOLD, 'tb')
        return True
    return False


def drummer_painter(c):
    def paint_box(p, style, f, dims):
        if paint_drummer(p, style, f, dims, c) or paint_trooper(p, style, f, dims, c) or paint_arms(p, style, f, dims, c):
            return
        p.shaded(f, (255, 0, 255))
    return paint_box
