package dev.twme.ombre.blockpalettes.api;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;

import com.google.gson.JsonObject;

import dev.twme.ombre.i18n.MessageManager;

/**
 * 調色板資料模型
 */
public class PaletteData {
    
    private int id;
    private String author;
    private String uploadTime;      // 相對時間 ("9 minutes ago")
    private String timestamp;       // 絕對時間 ("2024-01-15 10:30:00")
    private int likes;
    private int views;              // 僅詳細頁面有
    private List<String> blocks;    // 6個方塊 (內部 ID: snake_case)
    private List<PaletteData> similarPalettes;  // 僅詳細頁面有
    
    private static MessageManager messageManager; // For i18n support
    
    public PaletteData() {
        this.blocks = new ArrayList<>();
        this.similarPalettes = new ArrayList<>();
    }
    
    /**
     * 設定 MessageManager 用於國際化
     */
    public static void setMessageManager(MessageManager msg) {
        messageManager = msg;
    }
    
    /**
     * 從 API 回應轉換為 PaletteData
     */
    public static PaletteData fromApiResponse(JsonObject json) {
        PaletteData data = new PaletteData();
        
        // 安全地取得基本欄位
        data.id = json.has("id") && json.get("id") != null ? json.get("id").getAsInt() : 0;
        
        // API 使用 user_id 而不是 author
        if (json.has("user_id") && json.get("user_id") != null) {
            data.author = "User #" + json.get("user_id").getAsInt();
        } else if (json.has("author") && json.get("author") != null) {
            data.author = json.get("author").getAsString();
        } else {
            data.author = messageManager != null ? messageManager.getMessage("blockpalettes.palette.unknown-author") : "Unknown Author";
        }
        
        // API 使用 date 欄位作為時間戳，time_ago 作為相對時間
        data.timestamp = json.has("date") && json.get("date") != null ? json.get("date").getAsString() : "";
        
        // 直接使用 API 提供的 time_ago
        if (json.has("time_ago") && json.get("time_ago") != null) {
            data.uploadTime = json.get("time_ago").getAsString();
        } else {
            data.uploadTime = calculateRelativeTime(data.timestamp);
        }
        
        data.likes = json.has("likes") && json.get("likes") != null ? json.get("likes").getAsInt() : 0;
        data.views = json.has("views") && json.get("views") != null ? json.get("views").getAsInt() : 0;
        
        // 轉換方塊資料 - 安全地取得每個方塊，如果缺少則使用預設值
        List<String> blockList = new ArrayList<>();
        String[] blockKeys = {"blockOne", "blockTwo", "blockThree", "blockFour", "blockFive", "blockSix"};
        
        for (String key : blockKeys) {
            if (json.has(key) && json.get(key) != null && !json.get(key).isJsonNull()) {
                blockList.add(json.get(key).getAsString());
            } else {
                blockList.add("stone"); // 預設方塊
            }
        }
        
        data.blocks = blockList;
        
        return data;
    }
    
    /**
     * 轉換為 Minecraft Material
     */
    public List<Material> getMaterials() {
        return blocks.stream()
            .map(block -> {
                try {
                    Material material = Material.valueOf(block.toUpperCase());
                    if (!material.isBlock()) {
                        return Material.STONE; // 降級處理
                    }
                    return material;
                } catch (IllegalArgumentException e) {
                    return Material.STONE; // 降級處理
                }
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 取得顯示名稱
     */
    public List<String> getDisplayNames() {
        return blocks.stream()
            .map(this::toDisplayName)
            .collect(Collectors.toList());
    }
    
    /**
     * 轉換方塊 ID 為顯示名稱
     */
    private String toDisplayName(String blockId) {
        // "red_wool" → "Red Wool"
        return Arrays.stream(blockId.split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(Collectors.joining(" "));
    }
    
    /**
     * 計算相對時間 (固定使用英文)
     */
    private static String calculateRelativeTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "Unknown time";
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
            Instant pastTime = Instant.from(formatter.parse(timestamp));
            Instant now = Instant.now();
            
            long minutes = ChronoUnit.MINUTES.between(pastTime, now);
            long hours = ChronoUnit.HOURS.between(pastTime, now);
            long days = ChronoUnit.DAYS.between(pastTime, now);
            
            // Always use English for data class
            if (minutes < 60) {
                return minutes + " minutes ago";
            } else if (hours < 24) {
                return hours + " hours ago";
            } else if (days < 30) {
                return days + " days ago";
            } else if (days < 365) {
                return (days / 30) + " months ago";
            } else {
                return (days / 365) + " years ago";
            }
        } catch (Exception e) {
            return "Unknown time";
        }
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getUploadTime() {
        return uploadTime;
    }
    
    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        this.uploadTime = calculateRelativeTime(timestamp);
    }
    
    public int getLikes() {
        return likes;
    }
    
    public void setLikes(int likes) {
        this.likes = likes;
    }
    
    public int getViews() {
        return views;
    }
    
    public void setViews(int views) {
        this.views = views;
    }
    
    public List<String> getBlocks() {
        return blocks;
    }
    
    public void setBlocks(List<String> blocks) {
        this.blocks = blocks;
    }
    
    public List<PaletteData> getSimilarPalettes() {
        return similarPalettes;
    }
    
    public void setSimilarPalettes(List<PaletteData> similarPalettes) {
        this.similarPalettes = similarPalettes;
    }
}
