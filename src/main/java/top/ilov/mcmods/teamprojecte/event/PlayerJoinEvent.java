package top.ilov.mcmods.teamprojecte.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import top.ilov.mcmods.teamprojecte.TeamProjectERebornMod;
import top.ilov.mcmods.teamprojecte.utils.TeamUtils;

@EventBusSubscriber(modid = TeamProjectERebornMod.MOD_ID)
public class PlayerJoinEvent {

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeamUtils.onPlayerJoinLevel(event.getLevel(), player);
        }
    }

}
