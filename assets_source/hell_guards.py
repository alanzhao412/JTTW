"""Ox-Head (牛头) and Horse-Face (马面), the underworld's two guards, who fetch the dead to judgement. As
king_yama.py: each one's model and skins come from one list of parts (beastgen): the model is written
into HellRenderers' oxLayer() and horseLayer() between their GENERATED markers, the skins painted to
the same box-UV layout at twice the model's resolution, with a third skin, their burning eyes, drawn
full-bright over them (ox_head_eyes.png, horse_face_eyes.png). A captain wears the same armour laced
in violet, under a violet cloak (ox_head_captain.png, horse_face_captain.png).

A matched pair, hell's enforcers, half again a man's height: the same armour on both, black lacquered
lamellae edged and studded in gold and laced in red: a breastplate with two round mirror plates (护心镜),
a lamellar skirt over the belly, shoulder guards in three tiers each bitten by a gold beast's head
(吞口), a red belt with a gold demon's face for its buckle, tassets before, behind and at the hips over
a red apron, red sash ends at the hip, a gold-rimmed knee guard and greave over each shin, bracers;
a tattered red cloak down the back; bare hide arms and fists, and the beast's own legs ending in
hooves. Each with a black headband and its gold plaque.

Horse-Face: leaner, a dark bay horse's head on a long neck: a long face with a pale blaze, a soft dark
nose with flared nostrils, the lips drawn back off his teeth, ears laid back, red eyes under a scowling
brow; a black mane, a forelock, and the mane flowing down his neck and back over the cloak. In his
right hand a halberd (a spear-point, a crescent blade, a back hook, a red tassel); in his left the
chain and soul-hook he hauls the dead in with.

Ox-Head: broader, a black-brown ox's head on a thick, humped neck: a heavy brow under a curly tuft, a
broad grey snout, a gold ring through the nose, red eyes; long ivory horns, ringed and black-tipped,
curving out, up and forward from gold bands. In his right hand a steel trident; at his hip a chain and
shackle.

Run from the repository root: python assets_source/hell_guards.py"""
import math

import beastgen as bg
from beastgen import Painter, emit
from fire_court import part, mirrored
from sea_court import aim
from king_yama import paint, point, lock, ragged, circle, mixc, k, noisy, BLUE_BLACK, BROWN_BLACK

bg.UNITS = 256
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/HellRenderers.java'
OUT = 'src/main/resources/assets/jttw_engine/textures/entity/'

# How much larger than his pixels each is drawn (HellRenderers.OxRenderer.SIZE, HorseRenderer.SIZE)
OX_SIZE, HORSE_SIZE = 0.78, 0.78

BLACK = (30, 26, 32)
BLACK_SHEEN = (66, 58, 70)
GOLD = (230, 182, 66)
GOLD_DEEP = (150, 100, 34)
GOLD_LIGHT = (255, 230, 140)
IVORY = (236, 226, 200)
STEEL = (178, 184, 196)
STEEL_DARK = (96, 100, 112)
IRON = (70, 68, 76)
IRON_DARK = (32, 30, 36)
CLOTH = (38, 30, 36)
EMBER = (200, 34, 18)
EMBER_HOT = (255, 150, 60)

# the lacing and the cloak: red for a guard, violet for a captain
COLOURS = {
    False: dict(lace=(176, 32, 30), lace_deep=(96, 14, 18), cloak=(132, 22, 26), cloak_deep=(62, 8, 14), trim=GOLD),
    True: dict(lace=(150, 72, 206), lace_deep=(78, 30, 116), cloak=(92, 36, 128), cloak_deep=(40, 12, 62), trim=GOLD_LIGHT),
}

# hides: (shadow, body, light)
OX_HIDE = ((30, 20, 18), (74, 50, 40), (132, 98, 76))
OX_MUZZLE = ((70, 60, 56), (124, 108, 96), (164, 150, 136))
HORSE_HIDE = ((54, 22, 12), (112, 52, 28), (164, 92, 52))
HORSE_NOSE = ((28, 22, 24), (74, 62, 62), (132, 116, 114))
BLAZE = (226, 216, 196)
JET = ((6, 5, 6), (26, 22, 24), (66, 56, 58), (118, 104, 104))   # a black mane: shadow, body, crest, gleam

ARM, FOREARM = (-0.1, 0, 0.14), (-0.62, -0.18, 0)
LARM, LFOREARM = ((x, -y, -z) for x, y, z in (ARM, FOREARM))


def guard_parts(ox):
    """The two guards' common frame (`ox` the broader), less the head and weapon."""
    W = 7.6 if ox else 6.8                      # half the chest's breadth
    D = 4.8 if ox else 4.4                      # half its depth
    sx = W + 2.6                                # the shoulder
    parts = [
        # the waist: hips, belt and buckle, and what hangs from them
        part('waist', 'root', (0, 2, 0), (0, 0, 0), [
            (-W + 1.4, -2, -D + 0.8, 2 * W - 2.8, 4, 2 * D - 1.6, 'hips'),
            (-W + 0.6, -3.6, -D + 0.2, 2 * W - 1.2, 3.2, 2 * D - 0.4, 'belt'),
            (-2.8, -4.8, -D - 0.9, 5.6, 5.2, 1.4, 'buckle'),
            (-3.7, -5.8, -D - 0.6, 1.2, 1.8, 1, 'buckle_horn'),
            (2.5, -5.8, -D - 0.6, 1.2, 1.8, 1, 'buckle_horn'),
        ]),
        part('apron', 'waist', (0, -0.2, -D - 0.1), (-0.06, 0, 0), [(-2.4, 0, -0.4, 4.8, 12.5, 0.8, 'apron')]),
        part('right_tasset', 'waist', (-W / 2 - 0.2, -0.6, -D + 0.1), (-0.1, 0.12, 0.08), [(-3.2, 0, -0.6, 6.4, 10, 1.2, 'tasset')]),
        part('right_side_tasset', 'waist', (-W + 0.5, -0.6, 0), (0, 0, 0.18), [(-1.2, 0, -D + 0.6, 1.2, 9.5, 2 * D - 1.2, 'tasset_side')]),
        part('back_tasset', 'waist', (0, -0.6, D - 0.1), (0.12, 0, 0), [(-W + 1.4, 0, -0.6, 2 * W - 2.8, 10.5, 1.2, 'tasset_back')]),
        part('right_sash', 'waist', (-W + 1.2, -2.6, -D + 1.0), (0.02, 0, 0.1), [
            (-0.9, 0, -0.4, 1.8, 11, 0.8, 'sashend'), (-0.4, 0.5, 0.6, 1.8, 8.5, 0.8, 'sashend_short')]),
        # the legs: trousered thighs, then the beast's legs, greaved, down to the hooves
        part('right_leg', 'root', (-W / 2 - 0.5, 2, 0.4), (0, 0, 0), [(-2.9, 0, -2.9, 5.8, 10.5, 5.8, 'thigh')]),
        part('right_shin', 'right_leg', (0, 10, 0), (0, 0, 0), [
            (-2.4, 0, -2.4, 4.8, 8.5, 4.8, 'shin'),
            (-2.9, 0.4, -3.1, 5.8, 7, 3.4, 'greave'),
            (-2.7, -1.6, -3.6, 5.4, 3.4, 1.4, 'kneeguard'),
            (-2.7, 7, -2.7, 5.4, 2.2, 5.4, 'fetlock'),
        ] + ([(-2.6, 8.4, -3.4, 2.5, 3.6, 5.6, 'hoof'), (0.1, 8.4, -3.4, 2.5, 3.6, 5.6, 'hoof')] if ox
             else [(-2.6, 8.4, -3.2, 5.2, 3.6, 5.6, 'hoof')])),
        # the body: a broad chest over a narrower belly; the breastplate and its mirrors, scale armour over
        # the belly, a red silk sash knotted over the belt, the gorget, the shoulders' band
        part('body', 'waist', (0, -3, 0), (0, 0, 0), [
            (-W, -15, -D, 2 * W, 8, 2 * D, 'torso'),
            (-W + 1, -7.4, -D + 0.5, 2 * W - 2, 7.4, 2 * D - 1, 'belly'),
            (-W - 0.6, -15.4, -D - 0.7, 2 * W + 1.2, 8.4, 2 * D + 1.4, 'breastplate'),
            (-W + 0.5, -7.4, -D - 0.1, 2 * W - 1, 6.2, 2 * D + 0.2, 'abdomen'),
            (-W + 0.3, -1.8, -D - 0.3, 2 * W - 0.6, 1.8, 2 * D + 0.6, 'waistsash'),
            (-1.5, -2.4, -D - 0.9, 3, 2.4, 1, 'sashknot'),
            (-W + 0.6, -14, -D - 1.2, 4.6, 4.6, 0.6, 'mirror'),
            (W - 5.2, -14, -D - 1.2, 4.6, 4.6, 0.6, 'mirror'),
            (-2.2, -13.6, -D - 1.0, 4.4, 1.0, 0.4, 'chestcord'),
            (-5, -17, -D + 0.4, 10, 2.2, 2 * D - 0.8, 'gorget'),
            (-W - 0.8, -15.8, -D - 0.2, 2 * W + 1.6, 1.2, 2 * D + 0.4, 'mantle'),
        ]),
        # the cloak, tattered, down his back from the shoulders
        part('cape', 'body', (0, -15.6, D + 1.0), (0.1, 0, 0), [(-W - 0.4, 0, -0.5, 2 * W + 0.8, 14, 1, 'cape')]),
        part('cape_lower', 'cape', (0, 13.6, 0), (0.08, 0, 0), [(-W - 0.8, 0, -0.5, 2 * W + 1.6, 13.5, 1, 'cape_lower')]),
        # the arms: armoured shoulders, hide upper arms, bracers, fists
        part('right_arm', 'body', (-sx, -13.6, 0), ARM, [(-2.7, -1.4, -2.7, 5.4, 8, 5.4, 'upperarm'),
                                                        (-3.0, 4.0, -3.0, 6, 1.4, 6, 'armband')]),
        part('right_pauldron', 'right_arm', (-0.4, -1.4, 0), (0, 0, 0.4), [
            (-4.2, -1.2, -3.9, 6.6, 1.6, 7.8, 'pauldron'),
            (-4.8, 0.3, -4.3, 7, 1.6, 8.6, 'pauldron2'),
            (-5.4, 1.8, -4.7, 7.4, 2, 9.4, 'pauldron3'),
            (-2.8, -2.4, -3.4, 1.2, 1.4, 6.8, 'pauldron_crest'),
            (-6.1, -0.2, -2.1, 1, 4.2, 4.2, 'beastface'),
            (-6.6, 1.6, -1.4, 0.5, 1.2, 2.8, 'beastjaw'),
        ]),
        part('right_forearm', 'right_arm', (0, 6.4, 0), FOREARM, [
            (-2.3, 0, -2.3, 4.6, 7, 4.6, 'forearm'),
            (-2.8, 0.6, -2.8, 5.6, 5.4, 5.6, 'bracer'),
        ]),
        part('right_hand', 'right_forearm', (0, 7, 0), (0, 0, 0), [(-2.1, 0, -2.1, 4.2, 4, 4.2, 'fist')]),
    ]
    return parts, W, D


