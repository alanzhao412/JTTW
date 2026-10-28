"""The goblin (山魈小妖), a mountain imp of the open world. As wild_paint.py says: its model is written
into WildRenderers' goblinLayer() between its GENERATED markers, its skin (textures/entity/goblin.png)
painted to the same box-UV layout, its eyes glowing (goblin_glow.png).

A small, wiry, hunched imp, all elbows and knees: moss-green skin blotched darker and warty, ribs
showing over a little pot belly; a big head thrust forward with long drooping ears (a bronze ring in
the right one), a hooked nose ruddy at its tip, a heavy brow over yellow glaring eyes, a wide grin of
crooked teeth, two horn-nubs and a black topknot tied with red cord, ragged black hair behind. A
patched hemp robe hanging open, torn at its hem and sleeves, a rope belt, a loin-wrap; a string of
wooden prayer beads (red ones among them) with a stolen talisman hanging from it, another stuck
askew on its head; a strap across its chest to a sack of thefts on its back, a scroll and a stick
poking out of it; a gourd at its hip; its shins and forearms bound in rags; long clawed hands and
feet. A crude bronze dagger, green with age and notched, in its right fist; a sling and its stone
dangling from its left.

Run from the repository root: python assets_source/goblin.py"""
import math

import beastgen as bg
import wild_paint as w
from wild_paint import part, mirrored, aim, paint, Painter, k, mix

bg.UNITS = 128
bg.TEX = bg.UNITS * bg.S

SIZE = 0.68          # GoblinRenderer.SIZE

SKIN = (104, 134, 58)
SKIN_SHADE = (52, 76, 36)
SKIN_LIGHT = (164, 184, 92)
BELLY = (150, 160, 92)
RUDDY = (168, 96, 70)
ROBE = (134, 98, 56)
ROBE_DEEP = (72, 50, 30)
PATCHES = ((88, 72, 86), (70, 90, 96), (156, 128, 76), (112, 64, 44), (84, 96, 62))
STITCH = (216, 196, 150)
ROPE = (174, 146, 92)
WRAP = (176, 160, 124)
WRAP_DEEP = (112, 98, 74)
BEAD = (112, 66, 34)
BEAD_RED = (156, 32, 26)
BRONZE = (172, 124, 58)
BRONZE_DEEP = (92, 60, 30)
VERDIGRIS = (74, 138, 108)
EYE = (255, 206, 52)
TOOTH = (226, 212, 164)
GUM = (96, 26, 30)
HAIR = (44, 38, 38)
HAIR_GREY = (118, 110, 102)
CORD = (176, 34, 28)
SACK = (150, 128, 88)
LEATHER = (96, 64, 40)
CLAW = (40, 34, 26)

BODY = (0.32, 0, 0)
HEAD = (-0.3, 0, 0)
ARM, FOREARM, HAND = (-0.3, 0, 0.2), (-0.75, 0, 0), (0.1, 0, 0)
LARM, LFOREARM, LHAND = (-0.15, 0, -0.22), (-0.45, 0, 0), (0, 0, 0)


def beads():
    """The string of prayer beads round its neck, down over its chest: wooden, a red one every third."""
    out = []
    n = 11
    for i in range(n):
        a = -1.25 + 2.5 * i / (n - 1)
        x, y = 3.0 * math.sin(a), -11.4 + 3.2 * math.cos(a) ** 0.6 * (1 - abs(a) / 1.6)
        out.append((x - 0.55, y - 0.55, -2.95 - 0.25 * math.cos(a), 1.1, 1.1, 1.1, 'bead_red' if i % 3 == 1 else 'bead'))
    return out


