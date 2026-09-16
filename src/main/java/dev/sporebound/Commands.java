package dev.sporebound;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import static net.minecraft.commands.Commands.*;

public final class Commands {
    private Commands() {}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("sporebound").requires(source -> source.hasPermission(2))
            .then(literal("get").executes(ctx -> show(ctx.getSource(),ctx.getSource().getLevel()))
                .then(argument("dimension",DimensionArgument.dimension()).executes(ctx -> show(ctx.getSource(),DimensionArgument.getDimension(ctx,"dimension")))))
            .then(literal("set").then(argument("dimension", DimensionArgument.dimension())
                .then(argument("index", DoubleArgumentType.doubleArg(-2,10)).executes(ctx -> {
                    var source = ctx.getSource(); var level = DimensionArgument.getDimension(ctx,"dimension");
                    double value = DoubleArgumentType.getDouble(ctx,"index");
                    try { CorruptionData.get(level).set(value); } catch (IllegalArgumentException ex) { source.sendFailure(Component.literal(ex.getMessage())); return 0; }
                    WorldRules.enforce(level); return show(source,level);
                }))))
            .then(literal("enter").executes(ctx -> { return Travel.enter(ctx.getSource().getPlayerOrException())?1:0; }))
            .then(literal("return").executes(ctx -> { return Travel.leave(ctx.getSource().getPlayerOrException())?1:0; }))
            .then(literal("hives").executes(ctx -> {
                var level=ctx.getSource().getLevel();var census=HivePopulation.get(level);census.reconcile(level);
                int limit=CorruptionMath.hiveLimit(CorruptionData.get(level).index());
                ctx.getSource().sendSuccess(()->Component.literal("Hive Minds in this dimension (including unloaded): "+census.count()+" / "+(limit==Integer.MAX_VALUE?"unlimited":limit)),false);
                for (int[] site : FoundingHives.SITES) ctx.getSource().sendSuccess(() -> Component.literal("Founding site: " + site[0] + ", " + site[1] + " in sporebound:blighted_world"), false);
                return 1;
            })));
    }
    private static int show(CommandSourceStack source, ServerLevel level) {
        double index = CorruptionData.get(level).index();
        source.sendSuccess(() -> Component.literal(level.dimension().location()+": "+String.format(java.util.Locale.ROOT,"%.3f",index)+" / 10 — "+CorruptionMath.state(index)), false);
        return 1;
    }
}
