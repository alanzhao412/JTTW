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
| **Heavenly** | 1000 | Medium | Widest utility toolkit. Ability-expansion focused; higher forms gain perception and authority advantages. |

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

- **Water** (Dragon King) — oceans, rivers, and during rain
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
activates (`MENU`, `TOGGLE`, `HELD`, `PASSIVE`), and what each slot's key does.
[`Characters`](src/main/java/com/jttw/engine/character/Characters.java) is the one place a player's
reincarnation outcome is mapped to their character. A passive ability is a new node; an active one
is a new node plus its slot in that character's class.

Levelling runs on a Qi resource system (`QiManager`), with `BiomeLevelBand` and `EnemyTier` scaling
the world to player level.

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
- **Wu Clones** — Wukong's hair-clone technique, implemented as summoned entities with their own
  pathfinding goals ([`WuCloneManager`](src/main/java/com/jttw/engine/character/wukong/WuCloneManager.java))
- **Xiaotian Quan** — Erlang Shen's hound, a custom entity with hand-authored model and animation
- **Phoenix revival** — death-and-rebirth mechanic tied to the Phoenix line

Custom **Heaven** and **Hell** dimensions ship with their own worldgen, biomes, and noise settings.
Heaven is a sky of cloud islands with the Immortals' pavilions, the Queen Mother's peach gardens
and the Jade Palace. Hell (Diyu) is a dark underworld of ash plains, black stone hills and rivers,
crossed by the Naihe, with the arrival plaza where the Book of Life and Death is kept and, far to
the north, the fortress of the Ten Courts: the levels 57–66 zone (see "Hell" under Zones). Sun
Wukong is sent there at level 30 to strike his name from the Book; the Dragon's and Wukong's
dimension abilities travel there too.

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
  permits dimension travel — with per-character speed variation layered on top.
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