def horse_head():
    """Horse-Face's neck and head, his halberd, the chain in his left hand."""
    mane = []
    # the mane down the crest of his neck and on down his back, over the cloak
    for kk, (y, length, lean, x) in enumerate(((-12.2, 6, 0.75, 0), (-10.6, 7, 0.6, -0.8), (-9.0, 8, 0.5, 0.8), (-7.4, 9.5, 0.4, -0.6),
                                                 (-5.8, 11, 0.32, 0.6), (-4.2, 12.5, 0.24, -0.2), (-2.6, 14, 0.16, 0.7), (-1.0, 15.5, 0.1, -0.8),
                                                 (0.4, 16, 0.06, 0.2))):
        mane.append(part('mane_%d' % kk, 'neck', (x, y, 3.4), (lean, 0, 0.12 if x < 0 else -0.12 if x > 0 else 0),
                         [(-1.2, 0, -1, 2.4, length, 2, 'mane_a' if kk % 2 == 0 else 'mane_b')]))
    return [
        part('neck', 'body', (0, -15.6, -0.4), (0.2, 0, 0), [
            (-3.2, -12.6, -3.6, 6.4, 13.4, 7.2, 'horse_neck'),
            (-2.6, -12, -4.3, 5.2, 11, 1, 'throat'),
        ]),
    ] + mane + [
        # the head, its long face pitched down before him
        part('head', 'neck', (0, -12.2, -0.6), (0.5, 0, 0), [
            (-3.8, -5.2, -4.6, 7.6, 7.4, 8.2, 'horse_skull'),
            (-4.2, -1.6, -3.8, 8.4, 5.2, 6.2, 'horse_jowl'),
            (-2.9, -4.6, -15.4, 5.8, 5.4, 10.8, 'horse_face'),
            (-3.2, -3.6, -17.2, 6.4, 4.4, 2.8, 'horse_nose'),
            (-2.6, 0.8, -16.6, 5.2, 1, 5, 'teeth'),
            (-4.1, -4.4, -5.0, 8.2, 1.5, 8.8, 'circlet'),
            (-1.6, -4.8, -5.4, 3.2, 2.4, 0.6, 'plaque'),
        ]),
        part('jaw', 'head', (0, 1.8, -4.0), (0.14, 0, 0), [(-2.5, 0, -12.6, 5, 2.6, 12.6, 'horse_jaw'),
                                                          (-2.3, -0.7, -12.4, 4.6, 0.8, 3.6, 'lowteeth')]),
        part('right_eye', 'head', (-3.75, -3.2, -4.8), (0, -0.3, 0), [(-0.6, -1, -1.4, 0.9, 2, 2.8, 'horse_eye')]),
        part('right_brow', 'head', (-3.6, -4.6, -4.6), (0, -0.3, -0.35), [(-1, -0.7, -2.1, 2, 1.4, 4.4, 'horse_brow')]),
        part('right_nostril', 'head', (-3.0, -2.6, -16.6), (0, 0, 0.2), [(-0.8, -1, -1, 1.1, 2.3, 2.6, 'nostril')]),
        part('right_ear', 'head', (-2.6, -5.0, -0.4), (0.5, 0, -0.3), [(-1.1, -5.8, -0.8, 2.2, 5.8, 1.6, 'ear')]),
        part('forelock', 'head', (0, -5.2, -3.2), (-0.9, 0, 0), [(-1.7, 0, -0.8, 3.4, 6, 1.6, 'mane_a')]),
        part('poll_mane', 'head', (0, -5.0, 1.0), (0.6, 0, 0), [(-1.4, 0, -0.9, 2.8, 7, 1.8, 'mane_b')]),
        # the halberd: a spear-point, a crescent blade, a back hook, a red tassel, upright in his right fist
        part('halberd', 'right_hand', (0, 2.2, 0), aim([ARM, FOREARM, (0, 0, 0)], (0.04, 0, 0)), [
            (-0.55, -31, -0.55, 1.1, 46, 1.1, 'shaft'),
            (-1.0, -32.6, -1.0, 2, 2.2, 2, 'gold_collar'),
            (-0.9, -42, -0.35, 1.8, 9.6, 0.7, 'blade'),
            (-0.7, -31.6, -0.7, 1.4, 1.4, 1.4, 'ferrule'),
            (-1.4, -30.6, -1.4, 2.8, 4.4, 2.8, 'tassel'),
            (-0.5, 15, -0.5, 1, 3, 1, 'butt'),
            (-0.7, 2, -0.7, 1.4, 1.2, 1.4, 'ferrule'),
        ]),
        part('crescent', 'halberd', (0.8, -36, 0), (0, 0, 0), [(0, -4, -0.3, 4.4, 8, 0.6, 'crescent')]),
        part('back_hook', 'halberd', (-0.8, -36.2, 0), (0, 0, 0), [(-3.6, -0.5, -0.3, 3.6, 1, 0.6, 'hookbar'),
                                                                   (-3.6, -3, -0.3, 1, 2.6, 0.6, 'hookbar_up')]),
        # the soul-catching chain in his left fist, hanging, a hook at its end
        part('chain', 'left_hand', (0, 3, 0), aim([LARM, LFOREARM, (0, 0, 0)], (0, 0, 0)), [
            (-0.35, 1.8 * kk, -0.75, 0.7, 2, 1.5, 'link_a') if kk % 2 == 0 else (-0.75, 1.8 * kk, -0.35, 1.5, 2, 0.7, 'link_b')
            for kk in range(9)] + [(-0.4, 16.2, -2.6, 0.8, 1, 3.4, 'soulhook'), (-0.4, 13.8, -3.0, 0.8, 2.6, 1, 'soulhook_tip')]),
    ]


