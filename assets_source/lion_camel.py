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


# ── The Azure Lion ────────────────────────────────────────────────────────────
AZURE = (52, 104, 170)
AZURE_DARK = (26, 58, 110)
LION_FACE = (198, 160, 98)
LION_FACE_DARK = (140, 104, 58)
LACQUER = (34, 30, 40)

AZURE_LION = mirrored([
    part('body', 'root', (0, 4, 0), (0, 0, 0), [
        (-8, -22, -4.5, 16, 22, 9, 'lacquer'),
        (-6, -21, -5.1, 12, 10, 0.8, 'breastplate'),
        (-8.6, -6, -5.1, 17.2, 3, 10.2, 'gold'),
        (-8.4, -3, -4.9, 16.8, 8, 9.8, 'tassets'),
        (-7.5, -23, 4.2, 15, 13, 2.5, 'azuremane'),
    ]),
    part('head', 'body', (0, -22, -1), (0, 0, 0), [
        (-6, -11, -7, 12, 11, 10, 'lionface'),
        (-4.5, -6, -11, 9, 5, 4.5, 'muzzle'),
        (-1.75, -7, -11.6, 3.5, 2, 1.6, 'nose'),
        (-9, -15, -3, 18, 18, 9, 'azuremane'),
        (-7, -16.5, -6, 14, 4, 7, 'azuremane'),
        (-9.5, -8, -6, 3, 8, 4, 'azuremane'),
        (6.5, -8, -6, 3, 8, 4, 'azuremane'),
    ]),
    part('jaw', 'head', (0, -1, -5), (0.05, 0, 0), [(-4, 0, -5.5, 8, 2.5, 6, 'jaw'), (-5, 2, -4.5, 10, 4, 5, 'azuremane')]),
    part('right_ear', 'head', (-5, -14, -2), (0, 0, -0.4), [(-1.5, -3, -1, 3, 3, 2, 'ear')]),
    part('right_arm', 'body', (-10.5, -19, 0), (0, 0, 0), [
        (-3, -2, -3, 6, 19, 6, 'lionarm'), (-4, -4, -4, 8, 6, 8, 'pauldron'), (-3.5, 12, -3.5, 7, 4, 7, 'gold')]),
    part('sword', 'right_arm', (0, 16, 0), (1.5708, 0, 0), [
        (-0.75, -3, -0.75, 1.5, 8, 1.5, 'grip'), (-2, -4, -2, 4, 1.2, 4, 'gold'),
        (-0.5, -30, -2.8, 1, 26, 5, 'broadblade'), (-0.5, -33, -1.8, 1, 3, 3.6, 'broadblade')]),
    part('right_leg', 'root', (-4, 4, 0), (0, 0, 0), [(-3, 0, -3, 6, 17, 6, 'greave'), (-3.4, 17, -4.6, 6.8, 3, 7.4, 'lionpaw')]),
    part('tail', 'body', (0, -3, 4.5), (0.7, 0, 0), [(-0.8, -0.8, 0, 1.6, 1.6, 14, 'tailfur'), (-2.2, -2.2, 12.5, 4.4, 4.4, 5, 'azuremane')]),
])


