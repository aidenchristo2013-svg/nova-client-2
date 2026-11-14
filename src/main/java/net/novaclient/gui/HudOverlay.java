package net.novaclient.gui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.novaclient.NovaClient;
import net.novaclient.module.Module;
import net.novaclient.module.VisualsModule;

import java.util.ArrayList;
import java.util.List;

/** Always-on info panel drawn over the game. Hidden while the menu is open or the HUD is hidden (F1). */
public final class HudOverlay {
    private static final int MARGIN = 4;
    private static final int LINE_H = 10;

    private HudOverlay() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("nova-client", "info"),
                HudOverlay::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        if (mc.screen instanceof NovaScreen) return;

        List<String> lines = new ArrayList<>();
        for (Module m : NovaClient.modules().all()) {
            if (!m.isEnabled() || !m.showOnHud()) continue;
            List<String> info = m.infoLines(mc);
            if (info.isEmpty()) continue;
            if (!lines.isEmpty()) lines.add("");
            lines.add(m.name);
            lines.addAll(info);
        }
        if (lines.isEmpty()) return;

        VisualsModule v = NovaClient.modules().visuals;
        Font font = mc.font;
        int width = 0;
        for (String l : lines) width = Math.max(width, font.width(l));
        int height = lines.size() * LINE_H;

        int corner = v.corner();
        int x = (corner == 1 || corner == 3) ? g.guiWidth() - width - MARGIN : MARGIN;
        int y = (corner >= 2) ? g.guiHeight() - height - MARGIN : MARGIN;

        for (String l : lines) {
            g.drawString(font, l, x, y, v.colourArgb(), v.shadow());
            y += LINE_H;
        }
    }
}
