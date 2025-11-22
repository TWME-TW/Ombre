package dev.twme.ombre.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import dev.twme.ombre.data.GradientConfig;

/**
 * 配置管理器
 * 負責儲存、載入和管理漸層配置
 */
public class ConfigManager {
    
    private final Plugin plugin;
    private final File playersFolder;
    private final File sharedFolder;
    private final Map<UUID, Set<UUID>> playerFavorites; // 玩家UUID -> 收藏的配置ID集合
    
    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.playersFolder = new File(plugin.getDataFolder(), "players");
        this.sharedFolder = new File(plugin.getDataFolder(), "shared");
        this.playerFavorites = new HashMap<>();
        
        // 確保資料夾存在
        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }
        if (!sharedFolder.exists()) {
            sharedFolder.mkdirs();
        }
    }
    
    /**
     * 儲存漸層配置
     */
    public boolean saveGradient(GradientConfig config) {
        File playerFolder = new File(playersFolder, config.getCreatorUuid().toString());
        if (!playerFolder.exists()) {
            playerFolder.mkdirs();
        }
        
        File configFile = new File(playerFolder, config.getId().toString() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        
        // 儲存基本資訊
        yaml.set("id", config.getId().toString());
        yaml.set("name", config.getName());
        yaml.set("creator-uuid", config.getCreatorUuid().toString());
        yaml.set("creator-name", config.getCreatorName());
        yaml.set("config-number", config.getConfigNumber());
        yaml.set("timestamp", config.getTimestamp());
        yaml.set("published", config.isPublished());
        yaml.set("favorite-count", config.getFavoriteCount());
        yaml.set("load-count", config.getLoadCount());
        
        // 儲存方塊配置
        ConfigurationSection blocksSection = yaml.createSection("blocks");
        for (Map.Entry<GradientConfig.Position, String> entry : config.getBlockConfiguration().entrySet()) {
            GradientConfig.Position pos = entry.getKey();
            String key = pos.getRow() + "," + pos.getCol();
            blocksSection.set(key, entry.getValue());
        }
        
        try {
            yaml.save(configFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save configuration: " + config.getId(), e);
            return false;
        }
    }
    
    /**
     * 載入漸層配置
     */
    public GradientConfig loadGradient(UUID configId, UUID playerUuid) {
        File playerFolder = new File(playersFolder, playerUuid.toString());
        File configFile = new File(playerFolder, configId.toString() + ".yml");
        
        if (!configFile.exists()) {
            return null;
        }
        
        return loadGradientFromFile(configFile);
    }
    
    /**
     * 從檔案載入配置
     */
    private GradientConfig loadGradientFromFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        
        try {
            UUID id = UUID.fromString(yaml.getString("id"));
            String name = yaml.getString("name");
            UUID creatorUuid = UUID.fromString(yaml.getString("creator-uuid"));
            String creatorName = yaml.getString("creator-name");
            int configNumber = yaml.getInt("config-number");
            long timestamp = yaml.getLong("timestamp");
            boolean published = yaml.getBoolean("published", false);
            int favoriteCount = yaml.getInt("favorite-count", 0);
            int loadCount = yaml.getInt("load-count", 0);
            
            // 載入方塊配置
            Map<GradientConfig.Position, String> blocks = new HashMap<>();
            ConfigurationSection blocksSection = yaml.getConfigurationSection("blocks");
            if (blocksSection != null) {
                for (String key : blocksSection.getKeys(false)) {
                    String[] parts = key.split(",");
                    int row = Integer.parseInt(parts[0]);
                    int col = Integer.parseInt(parts[1]);
                    String blockData = blocksSection.getString(key);
                    blocks.put(new GradientConfig.Position(row, col), blockData);
                }
            }
            
            return new GradientConfig(id, name, creatorUuid, creatorName, configNumber, 
                                     timestamp, blocks, published, favoriteCount, loadCount);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration: " + file.getName(), e);
            return null;
        }
    }
    
    /**
     * 發布配置到共享庫
     */
    public boolean publishGradient(UUID configId, UUID playerUuid) {
        GradientConfig config = loadGradient(configId, playerUuid);
        if (config == null) {
            return false;
        }
        
        config.setPublished(true);
        
        // 儲存到共享資料夾
        File sharedFile = new File(sharedFolder, configId.toString() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        
        yaml.set("id", config.getId().toString());
        yaml.set("name", config.getName());
        yaml.set("creator-uuid", config.getCreatorUuid().toString());
        yaml.set("creator-name", config.getCreatorName());
        yaml.set("config-number", config.getConfigNumber());
        yaml.set("timestamp", config.getTimestamp());
        yaml.set("published", true);
        yaml.set("favorite-count", config.getFavoriteCount());
        yaml.set("load-count", config.getLoadCount());
        
        ConfigurationSection blocksSection = yaml.createSection("blocks");
        for (Map.Entry<GradientConfig.Position, String> entry : config.getBlockConfiguration().entrySet()) {
            GradientConfig.Position pos = entry.getKey();
            String key = pos.getRow() + "," + pos.getCol();
            blocksSection.set(key, entry.getValue());
        }
        
        try {
            yaml.save(sharedFile);
            // 同時更新玩家資料夾中的配置
            saveGradient(config);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to publish configuration: " + configId, e);
            return false;
        }
    }
    
    /**
     * 刪除配置
     */
    public boolean deleteGradient(UUID configId, Player player) {
        GradientConfig config = loadGradient(configId, player.getUniqueId());
        if (config == null) {
            return false;
        }
        
        // 檢查權限
        if (!config.getCreatorUuid().equals(player.getUniqueId()) && 
            !player.hasPermission("ombre.admin")) {
            return false;
        }
        
        // 刪除玩家資料夾中的檔案
        File playerFolder = new File(playersFolder, player.getUniqueId().toString());
        File configFile = new File(playerFolder, configId.toString() + ".yml");
        
        boolean deleted = configFile.delete();
        
        // 如果已發布，也刪除共享資料夾中的檔案
        if (config.isPublished()) {
            File sharedFile = new File(sharedFolder, configId.toString() + ".yml");
            sharedFile.delete();
        }
        
        return deleted;
    }
    
    /**
     * 通過配置 ID 刪除配置（管理員）
     */
    public boolean deleteGradientById(UUID configId) {
        boolean deleted = false;
        
        // 搜尋所有玩家資料夾
        File[] playerFolders = playersFolder.listFiles(File::isDirectory);
        if (playerFolders != null) {
            for (File playerFolder : playerFolders) {
                File configFile = new File(playerFolder, configId.toString() + ".yml");
                if (configFile.exists()) {
                    deleted = configFile.delete();
                    if (deleted) {
                        // 同時刪除共享庫中的對應檔案
                        File sharedFile = new File(sharedFolder, configId.toString() + ".yml");
                        if (sharedFile.exists()) {
                            sharedFile.delete();
                        }
                        break;
                    }
                }
            }
        }
        
        return deleted;
    }
    
    /**
     * 刪除玩家所有配置（管理員）
     */
    public int deletePlayerGradients(UUID playerUuid) {
        File playerFolder = new File(playersFolder, playerUuid.toString());
        if (!playerFolder.exists()) {
            return 0;
        }
        
        int deletedCount = 0;
        File[] files = playerFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files != null) {
            for (File file : files) {
                if (file.delete()) {
                    deletedCount++;
                    
                    // 同時刪除共享庫中的對應檔案
                    String fileName = file.getName();
                    File sharedFile = new File(sharedFolder, fileName);
                    if (sharedFile.exists()) {
                        sharedFile.delete();
                    }
                }
            }
        }
        
        return deletedCount;
    }
    
    /**
     * 獲取所有共享配置
     */
    public List<GradientConfig> getSharedConfigs() {
        List<GradientConfig> configs = new ArrayList<>();
        File[] files = sharedFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files != null) {
            for (File file : files) {
                GradientConfig config = loadGradientFromFile(file);
                if (config != null) {
                    configs.add(config);
                }
            }
        }
        
        return configs;
    }
    
    /**
     * 獲取玩家的收藏列表
     */
    public Set<UUID> getFavorites(UUID playerUuid) {
        Set<UUID> favorites = playerFavorites.get(playerUuid);
        if (favorites == null) {
            loadFavorites(playerUuid);
            favorites = playerFavorites.get(playerUuid);
        }
        return favorites != null ? new HashSet<>(favorites) : new HashSet<>();
    }
    
    /**
     * 獲取本週熱門配置
     */
    public List<GradientConfig> getWeeklyHotGradients() {
        long oneWeekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        
        return getSharedConfigs().stream()
            .filter(config -> config.getTimestamp() >= oneWeekAgo)
            .sorted((a, b) -> Integer.compare(b.getFavoriteCount(), a.getFavoriteCount()))
            .collect(Collectors.toList());
    }
    
    /**
     * 獲取玩家收藏的配置
     */
    public List<GradientConfig> getFavoriteGradients(Player player) {
        Set<UUID> favoriteIds = playerFavorites.get(player.getUniqueId());
        if (favoriteIds == null || favoriteIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<GradientConfig> configs = new ArrayList<>();
        for (UUID configId : favoriteIds) {
            // 先從共享庫找
            File sharedFile = new File(sharedFolder, configId.toString() + ".yml");
            if (sharedFile.exists()) {
                GradientConfig config = loadGradientFromFile(sharedFile);
                if (config != null) {
                    configs.add(config);
                }
            }
        }
        
        return configs;
    }
    
    /**
     * 獲取玩家創建的漸層總數
     */
    public int getPlayerGradientCount(UUID playerUuid) {
        File playerFolder = new File(playersFolder, playerUuid.toString());
        if (!playerFolder.exists()) {
            return 0;
        }
        
        File[] files = playerFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        return files != null ? files.length : 0;
    }
    
    /**
     * 增加載入次數
     */
    public void incrementLoadCount(UUID configId) {
        // 從共享庫載入
        File sharedFile = new File(sharedFolder, configId.toString() + ".yml");
        if (sharedFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(sharedFile);
            int loadCount = yaml.getInt("load-count", 0) + 1;
            yaml.set("load-count", loadCount);
            try {
                yaml.save(sharedFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to update load count", e);
            }
        }
    }
    
    /**
     * 增加收藏數
     */
    public void incrementFavoriteCount(UUID configId) {
        File sharedFile = new File(sharedFolder, configId.toString() + ".yml");
        if (sharedFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(sharedFile);
            int favoriteCount = yaml.getInt("favorite-count", 0) + 1;
            yaml.set("favorite-count", favoriteCount);
            try {
                yaml.save(sharedFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to update favorite count", e);
            }
        }
    }
    
    /**
     * 減少收藏數
     */
    public void decrementFavoriteCount(UUID configId) {
        File sharedFile = new File(sharedFolder, configId.toString() + ".yml");
        if (sharedFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(sharedFile);
            int favoriteCount = Math.max(0, yaml.getInt("favorite-count", 0) - 1);
            yaml.set("favorite-count", favoriteCount);
            try {
                yaml.save(sharedFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to update favorite count", e);
            }
        }
    }
    
    /**
     * 添加收藏
     */
    public void addFavorite(UUID playerUuid, UUID configId) {
        Set<UUID> favorites = playerFavorites.computeIfAbsent(playerUuid, k -> new HashSet<>());
        favorites.add(configId);
        saveFavorites(playerUuid);
    }
    
    /**
     * 移除收藏
     */
    public void removeFavorite(UUID playerUuid, UUID configId) {
        Set<UUID> favorites = playerFavorites.get(playerUuid);
        if (favorites != null) {
            favorites.remove(configId);
            saveFavorites(playerUuid);
        }
    }
    
    /**
     * 檢查是否已收藏
     */
    public boolean isFavorited(Player player, UUID configId) {
        Set<UUID> favorites = playerFavorites.get(player.getUniqueId());
        return favorites != null && favorites.contains(configId);
    }
    
    /**
     * 儲存收藏列表
     */
    private void saveFavorites(UUID playerUuid) {
        File playerFolder = new File(playersFolder, playerUuid.toString());
        if (!playerFolder.exists()) {
            playerFolder.mkdirs();
        }
        
        File favoritesFile = new File(playerFolder, "favorites.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        
        Set<UUID> favorites = playerFavorites.get(playerUuid);
        if (favorites != null) {
            List<String> favoriteStrings = favorites.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
            yaml.set("favorites", favoriteStrings);
        }
        
        try {
            yaml.save(favoritesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save favorites list", e);
        }
    }
    
    /**
     * 載入收藏列表
     */
    public void loadFavorites(UUID playerUuid) {
        File playerFolder = new File(playersFolder, playerUuid.toString());
        File favoritesFile = new File(playerFolder, "favorites.yml");
        
        if (!favoritesFile.exists()) {
            return;
        }
        
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(favoritesFile);
        List<String> favoriteStrings = yaml.getStringList("favorites");
        
        Set<UUID> favorites = favoriteStrings.stream()
            .map(UUID::fromString)
            .collect(Collectors.toSet());
        
        playerFavorites.put(playerUuid, favorites);
    }
    
    /**
     * 驗證名稱合法性
     */
    public boolean validateName(String name, Player player) {
        if (name == null || name.isEmpty()) {
            return true; // 允許空名稱（使用預設名稱）
        }
        
        if (name.length() > 32) {
            return false; // 超過長度限制
        }
        
        // 檢查是否重複
        File playerFolder = new File(playersFolder, player.getUniqueId().toString());
        if (!playerFolder.exists()) {
            return true;
        }
        
        File[] files = playerFolder.listFiles((dir, fileName) -> fileName.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                GradientConfig config = loadGradientFromFile(file);
                if (config != null && name.equals(config.getName())) {
                    return false; // 名稱重複
                }
            }
        }
        
        return true;
    }
    
    /**
     * 生成預設名稱
     */
    public String generateDefaultName(Player player) {
        int nextNumber = getPlayerGradientCount(player.getUniqueId()) + 1;
        return player.getName() + "#" + nextNumber;
    }
}
