# Sporebound: The Blighted World

Independent companion to Fungal Infection: Spore, Minecraft 1.21.1 / NeoForge.

The Blighted World uses a dedicated data-driven density field: landmass noise plus
vertical falloff and three-dimensional folds. Sea level is 64; build limits and
normal ores/caves remain Overworld-like. A second regional noise partitions intact
Remnant Groves from corrupted Wilds, Drowned Hollows and Ribbed Highlands. Surface
rules use grass/dirt in groves, mycelium in wilds, mud/clay in hollows and
calcite/andesite in highlands. Small calcite arches are a dimension-guarded feature.
No global vanilla worldgen registry is replaced.

Each dimension owns a saved decimal index. Overworld, Nether, End and other
external dimensions start at -1. The Blighted World starts at 6; maximum is 10.

| Index | Rules |
|---|---|
| -2 | Purged: reject incoming Spore entities, discard loaded Spore entities without death rewards, prevent infection and Spore block placement/spread. No automatic index changes. |
| -1 | Dormant: no new Spore entities or infection/spread. Existing ordinary mobs remain inert; bosses are excluded. |
| 0 | Contained: ordinary Spore activity is allowed, but population cannot increase the index. |
| 0 < value < 5 | Active infection; natural growth. Hive Minds and all Calamities are excluded. |
| 5–10 | Hive Minds and Calamities allowed; increasing combat strength. |

Operators may explicitly change every state, including unlocking -2. Gameplay
never changes -2, -1, or 0. Changes take effect immediately and survive restarts.
Lowering below 5 removes existing bosses without loot, including on chunk load.

Growth samples loaded, active Spore populations once per minute. Basic mobs count
1, evolved mobs 4, hyper mobs 8, bosses 20. Rate is min(0.10, weight × 0.0002)
index points per minute. Unloaded mobs and offline time give no growth. This
avoids loading the world to count mobs and bounds farm-driven acceleration.
Health bonus is +15% and attack damage +10% per index point, continuously scaled.
Modifiers have stable IDs: reloading, traveling or changing index cannot stack them.

One founding Hive Mind is seeded near (512, 0). Its slot is saved once and never
replenished after defeat. Founding waits for index >=5 and non-Peaceful difficulty.
Each dimension maintains an ordered saved census of admitted Hive Mind UUIDs.
Limits are 0 below 5, 1 below 6, 2 below 7, 3 below 8, and unlimited from 8.
Admission is checked after cancellable join events, immediately before the entity
manager accepts the entity; only successful admission adds a UUID. Unloading retains
that UUID. Death/discard or changing dimensions releases it. On a reduced cap, newer
excess UUIDs are retired and their entities are discarded without rewards. Retirement
persists even across a subsequent increase, preventing dormant chunks bypassing caps.
A one-time upgrade scan reads existing entity-region NBT without ticking chunks.
Missing census data migrates; corrupt existing census data fails closed.

Regional index = clamp(dimension index + biome offset, 0, 10) when the dimension
index is positive. Nonpositive dimension values pass through unchanged. Only the
custom dimension has offsets: groves -4, wilds 0, hollows +1, highlands +2.
Strength follows local index, refreshed once per second. Dimension growth still
samples the dimension population as a whole; the regional layer has no independent
saved growth loop. Boss eligibility and Hive Mind caps use the dimension index.
Grove natural Spore spawns wait for local >=5. The grove biome has ordinary vegetation
and animals and is excluded from Spore foliage and structure-start biome tags.
Spread and wandering mobs can cross into groves; only Overworld mushroom fields
are permanently sterile regardless of world index.

Overworld mushroom fields are permanently immune to Spore generation, mobs,
infection and replacement blocks, regardless of the dimension index. Protection
covers the biome column, not only the block immediately beneath a creature.
Spore structure starts are forbidden in every other dimension even if an OP raises
its index. Protection applies at block-write time, not via destructive rollback.
Existing infected worlds are not reconstructed: original blocks are unknowable.
At -2 existing Spore blocks are quarantined; player buildings are not erased.

The upper-left HUD shows original fungal pixel art, the dimensional bar, and a
separate regional value/name, all driven by server packets. Networking protocol 2
requires matching addon versions on both sides. OP commands bypass the survival
ritual but still use safe travel and corruption rules.

Survival entry uses a flat 3x3 cairn: amethyst center, crying obsidian cardinal
neighbors, polished deepslate corners, with three clear blocks above. A talisman
activation consumes one ender pearl only after successful travel. Overworld mushroom
cairns refuse activation. Each player saves their own return coordinates and view.
A shared arrival cairn is constructed once above the footprint's loaded heightmaps.
A bounded collision/fluid/floor search chooses safe arrival and return positions;
no destination blocks are dug away. Return is free via talisman anywhere or empty
main hand on any complete corrupted-world cairn. No entity portal transports mobs.

Optional Civillis integration decorates its native transition label and replays
local state transitions using its last native epoch. It never invents an epoch
that would suppress subsequent Civillis events, and its native HUD settings apply.
No changes are made to Civillis civilization scores, towns, shrines or ConcentricWorld
terrain and radial settlement rules. Core operation has no Civillis class dependency.
