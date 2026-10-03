package fr.minenorth.drill.job;

import fr.minenorth.drill.config.SystemeConfigFiles;
import fr.minenorth.drill.MineNorthDrill;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public final class JobManager {
    private static final String TAG = "MineNorthJob";
    private static final String TITLE = "§1Pôle Emploi";
    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();
    private static long lastSalaryTick = 0;
    private static final Map<String, PermissionNode<Boolean>> PERMISSION_NODES = new HashMap<>();
    private JobManager() {}

    public static void openMenu(ServerPlayer player) {
        if (!JobConfig.enabled()) { player.displayClientMessage(Component.literal("§cLe Pôle Emploi est actuellement désactivé."), true); return; }
        fr.minenorth.drill.network.ModNetwork.sendJobMenu(player, JobConfig.jobs(), getJob(player));
    }

    public static void selectById(ServerPlayer player, String id) {
        JobConfig.Job job = JobConfig.byId(id);
        if (job == null) { player.displayClientMessage(Component.literal("§cCe métier n'existe plus dans la configuration."), true); return; }
        select(player, job);
    }

    private static int onlineCount(String id) { int n=0; for(ServerPlayer p:currentServerPlayers()) if(id.equals(getJob(p)))n++; return n; }
    private static Collection<ServerPlayer> currentServerPlayers(){ return CURRENT_SERVER==null?List.of():CURRENT_SERVER.getPlayerList().getPlayers(); }
    private static MinecraftServer CURRENT_SERVER;

    public static String getJob(ServerPlayer p){ String s=p.getPersistentData().getString(TAG); return s.isBlank()?JobConfig.defaultJob():s; }
    public static JobConfig.Job current(ServerPlayer p){ return JobConfig.byId(getJob(p)); }

    @SubscribeEvent
    public static void gatherPermissions(PermissionGatherEvent.Nodes event) {
        registerPermission(event, JobConfig.premiumPermission());
        for (JobConfig.Job job : JobConfig.jobs()) registerPermission(event, job.permission);
    }

    private static void registerPermission(PermissionGatherEvent.Nodes event, String permission) {
        if (permission == null || permission.isBlank() || PERMISSION_NODES.containsKey(permission)) return;
        PermissionNode<Boolean> node = new PermissionNode<>(MineNorthDrill.MOD_ID, permission, PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null && player.hasPermissions(2));
        PERMISSION_NODES.put(permission, node);
        event.addNodes(node);
    }

    private static boolean hasPermission(ServerPlayer p, String permission) {
        if (p.hasPermissions(2)) return true;
        PermissionNode<Boolean> node = PERMISSION_NODES.get(permission);
        if (node == null) return false;
        try { return PermissionAPI.getPermission(p, node); } catch (RuntimeException ignored) { return false; }
    }

    public static void select(ServerPlayer p, JobConfig.Job job) {
        JobConfig.Job old=current(p);
        if(job.candidature){p.displayClientMessage(Component.literal("§eCe métier est accessible sur candidature. Rends-toi sur le Discord de MineNorth RP pour postuler."),false);return;}
        if(job.premium && !p.hasPermissions(2) && !hasPermission(p, JobConfig.premiumPermission())) {p.displayClientMessage(Component.literal("§dCe métier est réservé aux membres Premium."),false);return;}
        if(!job.permission.isBlank() && !p.hasPermissions(2) && !hasPermission(p, job.permission)) {p.displayClientMessage(Component.literal("§cVous n'avez pas la permission nécessaire pour ce métier."),false);return;}
        if(job.max>0 && onlineCount(job.id)>=job.max && !job.id.equalsIgnoreCase(getJob(p))){p.displayClientMessage(Component.literal("§cLe nombre maximal de joueurs pour ce métier est atteint."),true);return;}
        if(old!=null && old.id.equalsIgnoreCase(job.id)){p.displayClientMessage(Component.literal("§eVous exercez déjà ce métier."),true);return;}
        leaveEffects(p,old);
        p.getPersistentData().putString(TAG,job.id);
        run(p,JobConfig.assignCommand(),job.id);
        run(p,job.selectCommand,job.id);
        if(job.clearInventoryOnSelect)p.getInventory().clearContent();
        p.displayClientMessage(Component.literal("§aVous êtes désormais §f"+job.name+"§a."),false);
    }
    public static void leave(ServerPlayer p){ JobConfig.Job old=current(p); leaveEffects(p,old); p.getPersistentData().putString(TAG,JobConfig.defaultJob()); p.displayClientMessage(Component.literal("§eVous êtes désormais §f"+Optional.ofNullable(JobConfig.byId(JobConfig.defaultJob())).map(j->j.name).orElse("Civil")+"§e."),false); }
    private static void leaveEffects(ServerPlayer p,JobConfig.Job old){if(old!=null){run(p,old.leaveCommand,old.id);if(old.clearInventoryOnLeave)p.getInventory().clearContent();}}
    private static void run(ServerPlayer p,String command,String job){if(command==null||command.isBlank())return;String c=command.replace("{player}",p.getGameProfile().getName()).replace("{uuid}",p.getUUID().toString()).replace("{job}",job);p.getServer().getCommands().performPrefixedCommand(p.getServer().createCommandSourceStack().withSuppressedOutput(),c);}

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END)return;CURRENT_SERVER=e.getServer(); if(!JobConfig.salaryEnabled())return;long now=e.getServer().getTickCount();long interval=JobConfig.intervalMinutes()*60L*20L;if(now-lastSalaryTick<interval)return;lastSalaryTick=now;for(ServerPlayer p:e.getServer().getPlayerList().getPlayers()){JobConfig.Job j=current(p);if(j!=null&&j.salary>0){String c=JobConfig.salaryCommand().replace("{player}",p.getGameProfile().getName()).replace("{uuid}",p.getUUID().toString()).replace("{salary}",String.valueOf(j.salary)).replace("{job}",j.id);e.getServer().getCommands().performPrefixedCommand(e.getServer().createCommandSourceStack().withSuppressedOutput(),c);p.displayClientMessage(Component.literal("§aSalaire reçu : §f"+j.salary+"€ §7("+j.name+")"),false);}}}

    @SubscribeEvent public static void containerClosed(net.minecraftforge.event.entity.player.PlayerContainerEvent.Close e){ if(e.getEntity() instanceof ServerPlayer p) VIEWERS.remove(p.getUUID()); }
    @SubscribeEvent public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)VIEWERS.remove(p.getUUID());}

    private static final class JobMenu extends ChestMenu {
        JobMenu(int id, Inventory inv){super(MenuType.GENERIC_9x6,id,inv,new SimpleContainer(54),6);}
        @Override public void clicked(int slot,int button,ClickType type,Player player){
            if(player instanceof ServerPlayer p && slot>=0 && slot<54 && VIEWERS.contains(p.getUUID())){
                if(slot==53){p.closeContainer();return;}
                if(slot<JobConfig.jobs().size()){JobConfig.Job j=JobConfig.jobs().get(slot);p.closeContainer();select(p,j);return;}
                return;
            }
            super.clicked(slot,button,type,player);
        }
    }
    public static boolean isJobMenu(ServerPlayer p){return VIEWERS.contains(p.getUUID())&&p.containerMenu instanceof JobMenu;}
}
