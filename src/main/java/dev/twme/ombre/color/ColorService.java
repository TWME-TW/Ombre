package dev.twme.ombre.color;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Color;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * 顏色服務
 * 負責從 colors.yml 讀取方塊顏色並提供快取機制
 */
public class ColorService {
    
    private final Plugin plugin;
    private final File colorsFile;
    private final Map<String, BlockColor> colorCache;
    private YamlConfiguration colorsConfig;
    
    public ColorService(Plugin plugin) {
        this.plugin = plugin;
        this.colorsFile = new File(plugin.getDataFolder(), "colors.yml");
        this.colorCache = new HashMap<>();
    }
    
    /**
     * 載入顏色資料從檔案
     * @return 是否成功載入
     */
    public boolean loadColorsFromFile() {
        if (!colorsFile.exists()) {
            plugin.getLogger().warning("colors.yml does not exist, unable to load color data");
            return false;
        }
        
        try {
            colorsConfig = YamlConfiguration.loadConfiguration(colorsFile);
            plugin.getLogger().info("Successfully loaded colors.yml");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load colors.yml", e);
            return false;
        }
    }
    
    /**
     * 獲取方塊顏色
     * @param blockData 方塊數據
     * @return 方塊顏色，如果找不到則返回 null
     */
    public BlockColor getBlockColor(BlockData blockData) {
        String blockDataString = blockData.getAsString();
        
        // 檢查快取
        if (colorCache.containsKey(blockDataString)) {
            return colorCache.get(blockDataString);
        }
        
        // 從配置檔案讀取
        BlockColor color = loadColorFromConfig(blockDataString);
        
        // 如果配置檔案中找不到，動態獲取並快取
        if (color == null) {
            color = getColorFromMapColor(blockData);
            if (color != null) {
                cacheColor(blockDataString, color);
                // 改用 FINE 級別，減少日誌輸出
                plugin.getLogger().fine(String.format("Dynamically resolved color for block %s and cached it", blockDataString));
            }
        } else {
            // 快取到記憶體
            colorCache.put(blockDataString, color);
        }
        
        return color;
    }
    
    /**
     * 從配置檔案讀取顏色
     * @param blockDataString 方塊數據字串
     * @return 方塊顏色，如果找不到則返回 null
     */
    private BlockColor loadColorFromConfig(String blockDataString) {
        if (colorsConfig == null) {
            return null;
        }
        
        String path = "blocks." + blockDataString;
        
        if (!colorsConfig.contains(path)) {
            return null;
        }
        
        try {
            int r = colorsConfig.getInt(path + ".r");
            int g = colorsConfig.getInt(path + ".g");
            int b = colorsConfig.getInt(path + ".b");
            
            return new BlockColor(r, g, b);
        } catch (Exception e) {
            plugin.getLogger().warning(String.format("Error reading color for block %s: %s", blockDataString, e.getMessage()));
            return null;
        }
    }
    
    /**
     * 從地圖顏色動態獲取方塊顏色
     * @param blockData 方塊數據
     * @return 方塊顏色
     */
    private BlockColor getColorFromMapColor(BlockData blockData) {
        try {
            Color mapColor = blockData.getMapColor();
            return new BlockColor(mapColor.getRed(), mapColor.getGreen(), mapColor.getBlue());
        } catch (Exception e) {
            plugin.getLogger().warning(String.format("Error retrieving map color for block %s: %s", blockData.getAsString(), e.getMessage()));
            return null;
        }
    }
    
    /**
     * 快取顏色到記憶體和配置檔案
     * @param blockDataString 方塊數據字串
     * @param color 方塊顏色
     */
    public void cacheColor(String blockDataString, BlockColor color) {
        // 快取到記憶體
        colorCache.put(blockDataString, color);
        
        // 寫入配置檔案（非同步處理以避免阻塞）
        if (colorsConfig != null) {
            String path = "blocks." + blockDataString;
            colorsConfig.set(path + ".color", color.getHexString());
            colorsConfig.set(path + ".r", color.getRed());
            colorsConfig.set(path + ".g", color.getGreen());
            colorsConfig.set(path + ".b", color.getBlue());
            
            // 非同步儲存
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    colorsConfig.save(colorsFile);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to save color cache", e);
                }
            });
        }
    }
    
    /**
     * 清除記憶體快取
     */
    public void clearCache() {
        colorCache.clear();
        plugin.getLogger().info("Color cache cleared");
    }
    
    /**
     * 重新載入顏色資料
     * @return 是否成功重新載入
     */
    public boolean reload() {
        clearCache();
        return loadColorsFromFile();
    }
    
    /**
     * 獲取快取大小
     * @return 快取中的顏色數量
     */
    public int getCacheSize() {
        return colorCache.size();
    }
}
