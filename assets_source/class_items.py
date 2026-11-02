"""The class identities' icons: the Divine Beasts' twenty evolution offerings (the Vermilion Bird's
plumes, the Azure Dragon's pearls, the White Tiger's fang totems, the Black Tortoise's shells, each
richer by its evolution), and the Heavenly's own crafts (the Five-Thunder and Heaven's Net
talismans, the Nine-Turn Golden Elixir).

Run from the repository root: python assets_source/class_items.py"""
import math
from PIL import Image

T = 'src/main/resources/assets/jttw_engine/textures/item/'
CLEAR = (0, 0, 0, 0)
OUT = (26, 18, 22, 255)


def rgba(c, k=1.0, a=255):
    return tuple(max(0, min(255, int(round(v * k)))) for v in c[:3]) + (a,)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def icon():
    return Image.new('RGBA', (16, 16), CLEAR)


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c if len(c) == 4 else rgba(c))


def outline(img, colour=OUT):
    """A dark rim round the shape, and a softer one on its lower right, as vanilla's items have."""
    src = img.copy()
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3] and src.getpixel((nx, ny))[:3] != colour[:3]:
                    img.putpixel((x, y), colour)
                    break


def ramp(stops, t):
    """A colour along a list of stops, t from 0 to 1."""
    t = max(0.0, min(1.0, t)) * (len(stops) - 1)
    i = min(int(t), len(stops) - 2)
    return mix(stops[i], stops[i + 1], t - i)


# ── The Vermilion Bird: a plume laid on the diagonal, its vane in the colours of its fire ──
def plume(name, vane, quill, tip=None, sparks=(), halo=None):
    img = icon()
    if halo:  # a ring of light behind it
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                if 6.3 < d < 7.5:
                    put(img, x, y, rgba(halo, 0.9 + 0.2 * math.sin(math.atan2(y - 7.5, x - 7.5) * 6)))
    # the shaft runs from the lower left to the upper right; u along it, v across it
    ux, uy = 1 / math.sqrt(2), -1 / math.sqrt(2)
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - 1.5, y + 0.5 - 14.5
            t = (px * ux + py * uy) / 18.0          # 0 at the quill's foot, 1 at the tip
            v = px * -uy + py * ux                  # across: <0 upper left, >0 lower right
            if t < 0 or t > 1:
                continue
            if t < 0.22:                            # the bare quill
                if abs(v) < 0.55:
                    put(img, x, y, rgba(quill, 1.1))
                continue
            k = (t - 0.22) / 0.78
            width = 0.6 + 3.6 * math.sin(math.pi * min(1.0, k * 1.08)) ** 0.8
            serr = 0.6 * (1 if (x + y) % 3 == 0 else 0)   # the barbs part at the edge
            if abs(v) < 0.55:
                put(img, x, y, rgba(mix(quill, (255, 255, 255), 0.2), 1.0))
            elif abs(v) < width - serr:
                c = ramp(vane, k * 0.8 + abs(v) / width * 0.25)
                put(img, x, y, rgba(c, 1.12 if v < 0 else 0.84))
    if tip:
        for (x, y), c in tip:
            put(img, x, y, rgba(c))
    for (x, y), c in sparks:
        put(img, x, y, rgba(c))
    outline(img)
    img.save(T + name + '.png')


EMBER = [(120, 30, 18), (200, 70, 24), (245, 140, 40), (255, 206, 90)]
plume('kindled_plume', EMBER, (90, 60, 40), sparks=[((12, 6), (255, 220, 120)), ((10, 2), (255, 160, 60))])
plume('wisp_fire_plume', [(40, 60, 120), (90, 150, 220), (190, 230, 255), (250, 250, 255)], (120, 130, 150),
      sparks=[((13, 6), (180, 240, 255)), ((9, 2), (120, 200, 255)), ((14, 9), (230, 250, 255))])
plume('candle_flame_plume', [(150, 40, 30), (230, 90, 40), (250, 180, 70), (255, 245, 200)], (220, 200, 160),
      tip=[((14, 1), (255, 250, 220)), ((13, 0), (255, 220, 120)), ((15, 0), (255, 180, 60))],
      sparks=[((12, 7), (255, 240, 180))])
plume('samadhi_plume', [(150, 10, 20), (230, 40, 30), (255, 150, 30), (255, 250, 220)], (255, 220, 120),
      sparks=[((13, 7), (255, 255, 230)), ((10, 1), (255, 240, 160)), ((15, 4), (255, 120, 40)), ((5, 7), (255, 200, 80))])
