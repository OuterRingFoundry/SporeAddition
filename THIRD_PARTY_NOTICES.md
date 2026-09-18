# Third-party components

Sporebound is an independently authored addon. **Fungal Infection: Spore** is by **Harbinger** and is a required external dependency under its upstream license. No upstream Spore JAR, structure NBT, texture, sound, or model is embedded in this addon. Generated structure definitions reference templates supplied by Spore at runtime.

**Minecraft and NeoForge** are external development/runtime dependencies. Some addon models reference vanilla Minecraft textures at runtime, including Spore Mycelium and the survivor player skins; these textures are not bundled here. The Rift Talisman uses the project's own item texture and Minecraft's handheld model parent.

The **Gradle wrapper** is supplied under the Apache License 2.0, as stated in its launcher scripts.

**ConcentricWorld and Civillis** are optional runtime integrations. Their binaries are kept outside source control and the distributed addon JAR. Exact artifact identities and validation scope are recorded in [compatibility](compatibility/README.md).

Project artwork includes generated raster assets and code-drawn HUD graphics. See the provenance records for the [Rift Talisman](docs/art/RIFT_TALISMAN.md) and [fungal materials and armor](docs/art/fungal-assets.md). Third-party components retain their respective licenses.
