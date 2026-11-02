"""Black Wind Mountain's other creatures: the Boar Demon (野猪精), the Black-Wind Imp (黑风小妖), and the Black
Bear's two friends, Lingxuzi (凌虚子, the grey-wolf Daoist) and the White-Robed Scholar (白衣秀士, the
white-flower snake). As black_bear.py (wild_paint): each one's model is written into BlackWindRenderers
between its GENERATED markers, its skin painted to the same box-UV layout at twice the model's
resolution, its eyes glowing (<name>_glow.png); a captain of the boars or the imps in violet
(<name>_captain.png).

The Boar Demon: a wild boar's head on a soldier's hunched body, a man and a half tall, on a boar's legs
and cloven hooves bound in cloth: bristling black-brown hide, a crest of bristles down his nape and
spine, a long snout with its pink-grey disc, yellow tusks curling up, little red eyes; a vest of bronze
scales over leather, a studded kilt, a rope belt hung with a skull and a pouch, leather bracers; a heavy
bronze broadaxe, its haft wrapped in red.

The Black-Wind Imp: a child's height of soot-grey hide mottled blue, a great head with fan ears veined
like a bat's wing, two horn nubs, a hooked nose, a lipless grin of needle teeth, big yellow eyes; a pot
belly, long thin arms with three long claws, short bowed legs; a ragged black scarf about its neck
streaming out behind in three tails, a little gourd of wind at its hip.

Lingxuzi: an old grey wolf as a Daoist immortal: a long grey muzzle, white brows, a long grey beard in
locks, his hair in a topknot under a jade lotus crown with its pin; a blue-grey Daoist's robe crossed at
the breast with a black collar, a dark sash with its tassels, a gourd of elixirs and a jade pendant at
his belt, great hanging sleeves, a short black-edged mantle; cloud-toed shoes under the hem; his bushy
tail; a straight sword in his right paw, a horsetail whisk in his left.

The White-Robed Scholar: slender, in long white robes to the ground bordered in black, wide sleeves, a
black sash and a jade pendant; a black scholar's cap with its two long wings; a pale narrow face with a
serpent's green slit eyes, faint scales at the cheeks, a forked tongue; scaled white hands, a painted
folding fan; and from under his hem behind, the long white coils of his own tail, spotted with dark
flowers.

Run from the repository root: python assets_source/black_wind_folk.py"""
import math

import beastgen as bg
import wild_paint as w
from wild_paint import part, mirrored, aim, paint, Painter, k, mix

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/BlackWindRenderers.java'
BOAR_SIZE, IMP_SIZE, WOLF_SIZE, SCHOLAR_SIZE = 0.85, 0.85, 0.9, 0.92

GOLD = (228, 178, 62)
GOLD_DEEP = (142, 96, 30)
BRONZE = (176, 120, 56)
BRONZE_DEEP = (100, 64, 28)
IVORY = (238, 228, 200)
STEEL = (200, 206, 216)
LEATHER = (96, 60, 38)
JADE = (96, 182, 136)
JADE_DEEP = (52, 116, 86)
TRIM = {False: ((168, 30, 28), (90, 14, 18)), True: ((150, 72, 206), (78, 30, 116))}


def jade(p, f):
    w.flat(p, f, JADE)
    for s in f:
        fx, fy, fw, fh = f[s]
        for i, j, xy in w.each(p, f[s]):
            if w.vnoise(fx + i, fy + j, 2.0, 4) > 0.6:
                p.px(xy, k(JADE, 1.2))
            elif (i + j) % 5 == 0:
                p.px(xy, JADE_DEEP)


# ══ The Boar Demon ═══════════════════════════════════════════════════════════

HIDE = (66, 46, 34)
HIDE_DARK = (30, 20, 16)
BRISTLE = (40, 30, 26)
BRISTLE_TIP = (150, 128, 100)
SNOUT = (168, 132, 120)
TUSK = (226, 204, 150)
SCALE = (150, 104, 52)
WRAP = (150, 136, 110)

BBODY = (0.28, 0, 0)
BARM, BFORE, BHAND = (-0.3, 0, 0.18), (-0.8, 0, 0), (0, 0, 0)

BOAR = mirrored([
    part('right_leg', 'root', (-4.0, 4, 0.5), (-0.5, 0, 0.06), [(-2.9, -1, -3, 5.8, 9, 6, 'boar_thigh')]),
    part('right_shin', 'right_leg', (0, 8, 0.2), (1.1, 0, 0), [
        (-2.1, 0, -2.2, 4.2, 7.4, 4.4, 'boar_shin'),
        (-2.5, 1.0, -2.6, 5, 4.6, 5.2, 'wrap')]),
    part('right_ankle', 'right_shin', (0, 7.0, 0), (-0.85, 0, 0), [(-1.6, 0, -1.7, 3.2, 5, 3.4, 'boar_ankle')]),
    part('right_hoof', 'right_ankle', (0, 4.8, 0), (0.25, 0, 0), [
        (-1.9, 0, -2.6, 1.8, 2.4, 3.6, 'hoof'), (0.1, 0, -2.6, 1.8, 2.4, 3.6, 'hoof')]),
    part('body', 'root', (0, 4, 0.5), BBODY, [
        (-5.6, -3, -4.2, 11.2, 4.4, 8.4, 'kilt_top'),
        (-6.2, -10, -5.2, 12.4, 8, 10.4, 'boar_belly'),
        (-6.8, -19, -4.8, 13.6, 10, 9.6, 'boar_chest'),
        (-7.2, -19.4, -5.4, 14.4, 9.6, 10.8, 'vest'),
        (-6.6, -3.6, -5.4, 13.2, 1.6, 10.8, 'rope'),
        (-7.4, -22, -3.4, 14.8, 4.6, 8.4, 'boar_hump'),
        (-1.6, -26.4, -1.0, 3.2, 5.2, 7.4, 'bristles'),
    ]),
    part('crest_1', 'body', (0, -19, 4.8), (0.3, 0, 0), [(-1.4, -4.8, -1.2, 2.8, 4.8, 2.4, 'bristles')]),
    part('crest_2', 'body', (0, -14, 5.4), (0.5, 0, 0), [(-1.3, -4.2, -1.2, 2.6, 4.2, 2.4, 'bristles')]),
    part('crest_3', 'body', (0, -9, 5.4), (0.7, 0, 0), [(-1, -2.8, -1, 2, 2.8, 2, 'bristles')]),
    part('kilt_front', 'body', (0, -2, -4.6), (-0.08, 0, 0), [(-5, 0, -0.6, 10, 8, 1.2, 'kilt')]),
    part('kilt_back', 'body', (0, -2, 4.4), (0.14, 0, 0), [(-5.2, 0, -0.6, 10.4, 7.5, 1.2, 'kilt')]),
    part('right_kilt', 'body', (-5.6, -2, 0), (0, 0, 0.14), [(-0.6, 0, -3.8, 1.2, 7, 7.6, 'kilt_side')]),
    part('skull_charm', 'body', (-4.4, -3, -5.6), (0, 0, 0.05), [(-1.4, 0.4, -1.2, 2.8, 3, 2.4, 'charm_skull'), (-0.3, -0.6, -0.3, 0.6, 1.2, 0.6, 'cord')]),
    part('pouch', 'body', (4.6, -3, -4.8), (0, 0, -0.05), [(-1.8, 0, -1.2, 3.6, 3.6, 2.4, 'pouch')]),
    part('tail', 'body', (0, -4, 4.6), (-0.3, 0, 0), [(-0.5, 0, -0.5, 1, 5, 1, 'boar_tail'), (-0.9, 4.6, -0.9, 1.8, 1.6, 1.8, 'boar_tuft')]),
    part('right_arm', 'body', (-8.3, -17.4, 0), BARM, [
        (-2.6, -1.4, -2.6, 5.2, 8, 5.2, 'boar_arm'),
        (-3.6, -2.6, -3.4, 5.6, 3.8, 6.8, 'boar_pauldron')]),
    part('right_forearm', 'right_arm', (0, 6.6, 0), BFORE, [
        (-2.3, 0, -2.3, 4.6, 7.4, 4.6, 'boar_forearm'),
        (-2.7, 1.2, -2.7, 5.4, 5, 5.4, 'boar_bracer')]),
    part('right_hand', 'right_forearm', (0, 7.2, 0), BHAND, [(-2.2, 0, -2.4, 4.4, 3.6, 4.8, 'boar_hand')]),
    part('head', 'body', (0, -21.4, -3.6), (0.0, 0, 0), [
        (-4.6, -6.4, -5, 9.2, 8, 8.4, 'boar_skull'),
        (-3.8, -5.4, -8.2, 7.6, 6.6, 3.6, 'boar_snout'),
        (-3, -4.6, -11.6, 6, 5.6, 7, 'boar_snout'),
        (-2.9, -4.4, -12.4, 5.8, 4.6, 1, 'snout_disc'),
        (-5, -6.8, -3, 10, 2, 7, 'boar_brow'),
        (-5.2, -2, -3.6, 10.4, 4.6, 7, 'boar_jowl'),
    ]),
    part('jaw', 'head', (0, 0.8, -4.2), (0.05, 0, 0), [(-2.6, 0, -7, 5.2, 2.2, 7, 'boar_jaw')]),
    part('right_tusk', 'head', (-3.0, -1.4, -10.4), (-0.25, 0, 0.45), [(-0.8, -3.6, -0.8, 1.6, 4, 1.6, 'tusk')]),
    part('right_tusk_tip', 'right_tusk', (0, -3.4, 0), (-0.7, 0, -0.35), [(-0.6, -3.2, -0.6, 1.2, 3.4, 1.2, 'tusk_tip')]),
    part('right_ear', 'head', (-4.4, -6, -1), (0.3, 0.4, -0.7), [(-1.6, -4, -0.6, 3.2, 4.2, 1.2, 'boar_ear')]),
    part('right_cheek_bristle', 'head', (-5.0, -2.0, -2.6), (0, 0.3, 0.35), [(-1.4, -2, -2.4, 1.6, 5.6, 5, 'bristles')]),
    part('right_eye', 'head', (-3.1, -4.4, -5.0), (0, 0.3, 0), [(-0.8, -0.5, -0.4, 1.6, 1.1, 0.6, 'boar_eye')]),
])
BOAR += [
    part('axe', 'right_hand', (0, 1.8, 0), aim([BBODY, BARM, BFORE, BHAND], (0.35, 0, 0)), [
        (-0.6, -20, -0.6, 1.2, 28, 1.2, 'axe_haft'),
        (-0.8, 2, -0.8, 1.6, 4, 1.6, 'axe_wrap'),
        (-0.45, -25.5, -8.6, 0.9, 11, 8.6, 'axe_blade'),
        (-0.9, -21, -1.6, 1.8, 3.6, 3.2, 'axe_socket'),
        (-0.4, -22, 0.6, 0.8, 1.6, 2.4, 'axe_spike'),
        (-0.8, 7.8, -0.8, 1.6, 1.2, 1.6, 'axe_butt')]),
]
BOAR = w.settle(BOAR, ('right_hoof', 'left_hoof'))


