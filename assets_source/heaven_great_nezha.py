"""Nezha, the Third Lotus Prince (哪吒三太子) (heaven_great.py says how the great names are made). A youth
of the lotus: a round young face, bright fierce eyes under bold brows, a red cinnabar mark on his brow,
his hair in two buns bound with red ribbons that stream from them; a red bodice (肚兜) edged in gold with
a lotus worked on it, a gold neck-ring and longevity lock, a green silk sash, a skirt of lotus petals in
two tiers, pink to their red tips; bare arms with gold armlets and bracelets, lotus petals over his
shoulders; white trousers, red leg-wraps, red shoes. He stands on the Wind-Fire Wheels (风火轮), one
under each foot, hovering: gold-rimmed spoked wheels spinning, each wreathed in tongues of flame; the
Universe Ring (乾坤圈), gold, ringing his left arm; the Huntian sash (混天绫), long red silk, looping up
behind his head and streaming away from it; in his right hand the Fire-Tipped Spear (火尖枪), its
leaf-blade wreathed in flame, a red tassel under it. Winding up, his wheels spin and flare and he raises
his spear.

Run from the repository root: python assets_source/heaven_great.py nezha"""
import math

import beastgen as bg
import heaven_great_kit as kit
from heaven_great_kit import (part, mirrored, aim, paint, emit, report, k, mix, clamp, each, noisy, lock, point,
                              GOLD, GOLD_DEEP, GOLD_LIGHT, GOLD_SHADOW, IVORY, HAIR, RED, RED_DEEP, JADE, JADE_DEEP,
                              FIRE, FIRE_DEEP, FIRE_HOT, FIRE_WHITE, SIDES, LIGHT, BLUE_BLACK)
from heaven_great import hang, bare, ribbon_arc

SIZE = 0.74       # HeavenGreatRenderers.NEZHA_SIZE

ARM, FOREARM = (-0.12, 0, 0.16), (-0.85, -0.2, 0)
LARM, LFOREARM = (-0.35, 0, -0.35), (-0.9, 0.35, 0)

SKIN = (246, 206, 170)
SASH = (214, 28, 30)
SASH_DEEP = (120, 10, 16)
PINK = (250, 170, 190)
PETAL_TIP = (226, 52, 88)
GREEN = (60, 150, 90)
WHEEL_R = 4.8
WHEEL_Y = 24 - WHEEL_R - 1.4


def wheel(side):
    """A Wind-Fire Wheel under a foot: a gold rim of twelve segments, six spokes, a hub, upright along his
    way, spinning (its spokes and rim under `*_wheel_spin`); tongues of flame round it, trailing back."""
    x = -2.8 if side == 'right' else 2.8
    name = side + '_wheel'
    out = [part(name, 'root', (x, WHEEL_Y, 0.6), (0, 0.6 if side == 'right' else -0.6, 0.1 if side == 'right' else -0.1), []),
           part(name + '_spin', name, (0, 0, 0), (0, 0, 0), [
               (-1.0, -1.3, -1.3, 2.0, 2.6, 2.6, 'wh_hub'),
               (-0.35, -WHEEL_R, -0.35, 0.7, 2 * WHEEL_R, 0.7, 'wh_spoke'),
           ])]
    for i, a in enumerate((60, 120)):
        out.append(part('%s_spoke_%d' % (name, i), name + '_spin', (0, 0, 0), (math.radians(a), 0, 0),
                        [(-0.35, -WHEEL_R, -0.35, 0.7, 2 * WHEEL_R, 0.7, 'wh_spoke')]))
    n = 12
    chord = 2 * WHEEL_R * math.sin(math.pi / n) + 0.5
    for i in range(n):
        a = 2 * math.pi * (i + 0.5) / n
        out.append(part('%s_rim_%d' % (name, i), name + '_spin', (0, -WHEEL_R * math.cos(a), -WHEEL_R * math.sin(a)), (a + math.pi, 0, 0),
                        [(-0.75, -0.65, -chord / 2, 1.5, 1.3, chord, 'wh_rim')]))
    for i in range(10):                                  # the flames, licking out round it and streaming back
        a = 2 * math.pi * i / 10
        R = WHEEL_R + 0.3
        tall = 5.0 if i % 2 == 0 else 3.6
        st = 'wh_flame' if i % 2 == 0 else 'wh_flame_s'
        out.append(part('%s_flame_%d' % (name, i), name, (0, -R * math.cos(a), -R * math.sin(a)), (a - 0.75, 0, 0),
                        [(-0.25, -tall, -1.6, 0.5, tall, 3.2, st), (-1.0, -tall, -0.25, 2.0, tall, 0.5, st + 'x')]))
    return out


