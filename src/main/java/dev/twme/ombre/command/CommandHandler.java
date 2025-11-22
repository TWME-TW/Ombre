package dev.twme.ombre.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.gui.GUIManager;
import dev.twme.ombre.i18n.MessageManager;
import dev.twme.ombre.manager.ConfigManager;

/**
 * 指令處理器
 * 處理所有 /ombre 相關指令
 */
public class CommandHandler implements CommandExecutor, TabCompleter {
    
    private final Ombre plugin;
    private final GUIManager guiManager;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    
    public CommandHandler(Ombre plugin, GUIManager guiManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.configManager = configManager;
        MessageManager resolvedManager = plugin.getMessageManager();
        if (resolvedManager == null) {
            throw new IllegalStateException("MessageManager must be initialized before CommandHandler");
        }
        this.messageManager = resolvedManager;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getComponent("general.player-only"));
            return true;
        }
        
        // /ombre - 打開漸層製作 GUI
        if (args.length == 0) {
            return handleOmbreCommand(player);
        }
        
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "library", "lib" -> handleLibraryCommand(player);
            case "favorites", "fav" -> handleFavoritesCommand(player);
            case "my", "list" -> handleMyGradientsCommand(player);
            case "palette" -> handlePaletteCommand(player, args);
            case "exclusion", "exclude" -> handleExclusionCommand(player, args);
            case "stats" -> handleStatsCommand(player);
            case "admin" -> handleAdminCommand(player, args);
            case "reload" -> handleReloadCommand(player);
            case "help" -> handleHelpCommand(player);
            default -> {
                messageManager.sendMessage(player, "commands.ombre.unknown-subcommand");
                yield true;
            }
        };
    }
    
    /**
     * 處理 /ombre 指令
     */
    private boolean handleOmbreCommand(Player player) {
        if (!player.hasPermission("ombre.use")) {
            messageManager.sendMessage(player, "general.no-permission");
            return true;
        }
        
        guiManager.openOmbreGUI(player);
        return true;
    }
    
    /**
     * 處理 /ombre library 指令
     */
    private boolean handleLibraryCommand(Player player) {
        if (!player.hasPermission("ombre.library")) {
            messageManager.sendMessage(player, "general.no-permission");
            return true;
        }
        
        guiManager.openLibraryGUI(player);
        return true;
    }
    
    /**
     * 處理 /ombre favorites 指令
     */
    private boolean handleFavoritesCommand(Player player) {
        if (!player.hasPermission("ombre.use")) {
            messageManager.sendMessage(player, "general.no-permission");
            return true;
        }
        
        guiManager.openFavoritesGUI(player);
        return true;
    }
    
    /**
     * 處理 /ombre my 指令
     */
    private boolean handleMyGradientsCommand(Player player) {
        if (!player.hasPermission("ombre.use")) {
            messageManager.sendMessage(player, "general.no-permission");
            return true;
        }
        
        guiManager.openMyGradientsGUI(player);
        return true;
    }
    
    /**
     * 處理 /ombre palette 指令
     */
    private boolean handlePaletteCommand(Player player, String[] args) {
        if (!player.hasPermission("ombre.use")) {
            messageManager.sendMessage(player, "general.no-permission");
            return true;
        }
        
        var blockFilterManager = plugin.getBlockFilterManager();
        
        // /ombre palette - 列出所有色表
        if (args.length == 1) {
            messageManager.sendMessage(player, "messages.palette.list-title");
            
            var palettes = blockFilterManager.getColorPalettes();
            var playerPalettes = blockFilterManager.getPlayerPalettes(player.getUniqueId());
            
            if (palettes.isEmpty()) {
                messageManager.sendMessage(player, "messages.palette.none");
            } else {
                for (var palette : palettes.values()) {
                    boolean enabled = playerPalettes.contains(palette.getId());
                    String messageKey = enabled ? "messages.palette.entry-enabled" : "messages.palette.entry-disabled";
                    messageManager.sendMessage(player, messageKey,
                        "name", palette.getName(),
                        "description", palette.getDescription());
                }
            }
            
            messageManager.sendMessage(player, "messages.palette.usage");
            messageManager.sendMessage(player, "messages.palette.usage-reset");
            return true;
        }
        
        String action = args[1].toLowerCase();
        switch (action) {
            case "enable" -> {
                if (args.length < 3) {
                    messageManager.sendMessage(player, "messages.palette.usage");
                    return true;
                }
                String paletteId = args[2];
                if (blockFilterManager.getColorPalettes().containsKey(paletteId)) {
                    blockFilterManager.enablePaletteForPlayer(player.getUniqueId(), paletteId);
                    messageManager.sendMessage(player, "messages.palette.enabled", "palette", paletteId);
                } else {
                    messageManager.sendMessage(player, "messages.palette.not-found", "palette", paletteId);
                }
                return true;
            }
            case "disable" -> {
                if (args.length < 3) {
                    messageManager.sendMessage(player, "messages.palette.usage");
                    return true;
                }
                String paletteId = args[2];
                blockFilterManager.disablePaletteForPlayer(player.getUniqueId(), paletteId);
                messageManager.sendMessage(player, "messages.palette.disabled", "palette", paletteId);
                return true;
            }
            case "reset" -> {
                blockFilterManager.resetPalettesForPlayer(player.getUniqueId());
                messageManager.sendMessage(player, "messages.palette.reset");
                return true;
            }
            default -> {
                messageManager.sendMessage(player, "messages.palette.usage");
                return true;
            }
        }
    }
    
    /**
     * 處理 /ombre exclusion 指令
     */
    private boolean handleExclusionCommand(Player player, String[] args) {
        if (!player.hasPermission("ombre.use")) {
            messageManager.sendMessage(player, "general.no-permission");
            return true;
        }
        
        var blockFilterManager = plugin.getBlockFilterManager();
        
        // /ombre exclusion - 列出所有排除列表
        if (args.length == 1) {
            messageManager.sendMessage(player, "messages.exclusion.list-title");
            
            var exclusions = blockFilterManager.getExclusionLists();
            var playerExclusions = blockFilterManager.getPlayerExclusions(player.getUniqueId());
            
            if (exclusions.isEmpty()) {
                messageManager.sendMessage(player, "messages.exclusion.none");
            } else {
                for (var exclusion : exclusions.values()) {
                    boolean enabled = playerExclusions.contains(exclusion.getId());
                    String messageKey = enabled ? "messages.exclusion.entry-enabled" : "messages.exclusion.entry-disabled";
                    messageManager.sendMessage(player, messageKey,
                        "name", exclusion.getName(),
                        "description", exclusion.getDescription());
                }
            }
            
            messageManager.sendMessage(player, "messages.exclusion.usage");
            return true;
        }
        
        String action = args[1].toLowerCase();
        switch (action) {
            case "enable" -> {
                if (args.length < 3) {
                    messageManager.sendMessage(player, "messages.exclusion.usage");
                    return true;
                }
                String exclusionId = args[2];
                if (blockFilterManager.getExclusionLists().containsKey(exclusionId)) {
                    blockFilterManager.enableExclusionForPlayer(player.getUniqueId(), exclusionId);
                    messageManager.sendMessage(player, "messages.exclusion.enabled", "exclusion", exclusionId);
                } else {
                    messageManager.sendMessage(player, "messages.exclusion.not-found", "exclusion", exclusionId);
                }
                return true;
            }
            case "disable" -> {
                if (args.length < 3) {
                    messageManager.sendMessage(player, "messages.exclusion.usage");
                    return true;
                }
                String exclusionId = args[2];
                blockFilterManager.disableExclusionForPlayer(player.getUniqueId(), exclusionId);
                messageManager.sendMessage(player, "messages.exclusion.disabled", "exclusion", exclusionId);
                return true;
            }
            default -> {
                messageManager.sendMessage(player, "messages.exclusion.usage");
                return true;
            }
        }
    }
    
    /**
     * 處理 /ombre stats 指令
     */
    private boolean handleStatsCommand(Player player) {
        if (!player.hasPermission("ombre.use")) {
            messageManager.sendMessage(player, "general.no-permission");
            return true;
        }
        
        int gradientCount = configManager.getPlayerGradientCount(player.getUniqueId());
        
        messageManager.sendMessage(player, "messages.stats.title");
        messageManager.sendMessage(player, "messages.stats.gradient-count", "count", gradientCount);
        // TODO: 添加更多統計數據
        
        return true;
    }
    
    /**
     * 處理 /ombre admin 指令
     */
    private boolean handleAdminCommand(Player player, String[] args) {
        if (!player.hasPermission("ombre.admin")) {
            messageManager.sendMessage(player, "commands.ombre.admin.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            messageManager.sendMessage(player, "commands.ombre.admin.usage");
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        if ("delete".equals(action) && args.length >= 3) {
            String target = args[2];
            
            // 嘗試解析為 UUID（配置ID）
            try {
                UUID configId = UUID.fromString(target);
                if (configManager.deleteGradientById(configId)) {
                    messageManager.sendMessage(player, "commands.ombre.admin.delete-config-success",
                        "configId", target);
                } else {
                    messageManager.sendMessage(player, "commands.ombre.admin.config-not-found",
                        "configId", target);
                }
                return true;
            } catch (IllegalArgumentException e) {
                // 不是 UUID，當作玩家名稱處理
                Player targetPlayer = plugin.getServer().getPlayer(target);
                if (targetPlayer != null) {
                    int deletedCount = configManager.deletePlayerGradients(targetPlayer.getUniqueId());
                    messageManager.sendMessage(player, "commands.ombre.admin.delete-success",
                        "player", target,
                        "count", deletedCount);
                } else {
                    messageManager.sendMessage(player, "commands.ombre.admin.player-not-found",
                        "player", target);
                }
                return true;
            }
        }
        
        messageManager.sendMessage(player, "commands.ombre.admin.usage");
        return true;
    }
    
    /**
     * 處理 /ombre reload 指令
     */
    private boolean handleReloadCommand(Player player) {
        if (!player.hasPermission("ombre.admin")) {
            messageManager.sendMessage(player, "commands.ombre.admin.no-permission");
            return true;
        }
        
        plugin.reloadConfig();
        plugin.getColorService().reload();
        messageManager.sendMessage(player, "general.reload-success");
        return true;
    }
    
    /**
     * 處理 /ombre help 指令
     */
    private boolean handleHelpCommand(Player player) {
        String[] baseHelpKeys = {
            "commands.ombre.help.title",
            "commands.ombre.help.main",
            "commands.ombre.help.library",
            "commands.ombre.help.favorites",
            "commands.ombre.help.my",
            "commands.ombre.help.palette",
            "commands.ombre.help.exclusion",
            "commands.ombre.help.stats"
        };
        for (String key : baseHelpKeys) {
            player.sendMessage(messageManager.getComponent(key));
        }
        
        if (player.hasPermission("ombre.admin")) {
            player.sendMessage(messageManager.getComponent("commands.ombre.help.admin"));
            player.sendMessage(messageManager.getComponent("commands.ombre.help.reload"));
        }
        
        return true;
    }
    
    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        switch (args.length) {
            case 1 -> {
                List<String> subCommands = new ArrayList<>(Arrays.asList("library", "lib", "favorites", "fav",
                    "my", "list", "palette", "exclusion", "exclude", "stats", "help"));
                if (sender.hasPermission("ombre.admin")) {
                    subCommands.addAll(Arrays.asList("admin", "reload"));
                }
                String input = args[0].toLowerCase();
                for (String sub : subCommands) {
                    if (sub.startsWith(input)) {
                        completions.add(sub);
                    }
                }
            }
            case 2 -> {
                String subCommand = args[0].toLowerCase();
                if ("admin".equalsIgnoreCase(subCommand) && sender.hasPermission("ombre.admin")) {
                    completions.add("delete");
                } else if ("palette".equalsIgnoreCase(subCommand)) {
                    completions.addAll(Arrays.asList("enable", "disable", "reset"));
                } else if ("exclusion".equalsIgnoreCase(subCommand) || "exclude".equalsIgnoreCase(subCommand)) {
                    completions.addAll(Arrays.asList("enable", "disable"));
                }
            }
            case 3 -> {
                String subCommand = args[0].toLowerCase();
                String action = args[1].toLowerCase();
                var blockFilterManager = plugin.getBlockFilterManager();
                if ("palette".equalsIgnoreCase(subCommand) &&
                    ("enable".equalsIgnoreCase(action) || "disable".equalsIgnoreCase(action))) {
                    completions.addAll(blockFilterManager.getColorPalettes().keySet());
                } else if (("exclusion".equalsIgnoreCase(subCommand) || "exclude".equalsIgnoreCase(subCommand)) &&
                    ("enable".equalsIgnoreCase(action) || "disable".equalsIgnoreCase(action))) {
                    completions.addAll(blockFilterManager.getExclusionLists().keySet());
                }
            }
            default -> {
            }
        }
        return completions;
    }
}
