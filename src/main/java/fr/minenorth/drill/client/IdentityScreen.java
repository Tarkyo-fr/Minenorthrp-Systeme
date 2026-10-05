package fr.minenorth.drill.client;

import fr.minenorth.drill.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class IdentityScreen extends Screen {
    private final ModNetwork.IdentityViewPacket data;
    private EditBox lastName, firstName, birthDate, birthPlace, nationality;

    public IdentityScreen(ModNetwork.IdentityViewPacket data) { super(Component.literal("Carte d'identité")); this.data=data; }

    private MineNorthButton btn(int x,int y,int w,int h,String label,int color,Runnable r){return addRenderableWidget(new MineNorthButton(x,y,w,h,Component.literal(label),color,r));}
    private EditBox field(int x,int y,int w,String hint,String value){EditBox b=new EditBox(font,x,y,w,18,Component.literal(hint));b.setHint(Component.literal(hint));b.setValue(value);addRenderableWidget(b);return b;}

    @Override protected void init(){
        clearWidgets();
        if(data.creation()) {
            int w=Math.min(560,width-30), x=(width-w)/2, y=Math.max(18,(height-310)/2);
            if(data.exists()){
                btn(x+24,y+210,w-48,22,"CARTE PERDUE — RÉÉDITER",MineNorthStyle.CYAN,()->{ModNetwork.CHANNEL.sendToServer(new ModNetwork.IdentityLostPacket());onClose();});
                btn(x+24,y+238,w-48,20,"FERMER",MineNorthStyle.PINK,this::onClose);
            } else {
                lastName=field(x+24,y+76,w-48,"Nom",""); firstName=field(x+24,y+108,w-48,"Prénom","");
                birthDate=field(x+24,y+140,w-48,"Date de naissance",""); birthPlace=field(x+24,y+172,w-48,"Lieu de naissance","");
                nationality=field(x+24,y+204,w-48,"Nationalité","Française");
                btn(x+24,y+242,w-48,22,"CRÉER LA CARTE D'IDENTITÉ",MineNorthStyle.GREEN,this::save);
            }
        } else btn(width/2-80,height-30,160,20,"FERMER",MineNorthStyle.PINK,this::onClose);
    }
    private void save(){ModNetwork.CHANNEL.sendToServer(new ModNetwork.IdentitySavePacket(lastName.getValue(),firstName.getValue(),birthDate.getValue(),birthPlace.getValue(),nationality.getValue()));}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderBackground(g);
        if(data.creation()) renderCreation(g); else renderCard(g);
        super.render(g,mx,my,pt);
    }
    private void renderCreation(GuiGraphics g){
        int w=Math.min(560,width-30),x=(width-w)/2,y=Math.max(18,(height-310)/2);
        MineNorthStyle.panel(g,x,y,w,280,data.exists()?"IDENTITÉ EXISTANTE":"CRÉATION DE L'IDENTITÉ","MINE NORTH RP • DOCUMENT PERSONNEL");
        if(data.exists()){
            g.drawString(font,"Cette identité est déjà enregistrée.",x+24,y+78,MineNorthStyle.TEXT,false);
            g.drawString(font,"Elle est définitive et ne peut pas être modifiée par le joueur.",x+24,y+98,MineNorthStyle.MUTED,false);
            g.drawString(font,"Numéro : "+data.cardNumber(),x+24,y+135,MineNorthStyle.BLUE,false);
            g.drawString(font,"Vous pouvez uniquement demander une nouvelle carte si elle est perdue.",x+24,y+158,MineNorthStyle.TEXT,false);
        } else {
            g.drawString(font,"Les informations seront enregistrées définitivement.",x+24,y+62,MineNorthStyle.TEXT,false);
            g.drawString(font,"Nom",x+24,y+70,MineNorthStyle.BLUE,false);
            g.drawString(font,"Prénom",x+24,y+102,MineNorthStyle.BLUE,false);
            g.drawString(font,"Date de naissance",x+24,y+134,MineNorthStyle.BLUE,false);
            g.drawString(font,"Lieu de naissance",x+24,y+166,MineNorthStyle.BLUE,false);
            g.drawString(font,"Nationalité",x+24,y+198,MineNorthStyle.BLUE,false);
        }
    }
    private void renderCard(GuiGraphics g){
        int w=Math.min(700,width-30),h=340,x=(width-w)/2,y=Math.max(15,(height-h)/2-5);
        g.fill(x+5,y+6,x+w+5,y+h+6,0x22000000);g.fill(x,y,x+w,y+h,0xFFF8FAFC);g.renderOutline(x,y,w,h,0xFFB8C5D0);g.renderOutline(x+2,y+2,w-4,h-4,0xFFD6DEE5);
        g.fill(x,y,x+w,y+58,MineNorthStyle.NAVY);
        g.drawString(font,MineNorthStyle.bold("RÉPUBLIQUE DE MINENORTH"),x+20,y+10,0xFFFFFFFF,false);
        g.drawString(font,"CARTE NATIONALE D'IDENTITÉ",x+20,y+29,0xFFD9E7F7,false);
        drawFlag(g,x+w-52,y+12,30,18);
        int px=x+22,py=y+76,pw=150,ph=180;g.fill(px-2,py-2,px+pw+2,py+ph+2,0xFFFFFFFF);g.renderOutline(px-2,py-2,pw+4,ph+4,0xFFB2BBC4);g.fill(px,py,px+pw,py+ph,0xFFE2E6EA);drawFace(g,px+9,py+8,132);
        int ix=x+198,iw=w-222,yy=y+78;fieldText(g,"NOM",data.lastName(),ix,yy,iw);fieldText(g,"PRÉNOM",data.firstName(),ix,yy+42,iw);fieldText(g,"DATE DE NAISSANCE",data.birthDate(),ix,yy+84,iw);fieldText(g,"LIEU DE NAISSANCE",data.birthPlace(),ix,yy+126,iw);fieldText(g,"NATIONALITÉ",data.nationality(),ix,yy+168,iw);
        g.fill(x+22,y+272,x+w-22,y+273,0xFFD3D9DF);g.drawString(font,"N° DE CARTE",x+22,y+284,MineNorthStyle.BLUE,false);g.drawString(font,data.cardNumber(),x+22,y+299,MineNorthStyle.TEXT,false);
        drawQr(g,x+w-66,y+276);g.drawString(font,"MINE NORTH RP",x+w-170,y+302,MineNorthStyle.MUTED,false);
        if(!data.presenter().isBlank()){String t="Carte présentée par "+data.presenter();g.drawString(font,t,width/2-font.width(t)/2,y+h+10,MineNorthStyle.MUTED,false);}
    }
    private void fieldText(GuiGraphics g,String label,String value,int x,int y,int w){g.drawString(font,label,x,y,MineNorthStyle.BLUE,false);String v=value==null?"":value;while(font.width(v)>w&&v.length()>3)v=v.substring(0,v.length()-4)+"...";g.drawString(font,v,x,y+14,MineNorthStyle.TEXT,false);}
    private void drawFlag(GuiGraphics g,int x,int y,int w,int h){int t=w/3;g.fill(x,y,x+t,y+h,0xFF1B3F8F);g.fill(x+t,y,x+2*t,y+h,0xFFFFFFFF);g.fill(x+2*t,y,x+w,y+h,0xFFED2939);g.renderOutline(x,y,w,h,0xFFCBD4DD);}
    private void drawQr(GuiGraphics g,int x,int y){int c=4;int[][]p={{1,1,1,0,1,1,1,0},{1,0,1,0,0,0,1,0},{1,1,1,0,1,1,1,0},{0,0,0,1,0,1,0,1},{1,1,0,0,1,0,1,0},{0,1,1,1,0,0,0,1},{1,0,1,0,1,1,0,0},{1,1,1,0,0,1,1,1}};for(int r=0;r<8;r++)for(int j=0;j<8;j++)if(p[r][j]==1)g.fill(x+j*c,y+r*c,x+j*c+c,y+r*c+c,MineNorthStyle.BLUE);}
    private void drawFace(GuiGraphics g,int x,int y,int size){ResourceLocation skin=findSkin();if(skin!=null)PlayerFaceRenderer.draw(g,skin,x,y,size,true,false);else {String t="PHOTO";g.drawString(font,t,x+size/2-font.width(t)/2,y+size/2,MineNorthStyle.MUTED,false);}}
    private ResourceLocation findSkin(){Minecraft mc=Minecraft.getInstance();if(mc.player!=null&&mc.player.getUUID().equals(data.subjectUuid()))return mc.player.getSkinTextureLocation();if(mc.getConnection()!=null){PlayerInfo i=mc.getConnection().getPlayerInfo(data.subjectUuid());if(i!=null)return i.getSkinLocation();}return null;}
    @Override public boolean isPauseScreen(){return false;}
}
