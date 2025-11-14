package org.acidRain.acidRain.listeners;

import org.acidRain.acidRain.AcidRain;
import org.acidRain.acidRain.client.EffectManager;
import org.acidRain.acidRain.commands.CommandManager;
import org.acidRain.acidRain.managers.SuitManager;
import org.acidRain.acidRain.registry.item.ProtectionSuit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerListener implements Listener {
    private final AcidRain plugin;
    private final SuitManager suitManager;
    private final EffectManager effectManager;
    private final ProtectionSuit protectionSuit;
    private final CommandManager commandManager;

    public PlayerListener(AcidRain plugin, SuitManager suitManager, EffectManager effectManager,
                         ProtectionSuit protectionSuit, CommandManager commandManager) {
        this.plugin = plugin;
        this.suitManager = suitManager;
        this.effectManager = effectManager;
        this.protectionSuit = protectionSuit;
        this.commandManager = commandManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player) {
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> {
                boolean wasWearing = suitManager.hasFullSuit(player.getUniqueId());
                boolean nowWearing = suitManager.checkFullSuit(player);

                if (nowWearing && !wasWearing) {
                    if (suitManager.getExpirationTime(player.getUniqueId()) == null) {
                        suitManager.startProtectionTimer(player);
                    } else {
                        suitManager.resumeProtectionTimer(player);
                    }
                } else if (!nowWearing && wasWearing) {
                    suitManager.pauseProtectionTimer(player);
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer data = meta.getPersistentDataContainer();

                    if (data.has(new NamespacedKey(plugin, "full_protection_set"), PersistentDataType.INTEGER)) {
                        event.setCancelled(true);

                        org.bukkit.entity.Player player = event.getPlayer();
                        if (player.getInventory().firstEmpty() == -1) {
                            player.sendMessage(ChatColor.RED + plugin.getLocalizationManager().getMessage("player.errors.no_inventory_space"));
                            return;
                        }

                        ItemStack[] suitSet = protectionSuit.createSuitSet();
                        player.getInventory().addItem(suitSet);
                        item.setAmount(item.getAmount() - 1);
                        
                        effectManager.clearRadiationEffects(player);
                        
                        player.sendMessage(ChatColor.GREEN + plugin.getLocalizationManager().getMessage("player.success.suit_received"));
                        player.sendMessage(ChatColor.GREEN + plugin.getLocalizationManager().getMessage("player.success.radiation_cleared"));
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        if (suitManager.getExpirationTime(player.getUniqueId()) != null) {
            if (System.currentTimeMillis() >= suitManager.getExpirationTime(player.getUniqueId())) {
                suitManager.removeSuitData(player.getUniqueId());
            } else {
                suitManager.updateArmorLore(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!commandManager.isAcidRainEnabled()) return;

        org.bukkit.Location to = event.getTo();
        if (to == null) return;

        effectManager.handlePlayerMove(event.getPlayer(), event.getFrom(), to);
    }
}

