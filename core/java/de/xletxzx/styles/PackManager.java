package de.xletxzx.styles;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

final class PackManager {
    static final class Pack {
        String file, name, cat, folder = "";
        long size;
        boolean dir;
        /** Packs inside a group folder cannot be switched on by Minecraft itself. */
        boolean nested() { return file.contains("/"); }
        String id() { return "file/" + file; }
    }

    static final List<Pack> packs = new ArrayList<>();
    static final String[] CATS = {"Mace", "Spear", "Crystal", "PvP", "Overlay", "Other"};

    static Path dir() { return FabricLoader.getInstance().getGameDir().resolve("resourcepacks"); }

    /** Makes sure the resourcepacks folder exists and finds all packs in it. */
    static void install() {
        try {
            Files.createDirectories(dir());
        } catch (Exception e) {
            System.err.println("[XLetxzX] Pack folder failed: " + e);
        }
        scan();
    }

    /** Finds all packs, also inside folders that only group other packs (up to 3 levels deep). */
    static void scan() {
        packs.clear();
        scanDir(dir(), "", 0);
        packs.sort(Comparator.comparing(p -> p.name.toLowerCase(Locale.ROOT)));
    }

    private static void scanDir(Path d, String prefix, int depth) {
        try (Stream<Path> s = Files.list(d)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String fn = p.getFileName().toString();
                if (depth == 0 && (fn.equals(Tweaks.PACK) || fn.equals(Tweaks.LEGACY[0]) || fn.equals(Tweaks.LEGACY[1]))) continue; // internal packs
                boolean isDir = Files.isDirectory(p);
                if (isDir && !Files.exists(p.resolve("pack.mcmeta"))) {
                    if (depth < 3) scanDir(p, prefix + fn + "/", depth + 1); // group folder
                    continue;
                }
                if (!isDir && !fn.toLowerCase(Locale.ROOT).endsWith(".zip")) continue;
                Pack pk = new Pack();
                pk.file = prefix + fn;
                pk.dir = isDir;
                pk.folder = prefix.isEmpty() ? "" : PackFiles.clean(prefix.substring(0, prefix.length() - 1));
                String n = isDir ? fn : fn.substring(0, fn.length() - 4);
                pk.name = PackFiles.clean(n.replace('_', ' '));
                if (pk.name.isEmpty()) pk.name = n;
                try { pk.size = isDir ? 0 : Files.size(p); } catch (Exception ignored) {}
                pk.cat = category(fn.toLowerCase(Locale.ROOT));
                packs.add(pk);
            }
        } catch (Exception ignored) {}
    }

    static String category(String n) {
        if (n.contains("mace")) return "Mace";
        if (n.contains("spear")) return "Spear";
        if (n.contains("crystal")) return "Crystal";
        if (n.contains("overlay")) return "Overlay";
        if (n.contains("pvp") || n.contains("pack")) return "PvP";
        return "Other";
    }

    static Set<String> selected() {
        PackRepository repo = Minecraft.getInstance().getResourcePackRepository();
        return new HashSet<>(repo.getSelectedIds());
    }

    static void rescan() {
        scan();
        Minecraft.getInstance().getResourcePackRepository().reload();
    }

    static void toggle(Pack p) {
        if (p.nested()) return;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            PackRepository repo = mc.getResourcePackRepository();
            repo.reload();
            List<String> sel = new ArrayList<>(repo.getSelectedIds());
            if (!sel.remove(p.id())) sel.add(p.id());
            repo.setSelected(sel);
            mc.options.updateResourcePacks(repo);
        });
    }

    /**
     * Turns one pack on (placed on top) or off. If the selection did not change but the files did,
     * forceReload reloads the resources anyway.
     */
    static void setEnabled(String id, boolean enable, boolean forceReload) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            PackRepository repo = mc.getResourcePackRepository();
            repo.reload();
            List<String> before = new ArrayList<>(repo.getSelectedIds());
            List<String> sel = new ArrayList<>(before);
            sel.remove(id);
            if (enable && repo.getAvailableIds().contains(id)) sel.add(id);
            boolean changed = !sel.equals(before);
            if (changed) {
                repo.setSelected(sel);
                mc.options.updateResourcePacks(repo);
            } else if (forceReload) {
                mc.reloadResourcePacks();
            }
        });
    }

    static void disableAll() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            PackRepository repo = mc.getResourcePackRepository();
            List<String> sel = new ArrayList<>(repo.getSelectedIds());
            sel.removeIf(id -> id.startsWith("file/") && !id.equals(Tweaks.id()));
            repo.setSelected(sel);
            mc.options.updateResourcePacks(repo);
        });
    }

    /** Moves a pack out of its group folder into the main resourcepacks folder (so Minecraft can use it). */
    static boolean moveToTop(Pack p) {
        try {
            Path src = dir().resolve(p.file);
            String fn = src.getFileName().toString();
            Path dst = dir().resolve(fn);
            if (Files.exists(dst)) return false;
            Files.move(src, dst);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static String sizeText(Pack p) {
        if (p.dir) return "folder";
        if (p.size > 1_000_000) return String.format("%.1f MB", p.size / 1_000_000.0);
        return String.format("%d KB", p.size / 1000);
    }
}
