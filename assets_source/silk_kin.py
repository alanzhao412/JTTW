"""Silk-Web Ridge's lesser creatures: the spider demon and the Seven Spider Spirits' insect sons (the
stinging sons, the Mantis Son, the Cockroach Son). As silk_court.py: each one's model and skin come
from one list of parts; the model is written into SpiderCaveRenderers between its GENERATED markers.

- The spider demon (蜘蛛小妖): a spider the size of a calf, black-violet and bristling, its swollen
  abdomen marked with a crimson hourglass and pale chevrons like a mask; under a heavy brow its face is
  a cluster of eight eyes (two great ones, six small) glowing a sickly green-gold, heavy chelicerae and
  hooked fangs below them, feeling palps; eight long jointed legs, banded, spined, splayed wide.
- The stinging sons (蜜蜂, 马蜂, 牛蜢): waist-high insect demons on two pairs of veined, glassy wings,
  upright: a furred thorax, a head of two great compound eyes, feelers and mandibles; thin chitin arms
  that hold a barbed lance; four legs drawn up under them; a banded abdomen hanging in a curve to its
  sting. Three skins: the Honeybee Son (amber and black, golden fur), the Hornet Son (a rust-red head,
  chestnut and orange-yellow bands), the Gadfly Son (dun and grey, huge eyes of iridescent green).
- The Mantis Son (螳螂精): an upright mantis demon with weight to it, on four stout legs: a thick plated
  thorax, a heavy abdomen in ringed segments under broad leaf-green wings tented over it (the violet
  hind wings showing at their edges), its long prothorax in three segments with a keel and thorns,
  flaring into a shield at its shoulders; a triangular head, wide between two great bulging eyes and
  narrowing in tiers to mandibles and palps, three simple eyes on its crest, long feelers; two massive
  raptorial forelegs folded like scythes (a thick femur with a double row of spines, a spined blade to a
  hooked tip); a crimson sash knotted at its waist and a bronze collar plate.
- The Cockroach Son (蜚蠊精): a squat, broad beetle-demon on six spined legs, glossy chestnut wing
  covers domed and lacquer-bright, a pale-rimmed shield blotched dark over its head, long
  sweeping feelers, mandibles, cerci at its tail.

Run from the repository root: python assets_source/silk_kin.py"""
import math

import beastgen as bg
from silk_court import (part, mirrored, paint, emit, SilkPainter, SIDES, LIGHTS, k, mixc, noisy, each, chitin, fuzz, cloth,
                        edges, vedges, gold, compound_eye, gem_eye, cut_point, extent, JAVA)

bg.UNITS = 128
bg.TEX = bg.UNITS * bg.S


# ── The spider demon ─────────────────────────────────────────────────────────

SPIDER_LEGS = [(-4.2, -0.75, 1.12), (-1.6, -0.28, 1.0), (1.0, 0.25, 0.95), (3.6, 0.78, 1.06)]   # z, yaw (forward -), length


def spider_parts():
    ps = [
        part('body', 'root', (0, 12, 0), (0, 0, 0), [
            (-5, -4, -6, 10, 7, 11, 'thorax'),
            (-2, -5, -5, 4, 1.2, 9, 'thorax_ridge'),
            (-5.5, -1, -5, 11, 2, 9, 'thorax_rim'),
            (-3.6, -5.8, -4.2, 7.2, 1.2, 7.6, 'thorax_dome'),
        ]),
        part('head', 'body', (0, -0.5, -6), (0.12, 0, 0), [
            (-4, -3.5, -4.5, 8, 6.5, 4.5, 'face'),
            (-4.6, -4.4, -5, 9.2, 1.8, 3, 'brow'),
            (-3, 2.2, -5, 6, 1.2, 1, 'mouthplate'),
        ]),
        part('right_eye_great', 'head', (-1.6, -1.6, -4.6), (0, 0.15, 0), [(-1, -1, -0.8, 2, 2, 1, 'eye_great')]),
        part('right_eye_small_0', 'head', (-3.2, -2.6, -4.5), (0, 0.3, 0), [(-0.6, -0.6, -0.5, 1.2, 1.2, 0.6, 'eye_small')]),
        part('right_eye_small_1', 'head', (-0.7, -3.3, -4.6), (0, 0, 0), [(-0.5, -0.5, -0.5, 1, 1, 0.6, 'eye_small')]),
        part('right_eye_small_2', 'head', (-3.0, 0.2, -4.4), (0, 0.3, 0), [(-0.5, -0.5, -0.5, 1, 1, 0.6, 'eye_small')]),
        part('right_fang', 'head', (-1.5, 2.6, -4.2), (0.25, 0, 0.12), [
            (-1.2, 0, -1.2, 2.4, 3.4, 2.4, 'chelicera'),
            (-0.6, 3.2, -1.3, 1.2, 2.4, 1.2, 'fang'),
        ]),
        part('right_palp', 'head', (-3.4, 2.2, -3.6), (-0.7, 0.35, 0.35), [
            (-0.7, 0, -0.7, 1.4, 4, 1.4, 'palp'),
            (-0.8, 3.6, -0.8, 1.6, 1.8, 1.6, 'palp_tip'),
        ]),
        part('abdomen', 'body', (0, -1.5, 4.5), (-0.28, 0, 0), [
            (-7, -6, 0, 14, 11, 15, 'abdomen'),
            (-5.6, -8, 1.5, 11.2, 2.2, 12, 'abdomen_dome'),
            (-8, -4, 2, 1.2, 8, 11, 'abdomen_flank'),
            (6.8, -4, 2, 1.2, 8, 11, 'abdomen_flank'),
            (-5.5, -4.5, 14, 11, 8, 4, 'abdomen_tip'),
            (-1.5, -1, 17.5, 3, 2, 2, 'spinneret'),
        ] + [(-0.6, -9.6, 2.5 + 3.0 * i, 1.2, 2, 1.2, 'spike') for i in range(4)]),
    ]
    for i, (z, yaw, ln) in enumerate(SPIDER_LEGS):
        ps += [
            part('right_hip_%d' % i, 'body', (-4.6, 0.5, z), (0, yaw, 0), []),
            part('right_leg_%d' % i, 'right_hip_%d' % i, (0, 0, 0), (0, 0, 0.8), [
                (-2, -1.5, -1.5, 2, 3, 3, 'coxa'),
                (-11 * ln, -1.1, -1.1, 11 * ln, 2.2, 2.2, 'femur'),
                (-6 * ln, -2.2, -0.35, 0.7, 1.2, 0.7, 'legspine'),
            ]),
            part('right_shin_%d' % i, 'right_leg_%d' % i, (-11 * ln, 0, 0), (0, 0, -2.0), [
                (-9 * ln, -0.9, -0.9, 9 * ln, 1.8, 1.8, 'tibia'),
                (-19 * ln, -0.6, -0.6, 10.2 * ln, 1.2, 1.2, 'tarsus'),
                (-1.4, -1.3, -1.3, 1.6, 2.6, 2.6, 'knee'),
            ]),
        ]
    return mirrored(ps)


