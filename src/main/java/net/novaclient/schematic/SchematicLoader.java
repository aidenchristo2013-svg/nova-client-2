package net.novaclient.schematic;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Loads Sponge .schem (v2 and v3) and Litematica .litematic files. */
public final class SchematicLoader {
    private SchematicLoader() {}

    public static Schematic load(Path path) throws IOException {
        String file = path.getFileName().toString();
        String lower = file.toLowerCase(Locale.ROOT);
        String name = file.contains(".") ? file.substring(0, file.lastIndexOf('.')) : file;
        try (InputStream in = Files.newInputStream(path)) {
            Map<String, Object> root = Nbt.readGzipped(in);
            if (lower.endsWith(".schem")) return loadSponge(name, root);
            if (lower.endsWith(".litematic")) return loadLitematic(name, root);
        }
        throw new IOException("Unsupported file type: " + file);
    }

    // ---------------- Sponge (.schem) ----------------

    static Schematic loadSponge(String name, Map<String, Object> root) throws IOException {
        Map<String, Object> schem = Nbt.compound(root, "Schematic"); // v3 wraps everything
        if (schem == null) schem = root;

        int w = Nbt.intVal(schem, "Width", 0);
        int h = Nbt.intVal(schem, "Height", 0);
        int l = Nbt.intVal(schem, "Length", 0);
        if (w <= 0 || h <= 0 || l <= 0) throw new IOException("Invalid schematic size");

        Map<String, Object> paletteTag;
        byte[] data;
        Map<String, Object> blocks = Nbt.compound(schem, "Blocks"); // v3
        if (blocks != null) {
            paletteTag = Nbt.compound(blocks, "Palette");
            data = Nbt.bytes(blocks, "Data");
        } else { // v2
            paletteTag = Nbt.compound(schem, "Palette");
            data = Nbt.bytes(schem, "BlockData");
        }
        if (paletteTag == null || data == null) throw new IOException("Missing palette or block data");

        String[] byIndex = new String[paletteTag.size()];
        for (Map.Entry<String, Object> e : paletteTag.entrySet()) {
            int idx = ((Number) e.getValue()).intValue();
            if (idx < 0 || idx >= byIndex.length) throw new IOException("Bad palette index " + idx);
            byIndex[idx] = e.getKey();
        }
        for (String s : byIndex) if (s == null) throw new IOException("Gap in palette");

        int total = Math.multiplyExact(Math.multiplyExact(w, h), l);
        int[] cells = new int[total];
        int pos = 0;
        for (int i = 0; i < total; i++) {
            int value = 0, shift = 0;
            while (true) {
                if (pos >= data.length) throw new IOException("Block data ended early");
                byte b = data[pos++];
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
                if (shift > 35) throw new IOException("VarInt too long");
            }
            if (value >= byIndex.length) throw new IOException("Block references missing palette entry");
            cells[i] = value;
        }
        // Sponge order is (y * length + z) * width + x, same as Schematic.
        return new Schematic(name, w, h, l, Arrays.asList(byIndex), cells);
    }

    // ---------------- Litematica (.litematic) ----------------

