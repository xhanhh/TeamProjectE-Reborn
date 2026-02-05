package cn.leomc.teamprojecte.mixin.xaero;

import cn.leomc.teamprojecte.integration.xaero.MinimapDisplays;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.info.BuiltInInfoDisplays;
import xaero.hud.minimap.info.InfoDisplay;

import java.util.List;
import java.util.Objects;

@Mixin(BuiltInInfoDisplays.class)
public class XaeroDisplayMixin {

    @Shadow
    private static List<InfoDisplay<?>> ALL;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void xaeroInject(CallbackInfo ci) {
        Objects.requireNonNull(ALL);
        MinimapDisplays.PE_EMC = MinimapDisplays.PE_EMC_INFO_BUILDER.setDestination(ALL::add).build();
    }

}
