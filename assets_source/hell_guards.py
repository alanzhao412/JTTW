"""Paints Ox-Head's and Horse-Face's skins (textures/entity/ox_head.png, horse_face.png, their
captains' and their burning eyes; 128x64) to the layouts of HellRenderers.oxLayer() and
horseLayer(): beast heads on big men in black lacquered armour with gold studs and red cords, bare
hide arms, black trousers and boots; Ox-Head's steel fork and Horse-Face's hooked spear. A captain's
armour is trimmed in gold and violet.

Run from the repository root: python assets_source/hell_guards.py"""
from cave_skin import Skin, faces, shade
from hell_skin import HEAD, BODY, ARM, LEG, SIDES

ARMOUR, LACQUER = (34, 32, 38), (150, 26, 30)
STEEL, WOOD = (170, 176, 186), (52, 30, 24)


def armour_fill(s, box, base, trim, studs=True):
    f = faces(*box)
    for side in SIDES:
        x, y, w, h = f[side]
        s.fill(x, y, w, h, base, noise=4)
        if side in ('top', 'bottom'):
            continue
        # lamellae: rows of plates
        for j in range(1, h, 2):
            for i in range(w):
                if (i + j // 2) % 3 == 0:
                    s.put(x + i, y + j, shade(base, 1.35))
        for i in range(w):
            s.put(x + i, y, trim)
            s.put(x + i, y + h - 1, trim)
        if studs:
            for i in range(1, w, 3):
                s.put(x + i, y + h // 2, (226, 180, 70))


def guard(name, hide, muzzle_col, captain, horse):
    s = Skin(128, 64, seed=len(name) + (7 if captain else 0))
    trim = (200, 160, 60) if not captain else (176, 90, 220)
    # ── the head: hide, eyes up above the muzzle ────────────────────────
    f = faces(*HEAD)
    for side in SIDES:
        s.fill(*f[side], hide, noise=6)
    x, y, w, h = f['front']
    for (ex, ey) in ((1, 2), (6, 2)) if not horse else ((1, 1), (6, 1)):
        s.put(x + ex, y + ey, (20, 10, 10))
        s.put(x + ex + (1 if ex < 4 else -1), y + ey, (60, 20, 15))
    # brow ridge, darker
    for i in range(8):
        s.put(x + i, y + (1 if not horse else 0), shade(hide, 0.7))
    if horse:
        # a pale blaze down the face
        for j in range(2, 8):
            s.put(x + 3, y + j, shade(muzzle_col, 1.25))
            s.put(x + 4, y + j, shade(muzzle_col, 1.2))
    # ── the muzzle ─────────────────────────────────────────────────────
    mbox = (64, 24, 5, 6, 6) if horse else (64, 24, 6, 4, 3)
    mf = faces(*mbox)
    for side in SIDES:
        s.fill(*mf[side], muzzle_col, noise=5)
    x, y, w, h = mf['front']
    for nx in (1, w - 2):
        s.put(x + nx, y + h // 2, (25, 15, 12))
        s.put(x + nx, y + h // 2 + 1, (40, 25, 20))
    for i in range(w):
        s.put(x + i, y + h - 1, shade(muzzle_col, 0.7))   # the mouth's line
    if horse:
        # the mane, dark, and the ears
        mn = faces(64, 40, 2, 3, 8)
        for side in SIDES:
            xx, yy, ww, hh = mn[side]
            for j in range(hh):
                for i in range(ww):
                    s.put(xx + i, yy + j, shade((40, 28, 22), 0.85 + ((i * 7 + j * 3) % 5) * 0.08))
        s.fill(88, 36, 5, 4, shade(hide, 0.85), noise=4)
    else:
        # horns: bone, darker at the root; ears of hide
        s.fill(84, 24, 12, 4, (214, 204, 172), noise=6, grad=-0.1)
        s.fill(84, 28, 6, 6, (232, 224, 196), noise=5)
        s.put(85, 28, (250, 246, 230))
        s.fill(96, 24, 8, 3, shade(hide, 0.9), noise=4)
        # a gold ring through the nose
        x, y, w, h = mf['front']
        s.put(x + 2, y + h - 2, (230, 190, 70))
        s.put(x + 3, y + h - 2, (230, 190, 70))
    # ── the body: a dark tunic under the armour; bare arms of hide with bracers ──
    bf = faces(*BODY)
    for side in SIDES:
        s.fill(*bf[side], (60, 24, 26), noise=4)
    af = faces(*ARM)
    for side in SIDES:
        x, y, w, h = af[side]
        s.fill(x, y, w, h, hide, noise=6)
        if side not in ('top', 'bottom'):
            s.fill(x, y + 7, w, 3, ARMOUR, noise=3)
            for i in range(w):
                s.put(x + i, y + 7, trim)
    lf = faces(*LEG)
    for side in SIDES:
        x, y, w, h = lf[side]
        s.fill(x, y, w, h, (28, 26, 30), noise=3)
        if side not in ('top', 'bottom'):
            s.fill(x, y + h - 4, w, 4, (55, 38, 30), noise=3)   # boots
    # ── the armour: cuirass, armoured skirt, pauldrons ─────────────────
    armour_fill(s, (64, 0, 9, 8, 5), ARMOUR, trim)
    x, y, w, h = faces(64, 0, 9, 8, 5)['front']
    # a gold beast-face boss on the chest, red cords crossing
    s.fill(x + 3, y + 2, 3, 3, (220, 176, 64), noise=4)
    s.put(x + 4, y + 3, (120, 20, 20))
    for j in range(h):
        s.put(x + j * 8 // h, y + j, LACQUER)
    armour_fill(s, (64, 13, 9, 6, 5), shade(ARMOUR, 1.1), trim)
    x, y, w, h = faces(64, 13, 9, 6, 5)['front']
    s.fill(x + 3, y, 3, h, LACQUER, noise=4)
    armour_fill(s, (92, 0, 5, 4, 5), ARMOUR, trim, studs=False)
    # ── the weapons: a black shaft, steel ──────────────────────────────
    s.fill(120, 0, 4, 31, WOOD, noise=4)
    for j in range(0, 31, 6):
        s.fill(120, j, 4, 1, (180, 30, 30), noise=2)   # red bands
    s.fill(112, 40, 16, 8, STEEL, noise=8)
    if captain:
        s.fill(112, 40, 16, 2, (230, 200, 120), noise=4)
    s.save('entity', name + ('_captain' if captain else '') + '.png')


def eyes(name, horse, colour):
    s = Skin(128, 64)
    x, y, w, h = faces(*HEAD)['front']
    for (ex, ey) in ((1, 2), (6, 2)) if not horse else ((1, 1), (6, 1)):
        s.put(x + ex, y + ey, colour)
        s.put(x + ex + (1 if ex < 4 else -1), y + ey, shade(colour, 0.7))
    s.save('entity', name + '_eyes.png')


OX_HIDE, OX_MUZZLE = (84, 52, 38), (128, 92, 70)
HORSE_HIDE, HORSE_MUZZLE = (112, 86, 64), (140, 112, 90)
for captain in (False, True):
    guard('ox_head', OX_HIDE, OX_MUZZLE, captain, False)
    guard('horse_face', HORSE_HIDE, HORSE_MUZZLE, captain, True)
eyes('ox_head', False, (255, 70, 40))
eyes('horse_face', True, (255, 150, 40))
