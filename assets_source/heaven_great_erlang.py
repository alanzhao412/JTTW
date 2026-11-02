"""Erlang Shen (二郎神), the Illustrious Sage, True Lord of Guankou (heaven_great.py says how the great
names are made). A handsome, stern young god in silver: a fair long face, fine brows, narrowed dark eyes,
no beard; in his brow the third eye, upright, gold and glowing; the three-mountain crown (三山冠), a gold
band under three flame-pointed peaks of silver edged in gold, little phoenix wings at its sides and white
ties hanging behind; silver fish-scale armour, a round mirror on his breast, silver pauldrons bitten by
lions, silver lamellar tassets laced in blue over a pale-gold (鹅黄) robe worked with clouds, a white jade
belt, silver greaves and black boots; a great white cape lined in sky blue, worked in silver clouds,
flowing from his shoulders. In his right hand the three-pointed, two-edged blade (三尖两刃刀): a long
shaft banded in silver, a gold dragon's mouth, a broad two-edged blade between two side points, a red
tassel. His hound Xiaotian (哮天犬) trots at his heel (its own model, drawn beside him by his renderer).
Winding up, he lifts two fingers to his brow and the third eye opens and blazes.

Run from the repository root: python assets_source/heaven_great.py erlang"""
import math

import beastgen as bg
import heaven_great_kit as kit
from heaven_great_kit import (part, mirrored, aim, paint, emit, report, k, mix, clamp, each, noisy, lock, point,
                              GOLD, GOLD_DEEP, GOLD_LIGHT, GOLD_SHADOW, SILVER, SILVER_DEEP, SILVER_LIGHT, IVORY, HAIR,
                              RED, RED_DEEP, FIRE_HOT, FIRE_WHITE, SIDES, LIGHT, BLUE_BLACK)
from heaven_great import hang, bare, arch
from heaven_great_li import stern_eyes

SIZE = 0.72       # HeavenGreatRenderers.ERLANG_SIZE

ARM, FOREARM = (-0.08, 0, 0.12), (-0.8, -0.25, 0)
LARM, LFOREARM = (0.05, 0, -0.1), (-0.35, 0.2, 0)

FACE = (240, 214, 190)
ROBE = (236, 206, 120)              # pale goose-yellow (鹅黄)
ROBE_DEEP = (170, 128, 54)
CAPE = (240, 242, 248)
CAPE_DEEP = (150, 160, 186)
LINING = (110, 170, 226)
LACE = (60, 110, 200)
EYE3 = (255, 196, 60)


def blade():
    """The three-pointed, two-edged blade, upright in his right hand."""
    return [part('blade', 'right_hand', (0, 1.6, 0), aim([ARM, FOREARM], (-0.05, 0, 0.06)), [
        (-0.5, -24, -0.5, 1.0, 39, 1.0, 'bl_shaft'),
        (-0.8, -25.2, -0.8, 1.6, 1.4, 1.6, 'bl_collar'),
        (-1.6, -27.4, -1.2, 3.2, 2.4, 2.4, 'bl_mouth'),
        (-1.4, -37.4, -0.3, 2.8, 10.4, 0.6, 'bl_blade'),
        (-0.7, 14.6, -0.7, 1.4, 1.6, 1.4, 'bl_butt'),
    ]),
        part('right_bl_prong', 'blade', (-1.3, -26.6, 0), (0, 0, 0.38), [(-0.5, -6.4, -0.3, 1.0, 6.6, 0.6, 'bl_prong')]),
    ] + hang('blade_tassel', 'blade', (0, -24.6, 0), (0, 0, 0), [(0.9, 1.4, 0.9, 0), (2.2, 3.6, 2.2, 0.0)], 'bl_tassel')


