package dev.sporebound.client;
import dev.sporebound.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class HiveNetworkScreen extends Screen {
    public HiveNetworkScreen(){super(Component.translatable("screen.sporebound.network"));}
    @Override protected void init(){
        int x=width/2-150,y=height/2;
        addRenderableWidget(Button.builder(Component.literal("Previous node"),b->HiveControls.cycle(-1)).bounds(x,y,145,20).build());
        addRenderableWidget(Button.builder(Component.literal("Next node"),b->HiveControls.cycle(1)).bounds(x+155,y,145,20).build());
        addRenderableWidget(Button.builder(Component.literal("Travel to selected node"),b->{HiveControls.send(HiveActionPayload.TRAVEL);onClose();}).bounds(x,y+24,300,20).build());
        addRenderableWidget(Button.builder(Component.literal("Deploy to assignment"),b->{HiveControls.send(HiveActionPayload.DEPLOY);onClose();}).bounds(x,y+48,300,20).build());
        addRenderableWidget(Button.builder(Component.literal("Confirm cairn awakening"),b->{HiveControls.send(HiveActionPayload.AWAKEN);onClose();}).bounds(x,y+72,300,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        super.render(g,mx,my,delta);int y=height/2-82;
        g.drawCenteredString(font,title,width/2,y,0xFFEADDAE);
        var site=HiveControls.selection();
        String label=site==null?"Wear full Hivebound armor to sense active nodes.":
            (site.mind()?"Hive Mind":"Spore node")+" at "+site.pos().toShortString()+" ("+(int)Math.sqrt(site.pos().distToCenterSqr(minecraft.player.position()))+"m)";
        g.drawCenteredString(font,label,width/2,y+20,0xFFFFFFFF);
        g.drawCenteredString(font,"Travel: Hyper evolution, nearby node, 30-second cooldown",width/2,y+36,0xFFB5B5B5);
        var state=HiveSensePayload.ClientState.current;String order="No current Hive assignment";
        if(HiveVision.enabled())for(var target:state.targets())if(target.id().equals(state.assignment()))order="Assignment: "+target.pos().toShortString();
        g.drawCenteredString(font,order,width/2,y+52,0xFFFFB0D4);
        g.drawCenteredString(font,"Change keys in Options > Controls > Sporebound",width/2,height/2+103,0xFFB5B5B5);
    }
    @Override public boolean isPauseScreen(){return false;}
}
