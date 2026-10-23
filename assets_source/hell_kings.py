"""Paints the Ten Kings of Hell (textures/entity/king_*.png, 128x64) to the layout of
HellRenderers.kingLayer(): a judge's robe and wide sleeves in his court's colour, bordered and
embroidered in gold, a jade belt, the mian crown (a black board edged in red and gold, strings of
beads hanging before and behind), a beard, and the ivory tablet. King Yama is in deep crimson, his
face dark, his beard black and fierce, his eyes burning (king_yama_eyes.png, drawn full-bright).

Run from the repository root: python assets_source/hell_kings.py"""
from cave_skin import Skin, faces, shade, mix
from hell_skin import HEAD, BODY, ARM, SIDES, face_box, robe_body, sleeves, skirt

GOLD, JADE, IVORY = (222, 178, 64), (150, 200, 160), (236, 226, 200)

# court: (name, robe, face, beard, beads)
KINGS = {
    1: ('qinguang', (30, 52, 116), (222, 190, 160), (24, 20, 20), (200, 40, 40)),
    2: ('chujiang', (140, 178, 210), (214, 200, 196), (40, 40, 50), (220, 240, 250)),
    3: ('songdi', (76, 76, 88), (214, 184, 156), (120, 120, 124), (200, 40, 40)),
    4: ('wuguan', (38, 100, 56), (220, 188, 156), (24, 20, 20), GOLD),
    5: ('yama', (132, 18, 24), (110, 72, 60), (14, 12, 12), GOLD),
    6: ('biancheng', (88, 40, 112), (216, 186, 160), (30, 24, 24), (200, 40, 40)),
    7: ('taishan', (176, 78, 24), (220, 180, 150), (120, 40, 20), GOLD),
    8: ('dushi', (108, 84, 54), (210, 176, 146), (40, 30, 24), (200, 40, 40)),
    9: ('pingdeng', (218, 212, 192), (226, 196, 170), (236, 236, 236), JADE),
    10: ('zhuanlun', (30, 30, 34), (220, 190, 160), (24, 20, 20), GOLD),
}


def embroider(s, box, colour, every=7):
    f = faces(*box)
    for side in ('front', 'back', 'right', 'left'):
        x, y, w, h = f[side]
        for j in range(1, h - 2):
            for i in range(w):
                if (i * 3 + j * 5) % every == 0:
                    s.put(x + i, y + j, colour)


def king(court, name, robe, face, beard, beads):
    s = Skin(128, 64, seed=court * 13)
    yama = court == 5
    trim = GOLD if court != 10 else (240, 200, 80)
    face_box(s, face, (20, 18, 20), eyes=(250, 200, 60) if yama else (30, 20, 20),
             eye_white=(250, 240, 200) if yama else (236, 230, 220), brows=beard if court not in (9,) else (200, 200, 200),
             fierce=yama or court in (7, 8), beard=beard, mouth=(120, 50, 50))
    robe_body(s, robe, trim, collar=(230, 226, 214), belt=JADE)
    sleeves(s, robe, trim, face)
    skirt(s, 64, 0, 10, 14, 7, robe, trim, hem=2, panel=shade(robe, 0.75))
    embroider(s, (64, 0, 10, 14, 7), shade(trim, 0.9))
    embroider(s, BODY, shade(trim, 0.9), every=9)
    # a rank badge on the chest: a gold square
    x, y, w, h = faces(*BODY)['front']
    s.fill(x + 2, y + 3, 4, 3, trim, noise=4)
    s.put(x + 3, y + 4, shade(robe, 0.6))
    s.put(x + 4, y + 4, shade(robe, 0.6))
    # the wide sleeves, gold-bordered at the cuff
    f = faces(64, 22, 5, 8, 5)
    for side in SIDES:
        x, y, w, h = f[side]
        s.fill(x, y, w, h, robe, noise=5, grad=0.15)
        if side not in ('top', 'bottom'):
            s.fill(x, y + h - 2, w, 2, trim, noise=3)
    # the jade belt, its plaques gold
    f = faces(84, 22, 9, 2, 5)
    for side in SIDES:
        x, y, w, h = f[side]
        s.fill(x, y, w, h, JADE, noise=6)
        for i in range(0, w, 3):
            s.put(x + i, y + h // 2, GOLD)
    # the crown: its cap gold-and-black, its board black above, red at the edges, gold-rimmed
    f = faces(98, 0, 7, 2, 7)
    for side in SIDES:
        s.fill(*f[side], (30, 26, 28) if side == 'top' else GOLD, noise=3)
    f = faces(64, 36, 10, 1, 12)
    for side in SIDES:
        x, y, w, h = f[side]
        s.fill(x, y, w, h, (24, 22, 26), noise=2)
        if side in ('front', 'back', 'right', 'left'):
            s.fill(x, y, w, h, (170, 30, 30), noise=3)
        if side == 'top':
            for i in range(w):
                s.put(x + i, y, GOLD)
                s.put(x + i, y + h - 1, GOLD)
    if yama:
        # Yama's board bears the character 王 in gold
        x, y, w, h = f['top']
        for i in range(3, 7):
            for j in (3, 6, 9):
                s.put(x + i, y + j, GOLD)
        for j in range(3, 10):
            s.put(x + 5, y + j, GOLD)
    # the beads, strung alternately
    for j in range(5):
        for i in range(4):
            s.put(110 + i, 50 + j, beads if (j % 2 == 0) else shade(beads, 0.6))
    # the beard box
    f = faces(96, 50, 4, 6, 1)
    for side in SIDES:
        s.fill(*f[side], beard, noise=4)
    # the ivory tablet
    f = faces(116, 50, 2, 11, 1)
    for side in SIDES:
        s.fill(*f[side], IVORY, noise=4)
    s.save('entity', 'king_' + name + '.png')


for court, (name, robe, face, beard, beads) in KINGS.items():
    king(court, name, robe, face, beard, beads)

# King Yama's burning eyes
s = Skin(128, 64)
x, y, w, h = faces(*HEAD)['front']
for ex, inner in ((1, 2), (6, 5)):
    s.put(x + ex, y + 3, (255, 236, 150))
    s.put(x + inner, y + 3, (255, 140, 40))
s.save('entity', 'king_yama_eyes.png')
