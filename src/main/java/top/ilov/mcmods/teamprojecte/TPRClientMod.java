package top.ilov.mcmods.teamprojecte;

import top.ilov.mcmods.teamprojecte.integration.clothconfig.ClothConfig;
import top.ilov.mcmods.teamprojecte.utils.FMLUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.function.Supplier;

@Mod(value = TeamProjectERebornMod.MOD_ID, dist = Dist.CLIENT)
public class TPRClientMod {

    public TPRClientMod(ModContainer container) {

        if (FMLUtils.isModLoaded("cloth_config")) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (Supplier<IConfigScreenFactory>) () ->
                            (mod, parent) -> ClothConfig.genConfigScreen(parent)
            );
        }

    }

}
