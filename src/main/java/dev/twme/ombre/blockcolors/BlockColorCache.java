package dev.twme.ombre.blockcolors;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import dev.twme.ombre.blockcolors.data.BlockCategory;
import dev.twme.ombre.blockcolors.data.BlockColorData;
import dev.twme.ombre.blockcolors.util.MaterialMapper;

/**
 * BlockColor 快取管理系統
 * 負責從 API 下載、快取和管理方塊顏色資料
 */
public class BlockColorCache {
    private final JavaPlugin plugin;
    private static final String API_URL = "https://blockcolors.app/assets/color_data.json";
    private static final String CACHE_FILE = "blockcolors_cache.yml";
    private static final long CACHE_EXPIRY = 7 * 24 * 60 * 60 * 1000; // 7天

    // 快取資料
    private Map<String, BlockColorData> blockColorMap;  // Key: API ID
    private Map<Material, BlockColorData> materialToBlockMap;  // Key: Minecraft Material
    private List<BlockColorData> buildingBlocks;
    private List<BlockColorData> decorationBlocks;

    // 快取元資訊
    private long lastUpdateTime;
    private String cacheVersion = "1.0.0";
    private int totalBlocks;
    private String apiSourceUrl = API_URL;

    public BlockColorCache(JavaPlugin plugin) {
        this.plugin = plugin;
        this.blockColorMap = new HashMap<>();
        this.materialToBlockMap = new HashMap<>();
        this.buildingBlocks = new ArrayList<>();
        this.decorationBlocks = new ArrayList<>();
        
        MaterialMapper.setLogger(plugin.getLogger());
    }

    /**
     * 初始化快取
     * 非同步執行，避免阻塞伺服器啟動
     */
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("正在初始化 BlockColors 快取...");
                
                // 檢查是否需要更新
                if (needsUpdate()) {
                    plugin.getLogger().info("快取不存在或已過期，正在從 API 下載資料...");
                    boolean downloaded = downloadFromAPI();
                    
                    if (downloaded) {
                        plugin.getLogger().info("API 資料下載成功");
                        saveCacheToFile();
                    } else {
                        plugin.getLogger().warning("API 下載失敗，嘗試從快取檔案載入...");
                        if (!loadCacheFromFile()) {
                            plugin.getLogger().severe("快取載入失敗！BlockColors 功能可能無法正常運作");
                            return false;
                        }
                    }
                } else {
                    plugin.getLogger().info("從快取檔案載入資料...");
                    if (!loadCacheFromFile()) {
                        plugin.getLogger().warning("快取檔案載入失敗，嘗試從 API 下載...");
                        if (!downloadFromAPI()) {
                            plugin.getLogger().severe("無法載入方塊顏色資料！");
                            return false;
                        }
                        saveCacheToFile();
                    }
                }
                
                // 建立索引
                buildIndices();
                
