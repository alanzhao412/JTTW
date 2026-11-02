"""Heaven's rank and file (天兵天将): the heavenly soldier (a halberdier), the heavenly archer, the
cloud cavalry, the thunder drummer, and the Twenty-Eight Lunar Mansions (二十八宿), Heaven's captains.
As hell_guards.py and king_yama.py: each one's model and skins come from one list of parts
(beastgen): the model is written into HeavenHostRenderers' layers between their GENERATED markers,
the skins painted to the same box-UV layout at twice the model's resolution, with a glowing skin
(drawn full-bright over it) for what shines. The common frame and painting are heaven_host_kit.py's;
the cloud cavalry is heaven_host_cavalry.py's, the Lunar Mansions' beasts heaven_host_emblems.py's.

The heavenly soldier: a halberdier in the gilt mingguang armour of the Tang (two round mirror plates
on the breast, a lion's face on the belly, shoulder guards in three tiers bitten by gold beasts'
heads, a lamellar skirt), azure silk beneath, a gilt helmet with a red horsehair tassel and a flame
plaque, a crescent-bladed halberd (戟) with a red tassel, a sword at his hip. His captain (a Heavenly
General) in crimson and gold: a crimson cloak, phoenix wings on his helmet, a great tassel, a pale
celestial ribbon arching behind his head, a moustache.

Run from the repository root: python assets_source/heaven_host.py [soldier archer drummer mansion cavalry]"""
import math
import sys

import beastgen as bg
from beastgen import emit
from heaven_host_kit import *
from heaven_host_kit import paint

JAVA = 'src/main/java/com/jttw/engine/client/renderer/HeavenHostRenderers.java'

# How much larger than its pixels each is drawn (HeavenHostRenderers' SIZE constants)
TROOPER_SIZE = 0.86


# ── The heavenly soldier ─────────────────────────────────────────────────────

def halberd():
    """The halberd (戟), upright in the right fist: a red-lacquered shaft banded in gold, a spear point, a
    crescent blade on the outer side and a small hook on the inner, a red tassel, a gold butt spike."""
    return [
        part('halberd', 'right_hand', (0, 1.2, 0), aim([ARM, FOREARM, (0, 0, 0)], (0.03, 0, 0)), [
            (-0.45, -25, -0.45, 0.9, 35, 0.9, 'shaft'),
            (-0.8, -26.6, -0.8, 1.6, 1.8, 1.6, 'collar'),
            (-0.6, -28.4, -0.6, 1.2, 1.9, 1.2, 'socket'),
            (-1.0, -36.6, -0.25, 2.0, 8.4, 0.5, 'spearhead'),
            (0.35, -29.4, -0.2, 2.0, 0.8, 0.4, 'hookbar'),
            (1.7, -31.0, -0.2, 0.6, 2.0, 0.4, 'hookbar_up'),
            (-0.5, 9.6, -0.5, 1.0, 2.4, 1.0, 'butt'),
        ]),
        part('crescent', 'halberd', (-0.5, -29.6, 0), (0, 0, 0), [(-5.4, -4.4, -0.25, 5.4, 8.8, 0.5, 'crescent')]),
        part('halberd_tassel', 'halberd', (0, -25.8, 0), (0, 0, 0), [(-1.0, 0, -1.0, 2.0, 2.4, 2.0, 'tassel'),
                                                                     (-0.75, 2.1, -0.75, 1.5, 1.6, 1.5, 'tassel_tip')]),
    ]


def sword():
    """A sword in its scabbard at the left hip, hilt forward."""
    return [part('sword', 'waist', (5.2, -1.8, -0.6), (0.5, 0, -0.2), [
        (-0.55, 0, -0.7, 1.1, 9.5, 1.4, 'scabbard'),
        (-0.65, 9.4, -0.8, 1.3, 1.0, 1.6, 'chape'),
        (-1.3, -0.6, -0.95, 2.6, 0.6, 1.9, 'guard'),
        (-0.4, -3.4, -0.4, 0.8, 2.8, 0.8, 'hilt'),
        (-0.6, -4.2, -0.6, 1.2, 0.9, 1.2, 'pommel'),
    ])]


def cape(length=10.0, lower=9.5, width=5.2):
    """A captain's cloak, from his shoulders down his back in two falls, flaring."""
    return [
        part('cape', 'body', (0, -10.4, 3.3), (0.12, 0, 0), [(-width, 0, -0.4, 2 * width, length, 0.8, 'cape')]),
        part('cape_lower', 'cape', (0, length - 0.3, 0), (0.08, 0, 0), [(-width - 0.9, 0, -0.4, 2 * width + 1.8, lower, 0.8, 'cape_lower')]),
    ]


