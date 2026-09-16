package dev.sporebound.client;

import dev.sporebound.*;
import java.nio.file.Files;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid=Sporebound.ID,value=Dist.CLIENT)
public final class ClientValidation {
    private static int ticks,checks;
    private static long waitingSince;
    private static volatile BlockPos departure,arrival;
    private static volatile boolean setupComplete;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("sporebound.clientValidation"))return;
        var mc=Minecraft.getInstance();
        if(mc.level==null && mc.screen!=null) {
            for(var child:mc.screen.children())if(child instanceof net.minecraft.client.gui.components.Button button
                && button.getMessage().getString().equals("I know what I'm doing!")){button.onPress();break;}
        }
        if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        ++ticks;
        // CI clients can outrun integrated-server world generation. Wait for the actual
        // synchronized state with a wall-clock deadline, instead of assuming 80 frames.
        var state=CorruptionPayload.ClientState.current;
        boolean ready=switch(ticks) {
            case 80 -> setupComplete && state!=null && state.index()==-1
                && atCairn(departure) && holdingTalisman();
            case 100 -> atCairn(departure) && holdingTalisman();
            case 140 -> atCairn(departure) && holdingTalisman()
                && RiftCairn.complete(mc.level,departure);
            case 180 -> atCairn(departure) && holdingTalisman()
                && RiftCairn.complete(mc.level,departure)
                && mc.player.getInventory().countItem(Items.ENDER_PEARL)==2;
            case 400 -> holdingTalisman() && !mc.player.getCooldowns().isOnCooldown(Sporebound.TALISMAN.get());
            case 520 -> atCairn(departure) && holdingTalisman()
                && mc.player.getInventory().countItem(Items.ENDER_PEARL)==1
                && !mc.player.getCooldowns().isOnCooldown(Sporebound.TALISMAN.get());
            case 280,320,610 -> mc.level.dimension().equals(Sporebound.BLIGHT)
                && state!=null && state.dimension().equals(Sporebound.BLIGHT.location()) && state.index()>=6;
            case 375 -> state!=null && state.index()==10;
            case 460,700 -> mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                && state!=null && state.index()==-1;
            case 640 -> atCairn(arrival) && mc.player.getMainHandItem().isEmpty()
                && !mc.player.getCooldowns().isOnCooldown(Sporebound.TALISMAN.get());
            case 800 -> state!=null && state.region().equals("Remnant Grove") && state.regionalIndex()==2;
            case 900 -> state!=null && state.region().equals("Ribbed Highlands") && state.regionalIndex()==8;
            default -> true;
        };
        if(!ready) {
            if(waitingSince==0)waitingSince=System.nanoTime();
            if(System.nanoTime()-waitingSince>120_000_000_000L)
                throw new AssertionError("Timed out waiting for client stage "+ticks+": "+state
                    +"; position="+mc.player.position()+"; hand="+mc.player.getMainHandItem()
                    +"; pearls="+mc.player.getInventory().countItem(Items.ENDER_PEARL)
                    +"; departure="+departure+"; setup="+setupComplete);
            --ticks;return;
        }
        waitingSince=0;
        if(ticks==1){mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.setScreen(null);
            mc.getSingleplayerServer().execute(()->{
                var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);player.getInventory().clearContent();
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Sporebound.TALISMAN.get()));
                for(var level:server.getAllLevels())level.setDayTime(6000);
                var level=server.overworld();level.getChunk(6,6);
                departure=new BlockPos(100,Math.max(160,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,100,100)+8),100);
                RiftCairn.build(level,departure);level.setBlock(departure.east(),Blocks.AIR.defaultBlockState(),3);
                player.teleportTo(level,departure.getX()+2.5,departure.getY()+1,departure.getZ()+0.5,Set.of(),90,35);
                CorruptionData.get(level).set(-1);WorldRules.sync(player);setupComplete=true;
            });
        }
        if(ticks==80){check(CorruptionPayload.ClientState.current!=null&&CorruptionPayload.ClientState.current.index()==-1,"dormant HUD synchronized");shot("01-dormant.png");mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);}
        if(ticks==82) {
            check(ClientPreferences.HUD.get(),"corruption bar defaults on");
            check(net.neoforged.neoforge.client.ClientCommandHandler.runCommand("sporebound hud off"),"HUD off is a client command");
            check(!ClientPreferences.HUD.get(),"HUD off changes saved local preference");
        }
        if(ticks==85)shot("07-hud-off.png");
        if(ticks==87) {
            check(net.neoforged.neoforge.client.ClientCommandHandler.runCommand("sporebound hud on"),"HUD on is a client command");
            check(ClientPreferences.HUD.get(),"HUD on restores compact bar");
        }
        if(ticks==90)check(!mc.level.dimension().equals(Sporebound.BLIGHT),"air use cannot bypass the ritual");
        if(ticks==100)click(departure);
        if(ticks==120){check(!mc.level.dimension().equals(Sporebound.BLIGHT),"incomplete cairn rejects entry");mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().overworld().setBlock(departure.east(),Blocks.CRYING_OBSIDIAN.defaultBlockState(),3));}
        if(ticks==140)click(departure);
        if(ticks==160){check(!mc.level.dimension().equals(Sporebound.BLIGHT),"survival entry requires a pearl");mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().getInventory().add(new ItemStack(Items.ENDER_PEARL,2)));}
        if(ticks==180)click(departure);
        if(ticks==280){
            mc.player.setYRot(35);mc.player.setXRot(12);
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                check(player.getY()>player.level().getMinBuildHeight()+4 && player.level().noCollision(player),"arrival is above bedrock and collision free");
                check(player.level().getBlockState(player.blockPosition().below()).isSolid(),"arrival has a solid landing surface");
                check(player.getInventory().countItem(Items.ENDER_PEARL)==1,"entry consumes exactly one pearl in survival");
            });
        }
        if(ticks==320){
            check(mc.level.dimension().equals(Sporebound.BLIGHT),"actual client cairn-use packet enters dimension");
            check(CorruptionPayload.ClientState.current.dimension().equals(Sporebound.BLIGHT.location())&&CorruptionPayload.ClientState.current.index()>=6,"dimension HUD synchronized");
            shot("02-blighted-world.png");
            mc.getSingleplayerServer().execute(()->{
                var server=mc.getSingleplayerServer();var level=server.getLevel(Sporebound.BLIGHT);CorruptionData.get(level).set(10);WorldRules.enforce(level);
                server.overworld().setBlock(departure.offset(2,1,0),Blocks.STONE.defaultBlockState(),3);
                server.overworld().setBlock(departure.offset(2,2,0),Blocks.STONE.defaultBlockState(),3);
            });
        }
        if(ticks==375) {
            validateCivilis();
            var camera=mc.gameRenderer.getMainCamera();
            var fog=new net.neoforged.neoforge.client.event.ViewportEvent.RenderFog(
                net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN,
                net.minecraft.world.level.material.FogType.NONE,camera,0,120,160,
                com.mojang.blaze3d.shaders.FogShape.SPHERE);
            SporeFog.distance(fog);
            check(fog.isCanceled()&&fog.getFarPlaneDistance()<160,"corruption shortens real client fog distance");
        }
        if(ticks==380){check(CorruptionPayload.ClientState.current.index()==10,"live index update reaches client");shot("03-overrun.png");}
        if(ticks==400)mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
        if(ticks==460){
            var fog=new net.neoforged.neoforge.client.event.ViewportEvent.RenderFog(
                net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN,
                net.minecraft.world.level.material.FogType.NONE,mc.gameRenderer.getMainCamera(),0,120,160,
                com.mojang.blaze3d.shaders.FogShape.SPHERE);
            SporeFog.distance(fog);
            check(!fog.isCanceled()&&fog.getFarPlaneDistance()==160,"spore fog resets outside the corrupted world");
            check(mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD),"talisman returns to original dimension");
            check(CorruptionPayload.ClientState.current.index()==-1,"return HUD clears previous dimension value");
            shot("04-return.png");
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                check(player.level().noCollision(player)&&!player.blockPosition().equals(departure.offset(2,1,0)),"blocked departure relocates player safely without digging");
                check(player.getInventory().countItem(Items.ENDER_PEARL)==1,"talisman return costs no pearl");
            });
        }
        if(ticks==520)click(departure);
        if(ticks==610){
            check(mc.level.dimension().equals(Sporebound.BLIGHT),"second cairn entry succeeds");
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                var center=ArrivalData.get(player.serverLevel()).center(player.serverLevel());arrival=center;
                player.teleportTo(player.serverLevel(),center.getX()+2.5,center.getY()+1,center.getZ()+0.5,Set.of(),90,35);
            });
        }
        if(ticks==640)click(arrival);
        if(ticks==700){
            check(mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD),"empty-handed cairn use returns without a talisman or pearl");
        }
        if(ticks==720)preview("remnant_grove");
        if(ticks==800){
            check(CorruptionPayload.ClientState.current.region().equals("Remnant Grove")&&CorruptionPayload.ClientState.current.regionalIndex()==2,"healthy grove preview has regional pressure 2 at world index 6");shot("05-remnant-grove.png");
        }
        if(ticks==820)preview("ribbed_highlands");
        if(ticks==900){
            check(CorruptionPayload.ClientState.current.region().equals("Ribbed Highlands")&&CorruptionPayload.ClientState.current.regionalIndex()==8,"highland preview has regional pressure 8 at world index 6");shot("06-ribbed-highlands.png");
        }
        if(ticks==920){
            try{Files.writeString(mc.gameDirectory.toPath().resolve("client-validation.json"),"{\"status\":\"passed\",\"checks\":"+checks+",\"screenshots\":7}\n");}catch(Exception error){throw new RuntimeException(error);}
            System.out.println("SPOREBOUND CLIENT ACCEPTANCE PASS");mc.stop();
        }
    }
    private static void preview(String biome) {
        var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->{
            var server=mc.getSingleplayerServer();var level=server.getLevel(Sporebound.BLIGHT);var player=server.getPlayerList().getPlayers().getFirst();
            var generator=level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();
            for(int radius=1;radius<=24;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++) {
                if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;
                int bx=x*64,bz=z*64;
                if(!generator.getBiomeSource().getNoiseBiome(bx>>2,24,bz>>2,random.sampler()).is(Sporebound.id(biome)))continue;
                int y=generator.getBaseHeight(bx,bz,net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,level,random);
                if(y<72||y>210)continue;
                level.getChunk(bx>>4,bz>>4);player.setGameMode(GameType.SPECTATOR);
                player.teleportTo(level,bx+0.5,y+15,bz+0.5,Set.of(),35,25);CorruptionData.get(level).set(6);WorldRules.enforce(level);return;
            }
            throw new AssertionError("No land preview found for "+biome);
        });
    }
    private static void validateCivilis() {
        var data=CorruptionPayload.ClientState.current;
        check(data.regionalIndex()==CorruptionMath.regional(data.index(),data.region().equals("Remnant Grove")?-4:data.region().equals("Ribbed Highlands")?2:data.region().equals("Drowned Hollows")?1:0),"regional index synchronized separately from dimension index");
        if(!net.neoforged.fml.ModList.get().isLoaded("civil"))return;
        try {
            var config=Class.forName("civil.config.CivilConfig");var cooldown=config.getField("zoneTransitionHudCooldownSeconds");int old=cooldown.getInt(null);cooldown.setInt(null,0);
            var hud=Class.forName("civil.civilization.ZoneTransitionHud");var epochField=hud.getDeclaredField("latestEpoch");epochField.setAccessible(true);long epoch=epochField.getLong(null);
            var payload=Class.forName("civil.civilization.ZoneTransitionPayload");
            hud.getMethod("onPayload",payload).invoke(null,payload.getConstructor(long.class,int.class,String.class).newInstance(epoch,1,"Wilderness"));
            var text=hud.getDeclaredField("currentText");text.setAccessible(true);
            String label=((net.minecraft.network.chat.Component)text.get(null)).getString();
            check(label.contains("territory")&&label.contains("Wilderness"),"Civillis native HUD retains its label and adds corruption notice: "+label);
            check(epochField.getLong(null)==epoch,"corruption notices preserve Civillis notification epoch");cooldown.setInt(null,old);
        }catch(ReflectiveOperationException error){throw new RuntimeException(error);}
    }
    private static boolean holdingTalisman() {
        return Minecraft.getInstance().player.getMainHandItem().is(Sporebound.TALISMAN.get());
    }
    private static boolean atCairn(BlockPos pos) {
        var mc=Minecraft.getInstance();
        return pos!=null && mc.player.distanceToSqr(Vec3.atCenterOf(pos))<16
            && mc.level.getBlockState(pos).is(Blocks.AMETHYST_BLOCK);
    }
    private static void click(BlockPos pos){var mc=Minecraft.getInstance();mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,0.5,0),Direction.UP,pos,false));}
    private static synchronized void check(boolean ok,String what){if(!ok)throw new AssertionError(what);checks++;System.out.println("SPOREBOUND CLIENT CHECK PASS: "+what);}
    private static void shot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->System.out.println(message.getString()));}
}
