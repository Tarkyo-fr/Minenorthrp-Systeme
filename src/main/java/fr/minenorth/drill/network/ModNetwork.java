package fr.minenorth.drill.network;

import fr.minenorth.drill.MineNorthDrill;
import fr.minenorth.drill.bank.MainBankManager;
import fr.minenorth.drill.door.OwnerDoorManager;
import fr.minenorth.drill.door.OwnerDoorData;
import fr.minenorth.drill.document.IdentityData;
import fr.minenorth.drill.document.IdentityManager;
import fr.minenorth.drill.job.JobConfig;
import fr.minenorth.drill.job.JobManager;
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
                boolean allowed;
                if (door.type() == OwnerDoorData.DoorType.PERSONAL) {
                    boolean renter = data.listing(entry.getKey()) != null
                            && data.listing(entry.getKey()).rentalActive()
                            && player.getUUID().equals(data.listing(entry.getKey()).renter());
                    allowed = door.owner() != null && (data.isTrusted(door.owner(), player.getUUID()) || renter);
                } else {
                    allowed = switch (door.type()) {
                        case POLICE -> fr.minenorth.drill.door.OwnerDoorManager.hasPermission(player, "grade.police");
                        case POMPIER -> fr.minenorth.drill.door.OwnerDoorManager.hasPermission(player, "grade.pompier");
                        case ENTREPRISE, ORGANISATION -> !door.permission().isBlank() && fr.minenorth.drill.door.OwnerDoorManager.hasPermission(player, door.permission());
                        default -> false;
                    };
                }
                if (!allowed) locked.add(entry.getKey().dimension().location() + "|" + entry.getKey().pos().asLong());
            }
            sendDoorLocks(player, locked);
        }
    }

    public static void sendDoorPanel(ServerPlayer player, net.minecraft.server.level.ServerLevel level, BlockPos pos, int mode, String ownerName, List<String> trusted, List<String> requests, OwnerDoorData.DoorType type, String permission, boolean temporary) {
        var listing = OwnerDoorData.get(level).listing(level, pos);
        String houseName = "";
        OwnerDoorData.DoorEntry doorEntry = OwnerDoorData.get(level).get(level, pos);
        if (doorEntry != null) houseName = doorEntry.houseName();
        String listingMode = listing == null ? "NONE" : listing.mode().name();
        long listingPrice = listing == null ? 0 : listing.price();
        int listingDays = listing == null ? 0 : listing.days();
        String renter = "";
        long expiry = listing == null ? 0 : listing.expiryMs();
        if (listing != null && listing.renter() != null) {
            ServerPlayer rp = player.server.getPlayerList().getPlayer(listing.renter());
            renter = rp == null ? listing.renter().toString() : rp.getGameProfile().getName();
        }
        long purchasePrice = doorEntry == null ? 0 : doorEntry.purchasePrice();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DoorPanelPacket(level.dimension().location().toString(), pos.asLong(), mode, ownerName, trusted, requests, type.name(), permission, temporary, houseName, listingMode, listingPrice, listingDays, renter, expiry, purchasePrice));
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
    public record DoorPanelPacket(String dimension, long pos, int mode, String ownerName, List<String> trusted, List<String> requests, String type, String permission, boolean temporary, String houseName, String listingMode, long listingPrice, int listingDays, String renter, long expiryMs, long purchasePrice) {
        static void encode(DoorPanelPacket p, FriendlyByteBuf b){b.writeUtf(p.dimension);b.writeLong(p.pos);b.writeVarInt(p.mode);b.writeUtf(p.ownerName);b.writeCollection(p.trusted,(buf,v)->buf.writeUtf(v));b.writeCollection(p.requests,(buf,v)->buf.writeUtf(v));b.writeUtf(p.type);b.writeUtf(p.permission);b.writeBoolean(p.temporary);b.writeUtf(p.houseName);b.writeUtf(p.listingMode);b.writeLong(p.listingPrice);b.writeVarInt(p.listingDays);b.writeUtf(p.renter);b.writeLong(p.expiryMs);b.writeLong(p.purchasePrice);}
        static DoorPanelPacket decode(FriendlyByteBuf b){String d=b.readUtf();long pos=b.readLong();int m=b.readVarInt();String o=b.readUtf();List<String> t=b.readList(FriendlyByteBuf::readUtf);List<String> r=b.readList(FriendlyByteBuf::readUtf);String type=b.readUtf();String permission=b.readUtf();boolean temporary=b.readBoolean();String house=b.readUtf();String lm=b.readUtf();long lp=b.readLong();int ld=b.readVarInt();String renter=b.readUtf();long exp=b.readLong();long pp=b.readLong();return new DoorPanelPacket(d,pos,m,o,t,r,type,permission,temporary,house,lm,lp,ld,renter,exp,pp);}
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


    public static void sendJobMenu(ServerPlayer player) {
        java.util.ArrayList<JobInfo> jobs = new java.util.ArrayList<>();
        for (JobConfig.Job j : JobConfig.jobs()) {
            jobs.add(new JobInfo(j.id, j.name, j.description, j.icon, j.max, j.salary, j.premium, j.candidature, j.permission));
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new JobViewPacket(
                jobs, JobManager.getJob(player), JobConfig.salaryEnabled(), JobConfig.intervalMinutes()));
    }

    public record JobInfo(String id, String name, String description, String icon, int max, int salary, boolean premium, boolean candidature, String permission) {}

    public record JobViewPacket(List<JobInfo> jobs, String currentJob, boolean salaryEnabled, int intervalMinutes) {
        static void encode(JobViewPacket p, FriendlyByteBuf b) {
            b.writeVarInt(p.jobs.size());
            for (JobInfo j : p.jobs) {
                b.writeUtf(j.id); b.writeUtf(j.name); b.writeUtf(j.description); b.writeUtf(j.icon);
                b.writeVarInt(j.max); b.writeVarInt(j.salary); b.writeBoolean(j.premium); b.writeBoolean(j.candidature); b.writeUtf(j.permission);
            }
            b.writeUtf(p.currentJob); b.writeBoolean(p.salaryEnabled); b.writeVarInt(p.intervalMinutes);
        }
        static JobViewPacket decode(FriendlyByteBuf b) {
            int n=b.readVarInt(); java.util.ArrayList<JobInfo> jobs=new java.util.ArrayList<>();
            for(int i=0;i<n;i++) jobs.add(new JobInfo(b.readUtf(),b.readUtf(),b.readUtf(),b.readUtf(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readUtf()));
            return new JobViewPacket(jobs,b.readUtf(),b.readBoolean(),b.readVarInt());
        }
        static void handle(JobViewPacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.drill.client.ClientNetworkHandler.openJobs(p)));
            c.get().setPacketHandled(true);
        }
    }

    public record JobSelectPacket(String jobId) {
        static void encode(JobSelectPacket p, FriendlyByteBuf b) { b.writeUtf(p.jobId); }
        static JobSelectPacket decode(FriendlyByteBuf b) { return new JobSelectPacket(b.readUtf()); }
        static void handle(JobSelectPacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> { ServerPlayer sp=c.get().getSender(); if(sp!=null){ JobConfig.Job j=JobConfig.byId(p.jobId); if(j!=null) JobManager.select(sp,j); } });
            c.get().setPacketHandled(true);
        }
    }

    public record DoorLockSyncPacket(List<String> locked) {
        static void encode(DoorLockSyncPacket p, FriendlyByteBuf b) { b.writeCollection(p.locked, (buf, v) -> buf.writeUtf(v)); }
        static DoorLockSyncPacket decode(FriendlyByteBuf b) { return new DoorLockSyncPacket(b.readList(FriendlyByteBuf::readUtf)); }
        static void handle(DoorLockSyncPacket p, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.drill.client.DoorLockClientHandler.setLocked(p.locked()))); c.get().setPacketHandled(true); }
    }

}
