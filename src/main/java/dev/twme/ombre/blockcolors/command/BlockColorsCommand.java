package dev.twme.ombre.blockcolors.command;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.google.common.cache.CacheStats;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.blockcolors.BlockColorsFeature;
import dev.twme.ombre.blockcolors.ColorMatcher;
import dev.twme.ombre.blockcolors.gui.BlockColorsGUI;
import dev.twme.ombre.i18n.MessageManager;
import net.kyori.adventure.text.Component;

/**
 * BlockColors 指令處理器
 * 處理 /blockcolorsapp (/bca) 指令
 */
public class BlockColorsCommand implements CommandExecutor, TabCompleter {
    private final Ombre plugin;
    private final BlockColorsFeature feature;
    private final MessageManager msg;

    public BlockColorsCommand(Ombre plugin, BlockColorsFeature feature) {
        this.plugin = plugin;
        this.feature = feature;
        this.msg = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if feature is initialized
        if (!feature.isInitialized()) {
            if (sender instanceof Player) {
                sender.sendMessage(msg.getMessage("commands.blockcolors.not-initialized", (Player) sender));
            } else {
                sender.sendMessage(msg.getMessage("commands.blockcolors.not-initialized"));
            }
            return true;
        }

        // Main command - open GUI (players only)
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(msg.getMessage("general.player-only"));
                return true;
            }

            Player player = (Player) sender;
            
            // Check permission
            if (!player.hasPermission("ombre.blockcolorsapp.use")) {
                msg.sendMessage(player, "general.no-permission");
                return true;
            }

            // Check if player has accepted terms
            if (!feature.getTermsTracker().hasAcceptedTerms(player.getUniqueId())) {
                showTermsOfUse(player);
                return true;
            }

