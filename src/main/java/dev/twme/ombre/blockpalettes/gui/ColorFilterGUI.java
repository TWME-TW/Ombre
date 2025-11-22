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
import dev.twme.ombre.i18n.MessageManager;
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
    private final MessageManager messageManager;
    
    public ColorFilterGUI(Ombre plugin, BlockPalettesFeature feature, Player player, 
                          PaletteFilter currentFilter, Runnable onFilterApplied) {
        this.plugin = plugin;
        this.feature = feature;
        this.player = player;
        this.currentFilter = currentFilter;
        this.onFilterApplied = onFilterApplied;
        this.messageManager = plugin.getMessageManager();
        
        // 建立 27 格箱子 (3x9)
        this.inventory = Bukkit.createInventory(null, 27,
            messageManager.getComponent("blockpalettes.gui.color-filter.title", player)
        );
        
        setupItems();
    }
    
    /**
     * 設定介面物品
     */
    private void setupItems() {
        String currentColor = currentFilter.getColor();
        
        // 第1行 - 全部和暖色系
        setColorItem(10, "all", Material.WHITE_WOOL, NamedTextColor.WHITE, currentColor);
        setColorItem(11, "red", Material.RED_WOOL, NamedTextColor.RED, currentColor);
        setColorItem(12, "orange", Material.ORANGE_WOOL, NamedTextColor.GOLD, currentColor);
        setColorItem(13, "yellow", Material.YELLOW_WOOL, NamedTextColor.YELLOW, currentColor);
        
        // 第2行 - 冷色系
        setColorItem(19, "green", Material.GREEN_WOOL, NamedTextColor.GREEN, currentColor);
        setColorItem(20, "blue", Material.BLUE_WOOL, NamedTextColor.BLUE, currentColor);
        setColorItem(21, "purple", Material.PURPLE_WOOL, NamedTextColor.LIGHT_PURPLE, currentColor);
        setColorItem(22, "black", Material.BLACK_WOOL, NamedTextColor.DARK_GRAY, currentColor);
        setColorItem(23, "white", Material.WHITE_CONCRETE, NamedTextColor.WHITE, currentColor);
        
        // 返回按鈕 (格子 26)
        ItemStack backItem = createItem(Material.ARROW,
            messageManager.getComponent("blockpalettes.gui.color-filter.back", player),
            List.of(messageManager.getComponent("blockpalettes.gui.color-filter.back-lore", player))
        );
        inventory.setItem(26, backItem);
    }
    
    /**
     * 設定顏色物品
     */
    private void setColorItem(int slot, String colorId, Material material, 
                              NamedTextColor color, String currentColor) {
        boolean selected = colorId.equals(currentColor);
        
        Component displayName = messageManager.getComponent("blockpalettes.gui.color-filter.color." + colorId, player);
        
        List<Component> lore = List.of(
            messageManager.getComponent("blockpalettes.gui.color-filter.filter-hint", player, "color", 
                messageManager.getMessage("blockpalettes.gui.color-filter.color." + colorId, player)),
            Component.empty(),
            selected ? 
                messageManager.getComponent("blockpalettes.gui.color-filter.selected", player) :
                messageManager.getComponent("blockpalettes.gui.color-filter.click-to-select", player)
        );
        
        ItemStack item = createItem(material,
            displayName.color(color).decoration(TextDecoration.BOLD, selected),
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
                messageManager.getComponent("blockpalettes.gui.color-filter.applied", player,
                    "color", messageManager.getMessage("blockpalettes.gui.color-filter.color." + selectedColor, player))
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
}
