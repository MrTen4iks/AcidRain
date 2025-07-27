package org.acidRain.acidRain;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.block.Block;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.*;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import java.io.*;
import java.util.*;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class AcidRain extends JavaPlugin implements Listener, CommandExecutor {

    private int dangerZoneStart;
    private int zone1Radius = 300;
    private int zone2Radius = 750;
    private int zone3Radius = 1500;
    private int zone4Radius = 3000;
    private boolean acidRainEnabled;
    private boolean isExpanding;
    private FileConfiguration config;
    private File configFile;
    private final Map<UUID, Long> lastEffectTime = new HashMap<>();
    private final Map<UUID, Long> lastBorderNotify = new HashMap<>();
    private final Map<UUID, Integer> currentZone = new HashMap<>();
    private final Map<UUID, Long> suitExpirationTimes = new HashMap<>();
    private final Map<UUID, Boolean> hasFullSuit = new HashMap<>();
    private BukkitTask expansionTask;
    private BukkitTask autoExpandTask;

    @Override
    public void onEnable() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadConfig();
        loadProtectionTimes();
        calculateZoneRadii();

        registerCommands();
        registerRecipes();
        getServer().getPluginManager().registerEvents(this, this);
        startParticleTask();
        startAutoExpandTask();

        new BukkitRunnable() {
            @Override
            public void run() {
                updateSuitTimers();
            }
        }.runTaskTimer(this, 0L, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkSuitAndEffects(player);
                }
            }
        }.runTaskTimer(this, 0L, 20L);

        getLogger().info(ChatColor.GREEN + "AcidRain включен! Граница: " + dangerZoneStart + " блоков");
    }

    @Override
    public void onDisable() {
        saveProtectionTimes();
        cancelTasks();
        savePluginConfig();
        getLogger().info("AcidRain выключен");
    }

    private void saveProtectionTimes() {
        for (Map.Entry<UUID, Long> entry : suitExpirationTimes.entrySet()) {
            config.set("protectionTimes." + entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().warning("Ошибка сохранения времени защиты: " + e.getMessage());
        }
    }

    private void loadProtectionTimes() {
        ConfigurationSection section = config.getConfigurationSection("protectionTimes");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                long expireTime = section.getLong(key);
                suitExpirationTimes.put(uuid, expireTime);
            }
        }
    }

    private void loadConfig() {
        dangerZoneStart = config.getInt("dangerZoneStart", 1100);
        zone1Radius = config.getInt("zones.zone1.radius", 300);
        acidRainEnabled = config.getBoolean("acidRainEnabled", true);
    }

    private void calculateZoneRadii() {
        zone2Radius = (int)(zone1Radius * 2.5);
        zone3Radius = (int)(zone1Radius * 5.0);
        zone4Radius = (int)(zone1Radius * 10.0);
    }

    private void registerCommands() {
        String[] commands = {"acidrain", "aon", "aoff", "aset", "aexpand", "astatus", "asuit"};
        for (String cmd : commands) {
            PluginCommand command = getCommand(cmd);
            if (command != null) {
                command.setExecutor(this);
            }
        }
    }

    private void registerRecipes() {
        ShapedRecipe helmetRecipe = new ShapedRecipe(new NamespacedKey(this, "acidrain_helmet"),
                createArmorPiece(Material.LEATHER_HELMET, "Кислотостойкий шлем"));
        helmetRecipe.shape("LLL", "L L");
        helmetRecipe.setIngredient('L', Material.LEATHER);
        getServer().addRecipe(helmetRecipe);

        ShapedRecipe chestplateRecipe = new ShapedRecipe(new NamespacedKey(this, "acidrain_chestplate"),
                createArmorPiece(Material.LEATHER_CHESTPLATE, "Кислотостойкая кираса"));
        chestplateRecipe.shape("L L", "LLL", "LLL");
        chestplateRecipe.setIngredient('L', Material.LEATHER);
        getServer().addRecipe(chestplateRecipe);

        ShapedRecipe leggingsRecipe = new ShapedRecipe(new NamespacedKey(this, "acidrain_leggings"),
                createArmorPiece(Material.LEATHER_LEGGINGS, "Кислотостойкие поножи"));
        leggingsRecipe.shape("LLL", "L L", "L L");
        leggingsRecipe.setIngredient('L', Material.LEATHER);
        getServer().addRecipe(leggingsRecipe);

        ShapedRecipe bootsRecipe = new ShapedRecipe(new NamespacedKey(this, "acidrain_boots"),
                createArmorPiece(Material.LEATHER_BOOTS, "Кислотостойкие ботинки"));
        bootsRecipe.shape("L L", "L L");
        bootsRecipe.setIngredient('L', Material.LEATHER);
        getServer().addRecipe(bootsRecipe);

        ShapedRecipe fullSuitRecipe = new ShapedRecipe(
                new NamespacedKey(this, "acidrain_full_suit"),
                createFullProtectionSuit()
        );

        fullSuitRecipe.shape("NBT", "VEV", "LBL");
        fullSuitRecipe.setIngredient('N', Material.NETHER_STAR);
        fullSuitRecipe.setIngredient('B', Material.BREEZE_ROD);
        fullSuitRecipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        fullSuitRecipe.setIngredient('V', Material.LEATHER_HELMET);
        fullSuitRecipe.setIngredient('E', Material.LEATHER_CHESTPLATE);
        fullSuitRecipe.setIngredient('L', Material.LEATHER_BOOTS);

        getServer().addRecipe(fullSuitRecipe);
    }

    private ItemStack createFullProtectionSuit() {
        ItemStack resultItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = resultItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Набор защиты от кислотных дождей");

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(new NamespacedKey(this, "full_protection_set"), PersistentDataType.INTEGER, 1);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "ПКМ - получить полный комплект брони");
        lore.add(ChatColor.GRAY + "Защищает от всех эффектов кислотных дождей");
        meta.setLore(lore);

        resultItem.setItemMeta(meta);
        return resultItem;
    }

    private void startParticleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!acidRainEnabled) return;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    try {
                        handleParticles(player);
                    } catch (Exception e) {
                        getLogger().warning("Ошибка обработки игрока " + player.getName() + ": " + e.getMessage());
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void startAutoExpandTask() {
        autoExpandTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!config.getBoolean("autoExpand.enabled", true)) return;

                Calendar moscowTime = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
                if (moscowTime.get(Calendar.HOUR_OF_DAY) == config.getInt("autoExpand.triggerHour", 14) &&
                        moscowTime.get(Calendar.MINUTE) == 0) {

                    triggerAutoExpand();
                }
            }
        }.runTaskTimer(this, 0L, 20L * 60);
    }

    private void triggerAutoExpand() {
        Random random = new Random();
        int blocks = config.getInt("autoExpand.minBlocks", 50) +
                random.nextInt(config.getInt("autoExpand.maxBlocks", 80) -
                        config.getInt("autoExpand.minBlocks", 50) + 1);

        int seconds = config.getInt("autoExpand.minTime", 300) +
                random.nextInt(config.getInt("autoExpand.maxTime", 600) -
                        config.getInt("autoExpand.minTime", 300) + 1);

        if (!isExpanding) {
            startExpansion(blocks, seconds);
            broadcastExpansionMessage(blocks, seconds);
        }
    }

    private void broadcastExpansionMessage(int blocks, int seconds) {
        String message = ChatColor.translateAlternateColorCodes('&',
                config.getString("notifications.expandMessage",
                                "&6[AcidRain] &cГраница расширяется на &e{blocks} &cблоков за &e{minutes} &cминут!")
                        .replace("{blocks}", String.valueOf(blocks))
                        .replace("{minutes}", String.valueOf(seconds/60)));

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("acidrain.notify"))
                .forEach(p -> p.sendMessage(message));
    }

    private void startExpansion(int blocks, int seconds) {
        isExpanding = true;
        final int target = zone1Radius + blocks;
        final double increment = (double) blocks / (seconds * 20);

        expansionTask = new BukkitRunnable() {
            double current = zone1Radius;

            @Override
            public void run() {
                current += increment;
                zone1Radius = (int) current;
                calculateZoneRadii();

                if (current >= target) {
                    finishExpansion(target);
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void finishExpansion(int target) {
        zone1Radius = target;
        isExpanding = false;
        if (expansionTask != null) {
            expansionTask.cancel();
        }
        savePluginConfig();
    }

    private void cancelTasks() {
        if (expansionTask != null) expansionTask.cancel();
        if (autoExpandTask != null) autoExpandTask.cancel();
    }

    private void savePluginConfig() {
        try {
            config.set("dangerZoneStart", dangerZoneStart);
            config.set("zones.zone1.radius", zone1Radius);
            config.set("acidRainEnabled", acidRainEnabled);
            config.save(configFile);
        } catch (IOException e) {
            getLogger().warning("Ошибка сохранения конфига: " + e.getMessage());
        }
    }

    private int getZoneForLocation(Location loc) {
        if (loc == null) return 0;
        int distance = Math.max(Math.abs(loc.getBlockX()), Math.abs(loc.getBlockZ()));

        if (distance <= dangerZoneStart) return 0;
        if (distance <= dangerZoneStart + zone1Radius) return 1;
        if (distance <= dangerZoneStart + zone2Radius) return 2;
        if (distance <= dangerZoneStart + zone3Radius) return 3;
        return 4;
    }

    private void handleParticles(Player player) {
        int zone = getZoneForLocation(player.getLocation());
        if (zone > 0 && isUnderOpenSky(player.getLocation())) {
            spawnZoneParticles(player, zone);
        }
    }

    private boolean isUnderOpenSky(Location loc) {
        if (loc == null) return false;
        for (int y = loc.getBlockY() + 1; y < 256; y++) {
            Block block = loc.getWorld().getBlockAt(loc.getBlockX(), y, loc.getBlockZ());
            if (!block.isPassable()) {
                return false;
            }
        }
        return true;
    }

    private void spawnZoneParticles(Player player, int zone) {
        Location loc = player.getLocation();
        World world = player.getWorld();

        if (hasFullSuit.getOrDefault(player.getUniqueId(), false) &&
                suitExpirationTimes.containsKey(player.getUniqueId()) &&
                System.currentTimeMillis() < suitExpirationTimes.get(player.getUniqueId())) {
            spawnProtectionParticles(player);
            return;
        }

        switch (zone) {
            case 1:
                world.spawnParticle(Particle.FALLING_WATER, loc, 50, 2, 5, 2, 0.1);
                world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 20, 1, 1, 1);
                break;
            case 2:
                world.spawnParticle(Particle.FALLING_LAVA, loc, 40, 2, 5, 2, 0.1);
                world.spawnParticle(Particle.DRIPPING_LAVA, loc, 30, 0.5, 0.5, 0.5);
                world.spawnParticle(Particle.SMOKE, loc, 20, 1, 1, 1);
                break;
            case 3:
                world.spawnParticle(Particle.LAVA, loc, 30, 1, 1, 1);
                world.spawnParticle(Particle.LARGE_SMOKE, loc, 25, 0.5, 0.5, 0.5);
                world.spawnParticle(Particle.ITEM_SLIME, loc, 20, 1, 1, 1, new Particle.DustOptions(Color.RED, 1));
                break;
            case 4:
                world.spawnParticle(Particle.FLAME, loc, 50, 0.5, 0.5, 0.5);
                world.spawnParticle(Particle.LARGE_SMOKE, loc, 40, 1, 1, 1);
                world.spawnParticle(Particle.SQUID_INK, loc, 30, 1, 1, 1);
                break;
        }
    }

    private void spawnProtectionParticles(Player player) {
        // Альтернативный вариант с DustOptions
        player.getWorld().spawnParticle(
                Particle.DUST,
                player.getLocation().add(0, 2, 0),
                15,
                0.5, 0.5, 0.5,
                0.1,
                new Particle.DustOptions(Color.LIME, 1)
        );

        for (int i = 0; i < 360; i += 30) {
            double angle = Math.toRadians(i);
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            player.getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    player.getLocation().add(x, 1.5, z),
                    1,
                    0, 0, 0,
                    0
            );
        }
    }

    private void updateArmorLore(Player player) {
        UUID playerId = player.getUniqueId();
        Long expireTime = suitExpirationTimes.get(playerId);

        // Проверяем, есть ли запись о времени истечения защиты
        if (expireTime == null) {
            return;
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor == null) return;

        for (ItemStack item : armor) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                PersistentDataContainer data = meta.getPersistentDataContainer();

                if (data.has(new NamespacedKey(this, "acidrain_protection"), PersistentDataType.INTEGER)) {
                    List<String> lore = meta.getLore();
                    if (lore != null && lore.size() >= 3) {
                        lore.set(2, ChatColor.YELLOW + "Осталось: " + getRemainingTimeForDisplay(expireTime));
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                }
            }
        }
    }

    private void checkSuitAndEffects(Player player) {
        boolean wearingFullSuit = checkFullSuit(player);
        UUID playerId = player.getUniqueId();

        if (!wearingFullSuit && hasFullSuit.getOrDefault(playerId, false)) {
            // Игрок снял костюм
            suitExpirationTimes.remove(playerId);
            hasFullSuit.put(playerId, false);
            return;
        }

        if (wearingFullSuit) {
            Long expireTime = suitExpirationTimes.get(playerId);

            if (expireTime == null) {
                startProtectionTimer(player);
                return;
            }

            if (System.currentTimeMillis() >= expireTime) {
                damageSuit(player);
                return;
            }

            updateArmorLore(player);
        }

        int zone = getZoneForLocation(player.getLocation());
        if (zone > 0) {
            boolean protectionActive = wearingFullSuit &&
                    suitExpirationTimes.containsKey(playerId) &&
                    System.currentTimeMillis() < suitExpirationTimes.get(playerId);

            if (protectionActive) {
                spawnProtectionParticles(player);
            } else {
                applyZoneEffects(player, zone);
            }
        }
    }

    private boolean checkFullSuit(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor == null || armor.length < 4) return false;

        // Проверяем каждый элемент брони
        for (ItemStack item : armor) {
            if (item == null || !item.hasItemMeta()) return false;

            PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
            if (!data.has(new NamespacedKey(this, "acidrain_protection"), PersistentDataType.INTEGER)) {
                return false;
            }
        }

        // Если все проверки пройдены - сохраняем состояние
        hasFullSuit.put(player.getUniqueId(), true);
        return true;
    }

    private void updateSuitTimers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();

            // Проверяем только игроков с активной защитой
            if (hasFullSuit.getOrDefault(playerId, false) &&
                    suitExpirationTimes.containsKey(playerId)) {

                long expireTime = suitExpirationTimes.get(playerId);
                if (System.currentTimeMillis() >= expireTime) {
                    damageSuit(player);
                } else {
                    updateArmorLore(player);
                }
            }
        }
    }

    private void startProtectionTimer(Player player) {
        long duration = config.getInt("protectionSuit.duration", 60) * 60000L;
        suitExpirationTimes.put(player.getUniqueId(), System.currentTimeMillis() + duration);
        hasFullSuit.put(player.getUniqueId(), true); // Явно устанавливаем флаг
        updateArmorLore(player);

        // Уведомление игрока
        player.sendMessage(ChatColor.GREEN + "Защитный костюм активирован на " +
                config.getInt("protectionSuit.duration", 60) + " минут");
    }

    private void damageSuit(Player player) {
        UUID playerId = player.getUniqueId();

        // Удаляем только если время действительно истекло
        if (suitExpirationTimes.containsKey(playerId) &&
                System.currentTimeMillis() >= suitExpirationTimes.get(playerId)) {

            for (ItemStack item : player.getInventory().getArmorContents()) {
                if (item != null && item.hasItemMeta() &&
                        item.getItemMeta().getPersistentDataContainer().has(
                                new NamespacedKey(this, "acidrain_protection"),
                                PersistentDataType.INTEGER)) {
                    item.setAmount(0);
                }
            }

            suitExpirationTimes.remove(playerId);
            hasFullSuit.remove(playerId);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "Ваш защитный костюм разрушен кислотным дождем!");
        }
    }

    private String getRemainingTimeForDisplay(long expireTime) {
        long remaining = expireTime - System.currentTimeMillis();
        if (remaining <= 0) return "Истекло";
        long minutes = remaining / 60000;
        long seconds = (remaining % 60000) / 1000;
        return String.format("%d мин. %d сек.", minutes, seconds);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Player player = (Player) event.getWhoClicked();
                    boolean wasWearing = hasFullSuit.getOrDefault(player.getUniqueId(), false);
                    boolean nowWearing = checkFullSuit(player);

                    if (nowWearing && !wasWearing) {
                        if (!suitExpirationTimes.containsKey(player.getUniqueId())) {
                            startProtectionTimer(player);
                        }
                    } else if (!nowWearing && wasWearing) {
                        suitExpirationTimes.remove(player.getUniqueId());
                    }
                }
            }.runTaskLater(this, 1L);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.hasItemMeta()) {
                PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();

                if (data.has(new NamespacedKey(this, "full_protection_set"), PersistentDataType.INTEGER)) {
                    event.setCancelled(true);

                    Player player = event.getPlayer();
                    if (player.getInventory().firstEmpty() == -1) {
                        player.sendMessage(ChatColor.RED + "Недостаточно места в инвентаре!");
                        return;
                    }

                    ItemStack helmet = createArmorPiece(Material.LEATHER_HELMET, "Кислотостойкий шлем");
                    ItemStack chestplate = createArmorPiece(Material.LEATHER_CHESTPLATE, "Кислотостойкая кираса");
                    ItemStack leggings = createArmorPiece(Material.LEATHER_LEGGINGS, "Кислотостойкие поножи");
                    ItemStack boots = createArmorPiece(Material.LEATHER_BOOTS, "Кислотостойкие ботинки");

                    String[] colorParts = config.getString("protectionSuit.color", "50,200,50").split(",");
                    Color color = Color.fromRGB(
                            Integer.parseInt(colorParts[0].trim()),
                            Integer.parseInt(colorParts[1].trim()),
                            Integer.parseInt(colorParts[2].trim())
                    );

                    setArmorColor(helmet, color);
                    setArmorColor(chestplate, color);
                    setArmorColor(leggings, color);
                    setArmorColor(boots, color);

                    player.getInventory().addItem(helmet, chestplate, leggings, boots);
                    item.setAmount(item.getAmount() - 1);
                    player.sendMessage(ChatColor.GREEN + "Вы получили полный комплект защитной брони!");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (suitExpirationTimes.containsKey(player.getUniqueId())) {
            if (System.currentTimeMillis() >= suitExpirationTimes.get(player.getUniqueId())) {
                suitExpirationTimes.remove(player.getUniqueId());
                hasFullSuit.remove(player.getUniqueId());
            } else {
                updateArmorLore(player);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!acidRainEnabled) return;

        Location to = event.getTo();
        if (to == null || event.getFrom().getBlock().equals(to.getBlock())) return;

        Player player = event.getPlayer();
        int zone = getZoneForLocation(player.getLocation());
        Integer oldZone = currentZone.get(player.getUniqueId());

        if (oldZone == null || oldZone != zone) {
            currentZone.put(player.getUniqueId(), zone);
            if (zone == 0) {
                player.sendActionBar(ChatColor.GREEN + "■ Безопасная зона ■");
                clearEffects(player);
            } else {
                player.sendActionBar(ChatColor.RED + "■ Кислотная зона ■ Уровень " + zone);
            }
        }
    }

    private void applyZoneEffects(Player player, int zone) {
        long now = System.currentTimeMillis();
        if (now - lastEffectTime.getOrDefault(player.getUniqueId(), 0L) < 20000) {
            return;
        }

        clearEffects(player);

        switch (zone) {
            case 1:
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 0));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                break;
            case 2:
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
                break;
            case 3:
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 3));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.6f);
                break;
            case 4:
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 9));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 9));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 9));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 9));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
                break;
        }

        player.damage(zone * 0.5);
        lastEffectTime.put(player.getUniqueId(), now);
    }

    private void clearEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private void giveProtectionSuit(Player player) {
        ItemStack helmet = createArmorPiece(Material.LEATHER_HELMET, "Кислотостойкий шлем");
        ItemStack chestplate = createArmorPiece(Material.LEATHER_CHESTPLATE, "Кислотостойкая кираса");
        ItemStack leggings = createArmorPiece(Material.LEATHER_LEGGINGS, "Кислотостойкие поножи");
        ItemStack boots = createArmorPiece(Material.LEATHER_BOOTS, "Кислотостойкие ботинки");

        String[] colorParts = config.getString("protectionSuit.color", "50,200,50").split(",");
        Color color = Color.fromRGB(
                Integer.parseInt(colorParts[0].trim()),
                Integer.parseInt(colorParts[1].trim()),
                Integer.parseInt(colorParts[2].trim())
        );

        setArmorColor(helmet, color);
        setArmorColor(chestplate, color);
        setArmorColor(leggings, color);
        setArmorColor(boots, color);

        player.getInventory().addItem(helmet, chestplate, leggings, boots);
    }

    private ItemStack createArmorPiece(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + name);

        meta.getPersistentDataContainer().set(
                new NamespacedKey(this, "acidrain_protection"),
                PersistentDataType.INTEGER,
                1
        );

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Защищает от кислотных дождей");
        lore.add(ChatColor.GRAY + "Длительность: " + config.getInt("protectionSuit.duration", 60) + " минут");
        lore.add(ChatColor.YELLOW + "Осталось: " + getRemainingTimeForDisplay(
                System.currentTimeMillis() + config.getInt("protectionSuit.duration", 60) * 60000L));
        meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);

        item.setItemMeta(meta);
        return item;
    }

    private void setArmorColor(ItemStack item, Color color) {
        if (item.getItemMeta() instanceof LeatherArmorMeta) {
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            meta.setColor(color);
            item.setItemMeta(meta);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String command = cmd.getName().toLowerCase();

        if (command.equals("asuit")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Эта команда только для игроков!");
                return true;
            }
            if (!sender.hasPermission("acidrain.admin")) {
                sender.sendMessage(ChatColor.RED + "Недостаточно прав!");
                return true;
            }
            giveProtectionSuit((Player)sender);
            return true;
        }

        if (command.equals("aon")) {
            if (!sender.hasPermission("acidrain.admin")) {
                sender.sendMessage(ChatColor.RED + "Недостаточно прав!");
                return true;
            }
            acidRainEnabled = true;
            sender.sendMessage(ChatColor.GREEN + "Кислотные дожди включены!");
            return true;
        }

        if (command.equals("aoff")) {
            if (!sender.hasPermission("acidrain.admin")) {
                sender.sendMessage(ChatColor.RED + "Недостаточно прав!");
                return true;
            }
            acidRainEnabled = false;
            sender.sendMessage(ChatColor.GREEN + "Кислотные дожди выключены!");
            return true;
        }

        if (command.equals("astatus")) {
            String status = acidRainEnabled ? ChatColor.GREEN + "Включены" : ChatColor.RED + "Выключены";
            sender.sendMessage(ChatColor.GOLD + "Статус кислотных дождей: " + status);
            sender.sendMessage(ChatColor.GOLD + "Текущая граница: " + ChatColor.GREEN + dangerZoneStart + " блоков");
            return true;
        }

        if (command.equals("aexpand")) {
            if (!sender.hasPermission("acidrain.admin")) {
                sender.sendMessage(ChatColor.RED + "Недостаточно прав!");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Использование: /aexpand <блоки> <секунды>");
                return true;
            }
            try {
                int blocks = Integer.parseInt(args[0]);
                int seconds = Integer.parseInt(args[1]);
                startExpansion(blocks, seconds);
                sender.sendMessage(ChatColor.GREEN + "Расширение начато: +" + blocks + " блоков за " + seconds + " секунд");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Некорректные числовые значения!");
            }
            return true;
        }

        if (command.equals("aset")) {
            if (!sender.hasPermission("acidrain.admin")) {
                sender.sendMessage(ChatColor.RED + "Недостаточно прав!");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Использование: /aset <блоки>");
                return true;
            }
            try {
                dangerZoneStart = Integer.parseInt(args[0]);
                savePluginConfig();
                sender.sendMessage(ChatColor.GREEN + "Граница установлена на " + dangerZoneStart + " блоков");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Некорректное числовое значение!");
            }
            return true;
        }

        if (command.equals("acidrain")) {
            sender.sendMessage(ChatColor.GOLD + "===== AcidRain v1.0 =====");
            sender.sendMessage(ChatColor.YELLOW + "/asuit - Получить защитный костюм");
            sender.sendMessage(ChatColor.YELLOW + "/aon - Включить кислотные дожди");
            sender.sendMessage(ChatColor.YELLOW + "/aoff - Выключить кислотные дожди");
            sender.sendMessage(ChatColor.YELLOW + "/astatus - Статус системы");
            sender.sendMessage(ChatColor.YELLOW + "/aexpand <блоки> <секунды> - Расширить зону");
            sender.sendMessage(ChatColor.YELLOW + "/aset <блоки> - Установить границу");
            return true;
        }

        return false;
    }
}