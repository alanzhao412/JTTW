"""Sun Wukong himself, as every Wukong player is drawn from the start (below level 100, where the
Victorious Fighting Buddha takes his place): the Monkey King's own body, no armour or outfit of its own.

His head is the Fighting Buddha's sculpted one (fighting_buddha_head.py: brow, sockets, Fiery Golden
Eyes, fur frame, tiered muzzle, cupped ears), drawn without the Buddha's crown; this is the rest of him,
and the golden fillet (紧箍) the Bodhisattva set on his head, which is part of him, not armour:

- a lean, muscular monkey's build in golden-brown fur, about a man's size so that real armour fits him:
  a ruff at the neck, broad shoulders, the fur darker down the spine;
- bare tan skin on the chest and belly, shaped into pectorals and a lean belly by shading;
- long arms, furred, the forearms tapering to monkey hands of bare skin: a long palm, fingers curled
  with knuckles and dark nails, a thumb set low;
- bowed, furred legs to monkey feet of bare skin, long, with long toes and a big toe set apart;
- a tail, long and furred, curling up behind him in four lengths, a tuft at its tip, swaying;
- on his head the golden fillet: a plain band of gold over his brow, its two ends curled at the front.

As fighting_buddha.py: one list of parts gives the model, written into MonkeyKingModel.layer() between
its GENERATED markers, and the skin (monkey_king.png), painted to the same box-UV layout at four texture
pixels to the model pixel, each face where the game looks for it (king_yama.paint). Each piece hangs
from a copy of the player's head, body, arms and legs, posed as the player is each frame.

Run from the repository root: python assets_source/monkey_king.py"""
import math

import beastgen as bg
from beastgen import emit
from fire_court import part, mirrored
from king_yama import paint as exact_paint
import fighting_buddha_head as fbh
from fighting_buddha_head import fur, shaded, taper, mixc, k, SIDES, FUR, FUR_LIGHT, FUR_DARK, FUR_DEEP, \
    SKIN, SKIN_LIGHT, SKIN_HIGH, SKIN_DARK, SKIN_DEEP

JAVA = 'src/main/java/com/jttw/engine/client/renderer/MonkeyKingModel.java'

GOLD = (236, 188, 68)
GOLD_LIGHT = (255, 232, 150)
GOLD_DEEP = (150, 100, 26)
NAIL = (70, 40, 28)


def tail():
    """The tail: four lengths from the small of his back, curling up behind him, a tuft at its tip."""
    return [
        part('tail', 'body', (0, 10.4, 1.9), (0.95, 0, 0), [(-0.65, 0, -0.65, 1.3, 4.4, 1.3, 'tail')]),
        part('tail_1', 'tail', (0, 4.1, 0), (0.6, 0, 0), [(-0.6, 0, -0.6, 1.2, 4.2, 1.2, 'tail')]),
        part('tail_2', 'tail_1', (0, 3.9, 0), (0.6, 0, 0), [(-0.55, 0, -0.55, 1.1, 4.0, 1.1, 'tail')]),
        part('tail_3', 'tail_2', (0, 3.7, 0), (0.5, 0, 0), [(-0.5, 0, -0.5, 1.0, 3.4, 1.0, 'tail')]),
        part('tail_tip', 'tail_3', (0, 3.1, 0), (0.25, 0, 0), [(-0.75, 0, -0.75, 1.5, 3.0, 1.5, 'tail_tip')]),
    ]


