# Sporebound 0.7.0 — The Collective

Minecraft 1.21.1, NeoForge 21.1.249, Spore 2.2.0j. Install the same addon version on server and clients.

## Hive sight and orders

Full Hivebound armor grants private through-wall outlines on collective targets, native Spore Marker effects, and tracked Hive Minds. The HUD reports the direction, distance and coordinates of the nearest known hives and nodes. Outlines are client-specific: other players do not gain this sight. Creatures outside Minecraft's entity tracking distance are represented by target coordinates rather than rendered silhouettes.

An admitted Hive Mind activates shared intelligence across its dimension, including while its chunk is unloaded. Every second, loaded Spore mobs report prey they can see. Orders favor dangerous targets, nearby troops and targets with fewer assigned reinforcements. Native Spore combat and evolution remain in charge of attacks. Marks expire after 30 seconds without observation, or immediately when their loaded target dies, disappears, becomes Hivebound or enters sanctuary. No combat chunks are force-loaded.

## Feeding and consolidation

Crouch and interact with Infected Biomass using an empty main hand while Hivebound to consume its mass for food, healing, evolution and stored kill points. Ordinary interaction does not consume allies.

Growing or hungry biomass destroys Spore remains, wall/frozen remains, brain remnants, biomass bulbs and drowned lumps without block drops. Native corpse entities release their inventories once; the items can then be digested. Dropped items remain food. Block/item scavenging respects mobGriefing and sanctuary.

At least six nearby infected that have idled for 90 seconds can consolidate. The strongest receives a weaker peer after a two-second feeding period. Evolution and stored kill points transfer; hunger resets and health recovers. Combat or damage interrupts feeding, and the process stops below the crowd threshold. Native evolution thresholds still apply; bosses and players are never selected as donors.

## Hivebound progression

Stages use the existing Spore evolution thresholds, including configured thresholds:

| Stage | Added abilities |
| --- | --- |
| Bound | Hive/node sensing, private target outlines, biomass consumption |
| Evolved | Fungal growths on the player model; successful melee hits infect eligible prey |
| Hyper | Tactical assignments and travel between nodes and Hive Minds in the current dimension |

A fresh successful melee infection adds 0.01 to the dimension Index, including from 0. Repeated hits against an already infected target do not add points. Negative indices and mushroom sanctuaries remain protected. Fungal model growths disappear when the full Hivebound set is removed.

Use `/hive nodes` to list known nodes, `/hive travel <number>` to travel, `/hive assignment` to see your target, and `/hive deploy` to choose the staging node nearest that target. Travel requires Hyper evolution, a source node within 12 blocks, a valid safe destination and a 30-second cooldown. Native biomass lumps and reconstructed minds are discovered within six blocks; admitted Hive Minds register automatically, including a one-time scan of stored Hive positions on upgrade. Destroyed nodes are removed when their chunks are checked. Hive travel never digs a landing space.

## Deliberate awakening

Dormant dimensions cannot activate from random mobs, NPCs, ordinary feeding or accidental melee. To change **-1 to 0**:

1. Build a complete Rift Cairn outside a mushroom sanctuary.
2. Hold a Rift Talisman in the main hand and a Nether Star in the offhand.
3. Crouch and use the talisman on the center to arm the ritual.
4. Stay at the cairn and run `/hive awaken confirm` within ten seconds.

Confirmation consumes exactly one Nether Star. Holding or repeatedly clicking use cannot confirm it. Index -2 cannot be unlocked by this ritual. Ordinary population growth still requires positive Index; evolved Hivebound melee is the deliberate route from 0 into positive corruption.

## Survivors

Persistent NPC survivors use nine built-in Minecraft player skins, carry swords and saved inventories, gather exposed wood/stone/ores/crops and dropped items, eat carried food to recover health, and fight monsters and Spore. Spore sees them as prey, and ordinary monsters gain survivor targeting too.

Groups naturally appear near players in eligible loaded terrain in the Overworld, Nether and Blighted World. Each group establishes a shared home; groups of existing wandering survivors can also settle together. Residents construct a small shelter using finite supplies. Nearby colonies send patrols and lay lit stone or plank routes along paths their navigation can reach. Terrain obstructions stop work rather than being erased. Housing, gathering and road work honor mobGriefing; natural recruitment honors doMobSpawning. Colony records survive reloads, with a per-dimension cap and spacing to bound generation. This is a survival simulation with simple houses, not a full player bot or a colony-management interface. ConcentricWorld is optional; houses do not require its templates.

## Corrupted terrain

The founding Hive Mind is placed on naturally supported terrain in a corrupted area. It no longer creates a mycelium platform. Existing platforms from older versions are preserved. Spore Mycelium now uses Minecraft's actual mycelium top/side and dirt textures, with a subtle mauve tint on the top, so resource-pack replacements also apply.

Save-and-quit also yields between chunk-unload batches, preventing active Hive chunk generation from starving the pending save work during shutdown.
