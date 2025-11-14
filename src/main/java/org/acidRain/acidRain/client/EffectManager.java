package org.acidRain.acidRain.client;

import org.acidRain.acidRain.managers.LocalizationManager;
import org.acidRain.acidRain.managers.SuitManager;
import org.acidRain.acidRain.world.ZoneManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EffectManager {
    private final ZoneManager zoneManager;
    private final SuitManager suitManager;
    private final LocalizationManager localizationManager;
    private final Map<UUID, Long> lastEffectTime = new HashMap<>();
    private final Map<UUID, Integer> currentZone = new HashMap<>();

    public EffectManager(ZoneManager zoneManager, SuitManager suitManager, LocalizationManager localizationManager) {
        this.zoneManager = zoneManager;
        this.suitManager = suitManager;
        this.localizationManager = localizationManager;
    }

    public void checkSuitAndEffects(Player player) {
        UUID playerId = player.getUniqueId();
        boolean wearingFullSuit = suitManager.checkFullSuit(player);

        if (!wearingFullSuit && suitManager.hasFullSuit(playerId)) {
            suitManager.pauseProtectionTimer(player);
            return;
        }

        if (wearingFullSuit) {
            Long expireTime = suitManager.getExpirationTime(playerId);

            if (expireTime == null) {
                suitManager.startProtectionTimer(player);
                return;
            }

            if (System.currentTimeMillis() >= expireTime) {
                suitManager.damageSuit(player);
                return;
            }

            suitManager.updateArmorLore(player);
        }

        int zone = zoneManager.getZoneForLocation(player.getLocation());
        if (zone > 0) {
            boolean protectionActive = suitManager.hasActiveProtection(playerId);

            if (!protectionActive) {
                applyZoneEffects(player, zone);
            }
        }
    }

    public void handlePlayerMove(Player player, Location from, Location to) {
        if (to == null || from.getBlock().equals(to.getBlock())) return;

        int zone = zoneManager.getZoneForLocation(to);
        Integer oldZone = currentZone.get(player.getUniqueId());

        if (oldZone == null || oldZone != zone) {
            currentZone.put(player.getUniqueId(), zone);
            if (zone == 0) {
                clearEffects(player);
            }
        }
        
        if (zone > 0) {
            showZoneActionBar(player, zone);
        }
    }

    private void showZoneActionBar(Player player, int zone) {
        String zoneMessage;
        ChatColor zoneColor;
        switch (zone) {
            case 1:
                zoneMessage = localizationManager.getMessage("player.info.zone_1");
                zoneColor = ChatColor.YELLOW;
                break;
            case 2:
                zoneMessage = localizationManager.getMessage("player.info.zone_2");
                zoneColor = ChatColor.GOLD;
                break;
            case 3:
                zoneMessage = localizationManager.getMessage("player.info.zone_3");
                zoneColor = ChatColor.RED;
                break;
            case 4:
                zoneMessage = localizationManager.getMessage("player.info.zone_4");
                zoneColor = ChatColor.DARK_RED;
                break;
            default:
                zoneMessage = localizationManager.getMessage("player.info.dangerous_zone", zone);
                zoneColor = ChatColor.RED;
        }
        player.sendActionBar(zoneColor + zoneMessage);
    }

    private void applyZoneEffects(Player player, int zone) {
        long now = System.currentTimeMillis();
        if (now - lastEffectTime.getOrDefault(player.getUniqueId(), 0L) < 20000) {
            return;
        }

        clearEffects(player);

        if (player.getWorld() != null) {
            switch (zone) {
                case 1:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0));
                    player.getWorld().playSound(player.getLocation(), Sound.WEATHER_RAIN_ABOVE, 1.0f, 1.0f);
                    break;
                case 2:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20000000, 19));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20000000, 19));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSER, 1.0f, 0.8f);
                    break;
                case 3:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 3));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 3));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 3));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 3));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20000000, 19));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20000000, 19));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSER, 1.0f, 0.6f);
                    break;
                case 4:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 9));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 9));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 9));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 9));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 9));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20000000, 19));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20000000, 19));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSER, 0.5f, 1.0f);
                    break;
            }
        }

        player.damage(zone * 0.7);
        lastEffectTime.put(player.getUniqueId(), now);
    }

    public void clearEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    public void clearRadiationEffects(Player player) {
        suitManager.clearRadiationEffects(player);
        lastEffectTime.remove(player.getUniqueId());
        currentZone.remove(player.getUniqueId());
    }

    public void removePlayerData(UUID playerId) {
        lastEffectTime.remove(playerId);
        currentZone.remove(playerId);
    }
}

