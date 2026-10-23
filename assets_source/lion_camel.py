"""Lion-Camel Ridge's creatures: the Lion-Demon, the Elephant-Demon, the Peng-Demon, the Little
Wind-Driller, the Azure Lion and the White Elephant. As fire_court.py: each one's model and skin come
from one list of parts (beastgen): the model is written into its renderer's layer() between its
GENERATED markers, the skin painted to the same box-UV layout at twice the model's resolution, with
a second, glowing skin (eyes, gold) drawn full-bright.

Run from the repository root: python assets_source/lion_camel.py"""
import math

import beastgen as bg
from beastgen import Painter, paint, emit
from fire_court import part, mirrored, lamellar, eye

R = 'src/main/java/com/jttw/engine/client/renderer/'
DARK = (20, 16, 18)
GOLD = (222, 176, 64)
GOLD_DEEP = (160, 112, 40)
IVORY = (238, 230, 206)
BRONZE = (156, 104, 52)
STEEL = (196, 202, 212)


def fur(p, rect, base, dark=None, streak=0.25, noise=10, vertical=True):
    """Fur: a base tone in uneven streaks running down (or along) the face."""
    fx, fy, fw, fh = rect
    dark = dark or [c * 0.7 for c in base]
    for i in range(fw):
        col = p.rnd.random()
        for j in range(fh):
            t = (col + 0.15 * math.sin(j * 0.9 + i)) if vertical else (p.rnd.random())
            k = 1.0 - 0.22 * (j / max(1, fh - 1))
            c = dark if t < streak else base
            n = p.rnd.randint(-noise, noise)
            p.px((fx + i, fy + j), [v * k + n for v in c])


def shade_fur(p, f, base, dark=None, streak=0.25):
    for k, r in f.items():
        light = {'top': 1.08, 'bottom': 0.72, 'front': 1.0, 'back': 0.86}.get(k, 0.94)
        fur(p, r, [c * light for c in base], [c * light for c in (dark or [v * 0.7 for v in base])], streak)


def hide(p, f, base, wrinkle=0.18):
    """Elephant hide: grey, wrinkled in fine creases."""
    for k, r in f.items():
        light = {'top': 1.08, 'bottom': 0.72, 'front': 1.0, 'back': 0.86}.get(k, 0.94)
        fx, fy, fw, fh = r
        for j in range(fh):
            for i in range(fw):
                crease = (j + (i * 7 % 5)) % 6 == 0 and p.rnd.random() < 0.7
                k2 = light * (0.82 if crease else 1.0)
                n = p.rnd.randint(-6, 6)
                p.px((fx + i, fy + j), [c * k2 + n for c in base])


