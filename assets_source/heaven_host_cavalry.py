"""The cloud cavalry (云骑) (heaven_host.py builds it): one model, the steed and its rider.

The steed: a celestial horse, white with a cool blue shade, a long silver-white mane falling from its
crest and a forelock, a long flowing tail; gilt barding (a gilt chanfron with a red plume on its
brow, a gold collar of bells, a gilt peytral over its chest fringed in red, a gilt lamellar crupper
over its rump fringed in red tassels, a crimson saddlecloth worked in gold under a red saddle); gold
hooves, and from each fetlock a wisp of cloud trailing behind, faintly shining.

The rider: the heavenly soldier's armour and helmet (gilt mingguang armour over azure silk, a red
tassel), seated astride, the left fist on the reins, a long spear (枪) in his right with a red tassel
under its head and a swallow-tailed pennant, held slanting forward; winding up his charge, he
couches it level, leaning in, and the steed rears. His captain in crimson and gold, cloaked,
phoenix-winged."""
import math

from heaven_host_kit import *
from heaven_host import cape, plume_big, paint_arms
from heaven_host_ranks import strip, restyle

CAVALRY_SIZE = 1.05

R_ARM, R_FOREARM = (-0.15, 0, 0.12), (-0.75, -0.25, 0)       # the spear arm
L_ARM, L_FOREARM = (-0.35, 0, -0.08), (-0.9, 0.2, 0)        # the rein hand before him
SEAT = (0, -6.2, -0.8)                                      # the saddle's seat, in the horse's frame


def horse():
    out = [
        part('horse', 'root', (0, 8, 0), (0, 0, 0), [
            (-4.2, -4.5, -8.4, 8.4, 9.0, 16.8, 'barrel'),
            (-3.8, -4.1, -11.0, 7.6, 8.2, 3.0, 'h_chest'),
            (-4.0, -4.9, 6.0, 8.0, 8.4, 4.4, 'h_rump'),
            (-3.4, 4.1, -7.5, 6.8, 1.4, 13.0, 'h_belly'),
            # the barding: peytral, saddlecloth, saddle, crupper
            (-4.5, -2.6, -11.7, 9.0, 5.2, 1.2, 'peytral'),
            (-4.75, -4.75, -4.6, 9.5, 6.6, 8.6, 'saddlecloth'),
            (-3.0, -6.0, -3.8, 6.0, 1.5, 6.4, 'saddle'),
            (-1.8, -7.6, -4.2, 3.6, 1.8, 1.0, 'saddlebow'),
            (-2.4, -7.8, 2.4, 4.8, 2.0, 1.0, 'saddlebow'),
            (-4.6, -5.3, 4.6, 9.2, 4.6, 6.2, 'crupper'),
        ]),
        part('peytral_fringe', 'horse', (0, 2.5, -11.6), (0.1, 0, 0), [(-4.2, 0, -0.35, 8.4, 2.6, 0.7, 'fringe')]),
        part('right_crupper_fringe', 'horse', (-4.65, -0.8, 7.6), (0, 0, 0.08), [(-0.35, 0, -3.0, 0.7, 3.0, 6.0, 'fringe_side')]),
        part('right_cloth_fringe', 'horse', (-4.8, 1.8, -0.3), (0, 0, 0.06), [(-0.3, 0, -4.2, 0.6, 2.4, 8.4, 'fringe_side')]),
        # the neck rising from the chest, the head on it
        part('neck', 'horse', (0, -2.4, -9.0), (0.62, 0, 0), [
            (-2.3, -12.4, -2.7, 4.6, 13.4, 5.4, 'horse_neck'),
            (-2.6, -1.4, -3.0, 5.2, 2.2, 6.0, 'bell_collar'),
        ]),
        part('horse_head', 'neck', (0, -11.8, -0.6), (0.95, 0, 0), [
            (-2.4, -2.9, -3.2, 4.8, 5.0, 5.4, 'h_skull'),
            (-1.9, -2.3, -11.0, 3.8, 4.2, 8.0, 'h_face'),
            (-2.15, -1.7, -13.2, 4.3, 3.7, 2.4, 'h_muzzle'),
            (-1.75, 1.6, -10.6, 3.5, 1.6, 8.0, 'h_jaw'),
            (-1.55, -2.85, -10.8, 3.1, 0.7, 7.6, 'chanfron'),
            (-0.7, -5.4, -2.4, 1.4, 2.8, 1.4, 'chanfron_plume'),
        ]),
        part('right_ear', 'horse_head', (-1.5, -2.6, -0.2), (0.2, 0, -0.25), [(-0.5, -2.6, -0.6, 1.0, 2.8, 1.2, 'h_ear')]),
        part('forelock', 'horse_head', (0, -2.9, -2.6), (-1.0, 0, 0), [(-1.2, 0, -0.5, 2.4, 3.6, 1.0, 'mane_a')]),
        # the tail, long and flowing
        part('tail', 'horse', (0, -3.6, 10.2), (0.45, 0, 0), [(-1.2, -0.4, -1.2, 2.4, 6.0, 2.4, 'tail_root')]),
        part('tail_mid', 'tail', (0, 5.4, 0), (-0.1, 0, 0), [(-1.8, 0, -1.5, 3.6, 7.5, 3.0, 'tail_long')]),
        part('tail_end', 'tail_mid', (0, 7.1, 0), (-0.08, 0, 0), [(-1.5, 0, -1.2, 3.0, 6.5, 2.4, 'tail_end')]),
    ]
    # the mane down the crest of the neck, falling to the right and left by turns
    for kk in range(7):
        out.append(part('mane_%d' % kk, 'neck', ((-1.0 if kk % 2 else 1.0), -12.2 + 1.9 * kk, 1.8), (0.3, 0, (0.4 if kk % 2 else -0.4)),
                        [(-1.2, 0, -1.1, 2.4, 7.0 - 0.3 * kk, 2.6, 'mane_a' if kk % 2 else 'mane_b')]))
    # the legs: forelegs and hind legs, gold hooves, a wisp of cloud trailing each
    def lower(name, parent, pivot, length):
        return part(name, parent, pivot, (0, 0, 0), [
            (-0.95, 0, -1.05, 1.9, length, 2.1, 'h_shin'),
            (-1.15, length - 0.4, -1.25, 2.3, 1.2, 2.5, 'h_fetlock'),
            (-1.25, length + 0.6, -1.45, 2.5, 1.4, 2.8, 'hoof'),
            (-1.9, length - 1.3, -0.4, 3.8, 2.6, 5.6, 'wisp'),
        ])
    out += [
        part('right_foreleg', 'horse', (-2.4, 3.2, -7.4), (0, 0, 0), [(-1.4, -1.0, -1.7, 2.8, 7.0, 3.4, 'h_upper')]),
        lower('right_foreshin', 'right_foreleg', (0, 5.8, 0), 4.6),
        part('right_hindleg', 'horse', (-2.4, 2.4, 7.2), (0, 0, 0), [(-1.6, -1.6, -2.1, 3.2, 7.6, 4.4, 'h_thigh')]),
        lower('right_hindshin', 'right_hindleg', (0, 5.6, 0.6), 4.8),
    ]
    return out


