package net.novaclient.schematic;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Minimal NBT reader with no Minecraft dependency. Compounds become Map, lists become List. */
public final class Nbt {
    private Nbt() {}

    public static Map<String, Object> readGzipped(InputStream in) throws IOException {
        try (DataInputStream d = new DataInputStream(new BufferedInputStream(new GZIPInputStream(in)))) {
            byte type = d.readByte();
            if (type != 10) throw new IOException("Root tag is not a compound");
            d.readUTF(); // root name
            return readCompound(d);
        }
    }

    private static Map<String, Object> readCompound(DataInputStream d) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        while (true) {
            byte type = d.readByte();
            if (type == 0) return map;
            String name = d.readUTF();
            map.put(name, readPayload(d, type));
        }
    }

    private static Object readPayload(DataInputStream d, byte type) throws IOException {
        switch (type) {
            case 1: return d.readByte();
            case 2: return d.readShort();
            case 3: return d.readInt();
            case 4: return d.readLong();
            case 5: return d.readFloat();
            case 6: return d.readDouble();
            case 7: {
                byte[] b = new byte[checkLen(d.readInt())];
                d.readFully(b);
                return b;
            }
            case 8: return d.readUTF();
            case 9: {
                byte inner = d.readByte();
                int len = checkLen(d.readInt());
                List<Object> list = new ArrayList<>(Math.min(len, 4096));
                for (int i = 0; i < len; i++) list.add(readPayload(d, inner));
                return list;
            }
            case 10: return readCompound(d);
            case 11: {
                int[] a = new int[checkLen(d.readInt())];
                for (int i = 0; i < a.length; i++) a[i] = d.readInt();
                return a;
            }
            case 12: {
                long[] a = new long[checkLen(d.readInt())];
                for (int i = 0; i < a.length; i++) a[i] = d.readLong();
                return a;
            }
            default: throw new IOException("Unknown NBT tag type " + type);
        }
    }

    private static int checkLen(int len) throws IOException {
        if (len < 0 || len > 256_000_000) throw new IOException("Bad NBT array length " + len);
        return len;
    }

    // ---- typed accessors ----

    @SuppressWarnings("unchecked")
    public static Map<String, Object> compound(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof List ? (List<Object>) o : List.of();
    }

    public static int intVal(Map<String, Object> m, String key, int def) {
        Object o = m.get(key);
        return o instanceof Number n ? n.intValue() : def;
    }

    public static String string(Map<String, Object> m, String key, String def) {
        Object o = m.get(key);
        return o instanceof String s ? s : def;
    }

    public static byte[] bytes(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof byte[] b ? b : null;
    }

    public static long[] longs(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof long[] l ? l : null;
    }
}
