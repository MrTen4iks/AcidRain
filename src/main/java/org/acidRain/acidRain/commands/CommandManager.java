package org.acidRain.acidRain.commands;

import org.acidRain.acidRain.AcidRain;
import org.acidRain.acidRain.client.EffectManager;
import org.acidRain.acidRain.config.ConfigManager;
import org.acidRain.acidRain.managers.LocalizationManager;
import org.acidRain.acidRain.managers.SuitManager;
import org.acidRain.acidRain.registry.item.ProtectionSuit;
import org.acidRain.acidRain.world.ExpansionManager;
import org.acidRain.acidRain.world.ZoneManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommandManager implements CommandExecutor, TabCompleter {
    private final AcidRain plugin;
    private final ConfigManager configManager;
    private final LocalizationManager localizationManager;
    private final ZoneManager zoneManager;
    private final ExpansionManager expansionManager;
    private final SuitManager suitManager;
    private final ProtectionSuit protectionSuit;
    private boolean acidRainEnabled;

    public CommandManager(AcidRain plugin, ConfigManager configManager, LocalizationManager localizationManager,
                         ZoneManager zoneManager, ExpansionManager expansionManager, SuitManager suitManager,
                         ProtectionSuit protectionSuit) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.localizationManager = localizationManager;
        this.zoneManager = zoneManager;
        this.expansionManager = expansionManager;
        this.suitManager = suitManager;
        this.protectionSuit = protectionSuit;
        this.acidRainEnabled = configManager.getConfig().getBoolean("acidRainEnabled", true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String command = cmd.getName().toLowerCase();

        switch (command) {
            case "asuit":
                return handleSuitCommand(sender);
            case "aon":
                return handleOnCommand(sender);
            case "aoff":
                return handleOffCommand(sender);
            case "astatus":
                return handleStatusCommand(sender);
            case "aexpand":
                return handleExpandCommand(sender, args);
            case "aset":
                return handleSetCommand(sender, args);
            case "arecipes":
                return handleRecipesCommand(sender);
            case "atime":
                return handleTimeCommand(sender);
            case "adiscord":
                return handleDiscordCommand(sender);
            case "alang":
                return handleLangCommand(sender, args);
            case "acidrain":
                return handleMainCommand(sender);
            default:
                return false;
        }
    }

    private boolean handleSuitCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.players_only"));
            return true;
        }
        if (!sender.hasPermission("acidrain.admin")) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.no_permission"));
            return true;
        }
        suitManager.giveProtectionSuit((Player) sender);
        return true;
    }

    private boolean handleOnCommand(CommandSender sender) {
        if (!sender.hasPermission("acidrain.admin")) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.no_permission"));
            return true;
        }
        acidRainEnabled = true;
        configManager.getConfig().set("acidRainEnabled", true);
        configManager.saveConfig();
        sender.sendMessage(ChatColor.GREEN + localizationManager.getMessage("player.success.acid_rain_enabled"));
        return true;
    }

    private boolean handleOffCommand(CommandSender sender) {
        if (!sender.hasPermission("acidrain.admin")) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.no_permission"));
            return true;
        }
        acidRainEnabled = false;
        configManager.getConfig().set("acidRainEnabled", false);
        configManager.saveConfig();
        sender.sendMessage(ChatColor.GREEN + localizationManager.getMessage("player.success.acid_rain_disabled"));
        return true;
    }

    private boolean handleStatusCommand(CommandSender sender) {
        if (!sender.hasPermission("acidrain.admin")) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.no_permission"));
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "");
        sender.sendMessage(ChatColor.GOLD + "           " + ChatColor.WHITE + localizationManager.getMessage("commands.astatus.title") + ChatColor.GOLD + "           ");
        sender.sendMessage(ChatColor.GOLD + "");
        
        String systemStatus = acidRainEnabled ? 
            ChatColor.GREEN + "● АКТИВНА" : 
            ChatColor.RED + "● ОТКЛЮЧЕНА";
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.WHITE + 
            localizationManager.getMessage("commands.astatus.system", systemStatus) + 
            ChatColor.GOLD + " ");
        
        // Проверяем статус расширения
        boolean expanding = expansionManager.isExpanding();
        if (expanding) {
            sender.sendMessage(ChatColor.GOLD + "" + ChatColor.WHITE + 
                localizationManager.getMessage("commands.astatus.expansion_in_progress") + 
                ChatColor.YELLOW + " ⚠ Координаты обновляются в реальном времени!" +
                ChatColor.GOLD + "");
        } else {
            sender.sendMessage(ChatColor.GOLD + "" + ChatColor.WHITE + 
                localizationManager.getMessage("commands.astatus.expansion_stable") + 
                ChatColor.GOLD + "");
        }

        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.WHITE + 
            localizationManager.getMessage("commands.astatus.zone_boundaries") + 
            ChatColor.GOLD + " ");
        
        // Показываем точные координаты границ зон (автоматически обновляются во время расширения)
        // Координаты берутся напрямую из ZoneManager, который обновляется в реальном времени
        int safeZoneBorder = zoneManager.getDangerZoneStart();
        int zone1Radius = zoneManager.getZone1Radius(); // Обновляется во время расширения
        int zone2Radius = zoneManager.getZone2Radius(); // Автоматически пересчитывается
        int zone3Radius = zoneManager.getZone3Radius(); // Автоматически пересчитывается
        int zone4Radius = zoneManager.getZone4Radius(); // Автоматически пересчитывается
        
        int zone1Border = safeZoneBorder + zone1Radius;
        int zone2Border = safeZoneBorder + zone2Radius;
        int zone3Border = safeZoneBorder + zone3Radius;
        int zone4Border = safeZoneBorder + zone4Radius;
        
        // Показываем точные координаты: ±X блоков (от -X до +X)
        // Все координаты автоматически обновляются во время расширения
        String expandingIndicator = expanding ? ChatColor.YELLOW + " [РАСШИРЯЕТСЯ]" : "";
        
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.GREEN + "🛡 " + 
            ChatColor.WHITE + "Безопасная зона: " + ChatColor.GREEN + "±" + safeZoneBorder + 
            ChatColor.WHITE + " блоков " + ChatColor.GRAY + "(" + ChatColor.YELLOW + "-" + safeZoneBorder + 
            ChatColor.GRAY + " до " + ChatColor.YELLOW + "+" + safeZoneBorder + ChatColor.GRAY + ")" + 
            expandingIndicator + ChatColor.GOLD + " ");
        
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.YELLOW + "⚠ " + 
            ChatColor.WHITE + "Зона 1 (слабая): " + ChatColor.YELLOW + "±" + zone1Border + 
            ChatColor.WHITE + " блоков " + ChatColor.GRAY + "(" + ChatColor.YELLOW + "-" + zone1Border + 
            ChatColor.GRAY + " до " + ChatColor.YELLOW + "+" + zone1Border + ChatColor.GRAY + ")" + 
            expandingIndicator + ChatColor.GOLD + "");
        
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.GOLD + "⚡ " + 
            ChatColor.WHITE + "Зона 2 (средняя): " + ChatColor.GOLD + "±" + zone2Border + 
            ChatColor.WHITE + " блоков " + ChatColor.GRAY + "(" + ChatColor.YELLOW + "-" + zone2Border + 
            ChatColor.GRAY + " до " + ChatColor.YELLOW + "+" + zone2Border + ChatColor.GRAY + ")" + 
            expandingIndicator + ChatColor.GOLD + " ");
        
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.RED + "🔥 " + 
            ChatColor.WHITE + "Зона 3 (сильная): " + ChatColor.RED + "±" + zone3Border + 
            ChatColor.WHITE + " блоков " + ChatColor.GRAY + "(" + ChatColor.YELLOW + "-" + zone3Border + 
            ChatColor.GRAY + " до " + ChatColor.YELLOW + "+" + zone3Border + ChatColor.GRAY + ")" + 
            expandingIndicator + ChatColor.GOLD + " ");
        
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.DARK_RED + "💀 " + 
            ChatColor.WHITE + "Зона 4 (смертельная): " + ChatColor.DARK_RED + "±" + zone4Border + 
            ChatColor.WHITE + " блоков " + ChatColor.GRAY + "(" + ChatColor.YELLOW + "-" + zone4Border + 
            ChatColor.GRAY + " до " + ChatColor.YELLOW + "+" + zone4Border + ChatColor.GRAY + ")" + 
            expandingIndicator + ChatColor.GOLD + " ");

        if (sender instanceof Player) {
            Player player = (Player) sender;
            int currentZone = zoneManager.getZoneForLocation(player.getLocation());
            int distance = (int) player.getLocation().distance(new Location(player.getWorld(), 0, player.getLocation().getY(), 0));
            
            sender.sendMessage(ChatColor.GOLD + "" + ChatColor.WHITE + 
                localizationManager.getMessage("commands.astatus.your_position") + 
                ChatColor.GOLD + " ");
            sender.sendMessage(ChatColor.GOLD + "" + ChatColor.WHITE + "  " + 
                localizationManager.getMessage("commands.astatus.your_coordinates", 
                    player.getLocation().getBlockX(), 
                    player.getLocation().getBlockY(), 
                    player.getLocation().getBlockZ()) + 
                ChatColor.GOLD + " ");
            sender.sendMessage(ChatColor.GOLD + "" + ChatColor.WHITE + "  " + 
                localizationManager.getMessage("commands.astatus.distance_from_center", distance) + 
                ChatColor.GOLD + " ");
        }
        
        sender.sendMessage(ChatColor.GOLD + "");
        return true;
    }

    private boolean handleExpandCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("acidrain.admin")) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.no_permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("commands.usage.aexpand"));
            sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("commands.usage.aexpand_example"));
            return true;
        }
        try {
            int blocks = Integer.parseInt(args[0]);
            int minutes = Integer.parseInt(args[1]);
            int seconds = minutes * 60;
            expansionManager.startExpansion(blocks, seconds);
            sender.sendMessage(ChatColor.GREEN + localizationManager.getMessage("player.success.expansion_started", blocks, minutes));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.invalid_numbers"));
            sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("commands.usage.aexpand_example_short"));
        }
        return true;
    }

    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("acidrain.admin")) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.no_permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("commands.usage.aset"));
            sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("commands.usage.aset_example"));
            return true;
        }
        try {
            int value = Integer.parseInt(args[0]);
            zoneManager.setDangerZoneStart(value);
            sender.sendMessage(ChatColor.GREEN + localizationManager.getMessage("player.success.border_set", value));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.invalid_number"));
            sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("commands.usage.aset_example_short"));
        }
        return true;
    }

    private boolean handleRecipesCommand(CommandSender sender) {
        if (!sender.hasPermission("acidrain.admin")) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.no_permission"));
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + localizationManager.getMessage("commands.arecipes.title"));
        sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("commands.arecipes.helmet"));
        sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("commands.arecipes.chestplate"));
        sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("commands.arecipes.leggings"));
        sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("commands.arecipes.boots"));
        sender.sendMessage(ChatColor.GREEN + localizationManager.getMessage("commands.arecipes.full_suit"));
        sender.sendMessage(ChatColor.GREEN + localizationManager.getMessage("commands.arecipes.full_suit_shape_1"));
        sender.sendMessage(ChatColor.GREEN + localizationManager.getMessage("commands.arecipes.full_suit_shape_2"));
        sender.sendMessage(ChatColor.GREEN + localizationManager.getMessage("commands.arecipes.full_suit_shape_3"));
        sender.sendMessage(ChatColor.AQUA + localizationManager.getMessage("commands.arecipes.full_suit_legend_1"));
        sender.sendMessage(ChatColor.AQUA + localizationManager.getMessage("commands.arecipes.full_suit_legend_2"));
        return true;
    }

    private boolean handleTimeCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.players_only"));
            return true;
        }
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation(), 200, 15, 15, 15, new Particle.DustOptions(org.bukkit.Color.GRAY, 1));
        
        Long expireTime = suitManager.getExpirationTime(playerId);
        if (expireTime != null) {
            long remaining = expireTime - System.currentTimeMillis();
            
            if (remaining > 0) {
                long minutes = remaining / 60000;
                long seconds = (remaining % 60000) / 1000;
                boolean isWearing = suitManager.hasFullSuit(playerId);
                boolean isPaused = suitManager.isPaused(playerId);
                
                if (isWearing && !isPaused) {
                    sender.sendMessage(ChatColor.GREEN + localizationManager.getMessage("player.info.suit_active"));
                    sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("player.info.time_remaining", minutes, seconds));
                } else {
                    sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("player.info.suit_paused_status"));
                    sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("player.info.time_remaining", minutes, seconds));
                }
            } else {
                sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.info.suit_expired"));
            }
        } else {
            sender.sendMessage(ChatColor.GRAY + localizationManager.getMessage("player.info.no_active_suit"));
        }
        return true;
    }

    private boolean handleDiscordCommand(CommandSender sender) {
        if (!sender.hasPermission("acidrain.admin")) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.no_permission"));
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + localizationManager.getMessage("commands.discord_message", "https://discord.gg/gV2KmUbqXC"));
        return true;
    }

    private boolean handleLangCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("acidrain.admin")) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("player.errors.no_permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + localizationManager.getMessage("commands.acidrain.language_usage"));
            sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("commands.acidrain.language_example"));
            sender.sendMessage(ChatColor.GRAY + localizationManager.getMessage("commands.acidrain.language_available"));
            return true;
        }
        
        String newLanguage = args[0].toLowerCase();
        if (!newLanguage.equals("ru") && !newLanguage.equals("en")) {
            sender.sendMessage(ChatColor.RED + "Неподдерживаемый язык! " + localizationManager.getMessage("commands.acidrain.language_available"));
            return true;
        }
        
        configManager.getConfig().set("language", newLanguage);
        try {
            configManager.saveConfig();
            localizationManager.loadLanguage(newLanguage);
            sender.sendMessage(ChatColor.GREEN + localizationManager.getMessage("commands.acidrain.language_changed", newLanguage));
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Ошибка сохранения языка: " + e.getMessage());
        }
        return true;
    }

    private boolean handleMainCommand(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + localizationManager.getMessage("commands.acidrain.title"));
        sender.sendMessage(ChatColor.AQUA + localizationManager.getMessage("commands.acidrain.border", zoneManager.getDangerZoneStart()));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("commands.acidrain.commands_title"));
        sender.sendMessage(ChatColor.GREEN + "/asuit" + ChatColor.WHITE + " - " + localizationManager.getMessage("commands.acidrain.asuit"));
        sender.sendMessage(ChatColor.GREEN + "/aon" + ChatColor.WHITE + " - " + localizationManager.getMessage("commands.acidrain.aon"));
        sender.sendMessage(ChatColor.GREEN + "/aoff" + ChatColor.WHITE + " - " + localizationManager.getMessage("commands.acidrain.aoff"));
        sender.sendMessage(ChatColor.GREEN + "/astatus" + ChatColor.WHITE + " - " + localizationManager.getMessage("commands.acidrain.astatus"));
        sender.sendMessage(ChatColor.GREEN + "/aexpand <блоки> <минуты>" + ChatColor.WHITE + " - " + localizationManager.getMessage("commands.acidrain.aexpand"));
        sender.sendMessage(ChatColor.GREEN + "/aset <блоки>" + ChatColor.WHITE + " - " + localizationManager.getMessage("commands.acidrain.aset"));
        sender.sendMessage(ChatColor.GREEN + "/arecipes" + ChatColor.WHITE + " - " + localizationManager.getMessage("commands.acidrain.arecipes"));
        sender.sendMessage(ChatColor.GREEN + "/atime" + ChatColor.WHITE + " - " + localizationManager.getMessage("commands.acidrain.atime"));
        sender.sendMessage(ChatColor.GREEN + "/adiscord" + ChatColor.WHITE + " - " + localizationManager.getMessage("commands.acidrain.adiscord"));
        sender.sendMessage(ChatColor.GREEN + "/alang <ru|en>" + ChatColor.WHITE + " - " + localizationManager.getMessage("commands.acidrain.alang"));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + localizationManager.getMessage("commands.acidrain.discord", "https://discord.gg/gV2KmUbqXC"));
        return true;
    }

    public boolean isAcidRainEnabled() {
        return acidRainEnabled;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (cmd.getName().equalsIgnoreCase("acidrain")) {
                completions.add("status");
                completions.add("on");
                completions.add("off");
                completions.add("set");
                completions.add("expand");
                completions.add("suit");
                completions.add("recipes");
            } else if (cmd.getName().equalsIgnoreCase("alang")) {
                completions.add("ru");
                completions.add("en");
            }
        } else if (args.length == 2) {
            if (cmd.getName().equalsIgnoreCase("aexpand")) {
                completions.add("50");
                completions.add("100");
                completions.add("200");
                completions.add("300");
                completions.add("500");
            } else if (cmd.getName().equalsIgnoreCase("aset")) {
                completions.add("1000");
                completions.add("2000");
                completions.add("3000");
                completions.add("4000");
                completions.add("5000");
            }
        } else if (args.length == 3) {
            if (cmd.getName().equalsIgnoreCase("aexpand")) {
                completions.add("1");
                completions.add("2");
                completions.add("5");
                completions.add("10");
                completions.add("15");
            }
        }
        
        if (args.length > 0) {
            String input = args[args.length - 1].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        }
        
        return completions;
    }
}

