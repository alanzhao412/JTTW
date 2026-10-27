"""The folk of the Tang villages and the supernatural world's merchants (tang_folk/*.png). As
fire_court.py: one list of parts (beastgen) gives both the model (written into TangFolkRenderer's
layer() between its GENERATED markers) and the skins, painted to the same box-UV layout at twice the
model's resolution. Every piece of dress any of them wears is in the one frame; each look shows its
own (the LOOKS table, written into the renderer too).

The commoners the pilgrims meet in the novel, in Tang dress (seventh century):
- men in the round-collar robe (圆领袍) belted at the waist, its hem to the calf, boots below; on the
  head the black futou (幞头), a cloth cap tied over a topknot, its two ties hanging behind;
- women in ru-qun: a short jacket (襦) with a crossed collar over a skirt tied high under the bust,
  falling to the ground; a shawl (披帛) over the shoulders; the hair dressed high, a pin through it, a
  red flower-mark (花钿) on the brow;
- the farmer in undyed hemp, his trousers rolled, a conical straw hat (斗笠); the monk shaven, a grey
  robe, a russet kasaya across it and a string of beads; the smith in a headband and leather apron;
  the fortune teller in a long blue robe and square cap, a thin beard;
- the ghost merchant pale as paper, in funeral white, floating, a white paper lantern in his hand;
  the celestial merchant in white and gold, a gold crown on his topknot, a long white beard.

Run from the repository root: python assets_source/tang_folk.py"""
import math
import os

import beastgen as bg
from beastgen import Painter, paint, faces
from fire_court import part, mirrored

bg.UNITS = 128
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/TangFolkRenderer.java'
OUT = 'src/main/resources/assets/jttw_engine/textures/entity/tang_folk/'

