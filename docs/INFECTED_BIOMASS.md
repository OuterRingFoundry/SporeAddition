# Sporebound 0.4 development: fungal remnants

Status: implemented locally; compilation, server acceptance/restart, client rendering,
and multiplayer validation are pending. This is not a published release.

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

FungalValidation is integrated into the disposable core/compat acceptance fixture.
It checks target acquisition, preserved native conversions, idempotence, canceled
deaths, hunger gating, animation duration, nonviolent feeding, mass conservation,
interruption, evolution, NBT and full-restart persistence, edible restrictions, and
crop griefing controls. These new cases have NOT been run.

The real-client fixture still needs explicit biomass/sky visual probes. Two-player
synchronization must be checked against the final built artifact.

Executed locally: resource JSON parsing and Java grammar parsing only.
No successful 0.4 build or gameplay test is claimed.
