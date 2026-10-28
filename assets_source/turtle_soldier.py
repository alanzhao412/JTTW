"""The Turtle Soldier (龟兵), a sea turtle of the Dragon King's host (not the playable Turtle). As
shrimp_soldier.py: its model and skin come from one list of parts (beastgen): the model is written
into TurtleSoldierRenderer's layer() between its GENERATED markers, the skin painted to the same
box-UV layout at twice the model's resolution, with a second skin for the glint of its eyes drawn
full-bright (turtle_soldier_glow.png).

A sea turtle stood up as a heavy soldier: a great domed shell on its back, built up in tiers so its
dome stands out, every scute of it drawn: the vertebral scutes down its crown, the costal scutes
either side, the ring of marginal scutes about its serrated rim, each scute amber tortoiseshell
streaked dark brown out from its growth centre and ringed with its growth lines; a cream plastron
over its front, divided into its paired scutes; a leathery, wrinkled, scaled neck thrust forward
from the shell; a sea turtle's head, plated with large pale-edged scales, its hooked beak, dark
eyes under heavy brow scales; thick arms ending in broad clawed flippers, stumpy elephantine legs,
a stub of tail. The army's kit, the shrimp's: a teal-lacquered bowl helmet rimmed in gold with a
red plume, teal spaulders, a sea-blue sash across its plastron, a red belt with a gold buckle,
fish-scale tassets and a red apron, teal greaves and bracers; on its left arm a great round shield,
teal lacquer rimmed in gold, a red ring about a gold wave-and-tortoise roundel, a gold boss, red
tassels from its rim; in its right flipper a melon-headed mace (瓜锤) of gilt bronze on a red haft.

Run from the repository root: python assets_source/turtle_soldier.py"""
import math

import beastgen as bg
from beastgen import emit
from fire_court import part, mirrored
from sea_court import aim
from king_yama import paint, point, ragged, circle, mixc, k, noisy
import shrimp_soldier as shrimp
from shrimp_soldier import ShellPainter, LIGHTS, SIDES, lacquer, gold_box, rim, TEAL, TEAL_DEEP, TEAL_LIGHT, GOLD, GOLD_DEEP, RED, RED_DEEP, SEA
import fish_soldier as fish
from fish_soldier import armour_scales, clear_face, line, KIT

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/TurtleSoldierRenderer.java'
OUT = 'src/main/resources/assets/jttw_engine/textures/entity/'

# How much larger than its pixels it is drawn: TurtleSoldierRenderer.SIZE
SIZE = 0.95

AMBER = (222, 150, 58)          # tortoiseshell: amber,
AMBER_PALE = (252, 212, 120)
SHELL_DARK = (74, 40, 20)       # streaked dark brown
SEAM = (36, 22, 12)
PLASTRON = (228, 196, 124)
PLASTRON_DEEP = (166, 128, 72)
SKIN = (100, 110, 72)           # olive, leathery
SKIN_DEEP = (56, 62, 42)
SKIN_PALE = (186, 178, 126)     # the pale edges of its scales
BEAK = (156, 132, 84)
BEAK_DEEP = (78, 62, 38)
CLAW = (64, 52, 40)
PLATE = (98, 82, 48)            # the plates of its head and flippers: brown,
PLATE_EDGE = (200, 180, 116)    # edged cream
BRONZE = (196, 150, 64)

BODY_LEAN = 0.05
ARM, FOREARM, FLIPPER = (-0.3, 0, 0.42), (-0.7, 0, -0.15), (-0.2, 0, 0)
LARM, LFOREARM = (-0.85, 0.2, -0.28), (-0.85, 0, 0)

# the shell's tiers, from the back of the body outward, each an oval: (half-width, half-height, z from, z to);
# each built of boxes stepping in toward its ends so its outline is round
CY = -0.2
OVALS = ((7.4, 8.9, -0.8, 0.4), (7.0, 8.5, 0.4, 3.4), (6.1, 7.6, 3.4, 5.6), (4.9, 6.3, 5.6, 7.2), (3.4, 4.8, 7.2, 8.2), (1.8, 3.0, 8.2, 8.8))
STEPS = ((0.98, 0.42), (0.9, 0.66), (0.74, 0.84), (0.48, 0.98))
TIERS = []
for n, (A, B, z0, z1) in enumerate(OVALS):
    for m, (wf, hf) in enumerate(STEPS):
        w, h = round(2 * A * wf, 1), round(2 * B * hf, 1)
        TIERS.append((-w / 2, CY - h / 2, z0, w, h, round(z1 - z0, 1), 'tier%d' % n))
