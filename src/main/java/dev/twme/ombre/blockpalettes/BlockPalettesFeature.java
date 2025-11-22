package dev.twme.ombre.blockpalettes;

import java.util.concurrent.CompletableFuture;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.blockpalettes.api.BlockPalettesAPI;
import dev.twme.ombre.blockpalettes.api.PaletteData;
import dev.twme.ombre.blockpalettes.cache.FavoritesManager;
import dev.twme.ombre.blockpalettes.cache.PaletteCache;
import dev.twme.ombre.blockpalettes.cache.TermsTracker;
import dev.twme.ombre.blockpalettes.command.BlockPalettesCommand;

/**
 * Block Palettes 功能主類
 * 管理與 blockpalettes.com 的整合功能
 */
public class BlockPalettesFeature {
    
    private final Ombre plugin;
    private BlockPalettesAPI api;
    private PaletteCache cache;
    private FavoritesManager favoritesManager;
    private TermsTracker termsTracker;
    private BlockPalettesCommand commandHandler;
    private boolean enabled;
    
    public BlockPalettesFeature(Ombre plugin) {
        this.plugin = plugin;
        this.enabled = false;
    }
    
    /**
     * 初始化 Block Palettes 功能
     */
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 檢查是否啟用
                if (!plugin.getConfig().getBoolean("block-palettes.enabled", true)) {
                    plugin.getLogger().info("Block Palettes feature is disabled");
                    return false;
                }
                
                // 設定 PaletteData 的 MessageManager 用於國際化
                PaletteData.setMessageManager(plugin.getMessageManager());
                
                // 初始化條款追蹤器
                termsTracker = new TermsTracker(plugin);
                plugin.getLogger().info("Terms tracker initialized");
                
                // 初始化 API 客戶端
                api = new BlockPalettesAPI(plugin);
                plugin.getLogger().info("API client initialized");
                
                // 初始化快取
                cache = new PaletteCache(plugin, api);
                plugin.getLogger().info("Cache system initialized");
                
                // 初始化收藏管理器
                favoritesManager = new FavoritesManager(plugin);
                favoritesManager.loadFromFile();
                plugin.getLogger().info("Favorites manager initialized");
                
                // 初始化指令處理器
                commandHandler = new BlockPalettesCommand(plugin, this);
                plugin.getLogger().info("Command handler initialized");
                
                // 註冊搜尋輸入聊天監聽器
                org.bukkit.Bukkit.getPluginManager().registerEvents(
                    new dev.twme.ombre.blockpalettes.gui.SearchInputGUI.ChatListener(), 
                    plugin
                );
                plugin.getLogger().info("Search input listener registered");
                
                enabled = true;
                plugin.getLogger().info("Block Palettes feature initialization complete");
                return true;
                
            } catch (Exception e) {
                plugin.getLogger().severe("Block Palettes feature initialization failed: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 關閉功能
     */
    public void shutdown() {
        if (!enabled) return;
        
        try {
            // 儲存收藏資料
            if (favoritesManager != null) {
                favoritesManager.saveToFile();
            }
            
            // 清除快取
            if (cache != null) {
                cache.cleanup();
            }
            
            plugin.getLogger().info("Block Palettes feature shutdown");
        } catch (Exception e) {
            plugin.getLogger().severe("Error during Block Palettes feature shutdown: " + e.getMessage());
        }
    }
    
    // Getters
    public BlockPalettesAPI getApi() {
        return api;
    }
    
    public PaletteCache getCache() {
        return cache;
    }
    
    public FavoritesManager getFavoritesManager() {
        return favoritesManager;
    }
    
    public TermsTracker getTermsTracker() {
        return termsTracker;
    }
    
    public BlockPalettesCommand getCommandHandler() {
        return commandHandler;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
