package de.xletxzx.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Look of the XLetxzX menu. Saved to config/xletxzx_visuals.json. */
public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("xletxzx_visuals.json");

    public int theme = 0;
    public int background = 0;
    public int bgIndex = 0;
    public boolean mode3d = false;
    public int winScale = 100;   // window size in percent

    private static Config instance = new Config();

    public static Config get() { return instance; }

    public static void load() {
        try {
            if (Files.exists(FILE)) instance = GSON.fromJson(Files.readString(FILE), Config.class);
        } catch (Exception e) {
            instance = new Config();
        }
        if (instance == null) instance = new Config();
    }

    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(instance)); } catch (Exception ignored) {}
    }
}
