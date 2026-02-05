package cn.leomc.teamprojecte;

import com.mojang.logging.LogUtils;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.impl.capability.KnowledgeImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import cn.leomc.teamprojecte.mixin.KnowledgeAttachmentAccessor;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Mod("teamprojecte")
public class TeamProjectE {

    public static final Logger LOGGER = LogUtils.getLogger();

    public TeamProjectE() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommand(RegisterCommandsEvent event) {
        TPCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        TPCommand.INVITATIONS.clear();
        TPSavedData.onServerStopped();
    }

    @SubscribeEvent
    public void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().dimension() == Level.OVERWORLD && event.getEntity() instanceof ServerPlayer player) {
            migrateProjectEDataIfNeeded(player);
            sync(player);
        }
    }

    public static List<ServerPlayer> getAllOnline(List<UUID> uuids) {
        return uuids.stream()
                .map(uuid -> {
                    if (ServerLifecycleHooks.getCurrentServer() != null) {
                        return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public static void sync(ServerPlayer player) {
        IKnowledgeProvider provider = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
        if (provider != null) {
            provider.sync(player);
        }
    }

    private static void migrateProjectEDataIfNeeded(ServerPlayer player) {
        TPSavedData data = TPSavedData.getData();
        if (data == null) {
            return;
        }

        UUID uuid = getPlayerUUID(player);
        if (data.hasMigrated(uuid)) {
            return;
        }

        TPTeam existingTeam = TPTeam.getTeamByMember(uuid);
        if (existingTeam != null && !isPristineSoloTeam(existingTeam, uuid)) {
            data.markMigrated(uuid);
            return;
        }

        KnowledgeImpl.KnowledgeAttachment attachment = player.getData(PEAttachmentTypes.KNOWLEDGE);
        KnowledgeAttachmentAccessor accessor = (KnowledgeAttachmentAccessor) attachment;

        BigInteger emc = accessor.teamprojecte$getEmc();
        if (emc == null) {
            emc = BigInteger.ZERO;
        }
        boolean fullKnowledge = accessor.teamprojecte$isFullKnowledge();

        if (!fullKnowledge && emc.signum() == 0 && accessor.teamprojecte$getKnowledge().isEmpty()) {
            data.markMigrated(uuid);
            return;
        }

        TPTeam team = existingTeam != null ? existingTeam : TPTeam.createTeam(uuid);
        team.setEmc(emc, uuid);
        team.setFullKnowledge(fullKnowledge, uuid);
        accessor.teamprojecte$getKnowledge().forEach(info -> team.addKnowledge(info, uuid));

        data.markMigrated(uuid);
    }

    private static boolean isPristineSoloTeam(TPTeam team, UUID ownerUUID) {
        if (!ownerUUID.equals(team.getOwner())) {
            return false;
        }
        if (!team.getMembers().isEmpty()) {
            return false;
        }
        if (team.hasFullKnowledge(ownerUUID)) {
            return false;
        }
        if (!team.getKnowledge(ownerUUID).isEmpty()) {
            return false;
        }
        return team.getEmc(ownerUUID).signum() == 0;
    }

    public static List<ServerPlayer> getOnlineTeamMembers(UUID uuid) {
        return getOnlineTeamMembers(uuid, true);
    }

    public static List<ServerPlayer> getOnlineTeamMembers(UUID uuid, boolean includeOwner) {
        TPTeam team = TPTeam.getOrCreateTeam(uuid);
        return TeamProjectE.getAllOnline(includeOwner ? team.getAll() : team.getMembers());
    }

    public static UUID getPlayerUUID(Player player) {
        UUID uuid = player.getGameProfile().getId();
        if (uuid == null)
            uuid = player.getUUID();
        return uuid;
    }

}
