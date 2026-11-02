"""Li Jing, the Pagoda-Bearing Heavenly King (托塔李天王), commander of Heaven's armies (heaven_great.py
says how the great names are made). A commander in gold: a phoenix-winged gold helmet (凤翅盔) with a
red-tufted spike and a jewelled visor, its wings sweeping up past his crown; a stern, long face, narrowed
eyes under level brows, a long black beard in five locks down to his breast and a drooping moustache;
gold mountain-pattern armour (山文甲) over all of him, two round mirrors on his breast, gold pauldrons
bitten by lions, a red silk sash knotted over his belly, a jade belt, gold tassets over a red robe, black
boots; a great red cape from his shoulders to his shins, gold-clasped, worked in gold clouds. On his left
palm, held up beside his head, the golden pagoda (玲珑宝塔): seven tiers, its eaves upturned and hung
with bells, light in every window, a lotus under it and a pearl on its spire; in his right hand a sword,
its point down. Winding up he lifts the pagoda high over his head and it blazes, rays of light about it.

Run from the repository root: python assets_source/heaven_great.py li"""
import math

import beastgen as bg
import heaven_great_kit as kit
from heaven_great_kit import (part, mirrored, aim, paint, emit, report, k, mix, clamp, each, noisy, lock, point,
                              GOLD, GOLD_DEEP, GOLD_LIGHT, GOLD_SHADOW, IVORY, HAIR, RED, RED_DEEP, JADE,
                              FIRE_HOT, FIRE_WHITE, SIDES, LIGHT, BLUE_BLACK)
from heaven_great import hang, bare

SIZE = 0.84       # HeavenGreatRenderers.LI_SIZE

ARM, FOREARM = (-0.08, 0, 0.12), (-0.55, -0.25, 0)
LARM, LFOREARM = (-0.2, 0, -0.45), (-2.6, -0.3, 0.3)       # his left arm out, its forearm raised, the pagoda on his palm

FACE = (226, 182, 146)
CAPE = (176, 26, 24)
CAPE_DEEP = (92, 10, 14)
ROBE = (150, 24, 26)


def pagoda():
    """The golden pagoda on his left palm: a lotus, a plinth, seven tiers narrowing, eaves upturned at their
    corners, a spire of rings, a pearl."""
    boxes = [(-3.4, -1.4, -3.4, 6.8, 1.4, 6.8, 'pg_lotus'), (-2.8, -2.8, -2.8, 5.6, 1.4, 5.6, 'pg_plinth')]
    y = -2.8
    for i in range(7):
        b = 4.6 - 0.42 * i
        h = 1.75 - 0.06 * i
        e = b + 2.0
        boxes.append((-b / 2, y - h, -b / 2, b, h, b, 'pg_tier_%d' % i))
        y -= h
        boxes.append((-e / 2, y - 0.6, -e / 2, e, 0.6, e, 'pg_eave_%d' % i))
        for sx in (-1, 1):
            for sz in (-1, 1):
                boxes.append((sx * e / 2 - 0.35 - sx * 0.1, y - 1.1, sz * e / 2 - 0.35 - sz * 0.1, 0.7, 0.7, 0.7, 'pg_corner'))
        y -= 0.6
    boxes += [(-0.5, y - 4.4, -0.5, 1.0, 4.4, 1.0, 'pg_spire'),
              (-1.2, y - 1.4, -1.2, 2.4, 0.6, 2.4, 'pg_ring'), (-1.0, y - 2.6, -1.0, 2.0, 0.6, 2.0, 'pg_ring_s'),
              (-0.8, y - 3.7, -0.8, 1.6, 0.5, 1.6, 'pg_ring_t'), (-1.1, y - 6.6, -1.1, 2.2, 2.2, 2.2, 'pg_pearl')]
    return [part('pagoda', 'left_hand', (0, 3.4, 0), aim([LARM, LFOREARM], (0, 0, 0)), boxes),
            part('pagoda_rays', 'pagoda', (0, -12, 0), (0, 0.4, 0), [(-14, -14, 0, 28, 28, 0.01, 'pg_rays'),
                                                                     (0, -14, -14, 0.01, 28, 28, 'pg_rays_x')])]


