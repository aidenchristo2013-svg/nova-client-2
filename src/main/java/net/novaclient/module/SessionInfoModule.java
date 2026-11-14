package net.novaclient.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.List;

/** FPS and ping. */
public class SessionInfoModule extends Module {
    public SessionInfoModule() {
        super("Session Info", "FPS and ping", true);
    }

    @Override
    public List<String> infoLines(Minecraft mc) {
        List<String> out = new ArrayList<>();
        out.add("FPS: " + mc.getFps());
        if (mc.player != null && mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info != null) out.add("Ping: " + info.getLatency() + " ms");
        }
        return out;
    }
}
