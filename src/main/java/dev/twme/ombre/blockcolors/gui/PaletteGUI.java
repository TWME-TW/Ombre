package dev.twme.ombre.blockcolors.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import dev.twme.ombre.blockcolors.BlockColorsFeature;
import dev.twme.ombre.blockcolors.data.BlockColorData;
import dev.twme.ombre.blockcolors.data.PlayerPalette;
import dev.twme.ombre.blockcolors.util.GuiUtils;

/**
 * 調色盤管理 GUI
 * 顯示和管理玩家儲存的方塊
 */
public class PaletteGUI implements InventoryHolder {
    private static final int ROWS = 4;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS;
    
    // 控制按鈕槽位
    private static final int SLOT_BACK = 0;
    private static final int SLOT_CLOSE = 8;
    
    // 調色盤顯示區域（第2-3行，18個槽位）
    private static final int PALETTE_START = 9;
    private static final int PALETTE_END = 26;
    
    // 底部功能列
    private static final int SLOT_CLEAR = 27;
    private static final int SLOT_HELP = 31;
    
    private final BlockColorsFeature feature;
    private final Player player;
    private final BlockColorsGUI parentGUI;
    private final Inventory inventory;
    private final PlayerPalette palette;

    public PaletteGUI(BlockColorsFeature feature, Player player, BlockColorsGUI parentGUI) {
        this.feature = feature;
        this.player = player;
        this.parentGUI = parentGUI;
        this.palette = feature.getPlayerPalette(player.getUniqueId());
        
        this.inventory = Bukkit.createInventory(this, SIZE, "§d我的調色盤");
        
        setupGUI();
    }

    /**
     * 設置 GUI
     */
    private void setupGUI() {
        // 第1行：控制列
        inventory.setItem(SLOT_BACK, GuiUtils.createItem(
            Material.ARROW, "§e返回", "§7返回 BlockColors 主介面"
        ));
        inventory.setItem(4, GuiUtils.createItem(
            Material.PAINTING, "§d§l我的調色盤"
        ));
        inventory.setItem(SLOT_CLOSE, GuiUtils.createItem(
            Material.BARRIER, "§c關閉"
        ));
        
        // 更新調色盤顯示
        updatePaletteDisplay();
        
        // 底部功能
        inventory.setItem(SLOT_CLEAR, GuiUtils.createItem(
            Material.LAVA_BUCKET, "§c清空調色盤",
            "§7點擊清空所有方塊",
            "§c警告: 此操作無法復原！"
        ));
        
        inventory.setItem(SLOT_HELP, GuiUtils.createItem(
            Material.BOOK, "§e說明",
            "§7點擊方塊可將其移除",
            "§7最多可儲存 " + palette.getMaxSize() + " 個方塊"
        ));
        
        // 填充其他空格
        for (int i = 28; i < 31; i++) {
            inventory.setItem(i, GuiUtils.createFiller());
        }
        for (int i = 32; i < SIZE; i++) {
            inventory.setItem(i, GuiUtils.createFiller());
        }
    }

    /**
     * 更新調色盤顯示
     */
    private void updatePaletteDisplay() {
        // 清空顯示區域
        for (int i = PALETTE_START; i <= PALETTE_END; i++) {
            inventory.setItem(i, null);
        }
        
        List<Material> blocks = palette.getBlocks();
        
        for (int i = 0; i < blocks.size() && i < 18; i++) {
            Material material = blocks.get(i);
            
            // 從快取獲取方塊資訊
            BlockColorData blockData = feature.getCache().getBlockByMaterial(material);
            
            List<String> lore = new ArrayList<>();
            if (blockData != null) {
                lore.add("§7" + blockData.getDisplayName());
                lore.add("");
                lore.add("§7HEX: §f" + blockData.getHexColor());
                lore.add("§7RGB: §f(" + blockData.getRed() + ", " + 
                         blockData.getGreen() + ", " + blockData.getBlue() + ")");
            }
            lore.add("");
            lore.add("§c點擊移除");
            
            String displayName = blockData != null ? 
                "§b" + blockData.getDisplayName() : 
                "§b" + material.name();
            
            ItemStack item = GuiUtils.createItem(material, displayName, lore);
            inventory.setItem(PALETTE_START + i, item);
        }
        
        // 顯示空槽位提示
        for (int i = blocks.size(); i < 18; i++) {
            inventory.setItem(PALETTE_START + i, GuiUtils.createItem(
                Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                "§7空槽位",
                "§7在主介面點擊方塊加入"
            ));
        }
    }

    /**
     * 處理點擊事件
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // 控制按鈕
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        
        if (slot == SLOT_BACK) {
            player.closeInventory();
            if (parentGUI != null) {
                parentGUI.open();
            }
            return;
        }
        
        if (slot == SLOT_HELP) {
            player.sendMessage("§d§l========== 調色盤說明 ==========");
            player.sendMessage("§e功能:");
            player.sendMessage("§7- 點擊方塊可將其從調色盤移除");
            player.sendMessage("§7- 使用清空按鈕清除所有方塊");
            player.sendMessage("§7- 最多可儲存 " + palette.getMaxSize() + " 個方塊");
            return;
        }
        
        if (slot == SLOT_CLEAR) {
            if (palette.isEmpty()) {
                player.sendMessage("§c調色盤已經是空的");
                return;
            }
            
            palette.clear();
            updatePaletteDisplay();
            player.sendMessage("§a✓ 已清空調色盤");
            return;
        }
        
        // 調色盤方塊點擊 - 移除
        if (slot >= PALETTE_START && slot <= PALETTE_END) {
            int index = slot - PALETTE_START;
            List<Material> blocks = palette.getBlocks();
            
            if (index < blocks.size()) {
                Material material = blocks.get(index);
                
                if (palette.removeBlock(material)) {
                    updatePaletteDisplay();
                    
                    BlockColorData blockData = feature.getCache().getBlockByMaterial(material);
                    String displayName = blockData != null ? 
                        blockData.getDisplayName() : material.name();
                    
                    player.sendMessage("§a✓ 已移除 " + displayName);
                } else {
                    player.sendMessage("§c移除失敗");
                }
            }
        }
    }

    /**
     * 開啟 GUI
     */
    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
