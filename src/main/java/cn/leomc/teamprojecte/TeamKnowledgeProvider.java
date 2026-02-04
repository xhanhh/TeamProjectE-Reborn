package cn.leomc.teamprojecte;

import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.event.PlayerKnowledgeChangeEvent;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.emc.nbt.NBTManager;
import moze_intel.projecte.gameObjs.items.Tome;
import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.packets.IPEPacket;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncChangePKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncEmcPKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncInputsAndLocksPKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncPKT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;

public class TeamKnowledgeProvider implements IKnowledgeProvider {

    private final Supplier<UUID> playerUUID;

    private final ItemStackHandler inputLocks = new ItemStackHandler(9);

    public TeamKnowledgeProvider(@NotNull ServerPlayer player) {
        this.playerUUID = Suppliers.memoize(() -> TeamProjectE.getPlayerUUID(player));
    }

    public TeamKnowledgeProvider(UUID uuid) {
        this.playerUUID = () -> uuid;
    }

    private void fireChangedEvent() {
        getTeam().getAll()
                .forEach(uuid -> NeoForge.EVENT_BUS.post(new PlayerKnowledgeChangeEvent(uuid)));
    }

    private TPTeam getTeam() {
        return TPTeam.getOrCreateTeam(playerUUID.get());
    }

    @Override
    public boolean hasFullKnowledge() {
        return getTeam().hasFullKnowledge(playerUUID.get());
    }

    @Override
    public void setFullKnowledge(boolean fullKnowledge) {
        boolean changed = hasFullKnowledge() != fullKnowledge;
        getTeam().setFullKnowledge(fullKnowledge, playerUUID.get());
        if (changed) {
            fireChangedEvent();
        }
    }

    @Override
    public void clearKnowledge() {
        boolean hasKnowledge = hasFullKnowledge() || !getTeam().getKnowledge(playerUUID.get()).isEmpty();
        getTeam().clearKnowledge(playerUUID.get());
        getTeam().setFullKnowledge(false, playerUUID.get());
        if (hasKnowledge) {
            //If we previously had any knowledge fire the fact that our knowledge changed
            fireChangedEvent();
        }
    }

    @Nullable
    private ItemInfo getIfPersistent(@NotNull ItemInfo info) {
        if (!info.hasNBT() || EMCMappingHandler.hasEmcValue(info)) {
            //If we have no NBT or the base mapping has an emc value for our item with the given NBT
            // then we don't have an extended state
            return null;
        }
        ItemInfo cleanedInfo = NBTManager.getPersistentInfo(info);
        if (cleanedInfo.hasNBT() && !EMCMappingHandler.hasEmcValue(cleanedInfo)) {
            //If we still have NBT after unimportant parts being stripped and it doesn't
            // directly have an EMC value, then we it has some persistent information
            return cleanedInfo;
        }
        return null;
    }

    @Override
    public boolean hasKnowledge(@NotNull ItemInfo info) {
        if (getTeam().hasFullKnowledge(playerUUID.get())) {
            //If we have all knowledge, check if the item has extra data and
            // may not actually be in our knowledge set but can be added to it
            ItemInfo persistentInfo = getIfPersistent(info);
            return persistentInfo == null || getTeam().getKnowledge(playerUUID.get()).contains(persistentInfo);
        }
        return getTeam().getKnowledge(playerUUID.get()).contains(NBTManager.getPersistentInfo(info));
    }

    @Override
    public boolean addKnowledge(@NotNull ItemInfo info) {
        if (getTeam().hasFullKnowledge(playerUUID.get())) {
            ItemInfo persistentInfo = getIfPersistent(info);
            if (persistentInfo == null) {
                //If the item doesn't have extra data, and we have all knowledge, don't actually add any
                return false;
            }
            //If it does have extra data, pretend we don't have full knowledge and try adding it as what we have is persistent.
            // Note: We ignore the tome here being a separate entity because it should not have any persistent info
            return tryAdd(persistentInfo);
        }
        if (info.getItem() instanceof Tome) {
            if (info.hasNBT()) {
                //Make sure we don't have any NBT as it doesn't have any effect for the tome
                info = ItemInfo.fromItem(info.getItem());
            }
            //Note: We don't bother checking if we already somehow know the tome without having full knowledge
            // as we are learning it without any NBT which means that it doesn't have any extra persistent info
            // so can just check if it is already in it by nature of it being a set
            getTeam().addKnowledge(info, playerUUID.get());
            getTeam().setFullKnowledge(true, playerUUID.get());
            fireChangedEvent();
            return true;
        }
        return tryAdd(NBTManager.getPersistentInfo(info));
    }

    private boolean tryAdd(@NotNull ItemInfo cleanedInfo) {
        if (getTeam().addKnowledge(cleanedInfo, playerUUID.get())) {
            fireChangedEvent();
            return true;
        }
        return false;
    }

