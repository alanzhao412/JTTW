"""The class identities' recipes: every Heavenly armament's tier I and its refinements to VIII, the
Heavenly's own crafts, the Divine Beasts' evolution offerings and their altar. All gated
(jttw_engine:gated_shaped / gated_shapeless / refining / trigrams_refining / trigrams_forging with a
"gate"), so only the class or character named can make them.

Run from the repository root: python assets_source/class_recipes.py
The README's tables are written from the same lists (python assets_source/class_recipes.py --readme)."""
import json
import os
import sys

R = 'src/main/resources/data/jttw_engine/recipe/'
NEZHA, ERLANG, MARSHAL = 'third_lotus_prince', 'eye_of_heaven', 'marshal_of_the_heavenly_canopy'


def item(i):
    return i if ':' in i else 'jttw_engine:' + i


def write(path, data):
    os.makedirs(os.path.dirname(R + path), exist_ok=True)
    with open(R + path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def shaped(path, gate, pattern, key, result, count=1, components=None):
    res = {'id': item(result)}
    if count != 1:
        res['count'] = count
    if components:
        res['components'] = components
    write(path, {'type': 'jttw_engine:gated_shaped', 'gate': gate, 'category': 'equipment', 'pattern': pattern,
                 'key': {k: {'item': item(v)} for k, v in key.items()}, 'result': res})


def shapeless(path, gate, ingredients, result, count=1):
    ings = []
    for name, n in ingredients:
        ings += [{'item': item(name)}] * n
    res = {'id': item(result)}
    if count != 1:
        res['count'] = count
    write(path, {'type': 'jttw_engine:gated_shapeless', 'gate': gate, 'category': 'misc', 'ingredients': ings, 'result': res})


# ── The Heavenly armaments, tier I: a crafting table, the first ring's materials ──
SIDE = {'yin_sword': 'yin', 'yang_sword': 'yang'}
TIER_ONE = {
    # armament: (owner, item, pattern, key)
    'fire_tip_spear': (NEZHA, 'fire_tip_spear', [' IC', ' SI', 'S  '], {'I': 'minecraft:iron_ingot', 'C': 'minecraft:coal', 'S': 'minecraft:stick'}),
    'qiankun_ring': (NEZHA, 'qiankun_ring', [' G ', 'GCG', ' G '], {'G': 'minecraft:gold_ingot', 'C': 'copper_cash'}),
    'yin_sword': (NEZHA, 'yin_yang_sword', [' I ', ' N ', ' S '], {'I': 'minecraft:iron_ingot', 'N': 'minecraft:ink_sac', 'S': 'minecraft:stick'}),
    'yang_sword': (NEZHA, 'yin_yang_sword', [' I ', ' G ', ' S '], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:gold_ingot', 'S': 'minecraft:stick'}),
    'wind_fire_wheels': (NEZHA, 'wind_fire_wheels', ['IFI', 'F F', 'IFI'], {'I': 'minecraft:iron_ingot', 'F': 'minecraft:flint'}),
    'huntian_cloth': (NEZHA, 'huntian_cloth', ['RWR', 'WGW', 'RWR'], {'R': 'minecraft:red_dye', 'W': 'minecraft:white_wool', 'G': 'minecraft:gold_nugget'}),
    'three_pointed_spear': (ERLANG, 'three_pointed_spear', ['IGI', ' S ', ' S '], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:gold_ingot', 'S': 'minecraft:stick'}),
    'erlang_axe': (ERLANG, 'erlang_axe', ['IIT', 'IS ', ' S '], {'I': 'minecraft:iron_ingot', 'T': 'ogre_tusk', 'S': 'minecraft:stick'}),
    'golden_bow': (ERLANG, 'golden_bow', [' GS', 'G S', ' GS'], {'G': 'minecraft:gold_ingot', 'S': 'minecraft:string'}),
    'nine_tooth_rake': (MARSHAL, 'nine_tooth_rake', ['III', 'NSN', ' S '], {'I': 'minecraft:iron_ingot', 'N': 'minecraft:iron_nugget', 'S': 'minecraft:stick'}),
}
OWNED = {NEZHA: [a for a, v in TIER_ONE.items() if v[0] == NEZHA], ERLANG: [a for a, v in TIER_ONE.items() if v[0] == ERLANG],
         MARSHAL: ['nine_tooth_rake']}

# ── Their refinements, II to VIII: each ring's zones' materials (II-IV a crafting table, V-VIII the furnace) ──
REFINE = {
    NEZHA: {  # fire and the lotus
        2: [('servant_bone', 3), ('wraith_wisp', 2), ('monkey_hair', 1), ('captains_sigil', 1)],
        3: [('night_petal', 3), ('spirit_whisker', 2), ('captains_sigil', 1)],
        4: [('minecraft:prismarine_crystals', 2), ('minecraft:prismarine_shard', 2), ('minecraft:nautilus_shell', 1), ('captains_sigil', 1)],
        5: [('hell_bank_note', 4), ('soul_chain', 2), ('underworld_ink', 2), ('captains_sigil', 1)],
        6: [('ember_core', 4), ('furnace_brick', 2), ('true_samadhi_fire', 1), ('captains_sigil', 1)],
        7: [('spider_silk', 3), ('crane_plume', 2), ('daoist_talisman', 2), ('golden_eye', 1)],
        8: [('storm_feather', 3), ('mane_tuft', 2), ('ivory_shard', 1), ('captains_sigil', 2)],
    },
    ERLANG: {  # the hunt and war
        2: [('servant_bone', 3), ('minecraft:leather', 2), ('wraith_wisp', 1), ('captains_sigil', 1)],
        3: [('spirit_whisker', 3), ('night_petal', 1), ('minecraft:iron_ingot', 2), ('captains_sigil', 1)],
        4: [('minecraft:turtle_scute', 2), ('minecraft:prismarine_shard', 2), ('captains_sigil', 1)],
        5: [('ox_horn', 2), ('horse_mane', 2), ('hell_bank_note', 2), ('captains_sigil', 1)],
        6: [('furnace_brick', 3), ('ember_core', 2), ('true_samadhi_fire', 1), ('captains_sigil', 1)],
        7: [('insect_carapace', 3), ('crane_plume', 2), ('golden_eye', 1), ('captains_sigil', 1)],
        8: [('ivory_shard', 3), ('mane_tuft', 2), ('storm_feather', 2), ('captains_sigil', 1)],
    },
    MARSHAL: {  # iron, water and the field
        2: [('minecraft:iron_ingot', 4), ('servant_bone', 2), ('minecraft:porkchop', 1), ('captains_sigil', 1)],
        3: [('spirit_whisker', 2), ('night_petal', 2), ('minecraft:iron_ingot', 2), ('captains_sigil', 1)],
        4: [('minecraft:prismarine_shard', 2), ('minecraft:prismarine_crystals', 2), ('minecraft:nautilus_shell', 1), ('captains_sigil', 1)],
        5: [('minecraft:iron_ingot', 9), ('minecraft:gold_ingot', 3), ('rime_shackle', 1), ('jade_block', 1)],  # Laozi's forging
        6: [('furnace_brick', 3), ('ember_core', 2), ('true_samadhi_fire', 1)],
        7: [('daoist_talisman', 3), ('crane_plume', 2), ('spider_silk', 2)],
        8: [('ivory_shard', 3), ('mane_tuft', 2), ('storm_feather', 2), ('captains_sigil', 1)],
    },
}
FURNACE_FROM = 5

# ── The Heavenly's own crafts (any Heavenly) ──
HEAVENLY = {'class': 'heavenly'}
CRAFTS_SHAPELESS = {
    'five_thunder_talisman': ([('minecraft:paper', 1), ('minecraft:gold_nugget', 2), ('minecraft:redstone', 1)], 2),
    'heavens_net_talisman': ([('minecraft:paper', 1), ('minecraft:string', 2), ('minecraft:gold_nugget', 1), ('minecraft:ink_sac', 1)], 2),
}
ELIXIR = [('minecraft:gold_ingot', 3), ('wraith_wisp', 2), ('immortal_peach', 1), ('servant_bone', 1)]

# ── The Divine Beasts' offerings, by beast and evolution ──
OFFERINGS = {
    'phoenix': {
        20: ('kindled_plume', [('minecraft:feather', 4), ('minecraft:gold_ingot', 2), ('minecraft:coal', 2), ('minecraft:flint', 1)]),
        40: ('wisp_fire_plume', [('minecraft:feather', 4), ('wraith_wisp', 3), ('minecraft:gunpowder', 1), ('captains_sigil', 1)]),
        60: ('candle_flame_plume', [('spirit_mountain_candle', 1), ('night_petal', 3), ('minecraft:prismarine_crystals', 2), ('minecraft:gold_nugget', 2)]),
        80: ('samadhi_plume', [('true_samadhi_fire', 1), ('ember_core', 4), ('hell_bank_note', 2), ('soul_chain', 1)]),
        100: ('sovereign_sun_plume', [('storm_feather', 3), ('crane_plume', 3), ('golden_eye', 1), ('daoist_talisman', 1)]),
    },
    'dragon': {
        20: ('rain_pearl', [('minecraft:ender_pearl', 1), ('minecraft:lapis_lazuli', 4), ('silver_tael', 2), ('minecraft:water_bucket', 1)]),
        40: ('water_curtain_pearl', [('monkey_hair', 3), ('wraith_wisp', 3), ('immortal_peach', 1), ('captains_sigil', 1)]),
        60: ('four_seas_pearl', [('minecraft:heart_of_the_sea', 1), ('minecraft:prismarine_crystals', 3), ('minecraft:prismarine_shard', 2), ('spirit_whisker', 2)]),
        80: ('naihe_pearl', [('underworld_ink', 3), ('soul_chain', 2), ('ember_core', 2), ('captains_sigil', 1)]),
        100: ('dragon_kings_pearl', [('golden_eye', 1), ('spider_silk', 3), ('crane_plume', 2), ('storm_feather', 2)]),
    },
    'white_tiger': {
        20: ('fang_and_pelt_totem', [('wolf_pelt', 2), ('minecraft:bone', 4), ('minecraft:iron_ingot', 2), ('minecraft:leather', 1)]),
        40: ('bone_fang_totem', [('servant_bone', 4), ('minecraft:leather', 2), ('monkey_hair', 2), ('captains_sigil', 1)]),
        60: ('moonlit_whisker_totem', [('spirit_whisker', 4), ('night_petal', 2), ('minecraft:prismarine_shard', 2), ('captains_sigil', 1)]),
        80: ('ox_and_horse_totem', [('ox_horn', 2), ('horse_mane', 2), ('furnace_brick', 2), ('captains_sigil', 1)]),
        100: ('western_mane_totem', [('mane_tuft', 3), ('ivory_shard', 2), ('insect_carapace', 2), ('daoist_talisman', 1)]),
    },
    'turtle': {
        20: ('deepstone_shell', [('minecraft:cobbled_deepslate', 4), ('minecraft:clay_ball', 2), ('ogre_tusk', 1), ('minecraft:turtle_scute', 1)]),
        40: ('ridge_bone_shell', [('servant_bone', 4), ('wraith_wisp', 2), ('minecraft:coal', 2), ('captains_sigil', 1)]),
        60: ('tide_worn_shell', [('minecraft:turtle_scute', 2), ('minecraft:nautilus_shell', 2), ('spirit_whisker', 2), ('night_petal', 2)]),
        80: ('furnace_brick_shell', [('furnace_brick', 4), ('soul_chain', 2), ('hell_bank_note', 2)]),
        100: ('world_turtle_shell', [('insect_carapace', 4), ('ivory_shard', 2), ('daoist_talisman', 2)]),
    },
}
ALTAR = (['ILI', 'CGC', 'SSS'], {'I': 'minecraft:iron_ingot', 'L': 'minecraft:lapis_lazuli', 'C': 'minecraft:candle',
                                  'G': 'minecraft:gold_block', 'S': 'minecraft:polished_deepslate'})


def build():
    for path in ('nine_tooth_rake.json',):  # the old furnace-only Rake: now its tier V
        if os.path.exists(R + path):
            os.remove(R + path)
    for arm, (owner, it, pattern, key) in TIER_ONE.items():
        comps = {'minecraft:custom_data': {'jttw_yy_side': SIDE[arm]}} if arm in SIDE else None
        shaped('heavenly/' + arm + '.json', {'characters': [owner]}, pattern, key, it, components=comps)
    for owner, tiers in REFINE.items():
        short = {NEZHA: 'nezha', ERLANG: 'erlang', MARSHAL: 'marshal'}[owner]
        for tier, mats in tiers.items():
            gate = {'characters': [owner]}
            if tier < FURNACE_FROM:
                ings = []
                for name, n in mats:
                    ings += [{'item': item(name)}] * n
                assert len(ings) <= 8, (owner, tier)
                write('heavenly/%s_tier_%d.json' % (short, tier), {'type': 'jttw_engine:refining', 'gate': gate,
                                                                   'armaments': OWNED[owner], 'tier': tier, 'ingredients': ings})
            else:
                write('heavenly/%s_tier_%d.json' % (short, tier), {'type': 'jttw_engine:trigrams_refining', 'gate': gate,
                                                                   'armaments': OWNED[owner], 'tier': tier,
                                                                   'materials': [{'item': item(n), 'count': c} for n, c in mats]})
    for name, (ings, count) in CRAFTS_SHAPELESS.items():
        shapeless('heavenly/' + name + '.json', HEAVENLY, ings, name, count)
    write('heavenly/nine_turn_elixir.json', {'type': 'jttw_engine:trigrams_forging', 'gate': HEAVENLY,
                                             'materials': [{'item': item(n), 'count': c} for n, c in ELIXIR],
                                             'result': {'id': 'jttw_engine:nine_turn_elixir', 'count': 2}})
    for beast, stages in OFFERINGS.items():
        for level, (name, ings) in stages.items():
            assert sum(n for _, n in ings) <= 9, name
            shapeless('offering/' + name + '.json', {'characters': [beast]}, ings, name)
    shaped('offering/four_symbols_altar.json', {'class': 'divine_beast'}, ALTAR[0], ALTAR[1], 'four_symbols_altar')


def readme():
    def nice(n):
        return n.split(':')[-1].replace('_', ' ')
    def mats(m):
        return ', '.join('%d %s' % (c, nice(n)) for n, c in m)
    for owner, tiers in REFINE.items():
        print('\n' + owner)
        for t, m in tiers.items():
            print('| %d | %s | %s |' % (t, 'furnace' if t >= FURNACE_FROM else 'table', mats(m)))
    for beast, stages in OFFERINGS.items():
        print('\n' + beast)
        for lv, (name, m) in stages.items():
            print('| %d | %s | %s |' % (lv, nice(name), mats(m)))


if __name__ == '__main__':
    readme() if '--readme' in sys.argv else build()
