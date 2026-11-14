package net.novaclient.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.novaclient.NovaClient;
import net.novaclient.module.Module;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Searchable menu. Left: module toggles filtered by the search box.
 * Right: live info from every enabled module. Bottom: schematic file picker.
 */
public class NovaScreen extends Screen {
    private static final int LEFT = 12;
    private static final int ROW_H = 22;
    private static final int LIST_W = 180;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFAAAAAA;

    private EditBox search;
    private String lastQuery = "";
    private final List<Button> dynamic = new ArrayList<>();

    public NovaScreen() {
        super(Component.literal("Nova"));
    }

    @Override
    protected void init() {
        search = new EditBox(this.font, LEFT, 28, LIST_W, 18, Component.literal("Search"));
        search.setHint(Component.literal("Search tools..."));
        search.setResponder(q -> rebuild());
        addRenderableWidget(search);
        setInitialFocus(search);
        rebuild();
    }

    /** Recreates the module buttons for the current search text. */
    private void rebuild() {
        for (Button b : dynamic) removeWidget(b);
        dynamic.clear();
        lastQuery = search == null ? "" : search.getValue();

        int y = 54;
        for (Module m : NovaClient.modules().search(lastQuery)) {
            Button b = Button.builder(label(m), btn -> {
                        m.toggle();
                        NovaClient.modules().save();
                        btn.setMessage(label(m));
                    })
                    .bounds(LEFT, y, LIST_W, 20)
                    .build();
            dynamic.add(addRenderableWidget(b));
            y += ROW_H;
        }

        // Visual options
        var visuals = NovaClient.modules().visuals;
        if (visuals.isEnabled()) {
            dynamic.add(addRenderableWidget(Button.builder(Component.literal("HUD corner: " + visuals.cornerName()),
                    btn -> { visuals.cycleCorner(); NovaClient.modules().save();
                             btn.setMessage(Component.literal("HUD corner: " + visuals.cornerName())); })
                    .bounds(LEFT, y, LIST_W, 20).build()));
            y += ROW_H;
            dynamic.add(addRenderableWidget(Button.builder(Component.literal("HUD colour: " + visuals.colourName()),
                    btn -> { visuals.cycleColour(); NovaClient.modules().save();
                             btn.setMessage(Component.literal("HUD colour: " + visuals.colourName())); })
                    .bounds(LEFT, y, LIST_W, 20).build()));
            y += ROW_H;
            dynamic.add(addRenderableWidget(Button.builder(Component.literal("HUD shadow: " + (visuals.shadow() ? "on" : "off")),
                    btn -> { visuals.toggleShadow(); NovaClient.modules().save();
                             btn.setMessage(Component.literal("HUD shadow: " + (visuals.shadow() ? "on" : "off"))); })
                    .bounds(LEFT, y, LIST_W, 20).build()));
            y += ROW_H;
        }

        // Builder controls
        var builder = NovaClient.modules().builder;
        if (builder.isEnabled()) {
            dynamic.add(addRenderableWidget(Button.builder(Component.literal("Set origin here"),
                            btn -> builder.setOriginHere(Minecraft.getInstance()))
                    .bounds(LEFT, y, LIST_W, 20).build()));
            y += ROW_H;
            dynamic.add(addRenderableWidget(Button.builder(Component.literal("Start / pause build"),
                            btn -> builder.toggleRunning())
                    .bounds(LEFT, y, LIST_W, 20).build()));
            y += ROW_H;
        }

        // Schematic file picker
        y += 10;
        var schem = NovaClient.modules().schematics;
        if (schem.isEnabled()) {
            for (Path file : schem.listFiles(Minecraft.getInstance())) {
                Button b = Button.builder(Component.literal("Load: " + file.getFileName()), btn -> schem.load(file))
                        .bounds(LEFT, y, LIST_W, 20)
                        .build();
                dynamic.add(addRenderableWidget(b));
                y += ROW_H;
                if (y > this.height - 40) break;
            }
            if (schem.hasSchematic()) {
                dynamic.add(addRenderableWidget(Button.builder(Component.literal("Prev layer"), btn -> schem.prevLayer())
                        .bounds(LEFT, y, LIST_W / 2 - 2, 20).build()));
                dynamic.add(addRenderableWidget(Button.builder(Component.literal("Next layer"), btn -> schem.nextLayer())
                        .bounds(LEFT + LIST_W / 2 + 2, y, LIST_W / 2 - 2, 20).build()));
            }
        }
    }

    private static Component label(Module m) {
        Component text = Component.literal(m.name + ": " + (m.isEnabled() ? "ON" : "OFF"));
        if (!m.isRisky()) return text;
        // Red flag: this tool can get you banned on servers that prohibit it.
        return Component.literal("[RED FLAG] ").withStyle(ChatFormatting.RED).append(text);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawString(this.font, "Nova", LEFT, 12, WHITE);

        int x = LEFT + LIST_W + 24;
        int y = 28;
        Minecraft mc = Minecraft.getInstance();
        for (Module m : NovaClient.modules().all()) {
            if (!m.isEnabled()) continue;
            g.drawString(this.font, m.isRisky() ? m.name + "  [RED FLAG: can get you banned]" : m.name,
                    x, y, m.isRisky() ? 0xFFFF5555 : WHITE);
            y += 12;
            for (String line : m.infoLines(mc)) {
                g.drawString(this.font, line, x + 6, y, GREY);
                y += 10;
            }
            y += 8;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
