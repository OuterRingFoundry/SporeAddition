# Development

[Documentation](README.md) · [Contributing](../CONTRIBUTING.md)

## Repository layout

| Path | Contents |
| --- | --- |
| `src/main/java/dev/sporebound/` | Server mechanics, persistence, entities, and networking |
| `src/main/java/dev/sporebound/client/` | Rendering, HUD, controls, and client validation |
| `src/main/java/dev/sporebound/mixin/` | Integration hooks |
| `src/main/resources/` | Metadata, recipes, world generation, translations, and artwork |
| `src/test/` | Unit tests |
| `scripts/` | Disposable-world server/client acceptance tools |
| `compatibility/` | Exact dependency identities and integration notes |
| `docs/` | Player, developer, release, and validation documentation |
| `.github/` | CI and issue/pull-request templates |
| `releases/` | Historical 0.2 development artifact; use GitHub Releases for current downloads |

## Build

Use **JDK 21**. The Gradle wrapper resolves NeoForge and the pinned Spore dependency.

```sh
./gradlew --no-daemon clean check build
```

On Windows, use `gradlew.bat --no-daemon clean check build`. The JAR is written to `build/libs/`.

Run Gradle and acceptance fixtures **sequentially in one checkout**; simultaneous runs share outputs and game directories.

## Acceptance fixtures

These commands create disposable worlds on localhost port **25585**. Do not point them at player worlds. Native validation code activates only with explicit development JVM properties.

```sh
python3 scripts/acceptance.py --profile core --directory run-core-fresh
python3 scripts/client-acceptance.py --source run-core-fresh/world --civilis
```

The client harness launches through Xvfb and requires its native graphics/audio libraries. The workflow in [build.yml](../.github/workflows/build.yml) is the reference environment. Its Civillis mode uses [the locked artifact](../compatibility/civilis.lock.json).

For the combined optional-mod fixture, first place the exact optional JARs from [compatibility](../compatibility/README.md) in `compatibility/mods/`:

```sh
python3 scripts/acceptance.py --profile compat --directory run-compat-fresh
python3 scripts/client-acceptance.py --source run-compat-fresh/world --compat
python3 scripts/hive-census-acceptance.py --source run-core-fresh/world --prefix run-hive-audit
```

Archive any previous `run-client/` before a new client fixture. The harness requires passing markers because Minecraft can exit with code zero after a load failure. The server fixture audits saved entity NBT and then launches a fresh server process to verify persistence.

## Validation scope

Run checks that exercise the behavior you change. Use the core fixture for game mechanics and persistence, and client acceptance for controls, HUD, rendering, or packet-driven interaction. Optional integration changes need the corresponding pinned dependency.

Documentation-only edits should be checked for working relative links, accurate versions, and consistency with release behavior. They do not require rebuilding the unchanged game.

See [validation](VALIDATION.md) for current evidence and [maintaining](MAINTAINING.md) for publication.
