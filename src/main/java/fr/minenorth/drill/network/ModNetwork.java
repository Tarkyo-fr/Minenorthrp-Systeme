package fr.minenorth.drill.network;

import fr.minenorth.drill.MineNorthDrill;
import fr.minenorth.drill.bank.MainBankManager;
import fr.minenorth.drill.door.OwnerDoorManager;
import fr.minenorth.drill.door.OwnerDoorData;
import fr.minenorth.drill.document.IdentityData;
import fr.minenorth.drill.document.IdentityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;
import java.util.function.Supplier;

public final class ModNetwork {
    private ModNetwork() {}
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MineNorthDrill.MOD_ID, "network"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, OpenHackPacket.class, OpenHackPacket::encode, OpenHackPacket::decode, OpenHackPacket::handle);
        CHANNEL.registerMessage(id++, HackClickPacket.class, HackClickPacket::encode, HackClickPacket::decode, HackClickPacket::handle);
        CHANNEL.registerMessage(id++, CloseHackPacket.class, CloseHackPacket::encode, CloseHackPacket::decode, CloseHackPacket::handle);
        CHANNEL.registerMessage(id++, HackProgressPacket.class, HackProgressPacket::encode, HackProgressPacket::decode, HackProgressPacket::handle);
        CHANNEL.registerMessage(id++, DoorPanelPacket.class, DoorPanelPacket::encode, DoorPanelPacket::decode, DoorPanelPacket::handle);
        CHANNEL.registerMessage(id++, DoorActionPacket.class, DoorActionPacket::encode, DoorActionPacket::decode, DoorActionPacket::handle);
        CHANNEL.registerMessage(id++, DoorLockSyncPacket.class, DoorLockSyncPacket::encode, DoorLockSyncPacket::decode, DoorLockSyncPacket::handle);
        CHANNEL.registerMessage(id++, DoorInteractPacket.class, DoorInteractPacket::encode, DoorInteractPacket::decode, DoorInteractPacket::handle);
        CHANNEL.registerMessage(id++, IdentityViewPacket.class, IdentityViewPacket::encode, IdentityViewPacket::decode, IdentityViewPacket::handle);
        CHANNEL.registerMessage(id++, IdentitySavePacket.class, IdentitySavePacket::encode, IdentitySavePacket::decode, IdentitySavePacket::handle);
        CHANNEL.registerMessage(id++, IdentityLostPacket.class, IdentityLostPacket::encode, IdentityLostPacket::decode, IdentityLostPacket::handle);
        CHANNEL.registerMessage(id++, JobViewPacket.class, JobViewPacket::encode, JobViewPacket::decode, JobViewPacket::handle);
        CHANNEL.registerMessage(id++, JobSelectPacket.class, JobSelectPacket::encode, JobSelectPacket::decode, JobSelectPacket::handle);
    }

    public static void sendOpenHack(ServerPlayer player, List<Integer> sequence) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenHackPacket(sequence));
    }
    public static void sendCloseHack(ServerPlayer player) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseHackPacket()); }
    public static void sendHackProgress(ServerPlayer player, int index, int total) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HackProgressPacket(index, total)); }

    public static void sendDoorLocks(ServerPlayer player, java.util.Collection<String> locked) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DoorLockSyncPacket(new java.util.ArrayList<>(locked)));
    }

    public static void syncDoorLocks(net.minecraft.server.MinecraftServer server) {
        var data = fr.minenorth.drill.door.OwnerDoorData.get(server.overworld());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            java.util.List<String> locked = new java.util.ArrayList<>();
            for (var entry : data.allDoors()) {
                var door = entry.getValue();
                boolean allowed = switch (door.type()) {
                    case PERSONAL -> door.owner() != null && data.isTrusted(door.owner(), player.getUUID());
                    case POLICE -> fr.minenorth.drill.door.OwnerDoorManager.hasPermission(player, "grade.police");
                    case POMPIER -> fr.minenorth.drill.door.OwnerDoorManager.hasPermission(player, "grade.pompier");
                    case ENTREPRISE, ORGANISATION -> !door.permission().isBlank() && fr.minenorth.drill.door.OwnerDoorManager.hasPermission(player, door.permission());
                };
                if (!allowed) locked.add(entry.getKey().dimension().location() + "|" + entry.getKey().pos().asLong());
            }
            sendDoorLocks(player, locked);
        }
    }

    public static void sendDoorPanel(ServerPlayer player, net.minecraft.server.level.ServerLevel level, BlockPos pos, int mode, String ownerName, List<String> trusted, List<String> requests, OwnerDoorData.DoorType type, String permission, boolean temporary) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DoorPanelPacket(level.dimension().location().toString(), pos.asLong(), mode, ownerName, trusted, requests, type.name(), permission, temporary));
    }


    public record OpenHackPacket(List<Integer> sequence) {
        static void encode(OpenHackPacket p, FriendlyByteBuf b) { b.writeVarInt(p.sequence.size()); p.sequence.forEach(b::writeVarInt); }
        static OpenHackPacket decode(FriendlyByteBuf b) { int n=b.readVarInt(); java.util.ArrayList<Integer> s=new java.util.ArrayList<>(); for(int i=0;i<n;i++) s.add(b.readVarInt()); return new OpenHackPacket(s); }
        static void handle(OpenHackPacket p, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.drill.client.ClientNetworkHandler.openHack(p.sequence))); c.get().setPacketHandled(true); }
    }
    public record HackClickPacket(int cell) {
        static void encode(HackClickPacket p, FriendlyByteBuf b) { b.writeVarInt(p.cell); }
        static HackClickPacket decode(FriendlyByteBuf b) { return new HackClickPacket(b.readVarInt()); }
        static void handle(HackClickPacket p, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> { if(c.get().getSender()!=null) MainBankManager.click(c.get().getSender(), p.cell); }); c.get().setPacketHandled(true); }
    }
    public record CloseHackPacket() {
        static void encode(CloseHackPacket p, FriendlyByteBuf b) {}
        static CloseHackPacket decode(FriendlyByteBuf b) { return new CloseHackPacket(); }
        static void handle(CloseHackPacket p, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> { if (c.get().getSender() != null) MainBankManager.cancel(c.get().getSender().getUUID()); DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.drill.client.ClientNetworkHandler.closeHack()); }); c.get().setPacketHandled(true); }
    }
    public record HackProgressPacket(int index, int total) {
        static void encode(HackProgressPacket p, FriendlyByteBuf b) { b.writeVarInt(p.index); b.writeVarInt(p.total); }
        static HackProgressPacket decode(FriendlyByteBuf b) { return new HackProgressPacket(b.readVarInt(), b.readVarInt()); }
        static void handle(HackProgressPacket p, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.drill.client.ClientNetworkHandler.setProgress(p.index, p.total))); c.get().setPacketHandled(true); }
    }
    public record DoorPanelPacket(String dimension, long pos, int mode, String ownerName, List<String> trusted, List<String> requests, String type, String permission, boolean temporary) {
        static void encode(DoorPanelPacket p, FriendlyByteBuf b){b.writeUtf(p.dimension);b.writeLong(p.pos);b.writeVarInt(p.mode);b.writeUtf(p.ownerName);b.writeCollection(p.trusted,(buf,v)->buf.writeUtf(v));b.writeCollection(p.requests,(buf,v)->buf.writeUtf(v));b.writeUtf(p.type);b.writeUtf(p.permission);b.writeBoolean(p.temporary);}
        static DoorPanelPacket decode(FriendlyByteBuf b){String d=b.readUtf();long pos=b.readLong();int m=b.readVarInt();String o=b.readUtf();List<String> t=b.readList(FriendlyByteBuf::readUtf);List<String> r=b.readList(FriendlyByteBuf::readUtf);String type=b.readUtf();String permission=b.readUtf();boolean temporary=b.readBoolean();return new DoorPanelPacket(d,pos,m,o,t,r,type,permission,temporary);}
        static void handle(DoorPanelPacket p,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->fr.minenorth.drill.client.ClientNetworkHandler.openDoorPanel(p)));c.get().setPacketHandled(true);}
    }
    public record DoorActionPacket(int action,String text,boolean flag,String extra) {
        static void encode(DoorActionPacket p,FriendlyByteBuf b){b.writeVarInt(p.action);b.writeUtf(p.text);b.writeBoolean(p.flag);b.writeUtf(p.extra==null?"":p.extra);}
        static DoorActionPacket decode(FriendlyByteBuf b){return new DoorActionPacket(b.readVarInt(),b.readUtf(),b.readBoolean(),b.readUtf());}
        static void handle(DoorActionPacket p,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{ServerPlayer sp=c.get().getSender();if(sp==null)return;fr.minenorth.drill.door.OwnerDoorManager.handleAction(sp,p.action(),p.text(),p.flag(),p.extra());});c.get().setPacketHandled(true);}
    }

    public record DoorInteractPacket(long pos, boolean sneaking) {
        static void encode(DoorInteractPacket p, FriendlyByteBuf b) { b.writeLong(p.pos); b.writeBoolean(p.sneaking); }
        static DoorInteractPacket decode(FriendlyByteBuf b) { return new DoorInteractPacket(b.readLong(), b.readBoolean()); }
        static void handle(DoorInteractPacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> {
                ServerPlayer sp = c.get().getSender();
                if (sp != null) OwnerDoorManager.handleClientInteract(sp, BlockPos.of(p.pos), p.sneaking);
            });
            c.get().setPacketHandled(true);
        }
    }

    public static void sendIdentityCreation(ServerPlayer player, java.util.UUID subjectUuid) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IdentityViewPacket(
                false, true, false, true, subjectUuid, "", "", "", "", "", "", ""
        ));
    }

    public static void sendIdentityAlreadyExists(ServerPlayer player, IdentityData.Profile profile, java.util.UUID subjectUuid) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IdentityViewPacket(
                true, false, false, true, subjectUuid, "", profile.lastName(), profile.firstName(),
                profile.birthDate(), profile.birthPlace(), profile.nationality(), profile.cardNumber()
        ));
    }

    public static void sendIdentityView(ServerPlayer player, IdentityData.Profile profile,
                                        java.util.UUID subjectUuid, String presenter,
                                        boolean staff, boolean creation) {
        if (profile == null) {
            sendIdentityCreation(player, subjectUuid);
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IdentityViewPacket(
                true, false, staff, creation, subjectUuid, presenter,
                profile.lastName(), profile.firstName(), profile.birthDate(),
                profile.birthPlace(), profile.nationality(), profile.cardNumber()
        ));
    }

    public record IdentityViewPacket(
            boolean exists,
            boolean editable,
            boolean staff,
            boolean creation,
            java.util.UUID subjectUuid,
            String presenter,
            String lastName,
            String firstName,
            String birthDate,
            String birthPlace,
            String nationality,
            String cardNumber
    ) {
        static void encode(IdentityViewPacket p, FriendlyByteBuf b) {
            b.writeBoolean(p.exists);
            b.writeBoolean(p.editable);
            b.writeBoolean(p.staff);
            b.writeBoolean(p.creation);
            b.writeUUID(p.subjectUuid);
            b.writeUtf(p.presenter);
            b.writeUtf(p.lastName);
            b.writeUtf(p.firstName);
            b.writeUtf(p.birthDate);
            b.writeUtf(p.birthPlace);
            b.writeUtf(p.nationality);
            b.writeUtf(p.cardNumber);
        }

        static IdentityViewPacket decode(FriendlyByteBuf b) {
            return new IdentityViewPacket(
                    b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(),
                    b.readUUID(), b.readUtf(), b.readUtf(), b.readUtf(), b.readUtf(),
                    b.readUtf(), b.readUtf(), b.readUtf()
            );
        }

        static void handle(IdentityViewPacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT, () -> () -> fr.minenorth.drill.client.ClientNetworkHandler.openIdentity(p)
            ));
            c.get().setPacketHandled(true);
        }
    }

    public record IdentitySavePacket(
            String lastName, String firstName, String birthDate, String birthPlace, String nationality
    ) {
        static void encode(IdentitySavePacket p, FriendlyByteBuf b) {
            b.writeUtf(p.lastName);
            b.writeUtf(p.firstName);
            b.writeUtf(p.birthDate);
            b.writeUtf(p.birthPlace);
            b.writeUtf(p.nationality);
        }

        static IdentitySavePacket decode(FriendlyByteBuf b) {
            return new IdentitySavePacket(b.readUtf(), b.readUtf(), b.readUtf(), b.readUtf(), b.readUtf());
        }

        static void handle(IdentitySavePacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> {
                ServerPlayer sp = c.get().getSender();
                if (sp != null) IdentityManager.save(
                        sp, p.lastName(), p.firstName(), p.birthDate(), p.birthPlace(), p.nationality()
                );
            });
            c.get().setPacketHandled(true);
        }
    }

    public record IdentityLostPacket() {
        static void encode(IdentityLostPacket p, FriendlyByteBuf b) {}
        static IdentityLostPacket decode(FriendlyByteBuf b) { return new IdentityLostPacket(); }
        static void handle(IdentityLostPacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> {
                ServerPlayer sp = c.get().getSender();
                if (sp != null) IdentityManager.reissueLostCard(sp);
            });
            c.get().setPacketHandled(true);
        }
    }

    public record DoorLockSyncPacket(List<String> locked) {
        static void encode(DoorLockSyncPacket p, FriendlyByteBuf b) { b.writeCollection(p.locked, (buf, v) -> buf.writeUtf(v)); }
        static DoorLockSyncPacket decode(FriendlyByteBuf b) { return new DoorLockSyncPacket(b.readList(FriendlyByteBuf::readUtf)); }
        static void handle(DoorLockSyncPacket p, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.drill.client.DoorLockClientHandler.setLocked(p.locked()))); c.get().setPacketHandled(true); }
    }

    public static void sendJobMenu(ServerPlayer player, java.util.List<fr.minenorth.drill.job.JobConfig.Job> jobs, String currentJob) {
        java.util.ArrayList<JobInfo> info = new java.util.ArrayList<>();
        for (var j : jobs) info.add(new JobInfo(j.id, j.name, j.description, j.icon, j.max, j.salary, j.candidature, j.premium, j.permission));
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new JobViewPacket(info, currentJob, fr.minenorth.drill.job.JobConfig.salaryEnabled(), fr.minenorth.drill.job.JobConfig.intervalMinutes()));
    }

    public record JobInfo(String id, String name, String description, String icon, int max, int salary, boolean candidature, boolean premium, String permission) {
        static void encode(JobInfo j, FriendlyByteBuf b) { b.writeUtf(j.id); b.writeUtf(j.name); b.writeUtf(j.description); b.writeUtf(j.icon); b.writeVarInt(j.max); b.writeVarInt(j.salary); b.writeBoolean(j.candidature); b.writeBoolean(j.premium); b.writeUtf(j.permission); }
        static JobInfo decode(FriendlyByteBuf b) { return new JobInfo(b.readUtf(), b.readUtf(), b.readUtf(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readUtf()); }
    }

    public record JobViewPacket(java.util.List<JobInfo> jobs, String currentJob, boolean salaryEnabled, int intervalMinutes) {
        static void encode(JobViewPacket p, FriendlyByteBuf b) { b.writeCollection(p.jobs, (buf, j) -> JobInfo.encode(j, buf)); b.writeUtf(p.currentJob); b.writeBoolean(p.salaryEnabled); b.writeVarInt(p.intervalMinutes); }
        static JobViewPacket decode(FriendlyByteBuf b) { return new JobViewPacket(b.readList(JobInfo::decode), b.readUtf(), b.readBoolean(), b.readVarInt()); }
        static void handle(JobViewPacket p, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.drill.client.ClientNetworkHandler.openJobMenu(p))); c.get().setPacketHandled(true); }
    }

    public record JobSelectPacket(String id) {
        static void encode(JobSelectPacket p, FriendlyByteBuf b) { b.writeUtf(p.id); }
        static JobSelectPacket decode(FriendlyByteBuf b) { return new JobSelectPacket(b.readUtf()); }
        static void handle(JobSelectPacket p, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> { ServerPlayer sp = c.get().getSender(); if (sp != null) fr.minenorth.drill.job.JobManager.selectById(sp, p.id); }); c.get().setPacketHandled(true); }
    }

}