def ribbon(y=-12.8, r=8.2):
    """The pale celestial ribbon (披帛) arching up behind his head from shoulder to shoulder, its ends trailing."""
    return (ribbon_arch('ribbon', 'body', (0, y, 4.6), r, 166, 374, 10, width=2.2)
            + trail('right_ribbon_end', 'body', (-7.9, y + 2.0, 4.4), (0.14, 0, 0.1), [(5.0, (0.0, 0, 0)), (5.0, (-0.1, 0, -0.06)), (5.0, (-0.1, 0, -0.06))], 'ribbon', width=2.0))


def plume_big():
    """A captain's great tassel: a fuller burst of horsehair, a longer fall behind."""
    return [part('plume_big', 'helm', (0, -7.4, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3.0, 1.0, 3.0, 'plume_top'),
                                                                (-3.0, 0.8, -3.0, 6.0, 2.8, 6.0, 'plume')]),
            part('plume_big_tail', 'plume_big', (0, 2.0, 2.2), (0.65, 0, 0), [(-2.4, 0, -1.2, 4.8, 8.5, 2.4, 'plume_long')])]


def soldier_parts():
    parts = (legs() + waist() + body() + arms() + head() + helm(plume=True, wings=True) + halberd() + sword()
             + cape() + ribbon() + plume_big()
             + [part('right_moustache', 'head', (-0.3, -2.25, -3.45), (0, 0.15, -0.35), [(-2.2, -0.35, -0.3, 2.2, 0.6, 0.6, 'moustache')])])
    parts = mirrored(parts)
    return pose(parts, {'left_arm': LARM, 'left_forearm': LFOREARM})


SOLDIER = soldier_parts()

SOLDIER_COLOURS = {
    False: colours(silk=(56, 100, 172), silk_deep=(22, 40, 92), trim=(196, 44, 36), plume=(214, 38, 32), plume_deep=(112, 14, 16),
                   lace=(184, 38, 34), ribbon=(244, 236, 214)),
    True: colours(silk=(168, 28, 32), silk_deep=(84, 10, 16), trim=(236, 190, 76), plume=(226, 36, 30), plume_deep=(118, 12, 14),
                  lace=(120, 14, 18), cloak=(156, 22, 28), cloak_deep=(72, 8, 14), ribbon=(246, 236, 210), moustache=True,
                  captain=True, jewel=(80, 200, 220)),
}


def crescent_mask(w, h):
    """A crescent blade's shape on a w x h plate, its back to the shaft (s 1), its edge sweeping out (s 0)."""
    def m(s, t):
        x, y = s * w, t * h
        outer = math.hypot(x - (w + 0.4), y - h / 2) <= w + 0.4
        inner = math.hypot(x - (w + 2.6), y - h / 2) <= w + 1.15
        return outer and not inner
    return m


