package dev.sporebound.client;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.core.SConfig;
import dev.sporebound.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Runs only as part of the opt-in disposable real-client acceptance fixture. */
public final class FungalClientValidation {
    private static volatile int donorId, receiverId, feederId, foodId;
    private static volatile boolean prepared;
    private static int phase;
    private static long deadline;
    private static final BlockPos ORIGIN=new BlockPos(96,240,96);

    public static void setup() {
        var mc=Minecraft.getInstance();deadline=System.nanoTime()+120_000_000_000L;
        mc.getSingleplayerServer().execute(()->{
            var server=mc.getSingleplayerServer();var level=server.getLevel(Sporebound.BLIGHT);
            var player=server.getPlayerList().getPlayers().getFirst();
            level.getChunkAt(ORIGIN);level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
            for(int x=-6;x<=6;x++)for(int z=-4;z<=9;z++) {
                level.setBlockAndUpdate(ORIGIN.offset(x,-1,z),FungalContent.CRUST.get().defaultBlockState());
                for(int y=0;y<5;y++)level.setBlockAndUpdate(ORIGIN.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
            var donor=FungalContent.BIOMASS.get().create(level);donor.setNoAi(true);donor.setNoGravity(true);
            donor.moveTo(ORIGIN.getX()+1.6,ORIGIN.getY(),ORIGIN.getZ());level.addFreshEntity(donor);donorId=donor.getId();
            var receiver=FungalContent.BIOMASS.get().create(level);receiver.setNoAi(true);receiver.setNoGravity(true);receiver.setMass(3);
            receiver.moveTo(ORIGIN.getX()+3,ORIGIN.getY(),ORIGIN.getZ());level.addFreshEntity(receiver);receiverId=receiver.getId();
            var feeder=com.Harbinger.Spore.core.Sentities.INF_HUMAN.get().create(level);feeder.setNoAi(true);feeder.setNoGravity(true);
            feeder.setHunger(SConfig.SERVER.hunger.get());feeder.moveTo(ORIGIN.getX()-1.5,ORIGIN.getY(),ORIGIN.getZ());
            level.addFreshEntity(feeder);feederId=feeder.getId();
            var food=FungalContent.BIOMASS.get().create(level);food.setNoAi(true);food.setNoGravity(true);
            food.moveTo(ORIGIN.getX()-3,ORIGIN.getY(),ORIGIN.getZ());level.addFreshEntity(food);foodId=food.getId();
            player.setGameMode(GameType.SPECTATOR);
            player.teleportTo(level,ORIGIN.getX(),ORIGIN.getY()+2.5,ORIGIN.getZ()+7,Set.of(),180,16);
            CorruptionData.get(level).set(6);WorldRules.sync(player);prepared=true;
        });
    }
    public static boolean tick(BiConsumer<Boolean,String> check, Consumer<String> screenshot) {
        var mc=Minecraft.getInstance();
        if(System.nanoTime()>deadline)throw new AssertionError("Timed out waiting for fungal client phase "+phase);
        if(!prepared || mc.level==null || !mc.level.dimension().equals(Sporebound.BLIGHT)
                || mc.player.distanceToSqr(Vec3.atCenterOf(ORIGIN))>100)return false;
        var donor=mc.level.getEntity(donorId);var receiver=mc.level.getEntity(receiverId);var feeder=mc.level.getEntity(feederId);var food=mc.level.getEntity(foodId);
        if(phase==0) {
            if(!(donor instanceof InfectedBiomass d) || !(receiver instanceof InfectedBiomass r)
                    || !(food instanceof InfectedBiomass) || !(feeder instanceof Infected) || r.mass()!=3
                    || !mc.level.getBlockState(ORIGIN.below()).is(FungalContent.CRUST.get()))return false;
            check.accept(mc.getEntityRenderDispatcher().getRenderer(d) instanceof BiomassRenderer,"biomass has its own registered client renderer");
            check.accept(mc.getResourceManager().getResource(Sporebound.id("textures/entity/infected_biomass.png")).isPresent(),"biomass skin is present in client resources");
            check.accept(mc.getResourceManager().getResource(Sporebound.id("textures/block/remnant_mycelium.png")).isPresent(),"remnant crust skin is present in client resources");
            check.accept(r.mass()==3,"biomass mass reaches the client");
            check.accept(r.getBbWidth()>d.getBbWidth(),"mass-dependent collision size reaches the client");
            check.accept(CivilisBridge.caution(new CorruptionPayload(Sporebound.BLIGHT.location(),6,false,6,"Blighted Wilds"))
                && !CivilisBridge.caution(new CorruptionPayload(Sporebound.BLIGHT.location(),6,false,2,"Remnant Grove"))
                && !CivilisBridge.caution(new CorruptionPayload(Sporebound.BLIGHT.location(),10,true,10,"Mushroom Fields")),
                "Caution status applies to corrupted territory and respects remnant and sanctuary exceptions");
            validateSky(check);screenshot.accept("08-fungal-remnants.png");
            phase=1;
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().getLevel(Sporebound.BLIGHT);
                check.accept(((InfectedBiomass)level.getEntity(donorId)).beginAbsorption((InfectedBiomass)level.getEntity(receiverId)),"client fixture begins merge");
                check.accept(((InfectedBiomass)level.getEntity(foodId)).beginAbsorption((Infected)level.getEntity(feederId)),"client fixture begins feeding");
            });
        } else if(phase==1) {
            if(!(donor instanceof InfectedBiomass d) || !(food instanceof InfectedBiomass f))
                throw new AssertionError("Assimilation finished before client observed animation");
            if(d.absorptionTicks()<5 || f.absorptionTicks()<5)return false;
            check.accept(d.absorbing() && f.absorbing(),"merging and feeding animations are synchronized to the client");
            screenshot.accept("09-biomass-absorption.png");phase=2;
        } else if(phase==2) {
            if(donor!=null || food!=null || !(receiver instanceof InfectedBiomass r) || r.mass()!=4
                    || !(feeder instanceof Infected infected) || infected.getHunger()!=0)return false;
            check.accept(r.mass()==4,"completed merge mass reaches client without duplication");
            check.accept(infected.getHunger()==0 && infected.getTarget()==null,"feeding synchronizes satiety without combat targeting");
            screenshot.accept("10-biomass-integrated.png");phase=3;return true;
        }
        return phase==3;
    }
    private static void validateSky(BiConsumer<Boolean,String> check) {
        var mc=Minecraft.getInstance();var saved=CorruptionPayload.ClientState.current;
        if(saved==null)throw new AssertionError("Missing corruption payload during sky check");
        try {
            CorruptionPayload.ClientState.current=new CorruptionPayload(Sporebound.BLIGHT.location(),6,false,0,"Remnant Grove");
            Vec3 clear=mc.level.getSkyColor(mc.player.position(),0),cloud=mc.level.getCloudColor(0);
            CorruptionPayload.ClientState.current=new CorruptionPayload(Sporebound.BLIGHT.location(),6,false,8,"Ribbed Highlands");
            check.accept(clear.distanceToSqr(mc.level.getSkyColor(mc.player.position(),0))>0.001,"corrupted sky is visibly different from a clear remnant sky");
            check.accept(cloud.distanceToSqr(mc.level.getCloudColor(0))>0.001,"corruption discolors cloud rendering");
        } finally {CorruptionPayload.ClientState.current=saved;}
    }
}
