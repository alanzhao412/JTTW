"""The Ruyi Jingu Bang's qi wave: a strip of flowing energy, drawn along the wave's crescent (u runs
along the arc and wraps, so the renderer can scroll it; v runs across, edge to edge). It is white
and grey only, the renderer tinting it gold: bright filaments of qi streaming lengthwise over a
softer glow, a little denser toward the middle.

Run from the repository root: python assets_source/qi_wave.py"""
import math
import random
from PIL import Image

T = 'src/main/resources/assets/jttw_engine/textures/entity/'
W, H = 128, 32


def main():
    rnd = random.Random(7)
    # filaments: each a thin bright line across the strip at its own height, waving gently, and
    # brightening and dimming along its length; whole waves along u, so the strip wraps
    strands = []
    for _ in range(14):
        strands.append((rnd.uniform(0.12, 0.88), rnd.uniform(0.01, 0.05), rnd.randint(1, 3),
                        rnd.uniform(0, math.tau), rnd.randint(1, 4), rnd.uniform(0, math.tau),
                        rnd.uniform(0.010, 0.024), rnd.uniform(0.25, 0.6)))
    img = Image.new('RGBA', (W, H))
    for y in range(H):
        v = (y + 0.5) / H
        across = math.sin(v * math.pi)  # 0 at the edges, 1 at the middle
        for x in range(W):
            u = x / W
            lum = 0.84 + 0.08 * across
            for centre, sway, waves, phase, beats, beat_phase, width, strength in strands:
                c = centre + sway * math.sin(waves * math.tau * u + phase)
                d = (v - c) / width
                along = 0.5 + 0.5 * math.sin(beats * math.tau * u + beat_phase)
                lum += strength * along * math.exp(-d * d)
            # a faint grain, so it never looks flat
            lum += rnd.uniform(-0.03, 0.03)
            g = max(0, min(255, int(255 * min(1.0, lum))))
            a = max(0, min(255, int(255 * (0.88 + 0.12 * min(1.0, lum)))))
            img.putpixel((x, y), (g, g, g, a))
    img.save(T + 'qi_wave.png')


if __name__ == '__main__':
    main()
