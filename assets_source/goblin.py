"""The goblin (山魈), a demon of the mountains of the open world. As wild_paint.py says: its model is
written into WildRenderers' goblinLayer() between its GENERATED markers, its skin
(textures/entity/goblin.png) painted to the same box-UV layout, its eyes glowing (goblin_glow.png).

A feral mountain demon as tall as a man, hunched and long-limbed: a pack hunter of the high
forests. Soot-indigo skin, darker in its hollows and lighter over the swell of its muscles, its ribs
and spine showing; long arms hanging nearly to its shins, ending in hooked black claws; bony
digitigrade legs on clawed feet. A demon's face thrust forward under a heavy furrowed brow: deep
sockets with eyes burning amber in them, a flat snarling nose, a wide mouth hanging open full of
fangs, two tusks jutting up from its lower jaw, streaks of red ochre under its eyes. A great ridged
horn curling back from the right of its brow, the left one broken off short. A wild ash-white mane
from its crown down over its shoulders and back, small pointed ears in it. A necklace of bone beads
and fangs with a little skull hung from it, a jawbone tied on its left shoulder for a pauldron, bone
bangles; tattered dark wraps across its chest and about its forearms and shins, a dark loincloth
torn to rags on a rope belt, and torn Daoist talismans worn as trophies (two hanging from its belt,
one bound on its arm). In its right fist a crude jagged blade, a sawtoothed, hooked chopper of
black iron, its grip wound in rag.

Run from the repository root: python assets_source/goblin.py"""
import math

import beastgen as bg
import wild_paint as w
from wild_paint import part, mirrored, aim, paint, Painter, k, mix

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

SIZE = 0.86          # GoblinRenderer.SIZE

SKIN = (72, 78, 108)
SKIN_SHADE = (34, 36, 58)
SKIN_LIGHT = (120, 128, 166)
MANE = (214, 208, 196)
MANE_SHADE = (120, 114, 110)
MANE_ROOT = (70, 66, 70)
OCHRE = (168, 46, 30)
BONE = (222, 210, 176)
BONE_SHADE = (140, 126, 96)
WRAP = (52, 44, 44)
WRAP_DEEP = (24, 20, 22)
ROPE = (132, 106, 70)
IRON = (58, 56, 62)
IRON_EDGE = (176, 176, 182)
RAG = (110, 92, 70)
EYE = (255, 178, 40)
TOOTH = (232, 222, 192)
GUM = (82, 18, 26)
MAW = (40, 8, 12)
CLAW = (22, 20, 24)
HORN = (60, 50, 44)
HORN_TIP = (196, 182, 150)

BODY = (0.78, 0, 0)
HEAD = (-0.8, 0, 0)
ARM, FOREARM, HAND = (-0.92, 0, 0.16), (-0.42, 0, 0), (0.15, 0, 0)
LARM, LFOREARM, LHAND = (-0.7, 0, -0.2), (-0.3, 0, 0), (0.1, 0, 0)


def necklace():
    """Bone beads and fangs on a cord about its neck, hanging over its chest; a little skull at its foot."""
    out = []
    n = 11
    for i in range(n):
        a = -1.3 + 2.6 * i / (n - 1)
        x, y = 3.4 * math.sin(a), -13.0 + 3.0 * math.cos(a) ** 0.7
        z = -2.95 - 0.2 * math.cos(a)
        if i % 2:
            out.append((x - 0.35, y - 0.3, z, 0.7, 1.5, 0.5, 'fangbead'))
        else:
            out.append((x - 0.5, y - 0.5, z - 0.1, 1.0, 1.0, 0.8, 'bead'))
    out.append((-1.0, -10.6, -3.6, 2.0, 2.0, 1.4, 'skullbead'))
    return out


def spine():
    """The knobs of its spine down its hunched back."""
    return [(-0.45, y, 2.5 + (0.25 if y < -9 else 0) - (0.35 if y > -6 else 0), 0.9, 0.9, 0.7, 'knob')
            for y in (-12.5, -10.6, -8.7, -6.8, -5.0)]


