package de.xletxzx.styles;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Remembers which player render states belong to YOU, so only your own armor can be hidden. */
public final class SelfArmor {
    private static final Set<Object> MINE = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private SelfArmor() {}

    public static void mark(Object state, boolean mine) {
        if (mine) MINE.add(state); else MINE.remove(state);
    }

    public static boolean hide(Object state) {
        return Config.get().fx.hideMyArmor && MINE.contains(state);
    }
}
