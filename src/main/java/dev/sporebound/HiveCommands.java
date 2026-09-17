package dev.sporebound;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import static net.minecraft.commands.Commands.*;

/** Player-facing abilities; all checks are repeated server-side at execution. */
public final class HiveCommands {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("hive")
            .then(literal("awaken").then(literal("confirm").executes(c->HiveProgression.confirmAwakening(c.getSource().getPlayerOrException())?1:
                fail(c.getSource(),"First crouch-use a Rift Talisman on a complete cairn with a Nether Star offhand, then confirm here within 10 seconds. Only Index -1 can awaken."))))
            .then(literal("nodes").executes(c->{
                var p=c.getSource().getPlayerOrException();if(!Hivebound.member(p))return fail(c.getSource(),"Wear the full Hivebound set to sense the network.");
                var nodes=HiveNodes.get(p.serverLevel()).available(p.serverLevel());
                for(int i=0;i<nodes.size();i++){var n=nodes.get(i);String text=i+": "+(n.mind()?"Hive Mind":"Spore node")+" at "+n.pos().toShortString();c.getSource().sendSuccess(()->Component.literal(text),false);}
                if(nodes.isEmpty())c.getSource().sendSuccess(()->Component.literal("No known active nodes in this dimension."),false);return 1;
            }))
            .then(literal("travel").then(argument("node",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,127)).executes(c->{
                var p=c.getSource().getPlayerOrException();return HiveNodes.travel(p,com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"node"))?1:
                    fail(c.getSource(),"Travel requires Hyper evolution, an active node within 12 blocks, a safe destination, and a ready 30-second cooldown. Use /hive nodes.");
            })))
            .then(literal("assignment").executes(c->{
                var p=c.getSource().getPlayerOrException();
                if(!Hivebound.member(p)||HiveboundEvolution.stage(p)<2)return fail(c.getSource(),"Assignments unlock at Hyper evolution.");
                var mark=HiveNetwork.assignment(p);if(mark==null)return fail(c.getSource(),"The Hive has no current target.");
                c.getSource().sendSuccess(()->Component.literal("Hive assignment: target at "+mark.pos().toShortString()+". Use /hive deploy at a node to travel to the nearest staging node."),false);return 1;
            }))
            .then(literal("deploy").executes(c->{
                var p=c.getSource().getPlayerOrException();var mark=HiveNetwork.assignment(p);
                if(mark==null)return fail(c.getSource(),"The Hive has no current target.");
                var nodes=HiveNodes.get(p.serverLevel()).available(p.serverLevel());int best=-1;double distance=Double.MAX_VALUE;
                for(int i=0;i<nodes.size();i++){double d=nodes.get(i).pos().distSqr(mark.pos());if(d<distance){distance=d;best=i;}}
                return best>=0&&HiveNodes.travel(p,best)?1:fail(c.getSource(),"Cannot deploy: reach a node, attain Hyper evolution, and wait for your travel cooldown.");
            })));
    }
    private static int fail(net.minecraft.commands.CommandSourceStack source,String message){source.sendFailure(Component.literal(message));return 0;}
}
