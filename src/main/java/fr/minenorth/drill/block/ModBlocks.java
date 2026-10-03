package fr.minenorth.drill.block;

import fr.minenorth.drill.MineNorthDrill;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MineNorthDrill.MOD_ID);

    public static final RegistryObject<Block> BANK_VAULT = BLOCKS.register("bank_vault", () -> new BankVaultBlock(
            BlockBehaviour.Properties.of()
                    .strength(8.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
    ));
}
