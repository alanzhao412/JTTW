"""The Victorious Fighting Buddha (斗战胜佛): Sun Wukong at level 100, as the temples sculpt him once the
journey is done. While he is ascended the player's own body is not drawn (FightingBuddhaLayer hides it,
and his armour and cape with it) and this is drawn in its place, a little larger than a man, each piece
riding a copy of the player's head, body, arms and legs, posed as the player is each frame:

- the Monkey King's head: a skull of golden fur, longer at the crown and swept back; a heart-shaped
  face of bare peach skin under a heavy golden brow, ringed in red as the operas paint him; eyes of
  red-gold fire (the Fiery Golden Eyes, glowing); a short muzzle with its nostrils and a thin smile,
  round ears, ruffs of fur flaring from his cheeks;
- the golden fillet (紧箍), no longer a binding but a Buddha's jewelled crown: a band of chased gold,
  five lotus-petal plaques rising from it, the middle one tallest, each set with a jewel, and long
  ribbons of red silk edged in gold flaring from it behind his ears;
- a Buddhist robe (僧衣) of deep crimson, its collar crossed white over gold at his throat, its wide
  sleeves hemmed in gold, falling past his knees to cloud shoes with upturned toes;
- over it the kasaya (袈裟), the patched robe of the field-paddy pattern (田相): strips of saffron, ochre
  and old gold sewn between gold-thread seams, worn over the left shoulder and leaving the right bare
  (the robe beneath showing), its rolled edge running across his breast and back in folds, the gold
  ring and hook that fasten it at the left breast, a fold of it over the left shoulder and a long
  panel hanging from the left arm, a gold-thread cord at the waist knotted at the front with tassels,
  and below the cord its apron and panels hanging past the knees, front, back and sides;
- a strand of prayer beads (念珠) of dark bodhi wood round his neck, a jade guru bead and a red tassel
  at the breast, a wrist mala on each arm; a gold armlet on the bare right arm;
- the Ruyi Jingu Bang in his right hand, while he holds it: a staff of red lacquer banded in gold, a
  gold cap at each end chased with clouds;
- behind his head the halo (头光): a disc of light, an inner ring and an outer ring of beaded gold, and
  round it two layers of golden flame; behind his body the mandorla (身光), an almond of light pointed
  at the top, edged in gold and fringed with flame. The light is drawn full-bright, glowing.

As king_yama.py: one list of parts gives the model, written into FightingBuddhaLayer.layer() between
its GENERATED markers, and the skins (fighting_buddha.png, and the glowing fighting_buddha_glow.png),
painted to the same box-UV layout at twice the model's resolution, each face where the game looks for
it (king_yama.paint).

Run from the repository root: python assets_source/fighting_buddha.py"""
import math

import beastgen as bg
from beastgen import Painter, emit
from fire_court import part, mirrored
from king_yama import paint as exact_paint

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/FightingBuddhaLayer.java'

# ── Colours ──────────────────────────────────────────────────────────────────
FUR = (212, 156, 56)
FUR_LIGHT = (248, 208, 112)
FUR_DARK = (140, 88, 28)
FUR_DEEP = (96, 58, 20)
SKIN = (226, 172, 128)
SKIN_SHADE = (184, 126, 90)
SKIN_LIGHT = (242, 198, 156)
OPERA_RED = (204, 44, 32)
MOUTH = (120, 34, 30)
CRIMSON = (150, 26, 30)
CRIMSON_DEEP = (96, 14, 20)
CRIMSON_LIGHT = (190, 52, 48)
SAFFRON = (224, 146, 40)
OCHRE = (196, 118, 34)
OLD_GOLD = (204, 164, 70)
BROWN_GOLD = (166, 104, 36)
THREAD = (248, 214, 110)
SEAM = (110, 40, 20)
GOLD = (236, 188, 68)
GOLD_LIGHT = (255, 232, 150)
GOLD_DEEP = (150, 100, 26)
WHITE = (240, 232, 214)
JEWEL_RED = (210, 28, 46)
JEWEL_BLUE = (40, 110, 210)
JADE = (90, 176, 132)
BEAD = (86, 48, 30)
BEAD_LIGHT = (150, 98, 60)
SHOE = (40, 30, 34)
LIGHT_HOT = (255, 236, 160)
LIGHT = (255, 200, 90)
LIGHT_DEEP = (236, 136, 34)


class BuddhaPainter(Painter):
    """A Painter that can also clear a pixel (cut a face's shape) and light the glowing skin alone."""

    def clear(self, xy):
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            self.img.putpixel((x, y), (0, 0, 0, 0))
            self.glow.putpixel((x, y), (0, 0, 0, 0))

    def light(self, xy, c, alpha=255):
        """On the glowing skin only: light, with nothing on the skin beneath (so it shows as light alone)."""
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            self.img.putpixel((x, y), (0, 0, 0, 0))
            self.glow.putpixel((x, y), tuple(max(0, min(255, int(v))) for v in c[:3]) + (alpha,))


def k(c, f):
    return [v * f for v in c[:3]]


def mixc(a, b, t):
    t = max(0.0, min(1.0, t))
    return [x + (y - x) * t for x, y in zip(a, b)]


