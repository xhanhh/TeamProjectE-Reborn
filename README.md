## Team ProjectE Reborn
ProjectE knowledge and EMC syncing reborn!

This mod **forked from [Team ProjectE](https://www.curseforge.com/minecraft/mc-mods/team-projecte) by 6LeoY**, please **do not report problems to the original work**.

### Links
Modrinth: [https://modrinth.com/project/teamprojecte-reborn](https://modrinth.com/project/teamprojecte-reborn)

CurseForge: [https://www.curseforge.com/minecraft/mc-mods/teamprojecte-reborn](https://www.curseforge.com/minecraft/mc-mods/teamprojecte-reborn)

### 📖 About:
This mod inherits the functionality of the original Team ProjectE mod, but adds a few improvements and enhancements, and brings it to a higher version of Minecraft:

- Shared EMC and knowledge with your projecte team members.

**Changes:**

- Support MC 1.21.1
- **Fixed an issue where EMC and knowledge were cleared when the mod was added to a world that had already been created.** Even if the existing saves already exist the EMC and knowledge data of the player, they will not be cleared.
- The integration of Xaero's Minimap emc display has been moved to [Project Minimap Hud](https://modrinth.com/mod/project-minimap-hud) (after v1.1.0). ~~Integrated display of player/team EMC and EMC change rate in [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) display information.~~
- Optimized the logic of players joining team to deal with EMC and knowledge. See usage below for details.
- Fixed an issue where the transfer ownership command would not synchronize in real time.

### 🥁 Usage:
The command is used to operate the team, and the management interface of the GUI is in the plan.

#### Invite players
Invite a player to join your team, and his EMC and knowledge will be saved and replaced with yours, sharing the team with you.

```
/teamper invite <player_name>
```

#### Leave your current team
When a player leaves, replace the player's progress before the player joins the team. If the captain transfers the CAPTAIN without saving before joining the team, the progress in the team will be preserved when he leaves the team.

```
/teamper leave
```

#### Kick a team member
Refer to "Leave your current team" above for logic of progress data.

```
/teamper kick <member_name>
```

#### List team members
Only if team members > 1.

```
/teamper members
```

#### Transfer ownership

```
/teamper transfer_ownership <member_name>
```

#### Settings
Only if team members > 1 && player is team owner.

Set EMC sharing, default: true

```
/teamper settings share_emc <boolean>
```

Set knowledge sharing, default: true

```
/teamper settings share_knowledge <boolean>
```

### 🎮 Compatibility
Should be compatible with most PE additions.