GOBLIN = mirrored([
    # bony digitigrade legs: a long thigh, a shin bound in rags, a long ankle, a clawed foot
    part('right_leg', 'root', (-2.4, 13.0, 0.6), (-0.6, 0, 0.1), [
        (-1.6, -1.0, -1.7, 3.2, 7.6, 3.4, 'thigh'),
        (-1.2, 5.8, -2.1, 2.4, 1.8, 1.0, 'knee')]),
    part('right_shin', 'right_leg', (0, 6.9, 0.1), (1.25, 0, 0), [
        (-1.15, 0, -1.15, 2.3, 7.2, 2.3, 'shin'),
        (-1.45, 1.2, -1.45, 2.9, 3.6, 2.9, 'legwrap')]),
    part('right_ankle', 'right_shin', (0, 6.9, 0), (-1.05, 0, 0), [
        (-0.9, 0, -0.9, 1.8, 5.4, 1.8, 'ankle'),
        (-1.1, 0.4, -1.1, 2.2, 0.8, 2.2, 'anklet')]),
    part('right_foot', 'right_ankle', (0, 5.0, 0), (0.4, 0, 0), [
        (-1.5, -0.5, -2.8, 3.0, 1.5, 3.6, 'foot'),
        (-1.55, 0.0, -4.5, 0.85, 1.0, 1.9, 'toe'),
        (-0.42, 0.0, -4.8, 0.85, 1.0, 2.2, 'toe'),
        (0.7, 0.0, -4.5, 0.85, 1.0, 1.9, 'toe'),
        (-0.4, -0.3, 0.6, 0.8, 0.8, 1.4, 'spur')]),

    # the body from the hips up, hunched far forward; its pivot the hips
    part('body', 'root', (0, 13.0, 0.6), BODY, [
        (-3.2, -2.6, -2.2, 6.4, 3.4, 4.4, 'pelvis'),
        (-2.8, -7.4, -2.0, 5.6, 5.0, 4.0, 'waist'),
        (-4.3, -13.6, -2.7, 8.6, 6.6, 5.4, 'chest'),
        (-3.7, -15.0, -0.9, 7.4, 2.6, 4.2, 'traps'),
        (-1.5, -15.6, -2.2, 3.0, 2.4, 3.0, 'neck'),
        (-3.45, -3.5, -2.45, 6.9, 1.1, 4.9, 'rope'),
    ] + necklace() + spine()),
    # a tattered dark rag slung from its left shoulder across its chest and back to its right hip
    part('sash', 'body', (3.4, -13.4, -2.95), (0, 0, 0.6), [(-0.9, 0, -0.2, 1.8, 13.0, 0.4, 'sash')]),
    part('sash_back', 'body', (3.4, -13.4, 2.95), (0, 0, 0.6), [(-0.9, 0, -0.2, 1.8, 13.0, 0.4, 'sash')]),
    part('sash_end', 'body', (-3.2, -3.6, -2.7), (-0.1, 0, 0.15), [(-0.7, 0, -0.2, 1.4, 5.0, 0.4, 'sash_end')]),
    # the loincloth, torn to rags, before and behind
    part('loin_front', 'body', (0, -2.6, -2.5), (-0.1, 0, 0), [(-2.2, 0, -0.25, 4.4, 7.2, 0.5, 'loinflap')]),
    part('loin_back', 'body', (0, -2.6, 2.3), (0.12, 0, 0), [(-2.6, 0, -0.25, 5.2, 6.4, 0.5, 'loinflap')]),
    part('right_loin_side', 'body', (-3.2, -2.6, 0.1), (0, 0, 0.1), [(-0.25, 0, -1.8, 0.5, 5.4, 3.6, 'loinside')]),
    # torn talismans hanging from its belt, trophies
    part('right_talisman', 'body', (-2.0, -3.0, -2.6), (-0.15, 0.1, 0.18), [(-0.9, 0, -0.1, 1.8, 4.6, 0.2, 'talisman')]),
    part('talisman_back', 'body', (1.8, -3.0, 2.5), (0.2, 0, -0.1), [(-0.9, 0, -0.1, 1.8, 3.8, 0.2, 'talisman_torn')]),
    # its mane, falling from its nape over its shoulders and down its back
    part('mane', 'body', (0, -15.2, 1.6), (0.25, 0, 0), [
        (-4.2, 0, -0.6, 8.4, 8.8, 2.4, 'mane_back'),
        (-3.2, 7.2, 0.2, 6.4, 3.4, 1.4, 'mane_tail')]),
    part('right_mane_shoulder', 'body', (-3.4, -15.0, -0.2), (0, 0, 0.35), [(-1.8, 0, -1.8, 2.6, 5.2, 3.6, 'mane_side')]),

    # the head, thrust forward on its neck, turned up to look ahead
    part('head', 'body', (0, -15.2, -1.2), HEAD, [
        (-3.0, -6.6, -3.6, 6.0, 5.8, 5.8, 'cranium'),
        (-3.3, -3.0, -4.1, 6.6, 1.5, 3.0, 'cheeks'),
        (-2.4, -2.5, -5.3, 4.8, 2.0, 2.4, 'muzzle'),
        (-1.0, -3.7, -4.9, 2.0, 1.3, 1.0, 'nosebridge'),
        (-1.2, -2.9, -5.65, 2.4, 1.2, 0.9, 'nose'),
        (-3.4, -7.4, -3.2, 6.8, 1.6, 6.4, 'mane_top'),
        (-3.6, -6.6, 1.6, 7.2, 6.2, 2.2, 'mane_nape'),
        (-1.9, -1.0, -4.2, 3.8, 1.2, 3.0, 'maw'),
    ]),
    # the jaw hanging open, fangs along it, two tusks jutting up from its corners
    part('jaw', 'head', (0, -0.6, -1.4), (0.36, 0, 0), [
        (-2.3, 0, -4.4, 4.6, 1.4, 4.4, 'jaw'),
        (-1.6, 1.2, -4.0, 3.2, 1.0, 3.4, 'chin')]),
    part('right_tusk', 'jaw', (-1.85, 0.2, -3.95), (-0.25, 0, -0.18), [(-0.45, -2.6, -0.45, 0.9, 2.8, 0.9, 'tusk')]),
    part('right_fang', 'head', (-1.55, -0.6, -4.95), (0, 0, 0), [(-0.3, 0, -0.3, 0.6, 1.5, 0.6, 'fang')]),
    part('right_brow', 'head', (-1.55, -4.55, -3.75), (0.1, 0.12, 0.32), [(-2.1, -0.75, -0.8, 4.0, 1.5, 1.9, 'brow')]),
    part('right_eye', 'head', (-1.65, -3.5, -3.75), (0, 0.18, 0), [(-1.1, -0.5, -0.3, 2.2, 1.0, 0.4, 'eye')]),
    part('right_ear', 'head', (-2.9, -4.4, -0.8), (0.1, -0.55, -0.2), [(-0.3, -1.3, 0, 0.6, 2.4, 3.4, 'ear')]),
    part('right_lock', 'head', (-3.1, -5.6, -1.6), (0.1, 0, 0.12), [(-1.0, 0, -1.4, 1.4, 7.2, 3.0, 'lock')]),

    # the long arms: a gaunt upper arm, a forearm wound in rags, a big clawed hand
    part('right_arm', 'body', (-4.9, -12.6, 0.2), ARM, [
        (-1.4, -1.0, -1.4, 2.8, 8.6, 2.8, 'upperarm'),
        (-1.55, 3.8, -1.55, 3.1, 1.0, 3.1, 'bangle')]),
    part('right_forearm', 'right_arm', (0, 7.8, 0), FOREARM, [
        (-1.2, 0, -1.2, 2.4, 8.2, 2.4, 'forearm'),
        (-1.45, 3.4, -1.45, 2.9, 4.2, 2.9, 'armwrap')]),
    part('right_hand', 'right_forearm', (0, 8.0, 0), HAND, [
        (-1.5, 0, -1.6, 3.0, 2.4, 3.2, 'hand'),
        (-1.5, 2.1, -1.55, 0.7, 2.9, 0.7, 'claw'),
        (-0.62, 2.1, -1.55, 0.7, 3.2, 0.7, 'claw'),
        (0.26, 2.1, -1.55, 0.7, 3.0, 0.7, 'claw'),
        (1.0, 2.0, -1.35, 0.6, 2.4, 0.6, 'claw_small'),
        (-1.75, 0.8, -2.1, 0.75, 2.0, 0.75, 'claw_small')]),
])

