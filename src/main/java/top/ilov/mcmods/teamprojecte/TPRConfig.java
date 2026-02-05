package top.ilov.mcmods.teamprojecte;

import top.ilov.mcmods.teamprojecte.utils.FMLUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Data
public final class TPRConfig {

    private boolean isEnableXaeroMinimapEMCDisplay;

    private boolean isEnableXaeroMinimapEMCDisplayHoldShiftShowFull;

    private boolean isEnableXaeroMinimapEMCDisplayRate;

    private static final boolean DEFAULT_ENABLE_XAERO_MINIMAP_EMC_DISPLAY = true;
    private static final boolean DEFAULT_XAERO_MINIMAP_EMC_SHIFT_FULL = true;
    private static final boolean DEFAULT_XAERO_MINIMAP_EMC_RATE = false;

    static File config = new File(FMLUtils.getConfigDir().toFile(), "teamprojecte-reborn-common.json");

    @SneakyThrows
    public static TPRConfig loadConfig() {

        TPRConfig defaultConfig = new TPRConfig();
        defaultConfig.isEnableXaeroMinimapEMCDisplay = DEFAULT_ENABLE_XAERO_MINIMAP_EMC_DISPLAY;
        defaultConfig.isEnableXaeroMinimapEMCDisplayHoldShiftShowFull = DEFAULT_XAERO_MINIMAP_EMC_SHIFT_FULL;
        defaultConfig.isEnableXaeroMinimapEMCDisplayRate = DEFAULT_XAERO_MINIMAP_EMC_RATE;

        if (!config.exists()) {
            write(defaultConfig);
            return defaultConfig;
        }

        Gson gson = new Gson();
        JsonObject json;
        try (BufferedReader reader = Files.newBufferedReader(config.toPath(), StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }

        TPRConfig loaded = gson.fromJson(json, TPRConfig.class);
        if (loaded == null) {
            loaded = defaultConfig;
        }

        boolean changed = false;

        if (!json.has("isEnableXaeroMinimapEMCDisplay")) {
            loaded.isEnableXaeroMinimapEMCDisplay = DEFAULT_ENABLE_XAERO_MINIMAP_EMC_DISPLAY;
            changed = true;
        }
        if (!json.has("isEnableXaeroMinimapEMCDisplayHoldShiftShowFull")) {
            loaded.isEnableXaeroMinimapEMCDisplayHoldShiftShowFull = DEFAULT_XAERO_MINIMAP_EMC_SHIFT_FULL;
            changed = true;
        }
        if (!json.has("isEnableXaeroMinimapEMCDisplayRate")) {
            loaded.isEnableXaeroMinimapEMCDisplayRate = DEFAULT_XAERO_MINIMAP_EMC_RATE;
            changed = true;
        }

        if (changed) {
            write(loaded);
        }

        return loaded;

    }

    @SneakyThrows
    public static void write(TPRConfig cakeConfig) {

        Writer writer = Files.newBufferedWriter(
                config.toPath(),
                StandardCharsets.UTF_8
        );
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();
        writer.write(gson.toJson(cakeConfig));
        writer.close();

    }


}
