package top.ilov.mcmods.teamprojecte;

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

import java.math.BigInteger;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TPRSavedData extends SavedData {

    private static TPRSavedData DATA;
    private static final SavedData.Factory<TPRSavedData> FACTORY = new SavedData.Factory<>(TPRSavedData::new, TPRSavedData::new);

    static TPRSavedData getData() {
        if (DATA == null && ServerLifecycleHooks.getCurrentServer() != null)
            DATA = ServerLifecycleHooks.getCurrentServer().overworld().getDataStorage()
                    .computeIfAbsent(FACTORY, TeamProjectERebornMod.MOD_ID);
        return DATA;
    }

    static void onServerStopped() {
        DATA = null;
    }

    final Map<UUID, TPRTeam> teams = new HashMap<>();
    final Map<UUID, UUID> playerTeamCache = new HashMap<>();
    final Set<UUID> migratedPlayers = new HashSet<>();
    final Map<UUID, PlayerSnapshot> playerSnapshots = new HashMap<>();

    void invalidateCache(UUID uuid) {
        playerTeamCache.remove(uuid);
    }

    boolean hasMigrated(UUID uuid) {
        return migratedPlayers.contains(uuid);
    }

    void markMigrated(UUID uuid) {
        if (migratedPlayers.add(uuid)) {
            setDirty();
        }
    }

    boolean hasSnapshot(UUID uuid) {
        return playerSnapshots.containsKey(uuid);
    }

    void putSnapshot(UUID uuid, PlayerSnapshot snapshot) {
        playerSnapshots.put(uuid, snapshot);
        setDirty();
    }

    @Nullable
    PlayerSnapshot takeSnapshot(UUID uuid) {
        PlayerSnapshot snapshot = playerSnapshots.remove(uuid);
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
                entry.put("snapshot", snapshot.save());
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
                    playerSnapshots.put(player, PlayerSnapshot.load(entry.getCompound("snapshot")));
                }
            }
        }
    }

    static final class PlayerSnapshot {
        private final BigInteger emc;
        private final boolean fullKnowledge;
        private final Set<ItemInfo> knowledge;

        PlayerSnapshot(BigInteger emc, boolean fullKnowledge, Set<ItemInfo> knowledge) {
            this.emc = emc == null ? BigInteger.ZERO : emc;
            this.fullKnowledge = fullKnowledge;
            this.knowledge = knowledge == null ? Set.of() : Set.copyOf(knowledge);
        }

        BigInteger emc() {
            return emc;
        }

        boolean fullKnowledge() {
            return fullKnowledge;
        }

        Set<ItemInfo> knowledge() {
            return knowledge;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("emc", emc.toString());
            tag.putBoolean("fullKnowledge", fullKnowledge);
            ListTag list = new ListTag();
            for (ItemInfo info : knowledge) {
                ItemInfo.CODEC.encodeStart(NbtOps.INSTANCE, info).result().ifPresent(list::add);
            }
            tag.put("knowledge", list);
            return tag;
        }

        static PlayerSnapshot load(CompoundTag tag) {
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
            return new PlayerSnapshot(emc, fullKnowledge, knowledge);
        }
    }
}
