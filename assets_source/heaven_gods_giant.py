"""The Great Spirit Giant (巨灵神), Li Jing's vanguard, who fought the Monkey King at the gate of the Water
Curtain Cave and broke his axe on the iron cudgel (chapter 4): a towering god some four blocks tall.

Heavy armour of gilt-bronze lamellae laced in red: a moulded breastplate with two round mirror plates
(护心镜), a gold beast's face (吞口) for his belt buckle, its eyes burning; shoulder guards in three tiers each
bitten by a gold lion's head; tassets before and at the hips over a crimson robe falling to his shins;
bracers, greaves and armoured boots; a crimson cloak worked with gold clouds down his back and a red silk
sash looping behind his shoulders. A fierce dark-ruddy face: bulging eyes burning gold under brows knit
into a V, a broad nose, a roaring mouth, a great black beard flaring out in locks and whiskers. A
phoenix-winged helmet: a gilt bowl, upswept wings, a flame crest set with a burning jewel, a spike and a
red tassel on top, a lamellar neck guard. In each fist one of his pair of great broad axes (宣花斧): a long
red haft banded in gold, a huge crescent blade of steel etched with gold flowers, a spike at its back.

Run from the repository root: python assets_source/heaven_gods.py giant"""
import math

from fire_court import part, mirrored
from sea_court import aim
from king_yama import k, mixc, noisy, point, circle, lock, BLUE_BLACK
from heaven_gods_kit import *

# How much larger than his pixels he is drawn (HeavenGodRenderers.GiantModel.SIZE)
SIZE = 0.96

PLATE = (184, 132, 56)          # gilt bronze
PLATE_DEEP = (104, 66, 28)
LACE = (184, 34, 30)
ROBE = (176, 30, 32)
ROBE_DEEP = (92, 10, 16)
TROUSERS = (36, 70, 74)
FACE = (172, 86, 58)
EYE = (255, 206, 70)

ARM, FOREARM = (-0.12, 0, 0.2), (-0.75, -0.15, 0)
AXE = aim([ARM, FOREARM], (0.02, 0, -0.05))

W, D = 9.5, 5.6                 # half the chest's breadth and depth

