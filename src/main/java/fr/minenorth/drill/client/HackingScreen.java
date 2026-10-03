package fr.minenorth.drill.client;

import fr.minenorth.drill.network.ModNetwork;
import fr.minenorth.drill.item.ModSounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Mini-jeu de piratage : mémoriser 4 numéros parmi 6 puis les reproduire. */
public class HackingScreen extends Screen {
    private final List<Integer> sequence;
    private int progress = 0;
    private final long endTime;
    private final long sequenceStart;
    private int lastVisibleSequenceIndex = -2;

        private static final long STEP_MS = 650L;
    private static final long INTRO_MS = 900L;
    private static final long TOTAL_MS = 30000L;

    public HackingScreen(List<Integer> sequence) {
        super(Component.translatable("screen.minenorthdrill.hacking"));
        this.sequence = List.copyOf(sequence);
        long now = System.currentTimeMillis();
        this.sequenceStart = now;
        this.endTime = now + TOTAL_MS;
    }

    public void setProgress(int index, int total) { this.progress = index; }
    @Override public boolean isPauseScreen() { return false; }

    private boolean showingSequence() {
        return System.currentTimeMillis() - sequenceStart < INTRO_MS + sequence.size() * STEP_MS;
    }

    private int visibleSequenceIndex() {
        long elapsed = System.currentTimeMillis() - sequenceStart;
        if (elapsed < INTRO_MS) return -1;
        int step = (int)((elapsed - INTRO_MS) / STEP_MS);
        return step >= 0 && step < sequence.size() ? step : -1;
    }

    @Override public void tick() {
        long now = System.currentTimeMillis();
        if (now >= endTime) {
            onClose();
            return;
        }
        int visible = visibleSequenceIndex();
        if (visible != lastVisibleSequenceIndex) {
            if (visible >= 0) playLocalSound(ModSounds.HACK_BEEP.get(), 0.72F, 1.0F + visible * 0.03F);
            lastVisibleSequenceIndex = visible;
        }
    }

    private void playLocalSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (minecraft != null && minecraft.level != null && minecraft.player != null) {
            minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), sound, net.minecraft.sounds.SoundSource.MASTER, volume, pitch, false);
        }
    }

    private int cellSize() {
        return Math.max(48, Math.min(72, Math.min((width - 190) / 3, (height - 260) / 2)));
    }

    private int gap() { return 8; }
    private int gridWidth() { return cellSize() * 3 + gap() * 2; }
    private int gridLeft() { return (width - gridWidth()) / 2; }
    private int gridTop() { return Math.max(125, (height - (cellSize()*2 + gap())) / 2 + 42); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int cell = cellSize();
        int gap = gap();
        int left = gridLeft();
        int top = gridTop();
        int gridH = cell * 2 + gap;
        int panelLeft = Math.max(12, left - 28);
        int panelRight = Math.min(width - 12, left + gridWidth() + 28);
        int panelTop = 10;
        int panelBottom = Math.min(height - 10, top + gridH + 24);
        int remaining = Math.max(0, (int)Math.ceil((endTime - System.currentTimeMillis()) / 1000.0));
        boolean showing = showingSequence();
        int visible = visibleSequenceIndex();

        g.fill(panelLeft, panelTop, panelRight, panelBottom, 0xF20F151D);
        g.renderOutline(panelLeft, panelTop, panelRight-panelLeft, panelBottom-panelTop, 0xFF687583);
        g.drawCenteredString(font, Component.translatable("screen.minenorthdrill.hacking"), width/2, 24, 0xFFFFFF);
        Component instruction = showing
                ? Component.translatable("screen.minenorthdrill.remember_simple", sequence.size())
                : Component.translatable("screen.minenorthdrill.click_sequence_simple");
        g.drawCenteredString(font, instruction, width/2, 48, showing ? 0x55FFFF : 0x55FF55);
        g.drawCenteredString(font, Component.translatable("screen.minenorthdrill.progress", progress, sequence.size()), width/2, 70, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.minenorthdrill.time", remaining), width/2, 91, remaining <= 10 ? 0xFF5555 : 0xFFAA00);

        for (int i=0; i<6; i++) {
            int col=i%3, row=i/3;
            int x=left+col*(cell+gap), y=top+row*(cell+gap);
            int sequenceIndex=sequence.indexOf(i);
            boolean current=showing && visible>=0 && sequence.get(visible)==i;
            boolean done=sequenceIndex>=0 && sequenceIndex<progress;
            boolean hovered=!showing && mouseX>=x && mouseX<x+cell && mouseY>=y && mouseY<y+cell;
            int color=current?0xFF00AEEF:done?0xFF2ECC71:hovered?0xFF303942:0xFF1B2026;
            g.fill(x,y,x+cell,y+cell,color);
            g.renderOutline(x,y,cell,cell,0xFF8A96A2);
            if(current) g.drawCenteredString(font,String.valueOf(i+1),x+cell/2,y+cell/2-4,0xFFFFFFFF);
            else if(done) g.drawCenteredString(font,"✓",x+cell/2,y+cell/2-4,0xFFFFFFFF);
            else if(!showing) g.drawCenteredString(font,String.valueOf(i+1),x+cell/2,y+cell/2-4,0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX,double mouseY,int button) {
        if(button!=0 || showingSequence()) return true;
        int cell=cellSize(), gap=gap(), left=gridLeft(), top=gridTop();
        for(int i=0;i<6;i++) {
            int col=i%3,row=i/3,x=left+col*(cell+gap),y=top+row*(cell+gap);
            if(mouseX>=x&&mouseX<x+cell&&mouseY>=y&&mouseY<y+cell) {
                playLocalSound(ModSounds.HACK_BEEP.get(), 0.55F, 1.12F);
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.HackClickPacket(i));
                return true;
            }
        }
        return true;
    }

    @Override public void onClose() {
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.CloseHackPacket());
        super.onClose();
    }
}