SPIDER_BLACK = (34, 22, 40)
SPIDER_SHEEN = (120, 96, 150)
SPIDER_BAND = (120, 28, 44)
SPIDER_PALE = (214, 206, 176)
SPIDER_EYE = (190, 230, 70)


def spider_painter():
    def paint_box(p, style, f, dims):
        if style in ('thorax', 'thorax_ridge', 'thorax_rim', 'thorax_dome', 'brow', 'mouthplate'):
            chitin(p, f, SPIDER_BLACK, SPIDER_SHEEN)
            if style == 'thorax':
                fx, fy, fw, fh = f['top']                            # a pale lyre mark on its back, and bristles
                for i, j, xy in each(f['top']):
                    u, v = (i + 0.5) / fw, (j + 0.5) / fh
                    if abs(abs(u - 0.5) - 0.18 * (1 - v)) < 0.05 and v > 0.2:
                        p.px(xy, k(SPIDER_PALE, 0.85))
                for s in ('top', 'right', 'left', 'front', 'back'):
                    for i, j, xy in each(f[s]):
                        if p.rnd.random() < 0.08:
                            p.px(xy, (150, 130, 120))
            if style == 'thorax_rim':
                edges(p, f, k(SPIDER_BAND, 0.8), top=False)
        elif style == 'face':
            chitin(p, f, SPIDER_BLACK, SPIDER_SHEEN, gloss=False)
            fx, fy, fw, fh = f['front']                              # the mask: pale bristles round the mouth, dark round the eyes
            for i, j, xy in each(f['front']):
                v = (j + 0.5) / fh
                if v > 0.62 and p.rnd.random() < 0.45:
                    p.px(xy, k(SPIDER_PALE, 0.7 + 0.3 * p.rnd.random()))
            # a ring of lesser eyes painted between the domed ones
            for (ex, ey) in ((3, 2), (fw - 5, 2), (1, 5), (fw - 3, 5), (fw // 2 - 1, 1)):
                gem_eye(p, fx + ex, fy + ey, SPIDER_EYE, w=2, h=2, pupil=False)
        elif style in ('eye_great', 'eye_small'):
            for s, r in f.items():
                for i, j, xy in each(r):
                    u, v = (i + 0.5) / r[2], (j + 0.5) / r[3]
                    c = mixc(SPIDER_EYE, (255, 255, 230), 0.6) if (u < 0.45 and v < 0.45) else k(SPIDER_EYE, 0.9 - 0.3 * v)
                    if s == 'back':
                        c = SPIDER_BLACK
                    p.px(xy, c, glow=s != 'back')
        elif style in ('chelicera', 'palp', 'palp_tip', 'coxa', 'knee'):
            chitin(p, f, (48, 30, 52) if style != 'chelicera' else (60, 20, 30), SPIDER_SHEEN)
            if style == 'palp_tip':
                fuzz(p, {s: f[s] for s in ('front', 'bottom')}, (60, 50, 50))
        elif style in ('fang', 'spike', 'legspine'):
            for s, r in f.items():
                for i, j, xy in each(r):
                    t = (j + 0.5) / r[3]
                    p.px(xy, noisy(p, mixc((60, 40, 40), (226, 214, 190), t) if style == 'fang' else mixc((40, 28, 40), (200, 190, 170), 1 - t), 3))
        elif style in ('abdomen_dome', 'abdomen_flank'):
            chitin(p, f, (46, 28, 52), SPIDER_SHEEN)
            for s2 in ('top', 'right', 'left', 'front', 'back'):
                for i, j, xy in each(f[s2]):
                    r = p.rnd.random()
                    if r < 0.12:
                        p.px(xy, (150, 128, 120))
                    elif style == 'abdomen_dome' and s2 == 'top' and abs((i + 0.5) / f[s2][2] - 0.5) < 0.12:
                        p.px(xy, noisy(p, SPIDER_BAND, 5))
        elif style == 'abdomen':
            chitin(p, f, (42, 26, 48), SPIDER_SHEEN, gloss=True)
            # the hourglass and chevrons over its back like a mask; fine hair over it
            fx, fy, fw, fh = f['top']
            for i, j, xy in each(f['top']):
                u, v = (i + 0.5) / fw, (j + 0.5) / fh
                hour = abs(u - 0.5) < 0.1 + 0.22 * abs(v - 0.55) and abs(v - 0.55) < 0.3
                chev = abs((v * 6) % 1 - 0.5 - abs(u - 0.5) * 1.2) < 0.08 and v < 0.35 and abs(u - 0.5) < 0.35
                if hour:
                    p.px(xy, noisy(p, SPIDER_BAND if abs(u - 0.5) > 0.05 else k(SPIDER_BAND, 1.3), 6))
                elif chev:
                    p.px(xy, noisy(p, SPIDER_PALE, 6))
                elif p.rnd.random() < 0.07:
                    p.px(xy, (120, 100, 110))
            for s in ('right', 'left', 'back'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(f[s]):
                    if (i + j * 2) % 9 == 0 and j < fh * 0.7:
                        p.px(xy, k(SPIDER_PALE, 0.6))
        elif style in ('abdomen_tip', 'spinneret'):
            chitin(p, f, (52, 30, 50), SPIDER_SHEEN)
            if style == 'spinneret':
                for i, j, xy in each(f['back']):
                    p.px(xy, (236, 232, 222), glow=True)
        elif style in ('femur', 'tibia', 'tarsus'):
            chitin(p, f, SPIDER_BLACK, SPIDER_SHEEN, bands=3 if style != 'tarsus' else 2, band=(96, 40, 54))
            for s in ('top', 'front', 'back'):
                for i, j, xy in each(f[s]):
                    if p.rnd.random() < 0.12:
                        p.px(xy, (160, 140, 130))
        else:
            chitin(p, f, SPIDER_BLACK, SPIDER_SHEEN)
    return paint_box


# ── The stinging sons ────────────────────────────────────────────────────────

def stinger_parts():
    ps = [
        part('thorax', 'root', (0, 8, 0), (0.15, 0, 0), [
            (-3.5, -6, -2.5, 7, 7, 5, 'thorax'),
            (-4, -6.8, -3, 8, 2, 6, 'collar_fur'),
            (-2.4, 0.4, -2, 4.8, 1.6, 4, 'waist'),
        ]),
        part('head', 'thorax', (0, -6.6, -0.6), (0, 0, 0), [
            (-3, -5, -3.2, 6, 5, 4.6, 'head'),
            (-2, -1.2, -3.6, 4, 1.8, 1.4, 'clypeus'),
        ]),
        part('right_eye', 'head', (-2.6, -2.8, -1.4), (0, 0.2, 0), [(-1.3, -2, -1.8, 2, 4, 3.4, 'compound')]),
        part('right_mandible', 'head', (-1.1, -0.3, -3.2), (0, 0, 0.15), [(-1, 0, -1.4, 1.6, 1.4, 1.6, 'mandible')]),
        part('right_antenna', 'head', (-1, -4.8, -2.8), (0.55, 0.3, -0.25), [
            (-0.35, -5, -0.35, 0.7, 5, 0.7, 'antenna'),
            (-0.45, -7.4, -0.45, 0.9, 2.6, 0.9, 'antenna_club'),
        ]),
        part('right_arm', 'thorax', (-3.6, -5, -0.4), (-0.55, 0, 0.25), [(-1.2, -0.6, -1.2, 2, 6, 2, 'arm')]),
        part('right_forearm', 'right_arm', (-0.2, 5.4, 0), (-0.75, 0, 0), [
            (-1, 0, -1, 1.8, 5, 1.8, 'forearm'),
            (-1.2, 4.6, -1.3, 2.2, 1.6, 2.2, 'claw'),
        ]),
        part('lance', 'right_forearm', (-0.1, 5.4, -0.2), (1.3, 0, 0), [
            (-0.4, -12, -0.4, 0.8, 20, 0.8, 'lance_shaft'),
            (-0.75, -15.5, -0.75, 1.5, 3.5, 1.5, 'lance_head'),
            (-1.4, -12.6, -0.25, 2.8, 0.8, 0.5, 'lance_barb'),
        ]),
        part('right_leg_0', 'thorax', (-2.6, 0, -1.2), (0.6, 0, 0.55), [(-0.6, 0, -0.6, 1.2, 5, 1.2, 'leg')]),
        part('right_shin_0', 'right_leg_0', (0, 5, 0), (-1.3, 0, 0), [(-0.5, 0, -0.5, 1, 5.5, 1, 'leg_shin')]),
        part('right_leg_1', 'thorax', (-2.4, 0.4, 1.0), (0.2, 0, 0.6), [(-0.6, 0, -0.6, 1.2, 5, 1.2, 'leg')]),
        part('right_shin_1', 'right_leg_1', (0, 5, 0), (-1.1, 0, 0), [(-0.5, 0, -0.5, 1, 5.5, 1, 'leg_shin')]),
        part('abdomen', 'thorax', (0, 1.4, 1.2), (-0.95, 0, 0), [
            (-3, -1, 0, 6, 5.5, 7, 'abdomen_a'),
            (-2.5, -0.5, 6.5, 5, 4.5, 4, 'abdomen_b'),
            (-1.4, 0.4, 10, 2.8, 2.6, 2.2, 'abdomen_c'),
        ]),
        part('stinger', 'abdomen', (0, 1.6, 12), (0.25, 0, 0), [(-0.4, -0.4, 0, 0.8, 0.8, 3.4, 'sting')]),
        part('right_wing_fore', 'thorax', (-1.4, -5.6, 2), (0.15, 0.55, 0.35), [(-16, -0.1, -2.6, 16, 0.2, 6, 'wing_fore')]),
        part('right_wing_hind', 'thorax', (-1.4, -4.2, 2.6), (0.25, 0.85, 0.2), [(-12, -0.1, -2, 12, 0.2, 4.4, 'wing_hind')]),
    ]
    return mirrored(ps)


# (body, band, fur, eye, wing)
STINGERS = {
    'honeybee_son': ((206, 140, 34), (34, 22, 14), (236, 186, 70), (60, 40, 20), (200, 190, 150)),
    'hornet_son': ((150, 60, 26), (232, 170, 40), (190, 96, 40), (90, 30, 16), (210, 160, 110)),
    'gadfly_son': ((116, 108, 84), (70, 64, 50), (150, 140, 110), (60, 170, 110), (170, 180, 170)),
}


def stinger_painter(name):
    body, band, furc, eyec, wing = STINGERS[name]
    head = (170, 50, 24) if name == 'hornet_son' else k(body, 0.85)

    def paint_box(p, style, f, dims):
        if style in ('thorax', 'waist'):
            chitin(p, f, k(body, 0.75), bands=0)
            fuzz(p, {s: f[s] for s in ('top', 'front', 'right', 'left', 'back')}, furc, density=0.6)
        elif style == 'collar_fur':
            fuzz(p, f, furc, density=0.8)
        elif style in ('head', 'clypeus'):
            chitin(p, f, head)
            if style == 'clypeus':
                for i, j, xy in each(f['front']):
                    p.px(xy, noisy(p, (230, 196, 60) if name != 'gadfly_son' else (150, 140, 110), 5))
            else:
                fx, fy, fw, fh = f['front']                          # three little eyes on its crown
                for ex in (fw // 2 - 3, fw // 2, fw // 2 + 3):
                    p.px((fx + ex, fy + 1), (250, 240, 200), glow=True)
        elif style == 'compound':
            compound_eye(p, f, eyec if name != 'gadfly_son' else (70, 190, 120), glow=0.5)
            if name == 'gadfly_son':                                 # iridescent: a band of violet across
                for s in ('front', 'right', 'left'):
                    for i, j, xy in each(f[s]):
                        if abs(j - f[s][3] * 0.45) < 1.5:
                            p.px(xy, (150, 90, 190), glow=True)
        elif style in ('mandible', 'claw'):
            chitin(p, f, (60, 34, 20), (200, 160, 110))
        elif style in ('antenna', 'antenna_club', 'arm', 'forearm', 'leg', 'leg_shin'):
            chitin(p, f, (40, 28, 20) if name != 'gadfly_son' else (60, 56, 46))
            if style in ('arm', 'leg'):
                fuzz(p, {'top': f['top'], 'front': f['front']}, furc, density=0.4)
        elif style.startswith('abdomen'):
            chitin(p, f, body, bands=3 if style == 'abdomen_a' else 2, band=band)
            if name == 'honeybee_son':
                fuzz(p, {'top': f['top']}, mixc(body, furc, 0.5), density=0.3)
        elif style == 'sting':
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, (30, 20, 16), 3))
        elif style in ('lance_shaft',):
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, k((96, 60, 30), LIGHTS[s] * (0.9 if (j // 6) % 2 else 1.05)), 3))
        elif style in ('lance_head', 'lance_barb'):
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, k((180, 186, 196), LIGHTS[s] * (1.1 - 0.3 * (j / max(1, r[3])))), 3))
        elif style in ('wing_fore', 'wing_hind'):
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    if s not in ('top', 'bottom'):
                        p.px(xy, k(wing, 0.6), a=200)
                        continue
                    u, v = (i + 0.5) / fw, (j + 0.5) / fh
                    if (u - 0.5) ** 2 / 0.25 + (v - 0.5) ** 2 / 0.25 > 1.05:   # an oval wing, clear outside
                        p.clear(xy)
                        continue
                    vein = abs(v - 0.5 - 0.15 * math.sin(u * 7)) < 0.06 or abs((u * 5) % 1) < 0.08 or abs(v - 0.2) < 0.04
                    p.px(xy, k(wing, 0.55) if vein else mixc(wing, (255, 255, 255), 0.35 * (1 - u)), a=230 if vein else 110)
        else:
            chitin(p, f, body)
    return paint_box


# ── The Mantis Son ───────────────────────────────────────────────────────────

MANTIS_GREEN = (104, 160, 62)
MANTIS_DARK = (58, 102, 40)
MANTIS_PALE = (196, 214, 120)
BRONZE = (170, 112, 50)
CRIMSON = (150, 26, 34)


def mantis_parts():
    """A mantis with weight to it: a thick, plated thorax under a crimson sash; a heavy abdomen in six
    ringed segments under broad leaf-wings tented over it, the hind wings' violet showing at their edges; the
    long prothorax in three segments with a keel down its back and thorns down its edges, flaring at the
    top into the shield the forelegs hang from; the head a broad triangle, wide across the crown between two
    great bulging eyes and narrowing in tiers to the jaws (mandibles, palps), three simple eyes on a crest
    between the feelers; the raptorial forelegs massive: a long coxa, a thick femur with a double row of
    spines and its claw-groove, the tibia's blade spined along its edge to a hooked tip; four walking legs,
    each a stout femur to a spined knee, a long shin and a foot of segments flat on the ground."""
    ps = [
        part('waist', 'root', (0, 6.5, 0), (0, 0, 0), [(-4.0, -3.0, -3.6, 8.0, 6.0, 7.2, 'meso'),
                                                       (-4.3, -3.5, -3.1, 8.6, 1.3, 6.4, 'thorax_plate'),
                                                       (-4.5, -1.3, -3.9, 9.0, 1.8, 7.8, 'sash'),
                                                       (-1.4, -1.6, -4.4, 2.8, 2.6, 0.8, 'sash_knot')]),
        part('sash_tail', 'waist', (1.6, 0.4, -4.1), (-0.1, 0, -0.1), [(-0.9, 0, -0.3, 1.8, 7.5, 0.5, 'sash_tail')]),
        part('abdomen', 'waist', (0, -0.6, 2.8), (-0.32, 0, 0), [
            (-4.2, -2.8, 0, 8.4, 5.8, 4.0, 'abd_seg'),
            (-4.5, -3.0, 3.6, 9.0, 6.2, 4.0, 'abd_seg'),
            (-4.2, -2.8, 7.2, 8.4, 5.7, 3.8, 'abd_seg'),
            (-3.5, -2.3, 10.6, 7.0, 4.7, 3.4, 'abd_seg'),
            (-2.6, -1.7, 13.6, 5.2, 3.5, 2.9, 'abd_seg'),
            (-1.7, -1.1, 16.1, 3.4, 2.3, 2.2, 'abd_tip'),
            (-1.0, -0.4, 17.9, 0.7, 0.7, 2.8, 'cercus'), (0.3, -0.4, 17.9, 0.7, 0.7, 2.8, 'cercus'),
        ]),
        part('wings', 'abdomen', (0, -3.3, -0.4), (0.07, 0, 0), []),
        part('right_hindwing', 'wings', (-0.3, 0.6, 0.6), (0, 0, 0.66), [(-5.6, -0.25, 0, 5.6, 0.5, 18.2, 'hindwing')]),
        part('right_tegmen', 'wings', (-0.1, -0.1, 0), (0, 0, 0.4), [(-6.0, -0.35, 0, 6.0, 0.7, 19.6, 'tegmen'),
                                                                   (-6.3, -0.2, 0.6, 0.6, 0.9, 18.2, 'costa')]),
        # the prothorax, long and segmented, flaring at the top into the shield the forelegs hang from
        part('prothorax', 'waist', (0, -2.6, -0.6), (0.14, 0, 0), [
            (-2.8, -7.6, -2.7, 5.6, 7.8, 5.4, 'pro_low'),
            (-2.2, -13.0, -2.2, 4.4, 5.8, 4.4, 'pro_mid'),
            (-2.5, -17.4, -2.4, 5.0, 4.6, 4.8, 'pro_top'),
            (-3.8, -15.6, -3.0, 7.6, 3.4, 6.0, 'pronotum_lobe'),
            (-0.6, -17.2, 2.0, 1.2, 16.0, 0.8, 'pro_keel'),
            (-2.7, -18.0, -2.7, 5.4, 1.6, 5.4, 'collar'),
            (-2.3, -15.2, -3.4, 4.6, 1.4, 0.8, 'collar_plate'),
        ] + [(x, y, -0.5, 0.6, 0.6, 1.3, 'thorn') for y in (-11.5, -9.0, -6.2, -3.4) for hw in ((2.2 if y < -8 else 2.8),) for x in (-hw - 0.45, hw - 0.15)]),
        # the head: a broad triangle, wide across the crown between the eyes, narrowing in tiers to the jaws
        part('head', 'prothorax', (0, -18.0, -0.4), (-0.1, 0, 0), [
            (-5.8, -4.6, -3.0, 11.6, 2.4, 5.0, 'head_top'),
            (-4.8, -2.4, -3.0, 9.6, 2.6, 4.8, 'head_top2'),
            (-3.6, 0, -2.9, 7.2, 2.4, 4.2, 'head_mid'),
            (-2.4, 2.2, -2.8, 4.8, 1.9, 3.4, 'head_low'),
            (-1.4, 3.9, -2.7, 2.8, 1.8, 2.6, 'mouthparts'),
            (-2.0, -5.6, -1.4, 4.0, 1.2, 2.8, 'vertex'),
            (-1.9, -2.2, -3.35, 3.8, 3.2, 0.5, 'frons'),
        ]),
        part('right_eye', 'head', (-5.0, -3.2, -0.4), (0, 0.15, 0.15), [(-3.0, -3.0, -3.0, 4.2, 6.0, 6.0, 'compound')]),
        part('right_mandible', 'head', (-1.1, 5.0, -2.2), (0.15, 0.3, 0.35), [(-1.0, 0, -0.7, 1.5, 1.8, 1.4, 'mandible')]),
        part('right_palp', 'head', (-1.7, 4.4, -2.5), (0.35, 0.2, 0.3), [(-0.3, 0, -0.3, 0.6, 2.6, 0.6, 'palp')]),
        part('right_antenna', 'head', (-0.9, -3.6, -2.2), (0.85, 0.25, -0.2), [(-0.3, -13, -0.3, 0.6, 13, 0.6, 'antenna')]),
        part('right_antenna_tip', 'right_antenna', (0, -12.8, 0), (-0.35, 0, -0.1), [(-0.2, -9, -0.2, 0.4, 9, 0.4, 'antenna')]),
        # the raptorial forelegs, folded before it like scythes
        part('right_coxa', 'prothorax', (-2.9, -14.2, -1.6), (-0.85, 0.15, 0.3), [(-1.45, 0, -1.45, 2.9, 7.6, 2.9, 'coxa'),
                                                                           (-1.8, 0.4, -1.8, 3.6, 1.6, 3.6, 'armlet')]),
        part('right_femur', 'right_coxa', (0, 7.4, 0), (-2.55, 0, 0), [
            (-1.75, -0.2, -2.0, 3.5, 12.2, 3.9, 'femur'),
            (-0.6, 0.8, -2.8, 1.2, 10.4, 0.9, 'femur_spines'),
        ] + [(-1.2, 1.4 + 1.7 * n, -3.2 - (1.2 if n % 2 == 0 else 0.4), 0.6, 0.6, 1.4 + (1.2 if n % 2 == 0 else 0.4), 'spine') for n in range(6)]
          + [(0.6, 2.2 + 1.9 * n, -3.0 - (0.8 if n % 2 else 0.2), 0.6, 0.6, 1.2 + (0.8 if n % 2 else 0.2), 'spine') for n in range(5)]),
        part('right_tibia', 'right_femur', (0, 11.8, 0), (2.7, 0, 0), [
            (-1.15, 0, -1.25, 2.3, 9.8, 2.5, 'blade'),
            (-0.5, 0.8, -2.15, 1.0, 8.2, 1.0, 'blade_edge'),
            (-0.75, 9.3, -2.6, 1.5, 2.2, 2.6, 'hook'),
            (-0.4, 9.2, 0.8, 0.8, 3.2, 0.8, 'tarsus'),
        ] + [(-0.3, 1.6 + 1.6 * n, -2.9, 0.6, 0.6, 0.9, 'spine') for n in range(5)]),
    ]
    for i, (z, yaw) in enumerate(((-1.2, -0.55), (1.6, 0.6))):
        ps += [
            part('right_hip_%d' % i, 'waist', (-3.2, 1, z), (0, yaw, 0), []),
            part('right_leg_%d' % i, 'right_hip_%d' % i, (0, 0, 0), (0, 0, 0.5), [(-8.2, -1.05, -1.05, 8.6, 2.1, 2.1, 'leg'),
                                                                               (-5.6, -1.3, -1.25, 3.6, 2.5, 2.5, 'leg_bulge')]),
            part('right_shin_%d' % i, 'right_leg_%d' % i, (-8, 0, 0), (0, 0, -1.75), [(-21, -0.7, -0.7, 21, 1.4, 1.4, 'leg_shin'),
                                                                                       (-1.5, -1.25, -1.25, 2.2, 2.5, 2.5, 'knee'),
                                                                                       (-7.5, -1.4, -0.3, 0.6, 0.9, 0.6, 'spine'),
                                                                                       (-12.5, -1.3, -0.3, 0.6, 0.8, 0.6, 'spine'),
                                                                                       (-17.0, -1.2, -0.3, 0.6, 0.7, 0.6, 'spine')]),
            part('right_foot_%d' % i, 'right_shin_%d' % i, (-20.8, 0, 0), (0, 0, 1.25), [(-3.4, -0.45, -0.5, 3.4, 0.9, 1.0, 'tarsus'),
                                                                                         (-4.0, -0.3, -0.35, 0.8, 0.6, 0.7, 'claw')]),
        ]
    # its bulk: the body, the scythes and the legs thickened across their length (a heavy insect, not a stick)
    bulk = {'waist': (1.2, 1.15, 1.2), 'abdomen': (1.22, 1.2, 1.0), 'prothorax': (1.22, 1.0, 1.2), 'head': (1.12, 1.1, 1.1),
            'right_coxa': (1.2, 1.0, 1.2), 'right_femur': (1.25, 1.0, 1.15), 'right_tibia': (1.2, 1.0, 1.1),
            'right_eye': (1.15, 1.15, 1.15)}
    for i in range(2):
        bulk['right_leg_%d' % i] = (1.0, 1.3, 1.3)
        bulk['right_shin_%d' % i] = (1.0, 1.35, 1.35)
    out = []
    for name, parent, pivot, rot, boxes, mirror in ps:
        sx, sy, sz = bulk.get(name, (1, 1, 1))
        out.append((name, parent, pivot, rot, [(x * sx, y * sy, z * sz, w_ * sx, h * sy, d * sz, st) for x, y, z, w_, h, d, st in boxes], mirror))
    return mirrored(out)


def mantis_painter():
    def paint_box(p, style, f, dims):
        if style in ('abd_seg', 'abd_tip'):
            # a ring of the abdomen: pale-green plate, its lower edge overlapping the next in a dark seam, a
            # paler band along its belly, a faint dark dorsal line
            chitin(p, f, mixc(MANTIS_GREEN, MANTIS_PALE, 0.15), MANTIS_PALE, gloss=True)
            for s2 in SIDES:
                fx, fy, fw, fh = f[s2]
                for i in range(fw):
                    p.px((fx + i, fy), k(MANTIS_DARK, 0.8))
                    p.px((fx + i, fy + 1), k(MANTIS_DARK, 1.05))
            for s2 in ('top', 'bottom'):
                fx, fy, fw, fh = f[s2]
                for i, j, xy in each(f[s2]):
                    if j >= fh - 2:
                        p.px(xy, k(MANTIS_DARK, 0.85))
                    elif s2 == 'bottom' and abs(i - fw / 2) < fw * 0.3:
                        p.px(xy, noisy(p, mixc(MANTIS_PALE, (230, 226, 160), 0.4), 3))
                    elif s2 == 'top' and abs(i + 0.5 - fw / 2) < 1:
                        p.px(xy, k(MANTIS_DARK, 0.9))
            return
        if style in ('pro_low', 'pro_mid', 'pro_top', 'thorax_plate'):
            # the prothorax: glossy green, a pale seam down its front, ridged in rings, its edges darker
            chitin(p, f, MANTIS_GREEN, MANTIS_PALE, gloss=True)
            for s2 in SIDES:
                fx, fy, fw, fh = f[s2]
                for j in range(fh):
                    if j % 6 == 5:
                        for i in range(fw):
                            p.px((fx + i, fy + j), k(MANTIS_DARK, 0.95))
                    if s2 == 'front':
                        p.px((fx + fw // 2, fy + j), MANTIS_PALE)
                for i in range(fw):
                    p.px((fx + i, fy), k(MANTIS_PALE, 0.95))
            return
        if style in ('pro_keel', 'vertex', 'frons', 'costa'):
            chitin(p, f, mixc(MANTIS_GREEN, MANTIS_PALE, 0.4), (250, 255, 220))
            if style == 'vertex':                                    # its three simple eyes
                fx, fy, fw, fh = f['front']
                for ex in (fw // 2 - 3, fw // 2, fw // 2 + 3):
                    p.px((fx + ex, fy + 1), (250, 230, 150), glow=True)
            if style == 'frons':
                fx, fy, fw, fh = f['front']
                for j in range(fh):
                    p.px((fx + fw // 2, fy + j), k(MANTIS_DARK, 1.0))
            return
        if style == 'mandible':
            chitin(p, f, (150, 120, 60), (230, 210, 150))
            for s2 in ('front', 'right', 'left'):
                cut_point(p, f[s2], keep=0.3, down=True, sharp=0.6)
            return
        if style == 'palp':
            chitin(p, f, mixc(MANTIS_GREEN, MANTIS_PALE, 0.5))
            return
        if style == 'leg_bulge':
            chitin(p, f, MANTIS_GREEN, MANTIS_PALE, gloss=True)
            return
        if style in ('tarsus', 'claw'):
            chitin(p, f, MANTIS_DARK if style == 'tarsus' else (40, 34, 24))
            if style == 'tarsus':
                for s2 in SIDES:
                    fx, fy, fw, fh = f[s2]
                    for i in range(0, fw, 3):
                        for j in range(fh):
                            p.px((fx + i, fy + j), (30, 46, 22))
            return
        if style == 'sash_knot':
            cloth(p, f, CRIMSON)
            edges(p, f, (230, 180, 70))
            return
        if style == 'cercus':
            chitin(p, f, MANTIS_DARK)
            return
        if style in ('meso', 'prothorax', 'prothorax_low', 'head_top', 'head_mid', 'head_low', 'coxa', 'femur', 'leg', 'leg_shin', 'knee'):
            chitin(p, f, MANTIS_GREEN, MANTIS_PALE, gloss=style in ('head_top', 'femur', 'prothorax'))
            if style == 'prothorax':                                  # a pale seam down its front, and ridges
                fx, fy, fw, fh = f['front']
                for j in range(fh):
                    p.px((fx + fw // 2, fy + j), MANTIS_PALE)
                    if j % 5 == 0:
                        for i in range(fw):
                            p.px((fx + i, fy + j), k(MANTIS_DARK, 1.1))
            if style == 'head_top':
                fx, fy, fw, fh = f['front']
                for ex in (fw // 2 - 2, fw // 2, fw // 2 + 2):         # its three simple eyes
                    p.px((fx + ex, fy + 2), (240, 220, 140), glow=True)
            if style == 'femur':
                for i, j, xy in each(f['back']):
                    if j % 4 == 0:
                        p.px(xy, (230, 230, 200))
                fx, fy, fw, fh = f['front']                          # a dark eye-spot on its inner face, as a mantis's
                for i, j, xy in each(f['front']):
                    if math.hypot(i - fw / 2, j - fh * 0.3) < 1.6:
                        p.px(xy, (30, 24, 40))
        elif style == 'mouthparts':
            chitin(p, f, (140, 120, 60))
        elif style == 'compound':
            compound_eye(p, f, (130, 180, 64), glow=0.12)
            for s in ('front', 'right'):                              # the false pupil a mantis's eye shows
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(f[s]):
                    if math.hypot(i - fw / 2, j - fh / 2) < 1.1:
                        p.px(xy, (20, 26, 14))
        elif style == 'antenna':
            chitin(p, f, MANTIS_DARK)
        elif style.startswith('abdomen'):
            chitin(p, f, mixc(MANTIS_GREEN, MANTIS_PALE, 0.2), plate=3)
        elif style in ('tegmen', 'hindwing'):                         # the wings tented over its back: leaf-green, veined like a leaf
            for s2, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    u, v = (i + 0.5) / fw, (j + 0.5) / fh
                    base = mixc(MANTIS_GREEN, (170, 200, 90), 0.35) if style == 'tegmen' else (150, 120, 160)
                    c = k(base, LIGHTS[s2] * (1.05 - 0.2 * v))
                    if style == 'tegmen' and s2 in ('top', 'bottom'):
                        if abs(u - 0.62) < 0.05 or abs(((v * 6 - u * 2) % 1)) < 0.08 and u > 0.62:
                            c = k(MANTIS_PALE, 1.0)
                        elif abs(((v * 9 + u * 2) % 1)) < 0.06 and u < 0.62:
                            c = k(MANTIS_DARK, 1.0)
                        if u < 0.08:
                            c = k((120, 96, 50), 1.0)
                        if abs(u - 0.4) < 0.08 and abs(v - 0.2) < 0.04:
                            c = (240, 236, 200)
                    elif style == 'hindwing' and s2 in ('top', 'bottom') and (i + j) % 4 == 0:
                        c = k((90, 60, 110), 1.0)
                    p.px(xy, noisy(p, c, 3))
        elif style == 'pronotum_lobe':
            chitin(p, f, MANTIS_GREEN, MANTIS_PALE)
            for s2 in SIDES:
                fx, fy, fw, fh = f[s2]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), (120, 96, 50))
                    if i % 3 == 0:
                        p.px((fx + i, fy), MANTIS_PALE)
        elif style in ('thorn', 'spine'):
            for s2, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, (238, 232, 196) if s2 != 'front' else (30, 24, 20), 3))
            for s2 in ('right', 'left', 'top', 'bottom'):
                fx, fy, fw, fh = f[s2]
                for i, j, xy in each(f[s2]):
                    if i < fw * 0.3:
                        p.px(xy, (30, 24, 20))
        elif style == 'wing_folded':
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    u, v = (i + 0.5) / fw, (j + 0.5) / fh
                    c = k(mixc(MANTIS_GREEN, (150, 190, 80), 0.4), LIGHTS[s])
                    if s == 'top' and (abs(u - 0.5) < 0.03 or abs((v * 7 + abs(u - 0.5) * 3) % 1) < 0.07):
                        c = k(MANTIS_DARK, 1.0)
                    p.px(xy, noisy(p, c, 3))
        elif style in ('femur_spines',):
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, (230, 226, 196) if j % 3 != 2 else (60, 60, 40))
        elif style == 'blade_edge':
            for s2, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, (238, 240, 214) if j % 2 else (110, 120, 70))
        elif style in ('blade', 'hook'):
            chitin(p, f, mixc(MANTIS_GREEN, MANTIS_PALE, 0.35), (250, 255, 220))
            if style == 'blade':
                for i, j, xy in each(f['back']):
                    p.px(xy, (236, 236, 210) if j % 2 else (80, 90, 50))
        elif style in ('collar', 'collar_plate', 'armlet'):
            gold(p, f, BRONZE)
            if style == 'collar_plate':
                fx, fy, fw, fh = f['front']
                for i in range(fw):
                    if i % 3 == 1:
                        p.px((fx + i, fy + fh // 2), (90, 50, 20))
        elif style in ('sash', 'sash_tail'):
            cloth(p, f, CRIMSON)
            edges(p, f, (230, 180, 70), top=style == 'sash', bottom=True)
        else:
            chitin(p, f, MANTIS_GREEN)
    return paint_box


# ── The Cockroach Son ────────────────────────────────────────────────────────

ROACH = (112, 56, 26)
ROACH_DARK = (44, 22, 12)
ROACH_PALE = (214, 176, 112)
IRON = (70, 66, 72)
ROACH_LEGS = [(-6, -0.65), (1, 0.08), (8, 0.8)]


def roach_parts():
    ps = [
        part('body', 'root', (0, 12.5, 0), (0, 0, 0), [
            (-8.5, -4.5, -10, 17, 8, 21, 'body'),
            (-7, 3, -8, 14, 1.6, 18, 'belly'),
        ]),
        part('right_elytron', 'body', (0, -4.6, -8.5), (0.02, 0, 0.04), [(-9.6, -1.8, 0, 9.6, 2.2, 21.5, 'elytron'),
                                                                        (-10.1, 0.0, 0.8, 0.9, 2.8, 20.0, 'elytron_flank'),
                                                                        (-8.2, -1.6, 21.3, 8.0, 1.8, 1.6, 'elytron_tip'),
                                                                        (-7.4, -3.2, 1.2, 7.4, 1.6, 19, 'elytron_crown'),
                                                                        (-4.2, -4.2, 2.6, 4.2, 1.2, 15.5, 'elytron_crest')]),
        part('pronotum', 'body', (0, -4.3, -9.5), (0.25, 0, 0), [
            (-9, -1.6, -7, 18, 3, 9, 'pronotum'),
            (-6.5, -2.8, -6, 13, 1.4, 7.4, 'pronotum_dome'),
            (-8.2, -2.2, -6.6, 16.4, 0.8, 8.4, 'pronotum_rim'),
            (-4.2, -3.6, -5.2, 8.4, 1.0, 5.6, 'pronotum_crown'),
        ]),
        part('head', 'body', (0, 0, -11), (0.35, 0, 0), [
            (-4.8, -3, -5, 9.6, 6, 5.2, 'head'),
            (-2.2, 2.2, -4.8, 4.4, 1.4, 1.4, 'mouthparts'),
        ]),
        part('right_eye', 'head', (-3.4, -1.2, -2.6), (0, 0.2, 0), [(-1.3, -1.6, -1.5, 2.2, 3.2, 3, 'compound')]),
        part('right_mandible', 'head', (-1.6, 2.2, -4.2), (0, 0.2, 0.1), [(-1.2, -0.6, -1.8, 2, 1.4, 2, 'mandible')]),
        part('right_antenna', 'head', (-2, -2, -4), (0.15, 0.55, 0), [(-0.3, -0.3, -12, 0.6, 0.6, 12, 'antenna')]),
        part('right_antenna_tip', 'right_antenna', (0, 0, -12), (-0.15, 0.25, 0), [(-0.25, -0.25, -12, 0.5, 0.5, 12, 'antenna')]),
        part('right_cercus', 'body', (-3, 0, 11), (0.3, -0.35, 0), [(-0.6, -0.6, 0, 1.2, 1.2, 5, 'cercus')]),
    ]
    for i, (z, yaw) in enumerate(ROACH_LEGS):
        ps += [
            part('right_hip_%d' % i, 'body', (-7.5, 2, z), (0, yaw, 0), []),
            part('right_leg_%d' % i, 'right_hip_%d' % i, (0, 0, 0), (0, 0, 0.32), [
                (-9, -1.3, -1.3, 9, 2.6, 2.6, 'femur'),
                (-6, -2.4, -0.4, 0.8, 1.2, 0.8, 'legspine'), (-3, -2.4, -0.4, 0.8, 1.2, 0.8, 'legspine'),
            ]),
            part('right_shin_%d' % i, 'right_leg_%d' % i, (-9, 0, 0), (0, 0, -1.55), [
                (-11, -0.9, -0.9, 11, 1.8, 1.8, 'tibia'),
                (-7, -1.8, -0.35, 0.7, 1.2, 0.7, 'legspine'), (-3.5, -1.8, -0.35, 0.7, 1.2, 0.7, 'legspine'),
                (-14, -0.6, -0.6, 3.4, 1.2, 1.2, 'tarsus'),
            ]),
        ]
    return mirrored(ps)


def roach_painter():
    def paint_box(p, style, f, dims):
        if style in ('body', 'belly'):
            chitin(p, f, ROACH_DARK if style == 'body' else (90, 50, 26), plate=3 if style == 'belly' else 0)
        elif style in ('elytron_crown', 'elytron_crest'):
            chitin(p, f, k(ROACH, 1.08), (255, 220, 170))
            for i, j, xy in each(f['top']):
                if i % 4 == 2:
                    p.px(xy, k(ROACH, 0.85))
        elif style == 'pronotum_dome':
            chitin(p, f, ROACH, (250, 210, 160))
            for s2 in ('top', 'front'):
                fx, fy, fw, fh = f[s2]
                for i, j, xy in each(f[s2]):
                    u = (i + 0.5) / fw
                    if abs(u - 0.3) < 0.12 or abs(u - 0.7) < 0.12:
                        p.px(xy, noisy(p, ROACH_DARK, 4))
        elif style in ('elytron', 'elytron_flank', 'elytron_tip'):
            # lacquer-glossy chestnut: deep at its margins, warming to amber along its crown, a hard white
            # gleam running its length, faint veins; the light catching the curve of each flank
            for s, r in f.items():
                fx, fy, fw, fh = r
                L = LIGHTS[s]
                for i, j, xy in each(r):
                    u, v = (i + 0.5) / fw, (j + 0.5) / fh
                    across = u if s in ('top', 'bottom', 'front', 'back') else v
                    warm = 1 - abs(across - 0.55) * 1.6 if s == 'top' else 0.25
                    c = mixc(ROACH_DARK, (176, 92, 36), max(0, warm))
                    if s == 'top' and abs(u - 0.62) < 0.05:
                        c = mixc(c, (255, 236, 200), 0.75)
                    elif s == 'top' and abs(u - 0.72) < 0.03:
                        c = mixc(c, (255, 236, 200), 0.35)
                    if s == 'top' and i % 5 == 1 and abs(u - 0.62) > 0.08:
                        c = k(c, 0.88)
                    if style == 'elytron_flank' and s in ('right', 'left'):
                        c = mixc(ROACH_DARK, (150, 80, 32), 0.4 + 0.4 * (1 - v))
                    p.px(xy, noisy(p, k(c, L), 3))
        elif style == 'pronotum':
            chitin(p, f, ROACH, (250, 210, 160))
            fx, fy, fw, fh = f['top']                                # a pale rim round its shield, two dark blotches on it
            for i, j, xy in each(f['top']):
                u, v = (i + 0.5) / fw, (j + 0.5) / fh
                if min(u, 1 - u) < 0.08 or v < 0.14:
                    p.px(xy, noisy(p, ROACH_PALE, 5))
                elif math.hypot((u - 0.5) * 2 - 0.35, v - 0.55) < 0.22 or math.hypot((u - 0.5) * 2 + 0.35, v - 0.55) < 0.22:
                    p.px(xy, noisy(p, ROACH_DARK, 4))
            for s in SIDES:
                for i, j, xy in each(f[s]):
                    p.px(xy, noisy(p, k(ROACH_PALE, LIGHTS[s] * 0.9), 4))
        elif style in ('pronotum_rim', 'pronotum_crown'):
            chitin(p, f, ROACH_PALE if style == 'pronotum_rim' else (150, 78, 30), (255, 236, 200))
            if style == 'pronotum_crown':
                fx, fy, fw, fh = f['top']
                for i, j, xy in each(f['top']):
                    u, v = (i + 0.5) / fw, (j + 0.5) / fh
                    if math.hypot((u - 0.5) * 2 - 0.3, v - 0.5) < 0.3 or math.hypot((u - 0.5) * 2 + 0.3, v - 0.5) < 0.3:
                        p.px(xy, noisy(p, ROACH_DARK, 3))
                    elif abs(u - 0.5) < 0.04:
                        p.px(xy, (240, 200, 150))
        elif style in ('spike', 'boss_plate'):
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, k(IRON if style == 'spike' else BRONZE, LIGHTS[s] * 1.1), 4))
        elif style in ('head', 'mouthparts'):
            chitin(p, f, (70, 36, 18))
        elif style == 'compound':
            compound_eye(p, f, (40, 30, 26), glow=0)
            fx, fy, fw, fh = f['front']
            for ex in range(1, fw - 1, 2):
                p.px((fx + ex, fy + 2), (230, 120, 40), glow=True)    # an ember glint in its eyes
        elif style == 'mandible':
            chitin(p, f, (40, 20, 12), (180, 150, 110))
        elif style in ('antenna', 'cercus'):
            chitin(p, f, (90, 46, 22), gloss=False)
            for s in SIDES:
                for i, j, xy in each(f[s]):
                    if (j if s in ('right', 'left') else i) % 3 == 0:
                        p.px(xy, (50, 26, 14))
        elif style in ('femur', 'tibia', 'tarsus'):
            chitin(p, f, (96, 50, 24), (220, 180, 130))
        elif style == 'legspine':
            for s, r in f.items():
                for i, j, xy in each(r):
                    p.px(xy, noisy(p, (40, 24, 16), 3))
        else:
            chitin(p, f, ROACH)
    return paint_box


def save(name, parts, painter, tag, seed):
    p = SilkPainter(seed)
    slots = paint(parts, p, painter)
    p.save(name)
    emit(parts, slots, JAVA, tag)
    return slots


if __name__ == '__main__':
    sp = spider_parts()
    save('spider_demon', sp, spider_painter(), 'spider', 11)
    print('spider demon', len(sp), 'parts', {a: round(b, 2) for a, b in extent(sp, 1.0).items()})
    st = stinger_parts()
    for i, name in enumerate(STINGERS):
        p = SilkPainter(20 + i)
        slots = paint(st, p, stinger_painter(name))
        p.save(name)
    emit(st, slots, JAVA, 'stinger')
    print('stinging son', len(st), 'parts', {a: round(b, 2) for a, b in extent(st, 1.0).items()})
    mt = mantis_parts()
    bg.UNITS, bg.TEX = 256, 256 * bg.S                       # the mantis, heavier and more detailed, on a larger skin
    save('mantis_son', mt, mantis_painter(), 'mantis', 31)
    bg.UNITS, bg.TEX = 128, 128 * bg.S
    print('mantis son', len(mt), 'parts', {a: round(b, 2) for a, b in extent(mt, 1.0).items()})
    rc = roach_parts()
    save('cockroach_son', rc, roach_painter(), 'roach', 41)
    print('cockroach son', len(rc), 'parts', {a: round(b, 2) for a, b in extent(rc, 1.0).items()})