def lotus_skirt():
    out = []
    for i in range(8):                                   # the upper tier: long petals, pink to red tips
        th = 2 * math.pi * (i + 0.5) / 8
        out.append(part('petal_%d' % i, 'waist', (-3.6 * math.sin(th), -0.6, -2.7 * math.cos(th)), (-0.42, th, 0),
                        [(-1.9, 0, -0.25, 3.8, 6.2, 0.5, 'petal')]))
    for i in range(8):                                   # the lower: shorter, between them, flaring out more
        th = 2 * math.pi * i / 8
        out.append(part('petal_low_%d' % i, 'waist', (-3.2 * math.sin(th), 2.4, -2.4 * math.cos(th)), (-0.62, th, 0),
                        [(-1.6, 0, -0.25, 3.2, 4.6, 0.5, 'petal_low')]))
    return out


def ring():
    """The Universe Ring about his left arm: gold, twelve segments, tilted."""
    out = [part('qiankun', 'left_arm', (0.2, 2.2, 0), (0.35, 0, -0.5), [])]
    n, R = 12, 3.7
    L = 2 * R * math.sin(math.pi / n) + 0.4
    for i in range(n):
        th = 2 * math.pi * (i + 0.5) / n
        out.append(part('qiankun_%d' % i, 'qiankun', (R * math.cos(th), 0, R * math.sin(th)), (0, -th - math.pi / 2, 0),
                        [(-L / 2, -0.65, -0.65, L, 1.3, 1.3, 'ring')]))
    return out


def spear():
    return [part('spear', 'right_hand', (0, 1.2, 0), aim([ARM, FOREARM], (-0.12, 0, 0.08)), [
        (-0.5, -22, -0.5, 1.0, 34, 1.0, 'sp_shaft'),
        (-0.8, -23.2, -0.8, 1.6, 1.4, 1.6, 'sp_collar'),
        (-1.2, -25.0, -1.2, 2.4, 1.9, 2.4, 'sp_mouth'),
        (-1.1, -32.4, -0.3, 2.2, 7.6, 0.6, 'sp_blade'),
        (-0.7, 11.6, -0.7, 1.4, 1.6, 1.4, 'sp_butt'),
    ]),
        part('spear_fire', 'spear', (0, -27.6, 0), (0, 0, 0), [(-3.2, -8, -0.25, 6.4, 9, 0.5, 'sp_fire'),
                                                               (-0.25, -8, -3.2, 0.5, 9, 6.4, 'sp_firex')]),
    ] + hang('spear_tassel', 'spear', (0, -22.4, 0), (0, 0, 0), [(1.0, 1.4, 1.0, 0), (2.2, 3.6, 2.2, 0.0)], 'sp_tassel')


