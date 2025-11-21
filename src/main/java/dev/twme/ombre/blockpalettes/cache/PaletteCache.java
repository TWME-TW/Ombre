package dev.twme.ombre.blockpalettes.cache;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.blockpalettes.api.APIResponse;
import dev.twme.ombre.blockpalettes.api.BlockPalettesAPI;
import dev.twme.ombre.blockpalettes.api.PaletteData;
import dev.twme.ombre.blockpalettes.api.PaletteFilter;

/**
 * 調色板快取系統
 * 快取 API 請求結果以減少網路請求
 */
public class PaletteCache {
    
    private final Ombre plugin;
    private final BlockPalettesAPI api;
    
    // 列表快取 (過濾條件 -> 回應)
    private final Map<String, CachedResponse> listCache;
    
    // 詳細資料快取 (ID -> 調色板資料)
    private final Map<Integer, CachedPalette> detailCache;
    
    private final long listCacheDuration;  // 列表快取時間 (毫秒)
    private final long detailCacheDuration; // 詳細資料快取時間 (毫秒)
    
    public PaletteCache(Ombre plugin, BlockPalettesAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.listCache = new ConcurrentHashMap<>();
        this.detailCache = new ConcurrentHashMap<>();
        
        // 從設定讀取快取時間 (秒轉毫秒)
        int listSeconds = plugin.getConfig().getInt("block-palettes.cache-duration", 300);
        this.listCacheDuration = TimeUnit.SECONDS.toMillis(listSeconds);
        this.detailCacheDuration = TimeUnit.MINUTES.toMillis(30); // 詳細資料快取 30 分鐘
    }
    
    /**
     * 取得調色板列表 (使用快取)
     */
    public CompletableFuture<APIResponse> getPalettes(PaletteFilter filter, boolean forceRefresh) {
        String cacheKey = filter.toQueryString();
        
        if (!forceRefresh) {
            CachedResponse cached = listCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                plugin.getLogger().fine("使用快取的列表資料: " + cacheKey);
                return CompletableFuture.completedFuture(cached.response);
            }
        }
        
        // 快取過期或強制重新整理，從 API 取得
        return api.getPalettes(filter).thenApply(response -> {
            if (response.isSuccess()) {
                listCache.put(cacheKey, new CachedResponse(response, listCacheDuration));
                plugin.getLogger().fine("已快取列表資料: " + cacheKey);
            }
            return response;
        });
    }
    
    /**
     * 取得調色板詳細資訊 (使用快取)
     */
    public CompletableFuture<PaletteData> getPaletteDetails(int id, boolean forceRefresh) {
        if (!forceRefresh) {
            CachedPalette cached = detailCache.get(id);
            if (cached != null && !cached.isExpired()) {
                plugin.getLogger().fine("使用快取的詳細資料: " + id);
                return CompletableFuture.completedFuture(cached.data);
            }
        }
        
        // 快取過期或強制重新整理，從 API 取得
        return api.getPaletteDetails(id).thenApply(data -> {
            if (data != null) {
                detailCache.put(id, new CachedPalette(data, detailCacheDuration));
                plugin.getLogger().fine("已快取詳細資料: " + id);
            }
            return data;
        });
    }
    
    /**
     * 清除所有快取
     */
    public void clearAll() {
        listCache.clear();
        detailCache.clear();
        plugin.getLogger().info("已清除所有快取");
    }
    
    /**
     * 清除過期的快取項目
     */
    public void cleanupExpired() {
        listCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        detailCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        plugin.getLogger().fine("已清理過期快取");
    }
    
    /**
     * 取得快取統計資訊
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
            "list_cache_size", listCache.size(),
            "detail_cache_size", detailCache.size(),
            "list_cache_duration_ms", listCacheDuration,
            "detail_cache_duration_ms", detailCacheDuration
        );
    }
    
    /**
     * 清理資源
     */
    public void cleanup() {
        clearAll();
    }
    
    /**
     * 快取的 API 回應
     */
    private static class CachedResponse {
        final APIResponse response;
        final long expireTime;
        
        CachedResponse(APIResponse response, long duration) {
            this.response = response;
            this.expireTime = System.currentTimeMillis() + duration;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
    
    /**
     * 快取的調色板資料
     */
    private static class CachedPalette {
        final PaletteData data;
        final long expireTime;
        
        CachedPalette(PaletteData data, long duration) {
            this.data = data;
            this.expireTime = System.currentTimeMillis() + duration;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}
