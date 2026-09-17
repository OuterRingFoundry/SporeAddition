# Sporebound 0.4.0 — Fungal Remnants

For Minecraft **1.21.1**, NeoForge **21.1.249**, and Java **21**.
Requires **Fungal Infection: Spore 2.2.0j** (CurseForge file **8342823**) on both
server and clients.

- Corrupted skies and clouds visibly change with local corruption.
- Remnant Mycelial Crust replaces calcite surfaces and pale ribs in new terrain.
- Spore creatures target eligible animals, aquatic creatures, flying creatures,
  monsters, and modded living creatures.
- Unmatched infected creatures become mobile, pulsing Infected Biomass.
- Biomass merges with a tendril animation and can grow into stronger Spore mobs.
- Hungry Spore creatures absorb biomass without attacking it, with a visible
  feeding animation; mass and hunger transfer only when absorption completes.
- Roman-numeral corruption HUD, edible-loot digestion, and crop foraging.

Replace the previous Sporebound JAR on both server and clients. Keep matching
versions and retain the required Spore dependency. Existing chunks and player
builds are preserved; terrain changes apply to newly generated chunks.

The release workflow requires a clean build, 155 fresh-world checks, 11 restart
checks, and the real-client acceptance fixture. Screenshots and detailed logs
are retained in the corresponding Actions artifact. The published JAR is
downloaded again and compared byte-for-byte with the validated build.

Separate two-player sessions, long-running population balance, and the optional
ConcentricWorld/Civillis suite have not been rerun for 0.4.0.