PARTS = mirrored([
    # the waist: hips, belt and its beast-face buckle
    part('waist', 'root', (0, -1, 0), (0, 0, 0), [
        (-W + 1.5, -3, -D + 0.8, 2 * W - 3, 5, 2 * D - 1.6, 'hips'),
        (-W + 0.6, -4.6, -D + 0.1, 2 * W - 1.2, 3.6, 2 * D - 0.2, 'belt'),
        (-3.6, -6.4, -D - 1.1, 7.2, 6.6, 1.6, 'buckle'),
        (-2.2, -0.2, -D - 0.8, 4.4, 2.2, 1.0, 'buckle_ring'),
    ]),
    # the robe's skirt falling to his shins, the tassets over it
    part('robe_front', 'waist', (0, 0.5, -D + 0.6), (-0.06, 0, 0), [(-5, 0, -0.5, 10, 18, 1, 'robe_panel')]),
    part('robe_back', 'waist', (0, 0.5, D - 0.6), (0.1, 0, 0), [(-W + 1, 0, -0.5, 2 * W - 2, 19, 1, 'robe_back')]),
    part('right_tasset', 'waist', (-W / 2 - 0.4, -0.6, -D + 0.1), (-0.12, 0.15, 0.1), [(-4.2, 0, -0.7, 8.4, 12, 1.4, 'tasset')]),
    part('right_side_tasset', 'waist', (-W + 0.6, -0.6, 0), (0, 0, 0.2), [(-1.4, 0, -D + 0.6, 1.4, 11, 2 * D - 1.2, 'tasset_side')]),
    part('apron', 'waist', (0, 0.4, -D - 0.4), (-0.08, 0, 0), [(-2.6, 0, -0.4, 5.2, 14, 0.8, 'apron')]),
    part('right_sash', 'waist', (-W + 1.4, -3.4, -D + 1.2), (0.04, 0, 0.12), [
        (-1, 0, -0.4, 2, 15, 0.8, 'sash_end'), (-0.4, 0.5, 0.8, 2, 11, 0.8, 'sash_end')]),
    # the legs: trousered thighs, greaved shins, armoured boots
    part('right_leg', 'root', (-4.6, -1, 0.4), (0, 0, 0.02), [(-3.5, 0, -3.5, 7, 12.5, 7, 'thigh')]),
    part('right_shin', 'right_leg', (0, 12, 0), (0, 0, 0), [
        (-3, 0, -3, 6, 11, 6, 'shin'),
        (-3.5, 0.6, -3.8, 7, 9, 4, 'greave'),
        (-3.3, -1.8, -4.2, 6.6, 4, 1.6, 'kneeguard'),
        (-3.4, 10, -5.6, 6.8, 3, 9.4, 'boot'),
        (-3.6, 9.4, -3.8, 7.2, 1.2, 7.6, 'boot_cuff'),
    ]),
    # the body: a broad chest over a narrower belly, the breastplate, its mirrors, the gorget
    part('body', 'waist', (0, -4, 0), (0, 0, 0), [
        (-W, -20, -D, 2 * W, 12, 2 * D, 'torso'),
        (-W + 1.2, -8.4, -D + 0.6, 2 * W - 2.4, 8.4, 2 * D - 1.2, 'belly'),
        (-W - 0.6, -20.4, -D - 0.8, 2 * W + 1.2, 12.6, 2 * D + 1.6, 'breastplate'),
        (-W + 0.6, -8.4, -D - 0.2, 2 * W - 1.2, 7.6, 2 * D + 0.4, 'abdomen'),
        (-W + 1.2, -18.6, -D - 1.4, 7, 7, 0.8, 'mirror'),
        (W - 8.2, -18.6, -D - 1.4, 7, 7, 0.8, 'mirror'),
        (-6, -22.2, -D + 0.6, 12, 2.6, 2 * D - 1.2, 'gorget'),
        (-W - 0.8, -20.8, -D - 0.4, 2 * W + 1.6, 1.4, 2 * D + 0.8, 'mantle'),
        (-2.6, -22.8, -2.6, 5.2, 3, 5.2, 'neck'),
    ]),
    # the cloak, down his back, its foot parted and blown back
    part('cape', 'body', (0, -20.4, D + 1.2), (0.12, 0, 0), [(-W - 0.6, 0, -0.5, 2 * W + 1.2, 16, 1, 'cape')]),
    part('right_cape_lower', 'cape', (-W / 2, 15.6, 0), (0.1, 0, 0.06), [(-W / 2 - 0.6, 0, -0.5, W + 0.6, 16, 1, 'cape_lower')]),
    # the sash looping behind his shoulders, its ends streaming
    part('loop', 'body', (0, -19, D + 2.6), (0.15, 0, 0), []),
] + [
    part('loop_%d' % i, 'loop', (15 * math.cos(math.radians(a)), 15 * 0.55 * math.sin(math.radians(a)), 0),
         (0, 0, math.atan2(15 * 0.55 * math.cos(math.radians(a)), -15 * math.sin(math.radians(a)))), [(-2.6, -1.4, -0.3, 5.2, 2.8, 0.6, 'loop')])
    for i, a in enumerate(range(190, 360, 17))
] + [
    part('right_streamer', 'body', (-14.6, -19.6, D + 2.6), (0.12, 0.3, 0.18), [(-1.4, 0, -0.3, 2.8, 15, 0.6, 'streamer')]),
    part('right_streamer_2', 'right_streamer', (0, 14.6, 0), (0.2, 0, 0.18), [(-1.5, 0, -0.3, 3, 14, 0.6, 'streamer_end')]),
    # the arms: armoured shoulders, red-sleeved upper arms, bracers, great fists
    part('right_arm', 'body', (-W - 3, -18, 0), ARM, [(-3.2, -2, -3.2, 6.4, 11, 6.4, 'upperarm'),
                                                     (-3.6, 6.6, -3.6, 7.2, 1.6, 7.2, 'armband')]),
    part('right_pauldron', 'right_arm', (-0.4, -1.8, 0), (0, 0, 0.42), [
        (-5.0, -1.4, -4.6, 8, 2, 9.2, 'pauldron'),
        (-5.8, 0.4, -5.1, 8.6, 2, 10.2, 'pauldron2'),
        (-6.6, 2.2, -5.6, 9.2, 2.4, 11.2, 'pauldron3'),
        (-3.2, -2.8, -4.2, 1.6, 1.6, 8.4, 'pauldron_crest'),
        (-7.4, -0.4, -2.6, 1.2, 5.2, 5.2, 'lionface'),
        (-7.9, 2.0, -1.8, 0.6, 1.6, 3.6, 'lionjaw'),
    ]),
    part('right_forearm', 'right_arm', (0, 9, 0), FOREARM, [
        (-2.8, 0, -2.8, 5.6, 9, 5.6, 'forearm'),
        (-3.3, 0.8, -3.3, 6.6, 7, 6.6, 'bracer'),
    ]),
    part('right_hand', 'right_forearm', (0, 9, 0), (0, 0, 0), [(-2.6, 0, -2.6, 5.2, 5, 5.2, 'fist')]),
    # his great axe, upright in his fist: the haft, the crescent blade outward, a spike behind, a point on top
    part('right_axe', 'right_hand', (0, 2.6, 0), AXE, [
        (-0.8, -25, -0.8, 1.6, 34, 1.6, 'haft'),
        (-1.3, -26.4, -1.3, 2.6, 2.4, 2.6, 'axe_collar'),
        (-1.4, -16.4, -1.4, 2.8, 2.2, 2.8, 'axe_collar'),
        (-0.5, -30.6, -0.5, 1, 4.4, 1, 'axe_point'),
        (-1.1, 8.6, -1.1, 2.2, 2, 2.2, 'axe_butt'),
        (-1.6, -14.8, -1.6, 3.2, 4, 3.2, 'axe_tassel'),
    ]),
    part('right_blade', 'right_axe', (-1, -21, 0), (0, 0, 0), [
        (-12, -8, -0.5, 12, 16, 1.0, 'axe_blade'),
        (-1.4, -3.4, -0.9, 1.4, 6.8, 1.8, 'axe_socket'),
    ]),
    part('right_spike', 'right_axe', (0.8, -21, 0), (0, 0, 0), [(0, -0.8, -0.5, 4.2, 1.6, 1, 'axe_spike')]),
    # the head
    part('head', 'body', (0, -22.4, -0.6), (0, 0, 0), [
        (-5.4, -11, -5, 10.8, 10, 10, 'face'),
        (-1.6, -6.6, -6.2, 3.2, 3.2, 1.4, 'nose'),
        (-2.6, -4.4, -5.6, 5.2, 1.4, 0.8, 'cheeks'),
    ]),
    part('right_eye', 'head', (-2.6, -7.2, -5.1), (0, 0, 0), [(-1.3, -1, -0.6, 2.6, 2, 0.8, 'eye')]),
    part('right_brow', 'head', (-0.2, -8.6, -5.2), (0, 0.12, 0.38), [(-4.4, -1.1, -0.7, 4.4, 1.6, 1.4, 'brow')]),
    part('right_ear', 'head', (-5.4, -6.6, -0.6), (0, -0.25, 0), [(-1, -1.6, -1.2, 1, 3.6, 2.4, 'ear')]),
    part('jaw', 'head', (0, -1, -0.6), (0.06, 0, 0), [(-4.9, 0, -4.2, 9.8, 3, 8.4, 'jaw')]),
    # the beard: whiskers flaring from the cheeks, a moustache curling out, great locks down the chest
    part('right_moustache', 'head', (-0.8, -3.4, -5.6), (0, 0.25, -0.35), [(-4.6, -0.7, -0.7, 4.6, 1.4, 1.4, 'moustache')]),
    part('right_whisker', 'head', (-4.8, -4.2, -3.2), (0.1, 0.2, 0.55), [(-1.4, 0, -1.2, 2.8, 7, 2.4, 'whisker')]),
    part('beard', 'jaw', (0, 2.6, -3.8), (0.22, 0, 0), [(-1.9, 0, -1.2, 3.8, 9, 2.4, 'beard_c')]),
    part('beard_tip', 'beard', (0, 8.6, 0), (-0.15, 0, 0), [(-1.4, 0, -1, 2.8, 5, 2, 'beard_tip')]),
    part('right_beard', 'jaw', (-2.8, 2.2, -3.4), (0.2, 0.15, 0.35), [(-1.5, 0, -1.1, 3, 8.5, 2.2, 'beard_s')]),
    part('right_beard_out', 'jaw', (-4.4, 1.4, -2.0), (0.1, 0.2, 0.7), [(-1.3, 0, -1, 2.6, 7, 2, 'beard_o')]),
    # the phoenix-winged helmet
    part('helm', 'head', (0, -11, 0), (0, 0, 0), [
        (-5.9, -4.6, -5.6, 11.8, 5, 11.2, 'helm'),
        (-4.6, -6.4, -4.4, 9.2, 2, 8.8, 'helm_top'),
        (-6.4, -0.6, -6.0, 12.8, 1.8, 12, 'helm_band'),
        (-0.7, -11.6, -0.7, 1.4, 5.6, 1.4, 'helm_spike'),
        (-2, -9.6, -2, 4, 3.6, 4, 'plume'),
        (-5.8, 0.6, 1.4, 11.6, 6, 5.2, 'helm_neck'),
    ]),
    part('crest', 'helm', (0, -0.4, -6.1), (-0.12, 0, 0), [(-3, -7.6, -0.4, 6, 7.6, 0.8, 'helm_crest')]),
    part('right_helm_wing', 'helm', (-5.6, -2.4, 0.4), (0, 0.15, -0.35), [(-7, -7, -0.3, 7, 8, 0.6, 'helm_wing')]),
    part('right_plume_hair', 'helm', (0, -8.4, 0.6), (-0.6, 0, -0.25), [(-1.2, -0.2, -0.8, 2.4, 9, 1.6, 'plume_hair')]),
])


