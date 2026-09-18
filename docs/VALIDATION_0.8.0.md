# Sporebound 0.8.0 validation

**Published:** [v0.8.0](https://github.com/OuterRingFoundry/SporeAddition/releases/tag/v0.8.0). The [main-branch workflow](https://github.com/OuterRingFoundry/SporeAddition/actions/runs/35350707276) passed validation and release verification for `24a29f5a3406e7f71da011df0c3d0df6887e5105`. The release JAR has the same SHA-256 recorded below.

The following records the preceding development-branch validation.

Validated code commit: `15cb61e5c6c0aaa7d8fe8c1ba73945f2870045b9` on `feat/living-frontier-0.8.0`.

[GitHub Actions run 35339186265](https://github.com/OuterRingFoundry/SporeAddition/actions/runs/35339186265) passed on 2026-09-18:

- Clean build and unit checks on Java 21.
- 386 fresh-world native checks.
- Saved entity-region NBT audit: restart fixture persisted with mass 5 and hunger 100.
- 14 checks after a full server restart.
- Real Minecraft client acceptance with the locked Civillis dependency and 20 required screenshots.
- JAR checksum generation and validation artifact upload.

[Download the validation artifact](https://github.com/OuterRingFoundry/SporeAddition/actions/runs/35339186265/artifacts/10544975079). It contains the JAR, checksum, reports, logs and screenshots. The artifact expires on 2026-12-17; this is a development build, not a main-branch release.

`sporebound-0.8.0.jar` SHA-256:

```
674640326b4f34a932be7861f0f813842a69e35ab190202c7d07b4e36970a93b
```

## Coverage

Native frontier checks cover Spore grass conversion, exposed/covered soil, custom mycelium spread containment, pale remnant preservation, distant unloaded Hive discovery/removal, affordable armor recipes with component retention, real climbing behavior, forged ability requests, survivor crafting/smelting and save/load, and infected survivor death becoming one equipped native Infected Adventurer without biomass or duplicated equipment.

Portal checks cover first-crossing payment and melting, re-entry with zero pearls, mandatory talisman, active corrupted-world arrival generation, obstruction handling, shared activation surviving restart, and the shapeless rotten-flesh/red-mushroom/netherrack Spore Catalyst recipe. Awakening confirms through the server ability handler and consumes one catalyst; missing catalysts and purged dimensions are rejected.

Real-client checks exercise the configured network key, network screen, an unloaded Hive marker 50,000 blocks away, and the travel key through a real serverbound packet. Screenshots include the infected armor, Hive controls, distant beacon and both melted portals.

The earlier client assertion used a general level chunk-presence API. The passing version queries the actual client chunk cache with fallback creation disabled. Restart acceptance now waits for entity loading and independently inspects saved NBT before restart; the expected entity data is unchanged.

## Development-run execution scope

During this development run, main had not yet been merged and the release publish job was skipped. Publication subsequently passed in the main-branch workflow linked above. This host lacks the Java, memory and disk capacity for the Minecraft checks. Resource JSON, Python syntax and whitespace were checked locally. The artifact download service returned HTTP 403 to this host, so screenshots were captured and checked for presence by CI but were not visually reviewed locally; use the GitHub artifact link above.

## Survivor squad follow-up

The cooperation/bow/shield follow-up passed in the run linked above. Native checks cover colony composition, ally alerts, real projectile ownership, ammunition and durability costs, friendly fire, shot obstruction by allies, sword fallback/resupply, food transfer, role persistence, directional shield protection, axe disabling, and replacement recipes. The real-client suite passed with a twentieth screenshot showing a drawing archer and blocking defender and verified synchronized use of both hands.
