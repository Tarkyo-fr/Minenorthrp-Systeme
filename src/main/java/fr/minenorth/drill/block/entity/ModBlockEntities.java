package fr.minenorth.drill.block.entity;

import fr.minenorth.drill.MineNorthDrill;
import fr.minenorth.drill.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MineNorthDrill.MOD_ID);

    public static final RegistryObject<BlockEntityType<BankVaultBlockEntity>> BANK_VAULT =
            BLOCK_ENTITIES.register("bank_vault", () -> BlockEntityType.Builder.of(
                    BankVaultBlockEntity::new,
                    ModBlocks.BANK_VAULT.get()
            ).build(null));
}
