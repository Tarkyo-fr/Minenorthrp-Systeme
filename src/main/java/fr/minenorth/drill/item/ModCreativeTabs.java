package fr.minenorth.drill.item;

import fr.minenorth.drill.MineNorthDrill;
import fr.minenorth.drill.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MineNorthDrill.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MINENORTH_TAB = TABS.register("minenorth_drill", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.minenorthdrill"))
                    .icon(() -> new ItemStack(ModItems.DRILL.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.IDENTITY_CARD.get());
                        output.accept(ModItems.HACKER.get());
                        output.accept(ModItems.DRILL.get());
                        output.accept(ModItems.DRILL_MK2.get());
                        output.accept(ModItems.DRILL_MK3.get());
                        output.accept(ModBlocks.BANK_VAULT.get());
                    })
                    .build()
    );
}
