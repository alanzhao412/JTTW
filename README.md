# JTTW Engine - 72 Transformations System

This is the **core Forge mod** that provides advanced functionality that Palladium alone cannot handle.

## What This Does

The JTTW Engine provides:
- **72 Transformations** - Transform into any Minecraft entity
- **Automatic eye level adjustment** - Your view height changes based on entity size
- **Automatic hitbox scaling** - Your collision box changes to match the entity
- **Model rendering** - You actually look like the entity you transform into
- **Network synchronization** - Other players see your transformation
- **Persistent transformations** - Transformations survive logout/death

## How It Works

### Architecture

```
Player transforms → Command executed → TransformationManager
                                              ↓
                                    Applies transformations:
                                    - Eye height (via Mixin)
                                    - Scale attribute
                                    - Model rendering (via Mixin)
                                              ↓
                                    Syncs to all clients
                                              ↓
                                    Other players see you as the entity
```

### Key Components

1. **TransformationManager** - Core logic for transformations
2. **TransformationData** - Stores entity type, dimensions, eye height
3. **NetworkHandler** - Syncs transformation state
4. **PlayerMixin** - Adjusts eye height
5. **PlayerRendererMixin** - Renders entity model instead of player
6. **TransformCommand** - `/jttw transform` and `/jttw demorph` commands

## Commands

### Transform Into Entity
```
/jttw transform <entity_id>
```
Examples:
- `/jttw transform minecraft:zombie`
- `/jttw transform minecraft:enderman`
- `/jttw transform minecraft:chicken`
- `/jttw transform minecraft:ender_dragon`

### Transform Another Player
```
/jttw transform <player> <entity_id>
```
Example:
- `/jttw transform Steve minecraft:creeper`

### Return to Normal
```
/jttw demorph
```

### Return Another Player to Normal
```
/jttw demorph <player>
```

### Check Transformation Status
```
/jttw status
/jttw status <player>
```

## Integration with Palladium Powers

In your `j_t_t_w` addon power JSONs, you can call these commands:

```json
{
  "seventy_two_transformations_zombie": {
    "type": "palladium:command",
    "title": "Transform: Zombie",
    "hidden": false,
    "hidden_in_bar": false,
    "key_bind": "key.wukong.transform_zombie",
    "cooldown": 300,
    "description": "Transform into a zombie",
    "commands": [
      "jttw transform minecraft:zombie"
    ]
  },
  
  "seventy_two_transformations_enderman": {
    "type": "palladium:command",
    "title": "Transform: Enderman",
    "hidden": false,
    "hidden_in_bar": false,
    "key_bind": "key.wukong.transform_enderman",
    "cooldown": 300,
    "description": "Transform into an enderman",
    "commands": [
      "jttw transform minecraft:enderman"
    ]
  },
  
  "revert_transformation": {
    "type": "palladium:command",
    "title": "Revert Form",
    "hidden": false,
    "hidden_in_bar": false,
    "key_bind": "key.wukong.demorph",
    "description": "Return to normal form",
    "commands": [
      "jttw demorph"
    ]
  }
}
```

## Building the Mod

### Prerequisites
- JDK 17
- Gradle (wrapper included)

### Build Steps

1. **Navigate to directory**:
   ```bash
   cd jttw_engine_custom
   ```

2. **Run Gradle setup** (first time only):
   ```bash
   ./gradlew genIntellijRuns
   # or on Windows:
   gradlew.bat genIntellijRuns
   ```

3. **Build the mod**:
   ```bash
   ./gradlew build
   ```

4. **Find the JAR**:
   ```
   build/libs/jttw_engine-1.0.0.jar
   ```

## Installation

1. Install Minecraft Forge 1.20.1 (version 47.4.10+)
2. Place `jttw_engine-1.0.0.jar` in your `mods` folder
3. (Optional) Install `j_t_t_w` Palladium addon for the full class system

## Technical Details

### Eye Height Adjustment

The `PlayerMixin` intercepts the `getStandingEyeHeight` method and returns the transformed entity's eye height stored in persistent NBT data.

```java
@Inject(method = "getStandingEyeHeight", at = @At("RETURN"), cancellable = true)
private void jttw_adjustEyeHeight(...) {
    if (data.contains("jttw_morph_eye_height")) {
        cir.setReturnValue(data.getFloat("jttw_morph_eye_height"));
    }
}
```

### Model Rendering

The `PlayerRendererMixin` intercepts player rendering and instead:
1. Creates a dummy entity of the transformed type
2. Copies player position/rotation to the dummy
3. Uses the entity's renderer to draw it
4. Cancels normal player rendering

```java
@Inject(method = "render", at = @At("HEAD"), cancellable = true)
private void jttw_renderMorph(...) {
    if (transformed) {
        // Render entity model
        // Cancel player rendering
    }
}
```

### Networking

Transformations are synchronized using Forge's networking:
- Server → Client: When transformation changes
- Server → All tracking clients: So nearby players see the transformation

```java
NetworkHandler.sendToPlayer(new SyncTransformationPacket(uuid, data), player);
NetworkHandler.sendToTrackingPlayers(new SyncTransformationPacket(uuid, data), player);
```

### Persistence

Transformation data is stored in:
1. **Server memory** - `TransformationManager.TRANSFORMATIONS` map
2. **Player NBT** - `jttw_transformation` tag
3. **Client memory** - `TransformationManager.CLIENT_TRANSFORMATIONS` map

This ensures transformations persist across:
- Logout/login
- Death/respawn
- Dimension changes

## Supported Entities

Works with **ALL** Minecraft entities:
- Hostile mobs (zombie, skeleton, creeper, etc.)
- Passive mobs (cow, pig, chicken, etc.)
- Neutral mobs (enderman, spider, etc.)
- Boss mobs (ender dragon, wither)
- Aquatic mobs (dolphin, squid, fish)
- Flying mobs (bat, parrot, phantom)
- Modded entities (if the mod adds them properly)

## Known Limitations

1. **Entity AI** - You don't get the entity's AI/behavior, just appearance
2. **Special abilities** - You don't get entity abilities (creeper explosion, enderman teleport, etc.)
3. **Animations** - Limited to what the entity's model supports
4. **Inventory** - Player inventory remains accessible

## Troubleshooting

### Transformation not visible to other players
- Check that networking is initialized: Look for "Network packets registered" in logs
- Ensure both client and server have the mod

### Eye height not changing
- Verify mixin is loading: Look for mixin logs during startup
- Check that `jttw_engine.mixins.json` is in resources

### Model not rendering
- Check client logs for rendering errors
- Some entities may not render correctly (particles, projectiles, etc.)

### Game crashes when transforming
- Check entity ID is valid: `/jttw transform minecraft:invalid_entity` should fail gracefully
- Look for `ClassCastException` in crash logs

## Debugging

Enable debug logging in your JVM arguments:
```
-Dmixin.debug.verbose=true
-Dmixin.debug.export=true
```

Check transformation state:
```
/jttw status
```

## Future Enhancements

Possible additions:
- GUI for selecting transformations
- Entity ability inheritance (optional)
- Transformation whitelist/blacklist
- Cooldown system
- Transformation duration limits
- Custom entity models beyond vanilla

## Credits

- Built with Minecraft Forge
- Uses Mixin for bytecode modification
- Network sync via Forge networking

## License

All Rights Reserved