GOBLIN = mirrored([
    # the legs: a knobbly thigh, a shin bound in rags, a long clawed foot; crouched, the knee forward
    part('right_leg', 'root', (-2.3, 12.6, 0.4), (-0.42, 0, 0.06), [
        (-1.35, -0.6, -1.4, 2.7, 5.8, 2.8, 'thigh'),
        (-1.55, 4.1, -1.75, 3.1, 1.9, 2.6, 'knee')]),
    part('right_shin', 'right_leg', (0, 5.4, 0.1), (0.78, 0, 0), [
        (-1.05, 0, -1.05, 2.1, 5.6, 2.1, 'shin'),
        (-1.35, 0.8, -1.35, 2.7, 3.4, 2.7, 'legwrap')]),
    part('right_foot', 'right_shin', (0, 5.4, 0), (-0.36, 0, 0), [
        (-1.5, -0.2, -3.4, 3, 1.5, 4.6, 'foot'),
        (-1.55, 0.35, -4.6, 0.8, 1.0, 1.4, 'toe'),
        (-0.4, 0.35, -4.8, 0.8, 1.0, 1.5, 'toe'),
        (0.75, 0.35, -4.5, 0.8, 1.0, 1.3, 'toe'),
        (-0.6, 0.6, 0.9, 1.2, 0.8, 1.2, 'heel')]),

    # the body from the hips up, hunched forward over its belly; its pivot the hips
    part('body', 'root', (0, 12.6, 0.4), BODY, [
        (-3.2, -2.4, -2.3, 6.4, 3.4, 4.6, 'loinwrap'),
        (-3.0, -7.0, -2.65, 6.0, 4.9, 5.0, 'belly'),
        (-3.7, -11.4, -2.3, 7.4, 4.8, 4.6, 'chest'),
        (-4.3, -11.9, -2.1, 8.6, 1.6, 4.3, 'yoke'),
        (-4.0, -11.6, 1.9, 8.0, 10.8, 0.9, 'robe_back'),
        (-3.55, -3.3, -2.95, 7.1, 1.3, 5.6, 'rope'),
        (-1.4, -12.6, -1.7, 2.8, 1.6, 2.8, 'neck'),
        (-1.0, -12.1, -3.25, 2.0, 1.6, 0.5, 'pendant'),
    ] + beads()),
    # the robe hanging open down its front and at its sides, torn at the hem
    part('right_robe', 'body', (-3.95, -11.6, -2.35), (-0.06, 0.1, 0.07), [(-0.2, 0, -0.5, 2.7, 10.9, 0.6, 'robe_front')]),
    part('right_robe_side', 'body', (-3.95, -11.6, -0.2), (0, 0, 0.09), [(-0.6, 0, -2.1, 0.6, 10.6, 4.2, 'robe_side')]),
    part('loin_front', 'body', (0, -2.2, -2.6), (-0.12, 0, 0), [(-1.8, 0, -0.3, 3.6, 6.0, 0.5, 'loinflap')]),
    part('loin_back', 'body', (0, -2.2, 2.4), (0.14, 0, 0), [(-2.2, 0, -0.2, 4.4, 5.0, 0.5, 'loinflap_back')]),
    # the talisman hanging from its beads, and the strap across its chest to its sack
    part('talisman', 'body', (0.1, -8.5, -3.15), (-0.2, 0, 0.12), [(-1.0, 0, -0.1, 2.0, 4.2, 0.2, 'talisman')]),
    part('strap', 'body', (-3.4, -11.6, -2.55), (0, 0, -0.62), [(-0.55, 0, -0.25, 1.1, 12.5, 0.4, 'strap')]),
    part('sack', 'body', (0, -10.4, 2.8), (0.25, 0, 0.12), [
        (-3.2, 0, 0, 6.4, 6.6, 3.9, 'sack'),
        (-1.3, -1.1, 0.8, 2.6, 1.3, 2.3, 'sacktie'),
        (-2.6, -3.6, 1.2, 1.2, 4.2, 1.2, 'scroll'),
        (1.3, -4.4, 1.6, 0.7, 5.0, 0.7, 'stick'),
        (-2.2, 2.2, 3.85, 2.2, 2.8, 0.15, 'sack_talisman')]),
    part('gourd', 'body', (3.5, -3.0, -1.3), (0.1, 0, -0.25), [
        (-0.25, -0.5, -0.25, 0.5, 0.8, 0.5, 'cork'),
        (-0.85, 0.2, -0.85, 1.7, 1.5, 1.7, 'gourd_top'),
        (-1.3, 1.5, -1.3, 2.6, 2.6, 2.6, 'gourd')]),

    # the head, thrust forward on its neck, tipped back to look ahead
    part('head', 'body', (0, -12.2, -1.6), HEAD, [
        (-3.9, -8.2, -4.2, 7.8, 6.4, 7.4, 'skull'),
        (-3.2, -9.4, -3.4, 6.4, 1.4, 6.0, 'crown'),
        (-4.6, -4.4, -3.9, 9.2, 2.2, 4.6, 'cheeks'),
        (-4.2, -6.0, -4.9, 8.4, 1.5, 1.4, 'brow'),
        (-3.6, -2.5, -5.3, 7.2, 1.5, 4.4, 'lip'),
        (-1.45, -10.9, -0.1, 2.9, 1.9, 2.9, 'topknot'),
        (-1.6, -9.4, -0.25, 3.2, 0.7, 3.2, 'cord'),
        (-2.9, -10.4, 1.15, 5.8, 0.5, 0.5, 'hairpin'),
        (-3.8, -8.0, 2.4, 7.6, 7.4, 1.8, 'hair'),
    ]),
    # its grin: the jaw hangs a little open, fangs jutting up from it
    part('jaw', 'head', (0, -1.3, -1.2), (0.16, 0, 0), [
        (-3.1, 0, -4.1, 6.2, 1.9, 5.0, 'jaw'),
        (-1.4, 1.5, -3.9, 2.8, 1.0, 2.0, 'chin'),
        (-2.6, -1.1, -4.0, 0.7, 1.2, 0.7, 'fang'),
        (1.9, -1.1, -4.0, 0.7, 1.2, 0.7, 'fang')]),
    # a long nose hooking down over the grin, ruddy at its tip
    part('nose', 'head', (0, -5.7, -4.7), (0.55, 0, 0), [(-0.85, -0.2, -3.0, 1.7, 1.9, 3.1, 'nose')]),
    part('nose_tip', 'nose', (0, 0.4, -2.7), (0.5, 0, 0), [(-0.8, -0.3, -1.1, 1.6, 2.2, 1.4, 'nosetip')]),
    part('right_eye', 'head', (-2.05, -4.75, -4.75), (0, 0.15, 0), [(-1.3, -0.7, -0.4, 2.6, 1.5, 0.5, 'eye')]),
    # long ears, swept out and up, their tips drooping
    part('right_ear', 'head', (-3.8, -5.2, -0.2), (0.05, 0.3, 0.22), [(-7.4, -2.2, -0.3, 7.4, 4.4, 0.6, 'ear')]),
    part('right_ear_tip', 'right_ear', (-7.2, -0.4, 0), (0, 0.1, -0.4), [(-4.0, -1.2, -0.25, 4.0, 2.4, 0.5, 'eartip')]),
    part('earring', 'right_ear', (-5.0, 1.8, 0), (0, 0, 0), [(-0.25, 0, -0.8, 0.5, 1.8, 1.6, 'ring')]),
    part('right_horn', 'head', (-2.2, -8.6, -2.2), (-0.75, 0, -0.3), [(-0.6, -2.3, -0.6, 1.2, 2.5, 1.2, 'horn')]),
    part('right_horn_tip', 'right_horn', (0, -2.2, 0), (-0.5, 0, 0), [(-0.4, -1.5, -0.4, 0.8, 1.6, 0.8, 'horntip')]),
    part('right_tuft', 'head', (-3.9, -7.6, 1.0), (0.1, 0, 0.42), [(-1.0, 0, -1.6, 1.0, 5.4, 3.2, 'hairtuft')]),
    part('head_talisman', 'head', (2.4, -8.4, -3.7), (-0.25, 0.25, 0.42), [(-0.95, 0, -0.1, 1.9, 3.8, 0.2, 'talisman_small')]),

    # the arms: long, thin, a ragged sleeve over the shoulder, the forearm bound, a big clawed hand
    part('right_arm', 'body', (-4.5, -10.8, -0.2), ARM, [
        (-1.2, -0.6, -1.1, 2.2, 6.0, 2.2, 'arm'),
        (-1.85, -1.3, -1.75, 3.4, 4.4, 3.5, 'sleeve')]),
    part('right_forearm', 'right_arm', (-0.1, 5.6, 0), FOREARM, [
        (-1.0, 0, -1.0, 2.0, 5.8, 2.0, 'forearm'),
        (-1.25, 3.2, -1.25, 2.5, 2.4, 2.5, 'armwrap')]),
    part('right_hand', 'right_forearm', (0, 5.7, 0), HAND, [
        (-1.3, 0, -1.4, 2.6, 2.0, 2.8, 'hand'),
        (-1.25, 1.7, -1.35, 0.7, 1.7, 0.7, 'finger'),
        (-0.35, 1.7, -1.35, 0.7, 1.9, 0.7, 'finger'),
        (0.55, 1.7, -1.35, 0.7, 1.6, 0.7, 'finger'),
        (-1.4, 0.6, -1.9, 0.8, 1.4, 0.8, 'thumb')]),
])