SIDES = (('front', 1.0), ('back', 0.84), ('right', 0.92), ('left', 0.92), ('top', 1.08), ('bottom', 0.68))


# ── The model ────────────────────────────────────────────────────────────────

def crown():
    """The fillet become a crown: five lotus-petal plaques round the front of the band, the middle tallest."""
    out = []
    for n, deg in enumerate((-62, -31, 0, 31, 62)):
        a = math.radians(deg)
        r = 4.95
        main = deg == 0
        out.append(part('crown_leaf_%d' % n, 'head', (r * math.sin(a), -8.0, -r * math.cos(a)), (0.16, -a, 0),
                        [(-1.6, -4.4, -0.3, 3.2, 4.4, 0.6, 'crown_leaf_main')] if main else
                        [(-1.25, -3.3, -0.3, 2.5, 3.3, 0.6, 'crown_leaf')]))
        out.append(part('crown_jewel_%d' % n, 'crown_leaf_%d' % n, (0, -1.6 if not main else -2.0, -0.4), (0, 0, 0.785),
                        [(-0.5, -0.5, -0.25, 1, 1, 0.5, 'jewel_red' if n % 2 == 0 else 'jewel_blue')]))
    return out


def beads():
    """The strand of prayer beads hanging round his neck to his breast, a jade guru bead and a tassel at its foot."""
    out = []
    n = 13
    for i in range(n):
        t = -1 + 2 * i / (n - 1)
        x, y = 3.3 * t, 0.2 + 6.4 * (1 - t * t)
        out.append(part('bead_%d' % i, 'body', (x, y, -3.35), (0, 0, 0), [(-0.55, -0.55, -0.55, 1.1, 1.1, 1.1, 'bead')]))
    out.append(part('guru_bead', 'body', (0, 7.4, -3.4), (0, 0, 0), [(-0.8, -0.8, -0.8, 1.6, 1.6, 1.6, 'guru_bead'),
                                                                      (-0.35, 0.8, -0.35, 0.7, 2.8, 0.7, 'tassel')]))
    return out


def ring(prefix, parent, n, r, box, style, z=0.0, start=0.0, until=None):
    """Segments round a circle of radius r (about the parent's pivot), each turned along it."""
    out = []
    for i in range(n):
        a = start + i * 2 * math.pi / n
        if until is not None and abs(math.atan2(math.sin(a), math.cos(a))) > until:
            continue
        out.append(part('%s_%d' % (prefix, i), parent, (r * math.sin(a), -r * math.cos(a), z), (0, 0, a), [box + (style,)]))
    return out


def halo():
    """The halo behind his head: a disc of light, an inner ring and a beaded outer ring of gold, and two
    layers of flame round it, the inner layer long and gold, the outer shorter and deeper, between them."""
    out = [part('halo', 'body', (0, -4.2, 6.4), (0, 0, 0), [(-9, -9, 0.3, 18, 18, 0.2, 'halo_disc')])]
    out += ring('halo_inner', 'halo', 20, 5.3, (-0.95, -0.45, -0.1, 1.9, 0.9, 0.5), 'gold_ring')
    out += ring('halo_outer', 'halo', 28, 8.6, (-1.05, -0.6, -0.2, 2.1, 1.2, 0.7), 'gold_beaded')
    for i in range(14):
        a = i * 2 * math.pi / 14
        r = 9.2
        tall = 4.6 if i % 2 == 0 else 3.4
        # each tongue a cross of two flat flames, so it shows from the side as well as the front
        out.append(part('halo_flame_%d' % i, 'halo', (r * math.sin(a), -r * math.cos(a), 0.2), (0, 0, a),
                        [(-1.6, -tall, -0.1, 3.2, tall, 0.2, 'flame'),
                         (-0.1, -tall * 0.85, -1.2, 0.2, tall * 0.85, 2.4, 'flame_x')]))
        b = a + math.pi / 14
        out.append(part('halo_flame_back_%d' % i, 'halo', (r * math.sin(b), -r * math.cos(b), 0.55), (0, 0, b),
                        [(-1.3, -2.8, -0.1, 2.6, 2.8, 0.2, 'flame_deep')]))
    return out


MANDORLA_RX, MANDORLA_RY = 12.5, 17.0


def almond(t):
    """A point on the mandorla's edge at angle t (0 at the top): an ellipse drawn to a point at its top."""
    x = MANDORLA_RX * math.sin(t)
    y = -MANDORLA_RY * math.cos(t)
    if y < 0:
        x *= 1 - 0.45 * (-y / MANDORLA_RY) ** 2
    return x, y