LI = mirrored([
    part('waist', 'root', (0, 5, 0), (0, 0, 0), [
        (-6, -2, -4, 12, 4, 8, 'hips'),
        (-7, -4.2, -5, 14, 3.4, 10, 'belt'),
        (-2.6, -5.2, -5.8, 5.2, 4.4, 1.2, 'buckle'),
    ]),
    part('apron', 'waist', (0, 0.4, -5.2), (-0.05, 0, 0), [(-3, 0, -0.5, 6, 15.5, 0.8, 'apron')]),
    part('right_tasset', 'waist', (-3.8, 0.0, -4.4), (-0.12, 0.15, 0.1), [(-3.4, 0, -0.6, 6.8, 10.5, 1.2, 'tasset')]),
    part('right_side_tasset', 'waist', (-6.6, 0.0, 0), (0, 0, 0.2), [(-1.1, 0, -4.2, 1.1, 10, 8.4, 'tasset_side')]),
    part('back_tasset', 'waist', (0, 0.0, 4.6), (0.12, 0, 0), [(-6.2, 0, -0.6, 12.4, 10.5, 1.2, 'tasset_back')]),
    part('right_leg', 'root', (-3.3, 7, 0.3), (0, 0, 0), [(-2.8, 0, -2.8, 5.6, 8.4, 5.6, 'thigh')]),
    part('right_robe', 'right_leg', (0, -0.6, -3.0), (-0.05, 0, 0.04), [(-3.1, 0, -0.5, 6.2, 12, 0.6, 'robe')]),
    part('right_shin', 'right_leg', (0, 8.4, 0), (0, 0, 0), [
        (-2.4, 0, -2.4, 4.8, 5.6, 4.8, 'shin'),
        (-2.8, 0.4, -3.0, 5.6, 5.0, 3.0, 'greave'),
        (-2.8, 5.2, -3.2, 5.6, 3.4, 6.2, 'boot'),
        (-2.1, 6.6, -4.9, 4.2, 2.0, 1.8, 'boot_toe'),
        (-1.2, 5.6, -5.4, 2.4, 1.8, 0.8, 'boot_tip'),
    ]),
    part('body', 'waist', (0, -3, 0), (0, 0, 0), [
        (-6.5, -14, -4.4, 13, 14, 8.8, 'torso'),
        (-7.1, -14.4, -5.0, 14.2, 7.4, 10, 'breastplate'),
        (-5.6, -13.4, -5.6, 4.4, 4.4, 0.7, 'mirror'),
        (1.2, -13.4, -5.6, 4.4, 4.4, 0.7, 'mirror'),
        (-6.8, -4.6, -4.9, 13.6, 2.4, 9.8, 'waistsash'),
        (-1.6, -5.4, -5.9, 3.2, 3.0, 1.2, 'sashknot'),
        (-4.2, -15.8, -3.6, 8.4, 2.4, 7.2, 'gorget'),
        (-7.8, -14.8, -5.4, 15.6, 1.2, 10.8, 'mantle'),
        (-7.4, -14.6, -5.6, 2.6, 2.6, 0.8, 'clasp'),
        (4.8, -14.6, -5.6, 2.6, 2.6, 0.8, 'clasp'),
    ]),
] + hang('right_sashend', 'body', (-0.8, -3.0, -5.6), (-0.05, 0, 0.12), [(1.6, 5, 0.5, 0), (1.8, 5, 0.5, 0.05)], 'sashend') + [
    part('cape', 'body', (0, -14.6, 4.9), (0.1, 0, 0), [(-8.2, 0, -0.4, 16.4, 14, 0.8, 'cape')]),
    part('cape_lower', 'cape', (0, 13.8, 0), (0.06, 0, 0), [(-8.8, 0, -0.4, 17.6, 10, 0.8, 'cape_lower')]),
    part('cape_hem', 'cape_lower', (0, 9.8, 0), (0.05, 0, 0), [(-9.4, 0, -0.4, 18.8, 6, 0.8, 'cape_hem')]),
    part('right_cape_wrap', 'cape', (-8.0, 0.4, -0.2), (0, 0.65, 0.12), [(-0.4, 0, -6.5, 0.8, 12, 6.5, 'cape_wrap')]),
    # the head: a stern long face, the beard
    part('head', 'body', (0, -15.6, -0.3), (0, 0, 0), [
        (-4, -9.4, -3.9, 8, 8.2, 7.8, 'face'),
        (-3.6, -1.6, -3.6, 7.2, 2.2, 6.8, 'jaw'),
        (-1, -6.4, -4.7, 2, 2.9, 1, 'nose'),
        (-4.3, -9.6, -0.6, 8.6, 8.6, 5.2, 'hair_back'),
    ]),
    part('right_brow', 'head', (-0.5, -6.9, -4.1), (0, 0.1, 0.16), [(-3.4, -0.6, -0.4, 3.4, 0.9, 0.8, 'brow')]),
    part('right_ear', 'head', (-4.1, -5.8, -0.2), (0, 0, 0.06), [(-0.8, -1.6, -1, 0.8, 4.2, 2, 'ear')]),
    part('right_moustache', 'head', (-0.4, -3.1, -4.6), (0, 0.25, -0.62), [(-3.8, -0.5, -0.5, 3.8, 1, 1, 'moustache')]),
    part('beard', 'head', (0, -0.8, -3.6), (0.1, 0, 0), [(-1.5, 0, -1, 3, 10, 1.8, 'beard_c')]),
    part('beard_tip', 'beard', (0, 9.6, 0), (-0.1, 0, 0), [(-1.1, 0, -0.8, 2.2, 5, 1.6, 'beard_tip')]),
    part('right_beard', 'head', (-2.3, -1.2, -3.2), (0.12, 0.12, 0.1), [(-1.1, 0, -0.8, 2.2, 9.5, 1.6, 'beard_side')]),
    part('right_beard_outer', 'head', (-3.5, -2.6, -2.2), (0.05, 0.25, 0.14), [(-0.9, 0, -0.7, 1.8, 7.5, 1.4, 'beard_outer')]),
    # the phoenix-winged helmet
    part('helm', 'head', (0, -9.2, 0), (0, 0, 0), [
        (-4.6, -1.6, -4.4, 9.2, 2, 8.8, 'helm_band'),
        (-4.2, -5.6, -4.2, 8.4, 4.2, 8.4, 'helm_dome'),
        (-3.0, -7.2, -3.0, 6, 1.8, 6, 'helm_top'),
        (-1.9, -9.2, -1.9, 3.8, 2.2, 3.8, 'helm_tuft'),
        (-0.7, -12.6, -0.7, 1.4, 4, 1.4, 'helm_spike'),
        (-4.5, -0.6, -5.4, 9, 1.2, 1.2, 'helm_visor'),
        (-1.4, -4.6, -4.9, 2.8, 3.2, 0.8, 'helm_jewel'),
    ]),
    part('right_helm_wing', 'helm', (-4.3, -2.8, 0.6), (0.12, 0.18, -0.42), [(-0.4, -9.6, -0.8, 0.8, 9.6, 6.4, 'helm_wing')]),
    part('right_helm_flap', 'helm', (-4.5, -0.4, -0.4), (0, 0, 0.14), [(-0.6, 0, -2.2, 0.6, 4.2, 5.6, 'helm_flap')]),
    # the arms
    part('right_arm', 'body', (-8.6, -12.8, 0.2), ARM, [
        (-2.4, -1, -2.4, 4.8, 7.6, 4.8, 'upperarm'),
        (-2.8, 4.0, -2.8, 5.6, 1.2, 5.6, 'armband'),
    ]),
    part('right_pauldron', 'right_arm', (-0.3, -0.8, 0), (0, 0, 0.4), [
        (-4.2, -1.4, -3.8, 6.4, 1.6, 7.6, 'pauldron'),
        (-4.9, 0.0, -4.2, 7.0, 1.8, 8.4, 'pauldron2'),
        (-5.6, 1.6, -4.6, 7.6, 2.2, 9.2, 'pauldron3'),
        (-6.5, -1.6, -3.8, 0.9, 7.6, 7.6, 'pauldron_mane'),
        (-7.3, -0.4, -2.7, 1.5, 5.4, 5.4, 'pauldron_lion'),
    ]),
    part('right_forearm', 'right_arm', (0, 6.8, 0), FOREARM, [
        (-2.1, 0, -2.1, 4.2, 6.2, 4.2, 'forearm'),
        (-2.5, 0.8, -2.5, 5.0, 4.6, 5.0, 'bracer'),
        (-3.2, -1.2, -3.2, 6.4, 2.0, 6.4, 'frill'),
    ]),
    part('right_hand', 'right_forearm', (0, 6.2, 0), (0, 0, 0), [(-1.8, 0, -1.8, 3.6, 3.6, 3.6, 'fist')]),
    # his sword, its point down before him
    part('sword', 'right_hand', (0, 1.8, 0), aim([ARM, FOREARM], (math.pi - 0.62, 0, 0)), [
        (-0.6, -2.4, -0.6, 1.2, 5.4, 1.2, 'sword_grip'),
        (-1.0, 2.8, -1.0, 2.0, 1.4, 2.0, 'sword_pommel'),
        (-2.6, -3.6, -1.1, 5.2, 1.3, 2.2, 'sword_guard'),
        (-1.1, -23.6, -0.3, 2.2, 20.2, 0.6, 'sword_blade'),
    ]),
])


