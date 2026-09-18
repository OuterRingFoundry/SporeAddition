# Installation

[Documentation](README.md) · [Player guide](PLAYER_GUIDE.md)

## Required versions

Use Minecraft **Java Edition 1.21.1**, **Java 21**, and **NeoForge 21.1.249**, the loader version used for validation. Obtain the installer from [NeoForged](https://neoforged.net/).

Download these separately:

- [Fungal Infection: Spore 2.2.0j for NeoForge 1.21.1](https://www.curseforge.com/minecraft/mc-mods/fungal-infection-spore/files/8342823), file **8342823**.
- [Sporebound 0.8.0](https://github.com/OuterRingFoundry/SporeAddition/releases/tag/v0.8.0): `sporebound-0.8.0.jar` and its `.sha256` file.

The pinned Spore version matters: this addon integrates with Spore's infection internals. Fabric, Forge, other Minecraft versions, and other Spore versions are not validated by this release.

## Singleplayer

1. Install and launch the matching NeoForge profile once.
2. Close Minecraft and put both mod JARs into that profile's `mods/` folder.
3. Keep only one Sporebound version in the folder.
4. Launch the NeoForge profile and confirm Sporebound appears in the mod list.
5. Follow the [first-trip instructions](PLAYER_GUIDE.md#cairn-travel).

GitHub's **Source code** ZIP and tarball are for development; install the JAR listed under release assets.

## Multiplayer

Install matching Sporebound and Spore JARs on **the server and every client**. Use the same Minecraft and NeoForge versions across the group. A server-only installation is not supported.

Optional integrations and their exact artifacts are listed in [compatibility](../compatibility/README.md). Neither optional mod is needed to play the core addon.

## Verify the download

Place the JAR and checksum in the same directory. On Linux:

```sh
sha256sum --check sporebound-0.8.0.jar.sha256
```

On macOS:

```sh
shasum -a 256 -c sporebound-0.8.0.jar.sha256
```

On Windows PowerShell:

```powershell
Get-FileHash .\sporebound-0.8.0.jar -Algorithm SHA256
Get-Content .\sporebound-0.8.0.jar.sha256
```

Compare the PowerShell hash with the first field of the checksum file; letter case does not matter. The published 0.8.0 JAR has SHA-256:

```text
674640326b4f34a932be7861f0f813842a69e35ab190202c7d07b4e36970a93b
```

## Upgrade an existing world

Back up the world and stop the game/server before changing JARs. Replace the old Sporebound version on both sides.

New terrain, vegetation, and structure-generation changes apply to **new chunks**. Existing chunks are not regenerated, and previously overwritten blocks are not reconstructed. New infection and adjacent mycelium spread can still change eligible exposed soil.

Old saves may receive a one-time Hive census that reads saved entity records without loading their chunks. Existing hives remain subject to population caps. [Player guide: Hive population](PLAYER_GUIDE.md#hive-population)

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Missing dependency or startup failure | Exact Minecraft, NeoForge, Java, and Spore versions; remove duplicate addon JARs. |
| Cannot join a server | Both sides must have matching Sporebound and Spore versions. |
| Cairn will not activate | Complete 3×3 layout, three clear blocks above, talisman, first-use pearl, and no Overworld mushroom sanctuary. |
| Hive keys do nothing | Check **Options → Controls → Sporebound**, armor/evolution requirements, destination safety, and cooldowns. |
| New terrain is absent | Explore newly generated chunks; the update does not rebuild old terrain. |
| A mod combination fails | Reproduce with the required mods only in a disposable world, then include the full mod list in a report. |

Still stuck? [Open an issue](https://github.com/OuterRingFoundry/SporeAddition/issues/new/choose) with versions, reproduction steps, and the relevant log or crash report. See [support guidance](../SUPPORT.md).
