package dev.twme.ombre.blockcolors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import dev.twme.ombre.blockcolors.data.BlockCategory;
import dev.twme.ombre.blockcolors.data.BlockColorData;
import dev.twme.ombre.blockcolors.data.ColorMatch;
import dev.twme.ombre.blockcolors.util.ColorConverter;
import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 顏色匹配演算法
 * 使用 Delta E 2000 (CIE2000) 計算顏色相似度
 */
public class ColorMatcher {
    private static BlockColorCache cache;
    
    // 快取最近計算的顏色匹配結果
    private static LoadingCache<Integer, List<ColorMatch>> matchCache;
    
    // 預計算的 Lab 值快取（避免重複轉換）
    private static final Map<Integer, double[]> labCache = new ConcurrentHashMap<>();
    
    /**
     * 初始化 ColorMatcher
     */
    public static void initialize(BlockColorCache blockColorCache) {
        cache = blockColorCache;
        
        // 建立匹配結果快取
        matchCache = CacheBuilder.newBuilder()
            .maximumSize(500)  // 快取最多 500 種顏色的匹配結果
            .expireAfterWrite(10, TimeUnit.MINUTES)  // 10 分鐘後過期
            .recordStats()  // 記錄快取統計
            .build(new CacheLoader<Integer, List<ColorMatch>>() {
                @Override
                public List<ColorMatch> load(Integer targetColor) {
                    return calculateMatchesInternal(targetColor);
                }
            });
    }
    
    /**
     * 計算兩個顏色之間的差異
     * 使用 Delta E 2000 公式（業界標準）
     * 
     * @param targetRgb 目標顏色的 RGB 值
     * @param blockData 方塊顏色資料（含 Lab 值）
     * @return 顏色差異值（0-100，越小越相似）
     */
    public static double calculateDeltaE(int targetRgb, BlockColorData blockData) {
        // 1. 將目標顏色轉換到 Lab 空間
        double[] targetLab = labCache.computeIfAbsent(targetRgb, ColorConverter::rgbToLab);
        
        // 2. 使用 API 提供的 Lab 值（無需轉換，更精確）
        double[] blockLab = blockData.getLab();
        
        // 3. 計算 Delta E 2000
        return calculateDeltaE2000(targetLab, blockLab);
    }
    
    /**
     * Delta E 2000 (CIE2000) 演算法實作
     * 這是目前最精確的顏色差異計算方法
     * 
     * @param lab1 第一個 Lab 顏色 [L, a, b]
     * @param lab2 第二個 Lab 顏色 [L, a, b]
     * @return Delta E 值（0-100，越小越相似）
     */
    public static double calculateDeltaE2000(double[] lab1, double[] lab2) {
        double L1 = lab1[0], a1 = lab1[1], b1 = lab1[2];
        double L2 = lab2[0], a2 = lab2[1], b2 = lab2[2];
        
        // 計算 C1 和 C2 (色度)
        double C1 = Math.sqrt(a1 * a1 + b1 * b1);
        double C2 = Math.sqrt(a2 * a2 + b2 * b2);
        
        // 計算平均色度
        double avgC = (C1 + C2) / 2.0;
        
        // 計算 G
        double G = 0.5 * (1 - Math.sqrt(Math.pow(avgC, 7) / (Math.pow(avgC, 7) + Math.pow(25, 7))));
        
        // 調整 a 值
        double a1Prime = (1 + G) * a1;
        double a2Prime = (1 + G) * a2;
        
        // 重新計算 C'
        double C1Prime = Math.sqrt(a1Prime * a1Prime + b1 * b1);
        double C2Prime = Math.sqrt(a2Prime * a2Prime + b2 * b2);
        
        // 計算 h' (色調角)
        double h1Prime = calculateHuePrime(a1Prime, b1);
        double h2Prime = calculateHuePrime(a2Prime, b2);
        
        // 計算差值
        double deltaLPrime = L2 - L1;
        double deltaCPrime = C2Prime - C1Prime;
        double deltaHPrime = calculateDeltaHPrime(C1Prime, C2Prime, h1Prime, h2Prime);
        double deltaHPrimeValue = 2 * Math.sqrt(C1Prime * C2Prime) * Math.sin(Math.toRadians(deltaHPrime / 2));
        
        // 計算平均值
        double avgLPrime = (L1 + L2) / 2.0;
        double avgCPrime = (C1Prime + C2Prime) / 2.0;
        double avgHPrime = calculateAvgHPrime(C1Prime, C2Prime, h1Prime, h2Prime);
        
        // 計算 T
        double T = 1 - 0.17 * Math.cos(Math.toRadians(avgHPrime - 30)) +
                   0.24 * Math.cos(Math.toRadians(2 * avgHPrime)) +
                   0.32 * Math.cos(Math.toRadians(3 * avgHPrime + 6)) -
                   0.20 * Math.cos(Math.toRadians(4 * avgHPrime - 63));
        
        // 計算 SL, SC, SH
        double SL = 1 + (0.015 * Math.pow(avgLPrime - 50, 2)) / Math.sqrt(20 + Math.pow(avgLPrime - 50, 2));
        double SC = 1 + 0.045 * avgCPrime;
        double SH = 1 + 0.015 * avgCPrime * T;
        
        // 計算 RT (旋轉項)
        double deltaTheta = 30 * Math.exp(-Math.pow((avgHPrime - 275) / 25, 2));
        double RC = 2 * Math.sqrt(Math.pow(avgCPrime, 7) / (Math.pow(avgCPrime, 7) + Math.pow(25, 7)));
        double RT = -RC * Math.sin(Math.toRadians(2 * deltaTheta));
        
        // 權重因子 (可調整)
        double kL = 1.0;
        double kC = 1.0;
        double kH = 1.0;
        
        // 最終 Delta E 2000
        double deltaE = Math.sqrt(
            Math.pow(deltaLPrime / (kL * SL), 2) +
            Math.pow(deltaCPrime / (kC * SC), 2) +
            Math.pow(deltaHPrimeValue / (kH * SH), 2) +
            RT * (deltaCPrime / (kC * SC)) * (deltaHPrimeValue / (kH * SH))
        );
        
        return deltaE;
    }
    
