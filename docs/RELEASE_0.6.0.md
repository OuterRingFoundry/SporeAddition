# Sporebound 0.6.0 — Hive Symbiosis

Requires Minecraft 1.21.1, NeoForge 21.1, and Fungal Infection: Spore 2.2.0j (CurseForge file 8342823).

## Cooperative Hive Minds

Every ten seconds, a developing Hive can collect nearby idle infected using Spore’s native biomass calculation. Kill points are transferred exactly once; surplus idle populations can be harvested using Spore’s existing consumption probabilities and values. A Hive with fewer than 200 points prioritizes collection. Productive hosts are preferred; harvesting zero-kill hosts requires at least six idle infected nearby.

Healthy, idle Hives with more than 200 native biomass points can send up to 20 points per ten seconds to the poorest connected Hive below that reserve. Transfers conserve points. Combat or injury below 75% health stops donations. Hives retain their reserve for their own native casing, defenders and underground development. A completed, intact underground connection works in either direction; broken roots, moved/dead partners and unloaded paths cannot carry resources. Paths are checked without loading chunks.

## Hivebound armor

Upgrade each living exoskeleton piece at a smithing table: **Living Core (template slot) + matching Living armor piece (base) + Reforged Biomass A (addition)**. Each upgrade consumes one core and one reforged biomass and preserves the base item’s damage, enchantments, name and Spore mutation components. The new piece has 25% higher base durability, protection (rounded up) and toughness; native biological repair and mutation behavior remain available. Its native organic model is slightly enlarged and carries a permanent glint.

Each equipped piece binds immediately. Inventory movement, dropping, right-click replacement and creative slot replacement cannot remove it until the **dimension** is Index **-1 (Dormant)** or **-2 (Purged)**. Index 0 and regional refuges do not release the binding. Bound pieces survive death with their components; they never also drop. Administrative commands can still manage equipment.

A **complete four-piece set** makes the player Spore kin: infected do not target them, Hive raids exclude them, Spore friendly fire and harmful Spore effects are blocked, and corrupt fog does not weaken them. At positive local Index, each point grants +10% base maximum health, +7.5% base attack damage and +1.5% base movement speed. The bonuses update without stacking and preserve health percentage. At Index 0 or in clean/protected areas the full set instead gives -20% maximum health, -25% attack damage and -15% speed. Removing any piece ends full-set membership and scaling. Individual pieces retain their improved armor properties.

## Index bar

Index **0 is Normal**: it does not advance on its own, but unlike negative states it is not a sterile lock. Dormant, Purged and Normal bars show a healthy meadow with no fungal eyes, mushrooms or veins. As the Index rises, grass turns brown and droops, flowers wilt and shed petals, roots spread, and mature infestation opens eyes in the bar. Existing sanctuary behavior and local-pressure marker remain.

All 0.5.0 biomass ecology, terrain, villages, outposts and Civillis Caution integration are retained.
