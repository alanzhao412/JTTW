"""The Victorious Fighting Buddha (斗战胜佛): Sun Wukong at level 100, as the temples paint him once the
journey is done. Not a body of its own but what he wears over his own (and over his armour), drawn by
FightingBuddhaLayer on the player's model, each piece riding the player's head, body, arms and legs:

- the golden kasaya (袈裟), the monk's patched robe of the field-paddy pattern (田相), saffron and gold
  strips sewn between deep red seams, worn over the left shoulder and leaving the right bare: a robe
  over his body to the hips, a fold over the left shoulder and upper arm, its skirt falling over his
  thighs, a red sash edged in gold running from the left shoulder across the breast and the back to
  the right hip, and the gold ring that fastens it at the left breast;
- the gold-ringed fillet (金箍), no longer a binding but an ornament of the Buddha: a band of chased
  gold round his brow (over a helmet too), a ruyi-cloud jewel at the front and a ring at each temple;
- a gold armlet on the bare right arm;
- the halo (头光): a ring of golden light behind his head, rayed within and petalled without, slowly
  turning; and behind his body the aura (身光), a great ring of golden flame-tongues.

The halo, its rays and the aura are drawn full-bright from a glowing skin (fighting_buddha_glow.png).
As king_yama.py: one list of parts gives the model, written into FightingBuddhaLayer.layer() between
its GENERATED markers, and the skin, painted to the same box-UV layout at twice the model's resolution.

Run from the repository root: python assets_source/fighting_buddha.py"""
import math

import beastgen as bg
from beastgen import Painter, emit

bg.UNITS = 128
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/FightingBuddhaLayer.java'

SAFFRON = (222, 146, 38)
SAFFRON_LIGHT = (240, 178, 60)
SAFFRON_DEEP = (196, 116, 28)
GOLD = (238, 190, 70)
GOLD_LIGHT = (255, 226, 130)
GOLD_DEEP = (156, 104, 28)
SEAM = (118, 36, 22)
RED = (164, 40, 28)
RED_DEEP = (110, 22, 16)
JEWEL = (200, 30, 40)
HALO = (255, 214, 110)
HALO_HOT = (255, 244, 196)
AURA = (255, 186, 70)


def part(name, parent, pivot, rot, boxes, mirror=False):
    return (name, parent, pivot, rot, boxes, mirror)


def halo():
    """The head's halo: a ring of light behind his head (its pivot), rays within, petals without."""
    out = [part('halo', 'head', (0, -4, 6.6), (0, 0, 0), [])]
    n = 24
    for k in range(n):
        a = k * 2 * math.pi / n
        r = 7.6
        out.append(part('halo_ring_%d' % k, 'halo', (r * math.sin(a), -r * math.cos(a), 0), (0, 0, a),
                        [(-1.1, -0.6, -0.3, 2.2, 1.2, 0.6, 'halo')]))
    for k in range(12):
        a = k * 2 * math.pi / 12 + math.pi / 12
        out.append(part('halo_ray_%d' % k, 'halo', (0, 0, 0.1), (0, 0, a),
                        [(-0.3, -7.0, -0.1, 0.6, 4.6, 0.2, 'ray')]))
    for k in range(16):
        a = k * 2 * math.pi / 16
        r = 9.0
        out.append(part('halo_petal_%d' % k, 'halo', (r * math.sin(a), -r * math.cos(a), 0.1), (0, 0, a),
                        [(-0.8, -1.4, -0.15, 1.6, 1.9, 0.3, 'petal')]))
    return out


def aura():
    """The body's aura: a great ring of flame-tongues of golden light behind him, open at the bottom."""
    out = [part('aura', 'body', (0, 5, 7.2), (0, 0, 0), [])]
    for k, deg in enumerate(range(-150, 151, 15)):
        a = math.radians(deg)
        r = 13.0
        tall = 3.2 if k % 2 == 0 else 2.2
        # a tongue of light: a broad root, a narrower body, a point, each a little further out
        out.append(part('aura_flame_%d' % k, 'aura', (r * math.sin(a), -r * math.cos(a), 0), (0, 0, a),
                        [(-1.2, -1.2, -0.15, 2.4, 1.2, 0.3, 'aura_root'),
                         (-0.8, -1.2 - tall * 0.55, -0.12, 1.6, tall * 0.55, 0.24, 'aura'),
                         (-0.4, -1.2 - tall, -0.1, 0.8, tall * 0.45, 0.2, 'aura_tip')]))
    for k, deg in enumerate(range(-150, 151, 10)):
        a = math.radians(deg)
        r = 12.0
        out.append(part('aura_rim_%d' % k, 'aura', (r * math.sin(a), -r * math.cos(a), 0.05), (0, 0, a),
                        [(-1.1, -0.5, -0.2, 2.2, 1.0, 0.4, 'aura_rim')]))
    return out


