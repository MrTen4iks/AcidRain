package org.acidRain.acidRain.world;

import org.acidRain.acidRain.AcidRain;
import org.acidRain.acidRain.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.World;

public class ZoneManager {
    private final AcidRain plugin;
    private final ConfigManager configManager;
    
    private int dangerZoneStart;
    private int zone1Radius = 300;
    private int zone2Radius = 750;
    private int zone3Radius = 1500;
    private int zone4Radius = 3000;

    public ZoneManager(AcidRain plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        loadConfig();
        calculateZoneRadii();
    }

    public void loadConfig() {
        dangerZoneStart = configManager.getConfig().getInt("dangerZoneStart", 1100);
        zone1Radius = configManager.getConfig().getInt("zones.zone1.radius", 300);
        // Загружаем радиусы зон из конфига, если они заданы, иначе рассчитываем автоматически
        if (configManager.getConfig().contains("zones.zone2.radius")) {
            zone2Radius = configManager.getConfig().getInt("zones.zone2.radius");
        } else {
            zone2Radius = (int)(zone1Radius * 2.5);
        }
        if (configManager.getConfig().contains("zones.zone3.radius")) {
            zone3Radius = configManager.getConfig().getInt("zones.zone3.radius");
        } else {
            zone3Radius = (int)(zone1Radius * 5.0);
        }
        if (configManager.getConfig().contains("zones.zone4.radius")) {
            zone4Radius = configManager.getConfig().getInt("zones.zone4.radius");
        } else {
            zone4Radius = (int)(zone1Radius * 10.0);
        }
    }

    public void calculateZoneRadii() {
        // Пересчитываем только если радиусы не заданы в конфиге
        if (!configManager.getConfig().contains("zones.zone2.radius")) {
            zone2Radius = (int)(zone1Radius * 2.5);
        }
        if (!configManager.getConfig().contains("zones.zone3.radius")) {
            zone3Radius = (int)(zone1Radius * 5.0);
        }
        if (!configManager.getConfig().contains("zones.zone4.radius")) {
            zone4Radius = (int)(zone1Radius * 10.0);
        }
    }

    public int getZoneForLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return 0;
        
        // Зоны работают одинаково во всех мирах (обычный мир, ад, энд)
        // Радиусы зон указывают начало каждой зоны от безопасной зоны
        int distance = Math.max(Math.abs(loc.getBlockX()), Math.abs(loc.getBlockZ()));
        int safeZone = getDangerZoneStartForWorld(loc.getWorld());

        if (distance < safeZone) return 0; // Безопасная зона
        if (distance < safeZone + zone1Radius) return 0; // Еще безопасная зона до начала зоны 1
        if (distance < safeZone + zone2Radius) return 1; // Зона 1
        if (distance < safeZone + zone3Radius) return 2; // Зона 2
        if (distance < safeZone + zone4Radius) return 3; // Зона 3
        return 4; // Зона 4
    }

    public boolean isUnderOpenSky(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        for (int y = loc.getBlockY() + 1; y < 256; y++) {
            if (!loc.getWorld().getBlockAt(loc.getBlockX(), y, loc.getBlockZ()).isPassable()) {
                return false;
            }
        }
        return true;
    }

    public int getDangerZoneStart() {
        return dangerZoneStart;
    }

    /**
     * Получает границу безопасной зоны с учетом типа мира
     * Во всех мирах (обычный мир, ад, энд) безопасная зона одинаковая
     * @param world мир для проверки
     * @return граница безопасной зоны для данного мира
     */
    public int getDangerZoneStartForWorld(World world) {
        // Во всех мирах безопасная зона одинаковая
        return dangerZoneStart;
    }

    public void setDangerZoneStart(int dangerZoneStart) {
        this.dangerZoneStart = dangerZoneStart;
        saveConfig();
    }

    /**
     * Устанавливает границу безопасной зоны без сохранения конфига (для использования во время расширения)
     * @param dangerZoneStart новая граница безопасной зоны
     */
    public void setDangerZoneStartWithoutSave(int dangerZoneStart) {
        this.dangerZoneStart = dangerZoneStart;
        // Не сохраняем конфиг, чтобы не перегружать диск во время расширения
    }

    public int getZone1Radius() {
        return zone1Radius;
    }

    public void setZone1Radius(int zone1Radius) {
        this.zone1Radius = zone1Radius;
        calculateZoneRadii();
        saveConfig();
    }

    /**
     * Устанавливает радиус зоны 1 без сохранения конфига (для использования во время расширения)
     * @param zone1Radius новый радиус зоны 1
     */
    public void setZone1RadiusWithoutSave(int zone1Radius) {
        this.zone1Radius = zone1Radius;
        calculateZoneRadii();
        // Не сохраняем конфиг, чтобы не перегружать диск во время расширения
    }

    public int getZone2Radius() {
        return zone2Radius;
    }

    public int getZone3Radius() {
        return zone3Radius;
    }

    public int getZone4Radius() {
        return zone4Radius;
    }

    private void saveConfig() {
        configManager.getConfig().set("dangerZoneStart", dangerZoneStart);
        configManager.getConfig().set("zones.zone1.radius", zone1Radius);
        // Сохраняем радиусы зон, если они были изменены
        if (configManager.getConfig().contains("zones.zone2.radius")) {
            configManager.getConfig().set("zones.zone2.radius", zone2Radius);
        }
        if (configManager.getConfig().contains("zones.zone3.radius")) {
            configManager.getConfig().set("zones.zone3.radius", zone3Radius);
        }
        if (configManager.getConfig().contains("zones.zone4.radius")) {
            configManager.getConfig().set("zones.zone4.radius", zone4Radius);
        }
        configManager.saveConfig();
    }
}

