package net.novaclient.module;

import net.minecraft.client.Minecraft;
import net.novaclient.schematic.Schematic;
import net.novaclient.schematic.SchematicLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads .schem / .litematic files from the "nova-schematics" folder in the game directory
 * and shows size, a material list and per-layer block counts so you can plan a build.
 * It does not place blocks for you.
 */
public class SchematicModule extends Module {
    private Schematic loaded;
    private String status = "Put .schem or .litematic files in the nova-schematics folder";
    private int layer = 0;

    public SchematicModule() {
        super("Schematics", "Load a schematic and view its material list and layers", true);
    }

    @Override
    public boolean showOnHud() { return false; }

    public static Path folder(Minecraft mc) {
        return mc.gameDirectory.toPath().resolve("nova-schematics");
    }

    public List<Path> listFiles(Minecraft mc) {
        Path dir = folder(mc);
        try {
            Files.createDirectories(dir);
            try (Stream<Path> s = Files.list(dir)) {
                return s.filter(p -> {
                            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                            return n.endsWith(".schem") || n.endsWith(".litematic");
                        })
                        .sorted()
                        .toList();
            }
        } catch (IOException e) {
            status = "Could not read folder: " + e.getMessage();
            return List.of();
        }
    }

    public void load(Path file) {
        try {
            loaded = SchematicLoader.load(file);
            layer = 0;
            status = "Loaded " + loaded.name;
        } catch (IOException | RuntimeException e) {
            loaded = null;
            status = "Failed to load " + file.getFileName() + ": " + e.getMessage();
        }
    }

    public void unload() {
        loaded = null;
        status = "Unloaded";
    }

    public boolean hasSchematic() { return loaded != null; }

    public Schematic schematic() { return loaded; }

    public void nextLayer() { if (loaded != null) layer = (layer + 1) % loaded.height; }

    public void prevLayer() { if (loaded != null) layer = (layer - 1 + loaded.height) % loaded.height; }

    @Override
    public List<String> infoLines(Minecraft mc) {
        List<String> out = new ArrayList<>();
        out.add(status);
        if (loaded == null) return out;

        out.add(String.format("%d x %d x %d  (%d blocks)", loaded.width, loaded.height, loaded.length,
                loaded.nonAirCount()));
        out.add(String.format("Layer %d / %d: %d blocks", layer + 1, loaded.height, loaded.layerCount(layer)));
        out.add("Materials:");
        int shown = 0;
        for (Map.Entry<String, Integer> e : loaded.materialList().entrySet()) {
            if (shown++ >= 10) { out.add("  ..."); break; }
            out.add(String.format("  %s x%d", e.getKey().replace("minecraft:", ""), e.getValue()));
        }
        return out;
    }
}
