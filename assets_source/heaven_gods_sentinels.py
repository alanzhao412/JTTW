"""Thousand-Mile Eye (千里眼) and Wind-Following Ear (顺风耳), who keep watch on the world below from the
Southern Heavenly Gate: a matched pair, as the temples set them either side of a door, built on one frame.

Both: gilt lamellar armour laced in their colour over a robe of it: a moulded breastplate, a round mirror
plate over the belly, a beast's-face buckle, shoulder guards in two tiers bitten by gold lions' heads,
tassets and a long front panel, bracers, greaves and boots; a ribbon in their colour arching behind their
shoulders and streaming down; a gold headband with a jewel, wild dark hair standing up behind it, fierce
brows, fangs at the lip.

Thousand-Mile Eye: jade-green of face, robed and laced in teal; two great bulging eyes, burning gold; two
short horns; his right hand raised flat over his brow, shading his eyes as he peers into the distance; a
halberd (方天画戟) in his left, a crescent blade either side of its point, a red tassel.

Wind-Following Ear: red of face, robed and laced in vermilion; two huge fanned ears, rings of sound shining
in their hollows; one horn; his left hand cupped behind his ear; a bronze broad-axe (钺) on a long haft in
his right, a beast's mask cast on its blade.

Run from the repository root: python assets_source/heaven_gods.py eye ear"""
import math

from fire_court import part, mirrored
from sea_court import aim
from king_yama import k, mixc, noisy, point, circle, lock, BLUE_BLACK
from heaven_gods_kit import *
from heaven_gods_luminaries import tress, arc

# How much larger than their pixels they are drawn (HeavenGodRenderers.SentinelModel.SIZE)
SIZE = 0.72

PLATE = (196, 146, 60)
PLATE_DEEP = (108, 70, 28)

COLOURS = {
    True: dict(cloth=(36, 132, 112), deep=(12, 60, 52), lace=(30, 150, 130), face=(104, 168, 118), hair=BLUE_BLACK,
               ribbon=(70, 190, 150), eye=(255, 226, 80), jewel=(120, 250, 200)),
    False: dict(cloth=(206, 72, 40), deep=(112, 24, 14), lace=(220, 70, 40), face=(204, 84, 62), hair=BLUE_BLACK,
                ribbon=(236, 110, 60), eye=(255, 200, 80), jewel=(255, 120, 60)),
}


def rot_to(d):
    """The turn (x, 0, z) laying a part's +y (down its length) along the direction d."""
    n = math.sqrt(sum(v * v for v in d))
    dx, dy, dz = (v / n for v in d)
    return (math.asin(max(-1, min(1, dz))), 0, math.atan2(-dx, dy))


def sub(a, b):
    return tuple(x - y for x, y in zip(a, b))


SHOULDER = 7.0                  # the shoulder's pivot, out from the middle
ARM_LEN, FORE_LEN = 6.0, 6.0

REST = (-0.12, 0, 0.12)         # an arm at rest, holding the weapon: hanging, the forearm brought forward
REST_FORE = (-0.75, 0, 0)

# Thousand-Mile Eye's right hand shading his brow: the upper arm up beside his head, the forearm across over his brow
SH_AT = (-SHOULDER, -10.4, 0)
EYE_ELBOW = (-7.6, -16.6, -3.4)
EYE_HAND = (-1.0, -22.6, -5.8)
EYE_ARM = rot_to(sub(EYE_ELBOW, SH_AT))
EYE_FORE = aim([EYE_ARM], rot_to(sub(EYE_HAND, EYE_ELBOW)))

# Wind-Following Ear's left hand cupped behind his ear
LS_AT = (SHOULDER, -10.4, 0)
EAR_ELBOW = (11.0, -14.6, 1.4)
EAR_HAND = (6.2, -18.2, 1.8)
EAR_ARM = rot_to(sub(EAR_ELBOW, LS_AT))
EAR_FORE = aim([EAR_ARM], rot_to(sub(EAR_HAND, EAR_ELBOW)))


def flip(boxes):
    return [(-x - w, y, z, w, h, d, s) for (x, y, z, w, h, d, s) in boxes]


