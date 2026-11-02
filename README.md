# JTTW Engine

A NeoForge mod for Minecraft 1.21.1 implementing a full MMORPG progression system rooted in *Journey to the
West* and Chinese mythology — four class archetypes, nine skill trees, an alignment-driven
reincarnation system with twelve evolution outcomes, tiered flight, custom dimensions, and
lore-accurate artifacts and abilities.

Roughly 27,000 lines of Java across 144 source files, written solo. No dependency on any external
powers or animation framework — the engine, the progression system, and the models are all
implemented directly.

---

## The RPG system

### Four class archetypes

Each class in [`ProgressionClass`](src/main/java/com/jttw/engine/progression/stats/ProgressionClass.java)
carries a Base Stat Total and its own XP curve, so they scale differently rather than just having
different numbers:

| Class | BST | Growth curve | Design intent |
|---|---|---|---|
| **Primordial** | 1200 | Erratic | Punishing early game, dramatically outscales everything after level 50. A long-term investment. |
| **Divine Beast** | 1100 | Medium-fast | Naturally strong and stable. +16% stats at each form phase (20/40/60/80/100). |
| **Yaoguai** | 1030 | Fluctuating | Lower stat floor, strong transformation mechanics. Power comes from abilities, not raw stats. |
| **Heavenly** | 1000 | Medium | Widest utility toolkit. Ability-expansion focused: power comes from each character's own skill tree. |

Each class also grows in power in its own shape ([`LevelPower`](src/main/java/com/jttw/engine/progression/stats/LevelPower.java)),
read against a steady character that grows 1.10x a level (its "power level": the steady level as strong):

| Class | Shape | Through the climb | At 100, by stats |
|---|---|---|---|
| **Heavenly** | linear | the steady 1.10x a level itself | third |
| **Yaoguai** | logarithmic | quick early: some eight levels ahead from 20 to 40, flattening after | last: four levels behind the steady line |
| **Primordial** | exponential | weak for long: seven to nine levels behind from 30 to 60, level near 84, then a steep climb (growth 1.07x a level rising to 1.20x) | first: some twelve levels ahead |
| **Divine Beast** | staggered | a leap of 3.45x (some thirteen levels) at each evolution (20, 40, 60, 80), landing five levels ahead, then falling eight behind by the next | second: the last leap, at 100, lands level with the steady line |

The characters with a measure of their own stand off their class's curve by it, reached at
level 100 (`LevelPower.measure`): Wukong 3x, Yuan Di 1.12x, the Nine-Headed Lion 4.7x (his class
ends behind; he does not), Marshal Tianpeng 0.7x (the least of anyone at the top). The three heads of
the Primordials take turns on the way: Peng leads the early climb (some four levels over his class
about level 30), Yuan Di the middle (four over about 55, passing Peng near 44). Wukong is the steepest
of anyone: ten levels under his class, the weakest character there is until the 80s, then a surge
from 75 over 25 levels that carries him past everyone by the 90s and, at 100, to some 2.8x the next
strongest by stats (and his furnace boon besides). Erlang Shen, the Heavenly strongest by stats,
grows as a Primordial does (`LevelPower.erlang`): at the lower end of them, just under Peng and Yuan
Di once they pull ahead (from about 80; some 115 at 100), and never under Nezha's line before that.
Nezha keeps the plain Heavenly line by stats; his kit (the Samadhi fire and the Fire-Tip Spear) is
the stronger of the two.

The Divine Beasts are stat merchants who hit like trucks, each built to its own shape
(`StatBlock.Build`, multipliers on their class's stats; a class's typical character, what monsters
are set against, has none):

| Beast | Health | Attack | Defense (both resistances) | Pace |
|---|---|---|---|---|
| Dragon | ×1.0 (moderate) | ×1.3 | ×1.4 | — |
| White Tiger | ×0.8 (the least) | ×1.45 (the hardest) | — | +20% |
| Turtle | ×1.5 (the most) | ×1.1 | ×1.7 (the most) | — |
| Phoenix | ×1.25 | ×1.1 | — | +15% (and its abilities hit harder) |

Up to level 20, XP comes from ordinary Minecraft mobs, paid as in Pokémon by the level the kill
is won at ([`LevelManager.vanillaMobXp`](src/main/java/com/jttw/engine/progression/level/LevelManager.java)):
weight × 3 × level / 5, at least 1. Animals, villagers and anything else that is no monster pay
nothing; a common hostile (zombie, skeleton, spider, slime) weighs 1; a tougher one (creeper,
enderman, witch, phantom, raiders, guardian) 2; a strong one (blaze, wither skeleton, ghast, ravager,
evoker, piglin brute, breeze) 3; a boss 20. Level 20 takes about 170 common kills for a Heavenly,
210 for a Divine Beast, 260 for a Yaoguai and 370 for a Primordial; from 20 on, only the mod's own
enemies pay, by their zone's band.

### Class identities

Each class plays its own way, on top of its power curve:

- **Heavenly: crafted and refined.** Their strength is their armaments, every one crafted by their
  own hand and refined tier by tier as they climb.
- **Divine Beast: evolution.** They grow by evolving, and each evolution is a ritual they must
  prepare for: no XP until it is done.
- **Yaoguai: ability spammers.** Their abilities hit far harder than their weapons; the right play
  is always to cast.
- **Primordial: normal gameplay.** Unchanged.

What a class's way of fighting adds on top of its stats is its **kit**
([`ClassKit`](src/main/java/com/jttw/engine/progression/stats/ClassKit.java), checked by
`ClassKitTest`). A same-level fight takes three or four ordinary hits; a power level (the steady
line's 1.10x) is worth about a tenth of a hit, seven of them about half the hits. Overall power
(stats times kit), in power levels against the steady line, played as each class is meant to be (a
Heavenly with every armament at the tier of their ring, a Yaoguai casting on every cooldown):

| Character | Lv 30 | Lv 50 | Lv 70 | Lv 100 | Kit |
|---|---|---|---|---|---|
| Sun Wukong | -13 | -15 | -12 | +28 | none (normal gameplay) |
| Yuan Di | -3 | -1 | 0 | +17 | none |
| Gold-Winged Peng | +1 | -3 | -2 | +16 | none |
| **Erlang Shen** | +4 | +4 | +4 | **+18** | +2.5 to +3 levels: three armaments, his axe |
| **Nezha** | +7 | +8 | +8 | **+9** | +7 to +9 levels: six armaments, the Yin and Yang swords |
| **Marshal Tianpeng** | +2 | +2 | +1 | **+1** | +3 levels: his rake |
| Dragon, White Tiger, Black Tortoise | +3 to +8 | +3 | +3 | +3 | none (their leaps are their stats) |
| Phoenix | +4 to +9 | +4 | +4 | +4 | +1 level: its abilities |
| Bull Demon King, Saurian Demon King | +10 | +9 | +5 | -2 | +0.5 level: casting |
| Nine-Headed Lion | +15 | +17 | +16 | +14 | +0.5 level: casting |

So Erlang Shen lands at the lower end of the Primordials at the top (over the Peng and about Yuan
Di, far under Wukong), Nezha's kit is the strongest of the Heavenly's though his stats are the
plain line, the Marshal stays the least at the top, and the Yaoguai stand where they did with more
of their strength in their abilities.

#### Heavenly: crafted and refined

**Every armament is crafted, never found.** Nezha's Fire-Tip Spear, Qiankun (Universe) Ring, Yin
and Yang swords, Wind Fire Wheels and Huntian sash; Erlang Shen's Three-Pointed Spear, axe and
Golden Bow; the Marshal's Nine-Tooth Rake. No loot table or structure hands one out (none did:
they were forged in the Eight Trigrams Furnace from White Bone Ridge's bones, wisps and sigils, a
Java list, and the Rake forged there whole from the Rime Shackle; both are gone, replaced by the
recipes below). `HeavenlyCraftingGameTests` reads every loot table to keep it so.