def ox_head():
    """Ox-Head's neck and head, his trident, the chain at his hip."""
    return [
        part('neck', 'body', (0, -15.6, -0.8), (0.15, 0, 0), [
            (-5.4, -6.4, -4.6, 10.8, 7.2, 9.2, 'ox_neck'),
            (-5, -7.6, 0.6, 10, 6.8, 6, 'ox_hump'),
            (-3, -4, -6, 6, 7, 2.2, 'dewlap'),
        ]),
        # the head: a bull's. A broad poll between the horns over a wide, curly forehead; the eyes set at its
        # lower corners, out at the sides of the face under heavy bony brows; great round cheeks; and below,
        # the long face sloping down and narrowing to the muzzle, a broad wet nose at its end, flared
        # nostrils, a heavy jaw beneath
        part('head', 'neck', (0, -6.2, -1.6), (-0.05, 0, 0), [
            (-5.0, -8.0, -4.4, 10.0, 7.0, 8.4, 'ox_skull'),
            (-5.8, -8.7, -3.8, 11.6, 2.4, 4.6, 'ox_poll'),
            (-4.5, -7.4, -5.6, 9.0, 3.8, 1.6, 'ox_forehead'),
            (-3.0, -10.0, -5.0, 6.0, 2.8, 4.4, 'ox_tuft'),
            (-1.6, -6.9, -5.95, 3.2, 2.4, 0.6, 'plaque'),
            (-5.4, -2.8, -5.0, 1.8, 4.2, 4.8, 'ox_cheek'), (3.6, -2.8, -5.0, 1.8, 4.2, 4.8, 'ox_cheek'),
        ]),
        # the long face, sloping down from under the brow: the bridge, the muzzle narrowing to the nose
        part('face', 'head', (0, -1.0, -4.6), (0.3, 0, 0), [
            (-3.7, -3.4, -4.4, 7.4, 3.8, 4.8, 'ox_face'),
            (-1.5, -3.85, -4.6, 3.0, 0.6, 4.4, 'ox_ridge'),
            (-3.0, -3.7, -1.6, 6.0, 0.5, 2.0, 'ox_whorl'),
            (-3.4, -2.9, -7.0, 6.8, 3.7, 3.0, 'ox_muzzle'),
            (-3.8, -2.6, -7.8, 7.6, 3.2, 0.9, 'ox_nose'),
            (-4.2, -1.9, -7.5, 1.3, 2.0, 1.9, 'ox_nostril'), (2.9, -1.9, -7.5, 1.3, 2.0, 1.9, 'ox_nostril'),
            (-3.3, 0.6, -7.5, 6.6, 1.0, 3.6, 'ox_lip'),
            (-2.6, 1.3, -7.1, 0.8, 1.9, 0.9, 'fang'), (1.8, 1.3, -7.1, 0.8, 1.9, 0.9, 'fang'),
        ]),
        part('jaw', 'head', (0, -1.0, -4.6), (0.3, 0, 0), [
            (-3.4, 1.6, -6.8, 6.8, 1.8, 6.0, 'ox_jaw'),
            (-2.6, 3.0, -6.5, 5.2, 1.1, 2.6, 'ox_chin')]),
        part('nose_ring', 'face', (0, 0.2, -8.0), (-0.45, 0, 0), [
            (-3.1, 0, -0.6, 1.2, 5, 1.2, 'ring'), (1.9, 0, -0.6, 1.2, 5, 1.2, 'ring'),
            (-3.1, 5, -0.6, 6.2, 1.2, 1.2, 'ring'), (-1.0, 4.8, -0.9, 2, 2, 1.8, 'ringbead')]),
        part('right_eye', 'head', (-3.95, -2.95, -5.5), (0, 0.38, 0.18), [(-1.25, -0.7, -0.3, 2.5, 1.4, 0.5, 'ox_eye')]),
        part('right_brow', 'head', (-3.5, -4.05, -5.4), (0.1, 0.4, 0.22), [(-2.1, -0.7, -0.9, 3.8, 1.4, 1.7, 'ox_brow')]),
        part('right_ear', 'head', (-5.0, -4.6, -0.4), (0.1, 0.3, -0.4), [(-4.6, -1.0, -1.6, 4.6, 2.0, 3.2, 'ox_ear')]),
        # the horns: heavy at the root, a gold band about it, curving out, up and forward to a black tip
        part('right_horn', 'head', (-5.2, -6.6, -1.0), (0.1, -0.1, 0.08), [
            (-5, -1.9, -1.9, 5, 3.8, 3.8, 'horn1'),
            (-2.4, -2.2, -2.2, 1.6, 4.4, 4.4, 'horn_band'),
        ]),
        part('right_horn_2', 'right_horn', (-4.7, 0, 0), (0, -0.12, 0.45), [(-4.8, -1.5, -1.5, 4.8, 3, 3, 'horn2')]),
        part('right_horn_3', 'right_horn_2', (-4.5, 0, 0), (0, -0.16, 0.6), [(-4.2, -1.1, -1.1, 4.2, 2.2, 2.2, 'horn3')]),
        part('right_horn_4', 'right_horn_3', (-3.9, 0, 0), (0, -0.2, 0.55), [(-3.6, -0.7, -0.7, 3.6, 1.4, 1.4, 'horn4')]),
        # the trident: three steel prongs, the outer ones curving outward, a gold collar and red tassel
        part('trident', 'right_hand', (0, 2.2, 0), aim([ARM, FOREARM, (0, 0, 0)], (0.04, 0, 0)), [
            (-0.6, -31, -0.6, 1.2, 46, 1.2, 'shaft'),
            (-4.6, -33.2, -0.8, 9.2, 1.6, 1.6, 'crossbar'),
            (-1.4, -32.4, -1.4, 2.8, 2.8, 2.8, 'gold_collar'),
            (-0.65, -42.4, -0.65, 1.3, 9.4, 1.3, 'prong'),
            (-0.45, -44.6, -0.45, 0.9, 2.2, 0.9, 'prongtip'),
            (-0.7, -31.2, -0.7, 1.4, 1.4, 1.4, 'ferrule'),
            (-1.5, -29.6, -1.5, 3, 4.8, 3, 'tassel'),
            (-0.6, 15, -0.6, 1.2, 3, 1.2, 'butt'),
            (-0.75, 2, -0.75, 1.5, 1.2, 1.5, 'ferrule'),
        ]),
        part('prong_r', 'trident', (-4, -33, 0), (0, 0, -0.18), [(-0.6, -8, -0.6, 1.2, 8, 1.2, 'prong'),
                                                               (-0.4, -10, -0.4, 0.8, 2, 0.8, 'prongtip')]),
        part('prong_l', 'trident', (4, -33, 0), (0, 0, 0.18), [(-0.6, -8, -0.6, 1.2, 8, 1.2, 'prong'),
                                                              (-0.4, -10, -0.4, 0.8, 2, 0.8, 'prongtip')]),
        # the chain and shackle at his left hip
        part('chain', 'waist', (7.0, -2.2, -2.6), (0, 0, -0.1), [
            (-0.35, 1.8 * kk, -0.75, 0.7, 2, 1.5, 'link_a') if kk % 2 == 0 else (-0.75, 1.8 * kk, -0.35, 1.5, 2, 0.7, 'link_b')
            for kk in range(6)] + [(-1.5, 10.6, -1.5, 3, 2.2, 3, 'shackle')]),
    ]


def model(ox):
    body, W, D = guard_parts(ox)
    return mirrored(body + (ox_head() if ox else horse_head()))


OX = model(True)
HORSE = model(False)


# ── Painting ─────────────────────────────────────────────────────────────────

class GuardPainter(Painter):
    def clear(self, xy):
        x, y = int(xy[0]), int(xy[1])
        if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
            self.img.putpixel((x, y), (0, 0, 0, 0))
            self.glow.putpixel((x, y), (0, 0, 0, 0))

    def save_as(self, skin, eyes=None):
        self.img.save(OUT + skin + '.png')
        if eyes:
            self.glow.save(OUT + eyes + '.png')


SIDES = ('front', 'back', 'right', 'left')
LIGHTS = {'top': 1.1, 'bottom': 0.62, 'front': 1.0, 'back': 0.84, 'right': 0.92, 'left': 0.92}


def tone(c, t):
    """A hide's (shadow, body, light) at t: 0 shadow .. 1 body .. 2 light."""
    t = max(0.0, min(2.0, t))
    return mixc(c[0], c[1], t) if t < 1 else mixc(c[1], c[2], t - 1)


def each(rect):
    """Each pixel of a face: (i, j, (x, y))."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            yield i, j, (fx + i, fy + j)


def fur(p, f, hide, light=1.0, round_=True, streak=True, sides=None):
    """Short-haired hide over a box: lit above, rounded across each face (light in the middle, dark to its
    edges), in fine streaks of hair down it."""
    for s, (fx, fy, fw, fh) in f.items():
        if sides and s not in sides:
            continue
        lit = LIGHTS[s] * light
        cols = [p.rnd.uniform(-0.12, 0.12) for _ in range(fw)]
        for j in range(fh):
            for i in range(fw):
                across = abs((i + 0.5) / fw - 0.5) * 2
                t = 1.0 + (0.55 * (1 - across ** 2) - 0.35 if round_ else 0) + (cols[i] if streak else 0)
                t -= 0.35 * j / max(1, fh - 1) if s in SIDES else 0
                t = t * lit + (lit - 1) * 1.5
                p.px((fx + i, fy + j), noisy(p, tone(hide, t), 3))


def shaggy(p, rect, hide, depth=3, teeth=3):
    """Coarse hair: darker locks in strokes, its lower edge ragged."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        ph = p.rnd.random()
        for j in range(fh):
            n = ((i + j // 3) % 3)
            t = 0.55 + 0.6 * (n == 0) + 0.2 * (n == 1) + 0.3 * math.sin(ph * 6 + j * 0.3)
            p.px((fx + i, fy + j), noisy(p, tone(hide, t), 4))
    if depth:
        ragged(p, rect, teeth=teeth, depth=depth)


def curls(p, rect, hide):
    """Tight curls of hair (an ox's brow): little rings, lit on top, dark below."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            ci, cj = (i + (j // 3) % 2 * 2) % 4, j % 3
            t = 1.5 if cj == 0 and ci in (1, 2) else 0.35 if cj == 2 or ci == 0 else 1.0
            p.px((fx + i, fy + j), noisy(p, tone(hide, t + p.rnd.uniform(-0.2, 0.2)), 3))


def lamellar(p, rect, base, lace, lace_deep, trim, pw=4, ph=5, top_rim=True, bottom_rim=True, light=1.0, studs=False):
    """Lamellar armour: lacquered plates in staggered rows, each rounded (lit down its middle, a gleam at
    its top, dark at its edges and foot), laced to the next row in red at its middle."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        row, y = j // ph, j % ph
        off = (row % 2) * (pw // 2)
        for i in range(fw):
            x = (i + off) % pw
            across = abs(x + 0.5 - pw / 2) / (pw / 2)
            kk = (1.25 - 0.45 * across - 0.3 * y / max(1, ph - 1)) * light
            c = k(base, kk)
            if y == 0 and across < 0.6:
                c = k(BLACK_SHEEN, 1.5 * light)
            if x == 0:
                c = k(base, 0.4 * light)
            if y == ph - 1:
                c = k(lace, light) if x in (pw // 2 - 1, pw // 2) and row % 2 == 0 else k(base, 0.3 * light)
            p.px((fx + i, fy + j), noisy(p, c, 2))
    for i in range(fw):
        if top_rim:
            p.px((fx + i, fy), k(trim, light))
            p.px((fx + i, fy + 1), k(GOLD_DEEP, light))
        if bottom_rim:
            p.px((fx + i, fy + fh - 1), k(GOLD_DEEP, light))
            p.px((fx + i, fy + fh - 2), k(trim, light))
    if studs:
        for i in range(2, fw - 1, 4):
            p.px((fx + i, fy + 3), GOLD_LIGHT)
            p.px((fx + i, fy + 4), GOLD_DEEP)


def lamellar_box(p, f, c, **kw):
    for s, r in f.items():
        if s in ('top', 'bottom'):
            p.face(r, BLACK if s == 'bottom' else BLACK_SHEEN, LIGHTS[s], noise=3)
            fx, fy, fw, fh = r
            for i in range(fw):
                p.px((fx + i, fy), c['trim'])
                p.px((fx + i, fy + fh - 1), c['trim'])
            for j in range(fh):
                p.px((fx, fy + j), c['trim'])
                p.px((fx + fw - 1, fy + j), c['trim'])
        else:
            lamellar(p, r, BLACK_SHEEN, c['lace'], c['lace_deep'], c['trim'], light=LIGHTS[s], **kw)


def gold_box(p, f, light=1.0):
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                t = (j / max(1, fh - 1))
                c = mixc(GOLD_LIGHT, GOLD, min(1, t * 2)) if t < 0.5 else mixc(GOLD, GOLD_DEEP, (t - 0.5) * 2)
                p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s] * light), 4))


