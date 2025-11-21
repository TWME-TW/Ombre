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
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.cache.CacheStats;

import dev.twme.ombre.blockcolors.BlockColorsFeature;
import dev.twme.ombre.blockcolors.ColorMatcher;
import dev.twme.ombre.blockcolors.TermsTracker;
import dev.twme.ombre.blockcolors.gui.BlockColorsGUI;

/**
 * BlockColors 指令處理器
 * 處理 /blockcolorsapp (/bca) 指令
 */
public class BlockColorsCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final BlockColorsFeature feature;

    public BlockColorsCommand(JavaPlugin plugin, BlockColorsFeature feature) {
        this.plugin = plugin;
        this.feature = feature;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 檢查功能是否已初始化
        if (!feature.isInitialized()) {
            sender.sendMessage("§cBlockColors 功能尚未初始化完成，請稍後再試");
            return true;
        }

        // 主指令 - 開啟 GUI（僅玩家）
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c此指令只能由玩家執行");
                return true;
            }

            Player player = (Player) sender;
            
            // 檢查權限
            if (!player.hasPermission("ombre.blockcolorsapp.use")) {
                player.sendMessage("§c你沒有權限使用此功能");
                return true;
            }

            // 檢查是否已接受條款
            if (!feature.getTermsTracker().hasAcceptedTerms(player.getUniqueId())) {
                showTermsOfUse(player);
                return true;
            }

            // 開啟 BlockColors GUI
            openBlockColorsGUI(player);
            return true;
        }

        // 子指令
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);

            case "cache":
                return handleCacheInfo(sender);

            case "clear-cache":
                return handleClearCache(sender);

            default:
                sender.sendMessage("§c未知的子指令: " + subCommand);
                sender.sendMessage("§e用法: /bca [reload|cache|clear-cache]");
                return true;
        }
    }

    /**
     * 顯示使用條款
     */
    private void showTermsOfUse(Player player) {
        // 關閉任何開啟的 GUI
        player.closeInventory();
        
        // 顯示條款內容
        String[] termsText = TermsTracker.getTermsText();
        for (String line : termsText) {
            player.sendMessage(line);
        }
        
        // 標記玩家正在等待回應
        feature.markPendingTermsAcceptance(player.getUniqueId());
        
        // 60秒後自動清理
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (feature.isPendingTermsAcceptance(player.getUniqueId())) {
                player.sendMessage("§c回應超時，請重新執行指令");
                feature.removePendingTermsAcceptance(player.getUniqueId());
            }
        }, 20L * 60);  // 60秒
    }

    /**
     * 開啟 BlockColors GUI
     */
    private void openBlockColorsGUI(Player player) {
        BlockColorsGUI gui = new BlockColorsGUI(feature, player);
        gui.open();
    }

    /**
     * 處理 reload 子指令
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("ombre.blockcolorsapp.reload")) {
            sender.sendMessage("§c你沒有權限執行此指令");
            return true;
        }

        sender.sendMessage("§e正在重新載入 BlockColors...");
        
        feature.reload().thenAccept(success -> {
            if (success) {
                sender.sendMessage("§aBlockColors 重新載入成功！");
            } else {
                sender.sendMessage("§cBlockColors 重新載入失敗，請查看控制台");
            }
        });

        return true;
    }

    /**
     * 處理 cache 子指令 - 顯示快取資訊
     */
    private boolean handleCacheInfo(CommandSender sender) {
        if (!sender.hasPermission("ombre.blockcolorsapp.admin")) {
            sender.sendMessage("§c你沒有權限執行此指令");
            return true;
        }

        sender.sendMessage("§6§l===== BlockColors 快取資訊 =====");
        sender.sendMessage("§e總方塊數: §f" + feature.getCache().getTotalBlocks());
        
        long lastUpdate = feature.getCache().getLastUpdateTime();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastUpdate));
        sender.sendMessage("§e最後更新: §f" + dateStr);
        
        sender.sendMessage("§e快取版本: §f" + feature.getCache().getCacheVersion());
        
        // 顯示匹配快取統計
        CacheStats stats = ColorMatcher.getCacheStats();
        if (stats != null) {
            sender.sendMessage("");
            sender.sendMessage("§6§l===== 顏色匹配快取統計 =====");
            sender.sendMessage(String.format("§e命中率: §f%.2f%%", stats.hitRate() * 100));
            sender.sendMessage("§e請求總數: §f" + stats.requestCount());
            sender.sendMessage("§e命中次數: §f" + stats.hitCount());
            sender.sendMessage("§e未命中次數: §f" + stats.missCount());
        }

        return true;
    }

    /**
     * 處理 clear-cache 子指令
     */
    private boolean handleClearCache(CommandSender sender) {
        if (!sender.hasPermission("ombre.blockcolorsapp.admin")) {
            sender.sendMessage("§c你沒有權限執行此指令");
            return true;
        }

        sender.sendMessage("§e正在清除快取...");
        
        feature.getCache().clearCache();
        ColorMatcher.clearCache();
        
        sender.sendMessage("§a快取已清除！請執行 /bca reload 重新載入");

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
