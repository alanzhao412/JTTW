"""Shared painting for the Bottomless Cave's skins (mouse_spirit.py, flower_maid.py,
lady_earth_flow.py, cave_items.py): a canvas, mottled fills, and a box's six faces in the standard
box-UV places, so each script paints straight onto the boxes its renderer's layer() declares.

Box UV, for a box w x h x d at texture offset (u, v):
    top (u+d, v, w, d)        bottom (u+d+w, v, w, d)
    right (u, v+d, d, h)      front (u+d, v+d, w, h)    left (u+d+w, v+d, d, h)    back (u+2d+w, v+d, w, h)
"""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'jttw_engine', 'textures')


class Skin:
    def __init__(self, w, h, seed=1):
        self.img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        self.rnd = random.Random(seed)

    def put(self, x, y, c):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            self.img.putpixel((x, y), tuple(int(max(0, min(255, v))) for v in c[:3]) + ((c[3],) if len(c) > 3 else (255,)))

    def fill(self, x, y, w, h, c, k=1.0, noise=6, grad=0.0, to=None):
        """A rectangle of colour c at brightness k, mottled, darker downward by grad, or blending to `to`."""
        for j in range(h):
            t = j / max(1, h - 1)
            base = mix(c, to, t) if to else c
            g = k * (1 - grad * t)
            for i in range(w):
                n = self.rnd.randint(-noise, noise)
                self.put(x + i, y + j, tuple(v * g + n for v in base[:3]) + ((base[3],) if len(base) > 3 else ()))

    def box(self, u, v, w, h, d, c, **kw):
        """All six faces of a box in one colour, shaded as if lit from above."""
        f = faces(u, v, w, h, d)
        self.fill(*f['top'], c, k=1.08, **kw)
        self.fill(*f['bottom'], c, k=0.7, **kw)
        for side, k in (('right', 0.86), ('front', 1.0), ('left', 0.86), ('back', 0.9)):
            self.fill(*f[side], c, k=k, **kw)

    def save(self, *path):
        p = os.path.join(ROOT, *path)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        self.img.save(p)
        print('wrote', os.path.normpath(p))


def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def shade(c, k):
    return tuple(v * k for v in c[:3])