    static Schematic loadLitematic(String name, Map<String, Object> root) throws IOException {
        Map<String, Object> regions = Nbt.compound(root, "Regions");
        if (regions == null || regions.isEmpty()) throw new IOException("No regions in litematic");

        // Combine all regions into one bounding box.
        List<RegionData> parsed = new ArrayList<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (Object o : regions.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> region = (Map<String, Object>) o;
            RegionData r = parseRegion(region);
            parsed.add(r);
            minX = Math.min(minX, r.originX);
            minY = Math.min(minY, r.originY);
            minZ = Math.min(minZ, r.originZ);
            maxX = Math.max(maxX, r.originX + r.sx - 1);
            maxY = Math.max(maxY, r.originY + r.sy - 1);
            maxZ = Math.max(maxZ, r.originZ + r.sz - 1);
        }

        int w = maxX - minX + 1, h = maxY - minY + 1, l = maxZ - minZ + 1;
        long totalL = (long) w * h * l;
        if (totalL > 200_000_000L) throw new IOException("Schematic too large");
        int total = (int) totalL;

        List<String> palette = new ArrayList<>();
        Map<String, Integer> paletteIndex = new HashMap<>();
        palette.add("minecraft:air");
        paletteIndex.put("minecraft:air", 0);
        int[] cells = new int[total]; // zero = air

        for (RegionData r : parsed) {
            int[] map = new int[r.palette.size()];
            for (int i = 0; i < map.length; i++) {
                String s = r.palette.get(i);
                Integer idx = paletteIndex.get(s);
                if (idx == null) {
                    idx = palette.size();
                    palette.add(s);
                    paletteIndex.put(s, idx);
                }
                map[i] = idx;
            }
            for (int y = 0; y < r.sy; y++)
                for (int z = 0; z < r.sz; z++)
                    for (int x = 0; x < r.sx; x++) {
                        int local = (y * r.sz + z) * r.sx + x;
                        int p = r.get(local);
                        if (p >= map.length) throw new IOException("Bad palette reference in region");
                        int gx = r.originX + x - minX, gy = r.originY + y - minY, gz = r.originZ + z - minZ;
                        cells[(gy * l + gz) * w + gx] = map[p];
                    }
        }
        return new Schematic(name, w, h, l, palette, cells);
    }

    private static RegionData parseRegion(Map<String, Object> region) throws IOException {
        Map<String, Object> size = Nbt.compound(region, "Size");
        Map<String, Object> pos = Nbt.compound(region, "Position");
        if (size == null || pos == null) throw new IOException("Region missing Size/Position");

        int rawX = Nbt.intVal(size, "x", 0), rawY = Nbt.intVal(size, "y", 0), rawZ = Nbt.intVal(size, "z", 0);
        RegionData r = new RegionData();
        r.sx = Math.abs(rawX);
        r.sy = Math.abs(rawY);
        r.sz = Math.abs(rawZ);
        if (r.sx == 0 || r.sy == 0 || r.sz == 0) throw new IOException("Empty region");

        // A negative size means the region extends in the negative direction from Position.
        int px = Nbt.intVal(pos, "x", 0), py = Nbt.intVal(pos, "y", 0), pz = Nbt.intVal(pos, "z", 0);
        r.originX = rawX < 0 ? px + rawX + 1 : px;
        r.originY = rawY < 0 ? py + rawY + 1 : py;
        r.originZ = rawZ < 0 ? pz + rawZ + 1 : pz;

        r.palette = new ArrayList<>();
        for (Object o : Nbt.list(region, "BlockStatePalette")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) o;
            String id = Nbt.string(entry, "Name", "minecraft:air");
            Map<String, Object> props = Nbt.compound(entry, "Properties");
            if (props == null || props.isEmpty()) {
                r.palette.add(id);
            } else {
                StringBuilder sb = new StringBuilder(id).append('[');
                boolean first = true;
                for (Map.Entry<String, Object> p : new TreeMap<>(props).entrySet()) {
                    if (!first) sb.append(',');
                    sb.append(p.getKey()).append('=').append(p.getValue());
                    first = false;
                }
                r.palette.add(sb.append(']').toString());
            }
        }
        if (r.palette.isEmpty()) throw new IOException("Region has empty palette");

        r.bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(r.palette.size() - 1));
        r.states = Nbt.longs(region, "BlockStates");
        if (r.states == null) throw new IOException("Region missing BlockStates");
        long needed = ((long) r.sx * r.sy * r.sz * r.bits + 63) / 64;
        if (r.states.length < needed) throw new IOException("BlockStates array too short");
        return r;
    }

    private static final class RegionData {
        int sx, sy, sz, originX, originY, originZ, bits;
        List<String> palette;
        long[] states;

        /** Litematica packs entries back to back across long boundaries. */
        int get(int index) {
            long bitIndex = (long) index * bits;
            int word = (int) (bitIndex >>> 6);
            int offset = (int) (bitIndex & 63);
            long mask = (1L << bits) - 1L;
            long value = states[word] >>> offset;
            int spill = offset + bits - 64;
            if (spill > 0) value |= states[word + 1] << (bits - spill);
            return (int) (value & mask);
        }
    }
}
