package fr.minenorth.drill.client;

import fr.minenorth.drill.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/** Modern Pôle Emploi interface: visual job cards, icons and descriptions. */
public final class JobMenuScreen extends Screen {
    private final ModNetwork.JobViewPacket data;
    private int scroll = 0;
    private int maxScroll = 0;
    private final List<CardHit> hits = new ArrayList<>();
    private record CardHit(int x, int y, int w, int h, String id) {}

    public JobMenuScreen(ModNetwork.JobViewPacket data) {
        super(Component.literal("Pôle Emploi"));
        this.data = data;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose())
                .bounds(width / 2 - 45, height - 28, 90, 20).build());
        int rows = (data.jobs().size() + 2) / 3;
        maxScroll = Math.max(0, rows * 82 - (height - 108));
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        hits.clear();
        int panelW = Math.min(900, width - 40);
        int panelH = Math.min(600, height - 35);
        int left = (width - panelW) / 2, top = (height - panelH) / 2;

        g.fill(0, 0, width, height, 0x99070D18);
        g.fill(left, top, left + panelW, top + panelH, 0xF2182230);
        g.fill(left + 2, top + 2, left + panelW - 2, top + 62, 0xFF101C2A);
        g.fill(left + 2, top + 62, left + panelW - 2, top + 64, 0xFF3B82F6);
        g.drawCenteredString(font, Component.literal("PÔLE EMPLOI"), width / 2, top + 13, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.literal("Choisissez le métier qui vous correspond"), width / 2, top + 31, 0xFFB8C7D9);
        String current = data.currentJob();
        g.drawString(font, Component.literal("Métier actuel : " + current), left + 18, top + 72, 0xFFE8EEF5);
        if (data.salaryEnabled()) g.drawString(font, Component.literal("Salaires actifs • toutes les " + data.intervalMinutes() + " min"), left + panelW - 250, top + 72, 0xFF7FE29A);
        else g.drawString(font, Component.literal("Salaires désactivés"), left + panelW - 170, top + 72, 0xFF8998A8);

        int areaTop = top + 90;
        int areaBottom = top + panelH - 38;
        int cardW = (panelW - 58) / 3;
        int cardH = 74;
        int gapX = 10, gapY = 9;
        int contentH = ((data.jobs().size() + 2) / 3) * (cardH + gapY);
        maxScroll = Math.max(0, contentH - (areaBottom - areaTop));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        g.enableScissor(left + 10, areaTop, left + panelW - 10, areaBottom);
        for (int i = 0; i < data.jobs().size(); i++) {
            ModNetwork.JobInfo j = data.jobs().get(i);
            int col = i % 3, row = i / 3;
            int x = left + 14 + col * (cardW + gapX);
            int y = areaTop + row * (cardH + gapY) - scroll;
            boolean selected = j.id().equalsIgnoreCase(current);
            boolean hover = mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH;
            int bg = selected ? 0xFF173B4C : (hover ? 0xFF26384A : 0xFF1D2A38);
            g.fill(x, y, x + cardW, y + cardH, bg);
            g.fill(x, y, x + 4, y + cardH, selected ? 0xFF42D392 : 0xFF3B82F6);
            drawItemIcon(g, j.icon(), x + 12, y + 16);
            g.drawString(font, Component.literal(j.name()), x + 45, y + 10, 0xFFFFFFFF);
            int flags = 0;
            if (j.candidature()) flags |= 1;
            if (j.premium()) flags |= 2;
            if (j.salary() > 0 && data.salaryEnabled()) flags |= 4;
            String status = flags == 1 ? "CANDIDATURE" : flags == 2 ? "PREMIUM" : (j.salary() > 0 && data.salaryEnabled() ? j.salary() + "€" : "ACCÈS");
            g.drawString(font, Component.literal(status), x + 45, y + 23, flags == 1 ? 0xFFFFC857 : (flags == 2 ? 0xFFE58BFF : 0xFF7FE29A));
            int textX = x + 45, textY = y + 38;
            for (String line : wrap(j.description(), Math.max(20, (cardW - 52) / 6))) {
                if (textY > y + cardH - 7) break;
                g.drawString(font, Component.literal(line), textX, textY, 0xFFB8C7D9);
                textY += 10;
            }
            if (selected) g.drawString(font, Component.literal("✓ ACTUEL"), x + cardW - 58, y + 10, 0xFF7FE29A);
            hits.add(new CardHit(x, y, cardW, cardH, j.id()));
        }
        g.disableScissor();
        if (maxScroll > 0) {
            int barH = Math.max(20, (areaBottom-areaTop) * (areaBottom-areaTop) / contentH);
            int barY = areaTop + (areaBottom-areaTop-barH) * scroll / maxScroll;
            g.fill(left + panelW - 7, areaTop, left + panelW - 4, areaBottom, 0xFF34495E);
            g.fill(left + panelW - 7, barY, left + panelW - 4, barY + barH, 0xFF5DA9E9);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawItemIcon(GuiGraphics g, String id, int x, int y) {
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
            if (item != null) g.renderItem(new ItemStack(item), x, y);
        } catch (Exception ignored) {}
    }

    private static List<String> wrap(String s, int max) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isBlank()) return out;
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            if (line.length() + word.length() + 1 > max && line.length() > 0) { out.add(line.toString()); line.setLength(0); }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (CardHit h : hits) if (mouseX >= h.x && mouseX < h.x+h.w && mouseY >= h.y && mouseY < h.y+h.h) {
                Minecraft.getInstance().setScreen(null);
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.JobSelectPacket(h.id));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll -= (int)(delta * 28);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
