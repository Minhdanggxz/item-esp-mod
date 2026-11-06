package com.example.itemesp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class ItemEspConfig {
    public boolean enabled = true;
    public boolean onlySome = false;
    public double maxDistance = 0;
    public double fadeDistance = 0;
    public boolean dynamicColor = true;
    public int color = 0x00FFFF;
    public Set<String> items = new LinkedHashSet<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("itemesp.json");
    }

    public static ItemEspConfig load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                ItemEspConfig c = GSON.fromJson(Files.readString(p), ItemEspConfig.class);
                if (c != null) {
                    if (c.items == null) {
                        c.items = new LinkedHashSet<>();
                    }
                    return c;
                }
            }
        } catch (Exception ignored) {
        }
        return new ItemEspConfig();
    }

    public void save() {
        try {
            Files.writeString(path(), GSON.toJson(this));
        } catch (Exception ignored) {
        }
    }
}