def mandorla():
    """The mandorla behind his body: an almond of light pointed at the top, edged in gold, fringed with flame."""
    out = [part('mandorla', 'body', (0, 7.5, 7.6), (0, 0, 0), [(-13, -18, 0.4, 26, 36, 0.2, 'mandorla_disc')])]
    n = 40
    pts = [almond(-math.pi * 0.86 + 2 * math.pi * 0.86 * i / (n - 1)) for i in range(n)]
    for i in range(n - 1):
        (x0, y0), (x1, y1) = pts[i], pts[i + 1]
        mx, my = (x0 + x1) / 2, (y0 + y1) / 2
        seg = math.hypot(x1 - x0, y1 - y0)
        out.append(part('mandorla_rim_%d' % i, 'mandorla', (mx, my, 0.1), (0, 0, math.atan2(y1 - y0, x1 - x0)),
                        [(-seg / 2 - 0.15, -0.55, -0.25, seg + 0.3, 1.1, 0.6, 'gold_beaded')]))
    for i in range(1, n - 1, 2):
        (x0, y0), (x1, y1) = pts[i - 1], pts[i + 1]
        x, y = pts[i]
        # outward: across the edge, away from the middle
        a = math.atan2(y1 - y0, x1 - x0) - math.pi / 2
        tall = 4.2 if (i // 2) % 2 == 0 else 3.0
        out.append(part('mandorla_flame_%d' % i, 'mandorla', (x, y, 0.3), (0, 0, a + math.pi / 2),
                        [(-1.7, -tall, -0.1, 3.4, tall, 0.2, 'flame' if tall > 4 else 'flame_deep')]))
    # the top drawn up into a single great tongue of flame
    out.append(part('mandorla_crest', 'mandorla', (0, -MANDORLA_RY + 0.3, 0.3), (0, 0, 0),
                    [(-2.2, -6.5, -0.1, 4.4, 6.5, 0.2, 'flame')]))
    return out


ARM_DRAPE = (0, 0, -0.1)

BUDDHA = mirrored([
    # the player's own parts, at their pivots (the layer copies the player's pose onto them each frame)
    part('head', 'root', (0, 0, 0), (0, 0, 0), []),
    part('body', 'root', (0, 0, 0), (0, 0, 0), []),
    part('right_arm', 'root', (-5, 2, 0), (0, 0, 0), []),
    part('right_leg', 'root', (-1.9, 12, 0), (0, 0, 0), []),

    # ── the Monkey King's head ──
    part('skull', 'head', (0, 0, 0), (0, 0, 0), [
        (-4.4, -8.7, -4.2, 8.8, 8.8, 8.6, 'head'),
        (-2.3, -4.0, -6.0, 4.6, 2.9, 2.0, 'muzzle'),
        (-1.8, -1.15, -5.4, 3.6, 1.2, 1.4, 'jaw'),
        (-0.8, -4.7, -6.35, 1.6, 1.0, 0.6, 'nose'),
        (-3.9, -6.55, -4.95, 7.8, 1.15, 1.1, 'brow'),
        (-3.2, -9.9, -2.4, 6.4, 1.4, 6.0, 'fur_crown'),
        (-4.0, -8.6, 3.9, 8.0, 7.0, 1.4, 'fur_mane'),
    ]),
    part('right_eye', 'head', (-1.85, -5.1, -4.35), (0, 0.08, 0), [(-0.8, -0.45, -0.2, 1.6, 0.9, 0.35, 'eye')]),
    part('right_ear', 'head', (-4.4, -4.9, -0.4), (0, -0.35, 0), [(-1.1, -1.5, -1.2, 1.1, 3.0, 2.4, 'ear')]),
    part('right_ruff_0', 'head', (-3.9, -3.6, -3.2), (0.1, 0.2, 0.55), [(-0.7, 0, -0.9, 1.4, 3.6, 1.8, 'ruff')]),
    part('right_ruff_1', 'head', (-4.2, -2.6, -1.8), (0.2, 0.3, 0.85), [(-0.7, 0, -0.9, 1.4, 3.4, 1.8, 'ruff')]),
    part('right_ruff_2', 'head', (-4.0, -1.4, -0.4), (0.35, 0.2, 1.1), [(-0.6, 0, -0.8, 1.2, 3.0, 1.6, 'ruff_small')]),
    # the crown's band
    part('circlet', 'head', (0, 0, 0), (0, 0, 0), [(-4.95, -8.4, -4.75, 9.9, 1.5, 9.6, 'circlet')]),
    # its ribbons, flaring back from behind the ears, each in two lengths
    part('right_ribbon', 'head', (-4.9, -7.6, 2.2), (0.45, 0.25, 0.5), [(-0.75, 0, -0.15, 1.5, 6.5, 0.3, 'ribbon')]),
    part('right_ribbon_end', 'right_ribbon', (0, 6.3, 0), (0.25, 0, 0.4), [(-0.7, 0, -0.15, 1.4, 6.0, 0.3, 'ribbon_end')]),
    part('right_ribbon_b', 'head', (-4.7, -7.2, 3.4), (0.75, 0.1, 0.22), [(-0.65, 0, -0.15, 1.3, 7.0, 0.3, 'ribbon')]),
    part('right_ribbon_b_end', 'right_ribbon_b', (0, 6.8, 0), (0.3, 0, 0.25), [(-0.6, 0, -0.15, 1.2, 5.0, 0.3, 'ribbon_end')]),

    # ── the robe and the kasaya over it ──
    part('torso', 'body', (0, 0, 0), (0, 0, 0), [
        (-4.85, -0.15, -2.95, 9.7, 11.0, 5.9, 'kasaya_torso'),
        (-2.3, -0.9, -2.3, 4.6, 1.2, 4.6, 'fur_neck'),
        (-5.1, 9.6, -3.2, 10.2, 1.4, 6.4, 'cord'),
        (-1.0, 9.3, -3.8, 2.0, 2.0, 0.9, 'knot'),
    ]),
    # the kasaya's rolled edge, from the left shoulder across the breast to the right hip, and so behind
    part('edge_front', 'body', (0.2, 5.0, -3.05), (0, 0, -0.66), [(-0.95, -8.8, -0.5, 1.9, 17.0, 1.0, 'edge_roll')]),
    part('edge_back', 'body', (0.2, 5.0, 3.05), (0, 0, -0.66), [(-0.95, -8.8, -0.5, 1.9, 17.0, 1.0, 'edge_roll')]),
    # folds falling from it across the breast
    part('fold_0', 'body', (1.9, 7.2, -3.0), (0, 0, -0.5), [(-0.4, -4.4, -0.35, 0.8, 8.0, 0.7, 'fold')]),
    part('fold_1', 'body', (3.4, 8.6, -3.0), (0, 0, -0.38), [(-0.4, -3.2, -0.35, 0.8, 5.6, 0.7, 'fold')]),
    part('fold_back', 'body', (2.4, 7.8, 3.0), (0, 0, -0.45), [(-0.4, -4.2, -0.35, 0.8, 7.0, 0.7, 'fold')]),
    # the gold ring and hook that fasten it at the left breast
    part('clasp', 'body', (2.7, 2.4, -3.35), (0, 0, 0), [
        (-1.1, -1.1, -0.3, 2.2, 0.5, 0.6, 'gold'), (-1.1, 0.6, -0.3, 2.2, 0.5, 0.6, 'gold'),
        (-1.1, -0.6, -0.3, 0.5, 1.2, 0.6, 'gold'), (0.6, -0.6, -0.3, 0.5, 1.2, 0.6, 'gold'),
        (-0.25, 1.0, -0.25, 0.5, 1.4, 0.5, 'gold')]),
    # the tassels of the cord's knot
    part('tassel_right', 'body', (-0.5, 10.9, -3.6), (-0.05, 0, 0.06), [(-0.3, 0, -0.3, 0.6, 5.0, 0.6, 'tassel')]),
    part('tassel_left', 'body', (0.55, 10.9, -3.6), (-0.05, 0, -0.06), [(-0.3, 0, -0.3, 0.6, 4.2, 0.6, 'tassel')]),
    # below the cord, the kasaya hangs past the knees: an apron before, a panel behind, panels at the sides
    part('apron', 'body', (0, 10.9, -3.1), (-0.07, 0, 0), [(-3.6, 0, -0.45, 7.2, 10.8, 0.9, 'apron')]),
    part('back_panel', 'body', (0, 10.9, 3.1), (0.09, 0, 0), [(-4.6, 0, -0.45, 9.2, 11.4, 0.9, 'back_panel')]),
    part('right_side_panel', 'body', (-4.9, 10.9, 0), (0, 0, 0.1), [(-0.45, 0, -2.8, 0.9, 10.2, 5.6, 'side_panel')]),

    # ── the arms ──
    # the right: the shoulder bare of the kasaya, the crimson robe's sleeve, a gold armlet; the left: the
    # kasaya over the shoulder and a long panel of it hanging from the arm
    part('right_sleeve_upper', 'right_arm', (0, 0, 0), (0, 0, 0), [(-3.6, -2.5, -2.55, 5.1, 6.6, 5.1, 'robe_sleeve')]),
    part('armlet', 'right_arm', (0, 0, 0), (0, 0, 0), [(-3.95, 0.4, -2.85, 5.8, 1.0, 5.7, 'gold_band')]),
    part('right_sleeve_wide', 'right_arm', (-1, 3.8, 0), (0.08, 0, 0.05), [
        (-2.9, 0, -3.3, 5.8, 6.0, 6.6, 'wide_sleeve'), (-3.05, 5.4, -3.45, 6.1, 0.9, 6.9, 'sleeve_hem')]),
    part('right_hand', 'right_arm', (-1, 8.0, 0), (0, 0, 0), [(-1.8, 0, -1.9, 3.6, 2.8, 3.8, 'hand'),
                                                            (-2.05, -0.5, -2.15, 4.1, 0.8, 4.3, 'mala')]),
    part('left_kasaya_sleeve', 'left_arm', (0, 0, 0), (0, 0, 0), [(-1.55, -2.7, -2.85, 5.3, 7.2, 5.7, 'kasaya_sleeve')]),
    part('left_drape', 'left_arm', (3.6, -1.8, 0), ARM_DRAPE, [(0, 0, -2.9, 0.7, 13.5, 5.8, 'drape')]),
    # the Ruyi Jingu Bang, while he holds it (the layer shows it then): red lacquer banded in gold, gold caps
    # held upright through the fist, a little forward, as a monk holds his staff
    part('staff', 'right_hand', (0, 1.4, -0.2), (0.22, 0, 0.05), [
        (-0.65, -23, -0.65, 1.3, 36, 1.3, 'staff_shaft'),
        (-1.0, -25.2, -1.0, 2.0, 2.6, 2.0, 'staff_cap'),
        (-1.0, 12.6, -1.0, 2.0, 2.6, 2.0, 'staff_cap'),
        (-0.85, -21.4, -0.85, 1.7, 0.8, 1.7, 'staff_band'),
        (-0.85, 10.6, -0.85, 1.7, 0.8, 1.7, 'staff_band'),
        (-0.8, -1.4, -0.8, 1.6, 0.5, 1.6, 'staff_band'),
        (-0.8, 3.0, -0.8, 1.6, 0.5, 1.6, 'staff_band')]),

    part('staff_rest', 'body', (-5.0, -0.5, 0.6), (-0.785, 0, -0.21), [
        (-0.65, -23, -0.65, 1.3, 36, 1.3, 'staff_shaft'),
        (-1.0, -25.2, -1.0, 2.0, 2.6, 2.0, 'staff_cap'),
        (-1.0, 12.6, -1.0, 2.0, 2.6, 2.0, 'staff_cap'),
        (-0.85, -21.4, -0.85, 1.7, 0.8, 1.7, 'staff_band'),
        (-0.85, 10.6, -0.85, 1.7, 0.8, 1.7, 'staff_band')]),

    # ── the legs: the robe falling past the knees, flared and hemmed, to cloud shoes ──
    part('right_robe_leg', 'right_leg', (0, 0, 0), (0, 0, 0), [
        (-2.65, -0.2, -2.65, 5.3, 6.4, 5.3, 'robe_leg'),
        (-3.1, 5.8, -3.1, 6.2, 4.7, 6.2, 'robe_hem'),
        (-2.05, 10.3, -3.2, 4.1, 1.7, 5.0, 'shoe'),
        (-1.2, 9.6, -3.9, 2.4, 1.1, 1.1, 'shoe_toe')]),
] + crown() + beads() + halo() + mandorla())


# ── The painting ─────────────────────────────────────────────────────────────

def fur(p, rect, light=1.0, down=True, base=FUR):
    """Golden fur: strands running down (or across) the face, each its own shade, lit at the tips."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        tone = p.rnd.uniform(-0.12, 0.12)
        phase = p.rnd.randint(0, 5)
        for j in range(fh):
            t = j / max(1, fh - 1)
            stripe = ((j + phase) // 3 + i) % 4
            c = FUR_LIGHT if stripe == 0 else FUR_DARK if stripe == 3 else base
            f = light * (1.06 - 0.2 * t + tone)
            p.px((fx + i, fy + j), [v * f + p.rnd.randint(-4, 4) for v in c])


def heart_face(p, rect):
    """The front of his head: golden fur, and over it the heart-shaped face of bare skin, ringed in red,
    its two lobes over the eyes, its point at the chin; dark sockets lined in red."""
    fx, fy, fw, fh = rect
    fur(p, rect, 1.0)
    cx = fw / 2
    r = fw * 0.26
    for j in range(fh):
        for i in range(fw):
            x, y = i + 0.5, j + 0.5
            lobe = min(math.hypot(x - (cx - r * 0.85), y - fh * 0.5), math.hypot(x - (cx + r * 0.85), y - fh * 0.5)) - r
            # below the lobes, drawn in to the point of the chin
            if y > fh * 0.5:
                half = (cx - 0.6) * (1 - (y - fh * 0.5) / (fh * 0.52))
                body = abs(x - cx) - half
                d = min(lobe, body) if y < fh * 0.62 else body
            else:
                d = lobe
            if y < fh * 0.3:
                continue
            if d < -1.0:
                t = (y / fh)
                c = mixc(SKIN_LIGHT, SKIN, t)
                p.px((fx + i, fy + j), [v + p.rnd.randint(-3, 3) for v in c])
            elif d < 0.6:
                p.px((fx + i, fy + j), OPERA_RED)
    # the sockets: dark, lined in red, under the brow
    for side in (-1, 1):
        ex = int(cx + side * r * 0.95)
        for i in range(ex - 3, ex + 3):
            p.px((fx + i, fy + int(fh * 0.65)), OPERA_RED)


def patchwork(p, rect, light=1.0, cell=(7, 10), border=True, fold=True, gap=None):
    """The field-paddy pattern: strips of saffron, ochre and old gold sewn between seams of gold thread
    (a dotted gold line on a dark seam), a fall of folds across it, edged with a border of gold thread."""
    fx, fy, fw, fh = rect
    cw, ch = cell
    tones = (SAFFRON, OCHRE, OLD_GOLD, BROWN_GOLD, SAFFRON)
    for j in range(fh):
        for i in range(fw):
            if gap and gap(i, j, fw, fh):
                continue
            col = i // cw
            row = (j + (ch // 2 if col % 2 else 0)) // ch
            tone = tones[(col * 3 + row * 2) % len(tones)]
            # folds: soft light and shadow running down the cloth
            f = light * (1.0 + (0.1 * math.sin(i * 0.9 + j * 0.05) if fold else 0) - 0.12 * j / max(1, fh))
            c = k(tone, f)
            si, sj = i % cw, (j + (ch // 2 if col % 2 else 0)) % ch
            if si == 0 or sj == 0:
                c = k(THREAD, light) if (i + j) % 2 == 0 else k(SEAM, light)
            if border and (i < 2 or i >= fw - 2 or j < 2 or j >= fh - 2):
                c = k(CRIMSON_DEEP, light) if (i in (0, fw - 1) or j in (0, fh - 1)) else k(THREAD, light)
            p.px((fx + i, fy + j), [v + p.rnd.randint(-4, 4) for v in c])


def silk(p, rect, base=CRIMSON, light=1.0, hem=0):
    """The crimson robe: silk with folds running down it; a gold hem at its foot (hem rows)."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            f = light * (1.0 + 0.12 * math.sin(i * 1.1 + 0.3 * math.sin(j * 0.3)) - 0.14 * j / max(1, fh))
            c = k(base, f)
            if hem and j >= fh - hem:
                c = k(THREAD if j != fh - 1 else GOLD_DEEP, light)
                if j == fh - hem + 1 and i % 4 == 0:
                    c = k(CRIMSON_DEEP, light)
            p.px((fx + i, fy + j), [v + p.rnd.randint(-3, 3) for v in c])


def metal(p, rect, light=1.0, base=GOLD, chase=False, beaded=False):
    """Gold: lit along the top, darker below, a highlight; chased with a running cloud, or beaded."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            t = j / max(1, fh - 1)
            c = mixc(GOLD_LIGHT, base, t * 1.4) if t < 0.5 else mixc(base, GOLD_DEEP, (t - 0.5) * 1.6)
            if chase and fh >= 3 and (i + int(2 * math.sin(i * 0.8))) % 5 == 0 and 0 < j < fh - 1:
                c = GOLD_DEEP
            if beaded and i % 3 == 1 and 0 < j < fh - 1:
                c = GOLD_LIGHT
            if beaded and i % 3 == 0:
                c = GOLD_DEEP
            p.px((fx + i, fy + j), k(c, light * (1 + p.rnd.uniform(-0.03, 0.03))))


def shape_leaf(p, rect, keep=0.3):
    """Cuts a face to a lotus petal: round at its foot, pointed at its top."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        t = j / max(1, fh - 1)               # 0 top .. 1 foot
        half = (fw / 2) * (math.sin(min(1.0, t * 1.15) * math.pi * 0.62) if t < 0.86 else 1.0)
        half = max(keep, half)
        for i in range(fw):
            if abs(i + 0.5 - fw / 2) > half:
                p.clear((fx + i, fy + j))


def flame(p, rect, inner, outer, upward=True, alpha_glow=True):
    """A tongue of golden flame: cut to its flickering shape, hottest at the root and along its middle."""
    fx, fy, fw, fh = rect
    sway = p.rnd.uniform(0, 6.28)
    for j in range(fh):
        t = (j / max(1, fh - 1)) if upward else 1 - j / max(1, fh - 1)   # 0 at its tip .. 1 at its root
        width = (fw / 2) * (0.15 + 0.85 * math.sin(min(1.0, t * 1.2) * math.pi / 2))
        centre = fw / 2 + 0.6 * math.sin(t * 5 + sway) * (1 - t)
        for i in range(fw):
            d = abs(i + 0.5 - centre)
            if d > width:
                p.clear((fx + i, fy + j))
                continue
            core = 1 - d / max(0.5, width)
            c = mixc(outer, inner, 0.35 * t + 0.65 * core * t)
            p.px((fx + i, fy + j), c, glow=True)


def disc(p, rect, rx, ry, pointed=False):
    """A disc (an almond, if pointed) of light, on the glowing skin alone: brightest in a ring near its edge
    and in the middle, faint between, nothing outside it."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            x = (i + 0.5 - fw / 2) / (fw / 2)
            y = (j + 0.5 - fh / 2) / (fh / 2)
            if pointed and y < 0:
                x = x / max(0.05, 1 - 0.45 * y * y)
            r = math.hypot(x, y)
            if r > 1:
                p.clear((fx + i, fy + j))
                continue
            glow = 0.14 + 0.55 * math.exp(-((r - 0.84) / 0.08) ** 2) + 0.18 * math.exp(-(r / 0.3) ** 2)
            # rays of light, faintly, from the middle
            ang = math.atan2(y, x)
            glow += 0.1 * max(0.0, math.cos(ang * 12)) * (r > 0.3)
            c = mixc(LIGHT_DEEP, LIGHT_HOT, min(1.0, glow))
            p.light((fx + i, fy + j), [v * min(1.0, glow) for v in c])


def paint_box(p, style, f, dims):
    w, h, d = dims
    if style == 'head':
        for s, light in SIDES:
            if s == 'front':
                heart_face(p, f[s])
            else:
                fur(p, f[s], light)
    elif style in ('fur_crown', 'fur_mane', 'fur_neck', 'ruff', 'ruff_small'):
        for s, light in SIDES:
            fur(p, f[s], light, base=FUR if style != 'fur_mane' else FUR_DARK)
        if style.startswith('ruff'):
            for s in ('front', 'back', 'right', 'left'):
                fx, fy, fw, fh = f[s]
                for j in range(fh):           # drawn to a point at its end, a little ragged
                    half = (fw / 2) * (1 - j / fh) + 0.3
                    for i in range(fw):
                        if abs(i + 0.5 - fw / 2) > half:
                            p.clear((fx + i, fy + j))
    elif style == 'brow':
        for s, light in SIDES:
            fur(p, f[s], light * 0.85, base=FUR_DARK)
    elif style in ('muzzle', 'jaw', 'nose'):
        for s, light in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    c = mixc(SKIN_LIGHT, SKIN_SHADE, j / max(1, fh - 1) * 0.8)
                    p.px((fx + i, fy + j), k(c, light))
        fx, fy, fw, fh = f['front']
        if style == 'muzzle':      # the nostrils, and the line of the lip
            for i in (fw // 2 - 2, fw // 2 + 1):
                p.px((fx + i, fy + 1), (90, 50, 40))
            for i in range(0, fw):
                p.px((fx + i, fy + fh - 1 - (0 if 1 < i < fw - 2 else 1)), MOUTH)
        if style == 'jaw':
            for i in range(fw):
                p.px((fx + i, fy), MOUTH)
        if style == 'nose':
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), SKIN_SHADE)
    elif style == 'eye':
        for s, light in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), (40, 14, 10))
        fx, fy, fw, fh = f['front']
        # the Fiery Golden Eyes: red at the rim, gold within, a dark pupil, all burning
        for j in range(fh):
            for i in range(fw):
                rim = i in (0, fw - 1) or j in (0, fh - 1)
                c = OPERA_RED if rim else (255, 214, 80)
                if not rim and abs(i + 0.5 - fw / 2) < 1 and fh > 2:
                    c = (30, 10, 6)
                p.px((fx + i, fy + j), c, glow=c != (30, 10, 6))
    elif style == 'ear':
        for s, light in SIDES:
            fur(p, f[s], light)
        fx, fy, fw, fh = f['right']   # the inner ear, bare
        for j in range(1, fh - 1):
            for i in range(1, fw - 1):
                p.px((fx + i, fy + j), k(SKIN_SHADE, 0.95))
    elif style == 'circlet':
        for s, light in SIDES:
            metal(p, f[s], light, chase=True)
        fx, fy, fw, fh = f['front']
        for i in range(2, fw - 2, 6):   # jewels set round it
            for jj in range(fh // 3, fh - fh // 3):
                p.px((fx + i, fy + jj), JEWEL_RED if (i // 6) % 2 == 0 else JEWEL_BLUE)
    elif style in ('crown_leaf', 'crown_leaf_main'):
        for s, light in SIDES:
            metal(p, f[s], light)
            fx, fy, fw, fh = f[s]
            if s in ('front', 'back'):
                for j in range(fh):          # its edge raised, a vein of red enamel within
                    for i in range(fw):
                        if abs(i + 0.5 - fw / 2) < 0.8 and j > fh * 0.3:
                            p.px((fx + i, fy + j), k(CRIMSON_LIGHT, light))
                shape_leaf(p, f[s])
    elif style in ('jewel_red', 'jewel_blue'):
        base = JEWEL_RED if style == 'jewel_red' else JEWEL_BLUE
        for s, light in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    c = (255, 200, 200) if (i == 0 and j == 0) else base
                    p.px((fx + i, fy + j), k(c, light), glow=True)
    elif style in ('ribbon', 'ribbon_end'):
        for s, light in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    c = THREAD if i in (0, fw - 1) else k(CRIMSON_LIGHT if (j // 3) % 2 else CRIMSON, 1.0)
                    p.px((fx + i, fy + j), k(c, light * (1 - 0.15 * j / max(1, fh))))
            if style == 'ribbon_end' and s in ('front', 'back'):   # a forked end
                for j in range(fh - 3, fh):
                    for i in range(fw):
                        if abs(i + 0.5 - fw / 2) < (j - (fh - 4)):
                            p.clear((fx + i, fy + j))
    elif style == 'kasaya_torso':
        # the right breast and shoulder bare of the kasaya: there the crimson robe, its collar crossed
        def bare(i, j, fw, fh, front=True):
            x, y = i / fw, j / fh
            return y < 0.95 - 1.15 * (1 - x) if front else y < 0.95 - 1.15 * x
        for s, light in SIDES:
            if s in ('front', 'back'):
                front = s == 'front'
                silk(p, f[s], CRIMSON, light)
                patchwork(p, f[s], light, gap=lambda i, j, fw, fh, front=front: bare(i, j, fw, fh, front), border=False)
                if front:   # the robe's crossed collar at his throat: white over gold
                    fx, fy, fw, fh = f[s]
                    for j in range(int(fh * 0.4)):
                        for i in range(fw):
                            off = abs(i + 0.5 - fw * 0.42)
                            if abs(off - j * 0.55) < 1.0:
                                p.px((fx + i, fy + j), WHITE)
                            elif abs(off - j * 0.55 - 1.4) < 0.7:
                                p.px((fx + i, fy + j), THREAD)
            elif s == 'right':
                silk(p, f[s], CRIMSON, light)
            elif s == 'top':
                fx, fy, fw, fh = f[s]
                silk(p, f[s], CRIMSON, light)
                patchwork(p, (fx + fw // 2, fy, fw - fw // 2, fh), light, border=False)
            else:
                patchwork(p, f[s], light, border=False)
    elif style == 'edge_roll':
        for s, light in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    c = THREAD if i in (0, fw - 1) else (OCHRE if (j // 2) % 2 else SAFFRON)
                    if 0 < i < fw - 1 and j % 6 == 0:
                        c = SEAM
                    p.px((fx + i, fy + j), k(c, light * (1.05 if s == 'front' else 0.9)))
    elif style == 'fold':
        for s, light in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), k(OCHRE if s != 'front' else SAFFRON, light * (1.1 - 0.3 * j / max(1, fh))))
    elif style in ('cord', 'knot', 'tassel', 'mala', 'gold_band'):
        for s, light in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    if style == 'tassel':
                        c = CRIMSON_LIGHT if j > 2 else THREAD
                        if j > 2 and i % 2:
                            c = CRIMSON
                    elif style == 'mala':
                        c = BEAD_LIGHT if i % 3 == 1 else BEAD if i % 3 == 2 else (40, 22, 14)
                    elif style == 'gold_band':
                        c = mixc(GOLD_LIGHT, GOLD_DEEP, j / max(1, fh - 1))
                    else:   # a cord twisted of gold thread
                        c = THREAD if (i + j) % 3 else GOLD_DEEP
                    p.px((fx + i, fy + j), k(c, light))
    elif style in ('apron', 'back_panel', 'side_panel', 'drape', 'kasaya_sleeve'):
        for s, light in SIDES:
            patchwork(p, f[s], light, cell=(6, 9) if style != 'side_panel' else (5, 8))
    elif style in ('robe_sleeve', 'robe_leg'):
        for s, light in SIDES:
            silk(p, f[s], CRIMSON, light)
    elif style in ('wide_sleeve', 'robe_hem'):
        for s, light in SIDES:
            silk(p, f[s], CRIMSON, light, hem=3 if s not in ('top', 'bottom') else 0)
    elif style == 'sleeve_hem':
        for s, light in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), k(THREAD if (i // 2) % 3 else GOLD_DEEP, light))
    elif style == 'hand':
        for s, light in SIDES:
            if s == 'bottom' or s == 'front':
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    for i in range(fw):
                        c = SKIN_SHADE if (i % 3 == 0 and s == 'front') else SKIN
                        p.px((fx + i, fy + j), k(c, light))
            else:
                fur(p, f[s], light)
    elif style in ('bead', 'guru_bead'):
        base = BEAD if style == 'bead' else JADE
        for s, light in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    c = k(base, 1.4) if (i == 0 and j == 0) else base
                    p.px((fx + i, fy + j), k(c, light))
    elif style == 'gold':
        for s, light in SIDES:
            metal(p, f[s], light)
    elif style == 'staff_shaft':
        for s, light in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    # red lacquer, a narrow gold band every so often, a sheen down one side
                    band = (j % 14) in (0, 1)
                    c = GOLD if band else (CRIMSON_LIGHT if i == 0 else CRIMSON)
                    p.px((fx + i, fy + j), k(c, light * (1.1 if i == 0 else 1.0)))
    elif style in ('staff_cap', 'staff_band'):
        for s, light in SIDES:
            metal(p, f[s], light, chase=style == 'staff_cap')
    elif style == 'shoe':
        for s, light in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    p.px((fx + i, fy + j), k(SHOE if j < fh - 1 else (220, 210, 196), light))
    elif style == 'shoe_toe':
        for s, light in SIDES:
            metal(p, f[s], light * 0.9, base=CRIMSON_LIGHT)
    elif style in ('gold_ring', 'gold_beaded'):
        for s, light in SIDES:
            metal(p, f[s], light, beaded=style == 'gold_beaded')
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    if s == 'front':
                        p.glow.putpixel((fx + i, fy + j), (255, 214, 120, 255) if (i + j) % 2 == 0 else (180, 130, 50, 255))
    elif style in ('flame', 'flame_x', 'flame_deep'):
        inner, outer = (LIGHT_HOT, LIGHT) if style != 'flame_deep' else (LIGHT, LIGHT_DEEP)
        for s, _ in SIDES:
            if s in ('top', 'bottom'):
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    for i in range(fw):
                        p.clear((fx + i, fy + j))
                continue
            flame(p, f[s], inner, outer)
    elif style in ('halo_disc', 'mandorla_disc'):
        for s, _ in SIDES:
            fx, fy, fw, fh = f[s]
            if s in ('front', 'back'):
                disc(p, f[s], 1, 1, pointed=style == 'mandorla_disc')
            else:
                for j in range(fh):
                    for i in range(fw):
                        p.clear((fx + i, fy + j))
    else:
        raise ValueError(style)


if __name__ == '__main__':
    p = BuddhaPainter(1000)
    slots = exact_paint(BUDDHA, p, paint_box)
    p.save('fighting_buddha')
    emit(BUDDHA, slots, JAVA)
    print('fighting buddha', len(BUDDHA), 'parts,', len(slots), 'boxes')
