package fr.minenorth.drill.client;

import fr.minenorth.drill.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DoorPanelScreen extends Screen {
    private final ModNetwork.DoorPanelPacket data;
    private EditBox name;
    private EditBox permission;
    private int selectedType = 0;

    public DoorPanelScreen(ModNetwork.DoorPanelPacket data) {
        super(Component.translatable("screen.minenorthsysteme.door_title"));
        this.data = data;
        this.selectedType = switch (data.type()) {
            case "POLICE" -> 1;
            case "POMPIER" -> 2;
            case "ENTREPRISE" -> 3;
            case "ORGANISATION" -> 4;
            default -> 0;
        };
    }

    private void send(int action, String text, boolean flag, String extra) {
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.DoorActionPacket(action, text, flag, extra));
        this.onClose();
    }

    @Override protected void init() {
        super.init();
        int cx = width / 2;
        if (data.mode() == 0) {
            int y = 78;
            addRenderableWidget(Button.builder(Component.literal("Personnel"), b -> { selectedType = 0; rebuildWidgets(); }).bounds(cx-142,y,136,20).build());
            addRenderableWidget(Button.builder(Component.literal("Police"), b -> { selectedType = 1; rebuildWidgets(); }).bounds(cx+6,y,136,20).build());
            addRenderableWidget(Button.builder(Component.literal("Pompiers"), b -> { selectedType = 2; rebuildWidgets(); }).bounds(cx-142,y+25,136,20).build());
            addRenderableWidget(Button.builder(Component.literal("Entreprise"), b -> { selectedType = 3; rebuildWidgets(); }).bounds(cx+6,y+25,136,20).build());
            addRenderableWidget(Button.builder(Component.literal("Organisation"), b -> { selectedType = 4; rebuildWidgets(); }).bounds(cx-142,y+50,136,20).build());
            if (selectedType == 0) {
                name = new EditBox(font,cx-142,y+80,278,20,Component.literal("Joueur")); name.setHint(Component.literal("Nom du propriétaire")); addRenderableWidget(name);
                addRenderableWidget(Button.builder(Component.literal("Attribuer"),b->send(0,name.getValue(),false,"PERSONAL|")).bounds(cx-142,y+108,136,20).build());
                addRenderableWidget(Button.builder(Component.literal("Temporaire"),b->send(0,name.getValue(),true,"PERSONAL|")).bounds(cx+6,y+108,136,20).build());
                addRenderableWidget(Button.builder(Component.literal("Moi-même"),b->send(0,minecraft.player.getGameProfile().getName(),false,"PERSONAL|")).bounds(cx-142,y+134,278,20).build());
            } else {
                permission = new EditBox(font,cx-142,y+80,278,20,Component.literal("Permission"));
                permission.setValue(selectedType == 1 ? "grade.police" : selectedType == 2 ? "grade.pompier" : data.permission());
                permission.setEditable(selectedType >= 3); addRenderableWidget(permission);
                String type = switch(selectedType){case 1->"POLICE";case 2->"POMPIER";case 3->"ENTREPRISE";default->"ORGANISATION";};
                addRenderableWidget(Button.builder(Component.literal("Attribuer"),b->send(0,"",false,type+"|"+permission.getValue())).bounds(cx-142,y+108,278,20).build());
            }
        } else if (data.mode() == 1) {
            name = new EditBox(font,cx-110,82,220,20,Component.literal("Joueur")); name.setHint(Component.literal("Nom à autoriser")); addRenderableWidget(name);
            boolean personal = "PERSONAL".equals(data.type());
            if (personal && data.ownerName()!=null && !data.ownerName().isBlank()) {
                addRenderableWidget(Button.builder(Component.literal("Ajouter l'accès"),b->send(1,name.getValue(),false,"")).bounds(cx-110,108,220,20).build());
                addRenderableWidget(Button.builder(Component.literal("Retirer l'accès"),b->send(2,name.getValue(),false,"")).bounds(cx-110,134,220,20).build());
            }
            addRenderableWidget(Button.builder(Component.literal("Libérer la porte"),b->send(3,"",false,"")).bounds(cx-110,160,220,20).build());
            int y=195; for(String r:data.requests()){String[] parts=r.split("\\|",2);if(parts.length<2)continue;String u=parts[1];addRenderableWidget(Button.builder(Component.literal("✓ "+parts[0]),b->send(5,"",false,u)).bounds(cx-110,y,105,20).build());addRenderableWidget(Button.builder(Component.literal("✕"),b->send(6,"",false,u)).bounds(cx+5,y,105,20).build());y+=24;if(y>height-28)break;}
        } else {
            addRenderableWidget(Button.builder(Component.literal("Demander l'accès"),b->send(4,"",false,"")).bounds(cx-110,110,220,22).build());
        }
    }

    @Override
    protected void rebuildWidgets(){ clearWidgetsAndInit(); }
    private void clearWidgetsAndInit(){ clearWidgets(); init(); }

    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderBackground(g); int cx=width/2; g.fill(cx-165,28,cx+165,height-25,0xF210151B); g.renderOutline(cx-165,28,330,height-53,0xFF65717D);
        String title=data.mode()==0?"Attribuer la porte":data.mode()==1?"Gestion de la porte":"Porte protégée";
        g.drawCenteredString(font,title,cx,40,0xFFFFFF);
        if(data.mode()==0){String[] t={"Personnel","Police","Pompiers","Entreprise","Organisation"};g.drawCenteredString(font,"Type : "+t[selectedType],cx,63,0x55FFAA);}
        else if(data.mode()==1){g.drawCenteredString(font,"Type : "+data.type().toLowerCase(),cx,60,0x55FFAA);g.drawCenteredString(font,"Propriétaire : "+data.ownerName(),cx,74,0xC7D0D8);g.drawString(font,"Accès autorisés :",cx-130,190,0xFFFFFF);int y=204;for(String s:data.trusted()){g.drawString(font,"• "+s,cx-125,y,0xB8C7D1);y+=14;if(y>height-45)break;}g.drawString(font,"Demandes :",cx+15,190,0xFFFFFF);}
        else g.drawCenteredString(font,"Propriétaire : "+data.ownerName(),cx,76,0xC7D0D8);
        super.render(g,mx,my,pt);
    }
    @Override public boolean isPauseScreen(){return false;}
}