19 packet types in [`network/`](src/main/java/com/jttw/engine/network/) handle this, most of them
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
├── dimension/       Heaven and Hell dimensions with custom worldgen
├── world/           zones (Zones, ZoneLayout, ZoneMap, ZoneAllies), zone features, Heaven, morality
├── entity/          custom entities, projectiles, summons; zone/ holds ZoneMonster and ZoneBoss
├── item/            lore artifacts and weapons
├── client/          renderers, GUI screens, HUD overlays, client input and flight
└── commands/        /jttw command tree
```

Largest components: `TransformationAbilityHandler` (1,385 lines), `YuanDiAbilityHandler` (1,236),
`WuCloneManager` (874, including custom pathfinding goals).

### Zones

A level zone is a biome put over the overworld in regions laid out from the seed alone
(`world/Zones`): no biome library. Each zone has a biome, the vanilla biomes it may take over
(a tag, checked at its region's centre), a `ZoneLayout` (cell size, share of cells, radius,
wobble, a salt of its own), the heights it takes (`Vertical`: the whole column, `below(y)` for an
underground zone, `above(y)` for an aerial one), its level band and the players it is allied to.
Zones sharing heights never share ground: a later zone in `Zones.ALL` gives up a whole region
that would touch an earlier one's (`ZoneMap`). White Bone Ridge is the first, the Bottomless Cave the second, the Mystical
Waters the third; Hell, a dimension of its own, is the fourth; then the Flaming Mountains, and Mount Wuzhuang, the first
aerial zone, in the sky; Lion-Camel Ridge, the last band, is the last.

To add a zone:

1. **Biome**: `data/jttw_engine/worldgen/biome/<zone>.json` (its features) and a hosts tag in
   `tags/worldgen/biome/`.
2. **Definition**: a `Zone.overworld(biome, hosts, new ZoneLayout(..., uniqueSalt), vertical,
   minLevel, maxLevel, gruntXp)` in `Zones`, appended to `Zones.ALL`; `.hostSampledAt(y)` for a
   host judged off the surface, `.alliedTo(ZoneAllies.PENG)` for a zone that spares the Peng. A
   zone of another dimension (Hell) is `Zone.dimension(biome, ...)`: a level band, no regions.
   Level bands are registered from `Zones.ALL`; the biome source places the biome by itself.
3. **Monsters**: extend `entity/zone/ZoneMonster` (`zone()`, `hitsToKill()`, `blowShare()`;
   override levels, captain chance, escorts, captain loot, `allies()` as needed).
4. **Boss**: extend `entity/zone/ZoneBoss` (`zone()`, `lairRadius()`; a boss bar, persistence,
   Boss tier, phases through `phaseThresholds()` / `onPhase()`).
5. **Worldgen**: features read `zone.depth(...)`, `zone.centre(...)`, `zone.isAt(level, pos)`
   and `ZoneTerrain.landAt(...)`; the lair extends `world/feature/ZoneLairFeature` (`zone()`,
   `boss()`, `build(...)`, `floorAt(...)`), which builds it at each region's centre and puts the
   boss there once.
6. **Tests**: add the zone to `ZoneFrameworkTest`'s checks if its layout is unusual; GameTests
   for its monsters and boss as `RidgeGameTests` does.

#### The Bottomless Cave (levels 37–46)

Mount Xiankong's Bottomless Cave (无底洞, chapters 80–83), home of Lady Earth Flow, the
Golden-Nosed, White-Furred Mouse Spirit. An underground zone (`Zones.BOTTOMLESS`):
`Vertical.below(40)` under mountain country (peaks, slopes, groves, meadows, cherry groves,
windswept hills), in 90% of 1024-block cells (only those whose centre is in mountain country hold it: about one in thirty), regions about 220 blocks across. Its host is judged
at y 256, above any mountain's top, where a column's biome is always its surface biome (judged lower,
under a tall peak, it can be a cave biome). Being after White Bone Ridge in `Zones.ALL` and sharing
its heights, it gives up any region that would touch the ridge's. Grunt XP 100 (260 a kill there,
with the band's scaling), against the ridge's 80.

- **Terrain** (`world/feature/Labyrinth`, `BottomlessCaveFeature`): at the region's centre a
  sinkhole and a mouth about ten feet across, then a shaft down from the surface to y 32, open in
  the middle (a pool at the bottom to fall into) with a spiral mud-brick stair round it, laid in
  half blocks, a lantern each turn. Below, three levels of labyrinth (walked at y 32, 16 and 0), each
  a maze on a 21x21 grid within 70 blocks of the centre: a Kruskal spanning tree (it branches and
  dead-ends often) with a few loops, six chambers a level (nests, larders, bone heaps, flowered
  grottoes), and three spiral stairs down from each level, out toward the rim and never under one
  another, so every level must be crossed. The top level's hub takes the shaft. At the bottom, her
  palace: a 31-block hall of blackstone and gold, red pillars, a carpet from gate to gate up to her
  dais, flower beds down both sides, smoking censers, candles, lanterns on chains, spore blossoms
  and shroomlight lamps; and out toward the rim, her shrine, with tablets to her adoptive father and
  brother (Li Jing and Nezha). The labyrinth is worked out from the seed and the region's centre
  alone (`Labyrinth.of`, pure Java, unit-tested): each chunk lays its own part, palace included, so
  nothing laid later undoes it. Its slabs of height are its own (rock where not open), so vanilla
  caves, water and lava never break in. Tunnels are lit here and there (lanterns, pale soul
  lanterns), stairs by a light in their pillar, chambers by a lantern, the palace well.
- **Mouse Spirit** (`entity/cave/MouseSpiritEntity`): 2 hits, a bite of 7% of health; quick
  (0.36), in packs of 3–5 (a captain brings 4). Strike one and the pack turns on you; each darts in,
  bites and scurries back, so a pack circles.
- **Flower Maid** (`FlowerMaidEntity`): 5 hits, a stab of 15%; every 5 s she mends the most
  wounded of the household near her (a fifth of its health; 3% of her mistress's), and pressed
  close she breathes out pollen (Slowness II and Weakness for 4 s).
- **Lady Earth Flow** (`LadyEarthFlowEntity`, a `ZoneBoss`, level 43, 80 hits, pink bar): her
  twin swords cut twice a stroke (12.5% each; the female blade slows). At 60% (phase 1) she calls
  4 mice and 2 maids and starts to burrow, rising behind whoever she fights. At 30% (phase 2),
  once, she leaves her embroidered shoe in her likeness (`EarthFlowDecoyEntity`: weak, no XP or
  karma, a shoe again when struck down or after a minute) and flees to her shrine, strewing petals
  on the floor along the way; no blow kills her before that. Alone there she mends, up to half her
  health; found, she fights to the end. Karma +150, captains +5.
- **Drops**: her twin swords (`EarthFlowSwordItem`, male and female: +35% damage, a quick sword;
  held as a pair, drawn into both hands like Nezha's Yin-Yang Swords through `PairedBlade`, every cut
  is followed by a second for 40%), the Stolen Candle of Spirit Mountain, sigils, gold; Spirit
  Whiskers from mice, Night-Blooming Petals from maids (and a second True Samadhi Fire recipe from
  them), the Embroidered Shoe from her likeness.
- **Art**: `CaveRenderers` (a box model for the mice; a person's frame with skirt, collar, hair,
  ears, gold nose and tail for the maids and her); skins from `assets_source/mouse_spirit.py`,
  `flower_maid.py`, `lady_earth_flow.py`, item icons from `cave_items.py` (helpers in `cave_skin.py`).
- **Seeing it**: `./gradlew runFormGallery -Pgallery.only=cave_` (creatures, her, her likeness,
  and the labyrinth laid in the gallery's flat world: palace, shrine, tunnel, stair, hub);
  `./gradlew runWorldgenBench -Pbench.mode=cave` (the nearest real region: a plan of each level as
  built, a cut through the shaft, and pictures, in `run-bench/screenshots/zones/`).
#### The Mystical Waters (levels 47–56)

The Four Seas and the Dragon Kings' crystal palaces (水晶宫, novel chapter 3), in
[`world/waters/`](src/main/java/com/jttw/engine/world/waters/) and
[`entity/waters/`](src/main/java/com/jttw/engine/entity/waters/).

- **Where**: over deep ocean only (deep, deep cold, deep lukewarm and deep frozen ocean, judged just
  under the waves at y 60), the whole column. 90% of 1536-block cells roll a region about 440 blocks
  across; only those centred over deep ocean stand (27 regions in the 32x32 km round spawn of seed
  12345, the nearest 4.3 km out). Each region is one sea's: the one that lies that way from the
  world's origin (within 45° of +x the East's, +z the South's, and so on), and its palace's gate faces
  that way.
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
  lanterns out across the floor. Its king and a guard of six are put there once
  (`DragonPalaceFeature`, the framework's `ZoneLairFeature`, which now lets a zone choose the boss by
  region).
- **Creatures** (`SeaMonster`: level 47, captains 54; they breathe water, are not pushed by the
  current, and come in any light in water at least 24 blocks deep):
  **Shrimp Soldiers** (虾兵: swim, spear thrust from further than a sword's reach, dart backward when
  struck), **Crab Generals** (蟹将: walk the floor and sink, heavy pinch that holds, spring up at
  what swims above), **Yaksha** (夜叉, the sea patrol: the fastest swimmers, hurl bolts of the tide,
  raise the alarm so every sea creature near turns on their foe).
- **The Dragon Kings** (`DragonKingEntity`, a `ZoneBoss`, level 55, sixty ordinary hits): a dragon in
  a man's shape, robed and crowned, swimming freely. Bolts of his sea (three, fanned, past two
  thirds), a claw up close, and his sea's own wrath every 8 s (5 s past a third): **Ao Guang**
  (East, blue-green robes) a tidal surge that hurls all near him away; **Ao Qin** (South, red) the
  boiling sea that scalds and saps strength; **Ao Run** (West, white) a whirlpool round his foe that
  drags all near it to its grinding heart; **Ao Shun** (North, black) the frozen abyss that freezes and
  slows. At two thirds he calls Shrimp Soldiers and a Crab General, at a third his Yaksha.
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
  soul lanterns, with the **Gate of Ghosts** (鬼门关) at its north edge; the **road** north, lit
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
- **The arrival's safety** (`HellRules`): a level-30 Wukong comes here for the Book. The fortress
  and its kings are over 300 blocks north, over the Naihe (the nearest king 363 blocks off);
  nothing of Hell spawns by itself within 160 blocks of the arrival (the server spawns within 128
  of a player, so none ever spawns round someone standing there); the officials spawn only within
  the fortress's walls, the ghosts out on the plains; and the plaza is a **sanctuary** (48 blocks):
  no creature or king of Hell attacks anyone in it, and one that chased someone in gives up.
  `JTTWDimensions.findSafeSpawn` now makes a chunk before it reads its height (it used to read an
  unmade chunk's as the world's floor, and set the Book's dais in the air at y 64).
- **Ox-Head** (牛头, `entity/hell/OxHeadEntity`, level 58): 7 hits, a blow of 16%; kept at a
  distance he lowers his horns and **charges**, goring (his blow and a half) and throwing what he
  runs down. **Horse-Face** (马面, `HorseFaceEntity`, 58): 6 hits, 14%; casts his **hook on a
  chain** at whoever keeps off (4–12 blocks) and hauls them in, slowed.
- **Judge-Clerk** (判官, `JudgeClerkEntity`, 58): 4 hits, 9%; every 5 s he **writes his quarry's
  name** in his ledger: marked (glowing, 10 s), and every creature of Hell near them that is idle
  comes for them; every second writ **summons** a guard, Ox-Head and Horse-Face by turns, two of his
  at most.
- **Black and White Impermanence** (黑白无常, `ImpermanenceEntity`, 59): met in the world, one comes
  with the other. The Black (6 hits, 15%) **shackles** (Slowness III 2 s); the White (4.5 hits, 12%,
  a longer reach) **drinks** half of what his tongue takes and weakens. Near each other each takes a
  quarter less from every blow; when one falls the other is **enraged** (Strength II, Speed II).
- **Wandering ghosts** (游魂, `WanderingGhostEntity`, 57): 2.5 hits, 7%, in twos to fours on the
  plains; their touch **chills** (slowed, hungry); they drift, never take falls; restless dead
  (healing harms them).
- Captains as in every zone (purple, larger, stronger, with escorts; the Impermanence's captain
  brings only its partner), never past level 66.
- **The Ten Kings** (`KingOfHellEntity`, `HellKings`, ZoneBosses on their thrones, 50 hits, a
  tablet blow of 14%, red bars; levels 57–65 from the First to the Tenth, King Yama passed over),
  each passing his court's **judgement** every 7 s on whoever fights him within 14 blocks:
  1. **Qinguang** (秦广王), the **Karma Mirror**: a beam for his blow, ×0.6 for the good (karma 100+),
     up to ×3 for the wicked (1 + |karma|/500);
  2. **Chujiang** (楚江王), the cold hell: all within 8 **frozen** (frost, Slowness III 5 s);
  3. **Songdi** (宋帝王), the **iron hooks**: hauls his foe from up to 14 blocks to his feet and strikes;
  4. **Wuguan** (五官王), the **scales**: takes half the difference of their health shares from a
     healthier foe;
  6. **Biancheng** (卞城王), the **bell** of the Wailing City: all within 7 **kneel** (cannot move) 2 s;
  7. **Taishan** (泰山王), **boiling oil**: all within 7 scalded and burning (he does not burn);
  8. **Dushi** (都市王), the **crushing carts**: charges, crushing (×1.6) and flinging what he meets;
  9. **Pingdeng** (平等王), "Equal": **equal measure**, a third of every blow struck him struck back;
  10. **Zhuanlun** (转轮王), the **Wheel**: struck down the first time he is **born again** at half
      health; his judgement is **Meng Po's soup** (his foe's good effects forgotten; nausea).
- **King Yama** (阎罗王, `KingYamaEntity`, the Fifth Court, level 66, 140 hits, 17%, purple bar,
  the climax): phase 0, his blows and **the Writ** every 10 s (marked; Hell's creatures near come);
  at 66% **his court rises** (Ox-Head, Horse-Face, a clerk) and he passes **sentence**: a ring of
  soul fire where his foe stands, and 1.5 s later judgement falls there (a third of the health of
  whoever is still in it, and they burn; step out); at 33% **the wrath of Yama**: he grows (×1.35),
  strikes and moves 30% harder and faster, sentences fall every 3.5 s, and soul fire round him
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
  kings: robe, wide sleeves, jade belt, mian crown with bead strings, beard, ivory tablet, each his
  colours; Yama's eyes burn); skins from `assets_source/hell_guards.py`, `hell_officials.py`,
  `hell_kings.py` (helpers in `hell_skin.py`), icons from `hell_items.py`.
- **Tests and pictures**: `HellGameTests` (creatures, captains, XP, each king's judgement and relic,
  Yama's phases and sentence, karma and alignment, the arrival in the real Hell: level, the one
  Book, nothing near, the sanctuary; the kings on their thrones), `HellZoneTest` (the land, the
  plan, the arrival's distance and spawn rules); gallery scenes `-Pgallery.only=hell_` (creatures,
  the kings' lineup, Yama and his wrath, and pieces of the works laid off the stage: a court hall,
  Yama's hall, the Tenth Court and the Wheel, the bridge and Meng Po's pavilion); Hell as it is
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
  the whole column. 90% of 1152-block cells roll a range about 400 blocks across; being last in
  `Zones.ALL` it gives up any that would touch an earlier zone's, and only those centred on hot,
  dry ground stand: in the 32x32 km round spawn of seed 12345 the layout rolls 709, 398 give way,
  276 are not on hot ground, and **38 regions** stand (0.42% of the ground; the nearest 4.8 km out).
- **The range** (`RangeShape`, `FlamingRangeFeature`, pure and unit-tested): from the region's edge
  the ground climbs over the outer fifth into long chains of red mountains (ridged noise, rounded
  summits, up to some 90 blocks over the land), valleys between them; Mount Haohao rises 45 more
  at the heart, and a gorge is cut through it from the cave's gate to the region's edge. Faces show
  banded flame rock, red, orange and brown terracotta with seams of magma; high ground is flame
  rock and cinder ash; valley floors (about a quarter of it) are **burning earth**. Fire on the
  burning earth in patches, seams of lava in level valley floors, smoking vents, charred basalt
  spires (`FlameSeamsFeature`). Orange sky and fog, falling ash, no rain; nothing grows.
- **The heat** (`FireMountain`, `BurningEarthBlock`): lit burning earth scorches whatever stands on
  it once a second, **4% of its health** (magma's hot-floor damage, so armour softens it) and sets it
  alight for 3 s; **Fire Resistance**, or being fire-proof, keeps it off entirely (as it does the
  fire on the ground). Rock, ash and fanned earth are safe. Fire on lit earth never goes out, and
  creeps across it (never more than five fires in a 5x5).
- **The Banana-Leaf Fan** (芭蕉扇) puts it out: as before it throws back what is in front (the Bull
  Demon King's blows it away), and now each wave also quenches the burning earth and ordinary fire
  in its sweep (60° either side) out to **12 blocks** (20 in the Bull Demon King's hand) for **a
  minute**, after which the earth kindles again by itself. It does nothing to the Samadhi fire.
- **Creatures** (`FireMonster`: level 67, captains 74, fire-proof, any light): **Ember Imps**
  (火焰小妖: quick and frail, 4 hits; claws 10% that set alight; fling an ember for 12% every 2.5 s
  from 3–16 blocks, a captain three at once; strike one and the rest turn; water hurts them) and
  **Furnace-Brick Brutes** (炉砖怪, bricks of the Eight Trigrams Furnace that stood up: 12 hits,
  armoured, blows of 30%; when close they raise both fists for 0.8 s and slam the ground: 22%, thrown
  up and set alight within 4 blocks).
- **Red Boy's Six Strong Generals** (六健将, `StrongGeneralEntity`, one model, six skins): always of a
  captain's strength (level 74, 25 hits, blows of 29%), alone, posted at the cave (the two Winds at
  the gate, four in the hall), kept even on Peaceful. Each has his trick every 5 s: **Cloud-Soaring
  Sprite** (云里雾) leaps high and lands in a burst of blinding cloud; **Fog-Diving Monster** (雾里云)
  vanishes into fog and comes out behind his foe; **Mad-Hurrying Wind** (急如火) charges, fire springing
  up in his track; **Swift-Rushing Wind** (快如风), the fastest, hurls all near him away; **Flame-Head
  General** (兴烘掀) bursts into flame; **Eye-Spark Officer** (掀烘兴) shoots sparks from his eyes.
- **Red Boy** (红孩儿, the Holy Child King; `RedBoyEntity`, a `ZoneBoss`, level 77, 70 hits, red bar),
  son of the Bull Demon King and Princess Iron Fan: a child in a red stomacher with the Fire-Tipped
  Spear (the mod's own item, held). Spear thrusts of 26% that set alight. Every 7 s he stops,
  strikes his nose twice (1 s) and breathes the **True Samadhi Fire** (三昧真火) from mouth and nose: a
  cone 10 blocks long, a blow of 20%, then the burning; flames are left on the ground. At two
  thirds (phase 1) his **five fire carts** (五辆小车) are set out round his lair in the five-element
  pentagon and lit: each belches the fire round itself every 3 s, and while any stands **every blow
  on him is halved**; break them first (5 hits each). At a third (phase 2) the fire comes from his
  nose and mouth both: a ring of it bursts out all round him, and from then on it is fiercer and
  comes every 4 s. A Bull Demon player is hailed as his father, and fought all the same.
- **The True Samadhi Fire** (`SamadhiFire`, the `samadhi_fire` effect, `SamadhiFlameBlock`): it burns
  as a status, **3% of health a second** (4.5% at its fiercest), whatever the burning one is (the
  fire-proof burn too; armour does not soften it). **Water does not put it out, it feeds it**: in
  water or rain it burns half again as hard (the Dragon Kings' rain in chapter 41); milk does
  nothing. Only **Fire Resistance** stands against it, and only **halves** it (the breath's blow and
  the burning). On the ground it burns out after 5–8 s and only so: no water flows into it or can be
  poured on it, a splash of water does not douse it, no hand beats it out, the fan passes over it.
  It spares Red Boy's household. His breath lights the Eight Trigrams Furnace in its line.
- **The Fire Cloud Cave** (`FireCloudCave`, a blueprint each chunk lays its share of): a stone gate of
  polished blackstone with a gilded plaque, red pillars and braziers, at the head of the gorge; a
  lantern-hung passage into the mountain; the inner hall, a domed cavern of red brick, six pillars
  with fire-lamps, four pools of lava, a gilded dais where Red Boy stands, a red carpet to it, his
  throne between two braziers. Red Boy and his generals are put there once (`FireCloudCaveFeature`).
- **What they leave**: Red Boy **2–3 True Samadhi Fire** (the furnace's fuel; a real source of it at
  last), his **Fire-Tipped Spear** one time in four (+5% a Looting level), ember cores, furnace
  bricks, gold, sigils. Imps leave **Ember Cores**, brutes **Furnace Bricks**, generals both, a
  captain's sigil and now and then a True Samadhi Fire. Three ember cores, two furnace bricks and a
  captain's sigil make a True Samadhi Fire (`true_samadhi_fire_from_the_mountains`). Flame rock,
  cinder ash and burning earth can be mined.
- **XP and karma**: band 67–77, base grunt XP 130 (728 a grunt at the band's scale, about four kills
  a level at 67, near the waters' pace at 47). Subduing Red Boy, who wanted to eat the Tang Monk, is
  a good deed: **karma +150**, as Lady White Bone; the generals are captains (+5), the rest monsters
  (+1). Red Boy's creatures are **not** allied to a Bull Demon player.
- **Art**: `EmberImpRenderer`, `FurnaceBruteRenderer`, `StrongGeneralRenderer`, `RedBoyRenderer`,
  `FireCartRenderer` (box models and skins with glowing parts from `assets_source/fire_court.py`);
  blocks and items from `assets_source/fire_items.py`.
- **Tests and pictures**: `FireGameTests` (heat, fan, Samadhi fire and water, Red Boy's breath,
  carts and phases, creatures, generals, drops, XP, karma, the cave's blueprint),
  `FireMountainPlacementTest`; gallery scenes `-Pgallery.only=fire_`; the range in a real world with
  `./gradlew runWorldgenBench -Pbench.mode=fire`.

#### Mount Wuzhuang (levels 78–88)

Mount Longevity and the Temple of the Five Villages (万寿山五庄观, chapters 24–26), home of the
Great Immortal Zhenyuan and his ginseng-fruit tree, in
[`world/wuzhuang/`](src/main/java/com/jttw/engine/world/wuzhuang/) and
[`entity/wuzhuang/`](src/main/java/com/jttw/engine/entity/wuzhuang/). The first **aerial** zone
(`Zones.MOUNT_WUZHUANG`): `Vertical.above(192)`, over the world's own clouds, the ground under it
left to its host.

- **Where**: over serene country (plains, sunflower plains, forest, flower forest, birch and
  old-growth birch forest, meadow, cherry grove), judged at y 256 (where a column's biome is always
  its surface biome). 75% of 2048-block cells roll a region about 260 blocks across (never under
  224); last in `Zones.ALL`, it gives up any region that would touch a whole-column zone's (the
  ridge, the waters, the Flaming Mountains), and may float over the Bottomless Cave. In the 32×32 km
  round spawn of seed 12345: 124 regions laid out, **41 over serene hosts** (about one in every
  5×5 km), the nearest 3.1 km from spawn.
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
- **The temple** (`Temple`, a blueprint laid column by column; the region's centre is Zhenyuan's
  place): the mountain gate in the south wall with its plaque 五庄观 and the novel's couplet
  (长生不老神仙府 · 与天同寿道人家); a paved front court with a smoking censer, stone lanterns, pines,
  the bell tower and the drum tower; the disciples' wings down both flanks; the **Hall of Heaven
  and Earth**, double-eaved, red columns within, and at its back the **altar** where, as the novel
  says, only the words 天地 are honoured; through a moon gate the **garden**: a pond with lilies, a
  pavilion, rockeries, flowers, and the ginseng-fruit tree.
- **The ginseng-fruit tree** (`GinsengTree`): a broad trunk, green boughs, leaves like the
  plantain's, and twelve **fruit** (`GinsengFruitBlock`: blossom, green, ripe; shaped like a newborn
  child) hanging under its low boughs, half of them ripe when the land is made; each stage takes one
  random tick in 60 (about an hour of play). The fruit **fears the five elements**:
  - struck with **gold** (any golden tool, or the **Golden Striker** 金击子) a ripe fruit falls
    whole; struck with anything else (iron, wood, a bare hand) it falls and withers; an unripe one
    only withers;
  - falling, no hand can catch it: only a **Silk-Lined Tray** (丹盘) held under it (`Wuzhuang.fruitTick`);
  - wherever it comes to rest on the bare earth it **sinks into the ground** and is gone, unless on
    something lined (wool, a carpet, a bed); in water it **dissolves**;
  - **eaten**: healed whole, Regeneration II 30 s, Absorption III 2 min; and the **first** a soul
    ever eats adds its 47,000 years: **+20% maximum health, for good** (an `ADD_MULTIPLIED_TOTAL`
    modifier, kept through death and into a new life).
- **Daoist disciples** (`DaoistDiscipleEntity`, level 78): 6 hits, a sword stroke of 13%; at 3–10
  blocks a **binding talisman** (定身符: held fast 1.5 s, weakened). Righteous: they fight no one
  unprovoked, except a thief of the fruit; strike one and those near come.
- **Cloud cranes** (`CloudCraneEntity`, level 80): white, black-tipped wings, red crowns; they fly
  (never fall) within 40 blocks of where they came into the world and harry any stranger (not the
  temple's guests); from 3+ blocks above they **swoop**: a dive, the beak for its blow and a half,
  and whoever is struck is thrown back (mind the edge). 4 hits, 11%.
- Captains as in every zone (purple, larger, stronger, escorts of two), never past level 88.
  Spawned on the island in any light: cranes (weight 100, in twos and threes), disciples (60).
- **Clear Wind and Bright Moon** (清风, 明月; `TempleYouthEntity`, mini-bosses, level 84, 70 hits,
  12%, a blow takes at most 6% of their health), by the tree, fighting as a pair (strike one and the
  other comes): Clear Wind's **gust** hurls his foe away; Bright Moon's **moonlight** blinds and slows
  and mends his partner by a tenth; both **curse** a thief roundly (it weakens). Each **yields** at
  10% (see the trial): the first time, Clear Wind gives the **Golden Striker**, Bright Moon the
  **Silk-Lined Tray**: the two things it takes to bring a fruit down whole.
- **The Great Immortal Zhenyuan** (镇元大仙, `ZhenyuanEntity`, a `ZoneBoss`, level 88, 160 hits,
  15%, yellow bar), before his altar. Not evil, and not to be beaten: his fight is a **trial**
  (`TempleImmortal`). **Hard to harm**: no blow takes more than 2.5% of his health, and one blow in
  four struck at him hand to hand he brushes aside with his whisk. **The Universe in his Sleeve**
  (袖里乾坤, `Sleeve`): every 12 s (7 s from 30%) his sleeve is raised and the air drawn toward it for
  a second, then whoever he fights within 12 blocks (from 30%, everyone within 8) is **swept in**:
  out of the world (unseen, blind, held at his side wherever he goes, nothing can touch them nor they
  anything), for 5 s, then cast out bruised (a tenth of their health), unless they **struggle free**
  first (five blows at him from within: they burst out unhurt, and he staggers, unable to parry, for
  2 s). At 60% **his disciples** come (four). At **10% he yields**: he stops, cannot be harmed, lets
  go of whoever he holds, and acknowledges everyone who fought him in the last 30 s: a boss's XP,
  his loot, the first time his **flywhisk** and a **ginseng fruit**, and **his leave**: the
  temple's guest (its fruit theirs to take without theft, his cranes no longer harry them; kept by
  the soul). He rests five minutes, mends, and rises whole: the trial can be had again (rewards at
  most once a day a player). No blow kills him (only what kills outright, as /kill, does).
- **Drops** (1.21 loot tables, rolled when they yield): Zhenyuan sigils (3–4), a block of gold,
  crane plumes, talismans, a 25% ginseng fruit, a 12% flywhisk; the youths sigils, gold, talismans, a
  10% striker or tray. **Zhenyuan's Flywhisk** (拂尘, `FlywhiskItem`): +35% damage; used, it sweeps
  away what stands within 6 blocks before its bearer and brushes arrows and fireballs from the air
  (5 s rest). Disciples leave **Daoist Talismans**, cranes **Cloud Crane's Plumes**. A Silk-Lined Tray
  can also be made: three wool over three gold ingots.
- **XP and karma**: band 78–88, base grunt XP 145, stepped up from the Flaming Mountains' 130 (972 a
  grunt at the band's scale). The household is righteous: **killing a disciple −12** (captain or
  not); bringing a ripe fruit down without Zhenyuan's leave is **theft, −40** (as Heaven's peaches)
  and makes one the temple's thief for 5 minutes (every disciple, youth and the Great Immortal near
  turn on them); **passing his trial +10** (it is no killing); killing Zhenyuan outright −100, a
  youth −25; a fight with a crane is no deed either way.
- **Art**: `WuzhuangRenderers` (a robed frame for the disciples (topknot and pin), the youths (two
  buns of boys' hair, smaller) and Zhenyuan (wide sleeves, gold crown, white beard, his whisk; his
  left sleeve swells as he sweeps; a bow when they yield); a box model for the crane: long neck, red
  crown, wings spread and beating in flight, folded at rest, swept back in a swoop); skins from
  `assets_source/wuzhuang_folk.py`, `wuzhuang_crane.py`; the fruit, the tray, the striker, the whisk,
  plumes and talismans from `wuzhuang_items.py`.
- **Tests and pictures**: `WuzhuangGameTests` (heights and band, the stair's half steps and slow
  falling, the temple's blueprint, gold and the fruit, sinking, resting on wool, the tray, water,
  eating once, theft, creatures, captains, XP, talismans, swoops, the youths' arts and yielding, the
  sleeve held, struggled out of and cast out of, his sleeve in a live fight, his cap on blows and his
  disciples, his yielding and leave, the whisk, karma), `WuzhuangZoneTest` (only over the clouds,
  never on another zone's ground at its heights, the island within its region and over the clouds,
  level temple and road, the stair continuous); gallery scenes `-Pgallery.only=wuzhuang` (creatures,
  cranes in flight, the youths, Zhenyuan, the sleeve, his yielding, and the island laid north of the
  stage: gate, court, hall, garden, the fruit, the road, the stair, from below and from afar); the
  mountain in a real world with `./gradlew runWorldgenBench -Pbench.mode=wuzhuang`.

#### Lion-Camel Ridge (levels 89–100)

Lion-Camel Ridge (狮驼岭, chapters 74–77: eight hundred li of mountains swarming with demons, forty-seven
thousand little demons on the ridge alone) and Lion-Camel City (狮驼国), the walled kingdom the demons
took after eating its king, his court and all its people, where the Azure Lion and the White Elephant
hold court. The last band, to the levels' cap. In [`world/lion/`](src/main/java/com/jttw/engine/world/lion/)
and [`entity/lion/`](src/main/java/com/jttw/engine/entity/lion/).

- **Where** (`Zones.LION_CAMEL`): over mountain country (the Bottomless Cave's hosts: peaks, slopes,
  groves, meadows, cherry groves, windswept hills) and the rugged savanna highlands (plateaus,
  windswept savanna), host judged at y 256, **from y 40 up**
  (`Vertical.above(40)`). The Bottomless Cave takes the same mountains below y 40, so the two share no
  heights and may lie in the same columns: the ridge over the labyrinth (its shaft is left alone where
  it comes up through the ridge). It gives way to every zone before it that takes the surface. 90% of
  768-block cells roll a region about 380 blocks across; in the 32x32 km round spawn of seed 12345
  the layout rolls 1,624, 788 give way to earlier zones (Mount Wuzhuang's sky among them), 809 are
  not centred in mountain or highland country, and **29 regions** stand (0.29% of the ground, second
  only to the Flaming Mountains' 38; the nearest 0.9 km out; three of them over a Bottomless Cave).
- **The ridge** (`RidgeShape`, `LionCamelRidgeFeature`, pure and unit-tested): the mountains are broken
  into grim crags (ridged noise, up to 30 blocks more), climbing out of the land over the outer fifth;
  faces bare stone banded with tuff, andesite and deepslate; ground of coarse dirt, gravel, podzol and
  stone, a little yellowed grass, snow over y 196; skulls on stakes and bones about, great ribcages
  (`RidgeBonesFeature`, never on the city's ground) and dead shrubs. A grey sky and haze, a little ash
  in the air. Nothing under y 40 is touched.
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
  tiled roof fallen in at one corner) with the **three demon kings' thrones, the third, the Great
  Peng's, empty**; behind it the walled **elephant court**, ringed with tusks of ivory. Its court and
  garrison are put there once, each by the chunk its post lies in (`LionCamelCityFeature`), and kept
  (even on Peaceful): the Azure Lion on the terrace, the White Elephant in his court, lion-demon
  captains at the palace gate, lion- and peng-demons in the hall, elephant-demons at the main gate
  and in the court, a Little Wind-Driller at each gate.
- **Creatures** (`LionCamelMonster`: level 89, captains 96, any light, in numbers): **Lion-Demons**
  (狮子精, the Azure Lion's kin: 9 hits, sabre cuts of 22%; they pounce from 4–11 blocks for 26% and
  a stagger; strike one and the pride turns), **Elephant-Demons** (象精, the White Elephant's
  followers: 14 hits, armoured, blows of 30%; they charge, tusks down, goring for 26% and flinging
  aside), **Peng-Demons** (鹏精, the Great Peng's followers: 6 hits, spear thrusts of 16%; they beat
  up into the air and swoop for 24%, glide rather than fall). Captains as everywhere (8%, purple,
  2.5x health, with escorts: three, two, four).
- **The Little Wind-Driller** (小钻风, `LittleWindDrillerEntity`): the ridge's patrol demon in his
  pointed cap with the patrol token at his sash (the one Sun Wukong took to pass for him) and a
  clapper-bell. No fighter (3 hits, never a captain); but let him see a foe within 16 blocks, or be
  struck, and he **raises the alarm**: he rings his bell, every demon of the ridge within 32 blocks is
  set on the foe, three more come running (while fewer than 16 are about), and he runs off; then 30 s
  before he can raise it again. A Wind-Driller is posted at each of the city's gates.
- **The Azure Lion** (青狮, Manjushri's lion; `AzureLionEntity`, a `ZoneBoss`, level 100, 90 hits,
  an azure bar, on the palace terrace): broadsword blows of 26%. Every 9 s he throws back his head
  and **roars**: a cone 12 blocks long, 12%, Slowness II and Weakness, thrown back. Every 13 s, a foe
  within 4.5 blocks before him, his jaws gape for a second, and he **swallows the foe whole**: inside
  him the foe is unseen and in the dark and cannot get down; his belly takes 5% of a level-100
  character's health a second, armour or none. Blows struck from inside count half again, and once
  they come to 6% of his health he retches the foe up and reels for 2 s; else after 10 s he spits him
  out anyway. At two thirds he roars for his kin (three lion-demons); at a third he swells 35% larger,
  as he did to swallow Heaven's hosts, and swallows twice as often.
- **The White Elephant** (白象, Samantabhadra's six-tusked elephant; `WhiteElephantEntity`, level 98,
  85 hits, a white bar, in the elephant court): lance thrusts of 24%. Every 10 s, a foe within 6
  blocks before him, his trunk rears and **coils round the foe, lifting him high**: it crushes him
  every 0.75 s for 8% and after 3 s **hurls him away**; blows on the trunk from where one is held,
  once they come to 4% of his health, make him drop the foe and recoil (as Wukong got free by driving
  his staff up it). From two thirds he trumpets for his followers (two elephant-demons) and **stamps**
  (16%, thrown up, within 5 blocks); from a third he **charges** at foes 5–14 blocks off, goring for
  22%, and his trunk comes quicker. The two kings' lairs are 37 blocks apart: each is its own fight.
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
  kill XP's flat fallback for a tiered enemy outside every band stays, for Heaven's soldiers (they
  carry a tier but no zone, being levelled to whoever they hunt).
- **Art**: `LionDemonRenderer`, `ElephantDemonRenderer`, `PengDemonRenderer`,
  `LittleWindDrillerRenderer`, `AzureLionRenderer`, `WhiteElephantRenderer` (box models and skins with
  glowing eyes and gold from `assets_source/lion_camel.py`; the little demons bow, the Lion's jaw
  gapes, the Elephant's trunk sways, rears and coils); items from `assets_source/lion_items.py`.
- **Tests and pictures**: `LionCamelGameTests` (the Peng rule for every kind, both kings included,
  struck or not; swallowing, the belly, fighting out, being spat out; the trunk's lift, crush, hurl
  and breaking free; roar, stamp, charge, phases; the alarm; the creatures' tricks; captains; drops
  and the Peng's empty hands; the band's XP to the cap; karma; the city's blueprint),
  `LionCamelPlacementTest`; gallery scenes `-Pgallery.only=lion_`; the ridge and the city in a real
  world with `./gradlew runWorldgenBench -Pbench.mode=lion` (and every zone's share with `zones`).

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