# ── The frame ─────────────────────────────────────────────────────────────────
# a player's proportions: head 8, body 12, arms and legs 12; the ground at y 24, the face toward -z
FRAME = mirrored([
    part('head', 'root', (0, 0, 0), (0, 0, 0), [(-4, -8, -4, 8, 8, 8, 'face')]),
    # hats and hair
    part('futou', 'head', (0, 0, 0), (0, 0, 0), [(-4.4, -8.7, -4.4, 8.8, 3.6, 8.8, 'futou'), (-2.4, -11.2, -1.2, 4.8, 3, 4.4, 'futou_knob')]),
    part('futou_ties', 'head', (0, -6.5, 4.2), (0.22, 0, 0), [(-2.6, 0, -0.25, 1.3, 7.5, 0.5, 'tie'), (1.3, 0, -0.25, 1.3, 7.5, 0.5, 'tie')]),
    part('straw_hat', 'head', (0, 0, 0), (0, 0, 0), [(-8, -8.6, -8, 16, 1, 16, 'straw_brim'), (-5.5, -9.8, -5.5, 11, 1.4, 11, 'straw_mid'),
                                                     (-3.5, -11.1, -3.5, 7, 1.4, 7, 'straw_top'), (-1.5, -12.2, -1.5, 3, 1.2, 3, 'straw_tip')]),
    part('headcloth', 'head', (0, 0, 0), (0, 0, 0), [(-4.35, -8.6, -4.35, 8.7, 3.2, 8.7, 'cloth'), (-1.2, -7.8, 4.2, 2.4, 2.4, 1.6, 'cloth_knot')]),
    part('square_cap', 'head', (0, 0, 0), (0, 0, 0), [(-4.45, -12, -4.45, 8.9, 6.2, 8.9, 'cap')]),
    part('bun', 'head', (0, 0, 0), (0, 0, 0), [(-2.2, -12.6, -1.2, 4.4, 4.8, 4.4, 'bun'), (-4.3, -9, -3.6, 8.6, 1.4, 8, 'hair_band')]),
    part('hairpin', 'head', (0, 0, 0), (0, 0, 0), [(-4.6, -11.2, 0.6, 9.2, 0.7, 0.7, 'pin'), (3.6, -11.6, 0.3, 1.4, 1.4, 1.4, 'pin_head')]),
    part('topknot', 'head', (0, 0, 0), (0, 0, 0), [(-1.6, -11, -1.6, 3.2, 3.2, 3.2, 'knot')]),
    part('crown', 'head', (0, 0, 0), (0, 0, 0), [(-2.4, -10.4, -2.4, 4.8, 2, 4.8, 'crown'), (-0.5, -12.4, -0.5, 1, 2.2, 1, 'crown_jewel')]),
    part('beard', 'head', (0, -1.6, -4.25), (0.12, 0, 0), [(-1.8, 0, -0.4, 3.6, 4.5, 0.8, 'beard')]),
    part('long_beard', 'head', (0, -1.6, -4.25), (0.08, 0, 0), [(-2.4, 0, -0.5, 4.8, 8, 1, 'long_beard')]),
    # the body and what is worn on it
    part('body', 'root', (0, 0, 0), (0, 0, 0), [(-4, 0, -2, 8, 12, 4, 'torso')]),
    part('robe_skirt', 'body', (0, 0, 0), (0, 0, 0), [(-4.6, 11, -2.6, 9.2, 10, 5.2, 'skirt')]),
    part('long_skirt', 'body', (0, 0, 0), (0, 0, 0), [(-4.6, 8.5, -2.6, 9.2, 12, 5.2, 'gown'), (-5.4, 20, -3.3, 10.8, 4, 6.6, 'gown_hem')]),
    part('belt', 'body', (0, 0, 0), (0, 0, 0), [(-4.35, 10, -2.35, 8.7, 1.4, 4.7, 'belt')]),
    part('sash', 'body', (0, 0, 0), (0, 0, 0), [(-4.4, 7.6, -2.4, 8.8, 1.3, 4.8, 'sash')]),
    part('apron', 'body', (0, 11, -2.7), (-0.06, 0, 0), [(-3.6, -1.5, -0.4, 7.2, 10.5, 0.6, 'apron')]),
    part('right_shawl', 'body', (-3.2, 0.4, 0), (0, 0, 0.12), [(-1.4, -0.7, -2.6, 2.8, 1.1, 5.2, 'shawl'), (-1.5, 0, -2.75, 1.2, 15, 0.5, 'shawl_fall')]),
    # the arms, a wide sleeve's cuff at the end of each
    part('right_arm', 'root', (-5, 2, 0), (0, 0, 0), [(-3, -2, -2, 4, 12, 4, 'arm')]),
    part('right_cuff', 'right_arm', (0, 0, 0), (0, 0, 0), [(-3.7, 4.5, -2.7, 5.4, 6.2, 5.4, 'sleeve')]),
    part('lantern', 'left_arm', (1, 10, 0), (0, 0, 0), [(-0.4, 0, -0.4, 0.8, 3, 0.8, 'lantern_cord'), (-2.2, 2.5, -2.2, 4.4, 5.5, 4.4, 'lantern')]),
    part('right_leg', 'root', (-1.9, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 12, 4, 'leg')]),
])
# the lantern hangs from the left hand only
FRAME = [p for p in FRAME if p[0] != 'right_lantern']
LEFT_ARM_LANTERN = [p for p in FRAME if p[0] == 'lantern']

DRESS = ['futou', 'futou_ties', 'straw_hat', 'headcloth', 'square_cap', 'bun', 'hairpin', 'topknot', 'crown', 'beard', 'long_beard',
         'robe_skirt', 'long_skirt', 'belt', 'sash', 'apron', 'right_shawl', 'left_shawl', 'right_cuff', 'left_cuff', 'lantern']

MAN_ROBE = ['futou', 'futou_ties', 'robe_skirt', 'belt', 'right_cuff', 'left_cuff']
WOMAN = ['bun', 'hairpin', 'long_skirt', 'sash', 'right_shawl', 'left_shawl', 'right_cuff', 'left_cuff']
LOOKS = {
    'farmer': ['straw_hat', 'robe_skirt', 'belt'],
    'farmer_woman': ['headcloth', 'long_skirt', 'sash', 'apron'],
    'monk': ['long_skirt', 'right_cuff', 'left_cuff', 'sash'],
    'scholar': MAN_ROBE,
    'merchant': MAN_ROBE,
    'merchant_woman': WOMAN,
    'innkeeper': ['headcloth', 'robe_skirt', 'belt', 'apron'],
    'innkeeper_woman': ['bun', 'hairpin', 'long_skirt', 'sash', 'apron', 'right_cuff', 'left_cuff'],
    'herbalist': MAN_ROBE + ['beard'],
    'herbalist_woman': WOMAN,
    'fortune_teller': ['square_cap', 'beard', 'long_skirt', 'sash', 'right_cuff', 'left_cuff'],
    'smith': ['headcloth', 'robe_skirt', 'belt', 'apron'],
    'ghost_merchant': ['futou', 'futou_ties', 'long_skirt', 'sash', 'right_cuff', 'left_cuff', 'lantern'],
    'celestial_merchant': ['topknot', 'crown', 'long_beard', 'long_skirt', 'sash', 'right_shawl', 'left_shawl', 'right_cuff', 'left_cuff'],
}

