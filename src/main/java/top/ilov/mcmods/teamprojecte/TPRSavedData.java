package top.ilov.mcmods.teamprojecte;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

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
    }

}
