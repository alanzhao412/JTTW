"""The ogre (山夜叉), a mountain yaksha of the open world. As wild_paint.py says: its model is written
into WildRenderers' ogreLayer() between its GENERATED markers, its skin (textures/entity/ogre.png)
painted to the same box-UV layout, its eyes glowing (ogre_glow.png).

A brutish giant over three blocks tall, hunched under its own weight: brick-red skin, darker in its
hollows, every muscle heavy (the slabs of its chest, the hump of its shoulders, a great round paunch
over its belt, thick bowed legs); a huge head thrust forward on a bull neck, a wild black mane
streaked grey flowing down its back, two ridged horns of bone curving up and back from its brow, eyes
bulging and burning yellow under a crag of a brow, a broad flat nose with an iron ring through it,
an underbite jaw with two great tusks jutting up from it and a scruff of beard, pointed ears with
heavy gold hoops. A tiger-skin loincloth, the tiger's head hanging at the front of its belt, held up
by a thick twisted rope; a necklace of skulls and great bone beads; spiked iron bracers on its
wrists, a spiked iron plate over its left shoulder, iron fetters on its ankles with their broken
chains. Over its right shoulder a huge iron wolf-tooth club (狼牙棒), studded all over with spikes.

Run from the repository root: python assets_source/ogre.py"""
import math

import beastgen as bg
import wild_paint as w
from wild_paint import part, mirrored, aim, paint, Painter, k, mix

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

SIZE = 0.95          # OgreRenderer.SIZE

RED = (176, 54, 42)
RED_SHADE = (100, 26, 26)
RED_LIGHT = (226, 104, 76)
HAIR = (34, 28, 30)
HAIR_GREY = (128, 122, 118)
TIGER = (230, 138, 40)
TIGER_DEEP = (170, 82, 24)
STRIPE = (26, 20, 18)
TIGER_WHITE = (238, 230, 212)
IRON = (66, 64, 70)
IRON_DARK = (36, 34, 40)
RUST = (120, 60, 32)
GOLD = (226, 174, 62)
GOLD_DEEP = (140, 96, 34)
BONE = (228, 216, 182)
BONE_SHADE = (150, 136, 104)
EYE = (255, 222, 80)
TOOTH = (236, 226, 196)
GUM = (80, 20, 22)
ROPE = (178, 150, 96)
LEATHER = (90, 56, 34)
NAIL = (40, 30, 28)

BODY = (0.14, 0, 0)
HEAD = (0.05, 0, 0)
ARM, FOREARM, HAND = (-0.45, 0, 0.3), (-2.5, 0, 0), (0.2, 0, 0)
LARM, LFOREARM, LHAND = (0.05, 0, -0.18), (-0.3, 0, 0), (0, 0, 0)


def necklace():
    """Great bone beads and skulls on a cord about its neck, hanging over its chest."""
    out = []
    n = 13
    for i in range(n):
        a = -1.3 + 2.6 * i / (n - 1)
        x, y = 6.6 * math.sin(a), -20.4 + 5.2 * math.cos(a) * (1 - abs(a) / 2.2)
        z = -5.9 - 0.4 * math.cos(a)
        if i % 3 == 0:
            out.append((x - 1.1, y - 1.1, z - 0.5, 2.2, 2.4, 1.8, 'nskull'))
        else:
            out.append((x - 0.75, y - 0.75, z - 0.3, 1.5, 1.5, 1.5, 'nbead'))
    return out


def spikes(cx, cy, cz, r, n, length, rows, y0, dy, style, phase=0.0):
    """Spikes all round a cylinder of radius r about (cx, cz): `rows` rings of `n`, `dy` apart, from y0."""
    out = []
    for row in range(rows):
        for i in range(n):
            a = (i + 0.5 * (row % 2) + phase) * 2 * math.pi / n
            out.append((a, (cx + r * math.sin(a), y0 + row * dy, cz - r * math.cos(a))))
    return out


