package net.novaclient.schematic;

import java.util.*;

/**
 * A loaded schematic: dimensions, a palette of block state strings
 * (e.g. "minecraft:oak_stairs[facing=north,half=bottom]") and one palette index per cell.
 * Cell order is x fastest, then z, then y.
 */
public final class Schematic {
    public final String name;
    public final int width, height, length;
    public final List<String> palette;
    private final int[] cells;

    public Schematic(String name, int width, int height, int length, List<String> palette, int[] cells) {
        if ((long) width * height * length != cells.length) {
            throw new IllegalArgumentException("Cell count does not match dimensions");
        }
        this.name = name;
        this.width = width;
        this.height = height;
        this.length = length;
        this.palette = List.copyOf(palette);
        this.cells = cells;
    }

    public String stateAt(int x, int y, int z) {
        return palette.get(cells[(y * length + z) * width + x]);
    }

    public static boolean isAir(String state) {
        return state.startsWith("minecraft:air") || state.startsWith("minecraft:cave_air")
                || state.startsWith("minecraft:void_air");
    }

    /** Block id without properties. */
    public static String blockId(String state) {
        int i = state.indexOf('[');
        return i < 0 ? state : state.substring(0, i);
    }

    public int nonAirCount() {
        int n = 0;
        boolean[] air = new boolean[palette.size()];
        for (int i = 0; i < air.length; i++) air[i] = isAir(palette.get(i));
        for (int c : cells) if (!air[c]) n++;
        return n;
    }

    /** Material list: block id -> count, sorted by count descending. Air is skipped. */
    public LinkedHashMap<String, Integer> materialList() {
        Map<String, Integer> counts = new HashMap<>();
        for (int c : cells) {
            String s = palette.get(c);
            if (isAir(s)) continue;
            counts.merge(blockId(s), 1, Integer::sum);
        }
        LinkedHashMap<String, Integer> sorted = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted((a, b) -> b.getValue() != a.getValue().intValue()
                        ? Integer.compare(b.getValue(), a.getValue())
                        : a.getKey().compareTo(b.getKey()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    /** Number of non-air blocks in one horizontal layer (y). */
    public int layerCount(int y) {
        int n = 0;
        for (int z = 0; z < length; z++)
            for (int x = 0; x < width; x++)
                if (!isAir(stateAt(x, y, z))) n++;
        return n;
    }
}
