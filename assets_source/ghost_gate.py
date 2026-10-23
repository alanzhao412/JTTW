"""The ghost veil of a Gate of Ghosts (Fengdu's, and the side doors of Hell's): a cold soul-fire mist,
teal and ink-blue, swirling slowly upward, half see-through, with brighter wisps (souls) rising through it.
Thirty-two frames of 16x16, stacked, for an animated block texture (its .mcmeta sets the pace); the
pattern wraps in time and in space, so it loops and tiles without a seam.

Run from the repository root: python assets_source/ghost_gate.py"""
import math
import os
from PIL import Image

T = 'src/main/resources/assets/jttw_engine/textures/block/'
NAME = 'ghost_gate'
FRAMES = 32
SIZE = 16

DEEP = (14, 30, 48)
MIST = (40, 120, 132)
GLOW = (120, 230, 222)
WISP = (206, 255, 248)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def field(x, y, f):
    """The mist's density at (x, y) in frame f, 0..1: waves that wrap every 16 pixels and every FRAMES frames."""
    t = 2 * math.pi * f / FRAMES
    u, v = 2 * math.pi * x / SIZE, 2 * math.pi * y / SIZE
    d = (math.sin(v + t * 2 + 0.9 * math.sin(u + t)) * 0.5
         + math.sin(u - v + t) * 0.3
         + math.cos(u * 2 + v + t * 2) * 0.2)
    return 0.5 + 0.5 * d


def wisps(x, y, f):
    """Souls rising: a few bright points drifting up a column each, wrapping round."""
    total = 0.0
    for k, col in enumerate((3, 8, 12)):
        rise = (f * SIZE / FRAMES * (1 + k % 2) + k * 5) % SIZE
        cy = (SIZE - 1 - rise) % SIZE
        sway = 1.2 * math.sin(2 * math.pi * (f / FRAMES + k / 3))
        dx = min(abs(x - (col + sway)), SIZE - abs(x - (col + sway)))
        dy = min(abs(y - cy), SIZE - abs(y - cy))
        total += max(0.0, 1 - math.hypot(dx * 1.4, dy) / 2.2)
    return min(1.0, total)


def frame(f):
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            d = field(x, y, f)
            c = lerp(DEEP, MIST, min(1, d * 1.3)) if d < 0.7 else lerp(MIST, GLOW, (d - 0.7) / 0.3)
            w = wisps(x, y, f)
            c = lerp(c, WISP, w * 0.9)
            alpha = int(150 + 70 * d + 35 * w)
            img.putpixel((x, y), c + (min(255, alpha),))
    return img


def main():
    sheet = Image.new('RGBA', (SIZE, SIZE * FRAMES))
    for f in range(FRAMES):
        sheet.paste(frame(f), (0, f * SIZE))
    os.makedirs(T, exist_ok=True)
    sheet.save(T + NAME + '.png')
    print('wrote', T + NAME + '.png')


if __name__ == '__main__':
    main()