def roar(p, rect):
    """His mouth, roaring: a dark gape under the moustache, teeth top and bottom, fangs at its corners."""
    fx, fy, fw, fh = rect
    cx = fw / 2
    for i, j, xy in each(rect):
        d = abs(i + 0.5 - cx) / (fw * 0.32)
        if d < 1 and j < 4 - int(2 * d * d):
            p.px(xy, (60, 10, 12) if j > 0 else IVORY)
    for i in range(int(cx - fw * 0.32), int(cx + fw * 0.32)):
        if i % 2 == 0:
            p.px((fx + i, fy), IVORY)
    for i in (int(cx - fw * 0.28), int(cx + fw * 0.28) - 1):
        p.px((fx + i, fy), (250, 246, 230))
        p.px((fx + i, fy + 1), (250, 246, 230))


def paint_giant(p, style, f, dims):
    trim = GOLD_LIGHT
    lam = lambda s, r, **kw: lamellar(p, r, PLATE, LACE, trim, light=LIGHTS[s], **kw)

    if style == 'hips':
        silk(p, f, ROBE, ROBE_DEEP)
    if style == 'belt':                                           # a red-lacquered belt studded with gold plaques
        p.shaded(f, (120, 24, 22))
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(1, fw - 3, 5):
                for j in range(1, fh - 1):
                    for w in range(3):
                        p.px((fx + i + w, fy + j), GOLD if w < 2 else GOLD_DEEP)
            hline(p, f[s], 0, trim)
            hline(p, f[s], fh - 1, GOLD_DEEP)
    if style == 'buckle':                                         # the gold beast's face (吞口), its eyes burning, a ring in its jaws
        gold_box(p, f)
        fx, fy, fw, fh = f['front']                               # 14x13
        cx = fw // 2
        for i in range(fw):                                       # brows knit into a V, horns curling up from them
            d = abs(i - cx + 0.5)
            p.px((fx + i, fy + 2 + int(d < 3)), GOLD_DEEP)
        for (a, b) in ((0, 0), (1, 0), (0, 1), (fw - 1, 0), (fw - 2, 0), (fw - 1, 1)):
            p.px((fx + a, fy + b), GOLD_LIGHT)
        for side in (-1, 1):                                      # the eyes
            for i in range(3):
                for j in range(2):
                    p.lit((fx + cx + side * 2 + (i if side > 0 else -i - 1), fy + 4 + j), EYE if j else (255, 130, 40), glow=0.9)
        for j in range(4, 8):                                     # the nose
            p.px((fx + cx, fy + j), GOLD_LIGHT)
            p.px((fx + cx - 1, fy + j), GOLD_DEEP)
        for i in range(2, fw - 2):                                # the mouth: fangs, a dark gape
            p.px((fx + i, fy + 9), (60, 26, 10))
            p.px((fx + i, fy + 10), IVORY if i % 2 else (60, 26, 10))
        for i in (2, fw - 3):
            for j in range(8, 12):
                p.px((fx + i, fy + j), IVORY)
        border(p, f['front'], GOLD_DEEP)
    if style == 'buckle_ring':
        gold_box(p, f)
        fx, fy, fw, fh = f['front']
        for i, j, xy in each((fx + 2, fy + 1, fw - 4, fh - 2)):
            p.px(xy, (50, 24, 10))

    if style in ('robe_panel', 'robe_back'):                      # the crimson robe under the armour, its hem bordered in gold and waves
        silk(p, f, ROBE, ROBE_DEEP)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            clouds(p, f[s], GOLD_DEEP, every=11, r=1, skip_bottom=8)
            waves(p, f[s], 6, ((92, 10, 16), (230, 180, 70), (40, 80, 90), (250, 230, 160)))
    if style in ('tasset', 'tasset_side'):
        for s, r in f.items():
            if s in ('top', 'bottom'):
                plate(p, {s: r}, PLATE)
            else:
                lam(s, r)
                fx, fy, fw, fh = r
                for i in range(2, fw - 1, 4):
                    p.px((fx + i, fy + 3), GOLD_LIGHT)
        fx, fy, fw, fh = f['front']
        if style == 'tasset':                                     # a gold cloud scroll at its head
            for i in range(2, fw - 2):
                if (i // 3) % 2 == 0:
                    p.px((fx + i, fy + 2), GOLD_LIGHT)
    if style == 'apron':                                          # a hanging panel of red silk, gold-bordered, a beast's mask on it
        for s, r in f.items():
            p.face(r, ROBE, LIGHTS[s], noise=3, fall=0.25)
        fx, fy, fw, fh = f['front']
        border(p, f['front'], GOLD, inner=GOLD_DEEP)
        for j in range(4, fh - 4, 7):
            cloud_mark(p, fx + fw // 2 - 3, fy + j, GOLD, 1, f['front'])
    if style == 'sash_end':                                       # red silk sash ends, gold-tipped
        for s, r in f.items():
            silk_face(p, r, (200, 40, 36), (110, 14, 14), LIGHTS[s], 4)
            hline(p, r, r[3] - 1, GOLD)
            hline(p, r, r[3] - 2, GOLD_DEEP)

    if style == 'thigh':                                          # dark teal trousers, folded
        silk(p, f, TROUSERS, (14, 34, 38), fold=5)
    if style == 'shin':
        silk(p, f, TROUSERS, (14, 34, 38), fold=5)
    if style == 'greave':                                         # a gilt greave: rims, a ridge, cloud studs
        plate(p, f, PLATE, studs=False)
        fx, fy, fw, fh = f['front']
        for j in range(2, fh - 1):
            p.px((fx + fw // 2, fy + j), GOLD_LIGHT)
            p.px((fx + fw // 2 - 1, fy + j), PLATE_DEEP)
        for j in range(4, fh - 3, 5):
            cloud_mark(p, fx + 3, fy + j, GOLD_LIGHT, 1, f['front'])
    if style == 'kneeguard':                                      # a lion's face on the knee
        gold_box(p, f)
        fx, fy, fw, fh = f['front']
        circle(p, fx + fw / 2, fy + fh / 2, 0, 2.8, GOLD_DEEP)
        p.lit((fx + fw // 2 - 2, fy + fh // 2 - 1), EYE, glow=0.6)
        p.lit((fx + fw // 2 + 1, fy + fh // 2 - 1), EYE, glow=0.6)
    if style == 'boot':                                           # an armoured boot, its toe turned up
        p.shaded(f, (40, 30, 30))
        for s in SIDES:
            hline(p, f[s], 0, GOLD)
            hline(p, f[s], f[s][3] - 1, IVORY_SHADE)
        fx, fy, fw, fh = f['top']
        for i, j, xy in each(f['top']):
            if j > fh * 0.55:
                p.px(xy, noisy(p, PLATE, 4))
        border(p, f['top'], GOLD_DEEP)
    if style == 'boot_cuff':
        plate(p, f, PLATE, studs=False)

    if style == 'torso':                                          # the back: lamellar, a gold-rimmed back plate
        for s, r in f.items():
            if s in ('top', 'bottom'):
                plate(p, {s: r}, PLATE)
            else:
                lam(s, r)
        fx, fy, fw, fh = f['back']
        for i, j, xy in each((fx + 4, fy + 3, fw - 8, fh - 6)):
            if i in (0, fw - 9) or j in (0, fh - 7):
                p.px(xy, GOLD_LIGHT)
    if style == 'belly':
        silk(p, f, ROBE, ROBE_DEEP)
    if style == 'breastplate':                                    # moulded to his chest: two great swells, gold-rimmed, a scroll along its top
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                across = abs((i + 0.5) / fw - 0.5) * 2
                swell = 1 - abs(across - 0.5) * 1.2 if s == 'front' else 1 - across * 0.5
                t = (1.05 + 0.3 * swell - 0.3 * j / max(1, fh - 1)) * LIGHTS[s]
                p.px(xy, noisy(p, k(PLATE, t), 2))
            if s in SIDES:
                for w in (0, 1):
                    hline(p, r, w, GOLD_LIGHT if w == 0 else PLATE_DEEP)
                    hline(p, r, fh - 1 - w, GOLD_LIGHT if w == 0 else PLATE_DEEP)
                for j in range(4, fh - 2, 3):
                    p.px((fx + 1, fy + j), GOLD_LIGHT)
                    p.px((fx + fw - 2, fy + j), GOLD_LIGHT)
            if s == 'front':
                for j in range(3, fh - 2):                        # the line between the swells
                    p.px((fx + fw // 2, fy + j), PLATE_DEEP)
                for i in range(3, fw - 3):                        # a gold cloud scroll along the top
                    if (i // 3) % 2 == 0:
                        p.px((fx + i, fy + 3), GOLD_LIGHT)
                    else:
                        p.px((fx + i, fy + 2), GOLD)
    if style == 'mirror':                                         # a round mirror plate: polished, a gold rim, a boss
        for s, (fx, fy, fw, fh) in f.items():
            if s in ('front', 'back'):
                def c(r, a, d):
                    if r > 0.8:
                        return GOLD_LIGHT if int((a + math.pi) * 6 / math.pi) % 2 else GOLD
                    if r < 0.18:
                        return ('glow', (255, 230, 150), 0.6)
                    hl = d[0] + d[1] < -0.3
                    return (236, 240, 250) if hl and r > 0.35 else (150, 160, 180) if r > 0.3 else (190, 196, 210)
                disc(p, (fx, fy, fw, fh), c)
            else:
                p.face((fx, fy, fw, fh), GOLD_DEEP, 1.0, noise=3)
    if style == 'abdomen':                                        # fish-scale over the belly, gold-edged rows
        for s, r in f.items():
            if s in ('top', 'bottom'):
                p.face(r, PLATE_DEEP, LIGHTS[s], noise=3)
                continue
            scale_mail(p, r, PLATE, PLATE_DEEP, LIGHTS[s])
            hline(p, r, 0, GOLD_LIGHT)
            hline(p, r, r[3] - 1, PLATE_DEEP)
    if style == 'gorget':
        plate(p, f, PLATE)
    if style == 'mantle':
        p.shaded(f, (120, 24, 22))
        for s in SIDES + ('top',):
            fx, fy, fw, fh = f[s]
            hline(p, f[s], 0, GOLD_LIGHT)
            hline(p, f[s], fh - 1, GOLD_DEEP)
            for i in range(1, fw, 4):
                p.px((fx + i, fy + fh // 2), GOLD)
    if style == 'neck':
        skin_box(p, f, k(FACE, 0.8))

    if style in ('cape', 'cape_lower'):                           # the crimson cloak, its folds, gold clouds on it, a gold border
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                fold = 1 + 0.13 * math.sin(i * 0.55) + 0.05 * math.sin(i * 1.7)
                p.px(xy, noisy(p, k(ROBE, fold * LIGHTS[s] * (1.05 - 0.15 * j / fh)), 3))
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            clouds(p, f[s], GOLD_DEEP if s == 'front' else GOLD, every=12, r=2)
            if style == 'cape':
                hline(p, f[s], 0, GOLD)
                hline(p, f[s], 1, GOLD_DEEP)
            else:
                hem_band(p, f[s], 4, (60, 8, 12), GOLD, GOLD_DEEP)
                for i in range(fw):                               # its foot, a little ragged in the wind
                    cut = int(2 * abs(math.sin(i * 0.5)))
                    for j in range(cut):
                        p.clear((fx + i, fy + fh - 1 - j))

    if style in ('loop', 'streamer', 'streamer_end'):             # the red silk sash, its edges gold
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                wave = 1 + 0.12 * math.sin((i if style == 'loop' else j) * 0.5)
                p.px(xy, noisy(p, k((214, 44, 38), LIGHTS[s] * wave), 2))
            if s in ('front', 'back'):
                if style == 'loop':
                    hline(p, r, 0, GOLD)
                    hline(p, r, fh - 1, GOLD)
                else:
                    vline(p, r, 0, GOLD)
                    vline(p, r, fw - 1, GOLD)
        if style == 'streamer_end':
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    cut = int(5 * (1 - abs(i + 0.5 - fw / 2) / (fw / 2)))
                    for j in range(cut):
                        p.clear((fx + i, fy + fh - 1 - j))

    if style == 'upperarm':                                       # red silk sleeves
        silk(p, f, ROBE, ROBE_DEEP, fold=5)
    if style == 'armband':
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            p.lit((fx + fw // 2, fy + fh // 2), (255, 70, 50), glow=0.5)
    if style in ('pauldron', 'pauldron2', 'pauldron3'):           # a tier of the shoulder guard: gilt, rounded, scalloped, studded
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                t = (1.32 - 0.5 * j / max(1, fh - 1)) * LIGHTS[s]
                if s in ('top', 'bottom'):
                    t = LIGHTS[s] * (1.25 - 0.4 * abs(j / max(1, fh - 1) - 0.5))
                p.px(xy, noisy(p, k(PLATE, t), 3))
            if s in SIDES:
                hline(p, r, fh - 1, LACE)
                hline(p, r, fh - 2, GOLD_LIGHT)
                for i in range(2, fw - 1, 4):
                    p.px((fx + i, fy + 1), GOLD_LIGHT)
            if s == 'bottom':
                border(p, r, GOLD_LIGHT)
    if style == 'pauldron_crest':
        gold_box(p, f)
    if style == 'lionface':                                       # the gold lion's head biting the shoulder
        gold_box(p, f)
        fx, fy, fw, fh = f['right']                               # 10x10
        for i in range(fw):
            p.px((fx + i, fy + 1), GOLD_DEEP)
            if i % 2 == 0:
                p.px((fx + i, fy), GOLD_LIGHT)                    # its mane
        for i in (2, 3, fw - 4, fw - 3):
            p.lit((fx + i, fy + 3), EYE, glow=0.8)
            p.px((fx + i, fy + 4), GOLD_DEEP)
        p.px((fx + fw // 2, fy + 4), GOLD_LIGHT)
        p.px((fx + fw // 2 - 1, fy + 5), GOLD_DEEP)
        for i in range(1, fw - 1):
            p.px((fx + i, fy + 7), (60, 20, 10))
            p.px((fx + i, fy + 8), IVORY if i % 2 else (60, 20, 10))
    if style == 'lionjaw':
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(0, fw, 2):
                p.px((fx + i, fy), IVORY)
    if style == 'forearm':
        silk(p, f, ROBE, ROBE_DEEP, fold=5)
    if style == 'bracer':                                         # a gilt bracer, banded, a lion's face
        plate(p, f, PLATE, studs=False)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(3, fh - 3, 3):
                for i in range(1, fw - 1):
                    if (i + j) % 4 == 0:
                        p.px((fx + i, fy + j), GOLD_LIGHT)
            for j in range(2, fh - 2):
                p.px((fx + fw // 2, fy + j), LACE)
    if style == 'fist':
        hand(p, f, FACE, nails=(80, 40, 30))

    # the axes
    if style == 'haft':                                           # red lacquer, banded gold, a cord grip
        p.shaded(f, (150, 28, 24))
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 10):
                hline(p, f[s], j, GOLD)
                hline(p, f[s], j + 1, GOLD_DEEP)
            for j in range(fh - 30, fh - 14):                     # the grip, wound in dark cord
                hline(p, f[s], j, (40, 20, 20) if j % 2 else (90, 40, 30))
            vline(p, f[s], 0, (100, 14, 12))
    if style in ('axe_collar', 'axe_butt'):
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            hline(p, f[s], fh // 2, GOLD_DEEP)
    if style == 'axe_point':
        steel_box(p, f)
        for s in SIDES:
            point(p, f[s], keep=0.5, down=False, sharp=0.4)
    if style == 'axe_tassel':                                     # a red horsehair tassel under the head
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                p.px(xy, noisy(p, k((200, 30, 28), (1.1 - 0.35 * (i % 2)) * LIGHTS[s]), 4))
            if s in SIDES:
                for i in range(fw):
                    cut = p.rnd.randint(0, 2)
                    for j in range(cut):
                        p.clear((fx + i, fy + fh - 1 - j))
        hline(p, f['front'], 0, GOLD)
    if style == 'axe_socket':
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(1, fh, 3):
                hline(p, f[s], j, GOLD_DEEP)
    if style == 'axe_spike':
        steel_box(p, f)
        for s in ('front', 'back', 'top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in each(f[s]):
                half = (fh / 2) * (1 - i / fw) + 0.3 if s in ('front', 'back') else fh
                if s in ('front', 'back') and abs(j + 0.5 - fh / 2) > half:
                    p.clear(xy)
    if style == 'axe_blade':                                      # the great crescent blade: steel, its edge bright, etched with gold flowers
        def mask(s, t):                                           # s 0 at its edge (outward) .. 1 at the haft; t 0 top .. 1 bottom
            u = (t - 0.47) * 2
            reach = 0.14 * u * u                                  # the edge bowed outward
            half = 0.5 - 0.3 * s ** 1.3                           # narrowing to the socket
            if s < reach or abs(t - 0.47) > half:
                return False
            if s > 0.82 and abs(t - 0.47) > 0.16:
                return False
            return True

        def colour(s, t):
            u = (t - 0.47) * 2
            reach = 0.14 * u * u
            e = s - reach
            if e < 0.07:
                return (250, 252, 255)                            # the honed edge
            if e < 0.13:
                return (200, 206, 220)
            return mixc((176, 184, 200), (110, 118, 136), min(1, (s - 0.13) * 1.1))

        for s_ in ('front', 'back'):
            fx, fy, fw, fh = f[s_]
            cutout(p, f[s_], mask, colour, flip=s_ == 'back')
            # the gold flowers (宣花) etched on it: a rosette and scrolling stems, a gold rim inside its edge
            cx_, cy_ = fx + int(fw * 0.5), fy + int(fh * 0.47)
            for a in range(0, 360, 45):
                for rr in (1, 2, 3):
                    x = cx_ + round(rr * math.cos(math.radians(a + rr * 10)))
                    y = cy_ + round(rr * math.sin(math.radians(a + rr * 10)))
                    if p.img.getpixel((x, y))[3]:
                        p.px((x, y), GOLD if rr < 3 else GOLD_DEEP)
            p.px((cx_, cy_), (220, 40, 30))
            for t in range(fh):
                for s in (0.38, 0.66):
                    x = fx + int(fw * (s + 0.06 * math.sin(t * 0.6)))
                    if p.img.getpixel((x, fy + t))[3] and abs(t / fh - 0.47) > 0.15:
                        p.px((x, fy + t), GOLD_DEEP)
            for i, j, xy in each(f[s_]):                          # a gold line following the edge
                s, t = (i + 0.5) / fw, (j + 0.5) / fh
                u = (t - 0.47) * 2
                if abs(s - 0.14 * u * u - 0.2) < 0.035 and p.img.getpixel(xy)[3]:
                    p.px(xy, GOLD)
        bare(p, f)

    # the head: a fierce, dark-ruddy face
    if style == 'face':
        hair_box(p, f, BLUE_BLACK)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for j in range(3, fh):
                for i in range(8):
                    x = fx + (fw - 1 - i if s == 'right' else i)
                    p.px((x, fy + j), shade_of(BLUE_BLACK, 1.2) if j > fh * 0.55 and i > 4 else k(FACE, 0.9 - 0.03 * i))
        fx, fy, fw, fh = f['front']                               # 22x20
        for i, j, xy in each(f['front']):
            across = abs((i + 0.5) / fw - 0.5) * 2
            p.px(xy, noisy(p, k(FACE, 1.06 - 0.18 * across ** 2 - 0.1 * j / fh), 2))
        for i in range(fw):                                       # his hair under the helmet's band
            p.px((fx + i, fy), shade_of(BLUE_BLACK, 1.0))
        for j in range(2, 8):                                     # the scowl: deep furrows over the nose
            for x in (fw // 2 - 2, fw // 2 + 1):
                p.px((fx + x, fy + j), k(FACE, 0.7))
        for j in range(3, 6):
            for i in range(3, fw - 3):
                if j == 4 and (i < fw // 2 - 3 or i > fw // 2 + 2):
                    p.px((fx + i, fy + j), k(FACE, 0.8))
        for side in (0, 1):                                       # the sockets round the bulging eyes, dark
            ex = 2 if side == 0 else fw - 2 - 7
            for i in range(7):
                p.px((fx + ex + i, fy + 7), k(FACE, 0.55))
                p.px((fx + ex + i, fy + 11), k(FACE, 0.7))
        for j in range(12, 17):                                   # deep lines from the nose to the mouth
            p.px((fx + 4 - (j - 12) // 2, fy + j), k(FACE, 0.66))
            p.px((fx + fw - 5 + (j - 12) // 2, fy + j), k(FACE, 0.66))
        roar(p, (fx + 3, fy + fh - 3, fw - 6, 3))
        p.face(f['bottom'], k(FACE, 0.7), 1.0)
    if style == 'nose':                                           # a broad lion's nose, flared
        skin_box(p, f, FACE)
        fx, fy, fw, fh = f['front']
        for i in (0, 1, fw - 2, fw - 1):
            p.px((fx + i, fy + fh - 1), (50, 14, 12))
        hline(p, f['front'], 0, k(FACE, 1.15), fw // 2 - 1, fw // 2 + 1)
        p.face(f['bottom'], (50, 14, 12), 1.0)
    if style == 'cheeks':                                         # his cheekbones, swelling
        skin_box(p, f, FACE, 1.05)
    if style == 'eye':                                            # a bulging eye: white, a burning gold iris, a dark pupil
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                p.lit(xy, (250, 244, 226), glow=0.4)
        fx, fy, fw, fh = f['front']                               # 5x4
        for i, j, xy in each((fx + 1, fy, 3, fh)):
            p.lit(xy, EYE if j in (1, 2) else (255, 140, 40), glow=1.0)
        p.px((fx + 2, fy + 1), (14, 6, 6))
        p.px((fx + 2, fy + 2), (14, 6, 6))
        p.glow.putpixel((fx + 2, fy + 1), (0, 0, 0, 0))
        p.glow.putpixel((fx + 2, fy + 2), (0, 0, 0, 0))
        hline(p, f['front'], 0, (30, 10, 8))
    if style == 'brow':
        brow_box(p, f, BLUE_BLACK)
    if style == 'ear':
        skin_box(p, f, FACE)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            vline(p, f[s], fw // 2, k(FACE, 0.66))
            p.px((fx + fw // 2, fy + fh - 1), GOLD)               # a gold ring through the lobe
    if style == 'jaw':
        skin_box(p, f, FACE)
        for s in ('front', 'right', 'left', 'bottom'):
            for i, j, xy in each(f[s]):
                if s != 'front' or j >= 2 or i < 4 or i > f[s][2] - 5:
                    p.px(xy, shade_of(BLUE_BLACK, 0.8 + ((i + 2 * j) % 3) * 0.45))
        fx, fy, fw, fh = f['front']                               # the lower teeth, the gape's foot
        for i in range(5, fw - 5):
            p.px((fx + i, fy), IVORY if i % 2 else (60, 10, 12))
            p.px((fx + i, fy + 1), (60, 10, 12))
    if style in ('moustache',):
        brow_box(p, f, BLUE_BLACK)
    if style in ('whisker', 'beard_s', 'beard_o'):
        lock(p, f, keep=0.45, taper=0.3, tone=BLUE_BLACK, grey=0.03)
    if style in ('beard_c', 'beard_tip'):
        lock(p, f, keep=0.6 if style == 'beard_c' else 0.4, taper=0.3 if style == 'beard_c' else 0.45, tone=BLUE_BLACK, grey=0.03)

    # the helmet
    if style in ('helm', 'helm_top'):                             # a gilt bowl, ribbed, rivets in rows
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                across = abs((i + 0.5) / fw - 0.5) * 2
                t = (1.3 - 0.4 * across ** 2 - 0.2 * j / max(1, fh - 1)) * LIGHTS[s]
                p.px(xy, noisy(p, k(PLATE, t), 2))
            if s in SIDES:
                for i in range(1, fw, 4):
                    vline(p, r, i, k(PLATE_DEEP, 1.1))
                    if fh > 4:
                        p.px((fx + i + 1, fy + fh - 2), GOLD_LIGHT)
    if style == 'helm_band':                                      # a band of gold and red, studded
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                if i % 4 == 2:
                    p.lit((fx + i, fy + fh // 2), (255, 70, 50), glow=0.45)
    if style == 'helm_spike':
        gold_box(p, f)
        for s in SIDES:
            point(p, f[s], keep=0.5, down=False, sharp=0.5)
    if style == 'plume':                                          # the red tassel
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                p.px(xy, noisy(p, k((214, 32, 30), (1.15 - 0.35 * (i % 2) - 0.2 * j / fh) * LIGHTS[s]), 4))
        hline(p, f['front'], f['front'][3] - 1, GOLD)
    if style == 'plume_hair':
        lock(p, f, keep=0.4, taper=0.3, tone=RED_HAIR, grey=0.0)
    if style == 'helm_neck':                                      # the lamellar neck guard
        for s, r in f.items():
            if s in ('top', 'bottom'):
                plate(p, {s: r}, PLATE)
            else:
                lamellar(p, r, PLATE, LACE, GOLD_LIGHT, light=LIGHTS[s], pw=3, ph=4)
    if style == 'helm_crest':                                     # the flame crest on his brow: gold, a burning jewel in it
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            def mask(s_, t):
                u = abs(s_ - 0.5) * 2
                lobe = 0.25 + 0.75 * t ** 0.7
                return u < lobe * (0.85 + 0.15 * math.cos(t * 12)) and not (t < 0.25 and 0.2 < u)
            cutout(p, f[s], mask, lambda s_, t: mixc(GOLD_LIGHT, GOLD_DEEP, t * 0.8))
            if s == 'front':
                cx_, cy_ = fx + fw / 2, fy + fh * 0.62
                for i, j, xy in each(f[s]):
                    d = math.hypot(i + 0.5 - fw / 2, j + 0.5 - fh * 0.62)
                    if d < 2.4:
                        p.lit(xy, mixc((255, 220, 160), (230, 40, 30), d / 2.4), glow=0.95)
                    elif d < 3.2 and p.img.getpixel(xy)[3]:
                        p.px(xy, GOLD_DEEP)
        bare(p, f)
    if style == 'helm_wing':                                      # a phoenix wing, swept up, its feathers in gold
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            flip = s == 'back'
            def mask(s_, t):                                      # s_ 0 at its outer tip .. 1 at the helmet
                lo = 0.95 - 0.35 * (1 - s_) ** 0.6                # its foot curving up toward the tip
                hi = 0.55 * (1 - s_) ** 1.6                       # its top sweeping up to the tip
                feather = 0.06 * abs(math.sin(s_ * 14))
                return hi - 0.02 < t < lo - feather
            def colour(s_, t):
                c = mixc(GOLD_LIGHT, PLATE, s_ * 0.7)
                if int(s_ * 14) % 2 == 0:
                    c = k(c, 0.82)
                return c
            cutout(p, f[s], mask, colour, flip=flip)
        bare(p, f)