ERLANG = mirrored([
    part('waist', 'root', (0, 6, 0), (0, 0, 0), [
        (-5.2, -2, -3.6, 10.4, 4, 7.2, 'hips'),
        (-6.0, -3.6, -4.2, 12, 3, 8.4, 'belt'),
        (-2.0, -4.2, -4.9, 4.0, 3.6, 1.0, 'buckle'),
    ]),
    part('apron', 'waist', (0, 0.2, -4.4), (-0.05, 0, 0), [(-2.6, 0, -0.5, 5.2, 15, 0.7, 'apron')]),
    part('right_tasset', 'waist', (-3.2, -0.4, -3.8), (-0.12, 0.15, 0.1), [(-3.0, 0, -0.5, 6.0, 9.5, 1.0, 'tasset')]),
    part('right_side_tasset', 'waist', (-5.7, -0.4, 0), (0, 0, 0.2), [(-1.0, 0, -3.6, 1.0, 9, 7.2, 'tasset_side')]),
    part('back_tasset', 'waist', (0, -0.4, 4.0), (0.12, 0, 0), [(-5.4, 0, -0.5, 10.8, 9.5, 1.0, 'tasset_back')]),
    part('right_leg', 'root', (-2.8, 8, 0.2), (0, 0, 0), [(-2.4, 0, -2.4, 4.8, 8, 4.8, 'thigh')]),
    part('right_robe', 'right_leg', (0, -0.6, -2.6), (-0.05, 0, 0.04), [(-2.7, 0, -0.5, 5.4, 12.5, 0.6, 'robe')]),
    part('right_shin', 'right_leg', (0, 8, 0), (0, 0, 0), [
        (-2.1, 0, -2.1, 4.2, 5, 4.2, 'shin'),
        (-2.4, 0.4, -2.6, 4.8, 4.6, 2.6, 'greave'),
        (-2.4, 4.8, -2.8, 4.8, 3.2, 5.4, 'boot'),
        (-1.8, 6.0, -4.3, 3.6, 2.0, 1.6, 'boot_toe'),
    ]),
    part('body', 'waist', (0, -3, 0), (0, 0, 0), [
        (-5.6, -13, -3.8, 11.2, 13, 7.6, 'torso'),
        (-6.1, -13.4, -4.3, 12.2, 6.8, 8.6, 'breastplate'),
        (-2.6, -12.4, -4.9, 5.2, 5.2, 0.7, 'mirror'),
        (-3.6, -14.6, -3.0, 7.2, 2.0, 6.0, 'gorget'),
        (-6.8, -13.8, -4.6, 13.6, 1.1, 9.2, 'mantle'),
        (-6.4, -13.6, -4.8, 2.2, 2.2, 0.7, 'clasp'),
        (4.2, -13.6, -4.8, 2.2, 2.2, 0.7, 'clasp'),
    ]),
    part('cape', 'body', (0, -13.6, 4.2), (0.1, 0, 0), [(-7.4, 0, -0.4, 14.8, 12, 0.8, 'cape')]),
    part('cape_lower', 'cape', (0, 11.8, 0), (0.06, 0, 0), [(-8.0, 0, -0.4, 16.0, 10, 0.8, 'cape_lower')]),
    part('cape_hem', 'cape_lower', (0, 9.8, 0), (0.06, 0, 0), [(-8.6, 0, -0.4, 17.2, 6, 0.8, 'cape_hem')]),
    part('right_cape_wrap', 'cape', (-7.2, 0.4, -0.2), (0, 0.6, 0.12), [(-0.4, 0, -5.6, 0.8, 11, 5.6, 'cape_wrap')]),
    # the head: a handsome stern face, the third eye
    part('head', 'body', (0, -14.4, -0.2), (0, 0, 0), [
        (-3.6, -8.4, -3.5, 7.2, 7.6, 7.0, 'face'),
        (-3.2, -1.4, -3.2, 6.4, 1.8, 6.2, 'jaw'),
        (-0.8, -5.4, -4.2, 1.6, 2.6, 0.8, 'nose'),
        (-3.9, -8.7, -0.6, 7.8, 8.6, 4.8, 'hair_back'),
        (-0.75, -8.0, -3.85, 1.5, 3.2, 0.45, 'third_eye'),
    ]),
    part('third_lid', 'head', (0, -8.1, -4.0), (0, 0, 0), [(-0.9, 0, -0.25, 1.8, 1.3, 0.3, 'third_lid')]),
    part('third_lid_low', 'head', (0, -4.7, -4.0), (0, 0, 0), [(-0.9, -1.0, -0.25, 1.8, 1.0, 0.3, 'third_lid_low')]),
    part('right_brow', 'head', (-0.6, -6.1, -3.7), (0, 0.1, 0.22), [(-2.8, -0.45, -0.3, 2.8, 0.7, 0.6, 'brow')]),
    part('right_ear', 'head', (-3.7, -5.2, -0.2), (0, 0, 0.06), [(-0.7, -1.4, -0.9, 0.7, 3.6, 1.8, 'ear')]),
    # the three-mountain crown
    part('crown', 'head', (0, -8.2, 0), (0, 0, 0), [
        (-4.0, -1.6, -3.9, 8.0, 1.9, 7.8, 'crown_band'),
        (-3.4, -4.6, -3.4, 6.8, 3.2, 6.8, 'crown_cap'),
        (-2.2, -8.4, -4.6, 4.4, 7.6, 0.7, 'crown_peak'),
        (-0.9, -5.6, -0.9, 1.8, 1.4, 1.8, 'crown_knot'),
    ]),
    part('right_crown_peak', 'crown', (-2.8, -1.2, -3.2), (0, 0.55, -0.22), [(-1.7, -6.0, -0.35, 3.4, 6.0, 0.7, 'crown_peak_s')]),
    part('right_crown_wing', 'crown', (-3.9, -2.2, 1.0), (0.1, 0.25, -0.5), [(-0.35, -5.4, -0.4, 0.7, 5.4, 4.4, 'crown_wing')]),
] + hang('right_crown_tie', 'crown', (-3.6, -0.6, 3.0), (0.3, 0, 0.25), [(1.2, 4.6, 0.3, 0), (1.2, 4.6, 0.3, 0.1), (1.5, 4.0, 0.3, 0.14)], 'tie') + [
    part('right_arm', 'body', (-7.4, -11.8, 0.2), ARM, [
        (-2.1, -1, -2.1, 4.2, 7, 4.2, 'upperarm'),
        (-2.5, 3.6, -2.5, 5.0, 1.1, 5.0, 'armband'),
    ]),
    part('right_pauldron', 'right_arm', (-0.3, -0.7, 0), (0, 0, 0.4), [
        (-3.8, -1.3, -3.4, 5.8, 1.5, 6.8, 'pauldron'),
        (-4.4, 0.0, -3.8, 6.4, 1.7, 7.6, 'pauldron2'),
        (-5.0, 1.5, -4.2, 6.9, 2.0, 8.4, 'pauldron3'),
        (-5.8, -1.4, -3.4, 0.8, 6.8, 6.8, 'pauldron_mane'),
        (-6.5, -0.3, -2.4, 1.3, 4.8, 4.8, 'pauldron_lion'),
    ]),
    part('right_forearm', 'right_arm', (0, 6.2, 0), FOREARM, [
        (-1.9, 0, -1.9, 3.8, 5.6, 3.8, 'forearm'),
        (-2.25, 0.7, -2.25, 4.5, 4.2, 4.5, 'bracer'),
        (-2.9, -1.1, -2.9, 5.8, 1.8, 5.8, 'frill'),
    ]),
    part('right_hand', 'right_forearm', (0, 5.6, 0), (0, 0, 0), [(-1.6, 0, -1.6, 3.2, 3.2, 3.2, 'fist')]),
] + blade())


