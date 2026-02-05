package cn.leomc.teamprojecte.integration.clothconfig;

import cn.leomc.teamprojecte.TPConfig;
import cn.leomc.teamprojecte.TeamProjectEMod;
import cn.leomc.teamprojecte.mixin.TPMixinPlugin;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClothConfig {

    public static Screen genConfigScreen(Screen parent) {

        ConfigBuilder builder = ConfigBuilder.create()
                .setTitle(Component.translatable("config.teamprojecte.title"))
                .setParentScreen(parent)
                .setSavingRunnable(() -> {
                    TPConfig.write(TeamProjectEMod.CONFIG);
                    TPMixinPlugin.CONFIG = TeamProjectEMod.CONFIG;
                });

        ConfigCategory client = builder.getOrCreateCategory(Component.translatable("config.teamprojecte.client"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        client.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.teamprojecte.enable_xaerominimap_emc_display"),
                        TeamProjectEMod.CONFIG.isEnableXaeroMinimapEMCDisplay())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.teamprojecte.enable_xaerominimap_emc_display.tooltip"))
                .setSaveConsumer(newValue -> TeamProjectEMod.CONFIG.setEnableXaeroMinimapEMCDisplay(newValue))
                .requireRestart()
                .build()
        );

        client.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.teamprojecte.enable_xaerominimap_emc_display_hold_shift_show_full"),
                        TeamProjectEMod.CONFIG.isEnableXaeroMinimapEMCDisplayHoldShiftShowFull())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.teamprojecte.enable_xaerominimap_emc_display_hold_shift_show_full.tooltip"))
                .setSaveConsumer(newValue ->
                        TeamProjectEMod.CONFIG.setEnableXaeroMinimapEMCDisplayHoldShiftShowFull(newValue))
                .build()
        );

        client.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.teamprojecte.enable_xaerominimap_emc_display_rate"),
                        TeamProjectEMod.CONFIG.isEnableXaeroMinimapEMCDisplayRate())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.teamprojecte.enable_xaerominimap_emc_display_rate.tooltip"))
                .setSaveConsumer(newValue -> TeamProjectEMod.CONFIG.setEnableXaeroMinimapEMCDisplayRate(newValue))
                .build()
        );

        return builder.build();

    }

}
