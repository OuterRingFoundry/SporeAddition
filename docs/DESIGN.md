# Design overview

[Documentation](README.md) · [Player rules and commands](PLAYER_GUIDE.md)

This overview describes **0.8.0**. The [early design record](history/DESIGN_0.2.md) is retained as history and is not the current behavior contract.

## World boundaries

The Blighted World has its own terrain, regional biomes, structures, and saved corruption index. External dimensions start dormant at -1; the Blighted World starts at 6. Overworld mushroom fields remain sanctuaries regardless of the dimension's index.

Positive indices grow from loaded active Spore populations. Nonpositive indices do not grow through population alone. A deliberate catalyst ritual can awaken -1 to 0, and Evolved Hivebound melee infection can raise 0. Purged index -2 requires an operator to change it.

Regional modifiers affect local pressure and creature strength; boss eligibility and Hive caps use the dimension-wide index. Growth does not require world-wide chunk loading.

## Persistence

Corruption, Hive admission and retirement, shared cairn activation, player return positions, network state, and survivor progression persist across reloads. Unloaded admitted hives still count toward population caps. Reducing a cap retires excess hives without drops; raising it later does not resurrect them.

Upgrades may inspect saved entity records for a one-time Hive census without activating chunks. New generation rules do not rewrite old chunks or attempt to restore unknown original blocks.

## Travel and abilities

Travel uses bounded safety searches instead of excavating a destination. The first successful survival entry activates and melts a cairn and charges one pearl; later entries at that structure still require a talisman but cost no pearl. The arrival cairn is already active. Return is free.

Hive network actions are server-validated against armor, evolution, location, proximity, destination safety, and cooldowns. Distant Hive markers use saved locations without keeping destination chunks loaded. Client controls and HUD preferences do not confer server-side permission.

## Integration boundaries

Spore remains an external dependency. Mixins enforce containment and connect native infection behavior to addon systems. Matching addon versions are required on clients and servers.

Civillis integration adjusts territory presentation while retaining the upstream civilization rules and HUD preferences. Optional dependencies and their validation scope are recorded in [compatibility](../compatibility/README.md).

For recipes, thresholds, keys, and commands, use the [player guide](PLAYER_GUIDE.md) and [0.8.0 release notes](RELEASE_0.8.0.md). For executable evidence, use [validation](VALIDATION.md).
