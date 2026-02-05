package top.ilov.mcmods.teamprojecte.event;

import top.ilov.mcmods.teamprojecte.TPRTeam;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TeamChangeEvent extends Event {
    @Nullable
    private final UUID playerUUID;
    @Nullable
    private final TPRTeam team;
    @Nullable
    private final TPRTeam newTeam;

    public TeamChangeEvent(@Nullable UUID playerUUID, @Nullable TPRTeam team, @Nullable TPRTeam newTeam) {
        this.playerUUID = playerUUID;
        this.team = team;
        this.newTeam = newTeam;
    }

    /**
     * @return The UUID of the player changing teams, or {@code null} if no team member changes have occurred.
     */
    @Nullable
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * @return The team being changed, either in members, owner or sharing status, or {@code null} if a player joins
     * a team but was not already in another team.
     */
    @Nullable
    public TPRTeam getTeam() {
        return team;
    }

    /**
     * @return The new team that a player has joined, or {@code null} if either no team member changes have occurred
     * or a player has left or been kicked from their old team.
     */
    @Nullable
    public TPRTeam getNewTeam() {
        return newTeam;
    }
}
