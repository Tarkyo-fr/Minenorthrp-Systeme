package fr.minenorth.drill.tags;

import fr.minenorth.drill.MineNorthDrill;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static final TagKey<Block> DRILLABLE_BLOCKS = TagKey.create(
            Registries.BLOCK,
            new ResourceLocation(MineNorthDrill.MOD_ID, "drillable")
    );
}