# the dagger in its right fist, pointing ahead; the sling hanging from its left
GOBLIN += [
    part('dagger', 'right_hand', (0, 1.3, -0.3), aim([BODY, ARM, FOREARM, HAND], (1.25, 0, 0)), [
        (-0.45, -1.6, -0.45, 0.9, 2.9, 0.9, 'grip'),
        (-0.5, 1.2, -0.5, 1.0, 0.8, 1.0, 'pommel'),
        (-1.35, -2.1, -0.45, 2.7, 0.55, 0.9, 'guard'),
        (-0.85, -7.0, -0.25, 1.7, 5.0, 0.5, 'blade')]),
    part('blade_tip', 'dagger', (0, -6.8, 0), (0, 0, 0.28), [(-0.75, -2.6, -0.25, 1.5, 2.7, 0.5, 'bladetip')]),
    part('sling', 'left_hand', (0, 1.6, -0.3), aim([BODY, LARM, LFOREARM, LHAND], (0.05, 0, 0)), [
        (-0.75, 0, -0.15, 0.3, 5.0, 0.3, 'slingcord'),
        (0.45, 0, -0.15, 0.3, 5.0, 0.3, 'slingcord'),
        (-0.95, 4.8, -0.6, 1.9, 1.0, 1.2, 'pouch'),
        (-0.45, 4.3, -0.45, 0.9, 0.9, 0.9, 'stone')]),
]
GOBLIN = w.settle(GOBLIN, ('right_foot', 'left_foot'))


