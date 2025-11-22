package dev.twme.ombre.blockcolors.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.blockcolors.BlockColorsFeature;
import dev.twme.ombre.blockcolors.data.BlockColorData;
import dev.twme.ombre.blockcolors.data.PlayerPalette;
import dev.twme.ombre.blockcolors.util.GuiUtils;
import dev.twme.ombre.i18n.MessageManager;

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
    private final Ombre plugin;
    private final MessageManager msg;

    public PaletteGUI(BlockColorsFeature feature, Player player, BlockColorsGUI parentGUI) {
        this.feature = feature;
        this.player = player;
        this.parentGUI = parentGUI;
        this.palette = feature.getPlayerPalette(player.getUniqueId());
        this.plugin = (Ombre) feature.getPlugin();
        this.msg = plugin.getMessageManager();
        
        this.inventory = Bukkit.createInventory(this, SIZE, 
            msg.getComponent("blockcolors.gui.palette.title", player));
        
        setupGUI();
    }

    /**
     * 設置 GUI
     */
    private void setupGUI() {
        // 第1行：控制列
        inventory.setItem(SLOT_BACK, GuiUtils.createItem(
            Material.ARROW, msg.getComponent("blockcolors.gui.palette.back", player), msg.getComponent("blockcolors.gui.palette.back-lore", player)
        ));
        inventory.setItem(4, GuiUtils.createItem(
            Material.PAINTING, msg.getComponent("blockcolors.gui.palette.title", player)
        ));
        inventory.setItem(SLOT_CLOSE, GuiUtils.createItem(
            Material.BARRIER, msg.getComponent("blockcolors.gui.palette.close", player)
        ));
        
        // 更新調色盤顯示
        updatePaletteDisplay();
        
        // 底部功能
        inventory.setItem(SLOT_CLEAR, GuiUtils.createItem(
            Material.LAVA_BUCKET, msg.getComponent("blockcolors.gui.palette.clear", player),
            msg.getComponentList("blockcolors.gui.palette.clear-lore", player, null)
        ));
        
        inventory.setItem(SLOT_HELP, GuiUtils.createItem(
            Material.BOOK, msg.getComponent("blockcolors.gui.palette.help", player),
            msg.getComponentList("blockcolors.gui.palette.help-lore", player, null)
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
            
            List<net.kyori.adventure.text.Component> lore = buildPaletteBlockLore(blockData);
            String displayName = blockData != null ? blockData.getDisplayName() : material.name();
            ItemStack item = GuiUtils.createItem(
                material,
                msg.getComponent("blockcolors.gui.palette.item-name", player, Map.of("name", displayName)),
                lore
            );
            inventory.setItem(PALETTE_START + i, item);
        }
        
        // 顯示空槽位提示
        for (int i = blocks.size(); i < 18; i++) {
            inventory.setItem(PALETTE_START + i, GuiUtils.createItem(
                Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                msg.getComponent("blockcolors.gui.palette.empty-slot-title", player),
                msg.getComponent("blockcolors.gui.palette.empty-slot-lore", player)
            ));
        }
    }

    private List<net.kyori.adventure.text.Component> buildPaletteBlockLore(BlockColorData data) {
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        if (data != null) {
            lore.add(msg.getComponent("blockcolors.gui.palette.block-info.display-name", player, Map.of("name", data.getDisplayName())));
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(msg.getComponent("blockcolors.gui.palette.block-info.hex", player, Map.of("hex", data.getHexColor())));
            String rgb = "(" + data.getRed() + ", " + data.getGreen() + ", " + data.getBlue() + ")";
            lore.add(msg.getComponent("blockcolors.gui.palette.block-info.rgb", player, Map.of("rgb", rgb)));
        }
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(msg.getComponent("blockcolors.gui.palette.block-info.remove", player));
        lore.add(msg.getComponent("blockcolors.gui.palette.block-info.take", player));
        return lore;
    }

    /**
     * 處理點擊事件
     */
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        
        // 檢查是否是調色盤方塊的右鍵點擊
        boolean isPaletteRightClick = (slot >= PALETTE_START && slot <= PALETTE_END) && event.isRightClick();
        
        // 調色盤方塊右鍵不取消事件,允許拿取
        if (!isPaletteRightClick) {
            event.setCancelled(true);
        }
        
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
            msg.sendMessage(player, "blockcolors.gui.palette.help-title");
            msg.getComponentList("blockcolors.gui.palette.help-lines", Map.of("max", palette.getMaxSize()))
               .forEach(player::sendMessage);
            return;
        }
        
        if (slot == SLOT_CLEAR) {
            if (palette.isEmpty()) {
                msg.sendMessage(player, "blockcolors.gui.palette.messages.clear-empty");
                return;
            }
            
            palette.clear();
            updatePaletteDisplay();
            msg.sendMessage(player, "blockcolors.gui.palette.messages.clear-success");
            // 立即儲存調色盤
            feature.savePlayerPalette(player.getUniqueId());
            return;
        }
        
        // 調色盤方塊點擊
        if (slot >= PALETTE_START && slot <= PALETTE_END) {
            int index = slot - PALETTE_START;
            List<Material> blocks = palette.getBlocks();
            
            if (index < blocks.size()) {
                Material material = blocks.get(index);
                BlockColorData blockData = feature.getCache().getBlockByMaterial(material);
                String displayName = blockData != null ? 
                    blockData.getDisplayName() : material.name();
                
                if (event.isRightClick()) {
                    // 右鍵 - 允許拿取方塊(事件已不取消)
                    msg.sendMessage(player, "blockcolors.gui.messages.obtained", Map.of("block", displayName));
                    
                    // 延遲補上相同方塊
                    final int targetSlot = slot;
                    final Material blockMaterial = material;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        BlockColorData data = feature.getCache().getBlockByMaterial(blockMaterial);
                        
                        List<net.kyori.adventure.text.Component> lore = buildPaletteBlockLore(data);
                        String name = data != null ? data.getDisplayName() : blockMaterial.name();
                        ItemStack item = GuiUtils.createItem(
                            blockMaterial,
                            msg.getComponent("blockcolors.gui.palette.item-name", player, Map.of("name", name)),
                            lore
                        );
                        inventory.setItem(targetSlot, item);
                    }, 1L);
                } else {
                    // 左鍵 - 移除
                    if (palette.removeBlock(material)) {
                        updatePaletteDisplay();
                        msg.sendMessage(player, "blockcolors.gui.palette.messages.remove-success", Map.of("block", displayName));
                        // 立即儲存調色盤
                        feature.savePlayerPalette(player.getUniqueId());
                    } else {
                        msg.sendMessage(player, "blockcolors.gui.palette.messages.remove-failed");
                    }
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
