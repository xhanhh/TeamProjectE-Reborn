package top.ilov.mcmods.teamprojecte.common;

import top.ilov.mcmods.teamprojecte.TPRTeam;
import top.ilov.mcmods.teamprojecte.TeamProjectERebornMod;
import top.ilov.mcmods.teamprojecte.event.TeamChangeEvent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.UsernameCache;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TPRCommand {

    public static final Multimap<UUID, UUID> INVITATIONS = HashMultimap.create();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("teamper")
                .then(Commands.literal("invite")
                        .requires(TPRCommand::requiresPlayer)
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(TPRCommand::invite)))
                .then(Commands.literal("leave")
                        .requires(TPRCommand::requiresInTeam)
                        .executes(TPRCommand::leave))
                .then(Commands.literal("transfer_ownership")
                        .requires(TPRCommand::requiresOwner)
                        .then(Commands.argument("member", EntityArgument.player())
                                .executes(TPRCommand::transferOwnership)))
                .then(Commands.literal("members")
                        .requires(TPRCommand::requiresInTeam)
                        .executes(TPRCommand::members))
                .then(Commands.literal("kick")
                        .requires(TPRCommand::requiresOwner)
                        .then(Commands.argument("members", EntityArgument.players())
                                .executes(TPRCommand::kick)))
                .then(Commands.literal("accept")
                        .requires(TPRCommand::requiresPlayer)
                        .then(Commands.argument("team", UuidArgument.uuid())
                                .suggests(TPRCommand::createSuggestionsForInvitation)
                                .executes(TPRCommand::accept)))
                .then(Commands.literal("decline")
                        .requires(TPRCommand::requiresPlayer)
                        .then(Commands.argument("team", UuidArgument.uuid())
                                .suggests(TPRCommand::createSuggestionsForInvitation)
                                .executes(TPRCommand::decline)))
                .then(Commands.literal("settings")
                        .then(Commands.literal("share_emc")
                                .executes(TPRCommand::queryShareEMC)
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .requires(TPRCommand::requiresOwner)
                                        .executes(TPRCommand::setShareEMC)))
                        .then(Commands.literal("share_knowledge")
                                .executes(TPRCommand::queryShareKnowledge)
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .requires(TPRCommand::requiresOwner)
                                        .executes(TPRCommand::setShareKnowledge))))
        );
    }


    private static boolean requiresPlayer(CommandSourceStack stack) {
        return stack.getEntity() instanceof ServerPlayer;
    }


    private static boolean requiresInTeam(CommandSourceStack stack) {
        if (stack.getEntity() instanceof ServerPlayer player) {
            TPRTeam team = TPRTeam.getTeamByMember(TeamProjectERebornMod.getPlayerUUID(player));
            return team != null && (!team.getOwner().equals(TeamProjectERebornMod.getPlayerUUID(player)) || !team.getMembers().isEmpty());
        }
        return false;
    }

    private static boolean requiresOwner(CommandSourceStack stack) {
        if (!requiresInTeam(stack))
            return false;
        if (stack.getEntity() instanceof ServerPlayer player) {
            TPRTeam team = TPRTeam.getTeamByMember(TeamProjectERebornMod.getPlayerUUID(player));
            return team != null && TeamProjectERebornMod.getPlayerUUID(player).equals(team.getOwner());
        }
        return false;
    }


    private static int transferOwnership(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = checkPlayer(context);
        TPRTeam team = checkInTeam(player);
        if (team == null || !checkOwner(team, player))
            return 0;

        ServerPlayer newOwner = EntityArgument.getPlayer(context, "member");
        UUID newOwnerUUID = TeamProjectERebornMod.getPlayerUUID(newOwner);

        if (!team.getAll().contains(newOwnerUUID)) {
            context.getSource().sendFailure(Component.translatable("commands.teamprojecte_reborn.transfer_ownership.not_in_team"));
            return 0;
        }
          if (team.getOwner().equals(newOwnerUUID)) {
              context.getSource().sendFailure(Component.translatable("commands.teamprojecte_reborn.transfer_ownership.already_owner"));
              return 0;
          }

          // 如果没入队快照就保留进度
          TeamProjectERebornMod.bankTeamStateIfNoSnapshot(player, team);

          team.transferOwner(newOwnerUUID);
          newOwner.sendSystemMessage(Component.translatable("commands.teamprojecte_reborn.transfer_ownership.new_owner").withStyle(ChatFormatting.GREEN));
          context.getSource().sendSuccess(() -> Component.translatable("commands.teamprojecte_reborn.transfer_ownership.success", newOwner.getName()), true);
          postTeamAttributeChangeEvent(team);

          // 刷新服务端命令防止转让队伍命令提示没变
          TeamProjectERebornMod.refreshCommands(player);
          TeamProjectERebornMod.refreshCommands(newOwner);
          TeamProjectERebornMod.getAllOnline(team.getAll()).forEach(TeamProjectERebornMod::refreshCommands);

          return Command.SINGLE_SUCCESS;
      }

    private static int kick(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = checkPlayer(context);
        TPRTeam team = checkInTeam(player);
        if (team == null || !checkOwner(team, player))
            return 0;

        List<ServerPlayer> kick = EntityArgument.getPlayers(context, "members").stream()
                .filter(p -> team.getMembers().contains(TeamProjectERebornMod.getPlayerUUID(p)))
                .toList();
          kick.forEach(p -> {
              team.removeMember(TeamProjectERebornMod.getPlayerUUID(p));
              TeamProjectERebornMod.restoreBankedPersonalDataIfPresent(p);
              TeamProjectERebornMod.refreshCommands(p);
              p.sendSystemMessage(Component.translatable("commands.teamprojecte_reborn.kicked").withStyle(ChatFormatting.RED));
              UUID uuid = TeamProjectERebornMod.getPlayerUUID(p);
              postTeamMemberChangeEvent(uuid, team, null);
          });
          TeamProjectERebornMod.getAllOnline(team.getAll()).forEach(TeamProjectERebornMod::refreshCommands);

        if (!kick.isEmpty())
            context.getSource().sendSuccess(() -> Component.translatable("commands.teamprojecte_reborn.kick.success", kick.size()), true);
        else
            context.getSource().sendFailure(Component.translatable("commands.teamprojecte_reborn.players_not_found"));

        return kick.size();
    }

    private static int members(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = checkPlayer(context);
        TPRTeam team = checkInTeam(player);
        if (team == null)
            return 0;

        player.sendSystemMessage(Component.translatable("commands.teamprojecte_reborn.members", getNames(team.getOwner(), team.getAll())));
        return Command.SINGLE_SUCCESS;
    }

    private static Component getNames(UUID owner, List<UUID> uuids) {
        List<Component> components = new ArrayList<>();
        PlayerList playerList = null;
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();
        }

        for (UUID uuid : uuids) {
            MutableComponent component;
            ServerPlayer player = null;
            if (playerList != null) {
                player = playerList.getPlayer(uuid);
            }
            if (player != null)
                component = player.getName().copy()
                        .withStyle(ChatFormatting.GREEN)
                        .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("commands.teamprojecte_reborn.members.member_online"))));
            else if (UsernameCache.containsUUID(uuid))
                component = Component.literal(Objects.requireNonNull(UsernameCache.getLastKnownUsername(uuid)))
                        .withStyle(ChatFormatting.RED)
                        .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("commands.teamprojecte_reborn.members.member_offline"))));
            else
                component = Component.literal(uuid.toString())
                        .withStyle(ChatFormatting.GRAY)
                        .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("commands.teamprojecte_reborn.members.member_unknown"))));
            if (uuid.equals(owner))
                component.withStyle(ChatFormatting.BOLD);
            components.add(component);
        }
        return ComponentUtils.formatList(components, c -> c);
    }

      private static int leave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
          ServerPlayer player = checkPlayer(context);
          TPRTeam team = checkInTeam(player);
          if (team == null)
              return 0;

          UUID uuid = TeamProjectERebornMod.getPlayerUUID(player);
          team.removeMember(uuid);
          boolean restored = TeamProjectERebornMod.restoreBankedPersonalDataIfPresent(player);
          player.sendSystemMessage(Component.translatable(
                  restored
                          ? "commands.teamprojecte_reborn.leave.restored"
                          : "commands.teamprojecte_reborn.leave.no_snapshot"
          ).withStyle(ChatFormatting.GRAY));

          TeamProjectERebornMod.refreshCommands(player);
          TeamProjectERebornMod.getAllOnline(team.getAll()).forEach(TeamProjectERebornMod::refreshCommands);
          postTeamMemberChangeEvent(uuid, team, null);
          return Command.SINGLE_SUCCESS;
      }

      private static int accept(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
          ServerPlayer player = checkPlayer(context);
          UUID uuid = UuidArgument.getUuid(context, "team");
        if (!INVITATIONS.get(TeamProjectERebornMod.getPlayerUUID(player)).contains(uuid)) {
            context.getSource().sendFailure(Component.translatable("commands.teamprojecte_reborn.invitation.not_found"));
            return -1;
        }

        INVITATIONS.remove(TeamProjectERebornMod.getPlayerUUID(player), uuid);

        TPRTeam team = TPRTeam.getTeam(uuid);
        if (team == null) {
            context.getSource().sendFailure(Component.translatable("commands.teamprojecte_reborn.team_not_found"));
            return -1;
        }
        TPRTeam originalTeam = TPRTeam.getTeamByMember(TeamProjectERebornMod.getPlayerUUID(player));
          if (originalTeam != null)
              team.addMemberWithKnowledge(originalTeam, player);
          else
              team.addMember(TeamProjectERebornMod.getPlayerUUID(player));

        context.getSource().sendSuccess(() -> Component.translatable("commands.teamprojecte_reborn.invite.accepted").withStyle(ChatFormatting.GREEN), false);
        Component component = Component.translatable("commands.teamprojecte_reborn.joined_team", player.getDisplayName()).withStyle(ChatFormatting.GREEN);
          TeamProjectERebornMod.getAllOnline(team.getAll()).forEach(p -> p.sendSystemMessage(component));
          postTeamMemberChangeEvent(uuid, originalTeam, team);

          TeamProjectERebornMod.refreshCommands(player);
          TeamProjectERebornMod.getAllOnline(team.getAll()).forEach(TeamProjectERebornMod::refreshCommands);

          return Command.SINGLE_SUCCESS;
      }


    private static int decline(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = checkPlayer(context);
        UUID uuid = UuidArgument.getUuid(context, "team");
        if (!INVITATIONS.get(TeamProjectERebornMod.getPlayerUUID(player)).contains(uuid)) {
            context.getSource().sendFailure(Component.translatable("commands.teamprojecte_reborn.invitation.not_found"));
            return -1;
        }

        INVITATIONS.remove(TeamProjectERebornMod.getPlayerUUID(player), uuid);

        TPRTeam team = TPRTeam.getTeam(uuid);
        if (team == null) {
            context.getSource().sendFailure(Component.translatable("commands.teamprojecte_reborn.team_not_found"));
            return -1;
        }

        player.sendSystemMessage(Component.translatable("commands.teamprojecte_reborn.invite.declined").withStyle(ChatFormatting.RED));
        TeamProjectERebornMod.getAllOnline(Collections.singletonList(team.getOwner())).forEach(p ->
                p.sendSystemMessage(Component.translatable("commands.teamprojecte_reborn.invitation.declined", player.getDisplayName())));

        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> createSuggestionsForInvitation(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        Player player = checkPlayer(context);
        return SharedSuggestionProvider.suggest(INVITATIONS.get(TeamProjectERebornMod.getPlayerUUID(player)).stream().map(UUID::toString), builder);
    }

      private static int invite(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
          Player player = checkPlayer(context);
          TPRTeam team = TPRTeam.getOrCreateTeam(TeamProjectERebornMod.getPlayerUUID(player));


        Collection<ServerPlayer> players =
                EntityArgument.getPlayers(context, "players").stream()
                        .filter(p -> !team.getAll().contains(TeamProjectERebornMod.getPlayerUUID(p)))
                        .toList();

        Component component = Component.translatable("commands.teamprojecte_reborn.invitation",
                player.getDisplayName(),
                Component.translatable("commands.teamprojecte_reborn.invite.option.accept")
                        .withStyle(style -> style.applyFormat(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/teamper accept " + team.getUUID()))),
                Component.translatable("commands.teamprojecte_reborn.invite.option.decline")
                        .withStyle(style -> style.applyFormat(ChatFormatting.RED)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/teamper decline " + team.getUUID())))
        );

          for (ServerPlayer p : players) {
              INVITATIONS.put(TeamProjectERebornMod.getPlayerUUID(p), team.getUUID());
              p.sendSystemMessage(component);
              p.sendSystemMessage(Component.translatable("commands.teamprojecte_reborn.invitation.emc_notice").withStyle(ChatFormatting.GRAY));
          }
        if (!players.isEmpty())
            context.getSource().sendSuccess(() -> Component.translatable("commands.teamprojecte_reborn.invite.success", players.size()), true);
        else
            context.getSource().sendFailure(Component.translatable("commands.teamprojecte_reborn.players_not_found"));
        return players.size();
    }

    private static ServerPlayer checkPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (context.getSource().getEntity() instanceof ServerPlayer player)
            return player;
        throw CommandSourceStack.ERROR_NOT_PLAYER.create();
    }

    private static TPRTeam checkInTeam(Player player) {
        TPRTeam team = TPRTeam.getTeamByMember(TeamProjectERebornMod.getPlayerUUID(player));
        if (team == null || (team.getOwner().equals(TeamProjectERebornMod.getPlayerUUID(player)) && team.getMembers().isEmpty())) {
            player.sendSystemMessage(Component.translatable("commands.teamprojecte_reborn.leave.not_in_team").withStyle(ChatFormatting.RED));
            return null;
        }
        return team;
    }

    private static boolean checkOwner(TPRTeam team, ServerPlayer player) {
        if (!TeamProjectERebornMod.getPlayerUUID(player).equals(team.getOwner())) {
            player.sendSystemMessage(Component.translatable("commands.teamprojecte_reborn.not_owner").withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    private static int queryShareEMC(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = checkPlayer(context);
        TPRTeam team = checkInTeam(player);
        if (team == null)
            return 0;

        context.getSource().sendSuccess(() -> Component.translatable("commands.teamprojecte_reborn.settings.query.sharing_emc." + team.isSharingEMC()), true);

        return Command.SINGLE_SUCCESS;
    }

    private static int setShareEMC(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = checkPlayer(context);
        TPRTeam team = checkInTeam(player);
        if (team == null || !checkOwner(team, player))
            return 0;

        team.setShareEMC(BoolArgumentType.getBool(context, "value"));
        context.getSource().sendSuccess(() -> Component.translatable("commands.teamprojecte_reborn.settings.set.sharing_emc." + team.isSharingEMC()), true);
        postTeamAttributeChangeEvent(team);

        return Command.SINGLE_SUCCESS;
    }

    private static int queryShareKnowledge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = checkPlayer(context);
        TPRTeam team = checkInTeam(player);
        if (team == null)
            return 0;

        context.getSource().sendSuccess(() -> Component.translatable("commands.teamprojecte_reborn.settings.query.sharing_knowledge." + team.isSharingKnowledge()), true);

        return Command.SINGLE_SUCCESS;
    }

    private static int setShareKnowledge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = checkPlayer(context);
        TPRTeam team = checkInTeam(player);
        if (team == null || !checkOwner(team, player))
            return 0;

        team.setShareKnowledge(BoolArgumentType.getBool(context, "value"));
        context.getSource().sendSuccess(() -> Component.translatable("commands.teamprojecte_reborn.settings.set.sharing_knowledge." + team.isSharingKnowledge()), true);
        postTeamAttributeChangeEvent(team);

        return Command.SINGLE_SUCCESS;
    }

    private static void postTeamMemberChangeEvent(UUID playerUUID, TPRTeam oldTeam, TPRTeam newTeam) {
        NeoForge.EVENT_BUS.post(new TeamChangeEvent(playerUUID, oldTeam, newTeam));
    }

    private static void postTeamAttributeChangeEvent(TPRTeam team) {
        postTeamMemberChangeEvent(null, team, null);
    }
}
