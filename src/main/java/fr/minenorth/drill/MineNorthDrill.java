package fr.minenorth.drill;

import fr.minenorth.drill.block.ModBlocks;
import fr.minenorth.drill.config.ModConfig;
import fr.minenorth.drill.config.SystemeConfigFiles;
import fr.minenorth.drill.block.entity.ModBlockEntities;
import fr.minenorth.drill.item.ModItems;
import fr.minenorth.drill.item.ModCreativeTabs;
import fr.minenorth.drill.item.ModSounds;
import fr.minenorth.drill.network.ModNetwork;
import fr.minenorth.drill.job.JobConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

@Mod(MineNorthDrill.MOD_ID)
public class MineNorthDrill {
    public static final String MOD_ID = "minenorthdrill";

    public MineNorthDrill() {
        SystemeConfigFiles.init();
        JobConfig.load();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModCreativeTabs.TABS.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModNetwork.register();
        FMLJavaModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC, "minenorthdrill-common.toml");
    }
}
