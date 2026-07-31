# JTTW Engine

A Minecraft Forge 1.20.1 mod implementing a full MMORPG progression system rooted in *Journey to the
West* and Chinese mythology — four class archetypes, nine skill trees, an alignment-driven
reincarnation system with twelve evolution outcomes, tiered flight, custom dimensions, and
lore-accurate artifacts and abilities.

Roughly 27,000 lines of Java across 144 source files, written solo. No dependency on any external
powers or animation framework — the engine, the progression system, and the models are all
implemented directly.

---

## The RPG system

### Four class archetypes

Each class in [`ProgressionClass`](src/main/java/com/jttw/engine/progression/ProgressionClass.java)
carries a Base Stat Total and its own XP curve, so they scale differently rather than just having
different numbers:

| Class | BST | Growth curve | Design intent |
|---|---|---|---|
| **Primordial** | 1000 | Erratic | Punishing early game, dramatically outscales everything after level 50. A long-term investment. |
| **Divine Beast** | 900 | Medium-fast | Naturally strong and stable. +16% stats at each form phase (20/40/60/80/100). |
| **Yaoguai** | 750 | Fluctuating | Lower stat floor, strong transformation mechanics. Power comes from abilities, not raw stats. |
| **Heavenly** | 700 | Medium | Widest utility toolkit. Ability-expansion focused; higher forms gain perception and authority advantages. |

Stats resolve at runtime through
[`StatBlock.compute()`](src/main/java/com/jttw/engine/progression/StatBlock.java) from class and
level, rather than being hardcoded per form.

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
[`DivineBeastLine`](src/main/java/com/jttw/engine/progression/DivineBeastLine.java) elemental paths
grants a stat buff while you're in its domain:

- **Water** (Dragon King) — oceans, rivers, and during rain
- **Earth** (World Turtle) — below Y=0
- **Beast** (Byakko) — forests, jungles, plains
- **Fire** (Solar Sovereign) — deserts, taigas, savannahs

### Skill trees

Nine per-subclass trees — Wukong, Nezha, Erlang Shen, Yuan Di, Peng, Dragon, Phoenix, Turtle, Byakko
— built from [`SkillNode`](src/main/java/com/jttw/engine/progression/SkillNode.java) records with
prerequisites, skill-point cost, tier, branch grouping, and grid position.

The same node definitions drive three things: unlock tracking in `SkillTreeData`, rendering in
[`SkillTreeScreen`](src/main/java/com/jttw/engine/client/gui/SkillTreeScreen.java), and activation
behaviour via `AbilityType` (`MENU`, `TOGGLE`, `HELD`, `PASSIVE`). Adding an ability means adding a
record, not touching the UI or the input handling.

Levelling runs on a Qi resource system (`QiManager`), with `BiomeLevelBand` and `EnemyTier` scaling
the world to player level.

### Lore-accurate artifacts and abilities

Abilities and items are drawn from the source material rather than invented:

- **Ruyi Jingu Bang** — Wukong's staff, with the size-shifting behaviour it has in the novel
- **Wind Fire Wheels · Huntian Cloth · Fire Tip Spear · Qiankun Ring** — Nezha's arsenal
- **Three-Pointed Spear · Erlang Axe** — Erlang Shen, whose form is *Eye of Heaven* after his third eye
- **Immortal Peach · Yin Yang Sword**
- **Wu Clones** — Wukong's hair-clone technique, implemented as summoned entities with their own
  pathfinding goals ([`WuCloneManager`](src/main/java/com/jttw/engine/progression/WuCloneManager.java))
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
[`ClientFlightHandler`](src/main/java/com/jttw/engine/flight/ClientFlightHandler.java), written
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

18 packet types in [`network/`](src/main/java/com/jttw/engine/network/) handle this, most of them
one-way server→client syncs for a specific slice of state (level, Qi, cooldowns, skill tree, ring
state, transformation), plus client→server requests for actions the server must authorize.

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
├── progression/     4 classes, 9 skill trees, Qi, levelling, divine beast lines
├── reincarnation/   4-axis alignment tracking, 12 evolution outcomes
├── transformation/  72-entity transformation system + transmutation
├── mixin/           14 Mixin classes — bytecode instrumentation
├── flight/          custom flight, glide, and swim physics
├── network/         18 packet types for client–server state sync
├── dimension/       Heaven and Hell dimensions with custom worldgen
├── entity/          custom entities, projectiles, summons
├── item/            lore artifacts and weapons
├── client/          renderers, GUI screens, HUD overlays
└── commands/        /jttw command tree
```

Largest components: `TransformationAbilityHandler` (1,385 lines), `YuanDiAbilityHandler` (1,236),
`WuCloneManager` (874, including custom pathfinding goals).

---

## Building

Requires **JDK 17**.

```bash
./gradlew build          # jar lands in build/libs/
./gradlew runClient      # launch a dev client
```

> **Note:** the Gradle wrapper is not yet committed. Until it is, run `gradle wrapper` once with a
> local Gradle install (8.x), or open the project in an IDE with Gradle support and let it generate.

Then drop the built jar into `mods/` alongside Minecraft Forge 1.20.1 (47.4.10+).

### Commands

```
/jttw transform <entity_id>      transform into any Minecraft entity
/jttw transform <player> <id>    transform another player
/jttw demorph [player]           revert
/jttw status [player]            inspect current transformation state
```

---

## Known limitations

- Transformation grants appearance, hitbox, and eye height — not the entity's AI or native abilities.
- Animation fidelity for vanilla entities is bounded by what their existing models support.
- Some entities (particles, projectiles) don't render correctly when transformed into.

## License

All rights reserved. *Journey to the West* is public-domain source material; Minecraft and Forge are
the property of their respective owners, and no Mojang assets or source are redistributed here.
