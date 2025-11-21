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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Block Palettes 指令處理器
 * 處理 /blockpalettes 和 /bp 指令
 */
public class BlockPalettesCommand implements CommandExecutor, TabCompleter {
    
    private final Ombre plugin;
    private final BlockPalettesFeature feature;
    
    public BlockPalettesCommand(Ombre plugin, BlockPalettesFeature feature) {
        this.plugin = plugin;
        this.feature = feature;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此指令只能由玩家執行").color(NamedTextColor.RED));
            return true;
        }
        
        // 檢查功能是否啟用
        if (!feature.isEnabled()) {
            player.sendMessage(Component.text("Block Palettes 功能目前已停用").color(NamedTextColor.RED));
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
                player.sendMessage(Component.text("未知的子指令: " + subCommand).color(NamedTextColor.RED));
                player.sendMessage(Component.text("使用 /bp help 查看幫助").color(NamedTextColor.GRAY));
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
            termsGUI.open();
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
            player.sendMessage(Component.text("你沒有權限執行此指令").color(NamedTextColor.RED));
            return true;
        }
        
        feature.getCache().clearAll();
        player.sendMessage(Component.text("✓ 已重新載入 Block Palettes 快取").color(NamedTextColor.GREEN));
        return true;
    }
    
    /**
     * 處理 cache 子指令
     */
    private boolean handleCache(Player player) {
        if (!player.hasPermission("ombre.blockpalettes.reload")) {
            player.sendMessage(Component.text("你沒有權限執行此指令").color(NamedTextColor.RED));
            return true;
        }
        
        var stats = feature.getCache().getStatistics();
        
        player.sendMessage(Component.text("━━━━ 快取統計 ━━━━").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("列表快取數量: ").color(NamedTextColor.GRAY)
            .append(Component.text(stats.get("list_cache_size").toString()).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("詳細快取數量: ").color(NamedTextColor.GRAY)
            .append(Component.text(stats.get("detail_cache_size").toString()).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
        
        return true;
    }
    
    /**
     * 處理 terms 子指令
     */
    private boolean handleTerms(Player player, String[] args) {
        if (args.length < 2) {
            var stats = feature.getTermsTracker().getStatistics();
            
            player.sendMessage(Component.text("━━━━ 條款資訊 ━━━━").color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("當前版本: ").color(NamedTextColor.GRAY)
                .append(Component.text(stats.get("current_version").toString()).color(NamedTextColor.WHITE)));
            player.sendMessage(Component.text("已同意人數: ").color(NamedTextColor.GRAY)
                .append(Component.text(stats.get("total_agreed").toString()).color(NamedTextColor.WHITE)));
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
            return true;
        }
        
        String subCmd = args[1].toLowerCase();
        
        if ("reset".equals(subCmd) && player.hasPermission("ombre.blockpalettes.terms.manage")) {
            if (args.length < 3) {
                player.sendMessage(Component.text("用法: /bp terms reset <玩家>").color(NamedTextColor.RED));
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                player.sendMessage(Component.text("找不到玩家: " + args[2]).color(NamedTextColor.RED));
                return true;
            }
            
            feature.getTermsTracker().revokeAgreement(target.getUniqueId());
            player.sendMessage(Component.text("✓ 已重置玩家 " + target.getName() + " 的條款同意狀態").color(NamedTextColor.GREEN));
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
            player.sendMessage(Component.text("你還沒有收藏任何調色板").color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("使用 /bp 瀏覽並收藏喜歡的調色板").color(NamedTextColor.GRAY));
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
        player.sendMessage(Component.text("━━━━ Block Palettes 幫助 ━━━━").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/bp").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 開啟調色板瀏覽介面").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/bp favorites").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 查看收藏的調色板").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/bp reload").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 重新載入快取 (需要權限)").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/bp cache").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 查看快取統計 (需要權限)").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/bp terms").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 查看條款資訊").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
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