BUDDHA = [
    # the player's own parts, at their pivots (the layer copies the player's pose onto them each frame)
    part('head', 'root', (0, 0, 0), (0, 0, 0), []),
    part('body', 'root', (0, 0, 0), (0, 0, 0), []),
    part('right_arm', 'root', (-5, 2, 0), (0, 0, 0), []),
    part('left_arm', 'root', (5, 2, 0), (0, 0, 0), []),
    part('right_leg', 'root', (-1.9, 12, 0), (0, 0, 0), []),
    part('left_leg', 'root', (1.9, 12, 0), (0, 0, 0), []),

    # ── the kasaya ──
    part('robe', 'body', (0, 0, 0), (0, 0, 0), [(-5.4, -0.4, -3.4, 10.8, 12.8, 6.8, 'robe')]),
    # the sash: from the left shoulder across the breast to the right hip, and so across the back
    part('sash_front', 'body', (0.4, 5.6, -3.55), (0, 0, -0.62), [(-1.7, -8.2, -0.3, 3.4, 16.4, 0.6, 'sash')]),
    part('sash_back', 'body', (0.4, 5.6, 3.55), (0, 0, -0.62), [(-1.7, -8.2, -0.3, 3.4, 16.4, 0.6, 'sash')]),
    # its gold ring and hook at the left breast
    part('clasp', 'body', (2.6, 2.6, -3.95), (0, 0, 0.785), [(-1.1, -1.1, -0.35, 2.2, 2.2, 0.7, 'gold'),
                                                            (-0.5, -0.5, -0.55, 1.0, 1.0, 0.4, 'jewel')]),
    # the fold over the left shoulder and upper arm
    part('sleeve', 'left_arm', (0, 0, 0), (0, 0, 0), [(-2.4, -3.4, -3.4, 6.8, 8.6, 6.8, 'robe')]),
    part('sleeve_hem', 'left_arm', (0, 0, 0), (0, 0, 0), [(-2.6, 4.6, -3.6, 7.2, 1.4, 7.2, 'trim')]),
    # its skirt over the thighs, hemmed in red and gold
    part('skirt_right', 'right_leg', (0, 0, 0), (0, 0, 0), [(-3.4, -0.4, -3.4, 6.8, 9.2, 6.8, 'robe')]),
    part('skirt_left', 'left_leg', (0, 0, 0), (0, 0, 0), [(-3.4, -0.4, -3.4, 6.8, 9.2, 6.8, 'robe')]),
    part('hem_right', 'right_leg', (0, 0, 0), (0, 0, 0), [(-3.6, 8.6, -3.6, 7.2, 1.4, 7.2, 'trim')]),
    part('hem_left', 'left_leg', (0, 0, 0), (0, 0, 0), [(-3.6, 8.6, -3.6, 7.2, 1.4, 7.2, 'trim')]),
    # a gold armlet on the bare right arm
    part('armlet', 'right_arm', (0, 0, 0), (0, 0, 0), [(-4.4, 2.4, -3.4, 6.8, 1.4, 6.8, 'band')]),

    # ── the gold-ringed fillet ──
    part('fillet', 'head', (0, 0, 0), (0, 0, 0), [(-5.3, -6.9, -5.3, 10.6, 1.5, 10.6, 'band')]),
    part('fillet_jewel', 'head', (0, -7.2, -5.6), (0, 0, 0), [(-1.3, -1.2, -0.4, 2.6, 2.4, 0.6, 'jewel_set')]),
    part('fillet_ring_right', 'head', (-5.6, -6.2, -0.6), (0, 0, 0), [(-0.4, -1.4, -1.4, 0.8, 2.8, 2.8, 'ring')]),
    part('fillet_ring_left', 'head', (5.6, -6.2, -0.6), (0, 0, 0), [(-0.4, -1.4, -1.4, 0.8, 2.8, 2.8, 'ring')]),
] + halo() + aura()