def rider():
    """The soldier's frame seated: his legs astride the saddle, the spear in his right fist, the reins in his left."""
    parts = waist(skirt=6.0, apron=6.5) + legs() + body() + arms() + head() + helm(plume=True, wings=True) + cape(length=8.0, lower=6.5) + plume_big()
    parts = without(parts, ['robe_back'])
    out = [part('rider', 'horse', SEAT, (0, 0, 0), [])]
    for n, par, piv, rot, boxes, m in parts:
        if n == 'waist':
            par, piv = 'rider', (0, -1.6, 0)
        if n == 'right_leg':                               # astride: the thigh forward and out over the saddle, the shin hanging
            par, piv, rot = 'waist', (-2.4, 0.2, 0), (-1.25, 0, 0.42)
        if n == 'right_shin':
            rot = (1.35, 0, -0.25)
        out.append((n, par, piv, rot, boxes, m))
    out.append(part('right_moustache', 'head', (-0.3, -2.25, -3.45), (0, 0.15, -0.35), [(-2.2, -0.35, -0.3, 2.2, 0.6, 0.6, 'moustache')]))
    # the spear: a long red shaft, a steel head over a red tassel, a pennant below it
    out += [
        part('lance', 'right_hand', (0, 1.2, 0), aim([R_ARM, R_FOREARM, (0, 0, 0)], (0.7, 0, 0)), [
            (-0.45, -34, -0.45, 0.9, 44, 0.9, 'shaft'),
            (-0.8, -35.4, -0.8, 1.6, 1.6, 1.6, 'collar'),
            (-0.95, -42.6, -0.3, 1.9, 7.4, 0.6, 'spearhead'),
            (-0.5, 9.6, -0.5, 1.0, 2.4, 1.0, 'butt'),
        ]),
        part('lance_tassel', 'lance', (0, -34.2, 0), (0, 0, 0), [(-1.1, 0, -1.1, 2.2, 2.6, 2.2, 'tassel'),
                                                                 (-0.8, 2.3, -0.8, 1.6, 1.6, 1.6, 'tassel_tip')]),
        part('pennant', 'lance', (0, -31.5, 0.4), (0, 0, 0), [(-0.1, 0, 0, 0.2, 5.0, 8.5, 'pennant')]),
    ]
    out = mirrored(out)
    return pose(out, {'right_arm': R_ARM, 'right_forearm': R_FOREARM, 'left_arm': L_ARM, 'left_forearm': L_FOREARM})


