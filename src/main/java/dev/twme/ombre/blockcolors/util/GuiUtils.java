package dev.twme.ombre.blockcolors.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * GUI 工具類別
 * 提供建立 GUI 物品的輔助方法
 */
public class GuiUtils {
    
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * 建立 GUI 物品
     * 
     * @param material 材質
     * @param displayName 顯示名稱
     * @param lore 描述文字
     * @return ItemStack
     */
    public static ItemStack createItem(Material material, String displayName, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // 使用 MiniMessage 解析顯示名稱
            meta.displayName(miniMessage.deserialize(displayName));
            if (lore.length > 0) {
                // 使用 MiniMessage 解析 lore
                List<Component> loreComponents = Arrays.stream(lore)
                    .map(miniMessage::deserialize)
                    .collect(Collectors.toList());
                meta.lore(loreComponents);
            }
            // 隱藏所有標籤
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * 建立 GUI 物品(帶 List lore)
     */
    public static ItemStack createItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // 使用 MiniMessage 解析顯示名稱
            meta.displayName(miniMessage.deserialize(displayName));
            if (lore != null && !lore.isEmpty()) {
                // 使用 MiniMessage 解析 lore
                List<Component> loreComponents = lore.stream()
                    .map(miniMessage::deserialize)
                    .collect(Collectors.toList());
                meta.lore(loreComponents);
            }
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * 建立 GUI 物品 (使用 Component)
     * 
     * @param material 材質
     * @param displayName 顯示名稱 Component
     * @param lore 描述文字 Components
     * @return ItemStack
     */
    public static ItemStack createItem(Material material, Component displayName, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(displayName);
            if (lore.length > 0) {
                meta.lore(Arrays.asList(lore));
            }
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * 建立 GUI 物品 (使用 Component 和 List)
     * 
     * @param material 材質
     * @param displayName 顯示名稱 Component
     * @param lore 描述文字 Component List
     * @return ItemStack
     */
    public static ItemStack createItem(Material material, Component displayName, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(displayName);
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * 建立帶自定義模型資料的物品
     */
    public static ItemStack createItemWithCustomModelData(Material material, String displayName, 
                                                          int customModelData, String... lore) {
        ItemStack item = createItem(material, displayName, lore);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * 建立玻璃片（用於顯示顏色）
     * 
     * @param rgb RGB 顏色值
     * @param displayName 顯示名稱
     * @param lore 描述文字
     * @return ItemStack
     */
    public static ItemStack createColoredPane(int rgb, String displayName, String... lore) {
        // 根據 RGB 值選擇最接近的羊毛顏色
        Material paneMaterial = getClosestGlassPane(rgb);
        return createItem(paneMaterial, displayName, lore);
    }

    /**
     * 建立玻璃片（用於顯示顏色，使用 Component）
     * 
     * @param rgb RGB 顏色值
     * @param displayName 顯示名稱 Component
     * @param lore 描述文字 Components
     * @return ItemStack
     */
    public static ItemStack createColoredPane(int rgb, Component displayName, Component... lore) {
        Material paneMaterial = getClosestGlassPane(rgb);
        return createItem(paneMaterial, displayName, lore);
    }

    /**
     * 建立玻璃片（用於顯示顏色，使用 Component List）
     * 
     * @param rgb RGB 顏色值
     * @param displayName 顯示名稱 Component
     * @param lore 描述文字 Component List
     * @return ItemStack
     */
    public static ItemStack createColoredPane(int rgb, Component displayName, List<Component> lore) {
        Material paneMaterial = getClosestGlassPane(rgb);
        return createItem(paneMaterial, displayName, lore);
    }

    /**
     * 建立染色皮革（用於顯示顏色）
     * 
     * @param rgb RGB 顏色值
     * @param displayName 顯示名稱 Component
     * @param lore 描述文字 Component List
     * @return ItemStack
     */
    public static ItemStack createColoredLeather(int rgb, Component displayName, List<Component> lore) {
        ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        
        if (meta instanceof LeatherArmorMeta) {
            LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
            
            // 設置顏色
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            leatherMeta.setColor(org.bukkit.Color.fromRGB(red, green, blue));
            
            // 設置名稱和 lore
            leatherMeta.displayName(displayName);
            if (lore != null && !lore.isEmpty()) {
                leatherMeta.lore(lore);
            }
            
            // 隱藏所有標籤
            leatherMeta.addItemFlags(ItemFlag.values());
            
            item.setItemMeta(leatherMeta);
        }
        
        return item;
    }

    /**
     * 根據 RGB 值獲取最接近的玻璃片材質
     */
    private static Material getClosestGlassPane(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        
        // 簡單的顏色映射邏輯
        if (red > 200 && green > 200 && blue > 200) return Material.WHITE_STAINED_GLASS_PANE;
        if (red < 50 && green < 50 && blue < 50) return Material.BLACK_STAINED_GLASS_PANE;
        if (red > 150 && green < 100 && blue < 100) return Material.RED_STAINED_GLASS_PANE;
        if (red < 100 && green > 150 && blue < 100) return Material.LIME_STAINED_GLASS_PANE;
        if (red < 100 && green < 100 && blue > 150) return Material.BLUE_STAINED_GLASS_PANE;
        if (red > 150 && green > 150 && blue < 100) return Material.YELLOW_STAINED_GLASS_PANE;
        if (red > 150 && green < 100 && blue > 150) return Material.MAGENTA_STAINED_GLASS_PANE;
        if (red < 100 && green > 150 && blue > 150) return Material.CYAN_STAINED_GLASS_PANE;
        if (red > 150 && green > 100 && blue < 100) return Material.ORANGE_STAINED_GLASS_PANE;
        if (red > 100 && green < 100 && blue > 150) return Material.PURPLE_STAINED_GLASS_PANE;
        if (red > 150 && green > 150 && blue > 150) return Material.LIGHT_GRAY_STAINED_GLASS_PANE;
        
        return Material.GRAY_STAINED_GLASS_PANE;
    }

    /**
     * 建立填充物品（空格）
     */
    public static ItemStack createFiller() {
        return createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    /**
     * 驗證槽位索引是否有效
     */
    public static boolean isValidSlot(int slot, int size) {
        return slot >= 0 && slot < size;
    }

    /**
     * 格式化顏色為 HEX 字串
     */
    public static String formatColorHex(int rgb) {
        return ColorConverter.rgbToHex(rgb);
    }

    /**
     * 格式化顏色為 RGB 字串
     */
    public static String formatColorRgb(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return String.format("RGB(%d, %d, %d)", r, g, b);
    }
}
