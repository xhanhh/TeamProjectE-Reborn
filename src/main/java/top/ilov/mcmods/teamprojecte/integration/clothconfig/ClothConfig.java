package top.ilov.mcmods.teamprojecte.integration.clothconfig;

import top.ilov.mcmods.teamprojecte.TPRConfig;
import top.ilov.mcmods.teamprojecte.TeamProjectERebornMod;
import top.ilov.mcmods.teamprojecte.mixin.TPRMixinPlugin;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClothConfig {

    public static Screen genConfigScreen(Screen parent) {

        ConfigBuilder builder = ConfigBuilder.create()
                .setTitle(Component.translatable("config.teamprojecte_reborn.title"))
                .setParentScreen(parent)
                .setSavingRunnable(() -> {
                    TPRConfig.write(TeamProjectERebornMod.CONFIG);
                    TPRMixinPlugin.CONFIG = TeamProjectERebornMod.CONFIG;
                });

        ConfigCategory client = builder.getOrCreateCategory(Component.translatable("config.teamprojecte_reborn.client"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        client.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.teamprojecte_reborn.enable_xaerominimap_emc_display"),
                        TeamProjectERebornMod.CONFIG.isEnableXaeroMinimapEMCDisplay())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.teamprojecte_reborn.enable_xaerominimap_emc_display.tooltip"))
                .setSaveConsumer(newValue -> TeamProjectERebornMod.CONFIG.setEnableXaeroMinimapEMCDisplay(newValue))
                .requireRestart()
                .build()
        );

        client.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.teamprojecte_reborn.enable_xaerominimap_emc_display_hold_shift_show_full"),
                        TeamProjectERebornMod.CONFIG.isEnableXaeroMinimapEMCDisplayHoldShiftShowFull())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.teamprojecte_reborn.enable_xaerominimap_emc_display_hold_shift_show_full.tooltip"))
                .setSaveConsumer(newValue ->
                        TeamProjectERebornMod.CONFIG.setEnableXaeroMinimapEMCDisplayHoldShiftShowFull(newValue))
                .build()
        );

        client.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.teamprojecte_reborn.enable_xaerominimap_emc_display_rate"),
                        TeamProjectERebornMod.CONFIG.isEnableXaeroMinimapEMCDisplayRate())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.teamprojecte_reborn.enable_xaerominimap_emc_display_rate.tooltip"))
                .setSaveConsumer(newValue -> TeamProjectERebornMod.CONFIG.setEnableXaeroMinimapEMCDisplayRate(newValue))
                .build()
        );

        return builder.build();

    }

}
