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
import dev.twme.ombre.blockpalettes.util.MaterialValidator;

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
        // 驗證調色板是否包含有效的物品（靜默失敗，不記錄 log）
        if (!MaterialValidator.isValidPalette(paletteData)) {
            return false; // 調色板包含無效物品，拒絕加入收藏
        }
        
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
        // 過濾掉包含無效物品的調色板（靜默失敗，不記錄 log）
        return MaterialValidator.filterValidPalettes(new ArrayList<>(favorites.values()));
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
            plugin.getLogger().fine("Favorites data saved");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save favorites data: " + e.getMessage());
        }
    }
    
    /**
     * 從檔案載入
     */
    public void loadFromFile() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("Favorites data file does not exist, will create new one");
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
                            plugin.getLogger().warning("Invalid UUID: " + entry.getKey());
                        }
                    }
                    plugin.getLogger().info("Loaded favorites data for " + playerFavorites.size() + " players");
                }
                return;
            } catch (Exception e) {
                // 新格式載入失敗，嘗試舊格式
                plugin.getLogger().info("Detected old format favorites data, migrating...");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load favorites data: " + e.getMessage());
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
                        
                        plugin.getLogger().info("Migrated " + entry.getValue().size() + " favorite IDs for player " + uuid);
                        plugin.getLogger().warning("Note: Old favorites data cleared, please re-favorite palettes to cache full data");
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID: " + entry.getKey());
                    }
                }
                
                plugin.getLogger().info("Migrated favorites data for " + migratedCount + " players");
                
                // 儲存為新格式
                saveToFile();
                plugin.getLogger().info("Converted favorites data to new format");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load old format favorites data: " + e.getMessage());
            plugin.getLogger().warning("Recommend deleting favorites.json and re-favoriting palettes");
        }
    }
}
