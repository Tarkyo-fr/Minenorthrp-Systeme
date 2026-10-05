package fr.minenorth.drill.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.minenorth.drill.bank.MainBankData;
import fr.minenorth.drill.block.entity.BankVaultBlockEntity;
import fr.minenorth.drill.config.ModConfig;
import fr.minenorth.drill.config.SystemeConfigFiles;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.commands.arguments.EntityArgument;
import fr.minenorth.drill.document.IdentityManager;
import fr.minenorth.drill.job.JobConfig;
import fr.minenorth.drill.job.JobManager;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class ModCommands {
    private enum SelectionMode { ADD_BLOCK, HACK_BLOCK, ADD_GRILLE, GRILLE_HACK_BLOCK }
    private static final Map<UUID, SelectionMode> SELECTIONS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("cidmenu")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("joueur", EntityArgument.player())
                        .executes(c -> openIdentity(c.getSource(), EntityArgument.getPlayer(c, "joueur")))));
        dispatcher.register(Commands.literal("cidreset").requires(s -> s.hasPermission(2)).then(Commands.argument("joueur", EntityArgument.player()).executes(c -> resetIdentity(c.getSource(), EntityArgument.getPlayer(c, "joueur")))));
        dispatcher.register(Commands.literal("pemploi")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("joueur", EntityArgument.player())
                        .executes(c -> openFranceTravail(EntityArgument.getPlayer(c, "joueur")))));
        dispatcher.register(Commands.literal("emploireload").requires(s -> s.hasPermission(2)).executes(c -> reloadJobs(c.getSource())));
        dispatcher.register(Commands.literal("monmetier").executes(c -> myJob(c.getSource())));
        dispatcher.register(Commands.literal("pemploiadmin").requires(s -> s.hasPermission(2))
                .then(Commands.literal("see").then(Commands.argument("joueur", EntityArgument.player()).executes(c -> seeJob(c.getSource(), EntityArgument.getPlayer(c, "joueur")))))
                .then(Commands.literal("reset").then(Commands.argument("joueur", EntityArgument.player()).executes(c -> resetJob(c.getSource(), EntityArgument.getPlayer(c, "joueur"))))));

        dispatcher.register(Commands.literal("banquereload").requires(s -> s.hasPermission(2)).executes(c -> reload(c.getSource())));
        dispatcher.register(Commands.literal("portereload").requires(s -> s.hasPermission(2)).executes(c -> doorReload(c.getSource())));
        dispatcher.register(Commands.literal("banquereset").requires(s -> s.hasPermission(2)).executes(c -> reset(c.getSource())));

        dispatcher.register(Commands.literal("banque")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("bloc")
                        .then(Commands.literal("add").executes(c -> startSelection(c.getSource(), SelectionMode.ADD_BLOCK)))
                        .then(Commands.literal("clear").executes(c -> clearBlocks(c.getSource())))
                        .then(Commands.literal("list").executes(c -> listBlocks(c.getSource()))))
                .then(Commands.literal("hackblock")
                        .then(Commands.literal("clear").executes(c -> clearHackBlock(c.getSource())))
                        .executes(c -> startSelection(c.getSource(), SelectionMode.HACK_BLOCK)))
                .then(Commands.literal("grille")
                        .then(Commands.literal("add").executes(c -> startSelection(c.getSource(), SelectionMode.ADD_GRILLE)))
                        .then(Commands.literal("clear").executes(c -> clearGrilleBlocks(c.getSource())))
                        .then(Commands.literal("list").executes(c -> listGrilleBlocks(c.getSource())))
                        .then(Commands.literal("hackblock")
                                .then(Commands.literal("clear").executes(c -> clearGrilleHackBlock(c.getSource())))
                                .executes(c -> startSelection(c.getSource(), SelectionMode.GRILLE_HACK_BLOCK))))
                .then(Commands.literal("status").executes(c -> status(c.getSource()))));
    }

    private static int openFranceTravail(ServerPlayer target){ JobManager.openMenu(target); return 1; }
    private static int reloadJobs(CommandSourceStack source){ JobConfig.load(); source.sendSuccess(() -> Component.literal("§aConfiguration des métiers rechargée : "+JobConfig.jobs().size()+" métier(s). Salaire "+(JobConfig.salaryEnabled()?"activé":"désactivé")+"."), true); return 1; }
    private static int myJob(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException { ServerPlayer p=source.getPlayerOrException(); JobConfig.Job j=JobManager.current(p); source.sendSuccess(() -> Component.literal("§eVotre métier : §f"+(j==null?JobConfig.defaultJob():j.name)), false); return 1; }
    private static int seeJob(CommandSourceStack source, ServerPlayer p){ JobConfig.Job j=JobManager.current(p); source.sendSuccess(() -> Component.literal("§e"+p.getGameProfile().getName()+" exerce : §f"+(j==null?JobConfig.defaultJob():j.name)), false); return 1; }
    private static int resetJob(CommandSourceStack source, ServerPlayer p){ JobManager.leave(p); source.sendSuccess(() -> Component.literal("§aMétier de "+p.getGameProfile().getName()+" réinitialisé."), true); return 1; }

    private static int openIdentity(CommandSourceStack source, ServerPlayer target){
        // /cidmenu <joueur> est une commande de gestion : l'interface est ouverte directement au joueur ciblé.
        IdentityManager.open(target, target.getUUID(), true, false);
        return 1;
    }
    private static int resetIdentity(CommandSourceStack source, ServerPlayer target){IdentityManager.reset(source.getPlayer()!=null?source.getPlayer():target,target.getUUID());return 1;}
    private static int reload(CommandSourceStack source) {
        boolean success = ModConfig.reload();
        source.sendSuccess(() -> Component.translatable(success ? "command.minenorthdrill.config_reloaded" : "command.minenorthdrill.config_reload_failed"), true);
        return success ? 1 : 0;
    }

    private static int doorReload(CommandSourceStack source) {
        int count = SystemeConfigFiles.reloadDoors(source.getServer());
        if (count < 0) {
            source.sendFailure(Component.literal("Impossible de recharger config/Minenorth-systeme/porte.toml."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("§aConfiguration des portes rechargée : " + count + " porte(s)."), true);
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        int count = BankVaultBlockEntity.resetLoadedVaults();
        ServerLevel level = source.getLevel();
        MainBankData data = MainBankData.get(level);
        int restored = data.restoreBank(level);
        int grilleRestored = data.restoreGrille(level);
        source.sendSuccess(() -> Component.translatable("command.minenorthdrill.vaults_reset", count, restored + grilleRestored), true);
        return 1;
    }

    private static int startSelection(CommandSourceStack source, SelectionMode mode) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SELECTIONS.put(player.getUUID(), mode);
        String key = switch (mode) {
            case ADD_BLOCK -> "command.minenorthdrill.bank_select_blocks";
            case HACK_BLOCK -> "command.minenorthdrill.bank_select_hackblock";
            case ADD_GRILLE -> "command.minenorthdrill.bank_select_grille";
            case GRILLE_HACK_BLOCK -> "command.minenorthdrill.bank_select_grille_hackblock";
        };
        player.displayClientMessage(Component.translatable(key), false);
        return 1;
    }

    private static int clearBlocks(CommandSourceStack source) {
        MainBankData.get(source.getLevel()).clearBlocks();
        source.sendSuccess(() -> Component.translatable("command.minenorthdrill.bank_blocks_cleared"), true);
        return 1;
    }

    private static int clearGrilleBlocks(CommandSourceStack source) {
        MainBankData.get(source.getLevel()).clearGrilleBlocks();
        source.sendSuccess(() -> Component.translatable("command.minenorthdrill.grille_blocks_cleared"), true);
        return 1;
    }

    private static int clearHackBlock(CommandSourceStack source) {
        MainBankData.get(source.getLevel()).clearHackBlock();
        source.sendSuccess(() -> Component.translatable("command.minenorthdrill.bank_hackblock_cleared"), true);
        return 1;
    }

    private static int clearGrilleHackBlock(CommandSourceStack source) {
        MainBankData.get(source.getLevel()).clearGrilleHackBlock();
        source.sendSuccess(() -> Component.translatable("command.minenorthdrill.grille_hackblock_cleared"), true);
        return 1;
    }

    private static int listBlocks(CommandSourceStack source) {
        MainBankData data = MainBankData.get(source.getLevel());
        source.sendSuccess(() -> Component.literal("Blocs du coffre principal : " + data.getBlocks().size()), false);
        if (data.getHackBlock() != null) source.sendSuccess(() -> Component.literal("Bloc de piratage coffre : " + data.getHackBlock().toShortString()), false);
        return data.getBlocks().size();
    }

    private static int listGrilleBlocks(CommandSourceStack source) {
        MainBankData data = MainBankData.get(source.getLevel());
        source.sendSuccess(() -> Component.literal("Blocs des grilles : " + data.getGrilleBlocks().size()), false);
        if (data.getGrilleHackBlock() != null) source.sendSuccess(() -> Component.literal("Bloc de piratage grilles : " + data.getGrilleHackBlock().toShortString()), false);
        return data.getGrilleBlocks().size();
    }

    private static int status(CommandSourceStack source) {
        MainBankData data = MainBankData.get(source.getLevel());
        source.sendSuccess(() -> Component.literal("Coffre principal : " + data.getBlocks().size() + " bloc(s), déclencheur=" + (data.getHackBlock() != null ? data.getHackBlock().toShortString() : "non défini") + ", état=" + (data.isHacked() ? "piraté" : "fermé")), false);
        source.sendSuccess(() -> Component.literal("Grilles : " + data.getGrilleBlocks().size() + " bloc(s), déclencheur=" + (data.getGrilleHackBlock() != null ? data.getGrilleHackBlock().toShortString() : "non défini") + ", état=" + (data.isGrilleHacked() ? "piratées" : "fermées")), false);
        return 1;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;
        SelectionMode mode = SELECTIONS.get(player.getUUID());
        if (mode == null || !(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;

        MainBankData data = MainBankData.get(level);
        if (mode == SelectionMode.ADD_BLOCK) {
            boolean added = data.addBlock(level, event.getPos());
            player.displayClientMessage(Component.translatable(added ? "command.minenorthdrill.bank_block_added" : "command.minenorthdrill.bank_block_already"), false);
        } else if (mode == SelectionMode.ADD_GRILLE) {
            boolean added = data.addGrilleBlock(level, event.getPos());
            player.displayClientMessage(Component.translatable(added ? "command.minenorthdrill.grille_block_added" : "command.minenorthdrill.grille_block_already"), false);
        } else if (mode == SelectionMode.HACK_BLOCK) {
            if (data.contains(event.getPos(), level) || data.containsGrille(event.getPos(), level) || (data.getGrilleHackBlock() != null && data.getGrilleHackDimension().equals(level.dimension()) && data.getGrilleHackBlock().equals(event.getPos()))) {
                player.displayClientMessage(Component.translatable("command.minenorthdrill.bank_hackblock_must_be_outside_bank"), false);
            } else {
                data.setHackBlock(level, event.getPos());
                player.displayClientMessage(Component.translatable("command.minenorthdrill.bank_hackblock_set", event.getPos().toShortString()), false);
                SELECTIONS.remove(player.getUUID());
            }
        } else {
            if (data.contains(event.getPos(), level) || data.containsGrille(event.getPos(), level) || (data.getHackBlock() != null && data.getHackDimension().equals(level.dimension()) && data.getHackBlock().equals(event.getPos()))) {
                player.displayClientMessage(Component.translatable("command.minenorthdrill.grille_hackblock_must_be_outside"), false);
            } else {
                data.setGrilleHackBlock(level, event.getPos());
                player.displayClientMessage(Component.translatable("command.minenorthdrill.grille_hackblock_set", event.getPos().toShortString()), false);
                SELECTIONS.remove(player.getUUID());
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
