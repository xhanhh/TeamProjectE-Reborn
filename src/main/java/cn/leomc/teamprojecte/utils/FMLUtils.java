package cn.leomc.teamprojecte.utils;

import lombok.experimental.UtilityClass;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

@UtilityClass
public class FMLUtils {

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

}
