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
that would touch an earlier one's (`ZoneMap`). White Bone Ridge is the first.

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

### The Mystical Waters (levels 47-56)

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
