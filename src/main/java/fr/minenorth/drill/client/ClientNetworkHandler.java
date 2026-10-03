package fr.minenorth.drill.client;

import net.minecraft.client.Minecraft;

import java.util.List;

/** Client-only handlers for MineNorth Drill network packets. */
public final class ClientNetworkHandler {
    private ClientNetworkHandler() {}

    public static void openHack(List<Integer> sequence) {
        Minecraft.getInstance().setScreen(new HackingScreen(sequence));
    }

    public static void closeHack() {
        if (Minecraft.getInstance().screen instanceof HackingScreen) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    public static void openDoorPanel(fr.minenorth.drill.network.ModNetwork.DoorPanelPacket p) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new DoorPanelScreen(p));
    }

    public static void openIdentity(fr.minenorth.drill.network.ModNetwork.IdentityViewPacket p) { Minecraft.getInstance().setScreen(new IdentityScreen(p)); }

    public static void openJobMenu(fr.minenorth.drill.network.ModNetwork.JobViewPacket p) { Minecraft.getInstance().setScreen(new JobMenuScreen(p)); }

    public static void setProgress(int index, int total) {
        if (Minecraft.getInstance().screen instanceof HackingScreen screen) {
            screen.setProgress(index, total);
        }
    }
}
