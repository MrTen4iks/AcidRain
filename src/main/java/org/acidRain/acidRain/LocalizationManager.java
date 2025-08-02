package org.acidRain.acidRain;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class LocalizationManager {
    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> messages = new HashMap<>();
    private String currentLanguage = "ru";

    public LocalizationManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadLanguage(String language) {
        this.currentLanguage = language;
        messages.clear();
        
        // Загружаем файл локализации
        File messagesFile = new File(plugin.getDataFolder(), "messages_" + language + ".yml");
        
        // Если файл не существует, копируем из ресурсов
        if (!messagesFile.exists()) {
            plugin.saveResource("messages_" + language + ".yml", false);
        }
        
        // Принудительно перезагружаем файл
        try {
            messagesFile.setLastModified(System.currentTimeMillis());
        } catch (Exception e) {
            plugin.getLogger().warning("Could not update file timestamp: " + e.getMessage());
        }
        
        // Загружаем конфигурацию
        FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Загружаем дефолтные значения из ресурсов
        InputStream defConfigStream = plugin.getResource("messages_" + language + ".yml");
        if (defConfigStream != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)));
        }
        
        messages.put(language, config);
        plugin.getLogger().info("Language loaded: " + language);
    }

    public String getMessage(String path) {
        return getMessage(path, new Object[0]);
    }

    public String getMessage(String path, Object... args) {
        FileConfiguration config = messages.get(currentLanguage);
        if (config == null) {
            return "§c[Missing translation: " + path + "]";
        }
        
        String message = config.getString(path);
        if (message == null) {
            return "§c[Missing translation: " + path + "]";
        }
        
        if (args.length > 0) {
            try {
                message = MessageFormat.format(message, args);
            } catch (Exception e) {
                plugin.getLogger().warning("Error formatting message for path '" + path + "': " + e.getMessage());
            }
        }
        
        return message;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void reload() {
        loadLanguage(currentLanguage);
    }
} 
