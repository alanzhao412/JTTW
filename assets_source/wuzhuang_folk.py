"""Paints the household of the Temple of the Five Villages (textures/entity/*.png, 128x64) to the
layouts of WuzhuangRenderers' robed frame:
- daoist_disciple(_captain).png: a blue-grey Daoist robe (道袍) with a white collar and a dark
  border, a black sash; a topknot bound with a wooden hairpin. The captain, a senior disciple, in
  deep blue with a gold border and a black cap over his knot;
- clear_wind.png and bright_moon.png: the two youths, boys' faces, the two buns of a boy's hair
  bound with red cord; Clear Wind in pale green, Bright Moon in moon-white and blue;
- zhenyuan.png: the Great Immortal in an ochre-gold robe of the Earth Immortals (yellow is earth's
  colour) bordered in brown and embroidered with clouds, wide sleeves, a jade belt, a long white
  beard and white brows, a small gold crown and pin on his topknot; his horsetail whisk, a dark
  wooden handle and a long white tail.

Run from the repository root: python assets_source/wuzhuang_folk.py"""
from cave_skin import Skin, faces, shade, mix
from hell_skin import HEAD, BODY, ARM, SIDES, face_box, robe_body, sleeves, skirt

SKIN = (232, 200, 170)
YOUTH = (240, 210, 184)
GOLD = (226, 184, 70)
WOOD = (120, 84, 48)


def clouds(s, box, colour, every=9):
    """A scatter of little cloud scrolls over a box's sides."""
    f = faces(*box)
    for side in ('front', 'back', 'right', 'left'):
        x, y, w, h = f[side]
        for j in range(1, h - 3, 4):
            for i in range((j * 3) % every, w - 1, every):
                s.put(x + i, y + j, colour)
                s.put(x + i + 1, y + j, colour)
                s.put(x + i, y + j + 1, colour)


def bun(s, colour, cord):
    """The hair bun box (88, 22, 3x3x3), bound with a cord."""
    f = faces(88, 22, 3, 3, 3)
    for side in SIDES:
        s.fill(*f[side], colour, noise=3)
    for side in ('front', 'back', 'right', 'left'):
        x, y, w, h = f[side]
        for i in range(w):
            s.put(x + i, y + h - 1, cord)


def pin(s, colour):
    f = faces(100, 22, 6, 1, 1)
    for side in SIDES:
        s.fill(*f[side], colour, noise=4)


def disciple(captain):
    s = Skin(128, 64, seed=101 + captain)
    robe = (78, 104, 140) if not captain else (36, 52, 104)
    trim = (36, 40, 52) if not captain else GOLD
    hair = (22, 20, 22)
    face_box(s, SKIN, hair, eyes=(30, 22, 20), brows=(26, 22, 20), mouth=(160, 90, 80))
    robe_body(s, robe, trim, collar=(236, 232, 222), belt=(20, 20, 24))
    sleeves(s, robe, trim, SKIN)
    skirt(s, 64, 0, 10, 14, 7, robe, trim, hem=2, panel=shade(robe, 0.85))
    if captain:
        clouds(s, (64, 0, 10, 14, 7), shade(GOLD, 0.85), every=11)
        # a black Daoist cap over the knot
        bun(s, (24, 24, 30), GOLD)
    else:
        bun(s, hair, (60, 60, 70))
    pin(s, WOOD if not captain else GOLD)
    s.save('entity', 'daoist_disciple' + ('_captain' if captain else '') + '.png')


def youth(name, robe, trim, collar):
    s = Skin(128, 64, seed=len(name) * 7)
    hair = (24, 22, 24)
    face_box(s, YOUTH, hair, hair_front_rows=2, eyes=(30, 22, 20), brows=(40, 30, 26), mouth=(200, 110, 110))
    # rosy cheeks
    x, y, w, h = faces(*HEAD)['front']
    s.put(x + 1, y + 5, (236, 170, 160))
    s.put(x + 6, y + 5, (236, 170, 160))
    robe_body(s, robe, trim, collar=collar, belt=(180, 40, 40))
    sleeves(s, robe, trim, YOUTH)
    skirt(s, 64, 0, 10, 14, 7, robe, trim, hem=2, panel=shade(robe, 0.88))
    clouds(s, (64, 0, 10, 14, 7), shade(robe, 0.8), every=10)
    bun(s, hair, (196, 40, 40))
    s.save('entity', name + '.png')


def zhenyuan():
    s = Skin(128, 64, seed=777)
    robe = (206, 164, 74)
    trim = (110, 70, 34)
    white = (238, 236, 230)
    face_box(s, (226, 196, 168), (214, 212, 206), eyes=(30, 22, 20), brows=white, beard=white, mouth=(150, 90, 80))
    robe_body(s, robe, trim, collar=white, belt=(120, 180, 140))
    sleeves(s, robe, trim, (226, 196, 168))
    skirt(s, 64, 0, 10, 14, 7, robe, trim, hem=2, panel=shade(robe, 0.82))
    clouds(s, (64, 0, 10, 14, 7), shade(trim, 1.1), every=8)
    clouds(s, BODY, shade(trim, 1.1), every=10)
    # the yin-yang on his back
    x, y, w, h = faces(*BODY)['back']
    for j in range(4):
        for i in range(4):
            s.put(x + 2 + i, y + 3 + j, white if (i + (j > 1)) < 2 else (20, 20, 22))
    # the wide sleeves, bordered dark at the cuff, clouds on them
    f = faces(64, 22, 5, 9, 5)
    for side in SIDES:
        x, y, w, h = f[side]
        s.fill(x, y, w, h, robe, noise=5, grad=0.15)
        if side not in ('top', 'bottom'):
            s.fill(x, y + h - 2, w, 2, trim, noise=3)
    clouds(s, (64, 22, 5, 9, 5), shade(trim, 1.1), every=6)
    # his crown: small, gold, lotus-edged
    f = faces(64, 36, 5, 3, 5)
    for side in SIDES:
        x, y, w, h = f[side]
        s.fill(x, y, w, h, GOLD, noise=6)
        if side in ('front', 'back', 'right', 'left'):
            for i in range(0, w, 2):
                s.put(x + i, y, shade(GOLD, 1.2))
    pin(s, (240, 230, 200))
    # the long white beard
    f = faces(104, 25, 4, 8, 1)
    for side in SIDES:
        x, y, w, h = f[side]
        s.fill(x, y, w, h, white, noise=6, grad=0.1)
    # the whisk: a dark wooden handle, a long white horsetail
    s.fill(120, 0, 4, 10, (70, 44, 26), noise=5)
    s.fill(120, 0, 4, 1, GOLD, noise=2)
    f = faces(116, 36, 2, 10, 2)
    for side in SIDES:
        x, y, w, h = f[side]
        for j in range(h):
            for i in range(w):
                n = s.rnd.randint(-10, 6)
                s.put(x + i, y + j, tuple(v + n for v in white))
    s.save('entity', 'zhenyuan.png')


for c in (False, True):
    disciple(c)
youth('clear_wind', (128, 182, 150), (60, 110, 84), (240, 236, 220))
youth('bright_moon', (214, 220, 236), (80, 96, 160), (250, 250, 250))
zhenyuan()
