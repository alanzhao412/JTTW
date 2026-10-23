"""Paints the cloud crane (textures/entity/cloud_crane(_captain).png, 64x64) to the layout of
WuzhuangRenderers.craneLayer(): a white body, a black neck below a white throat and a white head,
the red crown (丹顶) of the red-crowned crane, a long olive-grey beak, white wings whose outer
flight feathers are black, a white tail edged black, grey legs. The captain, an elder crane, has
a deeper crown and golden-tinged plumage.

Run from the repository root: python assets_source/wuzhuang_crane.py"""
from cave_skin import Skin, faces, shade

SIDES = ('top', 'bottom', 'right', 'front', 'left', 'back')


def paint(s, box, colour, noise=5):
    f = faces(*box)
    for side, k in (('top', 1.06), ('bottom', 0.8), ('right', 0.92), ('front', 1.0), ('left', 0.92), ('back', 0.95)):
        s.fill(*f[side], colour, k=k, noise=noise)


def crane(captain):
    s = Skin(64, 64, seed=5 + captain)
    white = (244, 242, 236) if not captain else (246, 236, 206)
    black = (26, 26, 30)
    red = (210, 30, 34) if not captain else (230, 20, 30)
    # the body: white, a little grey under
    paint(s, (0, 0, 7, 7, 14), white, noise=6)
    x, y, w, h = faces(0, 0, 7, 7, 14)['bottom']
    s.fill(x, y, w, h, shade(white, 0.82), noise=5)
    # the neck: black, the upper neck black below a white nape
    paint(s, (0, 22, 2, 8, 2), black, noise=4)
    paint(s, (8, 22, 2, 6, 2), black, noise=4)
    f = faces(8, 22, 2, 6, 2)
    for side in ('front', 'back', 'right', 'left'):
        x, y, w, h = f[side]
        s.fill(x, y, w, 2, white, noise=4)
    # the head: white, black at the face, the red crown
    paint(s, (16, 22, 3, 3, 4), white, noise=4)
    for side in ('front', 'right', 'left'):
        x, y, w, h = faces(16, 22, 3, 3, 4)[side]
        s.fill(x, y + 1, w, h - 1, black, noise=3)
    for side in ('right', 'left'):
        x, y, w, h = faces(16, 22, 3, 3, 4)[side]
        s.put(x + 1, y + 1, (240, 200, 60))
    paint(s, (30, 22, 2, 1, 2), red, noise=6)
    paint(s, (36, 22, 1, 1, 5), (120, 120, 96), noise=5)
    # the wings: white coverts, black flight feathers at the outer half and trailing edge
    for v, outward_left in ((32, True), (44, True)):
        f = faces(0, v, 14, 1, 10)
        for side in SIDES:
            x, y, w, h = f[side]
            s.fill(x, y, w, h, white, noise=5)
        for side in ('top', 'bottom'):
            x, y, w, h = f[side]
            for j in range(h):
                for i in range(w):
                    outer = (i < w // 2) if outward_left else (i >= w - w // 2)
                    trailing = j >= h - 3
                    if outer and trailing or (outer and j >= h - 5 and (i + j) % 2 == 0):
                        s.put(x + i, y + j, shade(black, 1.0 + (i % 3) * 0.1))
                    elif trailing and (i % 3 == 0):
                        s.put(x + i, y + j, shade(black, 1.3))
    # the tail: white edged black
    paint(s, (42, 0, 5, 2, 5), white, noise=5)
    x, y, w, h = faces(42, 0, 5, 2, 5)['top']
    s.fill(x, y + h - 2, w, 2, black, noise=3)
    # legs and feet: grey
    paint(s, (50, 32, 1, 11, 1), (90, 90, 96), noise=4)
    paint(s, (54, 44, 2, 1, 3), (80, 80, 86), noise=4)
    s.save('entity', 'cloud_crane' + ('_captain' if captain else '') + '.png')


crane(False)
crane(True)
