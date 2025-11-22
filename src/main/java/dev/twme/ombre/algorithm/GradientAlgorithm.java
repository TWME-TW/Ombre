package dev.twme.ombre.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import dev.twme.ombre.color.BlockColor;
import dev.twme.ombre.color.ColorService;
import dev.twme.ombre.data.GradientConfig;
import dev.twme.ombre.palette.BlockFilterManager;

/**
 * 漸層算法
 * 負責計算方塊之間的顏色漸層並填充整個區域
 */
public class GradientAlgorithm {
    
    private final Plugin plugin;
    private final ColorService colorService;
    private final BlockFilterManager blockFilterManager;
    private final int rows;
    private final int cols;
    
    // 當前處理的玩家UUID（用於獲取可用方塊）
    private UUID currentPlayerUuid;
    
    // 顏色點（種子方塊）
    private static class ColorPoint {
        final int row;
        final int col;
        final BlockColor color;
        final String blockDataString;
        
        ColorPoint(int row, int col, BlockColor color, String blockDataString) {
            this.row = row;
            this.col = col;
            this.color = color;
            this.blockDataString = blockDataString;
        }
        
        double distanceTo(int targetRow, int targetCol) {
            int dr = row - targetRow;
            int dc = col - targetCol;
            return Math.sqrt(dr * dr + dc * dc);
        }
    }
    
    public GradientAlgorithm(Plugin plugin, ColorService colorService, 
                            BlockFilterManager blockFilterManager, int rows, int cols) {
        this.plugin = plugin;
        this.colorService = colorService;
        this.blockFilterManager = blockFilterManager;
        this.rows = rows;
        this.cols = cols;
    }
    
    /**
     * 設定當前玩家UUID（用於獲取方塊過濾設定）
     */
    public void setCurrentPlayer(UUID playerUuid) {
        this.currentPlayerUuid = playerUuid;
    }
    
    /**
     * 計算漸層並生成配置
     * @param inputBlocks 輸入的方塊映射（位置 -> BlockData字串）
     * @return 填充完整的漸層配置
     */
    public Map<GradientConfig.Position, String> calculateGradient(
            Map<GradientConfig.Position, String> inputBlocks) {
        
        // 提取顏色點
        List<ColorPoint> colorPoints = extractColorPoints(inputBlocks);
        
        if (colorPoints.isEmpty()) {
            return new HashMap<>();
        }
        
        // 如果只有一個顏色點，填充整個區域
        if (colorPoints.size() == 1) {
            return fillWithSingleColor(colorPoints.get(0));
        }
        
        // 計算每個位置的顏色並找到最接近的方塊
        Map<GradientConfig.Position, String> result = new HashMap<>();
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                GradientConfig.Position pos = new GradientConfig.Position(row, col);
                
                // 如果這個位置是原始的種子方塊，保留原始方塊
                if (inputBlocks.containsKey(pos)) {
                    result.put(pos, inputBlocks.get(pos));
                    continue;
                }
                
                // 計算這個位置的插值顏色
                BlockColor interpolatedColor = interpolateColor(row, col, colorPoints);
                
                // 找到最接近這個顏色的方塊
                String closestBlock = findClosestBlock(interpolatedColor);
                
                if (closestBlock != null) {
                    result.put(pos, closestBlock);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 提取顏色點
     */
    private List<ColorPoint> extractColorPoints(Map<GradientConfig.Position, String> inputBlocks) {
        List<ColorPoint> points = new ArrayList<>();
        
        for (Map.Entry<GradientConfig.Position, String> entry : inputBlocks.entrySet()) {
            GradientConfig.Position pos = entry.getKey();
            String blockDataString = entry.getValue();
            
            // 獲取方塊顏色
            try {
                BlockData blockData = Bukkit.createBlockData(blockDataString);
                BlockColor color = colorService.getBlockColor(blockData);
                
                if (color != null && !color.isTransparent()) {
                    points.add(new ColorPoint(pos.getRow(), pos.getCol(), color, blockDataString));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error processing block data: " + blockDataString);
            }
        }
        
        return points;
    }
    
    /**
     * 用單一顏色填充
     */
    private Map<GradientConfig.Position, String> fillWithSingleColor(ColorPoint point) {
        Map<GradientConfig.Position, String> result = new HashMap<>();
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                result.put(new GradientConfig.Position(row, col), point.blockDataString);
            }
        }
        
        return result;
    }
    
    /**
     * 插值計算顏色（加權平均）
     */
    private BlockColor interpolateColor(int row, int col, List<ColorPoint> colorPoints) {
        if (colorPoints.size() == 1) {
            return colorPoints.get(0).color;
        }
        
        // 計算到每個顏色點的距離
        double[] distances = new double[colorPoints.size()];
        double minDistance = Double.MAX_VALUE;
        
        for (int i = 0; i < colorPoints.size(); i++) {
            distances[i] = colorPoints.get(i).distanceTo(row, col);
            if (distances[i] < minDistance) {
                minDistance = distances[i];
            }
        }
        
        // 如果正好在某個顏色點上，直接返回該顏色
        if (minDistance < 0.01) {
            for (int i = 0; i < colorPoints.size(); i++) {
                if (distances[i] < 0.01) {
                    return colorPoints.get(i).color;
                }
            }
        }
        
        // 計算權重（距離的倒數，距離越近權重越大）
        double[] weights = new double[colorPoints.size()];
        double totalWeight = 0;
        
        for (int i = 0; i < colorPoints.size(); i++) {
            // 使用平方倒數增強近距離顏色的影響
            weights[i] = 1.0 / (distances[i] * distances[i] + 0.1); // 加0.1避免除以零
            totalWeight += weights[i];
        }
        
        // 歸一化權重
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= totalWeight;
        }
        
        // 加權平均計算顏色
        double r = 0, g = 0, b = 0;
        for (int i = 0; i < colorPoints.size(); i++) {
            BlockColor color = colorPoints.get(i).color;
            r += color.getRed() * weights[i];
            g += color.getGreen() * weights[i];
            b += color.getBlue() * weights[i];
        }
        
        return new BlockColor((int) r, (int) g, (int) b);
    }
    