    /**
     * 計算色調角 h'
     */
    private static double calculateHuePrime(double aPrime, double b) {
        if (aPrime == 0 && b == 0) {
            return 0;
        }
        double hPrime = Math.toDegrees(Math.atan2(b, aPrime));
        if (hPrime < 0) {
            hPrime += 360;
        }
        return hPrime;
    }
    
    /**
     * 計算色調角差值 Δh'
     */
    private static double calculateDeltaHPrime(double C1Prime, double C2Prime, double h1Prime, double h2Prime) {
        if (C1Prime * C2Prime == 0) {
            return 0;
        }
        
        double diff = h2Prime - h1Prime;
        if (Math.abs(diff) <= 180) {
            return diff;
        } else if (diff > 180) {
            return diff - 360;
        } else {
            return diff + 360;
        }
    }
    
    /**
     * 計算平均色調角 H̄'
     */
    private static double calculateAvgHPrime(double C1Prime, double C2Prime, double h1Prime, double h2Prime) {
        if (C1Prime * C2Prime == 0) {
            return h1Prime + h2Prime;
        }
        
        double diff = Math.abs(h1Prime - h2Prime);
        if (diff <= 180) {
            return (h1Prime + h2Prime) / 2.0;
        } else if (h1Prime + h2Prime < 360) {
            return (h1Prime + h2Prime + 360) / 2.0;
        } else {
            return (h1Prime + h2Prime - 360) / 2.0;
        }
    }
    
    /**
     * 從方塊中提取平均顏色
     * 
     * @param material 方塊材質類型
     * @return RGB 顏色值，若無法提取則返回 -1
     */
    public static int extractBlockColor(Material material) {
        // 檢查是否在快取的方塊顏色資料中
        BlockColorData cachedData = cache.getBlockByMaterial(material);
        if (cachedData != null) {
            return cachedData.getRgb();
        }
        
        // 若不在快取中，返回預設顏色或 -1
        return -1;
    }
    
    /**
     * 匹配最相似的方塊（使用快取優化）
     * 
     * @param targetColor 目標顏色
     * @param category 方塊類別篩選
     * @param maxResults 最大結果數
     * @return 排序後的匹配方塊清單
     */
    public static List<ColorMatch> findMatchingBlocks(
        int targetColor,
        BlockCategory category,
        int maxResults
    ) {
        try {
            // 從快取取得或計算匹配結果
            List<ColorMatch> allMatches = matchCache.get(targetColor);
            
            // 根據類別篩選並限制結果數量
            return allMatches.stream()
                .filter(match -> {
                    // 過濾掉沒有成功映射 Material 的方塊
                    if (match.getBlock().getMaterial() == null) {
                        return false;
                    }
                    // 類別篩選
                    return category == BlockCategory.ALL || match.getBlock().getCategory() == category;
                })
                .limit(maxResults)
                .collect(Collectors.toList());
                
        } catch (ExecutionException e) {
            // 快取載入失敗，直接計算
            return calculateMatchesInternal(targetColor, category, maxResults);
        }
    }
    
    /**
     * 內部計算方法（不使用快取）
     */
    private static List<ColorMatch> calculateMatchesInternal(int targetColor) {
        List<BlockColorData> allBlocks = cache.getAllBlocks();
        
        // 取得或計算目標顏色的 Lab 值
        double[] targetLab = labCache.computeIfAbsent(targetColor, ColorConverter::rgbToLab);
        
        // 並行計算所有方塊的相似度（提升效能）
        return allBlocks.parallelStream()
            .filter(block -> block.getMaterial() != null)  // 只處理成功映射的方塊
            .map(block -> {
                double deltaE = calculateDeltaE2000(targetLab, block.getLab());
                double similarity = Math.max(0, 100 - deltaE);
                return new ColorMatch(block, similarity, deltaE);
            })
            .sorted(Comparator.comparingDouble(ColorMatch::getSimilarity).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * 內部計算方法（帶類別和數量限制）
     */
    private static List<ColorMatch> calculateMatchesInternal(
        int targetColor,
        BlockCategory category,
        int maxResults
    ) {
        List<ColorMatch> allMatches = calculateMatchesInternal(targetColor);
        
        return allMatches.stream()
            .filter(match -> category == BlockCategory.ALL || match.getBlock().getCategory() == category)
            .limit(maxResults)
            .collect(Collectors.toList());
    }
    
    /**
     * 清除快取（在重新載入資料時呼叫）
     */
    public static void clearCache() {
        if (matchCache != null) {
            matchCache.invalidateAll();
        }
        labCache.clear();
    }
    
    /**
     * 取得快取統計資訊
     */
    public static CacheStats getCacheStats() {
        return matchCache != null ? matchCache.stats() : null;
    }
}
