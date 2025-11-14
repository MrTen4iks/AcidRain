package org.acidRain.acidRain.world;

import org.acidRain.acidRain.AcidRain;
import org.acidRain.acidRain.config.ConfigManager;
import org.bukkit.Location;

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
    }

    public void calculateZoneRadii() {
        zone2Radius = (int)(zone1Radius * 2.5);
        zone3Radius = (int)(zone1Radius * 5.0);
        zone4Radius = (int)(zone1Radius * 10.0);
    }

    public int getZoneForLocation(Location loc) {
        if (loc == null) return 0;
        int distance = Math.max(Math.abs(loc.getBlockX()), Math.abs(loc.getBlockZ()));

        if (distance <= dangerZoneStart) return 0;
        if (distance <= dangerZoneStart + zone1Radius) return 1;
        if (distance <= dangerZoneStart + zone2Radius) return 2;
        if (distance <= dangerZoneStart + zone3Radius) return 3;
        return 4;
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
        configManager.saveConfig();
    }
}

