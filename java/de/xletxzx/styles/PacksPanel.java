package de.xletxzx.styles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * PVP page: pick the texture of every PvP item from any of your resource packs
 * (copied into one generated pack, XLetxzX_Mix, on top), plus the texture pack manager.
 */
final class PacksPanel {
    static final String MIX = "XLetxzX_Mix";

    static final class Tile {
        final String id, name; final Item icon; final String[] paths; ItemStack stack; boolean cube;
        Tile(String id, String name, Item icon, String[] paths) { this.id = id; this.name = name; this.icon = icon; this.paths = paths; }
        ItemStack stack() { if (stack == null) stack = new ItemStack(icon); return stack; }
    }

    static final class Cat {
        final String name, desc; final Item icon; final String[] tiles; ItemStack stack;
        Cat(String name, String desc, Item icon, String... tiles) { this.name = name; this.desc = desc; this.icon = icon; this.tiles = tiles; }
        ItemStack stack() { if (stack == null) stack = new ItemStack(icon); return stack; }
    }

    static final Map<String, Tile> TILES = new LinkedHashMap<>();

    private static void t(String id, String name, Item icon, String... paths) {
        String[] p = new String[paths.length];
        for (int i = 0; i < paths.length; i++) p[i] = "textures/" + paths[i] + ".png";
        TILES.put(id, new Tile(id, name, icon, p));
    }

    private static void cube(String id) { TILES.get(id).cube = true; }

