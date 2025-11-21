package dev.twme.ombre.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 漸層配置資料類別
 * 儲存玩家創建的漸層配置資訊
 */
public class GradientConfig {
    
    private final UUID id;
    private String name; // 可為 null
    private final UUID creatorUuid;
    private String creatorName;
    private final int configNumber; // 玩家的第 N 個漸層
    private final long timestamp;
    private final Map<Position, String> blockConfiguration; // 位置到方塊數據字串的映射
    private boolean published; // 是否公開
    private int favoriteCount; // 收藏數
    private int loadCount; // 載入次數
    
    /**
     * 建立新的漸層配置
     */
    public GradientConfig(UUID creatorUuid, String creatorName, int configNumber) {
        this.id = UUID.randomUUID();
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.configNumber = configNumber;
        this.timestamp = System.currentTimeMillis();
        this.blockConfiguration = new HashMap<>();
        this.published = false;
        this.favoriteCount = 0;
        this.loadCount = 0;
    }
    
    /**
     * 從已存在的資料建立漸層配置
     */
    public GradientConfig(UUID id, String name, UUID creatorUuid, String creatorName, 
                          int configNumber, long timestamp, Map<Position, String> blockConfiguration,
                          boolean published, int favoriteCount, int loadCount) {
        this.id = id;
        this.name = name;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.configNumber = configNumber;
        this.timestamp = timestamp;
        this.blockConfiguration = new HashMap<>(blockConfiguration);
        this.published = published;
        this.favoriteCount = favoriteCount;
        this.loadCount = loadCount;
    }
    
    // Getters
    public UUID getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public UUID getCreatorUuid() {
        return creatorUuid;
    }
    
    public String getCreatorName() {
        return creatorName;
    }
    
    public int getConfigNumber() {
        return configNumber;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Map<Position, String> getBlockConfiguration() {
        return new HashMap<>(blockConfiguration);
    }
    
    public boolean isPublished() {
        return published;
    }
    
    public int getFavoriteCount() {
        return favoriteCount;
    }
    
    public int getLoadCount() {
        return loadCount;
    }
    
    /**
     * 獲取顯示名稱
     * 如果有自訂名稱則使用自訂名稱，否則使用 "創建者名稱#配置編號"
     */
    public String getDisplayName() {
        return (name != null && !name.isEmpty()) ? name : (creatorName + "#" + configNumber);
    }
    
    // Setters
    public void setName(String name) {
        this.name = name;
    }
    
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
    
    public void setPublished(boolean published) {
        this.published = published;
    }
    
    public void incrementFavoriteCount() {
        this.favoriteCount++;
    }
    
    public void decrementFavoriteCount() {
        if (this.favoriteCount > 0) {
            this.favoriteCount--;
        }
    }
    
    public void incrementLoadCount() {
        this.loadCount++;
    }
    
    /**
     * 設定方塊配置
     */
    public void setBlock(int row, int col, String blockDataString) {
        blockConfiguration.put(new Position(row, col), blockDataString);
    }
    
    /**
     * 獲取特定位置的方塊
     */
    public String getBlock(int row, int col) {
        return blockConfiguration.get(new Position(row, col));
    }
    
    /**
     * 移除特定位置的方塊
     */
    public void removeBlock(int row, int col) {
        blockConfiguration.remove(new Position(row, col));
    }
    
    /**
     * 清空所有方塊配置
     */
    public void clearBlocks() {
        blockConfiguration.clear();
    }
    
    /**
     * 獲取方塊數量
     */
    public int getBlockCount() {
        return blockConfiguration.size();
    }
    
    /**
     * 位置類別（內部使用）
     */
    public static class Position {
        private final int row;
        private final int col;
        
        public Position(int row, int col) {
            this.row = row;
            this.col = col;
        }
        
        public int getRow() {
            return row;
        }
        
        public int getCol() {
            return col;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            Position position = (Position) obj;
            return row == position.row && col == position.col;
        }
        
        @Override
        public int hashCode() {
            return 31 * row + col;
        }
        
        @Override
        public String toString() {
            return "(" + row + "," + col + ")";
        }
    }
}
