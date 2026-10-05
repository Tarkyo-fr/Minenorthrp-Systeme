package fr.minenorth.drill.client;

import fr.minenorth.drill.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;

public final class JobMenuScreen extends Screen {
    private final ModNetwork.JobViewPacket data;
    private int scroll=0,maxScroll=0;
    private final List<Hit> hits=new ArrayList<>();
    private record Hit(int x,int y,int w,int h,String id){}
    public JobMenuScreen(ModNetwork.JobViewPacket data){super(Component.literal("France Travail"));this.data=data;}
    private MineNorthButton btn(int x,int y,int w,int h,String label,int color,Runnable r){return addRenderableWidget(new MineNorthButton(x,y,w,h,Component.literal(label),color,r));}
    @Override protected void init(){clearWidgets();int w=Math.min(760,width-30),x=(width-w)/2,y=Math.max(12,(height-420)/2);btn(x+w-88,y+10,70,18,"FERMER",MineNorthStyle.PINK,this::onClose);}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderBackground(g);hits.clear();
        int w=Math.min(760,width-30),h=Math.min(420,height-20),x=(width-w)/2,y=Math.max(10,(height-h)/2);
        MineNorthStyle.panel(g,x,y,w,h,"FRANCE TRAVAIL","MINE NORTH RP • EMPLOI ET ACTIVITÉ");
        g.drawString(font,"Trouvez le métier qui vous correspond.",x+20,y+70,MineNorthStyle.TEXT,false);
        g.fill(x+20,y+88,x+w-20,y+120,0xFFE7EDF3);g.drawString(font,"MÉTIER ACTUEL",x+32,y+96,MineNorthStyle.MUTED,false);
        g.drawString(font,data.currentJob().isBlank()?"Aucun métier":data.currentJob(),x+32,y+106,MineNorthStyle.NAVY,false);
        g.drawString(font,data.salaryEnabled()?"Salaire actif • toutes les "+data.intervalMinutes()+" min":"Salaires désactivés",x+w-230,y+101,data.salaryEnabled()?MineNorthStyle.GREEN:MineNorthStyle.MUTED,false);

        int areaTop=y+132,areaBottom=y+h-34,cardW=(w-58)/2,cardH=104,gap=12,rows=(data.jobs().size()+1)/2,contentH=rows*(cardH+gap);
        maxScroll=Math.max(0,contentH-(areaBottom-areaTop));scroll=Math.max(0,Math.min(scroll,maxScroll));
        g.enableScissor(x+14,areaTop,x+w-14,areaBottom);
        for(int i=0;i<data.jobs().size();i++){
            var j=data.jobs().get(i);int col=i%2,row=i/2,cx=x+20+col*(cardW+gap),cy=areaTop+row*(cardH+gap)-scroll;boolean sel=j.id().equalsIgnoreCase(data.currentJob()),hover=mx>=cx&&mx<cx+cardW&&my>=cy&&my<cy+cardH;
            MineNorthStyle.card(g,cx,cy,cardW,cardH,hover,sel?MineNorthStyle.GREEN:MineNorthStyle.BLUE);
            drawIcon(g,j.icon(),cx+16,cy+20);
            g.drawString(font,j.name(),cx+56,cy+13,MineNorthStyle.TEXT,false);
            String badge=j.candidature()?"CANDIDATURE":j.premium()?"PREMIUM":(j.salary()>0&&data.salaryEnabled()?j.salary()+" € / PÉRIODE":"ACCÈS LIBRE");
            g.drawString(font,badge,cx+56,cy+28,j.candidature()?MineNorthStyle.WARN:j.premium()?MineNorthStyle.PINK:MineNorthStyle.GREEN,false);
            int ty=cy+47;for(String line:wrap(j.description(),(cardW-70)/6)){if(ty>cy+82)break;g.drawString(font,line,cx+56,ty,MineNorthStyle.MUTED,false);ty+=11;}
            if(j.max()>0)g.drawString(font,"Places : "+j.max(),cx+cardW-65,cy+13,MineNorthStyle.MUTED,false);
            if(sel)g.drawString(font,"✓ ACTUEL",cx+cardW-72,cy+89,MineNorthStyle.GREEN,false);
            hits.add(new Hit(cx,cy,cardW,cardH,j.id()));
        }
        g.disableScissor();
        if(maxScroll>0){int track=areaBottom-areaTop,bar=Math.max(25,track*track/contentH),by=areaTop+(track-bar)*scroll/maxScroll;g.fill(x+w-8,areaTop,x+w-5,areaBottom,0xFFCFD7DF);g.fill(x+w-8,by,x+w-5,by+bar,MineNorthStyle.BLUE);}
        super.render(g,mx,my,pt);
    }
    private void drawIcon(GuiGraphics g,String id,int x,int y){try{Item it=BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));if(it!=null)g.renderItem(new ItemStack(it),x,y);}catch(Exception ignored){}}
    private static List<String> wrap(String s,int max){List<String>o=new ArrayList<>();if(s==null||s.isBlank())return o;StringBuilder l=new StringBuilder();for(String w:s.split(" ")){if(l.length()+w.length()+1>max&&!l.isEmpty()){o.add(l.toString());l.setLength(0);}if(!l.isEmpty())l.append(' ');l.append(w);}if(!l.isEmpty())o.add(l.toString());return o;}
    @Override public boolean mouseClicked(double mx,double my,int button){if(button==0)for(Hit h:hits)if(mx>=h.x&&mx<h.x+h.w&&my>=h.y&&my<h.y+h.h){minecraft.setScreen(null);ModNetwork.CHANNEL.sendToServer(new ModNetwork.JobSelectPacket(h.id));return true;}return super.mouseClicked(mx,my,button);}
    @Override public boolean mouseScrolled(double mx,double my,double delta){scroll-=(int)(delta*32);scroll=Math.max(0,Math.min(scroll,maxScroll));return true;}
    @Override public boolean isPauseScreen(){return false;}
}
