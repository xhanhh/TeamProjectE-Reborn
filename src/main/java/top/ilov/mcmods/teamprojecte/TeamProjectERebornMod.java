package top.ilov.mcmods.teamprojecte;

import top.ilov.mcmods.teamprojecte.common.TPRCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

import top.ilov.mcmods.teamprojecte.common.TPRSavedData;
import top.ilov.mcmods.teamprojecte.mixin.TPRMixinPlugin;
import org.slf4j.LoggerFactory;

@Mod(TeamProjectERebornMod.MOD_ID)
public class TeamProjectERebornMod {

    public static final String MOD_ID = "teamprojecte_reborn";

    public static final Logger LOGGER = LoggerFactory.getLogger("Team ProjectE Reborn");

    public static TPRConfig CONFIG = new TPRConfig();

    public TeamProjectERebornMod() {
        NeoForge.EVENT_BUS.register(this);
        CONFIG = TPRConfig.loadConfig();
        TPRMixinPlugin.CONFIG = CONFIG;
    }

    @SubscribeEvent
    public void onRegisterCommand(RegisterCommandsEvent event) {
        TPRCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        TPRCommand.INVITATIONS.clear();
        TPRSavedData.onServerStopped();
    }

}
