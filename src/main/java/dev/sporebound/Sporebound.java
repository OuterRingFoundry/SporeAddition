package dev.sporebound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Sporebound.ID)
public final class Sporebound {
    public static final String ID = "sporebound";
    public static final net.neoforged.neoforge.registries.DeferredRegister.Items ITEMS = net.neoforged.neoforge.registries.DeferredRegister.createItems(ID);
    public static final java.util.function.Supplier<net.minecraft.world.item.Item> TALISMAN = ITEMS.register("rift_talisman", RiftTalisman::new);
    public static final java.util.function.Supplier<net.minecraft.world.item.Item> CATALYST = ITEMS.register("spore_catalyst", SporeCatalyst::new);
    public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES = net.neoforged.neoforge.registries.DeferredRegister.create(Registries.FEATURE, ID);
    public static final java.util.function.Supplier<RibFeature> RIBS = FEATURES.register("calcified_ribs", RibFeature::new);
    public static final java.util.function.Supplier<SporeColonyFeature> COLONIES = FEATURES.register("spore_colonies", SporeColonyFeature::new);
    public static final ResourceKey<Level> BLIGHT = ResourceKey.create(Registries.DIMENSION, id("blighted_world"));
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
    public Sporebound(IEventBus bus) {
        ITEMS.register(bus);FEATURES.register(bus);
        FungalContent.register(bus);
        Hivebound.register(bus);
        NeoForge.EVENT_BUS.register(new Hivebound());
        NeoForge.EVENT_BUS.register(new HiveboundEvolution());
        NeoForge.EVENT_BUS.register(new HiveNetwork());
        NeoForge.EVENT_BUS.register(new HiveProgression());
        NeoForge.EVENT_BUS.register(new SurvivorColonies());
        bus.addListener(HiveSensePayload::register);
        bus.addListener(HiveActionPayload::register);
        bus.addListener(InfusionPayload::register);
        NeoForge.EVENT_BUS.addListener(HiveCommands::register);
        bus.addListener(EvolutionPayload::register);
        NeoForge.EVENT_BUS.register(new FungalEcology());
        NeoForge.EVENT_BUS.register(new FungalForaging());
        NeoForge.EVENT_BUS.register(new HiveBurrowing());
        bus.addListener(CorruptionPayload::register);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
            event.enqueueWork(FungalEcology::installTargetPolicy));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)) { event.accept(TALISMAN.get()); event.accept(CATALYST.get()); }
        });
        NeoForge.EVENT_BUS.register(new WorldRules());
        NeoForge.EVENT_BUS.addListener(RiftCairn::interact);
        NeoForge.EVENT_BUS.addListener(Commands::register);
        NeoForge.EVENT_BUS.addListener(RuntimeValidation::start);
        NeoForge.EVENT_BUS.addListener(RuntimeValidation::tick);
    }
}
