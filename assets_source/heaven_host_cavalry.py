"""The cloud cavalry (云骑) (heaven_host.py builds it): the rider only. He rides a plain Minecraft
horse (HeavenHostRenderers draws vanilla's horse model under him, a white coat in gold horse armour; his
captain's a black one), so this model and its skins are the man alone, his seat at the model's origin.

The rider: the heavenly soldier's armour and helmet (gilt mingguang armour over azure silk, a red
tassel), seated astride, the left fist on the reins, a long spear (枪) in his right with a red tassel
under its head and a swallow-tailed pennant, held slanting forward; winding up his charge, he
couches it level, leaning in, and the horse rears. His captain in crimson and gold, cloaked,
phoenix-winged."""
from heaven_host_kit import *
from heaven_host import cape, plume_big, paint_arms
from heaven_host_ranks import strip, restyle

RIDER_SIZE = 0.86                                           # HeavenHostRenderers.RIDER_SIZE: a trooper's

R_ARM, R_FOREARM = (-0.15, 0, 0.12), (-0.75, -0.25, 0)       # the spear arm
L_ARM, L_FOREARM = (-0.35, 0, -0.08), (-0.9, 0.2, 0)        # the rein hand before him


def rider():
    """The soldier's frame seated: his legs astride the saddle, the spear in his right fist, the reins in his left."""
    parts = waist(skirt=6.0, apron=6.5) + legs() + body() + arms() + head() + helm(plume=True, wings=True) + cape(length=8.0, lower=6.5) + plume_big()
    parts = without(parts, ['robe_back'])
    out = [part('rider', 'root', (0, 0, 0), (0, 0, 0), [])]
    for n, par, piv, rot, boxes, m in parts:
        if n == 'waist':
            par, piv = 'rider', (0, -1.6, 0)
        # astride the horse's broad back as a rider sits it: from the hip's edge the thigh forward, down and
        # out over the top of the barrel, as vanilla's rider's legs reach forward; the knee just outside the
        # flank; the shin hanging plumb down the flank, the heel a little back (its turn undoes the thigh's,
        # so it hangs straight whatever the thigh does). The horse is vanilla's, drawn HORSE_SIZE / RIDER_SIZE
        # larger than him: its barrel's flank lies 6.5 of his pixels out from his middle, its back 0.5 below
        # his seat. His legs a little slimmer than a foot soldier's, so the shins clear the flank
        if n == 'right_leg':
            par, piv, rot = 'waist', (-4.4, -0.3, 0), (-1.121, 0.732, 0)
            boxes = [(x * 0.85, y, z, w * 0.85, h, d, st) for (x, y, z, w, h, d, st) in boxes]
        if n == 'right_shin':
            rot = (1.346, -0.295, -0.68)
            boxes = [(x * 0.8, y, z, w * 0.8, h, d, st) for (x, y, z, w, h, d, st) in boxes]
        out.append((n, par, piv, rot, boxes, m))
    out.append(part('right_moustache', 'head', (-0.3, -2.25, -3.45), (0, 0.15, -0.35), [(-2.2, -0.35, -0.3, 2.2, 0.6, 0.6, 'moustache')]))
    # the spear: a long red shaft, a steel head over a red tassel, a pennant below it
    out += [
        part('lance', 'right_hand', (0, 1.2, 0), aim([R_ARM, R_FOREARM, (0, 0, 0)], (0.7, 0, 0)), [
            (-0.45, -34, -0.45, 0.9, 44, 0.9, 'shaft'),
            (-0.8, -35.4, -0.8, 1.6, 1.6, 1.6, 'collar'),
            (-0.95, -42.6, -0.3, 1.9, 7.4, 0.6, 'spearhead'),
            (-0.5, 9.6, -0.5, 1.0, 2.4, 1.0, 'butt'),
        ]),
        part('lance_tassel', 'lance', (0, -34.2, 0), (0, 0, 0), [(-1.1, 0, -1.1, 2.2, 2.6, 2.2, 'tassel'),
                                                                 (-0.8, 2.3, -0.8, 1.6, 1.6, 1.6, 'tassel_tip')]),
        part('pennant', 'lance', (0, -31.5, 0.4), (0, 0, 0), [(-0.1, 0, 0, 0.2, 5.0, 8.5, 'pennant')]),
    ]
    out = mirrored(out)
    return pose(out, {'right_arm': R_ARM, 'right_forearm': R_FOREARM, 'left_arm': L_ARM, 'left_forearm': L_FOREARM})


def cavalry_parts():
    return rider()


CAVALRY = cavalry_parts()

CAVALRY_COLOURS = {
    False: colours(silk=(56, 100, 172), silk_deep=(22, 40, 92), trim=(196, 44, 36), plume=(214, 38, 32), plume_deep=(112, 14, 16),
                   lace=(184, 38, 34)),
    True: colours(silk=(168, 28, 32), silk_deep=(84, 10, 16), trim=(236, 190, 76), plume=(226, 36, 30), plume_deep=(118, 12, 14),
                  lace=(120, 14, 18), cloak=(156, 22, 28), cloak_deep=(72, 8, 14), moustache=True, captain=True, jewel=(80, 200, 220)),
}


def paint_pennant(p, style, f, c):
    """A swallow-tailed pennant: the field, a gold border, a gold cloud."""
    if style != 'pennant':
        return False
    bare(p, f, ('top', 'bottom', 'front', 'back'))
    for s in ('right', 'left'):
        fx, fy, fw, fh = f[s]
        for i, j, xy in each(f[s]):
            s_ = (i + 0.5) / fw if s == 'left' else 1 - (i + 0.5) / fw     # 0 at the shaft .. 1 at the tails
            t = (j + 0.5) / fh
            if s_ > 0.62 and abs(t - 0.5) < (s_ - 0.62) * 1.3:
                p.clear(xy)
                continue
            edge = t < 0.12 or t > 0.88 or s_ < 0.06
            p.px(xy, noisy(p, GOLD if edge else c['plume'], 3))
        cloud(p, fx + fw // 2 - 3, fy + fh // 2, GOLD_LIGHT, r=1, rect=f[s])
    return True


def cavalry_painter(c):
    def paint_box(p, style, f, dims):
        if paint_pennant(p, style, f, c) or paint_trooper(p, style, f, dims, c) or paint_arms(p, style, f, dims, c):
            return
        p.shaded(f, (255, 0, 255))
    return paint_box