**Tiers.** An armament has a tier, I to VIII, one for each ring of levels: I below 20, II at 20-36,
III at 37-46, IV at 47-56, V at 57-66, VI at 67-77, VII at 78-88, VIII at 89-100
([`ArmamentTiers`](src/main/java/com/jttw/engine/item/ArmamentTiers.java)). Tier I is crafted at a
crafting table from the first ring's things; each tier after takes the armament one tier short and
its ring's zones' materials, at a crafting table for II to IV and in the Eight Trigrams Furnace for
V to VIII (the furnace burns True Samadhi Fire, or Nezha's own fire). The armament keeps all else
about it (a lit spear stays lit). Its tooltip names its tier, its ring and its next tier's recipe;
the furnace's screen does the same for what is in its centre.

**What a tier is worth.** Two things:

- **Attunement, the class rule** ([`Armory`](src/main/java/com/jttw/engine/item/Armory.java)): a
  Heavenly's health and attack lean on their armaments. Averaged over all of a character's
  armaments (one they lack counts as tier 0), each tier they trail their ring's tier divides both by
  1.3, up to three tiers. With every armament at their ring's tier they are their full selves; a
  tier behind, a same-level foe takes them a hit more to kill and kills them a hit sooner; with
  none at all (three or more behind), they need twice the hits and die in half. Being ahead of the
  ring counts as level with it. So every ring a Heavenly crafts again, and each new tier is a real
  jump: a step of 1.3 on health and attack both.
- **The armament's own bonus**, raised a tenth of itself each tier (x1 at I, x1.7 at VIII), and the
  armory's 6% health and attack for each armament held:

  | Armament | Owner | Own bonus at tier I (on attack in hand, unless worn) | At VIII |
  |---|---|---|---|
  | Fire-Tip Spear | Nezha | x1.75 (lit, x2.25) | x2.28 (lit, x3.13) |
  | Yin and Yang swords | Nezha | x1.4 each, the pair x1.96 | each x1.68, the pair x2.82 |
  | Qiankun Ring | Nezha | x1.25 | x1.43 |
  | Wind Fire Wheels | Nezha | a quarter more speed, worn | x1.7 that |
  | Huntian sash | Nezha | 25 magic resistance, worn | 42.5 |
  | Three-Pointed Spear | Erlang Shen | x1.1 | x1.17 |
  | Erlang's axe | Erlang Shen | x1.15 (grown large, x3) | x1.26 (large, x4.4) |
  | Golden Bow | Erlang Shen | its arrows, as they were | |
  | Nine-Tooth Rake | the Marshal | x1.6 | x2.02 |

  Erlang Shen's old hidden x1.667 on every blow with his spear or axe in hand is gone (his stats
  already grow as a Primordial's); in its place his armaments' tiers. In hits: Nezha with his
  swords at his ring's tier kills a same-level plain Heavenly in one or two blows and takes about
  five of theirs; Erlang Shen at tier VIII kills one in two or three and takes about four.

**Recipes.** Data, in `data/jttw_engine/recipe/heavenly/`, written by
[`assets_source/class_recipes.py`](assets_source/class_recipes.py). Tier I, at a crafting table:

| Armament | Pattern | Key |
|---|---|---|
| Fire-Tip Spear | ` IC` / ` SI` / `S  ` | iron, coal, stick |
| Qiankun Ring | ` G ` / `GCG` / ` G ` | gold ingot, copper cash (goblins) |
| Yin sword | ` I ` / ` N ` / ` S ` | iron, ink sac, stick |
| Yang sword | ` I ` / ` G ` / ` S ` | iron, gold ingot, stick |
| Wind Fire Wheels | `IFI` / `F F` / `IFI` | iron, flint |
| Huntian sash | `RWR` / `WGW` / `RWR` | red dye, white wool, gold nugget |
| Three-Pointed Spear | `IGI` / ` S ` / ` S ` | iron, gold ingot, stick |
| Erlang's axe | `IIT` / `IS ` / ` S ` | iron, ogre tusk, stick |
| Golden Bow | ` GS` / `G S` / ` GS` | gold ingot, string |
| Nine-Tooth Rake | `III` / `NSN` / ` S ` | iron, iron nugget, stick |

Refinements, one recipe per character and tier, taking any of that character's armaments one tier
short (the materials besides the armament; a sigil is any captain's):

| Tier | Where | Nezha (fire and the lotus) | Erlang Shen (the hunt and war) | the Marshal (iron, water and the field) |
|---|---|---|---|---|
| II (20-36) | table | 3 servant bone, 2 wraith wisp, 1 monkey hair, 1 sigil | 3 servant bone, 2 leather, 1 wraith wisp, 1 sigil | 4 iron, 2 servant bone, 1 porkchop, 1 sigil |
| III (37-46) | table | 3 night petal, 2 spirit whisker, 1 sigil | 3 spirit whisker, 1 night petal, 2 iron, 1 sigil | 2 spirit whisker, 2 night petal, 2 iron, 1 sigil |
| IV (47-56) | table | 2 prismarine crystals, 2 prismarine shard, 1 nautilus shell, 1 sigil | 2 turtle scute, 2 prismarine shard, 1 sigil | 2 prismarine shard, 2 prismarine crystals, 1 nautilus shell, 1 sigil |
| V (57-66) | furnace | 4 hell bank note, 2 soul chain, 2 underworld ink, 1 sigil | 2 ox horn, 2 horse mane, 2 hell bank note, 1 sigil | 9 iron, 3 gold, 1 Rime Shackle, 1 jade block (Laozi's forging) |
| VI (67-77) | furnace | 4 ember core, 2 furnace brick, 1 True Samadhi Fire, 1 sigil | 3 furnace brick, 2 ember core, 1 True Samadhi Fire, 1 sigil | 3 furnace brick, 2 ember core, 1 True Samadhi Fire |
| VII (78-88) | furnace | 3 spider silk, 2 crane plume, 2 Daoist talisman, 1 golden eye | 3 insect carapace, 2 crane plume, 1 golden eye, 1 sigil | 3 Daoist talisman, 2 crane plume, 2 spider silk |
| VIII (89-100) | furnace | 3 storm feather, 2 mane tuft, 1 ivory shard, 2 sigils | 3 ivory shard, 2 mane tuft, 2 storm feather, 1 sigil | 3 ivory shard, 2 mane tuft, 2 storm feather, 1 sigil |

**Heavenly crafts**, any Heavenly's alone to make and to use
([`HeavenlyCraftItem`](src/main/java/com/jttw/engine/item/HeavenlyCraftItem.java)):

| Craft | Made | What it does |
|---|---|---|
| Five-Thunder Talisman (五雷符) | crafting table: paper, 2 gold nuggets, redstone (makes 2) | Burned, calls heavenly thunder down on the creature you look at within 24 blocks: three kill a foe of your level. |
| Heaven's Net Talisman (天罗符) | crafting table: paper, 2 string, gold nugget, ink sac (makes 2) | Burned, pins the creature you look at within 16 blocks for 5 seconds (a boss shakes it off in half). |
| Nine-Turn Golden Elixir (九转金丹) | the furnace: 3 gold ingots, 2 wraith wisps, an immortal peach, a servant bone (makes 2) | Eaten, heals you fully, fills your Qi, and gives Regeneration II for 30 s and Resistance I for a minute. |

**The class gate** ([`ClassGate`](src/main/java/com/jttw/engine/item/craft/ClassGate.java)). A
recipe names who may make it: `"gate": {"class": "heavenly"}`, `{"characters": ["third_lotus_prince"]}`,
or both. The types: `jttw_engine:gated_shaped` and `gated_shapeless` (vanilla's shaped and
shapeless with a gate), `jttw_engine:refining` (a tier at a crafting table), and the furnace's
`jttw_engine:trigrams_refining` and `trigrams_forging` (whose gate is optional). At a crafting table
the grid tells the recipe who is crafting (`CraftingMenuMixin`, and NeoForge's crafting player as the
result is taken); a recipe matches only for someone its gate lets through, and with no one known (a
Crafter block) never. The furnace answers to its **smith**, the last to open it. A character's gated
recipes are put in their recipe book as they awaken and at login (refinements stay out of it:
the armament's tooltip names them).

#### Divine Beast: evolution by ritual

At each evolution (levels 20, 40, 60, 80, and the last at 100) a beast that has levelled up to it
stops: it holds the level, but the evolution is owed. Until it is performed it gains **no XP**
(as Wukong at his checkpoints), its power and stats stay the level before's (the leap waits), and its
form and look stay the one before. To evolve, it crafts its **offering** for that evolution and
burns it at an **Altar of the Four Symbols**
([`BeastEvolution`](src/main/java/com/jttw/engine/character/divinebeast/BeastEvolution.java)). Then
the leap, the new form, its health refilled, and XP flowing again, at once. A level an operator sets
owes nothing; evolutions performed are kept through death; a new reincarnation starts over.

- **The altar** (四象坛, a craftable block: no existing place fitted every beast, and a beast should
  carry its altar with it): a stepped altar of dark stone banded with carved clouds, a jade table
  inlaid with the trigrams round a taiji, the Four Symbols' pillars at its corners crowned azure,
  vermilion, white and black, and a two-eared bronze ding on three legs with incense burning in it.
  Crafted by a Divine Beast alone: `ILI` / `CGC` / `SSS` (iron, lapis, candles, a gold block,
  polished deepslate). Used empty-handed, it says what the next ritual wants.
- **Feedback.** Reaching an evolution, the beast is told in chat that its XP has stopped, which
  offering the ritual wants, its recipe, and where its materials come from. Each offering's tooltip
  says whose it is, for which evolution, and where it is burned. The offerings are in the beast's
  recipe book.
- **The ritual.** Three seconds: the beast's element spirals up off the altar in four arms, the Four
  Symbols (flame and lava for the Phoenix, water and glow for the Dragon, white dust and sparks for
  the Tiger, deepslate and moss for the Tortoise), rising to the beast, a bell and a resonance each
  second, louder; then a flash, a burst of light and the beast's own cry (a blaze's roar, thunder, a
  ravager's roar, an anvil falling), a challenge chime, and the new form's name as a title.

The offerings, each crafted by its beast alone from the part of the world that leads to it
(`data/jttw_engine/recipe/offering/`; a sigil is any captain's):

| Evolution | Materials from | Phoenix (feathers and fire) | Dragon (pearls and water) | White Tiger (fang, pelt and mane) | Black Tortoise (shell and earth) |
|---|---|---|---|---|---|
| 20 | the open world: beasts, wild yaoguai, ores | **Kindled Plume**: 4 feather, 2 gold, 2 coal, 1 flint | **Rain Pearl**: 1 ender pearl, 4 lapis, 2 silver tael, 1 water bucket | **Fang-and-Pelt Totem**: 2 wolf pelt, 4 bone, 2 iron, 1 leather | **Deepstone Shell**: 4 cobbled deepslate, 2 clay, 1 ogre tusk, 1 turtle scute |
| 40 | White Bone, Black Wind, Flower-Fruit | **Wisp-Fire Plume**: 4 feather, 3 wraith wisp, 1 gunpowder, 1 sigil | **Water-Curtain Pearl**: 3 monkey hair, 3 wraith wisp, 1 immortal peach, 1 sigil | **Bone-Fang Totem**: 4 servant bone, 2 leather, 2 monkey hair, 1 sigil | **Ridge-Bone Shell**: 4 servant bone, 2 wraith wisp, 2 coal, 1 sigil |
| 60 | the Bottomless Cave, the Mystical Waters | **Candle-Flame Plume**: 1 Spirit Mountain candle, 3 night petal, 2 prismarine crystals, 2 gold nugget | **Pearl of the Four Seas**: 1 heart of the sea, 3 prismarine crystals, 2 prismarine shard, 2 spirit whisker | **Moonlit Whisker Totem**: 4 spirit whisker, 2 night petal, 2 prismarine shard, 1 sigil | **Tide-Worn Shell**: 2 turtle scute, 2 nautilus shell, 2 spirit whisker, 2 night petal |
| 80 | Hell, the Flaming Mountains | **Samadhi Plume**: 1 True Samadhi Fire, 4 ember core, 2 hell bank note, 1 soul chain | **Naihe Pearl**: 3 underworld ink, 2 soul chain, 2 ember core, 1 sigil | **Ox-and-Horse Totem**: 2 ox horn, 2 horse mane, 2 furnace brick, 1 sigil | **Furnace-Brick Shell**: 4 furnace brick, 2 soul chain, 2 hell bank note |
| 100 | Wuzhuang, the Spider Cave, Lion-Camel | **Sovereign Sun Plume**: 3 storm feather, 3 crane plume, 1 golden eye, 1 Daoist talisman | **Dragon King's Pearl**: 1 golden eye, 3 spider silk, 2 crane plume, 2 storm feather | **Western Mane Totem**: 3 mane tuft, 2 ivory shard, 2 insect carapace, 1 Daoist talisman | **World-Turtle Shell**: 4 insect carapace, 2 ivory shard, 2 Daoist talisman |

Every material is already dropped in its stage (turtle scutes and prismarine by the sea's soldiers,
hearts of the sea by yakshas, feathers and gunpowder by Black Wind's imps, leather by its bear
soldiers and wolfmen); no new drop was needed. `BeastEvolutionGameTests` reads each stage's
creatures' loot tables to keep every offering's materials in its stage. The forms the rituals bring:
the Phoenix Flame Guardian, Phoenix, Celestial Phoenix, Vermilion Bird, Solar Sovereign; the Dragon
River Guardian, Azure Dragon, Dragon Prince, Dragon King, Azure Dragon of the East; the Tiger Western
Guardian, White Tiger, Celestial White Tiger, Lord of the West, Byakko; the Tortoise Stone Sentinel,
Mountain Guardian, Earth Titan, Black Tortoise, World Turtle.

#### Yaoguai: ability damage over weapons

A class-wide rule ([`AbilityDamage`](src/main/java/com/jttw/engine/progression/stats/AbilityDamage.java)):
every blow a Yaoguai's ability deals lands x1.5, and every plain weapon hit (from the hand, or an
arrow they loosed) x0.6. A same-level foe that takes three or four ordinary hits takes **six** of a
Yaoguai's weapon hits and **two or three** of their ability hits. A blow struck in an ability's own
body (the Great White Bull, the Nine-Headed Lion's True Form) is the ability's. The **Phoenix**'s
abilities land x1.3 too (its weapon hits as they were).

How a hit is told apart: the damage types do not (an ability's blow is often a plain player attack),
so every ability deals its blows through `AbilityDamage.hurt`, which marks them, on the thread, as
its caster's (the Bull's Rush, wall slam and shockwaves; the Saurian's blows, lash, floods and wrath;
the Lion's heads and roaring shots; the Phoenix's flamethrower, Solar Flare, rebirth burst and
burning thorns). An unmarked hit from the player's hand or projectile is a weapon hit; anything else
(fire ticks, falling, reflections) is neither and is untouched. Casting on every cooldown (a little over half their damage from
abilities), the Yaoguai's kit comes to about x1.1 overall, half a
power level: where they stood, with the strength moved into their abilities.

#### Primordial

Normal gameplay: no change.

### Class passives

Besides each character's own skill tree there is one shared class layer
([`AbilityUnlock`](src/main/java/com/jttw/engine/progression/skilltree/AbilityUnlock.java)), shown on
the character screen's Skills tab (H) and announced in chat as each level is reached. It belongs to
the **Divine Beasts**: their forms come by evolution, each by its ritual at the Altar of the Four
Symbols (see "Divine Beast: evolution by ritual" above), and stand on the list as free milestones, and
between them come passives bought with skill points from the same pool as the tree (1 point below
level 50, 2 from 50, 3 at 100), kept running by
[`BeastPassives`](src/main/java/com/jttw/engine/character/divinebeast/BeastPassives.java):

| Lv | Entry | Cost | What it does |
|---|---|---|---|
| 1 | Lesser Form | free milestone | The beast's first shape, worn from the day you awaken. |
| 20 | Domain Sense | 1 | Hostile creatures that take you as their target within 24 blocks glow for you alone (3 s, renewed while they hunt you). |
| 30 | Guardian's Hide | 1 | +10% physical and magic resistance (a share of the whole, so it multiplies with the domain buff). |
| 40 | Intermediate Form | free milestone | The true beast form. |
| 60 | Domain Authority | 2 | Hostile creatures below your level within 8 blocks get Slowness I and Weakness I, renewed each second while they stay. A creature with no level of its own (a vanilla mob) counts as lower; players, tamed or owned creatures, allies and anything not hostile are left alone. |
| 80 | Sovereign Form | free milestone | The sovereign beast. |
| 100 | Apex Domain | 3, after Domain Authority | The domain reaches 16 blocks, and the creatures it holds deal 20% less damage, to anyone. |

The other classes draw their power from their characters' trees. Of their old class abilities only
the Heavenly cloud is left: **Heavenly Mobility** (Heavenly, level 10, 1 point), a slow cloud to fly
on. The rest were retired
([`RetiredUnlocks`](src/main/java/com/jttw/engine/progression/skilltree/RetiredUnlocks.java)): a save
that still holds one, or a paid-for form, has it cleared at login and its points returned, once,
with a line in chat saying what came back.

Stats resolve at runtime through
[`StatBlock.compute()`](src/main/java/com/jttw/engine/progression/stats/StatBlock.java) from class and
level, rather than being hardcoded per form. There are six: HP, STR, physical resistance (PRES),
magic resistance (MRES), Qi and Will.

Defence is split in two. Physical resistance reduces blows (anything vanilla armor would block);
magic resistance reduces magic, wither, thorns, sonic booms and dragon breath. Both are the mod's
own attributes rather than vanilla armor, so they have no cap. The formula is tuned for fighting
mobs, whose hits are fixed sizes: a hit first loses a flat `resistance / 10`, then the rest is
multiplied by `100 / (100 + resistance)`, and at least 5% of every hit always lands. So at 100
resistance a zombie's 4 does 0.2 while a Warden's 30 still does 10. Damage that already ignores armor (falls, drowning, fire ticks, the void) ignores
resistance too. A target marked magic-vulnerable by the Yin-Yang sword's Yang strike takes 25%
more magic damage for 5 seconds. See
[`ResistanceHandler`](src/main/java/com/jttw/engine/progression/stats/ResistanceHandler.java).

### Reincarnation and evolution lines

Progression isn't a menu choice — it's earned. Four alignment axes accumulate in player NBT from what
you actually do in the world:

**Heavenly · Spirit Beast · Primordial · Yaoguai**

[`ReincarnationResolver`](src/main/java/com/jttw/engine/reincarnation/ReincarnationResolver.java)
reads your **primary and secondary** alignment to determine which of twelve final forms you
reincarnate into:

| Dominant alignment | Evolution outcomes |
|---|---|
| **Primordial** | Celestial Primate · Yuan Di · Gold-Winged Peng |
| **Heavenly** | Eye of Heaven · Third Lotus Prince · Marshal of the Heavenly Canopy |
| **Spirit Beast** | Phoenix · Dragon · White Tiger · Turtle |
| **Yaoguai** | Bull Demon King · Saurian Demon King |

Because the outcome depends on the *pair* rather than a single dominant axis, two players who both
end up Heavenly-dominant can reach entirely different forms.

The Spirit Beast line maps to the **Four Symbols** (四象) — Azure Dragon, Vermilion Bird, White
Tiger, Black Tortoise — and each of the four
[`DivineBeastLine`](src/main/java/com/jttw/engine/progression/stats/DivineBeastLine.java) elemental paths
grants a stat buff while you're in its domain:

- **Water** (Dragon King) — oceans, rivers, and during rain (once he has learned Rain Call, anywhere
  in a world where it rains, roofed over or not)
- **Earth** (World Turtle) — below Y=0
- **Beast** (Byakko) — forests, jungles, plains
- **Fire** (Solar Sovereign) — deserts, taigas, savannahs

### Skill trees

Nine per-subclass trees — Wukong, Nezha, Erlang Shen, Yuan Di, Peng, Dragon, Phoenix, Turtle, Byakko
— built from [`SkillNode`](src/main/java/com/jttw/engine/progression/skilltree/SkillNode.java) records with
prerequisites, skill-point cost, tier, branch grouping, and grid position.

The same node definitions drive unlock tracking in `SkillTreeData` and rendering in
[`SkillTreeScreen`](src/main/java/com/jttw/engine/client/gui/SkillTreeScreen.java). Each character
is one [`PlayableCharacter`](src/main/java/com/jttw/engine/character/PlayableCharacter.java) in its
own package, which owns its tree, which unlocked branch sits in each skill-bar slot and how it
activates (`MENU`, `TOGGLE`, `HELD`, `PASSIVE`), and what each slot's key does: V, B, N and M,
and with Shift held a second page (Shift + V, B, N, M) for a fourth ability, the realm leaps and the
true forms (Yuan Di's Primordial Step, a blink of 8 blocks where he looks for 5 Qi, is on Shift + V;
it was once sprint + sneak, which sneaking broke off).
[`Characters`](src/main/java/com/jttw/engine/character/Characters.java) is the one place a player's
reincarnation outcome is mapped to their character. A passive ability is a new node; an active one
is a new node plus its slot in that character's class.

Levelling runs on a Qi resource system (`QiManager`), with `BiomeLevelBand` and `EnemyTier` scaling
the world to player level.

**Stunned and Pinned.** When a skill says its mark is stunned, pinned, frozen in place, held or
"can't move", it is a full stop, never a heavy Slowness: one shared helper,
[`Stun`](src/main/java/com/jttw/engine/world/Stun.java), and two effects with icons of their own.
*Pinned* takes all speed afoot, afloat and awing and every jump, and a creature drops its path each
tick, but it may still strike what is within reach. *Stunned* is pinned and cannot strike either: a
creature lets go of its target and takes up no other, and no blow, arrow or ability of its own
lands. Both hold players too, and neither stops a push: what is thrown back stands where it lands.
Bosses shake either off in half the time. Stunned: Bull Rush's wall, Predator's Leap III,
Unstoppable Force III, Primordial Smite III, the Bell of Taiyi. Pinned: Territorial Roar, Tidal
Wave III's bubble, the Frost Head's shard (and, against players, Biancheng's bell and the Daoist
disciples' binding talismans).

### Sun Wukong, the Monkey King

From the start, a Wukong player is drawn as the Monkey King himself, not in the player's own skin
([`MonkeyKingModel`](src/main/java/com/jttw/engine/client/renderer/MonkeyKingModel.java), drawn by
`MonkeyKingLayer`). He has no armour or outfit of his own:
- **Head:** the sculpted head the Fighting Buddha also wears
  ([`assets_source/fighting_buddha_head.py`](assets_source/fighting_buddha_head.py)), without the crown.
- **Body:** a lean, muscular monkey's build in golden-brown fur ([`assets_source/monkey_king.py`](assets_source/monkey_king.py)),
  sculpted rather than boxed: shoulders sloping from the neck, a deep chest narrowing through the ribcage to
  a hard waist and flaring a little at the hips, lats and obliques slanting in, shoulder blades and a
  furrow down the spine. The chest and belly are bare tan skin: two pectorals standing out over the ribs,
  the abdomen in six blocks. Tufts of longer fur break his outline: a ruff round the neck, at the
  shoulders, flanks, hips, forearms and down the back.
- **Limbs:** long arms ending in monkey hands, and bowed legs ending in long monkey feet.
- **Tail:** curling up behind him and swaying.
- **Golden fillet (紧箍):** worn on his head. It is part of him, not armour.

He is about a man's size, so real armour is drawn over him as on anyone, and he holds items in his
hands as usual. Everyone sees him this way: the server tells each client which players are Wukong. In
first person his arms are the Monkey King's. His shadow clones are drawn as he is. A transformation
(the 72 and the rest) wins: its form is drawn. At level 100 the Victorious Fighting Buddha takes his
place.

Carrying the staff at ease across his right shoulder (`StaffStance`), the Monkey King and the Buddha both
hold it ([`StaffGrip`](src/main/java/com/jttw/engine/client/StaffGrip.java)). The Monkey King's elbow is
back at his side and his fist is before his breast; his hand turns at the wrist and closes round the
shaft, which runs through the fist from thumb to little finger, the fingers curled over it and the thumb
wrapped over them. The Buddha's arm is raised, his hand in its wide sleeve hooked over the shaft. The
player model's arm takes the upper arm's pose, so armour sleeves follow it, and the drawn forearm folds at
the elbow. The pose was solved so the fist lies on the staff. While he strikes, uses the staff, or holds anything else, his arm
is his own again. In first person his monkey arm is drawn a little smaller, so the long hand doesn't
fill the view.

### The Victorious Fighting Buddha (斗战胜佛)

At level 100, the cap, Sun Wukong becomes the Victorious Fighting Buddha, as the Buddha names him at
the journey's end ([`FightingBuddha`](src/main/java/com/jttw/engine/character/wukong/FightingBuddha.java)).
It is a free milestone, reached by level alone. It is announced once, with a title, a chat message,
a bell and a burst of gold, and the server is told too. A save that is already at level 100 gets the
announcement on its next login. The character screen's Skills tab (H) lists the milestone and its four
powers, and the skill tree (K) shows them beneath the tree. No other character gets any of this, and
neither does Wukong below level 100.

- **Indestructible Body.** Nothing damages him: other players, mobs, bosses, arrows, fire, lava,
  falling, drowning, the void, magic, poison, wither, explosions, lightning and anything else. The hit
  is refused before it lands, so he loses no health, does not flinch and is not knocked back.
  Explosions do not push him either. No harmful effect takes hold, including the mod's Stunned and
  Pinned. Anything that gets past the events anyway (health set down directly, fire, frost, lost air)
  is undone every tick. In the void he is unharmed and is left where he is: he can fall through it or
  stay in it. Only an operator's `/kill` still kills him.
- **Appear and Disappear** (Shift + B, a toggle,
  [`BuddhaVanish`](src/main/java/com/jttw/engine/character/wukong/BuddhaVanish.java)). The server
  holds this state and saves it. While he is vanished, no other player's game is told he exists
  (`ServerPlayerVanishMixin` stops the server pairing him with their clients, as for a spectator), so
  they see no body, armour, held item, name tag, shadow or particles. He is also silent. Every mob
  that was targeting him lets go: its target, its brain's attack target and grudge, and a neutral
  mob's anger. No mob can take him as a target, and mob senses count him as unseen. When he
  reappears, the other players' games are told at once, with a golden flash and a chime. His own HUD
  shows that he is vanished.
- **Breaks Anything** ([`BuddhaBreaking`](src/main/java/com/jttw/engine/character/wukong/BuddhaBreaking.java)).
  Every block his abilities break ignores hardness and every protection. This covers the Ruyi Jingu
  Bang's blasts, its growing and its fall, the Qi Wave and the Fiery Golden Eyes' laser. It includes
  bedrock (the world's floor too), obsidian, reinforced deepslate, the mod's tough stone, the
  Bottomless Cave's rock, Lion-Camel's fabric and gates, and the technical blocks: command, structure
  and jigsaw blocks, barriers, light blocks, and end portals and gateways. There are no exceptions.
  - Carving budgets still apply, but no block costs him more than stone does.
  - No block resists his blasts more than stone, so they make the crater they would make in stone.
  - His shadow clones' mining breaks the same blocks, bedrock included, at the pace of stone.
- **Somersault Anywhere** (Shift + M, a menu,
  [`SomersaultTravel`](src/main/java/com/jttw/engine/character/wukong/SomersaultTravel.java)). The key
  asks the server for the menu, which lists:
  - every online player, in any realm
  - the world spawn and his respawn point
  - each realm
  - each zone's region (the four seas each) and its boss's lair
  - Fengdu's Gate of Ghosts, and Mount Kunlun's palace and Jade Pool
  - Hell's arrival and each of the Ten Courts
  - Heaven's arrival and the Jade Palace
  - typed coordinates (x, y, z and a realm)

  The server finds where the choice is and sets him down at the nearest spot with solid ground and
  room for his body. If there is none, it uses the nearest open space, then the top of the column. If
  he is enclosed in rock, room is cleared for him. He never lands inside a block. A golden streak
  rises where he leaves and falls where he lands (nothing shows if he is vanished). The cooldown is
  3 seconds.

He looks the part ([`FightingBuddhaLayer`](src/main/java/com/jttw/engine/client/renderer/FightingBuddhaLayer.java),
model and skins from [`assets_source/fighting_buddha.py`](assets_source/fighting_buddha.py)). Once he
ascends, his own body is no longer drawn. His armour and cape are hidden too (`BuddhaArmourMixin`,
`BuddhaCapeMixin`). In their place he is drawn as the Monkey King made Buddha, a little larger than a
man:
- **Head** (its own model at twice the detail, [`fighting_buddha_head.py`](assets_source/fighting_buddha_head.py)), built up in steps so nothing reads as a box: a skull rounded in tiers under a dome, overlapping locks of golden fur over the crown, temples and nape; a heart-shaped face of warm tan recessed in its fur frame and blending into it; a sculpted brow ridge drawn down in a frown and stepped back into the forehead's fur; the Fiery Golden Eyes as small almonds set in the shadow of their sockets (golden irises, dark pupils, a faint fiery rim, glowing); a muzzle stepped out in three narrowing tiers in the face's own skin, with nostrils, upper lip, a mouth running round it with a smirk, and a chin; round ear cups with a curled rim and a shadowed bowl, half in the fur.
- **Crown:** the golden fillet, now a jewelled Buddha's crown of five lotus-petal plaques, with red
  silk ribbons flaring behind.
- **Robe:** a crimson monk's robe with a crossed white collar and wide gold-hemmed sleeves, falling
  past the knees to cloud shoes.
- **Kasaya:** worn over the left shoulder with the right bare. It is patched in saffron and gold
  between gold-thread seams, with a rolled edge and folds across the breast and back, a gold ring
  clasp, a hanging panel on the left arm, and a gold-thread cord with tassels. Its apron and panels
  hang past the knees.
- **Ornaments:** prayer beads of bodhi wood with a jade guru bead, wrist malas and a gold armlet.
- **Staff:** the Ruyi Jingu Bang in red lacquer banded in gold, with gold end caps. He holds it
  upright in his hand, or carries it across his shoulder when at ease.
- **Light:** behind his head a halo with a disc of light, an inner and a beaded outer ring of gold,
  and two layers of golden flame. Behind his body is an almond-shaped mandorla of light, pointed at
  the top, edged in gold and fringed with flame. The light is drawn full-bright, and motes of gold
  drift round him.

### Shadow Clones

Sun Wukong's hair clones (Shadow Clone I–III: B summons one more, 10 Qi, up to 1 / 2 / 9 at
once; they drain Qi while out). Each is a second Wukong with his health, attack, resistances and
weapon, and copies of his best pickaxe and axe when it works. It acts as a player would
([`WuCloneManager`](src/main/java/com/jttw/engine/character/wukong/WuCloneManager.java),
[`CloneMining`](src/main/java/com/jttw/engine/character/wukong/CloneMining.java),
[`CloneCombat`](src/main/java/com/jttw/engine/character/wukong/CloneCombat.java),
[`CloneSense`](src/main/java/com/jttw/engine/character/wukong/CloneSense.java)).

**Commands.** All of them answer on the action bar.
- **Sneak + use a clone**: cycles that clone through **Follow me** → **Work here** → **Guard
  here**. Work and Guard take the spot where the clone stands.
  - *Follow me*: it keeps with you, fights at your side, and gathers what lies open near you
    (within 8 blocks).
  - *Work here*: it mines, fells and harvests within 16 blocks of the spot (10 up or down). It
    also digs to buried ores. When there is nothing left to do, it comes back and hands you what
    it gathered.
  - *Guard here*: it holds the spot and fights whatever comes near.
- **Use a clone**: opens its pack (27 slots). Only you can.
- **B at the cap**: calls every clone back. Each drops what it was doing, follows you again,
  and hands you its pack when it reaches you. One that is far off appears by you.
- **Hit a clone**: dismisses it. Its pack spills where it stood.

**Gathering.**
- *What it takes*: ores it has a good enough pickaxe for, ripe crops (it replants them), and
  trees it has an axe for. A tree is logs that its own grown leaves touch. Logs without them,
  such as a log house, are left alone.
- *Finding work*: it sweeps its area a layer at a time. A following clone takes only what lies
  open to the air. A working one also finds what is buried.
- *Sharing work*: it claims a whole vein (or tree), every connected block of it. Its fellows go
  to other veins, so two clones never work one block. It weighs the rarest ores first and veins
  away from where the others already are. It finishes a vein, including blocks that were hidden
  until it opened them.
- *Reach*: it works a block from a player's reach, 4.5 blocks from its eye to the nearest point
  of the block, with the face in sight. If it can already do that, it works from where it
  stands. Otherwise it picks a spot from which it could, never on the block or its vein, and
  paths there. When something is in the way within reach, it digs through it.
- *Hard-to-reach work* (Work here only): it tunnels, two blocks high, level or in stairs up and
  down. It bridges gaps with stone or dirt from its pack. It pillars up under what is overhead,
  and afterwards breaks its pillar back down and takes the blocks with it.
- *What it digs*: natural ground only (stone, deepslate, netherrack, dirt, sand, gravel, leaves
  and the like), never anything built. Each break goes through the break event a player's would,
  as you, so protected places stay protected. Ordinary clones respect hardness: bedrock, obsidian
  and the lairs' rock hold. The Fighting Buddha's clones break those too (see below).
- *Safety*: it never breaks a block that holds back lava or water. It never breaks the floor
  under its feet or under the path it is taking. It never digs out sand or gravel that would
  fall on its head. It does not stand next to lava or fire.
- *Giving up*: what it cannot reach, it looks at again a few times, then leaves alone for two
  minutes.
- *The pack*: what it breaks goes straight into its pack. A working clone also picks up what
  lies loose round its work, such as saplings and apples from leaves. A nearly full pack goes
  into the nearest chest or barrel, or, with none about, its best finds are sent to you.

**Keeping up.**
- A following clone takes its own place on an arc behind you, so the clones never crowd you or
  push you.
- If you leave it more than 20 blocks behind (by walking, flying or a somersault), it appears by
  you. If you are in the air, it lands on the ground below you. If you change realm, it follows.
- If it gets stuck, it first jumps and works out its path again, then digs through plain ground
  in its way, then comes back to you (or to its work site).
- If it ends up in lava, inside a wall or in the void, it is taken out at once.
- It never jumps down more than three blocks.
- Path-finding is bounded: at most 4 new paths per tick across all clones, and every sweep and
  search is limited in size, so nine clones do not slow the server.

**Fighting.**
- *What it fights, in order*: what is attacking you, then what you attack (another player only
  while you are fighting him, and only where PvP allows), then what attacks it, then hostile
  creatures near you (or near its post).
- *What it leaves alone*: you, your other clones, your pets and summons, those allied to you,
  and other players' pets unless they join the fight. A neutral creature is left alone until it
  turns on you or the clone.
- *Spreading out*: each enemy already fought by one of your clones counts against it, so
  several clones split between several enemies. Clones on one enemy surround it from evenly
  spread sides instead of queueing.
- *Hurt*: badly hurt, it lights its own Ring of Fire if you have that skill and it has the Qi.
  If it cannot, it falls back behind you and heals half a heart a second while out of the fight,
  then goes back in at 60% health. If cornered, it fights where it stands.
- *Leash*: it does not chase more than 24 blocks from you (or its post).

### The 18, 36 and 72 Transformations

Three transformation arts, each with its own menu and its own forms per level. A character practises
at most one (Wukong, Erlang Shen and the Bull Demon King the 72, the Marshal the 36; the four Divine
Beasts, the Azure Dragon, Vermilion Bird, Black Tortoise and White Tiger, and the Saurian Demon King
the 18), and its level comes from that character's skill tree. Each art's menu is on M once its first
node is learned; a Divine Beast's true form stays on Shift + M.

| | Level 1 | Level 2 | Level 3 |
|---|---|---|---|
| **18** | neutral land | + neutral water | + neutral air |
| **36** | neutral land and water | + neutral air | + hostile land |
| **72** | all neutral forms | + hostile land (no bosses) | + bosses, hostile sea and sky, objects, other players |

[`TransformationSchool`](src/main/java/com/jttw/engine/transformation/TransformationSchool.java) holds
the table, [`FormClassifier`](src/main/java/com/jttw/engine/transformation/FormClassifier.java) sorts
every entity into a category, and the menu and the server's check both ask the same
[`TransformationAccess`](src/main/java/com/jttw/engine/transformation/TransformationAccess.java), so the
menu only ever shows what the server will grant. A player form wears another online player's skin,
model and cape, and shows their name.

The player, not the creature's AI, drives a form.

Some forms are no art's but a character's own ability's, and no menu offers them (`FormCatalog.ABILITY_FORMS`):
the Bull Demon King's **Great White Bull**, and the Grand Saint of Nine Spirits' True Form (M), in which
he becomes the **Nine-Headed Lion** (`NineHeadedLionEntity`, 2.8 by 3.6 blocks, his heads rising
far over it): a vast golden lion, the Taiyi Heavenly Worthy's mount, in a great shaggy mane, a jewelled
gold breast-collar hung with bells and a celestial-blue saddle-cloth, nine maned necks fanned from his
shoulders in two ranks, the middle head the largest, each a guardian lion's with spiral curls, a
scowling brow and smouldering gold-ember eyes. The heads that strike with each of his blows (2 / 3 / 4
by True Form's tier) are awake, their eyes burning full (any client reads the tier from his
toughness); as he strikes or roars they lunge and bite while the rest rear back roaring. Its model and
skin come from [`assets_source/nine_headed_lion.py`](assets_source/nine_headed_lion.py). Leaving the
form, or being made another shape, ends it.

**Objects** (72, level III) are entities, such as minecarts, arrows and boats. Some entities
aren't offered: the ones with no body to be (lightning, a fishing bobber, markers, empty display
frames, a dropped item with no item, evoker fangs, an ominous item spawner with no item), primed TNT, and the mod's own machinery (Qi Wave, the thrown spear).
[`FormCatalog`](src/main/java/com/jttw/engine/transformation/FormCatalog.java) is the one list that
the menus, the server and the tests share. Some objects and blocks behave like the real thing.
Right-click with an empty hand to use them:

| Form | What it does |
|---|---|
| **Chest** | Opens your own 27-slot chest, kept on you through death and form changes, like a shulker box. |
| **Sign** | Write up to four lines; the sign shows them. |
| **TNT** | Light it yourself, or another player lights it with flint and steel. After 4 s it explodes like TNT, without hurting you, and you return to your own form. |
| **Boat** | Two players can ride in you (sneak to get off) while you steer; on water you float and sail at a boat's 8 blocks a second. A chest boat seats one and opens your chest. |
| **Creeper** | Its blast breaks blocks like a creeper's (the mobGriefing rule applies), and you stay a creeper. |

### Lore-accurate artifacts and abilities

Abilities and items are drawn from the source material rather than invented:

- **Ruyi Jingu Bang** — Wukong's staff, with the size-shifting behaviour it has in the novel
- **Wind Fire Wheels · Huntian Cloth · Fire Tip Spear · Qiankun Ring** — Nezha's arsenal
- **Three-Pointed Spear · Erlang Axe** — Erlang Shen, whose form is *Eye of Heaven* after his third eye
- **Immortal Peach · Yin Yang Sword**
- **The signature weapons and treasures, and where they are come by** (none of them in any other
  loot, vanilla's or ours):
  - **Nine-Tooth Rake** (九齿钉耙, the Marshal of the Heavenly Canopy's): crafted plain at a crafting
    table (tier I) and refined tier by tier like every Heavenly armament (see "Heavenly: crafted and
    refined"); at **tier V** it is re-forged in the Eight Trigrams Furnace as Laozi forged it, from
    **9 iron ingots, 3 gold ingots, a Rime Shackle** (King Chujiang's ice-iron, from the Second
    Court's cold hell) **and a jade block**, True Samadhi Fire burning. The furnace's recipes are data
    (`data/jttw_engine/recipe/`, types `jttw_engine:trigrams_forging` and `trigrams_refining`:
    [`TrigramsRecipe`](src/main/java/com/jttw/engine/world/furnace/TrigramsRecipe.java)), and its
    screen lists them.
  - **Mixed-Iron Cudgel** (混铁棍, the Bull Demon King's): crafted cheaply at a crafting table, two
    iron ingots and two iron nuggets on a diagonal over a stick (` NI` / ` IN` / `S  `). A plain
    iron club: 7 attack at 1 swing a second, 250 durability, uncommon (it was netherite-tier, 11
    attack at 0.8, epic and fireproof); the Bull Demon King's shockwave is his own, in any cudgel.
  - **Bell of Taiyi** (太乙钟): in the Jade Palace, one in all Heaven: the east one of the two
    treasury chests beside the throne (`chests/jade_palace_treasury`, the palace's loot and the bell,
    always). One palace a world, so one bell.
  - **Yin-Yang Vase** (阴阳二气瓶, the Great Peng's): at the bottom of the Lion-Camel Cave alone, in
    the demon kings' treasury, a chest on the throne room's dais beside the Great Peng's empty
    throne (`chests/lion_camel_treasury`: the vase, always, among gold, gems, bones and the ridge's
    spoils). One fortress a world, so one vase.
  - **Banana-Leaf Fan**: one a world, in the treasure chest of the palace on Mount Kunlun's summit (see Mount Kunlun).
- **Wu Clones** — Wukong's hair-clone technique: summoned clones that follow, work and guard,
  mining from a player's reach and fighting at his side (see "Shadow Clones" above)
- **Fiery Golden Eyes** (火眼金睛) — Wukong's eyes from the Eight Trigrams Furnace, on N once he
  has them. They always show the true name and health of whoever he looks at. **Shift + N** turns
  the x-ray on and off (ores through stone, 1 Qi a second). **Tap N** to fire the laser: it fires
  the moment the key comes up, with no charge-up. **Hold N and turn the mouse wheel** to set its
  output instead: up for more, down for less. Nothing fires while you do it, and the hotbar stays
  where it is. The new output is shown on the action bar, and five pips over slot N show it all
  the time. The output is saved with him and kept through death. It starts at I.
  ([`EyeLaser`](src/main/java/com/jttw/engine/character/wukong/EyeLaser.java))

  | Output | Qi | Range | Cut | Hit at level 1 | Same-level character, hits to kill | Fiery Gaze: beat a second at level 1, burns a same-level character to death in | Slowness | Furnaces | Cooldown |
  |---|---|---|---|---|---|---|---|---|---|
  | I, Glint | 10 | 20 | 1 block (the line itself) | 8 | 4 | 0.5, about 50 s | I | lit | 3 s |
  | II, Gaze | 20 | 30 | a cross 3 wide, 2 tall when level | 13 | 3 | 0.8, about 35 s | I | lit | 4 s |
  | III, Glare | 35 | 40 | 3 × 3 | 20 | 2 | 1.2, about 20 s | II | cut | 5 s |
  | IV, Blaze | 55 | 52 | a round bore 5 wide | 32 | 1 | 1.8, about 15 s | II | cut | 7 s |
  | V, Sunfire | 80 | 64 | a round bore 7 wide | 50 | 1, with room to spare | 2.5, about 10 s | III | cut | 10 s |

  - **Damage.** The hit scales with his power (`LevelPower.scaled`), so the table's hits-to-kill
    hold from level 1 to about 60. They count a typical character of his own level, before
    resistance. For comparison, an ordinary weapon hit takes about 3. Past level 60 the hits grow
    fewer, because Wukong's own power outgrows a typical character's (output I kills in 2 at level 80). Each creature in the beam is struck once.
    His own clones are spared.
  - **Cutting.** Every block within the cut of the beam's axis goes. The cut flares out from his
    eyes over the first few blocks, so it never takes the ground from under his feet. Fire is left
    along the floor of the bore and all round where the beam ends. Up to output III the blocks drop
    as if mined. At IV and V they are burnt away to nothing.
  - **Its fire never goes out** ([`FieryGaze`](src/main/java/com/jttw/engine/character/wukong/FieryGaze.java),
    the effect "Fiery Gaze"). Whatever the beam strikes burns until it dies. Water, rain, Fire
    Resistance and milk do not put it out; only a totem's death-defiance clears it, as it clears
    every effect. It is seen burning even under water. Each second it takes the beat in the table,
    scaled by his power when he fired, and it slows the burning one (Slowness by output, as long as
    it burns). A second shot keeps the fiercer fire; fires never stack. Bosses burn too: against a
    boss's great health it is only a slow chip (output V alone would take some twenty minutes to
    burn down a boss of his level). The hit also sets ordinary fire for 3–10 s by output.
  - **Furnaces.** At outputs I and II the beam does not cut a furnace, blast furnace or smoker: it
    stops there and lights it with a lava bucket's worth of fuel (a hundred items: 20,000 ticks in a
    furnace, 10,000 in the faster two), if it had less. The Eight Trigrams Furnace is lit as Nezha's or
    the Phoenix's true fire lights it. From output III furnaces are cut like any block.
  - **What stops it.** Bedrock and other unbreakable blocks always stop the beam. Obsidian-hard
    blocks (hardness 50 or more) stop it below output V, and V burns through them. Nothing past the
    block that stopped it is touched. Liquids do not stop it, and it leaves them as they are. As the
    Victorious Fighting Buddha, his beam breaks anything at every output and nothing stops it.
  - **How it looks.** Every player within 160 blocks is told of the shot (`EyeLaserBeamPacket`) and
    draws it at once ([`EyeLaserRenderer`](src/main/java/com/jttw/engine/client/renderer/EyeLaserRenderer.java)).
    Two fine golden beams leave his two eyes and follow his head, including a transformed form's
    head height. In his own first-person view they leave from just below and either side of the
    view. They meet a little ahead of him in one shaft of fire that runs to where the beam stopped.
    The shaft has a white-hot core inside gold, orange and a red haze, with pulses racing down it.
    It flashes as it fires, then fades and narrows over about half a second. Along it fly sparks,
    flames and lava. Chunks of the burnt blocks fly off in their own colour, red-hot and cooling,
    with crumbs. Where the beam ends there is a flare with rays and a burst of light, and the faces
    there glow and then char, cooling over seven seconds. The sound is a deep, searing roar (a sonic
    boom with a blaze's shot under it), deeper at higher outputs, with a hiss and, from III up, a
    blast where it ends.
- **Xiaotian Quan** — Erlang Shen's hound, a custom entity with hand-authored model and animation.
  Every bite slows (Slowness I, 2 s). Its own branch: I summons it; II gives it +25% health and
  attack and tracking sense (the nearest hostile glows for him); III +50%, its bites' Slowness I
  lasting 3 s, and a howl that recharges twice as fast. Hound Training: I +20 health, +3 attack; II
  +20 health more, and it tracks the three nearest hostiles; III peak stats and bites that give
  Slowness II for 3 s (the strongest Slowness wins; they never stack) ([`XiaotianQuanManager`](src/main/java/com/jttw/engine/character/erlang/XiaotianQuanManager.java))
- **Zhàn Shén** — Erlang Shen as God of War: below 40% health he is in Wrath (gone again once healed
  to 40%), with +15/25/35% damage on every blow and ability, and as much movement and attack speed,
  whatever he holds. At II he cannot be knocked back and Xiaotian Quan goes berserk (crimson
  markings and eyes, +30% bite, +25% speed, a bite every half second, and it hunts the nearest
  hostile); at III he heals 10% of the damage he deals and the hound's bites give Slowness II for
  5 s (its own nodes give at most 3 s). A
  red vignette beats at the edge of his sight while it lasts
  ([`ErlangWrath`](src/main/java/com/jttw/engine/character/erlang/ErlangWrath.java))
- **Phoenix revival** — death-and-rebirth mechanic tied to the Phoenix line

Custom **Heaven** and **Hell** dimensions ship with their own worldgen, biomes, and noise settings.
Heaven is a sky of cloud islands with the Immortals' pavilions, the Queen Mother's peach gardens
and the Jade Palace, held by Heaven's host: wild grass of every level from 1 on the outer clouds to
100 at the palace, reached by anyone with a Heaven Pass (see "Heaven" under Zones). Hell (Diyu) is a dark underworld of ash plains, black stone hills and rivers,
crossed by the Naihe, with the arrival plaza where the Book of Life and Death is kept and, far to
the north, the fortress of the Ten Courts: the levels 57–66 zone (see "Hell" under Zones). Sun
Wukong is sent there at level 30 to strike his name from the Book; the Dragon's and Wukong's
dimension abilities travel there too; and anyone at all may go down through the Gate of Ghosts of
Fengdu, the Ghost City, at the world's far edge, once they have come to it by an Ancient City's
frame, lit with soul fire, and fought it open; they come back up by the side doors of Hell's own
(see "Fengdu" under Zones).

---

## The engineering

If you're skimming to see how it's built, these are the three problems worth reading about.

### 1. Overriding engine internals without forking

Minecraft's movement, collision, and rendering behaviour lives in classes that aren't designed to be
extended. The usual options are to fork the game's source — unmaintainable across versions — or to
work around it from outside, which is fragile and often impossible.

This uses [SpongePowered Mixin](https://github.com/SpongePowered/Mixin) to instrument bytecode at
class-load time: 14 Mixin classes registered in
[`jttw_engine.mixins.json`](src/main/resources/jttw_engine.mixins.json), split across injectors,
accessors, and invokers.

The clearest example is phasing through blocks, which took two separate injections:

- [`BlockStateBaseMixin`](src/main/java/com/jttw/engine/mixin/BlockStateBaseMixin.java) makes blocks
  return an empty collision shape for a phasing entity.
- That alone isn't enough. `Entity.moveTowardsClosestSpace()` runs an independent pass that detects
  block overlap and *teleports* the entity to the nearest clear space — so a phasing player gets
  ejected the moment they enter a wall.
  [`EntityPhaseMixin`](src/main/java/com/jttw/engine/mixin/EntityPhaseMixin.java) injects at `HEAD`
  of that method and cancels it when the entity can phase.

Two injections into unrelated classes, required together, because the engine enforces the same
invariant in two places.

### 2. A custom flight and movement system

613 lines across [`JTTWFlight`](src/main/java/com/jttw/engine/flight/JTTWFlight.java) and
[`ClientFlightHandler`](src/main/java/com/jttw/engine/client/ClientFlightHandler.java), written
because vanilla creative flight and elytra gliding both felt wrong for cloud-riding.

It maintains a persistent velocity vector in player NBT rather than applying motion directly, which
is what makes the rest possible:

- **Turn-rate limiting.** Direction changes interpolate toward the input at a fixed rate per tick
  rather than snapping, so flight has weight instead of feeling like a free camera.
- **Momentum decay.** With no input, velocity decays by a constant factor per tick until it falls
  below a cutoff — and the same decay continues after flight ends, so landing reads as a continuous
  glide rather than an abrupt stop.
- **Speed tiers.** Glide-only (sinks, cannot gain altitude), full flight, and a top tier that also
  permits dimension travel (Shift + V: the Dragon's to the realm it picks, Wukong's Somersault Cloud
  to the next on the wheel of Overworld, Nether, End, Hell and Heaven, 20 Qi a leap; both come down
  at the realm's own arrival, [`RealmArrival`](src/main/java/com/jttw/engine/dimension/RealmArrival.java):
  Hell's arrival plaza and Heaven's arrival, as every traveller's; in the Nether, near the place
  scaled as a portal scales it (x / 8, z / 8), the nearest ground within 16 blocks with two blocks
  of air over it, not lava nor magma, under the bedrock roof (below y 120), or else an obsidian
  platform at y 65; in the End, its obsidian platform, laid as for the end portal; in the Overworld,
  the surface where one was (eight times out from the Nether), or, on water, lava or nothing, one's
  bed or anchor, else the world's spawn) — with
  per-character speed variation layered on top (Erlang Shen's Wind and Fog, Sun Wukong's Cloud Step
  and the Phoenix's Rising Ember are true flight from their first tier, a little under cruising
  speed, never a glide; the Dragon's Elytra Glide stays a glide, but only in its own shape: a
  form with wings, the Azure Dragon or a flying form of the 18 Transformations, always flies; Nezha
  flies on his Wind Fire Wheels at cruising speed, and at the fastest with Blazing Trail).
- **Momentum.** Above every tier, a flier held at full speed in their highest flight (the top tier
  of their flight line, or a form's wings or a Heavenly's cloud, which have none) builds over 10
  seconds to their own top speed, and loses it in 2 when they fall short
  ([`FlightMomentum`](src/main/java/com/jttw/engine/flight/FlightMomentum.java)): Sun Wukong and
  Erlang Shen 150 blocks a second; Nezha 120; the Dragon, the Phoenix, Heaven's other beings and
  everyone else 100; the great bulky ones (the Bull Demon King, the Saurian Demon King, the Turtle,
  the Grand Saint of Nine Spirits, the Marshal of the Heavenly Canopy, Yuan Di) 65. The Gold-Winged Peng builds to 150 his own way, by his
  Ninety-Thousand-Li wingbeats.
- **The Great Void.** Wukong and the Peng fly faster than the world can be generated (about 80
  blocks a second over new land on a 16-core machine), so they also have a key (Y) into
  [`GreatVoid`](src/main/java/com/jttw/engine/dimension/GreatVoid.java): an empty dimension laid
  one to one over every other, nothing generated in it, loaded only three chunks round a
  traveller, where their momentum builds to 1000 blocks a second. Empty chunks still cost the
  server something, so on that machine about 500 a second is made in practice (485 measured over
  never-visited space, against 72 in the Overworld at 200). One goes in at the same x, y, z and
  comes out at the same x, z, in the realm one came from, at one's own height, or the nearest room
  above or below if that is inside something. In the Void one always flies and is held up at its
  floor.
- **Independent velocity clamps** on the vertical and horizontal axes, plus a separate strafe scale,
  so lateral drift doesn't match forward thrust.
- **A parallel swim system** with its own stored vector, distinct cruise and sprint speeds, and a
  different acceleration constant.

Every tuning constant is a named field at the top of the file, because movement is iterative and
magic numbers buried in logic are miserable to adjust.

### 3. Keeping state consistent across clients

Transformation, progression, and ability state has to agree between the server and every client that
can see the player — otherwise your model desyncs from your hitbox, or a cooldown looks ready on one
screen and isn't on another.

37 packet types in [`network/`](src/main/java/com/jttw/engine/network/) handle this, most of them
one-way server→client syncs for a specific slice of state (level, Qi, cooldowns, skill tree, ring
state, transformation), plus client→server requests for actions the server must authorize. They are
split into `network/s2c` and `network/c2s`; packets only encode and route, and the rules they trigger
live with the system they belong to.

State persists in three places deliberately — server memory, player NBT, and a client-side mirror —
so progression and transformations survive logout, death, and dimension changes.

---

## Models and animation

**Every 3D model, animation, and texture in this project was made by me in
[Blockbench](https://www.blockbench.net/).** The repo includes the source `.bbmodel` project files
alongside their exports, in [`assets_source/`](assets_source/):

| Asset | Source file |
|---|---|
| Ruyi Jingu Bang | `Staff.bbmodel` |
| Celestial block | `celestial_block.bbmodel` |
| Immortal peach | `immortal_peach.bbmodel` |
| Xiaotian Quan | `xiaotian_quan.bbmodel` |

Plus 18 textures and the item/block model and blockstate JSONs under
[`src/main/resources/assets/`](src/main/resources/assets/).

The rigging and animation code is hand-written —
[`XiaotianQuanModel`](src/main/java/com/jttw/engine/client/model/XiaotianQuanModel.java) and
[`XiaotianQuanAnimations`](src/main/java/com/jttw/engine/client/model/XiaotianQuanAnimations.java)
drive the entity directly. There is no GeckoLib or other animation library in `build.gradle`.

---

## Layout

```
src/main/java/com/jttw/engine/
├── JTTWEngine.java  mod entry point: wires registries, handlers, network, config
├── registry/        blocks, items, entity types, worldgen types
├── progression/
│   ├── level/       levels, XP curves, kill XP, biome bands
│   ├── stats/       classes, base stats, stat formula, beast lines, forms
│   ├── qi/          Qi pool and regeneration
│   └── skilltree/   SkillNode/SkillTree, unlock state, purchases, level milestones
├── character/       one package per playable character: its skill tree, skill bar,
│   ├── wukong/      ability keys and ability handlers (Characters maps outcome -> character)
│   ├── erlang/  nezha/  yuandi/  peng/  marshal/
│   └── divinebeast/  dragon/  phoenix/  turtle/  byakko/
├── reincarnation/   4-axis alignment tracking, 12 evolution outcomes
├── transformation/  18 / 36 / 72 Transformations: forms by category, player disguises, transmutation
├── mixin/           14 Mixin classes — bytecode instrumentation
├── flight/          custom flight, glide, and swim physics (server side)
├── network/         c2s/ and s2c/ packets for client–server state sync
├── dimension/       Heaven, Hell and the Great Void; the taller Overworld (TallOverworld)
├── world/           zones (Zones, ZoneLayout, ZoneMap, ZoneAllies), zone features, Fengdu, Heaven, morality
├── entity/          custom entities, projectiles, summons; zone/ holds ZoneMonster and ZoneBoss
├── item/            lore artifacts and weapons
├── client/          renderers, GUI screens, HUD overlays, client input and flight
└── commands/        /jttw command tree
```

Largest components: `TransformationAbilityHandler` (1,385 lines), `YuanDiAbilityHandler` (1,236),
`CloneMining` (923, the shadow clones' work planner).

### The taller sky

New worlds' Overworld reaches **y 640** instead of vanilla's 320 (from y −64, 704 high), so Mount
Kunlun can stand far over every mountain the land makes ([`TallOverworld`](src/main/java/com/jttw/engine/dimension/TallOverworld.java)).

- **How**: a dimension type of the mod's own,
  [`tall_overworld`](src/main/resources/data/jttw_engine/dimension_type/tall_overworld.json),
  vanilla's Overworld in all else (its sky, light, monsters, beds, raids) but 704 high and as high
  logically; and the world presets a new world is made from (default, large biomes, amplified,
  single biome: `data/minecraft/worldgen/world_preset/`) give their Overworld this type. Superflat
  and debug worlds keep vanilla's. The GameTest server's world is made tall too
  (`GameTestServerMixin`), so the tests see the world as a new one is.
- **Only new worlds.** A world keeps the dimension type written in its level.dat when it was made,
  so an older world's Overworld stays `minecraft:overworld`, 320 high, and loads as it always did:
  raising a world's sky under chunks already saved would shift every section of them. (Tried: a
  world saved by the build before this one, opened by this one, is still 320 high and loads and
  generates cleanly.) Where an older world has not yet made Mount Kunlun, it is made pressed under
  its lower sky (see Mount Kunlun, below).
- **The land is vanilla's to the last block.** The Overworld's noise settings are untouched (y −64
  to 320): the terrain noise is cut to the lower of the two heights, so the same seed makes the same
  land, and over it there is only open sky. Ores and features keep their own heights; structures,
  features and light reach the new top (a block can stand at y 639 under open sky). The mod's own
  searches for the land (`ZoneTerrain.landTop`) start at the terrain's ceiling, not the sky, so the
  zones' works stand where they always did and the search costs nothing more.
- **What else was checked**: everything that asks how high a world is asks the world
  (`getMaxBuildHeight`), never 320: the zones' features (their caps under the sky), the Bottomless
  Cave's and the lairs' land, Zhenyuan's and the garrison's spots, the furnace, the villages; flight
  (no ceiling of its own; nothing sends a flier to Heaven for flying high, the realm leap is a key)
  and its look-ahead (horizontal); the realm arrivals (the Overworld's by its heightmap, the
  Nether's under its roof); Heaven's void handler; the Great Void (768 high, still taller than the
  Overworld can be); the Luopan (a compass, no height); the benches.
- **What it costs** (`./gradlew runWorldgenBench -Pbench.view=12`, seed 12345, the same world made
  under each sky with `-Pbench.sky=vanilla`, on a 16-core laptop shared with other work): new
  chunks are made at about the same speed (39.7 chunks a second against 41.5
  under vanilla's sky; on a busier run 20.8 against 25.4); a loaded chunk takes about a third more
  (555 KB against 407) of heap (the empty sections over the
  land are cheap but not free); saved, a chunk is the same size on disk (12.2 KB against 12.1: empty sections
  are not written). Flight over new land is bounded by generation either way (101 blocks a second
  against 106). Mount Kunlun's own chunks, the tallest in the world, take about 40 ms each to make
  where the old peak's took about 25 (the 441 round its summit, with the client drawing them).

### Zones

A level zone is a biome put over the overworld (`world/Zones`): no biome library. Each zone has a
biome, the vanilla biomes it may take over (a tag), a `ZoneLayout` (the ring round spawn its region
lies in, radius, wobble, a salt of its own), the heights it takes (`Vertical`: the whole column,
`below(y)` for an underground zone, `above(y)` for an aerial one), its level band and the players it
is allied to. White Bone Ridge is the first (Black Wind Mountain shares its band and ring), the Bottomless Cave the second, the Mystical Waters the
third; Hell, a dimension of its own, is the fourth; then the Flaming Mountains, and Mount Wuzhuang,
the first aerial zone, in the sky, and on the ground in the same band Silk-Web Ridge and its Spider Cave; Lion-Camel Ridge, the last band, is the last. Past it all lies
Fengdu, the Ghost City: a place, not a zone of a band, placed as the zones are, whose Gate of
Ghosts is the way down to Hell for everyone who comes to it by an Ancient City and fights it open.
Far to the west stands Mount Kunlun, another such place: a towering sacred massif with the Queen
Mother of the West's palace on its summit, where the Banana-Leaf Fan lies. Out in the sea near
spawn rises Flower-Fruit Mountain, the Monkey King's old island, of the first band's levels; at the
back of its Water Curtain Cave sits the Six-Eared Macaque, an optional level-120 superboss, the
strongest boss of all.

**One of each a world.** Every boss lives in one place: each overworld zone has a single region a
world (`ZoneMap`), except the Mystical Waters, which have one in each of the Four Seas, east, south,
west and north of spawn, each its own Dragon King's (Ao Guang, Ao Qin, Ao Run, Ao Shun). The journey
west lengthens with the pilgrim: the higher a zone's levels, the farther out its ring.

| Zone | Levels | Its bosses' levels | Distance from spawn (centre) | Regions |
|---|---|---|---|---|
| White Bone Ridge | 20–36 | Lady White Bone 39 | 1–2 km | one, ~840 blocks across (never under 720) |
| Black Wind Mountain | 20–36 (the ridge's band) | the Black Bear Demon 39 | 1–2 km | one, ~700 (never under 640), clear of the ridge |
| Flower-Fruit Mountain (an island in the sea) | 20–30 (the ridge's band) | the Six-Eared Macaque 120, an optional superboss | 1–2 km | one, ~640, clear of the ridge and Black Wind |
| Bottomless Cave (Pit-Hole Mountain and under it) | 37–46 | Lady Earth Flow 49 | 2–3 km | one, ~380 |
| Mystical Waters (deep ocean) | 47–56 | the four Dragon Kings 59 | 3–4.5 km | four, one per sea (within 30° of east, south, west, north), ~440 |
| Hell (a dimension, entered through Fengdu) | 57–66 | the nine Kings 68, King Yama 69 | — | the Ten Courts |
| Flaming Mountains | 67–77 | Red Boy 80 | 4.5–6 km | one, ~640 |
| Mount Wuzhuang (over the clouds) | 78–88 | Zhenyuan 91 | 6–7.5 km | one, ~260 |
| Silk-Web Ridge and the Spider Cave (on the ground) | 78–88 (Mount Wuzhuang's band) | the Hundred-Eyed Demon Lord 91 | 6–7.5 km | one, ~480 (never under 430) |
| Lion-Camel Ridge | 89–100 | the White Elephant 102, the Azure Lion 103 | 7.5–9 km | one, ~680 |
| Fengdu, the Ghost City (river and hill country) | none: the way to Hell | — | 9–10.5 km | one, ~240 |
| Mount Kunlun (mountain country, to the west) | none: the Banana-Leaf Fan | — | 5–8 km, within 30° of west | one, ~930 |

**A zone is a road; its boss is its gym's leader.** A zone is walked as a Pokémon route is, and
fought as its gym is:

- **Regulars are its wild creatures.** Each is of a level anywhere in its zone's band, fixed where it
  is put, whoever is near (`ZoneMonster.rollRegularLevel`): the band's bottom at the zone's outskirts
  and higher further in, a level either way. Its open land climbs from its region's edge to its
  middle (`ZoneMonster.routeDepth`, `Zone.levelAlong`); where a lair goes on from there, down or in,
  the land is only part of the road and the lair's floors carry it on to the band's top:

  | Zone | The land (edge → middle) | The lair, deeper ever higher |
  |---|---|---|
  | White Bone Ridge | 20 → 28 at the mountain (half the road) | the White Bone Cave's eight floors, 29 → 36; her court 36 |
  | Black Wind Mountain | 20 → 28 at the cave's door (half) | the forecourt 31, the Black Wind Court 36 |
  | Flower-Fruit Mountain | 20 → 26 at the island's heart (six tenths) | the Water Curtain Cave, 26 at its mouth → 30 before the old throne |
  | The Bottomless Cave | 37 → 40 at the pit's mouth (three tenths) | its eight floors, 41 → 46 (by the floor a creature stands on) |
  | The Mystical Waters | 47 → 56 over each palace (the whole road) | — (the palace's guard, at the middle, 55–56) |
  | Hell | — (no land: its road is its courts) | the First Court 57, one or so more a court, the Tenth 66 |
  | The Flaming Mountains | 67 → 71 at the Fire Cloud Cave's door (four tenths) | its floors, 71 in the forecourt → 77 on the eighth |
  | Mount Wuzhuang | 78 → 88 at the temple (the whole road) | — |
  | Silk-Web Ridge | 78 → 84 at its heart (six tenths) | the Silk Hall 84, the bowers 85 → 88 |
  | Lion-Camel Ridge | 89 → 92 at the city (three tenths) | the cave's floors, 92 in the gate hall → 100 on the eighth |

  A kind may start higher than its band's bottom (Ox-Head 58, the cloud cranes 80): it is never
  under its own least. The posts a lair keeps are set by its plan (`WhiteBoneCave.floorLevel`,
  `CaveMonster.regularOn`, `FortressPlan.regularLevel`, `LionFortress.levelOf`, `SpiderCavePlan`,
  `CavePlan`, `BlackWindPlan`, `HellMonster.courtLevel`); the rest roll where they stand.
- **Captains are its trainers**: three to five levels over the regulars round them
  (`ZoneMonster.rollCaptainLevel`), never more than one over the band's top (`topLevel`).
- **Minibosses are its elite trainers**: a lair's wardens, gate keepers and guardians, five or so
  over the regulars of their floor, never more than one over the band's top.
- **A boss is its gym's leader**: the top of its band and `ZoneBoss.LEVELS_ABOVE_BAND` (3) more
  (`ZoneBoss.levelOver(zone)`), the highest thing of its zone; a lesser boss under a head boss stands
  two over the band (`lesserLevelOver`): Hell's nine kings at 68 under King Yama's 69, the White
  Elephant at 102 under the Azure Lion's 103. A boss is built for **one** character of its band's
  top (`ZoneBoss.challengerOf`, the cap past it): that character can beat it alone, in a hard fight,
  some 60 to 80 of their ordinary hits all its lives together, while its blow kills them in five or
  six. Its numbers are set against that character, not one of its own level (three levels over, a
  third again as hard: a level is a tenth more power). A group finds it easy, as a gym's leader is
  for a party at the end of the road.

`ZoneBoss.measuredAt(level)` is a creature's own level up to the players' cap of 100, the cap past
it: the Lion-Camel kings (102, 103) are built for a capped character, as the Six-Eared Macaque (120)
is measured against one. **The Macaque is the one exception**: the legendary, not a gym's leader, an
optional superboss built for a group of capped players (430 hits of a level-100 character, his staff
a third of one's health: four kill), the strongest boss there is.

The XP a kill pays is its zone's tier's (a Grunt's, a General's, a Boss's), whatever its level, so
the levels a road's creatures roll change nothing of what they pay.

**The posts come back** (`world/ZonePosts`). Everything the land puts at a post as it is made (a
camp's band, an outpost's, a household's group, a court's door guards, a palace's hall guard, a
floor's warden; its regulars, captains and minibosses) comes back once it has fallen: a post whose
holders are all dead stays empty for **20 minutes**, then, the next time its ground is awake and no
one is in it or within 16 blocks of its holders' reach, it is held again by the same as held it, each
as it was first put there (its kind, rank and level, its health, its name, its post and its reach:
each carries its own likeness from the day it was put there). Those put round one middle (a band
round its fire) are one post: it is not empty until the last of them falls, and they come back
together. Flower-Fruit's garrison, the Black Wind Cave's and the Fire Cloud Cave's are mustered from
their plans by their own keepers, the White Bone Cave's floors by its (`CaveKeeper`), all on the same
20 minutes; every other post comes back through `ZonePosts`. What respawns, zone by zone:

| Zone | What comes back | How |
|---|---|---|
| White Bone Ridge | the cave's eight floors and the throne room's court; its forecourt and towers (the Keepers of the Bone Gate, the sentries, the road's bands) | a floor no player has been on for 20 minutes is manned again when one comes back (`CaveKeeper`; a seal once broken stays broken); the outside posts through `ZonePosts` |
| Black Wind Mountain | the cave's forecourt and court (the door's keepers, the grate's warden); the watch-camps' and the burnt temple's bands | the cave's garrisons mustered again (`BlackWindCave`); the bands through `ZonePosts` |
| Flower-Fruit Mountain | the Water Curtain Cave's garrison and its three fake pilgrims; every camp's band | `FlowerFruitGarrison` |
| The Bottomless Cave | every floor's posted groups and guardians, the gate's guard, the mountain's outposts, her palace's maids | `ZonePosts` (and the cave still breeds, unkept, round whoever walks it) |
| The Mystical Waters | each palace's guard of eight | `ZonePosts`, each held 12 blocks about his place |
| Hell | each court's door guards (Ox-Head and Horse-Face), the clerks, Yama's Impermanence | `ZonePosts`, each held 12 blocks about his place |
| The Flaming Mountains | the Fire Cloud Cave's floors; every outpost's band (patrols too) | the cave's floors mustered again (`FireFortress`); the bands through `ZonePosts` |
| Silk-Web Ridge | the hall's keepers, the Seven Spider Spirits and their sons in the bowers, the yard's, the spring's and the gate's bands | `ZonePosts` (a sister's death still loosens her seal for good, and the canopy, once fallen, stays down) |
| Lion-Camel Ridge | the city's garrison, the ways up's outposts and patrols, the cave's door wardens, every floor's keepers and wardens | `ZonePosts` (a gate once opened never closes) |
| Mount Wuzhuang | nothing: the temple's household is finite by design (`TempleRecord` counts it down to Zhenyuan's coming); its cranes are the island's own spawns | — |
| Fengdu, Mount Kunlun | no posts | — |

**A fallen boss's seat is usurped** (`world/Usurpers`, `ZoneBoss.becomeUsurper`). A lair's master
the world put there (or a usurper before him) leaves his seat empty when he dies; 20 minutes after,
the next time its ground is awake and no one is within 16 blocks of the lair's ground, another demon
king has heard of it and taken it: a boss of **another kind**, at random (one house as likely as
another: Lady Earth Flow, the Black Bear Demon, Red Boy, a King of Hell, the Hundred-Eyed Demon Lord,
a king of Lion-Camel; then one of the house), a **usurper**, named as his kind with his new level
("· Lv N"). When he falls another follows, and so on. A usurper is the lair's: of its zone (the XP
his kill pays, the allies he spares, his boss bar), kept to the fallen master's ground (drawn 16
blocks past it, he is back on his seat at once). And he is **easier**:

- **Level**: his lair's band's **top** (a master stands three over it), and measured against a
  character of that level (`ZoneBoss.measuredAt`), as the gym's leader he follows is against the
  same: so he is an easier fight for the one it was built for.
- **Hits**: **60%** of his kind's hits, all his lives together (never under 36), of a character of
  his level: some 36 to 47, where the leader he follows took 60 to 80.
- **Blows**: every blow and special of his kit is struck as his kind strikes it and lands scaled to
  his new level (`ZoneBoss.strikes`, hooked on all the damage he deals), so it kills a character of
  his level in as many hits as his kind's kills one of theirs (five or six for his main blow), and
  never harder than a boss's hardest (five kill), not even a hit weighed by its target's own health.
- **XP**: half a boss's XP of the lair's zone (`jttw_enemy_xp_share`). **Karma**: half what his
  kind's death says of his slayer. **Loot**: a usurper's (`entities/usurper`: a sigil or two, gold,
  perhaps a diamond and a golden apple), none of his kind's own treasures (no staff, spear, sabre,
  swords, kasaya or shard).

| Lair | Its usurper's level | Hits, all lives together |
|---|---|---|
| White Bone Cave, Black Wind Cave | 36 | by kind: Lady Earth Flow 37, the Black Bear 41, Red Boy 43, a King of Hell 38 (Zhuanlun 41 over his two lives, Yama 47), the Demon Lord 41, the White Elephant 44, the Azure Lion 47 |
| The Water Curtain Cave (the Macaque's sanctum) | 30 | as above |
| The Bottomless Cave's palace | 46 | as above |
| A Dragon King's palace | 56 | a king of another sea, 37 |
| A court of Hell (each of the Ten) | 66 | as above |
| The Fire Cloud Cave | 77 | as above |
| The Spider Cave's temple court | 88 | as above |
| The Lion-Camel throne room (each king's place) | 100 | as above |

Who never usurps: **the Six-Eared Macaque** (the one superboss, met once: he never comes back, and
his sanctum is a seat like any other once he has fallen, taken by a usurper of Flower-Fruit's band);
**the temple's youths** (minibosses, no lair's masters); **Zhenyuan** (righteous, he fights no one but
the temple's enemies, and keeps no lair: Mount Wuzhuang has no seat); **Lady White Bone** (her kit is
her ridge's: she passes for its dead, calls them up, lays its curse); and, but in a drowned palace,
**the Dragon Kings**, whose wrath is their sea's and who are made to swim; a palace's seat, under the
sea, is a sea king's only, of another sea. Lady Earth Flow, a usurper, has no shrine of hers to flee
to: she fights it out. **Two to a lair**: the White Elephant and the Azure Lion each have their own
place before their thrones, a seat each, taken each by its own usurper, never of a kind already sat
in the room; each of the Ten Courts is a seat of its own.

Where in its ring follows from the world seed and its biomes: earliest zone first, the places its
layout offers (spread evenly over the ring by the seed) are tried in turn, and it takes the first on
host ground (the vanilla biome at its host height, from the overworld's own biome source and
sampler, at the centre and at six of eight points round it) and clear, by both reaches and 48
blocks, of every region already placed at heights it shares. If no place in the ring will do, the
search widens outward a ring at a time (and says so in the log); failing all, it is put down in its
ring off its hosts. It is found once, as the overworld loads, before any ground is made or the spawn
chosen (the log says where each zone went and how long it took), and kept: chunk generation's worker
threads only read it. The same seed gives the same places. The biome source then gives a region's
whole footprint the zone's biome, at the heights it takes (`MultiNoiseBiomeSourceMixin`).

**The Earth God's Luopan** (`EarthGodLuopanItem`, `world/EarthGod`): in the novel, wherever Sun
Wukong came to a mountain whose demon he did not know, he struck the ground and the place's Earth
God (土地公) came up out of it, bowing, to tell him who lived there. Every player is given a luopan
(a geomancer's compass) the first time they join a world, and more are crafted from any planks, a
copper ingot and red dye. Used, the Earth God rises in a puff of earth and names the zone for its
holder's level (the one whose band holds it; under 20 the first band's, past 100 Lion-Camel Ridge),
its master, and which way and roughly how far it lies, while a trail of incense smoke drifts off that
way; for the Mystical Waters, the nearest sea whose Dragon King still lives (the kings' deaths are
remembered for the world). Where a band holds more than one zone (White Bone Ridge, Black Wind
Mountain and, to 30, Flower-Fruit Mountain; Mount Wuzhuang and Silk-Web Ridge), he speaks of the one
whose country its holder stands in, or else the nearest (by where its needle would point), and then
names the others too in grey, nearest first, each with its way and distance (`EarthGod.zonesFor`,
`chosenFor`); of Flower-Fruit he always warns that its master is beyond any mortal band. Held, its
needle points to the zone he spoke of, as a compass's does, kept up to date as its holder goes and grows. Hell's band (57–66) is told that its trials lie in Diyu, past any Earth
God's ground; that the way down is through Fengdu, the Ghost City, whose Gate of Ghosts is sealed to
those who walk there; and that the way in is under the earth, by an Ancient City, whose great frame
lit with soul fire carries one before the gate, where its guard must be fought: the needle points to
the nearest Ancient City (looked for in the world's structures when the luopan is used, within /locate's
reach, and kept until its holder has gone 1500 blocks; the lit frames are known too), or, if none is
known, to Fengdu. Standing in Fengdu's country, they are told its gate is sealed and opens only for
those who come by an Ancient City's frame and break its guard.
In Hell itself the needle points to the Ten Courts' gate; asked sneaking (and held while sneaking)
it shows the way back up instead, the nearer side door of the Gate of Ghosts on the arrival plaza.
In the world of the living, asked sneaking (and held while sneaking), anyone of any level is told
the way to Mount Kunlun instead, and the needle points to its summit; every other answer there
ends with a grey word on how to ask it, so the guidance by level is otherwise unchanged.
In the Nether, the End or Heaven no Earth God hears, and it spins.

**Clouds over the dark zones** (`client/ZoneClouds`, `ClientLevelMixin` on `getCloudColor`): vanilla's
bright white clouds would sit oddly under Black Wind Mountain's ash-grey sky, the Flaming Mountains'
orange one or Silk-Web Ridge's violet murk, so over those zones they take the sky's colour: dark ash
over Black Wind, smoky orange over the Flaming Mountains, violet-grey over Silk-Web Ridge, a leaden
grey-blue over Fengdu, a dim dusty grey over Lion-Camel Ridge. The tint multiplies vanilla's cloud
colour (night, rain and thunder still darken it); it is sampled round the camera as vanilla samples
the sky's colour (a gaussian over the biomes some twenty blocks about) and eased from tick to tick, so
it fades in over a border rather than snapping. The other zones' skies are pale (White Bone Ridge's
bone-white, Flower-Fruit's, Mount Wuzhuang's over its own clouds, Kunlun's) and keep white clouds;
Hell keeps the Nether's sky, which has none. Client-side only.

**The pecking order.** Every creature's strength is set against a typical character of its level
(`entity/MobScaling`): its health in that character's ordinary hits ("HITS"), each of its blows a
share of that character's health ("BLOW"); a boss's against the character it is built for, one of its
band's top. It holds hit for hit: a regular under a captain, a captain under a miniboss, a miniboss
under its boss, in hits to kill and in blow:

| Rank | Blow (before resistance) | Hits to kill | Who |
|---|---|---|---|
| Light mobs | 6–7% (15–17 kill you) | 2–4 | wraiths, mouse spirits, ember imps, fish soldiers, yin soldiers, wandering ghosts, judge-clerks, cloud cranes, Wind-Drillers, goblins, black-wind imps, spider demons, stinging sons, stone-throwing gibbons, the Macaque's hair clones |
| Normal mobs | 8–10% (10–13 kill you) | 4–8 | flower maids, shrimp soldiers, yaksha, Horse-Face, the Impermanence, peng-demons, Daoist disciples, kobolds, wolfmen, heavenly soldiers and archers, cloud cavalry, thunder drummers, boar demons, Mantis Sons, spear apes |
| Heavy mobs | 11–12% (9 kill you), no single hit (slam, gore, pounce, swoop) harder | 10–12 | Bone Servants, Furnace-Brick Brutes, ogres, elephant-demons, lion-demons, turtle soldiers, Crab Generals, Ox-Head, Black Bear Soldiers, Cockroach Sons, great baboons |
| Captains (trainers) | a regular's ×1.2: at most 14.4% (7 kill you) | a regular's ×2: 4–24 | any of the above, purple-named, 3–5 levels over the regulars round them; Heaven's Twenty-Eight Lunar Mansions |
| Minibosses (elite trainers) | their hardest hit 16% whatever their kind (7 kill you), their blow 14–16% | 25–40, over any captain's | the floor wardens and gate keepers of every lair (White Bone's Wardens and Keepers of the Bone Gate, the Bottomless Cave's guardians, the Fire Cloud Cave's door wardens and floor guardians, Lion-Camel's floor wardens), the Six Strong Generals, the Seven Spider Spirits, Clear Wind and Bright Moon, Heavenly Generals, the Nine Luminaries, the Great Spirit Giant, the Thunder Lord and the Lightning Mother, Thousand-Mile Eye and Wind-Following Ear, Lingxuzi and the White-Robed Scholar (Black Wind's door keepers) and its stair's warden, the Six-Eared Macaque's three fake pilgrims |
| Bosses (gym leaders) | 18–20% of a character of their band's top (5–6 blows kill them); no special or area hit harder | 60–80 of that character's, all their lives together, over any miniboss's | every `ZoneBoss` but the Macaque, three levels over its zone's band (a lesser boss two); Heaven's Four Kings, Li Jing, Nezha and Erlang Shen, each over his region's |
| Usurpers | as their kind's, at their own level (5–6 blows kill you) | 36–47: 60% of their kind's | a boss sat in a fallen master's lair, at its zone's band's top (above) |
| The superboss | a third of a capped character's health (4 kill one) | 430 of a capped character's | the Six-Eared Macaque, level 120 |

A miniboss stands at most one level over its band's top, and its hardest hit there is still under the
blow of its zone's boss on the character that boss is built for.

**Every boss keeps its kit.** A gym's leader is fought alone or by a group, and each still has at
least one blow that falls on everyone in an area, each telegraphed (a wind-up to step out of) and
declared, as mobs' special hits are, in `ZoneBoss.specialShares()` and struck with `hitOf(share)`, so
the pecking order holds it to a boss's blow (of the health of the character the boss is built for):

| Boss | Its blow on all | Telegraph (wind-up) | Area | Kills you in | Every |
|---|---|---|---|---|---|
| Lady White Bone | **the bone storm**: the bones of her dead burst out from her, cutting and slowing; besides, shards in a fan (7 kill you) and the blinding veil | arms flung wide, head thrown back, bones whirling round her, the ring marked in ash (30) | 8 round her | 8 (14%) | 11 s |
| Lady Earth Flow | **the whirl**: spins on her heel, both blades out; thrown back, slowed | sinks low, blades swept back, petals drawn in (20 ticks) | 4.5 round her | 8 (14%) | 9 s |
| The Dragon Kings | their sea's wrath (surge, boiling sea, whirlpool, frozen abyss) | — | round him / the whirlpool | 8 (14%) | a few seconds |
| Qinguang | **the mirror's flash**, weighed by karma; dazzles | mirror held high, light gathering, the ground glinting (30) | a 90° cone, 10 before him | 8 (13%); 13 the good, 7 the wicked | 12 s |
| Chujiang | his frost | — | 8 round him; frozen, slowed | 10 (10.8%) | 7 s |
| Songdi | **the ring of hooks**, dragging all in | chains rattle, the ring marked in sparks (30) | 7 round him | 8 (14%) | 12 s |
| Wuguan | **the weight of the scales**, Slowness III | glyphs settle over the ground (30) | 6 round him | 7 (15.5%) | 12 s |
| King Yama | sentence (a ring at his foe's feet), his wrath's soul fire | the ring marked (delay) | the ring; close round him | 5 (a fifth of one's health) | — |
| Biancheng | **the bell**: all kneel, and its note strikes them | — | 7 round him | 10 (10%) | 7 s |
| Taishan | the boiling oil, burning | — | 7 round him; burning | 10 (10.8%) | 7 s |
| Dushi | the crushing carts: everything run down on the way | the roar of the charge | his path | 6 (19.8%) | 7 s |
| Pingdeng | **impartial judgement**: the same blow on all | a golden balance, its ring marked (30) | 8 round him | 7 (16%) | 12 s |
| Zhuanlun | **the Wheel turning**: a shockwave, flinging all back | the Wheel spins in soul fire over his head (30) | 8 round him | 7 (16%) | 12 s |
| Red Boy | the True Samadhi Fire; at a third, a ring of it | strikes his nose twice | a cone; round him | 7 (16%) | — |
| The Hundred-Eyed Demon Lord | **the golden light** of his thousand eyes: on all who face him, blinding and holding them fast (5% to one turned away); **the poisoned tea**, cups bursting where his foes stand; **the centipede's sweep**, flinging all back | arms flung up, his eyes blazing (30); rings of yellow-green motes where each cup will burst (30); rearing on his coils, scratching the ground (20) | 14 round him; 3 round each cup; 7.5 round him | 7 (16%); 10 (10%); 8 (14%) | 16 s (11 s unrobed); 12 s (7.5 s past a third); 10 s, when foes are close |
| Zhenyuan | **the sweep of the whisk**: a gale, blowing all back; the Sleeve (from 30%) takes all within 8 | the whisk whirled overhead, a ring of cloud gathering (25) | 6 round him | 7 (16%) | 11 s |
| The Azure Lion | the roar | head thrown back | a cone | 13 (8%) | — |
| The White Elephant | stamping, trampling | — | round him | 10 (10%) | — |

(Bold: added for the group fight, and kept. Each wind-up roots the boss: he strikes nothing else meanwhile.)

A miniboss is a captain made one by `ZoneMonster.becomeMiniboss(level, health over a captain's)`;
its health is so many times a captain's of its kind, kept within 25 to 40 hits, and its hits are
scaled so its hardest is `MINIBOSS_HIT` (16%). A mob's special hits are declared in
`specialShares()` and struck with `hitOf(share)`, so its rank scales them with its blow.
A boss's lives are its `ZoneBoss.lives()` (Lady White Bone's four, whole each time, 17 hits apiece;
Zhuanlun's 45, and born again at half). By boss, in ordinary hits of a character of its band's top,
all lives together, and its main blow: Lady White Bone 68 (her claw kills in 6), the Black Bear Demon
68 (his spear, 6), Lady Earth Flow 62 (her stroke of two cuts, 6), the Dragon Kings 62 (a claw, 6),
the nine Kings of Hell 64 (Zhuanlun 67.5; the tablet, 6), King Yama 78 (6; 5 in his wrath), Red Boy
72 (his spear, 6), Zhenyuan 60 (more hand to hand: he brushes one blow in four aside, and no blow
takes more than 6.5% of his health; his whisk, 6), the Hundred-Eyed Demon Lord 68 (his sword, 6), the
White Elephant 74 (his lance, 6), the Azure Lion 78 (his sword, 6). Their health runs to millions,
past vanilla's cap of 1024 max health: `JTTWEngine` lifts the cap (and attack's) to a billion.
**One superboss** stands over them all, the legendary: the Six-Eared Macaque of Flower-Fruit
Mountain, an optional fight at **level 120** (past the players' cap and every head boss, the only
boss not at its band's top and three, and no gym's leader), measured at the cap: 430 hits of a
level-100 character and a blow of 33% of one's health, so four kill: the strongest boss there is.
He is meant for a group of capped characters; below the cap he is out of reach.
`PeckingOrderGameTests` makes every creature of the mod in each of its ranks and fails if any hits
out of its place: a regular outside 2–12 hits, a captain not between its kind's regular and any
miniboss, a miniboss outside 25–40 hits or its hardest not over a captain's; a boss outside 60–80
hits (but for the one named superboss exception, the Macaque, up to 460, whom it checks is the
strongest boss there is), or all its lives no more than the toughest miniboss's; any boss's special
hit harder than a boss's blow may be. `aBossIsSoloableAtItsZonesTopLevel` measures every boss against
a character of its band's top level: 60–80 of their hits, a blow that kills them in five or six,
no special harder. It fails, too, if any boss is not above everything else of its zone (fortress
and garrison posts counted: White Bone Cave's, Black Wind Cave's, the Bottomless Cave's households,
the Water Curtain Cave's, the Fire Cloud Cave's, the Spider Cave's, Lion-Camel's floors, none more
than one over the band), its head boss not at its band's top and three, its lesser bosses not at
two; and `regularsRollAcrossTheirBand` walks every kind out from its region's middle past its edge,
checking that its level is the band's bottom at the outskirts and higher at the heart, its captains
three to five over the road's, Hell's road the courts', and every lair's road reaching its band's top.
The bosses it measures are their own lairs' (no usurper among them), so its numbers are theirs.
`UsurperGameTests` sits usurpers on the tests' own seats and checks each: his kind never the fallen
one's nor the Macaque (nor any who may not usurp), his level, zone, XP share, loot and name, 60% of
his kind's hits, his blow and every special of his kit landing at his kind's share of a character of
his level, kept as he was saved and loaded, and his own fall seating another; `ZonePostsGameTests` a
post of each zone (White Bone's gate wardens and the Bottomless Cave's gate guard through the land's
own putting): written down as it falls, cleared, kept empty with a player in it or near it, then held
again, by the same, when its time comes round. A post comes back as it was saved at worldgen, its
level the one it was first put at.

To add a zone:

1. **Biome**: `data/jttw_engine/worldgen/biome/<zone>.json` (its features) and a hosts tag in
   `tags/worldgen/biome/`.
2. **Definition**: a `Zone.overworld(biome, hosts, new ZoneLayout(..., uniqueSalt), vertical,
   minLevel, maxLevel, gruntXp)` in `Zones`, appended to `Zones.ALL`; `.hostSampledAt(y)` for a
   host judged off the surface, `.alliedTo(ZoneAllies.PENG)` for a zone that spares the Peng. A
   zone of another dimension (Hell) is `Zone.dimension(biome, ...)`: a level band, no regions; a
   place of no band (Fengdu) is `Zone.place(biome, hosts, layout, vertical)`: a region, no band.
   Level bands are registered from `Zones.ALL`; the biome source places the biome by itself.
3. **Monsters**: extend `entity/zone/ZoneMonster` (`zone()`, `hitsToKill()`, `blowShare()`, any
   special hits in `specialShares()`, within the pecking order above; override levels, captain
   chance, escorts, captain loot, `allies()` as needed).
4. **Boss**: extend `entity/zone/ZoneBoss` (`zone()`, `lairRadius()`; a boss bar, persistence,
   Boss tier, phases through `phaseThresholds()` / `onPhase()`).
5. **Worldgen**: features read `zone.depth(...)`, `zone.centre(...)` (its nearest region's),
   `zone.centres(seed)`, `zone.isAt(level, pos)` and `ZoneTerrain.landAt(...)`; the lair extends
   `world/feature/ZoneLairFeature` (`zone()`, `boss()`, `build(...)`, `floorAt(...)`), which builds
   it at its region's centre and puts the boss there once (his seat: fallen, a usurper takes it).
   A post's holders are kept with `ZonePosts.keep(mob)`, last, just before they join the world: they
   come back 20 minutes after the post's last falls.
6. **Tests**: `ZonesTest` checks every zone of `Zones.ALL` (one region, in its ring, on its hosts,
   clear of the others); GameTests for its monsters and boss as `RidgeGameTests` does; a line for it
   in the Earth Gods' words (`EarthGod`). See where it lands with `./gradlew runWorldgenBench
   -Pbench.mode=zones -Pbench.seed=<seed>` (`ZoneBench`: each zone's place, distance and host, a
   map of the 20 km square round spawn, and a visit to each region: its biome, its bosses, a picture).
   The Black Wind, Silk-Web and Flower-Fruit benches also run `ZoneAudit` over the loaded zone: the
   mod's creatures inside blocks, trunks hanging over nothing or left bare, water and lava standing
   over or beside air, cliffs only at chunk borders (against a count of the same half-way across the
   chunks, which the land has anyway), and whether the ways to the zone's masters can be walked (a
   body's search: stepping up one, dropping three, swimming and climbing, through doors and seals);
   every finding in `run-bench/audit-<zone>.txt`. A band or post put among props is put where its
   body has room (`ZoneMonster.roomNear`).

#### White Bone Ridge (levels 20–36)

A bleak massif of bleached stone, crags and mesas strewn with bones, its dead (Bone Servants and
Wraiths, `entity/ridge`) about it in any light, two to four at a time. At its heart stands a great
bone mountain, and under it the **White Bone Cave** (白骨洞), Lady White Bone's fortress: a blueprint
(`world/ridge/WhiteBoneCave`) laid chunk by chunk as the land is made (`WhiteBoneCaveFeature`, the
mountain and the road by `BoneMassifFeature`), so it comes out whole whichever chunk is made first.

- **The approach.** The region is about 840 blocks across. The mountain is some 260 across, a spire
  at its top and great tusks of bone leaning out of its flanks; the ridge's own crags are worn down
  toward it so it rises out of them. A paved road winds 130 blocks down its southern face from the
  forecourt, under a bone arch, past three pairs of watchtowers; five palisaded outposts with a
  tower and a fire stand 190–260 blocks out round it. Over a hundred of the garrison stand outside: the
  door's two keepers and their escort in the forecourt, a sentry at each tower's foot and on its
  top, four patrols on the road (a captain and two), each outpost's captain and four, and sixteen
  packs of a captain and three roaming the flanks. Their levels are the ridge's road's: 20 at its
  edge, climbing to 28 at the mountain (a pack by how near it roams, the road's posts the nearer the
  door the higher, the forecourt's 28); captains four over the regulars round them.
- **The great door.** In the back of a forecourt cut into the mountain, under a skull carved in the
  cliff with eyes of crying obsidian, a door of bone and iron 13 wide and 16 high in a frame of
  blackstone, two ribs arched over the court before it. It stays shut while either of its keepers,
  the **Keepers of the Bone Gate** (level 34 minibosses, the elite of the mountain's road), lives; once both are dead it grinds open as
  someone comes within 12 blocks, its leaves folding back against the passage, and stays open for
  good (`CaveKeeper`, `CaveState`).
- **Eight floors**, each 16 blocks under the last, each a hall some 90 blocks across in its own
  stone, light and dead: the **Ossuary Gate** (a vaulted nave of bone pillars); the **Corpse Halls**
  (three long halls of biers, the middle one walled across); the **Bone Forge** (a forge chimney in a
  ring of lava, two lava channels bridged only near their ends); the **Catacombs** (a 12 by 12 maze
  of burial passages, the world's own); the **Hall of Borrowed Faces** (galleries of plaster, dark
  wood and smoked glass, her cast-off shapes against the walls, crossed by its inner ring); the
  **Charnel Pit** (a pit of bones seven deep, a ribbed causeway over it); the **Soul-Fire Crypt** (a
  low forest of basalt pillars, trenches of soul fire); and the **Bone Bridge** (a chasm, a bridge of
  bone over it). Each way down is a corridor in the hall's wall behind a **seal** of iron bars, then
  a stair in the rock; the stairs alternate north and south, so every floor is crossed end to end.
- **Who holds them.** Each floor has 8 + k regulars and 1 + k/2 captains (k from 0), regulars from
  level 29 on the first (the ridge's road goes on down from the mountain's 28) to the band's top, 36, on the last,
  captains four over (never more than one past the band's top, 37), placed at fixed posts (laid by the
  seed); and before its seal its **Warden** (a Bone Servant miniboss five levels over its floor, no higher than 37, with
  60% more than a captain's health: 32 hits). A Warden's death breaks its seal for good. Every one of them is kept (never
  despawned) and held near its post. One found off its floor with no one to fight is put back at its
  post, and none is ever let into the throne room.
- **Restocking.** A floor no player has been on for 20 minutes is manned again when someone comes
  back to it: each empty post is filled (the Warden too, though the seal stays broken), never within
  12 blocks of a player. The posts outside (the Keepers of the Bone Gate, the towers' sentries, the
  road's bands) come back as every zone's do (`ZonePosts`): 20 minutes after a post's last falls, no
  one near it.
- **Checkpoints: the well of the dead.** A ladder shaft runs in the rock from the first floor to the
  last, a short corridor to it from each. A grate bars it over each floor's landing until that floor
  is won (the seal above it broken), so after a death one climbs straight down to the deepest floor
  reached instead of fighting through again.
- **The Throne of Bones**, under the last floor: 61 by 69 blocks under a vault 28 high ribbed like a
  ribcage, two rows of bone pillars, braziers of soul fire, walls of stacked bone set with skulls, a
  great skull over a stepped dais and a throne of bone. Lady White Bone (level 39, the ridge's gym leader:
  68 hits of a level-36 character over her four lives, 17 a life, her claw killing one in six) is put there when the land is
  made, passing for one of her court of six; her fight is as it was (found out by a blow or by
  truth-seeing eyes, her blinks, her false deaths, her veil, her boss bar only while she is herself;
  and, herself, every 11 s with a foe within 8 blocks, **the bone storm**: she flings her arms wide
  and throws her head back, jaw agape, while the bones of her dead rise and whirl round her and the
  ring they will reach is marked in ash (1.5 s; she neither throws nor vanishes meanwhile), then they
  burst outward: everyone within 8 blocks struck for 14% (eight kill), cut and slowed),
  but she never leaves the room: a blink that would take her out of it is not taken, and if she is
  ever outside it (thrown, pulled) she is back in its middle at once. She persists and, once killed,
  does not come back: 20 minutes after, with no one in the room, a **usurper** takes her throne
  (level 36, 60% of his kind's hits, half a boss's XP: see Zones), and another after him.
- **Natural spawns** keep to the open ridge (`RidgeRules`): none inside the cave, whose floors hold
  only their garrison.
- **The Earth God's Luopan** points the ridge's band to the great door when the ridge is the nearest zone of the band (or is named among them, its way told), and in the ridge's country
  says the door is at the head of the road, that its keepers must fall first, and that eight floors
  lie under it.
- **Old worlds.** Whether a world has the cave is decided once, the first time it is loaded with it,
  and remembered (`CaveSites`, `CaveState`): a world any of whose chunks over the fortress were
  already made keeps its old open-air lair at the region's centre, Lady White Bone in it, and gets no
  mountain or cave at all, so nothing is half-built round her. The region's ground already made keeps
  its old shape; new ground out to the larger radius is the ridge's too, so its edge may show a seam.
- **Cost.** The ridge bench (`./gradlew runWorldgenBench -Pbench.mode=ridge -Pbench.seed=<seed>`,
  `RidgeBench`) times the 19 by 19 chunks round the heart and photographs the mountain, the road,
  the door and every floor: on seeds 12345 and 42, 18–26 s for the 361 chunks (14–20 a second; 14–15
  s, 24–26 a second, before the cave), the massif 12–14 ms a chunk and the cave 5–8 ms a chunk where it lies. The
  cave's inside is also laid in the form gallery (`RidgeScenes`).

#### Black Wind Mountain (levels 20–36)

Black Wind Mountain (黑风山, chapters 16–17), the novel's first demon lair: the old abbot of the
Guanyin Chan Temple coveted the Tang Monk's brocade kasaya and set fire to his own temple to burn the
pilgrims; the Monkey King turned the wind on the monks' halls, and while the temple burned the Black
Bear Demon, come over the mountain to help put the fire out, carried the kasaya off to his Black Wind
Cave. In [`world/blackwind/`](src/main/java/com/jttw/engine/world/blackwind/) and
[`entity/blackwind/`](src/main/java/com/jttw/engine/entity/blackwind/); its registrations are its own
(`registry/BlackWindRegistry`, one line in `JTTWEngine`), its renderers too (`BlackWindRenderers`).

- **Where** (`Zones.BLACK_WIND`): White Bone Ridge's band (20–36) and ring (1–2 km), over cold pine and
  mountain-forest country (taiga, old-growth pine and spruce taiga, snowy taiga, windswept forest and
  hills, groves, dark forest), the whole column, host judged at y 256. One region a world, about 700
  blocks across (never under 640), clear of the ridge and every zone before it.
- **The range** (`BlackWindShape`, `BlackWindRangeFeature`, pure and unit-tested): from the region's
  edge the ground climbs into ridges of dark mountain, their crests broken by crags, up to some 70
  blocks over the land; at the heart **the black peak** rises 190 more round a point 40 blocks north of
  the centre, its summit bare blackstone, basalt and gravel, its south face a cliff over the cave's
  door. The slopes are grass and podzol under the old pine taiga's pines (darker in the hollows), the
  faces dark rock in blotches of deepslate, tuff and blackstone. A dark grey sky and fog, falling ash,
  no rain; the soul-sand valley's wind for its ambience, the old taiga's music.
- **The black wind** (`BlackWind`, `BlackWhirlwindEntity`): whirls of black smoke some five blocks high
  rise round anyone on the mountain under the open sky, a few at a time, and wander across the pines,
  buffeting what they cross (harmless; the Black Bear's own are not).
- **The valley road**: a valley cut from the cliff south to the region's edge, a trodden road up its
  middle edged with cobbles, its floor level before the door (the forecourt) and across a terrace on the
  way, ramping between to the land at its mouth (`BlackWindShape.floors`); no pine stands in its way.
- **The burnt Guanyin Chan Temple** (观音禅院, `TemplePlan`, on the terrace 150 blocks down the valley):
  the compound's broken red wall, the mountain gate's two pillars (one fallen short) and its lintel in
  the dirt, the bell tower's and drum tower's charred posts and the great bell where it fell, the side
  halls' stubs, the main hall's platform with its columns burnt to stumps, its back wall standing black,
  one corner of its roof still on its beams, the statue of Guanyin thrown down before her empty
  pedestal, a broken relic pagoda, ash and cinders, three fires still smouldering under the rubble, dead
  trees burnt black. A boar-demon captain and four scavengers pick over it (put once, kept, back 20
  minutes after the last falls).
- **The watch-camps** (`BlackWindShape.camps`, laid by `BlackWindWorksFeature`): one tried every 84
  blocks over the range, on level ground clear of the peak and the valley: a stockade of sharpened spruce
  stakes round a smoking fire, a black banner on its pole, lean-tos of black cloth, a rack of spears;
  each with its band put there once and kept, a captain and three or four (one band in three roams 40
  blocks round, a patrol); a band all fallen is back 20 minutes after, no one near (`ZonePosts`).
- **Creatures** (`BlackWindMonster`: level 20 at the mountain's foot to 28 by the cave's door, captains
  three to five over the regulars round them, any light; never born inside the cave; one in
  ten of the world's own a captain, who comes with his escort). In hits of a same-level character
  ("you kill it in") and blows to kill that character ("it kills you in"):

  | Creature | Rank | You kill it in | It kills you in | What it does |
  |---|---|---|---|---|
  | **Black-Wind Imp** (黑风小妖) | light | 3 (a captain 6) | 15 claws of 6.5% | every 3.5 s, its foe 3–11 off, spins up a gust (0.5 s, the black wind gathering round it) and rides it at its foe: 7%, thrown back; strike one and the rest turn |
  | **Boar Demon** (野猪精) | normal | 6 (12) | 11 axe blows of 9% | every 5.5 s, its foe 4–15 off, paws the ground (0.8 s, snorting, the dirt flying) and charges: the first it meets gored for 10% and thrown |
  | **Black Bear Soldier** (黑熊兵) | heavy | 11 (22) | 8–9 mace blows of 12% | its foe close, rears up mace high (0.9 s) and crashes down: 11% to all within 3.5 blocks, thrown up |
  | **Lingxuzi** (凌虚子), the grey-wolf Daoist | miniboss (level 34) | 32 | 7 sword strokes of 16% | every 5 s crouches (0.6 s) and springs: 14% to all where he lands; once, under half his health, stops (1.2 s) and swallows an elixir, a fifth of his health back |
  | **The White-Robed Scholar** (白衣秀士), the white-flower snake | miniboss (level 34) | 29 | 7 fan strokes of 16% | every 3.5 s, his foe 4–16 off, rears hissing (0.6 s) and spits venom: 14% and poison; his foe close, gathers (0.6 s) and lashes his tail round him: 14% to all within 3.5, flung aside and slowed |
  | **Warden of the grand stair** (a Black Bear Soldier) | miniboss (level 37) | 35 | 7 blows of 16% | the soldier's slam at 14.5% |

- **The Black Wind Cave** (黑风洞, `BlackWindPlan`, worked out once, the same in every world, pure and
  unit-tested; `BlackWindBlocks` says what each code is; the range lays it column by column, so it comes
  out whole across its chunks; `BlackWindCave` keeps it):
  - **The forecourt** before the cliff: braziers of soul fire up the road; a tiled pavilion where the
    Black Bear's friends sit and talk, a stone table and stools; a stele across the road; and **its two
    keepers**, Lingxuzi and the White-Robed Scholar (minibosses, level 34), with two boars and four imps
    (level 30–32, the mountain's road going on in from its land's 28).
  - **The great door**: a facade cut into the cliff, deepslate tile banded in carved stone, pillars
    crowned with soul fire, a tiled eave over it hung with soul lanterns, a gilded plaque; the door two
    leaves of black lacquer banded in black iron and studded with gold, **11 wide and 15 high**; two
    crouching black bears of stone before it, eyes of crying obsidian. **The rule**: while its keepers
    live it is barred from within (knock on it and they turn on you); once both are dead the black wind
    blows it open from the middle out as soon as anyone comes within 14 blocks (or knocks), and it stays
    open. It does not yield to picks or axes (a player in creative breaks it as anything else).
  - **The Black Wind Court**, at the end of a passage thirteen wide: a cavern a hundred blocks across
    under a dome thirty high, hung with basalt and soul lanterns on chains, great basalt columns; the
    garrison's camp in it: four barracks of spruce and dark oak either side of a paved road, braziers,
    a training yard of straw dummies and spear racks; across it a wall with towers and a gatehouse hung
    in black and red; behind the wall the **inner court**, the great tiled pavilion where the
    **Buddha-Robe Feast** (佛衣会) was laid, its long tables with candles; and at its back the head of
    the grand stair, shut by an iron grate. **Its garrison**: 27 (imps, boars and bear soldiers, levels
    35–36, the band's top; captains 37), mustered when anyone comes to the forecourt or the court, and **the warden** of
    the stair (a Black Bear Soldier miniboss, level 37), whose death throws the grate open for good.
  - **The throne cavern**, down the grand stair (eighteen deep): two rows of basalt pillars, soul fire at
    their feet, a red carpet edged in black to a stepped dais and the Black Bear's throne of blackstone and
    gold; on the rock behind it the stolen kasaya's likeness worked in red and gold, its jade ring; his
    treasury chest beside the throne (`chests/black_wind_treasury`: gold, emeralds, diamonds, golden
    apples, bottles of experience, sigils). The Black Bear is put there once, when the land is made, on
    the open floor before the dais (`BlackWindCaveFeature`), and kept to the cavern; fallen, his throne
    is a usurper's 20 minutes after (level 36).
  - **The garrisons are kept** to their part of the cave: one found off it is taken back to its post.
    A part whose garrison is all dead stays empty 20 minutes, then is mustered again the next time
    someone comes near while no one is in it. The door and the grate, once opened, stay open.
- **The Black Bear Demon** (黑熊精, the Great King of the Black Wind 黑风大王; `BlackBearDemonEntity`, a
  `ZoneBoss`): a towering black bear near four blocks to his ears, in black lamellae edged in gold, a gold
  diadem, a black war cloak, **the stolen kasaya** thrown across his breast (red patchwork in five folds
  fanning from his left shoulder, sagging in swags below, gold only at its border and fringe; its skirt
  in three pleats at his hip, its corner and its long fall layered); his black-tasselled spear (黑缨
  枪). **Level 39**, his band's top and three, his mountain's gym leader; **68 hits** of a level-36
  character (the one he is built for), one life; his spear kills one in **six** thrusts (19.5%), a purple
  bar. One of the band's top can beat him alone, in a hard fight:
  - **the black wind** (黑风): every 9 s (6 s from his first phase), his foe within 11 blocks, he plants his
    spear, spreads his arms and roars while the black wind draws in round him (**1.5 s**); then it bursts
    out: **14%** (eight kill) to all within 9 blocks, **blinded** 3 s, and hurled away and up;
  - **the bear's maul**: his foe within 5, he rears up on his hind legs, both arms high (**0.9 s**), and
    crashes down: **17%** (six kill) to all within 5 blocks, thrown up;
  - **phase 1** (at 60%): three **whirlwinds** of the black wind rise round his lair and hunt his foes,
    drawing in and lifting what they catch and striking it **2.5% each half second**, for 30 s; the black
    wind comes faster;
  - **phase 2** (at 30%): two more whirlwinds, he is a sixth quicker, and every 7.5 s he draws back his spear
    (**1.1 s**) and hurls **the gale** before him: **16%** (seven kill) in a cone 14 blocks long, 30° either side,
    flinging what it catches.
  Brought down he is not killed but subdued: Guanyin's golden hoop closes on his brow and he gives up the
  kasaya, to keep the mountain of Potalaka ever after.
- **The Brocade Kasaya** (锦襕袈裟, `BrocadeKasayaItem`): the Buddha's robe, the Black Bear's treasure, his
  loot **every time**. Worn on the chest: as diamond against blows (8, toughness 3), **no fire burns its
  wearer** (fire, lava and burning cancelled), and poison leaves him. Epic, fire-proof as an item.
- **What they leave**: imps feathers and gunpowder (a phantom membrane now and then), boars pork and
  leather and iron, bear soldiers leather, bone, coal and iron; Lingxuzi a golden apple (his elixir),
  bottles of experience and gold; the Scholar paper, books, emeralds and bottles of experience; captains
  their sigils; the Black Bear the kasaya, sigils, gold, golden apples (an enchanted one in four),
  diamonds.
- **XP and karma**: White Bone Ridge's band and pay (base grunt XP 80); subduing the Black Bear is a good
  deed, **karma +100**; his friends are captains (+5). The Earth Gods' luopan sends the band to the
  nearer of White Bone Ridge, Black Wind and (to 30) Flower-Fruit, naming the others with their ways;
  in Black Wind Mountain's own country it names the Black Bear and points to his great door.
- **Art**: `assets_source/black_bear.py` (the King and his soldiers, one frame; a soldier's skin and a
  captain's), `black_wind_folk.py` (the boar, the imp, Lingxuzi, the Scholar; captains' skins for the boar
  and the imp), `kasaya_items.py` (the kasaya's icon and its worn layer). Their attacks are posed: the king
  spreading his arms and roaring as the wind gathers, rearing for the maul, drawing back for the gale;
  the soldiers rearing; the boar pawing and charging; the imp whirling and riding its gust; Lingxuzi
  crouching, leaping, drinking; the Scholar rearing with his tongue out, his tail sweeping as he coils.
- **Tests and pictures**: `BlackWindGameTests` (the region in the ridge's ring and band, the creatures and
  captains, each one's attack, the friends always minibosses, Lingxuzi's elixir once, the king's level,
  hits and blows, his black wind, maul, gale, whirlwinds and phases, the kasaya and what it does, XP and
  karma), `BlackWindCaveGameTests` (the whole cave laid off the grid: the door's rule, the court's
  muster and the warden's grate, the garrison and the king kept to their parts), `BlackWindPlanTest`
  (the door, the grate, the arena, a way on foot from the door to the king that the grate shuts, the
  garrisons on open floor, the temple, the peak and the valley), `ZonesTest`; gallery scenes
  `-Pgallery.only=bw_`; the mountain, the temple, a camp, the cave and the king in a real world with
  `./gradlew runWorldgenBench -Pbench.mode=blackwind`. (Its audit found bear soldiers born inside a
  camp's chopping log, now put where they have room, and the feast pavilion's middle posts stopping
  short of its roof, now hung from it; the way from the forecourt to the Black Bear walks.)

#### The Bottomless Cave (levels 37–46)

Pit-Hole Mountain's Bottomless Cave (陷空山无底洞, chapters 80–83), home of Lady Earth Flow, the
Golden-Nosed, White-Furred Mouse Spirit. An underground zone (`Zones.BOTTOMLESS`):
`Vertical.below(40)` under mountain country (peaks, slopes, groves, meadows, cherry groves,
windswept hills), one region a world 2–3 km from spawn, about 380 blocks across (radius 190, never
under 330). Its host is judged at y 256, above any mountain's top, where a column's biome is always
its surface biome (judged lower, under a tall peak, it can be a cave biome). Being after White Bone
Ridge in `Zones.ALL` and sharing its heights, it keeps clear of the ridge's region. Grunt XP 100 (260
a kill there, with the band's scaling), against the ridge's 80. Her lair is no longer out in the
open: the mountain crawls with her household, a giant door in its side opens on the cave, and eight
floors of maze go down, each wider and harder than the last, to her throne room at the very bottom
of the world.

- **Pit-Hole Mountain** (`world/feature/PitHole`, pure, unit-tested; laid by
  `BottomlessCaveFeature`): a craggy massif raised out of the land round the region's centre, steep
  (it falls 1.5 a block), its summit sunk in a hollow 18 deep (as its name says), rising to y 200,
  with a brow of rock 22 blocks over the door and a talus of rock falling away round the forecourt. A **road** winds up to the door: from the land below,
  a track of beaten earth, then a shelf cut into the mountain's flank, curling round it and climbing
  0.42 a block (about 220 blocks of it up on the mountain), lanterns on posts along its outer edge.
  **Outposts** watch it and the slopes: spruce watchtowers on stilts (a ladder up, a railed deck, a
  lantern, a banner), burrow mounds with a mouse hole, the mice's camps (a fire, log seats, a pot,
  hay), guard nooks along the mountain road, and where the road leaves the mountain, a stele
  (陷空山 Pit-Hole Mountain, 无底洞 ahead). Forecourt and road are laid in ground no tree roots in.
- **The great door**: in the mountainside, 56 blocks out from the centre on one of the four sides
  (from the seed), at the height of the land there but never under y 150: a forecourt cut level into
  the slope and open to the sky (mud-brick way, skulls on stakes, a soul lantern either side), and
  across its back the gate: leaves of red lacquer banded in iron and studded in gold, **nine wide
  and eleven high**, in a frame of black stone carved with a gold cloud-scroll; before it two great
  pillars of blackstone banded in gold with soul fire on their heads, a red beam with the plaque
  (无底洞 THE BOTTOMLESS CAVE) and a roof of dark tiles with lanterns hung from its eaves. **It is
  barred from within while its guard stands**: a mouse captain, three of his pack and two maids are
  posted in the forecourt; use the door with any of them alive within 48 blocks and it will not move
  (you are told how many still stand); when the last has fallen, it grinds open at a touch (its
  leaves are gone) and stays open. Its leaves and frame are the cave's own (see below).
- **The gate hall and the great shaft**: behind the door a hall 11 wide and 13 high runs in to the
  centre (deepslate and blackstone, lamps in its walls, pillars capped in gold, lanterns, a red
  carpet), and there **the great shaft** drops to the first floor's hub, some 50 blocks: a void 6
  across in the middle, a pool at its foot to fall into, and round it a mud-brick stair in a spiral,
  laid in half blocks (walked without a jump), a lantern each turn.
- **Eight floors of maze** (`world/feature/Labyrinth`, pure, unit-tested), one under another, each a
  maze on a grid of cells 7 blocks apart (3-block tunnels, 4 of rock between), wider and harder the
  deeper:

  | Floor | Walked at | Across | Halls |
  |---|---|---|---|
  | 1, the Burrows | y 98 (galleries 103) | ~84 | nests, larders; earth and roots |
  | 2, the Granaries | y 81 | ~98 | granaries (stolen grain, sacks, barrels), larders; mud brick in courses, planking |
  | 3, the Bone Warrens | y 64 | ~112 | bone heaps, nests; tuff, cobwebs, skulls |
  | 4, the Fungus Caverns | y 47 | ~126 | giant mushrooms on moss, grottoes; spore blossoms, glow lichen |
  | 5, the Underground River | y 30 | ~140 | the river through its halls, grottoes; a run of flooded tunnels across the floor |
  | 6, the Dark Warren | y 13 | ~154 | bone heaps, nests; blackstone and soul soil (soul sand slows), **unlit** |
  | 7, the Storerooms | y −4 | ~168 | her storerooms (chests of `chests/bottomless_storeroom`, barrels, gold), granaries |
  | 8, the Bridal Halls | y −21 | ~182 | her bridal chambers made ready for the Tang monk (red carpet, red candles, flowers), grottoes |

  Each floor is **a hard maze**: a spanning tree of its cells (Kruskal's: it branches and dead-ends
  everywhere), with a few loops (more the deeper); 3–6 halls merged from several cells; and **two
  storeys**: tunnels at its floor and galleries five blocks over them (blobs of upper cells, more and
  larger the deeper), joined to the tunnels by ramps (both ways) and dropped out of by holes in their
  floors and by balconies over the halls (one way only: what you drop into, you must walk out of).
  Every cell is reached both ways by the tree; the drops are shortcuts and traps. Lanterns light a
  share of the tunnels (13% on the first floor, fewer deeper, none on the sixth): long stretches are
  dark. **The way down is hidden deep in each floor**: a spiral stair in the cell farthest *by the
  walk* from where one came in (never within four cells of it as the crow flies; 18–49 cells' walk
  in, the deeper the longer), a step a block of the fall (17 blocks between floors, 29 for the last
  descent to the throne level), a light in its pillar at each end; its **guardian** stands in the cell
  before it (see below). Each floor's stair comes out in the next floor's maze, where its way down is
  again the farthest cell. From the door to her throne the cave falls some 200 blocks, from the mountain's side down
  nearly to the world's floor.
- **The throne level** (y −50): **her palace**, a hall 31 blocks square and 20 high (blackstone and
  gold, red pillars, a carpet from gate to gate up to her dais, flower beds down both sides, censers,
  candles, lanterns on long chains, spore blossoms, shroomlight lamps in two rows), reached from the
  last stair's foot by a processional way (red carpet, lanterns) round to its nearer gate; behind it,
  out of its back gate and down a narrow turning way, **her shrine** (tablets to Li Jing and Nezha,
  candles, incense, flowers).
- **The cave's own rock** (`bottomless_earth`, `bottomless_stone`, `bottomless_deep`, the door's
  `bottomless_gate` and `bottomless_gate_frame`; textures from `assets_source/bottomless_rock.py`):
  every floor's walls, floors and roofs, the shell round each floor, the stairs' wells, the gate
  hall, the shaft. **None digs it** (unbreakable), **blasts it** (resistance 3,600,000), **carves
  it** (`Carving` cost infinite, for every destruction power), **pushes it** (pistons) or **phases
  through it** (the tag `jttw_engine:unphasable`, which `BlockStateBaseMixin` respects). Where a face
  of it is backed by more of it, the face is dressed in the floor's own stone or earth (which may be
  dug, but only to find the cave's rock behind it); ceilings and the one-block slabs between storeys
  are the cave's rock itself. Chorus fruit does not work inside the fortress. Hitting it tells you so.
  The mazes must be walked.
- **The household** (`world/cave/CaveRules`, `CaveHousehold`):
  - **Outside**: no other monster comes of itself over the cave's region, on the mountain or in the
    fortress (`MobSpawnEvent.SpawnPlacementCheck`); instead the mountain is kept **crawling**: round
    anyone climbing it, packs of 3–5 mice and pairs of maids (12% led by a captain with escorts) come
    up out of sight 22–44 blocks off, every 3 s while fewer than 16 are within 48 blocks; a third of
    them **patrol** the road, a stretch at a time, up toward the door or down. Each outpost has its
    watch posted (4–5 mice, a captain at the towers; mixed at the camps; 3 at the nooks), the gate its
    guard.
  - **Inside**: each floor's household is **posted** when the land is made: a group in every hall (two
    in the larger), a few at crossings and in the galleries, more and with more captains the deeper
    (about 14 on the first floor to 30 on the eighth), and its **guardian** before its way down: a
    miniboss (twice a captain's health, kept to a miniboss's 25–40 hits; a miniboss's bite), named for the floor ("Warden of the
    Granaries"), mice on odd floors, maids on even (who mend the rest). **Stronger the deeper**:
    the cave's road goes on down from the mountain's (37 at its foot, 40 at the pit's mouth): regulars
    level 41 on the first floor to the band's top, 46, on the eighth, captains four over (45 to 47),
    guardians 45 to 47 (never more than one past the band's top, so their lady, at 49, stands over all of them;
    the mountain's own creatures 37 to 40, captains 41 to 44). The posted are kept (never despawned, even on Peaceful) and keep to
    their floor: idle and off it (having chased someone up or down a stair, or fallen), they go back
    to their post. **Respawn**: the posted, the guardians, the gate's guard, the outposts' watch and
    her palace's maids come back as every zone's posts do (`ZonePosts`: 20 minutes after a post's last
    falls, no one near; a door once opened stays open); and **the cave breeds**: while someone walks a floor, now and then (every 15 s on the first,
    11 s on the eighth) a group of that floor's strength comes up out of the tunnels 14–32 blocks off,
    while fewer than 5–8 are near. The bred are not kept, and one found off its floor with no one to
    fight slinks away (is gone), so none wanders down into her throne room.
- **The Earth Gods' landings (checkpoints)**: inside the door, on each floor in the cell one first
  steps into from the stair (on the first, the hub's edge), and before her palace's gate, an Earth
  God keeps a niche (his tablet, 土地, and three candles). Step up to one and he will raise you there
  should you fall: your respawn is set there (forced, no bed needed), and you are told. Dying on the
  sixth floor sends you back to the sixth floor's landing, not the mountain's foot.
- **Mouse Spirit** (`entity/cave/MouseSpiritEntity`): 2 hits, a bite of 7% of health; quick
  (0.36), in packs of 3–5 (a captain brings 4). Strike one and the pack turns on you; each darts in,
  bites and scurries back, so a pack circles.
- **Flower Maid** (`FlowerMaidEntity`): 5 hits, a stab of 9%; every 5 s she mends the most
  wounded of the household near her (a fifth of its health; 0.9% of her mistress's), and pressed
  close she breathes out pollen (Slowness II and Weakness for 4 s).
- **Lady Earth Flow** (`LadyEarthFlowEntity`, a `ZoneBoss`, level 49, the cave's gym leader: 62 hits of
  a level-46 character, pink bar), put on her
  dais once when the land is made, two maids beside her: her twin swords cut twice a stroke (9.5%
  each, so six strokes kill; the female blade slows). Every 9 s, a foe within 4.5 blocks, **the whirl**: she sinks low,
  both blades swept out behind her, petals drawn in round her (1 s: step back), then spins twice on
  her heel, a ring of steel: everyone within 4.5 blocks cut for 14% (eight kill), thrown back and
  slowed. At 60% (phase 1) she calls 4 mice and 2 maids and starts to burrow,
  rising behind whoever she fights. At 30% (phase 2), once, she leaves her embroidered shoe in her
  likeness (`EarthFlowDecoyEntity`: weak, no XP or karma, a shoe again when struck down or after a
  minute) and flees out of the palace's back gate to her shrine, strewing petals along the way; no
  blow kills her before that. Alone there she mends, up to half her health; found, she fights to the
  end. Karma +150, captains +5. **She never leaves her throne room**: bound to her cave, she keeps to
  her palace (18 blocks round her dais), and fled, to her shrine (8); drawn or carried six blocks
  past it, or as high or low, she is back where she belongs within a second (`keepToLair`). She is
  never despawned and, killed, does not come back: her palace is a usurper's 20 minutes after (level
  46; see Zones).
- **Drops**: her twin swords (`EarthFlowSwordItem`, male and female: +35% damage, a quick sword;
  held as a pair, drawn into both hands like Nezha's Yin-Yang Swords through `PairedBlade`, every cut
  is followed by a second for 40%), the Stolen Candle of Spirit Mountain, sigils, gold; Spirit
  Whiskers from mice, Night-Blooming Petals from maids (and a second True Samadhi Fire recipe from
  them), the Embroidered Shoe from her likeness; the storerooms' chests (gold, emeralds, bread,
  wheat, candles, iron, now and then a diamond or a captain's sigil, whiskers and petals).
- **The Earth God's Luopan** points to the great door (by the seed and the region's centre alone:
  `Labyrinth.doorOf`), and the Earth God tells of Pit-Hole Mountain's door and the eight floors.
- **Older worlds**: the cave's region is larger, so in a world made before this its place may have
  moved (its host is checked farther out). Ground already made keeps what it has: an old three-level
  labyrinth and its lady stay as they were and work as before (she is not bound to a fortress, so she
  is not leashed and, fleeing, runs off rather than to a shrine); new ground gets the new design, so
  a region half made before may have a fortress cut off at the old chunks' edge (closed in by the
  cave's rock; nothing breaks) or an old lady and, where the new throne room is first made, a new
  one. The Luopan points to where the new door is or will be. (Both bench seeds moved: 12345's from
  −1851 −1872 to 1283 2242, 42's from −2668 879 to −2668 1240.)
- **Cost**: the cave's own feature takes 2.3–3.1 ms a chunk where it lays anything (some 475 chunks
  round a region, 1.1–1.5 s of worker time all told; the old cave took 0.9–1.0 ms a chunk over about
  100 chunks), and posting the household about 0.2 s more; the 289 chunks round the centre take some
  18–20 s to make against 9 s before (`runWorldgenBench -Pbench.mode=cave` reports all three). In
  the GameTests the fortress is laid a few chunks a tick, its rock set straight into the chunks'
  sections: laid the ordinary way into the flat test world, its two million blocks of rock queued so
  much light work that the batch's platforms never came to tick.
- **Art**: `CaveRenderers` (a box model for the mice; a person's frame with skirt, collar, hair,
  ears, gold nose and tail for the maids and her); skins from `assets_source/mouse_spirit.py`,
  `flower_maid.py`, `lady_earth_flow.py`, item icons from `cave_items.py` (helpers in `cave_skin.py`),
  the cave's blocks from `bottomless_rock.py`.
- **Tests and pictures**: `LabyrinthTest` (the same cave from every chunk; eight floors, each under
  the last and wider; a block-by-block walk from inside the door reaches every floor's way in and way
  down, her palace and her shrine, and from her throne the way back up to the door; shut, the door
  holds; each floor a maze of dead ends, branchings, galleries, ramps and drops, its way down deep in
  it; one guardian a floor, more household the deeper; the stairs a step a block; the great shaft
  walked without a jump; the trail to her shrine), `PitHoleTest` (the mountain over every floor that
  rises above the land, the door in its cliff, the road down without a break, the outposts),
  `BottomlessCaveZoneTest` (one region a world, about 380 across, in its ring, its fortress, outposts
  and door within it, clear of the other zones); GameTests in `CaveFortressGameTests` (the fortress laid
  in a world and walked block by block from inside the door to every floor's way down and her throne;
  its walls the cave's own rock; the household posted floor by floor, a guardian before each way
  down; she stays in her throne room, and her fight there, her flight to her shrine; the door barred
  while its guard stands and open when they fall; the Earth Gods' landings; the Luopan to the door;
  no other monster over the cave; the household keeping to its floors) and `CaveGameTests` (the
  creatures and her fight). `./gradlew runFormGallery -Pgallery.only=cave_` (creatures, her, her
  likeness, and the fortress laid off the stage: her palace, dais and shrine, a tunnel of the
  burrows, the fungus caverns, a granary, a storeroom, a bridal hall, a ramp, a stair, a guardian, the
  hub, the gate hall); `./gradlew runWorldgenBench -Pbench.mode=cave` (its region in a real world: a
  plan of each floor's storeys as built, a cut through the whole depth, the household as posted,
  and pictures, from the mountain afar and its road to the door, the gate hall, the shaft, the
  floors, the last descent and her palace, in `run-bench/screenshots/zones/`).

#### The Mystical Waters (levels 47–56)

The Four Seas and the Dragon Kings' crystal palaces (水晶宫, novel chapter 3), in
[`world/waters/`](src/main/java/com/jttw/engine/world/waters/) and
[`entity/waters/`](src/main/java/com/jttw/engine/entity/waters/).

- **Where**: over deep ocean only (deep, deep cold, deep lukewarm and deep frozen ocean, judged just
  under the waves at y 60), the whole column. Four regions a world, about 440 blocks across, 3–4.5
  km from spawn: one in each sea, each sought within 30° of its own compass point. Each region is
  one sea's: the one that lies that way from the world's origin (within 45° of +x the East's, +z the
  South's, and so on), and its palace's gate faces that way.
- **The trench** (`TrenchShape`, `TrenchFeature`): sunk as deep as the world goes, its floor at
  y -58, six blocks over the world's bottom, just over the bedrock: some 120 blocks under the waves,
  85-100 under the deep ocean's own floor. Its walls drop near-sheer a few blocks in from the
  region's edge, their line wandering, broken by ledges every dozen blocks; its floor of sand, gravel
  and clay rolls in low mounds and is laid level round the palace. Worked out from the region and
  the seed alone, so every chunk agrees on it. Caves and lava under it are filled in. Kelp forests,
  seagrass, glowing sea pickles, glow lichen up the walls, now and then a basalt vent or a deepslate
  spire (`TrenchLifeFeature`).
- **The palace** (`DragonPalace`): a blueprint laid column by column by each chunk of the trench, so
  it comes out whole across the chunks it spans: a prismarine plinth, a throne hall of prismarine
  and crystal under a stepped, gilded roof, quartz columns, sea lanterns, the king's gold throne on a
  dais at the region's centre, a runner in his colour; behind it his treasury, four chests
  (`chests/dragon_treasury`) and heaps of gold; coral gardens, lantern towers at the gate, a road of
  lanterns out across the floor. Its king and a guard of eight are put there once (the guard back 20
  minutes after each falls, no one near; a fallen king's throne taken by a king of another sea, a
  usurper at level 56)
  (`DragonPalaceFeature`, the framework's `ZoneLairFeature`, which now lets a zone choose the boss by
  region).
- **Creatures** (`SeaMonster`: level 47 at a sea's edge to 56 over its palace in the middle, the
  palace's guard 55–56; captains three to five over the regulars round them; they breathe water, are not pushed by the
  current, and come in any light in water at least 24 blocks deep):
  **Shrimp Soldiers** (虾兵: swim, spear thrust from further than a sword's reach, dart backward when
  struck), **Fish Soldiers** (鱼兵: carp-men with a sky-piercer halberd; frailest and quickest, they
  swim fastest of all and lunge at a foe from a few blocks off), **Turtle Soldiers** (龟兵: heavy
  infantry under a domed tortoiseshell, slow, armoured, a melon-headed mace; a round shield takes
  most of a blow from in front, and badly hurt they draw into their shells a while, all but unhurt
  and unmoved but striking nothing), **Crab Generals** (蟹将: walk the floor and sink, heavy pinch
  that holds, spring up at what swims above), **Yaksha** (夜叉, the sea patrol: fast swimmers, hurl
  bolts of the tide, raise the alarm so every sea creature near turns on their foe). The shrimp,
  fish and turtle soldiers share one kit (teal-and-gold lacquer, red plumes, sea-blue sashes);
  their models are drawn by `assets_source/shrimp_soldier.py`, `fish_soldier.py` and
  `turtle_soldier.py`.
- **The Dragon Kings** (`DragonKingEntity`, a `ZoneBoss`, level 59, each his sea's gym leader: 62
  ordinary hits of a level-56 character): a dragon in
  a man's shape, robed and crowned, swimming freely; half again a man's height, a long antlered
  dragon's head with trailing barbels, an emperor's dragon robe with its sea-and-cliff hem, wide
  sleeves, a cloud collar and a jade belt. Each wears his own crown and carries his own regalia: Ao
  Guang the mian crown and court tablet (drawn largest, the eldest), Ao Qin a crown of flames and the
  flaming pearl, Ao Run the winged official's hat and a jade ruyi, Ao Shun an icicle crown, a sword
  and the frost pearl. Bolts of his sea (16%, seven kill; three, fanned, past two
  thirds), a claw up close (19.5%, six kill), and his sea's own wrath (14%, eight kill) every 8 s (5 s past a third): **Ao Guang**
  (East, blue-green robes) a tidal surge that hurls all near him away; **Ao Qin** (South, red) the
  boiling sea that scalds and saps strength; **Ao Run** (West, white) a whirlpool round his foe that
  drags all near it to its grinding heart; **Ao Shun** (North, black) the frozen abyss that freezes and
  slows. At two thirds he calls a Shrimp Soldier, a Fish Soldier and a Crab General, at a third two
  Yaksha and a Turtle Soldier.
- **What they give** (what each gave the Monkey King): Ao Run the **Phoenix-Plume Purple-Gold Cap**
  (+20 magic resistance), Ao Qin the **Golden Chain Mail** (+24 physical resistance), Ao Shun the
  **Lotus-Fibre Cloud-Stepping Boots** (+10 of each, +10% speed, swifter in water, softer falls); each
  netherite's equal against blows. Worn together, they let their wearer breathe under the sea. Ao
  Guang's treasury holds the **pillar that fixed the depth of the Milky Way** (定海神针): unbreakable,
  it shrinks into a Ruyi Jingu Bang for whoever lays hands on it and could wield it (Sun Wukong, or
  30 STR), and is gone: one staff per Eastern palace, once.
- **Breathing**: the **Water-Repelling Pearl** (避水珠), crafted from a Heart of the Sea ringed with
  prismarine crystals and gold, carried anywhere about one: its bearer breathes under the sea and
  has Conduit Power while in water (`SeaBreath`). Characters that breathe water already need nothing.
- **XP and karma**: band 47-56, base grunt XP 110 (a grunt pays 396 at the band's scale, about three
  and a half kills a level at 47, the ridge's pace at 20). Killing a Dragon King, an official of
  Heaven, is a crime against it: karma -80 (all four make an outlaw); plundering his treasury -10;
  his soldiers are monsters as any (+1, captains +5). A king's kill is a boss kill: +50 Primordial.
- **Tests and pictures**: `WatersGameTests`, `MysticalWatersPlacementTest`; gallery scenes
  `-Pgallery.only=waters`; the trench in a real world with `./gradlew runWorldgenBench -Pbench.mode=waters`.

#### Hell: the Ten Courts (levels 57–66)

Diyu (地狱), the underworld, and its Ten Courts (十殿), where every soul is judged by the Ten Kings
of Hell and sent on to be reborn. A dimension's zone (`Zones.HELL = Zone.dimension(hell_biome, 57,
66, 125)`): the biome covers all of `jttw_engine:hell`, and the zone gives it its level band. Grunt
XP 125, stepped up from the Mystical Waters' 110 (575 a grunt kill at the band's scale).

- **Terrain** (`world/hell/HellTerrain`, pure Java, the same in every world; the density function
  `dimension/HellHeightDensity` hands it to a noise generator, `worldgen/noise_settings/hell.json`,
  in place of the old flat world): plains of soul ash rolling a few blocks, hills of blackstone and
  basalt where a broad noise runs high, side rivers winding through, and across all Hell from west
  to east the **Naihe** (奈何), ten blocks either side of a wandering line through z −160, its water
  dark as blood (`hell_biome`'s water colour). The surface rules lay ash, soul-soil patches and
  black stone (steep banks, hilltops), soul soil under water, bedrock below. The plains carry
  soul-fire patches, black stone spires and bone heaps (`HellDecorFeature`), never on the works nor
  within 80 blocks of the arrival. Ash drifts in the air; the soul sand valley's sounds and music.
  The dimension keeps its nether sky and fog (so it is dark, and the view is short); its ambient
  light is up from 0.1 to 0.2.
- **The works** (`world/hell/HellWorks`, laid chunk by chunk through `Site` from the plan alone, so
  chunks agree whichever is made first): the **arrival plaza** round 0, 0, paved in rings, lit by
  soul lanterns, with the **Gate of Ghosts** (鬼门关) at its north edge, its arch the road's and
  a **side door** either side of it filled with the ghost veil, the way back up to Fengdu (set up
  as a traveller arrives in a Hell made before them, `HellWorks.ensureReturnDoors`); the **road** north, lit
  every 24 blocks; the **Bridge of Helplessness** (奈何桥), an arched bridge on piers over the Naihe,
  with **Meng Po's pavilion** (her soup in a cauldron) at its north end; and the **fortress of the
  Ten Courts** (`TenCourts`): walls 128 × 268 blocks with corner towers and a gatehouse (its plaque
  地狱十殿), its gate 320 blocks north of the arrival; an avenue lit by braziers runs north through
  it. Up the west side the Office of Records (the clerks' shelves and desks) and the First to
  Fourth Courts; at the head of the avenue King Yama's great hall, the Fifth; back down the east
  side the Sixth to the Tenth, King Zhuanlun's, whose yard holds the upright **Wheel of
  Reincarnation** (六道轮回: a gold rim, six spokes for the six paths). Each hall: a plinth and
  steps, dark walls between red pillars, barred windows, a hip roof of dark tiles, red columns
  within, a red carpet to the judge's bench (ledgers on lecterns, candles), the throne on a two-step
  dais before a red screen with black banners, shelves of scrolls, braziers, soul lanterns on
  chains, its plaque over the door (court, king, what it judges), and the instruments of its court:
  the Karma Mirror (1), ice (2), iron hooks (3), the scales (4), the register and gold seals (5),
  the bell of the Wailing City (6), cauldrons of boiling oil (7), the crushing carts' wheels (8), an
  iron cage of soul fire (9), Meng Po's soup (10). Two guards (Ox-Head and Horse-Face) stand at
  each door (captains at Yama's), three clerks keep the Office of Records, the Impermanence pair
  and a clerk wait on Yama, and each king sits on his dais, all put there once as the land is made.
  The guards and clerks come back 20 minutes after they fall, no one near (`ZonePosts`); a fallen
  king's court is a usurper's 20 minutes after (level 66, a boss of another house or another of the
  Ten: see Zones), each court its own seat.
- **The way in and out**: everyone comes down through Fengdu's Gate of Ghosts, once they have come to
  it by an Ancient City's frame and fought it open (and Wukong at level 30, and the Dragon's and
  Wukong's dimension travel, as before), all to the arrival plaza (`JTTWDimensions.teleportTo`: its
  safe ground, the Book's dais); everyone goes back up by its Gate of Ghosts' side doors, to
  Fengdu's gate, which burns for them so they can come down again (see "Fengdu" below).
- **The arrival's safety** (`HellRules`): a level-30 Wukong comes here for the Book, and anyone of any
  level may come down through Fengdu. The fortress
  and its kings are over 300 blocks north, over the Naihe (the nearest king 363 blocks off);
  nothing of Hell spawns by itself within 160 blocks of the arrival (the server spawns within 128
  of a player, so none ever spawns round someone standing there); the officials spawn only within
  the fortress's walls, the ghosts out on the plains; and the plaza is a **sanctuary** (48 blocks):
  no creature or king of Hell attacks anyone in it, and one that chased someone in gives up.
  `JTTWDimensions.findSafeSpawn` now makes a chunk before it reads its height (it used to read an
  unmade chunk's as the world's floor, and set the Book's dais in the air at y 64).
- **Ox-Head** (牛头, `entity/hell/OxHeadEntity`, level 58): 10 hits, a blow of 8%; kept at a
  distance he lowers his horns and **charges**, goring (his blow and a half, 12%) and throwing what he
  runs down. **Horse-Face** (马面, `HorseFaceEntity`, 58): 6 hits, 9%; casts his **hook on a
  chain** at whoever keeps off (4–12 blocks) and hauls them in, slowed.
- **Judge-Clerk** (判官, `JudgeClerkEntity`, 58): 4 hits, 6%; every 5 s he **writes his quarry's
  name** in his ledger: marked (glowing, 10 s), and every creature of Hell near them that is idle
  comes for them; every second writ **summons** a guard, Ox-Head and Horse-Face by turns, two of his
  at most.
- **Black and White Impermanence** (黑白无常, `ImpermanenceEntity`, 59): met in the world, one comes
  with the other. The Black (6 hits, 10%) **shackles** (Slowness III 2 s); the White (4.5 hits, 9%,
  a longer reach) **drinks** half of what his tongue takes and weakens. Near each other each takes a
  quarter less from every blow; when one falls the other is **enraged** (blows a fifth harder, Speed II).
- **Wandering ghosts** (游魂, `WanderingGhostEntity`, 57): 2.5 hits, 7%, in twos to fours on the
  plains; their touch **chills** (slowed, hungry); they drift, never take falls; restless dead
  (healing harms them).
- **Hell's road is its courts** (`HellMonster.courtLevel`): its creatures are of the nearest hall's
  level, 57 in the First Court (and the Office of Records) and one or so more a court to the band's
  top, 66, in the Tenth; the door guards of each court of its level (Yama's, captains, four over his
  court's).
- Captains as in every zone (purple, larger, stronger, with escorts; the Impermanence's captain
  brings only its partner), three to five over the regulars round them, never past level 67.
- **The Ten Kings** (`KingOfHellEntity`, `HellKings`, ZoneBosses on their thrones, each his court's
  gym leader: 64 hits of a level-66 character, a tablet blow that kills one in six (18%), red bars;
  the nine under King Yama all level 68, lesser bosses two over Hell's band),
  each passing his court's **judgement** every 7 s on whoever fights him within 14 blocks:
  1. **Qinguang** (秦广王), the **Karma Mirror**: a beam for his blow, ×0.6 for the good (karma 100+),
     up to ×1.1 for the wicked (1 + 0.1 × |karma|/1000; at −1000 and below, six kill);
     and every 12 s **the mirror's flash**: he holds the mirror high (1.5 s, light gathering in it, the
     ground before him glinting), then its light falls over a 90° cone 10 blocks before him, the way
     he faced as he raised it: 13% of the health of all in it (eight kill), weighed by karma as the mirror's blow
     (thirteen kill the good, seven the wicked), and it dazzles (blind 1.5 s); step aside or behind him;
  2. **Chujiang** (楚江王), the cold hell: all within 8 **frozen** (10.8%, ten kill; frost, Slowness III 5 s);
  3. **Songdi** (宋帝王), the **iron hooks**: hauls his foe from up to 14 blocks to his feet and strikes;
     and every 12 s **the ring of hooks**: chains rattling, the ring they reach marked in sparks
     (1.5 s), then they lash out all round him: all within 7 torn (14%, eight kill) and dragged in toward him;
  4. **Wuguan** (五官王), the **scales**: takes half the difference of their health shares from a
     healthier foe; and every 12 s **the weight of the scales**: the glyphs of the weighing settle
     over the ground round him (1.5 s), then the pans come down on all within 6 (15.5%, seven kill; Slowness III 3 s);
  6. **Biancheng** (卞城王), the **bell** of the Wailing City: all within 7 **kneel** (pinned: cannot move) 2 s,
     struck by its note (10%, ten kill);
  7. **Taishan** (泰山王), **boiling oil**: all within 7 scalded (10.8%, ten kill) and burning (he does not burn);
  8. **Dushi** (都市王), the **crushing carts**: charges, crushing (×1.1, six kill) and flinging everything he
     runs down on the way (each once a charge);
  9. **Pingdeng** (平等王), "Equal": **equal measure**, a third of every blow struck him struck back;
     and every 12 s **impartial judgement**: a golden balance over his head, its ring marked 8 blocks
     round him (1.5 s), then one blow, the same on all within it (16%, seven kill), a column of light on each;
  10. **Zhuanlun** (转轮王), the **Wheel**: struck down the first time he is **born again** at half
      health (45 hits, then 22½); his judgement is **Meng Po's soup** (his foe's good effects forgotten; nausea);
      and every 12 s **the Wheel turning**: it spins over his head in soul fire, faster and faster
      (1.5 s), then rolls out from him in a shockwave: all within 8 struck (16%, seven kill) and flung back.

  Each sentence on all is telegraphed alike: he stops, lifts tablet and emblem high over his head,
  trembling, while his court's sign gathers and its reach is marked on the ground; then brings them
  down. Raising it, he strikes no other blow and passes no judgement.
- **King Yama** (阎罗王, `KingYamaEntity`, the Fifth Court, level 69, Hell's gym leader: 78 hits of a
  level-66 character, his blows killing one in six (18.5%), purple bar,
  the climax): phase 0, his blows and **the Writ** every 10 s (marked; Hell's creatures near come);
  at 66% **his court rises** (Ox-Head, Horse-Face, a clerk) and he passes **sentence**: a ring of
  soul fire where his foe stands, and 1.5 s later judgement falls there (a fifth of the health of
  whoever is still in it, so five kill, and they burn; step out); at 33% **the wrath of Yama**: he grows (×1.35),
  strikes 8% harder (five blows kill) and moves 30% faster, sentences fall every 3.5 s, and soul fire round him
  scorches whoever stands within 3.5 blocks.
- **Drops** (1.21 loot tables): each king his relic, sigils (3–4), Hell Bank Notes, gold, and a 10%
  chance of a **Page of the Book of Life and Death**: the Shard of the Karma Mirror, the Rime
  Shackle, the Iron Soul-Hook, the Weight of the Scales, the Bell of the Wailing City, the Ember of
  the Oil Cauldron, the Spoke of the Crushing Cart, the Rod of Equal Measure (`HellRelicItem`,
  forging materials with their stories), and from Zhuanlun **Meng Po's Soup** (`MengPoSoupItem`:
  drunk, every effect is forgotten; the bowl is left). King Yama leaves his **Vermilion Brush of
  Judgement** (`JudgeBrushItem`: +40% damage, what it strikes glows and is weakened), his **Seal**,
  one or two Pages, 5–6 sigils and netherite scrap. The creatures: Hell Bank Notes (ghosts),
  Ox-Head's Horn, Horse-Face's Mane, Underworld Ink (clerks), the Soul-Catching Chain (the
  Impermanence).
- **Karma and alignment**: the Ten Kings are Heaven's judges of the dead; striking one down defies
  Heaven's order: −35 (King Yama −70; all ten −385, past outlaw, as the Dragon Kings).
  Hell's lesser officials are doing their work: no deed either way (captains +5 as any); a ghost
  laid to rest +1. Each king is a boss of the reincarnation's Primordial path (+20, Yama +50);
  ghosts are undead (+2 Heavenly).
- **Art**: `client/renderer/HellRenderers` (guards: a big armoured frame, a beast's head, a fork or
  a hooked spear, burning eyes; the clerk: robe, winged cap, brush and ledger; the Impermanence:
  long robes, three-tier hats, the White's tongue; ghosts: legless, translucent, trailing; the
  nine kings: one frame in King Yama's likeness, plainer and a head shorter (some 2.7 blocks), a
  man's stern face and a three-lock beard, each in his own colour, the ivory tablet in his right
  hand and his court's emblem held up in his left and worked on the badge on his breast (the Karma
  Mirror, ice, an iron hook, the scales, the bell, the cauldron of boiling oil, a cart wheel, 平,
  the Wheel of Reincarnation); King Yama a frame of his own, 256px, a head taller than the rest (some 3.3 blocks, his
  hitbox 1.2×3.3): a dark oxblood face, bulging eyes smouldering like coals under brows knit into a
  V, tusks, a wild black beard and moustache flaring out, the mian crown with its 王 plaque and red
  and gold bead strings, a dragon robe of crimson and black in tiers worked with gold dragons,
  flames and skulls over a sea-and-cliff hem, armoured shoulders bitten by gold beasts' heads, a
  jade belt with a skull buckle and a soul-chain at his hip, great sleeves, the vermilion brush and
  the Book of Life and Death, and behind his head a ring of hellfire set with skulls, embers
  drifting from it; in his wrath its flames flare, his eyes blaze, his jaws gape and fire runs
  round his hem); skins from `assets_source/hell_guards.py`, `hell_officials.py` (helpers in
  `hell_skin.py`), `hell_kings.py` (the nine kings' model and skins), `king_yama.py` (his model
  and skins), icons from `hell_items.py`.
- **Tests and pictures**: `HellGameTests` (creatures, captains, XP, each king's judgement and relic,
  Yama's phases and sentence, karma and alignment, the arrival in the real Hell: level, the one
  Book, nothing near, the sanctuary; the kings on their thrones), `HellZoneTest` (the land, the
  plan, the arrival's distance and spawn rules); gallery scenes `-Pgallery.only=hell_` (creatures,
  the kings' lineup, the nine in a line with their emblems and each of them close, Yama close,
  side, back, beside a man and beside two kings, and in his wrath,
  and pieces of the works laid off the stage: a court hall, Yama's hall and his throne, the Tenth
  Court and the Wheel, the bridge and Meng Po's pavilion); Hell as it is
  with `./gradlew runWorldgenBench -Pbench.mode=hell` (a map of it from the arrival to the north
  wall, what stands where, and pictures, some with night vision: it is dark).
#### The Flaming Mountains (levels 67–77)

The Flaming Mountains (火焰山, chapters 59–61: eight hundred li of fire where nothing grows, the
bricks of Laozi's furnace that fell when the Monkey King kicked it over) and in their heart Red
Boy's Fire Cloud Cave on Mount Haohao (火云洞, chapters 40–42), in
[`world/fire/`](src/main/java/com/jttw/engine/world/fire/) and
[`entity/fire/`](src/main/java/com/jttw/engine/entity/fire/).

- **Where** (`Zones.FIRE_MOUNTAIN`): over hot, dry country (desert, badlands, eroded and wooded
  badlands, savanna, savanna plateau, windswept savanna; White Bone Ridge's hosts and plain savanna),
  the whole column. One range a world, about 640 blocks across (never under 540), 4.5–6 km from
  spawn, on hot, dry ground and clear of every zone before it.
- **The range** (`RangeShape`, `FlamingRangeFeature`, pure and unit-tested): from the region's edge
  the ground climbs over the outer fifth into long chains of red mountains (ridged noise, rounded
  summits, up to some 90 blocks over the land), valleys between them; Mount Haohao rises 55 more
  over 140 blocks round the heart, and **Withered Pine Ravine** (枯松涧) is cut through it, 37 blocks
  wide, from the cave's great door to the region's edge, withered black pines along its floor. Faces show
  banded flame rock, red, orange and brown terracotta with seams of magma; high ground is flame
  rock and cinder ash; valley floors (about a quarter of it) are **burning earth**. Fire on the
  burning earth in patches, seams of lava in level valley floors, smoking vents, charred basalt
  spires (`FlameSeamsFeature`). Orange sky and fog, falling ash, no rain; nothing grows.
- **The outposts** (`RangeShape.outposts`, laid by `FlameSeamsFeature`): watchtowers of red brick in
  rings of black stone, a signal fire smoking on top and braziers round them, one tried every 72
  blocks over the range (some 20–35 a region) and three on the ravine's rims looking down on the way
  to the door. Each stands on the land, never sunk in it: its paving is seated on the ground round
  it (the range's height as it will be raised, sampled at its middle and in rings 3, 6 and 9 blocks
  out, and the level seven in ten of them lie at or under), a plinth of black stone under it down to
  the ground wherever the ground falls away, the ground over it cut away and the ground round it
  cut back 8 blocks out, rising a block for each block out (`FlameSeamsFeature.seat`,
  `layOutpost`). Each has its band, put there once and kept, on its paving: a captain and three or
  four imps and brutes; about one band in three is a patrol that roams 40 blocks round its tower. A
  band all fallen is back 20 minutes after, no one near (`ZonePosts`). The mountains'
  own creatures come in bigger packs than before (imps 3–5, brutes 1–2) and one in eight is a captain.
- **The heat** (`FireMountain`, `BurningEarthBlock`): lit burning earth scorches whatever stands on
  it once a second, **4% of its health** (magma's hot-floor damage, so armour softens it) and sets it
  alight for 3 s; **Fire Resistance**, or being fire-proof, keeps it off entirely (as it does the
  fire on the ground). Rock, ash and fanned earth are safe. Fire on lit earth never goes out, and
  creeps across it (never more than five fires in a 5x5).
- **The Banana-Leaf Fan** (芭蕉扇; one a world, in the treasure chest on Mount Kunlun's summit: see
  "Mount Kunlun" below) puts it out: as before it throws back what is in front (the Bull
  Demon King's blows it away), and now each wave also quenches the burning earth and ordinary fire
  in its sweep (60° either side) out to **12 blocks** (20 in the Bull Demon King's hand) for **a
  minute**, after which the earth kindles again by itself. It does nothing to the Samadhi fire.
- **Creatures** (`FireMonster`: level 67 at the mountains' edge to 71 by the Fire Cloud Cave's door,
  captains three to five over the regulars round them, fire-proof, any light): **Ember Imps**
  (火焰小妖: quick and frail, 4 hits; claws 7% that set alight; fling an ember for 8% every 2.5 s
  from 3–16 blocks, a captain three at once; strike one and the rest turn; water hurts them) and
  **Furnace-Brick Brutes** (炉砖怪, bricks of the Eight Trigrams Furnace that stood up: 12 hits,
  armoured, blows of 12%; when close they raise both fists for 0.8 s and slam the ground: 10%, thrown
  up and set alight within 4 blocks).
- **Red Boy's Six Strong Generals** (六健将, `StrongGeneralEntity`, one model, six skins): always
  minibosses (25 hits, blows of 16%, seven kill; their tricks 60% of that), each the guardian of one of the cave's first six
  floors (below), kept even on Peaceful. Each has his trick every 5 s: **Cloud-Soaring
  Sprite** (云里雾) leaps high and lands in a burst of blinding cloud; **Fog-Diving Monster** (雾里云)
  vanishes into fog and comes out behind his foe; **Mad-Hurrying Wind** (急如火) charges, fire springing
  up in his track; **Swift-Rushing Wind** (快如风), the fastest, hurls all near him away; **Flame-Head
  General** (兴烘掀) bursts into flame; **Eye-Spark Officer** (掀烘兴) shoots sparks from his eyes.
- **Red Boy** (红孩儿, the Holy Child King; `RedBoyEntity`, a `ZoneBoss`, level 80, the mountains' gym
  leader: 72 hits of a level-77 character, red bar),
  son of the Bull Demon King and Princess Iron Fan: a child in a red stomacher with **his own
  fire-tipped spear** (`RedBoySpearItem`, not Nezha's). Spear thrusts that set alight, six of which kill (19.5%). Every 7 s he stops,
  strikes his nose twice (1 s) and breathes the **True Samadhi Fire** (三昧真火) from mouth and nose: a
  cone 10 blocks long, a blow of 16% (seven kill), then the burning; flames are left on the ground. At two
  thirds (phase 1) his **five fire carts** (五辆小车) are set out round his lair in the five-element
  pentagon and lit: each belches the fire round itself every 3 s, and while any stands **every blow
  on him is halved**; break them first (5 hits each). At a third (phase 2) the fire comes from his
  nose and mouth both: a ring of it bursts out all round him, and from then on it is fiercer and
  comes every 4 s. He says nothing, to anyone (a Bull Demon player too, whom he fights all the
  same): no chat line, no taunt, no phase call.
- **The True Samadhi Fire** (`SamadhiFire`, the `samadhi_fire` effect, `SamadhiFlameBlock`): it burns
  as a status, **3% of health a second** (4.5% at its fiercest), whatever the burning one is (the
  fire-proof burn too; armour does not soften it). **Water does not put it out, it feeds it**: in
  water or rain it burns half again as hard (the Dragon Kings' rain in chapter 41); milk does
  nothing. Only **Fire Resistance** stands against it, and only **halves** it (the breath's blow and
  the burning). On the ground it burns out after 5–8 s and only so: no water flows into it or can be
  poured on it, a splash of water does not douse it, no hand beats it out, the fan passes over it.
  It spares Red Boy's household. His breath lights the Eight Trigrams Furnace in its line.
- **The Fire Cloud Cave** (`FortressPlan`, a plan of the whole fortress worked out once, the same in
  every world, pure and unit-tested; `FortressBlocks` says what each of its codes is, and the range
  lays it column by column so it comes out whole across its chunks; `FireFortress` keeps it). Red Boy
  is no longer in the open: the cave goes eight floors down into Mount Haohao, under the region's
  centre, and he sits at the very bottom.
  - **The great door**, at the head of Withered Pine Ravine: a facade of black stone in the cliff,
    two red pillars with braziers on them, two crouching demons with eyes of fire, a gilded plaque
    and a tiered roof; the door two leaves of bronze banded with black iron and studded with gold,
    **13 blocks wide and 14 high**. Before it a forecourt of standing braziers and **its two
    wardens** (Furnace-Brick Brute minibosses) with four imps. **The rule**: while the
    wardens live the door is barred from within (knock on it, that is use it, and they turn on you);
    kill them and knock again and it grinds open from the middle out over two seconds, and stays
    open. It does not yield to picks (nor do the floors' grates). A player in creative opens it
    regardless.
  - **Eight floors**, each a great hall of its own (at least 1,200 open columns, none a corridor),
    each 13 blocks under the last, reached down a stair cut in the rock round the halls (on a
    different side each time); each floor's way down is shut by an iron grate its **guardian**
    holds, and his death throws it open: (1) **the Outer Court**, a pillared nave with aisles, the
    flame idol on its dais; (2) **the Forges**, nine bays round a great furnace, lava hearths under
    hoods, anvils; (3) **the Lava Channels**, a rough cavern crossed by three channels of lava, a
    bridge over each; (4) **the Barracks of the Six Generals**, a round yard and six walled
    barracks round it, their standards; (5) **the Kitchen of the Captives**, the great steamer over
    its fire pit, cages along the wall, cauldrons; (6) **the Smoke Galleries**, a maze of narrow
    brick galleries, smoke in the dead ends, dim; (7) **the Hall of the Five Carts**, a round hall,
    the five-element pentagon in its floor round a well of fire, the five carts on their daises;
    (8) **the Magma Cistern**, a lake of lava, a causeway winding over it from island to island.
    Guardians: the six generals on floors 1–6 (the two Winds, Flame-Head, Eye-Spark, Fog-Diving,
    Cloud-Soaring), a warden (a Furnace-Brick Brute miniboss) on 7 and 8.
  - **The garrisons**: 15–17 a floor besides the guardian, placed by the plan on open floor (not on
    fire). Levels step up, the road going on down from the mountains' 71 at the door: regulars 72 on the
    first floor to the band's top, 77, on the eighth, captains four over (no more than 78), guardians
    five over (76 to 78); captains grow from one or two on the first floors to
    three to five lower down. A floor is **mustered** when someone comes near it (the forecourt
    when near the cave, the first floor at the door, each floor below when someone is on the floor
    over it). Each member is kept to its floor: one that strays up or down a stair (or anywhere off
    it) is taken back to its post, so nothing walks down into the throne room. Nothing of the
    world's own is born inside the cave. **Respawn**: a floor whose garrison is all dead stays
    empty for **20 minutes**; after that it is mustered again the next time someone comes near
    while no one is on it. A grate once opened stays open.
  - **Checkpoints**: coming down a stair onto a floor's landing marks it; the **return brazier** by
    the door (use it) then brings you straight back down to the deepest landing you have reached, so
    a death does not mean the whole descent again. It works only once the door is open.
  - **The throne room**, at the bottom (113 blocks under the door): a hall 53 by 59, its nave 18
    high, two rows of great red pillars, a moat of lava down each side, lamps in the floor round the
    open arena of his gilded dais (room for his five carts and his ring of fire), a red carpet from
    his doors, and behind, his throne of gold on its steps before a wall of flame; **his treasury**
    chest beside it (True Samadhi Fire, gold, ember cores, furnace bricks). Red Boy is put on his
    dais once, when the land is made (`FireCloudCaveFeature`), and is kept to the throne room:
    should anything bring him off it he is taken back to his dais. His fight (phases, bar, carts,
    breath) is as it was. Fallen, his dais is a usurper's 20 minutes after (level 77; see Zones).
  - **Old worlds**: chunks already made keep what they have. Where the region's centre was made
    before (the old cave, its hall at the gorge's level, Red Boy in it), the old cave is left as it
    is and none of this applies there. Where the centre is new but some of the fortress's chunks
    were made before, the fortress is finished over each of them when it is first loaded with someone
    near (a chunk most of whose sampled blocks are not the plan's is laid over whole; one changed a
    little by players since is left alone), so the way down is whole and Red Boy is never shut away.
  - **Where it is**: the Earth Gods' luopan points to the great door, and says so.
- **What they leave**: Red Boy **2–3 True Samadhi Fire** (the furnace's fuel; a real source of it at
  last), **his own spear** one time in two (55% with Looting I, +5% a level after), ember cores,
  furnace bricks, gold, sigils. **Red Boy's Fire-Tipped Spear** (火尖枪, `RedBoySpearItem`): a slim
  red-lacquered shaft ringed with gold, a red tassel, a gilded head; **+45% damage**, attack speed
  −2.6 (1.4 a second), fire-proof as an item, and nothing more: no use, nothing on a hit. Nezha's
  Fire-Tipped Spear no longer falls from him. Imps leave **Ember Cores**, brutes **Furnace Bricks**, generals both, a
  captain's sigil and now and then a True Samadhi Fire. Three ember cores, two furnace bricks and a
  captain's sigil make a True Samadhi Fire (`true_samadhi_fire_from_the_mountains`). Flame rock,
  cinder ash and burning earth can be mined.
- **XP and karma**: band 67–77, base grunt XP 130 (728 a grunt at the band's scale, about four kills
  a level at 67, near the waters' pace at 47). Subduing Red Boy, who wanted to eat the Tang Monk, is
  a good deed: **karma +150**, as Lady White Bone; the generals are captains (+5), the rest monsters
  (+1). Red Boy's creatures are **not** allied to a Bull Demon player.
- **Art**: `EmberImpRenderer`, `FurnaceBruteRenderer`, `StrongGeneralRenderer`, `RedBoyRenderer`,
  `FireCartRenderer` (box models and skins with glowing parts from `assets_source/fire_court.py`);
  blocks and items (Red Boy's spear among them) from `assets_source/fire_items.py`.
- **Tests and pictures**: `FireGameTests` (heat, fan, Samadhi fire and water, Red Boy's breath,
  carts and phases, that he says nothing through a whole fight (to a Bull Demon too), his spear's
  bare stats, creatures, generals, drops, XP, karma), `FireFortressGameTests` (the whole fortress laid
  off the grid: the door's rule, every floor's way down walked by a zombie from the door to the dais,
  the musters, a guardian's death opening his grate, a cleared floor staying empty, the garrisons and
  Red Boy kept to their floors, his fight in the throne room, the landings and the brazier, the
  luopan), `FireFortressPlanTest` (eight floors, a way on foot from door to throne that each grate
  shuts, hall sizes, garrisons, the throne room's arena), `FireMountainPlacementTest` (the bigger
  region, its outposts); gallery scenes `-Pgallery.only=fire_`; the range, the outposts and every
  floor in a real world with `./gradlew runWorldgenBench -Pbench.mode=fire`.

#### Mount Wuzhuang (levels 78–88)

Mount Longevity and the Temple of the Five Villages (万寿山五庄观, chapters 24–26), home of the
Great Immortal Zhenyuan (away, until his disciples are all killed) and his ginseng-fruit tree, in
[`world/wuzhuang/`](src/main/java/com/jttw/engine/world/wuzhuang/) and
[`entity/wuzhuang/`](src/main/java/com/jttw/engine/entity/wuzhuang/). The first **aerial** zone
(`Zones.MOUNT_WUZHUANG`): `Vertical.above(192)`, over the world's own clouds, the ground under it
left to its host.

- **Where**: over serene country (plains, sunflower plains, forest, flower forest, birch and
  old-growth birch forest, meadow, cherry grove), judged at y 256 (where a column's biome is always
  its surface biome). One region a world, about 260 blocks across (never under 224), 6–7.5 km from
  spawn, clear of every whole-column zone's (being aerial, it could float over the Bottomless Cave,
  but their rings are kilometres apart).
- **The island** (`IslandShape`, pure Java, unit-tested; laid chunk by chunk by
  `MountWuzhuangFeature` at the top-layer step, so the ground under it keeps its own trees and
  grass): a rough disc about 150 blocks across, its edge wandering ±12; a terrace at y 236 laid dead
  level under the temple and the road to its gate; rolling grass elsewhere, pines, flowers, moss;
  four peaks of grey crag streaked with white calcite, the highest (42 over the terrace) behind the
  garden; cliffs at the rim; its underside hanging some 40 blocks like the torn-out roots of a
  mountain (never below y 196), with hanging roots and glow berries; three **waterfalls** spilling
  off the rim (two blocks wide, falling to the ground); banks of **cloud** (celestial cloud) round
  its flanks and wisps under it. Rock is laid only into open air: a mountain of the world below that
  reached so high would keep its own.
- **The way up: the cloud stair** (云梯). A helix of cloud steps out beyond the island's southern
  rim, winding from the ground to the terrace in **half-block steps** (no jumping), 32 to a turn, a
  turn falling 16 blocks (about ten turns from plains country), with a stele at its foot (万寿山,
  Mount Longevity) and a cloud bridge from its top to the road of lanterns that leads to the gate.
  The cloud bears up whoever walks it: a step on it gives Slow Falling for 20 s (`CloudStepBlock`),
  so whoever slips off drifts down unhurt. Characters who fly (Wukong's cloud, the Heavenly cloud,
  the Peng) simply fly up.
- **The temple** (`Temple`, a blueprint laid column by column; the region's centre is before
  Zhenyuan's altar, his place, though he is away): the mountain gate in the south wall with its plaque 五庄观 and the novel's couplet
  (长生不老神仙府 · 与天同寿道人家); a paved front court with a smoking censer, stone lanterns, pines,
  the bell tower and the drum tower; the disciples' wings down both flanks; the **Hall of Heaven
  and Earth**, double-eaved, red columns within, and at its back the **altar** where, as the novel
  says, only the words 天地 are honoured; through a moon gate the **garden**: a pond with lilies, a
  pavilion, rockeries, flowers, and the ginseng-fruit tree.
- **The ginseng-fruit tree** (`GinsengTree`): a broad trunk, green boughs, leaves like the
  plantain's, and **twelve fruit** (`GinsengFruitBlock`, shaped like a newborn child) hanging under
  its low boughs, **all ripe** when the land is made. The tree fruits once in nine thousand years,
  and so here **never again**: nothing ripens or grows back, and a fruit lost (withered, sunk,
  dissolved, eaten) is gone for good; a world has only so many. The fruit **fears the five elements**:
  - struck with **gold** (any golden tool, or the **Golden Striker** 金击子) a ripe fruit falls
    whole; struck with anything else (iron, wood, a bare hand) it falls and withers; an unripe one
    only withers;
  - falling, no hand can catch it: only a **Silk-Lined Tray** (丹盘) held under it (`Wuzhuang.fruitTick`);
  - wherever it comes to rest on the bare earth it **sinks into the ground** and is gone, unless on
    something lined (wool, a carpet, a bed); in water it **dissolves**;
  - **eaten**: healed whole, Regeneration II 30 s, Absorption III 2 min; and the **first** a soul
    ever eats adds its 47,000 years: **+20% maximum health, for good** (an `ADD_MULTIPLIED_TOTAL`
    modifier, kept through death and into a new life). And whoever has eaten one is **the temple's
    enemy for good** (`Wuzhuang.isTempleEnemy`, kept by the soul through death, relogs and new lives):
    every disciple, Clear Wind and Bright Moon fight them on sight.
- **Daoist disciples** (`DaoistDiscipleEntity`, level 78): 6 hits, a sword stroke of 9%; at 3–10
  blocks a **binding talisman** (定身符: pinned 1.5 s, weakened). Righteous: they fight no one
  unprovoked, except a thief of the fruit and whoever has eaten one; strike one and those near come.
  **Finite**: each temple's household is the **24** put there as the land is made (22 disciples
  about the hall, court, wings, gate and garden, and the two youths; the novel's forty-eight, half
  away with their master). They never spawn of themselves (not in the biome's spawners, natural
  spawns refused), are kept even on Peaceful, and never come back.
- **Cloud cranes** (`CloudCraneEntity`, level 80): white, black-tipped wings, red crowns; they fly
  (never fall) within 40 blocks of where they came into the world and harry anyone who comes up;
  from 3+ blocks above they **swoop**: a dive, the beak for its blow and a half,
  and whoever is struck is thrown back (mind the edge). 4 hits, 7% (the swoop 10.5%).
- **The island's road** climbs from its edge to the temple in its middle: its creatures of level 78
  (cranes 80) at the edge to 88 at the temple, the household of the temple the highest of them.
- Captains as in every zone (purple, larger, stronger, escorts of two), three to five over the
  regulars round them, never past level 89.
  Spawned on the island in any light: cranes only (weight 100, in twos and threes).
- **Clear Wind and Bright Moon** (清风, 明月; `TempleYouthEntity`, mini-bosses, the temple's elite,
  level 84, 32 hits, a sword stroke that kills in seven (15.5%), a blow takes at most 6% of their
  health), by the tree, fighting as a pair (strike one and the
  other comes): Clear Wind's **gust** hurls his foe away; Bright Moon's **moonlight** blinds and slows
  and mends his partner by a tenth; both **curse** a thief roundly (it weakens). They count among the
  temple's disciples and fight to the death; struck down, Clear Wind always leaves the **Golden
  Striker**, Bright Moon the **Silk-Lined Tray**: the two things it takes to bring a fruit down whole.
  Each pays as a General.
- **The Great Immortal Zhenyuan** (镇元大仙, `ZhenyuanEntity`, a `ZoneBoss`, level 91, the mountain's
  gym leader: 60 hits of a level-88 character (the one he is built for), a whisk stroke that kills in
  six (19%), yellow bar). **Not at his temple**: as in the novel, he is away (hearing the Primordial Lord
  lecture), and no lair puts him there. **He comes back for whoever kills the last of his
  disciples** (`TempleRecord`, saved data of the overworld keyed by the temple's region centre, so it
  holds with the temple's chunks unloaded and through restarts): each of the 24 is marked as its
  temple's; each death is written down; when the last falls at a player's hand (their blow, or the
  last a player struck it), that player is marked and **a deep bell tolls** for them. **A full day
  later** (24,000 ticks of the overworld's game time from that kill; sleep does not hasten it) the
  bell sounds once more, far off, a minute before; then Zhenyuan **arrives near them, wherever they
  are** (any dimension, any place: a spot 4–8 blocks off with firm ground and room, else where they
  stand), down a column of cloud out of the sky, a ring of cloud rolling out over the ground and
  gold motes spiralling up about him, keeping to no lair, and fights them **to the death**, hunting
  them within 64 blocks. If they are not in the world then, he comes when they next are. Once for
  each temple; no word is spoken. His kit: **hard to harm** (no blow takes more than 6.5% of his
  health, some four ordinary hits; one blow in four struck hand to hand he brushes aside with his whisk); **the Universe in
  his Sleeve** (袖里乾坤, `Sleeve`): every 12 s (7 s from 30%) his sleeve is raised and the air drawn
  toward it for a second, then whoever he fights within 12 blocks (from 30%, everyone within 8) is
  **swept in**: out of the world (unseen, blind, held at his side wherever he goes, nothing can
  touch them nor they anything), for 5 s, then cast out bruised (a tenth of their health), unless
  they **struggle free** first (five blows at him from within: they burst out unhurt, and he
  staggers, unable to parry, for 2 s). **The sweep of the whisk**: every 11 s, a foe within 6 blocks,
  he lifts the whisk over his head and whirls it, a ring of cloud gathering at the sweep's edge
  (1.25 s: get out of it), then sweeps it round him: a gale off its horsetail strikes everyone within
  6 blocks he fights (the temple's enemies and whoever has struck him; never one who gave him no
  cause) for 16% (seven kill) and blows them back. At 60% he **calls his disciples**: any of his household still
  living within 48 blocks come to his side (he makes none, and when he comes for their killer there
  are none). There is no trial: no yielding, no rest, no guest's leave.
- **Drops**: Zhenyuan sigils (3–4), a block of gold, crane plumes, talismans and **always his
  flywhisk** (no ginseng fruit: the world has only what its trees bore); the youths sigils, gold,
  talismans and their striker or tray. **Zhenyuan's Flywhisk** (拂尘, `FlywhiskItem`): +35% damage;
  used, it sweeps away what stands within 6 blocks before its bearer and brushes arrows and
  fireballs from the air (5 s rest). Disciples leave **Daoist Talismans**, cranes **Cloud Crane's
  Plumes**. A Silk-Lined Tray can also be made: three wool over three gold ingots.
- **XP and karma**: band 78–88, base grunt XP 145, stepped up from the Flaming Mountains' 130 (972 a
  grunt at the band's scale). The household is righteous: **killing a disciple −12** (captain or
  not), a youth −25, Zhenyuan −100; bringing a ripe fruit down is **theft, −40** (as Heaven's
  peaches; no one has leave now) and makes one the temple's thief for 5 minutes (every disciple and
  youth near turn on them at once); a fight with a crane is no deed either way.
- **Art**: `WuzhuangRenderers` (a robed frame for the disciples (topknot and pin) and the youths
  (two buns of boys' hair, smaller); a box model for the crane: long neck, red crown, wings spread
  and beating in flight, folded at rest, swept back in a swoop); skins from
  `assets_source/wuzhuang_folk.py`, `wuzhuang_crane.py`; the fruit, the tray, the striker, the whisk,
  plumes and talismans from `wuzhuang_items.py`. **Zhenyuan** has a frame of his own, model and skin
  from `assets_source/zhenyuan.py` (as the Flaming Mountains' and Lion-Camel Ridge's, with a dim
  glowing skin): a tall, unbent immortal some three blocks to the top of his crown (his hitbox
  0.9 × 2.9), rising and falling a little on a small turning cloud that sheds wisps; layered robes of
  deep celestial indigo over an earth-gold under-robe and apron, a violet cloud collar with gold ruyi
  tips, gold cloud scrolls, a sea-and-cliff hem, a train behind, the bagua on his back; great
  hanging sleeves with gold lips, their mouths dark with stars; a gold lotus crown with a jade pin on
  his white topknot, long white hair down his back, stern long brows, a long white beard; a ribbon
  drifting from his shoulders; a slowly turning ring of gold with the eight trigrams behind his
  head; deep-set eyes with a faint violet gleam; his horsetail whisk held up before him. Sweeping,
  he flings his left sleeve open at his foe and its starry mouth swells.
- **Tests and pictures**: `WuzhuangGameTests` (heights and band, the stair's half steps and slow
  falling, the temple's blueprint and its 24 residents' spots, the fruit all ripe and never
  ripening or growing back, gold and the fruit, sinking, resting on wool, the tray, water, eating
  once, eating turning the household (live, and not before; through death and a relog), theft,
  disciples never spawning of themselves and no lair for Zhenyuan, creatures, captains, XP,
  talismans, swoops, the youths' arts, deaths and drops, the record: the last disciple's killer
  marked, Zhenyuan sent after 24,000 ticks to where they are, not while they are away, once only,
  and kept through saving and loading; his arrival spot, the sleeve held, struggled out of and cast
  out of, his sleeve in a live fight, his cap on blows and his call, his death and his flywhisk, no
  trial left anywhere, the whisk, karma), `WuzhuangZoneTest` (only over the clouds,
  never on another zone's ground at its heights, the island within its region and over the clouds,
  level temple and road, the stair continuous); gallery scenes `-Pgallery.only=wuzhuang` (creatures,
  cranes in flight, the youths, Zhenyuan, the sleeve, Zhenyuan arriving out of cloud in an ordinary
  farmyard, and the island laid north of the stage: gate, court, hall, garden, the fruit, the road, the stair, from below and from afar); the
  mountain in a real world with `./gradlew runWorldgenBench -Pbench.mode=wuzhuang`.

#### Silk-Web Ridge and the Spider Cave (levels 78–88)

Silk-Web Ridge (盘丝岭) and the Spider Cave (盘丝洞, chapters 72–73): the Seven Spider Spirits, seven
sisters who spin silk from their waists and bathe in the Purifying Spring, their adopted insect sons,
and their sworn brother the Hundred-Eyed Demon Lord of the Yellow Flower Temple, in
[`world/spider/`](src/main/java/com/jttw/engine/world/spider/) and
[`entity/spider/`](src/main/java/com/jttw/engine/entity/spider/); everything it registers is in its own
registers ([`SpiderCaveRegistry`](src/main/java/com/jttw/engine/registry/SpiderCaveRegistry.java)).
Mount Wuzhuang's band, on the ground (Wuzhuang is in the sky).

- **Where** (`Zones.SPIDER_CAVE`, after Mount Wuzhuang in `Zones.ALL`): over dark, overgrown country
  (dark forest, forest, birch and old-growth birch, taiga and both old-growth taigas, windswept forest,
  jungle, sparse and bamboo jungle, swamp), host judged at y 256, the whole column. One region a world,
  about 480 blocks across (never under 430), 6–7.5 km out, in Mount Wuzhuang's ring and clear of its
  region and of every zone before it.
- **The valley** (`SilkRidgeShape`, pure, unit-tested; laid by `SilkRidgeFeature`): within 185 blocks
  of the centre the land is brought to the valley's floor, rolling a little, then back to its own over
  30 more. Its ground is dark (podzol and moss in the grass, coarse earth), a dark forest and old
  spruces grow over it (the biome's own trees), and `SilkWebsFeature` strings silk through it:
  cobwebs in the hollows under the crowns, cocoons of woven silk hanging from the boughs, webs on the
  cliffs; here and there a binding web low over the grass or an old skull. Grey-violet fog, drifting
  motes of silk, cave and warped-forest sounds. Paths of worn earth lead from the valley's south edge
  up to the cottage, with branches to the spring and the temple.
- **The ridge**: some 400 blocks from end to end across the valley, east to west, its crest a little
  north of the centre, steep on the south, long on the north, crags of stone, tuff and andesite
  streaked with calcite, four peaks along it (the highest some 90 blocks over the floor), forest on its
  gentler slopes. Under it, always under five blocks of rock at least, the cave.
- **The thatched cottage** (茅屋, `SilkRidgeWorks`): at the ridge's southern foot, under a sheer stone
  face, a cottage of mud brick in a dark timber frame under a deep hip roof of thatch, its yard of
  trodden earth (a woodpile, a trough, a barrel, a withy fence); inside, two looms and an embroidery
  table (the four women at their needlework the Tang Monk saw), a bench, candles, a chest. Its back
  door opens on the passage into the cave.
- **The Spider Cave** (`SpiderCavePlan`, pure, unit-tested): the passage runs north into the rock to
  the **Silk Hall**, a dome 68 by 44 blocks and 26 high: ten pillars of woven silk from floor to vault,
  a great radial web hung across its northern end, cocoons hanging from the vault, egg sacs glowing
  green in it, bones on the floor. From the hall a chain of tunnels (binding webs strung low in them)
  leads through **the seven bowers** (七间绣房), west, north and round to the east, the youngest
  sister's first, the eldest's last: each a dome some 26 by 22 blocks, a dais of dark planks under a
  carpet of its sister's colour, a loom, silk drapes from its vault, a soul lantern on a chain; the
  last the sisters' **treasury** chest (`chests/spider_cave_treasury`). **The rule**: the tunnel on from
  each bower is shut by a wall of **sealing silk** (`silk_seal`: no hand, blast or art breaks it, no
  phasing passes it, see-through between its threads) that only that bower's sister's death loosens.
- **Who keeps it** (put at their posts when the land is made, kept, held near their posts; one found
  far off its post with no one to fight is put back; each back 20 minutes after it falls, no one near,
  `ZonePosts`: a sister come back does not shut her seal again, nor raise the fallen canopy): the hall's ten (spiders, mantises, a cockroach,
  stinging sons; two captains), in each bower its sister and two to five of her sons (captains in the
  later bowers); outside, a band in the cottage's yard, one by the spring, one at the temple's gate;
  and the ridge's own creatures come in any light (spider demons in threes and fours, stinging sons in
  twos and threes, mantises, a cockroach), one in twelve a captain.
- **The Purifying Spring** (濯垢泉, `SpiderCave.bathe`): a steaming pool in a rim of mossy stone, a
  bath pavilion over its north bank, stone lanterns at the steps down into it. Its water washes away
  the ridge's venom, poison, withering and the binding silk (pinned), and mends a little while one
  stays in it.
- **The Yellow Flower Temple** (黄花观): to the south-west, a walled Daoist temple in ochre under dark
  tiles, a gatehouse with a gilded plaque, a court of yellow flowers (dandelions, sunflowers) with a
  smoking censer and stone lanterns, side halls of cells, and the double-eaved main hall with gilded
  finials, its altar to the Three Pure Ones and the tea table. **The canopy**: while any of the seven
  sisters lives, their canopy of silk (sealing silk, an ellipsoid 72 by 88 blocks and 27 high) is over
  the whole temple, as they threw it over it in chapter 73, and the Demon Lord is shielded (no blow
  reaches him, against digging under it). When the last sister falls, everyone in the region is told,
  and the canopy sags and falls away (`SpiderCave.Record`, saved data keyed by the region: which
  sisters have fallen and whether the canopy is all down; any of it in an unloaded chunk falls when
  someone comes near).
- **The venom** (`venom`, `Venom`): the spiders' bite, the stinging sons' stings, the Demon Lord's tea:
  1% of its bearer's greatest health a second a level, through any armour (vanilla's Poison is nothing
  at these levels), never the last of it. Milk and the spring cure it; the ridge's own are proof.
- **Binding webs** (`binding_web`, `BindingWebBlock`): whatever walks into one (but the ridge's own) is
  **pinned** (`Stun.pin`: held, though it may strike what is in reach) for 2 s, then has 1.5 s to tear
  free before it holds again; it slows as a cobweb does; a sword or shears cut it quickly. The webs a
  sister casts in a fight are fresh: they fall apart by themselves after 8 s.
- **Creatures** (`SilkMonster`: level 78 at the ridge's edge to 84 at its heart, and on into the cave:
  the Silk Hall's garrison 84, the bowers' sons 85 to 88, the band's top, in the last; captains three to
  five over the regulars round them, never past 89; any light), each "you kill it in
  X ordinary hits / it kills you in Y blows" against a same-level character:
  - **Spider demons** (蜘蛛小妖, light): 3 hits / 16 bites (6.5%, with 3 s of venom); it leaps, and from
    4–14 blocks it **rears up** (0.6 s) and spits a strand of silk (`SilkStrandEntity`: 5%, pinned
    1.5 s); a captain spits two. Strike one and those near turn on you.
  - **Stinging sons** (蜜蜂, 马蜂, 牛蜢: the Honeybee, Hornet and Gadfly Sons, light, flying): 3 hits / 15
    stings (7%, venom 3 s); now and then it **hangs buzzing** (0.7 s), then darts straight at you: 9%
    and 5 s of venom.
  - **Mantis Sons** (螳螂精, normal; heavy-bodied: a plated thorax, a ringed abdomen under broad leaf-wings,
    a triangular head of two great eyes, massive spined scythes, stout spined legs): 6 hits / 12 cuts (9%); close, it **raises both scythes** (0.7 s)
    and sweeps them through all before it, 3.4 blocks and 60° either side: 10%. Step aside or back.
  - **Cockroach Sons** (蜚蠊精, heavy, armoured): 11 hits / 9 blows (11.5%); 3–12 blocks off it **scrapes
    the ground** (0.8 s), then charges straight where you were: 12% and thrown back. It cannot turn.
  - **Captains**: purple, larger, twice the hits (6, 6, 12, 22) and blows 1.2 times as hard
    (13, 12, 10 and 8 blows kill you); they bring two or three of their kind; besides the sigil every
    captain leaves, a skein of spider silk and now and then a Golden Eye.
- **The Seven Spider Spirits** (七个蜘蛛精, `SpiderSpiritEntity`, one frame, seven skins: the eldest in
  crimson and gold, the second rose, the third peach, the fourth gold, the fifth jade, the sixth azure,
  the seventh lilac): always minibosses, the cave's elite, one keeping each bower, the youngest (level 86) in the first to
  the eldest (89, one over the band) in the last; 28 hits at a captain's strength, 40 in her bower (1.6 times a captain's,
  kept to an elite's 40); her blow (a lash of her sash) a miniboss's 16% (7 kill you). **Silk from her waist**: every 6 s she
  draws herself up, silk gathering glowing at her waist (1 s), her spider's legs spreading; then it
  pours out in a cone 9 blocks long, 30° either side: 14% (8 kill) and pinned 2.5 s. **Webs underfoot**: every
  9 s she marks up to three foes within 12 blocks (a ring of white motes under each, 1.2 s), then a
  fresh binding web springs up where each stood. At half her health she **calls her sons**: two insect
  demons come (once). Her death loosens her bower's seal, and is remembered.
- **The Hundred-Eyed Demon Lord** (百眼魔君, `HundredEyedDemonEntity`, a `ZoneBoss`, level **91**: his
  band's top and three, the ridge's gym leader; 68 hits of a level-88 character, the one he is built for, a
  yellow bar; fallen, his court is a usurper's 20 minutes after, level 88), a centipede spirit in a Daoist's yellow robe (its skirt long gores of silk
  hanging in deep folds and flaring to the ground, gold kept to the hem, the opening's edges and embroidery), sworn brother of the seven, who poisoned
  the Tang Monk's tea. His blow, a cut of his Daoist sword: 19% (six kill). Every art is shown first:
  - **the golden light of his thousand eyes** (金光): every 16 s (11 s once his robe is off) he flings
    his arms up, his eyes blazing brighter for 1.5 s (gold motes gathering, a rising chime); then the
    light bursts over all within 14 blocks: whoever **faces him** takes 16% (seven kill), is blinded for 5 s and
    held 1.5 s; whoever has **turned away** takes only 5%;
  - **poisoned tea** (毒茶): every 12 s (7.5 s from a third) he flicks his sleeve: rings of yellow-green
    motes mark where each foe stands and a spot or two about them (1.5 s), then the tea bursts 3 blocks
    round each: 10% (ten kill) and 6 s of venom;
  - **the centipede's sweep**: when two foes are close, or one within 4 blocks, he rears on his coils,
    scratching the ground in a ring (1 s), then sweeps his long body round: 14% (eight kill) to all within 7.5
    blocks, hurled off;
  - at two thirds he **throws off his robe**: his eyes bared for good (they flare now and then), the
    light faster, and three stinging sons come at his call; at a third the tea comes faster and in more
    cups. No art of his hits harder than his blow; one of the band's top can beat him alone.
- **What they leave**: the Demon Lord three to five **Golden Eyes**, always his **Golden-Light Eye**
  (金光眼, `GoldenLightEyeItem`: raised, it flashes his light, every hostile creature within 8 blocks
  blinded and held 2 s; then 20 s to dim), a block of gold, spider silk, sigils; a sister spider silk,
  gold, a Golden Eye one time in four; spider demons **Spider Silk**, string, spider eyes; the sons
  **Insect Carapaces**. Four spider silk make a block of **woven silk**; one makes three string.
- **XP and karma**: band 78–88, base grunt XP 145, as Mount Wuzhuang's (972 a grunt). Subduing the
  Demon Lord is a good deed (**+150**, as Red Boy); the sisters are captains (+5), the rest monsters.
- **Where it is**: the Earth Gods know its master and point to the cottage over the cave's mouth.
  Asked by level, the band 78–88 is sent to the nearer of Silk-Web Ridge and Mount Wuzhuang (by where
  the needle would point: here the cottage) and told the other's way and distance too; in its own
  country (on the ground under Mount Wuzhuang's footprint, the heights decide), always here.
- **Art**: `SpiderCaveRenderers` (its own client registration; skins with a full-bright glowing skin
  over them, the Demon Lord's ablaze in his glare): models and skins from `assets_source/silk_kin.py`
  (the spider demon, the stinging sons' three skins, the Mantis Son, the Cockroach Son),
  `spider_spirits.py` (the seven sisters), `hundred_eyed.py` (the Demon Lord), on the shared
  `silk_court.py`; blocks and items from `spider_items.py`.
- **Tests and pictures**: `SpiderCaveGameTests` (the band, one region in its ring and clear of the
  others, the pay, the needle; the creatures and captains, the strand, the venom and the spring, the
  binding web's hold and grace and the fresh web falling apart, the sweep and the ram, the dart; the
  sisters always minibosses, their silk, webs and sons, a sister's death loosening her seal and being
  remembered, the Demon Lord shielded till all seven have fallen; his level and pecking order, the
  golden light on those facing and those turned away, the tea, the sweep, the robe and the brood; the
  keepers put at their posts as the land is made; loot; karma), `SpiderCavePlacementTest` (hosts,
  band and ring, never on another zone's ground, rock over the cave, the works level, a ridge that
  towers, every seal shutting all beyond it on foot, the hall and the bowers great rooms, the canopy
  closing over the temple), `PeckingOrderGameTests` (the sisters among the minibosses); gallery
  scenes `-Pgallery.only=spider_` (every creature from the front, side and back, close, and in each
  telegraphed art; a piece of the valley laid off the stage: the cottage, the passage, the hall and
  its web, a bower, a seal, the spring and its pavilion, the temple under its canopy and without it);
  the region in a real world with `./gradlew runWorldgenBench -Pbench.mode=spider`. (Its audit found
  the works hollowing three blocks of the ground out under the temple's and the cottage's floors, so
  the temple's paving was a crust over a void and its court could not be walked to; the works now
  leave the ground under their floors to the land, only the spring's pool going deeper. And the
  clearing of the paths and the works' grounds cut the forest's dark oaks to the column, leaving
  half-trunks and boughs in the air, and spared any tree standing in the yard: it now fells a tree
  whole, the works' own timber told from the forest's by what the works put there. The ways from the
  cottage to the Demon Lord and to every sister walk.)

#### Lion-Camel Ridge (levels 89–100)

Lion-Camel Ridge (狮驼岭, chapters 74–77: eight hundred li of mountains swarming with demons, forty-seven
thousand little demons on the ridge alone) and Lion-Camel City (狮驼国), the walled kingdom the demons
took after eating its king, his court and all its people; and behind the city the Lion-Camel Cave
(狮驼洞), the demon kings' fortress sunk deep in the mountain, where the Azure Lion and the White
Elephant hold court at the bottom. The last band, to the levels' cap. In [`world/lion/`](src/main/java/com/jttw/engine/world/lion/)
and [`entity/lion/`](src/main/java/com/jttw/engine/entity/lion/).

- **Where** (`Zones.LION_CAMEL`): over mountain country (the Bottomless Cave's hosts: peaks, slopes,
  groves, meadows, cherry groves, windswept hills) and the rugged savanna highlands (plateaus,
  windswept savanna), host judged at y 256, **the whole column** (`Vertical.WHOLE_COLUMN`): the
  fortress goes some 130 blocks down under the city, so the ridge no longer lies over the Bottomless
  Cave's labyrinth but keeps clear of it, as of every zone before it (its ground is still reshaped
  only from y 40 up). One region a world, **about 680 blocks across** (radius 340, never under 600:
  `ZoneLayout.LION_CAMEL`), the farthest out: 7.5–9 km from spawn, the end of the road west.
- **The ridge** (`RidgeShape`, `LionCamelRidgeFeature`, pure and unit-tested): the mountains are broken
  into grim crags (ridged noise, up to 30 blocks more), climbing out of the land over the outer fifth;
  faces bare stone banded with tuff, andesite and deepslate; ground of coarse dirt, gravel, podzol and
  stone, a little yellowed grass, snow over y 196; skulls on stakes and bones about, great ribcages
  (`RidgeBonesFeature`, never on the city's ground) and dead shrubs. A grey sky and haze, a little ash
  in the air. Nothing under y 40 is touched but the cave.
- **Lion-Camel City** (`LionCamelCity`, a blueprint each chunk lays its share of): on a level apron cut
  into (or built up on) the mountain round the centre, the ground climbing back to the ridge's over
  40 blocks. Walls of grim deepslate brick 11 high and three thick, 113 blocks square, crenellated,
  skulls on the battlements, breached here and there; a tower at each corner with a smoking beacon; a
  gate in each side, the main one to the south under a gatehouse with its gilded plaque (狮驼国).
  Two paved avenues cross at the palace, a road runs round inside the walls, alleys between plots of
  ruined houses (broken walls, fallen roofs, cobwebs), the demons' kitchens (a cauldron on a fire,
  the steamer meant for the Tang Monk stacked by it, a pen of iron bars), rubble and yards heaped
  with bones; skulls and bones in the streets. At the heart the palace in its red-walled compound:
  a terrace and on it the throne hall (red pillars, lattice walls, soul lanterns, a gold mural, its
  tiled roof fallen in at one corner, ten blocks clear within) with the **three demon kings' thrones,
  each a giant's (a seat three wide under a gilded back six high, a skull atop it), the third, the
  Great Peng's, empty**; behind it the walled **elephant court** (31 by 22 within), ringed with tusks
  of ivory. The city is built for its giants: the palace's gates and the court's are 7 wide and 6
  high, and a demon king walks from the hall to the court (round the terrace, through the palace's
  north gate and into his court); the court has room for the elephant he truly is. Its court and
  garrison are put there once, each by the chunk its post lies in (`LionCamelCityFeature`), and kept
  (even on Peaceful): lion-demon captains on the terrace where the Lion held court and at the palace
  gate, an elephant-demon captain in the Elephant's court, lion- and peng-demons in the hall,
  elephant-demons at the main gate and in the court, a Little Wind-Driller at each gate. The kings
  themselves have gone down into the mountain: the thrones in the hall stand empty, and the court's
  north gate and the city's lead on to the forecourt of the cave.
- **The ways up** (`RidgeApproach`, pure and unit-tested): three winding roads, south, east and west,
  from the region's edge to the city's gates, cut through the crags and built up over the hollows,
  paved down the middle, skulls on stakes along them; **nine outposts** beside them (a watch tower of
  dark brick 14 high with a beacon of soul fire on its railed platform, a ladder within, a ring of
  stakes open to the road, a cooking fire), each kept by a lion-demon captain, a lion-demon, an
  elephant-demon, a Little Wind-Driller and a peng-demon up on the tower; and **twelve patrols** of
  three out on the roads (a captain with the farthest). Some 100 demons are kept out on the ridge and
  in the city; the ridge's own spawning comes in bigger packs (lion-demons 3–6, peng-demons 2–4,
  elephant-demons 2–3, Wind-Drillers 1–2). Demons put at a post keep to it (they wander 12–20 blocks
  and are back at it if drawn 40–72 away).
- **The Lion-Camel Cave** (`LionFortress`, a blueprint each chunk lays its share of, pure and
  unit-tested; `LionGates`): north of the city a **mountain** rises (`RidgeShape.massifAt`, some 48
  blocks over the city, a cliff across its front), and in its cliff, past a forecourt of black stone
  lit by soul braziers between skulls on stakes, stands the **great door**: two leaves of dark wood
  15 wide and 17 high, banded with iron and studded with gold, knockers on them, between pillars of
  black stone and red under a lintel with the cave's name in gold (狮驼洞), crouching stone lions at
  its sides, a tower with a beacon at each end of the wall. **It holds while its six wardens stand**
  (two elephant-demon captains, two lion-demons, two peng-demons in the forecourt): try it and it
  will not move; once they have fallen it **opens to a hand** (use it), its leaves grinding back from
  the middle. The Great Peng's kin open it to him at once. Behind it the **gate hall** (63 blocks
  square, 20 high, two rows of red pillars, braziers), and under it **eight floors**, each 13 blocks
  under the last, each a hall 63 blocks square and 10 high in its own stone and look:
  1. **the Barracks**: dark timber, rows of straw pallets under red cloth, mess tables, the armoury;
  2. **the Drill Yard**: an open yard of sand with straw men on posts, a colonnade, the reviewing stands;
  3. **the Steaming Kitchens**: great cauldrons on fires over magma hearths, the steamers stacked,
     the pens where they kept the pilgrims they meant to eat;
  4. **the Bone Pits**: deepslate and bone, pits heaped with bones under cobwebs;
  5. **the Menagerie**: mossy pens of iron bars, hay and troughs;
  6. **the Vault**: black stone and gold, the hoard on plinths, pools of lava;
  7. **the Great Peng's Halls**: terracotta and gold, perches and fallen golden feathers;
  8. **the Generals' Hall**: nether brick and red pillars, moats of lava, the generals' standards.

  Side halls open off some through wide doorways under lintels. Each floor's **way down is a stair
  against a wall behind a portcullis**, coming down on the next floor at the other side (so every
  floor is crossed); the portcullis **rises of itself when the floor's warden falls** (a captain's
  level, half again a captain's health and a miniboss's blows, before it). The generals' hall's way down is a grand
  stair, balustraded, into the **throne room** at the bottom, some 127 blocks under the city: 63 by
  67 blocks and 20 high, great pillars banded with gold, braziers down the nave, bones heaped in the
  corners, a red carpet to a dais with the three thrones (a gold lion's face over the Lion's; the
  Great Peng's empty, his treasury beside it), the Azure Lion and the White Elephant before theirs.
  Its walls, floors, stairs and gates are proof against picks and blasts (the furnishings are not).
- **Who keeps the cave**: each floor's keepers are put there once (`LionCamelCityFeature`), kept and
  never despawned: 5 in the gate hall, then 7, 8, … 14 on the eighth floor, more captains the deeper
  (1 to 3), stronger the deeper (the ridge's road going on down from the city's 92: regulars level 92
  in the gate hall, one more a floor, to the band's top, 100, on the eighth floor, captains four over
  (96–101), wardens five over (97–101)), and the warden before the gate. Each keeps to its floor: it is
  back at its post if it strays 40 blocks or off its floor's heights, so none follows a foe down the
  stairs, and none comes into the throne room. **Respawn**: a fallen keeper or warden comes back 20
  minutes after, no one near (`ZonePosts`; the city's and the ways up's too), but a fallen warden's
  gate stays open; and the ridge's demons rise of themselves in the halls as anywhere in the zone (any light, as their mob cap allows) and keep to the
  hall they rose in; none rises in the throne room. **Checkpoints**: as a floor's way down opens,
  every player in its hall will rise again, should he fall, on its landing at the head of the stair
  down (whatever bed he had); and the gates once opened never close, so he walks back down through
  the floors he won. The Earth God's Luopan points to the great door.
- **The kings in their throne room**: they are put there once, as the land is made, their lairs
  before their thrones (a seat each: a fallen king's place is a usurper's 20 minutes after, level 100,
  never of the kind still sat there), and keep to them: drawn more than 16 blocks past their lairs (up the grand
  stair after a foe) they let go whatever they hold and are back before their thrones. Their fight is
  as before, phases, bars, true forms and all; the room is high and wide enough for the true forms.
- **Old worlds**: ground already made keeps what it had (no migration); new ground gets the new
  design. A king of a world made before (in the city) stays there until his region's throne room has
  been made and is loaded, then goes down to it, his lair with him; if a king of his kind was put
  there already, the city's is gone (`DemonKingEntity.goDownToHisThrone`). A cave laid half on ground
  made before is broken where the old ground is (it can be dug through: only the blueprint's own
  blocks are proof against picks), never sealed. A world whose city was made before keeps its old
  treasury chest on the hall's dais, and the vase is that one's: the throne room's chest then holds
  the rest of the hoard but no second vase (`LionTreasury`: the vase's entry is under the
  `jttw_engine:one_vase` condition, which looks for the old chest whenever a chest in the cave is
  first opened).
- **Creatures** (`LionCamelMonster`: level 89 at the ridge's edge to 92 in the city at its heart, the
  city's garrison and the ways up of their place's level, captains three to five over the regulars
  round them; any light, in numbers): **Lion-Demons**
  (狮子精, the Azure Lion's kin: 10 hits, sabre cuts of 11%; they pounce from 4–11 blocks for 12% and
  a stagger; strike one and the pride turns), **Elephant-Demons** (象精, the White Elephant's
  followers: 12 hits, armoured, blows of 11%; they charge, tusks down, goring for 12% and flinging
  aside), **Peng-Demons** (鹏精, the Great Peng's followers: 6 hits, spear thrusts of 10%; they beat
  up into the air and swoop for 11%, glide rather than fall). Captains as everywhere (8%, purple,
  twice the health, with escorts: three, two, four).
- **The Little Wind-Driller** (小钻风, `LittleWindDrillerEntity`): the ridge's patrol demon in his
  pointed cap with the patrol token at his sash (the one Sun Wukong took to pass for him) and a
  clapper-bell. No fighter (3 hits, a blow of 6%, never a captain); but let him see a foe within 16 blocks, or be
  struck, and he **raises the alarm**: he rings his bell, every demon of the ridge within 32 blocks is
  set on the foe, three more come running (while fewer than 16 are about), and he runs off; then 30 s
  before he can raise it again. A Wind-Driller is posted at each of the city's gates.
- **The two demon kings are giants** (`DemonKingEntity`): two and a half times a man's height (the
  Lion 2.0 by 4.5 blocks, the Elephant 2.2 by 4.6), and at a third of their health each **reveals
  his true form**, the beast a Bodhisattva rides: for 2 s he stands roaring as his demon shape
  falls away (smoke, fire, the ground shaking; no blow lands on him nor does he strike; a captive is
  let go), then the beast stands in his place, in another model and a larger body, and the players
  near are told what he is. The true form is synced and saved; the fight goes on in it.
- **The Azure Lion** (青狮, Manjushri's lion; `AzureLionEntity`, a `ZoneBoss`, level 103, the ridge's
  gym leader, built for a capped character: 78 hits of a level-100 character,
  an azure bar, in the throne room): a hulking demon king in dark lacquered armour, a wild green-blue
  mane bristling about a snarling maw, clawed hands, a giant's broadsword: six blows kill (19%). Every 9 s he throws back his head
  and **roars**: a cone 12 blocks long, 8% (thirteen kill), Slowness II and Weakness, thrown back. Every 13 s, a foe
  within 4.5 blocks before him, his jaws gape for a second, and he **swallows the foe whole**: inside
  him the foe is unseen and in the dark and cannot get down; his belly takes 3% of a level-100
  character's health a second, armour or none. Blows struck from inside count half again, and once
  they come to 5.4 ordinary hits of a level-100 character (some 7% of his health) he retches the foe up and reels for 2 s; else after 10 s he spits him
  out anyway. At two thirds he roars for his kin (three lion-demons); at a third he **reveals his
  true form**: a giant guardian lion of dark azure on all fours (3.6 by 4.0 blocks, some seven long,
  larger than the Great White Bull), lean and long-legged, his face ringed in spiral curls and his
  mane swept back in locks like flames, fanged, a gold collar of bells at his throat, who swallows
  twice as often and from 6 blocks, and **pounces** on a foe 5–13 blocks off, jaws first: if they
  reach the foe as he lands, it is swallowed (inside the lion as inside the demon king, and fought
  out of him the same way).
- **The White Elephant** (白象, Samantabhadra's six-tusked elephant; `WhiteElephantEntity`, level 102,
  a lesser boss one under the Azure Lion, built for a capped character: 74 hits of a level-100 character,
  a white bar, in the throne room): a giant in ivory-and-gold plate, a great domed
  elephant's head under a gold war-frontlet, ears like sails, a heavy trunk, six tusks, a giant's
  lance held upright: six thrusts kill (19%). Every 10 s, a foe within 6
  blocks before him, his trunk rears and **coils round the foe, lifting him high**: it crushes him
  every 0.75 s for 5% and after 3 s **hurls him away**; blows on the trunk from where one is held,
  once they come to 3.4 ordinary hits of a level-100 character (some 5% of his health), make him drop the foe and recoil (as Wukong got free by driving
  his staff up it). From two thirds he trumpets for his followers (two elephant-demons) and **stamps**
  (10%, ten kill; thrown up, within 5 blocks); at a third he **reveals his true form**: a towering white war
  elephant (3.6 by 4.6 blocks, some nine long with his tusks) in a crimson and gold saddle-cloth,
  the Bodhisattva's empty seat on his back. He **charges** at foes 5–14 blocks off, goring for 14% (eight kill) and
  trampling (Slowness III); his stamping becomes a **trampling** up on his hind legs that strikes
  within 7 blocks; his trunk reaches 8 blocks, comes quicker and lifts its catch high over his head.
  The two kings stand 14 blocks apart before their thrones, so the two fights are as one.
- **The Peng rule** (`ZoneAllies.PENG`, for the whole zone, both kings included): a player who is the
  Gold-Winged Peng, the third brother, is **never targeted and never fought back**, whatever he does:
  no demon, captain, Wind-Driller, the Azure Lion nor the White Elephant sets on him, nor swallows,
  grabs, roars at, stamps on or gores him, nor rings the alarm against him; his own area attacks pass
  them over. What he strikes down of theirs, unresisting, leaves him **nothing** (no loot, no XP, no
  karma: this is the framework's now, for any zone's allies), so the kings' rewards are not his to
  earn there. The little demons bow when he comes near and hail their Third Great King (once a
  minute); the Azure Lion and the White Elephant call him their third brother; his throne stands
  empty beside theirs.
- **What they leave**: the Azure Lion **2–3 locks of his azure mane**, his **broadsword** one time in
  four (+60% damage, slow; its swing cleaves what stands beyond its mark for 45%), the demons' hoard
  (2–3 gold blocks, 3–5 diamonds, 4–8 emeralds, 1–2 netherite scrap), 1–2 True Samadhi Fire, mane
  tufts, sigils; the White Elephant **2–3 white tusks**, his **lance** one time in four (+50% damage,
  1.5 blocks more reach, throws back), the hoard, ivory, sigils. Lion-demons leave **Mane Tufts**,
  elephant-demons **Ivory Shards**, peng-demons **Storm Feathers**, the Wind-Driller his **Patrol
  Token** half the time. Two mane tufts, an ivory shard, a storm feather and a captain's sigil make
  a True Samadhi Fire (`true_samadhi_fire_from_the_ridge`).
- **XP and karma**: band 89–100, the cap, base grunt XP 160 (1,248 a grunt at the band's scale, up
  from the Flaming Mountains' 130 and Mount Wuzhuang's 145). Striking down the Azure Lion or the White
  Elephant, who ate a kingdom, is a good deed: **karma +150** each, as Lady White Bone; captains +5,
  the rest +1. With this band and Mount Wuzhuang's (78–88) every level from 21 to 100 has one; the
  kill XP's flat fallback for a tiered enemy outside every band stays, for any mob tagged by hand
  (Heaven's host pays by its own level instead: see "Heaven", below).
- **Art**: `LionDemonRenderer`, `ElephantDemonRenderer`, `PengDemonRenderer`,
  `LittleWindDrillerRenderer`, `AzureLionRenderer`, `WhiteElephantRenderer` (box models and skins with
  glowing eyes and gold from `assets_source/lion_camel.py`; the little demons bow; each king has two
  models, his demon-king shape and his true form, drawn by the one renderer, their eyes smouldering
  (amber, red) in an angled glare under a scowling brow; the Lion's jaw gapes and he pounces, the
  Elephant's trunk sways, rears and coils, and he rears to trample); items from
  `assets_source/lion_items.py`.
- **Tests and pictures**: `LionCamelGameTests` (the Peng rule for every kind, both kings included,
  in either shape, struck or not; swallowing, the belly, fighting out, being spat out; the trunk's
  lift, crush, hurl and breaking free; roar, stamp, charge, phases; the true forms' reveal at a third,
  their bodies, their keeping; the lion's swallowing and pounce, the elephant's lift, trampling and
  goring; the alarm; the creatures' tricks; captains; drops and the Peng's empty hands; the band's XP
  to the cap; karma; the city's blueprint, built for its giants), `LionFortressGameTests` (the way on
  foot from the door to the throne room, every floor's stair; who keeps each floor; the great door
  holding while its wardens stand, opening to a hand, at once to the Peng; a portcullis rising as
  its warden falls; the fabric against picks; the checkpoints; the kings kept to the throne room,
  an old world's king going down to it; the Lion's fight and true form in the throne room; the
  needle to the door), `LionCamelPlacementTest`, `LionFortressTest` (eight floors, their stairs, the
  walk from the door to the kings with the gates open and barred with them shut, the keepers, the
  treasury, where it lies, the roads and outposts); gallery scenes `-Pgallery.only=lion_` (the kings
  from the front, the side, close and beside the player, in both shapes; the reveal; the swallow, the
  pounce, the trunk and the trampling; the city; the throne room with the kings, in both shapes, and
  the generals' hall); the ridge, the city, the door, every floor and the throne room in a real world
  with `./gradlew runWorldgenBench -Pbench.mode=lion` (and every zone's place with `zones`).

#### Fengdu, the Ghost City (the way to Hell)

Fengdu (丰都鬼城), on Mount Ming over the Yangtze, is in folk belief the city of the underworld's
officials, through which every soul passes on its way down to Diyu. Here it is how anyone, of any
kind and any level, gets to Hell (levels 57–66), the long way: its Gate of Ghosts is sealed to the
living who walk there, and opens only for those who come to it through the deep dark, by the
frame at the heart of an Ancient City lit with soul fire, and break the guard of Hell's soldiers
that rises to bar it. Before it, only Sun Wukong (sent there at level 30) and the Dragon's and
Wukong's dimension travel could. In
[`world/fengdu/`](src/main/java/com/jttw/engine/world/fengdu/).

- **Where** (`Zones.FENGDU = Zone.place(fengdu, fengdu_hosts, ZoneLayout.FENGDU, WHOLE_COLUMN)`):
  a place, not a zone of a band: no level band of its own (none is registered, the Earth Gods never
  send anyone there by level but Hell's band, it pays no XP). One a world, placed by `ZoneMap` as
  the zones are, last of them all: in the farthest ring, **9–10.5 km out**, past Lion-Camel Ridge
  (though Hell's levels are lower: the Ghost City lies at the end of the world), a region about 240
  blocks across on river and hill country (plains, forests, birch, dark and flower forests, taiga,
  swamps, rivers, windswept hills and forests; host judged at y 256), the whole column, so it never
  shares ground with any zone. In the bench's worlds: seed 12345 at (−428, −10431), 10.4 km north,
  on birch forest; seed 42 at (−8261, −5818), 10.1 km north-west, on old-growth birch forest.
- **The country** (`fengdu` biome; `world/feature/FengduFeature`, laid chunk by chunk): grey-violet
  sky, dark fog, murky water, ash drifting in the air, the soul sand valley's moods; no creature
  spawns in it, by day or night, so it is safe to reach the gate. The land keeps its shape round the
  hill, gone gloomy: soft patches of podzol and coarse earth in the grass, dead bushes, dead trees
  and dark spruces, old graves with mossy headstones.
- **The hill** (`FengduShape`, pure, unit-tested): a level top (a rounded square 49 blocks out each
  way) at the land's height at its centre or round its foot, whichever is higher, and 14 more;
  slopes back to the land over about 44 blocks, their foot wandering, dark spruces on them (as
  cypresses stand round a temple hill), bare rock where steep; where it meets water it rises out
  of it. Up its south slope runs the **Road to the Yellow Springs** (黄泉路), paved, a soul lantern on
  a post every sixth block, by turns either side.
- **The city** (`FengduCity`, a blueprint like Lion-Camel City's, laid column by column with its
  signs): a square wall of dark deepslate brick (8 high, 36 out each way), crenellated, soul lanterns
  on its merlons, a tower at each corner; one gate, south, under a gatehouse with a gate tower on it
  (red pillars, a lattice frieze, red lanterns, a hip roof of dark tiles) and the city's name over
  the way in (丰都); before it on the hill's top a three-bay stone **archway** (牌坊) over the road,
  丰都鬼城 on its face and 黄泉路 on its back. Within, the **lower city**: an avenue north from the gate
  lined with soul lanterns, a road round inside the walls, and plots of seven by seven: **shrines**
  to the officials of the dead (red walls, an open front, a dark tiled roof, a skull-faced image on
  the altar, candles), the **houses of the dead** (dark and shut, lattice windows, red lanterns by the
  door, a coffin within), yards where **paper offerings** burn in a walled fire, and **graveyards**.
  Up three flights of steps the **upper city**, a terrace across the north half: on it the **Gate of
  Ghosts** (鬼门关), a great arch of black stone and red with a gold-ended roof, soul lanterns at its
  eaves, a soul-fire brazier and a screen wall either side; behind it the **Palace of the Son of
  Heaven** (天子殿), King Yama's, on its plinth (red walls and pillars, a lattice front, a hip roof
  of dark tiles, his throne on a dais before a gilded screen, his image over it, candles, a red
  carpet to it), an incense burner before its steps; to its west the **Temple of the City God**
  (城隍庙), to its east the **Hall of the Judges** (判官殿), each with its god's image and candles.
  No one lives here, and nothing fights here but the guard that rises before the Gate of Ghosts.
  (Hell's own Naihe bridge and Meng Po are left to Hell.)
- **The ghost veil** (`GhostGateBlock`): a cold soul-fire mist (an animated, half-see-through teal
  block that sheds souls and soul flames, glows, has no collision, cannot be broken or picked, and
  no water washes out). Any player who stands in one for a second (20 ticks; stepping out sooner,
  nothing) is taken where it leads (its `way`): Fengdu's gate down to Hell, an Ancient City's frame
  to Fengdu, Fengdu's side doors back to the Ancient City; in Hell every veil leads up to Fengdu.
  One arrives out of the veils on every side, so no one is sent straight back.
- **The way in: an Ancient City's frame** (`AncientFrame`, `AncientGate`): at the heart of every
  Ancient City stands the great frame of reinforced deepslate (a ring 22 by 8 round a way 20 by 6,
  one block thick, running along x or z with its piece: the same in all three of vanilla's
  `ancient_city/city_center` pieces, read and checked by `AncientFrameTest`). Soul fire appearing in
  its way, by any means (soul sand or soil set in the frame and lit with flint and steel, a fire
  charge, a dispenser; caught by `BlockEvent.NeighborNotifyEvent`, so nothing is laid as the land is
  made, and old worlds' cities work as new ones'), runs round the frame: if its ring is whole (its
  corners not asked for) and its way open (air, water, the fire and its fuel, sculk veins), the way
  fills with the veil, with a shriek and a burst of soul fire. Stepping into it takes anyone, of any
  level, to Fengdu's terrace before its Gate of Ghosts (the frame, and which side of it they stood,
  remembered for them, through death too).
- **The Gate of Ghosts, sealed** (`FengduGate`, its rules in `GateRules`, pure, unit-tested): its way
  is barred with iron, and the veil burns in it only for those who fought for it. Each arrival by an
  Ancient City's frame raises a **swarm** of Hell's soldiers out of the terrace, every time: three
  waves, each rising when all but one of the last have fallen (five **Yin soldiers**; six more with
  their captain; then the gate's own guards, **Ox-Head and Horse-Face**, with three more), two more
  soldiers a wave (one in the last) for each player within 64 blocks of the gate past the first,
  sixteen in all for one; at the level of the highest of those there, but never under 50 nor over
  56 (no level is asked of anyone, yet they are strong, and still short of Hell's own 57-66). They are kept to the terrace (one that strays 22 blocks or falls off it is brought
  back), never despawn by themselves, and do not care that nothing spawns in Fengdu's country (they
  are raised, not spawned). A pilgrim who arrives while a swarm stands raises another vanguard at
  once. The swarm rises, and is beaten, only while someone is at the gate; with no one within 64
  blocks for 30 seconds it sinks back into the ground and is forgotten (any of it left in an unloaded
  chunk is turned away when the chunk loads). When the last soldier falls the gate **ignites by
  itself** (the bars gone, the veil roaring up with soul fire), and every pilgrim on the terrace who
  came by the frame may pass down to Hell's arrival plaza (`JTTWDimensions.teleportTo`, as every
  traveller). No one else may, though the veil burns before them: not one who walked to Fengdu
  over the land, nor one who watched. Leave is lost on leaving Fengdu (64 blocks from the gate, or
  another world, or death), so every trip down from an Ancient City is fought again. **It seals
  again** once no one who may pass has been within 64 blocks for ten seconds (they have gone down,
  or away), and at once when a new swarm rises.
- **Two ways, as a nether portal's**: the Gate of Ghosts on Hell's arrival plaza has a **side door**
  either side of its arch (the arch's own way stays the road's), each filled with the veil: standing
  in one takes anyone back **up** to Fengdu, onto its terrace just before its Gate of Ghosts, facing
  away from it (if the city is not there, onto the ground there), and the gate burns for them, so
  they may step straight back down. Fengdu's gate has a **side door** of its own in each of its
  screen walls (古城, back to the Ancient City): standing in one takes anyone back to the Ancient City
  they came by, before its frame on the side they went in by; one who never came by a city is taken
  to the lit frame nearest Fengdu, and if none has ever been lit, nowhere. Sun Wukong's level-30
  sending and the Dragon's and Wukong's dimension travel are unchanged.
- **Old worlds**: a Fengdu laid before the gate was sealed (its way open with the veil, no side
  doors) has its gate laid again from the blueprint, sealed, with its side doors and signs, as soon
  as anyone comes near it (as an old Hell's side doors are set up, `HellWorks.ensureReturnDoors`).
- **Yin soldiers** (阴兵, `entity/hell/YinSoldierEntity`, Hell's, level 57 by itself): the dead who
  served as soldiers, raised under Hell's banner: 2.5 hits, a blow of 6%, quick, a long reach of
  sight; in dark lamellar armour with a bronze mirror plate, a helmet with a neck guard and a red
  crest, a halberd, grey faces and soul-fire eyes; captains in violet and gold. Met only before
  Fengdu's gate. Hell's own: killing one is no deed either way; undead (+2 Heavenly). Hell Bank
  Notes and bones. Art: `HellRenderers.soldierLayer` (the guards' frame at a person's size), skins
  from `assets_source/yin_soldier.py`.
- **The Earth God's Luopan**: Hell's band is pointed to the nearest Ancient City and told the way in
  (see the luopan, above); in Hell, sneaking, to the side doors.
- **Art**: the ghost veil's 32-frame animated texture from `assets_source/ghost_gate.py`; everything
  else vanilla blocks.
- **Tests and pictures**: `FengduPlacementTest` (one a world, in its ring past Lion-Camel Ridge and
  farther than every zone, on its hosts, never on a zone's ground, the same for the same seed; a
  place of no band; the hill, the city and the road inside the region; the hill's shape and floor);
  `AncientFrameTest` (the frame found from every block of its way either way it runs, in each of
  vanilla's city centres as the game's own structure files lay it; not where its ring is broken,
  its way blocked or the shape another's); `GateRulesTest` (who may pass, when it burns and seals,
  the swarm's waves and size, the soldiers' level); `FengduGameTests` (anyone the burning gate lets
  pass, of any kind and level, taken down to Hell's plaza, and no one else, stepping out in time
  sparing them; the side doors in Hell, the road's way open, and back up to Fengdu's terrace before
  its gate, which burns for them, and down again through it; the luopan for levels 57–66, with and
  without an Ancient City known, at Fengdu, and in Hell, sneaking or not; the blueprint: the gate's
  way barred, its side doors' veils, the gate, the doors, the palace and the temples reachable on
  foot from outside the walls, room to stand where one comes back, every sign on a wall);
  `AncientGateGameTests` (soul fire lighting a built frame either way, and not a broken one, nor
  plain fire; through the frame to Fengdu, the swarm rising wave by wave at no less than level 50, a
  stray brought back, the gate barred until the last falls, then burning, and down to Hell; a walker
  over the land getting nowhere, even with the gate burning for another; the gate sealing again,
  and at once for a new arrival; the side doors back to the city, on the side one went in by, and
  for a stranger to a lit frame; an old city's open gate sealed and given its doors; a stray soldier
  of a forgotten swarm turned away); gallery scenes `-Pgallery.only=fengdu_` (the gate from the road,
  from above, the Gate of Ghosts sealed, its guard risen, igniting and burning, a side door, the
  palace inside and out, the avenue, the shrines, the City God's temple), `ancient_frame` (the frame
  as vanilla lays it, lighting and lit), `hell_yin_soldier` and `hell_gate_doors`; in a real world
  with `./gradlew runWorldgenBench -Pbench.mode=zones` (Fengdu's place, its floor, its gate sealed
  and its side doors, and pictures from the road, before its Gate of Ghosts and over the city).


#### Mount Kunlun (the Banana-Leaf Fan)

Mount Kunlun (昆仑山), at the western end of the world, is in the old books the pillar of heaven
and the home of the Queen Mother of the West, whose Jade Pool (瑶池) is on it; and in the novel
(chapter 59) the Banana-Leaf Fan is a treasure that grew behind it when chaos first parted, the
essence of the Great Yin. Here it is where the fan is found: one in the world, in a treasure chest
in the Queen Mother's palace on the summit. A place, not a combat zone: nothing fights there. It is
the axis of the world, the reason the Overworld's sky was raised (see **The taller sky**, above). In
[`world/kunlun/`](src/main/java/com/jttw/engine/world/kunlun/).

- **Where** (`Zones.KUNLUN = Zone.place(kunlun, kunlun_hosts, ZoneLayout.KUNLUN, WHOLE_COLUMN)`,
  host judged at y 256): no level band (none registered, it pays no XP, the Earth Gods never send
  anyone there by level). One a world, placed by `ZoneMap` last of all, so it keeps clear of every
  zone and of Fengdu: **5–8 km out, within 30° of due west** (the journey west; its layout keeps to
  the western quarter as a sea's keeps to its own, `ZoneLayout.quarter`, and if it must widen it
  widens westward still), a region about 930 blocks across (never under 870) on mountain country
  (jagged, frozen and stony peaks, snowy slopes, groves, meadows, cherry groves, windswept hills,
  gravelly hills and forests: the Bottomless Cave's hosts). In the bench's worlds: seed 12345 at
  (−6341, −1652), 6.6 km west, on grove (its summit 436 over the highest land just past its foot,
  the land round its foot at y 67); seed 42 at (−7272, −1873), 7.5 km west, on meadow (426 over it,
  the foot at y 76).
- **The massif** (`KunlunShape`, pure, unit-tested; raised by `world/feature/KunlunFeature` chunk by
  chunk): the summit a level circle 48 blocks out each way at **y 596** (under the taller sky: 44
  under it, room for the palace's roofs), ringed by a sheer crown of cliff; under it a **summit
  pyramid**, four-faced, its arêtes running out along two crossed axes, falling some 200 blocks in
  the first 100 out; then the **flanks**, falling ever more gently to the land's own about 400
  blocks out (a base 800 blocks and more across), meeting it without a crease (at the very foot the
  land's own crags show through; higher, the massif rises from the land's broad shape, as its stair
  is laid out on). Six **spurs** rib the flanks from the pyramid to the foot, their crests standing
  over the valleys between; on five of them a **lesser peak**, a sharp faceted pyramid of its own
  (y 350–460) rising over a saddle; in the middle of each valley a **gorge** from about 165 blocks
  out to the foot, up to 46 deep, steep-walled, a **spring** high in each, its stream falling down
  it. All of it broken by crags and stepped into ledges and cliff bands that wander up and down
  across the flanks (never level rings round it).
- **Its dress**: grass, ferns, podzol and pines low down; over the trees alpine stone, gravel and
  moss; bare rock where it is steep, banded with white calcite, andesite, tuff and diorite; over its
  **upper half snow** (from halfway up, wandering a little), more and more of the cliffs white the
  higher they stand; in the hollows high up **glaciers** of packed ice, falling over the steeps in
  **ice falls** of blue ice. Three **cloud belts** of celestial cloud ring it at about 0.36, 0.6 and
  0.8 of the way up (y 257, 384 and 490 in seed 12345's world): broken sheets, flat beneath and
  heaped above, thick against the flank and thinning out over the shoulders, the summit standing
  over them all; never on the stair. Celestial cloud draws no face against cloud now (as glass), so
  a bank of it is drawn by its outside alone (in Heaven and on Mount Wuzhuang too).
- **The pilgrim's way** (`KunlunShape.path`): a stone way three or four blocks wide that winds about
  twice round the massif, some 2,000–2,200 blocks from its foot to the summit's south edge (a
  long climb: about ten minutes' walk), laid out by walking down from the summit along the flank as
  it is really shaped, a quarter block lower a block, held near the massif's own contour at its
  height (so it winds outward as it falls), steered off the lesser peaks, never coming near its own
  turns; its height then kept within a few blocks of that even descent and to a climb of at most a
  third of a block a block, so it is walked in half steps (stone brick, a slab where the way is half
  a block up), never a whole block at once. Cut into the flank where it is higher (a bank cut back
  gently on the drop side and at 1.5 on the uphill, so it is never a trench), built out on stone
  where it is lower; over drops deeper than 14 blocks and across the **gorges** on **bridges** of
  arches, a pier every 11 blocks, the gorge cut down 5 under the deck so its stream runs beneath. A
  stone-brick rail along any drop, a lantern on a post every 18 blocks on the outer side.
- **Along the way** (`KunlunWay`, a blueprint in each site's own frame, the way up and across): a
  **gate** (牌坊, three bays, white pillars, a gold-trimmed beam, blue tiles) a little way up from the
  foot, 昆仑 *Mount Kunlun, The Way Up* on its face and 天柱 *The Pillar of Heaven* on its back; a
  **rest pavilion** (亭) over the way about every 210 blocks, four white pillars under a pyramid roof
  of blue tiles with a gold finial, a seat on the inner side, a rail on the drop, lanterns at the
  eaves, each its name on a board over the way in (迎仙亭 *Pavilion of Greeting the Immortals*,
  松风亭, 听泉亭, 中天门 *the Middle Gate of Heaven*, 望云亭, 积雪亭, 揽月亭, 凌霄亭, 摘星亭 from the foot
  up); and halfway between, a **shrine** cut into the mountain on the inner side, an altar with
  candles and flowers under a roof of blue tiles. Sites go where the way runs straight, along an
  axis, on its own ground; nothing of them stands on the way itself.
- **The summit** (`KunlunPalace`, a blueprint as Fengdu's city is, laid column by column with its
  signs and its treasure): a terrace paved in white jade (quartz, polished diorite, calcite) 44
  blocks out, a white balustrade round it, snow and snow-laden pines round that; at its south edge,
  where the stair arrives, a three-bay **gate** of white pillars under blue tiles, 昆仑 on its face and
  瑶池 on its back. The **lower court**: the **Jade Pool** in two great halves either side of a white
  causeway with rails and lamps, rimmed with jade, lit from below by sea lanterns (so it never
  freezes), lilies on it, and over the causeway's middle the **Jade Terrace** (瑶台), a pavilion of
  white pillars under a pyramid roof; **peach trees of immortality** in blossom round it. Up a broad
  flight of three steps the **upper court**: more peach trees, the **bell tower** and **drum tower**
  either side under pyramid roofs, and two **halls of scriptures** (shelves of scrolls, lecterns).
  At its head, up three more steps on a white plinth with a balustrade, the **palace** (西王母宫): a
  colonnade round it under a lower roof, white walls with pillars headed in gold and windows of blue
  glass, and over all a great hip roof of blue tiles with a gold-ended ridge (its gold 29 over the
  paving, y 625); within, white pillars, hanging lanterns, a blue carpet to a dais, and on it,
  before the Queen Mother's throne and a screen of jade and gold, between candles, the **treasure
  chest**.
- **The treasure** (`loot_table/chests/kunlun_treasure`, `Kunlun.TREASURE`): the Banana-Leaf Fan,
  always, once, alone in a pool of one roll; and three to five modest gifts (heavenly wine, an
  immortal peach now and then, jade, gold, emeralds, cherry saplings, lily pads). No other signature
  weapon, and no other loot table, recipe or tag in the mod gives the fan.
- **In an older world** (made before the taller sky, its Overworld still 320 high): where Kunlun is
  not yet made, it is made pressed under that sky, not cut off: its summit 44 under it (y 276), the
  same plan, footprint and stair (its heights, crags and gorges pressed down with it), the palace
  whole. It may stand somewhere new: the region is larger now, so where it fits in an older seed may
  differ; one made by an earlier build stays where it is (an older world could then hold two).
- **The Earth God's Luopan**: asked sneaking in the world of the living, the way to Kunlun, for
  anyone (see the luopan, above).
- **Art**: vanilla blocks, the mod's jade and celestial cloud.
- **Tests and pictures**: `KunlunPlacementTest` (one a world, 5–8 km out and within 30° of west in
  every seed, widened or not, on its hosts, never on a zone's or Fengdu's ground, the same for the
  same seed; a place of no band; the massif, the stair and the palace inside its region; the
  massif's shape: a level top, the summit pyramid falling steeply on most sides, a base 680 blocks
  across and more, each lesser peak over its saddle and well under the summit, a deep gorge between
  each two spurs and none high up, the snow from halfway up, the cloud belts in the open and under
  the summit; the summit at y 596 under the taller sky and y 276 under an older one, the massif
  pressed under it and its stair still in half steps; the stair from its foot to the summit's south
  edge, never steeper than its climb, its turns apart, walked in half steps from any block of it to
  the next, its gate at the foot, pavilions all the way up, shrines, every site on the way and found
  where it stands; the fan in Kunlun's treasure once and in no other data file); `KunlunGameTests`
  (the blueprint: from the stair's head through the gate, over the causeway under the Jade Terrace,
  up the broad steps, into the bell tower and a hall of scriptures, up the palace's steps to the
  chest, room to stand, one chest, water only in the pool, every sign on a wall; the treasure rolled
  forty times, the fan in it once each time, no other weapon; the chest laid on the summit, at y 596,
  opened, the fan in it; a stretch of the stair as the feature really lays it in the test world,
  walked block by block: ground under every step, nothing on the way, two blocks of room over head,
  no water, never more than a half step; an older world's Kunlun pressed under its sky; the luopan
  asked and held sneaking, and not, at every level, in Diyu and the Nether); gallery scenes
  `-Pgallery.only=kunlun_` (the gate from the stair's head, the summit from above, the Jade Pool, the
  upper court, the palace, the throne and chest within); in a real world with `./gradlew
  runWorldgenBench -Pbench.mode=kunlun -Pbench.seed=<seed>` (`KunlunBench`: its place, bearing and
  host, the world's sky, its summit against the highest land past its foot, its snow line and cloud
  belts, how long its chunks took, the chest and its loot, the stair walked block by block (its
  steps, room, water, bridges, pavilions, shrines); the views from afar drawn by `KunlunVista` from
  the world's surface as made (the game draws no farther than 32 chunks, and the bench, on modest
  graphics, 10): from 1 km and 2 km toward spawn and 1 km from the side; then pictures in the game:
  the gate at the foot, the stair partway, a pavilion, over the cloud belts from the summit's edge,
  the shoulder, the summit's gate and the summit from above). `-Pbench.mode=zones` shows it on the
  map.

---

#### Flower-Fruit Mountain (levels 20–30; the Six-Eared Macaque, level 120)

Flower-Fruit Mountain (花果山, the Monkey King's old kingdom) and the Water Curtain Cave (水帘洞)
behind its waterfall, taken by the Six-Eared Macaque (六耳猕猴, chapters 56–58), who wore the Monkey
King's shape so exactly that only the Buddha could tell them apart, beat the Tang Monk, stole his
luggage and made himself a false pilgrimage to fetch the scriptures in his place. In
[`world/flowerfruit/`](src/main/java/com/jttw/engine/world/flowerfruit/) and
[`entity/flowerfruit/`](src/main/java/com/jttw/engine/entity/flowerfruit/); its registrations kept to
`registry/FlowerFruitRegistry` (one line in `JTTWEngine`), its art to `FlowerFruitRenderers`.

- **Where** (`Zones.FLOWER_FRUIT`): an island raised out of the sea (ocean, warm, lukewarm and cold
  ocean and their deep kinds; the host judged just under the waves), the whole column. One a world,
  a region about 640 blocks across, 1–2 km from spawn: it shares the first band and its ring with
  White Bone Ridge and Black Wind Mountain (the Earth Gods' needle points to the nearest of the three
  for 20–30, and they name the others with their ways), and keeps clear of every zone before it.
- **The island** (`IslandShape`, pure and unit-tested; `FlowerFruitIslandFeature`, column by
  column): a towering mountain out of the sea, its coast some 220 blocks from the centre, its flanks
  climbing in shelves (the orchards' terraces) and spurs to a sharp peak 170 blocks over the sea; the
  **immortal stone egg** (仙石) on the summit that the Monkey King was born from, split open and
  hollow; **fantastic rock pillars**, sheer columns of banded stone with pines on their heads,
  standing out of the flanks and out of the shallows round it; beaches and shoals. Grey stone banded
  with calcite, andesite and tuff on the faces, moss on the lower ones; grass, moss and podzol above.
  Misty air (pale fog, drifting motes), clear blue-green water.
- **The falls** (on the south face): a great cliff, a river down a gorge from its spring to the
  lip, out over an overhang, and falling **sixty blocks** as a sheet into the pool in the basin at
  the cliff's foot, the pool's stream winding on to the sea; behind the sheet, a ledge along the
  cliff's foot to the cave's mouth.
- **Life** (`FlowerFruitLifeFeature`): peach orchards in pink blossom on the terraces, fallen petals
  under them; broad-leafed trees and flowering azalea lower down, pines on the heights, bamboo about
  the pool and the stream, ferns, grass, peonies, roses and lilac. **The monkeys' camps**
  (`IslandShape.camps`): a ring of mossy stones, a fire and stools, a lean-to, a pole flying the
  Great Sage's yellow banner; each with its band put there and kept (a captain and three or
  four; about one in three roams), and mustered again as the cave's garrison is (below).
- **In a real world** (the bench's audit): the bands round the camps' fires were born inside the
  stools; they are now put where they have room. The cliffs it finds on chunk borders are the rock
  pillars' sheer sides out of the sea (as many lie half-way across the chunks); the water it finds
  beside air is the falls and the river's edges.
- **The Water Curtain Cave** (`CavePlan`, the same in every world, pure and unit-tested, laid with
  the island): the mouth, an arch in the cliff behind the falling water; **the iron bridge** (铁板桥)
  across the chasm the cave's stream runs in, chains for its rails, lanterns at its ends; the gate
  court and its **stone gate**, two stone monkeys squatting before it and over it **the tablet**:
  花果山福地，水帘洞洞天, "Blessed Land of Flower-Fruit Mountain, Cave Heaven of Water Curtain Cave";
  **the great hall**, 84 by 60 under a vault 27 high, ten great columns of dripstone, stalactites,
  lamps on chains, shroomlight in the vault, the monkeys' stone tables, stools, pots and bowls, and
  the Monkey King's old stone throne on its dais under the Great Sage's yellow banners; the
  **dormitory** of stone beds to the west, the **store** of stone pots, bowls, cauldrons and the
  armoury to the east. Its garrison (`CavePlan.POSTS`, 22 monkeys and their captains, and the three
  fake pilgrims) is put there as the land is made and kept, each to its post. **Respawn**
  (`FlowerFruitGarrison`, as Black Wind's and the Fire Cloud Cave's): the garrison and each camp's
  band are marked with the part they hold; a part whose holders are all dead stays empty for **20
  minutes**, then is mustered again whole the next time someone comes near (80 blocks of the falls;
  56 of a camp) while no one is in it (under the cave's roof; within 24 of the camp's fire). The
  Macaque is no part of it. (In a world made before this, the first garrison is unmarked, so once
  its 20 minutes pass a second may join whoever of it still lives.) The Macaque's sanctum, once he
  has fallen, is a seat like any other: a usurper takes it 20 minutes after (level 30, Flower-Fruit's
  band's top), and another after him; the Macaque himself never comes back.
- **The monkey truce** (`ZoneAllies.WUKONG`): the monkeys know the true Monkey King. Nothing of the
  monkey army targets a Wukong player or fights him back, even struck (and what he strikes down of
  theirs leaves him nothing); when he comes near with no fight on they drop to their knees and
  kowtow, now and then one hailing him ("The Great King is home!"). The fake pilgrims and the
  Macaque know him too, and fight him all the same.
- **Creatures** (`FlowerFruitMonster`: level 20 at the shore to 26 at the island's heart, and on into
  the Water Curtain Cave, 26 at its mouth to the band's top, 30, before the old throne; captains three
  to five over the regulars round them, any light, one in eight a captain;
  every attack warned first):
  - **Stone-Throwing Gibbon** (投石长臂猿, light): long-armed, a pale ring of fur round its dark face,
    a sling-bag of stones. You kill it in **3 hits**; its claw is 6.5% (16 kill you). It rears back
    with a stone over its head (0.5 s), then hurls it in an arc, 7% (a captain three, fanned), from
    3–18 blocks.
  - **Spear Ape** (持枪猿兵, normal): a red-faced macaque in leather lamellae, a red headband, a long
    tasselled spear. **6 hits**; its thrust 9% (12 kill you). It draws its spear back (0.7 s) and
    lunges 5.5 blocks, 10% to all in its line.
  - **Great Baboon** (巨狒, heavy): a hulk with a silver mane, a dog's fanged muzzle, iron bands on its
    arms. **11 hits**; its blow 11.5% (9 kill you). It rears up beating its chest (1 s) and pounds
    the ground, 11% to all within 4.5 blocks, thrown up.
  - Captains: twice the hits (gibbon 6, ape 12, baboon 22), blows 1.2x (8–14%).
- **The fake pilgrims** (`FakePilgrimEntity`, the zone's minibosses, its elite trainers, levels 30–31, purple; one each,
  before the old throne with **the stolen luggage**, two chests): the **false Tang Sanzang**
  (kasaya, Vairocana crown, ringed staff) chants the **Tightening-Fillet Spell** (1.5 s, golden rings
  rising): all within 8 blocks struck, slowed and dizzied, and a Wukong player, whose head the fillet
  binds, held fast; the **false Zhu Bajie** (boar's tusks curling up from his jaw in four tapering lengths) heaves his nine-tooth rake over his shoulder (0.9 s) and
  sweeps it round before him, all in its arc thrown back; the **false Sha Wujing** (necklace of
  skulls, crescent-moon spade) levels his spade (0.7 s) and charges 8 blocks. Each a monkey in the
  likeness, a monkey's tail under his robe; struck down, the likeness falls away. You kill one in
  **32 hits** (a captain's 20, and 1.6x); a blow is 16% (7 kill you), a trick 11%. (The novel's fake
  white horse is left out.)
- **The Six-Eared Macaque** (`SixEaredMacaqueEntity`, the strongest boss in the world, and
  **optional**): level **120**, past the players' cap, in **his sanctum** at the back of the cave, a
  tunnel off the great hall with a warning at its mouth ("Beyond sits one who calls himself the Great
  Sage", Lv 120); no one has to face him to have the rest of the mountain, its army, its pilgrims and
  its cave. The sanctum: a round hall under a 30-high vault, six gold-banded black pillars (one for
  each ear) with soul fire at their feet, his throne of black stone and gold on its steps under torn
  yellow banners. He is the Monkey King's mirror: the phoenix-plumed cap seated on the crown of his head, the gold fillet
round it above his brow, golden chain
  mail, a tiger-skin kilt, a red cloak, an iron staff banded in gold, and three ears a side. **430
  hits of a level-100 character**, his staff a third of a level-100 character's health (measured at
  the cap, `ZoneBoss.measuredAt`: a capped group's fight, and four of his blows kill; the one boss that
  is no gym's leader, but the legendary); a yellow bar; he speaks. His kit,
  each part warned:
  - **the whirling staff**: he spins it over his head (1 s), then sweeps it round him: 30%, all within
    5.5 blocks thrown back;
  - **the cloud somersault** (筋斗云): he crouches as a cloud gathers under him (0.6 s), then shoots
    along a line up to 16 blocks, 28% to all in his path, and stands past his foe;
  - **hair clones** (身外身法, from two thirds): he plucks and chews his hair (0.8 s) and blows: three
    of himself (five from a third), frail (2 hits), a light blow, no bar, gone in half a minute, again
    every 30 s; they pay nothing;
  - **the Dharma image** (法天象地, at a third): half again his height, quicker; and **the
    heaven-splitting blow**: the grown staff raised high (1.3 s, its line lit gold on the ground) and
    brought down, 33% along a line 14 blocks long;
  - **six ears**: nothing is hidden from him; he finds the invisible and the sneaking.
  He keeps to his sanctum: brought far out of it he is back at once.
- **What they leave**: the monkeys **monkey hair**, apples, stones, iron; the fake pilgrims gold,
  emeralds, paper, now and then an immortal peach; the stolen luggage bread, paper, gold, emeralds,
  apples; the Macaque **his iron staff** one time in two (+60% damage, +1 reach, a staff's pace;
  `MacaqueStaffItem`), 2–4 immortal peaches, diamonds, gold, sigils, hair.
- **XP and karma**: band 20–30, base grunt XP 80 (White Bone Ridge's pay); the Macaque's hair clones
  pay nothing. Striking him down (he beat the Tang Monk and stole his luggage) is a good deed: **karma
  +150**; the pilgrims are captains (+5), the monkeys monsters (+1).
- **Art**: `FlowerFruitRenderers` (one frame for all of them, its poses for each warning and blow,
  kneeling to the king; models and skins with glowing eyes from `assets_source/flower_fruit.py`, the
  staff and the hair too).
- **Tests and pictures**: `FlowerFruitPlacementTest` (the sea for its hosts, its band, one island in
  its ring and on no other zone's ground, the towering peak and the sea round it, pillars and camps,
  the falls country, the egg, a way on foot from the mouth to the sanctum and the rock over all of the
  cave, the posts), `FlowerFruitGameTests` (the ranks, each warned attack, the truce and the kneeling,
  the pilgrims' tricks, the Macaque's level, kit, phases and lair, loot, XP and karma, the cave in
  blocks), `FlowerFruitGarrisonGameTests` (the cave's garrison and a camp's band: marked, cleared,
  kept empty while someone is in it, mustered again when its time comes round),
  `PeckingOrderGameTests` (the superboss exception, and that he is the strongest boss);
  gallery scenes `-Pgallery.only=scene__ff_`; the island, its falls and its cave in a real world with
  `./gradlew runWorldgenBench -Pbench.mode=flowerfruit`.

#### Heaven: wild grass of every level (levels 1–100; Li Jing, level 103)

Heaven (天庭, the `jttw_engine:heaven` dimension, a sky of cloud islands) is not one zone of a band but
the game's wild grass: an optional place to grind between the zones, with foes of every level, and a
brawl if Heaven turns on you. Its host keeps its islands, and its levels are read off the way to the
Jade Palace ([`HeavenRegions`](src/main/java/com/jttw/engine/world/heaven/HeavenRegions.java)): level
100 at the palace, one level lower every 36 blocks out, down to level 1 on the outermost clouds
(3,564 blocks out and beyond). The road falls into five regions of twenty levels, each ending at a gate
or a court whose head is its gym's leader (its band's top and three, as every zone's):

| Region | Levels | From the Jade Palace | Its head (boss) | Its gods and star lords (minibosses) |
|---|---|---|---|---|
| The Outer Clouds | 1–20 | 2,880 blocks and out | Chiguo, Heavenly King of the East (23), at the Eastern Gate | Rahu, Ketu |
| The Cloud Road | 21–40 | 2,160–2,880 | Zengzhang, Heavenly King of the South (43), at the Southern Gate | Thousand-Mile Eye and Wind-Following Ear (by the Southern Gate), the Moon, Mercury |
| The Star Palaces | 41–60 | 1,440–2,160 | Guangmu, Heavenly King of the West (63), at the Western Gate | Jupiter, Mars, Venus |
| The Thunder Court | 61–80 | 720–1,440 | Duowen, Heavenly King of the North (83), at the Northern Gate | the Thunder Lord and the Lightning Mother, Saturn |
| The Jade Palace | 81–100 | 0–720 | Li Jing, the Pagoda-Bearing Heavenly King (103), before the palace's gate; Nezha and Erlang Shen (102) | the Great Spirit Giant, the Sun |

- **Regulars** are the road's wild creatures: each of a level either way of the road's where it comes
  down, never out of its region's band. **Captains** (a Lunar Mansion, or a captain of the band's own
  kind) stand three to five levels over the regulars round them, never more than one over the region's
  top; **minibosses** five over the road at their post, never more than one over.
- **They keep coming back** ([`HeavenSpawner`](src/main/java/com/jttw/engine/world/heaven/HeavenSpawner.java)):
  every five seconds, round each player in Heaven (not on Peaceful), if fewer than ten of the wild host
  are within 48 blocks, a band of two to four comes down on a cloud onto island ground 10 to 40 blocks
  off (never within 8 of a player): soldiers (two bands in five), archers, thunder drummers or cloud
  cavalry; one band in eight led by a Lunar Mansion, one in twelve by a captain of its own kind. They
  drift over the island they came down on and go when no one is near, as any monster does.
- **The court keeps its posts** ([`HeavenCourt`](src/main/java/com/jttw/engine/world/heaven/HeavenCourt.java)):
  each god, star lord and great name holds an island of his region, toward his quarter (the Kings at
  their gates: east, south, west, north of the palace). He is put there the first time his ground is
  awake; once he falls his post stays empty 20 minutes, then, the next time its ground is awake and no
  one is within 32 blocks, Heaven sends him back. No usurper takes a post of Heaven's. Where no island
  lies near a post, a cloud is laid for him.

**Who the host fights** ([`HeavenHostility`](src/main/java/com/jttw/engine/world/heaven/HeavenHostility.java)).
It depends on who you are:

- **A demon**: a Yaoguai is attacked on sight by everything in Heaven, whatever their karma. That is what
  Havoc in Heaven is for a demon.
- **Bad karma**: at karma −100 and under (Heaven's "Suspect", the standing under Neutral) anyone is
  attacked on sight too.
- **An outlaw** (karma −250 and under) besides has the host sent down round them, wave after wave, as
  long as they stay in Heaven ([`HeavenlyHost`](src/main/java/com/jttw/engine/world/morality/HeavenlyHost.java)),
  at the higher of their own level and the road's, and more levels the more wanted:

  | Degree (karma) | A wave every | Regulars | Lunar Mansions | Heavenly Generals | Star lords | Most out at once | Levels over |
  |---|---|---|---|---|---|---|---|
  | Outlaw (−250) | 30 s | 3 | 0 | 0 | 0 | 10 | 0 |
  | Enemy of Heaven (−1,000) | 20 s | 5 | 1 | 1 | 0 | 18 | 3 |
  | Great Rebel (−3,000) | 12 s | 7 | 2 | 1 | 1 | 30 | 6 |

  (The hunt is Heaven's alone: the host does not come down into the world below.)
- **Everyone else** is left alone. Strike one of the host and it fights back, and every one of its allies
  within 16 blocks that has no foe yet comes to its aid.

**Who struck first.** Heaven judges a kill by who began it (`Grudge`, `HeavenHostility.killDeed`). Killing
one of the host that came at you (on sight, rallying to an ally, hunting you) is self-defence: no deed at
all. Killing one you struck first is a crime against Heaven: a soldier **−2**, a star general (any captain)
**−5**, a god or star lord (a miniboss) **−15**, a Heavenly King **−30**, Nezha or Erlang Shen **−40**, Li
Jing **−60** (as against King Yama's −70 and a Dragon King's −80). So a good player who grinds the host
pays two karma a band (the one they struck; the rest rally and are self-defence), takes some fifty bands
to sink to Suspect, and from then on is attacked on sight and no longer pays at all: grinding alone never
makes an outlaw. Striking first at all four Kings (−120) leaves a player of 0 karma Suspect, not
outlawed; every great name of Heaven, struck first, makes one (−260), as the four Dragon Kings do.

**XP: the road pays as the zones do.** A kill of Heaven's host pays what a zone's kill of the same level
and rank pays (`LevelManager.heavenKillXp`): the band holding its level (White Bone Ridge's for 20–36,
the Bottomless Cave's for 37–46, and so on, Lion-Camel Ridge's for 89 and up), its tier's XP (a captain
or a miniboss a General's, five times a regular's; a great name a Boss's, twenty times), to anyone no
more than five levels over it. Below level 20 it pays as any monster does, weighed by rank: a regular
twice a zombie's, a captain six times, a miniboss ten, a great name twenty.

| The kill's level | A regular pays | A captain or miniboss | A great name |
|---|---|---|---|
| 20–36 | 72 | 360 | 1,440 |
| 37–46 | 260 | 1,300 | 5,200 |
| 47–56 | 396 | 1,980 | 7,920 |
| 57–66 | 575 | 2,875 | 11,500 |
| 67–77 | 728 | 3,640 | 14,560 |
| 78–88 | 972 | 4,858 | 19,430 |
| 89–103 | 1,248 | 6,240 | 24,960 |

**The way up** ([`HeavenGate`](src/main/java/com/jttw/engine/world/heaven/HeavenGate.java)). Before this
only Sun Wukong (Somersault Cloud III) and the Dragon (Dimension Travel) could reach Heaven, both late.
Now anyone may craft a **Heaven Pass** (天门通牒, `HeavenPassItem`: paper, a gold nugget, a feather and
white wool) at level 1. Used below, it carries its holder up on a cloud to an island of Heaven whose road
is of their own level (within two; where islands are few, near the palace, within six, or else a cloud is
laid on their level's ring); used in Heaven, it carries them home. It is kept, 30 seconds between uses.
Falling off the clouds still sets one down in the world below, and the realm leaps still come down at
Heaven's arrival.

**The host** (each kind's strength as a character of its level finds it):

| Creature | Rank | Levels | Hits to kill | Its blow kills you in | Its blow on all (telegraph; area; kills you in) |
|---|---|---|---|---|---|
| Heavenly Soldier (天兵), halberd | regular (captain; a Heavenly General a miniboss) | the road's | 4 (captain 8, General 25) | 12 (captain 10, General 7) | — |
| Heavenly Archer, bow | regular | the road's | 3 (captain 6) | 13 (captain 11), from 14 blocks | — |
| Cloud Cavalry (云骑), lance | regular | the road's | 8 (captain 16) | 10 (captain 9) | the charge: lance lowered, its line marked (14 ticks); a line 7 long; 10 |
| Thunder Drummer (雷鼓手) | regular | the road's | 5 (captain 10) | 15 (captain 12) | the roll: sticks raised, the ring marked (18); 4 round it, quickening the host near; 13 |
| The Twenty-Eight Lunar Mansions (二十八宿) | captain | 3–5 over the road | 14 | 8 | — |
| The Nine Luminaries (九曜星君) | miniboss | 5 over the road at their posts | 28 | 7 | **starfall**: emblem raised, a ring of starlight under each foe within 12 (30); 2.5 round each mark, by his star burning, blinding, slowing, weakening or flinging; 7 |
| The Great Spirit Giant (巨灵神), two great axes | miniboss | ~94–101 | 36 | 7 | **axe-quake**: both axes heaved overhead (30); 6 round him, thrown up; 7 |
| The Thunder Lord (雷公), beak, wings, drums, mallet and chisel | miniboss | ~76–81 | 30 | 7 | **thunderbolt**: wings spread, mallet and chisel raised, crackling rings under each foe within 14 (30); 3 round each, lightning, Slowness II; 7 |
| The Lightning Mother (电母), two mirrors | miniboss | ~76–81 | 26 | 7 | **mirror-flash**: mirrors held up (25); a 90° cone 11 before her, Blindness; 7 |
| Thousand-Mile Eye (千里眼) | miniboss | ~36–41 | 25 | 7 | **piercing gaze**: eyes fixed, a line marked toward his foe (28); 16 long, Slowness II, Glowing; 7 |
| Wind-Following Ear (顺风耳) | miniboss | ~36–41 | 26 | 7 | **echo**: hands cupped to his ears (25); 6 round him, Nausea, thrown back; 7 |
| Chiguo, King of the East (持国天王), pipa | boss of the Outer Clouds | 23 | 70 of a level-20 character's | 6 | **discord**: the pipa swept (30); 7 round him, Nausea, Slowness; 7 |
| Zengzhang, King of the South (增长天王), sword | boss of the Cloud Road | 43 | 70 of a level-40's | 6 | **the blade's circle**: sword raised (25); 5.5 round him, flung back; 6 |
| Guangmu, King of the West (广目天王), serpent | boss of the Star Palaces | 63 | 70 of a level-60's | 6 | **the serpent's lash**: the serpent rears (28); a 70° cone 9 before him, Poison; 6 |
| Duowen, King of the North (多闻天王), umbrella | boss of the Thunder Court | 83 | 70 of a level-80's | 6 | **the umbrella opened**: darkness gathers (30); 8 round him, Blindness; 7 |
| Li Jing, the Pagoda-Bearing Heavenly King (托塔天王) | head of the Jade Palace | 103 | 70 of a level-100's | 6 | **the pagoda descends**: the pagoda raised, its golden shadow under each foe within 14 (30); 3.5 round each, pinned 1.5 s; 6 |
| Nezha, the Third Lotus Prince (哪吒) | boss of the Jade Palace (lesser) | 102 | 70 of a level-100's | 6 | **the wheels' blaze**: the wheels spin up (25); 6 round him, set alight; 6 |
| Erlang Shen (二郎神) and his hound Xiaotian | boss of the Jade Palace (lesser) | 102 | 70 of a level-100's | 6 | **the third eye's light**: the eye opens and burns (30); a 50° cone 14 before him, Glowing; 6 |

Every regular, captain and miniboss rides a small cloud: they fly, never fall (thrown off their island,
they ride back to their post), and with no foe drift low over the island they keep. The great names walk
their islands, and are back on their post at once if thrown off it.

**When a player is Erlang Shen or Nezha.** Heaven's champion will not fight himself: the NPC Erlang Shen
never takes a player who is Erlang Shen for his foe nor joins a fight against them, his blows pass over
them, and theirs pass through him ("I do not cross blades with myself"); so Nezha with a player who is
Nezha. Each fights every other player as all Heaven's host does (an outlawed Erlang Shen is hunted by all
Heaven but its Erlang Shen).

- **Loot** (nothing that breaks the game): soldiers a little gold, archers arrows, cavalry and drummers
  leather; the Lunar Mansions **Star Shards** (a new forging material) and gold, and a captain's sigil as
  every captain; the star lords and gods star shards, gold ingots and their own touch (glowstone, iron,
  copper, amethyst, now and then an Eye of Ender or an Echo Shard); the Kings gold, star shards, sigils,
  perhaps a diamond or a golden apple; Nezha and Erlang the same, and now and then Heavenly Wine; Li Jing
  a block of gold, diamonds, star shards, sigils, perhaps wine, and one time in four an Immortal Pill.
- **Tests and pictures**: `HeavenHostGameTests` (the road's levels from 1 to 100 and its five regions;
  regulars, captains and minibosses rolled by the road; the XP at every level and below 20; whom the host
  attacks on sight by class and karma, a neutral player left alone until they strike and the rally, who
  struck first and the karma, the outlaw's waves; every kind's rank, hits and blow; each great name at
  its region's top; every area blow striking the one who stayed in it and sparing the one who stepped
  out; the champions who will not fight themselves; the court's posts, each in its region and seated in
  Heaven; the wild bands; the pass), `MoralityGameTests` (the waves by degree), `PeckingOrderGameTests`
  (every kind in its rank band, the bosses built for their regions' tops); gallery scenes
  `-Pgallery.only=heaven_` (`HeavenScenes`, `HeavenHostScenes`, `HeavenGodScenes`, `HeavenGreatScenes`).

## The open world's yaoguai

Outside the zones the overworld has yaoguai of its own (`entity/wild`): the early road's monsters,
met before level 20. Each has a level in a low range, shown with its name, and its health and blow
are set against a typical character of that level (`MobScaling`), as a zone creature's are. They
carry no enemy tier and no zone, so their kills pay as vanilla monsters' do: below level 20 by the
killer's level and their weight (`WildYaoguai.vanillaXpShare`, as `LevelManager.vanillaMobWeight` is
a vanilla mob's: a goblin a zombie's 1, an ogre 5), from 20 nothing; the zones are where a character
grows from then on. They keep the zones' pecking order (see Zones): a goblin takes 4 hits and its
blow is 6% of a same-level character's health, a kobold 5 and 8%, a wolfman 8 and 10%, an ogre 12
and 12% (its slam three quarters of that).

| Kind | Levels | Looks | Ways | Where, when | Weight (below 20) | Leaves |
|---|---|---|---|---|---|---|
| Goblin (山魈) | 2–8 | a hunched, long-limbed mountain demon as tall as a man: soot-indigo skin, a wild white mane, a great horn and a broken one, tusks and a gaping fanged mouth, amber eyes under a heavy brow, bone ornaments, torn talismans worn as trophies, dark rags, a jagged iron chopper | quick; packs of 3–5; falls back (crouched, hissing, claws up, eyes on its foe) when hurt below 30% or with none of its pack about it, and comes on again once the pack is round it | hills, forests, plains; on the surface, by night | 1x | copper cash, flint, paper; now and then a string of cash |
| Kobold (穴居蜥妖) | 3–10 | a man-tall lizard warrior and miner: a lizard's head, broad and flat, its long snout low, single teeth along its jaws, slit-pupilled eyes set on its sides, glossy olive scales over a cream belly of scutes, bony plates and a crest of spines down its back and thick tail; a leather harness, a scrap-iron pauldron, a pick, a satchel of ore and a burning lantern at its belt | swarms of 3–6; raises the alarm (every kobold within 16 blocks comes); its pick costs a foe its footing; armoured | under any dry land, in the caves, well below the sea | 1x | ore (copper, iron, coal), copper cash; now and then raw gold, rarely a diamond |
| Wolfman (狼妖) | 10–17 | a tall wolf soldier in black lamellae laced red, a war cloak and a back banner, a long glaive | packs of 2–4; howls when it sights a foe and calls every wolfman within 32 blocks | forests and taiga; on the surface, by night | 2x | wolf pelts (2 leather each), bones, iron nuggets; now and then a string of cash |
| Ogre (山夜叉) | 15–20 | a red mountain yaksha over 3 blocks tall, horns and tusks, a tiger-skin loincloth, spiked iron bracers, a necklace of skulls, an iron wolf-tooth club | alone, rare; slow, every blow heavy and throwing its foe back, hardly moved itself; now and then heaves its club up and slams the ground (everything within 4 blocks hurt and flung) | mountains and swamps; on the surface, by night | 5x | tusks (bone meal), iron, leather, bones; now and then a tael of silver |

Their biomes are data: a NeoForge biome modifier each (`data/jttw_engine/neoforge/biome_modifier/`)
adds them to the monster spawns of the vanilla biomes in their tag (`tags/worldgen/biome/<kind>_haunts`),
so a zone, whose biome takes the ground's place, never has them; `WildSpawns`, their spawn
placements, keeps three of them to the open surface at night and the kobolds to the caves. Their
models and skins come from `assets_source/goblin.py`, `kobold.py`, `wolfman.py` and `ogre.py`
(sharing `wild_paint.py`), written into `client/renderer/WildRenderers`.

---

## Tang villages

Villages of the Tang (唐朝, the seventh century of the novel) generate in the overworld alongside
vanilla's, their folk dressed as the novel's commoners and trading in copper and silver for the
mod's own goods. Talismans and charms are not sold by mortals: only in the supernatural world, at
the ghost market of Fengdu and by Heaven's merchant-immortals. Everything is in
`com.jttw.engine.village`, registered by `registry/ModVillages` (apart from the shared registries,
so it merges cleanly).

**Money** (`Coins`). Copper cash (铜钱, one wen), strings of cash (一串, 8 wen) and taels of silver
(银两, the boat-shaped 元宝, 64 wen). Fixed rates both ways: at a money-changer (the merchant: 64
copper for a tael, a tael for 64 copper, a string for 8) or by crafting (eight copper round a string
make a string of cash, eight strings a tael, and back). A price is paid in silver and copper (100 wen
is a tael and 36 copper); a trader pays out in one stack. Coin comes from selling goods to the folk,
and from loot, modestly (`VillageLoot`): copper in vanilla's village chests, dungeons and desert
temples, a tael or two in shipwreck treasure, silver in the treasuries of the Dragon Kings,
Lion-Camel City and Heaven's pavilions and palace. The villages' own chests hold copper, rice and
food, now and then a tael.

**The folk** (`TangFolkEntity`, `Calling`, `TangTrades`). One creature, the vanilla trading screen,
offers in copper and silver, restocked every morning. Prices in wen (64 = a tael):

| Calling | Sells | Buys |
|---|---|---|
| Farmer (and wife) | 8 rice 6, 4 eggs 3, 4 apples 4, 2 pumpkin pies 5 | 20 wheat 4, 22 carrots 4, 26 potatoes 4, 15 beetroot 4 |
| Innkeeper | 3 steamed buns 6, 4 mantou 4, bowl of rice 4, rice wine 12, tea 8 | 6 chicken 5, 5 pork 5, 5 mutton 5, 6 cod 4 |
| Herbalist | herbal decoction 20, Qi-gathering pill 96, tea 8 | 8 brown or red mushrooms 3, 2 honey 6, 12 glow berries 4, 4 spider eyes 3 |
| Monk | tea 6, 4 mantou 3, a sutra (book) 24 | 2 candles 6, 24 paper 6, 3 honeycomb 4 |
| Scholar | maps to White Bone Ridge 64, the Bottomless Cave 80, the Mystical Waters 96, the Flaming Mountains 128, Mount Wuzhuang 160, Lion-Camel Ridge 192, Fengdu 192, Mount Kunlun 256; 12 paper 6, a book 30 | a book 8, 12 feathers 4, 6 ink sacs 4 |
| Fortune teller | rumour scroll 16, Earth God's Luopan 48 | 6 amethyst shards 6, a rabbit's foot 12 |
| Merchant (money-changer) | a tael for 64 copper, a string for 8 copper; 2 white wool (silk) 6, 4 red dye 4 | a tael (64 copper), a string (8), emerald 24, gold ingot 40, diamond 128 |
| Smith | iron ingot 10, gold ingot 56, jade 192, servant bone 40, wraith wisp 48, captain's sigil 384, Rime Shackle 768 | 4 iron 24, 12 coal 4, servant bone 16, wraith wisp 20, captain's sigil 128 |
| Ghost merchant | demon-quelling talisman 128, ghost-warding talisman 128, house-warding talisman 256, 3 incense 64, peace charm 384 | rice wine 10, 4 steamed buns 6, 2 candles 6, hell bank note 8, wraith wisp 24 |
| Celestial merchant | jade dew 192, demon-quelling talisman 160, peace charm 320, house-warding talisman 256, 3 incense 64 | jade 64, 2 gold ingots 64, crane plume 32 |

Nothing sells for less than any trader buys it back for (`TangTradesTest`): no coin from nothing.
No signature weapon is sold. The goods: **rice**, a **bowl of rice** (three rice, a bowl, water),
**steamed buns** (包子), **mantou**, **rice wine** (Strength for a minute, a little dizziness), **tea**
(Haste for 90 s; cures Nausea and Slowness), the **herbal decoction** (heals 2 hearts; cures Poison,
Nausea, Hunger, Weakness), the **Qi-gathering pill** (15% of one's Qi, at least 40) and Heaven's
**jade dew** (35%, a little Regeneration): far short of an Immortal Peach or Laozi's pill, which
refill a whole pool. The scholars' **maps** are explorer's maps of each zone's region, Fengdu and
Mount Kunlun (its place as the Earth Gods give it, `EarthGod.toKunlun`, kept to one call in
`VillageOffers`), the land drawn round a red cross. A **rumour scroll**, read, tells which demons lie
ahead (`Rumours`): the zone for the reader's level, who rules there, its levels, which way and how
far; and the trial after it.

Each mortal lives as a villager does: wanders the village by day, walks home to its own bed at night
and sleeps there, flees monsters, panics when struck. Killing one, or a spirit merchant, is killing a
villager (karma −50, `Morality`); trading is dealing fairly (+1). They are put there as the village
is made and never spawn of themselves. Their looks (`TangFolkRenderer`, `assets_source/tang_folk.py`,
one frame, fourteen skins): men in the round-collar robe (圆领袍) and the black futou cap (幞头) with
its two ties; women in ru-qun (a short jacket over a high-waisted skirt to the ground, a shawl, the
hair high in a bun with a pin, a red flower-mark on the brow); the farmer in hemp under a conical
straw hat; the monk shaven, a grey robe under a russet kasaya; the innkeeper in a headcloth and white
apron; the smith in a headband and leather apron; the herbalist bearded; the fortune teller in a
long blue robe and square cap; the ghost merchant pale, half-seen and floating, a paper lantern in his
hand; the celestial merchant in white and gold, a gold crown, a long white beard.

**Demons** (`YaoguaiFear`). A Yaoguai player (the Bull Demon King, the Saurian Demon King, the Grand
Saint of Nine Spirits) within 12 blocks sets villagers running: the Tang folk (their own avoidance)
and vanilla's villagers too (they take the demon for a hostile and panic: `VillagerHostilesSensorMixin`).
A mortal of the Tang villages will not trade with one ("A demon! Spare us, Great King!"); the ghost
market and Heaven's merchants deal with demons as with anyone (Heaven's not with its own outlaws).
As the novel's demons do, a Yaoguai may go among men disguised: in another player's likeness, as a
block, or as any vanilla creature that is not a monster, it frightens no one and may trade. A demon's
own true shape (the mod's creatures) or a monster's fools nobody, and a disguised demon who strikes a
villager or a golem is revealed for a minute.

**Talismans and charms** (`Talismans`, `Wards`), sold only in the supernatural world, against the
unclean (the walking dead, ghosts, the zones' demons and their kings; not Heaven's officials, the
Dragon Kings, the Kings of Hell and their officers, nor Mount Wuzhuang's Daoists): the
**demon-quelling talisman** (镇妖符; burned, 3 minutes of +25% damage to them), the **ghost-warding
talisman** (驱鬼符; 3 minutes of −25% damage from them), the **house-warding talisman** (镇宅符;
pasted on a block, for a day no monster comes into the world of itself within 24 blocks), **incense**
(香; for 90 s the lesser unclean will not hunt its burner, and stop hunting them) and the **peace
charm** (平安符; carried, −15% damage from them).

**The markets of the supernatural world** (`SpiritMarkets`). In Fengdu's lower city the two plots
either side of the avenue just inside the gate are the ghost market (鬼市): stalls under black
awnings, candles and soul lanterns on the counters, a dead shopkeeper behind each, placed with the
city (Fengdu spawns nothing). In Heaven one pavilion of the Immortals in three has a jade counter and
a merchant-immortal behind it.

**The villages** (`TangVillage`, a blueprint as Fengdu's city is; laid by `TangVillagePiece` chunk
by chunk, the land smoothed toward it). A structure (`worldgen/structure/tang_village_*`) in a
structure set of its own (`structure_set/tang_villages`: spacing 34 chunks, separation 12, as vanilla's villages; never
within 8 chunks of a vanilla village), in three kinds by biome: **plains** (plains, meadows, forests,
cherry groves: rice paddies, whitewashed walls, grey tiles), **mountain** (taiga and snowy country:
millet, potatoes and carrots, fieldstone and spruce) and **oasis** (desert and savanna: a caravan
halt of sandstone and mud brick, a pond, date palms, camels). It stands only on near-level, dry
ground, and never within a zone's region nor within 108 blocks of one (`VillageSites`, from
`Zones.MAP` alone: the zones, Fengdu and Kunlun). It is 93 by 70 blocks: a main street from the
village gate (牌坊, the village's name on it: Gao Village, Chen Village and others of the novel's
country) north to the **shrine of the Earth God** (土地庙: an altar, candles, an incense burner, the
keeper's bed); a cross street through the square and its **well**; two **courtyard houses** (四合院:
a compound wall and gatehouse, the main hall with its family altar, wing rooms, a paved court with a
water vat); two **cottages**; the **inn and teahouse** (客栈, two storeys, tables, wine jars, a
balcony); the **herbalist's** (药铺); the **smithy** (铁匠铺: a forge and chimney, anvil,
grindstone); eight **market stalls** under coloured awnings and a fortune teller's table; lanterns on
posts; mulberry trees; and **fields** either side. Eleven folk, every calling among them, each with a
bed. Laid mirrored or not, by its seed.

**Tests and pictures**: `CoinsTest`, `TangTradesTest` (prices payable, no arbitrage, the
money-changer at par, no mortal sells a talisman and the spirits do, maps priced by distance, mortal
remedies weaker than Heaven's), `VillageRulesTest` (no village in or beside any zone, the talismans'
arithmetic, the rumours); `TangVillageGameTests` (trading in copper and silver for the mod's goods,
karma for a fair deal, the money-changer, talismans by mortals and spirits, the scholar's maps, the
talismans', incense's and ward's effects, killing a villager, a Tang villager fleeing a Yaoguai,
vanilla villagers taking one for a hostile, refusal and disguise, the supernatural world trading with
demons, a village laid whole with every villager's bed, the structure refusing every zone); gallery
scenes `-Pgallery.only=tang_` (each kind of village from above and through its gate; the plains
village's market, shrine, square, courtyard, inn, smithy and fields; every look of the folk; the
ghost market; the merchant-immortal's stall); in a real world `./gradlew runWorldgenBench
-Pbench.mode=village` (`VillageBench`: the nearest village of each kind, its folk and their beds,
and pictures).

---

## Building

Requires **JDK 21** (Gradle downloads one if none is installed).

```bash
./gradlew build             # jar lands in build/libs/
./gradlew runClient         # launch a dev client
```

Then drop the built jar into `mods/` alongside NeoForge for Minecraft 1.21.1 (21.1.253+).

## Testing

```bash
./gradlew test              # unit tests: game logic, no Minecraft world needed
./gradlew runGameTestServer # GameTests: boots a headless server and runs in-world tests
```

- **Unit tests** (`src/test`) cover pure logic: XP curves, the stat formula, class and form
  rules, every skill tree's structure, each character's skill bar, reincarnation outcomes,
  level milestones, and the encoding of every network packet.
- **GameTests** (`src/gametest`) run against a real server: saved player data, levelling and
  kill XP, skill purchases, alignment and reincarnation, what survives death, Qi, flight,
  dimensions, items, transformations and each character's abilities. They live in their own
  source set, so nothing from them ships in the mod jar. Each test runs in a small platform
  structure with its own NeoForge `FakePlayer`.
- **The form gallery** checks what tests can't: how each transformation looks.
  `./gradlew runFormGallery` opens a real client and photographs every form from the front, the
  side, and the front while looking up. It saves the photos to
  `run-gallery/screenshots/gallery-<time>/` and then closes. `python tools/form_sheets.py` lays
  them out as contact sheets and lists the forms that draw nothing. In the front view a model's
  face should point at you, in the side view its head should point left, and in the looking-up
  view it should tilt up.

### Commands

```
/jttw transform                     open the transformation menu
/jttw transform <entity_id>         (operators) transform into any entity, skipping the arts' rules
/jttw transform <entity_id> <players>  (operators) transform other players
/jttw demorph [players]             revert (others: operators)
/jttw status [players]              inspect current transformation state (others: operators)
```

---

## Known limitations

- Animation fidelity for vanilla entities is bounded by what their existing models support.
- Projectiles drawn as flat sprites (snowballs, eggs, fireballs, firework rockets) always face the
  viewer, as they do in vanilla, so they don't turn or tilt with the player. An area effect cloud
  form is its cloud of particles.

## License

All rights reserved. *Journey to the West* is public-domain source material; Minecraft and NeoForge are
the property of their respective owners, and no Mojang assets or source are redistributed here.
