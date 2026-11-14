package net.novaclient.module;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.novaclient.schematic.Schematic;

import java.util.ArrayList;
import java.util.List;

/**
 * Places blocks from the loaded schematic, one at a time, within normal reach.
 *
 * Limits: matches block type only (stairs, logs etc. are not rotated to the schematic's
 * properties), uses blocks already in your hotbar, and only places against an existing
 * neighbouring block. Many servers prohibit automated building, so check the rules first.
 */
public class BuilderModule extends Module {
    private static final double REACH = 4.5;
    private static final int TICKS_BETWEEN_PLACEMENTS = 3;

    private final SchematicModule source;
    private BlockPos origin;
    private boolean running;
    private int cooldown;
    private int placed;
    private String status = "Load a schematic, stand where it should start, then set the origin";

    public BuilderModule(SchematicModule source) {
        super("Builder", "Place blocks from the loaded schematic within reach", false);
        this.source = source;
    }

    public boolean isRunning() { return running; }

    @Override
    public boolean isRisky() { return true; }

    public void setOriginHere(Minecraft mc) {
        if (mc.player == null) return;
        origin = mc.player.blockPosition();
        placed = 0;
        status = "Origin set at " + origin.toShortString();
    }

    public void toggleRunning() {
        if (!running && (origin == null || !source.hasSchematic())) {
            status = "Load a schematic and set the origin first";
            return;
        }
        running = !running;
        status = running ? "Building..." : "Paused";
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) running = false;
    }

    @Override
    public List<String> infoLines(Minecraft mc) {
        List<String> out = new ArrayList<>();
        out.add(status);
        if (origin != null) out.add("Origin: " + origin.toShortString() + "   Placed: " + placed);
        return out;
    }

    @Override
    public void tick(Minecraft mc) {
        if (!isEnabled() || !running) return;
        Schematic s = source.schematic();
        if (s == null || origin == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.screen != null) return;
        if (cooldown-- > 0) return;

        Vec3 eye = mc.player.getEyePosition();
        int r = (int) Math.ceil(REACH);
        int minX = Math.max(0, (int) Math.floor(eye.x) - r - origin.getX());
        int maxX = Math.min(s.width - 1, (int) Math.floor(eye.x) + r - origin.getX());
        int minY = Math.max(0, (int) Math.floor(eye.y) - r - origin.getY());
        int maxY = Math.min(s.height - 1, (int) Math.floor(eye.y) + r - origin.getY());
        int minZ = Math.max(0, (int) Math.floor(eye.z) - r - origin.getZ());
        int maxZ = Math.min(s.length - 1, (int) Math.floor(eye.z) + r - origin.getZ());

        boolean missingItems = false;
        boolean outstanding = false;

        for (int y = minY; y <= maxY; y++) {          // bottom to top
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    String state = s.stateAt(x, y, z);
                    if (Schematic.isAir(state)) continue;

                    BlockPos pos = origin.offset(x, y, z);
                    if (eye.distanceToSqr(Vec3.atCenterOf(pos)) > REACH * REACH) continue;

                    Block want = BuiltInRegistries.BLOCK.getValue(Identifier.parse(Schematic.blockId(state)));
                    BlockState current = mc.level.getBlockState(pos);
                    if (current.is(want)) continue;
                    if (!current.canBeReplaced()) continue;
                    outstanding = true;

                    int slot = findHotbarSlot(mc, want.asItem());
                    if (slot < 0) { missingItems = true; continue; }

                    BlockHitResult hit = hitAgainstNeighbour(mc, pos);
                    if (hit == null) continue; // nothing to place against yet

                    mc.player.getInventory().setSelectedSlot(slot);
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                    placed++;
                    cooldown = TICKS_BETWEEN_PLACEMENTS;
                    status = "Building... (" + placed + " placed)";
                    return;
                }
            }
        }

        if (missingItems) status = "Waiting: put the needed blocks in your hotbar";
        else if (outstanding) status = "Nothing placeable from here: move closer or add support blocks";
        else status = "Nothing left in reach (" + placed + " placed)";
    }

    private static int findHotbarSlot(Minecraft mc, Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    /** Finds a solid neighbour of pos to click on, so the new block lands in pos. */
    private static BlockHitResult hitAgainstNeighbour(Minecraft mc, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            BlockState ns = mc.level.getBlockState(n);
            if (ns.isAir() || ns.canBeReplaced()) continue;
            Direction face = d.getOpposite(); // face of n that points at pos
            Vec3 hitVec = Vec3.atCenterOf(n).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            return new BlockHitResult(hitVec, face, n, false);
        }
        return null;
    }
}