MONKEY = mirrored([
    # the player's own parts, at their pivots (the model copies the player's pose onto them each frame)
    part('head', 'root', (0, 0, 0), (0, 0, 0), []),
    part('body', 'root', (0, 0, 0), (0, 0, 0), []),
    part('right_arm', 'root', (-5, 2, 0), (0, 0, 0), []),
    part('right_leg', 'root', (-1.9, 12, 0), (0, 0, 0), []),

    # ── the golden fillet ──
    part('fillet', 'head', (0, 0, 0), (0, 0, 0), [(-4.45, -9.0, -4.15, 8.9, 1.0, 9.0, 'fillet')]),
    part('right_fillet_curl', 'head', (-0.9, -8.45, -4.35), (0, 0, 0.785), [(-0.75, -0.75, -0.25, 1.5, 1.5, 0.5, 'fillet_curl')]),

    # ── the body: lean, muscular, furred, broad at the shoulders and narrow at the waist; the chest and belly bare ──
    part('torso', 'body', (0, 0, 0), (0, 0, 0), [
        (-4.2, -0.1, -2.25, 8.4, 5.6, 4.5, 'fur_torso'),
        (-3.5, 5.3, -1.95, 7.0, 5.0, 3.9, 'fur_waist'),
        (-3.8, 9.9, -2.05, 7.6, 2.2, 4.1, 'fur_hips'),
        (-2.7, -1.1, -2.6, 5.4, 1.5, 5.2, 'ruff'),
        (-1.95, 4.5, -2.3, 3.9, 5.2, 0.45, 'belly'),
        (-3.7, 0.4, 2.05, 7.4, 5.2, 0.5, 'fur_back'),
    ]),
    part('right_pec', 'body', (0, 0, 0), (0, 0, 0), [(-3.35, 0.55, -2.75, 3.25, 3.7, 0.55, 'pec')]),
    # the flanks and back rounded: the lats swelling at the sides, the upper back over the shoulder blades
    part('right_flank', 'body', (0, 0, 0), (0, 0, 0), [(-4.6, 0.7, -1.7, 0.45, 4.4, 3.5, 'fur_flank')]),
    part('upper_back', 'body', (0, 0, 0), (0, 0, 0), [(-3.2, 0.3, 2.5, 6.4, 4.0, 0.4, 'fur_back')]),

    # ── the arms: long, furred, a round shoulder and a bulge of muscle, to bare monkey hands ──
    part('right_upper_arm', 'right_arm', (0, 0, 0), (0, 0, 0), [
        (-3.65, -2.6, -2.4, 4.6, 3.1, 4.8, 'fur_shoulder'),
        (-3.05, -0.2, -1.95, 3.8, 4.6, 3.9, 'fur_arm'),
        (-2.65, 0.4, -2.35, 2.9, 3.1, 0.5, 'fur_arm'),
    ]),
    # the forearm and hand, from the elbow: it bends there (MonkeyKingModel folds it to grip the staff at rest)
    part('right_forearm', 'right_upper_arm', (-1, 4.2, 0), (0, 0, 0), [
        (-2.0, 0, -2.0, 3.8, 3.0, 4.0, 'fur_forearm'),
        (-1.8, 2.8, -1.8, 3.4, 3.4, 3.6, 'fur_forearm'),
        (-1.85, 6.0, -1.85, 3.5, 2.3, 3.7, 'hand'),
        (-1.8, 8.1, -1.95, 3.4, 1.0, 2.9, 'fingers'),
        (0.85, 6.3, -2.4, 0.9, 1.7, 0.9, 'thumb'),
    ]),

    # ── the legs: bowed a little, furred, a heavy thigh and calf, to long monkey feet ──
    part('right_thigh', 'right_leg', (0, 0, 0), (0, 0, 0.04), [
        (-2.2, -0.1, -2.2, 4.4, 6.0, 4.4, 'fur_thigh'),
        (-1.95, 5.8, -1.95, 3.9, 4.8, 3.9, 'fur_shin'),
        (-1.6, 6.2, 1.9, 3.2, 2.6, 0.5, 'fur_shin'),
        (-2.0, 10.5, -3.3, 4.0, 1.5, 5.1, 'foot'),
        (-1.85, 10.9, -4.15, 3.1, 1.1, 0.9, 'toes'),
        (1.05, 10.85, -3.75, 0.9, 1.15, 1.3, 'big_toe'),
    ]),
] + tail())


def furred(p, f, light=1.0, base=FUR, slant=0.0):
    # no darkening at each face's top: the boxes stacked down a limb would show bands where they meet
    for s, l in SIDES:
        fur(p, f[s], light * (1.0 if s in ('front', 'back', 'right', 'left') else l), base=base, slant=slant, root_dark=False)


def skin_box(p, f, base=SKIN):
    shaded(p, f, base, top=1.08, bottom=0.6, side=0.88)


