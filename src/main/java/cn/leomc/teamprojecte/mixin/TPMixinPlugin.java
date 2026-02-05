package cn.leomc.teamprojecte.mixin;

import cn.leomc.teamprojecte.utils.FMLUtils;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class TPMixinPlugin implements IMixinConfigPlugin {

    boolean IS_PROJECTE_LOADED = FMLUtils.isModLoaded("projecte");
    boolean IS_XEARO_LOADED = FMLUtils.isModLoaded("xaerominimap");

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {

        return switch (mixinClassName) {
            case "cn.leomc.teamprojecte.mixin.pe.KnowledgeAttachmentAccessor",
                 "cn.leomc.teamprojecte.mixin.pe.PECoreKnowledgeCapabilityMixin" -> IS_PROJECTE_LOADED;

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