# ── Colours ───────────────────────────────────────────────────────────────────
SKIN = (226, 190, 156)
SKIN_WOMAN = (238, 206, 178)
SKIN_SUN = (196, 150, 110)
SKIN_GHOST = (196, 214, 214)
HAIR = (26, 22, 22)
GREY_HAIR = (150, 146, 140)
WHITE_HAIR = (236, 236, 240)
BLACK_CLOTH = (30, 28, 32)
HEMP = (176, 160, 128)
HEMP_DARK = (120, 104, 80)
STRAW = (212, 176, 98)
BOOT = (44, 34, 28)
GOLD = (228, 186, 80)


def look(robe, trim, under=None, skin=SKIN, hair=HAIR, belt=(40, 30, 26), hat=BLACK_CLOTH, apron=(236, 232, 220), legs=None, boots=BOOT,
         collar='round', woman=False, beard=HAIR, face='man', **more):
    d = dict(robe=robe, trim=trim, under=under or robe, skin=skin, hair=hair, belt=belt, hat=hat, apron=apron, legs=legs or (60, 52, 44),
             boots=boots, collar=collar, woman=woman, beard=beard, face=face)
    d.update(more)
    return d


PALETTES = {
    'farmer': look(HEMP, HEMP_DARK, skin=SKIN_SUN, belt=(90, 70, 40), legs=(130, 116, 90), boots=(120, 92, 60), collar='cross', face='weathered',
                   rolled=True),
    'farmer_woman': look((96, 120, 150), (60, 74, 100), under=(150, 112, 80), skin=SKIN_WOMAN, hat=(92, 128, 160), apron=(80, 96, 120),
                         collar='cross', woman=True, face='woman', skirt=(150, 112, 80), sash=(60, 74, 100)),
    'monk': look((128, 126, 120), (90, 88, 84), skin=SKIN, hair=None, collar='cross', face='monk', skirt=(118, 116, 110),
                 kasaya=(160, 74, 44), sash=(160, 74, 44), beads=(80, 40, 26)),
    'scholar': look((62, 112, 84), (36, 70, 52), face='young', belt=(30, 26, 24)),
    'merchant': look((138, 92, 52), (96, 60, 34), face='plump', belt=(60, 40, 22), purse=(170, 40, 40)),
    'merchant_woman': look((214, 120, 128), (160, 70, 80), skin=SKIN_WOMAN, woman=True, face='woman', collar='cross', skirt=(176, 40, 56),
                           sash=(240, 210, 120), shawl=(240, 220, 160)),
    'innkeeper': look((96, 72, 54), (64, 48, 36), hat=(232, 228, 216), face='plump', apron=(236, 232, 220), collar='cross'),
    'innkeeper_woman': look((120, 70, 110), (80, 44, 74), skin=SKIN_WOMAN, woman=True, face='woman', collar='cross', skirt=(64, 70, 110),
                            sash=(220, 190, 110), apron=(236, 232, 220)),
    'herbalist': look((92, 108, 120), (56, 68, 80), face='old', hair=GREY_HAIR, beard=GREY_HAIR, belt=(40, 36, 30)),
    'herbalist_woman': look((120, 150, 112), (76, 104, 70), skin=SKIN_WOMAN, woman=True, face='woman', collar='cross', skirt=(66, 96, 72),
                            sash=(210, 196, 150), shawl=(226, 236, 210)),
    'fortune_teller': look((58, 74, 130), (34, 44, 84), hat=BLACK_CLOTH, face='sly', beard=(40, 36, 34), collar='cross',
                           skirt=(52, 66, 118), sash=(34, 44, 84), taiji=True),
    'smith': look((88, 70, 56), (60, 48, 38), skin=SKIN_SUN, hat=(150, 50, 40), apron=(100, 70, 44), legs=(70, 60, 50), face='strong',
                  collar='cross', rolled=True, belt=(40, 30, 24)),
    'ghost_merchant': look((226, 226, 218), (150, 150, 156), skin=SKIN_GHOST, face='ghost', hat=BLACK_CLOTH, collar='cross',
                           skirt=(210, 212, 206), sash=(120, 120, 130), lantern=(244, 240, 226)),
    'celestial_merchant': look((240, 238, 230), GOLD, skin=(240, 220, 196), hair=WHITE_HAIR, beard=WHITE_HAIR, face='immortal', collar='cross',
                               skirt=(236, 234, 226), sash=GOLD, shawl=(186, 214, 236), clouds=True),
}


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def dk(c, k):
    return tuple(v * k for v in c[:3])