    static {
        t("crystal", "Crystal", Items.END_CRYSTAL, "item/end_crystal", "entity/end_crystal/end_crystal");
        t("obsidian", "Obsidian", Items.OBSIDIAN, "block/obsidian");
        cube("obsidian");
        t("anchor", "Anchor", Items.RESPAWN_ANCHOR, "block/respawn_anchor_top", "block/respawn_anchor_side4",
            "block/respawn_anchor_top_off", "block/respawn_anchor_side0", "block/respawn_anchor_side1",
            "block/respawn_anchor_side2", "block/respawn_anchor_side3", "block/respawn_anchor_bottom");
        cube("anchor");
        t("glowstone", "Glowstone", Items.GLOWSTONE, "block/glowstone");
        cube("glowstone");
        t("totem", "Totem", Items.TOTEM_OF_UNDYING, "item/totem_of_undying");
        t("netherite_armor", "Netherite Armor", Items.NETHERITE_CHESTPLATE, "item/netherite_chestplate", "item/netherite_helmet",
            "item/netherite_leggings", "item/netherite_boots", "entity/equipment/humanoid/netherite",
            "entity/equipment/humanoid_leggings/netherite");
        t("diamond_armor", "Diamond Armor", Items.DIAMOND_CHESTPLATE, "item/diamond_chestplate", "item/diamond_helmet",
            "item/diamond_leggings", "item/diamond_boots", "entity/equipment/humanoid/diamond",
            "entity/equipment/humanoid_leggings/diamond");
        t("gapple", "Gapple", Items.GOLDEN_APPLE, "item/golden_apple");
        t("pearl", "Pearl", Items.ENDER_PEARL, "item/ender_pearl");
        t("xp", "XP Bottle", Items.EXPERIENCE_BOTTLE, "item/experience_bottle");
        t("glint", "Glint", Items.ENCHANTED_BOOK, "misc/enchanted_glint_item", "misc/enchanted_glint_armor");
        t("cobweb", "Cobweb", Items.COBWEB, "block/cobweb");
        t("ender_chest", "Ender Chest", Items.ENDER_CHEST, "entity/chest/ender");
        t("shulker", "Shulker", Items.SHULKER_BOX, "block/shulker_box", "entity/shulker/shulker");
        t("netherite_sword", "Netherite Sword", Items.NETHERITE_SWORD, "item/netherite_sword");
        t("diamond_sword", "Diamond Sword", Items.DIAMOND_SWORD, "item/diamond_sword");
        t("netherite_axe", "Netherite Axe", Items.NETHERITE_AXE, "item/netherite_axe");
        t("diamond_axe", "Diamond Axe", Items.DIAMOND_AXE, "item/diamond_axe");
        t("mace", "Mace", Items.MACE, "item/mace");
        t("netherite_spear", "Netherite Spear", Items.NETHERITE_SPEAR, "item/netherite_spear", "item/netherite_spear_in_hand");
        t("diamond_spear", "Diamond Spear", Items.DIAMOND_SPEAR, "item/diamond_spear", "item/diamond_spear_in_hand");
        t("iron_spear", "Iron Spear", Items.IRON_SPEAR, "item/iron_spear", "item/iron_spear_in_hand");
        t("wind_charge", "Wind Charge", Items.WIND_CHARGE, "item/wind_charge", "entity/projectiles/wind_charge");
        t("potion", "Potions", Items.SPLASH_POTION, "item/splash_potion", "item/potion", "item/potion_overlay", "item/lingering_potion");
        t("steak", "Steak", Items.COOKED_BEEF, "item/cooked_beef");
        t("golden_carrot", "Golden Carrot", Items.GOLDEN_CARROT, "item/golden_carrot");
        t("elytra", "Elytra", Items.ELYTRA, "item/elytra", "entity/equipment/wings/elytra");
        t("firework", "Firework", Items.FIREWORK_ROCKET, "item/firework_rocket");
        t("shield", "Shield", Items.SHIELD, "entity/shield_base", "entity/shield_base_nopattern");
        t("bow", "Bow", Items.BOW, "item/bow", "item/bow_pulling_0", "item/bow_pulling_1", "item/bow_pulling_2");
        t("crossbow", "Crossbow", Items.CROSSBOW, "item/crossbow_standby", "item/crossbow_pulling_0",
            "item/crossbow_pulling_1", "item/crossbow_pulling_2", "item/crossbow_arrow", "item/crossbow_firework");
        t("arrow", "Arrow", Items.ARROW, "item/arrow", "entity/projectiles/arrow");
        t("tnt_cart", "TNT Cart", Items.TNT_MINECART, "item/tnt_minecart", "block/tnt_side", "block/tnt_top", "block/tnt_bottom");
        t("tnt", "TNT", Items.TNT, "block/tnt_top", "block/tnt_side", "block/tnt_bottom");
        cube("tnt");
        t("rail", "Rail", Items.RAIL, "block/rail", "block/rail_corner");
        t("activator_rail", "Activator Rail", Items.ACTIVATOR_RAIL, "block/activator_rail", "block/activator_rail_on");
        t("flint", "Flint & Steel", Items.FLINT_AND_STEEL, "item/flint_and_steel");
        t("fire", "Fire", Items.FIRE_CHARGE, "block/fire_0", "block/fire_1");
        t("hearts", "Hearts", Items.POPPY, "gui/sprites/hud/heart/full", "gui/sprites/hud/heart/half",
            "gui/sprites/hud/heart/container", "gui/sprites/hud/heart/hardcore_full");
        t("hunger", "Hunger", Items.COOKED_CHICKEN, "gui/sprites/hud/food_full", "gui/sprites/hud/food_half", "gui/sprites/hud/food_empty");
        t("armor_bar", "Armor Bar", Items.IRON_CHESTPLATE, "gui/sprites/hud/armor_full", "gui/sprites/hud/armor_half", "gui/sprites/hud/armor_empty");
        t("hotbar", "Hotbar", Items.ITEM_FRAME, "gui/sprites/hud/hotbar", "gui/sprites/hud/hotbar_selection",
            "gui/sprites/hud/hotbar_offhand_left", "gui/sprites/hud/hotbar_offhand_right");
        t("inventory", "Inventory", Items.CHEST, "gui/container/inventory", "gui/container/generic_54", "gui/container/shulker_box");
        t("crosshair", "Crosshair", Items.TARGET, "gui/sprites/hud/crosshair");
        t("xp_bar", "XP Bar", Items.LIME_DYE, "gui/sprites/hud/experience_bar_background", "gui/sprites/hud/experience_bar_progress");
        t("air", "Air Bubbles", Items.GLASS_BOTTLE, "gui/sprites/hud/air", "gui/sprites/hud/air_bursting");
        t("jump_bar", "Jump Bar", Items.SADDLE, "gui/sprites/hud/jump_bar_background", "gui/sprites/hud/jump_bar_progress");
        t("effects", "Effect Icons", Items.BEACON, "gui/sprites/hud/effect_background", "gui/sprites/hud/effect_background_ambient");
        t("boss_bar", "Boss Bar", Items.DRAGON_HEAD, "gui/sprites/boss_bar/pink_background", "gui/sprites/boss_bar/pink_progress");
        t("buttons", "Buttons", Items.STONE_BUTTON, "gui/sprites/widget/button", "gui/sprites/widget/button_highlighted", "gui/sprites/widget/button_disabled");
        t("crafting", "Crafting Table", Items.CRAFTING_TABLE, "gui/container/crafting_table");
        t("furnace", "Furnace", Items.FURNACE, "gui/container/furnace");
        t("anvil", "Anvil", Items.ANVIL, "gui/container/anvil");
        t("enchanting", "Enchanting Table", Items.ENCHANTING_TABLE, "gui/container/enchanting_table");
        t("grass", "Grass Block", Items.GRASS_BLOCK, "block/grass_block_top", "block/grass_block_side");
        cube("grass");
        t("dirt", "Dirt", Items.DIRT, "block/dirt");
        cube("dirt");
        t("stone", "Stone", Items.STONE, "block/stone");
        cube("stone");
        t("cobblestone", "Cobblestone", Items.COBBLESTONE, "block/cobblestone");
        cube("cobblestone");
        t("bedrock", "Bedrock", Items.BEDROCK, "block/bedrock");
        cube("bedrock");
        t("blackstone", "Blackstone", Items.BLACKSTONE, "block/blackstone_top", "block/blackstone");
        cube("blackstone");
        t("deepslate", "Deepslate", Items.DEEPSLATE, "block/deepslate_top", "block/deepslate");
        cube("deepslate");
        t("netherrack", "Netherrack", Items.NETHERRACK, "block/netherrack");
        cube("netherrack");
        t("end_stone", "End Stone", Items.END_STONE, "block/end_stone");
        cube("end_stone");
        t("sand", "Sand", Items.SAND, "block/sand");
        cube("sand");
        t("gravel", "Gravel", Items.GRAVEL, "block/gravel");
        cube("gravel");
        t("oak_planks", "Oak Planks", Items.OAK_PLANKS, "block/oak_planks");
        cube("oak_planks");
        t("netherite_block", "Netherite Block", Items.NETHERITE_BLOCK, "block/netherite_block");
        cube("netherite_block");
        t("diamond_block", "Diamond Block", Items.DIAMOND_BLOCK, "block/diamond_block");
        cube("diamond_block");
        t("gold_block", "Gold Block", Items.GOLD_BLOCK, "block/gold_block");
        cube("gold_block");
        t("iron_block", "Iron Block", Items.IRON_BLOCK, "block/iron_block");
        cube("iron_block");
        t("ice", "Ice", Items.PACKED_ICE, "block/packed_ice");
        cube("ice");
    }