def cavalry_parts():
    return mirrored(horse()) + rider()


CAVALRY = cavalry_parts()

CAVALRY_COLOURS = {
    False: colours(silk=(56, 100, 172), silk_deep=(22, 40, 92), trim=(196, 44, 36), plume=(214, 38, 32), plume_deep=(112, 14, 16),
                   lace=(184, 38, 34), saddle=(170, 30, 30), saddle_deep=(90, 12, 14)),
    True: colours(silk=(168, 28, 32), silk_deep=(84, 10, 16), trim=(236, 190, 76), plume=(226, 36, 30), plume_deep=(118, 12, 14),
                  lace=(120, 14, 18), cloak=(156, 22, 28), cloak_deep=(72, 8, 14), moustache=True, captain=True, jewel=(80, 200, 220),
                  saddle=(120, 20, 26), saddle_deep=(60, 8, 12)),
}

COAT = ((150, 160, 190), (226, 232, 244), (252, 252, 255))     # the steed's white coat: shadow, body, light
MANE = ((150, 160, 188), (220, 226, 240), (255, 255, 255))


def coat(p, f, light=1.0, dapple=True):
    """A white coat: lit above, rounded across each face, a cool blue shadow beneath, faint dapples."""
    for s, r in f.items():
        fx, fy, fw, fh = r
        for i, j, xy in each(r):
            a = abs((i + 0.5) / fw - 0.5) * 2
            t = 1.15 - 0.35 * a * a
            if s in SIDES:
                t -= 0.45 * (j / max(1, fh - 1)) ** 1.5
            t = t * LIGHTS[s] * light
            col = mixc(COAT[0], COAT[1], max(0, min(1, (t - 0.45) / 0.55))) if t < 1 else mixc(COAT[1], COAT[2], min(1, (t - 1) * 3))
            if dapple and (i * 5 + j * 3) % 11 == 0 and (i + j * 2) % 7 < 2:
                col = k(col, 0.94)
            p.px(xy, noisy(p, col, 2))


def mane_paint(p, f, light=1.0):
    for s, r in f.items():
        fx, fy, fw, fh = r
        for i in range(fw):
            tone = p.rnd.uniform(0.86, 1.08)
            for j in range(fh):
                t = j / max(1, fh - 1)
                col = MANE[2] if i % 3 == 0 else MANE[1]
                col = mixc(col, MANE[0], 0.4 * t + (0.25 if i % 3 == 2 else 0))
                p.px((fx + i, fy + j), noisy(p, k(col, tone * LIGHTS[s] * light), 3))
        if s in SIDES:
            ragged(p, r, teeth=2, depth=2)


def cloud_wisp(p, f):
    """A wisp of cloud: soft round puffs, white lit to blue-grey, ragged; faintly shining."""
    for s, r in f.items():
        fx, fy, fw, fh = r
        for i, j, xy in each(r):
            u, v = (i + 0.5) / fw, (j + 0.5) / fh
            if s in ('right', 'left'):
                along = u if s == 'left' else 1 - u                          # 0 at the hoof .. 1 at the tail of the wisp
                puff = 0.5 + 0.22 * math.cos(along * 9) - along * 0.35
                inside = abs(v - 0.55) < puff
            else:
                inside = (u - 0.5) ** 2 + (v - 0.5) ** 2 < 0.2 + 0.05 * math.sin(i * 1.7)
            if not inside or p.rnd.random() < 0.06:
                p.clear(xy)
                continue
            col = mixc(CLOUD, CLOUD_SHADE, min(1, v * 0.9 + p.rnd.uniform(0, 0.15)))
            p.lit(xy, col, glow=0.28, skin=noisy(p, col, 3))


