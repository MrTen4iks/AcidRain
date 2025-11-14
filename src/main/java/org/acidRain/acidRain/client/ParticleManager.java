package org.acidRain.acidRain.client;

import org.acidRain.acidRain.AcidRain;
import org.acidRain.acidRain.config.ConfigManager;
import org.acidRain.acidRain.managers.SuitManager;
import org.acidRain.acidRain.world.ZoneManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;

public class ParticleManager {
    private final AcidRain plugin;
    private final ConfigManager configManager;
    private final ZoneManager zoneManager;
    private final SuitManager suitManager;
    private final Random random = new Random();

    public ParticleManager(AcidRain plugin, ConfigManager configManager, ZoneManager zoneManager, SuitManager suitManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.zoneManager = zoneManager;
        this.suitManager = suitManager;
    }

    public void handleParticles(Player player) {
        int zone = zoneManager.getZoneForLocation(player.getLocation());
        if (zone > 0 && zoneManager.isUnderOpenSky(player.getLocation())) {
            UUID playerId = player.getUniqueId();
            
            if (suitManager.hasActiveProtection(playerId)) {
                spawnProtectionParticles(player);
            } else {
                spawnZoneParticles(player, zone);
            }
        }
    }

    private void spawnZoneParticles(Player player, int zone) {
        Location loc = player.getLocation();
        World world = player.getWorld();
        
        if (world == null) return;

        spawnAcidRainEffect(world, loc, zone);

        switch (zone) {
            case 1:
                world.spawnParticle(Particle.FALLING_WATER, loc, 50, 2, 5, 2, 0.1);
                break;
            case 2:
                world.spawnParticle(Particle.FALLING_LAVA, loc, 40, 2, 5, 2, 0.1);
                world.spawnParticle(Particle.DRIPPING_LAVA, loc, 30, 0.5, 0.5, 0.5);
                world.spawnParticle(Particle.SMOKE, player.getLocation(), 2000, 50, 50, 50, new Particle.DustOptions(Color.RED, 3));
                break;
            case 3:
                world.spawnParticle(Particle.LARGE_SMOKE, loc, 25, 0.5, 0.5, 0.5);
                world.spawnParticle(Particle.DUST, player.getLocation(), 2000, 50, 50, 50, new Particle.DustOptions(Color.RED, 3));
                break;
            case 4:
                world.spawnParticle(Particle.LARGE_SMOKE, loc, 40, 1, 1, 1);
                world.spawnParticle(Particle.SQUID_INK, loc, 30, 1, 1, 1);
                break;
        }
    }

    private void spawnAcidRainEffect(World world, Location center, int zone) {
        if (!configManager.getConfig().getBoolean("acidRainEffects.enabled", true)) {
            return;
        }
        
        int baseIntensity = configManager.getConfig().getInt("acidRainEffects.rainIntensity", 20);
        int rainIntensity = baseIntensity + (zone * 10);
        
        double baseRadius = configManager.getConfig().getDouble("acidRainEffects.rainRadius", 3.0);
        double radius = baseRadius + (zone * 0.5);
        
        // Создаем кислотный дождь
        for (int i = 0; i < rainIntensity; i++) {
            double x = center.getX() + (random.nextDouble() - 0.5) * radius * 2;
            double z = center.getZ() + (random.nextDouble() - 0.5) * radius * 2;
            double y = center.getY() + 15 + random.nextDouble() * 10;
            
            Location rainLoc = new Location(world, x, y, z);
            
            world.spawnParticle(
                Particle.DUST,
                rainLoc,
                250,
                50, -1.0, 50,
                0.2,
                new Particle.DustOptions(Color.GRAY, 1)
            );
        }
        
        // Создаем туманный эффект
        int fogIntensity = rainIntensity * 2;
        for (int i = 0; i < fogIntensity; i++) {
            double x = center.getX() + (random.nextDouble() - 0.5) * radius * 3;
            double z = center.getZ() + (random.nextDouble() - 0.5) * radius * 3;
            double y = center.getY() + 1 + random.nextDouble() * 8;
            
            Location fogLoc = new Location(world, x, y, z);
            
            world.spawnParticle(
                Particle.DUST,
                fogLoc,
                250,
                50, 50, 50,
                0.05,
                new Particle.DustOptions(Color.GREEN, 1)
            );
        }
        
        // Звуковой эффект
        if (configManager.getConfig().getBoolean("acidRainEffects.soundEnabled", true)) {
            int soundChance = configManager.getConfig().getInt("acidRainEffects.soundChance", 5);
            if (random.nextInt(100) < soundChance) {
                float volume = 0.3f + (zone * 0.1f);
                float pitch = 0.8f + (random.nextFloat() * 0.4f);
                world.playSound(center, Sound.WEATHER_RAIN, volume, pitch);
            }
        }
    }

    public void spawnProtectionParticles(Player player) {
        World world = player.getWorld();
        if (world == null) return;

        world.spawnParticle(
                Particle.DUST,
                player.getLocation().add(0, 2, 0),
                250,
                50, 50, 50,
                0.1,
                new Particle.DustOptions(Color.LIME, 1)
        );

        for (int i = 0; i < 360; i += 30) {
            double angle = Math.toRadians(i);
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            world.spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    player.getLocation().add(x, 1.5, z),
                    250,
                    50, 50, 50,
                    0
            );
        }
    }
}

