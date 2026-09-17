# Sporebound 0.4.0: fungal remnants

The 0.4.0 release contains the fungal-remnant and biomass update. Publication is
gated on a clean build, fresh-world checks, restart checks, and real-client
acceptance. [Release JAR and checksum](https://github.com/OuterRingFoundry/SporeAddition/releases/tag/v0.4.0).

## Player-facing changes

- Corrupted biomes have muted rose/mauve skies and discolored clouds, with stronger
  tint at higher local corruption. Day/night and weather remain active. Clear
  Remnant Groves retain their ordinary sky at local pressure two or less.
- Remnant Mycelial Crust replaces generated calcite surfaces in Ribbed Highlands
  and the pale portions of ribs. It has original ivory fungal-vein artwork, block
  and inventory models, a localized name, a creative-tab entry, and a loot table.
  Existing chunks and player constructions are not rewritten.
- Spore target goals and shared targeting policy include livestock, fish, flying
  creatures, monsters, and modded living creatures. Spore allies, biomass, armor
  stands, invulnerable creatures, creative/spectator players, and sanctuary
  occupants remain excluded.
- Existing configuration/JSON fungal counterparts retain their conversions.
  Unmatched infected mobs become Infected Biomass on death. Spore kills can trigger
  conversion even when their particular attack does not apply Mycelium.
- Infected Biomass is a crawling, pulsing, mushroom-covered lump. Nearby lumps
  coalesce through a two-second tendril animation. Eight units can form a Slasher
  or Brute when there is room. Failed spawning preserves the accumulated mass.
- Hungry infected seek biomass outside combat and absorb it without attacking,
  death events, or loot. Hunger resets, health recovers, and evolution points
  increase only on completion. Losing proximity or hunger cancels without loss.
- Mass, source creature identity, and names persist. Interrupted animations reset
  on reload; no partially transferred resource is saved.
- Biomass obeys corruption scaling, dormant/purged containment, and mushroom
  sanctuary rules. A creative spawn egg is included.
- Corruption bar indices use Roman numerals; dormant and purged states have labels.
- Hungry idle infected seek unclaimed edible drops, digest one item at a time, and
  consume mature crops and spoil farmland. Non-food loot is retained. Crop
  destruction respects mob-griefing rules and containment checks.

## Verification

Validated source: `c6968575208dad1f33210e51835ee7e18b677554`.
[Passing CI run](https://github.com/OuterRingFoundry/SporeAddition/actions/runs/35113467666).
[Download JAR, checksum, logs, and screenshots](https://github.com/OuterRingFoundry/SporeAddition/actions/runs/35113467666/artifacts/10453014401).

The clean build and all core fixture checks pass: 155 on the fresh world and 11
after a full process restart. Coverage includes target acquisition, preserved
native conversions, idempotence, canceled deaths, hunger gating, animation duration,
nonviolent feeding, mass conservation, interruption, evolution, entity NBT and
full-restart persistence, edible restrictions, and crop griefing controls.

The real-client fixture passes sky/cloud differences, custom renderer and texture
availability, synchronized mass, simultaneous merge/feeding animation state, and
completed transfer/satiety. It captures ten screenshots, including fungal remnants,
absorption, and integration. Existing ritual, containment, fog, and HUD tests pass.

Local Java grammar and JSON checks also passed. Separate two-player sessions,
long-running population balance, and the optional ConcentricWorld/Civillis
compatibility suite have not been rerun for 0.4. The older compatibility results
belong to their recorded versions. Terrain replacement applies to new generation.
