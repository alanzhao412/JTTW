"""Paints the skins of Hell's lesser officials and its ghosts (textures/entity/*.png) to the layouts
of HellRenderers' layers:
- judge_clerk(_captain).png (128x64): a red official's robe (a captain's violet), a black winged
  cap, a thin black beard, his ledger in blue covers, a bamboo brush tipped with black;
- black_impermanence.png and white_impermanence.png (128x64): the Black in black, his face dark
  and stern, "天下太平" in white down his tall hat; the White in white, his face pale and smiling,
  his long red tongue, "一见生财" in black down his hat; mourning staffs;
- wandering_ghost(_captain).png (64x64): a grey-white shroud, a grey face with hollow eyes, long
  black hair; below the waist it fades away (the trailing wisps grow more and more see-through).

Run from the repository root: python assets_source/hell_officials.py"""
from cave_skin import Skin, faces, shade, mix
from hell_skin import HEAD, BODY, ARM, LEG, SIDES, face_box, robe_body, sleeves, skirt

SKIN = (224, 196, 168)


def clerk(captain):
    s = Skin(128, 64, seed=21 + captain)
    robe = (150, 26, 30) if not captain else (96, 40, 120)
    trim = (30, 26, 30) if not captain else (220, 180, 70)
    face_box(s, SKIN, (24, 22, 24), eyes=(30, 20, 20), brows=(20, 18, 20), fierce=True, beard=None, mouth=(130, 70, 60))
    # a thin black beard and moustache
    x, y, w, h = faces(*HEAD)['front']
    for i in (2, 5):
        s.put(x + i, y + 5, (20, 18, 20))
    robe_body(s, robe, trim, collar=(235, 230, 220), belt=(40, 40, 44))
    sleeves(s, robe, trim, SKIN)
    skirt(s, 64, 0, 10, 14, 7, robe, trim, panel=shade(robe, 0.8))
    # the cap: black, faintly sheened; its crown; its wings
    for box in ((64, 22, 9, 3, 7), (96, 22, 7, 2, 4)):
        f = faces(*box)
        for side in SIDES:
            s.fill(*f[side], (26, 24, 28), noise=3)
        x, y, w, h = f['front']
        for i in range(w):
            s.put(x + i, y + h - 1, (60, 56, 64))
    f = faces(64, 34, 22, 2, 1)
    for side in SIDES:
        s.fill(*f[side], (20, 20, 24), noise=2)
    s.fill(118, 22, 8, 5, (20, 18, 20), noise=2)   # the beard box
    # the ledger: blue covers, white page edges
    f = faces(64, 40, 2, 6, 5)
    for side in SIDES:
        s.fill(*f[side], (40, 54, 110), noise=4)
    for side in ('front', 'back', 'bottom'):
        x, y, w, h = f[side]
        s.fill(x, y, w, h, (230, 222, 200), noise=3)
    x, y, w, h = f['right']
    s.fill(x + 1, y + 1, 2, 3, (220, 210, 190), noise=2)   # its title slip
    # the brush: bamboo, a black tip
    s.fill(120, 0, 4, 10, (176, 146, 80), noise=6)
    s.fill(120, 7, 4, 3, (16, 14, 16), noise=2)
    s.save('entity', 'judge_clerk' + ('_captain' if captain else '') + '.png')


def characters(s, box, colour, n=4):
    """A column of glyph-like marks down the front of a hat tier."""
    x, y, w, h = faces(*box)['front']
    cx = x + w // 2
    for j in range(h):
        if (j + n) % 2 == 0:
            s.put(cx, y + j, colour)
            s.put(cx - 1, y + j, colour)
        else:
            s.put(cx, y + j, colour)


def impermanence(white):
    s = Skin(128, 64, seed=31 + white)
    if white:
        robe, trim, hat, ink = (232, 232, 236), (150, 150, 160), (240, 240, 244), (24, 22, 26)
        face_box(s, (244, 238, 236), (30, 30, 34), closed=True, brows=(60, 60, 70), mouth=(200, 40, 50))
        # cheeks rouged, eyes smiling slits
        x, y, w, h = faces(*HEAD)['front']
        s.put(x + 1, y + 5, (240, 170, 170))
        s.put(x + 6, y + 5, (240, 170, 170))
    else:
        robe, trim, hat, ink = (26, 26, 30), (210, 210, 214), (22, 22, 26), (236, 236, 240)
        face_box(s, (82, 74, 78), (16, 16, 18), eyes=(240, 230, 120), eye_white=(250, 250, 250), brows=(10, 10, 12), fierce=True,
                 mouth=(40, 20, 24))
    robe_body(s, robe, trim, collar=trim, belt=trim)
    sleeves(s, robe, trim, (244, 238, 236) if white else (82, 74, 78))
    skirt(s, 64, 0, 10, 14, 7, robe, trim)
    # the tall hat, in three tiers, its motto down the front
    for k, box in enumerate(((64, 22, 9, 4, 9), (64, 36, 7, 4, 7), (92, 36, 5, 4, 5))):
        f = faces(*box)
        for side in SIDES:
            s.fill(*f[side], hat, noise=3)
        characters(s, box, ink, k)
    # the White's tongue: long, red
    f = faces(112, 0, 2, 8, 1)
    for side in SIDES:
        s.fill(*f[side], (204, 28, 40), noise=6)
    # a mourning staff, wrapped in paper
    s.fill(120, 0, 4, 25, (228, 228, 222) if white else (40, 38, 40), noise=4)
    for j in range(2, 25, 5):
        s.fill(120, j, 4, 2, (250, 250, 250) if not white else (120, 120, 126), noise=2)
    s.save('entity', ('white' if white else 'black') + '_impermanence.png')


def ghost(captain):
    s = Skin(64, 64, seed=41 + captain)
    shroud = (198, 208, 214) if not captain else (168, 226, 200)
    face = (176, 184, 190) if not captain else (160, 210, 190)
    hair = (18, 18, 22)
    face_box(s, face, hair, hair_front_rows=2, eyes=(10, 10, 14), eye_white=(10, 10, 14), mouth=(40, 40, 50))
    x, y, w, h = faces(*HEAD)['front']
    for ex in (1, 2, 5, 6):
        s.put(x + ex, y + 4, (30, 30, 40))   # hollows under the eyes
    robe_body(s, shroud, shade(shroud, 0.8))
    sleeves(s, shroud, shade(shroud, 0.85), face)
    # long hair down the back
    f = faces(0, 44, 8, 13, 1)
    for side in SIDES:
        s.fill(*f[side], hair, noise=4)
    # the trailing wisps, fading
    for box, alpha in (((0, 32, 8, 5, 4), 220), ((24, 32, 6, 4, 3), 150), ((44, 32, 3, 3, 2), 90)):
        f = faces(*box)
        for side in SIDES:
            x, y, w, h = f[side]
            for j in range(h):
                for i in range(w):
                    n = s.rnd.randint(-6, 6)
                    s.put(x + i, y + j, tuple(v + n for v in shade(shroud, 0.95)) + (alpha - j * 8,))
    s.save('entity', 'wandering_ghost' + ('_captain' if captain else '') + '.png')


for c in (False, True):
    clerk(c)
    ghost(c)
impermanence(False)
impermanence(True)