TIER_STYLES = sorted(set(t[6] for t in TIERS))


def body():
    return [
        part('body', 'root', (0, 14, 0.6), (BODY_LEAN, 0, 0), [
            (-5, -14, -3.4, 10, 14, 7, 'torso'),
            (-4.6, -13.6, -4.4, 9.2, 12.8, 1.2, 'plastron'),
            (-5.4, -2.4, -4.9, 10.8, 1.8, 8.6, 'belt'),
            (-1.4, -2.8, -5.5, 2.8, 2.6, 0.8, 'buckle'),
        ]),
        part('right_bridge', 'body', (-5.0, -12.6, -2.8), (0, 0, 0), [(-1.2, 0, 0, 1.4, 10.4, 6.4, 'bridge')]),
        part('sash', 'body', (0, -7.0, -5.0), (0, 0, 0.62), [(-1.2, -7.6, -0.3, 2.4, 14.6, 0.6, 'sash')]),
        part('apron', 'body', (0, -0.8, -4.9), (-0.06, 0, 0), [(-1.6, 0, -0.2, 3.2, 6.6, 0.4, 'apron')]),
        part('right_tasset', 'body', (-2.8, -0.9, -4.5), (-0.18, 0, 0.14), [(-2.6, 0, -0.4, 5.0, 5.6, 0.8, 'tasset')]),
        # the shell: tiers built out from its back so it stands up in a dome
        part('shell', 'body', (0, -6.6, 3.2), (-0.04, 0, 0), list(TIERS)),
        part('tail', 'body', (0, 0.2, 3.0), (-0.9, 0, 0), [(-1.1, -1.1, 0, 2.2, 2.2, 3.2, 'tailskin')]),
    ]


def head():
    return [
        # the neck, thrust forward and up out of the shell, wrinkled; a fold of skin at its base
        part('neck', 'body', (0, -13.4, -0.8), (0.42, 0, 0), [
            (-2.4, -5.2, -2.4, 4.8, 5.6, 4.8, 'neck'),
            (-3.0, -1.2, -3.0, 6.0, 1.8, 6.0, 'skinfold'),
        ]),
        part('head', 'neck', (0, -4.8, -0.4), (-0.42, 0, 0), [
            (-2.9, -4.8, -5.6, 5.8, 5.0, 7.0, 'turtlehead'),
            (-2.1, -3.6, -7.6, 4.2, 2.8, 2.2, 'beak'),
            (-1.0, -1.6, -8.2, 2.0, 1.6, 1.2, 'beaktip'),
        ]),
        part('jaw', 'head', (0, 0.0, -0.6), (0.08, 0, 0), [
            (-2.5, 0, -6.0, 5.0, 1.4, 6.0, 'jaw'),
            (-1.8, -0.5, -7.2, 3.6, 1.3, 1.4, 'lowbeak'),
        ]),
        part('right_eye', 'head', (-2.9, -2.8, -3.8), (0, 0.15, 0), [
            (-0.5, -1.0, -1.3, 0.6, 2.0, 2.6, 'turtleeye'),
            (-0.8, -2.0, -1.8, 1.0, 1.0, 3.4, 'browscale'),
        ]),
        # the soldier's helmet: the shrimp's teal bowl, gold-rimmed, a red plume
        part('helmet', 'head', (0, -4.8, -1.4), (0.1, 0, 0), [
            (-3.5, -0.8, -3.5, 7.0, 1.0, 7.0, 'helmbrim'),
            (-2.8, -3.6, -2.8, 5.6, 3.0, 5.6, 'helmbowl'),
            (-0.7, -4.8, -0.7, 1.4, 1.3, 1.4, 'helmknob'),
            (-1.7, -8.0, -1.7, 3.4, 3.4, 3.4, 'plume'),
            (-3.2, 0, 2.6, 6.4, 2.2, 1.0, 'helmguard'),
        ]),
        part('plume_tail', 'helmet', (0, -6.8, 1.2), (1.0, 0, 0), [(-1.4, 0, -0.8, 2.8, 6, 1.6, 'plumetail')]),
    ]


