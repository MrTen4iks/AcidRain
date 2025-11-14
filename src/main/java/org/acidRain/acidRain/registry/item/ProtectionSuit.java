package org.acidRain.acidRain.registry.item;

import org.acidRain.acidRain.AcidRain;
import org.acidRain.acidRain.config.ConfigManager;
import org.acidRain.acidRain.managers.LocalizationManager;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ProtectionSuit {
    private final AcidRain plugin;
    private final ConfigManager configManager;
    private final LocalizationManager localizationManager;
    private final Random random = new Random();

    public ProtectionSuit(AcidRain plugin, ConfigManager configManager, LocalizationManager localizationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.localizationManager = localizationManager;
    }

    public ItemStack createArmorPiece(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + name);

            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "acidrain_protection"),
                    PersistentDataType.INTEGER,
                    1
            );

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Защищает от кислотных дождей");
            lore.add(ChatColor.GRAY + "Длительность: " + configManager.getConfig().getInt("protectionSuit.duration", 60) + " минут");
            lore.add(ChatColor.YELLOW + "Осталось: " + getRemainingTimeForDisplay(
                    System.currentTimeMillis() + configManager.getConfig().getInt("protectionSuit.duration", 60) * 60000L));
            meta.setLore(lore);

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);

            item.setItemMeta(meta);
        }
        return item;
    }

    public void updateLore(ItemStack item, long expireTime) {
        if (item == null || !item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (!data.has(new NamespacedKey(plugin, "acidrain_protection"), PersistentDataType.INTEGER)) {
            return;
        }

        List<String> lore = meta.getLore();
        if (lore != null && lore.size() >= 3) {
            lore.set(2, ChatColor.YELLOW + "Осталось: " + getRemainingTimeForDisplay(expireTime));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    public ItemStack createFullProtectionSuit() {
        ItemStack resultItem = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = resultItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + localizationManager.getMessage("items.full_suit_name"));

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(new NamespacedKey(plugin, "full_protection_set"), PersistentDataType.INTEGER, 1);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "ПКМ - получить полный комплект брони");
            lore.add(ChatColor.GRAY + "Защищает от всех эффектов кислотных дождей");
            meta.setLore(lore);

            resultItem.setItemMeta(meta);
        }
        return resultItem;
    }

    public void giveFullSuit(Player player) {
        ItemStack helmet = createArmorPiece(Material.LEATHER_HELMET, localizationManager.getMessage("items.helmet_name"));
        ItemStack chestplate = createArmorPiece(Material.LEATHER_CHESTPLATE, localizationManager.getMessage("items.chestplate_name"));
        ItemStack leggings = createArmorPiece(Material.LEATHER_LEGGINGS, localizationManager.getMessage("items.leggings_name"));
        ItemStack boots = createArmorPiece(Material.LEATHER_BOOTS, localizationManager.getMessage("items.boots_name"));

        // Генерируем случайный цвет для костюма
        Color color = generateRandomColor();

        setArmorColor(helmet, color);
        setArmorColor(chestplate, color);
        setArmorColor(leggings, color);
        setArmorColor(boots, color);

        player.getInventory().addItem(helmet, chestplate, leggings, boots);
    }

    public ItemStack[] createSuitSet() {
        ItemStack helmet = createArmorPiece(Material.LEATHER_HELMET, localizationManager.getMessage("items.helmet_name"));
        ItemStack chestplate = createArmorPiece(Material.LEATHER_CHESTPLATE, localizationManager.getMessage("items.chestplate_name"));
        ItemStack leggings = createArmorPiece(Material.LEATHER_LEGGINGS, localizationManager.getMessage("items.leggings_name"));
        ItemStack boots = createArmorPiece(Material.LEATHER_BOOTS, localizationManager.getMessage("items.boots_name"));

        // Генерируем случайный цвет для костюма
        Color color = generateRandomColor();

        setArmorColor(helmet, color);
        setArmorColor(chestplate, color);
        setArmorColor(leggings, color);
        setArmorColor(boots, color);

        return new ItemStack[]{helmet, chestplate, leggings, boots};
    }

    /**
     * Генерирует случайный цвет для защитного костюма
     * @return случайный цвет RGB
     */
    private Color generateRandomColor() {
        // Генерируем случайные значения RGB (от 50 до 255 для ярких цветов)
        int r = 50 + random.nextInt(206); // 50-255
        int g = 50 + random.nextInt(206); // 50-255
        int b = 50 + random.nextInt(206); // 50-255
        
        return Color.fromRGB(r, g, b);
    }

    private void setArmorColor(ItemStack item, Color color) {
        if (item.getItemMeta() instanceof LeatherArmorMeta) {
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            meta.setColor(color);
            item.setItemMeta(meta);
        }
    }

    private String getRemainingTimeForDisplay(long expireTime) {
        long remaining = expireTime - System.currentTimeMillis();
        if (remaining <= 0) return localizationManager.getMessage("player.info.suit_expired");
        long minutes = remaining / 60000;
        long seconds = (remaining % 60000) / 1000;
        return String.format("%d мин. %d сек.", minutes, seconds);
    }
}

