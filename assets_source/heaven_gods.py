"""Heaven's gods and star lords (its minibosses): the Nine Luminaries, the Great Spirit Giant, the Thunder
Lord, the Lightning Mother, Thousand-Mile Eye and Wind-Following Ear. As hell_kings.py: each one's model
and skins come from one list of parts (beastgen): the model is written into HeavenGodRenderers' layer
methods between their GENERATED markers, each skin painted to the same box-UV layout at twice the model's
resolution (textures/entity/heaven_*.png), with a glowing skin drawn full-bright over it (heaven_*_glow.png).

The parts and the painting of each are in heaven_gods_*.py: _kit (what they share), _luminaries,
_giant, _thunder (the Thunder Lord and the Lightning Mother), _sentinels (the Eye and the Ear).

Run from the repository root: python assets_source/heaven_gods.py [luminaries giant thunder lightning eye ear]"""
import sys

import beastgen as bg
from beastgen import emit
from king_yama import paint
from lion_camel import extent
from heaven_gods_kit import GodPainter, JAVA


def build(tag, parts, skins, size):
    """Paints each (texture name, painter) and writes the model into its layer method."""
    slots = None
    for i, (name, painter) in enumerate(skins):
        p = GodPainter(17 + i * 31)
        slots = paint(parts, p, painter)
        p.save(name)
    emit(parts, slots, JAVA, tag)
    print('%-10s %3d parts, %3d boxes; drawn at %s:' % (tag, len(parts), len(slots), size),
          {kk: round(v, 2) for kk, v in extent(parts, size).items()})


def main(which):
    if 'luminaries' in which:
        import heaven_gods_luminaries as L
        build('luminaries', L.PARTS, [('heaven_star_lord_' + n, L.luminary_skin(i)) for i, n in enumerate(L.NAMES)], L.SIZE)
    if 'giant' in which:
        import heaven_gods_giant as G
        build('giant', G.PARTS, [('heaven_great_spirit_giant', G.paint_giant)], G.SIZE)
    if 'thunder' in which:
        import heaven_gods_thunder as T
        build('thunder', T.THUNDER, [('heaven_thunder_lord', T.paint_thunder)], T.THUNDER_SIZE)
    if 'lightning' in which:
        import heaven_gods_thunder as T
        build('lightning', T.LIGHTNING, [('heaven_lightning_mother', T.paint_lightning)], T.LIGHTNING_SIZE)
    if 'eye' in which:
        import heaven_gods_sentinels as S
        build('eye', S.EYE, [('heaven_thousand_mile_eye', S.painter(True))], S.SIZE)
    if 'ear' in which:
        import heaven_gods_sentinels as S
        build('ear', S.EAR, [('heaven_wind_following_ear', S.painter(False))], S.SIZE)


if __name__ == '__main__':
    main(sys.argv[1:] or ['luminaries', 'giant', 'thunder', 'lightning', 'eye', 'ear'])
