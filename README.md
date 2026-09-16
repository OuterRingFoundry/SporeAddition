# Sporebound: The Blighted World

A Minecraft 1.21.1 / NeoForge companion mod for **Fungal Infection: Spore**.
It adds a corrupted Overworld-like dimension, per-dimension corruption, and permanent
Spore protection for Overworld mushroom islands. This is an independent addon, not
an official Spore release.

## Installation

Download `sporebound-0.3.0-dev.jar` and its SHA-256 checksum from the
[0.3.0 development release](https://github.com/OuterRingFoundry/SporeAddition/releases/tag/v0.3.0-dev).


Use Java 21, NeoForge **21.1.249**, and [**Spore 2.2.0j for NeoForge 1.21.1**](https://www.curseforge.com/minecraft/mc-mods/fungal-infection-spore/files/8342823)
(CurseForge file 8342823). Install Sporebound and Spore on both server and clients.
The addon deliberately pins this Spore version because it hooks infection internals.
Spore remains an external dependency; none of its JAR, textures, sounds or structures
are bundled into this mod.

Optional compatibility targets: ConcentricWorld 0.31.1 structure-fix build and
Civillis 2.0.1-release. See `compatibility/` for precise artifact identities.

Versions 0.2 and 0.3 change only newly generated terrain; old chunks retain their previous landscape.
Upgrading from 0.1 performs a one-time census of stored Hive Mind entities without
loading their chunks. Existing founders are subject to the new population caps.

New worlds provide the complete terrain guarantees. Existing worlds gain the new
dimension, but previously generated Spore structures and overwritten blocks are
not reconstructed. Quarantine prevents new infection and spread; it cannot recover
unknown original terrain. Back up an existing world before changing its mod set.

## Playing

Craft a **Rift Talisman** with an ender eye in the center, amethyst shards in the
four corners, and crying obsidian on the four edges. Build a flat **Rift Cairn**:

```text
P C P     P = polished deepslate
C A C     C = crying obsidian
P C P     A = block of amethyst
```

Leave three air blocks above all nine blocks. Right-click the amethyst center
with the talisman while carrying an ender pearl. Each survival entry consumes one
pearl; the talisman and cairn remain intact. Creative entry is free. Cairns cannot
activate on Overworld mushroom islands. They can otherwise be built in any external
dimension, including the Nether and End.

Arrival uses a shared cairn with a safe landing. In the Blighted World, use the
talisman anywhere for a **free return**. If you lose it, right-click the center of
a complete cairn with an **empty main hand** to return free. Each player remembers
their own departure dimension and position. Travel has a five-second cooldown;
blocked return points use a nearby clear spot, then Overworld spawn if necessary.
If no safe spot exists, travel refuses instead of digging through buildings.

The Blighted World is a fractured patchwork. Its own folded noise terrain makes
uneven uplands, flooded depressions and overhangs, while caves and ores remain
available. Corrupted areas contain fungal vegetation and Spore ruins. Calcite ribs
with dark feet and luminous tips occasionally rise from suitable flat ground.
Intact **Remnant Groves** retain grass, ordinary trees, animals and an Overworld sky.
These are living frontiers, not the permanently protected Overworld mushroom islands.

Each dimension saves its own corruption index. Regional pressure in the Blighted
World is derived from that dimension's index; other dimensions use their own value
without these biome modifiers:

| Region | Local adjustment | Local value when world index is 6 |
| --- | --- | --- |
| Remnant Grove | −4 | 2 |
| Blighted Wilds | 0 | 6 |
| Drowned Hollows | +1 | 7 |
| Ribbed Highlands | +2 | 8 |

Positive local values clamp to 0–10. Dimension values **−2, −1 and 0 override all
regional modifiers**. Local pressure scales mob health/damage as they move, and
Remnant Groves prevent natural Spore spawning until their local index reaches 5
(world index 9). No Spore foliage or structure starts generate in groves, although
infection and creatures can invade from adjacent corrupted regions. Regional values
are deterministic modifiers, not separate saved population counters. The compact HUD shows world pressure as a continuous bar and local pressure as a
small notch. F3 reveals exact values and the current region.

A new corrupted world starts with **one founding Hive Mind** near (512, 0), at
world index 6. Its founding slot is saved once; killing it does not respawn it.
Additional Hive Minds must develop/spawn through normal Spore mechanics:

| Dimension index | Maximum Hive Minds in that dimension |
| --- | --- |
| Below 5 | 0 |
| 5 to below 6 | 1 |
| 6 to below 7 | 2 |
| 7 to below 8 | 3 |
| 8–10 | Unlimited |

Caps include unloaded hives and survive restarts. Kills free slots; chunk unloading
does not. Lowering a cap keeps the oldest admitted hives and removes excess newer
hives without drops, including when their chunks load. Retired hives cannot return
if the index is raised again. Calamities still require dimension index 5, but do
not consume Hive Mind slots. Founding is deferred in Peaceful or below index 5.

When **Civillis** is present, its own territory transition HUD also shows
`Corrupted territory`, `Overrun territory`, `Uncorrupted remnant`,
`Spore-threatened territory`, `Spore-purged territory`, or `Mushroom sanctuary`
where applicable, alongside its original label. Its HUD enable/cooldown/layout
settings and civilization scores, towns, shrines and spawn policies are retained.
Dormant ordinary dimensions keep the original Civillis labels.

The upper-left HUD is a short continuous strip: grass and tiny flowers mark the
healthy end; branching veins, mushrooms and watching eyes mark corruption. A second
eye opens at local index 8. It is on by default. Every player can use
`/sporebound hud on`, `/sporebound hud off`, or `/sporebound hud toggle` without OP.
The setting is saved locally in `config/sporebound-client.toml` and survives restarts.
F3 shows the world/local numbers, region, and mushroom sanctuary status. Hiding the
bar does not change corruption or fog gameplay.

In the Blighted World, local pressure above 2 gradually thickens an olive spore
haze and adds drifting spores. Groves stay clearer than corrupted regions. Water,
lava, blindness and darkness retain their own fog. At local index 8 or more, ten
seconds outdoors in survival causes Weakness I, refreshed while exposed. A solid
roof or leaving the hazard resets exposure; the weakness wears off within three
seconds. Creative and spectator players are exempt. Negative/zero indices have
no spore haze or exposure.

Submerged surfaces use sediment (sand, gravel, mud and clay) instead of grass or
mycelium. Spore ruins require dry, sufficiently even terrain across all their
pieces, with terrain blending at the foundations. Plains villages can generate in
Remnant Groves and Blighted Wilds. These generation changes apply to new chunks;
existing underwater turf and damaged structures are not automatically rebuilt.

| Index | Behavior |
| --- | --- |
| -2 | Purged. Existing loaded Spore entities are discarded without drops. New entities, infection and Spore block placement/spread are blocked. Existing blocks are quarantined, not erased. |
| -1 | Dormant. No new Spore entities, infection or Spore blocks. Previously present ordinary Spore entities freeze; bosses are removed. |
| 0 | Contained. Ordinary Spore activity can occur, but the index cannot grow automatically. |
| Above 0, below 5 | Active infection, population-driven growth, stronger mobs. No Hive Minds or Calamities. |
| 5–10 | Hive Minds and Calamities permitted, with continuing growth and scaling. |

The Overworld, Nether, End and other external dimensions default to **-1**. The
Blighted World defaults to **6**. Only explicit OP commands unlock negative states.
Gameplay never increases -2, -1 or 0. Mushroom fields remain immune at every index.
Spore structures and template placement are confined to the Blighted World even
when another dimension is activated by an operator.

Every active minute, loaded basic infected contribute weight 1, evolved infected 4,
hyper infected 8, and bosses 20. Growth is `min(0.10, totalWeight × 0.0002)` points
per minute, capped at index 10. No offline progress or world-wide chunk scans.
Health gains 15% and attack damage 10% of base per positive local index point. These
modifiers are updated rather than stacked. Changes preserve current health fraction.

## Operator commands

Permission level 2 is required for the following server commands (the local HUD
commands above do not require OP):

```text
/sporebound get
/sporebound get minecraft:overworld
/sporebound set minecraft:overworld -2
/sporebound set minecraft:the_nether 2.5
/sporebound set sporebound:blighted_world 6
/sporebound enter
/sporebound return
/sporebound hives
```

The `set` command accepts exactly -2, -1, or a decimal from 0 through 10. Setting
-2 immediately removes loaded Spore entities. Unloaded entities are rejected when
their chunks load. Lowering below 5 also removes existing Hive Minds and Calamities.

## Development

```sh
# Run Gradle/build fixtures sequentially in one checkout.
./gradlew clean check build
python3 scripts/acceptance.py --profile core --directory run-core-fresh
# Place the two exact optional JARs in compatibility/mods first:
python3 scripts/acceptance.py --profile compat --directory run-compat-fresh
# Optional real-client test (requires Xvfb); archive any previous run-client first:
python3 scripts/client-acceptance.py --source run-core-fresh/world --compat
python3 scripts/hive-census-acceptance.py --source run-core-fresh/world --prefix run-hive-audit
```

Fixtures create disposable worlds on localhost port 25585. The harness requires
explicit passing markers because Minecraft can exit with code 0 after a load failure.
Never use these fixtures against a player world. Native validation code is inert
unless its explicit development JVM property is set.

The `hives` command reports the current dimension's census and cap, including unloaded hives.

See `docs/DESIGN.md` for rule details and `docs/VALIDATION.md` for verified results
and remaining limitations of this development release.
