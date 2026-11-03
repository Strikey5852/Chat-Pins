package com.strikey.chat_pins.pin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PinManager {
    private static final int MAX_STORED = 50;

    private final Path configDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, List<Pin>> pinsByWorld = new LinkedHashMap<>();

    public PinManager(Path configDir) {
        this.configDir = configDir;
    }

    public List<Pin> getPins(String worldKey) {
        return pinsByWorld.computeIfAbsent(worldKey, k -> new ArrayList<>());
    }

    public Optional<Pin> addPin(String worldKey, String sender, String content, boolean system) {
        List<Pin> pins = getPins(worldKey);
        Pin pin = new Pin(UUID.randomUUID().toString(), sender, content, System.currentTimeMillis(), system);
        pins.add(0, pin);
        if (pins.size() > MAX_STORED) {
            pins.remove(pins.size() - 1);
        }
        save(worldKey);
        return Optional.of(pin);
    }

    public boolean removePin(String worldKey, UUID id) {
        List<Pin> pins = getPins(worldKey);
        boolean removed = pins.removeIf(pin -> pin.id().equals(id.toString()));
        if (removed) {
            save(worldKey);
        }
        return removed;
    }

    public boolean removePin(String worldKey, String id) {
        List<Pin> pins = getPins(worldKey);
        boolean removed = pins.removeIf(pin -> pin.id().equals(id));
        if (removed) {
            save(worldKey);
        }
        return removed;
    }

    public boolean clearPins(String worldKey) {
        List<Pin> pins = getPins(worldKey);
        if (pins.isEmpty()) {
            return false;
        }
        pins.clear();
        Path file = getFile(worldKey);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "[]");
        } catch (IOException ignored) {}
        return true;
    }

    public void load() {
        Path serversDir = configDir.resolve("chat_pins/servers");
        if (Files.isDirectory(serversDir)) {
            try (var stream = Files.list(serversDir)) {
                stream.filter(Files::isRegularFile)
                      .forEach(path -> loadWorldFile(path.getFileName().toString().replace(".json", ""), path));
            } catch (IOException ignored) {
            }
        }
        Path singleplayerFile = configDir.resolve("chat_pins/singleplayer.json");
        if (Files.isRegularFile(singleplayerFile)) {
            loadWorldFile("singleplayer", singleplayerFile);
        }
    }

    private void loadWorldFile(String worldKey, Path path) {
        try {
            String json = Files.readString(path);
            List<Pin> pins = gson.fromJson(json, new TypeToken<List<Pin>>() {}.getType());
            if (pins != null) {
                pinsByWorld.put(worldKey, pins);
            }
        } catch (IOException ignored) {
        }
    }

    public void save(String worldKey) {
        List<Pin> pins = pinsByWorld.get(worldKey);
        if (pins == null || pins.isEmpty()) {
            return;
        }
        Path file = getFile(worldKey);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, gson.toJson(pins));
        } catch (IOException ignored) {
        }
    }

    private Path getFile(String worldKey) {
        if ("singleplayer".equals(worldKey)) {
            return configDir.resolve("chat_pins/singleplayer.json");
        }
        return configDir.resolve("chat_pins/servers/" + worldKey + ".json");
    }
}