def steel_box(p, f, base=STEEL, edge=True):
    for s, (fx, fy, fw, fh) in f.items():
        for j in range(fh):
            for i in range(fw):
                across = (i + 0.5) / fw
                c = mixc((236, 240, 248), base, min(1, across * 2.2)) if across < 0.5 else mixc(base, STEEL_DARK, (across - 0.5) * 2)
                p.px((fx + i, fy + j), noisy(p, k(c, LIGHTS[s]), 3))


def hide_rim(p, rect, colour, sides='lrtb'):
    fx, fy, fw, fh = rect
    for i in range(fw):
        if 't' in sides:
            p.px((fx + i, fy), colour)
        if 'b' in sides:
            p.px((fx + i, fy + fh - 1), colour)
    for j in range(fh):
        if 'l' in sides:
            p.px((fx, fy + j), colour)
        if 'r' in sides:
            p.px((fx + fw - 1, fy + j), colour)


def eye_glow(p, rect, iris=EMBER, core=EMBER_HOT, pupil=True, slant=1):
    """A burning eye over a face: a dark rim, the lid falling toward the nose, a hot core, a dark pupil."""
    fx, fy, fw, fh = rect
    for j in range(fh):
        for i in range(fw):
            t = i / max(1, fw - 1)
            lid = int(round((t if slant > 0 else 1 - t) * (fh * 0.4)))
            if j < lid:
                p.px((fx + i, fy + j), (20, 8, 8))
                continue
            hot = abs(i - (fw - 1) / 2) <= fw * 0.22 and j >= lid + (fh - lid) // 3
            p.px((fx + i, fy + j), core if hot else iris, glow=True)
    if pupil and fw >= 3:
        cx = fx + fw // 2
        p.px((cx, fy + fh - 1), (12, 4, 4))
        p.glow.putpixel((cx, fy + fh - 1), (0, 0, 0, 0))


def demon_face(p, rect, eyes=True):
    """A gold demon's face (the buckle): horned brows, glaring eyes, a broad nose, fanged mouth."""
    fx, fy, fw, fh = rect                                         # 11x10
    gold_box(p, {'front': rect})
    D = GOLD_DEEP
    dk = (70, 40, 14)
    cx = fw // 2
    for i in range(fw):                                           # the brow, knit into a V
        d = abs(i - cx + 0.5)
        y = 1 + int(d < 2.5)
        p.px((fx + i, fy + y), D)
        p.px((fx + i, fy + y + 1), dk if 1.5 < d < 4.5 else GOLD)
    for i in (cx - 3, cx - 2, cx + 1, cx + 2):                    # the eyes
        if 0 <= i < fw:
            p.px((fx + i, fy + 3), EMBER if eyes else dk, glow=eyes)
            p.px((fx + i, fy + 4), D)
    for j in range(3, 6):                                         # the nose
        p.px((fx + cx, fy + j), GOLD_LIGHT)
        p.px((fx + cx - 1, fy + j), D)
    p.px((fx + cx - 2, fy + 6), dk)
    p.px((fx + cx + 1, fy + 6), dk)
    for i in range(1, fw - 1):                                    # the mouth: a dark gape, fangs, teeth
        p.px((fx + i, fy + 7), (40, 10, 8))
        p.px((fx + i, fy + 8), IVORY if i % 2 else (40, 10, 8))
    for i in (2, fw - 3):
        p.px((fx + i, fy + 7), IVORY)
        p.px((fx + i, fy + 9), IVORY)
    for j in range(fh):
        p.px((fx, fy + j), D)
        p.px((fx + fw - 1, fy + j), D)


