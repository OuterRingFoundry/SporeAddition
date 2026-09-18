# Compatibility

[Installation](../docs/INSTALLATION.md) · [Validation](../docs/VALIDATION.md)

## Required runtime

Sporebound 0.8.0 targets Minecraft **1.21.1**, Java **21**, NeoForge **21.1.249**, and **Fungal Infection: Spore 2.2.0j** ([file 8342823](https://www.curseforge.com/minecraft/mc-mods/fungal-infection-spore/files/8342823)).

The addon metadata permits NeoForge versions from 21.1.249 up to, but excluding, 21.2; the exact recorded test version is 21.1.249. Spore is pinned to 2.2.0j. Broader version compatibility is not implied.

## Optional integrations

| Mod | Pinned artifact | Evidence |
| --- | --- | --- |
| [Civillis](https://modrinth.com/mod/civillis) | **2.0.1-release**, NeoForge 1.21.1 | Included in the 0.8.0 real-client acceptance run |
| [ConcentricWorld](https://github.com/OuterRingFoundry/ModDevConcWorld/releases/tag/v0.31.1-neoforge-1.21.1) | **0.31.1-neoforge-1.21.1**, structure-fix artifact | Earlier combined fixtures; not a fresh 0.8.0 combined certification |

Civillis receives corruption-aware territory labels while retaining its civilization and spawning rules. Core Sporebound does not require Civillis or ConcentricWorld.

Exact filenames, SHA-256 hashes, and available download URLs are in [artifacts.json](artifacts.json). The [Civillis lockfile](civilis.lock.json) records the client fixture's exact download and upstream hashes.

## Developer fixtures

Place the pinned optional JARs in `compatibility/mods/` for the combined fixture, then use the commands in [development](../docs/DEVELOPMENT.md#acceptance-fixtures). This directory is ignored by Git.

Dependencies are obtained separately and retain their upstream licenses. Do not add them to the source repository or the distributed Sporebound JAR.