def legs():
    return [
        part('right_leg', 'root', (-3.0, 13.6, 0.6), (-0.06, 0, 0.08), [(-2.4, -0.4, -2.4, 4.8, 5.4, 4.8, 'thigh')]),
        part('right_shin', 'right_leg', (0, 4.8, 0), (0.08, 0, -0.08), [
            (-2.2, 0, -2.2, 4.4, 4.2, 4.4, 'shin'),
            (-2.5, 0.2, -2.7, 5.0, 3.2, 1.0, 'greave')]),
        part('right_foot', 'right_shin', (0, 4.0, 0), (-0.02, 0, 0), [
            (-2.6, 0, -3.2, 5.2, 1.8, 5.4, 'foot'),
            (-2.5, 0.6, -4.2, 1.2, 1.2, 1.2, 'claw'),
            (-0.6, 0.6, -4.4, 1.2, 1.2, 1.4, 'claw'),
            (1.3, 0.6, -4.2, 1.2, 1.2, 1.2, 'claw')]),
    ]


def arms():
    held = aim([(BODY_LEAN, 0, 0), ARM, FOREARM, FLIPPER], (-0.12, 0, -0.38))
    return [
        part('right_shoulder', 'body', (-5.4, -13.0, 0.2), (0, 0, 0.42), [
            (-3.8, -0.8, -3.0, 4.4, 1.4, 6.0, 'spauldron'),
            (-4.3, 0.4, -3.2, 4.6, 1.6, 6.4, 'spauldron2'),
            (-4.5, 1.8, -3.3, 4.0, 1.5, 6.6, 'spauldron')]),
        part('right_arm', 'body', (-5.8, -12.2, 0.4), ARM, [(-1.9, -0.4, -1.9, 3.8, 5.6, 3.8, 'upperarm')]),
        part('right_forearm', 'right_arm', (0, 5.0, 0), FOREARM, [
            (-1.7, 0, -1.7, 3.4, 3.8, 3.4, 'forearm'),
            (-2.0, 0.6, -2.0, 4.0, 2.8, 4.0, 'bracer')]),
        # the flipper: a broad clawed paddle, curled about the haft
        part('right_flipper', 'right_forearm', (0, 3.6, 0), FLIPPER, [
            (-2.2, 0, -0.8, 4.4, 3.6, 1.6, 'flipper'),
            (-2.6, 0.6, -1.0, 0.9, 1.4, 1.0, 'claw'),
            (-2.6, 2.2, -1.0, 0.9, 1.4, 1.0, 'claw')]),
        part('right_flipper_tip', 'right_flipper', (0, 3.4, 0.2), (0.9, 0, 0), [(-2.0, 0, -0.7, 4.0, 3.2, 1.3, 'flippertip')]),
        # the mace: a gilt bronze melon on a red haft, a red tassel at its collar
        part('mace', 'right_flipper', (0, 2.6, -1.4), held, [
            (-0.5, -13, -0.5, 1.0, 19, 1.0, 'shaft'),
            (-0.8, -13.8, -0.8, 1.6, 1.4, 1.6, 'collar'),
            (-1.2, -12.6, -1.2, 2.4, 3.6, 2.4, 'tassel'),
            (-2.1, -19.0, -2.1, 4.2, 5.2, 4.2, 'melon'),
            (-2.5, -18.4, -1.0, 5.0, 4.0, 2.0, 'melonrib'),
            (-1.0, -18.4, -2.5, 2.0, 4.0, 5.0, 'melonrib'),
            (-0.6, -20.4, -0.6, 1.2, 1.6, 1.2, 'collar'),
            (-0.8, 6.0, -0.8, 1.6, 1.4, 1.6, 'collar'),
        ]),
    ]


