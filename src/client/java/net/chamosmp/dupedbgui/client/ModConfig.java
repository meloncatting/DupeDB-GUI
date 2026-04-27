package net.chamosmp.dupedbgui.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.*;

public class ModConfig {
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("dupedbgui.json");
    private static final Gson GSON = new Gson();

    private static String token = null;
    private static boolean loaded = false;

    public static String getToken() {
        if (!loaded) load();
        return token;
    }

    public static void setToken(String t) {
        token = (t == null || t.isBlank()) ? null : t.trim();
        loaded = true;
        save();
    }

    private static void load() {
        loaded = true;
        try {
            if (!Files.exists(CONFIG_FILE)) return;
            String json = Files.readString(CONFIG_FILE);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj != null && obj.has("token")) {
                String t = obj.get("token").getAsString();
                token = t.isBlank() ? null : t;
            }
        } catch (Exception ignored) {}
    }

    private static void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("token", token != null ? token : "");
            Files.writeString(CONFIG_FILE, GSON.toJson(obj));
        } catch (Exception ignored) {}
    }
}