# ── The skin ─────────────────────────────────────────────────────────────────

def skin(p, f, faces=None, warts=0.5, belly=None):
    w.hide(p, f, SKIN, SKIN_SHADE, SKIN_LIGHT, mottle=0.55, warts=warts, belly=belly, faces=faces)


def claws(p, f):
    """Its fingers' and toes' ends: black hooked claws."""
    for s in w.SIDES:
        fx, fy, fw, fh = f[s]
        for j in range(max(1, fh // 3)):
            for i in range(fw):
                p.px((fx + i, fy + fh - 1 - j), k(CLAW, 1.0 + 0.3 * (j == max(1, fh // 3) - 1)))


def wraps(p, f, base=WRAP, deep=WRAP_DEEP):
    """Rag bandages wound round: slanting turns, each lit at its upper edge, frayed."""
    for s in f:
        fx, fy, fw, fh = f[s]
        for i, j, xy in w.each(p, f[s]):
            t = ((j + i * 0.45) % 3) / 3
            c = mix(deep, base, 1 - t * 0.8)
            if (i * 7 + j * 3) % 11 == 0:
                c = k(c, 0.75)
            p.px(xy, [v * w.LIGHT[s] + p.rnd.randint(-5, 5) for v in c])


def robe(p, f, faces=None, hem=True):
    w.cloth(p, f, ROBE, ROBE_DEEP, fold=5, faces=faces)


def paint_goblin(p, style, f, dims):
    S = w.SIDES

    if style in ('thigh', 'knee', 'arm', 'forearm', 'neck'):
        skin(p, f, warts=0.6 if style in ('knee', 'arm') else 0.3)
        if style == 'knee':
            for s in ('front',):
                fx, fy, fw, fh = f[s]
                for i in range(1, fw - 1):
                    p.px((fx + i, fy + fh // 2), k(SKIN_SHADE, 0.9))

    if style == 'shin':
        skin(p, f, warts=0.2)

    if style in ('legwrap', 'armwrap'):
        wraps(p, f)
        w.ragged(p, f['front'], teeth=2, depth=1)

    if style == 'foot':
        skin(p, f, warts=0.4, belly=k(SKIN_SHADE, 0.8))
        fx, fy, fw, fh = f['top']                                  # tendons along its top
        for i in (1, fw // 2, fw - 2):
            for j in range(fh):
                if j % 2 == 0:
                    p.px((fx + i, fy + j), k(SKIN_LIGHT, 0.95))

    if style in ('toe', 'finger', 'thumb'):
        skin(p, f, warts=0)
        claws(p, f)
        for s in ('bottom',):
            w.flat(p, f, CLAW, faces=('bottom',))

    if style == 'heel':
        skin(p, f, warts=0)

    if style == 'hand':
        skin(p, f, warts=0.3)
        fx, fy, fw, fh = f['front']
        for i in range(0, fw, 2):                                  # knuckles
            p.px((fx + i, fy + 1), k(SKIN_LIGHT, 1.05))

    if style == 'belly':                                           # a little pot belly, pale, a navel
        skin(p, f, warts=0.2)
        fx, fy, fw, fh = f['front']                                # 12x10
        for i, j, xy in w.each(p, f['front']):
            d = math.hypot((i - fw / 2 + 0.5) / (fw / 2), (j - fh * 0.45) / (fh * 0.6))
            if d < 1:
                p.px(xy, [v * (1.05 - 0.25 * d) + p.rnd.randint(-3, 3) for v in mix(BELLY, SKIN, d * 0.6)])
        p.px((fx + fw // 2, fy + fh // 2 + 1), k(SKIN_SHADE, 0.6))
        p.px((fx + fw // 2 - 1, fy + fh // 2 + 1), k(SKIN_SHADE, 0.8))

    if style == 'chest':                                           # a narrow chest, every rib showing
        skin(p, f, warts=0.25)
        fx, fy, fw, fh = f['front']                                # 15x10
        for j in range(1, fh - 1, 2):
            for i in range(1, fw - 1):
                if abs(i - fw / 2 + 0.5) > 1:
                    p.px((fx + i, fy + j + (abs(i - fw / 2) > fw / 3)), k(SKIN_SHADE, 0.95))
                    p.px((fx + i, fy + j - 1), k(SKIN_LIGHT, 0.95))
        for j in range(fh):
            p.px((fx + fw // 2, fy + j), k(SKIN_SHADE, 0.8))
        for s in ('right', 'left', 'back'):
            fx, fy, fw, fh = f[s]
            for j in range(1, fh, 2):
                w.hline(p, f[s], j, k(SKIN_SHADE, 0.9 * w.LIGHT[s]))

    if style == 'yoke':                                            # the robe over its shoulders
        robe(p, f)
        w.border(p, f, k(ROBE_DEEP, 0.9), top=False)

    if style in ('robe_back', 'robe_front', 'robe_side'):
        robe(p, f)
        broad = 'front' if style != 'robe_side' else 'right'
        other = 'back' if style != 'robe_side' else 'left'
        rnd_patch = p.rnd
        for s in (broad, other, 'front', 'back', 'right', 'left'):
            fx, fy, fw, fh = f[s]
            if fw >= 5 and fh >= 8:
                for _ in range(2 if fw > 8 else 1):
                    pw, ph = rnd_patch.randint(4, min(8, fw - 1)), rnd_patch.randint(4, 7)
                    w.patch(p, f[s], rnd_patch.choice(PATCHES), ROBE_DEEP, STITCH,
                            rnd_patch.randrange(0, max(1, fw - pw)), rnd_patch.randrange(2, max(3, fh - ph - 4)), pw, ph)
            w.ragged(p, f[s], teeth=3, depth=4)
        if style == 'robe_back':
            w.tear_holes(p, f['back'], n=2)
        if style == 'robe_front':                                  # its edge, hemmed darker
            for s in ('front',):
                w.vline(p, f[s], f[s][2] - 1, k(ROBE_DEEP, 0.9))

    if style == 'loinwrap':
        w.cloth(p, f, (120, 84, 50), (64, 44, 28), fold=3)

    if style in ('loinflap', 'loinflap_back'):
        w.cloth(p, f, (108, 74, 46), (58, 38, 24), fold=3)
        for s in ('front', 'back'):
            w.ragged(p, f[s], teeth=2, depth=4)

    if style == 'rope':
        w.rope(p, f, ROPE)

    if style in ('bead', 'bead_red'):                             # wooden prayer beads, a red one every third
        c = BEAD if style == 'bead' else BEAD_RED
        w.flat(p, f, c, noise=4)
        for s in ('front', 'top'):
            fx, fy, fw, fh = f[s]
            p.px((fx, fy), k(c, 1.7))

    if style == 'pendant':                                        # a bronze coin, square-holed
        w.iron(p, f, BRONZE, rivets=0, dents=0.05)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            if abs(i - fw / 2 + 0.5) < 1 and abs(j - fh / 2 + 0.5) < 1:
                p.px(xy, (20, 14, 10))
            elif (i + j) % 3 == 0:
                p.px(xy, VERDIGRIS)

    if style in ('talisman', 'talisman_small', 'sack_talisman'):  # stolen talismans: yellow paper, red spells
        w.cut_all(p, f, ('right', 'left', 'top', 'bottom'))
        for s in ('front', 'back'):
            w.talisman(p, f[s])
            if style != 'sack_talisman':
                w.ragged(p, f[s], teeth=1, depth=1)

    if style == 'strap':
        w.leather(p, f, LEATHER, stitch=(180, 150, 100))

    if style in ('sack', 'sacktie'):                              # a sack of its thefts, bulging
        w.cloth(p, f, SACK if style == 'sack' else ROPE, k(SACK, 0.55), fold=4)
        if style == 'sack':
            fx, fy, fw, fh = f['back']
            w.patch(p, f['back'], PATCHES[1], ROBE_DEEP, STITCH, 2, 3, 5, 5)
            for s in w.SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):                               # the bulge's creases
                    if (i * 5) % 7 == 0:
                        for j in range(2, fh - 2):
                            if p.rnd.random() < 0.6:
                                p.px((fx + i, fy + j), k(SACK, 0.6 * w.LIGHT[s]))

    if style == 'scroll':                                         # a stolen scroll: paper, red ends
        w.flat(p, f, (222, 208, 170))
        w.border(p, f, (150, 30, 24))
        w.flat(p, f, (150, 30, 24), faces=('top', 'bottom'))

    if style == 'stick':
        w.wood(p, f, (120, 84, 50))

    if style in ('cork',):
        w.wood(p, f, (150, 112, 70))

    if style in ('gourd', 'gourd_top'):                           # a calabash, yellowed and lacquered
        w.hide(p, f, (196, 152, 64), (128, 86, 34), (236, 204, 120), mottle=0.4)
        if style == 'gourd':
            w.hline(p, f['front'], 1, (150, 30, 24))
            w.hline(p, f['right'], 1, (150, 30, 24))

    if style == 'skull':                                          # its big head: skin, the face painted on its front
        skin(p, f, warts=0.5)
        fx, fy, fw, fh = f['front']                               # 16x13: brow wrinkles over the brow ridge
        for j in (fh - 9, fh - 7):                                # two furrows over its brow, broken
            for i in range(4, fw - 4):
                if (i * 5 + j) % 7 > 2:
                    p.px((fx + i, fy + j), k(SKIN_SHADE, 1.1))
        fx, fy, fw, fh = f['top']
        for i, j, xy in w.each(p, f['top']):                      # hair over the crown, but its front
            if j < fh - 3 or p.rnd.random() < 0.4:
                p.px(xy, k(mix(HAIR, HAIR_GREY, 0.25 * p.rnd.random()), 1.1))

    if style == 'cheeks':                                         # cheeks, ruddy; deep creases down from the nose
        skin(p, f, warts=0.4)
        fx, fy, fw, fh = f['front']                               # 18x5
        for i in range(2, 6):
            for j in range(fh):
                p.px((fx + i, fy + j), mix(p.get((fx + i, fy + j)), RUDDY, 0.35))
                p.px((fx + fw - 1 - i, fy + j), mix(p.get((fx + fw - 1 - i, fy + j)), RUDDY, 0.35))
        for j in range(fh):
            p.px((fx + 6 + j // 2, fy + j), k(SKIN_SHADE, 0.7))
            p.px((fx + fw - 7 - j // 2, fy + j), k(SKIN_SHADE, 0.7))

    if style == 'brow':                                           # a heavy, scowling brow ridge
        skin(p, f, warts=0.2)
        fx, fy, fw, fh = f['front']
        w.hline(p, f['front'], fh - 1, k(SKIN_SHADE, 0.6))
        for i in range(fw):
            p.px((fx + i, fy), k(SKIN_LIGHT, 1.0))
        fx, fy, fw, fh = f['bottom']
        w.flat(p, f, k(SKIN_SHADE, 0.6), faces=('bottom',))

    if style == 'lip':                                            # the upper lip and the grin's top: teeth along its foot
        skin(p, f, warts=0.1)
        fx, fy, fw, fh = f['front']                               # 13x3
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), GUM)
        for i in range(1, fw - 1):
            if i % 2 == 1 or p.rnd.random() < 0.3:
                p.px((fx + i, fy + fh - 1), TOOTH if (i * 7) % 5 else k(TOOTH, 0.8))
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            w.hline(p, f[s], fh - 1, GUM)
        w.flat(p, f, GUM, faces=('bottom',))
        fx, fy, fw, fh = f['bottom']                              # its upper teeth, seen from below
        for i in range(fw):
            for j in range(fh - 2, fh):
                if i % 2 == 0:
                    p.px((fx + i, fy + j), TOOTH)

    if style in ('jaw', 'chin'):                                  # the jaw, a row of crooked fangs along its top
        skin(p, f, warts=0.4)
        if style == 'jaw':
            fx, fy, fw, fh = f['front']                           # 14x4
            for i in range(fw):
                p.px((fx + i, fy), GUM)
                p.px((fx + i, fy + 1), k(SKIN, 0.85))
            for i in range(0, fw, 2):
                tall = 2 if i in (2, fw - 4) else 1
                for j in range(tall):
                    p.px((fx + i + (i > fw // 2), fy + j), TOOTH)
            fx, fy, fw, fh = f['top']                             # its lower teeth, seen from above, and the mouth's dark
            for i, j, xy in w.each(p, f['top']):
                p.px(xy, GUM if j < fh - 2 else (TOOTH if i % 2 == 0 else k(GUM, 0.6)))
            for s in ('right', 'left'):
                w.hline(p, f[s], 0, GUM)

    if style == 'nose':                                           # the long bridge of its nose, warty, reddening toward its end
        skin(p, f, warts=0.5)
        for s_ in ('top', 'right', 'left', 'bottom'):
            fx, fy, fw, fh = f[s_]
            for i, j, xy in w.each(p, f[s_]):
                along = (1 - j / max(1, fh - 1)) if s_ == 'top' else (1 - i / max(1, fw - 1)) if s_ == 'right' else (i / max(1, fw - 1)) if s_ == 'left' else j / max(1, fh - 1)
                p.px(xy, mix(p.get(xy), RUDDY, 0.55 * along))
        w.flat(p, f, RUDDY, faces=('front',))
        fx, fy, fw, fh = f['top']                                 # a ridge of light down it
        for j in range(fh):
            p.px((fx + fw // 2, fy + j), k(SKIN_LIGHT, 1.05))

    if style == 'nosetip':                                        # the hooked tip, ruddy and shining, nostrils under it
        w.hide(p, f, RUDDY, k(RUDDY, 0.6), k(RUDDY, 1.35), mottle=0.3, warts=0.5)
        fx, fy, fw, fh = f['bottom']
        p.px((fx, fy + fh - 1), (40, 14, 12))
        p.px((fx + fw - 1, fy + fh - 1), (40, 14, 12))
        fx, fy, fw, fh = f['front']
        p.px((fx + 1, fy + 1), (230, 170, 150))

    if style == 'eye':                                            # bulging yellow eyes, a slit pupil, a heavy lid
        skin(p, f, warts=0)
        fx, fy, fw, fh = f['front']                               # 5x3
        for i, j, xy in w.each(p, f['front']):
            if j == 0:
                p.px(xy, k(SKIN_SHADE, 0.8))
            else:
                p.lit(xy, mix(EYE, (255, 250, 200), 0.3 if j == 1 else 0) if i not in (0, fw - 1) else k(EYE, 0.75))
        for j in (1, 2):
            p.px((fx + fw // 2, fy + j), (20, 12, 8))
            p.glow.putpixel((fx + fw // 2, fy + j), (0, 0, 0, 0))
        p.lit((fx + fw // 2 - 1, fy + 1), (255, 255, 230))

    if style in ('ear', 'eartip'):                                # long pointed ears: green, a ruddy hollow inside, tapering to a point
        skin(p, f, warts=0.15)
        fx, fy, fw, fh = f['front']                               # the hollow, facing forward
        for i, j, xy in w.each(p, f['front']):
            t = i / max(1, fw - 1)                                # 0 at the tip end .. 1 at the root
            if abs(j - fh / 2 + 0.5) < fh * (0.06 + 0.14 * t) and (style == 'ear' and 0.1 < t < 0.92 or style == 'eartip' and t > 0.35):
                p.px(xy, mix(p.get(xy), (196, 128, 104), 0.55))
        for i in range(fw):                                       # its rim lit along the top
            p.px((fx + i, fy + 1), k(SKIN_LIGHT, 1.0))
        for s_ in ('front', 'back'):                              # a leaf's shape: broad at the root, to a point at the tip
            fx, fy, fw, fh = f[s_]
            for i in range(fw):
                t = i / max(1, fw - 1)
                if s_ == 'back':
                    t = 1 - t
                keep = (0.55 + 0.45 * t) if style == 'ear' else (0.12 + 0.88 * t ** 0.8)
                for j in range(fh):
                    if abs(j - fh / 2 + 0.5) > keep * fh / 2:
                        p.clear((fx + i, fy + j))
        w.cut_all(p, f, ('top', 'bottom', 'right', 'left'))     # (its thin edges would outline the uncut shape)

    if style == 'ring':                                           # a bronze earring
        w.iron(p, f, BRONZE, rivets=0, rim=False, dents=0)

    if style in ('horn', 'horntip'):                              # horn-nubs curling back: dark at the root, bone above, dark at the point
        for s_ in f:
            fx, fy, fw, fh = f[s_]
            for i, j, xy in w.each(p, f[s_]):
                t = 1 - j / max(1, fh - 1)                        # 0 at its root .. 1 at its end
                c = mix((70, 56, 44), (214, 196, 148), t) if style == 'horn' else mix((214, 196, 148), (54, 44, 36), t)
                if j % 2 == 0:
                    c = k(c, 0.85)
                p.px(xy, k(c, w.LIGHT[s_]))
        if style == 'horntip':
            for s_ in w.SIDES:
                w.point(p, f[s_], down=False, keep=0.3, sharp=0.5)

    if style == 'crown':                                          # the top of its skull, under its hair
        skin(p, f, warts=0.3)
        for i, j, xy in w.each(p, f['top']):
            p.px(xy, k(mix(HAIR, HAIR_GREY, 0.35 * p.rnd.random()), 1.0))
        for s_ in ('back', 'right', 'left'):
            fx, fy, fw, fh = f[s_]
            for i in range(fw):
                p.px((fx + i, fy), k(HAIR, 1.2))

    if style == 'fang':
        for s_ in f:
            fx, fy, fw, fh = f[s_]
            for i, j, xy in w.each(p, f[s_]):
                p.px(xy, k(mix(TOOTH, (170, 150, 110), j / max(1, fh)), w.LIGHT[s_]))
        for s_ in w.SIDES:
            w.point(p, f[s_], down=False, keep=0.4, sharp=0.6)

    if style == 'hairpin':                                        # a bone pin through its topknot
        w.flat(p, f, (222, 210, 176))
        fx, fy, fw, fh = f['front']
        p.px((fx, fy), (150, 30, 24))
        p.px((fx + fw - 1, fy), (150, 30, 24))

    if style in ('topknot', 'hair', 'hairtuft'):                  # coarse black hair, greying, lit along its strands
        for s_ in f:
            w.strands(p, f[s_], k(HAIR, w.LIGHT[s_] * 1.15), k(HAIR, 0.55), k(HAIR_GREY, w.LIGHT[s_]), density=0.5, lean=0.3)
        if style == 'topknot':                                    # a bun: its strands wound round it
            for s_ in w.SIDES:
                fx, fy, fw, fh = f[s_]
                for i in range(fw):
                    p.px((fx + i, fy + (i // 2) % fh), k(HAIR_GREY, 0.95 * w.LIGHT[s_]))
            fx, fy, fw, fh = f['top']
            w.disc(p, fx + fw / 2, fy + fh / 2, 1.5, k(HAIR_GREY, 1.05))
            p.px((fx + fw // 2, fy + fh // 2), k(HAIR, 0.6))
        else:
            for s_ in ('back', 'front', 'right', 'left'):
                w.tufts(p, f[s_], depth=3, every=3)

    if style == 'cord':
        w.rope(p, f, CORD)

    if style == 'sleeve':                                         # a ragged sleeve over the shoulder
        robe(p, f)
        for s in w.SIDES:
            w.ragged(p, f[s], teeth=2, depth=3)
        w.patch(p, f['right'], PATCHES[0], ROBE_DEEP, STITCH, 1, 1, 4, 4)

    if style == 'grip':
        wraps(p, f, (90, 40, 30), (50, 22, 18))

    if style in ('pommel', 'guard'):
        w.iron(p, f, BRONZE, rust=0.35, rustc=VERDIGRIS, rivets=0, dents=0.2)

    if style in ('blade', 'bladetip'):                            # crude bronze, green with age, its edge notched and bright
        w.iron(p, f, (150, 112, 60), rust=0.3, rustc=VERDIGRIS, rivets=0, dents=0.3, rim=False)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                p.px((fx, fy + j), (226, 196, 130))
                if (j * 3) % 7 == 0:
                    p.clear((fx, fy + j))
            if style == 'bladetip':
                w.point(p, f[s], down=False, keep=0.3, sharp=0.5)

    if style == 'slingcord':
        w.rope(p, f, (150, 120, 80))

    if style == 'pouch':
        w.leather(p, f, LEATHER)

    if style == 'stone':
        w.hide(p, f, (120, 118, 112), (70, 68, 66), mottle=0.5)


if __name__ == '__main__':
    p = Painter(31)
    slots = paint(GOBLIN, p, paint_goblin)
    p.save('goblin')
    w.emit(GOBLIN, slots, w.JAVA, 'goblin')
    w.report('goblin', GOBLIN, SIZE, slots)