    static final Cat[] PVP = {
        new Cat("Crystal PvP", "End crystals, obsidian, anchors and glowstone", Items.END_CRYSTAL,
            "crystal", "obsidian", "anchor", "glowstone", "cobweb"),
        new Cat("Totems & items", "Totems, gapples, pearls, potions and food", Items.TOTEM_OF_UNDYING,
            "totem", "gapple", "pearl", "xp", "potion", "wind_charge", "firework", "steak", "golden_carrot"),
        new Cat("Spear PvP", "Spears, mace, axes, wind charges and shields", Items.NETHERITE_SPEAR,
            "netherite_spear", "diamond_spear", "iron_spear", "mace", "netherite_axe", "diamond_axe", "wind_charge", "shield", "elytra", "firework"),
        new Cat("Swords & axes", "Swords, axes and the mace", Items.NETHERITE_SWORD,
            "netherite_sword", "diamond_sword", "netherite_axe", "diamond_axe", "mace"),
        new Cat("Armor", "Netherite, diamond, elytra, shield and glint", Items.NETHERITE_CHESTPLATE,
            "netherite_armor", "diamond_armor", "elytra", "shield", "glint"),
        new Cat("Bows", "Bow, crossbow and arrows", Items.BOW,
            "bow", "crossbow", "arrow"),
        new Cat("Cart PvP", "TNT carts, rails and fire", Items.TNT_MINECART,
            "tnt_cart", "tnt", "rail", "activator_rail", "flint", "fire"),
    };
    static final Cat[] ALL = {
        new Cat("Chests & shulkers", "Ender chest and shulker boxes", Items.ENDER_CHEST,
            "ender_chest", "shulker"),
        new Cat("HUD & screen", "Hearts, hunger, hotbar, inventory and crosshair", Items.FILLED_MAP,
            "hearts", "hunger", "armor_bar", "hotbar", "inventory", "crosshair", "xp_bar", "buttons", "air", "jump_bar", "effects", "boss_bar", "crafting", "furnace", "anvil", "enchanting"),
        new Cat("Blocks", "Grass, bedrock, stone, blackstone and more", Items.GRASS_BLOCK,
            "grass", "dirt", "stone", "cobblestone", "bedrock", "blackstone", "deepslate", "netherrack", "end_stone", "sand", "gravel", "oak_planks", "netherite_block", "diamond_block", "gold_block", "iron_block", "ice"),
    };
    private static final String[] FILTERS = {"All", "Yours", "★"};
    private static final String[] PACK_FILTERS = {"All", "Active", "Off"};

    // ---------------------------------------------------------------- pack index
    /** pack file -> (tile id -> texture paths the pack has for it, in tile order) */
    private static final Map<String, Map<String, List<String>>> index = new ConcurrentHashMap<>();
    private static final Map<String, String> names = new ConcurrentHashMap<>();
    private static final Map<String, String> descs = new ConcurrentHashMap<>();
    private static volatile int scanDone, scanTotal;
    private static volatile boolean scanning;
    private static boolean scannedOnce;
    private static Map<String, String> applied;
    static boolean dirty;

    private final Minecraft mc = Minecraft.getInstance();
    private final Font font = mc.font;
    private Cat cat = PVP[0];
    private int filter = 0, packFilter = 0;
    private String picking;
    private int scroll, maxScroll, pickScroll, pickMax, sideScroll, sideMax, listScroll, listMax;
    private String search = "";
    private String toast = "";
    private long toastUntil;
    private Runnable after;
    private int sideX, sideW;
    private long lastSelRead;
    private Set<String> selected = new HashSet<>();

    void toast(String s) { toast = s; toastUntil = System.currentTimeMillis() + 3500; }

    private static Map<String, String> applied() {
        if (applied == null) applied = new HashMap<>(Config.get().mix);
        return applied;
    }

    static void rescanPacks() {
        if (scanning) return;
        scanning = true;
        scannedOnce = true;
        PackManager.scan();
        Gfx.forget();
        List<PackManager.Pack> list = new ArrayList<>(PackManager.packs);
        scanTotal = list.size();
        scanDone = 0;
        Thread th = new Thread(() -> {
            Map<String, Map<String, List<String>>> out = new HashMap<>();
            for (PackManager.Pack p : list) {
                Map<String, List<String>> found = new HashMap<>();
                try {
                    Path f = PackManager.dir().resolve(p.file);
                    Set<String> entries = new HashSet<>();
                    if (p.dir) {
                        for (Tile t : TILES.values())
                            for (String tp : t.paths)
                                if (Files.exists(f.resolve("assets/minecraft/" + tp))) entries.add("assets/minecraft/" + tp);
                    } else {
                        try (ZipFile z = new ZipFile(f.toFile())) {
                            Enumeration<? extends ZipEntry> en = z.entries();
                            while (en.hasMoreElements()) {
                                String n = en.nextElement().getName();
                                if (n.startsWith("assets/minecraft/textures/")) entries.add(n);
                            }
                        }
                    }
                    for (Tile t : TILES.values()) {
                        List<String> have = new ArrayList<>();
                        for (String tp : t.paths) if (entries.contains("assets/minecraft/" + tp)) have.add("assets/minecraft/" + tp);
                        if (!have.isEmpty()) found.put(t.id, have);
                    }
                } catch (Exception ignored) {}
                names.put(p.file, p.name);
                descs.put(p.file, PackFiles.description(p.file));
                if (!found.isEmpty()) out.put(p.file, found);
                scanDone++;
            }
            index.clear();
            index.putAll(out);
            scanning = false;
        }, "XLetxzX-PackScan");
        th.setDaemon(true);
        th.start();
    }

