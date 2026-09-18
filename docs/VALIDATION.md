# Validation and known limits

[Documentation](README.md) · [Current release](https://github.com/OuterRingFoundry/SporeAddition/releases/tag/v0.8.0)

## Published 0.8.0

The [main-branch release workflow](https://github.com/OuterRingFoundry/SporeAddition/actions/runs/35350707276) passed on **2026-09-18** for commit `24a29f5a3406e7f71da011df0c3d0df6887e5105`.

| Stage | Result |
| --- | --- |
| Clean Java 21 build and unit tests | Passed |
| Fresh-world server acceptance | Passed |
| Saved entity NBT audit and full-process restart checks | Passed |
| Real Minecraft client acceptance with pinned Civillis | Passed |
| JAR checksum and validation artifact upload | Passed |
| Release publication and downloaded-asset comparison | Passed |

The matching development code passed **386 fresh-world checks**, **14 restart checks**, and client acceptance with **20 required screenshots**. [Detailed coverage and development evidence](VALIDATION_0.8.0.md)

The published JAR matches the previously validated development artifact:

```text
674640326b4f34a932be7861f0f813842a69e35ab190202c7d07b4e36970a93b
```

Use the JAR and checksum on [the release page](https://github.com/OuterRingFoundry/SporeAddition/releases/tag/v0.8.0). CI artifacts include test evidence but have retention limits; they are not the permanent download location.

## What this establishes

The fixtures exercise containment, progression, travel, Hive persistence, survivor behavior, client controls, rendering assertions, and screenshot capture. Restart acceptance uses a new server process and independently checks saved entity data.

## Limits

- This is not a long-duration multiplayer, exhaustive world-seed, or GPU-performance benchmark.
- Compatibility applies to [the recorded artifacts](../compatibility/README.md). The 0.8.0 client test includes Civillis; older combined ConcentricWorld results do not certify every current combination.
- Screenshot capture and required-image presence checks passed. The development report records that those images were not manually reviewed on the local host.
- New terrain appears in new chunks. Existing overwritten blocks, damaged structures, and old landscapes are not reconstructed.
- Passing gameplay checks do not imply warning-free upstream logs; optional Spore recipe/model warnings and headless audio limitations are recorded in historical reports.

## Historical evidence

[0.7.0](VALIDATION_0.7.0.md) · [0.2–0.4](history/VALIDATION_0.2-0.4.md) · [0.4 biomass](INFECTED_BIOMASS.md)
