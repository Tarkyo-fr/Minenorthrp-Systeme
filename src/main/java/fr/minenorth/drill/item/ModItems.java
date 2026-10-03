package fr.minenorth.drill.item;

import fr.minenorth.drill.MineNorthDrill;
import fr.minenorth.drill.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import fr.minenorth.drill.document.DocumentItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MineNorthDrill.MOD_ID);

    public static final RegistryObject<Item> BANK_VAULT = ITEMS.register("bank_vault", () -> new BlockItem(
            ModBlocks.BANK_VAULT.get(), new Item.Properties()
    ));

    public static final RegistryObject<Item> HACKER = ITEMS.register("hacker", () -> new HackerItem(new Item.Properties()));

    public static final RegistryObject<Item> DRILL = ITEMS.register("drill", () -> new DrillItem(
            DrillItem.DrillLevel.I, new Item.Properties()
    ));

    public static final RegistryObject<Item> DRILL_MK2 = ITEMS.register("drill_mk2", () -> new DrillItem(
            DrillItem.DrillLevel.II, new Item.Properties()
    ));


    public static final RegistryObject<Item> IDENTITY_CARD = ITEMS.register("identity_card", () -> new DocumentItem("identity"));
    public static final RegistryObject<Item> DRIVING_LICENSE = ITEMS.register("driving_license", () -> new DocumentItem("driving"));
    public static final RegistryObject<Item> MOTORCYCLE_LICENSE = ITEMS.register("motorcycle_license", () -> new DocumentItem("motorcycle"));
    public static final RegistryObject<Item> TRUCK_LICENSE = ITEMS.register("truck_license", () -> new DocumentItem("truck"));
    public static final RegistryObject<Item> PROFESSIONAL_CARD = ITEMS.register("professional_card", () -> new DocumentItem("professional"));
    public static final RegistryObject<Item> MEDICAL_CARD = ITEMS.register("medical_card", () -> new DocumentItem("medical"));

    public static final RegistryObject<Item> DRILL_MK3 = ITEMS.register("drill_mk3", () -> new DrillItem(
            DrillItem.DrillLevel.III, new Item.Properties()
    ));
}