def left_arm():
    """The left arm, its shield before it (not mirrored from the right: it holds another thing)."""
    shield = aim([(BODY_LEAN, 0, 0), LARM, LFOREARM], (0.04, -0.3, 0))
    return [
        part('left_arm', 'body', (5.8, -12.2, 0.4), LARM, [(-1.9, -0.4, -1.9, 3.8, 5.6, 3.8, 'upperarm')], True),
        part('left_forearm', 'left_arm', (0, 5.0, 0), LFOREARM, [
            (-1.7, 0, -1.7, 3.4, 3.8, 3.4, 'forearm'),
            (-2.0, 0.6, -2.0, 4.0, 2.8, 4.0, 'bracer')], True),
        part('left_flipper', 'left_forearm', (0, 3.6, 0), (0.3, 0, 0), [
            (-2.2, 0, -0.8, 4.4, 3.6, 1.6, 'flipper'),
            (-2.6, 0.6, -1.0, 0.9, 1.4, 1.0, 'claw'),
            (-2.6, 2.2, -1.0, 0.9, 1.4, 1.0, 'claw')], True),
        # the great round shield, strapped to the forearm
        part('shield', 'left_forearm', (0.6, 2.0, -2.4), shield, [
            (-7, -7, -0.8, 14, 14, 0.8, 'shield'),
            (-5.2, -5.2, -1.4, 10.4, 10.4, 0.6, 'shield2'),
            (-1.8, -1.8, -2.4, 3.6, 3.6, 1.0, 'boss'),
        ]),
        part('shield_tassel_l', 'shield', (-4.2, 5.2, -0.4), (0, 0, 0), [(-0.8, 0, -0.6, 1.6, 4.4, 1.2, 'tassel')]),
        part('shield_tassel_r', 'shield', (4.2, 5.2, -0.4), (0, 0, 0), [(-0.8, 0, -0.6, 1.6, 4.4, 1.2, 'tassel')]),
    ]


def ordered(parts):
    out, seen, pending = [], {'root'}, list(parts)
    while pending:
        rest = [q for q in pending if q[1] not in seen]
        for q in pending:
            if q[1] in seen:
                out.append(q)
                seen.add(q[0])
        assert len(rest) < len(pending)
        pending = [q for q in rest if q[0] not in seen]
    return out


TURTLE = [q for q in mirrored(body() + head() + legs() + arms()) if q[0] not in ('left_arm', 'left_forearm', 'left_flipper')]
TURTLE += [(n, pa, pv, r, [fish.mirror_box(b) if m else b for b in bx], m) for (n, pa, pv, r, bx, m) in left_arm()]
TURTLE = ordered(TURTLE)


# ── Painting ─────────────────────────────────────────────────────────────────

# The scutes, in the shell's own frame as seen from behind (X across, -7.4..7.4; Y down, -9..8.6):
# five vertebral scutes down the middle, four costal scutes each side, the marginal ring.
VERT = 2.3
COSTAL = 5.9


def scute(X, Y):
    """Which scute (X, Y) lies on, its growth centre, and how near its seam (0 on the seam)."""
    ax = abs(X)
    if ax > COSTAL or Y < -7.6 or Y > 7.2:                        # the marginal ring: small scutes about the rim
        if ax > COSTAL:
            n = math.floor((Y + 9) / 2.2)
            c = (math.copysign(6.6, X), -9 + 2.2 * n + 1.1)
            seam = min((Y + 9) % 2.2, 2.2 - (Y + 9) % 2.2, (ax - COSTAL))
        else:
            n = math.floor((X + 7.4) / 2.0)
            c = (-7.4 + 2.0 * n + 1.0, -8.3 if Y < 0 else 7.9)
            seam = min((X + 7.4) % 2.0, 2.0 - (X + 7.4) % 2.0, abs(Y) - (7.6 if Y < 0 else 7.2))
        return ('m', n, X > 0, Y > 0), c, seam
    if ax < VERT + 0.5 * math.cos(Y * 0.9):                      # the vertebral scutes: hexagons down the crown
        span = 14.8 / 5
        yy = Y + 7.6 - 0.35 * ax
        n = max(0, min(4, math.floor(yy / span)))
        top, bot = n * span, (n + 1) * span
        c = (0, -7.6 + top + span * 0.6)
        seam = min(yy - top, bot - yy, VERT + 0.5 * math.cos(Y * 0.9) - ax)
        return ('v', n), c, max(0, seam)
    span = 14.8 / 4                                               # the costal scutes: four each side, offset half a scute
    yy = Y + 7.6 + 0.3 * (ax - VERT)
    n = max(0, min(3, math.floor(yy / span)))
    top, bot = n * span, (n + 1) * span
    c = (math.copysign(4.4, X), -7.6 + top + span * 0.55)
    seam = min(yy - top, bot - yy, ax - (VERT + 0.5 * math.cos(Y * 0.9)), COSTAL - ax)
    return ('c', n, X > 0), c, max(0, seam)


