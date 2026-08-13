[English](README_EN.md) | [简体中文](README.md)
# Touhou Little Maid × Avaritia: Reborn (Addon)

![icon](src/main/resources/icon.png)

> A small addon that lets Touhou Little Maid maids actually wield the weapons and armor of Avaritia: Reborn.

| Item | Value |
|---|---|
| Current Version | 0.1.0 |
| Loader | Forge 1.20.1 (47.1.3+); NeoForge 1.20.1 |
| Java | 17 |
| License | MIT |

---

## Features

### Melee
- **Infinity Sword**: Trigger Infinity Sword effects on melee (instant kill + AOE); damage is routed through the `MaidHurtTarget.Pre` event with `setCanceled(true)`.
- **8-block melee range**: A Mixin extends the maid's melee range so sword-type weapons work at greater distance.

### Ranged
- **Infinity Bow**: Fires **arrow barrages**; with **Tracking Mode** enabled, fires **homing arrows**.
- **Infinity Crossbow**: Fires **arrow barrages**; with **Multi-Shot Mode** enabled, fires a **5-arrow spread** in a single shot.
- **Infinity Trident**: Can properly trigger **lightning strikes**.

### Weapon Mode Switch GUI
- A dedicated "Weapon Attack Mode Switch" task settings GUI for the bow/crossbow, used to toggle Tracking / Multi-Shot modes.
- Mode data is written to the weapon's NBT as `{mode:{...}}`, consistent with the vanilla Avaritia mode system.

### Infinity Armor – Maid Passive Adaptation
- **Helmet**: Night vision (configurable) + Water breathing
- **Chestplate**: Continuously removes negative effects
- **Leggings**: Fire resistance
- **Boots**: Step height 1.0625 (can walk up 1 block) + movement speed bonus (implemented via a `MOVEMENT_SPEED` attribute modifier; takes effect immediately on equip/unequip without interfering with the maid's AI pathfinding)
- Movement speed / swimming / sprinting multipliers are all configurable.

---

## Requirements

| Item | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.1.3+ |
| Java | 17 |
| Touhou Little Maid | 1.5.3 (Forge 1.20.1) |
| Avaritia: Reborn | 1.4.0+ (Forge 1.20.1) |

> Both the client and the server must have this mod and both prerequisites installed. **If any prerequisite is missing, Forge will raise an error during startup and refuse to load this mod.**

> **Regarding Avaritia compatibility**: This mod is **adapted specifically for the "Avaritia: Reborn" branch only**, and does not represent compatibility with other Avaritia branches.

---

## Loader & Version Compatibility

| Loader | Status |
|---|---|
| Forge 1.20.1 (47.1.3+) | ✅ Fully supported |
| NeoForge 1.20.1 | ✅ Verified working on **47.1.106** |
| NeoForge (other versions) | ⚠️ Untested; NeoForge can load some Forge mods natively, so it will likely work |

---

## Installation

1. Install Minecraft 1.20.1 + Forge 47.1.3+ (NeoForge 1.20.1 also works; see "Loader & Version Compatibility")
2. Install the prerequisites: Touhou Little Maid 1.5.3, Avaritia: Reborn 1.4.0+
3. Place this mod's jar into the `mods/` folder
4. Launch the game

---

## Configuration

The config file is located at `config/tml_avaritia_addon-common.toml`. Main options:

| Key | Default | Description |
|---|---|---|
| `maid_sword_aoe` | `true` | Whether Infinity Sword attacks deal area damage (maid version of the sweep attack) |
| `force_kill_mode` | `false` | Force the maid's Infinity Sword into kill/execute mode (off by default) |
| `maid_melee_reach` | `8.0` | Melee reach of the maid when wielding Infinity weapons (blocks, 3–16) |
| `infinity_armor_night_vision` | `true` | Infinity Armor helmet: night vision |
| `boot_speed_base` | `0.1` | Infinity boots speed (≈ 2× the maid's base movement speed, same multiplier as a player wearing the boots) |
| `boot_speed_swimming_multiplier` | `1.2` | Boots swimming multiplier |
| `boot_speed_sprinting_multiplier` | `0.2` | Boots sprinting multiplier (TLM maids don't sprint by default, so this stays dormant) |

> `boot_speed_flying/sneaking/backward/strafing_multiplier` are kept only to mirror Re-Avaritia's config; they are currently unused.

---

## Building from Source

Requirements: JDK 17

```bash
# 1. Put the jars of the two prerequisite mods into libs/ (file names must match the flatDir coordinates in build.gradle):
#    - touhoulittlemaid-1.5.3-forge-mc1.20.1.jar
#    - Re-Avaritia-forge-1.20.1-1.4.0-release.jar
#    (libs/ is gitignored and not committed; GitHub Actions downloads them automatically from the upstream Releases)

./gradlew build        # Build output in build/libs/
./gradlew runClient    # Launch the dev client
```

---

## Known Limitations

- **Thrown tridents are not retrieved**: An Infinity Trident thrown by a maid won't auto-return like a player's (`loyalty` doesn't work for maids); it sticks into the ground on hit.
- **Some player-only effects don't apply to maids** (e.g., the trident's Conduit Power mode, etc.).
- **The sprint multiplier is wired into the logic**, but TLM maid AI never sets sprinting, so this multiplier is dormant by default.

---

## License & Credits

This mod's code is released under the **MIT License**, Copyright © 2026 qinmadara. See [LICENSE](LICENSE).

This mod depends on and references the following open-source projects:

| Project | License | Copyright |
|---|---|---|
| [Touhou Little Maid](https://github.com/tartaricacid/TouhouLittleMaid) | Code: MIT / Assets: CC BY-NC-SA 4.0 | © tartaric_acid |
| [Avaritia: Reborn](https://github.com/Nova-Committee/Avaritia) | Code: MIT / Assets: CC BY-NC-SA 4.0 | © cnlimiter (Nova Committee) |

- This mod only references the public APIs of the above mods at compile/runtime time and **does not bundle any of their code or assets**.
- This mod is an **unofficial addon** and is not affiliated with the authors of Touhou Little Maid or Avaritia: Reborn.

---

## AI Disclosure

- This mod was developed with **extensive AI assistance**, including: code generation and refactoring, bug triage, code review, and documentation (including this README).
- All AI-generated code was **reviewed, revised, and tested in-game by a human** before being integrated; the final code is released under the MIT License.
- AI is used to improve development efficiency; code quality, functional correctness, and license compliance are the responsibility of the project author.

---

## Disclaimer

This mod is not affiliated with Mojang, the Touhou Project, or the official teams of the mods mentioned above. Mojang's official mapping names are governed by the license in [Mojang.md](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md).
