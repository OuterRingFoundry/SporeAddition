package dev.sporebound.client;

import dev.sporebound.Sporebound;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Local preference: no operator permission and no server-wide HUD toggle. */
@EventBusSubscriber(modid = Sporebound.ID, value = Dist.CLIENT)
public final class ClientPreferences {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HUD;
    static {
        var builder = new ModConfigSpec.Builder();
        HUD = builder.comment("Show the compact corruption bar. Also changed by /sporebound hud on|off.")
            .define("showCorruptionBar", true);
        SPEC = builder.build();
    }
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sporebound")
            .then(Commands.literal("hud")
                .then(Commands.literal("on").executes(ctx -> set(true)))
                .then(Commands.literal("off").executes(ctx -> set(false)))
                .then(Commands.literal("toggle").executes(ctx -> set(!HUD.get())))));
    }
    private static int set(boolean enabled) {
        HUD.set(enabled);
        SPEC.save();
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.translatable(
            enabled ? "message.sporebound.hud_on" : "message.sporebound.hud_off"), false);
        return 1;
    }
    @EventBusSubscriber(modid = Sporebound.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void construct(net.neoforged.fml.event.lifecycle.FMLConstructModEvent event) {
            net.neoforged.fml.ModList.get().getModContainerById(Sporebound.ID).orElseThrow()
                .registerConfig(ModConfig.Type.CLIENT, SPEC);
        }
    }
}