    @Override
    public boolean removeKnowledge(@NotNull ItemInfo info) {
        if (getTeam().hasFullKnowledge(playerUUID.get())) {
            if (info.getItem() instanceof Tome) {
                //If we have full knowledge and are trying to remove the tome allow it
                if (info.hasNBT()) {
                    //Make sure we don't have any NBT as it doesn't have any effect for the tome
                    info = ItemInfo.fromItem(info.getItem());
                }
                getTeam().removeKnowledge(info, playerUUID.get());
                getTeam().setFullKnowledge(false, playerUUID.get());
                fireChangedEvent();
                return true;
            }
            //Otherwise check if we have any persistent information, and if so try removing that
            // as we may have it known as an "extra" item
            ItemInfo persistentInfo = getIfPersistent(info);
            return persistentInfo != null && tryRemove(persistentInfo);
        }
        return tryRemove(NBTManager.getPersistentInfo(info));
    }

    private boolean tryRemove(@NotNull ItemInfo cleanedInfo) {
        if (getTeam().removeKnowledge(cleanedInfo, playerUUID.get())) {
            fireChangedEvent();
            return true;
        }
        return false;
    }

    @NotNull
    @Override
    public Set<ItemInfo> getKnowledge() {
        if (getTeam().hasFullKnowledge(playerUUID.get())) {
            Set<ItemInfo> allKnowledge = EMCMappingHandler.getMappedItems();
            //Make sure we include any extra items they have learned such as various enchanted items.
            allKnowledge.addAll(getTeam().getKnowledge(playerUUID.get()));
            return Collections.unmodifiableSet(allKnowledge);
        }
        return Collections.unmodifiableSet(getTeam().getKnowledge(playerUUID.get()));
    }

    @NotNull
    @Override
    public IItemHandlerModifiable getInputAndLocks() {
        return inputLocks;
    }

    @Override
    public BigInteger getEmc() {
        return getTeam().getEmc(playerUUID.get());
    }

    @Override
    public void setEmc(BigInteger emc) {
        getTeam().setEmc(emc, playerUUID.get());
    }

    @Override
    public void sync(@NotNull ServerPlayer player) {
        if (!getTeam().isSharingEMC() && !getTeam().isSharingKnowledge())
            sendKnowledgeSync(player);
        else
            TeamProjectE.getOnlineTeamMembers(TeamProjectE.getPlayerUUID(player))
                    .forEach(TeamKnowledgeProvider::sendKnowledgeSync);
    }

    private static void sendKnowledgeSync(ServerPlayer player) {
        player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY)
                .ifPresent(cap -> PacketHandler.sendTo(new KnowledgeSyncPKT(((TeamKnowledgeProvider) cap).serializeForClient()), player));
    }

    private CompoundTag serializeForClient() {
        CompoundTag properties = new CompoundTag();
        properties.putString("transmutationEmc", getTeam().getEmc(playerUUID.get()).toString());
        ListTag knowledgeWrite = new ListTag();
        for (ItemInfo i : getTeam().getKnowledge(playerUUID.get()))
            knowledgeWrite.add(i.write(new CompoundTag()));

        properties.put("knowledge", knowledgeWrite);
        properties.put("inputlock", this.inputLocks.serializeNBT());
        properties.putBoolean("fullknowledge", getTeam().hasFullKnowledge(playerUUID.get()));
        return properties;
    }


    @Override
    public void syncEmc(@NotNull ServerPlayer player) {
        sendPacket(new KnowledgeSyncEmcPKT(getEmc()), player, getTeam().isSharingEMC());
    }


    @Override
    public void syncKnowledgeChange(@NotNull ServerPlayer player, ItemInfo change, boolean learned) {
        sendPacket(new KnowledgeSyncChangePKT(change, learned), player, getTeam().isSharingKnowledge());
    }

    @Override
    public void syncInputAndLocks(@NotNull ServerPlayer serverPlayer, IntList intList, TargetUpdateType targetUpdateType) {
        if (!intList.isEmpty()) {
            int slots = inputLocks.getSlots();
            Map<Integer, ItemStack> stacksToSync = new HashMap<>();
            for (int slot : intList) {
                if (slot >= 0 && slot < slots) {
                    //Validate the slot is a valid index
                    stacksToSync.put(slot, inputLocks.getStackInSlot(slot));
                }
            }
            if (!stacksToSync.isEmpty()) {
                //Validate it is not empty in case we were fed bad indices
                PacketHandler.sendTo(new KnowledgeSyncInputsAndLocksPKT(stacksToSync, targetUpdateType), serverPlayer);
            }
        }
    }

    @Override
    public void receiveInputsAndLocks(Int2ObjectMap<ItemStack> int2ObjectMap) {
        int slots = inputLocks.getSlots();
        int2ObjectMap.forEach((key, value) -> {
            int slot = key;
            if (slot >= 0 && slot < slots) {
                //Validate the slot is a valid index
                inputLocks.setStackInSlot(slot, value);
            }
        });
    }

    private static void sendPacket(IPEPacket packet, ServerPlayer player, boolean team) {
        if (team)
            TeamProjectE.getOnlineTeamMembers(TeamProjectE.getPlayerUUID(player))
                    .forEach(p -> PacketHandler.sendTo(packet, p));
        else
            PacketHandler.sendTo(packet, player);
    }

}