package de.xletxzx.styles;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Finds every resource pack the player has: in the resourcepacks folder (any folder depth), packs whose files sit
 * in a sub folder of the zip, zips that only contain more zips, and packs in other Minecraft folders / launcher
 * profiles on this PC. Only the packs in the main resourcepacks folder can be switched on by Minecraft itself;
 * all packs can be used by the TxT texture mixer.
 */
final class PackManager {
    static final class Pack {
        String file, name, cat, folder = "";
        long size;
        boolean dir;
        /** Real file or folder on disk (for packs inside zips: the extracted copy in the cache). */
        Path real;
        /** Folder inside the pack where "assets/" starts, e.g. "MyPack/" (empty = pack root). */
        String prefix = "";
        /** From another folder / profile, or extracted from a zip: Minecraft cannot switch it on directly. */
        boolean external, virtual;

        /** Minecraft cannot switch this pack on where it is. */
        boolean nested() { return file.contains("/") || external || virtual || !prefix.isEmpty(); }
        /** "Use" can copy / move it into the main folder so Minecraft can switch it on. */
        boolean movable() { return prefix.isEmpty() && (file.contains("/") || external || virtual); }
        String id() { return "file/" + file; }
    }

    static volatile List<Pack> packs = new ArrayList<>();
    private static volatile Map<String, Pack> byId = new HashMap<>();
    static final String[] CATS = {"Mace", "Spear", "Crystal", "PvP", "Overlay", "Other"};
    private static final int MAX_DEPTH = 12;

    static Path dir() { return FabricLoader.getInstance().getGameDir().resolve("resourcepacks"); }
    static Path cache() { return FabricLoader.getInstance().getGameDir().resolve("xletxzx_cache").resolve("packs"); }

    static Pack get(String id) { return byId.get(id); }

    /** Makes sure the resourcepacks folder exists (the packs are read in the background, see PacksPanel). */
    static void install() {
        try {
            Files.createDirectories(dir());
        } catch (Exception e) {
            System.err.println("[XLetxzX] Pack folder failed: " + e);
        }
    }