def arm(side, rot, fore_rot, hand_boxes, hand_rot=(0, 0, 0)):
    """An arm: a robed upper arm, a bracered forearm, a hand; `side` 'right' (-x) or 'left' (mirrored)."""
    sg = -1 if side == 'right' else 1
    m = side == 'left'
    f = flip if m else (lambda b: b)
    return [
        part(side + '_arm', 'body', (sg * SHOULDER, -10.4, 0), rot, f([(-2, -1.4, -2, 4, 7.4, 4, 'sn_upperarm')]), m),
        part(side + '_forearm', side + '_arm', (0, ARM_LEN, 0), fore_rot, f([(-1.8, 0, -1.8, 3.6, 6.2, 3.6, 'sn_forearm'),
                                                                            (-2.1, 0.8, -2.1, 4.2, 4.6, 4.2, 'sn_bracer')]), m),
        part(side + '_hand', side + '_forearm', (0, FORE_LEN, 0), hand_rot, f(hand_boxes), m),
    ]


FIST = [(-1.5, 0, -1.5, 3, 3, 3, 'sn_fist')]


def frame(eye):
    """The pair's common frame: legs, waist, body, armour, ribbon, the head less its marks."""
    return mirrored([
        part('waist', 'root', (0, 9, 0), (0, 0, 0), [
            (-4.4, -2, -3, 8.8, 4, 6, 'sn_hips'),
            (-5.0, -3.4, -3.5, 10, 2.8, 7, 'sn_belt'),
            (-2.2, -4.4, -4.3, 4.4, 4.4, 1, 'sn_buckle'),
        ]),
        part('apron', 'waist', (0, 0.6, -3.7), (-0.08, 0, 0), [(-2.1, 0, -0.4, 4.2, 13.4, 0.8, 'sn_apron')]),
        part('robe_back', 'waist', (0, 0, 3.1), (0.14, 0, 0), [(-4.6, 0, -0.4, 9.2, 13.6, 0.8, 'sn_robe_back')]),
        part('right_tasset', 'waist', (-2.7, -0.8, -3.3), (-0.14, 0.18, 0.1), [(-2.6, 0, -0.5, 5.2, 7.6, 1, 'sn_tasset')]),
        part('right_side_tasset', 'waist', (-4.8, -0.8, 0), (0, 0, 0.22), [(-1, 0, -2.8, 1, 7.2, 5.6, 'sn_tasset_side')]),
        part('right_sash', 'waist', (-4.2, -2.4, -2.4), (0.04, 0, 0.14), [(-0.8, 0, -0.3, 1.6, 10, 0.6, 'sn_sash')]),
        part('right_leg', 'root', (-2.6, 9, 0.3), (0, 0, 0.03), [(-2.1, 0, -2.1, 4.2, 7.6, 4.2, 'sn_thigh')]),
        part('right_shin', 'right_leg', (0, 7.2, 0), (0, 0, -0.03), [
            (-1.8, 0, -1.8, 3.6, 7, 3.6, 'sn_shin'),
            (-2.2, 0.4, -2.5, 4.4, 5.6, 2.6, 'sn_greave'),
            (-2.0, -1.4, -2.8, 4, 2.6, 1, 'sn_knee'),
            (-2.1, 6.2, -3.8, 4.2, 1.6, 6, 'sn_boot'),
            (-2.3, 5.6, -2.3, 4.6, 1, 4.6, 'sn_boot_cuff'),
        ]),
        part('body', 'waist', (0, -2.6, 0), (0, 0, 0), [
            (-5.4, -11.4, -3.4, 10.8, 7.4, 6.8, 'sn_torso'),
            (-4.6, -4.4, -3.0, 9.2, 4.6, 6, 'sn_belly'),
            (-5.9, -11.8, -3.9, 11.8, 7.6, 7.8, 'sn_breastplate'),
            (-5.0, -10.9, -4.5, 4.4, 4.4, 0.7, 'sn_boss'),
            (0.6, -10.9, -4.5, 4.4, 4.4, 0.7, 'sn_boss'),
            (-4.8, -4.6, -3.4, 9.6, 4.4, 6.8, 'sn_scales'),
            (-2.4, -5.4, -4.3, 4.8, 4.8, 0.8, 'sn_mirror'),
            (-6.2, -12.2, -3.6, 12.4, 1.2, 7.2, 'sn_mantle'),
            (-3.4, -13.4, -2.8, 6.8, 1.8, 5.2, 'sn_gorget'),
            (-1.8, -14.4, -1.8, 3.6, 2.2, 3.6, 'sn_neck'),
        ]),
        part('right_pauldron', 'body', (-6.6, -11.4, 0), (0, 0, 0.45), [
            (-4.0, -1.2, -3.6, 6.6, 2, 7.2, 'sn_pauldron'),
            (-4.8, 0.6, -4.0, 7.2, 2.2, 8.0, 'sn_pauldron2'),
            (-5.6, 2.6, -4.4, 7.8, 2.2, 8.8, 'sn_pauldron3'),
            (-6.5, 0.6, -2.0, 1.0, 4.0, 4.0, 'sn_lionface'),
        ]),
        part('arch', 'body', (0, -11.6, 4.6), (0.15, 0, 0), []),
    ] + arc('arch_seg', 'arch', 0, 0, 0, 11.4, 180, 360, 9, 2, 0.4, 'sn_ribbon') + [
        part('right_ribbon', 'body', (-11.2, -12.0, 4.4), (0.1, 0.35, 0.12), [(-1.1, 0, -0.2, 2.2, 12, 0.4, 'sn_ribbon_a')]),
        part('right_ribbon_2', 'right_ribbon', (0, 11.6, 0), (0.15, 0, 0.18), [(-1.2, 0, -0.2, 2.4, 11, 0.4, 'sn_ribbon_b')]),
        # the head
        part('head', 'body', (0, -14.2, -0.4), (0, 0, 0), [
            (-4, -8, -3.8, 8, 8, 7.6, 'sn_face'),
            (-1.1, -4.4, -4.7, 2.2, 2.6, 1.0, 'sn_nose'),
            (-4.3, -7.0, -4.1, 8.6, 1.4, 8.2, 'sn_band'),
            (-1.2, -7.6, -4.6, 2.4, 2.4, 0.8, 'sn_jewel'),
            (-3.6, -8.6, -2.6, 7.2, 1.8, 6.4, 'sn_hair_top'),
        ]),
        part('jaw', 'head', (0, -0.6, -0.4), (0, 0, 0), [(-3.8, 0, -3.2, 7.6, 2.2, 6.4, 'sn_jaw')]),
        part('right_brow', 'head', (-0.3, -5.6, -4.1), (0, 0.12, 0.42), [(-3.6, -0.8, -0.6, 3.6, 1.3, 1.2, 'sn_brow')]),
    ] + [
        part('hair_%d' % i, 'head', (x, -7.6, z), (rx, 0, rz), [(-1.3, -ln, -1.1, 2.6, ln, 2.2, 'sn_hair')])
        for i, (x, z, rx, rz, ln) in enumerate(((-2.6, 0.6, -0.7, -0.45, 6.5), (0, 1.0, -0.8, 0, 7.5), (2.6, 0.6, -0.7, 0.45, 6.5),
                                                (-1.5, 2.8, -1.25, -0.25, 7), (1.5, 2.8, -1.25, 0.25, 7)))
    ])