NEZHA = mirrored([
    part('waist', 'root', (0, 3.6, 0), (0, 0, 0), [
        (-3.8, -1.8, -2.6, 7.6, 2.4, 5.2, 'waistband'),
        (-3.4, 0, -2.3, 6.8, 2.6, 4.6, 'hips'),
        (-1.1, -2.4, -3.2, 2.2, 2.0, 1.0, 'waistknot'),
    ]),
] + hang('right_waisttail', 'waist', (-0.6, -0.6, -3.0), (-0.2, 0, 0.15), [(1.2, 3.2, 0.4, 0), (1.4, 3, 0.4, 0.1)], 'waisttail') + [
    part('right_leg', 'root', (-2.0, 3.6, 0.1), (-0.12, 0, 0.04), [(-1.7, 0, -1.7, 3.4, 5.4, 3.4, 'thigh')]),
    part('right_shin', 'right_leg', (0, 5.2, 0), (0.12, 0, 0), [
        (-1.6, 0, -1.6, 3.2, 4.8, 3.2, 'shin'),
        (-1.7, 3.4, -2.9, 3.4, 1.6, 4.6, 'shoe'),
    ]),
    part('body', 'waist', (0, -1.6, 0), (0, 0, 0), [
        (-3.6, -7.2, -2.3, 7.2, 7.4, 4.6, 'torso'),
        (-2.9, -6.6, -2.7, 5.8, 6.8, 0.5, 'bodice'),
        (-2.9, -8.2, -2.7, 5.8, 1.3, 5.4, 'necklace'),
        (-1.1, -7.2, -3.1, 2.2, 1.9, 0.5, 'pendant'),
        (-1.4, -8.8, -1.4, 2.8, 1.8, 2.8, 'neck'),
    ]),
    part('right_shoulder_petal', 'body', (-3.4, -6.9, 0), (0, 0, 0.55), [(-3.6, -0.3, -2.0, 3.6, 0.6, 4.0, 'shoulder_petal')]),
    part('right_shoulder_petal_b', 'body', (-3.0, -7.2, 0.2), (0, 0.6, 0.85), [(-3.0, -0.3, -1.6, 3.0, 0.6, 3.2, 'shoulder_petal_s')]),
    part('head', 'body', (0, -8.6, -0.2), (0, 0, 0), [
        (-3.5, -7.2, -3.3, 7.0, 7.0, 6.6, 'face'),
        (-3.7, -3.4, -2.6, 7.4, 2.6, 5.0, 'cheek'),
        (-0.6, -3.7, -3.8, 1.2, 1.3, 0.6, 'nose'),
        (-3.7, -7.7, -3.1, 7.4, 2.0, 6.8, 'hair_cap'),
        (-3.7, -7.0, 0.3, 7.4, 6.2, 3.4, 'hair_back'),
        (-3.6, -6.0, -3.6, 7.2, 1.2, 0.6, 'bangs'),
    ]),
    part('right_brow', 'head', (-0.5, -4.85, -3.45), (0, 0.1, 0.28), [(-2.8, -0.45, -0.3, 2.8, 0.8, 0.5, 'brow')]),
    part('right_bun', 'head', (-2.7, -7.2, -0.2), (0, 0, -0.45), [
        (-1.5, -3.0, -1.5, 3.0, 3.0, 3.0, 'bun'),
        (-1.8, -0.9, -1.8, 3.6, 0.9, 3.6, 'bun_tie'),
    ]),
] + hang('right_bun_ribbon', 'right_bun', (-1.4, -0.6, 1.2), (0.6, 0, 0.5), [(0.9, 3.4, 0.3, 0), (1.0, 3.2, 0.3, 0.2)], 'bun_ribbon') + [
    part('right_arm', 'body', (-4.6, -6.4, 0), ARM, [
        (-1.3, -0.6, -1.3, 2.6, 4.8, 2.6, 'upperarm'),
        (-1.5, 1.6, -1.5, 3.0, 0.9, 3.0, 'armlet'),
    ]),
    part('right_forearm', 'right_arm', (0, 4.4, 0), FOREARM, [
        (-1.2, 0, -1.2, 2.4, 4.2, 2.4, 'forearm'),
        (-1.45, 2.8, -1.45, 2.9, 1.1, 2.9, 'bracelet'),
    ]),
    part('right_hand', 'right_forearm', (0, 4.0, 0), (0, 0, 0), [(-1.2, 0, -1.2, 2.4, 2.4, 2.4, 'hand')]),
] + lotus_skirt())


def pose_left(parts):
    out = []
    for name, parent, pivot, rot, boxes, mirror in parts:
        if name == 'left_arm':
            rot = LARM
        elif name == 'left_forearm':
            rot = LFOREARM
        out.append((name, parent, pivot, rot, boxes, mirror))
    return out


