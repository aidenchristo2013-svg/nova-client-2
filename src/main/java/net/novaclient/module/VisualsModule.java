package net.novaclient.module;

import net.minecraft.client.Minecraft;

import java.util.List;

/** HUD appearance: which corner the info panel sits in, and its text colour. */
public class VisualsModule extends Module {
    public static final String[] CORNERS = {"Top left", "Top right", "Bottom left", "Bottom right"};
    public static final String[] COLOUR_NAMES = {"White", "Cyan", "Green", "Gold"};
    private static final int[] COLOURS = {0xFFFFFFFF, 0xFF55FFFF, 0xFF55FF55, 0xFFFFAA00};

    private int corner = 0;
    private int colour = 0;
    private boolean shadow = true;

    public VisualsModule() {
        super("Visuals", "HUD corner, text colour and shadow", true);
    }

    @Override
    public boolean showOnHud() { return false; }

    public int corner() { return corner; }
    public int colourArgb() { return COLOURS[colour]; }
    public boolean shadow() { return shadow; }

    public void cycleCorner() { corner = (corner + 1) % CORNERS.length; }
    public void cycleColour() { colour = (colour + 1) % COLOURS.length; }
    public void toggleShadow() { shadow = !shadow; }

    public String cornerName() { return CORNERS[corner]; }
    public String colourName() { return COLOUR_NAMES[colour]; }

    public void restore(int corner, int colour, boolean shadow) {
        this.corner = Math.floorMod(corner, CORNERS.length);
        this.colour = Math.floorMod(colour, COLOURS.length);
        this.shadow = shadow;
    }

    @Override
    public List<String> infoLines(Minecraft mc) {
        return List.of("Corner: " + cornerName(), "Colour: " + colourName(), "Shadow: " + (shadow ? "on" : "off"));
    }
}