def eye_parts():
    return frame(True) + mirrored([
        part('right_eyeball', 'head', (-1.9, -4.2, -3.6), (0, 0.08, 0), [(-1.7, -1.7, -1.6, 3.4, 3.4, 2.0, 'sn_eyeball')]),
        part('right_horn', 'head', (-2.4, -8.0, -1.4), (-0.35, 0, -0.4), [(-0.8, -3.4, -0.8, 1.6, 3.6, 1.6, 'sn_horn'),
                                                                        (-0.5, -5, -0.5, 1, 1.8, 1, 'sn_horn_tip')]),
    ]) + arm('right', EYE_ARM, EYE_FORE, [(-0.7, -0.4, -1.9, 1.4, 4.4, 3.8, 'sn_palm')]) \
       + arm('left', (REST[0], -REST[1], -REST[2]), REST_FORE, FIST) + [
        # the halberd, upright in his left fist
        part('halberd', 'left_hand', (0, 1.6, 0), aim([(REST[0], -REST[1], -REST[2]), REST_FORE], (0.04, 0, 0)), [
            (-0.55, -29, -0.55, 1.1, 43, 1.1, 'sn_shaft'),
            (-1.0, -30.4, -1.0, 2, 2, 2, 'sn_collar'),
            (-0.9, -39.4, -0.35, 1.8, 9.2, 0.7, 'sn_spear'),
            (-1.4, -28.6, -1.4, 2.8, 4, 2.8, 'sn_tassel'),
            (-0.6, 13.8, -0.6, 1.2, 2.4, 1.2, 'sn_butt'),
        ]),
        part('crescent_r', 'halberd', (-0.8, -34.4, 0), (0, 0, 0), [(-4.6, -3.6, -0.3, 4.6, 7.2, 0.6, 'sn_crescent')], False),
        part('crescent_l', 'halberd', (0.8, -34.4, 0), (0, 0, 0), [(0, -3.6, -0.3, 4.6, 7.2, 0.6, 'sn_crescent')], True),
    ]


