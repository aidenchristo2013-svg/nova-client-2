package net.novaclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.novaclient.gui.HudOverlay;
import net.novaclient.gui.NovaScreen;
import net.novaclient.module.ModuleManager;
import org.lwjgl.glfw.GLFW;

public class NovaClient implements ClientModInitializer {
    private static ModuleManager modules;
    private boolean wasDown = false;

    public static ModuleManager modules() {
        return modules;
    }

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        HudOverlay.register();

        // Open the menu on Right Shift (edge-triggered so holding the key doesn't re-open it).
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        if (mc.level != null) modules.tick(mc);
        boolean down = InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (down && !wasDown && mc.screen == null && mc.level != null) {
            mc.setScreen(new NovaScreen());
        }
        wasDown = down;
    }
}