# the great horn on the right of its brow, curling up and back; the left broken off short
GOBLIN += [
    part('horn', 'head', (-2.0, -6.3, -2.4), (-0.35, 0, -0.45), [(-0.95, -2.6, -0.95, 1.9, 2.9, 1.9, 'horn1')]),
    part('horn_mid', 'horn', (0, -2.4, 0), (-0.55, 0, 0.12), [(-0.7, -2.6, -0.7, 1.4, 2.8, 1.4, 'horn2')]),
    part('horn_tip', 'horn_mid', (0, -2.4, 0), (-0.75, 0, 0.1), [(-0.45, -2.4, -0.45, 0.9, 2.6, 0.9, 'horn3')]),
    part('horn_broken', 'head', (2.0, -6.3, -2.4), (-0.35, 0, 0.45), [(-0.95, -2.2, -0.95, 1.9, 2.5, 1.9, 'hornbroken')]),
    # a jawbone tied on its left shoulder, and a torn talisman bound on its left arm
    part('shoulder_bone', 'left_arm', (0.6, -1.2, 0), (0, 0, -0.25), [
        (-1.0, -0.6, -2.2, 3.0, 1.0, 4.4, 'jawbone'),
        (-0.6, 0.3, -2.0, 2.4, 1.4, 0.6, 'jawteeth'),
        (-0.6, 0.3, 1.4, 2.4, 1.4, 0.6, 'jawteeth')]),
    part('arm_talisman', 'left_arm', (1.42, 2.0, 0), (0, 0, 0.05), [(-0.1, 0, -0.9, 0.2, 4.2, 1.8, 'talisman_side')]),
    # the jagged blade in its right fist, held low and forward
    part('blade', 'right_hand', (0, 1.2, -0.2), aim([BODY, ARM, FOREARM, HAND], (1.15, 0, 0)), [
        (-0.5, -1.6, -0.5, 1.0, 3.6, 1.0, 'grip'),
        (-0.6, 1.8, -0.6, 1.2, 0.9, 1.2, 'pommel'),
        (-0.8, -2.3, -1.3, 1.6, 0.8, 2.6, 'guard'),
        (-0.25, -12.0, -1.2, 0.5, 9.8, 2.9, 'chopper')]),
    part('grip_rag', 'blade', (0, 1.6, 0.5), (0.4, 0, 0), [(-0.4, 0, -0.1, 0.8, 3.2, 0.2, 'ragtail')]),
]
GOBLIN = w.settle(GOBLIN, ('right_foot', 'left_foot'))


