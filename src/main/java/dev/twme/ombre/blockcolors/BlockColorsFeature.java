package dev.twme.ombre.blockcolors;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.plugin.java.JavaPlugin;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.blockcolors.command.BlockColorsCommand;
import dev.twme.ombre.blockcolors.gui.BlockColorsGUIListener;
import dev.twme.ombre.blockcolors.gui.PaletteListener;
import dev.twme.ombre.blockcolors.gui.TermsAcceptanceListener;

/**
 * BlockColors 功能主類別
 * 管理整個 BlockColors 功能模組
 */
public class BlockColorsFeature {
    private final JavaPlugin plugin;
    private BlockColorCache cache;
    private TermsTracker termsTracker;
    private BlockColorsCommand commandHandler;
    private dev.twme.ombre.blockcolors.data.PaletteDataManager paletteDataManager;
    
    // 玩家調色盤管理
    private final Map<UUID, dev.twme.ombre.blockcolors.data.PlayerPalette> playerPalettes;
    
    // 等待條款回應的玩家
    private final Map<UUID, Long> pendingTermsAcceptance;
    
    private boolean initialized = false;

    public BlockColorsFeature(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerPalettes = new HashMap<>();
        this.pendingTermsAcceptance = new HashMap<>();
        this.paletteDataManager = new dev.twme.ombre.blockcolors.data.PaletteDataManager(plugin);
    }

    /**
     * 初始化 BlockColors 功能
     */
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Initializing BlockColors feature...");
                
                // 1. 初始化快取系統
                cache = new BlockColorCache(plugin);
                boolean cacheInitialized = cache.initialize().join();
                
                if (!cacheInitialized) {
                    plugin.getLogger().severe("Cache initialization failed!");
                    return false;
                }
                
                // 2. 初始化顏色匹配器
                ColorMatcher.initialize(cache);
                
                // 3. 初始化條款追蹤器
                termsTracker = new TermsTracker(plugin);
                
                // 4. 初始化指令處理器
                commandHandler = new BlockColorsCommand((Ombre) plugin, this);
                
                // 5. 註冊事件監聽器
                plugin.getServer().getPluginManager().registerEvents(
                    new TermsAcceptanceListener(this), 
                    plugin
                );
                plugin.getServer().getPluginManager().registerEvents(
                    new BlockColorsGUIListener(), 
                    plugin
                );
                plugin.getServer().getPluginManager().registerEvents(
                    new PaletteListener(this), 
                    plugin
                );
                
                initialized = true;
                plugin.getLogger().info("BlockColors feature initialization complete!");
                
                return true;
                
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, 
                    "Error occurred while initializing BlockColors feature", e);
                return false;
            }
        });
    }

    /**
     * 關閉 BlockColors 功能
     */
    public void shutdown() {
        // 儲存所有玩家的調色盤
        for (Map.Entry<UUID, dev.twme.ombre.blockcolors.data.PlayerPalette> entry : playerPalettes.entrySet()) {
            paletteDataManager.savePalette(entry.getKey(), entry.getValue());
        }
        
        if (cache != null) {
            cache.saveCacheToFile();
        }
        if (termsTracker != null) {
            termsTracker.saveTermsData();
        }
        
        // 清除快取
        ColorMatcher.clearCache();
        playerPalettes.clear();
        pendingTermsAcceptance.clear();
        
        initialized = false;
        plugin.getLogger().info("BlockColors feature disabled");
    }

    /**
     * 重新載入功能
     */
    public CompletableFuture<Boolean> reload() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Reloading BlockColors...");
                
                // 重新載入快取
                boolean success = cache.reload().join();
                
                if (success) {
                    // 重新初始化顏色匹配器
                    ColorMatcher.clearCache();
                    ColorMatcher.initialize(cache);
                    
                    plugin.getLogger().info("BlockColors reload complete");
                    return true;
                } else {
                    plugin.getLogger().warning("BlockColors reload failed");
                    return false;
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, 
                    "Error occurred during reload", e);
                return false;
            }
        });
    }

    /**
     * 取得玩家的調色盤
     */
    public dev.twme.ombre.blockcolors.data.PlayerPalette getPlayerPalette(UUID playerId) {
        return playerPalettes.computeIfAbsent(playerId, 
            k -> paletteDataManager.loadPalette(k, 18)
        );
    }

    /**
     * 取得玩家的調色盤（如果存在於記憶體中）
     */
    public dev.twme.ombre.blockcolors.data.PlayerPalette getPlayerPaletteIfExists(UUID playerId) {
        return playerPalettes.get(playerId);
    }

    /**
     * 載入玩家的調色盤資料
     */
    public void loadPlayerPalette(UUID playerId) {
        if (!playerPalettes.containsKey(playerId)) {
            dev.twme.ombre.blockcolors.data.PlayerPalette palette = paletteDataManager.loadPalette(playerId, 18);
            playerPalettes.put(playerId, palette);
        }
    }

    /**
     * 儲存玩家的調色盤資料
     */
    public void savePlayerPalette(UUID playerId) {
        dev.twme.ombre.blockcolors.data.PlayerPalette palette = playerPalettes.get(playerId);
        if (palette != null) {
            paletteDataManager.savePalette(playerId, palette);
        }
    }

    /**
     * 卸載玩家的調色盤資料（從記憶體移除）
     */
    public void unloadPlayerPalette(UUID playerId) {
        playerPalettes.remove(playerId);
    }

    /**
     * 標記玩家正在等待條款回應
     */
    public void markPendingTermsAcceptance(UUID playerId) {
        pendingTermsAcceptance.put(playerId, System.currentTimeMillis());
    }

    /**
     * 檢查玩家是否正在等待條款回應
     */
    public boolean isPendingTermsAcceptance(UUID playerId) {
        return pendingTermsAcceptance.containsKey(playerId);
    }

    /**
     * 移除等待條款回應的標記
     */
    public void removePendingTermsAcceptance(UUID playerId) {
        pendingTermsAcceptance.remove(playerId);
    }

    /**
     * 清理過期的條款等待（超過60秒）
     */
    public void cleanupExpiredTermsWaiting() {
        long now = System.currentTimeMillis();
        pendingTermsAcceptance.entrySet().removeIf(entry -> 
            (now - entry.getValue()) > 60000  // 60秒超時
        );
    }

    // Getters
    public BlockColorCache getCache() {
        return cache;
    }

    public TermsTracker getTermsTracker() {
        return termsTracker;
    }

    public BlockColorsCommand getCommandHandler() {
        return commandHandler;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
