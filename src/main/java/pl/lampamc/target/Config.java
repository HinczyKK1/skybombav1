package pl.lampamc.target;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public boolean tracers = true;
    public boolean hitbox = true;
    public boolean hud = true;
    public int durationSec = 60;
    public int theme = 0;

    public static final String[] NAMES = {"Czerwień", "Turkus", "Fiolet", "Niebieski", "Pomarańcz", "Róż", "Złoty", "Limonka", "Lód"};
    public static final int[] RGB = {0xE5383B, 0x2DE1A5, 0x9B5DE5, 0x3A86FF, 0xFF8A00, 0xF72585, 0xFFC300, 0x9EF01A, 0x5EEAD4};

    public static Config I = new Config();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("lampatarget.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                Config c = GSON.fromJson(Files.readString(p), Config.class);
                if (c != null) I = c;
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(I));
        } catch (Exception ignored) {
        }
    }

    public static int rgb() {
        return RGB[Math.floorMod(I.theme, RGB.length)];
    }

    public static int argb() {
        return 0xFF000000 | rgb();
    }
}
