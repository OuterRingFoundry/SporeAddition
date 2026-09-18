package dev.sporebound;

import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Explicit opt-in fixture. Never executes in an ordinary client or server. */
public final class RuntimeValidation {
    private static int checks;
    private static int ticks;
    private static boolean ran;
    private static String mode;
    public static void start(ServerStartedEvent event) {
        mode=System.getProperty("sporebound.validation");
        if(mode==null)return;
        for(var level:event.getServer().getAllLevels())level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,event.getServer());
        var blight=event.getServer().getLevel(Sporebound.BLIGHT);
        require(blight!=null,"dimension exists");
        blight.setChunkForced(176>>4,176>>4,true);
        blight.getChunk(176>>4,176>>4);
        // Reassert a runtime ticket even when the saved forced-chunk entry already exists.
        var fixtureChunk=new net.minecraft.world.level.ChunkPos(176>>4,176>>4);
        blight.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED,fixtureChunk,2,fixtureChunk);
        for(var site:FoundingHives.SITES)blight.getChunk(site[0]>>4,site[1]>>4);
    }
    public static void tick(ServerTickEvent.Post event) {
        if(mode==null||ran||++ticks<80)return;
        var fixture=event.getServer().getLevel(Sporebound.BLIGHT);
        boolean entitiesReady=fixture.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(176>>4,176>>4));
        if(!entitiesReady&&ticks<600)return;
        ran=true;
        try {
            require(entitiesReady,"fixture entity chunk completes asynchronous loading before validation");
            if(mode.endsWith("read"))read(event.getServer());else write(event.getServer());
            System.out.println("SPOREBOUND ACCEPTANCE PASS: "+mode+" checks="+checks);
            event.getServer().halt(false);
        } catch(Throwable error) {
            System.err.println("SPOREBOUND ACCEPTANCE FAILED: "+mode);
            throw new RuntimeException("Sporebound acceptance fixture",error);
        }
    }
    private static void require(boolean value,String label) {
        if(!value)throw new AssertionError(label);
        checks++;System.out.println("SPOREBOUND CHECK PASS: "+label);
    }
    private static Entity entity(String id,ServerLevel level,BlockPos pos) {
        var entity=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id)).create(level);
        if(entity==null)throw new AssertionError("Cannot create "+id);
        entity.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0);return entity;
    }
    private static void write(MinecraftServer server) throws Exception {
        var overworld=server.overworld();var blight=server.getLevel(Sporebound.BLIGHT);
        for(var level:server.getAllLevels())require(CorruptionData.get(level).index()==(level==blight?6:-1),"default "+level.dimension().location());
        terrainAndCairn(blight,overworld);
        waterAndVillages(blight);
        exposure(blight);
        FungalValidation.run(blight, RuntimeValidation::require);
        BiomassSurvivalValidation.run(blight, RuntimeValidation::require);
        HiveBurrowingValidation.run(blight, RuntimeValidation::require);
        HiveboundValidation.run(blight, RuntimeValidation::require);
        CollectiveValidation.run(blight, RuntimeValidation::require);
        FrontierValidation.run(blight, RuntimeValidation::require);
        SurvivorCombatValidation.run(blight, RuntimeValidation::require);
        int founders=0;
        for(var entity:blight.getAllEntities())if(entity instanceof com.Harbinger.Spore.Sentities.Organoids.Proto)founders++;
        require(founders==1,"exactly one initial Hive Mind: "+founders);
        FoundingHives.seed(blight);
        int repeat=0;for(var entity:blight.getAllEntities())if(entity instanceof com.Harbinger.Spore.Sentities.Organoids.Proto)repeat++;
        require(repeat==1,"seeding is idempotent");
        for(int i=0;i<FoundingHives.SITES.length;i++)require(CorruptionData.get(blight).seeded(i),"founder slot persisted "+i);
        hiveCaps(blight);
        BlockPos normal=new BlockPos(96,120,96);
        overworld.getChunkAt(normal);
        require(!overworld.addFreshEntity(entity("spore:inf_human",overworld,normal)),"dormant rejects Spore entity");
        var placement=new net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck(
            BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("spore:inf_human")),overworld,
            net.minecraft.world.entity.MobSpawnType.NATURAL,normal,overworld.random,true);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(placement);
        require(!placement.getPlacementCheckResult(),"natural spawn rejected before entity creation");
        var cow=EntityType.COW.create(overworld);cow.moveTo(normal.getX(),normal.getY(),normal.getZ());
        require(overworld.addFreshEntity(cow),"ordinary mobs remain allowed");
        var infection=new MobEffectInstance(com.Harbinger.Spore.core.Seffects.MYCELIUM,1200);
        require(!cow.addEffect(infection),"dormant rejects infection");
        var sporeBlock=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("spore:biomass_block")).defaultBlockState();
        require(Protection.spore(sporeBlock),"test block exists in pinned Spore");
        overworld.setBlock(normal,Blocks.STONE.defaultBlockState(),3);
        require(!overworld.setBlock(normal,sporeBlock,3)&&overworld.getBlockState(normal).is(Blocks.STONE),"dormant rejects replacement before mutation");
        require(overworld.getChunkAt(normal).setBlockState(normal,sporeBlock,false)==null,"direct chunk write contained");
        set(overworld,4.999);
        require(!overworld.addFreshEntity(entity("spore:proto",overworld,normal)),"fractional index below 5 rejects Hive Mind");
        require(!overworld.addFreshEntity(entity("spore:sieger",overworld,normal)),"below 5 rejects Calamity");
        var infected=(LivingEntity)entity("spore:inf_human",overworld,normal);
        // In ConcentricWorld, the inner sanctuary independently blocks hostiles; use the Blighted World for active population checks.
        set(blight,6);
        normal = new BlockPos(512,120,0);
        infected=(LivingEntity)entity("spore:inf_human",blight,normal);
        require(blight.addFreshEntity(infected),"active dimension accepts ordinary infected");
        WorldRules.scale(infected,RegionalCorruption.at(blight,normal));double hp=infected.getMaxHealth();
        for(int i=0;i<20;i++)WorldRules.scale(infected,RegionalCorruption.at(blight,normal));
        require(infected.getMaxHealth()==hp,"health scaling never stacks");
        set(blight,10);require(infected.getMaxHealth()>hp,"higher index increases health");
        require(infected.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(Sporebound.id("corruption_damage"))!=null,"damage modifier applied");
        set(blight,-1);
        var dormantTick=new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(infected);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(dormantTick);
        require(dormantTick.isCanceled()&&!infected.isRemoved(),"dormant freezes existing mob without purging it");
        set(blight,-2);require(infected.isRemoved(),"purge removes existing mobs immediately");
        require(!blight.addFreshEntity(entity("spore:inf_human",blight,normal)),"purge rejects new mobs");
        require(CorruptionMath.advance(CorruptionData.get(blight).index(),1000000,1200)==-2,"purge cannot grow");
        set(blight,0);require(blight.addFreshEntity(entity("spore:inf_human",blight,normal)),"index zero permits contained activity");
        require(CorruptionMath.advance(0,10000,1200)==0,"zero does not activate itself");
        set(blight,5);var hive=entity("spore:proto",blight,normal);
        require(blight.addFreshEntity(hive),"Hive Mind permitted at exact threshold");
        set(blight,4.25);require(hive.isRemoved(),"lowering below threshold removes bosses");
        set(overworld,10);
        var template=overworld.getStructureManager().getOrCreate(ResourceLocation.parse("spore:lab"));
        require(!template.placeInWorld(overworld,normal,normal,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),overworld.random,3),"Spore template placement denied outside custom dimension even at 10");
        BlockPos island=new BlockPos(168,80,168);
        var chunk=overworld.getChunkAt(island);
        var mushroom=overworld.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.MUSHROOM_FIELDS);
        chunk.fillBiomesFromNoise((x,y,z,sampler)->mushroom,overworld.getChunkSource().randomState().sampler());
        require(Protection.mushroom(overworld,island)&&Protection.mushroom(overworld,island.below(100)),"mushroom protection includes caves");
        overworld.setBlock(island,Blocks.MYCELIUM.defaultBlockState(),3);
        require(!overworld.setBlock(island,sporeBlock,3)&&overworld.getBlockState(island).is(Blocks.MYCELIUM),"mushroom terrain intact at index 10");
        require(!overworld.addFreshEntity(entity("spore:inf_human",overworld,island)),"mushroom sanctuary rejects infected at index 10");
        cow.moveTo(island.getX(),island.getY(),island.getZ());
        require(!cow.addEffect(new MobEffectInstance(com.Harbinger.Spore.core.Seffects.MYCELIUM,100)),"mushroom sanctuary rejects infection");
        require(mushroom.value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap().stream().noneMatch(entry->BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).getNamespace().equals("spore")),"mushroom spawn table retains no Spore");
        var features=mushroom.value().getGenerationSettings().features();
        require(features.stream().flatMap(s->s.stream()).noneMatch(h->h.unwrapKey().map(k->k.location().getNamespace().equals("spore")).orElse(false)),"mushroom worldgen contains no Spore foliage");
        var structures=server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        int total=0;
        for(var entry:structures.entrySet())if(entry.getKey().location().getNamespace().equals("spore")) {
            total++;require(entry.getValue().biomes().stream().allMatch(b->b.is(net.minecraft.tags.TagKey.create(Registries.BIOME,Sporebound.id("blighted")))),"structure isolated: "+entry.getKey().location());
        }
        require(total==13,"all thirteen upstream structures covered");
        var target=structures.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE,ResourceLocation.parse("spore:lab")));
        var found=blight.getChunkSource().getGenerator().findNearestMapStructure(blight,HolderSet.direct(target),new BlockPos(0,80,0),32,false);
        require(found!=null,"laboratory discoverable in corrupted world");
        if(found!=null){var c=blight.getChunkAt(found.getFirst());require(c.getAllStarts().values().stream().anyMatch(s->s.isValid()&&s.getStructure()==target.value()),"laboratory actually generated");}
        // OP permission is checked by Brigadier, independently of the operator's dimension.
        var root=server.getCommands().getDispatcher().getRoot().getChild("sporebound");
        require(!root.canUse(server.createCommandSourceStack().withPermission(0)),"non-OP denied commands");
        require(root.canUse(server.createCommandSourceStack().withPermission(2)),"OP permitted commands");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"sporebound set minecraft:overworld -2");
        require(CorruptionData.get(overworld).index()==-2,"real OP command applies purge");
        set(server.getLevel(Level.END),2.5);set(blight,4.25);
        var nether=server.getLevel(Level.NETHER);set(nether,6.5);nether.getChunk(0,0);
        require(nether.addFreshEntity(entity("spore:proto",nether,new BlockPos(8,120,8))),"independent Nether census accepts first hive");
        require(nether.addFreshEntity(entity("spore:proto",nether,new BlockPos(10,120,8))),"independent Nether census accepts second hive");
        require(HivePopulation.get(nether).count()==2,"two Nether hives saved for restart");
        if(mode.startsWith("compat"))compatibility(server);
        // The earlier purge test correctly removes all Spore allies, including biomass.
        FungalValidation.prepareRestart(blight);
    }
    private static void hiveCaps(ServerLevel level) {
        var pos=new BlockPos(512,130,0);var census=HivePopulation.get(level);
        require(census.count()==1,"initial census contains one founder");
        var second=entity("spore:proto",level,pos);
        require(level.addFreshEntity(second)&&census.count()==2,"index 6 permits a second hive");
        require(!level.addFreshEntity(entity("spore:proto",level,pos)),"index 6 refuses a third hive");
        set(level,7);var third=entity("spore:proto",level,pos);
        require(level.addFreshEntity(third)&&census.count()==3,"index 7 permits a third hive");
        require(!level.addFreshEntity(entity("spore:proto",level,pos)),"index 7 refuses a fourth hive");
        set(level,8);var fourth=entity("spore:proto",level,pos);
        require(level.addFreshEntity(fourth)&&census.count()==4,"index 8 lifts the hive cap");
        set(level,6);require(census.count()==2&&third.isRemoved()&&fourth.isRemoved(),"lowering the cap removes newest excess hives without rewards");
        UUID id=second.getUUID();second.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        require(census.count()==2&&!level.addFreshEntity(entity("spore:proto",level,pos)),"unloading a hive does not free its slot");
        var reloaded=entity("spore:proto",level,pos);reloaded.setUUID(id);
        require(level.addFreshEntity(reloaded)&&census.count()==2,"same hive reloads without double counting");
        reloaded.discard();require(census.count()==1,"removing a hive frees its slot");
        require(level.addFreshEntity(entity("spore:proto",level,pos)),"vacant slot accepts a new hive");
        set(level,5);require(census.count()==1,"index 5 keeps one hive");set(level,6);
    }
    private static void exposure(ServerLevel level) {
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
            new com.mojang.authlib.GameProfile(UUID.fromString("30000000-0000-0000-0000-000000000003"),"spore-fog-test"));
        var pos=new BlockPos(1032,250,1032);var chunk=level.getChunkAt(pos);
        var highland=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
            ResourceKey.create(Registries.BIOME,Sporebound.id("ribbed_highlands")));
        chunk.fillBiomesFromNoise((x,y,z,sampler)->highland,level.getChunkSource().randomState().sampler());
        for(int y=250;y<level.getMaxBuildHeight();y++)level.setBlock(new BlockPos(pos.getX(),y,pos.getZ()),Blocks.AIR.defaultBlockState(),3);
        player.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        CorruptionData.get(level).set(6);
        require(SporeExposure.hazardous(player),"local pressure 8 makes outdoor fog hazardous (local="+RegionalCorruption.at(level,player.blockPosition())+", sky="+level.canSeeSky(player.blockPosition().above())+", alive="+player.isAlive()+")");
        for(int i=0;i<9;i++)SporeExposure.tick(player);
        require(!player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),"short spore exposure has no debuff");
        SporeExposure.tick(player);
        require(player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),"ten seconds of exposure gives weakness");
        level.setBlock(pos.above(3),Blocks.STONE.defaultBlockState(),3);
        require(!SporeExposure.hazardous(player),"solid roof shelters players from spore exposure");
        SporeExposure.tick(player);player.removeAllEffects();
        level.setBlock(pos.above(3),Blocks.AIR.defaultBlockState(),3);
        SporeExposure.tick(player);
        require(!player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),"shelter resets accumulated exposure");
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        require(!SporeExposure.hazardous(player),"creative players are immune to exposure");
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);CorruptionData.get(level).set(-2);
        require(!SporeExposure.hazardous(player),"purged dimensions have no hazardous fog");
        SporeExposure.tick(player);CorruptionData.get(level).set(6);
    }
    private static void waterAndVillages(ServerLevel level) {
        var generator=level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();
        int sampled=0;
        for(int x=-768;x<=768&&sampled<32;x+=96)for(int z=-768;z<=768&&sampled<32;z+=96) {
            int y=generator.getBaseHeight(x,z,net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,level,random);
            if(y>=generator.getSeaLevel()-2)continue;
            level.getChunk(x>>4,z>>4);
            int floor=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR,x,z)-1;
            var pos=new BlockPos(x,floor,z);
            if(!level.getFluidState(pos.above()).is(net.minecraft.tags.FluidTags.WATER))continue;
            var state=level.getBlockState(pos);
            require(!state.is(Blocks.GRASS_BLOCK)&&!state.is(Blocks.MYCELIUM),"submerged floor is not living turf at "+pos);
            sampled++;
        }
        require(sampled>=16,"sampled actual generated water bodies: "+sampled);
        var structures=level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var village=structures.get(ResourceLocation.parse("minecraft:village_plains"));
        require(village.biomes().stream().anyMatch(b->b.is(Sporebound.id("remnant_grove"))),"villages allowed in remnant groves");
        require(village.biomes().stream().anyMatch(b->b.is(Sporebound.id("blighted_wilds"))),"villages allowed in blighted wilds");
        require(village.biomes().stream().anyMatch(b->b.is(net.minecraft.world.level.biome.Biomes.PLAINS)),"vanilla village biome tags preserved");
        var outpost=structures.get(ResourceLocation.parse("minecraft:pillager_outpost"));
        require(outpost.biomes().stream().anyMatch(b->b.is(Sporebound.id("blighted_wilds")))
            && outpost.biomes().stream().anyMatch(b->b.is(Sporebound.id("ribbed_highlands"))),
            "pillager outposts allowed in corrupted regions");
        require(outpost.biomes().stream().anyMatch(b->b.is(net.minecraft.world.level.biome.Biomes.PLAINS)),
            "vanilla outpost biome tags preserved");
        for(String name:new String[]{"blighted_wilds","drowned_hollows","ribbed_highlands"}) {
            var biome=level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(ResourceKey.create(Registries.BIOME,Sporebound.id(name)));
            var vegetation=biome.value().getGenerationSettings().features().stream().flatMap(h->h.stream())
                .map(h->h.unwrapKey().orElseThrow().location()).toList();
            require(vegetation.stream().noneMatch(id->id.getNamespace().equals("minecraft")&&id.getPath().contains("mushroom")),
                "no vanilla mushroom generation in "+name);
            require(vegetation.contains(Sporebound.id("spore_colonies"))
                &&vegetation.contains(ResourceLocation.parse("spore:ground_fungal_foliage")),"native Spore vegetation in "+name);
        }
        var generationPos=new BlockPos(224,240,224);level.getChunkAt(generationPos);
        for(var floor:BlockPos.betweenClosed(generationPos.offset(-3,-1,-3),generationPos.offset(3,-1,3)))
            level.setBlockAndUpdate(floor,com.Harbinger.Spore.core.Sblocks.INFESTED_DIRT.get().defaultBlockState());
        require(Sporebound.COLONIES.get().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(
            java.util.Optional.empty(),level,generator,net.minecraft.util.RandomSource.create(913),generationPos,
            net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE)),
            "Spore colonies grow on infested soil");
        require(BlockPos.betweenClosedStream(generationPos.offset(-3,0,-3),generationPos.offset(3,8,3))
            .anyMatch(p->level.getBlockState(p).is(com.Harbinger.Spore.core.Sblocks.FUNGAL_STEM_TOP.get())),
            "generated colonies contain mature Spore stalks");
    }
    private static void terrainAndCairn(ServerLevel blight,ServerLevel overworld) {
        var generator=blight.getChunkSource().getGenerator();
        require(generator instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator noise
            &&noise.generatorSettings().is(Sporebound.id("blighted_world")),"dedicated folded-terrain noise settings selected");
        var remote=new BlockPos(12000000,80,12000000);
        for(var sample:java.util.List.of(blight,overworld)) {
            var source=sample.getChunkSource();
            require(source.getChunk(remote.getX()>>4,remote.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY,false)==null,
                "remote biome fixture starts unloaded: "+sample.dimension().location());
            var expected=source.getGenerator().getBiomeSource().getNoiseBiome(remote.getX()>>2,remote.getY()>>2,remote.getZ()>>2,source.randomState().sampler());
            require(RegionalCorruption.biomeAt(sample,remote).equals(expected),"unloaded biome lookup matches generator");
            RegionalCorruption.at(sample,remote);RegionalCorruption.name(sample,remote);Protection.mushroom(sample,remote);
            require(source.getChunk(remote.getX()>>4,remote.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY,false)==null,
                "containment and regional checks never load remote chunks");
        }
        var regions=new java.util.HashSet<ResourceLocation>();int low=320,high=-64;
        for(int x=-1536;x<=1536;x+=384)for(int z=-1536;z<=1536;z+=384) {
            var biome=generator.getBiomeSource().getNoiseBiome(x>>2,20,z>>2,blight.getChunkSource().randomState().sampler());
            regions.add(biome.unwrapKey().orElseThrow().location());
            int y=generator.getBaseHeight(x,z,net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,blight,blight.getChunkSource().randomState());
            low=Math.min(low,y);high=Math.max(high,y);
        }
        var grove=blight.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME,Sporebound.id("remnant_grove")));
        require(grove.value().getGenerationSettings().features().stream().flatMap(h->h.stream()).noneMatch(h->h.unwrapKey().map(k->k.location().getNamespace().equals("spore")).orElse(false)),"remnant groves generate without Spore foliage");
        require(RegionalCorruption.offset(grove)==-4,"intact grove regional offset applied");
        var localPos=new BlockPos(120,100,120);var localChunk=blight.getChunkAt(localPos);
        localChunk.fillBiomesFromNoise((x,y,z,sampler)->grove,blight.getChunkSource().randomState().sampler());
        require(RegionalCorruption.at(blight,localPos)==2,"dimension 6 gives remnant grove local index 2");
        var spawnCheck=new net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck(
            BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("spore:inf_human")),blight,
            net.minecraft.world.entity.MobSpawnType.NATURAL,localPos,blight.random,true);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(spawnCheck);
        require(!spawnCheck.getPlacementCheckResult(),"quiet remnant grove blocks natural Spore spawns");
        require(CorruptionMath.regional(-2,RegionalCorruption.offset(grove))==-2,"purge overrides regional modifiers");

        require(regions.size()==4,"healthy groves and all three corrupted regions sampled: "+regions);
        require(high-low>=35,"folded terrain has substantial elevation relief: "+low+".."+high);
        var center=new BlockPos(96,250,96);overworld.getChunkAt(center);RiftCairn.build(overworld,center);
        require(RiftCairn.complete(overworld,center),"complete survival cairn recognized");
        overworld.setBlock(center.above(3),Blocks.STONE.defaultBlockState(),3);
        require(!RiftCairn.complete(overworld,center),"blocked cairn cannot activate");
        overworld.setBlock(center.above(3),Blocks.AIR.defaultBlockState(),3);
        overworld.setBlock(center.east(),Blocks.OBSIDIAN.defaultBlockState(),3);
        require(!RiftCairn.complete(overworld,center),"ordinary obsidian cannot substitute crying obsidian");
        require(!RiftCairn.melt(overworld,center),"damaged cairn cannot melt or repair itself");
        overworld.setBlock(center.east(),Blocks.CRYING_OBSIDIAN.defaultBlockState(),3);
        require(RiftCairn.melt(overworld,center)&&RiftCairn.active(overworld,center),"intact cairn becomes a complete fused rift");
        overworld.setBlock(center.above(),Blocks.STONE.defaultBlockState(),3);
        require(!RiftCairn.active(overworld,center),"melted rifts still require unobstructed headroom");
        overworld.setBlock(center.above(),Blocks.AIR.defaultBlockState(),3);
        var arrival=ArrivalData.get(blight).center(blight);
        require(RiftCairn.active(blight,arrival),"arrival cairn generates already melted and activated above terrain");
        require(ArrivalData.get(blight).center(blight).equals(arrival),"arrival anchor remains fixed on repeated visits");
        var ribOrigin=new BlockPos(32,260,32);blight.getChunkAt(ribOrigin);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)blight.setBlock(ribOrigin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
        require(Sporebound.RIBS.get().place(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
            blight,generator,net.minecraft.util.RandomSource.create(913),ribOrigin),"calcified rib feature places in corrupted world");
        int calcite=0,light=0;
        for(var block:BlockPos.betweenClosed(ribOrigin.offset(-4,0,-4),ribOrigin.offset(4,14,4))) {
            if(blight.getBlockState(block).is(FungalContent.PALE.get()))calcite++;
            if(blight.getBlockState(block).is(Blocks.SHROOMLIGHT))light++;
        }
        require(calcite>15&&light>0&&blight.getBlockState(ribOrigin.above(2)).isAir(),"mycelial ribs form an open arch with luminous tips");
        require(!Sporebound.RIBS.get().place(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
            overworld,overworld.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(913),center.above()),"rib feature refuses external dimensions");
    }
    private static void read(MinecraftServer server) {
        FungalValidation.afterRestart(server.getLevel(Sporebound.BLIGHT),RuntimeValidation::require);
        require(CorruptionData.get(server.overworld()).index()==-2,"purge survives process restart");
        require(CorruptionData.get(server.getLevel(Level.NETHER)).index()==6.5,"Nether saved independently");
        require(CorruptionData.get(server.getLevel(Level.END)).index()==2.5,"fractional End index survives restart");
        var nether=server.getLevel(Level.NETHER);
        require(HivePopulation.get(nether).count()==2,"unloaded Hive Minds count survives process restart");
        require(!HivePopulation.get(nether).allows(nether,UUID.randomUUID()),"unloaded hives prevent extra spawn after restart");
        var blight=server.getLevel(Sporebound.BLIGHT);
        require(CorruptionData.get(blight).index()==4.25,"custom index survives restart");
        require(!SurvivorColonies.get(blight).homes().isEmpty(),"survivor colony ledger survives process restart");
        for(int i=0;i<FoundingHives.SITES.length;i++)require(CorruptionData.get(blight).seeded(i),"founder marker survives restart "+i);
        require(RiftCairn.active(blight,ArrivalData.get(blight).center(blight)),"melted arrival cairn and coordinate survive restart");
        var savedRift=new BlockPos(96,250,96);server.overworld().getChunkAt(savedRift);
        require(RiftCairn.active(server.overworld(),savedRift),"shared melted departure activation survives a full restart");
        set(blight,6);FoundingHives.seed(blight);
        int count=0;for(var entity:blight.getAllEntities())if(entity instanceof com.Harbinger.Spore.Sentities.Organoids.Proto)count++;
        require(count==0,"defeated founding hives never respawn after restart");
    }
    private static void set(ServerLevel level,double value){CorruptionData.get(level).set(value);WorldRules.enforce(level);}
    private static void compatibility(MinecraftServer server) throws Exception {
        require(net.neoforged.fml.ModList.get().isLoaded("concentricworld"),"ConcentricWorld loaded");
        require(net.neoforged.fml.ModList.get().isLoaded("civil"),"Civillis loaded");
        var classifier=Class.forName("civil.civilization.CivilRegionClassifier");
        var classify=classifier.getMethod("classify",ServerLevel.class,int.class,int.class,int.class);
        var result=classify.invoke(null,server.overworld(),0,4,0);
        require(result.getClass().getMethod("kind").invoke(result).toString().equals("HIGH"),"Concentric/Civillis sanctuary still civilized");
    }
}
