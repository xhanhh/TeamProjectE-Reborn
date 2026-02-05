package top.ilov.mcmods.teamprojecte.utils;

import lombok.experimental.UtilityClass;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.nio.file.Path;

@UtilityClass
public class FMLUtils {

    public static boolean isModLoaded(String modId) {
        try {
            ModList modList = ModList.get();
            return modList != null && modList.isLoaded(modId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isClassPresent(String className) {
        try {
            MixinService.getService().getBytecodeProvider().getClassNode(className);
            return true;
        } catch (ClassNotFoundException | IOException e) {
            return false;
        }
    }

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

}