def paint_box(p, style, f, dims):
    if style in ('fur_torso', 'fur_flank', 'fur_waist', 'fur_hips', 'fur_arm', 'fur_shoulder', 'fur_forearm', 'fur_thigh', 'fur_shin'):
        furred(p, f, 1.0, base=FUR, slant=0.15)
    elif style == 'fur_back':
        furred(p, f, 0.92, base=FUR_DARK)
    elif style == 'ruff':
        furred(p, f, 1.05, base=FUR, slant=0.3)
        for s in ('front', 'back', 'right', 'left'):        # its lower edge in ragged points
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                cut = int(2 + 2 * abs(math.sin(i * 0.9)))
                for j in range(max(0, fh - cut), fh):
                    p.clear((fx + i, fy + j))
    elif style == 'pec':
        # a pectoral: rounded by its light, lit across the top, shadowed beneath
        skin_box(p, f, SKIN)
        fx, fy, fw, fh = f['front']
        for j in range(fh):
            for i in range(fw):
                y = j / max(1, fh - 1)
                x = (i + 0.5) / fw
                c = mixc(SKIN_HIGH, SKIN, min(1.0, y * 1.3))
                if y > 0.7:
                    c = mixc(c, SKIN_DARK, (y - 0.7) / 0.3 * 0.8)
                c = k(c, 1 - 0.1 * abs(x - 0.55) * 2)
                p.px((fx + i, fy + j), [v + p.rnd.randint(-2, 2) for v in c])
    elif style == 'chest':
        # bare skin: two pectorals, lit across their tops, shadowed beneath and down the middle
        skin_box(p, f, SKIN)
        fx, fy, fw, fh = f['front']
        cx = fw / 2
        for j in range(fh):
            for i in range(fw):
                x = abs(i + 0.5 - cx) / cx
                y = j / max(1, fh - 1)
                c = mixc(SKIN_LIGHT, SKIN, y * 0.9)
                if y > 0.72:
                    c = mixc(c, SKIN_DARK, (y - 0.72) / 0.28 * (0.4 + 0.6 * (1 - abs(x - 0.5) * 2)))
                if x < 0.08:
                    c = mixc(c, SKIN_DARK, 0.45)
                if x > 0.86:                                 # where the fur meets it
                    c = mixc(c, FUR_DARK, (x - 0.86) / 0.14)
                p.px((fx + i, fy + j), [v + p.rnd.randint(-2, 2) for v in c])
    elif style == 'belly':
        # a lean belly: the muscle in three pairs of soft blocks down the middle
        skin_box(p, f, SKIN)
        fx, fy, fw, fh = f['front']
        cx = fw / 2
        for j in range(fh):
            for i in range(fw):
                x = abs(i + 0.5 - cx) / cx
                y = j / max(1, fh - 1)
                c = mixc(SKIN_LIGHT, SKIN, 0.3 + 0.4 * y)
                band = (y * 3) % 1.0
                if band < 0.12 and x < 0.7:
                    c = mixc(c, SKIN_DARK, 0.5)
                if x < 0.07:
                    c = mixc(c, SKIN_DARK, 0.45)
                if x > 0.8:
                    c = mixc(c, mixc(SKIN_DARK, FUR_DARK, 0.5), (x - 0.8) / 0.2)
                p.px((fx + i, fy + j), [v + p.rnd.randint(-2, 2) for v in c])
    elif style in ('hand', 'foot'):
        skin_box(p, f, SKIN)
        fx, fy, fw, fh = f['top' if style == 'foot' else 'back']   # fur on the back of the hand and the top of the foot
        fur(p, (fx, fy, fw, fh), 0.95, base=FUR, slant=0.1)
    elif style in ('fingers', 'toes'):
        skin_box(p, f, SKIN_LIGHT)
        for s in ('front', 'bottom') if style == 'fingers' else ('front', 'top'):
            fx, fy, fw, fh = f[s]
            for i in range(fw):                              # the digits parted, dark nails at their ends
                if i % 4 == 0:
                    for j in range(fh):
                        p.px((fx + i, fy + j), SKIN_DARK)
            for i in range(fw):
                if i % 4 in (1, 2):
                    p.px((fx + i, fy + fh - 1), NAIL)
    elif style in ('thumb', 'big_toe'):
        skin_box(p, f, SKIN_LIGHT)
    elif style == 'tail':
        furred(p, f, 1.0, base=FUR, slant=0.2)
    elif style == 'tail_tip':
        furred(p, f, 1.05, base=FUR_LIGHT, slant=0.2)
        for s in ('front', 'back', 'right', 'left'):
            taper(p, f[s], sharp=1.2)
    elif style in ('fillet', 'fillet_curl'):
        for s, light in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh):
                for i in range(fw):
                    t = j / max(1, fh - 1)
                    c = mixc(GOLD_LIGHT, GOLD, t * 1.6) if t < 0.5 else mixc(GOLD, GOLD_DEEP, (t - 0.5) * 1.6)
                    p.px((fx + i, fy + j), k(c, light * (1 + p.rnd.uniform(-0.03, 0.03))))
        if style == 'fillet_curl':                          # its scroll: a ring cut within
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    for i in range(fw):
                        r = math.hypot((i + 0.5 - fw / 2) / (fw / 2), (j + 0.5 - fh / 2) / (fh / 2))
                        if r > 1.0:
                            p.clear((fx + i, fy + j))
                        elif r < 0.45:
                            p.px((fx + i, fy + j), GOLD_DEEP)
    else:
        raise ValueError(style)


if __name__ == '__main__':
    from fighting_buddha import BuddhaPainter
    bg.UNITS = 128
    bg.S = 4
    bg.TEX = bg.UNITS * bg.S
    p = BuddhaPainter(2024)
    slots = exact_paint(MONKEY, p, paint_box)
    p.img.save('src/main/resources/assets/jttw_engine/textures/entity/monkey_king.png')
    emit(MONKEY, slots, JAVA)
    print('monkey king', len(MONKEY), 'parts,', len(slots), 'boxes')
