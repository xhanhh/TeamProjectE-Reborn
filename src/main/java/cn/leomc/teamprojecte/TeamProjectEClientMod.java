package cn.leomc.teamprojecte;

import cn.leomc.teamprojecte.integration.clothconfig.ClothConfig;
import cn.leomc.teamprojecte.utils.FMLUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.function.Supplier;

@Mod(value = TeamProjectEMod.MOD_ID, dist = Dist.CLIENT)
public class TeamProjectEClientMod {

    public TeamProjectEClientMod(ModContainer container) {

        if (FMLUtils.isModLoaded("cloth_config")) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (Supplier<IConfigScreenFactory>) () ->
                            (mod, parent) -> ClothConfig.genConfigScreen(parent)
            );
        }

    }

}
