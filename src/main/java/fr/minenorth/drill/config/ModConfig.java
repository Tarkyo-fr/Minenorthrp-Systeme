package fr.minenorth.drill.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ModConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BANK_VAULT_LOOT;
    private static final String FILE_NAME = "Minenorth-systeme/coffre.toml";

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("bank_vault");
        BANK_VAULT_LOOT = builder
                .comment(
                        "Liste des objets pouvant apparaitre dans un coffre de banque.",
                        "Syntaxe : item_id|chance_en_pourcentage|min-max",
                        "Exemple : minecraft:diamond|25|1-3",
                        "La chance est comprise entre 0 et 100.",
                        "La quantite est tiree aleatoirement entre min et max.",
                        "Chaque ligne est testee independamment : plusieurs types d'objets peuvent donc apparaitre dans le meme coffre."
                )
                .defineListAllowEmpty(
                        "loot_entries",
                        Arrays.asList(
                                "minecraft:gold_ingot|75|2-8",
                                "minecraft:iron_ingot|55|4-16",
                                "minecraft:emerald|40|1-5",
                                "minecraft:diamond|20|1-3",
                                "minecraft:gold_nugget|60|4-16"
                        ),
                        value -> value instanceof String && !((String) value).trim().isEmpty()
                );
        builder.pop();

        SPEC = builder.build();
    }

    public static boolean reload() {
        try {
            Path path = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
            CommentedFileConfig file = CommentedFileConfig.builder(path)
                    .sync()
                    .autosave()
                    .writingMode(WritingMode.REPLACE)
                    .build();
            file.load();
            SPEC.setConfig(file);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