OGRE = mirrored([
    # thick bowed legs: a huge thigh, a calf with an iron fetter, a broad bare foot
    part('right_leg', 'root', (-4.4, 7.0, 0.6), (-0.12, 0, 0.1), [
        (-3.3, -1.6, -3.4, 6.6, 9.2, 6.8, 'thigh')]),
    part('right_shin', 'right_leg', (0, 8.2, 0.3), (0.26, 0, -0.08), [
        (-2.8, 0, -2.8, 5.6, 7.8, 5.6, 'calf'),
        (-3.2, 5.0, -3.2, 6.4, 1.8, 6.4, 'fetter')]),
    part('right_foot', 'right_shin', (0, 7.6, 0), (-0.14, 0, 0.0), [
        (-3.0, -0.3, -5.0, 6.0, 2.5, 7.4, 'foot'),
        (-2.95, 0.5, -6.3, 1.4, 1.7, 1.6, 'toe'),
        (-1.45, 0.5, -6.6, 1.4, 1.7, 1.8, 'toe'),
        (0.05, 0.5, -6.5, 1.4, 1.7, 1.7, 'toe'),
        (1.5, 0.6, -6.1, 1.4, 1.6, 1.4, 'toe')]),
    # the fetter's broken chain, dangling
    part('right_chain', 'right_shin', (2.6, 6.2, -1.6), (0.3, 0, 0.5), [
        (-0.3, 0, -0.7, 0.6, 1.8, 1.4, 'link_a'),
        (-0.7, 1.5, -0.3, 1.4, 1.8, 0.6, 'link_b')]),

    # the body from the hips up, hunched forward; its pivot the hips
    part('body', 'root', (0, 7.0, 0.6), BODY, [
        (-6.3, -3.6, -4.2, 12.6, 5.0, 8.6, 'loin'),
        (-7.4, -11.6, -5.6, 14.8, 8.8, 10.8, 'belly'),
        (-5.8, -10.8, -7.6, 11.6, 7.2, 2.2, 'paunch'),
        (-3.8, -9.6, -8.6, 7.6, 4.6, 1.0, 'paunch_front'),
        (-8.6, -20.6, -4.6, 17.2, 9.8, 9.4, 'chest'),
        (-7.8, -20.2, -5.7, 7.4, 5.4, 1.2, 'pec'),
        (0.4, -20.2, -5.7, 7.4, 5.4, 1.2, 'pec'),
        (-6.8, -23.4, -2.6, 13.6, 3.4, 7.2, 'traps'),
        (-5.6, -21.6, 3.6, 11.2, 7.0, 2.2, 'hump'),
        (-7.0, -4.8, -4.7, 14.0, 1.9, 9.8, 'rope'),
        (-1.4, -5.4, -5.4, 2.8, 2.8, 1.2, 'ropeknot'),
        (-4.6, -22.4, 4.4, 9.2, 11.0, 1.6, 'backmane'),
    ] + necklace()),
    # the tiger skin: its flaps before, behind and at the sides, the tiger's head at its belt
    part('loin_front', 'body', (0, -3.6, -4.6), (-0.16, 0, 0), [(-3.6, 0, -0.45, 7.2, 9.4, 0.9, 'tigerflap')]),
    part('tiger_head', 'body', (0, -4.6, -5.0), (-0.1, 0, 0), [
        (-2.3, -1.4, -2.6, 4.6, 3.6, 2.8, 'tigerhead'),
        (-1.3, 0.8, -3.4, 2.6, 1.6, 1.0, 'tigermuzzle'),
        (-2.3, -2.4, -1.4, 1.2, 1.2, 0.6, 'tigerear'),
        (1.1, -2.4, -1.4, 1.2, 1.2, 0.6, 'tigerear')]),
    part('loin_back', 'body', (0, -3.6, 4.2), (0.2, 0, 0), [(-4.8, 0, -0.45, 9.6, 8.6, 0.9, 'tigerflap_back')]),
    part('right_loin_side', 'body', (-6.2, -3.4, 0), (0, 0, 0.16), [(-0.45, 0, -3.4, 0.9, 7.4, 6.8, 'tigerside')]),

    # the head, huge, thrust forward on its bull neck
    part('head', 'body', (0, -21.4, -2.8), HEAD, [
        (-4.6, -8.6, -4.6, 9.2, 8.4, 8.6, 'skull'),
        (-5.0, -6.3, -5.5, 10.0, 1.9, 2.0, 'brow'),
        (-5.3, -3.9, -4.9, 10.6, 2.4, 5.0, 'cheeks'),
        (-1.9, -5.0, -6.0, 3.8, 2.6, 1.6, 'nose'),
        (-3.6, -2.0, -5.3, 7.2, 1.3, 2.0, 'lip'),
        (-5.4, -9.4, 1.6, 10.8, 11.6, 3.6, 'mane'),
    ]),
    # its hair bristling up off its crown in spiky tufts, the middle one tallest
    part('hair_top', 'head', (0, -8.4, -1.0), (-0.35, 0, 0), [(-1.5, -4.6, -1.6, 3.0, 5.0, 3.4, 'hairspike')]),
    part('right_hair_top', 'head', (-2.8, -8.2, -0.6), (-0.3, 0, -0.45), [(-1.4, -4.0, -1.5, 2.8, 4.4, 3.2, 'hairspike_side')]),
    part('right_hair_back', 'head', (-2.2, -8.0, 1.8), (0.35, 0, -0.3), [(-1.4, -4.2, -1.4, 2.8, 4.6, 3.0, 'hairspike_side')]),
    part('hair_back', 'head', (0, -8.2, 2.2), (0.55, 0, 0), [(-1.6, -4.8, -1.4, 3.2, 5.2, 3.0, 'hairspike')]),
    part('nose_ring', 'head', (0, -2.6, -6.2), (0.3, 0, 0), [(-1.1, 0, -0.25, 2.2, 1.8, 0.5, 'nosering')]),
    part('jaw', 'head', (0, -1.4, -1.2), (0.1, 0, 0), [
        (-4.5, 0, -4.7, 9.0, 3.2, 7.4, 'jaw'),
        (-3.3, 2.8, -4.4, 6.6, 3.0, 3.0, 'beard')]),
    part('right_tusk', 'jaw', (-3.0, 0.4, -4.1), (-0.18, 0, -0.22), [(-0.75, -3.6, -0.75, 1.5, 3.9, 1.5, 'tusk')]),
    part('right_tusk_tip', 'right_tusk', (0, -3.5, 0), (-0.35, 0, 0.1), [(-0.55, -2.0, -0.55, 1.1, 2.2, 1.1, 'tusktip')]),
    part('right_eye', 'head', (-2.4, -4.75, -5.0), (0, 0.12, 0), [(-1.45, -0.8, -0.5, 2.9, 1.7, 0.8, 'eye')]),
    part('right_ear', 'head', (-4.6, -4.6, -0.6), (0.05, 0.45, -0.3), [(-2.8, -1.8, -0.4, 2.8, 3.8, 0.8, 'ear')]),
    part('right_hoop', 'right_ear', (-1.6, 1.8, 0), (0, 0, 0.1), [
        (-0.3, 0, -1.2, 0.6, 0.5, 2.4, 'hoop'),
        (-0.3, 2.6, -1.2, 0.6, 0.5, 2.4, 'hoop'),
        (-0.3, 0.3, -1.4, 0.6, 2.5, 0.5, 'hoop_side'),
        (-0.3, 0.3, 0.9, 0.6, 2.5, 0.5, 'hoop_side')]),
    part('right_mane', 'head', (-5.0, -8.2, 0.4), (0.15, 0, 0.32), [(-1.6, 0, -3.4, 1.8, 9.4, 6.4, 'sidemane')]),
    # two ridged horns of bone, up and back from its brow
    part('right_horn', 'head', (-2.8, -8.4, -2.4), (-0.3, 0, -0.38), [(-1.2, -3.4, -1.2, 2.4, 3.6, 2.4, 'horn1')]),
    part('right_horn_mid', 'right_horn', (0, -3.2, 0), (-0.42, 0, 0.18), [(-0.9, -3.2, -0.9, 1.8, 3.4, 1.8, 'horn2')]),
    part('right_horn_tip', 'right_horn_mid', (0, -3.0, 0), (-0.45, 0, 0.12), [(-0.55, -2.8, -0.55, 1.1, 3.0, 1.1, 'horn3')]),

    # the arms: a huge shoulder, a thick arm, a spiked iron bracer, a great fist
    part('right_arm', 'body', (-10.0, -18.6, 0.2), ARM, [
        (-4.0, -2.8, -3.9, 7.4, 6.4, 7.8, 'shoulder'),
        (-3.2, -0.4, -3.2, 6.2, 9.4, 6.4, 'upperarm'),
        (-2.6, 1.4, -4.0, 5.0, 5.2, 1.0, 'bicep')]),
    part('right_forearm', 'right_arm', (-0.1, 8.6, 0), FOREARM, [
        (-2.9, 0, -2.9, 5.8, 8.8, 5.8, 'forearm'),
        (-3.4, 2.4, -3.4, 6.8, 5.4, 6.8, 'bracer'),
        (-0.5, 4.2, -4.6, 1.0, 1.0, 1.4, 'bspike'),
        (-0.5, 4.2, 3.2, 1.0, 1.0, 1.4, 'bspike'),
        (-4.6, 4.2, -0.5, 1.4, 1.0, 1.0, 'bspike_x'),
        (3.2, 4.2, -0.5, 1.4, 1.0, 1.0, 'bspike_x')]),
    part('right_hand', 'right_forearm', (0, 8.6, 0), HAND, [
        (-2.8, 0, -3.0, 5.6, 4.4, 6.0, 'fist'),
        (-3.2, 0.6, -3.4, 1.4, 2.6, 2.0, 'thumb')]),
])

