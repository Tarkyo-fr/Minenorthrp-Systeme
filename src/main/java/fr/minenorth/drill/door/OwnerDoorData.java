package fr.minenorth.drill.door;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.registries.Registries;

import java.util.*;

public class OwnerDoorData extends SavedData {
    private static final String DATA_NAME = "minenorthdrill_owner_doors";
    private final Map<DoorKey, DoorEntry> doors = new HashMap<>();
    private final Map<UUID, Set<UUID>> trusted = new HashMap<>();
    private final Map<UUID, Map<UUID, DoorKey>> requests = new HashMap<>();

    public enum DoorType {
        PERSONAL, POLICE, POMPIER, ENTREPRISE, ORGANISATION;
        public static DoorType from(String value) {
            try { return valueOf(value.toUpperCase(Locale.ROOT)); } catch (Exception e) { return PERSONAL; }
        }
    }

    public static OwnerDoorData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(OwnerDoorData::load, OwnerDoorData::new, DATA_NAME);
    }

    private static OwnerDoorData load(CompoundTag tag) {
        OwnerDoorData d = new OwnerDoorData();
        ListTag list = tag.getList("Doors", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            ResourceLocation dim = ResourceLocation.tryParse(e.getString("Dim"));
            if (dim == null) continue;
            UUID owner = parse(e.getString("Owner"));
            String ownerName = e.getString("OwnerName");
            DoorType type = DoorType.from(e.getString("Type"));
            String permission = e.getString("Permission");
            d.doors.put(new DoorKey(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(e.getLong("Pos"))),
                    new DoorEntry(owner, e.getBoolean("Temporary"), ownerName, type, permission));
        }
        ListTag tl = tag.getList("Trusted", 10);
        for (int i = 0; i < tl.size(); i++) {
            CompoundTag e = tl.getCompound(i);
            UUID owner = parse(e.getString("Owner"));
            if (owner == null) continue;
            Set<UUID> s = new HashSet<>();
            ListTag ids = e.getList("Players", 8);
            for (int j = 0; j < ids.size(); j++) {
                UUID u = parse(ids.getString(j));
                if (u != null) s.add(u);
            }
            d.trusted.put(owner, s);
        }
        return d;
    }

    private static UUID parse(String s) { try { return UUID.fromString(s); } catch (Exception e) { return null; } }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag dl = new ListTag();
        for (var en : doors.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putString("Dim", en.getKey().dimension.location().toString());
            e.putLong("Pos", en.getKey().pos.asLong());
            if (en.getValue().owner != null) e.putString("Owner", en.getValue().owner.toString());
            e.putBoolean("Temporary", en.getValue().temporary);
            e.putString("OwnerName", en.getValue().ownerName);
            e.putString("Type", en.getValue().type.name());
            e.putString("Permission", en.getValue().permission);
            dl.add(e);
        }
        tag.put("Doors", dl);

        ListTag tl = new ListTag();
        for (var en : trusted.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putString("Owner", en.getKey().toString());
            ListTag ids = new ListTag();
            for (UUID u : en.getValue()) ids.add(net.minecraft.nbt.StringTag.valueOf(u.toString()));
            e.put("Players", ids);
            tl.add(e);
        }
        tag.put("Trusted", tl);
        return tag;
    }

    public static BlockPos normalize(ServerLevel level, BlockPos pos) {
        var s = level.getBlockState(pos);
        if (s.getBlock() instanceof DoorBlock && s.hasProperty(DoorBlock.HALF) && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) return pos.below();
        return pos;
    }

    public DoorEntry get(ServerLevel level, BlockPos pos) { return doors.get(new DoorKey(level.dimension(), normalize(level, pos))); }

    public void assign(ServerLevel level, BlockPos pos, UUID uuid, String name, boolean temporary, DoorType type, String permission) {
        doors.put(new DoorKey(level.dimension(), normalize(level, pos)), new DoorEntry(uuid, temporary, name, type, permission == null ? "" : permission));
        setDirty();
    }

    public void unassign(ServerLevel level, BlockPos pos) { doors.remove(new DoorKey(level.dimension(), normalize(level, pos))); setDirty(); }

    public boolean isTrusted(UUID owner, UUID player) { return owner != null && (owner.equals(player) || trusted.getOrDefault(owner, Collections.emptySet()).contains(player)); }
    public boolean addTrusted(UUID owner, UUID player) { boolean a = trusted.computeIfAbsent(owner, k -> new HashSet<>()).add(player); if (a) setDirty(); return a; }
    public boolean removeTrusted(UUID owner, UUID player) { Set<UUID> s = trusted.get(owner); boolean a = s != null && s.remove(player); if (a) setDirty(); return a; }
    public Set<UUID> trusted(UUID owner) { return Set.copyOf(trusted.getOrDefault(owner, Collections.emptySet())); }

    public void request(ServerLevel level, BlockPos pos, UUID requester, UUID owner) { requests.computeIfAbsent(owner, k -> new HashMap<>()).put(requester, new DoorKey(level.dimension(), normalize(level, pos))); setDirty(); }
    public Map<UUID, DoorKey> requests(UUID owner) { return Map.copyOf(requests.getOrDefault(owner, Collections.emptyMap())); }
    public DoorKey popRequest(UUID owner, UUID requester) { Map<UUID, DoorKey> m = requests.get(owner); if (m == null) return null; DoorKey k = m.remove(requester); setDirty(); return k; }
    public void clearTemporary(UUID owner) { doors.entrySet().removeIf(e -> e.getValue().temporary && owner.equals(e.getValue().owner)); setDirty(); }
    public Collection<Map.Entry<DoorKey, DoorEntry>> allDoors() { return List.copyOf(doors.entrySet()); }

    public void replaceDoors(Collection<Map.Entry<DoorKey, DoorEntry>> entries) {
        doors.clear();
        for (var entry : entries) doors.put(entry.getKey(), entry.getValue());
        setDirty();
    }

    public record DoorKey(ResourceKey<Level> dimension, BlockPos pos) {}
    public record DoorEntry(UUID owner, boolean temporary, String ownerName, DoorType type, String permission) {}
}
