package net.novaclient.module;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Your position, facing and dimension. */
public class CoordinatesModule extends Module {
    public CoordinatesModule() {
        super("Coordinates", "Your position, facing direction and dimension", true);
    }

    @Override
    public List<String> infoLines(Minecraft mc) {
        List<String> out = new ArrayList<>();
        if (mc.player == null || mc.level == null) return out;
        BlockPos p = mc.player.blockPosition();
        out.add(String.format("XYZ: %d / %d / %d", p.getX(), p.getY(), p.getZ()));
        out.add("Facing: " + mc.player.getDirection().getName());
        out.add("Dimension: " + mc.level.dimension().identifier().getPath());
        return out;
    }
}