# ── The skin ─────────────────────────────────────────────────────────────────

def skin(p, f, faces=None, fall=0.28):
    w.hide(p, f, SKIN, SKIN_SHADE, SKIN_LIGHT, mottle=0.3, warts=0.0, faces=faces, fall=fall, seed=5)


def swell(p, rect, cx, cy, rx, ry, k_=1.16, rim=0.68):
    """A swell of muscle or bone under the skin: lit at its crown, a shadowed rim under it."""
    fx, fy, fw, fh = rect
    for i, j, xy in w.each(p, rect):
        dx, dy = (i - cx) / rx, (j - cy) / ry
        d = dx * dx + dy * dy
        if d < 1:
            p.px(xy, k(p.get(xy), 1 + (k_ - 1) * (1 - d) * (1 - max(0, dy) * 0.7)))
        elif d < 1.45 and dy > 0.15:
            p.px(xy, k(p.get(xy), rim))


def mane(p, f, faces=None, tuft=4, lean=0.15):
    """Coarse ash-white hair, dark at its roots, in locks; its ends cut into tufts."""
    for s in faces or f:
        lv = w.LIGHT[s]
        w.strands(p, f[s], k(MANE, lv * 0.92), k(MANE_SHADE, lv), k((240, 236, 226), lv), density=0.5, lean=lean, root_dark=0.35)
        fx, fy, fw, fh = f[s]
        for i in range(fw):                                       # dark partings between the locks
            if i % 4 == 3:
                for j in range(fh):
                    if p.rnd.random() < 0.8:
                        p.px((fx + i, fy + j), k(MANE_ROOT, lv * (1 + 0.4 * j / max(1, fh))))
    if tuft:
        for s in w.SIDES:
            if s in (faces or f):
                w.tufts(p, f[s], depth=tuft, every=3)


def bone(p, f, faces=None, base=BONE, cracks=True):
    for s in faces or f:
        fx, fy, fw, fh = f[s]
        for i, j, xy in w.each(p, f[s]):
            t = j / max(1, fh - 1)
            c = mix(base, BONE_SHADE, 0.35 * t + 0.25 * w.vnoise(fx + i, fy + j, 3, 9))
            p.px(xy, [v * w.LIGHT[s] + p.rnd.randint(-3, 3) for v in c])
        if cracks and fw * fh > 12 and s in w.SIDES:
            x = fx + p.rnd.randrange(fw)
            for j in range(min(fh, 3)):
                p.px((x + (j % 2), fy + j), k(BONE_SHADE, 0.7))


def claws(p, f, length=0.55):
    """A finger's or toe's end: a hooked black claw, glossy at its curve."""
    for s in w.SIDES + ('bottom',):
        fx, fy, fw, fh = f[s]
        for i, j, xy in w.each(p, f[s]):
            if j >= fh * (1 - length) or s == 'bottom':
                c = CLAW
                if s == 'front' and j < fh * (1 - length) + 2:
                    c = (92, 90, 100)
                p.px(xy, k(c, w.LIGHT[s]))
    for s in w.SIDES:
        w.point(p, f[s], down=True, keep=0.3, sharp=0.55)


def rags(p, f, base=WRAP, deep=WRAP_DEEP, faces=None):
    """Dark rag wound round: slanting turns lit along their upper edge, frayed."""
    for s in faces or f:
        fx, fy, fw, fh = f[s]
        for i, j, xy in w.each(p, f[s]):
            t = ((j + i * 0.5) % 4) / 4
            c = mix(base, deep, t * 0.8)
            if t < 0.22:
                c = k(base, 1.35)
            p.px(xy, [v * w.LIGHT[s] + p.rnd.randint(-4, 4) for v in c])


def torn_talisman(p, rect, burnt=True, tear=3):
    """A talisman torn from a shrine: yellowed paper, its red spell, its foot ripped away, singed."""
    w.talisman(p, rect, paper=(206, 180, 100), ink=(150, 24, 20))
    fx, fy, fw, fh = rect
    for i in range(fw):                                           # the ripped foot
        cut = tear if (i * 7) % 5 < 2 else tear // 2
        cut += p.rnd.randint(0, 1)
        for j in range(cut):
            p.clear((fx + i, fy + fh - 1 - j))
        if burnt and fh - cut - 1 > 0:
            p.px((fx + i, fy + fh - cut - 1), (60, 40, 24))


