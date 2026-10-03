package fr.minenorth.drill.item;

import fr.minenorth.drill.MineNorthDrill;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MineNorthDrill.MOD_ID);

    public static final RegistryObject<SoundEvent> DRILL_START = register("drill_start");
    public static final RegistryObject<SoundEvent> DRILL_LOOP = register("drill_loop");
    public static final RegistryObject<SoundEvent> DRILL_STOP = register("drill_stop");
    public static final RegistryObject<SoundEvent> HACK_START = register("hack_start");
    public static final RegistryObject<SoundEvent> HACK_BEEP = register("hack_beep");
    public static final RegistryObject<SoundEvent> HACK_SUCCESS = register("hack_success");
    public static final RegistryObject<SoundEvent> HACK_FAIL = register("hack_fail");

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(MineNorthDrill.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    private ModSounds() {}
}
