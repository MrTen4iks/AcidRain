package org.acidRain.acidRain.managers;

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
        try {
            this.currentLanguage = language;
            messages.clear();
            
            plugin.getLogger().info("Начинаем загрузку языка: " + language);
            
            // Загружаем файл локализации
            File messagesFile = new File(plugin.getDataFolder(), "messages_" + language + ".yml");
            
            // Если файл не существует, копируем из ресурсов
            if (!messagesFile.exists()) {
                plugin.getLogger().info("Файл локализации не найден, копируем из ресурсов: messages_" + language + ".yml");
                plugin.saveResource("messages_" + language + ".yml", false);
                
                // Проверяем, что файл создался
                if (!messagesFile.exists()) {
                    throw new RuntimeException("Не удалось создать файл локализации: messages_" + language + ".yml");
                }
            }
            
            // Принудительно перезагружаем файл
            try {
                messagesFile.setLastModified(System.currentTimeMillis());
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось обновить timestamp файла: " + e.getMessage());
            }
            
            // Загружаем конфигурацию
            FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);
            
            // Загружаем дефолтные значения из ресурсов
            InputStream defConfigStream = plugin.getResource("messages_" + language + ".yml");
            if (defConfigStream != null) {
                try {
                    config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)));
                    defConfigStream.close();
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка при загрузке дефолтных значений: " + e.getMessage());
                }
            } else {
                plugin.getLogger().warning("Ресурс messages_" + language + ".yml не найден в JAR файле");
            }
            
            messages.put(language, config);
            plugin.getLogger().info("Язык успешно загружен: " + language);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Критическая ошибка при загрузке языка '" + language + "': " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось загрузить язык: " + language, e);
        }
    }

    public String getMessage(String path) {
        return getMessage(path, new Object[0]);
    }

    public String getMessage(String path, Object... args) {
        FileConfiguration config = messages.get(currentLanguage);
        if (config == null) {
            plugin.getLogger().warning("Конфигурация для языка '" + currentLanguage + "' не найдена!");
            return "§c[Missing translation: " + path + "]";
        }
        
        String message = config.getString(path);
        if (message == null) {
            plugin.getLogger().warning("Перевод не найден для пути '" + path + "' в языке '" + currentLanguage + "'");
            return "§c[Missing translation: " + path + "]";
        }
        
        if (args.length > 0) {
            try {
                message = MessageFormat.format(message, args);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка форматирования сообщения для пути '" + path + "': " + e.getMessage());
            }
        }
        
        return message;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void reload() {
        try {
            loadLanguage(currentLanguage);
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при перезагрузке локализации: " + e.getMessage());
        }
    }
} 