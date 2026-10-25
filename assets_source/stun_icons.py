"""The icons of the two full stops (see Stun.java): Stunned, three gold stars wheeling round a dazed
head's crown; Pinned, an iron stake driven through a band into the ground. 18 x 18, like vanilla's.

Run from the repository root: python assets_source/stun_icons.py"""
from PIL import Image

T = 'src/main/resources/assets/jttw_engine/textures/mob_effect/'
CLEAR = (0, 0, 0, 0)
OUT = (36, 28, 20, 255)
GOLD = (248, 214, 72, 255)
GOLD_D = (196, 150, 40, 255)
WHITE = (255, 250, 220, 255)
IRON = (168, 178, 196, 255)
IRON_D = (104, 112, 130, 255)
IRON_L = (220, 226, 236, 255)
EARTH = (122, 84, 50, 255)
EARTH_D = (86, 58, 34, 255)
ROPE = (196, 64, 52, 255)
ROPE_D = (140, 40, 34, 255)


def outline(img):
    """A dark rim round every filled pixel, so the icon reads on any background."""
    w, h = img.size
    src = img.copy()
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src.getpixel((nx, ny))[3] and src.getpixel((nx, ny)) != OUT:
                    img.putpixel((x, y), OUT)
                    break
    return img


def star(img, cx, cy):
    for x, y in ((0, -2), (0, -1), (-2, 0), (-1, 0), (0, 0), (1, 0), (2, 0), (0, 1), (0, 2)):
        img.putpixel((cx + x, cy + y), GOLD)
    for x, y in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        img.putpixel((cx + x, cy + y), GOLD_D)
    img.putpixel((cx, cy), WHITE)


def stunned():
    img = Image.new('RGBA', (18, 18), CLEAR)
    # the ring they wheel on, a dashed ellipse
    for x in range(3, 15):
        if x % 2 == 0:
            img.putpixel((x, 12), GOLD_D)
    for x in (2, 15):
        img.putpixel((x, 11), GOLD_D)
    star(img, 4, 6)
    star(img, 13, 5)
    star(img, 9, 11)
    return outline(img)


def pinned():
    img = Image.new('RGBA', (18, 18), CLEAR)
    # the ground
    for x in range(1, 17):
        img.putpixel((x, 14), EARTH)
        img.putpixel((x, 15), EARTH_D)
    # the band held under the stake
    for x in range(3, 15):
        img.putpixel((x, 12), ROPE)
        img.putpixel((x, 13), ROPE_D)
    # the stake: a broad head, a shaft, a point in the earth
    for x in range(6, 12):
        img.putpixel((x, 2), IRON_L)
        img.putpixel((x, 3), IRON)
    img.putpixel((6, 3), IRON_D)
    img.putpixel((11, 3), IRON_D)
    for y in range(4, 15):
        img.putpixel((8, y), IRON_L)
        img.putpixel((9, y), IRON_D)
    img.putpixel((8, 15), IRON_D)
    return outline(img)


if __name__ == '__main__':
    stunned().save(T + 'stunned.png')
    pinned().save(T + 'pinned.png')