def paint_arms(p, style, f, dims, c):
    """The weapons and gear the host carry: the halberd, the sword, the cloak."""
    if style == 'shaft':                                   # red lacquer, banded in gold every so often
        p.shaded(f, LACQUER_RED)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 16):
                for i in range(fw):
                    p.px((fx + i, fy + j), GOLD)
                    p.px((fx + i, fy + j + 1), GOLD_DEEP)
        return True
    if style in ('collar', 'socket', 'butt', 'chape', 'guard', 'pommel', 'hookbar_up'):
        gilt(p, f, 1.05)
        if style == 'butt':
            pointed(p, f, down=True, keep=0.3, sharp=0.8)
        return True
    if style == 'hookbar':
        steel(p, f)
        return True
    if style == 'spearhead':                               # a steel point, a ridge down its middle, cut to a tip
        steel(p, f)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                p.px((fx + fw // 2, fy + j), (250, 252, 255))
            point(p, f[s], keep=1.0, down=False, sharp=0.32)
        bare(p, f, ('top',))
        return True
    if style == 'crescent':                                # the crescent blade (月牙), polished, its edge bright
        bare(p, f, SIDES + ('top', 'bottom'))
        w, h = dims[0], dims[1]
        m = crescent_mask(w, h)

        def col(s, t):
            edge = not m(min(1, s + 0.12), t) or not m(max(0, s - 0.1), t)
            return (250, 252, 255) if s < 0.3 and edge else mixc(STEEL, STEEL_DARK, s * 0.8)
        plane(p, f, 'z', m, col)
        return True
    if style in ('tassel', 'tassel_tip'):
        for s, r in f.items():
            horsehair(p, r, c['plume'], c['plume_deep'], LIGHTS[s], cut=0.35 if style == 'tassel_tip' and s in SIDES else 0)
        return True
    if style == 'scabbard':                                # black lacquer, gold mounts
        p.shaded(f, (40, 30, 34))
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for j in (1, 2, fh // 2, fh // 2 + 1):
                for i in range(fw):
                    p.px((fx + i, fy + j), GOLD)
        return True
    if style == 'hilt':
        p.shaded(f, c['plume_deep'])
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(0, fh, 2):
                for i in range(fw):
                    p.px((fx + i, fy + j), k(c['plume'], 0.9))
        return True
    if style in ('cape', 'cape_lower'):                    # a cloak of silk: gold-bordered, clouds worked over it
        for s, r in f.items():
            fabric(p, r, c['cloak'], c['cloak_deep'], LIGHTS[s], 7, 0.45 if style == 'cape_lower' else 0.25)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            rim(p, f[s], GOLD, 'lr' + ('' if style == 'cape_lower' else 't'), 2)
            cloud_scrolls(p, (fx + 3, fy + 2, fw - 6, fh - 8), k(GOLD, 0.9), every=8, r=2, share=1.0)
            if style == 'cape_lower':
                key_fret(p, (fx, fy + fh - 5, fw, 5))
        return True
    if style == 'moustache':
        lock(p, f, along_x=True, tone=BROWN_BLACK)
        return True
    return False


def soldier_painter(c):
    def paint_box(p, style, f, dims):
        if paint_trooper(p, style, f, dims, c) or paint_arms(p, style, f, dims, c):
            return
        p.shaded(f, (255, 0, 255))                         # unpainted: shows up loud
    return paint_box


def save(name, parts, painters, tag, size):
    slots = None
    for skin, painter in painters.items():
        p = HostPainter(sum(map(ord, skin)))
        slots = paint(parts, p, painter)
        p.save_as(skin)
    emit(parts, slots, JAVA, tag)
    from lion_camel import extent
    print(name, len(parts), 'parts,', len(slots), 'boxes; drawn at', size, {kk: round(v, 2) for kk, v in extent(parts, size).items()})


def build_soldier():
    save('soldier', SOLDIER, {'heaven_soldier': soldier_painter(SOLDIER_COLOURS[False]),
                              'heaven_soldier_captain': soldier_painter(SOLDIER_COLOURS[True])}, 'soldier', TROOPER_SIZE)


def build_archer():
    from heaven_host_ranks import ARCHER, ARCHER_COLOURS, archer_painter
    save('archer', ARCHER, {'heaven_archer': archer_painter(ARCHER_COLOURS[False]),
                            'heaven_archer_captain': archer_painter(ARCHER_COLOURS[True])}, 'archer', TROOPER_SIZE)


def build_drummer():
    from heaven_host_ranks import DRUMMER, DRUMMER_COLOURS, drummer_painter
    save('drummer', DRUMMER, {'heaven_drummer': drummer_painter(DRUMMER_COLOURS[False]),
                              'heaven_drummer_captain': drummer_painter(DRUMMER_COLOURS[True])}, 'drummer', TROOPER_SIZE)


def build_mansion():
    """Each quarter's armour painted once; each mansion's beast stamped on a copy of it."""
    from heaven_host_mansions import MANSION, MANSIONS, quarter_colours, mansion_painter, stamp
    slots = None
    for q in range(4):
        c = quarter_colours(q)
        base = HostPainter(91 + q)
        slots = paint(MANSION, base, mansion_painter(c))
        for name in MANSIONS[q * 7:(q + 1) * 7]:
            p = HostPainter(7)
            p.img, p.glow = base.img.copy(), base.glow.copy()
            stamp(p, name, c)
            p.save_as('heaven_mansion_' + name)
    emit(MANSION, slots, JAVA, 'mansion')
    from lion_camel import extent
    print('mansion', len(MANSION), 'parts,', len(slots), 'boxes; drawn at', TROOPER_SIZE,
          {kk: round(v, 2) for kk, v in extent(MANSION, TROOPER_SIZE).items()})


def build_cavalry():
    from heaven_host_cavalry import CAVALRY, CAVALRY_COLOURS, cavalry_painter, CAVALRY_SIZE
    save('cavalry', CAVALRY, {'heaven_cavalry': cavalry_painter(CAVALRY_COLOURS[False]),
                              'heaven_cavalry_captain': cavalry_painter(CAVALRY_COLOURS[True])}, 'cavalry', CAVALRY_SIZE)


BUILDS = {'soldier': build_soldier, 'cavalry': build_cavalry, 'archer': build_archer, 'drummer': build_drummer, 'mansion': build_mansion}

if __name__ == '__main__':
    for name in (sys.argv[1:] or BUILDS):
        BUILDS[name]()
