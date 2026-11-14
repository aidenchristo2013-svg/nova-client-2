# Nova Client

A client-side Fabric mod for Minecraft 1.21.11, inspired by the feature set of Corz Client. All original code.

## Features
- **Menu**: press Right Shift in a world. Search box filters tools; click a tool to toggle it. Settings persist in `config/nova-client.properties`.
- **Player Info**: nearby players with distance and health.
- **Projectile Info**: nearby arrows, pearls, tridents with distance and speed.
- **Schematics**: reads `.schem` (Sponge v2/v3) and `.litematic` files from `<game dir>/nova-schematics/`. Shows size, material list and per-layer block counts, with a layer stepper.

- **Coordinates**: position, facing, dimension.
- **Session Info**: FPS and ping.
- **Visuals**: always-on HUD panel (Player/Projectile Info, Coordinates, Session Info, Builder status). Choose the corner, text colour and shadow in the menu. Hidden while the menu is open or with F1.
- **Builder** [RED FLAG] (off by default): places blocks from the loaded schematic within normal reach. Set the origin where you stand, then start/pause from the menu. Matches block type only (no stair/log rotation), uses blocks in your hotbar, and places against an existing neighbouring block. Many servers ban automated building, so check the rules first.

## Ban risk
Builder is marked **[RED FLAG]** in the menu because automated block placement is banned on many servers. The other tools only read information the game already gives your client, which servers generally allow, but rules differ per server, so check yours. Nothing here hides the mod from a server.

Everything except Builder only reads game state.

## Build
Needs JDK 21 and internet access for Gradle to fetch Minecraft and Fabric.

    gradle wrapper        # once, if you don't have gradlew
    ./gradlew build

The jar is in `build/libs/`. Drop it in your `mods` folder with Fabric Loader and Fabric API.

Version numbers in `gradle.properties` should be checked against https://fabricmc.net/develop

## Status
- `schematic/` is plain Java with no Minecraft dependency and is tested (Sponge v2/v3, litematic bit-packing, negative-size regions, corrupt files).
- The rest uses Minecraft's client classes, which change between versions. It has NOT been compiled. The most likely spots to need a small fix are `InputConstants.isKeyDown(...)` in `NovaClient`, `EditBox`/`Button` calls in `NovaScreen`, and the registry/identifier/hotbar calls in `BuilderModule` (`Identifier.parse`, `BuiltInRegistries.BLOCK.getValue`, `setSelectedSlot`), and the HUD registration calls in `HudOverlay`.

## Not included yet
- In-world ghost overlay of the schematic
- Block rotation/properties in Builder

Check the server rules before using any client mod on a multiplayer server.