def paint_goblin(p, style, f, dims):
    S = w.SIDES

    if style in ('thigh', 'shin', 'ankle', 'upperarm', 'forearm', 'neck'):
        skin(p, f)
        if style in ('thigh', 'upperarm'):                        # the long muscle down it, the bone at its ends
            for s in ('front', 'right', 'left'):
                fx, fy, fw, fh = f[s]
                swell(p, f[s], fw / 2, fh * 0.35, fw * 0.42, fh * 0.32)
        if style in ('shin', 'ankle', 'forearm'):                 # a sharp bony ridge down its front
            fx, fy, fw, fh = f['front']
            w.vline(p, f['front'], fw // 2, k(SKIN_LIGHT, 1.0))
            w.vline(p, f['front'], fw // 2 + 1, k(SKIN_SHADE, 1.0))

    if style == 'knee':
        skin(p, f)
        bone(p, f, faces=('front',), base=mix(SKIN_LIGHT, BONE, 0.2), cracks=False)

    if style in ('legwrap', 'armwrap'):
        rags(p, f)
        for s in S:
            w.ragged(p, f[s], teeth=2, depth=2)

    if style in ('anklet', 'bangle'):                             # bone bangles: rings of little bones
        bone(p, f, cracks=False)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(0, fw, 3):
                for j in range(fh):
                    p.px((fx + i, fy + j), k(BONE_SHADE, 0.6 * w.LIGHT[s]))

    if style in ('foot', 'hand'):
        skin(p, f, fall=0.15)
        for s in ('front', 'top'):                                # knuckles
            fx, fy, fw, fh = f[s]
            for i in range(1, fw - 1, 3):
                p.px((fx + i, fy + (1 if s == 'front' else fh - 2)), k(SKIN_LIGHT, 1.1))
                p.px((fx + i, fy + (2 if s == 'front' else fh - 1)), k(SKIN_SHADE, 1.0))
        w.flat(p, f, k(SKIN_SHADE, 0.8), faces=('bottom',))

    if style in ('toe', 'claw', 'claw_small'):
        skin(p, f, fall=0)
        claws(p, f, length=0.6 if style != 'toe' else 0.5)

    if style == 'spur':
        skin(p, f)
        claws(p, f, length=0.7)

    if style == 'pelvis':
        skin(p, f)

    if style == 'waist':                                          # a gaunt belly, its ribs and its hollow
        skin(p, f, fall=0.15)
        for s in ('front', 'right', 'left'):
            fx, fy, fw, fh = f[s]
            for r in range(2):                                    # the lowest ribs, curving down to the sides
                for i in range(fw):
                    u = abs(i - (fw - 1) / 2) / (fw / 2) if s == 'front' else 0.6
                    if s == 'front' and u < 0.3:
                        continue
                    j = 1 + r * 3 + int(u * 2)
                    if j < fh - 1:
                        p.px((fx + i, fy + j + 1), k(mix(SKIN, SKIN_SHADE, 0.5), w.LIGHT[s]))
            if s == 'front':                                      # the hard muscle of its belly
                for side in (-1, 1):
                    swell(p, f[s], fw / 2 + side * fw * 0.17, fh * 0.6, fw * 0.17, fh * 0.35, k_=1.1, rim=0.85)
            if s == 'front':                                      # the hollow under them, a line down its middle
                w.vline(p, f[s], fw // 2, k(SKIN_SHADE, 0.85), fh // 2, fh)

    if style == 'chest':                                          # a lean, hard chest; the ribs under it; its back
        skin(p, f)
        fx, fy, fw, fh = f['front']                               # 17x13
        for side in (-1, 1):                                      # the pectorals
            swell(p, f['front'], fw / 2 + side * fw * 0.23, fh * 0.3, fw * 0.24, fh * 0.26, k_=1.22)
        w.vline(p, f['front'], fw // 2, k(SKIN_SHADE, 0.8), 1, fh)
        for n in range(3):                                        # three slashes of red ochre over its left breast
            for t in range(5):
                p.px((fx + fw - 3 - n * 2 - t // 2, fy + 2 + t), k(OCHRE, 1.0 - 0.06 * t))
        for r in range(2):                                        # ribs below the pectorals, at its sides
            for i in range(fw):
                u = abs(i - (fw - 1) / 2) / (fw / 2)
                if u > 0.5:
                    j = int(fh * 0.66) + r * 2 + int(u * 2)
                    if j < fh:
                        p.px((fx + i, fy + j), k(mix(SKIN, SKIN_SHADE, 0.6), 1.0))
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for r in range(3):
                for i in range(fw):
                    j = 5 + r * 3 + i // 4
                    if j < fh:
                        p.px((fx + i, fy + j), k(mix(SKIN, SKIN_SHADE, 0.6), w.LIGHT[s]))
        fx, fy, fw, fh = f['back']                                # shoulder blades
        for side in (-1, 1):
            swell(p, f['back'], fw / 2 + side * fw * 0.26, fh * 0.3, fw * 0.2, fh * 0.3, k_=1.2)

    if style == 'traps':                                          # the hump of muscle over its shoulders
        skin(p, f)
        for s in ('front', 'back', 'top'):
            fx, fy, fw, fh = f[s]
            swell(p, f[s], fw / 2, fh / 2, fw * 0.48, fh * 0.6, k_=1.15, rim=0.8)

    if style == 'knob':                                           # its spine's knobs
        skin(p, f, fall=0)
        w.flat(p, f, k(SKIN_LIGHT, 1.05), faces=('top', 'back'))

    if style == 'rope':                                           # a rope belt, knotted
        w.rope(p, f, ROPE)

    if style in ('sash', 'sash_end'):                             # a dark rag, frayed at its edges, holed
        for s in f:
            w.weave(p, f[s], WRAP, WRAP_DEEP, light=w.LIGHT[s], fold=3, fall=0.1)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):                                   # frayed edges
                if (j * 7) % 5 == 0:
                    p.clear((fx, fy + j))
                if (j * 3) % 4 == 0:
                    p.clear((fx + fw - 1, fy + j))
            if style == 'sash':
                w.tear_holes(p, f[s], n=2, size_=2)
            else:
                w.ragged(p, f[s], teeth=1, depth=4)

    if style in ('loinflap', 'loinside'):                         # the loincloth: dark, worn, torn to rags
        for s in f:
            w.weave(p, f[s], WRAP, WRAP_DEEP, light=w.LIGHT[s], fold=4, fall=0.4)
        for s in S:
            w.ragged(p, f[s], teeth=2, depth=6)
            w.tear_holes(p, f[s], n=2, size_=2)
        for s in ('front', 'back'):                               # a red-ochre band of its tribe near the top
            fx, fy, fw, fh = f[s]
            if fh > 6:
                w.hline(p, f[s], 2, k(OCHRE, 0.8 * w.LIGHT[s]))

    if style == 'talisman':
        w.cut_all(p, f, ('right', 'left', 'top', 'bottom'))
        for s in ('front', 'back'):
            torn_talisman(p, f[s], tear=3)
    if style == 'talisman_torn':                                  # half of one, torn on the slant
        w.cut_all(p, f, ('right', 'left', 'top', 'bottom'))
        for s in ('front', 'back'):
            torn_talisman(p, f[s], tear=2)
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if j > fh * 0.45 + i * 0.8:
                    p.clear(xy)
    if style == 'talisman_side':
        w.cut_all(p, f, ('front', 'back', 'top', 'bottom'))
        for s in ('right', 'left'):
            torn_talisman(p, f[s], tear=2)
            fx, fy, fw, fh = f[s]
            w.hline(p, f[s], 2, ROPE)                             # the cord binding it on

    if style in ('mane_back', 'mane_tail', 'mane_side', 'lock', 'mane_nape'):
        mane(p, f, tuft=5 if style in ('mane_back', 'mane_tail', 'lock') else 3)
    if style == 'mane_top':                                       # its crown: the hair parted and swept back
        mane(p, f, tuft=0, lean=0.0)
        fx, fy, fw, fh = f['top']
        for i, j, xy in w.each(p, f['top']):
            if i % 3 == 0:
                p.px(xy, k(MANE_SHADE, 0.9))
        for s in ('front',):                                      # its fringe over the brow, ragged
            w.tufts(p, f[s], depth=2, every=2)

    if style == 'bead':
        bone(p, f, cracks=False)
        for s in S:
            fx, fy, fw, fh = f[s]
            p.px((fx + fw // 2, fy + fh // 2), k(BONE_SHADE, 0.5))
    if style == 'fangbead':                                       # a beast's fang strung on its necklace
        bone(p, f, base=(236, 226, 200), cracks=False)
        for s in S:
            w.point(p, f[s], down=True, keep=0.3, sharp=0.5)
            w.hline(p, f[s], 0, (40, 30, 24))
    if style == 'skullbead':                                      # a little skull: sockets, a nose-hole, its teeth
        bone(p, f, cracks=False)
        fx, fy, fw, fh = f['front']                               # 4x4
        for i, j in ((0, 1), (fw - 1, 1), (0, 2), (fw - 1, 2)):
            p.px((fx + i, fy + j), (26, 18, 16))
        p.px((fx + fw // 2 - 1, fy + 2), (60, 44, 36))
        p.px((fx + fw // 2, fy + 2), (60, 44, 36))
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), (40, 30, 26) if i % 2 else BONE)

    if style == 'cranium':                                        # its skull, under the mane: furrowed, the brow knotted
        skin(p, f)
        fx, fy, fw, fh = f['front']                               # 12x11
        for j in (3, 5):                                          # forehead furrows, bending down to the middle
            for i in range(2, fw - 2):
                p.px((fx + i, fy + j + (1 if abs(i - fw / 2 + 0.5) < 2 else 0)), k(SKIN_SHADE, 1.0))
        for i, j, xy in w.each(p, f['front']):                    # the deep shadow of the sockets under the brow
            if j >= fh - 6:
                p.px(xy, k(SKIN_SHADE, 0.45 + 0.1 * (j - fh + 6) / 6))

    if style == 'brow':                                           # a heavy brow, scowling down to a V between its eyes
        skin(p, f, fall=0)
        fx, fy, fw, fh = f['front']                               # 8x3
        for i in range(fw):
            for j in range(fh):
                p.px((fx + i, fy + j), k(SKIN_LIGHT, 1.05 - 0.2 * j))
            p.px((fx + i, fy + fh - 1), k(SKIN_SHADE, 0.5))
        for j in range(fh):                                       # knotted where the two meet
            p.px((fx + fw - 1, fy + j), k(SKIN_SHADE, 0.8))
        w.flat(p, f, k(SKIN_SHADE, 0.35), faces=('bottom',))
        for s in ('top',):
            fx, fy, fw, fh = f[s]
            for i in range(fw):                                   # bristling white brows over it
                if i % 2 == 0 and abs(i - fw / 2) > 2:
                    p.px((fx + i, fy + fh - 1), MANE)

    if style == 'cheeks':                                         # gaunt cheekbones; red ochre streaked under its eyes
        skin(p, f)
        fx, fy, fw, fh = f['front']                               # 13x3
        for i, j, xy in w.each(p, f['front']):
            if j == 0:
                p.px(xy, k(SKIN_LIGHT, 1.0))
        for x in (1, 3, fw - 2, fw - 4):                          # the ochre streaks: three claw marks each side
            for j in range(fh):
                p.px((fx + x, fy + j), k(OCHRE, 1.0 - 0.15 * j))
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), k(SKIN_LIGHT, 0.9))
            for j in range(fh):
                p.px((fx + (2 if s == 'left' else fw - 3), fy + j), k(OCHRE, 0.75))

    if style == 'muzzle':                                         # the snarling upper lip, wrinkled back from its fangs
        skin(p, f, fall=0.1)
        for s in ('front', 'right', 'left'):
            fx, fy, fw, fh = f[s]
            for i in range(fw):                                   # snarl wrinkles
                if i % 3 == 1:
                    p.px((fx + i, fy + 1), k(SKIN_SHADE, 0.9))
            for i in range(fw):                                   # the lip's edge, its teeth below it
                p.px((fx + i, fy + fh - 2), k(GUM, 1.1))
                p.px((fx + i, fy + fh - 1), TOOTH if i % 2 == 0 else (60, 40, 40))
        w.flat(p, f, MAW, faces=('bottom',))
        fx, fy, fw, fh = f['bottom']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), TOOTH if i % 2 == 0 else GUM)
        for j in range(fh):
            p.px((fx, fy + j), TOOTH if j % 2 == 0 else GUM)
            p.px((fx + fw - 1, fy + j), TOOTH if j % 2 == 0 else GUM)

    if style == 'nosebridge':                                     # the bridge between its eyes, knotted into the scowl
        skin(p, f, fall=0)
        fx, fy, fw, fh = f['front']
        for j in range(0, fh, 2):
            w.hline(p, f['front'], j, k(SKIN_SHADE, 1.0))

    if style == 'nose':                                           # a flat demon's nose, its nostrils flared
        skin(p, f, fall=0)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy), k(SKIN_LIGHT, 1.0))
        p.px((fx, fy + fh - 1), (14, 8, 12))
        p.px((fx + fw - 1, fy + fh - 1), (14, 8, 12))
        p.px((fx + 1, fy + fh - 1), (30, 18, 24))
        p.px((fx + fw - 2, fy + fh - 1), (30, 18, 24))
        fx, fy, fw, fh = f['bottom']
        for j in range(fh):
            p.px((fx, fy + j), (14, 8, 12))
            p.px((fx + fw - 1, fy + j), (14, 8, 12))

    if style == 'maw':                                            # the dark of its open mouth, its tongue
        w.flat(p, f, MAW)
        fx, fy, fw, fh = f['bottom']
        for i, j, xy in w.each(p, f['bottom']):
            if 1 < i < fw - 2:
                p.px(xy, (110, 34, 44))
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            p.px(xy, (14, 4, 6))

    if style in ('jaw', 'chin'):                                  # the lower jaw: skin without, fangs along its top
        skin(p, f, fall=0.15)
        if style == 'jaw':
            fx, fy, fw, fh = f['top']                             # its gum and tongue, ringed with teeth
            for i, j, xy in w.each(p, f['top']):
                edge = i in (0, 1, fw - 2, fw - 1) or j <= 1
                p.px(xy, (TOOTH if (i + j) % 2 == 0 else (70, 40, 40)) if edge else (120, 36, 46))
            for s in ('front', 'right', 'left'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), k(GUM, 1.0))
        fx, fy, fw, fh = f['front']                               # ochre on its chin
        if style == 'chin':
            w.vline(p, f['front'], fw // 2, k(OCHRE, 0.9))

    if style == 'tusk':                                           # tusks jutting up from its jaw: ivory, darker at the root
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                p.px(xy, k(mix(TOOTH, (170, 150, 110), j / max(1, fh - 1)), w.LIGHT[s]))
        for s in S:
            w.point(p, f[s], down=False, keep=0.3, sharp=0.6)
    if style == 'fang':
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                p.px(xy, k(mix(TOOTH, (180, 160, 120), 0.3 * j / max(1, fh - 1)), w.LIGHT[s]))
        for s in S:
            w.point(p, f[s], down=True, keep=0.3, sharp=0.6)

    if style == 'eye':                                            # eyes burning amber deep in their sockets
        w.flat(p, f, (20, 12, 14))
        fx, fy, fw, fh = f['front']                               # 4x2: slanting down to its inner corner, a lid over it
        for i, j, xy in w.each(p, f['front']):
            u = i / max(1, fw - 1)                                # 0 at its outer corner .. 1 at its inner
            if (j == 0 and u > 0.6) or (j == fh - 1 and u < 0.2):
                p.px(xy, (20, 12, 14))
                continue
            p.lit(xy, mix((255, 240, 150), EYE, abs(u - 0.5) * 2))
        p.px((fx + fw // 2, fy + fh - 1), (40, 10, 4))
        p.glow.putpixel((fx + fw // 2, fy + fh - 1), (0, 0, 0, 0))

    if style == 'ear':                                            # small pointed ears swept back through the mane
        skin(p, f)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if 1 < j < fh - 1 and 0 < i < fw - 2:
                    p.px(xy, k((96, 52, 70), w.LIGHT[s]))
            w.point(p, (fx, fy, fw, fh), down=False, keep=0.6, sharp=0.5) if False else None
            for i, j, xy in w.each(p, f[s]):                      # tapering to its tip, behind
                u = i / max(1, fw - 1) if s == 'right' else 1 - i / max(1, fw - 1)
                if abs(j - fh / 2) > fh * 0.5 * (0.25 + 0.75 * u):
                    p.clear(xy)

    if style in ('horn1', 'horn2', 'horn3', 'hornbroken'):        # ridged horn: dark at its root, pale toward its tip
        t0 = {'horn1': 0.0, 'horn2': 0.33, 'horn3': 0.66, 'hornbroken': 0.0}[style]
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                t = t0 + 0.33 * (1 - j / max(1, fh - 1))
                c = mix(HORN, HORN_TIP, t ** 1.4)
                if j % 3 == 0:                                    # its rings
                    c = k(c, 0.72)
                p.px(xy, [v * w.LIGHT[s] + p.rnd.randint(-3, 3) for v in c])
        if style == 'horn3':
            for s in S:
                w.point(p, f[s], down=False, keep=0.25, sharp=0.5)
            w.cut_all(p, f, ('top',))
        if style == 'hornbroken':                                 # snapped off: a jagged, splintered top
            for s in S:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    cut = (i * 5) % 3
                    for j in range(cut):
                        p.clear((fx + i, fy + j))
                    p.px((fx + i, fy + cut), (190, 176, 140))
            fx, fy, fw, fh = f['top']
            for i, j, xy in w.each(p, f['top']):
                p.px(xy, (176, 160, 124) if (i + j) % 3 else (120, 100, 80))

    if style == 'jawbone':                                        # a great beast's jawbone, lashed on
        bone(p, f)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                if i % 4 == 1:
                    for j in range(fh):
                        p.px((fx + i, fy + j), k(ROPE, w.LIGHT[s]))
    if style == 'jawteeth':
        bone(p, f, base=TOOTH, cracks=False)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                if i % 2 == 1 and j > 0:
                    p.clear(xy)

    if style == 'grip':                                           # its grip wound in rag
        rags(p, f, base=RAG, deep=k(RAG, 0.5))
    if style in ('pommel', 'guard'):                              # a lump of bone for a pommel, a crossbar of bone
        bone(p, f, cracks=False)
    if style == 'ragtail':
        for s in f:
            w.weave(p, f[s], RAG, k(RAG, 0.5), light=w.LIGHT[s], fold=3)
        for s in S:
            w.ragged(p, f[s], teeth=1, depth=2)
    if style == 'chopper':                                        # black iron, hammered crude: a sawtoothed back, a hooked point, a bright notched edge
        w.iron(p, f, IRON, rust=0.25, rivets=0, rim=False, dents=0.3, rustc=(98, 46, 26))
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]                                 # depth 2.9 -> 6 wide; height 9.8 -> 20
            for i, j, xy in w.each(p, f[s]):
                u = i / max(1, fw - 1)                            # across the blade: 0 .. 1
                if s == 'right':
                    u = 1 - u                                     # 0 at its front edge .. 1 at its back
                v = j / max(1, fh - 1)                            # 0 at its tip .. 1 at its root
                width = 0.55 + 0.45 * min(1, v * 2.2)             # it widens from its point
                if u > width:
                    p.clear(xy)
                    continue
                if v < 0.12 and u < 0.45 - v * 2:                 # the hook curling forward at its point
                    p.clear(xy)
                    continue
                if u > width - 0.2 and (j % 3 == 0) and v > 0.15:
                    p.clear(xy)                                   # the saw teeth along its back
                    continue
                if u < 0.14:
                    p.px(xy, k(IRON_EDGE, w.LIGHT[s] * (0.9 + 0.1 * (j % 2))))   # the bright ground edge
            for n in (5, 11, 15):                                 # notches in its edge
                if n < fh:
                    p.px((fx + (0 if s == 'left' else fw - 1), fy + n), (0, 0, 0)) if False else None
                    p.clear((fx + (0 if s == 'left' else fw - 1), fy + n))


if __name__ == '__main__':
    p = Painter(37)
    slots = paint(GOBLIN, p, paint_goblin)
    p.save('goblin')
    w.emit(GOBLIN, slots, w.JAVA, 'goblin')
    w.report('goblin', GOBLIN, SIZE, slots)
