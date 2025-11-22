package dev.twme.ombre.blockpalettes.util;

import java.util.List;

import org.bukkit.Material;

import dev.twme.ombre.blockpalettes.api.PaletteData;

/**
 * Material 版本驗證工具
 * 用於檢查調色板中的方塊是否在當前伺服器版本中可用
 */
public class MaterialValidator {
    
    /**
     * 驗證調色板是否包含當前版本不存在的物品
     * 
     * @param palette 要驗證的調色板
     * @return true 如果所有物品都有效，false 如果有任何物品不存在
     */
    public static boolean isValidPalette(PaletteData palette) {
        if (palette == null || palette.getBlocks() == null) {
            return false;
        }
        
        List<String> blocks = palette.getBlocks();
        
        // 檢查每個方塊是否在當前版本中存在
        for (String blockId : blocks) {
            if (!isValidMaterial(blockId)) {
                // 靜默失敗：發現無效物品時直接返回 false，不記錄 log
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 檢查單一 Material 是否有效
     * 
     * @param blockId 方塊 ID (snake_case 格式，例如: "red_wool")
     * @return true 如果 Material 存在且為方塊，false 否則
     */
    private static boolean isValidMaterial(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return false;
        }
        
        try {
            // 嘗試將 blockId 轉換為 Material
            Material material = Material.valueOf(blockId.toUpperCase());
            
            // 檢查是否為方塊（排除物品）
            return material.isBlock();
        } catch (IllegalArgumentException e) {
            // Material 不存在於當前版本，靜默失敗
            return false;
        }
    }
    
    /**
     * 批次過濾調色板列表，移除包含無效物品的調色板
     * 
     * @param palettes 要過濾的調色板列表
     * @return 過濾後的調色板列表（只包含有效的調色板）
     */
    public static List<PaletteData> filterValidPalettes(List<PaletteData> palettes) {
        if (palettes == null) {
            return List.of();
        }
        
        // 使用 Stream API 過濾出有效的調色板
        return palettes.stream()
                .filter(MaterialValidator::isValidPalette)
                .toList();
    }
}
