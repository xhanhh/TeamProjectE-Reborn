package cn.leomc.teamprojecte.mixin;

import cn.leomc.teamprojecte.TPConfig;
import cn.leomc.teamprojecte.utils.FMLUtils;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class TPMixinPlugin implements IMixinConfigPlugin {

    public static TPConfig CONFIG = new TPConfig();

    @Override
    public void onLoad(String mixinPackage) {
        try {
            CONFIG = TPConfig.loadConfig();
        } catch (Exception e) {
            CONFIG = new TPConfig();
        }
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {

        return switch (mixinClassName) {
            case "cn.leomc.teamprojecte.mixin.pe.KnowledgeAttachmentAccessor",
                 "cn.leomc.teamprojecte.mixin.pe.PECoreKnowledgeCapabilityMixin" ->
                    FMLUtils.isClassPresent("moze_intel.projecte.PECore");

            case "cn.leomc.teamprojecte.mixin.xaero.XaeroDisplayMixin" ->
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
