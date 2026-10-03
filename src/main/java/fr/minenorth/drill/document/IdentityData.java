package fr.minenorth.drill.document;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent, immutable RP identities. */
public class IdentityData extends SavedData {
    private static final String NAME = "minenorthsysteme_identity";
    private final Map<UUID, Profile> profiles = new HashMap<>();

    public record Profile(
            UUID uuid,
            String lastName,
            String firstName,
            String birthDate,
            String birthPlace,
            String nationality,
            String cardNumber
    ) {}

    public static IdentityData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(IdentityData::load, IdentityData::new, NAME);
    }

    public boolean exists(UUID uuid) { return profiles.containsKey(uuid); }
    public Profile get(UUID uuid) { return profiles.get(uuid); }

    public boolean profilesContainsCard(String cardNumber) {
        return profiles.values().stream().anyMatch(p -> p.cardNumber().equals(cardNumber));
    }

    public void set(Profile profile) {
        profiles.put(profile.uuid(), profile);
        setDirty();
    }

    public void reset(UUID uuid) {
        profiles.remove(uuid);
        setDirty();
    }

    private static IdentityData load(CompoundTag tag) {
        IdentityData data = new IdentityData();
        ListTag list = tag.getList("Profiles", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            try {
                UUID uuid = UUID.fromString(t.getString("UUID"));
                data.profiles.put(uuid, new Profile(
                        uuid,
                        t.getString("LastName"),
                        t.getString("FirstName"),
                        t.getString("BirthDate"),
                        t.getString("BirthPlace"),
                        t.getString("Nationality"),
                        t.getString("Card")
                ));
            } catch (Exception ignored) {}
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Profile profile : profiles.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("UUID", profile.uuid().toString());
            t.putString("LastName", profile.lastName());
            t.putString("FirstName", profile.firstName());
            t.putString("BirthDate", profile.birthDate());
            t.putString("BirthPlace", profile.birthPlace());
            t.putString("Nationality", profile.nationality());
            t.putString("Card", profile.cardNumber());
            list.add(t);
        }
        tag.put("Profiles", list);
        return tag;
    }
}
