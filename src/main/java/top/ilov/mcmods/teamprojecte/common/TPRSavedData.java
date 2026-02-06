package top.ilov.mcmods.teamprojecte.common;

import moze_intel.projecte.api.ItemInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ilov.mcmods.teamprojecte.TeamProjectERebornMod;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TPRSavedData extends SavedData {

    private static TPRSavedData DATA;
    private static final SavedData.Factory<TPRSavedData> FACTORY = new SavedData.Factory<>(TPRSavedData::new, TPRSavedData::new);

    @Nullable
    public static TPRSavedData getData() {
        if (DATA == null && ServerLifecycleHooks.getCurrentServer() != null)
            DATA = ServerLifecycleHooks.getCurrentServer().overworld().getDataStorage()
                    .computeIfAbsent(FACTORY, TeamProjectERebornMod.MOD_ID);
        return DATA;
    }

    public static void onServerStopped() {
        DATA = null;
    }

    final Map<UUID, TPRTeam> teams = new HashMap<>();
    final Map<UUID, UUID> playerTeamCache = new HashMap<>();
    final Set<UUID> migratedPlayers = new HashSet<>();
    final Map<UUID, PlayerSnapshotData> playerSnapshots = new HashMap<>();

    public void invalidateCache(UUID uuid) {
        playerTeamCache.remove(uuid);
    }

    public boolean hasMigrated(UUID uuid) {
        return migratedPlayers.contains(uuid);
    }

    public void markMigrated(UUID uuid) {
        if (migratedPlayers.add(uuid)) {
            setDirty();
        }
    }

    public boolean hasSnapshot(UUID uuid) {
        return playerSnapshots.containsKey(uuid);
    }

    public void putSnapshot(UUID uuid, BigInteger emc, boolean fullKnowledge, Set<ItemInfo> knowledge) {
        playerSnapshots.put(uuid, new PlayerSnapshotData(emc, fullKnowledge, knowledge));
        setDirty();
    }

    @Nullable
    public PlayerSnapshotData takeSnapshot(UUID uuid) {
        PlayerSnapshotData snapshot = playerSnapshots.remove(uuid);
        if (snapshot != null) {
            setDirty();
        }
        return snapshot;
    }

    TPRSavedData() {
    }

    @NotNull
    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        compoundTag.putString("version", "1");

        ListTag teams = new ListTag();
        this.teams.forEach((uuid, team) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("uuid", uuid);
            t.put("team", team.save());
            teams.add(t);
        });
        compoundTag.put("teams", teams);

        ListTag migrated = new ListTag();
        migratedPlayers.forEach(uuid -> migrated.add(NbtUtils.createUUID(uuid)));
        compoundTag.put("migratedPlayers", migrated);

        if (!playerSnapshots.isEmpty()) {
            ListTag snapshots = new ListTag();
            playerSnapshots.forEach((uuid, snapshot) -> {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("player", uuid);
                entry.put("snapshot", saveSnapshot(snapshot));
                snapshots.add(entry);
            });
            compoundTag.put("playerSnapshots", snapshots);
        }

        return compoundTag;
    }

    TPRSavedData(CompoundTag tag, HolderLookup.Provider provider) {
        TeamProjectERebornMod.LOGGER.debug(tag.toString());
        String version = tag.getString("version");
        for (Tag t : tag.getList("teams", Tag.TAG_COMPOUND)) {
            CompoundTag team = (CompoundTag) t;
            teams.put(team.getUUID("uuid"), new TPRTeam(team.getCompound("team"), version));
        }

        migratedPlayers.clear();
        for (Tag t : tag.getList("migratedPlayers", Tag.TAG_INT_ARRAY)) {
            migratedPlayers.add(NbtUtils.loadUUID(t));
        }

        playerSnapshots.clear();
        if (tag.contains("playerSnapshots", Tag.TAG_LIST)) {
            for (Tag t : tag.getList("playerSnapshots", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) t;
                UUID player = entry.getUUID("player");
                if (entry.contains("snapshot", Tag.TAG_COMPOUND)) {
                    playerSnapshots.put(player, loadSnapshot(entry.getCompound("snapshot")));
                }
            }
        }
    }

    public record PlayerSnapshotData(BigInteger emc, boolean fullKnowledge, Set<ItemInfo> knowledge) {
        public PlayerSnapshotData {
            if (emc == null) {
                emc = BigInteger.ZERO;
            }
            if (knowledge == null) {
                knowledge = Set.of();
            } else {
                knowledge = Set.copyOf(knowledge);
            }
        }
    }

    private static CompoundTag saveSnapshot(PlayerSnapshotData snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putString("emc", snapshot.emc().toString());
        tag.putBoolean("fullKnowledge", snapshot.fullKnowledge());
        ListTag list = new ListTag();
        for (ItemInfo info : snapshot.knowledge()) {
            ItemInfo.CODEC.encodeStart(NbtOps.INSTANCE, info).result().ifPresent(list::add);
        }
        tag.put("knowledge", list);
        return tag;
    }

    private static PlayerSnapshotData loadSnapshot(CompoundTag tag) {
        BigInteger emc;
        try {
            emc = new BigInteger(tag.getString("emc"));
        } catch (Exception ex) {
            emc = BigInteger.ZERO;
        }
        boolean fullKnowledge = tag.getBoolean("fullKnowledge");
        Set<ItemInfo> knowledge = new HashSet<>();
        for (Tag entry : tag.getList("knowledge", Tag.TAG_COMPOUND)) {
            ItemInfo.CODEC.parse(NbtOps.INSTANCE, entry).result().ifPresent(knowledge::add);
        }
        return new PlayerSnapshotData(emc, fullKnowledge, knowledge);
    }
}
