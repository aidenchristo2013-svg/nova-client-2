package net.novaclient.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lists nearby players with distance and health. */
public class PlayerInfoModule extends Module {
    private static final int MAX_LINES = 8;

    public PlayerInfoModule() {
        super("Player Info", "Nearby players with distance and health", true);
    }

    @Override
    public List<String> infoLines(Minecraft mc) {
        List<String> out = new ArrayList<>();
        if (mc.player == null || mc.level == null) return out;

        List<AbstractClientPlayer> others = new ArrayList<>(mc.level.players());
        others.remove(mc.player);
        others.sort(Comparator.comparingDouble(p -> p.distanceToSqr(mc.player)));

        if (others.isEmpty()) {
            out.add("No other players in range");
            return out;
        }
        for (int i = 0; i < Math.min(MAX_LINES, others.size()); i++) {
            AbstractClientPlayer p = others.get(i);
            out.add(String.format("%s  %.0fm  %.0f HP",
                    p.getName().getString(),
                    Math.sqrt(p.distanceToSqr(mc.player)),
                    p.getHealth()));
        }
        if (others.size() > MAX_LINES) out.add("+" + (others.size() - MAX_LINES) + " more");
        return out;
    }
}
