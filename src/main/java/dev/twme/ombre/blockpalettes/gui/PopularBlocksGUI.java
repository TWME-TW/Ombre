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
 * 熱門方塊快速篩選 GUI
 */
public class PopularBlocksGUI implements Listener {
    
    private final Ombre plugin;
    private final BlockPalettesFeature feature;
    private final Player player;
    private final Inventory inventory;
    private final PaletteFilter currentFilter;
    private final Runnable onFilterApplied;
    
    // 熱門方塊列表（根據 blockpalettes.com 統計）
    private static final String[][] POPULAR_BLOCKS = {
        {"spruce_planks", "雲杉木材"},
        {"stone_bricks", "石磚"},
        {"dark_oak_planks", "黑橡木材"},
        {"deepslate_tiles", "深板岩磚"},
        {"spruce_log", "雲杉原木"},
        {"moss_block", "苔蘚方塊"},
        {"stripped_spruce_log", "剝皮雲杉原木"},
        {"deepslate_bricks", "深板岩磚塊"},
        {"calcite", "方解石"},
        {"quartz_block", "石英方塊"},
        {"oak_planks", "橡木材"},
        {"white_concrete", "白色混凝土"},
        {"gray_concrete", "灰色混凝土"},
        {"smooth_stone", "平滑石頭"},
        {"polished_andesite", "磨製安山岩"}
    };
    
    public PopularBlocksGUI(Ombre plugin, BlockPalettesFeature feature, Player player,
                            PaletteFilter currentFilter, Runnable onFilterApplied) {
        this.plugin = plugin;
        this.feature = feature;
        this.player = player;
        this.currentFilter = currentFilter;
        this.onFilterApplied = onFilterApplied;
        
        // 建立 54 格箱子 (6x9)
        this.inventory = Bukkit.createInventory(null, 54,
            Component.text("熱門方塊篩選")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
        );
        
        setupItems();
    }
    
    /**
     * 設定介面物品
     */
    private void setupItems() {
        // 顯示熱門方塊（每行5個，共3行）
        int[] slots = {
            10, 11, 12, 13, 14,  // 第2行
            19, 20, 21, 22, 23,  // 第3行
            28, 29, 30, 31, 32   // 第4行
        };
        
        for (int i = 0; i < Math.min(POPULAR_BLOCKS.length, slots.length); i++) {
            String blockId = POPULAR_BLOCKS[i][0];
            String displayName = POPULAR_BLOCKS[i][1];
            Material material = getMaterialFromId(blockId);
            
            ItemStack item = createBlockItem(material, displayName, blockId);
            inventory.setItem(slots[i], item);
        }
        
        // 清除篩選 (格子 45)
        ItemStack clearItem = createItem(Material.BARRIER,
            Component.text("清除方塊篩選").color(NamedTextColor.RED),
            List.of(
                Component.text("移除方塊篩選條件").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("▸ 點擊清除").color(NamedTextColor.YELLOW)
            )
        );
        inventory.setItem(45, clearItem);
        
        // 返回 (格子 53)
        ItemStack backItem = createItem(Material.ARROW,
            Component.text("返回").color(NamedTextColor.YELLOW),
            List.of(Component.text("▸ 點擊返回列表").color(NamedTextColor.YELLOW))
        );
        inventory.setItem(53, backItem);
    }
    
    /**
     * 建立方塊物品
     */
    private ItemStack createBlockItem(Material material, String displayName, String blockId) {
        boolean selected = blockId.equals(currentFilter.getBlockSearch());
        
        List<Component> lore = List.of(
            Component.text("搜尋包含此方塊的調色板").color(NamedTextColor.GRAY),
            Component.empty(),
            selected ? 
                Component.text("✓ 當前選擇").color(NamedTextColor.GREEN) :
                Component.text("▸ 點擊選擇").color(NamedTextColor.YELLOW)
        );
        
        ItemStack item = createItem(material,
            Component.text(displayName).color(selected ? NamedTextColor.GOLD : NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, selected),
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
        
        return item;
    }
    
    /**
     * 從方塊 ID 取得 Material
     */
    private Material getMaterialFromId(String blockId) {
        try {
            return Material.valueOf(blockId.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
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
        
        // 方塊選擇
        int[] slots = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
        for (int i = 0; i < slots.length; i++) {
            if (slot == slots[i] && i < POPULAR_BLOCKS.length) {
                String blockId = POPULAR_BLOCKS[i][0];
                String displayName = POPULAR_BLOCKS[i][1];
                
                currentFilter.setBlockSearch(blockId);
                currentFilter.setPage(1); // 重置頁碼
                
                clicker.closeInventory();
                clicker.sendMessage(
                    Component.text("✓ 已篩選包含 " + displayName + " 的調色板")
                        .color(NamedTextColor.GREEN)
                );
                clicker.playSound(clicker.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                
                if (onFilterApplied != null) {
                    onFilterApplied.run();
                }
                return;
            }
        }
        
        // 清除篩選 (格子 45)
        if (slot == 45) {
            currentFilter.setBlockSearch("");
            currentFilter.setPage(1);
            
            clicker.closeInventory();
            clicker.sendMessage(Component.text("✓ 已清除方塊篩選").color(NamedTextColor.GREEN));
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            
            if (onFilterApplied != null) {
                onFilterApplied.run();
            }
            return;
        }
        
        // 返回 (格子 53)
        if (slot == 53) {
            clicker.closeInventory();
            if (onFilterApplied != null) {
                onFilterApplied.run();
            }
        }
    }
}