# the spiked plate over its left shoulder alone
OGRE += [
    part('shoulder_plate', 'left_arm', (0.6, -3.0, 0), (0, 0, -0.25), [
        (-3.4, -1.0, -4.4, 7.6, 2.4, 8.8, 'plate'),
        (-2.8, 1.2, -4.6, 6.8, 2.0, 9.2, 'plate2'),
        (0.6, -2.8, -2.6, 1.2, 2.2, 1.2, 'pspike'),
        (0.6, -2.8, 1.2, 1.2, 2.2, 1.2, 'pspike'),
        (2.8, -2.0, -0.6, 1.2, 2.0, 1.2, 'pspike')]),
]


def club():
    """The wolf-tooth club: a leather-bound grip, an iron collar, a long iron head studded with spikes."""
    boxes = [
        (-0.95, -6.0, -0.95, 1.9, 13.0, 1.9, 'grip'),
        (-1.4, 7.0, -1.4, 2.8, 1.6, 2.8, 'pommel'),
        (-1.9, -7.6, -1.9, 3.8, 1.8, 3.8, 'collar'),
        (-2.7, -26.0, -2.7, 5.4, 18.6, 5.4, 'clubhead'),
        (-2.2, -27.6, -2.2, 4.4, 1.8, 4.4, 'clubcap'),
        (-0.6, -29.6, -0.6, 1.2, 2.2, 1.2, 'capspike'),
    ]
    for row in range(5):                                          # the spikes: five rings of them, staggered
        y = -24.0 + row * 3.6
        off = 1.0 if row % 2 else 0
        for i, x in enumerate((-1.8 + off, 0.2 + off)):
            if x > 1.6:
                x -= 4.0
            boxes += [(x, y, -4.4, 1.0, 1.0, 1.8, 'cspike'),       # before
                      (x, y, 2.6, 1.0, 1.0, 1.8, 'cspike'),        # behind
                      (-4.4, y, x, 1.8, 1.0, 1.0, 'cspike_x'),     # right
                      (2.6, y, x, 1.8, 1.0, 1.0, 'cspike_x')]      # left
    return boxes


