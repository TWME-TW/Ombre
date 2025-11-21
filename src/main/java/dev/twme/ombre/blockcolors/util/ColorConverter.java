package dev.twme.ombre.blockcolors.util;

/**
 * 顏色轉換工具
 * 提供 RGB、HEX、Lab 之間的轉換
 */
public class ColorConverter {

    /**
     * RGB 轉 HEX
     * 
     * @param red 紅色值 (0-255)
     * @param green 綠色值 (0-255)
     * @param blue 藍色值 (0-255)
     * @return HEX 色碼 (例如: "#FF0000")
     */
    public static String rgbToHex(int red, int green, int blue) {
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    /**
     * RGB int 轉 HEX
     * 
     * @param rgb RGB 整數值
     * @return HEX 色碼
     */
    public static String rgbToHex(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return rgbToHex(red, green, blue);
    }

    /**
     * HEX 轉 RGB int
     * 
     * @param hex HEX 色碼 (例如: "#FF0000" 或 "FF0000")
     * @return RGB 整數值，失敗返回 -1
     */
    public static int hexToRgb(String hex) {
        try {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 驗證 HEX 色碼格式
     * 
     * @param hex HEX 色碼
     * @return 是否有效
     */
    public static boolean isValidHex(String hex) {
        if (hex == null) {
            return false;
        }
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        return hex.matches("^[0-9A-Fa-f]{6}$");
    }

    /**
     * RGB 轉 Lab 色彩空間
     * Lab 色彩空間更接近人眼對顏色的感知
     * 
     * @param rgb RGB 整數值
     * @return Lab 值 [L, a, b]
     */
    public static double[] rgbToLab(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return rgbToLab(red, green, blue);
    }

    /**
     * RGB 轉 Lab 色彩空間
     * 
     * @param red 紅色值 (0-255)
     * @param green 綠色值 (0-255)
     * @param blue 藍色值 (0-255)
     * @return Lab 值 [L, a, b]
     */
    public static double[] rgbToLab(int red, int green, int blue) {
        // 步驟 1: RGB -> XYZ
        double[] xyz = rgbToXyz(red, green, blue);
        
        // 步驟 2: XYZ -> Lab
        return xyzToLab(xyz);
    }

    /**
     * RGB 轉 XYZ 色彩空間
     * 使用 sRGB 色彩空間和 D65 光源
     */
    private static double[] rgbToXyz(int red, int green, int blue) {
        // 正規化到 0-1
        double r = red / 255.0;
        double g = green / 255.0;
        double b = blue / 255.0;

        // Gamma 校正 (sRGB)
        r = (r > 0.04045) ? Math.pow((r + 0.055) / 1.055, 2.4) : r / 12.92;
        g = (g > 0.04045) ? Math.pow((g + 0.055) / 1.055, 2.4) : g / 12.92;
        b = (b > 0.04045) ? Math.pow((b + 0.055) / 1.055, 2.4) : b / 12.92;

        // 轉換為 XYZ (使用 D65 光源的轉換矩陣)
        double x = r * 0.4124564 + g * 0.3575761 + b * 0.1804375;
        double y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750;
        double z = r * 0.0193339 + g * 0.1191920 + b * 0.9503041;

        return new double[]{x * 100, y * 100, z * 100};
    }

    /**
     * XYZ 轉 Lab 色彩空間
     * 使用 D65 標準觀察者 (2°)
     */
    private static double[] xyzToLab(double[] xyz) {
        // D65 標準光源的參考白點
        final double REF_X = 95.047;
        final double REF_Y = 100.000;
        final double REF_Z = 108.883;

        double x = xyz[0] / REF_X;
        double y = xyz[1] / REF_Y;
        double z = xyz[2] / REF_Z;

        // 套用 Lab 轉換函數
        x = labFunction(x);
        y = labFunction(y);
        z = labFunction(z);

        double l = (116 * y) - 16;
        double a = 500 * (x - y);
        double b = 200 * (y - z);

        return new double[]{l, a, b};
    }

    /**
     * Lab 轉換的輔助函數
     */
    private static double labFunction(double t) {
        final double DELTA = 6.0 / 29.0;
        if (t > DELTA * DELTA * DELTA) {
            return Math.cbrt(t);
        } else {
            return t / (3 * DELTA * DELTA) + (4.0 / 29.0);
        }
    }

    /**
     * 限制 RGB 值在有效範圍內 (0-255)
     */
    public static int clampRgb(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * 建立 RGB 整數值
     * 
     * @param red 紅色值 (0-255)
     * @param green 綠色值 (0-255)
     * @param blue 藍色值 (0-255)
     * @return RGB 整數值
     */
    public static int createRgb(int red, int green, int blue) {
        red = clampRgb(red);
        green = clampRgb(green);
        blue = clampRgb(blue);
        return (red << 16) | (green << 8) | blue;
    }
}
