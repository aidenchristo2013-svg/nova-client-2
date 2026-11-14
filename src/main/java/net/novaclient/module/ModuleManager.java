package net.novaclient.module;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    public final SchematicModule schematics = new SchematicModule();
    public final BuilderModule builder = new BuilderModule(schematics);
    public final VisualsModule visuals = new VisualsModule();

    public ModuleManager() {
        modules.add(new PlayerInfoModule());
        modules.add(new ProjectileInfoModule());
        modules.add(new CoordinatesModule());
        modules.add(new SessionInfoModule());
        modules.add(visuals);
        modules.add(schematics);
        modules.add(builder);
        load();
    }

    public List<Module> all() { return modules; }

    public void tick(net.minecraft.client.Minecraft mc) {
        for (Module m : modules) m.tick(mc);
    }

    /** Modules whose name or description contains the query (case-insensitive). Empty query = all. */
    public List<Module> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return modules;
        List<Module> out = new ArrayList<>();
        for (Module m : modules) {
            if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q)) {
                out.add(m);
            }
        }
        return out;
    }

    private Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("nova-client.properties");
    }

    private void load() {
        Path file = configFile();
        if (!Files.exists(file)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        for (Module m : modules) {
            String v = p.getProperty("module." + m.name);
            if (v != null) m.setEnabled(Boolean.parseBoolean(v));
        }
        try {
            visuals.restore(
                    Integer.parseInt(p.getProperty("visuals.corner", "0")),
                    Integer.parseInt(p.getProperty("visuals.colour", "0")),
                    Boolean.parseBoolean(p.getProperty("visuals.shadow", "true")));
        } catch (NumberFormatException ignored) {
            // keep defaults
        }
    }

    public void save() {
        Properties p = new Properties();
        for (Module m : modules) p.setProperty("module." + m.name, Boolean.toString(m.isEnabled()));
        p.setProperty("visuals.corner", Integer.toString(visuals.corner()));
        p.setProperty("visuals.colour", Integer.toString(java.util.Arrays.asList(VisualsModule.COLOUR_NAMES).indexOf(visuals.colourName())));
        p.setProperty("visuals.shadow", Boolean.toString(visuals.shadow()));
        try (OutputStream out = Files.newOutputStream(configFile())) {
            p.store(out, "Nova Client settings");
        } catch (IOException ignored) {
            // Settings are best-effort.
        }
    }
}
