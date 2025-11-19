package org.acidRain.acidRain;

import org.acidRain.acidRain.client.EffectManager;
import org.acidRain.acidRain.client.ParticleManager;
import org.acidRain.acidRain.commands.CommandManager;
import org.acidRain.acidRain.config.ConfigManager;
import org.acidRain.acidRain.listeners.PlayerListener;
import org.acidRain.acidRain.managers.LocalizationManager;
import org.acidRain.acidRain.managers.SuitManager;
import org.acidRain.acidRain.registry.RecipeRegistry;
import org.acidRain.acidRain.registry.item.ProtectionSuit;
import org.acidRain.acidRain.world.ExpansionManager;
import org.acidRain.acidRain.world.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public final class AcidRain extends JavaPlugin {
    
    // Managers
    private ConfigManager configManager;
    private LocalizationManager localizationManager;
    private ZoneManager zoneManager;
    private ExpansionManager expansionManager;
    private SuitManager suitManager;
    private EffectManager effectManager;
    private ParticleManager particleManager;
    private CommandManager commandManager;
    
    // Registry
    private ProtectionSuit protectionSuit;
    private RecipeRegistry recipeRegistry;
    
    // Listeners
    private PlayerListener playerListener;
    
    // Tasks
    private ScheduledTask particleTask;
    private ScheduledTask effectTask;
    private ScheduledTask suitTimerTask;

    @Override
    public void onEnable() {
        // Инициализация конфигурации
        configManager = new ConfigManager(this);
        
        // Инициализация локализации
        localizationManager = new LocalizationManager(this);
        localizationManager.loadLanguage(configManager.getConfig().getString("language", "ru"));
        
        // Инициализация зон
        zoneManager = new ZoneManager(this, configManager);
        
        // Инициализация расширения
        expansionManager = new ExpansionManager(this, configManager, zoneManager, localizationManager);
        
        // Инициализация предметов
        protectionSuit = new ProtectionSuit(this, configManager, localizationManager);
        
        // Инициализация менеджера костюмов
        suitManager = new SuitManager(this, configManager, localizationManager, protectionSuit);
        
        // Инициализация эффектов
        effectManager = new EffectManager(zoneManager, suitManager, localizationManager);
        
        // Инициализация частиц
        particleManager = new ParticleManager(this, configManager, zoneManager, suitManager);
        
        // Инициализация команд
        commandManager = new CommandManager(this, configManager, localizationManager, zoneManager, 
                                           expansionManager, suitManager, protectionSuit);
        
        // Регистрация рецептов
        recipeRegistry = new RecipeRegistry(this, localizationManager, protectionSuit);
        recipeRegistry.registerRecipes();
        
        // Регистрация команд
        registerCommands();
        
        // Регистрация слушателей
        playerListener = new PlayerListener(this, suitManager, effectManager, protectionSuit, commandManager);
        getServer().getPluginManager().registerEvents(playerListener, this);
        
        // Запуск задач
        startTasks();
        
        // Логирование запуска
        logStartup();
    }

    @Override
    public void onDisable() {
        // Сохранение данных
        suitManager.saveProtectionTimes();
        expansionManager.cancelTasks();
        
        // Отмена задач
        if (particleTask != null) particleTask.cancel();
        if (effectTask != null) effectTask.cancel();
        if (suitTimerTask != null) suitTimerTask.cancel();
        
        // Логирование выключения
        logShutdown();
    }

    private void registerCommands() {
        String[] commands = {"acidrain", "aon", "aoff", "aset", "aexpand", "astatus", "asuit", "arecipes", "atime", "adiscord", "atg_dex", "alang"};
        for (String cmd : commands) {
            org.bukkit.command.PluginCommand command = getCommand(cmd);
            if (command != null) {
                command.setExecutor(commandManager);
                command.setTabCompleter(commandManager);
            }
        }
    }

    private void startTasks() {
        // Задача для частиц
        particleTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            if (!commandManager.isAcidRainEnabled()) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    if (player.getLocation().getWorld() != null) {
                        player.getScheduler().run(this, scheduledTask -> {
                            particleManager.handleParticles(player);
                        }, () -> {});
                    }
                } catch (Exception e) {
                    getLogger().warning(localizationManager.getMessage("console.errors.player_processing", player.getName(), e.getMessage()));
                }
            }
        }, 1L, 20L);

        // Задача для эффектов и проверки костюмов
        effectTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            if (!commandManager.isAcidRainEnabled()) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getLocation().getWorld() != null) {
                    player.getScheduler().run(this, scheduledTask -> {
                        effectManager.checkSuitAndEffects(player);
                    }, () -> {});
                }
            }
        }, 1L, 20L);

        // Задача для обновления таймеров костюмов
        suitTimerTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getLocation().getWorld() == null) continue;
                
                if (suitManager.hasActiveProtection(player.getUniqueId())) {
                    player.getScheduler().run(this, scheduledTask -> {
                        Long expireTime = suitManager.getExpirationTime(player.getUniqueId());
                        if (expireTime != null) {
                            if (System.currentTimeMillis() >= expireTime) {
                                suitManager.damageSuit(player);
                            } else {
                                suitManager.updateArmorLore(player);
                            }
                        }
                    }, () -> {});
                }
            }
        }, 1L, 20L);
    }

    private void logStartup() {
        getLogger().info("");
        getLogger().info("================================================================");
        getLogger().info("                    " + localizationManager.getMessage("console.startup.title"));
        getLogger().info("================================================================");
        getLogger().info("                    " + localizationManager.getMessage("console.startup.subtitle"));
        getLogger().info("                    " + localizationManager.getMessage("console.startup.border", zoneManager.getDangerZoneStart()));
        getLogger().info("                    " + localizationManager.getMessage("console.startup.discord"));
        getLogger().info("                    " + localizationManager.getMessage("console.startup.authors"));
        getLogger().info("================================================================");
        getLogger().info("");
        getLogger().info(localizationManager.getMessage("console.startup.success"));
        getLogger().info(localizationManager.getMessage("console.startup.join_discord"));
        getLogger().info("");
    }

    private void logShutdown() {
        getLogger().info("");
        getLogger().info("================================================================");
        getLogger().info("                    " + localizationManager.getMessage("console.shutdown.title"));
        getLogger().info("================================================================");
        getLogger().info("                    " + localizationManager.getMessage("console.shutdown.subtitle"));
        getLogger().info("                              " + localizationManager.getMessage("console.shutdown.disabled"));
        getLogger().info("                    " + localizationManager.getMessage("console.shutdown.discord"));
        getLogger().info("================================================================");
        getLogger().info("");
        getLogger().info(localizationManager.getMessage("console.shutdown.success"));
        getLogger().info("");
    }

    // Геттеры для доступа к менеджерам из других классов
    public LocalizationManager getLocalizationManager() {
        return localizationManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public SuitManager getSuitManager() {
        return suitManager;
    }
}