def raise_left(parts):
    out = []
    for name, parent, pivot, rot, boxes, mirror in parts:
        if name == 'left_arm':
            rot = LARM
        elif name == 'left_pauldron':
            rot = (rot[0], rot[1], rot[2] - (LARM[2] + ARM[2]) * 0.7)
        elif name == 'left_forearm':
            rot = LFOREARM
        elif name == 'left_hand':
            boxes = [(-1.9, 0, -1.9, 3.8, 3.4, 3.8, 'palm')]
        elif name == 'left_sword' or name.startswith('left_sword'):
            continue
        out.append((name, parent, pivot, rot, boxes, mirror))
    return out


LI = raise_left(LI) + pagoda()


def skin():
    face_dark = kit.skin_shade(FACE, 0.35)

    def mountain_box(p, f, light=1.0):
        for s, r in f.items():
            if s in ('top', 'bottom'):
                p.face(r, GOLD_DEEP, LIGHT[s], noise=3)
            else:
                kit.mountain(p, r, GOLD, GOLD_SHADOW, GOLD_LIGHT, light=LIGHT[s] * light)

    def paint_box(p, style, f, dims):
        if style == 'hips':
            kit.silk(p, f, ROBE)
        if style == 'belt':                                    # a jade belt: jade plaques in gold on red leather
            p.shaded(f, (110, 20, 20))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(1, fw - 3, 5):
                    for j in range(1, fh - 1):
                        for w in range(3):
                            p.px((fx + i + w, fy + j), JADE if w < 2 else kit.JADE_DEEP)
                    p.px((fx + i, fy + 1), (180, 236, 206))
            kit.gold_rim(p, f)
        if style == 'buckle':
            kit.gold(p, f)
            kit.lion(p, f['front'], open_=0.3, mane=False)
        if style == 'apron':                                   # red, gold-bordered, a gold cloud and a tiger's head, a fringe
            for s, r in f.items():
                p.face(r, ROBE, LIGHT[s], noise=3, fall=0.2)
            fx, fy, fw, fh = f['front']
            kit.frame(p, (fx, fy, fw, fh - 4), GOLD, GOLD_DEEP)
            for j in range(8, fh - 8, 8):
                kit.cloud(p, fx + fw // 2 - 1, fy + j, GOLD, r=1)
            for i in range(fw):
                for j in range(fh - 4, fh):
                    if i % 2 == 0 or j < fh - 2:
                        p.px((fx + i, fy + j), GOLD if (i // 2) % 2 else (230, 60, 50))
                    else:
                        p.clear((fx + i, fy + j))
        if style in ('tasset', 'tasset_side', 'tasset_back'):   # gold mountain-pattern, gold-rimmed, scalloped
            mountain_box(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), GOLD_LIGHT)
                    p.px((fx + i, fy + 1), RED)
                if s in ('front', 'back') and fw > 6:
                    for i in range(fw):
                        t = (i % 6) / 5
                        dip = int(2.5 * (1 - abs(t - 0.5) * 2))
                        for j in range(fh - 3 + dip, fh):
                            p.clear((fx + i, fy + j))
                        p.px((fx + i, fy + fh - 4 + dip), GOLD_LIGHT)
                        p.px((fx + i, fy + fh - 5 + dip), RED)
        if style in ('thigh', 'shin'):
            kit.silk(p, f, ROBE, fold=5)
        if style == 'robe':
            kit.silk(p, f, ROBE, fold=6)
            for s in ('front', 'back'):
                kit.hem(p, f[s], 5, (40, 60, 120))
                kit.scroll_band(p, f[s], f[s][3] - 4, GOLD, GOLD_DEEP, period=7)
        if style == 'greave':
            mountain_box(p, f)
            kit.gold_rim(p, f)
        if style == 'boot':
            p.shaded(f, (30, 24, 28))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), GOLD)
                    p.px((fx + i, fy + fh - 1), IVORY)
                kit.scroll_band(p, f[s], 2, GOLD_DEEP, (20, 16, 20), period=6)
        if style == 'boot_toe':
            p.shaded(f, (30, 24, 28))
            kit.gold_rim(p, f, bottom=False)
        if style == 'boot_tip':
            kit.gold(p, f)

        if style == 'torso':
            mountain_box(p, f)
            kit.gold_rim(p, f)
        if style == 'breastplate':                             # gold, moulded, the pectorals outlined in red-gold
            kit.gold(p, f, round_=0.7)
            for s in SIDES:
                kit.mountain(p, f[s], GOLD, GOLD_SHADOW, GOLD_LIGHT, light=LIGHT[s]) if s != 'front' else None
            fx, fy, fw, fh = f['front']
            for side in (0, 1):
                x0 = fx + (2 if side == 0 else fw // 2 + 1)
                w_ = fw // 2 - 3
                for i in range(w_):
                    t = (i + 0.5) / w_
                    y = fy + fh - 3 - int(3 * math.sin(t * math.pi))
                    p.px((x0 + i, y), GOLD_SHADOW)
                    p.px((x0 + i, y + 1), GOLD_LIGHT)
            kit.gold_rim(p, f, width=2, colour=None)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 2), RED)
        if style == 'mirror':
            kit.gold(p, f)
            kit.mirror_plate(p, f['front'], glass=(200, 210, 236))
            bare(p, f, ('top', 'bottom', 'right', 'left'))
        if style == 'waistsash':                               # red silk wound round and knotted
            kit.silk(p, f, (200, 34, 30), (110, 14, 16), fold=3)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    if (i + 1) % 4 == 0:
                        for j in range(fh):
                            p.px((fx + i, fy + j), (120, 16, 18))
        if style == 'sashknot':
            kit.silk(p, f, (210, 40, 34), (120, 16, 18), fold=2)
        if style == 'sashend':
            kit.ribbon(p, f, (200, 34, 30), (110, 14, 16))
        if style == 'gorget':
            for s, r in f.items():
                if s in SIDES:
                    kit.fret(p, r, GOLD, GOLD_DEEP)
                else:
                    p.face(r, GOLD_DEEP, LIGHT[s], noise=3)
        if style == 'mantle':
            kit.gold(p, f)
            fx, fy, fw, fh = f['top']
            cx, cy = fx + fw / 2, fy + fh / 2
            for i, j, (x, y) in each(p, f['top']):
                a = math.atan2(y - cy, (x - cx) * fh / fw)
                if abs(math.sin(a * 4)) < 0.15:
                    p.px((x, y), GOLD_SHADOW)
            kit.gold_rim(p, f, colour=RED)
        if style == 'clasp':                                   # the cape's clasps: gold discs, a red jewel
            kit.gold(p, f)
            fx, fy, fw, fh = f['front']
            kit.wp.disc(p, fx + fw / 2, fy + fh / 2, fw / 2 - 0.2, GOLD, rim=GOLD_DEEP)
            kit.wp.disc(p, fx + fw / 2, fy + fh / 2, 1.2, (220, 30, 30))
        if style in ('cape', 'cape_lower', 'cape_hem', 'cape_wrap'):   # the red cape: gold clouds, a gold hem, lined in gold-yellow
            kit.silk(p, f, CAPE, CAPE_DEEP, fold=9)
            fx, fy, fw, fh = f['front']                        # its lining toward him
            for i, j, xy in each(p, f['front']):
                p.px(xy, noisy(p, k((214, 160, 60), 0.85 - 0.2 * j / max(1, fh))))
            outer = 'back' if style != 'cape_wrap' else 'right'
            kit.clouds(p, f[outer], GOLD_DEEP, every=10, r=2, share=0.5)
            if style == 'cape_hem':
                kit.hem(p, f['back'], 5, (40, 60, 120))
                kit.scroll_band(p, f['back'], f['back'][3] - 4, GOLD, GOLD_DEEP)
                for s in ('front', 'back'):
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):                        # its foot fluttering
                        cutd = int(1.5 + 1.5 * math.sin(i * 0.7))
                        for j in range(cutd):
                            p.clear((fx + i, fy + fh - 1 - j))
            if style == 'cape':
                kit.hem(p, f['back'], 3, GOLD, at_bottom=False)
            if style == 'cape_wrap':
                for s in ('right', 'left'):
                    fx, fy, fw, fh = f[s]
                    for i, j, xy in each(p, f[s]):
                        u, v = (i + 0.5) / fw, (j + 0.5) / fh
                        uu = u if s == 'right' else 1 - u     # 1 toward his front
                        if uu > 0.9 - 0.6 * v:
                            p.clear(xy)

        if style == 'face':                                    # a stern long face; narrowed eyes; the hair drawn up under the helm
            kit.flesh(p, f, FACE)
            fx, fy, fw, fh = f['front']                        # 16x16
            for i in range(fw):
                p.px((fx + i, fy), HAIR)
            for j in range(3, 6):                              # a furrow between the brows
                p.px((fx + fw // 2, fy + j), face_dark)
            stern_eyes(p, f['front'], 7, (60, 36, 24), FACE)
            for j in range(11, fh):                            # lines from the nose
                p.px((fx + 3 - (j - 11) // 3, fy + j), face_dark)
                p.px((fx + fw - 4 + (j - 11) // 3, fy + j), face_dark)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, (x, y) in each(p, f[s]):
                    tb = (fw - 1 - i) if s == 'right' else i
                    if tb < fw * 0.5 or j < 2:
                        p.px((x, y), noisy(p, mix(HAIR, (60, 50, 60), (i + j) % 3 * 0.3)))
            for s in ('back', 'top'):
                for i, j, xy in each(p, f[s]):
                    p.px(xy, noisy(p, mix(HAIR, (60, 50, 60), (i * 2 + j) % 3 * 0.3)))
        if style == 'jaw':
            kit.flesh(p, f, FACE, round_=0.7)
            fx, fy, fw, fh = f['front']
            for i in range(4, fw - 4):
                p.px((fx + i, fy), (110, 40, 40))
            p.face(f['bottom'], kit.skin_shade(FACE, 0.45), 1.0, noise=2)
        if style == 'nose':
            kit.flesh(p, f, FACE, round_=0.4)
            fx, fy, fw, fh = f['front']
            for j in range(fh - 1):
                p.px((fx + 1, fy + j), kit.skin_shade(FACE, 1.4))
            p.px((fx, fy + fh - 1), face_dark)
            p.px((fx + fw - 1, fy + fh - 1), face_dark)
        if style == 'ear':
            kit.flesh(p, f, FACE, round_=0.5)
        if style in ('brow', 'moustache'):
            lock(p, f, along_x=True, tone=BLUE_BLACK)
        if style in ('beard_c', 'beard_side', 'beard_tip', 'beard_outer'):
            lock(p, f, keep=0.6 if style != 'beard_tip' else 0.35, taper=0.25, tone=BLUE_BLACK, grey=0.06)
        if style == 'hair_back':
            for s, r in f.items():
                kit.wp.strands(p, r, (40, 32, 40), HAIR)

        if style in ('helm_band', 'helm_dome', 'helm_top', 'helm_visor'):
            kit.gold(p, f, round_=0.8)
            if style == 'helm_dome':                           # a gold cloud scroll round it, red-lacquered ribs
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(1, fw, 4):
                        for j in range(fh):
                            p.px((fx + i, fy + j), GOLD_DEEP)
                    kit.scroll_band(p, f[s], fh - 3, GOLD_LIGHT, GOLD_SHADOW, period=6)
            if style == 'helm_band':
                for s in SIDES:
                    kit.studs(p, f[s], 1, every=4)
        if style == 'helm_tuft':                               # the red tuft about the spike
            for s, r in f.items():
                kit.wp.strands(p, r, (220, 30, 30), (120, 10, 14), down=True)
            for s in SIDES:
                kit.wp.tufts(p, f[s], depth=2)
        if style == 'helm_spike':
            kit.gold(p, f)
            for s in SIDES:
                point(p, f[s], keep=0.3, down=False, sharp=0.35)
        if style == 'helm_jewel':                              # a flame-shaped plaque, a red jewel
            kit.gold(p, f)
            fx, fy, fw, fh = f['front']
            kit.wp.disc(p, fx + fw / 2, fy + fh * 0.6, 1.3, (230, 40, 40), rim=GOLD_LIGHT)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u, v = (i + 0.5) / fw, (j + 0.5) / fh
                    from heaven_great import arch
                    if not arch(u, v, waist=0.5):
                        p.clear(xy)
        if style == 'helm_wing':                               # a phoenix wing: gold feathers, red-tipped
            for s, r in f.items():
                p.face(r, GOLD_DEEP, 1.0, noise=3)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u = (i + 0.5) / fw
                    if s == 'right':
                        u = 1 - u
                    v = (j + 0.5) / fh
                    reach = 0.22 + 0.78 * (1 - v) ** 0.6
                    feather = 0.08 * math.sin(v * 26)
                    if u > reach + feather:
                        p.clear(xy)
                        continue
                    stripe = int(v * 9 + u * 3) % 2
                    c = mix(GOLD_LIGHT, GOLD, u) if stripe else mix(GOLD, GOLD_DEEP, u)
                    if u > reach + feather - 0.14:
                        c = (210, 40, 34)
                    p.px(xy, noisy(p, c, 3))
        if style == 'helm_flap':                               # the neck guard: gold lamellae
            for s, r in f.items():
                kit.lamellae(p, r, GOLD, RED, pw=3, ph=4, light=LIGHT[s], edge=GOLD_SHADOW, gleam=GOLD_LIGHT)

        if style in ('upperarm', 'forearm'):
            kit.silk(p, f, CAPE, CAPE_DEEP, fold=5)
        if style == 'armband':
            kit.gold(p, f)
        if style in ('pauldron', 'pauldron2', 'pauldron3'):
            for s, r in f.items():
                if s in SIDES:
                    kit.mountain(p, r, GOLD, GOLD_SHADOW, GOLD_LIGHT, light=LIGHT[s])
                else:
                    kit.gold(p, f, faces=(s,))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), RED)
                    p.px((fx + i, fy + fh - 2), GOLD_LIGHT)
        if style == 'pauldron_mane':
            kit.gold(p, f)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh - 0.45
                    r = math.hypot(u, v) * 2
                    if r > 1.0:
                        p.clear(xy)
                    else:
                        curl = 0.5 + 0.5 * math.sin(math.atan2(v, u) * 10 + r * 11)
                        p.px(xy, noisy(p, mix(GOLD_SHADOW, GOLD_LIGHT, curl * 0.9)))
        if style == 'pauldron_lion':
            kit.gold(p, f)
            kit.lion(p, f['right'], open_=0.3, mane=False)
            kit.lion(p, f['left'], open_=0.3, mane=False)
        if style == 'bracer':
            kit.gold(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                kit.cloud(p, fx + fw // 2 - 2, fy + fh // 2, GOLD_SHADOW, r=1)
        if style == 'frill':
            kit.silk(p, f, CAPE, CAPE_DEEP, fold=3)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), GOLD)
                    if i % 3 == 0:
                        p.clear((fx + i, fy + fh - 1))
        if style in ('fist', 'palm'):
            kit.flesh(p, f, FACE)
            if style == 'fist':
                for s in ('front', 'right', 'left', 'top'):
                    fx, fy, fw, fh = f[s]
                    for i in range(1, fw, 2):
                        p.px((fx + i, fy + fh - 2), kit.skin_shade(FACE, 0.6))

        if style == 'sword_blade':
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(p, r):
                    u = (i + 0.5) / fw
                    c = mix((250, 252, 255), (150, 160, 178), abs(u - 0.5) * 2) if s in ('front', 'back') else (230, 236, 246)
                    if s in ('front', 'back') and abs(u - 0.5) < 0.12:
                        c = (120, 130, 150)
                    p.px(xy, noisy(p, c, 2))
            for s in ('front', 'back'):
                point(p, f[s], keep=0.0, down=False, sharp=0.4)
        if style in ('sword_guard', 'sword_pommel'):
            kit.gold(p, f)
        if style == 'sword_grip':
            p.shaded(f, RED_DEEP)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(0, fh, 2):
                    for i in range(fw):
                        p.px((fx + i, fy + j), (60, 10, 12))

        # ── the pagoda ──
        if style in ('pg_lotus',):                             # a gold lotus under it, its petals
            kit.gold(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    t = (i % 4) / 3
                    if j < int(fh * (1 - abs(t - 0.5) * 2) * 0.8):
                        continue
                    p.px(xy, noisy(p, mix(GOLD_LIGHT, GOLD, j / fh)))
                for i in range(0, fw, 4):
                    for j in range(fh):
                        p.px((fx + i, fy + j), GOLD_DEEP)
        if style == 'pg_plinth':
            kit.gold(p, f)
            for s in SIDES:
                kit.studs(p, f[s], 1, every=3)
        if style.startswith('pg_tier_'):                       # each storey: gold walls, a window glowing in each, pillars
            kit.gold(p, f, round_=0.3)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    p.px((fx, fy + j), GOLD_SHADOW)
                    p.px((fx + fw - 1, fy + j), GOLD_SHADOW)
                ww = max(2, fw // 3)
                x0 = fx + (fw - ww) // 2
                for i in range(ww):
                    for j in range(1, fh - 0):
                        top = j == 1 and (i == 0 or i == ww - 1)
                        if top:
                            continue
                        c = mix(FIRE_WHITE, (255, 190, 80), j / fh)
                        p.lit((x0 + i, fy + j), c, 0.7, blaze=1.0, hot=FIRE_WHITE)
        if style.startswith('pg_eave_'):                       # the eaves: gold tiles above, red-lacquered beneath
            for s, r in f.items():
                fx, fy, fw, fh = r
                if s == 'top':
                    for i, j, xy in each(p, r):
                        ring = min(i, j, fw - 1 - i, fh - 1 - j)
                        c = GOLD_LIGHT if ring % 2 == 0 else GOLD
                        p.px(xy, noisy(p, c, 3))
                elif s == 'bottom':
                    p.face(r, (150, 30, 24), 1.0, noise=3)
                else:
                    for i, j, xy in each(p, r):
                        p.px(xy, noisy(p, GOLD_LIGHT if j == 0 else GOLD_DEEP, 3))
                        if i % 3 == 0:
                            p.lit((fx + i, fy + fh - 1), GOLD_LIGHT, 0.4, blaze=0.9)
        if style == 'pg_corner':                               # an upturned eave's corner, a bell under it
            kit.gold(p, f)
            p.lit((f['bottom'][0], f['bottom'][1]), GOLD_LIGHT, 0.5, blaze=1.0)
        if style in ('pg_spire', 'pg_ring', 'pg_ring_s', 'pg_ring_t'):
            kit.gold(p, f, round_=0.8)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.lit((fx + i, fy), GOLD_LIGHT, 0.4, blaze=0.9)
        if style == 'pg_pearl':                                # the pearl on its spire, shining
            for s, r in f.items():
                for i, j, xy in each(p, r):
                    u, v = (i + 0.5) / r[2] - 0.5, (j + 0.5) / r[3] - 0.5
                    d = math.hypot(u, v) * 2
                    p.lit(xy, mix((255, 255, 240), (255, 200, 100), clamp(d)), 0.9, blaze=1.0)
        if style in ('pg_rays', 'pg_rays_x'):                  # rays of light about it, in the wind-up alone
            flat = ('front', 'back') if style == 'pg_rays' else ('right', 'left')
            for s in f:
                for i, j, xy in each(p, f[s]):
                    p.clear(xy)
            for s in flat:
                fx, fy, fw, fh = f[s]
                for i, j, (x, y) in each(p, f[s]):
                    u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh - 0.5
                    d = math.hypot(u, v) * 2
                    a = math.atan2(v, u)
                    ray = 0.5 + 0.5 * math.cos(a * 12)
                    ray2 = 0.5 + 0.5 * math.cos(a * 12 + math.pi)
                    strength = (ray ** 6 * (1 - d) + 0.6 * ray2 ** 10 * (1 - d) * 0.6 + max(0, 0.45 - d) * 1.4)
                    if d >= 1 or strength < 0.06:
                        continue
                    c = mix((255, 196, 80), FIRE_WHITE, clamp(strength))
                    p.img.putpixel((x, y), (0, 0, 0, 0))
                    p.flare((x, y), c, clamp(strength * 1.3))
                    # it must be drawn to be lit: a faint pixel on the skin, so the cutout keeps it
                    p.img.putpixel((x, y), (255, 220, 140, 255)) if False else None

    return paint_box


def stern_eyes(p, rect, y, iris, face, w=5, gap=None, glow=0.0):
    """Two narrowed, stern eyes across a face's front at row y: a heavy dark upper lid, the white, a dark
    iris against the inner corner, the lower lid shaded."""
    fx, fy, fw, fh = rect
    gap = gap if gap is not None else fw - 2 * w - 4
    for side in (0, 1):
        ex = fx + 2 if side == 0 else fx + fw - 2 - w
        inner = (w - 1) if side == 0 else 0
        for i in range(w):
            slope = 1 if (side == 0 and i < 1) or (side == 1 and i > w - 2) else 0
            p.px((ex + i, fy + y - 1 + slope), (34, 20, 22))
            c = (240, 232, 220)
            if abs(i - (inner - (1 if side == 0 else -1) * 1.2)) < 1.1:
                c = iris
            if glow:
                p.lit((ex + i, fy + y + slope), c, glow)
            else:
                p.px((ex + i, fy + y + slope), c)
            p.px((ex + i, fy + y + 1), kit.skin_shade(face, 0.7))
        p.px((ex + (inner - 1 if side == 0 else inner + 1), fy + y), (10, 6, 8))


def build():
    p = kit.GreatPainter(77)
    slots = paint(LI, p, skin())
    p.save('heaven_li_jing')
    emit(LI, slots, kit.JAVA, 'li_jing')
    report('Li Jing', LI, SIZE, slots)