def chain_paint(p, style, f):
    if style in ('link_a', 'link_b'):
        p.shaded(f, IRON)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            if fw > 2 and fh > 2:
                for j in range(1, fh - 1):
                    for i in range(1, fw - 1):
                        p.clear((fx + i, fy + j))
            for i in range(fw):
                p.px((fx + i, fy), (130, 128, 138))
    if style == 'shackle':
        p.shaded(f, IRON)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                p.px((fx + i, fy), (130, 128, 138))
                p.px((fx + i, fy + fh - 1), IRON_DARK)
            p.px((fx + fw // 2, fy + fh // 2), IRON_DARK)


def guard_painter(ox, captain):
    c = COLOURS[captain]
    HIDE = OX_HIDE if ox else HORSE_HIDE
    MANE = (JET, BROWN_BLACK)

    def paint_guard(p, style, f, dims):
        if style in ('link_a', 'link_b', 'shackle'):
            chain_paint(p, style, f)

        # ── cloth, belt, buckle ──
        if style in ('hips', 'thigh'):
            for s, r in f.items():
                p.face(r, CLOTH, LIGHTS[s], noise=3, fall=0.25)
                fx, fy, fw, fh = r
                for i in range(1, fw, 4):
                    for j in range(fh):
                        if p.rnd.random() < 0.7:
                            p.px((fx + i, fy + j), k(CLOTH, 0.7 * LIGHTS[s]))
            if style == 'thigh':                                   # bound at the knee with red cord
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 2), c['lace'])
                        p.px((fx + i, fy + fh - 1), c['lace_deep'])
        if style == 'belt':
            for s, r in f.items():
                p.face(r, c['lace_deep'], LIGHTS[s], noise=3)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), c['trim'])
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
                for i in range(3, fw - 4, 9):                     # gold plaques along it, a jade set in each
                    for j in range(1, fh - 1):
                        for w in range(4):
                            p.px((fx + i + w, fy + j), k(GOLD if w < 3 and j < fh - 2 else GOLD_DEEP, LIGHTS[s]))
                    p.px((fx + i + 1, fy + fh // 2), (96, 178, 136))
                    p.px((fx + i + 2, fy + fh // 2), (52, 116, 86))
        if style == 'buckle':
            gold_box(p, f)
            demon_face(p, f['front'])
        if style == 'buckle_horn':
            gold_box(p, f)
            for s in SIDES:
                point(p, f[s], keep=0.5, down=False, sharp=0.6)
        if style == 'apron':                                      # red cloth, a gold border, a black seal, a ragged foot
            for s, r in f.items():
                p.face(r, c['cloak'], LIGHTS[s], noise=4, fall=0.3)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    p.px((fx, fy + j), c['trim'])
                    p.px((fx + fw - 1, fy + j), c['trim'])
                for i in range(fw):
                    p.px((fx + i, fy), c['trim'])
                cx, cy = fx + fw / 2, fy + 8
                circle(p, cx, cy, 0, 3.2, BLACK)
                circle(p, cx, cy, 2.4, 3.3, c['trim'])
                for j in (-1, 0, 1):                              # 冥, as three gold strokes
                    p.px((cx - 1 + j, cy - 1), GOLD)
                p.px((cx - 1, cy), GOLD)
                p.px((cx, cy + 1), GOLD)
                for i in range(fw):                               # gold flames up from its foot
                    h = 3 + int(2 * abs(math.sin(i * 1.3)))
                    for j in range(h):
                        p.px((fx + i, fy + fh - 6 - j), mixc(GOLD, c['cloak'], j / h))
                ragged(p, f[s], teeth=3, depth=3)
        if style in ('sashend', 'sashend_short'):
            for s, r in f.items():
                p.face(r, c['lace'], LIGHTS[s], noise=4, fall=0.35)
                fx, fy, fw, fh = r
                if s in ('front', 'back'):
                    for j in range(0, fh, 5):
                        for i in range(fw):
                            p.px((fx + i, fy + j), k(c['lace'], 0.7))
                    ragged(p, r, teeth=2, depth=3)

        # ── armour ──
        if style == 'belly':
            for s, r in f.items():
                p.face(r, CLOTH, LIGHTS[s], noise=3)
        if style in ('waistsash', 'sashknot'):                    # red silk, wound round and knotted
            for s, r in f.items():
                fx, fy, fw, fh = r
                for j in range(fh):
                    for i in range(fw):
                        t = 1.15 - 0.4 * j / max(1, fh - 1) + (0.12 if (i + j) % 4 == 0 else 0)
                        p.px((fx + i, fy + j), noisy(p, k(c['lace'], t * LIGHTS[s]), 3))
            if style == 'sashknot':
                fx, fy, fw, fh = f['front']
                for j in range(fh):
                    p.px((fx + fw // 2, fy + j), c['lace_deep'])
        if style in ('pauldron', 'pauldron2', 'pauldron3'):      # a tier of the shoulder guard: black lacquer, rounded, a gold lip, studs
            for s, r in f.items():
                fx, fy, fw, fh = r
                for j in range(fh):
                    for i in range(fw):
                        t = (1.3 - 0.5 * j / max(1, fh - 1)) * LIGHTS[s]
                        if s in ('top', 'bottom'):
                            t = LIGHTS[s] * (1.25 - 0.4 * abs(j / max(1, fh - 1) - 0.5))
                        p.px((fx + i, fy + j), noisy(p, k(BLACK_SHEEN, t), 3))
                if s in SIDES:
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 1), c['trim'])
                        if i % 4 == 2 and fh > 3:
                            p.px((fx + i, fy + 1), GOLD)
                    for j in range(fh - 1):
                        if s in ('front', 'back'):
                            for i in range(0, fw, 5):
                                p.px((fx + i, fy + j), k(BLACK, 0.6))
                if s == 'bottom':
                    hide_rim(p, r, c['trim'])
        if style == 'abdomen':                                    # fish-scale armour (鱼鳞甲): dark iron scales, gold-edged rows
            for s, r in f.items():
                fx, fy, fw, fh = r
                if s in ('top', 'bottom'):
                    p.face(r, BLACK, LIGHTS[s], noise=3)
                    continue
                for j in range(fh):
                    row = j // 3
                    for i in range(fw):
                        x = (i + (row % 2) * 2) % 4
                        y = j % 3
                        d = math.hypot((x - 1.5) / 2, (y - 0.2) / 3)
                        t = (1.35 - 0.7 * d) * LIGHTS[s]
                        col = k((84, 76, 80), t)
                        if d > 0.85 or y == 2 and x in (0, 3):
                            col = k(GOLD_DEEP, 0.8 * LIGHTS[s])
                        p.px((fx + i, fy + j), noisy(p, col, 3))
                for i in range(fw):
                    p.px((fx + i, fy), c['trim'])
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
        if style in ('tasset', 'tasset_side', 'tasset_back', 'torso'):
            lamellar_box(p, f, c)
            if style.startswith('tasset'):                        # gold studs down the plates' edges, a scalloped foot
                for s in ('front', 'back', 'right', 'left'):
                    fx, fy, fw, fh = f[s]
                    if fw > 4:
                        for i in range(2, fw - 1, 4):
                            p.px((fx + i, fy + 3), GOLD_LIGHT)
            if style == 'torso':                                  # the back: a gold-rimmed back plate
                fx, fy, fw, fh = f['back']
                for j in range(4, 14):
                    for i in range(4, fw - 4):
                        if j in (4, 13) or i in (4, fw - 5):
                            p.px((fx + i, fy + j), c['trim'])
        if style == 'breastplate':                                # the breastplate: black lacquer moulded to the chest, gold-rimmed,
            for s, r in f.items():                                # a gold cloud scroll along its top, studs down its edges
                fx, fy, fw, fh = r
                for j in range(fh):
                    for i in range(fw):
                        across = abs((i + 0.5) / fw - 0.5) * 2
                        t = (1.45 - 0.55 * across ** 1.5 - 0.35 * j / max(1, fh - 1)) * LIGHTS[s]
                        p.px((fx + i, fy + j), noisy(p, k((52, 40, 46), t), 2))
                if s in SIDES:
                    for i in range(fw):
                        for w in (0, 1):
                            p.px((fx + i, fy + w), c['trim'] if w == 0 else GOLD_DEEP)
                            p.px((fx + i, fy + fh - 1 - w), c['trim'] if w == 0 else GOLD_DEEP)
                    for j in range(3, fh - 2, 3):
                        p.px((fx + 1, fy + j), GOLD)
                        p.px((fx + fw - 2, fy + j), GOLD)
                if s == 'front':                                  # the pectoral line and a gold cloud scroll over it
                    for j in range(3, fh - 2):
                        p.px((fx + fw // 2, fy + j), k(BLACK, 0.7))
                    for i in range(3, fw - 3):
                        if (i // 3) % 2 == 0:
                            p.px((fx + i, fy + 3), GOLD)
                        else:
                            p.px((fx + i, fy + 2), GOLD_DEEP)
                    for i in range(fw // 2 - 3, fw // 2 + 3):  # a tiger's face at the throat
                        p.px((fx + i, fy + 4), GOLD)
                        p.px((fx + i, fy + 5), GOLD_DEEP if abs(i - fw // 2 + 0.5) > 1 else EMBER)
                if s == 'back':
                    for i in range(3, fw - 3):
                        if (i // 3) % 2 == 0:
                            p.px((fx + i, fy + 3), GOLD_DEEP)
        if style == 'mirror':                                     # a round mirror plate: polished dark steel, a gold rim and boss
            for s, (fx, fy, fw, fh) in f.items():
                if s in ('front', 'back'):
                    cx, cy, R = fx + fw / 2, fy + fh / 2, fw / 2
                    for y in range(fy, fy + fh):
                        for x in range(fx, fx + fw):
                            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / R
                            if d > 1.02:
                                p.clear((x, y))
                            elif d > 0.78:
                                p.px((x, y), GOLD if (x + y) % 3 else GOLD_DEEP)
                            else:
                                hl = (x - fx) + (y - fy) < fw * 0.7
                                p.px((x, y), noisy(p, (150, 156, 170) if hl else (64, 68, 80), 4))
                    circle(p, cx, cy, 0, 1.2, GOLD_LIGHT)
                else:
                    p.face((fx, fy, fw, fh), GOLD_DEEP, 1.0, noise=3)
        if style == 'chestcord':
            for s, r in f.items():
                p.face(r, c['lace'], LIGHTS[s], noise=3)
            fx, fy, fw, fh = f['front']
            p.px((fx + fw // 2, fy), GOLD)
            p.px((fx + fw // 2 - 1, fy + 1), GOLD)
        if style == 'gorget':
            for s, r in f.items():
                p.face(r, BLACK_SHEEN, LIGHTS[s], noise=3)
                fx, fy, fw, fh = r
                for i in range(fw):
                    p.px((fx + i, fy), c['trim'])
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
                    if i % 3 == 1:
                        p.px((fx + i, fy + fh // 2), GOLD)
        if style == 'mantle':                                     # the shoulders' edge: a gold-studded band
            for s, r in f.items():
                p.face(r, BLACK, LIGHTS[s], noise=3)
                fx, fy, fw, fh = r
                for i in range(fw):
                    p.px((fx + i, fy), c['trim'])
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
                    if s == 'top' and i % 4 == 2:
                        for j in range(1, fh - 1, 3):
                            p.px((fx + i, fy + j), GOLD)
        if style == 'pauldron_crest':
            gold_box(p, f)
            for s in SIDES + ('top',):
                fx, fy, fw, fh = f[s]
                for i in range(0, fw, 3):
                    p.px((fx + i, fy + fh // 2), GOLD_DEEP)
        if style == 'beastface':                                  # the gold beast's head biting the shoulder
            gold_box(p, f)
            fx, fy, fw, fh = f['right']                           # 8x8
            for i in range(fw):
                p.px((fx + i, fy + 1), GOLD_DEEP)
            for i in (1, 2, fw - 3, fw - 2):
                p.px((fx + i, fy + 2), EMBER, glow=True)
                p.px((fx + i, fy + 3), GOLD_DEEP)
            p.px((fx + fw // 2, fy + 3), GOLD_LIGHT)
            p.px((fx + fw // 2 - 1, fy + 4), GOLD_DEEP)
            for i in range(1, fw - 1):
                p.px((fx + i, fy + 5), (50, 20, 10))
                p.px((fx + i, fy + 6), IVORY if i % 2 else (50, 20, 10))
        if style == 'beastjaw':
            gold_box(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(0, fw, 2):
                    p.px((fx + i, fy), IVORY)
        if style == 'kneeguard':                                  # a round boss, gold-rimmed
            for s, r in f.items():
                p.face(r, BLACK_SHEEN, LIGHTS[s], noise=3)
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                p.px((fx + i, fy), c['trim'])
                p.px((fx + i, fy + fh - 1), GOLD_DEEP)
            for j in range(fh):
                p.px((fx, fy + j), c['trim'])
                p.px((fx + fw - 1, fy + j), c['trim'])
            circle(p, fx + fw / 2, fy + fh / 2, 0, 2.2, GOLD)
            circle(p, fx + fw / 2, fy + fh / 2, 0, 1.0, c['lace'])
        if style == 'greave':                                     # a black greave: gold rims, a gold ridge, cloud studs
            for s, r in f.items():
                p.face(r, BLACK_SHEEN, LIGHTS[s], noise=3, fall=0.2)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), c['trim'])
                    p.px((fx + i, fy + 1), GOLD_DEEP)
                    p.px((fx + i, fy + fh - 1), c['trim'])
                for j in range(fh):
                    for i in range(fw):
                        if (j - 3) % 4 == 0 and 0 < i < fw - 1:
                            p.px((fx + i, fy + j), k(BLACK, LIGHTS[s]))
            fx, fy, fw, fh = f['front']
            for j in range(2, fh - 1):
                p.px((fx + fw // 2, fy + j), GOLD)
                p.px((fx + fw // 2 - 1, fy + j), GOLD_DEEP)
            for j in (4, 9):
                p.px((fx + 2, fy + j), GOLD_LIGHT)
                p.px((fx + fw - 3, fy + j), GOLD_LIGHT)
        if style == 'bracer':
            for s, r in f.items():
                p.face(r, BLACK_SHEEN, LIGHTS[s], noise=3)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    for w in (0, 1):
                        p.px((fx + i, fy + w), c['trim'] if w == 0 else GOLD_DEEP)
                        p.px((fx + i, fy + fh - 1 - w), c['trim'] if w == 0 else GOLD_DEEP)
                for j in range(3, fh - 3, 2):
                    for i in range(1, fw - 1):
                        if (i + j) % 4 == 0:
                            p.px((fx + i, fy + j), GOLD)
                for j in range(2, fh - 2):
                    p.px((fx + fw // 2, fy + j), c['lace'])
        if style == 'cape':                                       # the cloak's upper half: gold at the shoulders, the seal
            for s, r in f.items():
                fx, fy, fw, fh = r
                for j in range(fh):
                    for i in range(fw):
                        fold = 1 + 0.12 * math.sin(i * 0.55) + 0.05 * math.sin(i * 1.7)
                        p.px((fx + i, fy + j), noisy(p, k(c['cloak'], fold * LIGHTS[s] * (1.05 - 0.1 * j / fh)), 3))
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), c['trim'])
                    p.px((fx + i, fy + 1), GOLD_DEEP)
                    p.px((fx + i, fy + 3), GOLD_DEEP)
                if s == 'back':                                   # a great black seal on it, faded
                    cx, cy = fx + fw / 2, fy + 16
                    circle(p, cx, cy, 6, 7.2, k(c['cloak_deep'], 0.7))
                    circle(p, cx, cy, 4.2, 4.8, k(c['cloak_deep'], 0.7))
                    for j in range(-3, 4):
                        p.px((cx + j, cy), k(c['cloak_deep'], 0.6))
                        p.px((cx, cy + j), k(c['cloak_deep'], 0.6))
                x0 = int(fw * (0.3 if s == 'back' else 0.7)) + fx
                for j in range(3):
                    for i in range(2):
                        p.clear((x0 + i, int(fh * 0.7) + fy + j))
        if style == 'armband':                                    # a gold arm ring about the biceps, a red stone
            gold_box(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
                p.px((fx + fw // 2, fy + 1), (210, 30, 30))
        if style == 'ox_nostril':                                 # the flared wing of a nostril
            fur(p, f, ((40, 30, 32), (84, 66, 68), (130, 110, 112)), streak=False)
            for s in ('front', 'right', 'left'):
                fx, fy, fw, fh = f[s]
                for j in range(1, fh - 1):
                    p.px((fx + fw // 2, fy + j), (14, 6, 8))
        if style == 'cape_lower':                                 # the cloak's lower half, tattered
            for s, r in f.items():
                fx, fy, fw, fh = r
                for j in range(fh):
                    t = j / max(1, fh - 1)
                    for i in range(fw):
                        fold = 1 + 0.12 * math.sin(i * 0.55) + 0.05 * math.sin(i * 1.7)
                        col = mixc(c['cloak'], c['cloak_deep'], 0.1 + t * 0.7)
                        p.px((fx + i, fy + j), noisy(p, k(col, fold * LIGHTS[s]), 3))
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                # tatters: a ragged foot, deep rents, a few holes
                for i in range(fw):
                    ii = i if s == 'back' else fw - 1 - i
                    cut = int(4 + 6 * abs(math.sin(ii * 0.37 + 1.1)) + 6 * (math.sin(ii * 1.3) > 0.6))
                    for j in range(cut):
                        p.clear((fx + i, fy + fh - 1 - j))
                    p.px((fx + i, fy + fh - 1 - cut), k(c['cloak_deep'], 0.7))
                for (hx, hy) in ((0.25, 0.32), (0.7, 0.2), (0.55, 0.5)):
                    x0 = int(fw * (hx if s == 'back' else 1 - hx)) + fx
                    y0 = int(fh * hy) + fy
                    for j in range(3):
                        for i in range(2 - abs(j - 1) + 1):
                            p.clear((x0 + i, y0 + j))
            fx, fy, fw, fh = f['bottom']
            for i in range(fw):
                p.clear((fx + i, fy))
                p.clear((fx + i, fy + 1))

        # ── hide: arms, fists, legs ──
        if style in ('upperarm', 'forearm'):
            fur(p, f, HIDE)
            for s in ('front', 'right', 'left'):                  # veins over the muscle
                fx, fy, fw, fh = f[s]
                x = fw // 3
                for j in range(2, fh - 2):
                    x += p.rnd.choice((-1, 0, 0, 1))
                    x = max(1, min(fw - 2, x))
                    p.px((fx + x, fy + j), tone(HIDE, 0.55))
        if style == 'fist':
            fur(p, f, HIDE, light=0.95)
            for s in ('front', 'right', 'left'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    if i % 2 == 0:
                        p.px((fx + i, fy + fh - 3), tone(HIDE, 0.3))
                    p.px((fx + i, fy + 1), tone(HIDE, 1.6))
            fx, fy, fw, fh = f['bottom']
            for i in range(1, fw - 1):
                p.px((fx + i, fy + fh // 2), tone(HIDE, 0.25))
        if style == 'shin':
            fur(p, f, HIDE)
        if style == 'fetlock':                                    # long hair falling over the hoof
            for s, r in f.items():
                if s == 'top':
                    p.face(r, tone(HIDE, 1.0), 1.0, noise=3)
                elif s == 'bottom':
                    p.face(r, tone(HIDE, 0.3), 1.0, noise=3)
                else:
                    shaggy(p, r, HIDE if ox else ((40, 34, 34), (104, 96, 92), (190, 182, 172)), depth=2, teeth=2)
        if style == 'hoof':                                       # horn: growth lines down it, a pale coronet, dark at the sole
            HORN = (54, 46, 44)
            for s, r in f.items():
                fx, fy, fw, fh = r
                for j in range(fh):
                    for i in range(fw):
                        t = j / max(1, fh - 1)
                        col = k(HORN, (1.25 - 0.45 * t) * LIGHTS[s] * (0.9 if i % 3 == 0 else 1.0))
                        p.px((fx + i, fy + j), noisy(p, col, 3))
                if s in SIDES:
                    for i in range(fw):
                        p.px((fx + i, fy), k((120, 108, 100), LIGHTS[s]))

        # ── weapons ──
        if style == 'shaft':                                      # dark red lacquer, banded gold, a cord grip
            for s, r in f.items():
                p.face(r, (62, 18, 20), LIGHTS[s], noise=3)
                fx, fy, fw, fh = r
                if s in SIDES:
                    for j in range(0, fh, 14):
                        for w in range(2):
                            for i in range(fw):
                                p.px((fx + i, fy + j + w), GOLD if w == 0 else GOLD_DEEP)
                    for j in range(58, 74):                       # the grip, bound in black cord
                        for i in range(fw):
                            p.px((fx + i, fy + j), (24, 20, 22) if (i + j) % 2 else (60, 50, 50))
                    p.px((fx, fy + 3), (110, 40, 40))
        if style in ('gold_collar', 'ferrule', 'crossbar'):
            gold_box(p, f)
            if style == 'crossbar':                               # a beast's mouth at its middle, swallowing the prong
                fx, fy, fw, fh = f['front']
                cx = fx + fw // 2
                for i in range(-3, 3):
                    p.px((cx + i, fy + 1), (60, 30, 10))
                for i in (-3, 2):
                    p.px((cx + i, fy), EMBER)
            if style == 'gold_collar':
                for s in SIDES:
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        p.px((fx + i, fy + fh // 2), GOLD_DEEP)
        if style == 'butt':
            steel_box(p, f, STEEL_DARK)
            for s in SIDES:
                point(p, f[s], keep=0.5, down=True, sharp=0.5)
        if style in ('prong', 'prongtip'):
            steel_box(p, f)
            if style == 'prongtip':
                for s in SIDES:
                    point(p, f[s], keep=0.3, down=False, sharp=0.5)
        if style == 'blade':                                      # a long leaf-shaped point, a ridge down it
            steel_box(p, f)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    p.px((fx + fw // 2, fy + j), (240, 244, 252))
                    p.px((fx + fw // 2 - 1, fy + j), STEEL_DARK)
                point(p, f[s], keep=0.5, down=False, sharp=0.35)
            for s in ('right', 'left'):
                point(p, f[s], keep=0.5, down=False, sharp=0.35)
        if style == 'crescent':                                   # the crescent blade: a moon, its edge outward
            for s, r in f.items():
                p.face(r, STEEL, LIGHTS[s], noise=3)
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]                             # 9x16
                for y in range(fh):
                    for x in range(fw):
                        xx = x if s == 'front' else fw - 1 - x
                        dy = (y + 0.5 - fh / 2) / (fh / 2)
                        outer = (fw - 0.5) * math.sqrt(max(0, 1 - dy * dy))
                        inner = 0.45 * fw * math.sqrt(max(0, 1 - (dy / 0.8) ** 2)) if abs(dy) < 0.8 else -1
                        if xx > outer or xx < inner:
                            p.clear((fx + x, fy + y))
                        elif xx > outer - 1.6:
                            p.px((fx + x, fy + y), (244, 248, 255))
                        else:
                            p.px((fx + x, fy + y), noisy(p, mixc(STEEL, STEEL_DARK, (y / fh) * 0.6), 3))
        if style in ('hookbar', 'hookbar_up'):
            steel_box(p, f)
            if style == 'hookbar_up':
                for s in SIDES:
                    point(p, f[s], keep=0.3, down=False, sharp=0.5)
        if style == 'tassel':                                     # a red horsehair tassel, ragged
            for s, r in f.items():
                fx, fy, fw, fh = r
                for i in range(fw):
                    sh = p.rnd.uniform(0.7, 1.15)
                    for j in range(fh):
                        p.px((fx + i, fy + j), noisy(p, k(c['lace'], sh * (1.15 - 0.4 * j / fh) * LIGHTS[s]), 4))
                if s in SIDES:
                    ragged(p, r, teeth=2, depth=4)
                    for i in range(fw):
                        p.px((fx + i, fy), GOLD)
        if style in ('soulhook', 'soulhook_tip'):
            steel_box(p, f, IRON)
            if style == 'soulhook_tip':
                for s in SIDES:
                    point(p, f[s], keep=0.3, down=False, sharp=0.5)

        # ── heads ──
        if style == 'circlet':                                    # a black headband, gold-edged and studded
            for s, r in f.items():
                fx, fy, fw, fh = r
                if s in ('top', 'bottom'):
                    for j in range(fh):
                        for i in range(fw):
                            p.clear((fx + i, fy + j))
                    continue
                p.face(r, BLACK_SHEEN, LIGHTS[s], noise=3)
                for i in range(fw):
                    p.px((fx + i, fy), c['trim'])
                    p.px((fx + i, fy + fh - 1), GOLD_DEEP)
                    if i % 3 == 1:
                        p.px((fx + i, fy + 1), GOLD)
        if style == 'plaque':                                     # the band's plaque: gold, a red jewel
            gold_box(p, f)
            fx, fy, fw, fh = f['front']
            for i in (fw // 2 - 1, fw // 2):
                for j in (1, 2):
                    p.px((fx + i, fy + j), (220, 30, 30) if j == 1 else (140, 10, 14), glow=j == 1)
        if style == 'mane_a' or style == 'mane_b':
            lock(p, f, keep=0.6, taper=0.28, tone=MANE[style == 'mane_b'], grey=0.0)
        if style == 'horse_neck':
            fur(p, f, HIDE)
            fx, fy, fw, fh = f['front']                           # the throat's veins and the windpipe
            for j in range(fh):
                p.px((fx + fw // 2, fy + j), tone(HIDE, 0.7))
        if style == 'throat':
            fur(p, f, HIDE, light=1.05)
            for s in ('front',):
                fx, fy, fw, fh = f[s]
                for j in range(1, fh, 3):
                    for i in range(1, fw - 1):
                        p.px((fx + i, fy + j), tone(HIDE, 0.75))
        if style == 'horse_skull':
            fur(p, f, HIDE)
            fx, fy, fw, fh = f['top']                             # the mane's root along the poll
            for j in range(fh):
                for i in range(fw // 2 - 2, fw // 2 + 2):
                    p.px((fx + i, fy + j), shade_hair(p, JET))
            fx, fy, fw, fh = f['front']                           # the brow: the blaze starts here
            for j in range(fh):
                for i in range(fw // 2 - 2, fw // 2 + 2):
                    p.px((fx + i, fy + j), noisy(p, BLAZE, 4))
        if style == 'horse_jowl':                                 # the round cheek: a dark rim about it, lit across it
            fur(p, f, HIDE, light=1.0)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                cx, cy = fx + fw * 0.5, fy + fh * 0.45
                for y in range(fy, fy + fh):
                    for x in range(fx, fx + fw):
                        d = math.hypot((x + 0.5 - cx) / (fw / 2), (y + 0.5 - cy) / (fh / 2))
                        if 0.85 < d < 1.05:
                            p.px((x, y), tone(HIDE, 0.35))
                        elif d < 0.5:
                            p.px((x, y), noisy(p, tone(HIDE, 1.6), 3))
        if style == 'horse_face':                                 # the long face: a pale blaze down the bridge, the crest's ridge
            fur(p, f, HIDE)
            fx, fy, fw, fh = f['top']                             # 10x19: the bridge, seen from before and above
            for j in range(fh):
                half = 2.2 - 1.2 * (j / fh)
                for i in range(fw):
                    if abs(i + 0.5 - fw / 2) < half:
                        p.px((fx + i, fy + j), noisy(p, BLAZE, 4))
            for s in ('right', 'left'):                           # sides: darker to the muzzle, the facial crest's line
                fx, fy, fw, fh = f[s]
                for y in range(fh):
                    for x in range(fw):
                        toward_nose = (x / fw) if s == 'right' else (1 - x / fw)
                        if toward_nose < 0.4:
                            p.px((fx + x, fy + y), noisy(p, tone(HIDE, 1.0 - toward_nose * 1.3), 3))
                for x in range(fw // 3, fw):
                    xx = x if s == 'left' else fw - 1 - x
                    p.px((fx + xx, fy + fh // 2 + (x % 4 == 0)), tone(HIDE, 0.4))
            fx, fy, fw, fh = f['bottom']
            p.face((fx, fy, fw, fh), (40, 14, 16), 1.0, noise=3)
        if style == 'horse_nose':                                 # the soft dark nose: flared nostrils, red within
            fur(p, f, HORSE_NOSE, light=1.0, streak=False)
            fx, fy, fw, fh = f['front']                           # 11x8
            for (cx, flip) in ((2, 1), (fw - 3, -1)):
                for j in range(1, fh - 2):
                    for w in range(2):
                        x = cx + flip * (w + (j > 3))
                        p.px((fx + x, fy + j), (14, 6, 6) if w == 0 else (110, 20, 18))
            fx, fy, fw, fh = f['top']
            for i in range(fw):
                p.px((fx + i, fy), noisy(p, BLAZE, 4) if fw * 0.3 < i < fw * 0.7 else tone(HORSE_NOSE, 1.2))
            fx, fy, fw, fh = f['bottom']
            p.face(f['bottom'], (60, 16, 20), 1.0, noise=3)
        if style == 'nostril':                                    # the flared wing of the nostril, its opening red-dark
            fur(p, f, HORSE_NOSE, streak=False)
            for s in ('front', 'right', 'left'):
                fx, fy, fw, fh = f[s]
                for j in range(1, fh - 1):
                    p.px((fx + fw // 2, fy + j), (14, 6, 6))
        if style in ('teeth', 'lowteeth'):                        # big yellowed teeth, gums above, bared
            for s, r in f.items():
                fx, fy, fw, fh = r
                for j in range(fh):
                    for i in range(fw):
                        gap = i % 3 == 2
                        col = (60, 20, 18) if gap else mixc(IVORY, (196, 170, 120), j / max(1, fh))
                        p.px((fx + i, fy + j), noisy(p, k(col, LIGHTS[s]), 3))
        if style == 'horse_jaw':                                  # the lower jaw: hide, a dark chin, the mouth's red within
            fur(p, f, HIDE)
            fx, fy, fw, fh = f['front']
            p.face((fx, fy, fw, fh), HORSE_NOSE[1], 1.0, noise=3)
            fx, fy, fw, fh = f['top']
            p.face((fx, fy, fw, fh), (110, 24, 30), 1.0, noise=4)
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for y in range(fh):
                    for x in range(fw):
                        toward = (x / fw) if s == 'right' else (1 - x / fw)
                        if toward < 0.4:
                            p.px((fx + x, fy + y), noisy(p, tone(HORSE_NOSE, 1.0), 3))
                for x in range(fw):                               # the lip line, drawn back in a snarl
                    p.px((fx + x, fy), (40, 12, 14))
        if style in ('horse_eye', 'ox_eye'):
            for s, r in f.items():
                p.face(r, (40, 10, 8), 1.0, noise=2)
            target = ('right', 'left') if style == 'horse_eye' else ('front',)
            for s in target:
                eye_glow(p, f[s], slant=1 if s != 'left' else -1)
            if style == 'horse_eye':
                fx, fy, fw, fh = f['front']
                for j in range(fh):
                    for i in range(fw):
                        p.px((fx + i, fy + j), EMBER, glow=True)
        if style in ('horse_brow', 'ox_brow'):                    # a scowling ridge over the eye
            fur(p, f, HIDE, light=0.85)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), tone(HIDE, 0.1))
        if style == 'ear':
            fur(p, f, HIDE)
            for s in SIDES:
                point(p, f[s], keep=0.5, down=False, sharp=0.45)
            fx, fy, fw, fh = f['front']                           # inside: dark, hairy at its rim
            for j in range(2, fh):
                for i in range(1, fw - 1):
                    if p.img.getpixel((fx + i, fy + j))[3]:
                        p.px((fx + i, fy + j), noisy(p, (40, 22, 22), 4))

        if style in ('ox_neck', 'ox_hump', 'dewlap'):
            fur(p, f, HIDE, light=0.95)
            for s in ('right', 'left', 'back', 'top'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    for j in range(fh):
                        if (i * 7 + j * 3) % 5 == 0:
                            p.px((fx + i, fy + j), tone(HIDE, 0.4))
            if style == 'ox_hump':                                # a shaggy mane over the hump
                for s in ('top', 'back', 'right', 'left'):
                    shaggy(p, f[s], HIDE, depth=0)
        if style == 'ox_skull':
            fur(p, f, HIDE)
            fx, fy, fw, fh = f['front']                           # the face: curly between the eyes, darker to the edges
            curls(p, (fx + 3, fy, fw - 6, fh // 2), HIDE)
            for j in range(fh // 2, fh):
                for i in range(fw):
                    edge = min(i, fw - 1 - i)
                    if edge < 3:
                        p.px((fx + i, fy + j), tone(HIDE, 0.5 + edge * 0.15))
        if style in ('ox_poll', 'ox_tuft'):
            for s, r in f.items():
                curls(p, r, HIDE)
            if style == 'ox_tuft':
                for s in SIDES:
                    ragged(p, f[s], teeth=2, depth=2, down=False)
        if style == 'ox_forehead':                                # the broad forehead: tight curls, darker to its edges, shadowed under
            for s, r in f.items():
                if s in ('front', 'top'):
                    curls(p, r, HIDE)
                else:
                    fur(p, {s: r}, HIDE)
            fx, fy, fw, fh = f['front']
            for i, j, xy in each(f['front']):
                edge = min(i, fw - 1 - i)
                if edge < 3:
                    p.px(xy, noisy(p, tone(HIDE, 0.4 + 0.2 * edge), 3))
                if j >= fh - 1:
                    p.px(xy, tone(HIDE, 0.25))
            p.face(f['bottom'], tone(HIDE, 0.15), 1.0, noise=2)
        if style == 'ox_cheek':                                   # the great round cheek (the chewing muscle): lit across its swell, rimmed dark
            fur(p, f, HIDE)
            for s in ('right', 'left', 'front'):
                fx, fy, fw, fh = f[s]
                cx, cy = fw * 0.5, fh * 0.42
                for i, j, xy in each(f[s]):
                    d = math.hypot((i + 0.5 - cx) / (fw / 2), (j + 0.5 - cy) / (fh / 2))
                    if d < 0.55:
                        p.px(xy, noisy(p, tone(HIDE, 1.55 - d), 3))
                    elif 0.85 < d < 1.05:
                        p.px(xy, tone(HIDE, 0.4))
        if style == 'ox_face':                                    # the long face: dark hide, lit down its bridge, shadowed down its sides
            fur(p, f, HIDE)
            fx, fy, fw, fh = f['top']                             # the bridge, back to front
            for i, j, xy in each(f['top']):
                d = abs(i + 0.5 - fw / 2) / (fw / 2)
                p.px(xy, noisy(p, tone(HIDE, 1.2 - 0.7 * d + (0.12 if i % 3 == 0 else 0) - (0.25 if d < 0.12 else 0)), 3))
            for s in ('right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(f[s]):
                    if j == 1 and i % 4 != 0:
                        p.px(xy, tone(HIDE, 1.5))                 # the ridge of the face's bone, catching the light
                    elif j > fh * 0.55:
                        p.px(xy, noisy(p, tone(HIDE, 0.55), 3))
            p.face(f['bottom'], tone(HIDE, 0.2), 1.0, noise=2)
        if style == 'ox_ridge':                                   # the bone of the face's bridge, standing up down its middle: lit along its crest
            fur(p, f, HIDE)
            for s_ in ('top', 'front'):
                fx, fy, fw, fh = f[s_]
                for i, j, xy in each(f[s_]):
                    d = abs(i + 0.5 - fw / 2) / (fw / 2)
                    p.px(xy, noisy(p, tone(HIDE, 1.6 - 0.9 * d), 3))
            for s_ in ('right', 'left'):
                fx, fy, fw, fh = f[s_]
                for i, j, xy in each(f[s_]):
                    p.px(xy, tone(HIDE, 0.45))
        if style == 'ox_whorl':                                   # the curly whorl of hair at the top of the face, under the brow
            for s_, r in f.items():
                curls(p, r, HIDE)
        if style == 'ox_muzzle':                                  # the muzzle: hide down the bridge greying to the pale muzzle at the nose
            fur(p, f, HIDE)
            for s in ('top', 'right', 'left', 'front'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(f[s]):
                    if s == 'top':
                        t = j / max(1, fh - 1)                    # 0 at its back .. 1 at the nose
                    elif s == 'front':
                        t = 1.0
                    else:
                        t = (i / max(1, fw - 1)) if s == 'right' else (1 - i / max(1, fw - 1))
                        t = t * 0.8 + 0.4 * j / max(1, fh - 1)
                    if t > 0.45:
                        a = min(1.0, (t - 0.45) / 0.4)
                        col = mixc(tone(HIDE, 1.0), tone(OX_MUZZLE, 1.05 - 0.35 * j / max(1, fh - 1)), a)
                        p.px(xy, noisy(p, k(col, LIGHTS[s]), 4))
            p.face(f['bottom'], (60, 26, 26), 1.0, noise=3)
        if style == 'ox_lip':                                     # the upper lip: pale grey muzzle, whiskered, its edge dark; the mouth red under it
            fur(p, f, OX_MUZZLE, streak=False)
            for s in ('front', 'right', 'left'):
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(f[s]):
                    if j >= fh - 1:
                        p.px(xy, (40, 24, 22))
                    elif (i * 7 + j * 5) % 11 == 0:
                        p.px(xy, tone(OX_MUZZLE, 0.4))           # whisker pits
            p.face(f['bottom'], (120, 30, 34), 1.0, noise=4)
        if style == 'ox_chin':                                    # the chin: pale muzzle, a dark lip along its top, a tuft beneath
            fur(p, f, OX_MUZZLE, light=0.9)
            for s in ('front', 'right', 'left'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    p.px((fx + i, fy), (60, 30, 30))
            shaggy(p, f['bottom'], HIDE, depth=0)
        if style == 'ox_nose':                                    # the wet nose: two great nostrils, a gleam
            PAD = (88, 66, 70)
            for s, r in f.items():
                fx, fy, fw, fh = r
                for j in range(fh):
                    for i in range(fw):
                        mott = 0.85 if (i * 5 + j * 3) % 7 == 0 else 1.0
                        p.px((fx + i, fy + j), noisy(p, k(PAD, LIGHTS[s] * mott * (1.1 - 0.25 * j / max(1, fh))), 4))
            fx, fy, fw, fh = f['front']                           # 16x9: two great nostrils, curving out and down, a groove between
            for (cx, flip) in ((3.5, -1), (fw - 3.5, 1)):
                for y in range(fh):
                    for x in range(fw):
                        dx, dy = x + 0.5 - cx, y + 0.5 - fh * 0.45
                        d = math.hypot(dx / 2.1, dy / 2.6) + 0.18 * flip * dx * (dy > 0)
                        if d < 0.75:
                            p.px((fx + x, fy + y), (12, 4, 6))
                        elif d < 1.05:
                            p.px((fx + x, fy + y), (60, 30, 34))
            for j in range(fh):
                p.px((fx + fw // 2, fy + j), k(PAD, 0.6))
            for (x, y) in ((5, 1), (fw - 6, 1), (6, 1), (fw // 2 - 2, fh - 2)):
                p.px((fx + x, fy + y), (200, 186, 194))
        if style == 'ox_jaw':
            fur(p, f, OX_MUZZLE, light=0.9)
            fx, fy, fw, fh = f['top']
            p.face((fx, fy, fw, fh), (120, 30, 34), 1.0, noise=4)
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                p.px((fx + i, fy), (60, 26, 26))
        if style == 'fang':
            for s, r in f.items():
                p.face(r, IVORY, LIGHTS[s], noise=3, fall=0.3)
            for s in SIDES:
                point(p, f[s], keep=0.5, down=True, sharp=0.6)
        if style == 'ox_ear':
            fur(p, f, HIDE)
            for s in ('front', 'back', 'top', 'bottom'):          # its tip rounded off (the tip at its outer, -x, end)
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(f[s]):
                    t = i if s != 'back' else fw - 1 - i           # 0 at its tip
                    across = abs(j + 0.5 - fh / 2) / (fh / 2)
                    if t < 3 and across > 0.35 + 0.22 * t:
                        p.clear(xy)
            fx, fy, fw, fh = f['bottom']                          # the ear's hollow, beneath and before
            for j in range(1, fh - 1):
                for i in range(1, fw - 1):
                    p.px((fx + i, fy + j), noisy(p, (110, 60, 56), 4))
            fx, fy, fw, fh = f['front']
            for i in range(fw):
                p.px((fx + i, fy + fh - 1), (110, 60, 56))
        if style.startswith('horn') and style != 'horn_band':     # ivory horn: ringed, darkening to a black tip
            n = int(style[-1])
            for s, r in f.items():
                fx, fy, fw, fh = r
                along_x = s in ('front', 'back', 'top', 'bottom')
                for y in range(fh):
                    for x in range(fw):
                        a = (x / max(1, fw - 1)) if along_x else 0.5   # 0 at its base (the box's +x end) .. 1
                        if s in ('front', 'top', 'bottom'):
                            a = 1 - a
                        if s == 'right':
                            a = 1.0
                        if s == 'left':
                            a = 0.0
                        t = ((n - 1) + a) / 4                     # 0 at the root .. 1 at the tip
                        col = mixc((190, 176, 150), IVORY, min(1, t * 2.5)) if t < 0.45 else mixc(IVORY, (40, 32, 30), min(1, (t - 0.45) / 0.5) ** 1.3)
                        across = (y + 0.5) / fh
                        lit = 1.12 - 0.4 * across
                        ring = along_x and (x % 3 == 0) and t < 0.7
                        p.px((fx + x, fy + y), noisy(p, k(col, LIGHTS[s] * lit * (0.82 if ring else 1.0)), 3))
        if style == 'horn_band':                                  # a gold band about the horn's root
            gold_box(p, f)
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for j in range(1, fh, 2):
                    p.px((fx + fw // 2, fy + j), GOLD_LIGHT)
        if style in ('ring', 'ringbead'):
            gold_box(p, f, light=1.1)

    return paint_guard


def shade_hair(p, t):
    return noisy(p, t[1] if p.rnd.random() < 0.6 else t[2], 3)


def save(name, parts, ox):
    for captain in (False, True):
        p = GuardPainter(31 if ox else 37)
        slots = paint(parts, p, guard_painter(ox, captain))
        p.save_as(name + ('_captain' if captain else ''), None if captain else name + '_eyes')
    emit(parts, slots, JAVA, 'ox' if ox else 'horse')
    print(name, len(parts), 'parts,', len(slots), 'boxes')


if __name__ == '__main__':
    from lion_camel import extent
    save('ox_head', OX, True)
    print('   drawn at', OX_SIZE, {kk: round(v, 2) for kk, v in extent(OX, OX_SIZE).items()})
    save('horse_face', HORSE, False)
    print('   drawn at', HORSE_SIZE, {kk: round(v, 2) for kk, v in extent(HORSE, HORSE_SIZE).items()})