def feathers(p, rect, base, edge, rows=3):
    """Overlapping feathers, rows of them, each lit at its root and edged at its tip."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            cx = (i + (j // rows) * 3) % 6
            cy = j % rows
            tip = cy == rows - 1 and cx in (1, 2, 3, 4)
            n = p.rnd.randint(-5, 5)
            c = [a * 0.55 + b * 0.45 for a, b in zip(edge, base)] if tip else [v * (1.12 - 0.12 * cy) for v in base]
            p.px((fx + i, fy + j), [v + n for v in c])


def gold(p, f):
    p.shaded(f, GOLD)
    for r in f.values():
        fx, fy, fw, fh = r
        for i in range(fw):
            p.px((fx + i, fy), (250, 214, 110), glow=True)


def plain(p, f, c, glow=False):
    for k, r in f.items():
        light = {'top': 1.08, 'bottom': 0.72, 'front': 1.0, 'back': 0.86}.get(k, 0.94)
        p.face(r, c, light, noise=6, glow=glow)


def blade(p, f):
    for k, r in f.items():
        p.gradient(r, (236, 240, 246), (150, 156, 168), horizontal=k in ('front', 'back'))


# ── Lion-Demon ────────────────────────────────────────────────────────────────
TAWNY = (196, 142, 64)
TAWNY_DARK = (128, 82, 34)
MANE = (120, 66, 26)
LEATHER = (66, 46, 36)

LION_DEMON = mirrored([
    part('body', 'root', (0, 10, 0), (0, 0, 0), [
        (-5, -16, -3, 10, 16, 6, 'jerkin'),
        (-5.5, -3, -3.5, 11, 2, 7, 'belt'),
        (-5.4, -1, -3.3, 10.8, 5, 6.6, 'skirt'),
        (-2, -14, -3.6, 4, 4, 0.6, 'plate'),
    ]),
    part('head', 'body', (0, -16, -0.5), (0, 0, 0), [
        (-4.5, -9, -5, 9, 9, 8, 'lionface'),
        (-2.5, -4, -7.5, 5, 4, 2.5, 'muzzle'),
        (-6.5, -11.5, -1.5, 13, 13, 7, 'mane'),
        (-5, -12, -4, 10, 3, 4, 'mane'),
    ]),
    part('right_ear', 'head', (-3.5, -9, -1), (0, 0, -0.3), [(-1, -2, -0.5, 2, 2, 1, 'ear')]),
    part('right_arm', 'body', (-6.5, -14, 0), (0, 0, 0), [(-2.5, -2, -2, 4, 13, 4, 'arm'), (-3.2, -3, -2.6, 5.4, 4, 5.2, 'pauldron')]),
    part('sabre', 'right_arm', (-0.5, 10, 0), (1.5708, 0, 0), [
        (-0.5, -2, -0.5, 1, 5, 1, 'grip'), (-1.5, -3, -1.5, 3, 1, 3, 'bronze'),
        (-0.4, -17, -1.5, 0.8, 14, 3.5, 'blade'), (-0.4, -19, -0.5, 0.8, 2, 2.5, 'blade')]),
    part('right_leg', 'root', (-2.5, 10, 0), (0, 0, 0), [(-2, 0, -2, 4, 12, 4, 'leg'), (-2.3, 11, -3, 4.6, 3, 5, 'paw')]),
    part('tail', 'body', (0, -2, 3), (0.7, 0, 0), [(-0.6, -0.6, 0, 1.2, 1.2, 10, 'tailfur'), (-1.3, -1.3, 9, 2.6, 2.6, 3.5, 'mane')]),
])


def lion_face(p, f, base, dark, mane_c, eyes, scale=1):
    shade_fur(p, f, base, dark, streak=0.1)
    fx, fy, fw, fh = f['front']
    # a heavy brow, slanted golden eyes, a dark nose line down to the muzzle
    for i in range(1, fw - 1):
        p.px((fx + i, fy + fh // 3 - 1), [c * 0.6 for c in dark])
    ew = max(3, fw // 5)
    for ex, inward in ((2, 1), (fw - 2 - ew, -1)):
        for i in range(ew):
            j = fy + fh // 3 + (i if inward > 0 else ew - 1 - i) // 2
            p.px((fx + ex + i, j), eyes, glow=True)
            p.px((fx + ex + i, j + 1), [c * 0.8 for c in eyes], glow=True)
        p.px((fx + ex + ew // 2, fy + fh // 3 + 1), DARK)
    for j in range(fh // 3 + 2, fh - 2):
        p.px((fx + fw // 2, fy + j), [c * 0.75 for c in dark])
        p.px((fx + fw // 2 - 1, fy + j), [c * 0.85 for c in dark])


def paint_lion_demon(p, style, f, dims):
    if style == 'lionface':
        lion_face(p, f, TAWNY, TAWNY_DARK, MANE, (255, 200, 60))
    if style == 'muzzle':
        shade_fur(p, f, [c * 1.08 for c in TAWNY], TAWNY_DARK, 0.05)
        fx, fy, fw, fh = f['front']
        for i in range(fw // 2 - 2, fw // 2 + 2):          # the nose
            for j in range(0, 3):
                p.px((fx + i, fy + j), (40, 26, 22))
        for i in range(1, fw - 1):                          # a snarl, fangs
            p.px((fx + i, fy + fh - 3), (70, 20, 20))
        for i in (2, fw - 3):
            p.px((fx + i, fy + fh - 2), IVORY)
            p.px((fx + i, fy + fh - 3), IVORY)
    if style == 'mane':
        shade_fur(p, f, MANE, (70, 36, 14), streak=0.4)
    if style == 'ear':
        plain(p, f, TAWNY_DARK)
    if style in ('arm', 'leg'):
        shade_fur(p, f, TAWNY, TAWNY_DARK, 0.15)
    if style == 'paw':
        shade_fur(p, f, TAWNY, TAWNY_DARK, 0.1)
        fx, fy, fw, fh = f['front']
        for i in range(1, fw, 3):
            p.px((fx + i, fy + fh - 1), IVORY)              # claws
    if style == 'jerkin':
        for k, r in f.items():
            lamellar(p, r, LEATHER, (40, 28, 22))
        fx, fy, fw, fh = f['front']
        for j in range(fh):                                  # its lacing down the front
            p.px((fx + fw // 2, fy + j), BRONZE)
    if style in ('belt', 'bronze', 'plate'):
        p.shaded(f, BRONZE)
    if style == 'pauldron':
        for r in f.values():
            lamellar(p, r, BRONZE, (90, 60, 30))
    if style == 'skirt':
        for r in f.values():
            lamellar(p, r, (110, 34, 28), (60, 20, 18))
    if style == 'grip':
        p.shaded(f, (60, 36, 24))
    if style == 'blade':
        blade(p, f)
    if style == 'tailfur':
        shade_fur(p, f, TAWNY, TAWNY_DARK)


# ── Elephant-Demon ────────────────────────────────────────────────────────────
GREY = (128, 128, 132)
GREY_DARK = (92, 92, 98)
IRON = (82, 84, 92)

ELEPHANT_DEMON = mirrored([
    part('body', 'root', (0, 8, 0), (0, 0, 0), [
        (-8, -20, -5, 16, 20, 10, 'greyhide'),
        (-6.5, -19, -5.6, 13, 8, 0.8, 'iron'),
        (-8.5, -4, -5.5, 17, 3, 11, 'belt'),
        (-8.3, -1, -5.3, 16.6, 5, 10.6, 'loin'),
    ]),
    part('head', 'body', (0, -20, -1.5), (0, 0, 0), [
        (-5, -10, -6, 10, 10, 9, 'greyface'),
        (-3.6, -2.5, -9, 1.4, 1.4, 5, 'tusk'),
        (2.2, -2.5, -9, 1.4, 1.4, 5, 'tusk'),
    ]),
    part('right_ear', 'head', (-5, -9, -2), (0, -0.35, 0), [(-1, -1, -1, 1, 11, 10, 'greyear')]),
    part('trunk1', 'head', (0, -3, -6), (0.08, 0, 0), [(-1.75, 0, -1.75, 3.5, 6, 3.5, 'greytrunk')]),
    part('trunk2', 'trunk1', (0, 6, 0), (0.12, 0, 0), [(-1.5, 0, -1.5, 3, 6, 3, 'greytrunk')]),
    part('trunk3', 'trunk2', (0, 6, 0), (0.18, 0, 0), [(-1.25, 0, -1.25, 2.5, 5, 2.5, 'greytrunk')]),
    part('right_arm', 'body', (-10, -17, 0), (0, 0, 0), [
        (-3, -2, -3, 6, 17, 6, 'greyarm'), (-3.6, -3, -3.6, 7.2, 5, 7.2, 'iron'), (-3.4, 11, -3.4, 6.8, 3, 6.8, 'iron')]),
    part('mace', 'right_arm', (0, 14, 0), (1.5708, 0, 0), [(-0.75, -6, -0.75, 1.5, 12, 1.5, 'wood'), (-2.2, -12, -2.2, 4.4, 6, 4.4, 'macehead')]),
    part('right_leg', 'root', (-4.5, 8, 0), (0, 0, 0), [(-3, 0, -3, 6, 13, 6, 'greyleg'), (-3.4, 13, -3.6, 6.8, 3, 7.2, 'foot')]),
])


def elephant_face(p, f, base, eyes, ivory_face=False):
    hide(p, f, base)
    fx, fy, fw, fh = f['front']
    # small deep-set eyes high on the face, a brow over each, the trunk's root below
    for ex in (2, fw - 5):
        p.px((fx + ex - 1, fy + fh // 3 - 1), [c * 0.6 for c in base])
        for i in range(3):
            p.px((fx + ex + i, fy + fh // 3 - 1), [c * 0.6 for c in base])
            p.px((fx + ex + i, fy + fh // 3), eyes, glow=True)
        p.px((fx + ex + 1, fy + fh // 3), DARK)
    for j in range(fh // 2, fh):
        for i in range(fw // 2 - 3, fw // 2 + 3):
            if (j + i) % 3 == 0:
                p.px((fx + i, fy + j), [c * 0.84 for c in base])


def paint_elephant_demon(p, style, f, dims):
    if style in ('greyhide', 'greyarm', 'greyleg', 'greytrunk'):
        hide(p, f, GREY)
    if style == 'greytrunk':
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(1, fh, 3):
                for i in range(fw):
                    p.px((fx + i, fy + j), [c * 0.78 for c in GREY])   # its rings
    if style == 'greyface':
        elephant_face(p, f, GREY, (230, 70, 40))
    if style == 'greyear':
        hide(p, f, [c * 0.95 for c in GREY])
        for k in ('right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(fh):
                for i in range(fw):
                    if (i - fw / 2) ** 2 / (fw / 2) ** 2 + (j - fh / 2) ** 2 / (fh / 2) ** 2 < 0.4:
                        p.px((fx + i, fy + j), (176, 128, 128))         # pink within
    if style == 'tusk':
        for r in f.values():
            p.face(r, IVORY, 1.0, noise=4)
    if style in ('iron', 'macehead'):
        for r in f.values():
            lamellar(p, r, IRON, (50, 52, 58))
    if style == 'macehead':
        for r in f.values():
            fx, fy, fw, fh = r
            for i in range(1, fw, 3):
                for j in range(1, fh, 3):
                    p.px((fx + i, fy + j), (200, 200, 210))             # studs
    if style == 'belt':
        p.shaded(f, (90, 60, 40))
    if style == 'loin':
        p.shaded(f, (120, 92, 52))
    if style == 'wood':
        p.shaded(f, (90, 60, 36))
    if style == 'foot':
        hide(p, f, GREY_DARK)
        fx, fy, fw, fh = f['front']
        for i in range(1, fw - 1, 4):
            for di in range(2):
                p.px((fx + i + di, fy + fh - 2), IVORY)                # toenails


# ── Peng-Demon ────────────────────────────────────────────────────────────────
PLUME = (46, 44, 58)
PLUME_EDGE = (176, 136, 58)
BEAK = (214, 168, 60)

PENG_DEMON = mirrored([
    part('body', 'root', (0, 12, 0), (0, 0, 0), [
        (-4, -16, -2.5, 8, 16, 5, 'plumage'),
        (-4.4, -4, -2.9, 8.8, 2, 5.8, 'sash'),
        (-3, -15, -2.9, 6, 9, 0.5, 'breast'),
    ]),
    part('head', 'body', (0, -16, 0), (0, 0, 0), [
        (-3, -7, -3.5, 6, 7, 7, 'birdhead'),
        (-1, -4.5, -6.5, 2, 2.2, 3.2, 'beak'),
        (-1, -2.6, -6.6, 2, 2, 1, 'beak'),
    ]),
    part('crest', 'head', (0, -7, 0), (-0.3, 0, 0), [(-0.5, -4, -1, 1, 4, 6, 'crest')]),
    part('right_wing', 'body', (-3.5, -14, 2.5), (0.15, 0, 0.12), [(-1, 0, 0, 1, 16, 9, 'wing')]),
    part('right_arm', 'body', (-5, -14, 0), (0, 0, 0), [(-1.5, -1, -1.5, 3, 12, 3, 'featherarm')]),
    part('spear', 'right_arm', (0, 10, 0), (1.5708, 0, 0), [(-0.5, -20, -0.5, 1, 30, 1, 'shaft'), (-0.8, -25, -0.8, 1.6, 5, 1.6, 'spearhead')]),
    part('right_leg', 'root', (-2, 12, 0), (0, 0, 0), [
        (-1.6, 0, -1.6, 3.2, 6, 3.2, 'thigh'), (-1, 6, -1, 2, 5, 2, 'shank'), (-1.6, 11, -3, 3.2, 1, 4.4, 'talon')]),
    part('tail', 'body', (0, -1, 2.5), (0.6, 0, 0), [(-3, 0, 0, 6, 1, 9, 'tailfeathers')]),
])


def paint_peng_demon(p, style, f, dims):
    if style in ('plumage', 'thigh', 'featherarm', 'wing', 'tailfeathers', 'crest'):
        for k, r in f.items():
            feathers(p, r, PLUME if style != 'crest' else (150, 40, 34), PLUME_EDGE if style != 'crest' else (220, 90, 50))
    if style == 'breast':
        for r in f.values():
            feathers(p, r, (196, 160, 90), (150, 110, 50), rows=2)
    if style == 'birdhead':
        for k, r in f.items():
            feathers(p, r, PLUME, (70, 66, 86), rows=2)
        fx, fy, fw, fh = f['front']
        for ex in (1, fw - 4):                                   # fierce gold eyes, a dark stripe through each
            for i in range(3):
                p.px((fx + ex + i, fy + 5), (255, 214, 60), glow=True)
                p.px((fx + ex + i, fy + 6), (230, 160, 40), glow=True)
            p.px((fx + ex + 1, fy + 5), DARK)
        for k in ('right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                p.px((fx + i, fy + 6), (30, 26, 34))
    if style == 'beak':
        plain(p, f, BEAK)
        fx, fy, fw, fh = f['bottom']
        p.face(f['bottom'], [c * 0.7 for c in BEAK], 1.0)
    if style in ('shank', 'talon'):
        plain(p, f, (196, 160, 70))
    if style == 'talon':
        fx, fy, fw, fh = f['top']
        for i in range(0, fw, 2):
            p.px((fx + i, fy), DARK)
    if style == 'sash':
        p.shaded(f, (150, 30, 30))
    if style == 'shaft':
        p.shaded(f, (70, 40, 30))
    if style == 'spearhead':
        blade(p, f)


# ── The Little Wind-Driller ──────────────────────────────────────────────────
GOBLIN = (120, 150, 82)
TUNIC = (150, 112, 70)

WIND_DRILLER = mirrored([
    part('body', 'root', (0, 16, 0), (0, 0, 0), [
        (-3.5, -10, -2, 7, 10, 4, 'tunic'),
        (-3.8, -3, -2.3, 7.6, 1.5, 4.6, 'sash'),
        (-1, -3.8, -2.7, 2, 3, 0.5, 'token'),
        (-3.6, -1.5, -2.2, 7.2, 3, 4.4, 'tunichem'),
    ]),
    part('head', 'body', (0, -10, 0), (0, 0, 0), [
        (-4, -7, -3.5, 8, 7, 7, 'goblin'),
        (-3.6, -9.5, -3.1, 7.2, 3, 6.2, 'cap'),
        (-1.6, -12.5, -1.6, 3.2, 3, 3.2, 'cap'),
        (-0.6, -14, -0.6, 1.2, 1.5, 1.2, 'capknot'),
    ]),
    part('right_ear', 'head', (-4, -4.5, 0), (0, 0.3, -0.3), [(-2.5, -1, -0.5, 2.5, 2, 1, 'goblinear')]),
    part('right_arm', 'body', (-4.5, -9, 0), (0, 0, 0), [(-1.5, -1, -1.25, 2.5, 9, 2.5, 'goblinarm')]),
    part('clapper', 'right_arm', (-0.25, 7.5, 0), (1.5708, 0, 0), [(-0.5, -6, -0.5, 1, 7, 1, 'wood'), (-1.5, -9, -1.5, 3, 3, 3, 'bell')]),
    part('right_leg', 'root', (-1.7, 16, 0), (0, 0, 0), [(-1.4, 0, -1.4, 2.8, 8, 2.8, 'trousers')]),
])


def paint_wind_driller(p, style, f, dims):
    if style in ('goblin', 'goblinear', 'goblinarm'):
        p.shaded(f, GOBLIN)
    if style == 'goblin':
        fx, fy, fw, fh = f['front']                          # 16x14: big round eyes, a wide grin
        for ex in (2, fw - 6):
            for i in range(4):
                for j in range(3):
                    p.px((fx + ex + i, fy + 5 + j), (250, 240, 200))
            p.px((fx + ex + 1 + (1 if ex > 4 else 0), fy + 6), DARK)
            p.px((fx + ex + 2 - (1 if ex > 4 else 0), fy + 6), (40, 120, 40), glow=True)
        for i in range(4, fw - 4):
            p.px((fx + i, fy + fh - 3), (60, 30, 30))
        for i in (5, fw - 6):
            p.px((fx + i, fy + fh - 4), IVORY)
        p.px((fx + fw // 2, fy + 9), [c * 0.8 for c in GOBLIN])
    if style == 'cap':
        p.shaded(f, (66, 92, 120))
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), (180, 40, 36))
    if style == 'capknot':
        plain(p, f, (180, 40, 36))
    if style in ('tunic', 'tunichem'):
        for k, r in f.items():
            p.face(r, TUNIC, {'top': 1.08, 'bottom': 0.72}.get(k, 0.95), noise=8, fall=0.2)
        fx, fy, fw, fh = f['front']
        for j in range(3, fh - 3, 4):                         # patches
            for i in range(2, 5):
                p.px((fx + i + (j % 3), fy + j), (110, 80, 50))
    if style == 'sash':
        p.shaded(f, (180, 40, 36))
    if style == 'token':                                    # 令: the patrol token, gold
        gold(p, f)
        fx, fy, fw, fh = f['front']
        for j in range(1, fh - 1):
            p.px((fx + fw // 2, fy + j), (120, 60, 20))
    if style == 'trousers':
        p.shaded(f, (66, 92, 120))
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), (40, 30, 26))
    if style == 'wood':
        p.shaded(f, (120, 80, 44))
    if style == 'bell':
        gold(p, f)


# ── The two demon kings ───────────────────────────────────────────────────────
# Each has two models. His demon-king shape: a hulking armoured giant, two and a half times a man's
# height, all his weight in his chest and shoulders, his head carried low and forward. And his true
# form, revealed at his last third: the beast a Bodhisattva rides, long and heavy, as large as the
# Great White Bull or larger. The Elephant's two shapes share his head; the Lion's true form has a
# head of its own, the guardian lion's. Their eyes smoulder under a heavy brow
# that slopes down to the nose in a scowl (drawn full-bright, but dark), angled into a glare: each
# eye two model pixels along its bottom, two tall at its outer side, one at its inner, the brow's
# shadow cutting its inner corner.

SOCKET = (12, 9, 10)
UNDER = (26, 20, 20)
CLAW = (40, 32, 28)
MOUTH = (82, 16, 22)
THROAT = (36, 8, 12)


def cell(p, rect, i, j, c, glow=False):
    """One model pixel (S x S texture pixels) of a face: `i` across, `j` down."""
    for a in range(bg.S):
        for b in range(bg.S):
            p.px((rect[0] + i * bg.S + a, rect[1] + j * bg.S + b), c, glow=glow)


def ember_cell(p, rect, i, j, c):
    """A model pixel of a burning eye: the skin dark beneath, the glow drawn over it."""
    for a in range(bg.S):
        for b in range(bg.S):
            x, y = rect[0] + i * bg.S + a, rect[1] + j * bg.S + b
            p.px((x, y), [v * 0.35 for v in c])
            p.glow.putpixel((x, y), tuple(int(v) for v in c) + (255,))


def glare(p, f, eyes, row, ember, core):
    """Eyes on a box's front: each (outer, inner) a pair of model-pixel columns, `row` the socket's
    shadow under the brow. The outer column burns two tall, the inner one only at the bottom (the
    brow's shadow over it), so the eye's top slopes down toward the nose; a glint at each side."""
    rect = f['front']
    for o, i in eyes:
        cell(p, rect, o, row, SOCKET)
        cell(p, rect, i, row, SOCKET)
        cell(p, rect, i, row + 1, SOCKET)
        ember_cell(p, rect, o, row + 1, ember)
        ember_cell(p, rect, o, row + 2, ember)
        ember_cell(p, rect, i, row + 2, core)
        cell(p, rect, o, row + 3, UNDER)
        cell(p, rect, i, row + 3, UNDER)
    for side, col in (('right', f['right'][2] // bg.S - 1), ('left', 0)):
        cell(p, f[side], col, row + 1, SOCKET)
        ember_cell(p, f[side], col, row + 2, ember)


def pelt(p, f, base, dark, belly=None, along=False):
    """A short-haired hide: lit above and shadowed below, faintly mottled, a few short strokes of
    darker hair (running down it, or `along` a beast's flanks), the belly paler."""
    for k, r in f.items():
        light = {'top': 1.06, 'bottom': 0.8, 'front': 1.0, 'back': 0.86}.get(k, 0.93)
        c0 = belly if (k == 'bottom' and belly) else base
        fx, fy, fw, fh = r
        for j in range(fh):
            for i in range(fw):
                k2 = light * (1.04 - 0.24 * (j / max(1, fh - 1)) if k not in ('top', 'bottom') else light)
                n = p.rnd.randint(-4, 4)
                p.px((fx + i, fy + j), [v * k2 + n for v in c0])
        if along and k in ('right', 'left', 'top'):                    # darker along the back
            for j in range(fh if k == 'top' else fh // 3):
                for i in range(fw):
                    c = p.img.getpixel((fx + i, fy + j))
                    t = 0.7 if k == 'top' else 0.7 + 0.3 * j / max(1, fh // 3)
                    p.px((fx + i, fy + j), [v * t for v in c[:3]])
        for _ in range(fw * fh // 40):                                 # its hair, in a few short strokes
            x, y = fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)
            for d in range(2):
                q = (x + d, y) if along and k in ('right', 'left', 'top') else (x, y + d)
                if q[0] < fx + fw and q[1] < fy + fh:
                    c = p.img.getpixel(q)
                    p.px(q, [a * 0.7 + b * 0.3 for a, b in zip(c[:3], dark)])


def mane(p, f, base, dark, tip):
    """A wild mane in heavy locks: broad strands, darker in their partings and toward their roots,
    lit along their ends, glinting here and there."""
    for k, r in f.items():
        light = {'top': 1.06, 'bottom': 0.6, 'front': 0.72, 'back': 0.84}.get(k, 0.9)
        fx, fy, fw, fh = r
        i = 0
        while i < fw:
            width = p.rnd.randint(3, 6)
            tone = p.rnd.uniform(0.82, 1.12)
            reach = p.rnd.uniform(0.75, 1.0)
            for di in range(width):
                if i + di >= fw:
                    break
                parting = di == width - 1
                for j in range(fh):
                    s = j / max(1, fh - 1)
                    if k in ('top', 'bottom'):
                        c = [v * tone for v in base]
                    elif s > reach:
                        c = dark                                       # the lock ends; the dark beneath shows
                    else:
                        c = [a + (b - a) * (s / reach) ** 2 for a, b in zip(dark, base)]
                        c = [v * tone for v in c]
                        if s / reach > 0.82:
                            c = [a * 0.6 + b * 0.4 for a, b in zip(c, tip)]
                    if parting:
                        c = [v * 0.62 for v in c]
                    n = p.rnd.randint(-4, 4)
                    p.px((fx + i + di, fy + j), [v * light + n for v in c])
            i += width


def spike(p, f, base, dark, tip):
    """A tuft of mane come to a point: dark at its root (the bottom of its faces), pale at its tip."""
    for k, r in f.items():
        if k in ('top', 'bottom'):
            p.face(r, tip if k == 'top' else dark, 1.0, noise=6)
            continue
        fx, fy, fw, fh = r
        for i in range(fw):
            off = p.rnd.random() * 0.3
            for j in range(fh):
                t = min(1.0, j / max(1, fh - 1) + off)
                c = [a + (b - a) * t * 2 for a, b in zip(tip, base)] if t < 0.5 else [a + (b - a) * (t - 0.5) * 2 for a, b in zip(base, dark)]
                n = p.rnd.randint(-6, 6)
                p.px((fx + i, fy + j), [v + n for v in c])


def trim(p, f, colour, sides=('front', 'back', 'right', 'left'), top=True, bottom=True):
    """A rim along a box's faces, a model pixel deep at their top and bottom."""
    for k in sides:
        fx, fy, fw, fh = f[k]
        for i in range(fw):
            for d in range(bg.S):
                if top:
                    p.px((fx + i, fy + d), colour if d == 0 else [v * 0.82 for v in colour])
                if bottom:
                    p.px((fx + i, fy + fh - 1 - d), [v * 0.72 for v in colour])


def bands(p, f, base, shadow, edge, rivet=None, band=5):
    """Plate armour in overlapping horizontal bands, `band` model pixels deep: each lit along its
    upper edge, shadowed under the lip of the one above, an edge of `edge` along its foot, rivets."""
    h = band * bg.S
    for k, r in f.items():
        light = {'top': 1.1, 'bottom': 0.62, 'front': 1.0, 'back': 0.84}.get(k, 0.9)
        fx, fy, fw, fh = r
        for j in range(fh):
            row = j % h
            for i in range(fw):
                n = p.rnd.randint(-3, 3)
                if k in ('top', 'bottom'):
                    c = [v * light for v in base]
                elif row == h - 1:
                    c = edge
                elif row < bg.S:
                    c = [v * light * 0.78 + (s - v) * 0.3 for v, s in zip(base, shadow)]   # under the lip above
                elif row < bg.S + 1:
                    c = [v * light * 1.12 for v in base]                                 # its lit edge
                else:
                    c = [v * light * (1.04 - 0.1 * row / h) for v in base]
                p.px((fx + i, fy + j), [v + n for v in c])
            if rivet and k not in ('top', 'bottom') and row == bg.S + 2:
                for i in range(bg.S * 2, fw - bg.S, bg.S * 5):
                    for a in range(bg.S):
                        p.px((fx + i + a, fy + j), rivet)


def claws(p, f):
    """A claw: dark horn at its root, pale to its point (the bottom of its faces)."""
    for k, r in f.items():
        p.gradient(r, CLAW, (220, 210, 186), noise=3)


def teeth(p, f):
    for r in f.values():
        p.gradient(r, (236, 228, 204), (196, 184, 150), noise=3)


def tuft(prefix, parent, at, rot, big=True):
    """A tuft of the mane, tapering to a point (a part of two boxes)."""
    boxes = [(-2.5, -7, -2.5, 5, 7, 5, 'manespike'), (-1.5, -12, -1.5, 3, 5, 3, 'manespike')] if big \
        else [(-2, -5, -2, 4, 5, 4, 'manespike'), (-1, -9, -1, 2, 4, 2, 'manespike')]
    return part(prefix, parent, at, rot, boxes)


def brow(prefix, parent, at, width, slope, style):
    """Half a heavy brow, sloping down from its outer end to the nose: a scowl."""
    return part(prefix, parent, at, (0, 0, slope), [(-width, -1, -1.5, width, 2.5, 3, style)])


# ── The Azure Lion ────────────────────────────────────────────────────────────
TEAL = (32, 104, 98)           # 青: the green-blue of his mane, dark
TEAL_DARK = (8, 38, 42)
TEAL_TIP = (86, 164, 144)
FUR = (128, 104, 74)           # his hide, an ashen tawny
FUR_DARK = (54, 40, 28)
FUR_PALE = (156, 132, 100)
LACQUER = (28, 24, 32)
LION_EMBER = (132, 72, 6)      # his eyes smoulder amber: dark, not a blaze
LION_CORE = (168, 104, 14)


def lion_head(parent, at, rot):
    """The lion's head in his demon-king shape: a broad skull, wide at the jowls, under a scowling brow;
    a short heavy muzzle; the jaws open in a snarl round long fangs; behind it a ruff of mane
    bristling into points, swept back, cheek ruffs, a beard. Its pivot is the back of the skull."""
    return [
        part('head', parent, at, rot, [
            (-7.5, -7, -12, 15, 11, 12, 'lionface'),
            (-8.5, -2, -11, 17, 5, 9, 'jowl'),                # the jowls: the face broadest under the eyes
            (-5, -4, -17, 10, 6.5, 6, 'muzzle'),
            (-3, -5.5, -16, 6, 2, 5, 'snout'),                 # the bridge of the nose, rising to the brow
            (-2.5, -5.2, -17.6, 5, 2.4, 1.2, 'nose'),
            (-4.2, 2.5, -16.4, 1.6, 5, 1.6, 'fang'), (2.6, 2.5, -16.4, 1.6, 5, 1.6, 'fang'),
            (-12, -14, -4, 24, 24, 8, 'azuremane'),            # the ruff behind his face
            (-7, 4.5, -9, 14, 10, 8, 'azuremane'),             # his beard
        ]),
        brow('right_brow', 'head', (-0.5, -6.5, -12.5), 8, 0.32, 'brow'),
        part('jaw', 'head', (0, 2.5, -11), (0.42, 0, 0), [
            (-4.5, 0, -6, 9, 2.5, 6, 'jaw'),
            (-3.6, -2.4, -5.6, 1.4, 2.4, 1.4, 'fang'), (2.2, -2.4, -5.6, 1.4, 2.4, 1.4, 'fang'),
        ]),
        part('right_cheek', 'head', (-8, 1, -6), (0, 0.4, 0.2), [(-4, -8, -4, 4, 15, 8, 'azuremane')]),
        tuft('top_tuft', 'head', (0, -13, 1), (-0.85, 0, 0)),
        tuft('right_tuft_a', 'head', (-6.5, -12, 1), (-0.75, 0, -0.5)),
        tuft('right_tuft_b', 'head', (-11, -6, 1), (-0.7, 0, -1.05)),
        tuft('right_tuft_c', 'head', (-11.5, 2, 1), (-0.6, 0, -1.6)),
        tuft('right_tuft_d', 'head', (-9, 9, 0), (-0.4, 0, -2.25), big=False),
        tuft('right_flare_a', 'head', (-9, -10, -3), (0.35, 0, -0.75), big=False),
        tuft('right_flare_b', 'head', (-11, 0, -3), (0.3, 0, -1.4), big=False),
        tuft('flare_top', 'head', (0, -12, -3), (0.4, 0, 0), big=False),
    ]


# His demon-king shape: hunched forward from the waist, his head thrust out low in its ruff, arms
# that hang to his knees and end in claws; dark lacquered armour edged in gold, a gold lion's face on
# his breast, layered pauldrons spiked with gold; his mane falls in a mantle over his shoulders and
# down his back. A giant's broadsword.
AZURE_LION = mirrored([
    part('body', 'root', (0, 4, 0), (0.12, 0, 0), [
        (-9.5, -11, -6, 19, 11, 12, 'lacquer'),                # the belly
        (-13, -25, -8, 26, 15, 16, 'chest'),                   # the chest: his weight is here
        (-8.5, -23, -8.6, 17, 11, 1, 'breastplate'),
        (-11, -3, -6.8, 22, 3, 13.6, 'gold'),                  # the belt
        (-3, -4, -7.6, 6, 5, 1, 'buckle'),
        (-12, 0, -7.2, 24, 9, 14.4, 'tassets'),
        (-14, -31, -2, 28, 9, 12, 'azuremane'),                # the mane over his shoulders
        (-12.5, -24, 6, 25, 22, 5, 'azuremane'),               # and falling down his back
    ]),
    tuft('right_shoulder_tuft', 'body', (-10, -30, 6), (0.8, 0, -0.6)),
    tuft('back_tuft', 'body', (0, -30, 8), (1.1, 0, 0)),
    tuft('right_back_tuft', 'body', (-6, -22, 10), (1.3, 0, -0.3), big=False),
] + lion_head('body', (0, -27, -7), (0.18, 0, 0)) + [
    part('right_arm', 'body', (-16.5, -21, -0.5), (0, 0, 0), [
        (-4.5, -2, -4.5, 9, 13, 9, 'lionarm'),
        (-7, -6, -7, 14, 7, 14, 'pauldron'),
        (-7.5, -1, -6.5, 8, 5, 13, 'pauldron'),
    ]),
    part('right_pauldron_spike', 'right_arm', (-3.5, -6, -2.5), (-0.25, 0, -0.55), [(-1.5, -6, -1.5, 3, 6, 3, 'goldspike'), (-0.75, -9, -0.75, 1.5, 3, 1.5, 'goldspike')]),
    part('right_pauldron_spike2', 'right_arm', (-3.5, -6, 3), (0.25, 0, -0.55), [(-1.25, -5, -1.25, 2.5, 5, 2.5, 'goldspike'), (-0.6, -7.5, -0.6, 1.2, 2.5, 1.2, 'goldspike')]),
    part('right_forearm', 'right_arm', (0, 11, 0), (-0.2, 0, 0), [
        (-5, 0, -5, 10, 9, 10, 'bracer'),
        (-4.5, 9, -4.8, 9, 5, 9.6, 'lionhand'),
        (-3.8, 13, -5.4, 1.5, 4.5, 1.5, 'claw'), (-0.75, 13, -5.4, 1.5, 4.5, 1.5, 'claw'), (2.3, 13, -5.4, 1.5, 4.5, 1.5, 'claw'),
    ]),
    part('sword', 'right_forearm', (0, 11.5, 0), (1.5708, 0, 0), [
        (-1, -3, -1, 2, 10, 2, 'grip'),
        (-1.6, 7, -1.6, 3.2, 2, 3.2, 'gold'),                  # the pommel
        (-3.5, -4.5, -3.5, 7, 1.5, 7, 'gold'),                 # the guard
        (-0.8, -38, -3, 1.6, 34, 7, 'broadblade'),
        (-0.8, -44, -0.5, 1.6, 6, 4.5, 'broadblade'),          # the point, swept up to the back
    ]),
    part('right_leg', 'root', (-6.5, 5, 0.5), (0, 0, 0), [
        (-5, 0, -5, 10, 16, 10, 'lionleg'),
        (-5.5, 7, -5.5, 11, 9, 11, 'greave'),
        (-5.5, 16, -8.5, 11, 3, 12.5, 'lionpaw'),
        (-4.6, 17, -10.2, 1.7, 2, 2, 'claw'), (-0.85, 17, -10.2, 1.7, 2, 2, 'claw'), (2.9, 17, -10.2, 1.7, 2, 2, 'claw'),
    ]),
    part('tail', 'body', (0, -2, 7), (0.75, 0, 0), [(-1.3, -1.3, 0, 2.6, 2.6, 16, 'tailfur'), (-3, -3, 14, 6, 6, 7, 'azuremane')]),
])

# His true form, Manjushri's lion (狻猊) turned demon: a beast of reach and spring, not of bulk. A deep
# chest, the waist drawn in under it, long legs (the hind ones bent like a cat's), great clawed paws;
# his hide a dark azure. The mane is everything: a ring of spiral curls (the stone lions' curls)
# framing a broad, flat face, and behind them locks like tongues of flame, swept back in tiers off his
# head, down his neck and over his shoulders, a crest of them along his spine and a fringe down his
# breast. The face: a heavy scowling brow over deep-set eyes, a short wide snout wrinkled in a snarl,
# flared nostrils, the jaws open on long fangs and a lolling tongue (they gape wide enough to swallow
# a man). A gold collar hung with bells: he is a Bodhisattva's mount. A long tail, curling up into a
# flame. Its pivots are set so the body turns about its middle, the legs about shoulder and hip.
AZ = (62, 102, 112)            # his hide: a dark azure, slate in its shadows
AZ_DARK = (20, 36, 46)
AZ_PALE = (124, 150, 150)
MANE_TEAL = (26, 104, 100)     # his mane, deeper and greener than his hide, its locks lit at their ends
MANE_DEEP = (6, 28, 36)
MANE_TIP = (118, 196, 170)
TONGUE = (150, 50, 62)
AZ_EMBER = (108, 52, 6)        # his eyes smoulder deeper in this shape: a banked fire
AZ_CORE = (142, 80, 10)


def lock(prefix, parent, at, rot, k=1.0, fin=False):
    """A lock of the mane like a tongue of flame: broad at its root, tapering in two steps to a point
    that bends over. It points up (-y) from its pivot; `rot` sweeps it back and fans it out. Flat across
    x, or (a `fin`) across z, so that swept back without a roll it still shows its breadth from the side."""
    w, d = (2.6 * k, 6.5 * k) if fin else (6.5 * k, 2.6 * k)
    l1, l2, l3 = 7 * k, 5 * k, 4 * k
    return part(prefix, parent, at, rot, [
        (-w / 2, -l1, -d / 2, w, l1, d, 'azlock'),
        (-w * 0.32, -l1 - l2, -d / 2 + 0.7 * k, w * 0.64, l2, d * 0.7, 'azlockmid'),
        (-w * 0.15, -l1 - l2 - l3, -d / 2 + 1.5 * k, w * 0.3, l3, d * 0.45, 'azlocktip'),
    ])


def curl(x, y, z, s=5.0, d=3):
    """A spiral curl (a box, seen end on), centred at x, y, its face at z."""
    return (x - s / 2, y - s / 2, z, s, s, d, 'azcurl')


# the curls framing his face, round its edge from the brow to the jowls
FACE_CURLS = [(x * side, y, s) for x, y, s in ((3, -12.1, 5.2), (8.1, -9.3, 4.4), (11.1, -4.4, 5.2), (11.1, 1.4, 4.4),
                                                (8.1, 6.3, 5.2), (3, 9.1, 4.4)) for side in (-1, 1)]


def true_lion_head(parent, at, rot):
    """The lion's head in his true form: a broad, flat face, a skull wide at the jowls under a heavy
    brow, the short wide snout wrinkled up to it in a snarl, flared nostrils; long fangs, a lolling
    tongue; a ring of curls about the face and a beard of them, and locks of mane swept back off it
    all in two tiers. Its pivot is the back of the skull."""
    return [
        part('head', parent, at, rot, [
            (-7, -6, -11, 14, 11, 11, 'azface'),
            (-6, -9, -10, 12, 4, 9, 'azcrown'),                # the crown of his skull, flat and broad
            (-8.5, -1, -10, 17, 6, 8, 'azjowl'),               # the face broadest under the eyes
            (-3, -4.2, -16, 6, 3.6, 5.5, 'azsnout'),           # the bridge of the nose, wrinkled
            (-5.8, -0.8, -16.5, 5.6, 4.8, 6, 'azmuzzle'), (0.2, -0.8, -16.5, 5.6, 4.8, 6, 'azmuzzle'),   # the whisker pads, the lip drawn back
            (-1, -0.8, -16, 2, 4.4, 5, 'azsnout'),             # the cleft between them
            (-2.8, -3.4, -17, 5.6, 2.8, 1.4, 'aznose'),
            (-5.6, 3.2, -16, 1.7, 3.6, 1.7, 'fang'), (3.9, 3.2, -16, 1.7, 3.6, 1.7, 'fang'),   # fangs outside the jaw,
            (-5.3, 6.6, -15.8, 1.1, 3, 1.1, 'fang'), (4.2, 6.6, -15.8, 1.1, 3, 1.1, 'fang'),   # tapering to points
            (-3.7, 3.6, -16.3, 7.4, 1.4, 0.8, 'azteeth'),      # the teeth between
            (-3.6, 1, -11, 7.2, 6, 4, 'azthroat'),
            (-11, -9, -4, 22, 14, 7, 'azruff'),                # the mane behind his face, round it
            (-7.5, -13, -4, 15, 22, 7, 'azruff'),
        ] + [curl(x, y, -6.5 if s > 5 else -5.8, s) for x, y, s in FACE_CURLS]),
        brow('right_brow', 'head', (-0.5, -5.5, -11.5), 7.5, 0.35, 'azbrow'),
        part('jaw', 'head', (0, 3.5, -10), (0.25, 0, 0), [
            (-3.8, 0, -6.5, 7.6, 2.5, 6.5, 'azjaw'),
            (-3.6, -2.6, -6.2, 1.5, 2.6, 1.5, 'fang'), (2.1, -2.6, -6.2, 1.5, 2.6, 1.5, 'fang'),
            curl(-5, 4.5, -7), curl(0, 5, -7.5), curl(5, 4.5, -7),   # his beard
        ]),
        part('tongue', 'jaw', (0, 0.3, -3), (0.35, 0, 0), [(-2, -0.5, -5.8, 4, 1, 6.8, 'aztongue')]),
        lock('top_lock', 'head', (0, -12, 1), (-1.3, 0, 0), 1.15, fin=True),
        lock('right_lock_a', 'head', (-6, -11, 0), (-1.1, 0, -0.5), 1.15, fin=True),
        lock('right_lock_b', 'head', (-10, -5, 0), (-0.9, 0, -1.05), 1.2),
        lock('right_lock_c', 'head', (-11, 2, 0), (-0.8, 0, -1.6), 1.2),
        lock('right_lock_d', 'head', (-8, 8, 0), (-0.7, 0, -2.2), 1.05),
    ]


AZURE_LION_TRUE = mirrored([
    part('body', 'root', (0, 0, 0), (0, 0, 0), [
        (-9, -7, -21, 18, 17, 17, 'azfur'),                    # chest and shoulders, deep
        (-7, 0, -23, 14, 11, 8, 'azchest'),                    # the breast
        (-6.5, -6, -5, 13, 10, 16, 'azflank'),                 # the waist, drawn in, the belly tucked up
        (-8, -5, 9, 16, 11, 11, 'azfur'),                      # the haunches
        (-10.5, -11, -22, 21, 19, 9, 'azmane'),                # the mane about his neck
        (-10, -12, -15, 20, 9, 8, 'azmane'),                   # and over his shoulders
        (-9.6, 3, -23.6, 19.2, 2.5, 9, 'azcollar'),            # the Bodhisattva's collar, and its bells
        (-2, 5, -25.4, 4, 4.5, 4, 'azbell'),
        (-6.6, 5, -24.4, 2.6, 3, 2.6, 'azbell'), (4, 5, -24.4, 2.6, 3, 2.6, 'azbell'),
        (-0.8, 9.5, -24.2, 1.6, 4, 1.6, 'aztassel'),
    ]),
    part('rump', 'body', (0, -7, 10), (-0.4, 0, 0), [(-7.5, 0, 0, 15, 7, 12, 'azfur')]),     # sloping down to his tail
    lock('crest_a', 'body', (0, -12, -15), (-1.35, 0, 0), 1.1, fin=True),
    lock('crest_b', 'body', (0, -11.5, -8), (-1.35, 0, 0), fin=True),
    lock('crest_c', 'body', (0, -8.5, -1), (-1.45, 0, 0), 0.8, fin=True),
    lock('crest_d', 'body', (0, -8, 7), (-1.5, 0, 0), 0.6, fin=True),
    lock('right_neck_a', 'body', (-7.5, -10, -18), (-1.4, 0, -0.55), 1.3, fin=True),
    lock('right_neck_b', 'body', (-10, -4, -18), (-1.4, 0, -1.2), 1.3),
    lock('right_neck_c', 'body', (-9.5, 3, -19), (-1.3, 0, -1.9), 1.15),
    lock('right_neck_d', 'body', (-9.5, -5, -9), (-1.5, 0, -1.05), 0.95),
    lock('apron', 'body', (0, 7, -22), (-2.75, 0, 0), 0.8),
    lock('right_apron', 'body', (-4.5, 6, -21.5), (-2.8, 0, 0.3), 0.8),
] + true_lion_head('body', (0, -6, -21), (0.1, 0, 0)) + [
    part('right_foreleg', 'body', (-6.5, -1, -14), (0, 0, 0), [
        (-4, -4, -5, 8, 12, 10, 'azfur'),                      # the shoulder's muscle
        (-3, 8, -3.5, 6, 13, 6.5, 'azleg'),                    # the forearm, long
        curl(0, 11.5, 3),                                      # a curl at the elbow
        (-4, 20.5, -7.5, 8, 4.5, 9, 'azpaw'),
    ] + [(-3.7 + i * 2.05, 22, -9.3, 1.4, 2.5, 2.2, 'claw') for i in range(4)]),
    part('right_hindleg', 'body', (-5.5, -2, 15), (0, 0, 0), []),
    part('right_thigh', 'right_hindleg', (0, 0, 0), (-0.5, 0, 0), [(-4, -3, -5.5, 8, 15, 10, 'azfur')]),
    part('right_shin', 'right_thigh', (0, 11, -1), (1.4, 0, 0), [(-2.75, -1.5, -2.5, 5.5, 11, 5, 'azleg')]),
    part('right_hock', 'right_shin', (0, 9.5, 0), (-1.2, 0, 0), [(-2.5, -1, -2.25, 5, 9, 4.5, 'azleg')]),
    part('right_hindpaw', 'right_hock', (0, 8, 0), (0.3, 0, 0), [
        (-3.5, -0.7, -6.5, 7, 4, 8, 'azpaw'),
    ] + [(-3 + i * 2.1, 1, -8.2, 1.3, 2.2, 2, 'claw') for i in range(3)]),
    part('tail', 'body', (0, -3, 20.5), (-0.5, 0, 0), [(-1.3, -1.3, 0, 2.6, 2.6, 11, 'aztail')]),
    part('tail_mid', 'tail', (0, 0, 10.5), (0.9, 0, 0), [(-1.1, -1.1, 0, 2.2, 2.2, 10, 'aztail')]),
    part('tail_tip', 'tail_mid', (0, 0, 9.5), (0.8, 0, 0), [(-1, -1, 0, 2, 2, 5, 'aztail'), (-2.2, -2.2, 3.5, 4.4, 4.4, 4, 'azmane')]),
    lock('tail_flame', 'tail_tip', (0, -0.5, 6), (-1.1, 0, 0), 0.95, fin=True),
])


def lion_common(p, style, f):
    """What the Lion's two shapes share: his face, mane, claws, hide. Returns whether it painted."""
    if style == 'lionface':
        pelt(p, f, FUR, FUR_DARK)
        fx, fy, fw, fh = f['front']
        # a dark mask over the brow, about the eyes and down the sides of the face; a scowl's furrows between the brows
        for j in range(fh):
            for i in range(fw):
                edge = abs(i + 0.5 - fw / 2) / (fw / 2)
                if j < 7 * bg.S and edge > 0.28 or j < 2 * bg.S:
                    c = p.img.getpixel((fx + i, fy + j))
                    p.px((fx + i, fy + j), [v * (0.42 if j < 2 * bg.S else 0.55) for v in c[:3]])
        for j in range(0, 5 * bg.S):
            for d in (-bg.S, bg.S - 1):
                p.px((fx + fw // 2 + d, fy + j), [v * 0.45 for v in FUR_DARK])
        w = fw // bg.S
        glare(p, f, ((2, 3), (w - 3, w - 4)), 1, LION_EMBER, LION_CORE)
    elif style == 'jowl':
        pelt(p, f, [v * 0.7 for v in FUR], FUR_DARK)
        fx, fy, fw, fh = f['front']
        for j in range(fh):
            for i in range(fw):
                if abs(i + 0.5 - fw / 2) > fw * 0.3:
                    c = p.img.getpixel((fx + i, fy + j))
                    p.px((fx + i, fy + j), [v * 0.7 for v in c[:3]])
    elif style == 'brow':
        for k, r in f.items():
            p.face(r, (24, 17, 14), {'top': 1.5, 'bottom': 0.6}.get(k, 1.0), noise=3)
    elif style == 'snout':
        pelt(p, f, [v * 0.8 for v in FUR], FUR_DARK)
        for k in ('top', 'right', 'left'):                            # the snarl's wrinkles, across the bridge
            fx, fy, fw, fh = f[k]
            for j in range(0, fh, bg.S * 2) if k == 'top' else range(fh):
                for i in range(fw) if k == 'top' else range(bg.S, fw, bg.S * 2):
                    p.px((fx + i, fy + j), [v * 0.45 for v in FUR_DARK])
    elif style == 'muzzle':
        pelt(p, f, [v * 0.82 for v in FUR], FUR_DARK)
        fx, fy, fw, fh = f['front']
        for j in range(fh // 2):                                      # dark down the middle, from the nose
            for i in range(fw // 2 - bg.S, fw // 2 + bg.S):
                p.px((fx + i, fy + j), [v * 0.5 for v in FUR_DARK])
        for k in ('front', 'right', 'left'):                         # the lip drawn back, black, over the gums
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for j in range(fh - bg.S, fh):
                    p.px((fx + i, fy + j), (24, 10, 10))
                for j in range(fh - 2 * bg.S, fh - bg.S):
                    p.px((fx + i, fy + j), MOUTH if (k == 'front' or i >= fw // 2) else (24, 10, 10))
        fx, fy, fw, fh = f['front']                                   # whisker spots
        for i in list(range(bg.S, bg.S * 3, bg.S)) + list(range(fw - bg.S * 3, fw - bg.S, bg.S)):
            p.px((fx + i, fy + fh // 2), [v * 0.4 for v in FUR_DARK])
        p.face(f['bottom'], THROAT, 1.0, noise=4)
    elif style == 'nose':
        plain(p, f, (22, 14, 16))
        fx, fy, fw, fh = f['front']
        for i in range(fw):                                           # narrowing down to its tip
            for j in range(fh):
                if abs(i + 0.5 - fw / 2) > fw / 2 - j * 0.9:
                    p.px((fx + i, fy + j), [v * 0.5 for v in FUR_DARK])
    elif style == 'fang':
        teeth(p, f)
    elif style == 'jaw':
        pelt(p, f, [v * 0.9 for v in FUR_PALE], FUR)
        fx, fy, fw, fh = f['top']                                     # the inside of the mouth: a dark tongue, the throat
        for j in range(fh):
            for i in range(fw):
                c = THROAT if j > fh * 0.6 else MOUTH if abs(i + 0.5 - fw / 2) > fw * 0.25 else (104, 30, 36)
                p.px((fx + i, fy + j), [v + p.rnd.randint(-4, 4) for v in c])
        for k in ('front', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for j in range(bg.S):
                    p.px((fx + i, fy + j), (24, 10, 10))
    elif style == 'azuremane':
        mane(p, f, TEAL, TEAL_DARK, TEAL_TIP)
    elif style == 'manespike':
        spike(p, f, TEAL, TEAL_DARK, TEAL_TIP)
    elif style == 'claw':
        claws(p, f)
    elif style in ('lionpaw', 'lionhand'):
        pelt(p, f, [v * 0.9 for v in FUR], FUR_DARK)
        p.face(f['bottom'], (40, 30, 28), 1.0, noise=4)              # dark pads
    elif style in ('lionarm', 'lionleg', 'tailfur'):
        pelt(p, f, FUR, FUR_DARK, FUR_PALE)
    elif style == 'lionfur':
        pelt(p, f, FUR, FUR_DARK, FUR_PALE, along=True)
    elif style == 'flank':
        pelt(p, f, [v * 0.94 for v in FUR], FUR_DARK, FUR_PALE, along=True)
    elif style == 'gold':
        gold(p, f)
    else:
        return False
    return True


def paint_azure_lion(p, style, f, dims):
    if lion_common(p, style, f):
        return
    if style in ('lacquer', 'chest', 'bracer', 'greave'):
        bands(p, f, LACQUER, (4, 4, 8), GOLD_DEEP, rivet=GOLD_DEEP if style == 'chest' else None, band=4 if style == 'bracer' else 5)
        trim(p, f, GOLD, bottom=style in ('bracer', 'greave'))
    if style == 'breastplate':                                        # a lion's face in gold, snarling
        for r in f.values():
            p.face(r, (40, 34, 44), 1.0, noise=4)
        fx, fy, fw, fh = f['front']
        cx, cy = fx + fw // 2, fy + fh // 2
        for j in range(fh):
            for i in range(fw):
                d = math.hypot((i + 0.5 - fw / 2) / (fw / 2), (j + 0.5 - fh / 2) / (fh / 2))
                if abs(d - 0.82) < 0.1:
                    p.px((fx + i, fy + j), GOLD)
        for side in (-1, 1):                                          # its eyes, slanted toward the nose
            for i in range(3 * bg.S):
                drop = (i if side > 0 else 3 * bg.S - 1 - i) // bg.S
                x = cx + bg.S + i if side > 0 else cx - bg.S - 3 * bg.S + i
                for j in range(bg.S):
                    p.px((x, cy - 3 * bg.S + j + (2 - drop)), GOLD)
        for i in range(-3 * bg.S, 3 * bg.S):                          # its fanged mouth
            p.px((cx + i, cy + 2 * bg.S), GOLD)
            if abs(i) in (2 * bg.S, 2 * bg.S + 1):
                for j in range(bg.S * 2):
                    p.px((cx + i, cy + 2 * bg.S + j), IVORY)
    if style == 'buckle':
        gold(p, f)
        fx, fy, fw, fh = f['front']
        for i in range(bg.S, fw - bg.S):
            p.px((fx + i, fy + fh // 2), (90, 60, 20))
    if style in ('tassets', 'pauldron'):
        bands(p, f, (30, 62, 66), (6, 16, 20), GOLD, band=4)          # lacquered teal-black plates, gold-edged
        trim(p, f, GOLD)
    if style == 'goldspike':
        for k, r in f.items():
            p.gradient(r, (250, 214, 110), GOLD_DEEP, noise=4)
    if style == 'grip':
        p.shaded(f, (56, 24, 26))
        for r in f.values():
            fx, fy, fw, fh = r
            for j in range(0, fh, 3):
                for i in range(fw):
                    p.px((fx + i, fy + j), (30, 12, 14))               # its binding
    if style == 'broadblade':
        for k, r in f.items():
            p.gradient(r, (214, 222, 232), (120, 128, 142), horizontal=k in ('front', 'back'))
        for k in ('right', 'left'):                                   # a dark fuller, an edge that catches the light
            fx, fy, fw, fh = f[k]
            for j in range(bg.S, fh - bg.S):
                p.px((fx + fw * 2 // 3, fy + j), (60, 70, 86))
                p.px((fx + fw * 2 // 3 + 1, fy + j), (60, 70, 86))
                p.px((fx, fy + j), (240, 244, 250))


def blend(a, b, t):
    t = max(0.0, min(1.0, t))
    return [x + (y - x) * t for x, y in zip(a, b)]


def rosette(p, rect, cx, cy, radius, base, dark, tip, turns=1.6):
    """A spiral curl seen end on, centred at cx, cy (texture pixels within `rect`): a groove winding
    in to its heart, the coil lit on its crest and from above, dark about its rim."""
    fx, fy, fw, fh = rect
    for j in range(int(cy - radius) - 1, int(cy + radius) + 2):
        for i in range(int(cx - radius) - 1, int(cx + radius) + 2):
            if not (0 <= i < fw and 0 <= j < fh):
                continue
            dx, dy = i + 0.5 - cx, j + 0.5 - cy
            r = math.hypot(dx, dy) / radius
            if r > 1.05:
                continue
            a = (math.atan2(dy, dx) / (2 * math.pi)) % 1.0
            ridge = 1 - abs((r * turns - a) % 1.0 - 0.5) * 2          # 1 on the coil's crest, 0 in its groove
            c = blend(base, dark, 0.75) if ridge < 0.28 else blend(base, tip, 0.5) if ridge > 0.78 else base
            if r > 0.9:
                c = [v * 0.72 for v in c]                             # its rim, rolling into shadow
            lit = 1.12 - 0.3 * (dy / radius + 1) / 2
            n = p.rnd.randint(-4, 4)
            p.px((fx + i, fy + j), [v * lit + n for v in c])


def strands(p, f, base, dark, tip, start, end):
    """A lock's faces in strands running along it, its end (the top of its faces) at tone `start` and
    its root at `end` along the scale tip (0) - base (0.5) - dark (1); its end lit, its root dark."""
    for k, r in f.items():
        fx, fy, fw, fh = r
        if k in ('top', 'bottom'):
            p.face(r, blend(tip, base, start * 2) if k == 'top' else dark, 1.0, noise=5)
            continue
        light = {'front': 0.95, 'back': 0.78}.get(k, 1.0)
        for i in range(fw):
            groove = fw > 7 and i % 6 == 5
            round_ = 0.72 + 0.36 * math.sin(math.pi * (i + 0.5) / fw)     # the lock rounded, dark at its edges
            off = p.rnd.uniform(-0.12, 0.12)
            for j in range(fh):
                t = max(0.0, min(1.0, start + (end - start) * j / max(1, fh - 1) + off))
                c = blend(tip, base, t * 2) if t < 0.5 else blend(base, dark, (t - 0.5) * 2)
                if groove:
                    c = [v * 0.78 for v in c]
                n = p.rnd.randint(-4, 4)
                p.px((fx + i, fy + j), [v * light * round_ + n for v in c])


def flame_marks(p, rect, dark, n=3):
    """The flame-curls of a guardian lion's hide: a few dark strokes that sweep along a face and curl up."""
    fx, fy, fw, fh = rect
    for _ in range(n):
        x, y = p.rnd.uniform(0, fw * 0.6), p.rnd.uniform(fh * 0.3, fh * 0.85)
        a, length = p.rnd.uniform(-0.4, -0.1), p.rnd.uniform(fw * 0.25, fw * 0.45)
        s = 0
        while s < length:
            for d in range(3 if s < length * 0.4 else 2 if s < length * 0.75 else 1):
                q = (int(x), int(y) + d)
                if 0 <= q[0] < fw and 0 <= q[1] < fh:
                    c = p.img.getpixel((fx + q[0], fy + q[1]))
                    p.px((fx + q[0], fy + q[1]), blend(c[:3], dark, 0.6))
            q = (int(x), int(y) - 1)                                    # lit along its upper edge
            if 0 <= q[0] < fw and 0 <= q[1] < fh:
                c = p.img.getpixel((fx + q[0], fy + q[1]))
                p.px((fx + q[0], fy + q[1]), [v * 1.18 for v in c[:3]])
            x += math.cos(a)
            y += math.sin(a)
            a -= 0.12 if s > length * 0.5 else 0.02                    # curling up at its end
            s += 1


def paint_azure_lion_true(p, style, f, dims):
    S = bg.S
    if style in ('azfur', 'azflank', 'aztail', 'azchest', 'azleg'):
        base = {'azflank': [v * 0.94 for v in AZ], 'azchest': blend(AZ, AZ_PALE, 0.35)}.get(style, AZ)
        pelt(p, f, base, AZ_DARK, AZ_PALE, along=style != 'azleg')
        if style in ('azfur', 'azflank', 'azleg'):
            for k in ('right', 'left'):
                flame_marks(p, f[k], AZ_DARK, n=max(1, f[k][2] * f[k][3] // 300))
    elif style == 'azface':
        pelt(p, f, AZ, AZ_DARK)
        fx, fy, fw, fh = f['front']
        # a dark mask about the eyes and down the sides of the face; the brow's furrows between them
        for j in range(fh):
            for i in range(fw):
                edge = abs(i + 0.5 - fw / 2) / (fw / 2)
                if j < 6 * S and edge > 0.25 or j < 2 * S:
                    c = p.img.getpixel((fx + i, fy + j))
                    p.px((fx + i, fy + j), [v * (0.5 if j < 2 * S else 0.66) for v in c[:3]])
        for j in range(0, 4 * S):
            for d in (-S, S - 1):
                p.px((fx + fw // 2 + d, fy + j), [v * 0.4 for v in AZ_DARK])
        w = fw // S
        glare(p, f, ((2, 3), (w - 3, w - 4)), 1, AZ_EMBER, AZ_CORE)
    elif style == 'azcrown':
        pelt(p, f, [v * 0.9 for v in AZ], AZ_DARK)
        for k in ('top', 'front'):                                    # the furrows of his scowl
            fx, fy, fw, fh = f[k]
            for j in range(fh):
                for d in (-S - 1, S):
                    p.px((fx + fw // 2 + d, fy + j), [v * 0.45 for v in AZ_DARK])
    elif style == 'azjowl':
        pelt(p, f, [v * 0.8 for v in AZ], AZ_DARK)
        fx, fy, fw, fh = f['front']
        for j in range(fh):
            for i in range(fw):
                if abs(i + 0.5 - fw / 2) > fw * 0.3:
                    c = p.img.getpixel((fx + i, fy + j))
                    p.px((fx + i, fy + j), [v * 0.72 for v in c[:3]])
    elif style == 'azbrow':
        for k, r in f.items():
            p.face(r, (24, 42, 48), {'top': 1.8, 'bottom': 0.45, 'front': 1.1}.get(k, 1.0), noise=3)
        fx, fy, fw, fh = f['top']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), [v * 0.7 for v in AZ_PALE])         # the light along its ridge
    elif style == 'azsnout':
        pelt(p, f, [v * 0.85 for v in AZ], AZ_DARK)
        for k in ('top', 'right', 'left'):                            # the snarl's wrinkles, deep, across the bridge
            fx, fy, fw, fh = f[k]
            if k == 'top':
                for j in range(S, fh, S * 2):
                    for i in range(fw):
                        p.px((fx + i, fy + j), [v * 0.35 for v in AZ_DARK])
                        if j + 1 < fh:
                            p.px((fx + i, fy + j + 1), [v * 1.15 for v in AZ])
            else:
                for i in range(S, fw, S * 2):
                    for j in range(fh):
                        p.px((fx + i, fy + j), [v * 0.35 for v in AZ_DARK])
    elif style == 'azmuzzle':
        pelt(p, f, blend(AZ, AZ_PALE, 0.4), AZ_DARK)
        fx, fy, fw, fh = f['front']                                   # (one pad of two: the same both sides)
        for row in (fh // 4, fh // 2 - 1):                            # whisker spots, in rows
            for i in range(S, fw - S, S + 1):
                p.px((fx + i, fy + row), [v * 0.4 for v in AZ_DARK])
        for k in ('front', 'right', 'left'):                          # the lip drawn back, black, over red gums
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                gum = k == 'front' and S <= i < fw - S or k != 'front' and i < fw // 2
                for j in range(fh - S, fh):
                    p.px((fx + i, fy + j), (98, 24, 30) if gum else (16, 10, 12))
                for j in range(fh - 2 * S, fh - S):
                    p.px((fx + i, fy + j), (16, 10, 12))
        fx, fy, fw, fh = f['bottom']                                  # the roof of his mouth, ridged
        for j in range(fh):
            for i in range(fw):
                c = THROAT if j % 3 == 0 else MOUTH
                p.px((fx + i, fy + j), [v + p.rnd.randint(-4, 4) for v in c])
    elif style == 'aznose':
        plain(p, f, (38, 42, 50))
        p.face(f['top'], (84, 92, 102), 1.0, noise=3)                 # lit along its top
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy), (84, 92, 102))
        for j in range(fh // 2, fh):                                  # narrowing to its foot
            for i in range(fw):
                if abs(i + 0.5 - fw / 2) > fw / 2 - (j - fh // 2 + 1):
                    p.px((fx + i, fy + j), blend(AZ, AZ_PALE, 0.3))
        for side in (-1, 1):                                          # the nostrils, flared
            cx = fx + fw // 2 + side * (fw // 4)
            for j in range(fh // 2, fh):
                for i in range(-S, S):
                    p.px((cx + i, fy + j), (4, 2, 4))
            p.px((cx + side * S, fy + fh // 2 - 1), (4, 2, 4))
    elif style == 'azjaw':
        pelt(p, f, blend(AZ, AZ_PALE, 0.5), AZ)
        fx, fy, fw, fh = f['top']                                     # the bed of the tongue, the throat
        for j in range(fh):
            for i in range(fw):
                c = THROAT if j > fh * 0.7 else MOUTH
                p.px((fx + i, fy + j), [v + p.rnd.randint(-4, 4) for v in c])
        for k in ('front', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for j in range(S):
                    p.px((fx + i, fy + j), (16, 10, 12))
    elif style == 'aztongue':
        for k, r in f.items():
            p.face(r, TONGUE, {'top': 1.1, 'bottom': 0.6}.get(k, 0.85), noise=5)
        fx, fy, fw, fh = f['top']
        for j in range(fh):                                           # its groove
            p.px((fx + fw // 2, fy + j), [v * 0.6 for v in TONGUE])
            p.px((fx + fw // 2 - 1, fy + j), [v * 0.7 for v in TONGUE])
    elif style == 'azteeth':                                        # a row of teeth, parted
        teeth(p, f)
        fx, fy, fw, fh = f['front']
        for i in range(0, fw, S + 1):
            for j in range(fh):
                p.px((fx + i, fy + j), (60, 20, 24))
    elif style == 'azthroat':
        plain(p, f, THROAT)
    elif style == 'azmane':
        mane(p, f, MANE_TEAL, MANE_DEEP, MANE_TIP)
        for k in ('right', 'left', 'front', 'back'):                  # curls among the locks
            fx, fy, fw, fh = f[k]
            rad = 2.6 * S
            for cy in range(int(rad), fh - int(rad) + 1, int(rad * 2.2)):
                for cx in range(int(rad) + (int(rad) if (cy // int(rad * 2.2)) % 2 else 0), fw - int(rad) + 1, int(rad * 2.4)):
                    if p.rnd.random() < 0.55:
                        rosette(p, f[k], cx, cy, rad, MANE_TEAL, MANE_DEEP, MANE_TIP)
    elif style == 'azruff':
        mane(p, f, MANE_TEAL, MANE_DEEP, MANE_TIP)
        fx, fy, fw, fh = f['front']                                   # curl on curl behind the face
        rad = 2.4 * S
        for row, cy in enumerate(range(int(rad), fh, int(rad * 1.9))):
            for cx in range(int(rad) + (int(rad) if row % 2 else 0), fw, int(rad * 2)):
                rosette(p, f['front'], cx, cy, rad, MANE_TEAL, MANE_DEEP, MANE_TIP)
    elif style == 'azcurl':
        strands(p, f, MANE_TEAL, MANE_DEEP, MANE_TIP, 0.35, 0.8)
        fx, fy, fw, fh = f['front']
        p.face(f['front'], blend(MANE_DEEP, MANE_TEAL, 0.3), 1.0, noise=2)
        rosette(p, f['front'], fw / 2, fh / 2, fw / 2, MANE_TEAL, MANE_DEEP, MANE_TIP, turns=1.5)
    elif style == 'azlock':
        strands(p, f, MANE_TEAL, MANE_DEEP, MANE_TIP, 0.4, 1.0)
    elif style == 'azlockmid':
        strands(p, f, MANE_TEAL, MANE_DEEP, MANE_TIP, 0.2, 0.5)
    elif style == 'azlocktip':
        strands(p, f, MANE_TEAL, MANE_DEEP, MANE_TIP, 0.0, 0.25)
    elif style == 'azpaw':
        pelt(p, f, [v * 0.9 for v in AZ], AZ_DARK)
        p.face(f['bottom'], (24, 22, 26), 1.0, noise=4)              # dark pads
        for k in ('front', 'top'):                                   # the toes, parted
            fx, fy, fw, fh = f[k]
            for i in range(fw // 4, fw, fw // 4):
                for j in range(fh if k == 'front' else fh // 3):
                    jj = j if k == 'front' else fh - 1 - j
                    p.px((fx + i, fy + jj), [v * 0.45 for v in AZ_DARK])
    elif style == 'azcollar':
        gold(p, f)
        for k in ('front', 'right', 'left'):                         # its edges, and jewels set along it
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), GOLD_DEEP)
            for i in range(S * 2, fw - S, S * 5):
                for a in range(S):
                    for b in range(S):
                        p.px((fx + i + a, fy + fh // 2 - S // 2 + b), (150, 20, 34) if (i // (S * 5)) % 2 else (24, 86, 150))
    elif style == 'azbell':
        for k, r in f.items():
            p.gradient(r, (250, 214, 110), GOLD_DEEP, noise=3)
        for k in ('front', 'right', 'left', 'back'):                 # a band round it, its mouth dark
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                p.px((fx + i, fy + fh // 3), GOLD_DEEP)
                p.px((fx + i, fy + fh - 1), (40, 26, 10))
        fx, fy, fw, fh = f['front']
        for j in range(fh * 2 // 3, fh - 1):
            p.px((fx + fw // 2, fy + j), (40, 26, 10))
        p.face(f['bottom'], (30, 20, 10), 1.0, noise=2)
    elif style == 'aztassel':
        for k, r in f.items():
            p.gradient(r, (160, 26, 34), (96, 12, 20), noise=4)
    else:
        lion_common(p, style, f)


# ── The White Elephant ───────────────────────────────────────────────────────
HIDE = (210, 206, 194)         # white, a little grey in the creases
HIDE_DARK = (150, 144, 132)
IVORY_ARMOUR = (226, 216, 184)
CRIMSON = (112, 18, 28)
ELEPHANT_EMBER = (112, 16, 10)  # his eyes smoulder a deep red
ELEPHANT_CORE = (150, 34, 14)


def tusk_parts(prefix, parent, at, rot, k=1.0, big=True):
    """A tusk out of the jaw, its three joints sweeping forward and up to a gold point."""
    (w1, l1), (w2, l2), cap = ((4.5, 14), (3.6, 12), 3.6) if big else ((1.8, 6), (1.4, 5), 1.6)
    w1, l1, w2, l2, cap = (v * k for v in (w1, l1, w2, l2, cap))
    return [
        part(prefix, parent, at, rot, [(-w1 / 2, 0, -w1 / 2, w1, l1, w1, 'tusk')]),
        part(prefix + '_curve', prefix, (0, l1 - w2 / 2, 0), (-0.55, 0, 0), [(-w2 / 2, 0, -w2 / 2, w2, l2, w2, 'tusk')]),
        part(prefix + '_tip', prefix + '_curve', (0, l2 - cap / 3, 0), (-0.65, 0, 0), [(-cap / 2, 0, -cap / 2, cap, cap * 1.4, cap, 'goldcap'),
                                                                          (-cap / 4, cap * 1.4, -cap / 4, cap / 2, cap, cap / 2, 'goldcap')]),
    ]


def elephant_head(parent, at, rot, trunk, ear=1.0, tusk=1.0):
    """The elephant's head, his in both shapes: a high double-domed skull under a gold war-frontlet
    crested with spikes, deep-set eyes under a scowling brow, the face narrowing to a heavy trunk
    (`trunk`: each joint's width and length), ears like ragged sails, six tusks (two great ones)."""
    parts = [
        part('head', parent, at, rot, [
            (-9, -16, -12, 18, 9, 12, 'ehide'),                # the dome of his skull
            (-8, -19, -10, 7, 3, 8, 'ehide'), (1, -19, -10, 7, 3, 8, 'ehide'),   # its two crowns
            (-8, -7, -13, 16, 9, 12, 'eface'),
            (-5.5, 2, -13, 11, 6, 9, 'ehide'),                 # narrowing to the trunk
            (-3.5, 7, -10, 7, 3, 6, 'mouth'),
            (-7, 3, -12, 3, 4, 5, 'ehide'), (4, 3, -12, 3, 4, 5, 'ehide'),        # the tusks' sheaths
            (-4, -17.5, -13.4, 8, 17, 1.4, 'frontlet'),        # the gold war-frontlet
            (-9.5, -13, -12.4, 19, 2, 1, 'gold'),              # its band across the brow
        ]),
        brow('right_ebrow', 'head', (-0.5, -7, -13.6), 8.5, 0.28, 'ebrow'),
        part('crest', 'head', (0, -17.5, -12), (-0.35, 0, 0), [(-1.25, -5, -1.25, 2.5, 5, 2.5, 'goldspike'), (-0.6, -7.5, -0.6, 1.2, 2.5, 1.2, 'goldspike')]),
        part('right_crest', 'head', (-3, -17, -12), (-0.35, 0, -0.35), [(-1, -4, -1, 2, 4, 2, 'goldspike'), (-0.5, -6, -0.5, 1, 2, 1, 'goldspike')]),
        part('right_ear', 'head', (-9, -13, -6), (0, -0.7, 0.14), [
            (-1.5, -3, 0, 1.5, 15 * ear, 20 * ear, 'bigear'),
            (-1.5, -3 + 15 * ear, 2 * ear, 1.5, 9 * ear, 15 * ear, 'bigear'),
            (-1.5, -3 + 24 * ear, 5 * ear, 1.5, 4 * ear, 8 * ear, 'bigear'),
        ]),
    ]
    prev, y = 'head', 6
    for i, (w, l) in enumerate(trunk):
        name = 'trunk%d' % (i + 1)
        boxes = [(-w / 2, 0, -w / 2, w, l, w, 'trunk')]
        if i == 0:
            boxes.append((-w / 2 - 0.4, 0, -w / 2 - 0.4, w + 0.8, 2.5, w + 0.8, 'gold'))
            boxes.append((-w / 2 + 1, 2.5, -w / 2 - 0.6, w - 2, l - 2.5, 0.6, 'goldplate'))   # the frontlet's tongue down it
        if i == len(trunk) - 1:
            boxes.append((-w / 2 - 0.3, l - 2.5, -w / 2 - 0.3, w + 0.6, 1.5, w + 0.6, 'gold'))
        parts.append(part(name, prev, (0, y, -11.5) if i == 0 else (0, trunk[i - 1][1], 0), (-0.05, 0, 0), boxes))
        prev, y = name, l
    parts += tusk_parts('right_tusk', 'head', (-5.5, 6, -11), (-1.4, 0, 0.2), k=tusk)
    parts += tusk_parts('right_tusk_high', 'head', (-6.5, 3, -12), (-1.0, 0, 0.45), k=tusk, big=False)
    parts += tusk_parts('right_tusk_low', 'head', (-2.5, 8.5, -10), (-0.7, 0, 0.1), k=tusk, big=False)
    return parts


# His demon-king shape: broader and heavier than the Lion, in ivory plate edged in gold over white
# hide, a gold wheel on his breast, a crimson mantle, great round pauldrons; the elephant's head;
# pillar legs; a giant's lance, held upright.
WHITE_ELEPHANT = mirrored([
    part('body', 'root', (0, 3, 0), (0.08, 0, 0), [
        (-10.5, -12, -7, 21, 12, 14, 'ivoryplate'),
        (-14, -27, -8.5, 28, 16, 17, 'ivorychest'),
        (-9, -25, -9.1, 18, 12, 1, 'goldplate'),
        (-12, -3, -7.6, 24, 3, 15.2, 'gold'),
        (-12.5, 0, -7.8, 25, 10, 15.6, 'skirt'),
        (-12, -30, -7, 24, 4, 14, 'gold'),                     # the gorget
        (-13, -27, 8.5, 26, 28, 2, 'mantle'),
    ]),
] + elephant_head('body', (0, -30, -6), (0.12, 0, 0), ((6.4, 8), (5.4, 8), (4.4, 8), (3.4, 7))) + [
    part('right_arm', 'body', (-18, -24, -0.5), (0, 0, 0), [
        (-5, -2, -5, 10, 13, 10, 'ehide'),
        (-8, -7, -7.5, 15, 9, 15, 'epauldron'),
        (-8.5, -1, -7, 8, 5, 14, 'epauldron'),
        (-8.6, -4.5, -2.5, 1, 5, 5, 'goldboss'),
    ]),
    part('right_forearm', 'right_arm', (0, 11, 0), (-0.2, 0, 0), [
        (-5.5, 0, -5.5, 11, 9, 11, 'bracer'),
        (-5, 9, -5.2, 10, 5, 10.4, 'ehide'),
    ]),
    part('lance', 'right_forearm', (0, 11.5, 0), (0.7, 0, 0), [
        (-1, -48, -1, 2, 66, 2, 'shaft'),
        (-1.2, -64, -3, 2.4, 16, 6, 'lanceblade'),
        (-0.8, -70, -1.8, 1.6, 6, 3.6, 'lanceblade'),
        (-2, -49, -2, 4, 2, 4, 'gold'),
        (-1.8, -47, -1.8, 3.6, 5, 3.6, 'tassel'),
        (-1.4, 17, -1.4, 2.8, 2, 2.8, 'gold'),
    ]),
    part('right_leg', 'root', (-7, 5, 0.5), (0, 0, 0), [
        (-5.5, 0, -5.5, 11, 15, 11, 'ehide'),
        (-6, 3, -6, 12, 6, 12, 'kneeplate'),
        (-6, 15, -6, 12, 4, 12, 'efoot'),
    ]),
])

# His true form, Samantabhadra's white elephant: towering, a war elephant, the weight of him in a
# great humped forequarter; a crimson saddle-cloth edged and fringed with gold, the Bodhisattva's
# seat on his back, empty, its posts gold; a gold breast-band and bell; pillar legs ringed with
# gold; the elephant's head, larger, its trunk to the ground and its two great tusks longer than a man.
WHITE_ELEPHANT_TRUE = mirrored([
    part('body', 'root', (0, -2, 2), (0, 0, 0), [
        (-14, -16, -21, 28, 24, 48, 'barrel'),
        (-13, -20, -18, 26, 5, 18, 'ehide'),                   # the hump of his shoulders
        (-15, -18, -10, 30, 15, 29, 'caparison'),
        (-15.4, -4, -10.4, 30.8, 2, 29.8, 'fringe'),
        (-7, -21, -6, 14, 3, 14, 'seat'),                      # the Bodhisattva's seat, empty
        (-14.6, -10, -21, 29.2, 6, 2, 'gold'),                 # the breast-band
        (-2, -4.5, -22.5, 4, 4, 3, 'gold'),                    # its bell
    ]),
    part('right_seat_post', 'body', (-6.5, -21, -5.5), (0, 0, 0), [(-0.75, -7, -0.75, 1.5, 7, 1.5, 'gold')]),
    part('right_seat_post2', 'body', (-6.5, -21, 7.5), (0, 0, 0), [(-0.75, -7, -0.75, 1.5, 7, 1.5, 'gold')]),
    part('seat_canopy', 'body', (0, -28, 1), (0, 0, 0), [(-8, -1.5, -8, 16, 1.5, 16, 'canopy'), (-1, -4.5, -1, 2, 3, 2, 'goldspike')]),
] + elephant_head('body', (0, -12, -20), (0.12, 0, 0), ((7.6, 9), (6.6, 8), (5.6, 8), (4.6, 7), (3.6, 6)), ear=1.15, tusk=1.15) + [
    part('right_foreleg', 'body', (-8.5, 8, -11), (0, 0, 0), [
        (-6, -2, -6, 12, 18, 12, 'ehide'),
        (-6.4, 9, -6.4, 12.8, 2, 12.8, 'gold'),
        (-6.5, 16, -6.5, 13, 2, 13, 'efoot'),
    ]),
    part('right_hindleg', 'body', (-8.5, 8, 20), (0, 0, 0), [
        (-6, -2, -6, 12, 18, 12, 'ehide'),
        (-6.4, 9, -6.4, 12.8, 2, 12.8, 'gold'),
        (-6.5, 16, -6.5, 13, 2, 13, 'efoot'),
    ]),
    part('tail', 'body', (0, -12, 27), (0.35, 0, 0), [(-1, 0, -1, 2, 18, 2, 'ehide'), (-1.6, 16, -1.6, 3.2, 5, 3.2, 'tuft')]),
])


def elephant_common(p, style, f):
    """What the Elephant's two shapes share: hide, face, ears, trunk, tusks, gold. Returns whether it painted."""
    if style in ('ehide', 'barrel'):
        hide(p, f, HIDE)
    elif style == 'eface':
        hide(p, f, HIDE)
        fx, fy, fw, fh = f['front']
        # deep-set eyes in the brow's shadow either side of the frontlet; creases down to the trunk
        w = fw // bg.S
        for j in range(fh):
            for i in range(fw):
                edge = abs(i + 0.5 - fw / 2) / (fw / 2)
                if edge > 0.3 and j < 6 * bg.S:
                    c = p.img.getpixel((fx + i, fy + j))
                    p.px((fx + i, fy + j), [v * (0.6 if j < 3 * bg.S else 0.8) for v in c[:3]])
        glare(p, f, ((2, 3), (w - 3, w - 4)), 1, ELEPHANT_EMBER, ELEPHANT_CORE)
        for j in range(5 * bg.S, fh, bg.S * 2):
            for i in range(fw // 3, fw * 2 // 3):
                p.px((fx + i, fy + j), [v * 0.74 for v in HIDE])
    elif style == 'ebrow':
        hide(p, f, [v * 0.84 for v in HIDE])
        for k in ('front', 'bottom'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for j in range(fh - bg.S, fh) if k == 'front' else range(fh):
                    p.px((fx + i, fy + j), [v * 0.38 for v in HIDE_DARK])
    elif style == 'mouth':
        hide(p, f, HIDE_DARK)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            for j in range(bg.S):
                p.px((fx + i, fy + j), (70, 30, 34))
    elif style == 'trunk':
        hide(p, f, HIDE)
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(bg.S, fh, bg.S * 2):
                for i in range(fw):
                    p.px((fx + i, fy + j), HIDE_DARK)                 # its rings
    elif style == 'bigear':
        hide(p, f, HIDE)
        hide(p, {'left': f['left']}, (196, 172, 166))                 # within, toward his head: pinker
        for k in ('right', 'left'):                                   # veins branching over it, its edge darker
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for d in range(bg.S):
                    p.px((fx + i, fy + fh - 1 - d), HIDE_DARK)
            for j in range(fh):
                for d in range(bg.S):
                    p.px((fx + fw - 1 - d, fy + j), HIDE_DARK)
            for v in range(3):
                x, y = fx + p.rnd.randrange(fw // 4), fy + p.rnd.randrange(fh)
                for _ in range(fw):
                    if fx <= x < fx + fw and fy <= y < fy + fh:
                        p.px((x, y), (160, 132, 128))
                    x += 1
                    y += p.rnd.choice((-1, 0, 0, 1))
    elif style == 'tusk':
        for k, r in f.items():
            p.gradient(r, (240, 232, 208), (200, 186, 150), noise=3)
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(0, fh, bg.S * 3):
                for i in range(fw):
                    p.px((fx + i, fy + j), (186, 170, 132))           # the grain
    elif style in ('goldcap', 'goldspike'):
        for k, r in f.items():
            p.gradient(r, (250, 222, 130), GOLD_DEEP, noise=3)
    elif style == 'frontlet':
        gold(p, f)
        fx, fy, fw, fh = f['front']
        for j in range(fh):                                           # its raised edges and ribs
            for d in range(bg.S):
                p.px((fx + d, fy + j), GOLD_DEEP)
                p.px((fx + fw - 1 - d, fy + j), GOLD_DEEP)
        for j in range(bg.S * 4, fh, bg.S * 4):
            for i in range(fw):
                p.px((fx + i, fy + j), GOLD_DEEP)
        cx, cy = fx + fw // 2, fy + fh // 3
        for j in range(-bg.S, bg.S):
            for i in range(-bg.S, bg.S):
                p.px((cx + i, cy + j), (130, 16, 30))                 # a jewel, dark red
    elif style == 'goldplate':
        gold(p, f)
    elif style == 'efoot':
        hide(p, f, HIDE_DARK)
        fx, fy, fw, fh = f['front']
        for i in range(bg.S, fw - bg.S * 2, bg.S * 4):
            for di in range(bg.S * 2):
                for j in range(fh - bg.S * 2, fh):
                    p.px((fx + i + di, fy + j), (232, 224, 200))      # toenails
    elif style == 'gold':
        gold(p, f)
    else:
        return False
    return True


def paint_white_elephant(p, style, f, dims):
    if elephant_common(p, style, f):
        return
    if style in ('ivoryplate', 'ivorychest', 'skirt', 'kneeplate', 'epauldron'):
        bands(p, f, IVORY_ARMOUR, (120, 104, 72), GOLD, rivet=GOLD_DEEP, band=4 if style in ('skirt', 'epauldron') else 5)
        trim(p, f, GOLD)
    if style == 'goldplate':                                          # a gold wheel of the law on his breast
        for r in f.values():
            p.face(r, (150, 26, 34), 1.0, noise=4)
        fx, fy, fw, fh = f['front']
        for j in range(fh):
            for i in range(fw):
                dx, dy = (i + 0.5 - fw / 2) / (fh / 2), (j + 0.5 - fh / 2) / (fh / 2)
                d = math.hypot(dx, dy)
                spoke = d < 0.8 and min(abs(dx), abs(dy), abs(abs(dx) - abs(dy))) < 0.09
                if abs(d - 0.8) < 0.11 or d < 0.2 or spoke:
                    p.px((fx + i, fy + j), GOLD)
    if style == 'goldboss':
        gold(p, f)
    if style == 'bracer':
        gold(p, f)
        trim(p, f, GOLD_DEEP)
    if style == 'mantle':
        for k, r in f.items():
            p.face(r, CRIMSON, {'back': 0.9, 'front': 0.6}.get(k, 0.8), noise=5, fall=0.25)
        trim(p, f, GOLD, top=False)
        fx, fy, fw, fh = f['back']
        for j in range(bg.S * 3, fh - bg.S * 3, bg.S * 5):          # cloud scrolls in gold
            for i in range(fw):
                if (i // bg.S + j // bg.S) % 4 == 0:
                    p.px((fx + i, fy + j), GOLD_DEEP)
    if style == 'shaft':
        p.shaded(f, (100, 22, 26))
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(0, fh, bg.S * 12):
                for i in range(fw):
                    p.px((fx + i, fy + j), GOLD)
    if style == 'lanceblade':
        for k, r in f.items():
            p.gradient(r, (220, 226, 236), (120, 128, 142), horizontal=k in ('front', 'back'))
    if style == 'tassel':
        plain(p, f, (160, 22, 30))


def paint_white_elephant_true(p, style, f, dims):
    if elephant_common(p, style, f):
        return
    if style == 'caparison':                                           # a crimson saddle-cloth, gold-bordered
        for k, r in f.items():
            p.face(r, CRIMSON, {'top': 1.05}.get(k, 0.92), noise=5, fall=0.15)
        trim(p, f, GOLD)
        for k in ('right', 'left'):                                   # a gold lattice over it, a border within the edge
            fx, fy, fw, fh = f[k]
            for j in range(bg.S * 3, fh - bg.S * 3):
                for i in range(bg.S * 3, fw - bg.S * 3):
                    inner = i < bg.S * 4 or i >= fw - bg.S * 4 or j < bg.S * 4 or j >= fh - bg.S * 4
                    lattice = (i + j) % (bg.S * 6) == 0 or (i - j) % (bg.S * 6) == 0
                    if inner and (i < bg.S * 3 + 2 or i >= fw - bg.S * 3 - 2 or j < bg.S * 3 + 2 or j >= fh - bg.S * 3 - 2):
                        p.px((fx + i, fy + j), GOLD_DEEP)
                    elif not inner and lattice:
                        p.px((fx + i, fy + j), GOLD_DEEP)
    if style == 'fringe':
        for k, r in f.items():
            fx, fy, fw, fh = r
            for i in range(fw):
                for j in range(fh):
                    p.px((fx + i, fy + j), GOLD if (i // bg.S) % 2 == 0 else GOLD_DEEP)
    if style == 'seat':                                               # the Bodhisattva's seat: crimson, gold-edged
        for k, r in f.items():
            p.face(r, (96, 14, 22), {'top': 1.1}.get(k, 0.9), noise=4)
        trim(p, f, GOLD)
    if style == 'canopy':
        for k, r in f.items():
            p.face(r, (96, 14, 22), {'top': 1.05, 'bottom': 0.7}.get(k, 0.95), noise=4)
        trim(p, f, GOLD)
    if style == 'tuft':
        plain(p, f, (70, 64, 60))


def extent(parts, k):
    """How far the model reaches, drawn at `k` times its pixels, in blocks: its height, its breadth, how
    far it runs ahead of and behind its feet's middle (the parts at rest)."""
    def mul(a, b):
        return [[sum(a[i][t] * b[t][j] for t in range(3)) for j in range(3)] for i in range(3)]

    def apply(m, v):
        return [sum(m[i][t] * v[t] for t in range(3)) for i in range(3)]

    def rot(rx, ry, rz):
        cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
        x = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
        y = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
        z = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
        return mul(mul(z, y), x)

    frames = {}
    lo, hi = [1e9] * 3, [-1e9] * 3
    eye = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    for name, parent, pivot, r, boxes, _ in parts:
        pm, pt = frames.get(parent, (eye, [0, 0, 0]))
        m = mul(pm, rot(*r))
        t = [a + b for a, b in zip(pt, apply(pm, pivot))]
        frames[name] = (m, t)
        for b in boxes:
            for cx_ in (b[0], b[0] + b[3]):
                for cy_ in (b[1], b[1] + b[4]):
                    for cz_ in (b[2], b[2] + b[5]):
                        q = [a + c for a, c in zip(t, apply(m, (cx_, cy_, cz_)))]
                        lo = [min(a, c) for a, c in zip(lo, q)]
                        hi = [max(a, c) for a, c in zip(hi, q)]
    return {'height': (24 - lo[1]) * k / 16, 'width': (hi[0] - lo[0]) * k / 16,
            'ahead': -lo[2] * k / 16, 'behind': hi[2] * k / 16}


# How much larger than their pixels each is drawn: the renderers' SCALE and TRUE_SCALE
KINGS = {'azure_lion': 1.15, 'azure_lion_true': 1.7, 'white_elephant': 1.08, 'white_elephant_true': 1.55}

if __name__ == '__main__':
    for name, parts, painter_fn, java, seed, units, tag in (
            ('lion_demon', LION_DEMON, paint_lion_demon, 'LionDemonRenderer.java', 21, 128, ''),
            ('elephant_demon', ELEPHANT_DEMON, paint_elephant_demon, 'ElephantDemonRenderer.java', 22, 128, ''),
            ('peng_demon', PENG_DEMON, paint_peng_demon, 'PengDemonRenderer.java', 23, 128, ''),
            ('little_wind_driller', WIND_DRILLER, paint_wind_driller, 'LittleWindDrillerRenderer.java', 24, 128, ''),
            ('azure_lion', AZURE_LION, paint_azure_lion, 'AzureLionRenderer.java', 25, 256, ''),
            ('azure_lion_true', AZURE_LION_TRUE, paint_azure_lion_true, 'AzureLionRenderer.java', 27, 256, 'true form'),
            ('white_elephant', WHITE_ELEPHANT, paint_white_elephant, 'WhiteElephantRenderer.java', 26, 256, ''),
            ('white_elephant_true', WHITE_ELEPHANT_TRUE, paint_white_elephant_true, 'WhiteElephantRenderer.java', 28, 256, 'true form')):
        bg.UNITS = units
        bg.TEX = units * bg.S
        p = Painter(seed)
        slots = paint(parts, p, painter_fn)
        p.save(name)
        emit(parts, slots, R + java, tag)
        print(name, len(parts), 'parts,', len(slots), 'boxes')
        if name in KINGS:
            print('   drawn at', KINGS[name], {k: round(v, 2) for k, v in extent(parts, KINGS[name]).items()})
