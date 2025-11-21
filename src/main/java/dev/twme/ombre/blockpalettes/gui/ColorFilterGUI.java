package dev.twme.ombre.blockpalettes.gui;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.blockpalettes.BlockPalettesFeature;
import dev.twme.ombre.blockpalettes.api.PaletteFilter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * 顏色篩選選單 GUI
 */
public class ColorFilterGUI implements Listener {
    
    private final Ombre plugin;
    private final BlockPalettesFeature feature;
    private final Player player;
    private final Inventory inventory;
    private final PaletteFilter currentFilter;
    private final Runnable onFilterApplied;
    
    public ColorFilterGUI(Ombre plugin, BlockPalettesFeature feature, Player player, 
                          PaletteFilter currentFilter, Runnable onFilterApplied) {
        this.plugin = plugin;
        this.feature = feature;
        this.player = player;
        this.currentFilter = currentFilter;
        this.onFilterApplied = onFilterApplied;
        
        // 建立 27 格箱子 (3x9)
        this.inventory = Bukkit.createInventory(null, 27,
            Component.text("顏色篩選")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
        );
        
        setupItems();
    }
    
    /**
     * 設定介面物品
     */
    private void setupItems() {
        String currentColor = currentFilter.getColor();
        
        // 第1行 - 全部和暖色系
        setColorItem(10, "all", Material.WHITE_WOOL, "全部", NamedTextColor.WHITE, currentColor);
        setColorItem(11, "red", Material.RED_WOOL, "紅色", NamedTextColor.RED, currentColor);
        setColorItem(12, "orange", Material.ORANGE_WOOL, "橙色", NamedTextColor.GOLD, currentColor);
        setColorItem(13, "yellow", Material.YELLOW_WOOL, "黃色", NamedTextColor.YELLOW, currentColor);
        
        // 第2行 - 冷色系
        setColorItem(19, "green", Material.GREEN_WOOL, "綠色", NamedTextColor.GREEN, currentColor);
        setColorItem(20, "blue", Material.BLUE_WOOL, "藍色", NamedTextColor.BLUE, currentColor);
        setColorItem(21, "purple", Material.PURPLE_WOOL, "紫色", NamedTextColor.LIGHT_PURPLE, currentColor);
        setColorItem(22, "black", Material.BLACK_WOOL, "黑色", NamedTextColor.DARK_GRAY, currentColor);
        setColorItem(23, "white", Material.WHITE_CONCRETE, "白色", NamedTextColor.WHITE, currentColor);
        
        // 返回按鈕 (格子 26)
        ItemStack backItem = createItem(Material.ARROW,
            Component.text("返回").color(NamedTextColor.YELLOW),
            List.of(Component.text("▸ 點擊返回列表").color(NamedTextColor.YELLOW))
        );
        inventory.setItem(26, backItem);
    }
    
    /**
     * 設定顏色物品
     */
    private void setColorItem(int slot, String colorId, Material material, String displayName, 
                              NamedTextColor color, String currentColor) {
        boolean selected = colorId.equals(currentColor);
        
        List<Component> lore = List.of(
            Component.text("篩選 " + displayName + " 色系的調色板").color(NamedTextColor.GRAY),
            Component.empty(),
            selected ? 
                Component.text("✓ 當前選擇").color(NamedTextColor.GREEN) :
                Component.text("▸ 點擊選擇").color(NamedTextColor.YELLOW)
        );
        
        ItemStack item = createItem(material,
            Component.text(displayName).color(color).decoration(TextDecoration.BOLD, selected),
            lore
        );
        
        if (selected) {
            // 添加附魔光效
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        
        inventory.setItem(slot, item);
    }
    
    /**
     * 建立物品
     */
    private ItemStack createItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 開啟介面
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * 處理點擊事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player clicker)) {
            return;
        }
        
        if (!clicker.equals(player)) {
            return;
        }
        
        // 檢查是否點擊的是 GUI 而不是玩家背包
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) {
            return;
        }
        
        int slot = event.getSlot();
        
        // 顏色選擇
        String selectedColor = switch (slot) {
            case 10 -> "all";
            case 11 -> "red";
            case 12 -> "orange";
            case 13 -> "yellow";
            case 19 -> "green";
            case 20 -> "blue";
            case 21 -> "purple";
            case 22 -> "black";
            case 23 -> "white";
            default -> null;
        };
        
        if (selectedColor != null) {
            currentFilter.setColor(selectedColor);
            currentFilter.setPage(1); // 重置頁碼
            
            clicker.closeInventory();
            clicker.sendMessage(
                Component.text("✓ 已套用顏色篩選: " + getColorDisplayName(selectedColor))
                    .color(NamedTextColor.GREEN)
            );
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            
            if (onFilterApplied != null) {
                onFilterApplied.run();
            }
            return;
        }
        
        // 返回 (格子 26)
        if (slot == 26) {
            clicker.closeInventory();
            if (onFilterApplied != null) {
                onFilterApplied.run();
            }
        }
    }
    
    /**
     * 取得顏色顯示名稱
     */
    private String getColorDisplayName(String colorId) {
        return switch (colorId) {
            case "all" -> "全部";
            case "red" -> "紅色";
            case "orange" -> "橙色";
            case "yellow" -> "黃色";
            case "green" -> "綠色";
            case "blue" -> "藍色";
            case "purple" -> "紫色";
            case "black" -> "黑色";
            case "white" -> "白色";
            default -> "未知";
        };
    }
}
