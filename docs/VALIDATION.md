# Verified development build — 2026-09-16

Artifact: `sporebound-0.2.0-dev.jar`. See the release `SHA256SUMS` for its checksum.

Built on Java 21 / Minecraft 1.21.1 / NeoForge 21.1.249 with Spore 2.2.0j.
Exact compatibility artifacts are recorded in `compatibility/artifacts.json`.

## Results

- Gradle `check build`: passed; five JUnit tests, zero failures/errors.
- Fresh native core world: **86 checks passed**, followed by **10 restart checks**.
- Native ConcentricWorld + Civillis fixture: **86 checks passed**, followed by
  **10 restart checks**. That run preceded three additional core regional checks;
  the final combined client also verifies regional values and the native Civillis HUD.
- Real core client: **17 checks passed**, four screenshots. Actual survival packets
  exercise incomplete cairn rejection, pearl requirement, exact pearl consumption,
  safe arrival, free recall, blocked-return relocation and empty-handed escape.
- Final real combined client: **21 checks passed**, six screenshots. Includes the
  native Civillis label decoration and unchanged notification epoch, plus intact
  grove local index 2 and highland local index 8 at world index 6.
- Hive census upgrade: stored unloaded Hive Minds are discovered from existing
  entity-region NBT, and their cap survives the upgrade and another save.
- Damaged census refusal: truncated gzip and a missing required list are preserved
  and rejected instead of silently resetting the population count.

The core suite retains infection, spawn, mushroom-column protection, structure
isolation, strength scaling, OP permissions and independent dimension persistence
checks. New checks verify one initial founder; the 1/2/3/unlimited hive thresholds;
newest excess hive removal; unload without freeing slots; reload without duplicate
counting; death freeing a slot; persisted unloaded counts; four biome regions;
custom density settings; safe shared cairn persistence; ritual geometry/headroom;
calcite arch placement; and refusal to place arches outside the custom dimension.

The fixture seed's sampled terrain heights span -26 to 175. This is a bounded sample
of generated terrain, not a promise that every seed has those exact heights.

## Evidence on the development server

Project: `/data/.tmp/sporebound`

- `v2-final-build.log`, `build/test-results/test/`
- `v2-final-core.log`, `run-core-v2-final-write.log`, `run-core-v2-final-read.log`
- `v2-compat-acceptance.log`, `run-compat-v2-final-write.log`, `run-compat-v2-final-read.log`
- `v2-core-client-validation.log`, `run-client-v2-core/`
- `client-validation.log`, `run-client/client-validation.json`, `run-client/screenshots/`
- `v2-census.log`, `run-census-v2-*.log`

A parallel Gradle compilation attempt conflicted on shared output files. The
final build, native core fixture and combined client were rerun sequentially and
passed. The documented development commands should run sequentially per checkout.

## Limits

This is a tested development build, not a long-duration multiplayer or GPU
performance benchmark. Compatibility covers the exact recorded versions.
Civillis is optional; its HUD adapter is tested against 2.0.1-release. It changes
notification text only, retaining civilization classification and gameplay rules.

The pinned upstream Spore JAR logs optional Create/Farmer's Delight recipe errors
when those optional mods are absent, and some upstream model warnings. The
headless test environment also lacks audio. These did not prevent the passing
native and real-client checks.

Version 0.2 terrain appears only in newly generated chunks. Existing chunks are
not regenerated. Remnant Groves have intact generation but can be invaded; only
Overworld mushroom fields remain permanently protected. Existing overwritten
Overworld blocks are not reconstructed. Purge quarantines Spore blocks rather
than erasing player structures. Removed excess Hive Minds do not return after an
operator raises the cap again. A one-time upgrade census may add startup time in
large existing worlds; it reads entity records without activating their chunks.