                plugin.getLogger().info("BlockColors 快取初始化完成");
                plugin.getLogger().info(MaterialMapper.getMappingStats(blockColorMap.values()));
                
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "初始化 BlockColors 快取時發生錯誤", e);
                return false;
            }
        });
    }

    /**
     * 從 API 下載方塊顏色資料
     */
    public boolean downloadFromAPI() {
        int maxRetries = 3;
        int retryDelay = 2000; // 2 秒
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                plugin.getLogger().info("嘗試從 API 下載 (第 " + attempt + "/" + maxRetries + " 次)...");
                
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("User-Agent", "Ombre-Minecraft-Plugin");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // 讀取回應
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // 解析 JSON
                    parseApiResponse(response.toString());
                    
                    lastUpdateTime = System.currentTimeMillis();
                    totalBlocks = blockColorMap.size();
                    
                    return true;
                } else {
                    plugin.getLogger().warning("API 回應代碼: " + responseCode);
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("API 下載失敗 (第 " + attempt + " 次): " + e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * 解析 API 回應的 JSON 資料
     */
    private void parseApiResponse(String jsonString) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
        
        blockColorMap.clear();
        
        for (String id : jsonObject.keySet()) {
            JsonObject blockJson = jsonObject.getAsJsonObject(id);
            
            try {
                String displayName = blockJson.get("display_name").getAsString();
                String hex = blockJson.get("hex").getAsString();
                String textureName = blockJson.get("texture_name").getAsString();
                boolean isDecoration = blockJson.get("is_decoration").getAsBoolean();
                boolean show3d = blockJson.get("show_3d").getAsBoolean();
                
                // 解析 Lab 陣列
                double[] lab = gson.fromJson(
                    blockJson.get("lab"), 
                    new TypeToken<double[]>(){}.getType()
                );
                
                // 建立 BlockColorData
                BlockColorData data = new BlockColorData(
                    id, displayName, hex, lab, textureName, isDecoration, show3d
                );
                
                // 映射 Material
                Material material = BlockColorData.parseMaterialFromTextureName(textureName);
                
                // 只保留可以作為物品的方塊
                if (material != null && material.isItem()) {
                    data.setMaterial(material);
                    blockColorMap.put(id, data);
                } else if (material != null) {
                    plugin.getLogger().fine("跳過非物品方塊: " + textureName + " (" + material.name() + ")");
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("解析方塊資料失敗 (ID: " + id + "): " + e.getMessage());
            }
        }
    }

    /**
     * 建立索引以加速查詢
     */
    private void buildIndices() {
        materialToBlockMap.clear();
        buildingBlocks.clear();
        decorationBlocks.clear();
        
        for (BlockColorData data : blockColorMap.values()) {
            // 只索引成功映射的方塊
            if (data.getMaterial() != null) {
                materialToBlockMap.put(data.getMaterial(), data);
                
                if (data.getCategory() == BlockCategory.BUILDING) {
                    buildingBlocks.add(data);
                } else if (data.getCategory() == BlockCategory.DECORATION) {
                    decorationBlocks.add(data);
                }
            }
        }
        
        plugin.getLogger().info("已建立索引: " + materialToBlockMap.size() + " 個可用方塊");
    }

    /**
     * 將快取儲存到檔案
     */
    public void saveCacheToFile() {
        try {
            File cacheFile = new File(plugin.getDataFolder(), CACHE_FILE);
            YamlConfiguration config = new YamlConfiguration();
            
            // 儲存元資訊
            config.set("version", cacheVersion);
            config.set("last_update", lastUpdateTime);
            config.set("api_source", apiSourceUrl);
            config.set("total_blocks", totalBlocks);
            
            // 儲存方塊資料
            for (Map.Entry<String, BlockColorData> entry : blockColorMap.entrySet()) {
                String id = entry.getKey();
                BlockColorData data = entry.getValue();
                String path = "blocks." + id;
                
                config.set(path + ".display_name", data.getDisplayName());
                config.set(path + ".hex", data.getHexColor());
                config.set(path + ".rgb", data.getRgb());
                config.set(path + ".red", data.getRed());
                config.set(path + ".green", data.getGreen());
                config.set(path + ".blue", data.getBlue());
                config.set(path + ".lab", Arrays.asList(data.getLab()[0], data.getLab()[1], data.getLab()[2]));
                config.set(path + ".texture_name", data.getTextureName());
                config.set(path + ".material", data.getMaterial() != null ? data.getMaterial().name() : null);
                config.set(path + ".is_decoration", data.isDecoration());
                config.set(path + ".show_3d", data.isShow3d());
                config.set(path + ".category", data.getCategory().name());
            }
            
            config.save(cacheFile);
            plugin.getLogger().info("快取已儲存到: " + cacheFile.getAbsolutePath());
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "儲存快取檔案失敗", e);
        }
    }

    /**
     * 從檔案載入快取
     */
    public boolean loadCacheFromFile() {
        try {
            File cacheFile = new File(plugin.getDataFolder(), CACHE_FILE);
            if (!cacheFile.exists()) {
                return false;
            }
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(cacheFile);
            
            // 載入元資訊
            cacheVersion = config.getString("version", "1.0.0");
            lastUpdateTime = config.getLong("last_update", 0);
            apiSourceUrl = config.getString("api_source", API_URL);
            totalBlocks = config.getInt("total_blocks", 0);
            
            // 載入方塊資料
            blockColorMap.clear();
            if (config.contains("blocks")) {
                for (String id : config.getConfigurationSection("blocks").getKeys(false)) {
                    String path = "blocks." + id;
                    
                    try {
                        String displayName = config.getString(path + ".display_name");
                        String hex = config.getString(path + ".hex");
                        int rgb = config.getInt(path + ".rgb");
                        int red = config.getInt(path + ".red");
                        int green = config.getInt(path + ".green");
                        int blue = config.getInt(path + ".blue");
                        List<Double> labList = (List<Double>) config.getList(path + ".lab");
                        double[] lab = new double[]{labList.get(0), labList.get(1), labList.get(2)};
                        String textureName = config.getString(path + ".texture_name");
                        String materialName = config.getString(path + ".material");
                        boolean isDecoration = config.getBoolean(path + ".is_decoration");
                        boolean show3d = config.getBoolean(path + ".show_3d");
                        
                        BlockColorData data = new BlockColorData(
                            id, displayName, hex, lab, textureName, isDecoration, show3d
                        );
                        
                        if (materialName != null) {
                            try {
                                data.setMaterial(Material.valueOf(materialName));
                            } catch (IllegalArgumentException e) {
                                // Material 不存在，設為 null
                            }
                        }
                        
                        blockColorMap.put(id, data);
                        
                    } catch (Exception e) {
                        plugin.getLogger().warning("載入方塊資料失敗 (ID: " + id + "): " + e.getMessage());
                    }
                }
            }
            
            plugin.getLogger().info("從快取檔案載入了 " + blockColorMap.size() + " 個方塊");
            return !blockColorMap.isEmpty();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "載入快取檔案失敗", e);
            return false;
        }
    }

    /**
     * 檢查是否需要更新快取
     */
    public boolean needsUpdate() {
        File cacheFile = new File(plugin.getDataFolder(), CACHE_FILE);
        if (!cacheFile.exists()) {
            return true;
        }
        
        // 檢查是否過期
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastUpdateTime) > CACHE_EXPIRY;
    }

    /**
     * 手動重新載入快取
     */
    public CompletableFuture<Boolean> reload() {
        return CompletableFuture.supplyAsync(() -> {
            plugin.getLogger().info("重新載入 BlockColors 快取...");
            boolean success = downloadFromAPI();
            if (success) {
                saveCacheToFile();
                buildIndices();
                plugin.getLogger().info("快取重新載入完成");
            } else {
                plugin.getLogger().warning("快取重新載入失敗");
            }
            return success;
        });
    }

    /**
     * 清除快取
     */
    public void clearCache() {
        blockColorMap.clear();
        materialToBlockMap.clear();
        buildingBlocks.clear();
        decorationBlocks.clear();
        
        File cacheFile = new File(plugin.getDataFolder(), CACHE_FILE);
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
        
        plugin.getLogger().info("快取已清除");
    }

    // Getters
    public List<BlockColorData> getAllBlocks() {
        return new ArrayList<>(blockColorMap.values());
    }

    public List<BlockColorData> getBlocksByCategory(BlockCategory category) {
        if (category == BlockCategory.BUILDING) {
            return new ArrayList<>(buildingBlocks);
        } else if (category == BlockCategory.DECORATION) {
            return new ArrayList<>(decorationBlocks);
        } else {
            return getAllBlocks();
        }
    }

    public BlockColorData getBlockByMaterial(Material material) {
        return materialToBlockMap.get(material);
    }

    public BlockColorData getBlockById(String id) {
        return blockColorMap.get(id);
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public String getCacheVersion() {
        return cacheVersion;
    }
}
