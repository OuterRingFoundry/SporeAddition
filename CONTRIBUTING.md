# Contributing to Sporebound

Start with the [player guide](docs/PLAYER_GUIDE.md) and [development guide](docs/DEVELOPMENT.md). The project uses the existing [All Rights Reserved license](LICENSE); this contribution guide does not grant additional redistribution rights.

## Bugs and suggestions

Use the [issue chooser](https://github.com/OuterRingFoundry/SporeAddition/issues/new/choose). For bugs, include:

- Sporebound, Spore, Minecraft, NeoForge, and Java versions.
- Singleplayer or dedicated-server context, plus optional mods.
- Reproduction steps, expected behavior, and what happened.
- The relevant log or crash report, with private server addresses, tokens, and personal information removed.

For feature requests, describe the gameplay problem and the result you want. Check existing issues first.

## Pull requests

Keep each pull request focused. Explain the player-visible change and relevant validation. Update player documentation and the changelog when behavior changes; include screenshots when changing visuals.

Follow the existing code conventions. Preserve client/server separation, server validation of requests, containment boundaries, and saved-world compatibility. Do not commit dependency JARs, generated worlds, logs, credentials, or local validation manifests.

See [development](docs/DEVELOPMENT.md) for builds and acceptance fixtures. Run shared-checkout fixtures sequentially. For documentation changes, verify links and version references.

The pull-request template asks for a concise change description and the checks actually performed. Report any untested behavior clearly.