    /** Opens a zip; names in old encodings (e.g. with the § sign) are read with the DOS charset. */
    static ZipFile openZip(Path p) throws Exception {
        try {
            return new ZipFile(p.toFile(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new ZipFile(p.toFile(), Charset.forName("IBM437"));
        }
    }

    /** Finds all packs (slow: call it off the render thread). */
    static void scan() {
        List<Pack> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        scanDir(out, seen, dir(), "", 0, null);
        if (Config.get().packsEverywhere) {
            for (Path[] root : extraRoots()) scanDir(out, seen, root[0], "", 0, root[1].toString());
        }
        out.sort(Comparator.comparing(p -> p.name.toLowerCase(Locale.ROOT)));
        Map<String, Pack> m = new HashMap<>();
        for (Pack p : out) m.put(p.file, p);
        byId = m;
        packs = out;
    }

    /** Other resourcepacks folders: the normal .minecraft and the other profiles / instances of this launcher. */
    private static List<Path[]> extraRoots() {
        List<Path[]> out = new ArrayList<>();
        Path main = dir().toAbsolutePath().normalize();
        try {
            String home = System.getProperty("user.home");
            String appdata = System.getenv("APPDATA");
            List<Path> mc = new ArrayList<>();
            if (appdata != null) mc.add(Paths.get(appdata, ".minecraft"));
            if (home != null) {
                mc.add(Paths.get(home, ".minecraft"));
                mc.add(Paths.get(home, "Library", "Application Support", "minecraft"));
            }
            for (Path p : mc) add(out, main, p.resolve("resourcepacks"), Paths.get(".minecraft"));
            Path game = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
            Path parent = game.getParent();
            if (parent != null) {
                try (Stream<Path> s = Files.list(parent)) {
                    s.limit(60).forEach(p -> {
                        if (!Files.isDirectory(p) || p.equals(game)) return;
                        add(out, main, p.resolve("resourcepacks"), p.getFileName());
                        add(out, main, p.resolve(".minecraft").resolve("resourcepacks"), p.getFileName());   // Prism / MultiMC
                    });
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static void add(List<Path[]> out, Path main, Path rp, Path label) {
        try {
            Path n = rp.toAbsolutePath().normalize();
            if (n.equals(main) || !Files.isDirectory(n)) return;
            for (Path[] o : out) if (o[0].equals(n)) return;
            out.add(new Path[]{n, label});
        } catch (Exception ignored) {}
    }

    /** from = null for the main folder, otherwise the name of the other folder / profile. */
    private static void scanDir(List<Pack> out, Set<String> seen, Path d, String rel, int depth, String from) {
        if (depth > MAX_DEPTH) return;
        try (Stream<Path> s = Files.list(d)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String fn = p.getFileName().toString();
                if (depth == 0 && from == null && (fn.equals(Tweaks.PACK) || fn.equals(Tweaks.LEGACY[0]) || fn.equals(Tweaks.LEGACY[1]))) continue; // internal packs
                if (depth == 0 && from != null && (fn.equals(Tweaks.PACK) || fn.startsWith("XLetxzX_"))) continue;
                String r = rel + fn;
                if (Files.isDirectory(p)) {
                    if (Files.exists(p.resolve("pack.mcmeta")) || Files.isDirectory(p.resolve("assets"))) {
                        Pack pk = make(p, r, fn, true, rel, from);
                        pk.real = p;
                        addPack(out, seen, pk, fn);
                    } else {
                        scanDir(out, seen, p, r + "/", depth + 1, from);   // folder that groups packs
                    }
                    continue;
                }
                if (!fn.toLowerCase(Locale.ROOT).endsWith(".zip")) continue;
                scanZip(out, seen, p, r, fn, rel, from);
            }
        } catch (Exception ignored) {}
    }

    /** One zip: a normal pack, a pack inside a sub folder of the zip, or a zip full of more zips. */
    private static void scanZip(List<Pack> out, Set<String> seen, Path zip, String r, String fn, String rel, String from) {
        String prefix = null;
        List<String> inner = new ArrayList<>();
        try (ZipFile z = openZip(zip)) {
            int best = Integer.MAX_VALUE;
            Enumeration<? extends ZipEntry> en = z.entries();
            while (en.hasMoreElements()) {
                String n = en.nextElement().getName();
                String pre = null;
                if (n.equals("pack.mcmeta") || n.startsWith("assets/")) pre = "";
                else if (n.endsWith("/pack.mcmeta")) pre = n.substring(0, n.length() - "pack.mcmeta".length());
                else if (n.contains("/assets/")) pre = n.substring(0, n.indexOf("/assets/") + 1);
                else if (n.toLowerCase(Locale.ROOT).endsWith(".zip")) inner.add(n);
                if (pre != null) {
                    int depthOf = pre.isEmpty() ? 0 : pre.split("/").length;
                    if (depthOf < best) { best = depthOf; prefix = pre; }
                }
            }
        } catch (Exception e) {
            return;   // broken zip
        }
        if (prefix != null) {
            Pack pk = make(zip, r, fn, false, rel, from);
            pk.real = zip;
            pk.prefix = prefix;
            addPack(out, seen, pk, fn);
            return;
        }
        // a zip that only contains more packs: extract them once into the cache and read those
        for (String n : inner) {
            try {
                Path cached = extractInner(zip, n);
                if (cached == null) continue;
                String innerName = n.substring(n.lastIndexOf('/') + 1);
                String id = r + "!/" + n;
                String folderRel = rel + fn.substring(0, fn.length() - 4) + "/";
                List<Pack> tmp = new ArrayList<>();
                scanZip(tmp, seen, cached, id, innerName, folderRel, from);
                for (Pack pk : tmp) { pk.virtual = true; out.add(pk); }
            } catch (Exception ignored) {}
        }
    }

    private static Path extractInner(Path zip, String entry) {
        try (ZipFile z = openZip(zip)) {
            ZipEntry e = z.getEntry(entry);
            if (e == null) return null;
            String key = Integer.toHexString((zip.toAbsolutePath() + "!" + entry).hashCode()) + "_"
                + entry.substring(entry.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_");
            Path dst = cache().resolve(key);
            if (Files.exists(dst) && e.getSize() >= 0 && Files.size(dst) == e.getSize()) return dst;
            Files.createDirectories(dst.getParent());
            try (InputStream in = z.getInputStream(e)) { Files.copy(in, dst, StandardCopyOption.REPLACE_EXISTING); }
            return dst;
        } catch (Exception ex) {
            return null;
        }
    }

    private static Pack make(Path p, String r, String fn, boolean isDir, String rel, String from) {
        Pack pk = new Pack();
        pk.file = from == null ? r : "ext:" + p.toAbsolutePath().normalize().toString().replace('\\', '/');
        pk.dir = isDir;
        pk.external = from != null;
        String folder = rel.isEmpty() ? "" : PackFiles.clean(rel.substring(0, rel.length() - 1));
        pk.folder = from == null ? folder : ("from " + from + (folder.isEmpty() ? "" : " / " + folder));
        String n = isDir ? fn : fn.substring(0, fn.length() - 4);
        pk.name = PackFiles.clean(n.replace('_', ' '));
        if (pk.name.isEmpty()) pk.name = n;
        try { pk.size = isDir ? 0 : Files.size(p); } catch (Exception ignored) {}
        pk.cat = category(fn.toLowerCase(Locale.ROOT));
        return pk;
    }

    /** Skips copies of the same pack (same file name and size) found in several folders. */
    private static void addPack(List<Pack> out, Set<String> seen, Pack pk, String fn) {
        String key = fn.toLowerCase(Locale.ROOT) + "|" + pk.size;
        if (!seen.add(key)) return;
        out.add(pk);
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

    /**
     * Makes a pack usable by Minecraft: packs from a group folder are moved into the main resourcepacks
     * folder, packs from other folders or from inside a zip are copied there.
     */
    static boolean moveToTop(Pack p) {
        try {
            Path src = p.real != null ? p.real : dir().resolve(p.file);
            String fn = p.virtual ? p.name.replaceAll("[\\\\/:*?\"<>|]", "_") + ".zip" : src.getFileName().toString();
            Path dst = dir().resolve(fn);
            if (Files.exists(dst)) return false;
            if (p.external || p.virtual) {
                if (Files.isDirectory(src)) copyTree(src, dst);
                else Files.copy(src, dst);
            } else {
                Files.move(src, dst);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void copyTree(Path src, Path dst) throws Exception {
        try (Stream<Path> s = Files.walk(src)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                Path t = dst.resolve(src.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(t);
                else Files.copy(p, t, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    static String sizeText(Pack p) {
        if (p.dir) return "folder";
        if (p.size > 1_000_000) return String.format("%.1f MB", p.size / 1_000_000.0);
        return String.format("%d KB", p.size / 1000);
    }
}
