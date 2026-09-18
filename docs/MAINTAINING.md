# Maintaining and sharing

[Documentation](README.md) · [Development](DEVELOPMENT.md)

## Project description

Suggested GitHub **About** text:

> A Minecraft 1.21.1 NeoForge addon for Fungal Infection: Spore, with a fungal dimension, Hivebound progression, and survivor colonies.

Suggested topics: `minecraft`, `minecraft-mod`, `neoforge`, `minecraft-1-21-1`, `spore`, `survival`.

Share the [repository](https://github.com/OuterRingFoundry/SporeAddition) for an overview and [latest release](https://github.com/OuterRingFoundry/SporeAddition/releases/latest) for installation assets. Repository visibility controls access to both. A private repository requires collaborator access; public sharing requires the owner's explicit visibility change.

## Release process

1. Update the version, player documentation, and release notes together.
2. Update version-specific JAR paths and artifact names in [the workflow](../.github/workflows/build.yml).
3. Run development-branch build, server/restart, and client acceptance. Record the commit, workflow URL, and actual coverage.
4. Merge the validated release into `main`. The push workflow repeats validation before publishing.
5. Verify the tag targets the release commit, the JAR metadata matches the version, and downloaded release assets match their checksum.
6. Update the validation overview and README links after publication.

Published tags and assets are immutable records. For changed game binaries, choose a new version; do not move an existing release tag to a documentation commit. The current publication script deliberately rejects a different commit reusing an existing tag.

Documentation-only edits are excluded from automatic build/release triggers. If a change also touches workflow configuration without changing runtime code, a maintainer may use `[skip ci]` after validating that configuration and the documentation. Game or dependency changes must receive the relevant validation.

## Licensing and credits

The project is currently **All Rights Reserved**. Public repository visibility does not change that license. Keep upstream dependencies separate and preserve the [third-party notices](../THIRD_PARTY_NOTICES.md) and asset provenance.

## Repository hygiene

Keep player guidance in the documentation index, release history in the changelog, and current downloads on GitHub Releases. Keep local manifests, generated worlds, Python caches, logs, screenshots from test runs, and dependency JARs out of commits.