def tortoiseshell(p, xy, X, Y, light=1.0, height=0.0):
    """One pixel of the shell at (X, Y): the scute's amber, streaked dark from its growth centre and
    ringed with growth lines, its seam dark; `height` lightens the higher tiers."""
    key, (cx, cy), seam = scute(X, Y)
    dx, dy = X - cx, Y - cy
    d = math.hypot(dx, dy)
    ang = math.atan2(dy, dx)
    h = hash(key) % 1000 / 1000.0
    streak = math.sin(ang * 4 + h * 9 + d * 0.5) * 0.55 + math.sin(ang * 9 + h * 4) * 0.3 + math.sin(d * 1.1 + h * 6) * 0.25
    col = mixc(AMBER, AMBER_PALE, 0.2 + 0.25 * height)
    if streak > 0.15:                                             # dark flames radiating from its growth centre
        col = mixc(col, SHELL_DARK, min(0.9, (streak - 0.15) * 1.6))
    if 0.6 < seam and (seam * 2.6) % 1 < 0.22:                    # growth rings, following its edge
        col = mixc(col, SHELL_DARK, 0.35)
    if d < 0.5:
        col = mixc(col, AMBER_PALE, 0.6)                          # the growth centre, worn pale
    if seam < 0.25:
        col = SEAM
    elif seam < 0.55:
        col = mixc(col, AMBER_PALE, 0.45)                         # the scute's raised edge, catching the light
    p.px(xy, noisy(p, k(col, light), 3))


def paint_tier(p, f, dims, style):
    w, h, d = dims
    x0, y0 = -w / 2, CY - h / 2
    tier = int(style[4:])
    height = tier / (len(OVALS) - 1)
    rim_tier = tier == 0
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                u, v = (i + 0.5) / fw, (j + 0.5) / fh
                if s == 'back':                                   # seen from behind: +x (its left) at the texture's left
                    X, Y = x0 + w * (1 - u), y0 + h * v
                elif s == 'front':
                    X, Y = x0 + w * u, y0 + h * v
                elif s in ('right', 'left'):
                    X, Y = (x0 if s == 'right' else x0 + w), y0 + h * v
                else:
                    X, Y = x0 + w * u, (y0 if s == 'top' else y0 + h)
                if s == 'front' and not rim_tier:
                    p.px((fx + i, fy + j), SKIN_DEEP)
                    continue
                if rim_tier and s != 'back':
                    # the rim: serrated, each marginal scute's point jutting, seen edge on
                    along = v if s in ('right', 'left') else u
                    n = along * (h if s in ('right', 'left') else w) / 2.1
                    col = mixc(AMBER, SHELL_DARK, 0.2 if int(n) % 2 else 0.5)
                    if (n % 1) < 0.2:
                        col = SEAM
                    if s == 'front':
                        col = k(col, 0.7)
                    p.px((fx + i, fy + j), noisy(p, k(col, LIGHTS[s]), 3))
                    continue
                tortoiseshell(p, (fx + i, fy + j), X, Y, LIGHTS[s] * (0.9 if s != 'back' else 1.0), height)
        if s in ('right', 'left', 'top', 'bottom') and not rim_tier:
            # where each tier rises from the one below: a dark seam
            if s in ('right', 'left'):
                c0 = fw - 1 if s == 'right' else 0
                for j in range(fh):
                    p.px((fx + c0, fy + j), SEAM)
            else:
                for i in range(fw):
                    p.px((fx + i, fy + (fh - 1 if s == 'top' else 0)), SEAM)


PLASTRON_SEAMS = (0.12, 0.27, 0.48, 0.7, 0.88)                    # gular, humeral, pectoral, abdominal, femoral, anal


def leathery(p, f, base=SKIN, cell=3, fold=True, scales=True):
    """Leathery skin: small pale-edged scales in a crazed net, the folds of its wrinkles darker."""
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                c = base
                if scales:
                    jj = j + (2 if (i // cell) % 2 else 0)
                    if jj % cell == 0 or i % cell == 0:
                        c = mixc(base, SKIN_PALE, 0.35)
                    if (jj % cell == cell - 1):
                        c = k(base, 0.8)
                if fold and j % 5 == 4:
                    c = k(SKIN_DEEP, 0.9)
                if s == 'bottom' or (s == 'front' and fold and False):
                    c = mixc(c, SKIN_PALE, 0.3)
                p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 4))