def sash():
    """The Huntian sash: a loop of red silk up behind his head, leaning back, its ends streaming away."""
    out = [part('sash', 'body', (0, -6.5, 3.4), (0.45, 0, 0), [])]
    out += ribbon_arc('sash', -3.0, 0, 8.6, -125, 125, 13, 2.4, 'sash_arc')
    for side, sgn in (('right', -1), ('left', 1)):
        a = math.radians(125)
        out += hang('%s_sash_end' % side, 'sash', (sgn * 8.6 * math.sin(a), -3.0 - 8.6 * math.cos(a) + 0.3, 0),
                    (0.5, 0, -sgn * 0.35), [(2.4, 4.6, 0.5, 0), (2.5, 4.6, 0.5, (0.15, 0, -sgn * 0.1)), (2.6, 4.6, 0.5, (0.2, 0, -sgn * 0.12)),
                                            (2.7, 4.4, 0.5, (0.2, 0, -sgn * 0.1)), (2.8, 4.0, 0.5, (0.15, 0, -sgn * 0.08)),
                                            (3.0, 3.6, 0.5, (0.1, 0, -sgn * 0.06))], 'sash_end')
    return out


NEZHA = pose_left(NEZHA) + wheel('right') + wheel('left') + ring() + spear() + sash()


def skin():
    skin_dark = kit.skin_shade(SKIN, 0.4)

    def flame(p, rect, glow=0.9, keep_core=True):
        fx, fy, fw, fh = rect
        for i, j, xy in each(p, rect):
            t = 1 - (j + 0.5) / fh                      # 0 at its root .. 1 at its tip
            u = (i + 0.5) / fw - 0.5
            half = 0.5 * (1 - t ** 1.3) + 0.04
            sway = 0.12 * math.sin(t * 4) * t
            d = abs(u - sway)
            if d > half:
                p.clear(xy)
                continue
            core = d < half * 0.45 and t < 0.8
            c = mix(FIRE_WHITE, FIRE_HOT, t * 1.2) if core else mix(FIRE_HOT, FIRE_DEEP, clamp(t * 0.9 + d))
            p.lit(xy, c, glow, blaze=1.0, hot=mix(FIRE_WHITE, c, 0.4))

    def paint_box(p, style, f, dims):
        if style in ('waistband', 'waistknot'):         # green silk wound about him, knotted at the front
            kit.silk(p, f, GREEN, k(GREEN, 0.5), fold=3)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), GOLD)
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
        if style == 'waisttail':
            kit.ribbon(p, f, GREEN, k(GREEN, 0.5))
        if style in ('hips', 'thigh'):                  # white trousers, loose
            kit.silk(p, f, (240, 236, 228), (180, 170, 170), fold=4)
        if style == 'shin':                             # red leg-wraps, crossed with gold cord
            kit.silk(p, f, (200, 40, 40), (110, 16, 20), fold=3)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    for i in range(fw):
                        if (i + j) % 5 == 0 or (i - j) % 5 == 0:
                            p.px((fx + i, fy + j), GOLD)
        if style == 'shoe':                             # red shoes, a gold toe
            p.shaded(f, (170, 24, 26))
            kit.gold_rim(p, f, top=True, bottom=False)
            fx, fy, fw, fh = f['front']
            kit.wp.disc(p, fx + fw / 2, fy + fh / 2, 1.2, GOLD)

        if style == 'torso':                            # bare young skin; the bodice's red ties across his back
            kit.flesh(p, f, SKIN, round_=0.5)
            fx, fy, fw, fh = f['back']
            for i in range(fw):
                p.px((fx + i, fy + 3), RED)
                p.px((fx + i, fy + fh - 3), RED)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + 3), RED)
                    p.px((fx + i, fy + fh - 3), RED)
        if style == 'bodice':                           # the red bodice: a diamond, gold-edged, a lotus on it
            for s, r in f.items():
                p.face(r, (200, 30, 34), LIGHT[s], noise=3)
            for s in ('front',):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh
                    if v < 0.25 and abs(u) > v * 1.6 + 0.12:
                        p.clear(xy)                      # cut to a point at the throat
                        continue
                    edge = v < 0.25 and abs(u) > v * 1.6 + 0.02 or i in (0, fw - 1) or j == fh - 1
                    c = GOLD if edge else mix((220, 40, 40), (160, 20, 26), v)
                    p.px(xy, noisy(p, c, 3))
                cx, cy = fx + fw / 2, fy + fh * 0.55        # a lotus in gold and pink
                for a in range(5):
                    ang = math.radians(-90 + (a - 2) * 32)
                    for r in range(4):
                        x = int(cx + math.cos(ang) * r * 0.9)
                        y = int(cy + math.sin(ang) * r)
                        p.px((x, y), PINK if r < 3 else GOLD_LIGHT)
                for i in range(-3, 3):
                    p.px((int(cx) + i, int(cy) + 1), GOLD)
        if style == 'necklace':                         # the gold neck-ring
            kit.gold(p, f, round_=0.8)
            for s in SIDES:
                kit.studs(p, f[s], 1, every=3)
        if style == 'pendant':                          # the longevity lock (长命锁)
            kit.gold(p, f)
            fx, fy, fw, fh = f['front']
            kit.wp.disc(p, fx + fw / 2, fy + fh / 2, 1.0, (230, 40, 40))
        if style == 'neck':
            kit.flesh(p, f, SKIN, round_=0.5)
        if style in ('shoulder_petal', 'shoulder_petal_s'):     # lotus petals over his shoulders
            for s, r in f.items():
                p.face(r, PINK, LIGHT[s], noise=3)
            for s in ('top', 'bottom'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u, v = (i + 0.5) / fw, (j + 0.5) / fh - 0.5    # u 0 outer tip (-x) .. 1 at the shoulder
                    half = 0.5 * math.sin(clamp(u) * math.pi) ** 0.6
                    if abs(v) > half:
                        p.clear(xy)
                        continue
                    c = mix(PETAL_TIP, PINK, clamp(u * 1.8))
                    c = mix(c, (255, 240, 240), clamp(0.3 - abs(v)))
                    if abs(v) < 0.04:
                        c = k(c, 0.85)
                    p.px(xy, noisy(p, c, 3))
            bare(p, f, ('front', 'back', 'right', 'left'))

        if style == 'face':                             # a round young face, eyes bright and fierce, the cinnabar mark
            kit.flesh(p, f, SKIN, round_=0.5)
            fx, fy, fw, fh = f['front']                 # 14x14
            for side in (0, 1):                         # big eyes, a fierce slant, dark irises with a catch-light
                ex = fx + (2 if side == 0 else fw - 6)
                ey = fy + 6
                for i in range(4):
                    slope = (1 if i == 0 else 0) if side == 0 else (1 if i == 3 else 0)
                    p.px((ex + i, ey - 1 + slope), (30, 16, 18))
                    p.px((ex + i, ey + slope), (244, 240, 236) if i in ((0,) if side == 0 else (3,)) else (40, 24, 30))
                    p.px((ex + i, ey + 1), (244, 240, 236) if i in ((0, 3)) else (60, 34, 40))
                    p.px((ex + i, ey + 2), kit.skin_shade(SKIN, 0.75))
                cx_ = ex + (2 if side == 0 else 1)
                p.px((cx_, ey + 1), (16, 8, 12))
                p.px((cx_ - (1 if side == 0 else -1), ey), (255, 255, 255))
            for j in range(1, 5):                       # the cinnabar mark: a red flame on his brow
                w_ = 1 if j < 3 else 2
                for i in range(w_):
                    p.lit((fx + fw // 2 - 1 + i, fy + j + 1), (230, 30, 40), 0.35, blaze=0.8)
            for side in (0, 1):                         # rosy cheeks
                cx_ = fx + (3 if side == 0 else fw - 4)
                for i in range(-1, 2):
                    p.px((cx_ + i, fy + 10), mix(SKIN, (240, 120, 120), 0.45))
            for i in range(fw // 2 - 2, fw // 2 + 2):   # a small, set mouth
                p.px((fx + i, fy + 12), (180, 60, 60))
            p.px((fx + fw // 2 - 3, fy + 11), (180, 60, 60))
            p.px((fx + fw // 2 + 2, fy + 11), (180, 60, 60))
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, (x, y) in each(p, f[s]):
                    tb = (fw - 1 - i) if s == 'right' else i
                    if tb < fw * 0.4 or j < 2:
                        p.px((x, y), noisy(p, mix(HAIR, (60, 50, 70), (i + j) % 3 * 0.3)))
                ex = fx + (fw - 7 if s == 'right' else 4)          # the ear
                for j in range(5, 9):
                    for i in range(3):
                        p.px((ex + i, fy + j), kit.skin_shade(SKIN, 0.8 if i == 1 else 0.95))
        if style == 'cheek':                            # round young cheeks
            kit.flesh(p, f, SKIN, round_=0.9)
            fx, fy, fw, fh = f['front']
            for side in (0, 1):
                cx_ = fx + (2 if side == 0 else fw - 3)
                for i in range(-1, 2):
                    p.px((cx_ + i, fy + 1), mix(SKIN, (240, 120, 120), 0.35))
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, (x, y) in each(p, f[s]):
                    tb = (fw - 1 - i) if s == 'right' else i
                    if tb < fw * 0.3:
                        p.px((x, y), noisy(p, HAIR))
            p.face(f['bottom'], kit.skin_shade(SKIN, 0.6), 1.0, noise=2)
        if style == 'nose':
            kit.flesh(p, f, SKIN, round_=0.3)
        if style in ('hair_cap', 'hair_back', 'bangs'):
            for s, r in f.items():
                kit.wp.strands(p, r, (34, 26, 36), (12, 8, 14), tip=(70, 60, 80))
            if style == 'bangs':
                for s in ('front', 'back'):
                    kit.wp.tufts(p, f[s], depth=1, every=3)
        if style == 'brow':
            lock(p, f, along_x=True, tone=BLUE_BLACK)
        if style == 'bun':
            for s, r in f.items():
                kit.wp.strands(p, r, (40, 30, 42), (12, 8, 14), tip=(80, 70, 90))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):                     # rounded off at its top corners
                    if i < 1 or i > fw - 2:
                        p.clear((fx + i, fy))
        if style == 'bun_tie':
            for s, r in f.items():
                p.face(r, RED, LIGHT[s], noise=3)
            kit.gold_rim(p, f, top=False)
        if style == 'bun_ribbon':
            kit.ribbon(p, f, (220, 36, 36), (120, 12, 16))

        if style in ('upperarm', 'forearm', 'hand'):
            kit.flesh(p, f, SKIN, round_=0.6)
        if style in ('armlet', 'bracelet'):
            kit.gold(p, f, round_=0.8)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                p.lit((fx + fw // 2, fy + fh // 2), (230, 40, 40), 0.3)

        if style in ('petal', 'petal_low'):             # a lotus petal: pink, paling to its middle, its tip red, veined
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh       # v 0 at its root .. 1 at its tip
                    half = 0.5 * math.sin(clamp(v * 0.92 + 0.08) * math.pi) ** 0.7
                    if abs(u) > half:
                        p.clear(xy)
                        continue
                    c = mix(mix((255, 236, 236), PINK, clamp(v * 1.4)), PETAL_TIP, clamp((v - 0.55) * 2.2))
                    if v < 0.18:
                        c = mix(GREEN, c, v / 0.18)
                    if abs(u) < 0.05 or (abs(abs(u) - v * 0.25) < 0.04 and v > 0.2):
                        c = k(c, 0.86)
                    p.px(xy, noisy(p, k(c, 1.0 if s == 'front' else 0.82), 3))
            bare(p, f, ('top', 'bottom', 'right', 'left'))

        # ── the wheels ──
        if style == 'wh_rim':                           # the rim: gold, a red band of fire within it
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(p, r):
                    if s in ('right', 'left'):
                        c = GOLD_LIGHT if j == 0 else GOLD if j < fh - 1 else GOLD_DEEP
                        if 0 < j < fh - 1 and i % 3 == 1:
                            p.lit(xy, FIRE_HOT, 0.45, blaze=0.9)
                            continue
                    elif s == 'top':                    # its outer face, toward the flames
                        c = mix(GOLD_LIGHT, GOLD, abs((i + 0.5) / fw - 0.5) * 2)
                    else:
                        c = GOLD_DEEP
                    p.px(xy, noisy(p, c, 3))
        if style == 'wh_spoke':
            kit.gold(p, f, round_=0.6)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(0, fh, 4):
                    p.lit((fx + fw // 2, fy + j), FIRE_HOT, 0.3, blaze=0.8)
        if style == 'wh_hub':                           # the hub: a gold boss, a burning jewel
            kit.gold(p, f, round_=0.8)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    d = math.hypot((i + 0.5) / fw - 0.5, (j + 0.5) / fh - 0.5) * 2
                    if d < 0.55:
                        p.lit(xy, mix(FIRE_WHITE, FIRE, d * 1.6), 0.8, blaze=1.0)
                    elif d < 0.75:
                        p.px(xy, GOLD_LIGHT)
        if style in ('wh_flame', 'wh_flame_s'):
            for s in ('right', 'left'):
                flame(p, f[s])
            bare(p, f, ('front', 'back', 'top', 'bottom'))
        if style in ('wh_flamex', 'wh_flame_sx'):
            for s in ('front', 'back'):
                flame(p, f[s])
            bare(p, f, ('right', 'left', 'top', 'bottom'))

        # ── the ring ──
        if style == 'ring':                             # the Universe Ring: gold, faintly shining
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(p, r):
                    v = (j + 0.5) / fh
                    c = mix(GOLD_LIGHT, GOLD, abs(v - 0.3) * 1.6) if s != 'bottom' else GOLD_DEEP
                    p.lit(xy, noisy(p, c, 3), 0.25, blaze=0.6)

        # ── the spear ──
        if style == 'sp_shaft':                         # red lacquer, banded in gold
            p.shaded(f, (178, 30, 28))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(0, fh, 10):
                    for i in range(fw):
                        p.px((fx + i, fy + j), GOLD)
                        p.px((fx + i, fy + j + 1), GOLD_DEEP)
        if style in ('sp_collar', 'sp_mouth', 'sp_butt'):
            kit.gold(p, f, round_=0.7)
            if style == 'sp_mouth':
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    p.px((fx + 1, fy + 1), (230, 40, 30))
                    p.px((fx + fw - 2, fy + 1), (230, 40, 30))
            if style == 'sp_butt':
                for s in SIDES:
                    point(p, f[s], keep=0.3, down=True, sharp=0.5)
        if style == 'sp_blade':                         # the leaf blade, white-hot at its edges
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(p, r):
                    u = (i + 0.5) / fw
                    c = mix((250, 250, 255), (170, 176, 196), abs(u - 0.5) * 2)
                    if abs(u - 0.5) < 0.1:
                        c = (130, 136, 160)
                    p.px(xy, noisy(p, c, 2))
                    if s in ('front', 'back') and (i == 0 or i == fw - 1):
                        p.lit(xy, FIRE_HOT, 0.6, blaze=1.0)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh
                    half = 0.5 * math.sin(clamp(v) * math.pi * 0.95 + 0.1) ** 0.8
                    if abs(u) > half:
                        p.clear(xy)
        if style in ('sp_fire', 'sp_firex'):            # the fire wreathing the blade
            flat = ('front', 'back') if style == 'sp_fire' else ('right', 'left')
            for s in flat:
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh
                    t = 1 - v
                    half = 0.5 * (1 - t ** 1.5) * (0.75 + 0.25 * math.sin(v * 9 + u * 3))
                    if abs(u) > half or abs(u) < 0.12 and 0.2 < v < 0.95:
                        p.clear(xy)
                        continue
                    c = mix(FIRE_HOT, FIRE_DEEP, clamp(t + abs(u)))
                    p.lit(xy, c, 0.8, blaze=1.0)
            bare(p, f, [s for s in f if s not in flat])
        if style == 'sp_tassel':
            for s, r in f.items():
                kit.wp.strands(p, r, (220, 30, 30), (120, 10, 14))
            if dims[0] > 1.5:
                for s in SIDES:
                    kit.wp.tufts(p, f[s], depth=2)

        # ── the sash ──
        if style in ('sash_arc', 'sash_end'):           # the Huntian sash: red silk, shimmering, a gold thread
            kit.ribbon(p, f, SASH, SASH_DEEP)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    if (i + j * 2) % 7 == 0:
                        p.px(xy, mix(SASH, (255, 170, 150), 0.5))
            if style == 'sash_arc':
                for s in ('front', 'back'):
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        p.px((fx + i, fy), GOLD)
                        p.px((fx + i, fy + fh - 1), GOLD)

    return paint_box


def build():
    p = kit.GreatPainter(55)
    slots = paint(NEZHA, p, skin())
    p.save('heaven_nezha')
    emit(NEZHA, slots, kit.JAVA, 'nezha')
    report('Nezha', NEZHA, SIZE, slots)