CLUB_AIM = aim([BODY, ARM, FOREARM, HAND], (-0.85, 0, -0.62))
OGRE += [part('club', 'right_hand', (0, 2.0, -0.2), CLUB_AIM, club())]
OGRE = w.settle(OGRE, ('right_foot', 'left_foot'))


# ── The skin ─────────────────────────────────────────────────────────────────

def skin(p, f, faces=None, warts=0.15, fall=0.25):
    w.hide(p, f, RED, RED_SHADE, RED_LIGHT, mottle=0.45, warts=warts, faces=faces, fall=fall)


def muscle(p, rect, cx, cy, rx, ry, k_=1.12, rim=0.7):
    """A swell of muscle on a face: lit at its crown, a shadowed rim at its lower edge."""
    fx, fy, fw, fh = rect
    for i, j, xy in w.each(p, rect):
        dx, dy = (i - cx) / rx, (j - cy) / ry
        d = dx * dx + dy * dy
        if d < 1:
            c = p.get(xy)
            p.px(xy, k(c, 1 + (k_ - 1) * (1 - d) * (1 - max(0, dy) * 0.8)))
        elif d < 1.35 and dy > 0.2:
            p.px(xy, k(p.get(xy), rim))


def tiger(p, f, faces=None, belly=False, seed=0):
    """Tiger skin: orange, deeper toward its edges, white beneath, black stripes across it."""
    for s in faces or f:
        fx, fy, fw, fh = f[s]
        for i, j, xy in w.each(p, f[s]):
            c = mix(TIGER, TIGER_DEEP, abs(i / max(1, fw - 1) - 0.5) * 1.2)
            if belly and j > fh * 0.7:
                c = TIGER_WHITE
            p.px(xy, [v * w.LIGHT[s] + p.rnd.randint(-5, 5) for v in c])
        y = p.rnd.randint(1, 3)
        while y < fh - 1:                                         # stripes: tapering wavy bars across
            reach = fw * (0.3 + 0.35 * p.rnd.random())
            start = 0 if p.rnd.random() < 0.5 else fw - 1
            for t in range(int(reach)):
                x = start + t if start == 0 else start - t
                thick = 2 if t < reach * 0.6 else 1
                wob = round(0.8 * math.sin(t * 0.6 + y))
                for q in range(thick):
                    yy = y + q + wob
                    if 0 <= yy < fh:
                        p.px((fx + x, fy + yy), k(STRIPE, w.LIGHT[s]))
            y += p.rnd.randint(3, 5)


def mane(p, f, faces=None, ragged=True):
    for s in faces or f:
        w.strands(p, f[s], k(HAIR, w.LIGHT[s] * 1.4), k(HAIR, 0.5), k(HAIR_GREY, w.LIGHT[s]), density=0.45, lean=0.25)
        fx, fy, fw, fh = f[s]
        for i in range(fw):                                       # streaks of grey through it
            if p.rnd.random() < 0.12:
                for j in range(fh):
                    if p.rnd.random() < 0.7:
                        p.px((fx + i, fy + j), k(HAIR_GREY, w.LIGHT[s] * (0.8 + 0.3 * j / fh)))
    if ragged:
        for s in w.SIDES:
            w.tufts(p, f[s], depth=5, every=3)


def bone(p, f, faces=None, base=BONE, banded=True):
    for s in faces or f:
        fx, fy, fw, fh = f[s]
        for i, j, xy in w.each(p, f[s]):
            c = base
            if banded and j % 3 == 0:
                c = k(base, 0.82)
            p.px(xy, [v * w.LIGHT[s] + p.rnd.randint(-4, 4) for v in c])


def nails(p, f):
    for s in w.SIDES + ('top',):
        fx, fy, fw, fh = f[s]
        if s == 'front':
            for i, j, xy in w.each(p, f[s]):
                if j < fh * 0.5:
                    p.px(xy, k(NAIL, 1.3))


