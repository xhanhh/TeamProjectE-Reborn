package cn.leomc.teamprojecte.mixin;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.impl.capability.KnowledgeImpl;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.math.BigInteger;
import java.util.Set;

@Mixin(value = KnowledgeImpl.KnowledgeAttachment.class, remap = false)
public interface KnowledgeAttachmentAccessor {

    @Accessor("knowledge")
    Set<ItemInfo> teamprojecte$getKnowledge();

    @Accessor("inputLocks")
    ItemStackHandler teamprojecte$getInputLocks();

    @Accessor("emc")
    void teamprojecte$setEmc(BigInteger emc);

    @Accessor("fullKnowledge")
    void teamprojecte$setFullKnowledge(boolean fullKnowledge);
}