def paint_horse(p, style, f, dims, c):
    if style in ('barrel', 'h_chest', 'h_rump', 'h_belly', 'horse_neck', 'h_upper', 'h_thigh', 'h_shin', 'h_skull', 'h_face', 'h_jaw'):
        coat(p, f)
        if style == 'h_skull':                             # the eye: large, dark, a gold glint; under the brow
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                ex = fx + (2 if s == 'right' else fw - 5)
                for i in range(3):
                    p.px((ex + i, fy + 3), (90, 96, 120))
                    p.px((ex + i, fy + 4), (24, 20, 30))
                    p.px((ex + i, fy + 5), (40, 34, 46))
                p.lit((ex + (0 if s == 'right' else 2), fy + 4), (255, 214, 120), glow=0.6)
        if style == 'h_face':                              # the bridle: gold straps across the face
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    p.px((fx + fw // 2 + (1 if s == 'left' else -1), fy + j), GOLD)
                for i in range(fw):
                    p.px((fx + i, fy + fh - 3), GOLD_DEEP)
        return True
    if style == 'h_muzzle':                                # a soft grey-pink muzzle, dark nostrils
        coat(p, f, 0.95, dapple=False)
        fx, fy, fw, fh = f['front']
        for i, j, xy in each(f['front']):
            p.px(xy, noisy(p, mixc((210, 190, 196), (150, 130, 140), j / fh), 2))
        for side in (1, fw - 3):
            p.px((fx + side, fy + 2), (50, 36, 44))
            p.px((fx + side + 1, fy + 2), (50, 36, 44))
            p.px((fx + side, fy + 3), (70, 54, 60))
        for i in range(1, fw - 1):
            p.px((fx + i, fy + fh - 1), (90, 70, 80))
        return True
    if style == 'h_ear':
        coat(p, f, dapple=False)
        fx, fy, fw, fh = f['front']
        for j in range(1, fh - 1):
            p.px((fx + fw // 2, fy + j), (190, 160, 170))
        pointed(p, f, keep=0.4, sharp=0.6)
        return True
    if style == 'h_fetlock':                               # white feathering over the hoof
        mane_paint(p, f)
        return True
    if style == 'hoof':                                    # gold hooves
        gilt(p, f, 1.05)
        return True
    if style == 'wisp':
        cloud_wisp(p, f)
        return True
    if style in ('mane_a', 'mane_b', 'tail_root', 'tail_long', 'tail_end'):
        mane_paint(p, f, 1.0 if style != 'mane_b' else 0.94)
        return True
    if style in ('peytral', 'crupper'):                    # gilt lamellar barding, red-laced, a gold rim
        lamellar_box(p, f, c, pw=3, ph=4)
        if style == 'peytral':
            fx, fy, fw, fh = f['front']
            beast_mask(p, (fx + fw // 2 - 4, fy + 1, 8, 7), ring=True)
        return True
    if style in ('fringe', 'fringe_side'):                 # red tassels hanging from the barding
        for s, r in f.items():
            horsehair(p, r, c['plume'], c['plume_deep'], LIGHTS[s], cut=0.45 if s in SIDES else 0)
        for s in SIDES:
            rim(p, f[s], GOLD, 't')
        return True
    if style == 'saddlecloth':                             # crimson silk, a gold key-fret border, clouds
        for s, r in f.items():
            fabric(p, r, c['saddle'], c['saddle_deep'], LIGHTS[s], 5, 0.3)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            key_fret(p, (fx, fy + fh - 4, fw, 4))
            cloud_scrolls(p, (fx + 2, fy + 1, fw - 4, fh - 6), GOLD, every=8, r=1, share=0.9)
        return True
    if style in ('saddle', 'saddlebow'):
        p.shaded(f, (150, 30, 26))
        for s in SIDES:
            rim(p, f[s], GOLD, 'tb')
        return True
    if style == 'bell_collar':                             # a red collar hung with gold bells
        for s, r in f.items():
            p.face(r, c['plume'], LIGHTS[s], noise=3)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(1, fw - 1, 3):
                p.px((fx + i, fy + fh - 2), GOLD_LIGHT)
                p.px((fx + i, fy + fh - 1), GOLD_DEEP)
        return True
    if style == 'chanfron':                                # the gilt face plate
        gilt_rounded(p, f, 1.05)
        fx, fy, fw, fh = f['top']
        for j in range(1, fh - 1, 3):
            p.px((fx + fw // 2, fy + j), (220, 40, 40))
        return True
    if style == 'chanfron_plume':
        for s, r in f.items():
            horsehair(p, r, c['plume'], c['plume_deep'], LIGHTS[s], down=False)
        return True
    if style == 'pennant':                                 # a swallow-tailed pennant: the field, a gold border, a gold cloud
        bare(p, f, ('top', 'bottom', 'front', 'back'))
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in each(f[s]):
                s_ = (i + 0.5) / fw if s == 'left' else 1 - (i + 0.5) / fw     # 0 at the shaft .. 1 at the tails
                t = (j + 0.5) / fh
                if s_ > 0.62 and abs(t - 0.5) < (s_ - 0.62) * 1.3:
                    p.clear(xy)
                    continue
                edge = t < 0.12 or t > 0.88 or s_ < 0.06
                p.px(xy, noisy(p, GOLD if edge else c['plume'], 3))
            cloud(p, fx + fw // 2 - 3, fy + fh // 2, GOLD_LIGHT, r=1, rect=f[s])
        return True
    return False


def cavalry_painter(c):
    def paint_box(p, style, f, dims):
        if paint_horse(p, style, f, dims, c) or paint_trooper(p, style, f, dims, c) or paint_arms(p, style, f, dims, c):
            return
        p.shaded(f, (255, 0, 255))
    return paint_box
