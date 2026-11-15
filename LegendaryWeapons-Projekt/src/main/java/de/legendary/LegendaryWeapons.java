package de.legendary;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LegendaryWeapons extends JavaPlugin implements Listener, TabExecutor {

    // ------------------------------------------------------------------
    //  Die 7 Waffen inkl. Rezepte
    // ------------------------------------------------------------------
    enum Weapon {
        FIRE("fire", "Feuerschwert", Material.NETHERITE_SWORD, NamedTextColor.RED,
                new String[]{"Gegner brennen 10 Sekunden", "Halten: Feuerresistenz", "Rechtsklick: Feuerball (5s Abklingzeit)"},
                new String[]{"MBM", "BSB", " N "},
                'M', Material.MAGMA_BLOCK, 'B', Material.BLAZE_ROD, 'S', Material.DIAMOND_SWORD, 'N', Material.NETHERITE_INGOT),

        WATER("water", "Wasserschwert", Material.DIAMOND_SWORD, NamedTextColor.AQUA,
                new String[]{"Gegner werden verlangsamt", "Halten: Unterwasseratmung + Delfingeschwindigkeit", "Rechtsklick: Flutwelle (10s Abklingzeit)"},
                new String[]{"PHP", "DSD", " N "},
                'P', Material.PRISMARINE_CRYSTALS, 'H', Material.HEART_OF_THE_SEA, 'D', Material.DIAMOND,
                'S', Material.DIAMOND_SWORD, 'N', Material.NETHERITE_INGOT),

        STONE("stone", "Steinschwert", Material.NETHERITE_SWORD, NamedTextColor.GRAY,
                new String[]{"+3 Schaden, Gegner werden schwer und schwach", "Rechtsklick: Steinhaut (20s Abklingzeit)"},
                new String[]{"OIO", "ISI", " N "},
                'O', Material.OBSIDIAN, 'I', Material.IRON_BLOCK, 'S', Material.STONE_SWORD, 'N', Material.NETHERITE_INGOT),

        AIR("air", "Luftschwert", Material.DIAMOND_SWORD, NamedTextColor.WHITE,
                new String[]{"Schlag schleudert Gegner in die Luft", "Halten: Geschwindigkeit + Sprungkraft, kein Fallschaden", "Rechtsklick: Luftstoß (3s Abklingzeit)"},
                new String[]{"PBP", "FSF", " N "},
                'P', Material.PHANTOM_MEMBRANE, 'B', Material.BREEZE_ROD, 'F', Material.FEATHER,
                'S', Material.DIAMOND_SWORD, 'N', Material.NETHERITE_INGOT),

        VOID("void", "Void-Schwert", Material.NETHERITE_SWORD, NamedTextColor.DARK_PURPLE,
                new String[]{"Gegner: Dunkelheit + Wither", "Du heilst dich bei Treffern", "Rechtsklick: Schwarzes Loch (15s Abklingzeit)"},
                new String[]{"EWE", "OSO", " N "},
                'E', Material.ECHO_SHARD, 'W', Material.NETHER_STAR, 'O', Material.OBSIDIAN,
                'S', Material.NETHERITE_SWORD, 'N', Material.NETHERITE_INGOT),

        MACE("ender_mace", "Ender-Mace", Material.MACE, NamedTextColor.DARK_AQUA,
                new String[]{"Rechtsklick: Teleport bis 20 Blöcke (8s Abklingzeit)", "Kein Fallschaden nach dem Teleport"},
                new String[]{"PEP", "EME", " N "},
                'P', Material.ENDER_PEARL, 'E', Material.ENDER_EYE, 'M', Material.MACE, 'N', Material.NETHERITE_INGOT),

        PICKAXE("bedrock_pickaxe", "Bedrock-Spitzhacke", Material.NETHERITE_PICKAXE, NamedTextColor.DARK_GRAY,
                new String[]{"Baut 3x3 Blöcke gleichzeitig ab", "Kann Bedrock im 3x3-Bereich abbauen", "Unzerstörbar"},
                new String[]{"NWN", "DPD", " B "},
                'N', Material.NETHERITE_INGOT, 'W', Material.NETHER_STAR, 'D', Material.DIAMOND_BLOCK,
                'P', Material.NETHERITE_PICKAXE, 'B', Material.BREEZE_ROD);

        final String id, displayName;
        final Material base;
        final NamedTextColor color;
        final String[] lore, shape;
        final Map<Character, Material> ingredients = new LinkedHashMap<>();

        Weapon(String id, String displayName, Material base, NamedTextColor color,
               String[] lore, String[] shape, Object... ing) {
            this.id = id;
            this.displayName = displayName;
            this.base = base;
            this.color = color;
            this.lore = lore;
            this.shape = shape;
            for (int i = 0; i < ing.length; i += 2) {
                ingredients.put((Character) ing[i], (Material) ing[i + 1]);
            }
        }

        static Weapon byId(String id) {
            for (Weapon w : values()) if (w.id.equalsIgnoreCase(id)) return w;
            return null;
        }
    }

    // ------------------------------------------------------------------
    //  Felder
    // ------------------------------------------------------------------
    private NamespacedKey weaponKey;
    private File dataFile;
    private YamlConfiguration data;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, BlockFace> lastFace = new HashMap<>();
    private final Set<UUID> breaking = new HashSet<>();

    @Override
    public void onEnable() {
        weaponKey = new NamespacedKey(this, "weapon");
        getDataFolder().mkdirs();
        dataFile = new File(getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);

        for (Weapon w : Weapon.values()) {
            ShapedRecipe r = new ShapedRecipe(new NamespacedKey(this, w.id), createItem(w));
            r.shape(w.shape);
            w.ingredients.forEach(r::setIngredient);
            Bukkit.addRecipe(r);
        }

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("legendary").setExecutor(this);
        getCommand("legendary").setTabCompleter(this);

        // Passive Effekte
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                Weapon w = weaponOf(p.getInventory().getItemInMainHand());
                if (w == null) continue;
                switch (w) {
                    case FIRE -> effect(p, PotionEffectType.FIRE_RESISTANCE, 0);
                    case WATER -> {
                        effect(p, PotionEffectType.WATER_BREATHING, 0);
                        effect(p, PotionEffectType.DOLPHINS_GRACE, 0);
                    }
                    case AIR -> {
                        effect(p, PotionEffectType.SPEED, 1);
                        effect(p, PotionEffectType.JUMP_BOOST, 1);
                    }
                    default -> { }
                }
            }
        }, 20L, 20L);
    }

    @Override
    public void onDisable() {
        for (Weapon w : Weapon.values()) Bukkit.removeRecipe(new NamespacedKey(this, w.id));
    }

    private void effect(Player p, PotionEffectType type, int amp) {
        p.addPotionEffect(new PotionEffect(type, 60, amp, true, false));
    }

    // ------------------------------------------------------------------
    //  Items
    // ------------------------------------------------------------------
    private ItemStack createItem(Weapon w) {
        ItemStack item = new ItemStack(w.base);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(w.displayName, w.color, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : w.lore) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Einzigartig – nur 1x pro Server", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setUnbreakable(true);
        meta.setItemModel(new NamespacedKey("legendary", w.id)); // Texturepack: assets/legendary/items/<id>.json
        meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, w.id);
        item.setItemMeta(meta);
        return item;
    }

    private Weapon weaponOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING);
        return id == null ? null : Weapon.byId(id);
    }

    private Weapon weaponOf(Recipe recipe) {
        if (recipe instanceof ShapedRecipe sr && sr.getKey().getNamespace().equals(weaponKey.getNamespace())) {
            return Weapon.byId(sr.getKey().getKey());
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  Nur einmal pro Server craftbar
    // ------------------------------------------------------------------
    private boolean isCrafted(Weapon w) {
        return data.contains("crafted." + w.id);
    }

    private void markCrafted(Weapon w, String player) {
        data.set("crafted." + w.id, player);
        saveData();
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("data.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent e) {
        Weapon w = weaponOf(e.getRecipe());
        if (w != null && isCrafted(w)) {
            e.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        Weapon w = weaponOf(e.getRecipe());
        if (w == null) return;
        Player p = (Player) e.getWhoClicked();
        if (isCrafted(w) || e.isShiftClick()) {
            e.setCancelled(true);
            if (isCrafted(w)) {
                p.sendMessage(Component.text("Diese Waffe wurde auf dem Server schon gecraftet!", NamedTextColor.RED));
            } else {
                p.sendMessage(Component.text("Bitte ohne Shift-Klick craften.", NamedTextColor.RED));
            }
            return;
        }
        markCrafted(w, p.getName());
        Bukkit.broadcast(Component.text(p.getName() + " hat das ", NamedTextColor.GOLD)
                .append(Component.text(w.displayName, w.color, TextDecoration.BOLD))
                .append(Component.text(" gecraftet! Es kann nie wieder gecraftet werden.", NamedTextColor.GOLD)));
    }

    // ------------------------------------------------------------------
    //  Cooldowns
    // ------------------------------------------------------------------
    private boolean onCooldown(Player p, Weapon w, int seconds) {
        String key = p.getUniqueId() + w.id;
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(key);
        if (until != null && until > now) {
            p.sendActionBar(Component.text("Abklingzeit: " + ((until - now) / 1000 + 1) + "s", NamedTextColor.GRAY));
            return true;
        }
        cooldowns.put(key, now + seconds * 1000L);
        return false;
    }

    // ------------------------------------------------------------------
    //  Treffer-Effekte
    // ------------------------------------------------------------------
    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity target)) return;
        Weapon w = weaponOf(p.getInventory().getItemInMainHand());
        if (w == null) return;

        switch (w) {
            case FIRE -> target.setFireTicks(200);
            case WATER -> target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            case STONE -> {
                e.setDamage(e.getDamage() + 3);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
            }
            case AIR -> target.setVelocity(target.getVelocity().setY(1.0));
            case VOID -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
                var attr = p.getAttribute(Attribute.MAX_HEALTH);
                double max = attr == null ? 20 : attr.getValue();
                p.setHealth(Math.min(max, p.getHealth() + 2));
            }
            default -> { }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL || !(e.getEntity() instanceof Player p)) return;
        Weapon w = weaponOf(p.getInventory().getItemInMainHand());
        if (w == Weapon.AIR || w == Weapon.MACE) e.setCancelled(true);
    }

    // ------------------------------------------------------------------
    //  Rechtsklick-Fähigkeiten
    // ------------------------------------------------------------------
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            lastFace.put(p.getUniqueId(), e.getBlockFace());
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Weapon w = weaponOf(p.getInventory().getItemInMainHand());
        if (w == null) return;
        World world = p.getWorld();
        Location loc = p.getLocation();

        switch (w) {
            case FIRE -> {
                if (onCooldown(p, w, 5)) return;
                Fireball fb = p.launchProjectile(Fireball.class, p.getEyeLocation().getDirection().multiply(1.5));
                fb.setYield(1.5f);
                fb.setIsIncendiary(false);
                world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
            }
            case WATER -> {
                if (onCooldown(p, w, 10)) return;
                for (var ent : p.getNearbyEntities(6, 3, 6)) {
                    if (!(ent instanceof LivingEntity le)) continue;
                    Vector push = le.getLocation().toVector().subtract(loc.toVector());
                    if (push.lengthSquared() < 0.01) continue;
                    le.setVelocity(push.normalize().multiply(1.5).setY(0.4));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                }
                world.spawnParticle(Particle.SPLASH, loc.clone().add(0, 1, 0), 200, 3, 1, 3);
                world.playSound(loc, Sound.ENTITY_PLAYER_SPLASH, 1f, 0.8f);
            }
            case STONE -> {
                if (onCooldown(p, w, 20)) return;
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 160, 1));
                world.spawnParticle(Particle.BLOCK, loc.clone().add(0, 1, 0), 60, 0.5, 1, 0.5,
                        Material.STONE.createBlockData());
                world.playSound(loc, Sound.BLOCK_STONE_PLACE, 1f, 0.6f);
            }
            case AIR -> {
                if (onCooldown(p, w, 3)) return;
                Vector v = loc.getDirection().multiply(1.5);
                v.setY(v.getY() + 0.4);
                p.setVelocity(v);
                world.spawnParticle(Particle.CLOUD, loc, 40, 0.4, 0.2, 0.4, 0.05);
                world.playSound(loc, Sound.ENTITY_BREEZE_JUMP, 1f, 1f);
            }
            case VOID -> {
                if (onCooldown(p, w, 15)) return;
                for (var ent : p.getNearbyEntities(10, 5, 10)) {
                    if (!(ent instanceof LivingEntity le)) continue;
                    Vector pull = loc.toVector().subtract(le.getLocation().toVector());
                    if (pull.lengthSquared() < 0.01) continue;
                    le.setVelocity(pull.normalize().multiply(1.2).setY(0.2));
                }
                world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 200, 2, 1, 2, 0.1);
                world.playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 1f, 0.5f);
            }
            case MACE -> {
                if (onCooldown(p, w, 8)) return;
                Location eye = p.getEyeLocation();
                Vector dir = eye.getDirection();
                RayTraceResult r = world.rayTraceBlocks(eye, dir, 20, FluidCollisionMode.NEVER, true);
                Location target = (r != null && r.getHitPosition() != null)
                        ? r.getHitPosition().toLocation(world).subtract(dir.clone().multiply(0.6))
                        : eye.clone().add(dir.clone().multiply(20));
                target.setYaw(loc.getYaw());
                target.setPitch(loc.getPitch());
                target.subtract(0, 1, 0); // Augenhöhe -> Füße
                world.spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 80, 0.3, 0.5, 0.3);
                p.teleport(target);
                p.setFallDistance(0);
                world.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------
    //  3x3 Bedrock-Spitzhacke
    // ------------------------------------------------------------------
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (weaponOf(tool) != Weapon.PICKAXE || breaking.contains(p.getUniqueId())) return;

        BlockFace face = lastFace.getOrDefault(p.getUniqueId(), BlockFace.UP);
        Block center = e.getBlock();
        breaking.add(p.getUniqueId());
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    Block n = switch (face) {
                        case UP, DOWN -> center.getRelative(a, 0, b);
                        case NORTH, SOUTH -> center.getRelative(a, b, 0);
                        default -> center.getRelative(0, a, b);
                    };
                    breakExtra(p, n, tool);
                }
            }
        } finally {
            breaking.remove(p.getUniqueId());
        }
    }

    private void breakExtra(Player p, Block n, ItemStack tool) {
        Material type = n.getType();
        if (type.isAir() || n.isLiquid()) return;

        World w = n.getWorld();
        if (type == Material.BEDROCK) {
            // unterste Schicht und Nether-Decke bleiben geschützt
            if (n.getY() <= w.getMinHeight()) return;
            if (w.getEnvironment() == World.Environment.NETHER && n.getY() >= 127) return;
        } else if (type.getHardness() < 0) {
            return; // Barrier, Befehlsblöcke, Endportalrahmen usw.
        }

        BlockBreakEvent be = new BlockBreakEvent(n, p);
        Bukkit.getPluginManager().callEvent(be);
        if (be.isCancelled()) return;

        if (type == Material.BEDROCK) {
            n.setType(Material.AIR);
            w.dropItemNaturally(n.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.BEDROCK));
        } else if (be.isDropItems()) {
            n.breakNaturally(tool);
        } else {
            n.setType(Material.AIR);
        }
    }

    // ------------------------------------------------------------------
    //  Waffen-Menü (für Creative-Spieler und Admins)
    // ------------------------------------------------------------------
    private static class MenuHolder implements InventoryHolder {
        private Inventory inv;

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private boolean canUseMenu(Player p) {
        return p.hasPermission("legendary.admin") || p.getGameMode() == GameMode.CREATIVE;
    }

    private void openMenu(Player p) {
        MenuHolder holder = new MenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text("Legendäre Waffen", NamedTextColor.GOLD));
        holder.inv = inv;
        Weapon[] all = Weapon.values();
        for (int i = 0; i < all.length; i++) {
            inv.setItem(1 + i, createItem(all[i]));
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof MenuHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        Weapon w = weaponOf(e.getCurrentItem());
        if (w == null || !canUseMenu(p)) return;
        p.getInventory().addItem(createItem(w));
        p.sendMessage(Component.text(w.displayName + " erhalten.", w.color));
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof MenuHolder) e.setCancelled(true);
    }

    // ------------------------------------------------------------------
    //  Befehl /legendary
    // ------------------------------------------------------------------
    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            if (s instanceof Player pl && canUseMenu(pl)) {
                openMenu(pl);
            } else {
                s.sendMessage(Component.text("/legendary <list|recipe <waffe>|menu|give <spieler> <waffe>|reset <waffe>>", NamedTextColor.YELLOW));
            }
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "menu" -> {
                if (s instanceof Player pl && canUseMenu(pl)) {
                    openMenu(pl);
                } else {
                    s.sendMessage(Component.text("Das Menü ist nur für OPs oder im Creative-Modus.", NamedTextColor.RED));
                }
            }
            case "list" -> {
                for (Weapon w : Weapon.values()) {
                    String status = isCrafted(w) ? "gecraftet von " + data.getString("crafted." + w.id) : "noch verfügbar";
                    s.sendMessage(Component.text(w.displayName + " [" + w.id + "] – " + status, w.color));
                }
            }
            case "recipe" -> {
                Weapon w = a.length > 1 ? Weapon.byId(a[1]) : null;
                if (w == null) {
                    s.sendMessage(Component.text("Unbekannte Waffe. /legendary list", NamedTextColor.RED));
                    return true;
                }
                s.sendMessage(Component.text("Rezept: " + w.displayName, w.color, TextDecoration.BOLD));
                for (String row : w.shape) {
                    s.sendMessage(Component.text("  [" + row.replace(' ', '_') + "]", NamedTextColor.WHITE));
                }
                w.ingredients.forEach((ch, mat) ->
                        s.sendMessage(Component.text("  " + ch + " = " + mat.name(), NamedTextColor.GRAY)));
            }
            case "give" -> {
                if (!s.hasPermission("legendary.admin")) {
                    s.sendMessage(Component.text("Keine Berechtigung.", NamedTextColor.RED));
                    return true;
                }
                if (a.length < 3) {
                    s.sendMessage(Component.text("/legendary give <spieler> <waffe>", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(a[1]);
                Weapon w = Weapon.byId(a[2]);
                if (target == null || w == null) {
                    s.sendMessage(Component.text("Spieler oder Waffe nicht gefunden.", NamedTextColor.RED));
                    return true;
                }
                target.getInventory().addItem(createItem(w));
                s.sendMessage(Component.text(w.displayName + " an " + target.getName() + " gegeben.", NamedTextColor.GREEN));
            }
            case "reset" -> {
                if (!s.hasPermission("legendary.admin")) {
                    s.sendMessage(Component.text("Keine Berechtigung.", NamedTextColor.RED));
                    return true;
                }
                Weapon w = a.length > 1 ? Weapon.byId(a[1]) : null;
                if (w == null) {
                    s.sendMessage(Component.text("Unbekannte Waffe. /legendary list", NamedTextColor.RED));
                    return true;
                }
                data.set("crafted." + w.id, null);
                saveData();
                s.sendMessage(Component.text(w.displayName + " kann wieder gecraftet werden.", NamedTextColor.GREEN));
            }
            default -> s.sendMessage(Component.text("Unbekannter Unterbefehl.", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (Weapon w : Weapon.values()) ids.add(w.id);

        if (a.length == 1) {
            out.addAll(List.of("menu", "list", "recipe", "give", "reset"));
        } else if (a.length == 2) {
            switch (a[0].toLowerCase()) {
                case "recipe", "reset" -> out.addAll(ids);
                case "give" -> Bukkit.getOnlinePlayers().forEach(pl -> out.add(pl.getName()));
                default -> { }
            }
        } else if (a.length == 3 && a[0].equalsIgnoreCase("give")) {
            out.addAll(ids);
        }
        String prefix = a[a.length - 1].toLowerCase();
        out.removeIf(x -> !x.toLowerCase().startsWith(prefix));
        return out;
    }
}
