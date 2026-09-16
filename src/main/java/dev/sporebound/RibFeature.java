package dev.sporebound;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Small mycelial remnant ribs with dark feet and luminous tips. Entire footprint stays within a 9x9 area. */
public final class RibFeature extends Feature<NoneFeatureConfiguration> {
    public RibFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level=context.level();var origin=context.origin();
        if(!level.getLevel().dimension().equals(Sporebound.BLIGHT))return false;
        boolean alongX=context.random().nextBoolean();int height=8+context.random().nextInt(6);
        // Validate the entire arch before changing anything; preserve ruins and other features.
        for(int side:new int[]{-1,1})for(int thick=0;thick<=1;thick++) {
            var foot=origin.offset(alongX?side*4:thick,-1,alongX?thick:side*4);
            if(!level.getBlockState(foot).isSolid()||!level.getFluidState(foot).isEmpty())return false;
        }
        var positions=new java.util.ArrayList<BlockPos>();
        for(int y=0;y<=height;y++) {
            int reach=4-(int)Math.round(4.0*y*y/(height*height));
            for(int side:new int[]{-1,1})for(int thick=0;thick<=1;thick++) {
                var pos=origin.offset(alongX?side*reach:thick,y,alongX?thick:side*reach);
                if(!level.getBlockState(pos).isAir())return false;
                positions.add(pos);
            }
        }
        for(var pos:positions)level.setBlock(pos,(pos.getY()<origin.getY()+2?Blocks.POLISHED_BASALT:
            pos.getY()==origin.getY()+height?Blocks.SHROOMLIGHT:FungalContent.CRUST.get()).defaultBlockState(),2);
        return true;
    }
}
