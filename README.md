# VSIA

VSIA is a Minecraft Forge mod that aims to provide the complete collection of equipment, weapons, sensors, and supporting systems an infantry unit may need.

Rather than treating firearms, protective equipment, night vision, communications, and battlefield awareness as unrelated features, VSIA is being developed as one connected infantry platform. Its long-term goal is to support individual soldiers, organized squads, and larger combined-arms environments through consistent mechanics and interoperable equipment.

## Project vision

VSIA aims to bring infantry capabilities into one extensible mod, including:

- Individual weapons and ammunition
- Weapon attachments and configurable fire modes
- Helmets, armor, camouflage, and load-bearing equipment
- Night-vision and other optical systems
- Communications and electronic-signal systems
- Radar, detection, and battlefield-awareness tools
- Infantry utilities, deployable equipment, and support assets
- Compatibility with vehicle and physics mods where appropriate

The project is under active development. Not every planned system is currently available, and existing mechanics may change as the common framework is improved.

## Currently implemented

### Weapons

- Animated 3D firearm models powered by GeckoLib
- Single, burst, and automatic fire-mode support
- Server-authoritative ammunition, firing, hit detection, and reloading
- Weapon-specific damage, range, recoil, accuracy, RPM, and magazine capacity
- First-person aiming-down-sights positioning and camera zoom
- Fire, reload, draw, and inspect animation support
- Ammunition compatibility and inventory consumption
- Attachment slots with stat modifiers
- Configurable weapon sounds and HUD feedback

The current weapon set includes the AK-74SU VSOP and its compatible 5.45x39 mm ammunition. Additional weapons will be introduced as their models, animations, sounds, and gameplay behavior reach the required quality.

### Tactical equipment

- Tactical helmet variants
- Ghillie, sand, and snow camouflage variants
- PVS-31 night-vision helmet variants
- GPNVG-18 night-vision helmet variants
- Toggleable night vision
- Selectable night-vision display colors
- Custom 3D equipment rendering

### Signals and detection

VSIA includes an embedded Signality subsystem for radar and electronic-signal simulation. It provides foundations for:

- Radar emitters and targets
- Radar cross-section calculations
- Signal transmission and reception
- Path-loss and occlusion calculations
- Search, scanning, tracking, and pulse-Doppler radar behavior
- Asynchronous radar scanning designed to reduce server-tick impact
- Optional Valkyrien Skies ship detection
- Debug commands and visualization tools for development

Some Signality blocks and tools are currently intended for testing and API development rather than finished survival gameplay.

## Controls

Default controls can be changed from Minecraft's Controls menu.

| Action | Default input |
|---|---:|
| Fire weapon | Left mouse button |
| Reload | `R` |
| Inspect weapon | `F` |
| Change fire mode | `X` |
| Toggle night vision | Configurable in Controls |
| Change night-vision color | Configurable in Controls |

## Requirements

- Minecraft 1.20.1
- Minecraft Forge 47.x
- Java 17
- GeckoLib 4.4.7

Valkyrien Skies 2 is optional. When available, VSIA can use it for ship-aware radar and signal integration. Development runs may load VS2 through Gradle without installing it in a normal Minecraft instance.

## Development setup

Clone or extract the project, open it as a Gradle project, and use Java 17.

Build the mod:

```powershell
.\gradlew clean build
```

Launch the isolated Forge development client:

```powershell
.\gradlew runClient
```

Launch the isolated development server:

```powershell
.\gradlew runServer
```

Generated JAR files are placed in:

```text
build/libs
```

The development client uses the project's `run` directory and does not modify a normal Minecraft launcher installation.

## Project structure

The main systems are organized under:

```text
src/main/java/com/k1ngtle/vsia
```

Important packages include:

- `weapon` — firearms, ammunition, attachments, networking, rendering, and client feedback
- `registry` — equipment and creative-tab registration
- `network` — night-vision synchronization
- `signality` — radar, signals, occlusion, scanning, and optional integrations
- `mixin` — narrowly scoped Minecraft and compatibility hooks

Resources such as models, animations, textures, sounds, and translations are located under:

```text
src/main/resources/assets/vsia
```

## Design principles

- Server authority for gameplay-critical actions
- Data-driven models, animation, sound, and equipment presentation
- Modular systems that can support multiple weapons and equipment families
- First-person quality without unnecessary third-person animation artifacts
- Compatibility with larger military and combined-arms mod environments
- Performance-conscious scanning, networking, and visual effects

## Status

VSIA is an in-development project. Expect incomplete assets, balance changes, API changes, and temporary debugging content. Back up important worlds before testing development builds.

## License

All rights reserved unless a different license is explicitly provided with a specific dependency or source component. Third-party libraries and integrations remain subject to their respective licenses.