plume('sovereign_sun_plume', [(170, 20, 30), (240, 90, 30), (255, 200, 60), (255, 255, 235)], (255, 230, 140),
      sparks=[((13, 7), (255, 255, 240)), ((10, 1), (255, 245, 170))], halo=(255, 210, 90))


# ── The Azure Dragon: a pearl, lit from the upper left, in its own setting ──
def pearl(name, dark, mid, light, ring=None, ring_dark=None, aura=None, inner=None, drops=()):
    img = icon()
    if aura:  # a flame-like aura licking up round it (the dragon's flaming pearl)
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, (y - 8.5) * 0.9)
                flick = 0.9 * math.sin(math.atan2(y - 8.5, x - 7.5) * 5)
                if 5.6 < d < 7.0 + flick * 0.6 and y < 13:
                    put(img, x, y, rgba(aura, 1.0 - (d - 5.6) * 0.18))
    if ring:  # a setting: a band round its waist, in front
        for x in range(2, 14):
            for y in (11, 12):
                put(img, x, y, rgba(ring if (x + y) % 3 else ring_dark or ring, 1.05 if x < 8 else 0.85))
        for x in (3, 7, 12):
            put(img, x, 13, rgba(ring_dark or ring, 0.8))
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.0
            d = math.hypot(dx, dy)
            if d >= 5.3:
                continue
            light_t = max(0.0, 1 - math.hypot(dx + 2.2, dy + 2.2) / 7.0)
            rim = max(0.0, (d - 3.8) / 1.5)
            c = mix(mix(mid, light, light_t ** 1.4), dark, rim * (0.6 if dx + dy > 0 else 0.25))
            if inner and math.hypot(dx - 0.5, dy - 0.5) < 2.0 and (x + y) % 2 == 0:
                c = mix(c, inner, 0.5)
            put(img, x, y, rgba(c))
    for p in ((5, 4), (6, 4), (5, 5)):
        put(img, p[0], p[1], (255, 255, 255, 255))
    put(img, 9, 10, rgba(light, 1.0))  # the far glint
    for (x, y), c in drops:
        put(img, x, y, rgba(c))
    outline(img)
    img.save(T + name + '.png')


pearl('rain_pearl', (20, 50, 120), (60, 110, 200), (180, 215, 255), ring=(200, 205, 215), ring_dark=(120, 125, 140),
      drops=[((1, 3), (150, 200, 255)), ((14, 2), (150, 200, 255)), ((14, 5), (110, 170, 240))])
pearl('water_curtain_pearl', (10, 80, 90), (40, 170, 170), (190, 250, 240), ring=(150, 110, 70), ring_dark=(100, 70, 45),
      drops=[((2, 1), (170, 240, 240)), ((2, 3), (120, 210, 220)), ((13, 1), (170, 240, 240))])
pearl('four_seas_pearl', (10, 70, 70), (60, 160, 140), (210, 255, 235), ring=(90, 170, 160), ring_dark=(40, 110, 105),
      inner=(255, 255, 255))
pearl('naihe_pearl', (40, 10, 40), (110, 40, 110), (230, 170, 220), ring=(60, 60, 70), ring_dark=(30, 30, 36),
      inner=(170, 255, 200), drops=[((1, 6), (140, 255, 190)), ((14, 4), (140, 255, 190))])
pearl('dragon_kings_pearl', (20, 60, 140), (70, 140, 230), (230, 245, 255), aura=(255, 190, 60), inner=(255, 230, 140))


