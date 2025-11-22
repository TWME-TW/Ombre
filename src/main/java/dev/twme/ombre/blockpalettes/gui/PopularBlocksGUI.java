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
 * 熱門方塊快速篩選 GUI
 */
public class PopularBlocksGUI implements Listener {
    
    private final Ombre plugin;
    private final BlockPalettesFeature feature;
    private final Player player;
    private final Inventory inventory;
    private final PaletteFilter currentFilter;
    private final Runnable onFilterApplied;
    private final MessageManager messageManager;
    
    // 熱門方塊列表（根據 blockpalettes.com 統計）
    private static final String[] POPULAR_BLOCKS = {
        "spruce_planks",
        "stone_bricks",
        "dark_oak_planks",
        "deepslate_tiles",
        "spruce_log",
        "moss_block",
        "stripped_spruce_log",
        "deepslate_bricks",
        "calcite",
        "quartz_block",
        "oak_planks",
        "white_concrete",
        "gray_concrete",
        "smooth_stone",
        "polished_andesite"
    };
    
    public PopularBlocksGUI(Ombre plugin, BlockPalettesFeature feature, Player player,
                            PaletteFilter currentFilter, Runnable onFilterApplied) {
        this.plugin = plugin;
        this.feature = feature;
        this.player = player;
        this.currentFilter = currentFilter;
        this.onFilterApplied = onFilterApplied;
        this.messageManager = plugin.getMessageManager();
        
        // 建立 54 格箱子 (6x9)
        this.inventory = Bukkit.createInventory(null, 54,
            messageManager.getComponent("blockpalettes.gui.popular-blocks.title", player)
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
            String blockId = POPULAR_BLOCKS[i];
            String displayName = messageManager.getMessage("blockpalettes.gui.popular-blocks.block." + blockId, player);
            Material material = getMaterialFromId(blockId);
            
            ItemStack item = createBlockItem(material, displayName, blockId);
            inventory.setItem(slots[i], item);
        }
        
        // 清除篩選 (格子 45)
        ItemStack clearItem = createItem(Material.BARRIER,
            messageManager.getComponent("blockpalettes.gui.popular-blocks.clear.title", player),
            List.of(
                messageManager.getComponent("blockpalettes.gui.popular-blocks.clear.description", player),
                Component.empty(),
                messageManager.getComponent("blockpalettes.gui.popular-blocks.clear.click", player)
            )
        );
        inventory.setItem(45, clearItem);
        
        // 返回 (格子 53)
        ItemStack backItem = createItem(Material.ARROW,
            messageManager.getComponent("blockpalettes.gui.popular-blocks.back", player),
            List.of(messageManager.getComponent("blockpalettes.gui.popular-blocks.back-lore", player))
        );
        inventory.setItem(53, backItem);
    }
    
    /**
     * 建立方塊物品
     */
    private ItemStack createBlockItem(Material material, String displayName, String blockId) {
        boolean selected = blockId.equals(currentFilter.getBlockSearch());
        
        List<Component> lore = List.of(
            messageManager.getComponent("blockpalettes.gui.popular-blocks.description", player),
            Component.empty(),
            selected ? 
                messageManager.getComponent("blockpalettes.gui.popular-blocks.selected", player) :
                messageManager.getComponent("blockpalettes.gui.popular-blocks.click-to-select", player)
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
                String blockId = POPULAR_BLOCKS[i];
                String displayName = messageManager.getMessage("blockpalettes.gui.popular-blocks.block." + blockId, player);
                
                currentFilter.setBlockSearch(blockId);
                currentFilter.setPage(1); // 重置頁碼
                
                clicker.closeInventory();
                clicker.sendMessage(
                    messageManager.getComponent("blockpalettes.gui.popular-blocks.messages.filtered", player, "block", displayName)
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
            clicker.sendMessage(messageManager.getComponent("blockpalettes.gui.popular-blocks.messages.cleared", player));
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
