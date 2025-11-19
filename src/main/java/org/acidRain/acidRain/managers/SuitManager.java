package org.acidRain.acidRain.managers;

import org.acidRain.acidRain.AcidRain;
import org.acidRain.acidRain.config.ConfigManager;
import org.acidRain.acidRain.registry.item.ProtectionSuit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SuitManager {
    private final AcidRain plugin;
    private final ConfigManager configManager;
    private final LocalizationManager localizationManager;
    private final ProtectionSuit protectionSuit;
    
    private final Map<UUID, Long> suitExpirationTimes = new HashMap<>();
    private final Map<UUID, Boolean> hasFullSuit = new HashMap<>();
    private final Map<UUID, Long> suitPauseTimes = new HashMap<>();

    public SuitManager(AcidRain plugin, ConfigManager configManager, LocalizationManager localizationManager, ProtectionSuit protectionSuit) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.localizationManager = localizationManager;
        this.protectionSuit = protectionSuit;
        loadProtectionTimes();
    }

    public void loadProtectionTimes() {
        ConfigurationSection section = configManager.getConfig().getConfigurationSection("protectionTimes");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long expireTime = section.getLong(key);
                    if (System.currentTimeMillis() < expireTime) {
                        suitExpirationTimes.put(uuid, expireTime);
                    } else {
                        plugin.getLogger().info(localizationManager.getMessage("console.info.remove_expired", uuid.toString()));
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(localizationManager.getMessage("console.info.invalid_uuid", key));
                }
            }
        }
    }

    public void saveProtectionTimes() {
        for (Map.Entry<UUID, Long> entry : suitExpirationTimes.entrySet()) {
            configManager.getConfig().set("protectionTimes." + entry.getKey().toString(), entry.getValue());
        }
        configManager.saveConfig();
    }

    public boolean checkFullSuit(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor == null || armor.length < 4) return false;

        for (ItemStack item : armor) {
            if (item == null || !item.hasItemMeta()) return false;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) return false;

            PersistentDataContainer data = meta.getPersistentDataContainer();
            if (!data.has(new NamespacedKey(plugin, "acidrain_protection"), PersistentDataType.INTEGER)) {
                return false;
            }
        }

        hasFullSuit.put(player.getUniqueId(), true);
        return true;
    }

    public void startProtectionTimer(Player player) {
        UUID playerId = player.getUniqueId();
        long duration = configManager.getConfig().getInt("protectionSuit.duration", 60) * 60000L;
        
        // Всегда начинаем таймер заново при надевании нового костюма
        suitExpirationTimes.put(playerId, System.currentTimeMillis() + duration);
        hasFullSuit.put(playerId, true);
        suitPauseTimes.remove(playerId);
        updateArmorLore(player);

        clearRadiationEffects(player);

        player.sendMessage(ChatColor.GREEN + localizationManager.getMessage("player.success.suit_activated", 
                configManager.getConfig().getInt("protectionSuit.duration", 60)));
        player.sendMessage(ChatColor.GREEN + localizationManager.getMessage("player.success.radiation_cleared"));
    }

    public void pauseProtectionTimer(Player player) {
        UUID playerId = player.getUniqueId();
        hasFullSuit.put(playerId, false);
        suitPauseTimes.put(playerId, System.currentTimeMillis());
        player.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("player.success.suit_paused"));
    }

    public void resumeProtectionTimer(Player player) {
        UUID playerId = player.getUniqueId();
        Long pauseTime = suitPauseTimes.get(playerId);
        
        if (pauseTime != null) {
            long pauseDuration = System.currentTimeMillis() - pauseTime;
            Long currentExpireTime = suitExpirationTimes.get(playerId);
            if (currentExpireTime != null) {
                suitExpirationTimes.put(playerId, currentExpireTime + pauseDuration);
            }
            suitPauseTimes.remove(playerId);
        }
        
        hasFullSuit.put(playerId, true);
        updateArmorLore(player);
        clearRadiationEffects(player);
        
        player.sendMessage(ChatColor.GREEN + localizationManager.getMessage("player.success.suit_activated_short"));
        player.sendMessage(ChatColor.GREEN + localizationManager.getMessage("player.success.radiation_cleared"));
    }

    public void damageSuit(Player player) {
        UUID playerId = player.getUniqueId();

        // Проверяем что таймер истек
        if (!suitExpirationTimes.containsKey(playerId)) {
            return;
        }
        
        long expireTime = suitExpirationTimes.get(playerId);
        if (System.currentTimeMillis() < expireTime) {
            return; // Таймер еще не истек
        }

        // Ломаем всю броню с защитой
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor != null) {
            for (ItemStack item : armor) {
                if (item != null && item.hasItemMeta()) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().has(
                            new NamespacedKey(plugin, "acidrain_protection"),
                            PersistentDataType.INTEGER)) {
                        item.setAmount(0);
                    }
                }
            }
            // Обновляем слоты брони
            player.getInventory().setArmorContents(armor);
        }

        // Удаляем данные о костюме
        suitExpirationTimes.remove(playerId);
        hasFullSuit.remove(playerId);
        suitPauseTimes.remove(playerId);
        
        if (player.getWorld() != null) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }
        player.sendMessage(ChatColor.RED + localizationManager.getMessage("player.success.suit_destroyed"));
    }

    public void updateArmorLore(Player player) {
        UUID playerId = player.getUniqueId();
        Long expireTime = suitExpirationTimes.get(playerId);

        if (expireTime == null) {
            return;
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor == null) return;

        for (ItemStack item : armor) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer data = meta.getPersistentDataContainer();

                    if (data.has(new NamespacedKey(plugin, "acidrain_protection"), PersistentDataType.INTEGER)) {
                        protectionSuit.updateLore(item, expireTime);
                    }
                }
            }
        }
    }

    public void giveProtectionSuit(Player player) {
        protectionSuit.giveFullSuit(player);
    }

    public String getRemainingTimeForDisplay(long expireTime) {
        long remaining = expireTime - System.currentTimeMillis();
        if (remaining <= 0) return localizationManager.getMessage("player.info.suit_expired");
        long minutes = remaining / 60000;
        long seconds = (remaining % 60000) / 1000;
        return String.format("%d мин. %d сек.", minutes, seconds);
    }

    public void clearRadiationEffects(Player player) {
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.POISON);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.WITHER);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.HUNGER);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.NAUSEA);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.MINING_FATIGUE);
    }

    public boolean hasFullSuit(UUID playerId) {
        return hasFullSuit.getOrDefault(playerId, false);
    }

    public boolean hasActiveProtection(UUID playerId) {
        // Проверяем что таймер активен и не истек
        if (!suitExpirationTimes.containsKey(playerId)) {
            return false;
        }
        
        long expireTime = suitExpirationTimes.get(playerId);
        if (System.currentTimeMillis() >= expireTime) {
            return false; // Таймер истек
        }
        
        if (suitPauseTimes.containsKey(playerId)) {
            return false; // Таймер на паузе
        }
        
        // Проверяем что костюм действительно надет
        return hasFullSuit.getOrDefault(playerId, false);
    }

    public Long getExpirationTime(UUID playerId) {
        return suitExpirationTimes.get(playerId);
    }

    public boolean isPaused(UUID playerId) {
        return suitPauseTimes.containsKey(playerId);
    }

    public void removeSuitData(UUID playerId) {
        suitExpirationTimes.remove(playerId);
        hasFullSuit.remove(playerId);
        suitPauseTimes.remove(playerId);
    }
}

