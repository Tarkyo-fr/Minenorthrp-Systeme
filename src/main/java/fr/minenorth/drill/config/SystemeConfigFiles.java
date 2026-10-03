package fr.minenorth.drill.config;

import fr.minenorth.drill.door.OwnerDoorData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SystemeConfigFiles {
    private static final Path ROOT = FMLPaths.CONFIGDIR.get().resolve("Minenorth-systeme");
    private static final Path BANQUE = ROOT.resolve("banque.toml");
    private static final Path COFFRE = ROOT.resolve("coffre.toml");
    private static final Path PORTE = ROOT.resolve("porte.toml");
    private static final Path DOCUMENTS = ROOT.resolve("documents.toml");

    private SystemeConfigFiles() {}

    public static void init() {
        try {
            Files.createDirectories(ROOT);
            if (!Files.exists(BANQUE)) Files.writeString(BANQUE, "# Configuration du système bancaire MineNorth\n\n", StandardCharsets.UTF_8);
            if (!Files.exists(COFFRE)) Files.writeString(COFFRE, "[bank_vault]\n# Liste des objets pouvant apparaitre dans un coffre de banque.\n# Syntaxe : item_id|chance_en_pourcentage|min-max\n# La chance est comprise entre 0 et 100.\n# Chaque ligne est testée indépendamment.\nloot_entries = [\"minecraft:gold_ingot|75|2-8\", \"minecraft:golden_apple|55|4-16\", \"minecraft:emerald|40|1-5\", \"minecraft:diamond|20|1-3\", \"minecraft:gold_nugget|60|4-16\"]\n", StandardCharsets.UTF_8);
                        if (!Files.exists(DOCUMENTS)) Files.writeString(DOCUMENTS, "[identite]\nobligatoire = true\nnationalite_par_defaut = \"Française\"\nmodification_joueur = false\ndate_expiration = false\n\n[identite.creation]\ncommande = \"cidmenu <joueur>\"\n\n[identite.photo]\nsource = \"skin_minecraft\"\n\n[permissions]\nstaff = \"cid.staff\"\n", StandardCharsets.UTF_8);
            if (!Files.exists(PORTE)) Files.writeString(PORTE, "# Portes protégées MineNorth\n# Ce fichier est généré automatiquement.\n\n", StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    public static synchronized void writeDoors(MinecraftServer server, OwnerDoorData data) {
        init();
        List<String> lines = new ArrayList<>();
        lines.add("# Portes protégées MineNorth");
        lines.add("# Généré automatiquement. Ne pas modifier pendant le serveur.");
        lines.add("");
        int i = 0;
        for (var entry : data.allDoors()) {
            var key = entry.getKey();
            var door = entry.getValue();
            lines.add("[[porte]]");
            lines.add("index = " + i++);
            lines.add("dimension = \"" + key.dimension().location() + "\"");
            lines.add("x = " + key.pos().getX());
            lines.add("y = " + key.pos().getY());
            lines.add("z = " + key.pos().getZ());
            lines.add("type = \"" + door.type().name().toLowerCase() + "\"");
            lines.add("proprietaire = \"" + escape(door.ownerName()) + "\"");
            lines.add("uuid = \"" + (door.owner() == null ? "" : door.owner()) + "\"");
            lines.add("temporaire = " + door.temporary());
            lines.add("permission = \"" + escape(door.permission()) + "\"");
            lines.add("");
        }
        try { Files.write(PORTE, lines, StandardCharsets.UTF_8); } catch (IOException ignored) {}
    }

    public static synchronized boolean reloadDoors() {
        init();
        return Files.exists(PORTE);
    }



    public static int reloadDoors(MinecraftServer server) {
        init();
        List<java.util.Map.Entry<OwnerDoorData.DoorKey, OwnerDoorData.DoorEntry>> entries = new ArrayList<>();
        try {
            if (!Files.exists(PORTE)) return 0;
            String dim = null, owner = "", uuid = "", type = "PERSONAL", permission = "";
            int x = 0, y = 0, z = 0; boolean temporary = false; boolean active = false;
            for (String raw : Files.readAllLines(PORTE, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.equals("[[porte]]")) {
                    if (active) addParsed(entries, dim, x, y, z, owner, uuid, type, permission, temporary);
                    active = true; dim = null; owner = ""; uuid = ""; type = "PERSONAL"; permission = ""; x = y = z = 0; temporary = false;
                } else if (active && line.startsWith("dimension")) dim = quoted(line);
                else if (active && line.startsWith("proprietaire")) owner = quoted(line);
                else if (active && line.startsWith("uuid")) uuid = quoted(line);
                else if (active && line.startsWith("type")) type = quoted(line);
                else if (active && line.startsWith("permission")) permission = quoted(line);
                else if (active && line.startsWith("x")) x = integer(line);
                else if (active && line.startsWith("y")) y = integer(line);
                else if (active && line.startsWith("z")) z = integer(line);
                else if (active && line.startsWith("temporaire")) temporary = line.toLowerCase().endsWith("true");
            }
            if (active) addParsed(entries, dim, x, y, z, owner, uuid, type, permission, temporary);
        } catch (Exception ignored) { return -1; }
        OwnerDoorData data = OwnerDoorData.get(server.overworld());
        data.replaceDoors(entries);
        writeDoors(server, data);
        return entries.size();
    }

    private static void addParsed(List<java.util.Map.Entry<OwnerDoorData.DoorKey, OwnerDoorData.DoorEntry>> out, String dim, int x, int y, int z, String owner, String uuid, String type, String permission, boolean temporary) {
        if (dim == null || dim.isBlank()) return;
        ResourceLocation rl = ResourceLocation.tryParse(dim);
        if (rl == null) return;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, rl);
        java.util.UUID id = null;
        try { if (!uuid.isBlank()) id = java.util.UUID.fromString(uuid); } catch (Exception ignored) {}
        OwnerDoorData.DoorEntry entry = new OwnerDoorData.DoorEntry(id, temporary, owner, OwnerDoorData.DoorType.from(type), permission);
        out.add(new java.util.AbstractMap.SimpleEntry<>(new OwnerDoorData.DoorKey(key, new BlockPos(x, y, z)), entry));
    }
    private static String quoted(String line) { int a=line.indexOf('"'), b=line.lastIndexOf('"'); return a>=0 && b>a ? line.substring(a+1,b) : ""; }
    private static int integer(String line) { int a=line.indexOf('='); try { return Integer.parseInt(line.substring(a+1).trim()); } catch (Exception e) { return 0; } }

    public static Set<String> readConfiguredPermissions() {
        init();
        Set<String> result = new LinkedHashSet<>();
        try {
            if (!Files.exists(PORTE)) return result;
            for (String line : Files.readAllLines(PORTE, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("permission")) continue;
                int first = trimmed.indexOf('"');
                int last = trimmed.lastIndexOf('"');
                if (first >= 0 && last > first) {
                    String value = trimmed.substring(first + 1, last).trim();
                    if (!value.isBlank()) result.add(value);
                }
            }
        } catch (IOException ignored) {}
        return result;
    }

    public static Path getRoot() { return ROOT; }
    private static String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
