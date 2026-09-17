# Sporebound 0.5.0 — Living Infestation

Minecraft 1.21.1, NeoForge 21.1.249, Java 21; requires Spore 2.2.0j on server and clients.

- Corrupted biomes generate native Spore stalk colonies and foliage instead of
  vanilla mushroom patches or giant mushrooms. Spore ruins remain enabled.
- Dry corrupted soil uses Spore's distinctly textured infested dirt; Remnant
  Groves, pale highland crust, and protected Overworld mushroom islands retain
  their identities. Terrain changes apply to newly generated chunks.
- Unmatched infected creatures yield one biomass unit per 20 maximum health,
  rounded up. Each initial lump holds at most eight units: a 500-health creature
  yields 8 + 8 + 8 + 1. Native Spore counterpart conversions remain intact.
- Biomass volume, collision size, and health grow with mass. It actively seeks
  dropped items, including non-food and Spore items. Each item adds one unit and
  heals the lump. Pickup delays and mobGriefing are respected.
- After 30 seconds without feeding, nearby biomass gathers and merges. Mass above
  eight triggers attempts to develop into a Slasher or Brute; blocked evolution
  retains the mass and retries. Body size remains bounded during that wait.
- Biomass follows Spore's starvation toggle and hunger duration (300 seconds by
  default). It seeks native human/wall/frozen remains and corpse entities when
  hungry, and can scavenge nearby dying Spore mobs. At starvation it loses one
  mass unit every 60 seconds; only after reaching mass one can it expire, leaving
  a Spore fungal sapling. If terrain placement is unavailable, it drops the
  sapling item. Hunger and shrink progress persist; feeding resets both.
- Biomass attacks eligible nearby creatures with base damage one and a six-block
  detection range. It never attacks its Spore allies.
- Groups of six or more fed basic infected, idle outside combat for 30 seconds,
  have a one-in-eight chance per ten-second check to approach nearby biomass and
  volunteer for assimilation. Evolution tiers are excluded. A two-second tendril
  animation transfers mass without death loot; combat, damage, separation, or
  reduced crowding cancels the transfer. Hungry infected retain their existing
  biomass-feeding behavior.
- Villages still generate with villagers in groves and Blighted Wilds. Pillager
  outposts now generate in groves, wilds, and highlands with vanilla inhabitants
  and spawning rules.
- Hive Minds develop underground roots after five loaded minutes, at local
  corruption 7+ with at least 100 stored biomass. Growth consumes one biomass per
  root, up to four placements every ten seconds. Networks persist with the Hive,
  extend down and sideways through natural terrain, and expose hanging fungal
  roots in caves. Each Hive is limited to 256 nodes, 24 blocks horizontally and
  32 blocks downward. No extra Hive entities or chunk tickets are created.
- Mature Hive Minds seek another loaded mature Hive within 512 blocks and 64 blocks
  of vertical separation. They prioritize underground connecting tendrils before
  expanding their own roots, sharing the four-block growth budget. Each stored
  route is bounded to 1,024 blocks; searches are bounded and never load chunks. A route can form only while its
  corridor is loaded. The 512-block reach accommodates Spore's default 300-block
  spacing between naturally developing Hives.
  Peer and route persist, broken routes are repaired or replanned, and missing
  peers are released. Existing native Hive shelters remain available.
- In Civillis, corrupted Wilderness displays as Caution, including its HUD semantic
  state, with the corruption notice retained. Civilized/Shrine states, scores,
  and server spawn policies are preserved; remnant groves and sanctuaries are exempt.
- Underground growth respects containment, mobGriefing, bedrock, ores, fluids and
  block entities. Natural stone or soil placed by players is indistinguishable
  from natural terrain and can be colonized under those same rules.

Replace the older addon JAR on both server and clients. Publication is gated on
compilation, unit tests, fresh-world/restart acceptance and real-client acceptance.
The real-client fixture includes the exact locked Civillis version to exercise
Caution status. Long-running balance and the combined ConcentricWorld/Civillis
compatibility suite are not part of this update's acceptance run.