def paint_boar(trim):
    trim_c, trim_d = trim

    def hide(p, f, base=HIDE, density=0.55):
        for s in f:
            b = base if s != 'bottom' else HIDE_DARK
            w.strands(p, f[s], k(b, w.LIGHT[s]), k(HIDE_DARK, w.LIGHT[s]), k(BRISTLE_TIP, w.LIGHT[s]), density=density, lean=0.1)

    def box(p, style, f, dims):
        S = w.SIDES
        if style in ('boar_thigh', 'boar_shin', 'boar_ankle', 'boar_arm', 'boar_forearm', 'boar_belly', 'boar_chest', 'boar_hand', 'boar_skull',
                     'boar_jowl', 'boar_hump', 'boar_jaw', 'boar_tail'):
            hide(p, f, base=mix(HIDE, (90, 70, 56), 0.4) if style == 'boar_belly' else HIDE)
            if style == 'boar_jowl':
                for s in S:
                    w.tufts(p, f[s], depth=3, every=2)
            if style == 'boar_skull':
                fx, fy, fw, fh = f['front']
                for i, j, xy in w.each(p, f['front']):
                    if j > fh * 0.5:
                        p.px(xy, mix(p.get(xy), (110, 88, 70), 0.35))
        if style in ('bristles', 'boar_tuft', 'boar_brow'):
            for s in f:
                w.strands(p, f[s], k(BRISTLE, w.LIGHT[s]), (10, 8, 6), k(BRISTLE_TIP, w.LIGHT[s]), density=0.7, down=style == 'boar_brow')
            for s in S:
                w.tufts(p, f[s], depth=3, every=2, down=False)
        if style == 'wrap':                                       # cloth bindings round the shins, crossed
            w.cloth(p, f, WRAP, k(WRAP, 0.6), fold=3)
            for s in S:
                fx, fy, fw, fh = f[s]
                for j in range(0, fh, 3):
                    w.hline(p, f[s], j, k(WRAP, 0.55 * w.LIGHT[s]))
        if style == 'hoof':
            for s in f:
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    p.px(xy, k(mix((30, 24, 22), (70, 60, 52), j / max(1, fh) * 0.5), w.LIGHT[s]))
        if style == 'vest':                                       # bronze scales over leather, laced at the sides
            for s in S:
                w.scale_tiles(p, f[s], k(SCALE, w.LIGHT[s]), k(BRONZE_DEEP, w.LIGHT[s] * 0.7), cw=3, ch=3)
            w.leather(p, f, LEATHER, faces=('top', 'bottom'))
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for j in range(1, fh, 2):
                    p.px((fx + fw // 2, fy + j), trim_c)
            w.border(p, f, LEATHER, bottom=True)
        if style == 'boar_pauldron':
            for s in S + ('top',):
                w.scale_tiles(p, f[s], k(SCALE, w.LIGHT[s]), k(BRONZE_DEEP, w.LIGHT[s] * 0.7), cw=3, ch=2)
            w.border(p, f, trim_c, top=False)
        if style in ('kilt', 'kilt_side', 'kilt_top'):            # a leather kilt, studded, trimmed in red
            w.leather(p, f, LEATHER, stitch=(150, 110, 70))
            for s in S:
                fx, fy, fw, fh = f[s]
                for i in range(1, fw - 1, 3):
                    for j in range(2, fh - 1, 3):
                        w.rivet(p, fx + i, fy + j, BRONZE)
                w.hline(p, f[s], fh - 1, trim_d)
                w.hline(p, f[s], fh - 2, trim_c)
            if style != 'kilt_top':
                for s in ('front', 'back'):
                    w.ragged(p, f[s], teeth=2, depth=2)
        if style == 'rope':
            w.rope(p, f, (170, 140, 90))
        if style == 'charm_skull':                                # a little skull at his belt
            w.flat(p, f, IVORY)
            fx, fy, fw, fh = f['front']
            p.px((fx + 1, fy + 2), (30, 20, 16))
            p.px((fx + fw - 2, fy + 2), (30, 20, 16))
            w.hline(p, f['front'], fh - 1, (90, 80, 60))
        if style == 'cord':
            w.flat(p, f, (60, 40, 26))
        if style == 'pouch':
            w.leather(p, f, (110, 76, 46), stitch=(170, 130, 90))
        if style == 'boar_bracer':
            w.leather(p, f, LEATHER, stitch=(170, 130, 90))
            for s in S:
                w.hline(p, f[s], 0, BRONZE)
                w.hline(p, f[s], f[s][3] - 1, BRONZE)
        if style == 'boar_snout':                                 # the long snout, its hide thinning toward the disc
            for s in f:
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    t = (fw - 1 - i) / max(1, fw - 1) if s == 'right' else i / max(1, fw - 1) if s == 'left' else 0.5
                    c = mix(HIDE, (120, 96, 84), 0.3 + 0.5 * t)
                    p.px(xy, [v * w.LIGHT[s] + p.rnd.randint(-5, 5) for v in c])
            for s in ('right', 'left'):
                w.hline(p, f[s], f[s][3] - 1, (40, 22, 20))
        if style == 'snout_disc':                                 # the disc, two deep nostrils
            w.flat(p, f, SNOUT)
            fx, fy, fw, fh = f['front']
            for nx in (fw // 3 - 1, fw - fw // 3):
                for j in range(fh // 3, fh - fh // 3):
                    p.px((fx + nx, fy + j), (40, 20, 22))
                    p.px((fx + nx + 1, fy + j), (60, 30, 30))
            w.border(p, {'front': f['front']}, k(SNOUT, 0.75), sides=('front',))
        if style in ('tusk', 'tusk_tip'):
            for s in f:
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    p.px(xy, k(mix(TUSK, (160, 130, 80), j / max(1, fh) * 0.6), w.LIGHT[s]))
            if style == 'tusk_tip':
                for s in S:
                    w.point(p, f[s], down=False, keep=0.25, sharp=0.5)
        if style == 'boar_ear':
            hide(p, f)
            fx, fy, fw, fh = f['front']
            for i, j, xy in w.each(p, f['front']):
                if 0 < i < fw - 1 and j > 1:
                    p.px(xy, (130, 90, 80))
            for s in S:
                w.point(p, f[s], down=False, keep=0.5, sharp=0.5)
                w.tufts(p, f[s], depth=1, every=2, down=False)
        if style == 'boar_eye':
            hide(p, f)
            fx, fy, fw, fh = f['front']
            for i, j, xy in w.each(p, f['front']):
                p.lit(xy, (255, 60, 40) if j == fh - 1 or i == fw // 2 else (200, 30, 20))
            p.px((fx + fw // 2, fy), (20, 6, 6))
        if style in ('axe_haft',):
            w.wood(p, f, (90, 62, 40), along='y')
        if style == 'axe_wrap':
            w.cloth(p, f, trim_c, trim_d, fold=2)
        if style in ('axe_socket', 'axe_butt', 'axe_spike'):
            w.iron(p, f, BRONZE, rivets=0, dents=0.1)
            if style == 'axe_spike':
                for s in ('right', 'left'):
                    w.point(p, f[s], down=False, keep=0.2, sharp=0.6)
        if style == 'axe_blade':                                  # the broad bronze blade: a crescent edge, a beast's eye
            w.flat(p, f, BRONZE)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    u = i / max(1, fw - 1)
                    if s == 'right':
                        u = 1 - u                                 # 0 at the haft .. 1 at the edge
                    v = j / max(1, fh - 1)
                    edge = 0.55 + 0.45 * math.sin(math.pi * v) ** 0.6
                    neck = 0.3 * (1 - math.sin(math.pi * v)) * (u > 0.3)
                    if u > edge or v < neck * 0.5 or v > 1 - neck * 0.5:
                        p.clear(xy)
                        continue
                    c = (240, 220, 160) if u > edge - 0.14 else k(BRONZE, 0.8 + 0.35 * w.vnoise(i, j, 3, 2))
                    p.px(xy, [x * w.LIGHT[s] + p.rnd.randint(-4, 4) for x in c])
                w.disc(p, fx + (fw * 0.3 if s == 'left' else fw * 0.7), fy + fh / 2, 1.2, BRONZE_DEEP)
    return box


# ══ The Black-Wind Imp ═══════════════════════════════════════════════════════

IMP_SKIN = (70, 72, 82)
IMP_SHADE = (34, 34, 42)
IMP_BLUE = (60, 78, 100)
IMP_EYE = (255, 222, 80)
SCARF = (20, 18, 22)

IARM, IFORE = (-0.35, 0, 0.35), (-0.5, 0, 0)

IMP = mirrored([
    part('right_leg', 'root', (-2.2, 12.5, 0.5), (-0.3, 0, 0.18), [(-1.4, -0.5, -1.5, 2.8, 5, 3, 'imp_thigh')]),
    part('right_shin', 'right_leg', (0, 4.4, 0), (0.55, 0, -0.1), [(-1.1, 0, -1.1, 2.2, 4.6, 2.2, 'imp_shin')]),
    part('right_foot', 'right_shin', (0, 4.4, 0), (-0.25, 0, 0), [
        (-1.4, 0, -3, 2.8, 1.4, 4, 'imp_foot'),
        (-1.4, 0.4, -4.4, 0.8, 1, 1.6, 'imp_toe'), (0.6, 0.4, -4.4, 0.8, 1, 1.6, 'imp_toe')]),
    part('body', 'root', (0, 12.5, 0.5), (0.35, 0, 0), [
        (-3.2, -4, -2.8, 6.4, 4.8, 5.6, 'imp_belly'),
        (-3.6, -8.6, -2.6, 7.2, 5, 5, 'imp_chest'),
        (-1.4, -9.8, 1.0, 2.8, 1.6, 2.4, 'imp_spine'),
        (-3.8, -1.2, -3, 7.6, 1.8, 6, 'imp_loin'),
    ]),
    part('loincloth', 'body', (0, 0.4, -2.6), (-0.1, 0, 0), [(-2.2, 0, -0.3, 4.4, 4.5, 0.6, 'imp_rag')]),
    part('gourd', 'body', (3.6, -1, -1), (0, 0, -0.2), [(-1.2, 0, -1.2, 2.4, 2.6, 2.4, 'gourd'), (-0.8, -1.4, -0.8, 1.6, 1.6, 1.6, 'gourd_top')]),
    part('scarf', 'body', (0, -8.8, 0), (0, 0, 0), [(-3.4, -1.2, -3, 6.8, 2.4, 6, 'imp_scarf')]),
    part('scarf_tail_0', 'scarf', (-1.6, 0, 2.8), (1.2, 0.2, 0), [(-1, 0, -0.2, 2, 7, 0.4, 'imp_scarf_tail')]),
    part('scarf_tail_1', 'scarf', (0.4, 0.4, 2.9), (1.35, -0.1, 0), [(-1.1, 0, -0.2, 2.2, 9, 0.4, 'imp_scarf_tail_long')]),
    part('scarf_tail_2', 'scarf', (2.0, 0, 2.8), (1.1, -0.3, 0), [(-0.9, 0, -0.2, 1.8, 6, 0.4, 'imp_scarf_tail')]),
    part('right_arm', 'body', (-4.2, -7.8, 0), IARM, [(-1.0, -0.6, -1.0, 2, 6, 2, 'imp_arm')]),
    part('right_forearm', 'right_arm', (0, 5.6, 0), IFORE, [(-0.9, 0, -0.9, 1.8, 6, 1.8, 'imp_forearm')]),
    part('right_hand', 'right_forearm', (0, 5.8, 0), (0, 0, 0), [
        (-1.2, 0, -1.2, 2.4, 1.6, 2.4, 'imp_hand'),
        (-1.2, 1.4, -1.2, 0.6, 3, 0.6, 'imp_claw'), (-0.3, 1.4, -1.3, 0.6, 3.4, 0.6, 'imp_claw'), (0.6, 1.4, -1.2, 0.6, 3, 0.6, 'imp_claw')]),
    part('head', 'body', (0, -9.6, -1.4), (-0.3, 0, 0), [
        (-4.4, -8, -4.4, 8.8, 8, 8, 'imp_skull'),
        (-1, -4.6, -5.6, 2, 2.6, 1.4, 'imp_nose'),
        (-3.8, -1.2, -4.8, 7.6, 1.6, 1, 'imp_grin'),
    ]),
    part('jaw', 'head', (0, 0, -1.8), (0.08, 0, 0), [(-3.4, 0, -3, 6.8, 1.8, 4.6, 'imp_jaw')]),
    part('right_horn', 'head', (-2.2, -8, -1.6), (-0.2, 0, -0.35), [(-0.6, -2, -0.6, 1.2, 2.2, 1.2, 'imp_horn')]),
    part('right_ear', 'head', (-4.2, -5.6, 0), (0, -0.45, 0.3), [(-10, -5.5, -0.3, 10, 9.5, 0.6, 'imp_ear')]),
    part('right_eye', 'head', (-2.2, -4.8, -4.5), (0, 0.15, 0), [(-1.3, -0.9, -0.3, 2.6, 2, 0.5, 'imp_eye')]),
])
IMP = w.settle(IMP, ('right_foot', 'left_foot'))


def paint_imp(trim):
    trim_c, trim_d = trim

    def skin(p, f, mottle=0.5):
        w.hide(p, f, IMP_SKIN, IMP_SHADE, light=mix(IMP_SKIN, IMP_BLUE, 0.6), mottle=mottle, warts=0.15, seed=5, belly=k(IMP_SKIN, 1.1))

    def box(p, style, f, dims):
        S = w.SIDES
        if style in ('imp_thigh', 'imp_shin', 'imp_foot', 'imp_belly', 'imp_chest', 'imp_arm', 'imp_forearm', 'imp_hand', 'imp_skull', 'imp_jaw', 'imp_spine'):
            skin(p, f)
            if style == 'imp_chest':                              # its ribs showing
                fx, fy, fw, fh = f['front']
                for j in range(1, fh - 1, 2):
                    w.hline(p, f['front'], j, k(IMP_SHADE, 1.0), 1, fw - 1)
            if style == 'imp_spine':
                for s in S:
                    w.point(p, f[s], down=False, keep=0.4, sharp=0.7)
            if style == 'imp_skull':                              # a heavy brow over the eyes, wrinkles
                fx, fy, fw, fh = f['front']
                for i in range(fw):
                    p.px((fx + i, fy + 5), k(IMP_SHADE, 0.8))
                    p.px((fx + i, fy + 6), k(IMP_SHADE, 1.1))
                for j in range(1, 4):
                    p.px((fx + fw // 2, fy + j), k(IMP_SHADE, 0.9))
        if style == 'imp_nose':
            skin(p, f, 0.2)
            fx, fy, fw, fh = f['bottom']
            p.px((fx, fy), (10, 8, 10))
            p.px((fx + fw - 1, fy), (10, 8, 10))
        if style == 'imp_grin':                                   # a lipless grin of needle teeth
            w.flat(p, f, (40, 10, 16))
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                if i % 2 == 0:
                    for j in range(fh):
                        p.px((fx + i, fy + j), IVORY)
            w.point(p, f['front'], down=True, keep=1.0, sharp=2)
        if style == 'imp_jaw':
            fx, fy, fw, fh = f['top']
            for i, j, xy in w.each(p, f['top']):
                p.px(xy, (90, 20, 30) if 0 < i < fw - 1 and j > 0 else IVORY if (i + j) % 2 == 0 else (40, 10, 16))
        if style == 'imp_claw' or style == 'imp_toe' or style == 'imp_horn':
            for s in f:
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    p.px(xy, k(mix((30, 28, 30), (170, 160, 150), j / max(1, fh) * 0.6 if style != 'imp_horn' else 0.3), w.LIGHT[s]))
            for s in S:
                w.point(p, f[s], down=style != 'imp_horn', keep=0.25, sharp=0.5)
        if style == 'imp_ear':                                    # fan ears, veined like a bat's wing, pale and thin within, torn
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    u = i / max(1, fw - 1) if s == 'back' else 1 - i / max(1, fw - 1)   # 0 at the head .. 1 at the tip
                    v = j / max(1, fh - 1)
                    if v < 0.5 - 0.5 * u * 0.9 or v > 0.5 + 0.5 * (1 - u * 0.4) or (u > 0.9 and abs(v - 0.4) > 0.2):
                        p.clear(xy)
                        continue
                    vein = abs((v - 0.45) - (u - 0.2) * 0.4) < 0.06 or abs((v - 0.6) + (u - 0.3) * 0.2) < 0.05
                    c = IMP_SHADE if vein else mix(mix((120, 118, 130), (196, 140, 150), 0.5 if s == 'front' else 0.15), IMP_SKIN, u * 0.4)
                    p.px(xy, [x * w.LIGHT[s] + p.rnd.randint(-4, 4) for x in c])
                w.tear_holes(p, f[s], n=1, size_=1)
            w.flat(p, f, IMP_SHADE, faces=('right', 'left', 'top', 'bottom'))
        if style == 'imp_eye':                                    # big yellow eyes, slit
            fx, fy, fw, fh = f['front']
            for i, j, xy in w.each(p, f['front']):
                p.lit(xy, mix(IMP_EYE, (255, 255, 200), 0.4 if j == 0 else 0))
            for j in range(fh):
                p.px((fx + fw // 2, fy + j), (14, 10, 6))
                p.glow.putpixel((fx + fw // 2, fy + j), (0, 0, 0, 0))
            w.flat(p, f, IMP_SHADE, faces=('right', 'left', 'top', 'bottom', 'back'))
        if style in ('imp_loin', 'imp_rag', 'imp_scarf', 'imp_scarf_tail', 'imp_scarf_tail_long'):
            w.cloth(p, f, SCARF, (6, 6, 8), fold=3)
            for s in S:                                           # a frayed hem, edged with its band's colour
                w.hline(p, f[s], 0, k(trim_c, w.LIGHT[s] * 0.8))
            if style in ('imp_scarf_tail', 'imp_scarf_tail_long', 'imp_rag'):
                for s in ('front', 'back'):
                    w.ragged(p, f[s], teeth=2, depth=3)
                    w.tear_holes(p, f[s], n=1, size_=1)
        if style in ('gourd', 'gourd_top'):                       # a little gourd of wind, a black cord round its neck
            w.flat(p, f, (150, 110, 50))
            for s in S:
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    p.px(xy, k(mix((176, 130, 60), (110, 74, 30), abs(i - fw / 2) / (fw / 2)), w.LIGHT[s]))
            if style == 'gourd_top':
                w.hline(p, f['front'], f['front'][3] - 1, (20, 16, 16))
    return box


# ══ Lingxuzi ═════════════════════════════════════════════════════════════════

GREY = (130, 130, 136)
GREY_DARK = (62, 62, 68)
GREY_LIGHT = (206, 204, 200)
ROBE = (92, 110, 128)
ROBE_DEEP = (46, 56, 70)
COLLAR = (22, 22, 28)
SASH = (34, 40, 66)
WOLF_EYE = (250, 210, 90)

WBODY = (0.06, 0, 0)
WARM, WFORE = (-0.15, 0, 0.12), (-0.95, -0.25, 0)
LWARM, LWFORE = (-0.3, 0, -0.18), (-1.25, 0.4, 0)

WOLF = mirrored([
    part('right_leg', 'root', (-2.6, 6, 0), (0, 0, 0), [(-2, 0, -2, 4, 13, 4, 'trouser')]),
    part('right_shoe', 'right_leg', (0, 13, 0), (0, 0, 0), [(-2.2, 0, -3.6, 4.4, 2.6, 5.8, 'shoe'), (-1.2, -0.8, -4.4, 2.4, 2.2, 1.2, 'shoe_cloud')]),
    part('body', 'root', (0, 6, 0), WBODY, [
        (-5, -15, -3.4, 10, 15, 6.8, 'robe_chest'),
        (-5.4, -6.4, -3.8, 10.8, 2.6, 7.6, 'dsash'),
        (-1.6, -7.2, -4.4, 3.2, 3.6, 0.8, 'sash_knot'),
        (-3.4, -16.4, -2.6, 6.8, 2.6, 5.4, 'wruff'),
    ]),
    part('collar_right', 'body', (-1.2, -15.2, -3.6), (0, 0, -0.55), [(-1.2, 0, -0.3, 2.4, 9.6, 0.6, 'collar')]),
    part('collar_left', 'body', (1.2, -15.2, -3.7), (0, 0, 0.55), [(-1.2, 0, -0.3, 2.4, 9.6, 0.6, 'collar')]),
    part('skirt', 'body', (0, -4, 0), (0, 0, 0), [(-5.6, 0, -3.8, 11.2, 8, 7.6, 'robe_skirt'), (-6.0, 7.6, -4.2, 12, 9.6, 8.4, 'robe_hem')]),
    part('skirt_front', 'skirt', (0, -0.4, -3.9), (-0.08, 0, 0), [(-2.8, 0, -0.4, 5.6, 17.4, 0.8, 'robe_panel')]),
    part('sash_tail', 'body', (2, -5.2, -4.3), (-0.06, 0, -0.05), [(-0.9, 0, -0.2, 1.8, 10, 0.4, 'sash_tail'), (-1, 10, -0.3, 2, 2.4, 0.6, 'tassel_red')]),
    part('belt_gourd', 'body', (-5.4, -5, -1), (0, 0, 0.1), [(-1.8, 0, -1.8, 3.6, 4.2, 3.6, 'elixir_gourd'), (-1.2, -2.6, -1.2, 2.4, 2.8, 2.4, 'elixir_gourd_top'),
                                                              (-0.5, -3.4, -0.5, 1, 1, 1, 'gourd_stopper')]),
    part('pendant', 'body', (3.6, -5, -4.2), (0, 0, 0), [(-0.25, 0, -0.25, 0.5, 2.6, 0.5, 'pcord'), (-1.2, 2.6, -0.4, 2.4, 2.4, 0.8, 'jade_disc')]),
    part('mantle', 'body', (0, -15.6, 0.2), (0.05, 0, 0), [(-6.8, -0.6, -3.9, 13.6, 4.4, 7.9, 'mantle')]),
    part('mantle_back', 'body', (0, -12.6, 3.6), (0.1, 0, 0), [(-6.6, 0, -0.4, 13.2, 9, 0.8, 'mantle_back')]),
    part('tail', 'body', (0, -3.4, 3.6), (-0.9, 0, 0), [(-1.5, -1.5, 0, 3, 3, 5, 'wtail1')]),
    part('tail_mid', 'tail', (0, 0, 4.6), (-0.4, 0, 0), [(-1.9, -1.9, 0, 3.8, 3.8, 5, 'wtail2')]),
    part('tail_tip', 'tail_mid', (0, 0, 4.6), (0.4, 0, 0), [(-1.4, -1.4, 0, 2.8, 2.8, 4, 'wtail3')]),
    part('right_arm', 'body', (-6, -13.6, 0), WARM, [(-1.7, -1, -1.7, 3.4, 7, 3.4, 'warm')]),
    part('right_forearm', 'right_arm', (0, 6.4, 0), WFORE, [(-1.5, 0, -1.5, 3, 6, 3, 'warm')]),
    part('right_sleeve', 'right_forearm', (0, 0.4, 0.6), aim([WBODY, WARM, WFORE], (0.08, 0, 0.12)), [(-2.3, -1, -5.4, 4.6, 9.5, 6, 'bigsleeve')]),
    part('right_hand', 'right_forearm', (0, 6, 0), (0, 0, 0), [(-1.4, 0, -1.5, 2.8, 2.6, 3, 'wpaw'), (-1.3, 2.4, -1.5, 2.6, 0.8, 0.6, 'wclaws')]),
    part('head', 'body', (0, -16.2, -1.4), (0, 0, 0), [
        (-4, -7.6, -4, 8, 7.4, 7.2, 'wskull'),
        (-2.3, -5.0, -12.4, 4.6, 3.4, 8.6, 'wmuzzle'),
        (-1.7, -4.7, -14.6, 3.4, 2.8, 2.4, 'wmuzzle'),
        (-1.2, -5.2, -15.3, 2.4, 1.6, 0.9, 'wnose'),
        (-4.2, -7.8, 1.4, 8.4, 7.4, 2.2, 'wnape'),
    ]),
    part('jaw', 'head', (0, -1.6, -3.8), (0.06, 0, 0), [(-2.0, 0, -10.4, 4, 1.8, 10.4, 'wjaw')]),
    part('beard', 'jaw', (0, 1.4, -4.6), (0.15, 0, 0), [(-1.6, 0, -1.2, 3.2, 8.5, 2.4, 'beard')]),
    part('beard_tip', 'beard', (0, 6.6, 0), (-0.1, 0, 0), [(-0.9, 0, -0.8, 1.8, 4, 1.6, 'beard_tip')]),
    part('right_beard', 'jaw', (-1.8, 1.0, -2.4), (0.1, 0, 0.2), [(-1.1, 0, -0.9, 2.2, 7.5, 1.8, 'beard_side')]),
    part('right_cheekfur', 'head', (-3.7, -3.2, -1.6), (0, 0.5, 0.3), [(-2.2, -2, -1.2, 2.6, 4.4, 4.2, 'wcheek')]),
    part('right_brow', 'head', (-2.1, -6.4, -4.1), (0.35, 0.3, 0.35), [(-2.0, -0.6, -1.2, 3.6, 1.2, 2.2, 'white_brow')]),
    part('right_eye', 'head', (-2.3, -5.4, -4.1), (0, 0.32, -0.1), [(-1.2, -0.55, -0.3, 2.4, 1.1, 0.45, 'weye')]),
    part('right_ear', 'head', (-2.5, -7.2, 0.4), (-0.35, 0.15, -0.28), [(-1.8, -3.6, -0.8, 3.6, 3.8, 1.6, 'wear'), (-1.1, -6.0, -0.6, 2.2, 2.6, 1.2, 'wear_tip')]),
    part('topknot', 'head', (0, -7.8, 0.4), (0, 0, 0), [(-1.6, -2.6, -1.6, 3.2, 2.8, 3.2, 'hair_bun'), (-2, -3.6, -2.2, 4, 1.4, 4.4, 'lotus_crown'),
                                                        (-2.8, -2.8, -0.3, 5.6, 0.6, 0.6, 'hairpin')]),
])
WOLF += [
    # his sword in his right paw, and his whisk in his left
    part('sword', 'right_hand', (0, 1.6, -0.4), aim([WBODY, WARM, WFORE], (1.95, 0, 0)), [
        (-0.5, -2.2, -0.5, 1, 4.4, 1, 'hilt'),
        (-2, -2.8, -0.7, 4, 0.8, 1.4, 'sword_guard'),
        (-0.7, 2, -0.7, 1.4, 1, 1.4, 'sword_pommel'),
        (-0.7, -19.8, -0.2, 1.4, 17, 0.4, 'jian')]),
    part('sword_tassel', 'sword', (0, 3, 0), (0.2, 0, 0), [(-0.4, 0, -0.4, 0.8, 4, 0.8, 'tassel_red')]),
    part('whisk', 'left_hand', (0, 1.4, 0), aim([WBODY, LWARM, LWFORE], (-0.4, 0, 0.2)), [
        (-0.4, -6, -0.4, 0.8, 8, 0.8, 'whisk_handle'), (-0.6, -6.6, -0.6, 1.2, 1, 1.2, 'whisk_cap')]),
    part('whisk_hair', 'whisk', (0, -6.4, 0), (2.4, 0, 0), [(-1.2, 0, -1.2, 2.4, 9, 2.4, 'whisk_hair')]),
]
WOLF = [(n, par, pv, (LWARM if n == 'left_arm' else LWFORE if n == 'left_forearm' else aim([WBODY, LWARM, LWFORE], (0.08, 0, -0.12)) if n == 'left_sleeve' else r), b, m)
        for n, par, pv, r, b, m in WOLF]
WOLF = w.settle(WOLF, ('right_shoe', 'left_shoe'))


def paint_wolf(p, style, f, dims):
    S = w.SIDES

    def pelt(base=GREY, density=0.45, faces=None):
        for s in faces or f:
            b = base if s != 'bottom' else GREY_LIGHT
            w.strands(p, f[s], k(b, w.LIGHT[s]), k(GREY_DARK, w.LIGHT[s]), k(GREY_LIGHT, w.LIGHT[s]), density=density, lean=0.2)

    if style in ('warm', 'wpaw', 'wskull', 'wnape', 'wjaw', 'wtail1', 'wtail2', 'wtail3', 'wruff', 'wcheek', 'wear', 'wear_tip', 'wmuzzle'):
        pelt(base=GREY_LIGHT if style in ('wcheek', 'wruff', 'wtail3') else GREY)
        if style in ('wruff', 'wcheek', 'wtail3', 'wnape'):
            for s in S:
                w.tufts(p, f[s], depth=3, every=2)
        if style == 'wskull':
            fx, fy, fw, fh = f['front']
            for i, j, xy in w.each(p, f['front']):
                if j > fh * 0.6:
                    p.px(xy, mix(p.get(xy), GREY_LIGHT, 0.5))
                elif abs(i - fw / 2 + 0.5) < 1.5:
                    p.px(xy, k(p.get(xy), 0.8))
        if style == 'wmuzzle':                                    # grey above, pale along the lips, a black lip
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    if j >= fh - 2:
                        p.px(xy, mix(p.get(xy), GREY_LIGHT, 0.6))
                w.hline(p, f[s], fh - 1, (24, 22, 26))
                p.px((fx + fw // 3, fy + fh - 1), IVORY)
            w.flat(p, f, (70, 30, 34), faces=('bottom',))
        if style == 'wear_tip':
            for s_ in S:
                w.point(p, f[s_], down=False, keep=0.3, sharp=0.5)
        if style == 'wear':
            fx, fy, fw, fh = f['front']
            for i, j, xy in w.each(p, f['front']):
                if 0 < i < fw - 1 and j > fh * 0.3:
                    p.px(xy, mix(GREY_LIGHT, (170, 130, 130), 0.4))
            for s in S:
                w.point(p, f[s], down=False, keep=0.5, sharp=0.45)
        if style == 'wjaw':
            fx, fy, fw, fh = f['top']
            for i, j, xy in w.each(p, f['top']):
                p.px(xy, (96, 30, 34) if 0 < i < fw - 1 else (IVORY if j % 2 == 0 else (40, 20, 22)))
        if style == 'wpaw':
            w.flat(p, f, (40, 36, 38), faces=('bottom',))
    if style == 'wclaws':
        w.flat(p, f, (36, 32, 34))
        w.point(p, f['front'], down=True, keep=0.3, sharp=0.8)
    if style == 'wnose':
        w.flat(p, f, (18, 16, 18))
        fx, fy, fw, fh = f['front']
        p.px((fx + 1, fy), (120, 116, 126))
    if style == 'weye':                                           # old gold eyes, narrowed
        pelt()
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            p.lit(xy, WOLF_EYE)
        p.px((fx + fw // 2, fy + fh - 1), (20, 12, 8))
        p.glow.putpixel((fx + fw // 2, fy + fh - 1), (0, 0, 0, 0))
    if style in ('white_brow', 'white_brow_long', 'beard', 'beard_tip', 'beard_side'):   # an old wolf's white brows and long grey beard
        for s in f:
            w.strands(p, f[s], k(GREY_LIGHT, w.LIGHT[s]), k((150, 148, 146), w.LIGHT[s]), (240, 238, 232), density=0.35)
        for s in S:
            w.tufts(p, f[s], depth=2 if style.startswith('white') else 3, every=2)
    if style in ('hair_bun',):
        for s in f:
            w.strands(p, f[s], k((170, 170, 172), w.LIGHT[s]), k(GREY_DARK, w.LIGHT[s]), density=0.3, down=False)
    if style == 'lotus_crown':                                    # a jade lotus crown
        jade(p, f)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(fw):                                   # petals
                if i % 2 == 0:
                    p.clear((fx + i, fy))
    if style == 'hairpin':
        w.iron(p, f, GOLD, rivets=0, rim=False, dents=0)
    if style in ('robe_chest', 'robe_skirt', 'robe_hem', 'robe_panel'):   # the Daoist's robe, blue-grey, its hem bordered black
        w.cloth(p, f, ROBE, ROBE_DEEP, fold=5 if style != 'robe_panel' else 3)
        if style in ('robe_hem', 'robe_panel'):
            for s in S:
                fx, fy, fw, fh = f[s]
                for j in range(fh - 2, fh):
                    w.hline(p, f[s], j, k(COLLAR, w.LIGHT[s]))
                w.hline(p, f[s], fh - 3, k((150, 160, 170), w.LIGHT[s]))
        if style in ('robe_chest', 'robe_hem'):                   # the eight trigrams (八卦) worked in silver round the hem and on the breast
            TRI = ['111', '110', '101', '100', '011', '010', '001', '000']
            for s_ in (('front',) if style == 'robe_chest' else S):
                fx, fy, fw, fh = f[s_]
                y0 = fy + (fh - 9 if style == 'robe_hem' else 3)
                n = 0
                for x0 in range(fx + 1, fx + fw - 4, 6):
                    if style == 'robe_chest' and abs(x0 + 2 - (fx + fw / 2)) < 4:
                        continue
                    for r, bit in enumerate(TRI[n % 8]):
                        for i in range(5):
                            if bit == '1' or i != 2:
                                p.px((x0 + i, y0 + r * 2), (214, 214, 206))
                    n += 1
        if style == 'robe_chest':                                 # the trigram 乾 worked on his back
            fx, fy, fw, fh = f['back']
            for n, y in enumerate((fy + 4, fy + 7, fy + 10)):
                for i in range(fw // 4, fw - fw // 4):
                    p.px((fx + i, y), (210, 200, 150))
            w.disc(p, fx + fw / 2, fy + fh * 0.72, 3, (230, 226, 210), rim=(20, 20, 24))
            w.disc(p, fx + fw / 2 + 1, fy + fh * 0.72, 1.4, (20, 20, 24))
    if style == 'collar':
        w.cloth(p, f, COLLAR, (8, 8, 10), fold=2)
        for s in ('front',):
            w.vline(p, f[s], 0, (150, 160, 170))
    if style in ('dsash', 'sash_tail', 'sash_knot'):
        w.cloth(p, f, SASH, k(SASH, 0.55), fold=3, coarse=False)
    if style == 'tassel_red':
        for s in f:
            w.strands(p, f[s], k((180, 30, 28), w.LIGHT[s]), (90, 14, 16), density=0.4)
        for s in S:
            w.tufts(p, f[s], depth=1, every=2)
    if style in ('elixir_gourd', 'elixir_gourd_top'):             # his gourd of elixirs, lacquered red-brown
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                p.px(xy, k(mix((170, 70, 40), (100, 36, 22), abs(i - fw / 2) / (fw / 2)), w.LIGHT[s]))
        if style == 'elixir_gourd':
            w.hline(p, f['front'], 1, GOLD)
            w.disc(p, f['front'][0] + f['front'][2] / 2, f['front'][1] + f['front'][3] * 0.6, 1.4, GOLD)
    if style == 'gourd_stopper':
        w.wood(p, f, (120, 90, 60))
    if style == 'pcord':
        w.flat(p, f, (170, 30, 28))
    if style == 'jade_disc':
        jade(p, f)
        fx, fy, fw, fh = f['front']
        w.disc(p, fx + fw / 2, fy + fh / 2, 0.8, JADE_DEEP)
    if style in ('mantle', 'mantle_back'):                        # a short black-edged mantle over his shoulders, cranes on it
        w.cloth(p, f, (60, 72, 88), (28, 34, 44), fold=4)
        w.border(p, f, COLLAR, width=2)
        if style == 'mantle_back':
            fx, fy, fw, fh = f['back']
            for i in range(4):                                    # a white crane in flight
                p.px((fx + fw // 2 - 3 + i, fy + 3 + abs(i - 1)), (226, 226, 220))
                p.px((fx + fw // 2 + i, fy + 3 + abs(i - 2)), (226, 226, 220))
            p.px((fx + fw // 2, fy + 4), (190, 30, 30))
    if style == 'bigsleeve':                                      # the great hanging sleeve, lined in black
        w.cloth(p, f, ROBE, ROBE_DEEP, fold=4)
        for s in S:
            fx, fy, fw, fh = f[s]
            for j in range(fh - 2, fh):
                w.hline(p, f[s], j, k(COLLAR, w.LIGHT[s]))
        w.flat(p, f, (14, 14, 18), faces=('bottom',))
    if style in ('trouser',):
        w.cloth(p, f, (200, 200, 196), (140, 140, 138), fold=3)
    if style in ('shoe', 'shoe_cloud'):                           # black cloud-toed shoes, white soles
        w.cloth(p, f, (22, 22, 26), (8, 8, 10), fold=3, coarse=False)
        for s in S:
            w.hline(p, f[s], f[s][3] - 1, (220, 218, 210))
        if style == 'shoe_cloud':
            w.disc(p, f['front'][0] + f['front'][2] / 2, f['front'][1] + f['front'][3] / 2, 0.8, (200, 200, 196))
    if style in ('hilt',):
        w.leather(p, f, (40, 30, 30))
    if style in ('sword_guard', 'sword_pommel'):
        w.iron(p, f, BRONZE, rivets=0, dents=0.05)
    if style == 'jian':                                           # the straight double-edged sword, a ridge down it
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                c = (240, 244, 250) if i in (0, fw - 1) else STEEL if abs(i - fw / 2 + 0.5) > 0.6 else k(STEEL, 0.75)
                p.px(xy, [x * w.LIGHT[s] + p.rnd.randint(-3, 3) for x in c])
        for s in ('front', 'back'):
            w.point(p, f[s], down=False, keep=0.3, sharp=0.6)
    if style in ('whisk_handle', 'whisk_cap'):
        w.wood(p, f, (160, 120, 70)) if style == 'whisk_handle' else jade(p, f)
    if style == 'whisk_hair':                                     # the horsetail's white hair
        for s in f:
            w.strands(p, f[s], k((236, 234, 226), w.LIGHT[s]), (180, 178, 170), (255, 255, 250), density=0.3)
        for s in S:
            w.tufts(p, f[s], depth=3, every=2)


# ══ The White-Robed Scholar ══════════════════════════════════════════════════

WHITE = (234, 232, 226)
WHITE_DEEP = (176, 176, 174)
BLACK_EDGE = (24, 22, 26)
SKIN = (222, 226, 214)
SKIN_SHADE = (170, 180, 166)
SNAKE_EYE = (150, 230, 90)
SNAKE = (232, 230, 216)
SNAKE_SPOT = (70, 54, 40)

SARM, SFORE = (-0.2, 0, 0.1), (-1.1, -0.35, 0)
SLARM, SLFORE = (0.35, 0, -0.1), (-0.4, 0.3, -0.4)


def tail_parts():
    """The serpent's tail, thick as a man's thigh where it comes out from under the back of his robe, lying
    along the ground and curling round to his right in a long coil, tapering to its tip."""
    out = []
    parent, n = 'body', 10
    length_prev = 0
    for kk in range(n):
        wdt = 8.0 - kk * 0.62
        length = 7.4 - kk * 0.3
        pivot = (0, 12.2, 2.4) if kk == 0 else (0, 0, length_prev - 0.5)
        rot = (0.12, 0.1, 0) if kk == 0 else (-0.03 if kk < 3 else 0.0, 0.40 + 0.02 * kk, 0)
        name = 'tail_%d' % kk
        out.append(part(name, parent, pivot, rot, [(-wdt / 2, 2.6 - wdt * 0.66, 0, wdt, wdt * 0.66, length, 'snake' if kk < n - 1 else 'snake_tip')]))
        parent, length_prev = name, length
    return out


SCHOLAR = mirrored([
    part('right_leg', 'root', (-2.2, 9, 0), (0, 0, 0), [(-1.6, 0, -1.6, 3.2, 14, 3.2, 'sleg')]),
    part('right_shoe', 'right_leg', (0, 14, 0), (0, 0, 0), [(-1.8, 0, -3.6, 3.6, 1.2, 5.0, 'sshoe'), (-1.0, -0.6, -4.1, 2, 1.4, 0.8, 'sshoe')]),
    part('body', 'root', (0, 9, 0), (0, 0, 0), [
        (-4.4, -15, -2.8, 8.8, 15, 5.6, 'srobe_chest'),
        (-2.0, -15.2, -3.1, 4, 6, 0.4, 'inner_collar'),
        (-4.9, -7.0, -3.3, 9.8, 3.2, 6.6, 'ssash'),
        (-1.4, -7.6, -3.9, 2.8, 3.6, 0.8, 'ssash_knot'),
        (-1.4, -17.2, -1.4, 2.8, 2.8, 2.8, 'sneck'),
        (-4.6, -15.4, -2.9, 9.2, 1.2, 5.8, 'shoulder_seam'),
    ]),
    part('scollar_right', 'body', (-0.6, -15.3, -3.0), (0, 0, -0.52), [(-1.3, 0, -0.3, 2.6, 9.4, 0.6, 'scollar')]),
    part('scollar_left', 'body', (0.6, -15.3, -3.1), (0, 0, 0.52), [(-1.3, 0, -0.3, 2.6, 9.4, 0.6, 'scollar')]),
    part('skirt', 'body', (0, -4.2, 0), (0, 0, 0), [
        (-5.2, 0, -3.4, 10.4, 8, 6.8, 'srobe_skirt'),
        (-6.2, 7.6, -4.2, 12.4, 6.4, 8.4, 'srobe_skirt2'),
        (-7.0, 13.6, -4.8, 14, 4.6, 9.6, 'srobe_hem')]),
    part('skirt_front', 'skirt', (0, 0.2, -3.6), (-0.06, 0, 0), [(-2.2, 0, -0.3, 4.4, 18, 0.6, 'srobe_panel')]),
    part('skirt_back', 'skirt', (0, 1.4, 3.6), (0.12, 0, 0), [(-5.6, 0, -0.4, 11.2, 17.6, 0.8, 'srobe_back')]),
    part('ssash_tail', 'body', (-1.4, -4.6, -3.6), (-0.05, 0, 0.05), [(-0.9, 0, -0.2, 1.8, 12, 0.4, 'ssash_tail')]),
    part('ssash_tail_short', 'body', (0.4, -4.6, -3.7), (-0.04, 0, -0.03), [(-0.8, 0, -0.2, 1.6, 8.5, 0.4, 'ssash_tail')]),
    part('spendant', 'body', (3.2, -4.2, -3.4), (0, 0, 0), [(-0.2, 0, -0.2, 0.4, 3, 0.4, 'spcord'), (-1, 3, -0.4, 2, 2.6, 0.8, 'sjade'),
                                                          (-0.6, 5.6, -0.4, 1.2, 3.4, 0.8, 'stassel')]),
    part('right_arm', 'body', (-5.4, -13.8, 0), SARM, [(-1.4, -1, -1.4, 2.8, 7, 2.8, 'swarm')]),
    part('right_forearm', 'right_arm', (0, 6.4, 0), SFORE, [(-1.3, 0, -1.3, 2.6, 6, 2.6, 'swarm')]),
    # the great scholar's sleeve (袖), hanging from the forearm whatever it does: not a block but a long
    # bag of white silk, narrow across and deep from front to back as a hanfu sleeve is, its cuff bordered
    # black where the hand comes out; below the arm the cloth swells and swoops down toward the back into the
    # sleeve's hanging pocket (袂), its lower edge a curve hemmed in black, deepest behind; an outer fold
    # lying over it, the lining pale blue-grey within
    part('right_sleeve', 'right_forearm', (0, 0.4, 0.8), aim([SARM, SFORE], (0.1, 0, 0.1)), [
        (-2.2, -1.3, -6.9, 4.4, 5.4, 9.2, 'ssleeve_wall'),
        (-2.55, -1.6, -7.25, 5.1, 4.3, 0.6, 'ssleeve_cuff'),
    ]),
    part('right_sleeve_fall', 'right_sleeve', (0, 1.0, 4.75), (-0.45, 0, 0), [(-2.6, 0, -8.0, 5.2, 10.0, 8.0, 'ssleeve_pocket')]),
    part('right_sleeve_swell', 'right_sleeve', (0, 0.6, 3.2), (-0.2, 0, 0), [(-2.4, 0, -5.5, 4.8, 9.0, 5.5, 'ssleeve_swell')]),
    part('right_sleeve_drape', 'right_sleeve_fall', (-2.75, 0.6, -0.8), (0.03, 0, 0.06), [(-0.4, 0, -6.8, 0.45, 8.6, 6.4, 'ssleeve_drape')]),
    part('right_hand', 'right_forearm', (0, 6, 0), (0, 0, 0), [(-1.1, 0, -1.2, 2.2, 2.8, 2.4, 'shand')]),
    part('head', 'body', (0, -17.2, -0.6), (0, 0, 0), [
        (-2.6, -7.6, -3.0, 5.2, 4.8, 5.8, 'sface'),
        (-2.0, -2.8, -2.8, 4.0, 2.6, 4.8, 'sjawface'),
        (-1.0, -0.4, -2.7, 2, 0.8, 2.2, 'schin'),
        (-0.5, -5.4, -3.5, 1, 2.4, 0.6, 'snose'),
        (-2.9, -7.8, -0.6, 5.8, 6.2, 3.8, 'shair'),
        (-2.8, -6.4, -2.4, 0.4, 3.6, 1.6, 'shair'), (2.4, -6.4, -2.4, 0.4, 3.6, 1.6, 'shair'),
    ]),
    part('tongue', 'head', (0, -1.6, -2.9), (0.1, 0, 0), [(-0.3, 0, -3.8, 0.6, 0.2, 3.8, 'stongue')]),
    part('right_eye', 'head', (-1.25, -4.9, -3.05), (0, 0.15, 0.22), [(-1.0, -0.3, -0.2, 2.0, 0.6, 0.3, 'seye')]),
    part('right_brow', 'head', (-1.2, -5.7, -3.1), (0, 0.15, 0.4), [(-1.3, -0.25, -0.2, 2.4, 0.5, 0.4, 'sbrow')]),
    part('cap', 'head', (0, -7.6, 0), (0, 0, 0), [(-2.9, -3.6, -3.2, 5.8, 3.8, 6.2, 'scap'), (-2.3, -6.0, -0.2, 4.6, 2.8, 3.4, 'scap_back'),
                                                  (-3.0, -0.6, -3.3, 6, 0.8, 6.4, 'scap_band')]),
    part('right_cap_wing', 'cap', (-2.9, -2.6, 2.4), (0, 0.06, -0.08), [(-11, -0.45, -0.2, 11, 0.9, 0.4, 'scap_wing')]),
] + tail_parts())
SCHOLAR += [
    # the fan, open in his right hand, held up before him
    part('fan', 'right_hand', (0, 2.4, -0.4), aim([SARM, SFORE], (0.2, 0.5, 0)), [
        (-0.4, -1, -0.4, 0.8, 2, 0.8, 'fan_rivet'),
        (-6.5, -9.0, -0.15, 13, 8.6, 0.3, 'fan_leaf')]),
]
SCHOLAR = [(n, par, pv, (SLARM if n == 'left_arm' else SLFORE if n == 'left_forearm' else aim([SLARM, SLFORE], (0.1, 0, -0.1)) if n == 'left_sleeve' else r), b, m)
           for n, par, pv, r, b, m in SCHOLAR]
SCHOLAR = w.settle(SCHOLAR, ('right_shoe', 'left_shoe'))


def paint_scholar(p, style, f, dims):
    S = w.SIDES
    if style in ('inner_collar', 'scuff', 'srobe_hem'):                          # the inner robe, pale blue-grey, a fine woven ground
        w.cloth(p, f, (196, 210, 222), (140, 156, 170), fold=4, coarse=False)
        for s_ in S:
            w.hline(p, f[s_], f[s_][3] - 1, (110, 126, 142))
        return
    if style == 'shoulder_seam':
        w.flat(p, f, WHITE_DEEP)
        return
    if style == 'sbrow':
        w.flat(p, f, (24, 22, 26))
        return
    if style == 'stassel':
        for s_ in f:
            w.strands(p, f[s_], k((176, 30, 34), w.LIGHT[s_]), (90, 14, 18), density=0.4)
        return
    if style == 'scap_band':
        w.flat(p, f, (60, 56, 66))
        return
    if style in ('srobe_chest', 'srobe_skirt', 'srobe_skirt2', 'srobe_back', 'srobe_panel', 'sbigsleeve', 'swarm'):   # the white robe, bordered black, a faint cloud pattern
        w.cloth(p, f, WHITE, WHITE_DEEP, fold=5, coarse=False)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if w.vnoise(fx + i, fy + j, 3.0, 17) > 0.74:
                    p.px(xy, k(p.get(xy), 0.9))
        if style in ('srobe_skirt2', 'sbigsleeve', 'srobe_back', 'srobe_panel'):
            for s in S:
                fx, fy, fw, fh = f[s]
                for j in range(fh - 2, fh):
                    w.hline(p, f[s], j, k(BLACK_EDGE, w.LIGHT[s]))
        if style == 'sbigsleeve':
            w.flat(p, f, BLACK_EDGE, faces=('bottom',))
    if style.startswith('ssleeve'):
        # the great sleeve: white silk in soft folds, a faint cloud pattern; its swoop hemmed in black; its
        # cuff a black border round the dark of the sleeve's mouth, the lining showing at its rim
        LINING = (196, 210, 222)
        w.cloth(p, f, WHITE, WHITE_DEEP, fold=4, coarse=False)
        for s_ in S:
            fx, fy, fw, fh = f[s_]
            for i, j, xy in w.each(p, f[s_]):
                if w.vnoise(fx + i, fy + j, 3.0, 17) > 0.74:
                    p.px(xy, k(p.get(xy), 0.9))
                if style in ('ssleeve_pocket', 'ssleeve_drape'):   # the cloth's folds, falling
                    p.px(xy, k(p.get(xy), 0.93 + 0.1 * math.cos((i % 6) / 6.0 * 2 * math.pi)))
        if style in ('ssleeve_pocket', 'ssleeve_drape'):
            w.flat(p, f, BLACK_EDGE, faces=('bottom',))
            for s_ in ('front', 'right', 'left'):
                fx, fy, fw, fh = f[s_]
                for j in range(max(0, fh - 2), fh):
                    w.hline(p, f[s_], j, k(BLACK_EDGE, w.LIGHT[s_]))
            fx, fy, fw, fh = f['back']
            for j in range(fh):                                   # the hem running up the back of the swoop
                for i in range(fw):
                    if j >= fh - 2:
                        p.px((fx + i, fy + j), k(BLACK_EDGE, w.LIGHT['back']))
        if style == 'ssleeve_drape':
            for s_ in ('right', 'left'):
                fx, fy, fw, fh = f[s_]
                for j in range(fh):
                    p.px((fx, fy + j), BLACK_EDGE)
        if style == 'ssleeve_cuff':
            w.flat(p, f, k(LINING, 0.95))
            fx, fy, fw, fh = f['front']
            for i, j, xy in w.each(p, f['front']):
                edge = i == 0 or i == fw - 1 or j == 0 or j == fh - 1
                inner = 3 <= i < fw - 3 and 2 <= j < fh - 3
                p.px(xy, BLACK_EDGE if edge else (70, 76, 88) if inner else k(LINING, 0.9))
            for s_ in S:
                w.hline(p, f[s_], 0, (120, 120, 126))
        return
    if style in ('scollar', 'ssash', 'ssash_knot', 'ssash_tail'):
        w.cloth(p, f, BLACK_EDGE, (6, 6, 8), fold=2, coarse=False)
        if style == 'ssash':
            for s in S:
                w.hline(p, f[s], 0, (120, 120, 126))
    if style in ('sleg',):
        w.cloth(p, f, (220, 218, 212), (160, 160, 156), fold=3)
    if style == 'sshoe':
        w.flat(p, f, BLACK_EDGE)
        for s in S:
            w.hline(p, f[s], f[s][3] - 1, WHITE)
    if style in ('sface', 'schin', 'sneck', 'snose', 'shand', 'sjawface'):  # pale skin, scaled faintly at the cheeks and the backs of the hands
        w.hide(p, f, SKIN, SKIN_SHADE, mottle=0.25, seed=13, fall=0.15)
        for s in (('right', 'left') if style != 'shand' else S):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if (i + (j // 2) % 2) % 2 == 0 and j % 2 == 0 and (style == 'shand' or j > fh * 0.3):
                    p.px(xy, k(p.get(xy), 0.88))
        if style == 'sjawface':                                  # a thin-lipped smile, sly, curling up at one corner; scales along the jaw
            fx, fy, fw, fh = f['front']
            for i in range(1, fw - 1):
                p.px((fx + i, fy + 2 - (1 if i >= fw - 3 else 0)), (120, 100, 116))
            for s_ in ('right', 'left'):
                w.scale_tiles(p, f[s_], k((200, 214, 196), w.LIGHT[s_]), k((140, 160, 138), w.LIGHT[s_]), cw=2, ch=2)
    if style == 'shair':
        for s in f:
            w.strands(p, f[s], k((24, 22, 26), w.LIGHT[s]), (6, 6, 8), (70, 66, 76), density=0.3)
    if style == 'seye':                                          # a serpent's eye: green, a black slit
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            p.lit(xy, SNAKE_EYE)
        for j in range(fh):
            p.px((fx + fw // 2, fy + j), (8, 10, 6))
            p.glow.putpixel((fx + fw // 2, fy + j), (0, 0, 0, 0))
        w.flat(p, f, SKIN_SHADE, faces=('right', 'left', 'top', 'bottom', 'back'))
    if style == 'stongue':                                       # the forked tongue
        w.flat(p, f, (200, 40, 60))
        for s in ('top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for j in range(min(2, fh)):
                p.clear((fx + fw // 2, fy + j))
    if style in ('scap', 'scap_back'):                           # the scholar's black cap
        w.cloth(p, f, (26, 24, 28), (8, 8, 10), fold=3, coarse=False)
        w.hline(p, f['front'], f['front'][3] - 1, (70, 66, 76))
    if style == 'scap_wing':                                     # its long stiff wings
        w.flat(p, f, (22, 20, 24))
        for s in ('front', 'back', 'top'):
            w.hline(p, f[s], 0, (60, 58, 66))
    if style in ('spcord',):
        w.flat(p, f, (40, 120, 90))
    if style == 'sjade':
        jade(p, f)
    if style in ('snake', 'snake_tip'):                         # (the white-flower snake's tail)                          # the white-flower snake: white scales, a cream belly in bands, dark flowers
        for s in f:
            fx, fy, fw, fh = f[s]
            if s == 'bottom':
                w.plates(p, f[s], (226, 214, 180), (160, 146, 110), rows=3)
                continue
            w.scale_tiles(p, f[s], k(SNAKE, w.LIGHT[s]), k((150, 150, 140), w.LIGHT[s]), cw=3, ch=2, seed=3)
            for n in range(max(1, fw * fh // 40)):               # the flowers: dark rosettes of four spots
                cx, cy = fx + p.rnd.randrange(max(1, fw)), fy + p.rnd.randrange(max(1, fh))
                for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
                    if fx <= cx + dx < fx + fw and fy <= cy + dy < fy + fh:
                        p.px((cx + dx, cy + dy), SNAKE_SPOT if (dx, dy) != (0, 0) else (140, 110, 70))
        if style == 'snake_tip':
            for s in ('right', 'left', 'top'):
                w.point(p, f[s], down=False, keep=0.3, sharp=0.35)
    if style == 'fan_rivet':
        jade(p, f)
    if style == 'fan_leaf':                                      # an open folding fan: white paper on dark ribs, ink mountains and a red seal
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            cx, cy = fw / 2 - 0.5, fh + 0.5
            for i, j, xy in w.each(p, f[s]):
                dx, dy = i - cx, cy - j
                r, a = math.hypot(dx, dy), math.atan2(dx, dy)
                if r > fh + 0.3 or r < 2.5 or abs(a) > 1.15:
                    p.clear(xy)
                    continue
                rib = int((a + 1.15) / 2.3 * 12) % 1 == 0 and abs(((a + 1.15) / 2.3 * 12) % 1 - 0.5) > 0.42
                c = (60, 42, 30) if r < 3.5 or rib else WHITE
                if r > 3.5 and not rib:                          # ink mountains along it
                    m = fh * (0.55 + 0.18 * math.sin(a * 6) + 0.08 * math.sin(a * 13))
                    if abs(r - m) < 0.9:
                        c = (40, 40, 46)
                    elif r < m and r > m - 2:
                        c = (130, 130, 136)
                if r > fh - 0.7:
                    c = (40, 36, 34)
                p.px(xy, [v * w.LIGHT[s] + p.rnd.randint(-3, 3) for v in c])
            p.px((fx + int(fw * 0.7), fy + 2), (190, 30, 30))
            p.px((fx + int(fw * 0.7) + 1, fy + 2), (190, 30, 30))
        w.flat(p, f, (60, 42, 30), faces=('right', 'left', 'top', 'bottom'))


if __name__ == '__main__':
    for captain in (False, True):
        p = Painter(61)
        slots = paint(BOAR, p, paint_boar(TRIM[captain]))
        p.save('boar_demon_captain' if captain else 'boar_demon')
    w.emit(BOAR, slots, JAVA, 'boar')
    w.report('boar_demon', BOAR, BOAR_SIZE, slots)
    for captain in (False, True):
        p = Painter(62)
        slots = paint(IMP, p, paint_imp(TRIM[captain]))
        p.save('wind_imp_captain' if captain else 'wind_imp')
    w.emit(IMP, slots, JAVA, 'imp')
    w.report('wind_imp', IMP, IMP_SIZE, slots)
    p = Painter(63)
    slots = paint(WOLF, p, paint_wolf)
    p.save('lingxuzi')
    w.emit(WOLF, slots, JAVA, 'wolf')
    w.report('lingxuzi', WOLF, WOLF_SIZE, slots)
    p = Painter(64)
    slots = paint(SCHOLAR, p, paint_scholar)
    p.save('white_robed_scholar')
    w.emit(SCHOLAR, slots, JAVA, 'scholar')
    w.report('white_robed_scholar', SCHOLAR, SCHOLAR_SIZE, slots)
