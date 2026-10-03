package fr.minenorth.drill.document;

import fr.minenorth.drill.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber
public final class IdentityManager {
    private IdentityManager() {}

    public static final String CARD_TAG = "MineNorthIdentityOwner";
    public static final String CARD_NUMBER_TAG = "MineNorthIdentityNumber";

    /**
     * Utilise le prénom + nom RP dans le chat sans modifier le nom au-dessus de la tête.
     */
    @SubscribeEvent
    public static void nameFormat(PlayerEvent.NameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        IdentityData.Profile profile = IdentityData.get(player.serverLevel()).get(player.getUUID());
        if (profile != null) {
            event.setDisplayname(Component.literal(profile.firstName() + " " + profile.lastName()));
        }
    }

    /**
     * Utilise le prénom + nom RP dans le TAB.
     */
    @SubscribeEvent
    public static void tabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        IdentityData.Profile profile = IdentityData.get(player.serverLevel()).get(player.getUUID());
        if (profile != null) {
            event.setDisplayName(Component.literal(profile.firstName() + " " + profile.lastName()));
        }
    }

    /**
     * Force le format du chat côté serveur pour les serveurs/mods qui ignorent
     * le PlayerEvent.NameFormat. Le pseudo Minecraft n'est jamais modifié.
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void rpChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        IdentityData.Profile profile = IdentityData.get(player.serverLevel()).get(player.getUUID());
        if (profile == null) return;

        net.minecraft.network.chat.MutableComponent formatted = Component.literal(profile.firstName() + " " + profile.lastName());
        formatted.append(Component.literal(" » "));
        formatted.append(event.getMessage());

        event.setCanceled(true);
        player.server.getPlayerList().broadcastSystemMessage(formatted, false);
    }

    @SubscribeEvent
    public static void join(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Recalcule explicitement le nom TAB à la connexion.
        player.refreshTabListName();
    }

    @SubscribeEvent
    public static void rightClickPlayer(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer viewer)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;

        // Une interaction normale sur un joueur ne fait rien.
        // Pour présenter sa propre carte, le joueur doit faire sneak + clic droit
        // sur la personne à qui il souhaite la montrer, avec sa carte en main.
        if (!viewer.isShiftKeyDown()) return;

        ItemStack held = viewer.getItemInHand(event.getHand());
        if (!(held.getItem() instanceof DocumentItem document) || !"identity".equals(document.type())) return;

        IdentityData.Profile profile = IdentityData.get(viewer.serverLevel()).get(viewer.getUUID());
        if (profile == null) {
            viewer.displayClientMessage(Component.literal("§cVous ne possédez pas encore de carte d'identité."), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        showTo(viewer, target, profile);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!(stack.getItem() instanceof DocumentItem document) || !"identity".equals(document.type())) return;

        UUID owner = stack.getTag() != null && stack.getTag().hasUUID(CARD_TAG)
                ? stack.getTag().getUUID(CARD_TAG) : player.getUUID();
        IdentityData.Profile profile = IdentityData.get(player.serverLevel()).get(owner);
        String cardNumber = stack.getTag() != null ? stack.getTag().getString(CARD_NUMBER_TAG) : "";
        if (profile == null || !profile.cardNumber().equals(cardNumber)) {
            player.displayClientMessage(Component.literal("§cCette carte d'identité n'est plus valide."), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        // Clic droit sur la carte : lecture de la carte, quel que soit son propriétaire.
        ModNetwork.sendIdentityView(player, profile, owner, "", false, false);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    public static void showTo(ServerPlayer from, ServerPlayer to, IdentityData.Profile profile) {
        ModNetwork.sendIdentityView(to, profile, from.getUUID(), from.getGameProfile().getName(), false, false);
        from.displayClientMessage(Component.literal("§aVotre carte d'identité a été présentée."), true);
    }

    /** Opens the /cidmenu management screen. Only a command source with permission can call this. */
    public static void open(ServerPlayer viewer, UUID target, boolean staff, boolean forceCreation) {
        IdentityData.Profile profile = IdentityData.get(viewer.serverLevel()).get(target);
        if (profile == null) ModNetwork.sendIdentityCreation(viewer, target);
        else ModNetwork.sendIdentityAlreadyExists(viewer, profile, target);
    }

    /** Creates an identity exactly once. Players cannot overwrite their identity. */
    public static void save(ServerPlayer actor, String lastName, String firstName, String birthDate,
                            String birthPlace, String nationality) {
        IdentityData data = IdentityData.get(actor.serverLevel());
        if (data.exists(actor.getUUID())) {
            actor.displayClientMessage(Component.literal("§cVotre carte d'identité existe déjà. Elle ne peut pas être modifiée."), true);
            open(actor, actor.getUUID(), false, false);
            return;
        }

        lastName = clean(lastName);
        firstName = clean(firstName);
        birthDate = clean(birthDate);
        birthPlace = clean(birthPlace);
        nationality = clean(nationality);

        if (lastName.isBlank() || firstName.isBlank() || birthDate.isBlank() || birthPlace.isBlank()) {
            actor.displayClientMessage(Component.literal("§cTous les champs doivent être remplis."), true);
            return;
        }
        if (nationality.isBlank()) nationality = "Française";

        String cardNumber = generateCardNumber(data);
        IdentityData.Profile profile = new IdentityData.Profile(
                actor.getUUID(), lastName, firstName, birthDate, birthPlace, nationality, cardNumber
        );
        data.set(profile);
        refreshTabName(actor);
        giveCard(actor, profile);

        ModNetwork.sendIdentityView(actor, profile, actor.getUUID(), "", false, false);
        actor.displayClientMessage(Component.literal("§aVotre carte d'identité a été créée définitivement."), true);
    }

    /** Force le serveur et les clients à recalculer le nom affiché dans le TAB. */
    private static void refreshTabName(ServerPlayer player) {
        player.refreshTabListName();
    }

    public static void giveCard(ServerPlayer player, IdentityData.Profile profile) {
        ItemStack card = new ItemStack(fr.minenorth.drill.item.ModItems.IDENTITY_CARD.get());
        card.getOrCreateTag().putUUID(CARD_TAG, profile.uuid());
        card.getOrCreateTag().putString(CARD_NUMBER_TAG, profile.cardNumber());
        if (!player.getInventory().add(card)) player.drop(card, false);
    }

    public static void reissueLostCard(ServerPlayer player) {
        IdentityData.Profile profile = IdentityData.get(player.serverLevel()).get(player.getUUID());
        if (profile == null) {
            player.displayClientMessage(Component.literal("§cVous n'avez pas encore de carte d'identité."), true);
            return;
        }
        giveCard(player, profile);
        player.displayClientMessage(Component.literal("§aUne nouvelle carte d'identité vous a été remise."), true);
    }

    private static String generateCardNumber(IdentityData data) {
        String number;
        do {
            long value = ThreadLocalRandom.current().nextLong(10_000_000L, 100_000_000L);
            number = "FR-" + value;
        } while (data.profilesContainsCard(number));
        return number;
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }

    public static void reset(ServerPlayer actor, UUID target) {
        IdentityData data = IdentityData.get(actor.serverLevel());
        data.reset(target);
        ServerPlayer targetPlayer = actor.server.getPlayerList().getPlayer(target);
        if (targetPlayer != null) {
            targetPlayer.displayClientMessage(Component.literal("§eVotre carte d'identité a été réinitialisée par le staff. Vous devez en créer une nouvelle."), false);
        }
        actor.displayClientMessage(Component.literal("§aCarte d'identité de " + (targetPlayer != null ? targetPlayer.getGameProfile().getName() : target) + " réinitialisée."), true);
    }
}