def k(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def patchwork(p, rect, light=1.0, hem=False):
    """The field-paddy pattern: strips of saffron and gold in a grid, sewn with dark red seams, edged in red."""
    fx, fy, fw, fh = rect
    cw, ch = 6, 9
    for j in range(fh):
        for i in range(fw):
            ci, cj = i // cw, (j + (3 if (i // cw) % 2 else 0)) // ch
            tone = (SAFFRON, SAFFRON_LIGHT, SAFFRON_DEEP)[(ci * 7 + cj * 3) % 3]
            f = light * (1.04 - 0.12 * (j / max(1, fh - 1)))
            c = k(tone, f + p.rnd.uniform(-0.03, 0.03))
            if i % cw == 0 or (j + (3 if (i // cw) % 2 else 0)) % ch == 0:
                c = k(SEAM, light)
            if i == 0 or i == fw - 1 or j == 0 or j == fh - 1:
                c = k(RED, light)
            if hem and j >= fh - 3:
                c = k(GOLD if j == fh - 2 else RED, light)
            p.px((fx + i, fy + j), c)


def band(p, rect, base, light=1.0, glow=False, chased=True):
    """A band of gold: lit along its top edge, darker below, chased with a fine wave."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            f = light * (1.15 - 0.35 * (j / max(1, fh - 1)))
            c = k(base, f + p.rnd.uniform(-0.03, 0.03))
            if chased and fh >= 3 and j == fh // 2 and (i // 2) % 3 == 0:
                c = k(GOLD_DEEP, light)
            p.px((fx + i, fy + j), c, glow)


def paint_box(p, style, f, dims):
    if style == 'robe':
        for side, light in (('front', 1.0), ('back', 0.86), ('right', 0.92), ('left', 0.92), ('top', 1.06), ('bottom', 0.7)):
            patchwork(p, f[side], light)
    elif style == 'trim':
        for side, light in (('front', 1.0), ('back', 0.86), ('right', 0.92), ('left', 0.92), ('top', 1.06), ('bottom', 0.7)):
            fx, fy, fw, fh = f[side]
            for j in range(fh):
                for i in range(fw):
                    c = GOLD if (fh > 1 and j == fh // 2) else RED
                    p.px((fx + i, fy + j), k(c, light + p.rnd.uniform(-0.04, 0.04)))
    elif style == 'sash':
        for side, light in (('front', 1.0), ('back', 0.8), ('right', 0.9), ('left', 0.9), ('top', 1.0), ('bottom', 0.8)):
            fx, fy, fw, fh = f[side]
            for j in range(fh):
                for i in range(fw):
                    edge = i == 0 or i == fw - 1
                    c = GOLD if edge else (RED if (j // 4) % 2 else RED_DEEP)
                    if not edge and j % 8 == 0:
                        c = GOLD_DEEP
                    p.px((fx + i, fy + j), k(c, light + p.rnd.uniform(-0.04, 0.04)))
    elif style in ('band', 'gold', 'ring'):
        for side, light in (('front', 1.0), ('back', 0.85), ('right', 0.95), ('left', 0.95), ('top', 1.1), ('bottom', 0.7)):
            band(p, f[side], GOLD, light, chased=style == 'band')
        if style == 'ring':  # a ring seen side on: its hollow
            for side in ('right', 'left'):
                fx, fy, fw, fh = f[side]
                for j in range(fh // 3, fh - fh // 3):
                    for i in range(fw // 3, fw - fw // 3):
                        p.px((fx + i, fy + j), GOLD_DEEP)
    elif style in ('jewel', 'jewel_set'):
        for side, light in (('front', 1.0), ('back', 0.8), ('right', 0.9), ('left', 0.9), ('top', 1.05), ('bottom', 0.7)):
            fx, fy, fw, fh = f[side]
            for j in range(fh):
                for i in range(fw):
                    inner = 0 < i < fw - 1 and 0 < j < fh - 1
                    c = JEWEL if (inner or style == 'jewel') else GOLD
                    if inner and i == 1 and j == 1:
                        c = (255, 140, 140)
                    p.px((fx + i, fy + j), k(c, light))
    elif style in ('halo', 'ray', 'petal', 'aura', 'aura_root', 'aura_tip', 'aura_rim'):
        # light: whole faces, the flame's root hottest, its tip deepest in colour
        top, bottom = {'halo': (HALO, HALO), 'ray': (HALO_HOT, HALO), 'petal': (HALO, HALO_HOT),
                       'aura_root': (HALO_HOT, HALO_HOT), 'aura': (AURA, HALO_HOT), 'aura_tip': (GOLD_DEEP, AURA),
                       'aura_rim': (HALO, HALO)}[style]
        for side in ('front', 'back', 'right', 'left', 'top', 'bottom'):
            fx, fy, fw, fh = f[side]
            for j in range(fh):
                for i in range(fw):
                    t = j / max(1, fh - 1)
                    c = tuple(int(a + (b - a) * t) for a, b in zip(top, bottom))
                    p.px((fx + i, fy + j), k(c, 1.0 + p.rnd.uniform(-0.05, 0.05)), glow=True)
    else:
        raise ValueError(style)


if __name__ == '__main__':
    p = Painter(1000)
    slots = bg.paint(BUDDHA, p, paint_box)
    p.save('fighting_buddha')
    emit(BUDDHA, slots, JAVA)
    print('fighting buddha', len(BUDDHA), 'parts,', len(slots), 'boxes')
