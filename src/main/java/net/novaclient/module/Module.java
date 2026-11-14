package net.novaclient.module;

import net.minecraft.client.Minecraft;

import java.util.List;

/** A toggleable tool. Modules only read game state; they never modify it. */
public abstract class Module {
    public final String name;
    public final String description;
    private boolean enabled;

    protected Module(String name, String description, boolean enabledByDefault) {
        this.name = name;
        this.description = description;
        this.enabled = enabledByDefault;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void toggle() { enabled = !enabled; }

    /** Text lines this module wants shown in the Nova panel. Called every frame while enabled. */
    public abstract List<String> infoLines(Minecraft mc);

    /** True if using this tool can get you banned on servers that prohibit it. Shown as a red flag in the menu. */
    public boolean isRisky() { return false; }

    /** Whether this module's lines appear on the always-on HUD. */
    public boolean showOnHud() { return true; }

    /** Called once per client tick while a world is loaded. Does nothing by default. */
    public void tick(Minecraft mc) {}
}
