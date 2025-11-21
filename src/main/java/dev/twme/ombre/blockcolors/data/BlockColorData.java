package dev.twme.ombre.blockcolors.data;

import org.bukkit.Material;

import java.util.Arrays;

/**
 * 方塊顏色資料
 * 對應 API 的 color_data.json 格式
 */
public class BlockColorData {
    private String id;                   // API 中的 ID (例如: "0", "837")
    private String displayName;          // 顯示名稱 (例如: "Cherry Planks")
    private String hexColor;             // HEX 顏色碼 (例如: "#e6c1ba")
    private int rgb;                     // RGB 顏色值 (轉換自 hex)
    private int red, green, blue;        // 分解的 RGB 值
    private double[] lab;                // Lab 色彩空間值 [L, a, b]
    private String textureName;          // 材質名稱 (例如: "cherry_planks")
    private Material material;           // Minecraft Material 枚舉 (從 textureName 解析)
    private boolean isDecoration;        // 是否為裝飾方塊 (API: is_decoration)
    private boolean show3d;              // 是否顯示 3D (API: show_3d)
    private BlockCategory category;      // 方塊類別 (根據 isDecoration 判斷)

    public BlockColorData() {
    }

    public BlockColorData(String id, String displayName, String hexColor, double[] lab,
                          String textureName, boolean isDecoration, boolean show3d) {
        this.id = id;
        this.displayName = displayName;
        this.hexColor = hexColor;
        this.lab = lab;
        this.textureName = textureName;
        this.isDecoration = isDecoration;
        this.show3d = show3d;
        this.category = isDecoration ? BlockCategory.DECORATION : BlockCategory.BUILDING;
        
        // 解析 HEX 到 RGB
        parseHexToRgb();
    }

    /**
     * 將 HEX 顏色轉換為 RGB 值
     */
    private void parseHexToRgb() {
        if (hexColor != null && hexColor.startsWith("#") && hexColor.length() == 7) {
            try {
                this.rgb = Integer.parseInt(hexColor.substring(1), 16);
                this.red = (rgb >> 16) & 0xFF;
                this.green = (rgb >> 8) & 0xFF;
                this.blue = rgb & 0xFF;
            } catch (NumberFormatException e) {
                // 預設為白色
                this.rgb = 0xFFFFFF;
                this.red = 255;
                this.green = 255;
                this.blue = 255;
            }
        }
    }

    /**
     * 從 texture_name 解析對應的 Minecraft Material
     * 使用遞迴截斷策略：如果完整名稱不存在，則移除最後一個 "_狀態" 後重試
     * 
     * 例如：
     * - "acacia_door_bottom" → 檢查 ACACIA_DOOR_BOTTOM → 不存在
     *                        → 檢查 ACACIA_DOOR → 存在 ✓
     * - "cherry_planks" → 檢查 CHERRY_PLANKS → 存在 ✓
     * - "stripped_oak_log_top" → 檢查 STRIPPED_OAK_LOG_TOP → 不存在
     *                           → 檢查 STRIPPED_OAK_LOG → 存在 ✓
     */
    public static Material parseMaterialFromTextureName(String textureName) {
        if (textureName == null || textureName.isEmpty()) {
            return null;
        }
        
        // 將 texture_name 轉換為大寫並嘗試匹配 Material
        String[] parts = textureName.split("_");
        
        // 從完整名稱開始，逐步移除最後一個部分
        for (int i = parts.length; i > 0; i--) {
            String materialName = String.join("_", Arrays.copyOfRange(parts, 0, i)).toUpperCase();
            try {
                Material material = Material.valueOf(materialName);
                if (material.isBlock()) {  // 確保是方塊而非物品
                    return material;
                }
            } catch (IllegalArgumentException e) {
                // 該名稱不存在，繼續嘗試下一個
            }
        }
        
        return null;  // 無法找到對應的 Material
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getHexColor() {
        return hexColor;
    }

    public void setHexColor(String hexColor) {
        this.hexColor = hexColor;
        parseHexToRgb();
    }

    public int getRgb() {
        return rgb;
    }

    public void setRgb(int rgb) {
        this.rgb = rgb;
        this.red = (rgb >> 16) & 0xFF;
        this.green = (rgb >> 8) & 0xFF;
        this.blue = rgb & 0xFF;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
        updateRgb();
    }

    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = green;
        updateRgb();
    }

    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = blue;
        updateRgb();
    }

    private void updateRgb() {
        this.rgb = (red << 16) | (green << 8) | blue;
    }

    public double[] getLab() {
        return lab;
    }

    public void setLab(double[] lab) {
        this.lab = lab;
    }

    public String getTextureName() {
        return textureName;
    }

    public void setTextureName(String textureName) {
        this.textureName = textureName;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public boolean isDecoration() {
        return isDecoration;
    }

    public void setDecoration(boolean decoration) {
        isDecoration = decoration;
        this.category = decoration ? BlockCategory.DECORATION : BlockCategory.BUILDING;
    }

    public boolean isShow3d() {
        return show3d;
    }

    public void setShow3d(boolean show3d) {
        this.show3d = show3d;
    }

    public BlockCategory getCategory() {
        return category;
    }

    public void setCategory(BlockCategory category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "BlockColorData{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", hexColor='" + hexColor + '\'' +
                ", material=" + material +
                ", category=" + category +
                '}';
    }
}
