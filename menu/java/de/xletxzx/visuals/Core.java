package de.xletxzx.visuals;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Calls the feature part of the mod (de.xletxzx.styles.Bridge) by method name. */
final class Core {
    private static final Map<String, Method> CACHE = new HashMap<>();
    private static Class<?> bridge;

    private Core() {}

    static Object call(String name, Object... args) {
        try {
            Method m = CACHE.get(name);
            if (m == null) {
                if (bridge == null) bridge = Class.forName("de.xletxzx.styles.Bridge");
                for (Method x : bridge.getMethods()) if (x.getName().equals(name)) { m = x; break; }
                if (m == null) return null;
                CACHE.put(name, m);
            }
            return m.invoke(null, args);
        } catch (Throwable t) {
            Throwable c = t.getCause() != null ? t.getCause() : t;
            StackTraceElement[] st = c.getStackTrace();
            System.err.println("[XLetxzX] " + name + " failed: " + c + (st.length > 0 ? " at " + st[0] : "") + (st.length > 1 ? " / " + st[1] : ""));
            return null;
        }
    }

    private static final Map<String, String> TR = new HashMap<>();

    /** Translated text for the current game language (cached). */
    static String tr(String s) {
        if (s == null || s.length() < 2) return s;
        String r = TR.get(s);
        if (r == null) {
            Object o = call("tr", s);
            r = o instanceof String t ? t : s;
            TR.put(s, r);
        }
        return r;
    }

    static void clearTr() { TR.clear(); }

    static boolean bool(String name) { return Boolean.TRUE.equals(call(name)); }

    static int num(String name) {
        Object o = call(name);
        return o instanceof Integer i ? i : 0;
    }
}