def paint_ogre(p, style, f, dims):
    S = w.SIDES

    if style == 'thigh':
        skin(p, f)
        for s in ('front', 'right', 'left'):
            fx, fy, fw, fh = f[s]
            muscle(p, f[s], fw * 0.5, fh * 0.45, fw * 0.45, fh * 0.45)

    if style == 'calf':
        skin(p, f)
        for s in ('back', 'right', 'left'):
            fx, fy, fw, fh = f[s]
            muscle(p, f[s], fw * 0.5, fh * 0.3, fw * 0.45, fh * 0.35)

    if style in ('foot', 'toe'):
        skin(p, f, warts=0.3)
        if style == 'toe':
            nails(p, f)
        else:
            fx, fy, fw, fh = f['top']
            for i in range(1, fw, 3):
                for j in range(fh):
                    if j % 2:
                        p.px((fx + i, fy + j), k(RED_LIGHT, 0.95))

    if style == 'fetter':                                         # an iron fetter about its ankle, rusted
        w.iron(p, f, IRON, rust=0.35, rivets=4, rustc=RUST)

    if style in ('link_a', 'link_b'):
        w.iron(p, f, IRON, rust=0.3, rivets=0, rim=False, rustc=RUST)
        for s in S:
            fx, fy, fw, fh = f[s]
            if fw > 2 and fh > 2:
                for j in range(1, fh - 1):
                    for i in range(1, fw - 1):
                        p.px((fx + i, fy + j), IRON_DARK)

    if style == 'loin':                                           # the tiger skin wrapped round its hips
        tiger(p, f)

    if style in ('tigerflap', 'tigerflap_back', 'tigerside'):
        tiger(p, f)
        for s in S:
            fx, fy, fw, fh = f[s]
            if fw > 3:
                w.tufts(p, f[s], depth=3, every=4)
        if style == 'tigerflap_back':                             # the tail of the skin, down its middle
            fx, fy, fw, fh = f['back']
            for j in range(fh):
                for i in range(fw // 2 - 1, fw // 2 + 1):
                    p.px((fx + i, fy + j), STRIPE if (j // 2) % 2 else TIGER)

    if style == 'tigerhead':                                      # the tiger's head at its belt: a face, glass eyes, its 王
        tiger(p, f)
        fx, fy, fw, fh = f['front']                               # 9x7
        for i in range(fw):
            for j in range(fh - 2, fh):
                p.px((fx + i, fy + j), TIGER_WHITE)
        w.glyph(p, fx + fw // 2 - 2, fy, ['####', '.##.', '####', '.##.', '####'][:3], STRIPE)
        for ex in (1, fw - 3):
            p.px((fx + ex, fy + 3), (200, 190, 60))
            p.px((fx + ex + 1, fy + 3), (30, 20, 10))
    if style == 'tigermuzzle':
        w.flat(p, f, TIGER_WHITE)
        fx, fy, fw, fh = f['front']
        p.px((fx + fw // 2, fy), (60, 30, 30))
        p.px((fx + fw // 2 - 1, fy), (60, 30, 30))
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), TOOTH if i % 2 else (60, 20, 20))
    if style == 'tigerear':
        tiger(p, f)

    if style == 'belly':                                          # its belly under the paunch, and its flanks
        skin(p, f)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            muscle(p, f[s], fw * 0.5, fh * 0.5, fw * 0.5, fh * 0.6, k_=1.08)

    if style in ('hump',):
        skin(p, f)
        fx, fy, fw, fh = f['back']
        muscle(p, f['back'], fw * 0.5, fh * 0.35, fw * 0.5, fh * 0.55, k_=1.12)
    if style == 'paunch_front':
        skin(p, f, warts=0.02, fall=0.05)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            d = math.hypot((i - fw / 2 + 0.5) / (fw / 2), (j - fh * 0.5) / (fh * 0.7))
            p.px(xy, k(p.get(xy), 1.2 - 0.25 * d))
        p.px((fx + fw // 2, fy + fh // 2 + 1), RED_SHADE)
        p.px((fx + fw // 2 - 1, fy + fh // 2 + 1), k(RED_SHADE, 0.7))
    if style == 'paunch':                                         # the great round paunch: lit at its swell, a navel
        skin(p, f, warts=0.05, fall=0.1)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            d = math.hypot((i - fw / 2 + 0.5) / (fw / 2), (j - fh * 0.42) / (fh * 0.6))
            p.px(xy, k(p.get(xy), 1.16 - 0.3 * d))

    if style == 'chest':                                          # the slabs of its chest, its ribs under them, a hairy breast
        skin(p, f)
        fx, fy, fw, fh = f['front']                               # 34x20
        for side in (-1, 1):
            muscle(p, f['front'], fw / 2 + side * fw * 0.24, fh * 0.32, fw * 0.24, fh * 0.32, k_=1.18, rim=0.62)
        for j in range(int(fh * 0.7), fh, 2):                     # its ribs, below
            for i in range(2, int(fw * 0.3)):
                p.px((fx + i, fy + j), k(RED_SHADE, 1.1))
                p.px((fx + fw - 1 - i, fy + j), k(RED_SHADE, 1.1))
        for j in range(fh):                                       # its breastbone
            p.px((fx + fw // 2, fy + j), k(RED_SHADE, 1.0))
        for _ in range(40):                                       # dark hair on its breast
            x, y = fx + fw // 2 + p.rnd.randint(-4, 4), fy + p.rnd.randint(2, int(fh * 0.7))
            p.px((x, y), k(HAIR, 1.4))
        fx, fy, fw, fh = f['back']                                # its back: shoulder blades, the spine's furrow
        for side in (-1, 1):
            muscle(p, f['back'], fw / 2 + side * fw * 0.25, fh * 0.35, fw * 0.2, fh * 0.35, k_=1.12)
        for j in range(fh):
            p.px((fx + fw // 2, fy + j), k(RED_SHADE, 0.9))
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            muscle(p, f[s], fw * 0.5, fh * 0.3, fw * 0.45, fh * 0.35)

    if style == 'traps':                                          # the hump of its shoulders
        skin(p, f)
        fx, fy, fw, fh = f['top']
        for i, j, xy in w.each(p, f['top']):
            p.px(xy, k(p.get(xy), 1.0 + 0.12 * (1 - abs(i / max(1, fw - 1) - 0.5) * 2)))

    if style == 'rope':                                           # a thick twisted rope for its belt
        w.rope(p, f, ROPE, pitch=4)
    if style == 'ropeknot':
        w.rope(p, f, k(ROPE, 0.95), pitch=3)

    if style == 'backmane':
        mane(p, f)

    if style in ('nbead',):
        bone(p, f, banded=False)
        for s in ('front', 'top'):
            fx, fy, fw, fh = f[s]
            p.px((fx, fy), (250, 244, 226))
    if style == 'nskull':                                         # skulls strung among them: sockets dark, a grin
        bone(p, f, banded=False)
        fx, fy, fw, fh = f['front']                               # 4x5
        for i, j in ((0, 1), (fw - 1, 1), (0, 2), (fw - 1, 2)):
            p.px((fx + i, fy + j), (30, 20, 18))
        p.px((fx + fw // 2, fy + 2), BONE_SHADE)
        p.px((fx + fw // 2 - 1, fy + 2), BONE_SHADE)
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), (40, 30, 26) if i % 2 else BONE)

    if style == 'skull':                                          # its great head; brow furrows, its face below the brow
        skin(p, f, warts=0.3)
        fx, fy, fw, fh = f['front']                               # 18x17
        for j in (3, 6):                                          # two furrows, broken
            for i in range(4, fw - 4):
                if (i * 3 + j) % 7 > 1:
                    p.px((fx + i, fy + j + (abs(i - fw / 2) > fw / 4)), k(RED_SHADE, 1.25))
    if style == 'brow':                                           # a crag of a brow, its black brows bristling
        skin(p, f, warts=0.4)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            if j < fh - 1 and (i < fw * 0.42 or i > fw * 0.58):
                p.px(xy, k(mix(HAIR, HAIR_GREY, 0.2 * p.rnd.random()), 1.3))
        w.hline(p, f['front'], fh - 1, k(RED_SHADE, 0.7))
        w.flat(p, f, k(RED_SHADE, 0.55), faces=('bottom',))
        for i, j, xy in w.each(p, f['top']):
            if p.rnd.random() < 0.5:
                p.px(xy, k(HAIR, 1.3))
    if style == 'cheeks':
        skin(p, f, warts=0.3)
        fx, fy, fw, fh = f['front']
        for j in range(fh):                                       # the deep creases from the nose
            p.px((fx + fw // 2 - 3 - j // 2, fy + j), k(RED_SHADE, 0.85))
            p.px((fx + fw // 2 + 2 + j // 2, fy + j), k(RED_SHADE, 0.85))
    if style == 'nose':                                           # broad and flat, nostrils flared
        skin(p, f, warts=0.2)
        fx, fy, fw, fh = f['front']
        for i in (1, 2, fw - 3, fw - 2):
            p.px((fx + i, fy + fh - 1), (30, 8, 8))
        p.px((fx + fw // 2, fy + 1), RED_LIGHT)
        fx, fy, fw, fh = f['bottom']
        for i in (1, 2, fw - 3, fw - 2):
            for j in range(fh):
                p.px((fx + i, fy + j), (30, 8, 8))
    if style == 'nosering':                                       # an iron ring through its nose
        w.cut_all(p, f, ('right', 'left', 'top', 'bottom'))
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                ring = i in (0, fw - 1) or j in (fh - 1,)
                if ring:
                    p.px(xy, k(GOLD, 1.0 if j < fh - 1 else 0.8))
                else:
                    p.clear(xy)
    if style == 'lip':                                            # its upper lip: the teeth under it, gapped
        skin(p, f, warts=0.1)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy + fh - 1), TOOTH if i % 3 else GUM)
        w.flat(p, f, GUM, faces=('bottom',))
    if style in ('hairspike', 'hairspike_side'):                  # a tuft of its hair, bristling to ragged points
        mane(p, f, ragged=False)
        for s_ in S:
            w.point(p, f[s_], down=False, keep=0.8, sharp=0.5)
            w.tufts(p, f[s_], depth=2, every=2, down=False)
        w.cut_all(p, f, ('top',))
    if style == 'pec':                                            # the slabs of its chest, lit across their swell
        skin(p, f, warts=0.05, fall=0.1)
        fx, fy, fw, fh = f['front']
        muscle(p, f['front'], fw * 0.5, fh * 0.4, fw * 0.55, fh * 0.6, k_=1.2, rim=0.6)
        for _ in range(10):
            p.px((fx + p.rnd.randrange(fw), fy + p.rnd.randrange(fh)), k(HAIR, 1.4))
    if style == 'bicep':
        skin(p, f, warts=0.05, fall=0.1)
        fx, fy, fw, fh = f['front']
        muscle(p, f['front'], fw * 0.5, fh * 0.45, fw * 0.55, fh * 0.55, k_=1.18, rim=0.6)
    if style == 'tophair':                                        # its wild hair, bristling up off its crown
        mane(p, f, ragged=False)
        for s in S:
            w.tufts(p, f[s], depth=3, every=2, down=False)
    if style in ('mane', 'sidemane'):
        mane(p, f)
    if style == 'jaw':                                            # the underbite: lower teeth jutting over its lip
        skin(p, f, warts=0.3)
        fx, fy, fw, fh = f['front']
        for i in range(fw):
            p.px((fx + i, fy), TOOTH if i % 2 == 0 else (60, 16, 18))
            if i % 4 == 0:
                p.px((fx + i, fy + 1), TOOTH)
        fx, fy, fw, fh = f['top']
        for i, j, xy in w.each(p, f['top']):
            p.px(xy, GUM if 0 < i < fw - 1 and j < fh - 2 else TOOTH)
    if style == 'beard':                                          # a scruff of black beard on its chin
        mane(p, f)
    if style in ('tusk', 'tusktip'):                              # great tusks, yellowed ivory, dark at the root
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                t = 1 - j / max(1, fh - 1)                        # 0 root .. 1 point
                c = mix((150, 128, 92), TOOTH, min(1, t * 1.5)) if style == 'tusk' else mix(TOOTH, (250, 244, 228), t)
                p.px(xy, k(c, w.LIGHT[s]))
        if style == 'tusktip':
            for s in S:
                w.point(p, f[s], down=False, keep=0.25, sharp=0.5)
    if style == 'eye':                                            # bulging eyes, burning yellow, a red rim, a black pupil
        skin(p, f)
        fx, fy, fw, fh = f['front']                               # 6x3
        for i, j, xy in w.each(p, f['front']):
            c = mix(EYE, (255, 250, 200), 0.4) if j == 1 else EYE
            if i in (0, fw - 1):
                c = (230, 90, 40)
            p.lit(xy, c)
        for j in range(fh):
            p.px((fx + fw // 2, fy + j), (20, 10, 6))
            p.glow.putpixel((fx + fw // 2, fy + j), (0, 0, 0, 0))
        p.lit((fx + fw // 2 - 1, fy), (255, 255, 236))
        fx, fy, fw, fh = f['top']
        w.flat(p, f, k(RED_SHADE, 0.8), faces=('top',))
    if style == 'ear':
        skin(p, f)
        fx, fy, fw, fh = f['front']
        for i, j, xy in w.each(p, f['front']):
            if 0 < j < fh - 1 and i > 0:
                p.px(xy, k(RED_SHADE, 1.2))
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                t = i / max(1, fw - 1) if s == 'front' else 1 - i / max(1, fw - 1)
                keep = 0.3 + 0.7 * t
                for j in range(fh):
                    if j < fh * (1 - keep) * 0.8:
                        p.clear((fx + i, fy + j))
    if style in ('hoop', 'hoop_side'):
        w.iron(p, f, GOLD, rivets=0, rim=False, dents=0.05)
    if style in ('horn1', 'horn2', 'horn3'):                      # horns of bone, ridged, darkening to their points
        base = {'horn1': (120, 104, 82), 'horn2': (200, 186, 150), 'horn3': (226, 214, 180)}[style]
        tip = {'horn1': (200, 186, 150), 'horn2': (226, 214, 180), 'horn3': (70, 58, 46)}[style]
        for s in f:
            fx, fy, fw, fh = f[s]
            for i, j, xy in w.each(p, f[s]):
                t = 1 - j / max(1, fh - 1)
                c = mix(base, tip, t)
                if j % 3 == 0:
                    c = k(c, 0.78)
                p.px(xy, k(c, w.LIGHT[s]))
        if style == 'horn3':
            for s in S:
                w.point(p, f[s], down=False, keep=0.3, sharp=0.4)

    if style == 'shoulder':                                       # the round of its shoulder
        skin(p, f)
        for s in ('front', 'right', 'left', 'back'):
            fx, fy, fw, fh = f[s]
            muscle(p, f[s], fw * 0.5, fh * 0.4, fw * 0.48, fh * 0.5, k_=1.15)
    if style == 'upperarm':
        skin(p, f)
        for s in ('front', 'right'):
            fx, fy, fw, fh = f[s]
            muscle(p, f[s], fw * 0.5, fh * 0.35, fw * 0.42, fh * 0.35)
    if style == 'forearm':
        skin(p, f)
        for s in S:
            fx, fy, fw, fh = f[s]
            for i in range(1, fw, 4):                             # its veins
                for j in range(fh):
                    if (j + i) % 3:
                        p.px((fx + i + (j // 4) % 2, fy + j), k(RED_SHADE, 1.2))
    if style == 'bracer':                                         # an iron bracer, studded, rimmed
        w.iron(p, f, IRON, rust=0.2, rivets=3, rustc=RUST)
    if style in ('bspike', 'bspike_x', 'pspike', 'cspike', 'cspike_x', 'capspike'):  # iron spikes, bright at their points
        w.iron(p, f, IRON, rivets=0, rim=False, dents=0)
        for s in f:
            fx, fy, fw, fh = f[s]
            p.px((fx + fw // 2, fy + fh // 2), (170, 168, 176))
        if style in ('pspike', 'capspike'):
            for s in S:
                w.point(p, f[s], down=False, keep=0.3, sharp=0.5)
    if style == 'fist':                                           # a great fist, knuckles lit, black nails
        skin(p, f, warts=0.2)
        fx, fy, fw, fh = f['front']
        for i in range(0, fw, 3):
            for t in range(2):
                p.px((fx + i + t, fy + fh - 2), k(RED_LIGHT, 1.0))
                p.px((fx + i + t, fy + fh - 1), k(NAIL, 1.3))
            p.px((fx + i + 2, fy + fh - 2), k(RED_SHADE, 0.9))
    if style == 'thumb':
        skin(p, f)
        nails(p, f)
    if style in ('plate', 'plate2'):                              # the spiked iron plate over its left shoulder
        w.iron(p, f, IRON, rust=0.25, rivets=3, dents=0.3, rustc=RUST)
        for s in S:
            fx, fy, fw, fh = f[s]
            w.hline(p, f[s], fh - 1, GOLD_DEEP)

    if style == 'grip':                                           # the club's grip, bound in leather
        w.leather(p, f, LEATHER)
        for s in S:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                if (j + (s == 'front')) % 3 == 0:
                    w.hline(p, f[s], j, k(LEATHER, 0.6 * w.LIGHT[s]))
    if style in ('pommel', 'collar', 'clubcap'):
        w.iron(p, f, IRON, rust=0.2, rivets=2, rustc=RUST)
        w.border(p, f, GOLD_DEEP, bottom=False)
    if style == 'clubhead':                                       # the club's iron head: dark, hammered, banded in iron
        w.iron(p, f, IRON, rust=0.35, rivets=0, dents=0.5, rustc=RUST)
        for s in S:
            fx, fy, fw, fh = f[s]
            for j in (2, fh // 2, fh - 3):
                w.hline(p, f[s], j, k(IRON_DARK, w.LIGHT[s]))
                w.hline(p, f[s], j + 1, k(IRON, 1.6 * w.LIGHT[s]))


# ── The Yaksha King (夜叉王), a raided village's chief ──────────────────────────
#
# The ogre's own frame (WildRenderers' ogreLayer, drawn 1.2 times as large: RaidChiefEntity.SIZE), in
# a skin of his own (textures/entity/ogre_chief.png): the blue-black of the old temple yakshas where his
# kind are brick-red, a blood-red mane streaked with ash, a black-and-crimson tiger skin, his iron
# blued and chased with gold (his club, bracers, fetters and shoulder plate), gold rings and a gilded
# necklace of skulls, and eyes burning red-gold.
CHIEF = dict(
    RED=(54, 66, 104), RED_SHADE=(22, 26, 52), RED_LIGHT=(106, 126, 176),
    HAIR=(132, 22, 20), HAIR_GREY=(214, 120, 64),
    TIGER=(150, 30, 26), TIGER_DEEP=(92, 16, 16), STRIPE=(18, 12, 12), TIGER_WHITE=(222, 206, 170),
    IRON=(58, 62, 82), IRON_DARK=(26, 28, 40), RUST=(196, 148, 52),
    GOLD=(250, 204, 80), GOLD_DEEP=(178, 124, 40),
    BONE=(236, 210, 150), BONE_SHADE=(160, 120, 70),
    EYE=(255, 120, 40), GUM=(40, 10, 24), ROPE=(150, 40, 30), LEATHER=(58, 26, 22), NAIL=(16, 12, 20),
)


if __name__ == '__main__':
    p = Painter(73)
    slots = paint(OGRE, p, paint_ogre)
    p.save('ogre')
    w.emit(OGRE, slots, w.JAVA, 'ogre')
    w.report('ogre', OGRE, SIZE, slots)
    globals().update(CHIEF)
    p = Painter(73)
    paint(OGRE, p, paint_ogre)
    p.save('ogre_chief')
