package top.ilov.mcmods.teamprojecte;

import top.ilov.mcmods.teamprojecte.data.EMCData;
import top.ilov.mcmods.teamprojecte.data.KnowledgeData;
import com.google.common.collect.Lists;
import lombok.Getter;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TPRTeam {
    private final UUID teamUUID;
    @Getter
    private UUID owner;
    private final List<UUID> members;

    private KnowledgeData knowledge;
    private EMCData emc;

    public TPRTeam(UUID teamUUID, UUID owner) {
        this.teamUUID = teamUUID;
        this.owner = owner;
        this.members = new ArrayList<>();
        this.knowledge = new KnowledgeData.Sharing();
        this.emc = new EMCData.Sharing();
    }

    public TPRTeam(UUID owner) {
        this(UUID.randomUUID(), owner);
    }

    public TPRTeam(CompoundTag tag, String version) {
        this.teamUUID = tag.getUUID("uuid");
        this.owner = tag.getUUID("owner");
        this.members = new ArrayList<>();
        this.members.addAll(tag.getList("members", Tag.TAG_COMPOUND).stream().map(t -> ((CompoundTag) t).getUUID("uuid")).toList());
        switch (version) {
            case "" -> {
                this.knowledge = new KnowledgeData.Sharing();
                for (Tag t : tag.getList("knowledge", Tag.TAG_COMPOUND)) {
                    CompoundTag ct = (CompoundTag) t;
                    ItemInfo.CODEC.parse(NbtOps.INSTANCE, ct).result()
                            .ifPresent(info -> this.knowledge.addKnowledge(info, Util.NIL_UUID));
                }
                this.emc = new EMCData.Sharing();
                this.emc.setEMC(new BigInteger(tag.getString("emc")), Util.NIL_UUID);
            }
            case "1" -> {
                this.knowledge = KnowledgeData.of(tag.getCompound("knowledge"));
                this.emc = EMCData.of(tag.getCompound("emc"));
            }
        }
    }


    public UUID getUUID() {
        return teamUUID;
    }

    public void addMemberWithKnowledge(TPRTeam originalTeam, Player player) {
        markDirty();
        UUID playerUUID = TeamProjectERebornMod.getPlayerUUID(player);
        addMember(playerUUID);

        if (!originalTeam.isSharingEMC()) {
            BigInteger emcToTransfer = originalTeam.getEmc(playerUUID);
            if (emcToTransfer.signum() != 0) {
                if (isSharingEMC()) {
                    setEmc(getEmc(playerUUID).add(emcToTransfer), playerUUID);
                } else {
                    setEmc(emcToTransfer, playerUUID);
                }
            }
            originalTeam.setEmc(BigInteger.ZERO, playerUUID);
        }

        if (!originalTeam.isSharingKnowledge()) {
            if (originalTeam.hasFullKnowledge(playerUUID)) {
                setFullKnowledge(true, playerUUID);
                originalTeam.setFullKnowledge(false, playerUUID);
            }
            originalTeam.getKnowledge(playerUUID).forEach(k -> addKnowledge(k, playerUUID));
            originalTeam.clearKnowledge(playerUUID);
        }
        originalTeam.removeMember(playerUUID);
        sync();
    }

    public void addMember(UUID uuid) {
        markDirty();
        TPRSavedData.getData().invalidateCache(uuid);
        members.add(uuid);
        sync();
    }

    public void removeMember(UUID uuid) {
        markDirty();
        TPRSavedData.getData().invalidateCache(uuid);
        knowledge.removeMember(uuid);
        emc.removeMember(uuid);
        if (owner.equals(uuid)) {
            if (members.isEmpty()) {
                TPRSavedData.getData().teams.remove(teamUUID);
                return;
            }
            UUID newOwner = members.get(ThreadLocalRandom.current().nextInt(members.size()));
            owner = newOwner;
            members.remove(newOwner);
        } else
            members.remove(uuid);
        sync(uuid);
    }

    public void transferOwner(UUID newOwner) {
        if (owner.equals(newOwner) || !members.contains(newOwner))
            return;
        members.add(owner);
        owner = newOwner;
        members.remove(newOwner);
    }

    public List<UUID> getMembers() {
        return List.copyOf(members);
    }

    public List<UUID> getAll() {
        return Lists.asList(owner, members.toArray(UUID[]::new));
    }


    public boolean addKnowledge(ItemInfo info, UUID player) {
        markDirty();
        return knowledge.addKnowledge(info, player);
    }

    public boolean removeKnowledge(ItemInfo info, UUID player) {
        markDirty();
        return knowledge.removeKnowledge(info, player);
    }

    public void clearKnowledge(UUID player) {
        markDirty();
        knowledge.clearKnowledge(player);
    }


    public Set<ItemInfo> getKnowledge(UUID player) {
        return knowledge.getKnowledge(player);
    }

    public void setEmc(BigInteger emc, UUID player) {
        markDirty();
        this.emc.setEMC(emc, player);
    }

    public BigInteger getEmc(UUID player) {
        return emc.getEMC(player);
    }

    public void setFullKnowledge(boolean fullKnowledge, UUID player) {
        markDirty();
        knowledge.setFullKnowledge(fullKnowledge, player);
    }

    public boolean hasFullKnowledge(UUID player) {
        return knowledge.hasFullKnowledge(player);
    }

    public boolean isSharingEMC() {
        return emc instanceof EMCData.Sharing;
    }

    public boolean isSharingKnowledge() {
        return knowledge instanceof KnowledgeData.Sharing;
    }


    public void setShareEMC(boolean share) {
        if (isSharingEMC() == share)
            return;
        emc = emc.convert(getOwner());
        markDirty();
        sync();
    }

    public void setShareKnowledge(boolean share) {
        if (isSharingKnowledge() == share)
            return;
        knowledge = knowledge.convert(getOwner());
        markDirty();
        sync();
    }

    public void markDirty() {
        TPRSavedData.getData().setDirty();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", teamUUID);
        tag.putUUID("owner", owner);
        ListTag list = new ListTag();
        for (UUID member : members) {
            CompoundTag t = new CompoundTag();
            t.putUUID("uuid", member);
            list.add(t);
        }
        tag.put("members", list);

        tag.put("knowledge", knowledge.save());
        tag.put("emc", emc.save());

        return tag;
    }


    public static TPRTeam getOrCreateTeam(UUID uuid) {
        TPRTeam team = getTeamByMember(uuid);
        if (team == null)
            team = createTeam(uuid);
        return team;
    }

    public static TPRTeam createTeam(UUID uuid) {
        TPRTeam team = new TPRTeam(uuid);
        TPRSavedData.getData().teams.put(team.getUUID(), team);
        TPRSavedData.getData().setDirty();
        return team;
    }

    public static TPRTeam getTeam(UUID uuid) {
        return TPRSavedData.getData().teams.get(uuid);
    }

    public static boolean isInTeam(UUID uuid) {
        return getTeamByMember(uuid) != null;
    }

    public static TPRTeam getTeamByMember(UUID uuid) {
        UUID teamUUID = TPRSavedData.getData().playerTeamCache.get(uuid);

        if (teamUUID == null)
            for (Map.Entry<UUID, TPRTeam> entry : TPRSavedData.getData().teams.entrySet()) {
                if (entry.getValue().getAll().contains(uuid)) {
                    teamUUID = entry.getKey();
                    TPRSavedData.getData().playerTeamCache.put(uuid, teamUUID);
                    break;
                }
            }

        if (teamUUID != null)
            return TPRSavedData.getData().teams.get(teamUUID);
        return null;
    }

    public void sync() {
        TeamProjectERebornMod.getAllOnline(getAll()).forEach(TeamProjectERebornMod::sync);
    }

    public void sync(UUID uuid) {
        TeamProjectERebornMod.getAllOnline(List.of(uuid)).forEach(TeamProjectERebornMod::sync);
    }

}
