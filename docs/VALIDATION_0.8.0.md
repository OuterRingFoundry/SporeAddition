# Sporebound 0.8.0 validation

Validated code commit: `5e27a833908ecb00a2840342b5423e7b6b78fc02` on `feat/living-frontier-0.8.0`.

[GitHub Actions run 35336689768](https://github.com/OuterRingFoundry/SporeAddition/actions/runs/35336689768) passed on 2026-09-18:

- Clean build and unit checks on Java 21.
- 368 fresh-world native checks.
- Saved entity-region NBT audit: restart fixture persisted with mass 5 and hunger 100.
- 14 checks after a full server restart.
- Real Minecraft client acceptance with the locked Civillis dependency and 19 required screenshots.
- JAR checksum generation and validation artifact upload.

[Download the validation artifact](https://github.com/OuterRingFoundry/SporeAddition/actions/runs/35336689768/artifacts/10543596296). It contains the JAR, checksum, reports, logs and screenshots. The artifact expires on 2026-12-17; this is a development build, not a main-branch release.

`sporebound-0.8.0.jar` SHA-256:

```
8e02bf299ea0286edef63dd5e455fadb16baec316d5651bbd30f40a09cef4196
```

## Coverage

Native frontier checks cover Spore grass conversion, exposed/covered soil, custom mycelium spread containment, pale remnant preservation, distant unloaded Hive discovery/removal, affordable armor recipes with component retention, real climbing behavior, forged ability requests, survivor crafting/smelting and save/load, and infected survivor death becoming one equipped native Infected Adventurer without biomass or duplicated equipment.

Portal checks cover first-crossing payment and melting, re-entry with zero pearls, mandatory talisman, active corrupted-world arrival generation, obstruction handling, shared activation surviving restart, and the shapeless rotten-flesh/red-mushroom/netherrack Spore Catalyst recipe. Awakening confirms through the server ability handler and consumes one catalyst; missing catalysts and purged dimensions are rejected.

Real-client checks exercise the configured network key, network screen, an unloaded Hive marker 50,000 blocks away, and the travel key through a real serverbound packet. Screenshots include the infected armor, Hive controls, distant beacon and both melted portals.

The earlier client assertion used a general level chunk-presence API. The passing version queries the actual client chunk cache with fallback creation disabled. Restart acceptance now waits for entity loading and independently inspects saved NBT before restart; the expected entity data is unchanged.

## Execution scope

The user explicitly approved development-branch upload and GitHub Actions testing. Main was not merged and the release publish job was skipped. This host lacks the Java, memory and disk capacity for the Minecraft checks. Resource JSON, Python syntax and whitespace were checked locally. The artifact download service returned HTTP 403 to this host, so screenshots were captured and checked for presence by CI but were not visually reviewed locally; use the GitHub artifact link above.