def ear_parts():
    return frame(False) + mirrored([
        part('right_bigear', 'head', (-3.9, -4.4, 0.0), (0, 0.55, -0.12), [(-9, -6.2, -0.35, 9, 11.4, 0.7, 'sn_bigear')]),
    ]) + [
        part('horn', 'head', (0, -8.4, -2.2), (-0.3, 0, 0), [(-0.9, -4, -0.9, 1.8, 4.2, 1.8, 'sn_horn'),
                                                            (-0.55, -5.8, -0.55, 1.1, 2, 1.1, 'sn_horn_tip')]),
    ] + arm('right', REST, REST_FORE, FIST) + arm('left', EAR_ARM, EAR_FORE, [(-0.6, -0.2, -1.8, 1.2, 3.6, 3.6, 'sn_palm')]) + [
        # the broad-axe, upright in his right fist, its blade outward
        part('axe', 'right_hand', (0, 1.6, 0), aim([REST, REST_FORE], (0.04, 0, 0)), [
            (-0.6, -27, -0.6, 1.2, 41, 1.2, 'sn_shaft'),
            (-1.0, -28.4, -1.0, 2, 2, 2, 'sn_collar'),
            (-0.6, -33.6, -0.6, 1.2, 5.4, 1.2, 'sn_axe_spike'),
            (-1.4, -16.4, -1.4, 2.8, 4, 2.8, 'sn_tassel'),
            (-0.6, 13.8, -0.6, 1.2, 2.4, 1.2, 'sn_butt'),
            (0.6, -25.6, -0.4, 3.0, 1.6, 0.8, 'sn_axe_hook'),
        ]),
        part('axe_blade', 'axe', (-0.8, -22.6, 0), (0, 0, 0), [(-9.4, -6, -0.45, 9.4, 12.4, 0.9, 'sn_axe_blade')]),
    ]


EYE = eye_parts()
EAR = ear_parts()


