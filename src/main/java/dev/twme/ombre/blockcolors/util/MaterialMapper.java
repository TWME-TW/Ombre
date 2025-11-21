package dev.twme.ombre.blockcolors.util;

import dev.twme.ombre.blockcolors.data.BlockColorData;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

/**
 * Material 映射工具
 * 負責將 API 的 texture_name 映射到 Minecraft Material
 */
public class MaterialMapper {
    private static Logger logger;

    public static void setLogger(Logger pluginLogger) {
        logger = pluginLogger;
    }

    /**
     * 智能映射 texture_name 到 Material
     * 策略：從完整名稱開始，逐步移除最後一個 "_狀態" 直到找到匹配的 Material
     * 
     * @param textureName API 提供的材質名稱
     * @return 對應的 Material，若無法映射則返回 null
     */
    public static Material mapTextureName(String textureName) {
        if (textureName == null || textureName.isEmpty()) {
            return null;
        }
        
        String[] parts = textureName.split("_");
        
        // 從完整名稱開始嘗試
        for (int i = parts.length; i > 0; i--) {
            String materialName = String.join("_", 
                Arrays.copyOfRange(parts, 0, i)).toUpperCase();
            
            try {
                Material material = Material.valueOf(materialName);
                
                // 驗證是方塊而非物品
                if (material.isBlock()) {
                    return material;
                }
            } catch (IllegalArgumentException ignored) {
                // 繼續嘗試下一個組合
            }
        }
        
        return null;
    }

    /**
     * 批量映射並記錄無法映射的材質
     * 
     * @param blocks 方塊顏色資料集合
     * @param logUnmapped 是否記錄無法映射的材質
     * @return 映射結果 (textureName -> Material)
     */
    public static Map<String, Material> mapAllTextures(
        Collection<BlockColorData> blocks,
        boolean logUnmapped
    ) {
        Map<String, Material> mapping = new HashMap<>();
        List<String> unmapped = new ArrayList<>();
        
        for (BlockColorData block : blocks) {
            Material material = mapTextureName(block.getTextureName());
            
            if (material != null) {
                mapping.put(block.getTextureName(), material);
                // 同時更新 BlockColorData 的 material 欄位
                block.setMaterial(material);
            } else if (logUnmapped) {
                unmapped.add(block.getTextureName());
            }
        }
        
        if (logUnmapped && !unmapped.isEmpty() && logger != null) {
            logger.warning(
                "無法映射以下 " + unmapped.size() + " 個材質: " + 
                String.join(", ", unmapped)
            );
        }
        
        return mapping;
    }

    /**
     * 驗證 Material 是否有效且為方塊
     * 
     * @param material 要驗證的材質
     * @return 是否有效
     */
    public static boolean isValidBlockMaterial(Material material) {
        return material != null && material.isBlock();
    }

    /**
     * 取得映射統計資訊
     * 
     * @param blocks 方塊顏色資料集合
     * @return 統計字串
     */
    public static String getMappingStats(Collection<BlockColorData> blocks) {
        int total = blocks.size();
        long mapped = blocks.stream()
            .map(BlockColorData::getMaterial)
            .filter(Objects::nonNull)
            .count();
        
        return String.format("已映射 %d/%d 個方塊 (%.1f%%)", 
            mapped, total, (mapped * 100.0 / total));
    }
}
