package dev.twme.ombre.color;

import org.bukkit.Color;

/**
 * 方塊顏色類別
 * 表示 RGB 顏色值
 */
public class BlockColor {
    
    private final int red;
    private final int green;
    private final int blue;
    
    public BlockColor(int red, int green, int blue) {
        this.red = clamp(red);
        this.green = clamp(green);
        this.blue = clamp(blue);
    }
    
    /**
     * 從 Bukkit Color 建立
     */
    public static BlockColor fromBukkitColor(Color color) {
        return new BlockColor(color.getRed(), color.getGreen(), color.getBlue());
    }
    
    /**
     * 從十六進制字串建立
     */
    public static BlockColor fromHex(String hex) {
        hex = hex.replace("#", "");
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }
        
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        
        return new BlockColor(r, g, b);
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
    
    /**
     * 轉換為十六進制字串
     */
    public String getHexString() {
        return String.format("%02X%02X%02X", red, green, blue);
    }
    
    /**
     * 轉換為 Bukkit Color
     */
    public Color toBukkitColor() {
        return Color.fromRGB(red, green, blue);
    }
    
    /**
     * 計算與另一個顏色的距離（歐氏距離）
     */
    public double distanceTo(BlockColor other) {
        int dr = this.red - other.red;
        int dg = this.green - other.green;
        int db = this.blue - other.blue;
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
    
    /**
     * 線性插值到另一個顏色
     * @param other 目標顏色
     * @param factor 插值因子 (0.0 到 1.0)
     * @return 插值後的顏色
     */
    public BlockColor lerp(BlockColor other, double factor) {
        factor = Math.max(0.0, Math.min(1.0, factor)); // 限制在 0-1 範圍
        
        int r = (int) (this.red + (other.red - this.red) * factor);
        int g = (int) (this.green + (other.green - this.green) * factor);
        int b = (int) (this.blue + (other.blue - this.blue) * factor);
        
        return new BlockColor(r, g, b);
    }
    
    /**
     * 檢查是否為透明顏色（全黑）
     */
    public boolean isTransparent() {
        return red == 0 && green == 0 && blue == 0;
    }
    
    /**
     * 檢查是否與另一個顏色相同
     */
    public boolean isSameColor(BlockColor other) {
        return this.red == other.red && 
               this.green == other.green && 
               this.blue == other.blue;
    }
    
    /**
     * 限制顏色值在 0-255 範圍內
     */
    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BlockColor that = (BlockColor) obj;
        return red == that.red && green == that.green && blue == that.blue;
    }
    
    @Override
    public int hashCode() {
        int result = red;
        result = 31 * result + green;
        result = 31 * result + blue;
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("BlockColor{R=%d, G=%d, B=%d, Hex=#%s}", 
            red, green, blue, getHexString());
    }
}