def painter(eye):
    c = COLOURS[eye]
    cloth, deep, lace, face, hair = c['cloth'], c['deep'], c['lace'], c['face'], c['hair']

    def paint(p, style, f, dims):
        if style == 'sn_hips':
            silk(p, f, cloth, deep)
        if style == 'sn_belt':
            p.shaded(f, (60, 30, 24))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(1, fw - 2, 5):
                    for j in range(1, fh - 1):
                        for w in range(3):
                            p.px((fx + i + w, fy + j), GOLD if w < 2 else GOLD_DEEP)
                hline(p, f[s], 0, GOLD_LIGHT)
        if style == 'sn_buckle':                                  # a gold beast's face, its eyes burning
            gold_box(p, f)
            fx, fy, fw, fh = f['front']                           # 9x9
            cx = fw // 2
            for i in range(fw):
                p.px((fx + i, fy + 1 + int(abs(i - cx + 0.5) < 2)), GOLD_DEEP)
            for i in (cx - 3, cx - 2, cx + 1, cx + 2):
                p.lit((fx + i, fy + 3), c['eye'], glow=0.8)
            for j in range(3, 6):
                p.px((fx + cx, fy + j), GOLD_LIGHT)
            for i in range(1, fw - 1):
                p.px((fx + i, fy + 6), (60, 26, 10))
                p.px((fx + i, fy + 7), IVORY if i % 2 else (60, 26, 10))
            border(p, f['front'], GOLD_DEEP)
        if style in ('sn_apron', 'sn_robe_back'):                 # the robe's long panel: their colour, cloud-worked, a wave border
            for s, r in f.items():
                silk_face(p, r, cloth, deep, LIGHTS[s], 5)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                vline(p, f[s], 0, GOLD)
                vline(p, f[s], fw - 1, GOLD)
                clouds(p, f[s], GOLD_DEEP, every=10, r=1, skip_bottom=6)
                waves(p, f[s], 5, (deep, (240, 220, 160), k(cloth, 1.2), (255, 250, 220)))
        if style in ('sn_tasset', 'sn_tasset_side'):
            for s, r in f.items():
                if s in ('top', 'bottom'):
                    plate(p, {s: r}, PLATE)
                else:
                    lamellar(p, r, PLATE, lace, GOLD_LIGHT, light=LIGHTS[s], pw=3, ph=4)
        if style == 'sn_sash':
            for s, r in f.items():
                silk_face(p, r, c['ribbon'], deep, LIGHTS[s], 4)
                hline(p, r, r[3] - 1, GOLD)
        if style == 'sn_thigh':
            silk(p, f, k(deep, 1.3), k(deep, 0.6), fold=4)
        if style == 'sn_shin':
            silk(p, f, k(deep, 1.3), k(deep, 0.6), fold=4)
        if style == 'sn_greave':
            plate(p, f, PLATE, studs=False)
            fx, fy, fw, fh = f['front']
            for j in range(2, fh - 1):
                p.px((fx + fw // 2, fy + j), GOLD_LIGHT)
            cloud_mark(p, fx + 2, fy + fh // 2, GOLD_LIGHT, 1, f['front'])
        if style == 'sn_knee':
            gold_box(p, f)
            fx, fy, fw, fh = f['front']
            circle(p, fx + fw / 2, fy + fh / 2, 0, 1.6, lace)
        if style == 'sn_boot':
            p.shaded(f, (36, 28, 30))
            for s in SIDES:
                hline(p, f[s], 0, GOLD)
                hline(p, f[s], f[s][3] - 1, IVORY_SHADE)
        if style == 'sn_boot_cuff':
            plate(p, f, PLATE, studs=False)

        if style == 'sn_torso':
            for s, r in f.items():
                if s in ('top', 'bottom'):
                    plate(p, {s: r}, PLATE)
                else:
                    lamellar(p, r, PLATE, lace, GOLD_LIGHT, light=LIGHTS[s])
        if style == 'sn_belly':
            silk(p, f, cloth, deep)
        if style == 'sn_breastplate':                             # moulded to the chest, gold-rimmed, a cloud scroll along its top
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    across = abs((i + 0.5) / fw - 0.5) * 2
                    swell = 1 - abs(across - 0.5) * 1.2 if s == 'front' else 1 - across * 0.5
                    p.px(xy, noisy(p, k(PLATE, (1.05 + 0.3 * swell - 0.3 * j / max(1, fh - 1)) * LIGHTS[s]), 2))
                if s in SIDES:
                    hline(p, r, 0, GOLD_LIGHT)
                    hline(p, r, 1, PLATE_DEEP)
                    hline(p, r, fh - 1, lace)
                    hline(p, r, fh - 2, GOLD_LIGHT)
                if s in ('front', 'back') and fh > 8:
                    scale_mail(p, (fx + 2, fy + fh - 6, fw - 4, 4), PLATE, PLATE_DEEP, LIGHTS[s])
                if s == 'front':
                    for j in range(3, fh - 2):
                        p.px((fx + fw // 2, fy + j), PLATE_DEEP)
                    for i in range(3, fw - 3):
                        p.px((fx + i, fy + 3 - (i // 3) % 2), GOLD_LIGHT if (i // 3) % 2 == 0 else GOLD)
        if style == 'sn_boss':                                    # a round boss on the breast: a lion's face cast in gold
            for s, (fx, fy, fw, fh) in f.items():
                if s in ('front', 'back'):
                    def col(r, a, d):
                        if r > 0.82:
                            return GOLD_DEEP if int((a + math.pi) * 8 / math.pi) % 2 else GOLD_LIGHT
                        if r > 0.7:
                            return lace
                        return GOLD
                    disc(p, (fx, fy, fw, fh), col)
                    if s == 'front':                              # 9x9: brows, eyes, the muzzle
                        for i in range(2, 7):
                            p.px((fx + i, fy + 2 + (i == 4)), GOLD_DEEP)
                        p.lit((fx + 3, fy + 3), c['eye'], glow=0.5)
                        p.lit((fx + 5, fy + 3), c['eye'], glow=0.5)
                        p.px((fx + 4, fy + 4), GOLD_LIGHT)
                        for i in range(3, 6):
                            p.px((fx + i, fy + 6), PLATE_DEEP)
                else:
                    p.face((fx, fy, fw, fh), GOLD_DEEP, 1.0, noise=3)
        if style == 'sn_scales':
            for s, r in f.items():
                if s in ('top', 'bottom'):
                    p.face(r, PLATE_DEEP, LIGHTS[s], noise=3)
                    continue
                scale_mail(p, r, PLATE, PLATE_DEEP, LIGHTS[s])
                hline(p, r, r[3] - 1, PLATE_DEEP)
        if style == 'sn_mirror':                                  # the round mirror over the belly, its boss their colour
            for s, (fx, fy, fw, fh) in f.items():
                if s in ('front', 'back'):
                    def col(r, a, d):
                        if r > 0.78:
                            return GOLD_LIGHT if int((a + math.pi) * 6 / math.pi) % 2 else GOLD
                        if r < 0.22:
                            return ('glow', c['jewel'], 0.7)
                        return (236, 240, 250) if d[0] + d[1] < -0.3 and r > 0.35 else (150, 160, 180)
                    disc(p, (fx, fy, fw, fh), col)
                else:
                    p.face((fx, fy, fw, fh), GOLD_DEEP, 1.0, noise=3)
        if style == 'sn_mantle':
            p.shaded(f, k(deep, 1.2))
            for s in SIDES + ('top',):
                fx, fy, fw, fh = f[s]
                hline(p, f[s], 0, GOLD_LIGHT)
                for i in range(1, fw, 4):
                    p.px((fx + i, fy + fh // 2), GOLD)
        if style == 'sn_gorget':
            plate(p, f, PLATE)
        if style == 'sn_neck':
            skin_box(p, f, k(face, 0.82))
        if style in ('sn_pauldron', 'sn_pauldron2', 'sn_pauldron3'):
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    t = (1.3 - 0.5 * j / max(1, fh - 1)) * LIGHTS[s]
                    p.px(xy, noisy(p, k(PLATE, t), 3))
                if s in SIDES:
                    hline(p, r, fh - 1, lace)
                    for i in range(1, fw - 1, 3):
                        p.px((fx + i, fy + 1), GOLD_LIGHT)
                if s == 'bottom':
                    border(p, r, GOLD_LIGHT)
            fx, fy, fw, fh = f['top']                             # a cloud scroll on the top
            cloud_mark(p, fx + fw // 2 - 2, fy + fh // 2, GOLD_LIGHT, 1, f['top'])
        if style == 'sn_lionface':
            gold_box(p, f)
            fx, fy, fw, fh = f['right']                           # 8x7
            for i in (1, 2, fw - 3, fw - 2):
                p.lit((fx + i, fy + 2), c['eye'], glow=0.7)
            for i in range(1, fw - 1):
                p.px((fx + i, fy + 4), (60, 20, 10))
                p.px((fx + i, fy + 5), IVORY if i % 2 else (60, 20, 10))

        if style in ('sn_ribbon', 'sn_ribbon_a', 'sn_ribbon_b'):
            along_x = style == 'sn_ribbon'
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    along = i if along_x else j
                    across = (j / max(1, fh - 1)) if along_x else (i / max(1, fw - 1))
                    p.px(xy, noisy(p, k(c['ribbon'], LIGHTS[s] * (1 + 0.12 * math.sin(along * 0.5)) * (1.08 - 0.25 * abs(across - 0.4))), 2))
                if s in ('front', 'back'):
                    if along_x:
                        hline(p, r, 0, GOLD)
                        hline(p, r, fh - 1, GOLD)
                    else:
                        vline(p, r, 0, GOLD)
                        vline(p, r, fw - 1, GOLD)
            if style == 'sn_ribbon_b':
                for s in ('front', 'back'):
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        cut = int(4 * (1 - abs(i + 0.5 - fw / 2) / (fw / 2)))
                        for j in range(cut):
                            p.clear((fx + i, fy + fh - 1 - j))

        if style == 'sn_upperarm':
            silk(p, f, cloth, deep, fold=4)
        if style == 'sn_forearm':
            skin_box(p, f, face)
        if style == 'sn_bracer':
            plate(p, f, PLATE, studs=False)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(2, fh - 2):
                    p.px((fx + fw // 2, fy + j), lace)
        if style == 'sn_fist':
            hand(p, f, face, nails=(40, 30, 30))
        if style == 'sn_palm':                                    # a hand held flat (or cupped): the fingers' lines along it
            skin_box(p, f, face)
            for s in SIDES + ('top', 'bottom'):
                fx, fy, fw, fh = f[s]
                if fw > 3:
                    for i in range(1, fw, 2):
                        for j in range(fh // 3, fh):
                            p.px((fx + i, fy + j), k(face, 0.78))
                hline(p, f[s], fh - 1, (40, 30, 30))

        # the weapons
        if style == 'sn_shaft':                                   # red lacquer, banded gold
            p.shaded(f, (150, 30, 26))
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(0, fh, 9):
                    hline(p, f[s], j, GOLD)
                vline(p, f[s], 0, (100, 16, 14))
        if style in ('sn_collar', 'sn_butt'):
            gold_box(p, f)
        if style == 'sn_spear':
            steel_box(p, f)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    p.px((fx + fw // 2, fy + j), (250, 252, 255))
            for s in SIDES:
                point(p, f[s], keep=0.5, down=False, sharp=0.28)
        if style == 'sn_tassel':
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, k((200, 30, 28), (1.1 - 0.35 * (i % 2)) * LIGHTS[s]), 4))
                if s in SIDES:
                    for i in range(fw):
                        for j in range(p.rnd.randint(0, 2)):
                            p.clear((fx + i, fy + fh - 1 - j))
        if style == 'sn_crescent':                                # a crescent blade (月牙), its edge outward
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                def mask(s_, t):                                  # s_ 0 at the shaft .. 1 outward
                    u = (t - 0.5) * 2
                    outer = 0.25 + 0.75 * math.sqrt(max(0, 1 - u * u))
                    inner = 0.55 * math.sqrt(max(0, 1 - u * u)) - 0.1
                    return inner < s_ < outer and abs(u) < 0.98
                cutout(p, f[s], mask, lambda s_, t: mixc((250, 252, 255), (150, 158, 176), 1 - s_), flip=(s == 'front'))
            bare(p, f)
        if style == 'sn_axe_spike':
            steel_box(p, f)
            for s in SIDES:
                point(p, f[s], keep=0.5, down=False, sharp=0.35)
        if style == 'sn_axe_hook':
            metal(p, f, BRONZE, BRONZE_DEEP)
        if style == 'sn_axe_blade':                               # the bronze broad-axe (钺): a rounded edge outward, a beast's mask cast on it
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                def mask(s_, t):                                  # s_ 0 at the edge .. 1 at the haft
                    u = (t - 0.5) * 2
                    edge = 0.18 * u * u
                    half = 0.5 - 0.22 * s_
                    return s_ > edge and abs(t - 0.5) < half
                def col(s_, t):
                    u = (t - 0.5) * 2
                    e = s_ - 0.18 * u * u
                    if e < 0.08:
                        return (240, 220, 170)                    # its honed edge, bright
                    return mixc(mixc(BRONZE, (236, 190, 120), 0.3), BRONZE_DEEP, min(1, s_ * 0.8))
                cutout(p, f[s], mask, col, flip=(s == 'back'))
                cx, cy = fx + int(fw * 0.55), fy + fh // 2        # the beast's mask: brows, round eyes, a fanged mouth
                for i in range(-4, 5):
                    p.px((cx + i, cy - 4 + (abs(i) < 2)), BRONZE_GREEN)
                for i in (-3, -2, 2, 3):
                    p.lit((cx + i, cy - 2), c['eye'], glow=0.5, skin=GOLD)
                for i in range(-3, 4):
                    p.px((cx + i, cy + 2), BRONZE_DEEP)
                    p.px((cx + i, cy + 3), GOLD if i % 2 else BRONZE_DEEP)
                for j in range(fh):                               # verdigris along the haft's side
                    x = fx + (fw - 2 if s == 'front' else 1)
                    if p.img.getpixel((x, fy + j))[3] and j % 3 == 0:
                        p.px((x, fy + j), BRONZE_GREEN)
            bare(p, f)

        # the head
        if style == 'sn_face':
            hair_box(p, f, hair)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for j in range(3, fh):
                    for i in range(6):
                        x = fx + (fw - 1 - i if s == 'right' else i)
                        p.px((x, fy + j), k(face, 0.92 - 0.03 * i))
            fx, fy, fw, fh = f['front']                           # 16x16
            for i, j, xy in each(f['front']):
                across = abs((i + 0.5) / fw - 0.5) * 2
                p.px(xy, noisy(p, k(face, 1.06 - 0.18 * across ** 2 - 0.1 * j / fh), 2))
            for j in range(3, 6):                                 # the scowl
                p.px((fx + fw // 2 - 1, fy + j), k(face, 0.68))
                p.px((fx + fw // 2, fy + j), k(face, 0.68))
            if eye:                                               # the sockets of his great eyes, deep
                for side in (0, 1):
                    ex = 0 if side == 0 else fw - 8
                    for i, j, xy in each((fx + ex, fy + 5, 8, 7)):
                        d = math.hypot((i + 0.5) / 8 - 0.5, (j + 0.5) / 7 - 0.5)
                        if d < 0.5:
                            p.px(xy, k(face, 0.55))
            else:                                                 # narrowed eyes, listening, under the scowl
                for side in (0, 1):
                    ex = 2 if side == 0 else fw - 2 - 5
                    for i in range(5):
                        p.px((fx + ex + i, fy + 7), (30, 10, 10))
                        p.lit((fx + ex + i, fy + 8), c['eye'] if i in (2, 3) else (250, 240, 220), glow=0.6 if i in (2, 3) else 0)
                        p.px((fx + ex + i, fy + 9), k(face, 0.7))
            for j in range(fh - 5, fh - 1):                       # lines from the nose
                p.px((fx + 3 - (j - fh + 5) // 2, fy + j), k(face, 0.66))
                p.px((fx + fw - 4 + (j - fh + 5) // 2, fy + j), k(face, 0.66))
            for i in range(4, fw - 4):                            # a grim mouth, fangs over the lip
                p.px((fx + i, fy + fh - 1), (60, 16, 16))
            for i in (4, fw - 5):
                p.px((fx + i, fy + fh - 1), IVORY)
                p.px((fx + i, fy + fh - 2), IVORY)
            p.face(f['bottom'], k(face, 0.7), 1.0)
        if style == 'sn_nose':
            skin_box(p, f, face)
            fx, fy, fw, fh = f['front']
            p.px((fx, fy + fh - 1), (40, 14, 12))
            p.px((fx + fw - 1, fy + fh - 1), (40, 14, 12))
            p.face(f['bottom'], k(face, 0.6), 1.0)
        if style == 'sn_band':
            gold_box(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(2, fw, 4):
                    p.px((fx + i, fy + fh // 2), lace)
        if style == 'sn_jewel':
            gold_box(p, f)
            fx, fy, fw, fh = f['front']
            for i, j, xy in each((fx + 1, fy + 1, fw - 2, fh - 2)):
                p.lit(xy, c['jewel'], glow=0.9)
        if style in ('sn_hair_top',):
            hair_box(p, f, hair)
        if style == 'sn_hair':
            tress(p, f, hair, axis='y', up=True, keep=0.4)
        if style == 'sn_jaw':
            skin_box(p, f, face)
            fx, fy, fw, fh = f['front']
            for i, j, xy in each(f['front']):                     # a short beard along the jaw, dark
                if j >= 1 and (i < 3 or i > fw - 4 or j >= 2):
                    p.px(xy, shade_of(hair, 0.9 + ((i + j) % 3) * 0.4))
            for s in ('right', 'left', 'bottom'):
                for i, j, xy in each(f[s]):
                    p.px(xy, shade_of(hair, 0.8 + ((i + 2 * j) % 3) * 0.4))
        if style == 'sn_brow':
            brow_box(p, f, hair)

        if style == 'sn_eyeball':                                 # a great bulging eye: white, a burning gold iris, a black pupil; red-rimmed
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    p.lit(xy, (240, 236, 220), glow=0.45)
                if s in SIDES and s != 'front':
                    hline(p, r, 0, (150, 40, 30))
            fx, fy, fw, fh = f['front']                           # 7x7
            def col(r, a, d):
                if r > 1.0:
                    return (150, 40, 30)
                if r > 0.86:
                    return (180, 50, 40)
                if r < 0.24:
                    return (8, 6, 8)
                if r < 0.68:
                    return ('glow', mixc((255, 250, 200), c['eye'], r / 0.68), 1.0)
                return ('glow', (250, 246, 236), 0.6)
            disc(p, f['front'], col, clear_out=False)
            p.lit((fx + 2, fy + 2), (255, 255, 255), glow=1.0)
        if style in ('sn_horn', 'sn_horn_tip'):                   # a short horn, ivory ringed, dark at its point
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    t = j / max(1, fh - 1)
                    base = mixc((90, 70, 50), IVORY, t) if style == 'sn_horn_tip' else mixc(IVORY, (220, 200, 160), t)
                    if style == 'sn_horn' and j % 3 == 0:
                        base = k(base, 0.85)
                    p.px(xy, noisy(p, k(base, LIGHTS[s]), 2))
            if style == 'sn_horn_tip':
                for s in SIDES:
                    point(p, f[s], keep=0.5, down=False, sharp=0.5)
        if style == 'sn_bigear':                                  # a huge fanned ear: its rim, its hollows, rings of sound shining in them
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                def mask(s_, t):                                  # s_ 0 at its outer rim .. 1 at the head
                    cx_, cy_ = 0.45, 0.42
                    d = math.hypot((s_ - cx_) / 0.55, (t - cy_) / 0.48)
                    lobe = math.hypot((s_ - 0.72) / 0.28, (t - 0.86) / 0.16)
                    return (d < 1 and s_ < 0.97) or lobe < 1
                def col(s_, t):
                    d = math.hypot((s_ - 0.45) / 0.55, (t - 0.42) / 0.48)
                    if d > 0.8:
                        return k(face, 1.08)                      # the rim, rolled, lit
                    if d > 0.68:
                        return k(face, 0.62)                      # the fold inside it
                    return mixc(k(face, 0.82), (230, 120, 110), 0.25)
                cutout(p, f[s], mask, col, flip=(s == 'back'))
                if s == 'front':
                    for i, j, xy in each(f[s]):
                        s_, t = (i + 0.5) / fw, (j + 0.5) / fh
                        d = math.hypot((s_ - 0.55) / 0.5, (t - 0.45) / 0.44)
                        if d < 0.66 and p.img.getpixel(xy)[3] and abs((d * 4.2) % 1.0 - 0.5) < 0.16:
                            p.lit(xy, (255, 200, 120), glow=0.75, skin=k(face, 0.95))
            bare(p, f)

    return paint
