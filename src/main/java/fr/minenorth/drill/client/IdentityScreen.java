package fr.minenorth.drill.client;

import fr.minenorth.drill.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

/** Carte d'identité MineNorth : création définitive et consultation. */
public class IdentityScreen extends Screen {
    private final ModNetwork.IdentityViewPacket data;
    private EditBox lastName, firstName, birthDate, birthPlace, nationality;

    private static final int NAVY = 0xFF173B68;
    private static final int BLUE = 0xFF2B5C92;
    private static final int TEXT = 0xFF17212B;
    private static final int MUTED = 0xFF64707C;
    private static final int CARD = 0xFFF5F7F9;

    public IdentityScreen(ModNetwork.IdentityViewPacket data) {
        super(Component.literal("Carte d'identité"));
        this.data = data;
    }

    @Override
    protected void init() {
        super.init();
        if (!data.creation()) {
            addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose())
                    .bounds(width / 2 - 55, height - 30, 110, 20).build());
            return;
        }

        int panelW = Math.min(520, width - 40);
        int left = width / 2 - panelW / 2;
        if (data.exists()) {
            addRenderableWidget(Button.builder(Component.literal("Carte perdue"), b -> lostCard())
                    .bounds(left + 30, 222, panelW - 60, 24).build());
            addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose())
                    .bounds(left + 30, 252, panelW - 60, 20).build());
            return;
        }

        lastName = field(left + 30, 72, panelW - 60, "Nom", "");
        firstName = field(left + 30, 108, panelW - 60, "Prénom", "");
        birthDate = field(left + 30, 144, panelW - 60, "Date de naissance", "");
        birthPlace = field(left + 30, 180, panelW - 60, "Lieu de naissance", "");
        nationality = field(left + 30, 216, panelW - 60, "Nationalité", "Française");
        addRenderableWidget(Button.builder(Component.literal("Créer ma carte d'identité"), b -> save())
                .bounds(left + 30, 252, panelW - 60, 24).build());
    }

    private EditBox field(int x, int y, int w, String hint, String value) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.literal(hint));
        box.setHint(Component.literal(hint));
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.IdentitySavePacket(
                lastName.getValue(), firstName.getValue(), birthDate.getValue(),
                birthPlace.getValue(), nationality.getValue()
        ));
    }

    private void lostCard() {
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.IdentityLostPacket());
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        if (data.creation()) renderCreation(g);
        else renderCard(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderCreation(GuiGraphics g) {
        int panelW = Math.min(520, width - 40);
        int panelH = data.exists() ? 270 : 310;
        int x = width / 2 - panelW / 2;
        int y = data.exists() ? 55 : 25;
        g.fill(x, y, x + panelW, y + panelH, 0xFF101820);
        g.renderOutline(x, y, panelW, panelH, 0xFF58718B);
        g.fill(x, y, x + panelW, y + 43, NAVY);
        g.drawString(font, "RÉPUBLIQUE DE MINENORTH", x + 18, y + 10, 0xFFFFFFFF, false);
        g.drawString(font, data.exists() ? "CARTE D'IDENTITÉ" : "CRÉATION DE LA CARTE D'IDENTITÉ", x + 18, y + 25, 0xFFDCE9F8, false);

        if (data.exists()) {
            g.drawString(font, "Votre carte d'identité existe déjà.", x + 30, y + 65, 0xFFFFFFFF, false);
            g.drawString(font, "Elle est définitive et ne peut pas être modifiée.", x + 30, y + 84, 0xFFB9C5D0, false);
            g.drawString(font, "Vous avez perdu votre carte ?", x + 30, y + 128, 0xFFFFFFFF, false);
            g.drawString(font, "Utilisez le bouton ci-dessous pour en recevoir une nouvelle.", x + 30, y + 147, 0xFFB9C5D0, false);
        } else {
            g.drawString(font, "Les informations saisies seront définitives.", x + 30, y + 284, 0xFFB9C5D0, false);
        }
    }

    private void renderCard(GuiGraphics g) {
        int cardW = Math.min(640, width - 40);
        int cardH = 315;
        int cardX = width / 2 - cardW / 2;
        int cardY = Math.max(18, height / 2 - cardH / 2 - 12);

        g.fill(cardX + 4, cardY + 5, cardX + cardW + 4, cardY + cardH + 5, 0x55000000);
        g.fill(cardX, cardY, cardX + cardW, cardY + cardH, CARD);
        g.renderOutline(cardX, cardY, cardW, cardH, NAVY);
        g.renderOutline(cardX + 2, cardY + 2, cardW - 4, cardH - 4, 0xFFCBD4DD);

        g.fill(cardX, cardY, cardX + cardW, cardY + 52, NAVY);
        g.drawString(font, "RÉPUBLIQUE DE MINENORTH", cardX + 20, cardY + 10, 0xFFFFFFFF, false);
        g.drawString(font, "CARTE NATIONALE D'IDENTITÉ", cardX + 20, cardY + 27, 0xFFD9E7F7, false);
        g.drawString(font, "FR", cardX + cardW - 38, cardY + 18, 0xFFFFFFFF, false);

        int photoX = cardX + 24;
        int photoY = cardY + 72;
        int photoW = 142;
        int photoH = 176;
        g.fill(photoX, photoY, photoX + photoW, photoY + photoH, 0xFFD9DEE3);
        g.renderOutline(photoX, photoY, photoW, photoH, 0xFFB2BBC4);
        drawPlayerFace(g, photoX + 8, photoY + 8, photoW - 16);
        drawCenteredNoShadow(g, "PHOTO", photoX + photoW / 2, photoY + photoH - 17, 0xFF66717C);

        int infoX = cardX + 194;
        int infoW = cardW - 218;
        int y = cardY + 73;
        drawField(g, "NOM", data.lastName(), infoX, y, infoW);
        drawField(g, "PRÉNOM", data.firstName(), infoX, y + 43, infoW);
        drawField(g, "DATE DE NAISSANCE", data.birthDate(), infoX, y + 86, infoW);
        drawField(g, "LIEU DE NAISSANCE", data.birthPlace(), infoX, y + 129, infoW);
        // Un peu plus d'espace avant la nationalité pour éviter le chevauchement avec la zone du bas.
        drawField(g, "NATIONALITÉ", data.nationality(), infoX, y + 169, infoW);

        g.fill(cardX + 194, cardY + 266, cardX + cardW - 24, cardY + 267, 0xFFD3D9DF);
        g.drawString(font, "N° " + data.cardNumber(), cardX + 194, cardY + 276, TEXT, false);
        g.drawString(font, "DOCUMENT OFFICIEL", cardX + 194, cardY + 292, MUTED, false);

        if (!data.presenter().isBlank()) {
            drawCenteredNoShadow(g, "Carte présentée par " + data.presenter(), width / 2, cardY + cardH + 12, 0xFFE2E7EC);
        }
    }

    private void drawField(GuiGraphics g, String label, String value, int x, int y, int maxWidth) {
        g.drawString(font, label, x, y, BLUE, false);
        String safe = value == null ? "" : value;
        g.drawString(font, trimToWidth(safe, maxWidth), x, y + 14, TEXT, false);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + suffix) > maxWidth) end--;
        return text.substring(0, end) + suffix;
    }

    private void drawCenteredNoShadow(GuiGraphics g, String text, int centerX, int y, int color) {
        g.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private void drawPlayerFace(GuiGraphics g, int x, int y, int size) {
        ResourceLocation skin = findSkin();
        if (skin != null) PlayerFaceRenderer.draw(g, skin, x, y, size, true, false);
        else drawCenteredNoShadow(g, "PHOTO", x + size / 2, y + size / 2 - 4, MUTED);
    }

    private ResourceLocation findSkin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(data.subjectUuid())) return mc.player.getSkinTextureLocation();
        if (mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(data.subjectUuid());
            if (info != null) return info.getSkinLocation();
        }
        return null;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
