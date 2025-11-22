package dev.twme.ombre.color;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * 顏色資料生成器
 * 負責生成 colors.yml 檔案，包含所有方塊及其狀態的顏色資訊
 */
public class ColorDataGenerator {
    
    private final Plugin plugin;
    private final File colorsFile;
    
    public ColorDataGenerator(Plugin plugin) {
        this.plugin = plugin;
        this.colorsFile = new File(plugin.getDataFolder(), "colors.yml");
    }
    
    /**
     * 檢查並生成 colors.yml 檔案
     * @return 是否成功生成或檔案已存在
     */
    public boolean generateIfNotExists() {
        if (colorsFile.exists()) {
            plugin.getLogger().info("colors.yml already exists, skipping generation");
            return true;
        }
        
        plugin.getLogger().info("Starting colors.yml generation...");
        return generateColorsFile();
    }
    
    /**
     * 強制重新生成 colors.yml 檔案
     * @return 是否成功生成
     */
    public boolean regenerateColorsFile() {
        plugin.getLogger().info("Forcing colors.yml regeneration...");
        if (colorsFile.exists()) {
            colorsFile.delete();
        }
        return generateColorsFile();
    }
    
    /**
     * 生成 colors.yml 檔案
     * @return 是否成功生成
     */
    private boolean generateColorsFile() {
        try {
            // 確保資料夾存在
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // 獲取所有方塊數據顏色
            Map<String, ColorData> blockColors = getAllBlockDataColors();
            
            // 建立 YAML 配置
            YamlConfiguration config = new YamlConfiguration();
            
            // 寫入資料
            for (Map.Entry<String, ColorData> entry : blockColors.entrySet()) {
                String blockDataString = entry.getKey();
                ColorData colorData = entry.getValue();
                
                String path = "blocks." + blockDataString;
                config.set(path + ".color", colorData.getHexString());
                config.set(path + ".r", colorData.getRed());
                config.set(path + ".g", colorData.getGreen());
                config.set(path + ".b", colorData.getBlue());
            }
            
            // 儲存檔案
            config.save(colorsFile);
            
            plugin.getLogger().info(String.format("Successfully generated colors.yml with %d block states", blockColors.size()));
            return true;
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to generate colors.yml", e);
            return false;
        }
    }
    
    /**
     * 獲取所有方塊數據的顏色
     * @return 方塊數據字串到顏色的映射
     */
    private Map<String, ColorData> getAllBlockDataColors() {
        Map<String, ColorData> colorMap = new HashMap<>();
        int processedCount = 0;
        int skippedCount = 0;
        
        // 遍歷所有 Material
        for (Material material : Material.values()) {
            // 跳過非方塊材質
            if (!material.isBlock()) {
                continue;
            }
            
            // 跳過無法變成物品的方塊
            if (!material.isItem()) {
                skippedCount++;
                continue;
            }
            
            try {
                // 獲取預設的 BlockData
                BlockData blockData = material.createBlockData();
                
                // 獲取地圖顏色
                Color mapColor = blockData.getMapColor();
                
                // 獲取方塊數據字串（包含所有屬性）
                String blockDataString = blockData.getAsString();
                
                // 儲存顏色資料
                ColorData colorData = new ColorData(
                    mapColor.getRed(),
                    mapColor.getGreen(),
                    mapColor.getBlue()
                );
                
                colorMap.put(blockDataString, colorData);
                processedCount++;
                
                // 每處理 100 個方塊記錄一次
                if (processedCount % 100 == 0) {
                    plugin.getLogger().info(String.format("Processed %d block states so far...", processedCount));
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning(String.format("Error processing block %s: %s", material.name(), e.getMessage()));
                skippedCount++;
            }
        }
        
        plugin.getLogger().info(String.format("Processing complete: %d usable blocks, %d skipped", processedCount, skippedCount));
        return colorMap;
    }
    
    /**
     * 顏色資料內部類別
     */
    private static class ColorData {
        private final int red;
        private final int green;
        private final int blue;
        
        public ColorData(int red, int green, int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
        
        public int getRed() {
            return red;
        }
        
        public int getGreen() {
            return green;
        }
        
        public int getBlue() {
            return blue;
        }
        
        public String getHexString() {
            return String.format("%02X%02X%02X", red, green, blue);
        }
    }
}
