"""Paints the Yin soldiers' skins (textures/entity/yin_soldier.png, its captain's and its burning
eyes; 128x64) to the layout of HellRenderers.soldierLayer(): dead soldiers of the underworld, grey
of face and hand, in dark lamellar armour gone cold and tarnished (bronze studs, a teal sheen),
a helmet with a neck guard and a red crest, dark trousers and boots; a black halberd with a steel
spike and crescent blade, a red tassel under them. A captain's armour is trimmed in violet and gold.

Run from the repository root: python assets_source/yin_soldier.py"""
from cave_skin import Skin, faces, shade
from hell_skin import HEAD, BODY, ARM, LEG, SIDES


def armour_fill(s, box, base, trim, studs=True):
    """A box of lamellar armour (as hell_guards.py's): rows of plates, trimmed top and bottom, a row of studs."""
    f = faces(*box)
    for side in SIDES:
        x, y, w, h = f[side]
        s.fill(x, y, w, h, base, noise=4)
        if side in ('top', 'bottom'):
            continue
        for j in range(1, h, 2):
            for i in range(w):
                if (i + j // 2) % 3 == 0:
                    s.put(x + i, y + j, shade(base, 1.35))
        for i in range(w):
            s.put(x + i, y, trim)
            s.put(x + i, y + h - 1, trim)
        if studs:
            for i in range(1, w, 3):
                s.put(x + i, y + h // 2, (176, 140, 70))

FACE = (138, 150, 156)
IRON = (46, 52, 60)
CREST = (168, 28, 30)
STEEL, WOOD = (170, 178, 188), (36, 28, 26)


def soldier(captain):
    s = Skin(128, 64, seed=57 + (9 if captain else 0))
    trim = (92, 150, 140) if not captain else (176, 90, 220)
    stud = (176, 140, 70) if not captain else (230, 196, 90)
    # ── the head: a grey dead face, hollow eyes, a thin dark mouth ──────
    f = faces(*HEAD)
    for side in SIDES:
        s.fill(*f[side], shade(FACE, 0.9), noise=5)
    x, y, w, h = f['front']
    s.fill(x, y, w, h, FACE, noise=5)
    for ex in (1, 2, 5, 6):
        s.put(x + ex, y + 3, (26, 30, 34))   # the sockets
        s.put(x + ex, y + 2, shade(FACE, 0.7))
    s.put(x + 3, y + 5, shade(FACE, 0.75))
    s.put(x + 4, y + 5, shade(FACE, 0.75))
    for i in range(2, 6):
        s.put(x + i, y + 6, (60, 40, 44))
    # ── the body: a dark tunic; sleeves to armoured bracers, grey hands ──
    bf = faces(*BODY)
    for side in SIDES:
        s.fill(*bf[side], (38, 34, 40), noise=4)
    af = faces(*ARM)
    for side in SIDES:
        x, y, w, h = af[side]
        s.fill(x, y, w, h, (44, 40, 46), noise=4)
        if side in ('top', 'bottom'):
            continue
        s.fill(x, y + 6, w, 4, IRON, noise=3)
        for i in range(w):
            s.put(x + i, y + 6, trim)
        s.fill(x, y + h - 2, w, 2, FACE, noise=3)
    s.fill(*af['bottom'], FACE, noise=3)
    lf = faces(*LEG)
    for side in SIDES:
        x, y, w, h = lf[side]
        s.fill(x, y, w, h, (30, 28, 34), noise=3)
        if side not in ('top', 'bottom'):
            s.fill(x, y + h - 4, w, 4, (24, 20, 20), noise=2)   # boots
            for i in range(w):
                s.put(x + i, y + h - 4, shade(trim, 0.6))
    # ── the armour: cuirass, skirt, pauldrons, all lamellar ─────────────
    armour_fill(s, (64, 0, 9, 8, 5), IRON, trim)
    x, y, w, h = faces(64, 0, 9, 8, 5)['front']
    # a round mirror plate on the chest (护心镜), bronze
    s.fill(x + 3, y + 2, 3, 3, stud, noise=4)
    s.put(x + 4, y + 3, shade(stud, 1.25))
    armour_fill(s, (64, 13, 9, 6, 5), shade(IRON, 1.12), trim)
    armour_fill(s, (92, 0, 5, 4, 5), IRON, trim, studs=False)
    # ── the helmet: dark iron, a bronze rim; the neck guard; the crest ──
    hf = faces(64, 24, 9, 5, 9)
    for side in SIDES:
        xx, yy, ww, hh = hf[side]
        s.fill(xx, yy, ww, hh, shade(IRON, 1.05), noise=4)
        if side in ('top', 'bottom'):
            continue
        for i in range(ww):
            s.put(xx + i, yy + hh - 1, stud)   # the rim
        for i in range(1, ww, 3):
            s.put(xx + i, yy + 1, shade(IRON, 1.5))
    xx, yy, ww, hh = hf['bottom']
    s.fill(xx, yy, ww, hh, (0, 0, 0, 0), noise=0)   # open below: the face shows
    armour_fill(s, (64, 40, 9, 3, 2), IRON, trim, studs=False)
    cf = faces(100, 24, 1, 4, 6)
    for side in SIDES:
        xx, yy, ww, hh = cf[side]
        for j in range(hh):
            for i in range(ww):
                s.put(xx + i, yy + j, shade(CREST, 0.85 + ((i * 5 + j * 3) % 4) * 0.08))
    # ── the halberd ────────────────────────────────────────────────────
    s.fill(120, 0, 4, 31, WOOD, noise=3)
    s.fill(112, 40, 16, 8, STEEL, noise=8)
    if captain:
        s.fill(112, 40, 16, 2, (230, 200, 120), noise=4)
    s.fill(100, 40, 8, 4, CREST, noise=6)
    s.save('entity', 'yin_soldier' + ('_captain' if captain else '') + '.png')


def eyes():
    s = Skin(128, 64)
    x, y, w, h = faces(*HEAD)['front']
    for ex in (1, 6):
        s.put(x + ex, y + 3, (120, 255, 240))
        s.put(x + ex + (1 if ex < 4 else -1), y + 3, (70, 200, 200))
    s.save('entity', 'yin_soldier_eyes.png')


for captain in (False, True):
    soldier(captain)
eyes()