def paint_azure_lion(p, style, f, dims):
    if style == 'lionface':
        lion_face(p, f, LION_FACE, LION_FACE_DARK, AZURE, (255, 214, 70))
        fx, fy, fw, fh = f['front']
        for i in range(2, fw - 2):                               # a band of azure over the brow
            p.px((fx + i, fy + 1), AZURE_DARK)
            p.px((fx + i, fy + 2), AZURE)
    if style == 'muzzle':
        shade_fur(p, f, [c * 1.06 for c in LION_FACE], LION_FACE_DARK, 0.05)
        fx, fy, fw, fh = f['front']
        for i in range(1, fw - 1):
            p.px((fx + i, fy + fh - 2), (90, 24, 24))
        for i in (2, 3, fw - 4, fw - 3):                          # fangs
            p.px((fx + i, fy + fh - 1), IVORY)
            p.px((fx + i, fy + fh - 2), IVORY)
    if style == 'nose':
        plain(p, f, (36, 26, 30))
    if style == 'jaw':
        shade_fur(p, f, LION_FACE, LION_FACE_DARK, 0.05)
        fx, fy, fw, fh = f['top']                                # the inside of the mouth: red, teeth round it
        for j in range(fh):
            for i in range(fw):
                edge = i == 0 or i == fw - 1 or j == 0
                p.px((fx + i, fy + j), IVORY if edge and (i + j) % 2 == 0 else (150, 30, 40))
    if style == 'azuremane':
        shade_fur(p, f, AZURE, AZURE_DARK, streak=0.4)
        for k, r in f.items():
            fx, fy, fw, fh = r
            for _ in range(fw * fh // 30):
                p.px((fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)), (120, 176, 230))   # glints
    if style == 'ear':
        plain(p, f, LION_FACE_DARK)
    if style in ('lionarm', 'tailfur'):
        shade_fur(p, f, LION_FACE, LION_FACE_DARK, 0.15)
    if style == 'lionpaw':
        shade_fur(p, f, LION_FACE, LION_FACE_DARK, 0.1)
        fx, fy, fw, fh = f['front']
        for i in range(1, fw, 3):
            p.px((fx + i, fy + fh - 1), IVORY)
            p.px((fx + i, fy + fh - 2), IVORY)
    if style in ('lacquer', 'greave'):
        for k, r in f.items():
            lamellar(p, r, LACQUER, (16, 14, 20))
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                p.px((fx + i, fy), GOLD_DEEP)
    if style == 'breastplate':                                   # a lion's face in gold on his chest
        for r in f.values():
            p.face(r, (60, 54, 70), 1.0, noise=4)
        fx, fy, fw, fh = f['front']
        cx, cy = fx + fw / 2, fy + fh / 2
        for j in range(fh):
            for i in range(fw):
                d = math.hypot(i + fx - cx + 0.5, j + fy - cy + 0.5)
                if abs(d - fh * 0.38) < 1.1:
                    p.px((fx + i, fy + j), GOLD, glow=True)
        for ex in (-3, 2):
            p.px((cx + ex, cy - 1), GOLD, glow=True)
            p.px((cx + ex + 1, cy - 1), GOLD, glow=True)
    if style in ('pauldron', 'tassets'):
        for r in f.values():
            lamellar(p, r, (40, 64, 110), (20, 34, 66))
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), GOLD)
    if style == 'gold':
        gold(p, f)
    if style == 'grip':
        p.shaded(f, (60, 30, 30))
    if style == 'broadblade':
        for k, r in f.items():
            p.gradient(r, (226, 232, 240), (140, 148, 160), horizontal=k in ('front', 'back'))
        for k in ('right', 'left'):                               # an azure fuller down the blade
            fx, fy, fw, fh = f[k]
            for j in range(2, fh - 2):
                p.px((fx + fw // 2, fy + j), (90, 140, 200), glow=True)


# ── The White Elephant ───────────────────────────────────────────────────────
WHITE = (232, 228, 216)
WHITE_DARK = (196, 190, 176)
ROBE = (40, 52, 110)

WHITE_ELEPHANT = mirrored([
    part('body', 'root', (0, 4, 0), (0, 0, 0), [
        (-9, -24, -5.5, 18, 24, 11, 'robe'),
        (-7.5, -25, -5.2, 15, 3, 10.4, 'gold'),
        (-9.6, -7, -6, 19.2, 3, 12, 'gold'),
        (-9.4, -4, -5.8, 18.8, 8, 11.6, 'robeskirt'),
        (-2, -21, -6, 4, 4, 0.6, 'jewel'),
    ]),
    part('head', 'body', (0, -25, -1), (0, 0, 0), [
        (-6.5, -13, -7.5, 13, 13, 11, 'ivoryface'),
        (-5, -16, -5, 10, 3, 8, 'crown'),
        (-1, -18, -2, 2, 2, 2, 'jewel'),
        (-5, -3, -14, 1.6, 1.6, 7, 'tusk'), (3.4, -3, -14, 1.6, 1.6, 7, 'tusk'),
        (-6.3, -5.5, -11, 1.4, 1.4, 4, 'tusk'), (4.9, -5.5, -11, 1.4, 1.4, 4, 'tusk'),
        (-4.2, -0.8, -11, 1.3, 1.3, 3.5, 'tusk'), (2.9, -0.8, -11, 1.3, 1.3, 3.5, 'tusk'),
    ]),
    part('right_ear', 'head', (-6.5, -12, -3), (0, -0.4, 0), [(-1, 0, 0, 1, 15, 13, 'bigear')]),
    part('trunk1', 'head', (0, -5, -7.5), (0.1, 0, 0), [(-2, 0, -2, 4, 8, 4, 'trunk')]),
    part('trunk2', 'trunk1', (0, 8, 0), (0.15, 0, 0), [(-1.75, 0, -1.75, 3.5, 8, 3.5, 'trunk')]),
    part('trunk3', 'trunk2', (0, 8, 0), (0.2, 0, 0), [(-1.5, 0, -1.5, 3, 7, 3, 'trunk'), (-1.8, 6, -1.8, 3.6, 1.6, 3.6, 'gold')]),
    part('right_arm', 'body', (-11.5, -21, 0), (0, 0, 0), [
        (-3.5, -2, -3.5, 7, 21, 7, 'ivoryarm'), (-4.4, -4, -4.4, 8.8, 6, 8.8, 'gold'), (-3.9, 14, -3.9, 7.8, 3, 7.8, 'gold')]),
    part('lance', 'right_arm', (0, 18, 0), (1.5708, 0, 0), [
        (-0.6, -36, -0.6, 1.2, 52, 1.2, 'shaft'), (-0.7, -44, -1.6, 1.4, 8, 3.2, 'lanceblade'), (-1.3, -36.5, -1.3, 2.6, 2, 2.6, 'tassel')]),
    part('right_leg', 'root', (-4.8, 4, 0), (0, 0, 0), [(-3.5, 0, -3.5, 7, 17, 7, 'ivoryleg'), (-4, 17, -4, 8, 3, 8, 'foot')]),
])


def paint_white_elephant(p, style, f, dims):
    if style in ('ivoryarm', 'ivoryleg', 'trunk'):
        hide(p, f, WHITE)
    if style == 'trunk':
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(1, fh, 3):
                for i in range(fw):
                    p.px((fx + i, fy + j), WHITE_DARK)
    if style == 'ivoryface':
        elephant_face(p, f, WHITE, (90, 170, 255))
        fx, fy, fw, fh = f['front']                              # a jewelled mark on his brow
        for i in range(fw // 2 - 1, fw // 2 + 1):
            for j in range(2, 4):
                p.px((fx + i, fy + j), (220, 40, 60), glow=True)
    if style == 'bigear':
        hide(p, f, WHITE)
        for k in ('right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(fh):
                for i in range(fw):
                    if (i - fw / 2) ** 2 / (fw / 2) ** 2 + (j - fh / 2) ** 2 / (fh / 2) ** 2 < 0.45:
                        p.px((fx + i, fy + j), (236, 196, 190))
                    elif (i - fw / 2) ** 2 / (fw / 2) ** 2 + (j - fh / 2) ** 2 / (fh / 2) ** 2 > 0.9:
                        p.px((fx + i, fy + j), GOLD_DEEP)
    if style == 'tusk':
        for k, r in f.items():
            p.face(r, IVORY, 1.0, noise=3)
        fx, fy, fw, fh = f['front']                               # gold caps
        p.face(f['front'], GOLD, 1.0, noise=3, glow=True)
    if style == 'crown':
        gold(p, f)
        fx, fy, fw, fh = f['front']
        for i in range(1, fw - 1, 4):
            for j in range(1, fh - 1):
                p.px((fx + i, fy + j), (60, 140, 220), glow=True)
    if style == 'jewel':
        plain(p, f, (220, 40, 60), glow=True)
    if style == 'gold':
        gold(p, f)
    if style in ('robe', 'robeskirt'):
        for k, r in f.items():
            p.face(r, ROBE, {'top': 1.08, 'bottom': 0.72, 'front': 1.0, 'back': 0.86}.get(k, 0.94), noise=6, fall=0.2)
        for k in ('front', 'back', 'right', 'left'):            # cloud scrolls in gold
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for j in range(fh):
                    if (i + 2 * j) % 11 == 0 and (j // 5) % 2 == 0:
                        p.px((fx + i, fy + j), GOLD_DEEP)
    if style == 'foot':
        hide(p, f, WHITE_DARK)
        fx, fy, fw, fh = f['front']
        for i in range(1, fw - 1, 4):
            for di in range(2):
                p.px((fx + i + di, fy + fh - 2), GOLD)
    if style == 'shaft':
        p.shaded(f, (120, 30, 30))
        for k in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(0, fh, 12):
                for i in range(fw):
                    p.px((fx + i, fy + j), GOLD)
    if style == 'lanceblade':
        blade(p, f)
    if style == 'tassel':
        plain(p, f, (200, 30, 36))


if __name__ == '__main__':
    for name, parts, painter_fn, java, seed, units in (
            ('lion_demon', LION_DEMON, paint_lion_demon, 'LionDemonRenderer.java', 21, 128),
            ('elephant_demon', ELEPHANT_DEMON, paint_elephant_demon, 'ElephantDemonRenderer.java', 22, 128),
            ('peng_demon', PENG_DEMON, paint_peng_demon, 'PengDemonRenderer.java', 23, 128),
            ('little_wind_driller', WIND_DRILLER, paint_wind_driller, 'LittleWindDrillerRenderer.java', 24, 128),
            ('azure_lion', AZURE_LION, paint_azure_lion, 'AzureLionRenderer.java', 25, 256),
            ('white_elephant', WHITE_ELEPHANT, paint_white_elephant, 'WhiteElephantRenderer.java', 26, 256)):
        bg.UNITS = units
        bg.TEX = units * bg.S
        p = Painter(seed)
        slots = paint(parts, p, painter_fn)
        p.save(name)
        emit(parts, slots, R + java)
        print(name, len(parts), 'parts,', len(slots), 'boxes')
