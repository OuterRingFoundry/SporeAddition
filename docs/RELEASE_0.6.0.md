# Sporebound 0.6.0 — Hive Symbiosis

Requires Minecraft 1.21.1, NeoForge 21.1, and Fungal Infection: Spore 2.2.0j (CurseForge file 8342823).

## Cooperative Hive Minds

Every ten seconds, a developing Hive can collect nearby idle infected using Spore’s native biomass calculation. Kill points are transferred exactly once; surplus idle populations can be harvested using Spore’s existing consumption probabilities and values. A Hive with fewer than 200 points prioritizes collection. Productive hosts are preferred; harvesting zero-kill hosts requires at least six idle infected nearby.

Healthy, idle Hives with more than 200 native biomass points can send up to 20 points per ten seconds to the poorest connected Hive below that reserve. Transfers conserve points. Combat or injury below 75% health stops donations. Hives retain their reserve for their own native casing, defenders and underground development. A completed, intact underground connection works in either direction; broken roots, moved/dead partners and unloaded paths cannot carry resources. Paths are checked without loading chunks.

## Hivebound armor

Upgrade each living exoskeleton piece at a smithing table: **Living Core (template slot) + matching Living armor piece (base) + Reforged Biomass A (addition)**. Each upgrade consumes one core and one reforged biomass and preserves the base item’s damage, enchantments, name and Spore mutation components. The new piece has 25% higher base durability, protection (rounded up) and toughness; native biological repair and mutation behavior remain available. Its native organic model is slightly enlarged and carries a permanent glint.

Each equipped piece binds immediately. Inventory movement, dropping, right-click replacement and creative slot replacement cannot remove it until the **dimension** is Index **-1 (Dormant)** or **-2 (Purged)**. Index 0 and regional refuges do not release the binding. Bound pieces survive death with their components; they never also drop. Administrative commands can still manage equipment.

A **complete four-piece set** makes the player Spore kin: infected do not target them, Hive raids exclude them, Spore friendly fire and harmful Spore effects are blocked, and corrupt fog does not weaken them. The full set retains native Symbiosis benefits while corruption is positive. At positive local Index, each point also grants +10% base maximum health, +7.5% base attack damage and +1.5% base movement speed. The bonuses update without stacking and preserve health percentage. At Index 0 or in clean/protected areas the full set instead gives -20% maximum health, -25% attack damage and -15% speed. Removing any piece ends full-set membership and scaling. Individual pieces retain their improved armor properties.

## Hivebound evolution and hunger

Spore calls its upgrade resource **evolution points** and tracks **kill points** separately. A fully equipped Hivebound player earns one of each per credited living-creature kill, matching native infected. Duplicate conversion/kill callbacks cannot credit the same victim twice. The player keeps these counters across saving, death and dimension changes; taking the armor off pauses benefits and earnings.

A second fungal meter sits beside the experience bar, showing evolution stage, points and available kill points. It uses the installed Spore configuration’s evolution thresholds (defaults: Evolved at 1, Hyper at 7). After Hyper, the point count continues to grow. Narrow windows place the meter above the hotbar. The existing HUD toggle controls both meters; negative Index states have no fungal veins on either bar.

In corrupted areas, each evolution point adds 5% maximum health and 2.5% attack damage, capped at 20 points of stat bonuses. Each evolution stage adds 2.5% movement speed. Clean-area penalties still take precedence. Every equipped Hivebound piece also adds hunger exhaustion, even without a full set: 0.025 exhaustion per second per piece, increased by 25% per evolution stage, in addition to normal movement and native Symbiosis hunger. Creative and spectator players are exempt.

Weaker native infected within 24 blocks follow a fully equipped player when idle, leaving combat and feeding behavior in control. Dominance compares evolution points, with evolved/hyper mobs assigned at least their native stage threshold. Equal or stronger infected do not follow. Followers stop when out of range, when the full set is removed, when their points catch up, or in protected areas; they do not teleport or load chunks.

**Crouch and right-click a weaker infected with an empty main hand** to consume it within three blocks and line of sight. This restores 3/6/10 food points for basic/evolved/hyper infected, gains its evolution value (at least one), and transfers its remaining native kill points. Consumption produces no death loot or second kill reward. Hives and Calamities cannot be consumed.

Player kill points contribute to the actual Spore economy: a developing nearby Hive can collect them into native biomass, and standing on a native Biomass Lump or Reconstructed Mind contributes one point per second to that structure’s kill score (retaining one point, like native infected). These transfers spend the available kill points exactly once and leave evolution progress intact.

## Index bar

Index **0 is Normal**: it does not advance on its own, but unlike negative states it is not a sterile lock. Dormant, Purged and Normal bars show a healthy meadow with no fungal eyes, mushrooms or veins. As the Index rises, grass turns brown and droops, flowers wilt and shed petals, roots spread, and mature infestation opens eyes in the bar. Existing sanctuary behavior and local-pressure marker remain.

All 0.5.0 biomass ecology, terrain, villages, outposts and Civillis Caution integration are retained.
