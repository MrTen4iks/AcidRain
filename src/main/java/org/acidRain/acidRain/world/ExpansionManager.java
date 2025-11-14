package org.acidRain.acidRain.world;

import org.acidRain.acidRain.AcidRain;
import org.acidRain.acidRain.config.ConfigManager;
import org.acidRain.acidRain.managers.LocalizationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

public class ExpansionManager {
    private final AcidRain plugin;
    private final ConfigManager configManager;
    private final ZoneManager zoneManager;
    private final LocalizationManager localizationManager;
    
    private boolean isExpanding = false;
    private ScheduledTask expansionTask;
    private ScheduledTask autoExpandTask;

    public ExpansionManager(AcidRain plugin, ConfigManager configManager, ZoneManager zoneManager, LocalizationManager localizationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.zoneManager = zoneManager;
        this.localizationManager = localizationManager;
        startAutoExpandTask();
    }

    public void startAutoExpandTask() {
        autoExpandTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            if (!configManager.getConfig().getBoolean("autoExpand.enabled", true)) return;

            Calendar moscowTime = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
            if (moscowTime.get(Calendar.HOUR_OF_DAY) == configManager.getConfig().getInt("autoExpand.triggerHour", 14) &&
                    moscowTime.get(Calendar.MINUTE) == 0) {
                triggerAutoExpand();
            }
        }, 1L, 20L * 60);
    }

    private void triggerAutoExpand() {
        Random random = new Random();
        int blocks = configManager.getConfig().getInt("autoExpand.minBlocks", 50) +
                random.nextInt(configManager.getConfig().getInt("autoExpand.maxBlocks", 80) -
                        configManager.getConfig().getInt("autoExpand.minBlocks", 50) + 1);

        int seconds = configManager.getConfig().getInt("autoExpand.minTime", 300) +
                random.nextInt(configManager.getConfig().getInt("autoExpand.maxTime", 600) -
                        configManager.getConfig().getInt("autoExpand.minTime", 300) + 1);

        if (!isExpanding) {
            startExpansion(blocks, seconds);
            broadcastExpansionMessage(blocks, seconds);
        }
    }

    private void broadcastExpansionMessage(int blocks, int seconds) {
        String message = ChatColor.translateAlternateColorCodes('&',
                localizationManager.getMessage("notifications.expand_message", blocks, seconds/60));

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("acidrain.notify"))
                .forEach(p -> p.sendMessage(message));
    }

    public void startExpansion(int blocks, int seconds) {
        isExpanding = true;
        final int currentRadius = zoneManager.getZone1Radius();
        final int currentSafeZone = zoneManager.getDangerZoneStart();
        final int target = currentRadius + blocks;
        final int targetSafeZone = currentSafeZone + blocks; // Safe zone тоже расширяется
        // Инкремент на секунду: если нужно расширить на blocks за seconds секунд
        // Задача выполняется каждую секунду (20 тиков), поэтому делим blocks на seconds
        final double increment = (double) blocks / seconds;

        expansionTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            int current = zoneManager.getZone1Radius();
            int currentSafe = zoneManager.getDangerZoneStart();
            double newValue = current + increment;
            double newSafeValue = currentSafe + increment;
            int newRadius = (int) newValue;
            int newSafeZone = (int) newSafeValue;
            
            // Обновляем радиус зоны 1 и безопасной зоны без сохранения конфига (сохраним в конце)
            zoneManager.setZone1RadiusWithoutSave(newRadius);
            zoneManager.setDangerZoneStartWithoutSave(newSafeZone);

            if (newRadius >= target) {
                finishExpansion(target, targetSafeZone);
                task.cancel();
            }
        }, 1L, 20L); // Выполняется каждую секунду (20 тиков)
    }

    private void finishExpansion(int target, int targetSafeZone) {
        // Устанавливаем финальное значение и сохраняем конфиг
        zoneManager.setZone1Radius(target);
        zoneManager.setDangerZoneStart(targetSafeZone);
        isExpanding = false;
        if (expansionTask != null) {
            expansionTask.cancel();
        }
        plugin.getLogger().info("Расширение завершено. Новый радиус зоны 1: " + target + ", новая безопасная зона: " + targetSafeZone);
    }

    public void cancelTasks() {
        if (expansionTask != null) expansionTask.cancel();
        if (autoExpandTask != null) autoExpandTask.cancel();
    }

    public boolean isExpanding() {
        return isExpanding;
    }

    public ScheduledTask getExpansionTask() {
        return expansionTask;
    }

    public ScheduledTask getAutoExpandTask() {
        return autoExpandTask;
    }
}