# ── The White Tiger: a fang, carved and bound on a cord, with what of the beast it carries ──
def totem(name, fang, fang_dark, cord, cord_dark, charm=None, tufts=(), whiskers=None):
    img = icon()
    # the cord: a loop over the top
    for a in range(0, 360, 12):
        x = int(round(7.5 + 4.2 * math.cos(math.radians(a))))
        y = int(round(4.0 + 3.0 * math.sin(math.radians(a))))
        if y <= 5:
            put(img, x, y, rgba(cord if a % 24 else cord_dark))
    # the binding: wraps round the fang's root
    for x in range(5, 11):
        for y in (6, 7):
            put(img, x, y, rgba(cord if (x + y) % 2 else cord_dark, 1.05 if x < 8 else 0.9))
    # the fang: broad at the root, curving down to a point
    for y in range(8, 16):
        t = (y - 8) / 7.0
        half = 2.6 * (1 - t ** 1.3)
        cx = 7.5 + 1.6 * t * t  # curving
        for x in range(16):
            if abs(x - cx) <= half + 0.2:
                edge = (x - cx) / max(half, 0.5)
                c = mix(fang, fang_dark, max(0.0, edge) * 0.8 + t * 0.15)
                if edge < -0.4:
                    c = mix(c, (255, 255, 255), 0.25)
                put(img, x, y, rgba(c))
    put(img, 6, 9, rgba(fang, 1.15))
    for (x, y), c in tufts:
        put(img, x, y, rgba(c))
    if charm:  # a bead on the cord
        for p in ((7, 1), (8, 1)):
            put(img, p[0], p[1], rgba(charm, 1.1))
        put(img, 7, 2, rgba(charm, 0.85))
        put(img, 8, 2, rgba(charm, 0.75))
    if whiskers:
        for x, y in ((1, 9), (2, 9), (3, 10), (14, 9), (13, 9), (12, 10)):
            put(img, x, y, rgba(whiskers))
    outline(img)
    img.save(T + name + '.png')


totem('fang_and_pelt_totem', (232, 222, 196), (170, 150, 120), (130, 90, 55), (90, 60, 35),
      tufts=[((4, 7), (150, 110, 70)), ((11, 7), (150, 110, 70)), ((4, 8), (120, 85, 50))])
totem('bone_fang_totem', (240, 235, 220), (175, 165, 150), (190, 40, 40), (120, 25, 25), charm=(220, 210, 190))
totem('moonlit_whisker_totem', (215, 230, 245), (130, 150, 185), (60, 70, 120), (35, 40, 80), charm=(200, 220, 255),
      whiskers=(235, 240, 255))
totem('ox_and_horse_totem', (70, 55, 50), (35, 25, 25), (170, 30, 30), (100, 15, 15), charm=(210, 170, 70),
      tufts=[((4, 7), (110, 75, 40)), ((11, 7), (110, 75, 40))])
totem('western_mane_totem', (250, 248, 240), (190, 185, 175), (230, 180, 60), (160, 110, 30), charm=(255, 230, 120),
      tufts=[((3, 6), (250, 250, 250)), ((12, 6), (250, 250, 250)), ((3, 7), (220, 220, 225)), ((12, 7), (220, 220, 225)),
             ((4, 8), (240, 240, 245)), ((11, 8), (240, 240, 245))])


# ── The Black Tortoise: a shell seen from above, its plates in the earth it was made from ──
def shell(name, plate, seam, rim, glow=None, mark=None, spiral=None):
    img = icon()
    for y in range(16):
        for x in range(16):
            dx, dy = (x + 0.5 - 8) / 6.7, (y + 0.5 - 8.2) / 6.1
            d = math.hypot(dx, dy)
            if d >= 1.0:
                continue
            ang = math.degrees(math.atan2(dy, dx)) % 360
            k = 1.18 - (dx + dy) * 0.2 - d * 0.18
            if d > 0.8:                                     # the marginal plates round the rim
                c = seam if (ang % 30) < 7 else rim
            else:
                vertebral = abs(dx) < 0.3                   # the spine's plates
                ring = 0.36 < d < 0.48 and not vertebral
                radial = d > 0.45 and (ang % 60) < 8
                cross = vertebral and any(abs(dy - c0) < 0.08 for c0 in (-0.45, 0.0, 0.45))
                edge = vertebral and abs(abs(dx) - 0.28) < 0.06
                c = seam if (ring or radial or cross or edge) else plate
                if c is seam and glow:
                    c, k = glow, 1.0
            put(img, x, y, rgba(c, k))
    if mark:
        for (x, y), c in mark:
            put(img, x, y, rgba(c))
    if spiral:
        for a in range(0, 600, 20):
            r = 0.3 + a / 600 * 2.4
            put(img, int(round(7.6 + r * math.cos(math.radians(a)))), int(round(8 + r * math.sin(math.radians(a)))), rgba(spiral))
    # head, legs, tail peeking out
    for p in ((7, 0), (8, 0), (1, 3), (14, 3), (1, 13), (14, 13), (8, 15)):
        put(img, p[0], p[1], rgba(mix(plate, rim, 0.5), 0.95))
    outline(img)
    img.save(T + name + '.png')


