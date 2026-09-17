# Sporebound 0.7.0 validation

Environment: Java 21, Minecraft 1.21.1, NeoForge 21.1.249, Spore 2.2.0j.
The isolated build and disposable test worlds are under `/data/.tmp/sporebound-v7` on the designated development server.

- `./gradlew --no-daemon --offline check build`: passed, 15 JUnit tests, no failures.
- Fresh native server: 318 checks passed (`run-v7-release-write.log`).
- Native server process restart: 12 checks passed (`run-v7-release-read.log`).
- Real client: 65 checks passed, 15 screenshots, and clean save-and-quit with an active Hive Mind (`v7-final-client.log`, `run-client/client-validation.json`).
- All 59 JSON assets parse successfully; `git diff --check` passes.

New native coverage includes shared prey reports and assignments, dead-target cleanup,
biomass consumption without duplicate rewards, infected resource transfer, node discovery,
travel progression/cooldown checks, destroyed-node removal, melee infection from Index 0,
two-step ritual confirmation and resource charging, purge protection, survivor inventory,
skin/home serialization, colony construction costs, obstruction preservation, actual paths
between colonies, generation dimension restrictions, and colony-ledger restart persistence.

The native fixture uses FakePlayer for inventory and ability checks. Its connection deliberately
ignores teleport movement, so physical node teleportation is checked with an actual client player.
Completed client fixture phases now remain completed when later tests move the player.

The addon remains pinned to Spore 2.2.0j. This run covers the core mod combination;
optional ConcentricWorld/Civillis compatibility was not rerun for 0.7.0. Existing platform blocks
are preserved on upgrade; the natural-placement rule applies to new founding hives.

A real Hive in the client fixture exposed an unload-loop starvation case: a completed save
future repeatedly requeued itself while generation still retained the chunk. Shutdown now
processes bounded unload batches, allowing the server to service pending tasks between them.
No queued unload or save is discarded, and normal running-world scheduling is unchanged.
The final real-client run saved every dimension and exited successfully.

The local EC2 disk was full and had no Java toolchain. Builds therefore used the designated
server. To retain existing data while recovering working space, old artifact logs were
losslessly compressed to `.log.gz`, and unused upstream reference assets were archived at
`research/spore/src/main/resources.reference.tar.gz` after byte-for-byte verification.
The archive can be expanded back into that directory when disk space is available.

Native Spore Marker targets are sent explicitly in the private sensing packet; arbitrary mob
effect lists are not available to observing clients. The final client checks this independently
of shared Hive orders, alongside actual node teleportation and reversible appearance.