    private static int count(String tile) {
        int n = 0;
        for (Map<String, List<String>> m : index.values()) if (m.containsKey(tile)) n++;
        return n;
    }

    private static List<String> offering(String tile) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<String>>> e : index.entrySet()) if (e.getValue().containsKey(tile)) out.add(e.getKey());
        out.sort(Comparator.comparing(f -> packName(f).toLowerCase(Locale.ROOT)));
        return out;
    }

    private static String packName(String file) { return names.getOrDefault(file, file); }

    // ---------------------------------------------------------------- drawing helpers
    private void drawItem(ItemStack st, int cx, int cy, float scale) {
        scale = Math.max(1, Math.round(scale));   // whole-number scale keeps every pixel the same size
        Ui.g.pose().pushMatrix();
        Ui.g.pose().translate((float) cx, (float) cy);
        Ui.g.pose().scale(scale, scale);
        Ui.g.renderItem(st, -8, -8);
        Ui.g.pose().popMatrix();
    }

    /** Draws the texture a pack has for a tile (3D block, animated). Falls back to the item. */
    private void drawPreview(Tile t, String pack, int cx, int cy, int size) {
        size = Math.max(16, size / 16 * 16);
        if (pack != null) {
            Map<String, List<String>> m = index.get(pack);
            List<String> have = m == null ? null : m.get(t.id);
            if (have != null && !have.isEmpty()) {
                boolean ok;
                if (t.cube) {
                    String top = have.get(0), side = have.size() > 1 ? have.get(1) : top;
                    ok = Gfx.cube(Ui.g, pack, top, side, cx, cy, size);
                } else {
                    // only flat item / block textures; entity textures (shield, elytra ...) are unwrapped model skins
                    String flat = null;
                    for (String hp : have) if (hp.contains("textures/item/") || hp.contains("textures/block/") || hp.contains("textures/gui/")) { flat = hp; break; }
                    if (flat != null) {
                        int[] d = flat.contains("textures/gui/") ? pngSize(pack, flat) : null;
                        if (d != null && d[0] > 0 && d[1] > 0 && d[0] != d[1]) {
                            // screen sprites keep their own proportions
                            int w = d[0] >= d[1] ? size : Math.max(2, size * d[0] / d[1]);
                            int h = d[0] >= d[1] ? Math.max(2, size * d[1] / d[0]) : size;
                            ok = Gfx.draw(Ui.g, pack, flat, cx - w / 2, cy - h / 2, w, h);
                        } else ok = Gfx.draw(Ui.g, pack, flat, cx - size / 2, cy - size / 2, size, size);
                    }
                    else if (t.id.equals("crystal")) {
                        String ent = null;
                        for (String hp : have) if (hp.contains("entity/end_crystal/end_crystal.png")) ent = hp;
                        ok = ent != null && Gfx.crystal(Ui.g, pack, ent, cx, cy, size);
                    } else ok = false;
                }
                if (ok) return;
            }
        }
        drawItem(t.stack(), cx, cy, size / 16f);
    }

    private static final Map<String, int[]> DIMS = new ConcurrentHashMap<>();

    /** Width and height of a PNG inside a pack (read once from its header). */
    private static int[] pngSize(String pack, String path) {
        String k = pack + "|" + path;
        int[] d = DIMS.get(k);
        if (d != null) return d;
        d = new int[]{0, 0};
        try {
            byte[] b = PackFiles.read(pack, path);
            if (b != null && b.length > 24) {
                d[0] = ((b[16] & 255) << 24) | ((b[17] & 255) << 16) | ((b[18] & 255) << 8) | (b[19] & 255);
                d[1] = ((b[20] & 255) << 24) | ((b[21] & 255) << 16) | ((b[22] & 255) << 8) | (b[23] & 255);
            }
        } catch (Throwable ignored) {}
        DIMS.put(k, d);
        return d;
    }

    private static Map<String, String> mix() { return Config.get().mix; }

    private static int changed(Cat c) {
        int n = 0;
        for (String t : c.tiles) if (mix().containsKey(t)) n++;
        return n;
    }

    private List<Tile> visibleTiles() {
        List<Tile> out = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (String id : cat.tiles) {
            Tile t = TILES.get(id);
            String chosen = mix().get(id);
            if (filter == 1 && chosen == null) continue;
            if (filter == 2 && !Config.get().favTiles.contains(id)) continue;
            if (!q.isEmpty() && !t.name.toLowerCase(Locale.ROOT).contains(q)
                && (chosen == null || !packName(chosen).toLowerCase(Locale.ROOT).contains(q))) continue;
            out.add(t);
        }
        return out;
    }

    private boolean chip(String label, int x, int y, int w, boolean on) {
        boolean hv = Ui.hover(x, y, w, 14);
        if (on) {
            Ui.grad(x, y, w, 14, Ui.alpha(Ui.accent(), 150), Ui.alpha(Ui.accent(), 90));
            Ui.border(x, y, w, 14, Ui.accent());
        } else {
            Ui.card(x, y, w, 14, hv, false);
        }
        Ui.textC(label, x + w / 2, y + 3, 0xFFFFFFFF);
        if (hv && Ui.pressed) { Ui.pressed = false; return true; }
        return false;
    }

    private void drawToast(int x, int w, int y) {
        if (System.currentTimeMillis() >= toastUntil) return;
        int tw = font.width(toast) + 18;
        int tx = x + (w - tw) / 2;
        Ui.card(tx, y, tw, 16, false, true);
        Ui.textC(toast, tx + tw / 2, y + 4, 0xFFFFE6A8);
    }

    // ---------------------------------------------------------------- PVP page
    void render(GuiGraphics g, int x0, int y0, int W, int H, int mx, int my, String search) {
        this.search = search == null ? "" : search;
        if (!scannedOnce) rescanPacks();
        sideX = x0; sideW = 142;
        sidebar(x0, y0, sideW, H);
        int cx = x0 + sideW + 10, cw = W - sideW - 10;
        if (picking != null) chooser(cx, y0, cw, H);
        else grid(cx, y0, cw, H);
        drawToast(cx, cw, y0 + H - 34);
        if (after != null) { Runnable r = after; after = null; r.run(); }
    }

    /** Rectangle of the search field (drawn by the menu): x, y, w, h. */
    static int[] searchRect(int x0, int y0, int W) { return new int[]{x0 + W - 130, y0 + 2, 128, 16}; }

    private void sidebar(int x, int y, int w, int h) {
        Ui.card(x, y, w, h, false, false);
        Set<String> all = new HashSet<>();
        for (Cat c : PVP) Collections.addAll(all, c.tiles);
        for (Cat c : ALL) Collections.addAll(all, c.tiles);
        int ch = 0;
        for (String t : all) if (mix().containsKey(t)) ch++;
        Ui.text("Your TxT setup", x + 9, y + 8, 0xFFFFFFFF);
        Ui.text(ch + " / " + all.size() + " changed", x + 9, y + 19, Ui.accent());
        int bw = w - 18;
        Ui.fill(x + 9, y + 30, bw, 2, 0x22FFFFFF);
        Ui.fill(x + 9, y + 30, bw * ch / Math.max(1, all.size()), 2, Ui.accent());

        int listTop = y + 38, bottomH = 3 * 18 + 16, listH = h - (listTop - y) - bottomH;
        int rows = 2 * 13 + (PVP.length + ALL.length) * 19;
        sideMax = Math.max(0, rows - listH);
        sideScroll = Math.max(0, Math.min(sideMax, sideScroll));
        Ui.clip(x + 1, listTop, w - 2, listH);
        int ry = listTop - sideScroll;
        Ui.text("COMBAT", x + 9, ry + 2, Ui.dim()); ry += 13;
        for (Cat c : PVP) { catRow(c, x + 4, ry, w - 8); ry += 19; }
        Ui.text("MORE", x + 9, ry + 2, Ui.dim()); ry += 13;
        for (Cat c : ALL) { catRow(c, x + 4, ry, w - 8); ry += 19; }
        Ui.unclip();

        int by = y + h - bottomH + 4;
        Ui.text(index.size() + " packs found", x + 9, by, Ui.dim());
        by += 12;
        if (Ui.button(dirty ? "Apply changes" : "Applied", x + 6, by, w - 12, 16, dirty)) {
            if (dirty) after = this::apply;
        }
        by += 18;
        if (Ui.button("Reset all", x + 6, by, w - 12, 16, false)) {
            mix().clear(); Config.save(); dirty = true; toast("All textures back to vanilla.");
        }
    }

    private void catRow(Cat c, int x, int y, int w) {
        boolean on = c == cat, hv = Ui.hover(x, y, w, 18);
        if (on) {
            Ui.grad(x, y, w, 18, Ui.alpha(Ui.accent(), 120), Ui.alpha(Ui.accent(), 50));
            Ui.fill(x, y, 2, 18, Ui.accent());
        } else if (hv) {
            Ui.fill(x, y, w, 18, 0x1AFFFFFF);
        }
        Ui.g.renderItem(c.stack(), x + 4, y + 1);
        Ui.text(Ui.fit(c.name, w - 60), x + 24, y + 5, on || hv ? 0xFFFFFFFF : 0xFFD6D8EA);
        Ui.textR(changed(c) + "/" + c.tiles.length, x + w - 5, y + 5, on ? 0xFFFFFFFF : Ui.accent());
        if (hv && Ui.pressed) { Ui.pressed = false; cat = c; picking = null; scroll = 0; }
    }

    private void grid(int x, int y, int w, int h) {
        Ui.text(cat.name, x, y + 4, 0xFFFFFFFF);
        Ui.text(cat.desc, x, y + 16, Ui.dim());
        int fx = x + w;
        for (int i = FILTERS.length - 1; i >= 0; i--) {
            int bw = font.width(FILTERS[i]) + 14;
            fx -= bw;
            if (chip(FILTERS[i], fx, y + 21, bw, filter == i)) { filter = i; scroll = 0; }
            fx -= 3;
        }

        int top = y + 40, gh = h - 40 - 14;
        List<Tile> list = visibleTiles();
        int gap = 7;
        int cols = Math.max(2, Math.min(6, (w + gap) / (96 + gap)));
        int tw = (w - gap * (cols - 1) - 4) / cols;
        int th = Math.max(78, Math.min(110, tw + 4));
        int rows = (list.size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * (th + gap) - gap - gh);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        Ui.clip(x - 2, top - 2, w + 4, gh + 4);
        for (int i = 0; i < list.size(); i++) {
            int tx = x + (i % cols) * (tw + gap), ty = top + (i / cols) * (th + gap) - scroll;
            if (ty + th < top || ty > top + gh) continue;
            tile(list.get(i), tx, ty, tw, th);
        }
        Ui.unclip();
        if (list.isEmpty()) {
            Ui.textC(scanning ? "Reading packs..." : "Nothing here", x + w / 2, top + gh / 2 - 12, Ui.dim());
            if (!scanning && filter != 0 && Ui.button("Show all", x + w / 2 - 40, top + gh / 2 + 2, 80, 16, true)) filter = 0;
        }
        scrollbar(x + w - 2, top, gh, scroll, maxScroll);
        status(x, y + h - 11, w);
    }

    private void scrollbar(int x, int top, int h, int s, int max) {
        if (max <= 0) return;
        int bh = Math.max(16, h * h / (h + max));
        Ui.fill(x, top, 2, h, 0x14FFFFFF);
        Ui.fill(x, top + (int) ((h - bh) * (s / (float) max)), 2, bh, Ui.accent());
    }

    private void status(int x, int y, int w) {
        String s = scanning ? "Reading packs " + scanDone + "/" + scanTotal : PackManager.packs.size() + " packs";
        int sw = font.width(s);
        Ui.text(s, x + w - sw, y, Ui.dim());
        if (scanning && scanTotal > 0) {
            Ui.fill(x + w - sw, y + 9, sw, 1, 0x33FFFFFF);
            Ui.fill(x + w - sw, y + 9, sw * scanDone / Math.max(1, scanTotal), 1, Ui.accent());
        }
    }

    private void tile(Tile t, int x, int y, int w, int h) {
        String chosen = mix().get(t.id);
        boolean hv = Ui.hover(x, y, w, h);
        Ui.card(x, y, w, h, hv, false);
        if (chosen != null) Ui.grad(x + 1, y + 1, w - 2, 3, Ui.accent(), Ui.alpha(Ui.accent(), 120));

        String n = String.valueOf(count(t.id));
        int tw = Ui.w(n), nw = Math.max(15, tw + 8), bx = x + w - nw - 5, by = y + 6;
        Ui.fill(bx, by, nw, 13, 0xAA000000);
        Ui.border(bx, by, nw, 13, 0x22FFFFFF);
        Ui.text(n, bx + (nw - tw) / 2, by + 3, 0xFFDDDDEE);
        boolean fav = Config.get().favTiles.contains(t.id);
        boolean shv = Ui.hover(x + 4, y + 6, 11, 11);
        if (fav || hv) Ui.text("★", x + 6, y + 8, fav ? 0xFFFFD27A : (shv ? 0xFFFFFFFF : 0x66FFFFFF));

        int area = h - 36;
        int size = Math.max(24, Math.min(area - 6, w - 34));
        int cx = x + w / 2, cy = y + 8 + area / 2;
        // glow behind the item
        Ui.g.fillGradient(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, Ui.alpha(Ui.accent(), hv ? 40 : 18), 0x00000000);
        boolean preview = chosen != null;
        if (preview) drawPreview(t, chosen, cx, cy, size);
        else drawItem(t.stack(), cx, cy, size / 16f);

        Ui.text(Ui.fit(t.name, w - 12), x + 7, y + h - 25, 0xFFFFFFFF);
        Ui.text(Ui.fit(chosen == null ? "Vanilla" : packName(chosen), w - 12), x + 7, y + h - 13,
            chosen == null ? Ui.dim() : Ui.accent());

        if (hv && Ui.pressed) {
            Ui.pressed = false;
            if (shv) {
                if (fav) Config.get().favTiles.remove(t.id); else Config.get().favTiles.add(t.id);
                Config.save();
            } else {
                picking = t.id; pickScroll = 0;
            }
        }
        if (hv && Ui.rpressed && chosen != null) {
            Ui.rpressed = false;
            mix().remove(t.id); Config.save(); dirty = true;
        }
    }

    // ---------------------------------------------------------------- chooser
    private void chooser(int x, int y, int w, int h) {
        Tile t = TILES.get(picking);
        String chosen = mix().get(t.id);
        Ui.g.renderItem(t.stack(), x, y + 3);
        Ui.text(t.name, x + 21, y + 4, 0xFFFFFFFF);
        Ui.text("Pick a style", x + 21, y + 16, Ui.dim());
        if (Ui.button("Back", x + w - 52, y + 21, 52, 15, false)) { picking = null; return; }

        String q = search.toLowerCase(Locale.ROOT).trim();
        List<String> packs = new ArrayList<>();
        packs.add(null);
        for (String f : offering(t.id)) if (q.isEmpty() || packName(f).toLowerCase(Locale.ROOT).contains(q)) packs.add(f);

        int top = y + 42, gh = h - 42 - 14, gap = 7;
        int sideW = w >= 330 ? 96 : 0;
        int gx = x + (sideW > 0 ? sideW + 8 : 0), gw = w - (gx - x);
        if (sideW > 0) skinPreview(t, x, top, sideW, gh);
        int cols = Math.max(2, Math.min(6, (gw + gap) / (100 + gap)));
        int cw = (gw - gap * (cols - 1) - 4) / cols, chh = Math.max(84, Math.min(104, cw));
        int rows = (packs.size() + cols - 1) / cols;
        pickMax = Math.max(0, rows * (chh + gap) - gap - gh);
        pickScroll = Math.max(0, Math.min(pickMax, pickScroll));
        Ui.clip(x - 2, top - 2, w + 4, gh + 4);
        for (int i = 0; i < packs.size(); i++) {
            int px = gx + (i % cols) * (cw + gap), py = top + (i / cols) * (chh + gap) - pickScroll;
            if (py + chh < top || py > top + gh) continue;
            String f = packs.get(i);
            boolean sel = Objects.equals(f, chosen), hv = Ui.hover(px, py, cw, chh);
            Ui.card(px, py, cw, chh, hv, sel);
            int size = Math.max(24, Math.min(chh - 38, cw - 30));
            int cx = px + cw / 2, cy = py + 6 + (chh - 30) / 2;
            Ui.g.fillGradient(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, Ui.alpha(Ui.accent(), hv ? 40 : 16), 0x00000000);
            if (f == null) drawItem(t.stack(), cx, cy, size / 16f);
            else drawPreview(t, f, cx, cy, size);
            Ui.text(Ui.fit(f == null ? "Vanilla" : packName(f), cw - 12), px + 7, py + chh - 24, 0xFFFFFFFF);
            Ui.text(sel ? "Selected" : (f == null ? "Default" : "Click to use"), px + 7, py + chh - 12, sel ? Ui.accent() : Ui.dim());
            if (hv && Ui.pressed) {
                Ui.pressed = false;
                if (f == null) mix().remove(t.id); else mix().put(t.id, f);
                Config.save();
                dirty = true;
            }
        }
        Ui.unclip();
        if (packs.size() == 1) Ui.textC(scanning ? "Reading packs..." : "No pack has this texture", gx + gw / 2, top + 110, Ui.dim());
        scrollbar(x + w - 2, top, gh, pickScroll, pickMax);
        status(x, y + h - 11, w);
    }

    // ---------------------------------------------------------------- texture pack manager
    void renderManager(GuiGraphics g, int x, int y, int w, int h, String search) {
        if (!scannedOnce) rescanPacks();
        long now = System.currentTimeMillis();
        if (now - lastSelRead > 500) { selected = PackManager.selected(); lastSelRead = now; }
        String q = search == null ? "" : search.toLowerCase(Locale.ROOT).trim();

        Ui.text("Texture packs", x, y + 4, 0xFFFFFFFF);
        int active = 0;
        for (PackManager.Pack p : PackManager.packs) if (selected.contains(p.id())) active++;
        Ui.text(PackManager.packs.size() + " packs, " + active + " active", x, y + 16, Ui.dim());

        int bx = x + 150;
        if (Ui.button(scanning ? "Scanning..." : "Rescan", bx, y + 2, 64, 16, true)) { rescanPacks(); toast("Packs rescanned."); }
        if (Ui.button("Folder", bx + 68, y + 2, 52, 16, false)) { try { OpenUtil.open(PackManager.dir()); } catch (Exception ignored) {} }
        if (Ui.button("All off", bx + 124, y + 2, 52, 16, false)) PackManager.disableAll();
        int fx = x + w;
        for (int i = PACK_FILTERS.length - 1; i >= 0; i--) {
            int bw = font.width(PACK_FILTERS[i]) + 14;
            fx -= bw;
            if (chip(PACK_FILTERS[i], fx, y + 22, bw, packFilter == i)) { packFilter = i; listScroll = 0; }
            fx -= 3;
        }

        List<PackManager.Pack> list = new ArrayList<>();
        for (PackManager.Pack p : PackManager.packs) {
            boolean on = selected.contains(p.id());
            if (packFilter == 1 && !on) continue;
            if (packFilter == 2 && on) continue;
            if (!q.isEmpty() && !p.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            list.add(p);
        }

        int top = y + 42, lh = h - 42 - 2, gap = 6;
        int cols = w >= 460 ? 2 : 1;
        int cw = (w - gap * (cols - 1) - 4) / cols, rh = 46;
        int rows = (list.size() + cols - 1) / cols;
        listMax = Math.max(0, rows * (rh + gap) - gap - lh);
        listScroll = Math.max(0, Math.min(listMax, listScroll));
        Ui.clip(x - 2, top - 2, w + 4, lh + 4);
        for (int i = 0; i < list.size(); i++) {
            int px = x + (i % cols) * (cw + gap), py = top + (i / cols) * (rh + gap) - listScroll;
            if (py + rh < top || py > top + lh) continue;
            PackManager.Pack p = list.get(i);
            boolean on = selected.contains(p.id()), hv = Ui.hover(px, py, cw, rh);
            Ui.card(px, py, cw, rh, hv, on);
            if (on) Ui.fill(px + 1, py + 1, 2, rh - 2, Ui.accent());
            Ui.fill(px + 6, py + 6, 34, 34, 0x66000000);
            if (!Gfx.draw(Ui.g, p.file, "pack.png", px + 7, py + 7, 32, 32)) {
                Ui.grad(px + 7, py + 7, 32, 32, Ui.mix(Ui.accent(), 0xFF000000, 0.3f), Ui.mix(Ui.accent(), 0xFF000000, 0.7f));
                Ui.textC(p.name.isEmpty() ? "?" : p.name.substring(0, 1).toUpperCase(Locale.ROOT), px + 23, py + 19, 0xFFFFFFFF);
            }
            Ui.text(Ui.fit(p.name, cw - 86), px + 46, py + 9, 0xFFFFFFFF);
            String d = descs.getOrDefault(p.file, "");
            Ui.text(Ui.fit(d.isEmpty() ? PackManager.sizeText(p) : d, cw - 86), px + 46, py + 22, Ui.dim());
            if (p.nested()) {
                Ui.text("\u25B8 " + Ui.fit(p.folder, cw - 140), px + 46, py + 33, Ui.dim());
                if (Ui.button("Use", px + cw - 44, py + 15, 38, 15, false)) {
                    PackManager.Pack fp = p;
                    after = () -> { if (PackManager.moveToTop(fp)) { rescanPacks(); toast("Moved to your main pack folder."); } else toast("Could not move the pack."); };
                }
            } else {
                Ui.text(on ? "Active" : "", px + 46, py + 33, Ui.accent());
                if (Ui.toggle(px + cw - 34, py + 16, on)) PackManager.toggle(p);
            }
        }
        Ui.unclip();
        if (list.isEmpty()) Ui.textC(scanning ? "Reading packs..." : "No packs found", x + w / 2, top + 60, Ui.dim());
        scrollbar(x + w - 2, top, lh, listScroll, listMax);
        drawToast(x, w, y + h - 20);
        if (after != null) { Runnable r = after; after = null; r.run(); }
    }

    void scroll(double mx, double my, double amount) {
        int d = (int) (amount * 26);
        if (mx < sideX + sideW) { sideScroll = Math.max(0, Math.min(sideMax, sideScroll - d)); return; }
        if (picking != null) pickScroll = Math.max(0, Math.min(pickMax, pickScroll - d));
        else scroll = Math.max(0, Math.min(maxScroll, scroll - d));
    }

    void scrollManager(double amount) { listScroll = Math.max(0, Math.min(listMax, listScroll - (int) (amount * 26))); }

    /** Your own skin on the left, holding the item / block of this tile. */
    private void skinPreview(Tile t, int x, int y, int w, int h) {
        Ui.card(x, y, w, h, false, false);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.client.player.LocalPlayer pl = mc.player;
        if (pl == null) return;
        net.minecraft.world.entity.player.Inventory inv = pl.getInventory();
        int sel = inv.getSelectedSlot();
        ItemStack mainHand = inv.getItem(sel), offHand = inv.getItem(40);
        try {
            inv.setItem(sel, t.stack().copy());
            inv.setItem(40, ItemStack.EMPTY);
            int sc = Math.max(20, Math.min((int) (h * 0.36f), w - 24));
            Ui.g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(
                Ui.g, x + 2, y + 6, x + w - 2, y + h - 18, sc, 0.0625f, (float) Ui.mx, (float) Ui.my, pl);
            Ui.g.disableScissor();
        } catch (Throwable ignored) {
        } finally {
            inv.setItem(sel, mainHand);
            inv.setItem(40, offHand);
        }
        Ui.textC(Ui.fit(t.name, w - 8), x + w / 2, y + h - 13, Ui.dim());
    }

    // ---------------------------------------------------------------- build the mix pack
    /** After the texture choices were replaced from outside (presets). */
    static void syncMix() { applied = null; }

    static Map<String, String> currentMix() { return applied != null ? applied : new HashMap<>(Config.get().mix); }

    /** Copies the chosen textures into the generated pack folder. */
    static void copyMix(Path out) throws Exception {
        for (Map.Entry<String, String> e : currentMix().entrySet()) {
            Tile t = TILES.get(e.getKey());
            if (t == null) continue;
            for (String p : t.paths) {
                for (String q : new String[]{p, p + ".mcmeta"}) {
                    byte[] b = PackFiles.read(e.getValue(), "assets/minecraft/" + q);
                    if (b == null) continue;
                    Path dst = out.resolve("assets/minecraft/" + q);
                    Files.createDirectories(dst.getParent());
                    Files.write(dst, b);
                }
            }
        }
    }

    void apply() {
        try {
            dirty = false;
            applied = new HashMap<>(mix());
            boolean any = !mix().isEmpty();
            Tweaks.rebuild(true);
            toast(any ? "Textures applied - reloading..." : "Vanilla textures - reloading...");
        } catch (Exception e) {
            toast("Could not build the pack: " + e.getClass().getSimpleName());
        }
    }

    void closed() {
        if (dirty) apply();
    }
}
