package space.shugen.danmaku;

import io.github.prospector.modmenu.ModMenu;
import io.github.prospector.modmenu.config.ModMenuConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;


public class ConfigManager {
    private static File file;
    private static DanmakuConfig config;
    private static void prepareBiomeConfigFile() {
        if (file != null) {
            return;
        }
        file = new File(FabricLoader.getInstance().getConfigDirectory(), Main.MOD_ID + ".json");
    }
    public static DanmakuConfig initializeConfig() {
        if (config != null) {
            return config;
        }

        config = new DanmakuConfig();
        load();

        return config;
    }

    private static void load() {
        prepareBiomeConfigFile();
        try {
            if (!file.exists()) {
                save();
            }
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));

                config = Main.GSON.fromJson(br, DanmakuConfig.class);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Couldn't load Danmaku Mod configuration file; reverting to defaults");
            e.printStackTrace();
        }
    }
    public static void save() {
        prepareBiomeConfigFile();

        String jsonString = Main.GSON.toJson(config);

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(jsonString);
        } catch (IOException e) {
            System.err.println("Couldn't save Danmaku Mod configuration file");
            e.printStackTrace();
        }
    }

    public static DanmakuConfig getConfig() {
        return config;
    }
}
