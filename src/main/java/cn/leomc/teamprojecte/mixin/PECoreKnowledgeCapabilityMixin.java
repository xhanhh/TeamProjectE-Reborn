package cn.leomc.teamprojecte.mixin;

import cn.leomc.teamprojecte.TeamKnowledgeProvider;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.impl.capability.KnowledgeImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PECore.class, remap = false)
public class PECoreKnowledgeCapabilityMixin {

    @Redirect(
            method = "registerCapabilities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/capabilities/RegisterCapabilitiesEvent;registerEntity(Lnet/neoforged/neoforge/capabilities/EntityCapability;Lnet/minecraft/world/entity/EntityType;Lnet/neoforged/neoforge/capabilities/ICapabilityProvider;)V"
            ),
            remap = false
    )
    private void teamprojecte$registerEntity(RegisterCapabilitiesEvent event, EntityCapability<?, ?> capability, EntityType<?> entityType, ICapabilityProvider<?, ?, ?> provider) {
        if (capability == PECapabilities.KNOWLEDGE_CAPABILITY && entityType == EntityType.PLAYER) {
            event.registerEntity(PECapabilities.KNOWLEDGE_CAPABILITY, EntityType.PLAYER, (Player player, Void context) -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    return new TeamKnowledgeProvider(serverPlayer);
                }
                return new KnowledgeImpl(player);
            });
        } else {
            event.registerEntity((EntityCapability) capability, (EntityType) entityType, (ICapabilityProvider) provider);
        }
    }
}

