# Player guide

[Documentation](README.md) · [Installation](INSTALLATION.md) · [0.8.0 release notes](RELEASE_0.8.0.md)

This guide describes Sporebound **0.8.0**. Start with [cairn travel](#cairn-travel), then explore [Hivebound progression](#hivebound-progression) and [survivor colonies](#survivor-colonies).

## Cairn travel

Craft a **Rift Talisman** with an ender eye in the center, amethyst shards in the
four corners, and crying obsidian on the four edges. Build a flat **Rift Cairn**:

```text
P C P     P = polished deepslate
C A C     C = crying obsidian
P C P     A = block of amethyst
```

Leave three air blocks above all nine blocks. Right-click the amethyst center
with the talisman while carrying an ender pearl. The first successful survival crossing consumes
one pearl and melts the ritual into a glowing violet core surrounded by fused dark slag.
Every later entry through that intact melted structure still needs the talisman, but no pearl.
The shared arrival portal in the Blighted World is generated already melted and active.
Creative activation is free. Cairns cannot
activate on Overworld mushroom islands. They can otherwise be built in any external
dimension, including the Nether and End.

Arrival uses a shared cairn with a safe landing. In the Blighted World, use the
talisman anywhere for a **free return**. If you lose it, right-click the center of
a complete cairn with an **empty main hand** to return free. Each player remembers
their own departure dimension and position. Travel has a five-second cooldown;
blocked return points use a nearby clear spot, then Overworld spawn if necessary.
If no safe spot exists, travel refuses instead of digging through buildings.

## Terrain and regions

The Blighted World is a fractured patchwork. Its own folded noise terrain makes
uneven uplands, flooded depressions and overhangs, while caves and ores remain
available. Corrupted areas contain fungal vegetation and Spore ruins. Pale fungal-remnant ribs
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

## Hive population

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

## HUD, fog, and exposure

When **Civillis** is present, its own territory transition HUD also shows
`Corrupted territory`, `Overrun territory`, `Uncorrupted remnant`,
`Spore-threatened territory`, `Spore-purged territory`, or `Mushroom sanctuary`
where applicable, alongside its original label. Corrupted Wilderness uses the Caution HUD state and label. Its HUD enable/cooldown/layout
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
Remnant Groves and Blighted Wilds. Pillager outposts can generate in those regions
and Ribbed Highlands, retaining their vanilla inhabitants and spawn rules. These generation changes apply to new chunks;
existing underwater turf and damaged structures are not automatically rebuilt.

## Corruption and containment

| Index | Behavior |
| --- | --- |
| -2 | Purged. Existing loaded Spore entities are discarded without drops. New entities, infection and Spore block placement/spread are blocked. Existing blocks are quarantined, not erased. |
| -1 | Dormant. No new Spore entities, infection or Spore blocks. Previously present ordinary Spore entities freeze; bosses are removed. |
| 0 | Normal. Ordinary Spore activity can occur, but the index cannot grow automatically. |
| Above 0, below 5 | Active infection, population-driven growth, stronger mobs. No Hive Minds or Calamities. |
| 5–10 | Hive Minds and Calamities permitted, with continuing growth and scaling. |

The Overworld, Nether, End and other external dimensions default to **-1**. The
Blighted World defaults to **6**. Index -1 can be unlocked deliberately by the two-step Spore Catalyst cairn ritual (crafted from one rotten flesh, red mushroom, and netherrack) (crouch-use, then the awakening key);
-2 requires an OP command. Evolved Hivebound melee infections can raise Index 0.
Population growth alone never increases -2, -1 or 0. Mushroom fields remain immune at every index.
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

The `hives` command reports the current dimension's census and cap, including unloaded hives.

## Hivebound progression

At a smithing table, combine an **iron armor piece**, a **red or brown mushroom in the template slot**, and **one rotten flesh**. Repeat for a full set. Base names, enchantments, and other armor components transfer. The earlier living-armor upgrade recipes remain available.

Full Hivebound armor enables Spore kinship and progression where the location permits it. **Evolved** players can climb solid walls by moving into them; crouching holds position. Swimming, flying, riding, and sterile locations disable climbing.

**Hyper** evolution unlocks node travel. Travel requires a source node within 12 blocks, a safe destination, and a 30-second cooldown. Hive sight can show saved Hive Mind locations beyond render distance without loading their chunks.

| Default key | Action |
| --- | --- |
| H | Open the Hive network and select a destination or assignment |
| N | Select the next known node |
| J | Travel to the selected node |
| B | Deploy to the node nearest the current assignment |
| K | Confirm an armed cairn awakening |

Rebind these under **Options → Controls → Sporebound**. Equipment, evolution, and travel requirements are enforced by the server.

To awaken **Index -1 → 0**, craft a **Spore Catalyst** from one rotten flesh, one red mushroom, and one netherrack. Hold it, crouch-use a complete cairn, then press the awakening key within ten seconds while staying nearby. Successful confirmation consumes the catalyst. Index -2 requires an operator command.

The [0.6](RELEASE_0.6.0.md) and [0.7](RELEASE_0.7.0.md) notes describe the underlying progression; [0.8](RELEASE_0.8.0.md) supersedes their armor and awakening costs.

## Survivor colonies

Survivors gather supplies, gain experience, improve weapons and armor, and preserve their equipment and resources across reloads. Food heals them. Mining uses appropriate tools and durability; crafting and smelting consume real supplies.

New colonies include one archer and two sword-and-shield defenders. Survivors alert nearby allies, share arrows, and bring food to injured companions. Archers avoid shooting through allies, switch to a carried sword when out of arrows, and return to bow use after resupply. Defenders alternate blocking and attacking; axes can disable their shields.

An infected survivor's death produces Spore's native **Infected Adventurer**, retaining its skin, name, and equipment. Supplies drop once and equipment transfers once. Ordinary uninfected deaths remain ordinary deaths.

See [0.8.0 survivor mechanics](RELEASE_0.8.0.md#survivors) for experience thresholds, crafting costs, and squad behavior.
