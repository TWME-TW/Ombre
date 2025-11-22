package dev.twme.ombre.blockpalettes.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.blockpalettes.BlockPalettesFeature;
import dev.twme.ombre.blockpalettes.gui.FavoritesGUI;
import dev.twme.ombre.blockpalettes.gui.PalettesListGUI;
import dev.twme.ombre.blockpalettes.gui.TermsAcceptanceGUI;
import dev.twme.ombre.i18n.MessageManager;

/**
 * Block Palettes 指令處理器
 * 處理 /blockpalettes 和 /bp 指令
 */
public class BlockPalettesCommand implements CommandExecutor, TabCompleter {
    
    private final Ombre plugin;
    private final BlockPalettesFeature feature;
    private final MessageManager messageManager;
    
    public BlockPalettesCommand(Ombre plugin, BlockPalettesFeature feature) {
        this.plugin = plugin;
        this.feature = feature;
        this.messageManager = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getComponent("blockpalettes.command.player-only"));
            return true;
        }
        
        // 檢查功能是否啟用
        if (!feature.isEnabled()) {
            player.sendMessage(messageManager.getComponent("blockpalettes.command.feature-disabled"));
            return true;
        }
        
        // 無參數 - 開啟主介面
        if (args.length == 0) {
            openMainGUI(player);
            return true;
        }
        
        // 子指令處理
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "reload" -> handleReload(player);
            case "cache" -> handleCache(player);
            case "terms" -> handleTerms(player, args);
            case "favorites", "fav" -> handleFavorites(player);
            case "help" -> handleHelp(player);
            default -> {
                player.sendMessage(messageManager.getComponent("blockpalettes.command.unknown-subcommand", "subcommand", subCommand));
                player.sendMessage(messageManager.getComponent("blockpalettes.command.use-help"));
                yield true;
            }
        };
    }
    
    /**
     * 開啟主介面
     */
    private void openMainGUI(Player player) {
        // 檢查是否已同意條款
        if (!feature.getTermsTracker().hasAgreed(player.getUniqueId())) {
            // 開啟條款同意介面
            TermsAcceptanceGUI termsGUI = new TermsAcceptanceGUI(
                plugin,
                messageManager,
                player,
                () -> {
                    // 同意後的回調
                    String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
                    feature.getTermsTracker().recordAgreement(player.getUniqueId(), ip);
                    
                    // 延遲開啟主介面
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        PalettesListGUI listGUI = new PalettesListGUI(plugin, feature, player);
                        Bukkit.getPluginManager().registerEvents(listGUI, plugin);
                        listGUI.open();
                    }, 10L);
                },
                () -> {
                    // 不同意後的回調 (什麼都不做)
                }
            );
            
            Bukkit.getPluginManager().registerEvents(termsGUI, plugin);
            termsGUI.show();
            return;
        }
        
        // 已同意條款，直接開啟主介面
        PalettesListGUI listGUI = new PalettesListGUI(plugin, feature, player);
        Bukkit.getPluginManager().registerEvents(listGUI, plugin);
        listGUI.open();
    }
    
    /**
     * 處理 reload 子指令
     */
    private boolean handleReload(Player player) {
        if (!player.hasPermission("ombre.blockpalettes.reload")) {
            player.sendMessage(messageManager.getComponent("blockpalettes.command.no-permission"));
            return true;
        }
        
        feature.getCache().clearAll();
        player.sendMessage(messageManager.getComponent("blockpalettes.command.reload-success"));
        return true;
    }
    
    /**
     * 處理 cache 子指令
     */
    private boolean handleCache(Player player) {
        if (!player.hasPermission("ombre.blockpalettes.reload")) {
            player.sendMessage(messageManager.getComponent("blockpalettes.command.no-permission"));
            return true;
        }
        
        var stats = feature.getCache().getStatistics();
        
        player.sendMessage(messageManager.getComponent("blockpalettes.command.cache.header"));
        player.sendMessage(messageManager.getComponent("blockpalettes.command.cache.list-size", "count", stats.get("list_cache_size").toString()));
        player.sendMessage(messageManager.getComponent("blockpalettes.command.cache.detail-size", "count", stats.get("detail_cache_size").toString()));
        player.sendMessage(messageManager.getComponent("blockpalettes.command.cache.footer"));
        
        return true;
    }
    
    /**
     * 處理 terms 子指令
     */
    private boolean handleTerms(Player player, String[] args) {
        if (args.length < 2) {
            var stats = feature.getTermsTracker().getStatistics();
            
            player.sendMessage(messageManager.getComponent("blockpalettes.command.terms.header"));
            player.sendMessage(messageManager.getComponent("blockpalettes.command.terms.version", "version", stats.get("current_version").toString()));
            player.sendMessage(messageManager.getComponent("blockpalettes.command.terms.agreed-count", "count", stats.get("total_agreed").toString()));
            player.sendMessage(messageManager.getComponent("blockpalettes.command.terms.footer"));
            return true;
        }
        
        String subCmd = args[1].toLowerCase();
        
        if ("reset".equals(subCmd) && player.hasPermission("ombre.blockpalettes.terms.manage")) {
            if (args.length < 3) {
                player.sendMessage(messageManager.getComponent("blockpalettes.command.terms.reset-usage"));
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                player.sendMessage(messageManager.getComponent("blockpalettes.command.player-not-found", "player", args[2]));
                return true;
            }
            
            feature.getTermsTracker().revokeAgreement(target.getUniqueId());
            player.sendMessage(messageManager.getComponent("blockpalettes.command.terms.reset-success", "player", target.getName()));
            return true;
        }
        
        return true;
    }
    
    /**
     * 處理 favorites 子指令
     */
    private boolean handleFavorites(Player player) {
        int count = feature.getFavoritesManager().getFavoriteCount(player.getUniqueId());
        
        if (count == 0) {
            player.sendMessage(messageManager.getComponent("blockpalettes.command.favorites.empty"));
            player.sendMessage(messageManager.getComponent("blockpalettes.command.favorites.hint"));
            return true;
        }
        
        // 開啟收藏 GUI
        FavoritesGUI favoritesGUI = new FavoritesGUI(plugin, feature, player);
        Bukkit.getPluginManager().registerEvents(favoritesGUI, plugin);
        favoritesGUI.open();
        
        return true;
    }
    
    /**
     * 處理 help 子指令
     */
    private boolean handleHelp(Player player) {
        player.sendMessage(messageManager.getComponent("blockpalettes.command.help.header"));
        player.sendMessage(messageManager.getComponent("blockpalettes.command.help.main"));
        player.sendMessage(messageManager.getComponent("blockpalettes.command.help.favorites"));
        player.sendMessage(messageManager.getComponent("blockpalettes.command.help.reload"));
        player.sendMessage(messageManager.getComponent("blockpalettes.command.help.cache"));
        player.sendMessage(messageManager.getComponent("blockpalettes.command.help.terms"));
        player.sendMessage(messageManager.getComponent("blockpalettes.command.help.footer"));
        return true;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(List.of("reload", "cache", "terms", "favorites", "help"));
        } else if (args.length == 2 && "terms".equals(args[0].toLowerCase())) {
            if (sender.hasPermission("ombre.blockpalettes.terms.manage")) {
                completions.add("reset");
            }
        }
        
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .toList();
    }
}
