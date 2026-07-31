# JTTW Engine

A Minecraft Forge 1.20.1 mod implementing a *Journey to the West* transformation and progression
system — 72 entity transformations, tiered flight, skill trees, custom dimensions, and bespoke
entities. Roughly 27,000 lines of Java across 144 source files, written solo.

It started as an addon for [Palladium](https://www.curseforge.com/minecraft/mc-mods/palladium), a
powers framework. Working inside that framework meant accepting its model for how abilities,
movement, and rendering behave — and the behaviours I wanted didn't fit it. So I dropped the
dependency and reimplemented the parts I needed directly against Minecraft's own classes. This repo
is that rewrite. It has no runtime dependency on Palladium or any other powers framework.

---

## The interesting parts

If you're skimming this to see how it's built, these are the three problems worth reading about.

### 1. Overriding engine internals without forking

Minecraft's movement, collision, and rendering behaviour lives in classes that aren't designed to be
extended. The usual options are to fork the game's source (unmaintainable across versions) or to
work around the engine from the outside (fragile, and often impossible).

Instead this uses [SpongePowered Mixin](https://github.com/SpongePowered/Mixin) to instrument
bytecode at class-load time — 14 Mixin classes registered in
[`jttw_engine.mixins.json`](src/main/resources/jttw_engine.mixins.json), split across injectors,
accessors, and invokers.

The clearest example is phasing through blocks, which took two separate injections to get right:

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
because vanilla creative flight and elytra gliding both felt wrong for the movement I wanted.

It maintains a persistent velocity vector in player NBT rather than applying motion directly, which
is what makes the rest possible:

- **Turn-rate limiting.** Direction changes interpolate toward the input at a fixed rate per tick
  rather than snapping, so flight has weight instead of feeling like a free camera.
- **Momentum decay.** With no input, velocity decays by a constant factor per tick until it falls
  below a cutoff — and the same decay continues after flight ends, so landing reads as a continuous
  glide rather than an abrupt stop.
- **Speed tiers.** Glide-only (sinks, cannot gain altitude), full flight, and a top tier that also
  permits dimension travel — with per-character speed variation layered on top.
- **Independent velocity clamps** on vertical and horizontal axes, and a separate scale factor for
  strafing, so lateral drift doesn't match forward thrust.
- **A parallel swim system** with its own stored vector, distinct cruise and sprint speeds, and a
  different acceleration constant.

Every constant is a named field at the top of the file, because tuning movement is iterative and
magic numbers buried in the logic are miserable to adjust.

### 3. Keeping state consistent across clients

Transformation, progression, and ability state has to agree between the server and every client that
can see the player — otherwise your model desyncs from your hitbox, or a cooldown looks ready on one
screen and isn't on another.

18 packet types in [`network/`](src/main/java/com/jttw/engine/network/) handle this, most of them
one-way server→client syncs for a specific slice of state (level, Qi, cooldowns, skill tree, ring
state, transformation), plus client→server requests for actions the server must authorize.

State is persisted in three places deliberately —
server memory, player NBT, and a client-side mirror — so transformations survive logout, death, and
dimension changes.

---

## Models and animation

**Every 3D model, animation, and texture in this project was made by me in
[Blockbench](https://www.blockbench.net/).** The repo includes the source `.bbmodel` project files
alongside their exports, in [`assets_source/`](assets_source/):

| Asset | Source file |
|---|---|
| Ruyi Jingu Bang (staff) | `Staff.bbmodel` |
| Celestial block | `celestial_block.bbmodel` |
| Immortal peach | `immortal_peach.bbmodel` |
| Xiaotian Quan (Erlang Shen's hound) | `xiaotian_quan.bbmodel` |

Plus 18 textures and the item/block model and blockstate JSONs under
[`src/main/resources/assets/`](src/main/resources/assets/).

The rigging and animation code is hand-written —
[`XiaotianQuanModel`](src/main/java/com/jttw/engine/client/model/XiaotianQuanModel.java) and
[`XiaotianQuanAnimations`](src/main/java/com/jttw/engine/client/model/XiaotianQuanAnimations.java)
drive the entity directly. There's no GeckoLib or other animation library in `build.gradle`.

---

## Layout

```
src/main/java/com/jttw/engine/
├── mixin/           14 Mixin classes — bytecode instrumentation
├── flight/          custom flight, glide, and swim physics
├── network/         18 packet types for client–server state sync
├── progression/     leveling, Qi, skill trees, per-character ability handlers
├── transformation/  72-entity transformation system + transmutation
├── reincarnation/   alignment tracking and reincarnation outcomes
├── dimension/       custom Heaven and Hell dimensions with worldgen
├── entity/          custom entities, projectiles, summons
├── item/            artifacts and weapons
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
