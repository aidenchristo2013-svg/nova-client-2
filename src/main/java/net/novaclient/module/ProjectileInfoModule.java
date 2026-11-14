package net.novaclient.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lists projectiles (arrows, pearls, tridents...) near the player with distance and speed. */
public class ProjectileInfoModule extends Module {
    private static final double RANGE = 64.0;
    private static final int MAX_LINES = 8;

    public ProjectileInfoModule() {
        super("Projectile Info", "Nearby arrows, pearls and other projectiles", true);
    }

    @Override
    public List<String> infoLines(Minecraft mc) {
        List<String> out = new ArrayList<>();
        if (mc.player == null || mc.level == null) return out;

        AABB box = mc.player.getBoundingBox().inflate(RANGE);
        List<Projectile> found = mc.level.getEntitiesOfClass(Projectile.class, box);
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)));

        if (found.isEmpty()) {
            out.add("No projectiles nearby");
            return out;
        }
        for (int i = 0; i < Math.min(MAX_LINES, found.size()); i++) {
            Entity e = found.get(i);
            double blocksPerSecond = e.getDeltaMovement().length() * 20.0;
            out.add(String.format("%s  %.0fm  %.1f b/s",
                    e.getType().getDescription().getString(),
                    Math.sqrt(e.distanceToSqr(mc.player)),
                    blocksPerSecond));
        }
        if (found.size() > MAX_LINES) out.add("+" + (found.size() - MAX_LINES) + " more");
        return out;
    }
}
