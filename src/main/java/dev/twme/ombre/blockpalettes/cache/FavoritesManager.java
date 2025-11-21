package dev.twme.ombre.blockpalettes.cache;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.blockpalettes.api.PaletteData;

/**
 * 收藏管理器
 * 處理玩家的調色板收藏功能，並快取完整的調色板資料
 */
public class FavoritesManager {
    
    private final Ombre plugin;
    private final File dataFile;
    private final Gson gson;
    private final Map<UUID, Map<Integer, PaletteData>> playerFavorites;  // UUID -> (PaletteId -> PaletteData)
    private final int maxFavorites;
    
    public FavoritesManager(Ombre plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.playerFavorites = new HashMap<>();
        this.maxFavorites = plugin.getConfig().getInt("block-palettes.max-favorites", 100);
        
        // 確保資料目錄存在
        File dataFolder = new File(plugin.getDataFolder(), "blockpalettes");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        this.dataFile = new File(dataFolder, "favorites.json");
    }
    
    /**
     * 新增收藏（包含完整資料）
     */
    public boolean addFavorite(UUID player, PaletteData paletteData) {
        Map<Integer, PaletteData> favorites = playerFavorites.computeIfAbsent(player, k -> new HashMap<>());
        
        if (favorites.size() >= maxFavorites) {
            return false; // 達到上限
        }
        
        favorites.put(paletteData.getId(), paletteData);
        saveToFile();
        return true;
    }
    
    /**
     * 移除收藏
     */
    public boolean removeFavorite(UUID player, int paletteId) {
        Map<Integer, PaletteData> favorites = playerFavorites.get(player);
        if (favorites == null) {
            return false;
        }
        
        boolean removed = favorites.remove(paletteId) != null;
        if (removed) {
            saveToFile();
        }
        return removed;
    }
    
    /**
     * 取得玩家的所有收藏（完整資料）
     */
    public List<PaletteData> getFavorites(UUID player) {
        Map<Integer, PaletteData> favorites = playerFavorites.get(player);
        if (favorites == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(favorites.values());
    }
    
    /**
     * 取得玩家的所有收藏 ID
     */
    public Set<Integer> getFavoriteIds(UUID player) {
        Map<Integer, PaletteData> favorites = playerFavorites.get(player);
        if (favorites == null) {
            return new HashSet<>();
        }
        return new HashSet<>(favorites.keySet());
    }
    
    /**
     * 檢查是否已收藏
     */
    public boolean isFavorite(UUID player, int paletteId) {
        Map<Integer, PaletteData> favorites = playerFavorites.get(player);
        return favorites != null && favorites.containsKey(paletteId);
    }
    
    /**
     * 清空玩家的所有收藏
     */
    public void clearFavorites(UUID player) {
        playerFavorites.remove(player);
        saveToFile();
    }
    
    /**
     * 取得收藏數量
     */
    public int getFavoriteCount(UUID player) {
        Map<Integer, PaletteData> favorites = playerFavorites.get(player);
        return favorites != null ? favorites.size() : 0;
    }
    
    /**
     * 儲存到檔案
     */
    public void saveToFile() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            // 轉換 UUID 為字串以便序列化
            Map<String, Map<Integer, PaletteData>> saveData = new HashMap<>();
            for (Map.Entry<UUID, Map<Integer, PaletteData>> entry : playerFavorites.entrySet()) {
                saveData.put(entry.getKey().toString(), entry.getValue());
            }
            
            gson.toJson(saveData, writer);
            plugin.getLogger().fine("收藏資料已儲存");
        } catch (IOException e) {
            plugin.getLogger().warning("儲存收藏資料失敗: " + e.getMessage());
        }
    }
    
    /**
     * 從檔案載入
     */
    public void loadFromFile() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("收藏資料檔案不存在，將建立新的");
            return;
        }
        
        try (FileReader reader = new FileReader(dataFile)) {
            // 先嘗試載入新格式 (Map<String, Map<Integer, PaletteData>>)
            try {
                Map<String, Map<Integer, PaletteData>> loadData = gson.fromJson(
                    reader, 
                    new TypeToken<Map<String, Map<Integer, PaletteData>>>(){}.getType()
                );
                
                if (loadData != null) {
                    playerFavorites.clear();
                    for (Map.Entry<String, Map<Integer, PaletteData>> entry : loadData.entrySet()) {
                        try {
                            UUID uuid = UUID.fromString(entry.getKey());
                            playerFavorites.put(uuid, entry.getValue());
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("無效的 UUID: " + entry.getKey());
                        }
                    }
                    plugin.getLogger().info("已載入 " + playerFavorites.size() + " 位玩家的收藏資料");
                }
                return;
            } catch (Exception e) {
                // 新格式載入失敗，嘗試舊格式
                plugin.getLogger().info("偵測到舊版收藏資料格式，正在進行遷移...");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("載入收藏資料失敗: " + e.getMessage());
            return;
        }
        
        // 嘗試載入舊格式 (Map<String, Set<Integer>>)
        try (FileReader reader = new FileReader(dataFile)) {
            Map<String, Set<Integer>> oldFormatData = gson.fromJson(
                reader, 
                new TypeToken<Map<String, Set<Integer>>>(){}.getType()
            );
            
            if (oldFormatData != null) {
                playerFavorites.clear();
                int migratedCount = 0;
                
                for (Map.Entry<String, Set<Integer>> entry : oldFormatData.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        // 舊格式只有 ID，建立空的 Map，之後第一次開啟時會從 API 重新載入完整資料
                        playerFavorites.put(uuid, new HashMap<>());
                        migratedCount++;
                        
                        plugin.getLogger().info("已為玩家 " + uuid + " 遷移 " + entry.getValue().size() + " 個收藏 ID");
                        plugin.getLogger().warning("注意: 舊收藏資料已清除，請重新收藏調色板以快取完整資料");
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("無效的 UUID: " + entry.getKey());
                    }
                }
                
                plugin.getLogger().info("已遷移 " + migratedCount + " 位玩家的收藏資料");
                
                // 儲存為新格式
                saveToFile();
                plugin.getLogger().info("已將收藏資料轉換為新格式");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("載入舊格式收藏資料失敗: " + e.getMessage());
            plugin.getLogger().warning("建議刪除 favorites.json 並重新收藏調色板");
        }
    }
}