def head_plates(p, rect, light, cells=((0, 0, 0.5, 0.45), (0.5, 0, 1, 0.45), (0, 0.45, 0.35, 1), (0.35, 0.45, 0.7, 1), (0.7, 0.45, 1, 1))):
    """Large plates over a sea turtle's head: olive, each edged pale, darker toward its middle."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            u, v = (i + 0.5) / fw, (j + 0.5) / fh
            c = PLATE_EDGE
            for (a, b, c2, d2) in cells:
                if a <= u < c2 and b <= v < d2:
                    e = min((u - a) * fw, (c2 - u) * fw, (v - b) * fh, (d2 - v) * fh)
                    c = mixc(mixc(PLATE, (150, 120, 70), 0.5), PLATE, min(1, e / 3))
                    if e < 0.8:
                        c = PLATE_EDGE
                    break
            p.px((fx + i, fy + j), noisy(p, k(c, light), 3))


def paint_turtle(p, style, f, dims):
    if style in KIT:
        shrimp.paint_shrimp(p, style, f, dims)
        return
    if style in TIER_STYLES:
        paint_tier(p, f, dims, style)
    if style == 'torso':
        leathery(p, f)
    if style == 'bridge':                                         # the bridge joining the plastron to the carapace: shell
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    c = mixc(PLASTRON, AMBER, 0.5)
                    if j % 7 == 0:
                        c = PLASTRON_DEEP
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))
    if style == 'plastron':                                       # cream, its paired scutes, a seam down the middle, growth lines
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    v = (j + 0.5) / fh
                    c = mixc(PLASTRON, PLASTRON_DEEP, abs((i + 0.5) / fw - 0.5) * 0.7)
                    if s == 'front':
                        seg = sum(1 for q in PLASTRON_SEAMS if v > q)
                        lo = ([0] + list(PLASTRON_SEAMS))[seg]
                        hi = (list(PLASTRON_SEAMS) + [1])[seg]
                        t = (v - lo) / (hi - lo)
                        if (t * 6) % 2 < 0.35:
                            c = k(c, 0.9)                         # growth lines across each scute
                        if t < 0.1 or t > 0.93:
                            c = (110, 78, 44)                     # the seams between its scutes
                        if abs(i + 0.5 - fw / 2) < 0.7:
                            c = (110, 78, 44)                     # the midline seam
                        if i == 0 or i == fw - 1:
                            c = mixc(c, AMBER, 0.4)
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))
    if style in ('neck', 'skinfold', 'upperarm', 'thigh', 'tailskin'):
        leathery(p, f, cell=3, fold=style in ('neck', 'skinfold', 'upperarm', 'thigh'))
        if style == 'skinfold':
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), SKIN_PALE)
                    p.px((fx + i, fy + fh - 1), SKIN_DEEP)
    if style in ('shin', 'forearm'):
        leathery(p, f, cell=4, fold=False)
    if style == 'turtlehead':                                     # the head: plated with large pale-edged scales, paler beneath
        leathery(p, f, cell=3, fold=False)
        head_plates(p, f['top'], LIGHTS['top'],
                    cells=((0, 0, 0.5, 0.3), (0.5, 0, 1, 0.3), (0, 0.3, 0.33, 0.62), (0.33, 0.3, 0.67, 0.62), (0.67, 0.3, 1, 0.62),
                           (0, 0.62, 0.5, 1), (0.5, 0.62, 1, 1)))
        for s in ('right', 'left'):
            head_plates(p, f[s], LIGHTS[s], cells=((0, 0, 0.3, 0.5), (0.3, 0, 0.62, 0.42), (0.62, 0, 1, 0.38), (0, 0.5, 0.45, 1),
                                                     (0.45, 0.42, 0.75, 0.85), (0.75, 0.38, 1, 0.8)))
        fx, fy, fw, fh = f['bottom']
        p.face((fx, fy, fw, fh), SKIN_PALE, 0.8, noise=3)
        fx, fy, fw, fh = f['front']                               # its face: one broad plate between the eyes
        for j in range(fh):
            for i in range(fw):
                u, v = (i + 0.5) / fw, (j + 0.5) / fh
                c = PLATE if 0.22 < u < 0.78 and v < 0.62 else SKIN
                if (0.18 < u < 0.26 or 0.74 < u < 0.82) and v < 0.66 or 0.6 < v < 0.68:
                    c = PLATE_EDGE
                p.px((fx + i, fy + j), noisy(p, c, 3))
    if style in ('beak', 'beaktip', 'lowbeak'):                   # the horny beak: pale horn, dark at its cutting edge
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    t = j / max(1, fh - 1)
                    c = mixc(BEAK, BEAK_DEEP, t * 0.8 if style != 'lowbeak' else (1 - t) * 0.8)
                    if (i + j) % 6 == 0:
                        c = k(c, 0.9)
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))
        if style == 'beaktip':
            for s in ('front', 'right', 'left'):
                point(p, f[s], keep=0.6, down=True, sharp=0.7)
        if style == 'beak':
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), (40, 30, 20))
            for x in (fx + 2, fx + fw - 3):                       # its nostrils
                p.px((x, fy + 1), (30, 24, 18))
    if style == 'jaw':
        leathery(p, f, cell=3, fold=False)
        fx, fy, fw, fh = f['top']
        for j in range(fh):
            for i in range(fw):
                p.px((fx + i, fy + j), (70, 30, 30) if 1 < i < fw - 2 else BEAK_DEEP)
    if style == 'turtleeye':                                      # a dark, wet eye, a glint
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    c = (22, 18, 16)
                    if s == 'right' and (i == 0 or i == fw - 1 or j == 0 or j == fh - 1):
                        c = (60, 46, 30)
                    p.px((fx + i, fy + j), c)
            if s == 'right':
                p.px((fx + fw - 2, fy + 1), (255, 255, 240), glow=True)
                p.px((fx + fw - 3, fy + 1), (200, 210, 220), glow=True)
                p.px((fx + 2, fy + fh - 2), (120, 90, 50))
    if style == 'browscale':                                      # the heavy scale over the eye
        head_plates(p, f['top'], LIGHTS['top'], cells=((0, 0, 0.5, 1), (0.5, 0, 1, 1)))
        for s in ('right', 'left', 'front', 'back', 'bottom'):
            p.face(f[s], mixc(SKIN, SKIN_PALE, 0.4), LIGHTS[s], noise=3)
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), SKIN_DEEP)
    if style == 'foot':                                           # elephantine feet: scaled, pale soles
        leathery(p, f, cell=3, fold=False)
        p.face(f['bottom'], SKIN_PALE, 0.7, noise=3)
    if style == 'claw':
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), noisy(p, k(mixc(CLAW, BEAK, 0.4 * (1 - j / max(1, fh))), LIGHTS[s]), 3))
    if style in ('flipper', 'flippertip'):                        # the flipper: big pale-edged scales over it, a leading edge of plates
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    c = mixc(PLATE, SKIN_DEEP, 0.25)
                    jj = j + (2 if (i // 3) % 2 else 0)
                    if i % 3 == 0 or jj % 4 == 0:                 # the net of its plates
                        c = mixc(PLATE_EDGE, PLATE, 0.45)
                    if s == 'back':
                        c = mixc(c, SKIN_PALE, 0.3)               # paler beneath
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))
        if style == 'flippertip':
            for s in ('front', 'back'):
                point(p, f[s], keep=2, down=True, sharp=1.0)
    # ── the soldier's kit ──
    if style == 'tasset':
        fish.paint_fish(p, 'tasset', f, dims)
    if style == 'apron':
        fish.paint_fish(p, 'apron', f, dims)
    if style == 'helmguard':
        fish.paint_fish(p, 'helmguard', f, dims)
    if style == 'greave':
        fish.paint_fish(p, 'greave', f, dims)
    if style == 'shield':                                         # teal lacquer, a gold rim, a red ring, studs
        for s, (fx, fy, fw, fh) in f.items():
            if s in ('front', 'back'):
                cx, cy, r = fx + fw / 2, fy + fh / 2, fw / 2
                for j in range(fh):
                    for i in range(fw):
                        d = math.hypot(fx + i + 0.5 - cx, fy + j + 0.5 - cy)
                        if d > r + 0.3:
                            p.clear((fx + i, fy + j))
                            continue
                        if s == 'back':                           # its back: dark wood, a leather grip
                            c = (90, 60, 36) if (j // 3) % 2 else (76, 50, 30)
                            if abs(fy + j + 0.5 - cy) < 2.5:
                                c = (110, 40, 30)
                        elif d > r - 1.6:
                            c = GOLD if d < r - 0.6 else GOLD_DEEP
                        elif d > r - 3.2:
                            c = mixc(RED, RED_DEEP, 0.3)
                            a = math.atan2(fy + j + 0.5 - cy, fx + i + 0.5 - cx)
                            if (a * 12 / math.pi) % 2 < 0.35:
                                c = GOLD                          # gold studs round the red ring
                        else:
                            t = (d / r)
                            c = mixc(mixc(TEAL, (170, 236, 222), 0.3), TEAL_DEEP, t)
                        p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 2))
            else:
                clear_face(p, (fx, fy, fw, fh))
    if style == 'shield2':                                        # the roundel: a gold tortoise in a ring of waves, on teal
        for s, (fx, fy, fw, fh) in f.items():
            if s not in ('front', 'back'):
                clear_face(p, (fx, fy, fw, fh))
                continue
            cx, cy, r = fx + fw / 2, fy + fh / 2, fw / 2
            for j in range(fh):
                for i in range(fw):
                    x, y = fx + i + 0.5 - cx, fy + j + 0.5 - cy
                    d = math.hypot(x, y)
                    if d > r + 0.3:
                        p.clear((fx + i, fy + j))
                        continue
                    a = math.atan2(y, x)
                    c = mixc(TEAL, TEAL_DEEP, 0.4)
                    if d > r - 1.2:
                        c = GOLD
                    elif d > r - 3.4:                             # the ring of waves: crests curling round
                        wave = math.sin(a * 8 + (d - r) * 1.6)
                        c = (226, 240, 236) if wave > 0.55 else mixc(SEA, TEAL_DEEP, 0.3)
                    elif d < r - 4.2:
                        # the tortoise (玄武): its shell an oval with a hexagon net, head, four flippers
                        ex, ey = x / 3.4, y / 4.0
                        if ex * ex + ey * ey < 1:
                            c = GOLD if ((round(x) + round(y)) % 3) else GOLD_DEEP
                        if abs(x) < 1.0 and -6.6 < y < -3.6:
                            c = GOLD
                        if 2.6 < abs(x) < 5.0 and 2.0 < abs(y) < 4.6 and abs(abs(x) - abs(y) * 0.9) < 1.1:
                            c = GOLD
                        if abs(x) < 0.6 and 3.8 < y < 5.4:
                            c = GOLD_DEEP
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 2))
    if style == 'boss':                                           # the gold boss, domed, a glint
        gold_box(p, f)
        fx, fy, fw, fh = f['front']
        circle(p, fx + fw / 2, fy + fh / 2, 0, 1.6, (255, 246, 200))
        circle(p, fx + fw / 2 + 0.8, fy + fh / 2 - 0.8, 0, 0.7, (255, 255, 240))
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for (i, j) in ((0, 0), (fw - 1, 0), (0, fh - 1), (fw - 1, fh - 1)):
                p.clear((fx + i, fy + j))
    if style in ('melon', 'melonrib'):                            # the melon head: gilt bronze, ribbed with deep grooves
        for s, (fx, fy, fw, fh) in f.items():
            for j in range(fh):
                for i in range(fw):
                    t = j / max(1, fh - 1)
                    c = mixc((255, 226, 140), BRONZE, min(1, t * 1.4)) if s != 'bottom' else BRONZE
                    if s in SIDES and i % 3 == 0:
                        c = k(BRONZE, 0.6)
                    if s == 'top' and (i % 3 == 0 or j % 3 == 0):
                        c = k(BRONZE, 0.7)
                    p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))


if __name__ == '__main__':
    p = ShellPainter(29)
    slots = paint(TURTLE, p, paint_turtle)
    p.img.save(OUT + 'turtle_soldier.png')
    p.glow.save(OUT + 'turtle_soldier_glow.png')
    import os
    if os.path.exists(JAVA):
        emit(TURTLE, slots, JAVA)
    print('turtle soldier', len(TURTLE), 'parts,', len(slots), 'boxes')
    from lion_camel import extent
    print('   drawn at', SIZE, {kk: round(v, 2) for kk, v in extent(TURTLE, SIZE).items()})