    /**
     * 找到最接近指定顏色的方塊
     * 使用玩家的色表設定來過濾可用方塊
     */
    private String findClosestBlock(BlockColor targetColor) {
        String closestBlock = null;
        double minDistance = Double.MAX_VALUE;
        
        // 獲取玩家可用的方塊集合
        Set<Material> availableBlocks;
        if (currentPlayerUuid != null) {
            availableBlocks = blockFilterManager.getAvailableBlocks(currentPlayerUuid);
        } else {
            // 如果沒有設定玩家，使用所有方塊（並套用預設排除）
            availableBlocks = blockFilterManager.getAvailableBlocks(null);
        }
        
        // 遍歷可用的方塊材質
        for (Material material : availableBlocks) {
            try {
                BlockData blockData = material.createBlockData();
                BlockColor color = colorService.getBlockColor(blockData);
                
                if (color != null && !color.isTransparent()) {
                    double distance = targetColor.distanceTo(color);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestBlock = blockData.getAsString();
                    }
                }
            } catch (Exception e) {
                // 忽略錯誤，繼續下一個
            }
        }
        
        return closestBlock;
    }
    
    /**
     * 檢查配置是否有效（符合最少方塊數、不全為透明、不全為相同顏色）
     */
    public boolean isValidConfiguration(Map<GradientConfig.Position, String> inputBlocks, 
                                       int minBlocks, boolean ignoreTransparent, 
                                       boolean ignoreSameColor) {
        if (inputBlocks.size() < minBlocks) {
            return false;
        }
        
        List<ColorPoint> colorPoints = extractColorPoints(inputBlocks);
        
        if (colorPoints.isEmpty()) {
            return false;
        }
        
        // 檢查是否忽略透明方塊
        if (ignoreTransparent && colorPoints.stream().allMatch(p -> p.color.isTransparent())) {
            return false;
        }
        
        // 檢查是否所有顏色都相同
        if (ignoreSameColor && colorPoints.size() > 1) {
            BlockColor firstColor = colorPoints.get(0).color;
            boolean allSame = colorPoints.stream().allMatch(p -> p.color.isSameColor(firstColor));
            if (allSame) {
                return false;
            }
        }
        
        return true;
    }
}
