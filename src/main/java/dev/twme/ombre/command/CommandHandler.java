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

import dev.twme.ombre.Ombre;
import dev.twme.ombre.gui.GUIManager;
import dev.twme.ombre.manager.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * 指令處理器
 * 處理所有 /ombre 相關指令
 */
public class CommandHandler implements CommandExecutor, TabCompleter {
    
    private final Ombre plugin;
    private final GUIManager guiManager;
    private final ConfigManager configManager;
    
    public CommandHandler(Ombre plugin, GUIManager guiManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.configManager = configManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此指令只能由玩家執行").color(NamedTextColor.RED));
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
                player.sendMessage(Component.text("未知的子指令。使用 /ombre help 查看幫助")
                    .color(NamedTextColor.RED));
                yield true;
            }
        };
    }
    
    /**
     * 處理 /ombre 指令
     */
    private boolean handleOmbreCommand(Player player) {
        if (!player.hasPermission("ombre.use")) {
            player.sendMessage(Component.text("你沒有權限使用此指令").color(NamedTextColor.RED));
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
            player.sendMessage(Component.text("你沒有權限訪問共享庫").color(NamedTextColor.RED));
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
            player.sendMessage(Component.text("你沒有權限使用此指令").color(NamedTextColor.RED));
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
            player.sendMessage(Component.text("你沒有權限使用此指令").color(NamedTextColor.RED));
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
            player.sendMessage(Component.text("你沒有權限使用此指令").color(NamedTextColor.RED));
            return true;
        }
        
        var blockFilterManager = plugin.getBlockFilterManager();
        
        // /ombre palette - 列出所有色表
        if (args.length == 1) {
            player.sendMessage(Component.text("=== 可用的色表 ===").color(NamedTextColor.GOLD));
            
            var palettes = blockFilterManager.getColorPalettes();
            var playerPalettes = blockFilterManager.getPlayerPalettes(player.getUniqueId());
            
            if (palettes.isEmpty()) {
                player.sendMessage(Component.text("沒有可用的色表").color(NamedTextColor.GRAY));
            } else {
                for (var palette : palettes.values()) {
                    boolean enabled = playerPalettes.contains(palette.getId());
                    NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY;
                    String status = enabled ? "[✓] " : "[  ] ";
                    
                    player.sendMessage(Component.text(status + palette.getName() + " - " + palette.getDescription())
                        .color(color));
                }
            }
            
            player.sendMessage(Component.text("使用 /ombre palette enable/disable <id> 來啟用/停用色表")
                .color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("使用 /ombre palette reset 來重置色表設定")
                .color(NamedTextColor.YELLOW));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        // /ombre palette enable <id>
        if ("enable".equals(action)) {
            if (args.length < 3) {
                player.sendMessage(Component.text("用法: /ombre palette enable <id>")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            String paletteId = args[2];
            if (blockFilterManager.getColorPalettes().containsKey(paletteId)) {
                blockFilterManager.enablePaletteForPlayer(player.getUniqueId(), paletteId);
                player.sendMessage(Component.text("已啟用色表: " + paletteId)
                    .color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("找不到色表: " + paletteId)
                    .color(NamedTextColor.RED));
            }
            return true;
        }
        
        // /ombre palette disable <id>
        if ("disable".equals(action)) {
            if (args.length < 3) {
                player.sendMessage(Component.text("用法: /ombre palette disable <id>")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            String paletteId = args[2];
            blockFilterManager.disablePaletteForPlayer(player.getUniqueId(), paletteId);
            player.sendMessage(Component.text("已停用色表: " + paletteId)
                .color(NamedTextColor.GREEN));
            return true;
        }
        
        // /ombre palette reset
        if ("reset".equals(action)) {
            blockFilterManager.resetPalettesForPlayer(player.getUniqueId());
            player.sendMessage(Component.text("已重置色表設定（將使用所有方塊）")
                .color(NamedTextColor.GREEN));
            return true;
        }
        
        player.sendMessage(Component.text("用法: /ombre palette [enable|disable|reset] <id>")
            .color(NamedTextColor.RED));
        return true;
    }
    
    /**
     * 處理 /ombre exclusion 指令
     */
    private boolean handleExclusionCommand(Player player, String[] args) {
        if (!player.hasPermission("ombre.use")) {
            player.sendMessage(Component.text("你沒有權限使用此指令").color(NamedTextColor.RED));
            return true;
        }
        
        var blockFilterManager = plugin.getBlockFilterManager();
        
        // /ombre exclusion - 列出所有排除列表
        if (args.length == 1) {
            player.sendMessage(Component.text("=== 方塊排除列表 ===").color(NamedTextColor.GOLD));
            
            var exclusions = blockFilterManager.getExclusionLists();
            var playerExclusions = blockFilterManager.getPlayerExclusions(player.getUniqueId());
            
            if (exclusions.isEmpty()) {
                player.sendMessage(Component.text("沒有可用的排除列表").color(NamedTextColor.GRAY));
            } else {
                for (var exclusion : exclusions.values()) {
                    boolean enabled = playerExclusions.contains(exclusion.getId());
                    NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY;
                    String status = enabled ? "[✓] " : "[  ] ";
                    
                    player.sendMessage(Component.text(status + exclusion.getName() + " - " + exclusion.getDescription())
                        .color(color));
                }
            }
            
            player.sendMessage(Component.text("使用 /ombre exclusion enable/disable <id> 來啟用/停用排除列表")
                .color(NamedTextColor.YELLOW));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        // /ombre exclusion enable <id>
        if ("enable".equals(action)) {
            if (args.length < 3) {
                player.sendMessage(Component.text("用法: /ombre exclusion enable <id>")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            String exclusionId = args[2];
            if (blockFilterManager.getExclusionLists().containsKey(exclusionId)) {
                blockFilterManager.enableExclusionForPlayer(player.getUniqueId(), exclusionId);
                player.sendMessage(Component.text("已啟用排除列表: " + exclusionId)
                    .color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("找不到排除列表: " + exclusionId)
                    .color(NamedTextColor.RED));
            }
            return true;
        }
        
        // /ombre exclusion disable <id>
        if ("disable".equals(action)) {
            if (args.length < 3) {
                player.sendMessage(Component.text("用法: /ombre exclusion disable <id>")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            String exclusionId = args[2];
            blockFilterManager.disableExclusionForPlayer(player.getUniqueId(), exclusionId);
            player.sendMessage(Component.text("已停用排除列表: " + exclusionId)
                .color(NamedTextColor.GREEN));
            return true;
        }
        
        player.sendMessage(Component.text("用法: /ombre exclusion [enable|disable] <id>")
            .color(NamedTextColor.RED));
        return true;
    }
    
    /**
     * 處理 /ombre stats 指令
     */
    private boolean handleStatsCommand(Player player) {
        if (!player.hasPermission("ombre.use")) {
            player.sendMessage(Component.text("你沒有權限使用此指令").color(NamedTextColor.RED));
            return true;
        }
        
        int gradientCount = configManager.getPlayerGradientCount(player.getUniqueId());
        
        player.sendMessage(Component.text("=== 你的統計數據 ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("創建數量: " + gradientCount).color(NamedTextColor.YELLOW));
        // TODO: 添加更多統計數據
        
        return true;
    }
    
    /**
     * 處理 /ombre admin 指令
     */
    private boolean handleAdminCommand(Player player, String[] args) {
        if (!player.hasPermission("ombre.admin")) {
            player.sendMessage(Component.text("你沒有管理員權限").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /ombre admin <delete> <玩家名稱|配置ID>")
                .color(NamedTextColor.RED));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        if ("delete".equals(action) && args.length >= 3) {
            String target = args[2];
            
            // 嘗試解析為 UUID（配置ID）
            try {
                UUID configId = UUID.fromString(target);
                player.sendMessage(Component.text("刪除配置功能尚未完全實作").color(NamedTextColor.YELLOW));
                return true;
            } catch (IllegalArgumentException e) {
                // 不是 UUID，當作玩家名稱處理
                Player targetPlayer = plugin.getServer().getPlayer(target);
                if (targetPlayer != null) {
                    int deletedCount = configManager.deletePlayerGradients(targetPlayer.getUniqueId());
                    player.sendMessage(Component.text("已刪除玩家 " + target + " 的 " + deletedCount + " 個配置")
                        .color(NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("找不到玩家: " + target).color(NamedTextColor.RED));
                }
                return true;
            }
        }
        
        player.sendMessage(Component.text("用法: /ombre admin delete <玩家名稱|配置ID>")
            .color(NamedTextColor.RED));
        return true;
    }
    
    /**
     * 處理 /ombre reload 指令
     */
    private boolean handleReloadCommand(Player player) {
        if (!player.hasPermission("ombre.admin")) {
            player.sendMessage(Component.text("你沒有管理員權限").color(NamedTextColor.RED));
            return true;
        }
        
        plugin.reloadConfig();
        plugin.getColorService().reload();
        player.sendMessage(Component.text("配置已重新載入").color(NamedTextColor.GREEN));
        return true;
    }
    
    /**
     * 處理 /ombre help 指令
     */
    private boolean handleHelpCommand(Player player) {
        player.sendMessage(Component.text("=== Ombre 指令幫助 ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/ombre - 打開漸層製作 GUI").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/ombre library - 打開共享庫").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/ombre favorites - 打開我的最愛").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/ombre my - 查看我的漸層配置").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/ombre palette - 管理色表").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/ombre exclusion - 管理方塊排除").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/ombre stats - 查看統計數據").color(NamedTextColor.YELLOW));
        
        if (player.hasPermission("ombre.admin")) {
            player.sendMessage(Component.text("/ombre admin delete <玩家> - 刪除玩家配置")
                .color(NamedTextColor.RED));
            player.sendMessage(Component.text("/ombre reload - 重新載入配置").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("library", "lib", "favorites", "fav", 
                "my", "list", "palette", "exclusion", "exclude", "stats", "help");
            
            if (sender.hasPermission("ombre.admin")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.addAll(Arrays.asList("admin", "reload"));
            }
            
            String input = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if ("admin".equalsIgnoreCase(subCommand) && sender.hasPermission("ombre.admin")) {
                completions.add("delete");
            } else if ("palette".equalsIgnoreCase(subCommand)) {
                completions.addAll(Arrays.asList("enable", "disable", "reset"));
            } else if ("exclusion".equalsIgnoreCase(subCommand) || "exclude".equalsIgnoreCase(subCommand)) {
                completions.addAll(Arrays.asList("enable", "disable"));
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            
            if ("palette".equalsIgnoreCase(subCommand) && 
                ("enable".equalsIgnoreCase(action) || "disable".equalsIgnoreCase(action))) {
                var blockFilterManager = plugin.getBlockFilterManager();
                completions.addAll(blockFilterManager.getColorPalettes().keySet());
            } else if (("exclusion".equalsIgnoreCase(subCommand) || "exclude".equalsIgnoreCase(subCommand)) && 
                ("enable".equalsIgnoreCase(action) || "disable".equalsIgnoreCase(action))) {
                var blockFilterManager = plugin.getBlockFilterManager();
                completions.addAll(blockFilterManager.getExclusionLists().keySet());
            }
        }
        
        return completions;
    }
}