def pose_left(parts):
    out = []
    for name, parent, pivot, rot, boxes, mirror in parts:
        if name == 'left_arm':
            rot = LARM
        elif name == 'left_forearm':
            rot = LFOREARM
        elif name == 'left_hand':
            boxes = [(-1.6, 0, -1.6, 3.2, 3.2, 3.2, 'fist')]
        out.append((name, parent, pivot, rot, boxes, mirror))
    return out


ERLANG = pose_left(ERLANG)


def skin():
    face_dark = kit.skin_shade(FACE, 0.4)

    def silver_scales(p, f, light=1.0):
        for s, r in f.items():
            if s in ('top', 'bottom'):
                p.face(r, SILVER_DEEP, LIGHT[s], noise=3)
            else:
                kit.fish_scales(p, r, (214, 220, 232), (96, 104, 126), light=LIGHT[s] * light)

    def lam(p, f):
        for s, r in f.items():
            if s in ('top', 'bottom'):
                p.face(r, SILVER_DEEP, LIGHT[s], noise=3)
            else:
                kit.lamellae(p, r, (204, 212, 226), LACE, pw=4, ph=5, light=LIGHT[s], edge=(96, 104, 126), gleam=SILVER_LIGHT)

    def paint_box(p, style, f, dims):
        if style == 'hips':
            kit.silk(p, f, ROBE, ROBE_DEEP)
        if style == 'belt':                                    # white jade plaques in silver
            p.shaded(f, (40, 50, 90))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(1, fw - 3, 5):
                    for j in range(1, fh - 1):
                        for w in range(3):
                            p.px((fx + i + w, fy + j), (236, 240, 232) if w < 2 else (180, 190, 186))
            kit.gold_rim(p, f, colour=SILVER_LIGHT)
        if style == 'buckle':
            kit.silver(p, f)
            kit.lion(p, f['front'], metal=SILVER, deep=SILVER_DEEP, hi=SILVER_LIGHT, open_=0.3, mane=False, glow=0.3)
        if style == 'apron':                                   # the pale-gold robe's panel: clouds, a blue border, a fringe
            for s, r in f.items():
                p.face(r, ROBE, LIGHT[s], noise=3, fall=0.2)
            fx, fy, fw, fh = f['front']
            kit.frame(p, (fx, fy, fw, fh - 4), LACE, (40, 70, 140))
            for j in range(7, fh - 8, 7):
                kit.cloud(p, fx + fw // 2 - 1, fy + j, ROBE_DEEP, r=1)
            for i in range(fw):
                for j in range(fh - 4, fh):
                    if i % 2 == 0 or j < fh - 2:
                        p.px((fx + i, fy + j), SILVER_LIGHT if (i // 2) % 2 else LACE)
                    else:
                        p.clear((fx + i, fy + j))
        if style in ('tasset', 'tasset_side', 'tasset_back'):
            lam(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), GOLD_LIGHT)
                    p.px((fx + i, fy + 1), GOLD_DEEP)
                if s in ('front', 'back') and fw > 6:
                    for i in range(fw):
                        t = (i % 6) / 5
                        dip = int(2.5 * (1 - abs(t - 0.5) * 2))
                        for j in range(fh - 3 + dip, fh):
                            p.clear((fx + i, fy + j))
                        p.px((fx + i, fy + fh - 4 + dip), GOLD)
        if style in ('thigh', 'shin'):
            kit.silk(p, f, ROBE, ROBE_DEEP, fold=5)
        if style == 'robe':
            kit.silk(p, f, ROBE, ROBE_DEEP, fold=6)
            for s in ('front', 'back'):
                kit.clouds(p, f[s], ROBE_DEEP, every=8, r=1, share=0.5)
                kit.hem(p, f[s], 5, LACE, line=SILVER_LIGHT, deep=SILVER_DEEP)
        if style == 'greave':
            kit.silver(p, f)
            kit.gold_rim(p, f)
            fx, fy, fw, fh = f['front']
            kit.cloud(p, fx + fw // 2 - 2, fy + 3, GOLD, r=1)
        if style == 'boot':
            p.shaded(f, (34, 32, 44))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), SILVER_LIGHT)
                    p.px((fx + i, fy + fh - 1), IVORY)
                kit.scroll_band(p, f[s], 2, SILVER_DEEP, (20, 20, 30), period=6)
        if style == 'boot_toe':
            p.shaded(f, (34, 32, 44))
            kit.gold_rim(p, f, bottom=False, colour=SILVER_LIGHT)

        if style == 'torso':
            silver_scales(p, f)
            kit.gold_rim(p, f)
        if style == 'breastplate':                             # polished silver, moulded, gold-edged
            kit.silver(p, f, round_=0.7)
            for s in ('right', 'left', 'back'):
                kit.fish_scales(p, f[s], (214, 220, 232), (96, 104, 126), light=LIGHT[s])
            fx, fy, fw, fh = f['front']
            for side in (0, 1):
                x0 = fx + (2 if side == 0 else fw // 2 + 1)
                w_ = fw // 2 - 3
                for i in range(w_):
                    t = (i + 0.5) / w_
                    y = fy + fh - 3 - int(3 * math.sin(t * math.pi))
                    p.px((x0 + i, y), SILVER_DEEP)
                    p.px((x0 + i, y + 1), SILVER_LIGHT)
            kit.gold_rim(p, f, width=2)
        if style == 'mirror':
            kit.gold(p, f)
            kit.mirror_plate(p, f['front'], glass=(196, 220, 250), deep=(70, 90, 130))
            bare(p, f, ('top', 'bottom', 'right', 'left'))
        if style == 'gorget':
            for s, r in f.items():
                if s in SIDES:
                    kit.fret(p, r, SILVER, SILVER_DEEP)
                else:
                    p.face(r, SILVER_DEEP, LIGHT[s], noise=3)
            kit.gold_rim(p, f, bottom=False)
        if style == 'mantle':
            kit.silver(p, f)
            fx, fy, fw, fh = f['top']
            cx, cy = fx + fw / 2, fy + fh / 2
            for i, j, (x, y) in each(p, f['top']):
                a = math.atan2(y - cy, (x - cx) * fh / fw)
                if abs(math.sin(a * 4)) < 0.15:
                    p.px((x, y), GOLD_DEEP)
            kit.gold_rim(p, f)
        if style == 'clasp':
            kit.gold(p, f)
            fx, fy, fw, fh = f['front']
            kit.wp.disc(p, fx + fw / 2, fy + fh / 2, 1.0, LACE)
        if style in ('cape', 'cape_lower', 'cape_hem', 'cape_wrap'):    # the white cape, sky-blue lining, silver clouds
            kit.silk(p, f, CAPE, CAPE_DEEP, fold=9, fall=0.25)
            fx, fy, fw, fh = f['front']
            for i, j, xy in each(p, f['front']):
                p.px(xy, noisy(p, k(LINING, 0.9 - 0.2 * j / max(1, fh))))
            outer = 'back' if style != 'cape_wrap' else 'right'
            kit.clouds(p, f[outer], (130, 160, 210), every=10, r=2, share=0.6)
            if style == 'cape_hem':
                kit.hem(p, f['back'], 4, LINING, line=SILVER_LIGHT, deep=SILVER_DEEP)
                for s in ('front', 'back'):
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        cutd = int(1.5 + 1.5 * math.sin(i * 0.7 + 1))
                        for j in range(cutd):
                            p.clear((fx + i, fy + fh - 1 - j))
            if style == 'cape':
                kit.hem(p, f['back'], 3, LINING, line=GOLD, at_bottom=False)
            if style == 'cape_wrap':
                for s in ('right', 'left'):
                    fx, fy, fw, fh = f[s]
                    for i, j, xy in each(p, f[s]):
                        u, v = (i + 0.5) / fw, (j + 0.5) / fh
                        uu = u if s == 'right' else 1 - u
                        if uu > 0.9 - 0.6 * v:
                            p.clear(xy)

        if style == 'face':                                    # handsome, stern; narrowed dark eyes
            kit.flesh(p, f, FACE, round_=0.55)
            fx, fy, fw, fh = f['front']                        # 14x15
            for i in range(fw):
                p.px((fx + i, fy), HAIR)
                if i < 3 or i > fw - 4:
                    p.px((fx + i, fy + 1), HAIR)
            stern_eyes(p, f['front'], 8, (40, 30, 40), FACE, w=4)
            for i in range(fw // 2 - 1, fw // 2 + 1):          # a firm mouth
                p.px((fx + i, fy + fh - 1), (160, 80, 76))
            p.px((fx + fw // 2 - 2, fy + fh - 1), kit.skin_shade(FACE, 0.6))
            p.px((fx + fw // 2 + 1, fy + fh - 1), kit.skin_shade(FACE, 0.6))
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, (x, y) in each(p, f[s]):
                    tb = (fw - 1 - i) if s == 'right' else i
                    if tb < fw * 0.5 or j < 2:
                        p.px((x, y), noisy(p, mix(HAIR, (60, 50, 70), (i + j) % 3 * 0.3)))
            for s in ('back', 'top'):
                for i, j, xy in each(p, f[s]):
                    p.px(xy, noisy(p, mix(HAIR, (60, 50, 70), (i * 2 + j) % 3 * 0.3)))
        if style == 'jaw':
            kit.flesh(p, f, FACE, round_=0.8)
            p.face(f['bottom'], kit.skin_shade(FACE, 0.5), 1.0, noise=2)
        if style == 'nose':
            kit.flesh(p, f, FACE, round_=0.4)
            fx, fy, fw, fh = f['front']
            for j in range(fh - 1):
                p.px((fx + 1, fy + j), kit.skin_shade(FACE, 1.4))
            p.px((fx, fy + fh - 1), face_dark)
            p.px((fx + fw - 1, fy + fh - 1), face_dark)
        if style == 'ear':
            kit.flesh(p, f, FACE, round_=0.5)
        if style == 'brow':
            lock(p, f, along_x=True, tone=BLUE_BLACK)
        if style == 'hair_back':                               # his hair, long, falling down his back
            for s, r in f.items():
                kit.wp.strands(p, r, (34, 28, 40), HAIR, tip=(60, 54, 70))
        if style == 'third_eye':                               # the third eye, upright: gold iris, a dark slit, shining
            for s, r in f.items():
                p.face(r, kit.skin_shade(FACE, 0.5), 1.0, noise=2)
            fx, fy, fw, fh = f['front']                        # 2x5
            for i, j, xy in each(p, f['front']):
                v = (j + 0.5) / fh
                if v < 0.12 or v > 0.88:
                    p.px(xy, (60, 20, 20))
                    continue
                c = mix(FIRE_WHITE, EYE3, abs(v - 0.5) * 2)
                p.lit(xy, c, 0.75, blaze=1.0, hot=FIRE_WHITE)
            for j in range(1, fh - 1):
                p.px((fx + (fw - 1) // 2, fy + j), (30, 10, 10)) if fw > 2 else None
        if style in ('third_lid', 'third_lid_low'):
            kit.flesh(p, f, FACE, round_=0.3)
            for s in ('front',):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), (70, 30, 30))
                    p.px((fx + i, fy), (70, 30, 30))

        if style in ('crown_band', 'crown_knot'):
            kit.gold(p, f)
            if style == 'crown_band':
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(2, fw - 1, 4):
                        p.lit((fx + i, fy + fh // 2), LINING, 0.3)
        if style == 'crown_cap':
            kit.silver(p, f)
            kit.gold_rim(p, f)
        if style in ('crown_peak', 'crown_peak_s'):            # a mountain-peak plaque: silver, gold-edged, a jewel
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u, v = (i + 0.5) / fw, (j + 0.5) / fh
                    if not arch(u, v, waist=0.55):
                        p.clear(xy)
                        continue
                    inner = arch(0.5 + (u - 0.5) * 1.5, v - 0.1, waist=0.45) and v < 0.88 and abs(u - 0.5) < 0.34
                    c = mix(SILVER_LIGHT, SILVER, v) if inner else (mix(GOLD_LIGHT, GOLD, v) if s == 'front' else GOLD_DEEP)
                    p.px(xy, noisy(p, c, 3))
                if s == 'front':
                    kit.wp.disc(p, fx + fw / 2, fy + fh * 0.66, max(1.0, fw * 0.14), LINING, rim=GOLD_LIGHT)
            for s in ('right', 'left', 'top', 'bottom'):
                p.face(f[s], GOLD_DEEP, 1.0, noise=3)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for j in range(int(fh * 0.55)):
                    for i in range(fw):
                        p.clear((fx + i, fy + j))
            bare(p, f, ('top',))
        if style == 'crown_wing':
            for s, r in f.items():
                p.face(r, GOLD_DEEP, 1.0, noise=3)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u = (i + 0.5) / fw
                    if s == 'right':
                        u = 1 - u
                    v = (j + 0.5) / fh
                    reach = 0.25 + 0.75 * (1 - v) ** 0.6
                    feather = 0.08 * math.sin(v * 22)
                    if u > reach + feather:
                        p.clear(xy)
                        continue
                    stripe = int(v * 8 + u * 3) % 2
                    c = mix(SILVER_LIGHT, SILVER, u) if stripe else mix(GOLD_LIGHT, GOLD, u)
                    p.px(xy, noisy(p, c, 3))
        if style == 'tie':
            kit.ribbon(p, f, CAPE, CAPE_DEEP, edge=LINING)

        if style in ('upperarm', 'forearm'):
            kit.silk(p, f, ROBE, ROBE_DEEP, fold=5)
        if style == 'armband':
            kit.gold(p, f)
        if style in ('pauldron', 'pauldron2', 'pauldron3'):
            kit.silver(p, f, round_=0.6)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), GOLD)
                    p.px((fx + i, fy + fh - 2), GOLD_DEEP)
                    if i % 4 == 1 and fh > 3:
                        p.px((fx + i, fy + 1), GOLD_LIGHT)
        if style == 'pauldron_mane':
            kit.silver(p, f)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(p, f[s]):
                    u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh - 0.45
                    r = math.hypot(u, v) * 2
                    if r > 1.0:
                        p.clear(xy)
                    else:
                        curl = 0.5 + 0.5 * math.sin(math.atan2(v, u) * 10 + r * 11)
                        p.px(xy, noisy(p, mix(SILVER_DEEP, SILVER_LIGHT, curl * 0.9)))
        if style == 'pauldron_lion':
            kit.gold(p, f)
            kit.lion(p, f['right'], open_=0.3, mane=False, eye=LINING)
            kit.lion(p, f['left'], open_=0.3, mane=False, eye=LINING)
        if style == 'bracer':
            kit.silver(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                kit.cloud(p, fx + fw // 2 - 2, fy + fh // 2, GOLD, r=1)
            kit.gold_rim(p, f)
        if style == 'frill':
            kit.silk(p, f, ROBE, ROBE_DEEP, fold=3)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), LACE)
                    if i % 3 == 0:
                        p.clear((fx + i, fy + fh - 1))
        if style == 'fist':
            kit.flesh(p, f, FACE)
            for s in ('front', 'right', 'left', 'top'):
                fx, fy, fw, fh = f[s]
                for i in range(1, fw, 2):
                    p.px((fx + i, fy + fh - 2), kit.skin_shade(FACE, 0.6))

        # ── the blade ──
        if style == 'bl_shaft':                                # dark lacquer banded in silver, a cord grip
            p.shaded(f, (40, 36, 52))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(0, fh, 9):
                    for i in range(fw):
                        p.px((fx + i, fy + j), SILVER_LIGHT)
                        p.px((fx + i, fy + j + 1), SILVER_DEEP)
        if style in ('bl_collar', 'bl_butt'):
            kit.silver(p, f, round_=0.7)
        if style == 'bl_mouth':                                # a gold dragon's mouth the blade springs from
            kit.gold(p, f, round_=0.7)
            for s in ('front', 'back'):
                kit.lion(p, f[s], open_=0.35, mane=False, eye=LINING, glow=0.4)
        if style in ('bl_blade', 'bl_prong'):                  # bright steel, a ridge, two edges, cut to its point
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(p, r):
                    u = (i + 0.5) / fw
                    c = mix((252, 254, 255), (150, 164, 190), abs(u - 0.5) * 2) if s in ('front', 'back') else (230, 238, 250)
                    if s in ('front', 'back') and abs(u - 0.5) < 0.1:
                        c = (110, 124, 156)
                    p.px(xy, noisy(p, c, 2))
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    p.flare((fx, fy + j), (190, 220, 255), 0.7)
                    p.flare((fx + fw - 1, fy + j), (190, 220, 255), 0.7)
                if style == 'bl_blade':
                    for i, j, xy in each(p, f[s]):
                        u, v = (i + 0.5) / fw - 0.5, (j + 0.5) / fh
                        half = 0.5 if v > 0.35 else 0.5 * (v / 0.35) ** 0.7
                        if abs(u) > half:
                            p.clear(xy)
                else:
                    point(p, f[s], keep=0.0, down=False, sharp=0.3)
        if style == 'bl_tassel':
            for s, r in f.items():
                kit.wp.strands(p, r, (220, 30, 30), (120, 10, 14))
            if dims[0] > 1.5:
                for s in SIDES:
                    kit.wp.tufts(p, f[s], depth=2)

    return paint_box


def build():
    p = kit.GreatPainter(22)
    slots = paint(ERLANG, p, skin())
    p.save('heaven_erlang_shen')
    emit(ERLANG, slots, kit.JAVA, 'erlang')
    report('Erlang Shen', ERLANG, SIZE, slots)