# ── Painting ──────────────────────────────────────────────────────────────────

def cloth(p, f, base, trim=None, hem=0, fold=True, light=1.0):
    """Cloth over a box: lit above, its sides falling into shade, soft vertical folds, a trimmed hem."""
    for k, r in f.items():
        k_light = {'top': 1.08, 'bottom': 0.62, 'front': 1.0, 'back': 0.86, 'right': 0.92, 'left': 0.92}[k] * light
        fx, fy, fw, fh = r
        for j in range(fh):
            for i in range(fw):
                t = j / max(1, fh - 1)
                fl = 1.0
                if fold and k not in ('top', 'bottom'):
                    fl = 1 + 0.06 * math.sin(i * 1.3 + (j * 0.15)) - 0.04 * (i % 5 == 0)
                c = dk(base, k_light * fl * (1 - 0.12 * t))
                n = p.rnd.randint(-3, 3)
                p.px((fx + i, fy + j), [v + n for v in c])
        if trim and hem and k not in ('top', 'bottom'):
            for j in range(fh - hem, fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), dk(trim, k_light * (0.95 + p.rnd.random() * 0.08)))


def paint_face(p, f, c):
    """The head: its face (16 x 16 pixels) by kind, the hair over its crown and back."""
    skin, hair, kind = c['skin'], c['hair'], c['face']
    for k, r in f.items():
        light = {'top': 1.05, 'bottom': 0.7, 'front': 1.0, 'back': 0.9, 'right': 0.94, 'left': 0.94}[k]
        p.face(r, skin, light, noise=3)
    fx, fy, fw, fh = f['front']
    if hair is not None:
        # hair: the crown, the back, the sides; a parting line at the brow
        for k in ('top', 'back'):
            p.face(f[k], hair, 1.0 if k == 'top' else 0.92, noise=4)
        for k in ('right', 'left'):
            sx, sy, sw, sh = f[k]
            for j in range(sh):
                for i in range(sw):
                    if j < 7 or (k == 'right' and i < sw - 4) or (k == 'left' and i >= 4) or (c['woman'] and j < 11):
                        p.px((sx + i, sy + j), dk(hair, 0.95 + p.rnd.random() * 0.1))
        top = 4 if c['woman'] else 3
        for i in range(fw):
            for j in range(top + (1 if c['woman'] and (i < 3 or i > fw - 4) else 0)):
                p.px((fx + i, fy + j), dk(hair, 1.0 + p.rnd.random() * 0.08))
        if c['woman']:
            # side locks framing the face
            for j in range(top, top + 6):
                p.px((fx, fy + j), hair)
                p.px((fx + fw - 1, fy + j), hair)
    else:
        # the shaven head: a shadow of stubble, and the monk's ordination marks
        for k in ('top', 'back', 'right', 'left'):
            r = f[k]
            for j in range(r[3]):
                for i in range(r[2]):
                    if k != 'top' and j > 7:
                        continue
                    if p.rnd.random() < 0.35:
                        p.px((r[0] + i, r[1] + j), mix(skin, (110, 100, 96), 0.35))
        tx, ty, tw, th = f['top']
        for dx, dy in ((5, 5), (8, 5), (11, 5), (6, 8), (10, 8), (8, 11)):
            p.px((tx + dx, ty + dy), dk(skin, 0.78))
    eye_dark = (24, 18, 18)
    brow = dk(hair or (60, 50, 44), 0.9) if kind not in ('old', 'immortal') else (c['beard'] if kind == 'immortal' else GREY_HAIR)
    ey = 8
    if kind == 'ghost':
        # hollow, shadowed eyes, a gaunt pallor, a thin dark mouth
        for ex in (3, 10):
            for i in range(3):
                for j in range(3):
                    p.px((fx + ex + i, fy + ey - 1 + j), (70, 90, 96))
            p.px((fx + ex + 1, fy + ey), (230, 250, 250))
        for i in range(5, 11):
            p.px((fx + i, fy + 13), (60, 70, 76))
        for j in range(11, 13):
            p.px((fx + 3, fy + j), dk(skin, 0.86))
            p.px((fx + 12, fy + j), dk(skin, 0.86))
        return
    # brows
    for ex, slant in ((2, 1), (10, -1)):
        for i in range(4):
            dy = 0 if kind != 'sly' else (i // 2) * slant
            if c['woman']:
                dy = 0 if i in (1, 2) else 1
            if kind == 'strong':
                dy = (i // 2) * -slant
            p.px((fx + ex + i, fy + ey - 2 + dy), brow)
    # eyes: the almond eye, a dark iris, a catch of light
    closed = kind in ('monk', 'immortal')
    for ex in (3, 9):
        if closed:
            for i in range(4):
                p.px((fx + ex + i, fy + ey + (1 if i in (1, 2) else 0)), eye_dark)
            continue
        # the almond eye: white either side of a dark iris two high, the lid's line over it
        for i in range(4):
            p.px((fx + ex + i, fy + ey - 1), dk(skin, 0.72) if 0 < i < 3 else skin)
        iris = 1 if ex < 8 else 2
        for i in range(4):
            p.px((fx + ex + i, fy + ey), (40, 28, 24) if i in (iris, iris + 0) else (244, 240, 232) if 0 < i < 3 or True else skin)
            p.px((fx + ex + i, fy + ey + 1), (40, 28, 24) if i == iris else dk(skin, 0.92))
        p.px((fx + ex + iris, fy + ey), (24, 16, 14))
        p.px((fx + ex, fy + ey + 1), skin)
        p.px((fx + ex + 3, fy + ey + 1), skin)
    # the nose: a shadow down its side, its tip
    p.px((fx + 7, fy + 10), dk(skin, 0.84))
    p.px((fx + 7, fy + 11), dk(skin, 0.8))
    p.px((fx + 8, fy + 11), dk(skin, 0.9))
    # the mouth
    lip = (196, 64, 64) if c['woman'] else dk(skin, 0.62)
    mw = 2 if c['woman'] else 4
    mx = 8 - mw // 2
    for i in range(mw):
        p.px((fx + mx + i, fy + 13), lip)
    if c['woman']:
        # a touch of rouge, and the flower-mark (花钿) between the brows
        for cx in (3, 12):
            p.px((fx + cx, fy + 11), mix(skin, (236, 150, 140), 0.45))
        p.px((fx + 7, fy + 5), (210, 40, 50))
        p.px((fx + 8, fy + 5), (210, 40, 50))
    if kind in ('old', 'weathered', 'strong'):
        # the lines of age and weather
        for ex in (2, 12):
            p.px((fx + ex, fy + 9), dk(skin, 0.85))
        p.px((fx + 5, fy + 12), dk(skin, 0.88))
        p.px((fx + 10, fy + 12), dk(skin, 0.88))
    if kind == 'plump':
        for cx in (3, 12):
            p.px((fx + cx, fy + 11), (230, 168, 140))
        for i in range(5, 11):
            p.px((fx + i, fy + 15), dk(skin, 0.9))
    if kind in ('herbalist', 'old', 'sly', 'immortal', 'plump', 'strong', 'weathered'):
        # a moustache
        mc = c['beard']
        if kind in ('old', 'sly', 'immortal'):
            for i in (4, 5, 6, 9, 10, 11):
                p.px((fx + i, fy + 12 + (1 if i in (4, 11) else 0)), mc)
        elif kind == 'strong':
            for i in range(4, 12):
                if p.rnd.random() < 0.5:
                    p.px((fx + i, fy + 14 + p.rnd.randint(0, 1)), dk(c['hair'] or HAIR, 1.2))


def paint_torso(p, f, c):
    robe, trim = c['robe'], c['trim']
    cloth(p, f, robe)
    fx, fy, fw, fh = f['front']        # 16 x 24
    bx, by, bw, bh = f['back']
    if c['collar'] == 'round':
        # the round collar (圆领): a band of the robe's trim round the neck, a white under-collar
        # showing inside it, the robe fastened at the right shoulder, its seam running down there
        cx = (fw - 1) / 2
        for i in range(fw):
            for j in range(6):
                d = math.hypot((i - cx) / 1.25, j + 1.5)
                if 3.6 <= d <= 5.4:
                    p.px((fx + i, fy + j), dk(trim, 0.95 + 0.1 * (j == 0)))
                elif d < 3.6 and j < 3:
                    p.px((fx + i, fy + j), (238, 234, 222))
        for j in range(2, 9):
            p.px((fx + 3 - (j - 2) // 3, fy + j), dk(trim, 0.85))
        p.px((fx + 2, fy + 4), GOLD)
        for i in range(bw):
            p.px((bx + i, by), dk(trim, 0.9))
            p.px((bx + i, by + 1), dk(trim, 0.8))
    else:
        # a crossed collar: the left lapel over the right, a broad band of trim
        for j in range(min(fh, 14)):
            for w in range(3):
                x1 = fw // 2 - 1 - j // 2 + w + (fw // 2 - 2)
                x1 = fw - 1 - (j * 6 // 10) - w - 2
                x2 = (j * 6 // 10) + w + 2
                if j < 9:
                    p.px((fx + x2, fy + j), dk(trim, 1.0 if w else 0.85))
                if j < 12:
                    p.px((fx + x1, fy + j), dk(trim, 1.0 if w else 0.85))
        p.px((fx + fw // 2, fy + 1), dk(robe, 0.7))
        for i in range(bw):
            p.px((bx + i, by), dk(trim, 0.9))
    if c.get('kasaya'):
        # the kasaya, russet, across the body from the left shoulder, its patchwork lines
        k = c['kasaya']
        for j in range(fh):
            for w in range(6):
                x = j * 2 // 3 + w - 1
                if 0 <= x < fw:
                    p.px((fx + x, fy + j), dk(k, 0.95 + ((x + j) % 6 == 0) * -0.25))
        for j in range(bh):
            for w in range(6):
                x = bw - 1 - (j * 2 // 3 + w - 1)
                if 0 <= x < bw:
                    p.px((bx + x, by + j), dk(k, 0.86))
        # the beads round the neck
        for i in range(fw):
            d = abs(i - (fw - 1) / 2)
            j = int(3 + 0.09 * d * d)
            if i % 2 == 0 and j < fh:
                p.px((fx + i, fy + j), c['beads'])
    if c.get('taiji'):
        # a taiji worked on the fortune teller's back
        cx, cy = bx + bw // 2, by + 9
        for j in range(-4, 5):
            for i in range(-4, 5):
                if i * i + j * j <= 16:
                    yang = (i > 0) if abs(j) > 2 else (math.atan2(j, i) > 0)
                    p.px((cx + i, cy + j), (236, 232, 220) if yang else (24, 24, 30))
    if c.get('clouds'):
        for r in (f['front'], f['back']):
            for j in range(4, r[3] - 2, 5):
                for i in range((j * 3) % 7, r[2] - 2, 7):
                    p.px((r[0] + i, r[1] + j), GOLD)
                    p.px((r[0] + i + 1, r[1] + j), GOLD)
                    p.px((r[0] + i, r[1] + j + 1), dk(GOLD, 0.8))
    if c['woman']:
        # the jacket's hem under the high skirt line is hidden; the bust-line ribbon is the sash
        pass


def paint_skirt(p, f, c, base, trim, gown=False, hem_rows=2):
    cloth(p, f, base, trim, hem=hem_rows)
    if gown:
        # pleats down the skirt
        for k in ('front', 'back', 'right', 'left'):
            r = f[k]
            for i in range(1, r[2], 3):
                for j in range(r[3] - hem_rows):
                    p.px((r[0] + i, r[1] + j), dk(base, 0.84 + 0.04 * math.sin(j * 0.4)))
    else:
        # the robe's side slit, a darker seam
        for k in ('right', 'left'):
            r = f[k]
            for j in range(r[3] // 2, r[3]):
                p.px((r[0] + r[2] // 2, r[1] + j), dk(base, 0.55))
    if c.get('kasaya') and gown:
        r = f['front']
        for j in range(r[3]):
            for w in range(6):
                x = r[2] - 1 - (j // 2 + w)
                if 0 <= x < r[2] and j < r[3] - 3:
                    p.px((r[0] + x, r[1] + j), dk(c['kasaya'], 0.9))
    if c.get('clouds'):
        for k in ('front', 'back', 'right', 'left'):
            r = f[k]
            for i in range(0, r[2], 4):
                p.px((r[0] + i, r[1] + r[3] - hem_rows - 2), GOLD)
                p.px((r[0] + i + 1, r[1] + r[3] - hem_rows - 3), GOLD)


def paint_box(p, style, f, dims, c):
    robe, trim = c['robe'], c['trim']
    skirt = c.get('skirt', robe)
    if style == 'face':
        paint_face(p, f, c)
    elif style == 'torso':
        paint_torso(p, f, c)
    elif style == 'skirt':
        paint_skirt(p, f, c, robe, trim, hem_rows=2)
    elif style in ('gown', 'gown_hem'):
        paint_skirt(p, f, c, skirt, dk(skirt, 0.75) if not c.get('clouds') else GOLD, gown=True, hem_rows=2 if style == 'gown_hem' else 0)
    elif style == 'arm':
        # the sleeve to the wrist, the hand below it (tight sleeves rolled to the elbow for working men)
        rolled = c.get('rolled')
        for k, r in f.items():
            light = {'top': 1.05, 'bottom': 0.8, 'front': 1.0, 'back': 0.88, 'right': 0.92, 'left': 0.92}[k]
            rx, ry, rw, rh = r
            for j in range(rh):
                for i in range(rw):
                    if k == 'bottom' or (k != 'top' and j >= rh - 4) or (rolled and k != 'top' and j >= rh // 2):
                        col = dk(c['skin'], light)
                    else:
                        col = dk(robe, light * (1 - 0.1 * j / max(1, rh)))
                    if rolled and k != 'top' and rh // 2 - 2 <= j < rh // 2:
                        col = dk(robe, light * 0.8)
                    p.px((rx + i, ry + j), [v + p.rnd.randint(-3, 3) for v in col])
    elif style == 'sleeve':
        cloth(p, f, robe, trim, hem=2)
        r = f['bottom']
        p.face(r, dk(robe, 0.35), noise=2)
    elif style == 'leg':
        legs, boots = c['legs'], c['boots']
        for k, r in f.items():
            light = {'top': 1.0, 'bottom': 0.7, 'front': 1.0, 'back': 0.88, 'right': 0.92, 'left': 0.92}[k]
            rx, ry, rw, rh = r
            for j in range(rh):
                for i in range(rw):
                    boot = k == 'bottom' or j >= rh - (8 if not c.get('rolled') else 3)
                    col = boots if boot else legs
                    if c.get('rolled') and not boot and j >= rh - 9:
                        col = c['skin'] if k != 'top' else legs
                    p.px((rx + i, ry + j), [v * light + p.rnd.randint(-3, 3) for v in col])
    elif style in ('futou', 'futou_knob'):
        cloth(p, f, c['hat'], fold=False)
    elif style == 'tie':
        for r in f.values():
            p.gradient(r, c['hat'], dk(c['hat'], 0.6))
    elif style.startswith('straw'):
        for k, r in f.items():
            rx, ry, rw, rh = r
            for j in range(rh):
                for i in range(rw):
                    weave = ((i + j) % 3 == 0) * 0.86 + ((i + j) % 3 != 0)
                    rings = 0.92 if (int(math.hypot(i - rw / 2, j - rh / 2)) % 3 == 0 and k in ('top', 'bottom')) else 1
                    light = 1.08 if k == 'top' else 0.62 if k == 'bottom' else 0.9
                    p.px((rx + i, ry + j), [v * weave * rings * light + p.rnd.randint(-5, 5) for v in STRAW])
    elif style in ('cloth', 'cloth_knot'):
        cloth(p, f, c['hat'], fold=False)
        if style == 'cloth':
            r = f['front']
            for i in range(r[2]):
                p.px((r[0] + i, r[1] + r[3] - 1), dk(c['hat'], 0.8))
    elif style == 'cap':
        cloth(p, f, c['hat'], fold=False)
        r = f['front']
        for i in range(r[2]):
            p.px((r[0] + i, r[1] + r[3] - 2), (70, 70, 80))
        for j in range(r[3]):
            p.px((r[0] + r[2] // 2, r[1] + j), dk(c['hat'], 0.7))
    elif style in ('bun', 'hair_band', 'knot'):
        cloth(p, f, c['hair'] or HAIR, fold=False, light=1.05)
        if style == 'bun':
            for r in f.values():
                for j in range(0, r[3], 3):
                    for i in range(r[2]):
                        p.px((r[0] + i, r[1] + j), dk(c['hair'] or HAIR, 1.5))
    elif style in ('pin', 'pin_head'):
        for r in f.values():
            p.face(r, GOLD if style == 'pin' else (210, 50, 60), 1.0, noise=6)
    elif style in ('crown', 'crown_jewel'):
        for r in f.values():
            p.face(r, GOLD if style == 'crown' else (110, 196, 160), 1.0, noise=6)
        if style == 'crown':
            r = f['front']
            for i in range(0, r[2], 2):
                p.px((r[0] + i, r[1]), (250, 226, 130))
    elif style in ('beard', 'long_beard'):
        for r in f.values():
            rx, ry, rw, rh = r
            for j in range(rh):
                for i in range(rw):
                    taper = abs(i - rw / 2) / (rw / 2)
                    if style == 'long_beard' and j > rh * 0.6 and taper > 1.2 - j / rh:
                        continue
                    p.px((rx + i, ry + j), dk(c['beard'], 0.92 + 0.1 * ((i + j) % 2)))
    elif style == 'belt':
        cloth(p, f, c['belt'], fold=False)
        r = f['front']
        p.px((r[0] + r[2] // 2, r[1] + 1), GOLD)
        p.px((r[0] + r[2] // 2 + 1, r[1] + 1), GOLD)
        if c.get('purse'):
            for j in range(r[3]):
                p.px((r[0] + 4, r[1] + j), c['purse'])
    elif style == 'sash':
        cloth(p, f, c.get('sash', trim), fold=False)
        r = f['front']
        # its knot, the ends of the bow
        p.px((r[0] + r[2] // 2, r[1]), dk(c.get('sash', trim), 1.2))
    elif style == 'apron':
        cloth(p, f, c['apron'], fold=True)
        r = f['front']
        for i in range(r[2]):
            p.px((r[0] + i, r[1]), dk(c['apron'], 0.8))
    elif style in ('shawl', 'shawl_fall'):
        cloth(p, f, c.get('shawl', (230, 220, 200)), fold=False, light=1.04)
    elif style == 'lantern_cord':
        for r in f.values():
            p.face(r, (40, 34, 30), 1.0, noise=2)
    elif style == 'lantern':
        # a white paper lantern, ribbed, its paper lit from within
        lc = c.get('lantern', (244, 240, 226))
        for k, r in f.items():
            rx, ry, rw, rh = r
            for j in range(rh):
                for i in range(rw):
                    rib = (j % 3 == 0)
                    col = dk(lc, 0.75) if rib else lc
                    if k in ('top', 'bottom'):
                        col = (40, 34, 30)
                    p.px((rx + i, ry + j), col, glow=k not in ('top', 'bottom'))
        r = f['front']
        for j in range(2, r[3] - 2):
            p.px((r[0] + r[2] // 2, r[1] + j), (40, 30, 30), glow=True)


def main():
    slots = bg.layout(FRAME)
    os.makedirs(OUT, exist_ok=True)
    for name, colours in PALETTES.items():
        p = Painter(sum(map(ord, name)))
        for (w, h, d, style), (u, v) in slots.items():
            paint_box(p, style, faces(u, v, w, h, d), (w, h, d), colours)
        p.img.save(OUT + name + '.png')
        print('wrote', OUT + name + '.png')
    bg.emit(FRAME, slots, JAVA)
    # the looks, into the renderer's table
    lines = ['        DRESS = List.of(%s);' % ', '.join('"%s"' % n for n in DRESS)]
    for name, shown in LOOKS.items():
        lines.append('        LOOKS.put("%s", Set.of(%s));' % (name, ', '.join('"%s"' % n for n in shown)))
    s = open(JAVA, encoding='utf-8').read()
    a = s.index('        // GENERATED looks: begin') + len('        // GENERATED looks: begin')
    b = s.index('        // GENERATED looks: end')
    open(JAVA, 'w', encoding='utf-8', newline='\n').write(s[:a] + '\n' + '\n'.join(lines) + '\n' + s[b:])


main()
