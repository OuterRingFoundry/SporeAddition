package dev.sporebound;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FungalContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Sporebound.ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Sporebound.ID);
    public static final java.util.function.Supplier<Block> CRUST = BLOCKS.register("remnant_mycelium",
        () -> new Block(BlockBehaviour.Properties.of().strength(1.5F, 3).sound(SoundType.WART_BLOCK)));
    public static final java.util.function.Supplier<EntityType<InfectedBiomass>> BIOMASS = ENTITIES.register("infected_biomass",
        () -> EntityType.Builder.of(InfectedBiomass::new, MobCategory.MONSTER).sized(0.9F, 0.5F)
            .clientTrackingRange(8).updateInterval(2).build("sporebound:infected_biomass"));
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ENTITIES.register(bus);
        Sporebound.ITEMS.register("remnant_mycelium", () -> new BlockItem(CRUST.get(), new Item.Properties()));
        Sporebound.ITEMS.register("infected_biomass_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(
            BIOMASS, 0xA2797C, 0xDDD8BB, new Item.Properties()));
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) ->
            event.put(BIOMASS.get(), InfectedBiomass.attributes().build()));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.NATURAL_BLOCKS)) event.accept(CRUST.get());
            if (event.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS))
                event.accept(Sporebound.ITEMS.getEntries().stream().filter(x -> x.getId().getPath().equals("infected_biomass_spawn_egg")).findFirst().orElseThrow().get());
        });
    }
}
