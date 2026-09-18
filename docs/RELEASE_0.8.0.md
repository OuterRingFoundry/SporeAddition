# Sporebound 0.8.0 — The Living Frontier

Minecraft 1.21.1 / NeoForge 21.1.249 / Spore 2.2.0j. Install matching versions on server and clients.

## Terrain

Pale Mycelial Remnant returns as a separate ivory block on ribs and in patches across Ribbed Highlands. Spore Mycelium covers the dry corrupted soil formerly generated as infected dirt and spreads itself over exposed dirt and grass. Native Spore conversion now produces this custom mycelium instead of vanilla mycelium. Submerged sediment, intact groves, negative indices and mushroom sanctuaries retain their protections. The existing `remnant_mycelium` ID remains Spore Mycelium; the restored pale block is `pale_remnant`.

New terrain appears in newly generated chunks. Existing chunks are not mass-rewritten; new infection and adjacent mycelium spread update eligible exposed dirt during play.

## Hivebound controls and progression

Configurable keys are in **Options → Controls → Sporebound**:

| Default | Action |
| --- | --- |
| H | Open the Hive network: view nodes and assignments, select destination, travel or deploy |
| N | Select the next known node |
| J | Travel to the selected node |
| B | Deploy to the node nearest the current assignment |
| K | Confirm an armed cairn awakening |

Hive Mind locations appear as private beacon-like columns projected into the HUD, with edge indicators when offscreen. Saved locations remain visible across the dimension beyond entity/chunk render distance, without loading those chunks. The selected destination displays distance. The network sends at most 128 sites, prioritizing Hive Minds. Sight requires full Hivebound armor and an active, non-sanctuary location. Travel still requires Hyper evolution, a source node within 12 blocks, a safe destination and a 30-second cooldown. All requests are checked by the server.

**Evolved** Hivebound players can climb solid walls by moving into them; crouch holds position like a ladder. Requires full armor in a non-sterile location. Swimming, flying and riding do not activate this ability.

## Lower costs and armor appearance

At a smithing table combine **one iron armor piece + one red or brown mushroom in the template slot + one rotten flesh**. A full set costs ordinary iron armor, four mushrooms and four rotten flesh. Names, enchantments and other base armor components transfer. The earlier living-armor upgrade recipes remain available.

The organic armor model now uses an original texture of pale fungal plates, dark mycelial veins, mauve tissue and amber spore clusters.

To awaken Index **-1 → 0**, craft a **Spore Catalyst** with **one rotten flesh + one red mushroom + one netherrack** (shapeless). Hold it and crouch-use a complete cairn, then press **K** within ten seconds while staying nearby. Confirmation consumes one catalyst. Both original and melted cairns work. Index -2 remains protected.

A cairn's first successful crossing consumes one ender pearl and melts its nine blocks into a violet core and fused dark slag. Later crossings through that shared structure require the Rift Talisman but no pearl. Activation survives reloads. An incomplete or obstructed cairn is unusable; failed travel does not charge a pearl or activate it. The corrupted world's arrival cairn generates already melted and active; intact arrival cairns from older saves also acquire this form. Empty-hand return remains free.

## Survivors

Survivors gain experience from collecting supplies, harvesting/mining, smelting and fighting monsters. At 16 / 48 / 96 experience they gain +4 / +8 / +12 maximum health and +1 / +2 / +3 base attack damage. Food still heals them.

They equip recovered stronger swords and armor, make sticks from planks, turn eight cobblestone into a saved portable furnace, and smelt raw iron with finite coal or charcoal (eight ore per fuel). They craft iron pickaxes, iron or diamond swords, and iron armor using real supplies. Tools determine valid ore drops and wear out through mining; replacements consume materials. Inventory, experience, armor, mining tool, furnace and remaining fuel survive reloads. Mob griefing and colony protection still govern work.

An infected survivor's death creates Spore's native **Infected Adventurer**, retaining its character skin, name and equipped items. Supplies drop once; the equipment transfers once. It does not become generic Infected Biomass. Uninfected deaths remain ordinary deaths.

### Survivor squads

Each new colony has one archer and two sword-and-shield defenders. Nearby survivors alert idle allies to hostile targets, share spare arrows, and bring food to injured companions. Archers use vanilla bow movement and real arrows, hold fire when another survivor crosses their aim, and cannot damage survivors with their arrows. When ammunition runs out, they keep the bow and switch to a carried sword; resupply restores bow use.

Defenders alternate blocking and striking. Shields use vanilla directional protection and raise delay, take durability damage, and are disabled for five seconds by an axe strike. Bow shots also wear the bow. Survivors equip recovered shields and can craft replacements from six planks and one iron, bows from three sticks and three string, and four arrows from one flint, stick and feather. Their combat role and carried ammunition persist with their saved equipment. The client shows bow-drawing and shield-blocking poses.