shell('deepstone_shell', (78, 80, 92), (36, 36, 44), (55, 55, 64), mark=[((7, 8), (180, 200, 150)), ((8, 8), (150, 175, 120))])
shell('ridge_bone_shell', (225, 218, 195), (140, 125, 100), (190, 180, 155))
shell('tide_worn_shell', (60, 140, 135), (25, 70, 70), (40, 105, 100), spiral=(235, 220, 200))
shell('furnace_brick_shell', (150, 60, 40), (60, 25, 20), (110, 45, 30), glow=(255, 150, 50))
shell('world_turtle_shell', (30, 50, 45), (210, 170, 70), (20, 32, 30),
      mark=[((7, 6), (120, 170, 120)), ((8, 6), (120, 170, 120)), ((6, 7), (90, 140, 95)), ((7, 7), (150, 200, 150)),
            ((8, 7), (150, 200, 150)), ((9, 7), (90, 140, 95)), ((7, 8), (220, 230, 235))])


# ── The Heavenly's talismans: yellow paper, a seal in vermilion, and the art they call ──
def talisman(name, paper, paper_dark, art):
    img = icon()
    for y in range(1, 16):
        for x in range(4, 12):
            k = 1.08 if x < 6 else (0.88 if x > 9 else 1.0)
            put(img, x, y, rgba(paper if (y % 5) else paper_dark, k))
    for x in range(4, 12):  # the torn foot
        if x % 2:
            put(img, x, 15, CLEAR)
    for (x, y), c in art:
        put(img, x, y, rgba(c))
    outline(img)
    img.save(T + name + '.png')


RED, RED_D = (200, 30, 30), (140, 18, 18)
BOLT = (120, 190, 255)
talisman('five_thunder_talisman', (238, 205, 90), (215, 180, 70),
         [((7, 2), RED), ((8, 2), RED), ((6, 3), RED), ((9, 3), RED), ((7, 3), RED_D), ((8, 3), RED_D),   # the seal's crown
          ((8, 5), BOLT), ((7, 6), BOLT), ((7, 7), (230, 245, 255)), ((8, 8), BOLT), ((9, 8), BOLT), ((8, 9), BOLT),
          ((7, 10), (230, 245, 255)), ((7, 11), BOLT), ((6, 12), BOLT),                                       # the thunder
          ((9, 12), RED), ((9, 13), RED_D), ((10, 13), RED)])
GOLD_T = (225, 180, 60)
talisman('heavens_net_talisman', (245, 232, 170), (225, 210, 150),
         [((x, y), GOLD_T) for y in range(4, 12) for x in range(5, 11) if (x + y) % 3 == 0 or (x - y) % 3 == 0] +
         [((7, 2), RED), ((8, 2), RED), ((7, 13), RED), ((8, 13), RED_D)])

# ── the Nine-Turn Golden Elixir: a golden pill glowing on a jade dish ──
img = icon()
for y in range(16):  # its light, in rays
    for x in range(16):
        dx, dy = x - 7.5, y - 7.0
        d = math.hypot(dx, dy)
        ray = abs(math.sin(math.atan2(dy, dx) * 4)) > 0.92
        if 4.0 < d < 6.8 and ray and y < 11:
            put(img, x, y, rgba((255, 225, 120), 1.0 - (d - 4) * 0.12))
for y in range(10, 14):  # the dish
    for x in range(1, 15):
        if abs(x - 7.5) <= 6.5 - (y - 10) * 1.5:
            put(img, x, y, rgba((60, 160, 115), 1.18 - (y - 10) * 0.14 - (0.1 if x > 9 else 0)))
for x in range(2, 14):
    put(img, x, 10, rgba((160, 225, 190)))
for y in range(16):  # the pill
    for x in range(16):
        dx, dy = x - 7.5, y - 7.0
        d = math.hypot(dx, dy)
        if d < 3.7:
            t = max(0.0, 1 - math.hypot(dx + 1.3, dy + 1.3) / 4.6)
            put(img, x, y, rgba(mix((180, 110, 15), (255, 238, 150), t)))
put(img, 6, 5, (255, 255, 240, 255))
put(img, 6, 6, (255, 250, 210, 255))
put(img, 5, 6, (255, 246, 200, 255))
outline(img)
img.save(T + 'nine_turn_elixir.png')