            // Open BlockColors GUI
            openBlockColorsGUI(player);
            return true;
        }

        // Subcommands
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);

            case "cache":
                return handleCacheInfo(sender);

            case "clear-cache":
                return handleClearCache(sender);

            default:
                if (sender instanceof Player) {
                    sender.sendMessage(msg.getMessage("general.unknown-command", (Player) sender));
                } else {
                    sender.sendMessage(msg.getMessage("general.unknown-command"));
                }
                sender.sendMessage("<yellow>Usage: /bca [reload|cache|clear-cache]</yellow>");
                return true;
        }
    }

    /**
     * Show terms of use
     */
    private void showTermsOfUse(Player player) {
        // Close any open GUI
        player.closeInventory();
        
        // Display terms content using Components for proper MiniMessage parsing
        MessageManager msg = plugin.getMessageManager();
        player.sendMessage(msg.getComponent("terms.blockcolors.title"));
        player.sendMessage(msg.getComponent("terms.blockcolors.subtitle"));
        player.sendMessage(msg.getComponent("terms.blockcolors.separator"));
        player.sendMessage(Component.empty());
        player.sendMessage(msg.getComponent("terms.blockcolors.line1"));
        player.sendMessage(Component.empty());
        player.sendMessage(msg.getComponent("terms.blockcolors.line2"));
        player.sendMessage(msg.getComponent("terms.blockcolors.website"));
        player.sendMessage(Component.empty());
        player.sendMessage(msg.getComponent("terms.blockcolors.data-source"));
        player.sendMessage(msg.getComponent("terms.blockcolors.author"));
        player.sendMessage(Component.empty());
        player.sendMessage(msg.getComponent("terms.blockcolors.accept"));
        player.sendMessage(msg.getComponent("terms.blockcolors.decline"));
        player.sendMessage(Component.empty());
        player.sendMessage(msg.getComponent("terms.blockcolors.timeout"));
        
        // Mark player as pending terms acceptance
        feature.markPendingTermsAcceptance(player.getUniqueId());
        
        // Auto-cleanup after 60 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (feature.isPendingTermsAcceptance(player.getUniqueId())) {
                msg.sendMessage(player, "terms.blockcolors.timeout-message");
                feature.removePendingTermsAcceptance(player.getUniqueId());
            }
        }, 20L * 60);  // 60 seconds
    }

    /**
     * Open BlockColors GUI
     */
    private void openBlockColorsGUI(Player player) {
        BlockColorsGUI gui = new BlockColorsGUI(feature, player);
        gui.open();
    }

    /**
     * Handle reload subcommand
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("ombre.blockcolorsapp.reload")) {
            if (sender instanceof Player) {
                sender.sendMessage(msg.getMessage("general.no-permission", (Player) sender));
            } else {
                sender.sendMessage(msg.getMessage("general.no-permission"));
            }
            return true;
        }

        sender.sendMessage("<yellow>Reloading BlockColors...</yellow>");
        
        feature.reload().thenAccept(success -> {
            if (success) {
                if (sender instanceof Player) {
                    sender.sendMessage(msg.getMessage("commands.blockcolors.reload-success", (Player) sender));
                } else {
                    sender.sendMessage(msg.getMessage("commands.blockcolors.reload-success"));
                }
            } else {
                if (sender instanceof Player) {
                    sender.sendMessage(msg.getMessage("commands.blockcolors.reload-fail", (Player) sender));
                } else {
                    sender.sendMessage(msg.getMessage("commands.blockcolors.reload-fail"));
                }
            }
        });

        return true;
    }

    /**
     * Handle cache subcommand - display cache information
     */
    private boolean handleCacheInfo(CommandSender sender) {
        if (!sender.hasPermission("ombre.blockcolorsapp.admin")) {
            if (sender instanceof Player) {
                sender.sendMessage(msg.getMessage("general.no-permission", (Player) sender));
            } else {
                sender.sendMessage(msg.getMessage("general.no-permission"));
            }
            return true;
        }

        Player player = sender instanceof Player ? (Player) sender : null;
        
        if (player != null) {
            sender.sendMessage(msg.getMessage("messages.cache.title", player));
            sender.sendMessage(msg.getMessage("messages.cache.total-blocks", player, "count", feature.getCache().getTotalBlocks()));
            
            long lastUpdate = feature.getCache().getLastUpdateTime();
            String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastUpdate));
            sender.sendMessage(msg.getMessage("messages.cache.last-update", player, "date", dateStr));
            
            sender.sendMessage(msg.getMessage("messages.cache.version", player, "version", feature.getCache().getCacheVersion()));
        } else {
            // Console: use English hardcoded
            sender.sendMessage("§6=== Cache Information ===");
            sender.sendMessage("§7Total Blocks: §f" + feature.getCache().getTotalBlocks());
            
            long lastUpdate = feature.getCache().getLastUpdateTime();
            String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastUpdate));
            sender.sendMessage("§7Last Update: §f" + dateStr);
            
            sender.sendMessage("§7Cache Version: §f" + feature.getCache().getCacheVersion());
        }
        
        // Display color matching cache statistics
        CacheStats stats = ColorMatcher.getCacheStats();
        if (stats != null) {
            sender.sendMessage("");
            if (player != null) {
                sender.sendMessage(msg.getMessage("messages.cache.stats-title", player));
                sender.sendMessage(msg.getMessage("messages.cache.hit-rate", player, "rate", String.format("%.2f", stats.hitRate() * 100)));
                sender.sendMessage(msg.getMessage("messages.cache.requests", player, "count", stats.requestCount()));
                sender.sendMessage(msg.getMessage("messages.cache.hits", player, "count", stats.hitCount()));
                sender.sendMessage(msg.getMessage("messages.cache.misses", player, "count", stats.missCount()));
            } else {
                // Console: use English hardcoded
                sender.sendMessage("§6=== Cache Statistics ===");
                sender.sendMessage("§7Hit Rate: §f" + String.format("%.2f", stats.hitRate() * 100) + "%");
                sender.sendMessage("§7Requests: §f" + stats.requestCount());
                sender.sendMessage("§7Hits: §f" + stats.hitCount());
                sender.sendMessage("§7Misses: §f" + stats.missCount());
            }
        }

        return true;
    }

    /**
     * Handle clear-cache subcommand
     */
    private boolean handleClearCache(CommandSender sender) {
        if (!sender.hasPermission("ombre.blockcolorsapp.admin")) {
            if (sender instanceof Player) {
                sender.sendMessage(msg.getMessage("general.no-permission", (Player) sender));
            } else {
                sender.sendMessage(msg.getMessage("general.no-permission"));
            }
            return true;
        }

        sender.sendMessage("<yellow>Clearing cache...</yellow>");
        
        feature.getCache().clearCache();
        ColorMatcher.clearCache();
        
        if (sender instanceof Player) {
            sender.sendMessage(msg.getMessage("commands.blockcolors.cache-cleared", (Player) sender));
        } else {
            sender.sendMessage(msg.getMessage("commands.blockcolors.cache-cleared"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 第一層子指令
            if (sender.hasPermission("ombre.blockcolorsapp.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("ombre.blockcolorsapp.admin")) {
                completions.add("cache");
                completions.add("clear-cache");
            }

            // 過濾匹配的補全
            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        }

        return completions;
    }
}
