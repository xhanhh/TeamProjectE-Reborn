package top.ilov.mcmods.teamprojecte.mixin;

import top.ilov.mcmods.teamprojecte.TPRConfig;
import top.ilov.mcmods.teamprojecte.utils.FMLUtils;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class TPRMixinPlugin implements IMixinConfigPlugin {

    public static TPRConfig CONFIG = new TPRConfig();

    @Override
    public void onLoad(String mixinPackage) {
        try {
            CONFIG = TPRConfig.loadConfig();
        } catch (Exception e) {
            CONFIG = new TPRConfig();
        }
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {

        return switch (mixinClassName) {
            case "top.ilov.mcmods.teamprojecte.mixin.pe.KnowledgeAttachmentAccessor",
                 "top.ilov.mcmods.teamprojecte.mixin.pe.PECoreKnowledgeCapabilityMixin" ->
                    FMLUtils.isClassPresent("moze_intel.projecte.PECore");

            case "top.ilov.mcmods.teamprojecte.mixin.xaero.XaeroDisplayMixin" ->
                    FMLUtils.isClassPresent("xaero.hud.minimap.info.BuiltInInfoDisplays")
                            && CONFIG.isEnableXaeroMinimapEMCDisplay();

            default -> false;
        };

    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {

    }

}
